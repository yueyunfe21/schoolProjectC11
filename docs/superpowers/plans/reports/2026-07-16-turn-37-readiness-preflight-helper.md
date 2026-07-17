# TURN-37 Readiness Preflight Helper

## REPLACEMENT CLAIMED

- claimedAt: `2026-07-16T03:26:34-04:00`
- agent: `Codex / TURN-37 replacement readiness helper (current task)`
- role: non-binding readiness helper; not reviewer; no `APPROVED` / `BLOCKED` authority
- reason: previous TURN-37 helper session returned `not_found`; this pass does not inherit or assume any prior conclusion
- card: `TURN-37`
- scope: Xiuluo whole-task HTTPS turn cutover readiness only
- uniqueWriteSet: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-37-readiness-preflight-helper.md`
- sourceMutation: none permitted
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`

Precheck is in progress. Evidence and a dispatch brief will be appended only after the required documents, repository state, card reports, baseline source, and current source have been independently read.

## IDENTITY CORRECTION

- correctedAt: `2026-07-16T03:31:31-04:00`
- platformAgentId: `019f69d1-6733-79f0-b7b6-ce99b8830a18`
- nickname: `Feynman`
- correction: the generic identity text in the initial replacement claim is superseded by this platform identity; role, scope, and unique write set are unchanged

## 1. Precheck authority and evidence boundary

- observedAt: `2026-07-16T03:42:53-04:00`
- helperIdentity: `Feynman / 019f69d1-6733-79f0-b7b6-ce99b8830a18`
- roleBoundary: evidence, risk, dependency, write-set, acceptance-matrix, and dispatch-brief preparation only; this report is not a review verdict or card-completion decision
- businessBaseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- migrationRule: Cloud owns phase decisions, OCR, image interpretation, retry/fallback choice, and business state; DHXY owns only the explicit mechanics requested by one ordered HTTPS JSON action
- noApprovedBusinessDifference: `无已批准业务差异；按基线等价迁移`

This pass independently read the required project instructions and context, the CR271 active-work header, plan Sections 14-19, the HTTPS turn protocol specification, the Xiuluo portions of `docs/业务逻辑.md`, both repository statuses, related predecessor reports, the current Cloud Xiuluo source, and the exact baseline source. No conclusion from the lost helper session was inherited.

## 2. Repository and source snapshot

### 2.1 Dirty-worktree protection

- DHXY branch observed: `thin-client-design`; the worktree contains extensive tracked and untracked changes.
- Cloud branch observed: `navigation-migration`; the worktree contains extensive tracked and untracked changes.
- This helper performed no checkout, reset, restore, clean, stage, commit, merge, rebase, cherry-pick, deletion, or other Git mutation.
- This report is the only file written by this helper.

### 2.2 Exact source comparison

