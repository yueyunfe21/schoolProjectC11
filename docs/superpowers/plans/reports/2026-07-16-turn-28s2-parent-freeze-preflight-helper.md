# CR271 TURN-28S2 parent-freeze preflight helper

## 1. Role and evidence snapshot

- Role: internal read-only/preflight helper only. This report is not a card claim, implementation delivery,
  independent review, approval, blocker, or parent-final judgment.
- Snapshot: `2026-07-16T09:25:04.6000360-04:00`.
- Read authority: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top CR271 block in
  `docs/ACTIVE_WORK.md`, Sections 14-19 of the authoritative migration plan, the HTTPS-turn protocol
  specification, the NPC Click section of `docs/业务逻辑.md`, both repository statuses, the current source,
  strict baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, and
  `2026-07-16-turn-28-next-slice-decomposition-helper.md`.
- Repository snapshot: DHXY branch `thin-client-design`, HEAD
  `0114604e1ff5f15491d2910959c45252e893d04f`; Cloud branch `navigation-migration`, HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`. Both worktrees already contain protected dirty/untracked
  work. These HEADs are repository snapshots, not business baselines.
- The only business baseline used here is strict `696a12b0`.

**Non-binding source-start recommendation:** the parent may freeze one production-only child slice named
`TURN-28S2 - Cloud NpcClickService active Alt-shortcut HTTPS-turn cutover`, subject to the exact claim-time
SHA gate below. This helper does not create or freeze that card.

## 2. Exact production and test write set

### 2.1 Production write set: exactly one file

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`

Claim-time SHA-256 must be exactly:

`cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`

The current file and
`D:/mavenProject/dhxy-cloud-brain/migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java`
have that same SHA and are byte-identical at this snapshot. If the production SHA changes before true EOF
claim, the worker must not restore, overwrite, or merge from the mirror; the parent must re-read the new bytes
and re-freeze the write set and line evidence.

### 2.2 Test write set: intentionally empty

TURN-28S2 creates or modifies no test file. In particular, the parent-owned unique named test
`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java`
remains read-only/absent for this source slice. Its S2 cases are retained in Section 8 for the parent final gate.

This empty test write set is deliberate. The authoritative plan assigns one complete named contract test to
TURN-28; a partial S2-only test or a second test class could be mistaken for the parent test gate and is not
authorized.

### 2.3 Process write set for the future implementation owner

After the parent freezes it, the implementation owner may append only its claim/delivery evidence to:

`D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md`

The child card does not exist at this helper snapshot. The parent must create and freeze it before any Java
claim. All other cards, reports, plans, `ACTIVE_WORK`, dashboard files, and migration matrices are read-only to
the S2 worker.

### 2.4 Read-only reference hashes

| Reference | Snapshot SHA-256 | S2 use |
|---|---|---|
| Cloud `TurnGameClient.java` | `a8f64d8dbb5f9ed2852975d518836e25af92073f9c818d5f7e9da7cf18056cb9` | public client boundary only |
| Cloud `TaskExecutionContext.java` | `6d4e4a20a6fb4b6dba6a59cb45e95dd39c78a0415b9b2a650d75f9704151d003` | exact binding/client access only |
| Cloud `TaskExecutionContextHolder.java` | `3fa2729917449fbb75bf72614e46a223526ea2acb53dc96351886559192c6f3b` | current-context preflight only |
| Cloud `SmartClickRecognizer.java` | `ffbd984a4ed5841ccba6b87bf3378a1e0cb1e7d2bea68be3eed656be7324f102` | unchanged |
| Cloud `ObjectiveTextRecognizer.java` | `d3dc3cc247058ae85a6258e6173f8d9b56d7be119443c90a24c4bf6f180f3fe1` | unchanged |
| DHXY `BoundWindowKeyboardService.java` | `37d97cfb569bcca49d0b955d0ec462bf811ef7c49fda156f6457b5642f1330fe` | proves `ALT_4/A/C` background support |
| DHXY `TurnKeyMapper.java` | `d2bf68450c2a0709045b6bc3f03c9447e84dd195940713019e3b0cfbba0a8958` | proves accepted shortcut mapping |

No reference file is part of the S2 write set.

## 3. Four and only four shortcut migrations

