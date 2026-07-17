package com.bot.dhxy.cloud.remote;

import java.util.Optional;

/** Fail-closed lifecycle orchestration error; it never grants task or input ownership. */
public final class RemoteTaskRunLifecycleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;
    private final boolean retryable;
    private final RemoteTaskRunRegistration recoveryBinding;
    private final boolean cleanupPending;

    public RemoteTaskRunLifecycleException(
            Reason reason,
            boolean retryable,
            String message,
            Throwable cause) {
        this(reason, retryable, null, false, message, cause);
    }

    public RemoteTaskRunLifecycleException(
            Reason reason,
            boolean retryable,
            RemoteTaskRunRegistration recoveryBinding,
            boolean cleanupPending,
            String message,
            Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.retryable = retryable;
        this.recoveryBinding = recoveryBinding;
        this.cleanupPending = cleanupPending;
    }

    public Reason getReason() {
        return reason;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Optional<RemoteTaskRunRegistration> getRecoveryBinding() {
        return Optional.ofNullable(recoveryBinding);
    }

    public boolean isCleanupPending() {
        return cleanupPending;
    }

    public enum Reason {
        INVALID_REQUEST,
        BINDING_MISMATCH,
        POLLER_NOT_READY,
        REMOTE_STATE_UNCERTAIN,
        INVALID_REMOTE_STATE,
        LOCAL_REGISTRATION_MISSING,
        CAPACITY_EXCEEDED
    }
}
