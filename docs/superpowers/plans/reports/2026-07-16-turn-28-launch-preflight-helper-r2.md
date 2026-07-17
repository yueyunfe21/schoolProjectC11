# CR271 TURN-28 External B Launch Preflight Helper R2

HELPER CLAIMED

<!-- TRUE_EOF: TURN-28 LAUNCH PREFLIGHT HELPER R2 CLAIMED -->

## 1. Role and current gate

- Role: External B launch-preflight helper, not implementation owner and not reviewer.
- This report does not write `READY`, `TURN-28 CLAIMED`, `APPROVED`, or `CLOSED`.
- The authoritative plan now records TURN-28P as
  `SOURCE+TEST SOURCE REVIEW PASSED / REVIEW+BUILD PENDING`; the fixed TURN-28P card true EOF records
  parent source review `P0/P1/P2=0/0/0` at `2026-07-16T04:48:07.493-04:00`.
- Therefore TURN-28's mechanics source dependency is open. TURN-28 still requires a parent-frozen fixed card and
  an External B true-EOF claim before any production/test edit begins. TURN-28P is not being promoted here to
  full-card approval.
- Current Java writers remain active elsewhere. This helper did not run Maven/JUnit/compile, did not start a
  runtime/server/Task/UI/input path, and did not perform Git mutation.

## 2. Authority and byte evidence

The launch card must cite these authorities in this order:

1. `D:/mavenProject/DHXY/AGENTS.md` and current `docs/ACTIVE_WORK.md`.
2. `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`, especially TURN-23,
   TURN-24/24A, TURN-26, TURN-27, TURN-28, TURN-28P, the exact write-set table, and the named-test table.
3. `docs/业务逻辑.md` baseline gate and NPC FIFO section.
4. Git `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` as the default NpcClick business baseline.
5. The current Cloud callers and current three reserved production files, solely for compatibility and collision
   evidence; current local behavior does not override the baseline by itself.

Read-only byte anchors:

| Evidence | Current value |
|---|---|
| Authoritative 696 NpcClick git blob | `74d9b26b76b84052718d5679529f7ffeb46e3273` |
| Cloud baseline mirror | `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java`; 3374 lines; same git blob |
| Cloud preserved historical shard | `migration-preserved/pre-696a12b0-whole-service-cutover-20260714T1129/src/main/java/com/bot/dhxy/service/NpcClickService.java`; blob `7574e3c35cb6ba789d0c9f4dd99abdaac54597b6` |
| Cloud current NpcClick | 3406 lines; blob `4d5339cc7b4c2836cc5461e911056d75938318b6`; SHA-256 `f4e3842cdb5f59580d8f25f0191ade4847bfe8ca6c7939ac73a70bd561bfd870` |
| DHXY current NpcClick | 1385 lines; blob `c853ced7c3ac1a74f12b668380afa72952a7f619`; not the 696 authority |

The Cloud current NpcClick differs from the 696 mirror only by a normalized `sourceTask` field/gate on pending
expected-option proof. That is a real semantic difference, but no approval for it is present in TURN-28. It is
listed in the conflict section below and must not be silently retained merely as a safety improvement.

## 3. Exact External B write set

### 3.1 Production: Cloud only, exactly three reserved paths

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java`

`ObjectiveTextRecognizer.java` is reservation-only unless the implementation genuinely reuses its existing pure
map/coordinate calculation. A zero-diff delivery for that path is valid. Do not manufacture a change merely to
touch all three reservations.

### 3.2 Named test: Cloud only, exactly one new path

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java`

This path follows every existing Cloud `service/*TurnContractTest` package and the authoritative named-test table's
`service/NpcClickTurnContractTest` convention. The older readiness report proposed
`src/test/java/com/bot/dhxy/service/...`; the parent launch card should supersede that helper-only proposal with the
path above so External B has one unambiguous test owner.

### 3.3 No implicit fourth production file

- No new decision/model top-level production Java file. Required implementation values stay as concise nested
  immutable records/enums in one of the three reserved files.
- No edits to `NpcClickRequest`, `DirectCombatClickResult`, `NpcSmartClickOutcome`, Task classes, Navigation,
  Dialog, BattleRadar, LocalOcrClient, protocol, turn client/factory/result, host/config, resources, `pom.xml`, or
  either repository's tests outside the one named test.
