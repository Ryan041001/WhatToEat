package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import com.zjgsu.whattoeat.service.application.RestaurantReviewApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users/{userId}/restaurant-reviews")
@Validated
public class RestaurantReviewController {

    private final AuthApplicationService authApplicationService;
    private final RestaurantReviewApplicationService restaurantReviewApplicationService;

    public RestaurantReviewController(
            AuthApplicationService authApplicationService,
            RestaurantReviewApplicationService restaurantReviewApplicationService) {
        this.authApplicationService = authApplicationService;
        this.restaurantReviewApplicationService = restaurantReviewApplicationService;
    }

    @GetMapping("/{poiId}")
    public ApiResponse<RestaurantReviewApplicationService.ReviewDetail> getReview(
            @PathVariable Long userId,
            @PathVariable @NotBlank String poiId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(restaurantReviewApplicationService.getUserReview(userId, poiId));
    }

    @PutMapping("/{poiId}")
    public ApiResponse<RestaurantReviewApplicationService.ReviewDetail> upsertReview(
            @PathVariable Long userId,
            @PathVariable @NotBlank String poiId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpsertRestaurantReviewRequest request) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(restaurantReviewApplicationService.upsertReview(
                userId,
                poiId,
                request.poiNameSnapshot(),
                request.ratingScore(),
                request.perCapitaPrice(),
                request.content()));
    }

    @DeleteMapping("/{poiId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long userId,
            @PathVariable @NotBlank String poiId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        restaurantReviewApplicationService.deleteReview(userId, poiId);
        return ApiResponse.ok();
    }

    private void authorizeUser(Long userId, String authorization) {
        String token = extractBearerToken(authorization);
        UserEntity currentUser = authApplicationService.me(token);
        if (!currentUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return token;
    }

    public record UpsertRestaurantReviewRequest(
            @Size(max = 128) String poiNameSnapshot,
            @NotNull BigDecimal ratingScore,
            @NotNull Integer perCapitaPrice,
            @NotNull String content) {
    }
}
