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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G142：可恢复终局必须守住本次 run 的主人，不能拿上一轮角色永久扣住自动重启。 */
class G142RecoverableRestartOwnerContractTest {

    private static final String OLD_OWNER_ID = "468413519";
    private static final String CURRENT_OWNER_ID = "468413473";

    @Test
    void theRetainedCurrentRunOwnerMustWinOverThePreviousRunOwner() throws Exception {
        WindowRuntimeContext context = new WindowRuntimeContext("hwnd-6E0B72", new GameContext());
        context.setNativeBinding(binding("火鸡味锅巴。", OLD_OWNER_ID));
        context.markStarted(TaskType.WUHUAN_V3, null);
        context.markFinished(WindowRuntimeStatus.ERROR, "previous run failed");

        context.setNativeBinding(binding("黑精的皮牛。", CURRENT_OWNER_ID));
        context.markStarted(TaskType.WUHUAN_V3, null);

        assertEquals(CURRENT_OWNER_ID, context.getTaskOwnerPlayerId(),
                "3473 is the owner captured for the currently retained run");
        assertEquals(OLD_OWNER_ID, context.getLastTaskOwnerPlayerId(),
                "3519 remains the previous-run fallback, reproducing the incident shape");

        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java"));
        int methodStart = source.indexOf("private boolean holdRestartUntilTaskOwnerReturns(");
        int methodEnd = source.indexOf("private void recoverRemoteTerminal(", methodStart);
        String method = source.substring(methodStart, methodEnd);
        int activeRead = method.indexOf("context.getTaskOwnerPlayerId()");
        int lastFallback = method.indexOf("context.getLastTaskOwnerPlayerId()", activeRead);

        assertTrue(activeRead >= 0, "the guard must read the retained current-run owner");
        assertTrue(lastFallback > activeRead, "lastTaskOwner is fallback only, never the first choice");
        assertTrue(method.contains("if (activeOwnerId != null)")
                        && method.contains("expectedOwnerId = activeOwnerId;"),
                "a present current-run owner must win over stale previous-run memory");
    }

    @Test
    void aTemporarySwitchStillSuspendsUntilTheSameCurrentOwnerReturns() {
        WindowRuntimeContext context = new WindowRuntimeContext("hwnd-6E0B72", new GameContext());
        context.setNativeBinding(binding("黑精的皮牛。", CURRENT_OWNER_ID));
        context.markStarted(TaskType.WUHUAN_V3, null);

        context.setNativeBinding(binding("火鸡味锅巴。", OLD_OWNER_ID));
        assertEquals(CURRENT_OWNER_ID, context.getTaskOwnerPlayerId(),
                "switching the visible role must not replace the task owner");
        assertEquals(OLD_OWNER_ID, context.getVisiblePlayerId());
        assertTrue(context.isIdentitySuspended(), "the task must pause while another role is visible");

        context.setNativeBinding(binding("黑精的皮牛。", CURRENT_OWNER_ID));
        assertEquals(CURRENT_OWNER_ID, context.getVisiblePlayerId());
        assertFalse(context.isIdentitySuspended(), "switching back to the task owner must resume the gate");
    }

    private static WindowNativeBinding binding(String playerName, String playerId) {
        return new WindowNativeBinding(
                "7211890",
                "大话西游2经典版 $Revision：2048846 - 盛世华章 - " + playerName + "（ID：" + playerId + "）",
                "DHXYJYMainFrame",
                11988L,
                7,
                599,
                1036,
                783);
    }
}
