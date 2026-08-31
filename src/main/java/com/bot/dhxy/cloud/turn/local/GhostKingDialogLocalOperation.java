package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.observation.DialogFramePresenceMechanics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;

/** Executes one explicitly selected task-owned 鬼王 dialog template against the bound Client window. */
@Slf4j
@Component
public final class GhostKingDialogLocalOperation {

    private static final String ACCEPT_TEMPLATE = "images/template/dialog/guiwang/accept.png";
    private static final String CANCEL_TEMPLATE = "images/template/dialog/guiwang/quxiao.png";
    private static final int DIALOG_ROI_X = 250;
    private static final int DIALOG_ROI_Y = 312;
    private static final int DIALOG_ROI_W = 529;
    private static final int DIALOG_ROI_H = 208;
    /*
     * G112 复审（2026-08-27）：取消模板未命中 != 对话框没开。quxiao.png 只覆盖"可取消的接任务框"，
     * 剧情框/其他选项框同样返回 NOT_MATCHED，Cloud 据此再点 NPC 就会把点击打进已经开着的框里。
     * 因此未命中时补一次结构化边框存在性判定，用的是全站通用的最大对话框 ROI（与观察循环
     * UnknownPhasePresenceLocalMechanics 同一矩形），只回答"框在不在"，不解释框里是什么。
     */
    private static final int FRAME_ROI_X = 200;
    private static final int FRAME_ROI_Y = 250;
    private static final int FRAME_ROI_W = 640;
    private static final int FRAME_ROI_H = 300;
    private static final double MATCH_RATE = 0.82D;

    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;
    private final GameClientTracker tracker;
    private final DialogFramePresenceMechanics dialogFramePresence = new DialogFramePresenceMechanics();

    public GhostKingDialogLocalOperation(InputSequences inputSequences,
                                         CoordinateHelper coordinateHelper,
                                         @Lazy GameClientTracker tracker) {
        this.inputSequences = inputSequences;
        this.coordinateHelper = coordinateHelper;
        this.tracker = tracker;
    }

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
     * Match and click only the 鬼王 cancel option once, and otherwise report whether a dialog is open at all.
     *
     * @return {@code CANCELLED} when {@code guiwang/quxiao.png} is found and its center click is submitted;
     * {@code DIALOG_WITHOUT_CANCEL} when no cancel option is present but a dialog frame is structurally
     * visible; {@code NOT_MATCHED} only when no dialog frame is visible either; {@code PRESENCE_UNKNOWN}
     * when the frame probe itself is unavailable. No generic dialog fallback is allowed here.
     */
    public CancelResult executeCancel() {
        boolean cancelClicked = clickTemplate("ghost-king:cancel", CANCEL_TEMPLATE);
        Boolean framePresent = cancelClicked ? Boolean.TRUE : probeDialogFramePresent();
        CancelResult result = mapCancelOutcome(cancelClicked, framePresent);
        if (result == CancelResult.PRESENCE_UNKNOWN) {
            log.warn("[ghost-king] cancel template absent and dialog-frame probe unavailable; report PRESENCE_UNKNOWN");
        } else if (result == CancelResult.DIALOG_WITHOUT_CANCEL) {
            log.info("[ghost-king] cancel template absent but a dialog frame is open; report DIALOG_WITHOUT_CANCEL");
        }
        return result;
    }

    /**
     * Pure mapping from the two observed facts to the reported cancel result (G112 复审的唯一口径)。
     *
     * @param cancelClicked whether {@code quxiao.png} matched and its click was submitted.
     * @param framePresent structural dialog-frame presence; {@code null} when the probe was unavailable.
     */
    static CancelResult mapCancelOutcome(boolean cancelClicked, Boolean framePresent) {
        if (cancelClicked) {
            return CancelResult.CANCELLED;
        }
        if (framePresent == null) {
            return CancelResult.PRESENCE_UNKNOWN;
        }
        return framePresent ? CancelResult.DIALOG_WITHOUT_CANCEL : CancelResult.NOT_MATCHED;
    }

    /**
     * Read-only structural probe: is any dialog frame open in the maximum dialog ROI?
     *
     * @return {@code null} when the ROI capture is unavailable, otherwise the frame presence fact.
     */
    private Boolean probeDialogFramePresent() {
        int[] roi = coordinateHelper.getScaledRect(FRAME_ROI_X, FRAME_ROI_Y, FRAME_ROI_W, FRAME_ROI_H);
        BufferedImage frame = tracker.captureToMemory(
                "ghost-king-cancel-frame-presence", roi[0], roi[1], roi[2], roi[3]);
        if (frame == null) {
            return null;
        }
        try {
            boolean present = dialogFramePresence.isPresent(frame);
            // 用户铁律：判定原图落盘。结构化判定没有模板也没有命中坐标，走无模板通道。
            MatchEvidenceStore.saveStructural("ghost-king-cancel-frame-presence", null, frame, present);
            return present;
        } finally {
            frame.flush();
        }
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
        DIALOG_WITHOUT_CANCEL,
        PRESENCE_UNKNOWN,
        NOT_MATCHED
    }
}
