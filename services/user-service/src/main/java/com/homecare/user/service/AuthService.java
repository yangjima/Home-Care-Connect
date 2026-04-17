package com.homecare.user.service;

import com.homecare.user.dto.*;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    TokenResponse login(LoginRequest request);

    /**
     * 用户注册
     */
    UserResponse register(RegisterRequest request);

    /**
     * 刷新令牌
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * 验证令牌
     */
    boolean validateToken(String token);

    /**
     * 获取当前用户信息
     */
    UserResponse getCurrentUser(String token);

    /**
     * 发送短信/邮箱验证码
     */
    void sendCode(String type, String target);

    /**
     * 校验短信/邮箱验证码（校验成功会消费该验证码）
     */
    boolean verifyCode(String type, String target, String code);

    /**
     * 邮箱注册（验证码注册）
     */
    UserResponse registerByEmail(EmailRegisterRequest request);
}
