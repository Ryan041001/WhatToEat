package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;

import java.util.List;
import java.util.stream.Stream;

final class RestaurantMetricSnapshotViewSupport {

    private RestaurantMetricSnapshotViewSupport() {
    }

    static boolean isAiReady(RestaurantMetricSnapshotEntity snapshot) {
        return snapshot != null && "ready".equalsIgnoreCase(snapshot.getAiStatus());
    }

    static List<String> visibleAiTags(RestaurantMetricSnapshotEntity snapshot) {
        if (!isAiReady(snapshot)) {
            return List.of();
        }
        return Stream.of(snapshot.getAiTag1(), snapshot.getAiTag2())
                .filter(tag -> tag != null && !tag.isBlank())
                .toList();
    }

    static String visibleAiSummary(RestaurantMetricSnapshotEntity snapshot) {
        return isAiReady(snapshot) ? snapshot.getAiSummary() : null;
    }
}
