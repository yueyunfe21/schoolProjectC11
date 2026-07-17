package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the {@code LOCAL_MACRO / NPC_PLAYER_ANCHOR} macro. Mirrors the Cloud
 * {@code NpcPlayerAnchorMacroCommand} field-for-field so the handler can restore the approved local
 * {@code NpcClickPlayerAnchorLocalObservationMechanics.ScanRegion} and drive the committed capture / optional
 * Alt+4 hide / purple-wash / connected-purple-blob observation without losing any caller-visible behavior.
 *
 * <p>{@code left/top/right/bottom} is the exact caller-decided window-relative scan region (positive-area
 * box); {@code prepareAlt4} chooses the committed Alt+4 player-name hide before the single capture;
 * {@code skipDefaultMask} skips the default full-window HUD/chat/shortcut mask. Carries no
 * owner/session/queue/retry.</p>
 *
 * <p>This is a standalone contract: it deliberately does not implement the shared
 * {@code RemoteLocalMacroCommandPayload} sealed hierarchy, carry a {@code RemoteLocalMacroKind}, or touch any
 * shared registration. Wiring it into the transport is a separate downstream integration step documented in
 * the cohort report.</p>
 */
@Value
@Jacksonized
public class RemoteNpcPlayerAnchorMacroCommandPayload {
    int left;
    int top;
    int right;
    int bottom;
    boolean prepareAlt4;
    boolean skipDefaultMask;

    @Builder
    public RemoteNpcPlayerAnchorMacroCommandPayload(
            int left,
            int top,
            int right,
            int bottom,
            boolean prepareAlt4,
            boolean skipDefaultMask) {
        if (!(right > left && bottom > top)) {
            throw new IllegalArgumentException("scan region must be a positive-area window-relative box");
        }
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.prepareAlt4 = prepareAlt4;
        this.skipDefaultMask = skipDefaultMask;
    }
}
