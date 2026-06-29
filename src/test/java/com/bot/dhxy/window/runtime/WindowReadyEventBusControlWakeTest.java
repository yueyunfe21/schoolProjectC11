package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowReadyEvent;
import com.bot.dhxy.window.model.WindowReadyEventType;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WindowReadyEventBusControlWakeTest {

    public static void main(String[] args) throws Exception {
        pauseWakeReturnsPromptlyFromGenericAwaitWithoutBusinessEvent();
        pauseWakeReturnsPromptlyFromPathingAwaitWithoutBusinessEvent();
        stopWakeReturnsPromptlyFromGenericAwaitWithoutBusinessEvent();
        runnerWiresPauseAndStopRequestsToReadyBus();
        wubeiFinitePreparedDialogWaitCheckpointsAfterEmptyWake();
        System.out.println("WindowReadyEventBusControlWakeTest passed");
    }

    private static void pauseWakeReturnsPromptlyFromGenericAwaitWithoutBusinessEvent() throws Exception {
        WindowReadyEventBus bus = new WindowReadyEventBus();
        String windowId = "hwnd-cr92-pause-generic";
        long afterSequence = bus.currentSequence();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Optional<WindowReadyEvent>> future = executor.submit(() -> bus.awaitNewer(
                windowId,
                EnumSet.of(WindowReadyEventType.PREPARED_ACTION_READY, WindowReadyEventType.PATHING_TERMINAL),
                afterSequence,
                5_000L));
        try {
            assertParked(future, "generic await should still be parked before pause wake");
            long elapsedMs = invokeControlWake(bus, "wakeForTaskPause", windowId, "pause requested", future);
            require(elapsedMs < 1_000L, "pause wake must return generic await promptly, elapsedMs=" + elapsedMs);
            Optional<WindowReadyEvent> result = future.get(1, TimeUnit.SECONDS);
            require(result.isEmpty(), "pause wake must not return a business ready event");
            requireNoBusinessReadyEvent(bus, windowId);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void pauseWakeReturnsPromptlyFromPathingAwaitWithoutBusinessEvent() throws Exception {
        WindowReadyEventBus bus = new WindowReadyEventBus();
        String windowId = "hwnd-cr92-pause-pathing";
        long afterSequence = bus.currentSequence();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Optional<WindowReadyEvent>> future = executor.submit(() -> bus.awaitNewerPathingTerminalOrPreparedRoute(
                windowId,
                "intent-cr92",
                "xiuluo-v2:tracker-shortcut",
                "target-map",
                afterSequence,
                5_000L));
        try {
            assertParked(future, "pathing/prepared await should still be parked before pause wake");
            long elapsedMs = invokeControlWake(bus, "wakeForTaskPause", windowId, "pause requested", future);
            require(elapsedMs < 1_000L, "pause wake must return pathing/prepared await promptly, elapsedMs=" + elapsedMs);
            Optional<WindowReadyEvent> result = future.get(1, TimeUnit.SECONDS);
            require(result.isEmpty(), "pause wake must not masquerade as PATHING_TERMINAL or PREPARED_ACTION_READY");
            requireNoBusinessReadyEvent(bus, windowId);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void stopWakeReturnsPromptlyFromGenericAwaitWithoutBusinessEvent() throws Exception {
        WindowReadyEventBus bus = new WindowReadyEventBus();
        String windowId = "hwnd-cr92-stop-generic";
        long afterSequence = bus.currentSequence();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Optional<WindowReadyEvent>> future = executor.submit(() -> bus.awaitNewer(
                windowId,
                EnumSet.of(WindowReadyEventType.PREPARED_ACTION_READY, WindowReadyEventType.PATHING_TERMINAL),
                afterSequence,
                5_000L));
        try {
            assertParked(future, "generic await should still be parked before stop wake");
            long elapsedMs = invokeControlWake(bus, "wakeForTaskStop", windowId, "stop requested", future);
            require(elapsedMs < 1_000L, "stop wake must return generic await promptly, elapsedMs=" + elapsedMs);
            Optional<WindowReadyEvent> result = future.get(1, TimeUnit.SECONDS);
            require(result.isEmpty(), "stop wake must not return a business ready event");
            requireNoBusinessReadyEvent(bus, windowId);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void runnerWiresPauseAndStopRequestsToReadyBus() throws Exception {
        String runner = Files.readString(Path.of("src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"),
                StandardCharsets.UTF_8);
        String pauseMethod = methodBlock(runner, "public boolean pauseCurrentTask()");
        String stopMethod = methodBlock(runner, "public boolean stopCurrentTask()");
        require(pauseMethod.contains("windowReadyEventBus.wakeForTaskPause("),
                "pauseCurrentTask must wake WindowReadyEventBus waiters");
        require(stopMethod.contains("windowReadyEventBus.wakeForTaskStop("),
                "stopCurrentTask must wake WindowReadyEventBus waiters");

        String bus = Files.readString(Path.of("src/main/java/com/bot/dhxy/window/runtime/WindowReadyEventBus.java"),
                StandardCharsets.UTF_8);
        require(bus.contains("\"pause-wake\""), "WindowReadyEventBus await logs must distinguish pause-wake");
        require(bus.contains("\"stop-wake\""), "WindowReadyEventBus await logs must distinguish stop-wake");
    }

    private static void wubeiFinitePreparedDialogWaitCheckpointsAfterEmptyWake() throws Exception {
        String task = Files.readString(Path.of("src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"),
                StandardCharsets.UTF_8);
        String waitMethod = methodBlock(task, "private DialogResult waitForPreparedWubeiDialog(");
        String emptyBranch = between(waitMethod, "if (ready.isEmpty()) {", "}");
        require(emptyBranch.contains("TaskCheckpoint.throwIfStopRequested(context, \"Wubei task interrupted\")"),
                "五倍 finite prepared-dialog wait must checkpoint after an empty ready-event wake");
    }

    private static long invokeControlWake(WindowReadyEventBus bus,
                                          String methodName,
                                          String windowId,
                                          String reason,
                                          Future<Optional<WindowReadyEvent>> future) throws Exception {
        Method method;
        try {
            method = WindowReadyEventBus.class.getMethod(methodName, String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("WindowReadyEventBus must expose " + methodName
                    + "(String windowId, String reason)", e);
        }
        long startedAt = System.currentTimeMillis();
        method.invoke(bus, windowId, reason);
        future.get(1, TimeUnit.SECONDS);
        return Math.max(0L, System.currentTimeMillis() - startedAt);
    }

    private static void assertParked(Future<Optional<WindowReadyEvent>> future, String message) throws Exception {
        Thread.sleep(150L);
        require(!future.isDone(), message);
    }

    private static void requireNoBusinessReadyEvent(WindowReadyEventBus bus, String windowId) {
        require(bus.latest(windowId, WindowReadyEventType.PATHING_TERMINAL).isEmpty(),
                "control wake must not publish PATHING_TERMINAL");
        require(bus.latest(windowId, WindowReadyEventType.PREPARED_ACTION_READY).isEmpty(),
                "control wake must not publish PREPARED_ACTION_READY");
        require(bus.latest(windowId, WindowReadyEventType.COMBAT_STATE_CHANGED).isEmpty(),
                "control wake must not publish COMBAT_STATE_CHANGED");
    }

    private static String methodBlock(String source, String signature) {
        int start = source.indexOf(signature);
        require(start >= 0, "missing method signature: " + signature);
        int end = source.indexOf("\n    /**", start + signature.length());
        return source.substring(start, end < 0 ? source.length() : end);
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        require(startIndex >= 0, "missing source marker: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        require(endIndex >= 0, "missing source end marker: " + end);
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
