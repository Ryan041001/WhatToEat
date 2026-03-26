package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.service.application.RecommendationApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@Validated
public class RecommendationController {

    private final RecommendationApplicationService service;

    public RecommendationController(RecommendationApplicationService service) {
        this.service = service;
    }

    /**
     * GET /api/v1/recommendations/random?userId=&longitude=&latitude=&radius=
     */
    @GetMapping("/random")
    public ApiResponse<RecommendationApplicationService.RecommendationResult> random(
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius) {
        return ApiResponse.ok(service.recommendRandom(userId, longitude, latitude, radius));
    }

    @GetMapping("/cards")
    public ApiResponse<List<com.zjgsu.whattoeat.integration.amap.AmapPoi>> cards(
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.recommendCards(userId, longitude, latitude, radius, size));
    }
}
