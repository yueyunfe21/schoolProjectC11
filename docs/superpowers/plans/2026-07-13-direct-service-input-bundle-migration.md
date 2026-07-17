# Direct Service/Input-Bundle Migration Implementation Plan

> **SUPERSEDED EXECUTION UNIT (2026-07-14):** The user selected DHXY `696a12b0` as the pre-cloud source baseline and required whole-Service-first migration. This document remains useful for typed local-boundary mechanics, but its method/cohort execution order is superseded by `docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`. No method/helper slice may be counted as a completed Service.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans task-by-task. This repository is in no-local-test mode; do not add or run tests unless the user explicitly requests one.

**Goal:** Preserve every existing Task/Service business branch while moving business decisions to Cloud and expressing local mechanical work as shared structured commands.

**Architecture:** Cloud keeps the existing Task/Service class boundary and decision order. DHXY keeps window binding, capture, template/OCR, continuous watchers, dialog/pathing/movement/battle observation, UICleaner, and the single physical-input queue. Ordinary physical work uses the existing `RemoteGameClientPort.executeInputBundle(...)`; only a sequence that must observe local pixels while a key/button is held remains one named local macro operation.

**Tech Stack:** Java 17, Spring Boot, Lombok, Jackson, Maven, existing `RemoteGameClientPort`, existing `RemoteInputActionDto`/`RemoteInputActionType`.

## Global Constraints

- Whole-Service source baseline is DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; `0114604e` is only the current migration/transport workspace starting point.
- Do not create per-Service owner, permit, ledger, retry state machine, durable workflow, or new execution thread.
- Reuse the shared scope/window/taskRun/runRevision fence, stable request/action identity, strict action allowlist, one input queue, and terminal outcome.
- An input bundle is an ordered list of existing input action DTOs; it is data, not a second business workflow engine.
- `moveMouse + click`, `focus/type/enter`, and other inseparable physical sequences stay in one bundle.
- A sequence that needs capture/template/OCR between key-down and key-up stays local as one named macro; Cloud receives only its typed result.
- Local capture/template/OCR/watcher/UICleaner code is not copied to Cloud merely to satisfy imports.
- Protect all dirty/untracked work. Do not reset, checkout, clean, delete, commit, or start runtime/application/Task/UI/input.
- Java gates after a stable wave: Cloud `mvn -q clean package`; DHXY `mvn -q -DskipTests compile` when DHXY Java changed.

---

### Task 1: Freeze Obsolete Per-Service Machinery

**Files:**
- Modify: `docs/PACKAGE_ARCHITECTURE.md`
- Modify: `docs/ACTIVE_WORK.md`
- Modify: `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`
- Modify: current Worker A/B/C/D and Internal AB append-only reports

**Produces:** One durable decision that cancels unimplemented per-Service owner/permit/ledger work without deleting already-written files.

- [x] Append the user-approved simplification decision to CR271, ACTIVE_WORK, and the migration matrix.
- [x] Mark each in-flight complex task `CANCELLED_BY_SIMPLIFICATION`; require each worker to stop without cleanup or rollback and list its touched files.
- [x] Keep already-compiled shared protocol safety code pending source review; do not assume it must be deleted.
- [x] Regenerate `docs/cr-dashboard-data.js` with `node scripts/generate-cr-dashboard-data.js`.

### Task 2: Build the Physical-Input Inventory

**Files:**
- Read: the 19 Task/Service files returned by `rg -l "inputSequences\\.|submitAndWait\\(|submitExclusiveAndWait\\(|InputAction\\." src/main/java/com/bot/dhxy/service src/main/java/com/bot/dhxy/task`
- Create: `docs/superpowers/plans/reports/2026-07-13-input-bundle-inventory.md`

**Produces:** For every call site: source method, baseline line, ordered actions, atomic boundary, coordinate space, local observation dependency, and migration disposition.

- [x] Record each call site exactly once; do not infer new actions or normalize existing delays.
- [x] Classify it as `ONE_BUNDLE`, `LOCAL_MACRO`, `LOCAL_RESIDENT`, or `NO_PHYSICAL_INPUT`.
- [x] Mark `move + click`, text-entry plus Enter, drag sequences, and held-key sequences as atomic where the current code already requires that atomicity.
- [x] For `LOCAL_MACRO`, name the exact existing local method that performs capture/template/OCR inside the physical sequence; do not design a new workflow DSL.
- [x] Parent reviewer compares the inventory against `0114604e` and rejects any changed order, delay, retry, or fallback.

