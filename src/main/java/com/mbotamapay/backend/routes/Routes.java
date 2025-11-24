package com.mbotamapay.backend.routes;

public class Routes {

    public static final String ADMIN = "/api/v1/admin";
    public static final String AUTH = "/api/v1/auth";
    public static final String PAYMENT = "/api/payment";
    public static final String PAYMENT_REQUESTS = "/api/v1/payment-requests";
    public static final String QR_CODE = "/api/qr";
    public static final String TRANSACTIONS = "/api/v1/transactions";
    public static final String USERS = "/api/v1/users";
    public static final String WALLET = "/api/v1/wallet";
    public static final String KYC = "/api/v1/kyc";
    public static final String DASHBOARD = "/api/v1/admin/dashboard";
    public static final String RECURRING_PAYMENTS = "/api/v1/recurring-payments";
    public static final String WS = "/ws";
    public static final String ACTUATOR = "/actuator";
    public static final String SUPPORT = "/api/v1/support";

    private Routes() {
        // Private constructor to prevent instantiation
    }
}
