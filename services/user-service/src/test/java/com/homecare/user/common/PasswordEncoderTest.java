package com.homecare.user.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordEncoder 单元测试
 */
class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    @DisplayName("密码加密成功")
    void encode_Success() {
        String rawPassword = "password123";

        String encoded = encoder.encode(rawPassword);

        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        // BCrypt 加密后的格式: $2a$XX$...
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
    }

    @Test
    @DisplayName("加密后密码不一致（每次盐值不同）")
    void encode_DifferentEachTime() {
        String rawPassword = "password123";

        String encoded1 = encoder.encode(rawPassword);
        String encoded2 = encoder.encode(rawPassword);

        // 每次加密结果不同（因为盐值不同）
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    @DisplayName("密码验证成功")
    void matches_Success() {
        String rawPassword = "password123";
        String encoded = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("密码验证失败-错误密码")
    void matches_WrongPassword() {
        String rawPassword = "password123";
        String encoded = encoder.encode(rawPassword);

        assertFalse(encoder.matches("wrongpassword", encoded));
    }

    @Test
    @DisplayName("密码验证失败-空密码")
    void matches_EmptyPassword() {
        String encoded = encoder.encode("password123");

        assertFalse(encoder.matches("", encoded));
    }

    @Test
    @DisplayName("同一密码多次验证成功")
    void matches_MultipleTimes() {
        String rawPassword = "testPassword456";
        String encoded = encoder.encode(rawPassword);

        // 同一个加密后的密码可以被多次正确验证
        assertTrue(encoder.matches(rawPassword, encoded));
        assertTrue(encoder.matches(rawPassword, encoded));
        assertTrue(encoder.matches(rawPassword, encoded));
    }
}
