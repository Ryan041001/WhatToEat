package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import com.zjgsu.whattoeat.service.application.UserBlacklistApplicationService.BlacklistPage;
import com.zjgsu.whattoeat.service.application.UserBlacklistApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        authorizeUser(userId, authorization);
        userBlacklistApplicationService.addBlacklist(userId, request.poiId(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @GetMapping("/{poiId}")
    public ApiResponse<UserBlacklistApplicationService.BlacklistItem> getBlacklist(
            @PathVariable Long userId,
            @PathVariable @NotBlank @Size(max = 64) String poiId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userBlacklistApplicationService.getBlacklist(userId, poiId));
    }

    @DeleteMapping("/{poiId}")
    public ApiResponse<Void> removeBlacklist(
            @PathVariable Long userId,
            @PathVariable @NotBlank @Size(max = 64) String poiId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        userBlacklistApplicationService.removeBlacklist(userId, poiId);
        return ApiResponse.ok();
    }

    @PutMapping("/{poiId}")
    public ApiResponse<Void> updateBlacklist(
            @PathVariable Long userId,
            @PathVariable @NotBlank @Size(max = 64) String poiId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateBlacklistRequest request) {
        authorizeUser(userId, authorization);
        userBlacklistApplicationService.updateBlacklist(userId, poiId, request.reason());
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<BlacklistPage> listBlacklist(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userBlacklistApplicationService.listBlacklist(userId, page, size));
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

    public record AddBlacklistRequest(@NotBlank @Size(max = 64) String poiId, @Size(max = 255) String reason) {
    }

    public record UpdateBlacklistRequest(@Size(max = 255) String reason) {
    }
}
