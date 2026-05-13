package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/wechat-login")
    public ResponseEntity<ApiResponse<LoginResponse>> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        AuthApplicationService.LoginResult loginResult = authApplicationService.wechatLogin(
                request.code(),
                request.nickname(),
                request.avatarUrl());
        LoginResponse response = new LoginResponse(loginResult.token(), UserInfo.from(loginResult.user()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        authApplicationService.logout(token);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        UserEntity user = authApplicationService.me(token);
        return ApiResponse.ok(UserInfo.from(user));
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

    public record WechatLoginRequest(@NotBlank String code, String nickname, String avatarUrl) {
    }

    public record LoginResponse(String token, UserInfo user) {
    }

    public record UserInfo(Long id, String openid, String nickname, String avatarUrl) {
        public static UserInfo from(UserEntity user) {
            return new UserInfo(user.getId(), user.getOpenid(), user.getNickname(), user.getAvatarUrl());
        }
    }
}
