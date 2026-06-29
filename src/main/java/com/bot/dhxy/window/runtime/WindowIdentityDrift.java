package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowNativeBinding;

/**
 * Result of applying a refreshed native binding to a runtime context.
 *
 * @param drifted true when the same native HWND now belongs to a different parsed player/title.
 * @param windowId logical runtime window id.
 * @param oldBinding binding before the refresh.
 * @param newBinding binding after the refresh.
 * @param oldIdentity parsed identity from the old title, nullable.
 * @param newIdentity parsed identity from the new title, nullable.
 * @param epoch player-identity epoch after the refresh.
 */
public record WindowIdentityDrift(boolean drifted,
                                  String windowId,
                                  WindowNativeBinding oldBinding,
                                  WindowNativeBinding newBinding,
                                  WindowTitleIdentity oldIdentity,
                                  WindowTitleIdentity newIdentity,
                                  long epoch) {

    public boolean isDrifted() {
        return drifted;
    }

    public static WindowIdentityDrift none(String windowId,
                                           WindowNativeBinding oldBinding,
                                           WindowNativeBinding newBinding,
                                           long epoch) {
        return new WindowIdentityDrift(false, windowId, oldBinding, newBinding, null, null, epoch);
    }

    public static WindowIdentityDrift detected(String windowId,
                                               WindowNativeBinding oldBinding,
                                               WindowNativeBinding newBinding,
                                               WindowTitleIdentity oldIdentity,
                                               WindowTitleIdentity newIdentity,
                                               long epoch) {
        return new WindowIdentityDrift(true, windowId, oldBinding, newBinding, oldIdentity, newIdentity, epoch);
    }
}
