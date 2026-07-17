# CR271 TURN-34BP1 Repair #2 Independent Delivery Review R2

## Review identity

- Role: independent delivery reviewer R2 for TURN-34BP1 Repair #2.
- Boundary: this is not implementation work and not the parent/final CR271 adjudication.
- Independence: I did not open, read, quote, or adopt the R1 report or its conclusion. This verdict was formed directly from the frozen sources and authoritative contracts.
- Review mode: source-only and read-only. Per assignment, no Maven, JUnit, compile, runtime, application/server, Task/UI, capture, or input operation was run, and no Git mutation was performed.

## Verdict

**APPROVED P0/P1/P2=0/0/0**

Repair #2 is bounded to its approved evidence/documentation scope. The production delta is class-level JavaDoc only; the exact-window monotonic latch, public API, stop/pause semantics, and zero-automatic-retry behavior are unchanged. The repaired contract source now proves the exact-positive and A0 -> B -> A' paths, including cumulative read/slot accounting, without command/action/UUID side effects.

## Authority and frozen inputs

The following material was read in full for the assigned scope before forming the verdict:

- `AGENTS.md`.
- `docs/DHXY_CONTEXT.md`.
- The top CR271 section of `docs/ACTIVE_WORK.md`.
- Sections 14 through 19 of `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`.
- The full HTTPS turn protocol in `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`.
- `docs/业务逻辑.md`.
- The complete TURN-34BP1 card history, including Delivery Review #1, Repair #1, Parent Review #2, Repair #2, and Parent Review #3, in `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34BP1.md`.
- Full production source `TaskExecutionContext.java`, SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e`.
- Full contract source `TaskExecutionContextTurnContractTest.java`, SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785`.
- Supporting value/port sources `TurnWindowMetadata.java`, `TurnWindowRect.java`, and `TurnGameClient.java`, used only to verify record equality and metadata-read side effects.

Both frozen file hashes match the TURN-34BP1 Repair #2 card exactly.

## Delta identity

### Production

A read-only in-memory reverse replacement of only the Repair #2 class-level JavaDoc restores SHA-256 `f278460ba9dc664974a98ea5ef19532e60514b29015a2e9b25b8f49bf0eba895`, the frozen Repair #1 production SHA. No file was written during this reconstruction.

This byte-exact reconstruction proves that Repair #2 changed only the class JavaDoc. No field, constructor, method declaration, method body, constant, import, or public API changed. The revised JavaDoc accurately describes the already-present context-local monotonic latch as powerless and does not claim owner/session/ledger/transport/runtime/lifecycle authority.

### Contract source

A read-only in-memory reversal of the four Repair #2 evidence blocks restores SHA-256 `7caf01272346b2f647e67c825b11b1606ba38b81ee1e29ff65b56c3bc6b9dbbf`, the frozen Repair #1 contract-source SHA. The reversed blocks are limited to:

1. The `assertNotEquals` static import.
2. Exact-positive UUID/action/read/slot assertions.
3. The same-context A0 -> B -> A' fixture and assertions.
4. Cumulative read/slot accounting in `assertTransition(...)`.

This proves that Repair #2 introduced no unrelated contract-source drift.

## Production review

### Exact-window monotonic latch

- `nativeGenerationRetired` remains guarded by the per-context `nativeGenerationLock` together with the single latest-slot read and comparison.
- The initial exact `windowTitle`, `nativeHandle`, and `processId` remain the native-generation authority.
- A difference in any of those three fields sets `nativeGenerationRetired = true` and throws typed `WINDOW_MISMATCH` before returning metadata to the caller.
- Once set, the latch is never assigned `false` or otherwise reset. A later value-equal A' slot still fails because the latch check precedes return.
- Missing binding, device mismatch, and logical-window mismatch keep their existing typed outcomes. Repair #2 did not alter their decisions or add new business truth.

### Public API, stop/pause, and retry

