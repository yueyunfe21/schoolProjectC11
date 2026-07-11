package com.bot.dhxy.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutoBattleCR212IdleBroadcastGateWiringTest {

    public static void main(String[] args) throws Exception {
        runTests(AutoBattleCR212IdleBroadcastGateWiringTest.class);
        System.out.println("AutoBattleCR212IdleBroadcastGateWiringTest passed");
    }

    @Test
    public void junitRunsMainSuite() throws Exception {
        main(new String[0]);
    }

    public void testAutoBattleIdleMaintenanceUsesLocalLeaderBroadcastGateOnlyForBroadcast() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/task/AutoBattleTask.java"));
        int start = source.indexOf("private void maybeRunIdleMaintenance");
        int end = source.indexOf("private static int summonSkillBudgetForRequestedTask", start);
        require(start >= 0 && end > start, "maybeRunIdleMaintenance boundary must be readable");
        String method = source.substring(start, end);

        int decision = method.indexOf("boolean handleIdleMaintenanceBroadcast");
        int gate = method.indexOf("taskMaintenanceService.shouldSuppressIdleMaintenanceBroadcast(context)");
        int builder = method.indexOf(".handleMaintenanceBroadcast(handleIdleMaintenanceBroadcast)");
        int summon = method.indexOf(".cleanSummonSkill(true)");
        require(decision >= 0, "AutoBattleTask must calculate a named CR212 broadcast gate");
        require(gate > decision, "broadcast gate must call TaskMaintenanceService");
        require(builder > gate, "handleMaintenanceBroadcast must use the CR212 gate");
        require(summon > builder, "CR212 gate must affect only broadcast scanning, not summon-skill capability");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void runTests(Class<?> testClass) throws Exception {
        Object test = testClass.getDeclaredConstructor().newInstance();
        for (Method method : testClass.getDeclaredMethods()) {
            if (!method.getName().startsWith("test")) {
                continue;
            }
            try {
                method.invoke(test);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw e;
            }
        }
    }
}
