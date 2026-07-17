# CR271 TURN-28S3 latest readiness helper R1

- Snapshot cutoff: `2026-07-16T11:10:41.7419690-04:00`.
- Role: CR271 Internal readiness helper only; not implementation owner, reviewer, approver, or parent.
- Result: `PRECHECK_COMPLETE / CONDITIONAL NEXT SLICE / CURRENT START GATE NOT OPEN`.
- This report does not create or claim TURN-28S3, does not review TURN-28S2, and does not change CR271 status.
- Actual write set: this report only. No Java, test, card, `ACTIVE_WORK`, dashboard, protocol, baseline, Maven,
  runtime, input, or Git mutation was performed.

## 1. Latest conclusion

The next bounded `NpcClickService` production slice after TURN-28S2 is:

> **TURN-28S3 - migrate the one active direct-combat failure-exit right-click submission to one exact-window
> HTTPS turn action.**

The candidate is mechanically ready to freeze, but it is **not source-start ready at this snapshot**. The latest
TURN-28S2 physical true EOF is:

`PARENT-RESTART FRESH-EXTERNAL-B-NEXT ZERO-OWNER INITIAL-SHA-UNCHANGED CLAIM-REQUIRED STRICT-696`

at `2026-07-16T11:03:03.155-04:00`. There is no later `EXTERNAL-B RESTART CLAIMED`, `SOURCE DELIVERED`, or parent
`SOURCE PASS`. Therefore this helper must not fabricate the premise “TURN-28S2 source pass has happened.” S3 is
the conditional next slice **after** that source pass and owner release.

## 2. Authority read

This pass read and cross-checked:

1. Full `AGENTS.md` and full `docs/DHXY_CONTEXT.md`.
2. The latest CR271 head in `docs/ACTIVE_WORK.md`, currently the `2026-07-16 / CR271 11:03` entry.
3. Sections 14-19 of
   `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`.
4. Full `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`.
5. Full `docs/业务逻辑.md`, including the strict `696a12b0` migration gate, direct-combat authorization, and NPC
   click FIFO/local-versus-Cloud responsibility rules.
6. Full TURN-28S2 child card through its current physical true EOF.
7. Full current Cloud `NpcClickService.java` and the strict baseline content at
   `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
8. Current public turn anchors used to validate this slice: `TurnGameClient`, `TurnInvocationContext`,
   `TurnInvocationResult`, `CloudTurnCommandResult`, `TaskExecutionContext`, `TaskExecutionContextHolder`,
   `TaskCheckpoint`, `TurnAction`, `TurnStep`, `TurnInputSpec`, `TurnOutcome`, `TurnStepResult`, and exact-window
   mouse sequence mapping/execution.

The sole business authority is strict commit
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

**无已批准业务差异；按基线等价迁移。**

## 3. Current source and baseline snapshot

Current Cloud production file:

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`

| Field | Current value |
|---|---|
| Lines | `3374` |
| Bytes | `175367` |
| SHA-256 | `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` |
| S2 turn machinery | absent; the four S2 Alt sites are still local `InputSequences` calls |

The Cloud `migration-baseline/696a12b0/.../NpcClickService.java` has the same line count, byte count, and SHA-256.
A read-only comparison against `git show 696a12b0...:src/main/java/com/bot/dhxy/service/NpcClickService.java`
found `3374` versus `3374` ordered lines and `0` ordered-line differences. Thus current production still carries
the strict baseline right-click loop unchanged; it is not a post-S2 snapshot.

The active S3 semantic anchor is current lines `711-754`, method
`exitDirectCombatClickModeAfterFailure(NpcClickRequest)`. Its one local input submission is current lines
`730-735`:

```java
inputSequences.submitAndWait("npcClick:directCombat:exitRightClick", List.of(
        InputAction.moveMouse(exitPoint.x, exitPoint.y),
        InputAction.sleep(120),
        InputAction.clickRight(exitPoint.x, exitPoint.y, 120),
        InputAction.sleep(600)
));
```

S2 will move these line numbers. A future S3 card must anchor by method and description, not copy today's line
numbers or restore today's `cce8...3441` bytes over S2.

## 4. Exact source-start gate

TURN-28S3 may be frozen and claimed only after all of the following are true, in this order:

