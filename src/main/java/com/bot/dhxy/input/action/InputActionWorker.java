package com.bot.dhxy.input.action;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InputActionWorker {

    private final InputActionQueue inputActionQueue;
    private final InputActionDeadLetter deadLetter;
    private final InputProvider inputProvider;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final WindowFocusService windowFocusService;
    private final WindowTaskContextHolder windowTaskContextHolder;

    public InputActionWorker(InputActionQueue inputActionQueue,
                             InputActionDeadLetter deadLetter,
                             InputProvider inputProvider,
                             WindowAwareInputCoordinator inputCoordinator,
                             WindowFocusService windowFocusService,
                             WindowTaskContextHolder windowTaskContextHolder) {
        this.inputActionQueue = inputActionQueue;
        this.deadLetter = deadLetter;
        this.inputProvider = inputProvider;
        this.inputCoordinator = inputCoordinator;
        this.windowFocusService = windowFocusService;
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::runLoop, "dhxy-input-action-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("Input action worker started");
    }

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

    private void handle(InputActionRequest request) {
        try {
            Boolean ok = windowTaskContextHolder.callWith(request.getWindowContext(), () ->
                    inputCoordinator.callInputTransaction("queued:" + request.getDescription(), () -> {
                        if (!windowFocusService.focusWithoutLock(request.getNativeBinding())) {
                            log.warn("Input queue failed to focus window: windowId={} description={}",
                                    request.getWindowId(), request.getDescription());
                            return false;
                        }
                        if (request.hasExclusiveCallback()) {
                            return Boolean.TRUE.equals(request.getExclusiveCallback().get());
                        }
                        for (InputAction action : request.getActions()) {
                            execute(action);
                        }
                        return true;
                    })
            );
            request.getResult().complete(Boolean.TRUE.equals(ok));
            if (!Boolean.TRUE.equals(ok)) {
                deadLetter.record(request, null);
            }
        } catch (Throwable e) {
            deadLetter.record(request, e);
            request.getResult().complete(false);
        }
    }

    private void execute(InputAction action) {
        switch (action.getType()) {
            case CLICK_LEFT -> inputProvider.clickLeft(action.getX(), action.getY(), action.getDelayMs());
            case CLICK_RIGHT -> inputProvider.clickRight(action.getX(), action.getY(), action.getDelayMs());
            case DOUBLE_RIGHT_CLICK -> inputProvider.doubleRightClick(
                    action.getX(), action.getY(), action.getDelayMs(), action.getIntervalMs());
            case MOVE_MOUSE -> inputProvider.moveMouse(action.getX(), action.getY());
            case DRAG_AND_DROP -> inputProvider.dragAndDrop(action.getX(), action.getY(), action.getEndX(), action.getEndY());
            case HOLD_CTRL -> inputProvider.holdCtrl();
            case RELEASE_CTRL -> inputProvider.releaseCtrl();
            case TYPE_TEXT_UNICODE -> inputProvider.typeTextUnicode(action.getText());
            case PASTE_TEXT -> inputProvider.pasteText(action.getText());
            case PRESS_ENTER -> inputProvider.pressEnter();
            case PRESS_ALT_1 -> inputProvider.pressAlt1();
            case PRESS_ALT_2 -> inputProvider.pressAlt2();
            case PRESS_ALT_4 -> inputProvider.pressAlt4();
            case PRESS_ALT_8 -> inputProvider.pressAlt8();
            case PRESS_ALT_E -> inputProvider.pressAltE();
            case PRESS_ALT_Q -> inputProvider.pressAltQ();
            case SCROLL_DOWN -> inputProvider.scrollDown(action.getClicks());
            case SCROLL_UP -> inputProvider.scrollUp(action.getClicks());
            case SLEEP -> sleepQuietly(action.getDelayMs());
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
