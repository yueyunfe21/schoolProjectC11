package com.bot.dhxy.update;

public final class UpdateVersionComparator {

    private UpdateVersionComparator() {
    }

    public static int compare(String left, String right) {
        int[] leftParts = parseNumericParts(left);
        int[] rightParts = parseNumericParts(right);
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int l = i < leftParts.length ? leftParts[i] : 0;
            int r = i < rightParts.length ? rightParts[i] : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static int[] parseNumericParts(String version) {
        String normalized = version == null ? "" : version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        int suffixIndex = normalized.indexOf('-');
        if (suffixIndex >= 0) {
            normalized = normalized.substring(0, suffixIndex);
        }
        if (normalized.isBlank()) {
            return new int[]{0};
        }
        String[] rawParts = normalized.split("\\.");
        int[] parts = new int[rawParts.length];
        for (int i = 0; i < rawParts.length; i++) {
            parts[i] = parsePart(rawParts[i]);
        }
        return parts;
    }

    private static int parsePart(String rawPart) {
        String digits = rawPart.replaceAll("[^0-9].*$", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }
}
