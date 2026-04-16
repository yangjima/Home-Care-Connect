package com.homecare.user.service;

import com.homecare.user.common.JwtUtil;
import com.homecare.user.common.PasswordEncoder;
import com.homecare.user.dto.LoginRequest;
import com.homecare.user.dto.RegisterRequest;
import com.homecare.user.dto.TokenResponse;
import com.homecare.user.dto.UserResponse;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.service.impl.AuthServiceImpl;
import com.homecare.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 认证服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserServiceImpl userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, userService, jwtUtil, passwordEncoder, redisTemplate);
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 7200000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void login_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            User user = createTestUser(1L, "testuser", "租客");
            user.setPassword("$2a$10$hashedpassword");

            when(userRepository.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
            when(jwtUtil.generateToken(1L, "testuser", "tenant")).thenReturn("access_token_123");
            when(jwtUtil.generateRefreshToken(1L, "testuser")).thenReturn("refresh_token_456");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

            TokenResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("access_token_123", response.getAccessToken());
            assertEquals("refresh_token_456", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(7200, response.getExpiresIn());
        }

        @Test
        @DisplayName("登录失败-用户不存在")
        void login_UserNotFound() {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password123");

            when(userRepository.selectOne(any())).thenReturn(null);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.login(request)
            );

            assertEquals(401, exception.getCode());
            assertEquals("用户名或密码错误", exception.getMessage());
        }

        @Test
        @DisplayName("登录失败-密码错误")
        void login_WrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            User user = createTestUser(1L, "testuser", "租客");
            user.setPassword("$2a$10$hashedpassword");

            when(userRepository.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("wrongpassword", user.getPassword())).thenReturn(false);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.login(request)
            );

            assertEquals(401, exception.getCode());
        }

        @Test
        @DisplayName("登录失败-账号已停用")
        void login_AccountInactive() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            User user = createTestUser(1L, "testuser", "租客");
            user.setPassword("$2a$10$hashedpassword");
            user.setStatus("inactive");

            when(userRepository.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.login(request)
            );

            assertEquals(403, exception.getCode());
            assertEquals("账号已停用", exception.getMessage());
        }

        @Test
        @DisplayName("登录失败-账号已被封禁")
        void login_AccountBanned() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            User user = createTestUser(1L, "testuser", "租客");
            user.setPassword("$2a$10$hashedpassword");
            user.setStatus("banned");

            when(userRepository.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.login(request)
            );

            assertEquals(403, exception.getCode());
            assertEquals("账号已被封禁", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        @Test
        @DisplayName("注册成功")
        void register_Success() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setConfirmPassword("password123");
            request.setRealName("新用户");
            request.setGender("male");
            request.setPhone("13900000000");
            request.setRole("tenant");

            when(userService.isUsernameExists("newuser")).thenReturn(false);
            when(userService.isPhoneExists("13900000000")).thenReturn(false);
            when(userService.isEmailExists(null)).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
            when(userRepository.insert(any(User.class))).thenReturn(1L);

            UserResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("newuser", response.getUsername());
            assertEquals("新用户", response.getRealName());
            verify(userRepository).insert(any(User.class));
        }

        @Test
        @DisplayName("注册失败-密码确认不一致")
        void register_PasswordMismatch() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setConfirmPassword("different_password");
            request.setRealName("新用户");

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.register(request)
            );

            assertEquals(400, exception.getCode());
            assertEquals("两次密码输入不一致", exception.getMessage());
        }

        @Test
        @DisplayName("注册失败-用户名已存在")
        void register_UsernameExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existinguser");
            request.setPassword("password123");
            request.setConfirmPassword("password123");
            request.setRealName("新用户");

            when(userService.isUsernameExists("existinguser")).thenReturn(true);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.register(request)
            );

            assertEquals(400, exception.getCode());
            assertEquals("用户名已存在", exception.getMessage());
        }

        @Test
        @DisplayName("注册失败-手机号已被注册")
        void register_PhoneExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setConfirmPassword("password123");
            request.setRealName("新用户");
            request.setPhone("13800138000");

            when(userService.isUsernameExists("newuser")).thenReturn(false);
            when(userService.isPhoneExists("13800138000")).thenReturn(true);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.register(request)
            );

            assertEquals(400, exception.getCode());
            assertEquals("手机号已被注册", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("令牌测试")
    class TokenTests {

        @Test
        @DisplayName("刷新令牌成功")
        void refreshToken_Success() {
            String refreshToken = "valid_refresh_token";
            User user = createTestUser(1L, "testuser", "租客");

            when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
            when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:user:1")).thenReturn(refreshToken);
            when(userRepository.selectById(1L)).thenReturn(user);
            when(jwtUtil.generateToken(1L, "testuser", "tenant")).thenReturn("new_access_token");
            when(jwtUtil.generateRefreshToken(1L, "testuser")).thenReturn("new_refresh_token");
            doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

            TokenResponse response = authService.refreshToken(refreshToken);

            assertNotNull(response);
            assertEquals("new_access_token", response.getAccessToken());
            assertEquals("new_refresh_token", response.getRefreshToken());
        }

        @Test
        @DisplayName("刷新令牌-无效令牌")
        void refreshToken_InvalidToken() {
            String refreshToken = "invalid_refresh_token";
            when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

            var exception = assertThrows(
                    com.homecare.user.common.BusinessException.class,
                    () -> authService.refreshToken(refreshToken)
            );

            assertEquals(401, exception.getCode());
            assertEquals("刷新令牌无效或已过期", exception.getMessage());
        }

        @Test
        @DisplayName("验证令牌成功")
        void validateToken_Success() {
            String token = "valid_token";
            when(jwtUtil.validateToken(token)).thenReturn(true);

            assertTrue(authService.validateToken(token));
        }

        @Test
        @DisplayName("验证令牌-无效令牌")
        void validateToken_Invalid() {
            String token = "invalid_token";
            when(jwtUtil.validateToken(token)).thenReturn(false);

            assertFalse(authService.validateToken(token));
        }
    }

    // ============ 辅助方法 ============

    private User createTestUser(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setPassword("$2a$10$hashedpassword");
        user.setGender("male");
        user.setPhone("13800138000");
        user.setEmail(username + "@example.com");
        user.setRole("tenant");
        user.setStatus("active");
        user.setDeleted(0);
        return user;
    }
}
