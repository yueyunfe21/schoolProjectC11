# CR271 TURN-28 next-slice decomposition helper

## 1. Role, authority, and snapshot

- Role: TURN-28 next-slice decomposition helper only. This helper is not an implementation owner, not an
  independent reviewer, and cannot approve, close, claim, or freeze TURN-28, TURN-28S1, or any proposed child
  slice.
- Snapshot time: `2026-07-16T09:06:33.9192793-04:00`.
- Read authority: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/业务逻辑.md`, TURN-28, TURN-28S1, TURN-28Q,
  TURN-34A, the TURN-28 parent/preflight reports, strict baseline
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, current Cloud production, and the existing public HTTPS-turn
  client/protocol APIs.
- DHXY snapshot: branch `thin-client-design`, HEAD `0114604e`; Cloud snapshot: branch
  `navigation-migration`, HEAD `3b988ca`. Both worktrees already contain extensive protected dirty/untracked
  work. This helper performed no Git mutation.
- TURN-28S1 physical true EOF now records parent source review
  `P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED / ... EXTERNAL-B-OWNER-RELEASED` at
  `2026-07-16T08:59:40.918-04:00`. Its two independent reviews remain separate pending gates; this helper does
  not substitute for them.
- Current Cloud `NpcClickService.java` is 3374 lines, SHA-256
  `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`, byte-identical to
  `migration-baseline/696a12b0/.../NpcClickService.java`. Thus the next slice can start from a clean strict-696
  business byte baseline once the parent creates and freezes a new child card.

**Non-binding decomposition result:** propose one next implementation slice named
`TURN-28S2 - active background shortcut HTTPS-turn cutover`.

This report does not itself make TURN-28S2 claimable. The parent must create/freeze the child card at physical
true EOF, recheck the initial production SHA, and then let External B append the real claim.

## 2. Why this is the smallest real next slice

The current strict-696 `NpcClickService` has four live, top-level background-shortcut action sites:

| Current source site | Active public-path meaning | Exact baseline action |
|---|---|---|
| `clickNpcSmart`, current `:624-627` | non-combat first pipeline miss before the one permitted second pipeline | `Alt+C`, then wait `700ms` |
| `tryDirectCombatTargetClick`, current `:667-670` | confirmed `FLYING` direct-combat preflight | `Alt+C`, then wait `700ms` |
| `tryDirectCombatTargetClick`, current `:682-685` | enter direct-combat targeting mode | `Alt+A`, then wait `350ms` |
| `prepareNpcPipelineNameLayerOnce`, current `:948-951` | ordinary-pipeline name-layer preparation | `Alt+4`, then wait `400ms` |

All four are reached from the frozen public methods; replacing them changes real production execution, not a
dormant helper. They form one coherent mechanics boundary: exact-HWND background keyboard plus the already
approved baseline wait in the same HTTPS action.

The needed public APIs already exist and were inspected, so this does not guess an interface:

- Cloud `TurnGameClient.bind(TurnInvocationContext)`, `latestWindowMetadata()`, and
  `execute(List<TurnStep>, boolean, Duration)` are public. One `execute` call generates one fresh UUID and sends
  one command. Current `TurnGameClient.java` SHA is
  `a8f64d8dbb5f9ed2852975d518836e25af92073f9c818d5f7e9da7cf18056cb9`.
- Existing protocol types already express `INPUT/KEY_TAP` and positive `WAIT` through `TurnStep`,
  `TurnInputAction`, and `TurnInputSpec`; no protocol/model/factory edit is needed.
- DHXY `TurnKeyMapper.findBackgroundTap(...)` accepts both `ALT_*` and `Alt+*`; current
  `BoundWindowKeyboardService.AltShortcut` explicitly lists background-supported `ALT_4`, `ALT_A`, and
  `ALT_C`. The key path is exact-HWND and has no foreground fallback.
- One action resolves one exact execution window before its ordered steps. TURN-28Q's still-reviewed frozen
  mouse action-list queue is therefore not a source-start dependency for this keyboard-only slice. TURN-28Q
  remains relevant to later mouse click integration, not to these background key taps.

Starting with either recognizer would not be a real vertical slice:

- Current `SmartClickRecognizer` SHA is
  `ffbd984a4ed5841ccba6b87bf3378a1e0cb1e7d2bea68be3eed656be7324f102`. Its callable entries still take
  `JsonNode`, decode `imagePayloadBase64`, or emit session-shaped `sessionId/windowId/taskRunId` queue messages.
  TURN-28 may not call those entries. Adding a new typed image facade without changing its real
  `NpcClickService` caller in the same slice would be dead code.
- Current `ObjectiveTextRecognizer` SHA is
  `d3dc3cc247058ae85a6258e6173f8d9b56d7be119443c90a24c4bf6f180f3fe1`. Its live pure shared surface is
  map/coordinate recognition for other callers; it owns no background-key decision. The parent already marks it
  reservation-only, so zero diff is the correct result here.
- Moving a left/right click or Ctrl probe first would couple External B's source result to the still-pending
  TURN-28Q/TURN-22 frozen mouse-queue integration. The shortcut slice uses only the already public and
  background-validated key path, so External B can advance without waiting for External A's source verdict.

## 3. Proposed TURN-28S2 exact write set

The future implementation worker may modify exactly these two paths:

1. Cloud production:
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`.
2. New append-only child card, to be created/frozen by the parent before claim:
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md`.

Initial production gate: SHA-256 must still equal
`cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` at claim. Any drift requires the parent
to re-read the new bytes and re-freeze the slice; External B must not overwrite or restore from a mirror.

Everything else is read-only in TURN-28S2, including:

- Cloud `SmartClickRecognizer.java`, `ObjectiveTextRecognizer.java`, all other production Java, and
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java` (still absent);
- `TurnGameClient`, `TurnInvocationResult`, `TaskExecutionContext`, holder/checkpoint classes, turn protocol,
  action factory, command port, Dialog/BattleRadar/Navigation/Task/caller code, POM, config, templates, and
  resources;
