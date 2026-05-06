package com.zjgsu.whattoeat.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrfTokenFilterTest {

    private final CsrfTokenFilter filter = new CsrfTokenFilter();

    @Test
    void getRequestsShouldPassThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        filter.doFilterInternal(request, response, chain);

        assertTrue(chainCalled[0], "Filter chain should be called for GET requests");
        assertEquals(200, response.getStatus());
    }

    @Test
    void postRequestsWithoutBearerOrCsrfShouldBeBlocked() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/wechat-login");
        request.setContentType("application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("1005"));
    }

    @Test
    void postRequestsWithBearerTokenShouldPassThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/1/reviews");
        request.addHeader("Authorization", "Bearer test-token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        filter.doFilterInternal(request, response, chain);

        assertTrue(chainCalled[0], "Filter chain should be called when Bearer token present");
        assertEquals(200, response.getStatus());
    }

    @Test
    void postRequestsWithCsrfTokenShouldPassThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/wechat-login");
        request.addHeader("X-CSRF-Token", "valid-csrf-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        filter.doFilterInternal(request, response, chain);

        assertTrue(chainCalled[0], "Filter chain should be called when CSRF token present");
        assertEquals(200, response.getStatus());
    }

    @Test
    void putRequestsWithoutBearerShouldBeBlocked() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/users/1/reviews/test-poi");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    void deleteRequestsWithoutBearerShouldBeBlocked() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/users/1/reviews/test-poi");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }
}
