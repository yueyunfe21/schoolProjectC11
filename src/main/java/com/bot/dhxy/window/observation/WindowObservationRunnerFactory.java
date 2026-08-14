package com.bot.dhxy.window.observation;

/**
 * TURN-40G: construction boundary for per-window observation runners. The turn loop calls this only after the
 * matching {@code TurnTaskStartAck} is accepted for its window — never before — so no observation exists for an
 * unacknowledged window.
 */
public interface WindowObservationRunnerFactory {

    /**
     * Creates one stopped observation runner bound to the exact acknowledged window identity.
     *
     * @param deviceId exact device identity
     * @param windowId exact logical window identity
     * @param hwnd exact native window handle at acknowledgement time
     * @param taskCode exact Cloud-authoritative child task code; never a comma-joined queue identity
     * @param taskRunId observation-plane run identity (the acknowledged start request id)
     * @return a stopped runner, or {@code null} when no observation transport is available in this process
     */
    WindowObservationRunner create(String deviceId, String windowId, String hwnd, String taskCode, String taskRunId);
}
