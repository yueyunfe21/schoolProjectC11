package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * W-696-NPC-TOOLTIP-PREPAREDPOINT-CONTRACT-COHORT-1: closed DHXY-side wire command for the NPC
 * task-tooltip local macro. Mirrors the Cloud {@code NpcTaskTooltipMacroCommand} contract exactly: the
 * decided template path (trimmed, non-blank) and the ordered recommended scan regions.
 *
 * <p>Standalone contract type; binding it to the sealed {@code RemoteLocalMacroCommandPayload} transport
 * and a {@code RemoteLocalMacroKind} value is the deferred shared-integration seam.</p>
 */
@Value
@Jacksonized
public class RemoteNpcTaskTooltipMacroCommandPayload {

    String templatePath;
    List<ScanRegion> regions;
    String description;

    @Builder
    public RemoteNpcTaskTooltipMacroCommandPayload(String templatePath, List<ScanRegion> regions, String description) {
        // Baseline equivalence with TaskTooltipClickIntent: keep the literal templatePath (no trim, no
        // required) so a null/blank path reaches the local mechanics and maps to TEMPLATE_UNAVAILABLE;
        // normalize null regions to an empty list (empty maps to NOT_FOUND), else copy preserving order.
        this.templatePath = templatePath;
        this.regions = regions == null ? List.of() : List.copyOf(regions);
        this.description = description;
    }

    /** One caller-decided screen-absolute scan rectangle (right/bottom edges exclusive). */
    @Value
    @Jacksonized
    public static class ScanRegion {
        int x1;
        int y1;
        int x2;
        int y2;

        @Builder
        public ScanRegion(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
