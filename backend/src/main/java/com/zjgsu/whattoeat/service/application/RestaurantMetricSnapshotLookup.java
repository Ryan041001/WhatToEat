package com.zjgsu.whattoeat.service.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RestaurantMetricSnapshotLookup {

    private final RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;
    private final Cache<String, RestaurantMetricSnapshotEntity> snapshotCache;

    public RestaurantMetricSnapshotLookup(
            RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository,
            Cache<String, RestaurantMetricSnapshotEntity> snapshotCache) {
        this.restaurantMetricSnapshotRepository = restaurantMetricSnapshotRepository;
        this.snapshotCache = snapshotCache;
    }

    public Map<String, RestaurantMetricSnapshotEntity> findByPoiIds(List<String> poiIds) {
        Map<String, RestaurantMetricSnapshotEntity> result = new HashMap<>();
        List<String> missedIds = new ArrayList<>();
        Set<String> seenMissedIds = new LinkedHashSet<>();

        for (String poiId : poiIds) {
            if (poiId == null || poiId.isBlank()) {
                continue;
            }
            RestaurantMetricSnapshotEntity cached = snapshotCache.getIfPresent(poiId);
            if (cached != null) {
                result.put(poiId, cached);
                continue;
            }
            if (seenMissedIds.add(poiId)) {
                missedIds.add(poiId);
            }
        }

        if (!missedIds.isEmpty()) {
            List<RestaurantMetricSnapshotEntity> fromDb = restaurantMetricSnapshotRepository.findAllById(missedIds);
            for (RestaurantMetricSnapshotEntity snapshot : fromDb) {
                if (snapshot.getPoiId() == null || snapshot.getPoiId().isBlank()) {
                    continue;
                }
                snapshotCache.put(snapshot.getPoiId(), snapshot);
                result.put(snapshot.getPoiId(), snapshot);
            }
        }

        return result;
    }
}
