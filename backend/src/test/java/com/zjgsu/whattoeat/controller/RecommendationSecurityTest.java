package com.zjgsu.whattoeat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(RecommendationControllerTest.TestConfig.class)
class RecommendationSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void askShouldRejectOverlongQuestionBeforeCallingRecommendationFlow() throws Exception {
        String overlongQuestion = "想".repeat(501);

        mockMvc.perform(post("/api/v1/recommendations/ask")
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 120.35,
                                  "latitude": 30.31,
                                  "question": "%s"
                                }
                                """.formatted(overlongQuestion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
