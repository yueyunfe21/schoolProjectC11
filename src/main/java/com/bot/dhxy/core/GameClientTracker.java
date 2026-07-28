package com.bot.dhxy.core;

import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.capture.WindowCaptureEvidenceStore;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.runtime.WindowTitleIdentityParser;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameClientTracker {

    private final VisionProvider eyes;
    private final BotProperties config;
    private final CoordinateHelper coordinateHelper;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final GlobalInputLock globalInputLock;
    private final WindowIsolationProperties windowIsolationProperties;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowFocusService windowFocusService;
    private final BoundWindowCaptureService boundWindowCaptureService;
    private final WindowInteractionMetricsService windowInteractionMetricsService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final WindowCaptureEvidenceStore captureEvidenceStore;

    public static final String LATEST_VISION_PATH = "images/temp/latest_vision.png";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final Path TRACKER_DIAGNOSTIC_LOG = Path.of("logs", "tracker-coordinate.log");

    private final TrackerState sharedState = new TrackerState();
    private final ThreadLocal<TrackerState> threadState = ThreadLocal.withInitial(TrackerState::new);
    private volatile CaptureAudit lastCaptureAudit = CaptureAudit.empty();

    public int getWindowBaseX() { return state().windowBaseX; }

    public int getWindowBaseY() { return state().windowBaseY; }

    public String getFullWindowTitle() { return state().fullWindowTitle; }

    public HWND getGameHwnd() { return state().gameHwnd; }

    public String getLatestVisionPath() { return windowScopedTempPath.resolve("latest_vision.png"); }

    public CaptureAudit getLastCaptureAudit() { return lastCaptureAudit; }

    public boolean refreshWindowState() {
        return globalInputLock.callWithLock(this::checkBaseAddress);
    }

    public boolean updateGlobalVision() {
        return globalInputLock.callWithLock(() -> {
            if (!checkBaseAddress()) return false;
            TrackerState s = state();
            logTrackerState("updateGlobalVision");
            int x1 = s.windowBaseX;
            int y1 = s.windowBaseY;
            int x2 = x1 + WINDOW_WIDTH;
            int y2 = y1 + WINDOW_HEIGHT;
            return captureToFileWithoutLock("全局视野", getLatestVisionPath(), x1, y1, x2, y2);
        });
    }

    public boolean locateWindow() {
        if (useBoundWindowIfAvailable()) {
            logTrackerState("locateWindow-bound");
            return true;
        }

        if (windowIsolationProperties.isBoundWindowTrackerActive()
                && windowTaskContextHolder.rawCurrent().isPresent()) {
            logTrackerMiss("bound-required-skip-title-search");
            return false;
        }

        String target = config.getWindowKeyword();
        final HWND[] targetHwnd = {null};
        final String[] targetTitle = {""};

        User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) {
                return true;
            }
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String wText = Native.toString(windowText).trim();
            if (wText.contains(target)) {
                targetHwnd[0] = hwnd;
                targetTitle[0] = wText;
                return false;
            }
            return true;
        }, null);

        if (targetHwnd[0] == null) {
            log.warn("定位失败，未找到包含关键字 [{}] 的窗口", target);
            return false;
        }

        updateBaseFromHwnd(targetHwnd[0], targetTitle[0]);
        logTrackerState("locateWindow-title-search");
        return true;
    }

    /*
     * Captures no longer take the global input lock. The lock's only legitimate job is
     * serializing REAL mouse/keyboard (its own class comment says captures may run in
     * parallel); captures were only ever inside it as a legacy of the removed
     * foreground-capture fallback. The whole capture chain is now pure background
     * PrintWindow (BoundWindowCaptureService never focuses or foregrounds), so holding the
     * input lock here only starved observation frames whenever the shared input worker was
     * busy — with five windows clicking, unlucky windows went 20+ seconds without a frame
     * and the Cloud misjudged their state.
     */
    public boolean captureToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return captureToFileWithoutLock(elementName, savePath, x1, y1, x2, y2);
    }

    /**
     * Kept for API compatibility: captures are lock-free now, so "if idle" simply captures.
     */
    public BufferedImage captureToMemoryIfIdle(String elementName, int x1, int y1, int x2, int y2) {
        return captureToMemory(elementName, x1, y1, x2, y2);
    }

    public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
        if (!isValidRect(x1, y1, x2, y2)) {
            logCaptureResult("memory", elementName, null, x1, y1, x2, y2, false, "INVALID_RECT");
            return null;
        }
        if (!checkBaseAddress()) {
            logCaptureResult("memory", elementName, null, x1, y1, x2, y2, false, "CHECK_BASE_FAILED");
            return null;
        }
        Optional<BoundWindowCaptureService.CaptureResult> hwndCapture = captureToMemoryByHwndIfAvailable(x1, y1, x2, y2);
        if (hwndCapture.isPresent()) {
            BoundWindowCaptureService.CaptureResult result = hwndCapture.get();
            logCaptureResult("memory", elementName, null, x1, y1, x2, y2, true, "OK", result.provider().name());
            return result.image();
        }
        logCaptureResult("memory", elementName, null, x1, y1, x2, y2, false,
                "HWND_CAPTURE_FAILED_NO_FOREGROUND_FALLBACK", "HWND");
        return null;
    }


    private boolean captureToFileWithoutLock(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!isValidRect(x1, y1, x2, y2)) {
            logCaptureResult("file", elementName, savePath, x1, y1, x2, y2, false, "INVALID_RECT");
            return false;
        }
        if (!checkBaseAddress()) {
            logCaptureResult("file", elementName, savePath, x1, y1, x2, y2, false, "CHECK_BASE_FAILED");
            return false;
        }
        Optional<BoundWindowCaptureService.CaptureResult> hwndCapture = captureToMemoryByHwndIfAvailable(x1, y1, x2, y2);
        if (hwndCapture.isPresent()) {
            BoundWindowCaptureService.CaptureResult result = hwndCapture.get();
            boolean success = writeCaptureToFile(result.image(), savePath);
            logCaptureResult("file", elementName, savePath, x1, y1, x2, y2, success,
                    success ? "OK" : "WRITE_FAILED", result.provider().name());
            return success;
        }
        logCaptureResult("file", elementName, savePath, x1, y1, x2, y2, false,
                "HWND_CAPTURE_FAILED_NO_FOREGROUND_FALLBACK", "HWND");
        return false;
    }

    private Optional<BoundWindowCaptureService.CaptureResult> captureToMemoryByHwndIfAvailable(int x1, int y1, int x2, int y2) {
        if (!windowIsolationProperties.isHwndCaptureActive()) {
            return Optional.empty();
        }
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            return Optional.empty();
        }
        WindowNativeBinding binding = current.get().getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return Optional.empty();
        }
        TrackerState s = state();
        Optional<BoundWindowCaptureService.CaptureResult> result = boundWindowCaptureService.captureRegion(
                binding, s.windowBaseX, s.windowBaseY, x1, y1, x2, y2);
        if (result.isEmpty()) {
            log.debug("HWND capture unavailable, fallback decision follows: windowId={} hwnd={} rect=({}, {})-({}, {}) base=({}, {})",
                    current.get().getWindowId(), binding.getNativeHandle(), x1, y1, x2, y2, s.windowBaseX, s.windowBaseY);
        }
        return result;
    }

    private WindowNativeBinding currentBinding() {
        return windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getNativeBinding)
                .orElse(WindowNativeBinding.empty());
    }

    private boolean writeCaptureToFile(BufferedImage image, String savePath) {
        if (image == null || savePath == null || savePath.isBlank()) {
            return false;
        }
        try {
            Path path = Path.of(savePath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return ImageIO.write(image, "png", path.toFile());
        } catch (Exception e) {
            log.warn("截图写入失败：path={} reason={}", savePath, e.getMessage(), e);
            return false;
        }
    }


    private boolean isValidRect(int x1, int y1, int x2, int y2) {
        return x1 != x2 && y1 != y2;
    }

    private void logCaptureResult(String mode, String elementName, String savePath,
                                  int x1, int y1, int x2, int y2,
                                  boolean success, String reason) {
        logCaptureResult(mode, elementName, savePath, x1, y1, x2, y2, success, reason, "UNKNOWN");
    }

    private void logCaptureResult(String mode, String elementName, String savePath,
                                  int x1, int y1, int x2, int y2,
                                  boolean success, String reason, String provider) {
        TrackerState s = state();
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        String windowId = current.map(WindowRuntimeContext::getWindowId).orElse("NO_WINDOW_CONTEXT");
        String hwndText = s.gameHwnd == null ? "null" : Pointer.nativeValue(s.gameHwnd.getPointer()) + "";
        String foregroundHwnd = windowFocusService.getForegroundNativeHandleText();
        windowInteractionMetricsService.recordCapture(windowId, provider, success, mode, elementName);
        lastCaptureAudit = new CaptureAudit(System.currentTimeMillis(), mode, elementName, savePath,
                success, reason, provider, x1, y1, x2, y2, s.windowBaseX, s.windowBaseY,
                windowId, hwndText, foregroundHwnd, s.fullWindowTitle);
        String message = "Capture result: mode={} element={} windowId={} result={} reason={} provider={} path={} rect=({}, {})-({}, {}) base=({}, {}) hwnd={} foreground={} title={}";
        Object[] args = {
                mode, elementName, windowId, success ? "success" : "failed", reason, provider, savePath,
                x1, y1, x2, y2, s.windowBaseX, s.windowBaseY, hwndText, foregroundHwnd, s.fullWindowTitle
        };
        if (!success || "ROBOT".equalsIgnoreCase(provider)) {
            log.info(message, args);
        } else {
            log.debug(message, args);
        }
    }

    private boolean checkBaseAddress() {
        if (useBoundWindowIfAvailable()) {
            return true;
        }
        if (windowIsolationProperties.isBoundWindowTrackerActive()
                && windowTaskContextHolder.rawCurrent().isPresent()) {
            logTrackerMiss("bound-required-skip-check-base-title-search");
            return false;
        }
        TrackerState s = state();
        if (s.windowBaseX == -1 || s.gameHwnd == null) {
            return locateWindow();
        }
        updateBaseFromHwnd(s.gameHwnd, s.fullWindowTitle);
        return true;
    }

    private boolean useBoundWindowIfAvailable() {
        if (!windowIsolationProperties.isBoundWindowTrackerActive()) {
            logTrackerMiss("bound-switch-off");
            return false;
        }
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty()) {
            logTrackerMiss("raw-current-empty");
            return false;
        }
        WindowRuntimeContext context = current.get();
        WindowNativeBinding binding = context.getNativeBinding();
        if (binding == null) {
            logTrackerMiss("native-binding-null windowId=" + context.getWindowId());
            return false;
        }
        if (!binding.hasNativeHandle()) {
            logTrackerMiss("native-handle-empty windowId=" + context.getWindowId()
                    + " title=" + binding.getTitle()
                    + " class=" + binding.getClassName()
                    + " pid=" + binding.getProcessId());
            return false;
        }
        HWND hwnd = toHwnd(binding.getNativeHandle());
        if (hwnd == null) {
            logTrackerMiss("native-handle-parse-failed windowId=" + context.getWindowId()
                    + " handle=" + binding.getNativeHandle());
            return false;
        }
        Optional<WindowNativeBinding> refreshed = bindingRefreshService.refreshAndCommit(context);
        if (refreshed.isEmpty()) {
            logTrackerMiss("native-binding-live-geometry-unavailable windowId=" + context.getWindowId()
                    + " handle=" + binding.getNativeHandle());
            return false;
        }
        WindowNativeBinding liveBinding = refreshed.get();
        String title = resolveBoundWindowTitle(liveBinding, context);
        updateBaseFromBinding(liveBinding, hwnd, title);
        return true;
    }

    private String resolveBoundWindowTitle(WindowNativeBinding liveBinding, WindowRuntimeContext context) {
        String liveTitle = normalizeTitle(liveBinding.getTitle());
        if (liveTitle != null) {
            return liveTitle;
        }

        TrackerState s = state();
        String previousTrackerTitle = normalizeTitle(s.fullWindowTitle);
        if (previousTrackerTitle != null && WindowTitleIdentityParser.parse(previousTrackerTitle).isPresent()) {
            log.warn("bound window live title is blank; retaining previous parseable tracker title: windowId={} hwnd={} previousTitle={}",
                    context.getWindowId(), liveBinding.getNativeHandle(), previousTrackerTitle);
            return previousTrackerTitle;
        }

        log.warn("bound window live title is blank and no parseable tracker title exists; tracker title will stay blank: windowId={} hwnd={}",
                context.getWindowId(), liveBinding.getNativeHandle());
        return "";
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void updateBaseFromBinding(WindowNativeBinding binding, HWND hwnd, String title) {
        double scale = coordinateHelper.getScaleRatio();
        TrackerState s = state();
        s.windowBaseX = (int) (binding.getX() / scale);
        s.windowBaseY = (int) (binding.getY() / scale);
        s.gameHwnd = hwnd;
        s.fullWindowTitle = title == null ? "" : title;
        logBindingLiveDelta(binding, hwnd, scale);
    }

    private void updateBaseFromHwnd(HWND hwnd, String title) {
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        double scale = coordinateHelper.getScaleRatio();
        TrackerState s = state();
        s.windowBaseX = (int) (rect.left / scale);
        s.windowBaseY = (int) (rect.top / scale);
        s.gameHwnd = hwnd;
        s.fullWindowTitle = title == null ? "" : title;
    }

    private void logBindingLiveDelta(WindowNativeBinding binding, HWND hwnd, double scale) {
        RECT live = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, live);
        int liveX = (int) (live.left / scale);
        int liveY = (int) (live.top / scale);
        int bindingX = (int) (binding.getX() / scale);
        int bindingY = (int) (binding.getY() / scale);
        int dx = liveX - bindingX;
        int dy = liveY - bindingY;
        if (dx == 0 && dy == 0) {
            TrackerState s = state();
            s.lastBindingLiveDeltaX = 0;
            s.lastBindingLiveDeltaY = 0;
            return;
        }
        TrackerState s = state();
        if (s.lastBindingLiveDeltaX == dx && s.lastBindingLiveDeltaY == dy) {
            return;
        }
        s.lastBindingLiveDeltaX = dx;
        s.lastBindingLiveDeltaY = dy;
        String line = LocalDateTime.now()
                + " | action=bound-geometry-live-delta"
                + " | binding=(" + bindingX + "," + bindingY + " " + binding.getWidth() + "x" + binding.getHeight() + ")"
                + " | live=(" + liveX + "," + liveY + " "
                + Math.max(live.right - live.left, 0) + "x" + Math.max(live.bottom - live.top, 0) + ")"
                + " | delta=(" + dx + "," + dy + ")"
                + " | hwnd=" + Pointer.nativeValue(hwnd.getPointer())
                + " | title=" + binding.getTitle();
        log.debug("[TrackerCoordinate] {}", line);
        appendTrackerDiagnostic(line);
    }

    public boolean bringWindowToFront() {
        return globalInputLock.callWithLock(this::bringWindowToFrontWithoutLock);
    }

    private boolean bringWindowToFrontWithoutLock() {
        if (!checkBaseAddress()) {
            return false;
        }
        TrackerState s = state();
        logTrackerState("bringWindowToFront");
        User32.INSTANCE.ShowWindow(s.gameHwnd, 9);
        User32.INSTANCE.BringWindowToTop(s.gameHwnd);
        boolean foregroundOk = User32.INSTANCE.SetForegroundWindow(s.gameHwnd);
        TaskSleep.sleep(200);
        HWND foreground = User32.INSTANCE.GetForegroundWindow();
        boolean focused = foreground != null
                && s.gameHwnd != null
                && Pointer.nativeValue(foreground.getPointer()) == Pointer.nativeValue(s.gameHwnd.getPointer());
        if (!foregroundOk || !focused) {
            log.warn("窗口置前可能失败：title={} hwnd={} foregroundOk={} focused={}",
                    s.fullWindowTitle,
                    s.gameHwnd == null ? "null" : Pointer.nativeValue(s.gameHwnd.getPointer()),
                    foregroundOk,
                    focused);
        }
        return focused || foregroundOk;
    }

    private TrackerState state() {
        return windowIsolationProperties.isTrackerStateIsolationActive() && windowTaskContextHolder.rawCurrent().isPresent()
                ? threadState.get()
                : sharedState;
    }

    private void logTrackerState(String action) {
        TrackerState s = state();
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        String windowId = current.map(WindowRuntimeContext::getWindowId).orElse("NO_WINDOW_CONTEXT");
        String hwndText = s.gameHwnd == null ? "null" : Pointer.nativeValue(s.gameHwnd.getPointer()) + "";
        String line = LocalDateTime.now()
                + " | action=" + action
                + " | windowId=" + windowId
                + " | base=(" + s.windowBaseX + "," + s.windowBaseY + ")"
                + " | hwnd=" + hwndText
                + " | title=" + s.fullWindowTitle;
        log.debug("[TrackerCoordinate] {}", line);
        appendTrackerDiagnostic(line);
    }

    private void logTrackerMiss(String reason) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        String rawWindowId = current.map(WindowRuntimeContext::getWindowId).orElse("NO_RAW_WINDOW_CONTEXT");
        String line = LocalDateTime.now()
                + " | action=bound-tracker-miss"
                + " | reason=" + reason
                + " | rawWindowId=" + rawWindowId
                + " | isolation=" + windowIsolationProperties.isIsolationEnabled()
                + " | inputFocus=" + windowIsolationProperties.isInputFocusEnabled()
                + " | trackerState=" + windowIsolationProperties.isTrackerStateIsolationEnabled()
                + " | boundTracker=" + windowIsolationProperties.isBoundWindowTrackerEnabled()
                + " | scopedTemp=" + windowIsolationProperties.isScopedTempPathEnabled();
        log.debug("[TrackerCoordinate] {}", line);
        appendTrackerDiagnostic(line);
    }

    private void appendTrackerDiagnostic(String line) {
        try {
            Files.createDirectories(TRACKER_DIAGNOSTIC_LOG.getParent());
            Files.writeString(TRACKER_DIAGNOSTIC_LOG,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.debug("无法写入 tracker 坐标诊断日志：{}", e.getMessage());
        }
    }

    private HWND toHwnd(String handleText) {
        Long value = parseHandleValue(handleText);
        if (value == null || value <= 0) {
            return null;
        }
        return new HWND(new Pointer(value));
    }

    private Long parseHandleValue(String handleText) {
        if (handleText == null || handleText.isBlank()) {
            return null;
        }
        String value = handleText.trim();
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseUnsignedLong(value.substring(2), 16);
            }
            if (value.matches(".*[a-fA-F].*")) {
                return Long.parseUnsignedLong(value, 16);
            }
            try {
                return Long.parseUnsignedLong(value);
            } catch (NumberFormatException ignored) {
                return Long.parseUnsignedLong(value, 16);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void testBackgroundAlt8() {
        TrackerState s = state();
        if (s.gameHwnd == null) {
            log.warn("gameHwnd 为空，请先执行 locateWindow");
            return;
        }
        int WM_SYSKEYDOWN = 0x0104;
        int WM_SYSKEYUP = 0x0105;
        int VK_8 = 0x38;
        long lParamDown = (1 << 29) | (0x09 << 16) | 1;
        long lParamUp = (1L << 31) | (1 << 30) | (1 << 29) | (0x09 << 16) | 1;
        User32.INSTANCE.PostMessage(s.gameHwnd, WM_SYSKEYDOWN, new WPARAM(VK_8), new LPARAM(lParamDown));
        TaskSleep.sleep(50);
        User32.INSTANCE.PostMessage(s.gameHwnd, WM_SYSKEYUP, new WPARAM(VK_8), new LPARAM(lParamUp));
        log.info("后台 Alt+8 指令投递完毕");
    }

    /**
     * Last screenshot attempt details for callers that need to prove which bound window produced a
     * diagnostic image without turning every successful HWND capture into a high-volume info log.
     */
    public record CaptureAudit(long capturedAtMs,
                               String mode,
                               String elementName,
                               String path,
                               boolean success,
                               String reason,
                               String provider,
                               int x1,
                               int y1,
                               int x2,
                               int y2,
                               int baseX,
                               int baseY,
                               String windowId,
                               String hwnd,
                               String foregroundHwnd,
                               String title) {
        private static CaptureAudit empty() {
            return new CaptureAudit(0L, "-", "-", null, false, "NO_CAPTURE", "UNKNOWN",
                    0, 0, 0, 0, 0, 0, "-", "-", "-", "-");
        }

        public String toLogText() {
            return "capturedAtMs=" + capturedAtMs
                    + " mode=" + mode
                    + " element=" + elementName
                    + " success=" + success
                    + " reason=" + reason
                    + " provider=" + provider
                    + " path=" + path
                    + " rect=(" + x1 + "," + y1 + ")-(" + x2 + "," + y2 + ")"
                    + " base=(" + baseX + "," + baseY + ")"
                    + " windowId=" + windowId
                    + " hwnd=" + hwnd
                    + " foreground=" + foregroundHwnd
                    + " title=" + title;
        }
    }

    private static class TrackerState {
        private int windowBaseX = -1;
        private int windowBaseY = -1;
        private String fullWindowTitle = "";
        private HWND gameHwnd = null;
        private int lastBindingLiveDeltaX = Integer.MIN_VALUE;
        private int lastBindingLiveDeltaY = Integer.MIN_VALUE;
    }
}
