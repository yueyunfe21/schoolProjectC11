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
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final int RECENT_EVENT_LIMIT = 20000;
    private static final int LEDGER_ROW_LIMIT = 1000;
    private static final long DASHBOARD_WRITE_INTERVAL_MS = 1000L;
    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final BlockingQueue<AutomationMetricEvent> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final BlockingQueue<String> dashboardWriteQueue = new LinkedBlockingQueue<>(1);
    private final String sessionId = UUID.randomUUID().toString();
    private final Path eventLogPath;
    private final Path snapshotPath;
    private final Path dashboardPath;
    private final DiagnosticCaseCaptureService diagnosticCaseCaptureService;
    private final AtomicLong acceptedEvents = new AtomicLong();
    private final AtomicLong droppedEvents = new AtomicLong();
    private final AtomicLong lastDashboardWriteMillis = new AtomicLong();
    private final AtomicLong coalescedDashboardWriteRequests = new AtomicLong();
    private final ConcurrentMap<String, LongAdder> countersByEventType = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> countersByStatus = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failuresByTask = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failuresByPhase = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failuresByErrorCode = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LatencyAggregate> latencyByEventType = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RoundSummary> activeRoundsById = new ConcurrentHashMap<>();
    private final Deque<AutomationMetricEvent> recentEvents = new ArrayDeque<>();
    private final Deque<RoundSummary> recentRounds = new ArrayDeque<>();
    private final Object recentEventsLock = new Object();
    private final Object recentRoundsLock = new Object();

    private volatile boolean running;
    private Thread writerThread;
    private Thread dashboardWriterThread;

    @Autowired
    public AutomationMetricsService(DiagnosticCaseCaptureService diagnosticCaseCaptureService) {
        this(Path.of("logs", "automation-metrics.jsonl"),
                Path.of("logs", "automation-dashboard-data.json"),
                Path.of("logs", "automation-dashboard.html"),
                diagnosticCaseCaptureService);
    }

    AutomationMetricsService(Path eventLogPath, Path snapshotPath, Path dashboardPath) {
        this(eventLogPath, snapshotPath, dashboardPath, DiagnosticCaseCaptureService.disabled());
    }

    AutomationMetricsService(Path eventLogPath,
                             Path snapshotPath,
                             Path dashboardPath,
                             DiagnosticCaseCaptureService diagnosticCaseCaptureService) {
        this.eventLogPath = eventLogPath.toAbsolutePath().normalize();
        this.snapshotPath = snapshotPath.toAbsolutePath().normalize();
        this.dashboardPath = dashboardPath.toAbsolutePath().normalize();
        this.diagnosticCaseCaptureService = diagnosticCaseCaptureService;
    }

    @PostConstruct
    public void start() {
        running = true;
        loadExistingMetricsForDashboard();
        writerThread = new Thread(this::writerLoop, "dhxy-automation-metrics-writer");
        writerThread.setDaemon(true);
        writerThread.start();
        dashboardWriterThread = new Thread(this::dashboardWriterLoop, "dhxy-automation-dashboard-writer");
        dashboardWriterThread.setDaemon(true);
        dashboardWriterThread.start();
        queueDashboardWrite("startup");
    }

    @PreDestroy
    public void stop() {
        running = false;
        Thread thread = writerThread;
        if (thread != null) {
            thread.interrupt();
        }
        Thread dashboardThread = dashboardWriterThread;
        if (dashboardThread != null) {
            dashboardThread.interrupt();
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
        maybeCaptureDiagnosticCase(completed);
        if (!eventQueue.offer(completed)) {
            long dropped = droppedEvents.incrementAndGet();
            log.warn("automation metrics queue full; drop event: type={} status={} dropped={}",
                    completed.getEventType(), completed.getStatus(), dropped);
            return;
        }
        writeDashboardThrottled();
    }

    /**
     * TURN-40B-C1 sole wire seam: records one already-reconstructed metric event exactly like
     * {@link #record(AutomationMetricEvent)} and, only when {@code queueDashboard} is true, also
     * queues the same dashboard write that the baseline {@code recordRoundFinished} queues.
     *
     * <p>The per-operation mapping is frozen as STARTED=false, FINISHED=true, FAILURE_CASE=false,
     * matching the baseline methods byte-for-byte. This method adds no second store, thread,
     * throttle, or retry — it only composes the two existing private behaviors for the local
     * metric-wire executor.</p>
     *
     * @param event fully reconstructed event; identity fields are preserved, missing
     *              ids/timestamps/session fields are filled exactly as in {@code record}.
     * @param queueDashboard true only for the round-finished wire operation.
     */
    public void recordWireEvent(AutomationMetricEvent event, boolean queueDashboard) {
        if (event == null) {
            return;
        }
        record(event);
        if (queueDashboard) {
            queueDashboardWrite("round-finished");
        }
    }

    private void maybeCaptureDiagnosticCase(AutomationMetricEvent event) {
        if (diagnosticCaseCaptureService == null) {
            return;
        }
        try {
            diagnosticCaseCaptureService.captureIfNeeded(event, recentEventSnapshot());
        } catch (Exception e) {
            log.warn("[diagnostic-case] capture hook failed but metrics event is kept: type={} status={} task={} window={} reason={}",
                    event.getEventType(), event.getStatus(), event.getTaskCode(), event.getWindowId(), e.getMessage(), e);
        }
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
     * Record a business round start for the operator-facing round ledger.
     *
     * @param context nullable bound task/window context.
     * @param roundId stable id for one business round; copied into the dashboard locator.
     * @param roundNumber human round number inside the current task run, or {@code <= 0} when unknown.
     * @param roundType business type, such as 普通怪、显形镜、黄袍怪, or unknown.
     * @param message short operator-facing start summary.
     * @param attributes small structured details such as character, target map, target NPC, or source phase.
     */
    public void recordRoundStarted(TaskExecutionContext context,
                                   String roundId,
                                   int roundNumber,
                                   String roundType,
                                   String message,
                                   Map<String, String> attributes) {
        Map<String, String> details = roundAttributes(roundId, roundNumber, roundType, attributes);
        record(baseEvent(context)
                .eventType(AutomationMetricEventType.TASK_ROUND_STARTED)
                .status(AutomationMetricStatus.STARTED)
                .runId(roundId)
                .phase(valueOrDefault(details.get("sourcePhase"), "round"))
                .message(message)
                .attributes(details)
                .build());
    }

    /**
     * Record a business round finish for the operator-facing round ledger.
     *
     * @param context nullable bound task/window context.
     * @param roundId stable id matching the start event.
     * @param roundNumber human round number inside the current task run, or {@code <= 0} when unknown.
     * @param roundType business type, such as 普通怪、显形镜、黄袍怪, or unknown.
     * @param status normalized final round status. Failed reaccept/retry rounds should use FAILED.
     * @param resultCode stable business result code, for example SUCCESS, FAILED_REACCEPT, TIMEOUT.
     * @param message short operator-facing finish summary.
     * @param elapsedMs round duration from accept/start to finish in milliseconds.
     * @param attributes small structured details such as slowestStage, target, caseId, or source phase.
     */
    public void recordRoundFinished(TaskExecutionContext context,
                                    String roundId,
                                    int roundNumber,
                                    String roundType,
                                    AutomationMetricStatus status,
                                    String resultCode,
                                    String message,
                                    long elapsedMs,
                                    Map<String, String> attributes) {
        Map<String, String> details = mutableCopy(attributes);
        details.putAll(roundAttributes(roundId, roundNumber, roundType, attributes));
        if (resultCode != null && !resultCode.isBlank()) {
            details.put("resultCode", resultCode);
        }
        record(baseEvent(context)
                .eventType(AutomationMetricEventType.TASK_ROUND_FINISHED)
                .status(status == null ? AutomationMetricStatus.INFO : status)
                .runId(roundId)
                .phase(valueOrDefault(details.get("sourcePhase"), "round"))
                .elapsedMs(elapsedMs)
                .errorCode(status == AutomationMetricStatus.SUCCESS ? null : resultCode)
                .caseId(details.get("caseId"))
                .message(message)
                .attributes(Map.copyOf(details))
                .build());
        queueDashboardWrite("round-finished");
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
        updateRoundAggregate(event);
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

    private void loadExistingMetricsForDashboard() {
        if (!Files.exists(eventLogPath)) {
            return;
        }
        ArrayDeque<String> tail = new ArrayDeque<>();
        try (var lines = Files.lines(eventLogPath, StandardCharsets.UTF_8)) {
            lines.filter(line -> line != null && !line.isBlank()).forEach(line -> {
                tail.addFirst(line);
                while (tail.size() > RECENT_EVENT_LIMIT) {
                    tail.removeLast();
                }
            });
        } catch (Exception e) {
            log.debug("automation metrics history load failed: path={} reason={}", eventLogPath, e.getMessage());
            return;
        }
        List<String> history = new ArrayList<>(tail);
        java.util.Collections.reverse(history);
        for (String line : history) {
            try {
                updateAggregate(parsePersistedMetricEvent(line));
            } catch (Exception e) {
                log.debug("automation metrics history line skipped: reason={}", e.getMessage());
            }
        }
    }

    private AutomationMetricEvent parsePersistedMetricEvent(String line) throws Exception {
        JsonNode root = objectMapper.readTree(line);
        Map<String, String> attributes = new LinkedHashMap<>();
        JsonNode attributeNode = root.path("attributes");
        if (attributeNode.isObject()) {
            attributeNode.fields().forEachRemaining(entry -> attributes.put(entry.getKey(), entry.getValue().asText("")));
        }
        return AutomationMetricEvent.builder()
                .schemaVersion(textField(root, "schemaVersion"))
                .timestamp(textField(root, "timestamp"))
                .eventId(textField(root, "eventId"))
                .sessionId(textField(root, "sessionId"))
                .runId(textField(root, "runId"))
                .customerId(textField(root, "customerId"))
                .licenseId(textField(root, "licenseId"))
                .appVersion(textField(root, "appVersion"))
                .windowId(textField(root, "windowId"))
                .windowRole(textField(root, "windowRole"))
                .nativeWindowHandle(textField(root, "nativeWindowHandle"))
                .taskCode(textField(root, "taskCode"))
                .taskName(textField(root, "taskName"))
                .phase(textField(root, "phase"))
                .eventType(enumField(root, "eventType", AutomationMetricEventType.SYSTEM_WARNING))
                .status(enumField(root, "status", AutomationMetricStatus.INFO))
                .elapsedMs(longField(root, "elapsedMs"))
                .errorCode(textField(root, "errorCode"))
                .caseId(textField(root, "caseId"))
                .message(textField(root, "message"))
                .attributes(Map.copyOf(attributes))
                .build();
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
        queueDashboardWrite("throttled");
    }

    private void queueDashboardWrite(String reason) {
        if (dashboardWriteQueue.offer(reason)) {
            if (isRoundFinishDashboardWrite(reason)) {
                log.info("automation metrics dashboard write queued: reason={}", reason);
            } else {
                log.debug("automation metrics dashboard write queued: reason={}", reason);
            }
            return;
        }
        long coalescedRequests = coalescedDashboardWriteRequests.incrementAndGet();
        if (isRoundFinishDashboardWrite(reason)) {
            log.info("automation metrics dashboard write coalesced: reason={} coalescedRequests={}",
                    reason, coalescedRequests);
        } else {
            log.debug("automation metrics dashboard write coalesced: reason={} coalescedRequests={}",
                    reason, coalescedRequests);
        }
    }

    private void dashboardWriterLoop() {
        while (running || !dashboardWriteQueue.isEmpty()) {
            try {
                String reason = dashboardWriteQueue.poll(1, TimeUnit.SECONDS);
                if (reason == null) {
                    continue;
                }
                int queuedRequestsDrained = 0;
                while (dashboardWriteQueue.poll() != null) {
                    queuedRequestsDrained++;
                }
                long coalescedRequests = coalescedDashboardWriteRequests.getAndSet(0L);
                long startedAt = System.currentTimeMillis();
                writeDashboard();
                long elapsedMs = System.currentTimeMillis() - startedAt;
                if (isRoundFinishDashboardWrite(reason) || coalescedRequests > 0L) {
                    log.info("automation metrics dashboard write flushed: reason={} queuedRequestsDrained={} coalescedRequests={} elapsedMs={}",
                            reason, queuedRequestsDrained, coalescedRequests, elapsedMs);
                } else {
                    log.debug("automation metrics dashboard write flushed: reason={} queuedRequestsDrained={} coalescedRequests={} elapsedMs={}",
                            reason, queuedRequestsDrained, coalescedRequests, elapsedMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running) {
                    break;
                }
            } catch (Exception e) {
                log.warn("automation metrics dashboard async write failed: reason={}", e.getMessage(), e);
            }
        }
        while (dashboardWriteQueue.poll() != null) {
            writeDashboard();
        }
    }

    private boolean isRoundFinishDashboardWrite(String reason) {
        return "round-finished".equals(reason);
    }

    private synchronized void writeDashboard() {
        lastDashboardWriteMillis.set(System.currentTimeMillis());
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
        snapshot.put("recentRounds", recentRoundSnapshot());
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

    private List<RoundSummary> recentRoundSnapshot() {
        synchronized (recentRoundsLock) {
            return new ArrayList<>(recentRounds);
        }
    }

    private void updateRoundAggregate(AutomationMetricEvent event) {
        if (event.getEventType() != AutomationMetricEventType.TASK_ROUND_STARTED
                && event.getEventType() != AutomationMetricEventType.TASK_ROUND_FINISHED) {
            return;
        }
        String roundId = valueOrDefault(event.getRunId(), event.getAttributes().get("roundId"));
        if (roundId == null || roundId.isBlank() || "unknown".equals(roundId)) {
            return;
        }
        if (event.getEventType() == AutomationMetricEventType.TASK_ROUND_STARTED) {
            RoundSummary started = RoundSummary.fromStart(event, roundId);
            activeRoundsById.put(roundId, started);
            addRecentRound(started);
            return;
        }
        RoundSummary base = activeRoundsById.remove(roundId);
        RoundSummary finished = RoundSummary.fromFinish(base, event, roundId);
        addRecentRound(finished);
    }

    private void addRecentRound(RoundSummary round) {
        synchronized (recentRoundsLock) {
            recentRounds.removeIf(existing -> existing.getRoundId().equals(round.getRoundId()));
            recentRounds.addFirst(round);
            while (recentRounds.size() > RECENT_EVENT_LIMIT) {
                recentRounds.removeLast();
            }
        }
    }

    private Map<String, String> roundAttributes(String roundId,
                                                int roundNumber,
                                                String roundType,
                                                Map<String, String> attributes) {
        Map<String, String> details = mutableCopy(attributes);
        details.put("roundId", valueOrDefault(roundId, "unknown-round"));
        if (roundNumber > 0) {
            details.put("roundNumber", Integer.toString(roundNumber));
        }
        details.put("roundType", valueOrDefault(roundType, "unknown"));
        return Map.copyOf(details);
    }

    private Map<String, String> mutableCopy(Map<String, String> attributes) {
        Map<String, String> copy = new LinkedHashMap<>();
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return copy;
    }

    private String roundLocator(RoundSummary round) {
        return String.join("\n",
                "DHXY_ROUND_REF v1",
                "roundId=" + valueOrDefault(round.getRoundId(), ""),
                "taskCode=" + valueOrDefault(round.getTaskCode(), ""),
                "taskName=" + valueOrDefault(round.getTaskName(), ""),
                "windowId=" + valueOrDefault(round.getWindowId(), ""),
                "role=" + valueOrDefault(round.getWindowRole(), ""),
                "character=" + valueOrDefault(round.getCharacter(), ""),
                "roundType=" + valueOrDefault(round.getRoundType(), ""),
                "result=" + valueOrDefault(round.getResultCode(), stringify(round.getStatus())),
                "startAt=" + valueOrDefault(round.getStartAt(), ""),
                "endAt=" + valueOrDefault(round.getEndAt(), ""),
                "durationMs=" + (round.getElapsedMs() == null ? "" : round.getElapsedMs()),
                "slowestStage=" + valueOrDefault(round.getSlowestStage(), ""),
                "consoleLog=" + Path.of("logs", "dhxy-console.log").toAbsolutePath().normalize(),
                "metricFile=" + eventLogPath);
    }

    private String formatDuration(long elapsedMs) {
        if (elapsedMs < 60_000L) {
            return String.format(Locale.ROOT, "%.1fs", elapsedMs / 1000.0);
        }
        long minutes = elapsedMs / 60_000L;
        long seconds = (elapsedMs % 60_000L) / 1000L;
        return minutes + "m " + seconds + "s (" + String.format(Locale.ROOT, "%.1fs", elapsedMs / 1000.0) + ")";
    }

    private String sortCell(String text) {
        return sortCell(text, text, "");
    }

    private String sortCell(String text, String css) {
        return sortCell(text, text, css);
    }

    private String sortCell(long sortValue, String text) {
        return sortCell(Long.toString(sortValue), text, "");
    }

    private String sortCell(String sortValue, String text, String css) {
        String classAttribute = css == null || css.isBlank() ? "" : " class=\"" + escapeAttribute(css) + "\"";
        return "<td" + classAttribute + " data-sort=\"" + escapeAttribute(valueOrDefault(sortValue, "")) + "\">"
                + escape(valueOrDefault(text, "-")) + "</td>";
    }

    private String sortCellHtml(String sortValue, String html, String css) {
        String classAttribute = css == null || css.isBlank() ? "" : " class=\"" + escapeAttribute(css) + "\"";
        return "<td" + classAttribute + " data-sort=\"" + escapeAttribute(valueOrDefault(sortValue, "")) + "\">"
                + valueOrDefault(html, "-") + "</td>";
    }

    private String statusCss(AutomationMetricStatus status) {
        if (status == AutomationMetricStatus.SUCCESS) {
            return "ok";
        }
        if (status == AutomationMetricStatus.WARNING) {
            return "warn";
        }
        if (status == AutomationMetricStatus.FAILED || status == AutomationMetricStatus.FATAL) {
            return "fail";
        }
        return "";
    }

    private String displayTaskName(RoundSummary round) {
        return displayTaskNameFromCode(round.getTaskCode(), round.getTaskName());
    }

    private String taskKind(RoundSummary round) {
        return taskKind(round.getTaskCode(), round.getTaskName(), round.getRoundType());
    }

    private String taskKind(String code, String name, String roundType) {
        String combined = (valueOrDefault(code, "") + " " + valueOrDefault(name, "") + " "
                + valueOrDefault(roundType, "")).toLowerCase(Locale.ROOT);
        if (combined.contains("xiuluo") || combined.contains("修罗")) {
            return "xiuluo";
        }
        if (combined.contains("wubei") || combined.contains("五倍")) {
            return "wubei";
        }
        if (combined.contains("wuhuan") || combined.contains("five_ring") || combined.contains("五环")) {
            return "wuhuan";
        }
        return "unknown";
    }

    private String taskBadge(RoundSummary round) {
        String taskName = displayTaskName(round);
        String kind = taskKind(round);
        return "<span class=\"task-badge task-" + escapeAttribute(kind) + "\">" + escape(taskName) + "</span>";
    }

    private String displayTaskNameFromCode(String code, String name) {
        String taskName = valueOrDefault(name, "");
        if (!taskName.isBlank()) {
            return taskName;
        }
        String taskCode = valueOrDefault(code, "");
        if (taskCode.contains("xiuluo")) {
            return "修罗";
        }
        if (taskCode.contains("wubei")) {
            return "五倍";
        }
        if (taskCode.contains("wuhuan") || taskCode.contains("five_ring")) {
            return "五环";
        }
        return valueOrDefault(taskCode, "任务");
    }

    private String displayRoundType(RoundSummary round) {
        String roundType = valueOrDefault(round.getRoundType(), "");
        if (!roundType.isBlank() && !"unknown".equalsIgnoreCase(roundType)) {
            return roundType;
        }
        return displayTaskName(round) + "一轮";
    }

    private String roundContentHtml(RoundSummary round) {
        StringBuilder html = new StringBuilder(escape(displayRoundType(round)));
        String markers = roundMarkersHtml(round);
        if (!markers.isBlank()) {
            html.append(markers);
        }
        return html.toString();
    }

    private String roundMarkersHtml(RoundSummary round) {
        StringBuilder markers = new StringBuilder();
        if (Boolean.TRUE.equals(round.getHadMaintenance())) {
            markers.append("<span class=\"round-dot dot-maintenance\" title=\"本轮有维护\"></span>");
        }
        if (Boolean.TRUE.equals(round.getHadSummonSkillCleanup())) {
            markers.append("<span class=\"round-dot dot-summon-skill\" title=\"本轮有三技能\"></span>");
        }
        if (markers.isEmpty()) {
            return "";
        }
        return "<span class=\"round-markers\">" + markers + "</span>";
    }

    private String displayPhase(String phase) {
        String value = valueOrDefault(phase, "");
        if (value.isBlank() || "-".equals(value)) {
            return "-";
        }
        if (value.contains("ACCEPT_TASK")) {
            return "接任务";
        }
        if (value.contains("TRY_TRACKER") || value.contains("TRACKER")) {
            return "点任务追踪";
        }
        if (value.contains("NAVIGATE")) {
            return "导航";
        }
        if (value.contains("ENTER_BATTLE")) {
            return "进战斗";
        }
        if (value.contains("WAIT_COMBAT") || value.contains("WAIT_BATTLE")) {
            return "等战斗";
        }
        if (value.contains("RETURN_HOME")) {
            return "回城";
        }
        if (value.contains("MAINTENANCE")) {
            return "补给维护";
        }
        if (value.contains("PREPARE_ROUND")) {
            return "准备下一轮";
        }
        if (value.contains(":")) {
            return value.substring(value.lastIndexOf(':') + 1).replace('_', ' ').toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private long epochMillis(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(timestamp).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
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
                  <title>DHXY Automation Metrics</title>
                  <style>
                    :root { color-scheme: light dark; font-family: "Segoe UI", "Microsoft YaHei", sans-serif; }
                    body { margin: 0; background: #0b1220; color: #e5e7eb; font-size: 16px; }
                    main { padding: 22px; max-width: 1760px; margin: 0 auto; }
                    h1 { margin: 0 0 8px; font-size: 30px; font-weight: 700; }
                    .muted { color: #94a3b8; margin-bottom: 14px; font-size: 15px; }
                    .summary { display: flex; flex-wrap: wrap; gap: 10px; margin: 14px 0 16px; }
                    .pill { border: 1px solid #334155; background: #111827; border-radius: 7px; padding: 8px 12px; font-size: 16px; }
                    .table-wrap { border: 1px solid #334155; overflow-x: auto; background: #111827; }
                    table { width: 100%%; border-collapse: collapse; min-width: 1380px; }
                    th, td { padding: 10px 12px; border-bottom: 1px solid #243044; text-align: left; vertical-align: top; font-size: 16px; line-height: 1.35; }
                    th { color: #dbeafe; background: #1e293b; position: sticky; top: 0; z-index: 1; white-space: nowrap; }
                    th button { all: unset; cursor: pointer; color: inherit; font-weight: 700; }
                    th button::after { content: " ↕"; color: #64748b; font-weight: 400; }
                    tr:hover td { background: #172033; }
                    code { color: #bfdbfe; }
                    .ok { color: #86efac; }
                    .warn { color: #fde68a; }
                    .fail { color: #fca5a5; }
                    .dashboard-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; margin: 12px 0 14px; flex-wrap: wrap; }
                    .filter-panel { display: flex; flex-direction: column; gap: 8px; }
                    .filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
                    .filter-label { color: #94a3b8; font-size: 14px; min-width: 42px; }
                    .task-filter { border: 1px solid #334155; background: #111827; color: #e5e7eb; border-radius: 6px; padding: 7px 11px; cursor: pointer; font-size: 15px; }
                    .task-filter.active { border-color: #93c5fd; background: #1e3a8a; }
                    .time-filter { border: 1px solid #334155; background: #111827; color: #e5e7eb; border-radius: 6px; padding: 7px 11px; cursor: pointer; font-size: 15px; }
                    .time-filter.active { border-color: #fbbf24; background: #78350f; }
                    .legend { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; color: #cbd5e1; font-size: 14px; }
                    .legend-item { display: inline-flex; align-items: center; gap: 6px; }
                    .legend-dot, .round-dot { display: inline-block; width: 9px; height: 9px; border-radius: 999px; vertical-align: middle; }
                    .dot-maintenance { background: #f59e0b; }
                    .dot-summon-skill { background: #22c55e; }
                    .round-markers { display: inline-flex; gap: 5px; margin-left: 8px; vertical-align: middle; }
                    .task-badge { display: inline-flex; align-items: center; gap: 6px; border: 1px solid transparent; border-radius: 999px; padding: 3px 9px; font-weight: 700; white-space: nowrap; }
                    .task-xiuluo { color: #bfdbfe; background: #1e3a8a; border-color: #2563eb; }
                    .task-wuhuan { color: #bbf7d0; background: #14532d; border-color: #16a34a; }
                    .task-wubei { color: #fed7aa; background: #7c2d12; border-color: #ea580c; }
                    .task-unknown { color: #e5e7eb; background: #334155; border-color: #475569; }
                    .task-summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; margin: 0 0 14px; }
                    .task-summary-card { border: 1px solid #334155; background: #111827; border-radius: 8px; padding: 11px 12px; }
                    .task-summary-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
                    .task-summary-value { font-size: 22px; font-weight: 800; }
                    .task-summary-meta { color: #94a3b8; font-size: 13px; margin-top: 4px; }
                    .lazy-row { display: none; }
                    .copy { background: #2563eb; color: white; border: 0; border-radius: 6px; padding: 7px 10px; cursor: pointer; font-size: 15px; white-space: nowrap; }
                    .copy:hover { background: #1d4ed8; }
                    .empty { padding: 22px; color: #94a3b8; background: #111827; border: 1px solid #334155; border-radius: 8px; }
                  </style>
                </head>
                <body>
                <main>
                  <h1>DHXY 任务统计</h1>
                  <div class="muted">更新：%s · 下滑加载更多轮次 · 点击“复制定位”后发给 Codex 可定位本地日志</div>
                  <section class="summary">
                    <div class="pill">记录：%d</div>
                    <div class="pill fail">失败：%d</div>
                    <div class="pill warn">警告：%d</div>
                    <div class="pill">停止：%d</div>
                  </section>
                  <section class="dashboard-top">
                    <div class="filter-panel">
                      <div class="filters" aria-label="任务筛选">
                        <span class="filter-label">任务</span>
                        <button class="task-filter active" type="button" data-task-filter="all" onclick="setTaskFilter('all', this)">全部</button>
                        <button class="task-filter" type="button" data-task-filter="xiuluo" onclick="setTaskFilter('xiuluo', this)">修罗</button>
                        <button class="task-filter" type="button" data-task-filter="wuhuan" onclick="setTaskFilter('wuhuan', this)">五环</button>
                        <button class="task-filter" type="button" data-task-filter="wubei" onclick="setTaskFilter('wubei', this)">五倍</button>
                      </div>
                      <div class="filters" aria-label="时间筛选">
                        <span class="filter-label">时间</span>
                        <button class="time-filter active" type="button" data-time-filter="day" onclick="setTimeFilter('day', this)">当天</button>
                        <button class="time-filter" type="button" data-time-filter="yesterday" onclick="setTimeFilter('yesterday', this)">昨天</button>
                        <button class="time-filter" type="button" data-time-filter="3d" onclick="setTimeFilter('3d', this)">三天</button>
                        <button class="time-filter" type="button" data-time-filter="week" onclick="setTimeFilter('week', this)">一周</button>
                        <button class="time-filter" type="button" data-time-filter="month" onclick="setTimeFilter('month', this)">一月</button>
                        <button class="time-filter" type="button" data-time-filter="all" onclick="setTimeFilter('all', this)">全部</button>
                      </div>
                    </div>
                    <div class="legend" aria-label="颜色说明">
                      <span class="legend-item"><span class="task-badge task-xiuluo">修罗</span></span>
                      <span class="legend-item"><span class="task-badge task-wuhuan">五环</span></span>
                      <span class="legend-item"><span class="task-badge task-wubei">五倍</span></span>
                      <span class="legend-item"><span class="legend-dot dot-maintenance"></span>维护</span>
                      <span class="legend-item"><span class="legend-dot dot-summon-skill"></span>三技能</span>
                    </div>
                  </section>
                  <section id="task-summary-grid" class="task-summary-grid" aria-label="任务耗时 P95"></section>
                  %s
                </main>
                <script>
                  let sortState = { index: 0, direction: "desc", type: "number" };
                  let visibleRows = 80;
                  let activeTaskFilter = "all";
                  let activeTimeFilter = "day";
                  const latestRoundEndMs = Math.max(0, ...Array.from(document.querySelectorAll("#task-ledger tbody tr"))
                    .map(row => Number(row.dataset.end || "0")));
                  function applyLazyRows() {
                    const rows = Array.from(document.querySelectorAll("#task-ledger tbody tr"));
                    let visibleIndex = 0;
                    rows.forEach(row => {
                      const task = row.dataset.task || "unknown";
                      const filteredOut = !matchesTaskFilter(row) || !matchesTimeFilter(row);
                      const lazyHidden = !filteredOut && visibleIndex++ >= visibleRows;
                      row.classList.toggle("lazy-row", filteredOut || lazyHidden);
                    });
                    updateSummary();
                  }
                  function loadMoreRows() {
                    visibleRows += 80;
                    applyLazyRows();
                  }
                  function sortTable(index, type) {
                    const tbody = document.querySelector("#task-ledger tbody");
                    if (!tbody) return;
                    const direction = sortState.index === index && sortState.direction === "asc" ? "desc" : "asc";
                    sortState = { index, direction, type };
                    const rows = Array.from(tbody.querySelectorAll("tr"));
                    rows.sort((a, b) => {
                      const av = a.children[index]?.dataset.sort || a.children[index]?.textContent || "";
                      const bv = b.children[index]?.dataset.sort || b.children[index]?.textContent || "";
                      const result = type === "number"
                        ? (Number(av) || 0) - (Number(bv) || 0)
                        : av.localeCompare(bv, "zh-CN");
                      return direction === "asc" ? result : -result;
                    });
                    rows.forEach(row => tbody.appendChild(row));
                    visibleRows = Math.max(visibleRows, 80);
                    applyLazyRows();
                  }
                  function setTaskFilter(task, button) {
                    activeTaskFilter = task;
                    visibleRows = 80;
                    document.querySelectorAll(".task-filter").forEach(item => item.classList.remove("active"));
                    if (button) button.classList.add("active");
                    applyLazyRows();
                  }
                  function setTimeFilter(range, button) {
                    activeTimeFilter = range;
                    visibleRows = 80;
                    document.querySelectorAll(".time-filter").forEach(item => item.classList.remove("active"));
                    if (button) button.classList.add("active");
                    applyLazyRows();
                  }
                  function matchesTaskFilter(row) {
                    const task = row.dataset.task || "unknown";
                    return activeTaskFilter === "all" || task === activeTaskFilter;
                  }
                  function matchesTimeFilter(row) {
                    if (activeTimeFilter === "all") return true;
                    const endMs = Number(row.dataset.end || "0");
                    if (!endMs) return false;
                    const now = latestRoundEndMs || Date.now();
                    let startMs = 0;
                    let endLimitMs = now + 60 * 60 * 1000;
                    if (activeTimeFilter === "day") {
                      const today = new Date(now);
                      today.setHours(0, 0, 0, 0);
                      startMs = today.getTime();
                      endLimitMs = startMs + 24 * 60 * 60 * 1000;
                    } else if (activeTimeFilter === "yesterday") {
                      const today = new Date(now);
                      today.setHours(0, 0, 0, 0);
                      endLimitMs = today.getTime();
                      startMs = endLimitMs - 24 * 60 * 60 * 1000;
                    } else if (activeTimeFilter === "3d") {
                      startMs = now - 3 * 24 * 60 * 60 * 1000;
                    } else if (activeTimeFilter === "week") {
                      startMs = now - 7 * 24 * 60 * 60 * 1000;
                    } else if (activeTimeFilter === "month") {
                      startMs = now - 30 * 24 * 60 * 60 * 1000;
                    }
                    return endMs >= startMs && endMs <= endLimitMs;
                  }
                  function timeFilterLabel() {
                    return {
                      day: "当天",
                      yesterday: "昨天",
                      "3d": "三天内",
                      week: "一周内",
                      month: "一月内",
                      all: "全部"
                    }[activeTimeFilter] || "当前范围";
                  }
                  function updateSummary() {
                    const grid = document.getElementById("task-summary-grid");
                    if (!grid) return;
                    const groups = new Map();
                    document.querySelectorAll("#task-ledger tbody tr").forEach(row => {
                      if (!matchesTaskFilter(row) || !matchesTimeFilter(row)) return;
                      const task = row.dataset.task || "unknown";
                      const taskName = row.dataset.taskName || task;
                      const duration = Number(row.dataset.duration || "0");
                      if (!duration) return;
                      if (!groups.has(task)) groups.set(task, { task, taskName, values: [] });
                      groups.get(task).values.push(duration);
                    });
                    if (groups.size === 0) {
                      grid.innerHTML = '<div class="empty">当前筛选没有任务轮次。</div>';
                      return;
                    }
                    const order = ["xiuluo", "wuhuan", "wubei", "unknown"];
                    const cards = Array.from(groups.values()).sort((a, b) => order.indexOf(a.task) - order.indexOf(b.task))
                      .map(group => {
                        const p95 = percentile(group.values, 0.95);
                        return `<div class="task-summary-card">
                          <div class="task-summary-title"><span class="task-badge task-${escapeHtml(group.task)}">${escapeHtml(group.taskName)}</span><span>${group.values.length} 轮</span></div>
                          <div class="task-summary-value">${formatDurationJs(p95)}</div>
                          <div class="task-summary-meta">${escapeHtml(timeFilterLabel())}整轮耗时 P95 · 样本 ${group.values.length}</div>
                        </div>`;
                      });
                    grid.innerHTML = cards.join("");
                  }
                  function percentile(values, ratio) {
                    const sorted = values.slice().sort((a, b) => a - b);
                    const index = Math.max(0, Math.min(sorted.length - 1, Math.ceil(sorted.length * ratio) - 1));
                    return sorted[index] || 0;
                  }
                  function formatDurationJs(ms) {
                    if (ms < 60000) return (ms / 1000).toFixed(1) + "s";
                    const minutes = Math.floor(ms / 60000);
                    const seconds = Math.floor((ms %% 60000) / 1000);
                    return `${minutes}m ${seconds}s (${(ms / 1000).toFixed(1)}s)`;
                  }
                  function escapeHtml(text) {
                    return String(text || "").replace(/[&<>"']/g, char => ({
                      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
                    }[char]));
                  }
                  window.addEventListener("scroll", () => {
                    if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 500) {
                      loadMoreRows();
                    }
                  });
                  window.addEventListener("DOMContentLoaded", applyLazyRows);
                  function copyRoundLocator(button) {
                    const text = button.dataset.locator || "";
                    const done = () => {
                      const old = button.textContent;
                      button.textContent = "已复制";
                      setTimeout(() => button.textContent = old, 1200);
                    };
                    if (navigator.clipboard && window.isSecureContext) {
                      navigator.clipboard.writeText(text).then(done).catch(() => fallbackCopy(text, done));
                    } else {
                      fallbackCopy(text, done);
                    }
                  }
                  function fallbackCopy(text, done) {
                    const area = document.createElement("textarea");
                    area.value = text;
                    area.style.position = "fixed";
                    area.style.left = "-9999px";
                    document.body.appendChild(area);
                    area.focus();
                    area.select();
                    try {
                      document.execCommand("copy");
                      done();
                    } finally {
                      document.body.removeChild(area);
                    }
                  }
                </script>
                </body>
                </html>
                """.formatted(updatedAt, totalEvents, failures, warnings, stopped,
                renderTaskLedger());
    }

    private String renderTaskLedger() {
        List<RoundSummary> rounds = ledgerRounds();
        if (rounds.isEmpty()) {
            return "<div class=\"empty\">还没有队长的一轮任务统计。任务跑起来后，这里只显示队长每一轮任务的总耗时。</div>";
        }
        StringBuilder rows = new StringBuilder();
        for (RoundSummary round : rounds) {
            String css = statusCss(round.getStatus());
            Long elapsedMs = round.getElapsedMs();
            String taskKind = taskKind(round);
            rows.append("<tr data-task=\"").append(escapeAttribute(taskKind))
                    .append("\" data-task-name=\"").append(escapeAttribute(displayTaskName(round)))
                    .append("\" data-end=\"").append(epochMillis(round.getEndAt()))
                    .append("\" data-duration=\"").append(elapsedMs == null ? -1L : elapsedMs)
                    .append("\">")
                    .append(sortCell(epochMillis(round.getEndAt()), shortTime(round.getEndAt())))
                    .append(sortCellHtml(displayTaskName(round), taskBadge(round), ""))
                    .append(sortCellHtml(displayRoundType(round), roundContentHtml(round), ""))
                    .append(sortCell(valueOrDefault(round.getResultCode(), stringify(round.getStatus())), css))
                    .append(sortCell(elapsedMs == null ? -1L : elapsedMs, elapsedMs == null ? "-" : formatDuration(elapsedMs)))
                    .append(sortCell(valueOrDefault(round.getMessage(), "-")))
                    .append(sortCell(round.getSlowestStageP95Ms() == null ? -1L : round.getSlowestStageP95Ms(),
                            round.getSlowestStageP95Ms() == null ? "-" : formatDuration(round.getSlowestStageP95Ms())))
                    .append(sortCell(round.getSlowestStageSampleCount() == null ? -1 : round.getSlowestStageSampleCount(),
                            round.getSlowestStageSampleCount() == null ? "-" : round.getSlowestStageSampleCount().toString()))
                    .append("<td><button class=\"copy\" type=\"button\" data-locator=\"")
                    .append(escapeAttribute(roundLocator(round)))
                    .append("\" onclick=\"copyRoundLocator(this)\">复制定位</button></td>")
                    .append("</tr>");
        }
        return """
                <div class="table-wrap">
                  <table id="task-ledger">
                    <thead><tr>
                      <th><button onclick="sortTable(0, 'number')">时间</button></th>
                      <th><button onclick="sortTable(1, 'text')">任务</button></th>
                      <th><button onclick="sortTable(2, 'text')">内容</button></th>
                      <th><button onclick="sortTable(3, 'text')">结果</button></th>
                      <th><button onclick="sortTable(4, 'number')">耗时</button></th>
                      <th><button onclick="sortTable(5, 'text')">最长记录</button></th>
                      <th><button onclick="sortTable(6, 'number')">P95</button></th>
                      <th><button onclick="sortTable(7, 'number')">样本</button></th>
                      <th>定位</th>
                    </tr></thead>
                    <tbody>%s</tbody>
                  </table>
                </div>
                """.formatted(rows);
    }

    private List<RoundSummary> ledgerRounds() {
        List<RoundSummary> rows = new ArrayList<>();
        for (RoundSummary round : recentRoundSnapshot()) {
            if (isLeaderRound(round)) {
                rows.add(round);
            }
        }
        rows.addAll(syntheticRoundsFromTransactions().stream()
                .filter(synthetic -> !overlapsExplicitRound(synthetic, rows))
                .toList());
        rows.sort(Comparator.comparingLong((RoundSummary round) -> epochMillis(round.getEndAt())).reversed());
        return rows.stream().limit(LEDGER_ROW_LIMIT).toList();
    }

    private boolean overlapsExplicitRound(RoundSummary synthetic, List<RoundSummary> explicitRounds) {
        long syntheticStart = epochMillis(synthetic.getStartAt());
        long syntheticEnd = epochMillis(synthetic.getEndAt());
        if (syntheticStart <= 0L || syntheticEnd <= 0L) {
            return false;
        }
        for (RoundSummary explicit : explicitRounds) {
            if (!sameRoundKey(synthetic, explicit)) {
                continue;
            }
            long explicitStart = epochMillis(explicit.getStartAt());
            long explicitEnd = epochMillis(explicit.getEndAt());
            if (explicitStart <= 0L || explicitEnd <= 0L) {
                continue;
            }
            if (syntheticStart <= explicitEnd && syntheticEnd >= explicitStart) {
                return true;
            }
        }
        return false;
    }

    private boolean sameRoundKey(RoundSummary left, RoundSummary right) {
        return valueOrDefault(left.getWindowId(), "").equals(valueOrDefault(right.getWindowId(), ""))
                && valueOrDefault(left.getTaskCode(), "").equals(valueOrDefault(right.getTaskCode(), ""));
    }

    private boolean isLeaderRound(RoundSummary round) {
        if (isDashboardTestFixture(round.getWindowId(), round.getRoundId())) {
            return false;
        }
        String role = valueOrDefault(round.getWindowRole(), "");
        return role.isBlank() || "LEADER".equalsIgnoreCase(role);
    }

    private List<RoundSummary> syntheticRoundsFromTransactions() {
        List<AutomationMetricEvent> events = recentEventSnapshot().stream()
                .filter(this::isLeaderTaskTransaction)
                .sorted(Comparator.comparingLong(event -> epochMillis(event.getTimestamp())))
                .toList();
        Map<String, PhaseLatencyStats> phaseStats = phaseLatencyStats(events);
        Map<String, SyntheticRoundBuilder> active = new LinkedHashMap<>();
        List<RoundSummary> rounds = new ArrayList<>();
        for (AutomationMetricEvent event : events) {
            String key = valueOrDefault(event.getWindowId(), "unknown-window")
                    + "|" + valueOrDefault(event.getTaskCode(), "unknown-task");
            SyntheticRoundBuilder current = active.get(key);
            if (isRoundStartBoundary(event)) {
                if (isCompleteSyntheticRound(current)) {
                    RoundSummary finished = current.finish();
                    rounds.add(finished);
                }
                current = new SyntheticRoundBuilder(event, phaseStats);
                active.put(key, current);
            } else if (current == null) {
                continue;
            }
            current.add(event);
        }
        for (SyntheticRoundBuilder current : active.values()) {
            if (isCompleteSyntheticRound(current)) {
                rounds.add(current.finish());
            }
        }
        return rounds;
    }

    private boolean isLeaderTaskTransaction(AutomationMetricEvent event) {
        if (event.getEventType() != AutomationMetricEventType.TASK_TRANSACTION) {
            return false;
        }
        if (!"LEADER".equalsIgnoreCase(valueOrDefault(event.getWindowRole(), ""))) {
            return false;
        }
        if (isDashboardTestFixture(event.getWindowId(), event.getRunId())) {
            return false;
        }
        String taskCode = valueOrDefault(event.getTaskCode(), "");
        return !"auto_battle".equalsIgnoreCase(taskCode);
    }

    private boolean isDashboardTestFixture(String windowId, String roundId) {
        String normalizedWindow = valueOrDefault(windowId, "");
        String normalizedRound = valueOrDefault(roundId, "");
        return normalizedWindow.startsWith("hwnd-test")
                || normalizedWindow.startsWith("hwnd-synthetic")
                || normalizedRound.startsWith("wubei-hwnd-test");
    }

    private boolean isRoundStartBoundary(AutomationMetricEvent event) {
        String phase = valueOrDefault(event.getPhase(), "");
        return phase.endsWith(":PREPARE_ROUND")
                || phase.endsWith(":ACCEPT_TASK_NAVIGATE_TO_NPC");
    }

    private boolean isRoundCompletionSignal(AutomationMetricEvent event) {
        String phase = valueOrDefault(event.getPhase(), "").toUpperCase(Locale.ROOT);
        return phase.contains("RETURN_HOME")
                || phase.contains("WAIT_TEAM_RETURN")
                || phase.contains("TASK_FINISH")
                || phase.contains("TASK_COMPLETE")
                || phase.contains("ROUND_FINISH");
    }

    private boolean isRoundProgressSignal(AutomationMetricEvent event) {
        String phase = valueOrDefault(event.getPhase(), "").toUpperCase(Locale.ROOT);
        return phase.contains("ACCEPT_TASK")
                || phase.contains("TRACKER")
                || phase.contains("PATHING")
                || phase.contains("COMBAT")
                || phase.contains("RETURN_HOME")
                || phase.contains("WAIT_TEAM_RETURN")
                || phase.contains("MAINTENANCE");
    }

    private boolean isMaintenanceSignal(AutomationMetricEvent event) {
        String text = eventSearchText(event);
        return text.contains("BROADCAST")
                || text.contains("医宝宝")
                || text.contains("修装备")
                || text.contains("修宝宝")
                || text.contains("REPAIR");
    }

    private static boolean isMaintenanceStageName(String stage) {
        String normalized = (stage == null ? "" : stage).toUpperCase(Locale.ROOT);
        return normalized.contains("MAINTENANCE")
                || normalized.contains("补给维护")
                || normalized.contains("医宝宝")
                || normalized.contains("修装备")
                || normalized.contains("修宝宝");
    }

    private boolean isSummonSkillSignal(AutomationMetricEvent event) {
        String text = eventSearchText(event);
        return text.contains("SUMMON")
                || text.contains("SKILL")
                || text.contains("CLEANUP")
                || text.contains("三技能");
    }

    private String eventSearchText(AutomationMetricEvent event) {
        StringBuilder text = new StringBuilder();
        text.append(valueOrDefault(event.getPhase(), "")).append(' ')
                .append(valueOrDefault(event.getMessage(), "")).append(' ')
                .append(valueOrDefault(event.getTaskCode(), "")).append(' ')
                .append(valueOrDefault(event.getTaskName(), ""));
        for (Map.Entry<String, String> entry : event.getAttributes().entrySet()) {
            text.append(' ').append(valueOrDefault(entry.getKey(), ""))
                    .append(' ').append(valueOrDefault(entry.getValue(), ""));
        }
        return text.toString().toUpperCase(Locale.ROOT);
    }

    private boolean isTerminalStatus(AutomationMetricEvent event) {
        AutomationMetricStatus status = event.getStatus();
        return status == AutomationMetricStatus.FAILED
                || status == AutomationMetricStatus.FATAL
                || status == AutomationMetricStatus.STOPPED;
    }

    private boolean isCompleteSyntheticRound(SyntheticRoundBuilder current) {
        return current != null
                && current.eventCount > 1
                && current.hasProgress
                && (current.hasCompletion || current.status != AutomationMetricStatus.SUCCESS);
    }

    private Map<String, PhaseLatencyStats> phaseLatencyStats(List<AutomationMetricEvent> events) {
        Map<String, List<Long>> valuesByPhase = new LinkedHashMap<>();
        for (AutomationMetricEvent event : events) {
            String phase = valueOrDefault(event.getPhase(), "");
            Long elapsedMs = event.getElapsedMs();
            if (phase.isBlank() || elapsedMs == null || elapsedMs < 0L) {
                continue;
            }
            valuesByPhase.computeIfAbsent(phase, ignored -> new ArrayList<>()).add(elapsedMs);
        }
        Map<String, PhaseLatencyStats> stats = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : valuesByPhase.entrySet()) {
            stats.put(entry.getKey(), PhaseLatencyStats.from(entry.getValue()));
        }
        return stats;
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

    private String textField(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Long longField(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asLong();
    }

    private <E extends Enum<E>> E enumField(JsonNode node, String fieldName, E fallback) {
        String value = textField(node, fieldName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
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

    private String escapeAttribute(String value) {
        return escape(value).replace("'", "&#39;");
    }

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    private static class RoundSummary {
        String roundId;
        Integer roundNumber;
        String startAt;
        String endAt;
        String windowId;
        String windowRole;
        String character;
        String taskCode;
        String taskName;
        String roundType;
        AutomationMetricStatus status;
        String resultCode;
        Long elapsedMs;
        String slowestStage;
        Long slowestStageP95Ms;
        Integer slowestStageSampleCount;
        Boolean hadMaintenance;
        Boolean hadSummonSkillCleanup;
        String message;

        private static RoundSummary fromStart(AutomationMetricEvent event, String roundId) {
            Map<String, String> attributes = event.getAttributes();
            return RoundSummary.builder()
                    .roundId(roundId)
                    .roundNumber(parseInt(attributes.get("roundNumber")))
                    .startAt(event.getTimestamp())
                    .windowId(valueOrDefaultStatic(event.getWindowId(), attributes.get("windowId")))
                    .windowRole(valueOrDefaultStatic(event.getWindowRole(), attributes.get("role")))
                    .character(attributes.get("character"))
                    .taskCode(event.getTaskCode())
                    .taskName(event.getTaskName())
                    .roundType(attributes.get("roundType"))
                    .status(event.getStatus())
                    .resultCode(stringifyStatic(event.getStatus()))
                    .message(event.getMessage())
                    .build();
        }

        private static RoundSummary fromFinish(RoundSummary base, AutomationMetricEvent event, String roundId) {
            Map<String, String> attributes = event.getAttributes();
            String slowestStage = attributes.get("slowestStage");
            return RoundSummary.builder()
                    .roundId(roundId)
                    .roundNumber(parseInt(valueOrDefaultStatic(attributes.get("roundNumber"),
                            base == null || base.getRoundNumber() == null ? null : base.getRoundNumber().toString())))
                    .startAt(base == null ? event.getTimestamp() : base.getStartAt())
                    .endAt(event.getTimestamp())
                    .windowId(valueOrDefaultStatic(event.getWindowId(),
                            valueOrDefaultStatic(attributes.get("windowId"), base == null ? null : base.getWindowId())))
                    .windowRole(valueOrDefaultStatic(event.getWindowRole(),
                            valueOrDefaultStatic(attributes.get("role"), base == null ? null : base.getWindowRole())))
                    .character(valueOrDefaultStatic(attributes.get("character"), base == null ? null : base.getCharacter()))
                    .taskCode(valueOrDefaultStatic(event.getTaskCode(), base == null ? null : base.getTaskCode()))
                    .taskName(valueOrDefaultStatic(event.getTaskName(), base == null ? null : base.getTaskName()))
                    .roundType(valueOrDefaultStatic(attributes.get("roundType"), base == null ? null : base.getRoundType()))
                    .status(event.getStatus())
                    .resultCode(valueOrDefaultStatic(attributes.get("resultCode"), stringifyStatic(event.getStatus())))
                    .elapsedMs(event.getElapsedMs())
                    .slowestStage(slowestStage)
                    .hadMaintenance(Boolean.parseBoolean(valueOrDefaultStatic(attributes.get("hadMaintenance"), "false"))
                            || isMaintenanceStageName(slowestStage))
                    .hadSummonSkillCleanup(Boolean.parseBoolean(valueOrDefaultStatic(attributes.get("hadSummonSkillCleanup"), "false")))
                    .message(event.getMessage())
                    .build();
        }

        private static Integer parseInt(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String stringifyStatic(Object value) {
            return value == null ? "unknown" : value.toString();
        }

        private static String valueOrDefaultStatic(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    private class SyntheticRoundBuilder {
        private final String roundId;
        private final String startAt;
        private final String windowId;
        private final String windowRole;
        private final String taskCode;
        private final String taskName;
        private final Map<String, PhaseLatencyStats> phaseStats;
        private String endAt;
        private AutomationMetricStatus status = AutomationMetricStatus.SUCCESS;
        private String resultCode = "SUCCESS";
        private String slowestStage;
        private long slowestStageMs = -1L;
        private int eventCount;
        private boolean hasProgress;
        private boolean hasCompletion;
        private boolean hadMaintenance;
        private boolean hadSummonSkillCleanup;

        private SyntheticRoundBuilder(AutomationMetricEvent first, Map<String, PhaseLatencyStats> phaseStats) {
            this.roundId = "synthetic-" + valueOrDefault(first.getWindowId(), "window")
                    + "-" + valueOrDefault(first.getTaskCode(), "task")
                    + "-" + epochMillis(first.getTimestamp());
            this.startAt = first.getTimestamp();
            this.windowId = first.getWindowId();
            this.windowRole = first.getWindowRole();
            this.taskCode = first.getTaskCode();
            this.taskName = first.getTaskName();
            this.phaseStats = phaseStats;
        }

        private void add(AutomationMetricEvent event) {
            eventCount++;
            endAt = event.getTimestamp();
            hasProgress = hasProgress || isRoundProgressSignal(event);
            hasCompletion = hasCompletion || isRoundCompletionSignal(event) || isTerminalStatus(event);
            hadMaintenance = hadMaintenance || isMaintenanceSignal(event);
            hadSummonSkillCleanup = hadSummonSkillCleanup || isSummonSkillSignal(event);
            if (event.getStatus() == AutomationMetricStatus.FAILED || event.getStatus() == AutomationMetricStatus.FATAL) {
                status = event.getStatus();
                resultCode = stringify(event.getStatus());
            } else if (event.getStatus() == AutomationMetricStatus.STOPPED && status == AutomationMetricStatus.SUCCESS) {
                status = AutomationMetricStatus.STOPPED;
                resultCode = "STOPPED";
            }
            Long elapsedMs = event.getElapsedMs();
            if (elapsedMs != null && elapsedMs > slowestStageMs) {
                slowestStageMs = elapsedMs;
                slowestStage = event.getPhase();
            }
        }

        private RoundSummary finish() {
            if (eventCount <= 0 || endAt == null) {
                return null;
            }
            long startMs = epochMillis(startAt);
            long endMs = epochMillis(endAt);
            long elapsedMs = startMs > 0 && endMs >= startMs ? endMs - startMs : 0L;
            String message = longestRecordedMessage();
            PhaseLatencyStats stats = slowestStage == null ? null : phaseStats.get(slowestStage);
            return RoundSummary.builder()
                    .roundId(roundId)
                    .startAt(startAt)
                    .endAt(endAt)
                    .windowId(windowId)
                    .windowRole(windowRole)
                    .taskCode(taskCode)
                    .taskName(taskName)
                    .roundType(displayTaskNameFromCode(taskCode, taskName) + "一轮")
                    .status(status)
                    .resultCode(resultCode)
                    .elapsedMs(elapsedMs)
                    .slowestStage(slowestStage)
                    .slowestStageP95Ms(stats == null ? null : stats.p95Ms())
                    .slowestStageSampleCount(stats == null ? null : stats.count())
                    .hadMaintenance(hadMaintenance || isMaintenanceStageName(slowestStage))
                    .hadSummonSkillCleanup(hadSummonSkillCleanup)
                    .message(message)
                    .build();
        }

        private String longestRecordedMessage() {
            if (slowestStage == null || slowestStageMs < 0L) {
                return "-";
            }
            return displayPhase(slowestStage) + " " + formatDuration(slowestStageMs);
        }
    }

    private record PhaseLatencyStats(int count, long p95Ms) {
        private static PhaseLatencyStats from(List<Long> values) {
            if (values == null || values.isEmpty()) {
                return new PhaseLatencyStats(0, 0L);
            }
            List<Long> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int index = (int) Math.ceil(sorted.size() * 0.95d) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            return new PhaseLatencyStats(sorted.size(), sorted.get(index));
        }
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
