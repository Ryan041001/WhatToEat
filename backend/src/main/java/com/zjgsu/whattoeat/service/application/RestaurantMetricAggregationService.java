package com.zjgsu.whattoeat.service.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RestaurantMetricAggregationService {

    private final RestaurantReviewRepository restaurantReviewRepository;
    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final Cache<String, RestaurantMetricSnapshotEntity> snapshotCache;

    public RestaurantMetricAggregationService(
            RestaurantReviewRepository restaurantReviewRepository,
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            Cache<String, RestaurantMetricSnapshotEntity> snapshotCache) {
        this.restaurantReviewRepository = restaurantReviewRepository;
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.snapshotCache = snapshotCache;
    }

    @Transactional
    public RestaurantMetricSnapshotEntity refreshSnapshot(String poiId) {
        List<RestaurantReviewEntity> reviews = restaurantReviewRepository.findByPoiIdOrderByUpdatedAtDescIdDesc(poiId);
        RestaurantMetricSnapshotEntity snapshot = restaurantMetricSnapshotRepository.findById(poiId)
                .orElseGet(() -> {
                    RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
                    entity.setPoiId(poiId);
                    return entity;
                });

        if (reviews.isEmpty()) {
            snapshot.setReviewCount(0);
            snapshot.setAvgRating(null);
            snapshot.setAvgPerCapitaPrice(null);
            snapshot.setAiStatus("idle");
            snapshot.setLastReviewAt(null);
            RestaurantMetricSnapshotEntity saved = restaurantMetricSnapshotRepository.saveAndFlush(snapshot);
            snapshotCache.invalidate(poiId);
            return saved;
        }

        BigDecimal totalRating = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (RestaurantReviewEntity review : reviews) {
            totalRating = totalRating.add(review.getRatingScore());
            totalPrice = totalPrice.add(BigDecimal.valueOf(review.getPerCapitaPrice()));
        }

        BigDecimal reviewCount = BigDecimal.valueOf(reviews.size());
        snapshot.setReviewCount(reviews.size());
        snapshot.setAvgRating(totalRating.divide(reviewCount, 1, RoundingMode.HALF_UP));
        snapshot.setAvgPerCapitaPrice(totalPrice.divide(reviewCount, 0, RoundingMode.HALF_UP).intValue());
        snapshot.setAiStatus("pending");
        snapshot.setLastReviewAt(reviews.get(0).getUpdatedAt());
        RestaurantMetricSnapshotEntity saved = restaurantMetricSnapshotRepository.saveAndFlush(snapshot);
        snapshotCache.invalidate(poiId);
        return saved;
    }
}
