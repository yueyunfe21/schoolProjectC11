package com.bot.dhxy.model.metrics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * One persisted business metric event.
 *
 * <p>The event is written as one JSON line in {@code logs/automation-metrics.jsonl}. Coordinates,
 * window ids, task phase names, and error codes should be stored in {@link #attributes} unless they
 * are part of the common fields below. Nullable fields may be absent for system-level events that do
 * not have a bound task/window.</p>
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class AutomationMetricEvent {
    /** Persisted schema version for future migrations. */
    @Builder.Default
    String schemaVersion = "1";
    /** ISO-8601 timestamp in system local offset. */
    String timestamp;
    /** Unique id for this single event. */
    String eventId;
    /** Local app-session id generated when the metrics service starts. */
    String sessionId;
    /** Reserved for future per-run correlation; may be null today. */
    String runId;
    /** Reserved for future cloud/customer aggregation; local-only builds keep it null. */
    String customerId;
    /** Reserved for future license/customer aggregation; local-only builds keep it null. */
    String licenseId;
    /** Build/application version if known. */
    String appVersion;
    /** Registered logical window id. */
    String windowId;
    /** Current window role, for example LEADER or MEMBER. */
    String windowRole;
    /** Native HWND handle text when available. */
    String nativeWindowHandle;
    /** Task code such as xiuluo_v2 or auto_battle. */
    String taskCode;
    /** User-facing task name. */
    String taskName;
    /** Task phase or service boundary name. */
    String phase;
    /** Stable event type used for aggregation. */
    AutomationMetricEventType eventType;
    /** Normalized event status. */
    AutomationMetricStatus status;
    /** Boundary elapsed time in milliseconds; null when not measured. */
    Long elapsedMs;
    /** Stable error code/classification, not a full stack trace. */
    String errorCode;
    /** Failure archive id/path key when this event links to a saved case. */
    String caseId;
    /** Short human-readable summary. */
    String message;
    /** Small structured details. Values should be short strings to keep JSONL grep-friendly. */
    @Builder.Default
    Map<String, String> attributes = Map.of();
}
