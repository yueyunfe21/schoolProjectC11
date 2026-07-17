package com.bot.dhxy.cloud.task;

import com.bot.dhxy.model.npc.NpcClickRequest;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Request envelope for CR165 NPC_CLICK_SMART cloud-owned ordinary NPC dialog clicks.
 *
 * <p>The local runtime sends raw bound-window pixels plus target facts and reusable dialog/tooltip templates.
 * Any executable click returned by cloud must use window-relative pixels; local code only validates
 * the point against the game window and ROI before submitting real input. Logical map coordinates
 * such as player/target map X/Y remain explicitly labeled as map facts in the cloud context.</p>
 */
@Value
@Builder(toBuilder = true)
public class NpcClickSmartCloudRequest {
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String rawImagePath;
    String debugImageId;
    String sessionId;
    Roi roi;
    int windowWidth;
    int windowHeight;
    NpcClickRequest npcRequest;
    String taskCode;
    String source;
    String phase;
    String verificationMode;
    int attemptIndex;
    String attemptToken;
    String lastOutcomeStatus;
    String lastOutcomeReason;
    String lastAction;
    String lastClick;
    String lastCandidateBox;
    String playerName;
    String playerMapName;
    Integer playerMapX;
    Integer playerMapY;
    Integer tuneX;
    Integer tuneY;
    Boolean tooltipFirst;
    Boolean closeStoryBeforeDirectSceneClick;
    String windowId;
    String taskRunId;
    String policyVersion;
    String hwnd;
    @Builder.Default
    List<ScanRegion> scanRegions = List.of();
    @Builder.Default
    List<String> templateSpecs = List.of();

    @Value
    @Builder
    public static class Roi {
        int x;
        int y;
        int width;
        int height;
    }

    @Value
    @Builder
    public static class ScanRegion {
        int index;
        int windowX;
        int windowY;
        int width;
        int height;
        int screenX;
        int screenY;
        int screenWidth;
        int screenHeight;
        int windowBaseX;
        int windowBaseY;
    }
}
