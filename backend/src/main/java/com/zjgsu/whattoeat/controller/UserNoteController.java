package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.service.application.AuthApplicationService;
import com.zjgsu.whattoeat.service.application.UserNoteApplicationService;
import com.zjgsu.whattoeat.service.application.UserNoteApplicationService.NoteDetail;
import com.zjgsu.whattoeat.service.application.UserNoteApplicationService.NotePage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/users/{userId}/notes")
@Validated
public class UserNoteController {

    private final AuthApplicationService authApplicationService;
    private final UserNoteApplicationService userNoteApplicationService;

    public UserNoteController(
            AuthApplicationService authApplicationService,
            UserNoteApplicationService userNoteApplicationService) {
        this.authApplicationService = authApplicationService;
        this.userNoteApplicationService = userNoteApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createNote(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateNoteRequest request) {
        authorizeUser(userId, authorization);
        userNoteApplicationService.createNote(userId, request.poiId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @GetMapping
    public ApiResponse<NotePage> listNotes(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userNoteApplicationService.listNotes(userId, page, size, keyword));
    }

    @GetMapping("/{noteId}")
    public ApiResponse<NoteDetail> getNote(
            @PathVariable Long userId,
            @PathVariable @Min(1) Long noteId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userNoteApplicationService.getNote(userId, noteId));
    }

    @PutMapping("/{noteId}")
    public ApiResponse<NoteDetail> updateNote(
            @PathVariable Long userId,
            @PathVariable @Min(1) Long noteId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateNoteRequest request) {
        authorizeUser(userId, authorization);
        return ApiResponse.ok(userNoteApplicationService.updateNote(userId, noteId, request.content()));
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<Void> deleteNote(
            @PathVariable Long userId,
            @PathVariable @Min(1) Long noteId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authorizeUser(userId, authorization);
        userNoteApplicationService.deleteNote(userId, noteId);
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

    public record CreateNoteRequest(
            @NotBlank @Size(max = 64) String poiId,
            @NotNull String content) {
    }

    public record UpdateNoteRequest(@NotNull String content) {
    }
}
