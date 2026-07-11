package com.bot.dhxy.model.npc;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.task.model.TaskType;

import java.util.List;

/**
 * Immutable input for the NPC smart-click pipeline.
 *
 * <p>Coordinates are logical in-game map coordinates, not screen pixels. OCR scan regions are
 * resolved later by vision memory so task code does not bypass the current ROI policy.</p>
 *
 * @param player current player identity for player-anchor formula; nullable when that strategy is
 *               not useful.
 * @param mapName target map name for memory and formula strategies.
 * @param mapX logical in-game X coordinate of the target.
 * @param mapY logical in-game Y coordinate of the target.
 * @param npcName target NPC/monster name or OCR keyword.
 * @param tuneX screen-pixel X correction for formula clicks.
 * @param tuneY screen-pixel Y correction for formula clicks.
 * @param expectedDialogTemplatePath green-option template used to verify success.
 * @param expectedDialogRawTemplatePath raw option template used to verify success without green
 *                                      washing; blank means use the green-washed verifier.
 * @param expectedDialogTemplatePaths optional green-option templates; success means any one is
 *                                    visible. When empty, {@code expectedDialogTemplatePath} is
 *                                    used for backward compatibility.
 * @param deferDialogVerificationToTask true when the task phase owns the post-NPC dialog handling
 *                                      itself. The NPC click pipeline then only submits the NPC
 *                                      click and skips expected-dialog verification.
 * @param roamingTarget true when the logical coordinate may come from a task refresh instead of a
 *                      static NPC table. This flag is retained for future roaming-specific strategy
 *                      selection; it does not currently skip coordinate-formula clicking.
 * @param tooltipType expected visual tooltip category for this target; non-null values control
 *                    whether the task-tooltip template strategy is allowed.
 * @param tooltipTemplatePath optional tooltip template path; null/blank uses the standard task
 *                            tooltip template.
 * @param tooltipFirst true when a task-specific target should try visible tooltip candidates before
 *                     learned/direct scene-click paths. This is used for 五倍显形镜 targets where a
 *                     story dialog may cover the target body but the tooltip remains clickable.
 * @param closeStoryBeforeDirectSceneClick true when a task allows clicking through a blocking story
 *                                        dialog before direct body/name click strategies. Tooltip
 *                                        matching still runs first when {@code tooltipFirst} is true.
 * @param targetEvidence whether task logic already confirmed the target exists, or is only probing
 *                       a possible target. This lets the smart-click pipeline keep confirmed combat
 *                       clicks thorough while making speculative fallbacks cheap.
 * @param targetRole whether this request is clicking a combat target, a task giver, or another
 *                   interaction NPC. The smart-click pipeline uses this for strategy/log decisions
 *                   that differ between entering battle and opening ordinary NPC dialogs.
 * @param sourceTask task that produced this request, for example 五环/五倍/修罗. This is diagnostic
 *                   and allows task-scoped strategy tuning without guessing from NPC names.
 * @param consumeStoryDialogVisibleEvents CR255: true only for the 修罗 accept-phase smart click.
 *                                        At the FIFO queue's natural boundary the pipeline then
 *                                        reads (in-memory, zero I/O) fresh STORY_DIALOG_VISIBLE
 *                                        events for this window/task, fast-clicks the known small
 *                                        story dialog once per event sequence, and restarts the
 *                                        smart session. The default keeps every other smart click
 *                                        free of any story-event behavior.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class NpcClickRequest {
    PlayerCharacter player;
    String mapName;
    int mapX;
    int mapY;
    String npcName;
    int tuneX;
    int tuneY;
    String expectedDialogTemplatePath;
    String expectedDialogRawTemplatePath;
    @Builder.Default
    List<String> expectedDialogTemplatePaths = List.of();
    @Builder.Default
    boolean deferDialogVerificationToTask = false;
    boolean roamingTarget;
    @Builder.Default
    NpcTooltipType tooltipType = NpcTooltipType.TASK;
    String tooltipTemplatePath;
    @Builder.Default
    boolean tooltipFirst = false;
    @Builder.Default
    boolean closeStoryBeforeDirectSceneClick = false;
    @Builder.Default
    NpcTargetEvidence targetEvidence = NpcTargetEvidence.CONFIRMED;
    @Builder.Default
    NpcRole targetRole = NpcRole.INTERACTION_TARGET;
    @Builder.Default
    TaskType sourceTask = TaskType.UNKNOWN;
    @Builder.Default
    boolean consumeStoryDialogVisibleEvents = false;

    /**
     * Build a fixed-coordinate target request. OCR regions are resolved later by vision memory.
     */
    public static NpcClickRequest fixed(PlayerCharacter player,
                                        String mapName,
                                        int mapX,
                                        int mapY,
                                        String npcName,
                                        String expectedDialogTemplatePath) {
        return NpcClickRequest.builder()
                .player(player)
                .mapName(mapName)
                .mapX(mapX)
                .mapY(mapY)
                .npcName(npcName)
                .expectedDialogTemplatePath(expectedDialogTemplatePath)
                .build();
    }

    /**
     * Build a tuned fixed-coordinate target request. OCR regions are resolved later by vision memory.
     */
    public static NpcClickRequest fixedWithTune(PlayerCharacter player,
                                                String mapName,
                                                int mapX,
                                                int mapY,
                                                String npcName,
                                                int tuneX,
                                                int tuneY,
                                                String expectedDialogTemplatePath) {
        return NpcClickRequest.builder()
                .player(player)
                .mapName(mapName)
                .mapX(mapX)
                .mapY(mapY)
                .npcName(npcName)
                .tuneX(tuneX)
                .tuneY(tuneY)
                .expectedDialogTemplatePath(expectedDialogTemplatePath)
                .build();
    }

    /**
     * Build a roaming-target request. OCR regions are resolved later by vision memory.
     */
    public static NpcClickRequest roaming(PlayerCharacter player,
                                          String mapName,
                                          int mapX,
                                          int mapY,
                                          String npcName,
                                          String expectedDialogTemplatePath) {
        return NpcClickRequest.builder()
                .player(player)
                .mapName(mapName)
                .mapX(mapX)
                .mapY(mapY)
                .npcName(npcName)
                .expectedDialogTemplatePath(expectedDialogTemplatePath)
                .roamingTarget(true)
                .build();
    }

}
