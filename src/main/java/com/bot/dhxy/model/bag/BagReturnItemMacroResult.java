package com.bot.dhxy.model.bag;

import com.bot.dhxy.model.bag.ReturnItemCachePoint;

import java.util.Objects;

/**
 * W-BAG-MACRO-LOCAL-MECHANICS-IMP1: closed typed result of one Bag return-item local macro run.
 *
 * <p>This is a DHXY-local domain model, not a Cloud wire DTO. It carries only the committed business
 * outcome, so the handler can map it onto the B wire exactly: a prescan that produced a non-null
 * cache point is {@link Status#FOUND} (carrying that point), otherwise {@link Status#NOT_FOUND}; a
 * cached use is {@link Status#USED} when it clicked and {@link Status#NOT_USED} otherwise. Transport
 * or fence terminals ({@code NOT_EXECUTED}/{@code STOPPED}/{@code UNKNOWN}) are never expressed here;
 * those stay outside the business result at the handler/wire boundary.</p>
 */
public final class BagReturnItemMacroResult {

    private final Status status;
    private final ReturnItemCachePoint cachePoint;

    private BagReturnItemMacroResult(Status status, ReturnItemCachePoint cachePoint) {
        this.status = Objects.requireNonNull(status, "status");
        if (status == Status.FOUND) {
            this.cachePoint = Objects.requireNonNull(cachePoint, "cachePoint");
        } else {
            if (cachePoint != null) {
                throw new IllegalArgumentException("only FOUND may carry a cache point");
            }
            this.cachePoint = null;
        }
    }

    /** A prescan that produced a cache point. */
    public static BagReturnItemMacroResult found(ReturnItemCachePoint cachePoint) {
        return new BagReturnItemMacroResult(Status.FOUND, cachePoint);
    }

    /** A prescan that produced no cache point. */
    public static BagReturnItemMacroResult notFound() {
        return new BagReturnItemMacroResult(Status.NOT_FOUND, null);
    }

    /** A cached use that clicked the item. */
    public static BagReturnItemMacroResult used() {
        return new BagReturnItemMacroResult(Status.USED, null);
    }

    /** A cached use that did not click (null point or non-verified). */
    public static BagReturnItemMacroResult notUsed() {
        return new BagReturnItemMacroResult(Status.NOT_USED, null);
    }

    public Status getStatus() {
        return status;
    }

    /** Non-null only for {@link Status#FOUND}. */
    public ReturnItemCachePoint getCachePoint() {
        return cachePoint;
    }

    /** Closed business outcome set mapped one-to-one onto the B wire. */
    public enum Status {
        FOUND,
        NOT_FOUND,
        USED,
        NOT_USED
    }
}
