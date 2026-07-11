package com.bot.dhxy.task.pause;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.pause.TaskPauseResumeFingerprint;
import com.bot.dhxy.model.pause.TaskPauseResumeReconcileResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingSnapshot;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * CR160 pause/resume reconciler for task-owned checkpoint and park-wait boundaries.
 *
 * <p>This service compares lightweight runtime facts captured before a pause wait with the facts
 * available after resume. It either compensates automation-owned cache/timer age and lets the task
 * continue its original phase, or clears volatile state and asks the task to enter its own
 * hot-start fallback. Real game-world TTLs are deliberately outside this service.</p>
 */
@Slf4j
@Service
public class TaskPauseResumeReconciler {

    private static final long VISIBLE_DIALOG_FINGERPRINT_MAX_AGE_MS = 8_000L;

    public TaskPauseResumeFingerprint capture(TaskExecutionContext context,
                                              TaskType taskType,
                                              String phase,
                                              String waitReason) {
        WindowRuntimeContext runtime = context == null ? null : context.getWindowRuntimeContext();
        long now = System.currentTimeMillis();
        if (runtime == null) {
            return TaskPauseResumeFingerprint.builder()
                    .taskType(taskType == null ? TaskType.UNKNOWN : taskType)
                    .taskCode(context == null ? null : context.getTaskCode())
                    .phase(normalize(phase))
                    .waitReason(normalize(waitReason))
                    .capturedAtMs(now)
                    .build();
        }
        WindowNativeBinding binding = runtime.getNativeBinding();
        return TaskPauseResumeFingerprint.builder()
                .taskType(taskType == null ? TaskType.UNKNOWN : taskType)
                .taskCode(context == null ? null : context.getTaskCode())
                .windowId(runtime.getWindowId())
                .hwnd(binding == null ? null : binding.getNativeHandle())
                .phase(normalize(phase))
                .waitReason(normalize(waitReason))
                .actionState(runtime.getGameState().getCurrentActionState())
                .preparedAction(preparedFact(runtime.getPreparedDialogAction()))
                .visibleDialog(dialogFact(runtime.getVisibleDialogSnapshot(VISIBLE_DIALOG_FINGERPRINT_MAX_AGE_MS)
                        .orElse(null)))
                .pathing(pathingFact(runtime.getPathingSnapshot()))
                .capturedAtMs(now)
                .build();
    }

    public TaskPauseResumeReconcileResult reconcileAfterPause(TaskPauseResumeFingerprint before,
                                                              TaskExecutionContext context,
                                                              long pauseBlockedMs) {
        if (pauseBlockedMs <= 0L) {
            return TaskPauseResumeReconcileResult.noPause();
        }
        WindowRuntimeContext runtime = context == null ? null : context.getWindowRuntimeContext();
        TaskPauseResumeFingerprint after = capture(
                context,
                before == null ? TaskType.UNKNOWN : before.getTaskType(),
                before == null ? null : before.getPhase(),
                before == null ? null : before.getWaitReason());
        String mismatchReason = mismatchReason(before, after);
        if ("matched".equals(mismatchReason)) {
            List<String> compensatedTimers = runtime == null
                    ? List.of()
                    : runtime.compensateVolatileAutomationTimersAfterPause(
                    pauseBlockedMs, reconcileSource(before, "matched"));
            TaskPauseResumeReconcileResult result =
                    TaskPauseResumeReconcileResult.matched(pauseBlockedMs, compensatedTimers);
            log.info("[cr160 pause-resume] reconcile result: task={} phase={} waitReason={} windowId={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={}",
                    after.getTaskType(), after.getPhase(), after.getWaitReason(), after.getWindowId(),
                    pauseBlockedMs, result.isFingerprintMatched(), result.getMismatchReason(),
                    result.getCompensatedTimers(), result.getClearedVolatileState(),
                    result.isFallbackTaskHotStart());
            return result;
        }

        List<String> clearedVolatileState = runtime == null
                ? List.of()
                : runtime.clearPauseResumeVolatileState(reconcileSource(before, mismatchReason));
        TaskPauseResumeReconcileResult result =
                TaskPauseResumeReconcileResult.fallback(pauseBlockedMs, mismatchReason, clearedVolatileState);
        log.warn("[cr160 pause-resume] reconcile result: task={} phase={} waitReason={} windowId={} pauseBlockedMs={} fingerprintMatched={} mismatchReason={} compensatedTimers={} clearedVolatileState={} fallbackTaskHotStart={}",
                after.getTaskType(), after.getPhase(), after.getWaitReason(), after.getWindowId(),
                pauseBlockedMs, result.isFingerprintMatched(), result.getMismatchReason(),
                result.getCompensatedTimers(), result.getClearedVolatileState(),
                result.isFallbackTaskHotStart());
        return result;
    }