| Artifact | Path | SHA-256 |
|---|---|---|
| Current Cloud Xiuluo | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java` | `46F9665999F644BE63B7F27E772429E68190322FBDE487641CBEFF0F747F519A` |
| Exact 696 baseline | `D:\mavenProject\DHXY-local-baseline\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java` | `9358133E62C06AA175D1588CE2D7E89EEF01FD09268AB25DD0DA9AFFD5E5233C` |

`git diff --no-index --stat` reports one file with `58 insertions / 38 deletions`. The observed current delta is the already-reviewed TURN-30 tracker caller conversion: exact asynchronous task-context binding, typed `TaskTrackerPanelService.readXiuluoTrackerPanel(...)`, and terminal propagation. The phase skeleton and 696 business ordering remain present. TURN-37 must preserve this accepted typed caller work and must not restore the old snapshot parser.

## 3. Dependency gate

The authoritative plan gives TURN-37 the precise start dependency set:

`S=13C+14+15+17+21+22+23+26+27+28+30+34A+34B`

The broad card text also describes the predecessor range as `TURN-14..17` and `TURN-24..34`. The precise status-row dependency set above controls dispatch.

| Dependency | Evidence observed | TURN-37 consequence |
|---|---|---|
| TURN-13C | Parent source/test-source review passed; build cohort still pending/shared-blocked | Public exact task context may be consumed read-only; final build gate remains outstanding |
| TURN-14 | Parent source/test-source review passed; build pending | Bag/return-item/incense must use this typed `LOCAL_SERVICE` boundary, never direct `BagService` |
| TURN-15 | Parent source/test-source review passed; build pending | UI cleanup must use the typed four-operation boundary, never direct `UICleanerService` |
| TURN-17 | Parent source/test-source review passed; build pending | Quest activation/detail capture is one typed command and raw `QUEST_DETAIL` PNG; real caller is intentionally left for TURN-37 |
| TURN-21 | Parent source/test-source review passed; build pending | Common-box 30-second pending/priority/current-identity behavior is available as a typed Cloud service |
| TURN-22 | Parent review recorded one P1 and released its owner | Exact 696 click timing (`150 ms` click delay plus `500 ms` queue hold) waits on shared TURN-28P mechanics before the original Cloud assembly/test can be repaired |
| TURN-23 | Parent source/test-source review passed; build pending | Player-state/first-aid/incense behavior must be called through the reviewed typed service |
| TURN-26 | Parent source/test-source review passed; build pending | Dialog OCR/options/white-story fallback must reuse one original frame and Cloud interpretation |
| TURN-27 | Readiness precheck complete only | It cannot be implemented until TURN-28 final public APIs and parent scope freeze exist; TURN-37 must not guess Navigation/NPC interfaces |
| TURN-28 | Not yet final | It is gated by TURN-28P; final typed NPC click and raw-image objective-recognizer surfaces are not frozen |
| TURN-30 | Parent source/test-source review passed; build pending | Preserve exact tracker phase/park/terminal conversion already present in `XiuluoTaskV2` |
| TURN-34A | Readiness evidence exists; implementation not yet claimable | Final AutoCombat public surface and dependency identities are not frozen |
| TURN-34B | Readiness evidence exists; implementation not yet claimable | Final TaskMaintenance public surface waits on TURN-22 and TURN-33 |
| TURN-28P | Replacement implementation currently claimed by `Locke / 019f69ce-9359-71a1-8402-cb7ee7d34404` | Supplies generic queue-owned click timing and single-CAPTURE pixel-change mechanics needed by TURN-22/28 |
| TURN-33 | Replacement implementation currently claimed by `Faraday / 019f69ce-d84c-7a11-a832-3ce77f8f739a` | Indirectly gates final TURN-34A/34B contracts |

Readiness result: the evidence package can be frozen now, but the implementation card is not presently ready to claim because the precise start dependency set is not source-stable. This is a dependency fact, not a helper approval or rejection decision.

## 4. Current runnable caller and target call chain

### 4.1 Current runnable Cloud task entry

- `XiuluoTaskV2.execute()` delegates to `execute(TaskExecutionContext)` at current source lines `303-318`.
- `execute(TaskExecutionContext)` initializes the real task run and enters the existing Xiuluo round loop.
- `runPhase(...)` at lines `1061-1083` dispatches all real phases, not a private count-only helper.
- The phase list remains:
  `PREPARE_ROUND -> ACCEPT_TASK_NAVIGATE_TO_NPC -> ACCEPT_TASK_CLICK_NPC -> ACCEPT_TASK_DIALOG -> READ_OBJECTIVE -> AFTER_ACCEPT_MAINTENANCE_CHECK -> BEFORE_ROUTE_MAINTENANCE_CHECK -> TRY_TRACKER_SHORTCUT -> WAIT_TRACKER_SHORTCUT_PATHING -> NAVIGATE_TO_TARGET -> CLICK_TARGET_NPC -> CONFIRM_ENTER_BATTLE -> WAIT_COMBAT -> RETURN_HOME -> NAVIGATE_BACK_TO_START -> WAIT_TEAM_READY / WAIT_TEAM_RETURN -> ROUND_DONE / FAILED / STOPPED`.

### 4.2 Required final chain

`real task runner -> XiuluoTaskV2.execute(context) -> existing phase handler -> reviewed typed Cloud Service/client -> TurnGameClient -> one HTTPS JSON action -> exact-bound DHXY turn handler/executor -> explicit mechanics or one of four closed LOCAL_SERVICE operations -> typed ActionOutcome plus at most one raw PNG -> Cloud phase decision`

The task must never call a DHXY service instance, input queue, tracker, temp-path owner, local OCR engine, or filesystem image path directly. It also must not insert a facade that merely forwards to another facade.

## 5. Current direct-local residue inventory

The current Cloud task still contains pre-cutover dependencies. These are implementation targets, not evidence that the business rule itself should change.

| Current evidence | Current behavior | Required reviewed replacement |
|---|---|---|
| imports/fields `GameClientTracker` at lines `5,252` and `WindowScopedTempPath` at `79,277` | Local window capture/path ownership | Exact metadata and raw capture through `TurnGameClient`; no shared/local temp-path authority in Task |
| `InputSequences` at `7,279`; `moveAndClickLeft` at `1655,2221`; `pressAltC` at `3359` | Direct input from Cloud task | TURN-27/28 typed Cloud orchestration plus TURN-28P queue-owned mechanics; each Cloud-requested action has its own UUID |
| direct `BagService` at `41,263`; item use at `2840,2889` | Permanent-local service invoked as an in-process object | TURN-14 typed Bag/return-item/incense `LOCAL_SERVICE` client |
| direct `QuestManagerService` at `48,259`; detail capture at `3393` | Local task-panel capture and image-path return | TURN-17 `CloudQuestLocalServiceClient`, one command, one raw `QUEST_DETAIL` PNG |
| direct `UICleanerService` at `53,269` and active cleanup calls | Local cleanup service invoked in-process | TURN-15 typed UI cleaner operations; preserve the exact 696 cleanup call sites/order |
| `TaskTrackerPanelService.readXiuluoTrackerPanel` at `999,1642,3183` | TURN-30 typed tracker conversion | Preserve the reviewed TURN-30 API, exact context, terminal propagation, phase and park semantics |
| `captureAcceptWindowSnapshot` called at `3108`, defined at `3192` | Full-window local capture, local crop, local artifact path | One explicit raw PNG capture action; Cloud crops/interprets the same frame; no second objective capture in `READ_OBJECTIVE` |
| `ObjectiveTextRecognitionService` at `67,260` | Direct copied/local-style objective recognizer dependency | Final TURN-28 raw-frame typed Cloud recognizer API after parent freeze |
| `TextRecognizer` at `6,261`; path-based OCR at `3437` | OCR reads a saved local image path | Cloud OCR over TURN-17 raw PNG; no path/Base64 compatibility route |

Additional typed collaborators that TURN-37 must consume rather than reimplement are TURN-21 CommonBox, TURN-22 TeamReturn, TURN-23 PlayerState, TURN-25/26 Dialog, TURN-27 Navigation, TURN-28 NpcClick, TURN-34A AutoCombat, and TURN-34B TaskMaintenance.

## 6. Critical API gap requiring parent freeze

The current Cloud main source does not contain either imported type:

- `com.bot.dhxy.vision.ObjectiveTextRecognitionService`
- `com.bot.dhxy.core.TextRecognizer`

The existing Cloud recognizers are package-private legacy entry points:

- `com.yueyunfe.dhxy.cloudbrain.ObjectiveTextRecognizer`: package-private, `recognize(JsonNode)`, decodes `imagePayloadBase64`.
- `com.yueyunfe.dhxy.cloudbrain.QuestDetailTextRecognizer`: package-private, `recognize(JsonNode)`, decodes `imagePayloadBase64`.

`XiuluoTaskV2` lives in `com.bot.dhxy.task.xiuluo`, so it cannot legally consume those classes. More importantly, their Base64/`JsonNode` contract violates the current raw multipart PNG turn contract. TURN-28 is the planned owner of the final reusable image/NPC recognition surface, but that API is not yet delivered.

Before TURN-37 claim, the parent brief must name the exact public typed API that accepts the current action's raw frame and returns the Cloud-owned objective interpretation. The implementation must not:

- copy either recognizer into the Xiuluo package;
- use reflection or a wrapper chain to reach package-private code;
- restore Base64 JSON image transport;
- move OCR or objective parsing to DHXY;
- broaden TURN-37 into unrelated recognizer production files without an explicit plan/write-set amendment.

## 7. Exact implementation and test write-set suggestion

### Production

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java`

