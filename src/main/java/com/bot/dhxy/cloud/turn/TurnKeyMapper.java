package com.bot.dhxy.cloud.turn;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/** Resolves wire keys against the closed keyboard vocabulary the input queue can express. */
@Component
public final class TurnKeyMapper {

    /**
     * Resolve a wire key to a supported Alt shortcut.
     *
     * @param key wire key such as {@code Alt+Q} or {@code ALT_Q}; nullable values are unsupported.
     * @return validated shortcut, or empty when no existing queue action safely expresses it.
     */
    public Optional<TurnKeyboardKeys.AltShortcut> findBackgroundTap(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String candidate = key.trim();
        String enumName = candidate.toUpperCase(Locale.ROOT).replace('+', '_');
        for (TurnKeyboardKeys.AltShortcut shortcut : TurnKeyboardKeys.AltShortcut.values()) {
            if (shortcut.displayName().equalsIgnoreCase(candidate)
                    || shortcut.name().equals(enumName)) {
                return Optional.of(shortcut);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolve a wire key to a supported Ctrl chord.
     *
     * @param key wire key such as {@code Ctrl+A} or {@code CTRL_U}; nullable values are unsupported.
     * @return validated chord, or empty when no existing queue action safely expresses it.
     */
    public Optional<TurnKeyboardKeys.ControlShortcut> findControlShortcut(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String candidate = key.trim();
        String enumName = candidate.toUpperCase(Locale.ROOT).replace('+', '_');
        for (TurnKeyboardKeys.ControlShortcut shortcut : TurnKeyboardKeys.ControlShortcut.values()) {
            if (shortcut.displayName().equalsIgnoreCase(candidate)
                    || shortcut.name().equals(enumName)) {
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
     * Resolve a wire key to the closed modifier used by KEY_DOWN/KEY_UP.
     *
     * @param key wire key such as {@code Ctrl} or {@code Control}; nullable values are unsupported.
     * @return validated modifier, or empty when no existing queue action expresses it.
     */
    public Optional<TurnKeyboardKeys.ModifierKey> findModifierKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String candidate = key.trim();
        if (candidate.equalsIgnoreCase("Ctrl") || candidate.equalsIgnoreCase("Control")) {
            return Optional.of(TurnKeyboardKeys.ModifierKey.CONTROL);
        }
        return Optional.empty();
    }
}
