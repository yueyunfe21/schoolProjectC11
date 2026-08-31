package com.bot.dhxy.driver.fakerinput;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Foreground {@link InputProvider} backed by FakerInput virtual HID reports.
 *
 * <p>Screen-absolute caller coordinates are scaled to physical pixels with the existing DHXY rule, then
 * mapped to FakerInput absolute HID coordinates and verified through cursor read-back. FakerInput is the
 * default backend; a missing driver fails application startup instead of falling back to {@code SendInput}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bot.input", name = "backend", havingValue = "FAKER_INPUT", matchIfMissing = true)
public class FakerInputProvider implements InputProvider {

    private static final int MODIFIER_LEFT_CTRL = 0x01;
    private static final int MODIFIER_LEFT_ALT = 0x04;
    private static final int BUTTON_LEFT = 0x01;
    private static final int BUTTON_RIGHT = 0x02;
    private static final int MOVE_ATTEMPTS_PER_WARNING = 20;
    private static final int MOVE_RETRY_DELAY_MS = 50;

    private static final byte KEY_A = 0x04;
    private static final byte KEY_B = 0x05;
    private static final byte KEY_C = 0x06;
    private static final byte KEY_E = 0x08;
    private static final byte KEY_O = 0x12;
    private static final byte KEY_Q = 0x14;
    private static final byte KEY_T = 0x17;
    private static final byte KEY_U = 0x18;
    /** HID 用法码：空格。Ctrl+Space 用于把输入法切回英文（用户 2026-08-21 拍板）。 */
    private static final byte KEY_SPACE = 0x2C;
    private static final byte KEY_V = 0x19;
    private static final byte KEY_1 = 0x1E;
    private static final byte KEY_2 = 0x1F;
    private static final byte KEY_4 = 0x21;
    private static final byte KEY_5 = 0x22;
    private static final byte KEY_6 = 0x23;
    private static final byte KEY_8 = 0x25;
    private static final byte KEY_ENTER = 0x28;
    private static final byte KEY_ESCAPE = 0x29;

    private final FakerInputDevice device;
    private final CoordinateHelper coordinateHelper;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final Set<Byte> heldKeys = new LinkedHashSet<>();
    private int heldModifiers;
    private int heldMouseButtons;

    /** Connect only when configuration explicitly selects this backend. */
    @PostConstruct
    public void start() {
        FakerInputDeviceStatus status = device.connect();
        if (status.state() != FakerInputDeviceState.DRIVER_READY) {
            throw new IllegalStateException("FakerInput backend selected but unavailable: " + status);
        }
    }

    @Override
    public boolean requiresForegroundKeyboard() {
        return true;
    }

    @Override
    public void clickLeft(int x, int y, int delayMs) {
        inputCoordinator.runInput("fakerInput:clickLeft", () -> click(x, y, BUTTON_LEFT, delayMs));
    }

    @Override
    public void clickRight(int x, int y, int delayMs) {
        inputCoordinator.runInput("fakerInput:clickRight", () -> click(x, y, BUTTON_RIGHT, delayMs));
    }

