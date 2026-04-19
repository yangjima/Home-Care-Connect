package com.homecare.user.service.mail;

import com.homecare.user.common.BusinessException;
import com.homecare.user.config.mail.AppMailProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 通过 SMTP 发送邮箱验证码（如 139 邮箱：smtp.139.com:465 + SSL）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationMailService {

    private final AppMailProperties mailProperties;

    /**
     * 发送验证码邮件；失败时抛出业务异常，由调用方决定是否写入 Redis
     */
    public void sendVerificationCode(String toEmail, String code) {
        if (!mailProperties.isEnabled()) {
            throw new BusinessException(503, "邮件验证码服务未启用，请配置 MAIL_ENABLED=true 及 SMTP 参数");
        }
        if (!StringUtils.hasText(mailProperties.getHost())) {
            throw new BusinessException(503, "邮件服务未配置 SMTP 主机（MAIL_HOST）");
        }
        if (!StringUtils.hasText(mailProperties.getUsername()) || !StringUtils.hasText(mailProperties.getPassword())) {
            throw new BusinessException(503, "邮件服务未配置发件账号或授权码（MAIL_USERNAME / MAIL_PASSWORD）");
        }
        String from = mailProperties.resolveFromAddress();
        if (!StringUtils.hasText(from)) {
            throw new BusinessException(503, "邮件服务未配置发件人地址（MAIL_FROM 或 MAIL_USERNAME）");
        }

        JavaMailSenderImpl sender = buildSender();
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(parseFromAddress(from));
            helper.setTo(toEmail);
            helper.setSubject("居服通验证码");
            helper.setText(buildTextBody(code), false);
            sender.send(message);
            log.info("邮箱验证码已发送 to={}", maskEmail(toEmail));
        } catch (Exception e) {
            log.warn("发送验证码邮件失败 to={}: {}", maskEmail(toEmail), e.getMessage());
            throw new BusinessException(502, "验证码邮件发送失败，请稍后重试", e);
        }
    }

    private JavaMailSenderImpl buildSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost().trim());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername().trim());
        mailSender.setPassword(mailProperties.getPassword());

        Properties p = new Properties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", "true");
        if (mailProperties.isSsl()) {
            p.put("mail.smtp.ssl.enable", "true");
        } else {
            p.put("mail.smtp.starttls.enable", "true");
        }
        mailSender.setJavaMailProperties(p);
        return mailSender;
    }

    /**
     * 支持纯邮箱，或 RFC 风格「显示名 + 空格 + 尖括号邮箱」，中文显示名使用 UTF-8 编码写入邮件头。
     */
    private static InternetAddress parseFromAddress(String fromSpec) throws Exception {
        String spec = fromSpec.trim();
        int lt = spec.indexOf('<');
        int gt = spec.indexOf('>');
        if (lt != -1 && gt > lt) {
            String personal = spec.substring(0, lt).trim();
            String email = spec.substring(lt + 1, gt).trim();
            if (StringUtils.hasText(personal)) {
                return new InternetAddress(email, personal, StandardCharsets.UTF_8.name());
            }
            return new InternetAddress(email);
        }
        return new InternetAddress(spec);
    }

    private static String buildTextBody(String code) {
        return "您的验证码为：" + code + "\n\n五分钟内有效。如非本人操作，请忽略本邮件。";
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "*" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
