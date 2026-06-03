package com.zjgsu.whattoeat.service.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RestaurantMetricSnapshotLookupTest {

    @Test
    void findByPoiIdsShouldReturnCachedSnapshotsWithoutQueryingRepository() {
        RestaurantMetricSnapshotRepository repository = mock(RestaurantMetricSnapshotRepository.class);
        Cache<String, RestaurantMetricSnapshotEntity> cache = Caffeine.newBuilder().maximumSize(100).build();
        RestaurantMetricSnapshotEntity cached = snapshot("poi-cached", "4.8");
        cache.put("poi-cached", cached);
        RestaurantMetricSnapshotLookup lookup = new RestaurantMetricSnapshotLookup(repository, cache);

        Map<String, RestaurantMetricSnapshotEntity> result = lookup.findByPoiIds(List.of("poi-cached"));

        assertSame(cached, result.get("poi-cached"));
        verifyNoInteractions(repository);
    }

    @Test
    void findByPoiIdsShouldQueryOnlyMissedSnapshotsAndBackfillCache() {
        RestaurantMetricSnapshotRepository repository = mock(RestaurantMetricSnapshotRepository.class);
        Cache<String, RestaurantMetricSnapshotEntity> cache = Caffeine.newBuilder().maximumSize(100).build();
        RestaurantMetricSnapshotEntity cached = snapshot("poi-cached", "4.2");
        RestaurantMetricSnapshotEntity missed = snapshot("poi-missed", "4.9");
        cache.put("poi-cached", cached);
        when(repository.findAllById(List.of("poi-missed"))).thenReturn(List.of(missed));
        RestaurantMetricSnapshotLookup lookup = new RestaurantMetricSnapshotLookup(repository, cache);

        Map<String, RestaurantMetricSnapshotEntity> result =
                lookup.findByPoiIds(List.of("poi-cached", "poi-missed", "poi-cached"));

        assertEquals(2, result.size());
        assertSame(cached, result.get("poi-cached"));
        assertSame(missed, result.get("poi-missed"));
        assertSame(missed, cache.getIfPresent("poi-missed"));
        verify(repository).findAllById(List.of("poi-missed"));
    }

    private RestaurantMetricSnapshotEntity snapshot(String poiId, String avgRating) {
        RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
        entity.setPoiId(poiId);
        entity.setAvgRating(new BigDecimal(avgRating));
        entity.setReviewCount(1);
        entity.setAiStatus("ready");
        return entity;
    }
}
