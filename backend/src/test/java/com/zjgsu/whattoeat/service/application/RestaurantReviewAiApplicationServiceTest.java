package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.integration.ai.AiAssistantClient;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class RestaurantReviewAiApplicationServiceTest {

    @Test
    void refreshTagsForPoiShouldWriteAiTagsAndSummary() {
        RestaurantReviewRepository reviewRepository = mock(RestaurantReviewRepository.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        AiAssistantClient aiAssistantClient = mock(AiAssistantClient.class);
        RestaurantReviewAiApplicationService service = new RestaurantReviewAiApplicationService(
                reviewRepository,
                snapshotRepository,
                aiAssistantClient);
        RestaurantMetricSnapshotEntity snapshot = new RestaurantMetricSnapshotEntity();
        snapshot.setPoiId("poi-1");
        snapshot.setReviewCount(3);
        snapshot.setAiStatus("pending");
        RestaurantReviewEntity review = review("poi-1", "兰州拉面", "汤底稳，性价比不错");

        when(reviewRepository.findByPoiIdOrderByUpdatedAtDescIdDesc("poi-1")).thenReturn(List.of(review));
        when(snapshotRepository.findById("poi-1")).thenReturn(Optional.of(snapshot));
        when(aiAssistantClient.summarizeReviewTags(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AiAssistantClient.ReviewTagResult("性价比高", "汤底稳", "评论普遍提到性价比高，汤底表现稳定。"));
        when(snapshotRepository.save(snapshot)).thenReturn(snapshot);

        service.refreshTagsForPoi("poi-1");

        assertEquals("性价比高", snapshot.getAiTag1());
        assertEquals("汤底稳", snapshot.getAiTag2());
        assertEquals("评论普遍提到性价比高，汤底表现稳定。", snapshot.getAiSummary());
        assertEquals("ready", snapshot.getAiStatus());
    }

    @Test
    void refreshTagsForPoiShouldClearTagsWhenNoReviewsRemain() {
        RestaurantReviewRepository reviewRepository = mock(RestaurantReviewRepository.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        AiAssistantClient aiAssistantClient = mock(AiAssistantClient.class);
        RestaurantReviewAiApplicationService service = new RestaurantReviewAiApplicationService(
                reviewRepository,
                snapshotRepository,
                aiAssistantClient);
        RestaurantMetricSnapshotEntity snapshot = new RestaurantMetricSnapshotEntity();
        snapshot.setPoiId("poi-2");
        snapshot.setAiTag1("出餐快");
        snapshot.setAiTag2("学生友好");
        snapshot.setAiSummary("旧摘要");
        snapshot.setAiStatus("ready");

        when(reviewRepository.findByPoiIdOrderByUpdatedAtDescIdDesc("poi-2")).thenReturn(List.of());
        when(snapshotRepository.findById("poi-2")).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.save(snapshot)).thenReturn(snapshot);

        service.refreshTagsForPoi("poi-2");

        assertNull(snapshot.getAiTag1());
        assertNull(snapshot.getAiTag2());
        assertNull(snapshot.getAiSummary());
        assertEquals("idle", snapshot.getAiStatus());
    }

    @Test
    void refreshTagsForPoiShouldClearStaleSummaryWhenAiRefreshFails() {
        RestaurantReviewRepository reviewRepository = mock(RestaurantReviewRepository.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        AiAssistantClient aiAssistantClient = mock(AiAssistantClient.class);
        RestaurantReviewAiApplicationService service = new RestaurantReviewAiApplicationService(
                reviewRepository,
                snapshotRepository,
                aiAssistantClient);
        RestaurantMetricSnapshotEntity snapshot = new RestaurantMetricSnapshotEntity();
        snapshot.setPoiId("poi-3");
        snapshot.setAiTag1("旧标签1");
        snapshot.setAiTag2("旧标签2");
        snapshot.setAiSummary("旧摘要");
        snapshot.setAiStatus("ready");
        snapshot.setLastAiGeneratedAt(LocalDateTime.of(2026, 4, 17, 12, 45));
        RestaurantReviewEntity review = review("poi-3", "兰州拉面", "新评论内容");

        when(reviewRepository.findByPoiIdOrderByUpdatedAtDescIdDesc("poi-3")).thenReturn(List.of(review));
        when(snapshotRepository.findById("poi-3")).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.save(snapshot)).thenReturn(snapshot);
        doThrow(new RuntimeException("ai failed")).when(aiAssistantClient)
                .summarizeReviewTags(org.mockito.ArgumentMatchers.any());

        service.refreshTagsForPoi("poi-3");

        assertNull(snapshot.getAiTag1());
        assertNull(snapshot.getAiTag2());
        assertNull(snapshot.getAiSummary());
        assertNull(snapshot.getLastAiGeneratedAt());
        assertEquals("failed", snapshot.getAiStatus());
    }

    private RestaurantReviewEntity review(String poiId, String poiNameSnapshot, String content) {
        RestaurantReviewEntity entity = new RestaurantReviewEntity();
        entity.setPoiId(poiId);
        entity.setPoiNameSnapshot(poiNameSnapshot);
        entity.setContent(content);
        entity.setRatingScore(new BigDecimal("4.5"));
        entity.setPerCapitaPrice(28);
        entity.setCreatedAt(LocalDateTime.of(2026, 4, 17, 12, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 4, 17, 12, 30));
        return entity;
    }
}
