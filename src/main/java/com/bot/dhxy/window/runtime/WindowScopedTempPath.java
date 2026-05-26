package com.bot.dhxy.window.runtime;

import com.bot.dhxy.config.WindowIsolationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Builds window-scoped temp image paths so concurrent windows do not overwrite
 * each other's screenshots and OCR inputs.
 */
@Component
public class WindowScopedTempPath {

    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowIsolationProperties windowIsolationProperties;

    public WindowScopedTempPath(WindowTaskContextHolder windowTaskContextHolder,
                                WindowIsolationProperties windowIsolationProperties) {
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowIsolationProperties = windowIsolationProperties;
    }

    public String resolve(String fileName) {
        String safeName = sanitizeFileName(fileName);
        if (!windowIsolationProperties.isScopedTempPathActive()) {
            return Path.of("images", "temp", safeName).toString();
        }
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            return Path.of("images", "temp", safeName).toString();
        }
        String safeWindowId = sanitizePathSegment(current.get().getWindowId());
        return Path.of("images", "temp", safeWindowId, safeName).toString();
    }

    private String sanitizeFileName(String value) {
        String name = value == null || value.isBlank() ? "temp.png" : value.trim();
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return sanitizePathSegment(name);
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
