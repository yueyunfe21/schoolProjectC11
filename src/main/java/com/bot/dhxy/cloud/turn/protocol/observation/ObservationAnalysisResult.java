package com.bot.dhxy.cloud.turn.protocol.observation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * TURN-40G: one Cloud analysis result for uploaded observation material. Results carry data and correlation only —
 * they never grant the local runner permission to execute an ordinary business action, and any actionable candidate
 * still travels through the existing prepared-candidate owner/CAS machinery.
 *
 * @param analysisId unique identity of this analysis result
 * @param resultType Cloud-defined result kind
 * @param roiKey optional key of the uploaded ROI this result answers
 * @param intentId optional pathing intent correlation
 * @param attemptId optional xiuluo attempt correlation
 * @param windowRelativeX optional window-relative X coordinate payload
 * @param windowRelativeY optional window-relative Y coordinate payload
 * @param mapName optional recognized map name for a pathing-coordinate response
 * @param coordinateX optional recognized logical map X coordinate
 * @param coordinateY optional recognized logical map Y coordinate
 * @param detail optional textual payload
 */
public record ObservationAnalysisResult(
        String analysisId,
        String resultType,
        @JsonInclude(JsonInclude.Include.NON_NULL) String roiKey,
        @JsonInclude(JsonInclude.Include.NON_NULL) String intentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String attemptId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer windowRelativeX,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer windowRelativeY,
        @JsonInclude(JsonInclude.Include.NON_NULL) String mapName,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer coordinateX,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer coordinateY,
        @JsonInclude(JsonInclude.Include.NON_NULL) String detail) {
}
