package com.bot.dhxy.metrics;

import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Focused behavior check for the local automation dashboard's round ledger.
 */
public class AutomationRoundDashboardRenderingTest {

    public static void main(String[] args) throws Exception {
        Path testDir = Files.createTempDirectory("dhxy-dashboard-test");
        AutomationMetricsService service = newTestService(testDir.resolve("explicit"));
        String roundId = "dashboard-fixture-20260626-001";

        try {
            service.recordRoundStarted(null, roundId, 7, "显形镜", "task accepted",
                    Map.of("windowId", "fixture-window-a", "role", "LEADER", "character", "忍者影"));
            service.recordRoundFinished(null, roundId, 7, "显形镜", AutomationMetricStatus.FAILED,
                    "FAILED_REACCEPT", "probe failed and reaccepted", 93_380L,
                    Map.of("slowestStage", "WAIT_ENTER_BATTLE"));

            Path dashboard = service.writeDashboardNow();
            String html = Files.readString(dashboard, StandardCharsets.UTF_8);

            require(html.contains("DHXY 任务统计"), "dashboard must expose the task ledger page");
            require(html.contains("task-ledger"), "dashboard must render the sortable ledger table");
            require(html.contains("sortTable"), "dashboard must provide sortable columns");
            require(html.contains(roundId), "dashboard must render the concrete round id");
            require(html.contains("显形镜"), "dashboard must render round type");
            require(html.contains("93.4s"), "dashboard must render readable round duration");
            require(html.contains("复制定位"), "dashboard must provide a copy-locator action");
            require(html.contains("DHXY_ROUND_REF"), "dashboard locator must include a stable round marker Codex can parse");
            require(!html.contains(">TASK_TRANSACTION<"), "dashboard must not expose raw transaction rows");
            require(!html.contains(">MEMBER<"), "dashboard must hide member-only activity");

            AutomationMetricsService transactionOnlyService = newTestService(testDir.resolve("synthetic"));
            try {
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:00-04:00",
                        "xiuluo-v2:PREPARE_ROUND", 80L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:04-04:00",
                        "xiuluo-v2:ACCEPT_TASK_NAVIGATE_TO_NPC", 4_000L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:11-04:00",
                        "xiuluo-v2:TRY_TRACKER_SHORTCUT", 700L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:12-04:00",
                        "xiuluo-v2:AFTER_ACCEPT_MAINTENANCE_CHECK", 600L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:12.500-04:00",
                        "xiuluo-v2:TEAM_MAINTENANCE_BROADCAST", 610L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:13-04:00",
                        "xiuluo-v2:SUMMON_SKILL_CLEANUP", 650L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:43-04:00",
                        "xiuluo-v2:WAIT_TRACKER_SHORTCUT_PATHING", 430L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:30:56-04:00",
                        "xiuluo-v2:WAIT_COMBAT", 400L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:31:40-04:00",
                        "xiuluo-v2:RETURN_HOME", 6_000L);
                recordLeaderTransaction(transactionOnlyService, "2026-06-26T20:31:41-04:00",
                        "xiuluo-v2:WAIT_TEAM_RETURN", 500L);
                recordMemberTransaction(transactionOnlyService, "2026-06-26T20:31:42-04:00",
                        "auto-battle:HEAL", 300L);

                String syntheticHtml = Files.readString(transactionOnlyService.writeDashboardNow(), StandardCharsets.UTF_8);
                require(syntheticHtml.contains("修罗"), "dashboard must synthesize leader task rounds from transactions");
                require(syntheticHtml.contains("1m 37s"), "synthetic round duration must use whole-round wall time");
                require(syntheticHtml.contains("最长记录"), "dashboard must label recorded phase latency honestly");
                require(syntheticHtml.contains(">P95<"), "dashboard must show P95 as its own column");
                require(syntheticHtml.contains(">样本<"), "dashboard must show sample size as its own column");
                require(syntheticHtml.contains("task-filter"), "dashboard must provide task filters");
                require(syntheticHtml.contains("data-task-filter=\"xiuluo\""), "dashboard must expose Xiuluo filter");
                require(syntheticHtml.contains("data-task-filter=\"wuhuan\""), "dashboard must expose Wuhuan filter");
                require(syntheticHtml.contains("data-task-filter=\"wubei\""), "dashboard must expose Wubei filter");
                require(syntheticHtml.contains("data-time-filter=\"day\""), "dashboard must expose day time filter");
                require(syntheticHtml.contains("data-time-filter=\"yesterday\""), "dashboard must expose yesterday time filter");
                require(syntheticHtml.contains("data-time-filter=\"3d\""), "dashboard must expose 3-day time filter");
                require(syntheticHtml.contains("data-time-filter=\"week\""), "dashboard must expose week time filter");
                require(syntheticHtml.contains("data-time-filter=\"month\""), "dashboard must expose month time filter");
                require(syntheticHtml.contains("task-summary-grid"), "dashboard must render task P95 summary area");
                require(syntheticHtml.contains("updateSummary"), "dashboard must recompute P95 summary for active filters");
                require(syntheticHtml.contains("timeFilterLabel"), "dashboard summary must name the active time window");
                require(syntheticHtml.contains("data-duration="), "dashboard rows must expose whole-round duration for P95 summary");
                require(syntheticHtml.contains("task-badge task-xiuluo"), "dashboard must color task type badges");
                require(syntheticHtml.contains("legend-dot dot-maintenance"), "dashboard must explain maintenance marker");
                require(syntheticHtml.contains("legend-dot dot-summon-skill"), "dashboard must explain summon-skill marker");
                require(syntheticHtml.contains("round-dot dot-maintenance"), "round content must mark maintenance activity");
                require(syntheticHtml.contains("round-dot dot-summon-skill"), "round content must mark summon skill cleanup");
                require(syntheticHtml.contains("P95"), "dashboard must provide a percentile baseline for the recorded phase");
                require(!syntheticHtml.contains("个步骤"), "dashboard must not call metric samples task steps");
                require(!syntheticHtml.contains(">0.4s<"), "small WAIT_COMBAT samples must not become standalone rounds");
                require(!syntheticHtml.contains("auto-battle:HEAL"), "member maintenance details must stay out of the leader ledger");
            } finally {
                transactionOnlyService.stop();
            }

            AutomationMetricsService checkOnlyService = newTestService(testDir.resolve("maintenance-check-only"));
            try {
                recordLeaderTransaction(checkOnlyService, "2026-06-26T20:40:00-04:00",
                        "xiuluo-v2:PREPARE_ROUND", 80L);
                recordLeaderTransaction(checkOnlyService, "2026-06-26T20:40:10-04:00",
                        "xiuluo-v2:AFTER_ACCEPT_MAINTENANCE_CHECK", 600L);
                recordLeaderTransaction(checkOnlyService, "2026-06-26T20:41:30-04:00",
                        "xiuluo-v2:RETURN_HOME", 6_000L);

                String checkOnlyHtml = Files.readString(checkOnlyService.writeDashboardNow(), StandardCharsets.UTF_8);
                require(!checkOnlyHtml.contains("round-dot dot-maintenance"),
                        "a short maintenance check must not mark the round unless maintenance is the meaningful slow stage");
            } finally {
                checkOnlyService.stop();
            }

            AutomationMetricsService explicitPreferredService = newTestService(testDir.resolve("explicit-preferred"));
            try {
                recordExplicitRoundEvent(explicitPreferredService, "2026-06-26T20:50:00-04:00",
                        AutomationMetricEventType.TASK_ROUND_STARTED, "explicit-round-001", null);
                recordLeaderTransaction(explicitPreferredService, "2026-06-26T20:50:00-04:00",
                        "xiuluo-v2:PREPARE_ROUND", 80L);
                recordLeaderTransaction(explicitPreferredService, "2026-06-26T20:50:08-04:00",
                        "xiuluo-v2:TRY_TRACKER_SHORTCUT", 700L);
                recordLeaderTransaction(explicitPreferredService, "2026-06-26T20:51:01-04:00",
                        "xiuluo-v2:RETURN_HOME", 6_000L);
                recordExplicitRoundEvent(explicitPreferredService, "2026-06-26T20:51:02-04:00",
                        AutomationMetricEventType.TASK_ROUND_FINISHED, "explicit-round-001", 62_000L);

                String explicitPreferredHtml = Files.readString(
                        explicitPreferredService.writeDashboardNow(), StandardCharsets.UTF_8);
                require(explicitPreferredHtml.contains("explicit-round-001"),
                        "explicit round should render when matching transaction history exists");
                require(!explicitPreferredHtml.contains("synthetic-fixture-window-b-xiuluo_v2"),
                        "overlapping synthetic round must be hidden when explicit round exists");
            } finally {
                explicitPreferredService.stop();
            }
        } finally {
            service.stop();
        }
    }

    private static AutomationMetricsService newTestService(Path dir) throws Exception {
        Files.createDirectories(dir);
        return new AutomationMetricsService(
                dir.resolve("automation-metrics.jsonl"),
                dir.resolve("automation-dashboard-data.json"),
                dir.resolve("automation-dashboard.html"));
    }

    private static void recordLeaderTransaction(AutomationMetricsService service,
                                                String timestamp,
                                                String phase,
                                                long elapsedMs) {
        service.record(transaction(timestamp, "LEADER", "xiuluo_v2", "修罗", phase, elapsedMs));
    }

    private static void recordMemberTransaction(AutomationMetricsService service,
                                                String timestamp,
                                                String phase,
                                                long elapsedMs) {
        service.record(transaction(timestamp, "MEMBER", "auto_battle", "自动战斗", phase, elapsedMs));
    }

    private static AutomationMetricEvent transaction(String timestamp,
                                                     String role,
                                                     String taskCode,
                                                     String taskName,
                                                     String phase,
                                                     long elapsedMs) {
        return AutomationMetricEvent.builder()
                .timestamp(timestamp)
                .windowId("fixture-window-b")
                .windowRole(role)
                .taskCode(taskCode)
                .taskName(taskName)
                .phase(phase)
                .eventType(AutomationMetricEventType.TASK_TRANSACTION)
                .status(AutomationMetricStatus.SUCCESS)
                .elapsedMs(elapsedMs)
                .message("task transaction finished")
                .build();
    }

    private static void recordExplicitRoundEvent(AutomationMetricsService service,
                                                 String timestamp,
                                                 AutomationMetricEventType eventType,
                                                 String roundId,
                                                 Long elapsedMs) {
        service.record(AutomationMetricEvent.builder()
                .timestamp(timestamp)
                .windowId("fixture-window-b")
                .windowRole("LEADER")
                .taskCode("xiuluo_v2")
                .taskName("修罗")
                .runId(roundId)
                .phase("xiuluo-v2:round")
                .eventType(eventType)
                .status(eventType == AutomationMetricEventType.TASK_ROUND_FINISHED
                        ? AutomationMetricStatus.SUCCESS
                        : AutomationMetricStatus.STARTED)
                .elapsedMs(elapsedMs)
                .message("explicit round fixture")
                .attributes(Map.of("roundId", roundId, "roundNumber", "1", "roundType", "修罗"))
                .build());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
