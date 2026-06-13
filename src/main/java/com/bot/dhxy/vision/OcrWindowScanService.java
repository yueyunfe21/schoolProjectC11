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
    private static final OcrWindowRegion FULL_WINDOW_REGION =
            new OcrWindowRegion(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    private static final List<OcrWindowRegion> DEFAULT_MASKS = List.of(
            new OcrWindowRegion(0, 0, 258, 200),
            new OcrWindowRegion(0, 0, 1024, 54),
            new OcrWindowRegion(768, 58, 1020, 160),
            new OcrWindowRegion(4, 735, 706, 768),
            new OcrWindowRegion(710, 700, 1024, 768)
    );


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
}