### Task 3: Migrate Pure Input-Bundle Service Cohorts

**Files:**
- Modify/Create only the Cloud copies of services classified `ONE_BUNDLE` and their direct compile dependencies.
- Modify DHXY only when the existing shared local transport lacks a DTO mapping for an already-supported `RemoteInputActionType`.

**Consumes:** Task 2 inventory rows classified `ONE_BUNDLE`.

**Produces:** Cloud Service methods with the same branch order that call `CloudTaskServicePort.executeInputBundle(...)` using one retained action identity per existing business action.

- [ ] Copy the service method from `git show 0114604e:<path>` before adapting collaborators.
- [ ] Replace only the physical input submission with an ordered `InputActionStep` list matching the inventory byte-for-byte in values and order.
- [ ] Keep caller-visible return values and exception/fallback behavior unchanged.
- [ ] Do not introduce a Service-specific state owner or retry; use the shared port outcome.
- [ ] Run Cloud `mvn -q clean package` after the cohort write set is stable.

### Task 4: Keep Observation-Interleaved Sequences Local

**Files:**
- Modify only existing DHXY local mechanics entry points classified `LOCAL_MACRO`, plus the smallest closed request/result DTO needed by the shared port.
- Modify Cloud Service call sites to invoke the typed local macro and consume its result.

**Consumes:** Task 2 inventory rows classified `LOCAL_MACRO`.

**Produces:** One local call for each physically continuous sequence that must inspect pixels before releasing control.

- [ ] Reuse the existing local method and its current queue/exclusive boundary.
- [ ] Request contains only closed parameters already chosen by Cloud business logic; no raw Java callback, filesystem path, or unrestricted script.
- [ ] Result reports the existing observed business fact; it does not add retry, expiry, or a new final-consumption ledger.
- [ ] Keep key/button release in `finally` exactly where the baseline has it.
- [ ] Run DHXY `mvn -q -DskipTests compile` and Cloud `mvn -q clean package` after both sides are stable.

### Task 5: Migrate Remaining Business Classes in Dependency Order

**Files:**
- Modify/Create Cloud `service/**` leaves first, then Task classes.
- Keep DHXY local-resident capability classes in place.

**Produces:** Existing Task/Service business logic running in Cloud with all local effects crossing only the shared port.

- [ ] Move pure CPU/model/config dependencies without behavioral edits.
- [ ] Migrate Service cohorts using Task 3 or Task 4 according to the inventory.
- [ ] Migrate Task classes only after all direct Service dependencies compile in Cloud.
- [ ] Keep the Cloud host dormant; do not start production or local input.
- [ ] Run fresh Cloud package and applicable DHXY compile after each stable cohort.

### Task 6: Final Static and Build Gate

**Files:**
- Modify: `docs/ACTIVE_WORK.md`
- Modify: `docs/PACKAGE_ARCHITECTURE.md`
- Modify: `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`
- Modify: `docs/cr-dashboard-data.js`

**Produces:** A reviewable migration state with no hidden business logic in DHXY and no physical mechanics in Cloud.

- [ ] Scan Cloud business classes for direct JNA/input/window/capture/template/OCR dependencies.
- [ ] Scan DHXY retained classes for task phase, retry, navigation, or business-decision ownership that should have moved to Cloud.
- [ ] Confirm every Task 2 row has exactly one implemented disposition.
- [ ] Run Cloud `mvn -q clean package` without skipping tests.
- [ ] Run DHXY `mvn -q -DskipTests compile`.
- [ ] Record build results and `无已批准业务差异；按基线等价迁移` in CR271 and regenerate the dashboard.

## 2026-07-13 Direct Migration Progress: CommonBoxService

- `CommonBoxService` is the third direct Service cohort accepted after `LeftTopStatusSwitchService` and
  `AutoCombatPanelService`. The Cloud copy preserves committed `0114604e` API shape, role gates, async detection,
  30-second pending semantics, stale checks, and consume/retain behavior.
- The only local observation is closed `COMMON_BOX`: one exact-window fixed ROI/template probe returns a five-state
  typed fact. Cloud sends the original move+click as one ordered bundle only at the committed consume point.
- Parent source/protocol review: `P0=0 / P1=0 / P2=0`. Fresh DHXY compile and Cloud clean package both passed;
  Cloud reports 4 suites / 21 tests with no failures, errors, or skips.
- Approved same-path count is `188/407`; no paused or unapproved in-flight source is included.

