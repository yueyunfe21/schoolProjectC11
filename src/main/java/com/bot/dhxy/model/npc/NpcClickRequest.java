package com.bot.dhxy.model.npc;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.PlayerCharacter;

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
 * @param expectedDialogTemplatePaths optional green-option templates; success means any one is
 *                                    visible. When empty, {@code expectedDialogTemplatePath} is
 *                                    used for backward compatibility.
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
 */
@Value
@Builder
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
    @Builder.Default
    List<String> expectedDialogTemplatePaths = List.of();
    boolean roamingTarget;
    @Builder.Default
    NpcTooltipType tooltipType = NpcTooltipType.TASK;
    String tooltipTemplatePath;
    @Builder.Default
    boolean tooltipFirst = false;
    @Builder.Default
    boolean closeStoryBeforeDirectSceneClick = false;

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
