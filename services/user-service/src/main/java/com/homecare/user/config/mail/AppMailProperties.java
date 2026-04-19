package com.homecare.user.config.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮箱验证码 SMTP 配置（与 application.yml 中 app.mail 对应）
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    /**
     * 是否启用真实发信；关闭时邮箱验证码接口会返回业务错误提示
     */
    private boolean enabled = false;

    /**
     * SMTP 主机
     */
    private String host = "";

    /**
     * SMTP 端口（明文 SMTP 常用 25）
     */
    private int port = 25;

    /**
     * SMTP 登录账号（一般为完整邮箱）
     */
    private String username = "";

    /**
     * SMTP 认证口令：配置键名沿用协议习惯为 password。
     * 139/QQ/网易等通常应填邮箱后台的「客户端授权码 / 专用密码」，不要填网页登录密码。
     */
    private String password = "";

    /**
     * 发件人：可为纯邮箱，或「显示名 + 空格 + 尖括号邮箱」（如 居服通官方账号 与 admin@jumeitong.top 组合），未配置时默认使用 username
     */
    private String from = "";

    public String resolveFromAddress() {
        if (from != null && !from.isBlank()) {
            return from.trim();
        }
        return username == null ? "" : username.trim();
    }
}