## 2026-07-13 Direct Migration In Flight: Bag Return-Item Local Macro

- User placement decision: the real `BagService` and `UICleanerService` remain in DHXY. Cloud must not contain
  business copies of either Service. Any future Service with both viable placements requires an explicit user choice.
- Approved placement test: a closed observe/match/fixed-input/typed-result mechanics loop with no task-phase,
  business-strategy, cross-Service orchestration, or business retry/fallback decision may remain local. Fixed UI
  delays, input serialization, and cleanup are mechanics. Anything that chooses business flow belongs in Cloud;
  ambiguous Services require a user decision before code placement.
- The next dependency is implemented as one shared closed `LOCAL_MACRO/BAG_RETURN_ITEM`, not as a Bag-specific
  workflow owner, permit, session, ledger, TTL, or retry mechanism.
- The first closed cohort covers only the three APIs consumed by committed `ReturnItemPrescanService`:
  `prescanMainBagTaskPageItem`, `prescanMainBagItemFromBack`, and `useCachedMainBagReturnItem`.
- Cloud `ReturnItemPrescanService` retains the committed random strategy, timing, cache, invalidation, and fallback
  decisions and calls the typed shared macro directly; no Cloud `BagService` facade is created. DHXY retains the
  existing exclusive input boundary and all capture/template/input interleaving, returning only `FOUND/NOT_FOUND`
  or `USED/NOT_USED` through a strict typed result.
- External A/B/C/D own disjoint Cloud type, DHXY wire, local mechanics/handler, and schema slices. Internal AO/AP own
  disjoint Cloud plumbing and Cloud Service slices. Work is direct implementation; no Design #N round is required.
- Completion requires parent source review plus fresh DHXY compile and Cloud clean package. Approved count remains
  `188/407` until that gate passes. No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-13 Placement Decision: GiveItemService

- The user chose `GiveItemService` as a permanent DHXY-local mechanical Service. It selects the requested item through
  local `BagService`, matches the Give button in the exact bound window, performs the fixed click/sleep sequence, and
  returns a boolean result; it owns no task phase, strategy, or cross-Service retry policy.
- Committed `0114604e` has exactly one consumer: `DialogService` calls
  `executeGiveDirectForExclusive(...)` while already inside the input worker's exclusive section. Preserve that call and
  its ordering locally. Do not create a same-name Cloud Service or a standalone `GIVE_ITEM` network operation.
- When the Dialog cohort is migrated, its closed local macro must encompass the existing GiveItem call so the held
  exclusive input section is not split by a network round trip. No Java change is required for this placement decision.

## 2026-07-13 Placement Decisions: TaskTrackerPanelService and QuestManagerService

- `TaskTrackerPanelService` migrates as a same-name Cloud Service. Cloud owns panel/detail geometry, title/task
  candidate selection, green-link segmentation, chained matching, fingerprints/cache decisions, classification,
  and result construction while preserving all committed thresholds, ordering, fallback, and return values.
- DHXY supplies only exact-window frame acquisition, locally required template/OCR primitives, coordinate-safe
  tracker drag/input execution, and closed typed observations. Its adapter must not become a second algorithm owner.
- `QuestManagerService` remains a DHXY-local mechanical Service. It receives an already chosen task name, opens the
  exact task panel, matches/highlights, scrolls/clicks, captures detail, and returns the typed result. Cloud callers
  retain task selection, phase, priority, orchestration, retry, and fallback decisions.

## 2026-07-13 Direct Migration Complete: ReturnItemPrescanService + Bag Local Macro

- Cloud `ReturnItemPrescanService` preserves committed `0114604e` strategy selection, 4-second maintenance gate,
  8..18-second combat due timing, cache/invalidation, fallback, and round-completion semantics. Real `BagService`
  remains DHXY-local and executes the three approved operations inside the single input queue.
- Shared `LOCAL_MACRO/BAG_RETURN_ITEM` is closed across Cloud types, DHXY strict codec/digest, local handler/mechanics,
  transport/final-ack, and schema. All terminal outcomes use exact four-key flat transport payloads; the client and
  Cloud both allow only `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`, while typed results exist only for `EXECUTED`.
- Parent review: `P0=0 / P1=0 / P2=0`. Fresh DHXY `mvn -q -DskipTests compile` exit 0. Fresh Cloud
  `mvn -q clean package` exit 0 with 4 suites / 21 tests and no failures, errors, or skips. Approved count is
  `189/407`; runtime remains dormant.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 Throughput Correction #2: Full Public Chains Replace Exclusion Cohorts

