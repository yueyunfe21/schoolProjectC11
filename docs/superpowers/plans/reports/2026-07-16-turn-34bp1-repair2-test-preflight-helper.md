# CR271 TURN-34BP1 Repair #2 Deterministic Test Preflight Helper

> Snapshot: `2026-07-16T11:29:47.0846502-04:00`  
> Role: CR271 Internal helper; not implementation owner, reviewer, approver, or parent.  
> Boundary: `PRECHECK_ONLY / TEST-EVIDENCE-AND-CLASS-DOC`; this report neither approves nor blocks TURN-34BP1.

## 1. Read Set And Current Authority

This pass read the repository `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current head of
`docs/ACTIVE_WORK.md`, the TURN-34BP1 child card through its physical true EOF, and the actual Cloud
`TaskExecutionContext.java` / `TaskExecutionContextTurnContractTest.java` bytes. It also read
`TurnWindowMetadata` and `TurnWindowRect` only to verify their record value semantics.

The latest child-card true EOF at this snapshot is line 293:

```text
TRUE_EOF: TURN-34BP1 PARENT-REVIEW-2 P0P1P2=0/1/2 REPAIR-2-REQUIRED
EXTERNAL-C-NEXT TEST-EVIDENCE-PLUS-CLASS-DOC 2026-07-16T11:26:00-04:00
```

The actual Repair #1 delivery bytes still match the card:

| Cloud file | Lines | SHA-256 |
|---|---:|---|
| `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java` | 524 | `f278460ba9dc664974a98ea5ef19532e60514b29015a2e9b25b8f49bf0eba895` |
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java` | 843 | `7caf01272346b2f647e67c825b11b1606ba38b81ee1e29ff65b56c3bc6b9dbbf` |

Per the latest true EOF, the production latch behavior is frozen. The bounded repair is deterministic test
evidence plus the production class-level description only. This helper made no Java/card change and performed no
Maven, runtime, application, server, Task, UI, capture, input, or Git operation.

### 1.1 Concurrent Owner Update After The Frozen Preflight Snapshot

After the evidence above was frozen, External C appended `EXTERNAL-C REPAIR #2 CLAIMED` at card lines 295-301 at
`2026-07-16T11:31:10.774-04:00`. At `11:31:46-04:00`, the physical card tail was that active claim while the latest
completed `TRUE_EOF` marker remained the parent Repair #2 boundary at line 293. Production still matched
`f278460b...`; the named test had begun active WIP at 857 lines / SHA
`52c9b496ac4cfee32ad5c19137009610b44941562895f78a01d6c917558cb7ca`.

That later byte stream is not a canonical delivery and is outside this requested preflight target. The analysis
below remains deliberately pinned to the Repair #1 delivery SHA `7caf0127...`; it does not review, approve, reject,
or otherwise characterize External C's in-progress Repair #2 implementation.

## 2. Shared `assertTransition` Deterministic Failure

The failure is mechanically confirmed from the current test source:

- `ScriptedCommandPort.scriptMetadata(...)` at test lines 817-822 clears the deque and resets
  `metadataReads` to zero.
- `latestWindowMetadata(...)` at lines 833-840 increments the cumulative counter once and consumes at most one
  queued slot per public checkpoint.
- The A0 -> B -> A' test at lines 444-463 scripts three slots once. A0 calls the public checkpoint directly, so
  the cumulative state becomes `metadataReads=1`, `slots=2`.
- The B call enters shared `assertTransition(...)` at lines 639-655. Its public checkpoint changes the cumulative
  state to `metadataReads=2`, `slots=1`, but line 654 still asserts absolute `metadataReads==1`.
- Therefore the test deterministically fails inside the B helper call. The A' call and final
  `metadataReads==3`/script-exhaustion assertions are unreachable.

The shared helper should keep its existing typed-decision and zero-command checks, but measure one-call deltas:

```java
int metadataReadsBefore = harness.port.metadataReads;
int scriptedSlotsBefore = harness.port.metadataScript.size();

// Existing holder/public-checkpoint call and typed decision assertions stay unchanged.

assertZeroCommandEvidence(harness);
assertEquals(0, harness.port.executeCalls);
assertEquals(metadataReadsBefore + 1, harness.port.metadataReads);
assertEquals(scriptedSlotsBefore - 1, harness.port.metadataScript.size());
```

This yields the required sequence without resetting state:

| Public checkpoint | Reads before -> after | Slots before -> after |
|---|---:|---:|
| standalone negative case | `0 -> 1` | `1 -> 0` |
| B after A0 | `1 -> 2` | `2 -> 1` |
| A' after B | `2 -> 3` | `1 -> 0` |

