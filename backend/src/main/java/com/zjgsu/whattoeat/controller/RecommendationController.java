package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.service.application.RecommendationApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<AmapPoi> random(
            @RequestParam Long userId,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius) {
        throw new UnsupportedOperationException("TODO");
    }
}
