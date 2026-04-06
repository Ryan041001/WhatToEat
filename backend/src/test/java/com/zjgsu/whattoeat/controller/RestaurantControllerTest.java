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
    private RestaurantQueryApplicationService queryService;

    @BeforeEach
    void setUp() {
        queryService = mock(RestaurantQueryApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RestaurantController(queryService))
                .setControllerAdvice(new com.zjgsu.whattoeat.common.web.GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchShouldReturnPagedResult() throws Exception {
        AmapPoi poi = new AmapPoi("id-2", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        RestaurantQueryApplicationService.RestaurantPage result =
                new RestaurantQueryApplicationService.RestaurantPage(List.of(poi), 2, 5, 12);
        when(queryService.search("拉面", 120.36, 30.32, 1500, 2, 5)).thenReturn(result);

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

        verify(queryService).search(eq("拉面"), eq(120.36), eq(30.32), eq(1500), eq(2), eq(5));
    }

    @Test
    void searchShouldMapNoResultTo3003() throws Exception {
        when(queryService.search("拉面", 120.35, 30.31, 1000, 1, 10))
                .thenThrow(new BusinessException(ErrorCode.AMAP_NO_RESULT));

        mockMvc.perform(get("/api/v1/restaurants/search")
                        .param("keyword", "拉面")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3003));
    }
}
