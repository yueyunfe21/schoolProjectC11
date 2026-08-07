package com.bot.dhxy.cloud.turn.protocol;

/**
 * Arguments for exactly one Cloud-authorized 新手 mechanical action.
 *
 * @param action required closed mechanical action
 * @param recoveryTemplateName allow-listed filename, present only for
 *                             {@code CLICK_RECOVERY_TEMPLATE}
 * @param screenX source-frame screen-absolute click x, present only for
 *                {@code CLICK_PREPARED_POINT} or {@code CAPTURE_COMBAT}
 * @param screenY source-frame screen-absolute click y, present only for
 *                {@code CLICK_PREPARED_POINT} or {@code CAPTURE_COMBAT}
 * @param sourceWindowLeft source capture window screen-absolute left
 * @param sourceWindowTop source capture window screen-absolute top
 * @param sourceWindowWidth source capture window width in pixels
 * @param sourceWindowHeight source capture window height in pixels
 */
public record TurnXinshouMechanicalArguments(
        TurnXinshouMechanicalAction action,
        String recoveryTemplateName,
        Integer screenX,
        Integer screenY,
        Integer sourceWindowLeft,
        Integer sourceWindowTop,
        Integer sourceWindowWidth,
        Integer sourceWindowHeight) {

    public TurnXinshouMechanicalArguments(
            TurnXinshouMechanicalAction action,
            String recoveryTemplateName) {
        this(action, recoveryTemplateName, null, null, null, null, null, null);
    }
}
