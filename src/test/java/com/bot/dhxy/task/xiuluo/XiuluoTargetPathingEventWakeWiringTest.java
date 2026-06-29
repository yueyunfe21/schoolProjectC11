package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR68 Xiuluo target-navigation scheduling.
 *
 * <p>Target navigation may legitimately remain ACTIVE for a long walk. The task must release the
 * turn and wait for the runner's PATHING_TERMINAL event instead of reacquiring every fixed 900ms
 * while the same pathing intent is still active.</p>
 */
public class XiuluoTargetPathingEventWakeWiringTest {

    public static void main(String[] args) throws Exception {
        XiuluoWaitReason reason = XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL;
        require(reason.name().equals("WAIT_TARGET_PATHING_TERMINAL"),
                "CR68 wait reason must exist for target-navigation pathing");

        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String waitSpec = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoWaitSpec.java"), StandardCharsets.UTF_8);
        String eventBus = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/runtime/WindowReadyEventBus.java"), StandardCharsets.UTF_8);

        String navigateToTarget = between(task,
                "private XiuluoStepOutcome navigateToTarget(",
                "private XiuluoStepOutcome clickTargetNpc(");
        require(navigateToTarget.contains("waitForTargetPathingWake(outcome)"),
                "NAVIGATE_TO_TARGET PATHING_STARTED must attach an event wait spec");
        require(navigateToTarget.indexOf("navigationOutcome(activeState, result, XiuluoPhase.CLICK_TARGET_NPC")
                        < navigateToTarget.indexOf("waitForTargetPathingWake(outcome)"),
                "target pathing wait must preserve the existing navigationOutcome phase bridge");

        String waitFactory = between(task,
                "private XiuluoStepOutcome waitForTargetPathingWake(",
                "private XiuluoStepOutcome waitForCombatStateWake(");
        require(waitFactory.contains("XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL"),
                "target pathing wait spec must identify the CR68 scheduling reason");
        require(waitFactory.contains("WindowReadyEventType.PATHING_TERMINAL"),
                "target pathing wait spec must wait for PATHING_TERMINAL");
        require(waitFactory.contains("WindowReadyEventType.PREPARED_ACTION_READY"),
                "target pathing wait spec must also wake for a prepared route dialog");
        require(waitFactory.contains("windowReadyEventBus.currentSequence()"),
                "target pathing wait must capture the ready-event sequence before releasing the turn");
        require(waitFactory.contains(".timeoutMs(WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS)")
                        && task.contains("WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS = -1L"),
                "target pathing wait must not reacquire only because a fixed 30s timeout elapsed");
        require(waitFactory.contains(".pathingIntentId("),
                "target pathing wait must capture the active pathing intent id");
        require(waitFactory.contains(".pathingSourcePrefix(\"xiuluo-v2:target\")"),
                "target pathing wait must record the Xiuluo target source prefix");
        require(waitFactory.contains(".pathingTargetMapName("),
                "target pathing wait must record the target map for diagnostics/fallback filtering");

        String yieldBlock = between(task,
                "private XiuluoStepOutcome yieldAfterMustYield(",
                "private long handoffDelayMs(");
        require(yieldBlock.contains("return parkAfterYieldIfNeeded(context, outcome)"),
                "MUST_YIELD path must park when a wait spec is present");
        require(yieldBlock.indexOf("return parkAfterYieldIfNeeded(context, outcome)")
                        < yieldBlock.indexOf("long delayMs = handoffDelayMs(outcome);"),
                "event-wait path must skip the fixed 900ms handoff sleep");

        String parkBlock = between(task,
                "private XiuluoStepOutcome parkAfterYieldIfNeeded(",
                "private long handoffDelayMs(");
        require(parkBlock.contains("awaitNewerPathingTerminalOrPreparedRoute("),
                "target pathing wait must wake on either intent-filtered PATHING_TERMINAL or matching prepared route dialog");
        require(parkBlock.contains("waitSpec.getPathingIntentId()"),
                "park path must pass the expected intent id to the event bus");
        require(parkBlock.contains("waitSpec.getPathingTargetMapName()"),
                "park path must pass the expected target map to match prepared route dialogs");

        String continuePathing = between(task,
                "private XiuluoStepOutcome continueIfNavigationStillPathing(",
                "private XiuluoStepOutcome runLeaderPathingSummonSkillMaintenance(");
        require(continuePathing.contains("state.phase() == XiuluoPhase.NAVIGATE_TO_TARGET"),
                "target pathing re-yield must be scoped to NAVIGATE_TO_TARGET");
        require(continuePathing.contains("return waitForTargetPathingWake(XiuluoStepOutcome.pathingStarted("),
                "same target pathing still active must reattach the CR68 wait spec instead of falling back to 900ms");

        require(waitSpec.contains("String pathingIntentId")
                        && waitSpec.contains("String pathingSourcePrefix")
                        && waitSpec.contains("String pathingTargetMapName"),
                "XiuluoWaitSpec must carry target pathing identity for CR68");

        String busWait = between(eventBus,
                "public Optional<WindowReadyEvent> awaitNewerPathingTerminalOrPreparedRoute(",
                "private Optional<WindowReadyEvent> findNewer(");
        require(busWait.contains("expectedIntentId"),
                "WindowReadyEventBus pathing wait must accept expected intent id");
        require(busWait.contains("getPathingIntent().getIntentId()"),
                "WindowReadyEventBus pathing wait must compare terminal event intent id");
        require(busWait.contains("DialogOperation.ROUTE_TRANSFER"),
                "WindowReadyEventBus pathing wait must match only prepared route-transfer dialogs");
        require(busWait.contains("expectedTargetMapName"),
                "WindowReadyEventBus pathing wait must use the target map when matching prepared route dialogs");
        require(busWait.contains("WindowReadyEventType.PREPARED_ACTION_READY"),
                "WindowReadyEventBus pathing wait must inspect prepared-action events");
        require(busWait.contains("WindowPathingState.ARRIVED")
                        && busWait.contains("WindowPathingState.STOPPED_AWAY"),
                "WindowReadyEventBus pathing wait must only accept terminal pathing states");
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
