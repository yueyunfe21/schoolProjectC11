package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowNativeBinding;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WindowNativeBindingRefreshService {

    public Optional<WindowNativeBinding> refreshGeometry(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle()) {
            return Optional.empty();
        }
        Long handle = WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (handle == null || handle == 0L) {
            return Optional.empty();
        }
        WinDef.HWND hwnd = new WinDef.HWND(new Pointer(handle));
        if (!User32.INSTANCE.IsWindow(hwnd)) {
            return Optional.empty();
        }
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(hwnd, rect)) {
            return Optional.empty();
        }
        int width = Math.max(rect.right - rect.left, 0);
        int height = Math.max(rect.bottom - rect.top, 0);
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        return Optional.of(binding.withGeometry(rect.left, rect.top, width, height));
    }
}
