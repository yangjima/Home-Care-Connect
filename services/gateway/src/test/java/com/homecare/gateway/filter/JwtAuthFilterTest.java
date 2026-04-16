package com.homecare.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JWT 认证过滤器单元测试
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String JWT_SECRET = "HomeCareConnectSecretKeyForJWT2024VeryLongStringThatIsAtLeast256Bits";

    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {
        jwtAuthFilter = new JwtAuthFilter();
        var field = JwtAuthFilter.class.getDeclaredField("jwtSecret");
        field.setAccessible(true);
        field.set(jwtAuthFilter, JWT_SECRET);
    }

    @Test
    @DisplayName("白名单路径放行")
    void whiteListPath_PassThrough() {
        ServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        jwtAuthFilter.filter(exchange, filterChain);

        verify(filterChain).filter(exchange);
    }

    @Test
    @DisplayName("无 Token 返回 401")
    void noToken_ReturnsUnauthorized() {
        ServerHttpRequest request = MockServerHttpRequest.get("/api/users/me").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = jwtAuthFilter.filter(exchange, filterChain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("无效 Token 返回 401")
    void invalidToken_ReturnsUnauthorized() {
        ServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header("Authorization", "Bearer invalid.token.here")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = jwtAuthFilter.filter(exchange, filterChain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("有效 Token 注入用户信息")
    void validToken_InjectsUserInfo() {
        String token = generateValidToken("1", "admin", "admin");
        ServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> result = jwtAuthFilter.filter(exchange, filterChain);
        result.block();

        assertTrue(exchange.getRequest().getHeaders().containsKey("X-User-Id"));
        assertEquals("1", exchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("admin", exchange.getRequest().getHeaders().getFirst("X-Username"));
        assertEquals("admin", exchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    private String generateValidToken(String userId, String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
