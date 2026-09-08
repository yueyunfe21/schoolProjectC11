package com.bot.dhxy.cloud.turn;

/**
 * Closed keyboard vocabulary for wire-key resolution (G146).
 *
 * <p>These enums used to live on the PostMessage keyboard service and carried Win32 virtual-key and
 * scan codes. Delivery is now HID-only through {@code InputProvider}, so only the closed name set
 * survives; the mapper resolves wire spellings against it and the executor maps each constant to its
 * serialized queue action.</p>
 */
public final class TurnKeyboardKeys {

    private TurnKeyboardKeys() {
    }

    /** Closed Alt-tap vocabulary the input queue can express. */
    public enum AltShortcut {
        ALT_1("Alt+1"),
        ALT_2("Alt+2"),
        ALT_4("Alt+4"),
        ALT_5("Alt+5"),
        ALT_6("Alt+6"),
        ALT_8("Alt+8"),
        ALT_Q("Alt+Q"),
        ALT_T("Alt+T"),
        ALT_O("Alt+O"),
        ALT_E("Alt+E"),
        ALT_A("Alt+A"),
        ALT_B("Alt+B"),
        ALT_C("Alt+C"),
        ALT_U("Alt+U");

        private final String displayName;

        AltShortcut(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** Closed Ctrl-chord vocabulary the input queue can express. */
    public enum ControlShortcut {
        CTRL_A("Ctrl+A"),
        CTRL_U("Ctrl+U");

        private final String displayName;

        ControlShortcut(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** Closed modifier vocabulary for KEY_DOWN/KEY_UP; V1 supports CONTROL only. */
    public enum ModifierKey {
        CONTROL
    }
}
