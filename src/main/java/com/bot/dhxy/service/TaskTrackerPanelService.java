package com.bot.dhxy.service;

import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.task.ImagePreprocessOperation;
import com.bot.dhxy.cloud.task.ImageProcessorService;
import com.bot.dhxy.cloud.task.TaskClassifierCloudShadowService;
import com.bot.dhxy.cloud.task.TrackerPanelReaderCloudDecision;
import com.bot.dhxy.cloud.task.TrackerPanelReaderCloudDecisionService;
import com.bot.dhxy.cloud.task.TrackerPanelReaderCloudRequest;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelCacheEntry;
import com.bot.dhxy.model.tasktracker.TaskTrackerFastMatchResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerGreenLink;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelNegativeResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelPrepareResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelReadResult;
import com.bot.dhxy.model.tasktracker.TaskTrackerPanelSourceType;
import com.bot.dhxy.model.tasktracker.TaskTrackerTitleTemplate;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
    // Yellow title is the business presence gate; the old washed title is only for prepare-time block cropping.
    private static final String WUHUAN_TRACKER_TITLE_TEMPLATE = "images/template/wuhuan/panel_title_yellow.png";
    private static final String WUHUAN_TRACKER_PREPARE_TITLE_TEMPLATE = "images/template/wuhuan/panel_title.png";
    private static final String WUHUAN_TASK_KEY_TRACKER = "wuhuan.tracker";
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
    private static final int WUBEI_CHAINED_FAST_FINGERPRINT_MAX_DISTANCE = 8;
    private static final int WUHUAN_PANEL_CACHE_FINGERPRINT_COLUMNS = 16;
    private static final int WUHUAN_PANEL_CACHE_FINGERPRINT_ROWS = 16;
    // 同面板快速复用只接受完全相同或极轻微噪点，不做跨内容近似匹配。
    private static final int WUHUAN_PANEL_CACHE_MAX_FINGERPRINT_DISTANCE = 1;
    private static final List<TaskTrackerTitleTemplate> WUBEI_TRACKER_TITLE_TEMPLATES = List.of(
        trackerTitleTemplate(WUBEI_TASK_KEY_DIANQIAN_XIANYI, "殿前献艺", "images/template/wubei/wubei_title_dianqian_xianyi_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_SANCANG_FENGMO, "三藏封魔", "images/template/wubei/wubei_title_sancang_fengmo_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_BAOXIANG_MIQING, "宝象谜情", "images/template/wubei/wubei_title_baoxiang_miqing_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_ZHIDOU_HUANGPAO, "智斗黄袍", "images/template/wubei/wubei_title_zhidou_huangpao_yellow.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_KUIXING_GUIWEI, "魁星归位", "images/template/wubei/wubei_title_kuixing_guiwei_yellow.png")
    );
    private static final TaskTrackerTitleTemplate WUHUAN_TRACKER_TITLE = trackerTitleTemplate(
        WUHUAN_TASK_KEY_TRACKER, "五环", WUHUAN_TRACKER_TITLE_TEMPLATE);
    private static final TaskTrackerTitleTemplate XIULUO_TRACKER_TITLE = trackerTitleTemplate(
        XIULUO_TASK_KEY_TRACKER, "修罗任务", XIULUO_TRACKER_TITLE_TEMPLATE);

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final InputSequences inputSequences;
    private final MapNameCanonicalizer mapNameCanonicalizer;
    private final ImageProcessorService imageProcessorService;
    private final TaskClassifierCloudShadowService taskClassifierCloudShadowService;
    private final TrackerPanelReaderCloudDecisionService trackerPanelReaderCloudDecisionService;
    private final WindowTaskContextHolder windowTaskContextHolder;

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
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel("wuhuan", WUHUAN_TRACKER_PREPARE_TITLE_TEMPLATE);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(new File(crop.path()));
            if (image == null) {
                log.warn("[task-tracker wuhuan] detail image unreadable: path={}", crop.path());
                return null;
            }
            return findWuhuanTrackerGreenClickPoint(crop, image, "wuhuan");
        } catch (IOException e) {
            log.warn("[task-tracker wuhuan] failed to read detail image: path={}", crop.path(), e);
            return null;
        }
    }

    /**
     * Reads only the 五环 title from the left tracker panel.
     *
     * <p>This mirrors the 五倍 tracker gate: title matched means the left tracker still has a 五环
     * task block; title missed means the task should treat the tracker as absent. This method does
     * not scan green links, OCR yellow text, interpret runner negative statuses, or touch dialogs.</p>
     *
     * @param source log/temp-file source tag; nullable and sanitized before being used in paths.
     * @param allowPanelReposition whether this title read may drag the tracker panel back to the
     *                             safe area before capturing.
     * @return read result with {@code found=true} only when `panel_title_yellow.png` matched.
     */
    public TaskTrackerPanelReadResult readWuhuanTrackerTitle(String source, boolean allowPanelReposition) {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(
                source, List.of(WUHUAN_TRACKER_TITLE), false, allowPanelReposition);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            log.info("[task-tracker wuhuan] title gate miss: source={}", source);
            return TaskTrackerPanelReadResult.empty();
        }
        log.info("[task-tracker wuhuan] title gate hit: source={} title={} detail={} windowRegion={}",
                source,
                crop.titleTemplate() == null ? null : crop.titleTemplate().getDisplayName(),
                crop.path(),
                crop.windowRegion() == null ? null : crop.windowRegion().toShortText());
        return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(crop.titleTemplate())
                .detailRawPath(crop.path())
                .detailAbsoluteLeft(crop.absoluteLeft())
                .detailAbsoluteTop(crop.absoluteTop())
                .yellowText("")
                .greenLinks(List.of())
                .greenBandWidth(0)
                .probeObjective(false)
                .sourceType(TaskTrackerPanelSourceType.LOCAL)
                .build();
    }

    /**
     * Prepares the current 五环 tracker green-link click for the window watcher.
     *
     * <p>The returned action uses screen-absolute coordinates and a small green-text fingerprint
     * around the click point. The watcher validates that fingerprint before task code consumes the
     * cached click, which avoids rerunning the full tracker scan every time this window gets a turn.</p>
     *
     * @param source log/temp-file source tag; nullable and sanitized before being used in paths.
     * @param allowPanelReposition whether this prepare may send real input to drag the tracker panel back to the safe area.
     * @return prepared task-tracker pathing action, explicit fresh negative, or empty for fail-closed/no evidence.
     */
    public TaskTrackerPanelPrepareResult prepareWuhuanPathingLink(String source, boolean allowPanelReposition) {
        TrackerPanelCapture panel = resolveTrackerPanelRect(source, allowPanelReposition);
        if (panel == null || panel.rawPath() == null || panel.rawPath().isBlank()) {
            return TaskTrackerPanelPrepareResult.empty();
        }

        BufferedImage panelImage = readTrackerPanelImage(source, panel);
        if (panelImage == null) {
            return TaskTrackerPanelPrepareResult.empty();
        }
        OcrWindowRegion trackerPanelRegion = windowRegionFromPanelCapture(source, panel, panelImage);
        String panelSignature = buildWuhuanTrackerPanelFingerprint(panelImage);
        Optional<TaskTrackerPanelPrepareResult> cacheHit =
                tryBuildWuhuanTrackerCacheHit(source, panel, panelImage, panelSignature, trackerPanelRegion);
        if (cacheHit.isPresent()) {
            return cacheHit.get();
        }

        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, WUHUAN_TRACKER_PREPARE_TITLE_TEMPLATE);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return TaskTrackerPanelPrepareResult.empty();
        }

        try {
            BufferedImage image = ImageIO.read(new File(crop.path()));
            if (image == null) {
                log.warn("[task-tracker wuhuan] detail image unreadable for prepare: source={} path={}",
                        source, crop.path());
                return TaskTrackerPanelPrepareResult.empty();
            }
            Point click = findWuhuanTrackerGreenClickPoint(crop, image, source);
            if (click == null) {
                return TaskTrackerPanelPrepareResult.empty();
            }
            Optional<PreparedDialogAction> prepared = buildTaskTrackerPreparedAction(source, "wuhuan", crop, image, click);
            prepared.ifPresent(action -> updateWuhuanTrackerPanelCache(
                    source, panel, panelImage, panelSignature, click, crop.windowRegion()));
            if (prepared.isPresent()) {
                return TaskTrackerPanelPrepareResult.action(
                        prepared.get(), trackerPanelRegion, crop.windowRegion());
            }
            return TaskTrackerPanelPrepareResult.empty();
        } catch (IOException e) {
            log.warn("[task-tracker wuhuan] failed to prepare tracker action: source={} path={}",
                    source, crop.path(), e);
            return TaskTrackerPanelPrepareResult.empty();
        }
    }


    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(lower(needle))) {
                return true;
            }
        }
        return false;
    }

    private String lower(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private BufferedImage readTrackerPanelImage(String source, TrackerPanelCapture panel) {
        try {
            BufferedImage image = ImageIO.read(new File(panel.rawPath()));
            if (image == null) {
                log.warn("[task-tracker wuhuan cache] panel image unreadable: source={} path={}",
                        source, panel.rawPath());
            }
            return image;
        } catch (IOException e) {
            log.warn("[task-tracker wuhuan cache] panel image read failed: source={} path={}",
                    source, panel.rawPath(), e);
            return null;
        }
    }

    private OcrWindowRegion windowRegionFromPanelCapture(String source,
                                                         TrackerPanelCapture panel,
                                                         BufferedImage panelImage) {
        if (panel == null || panelImage == null) {
            return null;
        }
        return windowRegionFromAbsoluteRect(
                source, "tracker-panel-crop",
                panel.absoluteLeft(), panel.absoluteTop(),
                panelImage.getWidth(), panelImage.getHeight());
    }

    private OcrWindowRegion windowRegionFromAbsoluteRect(String source,
                                                        String label,
                                                        int absoluteLeft,
                                                        int absoluteTop,
                                                        int width,
                                                        int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        if (!tracker.refreshWindowState() || tracker.getWindowBaseX() < 0 || tracker.getWindowBaseY() < 0) {
            log.info("[task-tracker] window-relative ROI unavailable: source={} label={} reason=window-base-unavailable absolute=({}, {}) size={}x{}",
                    source, label, absoluteLeft, absoluteTop, width, height);
            return null;
        }
        OcrWindowRegion region = new OcrWindowRegion(
                absoluteLeft - tracker.getWindowBaseX(),
                absoluteTop - tracker.getWindowBaseY(),
                absoluteLeft - tracker.getWindowBaseX() + width,
                absoluteTop - tracker.getWindowBaseY() + height)
                .clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        if (!region.isValid()) {
            log.warn("[task-tracker] window-relative ROI invalid: source={} label={} absolute=({}, {}) size={}x{} region={}",
                    source, label, absoluteLeft, absoluteTop, width, height, region.toShortText());
            return null;
        }
        return region;
    }

    private Optional<TaskTrackerPanelPrepareResult> tryBuildWuhuanTrackerCacheHit(String source,
                                                                                  TrackerPanelCapture panel,
                                                                                  BufferedImage panelImage,
                                                                                  String panelSignature,
                                                                                  OcrWindowRegion currentTrackerPanelRegion) {
        WindowRuntimeContext runtime = windowTaskContextHolder == null
                ? null
                : windowTaskContextHolder.rawCurrent().orElse(null);
        TaskTrackerPanelCacheEntry cache = runtime == null ? null : runtime.getTaskTrackerPanelCache();
        if (cache == null || !"wuhuan".equals(cache.getTaskCode())) {
            log.info("[task-tracker wuhuan cache] miss: source={} reason=no-cache", source);
            return Optional.empty();
        }
        if (panelSignature == null || panelSignature.isBlank()
                || cache.getPanelFingerprint() == null || cache.getPanelFingerprint().isBlank()) {
            log.info("[task-tracker wuhuan cache] miss: source={} reason=fingerprint-failed cacheSource={}",
                    source, cache.getSource());
            return Optional.empty();
        }
        if (!tracker.refreshWindowState() || tracker.getWindowBaseX() < 0 || tracker.getWindowBaseY() < 0) {
            log.info("[task-tracker wuhuan cache] miss: source={} reason=window-base-unavailable cacheSource={}",
                    source, cache.getSource());
            return Optional.empty();
        }
        int panelOriginWindowX = Math.max(0, panel.absoluteLeft() - tracker.getWindowBaseX());
        int panelOriginWindowY = Math.max(0, panel.absoluteTop() - tracker.getWindowBaseY());
        if (panelOriginWindowX != cache.getPanelOriginWindowX()
                || panelOriginWindowY != cache.getPanelOriginWindowY()
                || panelImage.getWidth() != cache.getPanelWidth()
                || panelImage.getHeight() != cache.getPanelHeight()) {
            log.info("[task-tracker wuhuan cache] miss: source={} reason=panel-geometry-changed "
                            + "currentOrigin=({}, {}) currentSize={}x{} cacheOrigin=({}, {}) cacheSize={}x{} cacheSource={}",
                    source, panelOriginWindowX, panelOriginWindowY, panelImage.getWidth(), panelImage.getHeight(),
                    cache.getPanelOriginWindowX(), cache.getPanelOriginWindowY(),
                    cache.getPanelWidth(), cache.getPanelHeight(), cache.getSource());
            return Optional.empty();
        }
        int distance = fingerprintDistance(cache.getPanelFingerprint(), panelSignature);
        if (distance > WUHUAN_PANEL_CACHE_MAX_FINGERPRINT_DISTANCE) {
            log.info("[task-tracker wuhuan cache] miss: source={} reason=panel-changed distance={} max={} cacheSource={}",
                    source, distance, WUHUAN_PANEL_CACHE_MAX_FINGERPRINT_DISTANCE, cache.getSource());
            return Optional.empty();
        }
        Point clickWindowRelative = cache.clickWindowRelative();
        if (!isWindowRelativePointInsidePanel(clickWindowRelative, panelOriginWindowX, panelOriginWindowY,
                panelImage.getWidth(), panelImage.getHeight())) {
            log.info("[task-tracker wuhuan cache] miss: source={} reason=click-outside-panel click={} "
                            + "panelOrigin=({}, {}) size={}x{} cacheSource={}",
                    source, clickWindowRelative, panelOriginWindowX, panelOriginWindowY,
                    panelImage.getWidth(), panelImage.getHeight(), cache.getSource());
            return Optional.empty();
        }
        Point click = screenPointFromWindowRelative(clickWindowRelative, source, "wuhuan");
        if (click == null) {
            return Optional.empty();
        }
        String cacheHitSource = source == null || source.isBlank() ? "wuhuan:cache-hit" : source + ":cache-hit";
        Optional<PreparedDialogAction> prepared = buildTaskTrackerPreparedAction(
                cacheHitSource, "wuhuan",
                new TaskDetailCrop(panel.rawPath(), panel.absoluteLeft(), panel.absoluteTop(), null),
                panelImage, click);
        OcrWindowRegion trackerPanelRegion = cache.getTrackerPanelRegion() == null
                ? currentTrackerPanelRegion
                : cache.getTrackerPanelRegion();
        OcrWindowRegion wuhuanBlockRegion = cache.getWuhuanTrackerBlockRegion();
        prepared.ifPresent(action -> log.info("[task-tracker wuhuan cache] cache-hit: source={} "
                        + "cacheSource={} distance={} clickWindowRelative=({}, {}) absolute=({}, {}) "
                        + "trackerPanelRegion={} wuhuanBlockRegion={}",
                source, cache.getSource(), distance, clickWindowRelative.x, clickWindowRelative.y,
                action.getAbsoluteX(), action.getAbsoluteY(),
                trackerPanelRegion == null ? null : trackerPanelRegion.toShortText(),
                wuhuanBlockRegion == null ? null : wuhuanBlockRegion.toShortText()));
        return prepared.map(action -> TaskTrackerPanelPrepareResult.action(
                action, trackerPanelRegion, wuhuanBlockRegion));
    }

    private void updateWuhuanTrackerPanelCache(String source,
                                               TrackerPanelCapture panel,
                                               BufferedImage panelImage,
                                               String panelSignature,
                                               Point click,
                                               OcrWindowRegion wuhuanTrackerBlockRegion) {
        WindowRuntimeContext runtime = windowTaskContextHolder == null
                ? null
                : windowTaskContextHolder.rawCurrent().orElse(null);
        if (runtime == null || panelSignature == null || panelSignature.isBlank() || click == null) {
            return;
        }
        if (!tracker.refreshWindowState() || tracker.getWindowBaseX() < 0 || tracker.getWindowBaseY() < 0) {
            log.info("[task-tracker wuhuan cache] skip update: source={} reason=window-base-unavailable", source);
            return;
        }
        int panelOriginWindowX = Math.max(0, panel.absoluteLeft() - tracker.getWindowBaseX());
        int panelOriginWindowY = Math.max(0, panel.absoluteTop() - tracker.getWindowBaseY());
        OcrWindowRegion trackerPanelRegion = new OcrWindowRegion(
                panelOriginWindowX,
                panelOriginWindowY,
                panelOriginWindowX + panelImage.getWidth(),
                panelOriginWindowY + panelImage.getHeight())
                .clamp(GAME_CLIENT_WIDTH, GAME_CLIENT_HEIGHT);
        Point clickWindowRelative = new Point(click.x - tracker.getWindowBaseX(), click.y - tracker.getWindowBaseY());
        if (!isWindowRelativePointInsidePanel(clickWindowRelative, panelOriginWindowX, panelOriginWindowY,
                panelImage.getWidth(), panelImage.getHeight())) {
            log.info("[task-tracker wuhuan cache] skip update: source={} reason=click-outside-panel click={} "
                            + "panelOrigin=({}, {}) size={}x{}",
                    source, clickWindowRelative, panelOriginWindowX, panelOriginWindowY,
                    panelImage.getWidth(), panelImage.getHeight());
            return;
        }
        TaskTrackerPanelCacheEntry entry = TaskTrackerPanelCacheEntry.builder()
                .taskCode("wuhuan")
                .panelFingerprint(panelSignature)
                .clickWindowRelative(clickWindowRelative)
                .panelOriginWindowX(panelOriginWindowX)
                .panelOriginWindowY(panelOriginWindowY)
                .panelWidth(panelImage.getWidth())
                .panelHeight(panelImage.getHeight())
                .trackerPanelRegion(trackerPanelRegion)
                .wuhuanTrackerBlockRegion(wuhuanTrackerBlockRegion)
                .updatedAtMs(System.currentTimeMillis())
                .source(source)
                .build();
        runtime.updateTaskTrackerPanelCache(entry);
        log.info("[task-tracker wuhuan cache] updated: source={} windowId={} clickWindowRelative=({}, {}) "
                        + "panelOrigin=({}, {}) size={}x{} trackerPanelRegion={} wuhuanBlockRegion={}",
                source, runtime.getWindowId(), clickWindowRelative.x, clickWindowRelative.y,
                panelOriginWindowX, panelOriginWindowY, panelImage.getWidth(), panelImage.getHeight(),
                trackerPanelRegion.toShortText(),
                wuhuanTrackerBlockRegion == null ? null : wuhuanTrackerBlockRegion.toShortText());
    }

    private boolean isWindowRelativePointInsidePanel(Point point,
                                                     int panelOriginWindowX,
                                                     int panelOriginWindowY,
                                                     int panelWidth,
                                                     int panelHeight) {
        return point != null
                && panelWidth > 0
                && panelHeight > 0
                && point.x >= panelOriginWindowX
                && point.y >= panelOriginWindowY
                && point.x < panelOriginWindowX + panelWidth
                && point.y < panelOriginWindowY + panelHeight;
    }

    private String buildWuhuanTrackerPanelFingerprint(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return "";
        }
        int cells = WUHUAN_PANEL_CACHE_FINGERPRINT_COLUMNS * WUHUAN_PANEL_CACHE_FINGERPRINT_ROWS;
        int[] gray = new int[cells];
        long total = 0L;
        int index = 0;
        for (int row = 0; row < WUHUAN_PANEL_CACHE_FINGERPRINT_ROWS; row++) {
            int y1 = row * image.getHeight() / WUHUAN_PANEL_CACHE_FINGERPRINT_ROWS;
            int y2 = Math.max(y1 + 1, (row + 1) * image.getHeight() / WUHUAN_PANEL_CACHE_FINGERPRINT_ROWS);
            for (int col = 0; col < WUHUAN_PANEL_CACHE_FINGERPRINT_COLUMNS; col++) {
                int x1 = col * image.getWidth() / WUHUAN_PANEL_CACHE_FINGERPRINT_COLUMNS;
                int x2 = Math.max(x1 + 1, (col + 1) * image.getWidth() / WUHUAN_PANEL_CACHE_FINGERPRINT_COLUMNS);
                long sum = 0L;
                int pixels = 0;
                for (int y = y1; y < y2 && y < image.getHeight(); y++) {
                    for (int x = x1; x < x2 && x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y);
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = rgb & 0xff;
                        sum += (r * 30L + g * 59L + b * 11L) / 100L;
                        pixels++;
                    }
                }
                int average = pixels <= 0 ? 0 : (int) (sum / pixels);
                gray[index++] = average;
                total += average;
            }
        }
        int threshold = (int) (total / Math.max(1, cells));
        StringBuilder fingerprint = new StringBuilder(cells);
        for (int value : gray) {
            fingerprint.append(value >= threshold ? '1' : '0');
        }
        return fingerprint.toString();
    }

    private int fingerprintDistance(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return Integer.MAX_VALUE;
        }
        int distance = 0;
        for (int i = 0; i < left.length(); i++) {
            if (left.charAt(i) != right.charAt(i)) {
                distance++;
            }
        }
        return distance;
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
        /*
         * CR248 review-3 (P1): the live entry must use the same pipeline as the snapshot entry —
         * local raw title-template match decides the 五倍 task class and crops the detail block,
         * then the cloud only processes that detail block. Sending the whole panel let the cloud
         * re-run title matching (matchWubeiTaskKey), so the two entries had different title judges
         * and different ROIs.
         */
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, WUBEI_TRACKER_TITLE_TEMPLATES, false);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowWubeiTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, true);
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
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowWubeiTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, true);
        }
        TitlePointMatch title = findTitlePointInPanelImage(source, windowSnapshotPath.toString(),
                windowSnapshotPath.toString(), absoluteLeft, absoluteTop, WUBEI_TRACKER_TITLE_TEMPLATES);
        if (title == null) {
            log.info("[task-tracker wubei] snapshot title miss: source={} snapshot={}",
                    source, windowSnapshotPath);
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowWubeiTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, true);
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
        TaskDetailCrop cloudCrop = new TaskDetailCrop(detailPath, absoluteLeft, absoluteTop, titleTemplate);
        Optional<TrackerPanelReaderCloudDecision> cloudDecision = readTrackerPanelImageFromCloud(
                "wubei", "wubei-tracker-detail", source, cloudCrop, -1, "TASK_AWARE_FIRST_LINK");
        if (cloudDecision.isPresent()) {
            return wubeiResultFromCloudDecision(source, cloudCrop, cloudDecision.get());
        }
        /*
         * CR248 (CR208 items 3/4): the cloud reader owns 五倍 detail yellow text and green-link
         * destination map names. CR257 C2 (D2 approved) deleted the pre-CR248 local OCR pipeline:
         * a cloud miss OR an inactive cloud reader is now a miss, never a local OCR run.
         */
        log.info("[task-tracker wubei] no wubei detail result; no local OCR fallback: source={} cloudActive={} detail={}",
                source, trackerPanelReaderCloudDecisionService.isActive(), detailPath);
        TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
        CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowWubeiTrackerResult(source, result);
        return applyTaskClassifierDecision(source, result, cloudResult, true);
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
        Optional<TaskTrackerPanelReadResult> cloudPanelResult = readXiuluoTrackerPanelFromCloudPanel(source);
        if (cloudPanelResult.isPresent()) {
            return cloudPanelResult.get();
        }

        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, List.of(XIULUO_TRACKER_TITLE));
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, false);
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
        TaskTrackerGreenLink link = panel.getSelectedGreenLink() == null
                ? panel.getGreenLinks().get(0)
                : panel.getSelectedGreenLink();
        return Optional.of(resolveWubeiTrackerGreenClickPoint(link));
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
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, false);
        }
        String safeSource = safeSource(source);
        Path yellowPath = panelRawPath.resolveSibling(panelRawPath.getFileName()
            + "." + safeSource + ".xiuluo-title-yellow.png");
        if (!washYellowToPath(panelRawPath, yellowPath, source, "xiuluo-tracker-title-yellow")) {
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, false);
        }
        TitlePointMatch title = findTitlePointInPanelImage(source, panelRawPath.toString(), yellowPath.toString(),
            absoluteLeft, absoluteTop, List.of(XIULUO_TRACKER_TITLE));
        if (title == null) {
            log.info("[task-tracker xiuluo] replay title miss: source={} panel={} yellow={}",
                source, panelRawPath, yellowPath);
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, false);
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
            TaskTrackerGreenLink link = panel.getSelectedGreenLink() == null
                    ? panel.getGreenLinks().get(0)
                    : panel.getSelectedGreenLink();
            TaskDetailCrop crop = new TaskDetailCrop(
                    panel.getDetailRawPath(),
                    panel.getDetailAbsoluteLeft(),
                    panel.getDetailAbsoluteTop(),
                    panel.getTitleTemplate());
            Point click = resolveWubeiTrackerGreenClickPoint(link);
            Optional<PreparedDialogAction> action = buildTaskTrackerPreparedAction(
                    source, "wubei-chained", crop, image, click);
            action = action.map(prepared -> prepared.toBuilder()
                    .trackerPanelSourceType(link.getSourceType())
                    .build());
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
            ImageProcessorService.ImageProcessorResult washResult = imageProcessorService.washGreenTextToBlackAndWhite(
                    raw,
                    imageProcessorMetadata(source, "wubei-chained-fast-green-wash", null,
                            "task-tracker-chained-fast"));
            if (!washResult.hasRequiredOutput()) {
                log.info("[task-tracker wubei] chained fast verify miss: source={} reason=green-wash-unavailable status={} cloudReason={}",
                        source, washResult.status(), washResult.reason());
                return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "green-wash-unavailable");
            }
            washed = washResult.image();
            ImageProcessorService.ImageProcessorResult fingerprintResult = imageProcessorService.buildBinaryFingerprint(
                    washed,
                    imageProcessorMetadata(source, "wubei-chained-fast-fingerprint", null,
                            "task-tracker-chained-fast"));
            if (!fingerprintResult.hasRequiredOutput()) {
                log.info("[task-tracker wubei] chained fast verify miss: source={} reason=fingerprint-unavailable status={} cloudReason={}",
                        source, fingerprintResult.status(), fingerprintResult.reason());
                return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "fingerprint-unavailable");
            }
            String currentFingerprint = fingerprintResult.binaryFingerprint();
            ImageProcessorService.ImageProcessorResult distanceResult = imageProcessorService.binaryFingerprintDistance(
                    cachedAction.getFingerprint(),
                    currentFingerprint,
                    imageProcessorMetadata(source, "wubei-chained-fast-fingerprint-distance", null,
                            "task-tracker-chained-fast"));
            if (!distanceResult.hasRequiredOutput()) {
                log.info("[task-tracker wubei] chained fast verify miss: source={} reason=fingerprint-distance-unavailable status={} cloudReason={}",
                        source, distanceResult.status(), distanceResult.reason());
                return chainedFastResult(false, Integer.MAX_VALUE, startedAt, null, "fingerprint-distance-unavailable");
            }
            int distance = distanceResult.binaryFingerprintDistance();
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


    private TaskTrackerPanelReadResult readXiuluoTrackerDetail(String detailPath,
                                                               int absoluteLeft,
                                                               int absoluteTop,
                                                               TaskTrackerTitleTemplate titleTemplate,
                                                               String source,
                                                               Path markedOutputPath) {
        TaskDetailCrop cloudCrop = new TaskDetailCrop(detailPath, absoluteLeft, absoluteTop, titleTemplate);
        Optional<TrackerPanelReaderCloudDecision> cloudDecision = readTrackerPanelImageFromCloud(
                "xiuluo", "xiuluo-tracker-detail", source, cloudCrop, 0, "FIRST_LINK");
        if (cloudDecision.isPresent()) {
            return xiuluoResultFromCloudDecision(source, cloudCrop, cloudDecision.get());
        }

        try {
            BufferedImage detail = ImageIO.read(new File(detailPath));
            if (detail == null) {
                log.warn("[task-tracker xiuluo] detail unreadable: source={} path={}", source, detailPath);
                TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
                CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
                return applyTaskClassifierDecision(source, result, cloudResult, false);
            }
            XiuluoGreenLinkScan scan = scanXiuluoTrackerGreenLinks(detail, absoluteLeft, absoluteTop, source);
            String markedPath = writeXiuluoTrackerMarkedImage(detail, absoluteLeft, absoluteTop, titleTemplate,
                scan.links(), markedOutputPath, source);
            boolean found = !scan.links().isEmpty();
            log.info("[task-tracker xiuluo] detail read: source={} found={} title={} links={} detail={} marked={}",
                source, found, titleTemplate.getDisplayName(), scan.links(), detailPath, markedPath);
            if (!found) {
                TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
                CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
                return applyTaskClassifierDecision(source, result, cloudResult, false);
            }
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.builder()
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
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, false);
        } catch (IOException e) {
            log.warn("[task-tracker xiuluo] detail read failed: source={} path={}", source, detailPath, e);
            TaskTrackerPanelReadResult result = TaskTrackerPanelReadResult.empty();
            CloudDecisionResult cloudResult = taskClassifierCloudShadowService.shadowXiuluoTrackerResult(source, result);
            return applyTaskClassifierDecision(source, result, cloudResult, false);
        }
    }

    /**
     * CR248 review-3: whole-panel cloud read is retired for 五倍 — it made the cloud re-run title
     * matching, giving the live entry a different title judge and ROI than the snapshot entry.
     * The live entry now crops the detail block locally like the snapshot entry does.
     */
    @Deprecated(since = "CR248", forRemoval = false)
    private Optional<TaskTrackerPanelReadResult> readWubeiTrackerPanelFromCloudPanel(String source) {
        if (!trackerPanelReaderCloudDecisionService.isActive()) {
            return Optional.empty();
        }
        TrackerPanelCapture panel = resolveTrackerPanelRect(source, true);
        if (panel == null || panel.rawPath() == null || panel.rawPath().isBlank()) {
            return Optional.of(wubeiResultFromCloudDecision(
                    source, null, trackerPanelReaderLocalFailure("missing tracker panel crop")));
        }
        TaskDetailCrop panelCrop = new TaskDetailCrop(panel.rawPath(), panel.absoluteLeft(), panel.absoluteTop(), null);
        Optional<TrackerPanelReaderCloudDecision> decision = readTrackerPanelImageFromCloud(
                "wubei", "wubei-tracker-panel", source, panel.rawPath(), panel.absoluteLeft(), panel.absoluteTop(),
                "TRACKER_PANEL_CROP", -1, "TASK_AWARE_FIRST_LINK");
        return decision.map(value -> wubeiResultFromCloudDecision(source, panelCrop, value));
    }

    private Optional<TaskTrackerPanelReadResult> readXiuluoTrackerPanelFromCloudPanel(String source) {
        if (!trackerPanelReaderCloudDecisionService.isActive()) {
            return Optional.empty();
        }
        TrackerPanelCapture panel = resolveTrackerPanelRect(source, true);
        if (panel == null || panel.rawPath() == null || panel.rawPath().isBlank()) {
            return Optional.of(xiuluoResultFromCloudDecision(
                    source, null, trackerPanelReaderLocalFailure("missing tracker panel crop")));
        }
        TaskDetailCrop panelCrop = new TaskDetailCrop(panel.rawPath(), panel.absoluteLeft(), panel.absoluteTop(), null);
        Optional<TrackerPanelReaderCloudDecision> decision = readTrackerPanelImageFromCloud(
                "xiuluo", "xiuluo-tracker-panel", source, panel.rawPath(), panel.absoluteLeft(), panel.absoluteTop(),
                "TRACKER_PANEL_CROP", 0, "FIRST_LINK");
        return decision.map(value -> xiuluoResultFromCloudDecision(source, panelCrop, value));
    }

    private Optional<TrackerPanelReaderCloudDecision> readTrackerPanelImageFromCloud(String taskCode,
                                                                                    String phase,
                                                                                    String source,
                                                                                    TaskDetailCrop crop,
                                                                                    int requestedLinkIndex,
                                                                                    String selectionPolicy) {
        if (crop == null) {
            return readTrackerPanelImageFromCloud(taskCode, phase, source,
                    null, -1, -1, "DETAIL_BLOCK_CROP", requestedLinkIndex, selectionPolicy, "");
        }
        /*
         * CR248: the local raw title-template match already established the task key for this
         * detail block. Send it so the cloud detail mode never re-runs title matching — one title
         * judge, one ROI, shared by the live and snapshot entries.
         */
        String localTaskKey = crop.titleTemplate() == null ? "" : crop.titleTemplate().getTaskKey();
        return readTrackerPanelImageFromCloud(taskCode, phase, source,
                crop.path(), crop.absoluteLeft(), crop.absoluteTop(),
                "DETAIL_BLOCK_CROP", requestedLinkIndex, selectionPolicy, localTaskKey);
    }

    private Optional<TrackerPanelReaderCloudDecision> readTrackerPanelImageFromCloud(String taskCode,
                                                                                    String phase,
                                                                                    String source,
                                                                                    String imagePath,
                                                                                    int absoluteLeft,
                                                                                    int absoluteTop,
                                                                                    String imageMode,
                                                                                    int requestedLinkIndex,
                                                                                    String selectionPolicy) {
        return readTrackerPanelImageFromCloud(taskCode, phase, source, imagePath, absoluteLeft, absoluteTop,
                imageMode, requestedLinkIndex, selectionPolicy, "");
    }

    private Optional<TrackerPanelReaderCloudDecision> readTrackerPanelImageFromCloud(String taskCode,
                                                                                    String phase,
                                                                                    String source,
                                                                                    String imagePath,
                                                                                    int absoluteLeft,
                                                                                    int absoluteTop,
                                                                                    String imageMode,
                                                                                    int requestedLinkIndex,
                                                                                    String selectionPolicy,
                                                                                    String taskKey) {
        if (!trackerPanelReaderCloudDecisionService.isActive()) {
            return Optional.empty();
        }
        if (imagePath == null || imagePath.isBlank()) {
            return Optional.of(trackerPanelReaderLocalFailure("missing tracker image crop"));
        }
        try {
            byte[] png = Files.readAllBytes(Path.of(imagePath));
            int originWindowX;
            int originWindowY;
            if ("XIULUO_ACCEPT_SNAPSHOT".equals(imageMode)) {
                originWindowX = 0;
                originWindowY = 0;
            } else {
                tracker.refreshWindowState();
                originWindowX = Math.max(0, absoluteLeft - tracker.getWindowBaseX());
                originWindowY = Math.max(0, absoluteTop - tracker.getWindowBaseY());
            }
            TrackerPanelReaderCloudRequest request = TrackerPanelReaderCloudRequest.builder()
                    .taskCode(taskCode)
                    .phase(phase)
                    .source(source)
                    .imagePayloadBase64(Base64.getEncoder().encodeToString(png))
                    .payloadMimeType("image/png")
                    .imageSha256(sha256Hex(png))
                    .imageMode(imageMode)
                    .imageOriginWindowX(originWindowX)
                    .imageOriginWindowY(originWindowY)
                    .requestedLinkIndex(requestedLinkIndex)
                    .selectionPolicy(selectionPolicy)
                    .taskKey(taskKey)
                    .build();
            TrackerPanelReaderCloudDecision decision = trackerPanelReaderCloudDecisionService.read(request);
            log.info("[task-tracker cloud-reader] result: taskCode={} phase={} source={} status={} action={} "
                            + "taskKey={} click={} links={} reason={} originWindow=({}, {}) detail={}",
                    taskCode, phase, source, decision.getStatus(), decision.getAction(), decision.getTaskKey(),
                    decision.getClickWindowRelative(), decision.getLinks(), decision.getReason(),
                    originWindowX, originWindowY, imagePath);
            return Optional.of(decision);
        } catch (IOException | RuntimeException e) {
            log.warn("[task-tracker cloud-reader] failed to build payload; no local fallback while active: "
                            + "taskCode={} phase={} source={} detail={}",
                    taskCode, phase, source, imagePath, e);
            return Optional.of(trackerPanelReaderLocalFailure("local payload error: " + e.getMessage()));
        }
    }

    private TaskTrackerPanelReadResult wubeiResultFromCloudDecision(String source,
                                                                    TaskDetailCrop crop,
                                                                    TrackerPanelReaderCloudDecision decision) {
        if (decision == null || !decision.found()) {
            log.warn("[task-tracker cloud-reader wubei] no production result: source={} status={} reason={}",
                    source, decision == null ? null : decision.getStatus(), decision == null ? null : decision.getReason());
            return TaskTrackerPanelReadResult.empty();
        }
        TaskTrackerTitleTemplate cloudTemplate = resolveWubeiCloudTitleTemplate(decision.taskKey());
        if (cloudTemplate == null) {
            log.warn("[task-tracker cloud-reader wubei] unsupported taskKey; no local fallback: source={} taskKey={} reason={}",
                    source, decision.taskKey(), decision.reason());
            return TaskTrackerPanelReadResult.empty();
        }
        List<TaskTrackerGreenLink> links = trackerLinksFromCloudDecision(decision, source, "wubei");
        TaskTrackerGreenLink selected = trackerLinkFromWindowRelativeClick(
                decision.clickWindowRelative(), source, "wubei", -1);
        selected = copyCloudLinkBusinessFields(selected, links);
        if (links.isEmpty() && selected != null) {
            links = List.of(selected);
        }
        String yellowText = wubeiYellowTextFromCloudDecision(decision);
        return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(cloudTemplate)
                .detailRawPath(crop == null ? "" : crop.path())
                .detailAbsoluteLeft(crop == null ? 0 : crop.absoluteLeft())
                .detailAbsoluteTop(crop == null ? 0 : crop.absoluteTop())
                .yellowText(yellowText)
                .greenLinks(links)
                .selectedGreenLink(selected)
                .greenBandWidth(links.stream().mapToInt(TaskTrackerGreenLink::width).max().orElse(0))
                .probeObjective(WUBEI_TASK_KEY_BAOXIANG_MIQING.equals(cloudTemplate.getTaskKey()) && links.size() > 1)
                .sourceType(TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER)
                .build();
    }

    private TaskTrackerPanelReadResult xiuluoResultFromCloudDecision(String source,
                                                                     TaskDetailCrop crop,
                                                                     TrackerPanelReaderCloudDecision decision) {
        if (decision == null || !decision.clickAction()) {
            log.warn("[task-tracker cloud-reader xiuluo] no production click; no local fallback: source={} status={} reason={}",
                    source, decision == null ? null : decision.getStatus(), decision == null ? null : decision.getReason());
            return TaskTrackerPanelReadResult.empty();
        }
        List<TaskTrackerGreenLink> links = trackerLinksFromCloudDecision(decision, source, "xiuluo");
        TaskTrackerGreenLink selected = trackerLinkFromWindowRelativeClick(
                decision.clickWindowRelative(), source, "xiuluo", -1);
        selected = copyCloudLinkBusinessFields(selected, links);
        if (links.isEmpty() && selected != null) {
            links = List.of(selected);
        }
        if (links.isEmpty()) {
            return TaskTrackerPanelReadResult.empty();
        }
        return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(XIULUO_TRACKER_TITLE)
                .detailRawPath(crop == null ? "" : crop.path())
                .detailAbsoluteLeft(crop == null ? 0 : crop.absoluteLeft())
                .detailAbsoluteTop(crop == null ? 0 : crop.absoluteTop())
                .yellowText("")
                .greenLinks(links)
                .selectedGreenLink(selected)
                .greenBandWidth(links.stream().mapToInt(TaskTrackerGreenLink::width).max().orElse(0))
                .probeObjective(false)
                .sourceType(TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER)
                .build();
    }

    private List<TaskTrackerGreenLink> trackerLinksFromCloudDecision(TrackerPanelReaderCloudDecision decision,
                                                                     String source,
                                                                     String taskCode) {
        List<TaskTrackerGreenLink> links = new ArrayList<>();
        List<TrackerPanelReaderCloudDecision.Link> cloudLinks = decision.links();
        if (cloudLinks.isEmpty() && decision.clickAction()) {
            links.add(trackerLinkFromWindowRelativeClick(decision.clickWindowRelative(), source, taskCode, 0));
        } else {
            for (TrackerPanelReaderCloudDecision.Link link : cloudLinks) {
                TaskTrackerGreenLink converted = trackerLinkFromCloudLink(link, source, taskCode);
                if (converted != null) {
                    links.add(converted);
                }
            }
        }
        return links.stream().filter(link -> link != null).toList();
    }

    private TaskTrackerGreenLink trackerLinkFromWindowRelativeClick(Point windowRelativeClick,
                                                                    String source,
                                                                    String taskCode,
                                                                    int index) {
        Point screen = screenPointFromWindowRelative(windowRelativeClick, source, taskCode);
        if (screen == null) {
            return null;
        }
        return TaskTrackerGreenLink.builder()
                .minX(screen.x)
                .minY(screen.y)
                .maxX(screen.x)
                .maxY(screen.y)
                .pixels(1)
                .targetMapName("")
                .targetMapScore(0.0D)
                .targetMapDebugPath("")
                .sourceType(TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER)
                .build();
    }

    private TaskTrackerGreenLink trackerLinkFromCloudLink(TrackerPanelReaderCloudDecision.Link link,
                                                          String source,
                                                          String taskCode) {
        if (link == null) {
            return null;
        }
        int[] rect = parseCloudWindowRelativeRect(link.getWindowRelativeRect());
        if (rect == null) {
            TaskTrackerGreenLink pointLink = trackerLinkFromWindowRelativeClick(
                    link.clickWindowRelative(), source, taskCode, link.getIndex());
            if (pointLink == null) {
                return null;
            }
            return TaskTrackerGreenLink.builder()
                    .minX(pointLink.getMinX())
                    .minY(pointLink.getMinY())
                    .maxX(pointLink.getMaxX())
                    .maxY(pointLink.getMaxY())
                    .pixels(pointLink.getPixels())
                    .targetMapName(link.getTargetMapName())
                    .targetMapScore(0.0D)
                    .targetMapDebugPath("")
                    .sourceType(TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER)
                    .build();
        }
        if (!tracker.refreshWindowState() || tracker.getWindowBaseX() < 0 || tracker.getWindowBaseY() < 0) {
            log.warn("[task-tracker cloud-reader] cannot resolve window base for link rect: taskCode={} source={} rect={}",
                    taskCode, source, link.getWindowRelativeRect());
            return null;
        }
        return TaskTrackerGreenLink.builder()
                .minX(tracker.getWindowBaseX() + rect[0])
                .minY(tracker.getWindowBaseY() + rect[1])
                .maxX(tracker.getWindowBaseX() + rect[2])
                .maxY(tracker.getWindowBaseY() + rect[3])
                .pixels(Math.max(1, (rect[2] - rect[0] + 1) * (rect[3] - rect[1] + 1)))
                .targetMapName(link.getTargetMapName())
                .targetMapScore(0.0D)
                .targetMapDebugPath("")
                .sourceType(TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER)
                .build();
    }

    private TaskTrackerGreenLink copyCloudLinkBusinessFields(TaskTrackerGreenLink selected,
                                                             List<TaskTrackerGreenLink> links) {
        if (selected == null || links == null || links.isEmpty()) {
            return selected;
        }
        Point selectedCenter = selected.centerPoint();
        for (TaskTrackerGreenLink link : links) {
            if (link != null && selectedCenter.equals(link.centerPoint())) {
                return link;
            }
        }
        TaskTrackerGreenLink source = links.get(0);
        return TaskTrackerGreenLink.builder()
                .minX(selected.getMinX())
                .minY(selected.getMinY())
                .maxX(selected.getMaxX())
                .maxY(selected.getMaxY())
                .pixels(selected.getPixels())
                .targetMapName(source.getTargetMapName())
                .targetMapScore(source.getTargetMapScore())
                .targetMapDebugPath(source.getTargetMapDebugPath())
                .sourceType(TaskTrackerPanelSourceType.CLOUD_TRACKER_PANEL_READER)
                .build();
    }

    private static int[] parseCloudWindowRelativeRect(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            int minX = Integer.parseInt(parts[0].trim());
            int minY = Integer.parseInt(parts[1].trim());
            int maxX = Integer.parseInt(parts[2].trim());
            int maxY = Integer.parseInt(parts[3].trim());
            if (minX < 0 || minY < 0 || maxX < minX || maxY < minY
                    || maxX >= GAME_CLIENT_WIDTH || maxY >= GAME_CLIENT_HEIGHT) {
                return null;
            }
            return new int[]{minX, minY, maxX, maxY};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String wubeiYellowTextFromCloudDecision(TrackerPanelReaderCloudDecision decision) {
        if (decision == null) {
            return "";
        }
        if (decision.getYellowText() != null && !decision.getYellowText().isBlank()) {
            return decision.getYellowText().trim();
        }
        return decision.getTargetName() == null ? "" : decision.getTargetName().trim();
    }

    private Point screenPointFromWindowRelative(Point windowRelativeClick, String source, String taskCode) {
        if (windowRelativeClick == null) {
            return null;
        }
        if (tracker.refreshWindowState() && tracker.getWindowBaseX() >= 0 && tracker.getWindowBaseY() >= 0) {
            return new Point(tracker.getWindowBaseX() + windowRelativeClick.x,
                    tracker.getWindowBaseY() + windowRelativeClick.y);
        }
        log.warn("[task-tracker cloud-reader] cannot resolve window base for click: taskCode={} source={} relative=({}, {})",
                taskCode, source, windowRelativeClick.x, windowRelativeClick.y);
        return null;
    }

    private static TrackerPanelReaderCloudDecision trackerPanelReaderLocalFailure(String reason) {
        return TrackerPanelReaderCloudDecision.builder()
                .status(TrackerPanelReaderCloudDecision.Status.REQUIRED_FAILURE)
                .action("NO_ACTION")
                .reason(reason)
                .build();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Applies the TASK_CLASSIFIER execute result only as a title-template correction.
     *
     * @param source diagnostic source label for logging; nullable.
     * @param localResult local tracker read result. It owns found state, OCR paths, green links, and
     *                    screen-absolute coordinates, and those fields are preserved unchanged.
     * @param cloudResult coordinator result returned by {@link TaskClassifierCloudShadowService}; nullable.
     * @param wubei true for 五倍 title-family mapping, false for 修罗 tracker confirmation.
     * @return local result, or an equivalent result with only {@code titleTemplate} replaced by an
     *         already-known local template when the execute gate fired and the cloud key is supported.
     */
    private TaskTrackerPanelReadResult applyTaskClassifierDecision(String source,
                                                                   TaskTrackerPanelReadResult localResult,
                                                                   CloudDecisionResult cloudResult,
                                                                   boolean wubei) {
        if (cloudResult != null && cloudResult.isRequiredExecuteFailure()) {
            log.error("[task-tracker classifier] cloud.required TASK_CLASSIFIER failed; return not-found: "
                            + "source={} family={} localFound={} localTaskKey={} reason={}",
                    source, wubei ? "wubei" : "xiuluo",
                    localResult != null && localResult.isFound(),
                    localResult == null || localResult.getTitleTemplate() == null
                            ? null
                            : localResult.getTitleTemplate().getTaskKey(),
                    cloudResult.getReason());
            return TaskTrackerPanelReadResult.empty();
        }
        if (localResult == null || !localResult.isFound()) {
            return localResult;
        }
        if (cloudResult == null || !cloudResult.isExecuted()) {
            return localResult;
        }

        String cloudTaskKey = cloudResult.getEffectiveDecision();
        TaskTrackerTitleTemplate cloudTemplate = wubei
                ? resolveWubeiCloudTitleTemplate(cloudTaskKey)
                : resolveXiuluoCloudTitleTemplate(cloudTaskKey);
        if (cloudTemplate == null) {
            log.warn("[task-tracker classifier] reject cloud task classifier decision: source={} family={} "
                            + "localTaskKey={} cloudTaskKey={} reason=unsupported-key cloud.required=no-local-fallback",
                    source, wubei ? "wubei" : "xiuluo",
                    localResult.getTitleTemplate() == null ? null : localResult.getTitleTemplate().getTaskKey(),
                    cloudTaskKey);
            return TaskTrackerPanelReadResult.empty();
        }
        if (localResult.getTitleTemplate() != null
                && cloudTemplate.getTaskKey().equals(localResult.getTitleTemplate().getTaskKey())) {
            return localResult;
        }

        log.info("[task-tracker classifier] apply cloud title template: source={} family={} localTaskKey={} "
                        + "cloudTaskKey={} title={}",
                source, wubei ? "wubei" : "xiuluo",
                localResult.getTitleTemplate() == null ? null : localResult.getTitleTemplate().getTaskKey(),
                cloudTaskKey, cloudTemplate.getDisplayName());
        return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(cloudTemplate)
                .detailRawPath(localResult.getDetailRawPath())
                .detailYellowPath(localResult.getDetailYellowPath())
                .detailAbsoluteLeft(localResult.getDetailAbsoluteLeft())
                .detailAbsoluteTop(localResult.getDetailAbsoluteTop())
                .yellowText(localResult.getYellowText())
                .greenLinks(localResult.getGreenLinks())
                .selectedGreenLink(localResult.getSelectedGreenLink())
                .greenBandWidth(localResult.getGreenBandWidth())
                .probeObjective(localResult.isProbeObjective())
                .sourceType(localResult.getSourceType())
                .build();
    }

    private TaskTrackerTitleTemplate resolveWubeiCloudTitleTemplate(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return null;
        }
        return WUBEI_TRACKER_TITLE_TEMPLATES.stream()
                .filter(template -> taskKey.equals(template.getTaskKey()))
                .findFirst()
                .orElse(null);
    }

    private TaskTrackerTitleTemplate resolveXiuluoCloudTitleTemplate(String taskKey) {
        return XIULUO_TASK_KEY_TRACKER.equals(taskKey) ? XIULUO_TRACKER_TITLE : null;
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
        return cropTaskDetailInTrackerPanel(source, templates, washTitleSource, true);
    }

    private TaskDetailCrop cropTaskDetailInTrackerPanel(String source,
                                                        List<TaskTrackerTitleTemplate> templates,
                                                        boolean washTitleSource,
                                                        boolean allowPanelReposition) {
        TitlePointMatch titleMatch = findTitlePoint(source, templates, washTitleSource, allowPanelReposition);
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
            int absoluteLeft = titleMatch.panelAbsoluteLeft() + left;
            int absoluteTop = titleMatch.panelAbsoluteTop() + top;
            OcrWindowRegion windowRegion = windowRegionFromAbsoluteRect(
                    source, "task-detail-crop", absoluteLeft, absoluteTop, width, height);
            log.info("[task-tracker] detail cropped: source={} panel={} titlePoint=({}, {}) crop=({}, {}) {}x{} windowRegion={} path={}",
                source, titleMatch.panelRawPath(), titlePoint.x, titlePoint.y, left, top, width, height,
                windowRegion == null ? null : windowRegion.toShortText(), detailPath);
            return new TaskDetailCrop(
                detailPath,
                absoluteLeft,
                absoluteTop,
                width,
                height,
                windowRegion,
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

            BufferedImage detailImage = copyImageRegion(panelImage, left, top, width, height);
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
        return findTitlePoint(source, templates, washTitleSource, true);
    }

    private TitlePointMatch findTitlePoint(String source,
                                           List<TaskTrackerTitleTemplate> templates,
                                           boolean washTitleSource,
                                           boolean allowPanelReposition) {
        TrackerPanelCapture panel = resolveTrackerPanelRect(source, allowPanelReposition);
        if (panel == null || panel.rawPath() == null || templates == null || templates.isEmpty()) {
            return null;
        }

        String matchPath = panel.rawPath();
        if (washTitleSource) {
            String safeSource = source == null ? "unknown" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
            matchPath = windowScopedTempPath.resolve("task_tracker_title_yellow_" + safeSource + ".png");
            if (!washYellowToPath(Path.of(panel.rawPath()), Path.of(matchPath), source, "task-tracker-title-yellow")) {
                return null;
            }
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

    private boolean washYellowToPath(Path rawPath, Path outputPath, String source, String phase) {
        ImageProcessorService.ImageProcessorResult result = imageProcessorService.washToPath(
                rawPath,
                outputPath,
                ImagePreprocessOperation.WASH_YELLOW,
                imageProcessorMetadata(source, phase, rawPath == null ? null : rawPath.toString(),
                        outputPath == null ? null : outputPath.getFileName().toString()));
        if (result.hasRequiredOutput()) {
            return true;
        }
        if (outputPath != null) {
            try {
                Files.deleteIfExists(outputPath);
            } catch (IOException ignored) {
                // Stale-output cleanup is best-effort; the caller still receives a fail-closed miss.
            }
        }
        log.warn("[task-tracker image] yellow preprocess unavailable: source={} phase={} raw={} output={} status={} reason={}",
                source, phase, rawPath, outputPath, result.status(), result.reason());
        return false;
    }

    private GreenTextScanInput resolveGreenTextScanInput(BufferedImage frame, String source, String phase) {
        if (frame == null) {
            return null;
        }
        ImageProcessorService.RequestMetadata metadata =
                imageProcessorMetadata(source, phase, null, "task-tracker-green");
        ImageProcessorService.ImageProcessorResult washResult =
                imageProcessorService.washGreenTextToBlackAndWhite(frame, metadata);
        if (!washResult.hasRequiredOutput()) {
            log.info("[task-tracker image] green mask unavailable: source={} phase={} status={} reason={}",
                    source, phase, washResult.status(), washResult.reason());
            return null;
        }

        BufferedImage greenMask = washResult.image();
        boolean handedOff = false;
        try {
            ImageProcessorService.ImageProcessorResult bandsResult =
                    imageProcessorService.findGreenTextBands(frame, metadata.toBuilder()
                            .phase(phase + "-bands")
                            .build());
            if (!bandsResult.hasRequiredOutput()) {
                log.info("[task-tracker image] green bands unavailable: source={} phase={} status={} reason={}",
                        source, phase, bandsResult.status(), bandsResult.reason());
                return null;
            }
            ImageProcessorService.ImageProcessorResult pickResult =
                    imageProcessorService.pickGreenTextBand(frame, true, metadata.toBuilder()
                            .phase(phase + "-pick")
                            .build());
            if (!pickResult.hasRequiredOutput()) {
                log.info("[task-tracker image] selected green band unavailable: source={} phase={} status={} reason={}",
                        source, phase, pickResult.status(), pickResult.reason());
                return null;
            }

            List<GreenTextBand> bands = new ArrayList<>();
            for (ImageProcessorService.GreenTextBand band : bandsResult.greenTextBands()) {
                GreenTextBand converted = toTrackerGreenBand(band);
                if (converted != null) {
                    bands.add(converted);
                }
            }
            GreenTextBand selected = toTrackerGreenBand(pickResult.selectedGreenTextBand());
            if (selected == null) {
                return null;
            }
            handedOff = true;
            return new GreenTextScanInput(greenMask, List.copyOf(bands), selected);
        } finally {
            if (!handedOff && greenMask != null) {
                greenMask.flush();
            }
        }
    }

    private GreenTextBand toTrackerGreenBand(ImageProcessorService.GreenTextBand band) {
        if (band == null || band.width() <= 0 || band.height() <= 0) {
            return null;
        }
        return new GreenTextBand(
                band.x(),
                band.y(),
                band.x() + band.width() - 1,
                band.y() + band.height() - 1,
                Math.max(0, band.pixels()));
    }

    private ImageProcessorService.RequestMetadata imageProcessorMetadata(
            String source,
            String phase,
            String rawImagePath,
            String debugImageId) {
        return ImageProcessorService.RequestMetadata.builder()
                .source(safeSource(source))
                .phase(phase)
                .rawImagePath(rawImagePath)
                .debugImageId(debugImageId)
                .build();
    }

    private static BufferedImage copyImageRegion(BufferedImage source, int x, int y, int width, int height) {
        if (source == null || width <= 0 || height <= 0
                || x < 0 || y < 0
                || x + width > source.getWidth()
                || y + height > source.getHeight()) {
            return null;
        }
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static boolean isBrightTextPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3 >= 128;
    }

    private TrackerPanelCapture resolveTrackerPanelRect(String source, boolean allowPanelReposition) {
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

        boolean anchorOutsideSafeArea = isTrackerPanelAnchorOutsideSafeArea(anchor);
        if (anchorOutsideSafeArea && !allowPanelReposition) {
            log.warn("[wubei] tracker panel anchor outside safe area but prepare is read-only; fail closed: source={} anchor=({}, {}) max=({}, {})",
                    source, anchor.x, anchor.y,
                    baseX + TRACKER_PANEL_ANCHOR_MAX_REL_X,
                    baseY + TRACKER_PANEL_ANCHOR_MAX_REL_Y);
            return null;
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

    /**
     * CR249: resolve the 五环 tracker pathing-link click point.
     *
     * <p>The title/anchor were already located by local raw-template matching (kept local by
     * design). This step is the wash-then-recognize half — green-link segmentation and
     * pathing-link selection — so it is owned by the cloud reader. A cloud miss is a miss, exactly
     * like 修罗/五倍; the local scan below only serves disabled/offline dev mode.</p>
     *
     * @param crop detail block already cropped by the local title match; sent to the cloud reader.
     * @param detailImage same detail block in memory, used only by the offline legacy scan.
     * @return screen-absolute click point, or null when neither cloud nor offline scan finds a link.
     */
    private Point findWuhuanTrackerGreenClickPoint(TaskDetailCrop crop,
                                                   BufferedImage detailImage,
                                                   String source) {
        Optional<TrackerPanelReaderCloudDecision> cloudDecision = readTrackerPanelImageFromCloud(
                "wuhuan", "wuhuan-tracker-detail", source, crop, 0, "FIRST_LINK");
        if (cloudDecision.isPresent()) {
            TrackerPanelReaderCloudDecision decision = cloudDecision.get();
            if (!decision.found() || decision.clickWindowRelative() == null) {
                log.info("[task-tracker wuhuan] cloud reader no pathing link: source={} status={} reason={}",
                        source, decision.getStatus(), decision.getReason());
                return null;
            }
            Point click = screenPointFromWindowRelative(decision.clickWindowRelative(), source, "wuhuan");
            log.info("[task-tracker wuhuan] cloud reader pathing link: source={} relative={} screen={}",
                    source, decision.clickWindowRelative(), click);
            return click;
        }
        if (trackerPanelReaderCloudDecisionService.isActive()) {
            log.info("[task-tracker wuhuan] cloud reader miss; no local green scan in production: source={}", source);
            return null;
        }
        return findWuhuanTrackerGreenClickPointLocallyLegacy(
                detailImage, crop.absoluteLeft(), crop.absoluteTop(), source);
    }

    /**
     * Legacy local 五环 green-link scan (wash-green + segment selection). CR249 moved production
     * recognition to the cloud reader; this stays only for disabled/offline dev mode.
     */
    @Deprecated(since = "CR249", forRemoval = false)
    private Point findWuhuanTrackerGreenClickPointLocallyLegacy(BufferedImage detailImage,
                                                                int absoluteLeft,
                                                                int absoluteTop,
                                                                String source) {
        TrackerGreenLinkScan scan = scanWuhuanTrackerGreenLinks(detailImage, absoluteLeft, absoluteTop, source);
        Optional<TrackerGreenLinkSegment> segment = findWuhuanPathingNameSegment(scan);
        return segment.map(this::resolveTrackerGreenClickPoint).orElse(null);
    }

    private String safeSource(String source) {
        return source == null || source.isBlank()
            ? "wubei"
            : source.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private List<TrackerGreenLinkSegment> splitWubeiTrackerGreenLinkSegments(BufferedImage frame,
                                                                             GreenTextBand band,
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

    /** CR249: only reachable from {@code findWuhuanTrackerGreenClickPointLocallyLegacy} (offline). */
    @Deprecated(since = "CR249", forRemoval = false)
    private TrackerGreenLinkScan scanWuhuanTrackerGreenLinks(BufferedImage frame,
                                                             int absoluteLeft,
                                                             int absoluteTop,
                                                             String source) {
        GreenTextScanInput greenText = resolveGreenTextScanInput(frame, source, "wuhuan-tracker-green-link");
        if (greenText == null) {
            log.info("[task-tracker wuhuan] green link scan: no green band");
            return TrackerGreenLinkScan.empty();
        }
        try {
            GreenTextBand band = greenText.band();
            List<TrackerGreenLinkSegment> segments = splitWuhuanTrackerGreenLinkSegments(
                greenText.greenMask(), band, absoluteLeft, absoluteTop);
            int bandWidth = band.maxX() - band.minX() + 1;
            log.info("[task-tracker wuhuan] green link scan: bands={} band=({}, {})-({}, {}) width={} segments={}",
                greenText.bands().size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
                absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, segments);
            return new TrackerGreenLinkScan(segments, bandWidth);
        } finally {
            greenText.flush();
        }
    }

    private XiuluoGreenLinkScan scanXiuluoTrackerGreenLinks(BufferedImage frame,
                                                            int absoluteLeft,
                                                            int absoluteTop,
                                                            String source) {
        GreenTextScanInput greenText = resolveGreenTextScanInput(frame, source, "xiuluo-tracker-green-link");
        if (greenText == null) {
            log.info("[task-tracker xiuluo] green link scan: no green band source={}", source);
            return XiuluoGreenLinkScan.empty();
        }

        try {
            GreenTextBand band = greenText.band();
            List<TrackerGreenLinkSegment> segments = splitWubeiTrackerGreenLinkSegments(
                greenText.greenMask(), band, absoluteLeft, absoluteTop)
                .stream()
                .filter(this::looksLikePathingLinkSegment)
                .sorted(Comparator.comparingInt(TrackerGreenLinkSegment::minY)
                    .thenComparingInt(TrackerGreenLinkSegment::minX))
                .toList();
            int bandWidth = band.maxX() - band.minX() + 1;
            if (segments.isEmpty()) {
                log.info("[task-tracker xiuluo] green link scan: no usable segment source={} bands={} band=({}, {})-({}, {}) width={}",
                    source, greenText.bands().size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
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
                source, greenText.bands().size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
                absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, link);
            return new XiuluoGreenLinkScan(List.of(link), bandWidth);
        } finally {
            greenText.flush();
        }
    }

    /*
     * 五环任务追踪的可点击目标是坐标数字后、进度 "[n/5]" 前的怪/NPC 名称。
     * 这里只负责选择绿色文字段，不负责截图、点击、放权或任务状态推进。
     */
    /**
     * CR249: segment-selection rules now live in the cloud reader
     * ({@code selectWuhuanPathingSegments}); this local copy serves the offline legacy scan only.
     */
    @Deprecated(since = "CR249", forRemoval = false)
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
                                                                              GreenTextBand band,
                                                                              int absoluteLeft,
                                                                              int absoluteTop) {
        List<TrackerGreenLinkSegment> targets = new ArrayList<>();
        for (GreenTextBand line : splitTrackerGreenLines(frame, band)) {
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

    private List<GreenTextBand> splitTrackerGreenLines(BufferedImage frame,
                                                       GreenTextBand band) {
        List<GreenTextBand> lines = new ArrayList<>();
        int startY = -1;
        int endY = -1;
        for (int y = band.minY(); y <= band.maxY(); y++) {
            int pixels = 0;
            for (int x = band.minX(); x <= band.maxX(); x++) {
                if (isBrightTextPixel(frame.getRGB(x, y))) {
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

    private GreenTextBand cropGreenBandToRows(BufferedImage frame,
                                              GreenTextBand band,
                                              int minY,
                                              int maxY) {
        int minX = Integer.MAX_VALUE;
        int maxX = -1;
        int pixels = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = band.minX(); x <= band.maxX(); x++) {
                if (isBrightTextPixel(frame.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    pixels++;
                }
            }
        }
        return new GreenTextBand(minX, minY, maxX, maxY, pixels);
    }

    private Optional<TrackerGreenLinkSegment> resolveTrackerTargetNameSegment(List<TrackerGreenGlyph> glyphs,
                                                                              GreenTextBand line,
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
                                                                         GreenTextBand line,
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
                                                              GreenTextBand band) {
        List<TrackerGreenGlyph> glyphs = new ArrayList<>();
        int startX = -1;
        int endX = -1;
        int pixels = 0;
        for (int x = band.minX(); x <= band.maxX(); x++) {
            int columnPixels = 0;
            for (int y = band.minY(); y <= band.maxY(); y++) {
                if (isBrightTextPixel(frame.getRGB(x, y))) {
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
                                   GreenTextBand band,
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

        BufferedImage validation = copyImageRegion(image, left, top, right - left, bottom - top);
        if (validation == null) {
            return Optional.empty();
        }
        BufferedImage washed = null;
        try {
            ImageProcessorService.ImageProcessorResult washResult = imageProcessorService.washGreenTextToBlackAndWhite(
                    validation,
                    imageProcessorMetadata(source, "task-tracker-prepared-green-wash", crop.path(),
                            targetKeyword + "-tracker-validation"));
            if (!washResult.hasRequiredOutput()) {
                log.info("[task-tracker] prepared action skipped: source={} target={} reason=green-wash-unavailable status={} cloudReason={}",
                        source, targetKeyword, washResult.status(), washResult.reason());
                return Optional.empty();
            }
            washed = washResult.image();
            ImageProcessorService.ImageProcessorResult fingerprintResult = imageProcessorService.buildBinaryFingerprint(
                    washed,
                    imageProcessorMetadata(source, "task-tracker-prepared-fingerprint", crop.path(),
                            targetKeyword + "-tracker-validation"));
            if (!fingerprintResult.hasRequiredOutput()) {
                log.info("[task-tracker] prepared action skipped: source={} target={} reason=fingerprint-unavailable status={} cloudReason={}",
                        source, targetKeyword, fingerprintResult.status(), fingerprintResult.reason());
                return Optional.empty();
            }
            String fingerprint = fingerprintResult.binaryFingerprint();
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
        if (!isTrackerPanelAnchorOutsideSafeArea(anchor)) {
            return false;
        }

        int maxX = tracker.getWindowBaseX() + TRACKER_PANEL_ANCHOR_MAX_REL_X;
        int maxY = tracker.getWindowBaseY() + TRACKER_PANEL_ANCHOR_MAX_REL_Y;
        int targetX = tracker.getWindowBaseX() + TRACKER_PANEL_DRAG_TARGET_REL_X;
        int targetY = tracker.getWindowBaseY() + TRACKER_PANEL_DRAG_TARGET_REL_Y;
        log.info("[wubei] tracker panel anchor outside safe area; drag source={} anchor=({}, {}) max=({}, {}) target=({}, {})",
            source, anchor.x, anchor.y, maxX, maxY, targetX, targetY);
        return inputSequences.submitAndWait("task-tracker:drag-panel:" + source, List.of(
            InputAction.dragAndDrop(anchor.x, anchor.y, targetX, targetY),
            InputAction.sleep(500)
        ));
    }

    private boolean isTrackerPanelAnchorOutsideSafeArea(Point anchor) {
        if (anchor == null) {
            return false;
        }
        int maxX = tracker.getWindowBaseX() + TRACKER_PANEL_ANCHOR_MAX_REL_X;
        int maxY = tracker.getWindowBaseY() + TRACKER_PANEL_ANCHOR_MAX_REL_Y;
        return anchor.x > maxX || anchor.y > maxY;
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
                                  int width,
                                  int height,
                                  OcrWindowRegion windowRegion,
                                  TaskTrackerTitleTemplate titleTemplate) {
        private TaskDetailCrop(String path,
                               int absoluteLeft,
                               int absoluteTop,
                               TaskTrackerTitleTemplate titleTemplate) {
            this(path, absoluteLeft, absoluteTop, 0, 0, null, titleTemplate);
        }
    }

    private record TrackerGreenLinkScan(List<TrackerGreenLinkSegment> segments, int bandWidth) {
        private static TrackerGreenLinkScan empty() {
            return new TrackerGreenLinkScan(List.of(), 0);
        }
    }

    private record GreenTextScanInput(BufferedImage greenMask,
                                      List<GreenTextBand> bands,
                                      GreenTextBand band) {
        private void flush() {
            if (greenMask != null) {
                greenMask.flush();
            }
        }
    }

    private record GreenTextBand(int minX, int minY, int maxX, int maxY, int pixels) {
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
