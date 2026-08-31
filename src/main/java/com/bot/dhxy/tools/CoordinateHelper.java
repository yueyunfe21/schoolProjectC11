package com.bot.dhxy.tools;


import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 坐标与矩阵计算核心大脑。
 */
@Component
@Slf4j
public class CoordinateHelper {

    private final GameClientTracker tracker;
    private final WindowScopedTempPath windowScopedTempPath;
    private final Random random = new Random();

    public CoordinateHelper(@Lazy GameClientTracker tracker,
                            WindowScopedTempPath windowScopedTempPath) {
        this.tracker = tracker;
        this.windowScopedTempPath = windowScopedTempPath;
    }

    /** 本程序只支持 100% 显示缩放；其他比例下点击落点与截图区域都不成立。 */
    private static final double SUPPORTED_SCALE_RATIO = 1.0D;
    private static final double SCALE_RATIO_EPSILON = 1e-6D;
    /**
     * 两次确认之间的间隔。JVM 刚起来那一瞬 AWT 的显示环境可能还没稳：同一台 100% 缩放的
     * 机器先后读出过 1.0 / 1.25 / 1.5，而错值会被缓存整个进程生命周期。
     */
    private static final long SCALE_PROBE_GAP_MS = 500L;
    /** 连续两次读数一致才采信；最多试这么多轮。 */
    private static final int SCALE_PROBE_MAX_ATTEMPTS = 5;
    /** Windows 下 96 DPI 即 100% 缩放。 */
    private static final int UNSCALED_SCREEN_DPI = 96;

    private double systemScaleRatio = SUPPORTED_SCALE_RATIO;

    @PostConstruct
    public void initScaleRatio() {
        if (GraphicsEnvironment.isHeadless()) {
            this.systemScaleRatio = SUPPORTED_SCALE_RATIO;
            log.info("System scale ratio: headless environment, use {}", this.systemScaleRatio);
            return;
        }
        double confirmed = confirmScaleRatio();
        int screenDpi = probeScreenDpi();
        if (Math.abs(confirmed - SUPPORTED_SCALE_RATIO) > SCALE_RATIO_EPSILON) {
            throw new IllegalStateException(unsupportedScaleMessage(confirmed, screenDpi));
        }
        this.systemScaleRatio = SUPPORTED_SCALE_RATIO;
        log.info("System scale ratio confirmed: {} screenDpi={}", this.systemScaleRatio, screenDpi);
    }

    /**
     * 连续两次读到相同值才算数。单次采样会把启动瞬间的错值钉死一整轮运行，
     * 而 {@code @PostConstruct} 只跑一次，之后没有任何自愈机会。
     */
    private double confirmScaleRatio() {
        double previous = probeScaleRatio();
        for (int attempt = 2; attempt <= SCALE_PROBE_MAX_ATTEMPTS; attempt++) {
            sleepBetweenScaleProbes();
            double current = probeScaleRatio();
            if (Math.abs(current - previous) <= SCALE_RATIO_EPSILON) {
                log.info("System scale ratio double-confirmed: value={} attempts={}", current, attempt);
                return current;
            }
            log.warn("System scale ratio unstable between probes: previous={} current={} attempt={}/{}",
                    previous, current, attempt, SCALE_PROBE_MAX_ATTEMPTS);
            previous = current;
        }
        throw new IllegalStateException(
                "显示缩放读数始终不稳定：连续 " + SCALE_PROBE_MAX_ATTEMPTS
                        + " 次采样没有两次一致，最后一次=" + previous
                        + "。请重启客户端；若反复出现，检查启动期是否有显示器热插拔或分辨率切换。");
    }

    private double probeScaleRatio() {
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        AffineTransform transform = config.getDefaultTransform();
        return transform.getScaleX();
    }

