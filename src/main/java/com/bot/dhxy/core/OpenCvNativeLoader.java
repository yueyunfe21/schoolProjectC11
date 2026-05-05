package com.bot.dhxy.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized OpenCV native library initialization.
 */
public final class OpenCvNativeLoader {

    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private OpenCvNativeLoader() {
    }

    public static void ensureLoaded() {
        if (LOADED.compareAndSet(false, true)) {
            nu.pattern.OpenCV.loadLocally();
        }
    }
}