    private String mismatchReason(TaskPauseResumeFingerprint before, TaskPauseResumeFingerprint after) {
        if (before == null) {
            return "missing-before-fingerprint";
        }
        if (after == null) {
            return "missing-after-fingerprint";
        }
        if (!Objects.equals(before.getWindowId(), after.getWindowId())) {
            return "window:" + before.getWindowId() + "->" + after.getWindowId();
        }
        if (!Objects.equals(before.getPhase(), after.getPhase())) {
            return "phase:" + before.getPhase() + "->" + after.getPhase();
        }
        if (before.getActionState() != after.getActionState()) {
            return "action-state:" + before.getActionState() + "->" + after.getActionState();
        }
        if (!Objects.equals(before.getPreparedAction(), after.getPreparedAction())) {
            if (before.getPreparedAction() != null && after.getPreparedAction() == null) {
                return "prepared-action:missing";
            }
            if (before.getPreparedAction() == null) {
                return "prepared-action:new";
            }
            return "prepared-action:changed";
        }
        if (!Objects.equals(before.getVisibleDialog(), after.getVisibleDialog())) {
            if (before.getVisibleDialog() != null && after.getVisibleDialog() == null) {
                return "visible-dialog:missing";
            }
            if (before.getVisibleDialog() == null) {
                return "visible-dialog:new";
            }
            return "visible-dialog:changed";
        }
        if (!Objects.equals(before.getPathing(), after.getPathing())) {
            return "pathing:changed";
        }
        return "matched";
    }

    private TaskPauseResumeFingerprint.PreparedActionFact preparedFact(PreparedDialogAction action) {
        if (action == null) {
            return null;
        }
        return TaskPauseResumeFingerprint.PreparedActionFact.builder()
                .operation(action.getOperation())
                .targetKeyword(action.getTargetKeyword())
                .dialogType(action.getDialogType())
                .intentId(action.getIntentId())
                .absoluteX(action.getAbsoluteX())
                .absoluteY(action.getAbsoluteY())
                .validationLeft(action.getValidationLeft())
                .validationTop(action.getValidationTop())
                .validationRight(action.getValidationRight())
                .validationBottom(action.getValidationBottom())
                .fingerprint(action.getFingerprint())
                .clickRequired(action.isClickRequired())
                .build();
    }

    private TaskPauseResumeFingerprint.VisibleDialogFact dialogFact(WindowDialogSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        int[] rect = snapshot.getDialogRect();
        return TaskPauseResumeFingerprint.VisibleDialogFact.builder()
                .type(snapshot.getType())
                .left(rect == null || rect.length < 4 ? null : rect[0])
                .top(rect == null || rect.length < 4 ? null : rect[1])
                .right(rect == null || rect.length < 4 ? null : rect[2])
                .bottom(rect == null || rect.length < 4 ? null : rect[3])
                .build();
    }

    private TaskPauseResumeFingerprint.PathingFact pathingFact(WindowPathingSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        WindowPathingIntent intent = snapshot.getIntent();
        return TaskPauseResumeFingerprint.PathingFact.builder()
                .state(snapshot.getState())
                .intentId(intent == null ? null : intent.getIntentId())
                .intentSource(intent == null ? null : intent.getSource())
                .targetMapName(intent == null ? null : intent.getTargetMapName())
                .targetX(intent == null ? null : intent.getTargetX())
                .targetY(intent == null ? null : intent.getTargetY())
                .intentType(intent == null ? null : intent.getType())
                .currentMapName(snapshot.getCurrentMapName())
                .currentX(snapshot.getCurrentX())
                .currentY(snapshot.getCurrentY())
                .probeInProgress(snapshot.isProbeInProgress())
                .dialogBlocking(snapshot.isDialogBlocking())
                .build();
    }

    private String reconcileSource(TaskPauseResumeFingerprint fingerprint, String reason) {
        return "cr160:" + normalize(fingerprint == null ? null : fingerprint.getTaskCode())
                + ":" + normalize(fingerprint == null ? null : fingerprint.getPhase())
                + ":" + normalize(fingerprint == null ? null : fingerprint.getWaitReason())
                + ":" + normalize(reason);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
