package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.integration.ai.AiAssistantClient;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
    private UserChoiceHistoryRepository userChoiceHistoryRepository;

    @Autowired
    private RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;

    @Autowired
    private StubAmapClient amapClient;

    @Autowired
    private StubAiAssistantClient aiAssistantClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userBlacklistRepository.deleteAll();
        userChoiceHistoryRepository.deleteAll();
        restaurantMetricSnapshotRepository.deleteAll();
        userRepository.deleteAll();
        amapClient.reset();
        aiAssistantClient.reset();
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
    void cardsShouldPreferNonRecentChoicesButFallbackWhenOnlyRecentRemain() throws Exception {
        UserEntity user = createUser("mock-openid-recommendation-011", "Ethan");
        createChoiceHistory(user.getId(), "poi-recent", "刚吃过的轻食", java.time.LocalDateTime.of(2026, 4, 18, 12, 0));
        AmapPoi recent = new AmapPoi("poi-recent", "刚吃过的轻食", "学林街", 120.35, 30.31, "餐饮", 100);
        AmapPoi fresh = new AmapPoi("poi-fresh", "新店热汤面", "文泽路", 120.36, 30.32, "餐饮", 180);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(recent, fresh), 2));

        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("userId", String.valueOf(user.getId()))
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].poiId").value("poi-fresh"));

        amapClient.reset();
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(recent), 1));

        mockMvc.perform(get("/api/v1/recommendations/cards")
                        .param("userId", String.valueOf(user.getId()))
                        .param("longitude", "120.35")
                        .param("latitude", "30.31")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].poiId").value("poi-recent"));
    }

    @Test
    void askShouldPassRefineContextAndFilterRejectedAndRecentCandidates() throws Exception {
        UserEntity user = createUser("mock-openid-recommendation-012", "Fiona");
        createChoiceHistory(user.getId(), "poi-recent", "刚吃过的轻食", java.time.LocalDateTime.of(2026, 4, 18, 11, 30));
        AmapPoi recent = new AmapPoi("poi-recent", "刚吃过的轻食", "学林街", 120.35, 30.31, "轻食简餐", 120);
        AmapPoi rejected = new AmapPoi("poi-rejected", "不想再吃的快餐", "文泽路", 120.36, 30.32, "快餐", 160);
        AmapPoi target = new AmapPoi("poi-target", "鸡胸肉能量碗", "学正街", 120.37, 30.33, "轻食", 220);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(recent, rejected, target), 3));
        restaurantMetricSnapshotRepository.save(metricSnapshot("poi-target", "4.7", 18, 31, "高蛋白", "清淡"));
        aiAssistantClient.setRecommendationAdvice(new AiAssistantClient.RecommendationAdvice(
                "在健身的话可以先看鸡胸肉能量碗。",
                List.of(new AiAssistantClient.RecommendationChoice("poi-target", "更贴近高蛋白、清淡的需求"))));

        mockMvc.perform(post("/api/v1/recommendations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "longitude": 120.35,
                                  "latitude": 30.31,
                                  "radius": 1000,
                                  "size": 2,
                                  "question": "换一家，还是想吃高蛋白",
                                  "context": {
                                    "previousQuestion": "预算 35 以内，想吃轻一点",
                                    "rejectedPoiIds": ["poi-rejected"],
                                    "userSignals": ["健身"]
                                  }
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations[0].poiId").value("poi-target"));

        assertEquals(List.of("poi-target"),
                aiAssistantClient.lastRecommendationRequest().candidates().stream()
                        .map(AiAssistantClient.RecommendationCandidate::poiId)
                        .toList());
        assertEquals("预算 35 以内，想吃轻一点", aiAssistantClient.lastRecommendationRequest().context().previousQuestion());
        assertEquals(List.of("健身"), aiAssistantClient.lastRecommendationRequest().context().userSignals());
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

    @Test
    void askShouldReturnAiOrderedRecommendationsWithMergedMetrics() throws Exception {
        AmapPoi noodle = new AmapPoi("poi-noodle", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        AmapPoi rice = new AmapPoi("poi-rice", "桂香卤味拌饭", "学林街", 120.35, 30.31, "餐饮", 180);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(rice, noodle), 2));
        restaurantMetricSnapshotRepository.save(metricSnapshot("poi-rice", "4.1", 12, 18, "出餐快", "学生友好"));
        restaurantMetricSnapshotRepository.save(metricSnapshot("poi-noodle", "4.8", 25, 29, "性价比高", "汤底稳"));
        aiAssistantClient.setRecommendationAdvice(new AiAssistantClient.RecommendationAdvice(
                "预算 30 以内更建议先看兰州拉面，评分和评论都更稳。",
                List.of(
                        new AiAssistantClient.RecommendationChoice("poi-noodle", "评分更高，人均 29 元还在预算内"),
                        new AiAssistantClient.RecommendationChoice("poi-rice", "更近，也适合想吃得快一点"))));

        mockMvc.perform(post("/api/v1/recommendations/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 120.35,
                                  "latitude": 30.31,
                                  "radius": 1000,
                                  "size": 2,
                                  "question": "预算30以内，想吃点带汤的"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.answer").value("预算 30 以内更建议先看兰州拉面，评分和评论都更稳。"))
                .andExpect(jsonPath("$.data.recommendations[0].poiId").value("poi-noodle"))
                .andExpect(jsonPath("$.data.recommendations[0].avgRating").value(4.8))
                .andExpect(jsonPath("$.data.recommendations[0].avgPerCapitaPrice").value(29))
                .andExpect(jsonPath("$.data.recommendations[0].aiTags[0]").value("性价比高"))
                .andExpect(jsonPath("$.data.recommendations[0].matchReason").value("评分更高，人均 29 元还在预算内"))
                .andExpect(jsonPath("$.data.recommendations[1].poiId").value("poi-rice"));

        assertEquals("预算30以内，想吃点带汤的", aiAssistantClient.lastRecommendationRequest().question());
        assertEquals(List.of("poi-rice", "poi-noodle"),
                aiAssistantClient.lastRecommendationRequest().candidates().stream()
                        .map(AiAssistantClient.RecommendationCandidate::poiId)
                        .toList());
    }

    @Test
    void askStreamShouldEmitStructuredRecommendationEvents() throws Exception {
        AmapPoi noodle = new AmapPoi("poi-noodle", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220);
        AmapPoi rice = new AmapPoi("poi-rice", "桂香卤味拌饭", "学林街", 120.35, 30.31, "餐饮", 180);
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(rice, noodle), 2));
        restaurantMetricSnapshotRepository.save(metricSnapshot("poi-rice", "4.1", 12, 18, "出餐快", "学生友好"));
        restaurantMetricSnapshotRepository.save(metricSnapshot("poi-noodle", "4.8", 25, 29, "性价比高", "汤底稳"));
        aiAssistantClient.setRecommendationAdvice(new AiAssistantClient.RecommendationAdvice(
                "预算 30 以内更建议先看兰州拉面，评分和评论都更稳。",
                List.of(new AiAssistantClient.RecommendationChoice("poi-noodle", "评分更高，人均 29 元还在预算内"))));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/recommendations/ask/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 120.35,
                                  "latitude": 30.31,
                                  "radius": 1000,
                                  "size": 2,
                                  "question": "预算30以内，想吃点带汤的"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:session.created")))
                .andExpect(content().string(containsString("event:retrieval.started")))
                .andExpect(content().string(containsString("event:retrieval.completed")))
                .andExpect(content().string(containsString("event:recommendation.card")))
                .andExpect(content().string(containsString("\"poiId\":\"poi-noodle\"")))
                .andExpect(content().string(containsString("\"avgPerCapitaPrice\":29")))
                .andExpect(content().string(containsString("event:answer.delta")))
                .andExpect(content().string(containsString("event:answer.done")))
                .andExpect(content().string(containsString("event:done")));
    }


    private void createChoiceHistory(Long userId, String poiId, String poiName, java.time.LocalDateTime chosenAt) {
        com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity entity = new com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiName(poiName);
        entity.setChosenAt(chosenAt);
        userChoiceHistoryRepository.saveAndFlush(entity);
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

    private RestaurantMetricSnapshotEntity metricSnapshot(
            String poiId,
            String avgRating,
            int reviewCount,
            int avgPerCapitaPrice,
            String aiTag1,
            String aiTag2) {
        RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
        entity.setPoiId(poiId);
        entity.setReviewCount(reviewCount);
        entity.setAvgRating(new BigDecimal(avgRating));
        entity.setAvgPerCapitaPrice(avgPerCapitaPrice);
        entity.setAiTag1(aiTag1);
        entity.setAiTag2(aiTag2);
        entity.setAiStatus("ready");
        return entity;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        StubAmapClient amapClient(MeterRegistry meterRegistry) {
            return new StubAmapClient(meterRegistry);
        }

        @Bean
        @Primary
        StubAiAssistantClient aiAssistantClient() {
            return new StubAiAssistantClient();
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

    static class StubAiAssistantClient implements AiAssistantClient {

        private RecommendationAdvice recommendationAdvice = new RecommendationAdvice("暂时没有合适的推荐。", List.of());
        private RecommendationRequest lastRecommendationRequest;

        void setRecommendationAdvice(RecommendationAdvice recommendationAdvice) {
            this.recommendationAdvice = recommendationAdvice;
        }

        RecommendationRequest lastRecommendationRequest() {
            return lastRecommendationRequest;
        }

        void reset() {
            recommendationAdvice = new RecommendationAdvice("暂时没有合适的推荐。", List.of());
            lastRecommendationRequest = null;
        }

        @Override
        public ReviewTagResult summarizeReviewTags(ReviewTagRequest request) {
            return new ReviewTagResult("出餐快", "学生友好", "评论普遍提到出餐快，学生党接受度高。");
        }

        @Override
        public RecommendationAdvice recommend(RecommendationRequest request) {
            lastRecommendationRequest = request;
            return recommendationAdvice;
        }

        @Override
        public void streamRecommend(RecommendationRequest request, Consumer<RecommendationStreamEvent> eventConsumer) {
            lastRecommendationRequest = request;
            int rank = 1;
            for (RecommendationChoice choice : recommendationAdvice.choices()) {
                Map<String, Object> arguments = new LinkedHashMap<>();
                arguments.put("poiId", choice.poiId());
                arguments.put("reason", choice.reason());
                arguments.put("rank", rank++);
                eventConsumer.accept(new RecommendationStreamEvent("tool.call", Map.of(
                        "toolName", "show_restaurant_card",
                        "arguments", arguments)));
            }
            String answer = recommendationAdvice.answer();
            int chunkSize = 24;
            for (int start = 0; start < answer.length(); start += chunkSize) {
                int end = Math.min(start + chunkSize, answer.length());
                eventConsumer.accept(new RecommendationStreamEvent("answer.delta", Map.of(
                        "delta", answer.substring(start, end))));
            }
            eventConsumer.accept(new RecommendationStreamEvent("answer.done", Map.of("answer", answer)));
            eventConsumer.accept(new RecommendationStreamEvent("done", Map.of("finishReason", "stop")));
        }
    }
}
