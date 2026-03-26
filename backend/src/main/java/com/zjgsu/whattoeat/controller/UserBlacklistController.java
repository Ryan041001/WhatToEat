package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/blacklist")
@Validated
public class UserBlacklistController {

    private final UserBlacklistApplicationService userBlacklistApplicationService;

    public UserBlacklistController(UserBlacklistApplicationService userBlacklistApplicationService) {
        this.userBlacklistApplicationService = userBlacklistApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addBlacklist(
            @PathVariable Long userId,
            @Valid @RequestBody AddBlacklistRequest request) {
        userBlacklistApplicationService.addBlacklist(userId, request.poiId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    public record AddBlacklistRequest(@NotBlank @Size(max = 64) String poiId) {
    }
}