- The prior six-way cohort produced only one TaskTracker method and three SummonSkill methods; TaskMaintenance,
  Navigation, NpcClick, and PlayerState produced zero Java. Root cause was the parent-authored admission rule that
  excluded every method whose required collaborator was not already present. That rule is retired.
- External A/B/C/D now own four disjoint, countable implementation chains: DecisionEngine to same-path
  TaskTrackerPanelService; TaskMaintenance summon/first-aid/team-window public coordination; SummonSkill's two
  committed public APIs over the existing closed local whole-pass capability; and Navigation's three public route
  entries over the shared typed fact/capture/InputBundle port.
- Internal CE and CF concurrently own the TeamReturn public fact/InputBundle plus leader-precheck chain and the
  BattleRadar Cloud state/signal/timer public chain. They do not review and do not overlap any external Java file.
- A Service's own baseline state maps, passive records/enums, Cloud config snapshot, explicit context projection,
  and private closure are now admitted in the same task. Local HWND holders, capture/template/OCR implementations,
  watchers, pathing/battle observations, and the physical input queue remain in DHXY.
- C's new `matchYellowTemplateInScan` is blocked at P1 because it performs template matching in Cloud; the same C
  must remove only that unapproved method/import before completing the public local-pass facade. All other workers'
  delivered zero-Java evidence is accepted but is not migration progress.
- Validation is now a reachable public call graph plus baseline-equivalent behavior, not a private-helper count.
  Approved same-path count remains `189/407` until parent source review and a fresh Cloud package close a full chain.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF: Coherent A-D Closure Wave Dispatched

- The previous four external leaves are parent-approved and the fresh Cloud package is green (4 suites / 21 tests).
- Four new disjoint implementation tasks were appended to the workers' fixed logs with a 06:36 claim deadline:
  AutoCombatPanel full direct closure (A), TaskTracker in-memory green-scan input (B), SummonSkill static-slot
  classifier (C), and PlayerState supply-plan generation with an immutable one-shot settings record (D).
- This wave replaces one-line helper slicing with coherent algorithm or Service closure. It adds no design round,
  owner, session, ledger, TTL, retry, host, caller, capture, or physical-input execution.
- External tasks are never internally taken over. The same-path count remains `189/407` until parent review and a
  fresh package close a countable Service/class chain.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF: Queue/Algorithm Wave Approved and Four Direct Slices Reissued

- Parent source review independently extracted the committed and Cloud blocks for TaskMaintenance summon-skill
  queue state, TaskTracker prepared-action construction, PlayerState conservative first-aid-plan caching, and
  SummonSkill clean-deadline handling. All four are approved with `P0/P1/P2=0` and no behavioral drift.
- After every Java writer stabilized, parent fresh Cloud `mvn -q clean package` passed with 4 suites / 21 tests,
  and DHXY `mvn -q -DskipTests compile` also passed. The shaded Cloud JAR is 120456347 bytes.
- Four new disjoint implementation tasks are visible at the workers' true EOF with an 08:39 claim deadline:
  TaskTracker supplied-artifact detail crop (A), Navigation map-name canonicalization (B), AutoCombat deferred-log
  throttle (C), and PlayerState incense fact application with explicit windowId only for logging (D).
- These are direct implementations, not Design rounds. They activate no caller/host/runtime surface and add no
  owner, session, ledger, TTL, retry, capture, or input behavior. External tasks are never internally taken over.
- Approved same-path count stays `189/407` until a complete Service/caller/typed-local-primitive chain closes.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 Direct Migration Progress: Four External Leaves Approved and Reissued

- Parent source review approved four exact committed blocks with `P0/P1/P2=0`: NpcClick metadata assembly,
  TaskTracker classifier projection, SummonSkill slot offsets, and PlayerState supply-target helpers.
- After all Java writers stabilized, parent fresh Cloud `mvn -q clean package` passed with 4 suites / 21 tests and
  regenerated the shaded JAR. No DHXY Java changed in this wave.
- External A/B/C/D immediately received four disjoint direct implementation slices: current-queue identity,
  image request metadata, cleanup-result construction, and first-aid bar probing. B/C claimed within two minutes;
  A/D remain inside the 20-minute claim window. These are implementation tasks, not Design rounds.
- All four slices remain private and dormant and perform no capture/template/OCR/input/caller activation. The
  approved same-path count stays `189/407` until a complete public/caller/typed-local-primitive chain closes.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 Direct Migration Progress: Six Disjoint Code Slices

