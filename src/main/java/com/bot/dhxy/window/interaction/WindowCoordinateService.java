package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Service;

/**
 * 窗口坐标换算服务。
 */
@Service
public class WindowCoordinateService {

    public WindowRect toWindowRect(WindowNativeBinding binding) {
        if (binding == null || !binding.hasGeometry()) {
            return new WindowRect(0, 0, 0, 0);
        }
        return new WindowRect(binding.getX(), binding.getY(), binding.getWidth(), binding.getHeight());
    }

    public WindowPoint toScreenPoint(WindowNativeBinding binding, int relativeX, int relativeY) {
        return toWindowRect(binding).relativePoint(relativeX, relativeY);
    }

    public WindowPoint center(WindowNativeBinding binding) {
        return toWindowRect(binding).center();
    }

    public WindowRect estimateClientArea(WindowNativeBinding binding) {
        WindowRect rect = toWindowRect(binding);
        if (rect.isEmpty()) {
            return rect;
        }
        return rect.inset(8, 32, 8, 8);
    }
}
