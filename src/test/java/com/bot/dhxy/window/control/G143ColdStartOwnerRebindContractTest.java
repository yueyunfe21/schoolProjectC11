package com.bot.dhxy.window.control;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G143：手动新开任务必须以重扫后的 exact-HWND 角色为本 run owner。 */
class G143ColdStartOwnerRebindContractTest {

    @Test
    void twoSwappedWindowsMustCaptureTheirCurrentlyVisiblePlayersForTheNewRun() {
        WindowRuntimeContext left = previousRun("hwnd-1900A7E", "乌龟的黑头。", "468413511");
        WindowRuntimeContext right = previousRun("hwnd-6E0B72", "黑精的皮牛。", "468413473");

        beginNewRunAfterRescan(left, "黑精的皮牛。", "468413473");
        beginNewRunAfterRescan(right, "乌龟的黑头。", "468413511");

        assertEquals("468413473", left.getTaskOwnerPlayerId(),
                "3473 must own the run on the HWND where the new scan actually found 3473");
        assertEquals("468413511", right.getTaskOwnerPlayerId(),
                "3511 must own the run on the HWND where the new scan actually found 3511");
    }

    @Test
    void allFiveRotatedWindowsMustUseThePostScanIdentityRatherThanThePreviousMapping() {
        String[] ids = {"468413443", "468413465", "468413473", "468413511", "468413519"};
        String[] names = {"单飞打手机。", "光牛的滑子。", "黑精的皮牛。", "乌龟的黑头。", "火鸡味锅巴。"};
        WindowRuntimeContext[] contexts = new WindowRuntimeContext[ids.length];
        for (int index = 0; index < ids.length; index++) {
            contexts[index] = previousRun("hwnd-g143-" + index, names[index], ids[index]);
        }

        for (int index = 0; index < ids.length; index++) {
            int rotated = (index + 1) % ids.length;
            beginNewRunAfterRescan(contexts[index], names[rotated], ids[rotated]);
            assertEquals(ids[rotated], contexts[index].getTaskOwnerPlayerId(),
                    "every selected HWND must independently adopt its post-scan player");
        }
    }

    @Test
    void aSwitchDuringTheStartAcknowledgementMustNotReplaceTheFrozenRunOwner() {
        WindowRuntimeContext context = previousRun("hwnd-1900A7E", "乌龟的黑头。", "468413511");
        context.setNativeBinding(binding("hwnd-1900A7E", "黑精的皮牛。", "468413473"));
        context.clearTaskExecutionState("G143 new task boundary");
        context.captureTaskOwnerForNewRun();

        context.setNativeBinding(binding("hwnd-1900A7E", "乌龟的黑头。", "468413511"));
        context.markStarted(TaskType.DALISI_QUIZ, null);

        assertEquals("468413473", context.getTaskOwnerPlayerId(),
                "the role seen at submission owns this exact run even if the title switches before ACK");
        assertEquals("468413511", context.getVisiblePlayerId());
        assertTrue(context.isIdentitySuspended(),
                "the pre-ACK switch must engage the existing safety gate instead of changing owner");
    }

    @Test
    void productionMustSnapshotTheLiveOwnerIntoTheImmutableRecoveryPlan() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java"));
        int freshBoundary = source.indexOf("runner.prepareRemoteFreshStart(");
        int liveRefresh = source.indexOf("bindingRefreshService.refreshAndCommit(context);", freshBoundary);
        int ownerCapture = source.indexOf("context.captureTaskOwnerForNewRun();", liveRefresh);
        int recoveryPlan = source.indexOf("new RemoteTerminalRecoveryPlan(", ownerCapture);
        int guard = source.indexOf("private boolean holdRestartUntilTaskOwnerReturns(");
        int planOwner = source.indexOf("recoveryPlan.taskOwnerPlayerId()", guard);
        int mutableOwner = source.indexOf("context.getTaskOwnerPlayerId()", guard);

        assertTrue(freshBoundary >= 0 && liveRefresh > freshBoundary && ownerCapture > liveRefresh,
                "a normal new task must refresh the exact HWND and capture its owner after clearing the old run");
        assertTrue(recoveryPlan > ownerCapture,
                "the immutable recovery plan must be built only after the new owner is captured");
        assertTrue(planOwner > guard && mutableOwner > planOwner,
                "restart must trust this run's immutable owner before mutable or previous-run caches");
        assertTrue(source.contains("startupMode == TaskStartupMode.NORMAL")
                        && source.contains("startupMode == TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP"),
                "the forced recapture belongs to new-task starts, not PAUSE_RESUME");
    }

    private static WindowRuntimeContext previousRun(String windowId, String name, String id) {
        WindowRuntimeContext context = new WindowRuntimeContext(windowId, new GameContext());
        context.setNativeBinding(binding(windowId, name, id));
        context.markStarted(TaskType.DALISI_QUIZ, null);
        context.markFinished(WindowRuntimeStatus.STOPPED, "previous run finished");
        return context;
    }

    private static void beginNewRunAfterRescan(WindowRuntimeContext context, String name, String id) {
        context.setNativeBinding(binding(context.getWindowId(), name, id));
        context.clearTaskExecutionState("G143 new task boundary");
        context.captureTaskOwnerForNewRun();
        context.markStarted(TaskType.DALISI_QUIZ, null);
    }

    private static WindowNativeBinding binding(String windowId, String playerName, String playerId) {
        String digits = windowId.replaceAll("[^0-9]", "");
        String nativeHandle = digits.isEmpty() ? "143" : digits;
        return new WindowNativeBinding(
                nativeHandle,
                "大话西游2经典版 $Revision：2048846 - 盛世华章 - " + playerName + "（ID：" + playerId + "）",
                "DHXYJYMainFrame",
                11988L,
                100,
                100,
                1036,
                783);
    }
}
