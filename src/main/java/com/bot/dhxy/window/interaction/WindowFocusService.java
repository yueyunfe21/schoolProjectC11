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
 * 和真实鼠标/键盘输入共用同一把 input.GlobalInputLock，避免多窗口同时激活/点击互相打架。
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

        sleepQuietly(200);

        WinDef.HWND foreground = User32.INSTANCE.GetForegroundWindow();
        boolean focused = sameHwnd(hwnd, foreground);
        if (!foregroundOk || !focused) {
            log.warn("窗口激活可能失败：handle={} title={} foregroundOk={} focused={}",
                    binding.getNativeHandle(), binding.getTitle(), foregroundOk, focused);
        }
        return focused || foregroundOk;
    }

    private boolean sameHwnd(WinDef.HWND expected, WinDef.HWND actual) {
        return expected != null
                && actual != null
                && Pointer.nativeValue(expected.getPointer()) == Pointer.nativeValue(actual.getPointer());
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
