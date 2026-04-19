package com.homecare.user.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String USER_STORE_ID = "X-User-Store-Id";

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

    /**
     * 网关注入的当前用户所属门店（非门店岗位可能为空）
     */
    public static Long storeId(HttpServletRequest request) {
        String raw = request.getHeader(USER_STORE_ID);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
