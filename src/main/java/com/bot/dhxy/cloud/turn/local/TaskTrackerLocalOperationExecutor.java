package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnTaskTrackerOperationResult;
import com.bot.dhxy.cloud.turn.local.tasktracker.TaskTrackerPanelCaptureLocalMechanics;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Same-turn adapter for the exact-window task-tracker capture mechanics. */
@Component
public final class TaskTrackerLocalOperationExecutor {

    private final TaskTrackerPanelCaptureLocalMechanics mechanics;
    private final WindowTaskContextHolder contextHolder;
    private final ObjectMapper objectMapper;

    public TaskTrackerLocalOperationExecutor(TaskTrackerPanelCaptureLocalMechanics mechanics,
                                             WindowTaskContextHolder contextHolder,
                                             ObjectMapper objectMapper) {
        this.mechanics = Objects.requireNonNull(mechanics, "mechanics");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute one validated capture while the dispatcher holds the input worker.
     *
     * @param call tracker-only local-service call
     * @param sourceStepIndex zero-based LOCAL_SERVICE step index used by the optional frame
     * @return completed closed state and an optional TASK_TRACKER_PANEL frame
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call, int sourceStepIndex) {
        if (call == null
                || call.operation() != TurnLocalOperation.TASK_TRACKER_CAPTURE_PANEL
                || call.taskTracker() == null
                || call.taskTracker().source() == null
                || call.taskTracker().source().isBlank()
                || sourceStepIndex < 0) {
            return LocalServiceExecution.failed("INVALID_TASK_TRACKER_ARGUMENTS", null);
        }
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        if (context == null || context.getNativeBinding() == null) {
            return LocalServiceExecution.failed("TASK_TRACKER_WINDOW_UNAVAILABLE", null);
        }

        TaskTrackerPanelCaptureLocalMechanics.CaptureResultDto captured = mechanics.capturePanel(
                context, context.getNativeBinding(), call.taskTracker().source());
        TurnTaskTrackerOperationResult.State state =
                TurnTaskTrackerOperationResult.State.valueOf(captured.state().name());
        TurnTaskTrackerOperationResult result = new TurnTaskTrackerOperationResult(
                state,
                captured.absoluteLeft(),
                captured.absoluteTop(),
                captured.panelWidth(),
                captured.panelHeight(),
                captured.panelSha256());

        TurnFrame frame = null;
        if (state == TurnTaskTrackerOperationResult.State.CAPTURED) {
            TurnRegion region = new TurnRegion(
                    captured.absoluteLeft(), captured.absoluteTop(),
                    captured.panelWidth(), captured.panelHeight());
            frame = new TurnFrame(
                    new TurnFrameMetadata(
                            TurnFramePurpose.TASK_TRACKER_PANEL,
                            "image/png",
                            captured.panelSha256(),
                            captured.panelWidth(),
                            captured.panelHeight(),
                            region,
                            sourceStepIndex),
                    captured.panelPngBytes());
        }
        return LocalServiceExecution.completed(code(state), json(result), frame);
    }

    private static String code(TurnTaskTrackerOperationResult.State state) {
        return "TASK_TRACKER_PANEL_" + state.name();
    }

    private String json(TurnTaskTrackerOperationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("task-tracker local result cannot be serialized", failure);
        }
    }
}
