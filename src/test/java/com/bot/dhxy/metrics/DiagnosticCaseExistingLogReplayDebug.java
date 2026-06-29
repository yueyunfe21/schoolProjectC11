package com.bot.dhxy.metrics;

import com.bot.dhxy.model.metrics.AutomationMetricEvent;
import com.bot.dhxy.model.metrics.AutomationMetricEventType;
import com.bot.dhxy.model.metrics.AutomationMetricStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Replays the local diagnostic case generator against the existing metrics and console logs.
 */
public class DiagnosticCaseExistingLogReplayDebug {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int RECENT_LIMIT = 5000;
    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        Path metricsPath = Path.of("logs", "automation-metrics.jsonl").toAbsolutePath().normalize();
        Path consolePath = Path.of("logs", "dhxy-console.log").toAbsolutePath().normalize();
        Path casesRoot = Path.of("logs", "cases-replay").toAbsolutePath().normalize();

        TimeRange consoleRange = consoleTimeRange(consolePath);
        Candidate candidate = findCandidate(metricsPath, consoleRange);
        DiagnosticCaseCaptureService service = new DiagnosticCaseCaptureService(casesRoot, consolePath);
        Optional<Path> path = service.captureIfNeeded(candidate.event(), candidate.recentEvents());
        if (path.isEmpty()) {
            throw new AssertionError("diagnostic case replay did not create a case");
        }

        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(path.get(), StandardCharsets.UTF_8));
        long size = Files.size(path.get());
        int timeline = root.path("timeline").size();
        int metrics = root.path("metrics").size();
        int lines = root.path("consoleLogExcerpt").path("lines").size();
        int related = root.path("relatedWindows").size();
        String hints = root.path("diagnosticHints").toString();

        System.out.println("casePath=" + path.get());
        System.out.println("caseSizeBytes=" + size);
        System.out.println("caseType=" + root.path("caseType").asText());
        System.out.println("trigger=" + root.path("trigger").path("eventType").asText()
                + "/" + root.path("trigger").path("status").asText()
                + " phase=" + root.path("trigger").path("phase").asText());
        System.out.println("task=" + root.path("task").path("taskCode").asText()
                + " round=" + root.path("task").path("roundId").asText());
        System.out.println("window=" + root.path("window").path("windowId").asText()
                + " role=" + root.path("window").path("role").asText());
        System.out.println("timelineCount=" + timeline);
        System.out.println("metricCount=" + metrics);
        System.out.println("consoleLineCount=" + lines);
        System.out.println("relatedWindowCount=" + related);
        System.out.println("diagnosticHints=" + hints);

        if (timeline == 0 || metrics == 0 || lines == 0) {
            throw new AssertionError("case lacks enough evidence: timeline=" + timeline
                    + " metrics=" + metrics + " consoleLines=" + lines);
        }
    }

    private static Candidate findCandidate(Path metricsPath, TimeRange consoleRange) throws Exception {
        ArrayDeque<AutomationMetricEvent> recent = new ArrayDeque<>();
        Candidate latestFailure = null;
        Candidate latestRound = null;
        Candidate latestAny = null;
        Candidate latestConsoleAlignedFailure = null;
        Candidate latestConsoleAlignedRound = null;
        Candidate latestConsoleAlignedAny = null;
        try (var lines = Files.lines(metricsPath, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                AutomationMetricEvent event = parseEvent(line);
                recent.addLast(event);
                while (recent.size() > RECENT_LIMIT) {
                    recent.removeFirst();
                }
                if (event.getEventType() == AutomationMetricEventType.TASK_ROUND_FINISHED) {
                    latestRound = new Candidate(forceCapture(event), new ArrayList<>(recent));
                    if (consoleRange.contains(event)) {
                        latestConsoleAlignedRound = latestRound;
                    }
                }
                if (event.getEventType() == AutomationMetricEventType.TASK_ROUND_FINISHED
                        || event.getEventType() == AutomationMetricEventType.TASK_FINISHED
                        || event.getEventType() == AutomationMetricEventType.TASK_TRANSACTION) {
                    latestAny = new Candidate(forceCapture(event), new ArrayList<>(recent));
                    if (consoleRange.contains(event)) {
                        latestConsoleAlignedAny = latestAny;
                    }
                }
                if (event.getStatus() == AutomationMetricStatus.FAILED
                        || event.getStatus() == AutomationMetricStatus.FATAL) {
                    latestFailure = new Candidate(event, new ArrayList<>(recent));
                    if (consoleRange.contains(event)) {
                        latestConsoleAlignedFailure = latestFailure;
                    }
                }
            }
        }
        if (latestConsoleAlignedFailure != null) {
            return latestConsoleAlignedFailure;
        }
        if (latestConsoleAlignedRound != null) {
            return latestConsoleAlignedRound;
        }
        if (latestConsoleAlignedAny != null) {
            return latestConsoleAlignedAny;
        }
        if (latestFailure != null) {
            return latestFailure;
        }
        if (latestRound != null) {
            return latestRound;
        }
        if (latestAny != null) {
            return latestAny;
        }
        throw new AssertionError("no metric event found for replay");
    }

    private static TimeRange consoleTimeRange(Path consolePath) throws Exception {
        LocalDateTime first = null;
        LocalDateTime last = null;
        try (var lines = Files.lines(consolePath, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                Optional<LocalDateTime> parsed = logLineTime(line);
                if (parsed.isEmpty()) {
                    continue;
                }
                if (first == null || parsed.get().isBefore(first)) {
                    first = parsed.get();
                }
                if (last == null || parsed.get().isAfter(last)) {
                    last = parsed.get();
                }
            }
        }
        return new TimeRange(first, last);
    }

    private static Optional<LocalDateTime> logLineTime(String line) {
        if (line == null || line.length() < 23) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(line.substring(0, 23), LOG_TIME_FORMATTER));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDateTime> eventTime(AutomationMetricEvent event) {
        if (event == null || event.getTimestamp() == null || event.getTimestamp().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(event.getTimestamp())
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime());
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static AutomationMetricEvent forceCapture(AutomationMetricEvent event) {
        Map<String, String> attributes = new LinkedHashMap<>(event.getAttributes());
        attributes.put("caseCapture", "always");
        attributes.putIfAbsent("resultCode", event.getErrorCode() == null ? "REPLAY_SAMPLE" : event.getErrorCode());
        return event.toBuilder()
                .attributes(Map.copyOf(attributes))
                .build();
    }

    private static AutomationMetricEvent parseEvent(String line) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(line);
        Map<String, String> attributes = new LinkedHashMap<>();
        JsonNode attributeNode = root.path("attributes");
        if (attributeNode.isObject()) {
            attributeNode.fields().forEachRemaining(entry -> attributes.put(entry.getKey(), entry.getValue().asText("")));
        }
        return AutomationMetricEvent.builder()
                .schemaVersion(text(root, "schemaVersion"))
                .timestamp(text(root, "timestamp"))
                .eventId(text(root, "eventId"))
                .sessionId(text(root, "sessionId"))
                .runId(text(root, "runId"))
                .customerId(text(root, "customerId"))
                .licenseId(text(root, "licenseId"))
                .appVersion(text(root, "appVersion"))
                .windowId(text(root, "windowId"))
                .windowRole(text(root, "windowRole"))
                .nativeWindowHandle(text(root, "nativeWindowHandle"))
                .taskCode(text(root, "taskCode"))
                .taskName(text(root, "taskName"))
                .phase(text(root, "phase"))
                .eventType(enumValue(root, "eventType", AutomationMetricEventType.SYSTEM_WARNING))
                .status(enumValue(root, "status", AutomationMetricStatus.INFO))
                .elapsedMs(root.path("elapsedMs").isNumber() ? root.path("elapsedMs").asLong() : null)
                .errorCode(text(root, "errorCode"))
                .caseId(text(root, "caseId"))
                .message(text(root, "message"))
                .attributes(Map.copyOf(attributes))
                .build();
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static <E extends Enum<E>> E enumValue(JsonNode root, String field, E fallback) {
        String value = text(root, field);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private record Candidate(AutomationMetricEvent event, List<AutomationMetricEvent> recentEvents) {
    }

    private record TimeRange(LocalDateTime first, LocalDateTime last) {
        private boolean contains(AutomationMetricEvent event) {
            if (first == null || last == null) {
                return false;
            }
            Optional<LocalDateTime> time = eventTime(event);
            return time.isPresent() && !time.get().isBefore(first) && !time.get().isAfter(last);
        }
    }
}
