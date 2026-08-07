package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.protocol.TurnXinshouMechanicalAction;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.service.GiveItemService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.tools.CoordinateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.List;
import java.util.Objects;

/**
 * Synchronous passive executor for one explicitly authorized 新手 title mechanical action.
 *
 * <p>The executor owns no title recognition, consumed set, retry policy, worker thread, phase state,
 * or fallback decision. A caller invokes {@link #execute} once and receives one terminal mechanical
 * result. Any retry or next-stage decision remains outside the Client.</p>
 */
@Component
public final class XinshouTitleMechanicalExecutor {

    private static final double MATCH_RATE = 0.85D;
    // BagService resolves item templates below images/template; these names must stay relative.
    private static final String ITEM_UPGRADE = "xinshou/shengji.png";
    private static final String ITEM_SHELL = "xinshou/hailuo.png";
    private static final String ITEM_REPAIR = "xinshou/xiufu_item.png";
    private static final String ITEM_LUNHUI = "xinshou/lunhui_item.png";
    private static final String OPTION_BLOW = "images/template/xinshou/chuixiang.png";
    private static final String MATERIAL = "images/template/xinshou/wuzi.png";
    private static final String REPAIR_OPENED = "images/template/xinshou/xiufu_opened.png";
    private static final String REPAIR_ITEM_SUCCESS = "images/template/xinshou/xiufu_fangru_suc.png";
    private static final String REPAIR_DONE = "images/template/xinshou/xiufu_alldone.png";
    private static final String LUNHUI_LIFE_DEATH = "images/template/xinshou/shengsi.png";
    private static final String ADOPTION_CONFIRM = "images/template/xinshou/lingyang.png";

    /** User-validated adoption ROI, in reference-window-relative pixels. */
    private static final int ADOPTION_ROI_X = 469;
    private static final int ADOPTION_ROI_Y = 592;
    private static final int ADOPTION_ROI_W = 103;
    private static final int ADOPTION_ROI_H = 29;

    /** Existing DialogService dialog rect, in reference-window-relative pixels. */
    private static final int DIALOG_ROI_X = 250;
    private static final int DIALOG_ROI_Y = 312;
    private static final int DIALOG_ROI_W = 529;
    private static final int DIALOG_ROI_H = 208;

    /** §8.1 reference ROI (1435,560)-(1520,698), converted from base (991,369). */
    private static final int LUNHUI_VERIFY_ROI_X = 444;
    private static final int LUNHUI_VERIFY_ROI_Y = 191;
    private static final int LUNHUI_VERIFY_ROI_W = 85;
    private static final int LUNHUI_VERIFY_ROI_H = 138;
    /** §8.1 reference point (1776,838), converted from base (991,369). */
    private static final int LUNHUI_START_X = 785;
    private static final int LUNHUI_START_Y = 469;

    /** User-validated adoption geometry relative to the matched 确认领养 point. */
    private static final int ADOPTION_PET_OFFSET_X = -30;
    private static final int ADOPTION_PET_OFFSET_Y = -200;

    private static final int[] REPAIR_ITEM_X = {491, 542, 595};
    private static final int[] REPAIR_ITEM_Y = {604, 595, 581};
    private static final int[] REPAIR_SLOT_X = {310, 404, 455};
    private static final int[] REPAIR_SLOT_Y = {470, 518, 521};
    private static final int REPAIR_CLOSE_X = 672;
    private static final int REPAIR_CLOSE_Y = 220;

    private final MechanicalPort mechanics;

    @Autowired
    public XinshouTitleMechanicalExecutor(InputSequences inputSequences,
                                           CoordinateHelper coordinateHelper,
                                           BagService bagService,
                                           GiveItemService giveItemService,
                                           UICleanerService uiCleanerService) {
        this(new ProductionMechanicalPort(
                inputSequences, coordinateHelper, bagService, giveItemService, uiCleanerService));
    }

    XinshouTitleMechanicalExecutor(MechanicalPort mechanics) {
        this.mechanics = Objects.requireNonNull(mechanics, "mechanics");
    }