- No edit to a fixed card, original plan, CR table/dashboard, `ACTIVE_WORK`, or another helper/reviewer report by
  External B unless the parent card explicitly grants its own append-only delivery report.

## 4. Caller-facing public API freeze

The current Cloud caller graph compiles against these four methods. Preserve their signatures and externally
observable return meaning:

```java
public boolean clickNpcSmart(NpcClickRequest request)

public DirectCombatClickResult tryDirectCombatTargetClick(NpcClickRequest request)

public void confirmPendingSmartClick(
        String mapName,
        String npcName,
        int mapX,
        int mapY,
        String verificationStrength,
        String reason)

@Override
public void confirmExpectedOptionProof(
        String sourceTask,
        String actionKey,
        String matchedText,
        String proofToken,
        String verificationStrength,
        String reason)
```

Additional compatibility rules:

- `NpcClickService` remains a Spring bean and remains the implementation of
  `SmartClickEvidenceConfirmationService`.
- `clickNpcSmart` returns `true` only after the applicable verifier proves success. A mechanically completed click,
  template hit, OCR hit, `PIXELS_CHANGED`, or bare `Alt+A` is not success.
- `DirectCombatClickResult` retains the existing three semantic outcomes: combat entered, skipped without position
  refresh, and failed-after-entering-mode with `positionRefreshRequired=true`.
- Do not change request/model fields or callers in TURN-28. Do not add a second same-purpose public wrapper.
- The current source callers in `NavigationService`, `XiuluoTaskV2`, `WubeiTask`, `FiveRingTaskV2`, and
  `DialogService.finishRequest` remain source-compatible and read-only. Current server reachability is still zero;
  Cloud host/Task activation belongs to later cards and must not enter this write set.

Recognizer compatibility:

- Preserve `ObjectiveTextRecognizer.recognize(JsonNode)`, `coordinatePlausible(String,int,int)`, and
  `mapTransform(String)` visibility/signatures because `DecisionEngine`, `QuestDetailTextRecognizer`, and
  TURN-27's `MiniMapPointResolver` consume them.
- Preserve the legacy `SmartClickRecognizer.recognize(...)`, `recognizeQueueMessages(...)`, and
  `produceQueueMessages(...)` entry points sufficiently for the read-only `DecisionEngine` and
  `NpcClickSmartQueueStore` to compile. The new NpcClick path must never call them.
- Cross-package reuse from `com.bot.dhxy.service.NpcClickService` may expose one typed, pure
  `BufferedImage`/frame-metadata facade from `SmartClickRecognizer`, with nested immutable request/result types.
  It must not expose or accept `JsonNode`, Base64, `sessionId`, queue-store handles, outcome endpoints, owner tokens,
  ledger keys, or TTL. It is an implementation API, not a new Task/caller contract.

## 5. Turn invocation and terminal contract

For every command boundary, NpcClick must follow the already-approved TURN-23/24/25/26/28P discipline:

1. Run `TaskCheckpoint` directly before selecting/submitting the action; no local checkpoint wrapper.
2. Resolve the exact current `TurnInvocationContext`, bind one `TurnGameClient` view, and read latest metadata before
   UUID creation.
3. Reject stop before UUID/command. Validate exact device/window plus immutable HWND/process identity; use the latest
   `windowRect` for screen-absolute ROI/click conversion.
4. One public `TurnGameClient.capture/execute` call creates one fresh UUID and one command. There is no transport
   retry and no action replay.
5. Treat command non-completion, `FAILED`, `STOPPED`, `DUPLICATE_OR_UNCERTAIN`, release uncertainty, and all
   action/window/frame/step correlation failures as fatal/stop/uncertain control flow. They are not candidate misses.
6. Only a strictly correlated `COMPLETED` result may become a visual miss or verifier false. Never catch a fatal
   result and continue to the next candidate, write learned success, or return a fabricated business boolean.
7. Checkpoint again after every closed action/observation and before choosing the next business action.

Each action may return at most one raw PNG. The strict rule is one frame per action, not one frame for the entire
multi-candidate pipeline. Do not collapse all 696 observations into a stale shared base frame, and do not add a
second requested frame to one action.

