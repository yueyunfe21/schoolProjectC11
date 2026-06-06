package com.bot.dhxy.metrics;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.transaction.TaskTransactionOutcome;
import com.bot.dhxy.task.transaction.TaskTransactionResult;
import com.bot.dhxy.task.transaction.TaskYieldPolicy;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * B-lite local business metrics sink.
 *
 * <p>The service accepts low-frequency business boundary events, writes immutable raw events to
 * {@code logs/automation-metrics.jsonl}, and keeps a small aggregate snapshot for
 * {@code logs/automation-dashboard.html}. It deliberately avoids per-frame/pixel sampling so the
 * dashboard stays useful during long multi-window runs without becoming another performance source.</p>
 */
@Slf4j
@Service
public class AutomationMetricsService {

    private static final String SCHEMA_VERSION = "1";
    private static final String APP_VERSION = "1.0-SNAPSHOT";
    private static final int QUEUE_CAPACITY = 5000;
    private static final int RECENT_EVENT_LIMIT = 200;
    private static final long DASHBOARD_WRITE_INTERVAL_MS = 1000L;
    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final BlockingQueue<AutomationMetricEvent> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final String sessionId = UUID.randomUUID().toString();
    private final Path eventLogPath = Path.of("logs", "automation-metrics.jsonl").toAbsolutePath().normalize();
    private final Path snapshotPath = Path.of("logs", "automation-dashboard-data.json").toAbsolutePath().normalize();
    private final Path dashboardPath = Path.of("logs", "automation-dashboard.html").toAbsolutePath().normalize();
    private final AtomicLong acceptedEvents = new AtomicLong();
    private final AtomicLong droppedEvents = new AtomicLong();
    private final AtomicLong lastDashboardWriteMillis = new AtomicLong();
    private final ConcurrentMap<String, LongAdder> countersByEventType = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> countersByStatus = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failuresByTask = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failuresByPhase = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failuresByErrorCode = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LatencyAggregate> latencyByEventType = new ConcurrentHashMap<>();
    private final Deque<AutomationMetricEvent> recentEvents = new ArrayDeque<>();
    private final Object recentEventsLock = new Object();

    private volatile boolean running;
    private Thread writerThread;

    @PostConstruct
    public void start() {
        running = true;
        writerThread = new Thread(this::writerLoop, "dhxy-automation-metrics-writer");
        writerThread.setDaemon(true);
        writerThread.start();
        writeDashboardThrottled();
    }

    @PreDestroy
    public void stop() {
        running = false;
        Thread thread = writerThread;
        if (thread != null) {
            thread.interrupt();
        }
        drainQueueOnce();
        writeDashboard();
    }

    /**
     * Record one already-built event.
     *
     * @param event event data. Missing ids/timestamps/session fields are filled by this service.
     */
    public void record(AutomationMetricEvent event) {
        if (event == null) {
            return;
        }
        AutomationMetricEvent completed = completeEvent(event);
        updateAggregate(completed);
        if (!eventQueue.offer(completed)) {
            long dropped = droppedEvents.incrementAndGet();
            log.warn("automation metrics queue full; drop event: type={} status={} dropped={}",
                    completed.getEventType(), completed.getStatus(), dropped);
            return;
        }
        writeDashboardThrottled();
    }

    /**
     * Record the start of a concrete window task.
     *
     * @param context bound task/window context.
     */
    public void recordTaskStarted(TaskExecutionContext context) {
        record(baseEvent(context)
                .eventType(AutomationMetricEventType.TASK_STARTED)
                .status(AutomationMetricStatus.STARTED)
                .phase("task")
                .message("task started")
                .build());
    }