    private int probeScreenDpi() {
        try {
            return Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (RuntimeException ex) {
            log.warn("Screen DPI probe failed; report as unknown: {}", ex.toString());
            return -1;
        }
    }

    private void sleepBetweenScaleProbes() {
        try {
            Thread.sleep(SCALE_PROBE_GAP_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("显示缩放二次确认被中断", interrupted);
        }
    }

    /**
     * 把两种截然不同的成因分开告诉使用者：真的设了缩放（screenDpi 也不是 96），
     * 还是 JVM 读错了（screenDpi=96 却测出非 1.0，两者自相矛盾）。
     */
    private String unsupportedScaleMessage(double detected, int screenDpi) {
        String head = "不支持的显示缩放：检测到 " + detected
                + "（约 " + Math.round(detected * 100) + "%），本程序只支持 100%。screenDpi="
                + screenDpi + "（96 = 100%）。";
        if (screenDpi == UNSCALED_SCREEN_DPI) {
            return head + " screenDpi 说是 100%、缩放比却不是 1.0，两者矛盾，"
                    + "这是 JVM 启动瞬间的异常读数——直接重启客户端即可。";
        }
        return head + " 请把 Windows 显示设置的缩放改回 100% 后重启客户端。";
    }

    public Point getRandomizedPoint(int baseX, int baseY, int maxRadiusX, int maxRadiusY) {
        int offsetX = maxRadiusX <= 0 ? 0 : random.nextInt(maxRadiusX * 2 + 1) - maxRadiusX;
        int offsetY = maxRadiusY <= 0 ? 0 : random.nextInt(maxRadiusY * 2 + 1) - maxRadiusY;
        return new Point(baseX + offsetX, baseY + offsetY);
    }

    public Point getRandomizedPoint(Point base, int maxRadiusX, int maxRadiusY) {
        if (base == null) return null;
        return getRandomizedPoint(base.x, base.y, maxRadiusX, maxRadiusY);
    }

    /**
     * Apply an asymmetric random offset to a screen-absolute point.
     *
     * @param base screen-absolute base point. Null returns null.
     * @param minOffsetX minimum X offset in screen pixels.
     * @param maxOffsetX maximum X offset in screen pixels.
     * @param randomRadiusY symmetric Y random radius in screen pixels. Values less than or equal to
     *                      zero keep Y unchanged.
     * @return randomized screen-absolute point.
     */
    public Point getRandomizedPoint(Point base, int minOffsetX, int maxOffsetX, int randomRadiusY) {
        if (base == null) {
            return null;
        }
        int lowX = Math.min(minOffsetX, maxOffsetX);
        int highX = Math.max(minOffsetX, maxOffsetX);
        int offsetX = lowX == highX ? lowX : random.nextInt(highX - lowX + 1) + lowX;
        int offsetY = randomRadiusY <= 0 ? 0 : random.nextInt(randomRadiusY * 2 + 1) - randomRadiusY;
        return new Point(base.x + offsetX, base.y + offsetY);
    }

    public double getScaleRatio() {
        return this.systemScaleRatio;
    }

    public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
        tracker.refreshWindowState();
        int xStart = tracker.getWindowBaseX() + offsetX;
        int yStart = tracker.getWindowBaseY() + offsetY;
        int xEnd = xStart + width;
        int yEnd = yStart + height;
        return new int[]{xStart, yStart, xEnd, yEnd};
    }

    public Point findImageAbsoluteCoordinate(String templatePath, double matchRate) {
        tracker.updateGlobalVision();
        String screenPath = tracker.getLatestVisionPath();

        double[] result = ImageFinder.find(screenPath, templatePath, matchRate);
        // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
        saveMatchEvidenceFromPaths("coordinate-global-vision", screenPath, templatePath, result);

        if (result != null && result.length >= 2) {
            tracker.refreshWindowState();
            int absoluteX = (int) Math.round(result[0] / systemScaleRatio) + tracker.getWindowBaseX();
            int absoluteY = (int) Math.round(result[1] / systemScaleRatio) + tracker.getWindowBaseY();
            log.info("Image matched [{}] at absolute coordinate ({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }

        return null;
    }

    /**
     * 取证辅助（不改判定）：交给 {@link MatchEvidenceStore#saveOnChangeLazy}——先做节流判定，
     * 确定要落盘了才把原图/模板从磁盘解码回内存；读图失败时工具类内部静默跳过。
     *
     * <p>2026-08-21 性能返修：旧实现无条件先做两次磁盘读+PNG 解码（其中原帧就是 ImageFinder
     * 刚读过的同一个文件，同一张全窗图解码两遍），节流早退的承诺被解码成本架空，还偶发
     * 边写边读的 IIOException。</p>
     */
    private static void saveMatchEvidenceFromPaths(
            String site, String framePath, String templatePath, double[] thresholdMatch) {
        MatchEvidenceStore.saveOnChangeLazy(
                site,
                null,
                () -> ImagePreprocessor.pathToBufferedImage(framePath),
                () -> ImagePreprocessor.pathToBufferedImage(templatePath),
                thresholdMatch);
    }

    public Point findImageAbsoluteCoordinateByImagePath(String templatePath, String screenPath, double matchRate) {
        double[] result = ImageFinder.find(screenPath, templatePath, matchRate);
        saveMatchEvidenceFromPaths("coordinate-vision-by-path", screenPath, templatePath, result);

        if (result != null && result.length >= 2) {
            tracker.refreshWindowState();
            int absoluteX = (int) Math.round(result[0] / systemScaleRatio) + tracker.getWindowBaseX();
            int absoluteY = (int) Math.round(result[1] / systemScaleRatio) + tracker.getWindowBaseY();
            log.info("Image matched [{}] at absolute coordinate ({},{})", templatePath, absoluteX, absoluteY);
            return new Point(absoluteX, absoluteY);
        }

        return null;
    }

    public int[] getAbsoluteRectByAnchor(Point anchor, int offsetX, int offsetY, int width, int height) {
        if (anchor == null) return null;
        int startX = anchor.x + offsetX;
        int startY = anchor.y + offsetY;
        int endX = startX + width;
        int endY = startY + height;
        return new int[]{startX, startY, endX, endY};
    }

    /**
     * Convert an image-template match inside a captured rectangle into a screen-absolute point.
     *
     * @param rect screen-absolute capture rectangle as {@code [left, top, right, bottom]}.
     * @param matchResult result returned by {@link ImageFinder#find(String, String, double)}. The
     *                    first two values are image-local X/Y inside {@code rect}.
     * @return screen-absolute point, or null when the inputs do not contain a usable match.
     */
    public Point resolveMatchedPointInRect(int[] rect, double[] matchResult) {
        if (rect == null || rect.length < 2 || matchResult == null || matchResult.length < 2) {
            return null;
        }
        int absoluteX = rect[0] + (int) Math.round(matchResult[0]);
        int absoluteY = rect[1] + (int) Math.round(matchResult[1]);
        return new Point(absoluteX, absoluteY);
    }

    public Point findImageInRegion(String templatePath, int[] rect, double matchRate) {
        String roiPath = windowScopedTempPath.resolve("roi_scan.png");
        if (!tracker.captureToFile("ROI-Scan", roiPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }

        long start = LatencyMetrics.start();
        double[] result;
        try {
            result = ImageFinder.find(roiPath, templatePath, matchRate);
        } catch (Throwable e) {
            log.warn("[latency] event=coordinate.findImageInRegion elapsedMs={} detail={}",
                    LatencyMetrics.elapsedMs(start),
                    "result=exception template=" + templatePath
                            + " roi=" + roiPath
                            + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                            + " matchRate=" + matchRate
                            + " error=" + e.getClass().getName() + ":" + e.getMessage());
            log.warn("findImageInRegion ImageFinder failed: template={} roi={} rect=({}, {})-({}, {}) matchRate={}",
                    templatePath, roiPath, rect[0], rect[1], rect[2], rect[3], matchRate, e);
            throw e;
        }
        saveMatchEvidenceFromPaths("coordinate-region-scan", roiPath, templatePath, result);

        if (result != null && result.length >= 2) {
            Point absolute = resolveMatchedPointInRect(rect, result);
            log.debug("[latency] event=coordinate.findImageInRegion elapsedMs={} detail={}",
                    LatencyMetrics.elapsedMs(start),
                    "result=matched template=" + templatePath
                            + " roi=" + roiPath
                            + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                            + " point=" + absolute.x + "," + absolute.y);
            log.debug("Region image matched [{}] at ({},{})", templatePath, absolute.x, absolute.y);
            return absolute;
        }

        log.debug("[latency] event=coordinate.findImageInRegion elapsedMs={} detail={}",
                LatencyMetrics.elapsedMs(start),
                "result=miss template=" + templatePath
                        + " roi=" + roiPath
                        + " rect=" + rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3]
                        + " matchRate=" + matchRate);
        return null;
    }

    public Point findGreenTextInRegion(String templatePath, int[] rect, double matchRate) {
        String rawScanPath = windowScopedTempPath.resolve("tem_dialog_cut.png");

        if (!tracker.captureToFile("临时截图处理黑白", rawScanPath, rect[0], rect[1], rect[2], rect[3])) {
            return null;
        }
        String washedScanPath = windowScopedTempPath.resolve("tem_dialog_cut_washed.png");
        BufferedImage raw = ImagePreprocessor.pathToBufferedImage(rawScanPath);
        if (raw == null) {
            return null;
        }
        BufferedImage washed = ImagePreprocessor.washGreenTextToBlackAndWhite(raw);
        raw.flush();
        if (washed == null || !ImagePreprocessor.saveImage(washed, washedScanPath)) {
            if (washed != null) {
                washed.flush();
            }
            log.info("Green text wash missed [{}]", templatePath);
            return null;
        }
        washed.flush();

        double[] result = ImageFinder.find(washedScanPath, templatePath, matchRate);
        saveMatchEvidenceFromPaths("coordinate-green-text", washedScanPath, templatePath, result);
        if (result != null && result.length >= 2) {
            Point absolute = resolveMatchedPointInRect(rect, result);
            log.info("Green text matched [{}] at ({},{})", templatePath, absolute.x, absolute.y);
            return absolute;
        }
        return null;
    }
}
