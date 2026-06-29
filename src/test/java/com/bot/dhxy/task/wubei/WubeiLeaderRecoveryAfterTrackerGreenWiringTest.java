package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for leader post-combat recovery timing in 五倍.
 */
public final class WubeiLeaderRecoveryAfterTrackerGreenWiringTest {

    private WubeiLeaderRecoveryAfterTrackerGreenWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        leaderRecoveryIsNotConsumedImmediatelyAfterAccept(wubei);
        leaderRecoveryRunsAfterTrackerGreenPathingIsRegistered(wubei);

        System.out.println("WubeiLeaderRecoveryAfterTrackerGreenWiringTest passed");
    }

    private static void leaderRecoveryIsNotConsumedImmediatelyAfterAccept(String wubei) {
        String afterAccept = methodBody(wubei,
                "private WubeiStepOutcome afterAcceptTaskSucceeded(");

        require(!afterAccept.contains("consumePendingLeaderPostCombatRecoveryIfAllowed"),
                "五倍 leader post-combat recovery must not run before tracker-green movement starts");
    }

    private static void leaderRecoveryRunsAfterTrackerGreenPathingIsRegistered(String wubei) {
        String trackerClick = methodBody(wubei,
                "private boolean clickTaskTrackerGreen(");

        int registerIntent = indexOf(trackerClick, "registerTrackerPathingIntent(intentSource)");
        int consumeCommonBox = indexOf(trackerClick,
                "consumeCommonBoxAfterTaskAccepted(context, \"wubei:tracker-green-click:\" + safeFileToken(label))");
        int consumeRecovery = indexOf(trackerClick,
                "autoCombatService.consumePendingLeaderPostCombatRecoveryIfAllowed(");
        int enterBattleGate = indexOf(trackerClick, "startOrdinaryEnterBattleTargetMapGateIfNeeded(");

        require(registerIntent < consumeRecovery,
                "五倍 leader post-combat recovery must run only after tracker-green pathing intent is registered");
        require(consumeCommonBox < consumeRecovery,
                "五倍 common-box must stay ahead of sheyaoxiang/post-combat recovery");
        require(consumeRecovery < enterBattleGate,
                "五倍 leader post-combat recovery should finish before arming ordinary enter-battle gate");
        require(trackerClick.contains("wubei:tracker-green-click:\" + safeFileToken(label)"),
                "五倍 leader post-combat recovery source should identify the tracker-green click");
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing method signature: " + signature);
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

    private static int indexOf(String source, String needle) {
        int index = source.indexOf(needle);
        if (index < 0) {
            throw new AssertionError("Missing expected snippet: " + needle);
        }
        return index;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