The implementation must replace the following four live top-level mechanics sites in place. Their surrounding
business branches, order, conditions, return values, and fallback count remain unchanged.

| Current production site | Baseline condition and next business line | HTTPS action |
|---|---|---|
| `clickNpcSmart`, current lines `624-627` | first full non-combat pipeline missed; after successful mechanics run exactly one second full pipeline | `ALT_C`, then `WAIT 700ms` |
| `tryDirectCombatTargetClick`, current lines `667-670` | `detectFlyingState(...) == FLYING`; after successful mechanics continue the same direct-combat preflight | `ALT_C`, then `WAIT 700ms` |
| `tryDirectCombatTargetClick`, current lines `682-685` | flying state is grounded, or confirmed flying already dismounted; enter direct-combat mode before the existing candidate pipeline | `ALT_A`, then `WAIT 350ms` |
| `prepareNpcPipelineNameLayerOnce`, current lines `948-951` | ordinary pipeline name-layer preparation at its existing position | `ALT_4`, then `WAIT 400ms` |

The private legacy `InputProvider.pressAlt4()` occurrences around current lines `3271` and `3302` are not among
the four active top-level targets. They and every other input/capture/click/Ctrl site remain untouched for later
slices.

The implementation must not broaden this slice to left/right click, Ctrl probe, OCR, template matching,
recognizers, capture, dialog verification, BattleRadar, memory, navigation, or task callers.

## 4. Real public TurnGameClient boundary

### 4.1 Required public path

Each shortcut must use the existing real public path, not a local fake, adapter, or direct DHXY class:

1. Obtain the current `TaskExecutionContext` from the injected existing `TaskExecutionContextHolder`.
2. Call `TaskCheckpoint.throwIfStopRequested(context, ...)` directly before mechanics.
3. Require the holder still contains the same context object.
4. Obtain the exact `TurnInvocationContext` through `context.getTurnInvocationContext()` and the already bound
   client through `context.getTurnGameClient().bind(binding)`.
5. Read `TurnGameClient.latestWindowMetadata()` and require exact `deviceId`, `windowId`, native HWND, process id,
   and a positive current rectangle against that context.
6. Reconfirm the same holder context immediately before the UUID-producing call.
7. Call `TurnGameClient.execute(orderedSteps, false, Duration.ofMillis(120_000L))` exactly once.

The `120_000ms` value is only the existing positive transport wait fence. It is not a business sleep, retry
budget, TTL, or permission to resend.

One private method inside `NpcClickService` is acceptable only if it owns this entire preflight, action creation,
public invocation, correlation, and terminal projection boundary. It must not introduce wrapper nesting or route
through another new helper/service/facade.

### 4.2 Minimal JSON action shape

Each Java `execute` call must serialize to the following minimal action shape, with one fresh action UUID and the
exact current device/window identity. This is the concrete generic-retry `ALT_C/700ms` instance; the other three
instances may change only `input.key` and `waitMs` according to the Section 3 table.

```json
{
  "contractVersion": 1,
  "actionId": "<fresh UUID for this one shortcut invocation>",
  "deviceId": "<exact current deviceId>",
  "windowId": "<exact current windowId>",
  "steps": [
    {
      "index": 0,
      "type": "INPUT",
      "inputAction": "KEY_TAP",
      "input": {"key": "ALT_C"}
    },
    {
      "index": 1,
      "type": "WAIT",
      "waitMs": 700
    }
  ],
  "fullWindowFailureEvidence": false
}
```

There is no capture, frame request, match, local service, mouse coordinate, `clickDelayMs`, `queueHoldMs`,
session, taskRun id, owner, ledger, or caller-generated UUID in this action. The remaining exact instances are
`ALT_C/700ms` for confirmed-flying dismount, `ALT_A/350ms` for direct-combat mode entry, and `ALT_4/400ms` for
ordinary-pipeline name-layer preparation.

The key and its baseline delay remain in the same ordered action. Do not split `KEY_TAP` and `WAIT` into two
public commands, add a second wait, call `InputSequences`/`InputProvider`, or use a foreground fallback.

## 5. Completion, terminal, uncertainty, UUID, and retry contract

### 5.1 Normal completion

Normal return to the existing caller is allowed only when all of these are true:

