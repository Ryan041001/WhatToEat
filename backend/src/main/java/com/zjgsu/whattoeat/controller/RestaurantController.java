package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.service.application.RestaurantQueryApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@Validated
public class RestaurantController {

    private final RestaurantQueryApplicationService service;

    public RestaurantController(RestaurantQueryApplicationService service) {
        this.service = service;
    }

    @GetMapping("/nearby")
    public ApiResponse<RestaurantQueryApplicationService.RestaurantPage> nearby(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return ApiResponse.ok(service.nearby(longitude, latitude, radius, page, size));
    }

    @GetMapping("/search")
    public ApiResponse<RestaurantQueryApplicationService.RestaurantPage> search(
            @RequestParam @NotBlank String keyword,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (keyword == null || keyword.isBlank() || page < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return ApiResponse.ok(service.search(keyword, longitude, latitude, radius, page, size));
    }
}
