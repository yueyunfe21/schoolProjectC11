package com.bot.dhxy.vision;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;


import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Captures the bound game window for OCR with masking and optional learned ROI acceleration.
 *
 * <p>The service always captures the full 1024x768 client first so debug images remain
 * reproducible. It then masks stable HUD/noise regions, optionally tries a previously learned ROI
 * from {@link OcrRoiMemoryService}, and falls back to the full masked image when the ROI misses.
 * Returned OCR boxes are window-relative pixels.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrWindowScanService {

    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 768;
    private static final int SUMMARY_LIMIT = 12;
    private static final OcrWindowRegion FULL_WINDOW_REGION =
            new OcrWindowRegion(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    private static final List<OcrWindowRegion> DEFAULT_MASKS = List.of(
            new OcrWindowRegion(0, 0, 258, 200),
            new OcrWindowRegion(0, 0, 1024, 54),
            new OcrWindowRegion(768, 58, 1020, 160),
            new OcrWindowRegion(4, 735, 706, 768),
            new OcrWindowRegion(710, 700, 1024, 768)
    );

    private final GameClientTracker tracker;
    private final TextRecognizer textRecognizer;
    private final WindowScopedTempPath windowScopedTempPath;
    private final OcrRoiMemoryService roiMemoryService;

    /**
     * Return the default full-window source region for masked OCR fallback.
     *
     * <p>The region intentionally represents the source capture before masking. Callers that receive
     * this region from recommendation code must apply {@link #copyWithDefaultMasks(BufferedImage)}
     * before yellow-name/OCR matching so HUD, chat, and other stable noise are hidden.</p>
     *
     * @return full game-client source region in window-relative pixels.
     */
    public static OcrWindowRegion defaultMaskedWindowRegion() {
        return FULL_WINDOW_REGION;
    }

    /**
     * Check whether a region is the default masked-window fallback source.
     *
     * @param region candidate window-relative OCR region; nullable.
     * @return true when the region equals the full source window that must be masked before scanning.
     */
    public static boolean isDefaultMaskedWindowRegion(OcrWindowRegion region) {
        return FULL_WINDOW_REGION.equals(region);
    }

    /**
     * Copy an image and apply the same default masks used by masked full-window OCR.
     *
     * @param source captured full game-window image; ownership stays with the caller.
     * @return masked copy, or null when source is null. The caller owns and must flush the copy.
     */
    public static BufferedImage copyWithDefaultMasks(BufferedImage source) {
        if (source == null) {
            return null;
        }
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        applyMasks(copy, DEFAULT_MASKS);
        return copy;
    }

    /**
     * Run a masked full-window OCR scan without memory lookup.
     *
     * @param purpose diagnostic label used in debug filenames and logs.
     * @param localOnly true to force the local OCR provider; false to use configured provider routing.
     * @return OCR scan result with debug image paths and window-relative word boxes.
     */
    public WindowOcrScanResult scanMaskedWindow(String purpose, boolean localOnly) {
        return scanMaskedWindowWithMemory(null, purpose, null, localOnly);
    }

    /**
     * Run masked window OCR with learned-ROI first pass and full-window fallback.
     *
     * @param memoryKey stable key for ROI memory, such as {@code npc-yellow-window:<name>}; null
     *                  disables memory lookup and recording.
     * @param purpose diagnostic label for logs/debug image names.
     * @param targetText expected text used to decide whether ROI/full scans matched; blank means any
     *                   OCR word counts as a match.
     * @param localOnly true to force local OCR; false to use configured provider routing.
     * @return result containing OCR mode, images, optional ROI, and window-relative OCR words.
     */
    public WindowOcrScanResult scanMaskedWindowWithMemory(String memoryKey,
                                                          String purpose,
                                                          String targetText,
                                                          boolean localOnly) {
        String safePurpose = safeFileName(purpose == null ? "window" : purpose);
        String rawPath = windowScopedTempPath.resolve("ocr_" + safePurpose + "_raw.png");
        String maskedPath = windowScopedTempPath.resolve("ocr_" + safePurpose + "_masked.png");
        String overlayPath = windowScopedTempPath.resolve("ocr_" + safePurpose + "_mask_overlay.png");
        String provider = providerLabel(localOnly);

        if (!tracker.refreshWindowState()) {
            roiMemoryService.recordOcrAttempt(memoryKey, purpose, "WINDOW_BASE_UNAVAILABLE",
                    null, targetText, List.of(), false, "window base unavailable", WINDOW_WIDTH, WINDOW_HEIGHT,
                    provider, "window-base-unavailable", rawPath, maskedPath, overlayPath, null);
            return WindowOcrScanResult.failed("window base unavailable", rawPath, maskedPath, overlayPath);
        }
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        boolean captured = tracker.captureToFile("ocr-window-" + safePurpose, rawPath,
                baseX, baseY, baseX + WINDOW_WIDTH, baseY + WINDOW_HEIGHT);
        if (!captured) {
            roiMemoryService.recordOcrAttempt(memoryKey, purpose, "CAPTURE_FAILED",
                    null, targetText, List.of(), false, "window capture failed", WINDOW_WIDTH, WINDOW_HEIGHT,
                    provider, "capture-failed", rawPath, maskedPath, overlayPath, null);
            return WindowOcrScanResult.failed("window capture failed", rawPath, maskedPath, overlayPath);
        }

        BufferedImage masked = null;
        try {
            masked = ImageIO.read(Path.of(rawPath).toFile());
            if (masked == null) {
                roiMemoryService.recordOcrAttempt(memoryKey, purpose, "CAPTURE_UNREADABLE",
                        null, targetText, List.of(), false, "captured image unreadable", WINDOW_WIDTH, WINDOW_HEIGHT,
                        provider, "capture-unreadable", rawPath, maskedPath, overlayPath, null);
                return WindowOcrScanResult.failed("captured image unreadable", rawPath, maskedPath, overlayPath);
            }
            // Write an overlay before masking so reviewers can see exactly which pixels were hidden
            // and which learned ROI was attempted.
            Optional<OcrWindowRegion> learnedRoi = roiMemoryService.recommendedRoi(memoryKey);
            writeMaskOverlay(masked, DEFAULT_MASKS, learnedRoi.orElse(null), Path.of(overlayPath));
            applyMasks(masked, DEFAULT_MASKS);
            writeImage(masked, Path.of(maskedPath));

            // Learned ROI is only an acceleration path. A miss never ends the scan; it falls through
            // to the full masked window below.
            if (learnedRoi.isPresent()) {
                OcrWindowRegion roi = learnedRoi.get().clamp(masked.getWidth(), masked.getHeight());
                if (roi.isValid()) {
                    String roiPath = windowScopedTempPath.resolve("ocr_" + safePurpose + "_roi.png");
                    List<OcrWordResult> roiWords = readRoiWords(masked, roi, roiPath, localOnly);
                    boolean roiMatched = targetText == null || targetText.isBlank()
                            ? !roiWords.isEmpty()
                            : containsTarget(roiWords, targetText);
                    roiMemoryService.recordOcrAttempt(memoryKey, purpose, "LEARNED_ROI",
                            roi, targetText, roiWords, roiMatched,
                            roiMatched ? "matched learned ROI" : "learned ROI missed target",
                            masked.getWidth(), masked.getHeight(),
                            provider, "masked-window-learned-roi", rawPath, maskedPath, overlayPath, roiPath);
                    if (roiMatched) {
                        log.info("[ocr-window] roi scan used: purpose={} key={} roi={} words={} target={}",
                                purpose, memoryKey, roi.toShortText(), roiWords.size(), normalizeText(targetText));
                        return WindowOcrScanResult.success("ROI", rawPath, maskedPath, overlayPath, roiPath, roi, roiWords);
                    }
                    log.info("[ocr-window] roi scan miss, fallback full: purpose={} key={} roi={} words={} target={}",
                            purpose, memoryKey, roi.toShortText(), roiWords.size(), normalizeText(targetText));
                }
            }

            // Full masked-window fallback is the correctness path. It also records enough evidence
            // for the memory layer to recompute future ROI recommendations.
            List<OcrWordResult> words = localOnly
                    ? textRecognizer.getAllTextResultsLocalOnly(maskedPath)
                    : textRecognizer.getAllTextResults(maskedPath);
            boolean fullMatched = targetText == null || targetText.isBlank()
                    ? !words.isEmpty()
                    : containsTarget(words, targetText);
            roiMemoryService.recordOcrAttempt(memoryKey, purpose, "FULL_MASKED",
                    new OcrWindowRegion(0, 0, masked.getWidth(), masked.getHeight()),
                    targetText, words, fullMatched,
                    fullMatched ? "matched full masked window" : "full masked window missed target",
                    masked.getWidth(), masked.getHeight(),
                    provider, "masked-window-full", rawPath, maskedPath, overlayPath, null);
            log.info("[ocr-window] full masked scan complete: purpose={} key={} localOnly={} words={} text={}",
                    purpose, memoryKey, localOnly, words.size(), summarize(words));
            return WindowOcrScanResult.success("FULL", rawPath, maskedPath, overlayPath, null, null, words);
        } catch (Exception e) {
            roiMemoryService.recordOcrAttempt(memoryKey, purpose, "SCAN_EXCEPTION",
                    null, targetText, List.of(), false, e.getMessage(), WINDOW_WIDTH, WINDOW_HEIGHT,
                    provider, "scan-exception", rawPath, maskedPath, overlayPath, null);
            log.warn("[ocr-window] scan failed: purpose={} reason={}", purpose, e.getMessage(), e);
            return WindowOcrScanResult.failed(e.getMessage(), rawPath, maskedPath, overlayPath);
        } finally {
            if (masked != null) {
                masked.flush();
            }
        }
    }

    private List<OcrWordResult> readRoiWords(BufferedImage masked,
                                                            OcrWindowRegion roi,
                                                            String roiPath,
                                                            boolean localOnly) throws Exception {
        BufferedImage cropped = masked.getSubimage(roi.x1(), roi.y1(), roi.width(), roi.height());
        BufferedImage copy = new BufferedImage(cropped.getWidth(), cropped.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(cropped, 0, 0, null);
            writeImage(copy, Path.of(roiPath));
        } finally {
            graphics.dispose();
            copy.flush();
        }
        List<OcrWordResult> rawWords = localOnly
                ? textRecognizer.getAllTextResultsLocalOnly(roiPath)
                : textRecognizer.getAllTextResults(roiPath);
        return shiftWords(rawWords, roi.x1(), roi.y1());
    }

    private List<OcrWordResult> shiftWords(List<OcrWordResult> words, int dx, int dy) {
        if (words == null || words.isEmpty()) {
            return List.of();
        }
        List<OcrWordResult> shifted = new ArrayList<>();
        for (OcrWordResult word : words) {
            if (word == null) {
                continue;
            }
            shifted.add(new OcrWordResult(
                    word.getText(),
                    word.getX() + dx,
                    word.getY() + dy,
                    word.getLeft() + dx,
                    word.getTop() + dy,
                    word.getWidth(),
                    word.getHeight(),
                    word.getScore()
            ));
        }
        return shifted;
    }

    private void writeMaskOverlay(BufferedImage source,
                                  List<OcrWindowRegion> masks,
                                  OcrWindowRegion roi,
                                  Path overlayPath) {
        BufferedImage overlay = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = overlay.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
            graphics.setStroke(new BasicStroke(3));
            graphics.setColor(new Color(255, 80, 80, 80));
            for (OcrWindowRegion mask : masks) {
                OcrWindowRegion region = mask.clamp(source.getWidth(), source.getHeight());
                if (region.isValid()) {
                    graphics.fillRect(region.x1(), region.y1(), region.width(), region.height());
                }
            }
            graphics.setColor(new Color(220, 0, 0));
            for (OcrWindowRegion mask : masks) {
                OcrWindowRegion region = mask.clamp(source.getWidth(), source.getHeight());
                if (region.isValid()) {
                    graphics.drawRect(region.x1(), region.y1(), region.width(), region.height());
                }
            }
            if (roi != null) {
                OcrWindowRegion region = roi.clamp(source.getWidth(), source.getHeight());
                if (region.isValid()) {
                    graphics.setColor(new Color(40, 120, 255, 80));
                    graphics.fillRect(region.x1(), region.y1(), region.width(), region.height());
                    graphics.setColor(new Color(20, 80, 230));
                    graphics.drawRect(region.x1(), region.y1(), region.width(), region.height());
                }
            }
            writeImage(overlay, overlayPath);
        } catch (Exception e) {
            log.warn("[ocr-window] write mask overlay failed: path={} reason={}",
                    overlayPath, e.getMessage(), e);
        } finally {
            graphics.dispose();
            overlay.flush();
        }
    }

    private static void applyMasks(BufferedImage image, List<OcrWindowRegion> masks) {
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            for (OcrWindowRegion mask : masks) {
                OcrWindowRegion region = mask.clamp(image.getWidth(), image.getHeight());
                if (region.isValid()) {
                    graphics.fillRect(region.x1(), region.y1(), region.width(), region.height());
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    private void writeImage(BufferedImage image, Path path) throws Exception {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(image, "png", path.toFile());
    }

    private boolean containsTarget(List<OcrWordResult> words, String targetText) {
        String target = normalizeText(targetText);
        if (target.isBlank()) {
            return false;
        }
        for (OcrWordResult word : words) {
            String text = normalizeText(word == null ? null : word.getText());
            if (!text.isBlank() && (text.contains(target) || target.contains(text) && text.length() >= 2)) {
                return true;
            }
        }
        return false;
    }

    private String summarize(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "-";
        }
        return words.stream()
                .limit(SUMMARY_LIMIT)
                .map(word -> word == null ? "null" : word.getText() + "@(" + word.getLeft() + "," + word.getTop()
                        + "," + word.getWidth() + "x" + word.getHeight() + ")")
                .collect(Collectors.joining(" | "));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private String safeFileName(String value) {
        String safe = value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return safe.isBlank() ? "window" : safe;
    }

    private String providerLabel(boolean localOnly) {
        if (localOnly) {
            return "local-only";
        }
        String configured = textRecognizer.currentProviderName();
        if (configured == null || configured.isBlank()) {
            return "unknown";
        }
        if ("compare".equalsIgnoreCase(configured)) {
            return "compare-returned-baidu";
        }
        if ("hybrid".equalsIgnoreCase(configured)) {
            return "hybrid-routed";
        }
        return configured.trim();
    }

    @Value


    @Builder


    @AllArgsConstructor(access = AccessLevel.PUBLIC)


    @Accessors(fluent = true)


    public static class WindowOcrScanResult {


        boolean success;


        String mode;


        String rawPath;


        String maskedPath;


        String overlayPath;


        String roiPath;


        OcrWindowRegion roi;


        List<OcrWordResult> words;


        String message;


        /**
         * Build a successful scan result.
         *
         * @param mode scan path that produced the words, for example {@code ROI} or {@code FULL}.
         * @param rawPath full raw window capture path.
         * @param maskedPath masked full-window image path.
         * @param overlayPath visual overlay showing masks and optional ROI.
         * @param roiPath cropped ROI image path when ROI mode was attempted.
         * @param roi window-relative ROI used for OCR, or null for full-window mode.
         * @param words OCR words in window-relative coordinates.
         * @return immutable scan result.
         */
        public static WindowOcrScanResult success(String mode,
                                                  String rawPath,
                                                  String maskedPath,
                                                  String overlayPath,
                                                  String roiPath,
                                                  OcrWindowRegion roi,
                                                  List<OcrWordResult> words) {
            return new WindowOcrScanResult(true, mode, rawPath, maskedPath, overlayPath, roiPath, roi,
                    words == null ? List.of() : List.copyOf(words), "");
        }

        /**
         * Build a failed scan result.
         *
         * @param message failure reason for logs/UI.
         * @param rawPath raw image path that may help debug partial failures.
         * @param maskedPath masked image path, possibly not written.
         * @param overlayPath overlay path, possibly not written.
         * @return failed result with no OCR words.
         */
        public static WindowOcrScanResult failed(String message, String rawPath, String maskedPath, String overlayPath) {
            return new WindowOcrScanResult(false, "FAILED", rawPath, maskedPath, overlayPath, null, null, List.of(),
                    message == null ? "" : message);
        }

        /**
         * @return compact single-line diagnostic summary for business logs.
         */
        public String summary() {
            String roiText = roi == null ? "-" : roi.toShortText();
            return "mode=" + mode
                    + " success=" + success
                    + " words=" + (words == null ? 0 : words.size())
                    + " roi=" + roiText
                    + " raw=" + rawPath
                    + " masked=" + maskedPath
                    + " overlay=" + overlayPath
                    + " roiPath=" + (roiPath == null ? "-" : roiPath)
                    + " message=" + message;
        }
    


    }
}
