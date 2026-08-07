package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnCaptureSpec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowRect;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionExecutionResult;
import com.bot.dhxy.input.action.InputActionSafetyReason;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.runner.stop.TaskStopRequestedException;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executes background capture against the refreshed exact HWND, with optional globally serialized pointer clearance.
 */
@Component
public final class TurnCaptureStepExecutor {

    private final BoundWindowCaptureService captureService;
    private final TurnPngCodec pngCodec;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder contextHolder;
    private final BoundWindowKeyboardService keyboardService;
    private final InputProvider inputProvider;
    private final Supplier<Point> pointerLocationSupplier;
    private final LongPredicate settleWait;

    @Autowired
    public TurnCaptureStepExecutor(BoundWindowCaptureService captureService,
                                   TurnPngCodec pngCodec,
                                   InputSequences inputSequences,
                                   WindowTaskContextHolder contextHolder,
                                   BoundWindowKeyboardService keyboardService,
                                   InputProvider inputProvider) {
        this(captureService, pngCodec, inputSequences, contextHolder, keyboardService, inputProvider,
                TurnCaptureStepExecutor::currentScreenPointer, TaskSleep::sleep);
    }

    /** Retains the existing pure-capture constructor for isolated tests that do not request pointer clearance. */
    public TurnCaptureStepExecutor(BoundWindowCaptureService captureService, TurnPngCodec pngCodec) {
        this(captureService, pngCodec, null, null, null, null,
                TurnCaptureStepExecutor::currentScreenPointer, TaskSleep::sleep);
    }

    TurnCaptureStepExecutor(BoundWindowCaptureService captureService,
                            TurnPngCodec pngCodec,
                            InputSequences inputSequences,
                            WindowTaskContextHolder contextHolder,
                            Supplier<Point> pointerLocationSupplier) {
        this(captureService, pngCodec, inputSequences, contextHolder, null, null,
                pointerLocationSupplier, TaskSleep::sleep);
    }