No new same-package DTO is currently justified. If a real distinct value type becomes unavoidable after predecessor APIs freeze, the parent must list the exact filename before claim; the worker must not invent an unlisted helper/facade layer.

### Named test

1. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\task\xiuluo\XiuluoWholeTaskTurnContractTest.java`

### Fixed implementation report

1. The parent-created TURN-37 implementation card/report only; this readiness report remains helper-only and append-only.

All other production, protocol, fixture, configuration, POM, task, service, plan, CR, matrix, dashboard, and test files remain read-only for the TURN-37 worker unless the parent explicitly amends the card before claim.

## 8. Strict 696 equivalence checklist

The implementation and named test must preserve these facts exactly.

### 8.1 Startup, priority, and shared state

1. Hot-start priority remains: combat -> task dialog -> team-return signal -> Xiuluo tracker -> return item -> saved objective -> normal accept flow.
2. Common-box pending is 30 seconds and is consumed before the next accepted-task maintenance/prepath point; it does not replace Xiuluo phase decisions.
3. A verified return-home map/coordinate is a task fact without TTL; clear it only when the accept option is actually clicked.
4. STOP, pause, and interruption remain control terminals, not business failure, retry, or success.

### 8.2 Accept, objective, tracker shortcut

1. Clicking the accept option schedules exactly one accept-time objective image for background Cloud parsing.
2. `READ_OBJECTIVE` only waits for/consumes that future. It performs no second capture, dialog detection, story read, task-panel read, or fallback OCR in the same phase.
3. No-maintenance flow may fire the validated exit prepath before consuming tracker; maintenance-due flow must use a fresh tracker read afterward.
4. Tracker click parks/yields. Pathing terminal rereads/retries the current tracker route; it does not use a fixed wait or splice into an old route middle.
5. `TRACKER_CONFIRM` combat returns home after expected exit; incidental combat cleans up and resumes tracker routing.
6. First shortcut failure with saved objective enters full `NAVIGATE_TO_TARGET`; with no objective it enters the `READ_OBJECTIVE` recovery chain. A later shortcut-path failure restarts the round.
7. TURN-30 exact context identity, terminal propagation, source binding, park and phase behavior remain unchanged.

### 8.3 Retry, recovery, fallback, and timing

1. `MAX_PHASE_RETRY=1`.
2. `MAX_RECOVERY_COUNT=2`.
3. Consecutive failed rounds cap at `10`.
4. Loop guard behavior remains the baseline guard (`>32`), with no new durable workflow or ledger.
5. Pre-combat watchdog remains `180000 ms`, with the exact baseline whitelist/exclusions and maintenance/pause compensation.
6. Return verification remains `2` attempts with `500 ms` between attempts.
7. Normal task-turn handoff remains `900 ms`.
8. Combat wake delay remains clamped to `500..10000 ms`.
9. Maintenance broadcast handoff remains `3000 ms`.
10. Observer snapshot/probe maximum ages remain `3000 ms` / `10000 ms`; prepared route dialog maximum age remains `10000 ms` where the reviewed predecessor owns those observations.
11. Maintenance hooks remain at most `5` attempts.
12. Enter-battle confirmation remains `4` no-combat ticks and `2` confirmation retries.
13. Team-ready/team-return polling remains `3000 ms`.
14. The detailed phase failure/recovery mapping in `docs/业务逻辑.md` lines `1272-1288` must be asserted, not replaced by a generic fail-closed branch.

### 8.4 Enter-combat and return rules

1. The shared enter-combat attempt identity remains current-attempt scoped.
2. Local template miss is observation, not a business fallback decision.
3. Only Cloud may request a static-image fallback or follow-up click; every explicit fallback is a new action/UUID, with the existing maximum of three actual Cloud fallbacks where that shared contract applies.
4. Expected-combat quick exit does not add a full radar wait before return.
5. Return verification failure followed by trusted `IN_COMBAT` returns to `WAIT_COMBAT`; otherwise follow the exact safe-recovery branch.

### 8.5 Historical CR material

The current CR271 authority says strict `696a12b0` and records no approved business differences. TURN-37 must not import an older CR230 sidecar/session/ledger/soft-restart implementation merely because historical prose mentions it. Any intended departure from the exact current baseline requires the parent to amend the card with a user-approved behavior difference before implementation.

## 9. HTTPS turn acceptance matrix

The sole named test should use `BC4+BASE+TASK+IMG+LS` and cover at least the following executable cases. “Count” means captured fake-port invocations and UUIDs, not a source-text assertion.

| Scenario | Required assertion |
|---|---|
| Exact binding | Initial and latest metadata retain device/window/title/HWND/process identity; missing or drifted identity rejects before port/action |
| One invocation | Each typed collaborator invocation creates exactly one command and one action UUID unless 696 explicitly performs a later business fallback; no transport retry |
| Ordered JSON | Every action contains only closed `CAPTURE/MATCH_TEMPLATE/INPUT/WAIT/LOCAL_SERVICE` steps in the exact source order |
| Raw image | At most one requested outcome frame per action; multipart raw PNG bytes, SHA, dimensions, purpose and screen-absolute region match; no Base64 |
| Coordinates | Use actual `windowRect.left/top`, unscaled screen-absolute points; never normalize window origin to `0,0` |
| Completed | Physical/mechanical success advances only through the existing Cloud phase decision |
| Failed step | Failed step is typed, remaining steps are `NOT_RUN`, failure evidence is returned, and the phase follows the exact 696 retry/recovery branch |
| STOPPED | No later steps/collaborators run; STOP is not mapped to false/success/business failure |
| DUPLICATE_OR_UNCERTAIN | Never mapped to success or automatic retry; Cloud chooses the next explicit action |
| Accept objective | Accept click triggers one objective capture; `READ_OBJECTIVE` creates zero captures and consumes the same future/frame |
| Objective hit/miss | Cloud parses raw PNG; hit stores map/coordinate; miss follows the baseline recovery with no local OCR or second capture |
| Tracker first read | Typed TURN-30 read retains exact asynchronous context; found green link enters shortcut path, terminal/miss follows exact baseline branch |
| Tracker park | Successful click emits the expected action once, parks/yields, and pathing terminal rereads current tracker without fixed-delay polling |
| Shortcut incidental combat | Enters `WAIT_COMBAT`, then cleanup and tracker continuation; does not return home as task-confirmed combat |
| Shortcut confirmed combat | `TRACKER_CONFIRM` expected exit enters return-home flow without an extra completion tracker read |
| Shortcut failure ladder | Saved objective -> `NAVIGATE_TO_TARGET`; absent objective -> `READ_OBJECTIVE` recovery; later shortcut failure -> round restart |
| Normal target route | Navigation -> target NPC click -> enter-battle confirmation retains phase order and predecessor typed terminal mapping |
| Enter-battle counts | Four none ticks, then no more than two confirmation retries; each explicit fallback has a new UUID and no hidden local loop |
| Return verification | Exactly two attempts and `500 ms` spacing; trusted combat correction returns to `WAIT_COMBAT` |
| Common box | Pending/priority/30-second rule and task-turn ownership remain exact; no Xiuluo-private duplicate state |
| Maintenance | `AFTER_ACCEPT` and `BEFORE_ROUTE` order, maximum five hooks, Summon/team coordination, and `3000 ms` handoff remain exact |
| Bag local boundary | Return-item/incense uses TURN-14 typed `LOCAL_SERVICE`; direct `BagService` invocation count is zero |
| UI local boundary | Cleanup uses TURN-15 typed operations in the same 696 call sites/order; direct `UICleanerService` count is zero |
| Quest local boundary | Task-detail fallback, only where baseline permits it, uses one TURN-17 command/raw PNG; direct `QuestManagerService` count is zero |
| OCR ownership | Objective/task-detail/dialog interpretation occurs in Cloud; DHXY OCR/business invocation count is zero |
| Direct mechanics zero | Xiuluo has zero direct `InputSequences`, `GameClientTracker`, `WindowScopedTempPath`, filesystem-image, and local-template business calls |
| Retry budgets | Exact `1/2/10/>32/180000` retry, recovery, round-failure, loop-guard, and watchdog behavior is asserted by message, count, and phase order |
| Team waits | `WAIT_TEAM_READY` and `WAIT_TEAM_RETURN` poll at `3000 ms` and retain source-dependent return phases |
| Pause/stop | Pause compensation preserves watchdog elapsed time; stop/interruption creates no business retry, cleanup invention, or false success |

The test must exercise the real public `execute(TaskExecutionContext)` and phase collaborators. It must not satisfy acceptance by invoking a private helper, synthetic DTO, duplicate parent chain, or a caller that does not compile.

## 10. Risks and conflict controls

### R1 - Objective recognizer contract is not yet consumable

Impact: TURN-37 can otherwise compile only by copying a recognizer, using Base64/`JsonNode`, or moving OCR local, all of which violate the plan.

Control: wait for TURN-28 final source/test-source delivery; parent freezes one public typed raw-frame API and exact terminal mapping before claim.

### R2 - Start dependencies are not source-stable

Impact: a TURN-37 worker would bind guessed TURN-27/28/34A/34B methods and produce churn or duplicate logic.

Control: do not issue implementation ownership until every precise `S=` dependency has its required source gate and the parent records final signatures.

### R3 - Same-file TURN-30 work must survive

Impact: replacing the whole Xiuluo file from 696 would erase the reviewed typed tracker caller and exact asynchronous context.

Control: edit the current bytes incrementally; compare every change against both current TURN-30 source and exact 696 baseline.

### R4 - One-frame rule can be broken by convenience fallback

Impact: a second objective capture/OCR in `READ_OBJECTIVE` changes the user-approved workflow and network behavior.

Control: test capture count `1` at accept and `0` in `READ_OBJECTIVE`; any later wider image is a separate explicit Cloud business action only where 696 already authorizes that fallback.

### R5 - Permanent-local Service boundary can leak

Impact: direct in-process Bag/UI/Quest calls make Cloud depend on unavailable DHXY classes and move orchestration local.

Control: direct service imports/calls are zero; use only the reviewed typed `LOCAL_SERVICE` clients. `GiveItemService` is not needed in the observed Xiuluo path and must not be introduced speculatively.

### R6 - Generic failure handling can erase phase semantics

Impact: mapping all terminals to boolean/empty or a common restart changes retry counts, park behavior, tracker fallback, combat correction, and round failure accounting.

Control: the named test asserts every phase row from the authoritative failure table, including messages/counts/order and STOP/uncertain separation.

### R7 - Hidden scope expansion

Impact: adding DTO/facade/recognizer/protocol files creates overlap with active writers and a second authority.

Control: one production file plus one named test; any extra path requires a parent amendment before claim.

### R8 - Source completion is not card completion

Impact: source review alone could be mistaken for final delivery.

Control: after writers stabilize, parent runs the named TURN-37 test and applicable compile/build. Whole-task approval also requires Foundation debt cards TURN-T01/T02/T03/T04; the TURN-37 test cannot bypass them. Two independent reviewers and parent final judgment remain separate later gates under `AGENTS.md`.

## 11. Executable parent dispatch brief

When the precise dependency set is satisfied, the parent can issue TURN-37 with this brief:

1. Claim exactly the current Cloud `XiuluoTaskV2.java`, the sole `XiuluoWholeTaskTurnContractTest.java`, and the fixed implementation report; no other file.
2. Start from current bytes, preserving TURN-30 tracker conversion. Use `696a12b0` only as business-semantic authority, not as an overwrite source.
3. Replace every direct local dependency listed in Section 5 with the final reviewed typed predecessor. Do not alter phase order, conditions, messages, count budgets, park/keep-turn boundaries, timing, expiry, fallback, or STOP semantics.
4. Use minimum HTTPS JSON turns. One typed invocation creates one command/UUID; a 696 business fallback is a new explicit Cloud action, never local/transport retry.
5. Upload raw PNG multipart only. Cloud performs crop/OCR/template interpretation. Local matching occurs only when an explicit `MATCH_TEMPLATE` step requests it.
6. Use only the four permanent-local services through the closed typed `LOCAL_SERVICE` operations; Xiuluo directly needs the reviewed Bag/UI/Quest boundaries and must not invent GiveItem usage.
7. Keep mouse actions queue-owned and exact screen-absolute/unscaled. Keyboard remains background-capable where the reviewed mechanics support it. Do not call input/capture/runtime during implementation.
8. Implement the Section 9 matrix in the sole named test using real public task entry and fake typed ports. Do not add source guards or unrelated test families.
9. Deliver source/test-source evidence only. The worker does not run Maven/JUnit/runtime and does not write approval language.
10. Parent independently reviews source/test-source, schedules the named test and applicable compile after all Java writers stabilize, then runs the required two independent reviewer gate and final judgment.

## 12. PRECHECK_COMPLETE

- completedAt: `2026-07-16T03:42:53-04:00`
- agent: `Feynman`
- platformAgentId: `019f69d1-6733-79f0-b7b6-ce99b8830a18`
- identityCorrectionApplied: yes; this identity supersedes the generic text in the initial replacement claim
- readinessEvidence: complete
- implementationClaimReadiness: dependency-gated pending TURN-28P -> TURN-22/TURN-28, TURN-27 final API, TURN-33 -> TURN-34A/34B, and parent final interface freeze
- sourceOrConfigMutation: none
- testsOrBuildsRun: none
- runtimeOrInputStarted: none
- gitMutation: none
- authority: non-binding helper evidence only; parent remains the sole manager/final reviewer

<!-- TRUE_EOF: TURN-37 readiness-preflight-helper PRECHECK_COMPLETE Feynman 019f69d1-6733-79f0-b7b6-ce99b8830a18 2026-07-16T03:42:53-04:00 -->
