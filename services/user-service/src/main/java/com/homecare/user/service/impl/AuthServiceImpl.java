package com.homecare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.user.common.BusinessException;
import com.homecare.user.common.PageResult;
import com.homecare.user.common.PasswordEncoder;
import com.homecare.user.dto.*;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.service.AuthService;
import com.homecare.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final com.homecare.user.common.JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.access-token-expiration:7200000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL = 300; // 5分钟

    @Override
    public TokenResponse login(LoginRequest request) {
        // 图形验证码校验
        if (StringUtils.hasText(request.getCaptchaId()) && StringUtils.hasText(request.getCaptcha())) {
            String cachedCaptcha = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + request.getCaptchaId());
            if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(request.getCaptcha())) {
                throw new BusinessException(400, "图形验证码错误或已过期");
            }
            redisTemplate.delete(CAPTCHA_PREFIX + request.getCaptchaId());
        }

        // 查询用户
        User user = userRepository.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .eq(User::getDeleted, 0)
        );

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 检查用户状态
        if ("inactive".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已停用");
        }
        if ("banned".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被封禁");
        }

        // 生成令牌
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 将 refreshToken 存入 Redis
        redisTemplate.opsForValue().set(
                "refresh:user:" + user.getId(),
                refreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        log.info("用户登录成功: {}", user.getUsername());

        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiration / 1000);
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        // 密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次密码输入不一致");
        }

        // 图形验证码校验
        if (StringUtils.hasText(request.getCaptchaId()) && StringUtils.hasText(request.getCaptcha())) {
            String cachedCaptcha = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + request.getCaptchaId());
            if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(request.getCaptcha())) {
                throw new BusinessException(400, "图形验证码错误或已过期");
            }
            redisTemplate.delete(CAPTCHA_PREFIX + request.getCaptchaId());
        }

        // 检查用户名是否已存在
        if (userService.isUsernameExists(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 检查手机号是否已存在
        if (StringUtils.hasText(request.getPhone()) && userService.isPhoneExists(request.getPhone())) {
            throw new BusinessException(400, "手机号已被注册");
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(request.getEmail()) && userService.isEmailExists(request.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 构建用户实体
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 默认角色
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("tenant");
        }
        user.setStatus("active");

        // 保存用户
        userRepository.insert(user);

        log.info("用户注册成功: {}", user.getUsername());

        return toUserResponse(user);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 验证 Redis 中的 refreshToken
        String cachedToken = redisTemplate.opsForValue().get("refresh:user:" + userId);
        if (!refreshToken.equals(cachedToken)) {
            throw new BusinessException(401, "刷新令牌已被使用或失效");
        }

        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 生成新令牌
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 更新 Redis 中的 refreshToken
        redisTemplate.opsForValue().set(
                "refresh:user:" + user.getId(),
                newRefreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        return TokenResponse.of(newAccessToken, newRefreshToken, accessTokenExpiration / 1000);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    public UserResponse getCurrentUser(String token) {
        String username = jwtUtil.getUsernameFromToken(token);
        return userService.getUserByUsername(username);
    }

    /**
     * 将 User 实体转换为 UserResponse
     */
    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        // UserResponse 不包含 password 字段，无需额外处理
        return response;
    }
}
