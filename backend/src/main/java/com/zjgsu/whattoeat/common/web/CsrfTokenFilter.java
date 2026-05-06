package com.zjgsu.whattoeat.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class CsrfTokenFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> PUBLIC_STATE_CHANGING_PATHS = Set.of("/api/v1/auth/wechat-login");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String method = request.getMethod().toUpperCase();

        if (!SAFE_METHODS.contains(method) && !PUBLIC_STATE_CHANGING_PATHS.contains(request.getRequestURI())) {
            String authorization = request.getHeader("Authorization");
            boolean hasBearerToken = authorization != null && authorization.startsWith("Bearer ");

            if (!hasBearerToken) {
                String csrfToken = request.getHeader(CSRF_HEADER);
                if (csrfToken == null || csrfToken.isBlank()) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":1005,\"message\":\"缺少CSRF令牌\",\"data\":null}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
