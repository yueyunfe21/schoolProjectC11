package com.bot.dhxy.capture;

import com.bot.dhxy.model.ocr.OcrWindowRegion;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Default masked-window geometry and image masking for full-window OCR fallback.
 *
 * <p>This is the pure, stateless subset of the {@code 696a12b0} DHXY vision scanner that the Cloud
 * turn path still needs: the default full-window source region, the fixed HUD/noise masks, and the
 * masking copy applied before yellow-name/OCR matching. The capture, learned-ROI acceleration and
 * temp-file behaviour of the original service stay on the DHXY side; this Cloud file therefore holds
 * no tracker, window context, capture, file, OCR or input dependency and constructs no instance.</p>
 */
public final class FixedOcrMaskPreprocessor {

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

    private FixedOcrMaskPreprocessor() {
    }

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
