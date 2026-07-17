package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.driver.BoundWindowKeyboardService;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/** Resolves only keyboard taps already validated for HWND background delivery. */
@Component
public final class TurnKeyMapper {

    /**
     * Resolve a wire key to an existing background-validated Alt shortcut.
     *
     * @param key wire key such as {@code Alt+Q} or {@code ALT_Q}; nullable values are unsupported.
     * @return validated shortcut, or empty when no existing background API safely expresses it.
     */
    public Optional<BoundWindowKeyboardService.AltShortcut> findBackgroundTap(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String candidate = key.trim();
        String enumName = candidate.toUpperCase(Locale.ROOT).replace('+', '_');
        for (BoundWindowKeyboardService.AltShortcut shortcut
                : BoundWindowKeyboardService.AltShortcut.values()) {
            if (shortcut.backgroundHwndSupported()
                    && (shortcut.displayName().equalsIgnoreCase(candidate)
                    || shortcut.name().equals(enumName))) {
                return Optional.of(shortcut);
            }
        }
        return Optional.empty();
    }
}
