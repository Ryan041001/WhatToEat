package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.RestaurantReviewEntity;
import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.service.application.RestaurantMetricAggregationService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class RestaurantReviewControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantReviewRepository restaurantReviewRepository;

    @Autowired
    private RestaurantMetricSnapshotRepository restaurantMetricSnapshotRepository;

    @Autowired
    private RestaurantMetricAggregationService restaurantMetricAggregationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        restaurantReviewRepository.deleteAll();
        restaurantMetricSnapshotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void putReviewShouldCreateOrUpdateCurrentUserReview() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-review-controller-001", "Alice");
        String token = loginAndExtractToken("mock-code-review-controller-001", "Alice");

        mockMvc.perform(put("/api/v1/users/{userId}/restaurant-reviews/{poiId}", user.getId(), "B0FFREVIEW001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"poiNameSnapshot":"沙县小吃","ratingScore":4.5,"perCapitaPrice":28,"content":"出餐快"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.poiId").value("B0FFREVIEW001"))
                .andExpect(jsonPath("$.data.ratingScore").value(4.5))
                .andExpect(jsonPath("$.data.perCapitaPrice").value(28))
                .andExpect(jsonPath("$.data.content").value("出餐快"));

        RestaurantReviewEntity saved = restaurantReviewRepository.findByUserIdAndPoiId(user.getId(), "B0FFREVIEW001")
                .orElseThrow();
        assertThat(saved.getRatingScore()).isEqualByComparingTo("4.5");
        assertThat(saved.getPerCapitaPrice()).isEqualTo(28);
        assertThat(saved.getContent()).isEqualTo("出餐快");
    }

    @Test
    void getReviewShouldReturnCurrentUserReview() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-review-controller-002", "Bob");
        String token = loginAndExtractToken("mock-code-review-controller-002", "Bob");
        saveReview(user.getId(), "B0FFREVIEW002", "港记云吞面", "整体清爽", "4.0", 26,
                LocalDateTime.of(2026, 4, 17, 12, 0),
                LocalDateTime.of(2026, 4, 17, 12, 30));

        mockMvc.perform(get("/api/v1/users/{userId}/restaurant-reviews/{poiId}", user.getId(), "B0FFREVIEW002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.poiId").value("B0FFREVIEW002"))
                .andExpect(jsonPath("$.data.ratingScore").value(4.0))
                .andExpect(jsonPath("$.data.perCapitaPrice").value(26))
                .andExpect(jsonPath("$.data.content").value("整体清爽"));
    }

    @Test
    void putReviewShouldReturn400WhenRatingIsNotHalfStep() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-review-controller-003", "Cathy");
        String token = loginAndExtractToken("mock-code-review-controller-003", "Cathy");

        mockMvc.perform(put("/api/v1/users/{userId}/restaurant-reviews/{poiId}", user.getId(), "B0FFREVIEW003")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"poiNameSnapshot":"沙县小吃","ratingScore":4.3,"perCapitaPrice":22,"content":"还可以"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2102));
    }

    @Test
    void putReviewShouldReturn401WhenTokenUserDoesNotMatchPathUser() throws Exception {
        UserEntity owner = createUser("mock-openid-mock-code-review-controller-004", "David");
        createUser("mock-openid-mock-code-review-controller-005", "Eva");
        String otherToken = loginAndExtractToken("mock-code-review-controller-005", "Eva");

        mockMvc.perform(put("/api/v1/users/{userId}/restaurant-reviews/{poiId}", owner.getId(), "B0FFREVIEW004")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"poiNameSnapshot":"沙县小吃","ratingScore":4.5,"perCapitaPrice":30,"content":"不该成功"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void deleteReviewShouldRemoveCurrentUserReview() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-review-controller-006", "Frank");
        String token = loginAndExtractToken("mock-code-review-controller-006", "Frank");
        saveReview(user.getId(), "B0FFREVIEW006", "福记川味馆", "比较下饭", "5.0", 35,
                LocalDateTime.of(2026, 4, 17, 14, 0),
                LocalDateTime.of(2026, 4, 17, 14, 20));

        mockMvc.perform(delete("/api/v1/users/{userId}/restaurant-reviews/{poiId}", user.getId(), "B0FFREVIEW006")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(restaurantReviewRepository.findByUserIdAndPoiId(user.getId(), "B0FFREVIEW006")).isEmpty();
    }

    @Test
    void listPublicReviewsShouldReturnPaginatedItemsOrderedByUpdatedAtDesc() throws Exception {
        UserEntity user1 = createUser("mock-openid-public-review-001", "Gina");
        UserEntity user2 = createUser("mock-openid-public-review-002", "Henry");
        saveReview(user1.getId(), "B0FFREVIEW101", "南巷酸菜鱼", "先写的评论", "4.0", 38,
                LocalDateTime.of(2026, 4, 17, 15, 0),
                LocalDateTime.of(2026, 4, 17, 15, 5));
        saveReview(user2.getId(), "B0FFREVIEW101", "南巷酸菜鱼", "后写的评论", "4.5", 42,
                LocalDateTime.of(2026, 4, 17, 16, 0),
                LocalDateTime.of(2026, 4, 17, 16, 10));

        mockMvc.perform(get("/api/v1/restaurants/{poiId}/reviews", "B0FFREVIEW101")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].content").value("后写的评论"))
                .andExpect(jsonPath("$.data.items[0].ratingScore").value(4.5))
                .andExpect(jsonPath("$.data.items[1].content").value("先写的评论"));
    }

    @Test
    void getReviewSummaryShouldReturnAggregatedMetricsAndAiTags() throws Exception {
        UserEntity user1 = createUser("mock-openid-public-review-003", "Ivy");
        UserEntity user2 = createUser("mock-openid-public-review-004", "Jack");
        saveReview(user1.getId(), "B0FFREVIEW102", "港记云吞面", "整体清爽", "4.0", 26,
                LocalDateTime.of(2026, 4, 17, 17, 0),
                LocalDateTime.of(2026, 4, 17, 17, 10));
        saveReview(user2.getId(), "B0FFREVIEW102", "港记云吞面", "午餐友好", "5.0", 30,
                LocalDateTime.of(2026, 4, 17, 18, 0),
                LocalDateTime.of(2026, 4, 17, 18, 15));

        restaurantMetricAggregationService.refreshSnapshot("B0FFREVIEW102");
        var snapshot = restaurantMetricSnapshotRepository.findById("B0FFREVIEW102").orElseThrow();
        snapshot.setAiTag1("清爽");
        snapshot.setAiTag2("午餐友好");
        snapshot.setAiSummary("整体口味偏清淡，适合工作日午餐。");
        snapshot.setAiStatus("ready");
        restaurantMetricSnapshotRepository.saveAndFlush(snapshot);

        mockMvc.perform(get("/api/v1/restaurants/{poiId}/review-summary", "B0FFREVIEW102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reviewCount").value(2))
                .andExpect(jsonPath("$.data.avgRating").value(4.5))
                .andExpect(jsonPath("$.data.avgPerCapitaPrice").value(28))
                .andExpect(jsonPath("$.data.aiTags[0]").value("清爽"))
                .andExpect(jsonPath("$.data.aiTags[1]").value("午餐友好"))
                .andExpect(jsonPath("$.data.aiSummary").value("整体口味偏清淡，适合工作日午餐。"));
    }


    @Test
    void getReviewSummaryShouldExposeScenarioHintsForDetailPage() throws Exception {
        UserEntity user1 = createUser("mock-openid-public-review-006", "Liam");
        UserEntity user2 = createUser("mock-openid-public-review-007", "Mia");
        saveReview(user1.getId(), "B0FFREVIEW104", "能量汤面", "整体清淡，工作日中午来很方便", "4.0", 28,
                LocalDateTime.of(2026, 4, 17, 20, 0),
                LocalDateTime.of(2026, 4, 17, 20, 10));
        saveReview(user2.getId(), "B0FFREVIEW104", "能量汤面", "热汤舒服，一个人吃也很快", "4.5", 30,
                LocalDateTime.of(2026, 4, 17, 20, 30),
                LocalDateTime.of(2026, 4, 17, 20, 40));

        restaurantMetricAggregationService.refreshSnapshot("B0FFREVIEW104");
        var snapshot = restaurantMetricSnapshotRepository.findById("B0FFREVIEW104").orElseThrow();
        snapshot.setAiTag1("清淡");
        snapshot.setAiTag2("热汤");
        snapshot.setAiSummary("适合工作日午餐，也适合一个人想吃点热乎的时候。");
        snapshot.setAiStatus("ready");
        restaurantMetricSnapshotRepository.saveAndFlush(snapshot);

        mockMvc.perform(get("/api/v1/restaurants/{poiId}/review-summary", "B0FFREVIEW104"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.recommendedScenarios.length()").value(4))
                .andExpect(jsonPath("$.data.recommendedScenarios[0]").value("工作日午餐"))
                .andExpect(jsonPath("$.data.recommendedScenarios[1]").value("一个人快吃"))
                .andExpect(jsonPath("$.data.recommendedScenarios[2]").value("想吃热汤时"))
                .andExpect(jsonPath("$.data.recommendedScenarios[3]").value("预算 30 左右"));
    }

    @Test
    void getReviewSummaryShouldHideStaleAiFieldsWhenSnapshotIsFailed() throws Exception {
        UserEntity user = createUser("mock-openid-public-review-005", "Kate");
        saveReview(user.getId(), "B0FFREVIEW103", "桂香卤味拌饭", "价格稳定", "4.0", 24,
                LocalDateTime.of(2026, 4, 17, 19, 0),
                LocalDateTime.of(2026, 4, 17, 19, 10));

        restaurantMetricAggregationService.refreshSnapshot("B0FFREVIEW103");
        var snapshot = restaurantMetricSnapshotRepository.findById("B0FFREVIEW103").orElseThrow();
        snapshot.setAiTag1("旧标签");
        snapshot.setAiTag2("旧摘要标签");
        snapshot.setAiSummary("这是一条不应继续对外暴露的旧摘要。");
        snapshot.setAiStatus("failed");
        restaurantMetricSnapshotRepository.saveAndFlush(snapshot);

        mockMvc.perform(get("/api/v1/restaurants/{poiId}/review-summary", "B0FFREVIEW103"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.avgRating").value(4.0))
                .andExpect(jsonPath("$.data.avgPerCapitaPrice").value(24))
                .andExpect(jsonPath("$.data.aiTags.length()").value(0))
                .andExpect(jsonPath("$.data.aiSummary").isEmpty());
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.saveAndFlush(user);
    }

    private void saveReview(
            Long userId,
            String poiId,
            String poiNameSnapshot,
            String content,
            String ratingScore,
            int perCapitaPrice,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        RestaurantReviewEntity entity = new RestaurantReviewEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setPoiNameSnapshot(poiNameSnapshot);
        entity.setContent(content);
        entity.setRatingScore(new BigDecimal(ratingScore));
        entity.setPerCapitaPrice(perCapitaPrice);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        restaurantReviewRepository.saveAndFlush(entity);
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
