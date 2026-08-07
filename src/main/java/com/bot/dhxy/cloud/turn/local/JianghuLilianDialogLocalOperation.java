package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Objects;

/**
 * Owns the 阿三 task-accept dialog decision for 江湖历练.
 *
 * <p>All templates are matched against the bound Client window. The Cloud receives only the
 * mechanical result and never runs an OCR/template fallback for these options.</p>
 */
@Component
public final class JianghuLilianDialogLocalOperation {

    private static final String ACCEPT_NORMAL_TEMPLATE =
            "images/template/dialog/a3/accept_normal.png";
    private static final String ACCEPT_RESET_TEMPLATE =
            "images/template/dialog/a3/accept.png";
    private static final String TASK_UNAVAILABLE_TEMPLATE =
            "images/template/dialog/a3/quzhuagui.png";
    private static final int DIALOG_ROI_X = 250;
    private static final int DIALOG_ROI_Y = 312;
    private static final int DIALOG_ROI_W = 529;
    private static final int DIALOG_ROI_H = 208;
    private static final double MATCH_RATE = 0.82D;

    private final MechanicalPort mechanics;

    @Autowired
    public JianghuLilianDialogLocalOperation(InputSequences inputSequences, CoordinateHelper coordinateHelper) {
        this(new ProductionMechanicalPort(inputSequences, coordinateHelper));
    }

    JianghuLilianDialogLocalOperation(MechanicalPort mechanics) {
        this.mechanics = Objects.requireNonNull(mechanics, "mechanics");
    }

    /**
     * Resolves the 阿三 accept dialog in fixed business priority.
     *
     * @return ACCEPTED after clicking accept_normal; RESET_CLICKED after clicking accept; TASK_UNAVAILABLE
     *         when 去抓鬼 is visible without clicking it; NOT_MATCHED otherwise.
     */
    public AcceptResult executeAccept() {
        int[] roi = dialogRoi();
        Point normal = mechanics.findImageInRegion(ACCEPT_NORMAL_TEMPLATE, roi, MATCH_RATE);
        if (normal != null) {
            return clickTemplateCentre("jianghu-lilian:accept-normal", normal)
                    ? AcceptResult.ACCEPTED
                    : AcceptResult.NOT_MATCHED;
        }

        Point reset = mechanics.findImageInRegion(ACCEPT_RESET_TEMPLATE, roi, MATCH_RATE);
        if (reset != null) {
            return clickTemplateCentre("jianghu-lilian:accept-reset", reset)
                    ? AcceptResult.RESET_CLICKED
                    : AcceptResult.NOT_MATCHED;
        }

        return mechanics.findImageInRegion(TASK_UNAVAILABLE_TEMPLATE, roi, MATCH_RATE) == null
                ? AcceptResult.NOT_MATCHED
                : AcceptResult.TASK_UNAVAILABLE;
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
        RESET_CLICKED,
        TASK_UNAVAILABLE,
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
