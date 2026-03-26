package com.zjgsu.whattoeat.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldMapBusinessExceptionTo409() throws Exception {
        mockMvc.perform(get("/test/business/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    void shouldMapBusinessExceptionTo401() throws Exception {
        mockMvc.perform(get("/test/business/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void shouldMapValidationExceptionTo400WithCode1001() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidationRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void shouldMapBusinessExceptionTo504WhenTimeout() throws Exception {
        mockMvc.perform(get("/test/business/timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value(3002));
    }

    @Test
    void shouldMapUnexpectedExceptionTo500WithCode9000() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(9000));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business/conflict")
        void conflict() {
            throw new BusinessException(ErrorCode.BLACKLIST_ALREADY_EXISTS);
        }

        @GetMapping("/test/business/unauthorized")
        void unauthorized() {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        @GetMapping("/test/business/timeout")
        void timeout() {
            throw new BusinessException(ErrorCode.AMAP_UPSTREAM_TIMEOUT);
        }

        @PostMapping("/test/validation")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new RuntimeException("boom");
        }
    }

    record ValidationRequest(@NotBlank(message = "name不能为空") String name) {
    }
}