- Parent source review approved the latest NpcClick tooltip-path, TaskTracker title-family,
  TaskMaintenance identity-index, AutoBattle retry-policy, and TeamReturn pathing-text slices against
  committed `0114604e`; all complete blocks are exact and compile after shared writers stabilized.
- The attempted PlayerState task-run-id leaf was withdrawn rather than adapted because the committed API uses
  `long` while the Cloud context exposes a typed `String`. No parsing or fallback semantic was invented.
- External A/B/C/D now own four new disjoint implementation slices. Internal BO/BP own two additional disjoint
  pure-CPU slices. No worker owns review, and no external task is internally taken over.
- These partial dependencies do not advance the `189/407` same-path completion count; a class is counted only when
  its public/caller/typed-local-primitive chain and the fresh parent build gate close.

## 2026-07-14 Direct Migration Progress: A-D + Internal BR/BS

- Parent review approved exact committed blocks for AutoCombat timing constants, NpcClick PNG-memory encoding,
  TaskTracker chained-fast result construction, TeamReturn no-match diagnostics, and PlayerState in-memory
  supply-needed classification. Navigation duplicate-source compatibility was already present and exact, so its
  task was a safe no-op rather than a duplicate method.
- A pre-edit baseline check caught a parent brief typo before source mutation: SummonSkill
  `buildTipRectByHoverPoint` returns an `int[]` corner tuple in `0114604e`, not a `Rectangle`. The same worker
  continues with the exact baseline signature; no behavior adaptation was approved.
- External A/B/C/D now own four disjoint direct code slices in NpcClick, TaskTrackerPanel, TaskMaintenance, and
  TeamReturn. Internal BR/BS own disjoint SummonSkill and PlayerState slices. No worker reviews its own work and no
  external task is internally taken over.
- Approved same-path count remains `189/407`; these are dependency blocks, not full Service closure. Parent
  `mvn -q clean package` waits until all active Java writers are stable.

## 2026-07-14 Direct Migration Progress: A-D + Internal BR/BS Approved

- Parent source review approved six disjoint committed blocks: NpcClick template-spec construction,
  TaskTracker expanded-anchor projection, TaskMaintenance first-aid group resolution, TeamReturn leader-precheck
  result value, SummonSkill hover-tip rectangle construction, and PlayerState in-memory PNG transfer encoding.
  Each block is exact against `0114604e`, compiles in Cloud, and has `P0/P1/P2=0`.
- Internal BR/BS are closed. External A/B/C/D now own four disjoint direct code slices: NpcClick metadata cohort,
  TaskTracker classifier-result projection, SummonSkill slot-offset metadata, and PlayerState supply-target helpers.
  These tasks contain no capture/template/OCR execution, input, caller activation, or new workflow machinery.
- The `189/407` count remains unchanged because these are partial dependency blocks. Parent fresh Cloud clean package
  is deferred until the four active writers deliver stable source; no runtime surface is activated.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF: Four External Leaves Approved and Reissued

- Parent source review approved four exact committed blocks with `P0/P1/P2=0`: NpcClick metadata assembly,
  TaskTracker classifier projection, SummonSkill slot offsets, and PlayerState supply-target helpers.
- After all Java writers stabilized, parent fresh Cloud `mvn -q clean package` passed with 4 suites / 21 tests and
  regenerated the shaded JAR. No DHXY Java changed in this wave.
- External A/B/C/D immediately received four disjoint direct implementation slices: current-queue identity,
  image request metadata, cleanup-result construction, and first-aid bar probing. B/C claimed within two minutes;
  A/D remain inside the 20-minute claim window. These are implementation tasks, not Design rounds.
- All four slices remain private and dormant and perform no capture/template/OCR/input/caller activation. The
  approved same-path count stays `189/407` until a complete public/caller/typed-local-primitive chain closes.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF: Second A-D Direct Wave Approved

- A-D all claimed and delivered within the published cycle. Parent extraction confirmed exact committed blocks for
  NpcClick current-message identity, TaskTracker image metadata, SummonSkill cleanup-result construction, and
  PlayerState first-aid bar probing; all four are `APPROVED` with `P0/P1/P2=0`.
- D correctly followed the real eight-parameter committed method rather than the approximate six-parameter wording
  in the parent brief. No adaptation or behavior change was introduced.
- Parent fresh Cloud `mvn -q clean package` passed with 4 suites / 21 tests and regenerated the shaded JAR. No DHXY
  Java changed in this wave.
