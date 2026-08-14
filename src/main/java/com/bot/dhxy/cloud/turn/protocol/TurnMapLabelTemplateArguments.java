package com.bot.dhxy.cloud.turn.protocol;

/** Typed payload for mirroring one trusted map-label PNG into the Client template library. */
public record TurnMapLabelTemplateArguments(
        String mapName,
        String pngBase64) {
}
