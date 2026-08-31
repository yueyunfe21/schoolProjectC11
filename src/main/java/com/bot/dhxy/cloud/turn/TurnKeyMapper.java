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

    /**
     * Resolve a wire key to an existing background-validated Ctrl chord.
     *
     * @param key wire key such as {@code Ctrl+A} or {@code CTRL_U}; nullable values are unsupported.
     * @return validated chord, or empty when no existing background API safely expresses it.
     */
    public Optional<BoundWindowKeyboardService.ControlShortcut> findControlShortcut(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String candidate = key.trim();
        String enumName = candidate.toUpperCase(Locale.ROOT).replace('+', '_');
        for (BoundWindowKeyboardService.ControlShortcut shortcut
                : BoundWindowKeyboardService.ControlShortcut.values()) {
            if (shortcut.backgroundHwndSupported()
                    && (shortcut.displayName().equalsIgnoreCase(candidate)
                    || shortcut.name().equals(enumName))) {
                return Optional.of(shortcut);
            }
        }
        return Optional.empty();
    }

    /**
     * @param key wire key spelling; nullable values are not Enter.
     * @return {@code true} only for the closed Enter spellings.
     */

    /** Ctrl+Space（切输入法到英文）：只认这一个写法族，避免与普通 Ctrl 快捷键混淆。 */
    public boolean isImeToggleKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String candidate = key.trim().toUpperCase(java.util.Locale.ROOT).replace(" ", "");
        return "CTRL+SPACE".equals(candidate) || "CTRL_SPACE".equals(candidate);
    }
    public boolean isEnterKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String candidate = key.trim();
        return candidate.equalsIgnoreCase("Enter") || candidate.equalsIgnoreCase("Return");
    }

    /**
     * Resolve a wire key to the closed exact-HWND background modifier used by KEY_DOWN/KEY_UP.
     *
     * @param key wire key such as {@code Ctrl} or {@code Control}; nullable values are unsupported.
     * @return validated modifier, or empty when no existing background API expresses it.
     */
    public Optional<BoundWindowKeyboardService.ModifierKey> findModifierKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String candidate = key.trim();
        if (candidate.equalsIgnoreCase("Ctrl") || candidate.equalsIgnoreCase("Control")) {
            return Optional.of(BoundWindowKeyboardService.ModifierKey.CONTROL);
        }
        return Optional.empty();
    }
}
