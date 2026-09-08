package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Selects the low-level input backend without changing task or queue semantics.
 *
 * <p>{@link Backend#FAKER_INPUT} is the only backend (G146: the WIN_API SendInput/SetCursorPos
 * controller and the PostMessage keyboard path are deleted). A missing or unavailable virtual HID
 * device must fail closed; there is no fallback input route of any kind.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.input")
public class InputBackendProperties {

    private Backend backend = Backend.FAKER_INPUT;
    private int fakerInputRequiredApiVersion = 1;

    public enum Backend {
        FAKER_INPUT
    }
}
