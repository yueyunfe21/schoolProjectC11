package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.bot.dhxy.cloud.turn.protocol.TurnXinshouDragArguments;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionQueue;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.runner.stop.TaskPauseToken;
import com.bot.dhxy.runner.stop.TaskStopToken;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 新手 §8.2 retained hold-sweep executor.
 *
 * <p>The first sweep opens one retained {@link InputActionQueue} request. Its single input worker
 * presses LEFT_DOWN, executes every later sweep/capture callback from Cloud without releasing the
 * global input transaction, and performs LEFT_UP from that same worker's {@code finally}. This
 * executor stores only the opaque handle needed to submit the next callback or terminal signal.</p>
 */
@Component
public final class XinshouDragLocalOperationExecutor {

    private static final Logger log = LoggerFactory.getLogger(XinshouDragLocalOperationExecutor.class);

    private final InputProvider mouseController;
    private final CoordinateHelper coordinateHelper;
    private final GameClientTracker tracker;
    private final InputActionQueue inputActionQueue;
    private final WindowTaskContextHolder contextHolder;
    private final Object retainedLock = new Object();
    private ActiveDrag activeDrag;

    public XinshouDragLocalOperationExecutor(InputProvider mouseController,
                                             CoordinateHelper coordinateHelper,
                                             GameClientTracker tracker,
                                             InputActionQueue inputActionQueue,
                                             WindowTaskContextHolder contextHolder) {
        this.mouseController = Objects.requireNonNull(mouseController, "mouseController");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.inputActionQueue = Objects.requireNonNull(inputActionQueue, "inputActionQueue");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
    }

    /**
     * Submit one Cloud sweep round to the single retained input-worker transaction.
     *
     * @param call typed sweep arguments from Cloud
     * @param pauseToken live pause token of the exact task owning this action
     * @param stopToken live stop token of the exact task owning this action
     * @param taskStillCurrent exact task-handle identity predicate
     * @return one sweep/capture result; the left button remains held until release or worker cleanup
     */
    public LocalServiceExecution sweep(TurnLocalServiceCall call,
                                       TaskPauseToken pauseToken,
                                       TaskStopToken stopToken,
                                       BooleanSupplier taskStillCurrent) {
        TurnXinshouDragArguments drag = call == null ? null : call.xinshouDrag();
        if (drag == null || pauseToken == null || stopToken == null || taskStillCurrent == null) {
            return LocalServiceExecution.failed("INVALID_XINSHOU_DRAG_CALL", null);
        }

        synchronized (retainedLock) {
            WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
            if (context == null) {
                return LocalServiceExecution.failed("XINSHOU_DRAG_WINDOW_UNAVAILABLE", null);
            }
            discardTerminalDrag();
            if (activeDrag != null
                    && (activeDrag.context != context || activeDrag.stopToken != stopToken)) {
                return LocalServiceExecution.failed("XINSHOU_DRAG_RETAINED_REQUEST_BUSY", null);
            }

            boolean firstSweep = activeDrag == null;
            if (firstSweep) {
                Supplier<InputActionSafetyReason> exactTaskSafety = () ->
                        taskStillCurrent.getAsBoolean()
                                ? InputActionSafetyReason.CLEAR
                                : InputActionSafetyReason.TASK_RUN_MISMATCH;
                InputActionQueue.RetainedSessionHandle handle =
                        inputActionQueue.openRetainedSession(
                                "xinshou:drag:segment-" + drag.segment(),
                                pauseToken,
                                stopToken,
                                exactTaskSafety,
                                exactTaskSafety,
                                this::releaseQuietly);
                if (handle.admission() != InputActionQueue.SessionAdmission.ADMITTED) {
                    return mapRetainedFailure(
                            handle.terminalSnapshot(),
                            "XINSHOU_DRAG_RETAINED_REQUEST_NOT_ADMITTED");
                }
                activeDrag = new ActiveDrag(context, stopToken, handle);
            }

            ActiveDrag retained = activeDrag;
            AtomicReference<LocalServiceExecution> callbackResult = new AtomicReference<>();
            InputActionExecutionResult stepResult;
            try {
                stepResult = inputActionQueue.submitRetainedSessionCallbackAndWait(
                        retained.handle,
                        () -> {
                            LocalServiceExecution execution =
                                    sweepInsideWorker(call, firstSweep);
                            callbackResult.set(execution);
                            return execution.status() == TurnStepResult.Status.COMPLETED;
                        });
            } catch (RuntimeException submissionFailure) {
                abortRetainedDrag();
                log.error("xinshou retained drag callback failed: segment={} message={}",
                        drag.segment(), submissionFailure.getMessage(), submissionFailure);
                return LocalServiceExecution.failed("XINSHOU_DRAG_SWEEP_FAILED", null);
            }

            LocalServiceExecution execution = callbackResult.get();
            if (stepResult.isCompleted() && execution != null) {
                return execution;
            }
            abortRetainedDrag();
            if (execution != null && execution.status() == TurnStepResult.Status.FAILED) {
                return execution;
            }
            return mapRetainedFailure(stepResult, "XINSHOU_DRAG_SWEEP_FAILED");
        }
    }

