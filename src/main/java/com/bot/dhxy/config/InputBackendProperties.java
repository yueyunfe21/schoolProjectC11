package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Selects the low-level input backend without changing task or queue semantics.
 *
 * <p>{@link Backend#FAKER_INPUT} is the production default after G041 passed its explicit-driver runtime gate.
 * A missing or unavailable virtual HID device must fail closed; the application must never silently route the
 * same action through {@code SendInput}. {@link Backend#WIN_API} remains available only as an explicit setting.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.input")
public class InputBackendProperties {

    private Backend backend = Backend.FAKER_INPUT;
    private int fakerInputRequiredApiVersion = 1;

    public enum Backend {
        WIN_API,
        FAKER_INPUT
    }
}
