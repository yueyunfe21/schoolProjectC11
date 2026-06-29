package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogPreparationRequest;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRuntimeStatus;

import java.util.List;

/**
 * Behavior test for CR95 same-HWND title/player drift handling.
 */
public class WindowRuntimeIdentityDriftGuardTest {

    public static void main(String[] args) {
        String oldTitle = "大话西游2经典版 - 江山如画 - 忆叶知秋（ID：451753529）";
        String newTitle = "大话西游2经典版 - 江山如画 - うprinoe大叔（ID：316365558）";

        WindowRuntimeContext runtime = new WindowRuntimeContext("hwnd-E850B6A", new GameContext());
        WindowIdentityDrift first = runtime.setNativeBinding(binding(oldTitle, 10, 20));
        assertFalse(first.isDrifted(), "initial registration must not count as drift");
        runtime.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.WUBEI)
                .operations(List.of(DialogOperation.WUBEI_ENTER_BATTLE))
                .source("test")
                .build(), "test");
        runtime.updateDialogPreparationRequest(DialogPreparationRequest.builder()
                .operation(DialogOperation.WUBEI_ENTER_BATTLE)
                .targetKeyword("白龙马")
                .source("test")
                .createdAtMs(System.currentTimeMillis())
                .build());
        runtime.markStarted(TaskType.AUTO_BATTLE);
        runtime.getGameState().setBotStatus(GameContext.BotStatus.RUNNING);

        long beforeEpoch = runtime.getPlayerIdentityEpoch();
        WindowIdentityDrift drift = runtime.setNativeBinding(binding(newTitle, 10, 20));
        assertTrue(drift.isDrifted(), "same HWND player change should be drift");
        assertEquals(beforeEpoch + 1, runtime.getPlayerIdentityEpoch(), "identity epoch");
        assertEquals("うprinoe大叔", runtime.getGameState().getMe().getName(), "player name");
        assertEquals("316365558", runtime.getGameState().getMe().getId(), "player id");
        assertEquals(GameContext.BotStatus.RUNNING, runtime.getGameState().getBotStatus(),
                "busy same-HWND drift must not stop the running task");
        assertEquals(WindowRuntimeStatus.RUNNING, runtime.getStatus(),
                "busy same-HWND drift must preserve window running status");
        assertTrue(runtime.isIdentitySuspended(), "busy same-HWND drift should suspend task input");
        assertEquals("451753529", runtime.getTaskOwnerPlayerId(), "task owner player id");
        assertEquals("316365558", runtime.getVisiblePlayerId(), "visible drift player id");
        assertTrue(runtime.getDialogInterest().isEmpty(), "dialog interest should clear");
        assertNull(runtime.getDialogPreparationRequest(), "dialog preparation should clear");

        WindowIdentityDrift geometryOnly = runtime.setNativeBinding(binding(newTitle, 30, 40));
        assertFalse(geometryOnly.isDrifted(), "same title geometry refresh is not identity drift");
        assertEquals(beforeEpoch + 1, runtime.getPlayerIdentityEpoch(), "geometry-only must not bump epoch");

        WindowIdentityDrift driftBack = runtime.setNativeBinding(binding(oldTitle, 40, 50));
        assertTrue(driftBack.isDrifted(), "same HWND player change back to owner should still be drift");
        assertFalse(runtime.isIdentitySuspended(), "returning to owner player id should resume task input");
        assertEquals("451753529", runtime.getVisiblePlayerId(), "visible player id after returning to owner");
        assertEquals(GameContext.BotStatus.RUNNING, runtime.getGameState().getBotStatus(),
                "resume from identity suspension must not stop the running task");

        WindowNativeBinding staleOldBlankRefresh = binding(oldTitle, 50, 60)
                .withLiveState(" ", "xy2", 123L, 50, 60, 1024, 768);
        WindowIdentityDrift blankTitle = runtime.setNativeBinding(staleOldBlankRefresh);
        assertFalse(blankTitle.isDrifted(), "late blank refresh from old cached title must not roll identity back");
        assertEquals(beforeEpoch + 2, runtime.getPlayerIdentityEpoch(), "blank title must not bump epoch");
        assertEquals("忆叶知秋", runtime.getGameState().getMe().getName(), "blank title must keep current player name");
        assertEquals(oldTitle, runtime.getNativeBinding().getTitle(), "blank title must preserve runtime current title");

        System.out.println("WindowRuntimeIdentityDriftGuardTest passed");
    }

    private static WindowNativeBinding binding(String title, int x, int y) {
        return new WindowNativeBinding("243600234", title, "xy2", 123L, x, y, 1024, 768);
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message + " actual=" + value);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
