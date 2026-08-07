package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Objects;

/**
 * Owns the 钟馗 task-accept/cancel dialog mechanics for 抓鬼.
 *
 * <p>All templates are matched against the bound Client window. The Cloud receives only the
 * mechanical result and never runs an OCR/template fallback for these options.</p>
 */
@Component
public final class CatchGhostDialogLocalOperation {

    private static final String ACCEPT_NORMAL_TEMPLATE =
            "images/template/dialog/zhuagui/accept.png";
    private static final String CANCEL_TEMPLATE =
            "images/template/dialog/zhuagui/cancel.png";
    private static final int DIALOG_ROI_X = 250;
    private static final int DIALOG_ROI_Y = 312;
    private static final int DIALOG_ROI_W = 529;
    private static final int DIALOG_ROI_H = 208;
    private static final double MATCH_RATE = 0.82D;

    private final MechanicalPort mechanics;

    @Autowired
    public CatchGhostDialogLocalOperation(InputSequences inputSequences, CoordinateHelper coordinateHelper) {
        this(new ProductionMechanicalPort(inputSequences, coordinateHelper));
    }

    CatchGhostDialogLocalOperation(MechanicalPort mechanics) {
        this.mechanics = Objects.requireNonNull(mechanics, "mechanics");
    }

    /**
     * Resolves the 钟馗 accept dialog. A successful click is not acceptance by itself: Cloud must
     * subsequently observe a fresh 抓鬼 title before it advances the task.
     *
     * @return ACCEPTED after clicking the only accept option; NOT_MATCHED otherwise.
     */
    public AcceptResult executeAccept() {
        int[] roi = dialogRoi();
        Point normal = mechanics.findImageInRegion(ACCEPT_NORMAL_TEMPLATE, roi, MATCH_RATE);
        if (normal != null) {
            return clickTemplateCentre("catch-ghost:accept", normal)
                    ? AcceptResult.ACCEPTED
                    : AcceptResult.NOT_MATCHED;
        }
        return AcceptResult.NOT_MATCHED;
    }

    /**
     * Clears the dialog only after an earlier accept click failed to produce a fresh task title.
     * This is intentionally separate from {@link #executeAccept()} so first-time acceptance never
     * cancels a dialog preemptively.
     */
    public AcceptResult executeCancel() {
        Point cancel = mechanics.findImageInRegion(CANCEL_TEMPLATE, dialogRoi(), MATCH_RATE);
        if (cancel != null) {
            return clickTemplateCentre("catch-ghost:cancel", cancel)
                    ? AcceptResult.CANCELLED
                    : AcceptResult.NOT_MATCHED;
        }
        return AcceptResult.NOT_MATCHED;
    }

    private int[] dialogRoi() {
        return mechanics.scaledRect(DIALOG_ROI_X, DIALOG_ROI_Y, DIALOG_ROI_W, DIALOG_ROI_H);
    }

    /**
     * Clicks a matched template.
     *
     * <p>{@code findImageInRegion} already returns the match CENTRE (ImageFinder adds half the
     * template to OpenCV's top-left), so the point is clicked as-is. Adding half a template here
     * once pushed the 抓鬼 accept click from the option's centre to its bottom-right corner, into
     * the line gap that belongs to the next option's hit box — measured on the 2026-07-31 frame:
     * the option rows are y=427..440 and y=444..457, and the click landed on y=443, so "我来帮你"
     * actually selected "我要取消全队任务".</p>
     */
    private boolean clickTemplateCentre(String source, Point centre) {
        return mechanics.moveAndClickLeft(source, centre.x, centre.y, 150, 650);
    }

    public enum AcceptResult {
        ACCEPTED,
        CANCELLED,
        NOT_MATCHED
    }

    interface MechanicalPort {
        int[] scaledRect(int relativeX, int relativeY, int width, int height);

        Point findImageInRegion(String template, int[] roi, double matchRate);

        boolean moveAndClickLeft(String source, int screenX, int screenY, int settleMs, int delayMs);
    }

    private static final class ProductionMechanicalPort implements MechanicalPort {
        private final InputSequences inputSequences;
        private final CoordinateHelper coordinateHelper;

        private ProductionMechanicalPort(InputSequences inputSequences, CoordinateHelper coordinateHelper) {
            this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
            this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        }

        @Override
        public int[] scaledRect(int relativeX, int relativeY, int width, int height) {
            return coordinateHelper.getScaledRect(relativeX, relativeY, width, height);
        }

        @Override
        public Point findImageInRegion(String template, int[] roi, double matchRate) {
            return coordinateHelper.findImageInRegion(template, roi, matchRate);
        }

        @Override
        public boolean moveAndClickLeft(String source, int screenX, int screenY, int settleMs, int delayMs) {
            return inputSequences.moveAndClickLeft(source, screenX, screenY, settleMs, delayMs);
        }
    }
}
