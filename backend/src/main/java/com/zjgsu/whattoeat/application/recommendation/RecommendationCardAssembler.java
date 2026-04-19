package com.zjgsu.whattoeat.application.recommendation;

import com.zjgsu.whattoeat.domain.recommendation.RecommendationInsightHeuristics;
import com.zjgsu.whattoeat.infrastructure.ai.AiAssistantClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.service.application.RestaurantMetricSnapshotViewSupport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
final class RecommendationCardAssembler {

    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;

    RecommendationCardAssembler(RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository) {
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
    }

    EnrichedRecommendationCandidates enrichCandidates(List<AmapPoi> candidates) {
        Map<String, RestaurantMetricSnapshotEntity> snapshotByPoiId = restaurantMetricSnapshotRepository.findAllById(
                        candidates.stream().map(AmapPoi::poiId).toList())
                .stream()
                .collect(Collectors.toMap(RestaurantMetricSnapshotEntity::getPoiId, snapshot -> snapshot));

        List<RecommendationApplicationService.RecommendationCandidateCard> cards = new ArrayList<>();
        List<AiAssistantClient.RecommendationCandidate> aiCandidates = new ArrayList<>();
        for (AmapPoi candidate : candidates) {
            RestaurantMetricSnapshotEntity snapshot = snapshotByPoiId.get(candidate.poiId());
            List<String> aiTags = RestaurantMetricSnapshotViewSupport.visibleAiTags(snapshot);
            String aiSummary = RestaurantMetricSnapshotViewSupport.visibleAiSummary(snapshot);
            List<String> derivedTags = RecommendationInsightHeuristics.deriveCandidateSignals(
                    candidate.category(),
                    aiTags,
                    aiSummary);
            cards.add(new RecommendationApplicationService.RecommendationCandidateCard(
                    candidate.poiId(),
                    candidate.name(),
                    candidate.address(),
                    candidate.longitude(),
                    candidate.latitude(),
                    candidate.category(),
                    candidate.distance(),
                    snapshot == null ? null : snapshot.getAvgRating(),
                    snapshot == null || snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(),
                    snapshot == null ? null : snapshot.getAvgPerCapitaPrice(),
                    aiTags,
                    null));
            aiCandidates.add(new AiAssistantClient.RecommendationCandidate(
                    candidate.poiId(),
                    candidate.name(),
                    candidate.address(),
                    candidate.category(),
                    candidate.distance(),
                    snapshot == null ? null : snapshot.getAvgRating(),
                    snapshot == null || snapshot.getReviewCount() == null ? 0 : snapshot.getReviewCount(),
                    snapshot == null ? null : snapshot.getAvgPerCapitaPrice(),
                    aiTags,
                    aiSummary,
                    derivedTags));
        }
        return new EnrichedRecommendationCandidates(List.copyOf(cards), List.copyOf(aiCandidates));
    }

    List<RecommendationApplicationService.RecommendationCandidateCard> orderRecommendations(
            List<RecommendationApplicationService.RecommendationCandidateCard> candidates,
            List<AiAssistantClient.RecommendationChoice> choices,
            int size) {
        Map<String, RecommendationApplicationService.RecommendationCandidateCard> candidateByPoiId = candidates.stream()
                .collect(Collectors.toMap(RecommendationApplicationService.RecommendationCandidateCard::poiId, candidate -> candidate));
        List<RecommendationApplicationService.RecommendationCandidateCard> ordered = new ArrayList<>();
        if (choices != null) {
            for (AiAssistantClient.RecommendationChoice choice : choices) {
                RecommendationApplicationService.RecommendationCandidateCard candidate = candidateByPoiId.remove(choice.poiId());
                if (candidate != null) {
                    ordered.add(candidate.withMatchReason(choice.reason()));
                }
            }
        }
        if (ordered.size() < size) {
            candidates.stream()
                    .filter(candidate -> candidateByPoiId.containsKey(candidate.poiId()))
                    .limit(size - ordered.size())
                    .forEach(ordered::add);
        }
        if (ordered.size() <= size) {
            return List.copyOf(ordered);
        }
        return List.copyOf(ordered.subList(0, size));
    }

    @SuppressWarnings("unchecked")
    void emitRecommendationCard(
            Map<String, Object> upstreamData,
            Map<String, RecommendationApplicationService.RecommendationCandidateCard> candidateByPoiId,
            int maxCards,
            AtomicInteger emittedCardCount,
            Set<String> emittedPoiIds,
            Consumer<RecommendationApplicationService.AskStreamEvent> eventConsumer) {
        Object toolName = upstreamData.get("toolName");
        if (!(toolName instanceof String tool) || !"show_restaurant_card".equals(tool)) {
            return;
        }
        Object argumentsObject = upstreamData.get("arguments");
        if (!(argumentsObject instanceof Map<?, ?> rawArguments)) {
            return;
        }
        Map<String, Object> arguments = (Map<String, Object>) rawArguments;
        Object poiIdValue = arguments.get("poiId");
        if (!(poiIdValue instanceof String poiId)) {
            return;
        }
        RecommendationApplicationService.RecommendationCandidateCard candidate = candidateByPoiId.get(poiId);
        if (candidate == null || emittedPoiIds.contains(poiId)) {
            return;
        }
        int fallbackRank = emittedCardCount.get() + 1;
        if (fallbackRank > maxCards) {
            return;
        }
        int rank = fallbackRank;
        Object rankValue = arguments.get("rank");
        if (rankValue instanceof Number number) {
            int providedRank = number.intValue();
            if (providedRank > 0 && providedRank <= maxCards) {
                rank = providedRank;
            }
        }
        emittedPoiIds.add(poiId);
        emittedCardCount.incrementAndGet();
        String reason = arguments.get("reason") instanceof String text ? text : candidate.matchReason();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rank", rank);
        payload.put("poiId", candidate.poiId());
        payload.put("name", candidate.name());
        payload.put("address", candidate.address());
        payload.put("category", candidate.category());
        payload.put("distance", candidate.distance());
        payload.put("avgRating", candidate.avgRating());
        payload.put("reviewCount", candidate.reviewCount());
        payload.put("avgPerCapitaPrice", candidate.avgPerCapitaPrice());
        payload.put("aiTags", candidate.aiTags());
        payload.put("matchReason", reason);
        eventConsumer.accept(new RecommendationApplicationService.AskStreamEvent("recommendation.card", payload));
    }

    record EnrichedRecommendationCandidates(
            List<RecommendationApplicationService.RecommendationCandidateCard> cards,
            List<AiAssistantClient.RecommendationCandidate> aiCandidates) {
    }
}