    /**
     * Execute exactly one mechanical action synchronously.
     *
     * @param action Cloud-authorized mechanical command; non-null.
     * @return typed terminal result for this one invocation; the executor never retries the command.
     */
    public ExecutionResult execute(TurnXinshouMechanicalAction action) {
        if (action == null) {
            return ExecutionResult.failed(ResultCode.INVALID_ACTION);
        }
        return switch (action) {
            case CONFIRM_ADOPTION -> confirmAdoption();
            case USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS -> useUpgradeItemAndClose();
            case USE_SHELL_AND_BLOW -> useShellAndBlow();
            case HAND_IN_MATERIALS -> handInMaterials();
            case REPAIR_ITEMS_ONCE -> repairItemsOnce();
            case CLOSE_REPAIR_WINDOW -> clickRelative(
                    "xinshou:title:repair:close", REPAIR_CLOSE_X, REPAIR_CLOSE_Y, 150)
                    ? ExecutionResult.completed(ResultCode.COMPLETED)
                    : ExecutionResult.failed(ResultCode.INPUT_FAILED);
            case USE_LUNHUI_ITEM_AND_START -> useLunhuiItemAndStart();
            case PRESS_ESCAPE,
                 CLICK_RECOVERY_TEMPLATE,
                 CLICK_PREPARED_POINT,
                 PRESS_ORDINARY_AUTO_COMBAT,
                 CAPTURE_COMBAT,
                 RESTORE_AUTO_COMBAT ->
                     ExecutionResult.failed(ResultCode.INVALID_ACTION);
        };
    }

    private ExecutionResult confirmAdoption() {
        int[] roi = mechanics.scaledRect(
                ADOPTION_ROI_X, ADOPTION_ROI_Y, ADOPTION_ROI_W, ADOPTION_ROI_H);
        Point target = mechanics.findImageInRegion(ADOPTION_CONFIRM, roi, MATCH_RATE);
        if (target == null) {
            return ExecutionResult.failed(ResultCode.ADOPTION_TARGET_NOT_FOUND);
        }
        int petX = target.x + ADOPTION_PET_OFFSET_X;
        int petY = target.y + ADOPTION_PET_OFFSET_Y;
        boolean completed = mechanics.submitInput("xinshou:title:adoption", List.of(
                InputAction.moveMouse(petX, petY),
                InputAction.sleep(80),
                InputAction.clickLeft(petX, petY, 100),
                InputAction.moveMouse(target.x, target.y),
                InputAction.sleep(80),
                InputAction.clickLeft(target.x, target.y, 500)));
        return completed
                ? ExecutionResult.completed(ResultCode.COMPLETED)
                : ExecutionResult.failed(ResultCode.INPUT_FAILED);
    }

    private ExecutionResult useUpgradeItemAndClose() {
        if (!mechanics.useFirstTabItem(
                "xinshou:title:upgrade", ITEM_UPGRADE)) {
            return ExecutionResult.failed(ResultCode.ITEM_NOT_USED);
        }
        mechanics.closeAllGenericWindows();
        return ExecutionResult.completed(ResultCode.COMPLETED);
    }

    private ExecutionResult useShellAndBlow() {
        if (!mechanics.useFirstTabItem("xinshou:title:shell", ITEM_SHELL)) {
            return ExecutionResult.failed(ResultCode.ITEM_NOT_USED);
        }
        return clickDialogTemplate(OPTION_BLOW, 150);
    }

    private ExecutionResult handInMaterials() {
        ExecutionResult selection = clickDialogTemplate(MATERIAL, 300);
        if (!selection.completed()) {
            return selection;
        }
        return mechanics.clickGiveButtonAfterLocalSelection()
                ? ExecutionResult.completed(ResultCode.COMPLETED)
                : ExecutionResult.failed(ResultCode.GIVE_FAILED);
    }

