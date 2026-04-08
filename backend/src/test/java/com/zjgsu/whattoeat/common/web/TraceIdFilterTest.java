package com.zjgsu.whattoeat.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceIdFilterTest {

    private TraceIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        request = new MockHttpServletRequest("GET", "/api/v1/restaurants/nearby");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPutTraceIdIntoMdcAndResponseHeader() throws Exception {
        filter.doFilter(request, response, (req, res) -> {
            assertNotNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
            assertEquals(MDC.get(TraceIdFilter.TRACE_ID_KEY), ((MockHttpServletResponse) res).getHeader(TraceIdFilter.TRACE_HEADER));
        });

        assertNotNull(response.getHeader(TraceIdFilter.TRACE_HEADER));
    }

    @Test
    void shouldReuseIncomingTraceId() throws Exception {
        request.addHeader(TraceIdFilter.TRACE_HEADER, "trace-fixed-001");

        filter.doFilter(request, response, (req, res) -> {
            assertEquals("trace-fixed-001", MDC.get(TraceIdFilter.TRACE_ID_KEY));
        });

        assertEquals("trace-fixed-001", response.getHeader(TraceIdFilter.TRACE_HEADER));
    }

    @Test
    void shouldGenerateNewTraceIdWhenIncomingTraceIdIsInvalid() throws Exception {
        request.addHeader(TraceIdFilter.TRACE_HEADER, "bad trace id\n");

        filter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
            assertNotNull(traceId);
            assertNotEquals("bad trace id\n", traceId);
            assertTrue(traceId.matches("^[A-Za-z0-9_-]{1,64}$"));
        });

        assertNotEquals("bad trace id\n", response.getHeader(TraceIdFilter.TRACE_HEADER));
    }

    @Test
    void shouldSkipActuatorRequests() throws Exception {
        MockHttpServletRequest actuatorRequest = new MockHttpServletRequest("GET", "/actuator/prometheus");

        assertTrue(filter.shouldNotFilter(actuatorRequest));
        filter.doFilter(actuatorRequest, response, (req, res) -> {
            assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
            assertNull(((MockHttpServletResponse) res).getHeader(TraceIdFilter.TRACE_HEADER));
        });

        assertNull(response.getHeader(TraceIdFilter.TRACE_HEADER));
        assertFalse(response.containsHeader(TraceIdFilter.TRACE_HEADER));
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }

    @Test
    void shouldClearMdcAfterFilterChain() throws Exception {
        filter.doFilter(request, response, (req, res) -> assertNotNull(MDC.get(TraceIdFilter.TRACE_ID_KEY)));

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }
}
