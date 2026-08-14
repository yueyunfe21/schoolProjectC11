package com.bot.dhxy.window.interaction;

import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Windows 原生窗口激活服务。
 *
 * SetForegroundWindow 在 Windows 前台权限限制下经常返回 false，
 * 但真实鼠标/键盘动作仍然可能正常执行。普通输入调用方仍可把 focus 视为 best-effort；
 * 本服务的返回值只表示目标 HWND 是否已被确认成当前前台窗口，供必须依赖前台键盘的动作严格判定。
 */
@Slf4j
@Service
public class WindowFocusService {

    private final GlobalInputLock inputLock;

    public WindowFocusService(GlobalInputLock inputLock) {
        this.inputLock = inputLock;
    }

    public boolean focus(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return false;
        }
        return inputLock.callWithLock(() -> focusWithoutLock(binding));
    }

    /**
     * 调用方已经持有全局输入锁时使用，避免重复套锁。
     */
    public boolean focusWithoutLock(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return false;
        }
        WinDef.HWND hwnd = toHwnd(binding.getNativeHandle());
        if (hwnd == null) {
            return false;
        }

        User32.INSTANCE.ShowWindow(hwnd, 9);
        User32.INSTANCE.BringWindowToTop(hwnd);
        boolean foregroundOk = User32.INSTANCE.SetForegroundWindow(hwnd);
        TaskSleep.sleep(80);

        boolean focused = isFocused(hwnd);
        if (!foregroundOk || !focused) {
            User32.INSTANCE.BringWindowToTop(hwnd);
            foregroundOk = User32.INSTANCE.SetForegroundWindow(hwnd) || foregroundOk;
            TaskSleep.sleep(120);
            focused = isFocused(hwnd);
        }
        if (!foregroundOk || !focused) {
            foregroundOk = focusWithAttachedInput(hwnd) || foregroundOk;
            focused = isFocused(hwnd);
        }

        if (!foregroundOk || !focused) {
            log.warn("Window focus not confirmed, continuing best-effort: handle={} title={} foregroundOk={} focused={}",
                    binding.getNativeHandle(), binding.getTitle(), foregroundOk, focused);
        } else {
            log.debug("Window focus confirmed: handle={} title={} foregroundOk={} focused={}",
                    binding.getNativeHandle(), binding.getTitle(), foregroundOk, focused);
        }
        return focused;
    }

    private boolean focusWithAttachedInput(WinDef.HWND hwnd) {
        int currentThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
        int targetThreadId = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null);
        WinDef.HWND foreground = User32.INSTANCE.GetForegroundWindow();
        int foregroundThreadId = foreground == null ? 0 : User32.INSTANCE.GetWindowThreadProcessId(foreground, null);

        DWORD current = new DWORD(currentThreadId);
        DWORD target = new DWORD(targetThreadId);
        DWORD foregroundThread = new DWORD(foregroundThreadId);
        boolean attachedTarget = false;
        boolean attachedForeground = false;
        try {
            if (targetThreadId > 0 && targetThreadId != currentThreadId) {
                attachedTarget = User32.INSTANCE.AttachThreadInput(current, target, true);
            }
            if (foregroundThreadId > 0
                    && foregroundThreadId != currentThreadId
                    && foregroundThreadId != targetThreadId) {
                attachedForeground = User32.INSTANCE.AttachThreadInput(current, foregroundThread, true);
            }
            User32.INSTANCE.ShowWindow(hwnd, 9);
            User32.INSTANCE.BringWindowToTop(hwnd);
            User32.INSTANCE.SetFocus(hwnd);
            boolean foregroundOk = User32.INSTANCE.SetForegroundWindow(hwnd);
            TaskSleep.sleep(120);
            boolean focused = isFocused(hwnd);
            log.info("Attached focus attempt: hwnd={} currentThread={} targetThread={} foregroundThread={} attachedTarget={} attachedForeground={} foregroundOk={} focused={}",
                    Pointer.nativeValue(hwnd.getPointer()), currentThreadId, targetThreadId, foregroundThreadId,
                    attachedTarget, attachedForeground, foregroundOk, focused);
            return foregroundOk || focused;
        } finally {
            if (attachedForeground) {
                User32.INSTANCE.AttachThreadInput(current, foregroundThread, false);
            }
            if (attachedTarget) {
                User32.INSTANCE.AttachThreadInput(current, target, false);
            }
        }
    }

    private boolean isFocused(WinDef.HWND hwnd) {
        WinDef.HWND foreground = User32.INSTANCE.GetForegroundWindow();
        return foreground != null
                && Pointer.nativeValue(foreground.getPointer()) == Pointer.nativeValue(hwnd.getPointer());
    }

    public String getForegroundNativeHandleText() {
        WinDef.HWND foreground = User32.INSTANCE.GetForegroundWindow();
        if (foreground == null) {
            return null;
        }
        return String.valueOf(Pointer.nativeValue(foreground.getPointer()));
    }

    /**
     * Checks whether the supplied exact binding is already the foreground HWND without changing focus.
     *
     * @param binding exact native binding expected to own the current input transaction.
     * @return true only when Windows currently reports the same native handle as foreground.
     */
    public boolean isForeground(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return false;
        }
        WinDef.HWND hwnd = toHwnd(binding.getNativeHandle());
        return hwnd != null && isFocused(hwnd);
    }

    private WinDef.HWND toHwnd(String handleText) {
        Long value = WindowHandleParser.parseHandle(handleText);
        if (value == null || value <= 0) {
            return null;
        }
        return new WinDef.HWND(new Pointer(value));
    }
}
