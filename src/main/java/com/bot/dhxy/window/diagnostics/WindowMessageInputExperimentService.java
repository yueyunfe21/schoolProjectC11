package com.bot.dhxy.window.diagnostics;

import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 调试用：验证目标游戏 HWND 是否接受后台窗口消息输入。
 *
 * 注意：这不是 Robot/SendInput 真实输入，不会移动真实鼠标，也不要求前台焦点。
 * 很多游戏会忽略这类 WM_* 消息，所以这里只做显式按钮触发的实验。
 */
@Slf4j
@Service
public class WindowMessageInputExperimentService {

    private static final int WM_MOUSEMOVE = 0x0200;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP = 0x0202;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_RBUTTONUP = 0x0205;
    private static final int WM_SYSKEYDOWN = 0x0104;
    private static final int WM_SYSKEYUP = 0x0105;
    private static final int MK_LBUTTON = 0x0001;
    private static final int MK_RBUTTON = 0x0002;
    private static final int VK_MENU = 0x12;
    private static final int VK_1 = 0x31;
    private static final int VK_Q = 0x51;
    private static final int SCAN_ALT = 0x38;
    private static final int SCAN_1 = 0x02;
    private static final int SCAN_Q = 0x10;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final BoundWindowCaptureService boundWindowCaptureService;
    private final Path outputDir = Path.of("images", "temp", "window_input_experiment")
            .toAbsolutePath()
            .normalize();

    public WindowMessageInputExperimentService(BoundWindowCaptureService boundWindowCaptureService) {
        this.boundWindowCaptureService = boundWindowCaptureService;
    }

    public List<WindowMessageInputExperimentResult> postAltQ(List<WindowTaskSnapshot> snapshots) {
        return run(snapshots, "altq", this::postAltQ);
    }

    public List<WindowMessageInputExperimentResult> postAlt1(List<WindowTaskSnapshot> snapshots) {
        return run(snapshots, "alt1", this::postAlt1);
    }

    public List<WindowMessageInputExperimentResult> clickClientCenter(List<WindowTaskSnapshot> snapshots) {
        return run(snapshots, "center-click", this::postClientCenterClick);
    }

    public List<WindowMessageInputExperimentResult> rightClickClientCenter(List<WindowTaskSnapshot> snapshots) {
        return run(snapshots, "center-right-click", this::postClientCenterRightClick);
    }

    public List<WindowMessageInputExperimentResult> rightClickLargestChildCenter(List<WindowTaskSnapshot> snapshots) {
        return run(snapshots, "largest-child-right-click", this::postLargestChildCenterRightClick);
    }

