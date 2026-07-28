package com.bot.dhxy.cloud.turn.protocol.observation;

/**
 * One exact-window PNG captured for a matching prepared-frame demand.
 *
 * @param demandId exact Cloud demand identity
 * @param purpose demand purpose copied without reinterpretation
 * @param generation exact demand generation
 * @param left window-relative left coordinate, always zero
 * @param top window-relative top coordinate, always zero
 * @param width exact frame width
 * @param height exact frame height
 * @param encoding image encoding, always PNG
 * @param capturedAtMs Client capture time
 * @param pngBytes encoded exact-window frame
 */
public record ObservationPreparedFrame(
        String demandId,
        String purpose,
        long generation,
        int left,
        int top,
        int width,
        int height,
        String encoding,
        long capturedAtMs,
        byte[] pngBytes) {
}
