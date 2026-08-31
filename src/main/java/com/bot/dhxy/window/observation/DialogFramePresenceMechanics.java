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
    /** G113-4 复修：横边带最少连续合格行数——单行场景线（压暗帧 y=0）不配当边。 */
    private static final int MIN_EDGE_BAND_ROWS = 2;
    /** G113-4：左右沿必须各有一条竖直中性边支撑；该列在上下边之间的中性覆盖率下限。 */
    private static final double MIN_VERTICAL_EDGE_RATIO = 0.80D;
    /** 对话框最小可信宽度（竖边求出的 right-left），防止把窄横条误判成框。 */
    private static final int MIN_PANEL_WIDTH = 200;

    /**
     * Tests whether the raw maximum dialog ROI contains both long, overlapping panel borders.
     *
     * @param dialogRoi raw window-relative dialog ROI in image-local pixels; may be {@code null}
     * @return {@code true} only when a complete dialog frame is structurally visible
     */
    public boolean isPresent(BufferedImage dialogRoi) {
        return analyze(dialogRoi).present();
    }

    /**
     * Same structural judgement as {@link #isPresent}, but also returns the frame rectangle it
     * matched.
     *
     * <p>Presence is judged locally and reported to the cloud; the cloud must never run a second
     * image judgement of its own. Before this, the fact carried only a word, so cloud-side callers
     * that needed the frame position re-analysed the picture with their own detector — a duplicate
     * authority that disagreed with this one (2026-08-28: this class said the dialog was present
     * while the cloud classifier found no frame on the same screen, and the quiz task spun for
     * ~30s and even clicked with no dialog on screen). The edges are already computed here; they
     * are now published instead of discarded.</p>
     *
     * @param dialogRoi raw window-relative dialog ROI in image-local pixels; may be {@code null}
     * @return presence plus the ROI-relative frame bounds (bounds are -1 when absent)
     */
    public FramePresence analyze(BufferedImage dialogRoi) {
        if (dialogRoi == null || dialogRoi.getWidth() <= 0 || dialogRoi.getHeight() <= 0) {
            return FramePresence.absent();
        }
        int requiredRun = (int) Math.ceil(dialogRoi.getWidth() * MIN_FRAME_RUN_RATIO);

        /*
         * 第一阶段：每行取最长中性 run，收集合格横线，并把相邻合格行折叠成"横边带"。
         * G113-4 复修（答错浮窗压暗帧实测）：压暗遮罩让场景大面积变中性，单独一行场景
         * 像素（y=0）也能凑出 65% 宽的 run 冒充顶横线；而真实面板边框总是连续多行合格
         * （实测顶边 13 行、底边 2 行）。单行合格的"横线"一律不进候选。
         */
        java.util.List<FrameEdge> qualifying = new java.util.ArrayList<>();
        for (int y = 0; y < dialogRoi.getHeight(); y++) {
            FrameEdge longest = longestNeutralRun(dialogRoi, y);
            if (longest != null && longest.width() >= requiredRun) {
                qualifying.add(longest);
            }
        }
        java.util.List<FrameEdge> bands = new java.util.ArrayList<>();
        int index = 0;
        while (index < qualifying.size()) {
            int bandStart = index;
            while (index + 1 < qualifying.size()
                    && qualifying.get(index + 1).y() == qualifying.get(index).y() + 1) {
                index++;
            }
            if (index - bandStart + 1 >= MIN_EDGE_BAND_ROWS) {
                // 代表行取带内首行（面板边框的外沿）。
                bands.add(qualifying.get(bandStart));
            }
            index++;
        }

        /*
         * 第二阶段：枚举全部 (top,bottom) 配对，通过高度/重叠校验的候选里取
         * 顶底 run 重叠区间最宽的一对——伪配对（场景行×真底边）的重叠总是更窄
         * （实测 422 vs 461）。竖边支撑只在该重叠区间内找。
         */
        FramePresence best = null;
        int bestOverlap = -1;
        for (int t = 0; t < bands.size(); t++) {
            for (int b = t + 1; b < bands.size(); b++) {
                FrameEdge top = bands.get(t);
                FrameEdge bottom = bands.get(b);
                if (!isMatchingBottomEdge(top, bottom)) {
                    continue;
                }
                int overlapStart = Math.max(top.startX(), bottom.startX());
                int overlapEnd = Math.min(top.endX(), bottom.endX());
                int overlap = overlapEnd - overlapStart;
                if (overlap <= bestOverlap) {
                    continue;
                }
                FramePresence framed = frameWithVerticalSupport(
                        dialogRoi, top, bottom, overlapStart, overlapEnd - 1);
                if (framed != null) {
                    best = framed;
                    bestOverlap = overlap;
                }
            }
        }
        return best == null ? FramePresence.absent() : best;
    }

    /** 该行最长的中性 run；无中性像素时返回 null。 */
    private static FrameEdge longestNeutralRun(BufferedImage dialogRoi, int y) {
        int runStart = -1;
        FrameEdge longest = null;
        for (int x = 0; x <= dialogRoi.getWidth(); x++) {
            boolean neutral = x < dialogRoi.getWidth() && isNeutralFramePixel(dialogRoi.getRGB(x, y));
            if (neutral && runStart < 0) {
                runStart = x;
            }
            if (!neutral && runStart >= 0) {
                if (longest == null || x - runStart > longest.width()) {
                    longest = new FrameEdge(runStart, x, y);
                }
                runStart = -1;
            }
        }
        return longest;
    }

    /** ROI-relative dialog frame rectangle; all bounds are -1 when no frame is present. */
    public record FramePresence(boolean present, int left, int top, int right, int bottom) {
        static FramePresence absent() {
            return new FramePresence(false, -1, -1, -1, -1);
        }
    }

    /**
     * Confirms the two horizontal edges are the top/bottom of one real panel by demanding a
     * near-continuous vertical neutral edge at both the left and the right side, then returns the
     * rectangle anchored on those verified vertical edges (not on the horizontal-run endpoints,
     * which text rows can shift).
     */
    private static FramePresence frameWithVerticalSupport(BufferedImage dialogRoi,
                                                          FrameEdge top,
                                                          FrameEdge bottom,
                                                          int overlapStart,
                                                          int overlapEnd) {
        // 只在顶底 run 的重叠区间内找竖边：union 会把画面边缘的压暗场景列误当框边。
        int searchLeft = Math.max(0, overlapStart);
        int searchRight = Math.min(dialogRoi.getWidth() - 1, overlapEnd);
        int left = findVerticalEdge(dialogRoi, top.y(), bottom.y(), searchLeft, searchRight, +1);
        if (left < 0) {
            return null;
        }
        int right = findVerticalEdge(dialogRoi, top.y(), bottom.y(), searchRight, searchLeft, -1);
        if (right < 0 || right - left < MIN_PANEL_WIDTH) {
            return null;
        }
        return new FramePresence(true, left, top.y(), right, bottom.y());
    }

    /**
     * Scans columns from {@code fromX} toward {@code toX} and returns the first column whose
     * neutral-pixel coverage between the two horizontal edges is at least
     * {@link #MIN_VERTICAL_EDGE_RATIO}; -1 when none qualifies.
     */
    private static int findVerticalEdge(BufferedImage dialogRoi,
                                        int topY,
                                        int bottomY,
                                        int fromX,
                                        int toX,
                                        int step) {
        int height = bottomY - topY + 1;
        int requiredNeutral = (int) Math.ceil(height * MIN_VERTICAL_EDGE_RATIO);
        for (int x = fromX; step > 0 ? x <= toX : x >= toX; x += step) {
            int neutral = 0;
            for (int y = topY; y <= bottomY; y++) {
                if (isNeutralFramePixel(dialogRoi.getRGB(x, y))) {
                    neutral++;
                }
            }
            if (neutral >= requiredNeutral) {
                return x;
            }
        }
        return -1;
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
