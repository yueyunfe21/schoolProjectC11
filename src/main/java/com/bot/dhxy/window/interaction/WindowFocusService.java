package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.model.WindowNativeBinding;
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
        Long value = parseHandleValue(handleText);
        if (value == null || value <= 0) {
            return null;
        }
        return new WinDef.HWND(new Pointer(value));
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
}
