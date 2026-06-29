package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR120 leader box consume timing in 修罗.
 */
public final class XiuluoCR120CommonBoxEarlyConsumeWiringTest {

    private XiuluoCR120CommonBoxEarlyConsumeWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        String xiuluo = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "task", "xiuluo", "XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String afterAccept = extractMethod(xiuluo,
                "private XiuluoStepOutcome afterAcceptMaintenanceCheck(");

        int earlyConsume = afterAccept.indexOf(
                "consumeCommonBoxDuringNextTaskProgress(context, \"xiuluo-v2:after-accept-maintenance-check\")");
        int dueCheck = afterAccept.indexOf("if (!isHealPetMaintenanceDue() && !isRepairEquipmentMaintenanceDue())");
        int prepath = afterAccept.indexOf("startLeavingStartMapIfPresent(");
        int maintenance = afterAccept.indexOf("taskMaintenanceService.runOpportunisticMaintenance(");

        require(earlyConsume >= 0,
                "CR120 leader pending box must get an early after-accept consume hook before prepath");
        require(dueCheck >= 0 && earlyConsume < dueCheck,
                "CR120 box consume must outrank after-accept maintenance/prepath decisions");
        require(prepath >= 0 && earlyConsume < prepath,
                "CR120 box consume must happen before start-exit-prepath can spend the TTL");
        require(maintenance >= 0 && earlyConsume < maintenance,
                "CR120 box consume must happen before opportunistic maintenance");

        System.out.println("XiuluoCR120CommonBoxEarlyConsumeWiringTest passed");
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