- all DHXY Java/tests, TURN-28/28S1/28P/28Q/22/34A cards, ACTIVE_WORK, dashboard, plans, and other reports.

No new top-level production class, model, facade, interface, or second test is allowed. A single private method
inside `NpcClickService` is permitted only if it owns the complete shortcut preflight, invocation, and terminal
validation boundary described below. It must call `TurnGameClient` directly and must not route through another
new local wrapper.

## 4. Exact API and mechanics contract

### 4.1 One action shape per shortcut

Each migrated call site submits exactly one `TurnGameClient.execute(...)` call with exactly two ordered steps:

```text
step 0 = INPUT / KEY_TAP / TurnInputSpec(key = ALT_4 | ALT_A | ALT_C)
step 1 = WAIT / 400 | 350 | 700 ms
```

The exact mapping is:

| Stage | Key | WAIT |
|---|---|---:|
| ordinary pipeline name-layer preparation | `ALT_4` | `400ms` |
| generic non-combat retry | `ALT_C` | `700ms` |
| direct-combat confirmed-FLYING dismount | `ALT_C` | `700ms` |
| direct-combat mode entry | `ALT_A` | `350ms` |

Use `fullWindowFailureEvidence=false`, return no frame, and use the existing project-standard positive turn
transport bound `Duration.ofMillis(120_000L)`. The `400/700/350ms` values are the business/mechanics waits inside
the action; the 120-second bound is only the no-retry transport wait fence and must never become another business
sleep or retry budget.

Do not emit a separate public call for the WAIT, split one shortcut across commands, add a capture, use
`queueHoldMs`, call `InputSequences`, call `InputProvider`, or add transport retry/replay.

### 4.2 Exact-context preflight before UUID creation

For every one of the four action invocations, the production path must perform the following inside the one real
shortcut execution boundary:

1. Require the current `TaskExecutionContext` from the existing `TaskExecutionContextHolder`; missing context is
   terminal before any public client call.
2. Call `TaskCheckpoint.throwIfStopRequested(context, ...)` directly. Do not add a checkpoint-only wrapper and do
   not replace it with `shouldStop()` or a new interruption helper.
3. Confirm the holder still contains the same context object, obtain `context.getTurnInvocationContext()`, and use
   the existing `context.getTurnGameClient().bind(binding)` view.
4. Read `latestWindowMetadata()` before the UUID-producing `execute` call. Require exact device/window identity,
   exact native HWND and process id against the context, and a non-null positive latest rectangle. A metadata STOP
   must be projected through the existing checkpoint; it must not become a normal key miss.
