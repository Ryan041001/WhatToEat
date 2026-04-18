package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.RecommendationFeedbackRepository;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserRecommendationFeedbackControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

    @Autowired
    private UserChoiceHistoryRepository userChoiceHistoryRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        recommendationFeedbackRepository.deleteAll();
        userChoiceHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createFeedbackShouldPersistRecordAndMirrorAlreadyEatenIntoChoiceHistory() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-feedback-001", "Alice");
        String token = loginAndExtractToken("mock-code-feedback-001", "Alice");

        mockMvc.perform(post("/api/v1/users/{userId}/recommendation-feedback", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "poiId": "B0FFFDBK001",
                                  "poiNameSnapshot": "轻食工坊",
                                  "feedbackType": "ALREADY_ATE",
                                  "detail": "中午刚吃过，晚上想换一家",
                                  "requestQuestion": "在健身，想吃高蛋白"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(recommendationFeedbackRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent())
                .extracting(RecommendationFeedbackEntity::getFeedbackType)
                .containsExactly("ALREADY_ATE");
        assertThat(userChoiceHistoryRepository.findByUserIdOrderByChosenAtDesc(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent())
                .extracting(UserChoiceHistoryEntity::getPoiId)
                .containsExactly("B0FFFDBK001");
    }

    @Test
    void listFeedbackShouldReturnPaginatedItems() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-feedback-002", "Bob");
        String token = loginAndExtractToken("mock-code-feedback-002", "Bob");
        createFeedback(user.getId(), "B0FFFDBK101", "TOO_FAR", "太远了");
        createFeedback(user.getId(), "B0FFFDBK102", "TOO_EXPENSIVE", "最近想控制预算");

        mockMvc.perform(get("/api/v1/users/{userId}/recommendation-feedback", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].feedbackType").value("TOO_EXPENSIVE"))
                .andExpect(jsonPath("$.data.items[1].feedbackType").value("TOO_FAR"));
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.saveAndFlush(user);
    }

    private RecommendationFeedbackEntity createFeedback(Long userId, String poiId, String feedbackType, String detail) {
        RecommendationFeedbackEntity entity = new RecommendationFeedbackEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setFeedbackType(feedbackType);
        entity.setDetail(detail);
        return recommendationFeedbackRepository.saveAndFlush(entity);
    }

    private String loginAndExtractToken(String code, String nickname) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extractToken(loginResult.getResponse().getContentAsString());
    }

    private String extractToken(String responseBody) {
        Matcher matcher = TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("token not found in response: " + responseBody);
        }
        return matcher.group(1);
    }
}
