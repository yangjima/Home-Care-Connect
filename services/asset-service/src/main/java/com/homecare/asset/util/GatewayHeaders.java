package com.homecare.asset.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private GatewayHeaders() {
    }

    public static Long userId(HttpServletRequest request) {
        String raw = request.getHeader(USER_ID);
        if (!StringUtils.hasText(raw)) {
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