1. A fresh TURN-28S2 worker appends a real true-EOF claim under the latest parent assignment.
2. TURN-28S2 produces a real Java increment and appends true-EOF `SOURCE DELIVERED` with its final
   `NpcClickService.java` SHA-256.
3. The parent independently reviews the delivered S2 source, records explicit `SOURCE PASS`, and releases the S2
   owner. S2 independent reviews/named tests/build remain TURN-28 final gates; they are not substitutes for this
   same-file source pass and are not required merely to open the next serial source slice.
4. At S3 freeze time, the current production SHA is recomputed and must equal the S2 delivered final SHA exactly.
   No S2 or other TURN-28 worker may still own or write `NpcClickService.java`.
5. The parent rereads the final S2 helpers/imports and the then-current public turn/context/executor surfaces.
   S3 must reuse S2's reviewed exact-context/terminal boundary without wrapper nesting. Any signature or terminal
   shape drift requires a refreshed readiness contract before claim.
6. The parent creates the fixed S3 child card with
   `S3.initialSha256 = S2.sourceDelivered.finalSha256`, the exact write set in section 5, and the strict contract
   below.
7. A fresh implementation worker appends a true-EOF claim to that S3 card before editing. A stale S2 heartbeat,
   this helper report, or an old S3 preflight is not ownership.

If any item is false, S3 remains unclaimed. In particular, today's `cce8...3441` is evidence that S2 has not
landed; it is not a legal future S3 initial SHA unless the parent-reviewed S2 delivery independently ends at that
same value.

## 5. Mutually exclusive write set

### Future TURN-28S3 implementation write set

Exactly two paths:

1. Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`.
2. Future append-only
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S3.md`.

S3 has no independent test write set. Parent TURN-28's sole
`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java` remains a later unified test
slice/gate.

This production write set is mutually exclusive with TURN-28S2 and every later TURN-28 slice that writes
`NpcClickService.java`. It may not overlap an active owner even though the source path is currently untracked in
the Cloud repository.

Read-only for S3: TURN-28/S1/S2 cards, this helper, `ObjectiveTextRecognizer.java`, `SmartClickRecognizer.java`,
all tests, protocol/client/context/executor/factory code, DHXY Java, `InputSequences`, `InputProvider`, OCR,
template, capture, Dialog, BattleRadar, Navigation, Task/caller, POM, resources, config, baseline trees, and every
other report/card.

### This helper's actual write set

Only `docs/superpowers/plans/reports/2026-07-16-turn-28s3-latest-readiness-helper-r1.md`.

## 6. Frozen business boundary

S3 replaces only the active local submission inside
`exitDirectCombatClickModeAfterFailure(...)`. It does not migrate or alter the anchor probe, OCR/formula, direct
combat entry, candidate pipeline, verifier, or caller result mapping.

The surrounding strict-696 flow remains:

1. Enter this method only after the direct-combat candidate pipeline returned `false` and the task is not stopped.
2. Permit at most `DIRECT_COMBAT_EXIT_ATTEMPTS == 3` business attempts.
3. On every reached attempt, recompute the purple/player anchor through the existing method. If it is absent,
   use the exact current window rectangle fallback point `(left + 512, top + 424)`.
4. Execute one right-click mechanics action for that attempt.
5. Only after strict mechanics completion, run the existing stop checkpoint and exactly one
   `isDirectCombatClickModeLikely("npc-direct-combat-exit-attempt-" + attempt)` probe.
6. If the probe is `false`, return `true` immediately. The caller retains
   `positionRefreshRequired("direct-combat-failed-after-alt-a")`.
7. If the probe is `true`, perform the baseline `TaskSleep.sleep(300)` and continue according to the loop.
8. If all three probes remain `true`, preserve the final error log and `false`; the caller preserves its existing
   `IllegalStateException`. There is no fourth click, cleanup, navigation, restart, or hybrid fallback.

The anchor method's current exception-to-null fallback remains unchanged. S3 must not turn anchor/OCR failure into
a transport terminal or add another observation.

## 7. One HTTPS action, three protocol steps, four queue actions

Each reached business attempt makes exactly one public
`TurnGameClient.execute(orderedSteps, false, Duration.ofMillis(120_000L))` call. The `120_000ms` value is the
bounded synchronous transport wait, not a business TTL or retry budget.

The ordered protocol steps are exactly:

