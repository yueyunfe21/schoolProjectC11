package com.bot.dhxy.cloud.turn.protocol;

public record TurnBagOperationArguments(
        ReturnItemIntent intent,
        String targetItemTemplate,
        Integer maxBagIndex,
        TurnReturnItemCachePoint cachedPoint,
        String source) {

    public enum ReturnItemIntent {
        PRESCAN_TASK_PAGE,
        PRESCAN_FROM_BACK,
        USE_CACHED_RETURN_ITEM
    }
}
