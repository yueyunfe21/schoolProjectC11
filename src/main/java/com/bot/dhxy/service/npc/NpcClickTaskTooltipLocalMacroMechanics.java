package com.bot.dhxy.service.npc;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.util.List;
import java.util.Objects;

/**
 * Whole continuous local macro for the committed {@code 696a12b0} NPC task-tooltip template click,
 * extracted from {@code NpcClickService.java:1147-1260} ({@code clickNpcByTaskTooltipTemplate}), the
 * atomic move/click/verify order of {@code executeMoveClickAndVerify:176-216} and the record-point /
 * learned-ROI helpers {@code directNpcPointFromTooltipCenter} + {@code tooltipLearnedRoiFromTooltipCenter}
 * ({@code :1433-1468}).
 *
 * <p>The Cloud command has already decided the caller-order scan regions, the template path and the
 * verifier mode. This macro only does exact local binding: for every recommended region, in order, it
 * matches the tooltip template inside the region rectangle at {@code threshold=0.82 / minDistance=36}
 * via the existing {@link CoordinateHelper#findImagesInRegion} (screen-absolute points in
 * {@code ImageFinder.findAll} score/dedup order), and processes every de-duplicated hit point in that
 * order. For each point it runs, directly on the already-held exclusive input worker (never a nested
 * input queue), the baseline sequence {@code move -> sleep 150ms -> clickLeft (hold 150ms) ->
 * wait 1200ms -> existing dialog/battle verify}; the first verified hit stops, otherwise all visible
 * candidates are exhausted. The learning payload keeps the baseline record point ({@code tooltipCenterY+90})
 * and learned ROI ({@code tooltip[-150,-100,+150,+200]} clamped to the 1024x768 window), all in
 * screen-absolute coordinates with the template/frame owner and verify count preserved. It never selects
 * an NPC, strategy or fallback, and adds no retry, TTL, owner, session, wrapper or checkpoint.</p>
 */
@Slf4j
@Service
public final class NpcClickTaskTooltipLocalMacroMechanics {

    private static final long NPC_LEFT_CLICK_HOLD_MS = 150L;
    private static final long MOVE_SETTLE_MS = 150L;
    private static final long FIRST_VERIFY_WAIT_MS = 1200L;
    private static final double NPC_TASK_TOOLTIP_MATCH_RATE = 0.82;
    private static final double NPC_TASK_TOOLTIP_DEDUP_DISTANCE_PX = 36.0;
    private static final int RECORD_POINT_Y_OFFSET = 90;
    private static final int ROI_LEFT_OFFSET = -150;
    private static final int ROI_TOP_OFFSET = -100;
    private static final int ROI_RIGHT_OFFSET = 150;
    private static final int ROI_BOTTOM_OFFSET = 200;
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final String INPUT_ACTION_WORKER_THREAD = "dhxy-input-action-worker";

    private final InputProvider inputProvider;
    private final CoordinateHelper coordinateHelper;
    private final WindowTaskContextHolder windowTaskContextHolder;

    public NpcClickTaskTooltipLocalMacroMechanics(
            InputProvider inputProvider,
            CoordinateHelper coordinateHelper,
            WindowTaskContextHolder windowTaskContextHolder) {
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.windowTaskContextHolder = Objects.requireNonNull(windowTaskContextHolder, "windowTaskContextHolder");
    }

    /**
     * The caller's existing local dialog/battle verifier. Ownership stays with the caller; this macro
     * only invokes {@link #verify(String)} at the single baseline verify point per clicked candidate.
     */
    @FunctionalInterface
    public interface TaskTooltipClickVerifier {
        boolean verify(String reason);
    }

    /** Closed terminal for one task-tooltip template click + verify pass. */
    public enum Status {
        VERIFIED,
        CLICK_NOT_VERIFIED,
        NOT_FOUND,
        BINDING_UNAVAILABLE,
        TEMPLATE_UNAVAILABLE,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /** One caller-decided screen-absolute scan rectangle (right/bottom exclusive, baseline rect form). */
    public record TaskTooltipScanRegion(int x1, int y1, int x2, int y2) {
        int[] toRect() {
            return new int[] {x1, y1, x2, y2};
        }
    }

    /**
     * Closed intent: the Cloud-decided template path and the ordered recommended scan regions. The
     * regions are processed exactly in supplied order; this macro adds no region of its own.
     */
    public record TaskTooltipClickIntent(String templatePath, List<TaskTooltipScanRegion> regions, String description) {

        public TaskTooltipClickIntent {
            regions = regions == null ? List.of() : List.copyOf(regions);
        }
    }

    /**
     * Learning payload for a produced click: the baseline record point ({@code tooltipCenterY+90},
     * screen-absolute) and the learned yellow-name ROI (window-relative, clamped). Only present when a
     * real click was issued and verified/not-verified against the expected dialog.
     */
    public record TaskTooltipClickPayload(int recordPointX, int recordPointY, OcrWindowRegion learnedRoi) {