    /**
     * Record the end of a concrete window task.
     *
     * @param context bound task/window context.
     * @param taskType resolved task type.
     * @param result task result returned to the window runner.
     * @param message short finish message.
     * @param elapsedMs task elapsed time in milliseconds.
     * @param errorCode nullable error code/class when the task failed by exception.
     */
    public void recordTaskFinished(TaskExecutionContext context,
                                   TaskType taskType,
                                   TaskRunResult result,
                                   String message,
                                   long elapsedMs,
                                   String errorCode) {
        record(baseEvent(context)
                .eventType(AutomationMetricEventType.TASK_FINISHED)
                .status(statusFromTaskResult(result, errorCode))
                .taskCode(taskType == null ? valueOrNull(context == null ? null : context.getTaskCode()) : taskType.getCode())
                .taskName(taskType == null ? valueOrNull(context == null ? null : context.getTaskName()) : taskType.getDisplayName())
                .phase("task")
                .elapsedMs(elapsedMs)
                .errorCode(errorCode)
                .message(message)
                .build());
    }

    /**
     * Record one task-turn transaction boundary.
     *
     * @param context nullable current task context.
     * @param name transaction name.
     * @param expectedResult expected business result.
     * @param yieldPolicy requested turn handoff policy.
     * @param outcome nullable transaction outcome; null means the callback threw before producing one.
     * @param elapsedMs transaction elapsed time in milliseconds.
     * @param exclusive true when the serialized input worker was held during the transaction.
     */
    public void recordTransaction(TaskExecutionContext context,
                                  String name,
                                  TaskTransactionResult expectedResult,
                                  TaskYieldPolicy yieldPolicy,
                                  TaskTransactionOutcome outcome,
                                  long elapsedMs,
                                  boolean exclusive) {
        TaskTransactionResult result = outcome == null ? TaskTransactionResult.FAILED : outcome.result();
        boolean completed = outcome != null && outcome.completed();
        record(baseEvent(context)
                .eventType(AutomationMetricEventType.TASK_TRANSACTION)
                .status(statusFromTransaction(result, completed))
                .phase(name)
                .elapsedMs(elapsedMs)
                .errorCode(completed ? null : "TRANSACTION_INCOMPLETE")
                .message("task transaction finished")
                .attributes(Map.of(
                        "expectedResult", stringify(expectedResult),
                        "result", stringify(result),
                        "yieldPolicy", stringify(yieldPolicy),
                        "completed", Boolean.toString(completed),
                        "exclusive", Boolean.toString(exclusive)))
                .build());
    }

    /**
     * Link a saved Xiuluo failure case into the metrics stream.
     *
     * @param context bound task/window context.
     * @param caseDir saved failure-case directory.
     * @param reason stable failure reason.
     * @param phase Xiuluo phase that failed.
     * @param round Xiuluo round number.
     * @param message short failure summary.
     */
    public void recordXiuluoFailureCase(TaskExecutionContext context,
                                        Path caseDir,
                                        String reason,
                                        String phase,
                                        int round,
                                        String message) {
        String caseId = caseDir == null ? null : caseDir.getFileName().toString();
        record(baseEvent(context)
                .eventType(AutomationMetricEventType.XIULUO_FAILURE_CASE)
                .status(AutomationMetricStatus.FAILED)
                .phase(phase)
                .errorCode(reason)
                .caseId(caseId)
                .message(message)
                .attributes(Map.of(
                        "round", Integer.toString(round),
                        "caseDir", caseDir == null ? "" : caseDir.toString()))
                .build());
    }

    /**
     * Record a non-fatal window warning that should be visible in the dashboard.
     *
     * @param windowContext current window runtime context, if any.
     * @param source warning source/service.
     * @param message short operator-facing warning.
     * @param attributes optional structured details.
     */
    public void recordWindowWarning(WindowRuntimeContext windowContext,
                                    String source,
                                    String message,
                                    Map<String, String> attributes) {
        record(baseEvent(windowContext)
                .eventType(AutomationMetricEventType.SYSTEM_WARNING)
                .status(AutomationMetricStatus.WARNING)
                .phase(source)
                .message(message)
                .attributes(attributes == null ? Map.of() : attributes)
                .build());
    }

    public Path writeDashboardNow() {
        writeDashboard();
        return dashboardPath;
    }

    public Path getDashboardPath() {
        return dashboardPath;
    }

