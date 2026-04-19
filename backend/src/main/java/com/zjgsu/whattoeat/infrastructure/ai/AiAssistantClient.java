package com.zjgsu.whattoeat.infrastructure.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AiAssistantClient {

    ReviewTagResult summarizeReviewTags(ReviewTagRequest request);

    RecommendationAdvice recommend(RecommendationRequest request);

    void streamRecommend(RecommendationRequest request, Consumer<RecommendationStreamEvent> eventConsumer);

    record ReviewTagRequest(String poiId, String poiName, List<ReviewText> reviews) {
    }

    record ReviewText(String content, BigDecimal ratingScore, Integer perCapitaPrice) {
    }

    record ReviewTagResult(String tag1, String tag2, String summary) {
    }

    record RecommendationRequest(String question, List<RecommendationCandidate> candidates, RecommendationContext context) {
    }

    record RecommendationCandidate(
            String poiId,
            String name,
            String address,
            String category,
            double distance,
            BigDecimal avgRating,
            int reviewCount,
            Integer avgPerCapitaPrice,
            List<String> aiTags,
            String aiSummary,
            List<String> derivedTags) {
    }

    record RecommendationContext(
            String previousQuestion,
            List<String> rejectedPoiIds,
            List<String> selectedPoiIds,
            List<String> userSignals,
            String temporalContext,
            String preferenceProfileSummary,
            List<String> preferredTags,
            List<String> avoidedTags,
            List<String> recentFeedbackSignals) {
    }

    record RecommendationAdvice(String answer, List<RecommendationChoice> choices) {
    }

    record RecommendationChoice(String poiId, String reason) {
    }

    record RecommendationStreamEvent(String name, Map<String, Object> data) {
    }
}
