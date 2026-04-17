package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.integration.ai.AiAssistantClient;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestaurantReviewAiApplicationService {

    private final RestaurantReviewRepository restaurantReviewRepository;
    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final AiAssistantClient aiAssistantClient;

    public RestaurantReviewAiApplicationService(
            RestaurantReviewRepository restaurantReviewRepository,
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            AiAssistantClient aiAssistantClient) {
        this.restaurantReviewRepository = restaurantReviewRepository;
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.aiAssistantClient = aiAssistantClient;
    }

    @Transactional
    public void refreshTagsForPoi(String poiId) {
        List<RestaurantReviewEntity> reviews = restaurantReviewRepository.findByPoiIdOrderByUpdatedAtDescIdDesc(poiId);
        RestaurantMetricSnapshotEntity snapshot = restaurantMetricSnapshotRepository.findById(poiId)
                .orElseGet(() -> {
                    RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
                    entity.setPoiId(poiId);
                    entity.setReviewCount(0);
                    entity.setAiStatus("idle");
                    return entity;
                });

        if (reviews.isEmpty()) {
            clearAiFields(snapshot);
            snapshot.setAiStatus("idle");
            restaurantMetricSnapshotRepository.save(snapshot);
            return;
        }

        RestaurantReviewEntity latestReview = reviews.get(0);
        try {
            AiAssistantClient.ReviewTagResult result = aiAssistantClient.summarizeReviewTags(
                    new AiAssistantClient.ReviewTagRequest(
                            poiId,
                            latestReview.getPoiNameSnapshot(),
                            reviews.stream()
                                    .map(review -> new AiAssistantClient.ReviewText(
                                            review.getContent(),
                                            review.getRatingScore(),
                                            review.getPerCapitaPrice()))
                                    .toList()));
            snapshot.setAiTag1(normalizeTag(result.tag1()));
            snapshot.setAiTag2(normalizeTag(result.tag2()));
            snapshot.setAiSummary(normalizeSummary(result.summary()));
            snapshot.setAiStatus("ready");
            snapshot.setLastAiGeneratedAt(LocalDateTime.now());
        } catch (RuntimeException ex) {
            clearAiFields(snapshot);
            snapshot.setAiStatus("failed");
        }
        restaurantMetricSnapshotRepository.save(snapshot);
    }

    private void clearAiFields(RestaurantMetricSnapshotEntity snapshot) {
        snapshot.setAiTag1(null);
        snapshot.setAiTag2(null);
        snapshot.setAiSummary(null);
        snapshot.setLastAiGeneratedAt(null);
    }

    private String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        String trimmed = tag.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSummary(String summary) {
        if (summary == null) {
            return null;
        }
        String trimmed = summary.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
