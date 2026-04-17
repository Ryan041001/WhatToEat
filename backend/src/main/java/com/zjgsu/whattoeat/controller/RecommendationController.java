package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.service.application.RecommendationApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/recommendations")
@Validated
public class RecommendationController {

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
            @Validated @RequestBody AskRecommendationRequest request) {
        int radius = request.radius() == null ? 1000 : request.radius();
        int size = request.size() == null ? 3 : request.size();
        return ApiResponse.ok(service.ask(
                request.userId(),
                request.longitude(),
                request.latitude(),
                radius,
                size,
                request.question(),
                toAskContext(request.context())));
    }

    @PostMapping(path = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Validated @RequestBody AskRecommendationRequest request) {
        int radius = request.radius() == null ? 1000 : request.radius();
        int size = request.size() == null ? 3 : request.size();
        SseEmitter emitter = new SseEmitter(60_000L);
        String requestId = UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();

        CompletableFuture.runAsync(() -> streamRecommendation(
                emitter,
                request,
                radius,
                size,
                requestId,
                messageId));
        return emitter;
    }

    private void streamRecommendation(
            SseEmitter emitter,
            AskRecommendationRequest request,
            int radius,
            int size,
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
                    request.question(),
                    toAskContext(request.context()),
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
                context.previousQuestion(),
                context.rejectedPoiIds() == null ? List.of() : List.copyOf(context.rejectedPoiIds()),
                context.selectedPoiIds() == null ? List.of() : List.copyOf(context.selectedPoiIds()),
                context.userSignals() == null ? List.of() : List.copyOf(context.userSignals()));
    }

    public record AskRecommendationRequest(
            @Min(1) Long userId,
            @NotNull Double longitude,
            @NotNull Double latitude,
            @Min(100) @Max(50000) Integer radius,
            @Min(1) @Max(10) Integer size,
            @NotBlank String question,
            AskRecommendationContextRequest context) {
    }

    public record AskRecommendationContextRequest(
            String previousQuestion,
            List<@NotBlank String> rejectedPoiIds,
            List<@NotBlank String> selectedPoiIds,
            List<@NotBlank String> userSignals) {
    }
}
