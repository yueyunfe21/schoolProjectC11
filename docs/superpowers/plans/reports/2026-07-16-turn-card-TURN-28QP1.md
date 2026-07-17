# TURN-28QP1 - InputActionRequest compile-surface correction

## PARENT FROZEN CARD - EXTERNAL-A NEXT - 2026-07-16T10:27:00-04:00

- Card type: bounded real DHXY production implementation slice of TURN-28Q; not helper/reviewer work.
- Status: `READY / CLAIM REQUIRED / SOURCE-START OPEN`.
- Owner after true-EOF claim: CR271 External Worker A.
- Business authority: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved behavior difference.

## Exact write set

1. DHXY `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`.
2. This append-only child card.

Initial production: 1148 lines, SHA-256
`4e40fcd4ce64b9cc5b7c1d4c6f5cf308dcb9933050629b687fae104105ec0652`.

Read-only snapshots:

- `InputActionQueue.java`: `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a`;
- `InputActionWorker.java`: `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43`;
- `InputActionFrozenExclusiveContractTest.java`:
  `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c`.

Every other production/test file, POM, caller and card is read-only.

## Frozen implementation contract

At `InputActionRequest.isFrozenExactWindowGenerationCurrent()`, replace only the unresolved
`Objects.equals(windowId, windowContext.getWindowId())` reference with
`java.util.Objects.equals(windowId, windowContext.getWindowId())`, matching existing same-file uses. Do not add
an abstraction, change the detector order, widen/narrow the generation witness, alter stop/cancel typing, or touch
queue/worker/test behavior. This is one symbol-resolution correction with zero business-semantic difference.

No auto retry/re-enqueue/replay/session/ledger/TTL/durable workflow. This child alone is not approval. Parent
source/test-source review, two independent reviews, authorized named test and applicable compile remain required.

## Claim and delivery

External A must append `EXTERNAL-A CLAIMED` here before editing and make the one production increment in its first
five-minute heartbeat window. Delivery is one true-EOF `EXTERNAL-A SOURCE DELIVERED` with final SHA/line evidence,
then stop editing. No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation
while another Java writer is active.

<!-- TRUE_EOF: TURN-28QP1 PARENT-FROZEN EXTERNAL-A-NEXT CLAIM-REQUIRED ONE-LINE-PRODUCTION-COMPILE-SURFACE 2026-07-16T10:27:00-04:00 -->

## EXTERNAL-A CLAIMED - 2026-07-16T10:30:52.681-04:00

- Lane report: `docs/superpowers/plans/reports/2026-07-16-cr271-external-worker-a.md`. Platform session id
  `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`, model `claude-opus-4-8`. No spawn nickname was surfaced, so none is
  fabricated. Implementation Worker only; this is not an approval and I create no reviewer.
- Continuing-session disclosure stands: I am the same A lane that has held the earlier 28Q/QT1 children, not a
  freshly restarted task. This card is one line, so context is not a risk here.
- **The blocker is mine and I confirm it against the bytes.** `InputActionRequest.java:458` is the only bare
  `Objects.equals(...)` in the file; there is no `import java.util.Objects` (imports at `:8-16`), while the same
  file already qualifies it at `:910`, `:933`, `:971`. I wrote that line in 28Q Repair #2 while making the
  generation witness pure — I copied the detector's comparison but dropped its qualification. The file cannot
  resolve the symbol, so it cannot compile. The compile ban means I could not have caught this by running
  anything; review did, and the finding is correct.
- Snapshot verified at claim time: `InputActionRequest.java` 1148 lines `4e40fcd4...` — identical to this card.
  Read-only and unmoved: `InputActionQueue.java` `c53a423e...`, `InputActionWorker.java` `225a9f3b...`,
  `InputActionFrozenExclusiveContractTest.java` `f72c7db0...`.
- Scope accepted verbatim: qualify that one reference as `java.util.Objects.equals(...)`. I will not add an
  import, abstraction or helper, reorder the detector, widen or narrow the witness, alter stop/cancel typing, or
  touch queue/worker/test bytes. Zero business-semantic difference.
- Hard constraints: no Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input (External C
  owns an active Java test repair); zero Git mutation; both repos' dirty/untracked protected; no other write set
  or log touched.

<!-- TRUE_EOF: TURN-28QP1 EXTERNAL-A CLAIMED SNAPSHOT-VERIFIED-IDENTICAL ONE-LINE-SYMBOL-RESOLUTION 2026-07-16T10:30:52.681-04:00 -->

## EXTERNAL-A SOURCE DELIVERED - 2026-07-16T10:31:44.864-04:00

