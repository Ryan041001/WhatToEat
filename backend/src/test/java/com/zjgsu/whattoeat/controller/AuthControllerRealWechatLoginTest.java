package com.zjgsu.whattoeat.controller;

import com.zjgsu.whattoeat.integration.wechat.WechatAuthGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "wechat.auth.app-id=test-app-id",
        "wechat.auth.app-secret=test-app-secret",
        "wechat.auth.mock-login-enabled=false"
})
@ActiveProfiles("test")
class AuthControllerRealWechatLoginTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void wechatLoginShouldSupportRealCodeWhenMockLoginDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-real-code-001\",\"nickname\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.openid").value("real-openid-wx-real-code-001"))
                .andExpect(jsonPath("$.data.user.nickname").value("Alice"));
    }

    @Test
    void wechatLoginShouldRejectMockCodeWhenMockLoginDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-code-001\",\"nickname\":\"Alice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @TestConfiguration
    static class StubWechatAuthConfiguration {
        @Bean
        @Primary
        WechatAuthGateway wechatAuthGateway() {
            return code -> new WechatAuthGateway.WechatSession(
                    "real-openid-" + code,
                    "session-key-" + code,
                    null);
        }
    }
}
