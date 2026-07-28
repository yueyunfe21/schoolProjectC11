package com.bot.dhxy.cloud.turn.local.dialog;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Objects;
import java.util.Random;

/**
 * W-696-DIALOG-STORY-ADVANCE-LOCAL-MECHANICS-1: closed local mechanical boundary for advancing a
 * story dialog by one click.
 *
 * <p>This class copies only the exact local mechanical action of the committed {@code 696a12b0}
 * {@code DialogService.fastClickStoryDialogDirect} (lines 1780-1789), together with the input-worker
 * gate of {@code handleStoryDialog} (line 1772). It must run on the input-worker thread and reads the
 * dialog rect from the exact caller-supplied {@link WindowNativeBinding} geometry rather than the
 * global tracker window state. It performs a pre-wait, one left click at the large-dialog bottom-centre,
 * and a post-wait, returning a closed typed result. It carries no business decision, retry, TTL, owner,
 * session, or wrapper: the timing expression, dialog-rect offsets, bottom offset, randomisation radii,
 * and click delay are preserved byte-for-byte from the baseline.</p>
 */
@Component
public final class DialogStoryAdvanceLocalMacroMechanics {

    // Baseline large-dialog ROI (696a12b0 DialogService DIALOG_LARGE_*), applied to binding origin.
    private static final int DIALOG_LARGE_X = 250;
    private static final int DIALOG_LARGE_Y = 312;
    private static final int DIALOG_LARGE_W = 529;
    private static final int DIALOG_LARGE_H = 208;

    // Baseline story-advance click geometry.
    private static final int STORY_CLICK_BOTTOM_OFFSET = 40;
    private static final int STORY_CLICK_RANDOM_RADIUS_X = 30;
    private static final int STORY_CLICK_RANDOM_RADIUS_Y = 10;
    private static final int STORY_CLICK_DELAY_MS = 150;

    private static final String INPUT_WORKER_THREAD_MARKER = "dhxy-input-action-worker";

    private final CoordinateHelper coordinateHelper;
    private final InputProvider inputProvider;
    private final Random random = new Random();

    public DialogStoryAdvanceLocalMacroMechanics(
            CoordinateHelper coordinateHelper, InputProvider inputProvider) {
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
    }

    /**
     * Advances the current story dialog by exactly one click, byte-equivalent to the baseline
     * {@code fastClickStoryDialogDirect}. Must be invoked on the input-worker thread with the exact
     * window binding whose geometry anchors the dialog rect.
     *
     * @param binding exact native-window binding whose screen-absolute origin anchors the dialog rect
     * @return a closed typed result: {@code BINDING_UNAVAILABLE} when the binding is missing or has no
     *         native handle or geometry (checked before any wait or input, so no click is ever sent),
     *         {@code NOT_ON_INPUT_WORKER} when the input-worker gate fails,
     *         {@code INTERRUPTED_BEFORE_CLICK} when the pre-click wait is interrupted with no click sent,
     *         {@code CLICKED_INTERRUPTED} when the click was sent but the post-click wait is interrupted,
     *         otherwise {@code ADVANCED}.
     */
    public Result advanceStoryDialog(WindowNativeBinding binding) {
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()
                || binding.getWidth() < DIALOG_LARGE_X + DIALOG_LARGE_W
                || binding.getHeight() < DIALOG_LARGE_Y + DIALOG_LARGE_H) {
            return new Result(Status.BINDING_UNAVAILABLE);
        }
        if (!isInputWorkerThread()) {
            return new Result(Status.NOT_ON_INPUT_WORKER);
        }
        if (!TaskSleep.sleep(600 + random.nextInt(100))) {
            return new Result(Status.INTERRUPTED_BEFORE_CLICK);
        }
        int xStart = binding.getX() + DIALOG_LARGE_X;
        int yStart = binding.getY() + DIALOG_LARGE_Y;
        int xEnd = xStart + DIALOG_LARGE_W;
        int yEnd = yStart + DIALOG_LARGE_H;
        double scale = coordinateHelper.getScaleRatio();
        int cx = xStart + (xEnd - xStart) / 2;
        int cy = yEnd - (int) Math.round(STORY_CLICK_BOTTOM_OFFSET / scale);
        Point safeClick = coordinateHelper.getRandomizedPoint(
                new Point(cx, cy), STORY_CLICK_RANDOM_RADIUS_X, STORY_CLICK_RANDOM_RADIUS_Y);
        inputProvider.clickLeft(safeClick.x, safeClick.y, STORY_CLICK_DELAY_MS);
        return TaskSleep.sleep(600 + random.nextInt(100))
                ? new Result(Status.ADVANCED)
                : new Result(Status.CLICKED_INTERRUPTED);
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_WORKER_THREAD_MARKER);
    }

    public enum Status {
        ADVANCED,
        INTERRUPTED_BEFORE_CLICK,
        CLICKED_INTERRUPTED,
        NOT_ON_INPUT_WORKER,
        BINDING_UNAVAILABLE
    }

    public record Result(Status status) {
        public Result {
            Objects.requireNonNull(status, "status");
        }
    }
}