    TurnCaptureStepExecutor(BoundWindowCaptureService captureService,
                            TurnPngCodec pngCodec,
                            InputSequences inputSequences,
                            WindowTaskContextHolder contextHolder,
                            BoundWindowKeyboardService keyboardService,
                            InputProvider inputProvider,
                            Supplier<Point> pointerLocationSupplier,
                            LongPredicate settleWait) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
        this.pngCodec = Objects.requireNonNull(pngCodec, "pngCodec");
        this.inputSequences = inputSequences;
        this.contextHolder = contextHolder;
        this.keyboardService = keyboardService;
        this.inputProvider = inputProvider;
        this.pointerLocationSupplier = Objects.requireNonNull(pointerLocationSupplier, "pointerLocationSupplier");
        this.settleWait = Objects.requireNonNull(settleWait, "settleWait");
    }

    /**
     * Capture one protocol CAPTURE step from the exact action window.
     *
     * @param window per-action execution snapshot whose binding was refreshed exactly once.
     * @param captureSpec capture request; a null region means the full bound window and forbids pointer clearance.
     * @param sourceStepIndex zero-based action step index.
     * @return typed mechanical result; successful legacy capture uses {@link Code#OK}, while a probe uses a
     * changed/unchanged code and returns only its after frame.
     */
    public Execution execute(TurnExecutionWindow window,
                             TurnCaptureSpec captureSpec,
                             int sourceStepIndex) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(captureSpec, "captureSpec");
        TurnCaptureSpec.ClearPointerIfOverRegion clear = captureSpec.clearPointerIfOverRegion();
        TurnCaptureSpec.PixelChangeProbe probe = captureSpec.pixelChangeProbe();
        if (clear != null && probe != null) {
            throw new IllegalArgumentException(
                    "clearPointerIfOverRegion and pixelChangeProbe are mutually exclusive");
        }
        if (probe != null) {
            return executePixelChangeProbe(window, captureSpec, probe, sourceStepIndex);
        }
        if (clear != null) {
            TurnRegion region = captureSpec.region();
            if (region == null) {
                throw new IllegalArgumentException(
                        "clearPointerIfOverRegion cannot be used with full-window capture");
            }
            if (clear.paddingPx() < 0 || clear.paddingPx() > 128) {
                throw new IllegalArgumentException("pointer-clear paddingPx must be in [0, 128]");
            }
            if (clear.settleMs() < 0 || clear.settleMs() > 5_000) {
                throw new IllegalArgumentException("pointer-clear settleMs must be in [0, 5000]");
            }

            TurnWindowRect windowRect = Objects.requireNonNull(
                    window.metadata().windowRect(), "window.metadata.windowRect");
            requireInsideWindow(region, windowRect);
            requireTargetInsideWindow(clear.targetX(), clear.targetY(), windowRect);
            if (isInsidePaddedRegion(clear.targetX(), clear.targetY(), region, clear.paddingPx())) {
                throw new IllegalArgumentException("pointer-clear target must be outside the padded capture region");
            }
            if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("pointer-clear input stopped before pointer read");
            }

            Point pointer = pointerLocationSupplier.get();
            if (pointer != null && isInsidePaddedRegion(pointer.x, pointer.y, region, clear.paddingPx())) {
                if (inputSequences == null || contextHolder == null) {
                    throw new IllegalStateException("pointer-clear input queue is unavailable");
                }
                List<InputAction> actions = List.of(
                        InputAction.moveMouse(clear.targetX(), clear.targetY()),
                        InputAction.sleep(clear.settleMs()));
                boolean moved = contextHolder.callWith(
                        window.context(),
                        () -> inputSequences.submitAndWait(
                                "turn:capture:pointer-clear:step-" + sourceStepIndex,
                                actions));
                if (!moved) {
                    throw new IllegalStateException(Thread.currentThread().isInterrupted()
                            ? "pointer-clear input queue was interrupted"
                            : "pointer-clear input queue did not complete");
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("pointer-clear input queue completed with interruption");
                }
            }
        }
        return Execution.completed(
                Code.OK,
                capture(window, captureSpec.region(), TurnFramePurpose.CAPTURE, sourceStepIndex));
    }

    private Execution executePixelChangeProbe(TurnExecutionWindow window,
                                              TurnCaptureSpec captureSpec,
                                              TurnCaptureSpec.PixelChangeProbe probe,
                                              int sourceStepIndex) {
        TurnRegion region = captureSpec.region();
        if (region == null || captureSpec.resultMode() != TurnCaptureSpec.ResultMode.UPLOAD_IMAGE) {
            throw new IllegalArgumentException("pixelChangeProbe requires a region and UPLOAD_IMAGE result mode");
        }
        TurnWindowRect windowRect = Objects.requireNonNull(
                window.metadata().windowRect(), "window.metadata.windowRect");
        requireInsideWindow(region, windowRect);
        requireTargetInsideWindow(probe.targetX(), probe.targetY(), windowRect);
        long regionRight = (long) region.x() + region.width();
        long regionBottom = (long) region.y() + region.height();
        if (probe.targetX() < region.x()
                || probe.targetX() >= regionRight
                || probe.targetY() < region.y()
                || probe.targetY() >= regionBottom) {
            throw new IllegalArgumentException("pixelChangeProbe target is outside the capture region");
        }
        if (probe.ctrlDownSettleMs() < 0 || probe.ctrlDownSettleMs() > 5_000
                || probe.afterMoveSettleMs() < 0 || probe.afterMoveSettleMs() > 5_000
                || probe.ctrlUpSettleMs() < 0 || probe.ctrlUpSettleMs() > 5_000) {
            throw new IllegalArgumentException("pixelChangeProbe timings must be in [0,5000]");
        }
        if (!Double.isFinite(probe.differenceRatioThreshold())
                || probe.differenceRatioThreshold() < 0.0
                || probe.differenceRatioThreshold() > 1.0) {
            throw new IllegalArgumentException("pixelChangeProbe differenceRatioThreshold must be finite and in [0,1]");
        }
        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
            return Execution.stopped("stop requested before pixel-change probe");
        }
        if (inputSequences == null || keyboardService == null || inputProvider == null) {
            return Execution.failed(Code.PIXEL_PROBE_FAILED, "pixel-change probe mechanics are unavailable");
        }

        ProbeState state = new ProbeState();
        /*
         * The frozen boundary owns the (binding, epoch) generation freeze: re-reading the mutable context
         * epoch here, after the action resolver already froze the binding, is exactly what allowed a stale
         * binding to be spliced onto a newer generation. Pass only the frozen window and let the queue
         * witness the generation under the context monitor.
         */
        InputActionExecutionResult queueResult = null;
        try {
            queueResult = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                    "turn:capture:pixel-change:step-" + sourceStepIndex,
                    window.context(),
                    window.binding(),
                    () -> {
                                BufferedImage before = null;
                                BufferedImage after = null;
                                boolean ctrlDownInvoked = false;
                                try {
                                    if (!probeCheckpoint(state, "before capture")) {
                                        return true;
                                    }
                                    before = captureImage(window, region);
                                    if (!probeCheckpoint(state, "after before capture")) {
                                        return true;
                                    }
                                    if (!probeCheckpoint(state, "before Ctrl DOWN")) {
                                        return true;
                                    }

                                    ctrlDownInvoked = true;
                                    if (inputProvider.requiresForegroundKeyboard()) {
                                        inputProvider.holdCtrl();
                                    } else {
                                        BoundWindowKeyboardService.KeyTransitionAttempt down =
                                                keyboardService.transitionModifier(
                                                        window.binding(),
                                                        window.metadata().windowId(),
                                                        BoundWindowKeyboardService.ModifierKey.CONTROL,
                                                        BoundWindowKeyboardService.KeyTransition.DOWN);
                                        if (!down.attempted() || !down.success()) {
                                            state.failed = true;
                                            state.detail = "Ctrl DOWN failed: " + down.reason();
                                            return true;
                                        }
                                    }
                                    if (!probeCheckpoint(state, "after Ctrl DOWN")) {
                                        return true;
                                    }

                                    boolean downSettled = settleWait.test(probe.ctrlDownSettleMs());
                                    if (!downSettled) {
                                        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
                                            state.stopped = true;
                                            state.detail = "Ctrl DOWN settle interrupted";
                                        } else {
                                            state.failed = true;
                                            state.detail = "Ctrl DOWN settle did not complete";
                                        }
                                        return true;
                                    }
                                    if (!probeCheckpoint(state, "after Ctrl DOWN settle")) {
                                        return true;
                                    }
                                    if (!probeCheckpoint(state, "before probe mouse move")) {
                                        return true;
                                    }

                                    inputProvider.moveMouse(probe.targetX(), probe.targetY());
                                    if (!probeCheckpoint(state, "after probe mouse move")) {
                                        return true;
                                    }
                                    boolean moveSettled = settleWait.test(probe.afterMoveSettleMs());
                                    if (!moveSettled) {
                                        if (window.metadata().stopRequested() || Thread.currentThread().isInterrupted()) {
                                            state.stopped = true;
                                            state.detail = "probe mouse settle interrupted";
                                        } else {
                                            state.failed = true;
                                            state.detail = "probe mouse settle did not complete";
                                        }
                                        return true;
                                    }
                                    if (!probeCheckpoint(state, "after probe mouse settle")) {
                                        return true;
                                    }
                                    if (!probeCheckpoint(state, "before after-capture")) {
                                        return true;
                                    }

                                    after = captureImage(window, region);
                                    if (!probeCheckpoint(state, "after after-capture")) {
                                        return true;
                                    }
                                    state.completedCode = ImageFinder.isMatch(
                                            before, after, probe.differenceRatioThreshold())
                                            ? Code.PIXELS_UNCHANGED
                                            : Code.PIXELS_CHANGED;
                                    state.afterImage = after;
                                    after = null;
                                    return true;
                                } catch (RuntimeException mechanicsFailure) {
                                    state.failed = true;
                                    state.detail = "pixel-change probe mechanics failed: " + mechanicsFailure;
                                    return true;
                                } finally {
                                    if (ctrlDownInvoked) {
                                        boolean released = false;
                                        String releaseDetail = null;
                                        try {
                                            if (inputProvider.requiresForegroundKeyboard()) {
                                                inputProvider.releaseCtrl();
                                                released = true;
                                            } else {
                                                BoundWindowKeyboardService.KeyTransitionAttempt up =
                                                        keyboardService.transitionModifier(
                                                                window.binding(),
                                                                window.metadata().windowId(),
                                                                BoundWindowKeyboardService.ModifierKey.CONTROL,
                                                                BoundWindowKeyboardService.KeyTransition.UP);
                                                released = up.attempted() && up.success();
                                                if (!released) {
                                                    releaseDetail = up.reason();
                                                }
                                            }
                                        } catch (Throwable releaseFailure) {
                                            state.releaseFailed = true;
                                            releaseDetail = releaseFailure.toString();
                                            state.detail = "Ctrl UP failed: " + releaseDetail;
                                        }

                                        boolean releaseSettled = false;
                                        try {
                                            releaseSettled = settleWait.test(probe.ctrlUpSettleMs());
                                        } catch (RuntimeException settleFailure) {
                                            if (releaseDetail == null) {
                                                state.failed = true;
                                                state.detail = "Ctrl UP settle failed: " + settleFailure;
                                            }
                                        }
                                        if (!released) {
                                            state.releaseFailed = true;
                                            state.detail = "Ctrl UP failed: " + releaseDetail;
                                        } else if (!releaseSettled) {
                                            if (window.metadata().stopRequested()
                                                    || Thread.currentThread().isInterrupted()) {
                                                state.stopped = true;
                                                state.detail = "Ctrl UP settle interrupted";
                                            } else {
                                                state.failed = true;
                                                if (state.detail == null) {
                                                    state.detail = "Ctrl UP settle did not complete";
                                                }
                                            }
                                        } else if (window.metadata().stopRequested()
                                                || Thread.currentThread().isInterrupted()) {
                                            state.stopped = true;
                                            state.detail = "stop requested after Ctrl UP";
                                        }
                                    }
                                    if (before != null) {
                                        before.flush();
                                    }
                                    if (after != null) {
                                        after.flush();
                                    }
                                }
                            });
        } catch (RuntimeException queueFailure) {
            queueResult = null;
            if (state.detail == null) {
                state.detail = "pixel-change probe queue failed: " + queueFailure;
            }
        }

        try {
            if (state.releaseFailed) {
                return Execution.failed(Code.CTRL_RELEASE_FAILED, state.detail);
            }
            /*
             * A stop that closed the token before worker admission never reaches the callback, so
             * state.stopped and the resolve-time metadata snapshot can both still be false and this thread
             * is not interrupted. The worker already classified it as STOP_REQUESTED; honour that typed
             * reason instead of degrading a real stop into a mechanics failure.
             */
            if (state.stopped
                    || window.metadata().stopRequested()
                    || Thread.currentThread().isInterrupted()
                    || (queueResult != null
                            && queueResult.getSafetyReason() == InputActionSafetyReason.STOP_REQUESTED)) {
                return Execution.stopped(state.detail == null ? "pixel-change probe stopped" : state.detail);
            }
            if (queueResult == null || !queueResult.isCompleted()) {
                return Execution.failed(
                        Code.PIXEL_PROBE_FAILED,
                        state.detail == null ? "pixel-change probe queue did not complete" : state.detail);
            }
            if (state.failed) {
                return Execution.failed(
                        Code.PIXEL_PROBE_FAILED,
                        state.detail == null ? "pixel-change probe mechanics failed" : state.detail);
            }
            if (state.completedCode == null || state.afterImage == null) {
                return Execution.failed(
                        Code.PIXEL_PROBE_FAILED,
                        state.detail == null ? "pixel-change probe did not complete" : state.detail);
            }
            TurnFrame afterFrame = pngCodec.encode(
                    state.afterImage, TurnFramePurpose.CAPTURE, region, sourceStepIndex);
            return Execution.completed(state.completedCode, afterFrame);
        } catch (RuntimeException encodeFailure) {
            return Execution.failed(
                    Code.PIXEL_PROBE_FAILED,
                    "pixel-change probe after-frame encoding failed: " + encodeFailure);
        } finally {
            if (state.afterImage != null) {
                state.afterImage.flush();
            }
        }
    }

    private static boolean probeCheckpoint(ProbeState state, String stage) {
        try {
            if (InputActionScope.checkpoint()) {
                return true;
            }
        } catch (TaskStopRequestedException stopRequested) {
            state.stopped = true;
            state.detail = "pixel-change probe stopped at " + stage + ": " + stopRequested.getMessage();
            return false;
        }
        state.stopped = true;
        state.detail = "pixel-change probe cancelled at " + stage;
        return false;
    }

    /**
     * Capture one full-window or screen-absolute ROI frame for a protocol-owned purpose.
     *
     * @param window per-action execution snapshot whose binding must remain the capture source.
     * @param requestedRegion screen-absolute ROI, or null for the complete bound window.
     * @param purpose closed protocol frame purpose.
     * @param sourceStepIndex zero-based source step index, or null for non-step failure evidence.
     * @return unscaled raw PNG frame with metadata derived from the captured pixels.
     */
    public TurnFrame capture(TurnExecutionWindow window,
                             TurnRegion requestedRegion,
                             TurnFramePurpose purpose,
                             Integer sourceStepIndex) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(purpose, "purpose");
        TurnWindowRect windowRect = Objects.requireNonNull(
                window.metadata().windowRect(), "window.metadata.windowRect");

        TurnRegion actualRegion = requestedRegion == null
                ? new TurnRegion(windowRect.left(), windowRect.top(), windowRect.width(), windowRect.height())
                : requestedRegion;
        requireInsideWindow(actualRegion, windowRect);

        BufferedImage image = captureImage(window, requestedRegion, actualRegion);
        try {
            return pngCodec.encode(image, purpose, actualRegion, sourceStepIndex);
        } finally {
            image.flush();
        }
    }

    /** Mechanical execution state consumed directly by the turn outcome projection. */
    public enum Status {
        COMPLETED,
        FAILED,
        STOPPED
    }

    /** Closed capture result code; only changed/unchanged codes represent a completed probe. */
    public enum Code {
        OK,
        PIXELS_CHANGED,
        PIXELS_UNCHANGED,
        PIXEL_PROBE_FAILED,
        CTRL_RELEASE_FAILED,
        STOPPED
    }

    /**
     * @param status terminal mechanical status.
     * @param code closed capture result code.
     * @param frame sole raw frame; present only for completed capture mechanics.
     * @param detail mechanical diagnostic without OCR, NPC, or business interpretation.
     */
    public record Execution(Status status, Code code, TurnFrame frame, String detail) {

        public Execution {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(code, "code");
            if (status == Status.COMPLETED && frame == null) {
                throw new IllegalArgumentException("completed capture execution requires one frame");
            }
            if (status != Status.COMPLETED && frame != null) {
                throw new IllegalArgumentException("failed/stopped capture execution must not carry a frame");
            }
        }

        private static Execution completed(Code code, TurnFrame frame) {
            return new Execution(Status.COMPLETED, code, Objects.requireNonNull(frame, "frame"), null);
        }

        private static Execution failed(Code code, String detail) {
            return new Execution(Status.FAILED, code, null, detail);
        }

        private static Execution stopped(String detail) {
            return new Execution(Status.STOPPED, Code.STOPPED, null, detail);
        }
    }

    private BufferedImage captureImage(TurnExecutionWindow window, TurnRegion region) {
        return captureImage(window, region, region);
    }

    private BufferedImage captureImage(TurnExecutionWindow window,
                                       TurnRegion requestedRegion,
                                       TurnRegion actualRegion) {
        WindowNativeBinding binding = Objects.requireNonNull(window.binding(), "window.binding");
        TurnWindowRect windowRect = Objects.requireNonNull(
                window.metadata().windowRect(), "window.metadata.windowRect");
        Optional<BoundWindowCaptureService.CaptureResult> captured = requestedRegion == null
                ? captureService.captureWindow(binding)
                : captureService.captureRegion(
                        binding,
                        windowRect.left(),
                        windowRect.top(),
                        actualRegion.x(),
                        actualRegion.y(),
                        Math.addExact(actualRegion.x(), actualRegion.width()),
                        Math.addExact(actualRegion.y(), actualRegion.height()));
        BoundWindowCaptureService.CaptureResult result = captured.orElseThrow(() ->
                new IllegalStateException("Background HWND capture failed for window "
                        + window.metadata().windowId()));
        BufferedImage image = Objects.requireNonNull(result.image(), "captured image");
        if (image.getWidth() != actualRegion.width() || image.getHeight() != actualRegion.height()) {
            image.flush();
            throw new IllegalStateException("Background HWND capture dimensions do not match requested region");
        }
        return image;
    }

    private void requireInsideWindow(TurnRegion region, TurnWindowRect window) {
        if (region.width() <= 0 || region.height() <= 0) {
            throw new IllegalArgumentException("capture region dimensions must be positive");
        }
        long regionRight = (long) region.x() + region.width();
        long regionBottom = (long) region.y() + region.height();
        long windowRight = (long) window.left() + window.width();
        long windowBottom = (long) window.top() + window.height();
        if (region.x() < window.left()
                || region.y() < window.top()
                || regionRight > windowRight
                || regionBottom > windowBottom) {
            throw new IllegalArgumentException("capture region is outside the refreshed window rectangle");
        }
    }

    private void requireTargetInsideWindow(int targetX, int targetY, TurnWindowRect window) {
        long right = (long) window.left() + window.width();
        long bottom = (long) window.top() + window.height();
        if (targetX < window.left() || targetX >= right || targetY < window.top() || targetY >= bottom) {
            throw new IllegalArgumentException("pointer-clear target is outside the refreshed window rectangle");
        }
    }

    private boolean isInsidePaddedRegion(int x, int y, TurnRegion region, int paddingPx) {
        long left = (long) region.x() - paddingPx;
        long top = (long) region.y() - paddingPx;
        long right = (long) region.x() + region.width() + paddingPx;
        long bottom = (long) region.y() + region.height() + paddingPx;
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private static Point currentScreenPointer() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        return pointerInfo == null ? null : pointerInfo.getLocation();
    }

    private static final class ProbeState {
        private Code completedCode;
        private BufferedImage afterImage;
        private boolean failed;
        private boolean releaseFailed;
        private boolean stopped;
        private String detail;
    }
}
