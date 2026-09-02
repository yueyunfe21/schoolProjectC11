package com.bot.dhxy.service;


import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.runner.stop.TaskSleep;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles non-task UI interruptions for the currently bound game window.
 *
 * <p>This service is intentionally conservative: it may close world-map/generic windows and click
 * known leave/cancel style options. Mouse clicks are serialized through {@link InputSequences};
 * methods ending in {@code Direct} are only used while already inside an exclusive input section.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UICleanerService {
    private static final int MAP_POPUP_RECT_X_OFFSET = 278;
    private static final int MAP_POPUP_RECT_Y_OFFSET = 595;
    private static final int MAP_POPUP_RECT_WIDTH = 406;
    private static final int MAP_POPUP_RECT_HEIGHT = 137;
    private static final Path GENERIC_CLOSE_TEMPLATE_DIR = Path.of("images/template/cancel");


    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final WindowScopedTempPath windowScopedTempPath;
    private final com.bot.dhxy.window.runtime.WindowTaskContextHolder windowTaskContextHolder;

    private final Random random = new Random();

    /*
     * 2026-08-26 00:39 用户令(灵兽村宠物面板挡接任务案):无关闭钮的展示面板(宠物属性/装备
     * 查看等)模板清障关不掉,任务卡死时上层每 ~30 秒来一轮清障、每轮都 not-found(实测 15 分钟
     * 25 次)。这类面板点击面板任意处即关,故:同一窗口在时间窗内连续多轮"清障后仍未恢复"
     * (又被请求清障且又什么都没关掉)时,fallback 点一次客户区中心——面板在时点击被面板吃掉
     * =关闭;真干净时误点游戏世界的代价(走两步/点开NPC)由后续流程纠正,且仅在高置信卡死时发生。
     */
    private static final Path CENTER_TAP_EVIDENCE_DIR =
            Path.of("images", "temp", "match-evidence", "center-tap-fallback");
    private static final DateTimeFormatter CENTER_TAP_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");


    /**
     * Run the broad cleanup used before/after generic task transitions.
     *
     * <p>Side effects: may press Alt+1, click dialog options, and click generic close buttons in the
     * current game window. Do not use this immediately after opening a business NPC dialog unless the
     * caller has first confirmed the dialog is safe to close.</p>
     */
    public void cleanUpAll() {
        log.info("UI cleanup started");
        boolean cleanedAny = false;
        CleanupPass cleanupPass = CleanupPass.start();

        if (isWorldMapOpened(cleanupPass)) {
            cleanedAny = closeMapWindow(cleanupPass) || cleanedAny;
        }

        if (closeAllGenericWindows(cleanupPass)) {
            cleanedAny = true;
        }

        log.info(cleanedAny ? "UI cleanup finished" : "UI already clean");
    }

    /**
     * Probe whether startup cleanup would be a no-op without clicking or sending hotkeys.
     *
     * <p>五环多窗口启动会在后台先做这次截图/对话框探测。只有这里确认没有地图窗口、
     * 没有业务对话框、也没有通用关闭按钮时，前台 PREPARE 才能跳过重扫；任何未知或
     * 可疑状态都保留给原来的 {@link #cleanUpAll()} 前台兜底处理。</p>
     *
     * @param source diagnostic source for logs.
     * @return true only when cleanup is clearly unnecessary.
     */
    private boolean isWorldMapOpened(CleanupPass cleanupPass) {
        String screenPath = cleanupPass.screenPath(tracker);
        if (screenPath == null || screenPath.isBlank()) {
            return false;
        }
        if (coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                "images/template/map/world_map_title.png", screenPath, 0.8) != null) {
            return true;
        }

        int[] rect = coordinateHelper.getScaledRect(MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
        return findImageInCachedRegion(screenPath, "images/template/map/checkbox_checked.png", rect, 0.95) != null
                || findImageInCachedRegion(screenPath, "images/template/map/checkbox_unchecked.png", rect, 0.95) != null;
    }

    /**
     * Reads whether a world/local-map overlay is visible in an already captured exact-window frame.
     *
     * @param frame full window-relative frame; ownership stays with the caller.
     * @param source diagnostic source only; this probe never sends input.
     * @return {@code TRUE} when the map title or map checkbox is visible, {@code FALSE} after a
     *         complete scan, or {@code null} when the frame/template evidence is unavailable.
     */
    public Boolean probeMapWindowPresent(BufferedImage frame, String source) {
        if (frame == null) {
            return null;
        }
        try {
            BufferedImage title = ImageIO.read(new File("images/template/map/world_map_title.png"));
            if (title == null || title.getWidth() > frame.getWidth() || title.getHeight() > frame.getHeight()) {
                if (title != null) {
                    title.flush();
                }
                return null;
            }
            try {
                double[] worldTitleMatch = ImageFinder.find(frame, title, 0.8);
                MatchEvidenceStore.saveOnChange("ui-map-probe-title", null, frame, title, worldTitleMatch);
                if (worldTitleMatch != null) {
                    log.info("UI map read-only probe present: source={} evidence=world-map-title", source);
                    return true;
                }
            } finally {
                title.flush();
            }

            int[] absoluteRect = coordinateHelper.getScaledRect(
                    MAP_POPUP_RECT_X_OFFSET, MAP_POPUP_RECT_Y_OFFSET,
                    MAP_POPUP_RECT_WIDTH, MAP_POPUP_RECT_HEIGHT);
            int[] frameRect = new int[]{
                    tracker.getWindowBaseX(), tracker.getWindowBaseY(),
                    tracker.getWindowBaseX() + frame.getWidth(),
                    tracker.getWindowBaseY() + frame.getHeight()
            };
            BufferedImage mapFooter = ImagePreprocessor.cropAbsoluteRect(frame, frameRect, absoluteRect);
            if (mapFooter == null) {
                return null;
            }
            try {
                for (String templatePath : List.of(
                        "images/template/map/checkbox_checked.png",
                        "images/template/map/checkbox_unchecked.png")) {
                    BufferedImage checkbox = ImageIO.read(new File(templatePath));
                    if (checkbox == null || checkbox.getWidth() > mapFooter.getWidth()
                            || checkbox.getHeight() > mapFooter.getHeight()) {
                        if (checkbox != null) {
                            checkbox.flush();
                        }
                        return null;
                    }
                    try {
                        double[] checkboxMatch = ImageFinder.find(mapFooter, checkbox, 0.95);
                        MatchEvidenceStore.saveOnChange("ui-map-probe-checkbox", null, mapFooter, checkbox, checkboxMatch);
                        if (checkboxMatch != null) {
                            log.info("UI map read-only probe present: source={} evidence={}", source, templatePath);
                            return true;
                        }
                    } finally {
                        checkbox.flush();
                    }
                }
            } finally {
                mapFooter.flush();
            }
            return false;
        } catch (IOException | RuntimeException failure) {
            log.warn("UI map read-only probe unavailable: source={} cause={}",
                    source, failure.toString());
            return null;
        }
    }

    /**
     * Closes only a currently visible map overlay and leaves business dialogs/generic windows alone.
     *
     * @param source diagnostic source written to the cleanup log.
     * @return true when a visible map was closed; false when no map was visible or input failed.
     */
    public boolean closeMapIfPresent(String source) {
        CleanupPass cleanupPass = CleanupPass.start();
        if (!isWorldMapOpened(cleanupPass)) {
            return false;
        }
        boolean closed = closeMapWindow(cleanupPass);
        log.info("UI map-only cleanup finished: source={} closed={}", source, closed);
        return closed;
    }

    private Point findImageInCachedRegion(String screenPath, String templatePath, int[] rect, double matchRate) {
        try {
            BufferedImage frame = ImageIO.read(new File(screenPath));
            if (frame == null) {
                return null;
            }
            int[] frameRect = new int[]{
                    tracker.getWindowBaseX(),
                    tracker.getWindowBaseY(),
                    tracker.getWindowBaseX() + frame.getWidth(),
                    tracker.getWindowBaseY() + frame.getHeight()
            };
            BufferedImage crop = ImagePreprocessor.cropAbsoluteRect(frame, frameRect, rect);
            if (crop == null) {
                return null;
            }
            String roiPath = windowScopedTempPath.resolve("ui_cleanup_cached_roi_scan.png");
            ImageIO.write(crop, "png", new File(roiPath));
            double[] result = ImageFinder.find(roiPath, templatePath, matchRate);
            MatchEvidenceStore.saveOnChange("ui-cleanup-roi-scan", null,
                    crop, ImageIO.read(new File(templatePath)), result);
            return coordinateHelper.resolveMatchedPointInRect(rect, result);
        } catch (Exception e) {
            log.warn("UI cleanup cached ROI match failed: template={} rect=({}, {})-({}, {}) reason={}",
                    templatePath, rect[0], rect[1], rect[2], rect[3], e.getMessage(), e);
            return null;
        }
    }

    private boolean closeMapWindow(CleanupPass cleanupPass) {
        boolean submitted = inputSequences.submitAndWait("uiCleanup:closeMapAlt1", List.of(
                InputAction.pressAlt1(),
                InputAction.sleep(500)
        ));
        cleanupPass.invalidateFrame("map window closed");
        if (submitted && !isWorldMapOpened(cleanupPass)) {
            return true;
        }

        if (submitted) {
            log.warn("UI cleanup pressed Alt+1 but map still appears open; falling back to close button");
        }
        return clickCloseButtonOnce("uiCleanup:closeMapButton", cleanupPass);
    }

    /**
     * Close generic X-button windows without processing business dialogs.
     *
     * @return true when at least one close button was clicked; false when no generic close button was
     * found or the thread was interrupted.
     */
    public boolean closeAllGenericWindows() {
        return closeAllGenericWindows(CleanupPass.start());
    }

    /**
     * Reads whether any generic close template is visible without sending input.
     *
     * @param source diagnostic source only; the probe never clicks, closes, or sends a hotkey
     * @return {@code TRUE} when any template matches, {@code FALSE} after a complete same-frame
     *         scan, or {@code null} when capture/template/matching evidence is unavailable
     */
    public Boolean probeGenericCloseButtonPresent(String source) {
        CleanupPass cleanupPass = CleanupPass.start();
        String screenPath = cleanupPass.screenPath(tracker);
        if (screenPath == null || screenPath.isBlank()) {
            log.warn("UI generic-close read-only probe unavailable: source={} cause=capture-failed", source);
            return null;
        }
        BufferedImage frame = null;
        try {
            frame = ImageIO.read(new File(screenPath));
            if (frame == null) {
                return null;
            }
            return probeGenericCloseButtonPresent(frame, source);
        } catch (IOException | RuntimeException failure) {
            log.warn("UI generic-close read-only probe unavailable: source={} cause={}",
                    source, failure.toString());
            return null;
        } finally {
            if (frame != null) {
                frame.flush();
            }
        }
    }

    /**
     * Same read-only generic-close scan against a caller-supplied in-memory window frame — no
     * capture, no file IO except template loads. Used by the observation cycle's precomputed
     * scene-presence cache so the arrival moment never pays recognition latency.
     */
    public Boolean probeGenericCloseButtonPresent(BufferedImage frame, String source) {
        if (frame == null) {
            return null;
        }
        List<String> templates = genericCloseButtonTemplates();
        if (templates.isEmpty()) {
            log.warn("UI generic-close read-only probe unavailable: source={} cause=no-templates", source);
            return null;
        }
        try {
            for (String templatePath : templates) {
                BufferedImage template = ImageIO.read(new File(templatePath));
                if (template == null || template.getWidth() > frame.getWidth()
                        || template.getHeight() > frame.getHeight()) {
                    if (template != null) {
                        template.flush();
                    }
                    log.warn("UI generic-close read-only probe unavailable: source={} template={} cause=invalid-template",
                            source, templatePath);
                    return null;
                }
                try {
                    double[] genericProbeMatch = ImageFinder.find(frame, template, 0.8);
                    MatchEvidenceStore.saveOnChange("ui-generic-close-probe", null, frame, template, genericProbeMatch);
                    if (genericProbeMatch != null) {
                        log.info("UI generic-close read-only probe present: source={} template={}", source, templatePath);
                        return true;
                    }
                } finally {
                    template.flush();
                }
            }
            log.debug("UI generic-close read-only probe absent: source={} templates={}", source, templates.size());
            return false;
        } catch (IOException | RuntimeException failure) {
            log.warn("UI generic-close read-only probe unavailable: source={} cause={}",
                    source, failure.toString());
            return null;
        }
    }

    private boolean closeAllGenericWindows(CleanupPass cleanupPass) {
        boolean closedAny = false;
        if (isWorldMapOpened(cleanupPass)) {
            closedAny = closeMapWindow(cleanupPass) || closedAny;
        }
        for (int i = 0; i < 3; i++) {
            if (!clickCloseButtonOnce("uiCleanup:closeGenericWindow", cleanupPass)) {
                break;
            }
            closedAny = true;
            cleanupPass.invalidateFrame("generic window closed");
        }
        return closedAny;
    }

    /** Dark-fraction probe over the client-center region a closable overlay panel would cover. */
    private boolean isDarkCenterOverlayPresent() {
        int left = tracker.getWindowBaseX() + 292;
        int top = tracker.getWindowBaseY() + 214;
        java.awt.image.BufferedImage region = tracker.captureToMemory(
                "uiCleanup:center-overlay-probe", left, top, left + 440, top + 340);
        if (region == null) {
            return false;
        }
        try {
            long dark = 0;
            long total = 0;
            for (int y = 0; y < region.getHeight(); y += 2) {
                for (int x = 0; x < region.getWidth(); x += 2) {
                    int rgb = region.getRGB(x, y);
                    int lum = (int) (0.299 * ((rgb >>> 16) & 0xFF)
                            + 0.587 * ((rgb >>> 8) & 0xFF) + 0.114 * (rgb & 0xFF));
                    if (lum <= 85) {
                        dark++;
                    }
                    total++;
                }
            }
            double fraction = total == 0 ? 0 : (double) dark / total;
            log.info("UI cleanup center-overlay probe: darkFraction={}", String.format("%.2f", fraction));
            return fraction >= 0.55;
        } finally {
            region.flush();
        }
    }

    private static String windowKeyFromScreenPath(String screenPath) {
        if (screenPath == null || screenPath.isBlank()) {
            return null;
        }
        for (String part : screenPath.replace(java.io.File.separatorChar, '/').split("/")) {
            if (part.startsWith("hwnd-")) {
                return part;
            }
        }
        return null;
    }

    /**
     * 显式的"点一次客户区中心"兜底：关掉一块没有关闭钮、模板清障关不掉的展示面板
     * （宠物属性/装备查看等——点面板任意处即关，2026-08-26 00:39 灵兽村案）。
     *
     * <p>2026-08-26 12:35 教训（用户抓获：五个挂机窗战斗中被 focus+点击、任务剧情框被越权
     * 点掉）：此兜底**绝不自动触发**——清障内部的"没关掉东西"计数分不清卡死遮挡/战斗画面/
     * 任务正要处理的对话框。只允许任务侧在**自己的多轮识别失败**重试链上显式调用，且内部
     * 仍有两道门：①战斗中不点；②客户区中央必须真有一块暗色大面板（在位检测），否则拒点。</p>
     *
     * @param source 调用方诊断标签（任务码+失败上下文）
     * @return true 仅当真的提交了中心点击
     */
    public boolean tapClientCenterToDismissOverlay(String source) {
        String windowKey = source == null ? "unknown"
                : source.replaceAll("[^A-Za-z0-9._-]", "_");
        String screenPath = tracker.getLatestVisionPath();
        if (windowTaskContextHolder.rawCurrent()
                .map(context -> context.isLocalCombatVisible()).orElse(false)) {
            log.info("UI cleanup center-tap refused: local combat visible source={}", source);
            return false;
        }
        if (!isDarkCenterOverlayPresent()) {
            log.info("UI cleanup center-tap refused: no dark overlay at client center source={} "
                    + "(screen is genuinely clean)", source);
            return false;
        }
        try {
            Files.createDirectories(CENTER_TAP_EVIDENCE_DIR);
            Files.copy(Path.of(Objects.requireNonNull(screenPath, "no latest vision frame")),
                    CENTER_TAP_EVIDENCE_DIR.resolve(
                            CENTER_TAP_STAMP.format(LocalDateTime.now()) + "_" + windowKey + ".png"));
        } catch (Exception evidenceFailure) {
            log.warn("UI cleanup center-tap evidence save failed: window={} reason={}",
                    windowKey, evidenceFailure.getMessage());
        }
        int clickX = tracker.getWindowBaseX() + 512 - 20 + random.nextInt(41);
        int clickY = tracker.getWindowBaseY() + 384 - 20 + random.nextInt(41);
        log.warn("UI cleanup center-tap fallback: explicit task-side request source={} "
                        + "click=({}, {}) — dismissing a closable overlay that has no close-button template",
                source, clickX, clickY);
        return inputSequences.submitExclusiveAndWait("uiCleanup:centerTapFallback", () -> {
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.clickLeft(clickX, clickY, 80);
            return TaskSleep.sleep(250) && InputActionScope.checkpoint();
        });
    }

    /**
     * Handle lightweight interruptions while an idle/member window is allowed to stay mostly quiet.
     *
     * @param sourceTask diagnostic task name written to logs.
     * @return true when a known business option or generic close button was handled; false when there
     * was nothing actionable or cleanup was interrupted.
     */
    public boolean cleanLightweightInterruptions(String sourceTask) {
        if (closeAllGenericWindows()) {
            log.info("UI lightweight cleanup closed generic window: source={}", sourceTask);
            return true;
        }
        return false;
    }

    private boolean clickCloseButtonOnce(String description, CleanupPass cleanupPass) {
        GenericCloseHit hit = findGenericCloseButtonPoint(description, cleanupPass);
        if (hit == null) {
            return false;
        }
        return inputSequences.submitExclusiveAndWait(description, () -> clickCloseButtonOnceDirect(description, hit));
    }

    /**
     * Close the world-map search/input overlay by clicking only the {@code x2.png} close button.
     *
     * <p>This is intentionally narrower than {@link #closeAllGenericWindows()}: route navigation calls
     * it immediately after clicking a world-map route result, where using other generic close-button
     * templates could close an unrelated panel or dialog. The method sends direct mouse input and must
     * therefore only be called by code that already owns the input worker's exclusive callback.</p>
     *
     * @param description diagnostic source written to input and cleanup logs.
     * @return true when the {@code x2.png} button was found and clicked; false when it was not visible
     *         or the capture failed.
     */
    public boolean closeMapSearchInputByX2Direct(String description) {
        return closeMapSearchInputByX2Direct(description, true);
    }

    /**
     * Close the world-map search/input overlay with optional post-click settling.
     *
     * @param description diagnostic source written to input and cleanup logs.
     * @param settleAfterClick true to retain the normal 250ms post-click settle; false when the
     *                         caller has declared this physical click to be its final foreground action.
     * @return true when the {@code x2.png} button was found and clicked.
     */
    public boolean closeMapSearchInputByX2Direct(String description, boolean settleAfterClick) {
        if (!tracker.updateGlobalVision()) {
            log.warn("UI cleanup x2-only close skipped: capture failed description={}", description);
            return false;
        }
        String screenPath = tracker.getLatestVisionPath();
        Point closeBtnPoint = coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                "images/template/cancel/x2.png", screenPath, 0.8);
        if (closeBtnPoint == null) {
            log.info("UI cleanup x2-only close skipped: x2 not found description={}", description);
            return false;
        }

        int clickX = closeBtnPoint.x + 4 + random.nextInt(5);
        int clickY = closeBtnPoint.y + 4 + random.nextInt(5);
        log.info("UI cleanup x2-only close matched: description={} template={} click=({}, {})",
                description, "images/template/cancel/x2.png", clickX, clickY);
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(clickX, clickY, 80);
        if (!settleAfterClick) {
            return true;
        }
        return TaskSleep.sleep(250) && InputActionScope.checkpoint();
    }

    /**
     * G117 attributability: a generic-close hit is the pair (which template matched, where it matched).
     *
     * <p>Incident 3519 could not be attributed from the logs because the click line carried only the
     * coordinate: 34 {@code close button matched} lines existed and not one of them named the template
     * that produced them, so the root cause (a stray {@code quxiao.png} in the scan directory) had to be
     * recovered by hand from evidence PNGs. Carrying the template all the way to the click log makes
     * this class of accident greppable and countable.</p>
     */
    private record GenericCloseHit(String templatePath, Point point) {
    }

    private GenericCloseHit findGenericCloseButtonPoint(String description, CleanupPass cleanupPass) {
        String screenPath = cleanupPass.screenPath(tracker);
        if (screenPath == null || screenPath.isBlank()) {
            log.warn("UI cleanup close button scan skipped: capture failed description={}", description);
            return null;
        }
        List<String> closeButtonTemplates = genericCloseButtonTemplates();
        for (String templatePath : closeButtonTemplates) {
            Point closeBtnPoint =
                    coordinateHelper.findImageAbsoluteCoordinateByImagePath(templatePath, screenPath, 0.8);
            if (closeBtnPoint != null) {
                return new GenericCloseHit(templatePath, closeBtnPoint);
            }
        }

        log.info("UI cleanup close button not found: description={} screenPath={} templates={}",
                description, screenPath, closeButtonTemplates);
        return null;
    }

    /**
     * G126: generic cleanup closes every X-button skin before considering {@code quxiao.png}.
     *
     * <p>Incident 3519 (2026-08-29 00:34) was an ordering defect: alphabetical discovery put
     * {@code quxiao.png} ahead of every {@code x*.png}, so cleanup cancelled a task while a
     * top-right X was available. Keep the cancellation template as the final fallback, but never
     * derive its priority from directory order. {@code npc_busy_cancel.png} remains excluded because
     * it is not the user-approved generic fallback.</p>
     */
    static List<String> genericCloseButtonTemplates() {
        try (var paths = Files.list(GENERIC_CLOSE_TEMPLATE_DIR)) {
            List<String> templates = new ArrayList<>(paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().matches("x\\d*\\.png"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(Path::toString)
                    .toList());
            Path cancelFallback = GENERIC_CLOSE_TEMPLATE_DIR.resolve("quxiao.png");
            if (Files.isRegularFile(cancelFallback)) {
                templates.add(cancelFallback.toString());
            }
            return List.copyOf(templates);
        } catch (IOException directoryUnavailable) {
            log.warn("UI cleanup generic close-template directory is unavailable: dir={} message={}",
                    GENERIC_CLOSE_TEMPLATE_DIR, directoryUnavailable.getMessage());
            return List.of();
        }
    }

    private boolean clickCloseButtonOnceDirect(String description, GenericCloseHit hit) {
        Point closeBtnPoint = hit.point();
        int clickX = closeBtnPoint.x + 4 + random.nextInt(5);
        int clickY = closeBtnPoint.y + 4 + random.nextInt(5);
        log.info("UI cleanup close button matched: description={} template={} click=({}, {})",
                description, hit.templatePath(), clickX, clickY);

        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(clickX, clickY, 80);
        return TaskSleep.sleep(250) && InputActionScope.checkpoint();
    }

    private static class CleanupPass {
        private String screenPath;

        static CleanupPass start() {
            return new CleanupPass();
        }

        String screenPath(GameClientTracker tracker) {
            if (screenPath == null || screenPath.isBlank()) {
                if (!tracker.updateGlobalVision()) {
                    return null;
                }
                screenPath = tracker.getLatestVisionPath();
            }
            return screenPath;
        }

        void invalidateFrame(String reason) {
            screenPath = null;
        }
    }

}