    @Override
    public void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs) {
        inputCoordinator.runInput("fakerInput:doubleRightClick", () -> {
            click(x, y, BUTTON_RIGHT, clickDelayMs);
            TaskSleep.sleep(intervalMs);
            click(x, y, BUTTON_RIGHT, clickDelayMs);
        });
    }

    @Override
    public void moveMouse(int x, int y) {
        inputCoordinator.runInput("fakerInput:moveMouse", () -> glideToLogicalPoint(x, y));
    }

    @Override
    public void holdCtrl() {
        inputCoordinator.runInput("fakerInput:holdCtrl", () -> setModifier(MODIFIER_LEFT_CTRL, true));
    }

    @Override
    public void releaseCtrl() {
        inputCoordinator.runInput("fakerInput:releaseCtrl", () -> setModifier(MODIFIER_LEFT_CTRL, false));
    }

    @Override
    public void pressCtrlU() {
        pressShortcut("Ctrl+U", MODIFIER_LEFT_CTRL, KEY_U);
    }

    @Override
    public void pressCtrlA() {
        pressShortcut("Ctrl+A", MODIFIER_LEFT_CTRL, KEY_A);
    }

    @Override
    public void pressCtrlSpace() {
        pressShortcut("Ctrl+Space", MODIFIER_LEFT_CTRL, KEY_SPACE);
    }

    @Override
    public void pressAlt1() {
        pressShortcut("Alt+1", MODIFIER_LEFT_ALT, KEY_1);
    }

    @Override
    public void pressAlt2() {
        pressShortcut("Alt+2", MODIFIER_LEFT_ALT, KEY_2);
    }

    @Override
    public void pressAlt4() {
        pressShortcut("Alt+4", MODIFIER_LEFT_ALT, KEY_4);
    }

    @Override
    public void pressAlt5() {
        pressShortcut("Alt+5", MODIFIER_LEFT_ALT, KEY_5);
    }

    @Override
    public void pressAlt6() {
        pressShortcut("Alt+6", MODIFIER_LEFT_ALT, KEY_6);
    }

    @Override
    public void pressAlt8() {
        pressShortcut("Alt+8", MODIFIER_LEFT_ALT, KEY_8);
    }

    @Override
    public void pressAltT() {
        pressShortcut("Alt+T", MODIFIER_LEFT_ALT, KEY_T);
    }

    @Override
    public void pressAltU() {
        pressShortcut("Alt+U", MODIFIER_LEFT_ALT, KEY_U);
    }

    @Override
    public void pressAltO() {
        pressShortcut("Alt+O", MODIFIER_LEFT_ALT, KEY_O);
    }

    @Override
    public void pressAltE() {
        pressShortcut("Alt+E", MODIFIER_LEFT_ALT, KEY_E);
    }

    @Override
    public void pressAltQ() {
        pressShortcut("Alt+Q", MODIFIER_LEFT_ALT, KEY_Q);
    }

    @Override
    public void pressAltA() {
        pressShortcut("Alt+A", MODIFIER_LEFT_ALT, KEY_A);
    }

    @Override
    public void pressAltB() {
        pressShortcut("Alt+B", MODIFIER_LEFT_ALT, KEY_B);
    }

    @Override
    public void pressAltC() {
        pressShortcut("Alt+C", MODIFIER_LEFT_ALT, KEY_C);
    }

    @Override
    public void pressEnter() {
        pressKey("Enter", KEY_ENTER);
    }

    @Override
    public void pressEscape() {
        pressKey("Escape", KEY_ESCAPE);
    }

    @Override
    public void pasteText(String text) {
        if (text == null) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        pressShortcut("Ctrl+V", MODIFIER_LEFT_CTRL, KEY_V);
    }

    @Override
    public void typeTextUnicode(String text) {
        // HID keyboard reports carry usages, not Unicode. Clipboard plus driver Ctrl+V preserves Unicode text
        // without reintroducing a SendInput or PostMessage keyboard fallback.
        pasteText(text);
    }

    @Override
    public void typeTextAscii(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        inputCoordinator.runInput("fakerInput:typeTextAscii", () -> {
            // Alt shortcuts run immediately before world-map ASCII input. Publish an explicit all-released
            // HID state first so a missed/delayed Alt-up report can never turn a token such as "l" into Alt+L.
            synchronized (this) {
                heldModifiers = 0;
                heldKeys.clear();
                device.releaseAll();
                TaskSleep.sleep(150);
            }
            for (int i = 0; i < text.length(); i++) {
                char value = text.charAt(i);
                byte usage;
                if (value >= 'a' && value <= 'z') {
                    usage = (byte) (0x04 + value - 'a');
                } else if (value >= '1' && value <= '9') {
                    usage = (byte) (0x1E + value - '1');
                } else if (value == '0') {
                    usage = 0x27;
                } else {
                    throw new IllegalArgumentException("FakerInput ASCII text accepts only lowercase a-z and 0-9");
                }
                pressKeyDirect(usage);
            }
        });
    }

    @Override
    public void scrollDown(int clicks) {
        scroll(-Math.max(0, clicks));
    }

    @Override
    public void scrollUp(int clicks) {
        scroll(Math.max(0, clicks));
    }

    @Override
    public void dragAndDrop(int startX, int startY, int endX, int endY) {
        inputCoordinator.runInput("fakerInput:dragAndDrop", () -> {
            glideToLogicalPoint(startX, startY);
            TaskSleep.sleep(200);
            setMouseButton(BUTTON_LEFT, true);
            try {
                TaskSleep.sleep(300);
                for (int i = 1; i <= 25; i++) {
                    moveToLogicalPoint(
                            startX + (endX - startX) * i / 25,
                            startY + (endY - startY) * i / 25);
                    TaskSleep.sleep(15);
                }
                TaskSleep.sleep(200);
            } finally {
                setMouseButton(BUTTON_LEFT, false);
            }
            TaskSleep.sleep(150);
        });
    }

    @Override
    public void holdSweepWithoutRelease(
            int startX, int startY, int leftX, int rightX, int endY, int rowStepPx) {
        inputCoordinator.runInput("fakerInput:holdSweep", () -> {
            glideToLogicalPoint(startX, startY);
            TaskSleep.sleep(120);
            setMouseButton(BUTTON_LEFT, true);
            TaskSleep.sleep(150);
            sweepRows(leftX, rightX, startY, endY, rowStepPx);
        });
    }

    @Override
    public void sweepWhileLeftHeld(
            int startX, int startY, int leftX, int rightX, int endY, int rowStepPx) {
        inputCoordinator.runInput("fakerInput:sweepWhileLeftHeld", () -> {
            glideToLogicalPoint(startX, startY);
            TaskSleep.sleep(120);
            sweepRows(leftX, rightX, startY, endY, rowStepPx);
        });
    }

    @Override
    public void releaseLeftButton() {
        inputCoordinator.runInput("fakerInput:releaseLeftButton", () -> setMouseButton(BUTTON_LEFT, false));
    }

    @Override
    public synchronized void releaseAllInput() {
        if (heldModifiers == 0 && heldKeys.isEmpty() && heldMouseButtons == 0) {
            return;
        }
        heldModifiers = 0;
        heldKeys.clear();
        heldMouseButtons = 0;
        device.releaseAll();
    }

    private void click(int x, int y, int button, int delayMs) {
        glideToLogicalPoint(x, y);
        setMouseButton(button, true);
        try {
            TaskSleep.sleep(Math.max(0, delayMs));
        } finally {
            setMouseButton(button, false);
        }
    }

    private synchronized void setModifier(int modifier, boolean down) {
        heldModifiers = down ? heldModifiers | modifier : heldModifiers & ~modifier;
        publishKeyboard();
    }

    private void pressShortcut(String label, int modifier, byte key) {
        inputCoordinator.runInput("fakerInput:" + label, () -> {
            synchronized (this) {
                int originalModifiers = heldModifiers;
                heldModifiers |= modifier;
                try {
                    publishKeyboard();
                    TaskSleep.sleep(60);
                    heldKeys.add(key);
                    publishKeyboard();
                    TaskSleep.sleep(80);
                    heldKeys.remove(key);
                    publishKeyboard();
                    TaskSleep.sleep(60);
                } finally {
                    heldKeys.remove(key);
                    heldModifiers = originalModifiers;
                    publishKeyboard();
                    TaskSleep.sleep(60);
                }
            }
        });
    }

    private void pressKey(String label, byte key) {
        inputCoordinator.runInput("fakerInput:" + label, () -> {
            pressKeyDirect(key);
        });
    }

    private synchronized void pressKeyDirect(byte key) {
        try {
            heldKeys.add(key);
            publishKeyboard();
            TaskSleep.sleep(60);
        } finally {
            heldKeys.remove(key);
            publishKeyboard();
            TaskSleep.sleep(60);
        }
    }

    private synchronized void publishKeyboard() {
        byte[] usages = new byte[heldKeys.size()];
        int index = 0;
        for (byte usage : heldKeys) {
            usages[index++] = usage;
        }
        device.updateKeyboard(heldModifiers, usages);
    }

    private synchronized void setMouseButton(int button, boolean down) {
        heldMouseButtons = down ? heldMouseButtons | button : heldMouseButtons & ~button;
        device.updateRelativeMouse(heldMouseButtons, 0, 0, 0, 0);
    }

    private void scroll(int clicks) {
        inputCoordinator.runInput("fakerInput:scroll", () -> {
            int remaining = clicks;
            while (remaining != 0) {
                int chunk = Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, remaining));
                synchronized (this) {
                    device.updateRelativeMouse(heldMouseButtons, 0, 0, chunk, 0);
                }
                remaining -= chunk;
                TaskSleep.sleep(50);
            }
        });
    }

    /** Same user decision as the WinApi backend (2026-08-17/18): below this a jump is human. */
    private static final int GLIDE_MIN_DISTANCE_PX = 80;

    /**
     * Human-like glide for the ACTIVE FakerInput backend — the earlier glide only covered the
     * inactive WinApi backend, so no visible change reached the game. Long moves ride 3~6 waypoint
     * HID reports (~40-150ms, 1-2px jitter, never on the endpoint); the final step still goes
     * through {@link #moveToLogicalPoint} with its verify/retry, so endpoint contracts are unchanged.
     */
    private void glideToLogicalPoint(int logicalX, int logicalY) {
        double scale = coordinateHelper.getScaleRatio();
        int targetPhysX = (int) Math.round(logicalX * scale);
        int targetPhysY = (int) Math.round(logicalY * scale);
        POINT current = new POINT();
        if (User32.INSTANCE.GetCursorPos(current)) {
            double distance = Math.hypot(targetPhysX - current.x, targetPhysY - current.y);
            if (distance > GLIDE_MIN_DISTANCE_PX) {
                int steps = (int) Math.min(6L, Math.max(3L, Math.round(distance / 150.0)));
                int totalMs = (int) Math.min(150L, 40L + Math.round(distance / 12.0));
                int stepDelayMs = Math.max(8, totalMs / steps);
                java.util.concurrent.ThreadLocalRandom random =
                        java.util.concurrent.ThreadLocalRandom.current();
                for (int step = 1; step < steps; step++) {
                    moveWaypointPhysical(
                            current.x + (int) ((targetPhysX - current.x) * (double) step / steps)
                                    + random.nextInt(-2, 3),
                            current.y + (int) ((targetPhysY - current.y) * (double) step / steps)
                                    + random.nextInt(-2, 3));
                    if (!TaskSleep.sleep(stepDelayMs)) {
                        break;
                    }
                }
            }
        }
        moveToLogicalPoint(logicalX, logicalY);
    }

    /** One unverified waypoint report; only the endpoint needs the converge/verify loop. */
    private void moveWaypointPhysical(int physX, int physY) {
        int screenWidth = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN);
        int screenHeight = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN);
        if (screenWidth <= 1 || screenHeight <= 1) {
            return;
        }
        int clampedX = Math.max(0, Math.min(physX, screenWidth - 1));
        int clampedY = Math.max(0, Math.min(physY, screenHeight - 1));
        synchronized (this) {
            device.updateAbsoluteMouse(heldMouseButtons,
                    normalizeAbsoluteCoordinate(clampedX, screenWidth),
                    normalizeAbsoluteCoordinate(clampedY, screenHeight), 0);
        }
    }

    private void moveToLogicalPoint(int logicalX, int logicalY) {
        double scale = coordinateHelper.getScaleRatio();
        int targetX = (int) Math.round(logicalX * scale);
        int targetY = (int) Math.round(logicalY * scale);
        int screenWidth = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXSCREEN);
        int screenHeight = User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYSCREEN);
        if (screenWidth <= 1 || screenHeight <= 1) {
            throw new IllegalStateException("FakerInput could not read primary screen bounds");
        }
        if (targetX < 0 || targetX >= screenWidth || targetY < 0 || targetY >= screenHeight) {
            throw new IllegalStateException("FakerInput absolute target is outside the primary screen: target=("
                    + targetX + "," + targetY + ") screen=(" + screenWidth + "," + screenHeight + ")");
        }
        int absoluteX = normalizeAbsoluteCoordinate(targetX, screenWidth);
        int absoluteY = normalizeAbsoluteCoordinate(targetY, screenHeight);
        POINT current = new POINT();
        int attempt = 0;
        while (true) {
            attempt++;
            synchronized (this) {
                device.updateAbsoluteMouse(heldMouseButtons, absoluteX, absoluteY, 0);
            }
            TaskSleep.sleep(MOVE_RETRY_DELAY_MS);
            if (!User32.INSTANCE.GetCursorPos(current)) {
                throw new IllegalStateException("FakerInput could not read the current cursor position");
            }
            log.info("[INPUT_CURSOR_TRACE] backend=FAKER_INPUT mode=ABSOLUTE logical=({}, {}) physical=({}, {}) "
                            + "actual=({}, {}) normalized=({}, {}) primaryScreen=({}, {}) attempt={}/{}",
                    logicalX, logicalY, targetX, targetY, current.x, current.y, absoluteX, absoluteY,
                    screenWidth, screenHeight, attempt, MOVE_ATTEMPTS_PER_WARNING);
            if (Math.abs(current.x - targetX) <= 1 && Math.abs(current.y - targetY) <= 1) {
                return;
            }

            // FakerInput's absolute report can converge in several physical steps or be displaced by a late
            // report. Correct the observed residual through the same driver before issuing the next absolute
            // anchor; the final <=1px gate remains unchanged.
            int correctionX = targetX - current.x;
            int correctionY = targetY - current.y;
            synchronized (this) {
                device.updateRelativeMouse(heldMouseButtons, correctionX, correctionY, 0, 0);
            }
            TaskSleep.sleep(MOVE_RETRY_DELAY_MS);
            if (!User32.INSTANCE.GetCursorPos(current)) {
                throw new IllegalStateException("FakerInput could not read the current cursor position");
            }
            log.info("[INPUT_CURSOR_TRACE] backend=FAKER_INPUT mode=RELATIVE_CORRECTION logical=({}, {}) "
                            + "physical=({}, {}) actual=({}, {}) correction=({}, {}) attempt={}",
                    logicalX, logicalY, targetX, targetY, current.x, current.y,
                    correctionX, correctionY, attempt);
            if (Math.abs(current.x - targetX) <= 1 && Math.abs(current.y - targetY) <= 1) {
                return;
            }
            if (attempt % MOVE_ATTEMPTS_PER_WARNING == 0) {
                log.warn("FakerInput cursor is still converging; retrying without dropping input: "
                                + "target=({}, {}) actual=({}, {}) attempts={}",
                        targetX, targetY, current.x, current.y, attempt);
            }
        }
    }

    static int normalizeAbsoluteCoordinate(int physicalPixel, int screenSpan) {
        if (screenSpan <= 1) {
            throw new IllegalArgumentException("screenSpan must be greater than one");
        }
        if (physicalPixel < 0 || physicalPixel >= screenSpan) {
            throw new IllegalArgumentException("physicalPixel is outside the screen: " + physicalPixel);
        }
        return (int) Math.round(physicalPixel * (double) Short.MAX_VALUE / (screenSpan - 1));
    }

    private void sweepRows(int leftX, int rightX, int startY, int endY, int rowStepPx) {
        int step = Math.max(1, rowStepPx);
        for (int y = startY; y <= endY; y += step) {
            for (int roundTrip = 0; roundTrip < 2; roundTrip++) {
                moveToLogicalPoint(leftX, y);
                TaskSleep.sleep(15);
                moveToLogicalPoint(rightX, y);
                TaskSleep.sleep(15);
                moveToLogicalPoint(leftX, y);
                TaskSleep.sleep(15);
            }
        }
    }
}
