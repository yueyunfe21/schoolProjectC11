package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * One dedicated exact-window frame captured by the local Runner for a stationary pathing candidate.
 *
 * <p>This carrier is deliberately separate from {@link ObservationRoi}: it allows one bounded
 * 1024x768 PNG without weakening the ordinary small-ROI limits. Identity is repeated here so a
 * stale frame cannot be detached from the request that transported it.</p>
 *
 * @param frameId positive per-window frame identity
 * @param pathingGeneration positive generation of the exact active pathing intent
 * @param tenantId exact tenant identity
 * @param deviceId exact device identity
 * @param windowId exact logical window identity
 * @param hwnd exact native window handle
 * @param taskRunId exact business task run identity
 * @param intentId exact pathing intent identity
 * @param left window-client X origin, normally zero
 * @param top window-client Y origin, normally zero
 * @param width frame width in pixels
 * @param height frame height in pixels
 * @param encoding image encoding; only {@code PNG} is accepted
 * @param capturedAtMs epoch millis of the exact capture
 * @param pngBytes encoded frame bytes
 */
public record ObservationTerminalFrame(
        long frameId,
        long pathingGeneration,
        String tenantId,
        String deviceId,
        String windowId,
        String hwnd,
        String taskRunId,
        String intentId,
        int left,
        int top,
        int width,
        int height,
        String encoding,
        long capturedAtMs,
        byte[] pngBytes) {
}
