package com.bot.dhxy.window.observation;

import java.awt.image.BufferedImage;

/**
 * Read-only structural presence probe for the game's wide dialog panel.
 *
 * <p>This deliberately answers only whether a complete neutral dialog frame is visible in the
 * supplied maximum dialog ROI. It never interprets text, chooses an option, or emits input. The
 * new-player tracker retry uses this fact solely to stop re-clicking a green link after that link
 * has already opened a dialog; Cloud remains responsible for dialog content recognition.</p>
 */
public final class DialogFramePresenceMechanics {

    private static final double MIN_FRAME_RUN_RATIO = 0.65D;
    private static final int MAX_NEUTRAL_CHANNEL_DELTA = 18;
    private static final int MIN_NEUTRAL_BRIGHTNESS = 25;
    private static final int MAX_NEUTRAL_BRIGHTNESS = 145;
    private static final int MIN_PANEL_HEIGHT = 80;
    private static final int MAX_PANEL_HEIGHT = 260;

    /**
     * Tests whether the raw maximum dialog ROI contains both long, overlapping panel borders.
     *
     * @param dialogRoi raw window-relative dialog ROI in image-local pixels; may be {@code null}
     * @return {@code true} only when a complete dialog frame is structurally visible
     */
    public boolean isPresent(BufferedImage dialogRoi) {
        if (dialogRoi == null || dialogRoi.getWidth() <= 0 || dialogRoi.getHeight() <= 0) {
            return false;
        }
        int requiredRun = (int) Math.ceil(dialogRoi.getWidth() * MIN_FRAME_RUN_RATIO);
        FrameEdge firstCandidate = null;
        for (int y = 0; y < dialogRoi.getHeight(); y++) {
            int runStart = -1;
            for (int x = 0; x <= dialogRoi.getWidth(); x++) {
                boolean neutral = x < dialogRoi.getWidth() && isNeutralFramePixel(dialogRoi.getRGB(x, y));
                if (neutral && runStart < 0) {
                    runStart = x;
                }
                if (!neutral && runStart >= 0) {
                    FrameEdge candidate = new FrameEdge(runStart, x, y);
                    runStart = -1;
                    if (candidate.width() < requiredRun) {
                        continue;
                    }
                    if (firstCandidate == null) {
                        firstCandidate = candidate;
                        continue;
                    }
                    if (isMatchingBottomEdge(firstCandidate, candidate)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isMatchingBottomEdge(FrameEdge top, FrameEdge bottom) {
        int height = bottom.y() - top.y();
        if (height < MIN_PANEL_HEIGHT || height > MAX_PANEL_HEIGHT) {
            return false;
        }
        int overlap = Math.min(top.endX(), bottom.endX()) - Math.max(top.startX(), bottom.startX());
        int requiredOverlap = (int) Math.ceil(Math.min(top.width(), bottom.width()) * 0.85D);
        return overlap >= requiredOverlap;
    }

    private static boolean isNeutralFramePixel(int rgb) {
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int brightest = Math.max(red, Math.max(green, blue));
        int darkest = Math.min(red, Math.min(green, blue));
        return brightest - darkest <= MAX_NEUTRAL_CHANNEL_DELTA
                && brightest >= MIN_NEUTRAL_BRIGHTNESS
                && brightest <= MAX_NEUTRAL_BRIGHTNESS;
    }

    private record FrameEdge(int startX, int endX, int y) {
        private int width() {
            return endX - startX;
        }
    }
}
