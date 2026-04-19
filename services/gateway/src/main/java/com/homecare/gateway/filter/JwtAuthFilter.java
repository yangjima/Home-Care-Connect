package com.homecare.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * JWT 全局认证过滤器
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> WHITE_LIST = List.of(
            // 用户认证
            "/api/user/auth/login",
            "/api/user/auth/register",
            "/api/user/auth/register-email",
            "/api/user/auth/send-code",
            "/api/user/auth/verify-code",
            "/api/user/auth/refresh",

            // 网关健康检查
            "/actuator/health",

            // AI（如需开放可放行；否则可移除）
            "/api/ai/chat",
            "/api/ai/ws",

            "/captcha"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        Optional<ServerHttpRequest> withUserHeaders = tryInjectUserFromToken(request);

        // 白名单路径：允许匿名；若携带合法 JWT 则仍注入用户头（便于公开列表按角色筛选待审等）
        if (isWhiteList(path, method)) {
            if (withUserHeaders.isPresent()) {
                return chain.filter(exchange.mutate().request(withUserHeaders.get()).build());
            }
            return chain.filter(exchange);
        }

        if (withUserHeaders.isEmpty()) {
            return unauthorized(exchange.getResponse());
        }

        return chain.filter(exchange.mutate().request(withUserHeaders.get()).build());
    }

    /**
     * 若存在合法 Bearer Token，返回已注入 X-User-* 头的请求；否则 empty。
     * Token 无效时不抛错（由调用方决定：公开接口忽略，受保护接口返回 401）。
     */
    private Optional<ServerHttpRequest> tryInjectUserFromToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = validateToken(token);
            String userId = null;
            Object userIdClaim = claims.get("userId");
            if (userIdClaim != null) {
                userId = String.valueOf(userIdClaim);
            }
            if (userId == null || userId.isBlank()) {
                userId = claims.getSubject();
            }
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            Object storeIdClaim = claims.get("storeId");

            var builder = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username != null ? username : "")
                    .header("X-User-Role", role != null ? role : "");
            if (storeIdClaim != null) {
                String storeId = String.valueOf(storeIdClaim);
                if (storeId != null && !storeId.isBlank() && !"null".equals(storeId)) {
                    builder.header("X-User-Store-Id", storeId);
                }
            }
            ServerHttpRequest mutatedRequest = builder.build();
            return Optional.of(mutatedRequest);
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isWhiteList(String path, HttpMethod method) {
        if (path == null) {
            return false;
        }

        // 精确前缀匹配（避免 "/properties" 误放行 POST 创建等写操作）
        boolean prefixWhitelisted = WHITE_LIST.stream().anyMatch(path::startsWith);
        if (prefixWhitelisted) {
            return true;
        }

        // 公开房源：仅允许 GET 查询/详情
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/property/properties")) {
            return true;
        }

        // 公开服务：服务类型列表/详情、服务人员列表（仅 GET）；管理端子路径需登录
        if (HttpMethod.GET.equals(method)) {
            if (path.startsWith("/api/service/service-types")) {
                if (path.contains("/service-types/admin/")) {
                    return false;
                }
                return true;
            }
            if (path.startsWith("/api/service/staff")) {
                return true;
            }
        }

        // 公开采购商品：仅允许 GET 列表与数字 id 详情
        if (HttpMethod.GET.equals(method) && isPublicProcurementAssetGet(path)) {
            return true;
        }

        // 公开跳蚤市场：仅允许 GET 列表/详情/统计（不含「我的」）
        if (HttpMethod.GET.equals(method) && isPublicSecondhandAssetGet(path)) {
            return true;
        }

        return false;
    }

    private boolean isPublicProcurementAssetGet(String path) {
        if (!path.startsWith("/api/asset/procurement-products")) {
            return false;
        }
        if (path.equals("/api/asset/procurement-products")) {
            return true;
        }
        if ("/api/asset/procurement-products/summary".equals(path)) {
            return true;
        }
        String rest = path.substring("/api/asset/procurement-products/".length());
        return rest.matches("\\d+");
    }

    private boolean isPublicSecondhandAssetGet(String path) {
        if (path == null) {
            return false;
        }
        String p = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        if (!p.startsWith("/api/asset/secondhand-items")) {
            return false;
        }
        if (p.contains("/secondhand-items/my")) {
            return false;
        }
        if ("/api/asset/secondhand-items/summary".equals(p)
                || p.equals("/api/asset/secondhand-items")
                || p.endsWith("/secondhand-items/summary")) {
            return true;
        }
        String rest = p.substring("/api/asset/secondhand-items/".length());
        return rest.matches("\\d+");
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