5. Confirm the same current context immediately before calling `execute`. Do not cache metadata across two
   shortcuts or across two pipeline attempts.

This is transport/window safety plumbing only. Do not add title search, first-window fallback, caller-supplied
epoch, session, owner, permit, ledger, TTL, cleanup, or a second context cache.

### 4.3 Terminal and correlation contract

After the one public call:

- Only `commandStatus=COMPLETED` plus `outcome.status=COMPLETED` may return normally to the existing caller.
- The returned outcome window must equal the preflight metadata for that exact action. Both step results must be
  present in order, indexes `0/1`, types `INPUT/WAIT`, and status `COMPLETED`; `failedStepIndex`, frame metadata,
  raw frame, match result, and local-service result must all be absent.
- `BUSY`, duplicate action id, timed-out uncertain, interrupted uncertain, `FAILED`, `STOPPED`,
  `DUPLICATE_OR_UNCERTAIN`, metadata drift, malformed step results, correlation rejection, or any runtime client
  failure is terminal. Confirmed task stop propagates as the existing stop exception; every other such condition
  fails closed as the existing task-fatal path. None may be flattened to `false`, `SKIPPED`, or a business miss.
- A terminal result issues zero later shortcut, pipeline, candidate, click, verifier, or memory command. There is no
  compensation action and no automatic resend.
- Successful key mechanics are not NPC/dialog/combat success. They only authorize the exact next baseline line:
  second pipeline after generic `Alt+C`, candidate pipeline after `Alt+A`, or name-layer observation after
  `Alt+4`.

## 5. Strict-696 behavior preservation

TURN-28S2 may move only the four physical key actions. It must preserve all existing decisions around them:

1. `clickNpcSmart` still runs the first complete pipeline first. A `COMBAT_TARGET` still gets zero generic
   `Alt+C`; every other first-pipeline business miss gets exactly one `Alt+C + 700ms`, then exactly one second full
   pipeline, never a third.
2. Ordinary pipeline execution still performs exactly one `Alt+4 + 400ms` at its existing position. Direct-combat
   mode still skips that preparation. Wubei tooltip-first, dialog gates, early/late memory, tooltip position,
   TENTATIVE cutoff, and yellow/purple/Ctrl order remain byte-semantically unchanged.
3. Direct combat still applies null/STOP first; `FLYING` gets one `Alt+C + 700ms`, `UNKNOWN` skips with zero key
   command, and grounded state continues without `Alt+C`. Exactly one `Alt+A + 350ms` precedes the candidate
   pipeline, and only BattleRadar may establish combat success.
4. Public signatures and meanings of `clickNpcSmart`, `tryDirectCombatTargetClick`,
   `confirmPendingSmartClick`, and `confirmExpectedOptionProof` remain unchanged. TURN-28S1's removal of the
   unapproved pending-evidence `sourceTask` equality gate must remain intact.
5. All mouse clicks, direct Ctrl callback mechanics, right-click exit, capture/OCR/template/formula/menu logic,
   pending-memory rules, constants, candidate budgets, verifier counts, and waits outside the four listed actions
   remain untouched in this slice.

