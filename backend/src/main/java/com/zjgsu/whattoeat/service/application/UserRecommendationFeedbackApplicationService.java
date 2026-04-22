package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.domain.recommendation.RecommendationInsightHeuristics;
import com.zjgsu.whattoeat.model.RecommendationFeedbackType;
import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import com.zjgsu.whattoeat.repository.RecommendationFeedbackRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserRecommendationFeedbackApplicationService {

    static final int RECENT_FEEDBACK_DAYS = 7;

    private final Clock clock;
    private final UserRepository userRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final UserChoiceHistoryApplicationService userChoiceHistoryApplicationService;

    public UserRecommendationFeedbackApplicationService(
            Clock clock,
            UserRepository userRepository,
            RecommendationFeedbackRepository recommendationFeedbackRepository,
            UserChoiceHistoryApplicationService userChoiceHistoryApplicationService) {
        this.clock = clock;
        this.userRepository = userRepository;
        this.recommendationFeedbackRepository = recommendationFeedbackRepository;
        this.userChoiceHistoryApplicationService = userChoiceHistoryApplicationService;
    }

    @Transactional
    public void createFeedback(
            Long userId,
            String poiId,
            String poiNameSnapshot,
            RecommendationFeedbackType feedbackType,
            String detail,
            String requestQuestion) {
        validateUserExists(userId);
        RecommendationFeedbackEntity entity = new RecommendationFeedbackEntity();
        entity.setUserId(userId);
        entity.setPoiId(normalize(poiId));
        entity.setPoiNameSnapshot(normalize(poiNameSnapshot));
        entity.setFeedbackType(feedbackType.name());
        entity.setDetail(normalize(detail));
        entity.setRequestQuestion(normalize(requestQuestion));
        recommendationFeedbackRepository.save(entity);

        if (feedbackType == RecommendationFeedbackType.ALREADY_ATE && entity.getPoiId() != null) {
            userChoiceHistoryApplicationService.createChoiceHistory(userId, entity.getPoiId(), entity.getPoiNameSnapshot());
        }
    }

    @Transactional(readOnly = true)
    public FeedbackPage listFeedback(Long userId, int page, int size) {
        validateUserExists(userId);
        Page<RecommendationFeedbackEntity> result = recommendationFeedbackRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId,
                PageRequest.of(page - 1, size));
        return new FeedbackPage(
                result.getContent().stream()
                        .map(item -> new FeedbackItem(
                                item.getId(),
                                item.getPoiId(),
                                item.getPoiNameSnapshot(),
                                item.getFeedbackType(),
                                item.getDetail(),
                                item.getRequestQuestion(),
                                item.getCreatedAt()))
                        .toList(),
                page,
                size,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Set<String> recentRejectedPoiIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        validateUserExists(userId);
        return recentFeedbackEntities(userId).stream()
                .filter(item -> item.getPoiId() != null)
                .filter(item -> RecommendationInsightHeuristics.parseType(item.getFeedbackType()) != RecommendationFeedbackType.ALREADY_ATE)
                .map(RecommendationFeedbackEntity::getPoiId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<String> recentFeedbackSignals(Long userId) {
        if (userId == null) {
            return List.of();
        }
        validateUserExists(userId);
        return RecommendationInsightHeuristics.deriveRecentFeedbackSignals(recentFeedbackEntities(userId));
    }

    @Transactional(readOnly = true)
    public List<RecommendationFeedbackEntity> allFeedback(Long userId) {
        if (userId == null) {
            return List.of();
        }
        validateUserExists(userId);
        return recommendationFeedbackRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId);
    }

    private List<RecommendationFeedbackEntity> recentFeedbackEntities(Long userId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(RECENT_FEEDBACK_DAYS);
        return recommendationFeedbackRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(userId, since);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record FeedbackPage(List<FeedbackItem> items, int page, int size, long total) {
    }

    public record FeedbackItem(
            Long id,
            String poiId,
            String poiNameSnapshot,
            String feedbackType,
            String detail,
            String requestQuestion,
            LocalDateTime createdAt) {
    }
}
