package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.UICleanerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Closed adapter for the permanent-local UICleanerService turn operations. */
@Component
public final class UiLocalOperationExecutor {

    private final UICleanerService uiCleanerService;
    private final InputSequences inputSequences;
    private final ObjectMapper objectMapper;

    public UiLocalOperationExecutor(UICleanerService uiCleanerService,
                                    InputSequences inputSequences,
                                    ObjectMapper objectMapper) {
        this.uiCleanerService = Objects.requireNonNull(uiCleanerService, "uiCleanerService");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute one validated UI local-Service call from outside the input worker.
     *
     * <p>The broad, generic, and lightweight operations retain the queues already owned by
     * {@link UICleanerService}. This adapter creates the one required exclusive callback only for the
     * X2 direct macro, preserving its capture-and-click sequence. A dispatcher must not wrap this whole
     * adapter in another exclusive callback.</p>
     *
     * @param call typed local-Service call; only the closed UICleanerService operations are supported.
     * @return completed mechanical result with typed JSON, or a fail-closed result before physical input.
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call) {
        if (call == null || call.operation() == null) {
            return LocalServiceExecution.failed("INVALID_LOCAL_SERVICE_CALL", null);
        }
        return switch (call.operation()) {
            case UI_CLEAN_ALL -> executeCleanAll(call);
            case UI_CLOSE_GENERIC_WINDOWS -> executeCloseGenericWindows(call);
            case UI_PROBE_GENERIC_CLOSE -> executeProbeGenericClose(call);
            case UI_CLEAN_LIGHTWEIGHT -> executeCleanLightweight(call);
            case UI_CLOSE_MAP_SEARCH_INPUT_BY_X2 -> executeCloseMapSearchInputByX2(call);
            case UI_TAP_CENTER_DISMISS_OVERLAY -> executeTapCenterDismissOverlay(call);
            default -> LocalServiceExecution.failed("UNSUPPORTED_LOCAL_OPERATION", null);
        };
    }

    private LocalServiceExecution executeCleanAll(TurnLocalServiceCall call) {
        if (hasAnyArguments(call)) {
            return LocalServiceExecution.failed("INVALID_UI_ARGUMENTS", null);
        }
        uiCleanerService.cleanUpAll();
        return completed(call.operation(), true);
    }

    private LocalServiceExecution executeCloseGenericWindows(TurnLocalServiceCall call) {
        if (hasAnyArguments(call)) {
            return LocalServiceExecution.failed("INVALID_UI_ARGUMENTS", null);
        }
        return completed(call.operation(), uiCleanerService.closeAllGenericWindows());
    }

    private LocalServiceExecution executeProbeGenericClose(TurnLocalServiceCall call) {
        if (hasAnyArguments(call)) {
            return LocalServiceExecution.failed("INVALID_UI_ARGUMENTS", null);
        }
        Boolean present = uiCleanerService.probeGenericCloseButtonPresent("turn:ui-probe-generic-close");
        return present == null
                ? LocalServiceExecution.failed("UI_GENERIC_CLOSE_PROBE_UNKNOWN", null)
                : completed(call.operation(), present);
    }

    private LocalServiceExecution executeCleanLightweight(TurnLocalServiceCall call) {
        String source = requireOnlyUiSource(call);
        if (source == null) {
            return LocalServiceExecution.failed("INVALID_UI_ARGUMENTS", null);
        }
        return completed(call.operation(), uiCleanerService.cleanLightweightInterruptions(source));
    }

    private LocalServiceExecution executeTapCenterDismissOverlay(TurnLocalServiceCall call) {
        if (call.ui() == null || call.ui().source() == null || call.ui().source().isBlank()) {
            return LocalServiceExecution.failed("INVALID_UI_ARGUMENTS", null);
        }
        return completed(call.operation(),
                uiCleanerService.tapClientCenterToDismissOverlay("turn:" + call.ui().source()));
    }

    private LocalServiceExecution executeCloseMapSearchInputByX2(TurnLocalServiceCall call) {
        String source = requireOnlyUiSource(call);
        if (source == null) {
            return LocalServiceExecution.failed("INVALID_UI_ARGUMENTS", null);
        }
        boolean closed = inputSequences.submitExclusiveAndWait(
                "turn:ui-close-map-search-x2:" + source,
                () -> call.ui().returnImmediatelyAfterClick()
                        ? uiCleanerService.closeMapSearchInputByX2Direct(source, false)
                        : uiCleanerService.closeMapSearchInputByX2Direct(source));
        return completed(call.operation(), closed);
    }

    private LocalServiceExecution completed(TurnLocalOperation operation, boolean handled) {
        return LocalServiceExecution.completed("OK", json(new UiOperationResult(operation, handled)), null);
    }

    private static boolean hasAnyArguments(TurnLocalServiceCall call) {
        return call.bag() != null || call.ui() != null || call.giveItem() != null || call.quest() != null;
    }

    private static String requireOnlyUiSource(TurnLocalServiceCall call) {
        if (call.bag() != null || call.ui() == null || call.giveItem() != null || call.quest() != null) {
            return null;
        }
        String source = call.ui().source();
        return source == null || source.isBlank() ? null : source;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("UI local result cannot be serialized", e);
        }
    }

    private record UiOperationResult(TurnLocalOperation operation, boolean handled) {
    }
}
