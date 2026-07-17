package com.bot.dhxy.service.npc;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.window.model.WindowNativeBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Whole continuous local macro for the committed {@code 696a12b0} prepared-point click + verify,
 * extracted from {@code NpcClickService.java:176-238} ({@code executeMoveClickAndVerify}, executed on
 * the input worker in the {@code executeClickAndVerifyDirect} style) and its
 * {@code clickNpcByPlayerAnchorFormula:3011-3055} caller shape.
 *
 * <p>From one caller-prepared screen-absolute point it runs the baseline atomic move / {@code 150ms} /
 * left-click (hold {@code 150ms}) / wait sequence directly on the exclusive input worker (never a nested
 * input queue), calls the caller's existing dialog/battle verifier, and on a miss repeats the baseline
 * optional retry with a {@code 1000ms} wait, then returns a closed terminal. It never re-selects a
 * candidate, formula, target or fallback, adds no retry/delay/checkpoint of its own, and preserves the
 * original owner semantics of the borrowed binding, the input worker and the verifier. Interruption is
 * expressed only through the input-worker interrupt flag and the existing {@link TaskSleep} return
 * values; a produced click is never disguised as a plain not-verified.</p>
 */
@Slf4j
@Service
public final class NpcClickPreparedPointLocalMacroMechanics {

    private static final long NPC_LEFT_CLICK_HOLD_MS = 150L;
    private static final long MOVE_SETTLE_MS = 150L;
    private static final long RETRY_WAIT_MS = 1000L;
    private static final String INPUT_ACTION_WORKER_THREAD = "dhxy-input-action-worker";

    private final InputProvider inputProvider;

    public NpcClickPreparedPointLocalMacroMechanics(InputProvider inputProvider) {
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
    }

    /**
     * The caller's existing local dialog/battle verifier. Ownership stays with the caller; this macro
     * only invokes {@link #verify(String)} at the baseline verify points.
     */
    @FunctionalInterface
    public interface PreparedPointClickVerifier {
        boolean verify(String reason);
    }

    /** Closed terminal for one prepared-point click + verify. */
    public enum Status {
        VERIFIED,
        NOT_VERIFIED,
        BINDING_UNAVAILABLE,
        NON_INPUT_WORKER,
        INTERRUPTED,
        MECHANICS_FAILED
    }

    /**
     * Closed, serializable intent: the caller-prepared screen-absolute click point, the first verify
     * wait, the baseline optional retry count and a diagnostic description. Only primitive/String data.
     */
    public record PreparedPointClickIntent(
            int screenX,
            int screenY,
            long firstWaitMs,
            int maxRetries,
            String description) {

        public PreparedPointClickIntent {
            if (firstWaitMs < 0) {
                throw new IllegalArgumentException("firstWaitMs must be non-negative");
            }
            // 696a12b0 callers only ever pass 0 or YELLOW_TARGET_CLICK_RETRIES=1; this migration
            // authorizes no additional physical input, so any other retry count is rejected at construction.
            if (maxRetries != 0 && maxRetries != 1) {
                throw new IllegalArgumentException("maxRetries must be 0 or 1");
            }
        }
    }

    /**
     * Immutable closed result. {@code clickProduced} records whether a real left-click was issued before
     * this result was produced, preserved across every sleep/interrupt/mechanics exit, so a produced
     * click is never downgraded to a plain {@link Status#NOT_VERIFIED}.
     */
    public record PreparedPointClickResult(
            Status status,
            boolean clickProduced,
            int screenX,
            int screenY,
            String reason) {

        public PreparedPointClickResult {
            Objects.requireNonNull(status, "status");
            // A verify verdict is only reachable after the first click was issued.
            if ((status == Status.VERIFIED || status == Status.NOT_VERIFIED) && !clickProduced) {
                throw new IllegalArgumentException(status + " requires clickProduced=true");
            }
            // Pre-click terminals cannot have produced a click.
            if ((status == Status.BINDING_UNAVAILABLE || status == Status.NON_INPUT_WORKER) && clickProduced) {
                throw new IllegalArgumentException(status + " requires clickProduced=false");
            }
            // INTERRUPTED / MECHANICS_FAILED intentionally allow either, matching the real point of occurrence.
        }
    }

