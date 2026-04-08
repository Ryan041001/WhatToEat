package com.zjgsu.whattoeat.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    static final String TRACE_HEADER = "X-Trace-Id";
    static final String TRACE_ID_KEY = "traceId";

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "/actuator".equals(requestUri) || requestUri.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(TRACE_HEADER))
                .filter(this::isValidTraceId)
                .orElseGet(() -> UUID.randomUUID().toString());
        long startAt = System.currentTimeMillis();

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - startAt;
            log.info("request path={} method={} status={} latencyMs={} traceId={}",
                    request.getRequestURI(),
                    request.getMethod(),
                    response.getStatus(),
                    latencyMs,
                    traceId);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private boolean isValidTraceId(String value) {
        return value != null && TRACE_ID_PATTERN.matcher(value).matches();
    }
}
