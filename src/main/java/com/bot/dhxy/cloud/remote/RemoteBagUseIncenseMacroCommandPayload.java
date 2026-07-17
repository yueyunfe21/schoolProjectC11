package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for {@code LOCAL_MACRO / BAG_USE_INCENSE}.
 *
 * <p>No template path crosses the wire. DHXY owns the fixed
 * {@code bag/sheyaoxiang_item.png} mechanical input.</p>
 */
@Value
@Jacksonized
public class RemoteBagUseIncenseMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;

    @Builder
    public RemoteBagUseIncenseMacroCommandPayload(RemoteLocalMacroKind macroKind) {
        if (macroKind != RemoteLocalMacroKind.BAG_USE_INCENSE) {
            throw new IllegalArgumentException("macroKind must be BAG_USE_INCENSE");
        }
        this.macroKind = macroKind;
    }
}