    /**
     * Run the baseline atomic move/click/verify (+ optional retry) for one prepared point on the input
     * worker. Order mirrors {@code executeMoveClickAndVerify}: move -> sleep 150 -> click (hold 150) ->
     * sleep firstWaitMs -> verify, then per retry move -> sleep 150 -> click -> sleep 1000 -> verify.
     */
    public PreparedPointClickResult clickPreparedPointAndVerify(
            WindowNativeBinding binding, PreparedPointClickIntent intent, PreparedPointClickVerifier verifier) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(verifier, "verifier");
        String description = safeDescription(intent.description());

        if (binding == null || !binding.hasNativeHandle()) {
            return result(Status.BINDING_UNAVAILABLE, false, intent, "binding-unavailable " + description);
        }
        if (!isInputWorkerThread()) {
            return result(Status.NON_INPUT_WORKER, false, intent, "non-input-worker " + description);
        }
        if (isInterrupted()) {
            return result(Status.INTERRUPTED, false, intent, "interrupted-before-move " + description);
        }

        boolean clickProduced = false;
        int x = intent.screenX();
        int y = intent.screenY();
        try {
            // Attempt 0: baseline move -> sleep 150 -> click (hold 150) -> sleep firstWaitMs.
            inputProvider.moveMouse(x, y);
            if (!TaskSleep.sleep(MOVE_SETTLE_MS)) {
                return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-move-settle " + description);
            }
            inputProvider.clickLeft(x, y, (int) NPC_LEFT_CLICK_HOLD_MS);
            clickProduced = true;
            if (!TaskSleep.sleep(intent.firstWaitMs())) {
                return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-first-wait " + description);
            }
            if (isInterrupted()) {
                return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-before-first-verify " + description);
            }
            if (verifier.verify(description + ":firstVerify")) {
                return result(Status.VERIFIED, clickProduced, intent, "verified " + description);
            }

            for (int i = 1; i <= intent.maxRetries(); i++) {
                if (isInterrupted()) {
                    return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-before-retry " + description);
                }
                log.warn("NPC prepared-point click retry {} point=({}, {}) {}", i, x, y, description);
                inputProvider.moveMouse(x, y);
                if (!TaskSleep.sleep(MOVE_SETTLE_MS)) {
                    return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-retry-move-settle " + description);
                }
                inputProvider.clickLeft(x, y, (int) NPC_LEFT_CLICK_HOLD_MS);
                if (!TaskSleep.sleep(RETRY_WAIT_MS)) {
                    return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-retry-wait " + description);
                }
                if (isInterrupted()) {
                    return result(Status.INTERRUPTED, clickProduced, intent, "interrupted-before-retry-verify " + description);
                }
                if (verifier.verify(description + ":retryVerify:" + i)) {
                    return result(Status.VERIFIED, clickProduced, intent, "verified-after-retry " + description);
                }
            }
            return result(Status.NOT_VERIFIED, clickProduced, intent, "not-verified " + description);
        } catch (RuntimeException e) {
            log.warn("NPC prepared-point click mechanics failed: {} point=({}, {}) reason={}",
                    description, x, y, e.getMessage(), e);
            return result(Status.MECHANICS_FAILED, clickProduced, intent, "mechanics-failed " + description);
        }
    }

    private static boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains(INPUT_ACTION_WORKER_THREAD);
    }

    private static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private static String safeDescription(String description) {
        return description == null || description.isBlank() ? "npcClick:preparedPoint" : description;
    }

    private static PreparedPointClickResult result(
            Status status, boolean clickProduced, PreparedPointClickIntent intent, String reason) {
        return new PreparedPointClickResult(status, clickProduced, intent.screenX(), intent.screenY(), reason);
    }
}
