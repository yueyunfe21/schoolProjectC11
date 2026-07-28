package com.bot.dhxy.model.bag;

import com.bot.dhxy.model.bag.ReturnItemCachePoint;

import java.util.Objects;

/**
 * W-BAG-MACRO-LOCAL-MECHANICS-IMP1: closed local intent selecting exactly one of the three committed
 * {@code 0114604e} Bag return-item exclusive flows to run as a single local macro.
 *
 * <p>This is a DHXY-local domain model, not a Cloud wire DTO. It carries only the committed business
 * parameters of the chosen flow; it introduces no owner, permit, session, ledger, TTL, or retry, and
 * does not change any committed template path, page order, capture count, coordinate, delay, or
 * fallback. The macro is meant to be dispatched inside the input worker's exclusive section; it never
 * itself acquires the exclusive queue.</p>
 */
public final class BagReturnItemMacroIntent {

    private final Kind kind;
    private final String targetItemTemplate;
    private final int maxBagIndex;
    private final ReturnItemCachePoint cachedPoint;
    private final String source;

    private BagReturnItemMacroIntent(
            Kind kind,
            String targetItemTemplate,
            int maxBagIndex,
            ReturnItemCachePoint cachedPoint,
            String source) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.targetItemTemplate = targetItemTemplate;
        this.maxBagIndex = maxBagIndex;
        this.cachedPoint = cachedPoint;
        this.source = requireText(source, "source");
    }

    /**
     * Prescan the main-bag task page (current visible frame first, then task page): committed
     * {@code prescanMainBagTaskPageItem} core.
     */
    public static BagReturnItemMacroIntent prescanTaskPage(String targetItemTemplate, String source) {
        return new BagReturnItemMacroIntent(
                Kind.PRESCAN_TASK_PAGE, requireText(targetItemTemplate, "targetItemTemplate"), -1, null, source);
    }

    /**
     * Prescan from the back (current visible frame first, then clamp(max, 0..5) down to 0): committed
     * {@code prescanMainBagItemFromBack} core.
     */
    public static BagReturnItemMacroIntent prescanFromBack(String targetItemTemplate, int maxBagIndex, String source) {
        return new BagReturnItemMacroIntent(
                Kind.PRESCAN_FROM_BACK, requireText(targetItemTemplate, "targetItemTemplate"), maxBagIndex, null, source);
    }

    /**
     * Use a previously cached main-bag return-item point without re-scanning: committed
     * {@code useCachedMainBagReturnItem} core. A null cached point preserves the committed
     * {@code false} outcome.
     */
    public static BagReturnItemMacroIntent useCachedReturnItem(ReturnItemCachePoint cachedPoint, String source) {
        return new BagReturnItemMacroIntent(Kind.USE_CACHED_RETURN_ITEM, null, -1, cachedPoint, source);
    }

    /**
     * Atomically find and use the main-bag task-page item in one exclusive pass: committed
     * {@code interactWithMainBagTaskPageItemExclusive(template, USE, context)} core. Carries no cached
     * point; the single exclusive lock spans the whole find+use so the two steps never split.
     */
    public static BagReturnItemMacroIntent findAndUseTaskPage(String targetItemTemplate, String source) {
        return new BagReturnItemMacroIntent(
                Kind.FIND_AND_USE_TASK_PAGE, requireText(targetItemTemplate, "targetItemTemplate"), -1, null, source);
    }

    public Kind getKind() {
        return kind;
    }

    public String getTargetItemTemplate() {
        return targetItemTemplate;
    }

    public int getMaxBagIndex() {
        return maxBagIndex;
    }

    public ReturnItemCachePoint getCachedPoint() {
        return cachedPoint;
    }

    public String getSource() {
        return source;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return value;
    }

    /** The covered committed Bag return-item flows. */
    public enum Kind {
        PRESCAN_TASK_PAGE,
        PRESCAN_FROM_BACK,
        USE_CACHED_RETURN_ITEM,
        FIND_AND_USE_TASK_PAGE
    }
}
