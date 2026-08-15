package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * TURN-40G: mechanical, business-free fact kinds a local window observation runner may report. Facts carry no task
 * interpretation; the Cloud consumes them to advance its own state machines.
 */
public enum ObservationFactType {
    /** Fixed-template or pixel-difference combat signal sample (no business hysteresis attached). */
    COMBAT_SIGNAL,
    /** Local position fast-path sample (template-first minimap coordinate strip result). */
    POSITION_SAMPLE,
    /** Local runtime timer edge (e.g. pre-battle timer progression) without consumption semantics. */
    TIMER_EDGE,
    /** Client-local 新手 template anchor hit; value is the matched template filename. */
    XINSHOU_ANCHOR,
    /** Client-local 新手 ESC template visibility; value is the literal `present` or `absent`. */
    XINSHOU_ESC_VISIBLE,
    /** Client-local 新手 animation skip template visibility; value is the literal `present` or `absent`. */
    XINSHOU_SKIP_VISIBLE,
    /** Client-local 新手 esc_bot recovery visibility; value is the literal `present` or `absent`. */
    XINSHOU_ESC_BOT,
    /** Client-local 新手 structural dialog visibility; value is the literal present or absent. */
    XINSHOU_DIALOG_PRESENCE,
    /** Client-local 新手 no-progress reproof marker; valid only with same-sequence scene ROI evidence. */
    XINSHOU_NO_PROGRESS_REFRESH,

    /** 新手 §3 领养 anchor visibility; independent of the title anchor so it is never swallowed. */
    XINSHOU_ADOPTION,
    /** Client-local 新手恢复动作状态; no template point or coordinate is sent to Cloud. */
    XINSHOU_RECOVERY_STATUS,
    /** Local deterministic 五环 tracker title presence; Cloud owns every business decision. */
    WUHUAN_TITLE_PRESENCE,
    /** Local structural dialog-presence fact; Cloud must demand one exact frame before acting. */
    WUHUAN_DIALOG_PRESENCE,
    /**
     * Client-local 五环 completion story verdict sampled in the dialog ROI when the tracker title is
     * absent; value is the literal {@code finished}, {@code finishedOnce} or {@code absent}. Raw
     * source crops are matched locally so completion never waits on a Cloud frame round trip.
     */
    WUHUAN_COMPLETION_PRESENCE,
    /** BR-DIALOG-001 current-task title presence from the same exact observation cycle. */
    UNKNOWN_PHASE_TITLE_PRESENCE,
    /** BR-DIALOG-001 dialog state paired with the title fact from the same observation cycle. */
    UNKNOWN_PHASE_DIALOG_PRESENCE,
    /** On-demand local saturation classification for one confirmed 天庭暗雷 attempt. */
    TIANTING_DARK_THUNDER_FLIGHT_STATE,
    /**
     * Client-local 天庭 dialog option outcome; the value names the matched option template and whether
     * the local click executed, so the Cloud learns what its window just answered without ever
     * receiving a screen point.
     */
    TIANTING_DIALOG_ACTION
}
