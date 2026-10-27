package com.bot.dhxy.model.auth;

import lombok.Builder;
import lombok.Value;

/** Process-wide stop notification, containing no session token, license code or device fingerprint. */
@Value
@Builder
public class RuntimeAuthorizationRevoked {
    long generation;
    RuntimeAuthorizationCode code;
}
