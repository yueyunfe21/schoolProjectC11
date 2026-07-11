package com.bot.dhxy.cloud.task;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RouteMemoryOutcomeIngestResult {

    public enum Status {
        SUBMITTED,
        DUPLICATE_SKIPPED,
        SKIPPED,
        FAILED
    }

    Status status;
    String idempotencyKey;
    String reason;

    static RouteMemoryOutcomeIngestResult submitted(String idempotencyKey) {
        return RouteMemoryOutcomeIngestResult.builder()
                .status(Status.SUBMITTED)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    static RouteMemoryOutcomeIngestResult duplicate(String idempotencyKey) {
        return RouteMemoryOutcomeIngestResult.builder()
                .status(Status.DUPLICATE_SKIPPED)
                .idempotencyKey(idempotencyKey)
                .reason("duplicate outcome already submitted")
                .build();
    }

    static RouteMemoryOutcomeIngestResult skipped(String idempotencyKey, String reason) {
        return RouteMemoryOutcomeIngestResult.builder()
                .status(Status.SKIPPED)
                .idempotencyKey(idempotencyKey)
                .reason(reason)
                .build();
    }

    static RouteMemoryOutcomeIngestResult failed(String idempotencyKey, String reason) {
        return RouteMemoryOutcomeIngestResult.builder()
                .status(Status.FAILED)
                .idempotencyKey(idempotencyKey)
                .reason(reason)
                .build();
    }
}
