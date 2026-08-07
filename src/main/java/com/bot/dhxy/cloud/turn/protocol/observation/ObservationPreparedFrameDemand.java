package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * One Cloud-issued request for a single exact-window frame outside pathing.
 *
 * @param demandId globally unique demand identity
 * @param purpose bounded business purpose
 * @param correlationId exact purpose-owned correlation, such as the NPC FIFO intent id
 * @param windowId exact logical window identity
 * @param hwnd exact native window handle
 * @param taskRunId exact observation run identity
 * @param generation positive demand generation
 * @param issuedAtMs Cloud issue time for diagnostics; demand lifetime is controlled by ACK/cancel/replacement
 */
public record ObservationPreparedFrameDemand(
        String demandId,
        String purpose,
        String correlationId,
        String windowId,
        String hwnd,
        String taskRunId,
        long generation,
        long issuedAtMs) {
}
