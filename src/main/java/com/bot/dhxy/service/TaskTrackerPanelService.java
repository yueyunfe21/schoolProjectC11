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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private static final String WUBEI_TASK_KEY_SANCANG_FENGMO = "wubei.sancang_fengmo";
    private static final String WUBEI_TASK_KEY_BAOXIANG_MIQING = "wubei.baoxiang_miqing";
    private static final String WUBEI_TASK_KEY_DIANQIAN_XIANYI = "wubei.dianqian_xianyi";
    private static final String WUBEI_TASK_KEY_ZHIDOU_HUANGPAO = "wubei.zhidou_huangpao";
    private static final String WUBEI_TASK_KEY_KUIXING_GUIWEI = "wubei.kuixing_guiwei";
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
    private static final int WUHUAN_TITLE_CENTER_FALLBACK_LEFT_SHIFT = 24;
    private static final int TRACKER_LINK_MIN_PIXELS = 20;
    private static final int TRACKER_LINK_SPLIT_GAP = 8;
    private static final int TRACKER_LINK_DELIMITER_MAX_WIDTH = 5;
    private static final int TRACKER_LINK_DELIMITER_MAX_PIXELS = 18;
    private static final int TRACKER_COORD_GLYPH_MAX_WIDTH = 5;
    private static final int TRACKER_COORD_GLYPH_MIN_RUN = 5;
    private static final double TRACKER_ANCHOR_THRESHOLD = 0.82;
    private static final int WUBEI_TRACKER_LINK_SINGLE_MAX_WIDTH = 72;
    private static final List<TaskTrackerTitleTemplate> WUBEI_TRACKER_TITLE_TEMPLATES = List.of(
        trackerTitleTemplate(WUBEI_TASK_KEY_SANCANG_FENGMO, "三藏封魔", "images/template/wubei/wubei_title_sancang_fengmo.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_BAOXIANG_MIQING, "宝象谜情", "images/template/wubei/wubei_title_baoxiang_miqing.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_DIANQIAN_XIANYI, "殿前献艺", "images/template/wubei/wubei_title_dianqian_xianyi.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_ZHIDOU_HUANGPAO, "智斗黄袍", "images/template/wubei/wubei_title_zhidou_huangpao.png"),
        trackerTitleTemplate(WUBEI_TASK_KEY_KUIXING_GUIWEI, "魁星归位", "images/template/wubei/wubei_title_kuixing_guiwei.png")
    );

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;
    private final InputSequences inputSequences;

    private static TaskTrackerTitleTemplate trackerTitleTemplate(String taskKey, String displayName, String templatePath) {
        return TaskTrackerTitleTemplate.builder()
            .taskKey(taskKey)
            .displayName(displayName)
            .templatePath(templatePath)
            .threshold(0.82)
            .build();
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
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, WUBEI_TRACKER_TITLE_TEMPLATES);
        if (crop == null || crop.path() == null || crop.path().isBlank()) {
            return TaskTrackerPanelReadResult.empty();
        }

        String safeSource = source == null ? "wubei" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
        String yellowPath = windowScopedTempPath.resolve("task_tracker_detail_yellow_" + safeSource + ".png");
        ImagePreprocessor.washYellowText(crop.path(), yellowPath);
        List<OcrWordResult> words = textRecognizer.getAllTextResultsForMatch(
            yellowPath,
            "wubei-tracker-yellow:" + safeSource,
            result -> !result.isEmpty());
        String yellowText = words.stream().map(OcrWordResult::getText).collect(java.util.stream.Collectors.joining("|"));

        try {
            BufferedImage image = ImageIO.read(new File(crop.path()));
            if (image == null) {
                log.warn("[task-tracker wubei] detail image unreadable: source={} path={}", source, crop.path());
                return TaskTrackerPanelReadResult.empty();
            }
            WubeiGreenLinkScan scan = scanWubeiTrackerGreenLinks(image, crop.absoluteLeft(), crop.absoluteTop());
            log.info("[task-tracker wubei] panel read: source={} title={} yellow='{}' probe={} links={} detail={} yellowPath={}",
                source,
                crop.titleTemplate() == null ? null : crop.titleTemplate().getDisplayName(),
                yellowText, scan.isProbeObjective(), scan.links(), crop.path(), yellowPath);
            return TaskTrackerPanelReadResult.builder()
                .found(true)
                .titleTemplate(crop.titleTemplate())
                .detailRawPath(crop.path())
                .detailYellowPath(yellowPath)
                .yellowText(yellowText)
                .greenLinks(scan.links())
                .greenBandWidth(scan.bandWidth())
                .probeObjective(scan.isProbeObjective())
                .build();
        } catch (IOException e) {
            log.warn("[task-tracker wubei] failed to read detail image: source={} path={}", source, crop.path(), e);
            return TaskTrackerPanelReadResult.empty();
        }
    }

    public String getCroppedTaskDetailInTrackerPanel(String source, String template) {
        TaskDetailCrop crop = cropTaskDetailInTrackerPanel(source, template);
        return crop == null ? null : crop.path();
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
        TitlePointMatch titleMatch = findTitlePoint(source, templates);
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
            int height = Math.min(WUHUAN_TRACKER_BLOCK_HEIGHT, panelImage.getHeight() - top);
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
        TrackerPanelCapture panel = resolveTrackerPanelRect(source);
        if (panel == null || panel.rawPath() == null || templates == null || templates.isEmpty()) {
            return null;
        }

        String safeSource = source == null ? "unknown" : source.replaceAll("[^a-zA-Z0-9._-]", "_");
        String yellowPath = windowScopedTempPath.resolve("task_tracker_title_yellow_" + safeSource + ".png");
        ImagePreprocessor.washYellowText(panel.rawPath(), yellowPath);

        for (TaskTrackerTitleTemplate template : templates) {
            if (template == null || template.getTemplatePath() == null || template.getTemplatePath().isBlank()) {
                continue;
            }
            double[] match = ImageFinder.find(yellowPath, template.getTemplatePath(), template.getThreshold());
            if (match == null || match.length < 2) {
                log.info("[task-tracker] title template not matched: source={} template={} panel={} yellow={}",
                    source, template.getTemplatePath(), panel.rawPath(), yellowPath);
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
            return new TitlePointMatch(panel.rawPath(), yellowPath, panel.absoluteLeft(), panel.absoluteTop(),
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
                    anchor = new Point((int) Math.round(result[0]), (int) Math.round(result[1]));
                    searchMode = "expanded";
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

    private Point findWuhuanTrackerGreenClickPoint(BufferedImage detailImage, int absoluteLeft, int absoluteTop) {
        TrackerGreenLinkScan scan = scanWuhuanTrackerGreenLinks(detailImage, absoluteLeft, absoluteTop);
        Optional<TrackerGreenLinkSegment> segment = findWuhuanPathingNameSegment(scan);
        return segment.map(this::resolveTrackerGreenClickPoint).orElse(null);
    }

    private WubeiGreenLinkScan scanWubeiTrackerGreenLinks(BufferedImage frame, int absoluteLeft, int absoluteTop) {
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
        List<TaskTrackerGreenLink> links = segments.stream()
            .map(segment -> TaskTrackerGreenLink.builder()
                .minX(segment.minX())
                .minY(segment.minY())
                .maxX(segment.maxX())
                .maxY(segment.maxY())
                .pixels(segment.pixels())
                .build())
            .toList();
        log.info("[task-tracker wubei] green link scan: bands={} band=({}, {})-({}, {}) width={} links={} probe={}",
            bands.size(), absoluteLeft + band.minX(), absoluteTop + band.minY(),
            absoluteLeft + band.maxX(), absoluteTop + band.maxY(), bandWidth, links, probe);
        return new WubeiGreenLinkScan(links, bandWidth, probe);
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
