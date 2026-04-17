package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.RecommendationFeedbackEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantMetricSnapshotEntity;
import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.model.entity.UserBlacklistEntity;
import com.zjgsu.whattoeat.model.entity.UserChoiceHistoryEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.model.entity.UserRestaurantNoteEntity;
import com.zjgsu.whattoeat.repository.RecommendationFeedbackRepository;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
import com.zjgsu.whattoeat.repository.UserChoiceHistoryRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.repository.UserRestaurantNoteRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserPreferenceProfileControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantReviewRepository restaurantReviewRepository;

    @Autowired
    private UserRestaurantNoteRepository userRestaurantNoteRepository;

    @Autowired
    private UserBlacklistRepository userBlacklistRepository;

    @Autowired
    private UserChoiceHistoryRepository userChoiceHistoryRepository;

    @Autowired
    private RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;

    @Autowired
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        recommendationFeedbackRepository.deleteAll();
        restaurantReviewRepository.deleteAll();
        restaurantMetricSnapshotRepository.deleteAll();
        userRestaurantNoteRepository.deleteAll();
        userBlacklistRepository.deleteAll();
        userChoiceHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getPreferenceProfileShouldAggregatePreferenceSignalsFromCurrentData() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-profile-001", "Alice");
        String token = loginAndExtractToken("mock-code-profile-001", "Alice");
        createReview(user.getId(), "B0FFPROF001", "轻食能量碗", "健身后会来吃，整体清淡，鸡胸肉和热汤都不错", "4.5", 32);
        createNote(user.getId(), "B0FFPROF001", "适合工作日午餐，想吃热汤时可以来");
        createBlacklist(user.getId(), "B0FFPROF099", "太油了");
        createChoice(user.getId(), "B0FFPROF001", "轻食能量碗");
        createSnapshot("B0FFPROF001", 32, "高蛋白", "清淡", "整体比较轻盈，午餐友好");
        createFeedback(user.getId(), "B0FFPROF001", "TOO_EXPENSIVE", "最近更想控制预算，健身期想吃轻一点");

        mockMvc.perform(get("/api/v1/users/{userId}/preference-profile", user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.blacklistCount").value(1))
                .andExpect(jsonPath("$.data.recentChoiceCount").value(1))
                .andExpect(jsonPath("$.data.avgPerCapitaBudget").value(32))
                .andExpect(jsonPath("$.data.preferredTags").isArray())
                .andExpect(jsonPath("$.data.preferredTags[0]").value("清淡"))
                .andExpect(jsonPath("$.data.avoidedTags").isArray())
                .andExpect(jsonPath("$.data.avoidedTags[0]").value("太油"))
                .andExpect(jsonPath("$.data.recentFeedbackSignals[0]").value("更在意预算"))
                .andExpect(jsonPath("$.data.summary").isString());
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.saveAndFlush(user);
    }

    private void createReview(Long userId, String poiId, String poiName, String content, String rating, int price) {
        RestaurantReviewEntity entity = new RestaurantReviewEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiNameSnapshot(poiName);
        entity.setContent(content);
        entity.setRatingScore(new BigDecimal(rating));
        entity.setPerCapitaPrice(price);
        restaurantReviewRepository.saveAndFlush(entity);
    }

    private void createNote(Long userId, String poiId, String content) {
        UserRestaurantNoteEntity entity = new UserRestaurantNoteEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setNote(content);
        userRestaurantNoteRepository.saveAndFlush(entity);
    }

    private void createBlacklist(Long userId, String poiId, String reason) {
        UserBlacklistEntity entity = new UserBlacklistEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setReason(reason);
        userBlacklistRepository.saveAndFlush(entity);
    }

    private void createChoice(Long userId, String poiId, String poiName) {
        UserChoiceHistoryEntity entity = new UserChoiceHistoryEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiName(poiName);
        entity.setChosenAt(LocalDateTime.of(2026, 4, 18, 12, 0));
        userChoiceHistoryRepository.saveAndFlush(entity);
    }

    private void createSnapshot(String poiId, int avgPrice, String aiTag1, String aiTag2, String aiSummary) {
        RestaurantMetricSnapshotEntity entity = new RestaurantMetricSnapshotEntity();
        entity.setPoiId(poiId);
        entity.setReviewCount(1);
        entity.setAvgRating(new BigDecimal("4.5"));
        entity.setAvgPerCapitaPrice(avgPrice);
        entity.setAiTag1(aiTag1);
        entity.setAiTag2(aiTag2);
        entity.setAiSummary(aiSummary);
        entity.setAiStatus("ready");
        restaurantMetricSnapshotRepository.saveAndFlush(entity);
    }

    private void createFeedback(Long userId, String poiId, String feedbackType, String detail) {
        RecommendationFeedbackEntity entity = new RecommendationFeedbackEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setFeedbackType(feedbackType);
        entity.setDetail(detail);
        recommendationFeedbackRepository.saveAndFlush(entity);
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
