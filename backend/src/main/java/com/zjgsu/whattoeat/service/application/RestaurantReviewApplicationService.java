package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RestaurantReviewApplicationService {

    private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
    private static final BigDecimal HALF_STEP_MULTIPLIER = new BigDecimal("2");
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final UserRepository userRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final RestaurantMetricAggregationService restaurantMetricAggregationService;
    private final RestaurantReviewAiApplicationService restaurantReviewAiApplicationService;

    public RestaurantReviewApplicationService(
            UserRepository userRepository,
            RestaurantReviewRepository restaurantReviewRepository,
            RestaurantMetricAggregationService restaurantMetricAggregationService,
            RestaurantReviewAiApplicationService restaurantReviewAiApplicationService) {
        this.userRepository = userRepository;
        this.restaurantReviewRepository = restaurantReviewRepository;
        this.restaurantMetricAggregationService = restaurantMetricAggregationService;
        this.restaurantReviewAiApplicationService = restaurantReviewAiApplicationService;
    }

    @Transactional(readOnly = true)
    public ReviewDetail getUserReview(Long userId, String poiId) {
        validateUserExists(userId);
        RestaurantReviewEntity entity = restaurantReviewRepository.findByUserIdAndPoiId(userId, poiId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        return toReviewDetail(entity);
    }

    @Transactional
    public ReviewDetail upsertReview(
            Long userId,
            String poiId,
            String poiNameSnapshot,
            BigDecimal ratingScore,
            Integer perCapitaPrice,
            String content) {
        validateUserExists(userId);
        validateRating(ratingScore);
        validatePerCapitaPrice(perCapitaPrice);
        validateContent(content);

        RestaurantReviewEntity entity = restaurantReviewRepository.findByUserIdAndPoiId(userId, poiId)
                .orElseGet(RestaurantReviewEntity::new);
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiNameSnapshot(normalizePoiNameSnapshot(poiNameSnapshot));
        entity.setRatingScore(ratingScore.stripTrailingZeros().scale() < 1 ? ratingScore.setScale(1) : ratingScore);
        entity.setPerCapitaPrice(perCapitaPrice);
        entity.setContent(content.trim());

        RestaurantReviewEntity saved = restaurantReviewRepository.saveAndFlush(entity);
        restaurantMetricAggregationService.refreshSnapshot(poiId);
        restaurantReviewAiApplicationService.refreshTagsForPoi(poiId);
        return toReviewDetail(saved);
    }

    @Transactional
    public void deleteReview(Long userId, String poiId) {
        validateUserExists(userId);
        RestaurantReviewEntity entity = restaurantReviewRepository.findByUserIdAndPoiId(userId, poiId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        restaurantReviewRepository.delete(entity);
        restaurantReviewRepository.flush();
        restaurantMetricAggregationService.refreshSnapshot(poiId);
        restaurantReviewAiApplicationService.refreshTagsForPoi(poiId);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateRating(BigDecimal ratingScore) {
        if (ratingScore == null
                || ratingScore.compareTo(MIN_RATING) < 0
                || ratingScore.compareTo(MAX_RATING) > 0
                || ratingScore.multiply(HALF_STEP_MULTIPLIER).stripTrailingZeros().scale() > 0) {
            throw new BusinessException(ErrorCode.REVIEW_RATING_INVALID);
        }
    }

    private void validatePerCapitaPrice(Integer perCapitaPrice) {
        if (perCapitaPrice == null || perCapitaPrice <= 0) {
            throw new BusinessException(ErrorCode.REVIEW_PRICE_INVALID);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.REVIEW_CONTENT_INVALID);
        }
    }

    private String normalizePoiNameSnapshot(String poiNameSnapshot) {
        if (poiNameSnapshot == null) {
            return null;
        }
        String trimmed = poiNameSnapshot.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewDetail toReviewDetail(RestaurantReviewEntity entity) {
        return new ReviewDetail(
                entity.getPoiId(),
                entity.getPoiNameSnapshot(),
                entity.getRatingScore(),
                entity.getPerCapitaPrice(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public record ReviewDetail(
            String poiId,
            String poiNameSnapshot,
            BigDecimal ratingScore,
            Integer perCapitaPrice,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
