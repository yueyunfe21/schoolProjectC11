package com.bot.dhxy.cloud.remote;

/** Registry-owned bounded slot for one resumed revision. */
final class PendingExecutorReadiness {

    enum State { AWAITING_DRAIN, READY, SENDING }

    final long slotGeneration;
    final long fromRevision;
    final long toRevision;
    State state = State.AWAITING_DRAIN;
    RetainedSend retainedSend;
    long nextAttemptNotBeforeNanos;

    PendingExecutorReadiness(long slotGeneration, long fromRevision, long toRevision) {
        this.slotGeneration = slotGeneration;
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    record RetainedSend(
            RemoteTaskRunActionRequest request,
            String requestId,
            String requestDigest,
            String factDigest) {
    }

    record DrainCandidate(
            long entryGeneration,
            long slotGeneration,
            String taskRunId,
            long fromRevision,
            long toRevision,
            RemoteTaskRunRegistration registration,
            long pauseTokenMechanicalGeneration) {
    }

    record PendingSendHandle(
            long entryGeneration,
            long slotGeneration,
            String requestId,
            long toRevision,
            RetainedSend retainedSend) {
    }
}
