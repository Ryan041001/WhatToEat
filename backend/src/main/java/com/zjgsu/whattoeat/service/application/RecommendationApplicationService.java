package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.service.domain.RecommendationDomainService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RecommendationApplicationService {

    private static final int RANDOM_CANDIDATE_SIZE = 20;

    private final AmapClient amapClient;
    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final RecommendationDomainService recommendationDomainService;
    private final MeterRegistry meterRegistry;

    public RecommendationApplicationService(
            AmapClient amapClient,
            UserRepository userRepository,
            UserBlacklistRepository userBlacklistRepository,
            RecommendationDomainService recommendationDomainService,
            MeterRegistry meterRegistry) {
        this.amapClient = amapClient;
        this.userRepository = userRepository;
        this.userBlacklistRepository = userBlacklistRepository;
        this.recommendationDomainService = recommendationDomainService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 根据用户位置和偏好，返回一个随机推荐餐厅
     */
    public RecommendationResult recommendRandom(Long userId, double longitude, double latitude, int radius) {
        try {
            List<AmapPoi> candidates = loadCandidates(userId, longitude, latitude, radius, RANDOM_CANDIDATE_SIZE);
            AmapPoi selected = recommendationDomainService.pickRandom(candidates);
            incrementRequestCounter("random", "success");
            return new RecommendationResult(
                    selected.poiId(),
                    selected.name(),
                    selected.address(),
                    selected.longitude(),
                    selected.latitude(),
                    selected.category(),
                    selected.distance(),
                    "符合筛选条件的随机结果");
        } catch (BusinessException e) {
            incrementRequestCounter("random", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("random", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    public List<AmapPoi> recommendCards(Long userId, double longitude, double latitude, int radius, int size) {
        try {
            List<AmapPoi> result = loadCandidates(userId, longitude, latitude, radius, size);
            incrementRequestCounter("cards", "success");
            return result;
        } catch (BusinessException e) {
            incrementRequestCounter("cards", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("cards", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    private void incrementRequestCounter(String endpoint, String result) {
        Counter.builder("recommendation.requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private List<AmapPoi> loadCandidates(Long userId, double longitude, double latitude, int radius, int size) {
        validateUser(userId);
        Set<String> blacklistedPoiIds = blacklistPoiIds(userId);
        List<AmapPoi> collected = new ArrayList<>();
        long total = Long.MAX_VALUE;
        long fetchedCount = 0L;
        int page = 1;

        while (collected.size() < size && fetchedCount < total) {
            AmapClient.AmapSearchResult result = amapClient.searchNearby(longitude, latitude, radius, page, size);
            total = result.total();
            fetchedCount += result.items().size();
            collected.addAll(recommendationDomainService.filterBlacklist(result.items(), blacklistedPoiIds));
            if (result.items().isEmpty()) {
                break;
            }
            page++;
        }

        if (collected.isEmpty()) {
            throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
        }
        if (collected.size() <= size) {
            return List.copyOf(collected);
        }
        return List.copyOf(collected.subList(0, size));
    }

    private void validateUser(Long userId) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Set<String> blacklistPoiIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return userBlacklistRepository.findByUserId(userId).stream()
                .map(blacklist -> blacklist.getPoiId())
                .collect(java.util.stream.Collectors.toSet());
    }


    public record RecommendationResult(
            String poiId,
            String name,
            String address,
            double longitude,
            double latitude,
            String category,
            double distance,
            String reason) {
    }
}
