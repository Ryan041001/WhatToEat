package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import com.zjgsu.whattoeat.service.application.UserPreferenceProfileApplicationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/preference-profile")
@Validated
public class UserPreferenceProfileController {

    private final AuthApplicationService authApplicationService;
    private final UserPreferenceProfileApplicationService userPreferenceProfileApplicationService;

    public UserPreferenceProfileController(
            AuthApplicationService authApplicationService,
            UserPreferenceProfileApplicationService userPreferenceProfileApplicationService) {
        this.authApplicationService = authApplicationService;
        this.userPreferenceProfileApplicationService = userPreferenceProfileApplicationService;
    }

    @GetMapping
    public ApiResponse<UserPreferenceProfileApplicationService.PreferenceProfile> getPreferenceProfile(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userPreferenceProfileApplicationService.getPreferenceProfile(userId));
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
}
