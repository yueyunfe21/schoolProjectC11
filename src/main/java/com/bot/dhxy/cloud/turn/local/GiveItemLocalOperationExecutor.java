package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnGiveItemOperationArguments;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.service.GiveItemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Closed adapter for the permanent-local GiveItemService turn operation. */
@Component
public final class GiveItemLocalOperationExecutor {

    private final GiveItemService giveItemService;
    private final ObjectMapper objectMapper;

    public GiveItemLocalOperationExecutor(GiveItemService giveItemService, ObjectMapper objectMapper) {
        this.giveItemService = Objects.requireNonNull(giveItemService, "giveItemService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Execute one validated GiveItem local-Service call inside the existing exclusive input boundary.
     *
     * <p>The adapter never acquires the input queue. It delegates the whole open-dialog entry,
     * item-selection, and Give-button sequence to
     * {@link GiveItemService#executeGiveFromOpenDialogDirectForExclusive(String, Integer)} without
     * splitting it.</p>
     *
     * @param call typed local-Service call; only GIVE_ITEM_FROM_OPEN_DIALOG is supported
     * @return completed mechanical result with {@code {"state":"<ENUM>"}}, or a fail-closed
     * result before Service invocation
     */
    public LocalServiceExecution execute(TurnLocalServiceCall call) {
        if (call == null || call.operation() == null) {
            return LocalServiceExecution.failed("INVALID_LOCAL_SERVICE_CALL", null);
        }
        return switch (call.operation()) {
            case GIVE_ITEM_FROM_OPEN_DIALOG -> executeGiveItem(call);
            default -> LocalServiceExecution.failed("UNSUPPORTED_LOCAL_OPERATION", null);
        };
    }

    private LocalServiceExecution executeGiveItem(TurnLocalServiceCall call) {
        TurnGiveItemOperationArguments arguments = call.giveItem();
        if (call.bag() != null || call.ui() != null || arguments == null || call.quest() != null
                || arguments.targetItemTemplate() == null || arguments.targetItemTemplate().isBlank()) {
            return LocalServiceExecution.failed("INVALID_GIVE_ITEM_ARGUMENTS", null);
        }

        GiveItemService.OpenDialogGiveState state = giveItemService.executeGiveFromOpenDialogDirectForExclusive(
                arguments.targetItemTemplate(), arguments.knownBagIndex());
        return LocalServiceExecution.completed("OK", json(new GiveItemOperationResult(state)), null);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("GiveItem local result cannot be serialized", e);
        }
    }

    private record GiveItemOperationResult(GiveItemService.OpenDialogGiveState state) {
    }
}
