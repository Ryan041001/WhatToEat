package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RestaurantQueryApplicationService {

    private static final String SORT_DISTANCE = "distance";
    private static final String SORT_AVG_RATING = "avgRating";
    private static final String SORT_REVIEW_COUNT = "reviewCount";
    private static final String SORT_AVG_PRICE_ASC = "avgPriceAsc";
    private static final String SORT_AVG_PRICE_DESC = "avgPriceDesc";
    private static final String SORT_SMART = "smart";
    private static final Set<String> SUPPORTED_SORTS = Set.of(
            SORT_DISTANCE,
            SORT_AVG_RATING,
            SORT_REVIEW_COUNT,
            SORT_AVG_PRICE_ASC,
            SORT_AVG_PRICE_DESC,
            SORT_SMART);
    private static final int SORTED_CANDIDATE_MIN_SIZE = 20;

    private final AmapClient amapClient;
    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final MeterRegistry meterRegistry;

    public RestaurantQueryApplicationService(
            AmapClient amapClient,
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            MeterRegistry meterRegistry) {
        this.amapClient = amapClient;
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.meterRegistry = meterRegistry;
    }

    public RestaurantPage nearby(double longitude, double latitude, int radius, int page, int size) {
        return nearby(longitude, latitude, radius, page, size, SORT_DISTANCE);
    }

    public RestaurantPage nearby(double longitude, double latitude, int radius, int page, int size, String sort) {
        try {
            String normalizedSort = normalizeSort(sort);
            AmapClient.AmapSearchResult result = fetchNearbyCandidates(longitude, latitude, radius, page, size, normalizedSort);
            if (result.total() == 0) {
                throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
            }
            List<RestaurantListItem> items = toRestaurantPageItems(result.items(), page, size, normalizedSort);
            incrementRequestCounter("nearby", "success");
            return new RestaurantPage(items, page, size, result.total());
        } catch (BusinessException e) {
            incrementRequestCounter("nearby", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("nearby", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    public RestaurantPage search(String keyword, double longitude, double latitude, int radius, int page, int size) {
        return search(keyword, longitude, latitude, radius, page, size, SORT_DISTANCE);
    }

    public RestaurantPage search(String keyword, double longitude, double latitude, int radius, int page, int size, String sort) {
        try {
            String normalizedSort = normalizeSort(sort);
            AmapClient.AmapSearchResult result = fetchSearchCandidates(keyword, longitude, latitude, radius, page, size, normalizedSort);
            if (result.total() == 0) {
                throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
            }
            List<RestaurantListItem> items = toRestaurantPageItems(result.items(), page, size, normalizedSort);
            incrementRequestCounter("search", "success");
            return new RestaurantPage(items, page, size, result.total());
        } catch (BusinessException e) {
            incrementRequestCounter("search", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("search", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    private void incrementRequestCounter(String scene, String result) {
        Counter.builder("restaurant.query.requests")
                .tag("scene", scene)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SORT_DISTANCE;
        }
        String normalized = sort.trim();
        if (!SUPPORTED_SORTS.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return normalized;
    }

    private AmapClient.AmapSearchResult fetchNearbyCandidates(
            double longitude,
            double latitude,
            int radius,
            int page,
            int size,
            String sort) {
        if (SORT_DISTANCE.equals(sort)) {
            return amapClient.searchNearby(longitude, latitude, radius, page, size);
        }
        return amapClient.searchNearby(longitude, latitude, radius, 1, candidatePoolSize(size));
    }

    private AmapClient.AmapSearchResult fetchSearchCandidates(
            String keyword,
            double longitude,
            double latitude,
            int radius,
            int page,
            int size,
            String sort) {
        if (SORT_DISTANCE.equals(sort)) {
            return amapClient.searchByKeyword(keyword, longitude, latitude, radius, page, size);
        }
        return amapClient.searchByKeyword(keyword, longitude, latitude, radius, 1, candidatePoolSize(size));
    }

    private int candidatePoolSize(int size) {
        return Math.max(size * 5, SORTED_CANDIDATE_MIN_SIZE);
    }

    private List<RestaurantListItem> toRestaurantPageItems(List<AmapPoi> pois, int page, int size, String sort) {
        Map<String, RestaurantMetricSnapshotEntity> snapshotByPoiId = restaurantMetricSnapshotRepository.findAllById(
                        pois.stream().map(AmapPoi::poiId).toList())
                .stream()
                .collect(Collectors.toMap(RestaurantMetricSnapshotEntity::getPoiId, snapshot -> snapshot));

        List<RestaurantListItem> items = pois.stream()
                .map(poi -> {
                    RestaurantMetricSnapshotEntity snapshot = snapshotByPoiId.get(poi.poiId());
                    return new RestaurantListItem(
                            poi.poiId(),
                            poi.name(),
                            poi.address(),
                            poi.longitude(),
                            poi.latitude(),
                            poi.category(),
                            poi.distance(),
                            snapshot == null ? null : snapshot.getAvgRating(),
                            snapshot == null || snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(),
                            snapshot == null ? null : snapshot.getAvgPerCapitaPrice(),
                            snapshot == null ? List.of() : Stream.of(snapshot.getAiTag1(), snapshot.getAiTag2())
                                    .filter(tag -> tag != null && !tag.isBlank())
                                    .toList())
                        ;
                })
                .toList();

        if (SORT_DISTANCE.equals(sort)) {
            return items;
        }

        List<RestaurantListItem> sorted = new ArrayList<>(items);
        sorted.sort(comparatorFor(sort));
        return paginate(sorted, page, size);
    }

    private List<RestaurantListItem> paginate(List<RestaurantListItem> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private Comparator<RestaurantListItem> comparatorFor(String sort) {
        Comparator<RestaurantListItem> distanceAsc = Comparator.comparingDouble(RestaurantListItem::distance);
        Comparator<RestaurantListItem> avgRatingDesc = Comparator.comparing(
                RestaurantListItem::avgRating,
                Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<RestaurantListItem> reviewCountDesc = Comparator.comparingInt(RestaurantListItem::reviewCount).reversed();
        Comparator<RestaurantListItem> avgPriceAsc = Comparator.comparing(
                RestaurantListItem::avgPerCapitaPrice,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<RestaurantListItem> avgPriceDesc = Comparator.comparing(
                RestaurantListItem::avgPerCapitaPrice,
                Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<RestaurantListItem> hasReviewsFirst = Comparator.comparingInt(item -> item.reviewCount() > 0 ? 0 : 1);

        return switch (sort) {
            case SORT_AVG_RATING -> avgRatingDesc.thenComparing(reviewCountDesc).thenComparing(distanceAsc);
            case SORT_REVIEW_COUNT -> reviewCountDesc.thenComparing(avgRatingDesc).thenComparing(distanceAsc);
            case SORT_AVG_PRICE_ASC -> avgPriceAsc.thenComparing(avgRatingDesc).thenComparing(distanceAsc);
            case SORT_AVG_PRICE_DESC -> avgPriceDesc.thenComparing(avgRatingDesc).thenComparing(distanceAsc);
            case SORT_SMART -> hasReviewsFirst.thenComparing(avgRatingDesc).thenComparing(reviewCountDesc).thenComparing(distanceAsc);
            default -> distanceAsc;
        };
    }

    public record RestaurantPage(List<RestaurantListItem> items, int page, int size, long total) {
    }

    public record RestaurantListItem(
            String poiId,
            String name,
            String address,
            double longitude,
            double latitude,
            String category,
            double distance,
            BigDecimal avgRating,
            int reviewCount,
            Integer avgPerCapitaPrice,
            List<String> aiTags) {
    }
}
