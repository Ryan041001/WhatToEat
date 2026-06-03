package com.zjgsu.whattoeat.application.recommendation;

import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.service.application.RestaurantMetricSnapshotLookup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationCardAssemblerTest {

    @Test
    void enrichCandidatesShouldUseSharedSnapshotLookup() {
        RestaurantMetricSnapshotLookup snapshotLookup = mock(RestaurantMetricSnapshotLookup.class);
        RecommendationCardAssembler assembler = new RecommendationCardAssembler(snapshotLookup);
        RestaurantMetricSnapshotEntity snapshot = snapshot("poi-1", "4.7", 28, 35, "出餐快", "适合午餐");
        when(snapshotLookup.findByPoiIds(List.of("poi-1"))).thenReturn(Map.of("poi-1", snapshot));

        RecommendationCardAssembler.EnrichedRecommendationCandidates enriched = assembler.enrichCandidates(List.of(
                new AmapPoi("poi-1", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮;中餐厅;面馆", 220)));

        assertEquals(1, enriched.cards().size());
        assertEquals(new BigDecimal("4.7"), enriched.cards().get(0).avgRating());
        assertEquals(28, enriched.cards().get(0).reviewCount());
        assertEquals(35, enriched.cards().get(0).avgPerCapitaPrice());
        assertEquals(List.of("出餐快", "适合午餐"), enriched.cards().get(0).aiTags());
        verify(snapshotLookup).findByPoiIds(List.of("poi-1"));
    }

    @Test
    void emitRecommendationCardShouldFallbackToSequentialRankWhenUpstreamRankIsOutOfRange() {
        RecommendationCardAssembler assembler = new RecommendationCardAssembler(mock(RestaurantMetricSnapshotLookup.class));
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
        RecommendationCardAssembler assembler = new RecommendationCardAssembler(mock(RestaurantMetricSnapshotLookup.class));
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

    private RestaurantMetricSnapshotEntity snapshot(
            String poiId,
            String avgRating,
            int reviewCount,
            Integer avgPerCapitaPrice,
            String aiTag1,
            String aiTag2) {
        RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
        entity.setPoiId(poiId);
        entity.setAvgRating(new BigDecimal(avgRating));
        entity.setReviewCount(reviewCount);
        entity.setAvgPerCapitaPrice(avgPerCapitaPrice);
        entity.setAiTag1(aiTag1);
        entity.setAiTag2(aiTag2);
        entity.setAiStatus("ready");
        return entity;
    }
}
