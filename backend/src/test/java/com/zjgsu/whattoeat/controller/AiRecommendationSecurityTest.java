package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.infrastructure.ai.AiAssistantClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(RecommendationControllerTest.TestConfig.class)
class AiRecommendationSecurityTest {

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
    private RecommendationControllerTest.StubAmapClient amapClient;

    @Autowired
    private RecommendationControllerTest.StubAiAssistantClient aiAssistantClient;

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
    void askShouldSanitizeQuestionAndContextBeforeSendingToAiService() throws Exception {
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(
                new AmapPoi("poi-noodle", "兰州拉面", "文泽路", 120.36, 30.32, "餐饮", 220)
        ), 1));
        aiAssistantClient.setRecommendationAdvice(new AiAssistantClient.RecommendationAdvice(
                "先看兰州拉面。",
                List.of(new AiAssistantClient.RecommendationChoice("poi-noodle", "热汤稳定"))));

        mockMvc.perform(post("/api/v1/recommendations/ask")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 120.35,
                                  "latitude": 30.31,
                                  "radius": 1000,
                                  "size": 1,
                                  "question": "<script>alert('xss')</script>想吃面",
                                  "context": {
                                    "previousQuestion": "<img src=x onerror=alert(1)>预算 35 以内",
                                    "userSignals": ["<b>健身</b>"]
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        assertEquals("想吃面", aiAssistantClient.lastRecommendationRequest().question());
        assertEquals("预算 35 以内", aiAssistantClient.lastRecommendationRequest().context().previousQuestion());
        assertEquals(List.of("健身"), aiAssistantClient.lastRecommendationRequest().context().userSignals());
    }

    @Test
    void askStreamShouldSanitizeQuestionBeforeSendingToAiService() throws Exception {
        amapClient.addNearbyPage(1, new AmapClient.AmapSearchResult(List.of(
                new AmapPoi("poi-rice", "桂香卤味拌饭", "学林街", 120.35, 30.31, "餐饮", 180)
        ), 1));
        aiAssistantClient.setRecommendationAdvice(new AiAssistantClient.RecommendationAdvice(
                "先看桂香卤味拌饭。",
                List.of(new AiAssistantClient.RecommendationChoice("poi-rice", "出餐快"))));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/recommendations/ask/stream")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 120.35,
                                  "latitude": 30.31,
                                  "radius": 1000,
                                  "size": 1,
                                  "question": "<svg onload=alert(1)>想吃饭"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        assertEquals("想吃饭", aiAssistantClient.lastRecommendationRequest().question());
    }
}