The helper must not clear/re-script the deque, reset counters, poll, sleep, or retry. One public call must account
for exactly one additional metadata read and exactly one consumed scripted slot.

## 3. Exact-Positive Zero-Evidence Gap

`exactNativeGenerationPassesTheCheckpointWithZeroCommand()` at test lines 429-435 currently proves only:

1. the public checkpoint returns `0L`;
2. `executeCalls==0`.

It does not call `assertZeroCommandEvidence(...)`, and it does not assert one metadata read or one-slot exhaustion.
The minimal direct completion after the existing public call is:

```java
assertZeroCommandEvidence(exact);       // uuids.calls == 0 and actions.isEmpty()
assertEquals(0, exact.port.executeCalls);
assertEquals(1, exact.port.metadataReads);
assertTrue(exact.port.metadataScript.isEmpty(), "the exact slot must be consumed");
```

The existing helper already provides absolute zero UUID/action evidence. No new helper, reflection seam, command,
or retry mechanism is needed.

## 4. Explicit A0 / A' Value And Identity Evidence

The current A0 and A' objects are constructed inline at test lines 446-452. Comments call them value-equal and
object-distinct, but no executable assertion proves either claim.

`TurnWindowMetadata` and its nested `TurnWindowRect` are both Java records, so separately constructed instances
with the same components are value-equal. The bounded test repair should name the three observed slots before
scripting:

```java
TurnWindowMetadata observedA0 = nativeWindow(
        INVOCATION, "game-window-exact", "0x1234", 4321L, false, false);
TurnWindowMetadata generationB = nativeWindow(
        INVOCATION, "game-window-b", "0xBBBB", 2222L, false, false);
TurnWindowMetadata reboundA = nativeWindow(
        INVOCATION, "game-window-exact", "0x1234", 4321L, false, false);

assertEquals(observedA0, reboundA);
assertNotSame(observedA0, reboundA);
boundToA.port.scriptMetadata(List.of(
        Optional.of(observedA0), Optional.of(generationB), Optional.of(reboundA)));
```

After all three public calls, retain the aggregate read/script/zero evidence and additionally pin the object that
was actually observed last:

```java
assertSame(reboundA, boundToA.port.lastMetadata.orElseThrow());
```

This identity assertion is deterministic because `latestWindowMetadata(...)` assigns the deque's exact
`Optional<TurnWindowMetadata>` element to `lastMetadata`; it does not copy or reconstruct the record. The required
`assertEquals`, `assertNotSame`, and `assertSame` imports already exist in the test.

## 5. Class JavaDoc-Only Correction

The production class JavaDoc at `TaskExecutionContext.java:26-29` currently says the turn-native factory stores
only powerless immutable scope/identity/metadata plus the bound client view. That description is stale because the
class now also owns `nativeGenerationLock` and the private monotonic `nativeGenerationRetired` safety latch at
lines 43-50.

Only the class-level prose should change. It should state that the turn-native context stores powerless immutable
scope/identity/metadata, the bound client view, and a private context-local monotonic native-generation safety
latch that can only retire the context. It should retain the existing statement that none of these creates an
owner, session, ledger, transport, task runtime, or replacement lifecycle authority.

This is the production class JavaDoc identified by the parent true EOF, not a request to add test-class JavaDoc or
to change fields, synchronization, checkpoint ordering, or latch behavior.

## 6. Minimal Delivery Checklist For The Owner

- Modify existing `assertTransition(...)` in place to assert one-read/one-slot deltas from pre-call snapshots.
- Preserve all typed decision assertions, absolute zero UUID/action evidence, and `executeCalls==0`.
- Add exact-positive zero UUID/action, `metadataReads==1`, and script-exhaustion assertions.
- Name A0, B, and A'; assert A0/A' value equality and object distinction before scripting.
- After the three calls, assert `lastMetadata` holds the exact A' object and preserve aggregate
  `metadataReads==3`, empty script, zero execute/action/UUID evidence.
- Correct only `TaskExecutionContext` class-level JavaDoc; production logic remains byte-for-byte read-only.
- Keep the existing 11-test surface and all unrelated files/tests unchanged.
- Do not introduce reset, retry, polling, sleep, reflection/source-scan proof, helper nesting, or a new test class.

This report supplies only a deterministic implementation precheck. It does not claim a delivery, review result,
approval, rejection, severity decision, compile result, or runtime result.

TRUE_EOF PRECHECK_COMPLETE
