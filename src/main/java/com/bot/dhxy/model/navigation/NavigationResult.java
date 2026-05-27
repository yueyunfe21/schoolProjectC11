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

    public static NavigationResult failed(String message) {
        return NavigationResult.builder()
                .status(NavigationResultStatus.FAILED)
                .message(message)
                .build();
    }

    public boolean success() {
        return status == NavigationResultStatus.SUCCESS || status == NavigationResultStatus.DIALOG_OPENED;
    }
}