    private ExecutionResult repairItemsOnce() {
        if (!mechanics.useFirstTabItem("xinshou:title:repair", ITEM_REPAIR)) {
            return ExecutionResult.failed(ResultCode.ITEM_NOT_USED);
        }
        if (!isVisible(REPAIR_OPENED)) {
            return ExecutionResult.failed(ResultCode.REPAIR_WINDOW_NOT_VISIBLE);
        }
        for (int item = 0; item < REPAIR_ITEM_X.length; item++) {
            if (!placeRepairItemOnce(item)) {
                return ExecutionResult.failed(ResultCode.REPAIR_ITEM_NOT_PLACED);
            }
        }
        return isVisible(REPAIR_DONE)
                ? ExecutionResult.completed(ResultCode.REPAIR_DONE_VISIBLE)
                : ExecutionResult.completed(ResultCode.REPAIR_ITEMS_PLACED);
    }

    /**
     * Runs at most two fixed candidate-slot passes for one item.
     *
     * <p>A successful first pass returns immediately. Only a complete three-slot miss reselects the
     * same item and runs the same slot order once more.</p>
     */
    private boolean placeRepairItemOnce(int itemIndex) {
        for (int pass = 0; pass < 2; pass++) {
            if (!clickRelative("xinshou:title:repair:item:" + itemIndex + ":pass:" + pass,
                    REPAIR_ITEM_X[itemIndex], REPAIR_ITEM_Y[itemIndex], 150)) {
                return false;
            }
            for (int slot = 0; slot < REPAIR_SLOT_X.length; slot++) {
                if (!clickRelative(
                        "xinshou:title:repair:slot:" + itemIndex + ':' + slot + ":pass:" + pass,
                        REPAIR_SLOT_X[slot], REPAIR_SLOT_Y[slot], 300)) {
                    return false;
                }
                if (isVisible(REPAIR_ITEM_SUCCESS)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ExecutionResult useLunhuiItemAndStart() {
        if (!mechanics.useFirstTabItem("xinshou:title:lunhui", ITEM_LUNHUI)) {
            return ExecutionResult.failed(ResultCode.ITEM_NOT_USED);
        }
        int[] verifyRoi = mechanics.scaledRect(
                LUNHUI_VERIFY_ROI_X, LUNHUI_VERIFY_ROI_Y,
                LUNHUI_VERIFY_ROI_W, LUNHUI_VERIFY_ROI_H);
        if (mechanics.findImageInRegion(LUNHUI_LIFE_DEATH, verifyRoi, MATCH_RATE) == null) {
            return ExecutionResult.failed(ResultCode.LUNHUI_VERIFY_NOT_VISIBLE);
        }
        if (!clickRelative("xinshou:title:lunhui:start", LUNHUI_START_X, LUNHUI_START_Y, 150)) {
            return ExecutionResult.failed(ResultCode.INPUT_FAILED);
        }
        if (!mechanics.sleep(1_000L)) {
            return ExecutionResult.failed(ResultCode.INTERRUPTED);
        }
        return clickRelative("xinshou:title:lunhui:start-2", LUNHUI_START_X, LUNHUI_START_Y, 150)
                ? ExecutionResult.completed(ResultCode.COMPLETED)
                : ExecutionResult.failed(ResultCode.INPUT_FAILED);
    }

    private ExecutionResult clickDialogTemplate(String template, int delayMs) {
        int[] roi = mechanics.scaledRect(DIALOG_ROI_X, DIALOG_ROI_Y, DIALOG_ROI_W, DIALOG_ROI_H);
        Point point = mechanics.findImageInRegion(template, roi, MATCH_RATE);
        if (point == null) {
            return ExecutionResult.failed(ResultCode.DIALOG_TARGET_NOT_FOUND);
        }
        return mechanics.moveAndClickLeft(
                "xinshou:title:dialog-template:" + template, point.x, point.y, 80, delayMs)
                ? ExecutionResult.completed(ResultCode.COMPLETED)
                : ExecutionResult.failed(ResultCode.INPUT_FAILED);
    }

    private boolean clickRelative(String source, int relativeX, int relativeY, int delayMs) {
        int[] rect = mechanics.scaledRect(relativeX, relativeY, 1, 1);
        return mechanics.moveAndClickLeft(source, rect[0], rect[1], 80, delayMs);
    }

    private boolean isVisible(String template) {
        return mechanics.findImageAbsoluteCoordinate(template, MATCH_RATE) != null;
    }

    /** Terminal result of one synchronous mechanical invocation. */
    public record ExecutionResult(boolean completed, ResultCode code) {
        public ExecutionResult {
            Objects.requireNonNull(code, "code");
        }

        static ExecutionResult completed(ResultCode code) {
            return new ExecutionResult(true, code);
        }

        static ExecutionResult failed(ResultCode code) {
            return new ExecutionResult(false, code);
        }
    }

    public enum ResultCode {
        COMPLETED,
        REPAIR_ITEMS_PLACED,
        REPAIR_DONE_VISIBLE,
        INVALID_ACTION,
        ADOPTION_TARGET_NOT_FOUND,
        ITEM_NOT_USED,
        DIALOG_TARGET_NOT_FOUND,
        GIVE_FAILED,
        REPAIR_WINDOW_NOT_VISIBLE,
        REPAIR_ITEM_NOT_PLACED,
        LUNHUI_VERIFY_NOT_VISIBLE,
        INPUT_FAILED,
        INTERRUPTED
    }

    interface MechanicalPort {
        boolean submitInput(String description, List<InputAction> actions);

        boolean useFirstTabItem(String source, String template);

        void closeAllGenericWindows();

        boolean clickGiveButtonAfterLocalSelection();

        int[] scaledRect(int relativeX, int relativeY, int width, int height);

        Point findImageInRegion(String template, int[] roi, double matchRate);

        Point findImageAbsoluteCoordinate(String template, double matchRate);

        boolean moveAndClickLeft(String description, int screenX, int screenY, int settleMs, int delayMs);

        boolean sleep(long millis);
    }

    private static final class ProductionMechanicalPort implements MechanicalPort {
        private final InputSequences inputSequences;
        private final CoordinateHelper coordinateHelper;
        private final BagService bagService;
        private final GiveItemService giveItemService;
        private final UICleanerService uiCleanerService;

        private ProductionMechanicalPort(InputSequences inputSequences,
                                         CoordinateHelper coordinateHelper,
                                         BagService bagService,
                                         GiveItemService giveItemService,
                                         UICleanerService uiCleanerService) {
            this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
            this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
            this.bagService = Objects.requireNonNull(bagService, "bagService");
            this.giveItemService = Objects.requireNonNull(giveItemService, "giveItemService");
            this.uiCleanerService = Objects.requireNonNull(uiCleanerService, "uiCleanerService");
        }

        @Override
        public boolean submitInput(String description, List<InputAction> actions) {
            return inputSequences.submitAndWait(description, actions);
        }

        @Override
        public boolean useFirstTabItem(String source, String template) {
            Boolean used = bagService.withMainBagOpenGuarded(
                    source, () -> true, null, session -> session.useItemOnFirstTab(template));
            return Boolean.TRUE.equals(used);
        }

        @Override
        public void closeAllGenericWindows() {
            uiCleanerService.closeAllGenericWindows();
        }

        @Override
        public boolean clickGiveButtonAfterLocalSelection() {
            return giveItemService.clickGiveButtonAfterLocalSelection();
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
        public Point findImageAbsoluteCoordinate(String template, double matchRate) {
            return coordinateHelper.findImageAbsoluteCoordinate(template, matchRate);
        }

        @Override
        public boolean moveAndClickLeft(
                String description, int screenX, int screenY, int settleMs, int delayMs) {
            return inputSequences.moveAndClickLeft(
                    description, screenX, screenY, settleMs, delayMs);
        }

        @Override
        public boolean sleep(long millis) {
            return TaskSleep.sleep(millis);
        }
    }
}
