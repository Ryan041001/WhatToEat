package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.service.application.RestaurantReviewQueryApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@Validated
public class RestaurantReviewSummaryController {

    private final RestaurantReviewQueryApplicationService restaurantReviewQueryApplicationService;

    public RestaurantReviewSummaryController(RestaurantReviewQueryApplicationService restaurantReviewQueryApplicationService) {
        this.restaurantReviewQueryApplicationService = restaurantReviewQueryApplicationService;
    }

    @GetMapping("/{poiId}/reviews")
    public ApiResponse<RestaurantReviewQueryApplicationService.ReviewPage> listReviews(
            @PathVariable @NotBlank String poiId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(restaurantReviewQueryApplicationService.listPublicReviews(poiId, page, size));
    }

    @GetMapping("/{poiId}/review-summary")
    public ApiResponse<RestaurantReviewQueryApplicationService.ReviewSummary> getReviewSummary(
            @PathVariable @NotBlank String poiId) {
        return ApiResponse.ok(restaurantReviewQueryApplicationService.getReviewSummary(poiId));
    }
}
