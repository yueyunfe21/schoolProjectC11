package com.bot.dhxy.cloud.turn.protocol;

import java.util.Map;

/**
 * TURN-40B-C1: one exact typed metric event carried by a {@code METRIC_*} LOCAL_SERVICE call.
 *
 * <p>The payload transports every persisted identity fact the DHXY metrics authority consumes
 * ({@code taskCode/taskName/windowId/windowRole/nativeWindowHandle}) plus the exact per-operation
 * event, round and failure-case fields, so the local executor can reconstruct one
 * {@code AutomationMetricEvent} without any second context, synthetic identity, or store. The
 * {@code caseDir} value is the verbatim Cloud filesystem locator string and is never rewritten to
 * a DHXY-local path. Fields not used by an operation stay null; the validator enforces the exact
 * per-operation shape. Metrics remain diagnostics only and never become business truth.</p>
 */
public record TurnMetricEventPayload(
        String taskCode,
        String taskName,
        String windowId,
        String windowRole,
        String nativeWindowHandle,
        String roundId,
        Integer roundNumber,
        String roundType,
        String status,
        String resultCode,
        String message,
        Long elapsedMs,
        String caseDir,
        String reason,
        String phase,
        Integer round,
        Map<String, String> attributes) {
}