        public TaskTooltipClickPayload {
            Objects.requireNonNull(learnedRoi, "learnedRoi");
        }
    }

    /**
     * Immutable closed result whose compact constructor self-verifies the terminal/payload/clickProduced
     * combination: a verify verdict is only reachable after a real click with a learning payload; the
     * pre-scan/miss terminals carry neither a click nor a payload; interruption / mechanics failure carry
     * no payload but record whether a click was already produced.
     */
    public record TaskTooltipClickResult(Status status, boolean clickProduced, TaskTooltipClickPayload payload, String reason) {

        public TaskTooltipClickResult {
            Objects.requireNonNull(status, "status");
            switch (status) {
                case VERIFIED, CLICK_NOT_VERIFIED -> {
                    if (!clickProduced || payload == null) {
                        throw new IllegalArgumentException(status + " requires a produced click with a payload");
                    }
                }
                case NOT_FOUND, BINDING_UNAVAILABLE, TEMPLATE_UNAVAILABLE -> {
                    if (clickProduced || payload != null) {
                        throw new IllegalArgumentException(status + " requires no click and no payload");
                    }
                }
                case INTERRUPTED, MECHANICS_FAILED -> {
                    if (payload != null) {
                        throw new IllegalArgumentException(status + " must not carry a payload");
                    }
                }
            }
        }
    }

    /**
     * Run the baseline task-tooltip template click for the caller-decided regions on the input worker.
     * Per de-duplicated hit, in region then score order: move -> sleep 150 -> click (hold 150) ->
     * wait 1200 -> verify; the first verified hit stops, otherwise all visible candidates are exhausted.
     */
    public TaskTooltipClickResult clickTaskTooltipAndVerify(
            WindowNativeBinding binding, TaskTooltipClickIntent intent, TaskTooltipClickVerifier verifier) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(verifier, "verifier");

        // Gate 1: this macro drives raw input directly; it may only run on the exclusive input worker.
        // Off the worker we must not emit any physical input, so end on the existing MECHANICS_FAILED
        // terminal with no click and no payload (no new terminal is introduced).
        if (!isInputWorkerThread()) {
            return new TaskTooltipClickResult(Status.MECHANICS_FAILED, false, null, "non-input-worker-thread");
        }
        if (binding == null || !binding.hasNativeHandle()) {
            return failure(Status.BINDING_UNAVAILABLE, "binding-unavailable");
        }
        if (intent.templatePath() == null || intent.templatePath().isBlank()) {
            return failure(Status.TEMPLATE_UNAVAILABLE, "tooltip-template-unavailable");
        }
        if (intent.regions().isEmpty()) {
            return failure(Status.NOT_FOUND, "no-recommended-regions");
        }
        // Gate 2: the ambient capture context and the command binding must be the same true window.
        // findImagesInRegion captures via WindowTaskContextHolder.rawCurrent(); require that raw binding
        // to share this command binding's normalized native handle with valid, identical geometry.
        if (matchingContextBinding(binding) == null) {
            return failure(Status.BINDING_UNAVAILABLE, "context-binding-mismatch");
        }

