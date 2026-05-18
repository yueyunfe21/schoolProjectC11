package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.GameStateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class UICleanerService {

    private final InputSequences inputSequences;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final GameStateUtil gameStateUtil;
    private final BotProperties config;
    private final DialogService dialogService;

    private final Random random = new Random();

    public void cleanUpAll() {
        log.info("UI cleanup started");
        boolean cleanedAny = false;

        if (isWorldMapOpened()) {
            closeMapByDoubleRightClick();
            cleanedAny = true;
        }

        if (forceCloseDialog()) {
            cleanedAny = true;
        }

        if (closeAllGenericWindows()) {
            cleanedAny = true;
        }

        log.info(cleanedAny ? "UI cleanup finished" : "UI already clean");
    }

    private boolean isWorldMapOpened() {
        return coordinateHelper.findImageAbsoluteCoordinate("images/template/world_map_title.png", 0.8) != null;
    }

    private void closeMapByDoubleRightClick() {
        int closeX = tracker.getWindowBaseX() + config.getAnchor_windowTo_map_scroll_X();
        int closeY = tracker.getWindowBaseY() + config.getAnchor_windowTo_map_scroll_Y();
        inputSequences.submitAndWait("uiCleanup:closeMap", List.of(
                InputAction.doubleRightClick(closeX, closeY, 150, 500),
                InputAction.sleep(1000)
        ));
    }

    public boolean closeAllGenericWindows() {
        boolean closedAny = false;
        String[] closeButtonTemplates = {
                "images/template/x1.png",
                "images/template/x2.png"
        };

        for (int i = 0; i < 3; i++) {
            boolean foundInThisPass = false;

            for (String templatePath : closeButtonTemplates) {
                Point closeBtnPoint = coordinateHelper.findImageAbsoluteCoordinate(templatePath, 0.8);

                if (closeBtnPoint != null) {
                    int clickX = closeBtnPoint.x + (random.nextInt(5) - 2);
                    int clickY = closeBtnPoint.y + (random.nextInt(5) - 2);

                    inputSequences.submitAndWait("uiCleanup:closeGenericWindow", List.of(
                            InputAction.clickLeft(clickX, clickY, 150),
                            InputAction.sleep(800)
                    ));

                    foundInThisPass = true;
                    closedAny = true;
                    break;
                }
            }

            if (!foundInThisPass) {
                break;
            }
        }
        return closedAny;
    }

    public boolean forceCloseDialog() {
        DialogService.DialogType type = dialogService.detectDialogType();
        if (type == DialogService.DialogType.NONE) {
            return false;
        }

        if (type == DialogService.DialogType.STORY) {
            dialogService.fastClickStoryDialog();
            sleepInterruptible(1000);
            return true;
        }

        int[] dialogRect = coordinateHelper.getScaledRect(250, 312, 529, 208);
        String imgPath = "images/temp/dialog_close_scan.png";
        tracker.captureToFile("扫除对话框", imgPath, dialogRect[0], dialogRect[1], dialogRect[2], dialogRect[3]);

        List<TextRecognizer.OcrWordResult> allWords = ocr.getAllTextResults(imgPath);
        if (allWords != null && !allWords.isEmpty()) {
            List<String> closeKeywords = Arrays.asList(
                    "取消", "离开", "看一看", "哪儿也", "以后再说", "原来你",
                    "看看", "我还有事", "不", "算了", "暂时", "路过", "再会"
            );
            for (String keyword : closeKeywords) {
                for (TextRecognizer.OcrWordResult word : allWords) {
                    if (word.getText().contains(keyword)) {
                        clickAbsolutePoint(dialogRect[0] + word.getX(), dialogRect[1] + word.getY(), "uiCleanup:dialogCloseKeyword");
                        sleepInterruptible(1000);
                        return true;
                    }
                }
            }
        }

        clickAbsolutePoint(dialogRect[0] + (dialogRect[2] - dialogRect[0]) / 2,
                dialogRect[1] + (dialogRect[3] - dialogRect[1]) / 2,
                "uiCleanup:dialogFallback");
        sleepInterruptible(1000);
        return true;
    }

    private void clickAbsolutePoint(int x, int y, String description) {
        inputSequences.clickLeft(description, x + (random.nextInt(5) - 2), y + (random.nextInt(5) - 2), 150);
    }

    private void sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}