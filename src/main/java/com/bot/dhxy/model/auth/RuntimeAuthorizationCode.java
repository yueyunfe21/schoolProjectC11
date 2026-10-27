package com.bot.dhxy.model.auth;

/** Closed authorization error vocabulary; unknown server errors are not turned into business failures. */
public enum RuntimeAuthorizationCode {
    CLIENT_BINDING_REQUIRED,
    CLIENT_BINDING_DISABLED,
    CLIENT_BINDING_MISMATCH,
    CLIENT_SESSION_REQUIRED,
    CLIENT_SESSION_DENIED,
    CLIENT_SESSION_INVALID,
    LICENSE_APP_MISMATCH,
    LICENSE_DISABLED,
    LICENSE_EXPIRED,
    DEVICE_BINDING_REQUIRED,
    DEVICE_MISMATCH,
    LEGACY_LICENSE_APP_REQUIRED,
    CLIENT_VERSION_REQUIRED,
    CLIENT_VERSION_REVOKED,
    CLIENT_VERSION_NOT_ALLOWED,
    CLIENT_UPDATE_REQUIRED,
    UNKNOWN;

    public static RuntimeAuthorizationCode fromWire(String value) {
        if (value == null) return UNKNOWN;
        try { return valueOf(value); }
        catch (IllegalArgumentException unknown) { return UNKNOWN; }
    }

    public boolean requiresUpdate() {
        return this == CLIENT_VERSION_REQUIRED || this == CLIENT_VERSION_REVOKED ||
                this == CLIENT_VERSION_NOT_ALLOWED || this == CLIENT_UPDATE_REQUIRED;
    }
}
