package com.zjgsu.whattoeat.application.recommendation;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.service.application.UserChoiceHistoryApplicationService;
import com.zjgsu.whattoeat.service.application.UserRecommendationFeedbackApplicationService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
final class RecommendationCandidateLoader {

    private final AmapClient amapClient;
    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final UserChoiceHistoryApplicationService userChoiceHistoryApplicationService;
    private final UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService;

    RecommendationCandidateLoader(
            AmapClient amapClient,
            UserRepository userRepository,
            UserBlacklistRepository userBlacklistRepository,
            UserChoiceHistoryApplicationService userChoiceHistoryApplicationService,
            UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService) {
        this.amapClient = amapClient;
        this.userRepository = userRepository;
        this.userBlacklistRepository = userBlacklistRepository;
        this.userChoiceHistoryApplicationService = userChoiceHistoryApplicationService;
        this.userRecommendationFeedbackApplicationService = userRecommendationFeedbackApplicationService;
    }

    List<AmapPoi> load(
            Long userId,
            double longitude,
            double latitude,
            int radius,
            int size,
            Set<String> requestRejectedPoiIds) {
        validateUser(userId);
        Set<String> hardExcludedPoiIds = blacklistPoiIds(userId);
        LinkedHashSet<String> softExcludedPoiIds = new LinkedHashSet<>();
        if (requestRejectedPoiIds != null) {
            softExcludedPoiIds.addAll(requestRejectedPoiIds);
        }
        if (userId != null) {
            softExcludedPoiIds.addAll(userChoiceHistoryApplicationService.recentPoiIds(userId));
            softExcludedPoiIds.addAll(userRecommendationFeedbackApplicationService.recentRejectedPoiIds(userId));
        }

        List<AmapPoi> preferred = collectCandidates(
                longitude,
                latitude,
                radius,
                size,
                mergeExcludedPoiIds(hardExcludedPoiIds, softExcludedPoiIds));
        if (!preferred.isEmpty() || softExcludedPoiIds.isEmpty()) {
            if (preferred.isEmpty()) {
                throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
            }
            return preferred;
        }

        List<AmapPoi> relaxed = collectCandidates(longitude, latitude, radius, size, hardExcludedPoiIds);
        if (relaxed.isEmpty()) {
            throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
        }
        return relaxed;
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
                .collect(Collectors.toSet());
    }

    private Set<String> mergeExcludedPoiIds(Set<String> hardExcludedPoiIds, Set<String> softExcludedPoiIds) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(hardExcludedPoiIds);
        merged.addAll(softExcludedPoiIds);
        return Set.copyOf(merged);
    }

    private List<AmapPoi> collectCandidates(
            double longitude,
            double latitude,
            int radius,
            int size,
            Set<String> excludedPoiIds) {
        List<AmapPoi> collected = new ArrayList<>();
        Set<String> seenPoiIds = new LinkedHashSet<>();
        long total = Long.MAX_VALUE;
        long fetchedCount = 0L;
        int page = 1;

        while (collected.size() < size && fetchedCount < total) {
            AmapClient.AmapSearchResult result = amapClient.searchNearby(longitude, latitude, radius, page, size);
            total = result.total();
            fetchedCount += result.items().size();
            for (AmapPoi item : result.items()) {
                if (excludedPoiIds.contains(item.poiId()) || !seenPoiIds.add(item.poiId())) {
                    continue;
                }
                collected.add(item);
                if (collected.size() >= size) {
                    break;
                }
            }
            if (result.items().isEmpty()) {
                break;
            }
            page++;
        }

        return List.copyOf(collected);
    }
}