| Index | Turn step | Exact payload |
|---:|---|---|
| `0` | `INPUT / MOVE_MOUSE` | `x=exitPoint.x`, `y=exitPoint.y`; every other input field null |
| `1` | `WAIT` | `waitMs=120` |
| `2` | `INPUT / CLICK_RIGHT` | `x=exitPoint.x`, `y=exitPoint.y`, `clickDelayMs=120`, `queueHoldMs=600`; every other input field null |

Action invariants:

- `contractVersion=1`, exact current `deviceId/windowId`, `fullWindowFailureEvidence=false`.
- No CAPTURE, MATCH_TEMPLATE, LOCAL_SERVICE, raw frame, failure-evidence request, local input fallback, or second
  command.
- The public `TurnGameClient.execute(...)` generates the action UUID internally. `NpcClickService` does not create,
  pass, cache, or reuse that UUID.

Those three protocol steps must map to one indivisible global input-queue submission containing exactly four
physical queue actions:

```text
MOVE_MOUSE(x,y)
-> SLEEP(120)
-> CLICK_RIGHT(x,y, clickDelayMs=120)
-> SLEEP(600)              // generated by queueHoldMs inside the same queue request
```

There must be no fourth protocol `WAIT 600` step: `queueHoldMs=600` already creates the fourth physical queue
action. Adding a trailing protocol wait would duplicate the delay and may release queue ownership before the hold.
MOVE and CLICK_RIGHT must never become separate commands or separate queue submissions.

## 8. Exact context and pre-UUID gate

Every attempt independently performs the following; no metadata is cached across attempts:

1. Obtain the non-null current `TaskExecutionContext` from the existing holder and directly call
   `TaskCheckpoint.throwIfStopRequested(...)` without adding a checkpoint wrapper.
2. Preserve the same holder context object through the pre-command path.
3. Use its existing `TurnInvocationContext` and existing bound `TurnGameClient`; do not inject or construct a
   second client/factory/provider.
4. After the existing anchor probe, read the attempt's latest `TurnWindowMetadata`. Require exact
   device/window/title/native HWND/process id, positive current rect, and no context/window-generation drift.
5. Compute a null-anchor fallback from that exact rect. Require either anchor or fallback point to be
   screen-absolute and inside that exact rect.
6. Immediately before the UUID-producing call, checkpoint again as provided by the final reviewed S2 boundary and
   confirm the same context/latest metadata still applies.
7. Only then invoke `execute(...)` once.

Missing context, confirmed stop, interruption, invalid rect/point, or metadata/context drift before `execute` means
zero UUID, zero command, zero right-click, zero mode probe, zero 300ms sleep, and zero later attempt.

## 9. Strict completion acceptance

The right-click mechanics is complete only when all of these hold:

1. `TurnInvocationResult.commandStatus == COMPLETED`.
2. The real outcome exists and `outcome.status == COMPLETED`.
3. Invocation/action/outcome action IDs correlate to the one fresh UUID.
4. Outcome full `TurnWindowMetadata` equals the attempt's exact pre-command snapshot.
5. `failedStepIndex == null`.
6. There are exactly three results in order:
   `(0, INPUT, COMPLETED)`, `(1, WAIT, COMPLETED)`, `(2, INPUT, COMPLETED)`.
7. Every step has null match and null local-service result.
8. Outcome frame metadata and invocation raw frame are both absent.
9. There are no extra steps, failure evidence, capture, match, local Service, or unrelated result fields.

Only this shape may continue to the mode probe. A completed right-click is mechanical completion only; it does not
itself prove direct-combat mode exited.

## 10. Terminal, uncertainty, UUID, and zero retry

The following are terminal for the current S3 invocation:

- Command: `BUSY`, `DUPLICATE_ACTION_ID`, `TIMED_OUT_UNCERTAIN`, `INTERRUPTED_UNCERTAIN`.
- Outcome: `FAILED`, `STOPPED`, `DUPLICATE_OR_UNCERTAIN`.
- Missing/malformed outcome; action, device, window, title, HWND, process, rect, step, status, or frame correlation
  mismatch; non-null failed index; frame/match/local-result pollution; runtime/client exception.

Confirmed stop must propagate through the existing `TaskCheckpoint`/task-stop exception path. A reported
`STOPPED` or interrupted uncertainty without a currently confirmed task stop is fatal, not success. Every other
terminal/uncertain condition uses the S2-reviewed task-fatal path; it must not be converted to `submitted=false`,
ordinary click miss, mode still active, `positionRefreshRequired`, or another business fact.

