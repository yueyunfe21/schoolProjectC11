package com.bot.dhxy.cloud.turn.protocol;

/**
 * Closed set of Cloud-authorized 新手 mechanical actions.
 *
 * <p>These values are commands, not observations. Client code may execute one requested action but
 * must not select an action from locally observed business state.</p>
 */
public enum TurnXinshouMechanicalAction {
    CONFIRM_ADOPTION,
    USE_UPGRADE_ITEM_AND_CLOSE_GENERIC_WINDOWS,
    USE_SHELL_AND_BLOW,
    HAND_IN_MATERIALS,
    REPAIR_ITEMS_ONCE,
    CLOSE_REPAIR_WINDOW,
    USE_LUNHUI_ITEM_AND_START,
    PRESS_ESCAPE,
    CLICK_RECOVERY_TEMPLATE,
    CLICK_PREPARED_POINT,
    PRESS_ORDINARY_AUTO_COMBAT,
    CAPTURE_COMBAT,
    RESTORE_AUTO_COMBAT,
    MAINTAIN_AUTO_PANEL
}
