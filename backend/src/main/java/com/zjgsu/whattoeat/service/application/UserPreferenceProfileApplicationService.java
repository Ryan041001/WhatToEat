package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import com.zjgsu.whattoeat.model.entity.UserRestaurantNoteEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.repository.UserRestaurantNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserPreferenceProfileApplicationService {

    private final UserRepository userRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final UserRestaurantNoteRepository userRestaurantNoteRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final UserChoiceHistoryRepository userChoiceHistoryRepository;
    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService;

    public UserPreferenceProfileApplicationService(
            UserRepository userRepository,
            RestaurantReviewRepository restaurantReviewRepository,
            UserRestaurantNoteRepository userRestaurantNoteRepository,
            UserBlacklistRepository userBlacklistRepository,
            UserChoiceHistoryRepository userChoiceHistoryRepository,
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService) {
        this.userRepository = userRepository;
        this.restaurantReviewRepository = restaurantReviewRepository;
        this.userRestaurantNoteRepository = userRestaurantNoteRepository;
        this.userBlacklistRepository = userBlacklistRepository;
        this.userChoiceHistoryRepository = userChoiceHistoryRepository;
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.userRecommendationFeedbackApplicationService = userRecommendationFeedbackApplicationService;
    }

    @Transactional(readOnly = true)
    public PreferenceProfile getPreferenceProfile(Long userId) {
        validateUserExists(userId);
        return buildProfile(userId);
    }

    @Transactional(readOnly = true)
    public PreferenceProfile buildProfile(Long userId) {
        validateUserExists(userId);
        List<RestaurantReviewEntity> reviews = restaurantReviewRepository.findByUserIdOrderByUpdatedAtDescIdDesc(userId);
        List<UserRestaurantNoteEntity> notes = userRestaurantNoteRepository.findByUserId(userId);
        List<UserBlacklistEntity> blacklists = userBlacklistRepository.findByUserId(userId);
        List<UserChoiceHistoryEntity> choiceHistory = userChoiceHistoryRepository.findByUserIdOrderByChosenAtDesc(userId);
        List<RecommendationFeedbackEntity> feedback = userRecommendationFeedbackApplicationService.allFeedback(userId);

        Map<String, RestaurantMetricSnapshotEntity> snapshotByPoiId = restaurantMetricSnapshotRepository.findAllById(
                        choiceHistory.stream().map(UserChoiceHistoryEntity::getPoiId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(RestaurantMetricSnapshotEntity::getPoiId, snapshot -> snapshot));

        List<String> positiveCorpus = new ArrayList<>();
        positiveCorpus.addAll(reviews.stream().map(RestaurantReviewEntity::getContent).toList());
        positiveCorpus.addAll(notes.stream().map(UserRestaurantNoteEntity::getNote).toList());
        positiveCorpus.addAll(choiceHistory.stream()
                .map(item -> snapshotByPoiId.get(item.getPoiId()))
                .filter(snapshot -> snapshot != null)
                .flatMap(snapshot -> Stream.of(snapshot.getAiTag1(), snapshot.getAiTag2(), snapshot.getAiSummary()))
                .toList());

        List<String> negativeCorpus = new ArrayList<>();
        negativeCorpus.addAll(blacklists.stream().map(UserBlacklistEntity::getReason).toList());
        negativeCorpus.addAll(feedback.stream()
                .flatMap(item -> Stream.of(item.getDetail(), item.getFeedbackType()))
                .toList());

        List<String> preferredTags = RecommendationInsightHeuristics.derivePreferredTags(positiveCorpus);
        List<String> avoidedTags = RecommendationInsightHeuristics.deriveAvoidedTags(negativeCorpus);
        List<String> recentFeedbackSignals = userRecommendationFeedbackApplicationService.recentFeedbackSignals(userId);
        List<Integer> pricePoints = new ArrayList<>();
        pricePoints.addAll(reviews.stream().map(RestaurantReviewEntity::getPerCapitaPrice).toList());
        pricePoints.addAll(choiceHistory.stream()
                .map(item -> snapshotByPoiId.get(item.getPoiId()))
                .filter(snapshot -> snapshot != null && snapshot.getAvgPerCapitaPrice() != null)
                .map(RestaurantMetricSnapshotEntity::getAvgPerCapitaPrice)
                .toList());

        Integer avgBudget = pricePoints.isEmpty()
                ? null
                : (int) Math.round(pricePoints.stream().mapToInt(Integer::intValue).average().orElse(0));
        BudgetRange budgetRange = pricePoints.isEmpty()
                ? new BudgetRange(null, null)
                : new BudgetRange(pricePoints.stream().min(Integer::compareTo).orElse(null),
                        pricePoints.stream().max(Integer::compareTo).orElse(null));

        List<String> lifestyleSignals = new ArrayList<>();
        if (preferredTags.contains("清淡") || preferredTags.contains("高蛋白")) {
            lifestyleSignals.add("健身期更友好");
        }
        if (preferredTags.contains("出餐快")) {
            lifestyleSignals.add("工作日午餐优先");
        }
        if (preferredTags.contains("适合久坐")) {
            lifestyleSignals.add("更适合久坐/聊天");
        }

        String summary = summarize(preferredTags, avoidedTags, recentFeedbackSignals);
        return new PreferenceProfile(
                summary,
                preferredTags,
                avoidedTags,
                avgBudget,
                budgetRange,
                choiceHistory.size(),
                reviews.size(),
                blacklists.size(),
                List.copyOf(new LinkedHashSet<>(lifestyleSignals)),
                recentFeedbackSignals);
    }

    private String summarize(List<String> preferredTags, List<String> avoidedTags, List<String> recentFeedbackSignals) {
        StringBuilder builder = new StringBuilder();
        if (!preferredTags.isEmpty()) {
            builder.append("你最近更偏向").append(String.join("、", preferredTags.stream().limit(3).toList())).append("的选择");
        }
        if (!avoidedTags.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("，");
            }
            builder.append("也会主动避开").append(String.join("、", avoidedTags.stream().limit(2).toList()));
        }
        if (!recentFeedbackSignals.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("，");
            }
            builder.append("最近还表现出").append(String.join("、", recentFeedbackSignals.stream().limit(2).toList()));
        }
        if (builder.length() == 0) {
            return "当前偏好信号还不够多，先继续通过评论、备注、选择记录和反馈积累画像。";
        }
        builder.append("。");
        return builder.toString();
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    public record PreferenceProfile(
            String summary,
            List<String> preferredTags,
            List<String> avoidedTags,
            Integer avgPerCapitaBudget,
            BudgetRange budgetRange,
            int recentChoiceCount,
            int reviewCount,
            int blacklistCount,
            List<String> lifestyleSignals,
            List<String> recentFeedbackSignals) {
    }

    public record BudgetRange(Integer min, Integer max) {
    }
}
