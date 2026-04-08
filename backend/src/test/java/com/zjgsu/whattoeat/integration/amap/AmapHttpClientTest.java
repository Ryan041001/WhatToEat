package com.zjgsu.whattoeat.integration.amap;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.AmapProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AmapHttpClientTest {

    private SimpleMeterRegistry meterRegistry;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private AmapHttpClient amapHttpClient;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        amapHttpClient = new AmapHttpClient(
                restClientBuilder,
                new AmapProperties("test-key", "https://restapi.amap.com", 2),
                meterRegistry);
    }

    @Test
    void searchNearbyShouldRecordSuccessMetrics() {
        mockServer.expect(once(), requestTo("https://restapi.amap.com/v3/place/around?key=test-key&location=120.35,30.31&radius=1000&types=050000&offset=10&page=1&output=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "1",
                          "count": "1",
                          "pois": [
                            {
                              "id": "poi-1",
                              "name": "沙县小吃",
                              "address": "学林街",
                              "location": "120.35,30.31",
                              "type": "餐饮",
                              "distance": "180"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AmapClient.AmapSearchResult result = amapHttpClient.searchNearby(120.35, 30.31, 1000, 1, 10);

        assertEquals(1, result.items().size());
        assertEquals(1.0, meterRegistry.get("amap.client.calls")
                .tag("operation", "nearby")
                .tag("result", "success")
                .counter()
                .count());
        assertEquals(1L, meterRegistry.get("amap.client.latency")
                .tag("operation", "nearby")
                .timer()
                .count());
        mockServer.verify();
    }

    @Test
    void searchByKeywordShouldRecordUpstreamErrorMetric() {
        mockServer.expect(once(), requestTo("https://restapi.amap.com/v3/place/around?key=test-key&location=120.36,30.32&radius=1500&types=050000&offset=5&page=2&output=json&keywords=%E6%8B%89%E9%9D%A2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "0",
                          "count": "0",
                          "pois": []
                        }
                        """, MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> amapHttpClient.searchByKeyword("拉面", 120.36, 30.32, 1500, 2, 5));

        assertEquals(ErrorCode.AMAP_UPSTREAM_ERROR, ex.getErrorCode());
        assertEquals(1.0, meterRegistry.get("amap.client.calls")
                .tag("operation", "keyword")
                .tag("result", ErrorCode.AMAP_UPSTREAM_ERROR.name())
                .counter()
                .count());
        assertEquals(1L, meterRegistry.get("amap.client.latency")
                .tag("operation", "keyword")
                .timer()
                .count());
        mockServer.verify();
    }

    @Test
    void searchNearbyShouldRecordTimeoutMetric() {
        mockServer.expect(once(), requestTo("https://restapi.amap.com/v3/place/around?key=test-key&location=120.35,30.31&radius=1000&types=050000&offset=10&page=1&output=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new ResourceAccessException("timeout", new SocketTimeoutException("read timed out"));
                });

        BusinessException ex = assertThrows(BusinessException.class,
                () -> amapHttpClient.searchNearby(120.35, 30.31, 1000, 1, 10));

        assertEquals(ErrorCode.AMAP_UPSTREAM_TIMEOUT, ex.getErrorCode());
        assertEquals(1.0, meterRegistry.get("amap.client.calls")
                .tag("operation", "nearby")
                .tag("result", ErrorCode.AMAP_UPSTREAM_TIMEOUT.name())
                .counter()
                .count());
        assertEquals(1L, meterRegistry.get("amap.client.latency")
                .tag("operation", "nearby")
                .timer()
                .count());
        mockServer.verify();
    }
}