## 6. Exact ordinary FIFO and branch order

### 6.1 `clickNpcSmart`

1. Run one expected-dialog pipeline.
2. Verified success returns immediately.
3. Confirmed stop/interruption returns or propagates immediately; it never enters another fallback.
4. `targetRole=COMBAT_TARGET` skips the generic `Alt+C` retry.
5. Every other target gets exactly one closed `Alt+C -> WAIT 700ms` action, then exactly one second full pipeline.
6. No third pipeline, timeout-driven retry, automatic reissue, or local full fallback.

### 6.2 One pipeline

1. For non-direct, non-Wubei, non-combat requests: run the first pre-click dialog gate, then early learned memory.
2. Ordinary mode performs exactly one closed `Alt+4 -> WAIT 400ms` name-layer preparation. Direct-combat mode does
   not repeat `Alt+4`.
3. Wubei tries tooltip before the main dialog gate.
4. At the main dialog gate, `STORY` is handled once by the approved Dialog path and then re-detected. Any remaining
   dialog blocks the pipeline. Existing `OPTION` blocks without generic cleanup.
5. Requests that did not use early memory try memory here.
6. Non-Wubei requests try tooltip here.
7. Non-direct mode performs the post-tooltip dialog detection once.
8. `targetEvidence=TENTATIVE` stops here. It must not run yellow, formula, or Ctrl.
9. Confirmed requests continue in this exact order: yellow target, purple player-anchor formula, final Ctrl menu.
10. Every verified candidate short-circuits all remaining candidates.

The external FIFO labels remain
`MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`, but they are a semantic ordering,
not permission to erase early-memory, Wubei tooltip-first, dialog gates, `TENTATIVE`, or the formula-immediate Ctrl
sub-branch. Do not materialize these labels as a runtime session/queue.

## 7. Candidate, OCR, template, and click baseline

### 7.1 Atomic left-click action

Every allowed click attempt is one closed turn action and one global mouse queue submission:

```text
MOVE_MOUSE(screenAbsoluteX, screenAbsoluteY)
WAIT(150ms)
CLICK_LEFT(x, y, clickDelayMs=150, queueHoldMs=firstWaitMs)
```

Do not append `firstWaitMs` as a separate outer sleep that releases the queue. A baseline business retry is a new
action/new UUID with the same `MOVE -> WAIT150 -> CLICK_LEFT(delay=150, hold=1000)` shape, followed by exactly one
new verifier. `maxRetries=N` means at most `N+1` clicks and `N+1` verifier calls.

### 7.2 Per-strategy facts

| Strategy | Exact baseline | Click and verify budget |
|---|---|---|
| Learned memory | Conservative remembered window-relative point for exact map/name/target; no known coordinate means skip; miss adds that point as a SMALL_RING Ctrl origin | 1 click, hold 1200ms, 1 verifier, zero retry |
| Tooltip | Request template or `images/template/npc/npc_task_tooltip.png`; recommended region order; threshold `0.82`; dedup distance `36px`; preserve matcher hit order inside each region | Each dedup hit: 1 click, hold 1200ms, 1 verifier, zero retry |
| Yellow name | Recommended region order; only target-not-found may expand ROI; a concrete click that fails verification does not expand; `降魔侍卫` requires longest common substring at least 3; click provider word center with final Y offset `-50` | Concrete hit: first hold 800ms plus exactly one retry hold 1000ms; at most 2 clicks/2 verifies |
| Purple formula | Purple-name OCR first, then purple component/blob fallback; constants `UX=20, UY=0, VX=0, VY=-20`; predicted target is player anchor plus logical map delta and tune, with final Y offset `-50` | 1 click, hold 1500ms, 1 verifier, zero retry; miss keeps the extra baseline 1500ms wait |
| Ctrl menu word | Process the successful probe's sole after PNG with yellow wash and canonical `LocalOcrClient.readWords`; keep provider order; accept first short-name match or `(?i).*(NPC|IPC|PC|NP).*`; click the returned word point | First hold 800ms plus exactly one retry hold 1000ms; at most 2 clicks/2 verifies |

