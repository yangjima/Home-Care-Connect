package com.homecare.property.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 从 API Gateway 注入的请求头解析当前用户（JWT 校验后写入）。
 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private GatewayHeaders() {
    }

    public static Long userId(HttpServletRequest request) {
        String raw = request.getHeader(USER_ID);
        if (!StringUtils.hasText(raw)) {
            Object attr = request.getAttribute("userId");
            if (attr instanceof Number) {
                return ((Number) attr).longValue();
            }
            if (attr != null) {
                try {
                    return Long.parseLong(String.valueOf(attr));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String role(HttpServletRequest request) {
        String r = request.getHeader(USER_ROLE);
        return StringUtils.hasText(r) ? r.trim() : "";
    }
}