- `commandStatus == COMPLETED`;
- `outcome.status == COMPLETED`;
- the echoed `actionId`, device/window identity, native HWND, process id, and window rectangle correlate with the
  one preflight snapshot;
- exactly two step results exist in order: index `0`, type `INPUT`, status `COMPLETED`; index `1`, type `WAIT`,
  status `COMPLETED`;
- `failedStepIndex`, frame metadata, raw frame, match result, and local-service result are absent.

A completed shortcut is mechanics completion only. It must not fabricate NPC-found, dialog-open, click-success,
combat-entered, verifier-success, or OCR truth. It merely permits the exact next strict-696 business line.

### 5.2 Terminal and uncertain outcomes

The following are terminal for this one call and must never be converted to `false`, a business miss, normal
`SKIPPED`, or success:

- command `BUSY`, `DUPLICATE_ACTION_ID`, `TIMED_OUT_UNCERTAIN`, or `INTERRUPTED_UNCERTAIN`;
- outcome `FAILED`, `STOPPED`, or `DUPLICATE_OR_UNCERTAIN`;
- missing/malformed outcome, correlation rejection, step mismatch, metadata drift, runtime client failure, or
  holder/context drift.

For a confirmed task stop, project through the existing checkpoint/stop exception. For every other terminal or
uncertain condition, use the existing task-fatal path. A terminal result permits zero later shortcut, pipeline,
candidate, click, verifier, memory action, compensation, or fallback command.

### 5.3 UUID and zero-retry invariants

- One reached shortcut site calls public `TurnGameClient.execute(...)` once and therefore produces exactly one
  fresh UUID and one command.
- The two distinct `ALT_C` business sites are distinct invocations and must receive distinct UUIDs when both are
  reached; no UUID is cached or supplied by `NpcClickService`.
- A second full non-combat pipeline is baseline business flow after a successfully completed generic `ALT_C`; it
  is not transport retry. It must not repeat the shortcut unless the strict-696 caller reaches that separate
  business site again.
- There is no automatic retry, replay, resend, same-UUID execution, session, permit, owner, ledger, TTL, durable
  workflow, local uncertainty resolution, or cleanup action.

## 6. Strict 696a12b0 business-equivalence checklist

The worker may move only physical ownership of the four key actions. It must preserve these decisions exactly:

1. `clickNpcSmart` still runs the first complete pipeline before any generic dismount. `COMBAT_TARGET` still gets
   zero generic `ALT_C`. Every other first-pipeline miss gets exactly one `ALT_C + 700ms`, then exactly one second
   full pipeline, never a third.
2. Ordinary pipeline execution still performs exactly one `ALT_4 + 400ms` at the existing name-layer position.
   Direct-combat candidate execution still skips that preparation.
3. Direct combat still handles null/STOP first. `FLYING` gets exactly one `ALT_C + 700ms`; `UNKNOWN` emits zero
   key command and returns the existing skip; grounded state emits no dismount. The continuing grounded path gets
   exactly one `ALT_A + 350ms` before the existing candidate pipeline.
4. Only the existing BattleRadar verification may establish direct-combat success. Shortcut completion itself is
   never combat success.
5. Wubei tooltip-first ordering, dialog gates, early/late memory, tooltip position, TENTATIVE cutoff,
   yellow/purple/Ctrl order, right-click exit, candidate budgets, verifier count, all other waits, public method
   signatures, and public return meanings remain unchanged.
6. No local OCR/business judgment, extra read/capture, fallback, retry, phase, park/yield, cleanup, TTL, or new
   cloud gate is introduced.

**无已批准业务差异；按 strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。**

## 7. Source-start gate versus parent/final gates

### 7.1 TURN-28S2 source-start prerequisites

The proposed S2 source may start only after the parent does all of the following:

- creates and freezes the unique TURN-28S2 child card at physical true EOF;
- records the one production path, empty test write set, and the exact claim-time SHA from Section 2;
- confirms TURN-28S1 parent source review remains passed and the current `NpcClickService.java` still has SHA
  `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`;
- confirms the existing public `TurnGameClient`, exact-context APIs, protocol models, `TurnKeyMapper`, and
  background-supported `ALT_4/A/C` path remain present;
- receives one real true EOF claim from the assigned worker before any Java edit.

This keyboard-only source-start does **not** wait for:

- TURN-28P final review/test/build gates; 28P owns shared capture/pixel-change mechanics, not these background key
  taps;
