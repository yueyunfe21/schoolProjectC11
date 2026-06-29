package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR119 修罗 leader 三技能 pathing maintenance before event park.
 */
public class XiuluoLeaderPathingSummonBeforeParkWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        String yield = between(task,
                "private XiuluoStepOutcome yieldAfterMustYield(",
                "    /**\n     * Park a 修罗 phase");
        require(yield.contains("maybeRunLeaderPathingSummonMaintenanceBeforePark(context, outcome)"),
                "CR119: 修罗 event-wait yield must check leader 三技能 before parking");
        require(yield.indexOf("maybeRunLeaderPathingSummonMaintenanceBeforePark(context, outcome)")
                        < yield.indexOf("return parkAfterYieldIfNeeded(context, outcome)"),
                "CR119: leader 三技能 maintenance must run before the event wait parks");

        String beforePark = between(task,
                "private XiuluoStepOutcome maybeRunLeaderPathingSummonMaintenanceBeforePark(",
                "private XiuluoStepOutcome parkAfterYieldIfNeeded(");
        require(beforePark.contains("outcome.transactionResult() != TaskTransactionResult.PATHING_STARTED"),
                "CR119: before-park maintenance must be limited to pathing-started outcomes");
        require(beforePark.contains("reason != XiuluoWaitReason.WAIT_TRACKER_SHORTCUT_PATHING"),
                "CR119: tracker shortcut pathing must be eligible for before-park leader maintenance");
        require(beforePark.contains("reason != XiuluoWaitReason.WAIT_TARGET_PATHING_TERMINAL"),
                "CR119: target pathing waits must be eligible for before-park leader maintenance");
        require(beforePark.contains("runLeaderPathingSummonSkillMaintenance("),
                "CR119: before-park hook must reuse the existing leader pathing summon maintenance path");
        require(beforePark.contains("\"before-park\""),
                "CR119: before-park maintenance needs a distinct log/source suffix for runtime validation");

        String leaderMaintenance = between(task,
                "private XiuluoStepOutcome runLeaderPathingSummonSkillMaintenance(",
                "private boolean isMemberWindow(");
        require(leaderMaintenance.contains("requireOpenTeamMaintenanceWindow(true)"),
                "CR119: leader pathing 三技能 must still require the team pathing maintenance window");
        require(leaderMaintenance.contains("oneSummonSkillPerTeamRound(true)"),
                "CR119: leader pathing 三技能 must preserve one-cleaner-per-round safety");
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
