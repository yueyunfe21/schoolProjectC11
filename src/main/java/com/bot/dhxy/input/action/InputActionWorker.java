package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.tools.LatencyMetrics;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Single background worker that executes queued physical input requests.
 *
 * <p>The worker is the only consumer of {@link InputActionQueue}. For key-only Alt shortcut
 * sequences it first attempts background hwnd keyboard delivery; mouse actions and exclusive
 * callbacks still require focused real input. The worker binds the request's captured window context
 * while executing so downstream focus/input helpers operate on the correct window.</p>
 */
@Slf4j
@Component
public class InputActionWorker {

    private final InputActionQueue inputActionQueue;
    private final InputActionDeadLetter deadLetter;
    private final InputProvider inputProvider;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final BoundWindowKeyboardService boundWindowKeyboardService;

    /**
     * Create the input worker.
     *
     * @param inputActionQueue producer queue.
     * @param deadLetter failed request recorder.
     * @param inputProvider real physical input provider.
     * @param inputCoordinator window-aware focus/input transaction coordinator.
     * @param windowTaskContextHolder current-window binding holder.
     * @param boundWindowKeyboardService hwnd/background keyboard helper for supported shortcuts.
     */
    public InputActionWorker(InputActionQueue inputActionQueue,
                             InputActionDeadLetter deadLetter,
                             InputProvider inputProvider,
                             WindowAwareInputCoordinator inputCoordinator,
                             WindowTaskContextHolder windowTaskContextHolder,
                             BoundWindowKeyboardService boundWindowKeyboardService) {
        this.inputActionQueue = inputActionQueue;
        this.deadLetter = deadLetter;
        this.inputProvider = inputProvider;
        this.inputCoordinator = inputCoordinator;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.boundWindowKeyboardService = boundWindowKeyboardService;
    }

