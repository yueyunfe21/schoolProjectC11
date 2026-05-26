package com.bot.dhxy.auth;

import java.util.Locale;

public enum LicenseActionType {

    NONE("none", "无需操作"),
    NEED_CAPTCHA("need_captcha", "需要验证码"),
    RENEW_30_DAYS("renew_30_days", "30天续约"),
    RENEW_REQUIRED("renew_required", "需要续约");

    private final String code;
    private final String displayName;

    LicenseActionType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LicenseActionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }

        String normalized = code.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "CAPTCHA", "NEED_CAPTCHA", "CAPTCHA_REQUIRED", "REQUIRE_CAPTCHA" -> NEED_CAPTCHA;
            case "RENEW_30_DAYS", "RENEW_30_DAY", "RENEW_30D", "RENEWAL_30_DAYS" -> RENEW_30_DAYS;
            case "RENEW", "RENEW_REQUIRED", "RENEWAL_REQUIRED", "LICENSE_EXPIRED" -> RENEW_REQUIRED;
            default -> NONE;
        };
    }
}
