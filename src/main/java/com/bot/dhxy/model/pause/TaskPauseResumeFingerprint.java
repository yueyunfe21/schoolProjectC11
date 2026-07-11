package com.bot.dhxy.model.pause;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.model.WindowPathingState;
import lombok.Builder;
import lombok.Value;

/**
 * Lightweight facts captured at a task pause checkpoint before the thread blocks.
 *
 * <p>The fingerprint intentionally stores identifiers and window-runtime facts only. It must not run
 * OCR/template matching or infer task business progress; task classes decide the hot-start fallback
 * after the reconciler reports whether these facts still match on resume.</p>
 */
@Value
@Builder
public class TaskPauseResumeFingerprint {
    TaskType taskType;
    String taskCode;
    String windowId;
    String hwnd;
    String phase;
    String waitReason;
    GameContext.ActionState actionState;
    PreparedActionFact preparedAction;
    VisibleDialogFact visibleDialog;
    PathingFact pathing;
    long capturedAtMs;

    @Value
    @Builder
    public static class PreparedActionFact {
        DialogOperation operation;
        String targetKeyword;
        DialogType dialogType;
        String intentId;
        int absoluteX;
        int absoluteY;
        int validationLeft;
        int validationTop;
        int validationRight;
        int validationBottom;
        String fingerprint;
        boolean clickRequired;
    }

    @Value
    @Builder
    public static class VisibleDialogFact {
        DialogType type;
        Integer left;
        Integer top;
        Integer right;
        Integer bottom;
    }

    @Value
    @Builder
    public static class PathingFact {
        WindowPathingState state;
        String intentId;
        String intentSource;
        String targetMapName;
        Integer targetX;
        Integer targetY;
        WindowPathingIntentType intentType;
        String currentMapName;
        Integer currentX;
        Integer currentY;
        boolean probeInProgress;
        boolean dialogBlocking;
    }
}
