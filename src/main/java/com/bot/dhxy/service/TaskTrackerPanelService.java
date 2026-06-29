package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerFastMatchResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerTitleTemplate;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.vision.OcrWindowScanService;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reader for the left-side task tracker panel.
 *
 * <p>This service owns task-tracker panel screenshots and text-link detection. It never sends
 * physical input. Window watchers may call it in the background to prepare a click target, and task
 * code later decides whether to consume that prepared action through the serialized input queue.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskTrackerPanelService {

    private static final String TRACKER_ANCHOR_TEMPLATE = "images/template/task/wubei_tracker_anchor.png";
    private static final String WUHUAN_TRACKER_TITLE_TEMPLATE = "images/template/wuhuan/panel_title.png";
    private static final String XIULUO_TRACKER_TITLE_TEMPLATE = "images/template/task/xiuluo_tracker_title.png";
    private static final String XIULUO_TASK_KEY_TRACKER = "xiuluo.tracker";
    public static final String WUBEI_TASK_KEY_SANCANG_FENGMO = "wubei.sancang_fengmo";
    public static final String WUBEI_TASK_KEY_BAOXIANG_MIQING = "wubei.baoxiang_miqing";
    public static final String WUBEI_TASK_KEY_DIANQIAN_XIANYI = "wubei.dianqian_xianyi";
    public static final String WUBEI_TASK_KEY_ZHIDOU_HUANGPAO = "wubei.zhidou_huangpao";
    public static final String WUBEI_TASK_KEY_KUIXING_GUIWEI = "wubei.kuixing_guiwei";
    private static final int GAME_CLIENT_WIDTH = 1024;
    private static final int GAME_CLIENT_HEIGHT = 768;
    private static final int TRACKER_ANCHOR_SEARCH_REL_LEFT = 6;
    private static final int TRACKER_ANCHOR_SEARCH_REL_TOP = 196;
    private static final int TRACKER_ANCHOR_SEARCH_REL_RIGHT = 207;
    private static final int TRACKER_ANCHOR_SEARCH_REL_BOTTOM = 551;
    private static final int TRACKER_PANEL_FROM_ANCHOR_LEFT = -96;
    private static final int TRACKER_PANEL_FROM_ANCHOR_TOP = 12;
    private static final int TRACKER_PANEL_FROM_ANCHOR_RIGHT = 86;
    private static final int TRACKER_PANEL_FROM_ANCHOR_BOTTOM = 350;
    private static final int TRACKER_PANEL_ANCHOR_MAX_REL_X = 164;
    private static final int TRACKER_PANEL_ANCHOR_MAX_REL_Y = 353;
    private static final int TRACKER_PANEL_DRAG_TARGET_REL_X = 104;
    private static final int TRACKER_PANEL_DRAG_TARGET_REL_Y = 221;
    private static final int TASK_DETAIL_LEFT_PADDING = 5;
    private static final int TASK_DETAIL_WIDTH = 175;
    private static final int WUHUAN_TRACKER_BLOCK_HEIGHT = 65;
    private static final int XIULUO_TRACKER_BLOCK_HEIGHT = 40;
    private static final int WUHUAN_TITLE_CENTER_FALLBACK_LEFT_SHIFT = 24;
    private static final int TRACKER_LINK_MIN_PIXELS = 20;
    private static final int TRACKER_LINK_SPLIT_GAP = 8;
    private static final int TRACKER_LINK_DELIMITER_MAX_WIDTH = 5;
    private static final int TRACKER_LINK_DELIMITER_MAX_PIXELS = 18;
    private static final int TRACKER_COORD_GLYPH_MAX_WIDTH = 5;
    private static final int TRACKER_COORD_GLYPH_MIN_RUN = 5;
    private static final double TRACKER_ANCHOR_THRESHOLD = 0.82;
    private static final int WUBEI_TRACKER_LINK_SINGLE_MAX_WIDTH = 72;
    private static final int WUBEI_CHAINED_FAST_FINGERPRINT_MAX_DISTANCE = 8;
    private static final List<TaskTrackerTitleTemplate> WUBEI_TRACKER_TITLE_TEMPLATES = List.of(
        trackerTitleTemplate(WUBEI_TASK_KEY_DIANQIAN_XIANYI, "殿前献艺", "images/template/wubei/wubei_title_dianqian_xianyi_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_SANCANG_FENGMO, "三藏封魔", "images/template/wubei/wubei_title_sancang_fengmo_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_BAOXIANG_MIQING, "宝象谜情", "images/template/wubei/wubei_title_baoxiang_miqing_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_ZHIDOU_HUANGPAO, "智斗黄袍", "images/template/wubei/wubei_title_zhidou_huangpao_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_KUIXING_GUIWEI, "魁星归位", "images/template/wubei/wubei_title_kuixing_guiwei_yellow.png")
    );
    private static final TaskTrackerTitleTemplate XIULUO_TRACKER_TITLE = trackerTitleTemplate(
        XIULUO_TASK_KEY_TRACKER, "修罗任务", XIULUO_TRACKER_TITLE_TEMPLATE);

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;
    private final InputSequences inputSequences;
    private final MapNameCanonicalizer mapNameCanonicalizer;

    private static TaskTrackerTitleTemplate trackerTitleTemplate(String taskKey, String displayName, String templatePath) {
        return TaskTrackerTitleTemplate.builder()
            .taskKey(taskKey)
            .displayName(displayName)
            .templatePath(templatePath)
            .threshold(0.82)
            .build();
    }

    private static int taskDetailBlockHeight(TaskTrackerTitleTemplate titleTemplate) {
        if (titleTemplate != null
                && (XIULUO_TASK_KEY_TRACKER.equals(titleTemplate.getTaskKey())
                || XIULUO_TRACKER_TITLE_TEMPLATE.equals(titleTemplate.getTemplatePath()))) {
            return XIULUO_TRACKER_BLOCK_HEIGHT;
        }
        return WUHUAN_TRACKER_BLOCK_HEIGHT;
    }

    public Point findWuhuanNextGreenClickPoint() {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel("wuhuan", WUHUAN_TRACKER_TITLE_TEMPLATE);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(new File(crop.path()));
            if (image == null) {
                log.warn("[task-tracker wuhuan] detail image unreadable: path={}", crop.path());
                return null;
            }
            return findWuhuanTrackerGreenClickPoint(image, crop.absoluteLeft(), crop.absoluteTop());
        } catch (IOException e) {
            log.warn("[task-tracker wuhuan] failed to read detail image: path={}", crop.path(), e);
            return null;
        }
    }

    /**
     * Prepares the current 五环 tracker green-link click for the window watcher.
     *
     * <p>The returned action uses screen-absolute coordinates and a small green-text fingerprint
     * around the click point. The watcher validates that fingerprint before task code consumes the
     * cached click, which avoids rerunning the full tracker scan every time this window gets a turn.</p>
     *
     * @param source log/temp-file source tag; nullable and sanitized before being used in paths.
     * @return prepared task-tracker pathing action, or empty when the 五环 tracker title/link is not visible.
     */
    public Optional<PreparedDialogAction> prepareWuhuanPathingLink(String source) {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, WUHUAN_TRACKER_TITLE_TEMPLATE);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return Optional.empty();
        }

        try {
            BufferedImage image = ImageIO.read(new File(crop.path()));
            if (image == null) {
                log.warn("[task-tracker wuhuan] detail image unreadable for prepare: source={} path={}",
                        source, crop.path());
                return Optional.empty();
            }
            Point click = findWuhuanTrackerGreenClickPoint(image, crop.absoluteLeft(), crop.absoluteTop());
            if (click == null) {
                return Optional.empty();
            }
            return buildTaskTrackerPreparedAction(source, "wuhuan", crop, image, click);
        } catch (IOException e) {
            log.warn("[task-tracker wuhuan] failed to prepare tracker action: source={} path={}",
                    source, crop.path(), e);
            return Optional.empty();
        }
    }

    /**
     * Reads the current 五倍 task block from the left tracker panel.
     *
     * <p>五倍 has several possible yellow titles. This method captures the tracker panel once,
     * washes yellow once, matches any known title, then reads yellow text and green clickable
     * segments from the same cropped block. Returned green links are screen-absolute pixels; the
     * caller still decides whether/when to click and how to yield the task turn.</p>
     *
     * @param source log/temp-file source tag; nullable and sanitized before being used in paths.
     * @return read result; {@code found=false} when no known 五倍 title is visible.
     */
    public TaskTrackerPanelReadResult readWubeiTrackerPanel(String source) {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, WUBEI_TRACKER_TITLE_TEMPLATES, false);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return TaskTrackerPanelReadResult.empty();
        }
        return readWubeiTrackerDetail(crop.path(), crop.absoluteLeft(), crop.absoluteTop(),
                crop.titleTemplate(), source);
    }

    /**
     * Reads 五倍 tracker evidence from an accept-time full game-window snapshot.
     *
     * @param windowSnapshotPath saved full game-window snapshot path.
     * @param absoluteLeft screen-absolute X coordinate of the snapshot left edge.
     * @param absoluteTop screen-absolute Y coordinate of the snapshot top edge.
     * @param source diagnostic source tag.
     * @return read result with the same 五倍 title/link algorithm as live tracker reads.
     */
    public TaskTrackerPanelReadResult readWubeiTrackerPanelFromSnapshot(Path windowSnapshotPath,
                                                                        int absoluteLeft,
                                                                        int absoluteTop,
                                                                        String source) {
        if (windowSnapshotPath == null || !Files.isRegularFile(windowSnapshotPath)) {
            return TaskTrackerPanelReadResult.empty();
        }
        TitlePointMatch title = findTitlePointInPanelImage(source, windowSnapshotPath.toString(),
                windowSnapshotPath.toString(), absoluteLeft, absoluteTop, WUBEI_TRACKER_TITLE_TEMPLATES);
        if (title == null) {
            log.info("[task-tracker wubei] snapshot title miss: source={} snapshot={}",
                    source, windowSnapshotPath);
            return TaskTrackerPanelReadResult.empty();
        }
        TaskDetailCrop crop = cropTaskDetailFromTitlePoint(source, title);
        return readWubeiTrackerDetail(crop.path(), crop.absoluteLeft(), crop.absoluteTop(),
                crop.titleTemplate(), source);
    }

    private TaskTrackerPanelReadResult readWubeiTrackerDetail(String detailPath,
                                                              int absoluteLeft,
                                                              int absoluteTop,
                                                              TaskTrackerTitleTemplate titleTemplate,
                                                              String source) {
        String safeSource = source == null ? "wubei" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path detail = Path.of(detailPath);
        String yellowPath = detail.resolveSibling(detail.getFileName() + "." + safeSource + ".wubei-detail-yellow.png")
                .toString();
        ImagePreprocessor.washYellowText(detailPath, yellowPath);
        List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
            yellowPath,
            "wubei-tracker-yellow:" + safeSource,
            result -> !result.isEmpty());
        String yellowText = words.stream().map(OcrWordResult::getText).collect(java.util.stream.Collectors.joining("|"));

        try {
            BufferedImage image = ImageIO.read(new File(detailPath));
            if (image == null) {
                log.warn("[task-tracker wubei] detail image unreadable: source={} path={}", source, detailPath);
                return TaskTrackerPanelReadResult.empty();
            }
            WubeiGreenLinkScan scan = scanWubeiTrackerGreenLinks(
                image, absoluteLeft, absoluteTop, safeSource, titleTemplate);
            log.info("[task-tracker wubei] panel read: source={} taskKey={} title={} yellow='{}' probe={} links={} detail={} yellowPath={}",
                source,
                titleTemplate == null ? null : titleTemplate.getTaskKey(),
                titleTemplate == null ? null : titleTemplate.getDisplayName(),
                yellowText, scan.isProbeObjective(), scan.links(), detailPath, yellowPath);
            return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(titleTemplate)
                .detailRawPath(detailPath)
                .detailYellowPath(yellowPath)
                .detailAbsoluteLeft(absoluteLeft)
                .detailAbsoluteTop(absoluteTop)
                .yellowText(yellowText)
                .greenLinks(scan.links())
                .greenBandWidth(scan.bandWidth())
                .probeObjective(scan.isProbeObjective())
                .build();
        } catch (IOException e) {
            log.warn("[task-tracker wubei] failed to read detail image: source={} path={}", source, detailPath, e);
            return TaskTrackerPanelReadResult.empty();
        }
    }

    /**
     * Reads the current 修罗 shortcut task block from the left tracker panel without clicking it.
     *
     * <p>CR81 only exposes evidence and a screen-absolute first-green-link coordinate. This method
     * must stay read-only: it does not register pathing intent, send input, or advance 修罗 phases.</p>
     *
     * @param source log/temp-file source tag; nullable and sanitized before being used in paths.
     * @return read result; {@code found=false} when the 修罗 tracker title or green link is absent.
     */
    public TaskTrackerPanelReadResult readXiuluoTrackerPanel(String source) {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, List.of(XIULUO_TRACKER_TITLE));
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return TaskTrackerPanelReadResult.empty();
        }
        return readXiuluoTrackerDetail(crop.path(), crop.absoluteLeft(), crop.absoluteTop(),
            crop.titleTemplate(), source, null);
    }

    /**
     * Returns the screen-absolute point that 修罗 shortcut mode would click on the first tracker link.
     *
     * <p>This remains a read-only CR81 helper: it only captures/recognizes the left tracker panel and
     * returns a coordinate. It does not click, register pathing intent, or mutate 修罗 task phases.</p>
     *
     * @param source log/temp-file source tag; nullable and sanitized before being used in paths.
     * @return first 修罗 tracker green-link click point in screen-absolute pixels, or empty on miss.
     */
    public Optional<Point> findXiuluoTrackerGreenClickPoint(String source) {
        return resolveXiuluoTrackerGreenClickPoint(readXiuluoTrackerPanel(source));
    }

    /**
     * Reads 修罗 tracker evidence from an accept-time full game-window snapshot.
     *
     * @param windowSnapshotPath saved full game-window snapshot path. The image is expected to use
     *                           normal window-local pixels, with {@code absoluteLeft/Top} supplied
     *                           separately for converting green links back to screen coordinates.
     * @param absoluteLeft screen-absolute X coordinate of the snapshot left edge.
     * @param absoluteTop screen-absolute Y coordinate of the snapshot top edge.
     * @param source diagnostic source tag.
     * @return read result with the same title/link algorithm as live 修罗 tracker reads.
     */
    public TaskTrackerPanelReadResult readXiuluoTrackerPanelFromSnapshot(Path windowSnapshotPath,
                                                                         int absoluteLeft,
                                                                         int absoluteTop,
                                                                         String source) {
        return readXiuluoTrackerPanelForReplay(windowSnapshotPath, absoluteLeft, absoluteTop, source, null);
    }

    /**
     * Resolves the click point from an already-read 修罗 tracker panel.
     *
     * @param panel read-only tracker panel result from live capture or replay.
     * @return first green-link click point in the same coordinate space as {@code panel}'s links.
     */
    public Optional<Point> resolveXiuluoTrackerGreenClickPoint(TaskTrackerPanelReadResult panel) {
        if (panel == null || !panel.isFound() || panel.getGreenLinks().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(resolveWubeiTrackerGreenClickPoint(panel.getGreenLinks().get(0)));
    }

    /**
     * Replays the 修罗 tracker reader against a saved panel/detail image.
     *
     * <p>This is for testcase/debug verification only. Coordinates in the result are based on the
     * supplied absolute origin; passing {@code 0,0} makes them image-local.</p>
     *
     * @param panelRawPath saved raw tracker panel or already-cropped detail block.
     * @param absoluteLeft screen-absolute X coordinate to add to detected green segments.
     * @param absoluteTop screen-absolute Y coordinate to add to detected green segments.
     * @param source diagnostic source tag.
     * @param markedOutputPath optional PNG path for a marked title/link/click evidence image.
     * @return read result with title evidence and first green link, or empty on miss.
     */
    public TaskTrackerPanelReadResult readXiuluoTrackerPanelForReplay(Path panelRawPath,
                                                                      int absoluteLeft,
                                                                      int absoluteTop,
                                                                      String source,
                                                                      Path markedOutputPath) {
        if (panelRawPath == null || !Files.isRegularFile(panelRawPath)) {
            return TaskTrackerPanelReadResult.empty();
        }
        String safeSource = safeSource(source);
        Path yellowPath = panelRawPath.resolveSibling(panelRawPath.getFileName()
            + "." + safeSource + ".xiuluo-title-yellow.png");
        ImagePreprocessor.washYellowText(panelRawPath.toString(), yellowPath.toString());
        TitlePointMatch title = findTitlePointInPanelImage(source, panelRawPath.toString(), yellowPath.toString(),
            absoluteLeft, absoluteTop, List.of(XIULUO_TRACKER_TITLE));
        if (title == null) {
            log.info("[task-tracker xiuluo] replay title miss: source={} panel={} yellow={}",
                source, panelRawPath, yellowPath);
            return TaskTrackerPanelReadResult.empty();
        }
        TaskDetailCrop crop = cropTaskDetailFromTitlePoint(source, title);
        TaskTrackerPanelReadResult result = readXiuluoTrackerDetail(crop.path(), crop.absoluteLeft(),
            crop.absoluteTop(), crop.titleTemplate(), source, markedOutputPath);
        if (result.isFound()) {
            log.info("[task-tracker xiuluo] replay read: source={} title={} panel={} detail={} links={} marked={}",
                source, title.titleTemplate().getDisplayName(), panelRawPath, crop.path(),
                result.getGreenLinks(), markedOutputPath);
        }
        return result;
    }

    public String getCroppedTaskDetailInTrackerPanel(String source, String template) {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, template);
        return crop == null ? null : crop.path();
    }

    /**
     * Build a reusable small-area cache for 黄袍续战 from a full tracker read.
     *
     * <p>The returned object reuses {@link PreparedDialogAction}'s click/fingerprint fields for a
     * tracker-green link, not a dialog. Callers should only use it after business logic has already
     * confirmed 黄袍 from the full tracker panel.</p>
     *
     * @param panel full 五倍 tracker result, including raw detail image and screen-absolute origin.
     * @param source diagnostic source tag.
     * @return cached first-green-link click/fingerprint, or empty when no green link can be cached.
     */
    public Optional<PreparedDialogAction> prepareWubeiChainedTrackerFastAction(TaskTrackerPanelReadResult panel,
                                                                               String source) {
        if (panel == null || !panel.isFound() || panel.getGreenLinks().isEmpty()
                || panel.getDetailRawPath() == null || panel.getDetailRawPath().isBlank()) {
            return Optional.empty();
        }
        try {
            BufferedImage image = ImageIO.read(new File(panel.getDetailRawPath()));
            if (image == null) {
                log.warn("[task-tracker wubei] chained fast cache skipped: source={} reason=detail-unreadable path={}",
                        source, panel.getDetailRawPath());
                return Optional.empty();
            }
            TaskTrackerGreenLink link = panel.getGreenLinks().get(0);
            TaskDetailCrop crop = new TaskDetailCrop(
                    panel.getDetailRawPath(),
                    panel.getDetailAbsoluteLeft(),
                    panel.getDetailAbsoluteTop(),
                    panel.getTitleTemplate());
            Point click = resolveWubeiTrackerGreenClickPoint(link);
            Optional<PreparedDialogAction> action = buildTaskTrackerPreparedAction(
                    source, "wubei-chained", crop, image, click);
            action.ifPresent(prepared -> log.info("[task-tracker wubei] chained fast cache prepared: "
                            + "source={} rect=({}, {})-({}, {}) click=({}, {}) detail={}",
                    source,
                    prepared.getValidationLeft(), prepared.getValidationTop(),
                    prepared.getValidationRight(), prepared.getValidationBottom(),
                    prepared.getAbsoluteX(), prepared.getAbsoluteY(), prepared.getDebugImagePath()));
            return action;
        } catch (IOException e) {
            log.warn("[task-tracker wubei] chained fast cache failed: source={} path={}",
                    source, panel.getDetailRawPath(), e);
            return Optional.empty();
        }
    }

    /**
     * Verify the cached 黄袍续战 tracker-green area with one small screenshot.
     *
     * @param cachedAction cache returned by {@link #prepareWubeiChainedTrackerFastAction}.
     * @param source diagnostic source tag.
     * @param writeMarkedImage true when a replay/debug output image should be written.
     * @return fast match result. Miss/invalid results are terminal for CR54 callers and should not
     *         trigger a full tracker reread.
     */
    public TaskTrackerFastMatchResult verifyWubeiChainedTrackerFastAction(PreparedDialogAction cachedAction,
                                                                          String source,
                                                                          boolean writeMarkedImage) {
        long startedAt = System.currentTimeMillis();
        if (cachedAction == null || cachedAction.getFingerprint() == null || cachedAction.getFingerprint().isBlank()) {
            return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "missing-cache");
        }
        int left = cachedAction.getValidationLeft();
        int top = cachedAction.getValidationTop();
        int right = cachedAction.getValidationRight();
        int bottom = cachedAction.getValidationBottom();
        if (right <= left || bottom <= top) {
            return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "invalid-rect");
        }

        BufferedImage raw = null;
        BufferedImage washed = null;
        try {
            raw = tracker.captureToMemory("task-tracker-chained-fast:" + safeSource(source), left, top, right, bottom);
            if (raw == null) {
                return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "capture-failed");
            }
            washed = ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
            String currentFingerprint = ImagePreprocessor.buildBinaryFingerprint(washed);
            int distance = ImagePreprocessor.binaryFingerprintDistance(cachedAction.getFingerprint(), currentFingerprint);
            boolean matched = distance <= WUBEI_CHAINED_FAST_FINGERPRINT_MAX_DISTANCE;
            String markedPath = writeMarkedImage
                    ? writeChainedFastMarkedImage(raw, cachedAction, source, matched, distance)
                    : null;
            TaskTrackerFastMatchResult result = chainedFastResult(matched, distance, startedAt, markedPath,
                    matched ? "hit" : "fingerprint-miss");
            log.info("[task-tracker wubei] chained fast verify: source={} matched={} distance={} maxDistance={} "
                            + "score={} elapsedMs={} rect=({}, {})-({}, {}) click=({}, {}) marked={}",
                    source, result.isMatched(), result.getDistance(), result.getMaxDistance(),
                    result.getScore(), result.getElapsedMs(), left, top, right, bottom,
                    cachedAction.getAbsoluteX(), cachedAction.getAbsoluteY(), markedPath);
            return result;
        } catch (RuntimeException e) {
            log.warn("[task-tracker wubei] chained fast verify failed: source={} rect=({}, {})-({}, {})",
                    source, left, top, right, bottom, e);
            return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "exception");
        } finally {
            if (raw != null) {
                raw.flush();
            }
            if (washed != null && washed != raw) {
                washed.flush();
            }
        }
    }

    /**
     * Scan saved 五倍 tracker panel images for green links and parse each link's target map name.
     *
     * <p>This replay/debug API is intentionally screenshot-only. It does not capture windows, send
     * input, register dialog interest, or mutate task state. Coordinates in the returned links are
     * relative to the supplied {@code absoluteLeft}/{@code absoluteTop} origin.</p>
     *
     * @param frame saved tracker panel or task-detail image in normal image-local pixels.
     * @param absoluteLeft screen-absolute X origin to add to detected link rectangles; use 0 for
     *                     offline image-local replay.
     * @param absoluteTop screen-absolute Y origin to add to detected link rectangles; use 0 for
     *                    offline image-local replay.
     * @param source diagnostic source used in OCR temp filenames/logs.
     * @return green tracker link segments, each with parsed target map name when recognized.
     */
    public List<TaskTrackerGreenLink> scanWubeiTrackerGreenLinksForReplay(BufferedImage frame,
                                                                          int absoluteLeft,
                                                                          int absoluteTop,
                                                                          String source) {
        return scanWubeiTrackerGreenLinks(frame, absoluteLeft, absoluteTop, source, null).links();
    }

    /**
     * Replay variant that applies the same yellow-text business boundary as production reads.
     *
     * @param frame saved tracker panel or task-detail image in normal image-local pixels.
     * @param absoluteLeft screen-absolute X origin to add to detected link rectangles.
     * @param absoluteTop screen-absolute Y origin to add to detected link rectangles.
     * @param source diagnostic source used in OCR temp filenames/logs.
     * @param yellowText already-read 五倍 yellow tracker text for this panel.
     * @return green tracker link segments; map name is populated only for links that can feed the
     *         ordinary/黄袍 map-match interest rule.
     */
    public List<TaskTrackerGreenLink> scanWubeiTrackerGreenLinksForReplay(BufferedImage frame,
                                                                          int absoluteLeft,
                                                                          int absoluteTop,
                                                                          String source,
                                                                          String yellowText) {
        return scanWubeiTrackerGreenLinks(frame, absoluteLeft, absoluteTop, source, null).links();
    }

    private TaskTrackerPanelReadResult readXiuluoTrackerDetail(String detailPath,
                                                               int absoluteLeft,
                                                               int absoluteTop,
                                                               TaskTrackerTitleTemplate titleTemplate,
                                                               String source,
                                                               Path markedOutputPath) {
        try {
            BufferedImage detail = ImageIO.read(new File(detailPath));
            if (detail == null) {
                log.warn("[task-tracker xiuluo] detail unreadable: source={} path={}", source, detailPath);
                return TaskTrackerPanelReadResult.empty();
            }
            XiuluoGreenLinkScan scan = scanXiuluoTrackerGreenLinks(detail, absoluteLeft, absoluteTop, source);
            String markedPath = writeXiuluoTrackerMarkedImage(detail, absoluteLeft, absoluteTop, titleTemplate,
                scan.links(), markedOutputPath, source);
            boolean found = !scan.links().isEmpty();
            log.info("[task-tracker xiuluo] detail read: source={} found={} title={} links={} detail={} marked={}",
                source, found, titleTemplate.getDisplayName(), scan.links(), detailPath, markedPath);
            if (!found) {
                return TaskTrackerPanelReadResult.empty();
            }
            return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(titleTemplate)
                .detailRawPath(detailPath)
                .detailAbsoluteLeft(absoluteLeft)
                .detailAbsoluteTop(absoluteTop)
                .greenLinks(scan.links())
                .greenBandWidth(scan.bandWidth())
                .yellowText("")
                .probeObjective(false)
                .build();
        } catch (IOException e) {
            log.warn("[task-tracker xiuluo] detail read failed: source={} path={}", source, detailPath, e);
            return TaskTrackerPanelReadResult.empty();
        }
    }

    private TaskDetailCrop cropTaskDetailInTrackerPanel(String source, String template) {
        return cropTaskDetailInTrackerPanel(source, List.of(
            TaskTrackerTitleTemplate.builder()
                .taskKey(source)
                .displayName(source)
                .templatePath(template)
                .threshold(0.82)
                .build()));
    }

    private TaskDetailCrop cropTaskDetailInTrackerPanel(String source, List<TaskTrackerTitleTemplate> templates) {
        return cropTaskDetailInTrackerPanel(source, templates, true);
    }

    private TaskDetailCrop cropTaskDetailInTrackerPanel(String source,
                                                        List<TaskTrackerTitleTemplate> templates,
                                                        boolean washTitleSource) {
        TitlePointMatch titleMatch = findTitlePoint(source, templates, washTitleSource);
        if (titleMatch == null || titleMatch.panelRawPath() == null || titleMatch.panelRawPath().isBlank()) {
            return null;
        }

        try {
            BufferedImage panelImage = ImageIO.read(new File(titleMatch.panelRawPath()));
            if (panelImage == null) {
                log.warn("[task-tracker] panel image unreadable for detail crop: source={} panel={}", source, titleMatch.panelRawPath());
                return null;
            }

            Point titlePoint = titleMatch.titlePoint();
            int left = Math.max(0, titlePoint.x - TASK_DETAIL_LEFT_PADDING);
            int top = Math.max(0, titlePoint.y);
            int width = Math.min(TASK_DETAIL_WIDTH, panelImage.getWidth() - left);
            int height = Math.min(taskDetailBlockHeight(titleMatch.titleTemplate()), panelImage.getHeight() - top);
            if (width <= 0 || height <= 0) {
                log.warn("[task-tracker] detail crop outside panel image: source={} panel={} titlePoint=({}, {}) imageSize={}x{}",
                    source, titleMatch.panelRawPath(), titlePoint.x, titlePoint.y, panelImage.getWidth(), panelImage.getHeight());
                return null;
            }

            BufferedImage detailImage = panelImage.getSubimage(left, top, width, height);
            String safeSource = source == null ? "unknown" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
            String detailPath = windowScopedTempPath.resolve("task_tracker_detail_crop_" + safeSource + ".png");
            ImageIO.write(detailImage, "png", new File(detailPath));
            log.info("[task-tracker] detail cropped: source={} panel={} titlePoint=({}, {}) crop=({}, {}) {}x{} path={}",
                source, titleMatch.panelRawPath(), titlePoint.x, titlePoint.y, left, top, width, height, detailPath);
            return new TaskDetailCrop(
                detailPath,
                titleMatch.panelAbsoluteLeft() + left,
                titleMatch.panelAbsoluteTop() + top,
                titleMatch.titleTemplate()
            );
        } catch (IOException e) {
            log.warn("[task-tracker] failed to crop task detail: source={} panel={} titlePoint={}",
                source, titleMatch.panelRawPath(), titleMatch.titlePoint(), e);
            return null;
        }
    }

    private TaskDetailCrop cropTaskDetailFromTitlePoint(String source, TitlePointMatch titleMatch) {
        if (titleMatch == null || titleMatch.panelRawPath() == null || titleMatch.panelRawPath().isBlank()) {
            return null;
        }

        try {
            BufferedImage panelImage = ImageIO.read(new File(titleMatch.panelRawPath()));
            if (panelImage == null) {
                log.warn("[task-tracker] panel image unreadable for replay detail crop: source={} panel={}",
                    source, titleMatch.panelRawPath());
                return null;
            }

            Point titlePoint = titleMatch.titlePoint();
            int left = Math.max(0, titlePoint.x - TASK_DETAIL_LEFT_PADDING);
            int top = Math.max(0, titlePoint.y);
            int width = Math.min(TASK_DETAIL_WIDTH, panelImage.getWidth() - left);
            int height = Math.min(taskDetailBlockHeight(titleMatch.titleTemplate()), panelImage.getHeight() - top);
            if (width <= 0 || height <= 0) {
                log.warn("[task-tracker] replay detail crop outside panel image: source={} panel={} titlePoint=({}, {}) imageSize={}x{}",
                    source, titleMatch.panelRawPath(), titlePoint.x, titlePoint.y,
                    panelImage.getWidth(), panelImage.getHeight());
                return null;
            }

            BufferedImage detailImage = ImagePreprocessor.cropCopy(panelImage, left, top, width, height);
            if (detailImage == null) {
                return null;
            }
            Path panelPath = Path.of(titleMatch.panelRawPath());
            Path output = panelPath.resolveSibling(panelPath.getFileName() + "." + safeSource(source)
                + ".task-detail.png");
            ImageIO.write(detailImage, "png", output.toFile());
            log.info("[task-tracker] replay detail cropped: source={} panel={} titlePoint=({}, {}) crop=({}, {}) {}x{} path={}",
                source, titleMatch.panelRawPath(), titlePoint.x, titlePoint.y, left, top, width, height, output);
            return new TaskDetailCrop(output.toString(), titleMatch.panelAbsoluteLeft() + left,
                titleMatch.panelAbsoluteTop() + top, titleMatch.titleTemplate());
        } catch (IOException e) {
            log.warn("[task-tracker] failed to crop replay task detail: source={} panel={} titlePoint={}",
                source, titleMatch.panelRawPath(), titleMatch.titlePoint(), e);
            return null;
        }
    }

    private TitlePointMatch findTitlePoint(String source, String template) {
        return findTitlePoint(source, List.of(
            TaskTrackerTitleTemplate.builder()
                .taskKey(source)
                .displayName(source)
                .templatePath(template)
                .threshold(0.82)
                .build()));
    }

    private TitlePointMatch findTitlePoint(String source, List<TaskTrackerTitleTemplate> templates) {
        return findTitlePoint(source, templates, true);
    }

    private TitlePointMatch findTitlePoint(String source,
                                           List<TaskTrackerTitleTemplate> templates,
                                           boolean washTitleSource) {
        TrackerPanelCapture panel = resolveTrackerPanelRect(source);
        if (panel == null || panel.rawPath() == null || templates == null || templates.isEmpty()) {
            return null;
        }

        String matchPath = panel.rawPath();
        if (washTitleSource) {
            String safeSource = source == null ? "unknown" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
            matchPath = windowScopedTempPath.resolve("task_tracker_title_yellow_" + safeSource + ".png");
            ImagePreprocessor.washYellowText(panel.rawPath(), matchPath);
        }

        return findTitlePointInPanelImage(source, panel.rawPath(), matchPath, panel.absoluteLeft(),
            panel.absoluteTop(), templates);
    }

    private TitlePointMatch findTitlePointInPanelImage(String source,
                                                       String rawPath,
                                                       String matchPath,
                                                       int absoluteLeft,
                                                       int absoluteTop,
                                                       List<TaskTrackerTitleTemplate> templates) {
        for (TaskTrackerTitleTemplate template : templates) {
            if (template == null || template.getTemplatePath() == null || template.getTemplatePath().isBlank()) {
                continue;
            }
            double[] match = ImageFinder.find(matchPath, template.getTemplatePath(), template.getThreshold());
            if (match == null || match.length < 2) {
                log.info("[task-tracker] title template not matched: source={} template={} panel={} matchSource={}",
                    source, template.getTemplatePath(), rawPath, matchPath);
                continue;
            }

            try {
            BufferedImage templateImage = ImageIO.read(new File(template.getTemplatePath()));
            if (templateImage == null) {
                log.warn("[task-tracker] title template image unreadable: source={} template={}", source, template.getTemplatePath());
                continue;
            }

            // ImageFinder returns the template center. The tracker title anchor we need is
            // image-local top-left, so later code can crop the task block from that point.
            int left = (int) Math.round(match[0] - templateImage.getWidth() / 2.0);
            int top = (int) Math.round(match[1] - templateImage.getHeight() / 2.0);
            double score = match.length >= 3 ? match[2] : 0.0;
            log.info("[task-tracker] title template matched: source={} template={} display={} score={} imageLocalTopLeft=({}, {}) center=({}, {})",
                source, template.getTemplatePath(), template.getDisplayName(), score, left, top, match[0], match[1]);
            return new TitlePointMatch(rawPath, matchPath, absoluteLeft, absoluteTop,
                new Point(left, top), score, template);
            } catch (IOException e) {
                log.warn("[task-tracker] failed to read title template: source={} template={}", source, template.getTemplatePath(), e);
            }
        }
        return null;
    }

    private TrackerPanelCapture resolveTrackerPanelRect(String source) {
        tracker.refreshWindowState();
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        int[] searchRect = new int[]{
            baseX + TRACKER_ANCHOR_SEARCH_REL_LEFT,
            baseY + TRACKER_ANCHOR_SEARCH_REL_TOP,
            baseX + TRACKER_ANCHOR_SEARCH_REL_RIGHT,
            baseY + TRACKER_ANCHOR_SEARCH_REL_BOTTOM
        };
        String searchMode = "narrow-default";
        Point anchor = coordinateHelper.findImageInRegion(TRACKER_ANCHOR_TEMPLATE, searchRect, 0.82);
        if (anchor == null) {
            log.warn("[wubei] tracker anchor not found in narrow area: source={} searchRect=({}, {})-({}, {})",
                source,
                searchRect[0], searchRect[1], searchRect[2], searchRect[3]);

            log.warn("keep searching in expanded area");
            if (tracker.updateGlobalVision()) {
                String path = tracker.getLatestVisionPath();
                double[] result = ImageFinder.find(path, TRACKER_ANCHOR_TEMPLATE, 0.82);
                if (result != null && result.length >= 2) {
                    Point localAnchor = new Point((int) Math.round(result[0]), (int) Math.round(result[1]));
                    anchor = expandedVisionAnchorToScreenAnchor(localAnchor, baseX, baseY);
                    searchMode = "expanded";
                    log.info("[wubei] tracker anchor found in expanded area: source={} localAnchor=({}, {}) base=({}, {}) screenAnchor=({}, {})",
                            source, localAnchor.x, localAnchor.y, baseX, baseY, anchor.x, anchor.y);
                } else {
                    log.warn("[wubei] tracker anchor not found in expanded area (full game window): source={}",
                        source);
                    return null;
                }
            }
        }

        if (dragTrackerPanelIfNeeded(anchor, source)) {
            log.info("[wubei] tracker panel dragged to the fixed point");
        }

        int[] panelRect = new int[]{
            anchor.x + TRACKER_PANEL_FROM_ANCHOR_LEFT,
            anchor.y + TRACKER_PANEL_FROM_ANCHOR_TOP,
            anchor.x + TRACKER_PANEL_FROM_ANCHOR_RIGHT,
            anchor.y + TRACKER_PANEL_FROM_ANCHOR_BOTTOM
        };
        log.info("[wubei] tracker panel rect resolved by anchor: source={} mode={} anchor=({}, {}) rect=({}, {})-({}, {})",
            source, searchMode, anchor.x, anchor.y,
            panelRect[0], panelRect[1], panelRect[2], panelRect[3]);

        String safeSource = source == null ? "unknown" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
        String rawPath = windowScopedTempPath.resolve("task_tracker_detail_" + safeSource + ".png");
        if (!tracker.captureToFile("task-tracker-detail:" + safeSource, rawPath,
            panelRect[0], panelRect[1], panelRect[2], panelRect[3])) {
            log.warn("task tracker panel capture failed: source={} rect=({}, {})-({}, {}) path={}",
                source, panelRect[0], panelRect[1], panelRect[2], panelRect[3], rawPath);
            return null;
        }
        log.info("task tracker panel captured: source={} rect=({}, {})-({}, {}) path={}",
            source, panelRect[0], panelRect[1], panelRect[2], panelRect[3], rawPath);
        return new TrackerPanelCapture(rawPath, panelRect[0], panelRect[1]);
    }

    static Point expandedVisionAnchorToScreenAnchor(Point localAnchor, int baseX, int baseY) {
        if (localAnchor == null) {
            return null;
        }
        return new Point(baseX + localAnchor.x, baseY + localAnchor.y);
    }

    private Point findWuhuanTrackerGreenClickPoint(BufferedImage detailImage, int absoluteLeft, int absoluteTop) {
        TrackerGreenLinkScan scan = scanWuhuanTrackerGreenLinks(detailImage, absoluteLeft, absoluteTop);
        Optional<TrackerGreenLinkSegment> segment = findWuhuanPathingNameSegment(scan);
        return segment.map(this::resolveTrackerGreenClickPoint).orElse(null);
    }

    private WubeiGreenLinkScan scanWubeiTrackerGreenLinks(BufferedImage frame,
                                                          int absoluteLeft,
                                                          int absoluteTop,
                                                          String source,
                                                          TaskTrackerTitleTemplate titleTemplate) {
        List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(frame);
        ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, true);
        if (band == null) {
            log.info("[task-tracker wubei] green link scan: no green band");
            return WubeiGreenLinkScan.empty();
        }
        List<TrackerGreenLinkSegment> segments = splitWubeiTrackerGreenLinkSegments(frame, band, absoluteLeft, absoluteTop);
        int bandWidth = band.maxX() - band.minX() + 1;
        boolean probe = segments.size() >= 2
            || (segments.size() == 1 && bandWidth > WUBEI_TRACKER_LINK_SINGLE_MAX_WIDTH);
        String taskKey = titleTemplate == null ? "" : titleTemplate.getTaskKey();
        boolean darkThunder = isWubeiDarkThunderTaskKey(taskKey);
        boolean mirrorProbe = isWubeiMirrorProbeTaskKey(taskKey);
        List<TaskTrackerGreenLink> links = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            TrackerGreenLinkSegment segment = segments.get(i);
            WubeiGreenMapText mapText = shouldParseWubeiTargetMap(darkThunder, mirrorProbe, i)
                ? recognizeWubeiGreenMapText(frame, segment, absoluteLeft, absoluteTop, source, i)
                : WubeiGreenMapText.empty();
            links.add(TaskTrackerGreenLink.builder()
                .minX(segment.minX())
                .minY(segment.minY())
                .maxX(segment.maxX())
                .maxY(segment.maxY())
                .pixels(segment.pixels())
                .targetMapName(mapText.targetMapName())
                .targetMapScore(mapText.score())
                .targetMapDebugPath(mapText.debugPath())
                .build());
        }
        log.info("[task-tracker wubei] green link scan: taskKey={} bands={} band=({}, {})-({}, {}) width={} links={} probe={} darkThunder={} mirrorProbe={}",
            taskKey,
            bands.size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
            absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, links, probe, darkThunder, mirrorProbe);
        return new WubeiGreenLinkScan(links, bandWidth, probe);
    }

    private boolean shouldParseWubeiTargetMap(boolean darkThunder, boolean mirrorProbe, int linkIndex) {
        if (darkThunder) {
            return false;
        }
        return !mirrorProbe || linkIndex == 0;
    }

    private boolean isWubeiDarkThunderTaskKey(String taskKey) {
        return WUBEI_TASK_KEY_DIANQIAN_XIANYI.equals(taskKey);
    }

    private boolean isWubeiMirrorProbeTaskKey(String taskKey) {
        return WUBEI_TASK_KEY_BAOXIANG_MIQING.equals(taskKey);
    }

    private WubeiGreenMapText recognizeWubeiGreenMapText(BufferedImage frame,
                                                         TrackerGreenLinkSegment segment,
                                                         int absoluteLeft,
                                                         int absoluteTop,
                                                         String source,
                                                         int index) {
        if (frame == null || segment == null || textRecognizer == null) {
            return WubeiGreenMapText.empty();
        }

        BufferedImage ocrImage = buildWubeiGreenMapOcrImage(frame, segment, absoluteLeft, absoluteTop);
        if (ocrImage == null) {
            return WubeiGreenMapText.empty();
        }

        Path ocrPath = resolveWubeiGreenMapOcrPath(source, index);
        try {
            Files.createDirectories(ocrPath.toAbsolutePath().getParent());
            ImageIO.write(ocrImage, "png", ocrPath.toFile());
            List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
                ocrPath.toString(),
                "wubei-tracker-green-map:" + safeSource(source),
                result -> !joinOcrWords(result).isBlank());
            String rawText = normalizeWubeiGreenMapText(joinOcrWords(words));
            if (rawText.isBlank()) {
                log.info("[task-tracker wubei] green map OCR empty: source={} index={} path={}",
                    source, index, ocrPath);
                return new WubeiGreenMapText("", 0.0, ocrPath.toString());
            }
            String canonical = mapNameCanonicalizer == null
                ? rawText
                : mapNameCanonicalizer.canonicalize(rawText, "wubei-tracker-green-map:" + safeSource(source));
            double score = words.stream()
                .mapToDouble(OcrWordResult::getScore)
                .max()
                .orElse(0.0);
            if (score <= 0.0) {
                score = 1.0;
            }
            log.info("[task-tracker wubei] green map parsed: source={} index={} raw='{}' canonical='{}' score={} path={}",
                source, index, rawText, canonical, score, ocrPath);
            return new WubeiGreenMapText(canonical, score, ocrPath.toString());
        } catch (Exception e) {
            log.warn("[task-tracker wubei] green map OCR failed: source={} index={} path={}",
                source, index, ocrPath, e);
            return WubeiGreenMapText.empty();
        } finally {
            ocrImage.flush();
        }
    }

    private BufferedImage buildWubeiGreenMapOcrImage(BufferedImage frame,
                                                     TrackerGreenLinkSegment segment,
                                                     int absoluteLeft,
                                                     int absoluteTop) {
        int localLeft = Math.max(0, segment.minX() - absoluteLeft - 2);
        int localTop = Math.max(0, segment.minY() - absoluteTop - 2);
        int localRight = Math.min(frame.getWidth(), segment.maxX() - absoluteLeft + 3);
        int localBottom = Math.min(frame.getHeight(), segment.maxY() - absoluteTop + 3);
        if (localRight <= localLeft || localBottom <= localTop) {
            return null;
        }

        BufferedImage crop = ImagePreprocessor.cropCopy(
            frame, localLeft, localTop, localRight - localLeft, localBottom - localTop);
        if (crop == null) {
            return null;
        }
        int scale = 4;
        int pad = 10;
        BufferedImage ocrImage = new BufferedImage(
            crop.getWidth() * scale + pad * 2,
            crop.getHeight() * scale + pad * 2,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ocrImage.createGraphics();
        int greenPixels = 0;
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ocrImage.getWidth(), ocrImage.getHeight());
            g.setColor(Color.BLACK);
            for (int y = 0; y < crop.getHeight(); y++) {
                for (int x = 0; x < crop.getWidth(); x++) {
                    if (ImagePreprocessor.isOptionGreen(crop.getRGB(x, y))) {
                        greenPixels++;
                        g.fillRect(pad + x * scale, pad + y * scale, scale, scale);
                    }
                }
            }
        } finally {
            g.dispose();
            crop.flush();
        }
        if (greenPixels < TRACKER_LINK_MIN_PIXELS) {
            ocrImage.flush();
            return null;
        }
        return ocrImage;
    }

    private Path resolveWubeiGreenMapOcrPath(String source, int index) {
        String safeSource = safeSource(source);
        String fileName = "task_tracker_green_map_" + Integer.toHexString((safeSource + ":" + index).hashCode())
            + "_" + index + ".png";
        if (windowScopedTempPath != null) {
            return Path.of(windowScopedTempPath.resolve(fileName));
        }
        return Path.of("images", "temp", "wubei_tracker_green_map_ocr", fileName);
    }

    private String safeSource(String source) {
        return source == null || source.isBlank()
            ? "wubei"
            : source.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String joinOcrWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "";
        }
        return words.stream()
            .filter(word -> word != null && word.getText() != null && !word.getText().isBlank())
            .sorted(Comparator.comparingInt(OcrWordResult::getTop).thenComparingInt(OcrWordResult::getLeft))
            .map(OcrWordResult::getText)
            .collect(java.util.stream.Collectors.joining(""));
    }

    private String normalizeWubeiGreenMapText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s　:：,，。.;；()（）\\[\\]【】<>《》\"'`·|丨/\\\\-]+", "")
            .replaceAll("^[到至往去前往]+", "")
            .trim();
    }

    private List<TrackerGreenLinkSegment> splitWubeiTrackerGreenLinkSegments(BufferedImage frame,
                                                                             ImagePreprocessor.GreenTextBand band,
                                                                             int absoluteLeft,
                                                                             int absoluteTop) {
        List<TrackerGreenGlyph> glyphs = collectTrackerGreenGlyphs(frame, band);
        List<TrackerGreenLinkSegment> segments = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        TrackerGreenGlyph previous = null;
        for (int i = 0; i < glyphs.size(); i++) {
            TrackerGreenGlyph glyph = glyphs.get(i);
            boolean delimiter = isTrackerLinkDelimiter(glyph, pixels, remainingPixels(glyphs, i + 1));
            boolean largeGap = startX >= 0
                && previous != null
                && glyph.minX - previous.maxX - 1 >= TRACKER_LINK_SPLIT_GAP;
            if (delimiter || largeGap) {
                addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
                previous = glyph;
                if (delimiter) {
                    continue;
                }
            }
            if (startX < 0) {
                startX = glyph.minX;
            }
            endX = glyph.maxX;
            pixels += glyph.pixels;
            previous = glyph;
        }
        addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
        return segments;
    }

    private TrackerGreenLinkScan scanWuhuanTrackerGreenLinks(BufferedImage frame, int absoluteLeft, int absoluteTop) {
        List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(frame);
        ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, true);
        if (band == null) {
            log.info("[task-tracker wuhuan] green link scan: no green band");
            return TrackerGreenLinkScan.empty();
        }
        List<TrackerGreenLinkSegment> segments = splitWuhuanTrackerGreenLinkSegments(frame, band, absoluteLeft, absoluteTop);
        int bandWidth = band.maxX() - band.minX() + 1;
        log.info("[task-tracker wuhuan] green link scan: bands={} band=({}, {})-({}, {}) width={} segments={}",
            bands.size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
            absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, segments);
        return new TrackerGreenLinkScan(segments, bandWidth);
    }

    private XiuluoGreenLinkScan scanXiuluoTrackerGreenLinks(BufferedImage frame,
                                                            int absoluteLeft,
                                                            int absoluteTop,
                                                            String source) {
        List<ImagePreprocessor.GreenTextBand> bands = ImagePreprocessor.findGreenTextBands(frame);
        ImagePreprocessor.GreenTextBand band = ImagePreprocessor.pickGreenTextBand(bands, true);
        if (band == null) {
            log.info("[task-tracker xiuluo] green link scan: no green band source={}", source);
            return XiuluoGreenLinkScan.empty();
        }

        List<TrackerGreenLinkSegment> segments = splitWubeiTrackerGreenLinkSegments(frame, band, absoluteLeft, absoluteTop)
            .stream()
            .filter(this::looksLikePathingLinkSegment)
            .sorted(Comparator.comparingInt(TrackerGreenLinkSegment::minY)
                .thenComparingInt(TrackerGreenLinkSegment::minX))
            .toList();
        int bandWidth = band.maxX() - band.minX() + 1;
        if (segments.isEmpty()) {
            log.info("[task-tracker xiuluo] green link scan: no usable segment source={} bands={} band=({}, {})-({}, {}) width={}",
                source, bands.size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
                absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth);
            return new XiuluoGreenLinkScan(List.of(), bandWidth);
        }

        TrackerGreenLinkSegment first = segments.get(0);
        TaskTrackerGreenLink link = TaskTrackerGreenLink.builder()
            .minX(first.minX())
            .minY(first.minY())
            .maxX(first.maxX())
            .maxY(first.maxY())
            .pixels(first.pixels())
            .targetMapName("")
            .targetMapScore(0.0)
            .targetMapDebugPath(null)
            .build();
        log.info("[task-tracker xiuluo] green link scan: source={} bands={} band=({}, {})-({}, {}) width={} selected={}",
            source, bands.size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
            absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, link);
        return new XiuluoGreenLinkScan(List.of(link), bandWidth);
    }

    /*
     * 五环任务追踪的可点击目标是坐标数字后、进度 "[n/5]" 前的怪/NPC 名称。
     * 这里只负责选择绿色文字段，不负责截图、点击、放权或任务状态推进。
     */
    private Optional<TrackerGreenLinkSegment> findWuhuanPathingNameSegment(TrackerGreenLinkScan scan) {
        List<TrackerGreenLinkSegment> segments = scan.segments;
        if (segments.size() == 1) {
            TrackerGreenLinkSegment only = segments.get(0);
            return looksLikePathingLinkSegment(only) ? Optional.of(only) : Optional.empty();
        }
        if (segments.size() == 2) {
            TrackerGreenLinkSegment last = segments.get(1);
            if (looksLikePathingLinkSegment(last)) {
                return Optional.of(last);
            }
            TrackerGreenLinkSegment first = segments.get(0);
            return looksLikePathingLinkSegment(first) ? Optional.of(first) : Optional.empty();
        }
        if (segments.size() < 3) {
            return Optional.empty();
        }

        TrackerGreenLinkSegment last = segments.get(segments.size() - 1);
        TrackerGreenLinkSegment beforeProgress = segments.get(segments.size() - 2);
        if (looksLikeProgressTailSegment(last) && looksLikePathingLinkSegment(beforeProgress)) {
            return Optional.of(beforeProgress);
        }
        return looksLikePathingLinkSegment(last) ? Optional.of(last) : Optional.empty();
    }

    private boolean looksLikePathingLinkSegment(TrackerGreenLinkSegment segment) {
        return segment.width() >= 18 && segment.pixels >= 50;
    }

    private boolean looksLikeProgressTailSegment(TrackerGreenLinkSegment segment) {
        return segment.width() <= 18 && segment.pixels <= 70;
    }

    private List<TrackerGreenLinkSegment> splitWuhuanTrackerGreenLinkSegments(BufferedImage frame,
                                                                              ImagePreprocessor.GreenTextBand band,
                                                                              int absoluteLeft,
                                                                              int absoluteTop) {
        List<TrackerGreenLinkSegment> targets = new ArrayList<>();
        for (ImagePreprocessor.GreenTextBand line : splitTrackerGreenLines(frame, band)) {
            List<TrackerGreenGlyph> lineGlyphs = collectTrackerGreenGlyphs(frame, line);
            Optional<TrackerGreenLinkSegment> target = resolveTrackerTargetNameSegment(
                lineGlyphs, line, absoluteLeft, absoluteTop);
            target.ifPresent(targets::add);
        }
        if (!targets.isEmpty()) {
            return targets;
        }

        List<TrackerGreenGlyph> glyphs = collectTrackerGreenGlyphs(frame, band);
        List<TrackerGreenLinkSegment> segments = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        TrackerGreenGlyph previous = null;
        for (int i = 0; i < glyphs.size(); i++) {
            TrackerGreenGlyph glyph = glyphs.get(i);
            boolean delimiter = isTrackerLinkDelimiter(glyph, pixels, remainingPixels(glyphs, i + 1));
            boolean largeGap = startX >= 0
                && previous != null
                && glyph.minX - previous.maxX - 1 >= TRACKER_LINK_SPLIT_GAP;
            if (delimiter) {
                addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
                previous = glyph;
                continue;
            }
            if (largeGap) {
                addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
                startX = -1;
                endX = -1;
                pixels = 0;
            }
            if (startX < 0) {
                startX = glyph.minX;
            }
            endX = glyph.maxX;
            pixels += glyph.pixels;
            previous = glyph;
        }
        addTrackerSegment(segments, absoluteLeft, absoluteTop, startX, endX, band, pixels);
        return segments;
    }

    private List<ImagePreprocessor.GreenTextBand> splitTrackerGreenLines(BufferedImage frame,
                                                                         ImagePreprocessor.GreenTextBand band) {
        List<ImagePreprocessor.GreenTextBand> lines = new ArrayList<>();
        int startY = -1;
        int endY = -1;
        for (int y = band.minY(); y <= band.maxY(); y++) {
            int pixels = 0;
            for (int x = band.minX(); x <= band.maxX(); x++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    pixels++;
                }
            }
            if (pixels >= 4) {
                if (startY < 0) {
                    startY = y;
                }
                endY = y;
            } else if (startY >= 0) {
                lines.add(cropGreenBandToRows(frame, band, startY, endY));
                startY = -1;
                endY = -1;
            }
        }
        if (startY >= 0) {
            lines.add(cropGreenBandToRows(frame, band, startY, endY));
        }
        return lines;
    }

    private ImagePreprocessor.GreenTextBand cropGreenBandToRows(BufferedImage frame,
                                                                ImagePreprocessor.GreenTextBand band,
                                                                int minY,
                                                                int maxY) {
        int minX = Integer.MAX_VALUE;
        int maxX = -1;
        int pixels = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = band.minX(); x <= band.maxX(); x++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    pixels++;
                }
            }
        }
        return new ImagePreprocessor.GreenTextBand(minX, minY, maxX, maxY, pixels);
    }

    private Optional<TrackerGreenLinkSegment> resolveTrackerTargetNameSegment(List<TrackerGreenGlyph> glyphs,
                                                                              ImagePreprocessor.GreenTextBand line,
                                                                              int absoluteLeft,
                                                                              int absoluteTop) {
        if (glyphs.isEmpty()) {
            return Optional.empty();
        }
        int endIndex = findProgressTailStart(glyphs).orElse(glyphs.size()) - 1;
        if (endIndex < 0) {
            return Optional.empty();
        }
        Optional<Integer> afterCoordinate = findGlyphAfterCoordinateRun(glyphs, endIndex);
        if (afterCoordinate.isPresent()) {
            return buildSegmentFromGlyphRange(glyphs, afterCoordinate.get(), endIndex, line, absoluteLeft, absoluteTop);
        }
        Optional<Integer> progressStart = findProgressTailStart(glyphs);
        if (progressStart.isPresent()) {
            return buildSegmentFromGlyphRange(glyphs, 0, progressStart.get() - 1, line, absoluteLeft, absoluteTop);
        }
        return Optional.empty();
    }

    private Optional<Integer> findProgressTailStart(List<TrackerGreenGlyph> glyphs) {
        if (glyphs.size() < 2) {
            return Optional.empty();
        }
        TrackerGreenGlyph last = glyphs.get(glyphs.size() - 1);
        TrackerGreenGlyph beforeLast = glyphs.get(glyphs.size() - 2);
        int minX = beforeLast.minX;
        int maxX = last.maxX;
        int pixels = beforeLast.pixels + last.pixels;
        if (maxX - minX + 1 <= 24 && pixels <= 80 && beforeLast.width() <= TRACKER_COORD_GLYPH_MAX_WIDTH) {
            return Optional.of(glyphs.size() - 2);
        }
        if (last.width() <= 18 && last.pixels <= 70) {
            return Optional.of(glyphs.size() - 1);
        }
        return Optional.empty();
    }

    private Optional<Integer> findGlyphAfterCoordinateRun(List<TrackerGreenGlyph> glyphs, int endIndex) {
        int bestAfter = -1;
        int runStart = -1;
        for (int i = 0; i <= endIndex; i++) {
            boolean narrow = glyphs.get(i).width() <= TRACKER_COORD_GLYPH_MAX_WIDTH;
            if (narrow) {
                if (runStart < 0) {
                    runStart = i;
                }
                continue;
            }
            if (runStart >= 0 && i - runStart >= TRACKER_COORD_GLYPH_MIN_RUN && i <= endIndex) {
                bestAfter = i;
            }
            runStart = -1;
        }
        if (runStart >= 0 && endIndex + 1 - runStart >= TRACKER_COORD_GLYPH_MIN_RUN
            && endIndex + 1 < glyphs.size()) {
            bestAfter = endIndex + 1;
        }
        return bestAfter >= 0 ? Optional.of(bestAfter) : Optional.empty();
    }

    private Optional<TrackerGreenLinkSegment> buildSegmentFromGlyphRange(List<TrackerGreenGlyph> glyphs,
                                                                         int startIndex,
                                                                         int endIndex,
                                                                         ImagePreprocessor.GreenTextBand line,
                                                                         int absoluteLeft,
                                                                         int absoluteTop) {
        if (startIndex < 0 || endIndex < startIndex || endIndex >= glyphs.size()) {
            return Optional.empty();
        }
        int minX = glyphs.get(startIndex).minX;
        int maxX = glyphs.get(endIndex).maxX;
        int pixels = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            pixels += glyphs.get(i).pixels;
        }
        if (pixels < TRACKER_LINK_MIN_PIXELS) {
            return Optional.empty();
        }
        return Optional.of(new TrackerGreenLinkSegment(
            absoluteLeft + minX,
            absoluteTop + line.minY(),
            absoluteLeft + maxX,
            absoluteTop + line.maxY(),
            pixels));
    }

    private List<TrackerGreenGlyph> collectTrackerGreenGlyphs(BufferedImage frame,
                                                              ImagePreprocessor.GreenTextBand band) {
        List<TrackerGreenGlyph> glyphs = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        for (int x = band.minX(); x <= band.maxX(); x++) {
            int columnPixels = 0;
            for (int y = band.minY(); y <= band.maxY(); y++) {
                if (ImagePreprocessor.isOptionGreen(frame.getRGB(x, y))) {
                    columnPixels++;
                }
            }
            if (columnPixels > 0) {
                if (startX < 0) {
                    startX = x;
                }
                endX = x;
                pixels += columnPixels;
            } else if (startX >= 0) {
                glyphs.add(new TrackerGreenGlyph(startX, endX, pixels));
                startX = -1;
                endX = -1;
                pixels = 0;
            }
        }
        if (startX >= 0) {
            glyphs.add(new TrackerGreenGlyph(startX, endX, pixels));
        }
        return glyphs;
    }

    private boolean isTrackerLinkDelimiter(TrackerGreenGlyph glyph, int leftPixels, int rightPixels) {
        return glyph.width() <= TRACKER_LINK_DELIMITER_MAX_WIDTH
            && glyph.pixels <= TRACKER_LINK_DELIMITER_MAX_PIXELS
            && leftPixels >= TRACKER_LINK_MIN_PIXELS
            && rightPixels >= TRACKER_LINK_MIN_PIXELS;
    }

    private int remainingPixels(List<TrackerGreenGlyph> glyphs, int fromIndex) {
        int total = 0;
        for (int i = fromIndex; i < glyphs.size(); i++) {
            total += glyphs.get(i).pixels;
        }
        return total;
    }

    private void addTrackerSegment(List<TrackerGreenLinkSegment> segments,
                                   int absoluteLeft,
                                   int absoluteTop,
                                   int startX,
                                   int endX,
                                   ImagePreprocessor.GreenTextBand band,
                                   int pixels) {
        if (pixels < TRACKER_LINK_MIN_PIXELS || endX < startX) {
            return;
        }
        segments.add(new TrackerGreenLinkSegment(
            absoluteLeft + startX,
            absoluteTop + band.minY(),
            absoluteLeft + endX,
            absoluteTop + band.maxY(),
            pixels));
    }

    private Point resolveTrackerGreenClickPoint(TrackerGreenLinkSegment segment) {
        return new Point((segment.minX + segment.maxX) / 2, (segment.minY + segment.maxY) / 2);
    }

    private Point resolveWubeiTrackerGreenClickPoint(TaskTrackerGreenLink link) {
        int clickX = link.getMinX() + Math.min(18, Math.max(0, link.width() / 3));
        int clickY = (link.getMinY() + link.getMaxY()) / 2;
        return new Point(clickX, clickY);
    }

    private TaskTrackerFastMatchResult chainedFastResult(boolean matched,
                                                        int distance,
                                                        long startedAt,
                                                        String markedPath,
                                                        String reason) {
        return TaskTrackerFastMatchResult.builder()
                .matched(matched)
                .distance(distance)
                .maxDistance(WUBEI_CHAINED_FAST_FINGERPRINT_MAX_DISTANCE)
                .score(fingerprintScore(distance))
                .elapsedMs(System.currentTimeMillis() - startedAt)
                .debugImagePath(markedPath)
                .reason(reason)
                .build();
    }

    private double fingerprintScore(int distance) {
        if (distance == Integer.MAX_VALUE) {
            return 0.0;
        }
        return Math.max(0.0, 1.0 - (distance / 100.0));
    }

    private String writeChainedFastMarkedImage(BufferedImage raw,
                                               PreparedDialogAction cachedAction,
                                               String source,
                                               boolean matched,
                                               int distance) {
        String outputPath = windowScopedTempPath == null
                ? Path.of("images/test-cases/task-tracker/wubei-task-panel/output",
                        "chained-fast-" + safeSource(source) + ".png").toString()
                : windowScopedTempPath.resolve("task_tracker_chained_fast_" + safeSource(source) + ".png");
        BufferedImage marked = null;
        try {
            Path output = Path.of(outputPath);
            Files.createDirectories(output.toAbsolutePath().getParent());
            marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = marked.createGraphics();
            try {
                g.drawImage(raw, 0, 0, null);
                g.setColor(matched ? Color.GREEN : Color.RED);
                g.drawRect(0, 0, Math.max(0, raw.getWidth() - 1), Math.max(0, raw.getHeight() - 1));
                g.drawString("d=" + distance + " click=(" + cachedAction.getAbsoluteX() + ","
                        + cachedAction.getAbsoluteY() + ")", 2, Math.max(12, raw.getHeight() - 2));
            } finally {
                g.dispose();
            }
            ImageIO.write(marked, "png", output.toFile());
            return output.toString();
        } catch (IOException e) {
            log.warn("[task-tracker wubei] chained fast marked image failed: source={} path={}",
                    source, outputPath, e);
            return null;
        } finally {
            if (marked != null) {
                marked.flush();
            }
        }
    }

    private String writeXiuluoTrackerMarkedImage(BufferedImage detail,
                                                 int absoluteLeft,
                                                 int absoluteTop,
                                                 TaskTrackerTitleTemplate titleTemplate,
                                                 List<TaskTrackerGreenLink> links,
                                                 Path markedOutputPath,
                                                 String source) {
        if (markedOutputPath == null) {
            return null;
        }
        try {
            Files.createDirectories(markedOutputPath.getParent());
            BufferedImage marked = new BufferedImage(detail.getWidth(), detail.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = marked.createGraphics();
            try {
                g.drawImage(detail, 0, 0, null);
                g.setColor(Color.ORANGE);
                g.drawRect(TASK_DETAIL_LEFT_PADDING, 0,
                    Math.max(1, Math.min(70, detail.getWidth() - TASK_DETAIL_LEFT_PADDING - 1)),
                    Math.max(1, Math.min(16, detail.getHeight() - 1)));
                g.drawString(titleTemplate.getDisplayName(), 2, Math.min(12, detail.getHeight() - 3));
                if (!links.isEmpty()) {
                    TaskTrackerGreenLink link = links.get(0);
                    int localLeft = link.getMinX() - absoluteLeft;
                    int localTop = link.getMinY() - absoluteTop;
                    int localRight = link.getMaxX() - absoluteLeft;
                    int localBottom = link.getMaxY() - absoluteTop;
                    Point click = resolveWubeiTrackerGreenClickPoint(link);
                    int clickX = click.x - absoluteLeft;
                    int clickY = click.y - absoluteTop;
                    g.setColor(Color.CYAN);
                    g.drawRect(localLeft, localTop, Math.max(1, localRight - localLeft),
                        Math.max(1, localBottom - localTop));
                    g.setColor(Color.RED);
                    g.fillOval(clickX - 3, clickY - 3, 7, 7);
                    g.drawString("click=(" + click.x + "," + click.y + ")", 2, detail.getHeight() - 3);
                }
            } finally {
                g.dispose();
            }
            ImageIO.write(marked, "png", markedOutputPath.toFile());
            return markedOutputPath.toString();
        } catch (IOException e) {
            log.warn("[task-tracker xiuluo] marked image failed: source={} path={}", source, markedOutputPath, e);
            return null;
        }
    }

    private Optional<PreparedDialogAction> buildTaskTrackerPreparedAction(String source,
                                                                         String targetKeyword,
                                                                         TaskDetailCrop crop,
                                                                         BufferedImage image,
                                                                         Point click) {
        int localX = click.x - crop.absoluteLeft();
        int localY = click.y - crop.absoluteTop();
        int left = Math.max(0, localX - 6);
        int top = Math.max(0, localY - 6);
        int right = Math.min(image.getWidth(), localX + 18);
        int bottom = Math.min(image.getHeight(), localY + 10);
        if (right <= left || bottom <= top) {
            return Optional.empty();
        }

        BufferedImage validation = ImagePreprocessor.cropCopy(image, left, top, right - left, bottom - top);
        if (validation == null) {
            return Optional.empty();
        }
        BufferedImage washed = ImagePreprocessor.washGreenTextToBlackAndWhite(validation);
        try {
            String fingerprint = ImagePreprocessor.buildBinaryFingerprint(washed);
            if (fingerprint.isBlank()) {
                return Optional.empty();
            }
            long now = System.currentTimeMillis();
            return Optional.of(PreparedDialogAction.builder()
                    .dialogType(DialogType.NONE)
                    .operation(DialogOperation.TASK_TRACKER_PATHING)
                    .targetKeyword(targetKeyword)
                    .matchedText(targetKeyword + "-tracker-green")
                    .relativeX(localX)
                    .relativeY(localY)
                    .absoluteX(click.x)
                    .absoluteY(click.y)
                    .validationLeft(crop.absoluteLeft() + left)
                    .validationTop(crop.absoluteTop() + top)
                    .validationRight(crop.absoluteLeft() + right)
                    .validationBottom(crop.absoluteTop() + bottom)
                    .washMode(DialogFingerprintWashMode.GREEN)
                    .fingerprint(fingerprint)
                    .preparedAtMs(now)
                    .lastVerifiedAtMs(now)
                    .source(source)
                    .debugImagePath(crop.path())
                    .build());
        } finally {
            validation.flush();
            if (washed != null && washed != validation) {
                washed.flush();
            }
        }
    }

    private boolean dragTrackerPanelIfNeeded(Point anchor, String source) {
        int maxX = tracker.getWindowBaseX() + TRACKER_PANEL_ANCHOR_MAX_REL_X;
        int maxY = tracker.getWindowBaseY() + TRACKER_PANEL_ANCHOR_MAX_REL_Y;
        if (anchor.x <= maxX && anchor.y <= maxY) {
            return false;
        }

        int targetX = tracker.getWindowBaseX() + TRACKER_PANEL_DRAG_TARGET_REL_X;
        int targetY = tracker.getWindowBaseY() + TRACKER_PANEL_DRAG_TARGET_REL_Y;
        log.info("[wubei] tracker panel anchor outside safe area; drag source={} anchor=({}, {}) max=({}, {}) target=({}, {})",
            source, anchor.x, anchor.y, maxX, maxY, targetX, targetY);
        return inputSequences.submitAndWait("task-tracker:drag-panel:" + source, List.of(
            InputAction.dragAndDrop(anchor.x, anchor.y, targetX, targetY),
            InputAction.sleep(500)
        ));
    }

    private record TitlePointMatch(String panelRawPath,
                                   String panelYellowPath,
                                   int panelAbsoluteLeft,
                                   int panelAbsoluteTop,
                                   Point titlePoint,
                                   double score,
                                   TaskTrackerTitleTemplate titleTemplate) {
    }

    private record TrackerPanelCapture(String rawPath, int absoluteLeft, int absoluteTop) {
    }

    private record TaskDetailCrop(String path,
                                  int absoluteLeft,
                                  int absoluteTop,
                                  TaskTrackerTitleTemplate titleTemplate) {
    }

    private record TrackerGreenLinkScan(List<TrackerGreenLinkSegment> segments, int bandWidth) {
        private static TrackerGreenLinkScan empty() {
            return new TrackerGreenLinkScan(List.of(), 0);
        }
    }

    private record WubeiGreenLinkScan(List<TaskTrackerGreenLink> links,
                                      int bandWidth,
                                      boolean isProbeObjective) {
        private static WubeiGreenLinkScan empty() {
            return new WubeiGreenLinkScan(List.of(), 0, false);
        }
    }

    private record WubeiGreenMapText(String targetMapName, double score, String debugPath) {
        private static WubeiGreenMapText empty() {
            return new WubeiGreenMapText("", 0.0, null);
        }
    }

    private record XiuluoGreenLinkScan(List<TaskTrackerGreenLink> links, int bandWidth) {
        private static XiuluoGreenLinkScan empty() {
            return new XiuluoGreenLinkScan(List.of(), 0);
        }
    }

    private record TrackerGreenLinkSegment(int minX, int minY, int maxX, int maxY, int pixels) {
        private int width() {
            return maxX - minX + 1;
        }
    }

    private record TrackerGreenGlyph(int minX, int maxX, int pixels) {
        private int width() {
            return maxX - minX + 1;
        }
    }

}
