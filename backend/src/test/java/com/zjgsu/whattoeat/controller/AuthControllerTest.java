package com.zjgsu.whattoeat.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void wechatLoginShouldReturn201WithTokenAndUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-001\",\"nickname\":\"Alice\",\"avatarUrl\":\"https://example.com/avatar-alice.png\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.openid").isString())
                .andExpect(jsonPath("$.data.user.nickname").value("Alice"))
                .andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/avatar-alice.png"));
    }

    @Test
    void wechatLoginShouldAcceptWxLoginCodeWhenMockLoginEnabledWithoutWechatCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-devtools-code-001\",\"nickname\":\"Alice\",\"avatarUrl\":\"https://example.com/avatar-alice.png\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.openid").value("mock-openid-wx-devtools-code-001"))
                .andExpect(jsonPath("$.data.user.nickname").value("Alice"))
                .andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/avatar-alice.png"));
    }

    @Test
    void wechatLoginShouldReturn400WhenCodeBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"nickname\":\"Alice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void meShouldReturn401WhenAuthorizationMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void meShouldReturnCurrentUserAfterLogin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-002\",\"nickname\":\"Bob\",\"avatarUrl\":\"https://example.com/avatar-bob.png\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String token = extractToken(loginResult.getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("Bob"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar-bob.png"));
    }

    @Test
    void meShouldReturn401AfterLogout() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-003\",\"nickname\":\"Cindy\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String token = extractToken(loginResult.getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void logoutShouldReturn401WhenTokenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    private String extractToken(String responseBody) {
        Matcher matcher = TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("token not found in response: " + responseBody);
        }
        return matcher.group(1);
    }
}
