package com.bot.dhxy.input;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Connectivity guard from the Windows hotkey dispatch to the shared UI lifecycle callback. */
public final class GlobalEmergencyStopHotkeyServiceConnectivityTest {

    @Test
    void f11UsesImmediateLifecyclePauseAndOnlyResumesWhenNothingIsRunning() throws Exception {
        main(new String[0]);
    }

    public static void main(String[] args) throws Exception {
        AtomicInteger pauseCalls = new AtomicInteger();
        AtomicInteger resumeCalls = new AtomicInteger();
        AtomicInteger stopCalls = new AtomicInteger();
        GlobalEmergencyStopHotkeyService runningService = new GlobalEmergencyStopHotkeyService(
                () -> {
                    pauseCalls.incrementAndGet();
                    return true;
                },
                resumeCalls::incrementAndGet,
                stopCalls::incrementAndGet);

        runningService.triggerPauseAll();
        require(pauseCalls.get() == 1, "F11 must attempt the immediate non-FX pause path first");
        require(resumeCalls.get() == 0, "a successful immediate pause must not dispatch resume");
        require(stopCalls.get() == 0, "F11 must not dispatch the F12 stop callback");

        runningService.triggerEmergencyStop();
        require(pauseCalls.get() == 1, "F12 must not dispatch the F11 pause callback");
        require(stopCalls.get() == 1, "F12 must retain the direct global stop callback");

        GlobalEmergencyStopHotkeyService pausedService = new GlobalEmergencyStopHotkeyService(
                () -> {
                    pauseCalls.incrementAndGet();
                    return false;
                },
                resumeCalls::incrementAndGet,
                stopCalls::incrementAndGet);
        pausedService.triggerPauseAll();
        require(pauseCalls.get() == 2, "F11 must inspect the immediate pause path before deciding to resume");
        require(resumeCalls.get() == 1, "F11 must dispatch resume when there are no running windows to pause");

        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/input/GlobalEmergencyStopHotkeyService.java"));
        require(source.contains("snapshot.getStatus() == WindowRuntimeStatus.RUNNING"),
                "F11 must use the remote lifecycle RUNNING status for immediate pause");
        require(!source.contains(".filter(snapshot -> snapshot.isRunning())"),
                "F11 must not use the legacy local-worker running boolean");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
