package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.model.entity.UserEntity;
import com.zjgsu.whattoeat.repository.UserBlacklistRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserBlacklistControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBlacklistRepository userBlacklistRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userBlacklistRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void addBlacklistShouldReturn201AndPersistRecord() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-blacklist-001", "Alice");
        String token = loginAndExtractToken("mock-code-blacklist-001", "Alice");

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(userBlacklistRepository.existsByUserIdAndPoiId(user.getId(), "B0FF123456")).isTrue();
    }

    @Test
    void addBlacklistShouldReturn401WhenAuthorizationMissing() throws Exception {
        UserEntity user = createUser("mock-openid-blacklist-unauthorized", "NoAuth");

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void addBlacklistShouldReturn401WhenTokenUserDoesNotMatchPathUser() throws Exception {
        UserEntity owner = createUser("mock-openid-blacklist-owner", "Owner");
        createUser("mock-openid-blacklist-other", "Other");
        String otherUserToken = loginAndExtractToken("mock-code-blacklist-other", "Other");

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", owner.getId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void addBlacklistShouldReturn401WhenTokenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", 999999L)
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void addBlacklistShouldReturn409WhenDuplicateRecordExists() throws Exception {
        UserEntity user = createUser("mock-openid-mock-code-blacklist-002", "Bob");
        String token = loginAndExtractToken("mock-code-blacklist-002", "Bob");

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    void addBlacklistShouldReturn400WhenPoiIdTooLong() throws Exception {
        UserEntity user = createUser("mock-openid-blacklist-003", "Cathy");
        String token = loginAndExtractToken("mock-code-blacklist-003", "Cathy");
        String poiId = "B".repeat(65);

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"" + poiId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void addBlacklistShouldReturn400WhenRequestBodyMalformed() throws Exception {
        UserEntity user = createUser("mock-openid-blacklist-004", "David");
        String token = loginAndExtractToken("mock-code-blacklist-004", "David");

        mockMvc.perform(post("/api/v1/users/{userId}/blacklist", user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void addBlacklistShouldReturn400WhenUserIdIsNotNumeric() throws Exception {
        mockMvc.perform(post("/api/v1/users/not-a-number/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poiId\":\"B0FF123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    private UserEntity createUser(String openid, String nickname) {
        UserEntity user = new UserEntity();
        user.setOpenid(openid);
        user.setNickname(nickname);
        return userRepository.save(user);
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
