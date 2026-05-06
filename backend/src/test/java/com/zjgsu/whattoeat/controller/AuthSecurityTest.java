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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void loginShouldSanitizeXssInNickname() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-xss-001\",\"nickname\":\"<script>alert('xss')</script>Alice\",\"avatarUrl\":\"https://example.com/avatar.png\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("<script>"), "Response should not contain <script> tags");
        assertFalse(responseBody.contains("alert"), "Response should not contain alert() calls");
    }

    @Test
    void loginShouldSanitizeXssInAvatarUrl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-xss-002\",\"nickname\":\"Bob\",\"avatarUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("javascript:"), "Response should not contain javascript: protocol");
    }

    @Test
    void loginShouldSanitizeXssInBothFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-xss-003\",\"nickname\":\"<img src=x onerror=alert(1)>Charlie\",\"avatarUrl\":\"<script>document.cookie</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("<script>"), "Response should not contain <script> tags");
        assertFalse(responseBody.contains("onerror"), "Response should not contain event handlers");
        assertFalse(responseBody.contains("<img"), "Response should not contain <img> tags");
    }

    @Test
    void loginShouldStoreNicknameAsPlainText() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-safe-001\",\"nickname\":\"Alice<b>bold</b>\",\"avatarUrl\":\"https://example.com/avatar.png\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.nickname").value("Alicebold"));
    }

    @Test
    void loginShouldSanitizeXssInAvatarUrlWithScriptTag() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-xss-004\",\"nickname\":\"Dave\",\"avatarUrl\":\"<script>document.cookie</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("<script>"), "Response should not contain <script> tags");
    }
}
