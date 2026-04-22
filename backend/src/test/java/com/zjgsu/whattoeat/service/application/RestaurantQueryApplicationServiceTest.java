package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestaurantQueryApplicationServiceTest {

    @Test
    void nearbyShouldReturnEmptyPageWhenTotalPositiveButItemsEmpty() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());
        when(snapshotRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(amapClient.searchNearby(120.35, 30.31, 1000, 2, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(), 35));

        RestaurantQueryApplicationService.RestaurantPage page = service.nearby(120.35, 30.31, 1000, 2, 10);

        assertEquals(2, page.page());
        assertEquals(10, page.size());
        assertEquals(35, page.total());
        assertEquals(0, page.items().size());
    }

    @Test
    void searchShouldThrowNoResultWhenTotalIsZero() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());
        when(amapClient.searchByKeyword("拉面", 120.36, 30.32, 1500, 1, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(), 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search("拉面", 120.36, 30.32, 1500, 1, 10));

        assertEquals(ErrorCode.AMAP_NO_RESULT, ex.getErrorCode());
    }

    @Test
    void nearbyShouldRecordNearbySuccessMetric() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                meterRegistry);
        AmapPoi poi = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        when(snapshotRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(amapClient.searchNearby(120.35, 30.31, 1000, 2, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(poi), 35));

        RestaurantQueryApplicationService.RestaurantPage page = service.nearby(120.35, 30.31, 1000, 2, 10);

        assertEquals(1, page.items().size());
        assertEquals(1.0, meterRegistry.get("restaurant.query.requests")
                .tag("scene", "nearby")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void searchShouldRecordNoResultMetricBeforeThrowing() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                meterRegistry);
        when(amapClient.searchByKeyword("拉面", 120.36, 30.32, 1500, 1, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(), 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search("拉面", 120.36, 30.32, 1500, 1, 10));

        assertEquals(ErrorCode.AMAP_NO_RESULT, ex.getErrorCode());
        assertEquals(1.0, meterRegistry.get("restaurant.query.requests")
                .tag("scene", "search")
                .tag("result", ErrorCode.AMAP_NO_RESULT.name())
                .counter()
                .count());
    }

    @Test
    void nearbyShouldMergeSnapshotFieldsAndSortByAvgRating() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());
        AmapPoi lowerRated = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        AmapPoi higherRated = new AmapPoi("id-2", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        when(amapClient.searchNearby(120.35, 30.31, 1000, 1, 50))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(lowerRated, higherRated), 2));
        when(snapshotRepository.findAllById(anyIterable())).thenReturn(List.of(
                snapshot("id-1", "4.2", 16, 26, "出餐快", "学生友好"),
                snapshot("id-2", "4.8", 25, 32, "性价比高", "汤底稳")));

        RestaurantQueryApplicationService.RestaurantPage page =
                service.nearby(120.35, 30.31, 1000, 1, 10, "avgRating");

        assertEquals(2, page.items().size());
        assertEquals("id-2", page.items().get(0).poiId());
        assertEquals(new BigDecimal("4.8"), page.items().get(0).avgRating());
        assertEquals(25, page.items().get(0).reviewCount());
        assertEquals(32, page.items().get(0).avgPerCapitaPrice());
        assertEquals(List.of("性价比高", "汤底稳"), page.items().get(0).aiTags());
    }

    @Test
    void nearbyShouldHideAiTagsWhenSnapshotIsNotReady() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());
        AmapPoi poi = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        when(amapClient.searchNearby(120.35, 30.31, 1000, 1, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(poi), 1));
        when(snapshotRepository.findAllById(anyIterable())).thenReturn(List.of(
                snapshot("id-1", "4.2", 16, 26, "出餐快", "学生友好", "pending")));

        RestaurantQueryApplicationService.RestaurantPage page =
                service.nearby(120.35, 30.31, 1000, 1, 10);

        assertEquals(List.of(), page.items().get(0).aiTags());
    }

    @Test
    void nearbyShouldFilterByCategoryAndPriceRange() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());
        AmapPoi noodle = new AmapPoi("id-3", "兰州拉面", "文一路", 120.35, 30.31, "面馆", 180);
        AmapPoi rice = new AmapPoi("id-4", "黄焖鸡米饭", "文二路", 120.36, 30.32, "快餐", 220);
        when(amapClient.searchNearby(120.35, 30.31, 1000, 1, 50))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(noodle, rice), 2));
        when(snapshotRepository.findAllById(anyIterable())).thenReturn(List.of(
                snapshot("id-3", "4.5", 11, 22, "热汤", null),
                snapshot("id-4", "4.7", 19, 34, "下饭", null)));

        RestaurantQueryApplicationService.RestaurantPage page = service.nearby(
                120.35,
                30.31,
                1000,
                1,
                10,
                "distance",
                "面馆",
                20,
                25);

        assertEquals(1, page.items().size());
        assertEquals(1, page.total());
        assertEquals("id-3", page.items().get(0).poiId());
    }

    @Test
    void nearbyShouldExcludeNullPriceWhenPriceBoundsPresent() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());
        AmapPoi noodle = new AmapPoi("id-5", "素面馆", "文三路", 120.35, 30.31, "面馆", 180);
        AmapPoi rice = new AmapPoi("id-6", "拌饭铺子", "文四路", 120.36, 30.32, "快餐", 220);
        when(amapClient.searchNearby(120.35, 30.31, 1000, 1, 50))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(noodle, rice), 2));
        when(snapshotRepository.findAllById(anyIterable())).thenReturn(List.of(
                snapshot("id-5", "4.4", 8, null, "热汤", null),
                snapshot("id-6", "4.6", 12, 26, "下饭", null)));

        RestaurantQueryApplicationService.RestaurantPage page = service.nearby(
                120.35,
                30.31,
                1000,
                1,
                10,
                "distance",
                null,
                20,
                30);

        assertEquals(1, page.items().size());
        assertEquals("id-6", page.items().get(0).poiId());
    }

    @Test
    void nearbyShouldRejectPriceRangeWhenMinGreaterThanMax() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantMetricSnapshotRepository snapshotRepository = mock(RestaurantMetricSnapshotRepository.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(
                amapClient,
                snapshotRepository,
                new SimpleMeterRegistry());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.nearby(120.35, 30.31, 1000, 1, 10, "distance", null, 50, 20));

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }

    private RestaurantMetricSnapshotEntity snapshot(
            String poiId,
            String avgRating,
            int reviewCount,
            Integer avgPerCapitaPrice,
            String aiTag1,
            String aiTag2) {
        return snapshot(poiId, avgRating, reviewCount, avgPerCapitaPrice, aiTag1, aiTag2, "ready");
    }

    private RestaurantMetricSnapshotEntity snapshot(
            String poiId,
            String avgRating,
            int reviewCount,
            Integer avgPerCapitaPrice,
            String aiTag1,
            String aiTag2,
            String aiStatus) {
        RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
        entity.setPoiId(poiId);
        entity.setAvgRating(new BigDecimal(avgRating));
        entity.setReviewCount(reviewCount);
        if (avgPerCapitaPrice != null) {
            entity.setAvgPerCapitaPrice(avgPerCapitaPrice);
        }
        entity.setAiTag1(aiTag1);
        entity.setAiTag2(aiTag2);
        entity.setAiStatus(aiStatus);
        return entity;
    }
}
