package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.RecommendationFeedbackType;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import com.zjgsu.whattoeat.service.application.UserRecommendationFeedbackApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/recommendation-feedback")
@Validated
public class UserRecommendationFeedbackController {

    private final AuthApplicationService authApplicationService;
    private final UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService;

    public UserRecommendationFeedbackController(
            AuthApplicationService authApplicationService,
            UserRecommendationFeedbackApplicationService userRecommendationFeedbackApplicationService) {
        this.authApplicationService = authApplicationService;
        this.userRecommendationFeedbackApplicationService = userRecommendationFeedbackApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createFeedback(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateRecommendationFeedbackRequest request) {
        authorizeUser(userId, authorization);
        userRecommendationFeedbackApplicationService.createFeedback(
                userId,
                request.poiId(),
                request.poiNameSnapshot(),
                request.feedbackType(),
                request.detail(),
                request.requestQuestion());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @GetMapping
    public ApiResponse<UserRecommendationFeedbackApplicationService.FeedbackPage> listFeedback(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userRecommendationFeedbackApplicationService.listFeedback(userId, page, size));
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

    public record CreateRecommendationFeedbackRequest(
            @Size(max = 64) String poiId,
            @Size(max = 128) String poiNameSnapshot,
            @NotNull RecommendationFeedbackType feedbackType,
            @Size(max = 255) String detail,
            @Size(max = 255) String requestQuestion) {
    }
}
