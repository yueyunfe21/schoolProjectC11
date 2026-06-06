package com.bot.dhxy.model.navigation;

import lombok.Builder;
import lombok.Value;

/**
 * Result returned by navigation APIs.
 */
@Value
@Builder
public class NavigationResult {
    /**
     * Coarse status for task-layer branching.
     */
    NavigationResultStatus status;

    /**
     * Short diagnostic message safe for logs/UI.
     */
    String message;

    public static NavigationResult success(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.SUCCESS)
                .message(message)
                .build();
    }

    public static NavigationResult arrived(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.ARRIVED)
                .message(message)
                .build();
    }

    public static NavigationResult pathingStarted(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.PATHING_STARTED)
                .message(message)
                .build();
    }

    public static NavigationResult failed(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.FAILED)
                .message(message)
                .build();
    }

    public static NavigationResult stopped(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.STOPPED)
                .message(message)
                .build();
    }

    public static NavigationResult interrupted(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.INTERRUPTED)
                .message(message)
                .build();
    }

    public static NavigationResult mapNotReached(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.MAP_NOT_REACHED)
                .message(message)
                .build();
    }

    public static NavigationResult pointNotReached(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.POINT_NOT_REACHED)
                .message(message)
                .build();
    }

    public static NavigationResult dialogOpened(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.DIALOG_OPENED)
                .message(message)
                .build();
    }

    public static NavigationResult dialogPreparing(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.DIALOG_PREPARING)
                .message(message)
                .build();
    }

    public boolean success() {
        return status == NavigationResultStatus.SUCCESS
                || status == NavigationResultStatus.ARRIVED
                || status == NavigationResultStatus.DIALOG_OPENED;
    }
}