    /**
     * Start the daemon worker thread after Spring construction.
     */
    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::runLoop, "dhxy-input-action-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("Input action worker started");
    }

    /**
     * Consume input requests until the worker thread is interrupted.
     */
    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            InputActionRequest request;
            try {
                request = inputActionQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            handle(request);
        }
    }

    /**
     * Execute one queued request and complete its waiting future.
     */
    private void handle(InputActionRequest request) {
        long latencyStart = LatencyMetrics.start();
        boolean completed = false;
        try {
            if (request.isCancelled()) {
                log.info("Input request skipped before focus because it was cancelled: windowId={} description={}",
                        request.getWindowId(), request.getDescription());
                request.getResult().complete(false);
                return;
            }
            boolean preferBackgroundKeyboard = canUseBackgroundKeyboard(request);
            boolean focusBeforeInput = request.hasExclusiveCallback() || !preferBackgroundKeyboard;
            Boolean ok = windowTaskContextHolder.callWith(request.getWindowContext(), () ->
                    inputCoordinator.callInputTransaction("queued:" + request.getDescription(), focusBeforeInput, () ->
                            InputActionScope.callWith(request, () -> {
                        if (request.isCancelled()) {
                            return false;
                        }

                        if (request.hasExclusiveCallback()) {
                            return Boolean.TRUE.equals(request.getExclusiveCallback().get());
                        }
                        int actionIndex = 0;
                        for (InputAction action : request.getActions()) {
                            actionIndex++;
                            if (request.isCancelled() || Thread.currentThread().isInterrupted()) {
                                return false;
                            }
                            log.info("[INPUT_TRACE] queued-action request={} windowId={} actionIndex={}/{} action={}",
                                    request.getDescription(), request.getWindowId(), actionIndex,
                                    request.getActions().size(), action);
                            if (!execute(action, preferBackgroundKeyboard)) {
                                return false;
                            }
                        }
                        return true;
                    }))
            );
            completed = Boolean.TRUE.equals(ok);
            request.getResult().complete(completed);
            if (!completed) {
                deadLetter.record(request, null);
            }
        } catch (Throwable e) {
            deadLetter.record(request, e);
            request.getResult().complete(false);
        } finally {
            LatencyMetrics.info(log, "input.request", latencyStart,
                    "result=" + completed + " request=" + request.getDescription()
                            + " windowId=" + request.getWindowId()
                            + " actions=" + request.getActions().size()
                            + " exclusive=" + request.hasExclusiveCallback());
        }
    }

    /**
     * Execute one action. Coordinates in the action are already screen-absolute.
     */
    private boolean execute(InputAction action, boolean preferBackgroundKeyboard) {
        switch (action.getType()) {
            case CLICK_LEFT -> inputProvider.clickLeft(action.getX(), action.getY(), action.getDelayMs());
            case CLICK_RIGHT -> inputProvider.clickRight(action.getX(), action.getY(), action.getDelayMs());
            case DOUBLE_RIGHT_CLICK -> inputProvider.doubleRightClick(action.getX(), action.getY(), action.getDelayMs(), action.getIntervalMs());
            case MOVE_MOUSE -> inputProvider.moveMouse(action.getX(), action.getY());
            case DRAG_AND_DROP -> inputProvider.dragAndDrop(action.getX(), action.getY(), action.getEndX(), action.getEndY());
            case HOLD_CTRL -> inputProvider.holdCtrl();
            case RELEASE_CTRL -> inputProvider.releaseCtrl();
            case PRESS_CTRL_U -> inputProvider.pressCtrlU();
            case TYPE_TEXT_UNICODE -> inputProvider.typeTextUnicode(action.getText());
            case PASTE_TEXT -> inputProvider.pasteText(action.getText());
            case PRESS_ENTER -> inputProvider.pressEnter();
            case PRESS_ALT_1, PRESS_ALT_2, PRESS_ALT_4, PRESS_ALT_6, PRESS_ALT_8,
                    PRESS_ALT_T, PRESS_ALT_O, PRESS_ALT_E, PRESS_ALT_Q, PRESS_ALT_A, PRESS_ALT_C, PRESS_ALT_U ->
                    pressAltShortcut(action.getType(), preferBackgroundKeyboard);
            case SCROLL_DOWN -> inputProvider.scrollDown(action.getClicks());
            case SCROLL_UP -> inputProvider.scrollUp(action.getClicks());
            case SLEEP -> TaskSleep.sleep(action.getDelayMs());
        }
        return !Thread.currentThread().isInterrupted();
    }

    /**
     * Press an Alt shortcut, preferring background hwnd delivery when the sequence is keyboard-only.
     */
    private void pressAltShortcut(InputActionType type, boolean preferBackgroundKeyboard) {
        BoundWindowKeyboardService.AltShortcut shortcut = toAltShortcut(type);
        if (preferBackgroundKeyboard) {
            BoundWindowKeyboardService.ShortcutAttempt attempt = boundWindowKeyboardService.pressShortcut(shortcut);
            if (attempt.attempted() && attempt.success()) {
                return;
            }
            if (attempt.attempted()) {
                log.warn("HWND {} failed, falling back to focused real input: reason={}",
                        shortcutDisplayName(shortcut, type), attempt.reason());
            } else {
                log.debug("HWND {} not attempted, falling back to focused real input: reason={}",
                        shortcutDisplayName(shortcut, type), attempt.reason());
            }
            inputCoordinator.focusCurrentWindowInActiveTransaction("fallback:" + shortcutDisplayName(shortcut, type));
        }
        pressAltShortcutWithRealInput(type);
    }

    /**
     * @return true when every action is either sleep or a supported Alt shortcut.
     */
    private boolean canUseBackgroundKeyboard(InputActionRequest request) {
        if (request.hasExclusiveCallback()) {
            return false;
        }
        if (request.getActions().isEmpty()) {
            return false;
        }
        for (InputAction action : request.getActions()) {
            InputActionType type = action.getType();
            if (type != InputActionType.SLEEP && toAltShortcut(type) == null) {
                return false;
            }
        }
        return true;
    }

    private BoundWindowKeyboardService.AltShortcut toAltShortcut(InputActionType type) {
        return switch (type) {
            case PRESS_ALT_1 -> BoundWindowKeyboardService.AltShortcut.ALT_1;
            case PRESS_ALT_2 -> BoundWindowKeyboardService.AltShortcut.ALT_2;
            case PRESS_ALT_4 -> BoundWindowKeyboardService.AltShortcut.ALT_4;
            case PRESS_ALT_6 -> BoundWindowKeyboardService.AltShortcut.ALT_6;
            case PRESS_ALT_8 -> BoundWindowKeyboardService.AltShortcut.ALT_8;
            case PRESS_ALT_T -> BoundWindowKeyboardService.AltShortcut.ALT_T;
            case PRESS_ALT_O -> BoundWindowKeyboardService.AltShortcut.ALT_O;
            case PRESS_ALT_E -> BoundWindowKeyboardService.AltShortcut.ALT_E;
            case PRESS_ALT_Q -> BoundWindowKeyboardService.AltShortcut.ALT_Q;
            case PRESS_ALT_A -> BoundWindowKeyboardService.AltShortcut.ALT_A;
            case PRESS_ALT_C -> BoundWindowKeyboardService.AltShortcut.ALT_C;
            case PRESS_ALT_U -> BoundWindowKeyboardService.AltShortcut.ALT_U;
            default -> null;
        };
    }

    private String shortcutDisplayName(BoundWindowKeyboardService.AltShortcut shortcut, InputActionType fallbackType) {
        return shortcut == null ? fallbackType.name() : shortcut.displayName();
    }

    private void pressAltShortcutWithRealInput(InputActionType type) {
        switch (type) {
            case PRESS_ALT_1 -> inputProvider.pressAlt1();
            case PRESS_ALT_2 -> inputProvider.pressAlt2();
            case PRESS_ALT_4 -> inputProvider.pressAlt4();
            case PRESS_ALT_6 -> inputProvider.pressAlt6();
            case PRESS_ALT_8 -> inputProvider.pressAlt8();
            case PRESS_ALT_T -> inputProvider.pressAltT();
            case PRESS_ALT_O -> inputProvider.pressAltO();
            case PRESS_ALT_E -> inputProvider.pressAltE();
            case PRESS_ALT_Q -> inputProvider.pressAltQ();
            case PRESS_ALT_A -> inputProvider.pressAltA();
            case PRESS_ALT_C -> inputProvider.pressAltC();
            case PRESS_ALT_U -> inputProvider.pressAltU();
            default -> throw new IllegalArgumentException("Unsupported Alt shortcut: " + type);
        }
    }
}
