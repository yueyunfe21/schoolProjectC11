package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * TURN-40G: key edge kinds that must never be lost — each key event has its own eventId and is retained and resent
 * by the local runner until the Cloud acknowledges it (unlike ordinary snapshots, which are latest-wins).
 */
public enum ObservationKeyEventType {
    /** Real combat entry confirmed by the combat probe (never inferred from a click alone). */
    IN_COMBAT,
    /** Combat exit edge. */
    COMBAT_EXITED,
    /** Pathing first reached a terminal state (ARRIVED / STOPPED_AWAY) for the carried intent. */
    PATHING_TERMINAL,
    /** The local pre-battle timer published its one-time timeout edge. */
    PRE_BATTLE_TIMEOUT,
    /** The xiuluo local-kanda fast path executed its enter-battle click (distinct from IN_COMBAT). */
    ENTER_BATTLE_CLICKED,
    /** Three executed local 看打 clicks produced no Runner-confirmed combat; Cloud may decide fallback. */
    ENTER_BATTLE_CLICK_FAILED,
    /** Retained return-home action completed before the local exit edge was published. */
    RETURN_HOME_REPLAY_SUCCEEDED,
    /** Retained return-home action failed mechanically; Cloud must enter its explicit fallback. */
    RETURN_HOME_REPLAY_FAILED,
    /** Retained replay was rejected by exact run/window/HWND/geometry fencing. */
    RETURN_HOME_REPLAY_IDENTITY_REJECTED,
    /**
     * The 天庭 local option probe clicked a dialog option. The click has already happened physically,
     * so this edge must survive a failed upload the same way an enter-battle click does — a lost
     * report would leave the Cloud parked on a dialog its own window already answered.
     */
    TIANTING_DIALOG_CLICKED,
    /** The 鬼王 local accept probe matched and attempted its exact dialog click. */
    GHOST_KING_DIALOG_CLICKED,
    DALISI_DIALOG_CLICKED,
    /**
     * One exact recovery-probe frame was available, but none of 天庭's seven known options matched.
     * This is the only event that authorizes the Cloud's generic dialog fallback.
     */
    TIANTING_RECOVERY_ALL_MISSED,
    /**
     * The local runner watched the auto-combat panel disappear during combat, pressed one Alt+8
     * itself, and verified the panel is visible again. The Cloud consumes this edge only to reset
     * its remaining-rounds ledger; the physical repair has already happened locally.
     */
    AUTO_PANEL_MAINTAINED
}
