package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.TurnFrame;
import com.bot.dhxy.cloud.turn.TurnPngCodec;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.model.quest.QuestDetailCapture;
import com.bot.dhxy.service.QuestManagerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Objects;

/** Closed adapter for the two permanent-local QuestManagerService turn operations. */
@Component
public final class QuestLocalOperationExecutor {

    private final QuestManagerService questManagerService;
    private final TurnPngCodec pngCodec;
    private final ObjectMapper objectMapper;

    public QuestLocalOperationExecutor(QuestManagerService questManagerService,
                                       TurnPngCodec pngCodec,
                                       ObjectMapper objectMapper) {
        this.questManagerService = Objects.requireNonNull(questManagerService, "questManagerService");
        this.pngCodec = Objects.requireNonNull(pngCodec, "pngCodec");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute one validated Quest local-Service call from outside the input worker.
     *
     * @param call typed local-Service call; only QUEST_ACTIVATE and QUEST_CAPTURE_DETAIL are supported.
     * @param sourceStepIndex zero-based action step index attached to a successful Quest-detail frame.
     * @return completed mechanical result, or a fail-closed result before any invalid Service invocation.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call, int sourceStepIndex) {
        if (call == null || call.operation() == null) {
            return LocalServiceExecution.failed("INVALID_LOCAL_SERVICE_CALL", null);
        }
        return switch (call.operation()) {
            case QUEST_ACTIVATE -> sourceStepIndex < 0
                    ? LocalServiceExecution.failed("INVALID_SOURCE_STEP_INDEX", null)
                    : executeActivate(call);
            case QUEST_CAPTURE_DETAIL -> sourceStepIndex < 0
                    ? LocalServiceExecution.failed("INVALID_SOURCE_STEP_INDEX", null)
                    : executeCaptureDetail(call, sourceStepIndex);
            default -> LocalServiceExecution.failed("UNSUPPORTED_LOCAL_OPERATION", null);
        };
    }

    private LocalServiceExecution executeActivate(TurnLocalServiceCall call) {
        if (!hasOnlyQuestArguments(call)
                || isBlank(call.quest().task())
                || call.quest().keepOpen() == null) {
            return LocalServiceExecution.failed("INVALID_QUEST_ARGUMENTS", null);
        }

        boolean activated = questManagerService.activateTaskIfPresent(
                call.quest().task(), call.quest().keepOpen());
        return LocalServiceExecution.completed(
                "OK", json(new QuestActivateResult(activated)), null);
    }

    private LocalServiceExecution executeCaptureDetail(TurnLocalServiceCall call, int sourceStepIndex) {
        if (!hasOnlyQuestArguments(call)
                || isBlank(call.quest().task())
                || call.quest().keepOpen() != null) {
            return LocalServiceExecution.failed("INVALID_QUEST_ARGUMENTS", null);
        }

        QuestDetailCapture capture = questManagerService.captureCurrentQuestDetailForTask(call.quest().task());
        BufferedImage image = capture == null ? null : capture.image();
        try {
            if (image == null) {
                return LocalServiceExecution.failed(
                        "QUEST_DETAIL_CAPTURE_FAILED", json(new QuestCaptureResult(false)));
            }
            TurnRegion region = new TurnRegion(
                    capture.screenX(), capture.screenY(), image.getWidth(), image.getHeight());
            TurnFrame frame = pngCodec.encode(image, TurnFramePurpose.QUEST_DETAIL, region, sourceStepIndex);
            return LocalServiceExecution.completed(
                    "OK", json(new QuestCaptureResult(true)), frame);
        } finally {
            if (image != null) {
                image.flush();
            }
        }
    }

    private static boolean hasOnlyQuestArguments(TurnLocalServiceCall call) {
        return call.bag() == null
                && call.ui() == null
                && call.giveItem() == null
                && call.quest() != null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Quest local result cannot be serialized", e);
        }
    }

    private record QuestActivateResult(boolean activated) {
    }

    private record QuestCaptureResult(boolean captured) {
    }
}
