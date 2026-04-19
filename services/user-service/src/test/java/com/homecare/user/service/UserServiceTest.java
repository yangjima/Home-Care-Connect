package com.homecare.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homecare.user.common.BusinessException;
import com.homecare.user.common.PasswordEncoder;
import com.homecare.user.dto.*;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.security.RsaPasswordDecryptor;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RsaPasswordDecryptor rsaPasswordDecryptor;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, rsaPasswordDecryptor);
        ReflectionTestUtils.setField(userService, "onlineWithinMinutes", 15);
    }

    @Nested
    @DisplayName("用户查询测试")
    class UserQueryTests {

        @Test
        @DisplayName("根据ID获取用户成功")
        void getUserById_Success() {
            User user = createTestUser(1L, "testuser", "租客");
            when(userRepository.selectById(1L)).thenReturn(user);

            UserResponse response = userService.getUserById(1L);

            assertNotNull(response);
            assertEquals("testuser", response.getUsername());
            assertEquals("租客", response.getRealName());
        }

        @Test
        @DisplayName("根据ID获取用户-用户不存在")
        void getUserById_NotFound() {
            when(userRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.getUserById(999L));

            assertEquals(404, exception.getCode());
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("根据用户名获取用户成功")
        void getUserByUsername_Success() {
            User user = createTestUser(1L, "testuser", "租客");
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            UserResponse response = userService.getUserByUsername("testuser");

            assertNotNull(response);
            assertEquals("testuser", response.getUsername());
        }

        @Test
        @DisplayName("根据用户名获取用户-用户不存在")
        void getUserByUsername_NotFound() {
            when(userRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.getUserByUsername("nonexistent"));

            assertEquals(404, exception.getCode());
        }
    }

    @Nested
    @DisplayName("用户检查测试")
    class UserCheckTests {

        @Test
        @DisplayName("用户名已存在")
        void isUsernameExists_True() {
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertTrue(userService.isUsernameExists("existinguser"));
        }

        @Test
        @DisplayName("用户名不存在")
        void isUsernameExists_False() {
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            assertFalse(userService.isUsernameExists("newuser"));
        }

        @Test
        @DisplayName("手机号已存在")
        void isPhoneExists_True() {
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertTrue(userService.isPhoneExists("13800138000"));
        }

        @Test
        @DisplayName("手机号为空-返回false")
        void isPhoneExists_NullPhone() {
            assertFalse(userService.isPhoneExists(null));
            assertFalse(userService.isPhoneExists(""));
        }

        @Test
        @DisplayName("邮箱已存在")
        void isEmailExists_True() {
            when(userRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertTrue(userService.isEmailExists("test@example.com"));
        }

        @Test
        @DisplayName("邮箱为空-返回false")
        void isEmailExists_NullEmail() {
            assertFalse(userService.isEmailExists(null));
            assertFalse(userService.isEmailExists(""));
        }
    }

    @Nested
    @DisplayName("用户状态更新测试")
    class UserStatusTests {

        @Test
        @DisplayName("更新用户状态成功")
        void updateUserStatus_Success() {
            User user = createTestUser(1L, "testuser", "租客");
            when(userRepository.selectById(1L)).thenReturn(user);
            doNothing().when(userRepository).updateById(any(User.class));

            assertDoesNotThrow(() -> userService.updateUserStatus(1L, "inactive"));

            verify(userRepository).updateById(argThat(u -> "inactive".equals(u.getStatus())));
        }

        @Test
        @DisplayName("更新用户状态-用户不存在")
        void updateUserStatus_UserNotFound() {
            when(userRepository.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(999L, "active"));

            assertEquals(404, exception.getCode());
        }

        @Test
        @DisplayName("更新用户状态-无效状态值")
        void updateUserStatus_InvalidStatus() {
            User user = createTestUser(1L, "testuser", "租客");
            when(userRepository.selectById(1L)).thenReturn(user);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(1L, "invalid_status"));

            assertEquals(400, exception.getCode());
            assertEquals("无效的状态值", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("密码更新测试")
    class PasswordUpdateTests {

        @Test
        @DisplayName("修改密码成功")
        void updatePassword_Success() {
            User user = createTestUser(1L, "testuser", "租客");
            user.setPassword("$2a$10$hashedpassword");
            when(userRepository.selectById(1L)).thenReturn(user);
            when(passwordEncoder.matches("oldpass123", user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newpass123")).thenReturn("$2a$10$newhashedpassword");
            doNothing().when(userRepository).updateById(any(User.class));

            assertDoesNotThrow(() -> userService.updatePassword(1L, "oldpass123", "newpass123"));

            verify(userRepository).updateById(argThat(u -> u.getPassword().equals("$2a$10$newhashedpassword")));
        }

        @Test
        @DisplayName("修改密码-原密码错误")
        void updatePassword_WrongOldPassword() {
            User user = createTestUser(1L, "testuser", "租客");
            user.setPassword("$2a$10$hashedpassword");
            when(userRepository.selectById(1L)).thenReturn(user);
            when(passwordEncoder.matches("wrongpassword", user.getPassword())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(1L, "wrongpassword", "newpass123"));

            assertEquals(400, exception.getCode());
            assertEquals("原密码错误", exception.getMessage());
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
        return user;
    }
}
