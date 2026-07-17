package com.bot.dhxy.cloud.remote;


import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * W-TEAMRETURN-REG-IMP1 (CR TeamReturnService lift-and-shift): self-contained bounded owner of the
 * leader-signal precheck frames, implementing the authoritative transitions of Parent Design Review #8
 * and the ownership/exactness repairs of Parent Source Review #1.
 *
 * <p>This is an <em>unmounted</em> leaf: it neither captures, analyses, nor starts any thread. The
 * caller borrows an admission permit before it captures ({@link #reserve}), attaches the immutable
 * frame ({@link #attachFrame}), and submits an external analysis whose worker first takes ownership
 * ({@link #pickup}: {@code RESERVED -> IN_FLIGHT}) before dereferencing the frame and finally settles
 * exactly once ({@link #completeSuccess}/{@link #completeFailed}). Terminal cleanup is driven by an
 * owner lifecycle caller ({@link #releaseRun}); the business result is consumed by the exact original
 * handle ({@link #consume}).</p>
 *
 * <p>Ownership &amp; exactness (Source Review #1):</p>
 * <ul>
 *   <li>Only a {@link ReserveStatus#FRESH} {@link Reservation} carries a real slot. {@code REUSED_ACTIVE}
 *       / {@code TEARDOWN_BUSY} / {@code CAPACITY_REJECTED} carry {@code slot == null}, so a same-key
 *       second caller can never mutate, cancel, pickup, or settle the first owner's slot.</li>
 *   <li>Every mutator requires the original FRESH handle and re-checks entry-object identity
 *       ({@code slots.get(key) == reservation.slot}); {@link #attachFrame} additionally fails closed on a
 *       second attach ({@code frame != null}).</li>
 *   <li>{@link #consume} is by exact handle, not by key — a late handle over a rebuilt slot is STALE.</li>
 *   <li>The analysis worker takes ownership via {@link #pickup} before any frame dereference; a lost
 *       pickup reads/flushes/releases nothing. The future is only a cancellation aid — bound after a
 *       teardown it is cancelled immediately, and {@code cancel} is always performed outside the lock.</li>
 * </ul>
 *
 * <p>Invariants: the permit is an admission fence taken before the {@link BufferedImage} exists; a
 * success/failure settle keeps the entry+typed result until the exact {@link #consume} or
 * {@link #releaseRun}; an IN_FLIGHT retire is settled only by the worker {@code finally}; the frame is
 * flushed and the permit released exactly once per slot; entry-object identity (no generation) is the
 * sole ABA fence.</p>
 *
 * @param <R> caller-supplied non-null typed analysis result stored on a DONE slot.
 */
final class LeaderPrecheckFrameRegistry<R> {

    private final int globalFrameLimit;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<RunWindowKey, Slot<R>> slots = new HashMap<>();
    private int usedPermits;

    /**
     * @param globalFrameLimit positive hard cap on concurrently-held precheck frames across all runs.
     */
    LeaderPrecheckFrameRegistry(int globalFrameLimit) {
        if (globalFrameLimit <= 0) {
            throw new IllegalArgumentException("globalFrameLimit must be positive");
        }
        this.globalFrameLimit = globalFrameLimit;
    }

    /** Outcome of an admission attempt. Only {@link #FRESH} carries a slot. */
    enum ReserveStatus {
        /** New RESERVED slot created and one permit borrowed; the caller must capture into it. */
        FRESH,
        /** An active slot (RESERVED / IN_FLIGHT / unconsumed DONE / unconsumed FAILED) already exists. */
        REUSED_ACTIVE,
        /** A slot for this key is tearing down (RETIRING); the caller must fall back live. */
        TEARDOWN_BUSY,
        /** The global frame permit pool is full; nothing was reserved. */
        CAPACITY_REJECTED
    }

    /** Outcome of a consume attempt. */
    enum ConsumeStatus {
        /** A DONE result is available in {@link ConsumeResult#value}. */
        READY,
        /** The slot exists but its analysis has not settled yet. */
        NOT_READY,
        /** The slot settled as FAILED (capture/analysis/submit failure); the caller must fall back live. */
        FAILED,
        /** No matching active slot for this exact handle; it is stale. */
        STALE
    }

    /**
     * Opaque handle returned by {@link #reserve}. A {@link ReserveStatus#FRESH} handle carries the
     * entry-object identity for every subsequent mutation/consume; all other statuses carry no slot.
     */
    static final class Reservation<R> {
        private final ReserveStatus status;
        private final RunWindowKey key;
        private final Slot<R> slot;

        private Reservation(ReserveStatus status, RunWindowKey key, Slot<R> slot) {
            this.status = status;
            this.key = key;
            this.slot = slot;
        }

        ReserveStatus status() {
            return status;
        }
    }

    /** Typed consume result. */
    record ConsumeResult<R>(ConsumeStatus status, R value) {
    }

    /**
     * Admission: take the map-key and one global permit before the caller captures. Idempotent for an
     * already-active key (returns {@link ReserveStatus#REUSED_ACTIVE} with no slot and no extra permit).
     *
     * @throws IllegalArgumentException if session is null or taskRunId/windowId is null/blank.
     */
    Reservation<R> reserve(RemoteClientSessionRef session, String taskRunId, String windowId) {
        RunWindowKey key = RunWindowKey.of(session, taskRunId, windowId);
        lock.lock();
        try {
            Slot<R> existing = slots.get(key);
            if (existing != null) {
                return switch (existing.state) {
                    case RESERVED, IN_FLIGHT, DONE, FAILED ->
                            new Reservation<>(ReserveStatus.REUSED_ACTIVE, key, null);
                    // RETIRING is tearing down; a stray mapped REMOVED_SENTINEL (should not occur) is also busy.
                    case RETIRING, REMOVED_SENTINEL ->
                            new Reservation<>(ReserveStatus.TEARDOWN_BUSY, key, null);
                };
            }
            if (usedPermits >= globalFrameLimit) {
                return new Reservation<>(ReserveStatus.CAPACITY_REJECTED, key, null);
            }
            usedPermits++;
            Slot<R> slot = new Slot<>();
            slots.put(key, slot);
            return new Reservation<>(ReserveStatus.FRESH, key, slot);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Attach the single immutable frame captured after a {@link ReserveStatus#FRESH} reserve. Fails
     * closed on a repeat attach (never overwrites a live frame). No counter change — the permit was
     * borrowed at reserve.
     *
     * <p><b>Ownership:</b> on {@code true} the registry takes ownership of {@code frame} and will flush
     * it exactly once at settle/cleanup. On {@code false} the registry never took the frame, so the
     * caller retains ownership and must flush it itself.</p>
     *
     * @return true when the frame was attached to the still-owned RESERVED slot.
     */
    boolean attachFrame(Reservation<R> reservation, BufferedImage frame) {
        Objects.requireNonNull(frame, "frame");
        lock.lock();
        try {
            Slot<R> slot = ownedSlot(reservation);
            if (slot == null || slot.state != State.RESERVED || slot.frame != null) {
                return false;
            }
            slot.frame = frame;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Analysis-worker ownership transfer: atomically validate the exact FRESH handle, {@code RESERVED}
     * state and an already-attached frame, move to {@code IN_FLIGHT}, and hand the worker the frame to
     * read. A lost pickup — foreign/stale handle, not RESERVED (cancelled/retired/already picked up), or
     * no frame yet attached — returns {@code null}, after which the worker must not read a frame, flush,
     * or settle. The registry stays the frame owner: the worker only reads it and must never flush it;
     * the sole flush/permit-release is the worker's {@code finally} via {@link #completeSuccess}/
     * {@link #completeFailed}. Returning the frame only when {@code frame != null} structurally forbids a
     * pickup before {@link #attachFrame}.
     *
     * @return the registry-owned {@link BufferedImage} to analyse, or {@code null} to bail out untouched.
     */
    BufferedImage pickup(Reservation<R> reservation) {
        lock.lock();
        try {
            Slot<R> slot = ownedSlot(reservation);
            if (slot == null || slot.state != State.RESERVED || slot.frame == null) {
                return null;
            }
            slot.state = State.IN_FLIGHT;
            return slot.frame;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Best-effort binding of the external analysis future so {@link #releaseRun} can cancel it. The
     * binding is single-owner: the first future for a live slot is stored; re-binding the same future is
     * idempotent; a <em>different</em> second future fails closed (the original owner future is never
     * overwritten) and the new future is cancelled. If the slot is already tearing down or gone the new
     * future is cancelled. The actual {@code cancel} is always performed outside the lock.
     *
     * @return true only when this exact future is (or is already) the bound owner future.
     */
    boolean bindFuture(Reservation<R> reservation, CompletableFuture<?> future) {
        Objects.requireNonNull(future, "future");
        boolean bound = false;
        boolean cancelNew = false;
        lock.lock();
        try {
            Slot<R> slot = ownedSlot(reservation);
            if (slot == null || (slot.state != State.RESERVED && slot.state != State.IN_FLIGHT)) {
                cancelNew = true;                 // tearing down / gone
            } else if (slot.future == null) {
                slot.future = future;             // first bind
                bound = true;
            } else if (slot.future == future) {
                bound = true;                     // idempotent re-bind of the same future
            } else {
                cancelNew = true;                 // different future: fail closed, do not overwrite owner
            }
        } finally {
            lock.unlock();
        }
        if (cancelNew) {
            future.cancel(true);
        }
        return bound;
    }

    /**
     * Worker {@code finally}: a successful analysis. IN_FLIGHT stores the non-null result as DONE (kept
     * until consume); a retired slot is removed. Either way the frame is flushed and the permit released
     * exactly once.
     *
     * @throws NullPointerException if result is null (a failed analysis must use {@link #completeFailed}).
     */
    void completeSuccess(Reservation<R> reservation, R result) {
        Objects.requireNonNull(result, "result");
        settleFromWorker(reservation, result, State.DONE);
    }

    /**
     * Worker {@code finally}: a failed/UNKNOWN analysis. Stays as a FAILED typed result (kept until
     * consume) unless retired mid-flight. Frame flushed and permit released exactly once. UNKNOWN is not
     * turned into a success here.
     */
    void completeFailed(Reservation<R> reservation) {
        settleFromWorker(reservation, null, State.FAILED);
    }

    private void settleFromWorker(Reservation<R> reservation, R result, State settledState) {
        lock.lock();
        try {
            Slot<R> slot = ownedSlot(reservation);
            if (slot == null) {
                return;
            }
            if (slot.state == State.IN_FLIGHT) {
                flushFrame(slot);
                releasePermit(slot);
                slot.result = result;
                slot.state = settledState;
                // entry + typed result retained until consume/releaseRun
            } else if (slot.state == State.RETIRING) {
                flushFrame(slot);
                releasePermit(slot);
                slot.state = State.REMOVED_SENTINEL;
                slots.remove(reservation.key, slot); // identity-conditional
            }
            // any other state: nothing to do
        } finally {
            lock.unlock();
        }
    }

    /** Caller: the single capture failed (no frame was attached). Release the borrowed permit. */
    void captureFailed(Reservation<R> reservation) {
        settleReservedByCaller(reservation);
    }

    /** Caller: the executor rejected the submit (frame may be attached). Flush the frame and release the permit. */
    void submitRejected(Reservation<R> reservation) {
        settleReservedByCaller(reservation);
    }

    /** Caller: cancel a not-yet-in-flight reservation. Flush any frame and release the permit. */
    void cancel(Reservation<R> reservation) {
        settleReservedByCaller(reservation);
    }

    private void settleReservedByCaller(Reservation<R> reservation) {
        CompletableFuture<?> toCancel = null;
        lock.lock();
        try {
            Slot<R> slot = ownedSlot(reservation);
            if (slot == null || slot.state != State.RESERVED) {
                return;
            }
            flushFrame(slot);
            releasePermit(slot);
            toCancel = slot.future;             // detach any bound future; cancel outside lock
            slot.future = null;
            slot.state = State.REMOVED_SENTINEL;
            slots.remove(reservation.key, slot); // invalidate first so a concurrent pickup can only fail
        } finally {
            lock.unlock();
        }
        if (toCancel != null) {
            toCancel.cancel(true);
        }
    }

    /**
     * Owner lifecycle terminal cleanup for one exact run/window. RESERVED is flushed+released by this
     * caller; IN_FLIGHT is only marked RETIRING (its future detached and cancelled outside the lock, the
     * worker finally being the sole flush/release); DONE/FAILED just drop the retained entry.
     */
    void releaseRun(RemoteClientSessionRef session, String taskRunId, String windowId) {
        RunWindowKey key = RunWindowKey.of(session, taskRunId, windowId);
        CompletableFuture<?> toCancel = null;
        lock.lock();
        try {
            Slot<R> slot = slots.get(key);
            if (slot == null) {
                return;
            }
            switch (slot.state) {
                case RESERVED -> {
                    flushFrame(slot);
                    releasePermit(slot);
                    toCancel = slot.future; // detach any bound future; cancel outside lock
                    slot.future = null;
                    slot.state = State.REMOVED_SENTINEL;
                    slots.remove(key, slot); // invalidate first so a concurrent pickup can only fail
                }
                case IN_FLIGHT -> {
                    slot.state = State.RETIRING;
                    toCancel = slot.future; // detach; cancel outside lock
                    slot.future = null;
                    // worker finally will flush + release + remove
                }
                case DONE, FAILED -> {
                    // frame already flushed, permit already released at settle; drop the retained entry
                    slot.state = State.REMOVED_SENTINEL;
                    slots.remove(key, slot);
                }
                case RETIRING, REMOVED_SENTINEL -> {
                    // already tearing down; leave to the worker finally
                }
            }
        } finally {
            lock.unlock();
        }
        if (toCancel != null) {
            toCancel.cancel(true);
        }
    }

    /**
     * Consume the exact reservation's slot (by entry-object identity, not by key). DONE returns its
     * non-null result and drops the entry; a settled FAILED returns FAILED and drops it; an unsettled
     * slot is NOT_READY; a rebuilt/removed slot is STALE.
     */
    ConsumeResult<R> consume(Reservation<R> reservation) {
        lock.lock();
        try {
            Slot<R> slot = ownedSlot(reservation);
            if (slot == null) {
                return new ConsumeResult<>(ConsumeStatus.STALE, null);
            }
            return switch (slot.state) {
                case DONE -> {
                    R value = slot.result;
                    assert slot.frame == null : "DONE slot must have no frame";
                    slot.state = State.REMOVED_SENTINEL;
                    slots.remove(reservation.key, slot); // release map-key only; permit released at settle
                    yield new ConsumeResult<>(ConsumeStatus.READY, value);
                }
                case FAILED -> {
                    assert slot.frame == null : "FAILED slot must have no frame";
                    slot.state = State.REMOVED_SENTINEL;
                    slots.remove(reservation.key, slot);
                    yield new ConsumeResult<>(ConsumeStatus.FAILED, null);
                }
                case RESERVED, IN_FLIGHT, RETIRING -> new ConsumeResult<>(ConsumeStatus.NOT_READY, null);
                case REMOVED_SENTINEL -> new ConsumeResult<>(ConsumeStatus.STALE, null);
            };
        } finally {
            lock.unlock();
        }
    }

    /** Test/diagnostic view of currently-held permits. Package-private, no external mutation. */
    int usedPermitsSnapshot() {
        lock.lock();
        try {
            return usedPermits;
        } finally {
            lock.unlock();
        }
    }

    // --- private helpers (all callers already hold the lock unless noted) ---

    /**
     * Entry-object identity fence: returns the reservation's slot only when it is the exact FRESH handle
     * that still owns the current map entry for its key. Any non-FRESH handle, or a rebuilt/removed slot,
     * yields null so no foreign or stale caller can mutate/consume it.
     */
    private Slot<R> ownedSlot(Reservation<R> reservation) {
        if (reservation == null
                || reservation.status != ReserveStatus.FRESH
                || reservation.slot == null) {
            return null;
        }
        return slots.get(reservation.key) == reservation.slot ? reservation.slot : null;
    }

    private void flushFrame(Slot<R> slot) {
        if (slot.frame != null) {
            slot.frame.flush();
            slot.frame = null;
        }
    }

    private void releasePermit(Slot<R> slot) {
        if (slot.permitReleased) {
            throw new IllegalStateException("precheck permit already released for this slot");
        }
        slot.permitReleased = true;
        if (usedPermits <= 0) {
            throw new IllegalStateException("precheck permit counter underflow");
        }
        usedPermits--;
    }

    private enum State {
        RESERVED,
        IN_FLIGHT,
        RETIRING,
        DONE,
        FAILED,
        REMOVED_SENTINEL
    }

    private static final class Slot<R> {
        private State state = State.RESERVED;
        private BufferedImage frame;
        private CompletableFuture<?> future;
        private R result;
        private boolean permitReleased;
    }

    private record RunWindowKey(RemoteClientSessionRef session, String taskRunId, String windowId) {

        private static RunWindowKey of(RemoteClientSessionRef session, String taskRunId, String windowId) {
            Objects.requireNonNull(session, "session");
            if (taskRunId == null || taskRunId.isBlank()) {
                throw new IllegalArgumentException("taskRunId must be non-blank");
            }
            if (windowId == null || windowId.isBlank()) {
                throw new IllegalArgumentException("windowId must be non-blank");
            }
            return new RunWindowKey(session, taskRunId, windowId);
        }
    }
}
