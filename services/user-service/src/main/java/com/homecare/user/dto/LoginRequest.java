package com.homecare.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 图形验证码（可选）
     */
    private String captcha;

    /**
     * 验证码ID（可选）
     */
    private String captchaId;
}
