package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.domain.recommendation.RecommendationInsightHeuristics;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RestaurantReviewQueryApplicationService {

    private final RestaurantReviewRepository restaurantReviewRepository;
    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final UserRepository userRepository;

    public RestaurantReviewQueryApplicationService(
            RestaurantReviewRepository restaurantReviewRepository,
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            UserRepository userRepository) {
        this.restaurantReviewRepository = restaurantReviewRepository;
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ReviewPage listPublicReviews(String poiId, int page, int size) {
        Page<RestaurantReviewEntity> result = restaurantReviewRepository.findByPoiIdOrderByUpdatedAtDescIdDesc(
                poiId,
                PageRequest.of(page - 1, size));
        Map<Long, String> nicknameByUserId = loadNicknames(result.getContent());
        List<PublicReviewItem> items = result.getContent().stream()
                .map(review -> new PublicReviewItem(
                        review.getId(),
                        review.getUserId(),
                        nicknameByUserId.getOrDefault(review.getUserId(), "匿名用户"),
                        review.getRatingScore(),
                        review.getPerCapitaPrice(),
                        review.getContent(),
                        review.getUpdatedAt()))
                .toList();
        return new ReviewPage(items, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReviewSummary getReviewSummary(String poiId) {
        return restaurantMetricSnapshotRepository.findById(poiId)
                .map(this::toReviewSummary)
                .orElseGet(() -> new ReviewSummary(0, null, null, List.of(), null, List.of()));
    }

    private Map<Long, String> loadNicknames(List<RestaurantReviewEntity> reviews) {
        if (reviews.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(reviews.stream().map(RestaurantReviewEntity::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> {
                    String nickname = user.getNickname();
                    return nickname == null || nickname.isBlank() ? "匿名用户" : nickname;
                }));
    }

    private ReviewSummary toReviewSummary(RestaurantMetricSnapshotEntity snapshot) {
        List<String> aiTags = RestaurantMetricSnapshotViewSupport.visibleAiTags(snapshot);
        String aiSummary = RestaurantMetricSnapshotViewSupport.visibleAiSummary(snapshot);
        List<String> recommendedScenarios = RestaurantMetricSnapshotViewSupport.isAiReady(snapshot)
                ? RecommendationInsightHeuristics.deriveRecommendedScenarios(
                snapshot.getAvgPerCapitaPrice(),
                aiTags,
                aiSummary,
                List.of())
                : List.of();
        return new ReviewSummary(
                snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(),
                snapshot.getAvgRating(),
                snapshot.getAvgPerCapitaPrice(),
                aiTags,
                aiSummary,
                recommendedScenarios);
    }

    public record ReviewPage(List<PublicReviewItem> items, int page, int size, long total) {
    }

    public record PublicReviewItem(
            Long id,
            Long userId,
            String nickname,
            BigDecimal ratingScore,
            Integer perCapitaPrice,
            String content,
            LocalDateTime updatedAt) {
    }

    public record ReviewSummary(
            int reviewCount,
            BigDecimal avgRating,
            Integer avgPerCapitaPrice,
            List<String> aiTags,
            String aiSummary,
            List<String> recommendedScenarios) {
    }
}
