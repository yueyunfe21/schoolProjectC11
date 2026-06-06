package com.bot.dhxy.window.diagnostics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight per-window counters for focus reduction diagnostics.
 */
@Slf4j
@Service
public class WindowInteractionMetricsService {

    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long DASHBOARD_WRITE_INTERVAL_MS = 1000;

    private final ConcurrentMap<String, Metrics> metricsByWindow = new ConcurrentHashMap<>();
    private final AtomicLong lastDashboardWriteMillis = new AtomicLong();
    private final Path dashboardPath = Path.of("logs", "interaction-metrics-dashboard.html")
            .toAbsolutePath()
            .normalize();

    public void recordFocus(String windowId, String actionName, boolean success) {
        Metrics metrics = metrics(windowId);
        long focusTotal = metrics.focusAttempts.incrementAndGet();
        if (success) {
            metrics.focusSuccess.incrementAndGet();
        } else {
            metrics.focusFailure.incrementAndGet();
        }
        log.info("Interaction metrics: windowId={} event=focus action={} success={} focusTotal={} focusSuccess={} focusFailure={} hwndCapture={} robotCapture={} captureFailure={} hwndKeyboardSuccess={} hwndKeyboardFailure={}",
                key(windowId), actionName, success, focusTotal, metrics.focusSuccess.get(), metrics.focusFailure.get(),
                metrics.hwndCaptureSuccess.get(), metrics.robotCaptureSuccess.get(), metrics.captureFailure.get(),
                metrics.hwndKeyboardSuccess.get(), metrics.hwndKeyboardFailure.get());
        writeDashboardThrottled();
    }

    public void recordCapture(String windowId, String provider, boolean success, String mode, String elementName) {
        Metrics metrics = metrics(windowId);
        String normalizedProvider = provider == null ? "UNKNOWN" : provider.toUpperCase(Locale.ROOT);
        if (success && normalizedProvider.startsWith("HWND")) {
            metrics.hwndCaptureSuccess.incrementAndGet();
        } else if (success && "ROBOT".equals(normalizedProvider)) {
            metrics.robotCaptureSuccess.incrementAndGet();
        } else if (!success) {
            metrics.captureFailure.incrementAndGet();
        }
        String message = "Interaction metrics: windowId={} event=capture provider={} success={} mode={} element={} focusTotal={} hwndCapture={} robotCapture={} captureFailure={} hwndKeyboardSuccess={} hwndKeyboardFailure={}";
        Object[] args = {
                key(windowId), normalizedProvider, success, mode, elementName,
                metrics.focusAttempts.get(), metrics.hwndCaptureSuccess.get(), metrics.robotCaptureSuccess.get(),
                metrics.captureFailure.get(), metrics.hwndKeyboardSuccess.get(), metrics.hwndKeyboardFailure.get()
        };
        if (!success || "ROBOT".equals(normalizedProvider)) {
            log.info(message, args);
        } else {
            log.debug(message, args);
        }
        writeDashboardThrottled();
    }

    public void recordHwndKeyboard(String windowId, String shortcut, boolean success) {
        Metrics metrics = metrics(windowId);
        if (success) {
            metrics.hwndKeyboardSuccess.incrementAndGet();
        } else {
            metrics.hwndKeyboardFailure.incrementAndGet();
        }
        log.info("Interaction metrics: windowId={} event=hwndKeyboard shortcut={} success={} focusTotal={} hwndCapture={} robotCapture={} captureFailure={} hwndKeyboardSuccess={} hwndKeyboardFailure={}",
                key(windowId), shortcut, success, metrics.focusAttempts.get(), metrics.hwndCaptureSuccess.get(),
                metrics.robotCaptureSuccess.get(), metrics.captureFailure.get(),
                metrics.hwndKeyboardSuccess.get(), metrics.hwndKeyboardFailure.get());
        writeDashboardThrottled();
    }

    public Path writeDashboardNow() {
        writeDashboard();
        return dashboardPath;
    }

