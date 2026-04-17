package com.homecare.user.controller;

import com.homecare.user.common.Result;
import com.homecare.user.dto.*;
import com.homecare.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return Result.success("注册成功", response);
    }

    /**
     * 邮箱注册（验证码注册）
     */
    @PostMapping("/register-email")
    public Result<UserResponse> registerByEmail(@Valid @RequestBody EmailRegisterRequest request) {
        UserResponse response = authService.registerByEmail(request);
        return Result.success("注册成功", response);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Result<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return Result.success("刷新成功", response);
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    public Result<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        boolean valid = authService.validateToken(token);
        return Result.success(valid);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        UserResponse response = authService.getCurrentUser(token);
        return Result.success(response);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // 可以在这里添加黑名单逻辑
        return Result.success("退出成功", null);
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam("username") String username) {
        // 这里需要调用 UserService，但 AuthController 不直接依赖 UserServiceImpl
        // 简化处理，后续可抽取到单独的校验 API
        return Result.success(false);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request.getType(), request.getTarget());
        return Result.success("验证码已发送", null);
    }

    /**
     * 验证验证码（预留接口）
     */
    @PostMapping("/verify-code")
    public Result<Boolean> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        boolean ok = authService.verifyCode(request.getType(), request.getTarget(), request.getCode());
        return Result.success(ok);
    }

    /**
     * 从 Authorization header 中提取 Token
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }

    // --- 内部请求类 ---

    @lombok.Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    @lombok.Data
    public static class SendCodeRequest {
        @jakarta.validation.constraints.NotBlank(message = "验证码类型不能为空")
        private String type; // sms / email
        @jakarta.validation.constraints.NotBlank(message = "接收方不能为空")
        private String target; // 手机号或邮箱
    }

    @lombok.Data
    public static class VerifyCodeRequest {
        @jakarta.validation.constraints.NotBlank(message = "验证码类型不能为空")
        private String type;
        @jakarta.validation.constraints.NotBlank(message = "接收方不能为空")
        private String target;
        @jakarta.validation.constraints.NotBlank(message = "验证码不能为空")
        private String code;
    }
}