- The approved same-path count remains `189/407`: these private dormant leaves are prerequisites, not a closed
  Service public/caller/typed-local-primitive chain. The next dispatch should prefer coherent compile-ready algorithm
  cohorts over one-line helper slicing, and must not migrate local transport/window/input infrastructure by count.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF: Coherent A-D Closure Wave Dispatched

- The previous four external leaves are parent-approved and the fresh Cloud package is green (4 suites / 21 tests).
- Four new disjoint implementation tasks were appended to the workers' fixed logs with a 06:36 claim deadline:
  AutoCombatPanel full direct closure (A), TaskTracker in-memory green-scan input (B), SummonSkill static-slot
  classifier (C), and PlayerState supply-plan generation with an immutable one-shot settings record (D).
- This wave replaces one-line helper slicing with coherent algorithm or Service closure. It adds no design round,
  owner, session, ledger, TTL, retry, host, caller, capture, or physical-input execution.
- External tasks are never internally taken over. The same-path count remains `189/407` until parent review and a
  fresh package close a countable Service/class chain.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF: Queue/Algorithm Wave Approved and Four Direct Slices Reissued

- Parent source review independently extracted the committed and Cloud blocks for TaskMaintenance summon-skill
  queue state, TaskTracker prepared-action construction, PlayerState conservative first-aid-plan caching, and
  SummonSkill clean-deadline handling. All four are approved with `P0/P1/P2=0` and no behavioral drift.
- After every Java writer stabilized, parent fresh Cloud `mvn -q clean package` passed with 4 suites / 21 tests,
  and DHXY `mvn -q -DskipTests compile` also passed. The shaded Cloud JAR is 120456347 bytes.
- Four new disjoint direct implementation tasks are visible at the workers' true EOF with an 08:39 claim deadline:
  TaskTracker supplied-artifact detail crop (A), Navigation map-name canonicalization (B), AutoCombat deferred-log
  throttle (C), and PlayerState incense fact application with explicit windowId only for logging (D).
- These are implementation tasks, not Design rounds; they activate no caller/host/runtime surface and add no owner,
  session, ledger, TTL, retry, capture, or input behavior. External tasks are never internally taken over.
- Approved same-path count stays `189/407` until a complete Service/caller/typed-local-primitive chain closes.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF: Detail/Canonical/Log/Fact Wave Approved and Reissued

- Parent source review approved the complete committed blocks for TaskTracker supplied-artifact detail crop,
  Navigation canonical map name, AutoCombat deferred-log throttle, and PlayerState incense fact application.
  All four have `P0/P1/P2=0`; the only non-identical projection is the explicitly approved `windowId` logging
  parameter in the PlayerState Cloud block.
- After every Java writer stabilized, parent fresh Cloud `mvn -q clean package` passed with 4 suites / 21 tests and
  regenerated the 120458926-byte shaded JAR. No DHXY Java changed in this wave.
- Four new disjoint direct implementation tasks are visible at the workers' true EOF with a 09:02:43 claim
  deadline: TaskTracker Xiuluo marked-image generation (A), TaskMaintenance existing not-due diagnostic throttle
  (B), SummonSkill supplied-path image payload reading (C), and BattleRadar action-state polling interval policy
  (D). D has claimed; A/B/C remain inside the claim window.
- These are implementation tasks, not Design rounds. They activate no caller/host/runtime surface and add no owner,
  session, ledger, business TTL, retry, capture, or input behavior. External tasks are never internally taken over.
- Approved same-path count stays `189/407` until a complete Service/caller/typed-local-primitive chain closes.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 Throughput Correction: Six Coherent Implementation Cohorts

- The parent stopped one-helper-per-task slicing after the user identified the throughput problem. The four current
  external leaves were source-approved first, then each worker received a same-file coherent follow-on cohort.
- External A owns TaskTracker pure artifact/image/result logic; B owns TaskMaintenance summon queue/window state;
  C owns SummonSkill image/artifact algorithms; D owns Navigation route-policy/value logic. Each cohort targets at
  least six complete methods or one complete algorithm chain.
- Internal CA and CB concurrently own disjoint NpcClick request/result/metadata and PlayerState
  snapshot/request/result/policy cohorts. No worker owns review, and no internal worker takes over an external task.
- A candidate whose committed call graph requires a missing collaborator is excluded and documented while the rest
  of the cohort continues; workers may not adapt semantics or invent seams to force compilation.
