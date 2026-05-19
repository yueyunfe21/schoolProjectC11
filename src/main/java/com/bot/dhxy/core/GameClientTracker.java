package com.bot.dhxy.core;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.VisionProvider;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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

    @Lazy
    @Autowired
    private InputProvider inputProvider;

    public static final String LATEST_VISION_PATH = "images/temp/latest_vision.png";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final Path TRACKER_DIAGNOSTIC_LOG = Path.of("logs", "tracker-coordinate.log");

    private final TrackerState sharedState = new TrackerState();
    private final ThreadLocal<TrackerState> threadState = ThreadLocal.withInitial(TrackerState::new);

    public int getWindowBaseX() { return state().windowBaseX; }

    public int getWindowBaseY() { return state().windowBaseY; }

    public String getFullWindowTitle() { return state().fullWindowTitle; }

    public HWND getGameHwnd() { return state().gameHwnd; }

    public String getLatestVisionPath() { return windowScopedTempPath.resolve("latest_vision.png"); }

    public boolean updateGlobalVision() {
        return globalInputLock.callWithLock(() -> {
            if (!checkBaseAddress()) return false;
            if (!bringWindowToFrontWithoutLock()) {
                log.warn("无法唤醒游戏窗口，停止本次视觉更新");
                return false;
            }
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

    public boolean captureToFile(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return globalInputLock.callWithLock(() -> captureToFileWithoutLock(elementName, savePath, x1, y1, x2, y2));
    }

    public boolean captureToFileWithShield(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        return globalInputLock.callWithLock(() -> {
            if (!checkBaseAddress()) return false;
            logTrackerState("captureToFileWithShield:" + elementName);
            log.debug("装甲截图开始：{} savePath={}", elementName, savePath);
            inputProvider.pressAlt4();
            sleepQuietly(400);
            try {
                return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
            } finally {
                inputProvider.pressAlt4();
                log.debug("装甲截图结束：{}", elementName);
            }
        });
    }

    public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
        return globalInputLock.callWithLock(() -> {
            if (!checkBaseAddress()) return null;
            logTrackerState("captureToMemory:" + elementName);
            return eyes.captureRegionByCoordinates(x1, y1, x2, y2);
        });
    }

    private boolean captureToFileWithoutLock(String elementName, String savePath, int x1, int y1, int x2, int y2) {
        if (!checkBaseAddress()) return false;
        logTrackerState("captureToFile:" + elementName);
        log.debug("截图：{} savePath={} rect=({}, {})-({}, {})", elementName, savePath, x1, y1, x2, y2);
        return eyes.captureRegionToFile(savePath, x1, y1, x2, y2);
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
        WindowNativeBinding binding = current.get().getNativeBinding();
        if (binding == null) {
            logTrackerMiss("native-binding-null windowId=" + current.get().getWindowId());
            return false;
        }
        if (!binding.hasNativeHandle()) {
            logTrackerMiss("native-handle-empty windowId=" + current.get().getWindowId()
                    + " title=" + binding.getTitle()
                    + " class=" + binding.getClassName()
                    + " pid=" + binding.getProcessId());
            return false;
        }
        HWND hwnd = toHwnd(binding.getNativeHandle());
        if (hwnd == null) {
            logTrackerMiss("native-handle-parse-failed windowId=" + current.get().getWindowId()
                    + " handle=" + binding.getNativeHandle());
            return false;
        }
        String title = binding.getTitle() == null || binding.getTitle().isBlank()
                ? current.get().getWindowId()
                : binding.getTitle();
        updateBaseFromHwnd(hwnd, title);
        return true;
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
        User32.INSTANCE.SetActiveWindow(s.gameHwnd);
        boolean foregroundOk = User32.INSTANCE.SetForegroundWindow(s.gameHwnd);
        sleepQuietly(200);
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

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        User32.INSTANCE.PostMessage(s.gameHwnd, WM_SYSKEYUP, new WPARAM(VK_8), new LPARAM(lParamUp));
        log.info("后台 Alt+8 指令投递完毕");
    }

    private static class TrackerState {
        private int windowBaseX = -1;
        private int windowBaseY = -1;
        private String fullWindowTitle = "";
        private HWND gameHwnd = null;
    }
}