Yellow and purple processing must reuse the existing `ImageAlgorithms` behavior without editing that file. Purple
uses `WASH_PURPLE`; yellow target/menu use the existing yellow operations appropriate to their 696 path. Do not
copy or fork `LocalOcrClient`, change its endpoint/timeout/health/failure semantics, sort provider words into a new
priority, or add another OCR read for confidence.

Formula miss behavior is deliberately non-obvious: after the direct formula click fails, immediately run a
SMALL_RING Ctrl probe around the formula point. If that fails, the final Ctrl strategy may probe the same origin
again. Preserve both stages; cross-stage dedup would be a behavioral change.

## 8. TURN-28P Ctrl probe integration

Profiles and order are exact:

- `DIRECT`: `(0,0)`.
- `SMALL_RING`: `(0,0),(8,-8),(8,0),(0,-8),(-8,0),(0,8),(-8,-8),(-8,8),(8,8)`.
- `FULL_RING`: `(0,0),(16,-16),(16,0),(0,-16),(8,-8),(8,0),(0,-8),(16,16),(0,16),(-16,0),(-16,-16),(-8,-8),(-8,0),(-16,16),(-8,8),(8,8),(0,8)`.

Normalize points in their existing order, clamp to the current 1024x768 window, and retain the 3px same-origin
dedup inside one origin list. Production does not add window-center fallback. Non-combat requests require the
formula reference and keep only origins within a 15px radius; combat targets do not apply that filter.

Each probe is exactly one `TurnGameClient.execute(...)` call containing one `CAPTURE` step whose
`TurnCaptureSpec` is:

- screen-absolute ROI = probe `x +/- 150`, `y +/- 120`, clamped to latest exact window;
- `resultMode=UPLOAD_IMAGE`;
- `clearPointerIfOverRegion=null`;
- `pixelChangeProbe=(targetX,targetY,80,280,100,0.05)`.

TURN-28P guarantees, inside one exact-HWND exclusive callback:

```text
before capture in memory
Ctrl DOWN
wait 80ms
MOVE_MOUSE unscaled
wait 280ms
after capture
RGB pixel comparison, fixed color tolerance 15 and ratio threshold 0.05
finally Ctrl UP
wait 100ms
```

`before` never uploads or becomes an artifact. A correlated completed probe returns only
`PIXELS_CHANGED` or `PIXELS_UNCHANGED` plus the sole after raw PNG. `PIXELS_UNCHANGED` advances to the next probe
without OCR. `PIXELS_CHANGED` allows Cloud OCR on that returned PNG. `CTRL_RELEASE_FAILED`, `PIXEL_PROBE_FAILED`,
STOP, uncertainty, or correlation failure aborts the business flow; none may be projected as unchanged.

The menu click occurs in the next explicit click action after the probe has released Ctrl. It remains atomic:
`MOVE -> WAIT100 -> CLICK_LEFT(delay=150, queueHold=800)`, then one verifier; the sole baseline retry uses hold
1000ms. The old `images/template/npc/npc_tag.png` remains read-only reference and is not an active decision shortcut.

## 9. Verifier baseline and exception priority

### 9.1 Dialog verifier

- Each candidate verify calls `DialogService.handleDialog(DialogHandleRequest.verifyExpectedOptionDialog(...))`
  exactly once.
- Only `OPTION_VISIBLE` and `GREEN_TEMPLATE_VISIBLE` are success.
- It must use the current TURN-26 typed Cloud dialog/OCR/template path. Do not capture again in NpcClick, call a
  legacy dialog macro, perform local OCR, or click the post-NPC option.
- `FAILED`, `INTERRUPTED`, fatal/uncertain/correlation, and confirmed STOP preserve their stronger meaning; they
  must not become verifier `false` followed by the next candidate.

### 9.2 Combat verifier

- For each click candidate, call `BattleRadarService.checkAndSyncCombatState()` at most four times.
- On every known `false`, wait 350ms, including after the fourth false. Thus the exact miss budget is four calls
  and four waits.
- A known `true` succeeds immediately. TURN-24A stop/uncertain/fatal behavior propagates and does not become false.

### 9.3 Priority

The priority is `confirmed STOP / interruption` and fatal/correlation/uncertain control flow before ordinary
business miss. A stronger terminal cannot be overwritten by cleanup, evidence removal, a false return, or a later
candidate. Successful pending evidence is written only after a real verifier success.

