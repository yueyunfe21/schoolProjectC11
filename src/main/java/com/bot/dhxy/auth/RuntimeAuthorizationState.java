package com.bot.dhxy.auth;

import com.bot.dhxy.model.auth.RuntimeAuthorizationCode;
import com.bot.dhxy.model.auth.RuntimeAuthorizationRevoked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.function.LongConsumer;

/**
 * One process's runtime permission and input generation. This is not a positive server-policy cache:
 * Cloud still checks current permission. Revocation removes local authority before notifying owners.
 * The same monitor linearizes revoke with queue admission and each native report, never with a full
 * input transaction or a network wait. Event listeners run outside it to avoid input/context lock cycles.
 */
@Component
@Slf4j
public final class RuntimeAuthorizationState {
    private final boolean required;
    @Value("${dhxy.update-only:false}")
    private boolean updateOnly;
    private final ApplicationEventPublisher events;
    // Child observation/transport/maintenance threads must retain their creating loop's authority.
    // Queue workers bind each request explicitly and never inherit authority from a later login.
    private final InheritableThreadLocal<Long> inputGeneration = new InheritableThreadLocal<>();
    private String sessionToken = "";
    private long generation;
    private boolean dispatchingRevocation;
    private boolean cleanupFailed;

    public RuntimeAuthorizationState(@Value("${cloud.turn.device-id:dhxy-client}") String clientId,
            @Value("${dhxy.install-root:}") String installRoot, ApplicationEventPublisher events) {
        required = !installRoot.isBlank() || clientId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        this.events = events;
    }

    public boolean requiresSession() { return required; }
    public boolean isUpdateOnly() { return updateOnly; }
    public synchronized boolean isAllowed() {
        Long captured = inputGeneration.get();
        return !updateOnly && (!required || (!sessionToken.isEmpty() && !dispatchingRevocation && !cleanupFailed
                && (captured == null || captured == generation)));
    }
    /** Transport-only secret, never used as a UI message or logging field. */
    public synchronized String sessionToken() { return isAllowed() ? sessionToken : ""; }

    /** Capture the creator's authority, preserving an inherited old generation rather than refreshing it. */
    public synchronized long executionGeneration() {
        Long captured = inputGeneration.get();
        return captured == null ? generation : captured;
    }

    /** Fence a response/request against the login it actually used, not whichever login exists now. */
    public synchronized boolean isCurrentSession(String expectedToken) {
        return !updateOnly && (!required || (isAllowed() && expectedToken != null && expectedToken.equals(sessionToken)));
    }

    /** Retire an existing login before explicit verification. No task or input is started. */
    public long beginLogin() {
        RuntimeAuthorizationRevoked event;
        synchronized (this) {
            if (cleanupFailed) throw new IllegalStateException("撤权清理失败，请重启客户端后重新授权。");
            if (dispatchingRevocation) throw new IllegalStateException("运行撤权清理正在进行，请稍后重新授权。");
            boolean hadSession = !sessionToken.isEmpty();
            sessionToken = "";
            generation++;
            event = required && hadSession ? RuntimeAuthorizationRevoked.builder()
                    .generation(generation).code(RuntimeAuthorizationCode.CLIENT_SESSION_INVALID).build() : null;
            dispatchingRevocation = event != null;
        }
        if (event != null) publishRevocation(event);
        synchronized (this) { return generation; }
    }

    /** Old or concurrently revoked OPEN replies cannot reactivate permission. */
    public synchronized boolean activate(long expectedGeneration, String token) {
        if (updateOnly || generation != expectedGeneration || dispatchingRevocation || cleanupFailed ||
                token == null || !token.matches("[A-Za-z0-9_-]{43,128}")) return false;
        sessionToken = token;
        return true;
    }

    /**
     * @param expectedToken exact token used by the rejected request; null/old/empty tokens cannot revoke a new login.
     * @param code closed, non-secret authorization error. UNKNOWN is not authority to stop.
     */
    public void reject(String expectedToken, RuntimeAuthorizationCode code) {
        RuntimeAuthorizationRevoked event;
        synchronized (this) {
            if (!required || expectedToken == null || expectedToken.isEmpty() || !expectedToken.equals(sessionToken) ||
                    code == null || code == RuntimeAuthorizationCode.UNKNOWN) return;
            sessionToken = "";
            generation++;
            dispatchingRevocation = true;
            event = RuntimeAuthorizationRevoked.builder().generation(generation).code(code).build();
        }
        // UI stop may hold the lifecycle monitor while joining this transport's worker. Dispatch outside
        // that worker so revoke cannot form a join/monitor cycle. The input permit is already empty;
        // activate remains blocked until all cleanup listeners have completed (or permanently on failure).
        Thread.startVirtualThread(() -> {
            try { publishRevocation(event); }
            catch (RuntimeException failure) { log.error("Runtime authorization cleanup failed: code={}", event.getCode()); }
        });
    }

    /** Queue publication and generation capture form one operation with respect to revoke. */
    public synchronized boolean admitInput(LongConsumer publication) {
        if (!isAllowed()) return false;
        publication.accept(generation);
        return true;
    }

    /** Bind an immutable loop/request generation for its entire execution and any newly created child threads. */
    public Scope bindInput(long capturedGeneration) {
        Long previous = inputGeneration.get();
        inputGeneration.set(capturedGeneration);
        return () -> { if (previous == null) inputGeneration.remove(); else inputGeneration.set(previous); };
    }

    /** Called before focus and before each real non-neutral native report; no network or sleep under this lock. */
    public synchronized void physicalReport(Runnable report) {
        Long captured = inputGeneration.get();
        if (!isAllowed() || (required && captured != null && captured != generation)) {
            throw new IllegalStateException("运行许可已失效；禁止继续输入，请重新授权或更新客户端。");
        }
        report.run();
    }

    private void publishRevocation(RuntimeAuthorizationRevoked event) {
        try { events.publishEvent(event); }
        catch (RuntimeException listenerFailure) {
            synchronized (this) { cleanupFailed = true; }
            throw listenerFailure;
        } finally {
            synchronized (this) { dispatchingRevocation = false; }
        }
    }

    /** Restores the caller's previous thread context; closing cannot fail or grant newer authority. */
    public interface Scope extends AutoCloseable {
        @Override void close();
    }
}
