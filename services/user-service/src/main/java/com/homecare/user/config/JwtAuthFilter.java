package com.homecare.user.config;

import com.homecare.user.common.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器（用于 Spring MVC 同步场景）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String USER_ID_ATTR = "userId";
    private static final String USERNAME_ATTR = "username";
    private static final String ROLE_ATTR = "role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 放行公开路径
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            sendUnauthorized(response, "未提供认证令牌");
            return;
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());

        try {
            if (!jwtUtil.validateToken(token)) {
                sendUnauthorized(response, "令牌无效或已过期");
                return;
            }

            // 解析令牌
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // 将用户信息注入到请求属性中
            request.setAttribute(USER_ID_ATTR, userId);
            request.setAttribute(USERNAME_ATTR, username);
            request.setAttribute(ROLE_ATTR, role);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT 认证失败: {}", e.getMessage());
            sendUnauthorized(response, "令牌验证失败");
        }
    }

    /**
     * 判断路径是否公开
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/auth/send-code")
                || path.startsWith("/auth/verify-code")
                || path.startsWith("/auth/check-username")
                || path.startsWith("/captcha")
                || path.startsWith("/health")
                || path.startsWith("/actuator")
                || path.equals("/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}
