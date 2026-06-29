package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

/**
 * Behavior test for CR95: player drift must invalidate summon-skill cooldown before not-due checks.
 */
public class TaskMaintenanceSummonSkillEpochCooldownTest {

    public static void main(String[] args) {
        WindowIsolationProperties isolation = new WindowIsolationProperties();
        isolation.setIsolationEnabled(true);
        WindowTaskContextHolder holder = new WindowTaskContextHolder(isolation);

        WindowRuntimeContext runtime = new WindowRuntimeContext("hwnd-E850B6A", new GameContext());
        runtime.setNativeBinding(binding("大话西游2经典版 - 江山如画 - 忆叶知秋（ID：451753529）"));
        holder.bind(runtime);

        BotProperties properties = new BotProperties();
        properties.setSummonSkillCleanEnabled(true);
        properties.setSummonSkillCleanIntervalMs(20 * 60 * 1000L);
        CountingSummonSkillService summonSkillService = new CountingSummonSkillService();
        TaskMaintenanceService maintenanceService = new TaskMaintenanceService(
                properties,
                runtime.getGameContext(),
                null,
                summonSkillService,
                holder);

        TaskExecutionContext context = TaskExecutionContext.builder()
                .windowId("hwnd-E850B6A")
                .windowRole("MEMBER")
                .build();
        TaskMaintenanceRequest request = TaskMaintenanceRequest.builder()
                .sourceTask("cr95-test")
                .handleMaintenanceBroadcast(false)
                .cleanSummonSkill(true)
                .requireFreeStateForSummonSkill(false)
                .build();

        TaskMaintenanceResult first = maintenanceService.runOpportunisticMaintenance(context, request);
        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_CLEANED, first.getStatus(), "first status");
        assertEquals(1, summonSkillService.calls, "first cleanup call count");

        runtime.setNativeBinding(binding("大话西游2经典版 - 江山如画 - うprinoe大叔（ID：316365558）"));
        TaskMaintenanceResult second = maintenanceService.runOpportunisticMaintenance(context, request);

        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_CLEANED, second.getStatus(),
                "new player must not inherit old summon-skill cooldown");
        assertEquals(2, summonSkillService.calls, "second cleanup call count");

        holder.clear();
        System.out.println("TaskMaintenanceSummonSkillEpochCooldownTest passed");
    }

    private static WindowNativeBinding binding(String title) {
        return new WindowNativeBinding("243600234", title, "xy2", 123L, 10, 20, 1024, 768);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static class CountingSummonSkillService extends SummonSkillService {
        private int calls;

        private CountingSummonSkillService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public SummonSkillCleanupResult cleanSummonSkillsOnce(SummonSkillCleanupRequest request) {
            calls++;
            return SummonSkillCleanupResult.builder()
                    .success(true)
                    .skillCount(8)
                    .nextStartIndex(8)
                    .message("test success")
                    .build();
        }
    }
}