## 10. Direct-combat baseline branch

The default launch branch under the authoritative plan's strict-696 rule is:

1. Null/stop gate.
2. Detect flying state. `FLYING` gets one closed `Alt+C -> WAIT700`; `UNKNOWN` skips direct combat; known grounded
   continues.
3. One closed `Alt+A -> WAIT350` action.
4. Run the same candidate pipeline with combat verifier, without repeated `Alt+4` or pre-click dialog gate.
5. Verified combat returns `combatEntered`; stop does not right-click to exit.
6. Non-stop failure exits direct-combat mode at most three times. Prefer the purple/player anchor; otherwise use
   `(windowLeft+512, windowTop+424)`.
7. Each exit attempt is one atomic
   `MOVE -> WAIT120 -> CLICK_RIGHT(clickDelayMs=120, queueHoldMs=600)` action, followed by the existing mode probe.
   If mode remains, wait 300ms before the next attempt.
8. Three unconfirmed exits throw. Do not continue cleanup, another target click, or caller retry.

No direct-combat candidate may treat bare `Alt+A`, scene change, template hit, or click completion as combat. Only
the four-probe BattleRadar verifier can close it as entered combat.

The CR255/CR267 conflict with this strict branch is material and is listed in section 13. External B must follow the
parent card's explicit selected branch, not combine the two.

## 11. Pending evidence and memory

- Preserve the 696 pending-evidence boundary keyed to the current exact window and proof token.
- Explicit confirmation matches map/name/mapX/mapY. Expected-option confirmation matches proof token and expected
  option evidence before committing learned success.
- Do not add expiry, TTL, cleanup scheduler, session owner, durable ledger, compaction, or a second memory store.
- A failed candidate, STOP, uncertain/fatal outcome, out-of-window point, mismatched proof, or unverified FIFO END
  cannot write success memory.
- The current extra normalized `sourceTask` equality gate is not part of 696. Under the default strict branch it is
  not carried forward. Retaining it requires an explicit approved-difference sentence in the parent card and a
  named-test case; a helper recommendation is not approval.

## 12. Reference/shadow and legacy preservation

These paths are read-only and must remain present:

