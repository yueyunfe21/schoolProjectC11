package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Selects the low-level input backend without changing task or queue semantics.
 *
 * <p>{@link Backend#WIN_API} remains the default until G041 completes its explicit-driver runtime gate.
 * Selecting {@link Backend#FAKER_INPUT} must fail closed when the virtual HID device is unavailable; it must
 * never silently route the same action through {@code SendInput}.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.input")
public class InputBackendProperties {

    private Backend backend = Backend.WIN_API;
    private int fakerInputRequiredApiVersion = 1;

    public enum Backend {
        WIN_API,
        FAKER_INPUT
    }
}
