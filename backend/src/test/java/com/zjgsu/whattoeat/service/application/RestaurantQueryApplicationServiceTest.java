package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestaurantQueryApplicationServiceTest {

    @Test
    void nearbyShouldReturnEmptyPageWhenTotalPositiveButItemsEmpty() {
        AmapClient amapClient = mock(AmapClient.class);
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(amapClient, new SimpleMeterRegistry());
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
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(amapClient, new SimpleMeterRegistry());
        when(amapClient.searchByKeyword("拉面", 120.36, 30.32, 1500, 1, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(), 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search("拉面", 120.36, 30.32, 1500, 1, 10));

        assertEquals(ErrorCode.AMAP_NO_RESULT, ex.getErrorCode());
    }

    @Test
    void nearbyShouldRecordNearbySuccessMetric() {
        AmapClient amapClient = mock(AmapClient.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(amapClient, meterRegistry);
        AmapPoi poi = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
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
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RestaurantQueryApplicationService service = new RestaurantQueryApplicationService(amapClient, meterRegistry);
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
}
