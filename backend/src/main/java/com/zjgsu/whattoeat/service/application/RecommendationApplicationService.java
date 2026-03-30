package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.service.domain.RecommendationDomainService;
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

    public RecommendationApplicationService(
            AmapClient amapClient,
            UserRepository userRepository,
            UserBlacklistRepository userBlacklistRepository,
            RecommendationDomainService recommendationDomainService) {
        this.amapClient = amapClient;
        this.userRepository = userRepository;
        this.userBlacklistRepository = userBlacklistRepository;
        this.recommendationDomainService = recommendationDomainService;
    }

    /**
     * 根据用户位置和偏好，返回一个随机推荐餐厅
     */
    public RecommendationResult recommendRandom(Long userId, double longitude, double latitude, int radius) {
        List<AmapPoi> candidates = loadCandidates(userId, longitude, latitude, radius, RANDOM_CANDIDATE_SIZE);
        AmapPoi selected = recommendationDomainService.pickRandom(candidates);
        return new RecommendationResult(
                selected.poiId(),
                selected.name(),
                selected.address(),
                selected.longitude(),
                selected.latitude(),
                selected.category(),
                selected.distance(),
                "符合筛选条件的随机结果");
    }

    public List<AmapPoi> recommendCards(Long userId, double longitude, double latitude, int radius, int size) {
        return loadCandidates(userId, longitude, latitude, radius, size);
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
