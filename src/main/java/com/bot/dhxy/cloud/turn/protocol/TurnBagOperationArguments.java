package com.bot.dhxy.cloud.turn.protocol;

/**
 * Typed bag arguments for every closed bag LOCAL_SERVICE operation. TURN-40B-C2 reuses this exact
 * record for the three queue-owning operations — no parallel payload model exists:
 *
 * <ul>
 *   <li>{@code BAG_FIVERING_SUPPLY_CHECK}: {@code targetItemTemplate} is the counted item template
 *       and the frozen {@code maxBagIndex} slot carries {@code requiredCount}; {@code intent} and
 *       {@code cachedPoint} stay null.</li>
 *   <li>{@code BAG_FIND_AND_USE_FROM_BACK}: {@code targetItemTemplate} plus {@code maxBagIndex} as
 *       the existing back-scan bound; {@code intent} and {@code cachedPoint} stay null.</li>
 *   <li>{@code BAG_FIND_ITEM_PAGE_INDEX}: {@code targetItemTemplate} only; {@code intent},
 *       {@code maxBagIndex} and {@code cachedPoint} stay null.</li>
 * </ul>
 *
 * <p>All three operate on the main bag exactly like their baseline call sites; the validator
 * enforces the exact per-operation shape.</p>
 */
public record TurnBagOperationArguments(
        ReturnItemIntent intent,
        String targetItemTemplate,
        Integer maxBagIndex,
        TurnReturnItemCachePoint cachedPoint,
        String source,
        String retainedReplayTaskCode,
        String retainedReplayObservationRunId,
        String retainedReplayBusinessTaskRunId) {

    public TurnBagOperationArguments(
            ReturnItemIntent intent,
            String targetItemTemplate,
            Integer maxBagIndex,
            TurnReturnItemCachePoint cachedPoint,
            String source) {
        this(intent, targetItemTemplate, maxBagIndex, cachedPoint, source, null, null, null);
    }

    public enum ReturnItemIntent {
        PRESCAN_TASK_PAGE,
        PRESCAN_FROM_BACK,
        USE_CACHED_RETURN_ITEM,
        FIND_AND_USE_TASK_PAGE
    }
}
