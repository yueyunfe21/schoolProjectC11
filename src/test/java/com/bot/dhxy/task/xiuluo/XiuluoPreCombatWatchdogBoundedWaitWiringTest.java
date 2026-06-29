package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR116 修罗 pre-combat watchdog wait bounds.
 *
 * <p>The 180s watchdog is only useful if every pre-combat park/future wait is capped by the
 * remaining budget. This guard intentionally checks the wiring shape because the bug was an
 * indefinite event/future wait, not a detector threshold issue.</p>
 */
public class XiuluoPreCombatWatchdogBoundedWaitWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertEventParksUseRemainingPreCombatBudget(task);
        assertBackgroundFutureWaitsUseRemainingPreCombatBudget(task);
    }

    private static void assertEventParksUseRemainingPreCombatBudget(String task) {
        String park = between(task,
                "private XiuluoStepOutcome parkAfterYieldIfNeeded(",
                "private EnumSet<WindowReadyEventType> toWakeTypeEnumSet(");
        require(park.contains("boundedPreCombatWaitTimeoutMs(outcome.nextState(), waitSpec.getTimeoutMs(),"),
                "event park must cap the wait timeout with the remaining pre-combat watchdog budget");
        require(park.contains("preCombatWatchdogTimeoutOutcome(outcome.nextState(), waitSpec.getReason().name(),"),
                "event park must return a normal FAILED outcome when the watchdog budget is exhausted");
        require(park.contains("boundedTimeoutMs"),
                "event park diagnostics must log the bounded timeout instead of the raw wait spec timeout only");
        require(park.contains("awaitNewerPathingTerminalOrPreparedRoute(")
                        && park.contains("boundedTimeoutMs)"),
                "pathing event wait must pass the bounded timeout to WindowReadyEventBus");
        require(park.contains("windowReadyEventBus.awaitNewer(")
                        && park.contains("boundedTimeoutMs)"),
                "generic event wait must pass the bounded timeout to WindowReadyEventBus");
    }

    private static void assertBackgroundFutureWaitsUseRemainingPreCombatBudget(String task) {
        String acceptFuture = between(task,
                "private TaskTrackerPanelReadResult waitForAcceptTrackerPanelResult(",
                "private String resolveReadyShortcutObjectiveTargetMap(");
        require(acceptFuture.contains("remainingPreCombatWatchdogBudgetMs(waitState,"),
                "accept-time tracker future wait must observe the remaining pre-combat watchdog budget");
        require(acceptFuture.contains("accept-tracker-parse"),
                "accept-time tracker future timeout log must identify the parse wait context");
        require(acceptFuture.contains("TaskTrackerPanelReadResult.empty()"),
                "accept-time tracker future timeout must fall back through existing shortcut failure logic");

        String objectiveFuture = between(task,
                "private Optional<NpcTarget> waitForBackgroundObjectiveResult(",
                "private String currentWindowLabel()");
        require(objectiveFuture.contains("remainingPreCombatWatchdogBudgetMs(waitState,"),
                "background objective future wait must observe the remaining pre-combat watchdog budget");
        require(objectiveFuture.contains("objective-parse"),
                "background objective future timeout log must identify the parse wait context");
        require(objectiveFuture.contains("return Optional.empty();"),
                "background objective future timeout must fall back through existing objective recovery logic");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
