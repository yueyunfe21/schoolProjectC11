package com.bot.dhxy.model.ocr;

/**
 * Immutable rectangular OCR region in game-window-relative pixels.
 *
 * <p>The coordinate system is the 1024x768 client content area: x grows to the right, y grows down,
 * and the right/bottom edges are exclusive. Callers convert this region to screen-absolute pixels by
 * adding the current window base from the bound {@code GameClientTracker}.</p>
 *
 * @param x1 left edge in window-relative pixels.
 * @param y1 top edge in window-relative pixels.
 * @param x2 right edge in window-relative pixels, exclusive.
 * @param y2 bottom edge in window-relative pixels, exclusive.
 */
public record OcrWindowRegion(int x1, int y1, int x2, int y2) {

    /**
     * Clamp this region to an image/window size.
     *
     * @param imageWidth maximum width in pixels.
     * @param imageHeight maximum height in pixels.
     * @return a region whose edges are ordered and constrained to {@code [0,width]} and {@code [0,height]}.
     */
    public OcrWindowRegion clamp(int imageWidth, int imageHeight) {
        int left = clampValue(Math.min(x1, x2), 0, Math.max(0, imageWidth));
        int right = clampValue(Math.max(x1, x2), 0, Math.max(0, imageWidth));
        int top = clampValue(Math.min(y1, y2), 0, Math.max(0, imageHeight));
        int bottom = clampValue(Math.max(y1, y2), 0, Math.max(0, imageHeight));
        return new OcrWindowRegion(left, top, right, bottom);
    }

    /**
     * Expand the region and clamp it to an image/window size.
     *
     * @param padX pixels to add on both left and right edges.
     * @param padY pixels to add on both top and bottom edges.
     * @param imageWidth maximum width in pixels.
     * @param imageHeight maximum height in pixels.
     * @return expanded, clamped region.
     */
    public OcrWindowRegion expand(int padX, int padY, int imageWidth, int imageHeight) {
        return new OcrWindowRegion(x1 - padX, y1 - padY, x2 + padX, y2 + padY)
                .clamp(imageWidth, imageHeight);
    }

    /**
     * @return true when this region has positive width and height.
     */
    public boolean isValid() {
        return x2 > x1 && y2 > y1;
    }

    /**
     * @return non-negative width in pixels.
     */
    public int width() {
        return Math.max(0, x2 - x1);
    }

    /**
     * @return non-negative height in pixels.
     */
    public int height() {
        return Math.max(0, y2 - y1);
    }

    /**
     * @return compact debug text in "x1,y1 -> x2,y2" form.
     */
    public String toShortText() {
        return x1 + "," + y1 + " -> " + x2 + "," + y2;
    }

    private static int clampValue(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
