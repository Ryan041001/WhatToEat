package com.zjgsu.whattoeat.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 为餐厅列表/搜索接口添加客户端缓存头，
 * 覆盖全局 SecurityHeadersFilter 的 no-store。
 * <p>
 * Cache-Control: max-age=300 允许客户端（含代理）缓存 5 分钟，
 * 减少重复请求时的数据传输与后端压力。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RestaurantCacheControlFilter extends OncePerRequestFilter {

    private static final Set<String> CACHEABLE_PATHS = Set.of(
            "/api/v1/restaurants/nearby",
            "/api/v1/restaurants/search"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (CACHEABLE_PATHS.contains(path)) {
            response.setHeader("Cache-Control", "max-age=300, must-revalidate");
        }
        filterChain.doFilter(request, response);
    }
}
