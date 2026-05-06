package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.common.security.XssSanitizer;
import com.zjgsu.whattoeat.application.recommendation.RecommendationApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/recommendations")
@Validated
public class RecommendationController {

    private static final long STREAM_TIMEOUT_MILLIS = Duration.ofMinutes(5).toMillis();
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_CONTEXT_TEXT_LENGTH = 500;
    private static final int MAX_CONTEXT_ITEMS = 20;
    private static final int MAX_POI_ID_LENGTH = 128;
    private static final int MAX_USER_SIGNAL_LENGTH = 64;

    private final RecommendationApplicationService service;

    public RecommendationController(RecommendationApplicationService service) {
        this.service = service;
    }

    /**
     * Example: GET /api/v1/recommendations/random?longitude=120.35&latitude=30.31&radius=1000
     */
    @GetMapping("/random")
    public ApiResponse<RecommendationApplicationService.RecommendationResult> random(
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius) {
        return ApiResponse.ok(service.recommendRandom(userId, longitude, latitude, radius));
    }

    @GetMapping("/cards")
    public ApiResponse<List<com.zjgsu.whattoeat.integration.amap.AmapPoi>> cards(
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "1000") @Min(100) @Max(50000) int radius,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.recommendCards(userId, longitude, latitude, radius, size));
    }

    @PostMapping("/ask")
    public ApiResponse<RecommendationApplicationService.AskRecommendationResult> ask(
            @Valid @RequestBody AskRecommendationRequest request) {
        int radius = request.radius() == null ? 1000 : request.radius();
        int size = request.size() == null ? 3 : request.size();
        String sanitizedQuestion = sanitizeRequiredText(request.question());
        return ApiResponse.ok(service.ask(
                request.userId(),
                request.longitude(),
                request.latitude(),
                radius,
                size,
                sanitizedQuestion,
                toAskContext(request.context())));
    }

    @PostMapping(path = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody AskRecommendationRequest request) {
        int radius = request.radius() == null ? 1000 : request.radius();
        int size = request.size() == null ? 3 : request.size();
        String sanitizedQuestion = sanitizeRequiredText(request.question());
        RecommendationApplicationService.AskContext sanitizedContext = toAskContext(request.context());
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        String requestId = UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();

        CompletableFuture.runAsync(() -> streamRecommendation(
                emitter,
                request,
                radius,
                size,
                sanitizedQuestion,
                sanitizedContext,
                requestId,
                messageId));
        return emitter;
    }

    private void streamRecommendation(
            SseEmitter emitter,
            AskRecommendationRequest request,
            int radius,
            int size,
            String sanitizedQuestion,
            RecommendationApplicationService.AskContext sanitizedContext,
            String requestId,
            String messageId) {
        try {
            sendEvent(emitter, "session.created", Map.of(
                    "messageId", messageId,
                    "requestId", requestId));
            sendEvent(emitter, "retrieval.started", Map.of(
                    "candidateCount", Math.max(size * 2, 12)));

            service.askStream(
                    request.userId(),
                    request.longitude(),
                    request.latitude(),
                    radius,
                    size,
                    sanitizedQuestion,
                    sanitizedContext,
                    streamEvent -> {
                        try {
                            sendEvent(emitter, streamEvent.name(), streamEvent.data());
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                    });
            emitter.complete();
        } catch (BusinessException ex) {
            completeWithError(emitter, ex.getErrorCode().getCode(), ex.getMessage());
        } catch (Exception ex) {
            completeWithError(emitter, 9000, "系统异常");
        }
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(name)
                .data(data, MediaType.APPLICATION_JSON));
    }

    private void completeWithError(SseEmitter emitter, int code, String message) {
        try {
            sendEvent(emitter, "error", Map.of(
                    "code", code,
                    "message", message));
        } catch (IOException ignored) {
            // Ignore send failure when the client disconnects mid-stream.
        } finally {
            emitter.complete();
        }
    }

    private RecommendationApplicationService.AskContext toAskContext(AskRecommendationContextRequest context) {
        if (context == null) {
            return RecommendationApplicationService.AskContext.empty();
        }
        return new RecommendationApplicationService.AskContext(
                sanitizeOptionalText(context.previousQuestion()),
                sanitizeList(context.rejectedPoiIds()),
                sanitizeList(context.selectedPoiIds()),
                sanitizeList(context.userSignals()));
    }

    private String sanitizeRequiredText(String value) {
        String sanitized = sanitizeOptionalText(value);
        if (sanitized == null || sanitized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return sanitized;
    }

    private String sanitizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        return XssSanitizer.stripAll(value).trim();
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::sanitizeOptionalText)
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED);
                    }
                    return value;
                })
                .toList();
    }

    public record AskRecommendationRequest(
            @Min(1) Long userId,
            @NotNull Double longitude,
            @NotNull Double latitude,
            @Min(100) @Max(50000) Integer radius,
            @Min(1) @Max(10) Integer size,
            @NotBlank @Size(max = MAX_QUESTION_LENGTH) String question,
            @Valid AskRecommendationContextRequest context) {
    }

    public record AskRecommendationContextRequest(
            @Size(max = MAX_CONTEXT_TEXT_LENGTH) String previousQuestion,
            @Size(max = MAX_CONTEXT_ITEMS) List<@NotBlank @Size(max = MAX_POI_ID_LENGTH) String> rejectedPoiIds,
            @Size(max = MAX_CONTEXT_ITEMS) List<@NotBlank @Size(max = MAX_POI_ID_LENGTH) String> selectedPoiIds,
            @Size(max = MAX_CONTEXT_ITEMS) List<@NotBlank @Size(max = MAX_USER_SIGNAL_LENGTH) String> userSignals) {
    }
}
