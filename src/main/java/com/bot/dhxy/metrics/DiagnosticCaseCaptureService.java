package com.bot.dhxy.metrics;

import com.bot.dhxy.auth.DeviceFingerprintService;
import com.bot.dhxy.auth.LicenseAuthService;
import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Writes a bounded local diagnostic case for failed or explicitly captured automation events.
 *
 * <p>The first version is intentionally local-only. It packages enough structured metrics and raw
 * console context for later manual debugging, while keeping capture best-effort so a broken disk,
 * malformed log line, or oversized payload never changes task execution.</p>
 */
@Slf4j
@Service
public class DiagnosticCaseCaptureService {

    private static final String SCHEMA_VERSION = "1";
    private static final int CONSOLE_LINE_LIMIT = 500;
    private static final int CONSOLE_CHAR_LIMIT = 250_000;
    private static final int METRIC_EVENT_LIMIT = 160;
    private static final long NORMAL_TARGET_BYTES = 500_000L;
    private static final long MULTI_WINDOW_TARGET_BYTES = 1_500_000L;
    private static final long HARD_CAP_BYTES = 2_000_000L;
    private static final Duration RELATED_EVENT_MIN_WINDOW = Duration.ofMinutes(5);
    private static final Duration RELATED_EVENT_MAX_WINDOW = Duration.ofMinutes(20);
    private static final Duration CONSOLE_MIN_BEFORE = Duration.ofMinutes(3);
    private static final Duration CONSOLE_MAX_BEFORE = Duration.ofMinutes(15);
    private static final Duration CONSOLE_AFTER = Duration.ofSeconds(5);
    private static final Duration DEDUPE_TTL = Duration.ofMinutes(10);
    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
    private static final DateTimeFormatter CASE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.ROOT);
    private static final DateTimeFormatter CASE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path casesRoot;
    private final Path consoleLogPath;
    private final boolean enabled;
    private final String machineHash;
    private final String appId;
    private final String appVersion;
    private final Supplier<String> licenseIdSupplier;
    private final DiagnosticCaseUploaderService uploaderService;
    private final ConcurrentMap<String, Long> capturedFingerprints = new ConcurrentHashMap<>();

    public DiagnosticCaseCaptureService() {
        this(Path.of("logs", "cases"), Path.of("logs", "dhxy-console.log"), localMachineHash(), "dhxy", "", () -> "",
                DiagnosticCaseUploaderService.disabled(), true);
    }

    @Autowired
    public DiagnosticCaseCaptureService(DeviceFingerprintService deviceFingerprintService,
                                        LicenseAuthService licenseAuthService,
                                        DiagnosticCaseUploaderService uploaderService) {
        this(Path.of("logs", "cases"),
                Path.of("logs", "dhxy-console.log"),
                deviceFingerprintService == null ? "" : deviceFingerprintService.getDeviceFingerprint(),
                licenseAuthService == null ? "" : licenseAuthService.getAppId(),
                licenseAuthService == null ? "" : licenseAuthService.getAppVersion(),
                licenseAuthService == null ? () -> "" : licenseAuthService::getLatestVerifiedLicenseCode,
                uploaderService == null ? DiagnosticCaseUploaderService.disabled() : uploaderService,
                true);
    }

    public DiagnosticCaseCaptureService(Path casesRoot, Path consoleLogPath) {
        this(casesRoot, consoleLogPath, localMachineHash(), "dhxy", "", () -> "",
                DiagnosticCaseUploaderService.disabled(), true);
    }

    public DiagnosticCaseCaptureService(Path casesRoot,
                                        Path consoleLogPath,
                                        String machineHash,
                                        String appId,
                                        String appVersion,
                                        Supplier<String> licenseIdSupplier,
                                        DiagnosticCaseUploaderService uploaderService) {
        this(casesRoot, consoleLogPath, machineHash, appId, appVersion, licenseIdSupplier, uploaderService, true);
    }

    private DiagnosticCaseCaptureService(Path casesRoot,
                                         Path consoleLogPath,
                                         String machineHash,
                                         String appId,
                                         String appVersion,
                                         Supplier<String> licenseIdSupplier,
                                         DiagnosticCaseUploaderService uploaderService,
                                         boolean enabled) {
        this.casesRoot = casesRoot == null ? Path.of("logs", "cases") : casesRoot.toAbsolutePath().normalize();
        this.consoleLogPath = consoleLogPath == null
                ? Path.of("logs", "dhxy-console.log").toAbsolutePath().normalize()
                : consoleLogPath.toAbsolutePath().normalize();
        this.enabled = enabled;
        this.machineHash = safe(machineHash);
        this.appId = safe(appId);
        this.appVersion = safe(appVersion);
        this.licenseIdSupplier = licenseIdSupplier == null ? () -> "" : licenseIdSupplier;
        this.uploaderService = uploaderService == null ? DiagnosticCaseUploaderService.disabled() : uploaderService;
    }

    static DiagnosticCaseCaptureService disabled() {
        return new DiagnosticCaseCaptureService(Path.of("logs", "cases"), Path.of("logs", "dhxy-console.log"),
                "", "", "", () -> "", DiagnosticCaseUploaderService.disabled(), false);
    }

    /**
     * Capture one diagnostic case when the trigger event is failure-like or explicitly marked.
     *
     * @param trigger completed metric event that may become the root case trigger.
     * @param recentEvents recent in-memory metric events, ideally including {@code trigger}.
     * @return path of the written case, or empty when not captured / duplicate / best-effort failure.
     */
    public Optional<Path> captureIfNeeded(AutomationMetricEvent trigger, List<AutomationMetricEvent> recentEvents) {
        if (!enabled || trigger == null || !shouldCapture(trigger)) {
            return Optional.empty();
        }
        String fingerprint = incidentFingerprint(trigger);
        long now = System.currentTimeMillis();
        pruneOldFingerprints(now);
        Long previous = capturedFingerprints.putIfAbsent(fingerprint, now);
        if (previous != null && now - previous < DEDUPE_TTL.toMillis()) {
            return Optional.empty();
        }
        try {
            LocalDateTime triggerTime = eventLocalTime(trigger).orElse(LocalDateTime.now());
            List<AutomationMetricEvent> relatedMetrics = relatedMetricEvents(trigger, recentEvents, triggerTime);
            List<Map<String, Object>> relatedWindows = relatedWindows(trigger, relatedMetrics);
            List<String> consoleLines = readConsoleExcerpt(trigger, triggerTime);
            Map<String, Object> document = buildDocument(trigger, relatedMetrics, relatedWindows, consoleLines, triggerTime);
            Path casePath = writeCase(document, trigger, triggerTime);
            uploaderService.enqueueUpload(casePath);
            log.warn("[diagnostic-case] local case captured: path={} caseId={} triggerType={} status={} task={} window={}",
                    casePath, document.get("caseId"), trigger.getEventType(), trigger.getStatus(),
                    trigger.getTaskCode(), trigger.getWindowId());
            return Optional.of(casePath);
        } catch (Exception e) {
            capturedFingerprints.remove(fingerprint);
            log.warn("[diagnostic-case] local case capture failed: triggerType={} status={} task={} window={} reason={}",
                    trigger.getEventType(), trigger.getStatus(), trigger.getTaskCode(), trigger.getWindowId(),
                    e.getMessage(), e);
            return Optional.empty();
        }
    }

    private boolean shouldCapture(AutomationMetricEvent event) {
        String capture = event.getAttributes().get("caseCapture");
        if ("always".equalsIgnoreCase(capture) || "true".equalsIgnoreCase(capture)) {
            return true;
        }
        AutomationMetricStatus status = event.getStatus();
        boolean failed = status == AutomationMetricStatus.FAILED || status == AutomationMetricStatus.FATAL;
        if (!failed) {
            return false;
        }
        AutomationMetricEventType type = event.getEventType();
        return type == AutomationMetricEventType.TASK_ROUND_FINISHED
                || type == AutomationMetricEventType.TASK_FINISHED
                || type == AutomationMetricEventType.TASK_TRANSACTION
                || type == AutomationMetricEventType.XIULUO_FAILURE_CASE;
    }

    private Map<String, Object> buildDocument(AutomationMetricEvent trigger,
                                              List<AutomationMetricEvent> relatedMetrics,
                                              List<Map<String, Object>> relatedWindows,
                                              List<String> consoleLines,
                                              LocalDateTime triggerTime) {
        Map<String, Object> root = new LinkedHashMap<>();
        String caseId = caseId(trigger, triggerTime);
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("caseId", caseId);
        root.put("createdAt", OffsetDateTime.now().toString());
        root.put("caseType", caseType(trigger));
        root.put("severity", severity(trigger));
        String licenseId = currentLicenseId();
        if (!licenseId.isBlank()) {
            root.put("licenseId", licenseId);
        }
        root.put("trigger", triggerSummary(trigger));
        root.put("app", appSummary(trigger, licenseId));
        root.put("task", taskSummary(trigger));
        root.put("window", windowSummary(trigger));
        root.put("failure", failureSummary(trigger));
        root.put("runtimeSnapshots", runtimeSnapshot(trigger));
        root.put("timeline", timeline(relatedMetrics));
        root.put("metrics", metricMaps(relatedMetrics));
        root.put("consoleLogExcerpt", consoleExcerpt(consoleLines));
        root.put("relatedWindows", relatedWindows);
        root.put("diagnosticHints", diagnosticHints(trigger, relatedMetrics, consoleLines));
        root.put("sizePolicy", sizePolicy(!relatedWindows.isEmpty()));
        root.put("upload", uploadStatus(licenseId));
        return root;
    }

    private Path writeCase(Map<String, Object> document,
                           AutomationMetricEvent trigger,
                           LocalDateTime triggerTime) throws Exception {
        String dateDir = triggerTime.format(CASE_DATE_FORMATTER);
        Path dir = casesRoot.resolve(dateDir).normalize();
        Files.createDirectories(dir);
        Path path = dir.resolve(caseId(trigger, triggerTime) + ".case.json").normalize();
        String json = objectMapper.writeValueAsString(document);
        if (json.getBytes(StandardCharsets.UTF_8).length > HARD_CAP_BYTES) {
            @SuppressWarnings("unchecked")
            Map<String, Object> console = (Map<String, Object>) document.get("consoleLogExcerpt");
            console.put("truncatedByHardCap", true);
            console.put("lines", List.of());
            json = objectMapper.writeValueAsString(document);
        }
        Files.writeString(path, json, StandardCharsets.UTF_8);
        return path;
    }

    private List<AutomationMetricEvent> relatedMetricEvents(AutomationMetricEvent trigger,
                                                            List<AutomationMetricEvent> recentEvents,
                                                            LocalDateTime triggerTime) {
        if (recentEvents == null || recentEvents.isEmpty()) {
            return List.of(trigger);
        }
        String triggerWindow = safe(trigger.getWindowId());
        String triggerRun = safe(firstNonBlank(trigger.getRunId(), trigger.getAttributes().get("roundId")));
        String triggerTeam = safe(trigger.getAttributes().get("teamKey"));
        LocalDateTime start = triggerTime.minus(relatedEventWindow(trigger));
        LocalDateTime end = triggerTime.plus(CONSOLE_AFTER);
        List<AutomationMetricEvent> related = new ArrayList<>();
        for (AutomationMetricEvent event : recentEvents) {
            Optional<LocalDateTime> eventTime = eventLocalTime(event);
            if (eventTime.isPresent() && (eventTime.get().isBefore(start) || eventTime.get().isAfter(end))) {
                continue;
            }
            String eventRun = safe(firstNonBlank(event.getRunId(), event.getAttributes().get("roundId")));
            String eventTeam = safe(event.getAttributes().get("teamKey"));
            boolean sameWindow = triggerWindow.equals(safe(event.getWindowId()));
            boolean sameRun = !triggerRun.isBlank() && triggerRun.equals(eventRun);
            boolean sameTeam = !triggerTeam.isBlank() && triggerTeam.equals(eventTeam);
            if (sameWindow || sameRun || sameTeam) {
                related.add(event);
            }
        }
        if (related.stream().noneMatch(event -> safe(event.getEventId()).equals(safe(trigger.getEventId())))) {
            related.add(trigger);
        }
        related.sort(Comparator.comparing(event -> eventLocalTime(event).orElse(LocalDateTime.MIN)));
        if (related.size() <= METRIC_EVENT_LIMIT) {
            return List.copyOf(related);
        }
        return List.copyOf(related.subList(Math.max(0, related.size() - METRIC_EVENT_LIMIT), related.size()));
    }

    private List<Map<String, Object>> relatedWindows(AutomationMetricEvent trigger,
                                                     List<AutomationMetricEvent> relatedMetrics) {
        String triggerWindow = safe(trigger.getWindowId());
        Map<String, WindowEvidenceAccumulator> accumulators = new LinkedHashMap<>();
        for (AutomationMetricEvent event : relatedMetrics) {
            String windowId = safe(event.getWindowId());
            if (windowId.isBlank() || windowId.equals(triggerWindow)) {
                continue;
            }
            accumulators.computeIfAbsent(windowId, WindowEvidenceAccumulator::new).add(event);
        }
        List<Map<String, Object>> windows = new ArrayList<>();
        for (WindowEvidenceAccumulator accumulator : accumulators.values()) {
            windows.add(accumulator.toMap());
        }
        return windows;
    }

    private List<String> readConsoleExcerpt(AutomationMetricEvent trigger, LocalDateTime triggerTime) {
        if (!Files.exists(consoleLogPath)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(consoleLogPath, StandardCharsets.UTF_8);
            LocalDateTime start = triggerTime.minus(consoleBeforeWindow(trigger));
            LocalDateTime end = triggerTime.plus(CONSOLE_AFTER);
            List<String> selected = new ArrayList<>();
            boolean sawTimestampedLine = false;
            for (String line : lines) {
                Optional<LocalDateTime> lineTime = logLineTime(line);
                if (lineTime.isPresent()) {
                    sawTimestampedLine = true;
                    LocalDateTime time = lineTime.get();
                    if (!time.isBefore(start) && !time.isAfter(end)) {
                        selected.add(line);
                    }
                }
            }
            if (selected.isEmpty() && !sawTimestampedLine) {
                selected = tail(lines, Math.min(CONSOLE_LINE_LIMIT, 300));
            }
            return trimConsole(selected);
        } catch (Exception e) {
            log.debug("[diagnostic-case] console excerpt read failed: path={} reason={}",
                    consoleLogPath, e.getMessage());
            return List.of();
        }
    }

    private List<String> trimConsole(List<String> lines) {
        Deque<String> kept = new ArrayDeque<>();
        int chars = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            int nextChars = chars + line.length() + 1;
            if (kept.size() >= CONSOLE_LINE_LIMIT || nextChars > CONSOLE_CHAR_LIMIT) {
                break;
            }
            kept.addFirst(line);
            chars = nextChars;
        }
        return List.copyOf(kept);
    }

    private Duration relatedEventWindow(AutomationMetricEvent trigger) {
        long elapsedMs = trigger == null || trigger.getElapsedMs() == null ? 0L : trigger.getElapsedMs();
        long desiredMs = Math.max(RELATED_EVENT_MIN_WINDOW.toMillis(), elapsedMs + 60_000L);
        return Duration.ofMillis(Math.min(desiredMs, RELATED_EVENT_MAX_WINDOW.toMillis()));
    }

    private Duration consoleBeforeWindow(AutomationMetricEvent trigger) {
        long elapsedMs = trigger == null || trigger.getElapsedMs() == null ? 0L : trigger.getElapsedMs();
        long desiredMs = Math.max(CONSOLE_MIN_BEFORE.toMillis(), elapsedMs + 30_000L);
        return Duration.ofMillis(Math.min(desiredMs, CONSOLE_MAX_BEFORE.toMillis()));
    }

    private List<String> tail(List<String> lines, int maxLines) {
        if (lines.size() <= maxLines) {
            return List.copyOf(lines);
        }
        return List.copyOf(lines.subList(lines.size() - maxLines, lines.size()));
    }

    private Optional<LocalDateTime> logLineTime(String line) {
        if (line == null || line.length() < 23) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(line.substring(0, 23), LOG_TIME_FORMATTER));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> eventLocalTime(AutomationMetricEvent event) {
        String timestamp = event == null ? null : event.getTimestamp();
        if (timestamp == null || timestamp.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
            try {
                return Optional.of(LocalDateTime.parse(timestamp));
            } catch (DateTimeParseException ignoredAgain) {
                return Optional.empty();
            }
        }
    }

    private Map<String, Object> triggerSummary(AutomationMetricEvent event) {
        Map<String, Object> trigger = new LinkedHashMap<>();
        trigger.put("eventId", event.getEventId());
        trigger.put("timestamp", event.getTimestamp());
        trigger.put("eventType", name(event.getEventType()));
        trigger.put("status", name(event.getStatus()));
        trigger.put("phase", event.getPhase());
        trigger.put("reason", firstNonBlank(event.getErrorCode(), event.getAttributes().get("resultCode"), event.getMessage()));
        trigger.put("elapsedMs", event.getElapsedMs());
        trigger.put("attributes", event.getAttributes());
        return trigger;
    }

    private Map<String, Object> appSummary(AutomationMetricEvent event, String licenseId) {
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("appId", appId);
        app.put("version", firstNonBlank(event.getAppVersion(), appVersion));
        app.put("appVersion", firstNonBlank(event.getAppVersion(), appVersion));
        app.put("licenseId", licenseId);
        app.put("sessionId", event.getSessionId());
        app.put("timezone", ZoneId.systemDefault().toString());
        return app;
    }

    private Map<String, Object> taskSummary(AutomationMetricEvent event) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskCode", event.getTaskCode());
        task.put("taskName", event.getTaskName());
        task.put("roundId", firstNonBlank(event.getRunId(), event.getAttributes().get("roundId")));
        task.put("roundNumber", event.getAttributes().get("roundNumber"));
        task.put("roundType", event.getAttributes().get("roundType"));
        task.put("teamKey", event.getAttributes().get("teamKey"));
        task.put("phase", event.getPhase());
        task.put("source", event.getAttributes().get("source"));
        task.put("resultCode", firstNonBlank(event.getAttributes().get("resultCode"), event.getErrorCode()));
        return task;
    }

    private Map<String, Object> windowSummary(AutomationMetricEvent event) {
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("windowId", event.getWindowId());
        window.put("role", event.getWindowRole());
        window.put("hwnd", event.getNativeWindowHandle());
        window.put("playerName", event.getAttributes().get("playerName"));
        window.put("playerId", event.getAttributes().get("playerId"));
        window.put("title", event.getAttributes().get("title"));
        return window;
    }

    private Map<String, Object> failureSummary(AutomationMetricEvent event) {
        Map<String, Object> failure = new LinkedHashMap<>();
        failure.put("message", event.getMessage());
        failure.put("errorCode", firstNonBlank(event.getErrorCode(), event.getAttributes().get("resultCode")));
        failure.put("exceptionClass", event.getAttributes().get("exceptionClass"));
        failure.put("stackTraceTop", event.getAttributes().getOrDefault("stackTraceTop", ""));
        return failure;
    }

    private Map<String, Object> runtimeSnapshot(AutomationMetricEvent event) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("machineHash", machineHash);
        snapshot.put("environment", environment);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("actionState", safe(event.getAttributes().get("actionState")));
        attributes.put("combatState", safe(event.getAttributes().get("combatState")));
        attributes.put("currentMap", safe(event.getAttributes().get("currentMap")));
        attributes.put("currentX", safe(event.getAttributes().get("currentX")));
        attributes.put("currentY", safe(event.getAttributes().get("currentY")));
        attributes.put("pathingState", safe(event.getAttributes().get("pathingState")));
        attributes.put("pathingTargetMap", safe(event.getAttributes().get("pathingTargetMap")));
        attributes.put("pathingIntentId", safe(event.getAttributes().get("pathingIntentId")));
        attributes.put("preparedOperation", safe(event.getAttributes().get("preparedOperation")));
        attributes.put("visibleDialog", safe(event.getAttributes().get("visibleDialog")));
        attributes.put("identityEpoch", safe(event.getAttributes().get("identityEpoch")));
        snapshot.put("fromMetricAttributes", attributes);
        return snapshot;
    }

    private Map<String, Object> uploadStatus(String licenseId) {
        Map<String, Object> upload = new LinkedHashMap<>();
        upload.put("attempts", 0);
        if (licenseId == null || licenseId.isBlank()) {
            upload.put("eligible", false);
            upload.put("blocker", "missing-license-id");
            upload.put("status", "LOCAL_ONLY");
            return upload;
        }
        upload.put("eligible", true);
        upload.put("blocker", "");
        upload.put("status", "PENDING");
        return upload;
    }

    private String currentLicenseId() {
        try {
            return safe(licenseIdSupplier.get());
        } catch (Exception e) {
            return "";
        }
    }

    private static String localMachineHash() {
        return new DeviceFingerprintService().getDeviceFingerprint();
    }

    private List<Map<String, Object>> timeline(List<AutomationMetricEvent> events) {
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (AutomationMetricEvent event : events) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("timestamp", event.getTimestamp());
            item.put("windowId", event.getWindowId());
            item.put("role", event.getWindowRole());
            item.put("eventType", name(event.getEventType()));
            item.put("status", name(event.getStatus()));
            item.put("taskCode", event.getTaskCode());
            item.put("phase", event.getPhase());
            item.put("elapsedMs", event.getElapsedMs());
            item.put("errorCode", event.getErrorCode());
            item.put("message", event.getMessage());
            timeline.add(item);
        }
        return timeline;
    }

    private List<Map<String, Object>> metricMaps(List<AutomationMetricEvent> events) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        for (AutomationMetricEvent event : events) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(event, Map.class);
            metrics.add(map);
        }
        return metrics;
    }

    private Map<String, Object> consoleExcerpt(List<String> lines) {
        Map<String, Object> excerpt = new LinkedHashMap<>();
        excerpt.put("path", consoleLogPath.toString());
        excerpt.put("lineCount", lines.size());
        excerpt.put("maxLines", CONSOLE_LINE_LIMIT);
        excerpt.put("maxChars", CONSOLE_CHAR_LIMIT);
        excerpt.put("truncated", lines.size() >= CONSOLE_LINE_LIMIT);
        excerpt.put("unavailableReason", lines.isEmpty() ? "no-console-lines-in-trigger-time-window" : "");
        excerpt.put("lines", lines);
        return excerpt;
    }

    private List<String> diagnosticHints(AutomationMetricEvent trigger,
                                         List<AutomationMetricEvent> relatedMetrics,
                                         List<String> consoleLines) {
        Set<String> hints = new LinkedHashSet<>();
        String combined = String.join("\n", rootWindowConsoleLines(trigger, consoleLines)).toLowerCase(Locale.ROOT);
        if (combined.contains("consumeprepared") && combined.contains("mismatch")) {
            hints.add("prepared-action-mismatch");
        }
        if (combined.contains("identity drift")) {
            hints.add("window-identity-drift");
        }
        if (combined.contains("stopped_away") || combined.contains("stopped-away")) {
            hints.add("pathing-stopped-away");
        }
        if (combined.contains("watchdog timeout") || safe(trigger.getErrorCode()).toLowerCase(Locale.ROOT).contains("timeout")) {
            hints.add("watchdog-or-timeout");
        }
        if (consoleLines.isEmpty()) {
            hints.add("console-excerpt-missing-for-trigger-time");
        }
        if (relatedMetrics.stream().anyMatch(event -> "MEMBER".equalsIgnoreCase(safe(event.getWindowRole())))) {
            hints.add("multi-window-related-evidence");
        }
        return List.copyOf(hints);
    }

    private List<String> rootWindowConsoleLines(AutomationMetricEvent trigger, List<String> consoleLines) {
        if (consoleLines == null || consoleLines.isEmpty()) {
            return List.of();
        }
        String windowId = safe(trigger.getWindowId());
        String hwnd = safe(trigger.getNativeWindowHandle());
        if (windowId.isBlank() && hwnd.isBlank()) {
            return consoleLines;
        }
        List<String> rootLines = consoleLines.stream()
                .filter(line -> (!windowId.isBlank() && line.contains(windowId))
                        || (!hwnd.isBlank() && line.contains(hwnd)))
                .toList();
        return rootLines.isEmpty() ? consoleLines : rootLines;
    }

    private Map<String, Object> sizePolicy(boolean multiWindow) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("normalTargetBytes", NORMAL_TARGET_BYTES);
        policy.put("multiWindowTargetBytes", MULTI_WINDOW_TARGET_BYTES);
        policy.put("hardCapBytes", HARD_CAP_BYTES);
        policy.put("activeTargetBytes", multiWindow ? MULTI_WINDOW_TARGET_BYTES : NORMAL_TARGET_BYTES);
        return policy;
    }

    private String incidentFingerprint(AutomationMetricEvent event) {
        String shared = firstNonBlank(event.getAttributes().get("teamKey"), event.getAttributes().get("roundId"), event.getRunId());
        if (shared != null && !shared.isBlank()) {
            return String.join("|", safe(event.getSessionId()), safe(shared));
        }
        LocalDateTime time = eventLocalTime(event).orElse(LocalDateTime.now());
        long minuteBucket = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 60_000L;
        return String.join("|", safe(event.getSessionId()), safe(event.getTaskCode()), safe(event.getWindowId()),
                safe(event.getPhase()), safe(event.getErrorCode()), Long.toString(minuteBucket));
    }

    private void pruneOldFingerprints(long now) {
        long cutoff = now - DEDUPE_TTL.toMillis();
        capturedFingerprints.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    private String caseId(AutomationMetricEvent event, LocalDateTime time) {
        return String.join("_",
                time.format(CASE_TIME_FORMATTER),
                safeFile(firstNonBlank(event.getTaskCode(), "task")),
                safeFile(firstNonBlank(event.getAttributes().get("roundId"), event.getRunId(), "round")),
                safeFile(firstNonBlank(event.getErrorCode(), event.getAttributes().get("resultCode"), name(event.getStatus()))),
                safeFile(firstNonBlank(event.getWindowId(), "window")));
    }

    private String caseType(AutomationMetricEvent event) {
        return String.join("_",
                safe(firstNonBlank(event.getTaskCode(), "task")).toUpperCase(Locale.ROOT),
                safe(firstNonBlank(event.getErrorCode(), event.getAttributes().get("resultCode"), name(event.getStatus())))
                        .toUpperCase(Locale.ROOT));
    }

    private String severity(AutomationMetricEvent event) {
        if (event.getStatus() == AutomationMetricStatus.FATAL) {
            return "FATAL";
        }
        if (event.getStatus() == AutomationMetricStatus.FAILED) {
            return "ERROR";
        }
        return "INFO";
    }

    private String safeFile(String value) {
        String text = safe(value).replaceAll("[^a-zA-Z0-9._-]+", "-");
        if (text.isBlank()) {
            return "unknown";
        }
        return text.length() > 80 ? text.substring(0, 80) : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class WindowEvidenceAccumulator {
        private final String windowId;
        private final Set<String> signals = new LinkedHashSet<>();
        private int events;
        private AutomationMetricEvent lastEvent;

        private WindowEvidenceAccumulator(String windowId) {
            this.windowId = windowId;
        }

        private void add(AutomationMetricEvent event) {
            events++;
            lastEvent = event;
            if (event.getPhase() != null && !event.getPhase().isBlank()) {
                signals.add(event.getPhase());
            }
            if (event.getStatus() != null) {
                signals.add(event.getStatus().name());
            }
            if (event.getMessage() != null && !event.getMessage().isBlank()) {
                signals.add(event.getMessage());
            }
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("windowId", windowId);
            map.put("role", lastEvent == null ? null : lastEvent.getWindowRole());
            map.put("hwnd", lastEvent == null ? null : lastEvent.getNativeWindowHandle());
            map.put("taskCode", lastEvent == null ? null : lastEvent.getTaskCode());
            map.put("taskName", lastEvent == null ? null : lastEvent.getTaskName());
            map.put("lastPhase", lastEvent == null ? null : lastEvent.getPhase());
            map.put("lastStatus", lastEvent == null || lastEvent.getStatus() == null ? null : lastEvent.getStatus().name());
            map.put("lastTimestamp", lastEvent == null ? null : lastEvent.getTimestamp());
            map.put("eventCount", events);
            map.put("signals", List.copyOf(signals));
            return map;
        }
    }
}
