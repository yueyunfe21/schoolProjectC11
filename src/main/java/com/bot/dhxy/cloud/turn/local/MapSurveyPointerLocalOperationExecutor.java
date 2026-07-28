package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnMapSurveyPointerSample;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowHandleParser;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.util.Objects;

/** Exact-window mechanical pointer sampling for the typed MapSurvey turn operation. */
@Component
public final class MapSurveyPointerLocalOperationExecutor {

    private final WindowTaskContextHolder contextHolder;
    private final ObjectMapper objectMapper;

    public MapSurveyPointerLocalOperationExecutor(WindowTaskContextHolder contextHolder,
                                                  ObjectMapper objectMapper) {
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Sample the current screen-absolute pointer while preserving the action's exact HWND identity.
     *
     * @param deviceId nonblank device id from the action envelope.
     * @param windowId nonblank logical window id from the action envelope.
     * @return completed typed JSON sample, or a fail-closed mechanical result.
     */
    public LocalServiceExecution execute(String deviceId, String windowId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        if (context == null || !Objects.equals(context.getWindowId(), windowId)) {
            return LocalServiceExecution.failed("MAP_SURVEY_WRONG_WINDOW", null);
        }
        WindowNativeBinding binding = context.getNativeBinding();
        Long hwnd = binding == null ? null : WindowHandleParser.parseHandle(binding.getNativeHandle());
        if (hwnd == null || hwnd == 0L || deviceId == null || deviceId.isBlank()) {
            return LocalServiceExecution.failed("MAP_SURVEY_WINDOW_UNAVAILABLE", null);
        }
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point pointer = pointerInfo == null ? null : pointerInfo.getLocation();
        if (pointer == null) {
            return LocalServiceExecution.failed("MAP_SURVEY_POINTER_UNAVAILABLE", null);
        }
        TurnMapSurveyPointerSample sample = new TurnMapSurveyPointerSample(
                deviceId, windowId, hwnd, pointer.x, pointer.y, System.currentTimeMillis());
        try {
            return LocalServiceExecution.completed(
                    "MAP_SURVEY_POINTER_SAMPLED", objectMapper.writeValueAsString(sample), null);
        } catch (JsonProcessingException serializationFailure) {
            return LocalServiceExecution.failed("MAP_SURVEY_POINTER_SERIALIZATION_FAILED", null);
        }
    }
}
