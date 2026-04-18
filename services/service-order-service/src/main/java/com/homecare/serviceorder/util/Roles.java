package com.homecare.serviceorder.util;

public final class Roles {

    public static final String ADMIN = "admin";
    public static final String STORE_MANAGER = "store_manager";
    public static final String MERCHANT = "supplier";

    private Roles() {
    }

    public static boolean isPlatformAdmin(String role) {
        return ADMIN.equals(role) || STORE_MANAGER.equals(role);
    }

    public static boolean isMerchant(String role) {
        return MERCHANT.equals(role);
    }

    public static boolean isStaffOperator(String role) {
        return isPlatformAdmin(role);
    }

    public static boolean canListAllOrders(String role) {
        return isPlatformAdmin(role) || isMerchant(role);
    }
}
