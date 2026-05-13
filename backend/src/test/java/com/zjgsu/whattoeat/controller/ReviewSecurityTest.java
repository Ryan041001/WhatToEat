package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.RestaurantMetricSnapshotRepository;
import com.zjgsu.whattoeat.repository.RestaurantReviewRepository;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ReviewSecurityTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        restaurantReviewRepository.deleteAll();
        restaurantMetricSnapshotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void putReviewShouldStorePlainTextContentAfterRemovingHtmlAndScripts() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-review-security-001", "Alice");
        String token = loginAndExtractToken("mock-code-review-security-001", "Alice");

        mockMvc.perform(put("/api/v1/users/{userId}/restaurant-reviews/{poiId}", user.getId(), "B0FFSEC001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "poiNameSnapshot": "测试餐厅",
                                  "ratingScore": 4.5,
                                  "perCapitaPrice": 28,
                                  "content": "<script>alert('xss')</script><b>出餐快</b>"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("出餐快"))
                .andExpect(content().string(not(containsString("<script"))))
                .andExpect(content().string(not(containsString("alert"))))
                .andExpect(content().string(not(containsString("<b>"))));

        var saved = restaurantReviewRepository.findByUserIdAndPoiId(user.getId(), "B0FFSEC001").orElseThrow();
        assertThat(saved.getContent()).isEqualTo("出餐快");
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.saveAndFlush(user);
    }

    private String loginAndExtractToken(String code, String nickname) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .header("X-CSRF-Token", "test-csrf")
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
