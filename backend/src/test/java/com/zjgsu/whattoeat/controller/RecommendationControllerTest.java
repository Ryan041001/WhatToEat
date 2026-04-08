package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(RecommendationControllerTest.TestConfig.class)
class RecommendationControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBlacklistRepository userBlacklistRepository;

    @Autowired
    private StubAmapClient amapClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userBlacklistRepository.deleteAll();
        userRepository.deleteAll();
        amapClient.reset();
    }

    @Test
    void randomShouldReturnRecommendationWithReason() throws Exception {
        AmapPoi poi = new AmapPoi("poi-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(poi), 1));

        mockMvc.perform(get("/api/v1/recommendations/random")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.poiId").value("poi-1"))
                .andExpect(jsonPath("$.data.name").value("沙县小吃"))
                .andExpect(jsonPath("$.data.reason").value("符合筛选条件的随机结果"));

        assertEquals(List.of(1), amapClient.requestedPages());
        assertNotNull(Search.in(meterRegistry).name("amap.client.calls").counter());
        assertNotNull(Search.in(meterRegistry).name("recommendation.requests")
                .tag("endpoint", "random")
                .tag("result", "success")
                .counter());
    }

    @Test
    void cardsShouldFilterBlacklistedPoiIds() throws Exception {
        UserEntity user = createUser("mock-openid-recommendation-007", "Alice");
        AmapPoi blocked = new AmapPoi("poi-blocked", "不想吃", "学林街", 120.35, 30.31, "餐饮", 100);
        AmapPoi allowed = new AmapPoi("poi-allowed", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(blocked, allowed), 2));
        userBlacklistRepository.save(blacklist(user.getId(), "poi-blocked"));

        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("userId", String.valueOf(user.getId()))
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].poiId").value("poi-allowed"));

        assertEquals(List.of(1), amapClient.requestedPages());
        assertNotNull(Search.in(meterRegistry).name("recommendation.requests")
                .tag("endpoint", "cards")
                .tag("result", "success")
                .counter());
    }

    @Test
    void randomShouldContinueToLaterPagesBeforeReturningNoResult() throws Exception {
        UserEntity user = createUser("mock-openid-recommendation-008", "Bob");
        AmapPoi blocked1 = new AmapPoi("poi-blocked-1", "不想吃1", "学林街", 120.35, 30.31, "餐饮", 100);
        AmapPoi blocked2 = new AmapPoi("poi-blocked-2", "不想吃2", "学林街", 120.35, 30.31, "餐饮", 120);
        AmapPoi allowed = new AmapPoi("poi-allowed-2", "后页可吃", "文泽路", 120.36, 30.32, "餐饮", 220);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(blocked1, blocked2), 3));
        amapClient.addNearbyPage(2, new AmapClient.AmapSearchResult(List.of(allowed), 3));
        userBlacklistRepository.save(blacklist(user.getId(), "poi-blocked-1"));
        userBlacklistRepository.save(blacklist(user.getId(), "poi-blocked-2"));

        mockMvc.perform(get("/api/v1/recommendations/random")
                        .param("userId", String.valueOf(user.getId()))
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.poiId").value("poi-allowed-2"));

        assertEquals(List.of(1, 2), amapClient.requestedPages());
        assertEquals(List.of(
                new StubAmapClient.NearbyRequest(1000, 1, 20),
                new StubAmapClient.NearbyRequest(1000, 2, 20)),
                amapClient.requestedSearches());
    }

    @Test
    void cardsShouldFetchAdditionalPagesUntilRequestedSizeReached() throws Exception {
        UserEntity user = createUser("mock-openid-recommendation-009", "Cathy");
        AmapPoi blocked = new AmapPoi("poi-blocked", "不想吃", "学林街", 120.35, 30.31, "餐饮", 100);
        AmapPoi allowed1 = new AmapPoi("poi-allowed-1", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        AmapPoi allowed2 = new AmapPoi("poi-allowed-2", "黄焖鸡", "学正街", 120.37, 30.33, "餐饮", 260);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(blocked, allowed1), 4));
        amapClient.addNearbyPage(2, new AmapClient.AmapSearchResult(List.of(allowed2, blocked), 4));
        userBlacklistRepository.save(blacklist(user.getId(), "poi-blocked"));

        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("userId", String.valueOf(user.getId()))
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].poiId").value("poi-allowed-1"))
                .andExpect(jsonPath("$.data[1].poiId").value("poi-allowed-2"));

        assertEquals(List.of(1, 2), amapClient.requestedPages());
        assertEquals(List.of(
                new StubAmapClient.NearbyRequest(1000, 1, 2),
                new StubAmapClient.NearbyRequest(1000, 2, 2)),
                amapClient.requestedSearches());
    }

    @Test
    void cardsShouldReturn404WhenAllPagesAreExhaustedAfterFiltering() throws Exception {
        UserEntity user = createUser("mock-openid-recommendation-010", "David");
        AmapPoi blocked1 = new AmapPoi("poi-blocked-1", "不想吃1", "学林街", 120.35, 30.31, "餐饮", 100);
        AmapPoi blocked2 = new AmapPoi("poi-blocked-2", "不想吃2", "文泽路", 120.36, 30.32, "餐饮", 200);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(blocked1), 2));
        amapClient.addNearbyPage(2, new AmapClient.AmapSearchResult(List.of(blocked2), 2));
        userBlacklistRepository.save(blacklist(user.getId(), "poi-blocked-1"));
        userBlacklistRepository.save(blacklist(user.getId(), "poi-blocked-2"));

        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("userId", String.valueOf(user.getId()))
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3003));

        assertEquals(List.of(1, 2), amapClient.requestedPages());
    }

    @Test
    void randomShouldMapUpstreamErrorTo502() throws Exception {
        amapClient.addNearbyFailure(1, new BusinessException(ErrorCode.AMAP_UPSTREAM_ERROR));

        mockMvc.perform(get("/api/v1/recommendations/random")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(3001));

        assertNotNull(Search.in(meterRegistry).name("recommendation.requests")
                .tag("endpoint", "random")
                .tag("result", ErrorCode.AMAP_UPSTREAM_ERROR.name())
                .counter());
    }

    @Test
    void cardsShouldMapUpstreamTimeoutTo504() throws Exception {
        amapClient.addNearbyFailure(1, new BusinessException(ErrorCode.AMAP_UPSTREAM_TIMEOUT));

        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "3"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(3002));

        assertNotNull(Search.in(meterRegistry).name("recommendation.requests")
                .tag("endpoint", "cards")
                .tag("result", ErrorCode.AMAP_UPSTREAM_TIMEOUT.name())
                .counter());
    }

    @Test
    void randomShouldReturn404WhenProvidedUserDoesNotExist() throws Exception {
        AmapPoi poi = new AmapPoi("poi-1", "沙县小吃", "学林街", 120.35, 30.31, "餐饮", 180);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(poi), 1));

        mockMvc.perform(get("/api/v1/recommendations/random")
                        .param("userId", "999999")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1002));

        assertNotNull(Search.in(meterRegistry).name("recommendation.requests")
                .tag("endpoint", "random")
                .tag("result", ErrorCode.USER_NOT_FOUND.name())
                .counter());
    }

    @Test
    void cardsShouldReturn400WhenUserIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("userId", "0")
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    private UserBlacklistEntity blacklist(Long userId, String poiId) {
        UserBlacklistEntity entity = new UserBlacklistEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        return entity;
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        StubAmapClient amapClient(MeterRegistry meterRegistry) {
            return new StubAmapClient(meterRegistry);
        }
    }

    static class StubAmapClient implements AmapClient {

        private final MeterRegistry meterRegistry;
        private final Map<Integer, AmapSearchResult> nearbyPages = new HashMap<>();
        private final Map<Integer, RuntimeException> nearbyFailures = new HashMap<>();
        private final List<Integer> requestedPages = new ArrayList<>();
        private final List<NearbyRequest> requestedSearches = new ArrayList<>();

        StubAmapClient(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        void addNearbyPage(int page, AmapSearchResult result) {
            nearbyPages.put(page, result);
        }

        void addNearbyFailure(int page, RuntimeException ex) {
            nearbyFailures.put(page, ex);
        }

        List<Integer> requestedPages() {
            return List.copyOf(requestedPages);
        }

        List<NearbyRequest> requestedSearches() {
            return List.copyOf(requestedSearches);
        }

        void reset() {
            nearbyPages.clear();
            nearbyFailures.clear();
            requestedPages.clear();
            requestedSearches.clear();
        }

        @Override
        public AmapSearchResult searchNearby(double longitude, double latitude, int radius, int page, int pageSize) {
            requestedPages.add(page);
            requestedSearches.add(new NearbyRequest(radius, page, pageSize));
            RuntimeException failure = nearbyFailures.get(page);
            if (failure != null) {
                Counter.builder("amap.client.calls")
                        .tag("operation", "nearby")
                        .tag("result", failure instanceof BusinessException businessException
                                ? businessException.getErrorCode().name()
                                : ErrorCode.AMAP_UPSTREAM_ERROR.name())
                        .register(meterRegistry)
                        .increment();
                throw failure;
            }
            Counter.builder("amap.client.calls")
                    .tag("operation", "nearby")
                    .tag("result", "success")
                    .register(meterRegistry)
                    .increment();
            return nearbyPages.getOrDefault(page, new AmapSearchResult(List.of(), 0));
        }

        @Override
        public AmapSearchResult searchByKeyword(String keyword, double longitude, double latitude, int radius, int page, int pageSize) {
            throw new UnsupportedOperationException("not used in recommendation tests");
        }

        record NearbyRequest(int radius, int page, int pageSize) {
        }
    }
}
