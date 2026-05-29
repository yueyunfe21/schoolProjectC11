package com.bot.dhxy.vision;

import com.bot.dhxy.model.ocr.OcrWordResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Shared OCR text matching helpers for short in-game names.
 *
 * <p>Game OCR frequently corrupts one or two glyphs in NPC names while preserving a contiguous
 * suffix. This utility centralizes the conservative fuzzy-name rule so services do not each grow
 * their own slightly different name matcher. The methods are pure CPU helpers: they do not capture
 * screenshots, call OCR providers, mutate task state, or send input.</p>
 */
public final class OcrTextMatcher {

    private OcrTextMatcher() {
    }

    /**
     * Normalize OCR text before comparing short game names.
     *
     * <p>The returned string removes common NPC tag suffixes, whitespace, and punctuation while
     * keeping CJK letters and ASCII letters/digits. This is intended for text comparison only; it
     * must not be shown to users as a corrected OCR value.</p>
     *
     * @param value raw OCR/provider text; null is accepted.
     * @return compact lowercase text used for matching; empty when no comparable characters remain.
     */
    public static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String withoutTag = value.replaceAll(
                "(?i)[()\\uFF08\\uFF09]?\\s*(NPC|IPC|PC|NP)\\s*[()\\uFF08\\uFF09]?",
                "");
        StringBuilder builder = new StringBuilder();
        for (char c : withoutTag.toCharArray()) {
            if ((c >= '\u4e00' && c <= '\u9fff') || Character.isLetterOrDigit(c)) {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    /**
     * Score OCR text against an expected in-game name.
     *
     * <p>Exact containment wins. Otherwise, a match is accepted only when the OCR text shares a
     * sufficiently long contiguous substring with the expected name. This intentionally rejects
     * one-character coincidences, which are too common in dense game UI text.</p>
     *
     * @param ocrText raw OCR text for one block/line; may include an NPC tag suffix.
     * @param expectedName expected NPC/map/object name.
     * @return positive score for an accepted match; zero means reject.
     */
    public static int shortNameMatchScore(String ocrText, String expectedName) {
        String text = normalizeName(ocrText);
        String target = normalizeName(expectedName);
        if (text.isBlank() || target.isBlank()) {
            return 0;
        }
        if (text.contains(target)) {
            return 1000 + target.length();
        }
        if (target.contains(text) && text.length() >= Math.min(3, target.length())) {
            return 700 + text.length();
        }

        int common = longestCommonSubstringLength(text, target);
        int required = Math.max(2, (int) Math.ceil(target.length() * 0.6));
        if (common >= required && text.length() >= 2) {
            return 400 + common * 10 - Math.abs(text.length() - target.length());
        }
        return 0;
    }

    /**
     * Check whether OCR text is an accepted fuzzy match for a short in-game name.
     *
     * @param ocrText raw OCR text for one block/line.
     * @param expectedName expected NPC/map/object name.
     * @return true when {@link #shortNameMatchScore(String, String)} is positive.
     */
    public static boolean isShortNameMatch(String ocrText, String expectedName) {
        return shortNameMatchScore(ocrText, expectedName) > 0;
    }

    /**
     * Check whether any OCR word contains one of the accepted keywords exactly.
     *
     * <p>This intentionally uses conservative {@code contains} matching rather than fuzzy name
     * matching. Dialog menu options often contain map names plus extra suffixes, for example
     * {@code 长安} should match {@code 长安桥}; loose fuzzy matching would be too risky for menu
     * clicks.</p>
     *
     * @param words OCR word boxes from one image, with image-local coordinates.
     * @param keywords accepted exact substrings; null/blank entries are ignored.
     * @return true when any OCR word text contains any keyword.
     */
    public static boolean hasAnyKeyword(List<OcrWordResult> words, List<String> keywords) {
        if (words == null || words.isEmpty() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank() && word.getText().contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Match OCR text against an expected name and expose diagnostics for logs.
     *
     * <p>This uses the same hit rule as {@link #shortNameMatchScore(String, String)}. The edit
     * distance is kept for debug output and candidate sorting; callers should not define a separate
     * hit rule from it.</p>
     *
     * @param ocrText raw OCR text for one block/line.
     * @param expectedName expected NPC/map/object name.
     * @return normalized names, hit status, score, edit distance, and longest common substring.
     */
    public static MatchResult matchShortName(String ocrText, String expectedName) {
        String text = normalizeName(ocrText);
        String target = normalizeName(expectedName);
        if (text.isBlank() || target.isBlank()) {
            return new MatchResult(false, 0, 999, 0, target, text);
        }
        int common = longestCommonSubstringLength(text, target);
        int distance = bestEditDistance(text, target);
        int score = shortNameMatchScore(ocrText, expectedName);
        return new MatchResult(score > 0, score, distance, common, target, text);
    }

    /**
     * Compute the best edit distance between OCR text and the expected target.
     *
     * <p>When OCR returns extra surrounding characters, this compares target-sized windows from the
     * OCR text instead of only the whole string. The value is for diagnostics and ranking.</p>
     *
     * @param text normalized OCR text.
     * @param target normalized expected name.
     * @return smallest edit distance found; 999 when either side is blank.
     */
    public static int bestEditDistance(String text, String target) {
        if (text == null || target == null || text.isBlank() || target.isBlank()) {
            return 999;
        }
        if (text.length() <= target.length()) {
            return editDistance(text, target);
        }
        int best = 999;
        int minWindow = Math.max(1, target.length() - 2);
        int maxWindow = Math.min(text.length(), target.length() + 2);
        for (int start = 0; start < text.length(); start++) {
            for (int len = minWindow; len <= maxWindow && start + len <= text.length(); len++) {
                best = Math.min(best, editDistance(text.substring(start, start + len), target));
            }
        }
        return best;
    }

    /**
     * Compute Levenshtein edit distance for two normalized strings.
     *
     * @param left first normalized string.
     * @param right second normalized string.
     * @return number of insert/delete/replace operations needed to transform left into right.
     */
    public static int editDistance(String left, String right) {
        if (left == null || right == null) {
            return 999;
        }
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    /**
     * Return the longest contiguous overlap length between two normalized strings.
     *
     * @param left first string; null is treated as no match.
     * @param right second string; null is treated as no match.
     * @return number of matching characters in the longest common substring.
     */
    public static int longestCommonSubstringLength(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0;
        }
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        int best = 0;
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    best = Math.max(best, dp[i][j]);
                }
            }
        }
        return best;
    }

    /**
     * Diagnostic result for a short-name OCR match.
     *
     * @param hit true when the shared matcher accepts the OCR text.
     * @param score shared ranking score; positive values mean hit.
     * @param editDistance diagnostic edit distance between normalized OCR and expected text.
     * @param longestCommonSubstring diagnostic contiguous overlap length.
     * @param normalizedTarget normalized expected name.
     * @param normalizedText normalized OCR text.
     */
    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    public static class MatchResult {
        boolean hit;
        int score;
        int editDistance;
        int longestCommonSubstring;
        String normalizedTarget;
        String normalizedText;
    }
}
