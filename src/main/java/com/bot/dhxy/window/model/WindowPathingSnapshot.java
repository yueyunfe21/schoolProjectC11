package com.bot.dhxy.window.model;

import com.bot.dhxy.model.dialog.DialogPreparationPhase;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.DialogOperation;
import lombok.Builder;
import lombok.Value;

/**
 * Latest window-level observation for a registered pathing intent.
 */
@Value
@Builder(toBuilder = true)
public class WindowPathingSnapshot {
    @Builder.Default
    WindowPathingState state = WindowPathingState.NONE;
    WindowPathingIntent intent;
    String currentMapName;
    Integer currentX;
    Integer currentY;
    String message;
    /**
     * Wall-clock time when the observed map/coordinate last changed.
     *
     * <p>This is intentionally separate from {@link #updatedAtMs}: the watcher may refresh
     * the same pixels every second, but pathing should be considered stopped if the location
     * itself has not changed for the stopped-away threshold.</p>
     */
    @Builder.Default
    long locationChangedAtMs = System.currentTimeMillis();
    /** True once Runner observed any logical coordinate change during this exact intent. */
    @Builder.Default
    boolean coordinateMovementObserved = false;
    @Builder.Default
    long updatedAtMs = System.currentTimeMillis();
    /**
     * Wall-clock time when the current background mini-map probe started.
     *
     * <p>This is a watcher lifecycle marker only. A probe start must not refresh
     * {@link #updatedAtMs}, because no new map/coordinate observation has been produced yet.</p>
     */
    @Builder.Default
    long probeStartedAtMs = 0L;
    /**
     * Wall-clock time when the latest background mini-map probe completed.
     */
    @Builder.Default
    long probeFinishedAtMs = 0L;
    /**
     * True while the watcher is currently spending time in screenshot/template/OCR work.
     */
    @Builder.Default
    boolean probeInProgress = false;
    /**
     * True when the foreground navigation handoff intentionally left a UI overlay in an uncertain
     * state. The task that consumes this pathing snapshot should run a boundary cleanup before
     * doing NPC/dialog business clicks.
     */
    @Builder.Default
    boolean uiCleanupRecommended = false;
    String uiCleanupReason;
    @Builder.Default
    long uiCleanupRecommendedAtMs = 0L;
    /**
     * True when this pathing observation coincides with a fresh dialog that is visible, being
     * prepared by the watcher, or already prepared for task consumption.
     *
     * <p>A terminal pathing state with this flag must not be treated as "free to retry movement":
     * the task should first let the dialog preparation/click path finish, otherwise it can reopen
     * the world map or re-click the task tracker while the expected route/combat option is already
     * on screen.</p>
     */
    @Builder.Default
    boolean dialogBlocking = false;
    String dialogBlockingReason;
    DialogType dialogBlockingType;
    @Builder.Default
    long dialogBlockingDetectedAtMs = 0L;
    DialogPreparationPhase dialogPreparationPhase;
    DialogOperation dialogPreparationOperation;
    String dialogPreparationTarget;

    public static WindowPathingSnapshot idle() {
        return WindowPathingSnapshot.builder()
                .state(WindowPathingState.NONE)
                .message("no active pathing intent")
                .build();
    }

    public boolean hasActiveIntent() {
        return intent != null
                && state != WindowPathingState.NONE
                && state != WindowPathingState.ARRIVED;
    }
}
