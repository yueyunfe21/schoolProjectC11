package com.bot.dhxy.service;


import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowKeyboardService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.WindowAwareInputCoordinator;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.navigation.PathingResult;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 任务情报总管。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestManagerService {

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final WindowAwareInputCoordinator inputCoordinator;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final GameContext context;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final BoundWindowKeyboardService boundWindowKeyboardService;

    private static final String ANCHOR_PATH = "images/template/task/task_fenxiang.png";

    private static final int OFFSET_X = -497;
    private static final int OFFSET_Y = 8;
    private static final int PANEL_H = 295;
    private static final int W_LEFT = 223;
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
    private static final int WUHUAN_TASK_LINK_CLICK_HOLD_MS = 150;


    public boolean activateTaskIfPresent(String task) { return activateTaskIfPresent(task, false); }

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

    public QuestDetailCapture captureCurrentQuestDetailForTask(String task) {
        AtomicReference<QuestDetailCapture> result = new AtomicReference<>(QuestDetailCapture.empty());
        boolean completed = inputSequences.submitExclusiveAndWait("quest:captureDetail:" + task, () -> {
            result.set(captureCurrentQuestDetailForTaskDirect(task));
            return true;
        });
        QuestDetailCapture capture = completed ? result.get() : QuestDetailCapture.empty();
        BufferedImage image = capture.image();
        log.info("quest detail capture request finished: task={} completed={} hasImage={} path={} size={}x{}",
                task, completed, image != null, capture.imagePath(),
                image == null ? 0 : image.getWidth(), image == null ? 0 : image.getHeight());
        return capture;
    }

    private QuestDetailCapture captureCurrentQuestDetailForTaskDirect(String task) {
        if (task == null || task.isBlank()) {
            log.warn("quest detail capture requested without task");
            return QuestDetailCapture.empty();
        }

        log.info("quest detail capture start: task={}", task);
        boolean activated = activateTaskIfPresentDirect(task, true);
        log.info("quest detail capture task activation: task={} activated={}", task, activated);
        if (!activated) {
            log.info("quest detail capture skipped, task not found: {}", task);
            return QuestDetailCapture.empty();
        }

        Point anchor = ensurePanelDirect();
        if (anchor == null) {
            log.warn("quest detail capture failed, panel anchor not found: {}", task);
            return QuestDetailCapture.empty();
        }
        log.info("quest detail capture panel anchor: task={} anchor=({}, {})", task, anchor.x, anchor.y);

        try {
            int[] rightRect = coordinateHelper.getAbsoluteRectByAnchor(
                    anchor, DETAIL_TEXT_OFFSET_X, DETAIL_TEXT_OFFSET_Y, DETAIL_TEXT_W, DETAIL_TEXT_H);
            log.info("quest detail capture rect: task={} x={} y={} w={} h={}",
                    task, rightRect[0], rightRect[1], rightRect[2], rightRect[3]);
            BufferedImage image = tracker.captureToMemory("quest-detail-image-" + task,
                    rightRect[0], rightRect[1], rightRect[2], rightRect[3]);
            if (image == null) {
                log.warn("quest detail image capture failed: task={}", task);
                return QuestDetailCapture.empty();
            }
            String latestPath = saveQuestDetailDebugImage(task, image);
            log.info("quest detail capture success: task={} path={} size={}x{}",
                    task, latestPath, image.getWidth(), image.getHeight());
            return new QuestDetailCapture(image, latestPath, rightRect[0], rightRect[1]);
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
            if (!InputActionScope.checkpoint()) {
                return null;
            }
            if (!pressBackgroundAltQ("ensure-panel-direct")) {
                return null;
            }
            if (!TaskSleep.sleep(SLOW) || !InputActionScope.checkpoint()) {
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
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(tab.x, tab.y, 100);
        return TaskSleep.sleep(FAST);
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
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(p.x, p.y, 100);
        return TaskSleep.sleep(delay) && InputActionScope.checkpoint();
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
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.moveMouse(h.x, h.y);
        if (!TaskSleep.sleep(FAST) || !InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.scrollDown(steps);
        return TaskSleep.sleep(MID) && InputActionScope.checkpoint();
    }

    private void closePanel(String description) {
        inputSequences.submitAndWait(description, List.of(InputAction.pressAltQ()));
    }

    private void closePanelDirect() {
        if (!InputActionScope.checkpoint()) {
            return;
        }
        pressBackgroundAltQ("close-panel-direct");
    }

    private boolean pressBackgroundAltQ(String source) {
        var current = windowTaskContextHolder.rawCurrent();
        if (current.isEmpty() || current.get().getNativeBinding() == null) {
            log.warn("[quest] background Alt+Q rejected without an exact window binding: source={}", source);
            return false;
        }
        var context = current.get();
        var attempt = boundWindowKeyboardService.pressShortcut(
                context.getNativeBinding(), context.getWindowId(),
                BoundWindowKeyboardService.AltShortcut.ALT_Q);
        if (!attempt.attempted() || !attempt.success()) {
            log.warn("[quest] background Alt+Q failed: source={} windowId={} reason={}",
                    source, context.getWindowId(), attempt.reason());
            return false;
        }
        return true;
    }

    private Point findAnchor() { return coordinateHelper.findImageAbsoluteCoordinate(ANCHOR_PATH, THRESHOLD_NORMAL); }

}
