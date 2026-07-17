package com.bot.dhxy.model.ocr;

/**
 * One OCR text block returned by a provider.
 *
 * <p>All coordinates are image-local pixels. {@code x/y} represent the preferred click or center
 * point for the text block, while {@code left/top/width/height} preserve the bounding rectangle.
 * {@code score} is provider-specific confidence and may be {@code 0.0} when the provider does not
 * expose confidence.</p>
 */
public class OcrWordResult {
    private final String text;
    private final int x;
    private final int y;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final double score;

    /**
     * Create a point-only OCR result.
     *
     * @param text recognized text; may be empty when the provider emits an empty block.
     * @param x image-local center/click X pixel.
     * @param y image-local center/click Y pixel.
     */
    public OcrWordResult(String text, int x, int y) {
        this(text, x, y, x, y, 0, 0, 0.0);
    }

    /**
     * Create an OCR result from a bounding rectangle.
     *
     * @param text recognized text; may be empty when the provider emits an empty block.
     * @param left image-local left pixel of the bounding rectangle.
     * @param top image-local top pixel of the bounding rectangle.
     * @param width rectangle width in pixels.
     * @param height rectangle height in pixels.
     */
    public OcrWordResult(String text, int left, int top, int width, int height) {
        this(text, left + width / 2, top + height / 2, left, top, width, height, 0.0);
    }

    /**
     * Create a complete OCR word result.
     *
     * @param text recognized text; may be empty when the provider emits an empty block.
     * @param x image-local center/click X pixel.
     * @param y image-local center/click Y pixel.
     * @param left image-local left pixel of the bounding rectangle.
     * @param top image-local top pixel of the bounding rectangle.
     * @param width rectangle width in pixels.
     * @param height rectangle height in pixels.
     * @param score provider-specific confidence score, or {@code 0.0} when unknown.
     */
    public OcrWordResult(String text, int x, int y, int left, int top, int width, int height, double score) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.score = score;
    }

    /** @return recognized text for this block, possibly empty. */
    public String getText() { return text; }

    /** @return image-local center/click X pixel. */
    public int getX() { return x; }

    /** @return image-local center/click Y pixel. */
    public int getY() { return y; }

    /** @return image-local left pixel of the bounding rectangle. */
    public int getLeft() { return left; }

    /** @return image-local top pixel of the bounding rectangle. */
    public int getTop() { return top; }

    /** @return bounding rectangle width in pixels. */
    public int getWidth() { return width; }

    /** @return bounding rectangle height in pixels. */
    public int getHeight() { return height; }

    /** @return provider-specific confidence score, or {@code 0.0} when unknown. */
    public double getScore() { return score; }
}
