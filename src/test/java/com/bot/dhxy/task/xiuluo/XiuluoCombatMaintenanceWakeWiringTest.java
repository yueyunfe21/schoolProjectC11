package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR104 修罗 WAIT_COMBAT maintenance wake scheduling.
 */
public class XiuluoCombatMaintenanceWakeWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String autoCombat = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"), StandardCharsets.UTF_8);

        String waitFactory = between(xiuluo,
                "private XiuluoStepOutcome waitForCombatStateWake(",
                "private Optional<XiuluoStepOutcome> suppressUnknownCombatExitIfActiveCombat(");
        require(waitFactory.contains("WindowReadyEventType.COMBAT_STATE_CHANGED"),
                "WAIT_COMBAT must still wake immediately on combat-state events");
        require(waitFactory.contains("combatMaintenanceWakeTimeoutMs()"),
                "WAIT_COMBAT wait timeout must be derived from due auto-combat maintenance");
        require(!waitFactory.contains(".timeoutMs(WAIT_COMBAT_STATE_CHANGE_TIMEOUT_MS)"),
                "WAIT_COMBAT must not park forever on COMBAT_STATE_CHANGED only");

        String timeoutMethod = between(xiuluo,
                "private long combatMaintenanceWakeTimeoutMs()",
                "private Optional<XiuluoStepOutcome> suppressUnknownCombatExitIfActiveCombat(");
        require(timeoutMethod.contains("autoCombatService.nextCombatMaintenanceDelayMs()"),
                "Xiuluo timeout helper must ask AutoCombatService for the next due maintenance delay");
        require(timeoutMethod.contains("WAIT_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS"),
                "Xiuluo timeout helper must cap long combat waits to a sparse maintenance wake");
        require(timeoutMethod.contains("WAIT_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS"),
                "Xiuluo timeout helper must avoid a tight zero-timeout loop");

        require(autoCombat.contains("public long nextCombatMaintenanceDelayMs()"),
                "AutoCombatService must expose the next due combat maintenance delay for task waits");
        String delayMethod = between(autoCombat,
                "public long nextCombatMaintenanceDelayMs()",
                "private void maybeHandleCombatEnter(");
        require(delayMethod.contains("pendingCombatEntryMaintenanceAt"),
                "next maintenance delay must include the 4s combat-entry maintenance deadline");
        require(delayMethod.contains("COMBAT_UI_CLEAN_INTERVAL_MS"),
                "next maintenance delay must include sparse in-combat generic UI cleanup");
        require(delayMethod.contains("getAutoBattleRefreshIntervalMs()"),
                "next maintenance delay must include configured auto-combat panel refresh");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