    /**
     * Terminate the exact retained worker request. LEFT_UP is performed by that worker's finally.
     *
     * @param stopToken exact task token that opened the retained request
     * @return typed terminal result; already released is idempotent success
     */
    public LocalServiceExecution release(TaskStopToken stopToken) {
        synchronized (retainedLock) {
            WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
            discardTerminalDrag();
            if (activeDrag == null) {
                return LocalServiceExecution.completed(
                        "XINSHOU_DRAG_ALREADY_RELEASED", null, null);
            }
            if (context != activeDrag.context || stopToken != activeDrag.stopToken) {
                return LocalServiceExecution.failed("XINSHOU_DRAG_RELEASE_OWNER_MISMATCH", null);
            }
            InputActionQueue.RetainedSessionHandle handle = activeDrag.handle;
            activeDrag = null;
            try {
                InputActionExecutionResult terminal =
                        inputActionQueue.terminateRetainedSessionAndWait(
                                handle, InputActionQueue.SessionTerminalCommand.RELEASE);
                return terminal.isCompleted()
                        ? LocalServiceExecution.completed("XINSHOU_DRAG_RELEASED", null, null)
                        : mapRetainedFailure(terminal, "XINSHOU_DRAG_RELEASE_FAILED");
            } catch (RuntimeException failure) {
                return LocalServiceExecution.failed("XINSHOU_DRAG_RELEASE_FAILED", null);
            }
        }
    }

    private LocalServiceExecution sweepInsideWorker(
            TurnLocalServiceCall call,
            boolean pressLeftButton) {
        TurnXinshouDragArguments drag = call.xinshouDrag();
        try {
            int[] start = coordinateHelper.getScaledRect(drag.startX(), drag.startY(), 1, 1);
            int[] left = coordinateHelper.getScaledRect(drag.leftX(), drag.startY(), 1, 1);
            int[] right = coordinateHelper.getScaledRect(drag.rightX(), drag.startY(), 1, 1);
            int[] end = coordinateHelper.getScaledRect(drag.leftX(), drag.endY(), 1, 1);
            if (pressLeftButton) {
                mouseController.holdSweepWithoutRelease(
                        start[0], start[1], left[0], right[0], end[1],
                        Math.max(1, drag.rowStepPx()));
            } else {
                mouseController.sweepWhileLeftHeld(
                        start[0], start[1], left[0], right[0], end[1],
                        Math.max(1, drag.rowStepPx()));
            }
            TurnFrame frame = captureProgressRoi(drag);
            if (frame == null) {
                log.warn("xinshou drag sweep captured no progress ROI: segment={}", drag.segment());
                return LocalServiceExecution.completed(
                        "XINSHOU_DRAG_SWEPT_NO_FRAME", null, null);
            }
            return LocalServiceExecution.completed("XINSHOU_DRAG_SWEPT", null, frame);
        } catch (RuntimeException failure) {
            log.error("xinshou drag sweep failed: segment={} message={}",
                    drag.segment(), failure.getMessage(), failure);
            return LocalServiceExecution.failed("XINSHOU_DRAG_SWEEP_FAILED", null);
        }
    }

