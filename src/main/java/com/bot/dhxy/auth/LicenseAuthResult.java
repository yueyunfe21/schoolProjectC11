package com.bot.dhxy.auth;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value

@Builder

@AllArgsConstructor(access = AccessLevel.PUBLIC)

@Accessors(fluent = true)

public class LicenseAuthResult {

    boolean success;

    String code;

    String message;

    LicenseActionType actionType;

    String appId;

    String licenseCode;

    String expireText;

    String expiresAt;

    boolean bound;

    Boolean currentDeviceMatched;


    public static LicenseAuthResult failure(String code, String message) {
        return new LicenseAuthResult(
                false,
                code,
                message,
                LicenseActionType.NONE,
                "",
                "",
                "暂无到期时间",
                "",
                false,
                null
        );
    }

    public String actionDisplayName() {
        return actionType == null ? LicenseActionType.NONE.getDisplayName() : actionType.getDisplayName();
    }


}
