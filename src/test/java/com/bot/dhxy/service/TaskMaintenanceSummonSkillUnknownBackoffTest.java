package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.model.maintenance.TaskMaintenanceStatus;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.util.Map;

/**
 * Behavior guard for CR94: unknown-class summon-skill failures must not create a 3s retry storm.
 */
public class TaskMaintenanceSummonSkillUnknownBackoffTest {

    public static void main(String[] args) {
        skipsCleanupDuringUnknownBackoff();
        invalidatesTrustedLayoutAfterUnknownFailure();
        System.out.println("TaskMaintenanceSummonSkillUnknownBackoffTest passed");
    }

    private static void skipsCleanupDuringUnknownBackoff() {
        WindowIsolationProperties isolation = new WindowIsolationProperties();
        isolation.setIsolationEnabled(true);
        WindowTaskContextHolder holder = new WindowTaskContextHolder(isolation);

        WindowRuntimeContext runtime = new WindowRuntimeContext("hwnd-cr94", new GameContext());
        holder.bind(runtime);

        BotProperties properties = new BotProperties();
        properties.setSummonSkillCleanEnabled(true);
        properties.setSummonSkillCleanIntervalMs(20 * 60 * 1000L);
        UnknownThenSuccessSummonSkillService summonSkillService = new UnknownThenSuccessSummonSkillService();
        TaskMaintenanceService maintenanceService = new TaskMaintenanceService(
                properties,
                runtime.getGameContext(),
                null,
                summonSkillService,
                holder);

        TaskExecutionContext context = TaskExecutionContext.builder()
                .windowId("hwnd-cr94")
                .windowRole("MEMBER")
                .build();
        TaskMaintenanceRequest request = TaskMaintenanceRequest.builder()
                .sourceTask("cr94-test")
                .handleMaintenanceBroadcast(false)
                .cleanSummonSkill(true)
                .requireFreeStateForSummonSkill(false)
                .build();

        TaskMaintenanceResult first = maintenanceService.runOpportunisticMaintenance(context, request);
        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_FAILED_RETRY_LATER, first.getStatus(), "first status");
        assertEquals(1, summonSkillService.calls, "first cleanup call count");

        TaskMaintenanceResult second = maintenanceService.runOpportunisticMaintenance(context, request);
        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_DEFERRED, second.getStatus(), "backoff status");
        assertContains(second.getMessage(), "unknown retry backoff", "backoff message");
        assertEquals(1, summonSkillService.calls, "backoff must not call summon skill service again");

        holder.clear();
    }

    private static void invalidatesTrustedLayoutAfterUnknownFailure() {
        WindowIsolationProperties isolation = new WindowIsolationProperties();
        isolation.setIsolationEnabled(true);
        WindowTaskContextHolder holder = new WindowTaskContextHolder(isolation);

        WindowRuntimeContext runtime = new WindowRuntimeContext("hwnd-cr94-layout", new GameContext());
        holder.bind(runtime);

        BotProperties properties = new BotProperties();
        properties.setSummonSkillCleanEnabled(true);
        properties.setSummonSkillCleanIntervalMs(10L);
        properties.setSummonSkillUnknownFailureRetryAfterMs(10L);
        LayoutInvalidationSummonSkillService summonSkillService = new LayoutInvalidationSummonSkillService();
        TaskMaintenanceService maintenanceService = new TaskMaintenanceService(
                properties,
                runtime.getGameContext(),
                null,
                summonSkillService,
                holder);

        TaskExecutionContext context = TaskExecutionContext.builder()
                .windowId("hwnd-cr94-layout")
                .windowRole("MEMBER")
                .build();
        TaskMaintenanceRequest request = TaskMaintenanceRequest.builder()
                .sourceTask("cr94-layout-test")
                .handleMaintenanceBroadcast(false)
                .cleanSummonSkill(true)
                .requireFreeStateForSummonSkill(false)
                .build();

        TaskMaintenanceResult first = maintenanceService.runOpportunisticMaintenance(context, request);
        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_CLEANED, first.getStatus(), "seed success status");
        assertEquals(false, summonSkillService.requests[0].isTrustExpectedSkillCount(), "initial request trust");

        sleep(25L);
        TaskMaintenanceResult second = maintenanceService.runOpportunisticMaintenance(context, request);
        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_FAILED_RETRY_LATER, second.getStatus(), "unknown status");
        assertEquals(true, summonSkillService.requests[1].isTrustExpectedSkillCount(),
                "pre-unknown request should reuse trusted success cache");
        assertEquals(Integer.valueOf(2), summonSkillService.requests[1].getStartSlotIndex(),
                "pre-unknown cached start slot");

        sleep(25L);
        TaskMaintenanceResult third = maintenanceService.runOpportunisticMaintenance(context, request);
        assertEquals(TaskMaintenanceStatus.SUMMON_SKILL_CLEANED, third.getStatus(), "post-backoff success status");
        assertEquals(false, summonSkillService.requests[2].isTrustExpectedSkillCount(),
                "post-unknown request must force full skill-count detection");
        assertEquals(null, summonSkillService.requests[2].getStartSlotIndex(),
                "post-unknown request must not reuse cached start slot");

        holder.clear();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("sleep interrupted", e);
        }
    }

    private static void assertContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError(label + " expected substring=" + expected + " actual=" + text);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static class UnknownThenSuccessSummonSkillService extends SummonSkillService {
        private int calls;

        private UnknownThenSuccessSummonSkillService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public SummonSkillCleanupResult cleanSummonSkillsOnce(SummonSkillCleanupRequest request) {
            calls++;
            if (calls == 1) {
                return SummonSkillCleanupResult.builder()
                        .success(false)
                        .skillCount(6)
                        .nextStartIndex(4)
                        .observedStatusesByIndex(Map.of(4, SummonSkillSlotStatus.UNKNOWN))
                        .message("slot status unknown")
                        .build();
            }
            return SummonSkillCleanupResult.builder()
                    .success(true)
                    .skillCount(8)
                    .nextStartIndex(8)
                    .message("test success")
                    .build();
        }
    }

    private static class LayoutInvalidationSummonSkillService extends SummonSkillService {
        private int calls;
        private final SummonSkillCleanupRequest[] requests = new SummonSkillCleanupRequest[3];

        private LayoutInvalidationSummonSkillService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public SummonSkillCleanupResult cleanSummonSkillsOnce(SummonSkillCleanupRequest request) {
            requests[calls] = request;
            calls++;
            if (calls == 1) {
                return SummonSkillCleanupResult.builder()
                        .success(true)
                        .skillCount(6)
                        .nextStartIndex(2)
                        .observedStatusesByIndex(Map.of(2, SummonSkillSlotStatus.EMPTY_SLOT))
                        .message("seed success")
                        .build();
            }
            if (calls == 2) {
                return SummonSkillCleanupResult.builder()
                        .success(false)
                        .skillCount(6)
                        .nextStartIndex(3)
                        .observedStatusesByIndex(Map.of(3, SummonSkillSlotStatus.UNKNOWN))
                        .message("slot status unknown")
                        .build();
            }
            return SummonSkillCleanupResult.builder()
                    .success(true)
                    .skillCount(8)
                    .nextStartIndex(8)
                    .message("post backoff success")
                    .build();
        }
    }
}
