package com.bot.dhxy.model.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure row clustering and exact-text selection shared by Cloud production and the G051 manual tool. */
public final class WorldMapSearchCandidateMatcher {

    private static final int ROW_CENTER_TOLERANCE_PX = 8;

    private WorldMapSearchCandidateMatcher() {
    }

    public static MatchResult matchExact(String targetMap, List<OcrBox> boxes) {
        String expected = normalizeExact(targetMap);
        if (expected.isEmpty() || boxes == null || boxes.isEmpty()) {
            return MatchResult.notFound(List.of());
        }
        List<List<OcrBox>> rows = new ArrayList<>();
        boxes.stream()
                .filter(box -> box != null && !normalizeExact(box.text()).isEmpty())
                .sorted(Comparator.comparingInt(OcrBox::centerY).thenComparingInt(OcrBox::left))
                .forEach(box -> {
                    List<OcrBox> row = rows.stream()
                            .filter(candidate -> Math.abs(rowCenter(candidate) - box.centerY())
                                    <= ROW_CENTER_TOLERANCE_PX)
                            .findFirst()
                            .orElse(null);
                    if (row == null) {
                        row = new ArrayList<>();
                        rows.add(row);
                    }
                    row.add(box);
                });

        List<Row> resolved = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            List<OcrBox> rowBoxes = rows.get(index);
            rowBoxes.sort(Comparator.comparingInt(OcrBox::left));
            String text = normalizeExact(rowBoxes.stream().map(OcrBox::text).reduce("", String::concat));
            int left = rowBoxes.stream().mapToInt(OcrBox::left).min().orElse(0);
            int top = rowBoxes.stream().mapToInt(OcrBox::top).min().orElse(0);
            int right = rowBoxes.stream().mapToInt(OcrBox::right).max().orElse(left);
            int bottom = rowBoxes.stream().mapToInt(OcrBox::bottom).max().orElse(top);
            resolved.add(new Row(index, text, left, top, right, bottom));
        }
        List<Row> exact = resolved.stream().filter(row -> expected.equals(row.text())).toList();
        return exact.size() == 1
                ? MatchResult.found(exact.get(0), resolved)
                : MatchResult.notFound(resolved);
    }

    /** Exact normalization intentionally preserves 城/村/层 and never applies map aliases. */
    public static String normalizeExact(String value) {
        return value == null ? "" : value.replaceAll("[\\s，,。:：;；]+", "").trim();
    }

    private static int rowCenter(List<OcrBox> row) {
        return (int) Math.round(row.stream().mapToInt(OcrBox::centerY).average().orElse(0));
    }

    public record OcrBox(String text, int left, int top, int width, int height) {
        public int right() { return left + Math.max(1, width); }
        public int bottom() { return top + Math.max(1, height); }
        public int centerY() { return top + Math.max(1, height) / 2; }
    }

    public record Row(int index, String text, int left, int top, int right, int bottom) {
        public int centerX() { return left + Math.max(1, right - left) / 2; }
        public int centerY() { return top + Math.max(1, bottom - top) / 2; }
    }

    public record MatchResult(boolean found, Row target, List<Row> rows) {
        static MatchResult found(Row target, List<Row> rows) {
            return new MatchResult(true, target, List.copyOf(rows));
        }
        static MatchResult notFound(List<Row> rows) {
            return new MatchResult(false, null, List.copyOf(rows));
        }
    }
}

