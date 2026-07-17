package com.bot.dhxy.service;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;

/**
 * 物品给予业务流程引擎。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GiveItemService {

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final CoordinateHelper coordinateHelper;
    private final BagService bagService;

    private static final String OPTION_GIVE_TEMPLATE =
            "images/template/dialog/maintenance/dialog_opt_give.png";
    private static final String BTN_GIVE_TEMPLATE = "images/template/300huan/btn_give.png";
    private static final int DIALOG_SMALL_X = 250;
    private static final int DIALOG_SMALL_Y = 345;
    private static final int DIALOG_SMALL_W = 529;
    private static final int DIALOG_SMALL_H = 143;

    /** Closed outcomes for the whole give-item macro that starts from an open option dialog. */
    public enum OpenDialogGiveState {
        GIVEN,
        GIVE_OPTION_NOT_FOUND,
        GIVE_ITEM_FAILED,
        INTERRUPTED
    }

    public boolean executeGive(String targetItemTemplate, Integer knownBagIndex) {
        if (isInputWorkerThread()) {
            return executeGiveDirectForExclusive(targetItemTemplate, knownBagIndex);
        }

        log.info("Give item flow started: {}", targetItemTemplate);
        if (!TaskSleep.sleep(800)) {
            return false;
        }

        boolean itemSelected = bagService.findAndSelectItem(BagService.GIVE_BAG, targetItemTemplate, knownBagIndex);

        if (!itemSelected) {
            log.error("Give item aborted because target item was not selected");
            return false;
        }

        return clickGiveButton();
    }

    public boolean executeGiveDirectForExclusive(String targetItemTemplate, Integer knownBagIndex) {
        if (!isInputWorkerThread()) {
            return executeGive(targetItemTemplate, knownBagIndex);
        }

        log.info("Give item exclusive flow started: {}", targetItemTemplate);
        if (!TaskSleep.sleep(800)) {
            return false;
        }

        boolean itemSelected = bagService.findAndSelectItemDirectForExclusive(
                BagService.GIVE_BAG, targetItemTemplate, knownBagIndex);

        if (!itemSelected) {
            log.error("Give item exclusive flow aborted because target item was not selected");
            return false;
        }

        return clickGiveButtonDirectForExclusive();
    }

    /**
     * Complete a give-item flow from an already-open option dialog inside the current exclusive
     * input-worker callback.
     *
     * <p>The entry match/click and the existing direct item-selection/Give-button flow must remain
     * in this one serialized input boundary. A miss, interrupted wait, failed item selection, or
     * missing final button stops the macro immediately without retrying.</p>
     *
     * @param targetItemTemplate non-null item template path consumed by the existing direct bag
     *                           selection.
     * @param knownBagIndex optional zero-based known bag page; null preserves the existing scan.
     * @return the closed mechanical outcome, preserving entry miss, interrupted entry wait, and
     * direct give success/failure as distinct states.
     * @throws IllegalStateException when called outside the exclusive input-worker callback.
     */
    public OpenDialogGiveState executeGiveFromOpenDialogDirectForExclusive(String targetItemTemplate,
                                                                            Integer knownBagIndex) {
        if (!isInputWorkerThread()) {
            throw new IllegalStateException(
                    "open-dialog give macro must run inside the exclusive input worker section");
        }

        log.info("Give item open-dialog flow started: {}", targetItemTemplate);
        int[] dialogRect = coordinateHelper.getScaledRect(
                DIALOG_SMALL_X, DIALOG_SMALL_Y, DIALOG_SMALL_W, DIALOG_SMALL_H);
        Point giveEntryPoint = coordinateHelper.findGreenTextInRegion(
                OPTION_GIVE_TEMPLATE, dialogRect, 0.85);
        if (giveEntryPoint == null) {
            log.warn("Give item open-dialog flow aborted because the give entry was not found");
            return OpenDialogGiveState.GIVE_OPTION_NOT_FOUND;
        }

        Point safeEntryClick = coordinateHelper.getRandomizedPoint(giveEntryPoint, 20, 5);
        inputProvider.clickLeft(safeEntryClick.x, safeEntryClick.y, 150);
        if (!TaskSleep.sleep(800)) {
            return OpenDialogGiveState.INTERRUPTED;
        }

        return executeGiveDirectForExclusive(targetItemTemplate, knownBagIndex)
                ? OpenDialogGiveState.GIVEN
                : OpenDialogGiveState.GIVE_ITEM_FAILED;
    }

    private boolean clickGiveButton() {
        Point btnGivePoint = coordinateHelper.findImageAbsoluteCoordinate(BTN_GIVE_TEMPLATE, 0.85);
        if (btnGivePoint == null) {
            log.error("Give button not found");
            return false;
        }

        Point safeBtnClick = coordinateHelper.getRandomizedPoint(btnGivePoint, 20, 8);
        boolean clicked = inputSequences.submitAndWait("giveItem:clickGiveButton", List.of(
                InputAction.clickLeft(safeBtnClick.x, safeBtnClick.y, 100),
                InputAction.sleep(1000)
        ));
        if (clicked) {
            log.info("Give item flow finished");
        }
        return clicked;
    }

    private boolean clickGiveButtonDirectForExclusive() {
        Point btnGivePoint = coordinateHelper.findImageAbsoluteCoordinate(BTN_GIVE_TEMPLATE, 0.85);
        if (btnGivePoint == null) {
            log.error("Give button not found");
            return false;
        }

        Point safeBtnClick = coordinateHelper.getRandomizedPoint(btnGivePoint, 20, 8);
        inputProvider.clickLeft(safeBtnClick.x, safeBtnClick.y, 100);
        boolean slept = TaskSleep.sleep(1000);
        if (slept) {
            log.info("Give item exclusive flow finished");
        }
        return slept;
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

}
