package com.zjgsu.whattoeat.common.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void responseShouldContainSecurityHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {});

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
    }
}
