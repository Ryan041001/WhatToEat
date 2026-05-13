package com.zjgsu.whattoeat.infrastructure.ai;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.AiServiceProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiHttpClientTest {

    @Test
    void summarizeReviewTagsShouldMapSuccessResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        AiHttpClient client = createClient(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse(200, """
                        {"tag1":"性价比高","tag2":"热汤稳","summary":"更适合想吃热一点的预算内选择。"}
                        """));

        AiAssistantClient.ReviewTagResult result = client.summarizeReviewTags(
                new AiAssistantClient.ReviewTagRequest("poi-1", "兰州拉面", List.of()));

        assertEquals("性价比高", result.tag1());
        assertEquals("热汤稳", result.tag2());
        assertEquals("更适合想吃热一点的预算内选择。", result.summary());
    }

    @Test
    void summarizeReviewTagsShouldMapTimeoutStatusToBusinessException() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        AiHttpClient client = createClient(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse(504, ""));

        BusinessException ex = assertThrows(BusinessException.class, () -> client.summarizeReviewTags(
                new AiAssistantClient.ReviewTagRequest("poi-1", "兰州拉面", List.of())));

        assertEquals(ErrorCode.AI_UPSTREAM_TIMEOUT, ex.getErrorCode());
    }

    @Test
    void streamRecommendShouldParseSseEventsIncludingMultilineData() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        AiHttpClient client = createClient(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(lineResponse(200, Stream.of(
                        "event:tool.call",
                        "data:{\"toolName\":\"show_restaurant_card\",",
                        "data:\"arguments\":{\"poiId\":\"poi-1\",\"reason\":\"高蛋白\",\"rank\":1}}",
                        "",
                        "event:answer.delta",
                        "data:{\"delta\":\"优先考虑轻食碗。\"}",
                        ""
                )));

        java.util.List<AiAssistantClient.RecommendationStreamEvent> events = new java.util.ArrayList<>();
        client.streamRecommend(recommendationRequest(), events::add);

        assertEquals(2, events.size());
        assertEquals("tool.call", events.get(0).name());
        assertEquals("poi-1", ((Map<?, ?>) events.get(0).data().get("arguments")).get("poiId"));
        assertEquals("answer.delta", events.get(1).name());
        assertEquals("优先考虑轻食碗。", events.get(1).data().get("delta"));
    }

    @Test
    void recommendShouldAggregateStreamedChoicesAndAnswer() {
        TestableAiHttpClient client = new TestableAiHttpClient();
        client.events = List.of(
                new AiAssistantClient.RecommendationStreamEvent("tool.call", Map.of(
                        "toolName", "show_restaurant_card",
                        "arguments", Map.of("poiId", "poi-1", "reason", "更贴近高蛋白", "rank", 1))),
                new AiAssistantClient.RecommendationStreamEvent("answer.delta", Map.of("delta", "先看轻食碗")),
                new AiAssistantClient.RecommendationStreamEvent("answer.done", Map.of("answer", "先看轻食碗。"))
        );

        AiAssistantClient.RecommendationAdvice advice = client.recommend(recommendationRequest());

        assertEquals("先看轻食碗。", advice.answer());
        assertEquals(1, advice.choices().size());
        assertEquals("poi-1", advice.choices().get(0).poiId());
        assertEquals("更贴近高蛋白", advice.choices().get(0).reason());
    }

    @Test
    void recommendShouldMapTimeoutFailuresFromStreaming() {
        TestableAiHttpClient client = new TestableAiHttpClient();
        client.failure = new RuntimeException(new TimeoutException("slow"));

        BusinessException ex = assertThrows(BusinessException.class, () -> client.recommend(recommendationRequest()));

        assertEquals(ErrorCode.AI_UPSTREAM_TIMEOUT, ex.getErrorCode());
    }

    private AiHttpClient createClient(HttpClient httpClient) {
        AiHttpClient client = new AiHttpClient(
                mock(RestClient.Builder.class),
                new AiServiceProperties(),
                new SimpleMeterRegistry());
        ReflectionTestUtils.setField(client, "httpClient", httpClient);
        return client;
    }

    private HttpResponse<String> stringResponse(int statusCode, String body) {
        return new FakeHttpResponse<>(statusCode, body);
    }

    private HttpResponse<Stream<String>> lineResponse(int statusCode, Stream<String> body) {
        return new FakeHttpResponse<>(statusCode, body);
    }

    private AiAssistantClient.RecommendationRequest recommendationRequest() {
        return new AiAssistantClient.RecommendationRequest(
                "想吃高蛋白",
                List.of(new AiAssistantClient.RecommendationCandidate(
                        "poi-1",
                        "鸡胸肉能量碗",
                        "学林街",
                        "轻食",
                        200,
                        null,
                        0,
                        30,
                        List.of("高蛋白"),
                        "适合健身后吃",
                        List.of("健身友好"))),
                null);
    }

    static class TestableAiHttpClient extends AiHttpClient {

        private List<AiAssistantClient.RecommendationStreamEvent> events = List.of();
        private RuntimeException failure;

        TestableAiHttpClient() {
            super(mock(RestClient.Builder.class), new AiServiceProperties(), new SimpleMeterRegistry());
        }

        @Override
        public void streamRecommend(
                AiAssistantClient.RecommendationRequest request,
                Consumer<AiAssistantClient.RecommendationStreamEvent> eventConsumer) {
            if (failure != null) {
                throw failure;
            }
            events.forEach(eventConsumer);
        }
    }

    static class FakeHttpResponse<T> implements HttpResponse<T> {

        private final int statusCode;
        private final T body;

        FakeHttpResponse(int statusCode, T body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder().uri(URI.create("http://localhost")).build();
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://localhost");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
