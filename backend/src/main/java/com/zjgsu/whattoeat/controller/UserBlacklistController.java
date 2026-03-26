package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import com.zjgsu.whattoeat.service.application.UserBlacklistApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/blacklist")
@Validated
public class UserBlacklistController {

    private final AuthApplicationService authApplicationService;
    private final UserBlacklistApplicationService userBlacklistApplicationService;

    public UserBlacklistController(
            AuthApplicationService authApplicationService,
            UserBlacklistApplicationService userBlacklistApplicationService) {
        this.authApplicationService = authApplicationService;
        this.userBlacklistApplicationService = userBlacklistApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addBlacklist(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AddBlacklistRequest request) {
        String token = extractBearerToken(authorization);
        UserEntity currentUser = authApplicationService.me(token);
        if (!currentUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userBlacklistApplicationService.addBlacklist(userId, request.poiId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
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

    public record AddBlacklistRequest(@NotBlank @Size(max = 64) String poiId) {
    }
}
