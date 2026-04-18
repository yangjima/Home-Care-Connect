package com.homecare.asset.util;

public final class Roles {

    public static final String ADMIN = "admin";
    public static final String STORE_MANAGER = "store_manager";
    public static final String MERCHANT = "supplier";

    private Roles() {
    }

    public static boolean isPlatformAdmin(String role) {
        return ADMIN.equals(role) || STORE_MANAGER.equals(role);
    }

    public static boolean canManageProcurement(String role) {
        return isPlatformAdmin(role) || MERCHANT.equals(role);
    }
}
