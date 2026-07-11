package com.bot.dhxy.input.action;

import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;

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
            if (!waitIfPaused(request, "before-focus")) {
                request.getResult().complete(false);
                deadLetter.record(request, null);
                return;
            }
            if (request.isCancelled()) {
                log.info("Input request skipped before focus because it was cancelled: windowId={} description={}",
                        request.getWindowId(), request.getDescription());
                request.getResult().complete(false);
                return;
            }
            if (!isPlayerIdentityEpochCurrent(request, "before-focus")) {
                request.getResult().complete(false);
                deadLetter.record(request, null);
                return;
            }
            boolean preferBackgroundKeyboard = canUseBackgroundKeyboard(request);
            boolean focusBeforeInput = request.hasExclusiveCallback() || !preferBackgroundKeyboard;
            Boolean ok = windowTaskContextHolder.callWith(request.getWindowContext(), () ->
                    inputCoordinator.callInputTransaction("queued:" + request.getDescription(), false, () -> {
                        if (focusBeforeInput) {
                            if (!waitIfPaused(request, "before-transaction-focus")
                                    || request.isCancelled()
                                    || !isPlayerIdentityEpochCurrent(request, "before-transaction-focus")) {
                                return false;
                            }
                            inputCoordinator.focusCurrentWindowInActiveTransaction("queued:" + request.getDescription());
                        }
                        return InputActionScope.callWith(request, () -> {
                        if (!waitIfPaused(request, "before-actions")
                                || request.isCancelled()
                                || !isPlayerIdentityEpochCurrent(request, "before-actions")) {
                            return false;
                        }

                        if (request.hasExclusiveCallback()) {
                            if (!waitIfPaused(request, "before-exclusive-callback")
                                    || !isPlayerIdentityEpochCurrent(request, "before-exclusive-callback")) {
                                return false;
                            }
                            return Boolean.TRUE.equals(request.getExclusiveCallback().get());
                        }
                        int actionIndex = 0;
                        for (InputAction action : request.getActions()) {
                            actionIndex++;
                            if (!waitIfPaused(request, "action-" + actionIndex)
                                    || request.isCancelled()
                                    || Thread.currentThread().isInterrupted()
                                    || !isPlayerIdentityEpochCurrent(request, "action-" + actionIndex)) {
                                return false;
                            }
                            log.info("[INPUT_TRACE] queued-action request={} windowId={} actionIndex={}/{} action={}",
                                    request.getDescription(), request.getWindowId(), actionIndex,
                                    request.getActions().size(), action);
                            if (!execute(request, action, preferBackgroundKeyboard, "action-" + actionIndex)) {
                                return false;
                            }
                        }
                        return true;
                    });
                    })
            );
            completed = Boolean.TRUE.equals(ok);
            request.getResult().complete(completed);
            if (!completed) {
                deadLetter.record(request, null);
            }
        } catch (TaskStopRequestedException e) {
            request.cancel("task-stop:" + e.getMessage());
            deadLetter.record(request, e);
            request.getResult().complete(false);
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
    private boolean execute(InputActionRequest request, InputAction action, boolean preferBackgroundKeyboard, String stage) {
        InputActionType type = action.getType();
        if (type == InputActionType.CLICK_LEFT) {
            inputProvider.clickLeft(action.getX(), action.getY(), action.getDelayMs());
        } else if (type == InputActionType.CLICK_RIGHT) {
            inputProvider.clickRight(action.getX(), action.getY(), action.getDelayMs());
        } else if (type == InputActionType.DOUBLE_RIGHT_CLICK) {
            inputProvider.doubleRightClick(action.getX(), action.getY(), action.getDelayMs(), action.getIntervalMs());
        } else if (type == InputActionType.MOVE_MOUSE) {
            inputProvider.moveMouse(action.getX(), action.getY());
        } else if (type == InputActionType.DRAG_AND_DROP) {
            inputProvider.dragAndDrop(action.getX(), action.getY(), action.getEndX(), action.getEndY());
        } else if (type == InputActionType.HOLD_CTRL) {
            inputProvider.holdCtrl();
        } else if (type == InputActionType.RELEASE_CTRL) {
            inputProvider.releaseCtrl();
        } else if (type == InputActionType.PRESS_CTRL_U) {
            inputProvider.pressCtrlU();
        } else if (type == InputActionType.TYPE_TEXT_UNICODE) {
            inputProvider.typeTextUnicode(action.getText());
        } else if (type == InputActionType.PASTE_TEXT) {
            inputProvider.pasteText(action.getText());
        } else if (type == InputActionType.PRESS_ENTER) {
            inputProvider.pressEnter();
        } else if (isAltShortcutAction(type)) {
            return pressAltShortcut(request, type, preferBackgroundKeyboard, stage);
        } else if (type == InputActionType.SCROLL_DOWN) {
            inputProvider.scrollDown(action.getClicks());
        } else if (type == InputActionType.SCROLL_UP) {
            inputProvider.scrollUp(action.getClicks());
        } else if (type == InputActionType.SLEEP) {
            TaskSleep.sleep(action.getDelayMs());
        } else {
            throw new IllegalArgumentException("Unsupported input action: " + type);
        }
        return !Thread.currentThread().isInterrupted();
    }

    private boolean waitIfPaused(InputActionRequest request, String stage) {
        if (!request.isPauseRequested()) {
            return true;
        }
        log.info("Input request paused; waiting before continuing: windowId={} description={} stage={}",
                request.getWindowId(), request.getDescription(), stage);
        long blockedMs = request.getPauseToken().waitIfPaused(request.getStopToken());
        log.info("Input request resumed; continuing same sequence: windowId={} description={} stage={} blockedMs={}",
                request.getWindowId(), request.getDescription(), stage, blockedMs);
        return !request.isCancelled() && !Thread.currentThread().isInterrupted();
    }

    private boolean isPlayerIdentityEpochCurrent(InputActionRequest request, String stage) {
        if (request.isPlayerIdentityEpochCurrent()) {
            return true;
        }
        long currentEpoch = request.getWindowContext() == null
                ? -1L
                : request.getWindowContext().getPlayerIdentityEpoch();
        log.warn("Input request skipped because player identity epoch changed: windowId={} description={} stage={} requestEpoch={} currentEpoch={}",
                request.getWindowId(), request.getDescription(), stage,
                request.getPlayerIdentityEpoch(), currentEpoch);
        request.cancel("player-identity-epoch-changed:" + stage);
        return false;
    }

    /**
     * Press an Alt shortcut, preferring background hwnd delivery when the sequence is keyboard-only.
     */
    private boolean pressAltShortcut(InputActionRequest request,
                                     InputActionType type,
                                     boolean preferBackgroundKeyboard,
                                     String stage) {
        BoundWindowKeyboardService.AltShortcut shortcut = toAltShortcut(type);
        if (preferBackgroundKeyboard) {
            BoundWindowKeyboardService.ShortcutAttempt attempt = boundWindowKeyboardService.pressShortcut(shortcut);
            if (attempt.attempted() && attempt.success()) {
                return true;
            }
            if (attempt.terminalFailure()) {
                log.warn("HWND {} terminally rejected; skip focused real-input fallback: reason={}",
                        shortcutDisplayName(shortcut, type), attempt.reason());
                return false;
            }
            if (attempt.attempted()) {
                log.warn("HWND {} failed, falling back to focused real input: reason={}",
                        shortcutDisplayName(shortcut, type), attempt.reason());
            } else {
                log.debug("HWND {} not attempted, falling back to focused real input: reason={}",
                        shortcutDisplayName(shortcut, type), attempt.reason());
            }
            if (!waitIfPaused(request, stage + "-before-focused-fallback")) {
                return false;
            }
            inputCoordinator.focusCurrentWindowInActiveTransaction("fallback:" + shortcutDisplayName(shortcut, type));
        }
        pressAltShortcutWithRealInput(type);
        return !Thread.currentThread().isInterrupted();
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
        if (type == InputActionType.PRESS_ALT_1) {
            return BoundWindowKeyboardService.AltShortcut.ALT_1;
        }
        if (type == InputActionType.PRESS_ALT_2) {
            return BoundWindowKeyboardService.AltShortcut.ALT_2;
        }
        if (type == InputActionType.PRESS_ALT_4) {
            return BoundWindowKeyboardService.AltShortcut.ALT_4;
        }
        if (type == InputActionType.PRESS_ALT_6) {
            return BoundWindowKeyboardService.AltShortcut.ALT_6;
        }
        if (type == InputActionType.PRESS_ALT_8) {
            return BoundWindowKeyboardService.AltShortcut.ALT_8;
        }
        if (type == InputActionType.PRESS_ALT_T) {
            return BoundWindowKeyboardService.AltShortcut.ALT_T;
        }
        if (type == InputActionType.PRESS_ALT_O) {
            return BoundWindowKeyboardService.AltShortcut.ALT_O;
        }
        if (type == InputActionType.PRESS_ALT_E) {
            return BoundWindowKeyboardService.AltShortcut.ALT_E;
        }
        if (type == InputActionType.PRESS_ALT_Q) {
            return BoundWindowKeyboardService.AltShortcut.ALT_Q;
        }
        return null;
    }

    private String shortcutDisplayName(BoundWindowKeyboardService.AltShortcut shortcut, InputActionType fallbackType) {
        return shortcut == null ? fallbackType.name() : shortcut.displayName();
    }

    private void pressAltShortcutWithRealInput(InputActionType type) {
        if (type == InputActionType.PRESS_ALT_1) {
            inputProvider.pressAlt1();
        } else if (type == InputActionType.PRESS_ALT_2) {
            inputProvider.pressAlt2();
        } else if (type == InputActionType.PRESS_ALT_4) {
            inputProvider.pressAlt4();
        } else if (type == InputActionType.PRESS_ALT_6) {
            inputProvider.pressAlt6();
        } else if (type == InputActionType.PRESS_ALT_8) {
            inputProvider.pressAlt8();
        } else if (type == InputActionType.PRESS_ALT_T) {
            inputProvider.pressAltT();
        } else if (type == InputActionType.PRESS_ALT_O) {
            inputProvider.pressAltO();
        } else if (type == InputActionType.PRESS_ALT_E) {
            inputProvider.pressAltE();
        } else if (type == InputActionType.PRESS_ALT_Q) {
            inputProvider.pressAltQ();
        } else if (type == InputActionType.PRESS_ALT_A) {
            inputProvider.pressAltA();
        } else if (type == InputActionType.PRESS_ALT_C) {
            inputProvider.pressAltC();
        } else if (type == InputActionType.PRESS_ALT_U) {
            inputProvider.pressAltU();
        } else {
            throw new IllegalArgumentException("Unsupported Alt shortcut: " + type);
        }
    }

    private boolean isAltShortcutAction(InputActionType type) {
        return type == InputActionType.PRESS_ALT_1
                || type == InputActionType.PRESS_ALT_2
                || type == InputActionType.PRESS_ALT_4
                || type == InputActionType.PRESS_ALT_6
                || type == InputActionType.PRESS_ALT_8
                || type == InputActionType.PRESS_ALT_T
                || type == InputActionType.PRESS_ALT_O
                || type == InputActionType.PRESS_ALT_E
                || type == InputActionType.PRESS_ALT_Q
                || type == InputActionType.PRESS_ALT_A
                || type == InputActionType.PRESS_ALT_C
                || type == InputActionType.PRESS_ALT_U;
    }
}