        String templatePath = intent.templatePath();
        boolean clickProduced = false;
        TaskTooltipClickPayload lastMiss = null;
        try {
            for (int i = 0; i < intent.regions().size(); i++) {
                if (isInterrupted()) {
                    return interrupted(clickProduced, "interrupted-before-region-scan");
                }
                TaskTooltipScanRegion region = intent.regions().get(i);
                if (region == null) {
                    continue;
                }
                List<Point> matchedPoints = coordinateHelper.findImagesInRegion(
                        templatePath, region.toRect(), NPC_TASK_TOOLTIP_MATCH_RATE, NPC_TASK_TOOLTIP_DEDUP_DISTANCE_PX);
                // Gate 3: findImagesInRegion captures/refreshes via the ambient context, so it may have
                // drifted the HWND/geometry regardless of whether it produced any match. Re-read the
                // post-capture binding from the same raw context BEFORE the empty/nonempty branch and
                // require the same HWND, valid geometry and geometry identical to this command's binding.
                // Any drift ends on BINDING_UNAVAILABLE (never disguised as a business NOT_FOUND); an empty
                // list may only continue after this passes, and a nonempty list reuses this validated
                // post-capture binding base to compute the ROI.
                WindowNativeBinding postCaptureBinding = matchingContextBinding(binding);
                if (postCaptureBinding == null) {
                    return failure(Status.BINDING_UNAVAILABLE, "post-capture-binding-drift");
                }
                if (matchedPoints.isEmpty()) {
                    continue;
                }
                Point windowBaseAbs = new Point(postCaptureBinding.getX(), postCaptureBinding.getY());
                for (int matchIndex = 0; matchIndex < matchedPoints.size(); matchIndex++) {
                    Point matchedPoint = matchedPoints.get(matchIndex);
                    String description = "npcClick:taskTooltipTemplate#" + (matchIndex + 1);

                    if (isInterrupted()) {
                        return interrupted(clickProduced, "interrupted-before-click");
                    }
                    inputProvider.moveMouse(matchedPoint.x, matchedPoint.y);
                    if (!TaskSleep.sleep(MOVE_SETTLE_MS)) {
                        return interrupted(clickProduced, "interrupted-move-settle");
                    }
                    inputProvider.clickLeft(matchedPoint.x, matchedPoint.y, (int) NPC_LEFT_CLICK_HOLD_MS);
                    clickProduced = true;
                    if (!TaskSleep.sleep(FIRST_VERIFY_WAIT_MS)) {
                        return interrupted(clickProduced, "interrupted-verify-wait");
                    }
                    if (isInterrupted()) {
                        return interrupted(clickProduced, "interrupted-before-verify");
                    }
                    boolean verified = verifier.verify(description + ":firstVerify");
                    TaskTooltipClickPayload payload = payloadFor(matchedPoint, windowBaseAbs);
                    if (verified) {
                        return new TaskTooltipClickResult(Status.VERIFIED, true, payload,
                                "task-tooltip template verified; recordPoint=tooltipCenterY+90; roi=tooltip[-150,-100,+150,+200]");
                    }
                    lastMiss = payload;
                }
            }
            if (lastMiss != null) {
                return new TaskTooltipClickResult(Status.CLICK_NOT_VERIFIED, true, lastMiss,
                        "task-tooltip clicked but expected dialog not verified; recordPoint=tooltipCenterY+90");
            }
            return failure(Status.NOT_FOUND, "task-tooltip template not found");
        } catch (RuntimeException e) {
            log.warn("NPC task-tooltip macro failed: template={} reason={}", templatePath, e.getMessage(), e);
            return new TaskTooltipClickResult(Status.MECHANICS_FAILED, clickProduced, null, "mechanics-failed");
        }
    }

    private TaskTooltipClickPayload payloadFor(Point tooltipCenterAbs, Point windowBaseAbs) {
        // directNpcPointFromTooltipCenter: body click point is tooltip center Y + 90 (screen-absolute).
        int recordPointX = tooltipCenterAbs.x;
        int recordPointY = tooltipCenterAbs.y + RECORD_POINT_Y_OFFSET;
        // tooltipLearnedRoiFromTooltipCenter: window-relative yellow-name ROI, clamped to the game window.
        OcrWindowRegion learnedRoi = new OcrWindowRegion(
                tooltipCenterAbs.x - windowBaseAbs.x + ROI_LEFT_OFFSET,
                tooltipCenterAbs.y - windowBaseAbs.y + ROI_TOP_OFFSET,
                tooltipCenterAbs.x - windowBaseAbs.x + ROI_RIGHT_OFFSET,
                tooltipCenterAbs.y - windowBaseAbs.y + ROI_BOTTOM_OFFSET)
                .clamp(WINDOW_WIDTH, WINDOW_HEIGHT);
        return new TaskTooltipClickPayload(recordPointX, recordPointY, learnedRoi);
    }

    private static boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_ACTION_WORKER_THREAD);
    }

    /**
     * Resolve the current thread's raw window context binding and accept it only when it is the same
     * true window as {@code commandBinding}: present, with a native handle, sharing the same normalized
     * native handle, and with valid geometry identical to the command binding. Returns the validated
     * context binding, or {@code null} on any mismatch/absence/geometry drift.
     */
    private WindowNativeBinding matchingContextBinding(WindowNativeBinding commandBinding) {
        WindowRuntimeContext context = windowTaskContextHolder.rawCurrent().orElse(null);
        if (context == null) {
            return null;
        }
        WindowNativeBinding contextBinding = context.getNativeBinding();
        if (contextBinding == null || !contextBinding.hasNativeHandle()) {
            return null;
        }
        if (!contextBinding.getNativeHandle().equals(commandBinding.getNativeHandle())) {
            return null;
        }
        if (!commandBinding.hasGeometry() || !contextBinding.hasGeometry()) {
            return null;
        }
        if (!commandBinding.hasSameGeometry(contextBinding)) {
            return null;
        }
        return contextBinding;
    }

    private static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private static TaskTooltipClickResult failure(Status status, String reason) {
        return new TaskTooltipClickResult(status, false, null, reason);
    }

    private static TaskTooltipClickResult interrupted(boolean clickProduced, String reason) {
        return new TaskTooltipClickResult(Status.INTERRUPTED, clickProduced, null, reason);
    }
}
