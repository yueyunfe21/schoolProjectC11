package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * W-BAG-MACRO-DHXY-WIRE-IMP1: closed wire command for the {@code LOCAL_MACRO / BAG_RETURN_ITEM} macro.
 * Mirrors the Cloud command contract exactly: {@code PRESCAN_MAIN_BAG_FROM_BACK} is the only operation
 * that carries {@code maxBackPage} (0..4); {@code USE_CACHED_MAIN_BAG_RETURN_ITEM} is the only operation
 * that carries {@code cachedPoint}. Strings are trimmed and must be non-blank.
 */
@Value
@Jacksonized
public class RemoteBagReturnItemMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    Operation operation;
    String templatePath;
    int maxBackPage;
    String source;
    CachePoint cachedPoint;

    @Builder
    public RemoteBagReturnItemMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            Operation operation,
            String templatePath,
            int maxBackPage,
            String source,
            CachePoint cachedPoint) {
        this.macroKind = requireNonNull(macroKind, "macroKind");
        require(macroKind == RemoteLocalMacroKind.BAG_RETURN_ITEM,
                "macroKind must be BAG_RETURN_ITEM");
        this.operation = requireNonNull(operation, "operation");
        this.templatePath = requireText(templatePath, "templatePath");
        this.source = requireText(source, "source");

        if (operation == Operation.PRESCAN_MAIN_BAG_FROM_BACK) {
            require(maxBackPage >= 0 && maxBackPage <= 4, "maxBackPage must be in 0..4 for FROM_BACK");
        } else {
            require(maxBackPage == 0, "maxBackPage must be 0 unless operation is PRESCAN_MAIN_BAG_FROM_BACK");
        }
        this.maxBackPage = maxBackPage;

        if (operation == Operation.USE_CACHED_MAIN_BAG_RETURN_ITEM) {
            this.cachedPoint = requireNonNull(cachedPoint, "cachedPoint");
        } else {
            require(cachedPoint == null, "cachedPoint is only allowed for USE_CACHED_MAIN_BAG_RETURN_ITEM");
            this.cachedPoint = null;
        }
    }

    public enum Operation {
        PRESCAN_MAIN_BAG_TASK_PAGE,
        PRESCAN_MAIN_BAG_FROM_BACK,
        USE_CACHED_MAIN_BAG_RETURN_ITEM
    }

    /** Learned bag return-item click point: template path, screen-absolute click, learn time, and source. */
    @Value
    @Jacksonized
    public static class CachePoint {
        String templatePath;
        int clickX;
        int clickY;
        long learnedAtMs;
        String source;

        @Builder
        public CachePoint(String templatePath, int clickX, int clickY, long learnedAtMs, String source) {
            this.templatePath = requireText(templatePath, "cachePoint.templatePath");
            require(clickX >= 0, "cachePoint.clickX must be non-negative");
            require(clickY >= 0, "cachePoint.clickY must be non-negative");
            require(learnedAtMs > 0L, "cachePoint.learnedAtMs must be positive");
            this.clickX = clickX;
            this.clickY = clickY;
            this.learnedAtMs = learnedAtMs;
            this.source = requireText(source, "cachePoint.source");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
