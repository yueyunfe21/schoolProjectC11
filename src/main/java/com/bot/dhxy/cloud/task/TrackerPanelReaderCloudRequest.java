package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

/**
 * Raw tracker-panel image request for cloud-owned task tracker reading.
 *
 * @param taskCode task family currently reading the left tracker, one of `wuhuan`, `xiuluo`, or
 *                 `wubei`; never used as OCR/title business truth by the local reader
 * @param phase caller phase label for diagnostics and rollout sampling; nullable but normally set
 * @param source caller/source label for traceability; nullable
 * @param imagePayloadBase64 PNG tracker crop payload encoded as base64; required in production
 * @param payloadMimeType payload MIME type; must be `image/png`
 * @param imageSha256 SHA-256 of the PNG payload, used for log redaction and replay matching
 * @param imageMode semantic image type such as `TRACKER_PANEL_CROP` or `DETAIL_BLOCK_CROP`
 * @param imageOriginWindowX crop origin X in game-window-relative pixels, non-negative
 * @param imageOriginWindowY crop origin Y in game-window-relative pixels, non-negative
 * @param requestedLinkIndex requested link index for probe/multi-link flows, or `-1`
 * @param selectionPolicy cloud-side selection policy such as `FIRST_LINK`, `PROBE_INDEX`, or
 *                        `ALL_LINKS_ONLY`
 */
@Value
@Builder(toBuilder = true)
public class TrackerPanelReaderCloudRequest {
    String taskCode;
    String phase;
    String source;
    String imagePayloadBase64;
    String payloadMimeType;
    String imageSha256;
    String imageMode;
    int imageOriginWindowX;
    int imageOriginWindowY;
    @Builder.Default
    int requestedLinkIndex = -1;
    String selectionPolicy;
    /**
     * CR248: task key already established by the local raw title-template match (e.g.
     * {@code wubei.sancang_fengmo}). In detail modes the cloud must trust it and must not re-run
     * title matching, so both the live and snapshot entries share one title judge and one ROI.
     */
    String taskKey;
}
