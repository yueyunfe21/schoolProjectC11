package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR62 Xiuluo WAIT_COMBAT scheduling.
 *
 * <p>Fresh runtime validation needs a real 修罗 battle. This guard protects the wiring shape:
 * when 修罗 sees combat still running, the task must release the turn and park on the current
 * window's COMBAT_STATE_CHANGED event instead of relying only on the fixed 900ms handoff loop.</p>
 */
public class XiuluoWaitCombatEventWakeWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String outcome = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoStepOutcome.java"), StandardCharsets.UTF_8);

        String waitCombat = between(task,
                "private XiuluoStepOutcome waitCombat(",
                "private XiuluoStepOutcome resolveUnknownCombatExit(");
        require(waitCombat.contains("waitForCombatStateWake("),
                "WAIT_COMBAT IN_COMBAT branch must return an event-wait outcome");
        require(!waitCombat.contains("return XiuluoStepOutcome.sharedState(combatState, \"combat still running\");"),
                "WAIT_COMBAT must not use the old bare sharedState 900ms handoff loop");

        String yieldBlock = between(task,
                "private XiuluoStepOutcome yieldAfterMustYield(",
                "private long handoffDelayMs(");
        require(yieldBlock.contains("parkAfterYieldIfNeeded(context, outcome)"),
                "MUST_YIELD path must park after releasing the turn when a wait spec is present");

        String parkBlock = between(task,
                "private XiuluoStepOutcome parkAfterYieldIfNeeded(",
                "private long handoffDelayMs(");
        require(parkBlock.contains("windowReadyEventBus.awaitNewer("),
                "Xiuluo wait parking must use WindowReadyEventBus.awaitNewer");
        String waitFactory = between(task,
                "private XiuluoStepOutcome waitForCombatStateWake(",
                "private long combatMaintenanceWakeTimeoutMs()");
        require(waitFactory.contains("WindowReadyEventType.COMBAT_STATE_CHANGED"),
                "Xiuluo WAIT_COMBAT wait spec must wait for COMBAT_STATE_CHANGED");
        require(waitCombat.indexOf("long combatWaitAfterSequence = windowReadyEventBus.currentSequence();")
                        < waitCombat.indexOf("autoCombatService.handleCombatTick("),
                "WAIT_COMBAT must capture the ready-event sequence before checking combat state");
        require(waitCombat.contains("waitForCombatStateWake(")
                        && waitCombat.contains("combatWaitAfterSequence"),
                "WAIT_COMBAT must pass the pre-check sequence into the combat wait spec");
        require(waitFactory.contains("waitForCombatStateWake(XiuluoStepOutcome outcome, long afterSequence)"),
                "Xiuluo combat wait spec must receive a pre-check sequence");
        require(!waitFactory.contains("windowReadyEventBus.currentSequence()"),
                "Xiuluo combat wait spec must not capture currentSequence after the combat check");
        require(parkBlock.contains("waitSpec.getAfterSequence()"),
                "Xiuluo wait parking must use the pre-captured sequence");

        require(outcome.contains("XiuluoWaitSpec waitSpec"),
                "XiuluoStepOutcome must carry a scheduling-only wait spec");
        require(outcome.contains("withWaitSpec("),
                "XiuluoStepOutcome must expose withWaitSpec for WAIT_COMBAT scheduling");
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
