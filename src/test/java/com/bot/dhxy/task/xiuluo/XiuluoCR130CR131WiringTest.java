package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR130/CR131 修罗 round-start and team-return precheck wiring.
 */
public final class XiuluoCR130CR131WiringTest {

    private XiuluoCR130CR131WiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = read(root, "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");
        String teamReturn = read(root, "src/main/java/com/bot/dhxy/service/TeamReturnService.java");

        continuousRoundsSkipHotStartInspection(xiuluo);
        startupResumePathStaysExplicit(xiuluo);
        returnHomeSchedulesPrecheckBeforeBag(xiuluo);
        waitTeamReturnConsumesPrecheckBeforeLiveProbe(xiuluo);
        teamReturnPrecheckIsReadOnlyAndScoped(teamReturn);

        System.out.println("XiuluoCR130CR131WiringTest passed");
    }

    private static void continuousRoundsSkipHotStartInspection(String xiuluo) {
        String execute = extractMethod(xiuluo,
                "public TaskRunResult execute(TaskExecutionContext executionContext)");
        require(!execute.contains("hotStartResolver.resolve("),
                "CR130 continuous round loop must not call XiuluoHotStartResolver");
        require(execute.contains("completedRuns == 0"),
                "CR130 only true startup should run startup-screen resume logic");
        require(execute.contains(": XiuluoRoundContext.start(round)"),
                "CR130 later continuous rounds must start from normal PREPARE_ROUND state");
    }

    private static void startupResumePathStaysExplicit(String xiuluo) {
        String execute = extractMethod(xiuluo,
                "public TaskRunResult execute(TaskExecutionContext executionContext)");
        require(execute.contains("\"startup-screen-resume\""),
                "CR130 true UI startup path must be logged as startup-screen-resume");
        require(execute.contains("\"after-combat-exit-startup-screen-resume\""),
                "CR130 after-combat startup recovery must keep explicit startup-screen resume label");
    }

    private static void returnHomeSchedulesPrecheckBeforeBag(String xiuluo) {
        String useReturnItem = extractMethod(xiuluo,
                "private boolean useReturnItemAndVerifyStartMap(");
        int precheck = useReturnItem.indexOf("pendingTeamReturnPrecheck = teamReturnService.beginLeaderSignalPrecheck(");
        int bag = useReturnItem.indexOf("bagService.findAndUseMainBagTaskPageItem(");
        require(precheck >= 0, "CR131 return-home must schedule team-return precheck");
        require(bag >= 0, "CR131 guard could not find return item bag call");
        require(precheck < bag, "CR131 precheck must be captured before opening/using the bag item");
    }

    private static void waitTeamReturnConsumesPrecheckBeforeLiveProbe(String xiuluo) {
        String waitTeamReturn = extractMethod(xiuluo,
                "private XiuluoStepOutcome waitTeamReturn(");
        int consume = waitTeamReturn.indexOf("teamReturnService.consumeLeaderSignalPrecheck(");
        int liveProbe = waitTeamReturn.indexOf("shouldYieldForTeamReturnSignal()");
        require(consume >= 0, "CR131 WAIT_TEAM_RETURN must consume the precomputed result");
        require(liveProbe >= 0, "CR131 guard could not find live team-return fallback");
        require(consume < liveProbe, "CR131 precheck must be consumed before live fallback detection");
    }

    private static void teamReturnPrecheckIsReadOnlyAndScoped(String teamReturn) {
        String begin = extractMethod(teamReturn,
                "public LeaderSignalPrecheck beginLeaderSignalPrecheck(");
        String consume = extractMethod(teamReturn,
                "public LeaderSignalPrecheckStatus consumeLeaderSignalPrecheck(");
        require(begin.contains("tracker.captureToMemory("),
                "CR131 precheck must capture the bound window ROI to memory");
        require(begin.contains("CompletableFuture.supplyAsync("),
                "CR131 precheck must analyze in the background while bag/return continues");
        require(consume.contains("!precheck.scope().matches(context)"),
                "CR131 precheck consume must reject stale window/task-run handles");
        require(consume.contains("!precheck.future().isDone()"),
                "CR131 not-ready precheck must fall back instead of blocking");
        require(!begin.contains("inputSequences") && !consume.contains("inputSequences"),
                "CR131 precheck capture/consume must be read-only and never send input");
    }

    private static String read(Path root, String relativePath) throws Exception {
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Method signature not found: " + signature);
        }
        int brace = source.indexOf('{', start);
        if (brace < 0) {
            throw new AssertionError("Method body not found: " + signature);
        }
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        throw new AssertionError("Method body not closed: " + signature);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
