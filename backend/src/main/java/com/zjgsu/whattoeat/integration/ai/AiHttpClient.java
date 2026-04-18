package com.zjgsu.whattoeat.integration.ai;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.AiServiceProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class AiHttpClient implements AiAssistantClient {

    private static final String REVIEW_TAGS_PATH = "/internal/review-tags";
    private static final String RECOMMEND_PATH = "/internal/recommend";
    private static final String RECOMMEND_STREAM_PATH = "/internal/recommend/stream";

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiServiceProperties properties;
    private final MeterRegistry meterRegistry;

    public AiHttpClient(
            RestClient.Builder builder,
            AiServiceProperties properties,
            MeterRegistry meterRegistry) {
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .build();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ReviewTagResult summarizeReviewTags(ReviewTagRequest request) {
        return post(REVIEW_TAGS_PATH, request, ReviewTagResult.class, "review-tags");
    }

    @Override
    public RecommendationAdvice recommend(RecommendationRequest request) {
        return post(RECOMMEND_PATH, request, RecommendationAdvice.class, "recommend");
    }

    @Override
    public void streamRecommend(RecommendationRequest request, Consumer<RecommendationStreamEvent> eventConsumer) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + RECOMMEND_STREAM_PATH))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() == 504) {
                incrementCallCounter("recommend-stream", ErrorCode.AI_UPSTREAM_TIMEOUT.name());
                throw new BusinessException(ErrorCode.AI_UPSTREAM_TIMEOUT);
            }
            if (response.statusCode() >= 400) {
                incrementCallCounter("recommend-stream", ErrorCode.AI_UPSTREAM_ERROR.name());
                throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
            }

            try (Stream<String> lines = response.body()) {
                parseSse(lines, eventConsumer);
            }
            incrementCallCounter("recommend-stream", "success");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            ErrorCode errorCode = isTimeoutException(e) ? ErrorCode.AI_UPSTREAM_TIMEOUT : ErrorCode.AI_UPSTREAM_ERROR;
            incrementCallCounter("recommend-stream", errorCode.name());
            throw new BusinessException(errorCode, e.getMessage());
        } finally {
            sample.stop(Timer.builder("ai.client.latency")
                    .tag("operation", "recommend-stream")
                    .register(meterRegistry));
        }
    }

    private <T> T post(String path, Object request, Class<T> responseType, String operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T response = restClient.post()
                    .uri(path)
                    .body(request)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
            }
            incrementCallCounter(operation, "success");
            return response;
        } catch (BusinessException e) {
            incrementCallCounter(operation, e.getErrorCode().name());
            throw e;
        } catch (RestClientResponseException e) {
            ErrorCode errorCode = e.getStatusCode().value() == 504
                    ? ErrorCode.AI_UPSTREAM_TIMEOUT
                    : ErrorCode.AI_UPSTREAM_ERROR;
            incrementCallCounter(operation, errorCode.name());
            throw new BusinessException(errorCode, e.getMessage());
        } catch (ResourceAccessException e) {
            ErrorCode errorCode = isTimeoutException(e) ? ErrorCode.AI_UPSTREAM_TIMEOUT : ErrorCode.AI_UPSTREAM_ERROR;
            incrementCallCounter(operation, errorCode.name());
            throw new BusinessException(errorCode, e.getMessage());
        } catch (Exception e) {
            incrementCallCounter(operation, ErrorCode.AI_UPSTREAM_ERROR.name());
            throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR, e.getMessage());
        } finally {
            sample.stop(Timer.builder("ai.client.latency")
                    .tag("operation", operation)
                    .register(meterRegistry));
        }
    }

    private void incrementCallCounter(String operation, String result) {
        Counter.builder("ai.client.calls")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private boolean isTimeoutException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void parseSse(Stream<String> lines, Consumer<RecommendationStreamEvent> eventConsumer) throws IOException {
        String currentEvent = null;
        StringBuilder currentData = new StringBuilder();
        for (String rawLine : (Iterable<String>) lines::iterator) {
            String line = rawLine == null ? "" : rawLine;
            if (line.isEmpty()) {
                dispatchSseEvent(currentEvent, currentData, eventConsumer);
                currentEvent = null;
                currentData = new StringBuilder();
                continue;
            }
            if (line.startsWith("event:")) {
                currentEvent = line.substring("event:".length()).trim();
                continue;
            }
            if (line.startsWith("data:")) {
                if (!currentData.isEmpty()) {
                    currentData.append('\n');
                }
                currentData.append(line.substring("data:".length()).trim());
            }
        }
        dispatchSseEvent(currentEvent, currentData, eventConsumer);
    }

    private void dispatchSseEvent(
            String eventName,
            StringBuilder currentData,
            Consumer<RecommendationStreamEvent> eventConsumer) throws IOException {
        if (eventName == null || eventName.isBlank()) {
            return;
        }
        Map<String, Object> data = currentData.isEmpty()
                ? Map.of()
                : objectMapper.readValue(currentData.toString(), new TypeReference<LinkedHashMap<String, Object>>() {
                });
        eventConsumer.accept(new RecommendationStreamEvent(eventName, data));
    }
}
