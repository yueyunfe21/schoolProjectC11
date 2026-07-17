package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Closed wire command for the NPC yellow-target local observation, mirroring the Cloud
 * {@code NpcYellowTargetMacroCommand} and the already source-approved
 * {@code NpcClickYellowTargetLocalObservationMechanics.ScanRegion} field-for-field so the handler can rebuild
 * an identical caller-selected scan region and drive the committed shape-only yellow-candidate scan.
 *
 * <p>{@code left/top/right/bottom} is the caller-selected window-relative positive-area scan box and
 * {@code skipDefaultMask} chooses the committed default-mask behavior. The Cloud caller keeps every target /
 * OCR-matcher / strategy / click / fallback decision; this command carries no target, verdict or
 * owner/session/queue/retry.</p>
 *
 * <p>This is one of the standalone yellow-target contract-cohort types; it is not yet a
 * {@code RemoteLocalMacroCommandPayload} variant, so the generic LOCAL_MACRO sealed permits and codec are
 * left untouched until the shared wiring lands.</p>
 */
@Value
@Jacksonized
public class RemoteNpcYellowTargetMacroCommandPayload {
    int left;
    int top;
    int right;
    int bottom;
    boolean skipDefaultMask;

    @Builder
    public RemoteNpcYellowTargetMacroCommandPayload(
            int left,
            int top,
            int right,
            int bottom,
            boolean skipDefaultMask) {
        if (right <= left || bottom <= top) {
            throw new IllegalArgumentException("scan region must be a positive-area window-relative box");
        }
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.skipDefaultMask = skipDefaultMask;
    }
}
