package com.bot.dhxy.model.npc;

import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.task.model.TaskType;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Canonical model for an NPC or monster target known by task logic.
 *
 * <p>The coordinates are logical in-game map coordinates, not screen pixels. Screen coordinates,
 * OCR regions, and current window base should be resolved later by navigation/click/vision
 * services so task code does not duplicate coordinate conversion rules. Do not pass this full model
 * into narrow services such as navigation when they only need map/coordinate fields; convert to that
 * service's request object instead.</p>
 */
@Value
@Builder
public class NpcTarget {
    /**
     * Stable optional key for config, memory records, and logs. Use a readable value such as
     * {@code xiuluo.acceptNpc} or {@code wuhuan.moyi}.
     */
    String key;

    /**
     * Map where this target should be found.
     */
    String mapName;

    /**
     * Primary in-game display name used for OCR/template matching.
     */
    String name;

    /**
     * Alternative display names or common OCR variants for the same target.
     */
    @Singular
    List<String> aliases;

    /**
     * Logical in-game X coordinate on {@link #mapName}.
     */
    int x;

    /**
     * Logical in-game Y coordinate on {@link #mapName}.
     */
    int y;

    /**
     * Whether this target is a task giver, combat target, interaction NPC, or debug-only target.
     */
    @Builder.Default
    NpcRole role = NpcRole.INTERACTION_TARGET;

    /**
     * Whether this target has a fixed coordinate, task-provided roaming coordinate, or loose drift.
     */
    @Builder.Default
    NpcMovementType movementType = NpcMovementType.UNKNOWN;

    /**
     * Screen-pixel X correction used only by formula click fallback.
     *
     * <p>The default follows the validated 五环 NPC body-click correction. Target-specific tuning
     * should only override this after an actual measured failure.</p>
     */
    @Builder.Default
    int tuneX = -10;

    /**
     * Screen-pixel Y correction used only by formula click fallback.
     */
    @Builder.Default
    int tuneY = 0;

    /**
     * Green dialog-option template used to verify that a click opened the expected dialog.
     */
    String expectedDialogTemplatePath;

    /**
     * Raw dialog-option template used by opt-in targets that should skip green washing during
     * expected-dialog verification. Null keeps the legacy green-washed verifier.
     */
    String expectedDialogRawTemplatePath;

    /**
     * True when the owning task phase will handle the opened dialog immediately after the NPC click.
     * This keeps NPC smart click from doing a duplicate visibility-only dialog verification.
     */
    @Builder.Default
    boolean deferDialogVerificationToTask = false;

    /**
     * Optional visual tooltip template above this target. Null falls back to the standard task
     * tooltip template in {@code NpcClickService}.
     */
    String tooltipTemplatePath;

    /**
     * Tooltip style expected above this target. Use {@link NpcTooltipType#NONE} for fixed transfer
     * NPCs such as 张闻 where probing for a task tooltip only wastes time before formula/OCR paths.
     */
    @Builder.Default
    NpcTooltipType tooltipType = NpcTooltipType.TASK;

    /**
     * Short origin label for logs, config migration, or learned OCR memory.
     */
    @Builder.Default
    String source = "npcTarget";

    /**
     * Convert this canonical target into the current smart-click request.
     *
     * @param player current player identity for player-anchor formula; nullable when unavailable.
     * @return click request using this target's map/name/coordinate/tuning/template fields.
     */
    public NpcClickRequest toClickRequest(PlayerCharacter player) {
        return toClickRequest(player, TaskType.UNKNOWN);
    }

    /**
     * Convert this canonical target into a smart-click request with task source metadata.
     *
     * @param player current player identity for player-anchor formula; nullable when unavailable.
     * @param sourceTask task currently requesting the click; null becomes {@link TaskType#UNKNOWN}.
     * @return click request using this target's map/name/coordinate/tuning/template/role fields.
     */
    public NpcClickRequest toClickRequest(PlayerCharacter player, TaskType sourceTask) {
        boolean roaming = movementType == NpcMovementType.ROAMING || movementType == NpcMovementType.FLOATING;
        return NpcClickRequest.builder()
                .player(player)
                .mapName(mapName)
                .mapX(x)
                .mapY(y)
                .npcName(name)
                .tuneX(tuneX)
                .tuneY(tuneY)
                .expectedDialogTemplatePath(expectedDialogTemplatePath)
                .expectedDialogRawTemplatePath(expectedDialogRawTemplatePath)
                .deferDialogVerificationToTask(deferDialogVerificationToTask)
                .roamingTarget(roaming)
                .tooltipType(tooltipType)
                .tooltipTemplatePath(tooltipTemplatePath)
                .targetRole(role)
                .sourceTask(sourceTask == null ? TaskType.UNKNOWN : sourceTask)
                .build();
    }
}
