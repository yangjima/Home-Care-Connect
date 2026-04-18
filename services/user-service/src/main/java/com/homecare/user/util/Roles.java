package com.homecare.user.util;

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
}
