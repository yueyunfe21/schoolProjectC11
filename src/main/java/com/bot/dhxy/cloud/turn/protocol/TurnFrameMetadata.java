package com.bot.dhxy.cloud.turn.protocol;

public record TurnFrameMetadata(
        TurnFramePurpose purpose,
        String contentType,
        String sha256,
        int width,
        int height,
        TurnRegion region,
        Integer sourceStepIndex) {
}
