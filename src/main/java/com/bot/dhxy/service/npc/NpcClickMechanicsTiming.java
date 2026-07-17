package com.bot.dhxy.service.npc;

/**
 * Single source of truth for NPC smart-click local input timing constants.
 *
 * <p>These are the exact fixed waits/holds from the HEAD {@code 0114604e}
 * {@code NpcClickService} local mechanics. They live here so both the local
 * {@code NpcClickService} and the remote command handler read one owner, with no value drift and no
 * merged, removed, or added wait. Values and semantics are byte-for-byte equivalent to baseline; the
 * cloud brain never selects, overrides, or transmits any of these timings.</p>
 */
public final class NpcClickMechanicsTiming {

    /** Left-click hold for a normal candidate / Ctrl menu click (HEAD NpcClickService NPC_LEFT_CLICK_HOLD_MS). */
    public static final long NPC_LEFT_CLICK_HOLD_MS = 150L;

    /** FIFO queue WAIT poll sleep between cloud poll attempts (HEAD NPC_CLICK_SMART_QUEUE_WAIT_SLEEP_MS). */
    public static final long NPC_CLICK_SMART_QUEUE_WAIT_SLEEP_MS = 100L;

    /** Settle after the Alt+4 clean-name preparation before the base screenshot. */
    public static final long ALT_4_POST_SLEEP_MS = 180L;

    /** Settle after moving the mouse to a normal candidate before the click. */
    public static final long STANDARD_MOVE_SETTLE_MS = 150L;

    /** Wait after a normal candidate click before running the verifier. */
    public static final long STANDARD_POST_CLICK_VERIFY_MS = 1500L;

    /** Settle after holding Ctrl for a menu probe. */
    public static final long CTRL_HOLD_SETTLE_MS = 80L;

    /** Settle after moving to a Ctrl probe offset. */
    public static final long CTRL_MOVE_SETTLE_MS = 280L;

    /** Interval between successive Ctrl probe offsets. */
    public static final long CTRL_OFFSET_INTERVAL_MS = 100L;

    /** Wait after a Ctrl menu click before verification. */
    public static final long CTRL_CLICK_TO_VERIFY_MS = 100L;

    /** Wait after a matched Ctrl candidate click before the verifier resolves. */
    public static final long CTRL_MATCH_POST_CLICK_MS = 900L;

    private NpcClickMechanicsTiming() {
    }
}
