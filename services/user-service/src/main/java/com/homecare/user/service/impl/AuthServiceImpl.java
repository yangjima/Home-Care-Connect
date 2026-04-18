package com.homecare.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homecare.user.common.BusinessException;
import com.homecare.user.common.PageResult;
import com.homecare.user.common.PasswordEncoder;
import com.homecare.user.dto.*;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.security.RsaPasswordDecryptor;
import com.homecare.user.service.AuthService;
import com.homecare.user.service.UserService;
import com.homecare.user.util.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

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
    private final RsaPasswordDecryptor rsaPasswordDecryptor;

    @Value("${jwt.access-token-expiration:86400000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL = 300; // 5分钟

    private static final String VERIFY_CODE_PREFIX = "verify:code:";
    private static final String VERIFY_CODE_COOLDOWN_PREFIX = "verify:cooldown:";
    private static final long VERIFY_CODE_TTL_SECONDS = 300; // 5分钟
    private static final long VERIFY_CODE_COOLDOWN_SECONDS = 60; // 60秒内只能发送一次
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\S+@\\S+\\.\\S+$");

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
        );

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 验证密码
        String rawPassword = rsaPasswordDecryptor.decryptRequired(request.getPassword());
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
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

        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiration / 1000, toUserResponse(user));
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        String rawPassword = rsaPasswordDecryptor.decryptRequired(request.getPassword());
        String rawConfirmPassword = rsaPasswordDecryptor.decryptRequired(request.getConfirmPassword());
        // 密码确认
        if (rawPassword == null || !rawPassword.equals(rawConfirmPassword)) {
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
        user.setPassword(passwordEncoder.encode(rawPassword));

        // 默认角色：注册仅允许普通用户，禁止自选管理类角色
        if (StringUtils.hasText(user.getRole())) {
            String rr = user.getRole().trim();
            if (!Roles.TENANT.equals(rr) && !Roles.USER.equals(rr)) {
                throw new BusinessException(400, "注册仅允许普通用户角色");
            }
        } else {
            user.setRole(Roles.TENANT);
        }
        user.setStatus("active");

        // 保存用户
        userRepository.insert(user);

        log.info("用户注册成功: {}", user.getUsername());

        return toUserResponse(user);
    }

    @Override
    public void sendCode(String type, String target) {
        String normalizedType = normalizeCodeType(type);
        String normalizedTarget = normalizeTarget(normalizedType, target);

        String cooldownKey = VERIFY_CODE_COOLDOWN_PREFIX + normalizedType + ":" + normalizedTarget;
        Boolean inCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(inCooldown)) {
            throw new BusinessException(429, "操作过于频繁，请稍后再试");
        }

        String code = generateNumericCode(6);
        String codeKey = VERIFY_CODE_PREFIX + normalizedType + ":" + normalizedTarget;

        redisTemplate.opsForValue().set(codeKey, code, VERIFY_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", VERIFY_CODE_COOLDOWN_SECONDS, TimeUnit.SECONDS);

        // MVP：先把验证码打印日志，后续对接短信/邮件服务
        log.info("发送验证码成功 type={} target={} code={} ttl={}s", normalizedType, normalizedTarget, code, VERIFY_CODE_TTL_SECONDS);
    }

    @Override
    public boolean verifyCode(String type, String target, String code) {
        String normalizedType = normalizeCodeType(type);
        String normalizedTarget = normalizeTarget(normalizedType, target);

        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "验证码不能为空");
        }

        String codeKey = VERIFY_CODE_PREFIX + normalizedType + ":" + normalizedTarget;
        String cached = redisTemplate.opsForValue().get(codeKey);
        if (!StringUtils.hasText(cached)) {
            return false;
        }

        boolean ok = cached.equals(code.trim());
        if (ok) {
            redisTemplate.delete(codeKey);
        }
        return ok;
    }

    @Override
    public UserResponse registerByEmail(EmailRegisterRequest request) {
        String rawPassword = rsaPasswordDecryptor.decryptRequired(request.getPassword());
        String rawConfirmPassword = rsaPasswordDecryptor.decryptRequired(request.getConfirmPassword());
        if (!StringUtils.hasText(rawPassword) || !rawPassword.equals(rawConfirmPassword)) {
            throw new BusinessException(400, "两次密码输入不一致");
        }

        String email = request.getEmail() == null ? null : request.getEmail().trim();
        boolean ok = verifyCode("email", email, request.getCode());
        if (!ok) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        if (StringUtils.hasText(email) && userService.isEmailExists(email)) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 邮箱即登录账号
        String username = email;
        if (userService.isUsernameExists(username)) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        } else if (StringUtils.hasText(email) && email.contains("@")) {
            user.setRealName(email.substring(0, email.indexOf('@')));
        } else {
            user.setRealName("用户");
        }

        user.setRole("tenant");
        user.setStatus("active");
        userRepository.insert(user);

        log.info("邮箱注册成功: {}", user.getEmail());
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

    private String normalizeCodeType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new BusinessException(400, "验证码类型不能为空");
        }
        String t = type.trim().toLowerCase();
        if (!("sms".equals(t) || "email".equals(t))) {
            throw new BusinessException(400, "验证码类型不支持");
        }
        return t;
    }

    private String normalizeTarget(String type, String target) {
        if (!StringUtils.hasText(target)) {
            throw new BusinessException(400, "接收方不能为空");
        }
        String v = target.trim();
        if ("sms".equals(type) && !PHONE_PATTERN.matcher(v).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }
        if ("email".equals(type) && !EMAIL_PATTERN.matcher(v).matches()) {
            throw new BusinessException(400, "邮箱格式不正确");
        }
        return v;
    }

    private String generateNumericCode(int length) {
        int bound = (int) Math.pow(10, length);
        int code = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + length + "d", code);
    }
}
