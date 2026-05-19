package com.bot.dhxy.window.interaction;

import com.bot.dhxy.input.GlobalInputLock;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Windows 原生窗口激活服务。
 *
 * SetForegroundWindow 在 Windows 前台权限限制下经常返回 false，
 * 但真实鼠标/键盘动作仍然可能正常执行。因此这里把 focus 视为 best-effort：
 * 只要 hwnd 合法并完成置前尝试，就不阻断输入队列。
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
        sleepQuietly(50);

        log.debug("窗口置前尝试完成：handle={} title={} foregroundOk={}",
                binding.getNativeHandle(), binding.getTitle(), foregroundOk);
        return true;
    }

    private WinDef.HWND toHwnd(String handleText) {
        Long value = WindowHandleParser.parseHexHandle(handleText);
        if (value == null || value <= 0) {
            return null;
        }
        return new WinDef.HWND(new Pointer(value));
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}