    public Path getDashboardPath() {
        return dashboardPath;
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
            Files.writeString(dashboardPath, renderDashboardHtml(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("Interaction metrics dashboard write failed: path={} reason={}", dashboardPath, e.getMessage());
        }
    }

    private String renderDashboardHtml() {
        List<MetricsSnapshot> snapshots = snapshots();
        long totalFocus = snapshots.stream().mapToLong(MetricsSnapshot::focusAttempts).sum();
        long totalHwndCapture = snapshots.stream().mapToLong(MetricsSnapshot::hwndCaptureSuccess).sum();
        long totalRobotCapture = snapshots.stream().mapToLong(MetricsSnapshot::robotCaptureSuccess).sum();
        long totalHwndKeyboard = snapshots.stream().mapToLong(MetricsSnapshot::hwndKeyboardSuccess).sum();
        long maxBar = snapshots.stream()
                .mapToLong(MetricsSnapshot::maxCounter)
                .max()
                .orElse(1);
        StringBuilder rows = new StringBuilder();
        for (MetricsSnapshot snapshot : snapshots) {
            rows.append("<tr>")
                    .append("<td>").append(escape(snapshot.windowId())).append("</td>")
                    .append(metricCell(snapshot.focusAttempts(), maxBar, "focus"))
                    .append(metricCell(snapshot.hwndCaptureSuccess(), maxBar, "hwnd"))
                    .append(metricCell(snapshot.robotCaptureSuccess(), maxBar, "robot"))
                    .append(metricCell(snapshot.captureFailure(), maxBar, "fail"))
                    .append(metricCell(snapshot.hwndKeyboardSuccess(), maxBar, "key"))
                    .append(metricCell(snapshot.hwndKeyboardFailure(), maxBar, "fail"))
                    .append("</tr>");
        }
        String updatedAt = DASHBOARD_TIME_FORMATTER.format(LocalDateTime.now());
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta http-equiv="refresh" content="3">
                  <title>DHXY Interaction Metrics</title>
                  <style>
                    :root { color-scheme: light dark; font-family: "Segoe UI", "Microsoft YaHei", sans-serif; }
                    body { margin: 0; background: #111827; color: #e5e7eb; }
                    main { padding: 24px; max-width: 1180px; margin: 0 auto; }
                    h1 { margin: 0 0 6px; font-size: 24px; }
                    .muted { color: #9ca3af; margin-bottom: 18px; }
                    .cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 18px 0; }
                    .card { background: #1f2937; border: 1px solid #374151; border-radius: 8px; padding: 14px; }
                    .label { color: #9ca3af; font-size: 12px; }
                    .value { font-size: 28px; font-weight: 700; margin-top: 4px; }
                    table { width: 100%; border-collapse: collapse; background: #111827; border: 1px solid #374151; }
                    th, td { padding: 10px; border-bottom: 1px solid #263244; text-align: left; vertical-align: middle; }
                    th { color: #cbd5e1; background: #1f2937; font-size: 13px; }
                    .metric { display: grid; grid-template-columns: 56px 1fr; gap: 8px; align-items: center; }
                    .barTrack { height: 10px; background: #334155; border-radius: 999px; overflow: hidden; }
                    .bar { height: 100%; border-radius: 999px; }
                    .focus { background: #f97316; }
                    .hwnd { background: #22c55e; }
                    .robot { background: #eab308; }
                    .key { background: #38bdf8; }
                    .fail { background: #ef4444; }
                    .empty { padding: 24px; color: #9ca3af; background: #1f2937; border-radius: 8px; }
                  </style>
                </head>
                <body>
                <main>
                  <h1>DHXY Interaction Metrics</h1>
                  <div class="muted">Updated: %s · Auto refreshes every 3 seconds</div>
                  <section class="cards">
                    <div class="card"><div class="label">Focus Attempts</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">HWND Captures</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">Robot Captures</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">HWND Keyboard</div><div class="value">%d</div></div>
                  </section>
                  %s
                </main>
                </body>
                </html>
                """.formatted(updatedAt, totalFocus, totalHwndCapture, totalRobotCapture, totalHwndKeyboard,
                snapshots.isEmpty() ? "<div class=\"empty\">No interaction metrics yet.</div>" : """
                        <table>
                          <thead>
                            <tr>
                              <th>Window</th>
                              <th>Focus</th>
                              <th>HWND Capture</th>
                              <th>Robot Capture</th>
                              <th>Capture Fail</th>
                              <th>HWND Keyboard</th>
                              <th>Keyboard Fail</th>
                            </tr>
                          </thead>
                          <tbody>
                        """ + rows + """
                          </tbody>
                        </table>
                        """);
    }

    private String metricCell(long value, long maxBar, String cssClass) {
        int percent = maxBar <= 0 ? 0 : (int) Math.round(value * 100.0 / maxBar);
        return """
                <td><div class="metric"><span>%d</span><div class="barTrack"><div class="bar %s" style="width:%d%%"></div></div></div></td>
                """.formatted(value, cssClass, Math.max(1, percent));
    }

    private List<MetricsSnapshot> snapshots() {
        return metricsByWindow.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparing(MetricsSnapshot::windowId))
                .toList();
    }

    private Metrics metrics(String windowId) {
        return metricsByWindow.computeIfAbsent(key(windowId), ignored -> new Metrics());
    }

    private String key(String windowId) {
        return windowId == null || windowId.isBlank() ? "NO_WINDOW_CONTEXT" : windowId;
    }

    private static class Metrics {
        private final AtomicLong focusAttempts = new AtomicLong();
        private final AtomicLong focusSuccess = new AtomicLong();
        private final AtomicLong focusFailure = new AtomicLong();
        private final AtomicLong hwndCaptureSuccess = new AtomicLong();
        private final AtomicLong robotCaptureSuccess = new AtomicLong();
        private final AtomicLong captureFailure = new AtomicLong();
        private final AtomicLong hwndKeyboardSuccess = new AtomicLong();
        private final AtomicLong hwndKeyboardFailure = new AtomicLong();

        private MetricsSnapshot snapshot(String windowId) {
            return new MetricsSnapshot(
                    windowId,
                    focusAttempts.get(),
                    focusSuccess.get(),
                    focusFailure.get(),
                    hwndCaptureSuccess.get(),
                    robotCaptureSuccess.get(),
                    captureFailure.get(),
                    hwndKeyboardSuccess.get(),
                    hwndKeyboardFailure.get()
            );
        }
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class MetricsSnapshot {

        String windowId;

        long focusAttempts;

        long focusSuccess;

        long focusFailure;

        long hwndCaptureSuccess;

        long robotCaptureSuccess;

        long captureFailure;

        long hwndKeyboardSuccess;

        long hwndKeyboardFailure;

        private long maxCounter() {
            return Math.max(focusAttempts,
                    Math.max(hwndCaptureSuccess,
                            Math.max(robotCaptureSuccess,
                                    Math.max(captureFailure,
                                            Math.max(hwndKeyboardSuccess, hwndKeyboardFailure)))));
        }
    

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
}
