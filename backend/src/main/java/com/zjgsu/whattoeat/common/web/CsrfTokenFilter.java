package com.zjgsu.whattoeat.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjgsu.whattoeat.common.api.ApiResponse;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Component
public class CsrfTokenFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> PUBLIC_STATE_CHANGING_PATHS = Set.of("/api/v1/auth/wechat-login");

    private final ObjectMapper objectMapper;

    public CsrfTokenFilter() {
        this(new ObjectMapper());
    }

    public CsrfTokenFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        if (!SAFE_METHODS.contains(method) && !PUBLIC_STATE_CHANGING_PATHS.contains(request.getRequestURI())) {
            String authorization = request.getHeader("Authorization");
            boolean hasBearerToken = authorization != null && authorization.startsWith("Bearer ");

            if (!hasBearerToken) {
                String csrfToken = request.getHeader(CSRF_HEADER);
                if (csrfToken == null || csrfToken.isBlank()) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setCharacterEncoding("UTF-8");
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ErrorCode errorCode = ErrorCode.CSRF_TOKEN_MISSING;
                    objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                            errorCode.getCode(),
                            errorCode.getMessage()));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