- The parent remains the sole source reviewer and will run one fresh Cloud package after all six writers stabilize.
  Approved same-path count remains `189/407` until a full public/caller/typed-local chain closes.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE TRUE EOF: Full Public Chains Replace Exclusion Cohorts

- The prior six-way cohort produced only one TaskTracker method and three SummonSkill methods; TaskMaintenance,
  Navigation, NpcClick, and PlayerState produced zero Java. The parent-authored “missing collaborator means exclude”
  rule caused the low throughput and is now retired.
- External A/B/C/D own four disjoint reachable chains: DecisionEngine to same-path TaskTrackerPanelService;
  TaskMaintenance summon/first-aid/team-window coordination; SummonSkill's committed public APIs over the closed
  local whole-pass capability; and Navigation's three public route entries over shared typed facts/capture/input.
- Internal CE `019f60ca-18b8-7173-ab9d-cb347c5d485d` owns TeamReturn public fact/InputBundle/leader-precheck;
  Internal CF `019f60ca-6344-7811-9f6d-f275717649cb` owns BattleRadar Cloud state/signal/timer APIs. The two
  internal write sets are disjoint from all four external Services.
- A Service's own committed state maps, passive types, Cloud config, explicit context projection, and private closure
  are admitted in one task. DHXY retains HWND, capture/template/OCR implementations, watchers, pathing/battle
  observation, and the physical input queue. No new owner/session/ledger/business TTL/automatic retry is allowed.
- C must remove only its unapproved Cloud template-match method/import before closing the public facade. Zero-Java
  reports are accepted as task evidence but not migration progress. Validation is a reachable public call graph,
  baseline equivalence, compile, and later parent fresh package, not a private-helper count.
- Approved same-path count remains `189/407` until a complete chain passes parent review and the fresh build gate.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE TRUE EOF: Six Countable Public Chains, Not Six Helper Slices

- Parent source review blocked External A's unconditional TaskTracker reader cutover (`P1=2/P2=1`), because it
  converted supported non-detail modes to `NO_ACTION` and dropped baseline diagnostics. A keeps the valid detail
  foundation and repairs only mode routing and exact result fields.
- External B's TaskMaintenance queue/window cohort is partially source-approved; its only open `P1` is the missing
  baseline startup cooldown initialization. The local HWND-dependent shadow publisher is diagnostic plumbing and
  is intentionally not copied into Cloud.
- External D's zero-Java Navigation inventory is not migration progress. D now owns an expanded end-to-end
  `NAVIGATE_IN_CURRENT_MAP` closed local-macro chain, preserving the committed 60-second observation/input loop
  inside the single local input-worker exclusive section. External C's SummonSkill facade is source-approved and C
  has been issued the complete four-entry NpcClick smart chain at the report's true EOF.
- Internal CG `019f60e1-96f2-7f83-b9eb-f0775b36210d` owns the complete PlayerState supply/first-aid/incense public
  chain. Internal CH `019f60e1-aaeb-7252-a4db-552735d83db0` owns the complete non-host AutoCombat public
  orchestration chain. Their Java write sets are one disjoint Cloud Service each.
- All six implementation slots are therefore assigned to countable public chains. No new per-Service owner,
  session, ledger, TTL, retry, host, or production activation is allowed. Parent clean/package waits until every
  active Java writer is stable. Approved same-path count remains `189/407`.
- No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 AUTHORITATIVE TRUE EOF: A/B Repairs Pass and Roll Directly into 11/30-API Cohorts

- TaskTracker's no-regression mode router and exact detail `links` fields are functionally source-approved. Its one
  stale JavaDoc statement is folded into External A's next task, which owns the eleven remaining same-path public
  APIs across the Wuhuan, Xiuluo, and Wubei families.
- TaskMaintenance's startup cooldown branches and corrected default-window JavaDoc are source-approved with
  `P0/P1/P2=0`. External B now owns the thirty remaining pure coordination public APIs across local-team session,
  combat phase, pathing/return, maintenance-broadcast FIFO, and baseline throttle. Three capture/input-interleaved
  mechanics entries remain explicitly local and outside that cohort.
- External C claimed the four-entry NpcClick smart chain at 10:00:27. External D claimed the end-to-end
  `NAVIGATE_IN_CURRENT_MAP` local macro at 09:57:14. Internal CG/CH continue the complete PlayerState and
  AutoCombat public chains. The six active write sets remain disjoint.
