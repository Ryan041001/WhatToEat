package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.service.application.RestaurantQueryApplicationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantControllerTest {

    private MockMvc mockMvc;
    private AmapClient amapClient;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        amapClient = mock(AmapClient.class);
        meterRegistry = new SimpleMeterRegistry();
        RestaurantQueryApplicationService queryService = new RestaurantQueryApplicationService(amapClient, meterRegistry);
        mockMvc = MockMvcBuilders.standaloneSetup(new RestaurantController(queryService))
                .setControllerAdvice(new com.zjgsu.whattoeat.common.web.GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchShouldReturnPagedResultAndIncrementSuccessMetric() throws Exception {
        AmapPoi poi = new AmapPoi("id-2", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        when(amapClient.searchByKeyword("拉面", 120.36, 30.32, 1500, 2, 5))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(poi), 12));

        mockMvc.perform(get("/api/v1/restaurants/search")
                        .param("keyword", "拉面")
                        .param("longitude", "120.36")
                        .param("latitude", "30.32")
                        .param("radius", "1500")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2));

        verify(amapClient).searchByKeyword(eq("拉面"), eq(120.36), eq(30.32), eq(1500), eq(2), eq(5));
        assertEquals(1.0, meterRegistry.get("restaurant.query.requests")
                .tag("scene", "search")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void searchShouldMapNoResultTo3003AndIncrementResultMetric() throws Exception {
        when(amapClient.searchByKeyword("拉面", 120.35, 30.31, 1000, 1, 10))
                .thenThrow(new BusinessException(ErrorCode.AMAP_NO_RESULT));

        mockMvc.perform(get("/api/v1/restaurants/search")
                        .param("keyword", "拉面")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3003));

        assertEquals(1.0, meterRegistry.get("restaurant.query.requests")
                .tag("scene", "search")
                .tag("result", ErrorCode.AMAP_NO_RESULT.name())
                .counter()
                .count());
    }

    @Test
    void nearbyShouldReturnPagedResultAndIncrementSuccessMetric() throws Exception {
        AmapPoi poi = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        when(amapClient.searchNearby(120.35, 30.31, 1000, 1, 10))
                .thenReturn(new AmapClient.AmapSearchResult(List.of(poi), 35));

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.total").value(35));

        verify(amapClient).searchNearby(eq(120.35), eq(30.31), eq(1000), eq(1), eq(10));
        assertEquals(1.0, meterRegistry.get("restaurant.query.requests")
                .tag("scene", "nearby")
                .tag("result", "success")
                .counter()
                .count());
    }
}
