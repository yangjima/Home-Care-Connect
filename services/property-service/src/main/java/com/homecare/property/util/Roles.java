package com.homecare.property.util;

import org.springframework.util.StringUtils;

/**
 * 与库表 sys_user.role 对齐：admin=超级管理员，store_manager=店长，supplier=商家，tenant/user=普通用户。
 */
public final class Roles {

    public static final String ADMIN = "admin";
    public static final String STORE_MANAGER = "store_manager";
    public static final String MERCHANT = "supplier";
    public static final String TENANT = "tenant";
    public static final String USER = "user";

    private Roles() {
    }

    public static boolean isPlatformAdmin(String role) {
        return ADMIN.equals(role) || STORE_MANAGER.equals(role);
    }

    public static boolean isMerchant(String role) {
        return MERCHANT.equals(role);
    }

    /** 可发布房源（上架房子） */
    public static boolean canCreateProperty(String role) {
        return isPlatformAdmin(role) || isMerchant(role);
    }

    public static boolean isNormalUser(String role) {
        return !StringUtils.hasText(role) || TENANT.equals(role) || USER.equals(role);
    }
}