- The byte-exact production delta check establishes that all public declarations and implementations are unchanged from Repair #1.
- `throwIfStopRequested()` still delegates turn-native checkpointing to the existing metadata path and preserves legacy delegation for the old path.
- Stop remains a typed stop exception. Interruption remains a stop exception. Pause remains a 250 ms metadata-poll loop with stop/interruption checks and reported blocked duration; Repair #2 did not alter ordering or cadence.
- `latestExactTurnMetadata()` performs one `TurnGameClient.latestWindowMetadata()` read per invocation and contains no retry loop.
- `TurnGameClient.latestWindowMetadata()` resolves the exact bound context and calls only the metadata port. It does not obtain an action UUID, build/publish an action, or execute a command.
- No TTL, retry, extra verification read, command fallback, park/yield, cleanup, or new cloud gate was added.

## Contract evidence review

### Exact-positive path

The exact-positive contract creates one exact metadata object and scripts exactly one slot. One public `throwIfStopRequested()` call then proves:

- Return value is `0L`.
- UUID supplier calls remain `0`.
- Published actions remain empty.
- Command-port execute calls remain `0`.
- Metadata reads are exactly `1`.
- The scripted metadata deque is empty afterward.

Therefore the exact-positive path has zero UUID/action/command side effects, exactly one read, exactly one consumed slot, and no retry.

### Same initial-A context: A0 -> B -> A'

- One `TaskExecutionContext` is constructed from `initialA`; the context is not recreated or rebound between calls.
- `slotA0` and `slotAPrime` are separately constructed `TurnWindowMetadata` record instances.
- The contract asserts `initialA == slotA0` by value, `slotA0 == slotAPrime` by value, `slotA0 != slotAPrime` by object identity, `initialA != slotA0` by object identity, and `slotA0 != slotB` by value.
- `TurnWindowMetadata` and nested `TurnWindowRect` are Java records, so the value assertions cover every record component rather than reference identity.
- The one script is `[A0, B, A']`; it is not reset between checkpoints.
- A0 succeeds. B returns typed `WINDOW_MISMATCH` and retires the context. A' also returns typed `WINDOW_MISMATCH` because the same context-local latch remains retired.
- The final counters prove exactly three reads and zero remaining slots.
- `ScriptedCommandPort.latestWindowMetadata(...)` assigns the exact deque element removed by `removeFirst()` to `lastMetadata`; the final `assertSame(slotAPrime, lastMetadata.orElseThrow())` therefore proves the third and last exact object was A', not a copy or stale B.
- Aggregate UUID calls, action publications, and command executions remain zero across all three public checkpoints.

This closes the previously missing executable proof of value-equal A' non-revival on the same initial-A context.

### Shared cumulative +1 read / +1 slot

`assertTransition(...)` snapshots `metadataReads` and scripted-slot count before each public checkpoint, then requires `readsAfter == readsBefore + 1` and `slotsAfter == max(0, slotsBefore - 1)`.

All eight current call sites enter with at least one scripted slot, so `max(0, slotsBefore - 1)` cannot mask a zero-slot read:

| Call | Reads before -> after | Slots before -> after | Result |
|---|---:|---:|---|
| missing binding | 0 -> 1 | 1 -> 0 | `MISSING_BINDING` |
| wrong device | 0 -> 1 | 1 -> 0 | `IDENTITY_OR_SESSION_MISMATCH` |
| wrong logical window | 0 -> 1 | 1 -> 0 | `WINDOW_MISMATCH` |
| drifted title | 0 -> 1 | 1 -> 0 | `WINDOW_MISMATCH` |
| drifted native handle | 0 -> 1 | 1 -> 0 | `WINDOW_MISMATCH` |
| drifted process id | 0 -> 1 | 1 -> 0 | `WINDOW_MISMATCH` |
| same-context B after A0 | 1 -> 2 | 2 -> 1 | `WINDOW_MISMATCH` |
| same-context A' after B | 2 -> 3 | 1 -> 0 | `WINDOW_MISMATCH` |

The helper also rechecks zero UUID/action/command evidence on every denied checkpoint. The shared cumulative claim is therefore true for every present invocation, including the two calls made after an earlier read on the same harness.

## Severity accounting

- P0: 0.
- P1: 0.
- P2: 0.
- Required repair points: none.
- Residual execution evidence: not assessed in this source-only delivery review because the assignment explicitly forbids test, compile, and runtime execution; that prohibition does not weaken the source-level approval above.

TRUE_EOF REVIEW_COMPLETE