    private AutomationMetricEvent.AutomationMetricEventBuilder baseEvent(TaskExecutionContext context) {
        AutomationMetricEvent.AutomationMetricEventBuilder builder = AutomationMetricEvent.builder()
                .taskCode(context == null ? null : context.getTaskCode())
                .taskName(context == null ? null : context.getTaskName())
                .windowId(context == null ? null : context.getWindowId())
                .windowRole(context == null ? null : context.getWindowRole())
                .nativeWindowHandle(context == null ? null : context.getNativeWindowHandle());
        return builder;
    }

    private AutomationMetricEvent.AutomationMetricEventBuilder baseEvent(WindowRuntimeContext windowContext) {
        WindowNativeBinding binding = windowContext == null ? null : windowContext.getNativeBinding();
        return AutomationMetricEvent.builder()
                .windowId(windowContext == null ? null : windowContext.getWindowId())
                .windowRole(windowContext == null || windowContext.getRole() == null
                        ? null
                        : windowContext.getRole().name())
                .nativeWindowHandle(binding == null ? null : binding.getNativeHandle())
                .taskCode(windowContext == null || windowContext.getLastTaskType() == null
                        ? null
                        : windowContext.getLastTaskType().getCode())
                .taskName(windowContext == null || windowContext.getLastTaskType() == null
                        ? null
                        : windowContext.getLastTaskType().getDisplayName());
    }

    private AutomationMetricEvent completeEvent(AutomationMetricEvent event) {
        return event.toBuilder()
                .schemaVersion(valueOrDefault(event.getSchemaVersion(), SCHEMA_VERSION))
                .timestamp(valueOrDefault(event.getTimestamp(), OffsetDateTime.now().toString()))
                .eventId(valueOrDefault(event.getEventId(), UUID.randomUUID().toString()))
                .sessionId(valueOrDefault(event.getSessionId(), sessionId))
                .appVersion(valueOrDefault(event.getAppVersion(), APP_VERSION))
                .eventType(event.getEventType() == null
                        ? AutomationMetricEventType.SYSTEM_WARNING
                        : event.getEventType())
                .status(event.getStatus() == null ? AutomationMetricStatus.INFO : event.getStatus())
                .attributes(sanitizeAttributes(event.getAttributes()))
                .build();
    }

    private void updateAggregate(AutomationMetricEvent event) {
        acceptedEvents.incrementAndGet();
        increment(countersByEventType, stringify(event.getEventType()));
        increment(countersByStatus, stringify(event.getStatus()));
        if (event.getElapsedMs() != null && event.getElapsedMs() >= 0L) {
            latencyByEventType.computeIfAbsent(stringify(event.getEventType()), ignored -> new LatencyAggregate())
                    .add(event.getElapsedMs());
        }
        if (isFailureLike(event)) {
            increment(failuresByTask, valueOrDefault(event.getTaskCode(), "unknown-task"));
            increment(failuresByPhase, valueOrDefault(event.getPhase(), "unknown-phase"));
            increment(failuresByErrorCode, valueOrDefault(event.getErrorCode(), stringify(event.getStatus())));
        }
        synchronized (recentEventsLock) {
            recentEvents.addFirst(event);
            while (recentEvents.size() > RECENT_EVENT_LIMIT) {
                recentEvents.removeLast();
            }
        }
    }

    private boolean isFailureLike(AutomationMetricEvent event) {
        AutomationMetricStatus status = event.getStatus();
        return status == AutomationMetricStatus.FAILED
                || status == AutomationMetricStatus.FATAL
                || status == AutomationMetricStatus.WARNING;
    }

    private void writerLoop() {
        while (running || !eventQueue.isEmpty()) {
            try {
                AutomationMetricEvent event = eventQueue.poll(1, TimeUnit.SECONDS);
                if (event != null) {
                    appendEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running) {
                    break;
                }
            } catch (Exception e) {
                log.debug("automation metrics writer failed: {}", e.getMessage(), e);
            }
        }
        drainQueueOnce();
    }

    private void drainQueueOnce() {
        AutomationMetricEvent event;
        while ((event = eventQueue.poll()) != null) {
            appendEvent(event);
        }
    }

