package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Service;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

/**
 * 基于真实屏幕截图的窗口截图服务。
 */
@Service
public class WindowScreenshotService {

    private final GlobalInputLock inputLock;
    private final WindowCoordinateService coordinateService;
    private final Robot robot;

    public WindowScreenshotService(GlobalInputLock inputLock,
                                   WindowCoordinateService coordinateService) throws AWTException {
        this.inputLock = inputLock;
        this.coordinateService = coordinateService;
        this.robot = new Robot();
    }

    public BufferedImage captureWindow(WindowNativeBinding binding) {
        return capture(coordinateService.toWindowRect(binding));
    }

    public BufferedImage captureClientArea(WindowNativeBinding binding) {
        return capture(coordinateService.estimateClientArea(binding));
    }

    public BufferedImage capture(WindowRect rect) {
        if (rect == null || rect.isEmpty()) {
            return null;
        }
        Rectangle awtRect = new Rectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        return inputLock.callLocked(() -> robot.createScreenCapture(awtRect));
    }
}
