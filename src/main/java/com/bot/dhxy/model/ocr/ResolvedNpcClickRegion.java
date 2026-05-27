package com.bot.dhxy.model.ocr;

/**
 * NPC click visual work region resolved from window-relative OCR memory to screen-absolute pixels.
 *
 * <p>The persisted {@link OcrWindowRegion} remains window-relative so it survives moving the game
 * client. Callers receive this resolved form so screenshot/template/OCR code can use the
 * screen-absolute rectangle directly without recalculating the current window base.</p>
 *
 * @param windowRegion source window-relative region in the 1024x768 client content area.
 * @param windowBaseX current game-window screen-absolute X origin.
 * @param windowBaseY current game-window screen-absolute Y origin.
 * @param screenX1 left edge in screen-absolute pixels.
 * @param screenY1 top edge in screen-absolute pixels.
 * @param screenX2 right edge in screen-absolute pixels, exclusive.
 * @param screenY2 bottom edge in screen-absolute pixels, exclusive.
 */
public record ResolvedNpcClickRegion(OcrWindowRegion windowRegion,
                                     int windowBaseX,
                                     int windowBaseY,
                                     int screenX1,
                                     int screenY1,
                                     int screenX2,
                                     int screenY2) {
    /**
     * Resolve a window-relative OCR region against the current bound window base.
     *
     * @param region window-relative OCR/click scan region.
     * @param windowBaseX current game-window screen-absolute X origin.
     * @param windowBaseY current game-window screen-absolute Y origin.
     * @return resolved region with both window-relative and screen-absolute coordinates.
     */
    public static ResolvedNpcClickRegion from(OcrWindowRegion region, int windowBaseX, int windowBaseY) {
        return new ResolvedNpcClickRegion(
                region,
                windowBaseX,
                windowBaseY,
                windowBaseX + region.x1(),
                windowBaseY + region.y1(),
                windowBaseX + region.x2(),
                windowBaseY + region.y2());
    }

    /**
     * @return screen-absolute rectangle as {@code [x1, y1, x2, y2]}.
     */
    public int[] screenRect() {
        return new int[]{screenX1, screenY1, screenX2, screenY2};
    }

    /**
     * @return compact diagnostic text with both relative and absolute bounds.
     */
    public String toShortText() {
        return windowRegion.toShortText()
                + " abs=[" + screenX1 + "," + screenY1 + "," + screenX2 + "," + screenY2 + "]";
    }
}
