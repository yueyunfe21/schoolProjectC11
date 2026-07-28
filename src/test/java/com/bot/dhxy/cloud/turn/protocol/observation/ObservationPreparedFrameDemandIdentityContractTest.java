package com.bot.dhxy.cloud.turn.protocol.observation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservationPreparedFrameDemandIdentityContractTest {

    @Test
    void responseDemandRejectsForeignWindowId() {
        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        response(demand("window-foreign", "12345", "run-1")),
                        request()));
    }

    @Test
    void responseDemandRejectsForeignHwnd() {
        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        response(demand("window-1", "99999", "run-1")),
                        request()));
    }

    @Test
    void responseDemandRejectsForeignTaskRunId() {
        assertThrows(IllegalArgumentException.class,
                () -> ObservationProtocolValidator.requireValid(
                        response(demand("window-1", "12345", "run-foreign")),
                        request()));
    }

    private static ObservationRequest request() {
        return new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                "tenant-1", "device-1", "window-1", "12345", "WUHuan_V2", "run-1",
                1L, System.currentTimeMillis(), 0L,
                null, null, null, "test", null,
                List.of(), List.of(), List.of(), List.of());
    }

    private static ObservationResponse response(ObservationPreparedFrameDemand demand) {
        return new ObservationResponse(
                ObservationProtocolValidator.CONTRACT_VERSION,
                1L, 0L, List.of(), List.of(), List.of(), List.of(demand));
    }

    private static ObservationPreparedFrameDemand demand(
            String windowId, String hwnd, String taskRunId) {
        long now = System.currentTimeMillis();
        return new ObservationPreparedFrameDemand(
                "demand-1", "WUHUAN_NPC_CLICK", "correlation-1",
                windowId, hwnd, taskRunId, 1L, now, now + 15_000L);
    }
}
