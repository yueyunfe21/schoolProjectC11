package com.bot.dhxy.input.action;

import java.awt.Point;

/**
 * The single owner of the rectangle the mouse must never come to rest in.
 *
 * <p>Both ways the game dialog is read are pixel reads with no mouse control of their own: the
 * observation sampler crops its shared cycle frame and matches the 天庭 option templates locally, and
 * {@code CloudDialogDetectionPort} uploads the same ROI for Cloud-side green/white classification. A
 * cursor sitting on an option line removes part of a 34–93px glyph run, so the local template misses
 * and the Cloud green-pixel count drops below its OPTION threshold — the dialog is then answered as a
 * story page. The sampler creates this state itself: after it clicks an option the pointer stays on
 * the line it just clicked, so the next cycle reads through the cursor.</p>
 *
 * <p>The rule is therefore placed where the mouse is <em>written</em>, not where pixels are read:
 * there are many readers and only one input worker. Read sites need no knowledge of this class.</p>
 *
 * <p>Geometry is window-relative and unscaled, the same space {@code CoordinateHelper.getScaledRect}
 * and the turn protocol use. It is derived from the Cloud dialog constants: x starts at the dialog
 * ROI's own x (200) and spans half the ROI width (640/2); y is the option-row band, from
 * {@code DIALOG_SMALL_Y + SMALL_ONE_LINE_CROP_TOP_Y} (345+34) to {@code DIALOG_SMALL_Y +
 * DIALOG_SMALL_H} (345+143). The 34 crop is the smallest of the three, so the band is the widest of
 * the three variants rather than only the default one.</p>
 */
public final class DialogMouseNoParkZone {

    /** Window-relative unscaled no-park rectangle; inclusive on all four edges. */
    static final int ZONE_LEFT_REL_X = 200;
    static final int ZONE_TOP_REL_Y = 379;
    static final int ZONE_RIGHT_REL_X = 520;
    static final int ZONE_BOTTOM_REL_Y = 488;

    /**
     * Window-relative rectangle the park point is drawn from at random.
     *
     * <p>A fixed landing point is a bot fingerprint — the cursor coming to rest on the exact same
     * pixel after every action is trivially detectable, so each sweep draws a fresh uniform point
     * (user contract, 2026-08-02). The rectangle itself is user-specified: screen-absolute
     * (969,463)-(1264,681) measured on the leader window whose base was (254,23), i.e.
     * window-relative (715,440)-(1010,658) — open scene ground on the window's lower right. It is
     * disjoint from the no-park zone, the task tracker search area (x 6–207 / y 196–551, entries
     * highlight on hover and break title matching), and the minimap (x ≥ 761 / y ≤ 147).</p>
     */
    static final int PARK_REGION_LEFT_REL_X = 715;
    static final int PARK_REGION_RIGHT_REL_X = 1010;
    static final int PARK_REGION_TOP_REL_Y = 440;
    static final int PARK_REGION_BOTTOM_REL_Y = 658;

    /** Fallback corner insets for degenerate window geometry that cannot fit the region. */
    static final int PARK_INSET_X = 1;
    static final int PARK_INSET_Y = 2;

    private DialogMouseNoParkZone() {
    }

    /**
     * Whether a screen-absolute pointer position rests inside the no-park rectangle.
     *
     * @param screenX screen-absolute unscaled pointer X.
     * @param screenY screen-absolute unscaled pointer Y.
     * @param windowLeft screen-absolute unscaled window left edge.
     * @param windowTop screen-absolute unscaled window top edge.
     * @return true only when the pointer is inside the rectangle and must be moved out.
     */
    /** Screen-point zone test for callers outside this package (pre-capture cursor nudge). */
    public static boolean containsScreenPoint(int screenX, int screenY, int windowLeft, int windowTop) {
        return contains(screenX, screenY, windowLeft, windowTop);
    }

    /**
     * 2026-08-17 user decision: leaving the zone must look human — shortest path out, not a 500px
     * teleport. Returns a screen point just past the NEAREST zone edge with 15~35px random margin.
     */
    public static Point nearestExitTarget(int screenX, int screenY, int windowLeft, int windowTop) {
        int relX = screenX - windowLeft;
        int relY = screenY - windowTop;
        int margin = java.util.concurrent.ThreadLocalRandom.current().nextInt(15, 36);
        int toLeft = relX - ZONE_LEFT_REL_X;
        int toRight = ZONE_RIGHT_REL_X - relX;
        int toTop = relY - ZONE_TOP_REL_Y;
        int toBottom = ZONE_BOTTOM_REL_Y - relY;
        int min = Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));
        if (min == toBottom) {
            return new Point(screenX, windowTop + ZONE_BOTTOM_REL_Y + margin);
        }
        if (min == toTop) {
            return new Point(screenX, windowTop + ZONE_TOP_REL_Y - margin);
        }
        if (min == toLeft) {
            return new Point(windowLeft + ZONE_LEFT_REL_X - margin, screenY);
        }
        return new Point(windowLeft + ZONE_RIGHT_REL_X + margin, screenY);
    }

    static boolean contains(int screenX, int screenY, int windowLeft, int windowTop) {
        int relX = screenX - windowLeft;
        int relY = screenY - windowTop;
        return relX >= ZONE_LEFT_REL_X
                && relX <= ZONE_RIGHT_REL_X
                && relY >= ZONE_TOP_REL_Y
                && relY <= ZONE_BOTTOM_REL_Y;
    }

    /**
     * A fresh random screen-absolute park point inside the safe bottom-left region.
     *
     * <p>Randomized on every call so the cursor never rests on the same pixel twice in a row; a
     * window too small to fit the region falls back to the bottom-left corner point.</p>
     *
     * @param windowLeft screen-absolute unscaled window left edge.
     * @param windowTop screen-absolute unscaled window top edge.
     * @param windowWidth positive unscaled window width.
     * @param windowHeight positive unscaled window height.
     * @return a screen-absolute unscaled park point inside the window.
     */
    static Point parkTarget(int windowLeft, int windowTop, int windowWidth, int windowHeight) {
        int right = Math.min(PARK_REGION_RIGHT_REL_X, windowWidth - PARK_INSET_X - 1);
        int bottom = Math.min(PARK_REGION_BOTTOM_REL_Y, windowHeight - PARK_INSET_Y - 1);
        if (right <= PARK_REGION_LEFT_REL_X || bottom <= PARK_REGION_TOP_REL_Y) {
            return new Point(windowLeft + PARK_INSET_X, windowTop + windowHeight - PARK_INSET_Y);
        }
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        return new Point(
                windowLeft + random.nextInt(PARK_REGION_LEFT_REL_X, right + 1),
                windowTop + random.nextInt(PARK_REGION_TOP_REL_Y, bottom + 1));
    }

    /**
     * Whether a screen-absolute point lies inside the current window rectangle.
     *
     * <p>The sweep must never emit a move whose endpoint is outside the window: such a move is a
     * no-op — the cursor effectively stays where it was, still covering the dialog — while the log
     * would claim it was parked. Verified against the same binding rectangle the target was computed
     * from, so a degenerate or stale geometry skips the move instead of faking one.</p>
     */
    static boolean insideWindow(Point point, int windowLeft, int windowTop,
                                int windowWidth, int windowHeight) {
        return point != null
                && point.x >= windowLeft
                && point.x < windowLeft + windowWidth
                && point.y >= windowTop
                && point.y < windowTop + windowHeight;
    }
}