    private void appendEvent(AutomationMetricEvent event) {
        try {
            Path parent = eventLogPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = objectMapper.writeValueAsString(event)
                    .replace(System.lineSeparator(), "")
                    .replace("\n", "")
                    .replace("\r", "");
            Files.writeString(eventLogPath, json + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            long dropped = droppedEvents.incrementAndGet();
            log.debug("automation metrics event write failed: path={} dropped={} reason={}",
                    eventLogPath, dropped, e.getMessage());
        }
    }

    private void writeDashboardThrottled() {
        long now = System.currentTimeMillis();
        long previous = lastDashboardWriteMillis.get();
        if (now - previous < DASHBOARD_WRITE_INTERVAL_MS) {
            return;
        }
        if (!lastDashboardWriteMillis.compareAndSet(previous, now)) {
            return;
        }
        writeDashboard();
    }

    private synchronized void writeDashboard() {
        try {
            Path parent = dashboardPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Map<String, Object> snapshot = snapshot();
            Files.writeString(snapshotPath, objectMapper.writeValueAsString(snapshot), StandardCharsets.UTF_8);
            Files.writeString(dashboardPath, renderDashboardHtml(snapshot), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("automation metrics dashboard write failed: path={} reason={}",
                    dashboardPath, e.getMessage());
        }
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", SCHEMA_VERSION);
        snapshot.put("updatedAt", OffsetDateTime.now().toString());
        snapshot.put("sessionId", sessionId);
        snapshot.put("acceptedEvents", acceptedEvents.get());
        snapshot.put("droppedEvents", droppedEvents.get());
        snapshot.put("eventLogPath", eventLogPath.toString());
        snapshot.put("dashboardPath", dashboardPath.toString());
        snapshot.put("byEventType", counterSnapshot(countersByEventType));
        snapshot.put("byStatus", counterSnapshot(countersByStatus));
        snapshot.put("failuresByTask", counterSnapshot(failuresByTask));
        snapshot.put("failuresByPhase", counterSnapshot(failuresByPhase));
        snapshot.put("failuresByErrorCode", counterSnapshot(failuresByErrorCode));
        snapshot.put("latencyByEventType", latencySnapshot());
        snapshot.put("recentEvents", recentEventSnapshot());
        return snapshot;
    }

    private Map<String, Long> counterSnapshot(ConcurrentMap<String, LongAdder> counters) {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        counters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> snapshot.put(entry.getKey(), entry.getValue().sum()));
        return snapshot;
    }

    private Map<String, Map<String, Long>> latencySnapshot() {
        Map<String, Map<String, Long>> snapshot = new LinkedHashMap<>();
        latencyByEventType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LatencySnapshot latency = entry.getValue().snapshot();
                    Map<String, Long> values = new LinkedHashMap<>();
                    values.put("count", latency.getCount());
                    values.put("totalMs", latency.getTotalMs());
                    values.put("avgMs", latency.getAvgMs());
                    values.put("maxMs", latency.getMaxMs());
                    snapshot.put(entry.getKey(), values);
                });
        return snapshot;
    }

    private List<AutomationMetricEvent> recentEventSnapshot() {
        synchronized (recentEventsLock) {
            return new ArrayList<>(recentEvents);
        }
    }

    private String renderDashboardHtml(Map<String, Object> snapshot) {
        long totalEvents = acceptedEvents.get();
        long warnings = counterValue(countersByStatus, AutomationMetricStatus.WARNING.name());
        long failures = counterValue(countersByStatus, AutomationMetricStatus.FAILED.name())
                + counterValue(countersByStatus, AutomationMetricStatus.FATAL.name());
        long stopped = counterValue(countersByStatus, AutomationMetricStatus.STOPPED.name());
        String updatedAt = DASHBOARD_TIME_FORMATTER.format(java.time.LocalDateTime.now());
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta http-equiv="refresh" content="3">
                  <title>DHXY Automation Metrics</title>
                  <style>
                    :root { color-scheme: light dark; font-family: "Segoe UI", "Microsoft YaHei", sans-serif; }
                    body { margin: 0; background: #0f172a; color: #e5e7eb; }
                    main { padding: 24px; max-width: 1320px; margin: 0 auto; }
                    h1 { margin: 0 0 6px; font-size: 24px; }
                    h2 { font-size: 16px; margin: 22px 0 10px; color: #dbeafe; }
                    .muted { color: #94a3b8; margin-bottom: 18px; }
                    .cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 18px 0; }
                    .card { background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 14px; }
                    .label { color: #94a3b8; font-size: 12px; }
                    .value { font-size: 28px; font-weight: 700; margin-top: 4px; }
                    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
                    table { width: 100%%; border-collapse: collapse; background: #111827; border: 1px solid #334155; }
                    th, td { padding: 9px; border-bottom: 1px solid #263244; text-align: left; vertical-align: top; font-size: 13px; }
                    th { color: #cbd5e1; background: #1e293b; }
                    code { color: #bfdbfe; }
                    .ok { color: #86efac; }
                    .warn { color: #fde68a; }
                    .fail { color: #fca5a5; }
                    .empty { padding: 18px; color: #94a3b8; background: #1e293b; border-radius: 8px; }
                  </style>
                </head>
                <body>
                <main>
                  <h1>DHXY Automation Metrics</h1>
                  <div class="muted">Updated: %s · Auto refreshes every 3 seconds · JSONL: <code>%s</code></div>
                  <section class="cards">
                    <div class="card"><div class="label">Events</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">Failures/Fatal</div><div class="value fail">%d</div></div>
                    <div class="card"><div class="label">Warnings</div><div class="value warn">%d</div></div>
                    <div class="card"><div class="label">Stopped</div><div class="value">%d</div></div>
                  </section>
                  <section class="grid">
                    <div><h2>Failures by Task</h2>%s</div>
                    <div><h2>Failures by Phase</h2>%s</div>
                    <div><h2>Failures by Error</h2>%s</div>
                    <div><h2>Latency by Event Type</h2>%s</div>
                  </section>
                  <h2>Recent Events</h2>
                  %s
                </main>
                </body>
                </html>
                """.formatted(updatedAt, escape(eventLogPath.toString()), totalEvents, failures, warnings, stopped,
                renderCounterTable(failuresByTask, "Task"),
                renderCounterTable(failuresByPhase, "Phase"),
                renderCounterTable(failuresByErrorCode, "Error"),
                renderLatencyTable(),
                renderRecentEvents());
    }

    private String renderCounterTable(ConcurrentMap<String, LongAdder> counters, String label) {
        List<Map.Entry<String, LongAdder>> rows = counters.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, LongAdder> entry) -> entry.getValue().sum()).reversed())
                .limit(12)
                .toList();
        if (rows.isEmpty()) {
            return "<div class=\"empty\">No data yet.</div>";
        }
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, LongAdder> row : rows) {
            body.append("<tr><td>").append(escape(row.getKey())).append("</td><td>")
                    .append(row.getValue().sum()).append("</td></tr>");
        }
        return "<table><thead><tr><th>" + escape(label) + "</th><th>Count</th></tr></thead><tbody>"
                + body + "</tbody></table>";
    }

    private String renderLatencyTable() {
        if (latencyByEventType.isEmpty()) {
            return "<div class=\"empty\">No latency data yet.</div>";
        }
        StringBuilder body = new StringBuilder();
        latencyByEventType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LatencySnapshot snapshot = entry.getValue().snapshot();
                    body.append("<tr><td>").append(escape(entry.getKey())).append("</td><td>")
                            .append(snapshot.getCount()).append("</td><td>")
                            .append(snapshot.getAvgMs()).append("</td><td>")
                            .append(snapshot.getMaxMs()).append("</td></tr>");
                });
        return "<table><thead><tr><th>Event</th><th>Count</th><th>Avg ms</th><th>Max ms</th></tr></thead><tbody>"
                + body + "</tbody></table>";
    }

    private String renderRecentEvents() {
        List<AutomationMetricEvent> events = recentEventSnapshot();
        if (events.isEmpty()) {
            return "<div class=\"empty\">No events yet.</div>";
        }
        StringBuilder rows = new StringBuilder();
        for (AutomationMetricEvent event : events) {
            String css = switch (event.getStatus()) {
                case SUCCESS -> "ok";
                case WARNING -> "warn";
                case FAILED, FATAL -> "fail";
                default -> "";
            };
            rows.append("<tr><td>").append(escape(shortTime(event.getTimestamp()))).append("</td>")
                    .append("<td>").append(escape(stringify(event.getEventType()))).append("</td>")
                    .append("<td class=\"").append(css).append("\">").append(escape(stringify(event.getStatus()))).append("</td>")
                    .append("<td>").append(escape(valueOrDefault(event.getWindowId(), "-"))).append("</td>")
                    .append("<td>").append(escape(valueOrDefault(event.getTaskCode(), "-"))).append("</td>")
                    .append("<td>").append(escape(valueOrDefault(event.getPhase(), "-"))).append("</td>")
                    .append("<td>").append(event.getElapsedMs() == null ? "-" : event.getElapsedMs()).append("</td>")
                    .append("<td>").append(escape(valueOrDefault(event.getMessage(), "-"))).append("</td>")
                    .append("<td>").append(escape(valueOrDefault(event.getCaseId(), "-"))).append("</td></tr>");
        }
        return "<table><thead><tr><th>Time</th><th>Event</th><th>Status</th><th>Window</th><th>Task</th><th>Phase</th><th>ms</th><th>Message</th><th>Case</th></tr></thead><tbody>"
                + rows + "</tbody></table>";
    }

    private long counterValue(ConcurrentMap<String, LongAdder> counters, String key) {
        LongAdder adder = counters.get(key);
        return adder == null ? 0L : adder.sum();
    }

    private void increment(ConcurrentMap<String, LongAdder> counters, String key) {
        counters.computeIfAbsent(valueOrDefault(key, "unknown"), ignored -> new LongAdder()).increment();
    }

    private AutomationMetricStatus statusFromTaskResult(TaskRunResult result, String errorCode) {
        if (errorCode != null && !errorCode.isBlank()) {
            return AutomationMetricStatus.FATAL;
        }
        if (result == null) {
            return AutomationMetricStatus.FAILED;
        }
        return switch (result) {
            case SUCCESS -> AutomationMetricStatus.SUCCESS;
            case FAILED -> AutomationMetricStatus.FAILED;
            case STOPPED -> AutomationMetricStatus.STOPPED;
            case SKIPPED -> AutomationMetricStatus.SKIPPED;
        };
    }

    private AutomationMetricStatus statusFromTransaction(TaskTransactionResult result, boolean completed) {
        if (!completed) {
            return AutomationMetricStatus.FAILED;
        }
        if (result == TaskTransactionResult.STOPPED) {
            return AutomationMetricStatus.STOPPED;
        }
        if (result == TaskTransactionResult.FAILED || result == TaskTransactionResult.RETRYABLE_ERROR) {
            return AutomationMetricStatus.FAILED;
        }
        return AutomationMetricStatus.SUCCESS;
    }

    private Map<String, String> sanitizeAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            sanitized.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return Map.copyOf(sanitized);
    }

    private String stringify(Object value) {
        return value == null ? "unknown" : value.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String valueOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String shortTime(String timestamp) {
        if (timestamp == null || timestamp.length() < 19) {
            return valueOrDefault(timestamp, "-");
        }
        return timestamp.substring(11, 19);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static class LatencyAggregate {
        private final LongAdder count = new LongAdder();
        private final LongAdder totalMs = new LongAdder();
        private final AtomicLong maxMs = new AtomicLong();

        private void add(long elapsedMs) {
            count.increment();
            totalMs.add(elapsedMs);
            maxMs.updateAndGet(current -> Math.max(current, elapsedMs));
        }

        private LatencySnapshot snapshot() {
            long c = count.sum();
            long total = totalMs.sum();
            return LatencySnapshot.builder()
                    .count(c)
                    .totalMs(total)
                    .avgMs(c <= 0 ? 0L : Math.round(total * 1.0 / c))
                    .maxMs(maxMs.get())
                    .build();
        }
    }

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    private static class LatencySnapshot {
        long count;
        long totalMs;
        long avgMs;
        long maxMs;
    }
}