    /**
     * 2026-08-23 用户契约（停止=彻底清空）：fresh-start 复位链调用——中止仍攥着的实体拖拽会话。
     * 审查修正：必须持 retainedLock（可见性/与 sweep 并发），且只中止属于本窗口的拖拽——
     * 别的窗口正在跑的新手拖拽不许被无关窗口的 fresh-start 误杀。
     */
    public void abortRetainedDragForFreshStart(String windowId) {
        synchronized (retainedLock) {
            ActiveDrag retained = activeDrag;
            if (retained == null) {
                return;
            }
            if (windowId != null && retained.context != null
                    && !windowId.equals(retained.context.getWindowId())) {
                return;
            }
            abortRetainedDrag();
        }
    }

    private void abortRetainedDrag() {
        ActiveDrag retained = activeDrag;
        activeDrag = null;
        if (retained == null) {
            return;
        }
        try {
            inputActionQueue.terminateRetainedSessionAndWait(
                    retained.handle, InputActionQueue.SessionTerminalCommand.ABORT);
        } catch (RuntimeException ignored) {
            log.warn("xinshou retained drag abort failed");
        }
    }

    private void discardTerminalDrag() {
        if (activeDrag != null && activeDrag.handle.terminalSnapshot() != null) {
            activeDrag = null;
        }
    }

    private static LocalServiceExecution mapRetainedFailure(
            InputActionExecutionResult terminal,
            String fallbackCode) {
        if (terminal != null
                && terminal.getSafetyReason() == InputActionSafetyReason.STOP_REQUESTED) {
            return LocalServiceExecution.stopped(null);
        }
        if (terminal != null
                && (terminal.getSafetyReason() == InputActionSafetyReason.WINDOW_BINDING_CHANGED
                || terminal.getSafetyReason() == InputActionSafetyReason.TASK_RUN_MISMATCH)) {
            return LocalServiceExecution.failed("XINSHOU_DRAG_RETAINED_REQUEST_STALE", null);
        }
        return LocalServiceExecution.failed(fallbackCode, null);
    }

    private void releaseQuietly() {
        try {
            mouseController.releaseLeftButton();
        } catch (RuntimeException ignored) {
            log.warn("xinshou drag release failed");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(bytes)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    private TurnFrame captureProgressRoi(TurnXinshouDragArguments drag) {
        int[] roi = coordinateHelper.getScaledRect(
                drag.progressRoiX(), drag.progressRoiY(),
                drag.progressRoiWidth(), drag.progressRoiHeight());
        BufferedImage image = tracker.captureToMemory(
                "xinshou:drag:progress:" + drag.segment(), roi[0], roi[1], roi[2], roi[3]);
        if (image == null) {
            return null;
        }
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream(4096);
            ImageIO.write(image, "png", png);
            byte[] bytes = png.toByteArray();
            TurnRegion region = new TurnRegion(roi[0], roi[1], image.getWidth(), image.getHeight());
            return new TurnFrame(
                    new TurnFrameMetadata(
                            TurnFramePurpose.MATCH_EVIDENCE,
                            "image/png",
                            sha256(bytes),
                            image.getWidth(),
                            image.getHeight(),
                            region,
                            null),
                    bytes);
        } catch (IOException encodeFailure) {
            log.warn("xinshou drag progress ROI encode failed: segment={}", drag.segment());
            return null;
        } finally {
            image.flush();
        }
    }

    private static final class ActiveDrag {
        private final WindowRuntimeContext context;
        private final TaskStopToken stopToken;
        private final InputActionQueue.RetainedSessionHandle handle;

        private ActiveDrag(WindowRuntimeContext context,
                           TaskStopToken stopToken,
                           InputActionQueue.RetainedSessionHandle handle) {
            this.context = context;
            this.stopToken = stopToken;
            this.handle = handle;
        }
    }
}
