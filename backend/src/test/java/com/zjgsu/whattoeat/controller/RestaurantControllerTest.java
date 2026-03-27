package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.service.application.RestaurantQueryApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantControllerTest {

    private MockMvc mockMvc;
    private RestaurantQueryApplicationService service;

    @BeforeEach
    void setUp() {
        service = mock(RestaurantQueryApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RestaurantController(service))
                .setControllerAdvice(new com.zjgsu.whattoeat.common.web.GlobalExceptionHandler())
                .build();
    }

    @Test
    void nearbyShouldReturnPagedResultWithDefaults() throws Exception {
        AmapPoi poi = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        RestaurantQueryApplicationService.RestaurantPage result =
                new RestaurantQueryApplicationService.RestaurantPage(List.of(poi), 1, 10, 35);
        when(service.nearby(120.35, 30.31, 1000, 1, 10)).thenReturn(result);

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(35))
                .andExpect(jsonPath("$.data.items[0].poiId").value("id-1"));

        verify(service).nearby(eq(120.35), eq(30.31), eq(1000), eq(1), eq(10));
    }

    @Test
    void nearbyShouldAllowSize100() throws Exception {
        AmapPoi poi = new AmapPoi("id-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        RestaurantQueryApplicationService.RestaurantPage result =
                new RestaurantQueryApplicationService.RestaurantPage(List.of(poi), 1, 100, 120);
        when(service.nearby(120.35, 30.31, 1000, 1, 100)).thenReturn(result);

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.size").value(100));

        verify(service).nearby(eq(120.35), eq(30.31), eq(1000), eq(1), eq(100));
    }

    @Test
    void nearbyShouldReturn400WhenPageInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void nearbyShouldReturnEmptyPageWhenTotalPositiveButCurrentPageEmpty() throws Exception {
        RestaurantQueryApplicationService.RestaurantPage result =
                new RestaurantQueryApplicationService.RestaurantPage(List.of(), 2, 10, 35);
        when(service.nearby(120.35, 30.31, 1000, 2, 10)).thenReturn(result);

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(35))
                .andExpect(jsonPath("$.data.items.length()").value(0));

        verify(service).nearby(eq(120.35), eq(30.31), eq(1000), eq(2), eq(10));
    }

    @Test
    void searchShouldReturnPagedResult() throws Exception {
        AmapPoi poi = new AmapPoi("id-2", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        RestaurantQueryApplicationService.RestaurantPage result =
                new RestaurantQueryApplicationService.RestaurantPage(List.of(poi), 2, 5, 12);
        when(service.search("拉面", 120.36, 30.32, 1500, 2, 5)).thenReturn(result);

        mockMvc.perform(get("/api/v1/restaurants/search")
                        .param("keyword", "拉面")
                        .param("longitude", "120.36")
                        .param("latitude", "30.32")
                        .param("radius", "1500")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.items[0].name").value("兰州拉面"));

        verify(service).search(eq("拉面"), eq(120.36), eq(30.32), eq(1500), eq(2), eq(5));
    }

    @Test
    void searchShouldReturn400WhenKeywordBlank() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/search")
                        .param("keyword", " ")
                        .param("longitude", "120.36")
                        .param("latitude", "30.32"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void nearbyShouldMapNoResultTo3003() throws Exception {
        when(service.nearby(120.35, 30.31, 1000, 1, 10))
                .thenThrow(new BusinessException(ErrorCode.AMAP_NO_RESULT));

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3003));
    }

    @Test
    void nearbyShouldMapTimeoutTo3002() throws Exception {
        when(service.nearby(120.35, 30.31, 1000, 1, 10))
                .thenThrow(new BusinessException(ErrorCode.AMAP_UPSTREAM_TIMEOUT));

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(3002));
    }

    @Test
    void searchShouldMapNoResultTo3003() throws Exception {
        when(service.search("拉面", 120.36, 30.32, 1500, 1, 10))
                .thenThrow(new BusinessException(ErrorCode.AMAP_NO_RESULT));

        mockMvc.perform(get("/api/v1/restaurants/search")
                        .param("keyword", "拉面")
                        .param("longitude", "120.36")
                        .param("latitude", "30.32")
                        .param("radius", "1500"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3003));
    }
}
