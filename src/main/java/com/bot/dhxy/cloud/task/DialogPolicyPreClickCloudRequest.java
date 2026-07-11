package com.bot.dhxy.cloud.task;

import com.bot.dhxy.service.dialog.DialogHandleRequest;
import lombok.Builder;
import lombok.Value;

/**
 * Request envelope for CR167 DialogCloud pre-click option decisions.
 *
 * <p>The image payload is raw dialog/window pixels for the current bound game window. `roi` and all
 * executable cloud clicks use window-relative pixels, while `rawImagePath` remains diagnostic only
 * and must not become production authority.</p>
 *
 * <p>`detectedDialogType` is the local read-only classification that decides which cloud action id
 * is legal. For example, `STORY + CLICK_THROUGH` can only execute `STORY_CLICK_THROUGH`; the same
 * request on an option dialog must still use the option policy's action id contract.</p>
 *
 * <p>`DialogHandleRequest.greenTemplateSpecs` is exported into the cloud request context as
 * `greenTemplateSpec.N.name/templatePath/minOffsetX/maxOffsetX/randomRadiusY`. When the range is
 * deterministic, the context also includes `greenTemplateSpec.N.clickOffsetX/clickOffsetY`. Those
 * fields let DIALOG_POLICY compute the final WINDOW_RELATIVE click; DHXY only validates and executes
 * the returned click.</p>
 */
@Value
@Builder(toBuilder = true)
public class DialogPolicyPreClickCloudRequest {
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String rawImagePath;
    String debugImageId;
    Roi roi;
    int windowWidth;
    int windowHeight;
    DialogHandleRequest dialogRequest;
    String detectedDialogType;
    String taskCode;
    String source;
    String phase;
    String windowId;
    String taskRunId;
    String policyVersion;
    String hwnd;
    /**
     * CR232: outcome of a previously cloud-supplied enter-battle click for the same attempt, e.g.
     * {@code CLICK_FAILED}. When present the cloud confirms the failure and returns an explicit
     * NO_ACTION fallback instead of recognizing the static image afresh.
     */
    String priorClickOutcome;
    String priorClickAttemptId;

    @Value
    @Builder
    public static class Roi {
        int x;
        int y;
        int width;
        int height;
    }
}