- DHXY git object `696a12b0:src/main/java/com/bot/dhxy/service/NpcClickService.java`.
- Cloud `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java`.
- Cloud `migration-preserved/pre-696a12b0-whole-service-cutover-20260714T1129/src/main/java/com/bot/dhxy/service/NpcClickService.java`.
- DHXY current `src/main/java/com/bot/dhxy/service/NpcClickService.java` and `src/main/java/com/bot/dhxy/service/npc/**`.
- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/ImageAlgorithms.java`.
- Existing NPC/status/dialog template assets, including `images/template/npc/npc_tag.png` as inactive reference.

Legacy Cloud `DecisionEngine`, `NpcClickSmartQueueStore`, old outcome endpoint/routes, fact/macro ports, and old
JsonNode recognizer entry points remain physically present and compile-compatible because their deletion is a later
zero-reference card. The new production NpcClick path must have zero calls to them. Do not interpret
"reference/shadow retained" as permission for a production full fallback.

## 13. Conflicts the parent card must freeze explicitly

### 13.1 Old FIFO document mechanics versus CR271 turn architecture

`docs/业务逻辑.md` still describes `NPC_CLICK_START`, a local FIFO consumer, one session base screenshot,
poll/outcome, and local Ctrl OCR. The newer unique authoritative TURN plan and the user's current instruction forbid
new session/ledger/TTL and put OCR/template/verification decisions in Cloud. Resolution for this launch:

- Preserve the FIFO business order and per-candidate baseline only.
- Implement one synchronous Cloud method issuing sequential closed turn actions.
- Do not create a runtime queue/session/poller/outcome reporter or reuse the old one.
- One raw frame per action is authoritative; "one session base screenshot" is not.

This is an ownership/transport migration, not permission to change candidate conditions, order, retries, or verifier
counts.

### 13.2 CR255/CR267 versus strict 696

CR255 and CR267 are later, explicitly approved cards. They add a fresh `STORY_DIALOG_VISIBLE` restart path and a
structured direct-combat authorization/FIFO-terminal gate, and CR267 records that the earlier flying/Alt+U preflight
was removed. The current TURN-28 authoritative plan and `ACTIVE_WORK` instead say strict 696 with no approved
TURN-28 difference. The three-file TURN-28 set also excludes the event bus, Tasks, request/result models, and callers
needed to reproduce CR255/CR267 end to end.

Default helper recommendation for a sendable three-file card: freeze strict 696 inside TURN-28, keep synchronous
STORY gates and the section-10 direct-combat path, and state that CR255/CR267 caller-era behavior is not silently
mixed into this card. If the parent instead decides those approved cards supersede 696 here, it must first write all
of the following into the fixed TURN-28 card:

1. The intentional approved differences from 696.
2. Whether `clickNpcSmartWithOutcome` becomes an additional public API and how read-only current callers obtain the
   genuine END-without-verification fact without new session/ledger state.
3. Exact allowlist for `WUBEI_PROBE_TARGET_READY` and `LEGACY_COMBAT_TARGET`, rejection of tracker shortcut/null/
   unknown scenarios, and the absence of the flying/Alt+U gate.
4. How the CR255 story event is supplied without adding a fourth production file or restoring old local ownership.

External B must not choose a hybrid. In particular, it must not use 696 candidate order while silently adding
CR255's three restarts or CR267's authorization boolean.

### 13.3 Current `sourceTask` safety delta

The current Cloud-only normalized `sourceTask` proof gate is a semantic difference from the authority blob. Default
strict resolution is to omit it. Retention requires explicit parent approval; "safer" is not approval.

### 13.4 Named-test package

Use the exact test path in section 3.2. Do not create both the older readiness path and the established Cloud
`com/yueyunfe/dhxy/cloudbrain/service` path.

### 13.5 Typed recognizer versus legacy recognizer

The new service needs a typed cross-package image API, while the legacy DecisionEngine still compiles against
JsonNode/session methods. Preserve legacy signatures as dormant compatibility, add only the smallest typed facade
inside the reserved file, and prove the new service never invokes the old path. Do not rewrite DecisionEngine in
TURN-28.

### 13.6 TURN-27 shared file boundary

`ObjectiveTextRecognizer.mapTransform` and `coordinatePlausible` are live TURN-27 dependencies. Prefer zero diff.
If TURN-28 changes visibility for pure reuse, preserve behavior and signatures exactly; do not change map loading,
plausibility, OCR thresholds, or Navigation decisions. TURN-27 remains blocked on the final TURN-28 API, and its
`navigateToNPC` must remain navigation-only rather than swallowing Task's later NPC-click phase.

## 14. Prohibited implementation shortcuts

- No edits outside the three production and one named-test paths.
- No DHXY production/test edit and no protocol/executor change in TURN-28.
- No Maven/runtime/server/UI/capture/input/Git mutation before the parent card permits its own gates.
- No `sessionId`, queue store, poll loop, outcome endpoint, owner/permit, ledger, TTL, compaction, durable workflow,
  candidate timeout, candidate budget, timer thread, or transport retry.
- No Base64 image payload, resized screenshot, second frame in one action, temp-file OCR, or second OCR client.
- No local/DHXY OCR, candidate ranking, story decision, verifier interpretation, or full Npc fallback.
- No extra Alt+C, Alt+A, Alt+4, Ctrl probe, click, verifier read, capture, retry, cleanup, or region expansion.
- No broad catch that turns fatal/uncertain/correlation/STOP into `false`, `UNCHANGED`, END, or success.
- No foreground keyboard fallback. Alt+A/Alt+C use TURN-28P exact-HWND support; only mouse motion/click uses the
  global foreground queue mechanics already encoded by the turn executor.
- No active `npc_tag.png` template shortcut, no window-center Ctrl fallback, and no dedup of the formula-immediate
  Ctrl stage against the final Ctrl stage.
- No deletion or rewriting of reference/shadow/legacy files and no source-string guard as a substitute for behavior
  assertions.
- No Task phase, navigation, dialog-option, recovery, park/yield, retry, or activation change.

## 15. Exact named-test source matrix

The one named class should use fake/scripted `TurnGameClient` command results and in-memory PNGs only. It must drive
the real `NpcClickService` production path and use counters/typed outcomes rather than source-text assertions.

| Test case | Required assertion |
|---|---|
| `ordinaryPipelineKeepsConditionalFifoOrder` | Early memory, Wubei tooltip-first, both dialog gates, normal order, `TENTATIVE` cutoff, success short-circuit |
| `ordinaryFailureGetsExactlyOneAltCRetryExceptCombatTarget` | One Alt+C/700 and second pipeline only for non-combat; combat target gets neither |
| `learnedTooltipYellowFormulaBudgetsAreExact` | Per-strategy thresholds, order, click/verify counts, waits, yellow expansion rule, formula extra wait |
| `formulaImmediateCtrlAndFinalCtrlBothRemain` | Exact immediate SMALL_RING plus later final Ctrl, with no cross-stage dedup |
| `ctrlProfilesAndNonCombatFilterAreExact` | Exact 1/9/17 offsets, 3px intra-list dedup, 15px noncombat filter, no production center fallback |
| `ctrlProbeUsesOneCaptureActionAndOneAfterPng` | One UUID/command/CAPTURE; ROI; 80/280/100; 0.05; changed/unchanged; sole raw after frame; no before upload |
| `ctrlReleaseFailureStopAndUncertaintyNeverBecomeMiss` | Release/mechanics/STOP/uncertain/correlation abort and issue zero later candidate/click/memory |
| `menuOcrKeepsProviderOrderAndInactiveTagTemplate` | Canonical LocalOcrClient provider order; short-name/regex; old tag template call count zero; exact click retry budget |
| `mouseClickTimingStaysOneQueueSubmission` | MOVE/WAIT150/click delay150/queue hold per strategy, one action/UUID/queue submission; retry is a new explicit action |
| `dialogVerifierReadsOnceAndAcceptsOnlyTwoStatuses` | One TURN-26 dialog call per verify; only OPTION_VISIBLE/GREEN_TEMPLATE_VISIBLE true; strong terminal propagates |
| `combatVerifierKeepsFourReadsAndFourMissWaits` | Up to four BattleRadar calls and four 350ms waits; success short-circuit; strong terminal propagates |
| `directCombatKeepsSelectedParentBranch` | The fixed card's strict-696 or explicitly approved CR267 branch, never a hybrid; right-click exit budget exact |
| `metadataAndTerminalFencesStopBeforeNextAction` | Pre-UUID STOP; exact device/window/HWND/process; latest rect; all fatal/correlation negatives issue zero later action |
| `pendingEvidenceCommitsOnlyAfterRealProof` | Exact window/token/map/name/coords/option; sourceTask behavior matches the parent-selected conflict resolution; no TTL |
| `typedPathNeverCallsLegacySessionOrShadow` | DecisionEngine/queue store/outcome/macro/full-fallback counters remain zero; reference files are not rewritten/deleted |
| `objectiveRecognizerCompatibilityIsPreserved` | Existing mapTransform/coordinatePlausible/recognize consumers remain source-compatible; zero Navigation semantic drift |

No test or build was run by this helper. The implementation card should say who owns the named-test and Cloud
compile/package gate once the Java-writer cohort is stable; source delivery must not be reported as runtime proof.

## 16. Parent launch checklist

Before sending TURN-28 to External B, the parent should place these facts in the fixed card true EOF:

1. TURN-28P parent source gate true EOF is still present and no overlapping writer owns the three production paths.
2. The exact four-file source/test set from section 3.
3. The four caller-facing methods from section 4.
4. Strict 696 as default plus an explicit selection for section 13.2 and section 13.3.
5. TURN-28P click/probe mechanics and terminal priority from sections 5, 7, and 8.
6. Reference/shadow preservation and zero legacy active calls.
7. External B is implementation Worker, not reviewer; only its own fixed-card true EOF `CLAIMED` establishes
   ownership.

This helper's conclusion is `PRECHECK_COMPLETE`: the source mechanics dependency is open and the exact launch
packet is available, but this report does not itself issue READY, claim TURN-28, approve code, or close any card.

PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-28 LAUNCH PREFLIGHT HELPER R2 PRECHECK_COMPLETE 2026-07-16T05:04:55.868-04:00 -->
