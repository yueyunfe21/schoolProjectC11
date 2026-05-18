package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.model.WindowNativeBinding;
import org.springframework.stereotype.Service;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

/**
 * 基于真实鼠标的窗口点击服务。
 */
@Service
public class WindowMouseService {

    private final GlobalInputLock inputLock;
    private final WindowCoordinateService coordinateService;
    private final Robot robot;

    public WindowMouseService(GlobalInputLock inputLock,
                              WindowCoordinateService coordinateService) throws AWTException {
        this.inputLock = inputLock;
        this.coordinateService = coordinateService;
        this.robot = new Robot();
        this.robot.setAutoDelay(40);
    }

    public void clickCenter(WindowNativeBinding binding) {
        WindowPoint point = coordinateService.center(binding);
        clickScreen(point);
    }

    public void clickRelative(WindowNativeBinding binding, int relativeX, int relativeY) {
        WindowPoint point = coordinateService.toScreenPoint(binding, relativeX, relativeY);
        clickScreen(point);
    }

    public void clickScreen(WindowPoint point) {
        if (point == null) {
            return;
        }
        inputLock.runLocked(() -> {
            robot.mouseMove(point.getX(), point.getY());
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(60);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(80);
        });
    }

    public void moveTo(WindowNativeBinding binding, int relativeX, int relativeY) {
        WindowPoint point = coordinateService.toScreenPoint(binding, relativeX, relativeY);
        moveToScreen(point);
    }

    public void moveToScreen(WindowPoint point) {
        if (point == null) {
            return;
        }
        inputLock.runLocked(() -> robot.mouseMove(point.getX(), point.getY()));
    }
}
