package com.zjgsu.whattoeat.application.recommendation;

import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RecommendationCardAssemblerTest {

    @Test
    void emitRecommendationCardShouldFallbackToSequentialRankWhenUpstreamRankIsOutOfRange() {
        RecommendationCardAssembler assembler = new RecommendationCardAssembler(mock(RestaurantMetricSnapshotRepository.class));
        RecommendationApplicationService.RecommendationCandidateCard candidate =
                new RecommendationApplicationService.RecommendationCandidateCard(
                        "poi-noodle",
                        "兰州拉面",
                        "文泽路",
                        120.36,
                        30.32,
                        "餐饮",
                        220,
                        null,
                        0,
                        null,
                        java.util.List.of("汤底稳"),
                        null);

        Map<String, RecommendationApplicationService.RecommendationCandidateCard> candidateByPoiId = new HashMap<>();
        candidateByPoiId.put(candidate.poiId(), candidate);
        java.util.List<RecommendationApplicationService.AskStreamEvent> emittedEvents = new java.util.ArrayList<>();

        assembler.emitRecommendationCard(
                Map.of(
                        "toolName", "show_restaurant_card",
                        "arguments", new LinkedHashMap<>(Map.of(
                                "poiId", "poi-noodle",
                                "reason", "更贴近热汤需求",
                                "rank", 99))),
                candidateByPoiId,
                3,
                new AtomicInteger(),
                new java.util.HashSet<>(),
                emittedEvents::add);

        assertEquals(1, emittedEvents.size());
        assertEquals("recommendation.card", emittedEvents.get(0).name());
        assertEquals(1, emittedEvents.get(0).data().get("rank"));
    }

    @Test
    void emitRecommendationCardShouldStopAfterMaxCardsReached() {
        RecommendationCardAssembler assembler = new RecommendationCardAssembler(mock(RestaurantMetricSnapshotRepository.class));
        Map<String, RecommendationApplicationService.RecommendationCandidateCard> candidateByPoiId = new HashMap<>();
        candidateByPoiId.put("poi-1", candidate("poi-1", "兰州拉面"));
        candidateByPoiId.put("poi-2", candidate("poi-2", "桂香卤味拌饭"));

        AtomicInteger emittedCardCount = new AtomicInteger(1);
        Set<String> emittedPoiIds = new java.util.HashSet<>(Set.of("poi-1"));
        java.util.List<RecommendationApplicationService.AskStreamEvent> emittedEvents = new java.util.ArrayList<>();

        assembler.emitRecommendationCard(
                Map.of(
                        "toolName", "show_restaurant_card",
                        "arguments", new LinkedHashMap<>(Map.of(
                                "poiId", "poi-2",
                                "reason", "离得近",
                                "rank", 2))),
                candidateByPoiId,
                1,
                emittedCardCount,
                emittedPoiIds,
                emittedEvents::add);

        assertTrue(emittedEvents.isEmpty());
        assertEquals(1, emittedCardCount.get());
        assertEquals(Set.of("poi-1"), emittedPoiIds);
    }

    private RecommendationApplicationService.RecommendationCandidateCard candidate(String poiId, String name) {
        return new RecommendationApplicationService.RecommendationCandidateCard(
                poiId,
                name,
                "学林街",
                120.35,
                30.31,
                "餐饮",
                180,
                null,
                0,
                null,
                java.util.List.of(),
                null);
    }
}
