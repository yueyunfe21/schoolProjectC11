package com.bot.dhxy.model.npc;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.model.PlayerCharacter;

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
 * @param roamingTarget true when the logical coordinate may come from a task refresh instead of a
 *                      static NPC table. This flag is retained for future roaming-specific strategy
 *                      selection; it does not currently skip coordinate-formula clicking.
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
    boolean roamingTarget;

    /**
     * Build a fixed-coordinate target request. OCR regions are resolved later by vision memory.
     */
    public static NpcClickRequest fixed(PlayerCharacter player,
                                        String mapName,
                                        int mapX,
                                        int mapY,
                                        String npcName,
                                        String expectedDialogTemplatePath) {
        return new NpcClickRequest(player, mapName, mapX, mapY, npcName,
                0, 0, expectedDialogTemplatePath, false);
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
        return new NpcClickRequest(player, mapName, mapX, mapY, npcName,
                tuneX, tuneY, expectedDialogTemplatePath, false);
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
        return new NpcClickRequest(player, mapName, mapX, mapY, npcName,
                0, 0, expectedDialogTemplatePath, true);
    }

}
