package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.QuestTargetInfo;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务情报总管。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestManagerService {

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer ocr;
    private final GameContext context;
    private final WindowScopedTempPath windowScopedTempPath;

    private static final String ANCHOR_PATH = "images/template/task/task_fenxiang.png";

    private static final int OFFSET_X = -497;
    private static final int OFFSET_Y = 8;
    private static final int PANEL_H = 295;
    private static final int W_LEFT = 223;
    private static final int W_RIGHT = 290;
    private static final int OFFSET_X_RIGHT = OFFSET_X + W_LEFT;
    private static final int DETAIL_TEXT_OFFSET_X = -269;
    private static final int DETAIL_TEXT_OFFSET_Y = 12;
    private static final int DETAIL_TEXT_W = 264;
    private static final int DETAIL_TEXT_H = 50;
    private static final int CURRENT_TASK_TAB_X = -442;
    private static final int CURRENT_TASK_TAB_Y = -25;

    private static final int P1_X = -209;
    private static final int P1_Y = 37;

    private static final double THRESHOLD_STRICT = 0.85;
    private static final double THRESHOLD_NORMAL = 0.80;

    private static final int GLOW_RGB_MIN = 220;
    private static final int GLOW_TARGET = 15;

    private static final long SLOW = 800;
    private static final long MID = 500;
    private static final long FAST = 200;

    private static final Pattern QUEST_PATTERN = Pattern.compile("([^\\(]+).*?在\\s*([\\u4e00-\\u9fa5]+)\\s*\\((\\d+)\\s*,\\s*(\\d+)\\)");

    private static final String[] MONSTERS = {
            "images/template/wuhuan/p2_guanpian.png",
            "images/template/wuhuan/p2_daohaozei.png",
            "images/template/wuhuan/p2_wuchi.png",
            "images/template/wuhuan/p2_shiyinggui.png",
            "images/template/wuhuan/p2_xie.png",
    };

    public enum PathingResult { SUCCESS, FINISHED, UI_ERROR }

    public PathingResult activateAndTriggerWuHuanPathing() {
        AtomicReference<PathingResult> result = new AtomicReference<>(PathingResult.UI_ERROR);
        boolean completed = inputSequences.submitExclusiveAndWait("quest:wuhuanActivateAndPathingTransaction", () -> {
            result.set(activateAndTriggerWuHuanPathingDirect());
            return true;
        });
        return completed ? result.get() : PathingResult.UI_ERROR;
    }

    public PathingResult activateAndTriggerWuHuanPathingDirectForExclusive() {
        if (!isInputWorkerThread()) {
            return activateAndTriggerWuHuanPathing();
        }
        return activateAndTriggerWuHuanPathingDirect();
    }

    private PathingResult activateAndTriggerWuHuanPathingDirect() {
        if (!activateTaskIfPresentDirect("wuhuan", true)) {
            log.info("wuhuan activate-and-pathing: task not found");
            return PathingResult.FINISHED;
        }

        PathingResult p2Result = triggerWuHuanNativePathingP2Direct(true);
        if (p2Result == PathingResult.SUCCESS) {
            log.info("wuhuan activate-and-pathing: P2 triggered");
            return PathingResult.SUCCESS;
        }

        log.info("wuhuan activate-and-pathing: P2 not available, trying P1");
        PathingResult p1Result = triggerWuHuanNativePathingP1Direct(true);
        if (p1Result == PathingResult.SUCCESS) {
            log.info("wuhuan activate-and-pathing: P1 triggered");
            return PathingResult.SUCCESS;
        }

        closePanelDirect();
        log.warn("wuhuan activate-and-pathing: task exists but P2/P1 pathing failed");
        return PathingResult.UI_ERROR;
    }

    public PathingResult triggerWuHuanNativePathingP1() { return triggerWuHuanNativePathingP1(false); }

    public PathingResult triggerWuHuanNativePathingP1(boolean skipScan) {
        AtomicReference<PathingResult> result = new AtomicReference<>(PathingResult.UI_ERROR);
        boolean completed = inputSequences.submitExclusiveAndWait("quest:p1PanelTransaction", () -> {
            result.set(triggerWuHuanNativePathingP1Direct(skipScan));
            return true;
        });
        return completed ? result.get() : PathingResult.UI_ERROR;
    }

    private PathingResult triggerWuHuanNativePathingP1Direct(boolean skipScan) {
        if (!skipScan && !activateTaskIfPresentDirect("wuhuan", true)) return PathingResult.FINISHED;

        Point anchor = ensurePanelDirect();
        if (anchor == null) return PathingResult.UI_ERROR;

        Point p = coordinateHelper.getRandomizedPoint(anchor.x + P1_X, anchor.y + P1_Y, 30, 8);
        log.info("🎯 [P1盲狙] 准备点击下一环 NPC 链接：anchor=({}, {}) offset=({}, {}) click=({}, {})",
                anchor.x, anchor.y, P1_X, P1_Y, p.x, p.y);
        inputProvider.clickLeft(p.x, p.y, 100);
        boolean ok = sleepInterruptible(1200);
        log.info("🎯 [P1盲狙] 点击序列结果：{}", ok);
        return ok ? PathingResult.SUCCESS : PathingResult.UI_ERROR;
    }

    public PathingResult triggerWuHuanNativePathingP2() { return triggerWuHuanNativePathingP2(false); }

    public PathingResult triggerWuHuanNativePathingP2(boolean skipScan) {
        AtomicReference<PathingResult> result = new AtomicReference<>(PathingResult.UI_ERROR);
        boolean completed = inputSequences.submitExclusiveAndWait("quest:p2PanelTransaction", () -> {
            result.set(triggerWuHuanNativePathingP2Direct(skipScan));
            return true;
        });
        return completed ? result.get() : PathingResult.UI_ERROR;
    }

    private PathingResult triggerWuHuanNativePathingP2Direct(boolean skipScan) {
        if (!skipScan && !activateTaskIfPresentDirect("wuhuan", true)) return PathingResult.UI_ERROR;

        Point anchor = ensurePanelDirect();
        if (anchor == null) return PathingResult.UI_ERROR;

        int[] rect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X_RIGHT, OFFSET_Y, W_RIGHT, PANEL_H);

        String rawPath = windowScopedTempPath.resolve("p2_raw.png");
        if (!tracker.captureToFile("P2右侧", rawPath, rect[0], rect[1], rect[2], rect[3])) {
            closePanelDirect();
            return PathingResult.UI_ERROR;
        }

        String washedPath = windowScopedTempPath.resolve("p2_washed.png");
        ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath);

        for (String m : MONSTERS) {
            double[] res = ImageFinder.find(washedPath, m, THRESHOLD_NORMAL);
            if (res != null && res.length >= 2) {
                log.info("P2识图已经匹配成功");
                Point p = coordinateHelper.getRandomizedPoint(rect[0] + (int) res[0], rect[1] + (int) res[1], 8, 4);
                inputProvider.clickLeft(p.x, p.y, 100);
                if (!sleepInterruptible(MID)) return PathingResult.UI_ERROR;
                log.info("P2 pathing clicked, skip Alt+Q close after pathing click");
                return PathingResult.SUCCESS;
            }
        }
        log.info("P2识图匹配失败");
        return PathingResult.UI_ERROR;
    }

    public boolean activateTaskIfPresent(String task) { return activateTaskIfPresent(task, false); }

    public boolean activateTaskIfPresentExclusive(String task, boolean keepOpen) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        boolean completed = inputSequences.submitExclusiveAndWait("quest:activateTaskIfPresent:" + task, () -> {
            result.set(activateTaskIfPresentDirect(task, keepOpen));
            return true;
        });
        return completed && Boolean.TRUE.equals(result.get());
    }

    public boolean activateTaskIfPresent(String task, boolean keepOpen) {
        Point anchor = ensurePanel();
        if (anchor == null) return false;

        int[] rect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X, OFFSET_Y, W_LEFT, PANEL_H);
        String titleImg = "images/template/task/" + task + "_title.png";
        boolean titleClicked = false;

        for (int p = 0; p < 3; p++) {
            Point taskPt = findTaskLabelInRegion(task, rect);
            if (taskPt != null) {
                if (isTextGlowing(taskPt)) {
                    log.info("✅ 任务 [{}] 已高亮", task);
                } else {
                    log.info("🖱️ 激活任务 [{}]", task);
                    click(taskPt.x, taskPt.y, 20, 5, MID);
                }
                if (!keepOpen) closePanel("quest:activateClose");
                return true;
            }

            Point titlePt = titleClicked ? null : coordinateHelper.findImageInRegion(titleImg, rect, THRESHOLD_STRICT);
            if (titlePt != null) {
                click(titlePt.x + 30, titlePt.y + 5, 20, 5, SLOW);
                titleClicked = true;
                continue;
            }

            if (p < 2) scroll(anchor, 3);
        }

        closePanel("quest:activateNotFoundClose");
        return false;
    }

    private boolean activateTaskIfPresentDirect(String task, boolean keepOpen) {
        Point anchor = ensurePanelDirect();
        if (anchor == null) return false;

        int[] rect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X, OFFSET_Y, W_LEFT, PANEL_H);
        String titleImg = "images/template/task/" + task + "_title.png";
        boolean titleClicked = false;

        for (int p = 0; p < 3; p++) {
            Point taskPt = findTaskLabelInRegion(task, rect);
            if (taskPt != null) {
                if (isTextGlowing(taskPt)) {
                    log.info("task [{}] already active", task);
                } else {
                    log.info("activate task [{}]", task);
                    clickDirect(taskPt.x, taskPt.y, 20, 5, MID);
                }
                if (!keepOpen) closePanelDirect();
                return true;
            }

            Point titlePt = titleClicked ? null : coordinateHelper.findImageInRegion(titleImg, rect, THRESHOLD_STRICT);
            if (titlePt != null) {
                clickDirect(titlePt.x + 30, titlePt.y + 5, 20, 5, SLOW);
                titleClicked = true;
                continue;
            }

            if (p < 2) scrollDirect(anchor, 3);
        }

        closePanelDirect();
        return false;
    }

    private Point findTaskLabelInRegion(String task, int[] rect) {
        for (String templatePath : taskLabelTemplatePaths(task)) {
            Point point = coordinateHelper.findImageInRegion(templatePath, rect, THRESHOLD_STRICT);
            if (point != null) {
                log.info("task label matched: task={} template={} point=({}, {})",
                        task, templatePath, point.x, point.y);
                return point;
            }
        }
        return null;
    }

    private List<String> taskLabelTemplatePaths(String task) {
        List<String> candidates = new ArrayList<>();
        candidates.add("images/template/task/" + task + ".png");
        candidates.add("images/template/task/" + task + "_active.png");
        candidates.add("images/template/task/" + task + "_selected.png");

        List<String> existing = new ArrayList<>();
        for (String candidate : candidates) {
            if (new File(candidate).exists()) {
                existing.add(candidate);
            }
        }
        return existing.isEmpty() ? List.of(candidates.get(0)) : existing;
    }

    public String readCurrentQuestDetailTextForTask(String task) {
        AtomicReference<String> result = new AtomicReference<>("");
        boolean completed = inputSequences.submitExclusiveAndWait("quest:readDetailText:" + task, () -> {
            result.set(readCurrentQuestDetailTextForTaskDirect(task));
            return true;
        });
        return completed ? result.get() : "";
    }

    public BufferedImage captureCurrentQuestDetailImageForTask(String task) {
        AtomicReference<BufferedImage> result = new AtomicReference<>(null);
        boolean completed = inputSequences.submitExclusiveAndWait("quest:captureDetailImage:" + task, () -> {
            result.set(captureCurrentQuestDetailImageForTaskDirect(task));
            return true;
        });
        return completed ? result.get() : null;
    }

    public QuestDetailCapture captureCurrentQuestDetailForTask(String task) {
        AtomicReference<QuestDetailCapture> result = new AtomicReference<>(QuestDetailCapture.empty());
        boolean completed = inputSequences.submitExclusiveAndWait("quest:captureDetail:" + task, () -> {
            result.set(captureCurrentQuestDetailForTaskDirect(task));
            return true;
        });
        return completed ? result.get() : QuestDetailCapture.empty();
    }

    private BufferedImage captureCurrentQuestDetailImageForTaskDirect(String task) {
        QuestDetailCapture capture = captureCurrentQuestDetailForTaskDirect(task);
        return capture.image();
    }

    private QuestDetailCapture captureCurrentQuestDetailForTaskDirect(String task) {
        if (task == null || task.isBlank()) {
            log.warn("quest detail capture requested without task");
            return QuestDetailCapture.empty();
        }

        boolean activated = activateTaskIfPresentDirect(task, true);
        if (!activated) {
            log.info("quest detail capture skipped, task not found: {}", task);
            return QuestDetailCapture.empty();
        }

        Point anchor = ensurePanelDirect();
        if (anchor == null) {
            log.warn("quest detail capture failed, panel anchor not found: {}", task);
            return QuestDetailCapture.empty();
        }

        try {
            int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(
                    anchor, DETAIL_TEXT_OFFSET_X, DETAIL_TEXT_OFFSET_Y, DETAIL_TEXT_W, DETAIL_TEXT_H);
            BufferedImage image = tracker.captureToMemory("quest-detail-image-" + task,
                    rightRect[0], rightRect[1], rightRect[2], rightRect[3]);
            if (image == null) {
                log.warn("quest detail image capture failed: task={}", task);
                return QuestDetailCapture.empty();
            }
            String latestPath = saveQuestDetailDebugImage(task, image);
            return new QuestDetailCapture(image, latestPath);
        } finally {
            closePanelDirect();
        }
    }

    private String saveQuestDetailDebugImage(String task, BufferedImage image) {
        String safeTask = task == null || task.isBlank() ? "unknown" : task;
        String latestPath = windowScopedTempPath.resolve("quest_detail_" + safeTask + ".png");
        String historyPath = windowScopedTempPath.resolve("quest_detail_" + safeTask + "_" + System.currentTimeMillis() + ".png");
        saveQuestDetailDebugImageToPath(safeTask, image, latestPath);
        saveQuestDetailDebugImageToPath(safeTask, image, historyPath);
        return latestPath;
    }

    private void saveQuestDetailDebugImageToPath(String task, BufferedImage image, String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("quest detail debug mkdir failed: path={}", parent);
            return;
        }
        try {
            ImageIO.write(image, "png", file);
            log.info("quest detail debug saved: task={} path={}", task, path);
        } catch (IOException e) {
            log.warn("quest detail debug save failed: task={} path={}", task, path, e);
        }
    }

    private String readCurrentQuestDetailTextForTaskDirect(String task) {
        if (task == null || task.isBlank()) {
            log.warn("quest detail read requested without task");
            return "";
        }

        boolean activated = activateTaskIfPresentDirect(task, true);
        if (!activated) {
            log.info("quest detail read skipped, task not found: {}", task);
            return "";
        }

        Point anchor = ensurePanelDirect();
        if (anchor == null) {
            log.warn("quest detail read failed, panel anchor not found: {}", task);
            return "";
        }

        try {
            int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(
                    anchor, DETAIL_TEXT_OFFSET_X, DETAIL_TEXT_OFFSET_Y, DETAIL_TEXT_W, DETAIL_TEXT_H);
            String detailPath = windowScopedTempPath.resolve("quest_detail_scan_" + task + ".png");
            if (!tracker.captureToFile("quest-detail-" + task, detailPath,
                    rightRect[0], rightRect[1], rightRect[2], rightRect[3])) {
                log.warn("quest detail capture failed: task={}", task);
                return "";
            }

            List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResults(detailPath);
            if (results == null || results.isEmpty()) {
                log.info("quest detail OCR empty: task={} path={}", task, detailPath);
                return "";
            }

            StringBuilder fullText = new StringBuilder();
            for (TextRecognizer.OcrWordResult word : results) {
                if (word.getText() != null) {
                    fullText.append(word.getText());
                }
            }
            String text = fullText.toString();
            log.info("quest detail OCR result: task={} text={}", task, text);
            return text;
        } finally {
            closePanelDirect();
        }
    }

    public QuestTargetInfo fetchCurrentQuestInfo(String expectedTaskImage) {
        log.info("📡 [深度解析] 启动重型 OCR 引擎获取任务情报...");

        inputSequences.submitAndWait("quest:fetchOpenPanel", List.of(
                InputAction.pressAltQ(),
                InputAction.sleep((int) SLOW)
        ));
        Point anchor = ensurePanel();
        if (anchor == null) return null;

        int[] leftRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X, OFFSET_Y, W_LEFT, PANEL_H);
        String taskTemplatePath = "images/template/" + expectedTaskImage;
        Point taskLabelPoint = coordinateHelper.findImageAbsoluteCoordinate(taskTemplatePath, THRESHOLD_STRICT);

        if (taskLabelPoint == null) {
            log.warn("⏭️ 未发现任务标签 [{}]，判定可能已完成", expectedTaskImage);
            closePanel("quest:fetchNoTaskClose");
            return null;
        }

        click(taskLabelPoint.x, taskLabelPoint.y, 20, 5, MID);

        int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(anchor, OFFSET_X_RIGHT, OFFSET_Y, W_RIGHT, PANEL_H);
        String detailPath = windowScopedTempPath.resolve("quest_detail_scan.png");

        tracker.captureToFile("任务详情", detailPath, rightRect[0], rightRect[1], rightRect[2], rightRect[3]);

        List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResultsForMatch(
                detailPath,
                "quest-target-info:" + expectedTaskImage,
                this::matchesQuestTargetInfo);
        if (results == null || results.isEmpty()) {
            closePanel("quest:fetchEmptyOcrClose");
            return null;
        }

        StringBuilder fullText = new StringBuilder();
        for (TextRecognizer.OcrWordResult word : results) {
            fullText.append(word.getText());
        }

        String cleanText = fullText.toString();
        log.info("🔍 OCR 结果: {}", cleanText);

        Matcher matcher = QUEST_PATTERN.matcher(cleanText);
        if (matcher.find()) {
            String npc = matcher.group(1).trim();
            String map = matcher.group(2).trim();
            int x = Integer.parseInt(matcher.group(3));
            int y = Integer.parseInt(matcher.group(4));

            log.info("✅ 情报解锁 -> NPC: [{}], 地图: [{}], 坐标: ({}, {})", npc, map, x, y);
            context.setCurrentTaskName(npc);

            closePanel("quest:fetchParsedClose");
            return new QuestTargetInfo(npc, map, x, y, cleanText);
        }

        log.warn("quest target OCR parse failed: expectedTaskImage={} path={} text={}",
                expectedTaskImage, detailPath, cleanText);
        closePanel("quest:fetchParseFailedClose");
        return null;
    }

    private boolean matchesQuestTargetInfo(List<TextRecognizer.OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return false;
        }
        StringBuilder fullText = new StringBuilder();
        for (TextRecognizer.OcrWordResult word : words) {
            if (word != null && word.getText() != null) {
                fullText.append(word.getText());
            }
        }
        return QUEST_PATTERN.matcher(fullText.toString()).find();
    }

    private Point ensurePanel() {
        Point a = findAnchor();
        if (a == null) {
            inputSequences.submitAndWait("quest:ensurePanelAltQ", List.of(
                    InputAction.pressAltQ(),
                    InputAction.sleep((int) SLOW)
            ));
            a = findAnchor();
        }
        if (a != null) {
            selectCurrentTaskTab(a);
        }
        return a;
    }

    private Point ensurePanelDirect() {
        Point a = findAnchor();
        if (a == null) {
            inputProvider.pressAltQ();
            if (!sleepInterruptible(SLOW)) {
                return null;
            }
            a = findAnchor();
        }
        if (a != null && !selectCurrentTaskTabDirect(a)) {
            return null;
        }
        return a;
    }

    private void selectCurrentTaskTab(Point anchor) {
        Point tab = coordinateHelper.getRandomizedPoint(
                anchor.x + CURRENT_TASK_TAB_X,
                anchor.y + CURRENT_TASK_TAB_Y,
                18,
                5);
        log.info("quest panel select current-task tab: anchor=({}, {}) offset=({}, {}) click=({}, {})",
                anchor.x, anchor.y, CURRENT_TASK_TAB_X, CURRENT_TASK_TAB_Y, tab.x, tab.y);
        inputSequences.submitAndWait("quest:selectCurrentTaskTab", List.of(
                InputAction.clickLeft(tab.x, tab.y, 100),
                InputAction.sleep((int) FAST)
        ));
    }

    private boolean selectCurrentTaskTabDirect(Point anchor) {
        Point tab = coordinateHelper.getRandomizedPoint(
                anchor.x + CURRENT_TASK_TAB_X,
                anchor.y + CURRENT_TASK_TAB_Y,
                18,
                5);
        log.info("quest panel select current-task tab direct: anchor=({}, {}) offset=({}, {}) click=({}, {})",
                anchor.x, anchor.y, CURRENT_TASK_TAB_X, CURRENT_TASK_TAB_Y, tab.x, tab.y);
        inputProvider.clickLeft(tab.x, tab.y, 100);
        return sleepInterruptible(FAST);
    }

    private boolean isTextGlowing(Point pt) {
        BufferedImage img = tracker.captureToMemory("probe", pt.x - 40, pt.y - 10, pt.x + 40, pt.y + 10);
        if (img == null) return false;
        int count = 0;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int c = img.getRGB(x, y);
                if (((c >> 16) & 0xFF) > GLOW_RGB_MIN
                        && ((c >> 8) & 0xFF) > GLOW_RGB_MIN
                        && (c & 0xFF) > GLOW_RGB_MIN) {
                    count++;
                }
            }
        }
        img.flush();
        return count > GLOW_TARGET;
    }

    private void click(int x, int y, int rx, int ry, long delay) {
        Point p = coordinateHelper.getRandomizedPoint(x, y, rx, ry);
        inputSequences.submitAndWait("quest:click", List.of(
                InputAction.clickLeft(p.x, p.y, 100),
                InputAction.sleep((int) delay)
        ));
    }

    private boolean clickDirect(int x, int y, int rx, int ry, long delay) {
        Point p = coordinateHelper.getRandomizedPoint(x, y, rx, ry);
        inputProvider.clickLeft(p.x, p.y, 100);
        return sleepInterruptible(delay);
    }

    private void scroll(Point a, int steps) {
        Point h = coordinateHelper.getRandomizedPoint(a.x - 400, a.y + 174, 50, 100);
        inputSequences.submitAndWait("quest:scroll", List.of(
                InputAction.moveMouse(h.x, h.y),
                InputAction.sleep((int) FAST),
                InputAction.scrollDown(steps),
                InputAction.sleep((int) MID)
        ));
    }

    private boolean scrollDirect(Point a, int steps) {
        Point h = coordinateHelper.getRandomizedPoint(a.x - 400, a.y + 174, 50, 100);
        inputProvider.moveMouse(h.x, h.y);
        if (!sleepInterruptible(FAST)) {
            return false;
        }
        inputProvider.scrollDown(steps);
        return sleepInterruptible(MID);
    }

    private void closePanel(String description) {
        inputSequences.submitAndWait(description, List.of(InputAction.pressAltQ()));
    }

    private void closePanelDirect() {
        inputProvider.pressAltQ();
    }

    private boolean sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private Point findAnchor() { return coordinateHelper.findImageAbsoluteCoordinate(ANCHOR_PATH, THRESHOLD_NORMAL); }

    public record QuestDetailCapture(BufferedImage image, String imagePath) {
        private static QuestDetailCapture empty() {
            return new QuestDetailCapture(null, "");
        }

        public boolean hasImage() {
            return image != null;
        }
    }
}