Implementation Worker delivery only. Not an approval, not a review verdict, not a source pass. Parent
source/test-source re-review, two independent reviews, the authorized named test and applicable compile all
remain.

| File | Lines | SHA-256 final | was |
|---|---:|---|---|
| `InputActionRequest.java` | 1148 | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` | `4e40fcd4...` |

Line count is unchanged because exactly one line changed. Read-only files verified unmoved at delivery:
`InputActionQueue.java` `c53a423e...`, `InputActionWorker.java` `225a9f3b...`,
`InputActionFrozenExclusiveContractTest.java` `f72c7db0...`. Nothing else touched. `git diff --check` exits 0.

### The fix

`InputActionRequest.java:458`, inside `isFrozenExactWindowGenerationCurrent()`:

- before: `if (windowId == null || !Objects.equals(windowId, windowContext.getWindowId())) {`
- after:  `if (windowId == null || !java.util.Objects.equals(windowId, windowContext.getWindowId())) {`

Nothing else. No import added, no abstraction, no helper, no detector reordering, no change to the witness's
breadth, no change to stop/cancel typing, no queue/worker/test byte moved. Zero business-semantic difference;
`696a12b0` baseline untouched.

### Evidence

- The file has no `java.util.Objects` import (imports are `:8-16`), and `:458` was the **only** bare `Objects.`
  reference in all 1148 lines; bare `Objects.` occurrences are now **0**.
- The same file already qualifies the identical call at `:910`, `:933` and `:971`, so this now matches existing
  same-file convention exactly rather than introducing a new style or an import that would touch unrelated lines.

### Ownership of the defect

I wrote `:458` in TURN-28Q Repair #2 while making the generation witness pure: I lifted the comparison out of
`detectFrozenExactWindowFailure`, which uses the fully-qualified form, and dropped the qualification on the way.
It was a static symbol-resolution failure sitting in frozen production for three review rounds. I could not have
found it by running anything — the compile ban is in force while other Java writers are active — but I also did
not find it by reading, and I asserted the file's line evidence twice without noticing. Review caught it; the
finding is correct and I contest nothing.

### Accepting the parent's correction to my own reasoning

The parent's Review #2 also corrected me: `WindowNativeBinding` does **not** override `equals`/`hashCode`, so the
old `assertEquals` on focus bindings was already an identity comparison today. My QT1 Repair #1 delivery claimed
that a value-equal copy would have passed it — that justification was wrong, and I accept the correction. The
part of P1-3 that was real stands on its own: a single "last focus" reference genuinely could let a wrong first
focus be overwritten by a correct second, and ordered `assertSame` closes that and keeps the contract explicit
if that class ever gains value equality.

### Honest gate record

- **Not run (External C owns an active Java test repair; I claim nothing about them):** Maven, JUnit, compile,
  package, runtime, application, server, Task, UI, capture, input. This correction is **unverified by
  execution**: I assert only that the symbol is now qualified the same way the file already qualifies it three
  times, which is a reading of the bytes, not a compile.
- Zero Git mutation. Both repos' dirty/untracked protected. No reviewer self-created. I stop editing now.
- Continuing-session disclosure stands: I am not a freshly restarted lane.

<!-- TRUE_EOF: TURN-28QP1 EXTERNAL-A SOURCE DELIVERED ONE-LINE-SYMBOL-QUALIFIED BARE-OBJECTS-ZERO READ-ONLY-FILES-UNMOVED NOT-COMPILED NOT-RUN AWAITING-PARENT-REVIEW 2026-07-16T10:31:44.864-04:00 -->

## PARENT SOURCE REVIEW #1 - 2026-07-16T10:38:00-04:00

- Verdict: **`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`**. Current production is 1148 lines, SHA
  `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8`; line 458 is exactly the frozen
  `java.util.Objects.equals(...)` qualification and no bare `Objects.` reference remains.
- Read-only snapshots are exact: queue `c53a423e...`, worker `225a9f3b...`, named test `f72c7db0...`. No detector
  ordering, generation-witness breadth, stop/cancel typing, queue/worker/test behavior or business semantics moved.
- External A's QP1 owner is released. TURN-28Q now enters independent R1/R2 plus stable-writer named-test/compile
  pending; this source pass is not CARD APPROVED. No Maven was run while External C owns active Java repair.

<!-- TRUE_EOF: TURN-28QP1 PARENT-SOURCE-REVIEW-1 PASSED P0P1P2=0/0/0 OWNER-RELEASED INDEPENDENT-REVIEW-BUILD-PENDING 2026-07-16T10:38:00-04:00 -->