- TURN-22 final review/test/build gates; TURN-22's click timing/executor repair has a different write set and S2
  uses the already present supported background-key path;
- completion of all remaining TURN-28 mouse/capture/Ctrl/recognizer slices;
- TURN-28's unique named test, two independent reviewers, or final Cloud compile/build.

Those are not waivers. They are later integration/final gates and must not be misrepresented as source-start
requirements for this isolated one-file keyboard slice.

### 7.2 TURN-28S2 source-delivery boundary

An S2 worker delivery is only material for parent source review when:

- the diff is limited to `NpcClickService.java` plus append-only evidence in the frozen S2 child card;
- exactly the four sites in Section 3 cross the real public HTTPS-turn boundary;
- all surrounding strict-696 decisions are preserved;
- the terminal/uncertain/UUID/zero-retry invariants in Section 5 are visible in production;
- the read-only reference files retain their recorded bytes;
- the worker records initial/final SHA and exact line evidence, then releases ownership without self-approval.

S2 source delivery or parent source review of S2 is not `TURN-28 SOURCE+TEST SOURCE REVIEW PASSED`, not
`TURN-28 CARD APPROVED`, and not approval of TURN-28P or TURN-22.

### 7.3 TURN-28 parent final gates retained

The TURN-28 parent still owns and must later satisfy all of the following:

- every remaining frozen production slice, including mouse/capture/Ctrl/recognizer work;
- applicable TURN-28P and TURN-22 shared-mechanics integration gates where the parent production/test matrix uses
  them;
- the one complete `NpcClickTurnContractTest` named test containing S2 and all other parent cases;
- parent source/test-source review, two independent reviewer approvals, the authorized named test, and applicable
  Cloud compile/build after all Java writers are stable.

This helper neither removes nor satisfies any final gate.

## 8. Assertions reserved for the parent unique named test

Although S2 has an empty test write set, the final parent `NpcClickTurnContractTest` must exercise the real public
`NpcClickService` and retain at least these S2 assertions:

1. Non-combat first-pipeline miss emits exactly one action containing `ALT_C/700`, then exactly one second
   pipeline; `COMBAT_TARGET` emits zero generic `ALT_C`; no third pipeline exists.
2. Direct `FLYING`, `UNKNOWN`, and grounded cases emit respectively one dismount `ALT_C/700`, zero command, and
   zero dismount command before the common continuing `ALT_A/350` path.
3. Each ordinary pipeline emits one `ALT_4/400`; direct-combat candidate execution emits none.
4. Each reached shortcut gets one fresh UUID, one command, exactly two correlated steps, and no frame.
5. `BUSY`, duplicate, timed-out uncertain, interrupted uncertain, outcome `FAILED/STOPPED/`
   `DUPLICATE_OR_UNCERTAIN`, metadata drift, context drift, and malformed results each produce zero later command
   and no fabricated boolean or `DirectCombatClickResult` success.
6. No automatic retry/replay occurs, including after uncertain outcomes; a confirmed stop follows the existing
   checkpoint path.

The parent test must not replace the real public `TurnGameClient` boundary with a synchronous local input fake.

## 9. Mutual exclusion and prohibited expansion

- S2 has zero production/test write-set intersection with TURN-28P and TURN-22.
- S2 has zero intersection with the current TURN-34A test work and with DHXY queue/executor tests.
- No second owner may edit Cloud `NpcClickService.java` while the future S2 claim is active.
- If source changes before claim, ownership is ambiguous, or the frozen APIs drift, stop for parent re-freeze; do
  not overwrite protected dirty/untracked work.
- Do not edit recognizers, protocol, action factory, command port, Task context/checkpoint classes, DHXY keyboard
  mapping/executor, POM, config, resources, callers, cards other than the future S2 card, plan, ACTIVE_WORK,
  matrix, or dashboard.
- Do not run Maven, JUnit, compile/package, runtime/application/server/Task/UI, capture, or input while this
  source-start brief is being prepared. The applicable parent build/test gate remains later and parent-owned.

This helper changed no Java, test, plan, ACTIVE_WORK, card, dashboard, repository status, or Git state. Its only
write is this report, and it makes no approval or blocking decision.

TRUE_EOF PRECHECK_COMPLETE
