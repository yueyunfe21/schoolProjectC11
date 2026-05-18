package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.runtime.WindowNativeBinding;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.springframework.stereotype.Service;

/**
 * Windows 原生窗口激活服务。
 */
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
        return inputLock.callLocked(() -> {
            WinDef.HWND hwnd = toHwnd(binding.getNativeHandle());
            if (hwnd == null) {
                return false;
            }
            User32.INSTANCE.ShowWindow(hwnd, 9);
            return User32.INSTANCE.SetForegroundWindow(hwnd);
        });
    }

    private WinDef.HWND toHwnd(String handleText) {
        try {
            long value = Long.parseUnsignedLong(handleText, 16);
            return new WinDef.HWND(new Pointer(value));
        } catch (Exception e) {
            return null;
        }
    }
}
