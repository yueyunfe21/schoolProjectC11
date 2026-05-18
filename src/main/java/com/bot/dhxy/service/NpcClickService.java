package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.tools.GameStateUtil;
import com.bot.dhxy.tools.ImagePreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NpcClickService {

    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final GameClientTracker tracker;
    private final GameStateUtil gameStateUtil;
    private final TextRecognizer ocr;
    private final LocationVisionService locationVisionService;
    private final DialogService dialogService;

    private static final double UX = 20.0;
    private static final double UY = 0.0;
    private static final double VX = 0.0;
    private static final double VY = -20.0;

    private static final int[][] DENSE_BLIND_OFFSETS = {
            {0, 0}, {0, -10}, {0, 10}, {-10, 0}, {10, 0},
            {0, -15}, {0, 15}, {-15, 0}, {15, 0},
            {0, -20}, {0, 20}, {-20, 0}, {20, 0},
            {-10, -10}, {-15, -15}, {-20, -20},
            {10, 10}, {15, 15}, {20, 20},
    };

    private static final String NPC_TAG_REGEX = "(?i).*(NPC|IPC|PC|NP).*";

    private boolean executeClickAndVerify(int x, int y, long firstWaitMs, int maxRetries) {
        inputSequences.submitAndWait("npcClick:clickAndVerify:first", List.of(
                InputAction.clickLeft(x, y, 100),
                InputAction.sleep((int) firstWaitMs)
        ));

        if (dialogService.detectDialogType() == DialogService.DialogType.OPTION) return true;

        for (int i = 1; i <= maxRetries; i++) {
            log.warn("NPC click retry {}", i);
            inputSequences.submitAndWait("npcClick:clickAndVerify:retry", List.of(
                    InputAction.clickLeft(x, y, 100),
                    InputAction.sleep(1000)
            ));
            if (dialogService.detectDialogType() == DialogService.DialogType.OPTION) return true;
        }
        return false;
    }

    private boolean scanMenuAndVerify(int testX, int testY, String targetName) {
        int scanW = 150;
        int scanH = 120;
        int scanX = testX;
        int scanY = testY - scanH;
        String menuScanPath = "images/temp/npc_menu_scan.png";
        String cleanPath = "images/temp/npc_menu_clean.png";

        tracker.captureToFileWithShield("菜单侦查", menuScanPath, scanX, scanY, scanX + scanW, testY);
        ImagePreprocessor.washYellowText(menuScanPath, cleanPath);

        List<TextRecognizer.OcrWordResult> menuWords = ocr.getAllTextResults(cleanPath);

        if (menuWords != null) {
            for (TextRecognizer.OcrWordResult w : menuWords) {
                String text = w.getText();
                if (text == null) continue;

                boolean isNameMatch = text.contains(targetName);
                boolean isTagMatch = text.matches(NPC_TAG_REGEX);

                if (isNameMatch || isTagMatch) {
                    int clickX = scanX + w.getX();
                    int clickY = scanY + w.getY();
                    inputSequences.submitAndWait("npcClick:menuMove", List.of(
                            InputAction.moveMouse(clickX, clickY),
                            InputAction.sleep(100)
                    ));
                    return executeClickAndVerify(clickX, clickY, 800, 1);
                }
            }
        }
        return false;
    }

    public boolean clickNpcSmart(PlayerCharacter player, String mapName, int mapX, int mapY, String npcName, int tuneX, int tuneY) {
        if (!tracker.bringWindowToFront()) {
            log.warn("NPC click aborted because game window cannot focus");
            return false;
        }

        tracker.updateGlobalVision();
        int gameBaseX = tracker.getWindowBaseX();
        int gameBaseY = tracker.getWindowBaseY();
        int screenCenterX = gameBaseX + (1024 / 2);
        int screenCenterY = gameBaseY + (768 / 2);

        TextRecognizer.LocationInfo locInfo = locationVisionService.scanCurrentLocation();

        int scanWidth = 350;
        int scanHeight = 200;
        int scanStartX = screenCenterX - (scanWidth / 2);
        int scanStartY = screenCenterY - (scanHeight / 2);

        String centerScanPath = "images/temp/center_scan_layer1.png";
        String playerScanPath = "images/temp/center_scan_player.png";

        tracker.captureToFileWithShield("中心区域侦查", centerScanPath, scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight);
        ImagePreprocessor.washPurpleTextToBlackAndWhite(centerScanPath, playerScanPath);

        List<TextRecognizer.OcrWordResult> playerWords = ocr.getAllTextResults(playerScanPath);
        Point playerAnchor = null;

        if (playerWords != null && player != null && player.getName() != null) {
            playerAnchor = locationVisionService.extractPlayerPhysicalAnchor(
                    playerWords, player.getName(), scanStartX, scanStartY, 0);
        }

        if (locInfo != null && playerAnchor != null) {
            int deltaLogicX = mapX - locInfo.x;
            int deltaLogicY = mapY - locInfo.y;

            int deltaPhysX = (int) Math.round(deltaLogicX * UX + deltaLogicY * VX);
            int deltaPhysY = (int) Math.round(deltaLogicX * UY + deltaLogicY * VY);

            int targetX = playerAnchor.x + deltaPhysX + tuneX;
            int targetY = playerAnchor.y + deltaPhysY - 50 + tuneY;

            inputSequences.submitAndWait("npcClick:firstShotMove", List.of(
                    InputAction.moveMouse(targetX, targetY),
                    InputAction.sleep(150)
            ));

            if (executeClickAndVerify(targetX, targetY, 1500, 0)) {
                return true;
            }

            sleepQuietly(1500);
        } else if (locInfo != null) {
            log.info("playerAnchor is null, give up the first shot");
        } else {
            log.info("locInfo is null, give up the first shot");
        }

        for (int[] offset : DENSE_BLIND_OFFSETS) {
            int testX = screenCenterX + offset[0];
            int testY = screenCenterY + offset[1] + 20;
            int scanW = 150;
            int scanH = 120;
            int scanX = testX;
            int scanY = testY - scanH;

            boolean success = inputSequences.submitExclusiveAndWait("npcClick:ctrlProbe", () -> {
                sleepQuietly(50);
                BufferedImage frameBefore = tracker.captureToMemory("menu_before", scanX, scanY, scanX + scanW, testY);
                ImagePreprocessor.saveDebugImage(frameBefore, "menu_before.png");
                inputProvider.moveMouse(testX, testY);
                inputProvider.holdCtrl();
                try {
                    sleepQuietly(200);
                    BufferedImage frameAfter = tracker.captureToMemory("menu_after", scanX, scanY, scanX + scanW, testY);
                    ImagePreprocessor.saveDebugImage(frameAfter, "menu_after.png");
                    if (frameBefore != null && frameAfter != null) {
                        boolean changed = !ImageFinder.isMatch(frameBefore, frameAfter, 0.05);
                        frameBefore.flush();
                        frameAfter.flush();
                        if (!changed) {
                            return false;
                        }
                    }
                    return scanMenuAndVerify(testX, testY, npcName);
                } finally {
                    inputProvider.releaseCtrl();
                    sleepQuietly(100);
                }
            });

            if (success) {
                return true;
            }
        }

        tracker.captureToFileWithShield("中心区域侦查(新)", centerScanPath, scanStartX, scanStartY, scanStartX + scanWidth, scanStartY + scanHeight);
        List<TextRecognizer.OcrWordResult> centerWordsNew = ocr.getAllTextResults(centerScanPath);

        if (centerWordsNew != null) {
            for (TextRecognizer.OcrWordResult w : centerWordsNew) {
                if (w.getText() != null && w.getText().contains(npcName)) {
                    int clickX = scanStartX + w.getX();
                    int clickY = scanStartY + w.getY() - 50;

                    inputSequences.submitAndWait("npcClick:visionMove", List.of(
                            InputAction.moveMouse(clickX, clickY),
                            InputAction.sleep(150)
                    ));

                    if (executeClickAndVerify(clickX, clickY, 2000, 1)) {
                        return true;
                    }
                    break;
                }
            }
        }

        log.error("NPC click failed: {}", npcName);
        return false;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
