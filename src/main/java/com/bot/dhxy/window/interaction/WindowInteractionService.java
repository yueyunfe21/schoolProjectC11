package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

/**
 * 单个窗口交互门面。
 */
@Service
public class WindowInteractionService {

    private final WindowFocusService focusService;
    private final WindowMouseService mouseService;
    private final WindowScreenshotService screenshotService;
    private final WindowCoordinateService coordinateService;

    public WindowInteractionService(WindowFocusService focusService,
                                    WindowMouseService mouseService,
                                    WindowScreenshotService screenshotService,
                                    WindowCoordinateService coordinateService) {
        this.focusService = focusService;
        this.mouseService = mouseService;
        this.screenshotService = screenshotService;
        this.coordinateService = coordinateService;
    }

    public boolean focus(WindowNativeBinding binding) {
        return focusService.focus(binding);
    }

    public void focusAndClickCenter(WindowNativeBinding binding) {
        focus(binding);
        mouseService.clickCenter(binding);
    }

    public void focusAndClickRelative(WindowNativeBinding binding, int relativeX, int relativeY) {
        focus(binding);
        mouseService.clickRelative(binding, relativeX, relativeY);
    }

    public BufferedImage captureClientArea(WindowNativeBinding binding) {
        return screenshotService.captureClientArea(binding);
    }

    public WindowRect clientArea(WindowNativeBinding binding) {
        return coordinateService.estimateClientArea(binding);
    }
}