**无已批准业务差异；按 strict `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。**

## 6. Slice acceptance and retained final gates

### 6.1 TURN-28S2 source-delivery acceptance

The parent can accept the implementation material for source review only when all of the following are true:

- Diff is limited to the exact two-path write set. `NpcClickService.java` starts from the recorded S1 SHA;
  `SmartClickRecognizer.java` and `ObjectiveTextRecognizer.java` retain the hashes recorded above.
- The four listed queued shortcut sites now use the one exact public turn boundary. Current
  `InputAction.pressAltC()` occurrences at the two listed sites, `InputAction.pressAltA()` at direct mode entry,
  and `InputAction.pressAlt4()` at name-layer preparation are gone. Other `InputSequences`/`InputProvider` uses
  remain untouched because their later slices are not authorized here.
- Each branch emits the exact key/WAIT pair and no frame, second command, local fallback, retry, or extra sleep.
- Exact-context preflight and terminal validation satisfy Section 4; a completed key action is not returned as
  business success.
- No JsonNode/Base64/session queue recognizer path is called, no typed recognizer facade is added, and no dormant
  code is introduced.
- External B appends one honest `SOURCE DELIVERED` section to the future S2 child card with initial/final SHA,
  exact line evidence, and the four branch mappings, then stops editing. External B cannot approve its work.

This is deliberately a production-only source slice, matching TURN-28S1's decomposition style. It does not create
or partially populate `NpcClickTurnContractTest.java`, because a partial harness must not be mistaken for the
parent card's complete named-test gate.

### 6.2 Evidence retained on the parent TURN-28 gate

The eventual single `NpcClickTurnContractTest` must still drive real public `NpcClickService` production with a
scripted `TurnGameClient` and prove at least these S2 cases together with the rest of TURN-28's frozen matrix:

- non-combat first-pipeline miss produces exactly one `ALT_C/700` action and one second pipeline; combat target
  produces no generic `ALT_C`; no third pipeline exists;
- direct `FLYING`, `UNKNOWN`, and grounded branches produce respectively one `ALT_C/700`, zero command, and zero
  dismount command before the common one `ALT_A/350` grounded path;
- ordinary pipelines produce one `ALT_4/400` each, while direct-combat candidate execution produces none;
- every command has a fresh UUID, exact two-step correlation, and no frame; every terminal/uncertain/drift case
  causes zero later command and no fabricated boolean/`DirectCombatClickResult` success.

TURN-28S2 source delivery or source review is not TURN-28 approval. The original named test, applicable Cloud
compile/build, TURN-28P/TURN-28Q/TURN-22 integration, two independent reviewers, and later mouse/recognizer/Ctrl
slices remain mandatory. While A/C Java writers are active, External B must not run Maven/JUnit/compile/package or
start runtime/application/server/Task/UI/capture/input. The parent must perform the mandatory Java compile gate
after the relevant writer cohort is stable and before any build is handed to the user.

## 7. Writer mutual exclusion

| Lane/card | Current reserved/active write set | Intersection with proposed TURN-28S2 |
|---|---|---:|
| External A / TURN-28Q | DHXY `InputSequences.java`, `InputActionQueue.java`, `InputActionRequest.java`, `InputActionWorker.java`, `InputActionFrozenExclusiveContractTest.java`, TURN-28Q card | `0` |
| External C / TURN-34A Repair #1 | Cloud `AutoCombatServiceTurnContractTest.java`, TURN-34A card; reviewed `AutoCombatService.java` remains read-only | `0` |
| External B / proposed TURN-28S2 | Cloud `NpcClickService.java`, new TURN-28S2 card | n/a |

TURN-28S2 also has zero intersection with `SmartClickRecognizer.java` and `ObjectiveTextRecognizer.java`. It does
not consume External A's unreviewed new source API and does not touch External C's production or test bytes.

If `NpcClickService.java` changes from the recorded S1 SHA before the future claim, or another owner is assigned the
same file, the parent must stop and re-freeze rather than allow a second writer. This helper creates no owner and
does not authorize concurrent modification.

## 8. Rejected next-slice shapes

| Rejected shape | Reason |
|---|---|
| `SmartClickRecognizer` typed facade only | No real NpcClick caller in the same slice; dead code. Existing callable routes are forbidden JsonNode/Base64/session paths. |
| `ObjectiveTextRecognizer` visibility/API edit | No shortcut dependency and a real TURN-27 shared surface; reservation-only zero diff is safer and sufficient. |
| full four-file TURN-28 reissue | External B already returned that context unit; it is not the smallest reviewable delivery and repeats the original failure mode. |
| first mouse move/click cutover | Source can describe it, but final one-frozen-queue mechanics still depends on the TURN-28Q/TURN-22 integration lane. It does not maximize immediate B independence. |
| Ctrl probe cutover | Couples ROI, pixel probe, raw PNG OCR, Ctrl release terminal handling, menu click, and recognizer facade; too large for the next slice. |
| test-only slice or helper-only adapter | No production behavior advances; violates the requirement for a real implementation slice. |

No Java, test, card, plan, ACTIVE_WORK, dashboard, Maven/runtime/input path, or Git state was modified by this
helper. The only write is this report.

PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-28 NEXT-SLICE-DECOMPOSITION-HELPER PRECHECK_COMPLETE NON-OWNER NON-REVIEWER PROPOSED-TURN-28S2-ACTIVE-BACKGROUND-SHORTCUT-CUTOVER 2026-07-16T09:06:33.9192793-04:00 -->
