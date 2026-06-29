package com.bot.dhxy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Boundary for host-level power actions.
 *
 * <p>This service is intentionally tiny and only called from explicit user-selected tasks. Keeping
 * the command behind one Spring service makes the sleep behavior auditable and prevents accidental
 * power actions from startup or diagnostic paths.</p>
 */
@Slf4j
@Service
public class SystemPowerService {

    /**
     * Ask Windows to enter sleep.
     *
     * @param source diagnostic source shown in logs.
     */
    public void sleepComputer(String source) {
        log.warn("system sleep requested: source={}", source);
        ProcessBuilder builder = new ProcessBuilder(
                "rundll32.exe",
                "powrprof.dll,SetSuspendState",
                "0,1,0");
        try {
            builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to request Windows sleep", e);
        }
    }
}
