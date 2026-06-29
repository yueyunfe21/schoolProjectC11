package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR120 leader common-box consume timing in 五倍.
 */
public final class WubeiCR120CommonBoxEarlyConsumeWiringTest {

    private WubeiCR120CommonBoxEarlyConsumeWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        commonBoxPendingIsConsumedBeforeAfterAcceptMaintenance(wubei);
        commonBoxPendingIsConsumedBeforeTrackerPathingMaintenance(wubei);
        helperDoesNotSwallowMaintenanceOrFirstAidFlow(wubei);

        System.out.println("WubeiCR120CommonBoxEarlyConsumeWiringTest passed");
    }

    private static void commonBoxPendingIsConsumedBeforeAfterAcceptMaintenance(String wubei) {
        String afterAccept = methodBody(wubei,
                "private WubeiStepOutcome runAfterAcceptMaintenanceCheck(");

        int earlyConsume = indexOf(afterAccept,
                "consumeCommonBoxAfterTaskAccepted(context, \"wubei:after-accept-maintenance-check\")");
        int healPet = indexOf(afterAccept, "triggerHealPetBroadcastBeforeTracker(context, state)");
        int nextPhase = indexOf(afterAccept, "WubeiPhase.BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK");

        require(earlyConsume < healPet,
                "CR120 五倍 common-box must be consumed before after-accept heal-pet broadcast");
        require(earlyConsume < nextPhase,
                "CR120 五倍 common-box must be consumed before leaving after-accept maintenance phase");
    }

    private static void commonBoxPendingIsConsumedBeforeTrackerPathingMaintenance(String wubei) {
        String beforeTracker = methodBody(wubei,
                "private WubeiStepOutcome runBeforeTrackerPathingMaintenanceCheck(");

        int earlyConsume = indexOf(beforeTracker,
                "consumeCommonBoxAfterTaskAccepted(context, \"wubei:before-tracker-pathing-maintenance-check\")");
        int repair = indexOf(beforeTracker, "triggerRepairEquipmentBroadcastBeforeTracker(context, state)");
        int trackerPathing = indexOf(beforeTracker, "WubeiPhase.TRACKER_PATHING");

        require(earlyConsume < repair,
                "CR120 五倍 common-box must be retried before repair-equipment broadcast");
        require(earlyConsume < trackerPathing,
                "CR120 五倍 common-box must be consumed before tracker pathing can start");
    }

    private static void helperDoesNotSwallowMaintenanceOrFirstAidFlow(String wubei) {
        String helper = methodBody(wubei,
                "private void consumeCommonBoxAfterTaskAccepted(");
        require(helper.contains("commonBoxService.consumePendingBoxIfAllowed(context, TASK_CODE, source);"),
                "CR120 五倍 helper must keep using the shared role/task/run-gated common-box service");

        String trackerClick = methodBody(wubei,
                "private boolean clickTaskTrackerGreen(");
        require(trackerClick.contains(
                        "consumeCommonBoxAfterTaskAccepted(context, \"wubei:tracker-green-click:\" + safeFileToken(label))"),
                "CR120 五倍 tracker-green fallback consume must stay in place");

        String returnHome = methodBody(wubei,
                "private WubeiStepOutcome returnHomeAfterCombatOrContinueSpecialTarget(");
        require(returnHome.contains("openTeamFirstAidMaintenanceWindow("),
                "五倍 chained-combat first-aid broadcast path must remain wired");
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
            throw new AssertionError("Missing source marker: " + needle);
        }
        return index;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