- B's latest non-clean compile was polluted only by D's in-flight five-component local-macro constructor update;
  TaskMaintenance had zero errors. Parent does not run a concurrent clean or treat this transient state as a B
  regression. Approved same-path count remains `189/407` until the full cohorts pass source review and fresh builds.
- No approved business differences; migrate equivalently to `0114604e`.
## 2026-07-14 10:14 - AutoCombat 完整公开链首版审查

- Internal CH 已落 10 个可保留 public API，但 committed 的 8 个 tick/read-only/reconcile 主入口仍缺，且
  Cloud 当前无 `AutoCombatService` caller；父级结论为 `PARTIAL SOURCE APPROVED / BLOCKED，P1=1`。
- CH 不得用 stub 或省略 phase 来补入口。它继续等待 TaskMaintenance team-phase/session API、typed local battle
  observation 与 closed local UI-clean operation；BattleRadar watcher/UICleaner mechanics 永久留 DHXY。
- 本轮不增加 `189/407`，其余五路继续并发，writer 稳定后再统一 fresh 双构建。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 10:25 - Typed-consumer contract and PlayerState ordering gate

- External A must implement all eleven remaining TaskTracker baseline-name Cloud algorithm entries with one shared
  immutable typed artifact. DHXY remains responsible for exact-window capture, title-template/OCR primitives,
  screen conversion, drag, and input; Cloud returns window-relative decisions/results. The JavaDoc-only delivery is
  therefore still blocked until real Java entries exist.
- Parent review found two P1 ordering regressions in Internal CG's PlayerState first-aid chain. `healAll` must retain
  one local exclusive sequence with immediate supply after each 350ms confirmation; `healPlayer/healPet` must retain
  per-target capture followed immediately by input and safe move. CG is suspended, not discarded, until External D
  releases the shared local-macro write set.
- Internal CI now owns the three missing BattleRadar typed-consumer public entries in one Cloud Service file. Local
  battle observation remains in DHXY; Cloud preserves signal priority, two-miss exit confirmation, fast-exit timing,
  and transition semantics. CI does not touch remote/schema/DHXY/caller files.
- Approved same-path count remains `189/407`. No approved business differences; migrate equivalently to `0114604e`.

## 2026-07-14 10:37 - Maintenance partial approval and navigation macro simplification

- External B's 29 landed TaskMaintenance coordination APIs are source-approved; the remaining queue-head consumer
  stays Cloud coordination and will synchronously call one closed local maintenance operation after D releases the
  shared macro write set. B is suspended rather than allowed to invent a callback/two-phase permit.
- External D's first NAVIGATE macro wire is blocked on three behavioral losses: it flattened the complete
  `NavigationRequest`, collapsed caller-visible navigation statuses, and proposed duplicating mechanics inside the
  sole input worker. Repair now mirrors the complete request/status and invokes the existing local
  `NavigationService.navigateInCurrentMap` outside the queue; that method retains its own queue/watcher behavior.
- Internal CH's two new AutoCombat public entries and six private closures are source-approved. Six public
  definitions still depend on CI's typed BattleRadar entries and D's closed UI-clean adapter, so CH is suspended
  with its landed code preserved.
- Count remains `189/407`; no approved business differences, migrate equivalently to `0114604e`.

## 2026-07-14 10:50 - Whole-class migration is the only completion unit

- `0114604e` is the current structure/migration starting point, not a universal business-behavior authority.
  For Wubei/Xiuluo behavior, use the user-approved commit named by `docs/业务逻辑.md`; conflicts stop for user review.
- Preserve the complete baseline public/private call graph, branches, ordering, delays, fallbacks, and state
  mutations; substitute only the exact local mechanics call sites with typed facts/captures/InputBundles or one
  closed local macro.
- Method-, helper-, DTO-, and policy-level work already landed is retained as reusable code, but none counts as a
  migrated Service by itself. Completion requires a reachable public caller, complete same-path Service, typed
  DHXY primitive/terminal boundary, parent whole-class review, and fresh build gates.
- Internal CI's three BattleRadar typed consumers are source-approved with `P0/P1/P2=0`, but remain a retained block
  until the complete caller chain closes. External A's eleven TaskTracker entries are blocked with four P1 contract
  breaks and cannot be used as a method-count proxy for class completion.
- Navigation repair must mirror the complete `NavigationRequest` and terminal status and invoke the existing local
  `NavigationService.navigateInCurrentMap` outside the input queue. No worker may recreate or shorten its middle
  algorithm.
- Approved same-path count remains `189/407`. No approved business differences; migrate equivalently to the
  applicable user-approved business baseline.
