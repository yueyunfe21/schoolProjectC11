package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Shared HTTPS-turn window metadata (identical in both repositories).
 *
 * <p>For a task-start window it also carries the baseline role/team/startup authority envelope. The Client owns
 * the exact same-batch session id and the one-shot CR212 preflight evidence. Cloud resolves that evidence once,
 * then projects the effective {@code windowRole}, {@code localLeaderPresent}, and {@code localSupportMember} into
 * the immutable task context; it does not guess from later runtime observations or create a second session.</p>
 *
 * <p>The two authority booleans are boxed {@link Boolean} and all six components are
 * {@code @JsonInclude(NON_NULL)} so a missing fact stays distinguishable from a legitimate {@code false} and
 * legacy (non-task-start) metadata serializes byte-shape identically to before. The legacy constructors exist
 * only so non-task-start callers keep compiling; they supply {@code null} for all six facts, and a request
 * carrying a {@code taskStartRequest} must supply them (the validator requires them non-null).</p>
 */
public record TurnWindowMetadata(
        String deviceId,
        String windowId,
        String windowTitle,
        String nativeHandle,
        long processId,
        TurnWindowRect windowRect,
        boolean pauseRequested,
        boolean stopRequested,
        @JsonInclude(JsonInclude.Include.NON_NULL) TurnPathingSnapshot pathingSnapshot,
        @JsonInclude(JsonInclude.Include.NON_NULL) String windowRole,
        @JsonInclude(JsonInclude.Include.NON_NULL) String localTeamSessionKey,
        @JsonInclude(JsonInclude.Include.NON_NULL) String localLeaderWindowId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean localLeaderPresent,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean localSupportMember,
        @JsonInclude(JsonInclude.Include.NON_NULL) String startupMode,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean localTeamRolePreflightComplete,
        @JsonInclude(JsonInclude.Include.NON_NULL) String localTeamRolePreflightSessionKey,
        @JsonInclude(JsonInclude.Include.NON_NULL) String localTeamRoleTooltipGroupHash,
        @JsonInclude(JsonInclude.Include.NON_NULL) String localTeamRoleTooltipMaskBase64,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean localTeamRoleTooltipRepresentative) {

    /** Retains the pre-CR212 wire constructor for non-preflight callers. */
    public TurnWindowMetadata(
            String deviceId, String windowId, String windowTitle, String nativeHandle, long processId,
            TurnWindowRect windowRect, boolean pauseRequested, boolean stopRequested,
            TurnPathingSnapshot pathingSnapshot, String windowRole, String localTeamSessionKey,
            String localLeaderWindowId, Boolean localLeaderPresent, Boolean localSupportMember, String startupMode) {
        this(deviceId, windowId, windowTitle, nativeHandle, processId, windowRect, pauseRequested, stopRequested,
                pathingSnapshot, windowRole, localTeamSessionKey, localLeaderWindowId, localLeaderPresent,
                localSupportMember, startupMode, null, null, null, null, null);
    }

    public TurnWindowMetadata(
            String deviceId,
            String windowId,
            String windowTitle,
            String nativeHandle,
            long processId,
            TurnWindowRect windowRect,
            boolean pauseRequested,
            boolean stopRequested) {
        this(deviceId, windowId, windowTitle, nativeHandle, processId, windowRect, pauseRequested, stopRequested,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public TurnWindowMetadata(
            String deviceId,
            String windowId,
            String windowTitle,
            String nativeHandle,
            long processId,
            TurnWindowRect windowRect,
            boolean stopRequested) {
        this(deviceId, windowId, windowTitle, nativeHandle, processId, windowRect, false, stopRequested,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public TurnWindowMetadata(
            String deviceId,
            String windowId,
            String windowTitle,
            String nativeHandle,
            long processId,
            TurnWindowRect windowRect,
            boolean pauseRequested,
            boolean stopRequested,
            TurnPathingSnapshot pathingSnapshot) {
        this(deviceId, windowId, windowTitle, nativeHandle, processId, windowRect, pauseRequested, stopRequested,
                pathingSnapshot, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Tests whether another turn still addresses the same native game-window identity.
     *
     * <p>Pause, stop, pathing and startup/role facts are live turn state and may legitimately change between an
     * action preflight and its outcome. They must never make an otherwise identical HWND fail consumer binding.
     * Device/window ids, HWND, PID, title and screen rectangle remain strict.</p>
     *
     * @param other outcome metadata to compare; nullable and never a match when absent
     * @return true only when all six stable identity components match exactly
     */
    public boolean hasSameStableWindowIdentity(TurnWindowMetadata other) {
        return other != null
                && processId == other.processId
                && Objects.equals(deviceId, other.deviceId)
                && Objects.equals(windowId, other.windowId)
                && Objects.equals(windowTitle, other.windowTitle)
                && Objects.equals(nativeHandle, other.nativeHandle)
                && Objects.equals(windowRect, other.windowRect);
    }
}