    private List<WindowMessageInputExperimentResult> run(List<WindowTaskSnapshot> snapshots,
                                                         String actionName,
                                                         MessageAction action) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        List<WindowMessageInputExperimentResult> results = new ArrayList<>();
        for (WindowTaskSnapshot snapshot : snapshots) {
            results.add(runOne(snapshot, actionName, action));
        }
        return results;
    }

    private WindowMessageInputExperimentResult runOne(WindowTaskSnapshot snapshot,
                                                      String actionName,
                                                      MessageAction action) {
        String windowId = snapshot == null ? null : snapshot.getWindowId();
        WindowNativeBinding binding = snapshot == null ? null : snapshot.getNativeBinding();
        if (binding == null || !binding.hasNativeHandle()) {
            return WindowMessageInputExperimentResult.failed(windowId, actionName, "窗口没有 native handle");
        }
        WinDef.HWND hwnd = toHwnd(binding);
        if (hwnd == null) {
            return WindowMessageInputExperimentResult.failed(windowId, actionName, "native handle 无法解析：" + binding.getNativeHandle());
        }

        try {
            Files.createDirectories(outputDir);
            String stamp = FILE_TIME_FORMATTER.format(LocalDateTime.now());
            String prefix = sanitizeFileName(windowId == null ? "window" : windowId) + "_" + actionName + "_" + stamp;
            Path beforePath = outputDir.resolve(prefix + "_before.png");
            Path afterPath = outputDir.resolve(prefix + "_after.png");

            saveWindowCapture(binding, beforePath);
            boolean posted = action.post(hwnd);
            TaskSleep.sleep(650);
            saveWindowCapture(binding, afterPath);

            WindowMessageInputExperimentResult result = new WindowMessageInputExperimentResult(
                    windowId,
                    actionName,
                    binding.getNativeHandle(),
                    binding.getTitle(),
                    posted,
                    beforePath,
                    afterPath,
                    null
            );
            log.info("[后台输入实验] windowId={} action={} posted={} hwnd={} title={} before={} after={}",
                    windowId, actionName, posted, binding.getNativeHandle(), binding.getTitle(), beforePath, afterPath);
            return result;
        } catch (Exception e) {
            log.warn("[后台输入实验] 执行失败 windowId={} action={} hwnd={}: {}",
                    windowId, actionName, binding.getNativeHandle(), e.getMessage(), e);
            return WindowMessageInputExperimentResult.failed(windowId, actionName, "异常：" + e.getMessage());
        }
    }

    private boolean postAltQ(WinDef.HWND hwnd) {
        return postAltShortcut(hwnd, VK_Q, SCAN_Q);
    }

    private boolean postAlt1(WinDef.HWND hwnd) {
        return postAltShortcut(hwnd, VK_1, SCAN_1);
    }

    private boolean postAltShortcut(WinDef.HWND hwnd, int virtualKey, int scanCode) {
        boolean altDown = postKey(hwnd, WM_SYSKEYDOWN, VK_MENU, SCAN_ALT, true, false);
        TaskSleep.sleep(40);
        boolean keyDown = postKey(hwnd, WM_SYSKEYDOWN, virtualKey, scanCode, true, false);
        TaskSleep.sleep(60);
        boolean keyUp = postKey(hwnd, WM_SYSKEYUP, virtualKey, scanCode, true, true);
        TaskSleep.sleep(40);
        boolean altUp = postKey(hwnd, WM_SYSKEYUP, VK_MENU, SCAN_ALT, false, true);
        return altDown && keyDown && keyUp && altUp;
    }

    private boolean postClientCenterClick(WinDef.HWND hwnd) {
        return postClientCenterMouseClick(hwnd, WM_LBUTTONDOWN, WM_LBUTTONUP, MK_LBUTTON);
    }

    private boolean postClientCenterRightClick(WinDef.HWND hwnd) {
        return postClientCenterMouseClick(hwnd, WM_RBUTTONDOWN, WM_RBUTTONUP, MK_RBUTTON);
    }

    private boolean postLargestChildCenterRightClick(WinDef.HWND hwnd) {
        List<ChildWindowInfo> children = enumerateChildWindows(hwnd);
        log.info("[后台输入实验] child window scan: parent={} count={} children={}",
                Pointer.nativeValue(hwnd.getPointer()), children.size(), summarizeChildren(children));
        Optional<ChildWindowInfo> target = children.stream()
                .filter(ChildWindowInfo::visible)
                .max(Comparator.comparingInt(ChildWindowInfo::area));
        if (target.isEmpty()) {
            return false;
        }
        ChildWindowInfo child = target.get();
        log.info("[后台输入实验] use largest child hwnd={} class={} title={} rect=({}, {})-({}, {}) area={}",
                child.handleText(), child.className(), child.title(),
                child.left(), child.top(), child.right(), child.bottom(), child.area());
        return postClientCenterMouseClick(child.hwnd(), WM_RBUTTONDOWN, WM_RBUTTONUP, MK_RBUTTON);
    }

    private boolean postClientCenterMouseClick(WinDef.HWND hwnd, int downMessage, int upMessage, int buttonState) {
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32Message.INSTANCE.GetClientRect(hwnd, rect)) {
            return false;
        }
        int x = Math.max(1, (rect.right - rect.left) / 2);
        int y = Math.max(1, (rect.bottom - rect.top) / 2);
        WinDef.LPARAM point = new WinDef.LPARAM(packPoint(x, y));
        boolean moved = User32Message.INSTANCE.PostMessage(hwnd, WM_MOUSEMOVE, new WinDef.WPARAM(0), point);
        TaskSleep.sleep(40);
        boolean down = User32Message.INSTANCE.PostMessage(hwnd, downMessage, new WinDef.WPARAM(buttonState), point);
        TaskSleep.sleep(80);
        boolean up = User32Message.INSTANCE.PostMessage(hwnd, upMessage, new WinDef.WPARAM(0), point);
        return moved && down && up;
    }

    private boolean postKey(WinDef.HWND hwnd, int message, int virtualKey, int scanCode, boolean altContext, boolean keyUp) {
        long lParam = 1L | ((long) scanCode << 16);
        if (altContext) {
            lParam |= 1L << 29;
        }
        if (keyUp) {
            lParam |= 1L << 30;
            lParam |= 1L << 31;
        }
        return User32Message.INSTANCE.PostMessage(hwnd, message, new WinDef.WPARAM(virtualKey), new WinDef.LPARAM(lParam));
    }

    private int packPoint(int x, int y) {
        return (y << 16) | (x & 0xffff);
    }

    private void saveWindowCapture(WindowNativeBinding binding, Path path) {
        Optional<BoundWindowCaptureService.CaptureResult> capture = boundWindowCaptureService.captureWindow(binding);
        if (capture.isEmpty()) {
            return;
        }
        try {
            ImageIO.write(capture.get().image(), "png", path.toFile());
        } catch (Exception e) {
            log.debug("后台输入实验截图保存失败：path={} reason={}", path, e.getMessage());
        }
    }

    private List<ChildWindowInfo> enumerateChildWindows(WinDef.HWND parent) {
        List<ChildWindowInfo> children = new ArrayList<>();
        User32Message.INSTANCE.EnumChildWindows(parent, (child, data) -> {
            WinDef.RECT rect = new WinDef.RECT();
            User32.INSTANCE.GetWindowRect(child, rect);
            char[] className = new char[256];
            User32.INSTANCE.GetClassName(child, className, className.length);
            char[] title = new char[256];
            User32.INSTANCE.GetWindowText(child, title, title.length);
            boolean visible = User32.INSTANCE.IsWindowVisible(child);
            children.add(new ChildWindowInfo(
                    child,
                    String.valueOf(Pointer.nativeValue(child.getPointer())),
                    Native.toString(className),
                    Native.toString(title),
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    visible
            ));
            return true;
        }, null);
        return children;
    }

    private String summarizeChildren(List<ChildWindowInfo> children) {
        if (children == null || children.isEmpty()) {
            return "[]";
        }
        return children.stream()
                .limit(8)
                .map(child -> "{hwnd=" + child.handleText()
                        + ", class=" + child.className()
                        + ", title=" + child.title()
                        + ", visible=" + child.visible()
                        + ", rect=(" + child.left() + "," + child.top() + ")-(" + child.right() + "," + child.bottom() + ")"
                        + ", area=" + child.area()
                        + "}")
                .toList()
                .toString();
    }

    private WinDef.HWND toHwnd(WindowNativeBinding binding) {
        Long handle = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (handle == null || handle <= 0) {
            return null;
        }
        return new WinDef.HWND(new Pointer(handle));
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "window";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @FunctionalInterface
    private interface MessageAction {
        boolean post(WinDef.HWND hwnd);
    }

    private interface User32Message extends StdCallLibrary {
        User32Message INSTANCE = Native.load("user32", User32Message.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean PostMessage(WinDef.HWND hwnd, int msg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);

        boolean GetClientRect(WinDef.HWND hwnd, WinDef.RECT rect);

        boolean EnumChildWindows(WinDef.HWND hwndParent, WinUser.WNDENUMPROC enumFunc, Pointer data);
    }

    private record ChildWindowInfo(WinDef.HWND hwnd,
                                   String handleText,
                                   String className,
                                   String title,
                                   int left,
                                   int top,
                                   int right,
                                   int bottom,
                                   boolean visible) {
        private int area() {
            return Math.max(0, right - left) * Math.max(0, bottom - top);
        }
    }

    public static class WindowMessageInputExperimentResult {
        private final String windowId;
        private final String actionName;
        private final String nativeHandle;
        private final String title;
        private final boolean posted;
        private final Path beforePath;
        private final Path afterPath;
        private final String errorMessage;

        private WindowMessageInputExperimentResult(String windowId,
                                                   String actionName,
                                                   String nativeHandle,
                                                   String title,
                                                   boolean posted,
                                                   Path beforePath,
                                                   Path afterPath,
                                                   String errorMessage) {
            this.windowId = windowId;
            this.actionName = actionName;
            this.nativeHandle = nativeHandle;
            this.title = title;
            this.posted = posted;
            this.beforePath = beforePath;
            this.afterPath = afterPath;
            this.errorMessage = errorMessage;
        }

        private static WindowMessageInputExperimentResult failed(String windowId, String actionName, String message) {
            return new WindowMessageInputExperimentResult(windowId, actionName, null, null, false, null, null, message);
        }

        public String getWindowId() { return windowId; }

        public boolean isPosted() { return posted && errorMessage == null; }

        public String toDetailMessage() {
            if (errorMessage != null) {
                return actionName + " 失败：" + errorMessage;
            }
            return "action=" + actionName
                    + " posted=" + posted
                    + " hwnd=" + nativeHandle
                    + " before=" + beforePath
                    + " after=" + afterPath
                    + " title=" + (title == null || title.isBlank() ? "-" : title);
        }
    }
}
