package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Objects;

/**
 * Baseline-equivalent local mechanical action for Xiuluo's accept-task option.
 *
 * <p>The Cloud authorizes this one action but does not infer an open option dialog from an NPC
 * click or dialog memory. The bound Client captures the exact baseline window-relative ROI,
 * matches the known accept template, then performs the original atomic move-and-click. A miss is
 * a normal miss, not acceptance.</p>
 */
@Component
public final class XiuluoAcceptDialogLocalOperation {

    static final String ACCEPT_OPTION_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_accept_xianlaiwu2.png";
    static final int ACCEPT_OPTION_ROI_X = 250;
    static final int ACCEPT_OPTION_ROI_Y = 312;
    static final int ACCEPT_OPTION_ROI_W = 529;
    static final int ACCEPT_OPTION_ROI_H = 208;
    static final double ACCEPT_OPTION_TEMPLATE_MATCH_RATE = 0.82D;
    static final int ACCEPT_OPTION_TEMPLATE_CLICK_OFFSET_X = 48;

    private final MechanicalPort mechanics;

    @Autowired
    public XiuluoAcceptDialogLocalOperation(InputSequences inputSequences, CoordinateHelper coordinateHelper) {
        this(new ProductionMechanicalPort(inputSequences, coordinateHelper));
    }

    XiuluoAcceptDialogLocalOperation(MechanicalPort mechanics) {
        this.mechanics = Objects.requireNonNull(mechanics, "mechanics");
    }

    /**
     * Attempts the one baseline accept-option template click.
     *
     * @return {@link Result#ACCEPTED} only after the template was matched and its atomic click was
     *         submitted; {@link Result#NOT_MATCHED} otherwise.
     */
    public Result execute() {
        int[] roi = mechanics.scaledRect(
                ACCEPT_OPTION_ROI_X, ACCEPT_OPTION_ROI_Y, ACCEPT_OPTION_ROI_W, ACCEPT_OPTION_ROI_H);
        Point anchor = mechanics.findImageInRegion(
                ACCEPT_OPTION_TEMPLATE, roi, ACCEPT_OPTION_TEMPLATE_MATCH_RATE);
        if (anchor == null) {
            return Result.NOT_MATCHED;
        }
        return mechanics.moveAndClickLeft(
                "xiuluo:acceptOptionTemplate",
                anchor.x + ACCEPT_OPTION_TEMPLATE_CLICK_OFFSET_X,
                anchor.y,
                150,
                650)
                ? Result.ACCEPTED
                : Result.NOT_MATCHED;
    }

    enum Result {
        ACCEPTED,
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
