package com.bot.dhxy.cloud.turn.protocol;

public record TurnWindowMetadata(
        String deviceId,
        String windowId,
        String windowTitle,
        String nativeHandle,
        long processId,
        TurnWindowRect windowRect,
        boolean pauseRequested,
        boolean stopRequested,
        TurnPathingSnapshot pathingSnapshot) {

    public TurnWindowMetadata(
            String deviceId,
            String windowId,
            String windowTitle,
            String nativeHandle,
            long processId,
            TurnWindowRect windowRect,
            boolean pauseRequested,
            boolean stopRequested) {
        this(deviceId, windowId, windowTitle, nativeHandle, processId, windowRect, pauseRequested, stopRequested, null);
    }

    public TurnWindowMetadata(
            String deviceId,
            String windowId,
            String windowTitle,
            String nativeHandle,
            long processId,
            TurnWindowRect windowRect,
            boolean stopRequested) {
        this(deviceId, windowId, windowTitle, nativeHandle, processId, windowRect, false, stopRequested, null);
    }
}
