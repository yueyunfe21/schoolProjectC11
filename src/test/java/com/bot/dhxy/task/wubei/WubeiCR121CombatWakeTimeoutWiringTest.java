package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR121 五倍 WAIT_BATTLE_FINISH combat wake scheduling.
 *
 * <p>Fresh runtime validation still needs a real expected 五倍 battle. This guard protects the
 * source wiring: 五倍 combat waits must wake for both COMBAT_STATE_CHANGED events and the same
 * fast expected-exit / maintenance deadline that 修罗 uses.</p>
 */
public class WubeiCR121CombatWakeTimeoutWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);
        String autoCombat = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/AutoCombatService.java"), StandardCharsets.UTF_8);

        wubeiWaitBattleFinishUsesDynamicCombatWakeTimeout(wubei);
        wubeiParkBoundaryRepairsCombatWakeTimeout(wubei);
        autoCombatWakeDelayIncludesFastExpectedExitProbe(autoCombat);
    }

    private static void wubeiWaitBattleFinishUsesDynamicCombatWakeTimeout(String wubei) {
        require(wubei.contains("WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS = 500L"),
                "五倍 combat wake must keep a WUBEI-named 500ms minimum timeout");
        require(wubei.contains("WUBEI_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS = 10_000L"),
                "五倍 combat wake must keep a WUBEI-named 10000ms maximum timeout");

        String waitFactory = methodBody(wubei, "private WubeiStepOutcome waitForCombatStateWake(");
        require(waitFactory.contains("WindowReadyEventType.COMBAT_STATE_CHANGED"),
                "五倍 WAIT_BATTLE_FINISH must still wake immediately on combat-state events");
        require(waitFactory.contains(".timeoutMs(wubeiCombatMaintenanceWakeTimeoutMs())"),
                "五倍 WAIT_BATTLE_FINISH timeout must be derived from the next combat wake deadline");
        require(!waitFactory.contains(".timeoutMs(WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS)"),
                "五倍 WAIT_BATTLE_FINISH must not wait forever on COMBAT_STATE_CHANGED only");

        String timeoutMethod = methodBody(wubei, "private long wubeiCombatMaintenanceWakeTimeoutMs()");
        require(timeoutMethod.contains("autoCombatService.nextCombatWakeDelayMs()"),
                "五倍 timeout helper must ask AutoCombatService for the fast expected-exit wake delay");
        require(timeoutMethod.contains("WUBEI_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS"),
                "五倍 timeout helper must cap long combat waits to a sparse maintenance wake");
        require(timeoutMethod.contains("WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS"),
                "五倍 timeout helper must avoid a tight zero-timeout loop");
        require(timeoutMethod.contains("Math.min(nextCombatWakeDelayMs, WUBEI_COMBAT_MAINTENANCE_WAKE_MAX_TIMEOUT_MS)"),
                "五倍 timeout helper must clamp the next combat wake delay to the max timeout");
        require(timeoutMethod.contains("Math.max(timeoutMs, WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS)"),
                "五倍 timeout helper must clamp the timeout to the min timeout");
    }

    private static void wubeiParkBoundaryRepairsCombatWakeTimeout(String wubei) {
        String parkMethod = methodBody(wubei,
                "private void parkAfterYieldIfNeeded(TaskExecutionContext context,");
        require(parkMethod.contains("waitSpec.getReason() == WubeiWaitReason.WAIT_COMBAT_STATE_CHANGE"),
                "final WUBEI park boundary must identify combat-state waits");
        require(parkMethod.contains("waitSpec.getTimeoutMs() < WUBEI_COMBAT_MAINTENANCE_WAKE_MIN_TIMEOUT_MS"),
                "final WUBEI park boundary must reject timeoutMs=-1/0 for combat-state waits");
        require(parkMethod.contains("waitSpec.toBuilder()")
                        && parkMethod.contains(".timeoutMs(wubeiCombatMaintenanceWakeTimeoutMs())"),
                "final WUBEI park boundary must rewrite combat-state waits to the bounded dynamic timeout");
        require(parkMethod.indexOf("waitSpec.toBuilder()")
                        < parkMethod.indexOf("windowReadyEventBus.awaitNewer("),
                "combat wait timeout must be repaired before the production awaitNewer call");
    }

    private static void autoCombatWakeDelayIncludesFastExpectedExitProbe(String autoCombat) {
        require(autoCombat.contains("public long nextCombatWakeDelayMs()"),
                "AutoCombatService must expose the next combat wake delay for task waits");
        String delayMethod = methodBody(autoCombat, "public long nextCombatWakeDelayMs()");
        require(delayMethod.contains("nextCombatMaintenanceDelayMs()"),
                "next combat wake delay must preserve normal combat maintenance deadlines");
        require(delayMethod.contains("fastExpectedExitWatchArmed"),
                "next combat wake delay must include the fast expected-exit watch gate");
        require(delayMethod.contains("battleRadarService.nextFastExpectedCombatExitProbeDelayMs()"),
                "next combat wake delay must include the lightweight avatar-diff exit probe");
        require(delayMethod.contains("Math.min(nextMaintenanceDelayMs, nextFastExitProbeDelayMs)"),
                "next combat wake delay must pick the earliest maintenance/probe deadline");
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing source marker: " + signature);
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0) {
            throw new AssertionError("Missing method body for: " + signature);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
