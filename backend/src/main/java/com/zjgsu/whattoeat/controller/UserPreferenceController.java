package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.service.application.UserPreferenceApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
public class UserPreferenceController {

    private final UserPreferenceApplicationService service;

    public UserPreferenceController(UserPreferenceApplicationService service) {
        this.service = service;
    }

    /** POST /api/v1/users/{userId}/preferences/blacklist */
    @PostMapping("/blacklist")
    public ApiResponse<Void> addBlacklist(@PathVariable Long userId, @RequestParam String poiId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** DELETE /api/v1/users/{userId}/preferences/blacklist/{poiId} */
    @DeleteMapping("/blacklist/{poiId}")
    public ApiResponse<Void> removeBlacklist(@PathVariable Long userId, @PathVariable String poiId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** PUT /api/v1/users/{userId}/preferences/notes/{poiId} */
    @PutMapping("/notes/{poiId}")
    public ApiResponse<Void> saveNote(@PathVariable Long userId, @PathVariable String poiId,
                                      @RequestParam String note) {
        throw new UnsupportedOperationException("TODO");
    }
}
