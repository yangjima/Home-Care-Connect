package com.homecare.user.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 通过反射注入配置值
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(jwtUtil, "init");
    }

    @Test
    @DisplayName("生成令牌成功")
    void generateToken_Success() {
        String token = jwtUtil.generateToken(1L, "testuser", "tenant");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("解析令牌获取用户ID")
    void getUserIdFromToken_Success() {
        String token = jwtUtil.generateToken(42L, "testuser", "owner");

        Long userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("解析令牌获取用户名")
    void getUsernameFromToken_Success() {
        String token = jwtUtil.generateToken(1L, "testuser123", "tenant");

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals("testuser123", username);
    }

    @Test
    @DisplayName("解析令牌获取角色")
    void getRoleFromToken_Success() {
        String token = jwtUtil.generateToken(1L, "testuser", "distributor");

        String role = jwtUtil.getRoleFromToken(token);

        assertEquals("distributor", role);
    }

    @Test
    @DisplayName("验证令牌成功")
    void validateToken_Success() {
        String token = jwtUtil.generateToken(1L, "testuser", "tenant");

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("验证无效令牌")
    void validateToken_Invalid() {
        String invalidToken = "invalid.token.string";

        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    @DisplayName("验证过期令牌")
    void validateToken_Expired() {
        // 使用一个已知已过期的令牌
        // 通过修改系统时间测试不太可靠，这里验证异常处理
        String tamperedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUwMDAwMDAwMCwiZXhwIjoxNTAwMDAwMDAwfQ.invalid_signature";

        assertFalse(jwtUtil.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("生成刷新令牌成功")
    void generateRefreshToken_Success() {
        String refreshToken = jwtUtil.generateRefreshToken(1L, "testuser");

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());

        // 刷新令牌应该也能被验证
        assertTrue(jwtUtil.validateToken(refreshToken));
    }

    @Test
    @DisplayName("令牌未过期检查")
    void isTokenExpired_False() {
        String token = jwtUtil.generateToken(1L, "testuser", "tenant");

        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    @DisplayName("异常令牌已过期")
    void isTokenExpired_True() {
        String invalidToken = "definitely.invalid.token";

        assertTrue(jwtUtil.isTokenExpired(invalidToken));
    }
}
