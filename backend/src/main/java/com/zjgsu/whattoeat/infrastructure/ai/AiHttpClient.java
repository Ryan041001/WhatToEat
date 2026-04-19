package com.zjgsu.whattoeat.infrastructure.ai;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.AiServiceProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class AiHttpClient implements AiAssistantClient {

    private static final String REVIEW_TAGS_PATH = "/internal/review-tags";
    private static final String RECOMMEND_STREAM_PATH = "/internal/recommend/stream";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiServiceProperties properties;
    private final MeterRegistry meterRegistry;

    public AiHttpClient(
            org.springframework.web.client.RestClient.Builder builder,
            AiServiceProperties properties,
            MeterRegistry meterRegistry) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1)
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
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            StringBuilder streamedAnswer = new StringBuilder();
            Map<Integer, RecommendationChoice> rankedChoices = new TreeMap<>();
            List<RecommendationChoice> unrankedChoices = new ArrayList<>();
            final String[] finalAnswer = {null};

            streamRecommend(request, upstreamEvent -> {
                switch (upstreamEvent.name()) {
                    case "answer.delta" -> appendAnswerDelta(streamedAnswer, upstreamEvent.data());
                    case "answer.done" -> finalAnswer[0] = extractAnswer(upstreamEvent.data(), streamedAnswer);
                    case "tool.call" -> collectRecommendationChoice(
                            upstreamEvent.data(),
                            rankedChoices,
                            unrankedChoices);
                    case "error" -> throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
                    default -> {
                        // Ignore unknown upstream events for forward compatibility.
                    }
                }
            });

            List<RecommendationChoice> orderedChoices = new ArrayList<>(rankedChoices.values());
            orderedChoices.addAll(unrankedChoices);
            incrementCallCounter("recommend", "success");
            return new RecommendationAdvice(
                    finalAnswer[0] == null || finalAnswer[0].isBlank()
                            ? streamedAnswer.toString().trim()
                            : finalAnswer[0],
                    List.copyOf(orderedChoices));
        } catch (BusinessException e) {
            incrementCallCounter("recommend", e.getErrorCode().name());
            throw e;
        } catch (Exception e) {
            ErrorCode errorCode = isTimeoutException(e) ? ErrorCode.AI_UPSTREAM_TIMEOUT : ErrorCode.AI_UPSTREAM_ERROR;
            incrementCallCounter("recommend", errorCode.name());
            throw new BusinessException(errorCode, e.getMessage());
        } finally {
            sample.stop(Timer.builder("ai.client.latency")
                    .tag("operation", "recommend")
                    .register(meterRegistry));
        }
    }

    @Override
    public void streamRecommend(RecommendationRequest request, Consumer<RecommendationStreamEvent> eventConsumer) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + RECOMMEND_STREAM_PATH))
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
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + path))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 504) {
                incrementCallCounter(operation, ErrorCode.AI_UPSTREAM_TIMEOUT.name());
                throw new BusinessException(ErrorCode.AI_UPSTREAM_TIMEOUT);
            }
            if (response.statusCode() >= 400) {
                incrementCallCounter(operation, ErrorCode.AI_UPSTREAM_ERROR.name());
                throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR, response.body());
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
            }

            T parsed = objectMapper.readValue(response.body(), responseType);
            incrementCallCounter(operation, "success");
            return parsed;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            incrementCallCounter(operation, ErrorCode.AI_UPSTREAM_ERROR.name());
            throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ErrorCode errorCode = isTimeoutException(e) ? ErrorCode.AI_UPSTREAM_TIMEOUT : ErrorCode.AI_UPSTREAM_ERROR;
            incrementCallCounter(operation, errorCode.name());
            throw new BusinessException(errorCode, e.getMessage());
        } catch (Exception e) {
            ErrorCode errorCode = isTimeoutException(e) ? ErrorCode.AI_UPSTREAM_TIMEOUT : ErrorCode.AI_UPSTREAM_ERROR;
            incrementCallCounter(operation, errorCode.name());
            throw new BusinessException(errorCode, e.getMessage());
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

    @SuppressWarnings("unchecked")
    private void collectRecommendationChoice(
            Map<String, Object> upstreamData,
            Map<Integer, RecommendationChoice> rankedChoices,
            List<RecommendationChoice> unrankedChoices) {
        Object toolName = upstreamData.get("toolName");
        if (!(toolName instanceof String tool) || !"show_restaurant_card".equals(tool)) {
            return;
        }
        Object argumentsObject = upstreamData.get("arguments");
        if (!(argumentsObject instanceof Map<?, ?> rawArguments)) {
            return;
        }
        Map<String, Object> arguments = (Map<String, Object>) rawArguments;
        Object poiIdValue = arguments.get("poiId");
        if (!(poiIdValue instanceof String poiId) || poiId.isBlank()) {
            return;
        }
        String reason = arguments.get("reason") instanceof String text ? text : null;
        RecommendationChoice choice = new RecommendationChoice(poiId, reason);
        Object rankValue = arguments.get("rank");
        if (rankValue instanceof Number number && number.intValue() > 0) {
            rankedChoices.putIfAbsent(number.intValue(), choice);
            return;
        }
        unrankedChoices.add(choice);
    }

    private void appendAnswerDelta(StringBuilder streamedAnswer, Map<String, Object> upstreamData) {
        Object delta = upstreamData.get("delta");
        if (delta instanceof String text) {
            streamedAnswer.append(text);
        }
    }

    private String extractAnswer(Map<String, Object> upstreamData, StringBuilder streamedAnswer) {
        Object answer = upstreamData.get("answer");
        if (answer instanceof String text && !text.isBlank()) {
            return text;
        }
        return streamedAnswer.toString().trim();
    }
}