After any terminal/uncertain result there must be:

- zero mode probe;
- zero baseline 300ms sleep;
- zero later attempt, later UUID, or later command;
- zero local/foreground fallback, cleanup, compensation, candidate, verifier, memory, or navigation;
- zero transport retry, replay, resend, or action-ID reuse.

The baseline maximum of three attempts is **not** permission to retry a failed/uncertain transport action. Attempt
`n+1` exists only when attempt `n` was strictly completed and its one mode probe still returned `true`. Each reached
business attempt therefore has a distinct fresh UUID; the maximum is three commands/three UUIDs.

## 11. Exact 300ms baseline semantics

Current source and strict commit both call `TaskSleep.sleep(300)` after every strictly completed right-click whose
mode probe remains `true`. The return value is currently not reinterpreted by this method. Preserve that exact
placement and behavior:

- Probe `false`: no 300ms; return success immediately.
- Probe `true` on attempt 1: one 300ms sleep, then attempt 2.
- Probe `true` on attempt 2: one 300ms sleep, then attempt 3.
- Probe `true` on attempt 3: **one 300ms sleep still occurs**, then the loop ends, logs the existing error, and
  returns `false` to the existing caller throw.

Thus an all-still-active run contains exactly three right-click commands, three mode probes, and three 300ms
sleeps. The 300ms sleep stays outside the HTTPS action and outside queue ownership. It must not be removed after
the third attempt, folded into `queueHoldMs`, made conditional on “another attempt exists,” or replaced by a
fourth attempt.

The older TURN-28 parent phrase “WAIT300 only before another attempt” is looser than the actual source. For S3,
the strict source is controlling and this report fixes the non-difference explicitly: preserve the third 300ms.

## 12. Acceptance matrix for the later TURN-28 named test

S3 itself does not own a test file, but the parent `NpcClickTurnContractTest` must eventually prove at least:

1. First probe false: one command/UUID, exact three protocol results, one four-action queue submission, no 300ms,
   no second command.
2. True then false: two distinct canonical UUIDs, two commands, one 300ms between them.
3. True/true/false: three distinct canonical UUIDs, three commands, exactly two 300ms sleeps.
4. True/true/true: three distinct canonical UUIDs, three commands, exactly three 300ms sleeps, existing false/
   caller-throw boundary, no fourth command.
5. Every command/outcome terminal and uncertainty class at attempts 1-3: immediate stop/fatal, no mode probe,
   no 300ms, no later UUID/command.
6. Pre-UUID missing/STOP/context/metadata/point drift: zero command and zero UUID consumption.
7. Exact anchor and null-anchor fallback coordinates; exact metadata return correlation; no frame/match/local
   result; no local `InputSequences` right-click fallback.

These are later parent test assertions, not tests run or authorized by this helper.

## 13. Explicit exclusions

S3 must not include S2's four Alt sites, S1 pending-proof cleanup, ordinary left-click migration, Ctrl probe/menu,
capture/pixel-change, OCR/template/recognizer, Dialog/BattleRadar, memory/proof, navigation, Task/caller, protocol,
context, executor, queue, config, or build changes. It must not add a helper stack, wrapper nesting, second service,
session, owner, ledger, TTL, durable workflow, extra verification/read, retry, cleanup, fourth attempt, or hybrid
fallback.

Parent TURN-28 still requires all remaining production slices, its one named test, assertion review, two independent
reviews, applicable build gates, and separate user fresh runtime evidence. This helper satisfies none of those
approval gates.

## 14. Read-only execution record

- No Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture, OCR, or input was run.
- No Git mutation was performed; read-only `status`, `rev-parse`, `hash-object`, `show`, and content/hash comparisons
  were used only for evidence.
- Both repositories' existing dirty and untracked files were preserved.
- No Java, test, card, `ACTIVE_WORK`, dashboard, baseline, or other document was modified.

<!-- TRUE_EOF: CR271 TURN-28S3 LATEST-READINESS-HELPER-R1 PRECHECK_COMPLETE START-GATE-CLOSED-UNTIL-S2-PARENT-SOURCE-PASS NON-IMPLEMENTER NON-REVIEWER NON-APPROVER 2026-07-16T11:10:41.7419690-04:00 -->
