package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Point;

/** Executes one explicitly selected task-owned 鬼王 dialog template against the bound Client window. */
@Component
@RequiredArgsConstructor
public final class GhostKingDialogLocalOperation {

    private static final String ACCEPT_TEMPLATE = "images/template/dialog/guiwang/accept.png";
    private static final String CANCEL_TEMPLATE = "images/template/dialog/guiwang/quxiao.png";
    private static final int DIALOG_ROI_X = 250;
    private static final int DIALOG_ROI_Y = 312;
    private static final int DIALOG_ROI_W = 529;
    private static final int DIALOG_ROI_H = 208;
    private static final double MATCH_RATE = 0.82D;

    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;

    /**
     * Match and click the 鬼王 accept option once.
     *
     * @return {@code ACCEPTED} only when the task-owned template is found and its center click is submitted;
     * otherwise {@code NOT_MATCHED}. The Cloud still verifies the fresh 鬼王 Tracker title afterwards.
     */
    public AcceptResult executeAccept() {
        return clickTemplate("ghost-king:accept", ACCEPT_TEMPLATE)
                ? AcceptResult.ACCEPTED
                : AcceptResult.NOT_MATCHED;
    }

    /**
     * Match and click only the 鬼王 cancel option once.
     *
     * @return {@code CANCELLED} only when {@code guiwang/quxiao.png} is found and its center click is
     * submitted; otherwise {@code NOT_MATCHED}. No generic dialog fallback is allowed here.
     */
    public CancelResult executeCancel() {
        return clickTemplate("ghost-king:cancel", CANCEL_TEMPLATE)
                ? CancelResult.CANCELLED
                : CancelResult.NOT_MATCHED;
    }

    private boolean clickTemplate(String source, String template) {
        int[] roi = coordinateHelper.getScaledRect(
                DIALOG_ROI_X, DIALOG_ROI_Y, DIALOG_ROI_W, DIALOG_ROI_H);
        Point centre = coordinateHelper.findImageInRegion(template, roi, MATCH_RATE);
        if (centre == null) {
            return false;
        }
        return inputSequences.moveAndClickLeft(
                source, centre.x, centre.y, 150, 650);
    }

    public enum AcceptResult {
        ACCEPTED,
        NOT_MATCHED
    }

    public enum CancelResult {
        CANCELLED,
        NOT_MATCHED
    }
}
