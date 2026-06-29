package com.bot.dhxy.task.xiuluo;

/**
 * Scheduling-only reason for parking a 修罗 phase after it has released the task turn.
 *
 * <p>These values are not business success/failure states. They only explain which runner/window
 * event can wake the phase so it may re-enter and read the true business state itself.</p>
 */
public enum XiuluoWaitReason {
    WAIT_COMBAT_STATE_CHANGE,
    WAIT_TARGET_PATHING_TERMINAL,
    WAIT_TRACKER_SHORTCUT_PATHING
}
