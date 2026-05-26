package com.bot.dhxy.service;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
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

    private static final String BTN_GIVE_TEMPLATE = "images/template/300huan/btn_give.png";

    public boolean executeGive(String targetItemTemplate, Integer knownBagIndex) {
        if (isInputWorkerThread()) {
            return executeGiveDirectForExclusive(targetItemTemplate, knownBagIndex);
        }

        log.info("Give item flow started: {}", targetItemTemplate);
        if (!sleep(800)) {
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
        if (!sleep(800)) {
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
        boolean slept = sleep(1000);
        if (slept) {
            log.info("Give item exclusive flow finished");
        }
        return slept;
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
