package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR134 五倍 post-accept prepath target selection.
 *
 * <p>Live validation still needs a real 五倍 round. This guard protects the narrow source
 * contract: after accepting a task, the first current-map mini-map target is 宝象国出口 by default,
 * but 医宝宝 due replaces that first target directly with the 医宝宝 NPC coordinate. Repair-only
 * must not replace the first prepath target because repair goes to 洛阳 later.</p>
 */
public final class WubeiCR134PostAcceptPrepathTargetWiringTest {

    private WubeiCR134PostAcceptPrepathTargetWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        constantsDefineBaoxiangExit(wubei);
        acceptPhaseStartsPrepathAfterAccept(wubei);
        prepathTargetSelectionUsesHealPetOnly(wubei);
        prepathOpensWithAltCThenNavigateInCurrentMap(wubei);

        System.out.println("WubeiCR134PostAcceptPrepathTargetWiringTest passed");
    }

    private static void constantsDefineBaoxiangExit(String wubei) {
        require(wubei.contains("START_EXIT_X = 88"),
                "CR134 must define 宝象国出口 X=88");
        require(wubei.contains("START_EXIT_Y = 157"),
                "CR134 must define 宝象国出口 Y=157");
    }

    private static void acceptPhaseStartsPrepathAfterAccept(String wubei) {
        String accept = methodBody(wubei, "private WubeiStepOutcome runAcceptTaskPhase(");
        String shared = methodBody(wubei, "private WubeiStepOutcome afterAcceptTaskSucceeded(");
        int accepted = indexOf(accept, "boolean accepted = OPTION_ACCEPT_TASK.equals(result.getActionKey())");
        int delegate = indexOf(accept, "return afterAcceptTaskSucceeded(");
        int schedule = indexOf(shared, "postAcceptTrackerPanelFuture = schedulePostAcceptTrackerPanelRead(state)");
        int prepath = indexOf(shared, "startPostAcceptPrepath(context, state)");
        int readTracker = indexOf(shared, "WubeiPhase.READ_TRACKER");

        require(accepted < delegate,
                "CR134 accept phase must delegate only after accept option is confirmed");
        require(schedule < prepath,
                "CR134 must schedule background tracker read before prepath so screenshot timing is not blocked");
        require(prepath < readTracker,
                "CR134 prepath must run before entering READ_TRACKER");
    }

    private static void prepathTargetSelectionUsesHealPetOnly(String wubei) {
        String selector = methodBody(wubei, "private WubeiPrepathTarget computePostAcceptPrepathTarget(");
        require(selector.contains("isHealPetMaintenanceDue()"),
                "CR134 selector must test heal-pet due before choosing first prepath target");
        require(selector.contains("HEAL_PET_NPC"),
                "CR134 heal-pet due must select the heal-pet NPC coordinate");
        require(selector.contains("START_EXIT_X")
                        && selector.contains("START_EXIT_Y")
                        && selector.contains("宝象国出口"),
                "CR134 default/repair-only target must remain 宝象国出口 (88,157)");
        require(!selector.contains("isRepairEquipmentMaintenanceDue()"),
                "CR134 repair-only must not change the first prepath target away from 宝象国出口");
    }

    private static void prepathOpensWithAltCThenNavigateInCurrentMap(String wubei) {
        String prepath = methodBody(wubei, "private void startPostAcceptPrepath(");
        int altC = indexOf(prepath, "InputAction.pressAltC()");
        int navigate = indexOf(prepath, "navigationService.navigateInCurrentMap");
        int targetX = indexOf(prepath, ".targetX(target.x())");
        int targetY = indexOf(prepath, ".targetY(target.y())");

        require(altC < navigate,
                "CR134 must press Alt+C before opening/clicking the current-map prepath target");
        require(targetX < navigate && targetY < navigate,
                "CR134 navigation request must use the computed target coordinate");
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
