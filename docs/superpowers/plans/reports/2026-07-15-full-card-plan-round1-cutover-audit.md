# CR271 HTTPS Turn Full Card Plan Round-1 Cutover Audit B

> Role: non-binding plan/source preflight helper  
> Scope: `TURN-18..TURN-47`, including the not-yet-materialized `TURN-34` subcards  
> Result vocabulary: `PRECHECK` and recommendations only; this report does not change card state, dispatch work, or make a final review decision.

## 1. PRECHECK summary

`PRECHECK: RISKS_FOUND`

The business direction is consistent with the approved thin-client contract: Cloud owns OCR, image computation,
business decisions and retry/fallback; DHXY executes exact-window capture/input and the four closed local Services.
The current card graph is not yet mechanically complete enough to dispatch all `TURN-18..47` cards unchanged.

The highest-impact planning gaps are:

1. The authoritative card plan still gives `TURN-18..24` a direct dependency on `TURN-13`, while CR271 and the
   migration matrix already introduced `TURN-13H`. Current source proves that the `/turn` exchange is retained only
   inside `CloudTurnRoutes.Bundle`, `CloudServiceHost` has no command-port bean, and the host scans no turn clients.
   Every Cloud card that publishes a capture/input/local-Service action therefore needs `TURN-13H` directly or
   transitively.
2. `TURN-18..37` are expected to create real turn callers before `TURN-39` creates `TurnGameClient`,
   `TurnTaskServicePort`, and `TurnTaskServiceExecutionContext`. Current `TaskExecutionContext` still exposes only
   old `CloudGameClient`/`CloudTaskServicePort` (`TaskExecutionContext.java:189-202`). If early cards directly inject
   `CloudTurnCommandPort`, the later facade cannot adopt them because `TURN-39` does not own those Service files. If
   they wait for the facade, the dependency order is circular. Split the current `TURN-39` into an early create-only
   facade/context capability and a late old-facade removal card.
3. There is no current production construction path for `WubeiTask`, `FiveRingTaskV2`, or `XiuluoTaskV2`: a full
   source scan finds only their declarations. `CloudServiceConfiguration` scans `com.bot.dhxy.service`, not tasks,
   and `CloudServiceHost.create(...)` has no caller. `TURN-40` must therefore freeze a task factory/registry,
   exact-window execution-context creation, authenticated scope/state-root selection, and close ownership; its
   present write set (`CloudBrainServer` plus three DHXY files) does not name that capability.
4. The current Cloud task copies are not merely missing transport calls. They still import DHXY tracker/input/window
   runtime and permanent local Service classes. They also contain fallback `TaskExecutionContext.builder()` calls,
   while the Cloud `TaskExecutionContext` has no builder. `TURN-35..37` must remove these source-only construction
   paths as part of their exact task-file cutover.
5. `TURN-38` and `TURN-39` are ordered in the wrong direction for compilation. `TURN-38` owns
   `TaskExecutionContext`, but the replacement types do not exist until `TURN-39`; `TURN-39` then cannot modify
   `TaskExecutionContext`. The current `TaskExecutionContextHolder.java:44` also calls `isPauseRequested()`, which
   the current Cloud context does not expose, yet that holder is absent from the `TURN-38` write set.
6. `TURN-44 -> TURN-45` has a deletion-order hazard. `RemoteTaskRunRoutes` and `CloudBrainServer` still reference
   `CloudTaskRunAuthorityAssembly`; deleting the authority in `TURN-44` before `TURN-45` severs routes produces an
   uncompilable intermediate tree. Either sever routes first, or merge the two cards into one exclusive deletion
   cohort with one build gate.
7. Zero-reference gates must be symbol/path specific. Generic searches for `/outcome`, `RemoteCommand`, or
   `final-consumed` produce unrelated business endpoints, comments, and the new `TurnOutcome`; deletion evidence
   must name the exact old route constants, classes, enum members and configuration keys.

## 2. Source facts used by every card

- `CloudBrainServer.java:87-93` creates the only live `CloudTurnExchange` and registers its handlers.
- `CloudTurnRoutes.java:43-68` retains the same `CloudTurnCommandPort`, but the accessor is package-private.
- `CloudServiceHost.java:35-46` registers only scope/storage/configuration; `CloudServiceHost.create(...)` currently
  has no Java caller.
- `CloudServiceConfiguration.java:23-30` scans only `com.bot.dhxy.service` and imports only the current CommonBox and
  TeamReturn assemblies. Tasks and `turn.client` are not scanned.
- `CloudServiceScope` is tenant/user only. Exact `deviceId` currently lives in
  `TaskExecutionContext.getScope().deviceId()`, while `windowId`, handle and process id are exposed separately.
- `CloudTurnActionFactory` requires caller-provided stable `actionId`, `deviceId`, and `windowId`; it does not mint
  identity or retries.
- The exchange receives `TurnWindowMetadata` in `CloudTurnExchange.exchange(...)` (`:99-108`), but
  `CloudTurnCommandPort` exposes only `execute(TurnAction, Duration)`. There is no general Cloud metadata/facade API
  for business Services today.
- Current callers are source-reachable through `AutoBattleTask`, `AutoCombatService`, and the three main Task files,
  but they are not runtime-reachable through a new Cloud task host yet.
- `docs/业务逻辑.md` requires preservation of CommonBox 30-second pending priority, Summon static slot rules,
  expected-combat return correction, 五倍 dialog-interest and pre-walk behavior, 白龙马/黄袍/普通怪 branches,
  修罗 phase/park/retry/fallback/verification/expiry behavior, and the retained NPC reference/shadow pipeline.

## 3. Per-card audit

### TURN-18 - Binding/geometry/focus/stop metadata

- **Goal:** Replace old `BINDING/GEOMETRY` facts with request/outcome metadata; keep focus diagnostic-only and stop
  free of new business meaning.
- **Dependency sufficiency:** `TURN-13` alone is insufficient. Add `TURN-13H` and the early turn context/facade
  capability. `TurnBindingMetadata` also needs a stated source: current request/outcome metadata or exact task
  context, not a new cache/session.
- **Write set/mutex:** `ClientIdentityService.java` plus new `TurnBindingMetadata.java`; strictly sequential with
  `TURN-23`, which also owns `ClientIdentityService.java`.
- **Reachable caller:** `PlayerStateService.syncMyIdentity()` calls
  `ClientIdentityService.scanAndSyncIdentity(...)` (`PlayerStateService.java:156-159`). The current implementation
  still calls `readWindowFact(BINDING)` (`ClientIdentityService.java:46,86-89`).
- **Acceptance/zero-ref:** No business-source references to `WindowFactKind.BINDING` or `GEOMETRY`; exact window
  metadata must be validated against action device/window; focus cannot fail a business turn; stop only checkpoints.
- **13H/host/context:** Direct 13H/context dependency. No host activation is needed for source construction, but a
  real caller cannot execute before TURN-40.
- **Risk/recommendation:** Freeze whether metadata is passed as a method argument or read from an early
  `TurnTaskServiceExecutionContext`. Do not let a tenant/user host-level singleton hold mutable current-window data.

### TURN-19 - LeftTopStatusSwitchService

- **Goal:** Upload the fixed ROI, decide OPEN/CLOSED in Cloud, and issue click only for OPEN.
- **Dependency sufficiency:** Add `TURN-13H` and early turn facade/context. A source card may retain the existing
  public Service API, but it cannot continue through `CloudLeftTopStatusPortAssembly`'s old fact/input methods.
- **Write set/mutex:** `service/lefttop/CloudLeftTopStatusPortAssembly.java` and
  `LeftTopStatusSwitchService.java`; mutually exclusive with any `TURN-34` child touching the latter's callers only
  at behavioral integration time, not file time.
- **Reachable caller:** `AutoBattleTask` and `AutoCombatService` both inject this Service. Public entry points are
  `handleLeaderStartup`, `probeMemberStartup`, `consumeFollowerSafeWindow`, and `handleCombatMaintenance`
  (`LeftTopStatusSwitchService.java:49-112`).
- **Acceptance/zero-ref:** Zero `LEFT_TOP_STATUS` fact use in production source; action contains capture followed by
  Cloud decision and a separate/new explicit click action; existing pending-close semantics remain Cloud state.
- **13H/host/context:** Direct. The assembly is under the Service scan, but still needs the same exchange capability,
  stable action identity and exact task/window context.
- **Risk/recommendation:** The plan says “bind one real caller” but does not name it or permit caller-file edits.
  Name the caller for coverage evidence while keeping this card's source write set limited to the Service/assembly.

### TURN-20 - AutoCombatPanelService

- **Goal:** Move panel visibility/position/rounds/drag decisions to Cloud and express Alt/drag as ordered input.
- **Dependency sufficiency:** Add `TURN-13H`, early turn facade/context, and `TURN-18` for geometry. Current
  `dependsOn: TURN-13` omits both.
- **Write set/mutex:** `AutoCombatPanelService.java` and explicitly listed Cloud panel decision/model files. It must
  be complete before any `TURN-34A` child modifies `AutoCombatService.java` caller behavior.
- **Reachable caller:** `AutoCombatService` calls panel visibility, verify/align, refresh and combat-exit methods
  (`AutoCombatService.java:342,361,657,696,727`).
- **Acceptance/zero-ref:** Zero `AUTO_COMBAT_PANEL` and panel `GEOMETRY` fact calls; no Cloud import of local tracker,
  OCR, input, temp-path or HWND classes; drag remains one ordered action with post-action evidence.
- **13H/host/context:** Direct plus metadata/context. Host activation remains TURN-40.
- **Risk/recommendation:** Current file still has many DHXY-only dependencies and uses old fact/input calls
  (`AutoCombatPanelService.java:71-80,119-351`). The write set must name every new Cloud algorithm/model file and
  forbid retaining local implementations merely to compile.

### TURN-21 - CommonBoxService

- **Goal:** Keep the 30-second pending state, role isolation and priority in Cloud; DHXY only captures and clicks.
- **Dependency sufficiency:** Add `TURN-13H`, early context/facade and `TURN-18`. `TURN-13` alone does not supply a
  real action publisher.
- **Write set/mutex:** `CloudCommonBoxPortAssembly.java` and `CommonBoxService.java`; sequential with the
  `AutoCombatService`, `AutoBattleTask`, Wubei and Xiuluo caller cards, but file-disjoint from them.
- **Reachable caller:** `AutoCombatService`, `AutoBattleTask`, `WubeiTask`, and `XiuluoTaskV2` inject it. Current
  public calls cover leader/member detection, pending consume, pending query and role clear
  (`CommonBoxService.java:64-226`).
- **Acceptance/zero-ref:** Zero `COMMON_BOX` fact use; retain per-window/task-run keys, 30-second expiry and the
  business priority from `docs/业务逻辑.md:69-168`; click point remains screen absolute.
- **13H/host/context:** Direct. Cloud host scope alone is insufficient for per-window pending keys, so the exact
  task/window identity must be an invocation input, not host-global mutable state.
- **Risk/recommendation:** Do not reinterpret the project's ban on adding business TTL as permission to remove the
  already-approved CommonBox 30-second baseline; this card preserves existing TTL, it does not add one.

### TURN-22 - TeamReturnService

- **Goal:** Cloud decides member button and leader signal; preserve incense-before-click and wait/precheck order.
- **Dependency sufficiency:** `TURN-14` is relevant for incense/Bag, but add `TURN-13H`, early context/facade,
  `TURN-18`, and `TURN-23` if the final code still calls `PlayerStateService.ensureSheYaoXiangActive(...)`.
- **Write set/mutex:** `CloudTeamReturnPortAssembly.java` and `TeamReturnService.java`; caller Task files remain later.
- **Reachable caller:** `AutoBattleTask`, `WubeiTask`, and `XiuluoTaskV2`; public methods include member click,
  marker probe, wait, signal read and leader precheck/consume (`TeamReturnService.java:55-304`).
- **Acceptance/zero-ref:** Zero `TEAM_RETURN_BUTTON` and `TEAM_RETURN_LEADER_SIGNAL` facts; no new polling cadence;
  existing wait timing and incense order stay unchanged.
- **13H/host/context:** Direct. Per-window asynchronous precheck state must remain tied to exact context and must not
  become a new owner/session abstraction.
- **Risk/recommendation:** Current dependency on `PlayerStateService` (`TeamReturnService.java:41,67`) makes the
  declared graph incomplete unless that call is replaced by TURN-14's typed Bag facade or TURN-23 is completed first.

### TURN-23 - PlayerState and ClientIdentity

- **Goal:** Cloud owns title parsing, HP/MP thresholds, first-aid planning and incense decisions; ordinary DHXY has no
  first-aid macro.
- **Dependency sufficiency:** Existing `TURN-14` and `TURN-18` are necessary; also require `TURN-13H` transitively and
  the early context/facade.
- **Write set/mutex:** `PlayerStateService.java`, `ClientIdentityService.java`, first-aid and incense ports. Sequential
  after TURN-18 and after TURN-14's edit to `PlayerStateService.java`.
- **Reachable caller:** `AutoBattleTask`, `AutoCombatService`, all three main Tasks, TeamReturn, Navigation and NpcClick
  inject or call PlayerState. `ClientIdentityService` is reached by `syncMyIdentity()`.
- **Acceptance/zero-ref:** Zero first-aid local macro and incense-status old capture port; all OCR/template/digit
  computation runs in Cloud; local input is only typed turn mechanics; no new cache expiry or retry.
- **13H/host/context:** Direct/transitive. It also needs exact current window outcome metadata for pixel ROI and title.
- **Risk/recommendation:** Current `PlayerStateService.java:70-83` still injects local `GameContext`, tracker,
  `InputProvider`, `InputSequences`, local `BagService`, OCR/temp-path and old ports. The card must enumerate any new
  pure Cloud model files and ensure no local implementation is copied forward.

### TURN-24 - BattleRadarService

- **Goal:** Capture and Cloud-compute auto/selection/top/minimap/avatar baseline/probe/refresh while preserving the
  four-stage priority, fast-exit cadence and probe counts.
- **Dependency sufficiency:** Add `TURN-13H`, early context/facade and `TURN-18`.
- **Write set/mutex:** `BattleRadarService.java` plus named Cloud algorithm/model files; caller integration follows in
  `TURN-28` and `TURN-34A`.
- **Reachable caller:** `AutoCombatService`, `NpcClickService`, and `NavigationService`. The current Service exposes
  main radar, fast expected exit, baseline refresh, signals and dynamic polling (`BattleRadarService.java:65-465`).
- **Acceptance/zero-ref:** Zero seven `BATTLE_RADAR_*` fact kinds; preserve expected-vs-unexpected combat semantics
  and return-home correction from `docs/业务逻辑.md:213-281`.
- **13H/host/context:** Direct. Baseline state must be per exact window, not a tenant-wide current frame.
- **Risk/recommendation:** “Seven old facts zero reference” should list all seven enum constants in the card report;
  otherwise a generic count can miss baseline/probe/refresh variants.

### TURN-25 - Dialog detection and prepared validation

- **Goal:** Upload one frame and perform dialog classification, wash, fingerprint and distance in Cloud.
- **Dependency sufficiency:** `TURN-16` is required for GiveItem; add `TURN-13H`, early context/facade and `TURN-18`.
- **Write set/mutex:** `DialogService.java`, `CloudDialogDetectionPort.java`, and
  `CloudDialogPreparedActionValidationPort.java`; sequential after TURN-16 and before TURN-26.
- **Reachable caller:** Navigation, NpcClick, SummonSkill, TaskMaintenance and all three main Tasks inject Dialog.
  `handleDialog` and prepared-validation public methods are live (`DialogService.java:151,1307`).
- **Acceptance/zero-ref:** Zero dialog-detection/prepared-validation local macro calls; one uploaded frame only;
  physical-action status must not be folded into dialog business success.
- **13H/host/context:** Direct plus exact action identity.
- **Risk/recommendation:** Current `CloudDialogDetectionPort` explicitly reconstructs a Cloud result from a local
  detection macro, contrary to the new responsibility split. Its replacement must be capture-only transport plus
  Cloud processing, not the same macro under a new name.

### TURN-26 - Dialog option OCR and white-story

- **Goal:** Upload one original image; perform OCR, ordering, option choice and white-template handling in Cloud.
- **Dependency sufficiency:** `TURN-25` gives same-frame dialog detection; it should also inherit `TURN-13H`, context
  and TURN-16.
- **Write set/mutex:** `DialogService.java` plus four option/white-story ports; strictly sequential with TURN-25 and
  TURN-27/28 consumers.
- **Reachable caller:** Public prepare methods at `DialogService.java:782-1002`, detection at `:1659-1697`, and Task
  calls in Wubei/FiveRing/Xiuluo.
- **Acceptance/zero-ref:** Zero old option-image, option-words and white-story macro types; ordinary OCR only in Cloud;
  preserve baseline failure ordering and remembered-choice behavior.
- **13H/host/context:** Transitive but required in the actual graph.
- **Risk/recommendation:** “One original upload” must include a single-frame assertion across detection and OCR; do
  not accidentally create detection capture plus OCR capture in the same turn.

### TURN-27 - NavigationService

- **Goal:** Cloud owns map OCR, coordinates, route ladder and fallback; DHXY executes capture/input/local UI clean.
- **Dependency sufficiency:** `TURN-15` and `TURN-26` are necessary. Add `TURN-18`, `TURN-23`, `TURN-24`, `TURN-13H`
  and early context/facade because the current file injects PlayerState/BattleRadar and exact-window services
  (`NavigationService.java:174-195`).
- **Write set/mutex:** `NavigationService.java` plus named navigation model/decision files. Main Task files are later.
- **Reachable caller:** all three main Tasks call `navigateToNPC`/`navigateInCurrentMap`; current public entries are
  at `NavigationService.java:216,524`.
- **Acceptance/zero-ref:** Zero `NAVIGATE_IN_CURRENT_MAP` old macro; preserve route memory, current-map fallback,
  exact coordinates and X2 plus mouse-away/direct-input as one closed action.
- **13H/host/context:** Direct/transitive. Requires exact window metadata and a stable action id per route step.
- **Risk/recommendation:** The current file also injects `NpcClickService` (`:185`), but TURN-28 follows this card.
  Either prove no migrated Navigation path invokes NpcClick before TURN-28, or add TURN-28 as a final integration
  dependency without creating a cycle.

### TURN-28 - NpcClickService

- **Goal:** Keep candidate FIFO, OCR/template, verification and story-blocker decisions in Cloud; DHXY only captures
  and performs atomic move+click.
- **Dependency sufficiency:** `TURN-24` and `TURN-26` cover radar/dialog, but current code also injects
  `PlayerStateService` (`NpcClickService.java:109`) and uses exact-window context. Add `TURN-23`, `TURN-18`,
  `TURN-13H` and early context/facade.
- **Write set/mutex:** `NpcClickService.java` plus named NPC model/decision files. It must not touch retained
  reference/shadow files outside the exact production cutover list.
- **Reachable caller:** Navigation and all three main Tasks. Public paths are smart click, direct-combat target click,
  pending confirmation and expected-option proof (`NpcClickService.java:600,654,2271,2295`).
- **Acceptance/zero-ref:** Production path has no old `NPC_CLICK_SMART` transport endpoint or old fact/macro; one base
  frame per session; atomic move+click; verifier outcome does not absorb downstream Dialog business.
- **13H/host/context:** Direct/transitive.
- **Risk/recommendation:** Preserve `docs/业务逻辑.md:1301-1419` reference/shadow pipeline. Deletion cards must
  distinguish retained reference code from production old-wire handlers.

### TURN-29 - TaskTrackerPanelService core

- **Goal:** Cloud owns anchor/rect/segmentation/OCR/fingerprint/order/selected link; DHXY only captures/materializes
  generic turn evidence.
- **Dependency sufficiency:** `TURN-13` alone is insufficient. Add `TURN-13H`, `TURN-18` and early context/facade.
- **Write set/mutex:** The declared two Service files and eight model files form one dual-repository exclusive card.
  No other writer may touch DHXY `TaskTrackerPanelService`, shared models, handler or turn protocol concurrently.
- **Reachable caller:** Xiuluo, Wubei and FiveRing Tasks currently call their specific panel methods. The DHXY file
  still contains local title matching and current cloud-decision HTTP paths; the Cloud copy exposes Wubei/Xiuluo/
  Wuhuan methods but is not runtime-hosted.
- **Acceptance/zero-ref:** Zero old READ/MATERIALIZE/final-consumed branches and zero ordinary local OCR/green scan in
  production; exact screen-absolute selected point; one frame; replay/debug methods must be classified separately.
- **13H/host/context:** Direct. Also requires TURN-40 task host for runtime reachability.
- **Risk/recommendation:** The declared write set does not name an adapter that issues generic CAPTURE/INPUT. Resolve
  that through the early shared facade, not a TaskTracker-specific DHXY operation.

### TURN-30 - Xiuluo TaskTracker caller

- **Goal:** Close the real Xiuluo caller to the TURN-29 result without changing phase/park/retry/fallback/verification.
- **Dependency sufficiency:** TURN-29 is necessary; add the early context/facade transitively. Runtime reachability
  still waits for TURN-40.
- **Write set/mutex:** Only `task/xiuluo/XiuluoTaskV2.java`; mutually exclusive with TURN-37.
- **Reachable caller:** Current calls include read at `XiuluoTaskV2.java:999,1642,3158` and green-link resolution at
  `:1649`.
- **Acceptance/zero-ref:** No old TaskTracker call shape in Xiuluo; baseline `696a12b0` phase transition and exact
  verification count unchanged; no source-only fallback `TaskExecutionContext.builder()`.
- **13H/host/context:** Transitive plus Task host/context capability.
- **Risk/recommendation:** Count/cutover evidence should name the exact public task entry `execute(context)` and all
  TaskTracker call sites, not only one helper.

### TURN-31 - Wubei TaskTracker caller

- **Goal:** Close Wubei tracker calls to TURN-29 while preserving 五倍 business tables.
- **Dependency sufficiency:** TURN-29 plus early context/facade; runtime host later.
- **Write set/mutex:** Only `task/wubei/WubeiTask.java`; mutually exclusive with TURN-35.
- **Reachable caller:** Current TaskTracker calls at `WubeiTask.java:2043,2359,3987,4045` cover normal and chained
  reads/verification.
- **Acceptance/zero-ref:** All Wubei tracker paths use one Cloud result owner; no old local OCR fallback; pre-walk,
  dialog-interest, normal/yellow/white-dragon branches remain as documented.
- **13H/host/context:** Transitive plus Task host/context.
- **Risk/recommendation:** Include chained-fast verification in the card acceptance; otherwise TURN-29 can be used by
  the primary read while a secondary old path remains.

### TURN-32 - FiveRing TaskTracker caller

- **Goal:** Close FiveRing tracker caller without restoring local TaskTracker algorithms.
- **Dependency sufficiency:** TURN-29 plus early context/facade; runtime host later.
- **Write set/mutex:** Only `task/wuhuan/FiveRingTaskV2.java`; mutually exclusive with TURN-36.
- **Reachable caller:** Current click-point call at `FiveRingTaskV2.java:2483`; related prepared/read paths must be
  included even if hidden behind Service methods.
- **Acceptance/zero-ref:** No local TaskTracker algorithm call from FiveRing; existing phase/wakeup behavior remains
  unchanged unless a separately approved business card says otherwise.
- **13H/host/context:** Transitive plus Task host/context.
- **Risk/recommendation:** `docs/业务逻辑.md:1032+` explicitly labels some FiveRing wakeup ideas as future work; this
  cutover must not implement them opportunistically.

### TURN-33 - SummonSkillService

- **Goal:** Cloud owns slot classification, deletion, ultimate skill, tail boundary and retry decisions; DHXY only
  executes a closed ordered action.
- **Dependency sufficiency:** TURN-15 and TURN-13 are not enough. Add TURN-26 because current Summon calls Dialog,
  plus `TURN-18`, `TURN-13H` and early context/facade.
- **Write set/mutex:** `SummonSkillService.java`, `TaskMaintenanceService.java`, old whole-pass capability and
  exclusive authority. Sequential with every `TURN-34B` child touching TaskMaintenance.
- **Reachable caller:** `TaskMaintenanceService.runOpportunisticMaintenance()` calls
  `cleanSummonSkillsOnce(...)` (`TaskMaintenanceService.java:578,755`).
- **Acceptance/zero-ref:** Zero whole-pass/exclusive acquire-release classes; preserve static 6/8-slot rules from
  `docs/业务逻辑.md:170-211`; no nested queue and no automatic retry.
- **13H/host/context:** Direct/transitive.
- **Risk/recommendation:** The current write set mixes business Service cutover with deletion of a large old
  authority used by the old task-run graph. Defer physical deletion to TURN-44 or prove all references are removed
  in this card; otherwise TURN-33 can break unrelated old context before Tasks are cut over.

### TURN-34A (recommended materialization) - AutoCombatService callers

- **Goal:** Replace all old fact/macro/input calls reachable through AutoCombat's public methods with migrated
  Services/turn outcomes.
- **Dependency sufficiency:** Expand the range to explicit TURN-19/20/21/23/24/33 plus TURN-13H and early context.
- **Write set/mutex:** Only `AutoCombatService.java`; one owner, sequential edits. Do not create multiple concurrent
  subcards for different methods in this same file.
- **Reachable caller:** Public paths include initialization, two `handleCombatTick` overloads, guard/read-only ticks,
  dynamic delays, expected-exit baseline refresh and deferred recovery (`AutoCombatService.java:82-442`).
- **Acceptance/zero-ref:** No direct old port/fact/macro/input use; preserve combat priority/cadence and CommonBox/
  first-aid/incense ordering. Record each covered public method; avoid duplicate historical coverage counting.
- **13H/host/context:** Transitive/direct.
- **Risk/recommendation:** The main plan has not created a real `TURN-34A` card or exact caller list. Freeze this
  file-level unit before dispatch rather than letting workers invent method-level overlapping cards.

### TURN-34B (recommended materialization) - TaskMaintenanceService callers

- **Goal:** Make maintenance coordination consume migrated Dialog/Summon/turn outcomes without old capabilities.
- **Dependency sufficiency:** TURN-21/22/23/25/26/33 and early context are relevant; do not rely on the ambiguous
  `TURN-19..24` range alone.
- **Write set/mutex:** Only `TaskMaintenanceService.java`; starts after TURN-33 releases that same file.
- **Reachable caller:** AutoBattle, Wubei and Xiuluo use task-start/team-window/maintenance methods; the main action
  path is `runOpportunisticMaintenance(...)` (`TaskMaintenanceService.java:578`).
- **Acceptance/zero-ref:** No old whole-pass/exclusive/fact/macro references; preserve approved team-window
  coordination and do not create a second owner/session/permit model.
- **13H/host/context:** Transitive plus exact team/window invocation context.
- **Risk/recommendation:** Explicitly separate existing local-team business coordination from old transport
  authority. “No owner/session” does not authorize deleting approved business state without a baseline comparison.

### TURN-34C (recommended materialization) - AutoBattleTask caller

- **Goal:** Close follower/startup/maintenance calls through migrated Cloud Services and turn outcomes.
- **Dependency sufficiency:** Explicit TURN-19/21/22/23/33/34B plus early context; TURN-20/24 arrive through
  AutoCombat where applicable.
- **Write set/mutex:** Only `AutoBattleTask.java`; no overlap with 34A/34B, but logically follows them.
- **Reachable caller:** `execute(context)` calls startup first aid and maintenance, then TeamReturn, LeftTop and
  CommonBox (`AutoBattleTask.java:111-253`).
- **Acceptance/zero-ref:** No old Service port/fact/macro; no new business retry; startup and follower mode decisions
  unchanged.
- **13H/host/context:** Transitive plus Task host/context.
- **Risk/recommendation:** The main plan's “one public caller per card” could imply several cards in one file. Prefer
  one file-exclusive integration card with a caller checklist; if coverage accounting requires multiple units, keep
  them sequential and name exact methods.

### TURN-35 - WubeiTask complete turn wiring

- **Goal:** Preserve the full 14-state 五倍 baseline while replacing every local action with turn/four-Service calls.
- **Dependency sufficiency:** The broad ranges are semantically sufficient only after expansion to concrete IDs,
  including all TURN-34 children, TURN-13H, and the early context/facade. Add explicit Task host readiness before
  claiming runtime reachability.
- **Write set/mutex:** Only `WubeiTask.java` and named same-package DTOs; follows TURN-31.
- **Reachable caller:** `execute(context)` exists (`WubeiTask.java:344`) but no production Cloud constructor/factory
  currently reaches it.
- **Acceptance/zero-ref:** No imports/fields for Cloud copies of `BagService`, `UICleanerService`, local tracker/input/
  window runtime or old remote ports; all ordinary/white-dragon/yellow-monster/return branches preserve documented
  ordering, budgets, park/wakeup and return correction.
- **13H/host/context:** Requires all three: command capability, exact turn context, and a future Task host.
- **Risk/recommendation:** Current file still imports tracker/input/window runtime and calls permanent local Service
  class copies (`WubeiTask.java:4-82,260-285`). A four-Service facade must be injected, not a Cloud-local Service copy.

### TURN-36 - FiveRingTaskV2 complete turn wiring

- **Goal:** Preserve FiveRing phase/dialog/navigation/item/combat order through turn/four-Service calls.
- **Dependency sufficiency:** Expand all ranges, include TURN-13H/early context and Task host readiness.
- **Write set/mutex:** Only `FiveRingTaskV2.java` and named same-package DTOs; follows TURN-32.
- **Reachable caller:** `execute(context)` exists (`FiveRingTaskV2.java:241`) but no production Cloud factory reaches it.
- **Acceptance/zero-ref:** No local tracker/OCR/input/window imports or Cloud copies of Bag/UI; no implementation of
  future FiveRing wakeup ideas; exact business phase order remains.
- **13H/host/context:** Command capability, exact context and Task host all required.
- **Risk/recommendation:** Current file performs direct template/input/shop mechanics in many private methods, not
  only through injected Services. The acceptance checklist must enumerate and replace those direct paths inside the
  same Task file.

### TURN-37 - XiuluoTaskV2 complete turn wiring

- **Goal:** Preserve exact `696a12b0` 修罗 STOP, keep-turn/park, retry/fallback, verification and expiry semantics.
- **Dependency sufficiency:** Expand all ranges, include TURN-13H/early context and Task host readiness.
- **Write set/mutex:** Only `XiuluoTaskV2.java` and named same-package DTOs; follows TURN-30.
- **Reachable caller:** `execute(context)` exists (`XiuluoTaskV2.java:318`) but no new Cloud Task factory reaches it.
- **Acceptance/zero-ref:** No Cloud copies/direct calls to local Bag/UI/Quest/input/window runtime; all phase tables in
  `docs/业务逻辑.md:1126-1296` remain equal; retained NPC reference/shadow remains outside production transport.
- **13H/host/context:** Command capability, exact context and Task host all required.
- **Risk/recommendation:** Current file still imports permanent local Services and DHXY runtime, and contains a
  `TaskExecutionContext.builder()` fallback (`:3865-3869`). That fallback must not survive as fake Cloud context.

### TURN-38 - Task execution context detached from old retained authority

- **Goal:** Remove business-source dependence on old retained action/final-consumption authority without adding a
  replacement session/ledger.
- **Dependency sufficiency:** Depending only on Tasks is too late for the new context and too early for the types now
  assigned to TURN-39. Reorder after an early create-only facade/context card and before the Service/Task callers that
  require it; reserve a late cleanup part after TURN-35..37.
- **Write set/mutex:** The listed 13 files are not sufficient. `TaskExecutionContextHolder.java` directly calls a
  method absent from the current Cloud context, and `TaskCheckpoint`, `TaskSleep`, `BaseTaskTemplate` and other
  files reference `TaskCheckpointDecision`. Any signature change requires an explicit expanded set or compatibility
  phase.
- **Reachable caller:** Every Cloud Service/Task uses `TaskExecutionContext` or its holder, so this is a shared
  integration boundary, not a late isolated cleanup.
- **Acceptance/zero-ref:** Business packages have zero `RemoteTaskRun*`, retained action and final-consumption refs;
  exact device/window/stop identity remains available; no new owner/session/ledger/TTL.
- **13H/host/context:** Core context capability. 13H alone does not solve it.
- **Risk/recommendation:** Split into `38A` create/adapt exact turn context early and `38B` remove old authority late.
  Do not attempt an in-place replacement before the new TURN-39 types exist.

### TURN-39 - CloudGameClient/ServicePort to turn facade

- **Goal:** Provide typed capture/input/local-Service results without old broker identity/final consumption.
- **Dependency sufficiency:** This capability is a prerequisite for TURN-18..37, not merely a successor of TURN-38.
- **Write set/mutex:** New facade/context types plus old facade files; single integration owner is correct, but the
  card needs `TaskExecutionContext.java` or a previously frozen adapter contract.
- **Reachable caller:** Current `TaskExecutionContext.getGameClient()/getRemoteGameClient()` reaches only old
  facades. No current source references the proposed new types.
- **Acceptance/zero-ref:** Business Services use the new typed facade; action IDs are stable and caller-supplied;
  transport uncertainty never remints/re-executes; no final-consumed concept.
- **13H/host/context:** Direct dependency on 13H and the exact invocation context; also a prerequisite to Task host.
- **Risk/recommendation:** Split `39A` (create facade + context adapter, early) and `39B` (remove old facade, late).
  This removes the current 18..39 dependency cycle and avoids every Service directly parsing raw step JSON.

### TURN-40 - Explicit REMOTE_TURN activation

- **Goal:** Explicit user start/stop per registered window; never run local Task and remote turn simultaneously.
- **Dependency sufficiency:** TURN-39 alone is insufficient. Require TURN-13H, all three Task cutovers, exact Task
  factory/context, host scope/state-root policy and dual build.
- **Write set/mutex:** DHXY control/config files and Cloud `CloudBrainServer.java` are insufficient unless the task
  factory/host lifecycle is implemented in already-created files. No current source constructs the three Tasks or
  calls `CloudServiceHost.create(...)`.
- **Reachable caller:** DHXY has `TurnModeGuard.startRemote(...)` (`TurnModeGuard.java:65`), but
  `WindowTaskControlService` currently only uses the guard for local starts. Cloud has no task activation entry.
- **Acceptance/zero-ref:** Explicit start only; exact registered/open/idle runner; authenticated scope; one host per
  intended scope; stop/unregister closes loop/host; no startup hook, scheduler, auto reconnect business retry or
  second exchange.
- **13H/host/context:** This is the host/context activation card and must consume the same exchange retained by 13H.
- **Risk/recommendation:** Freeze the activation API and task construction path before dispatch. Do not hide them in
  `CloudBrainServer` private code without an exact write set and close semantics.

### TURN-41 - User fresh runtime evidence gate

- **Goal:** User-run evidence for capture, Cloud OCR/calculation, input, post/failure frames, template refresh,
  action-id dedupe and two-window isolation.
- **Dependency sufficiency:** Require TURN-40 plus successful Cloud package and DHXY compile. Add an explicit check
  that all three Task types are activatable, even if the user samples only one task in the first run.
- **Write set/mutex:** No Worker/source write set; parent reads user logs/screenshots only.
- **Reachable caller:** Depends entirely on TURN-40's missing activation/task-host path.
- **Acceptance/zero-ref:** Runtime evidence is behavioral, not a zero-reference substitute. Deletion still requires
  independent source scans.
- **13H/host/context:** Full runtime dependency.
- **Risk/recommendation:** Record exact window IDs/action IDs and distinguish network resend of the same outcome from
  forbidden physical action retry.

### TURN-42 - Delete DHXY old transport/poller/lifecycle

- **Goal:** Remove old poll/outcome/task-run transport and local lifecycle/registry/ledger/receipt after user gate.
- **Dependency sufficiency:** TURN-41 is necessary; also require a clean symbol scan proving TURN-40 no longer calls
  any old lifecycle.
- **Write set/mutex:** The current delete set is descriptive, not exact. Freeze a file manifest before dispatch.
  Current source has 25 Java files containing `RemoteTaskRun`, four containing `RemoteCommandTransport`, and two
  containing `RemoteCommandPollingLoop`.
- **Reachable caller:** Old polling loop is still reached by `RemoteTaskRunLifecycleService`; exact startup/config
  references must be included in the manifest.
- **Acceptance/zero-ref:** Zero exact old route keys, transport interfaces, lifecycle services, registries, ledgers
  and receipts; DHXY compile. Do not use generic `/outcome` zero because unrelated decision endpoints remain.
- **13H/host/context:** No direct 13H implementation dependency, but deletion requires TURN-40 runtime replacement.
- **Risk/recommendation:** Include Spring bean/config references and constructor graph, not only class files.

### TURN-43 - Delete DHXY old handler/operation DTO/special mechanics

- **Goal:** Remove old handler, codecs, operation/fact/macro DTOs and specialized DHXY business mechanics after all
  callers move to generic turn primitives/four Services.
- **Dependency sufficiency:** TURN-42 plus a per-symbol zero-reference manifest from TURN-18..37.
- **Write set/mutex:** Freeze exact files. Preserve Bag/UICleaner/GiveItem/QuestManager and their necessary models,
  plus approved NPC reference/shadow code.
- **Reachable caller:** Current `LocalRemoteGameCommandHandler` is referenced by specialized leader-precheck
  mechanics; old `LocalMacroKind` appears across many DTOs.
- **Acceptance/zero-ref:** No production operation codec/fact/macro handler refs; permanent local Services still
  construct; no ordinary OCR/template logic left in DHXY.
- **13H/host/context:** Indirect only.
- **Risk/recommendation:** Folder-level deletion is unsafe. Classify every file as generic primitive, permanent-local
  dependency, retained reference/shadow, or old-wire-only before deleting.

### TURN-44 - Delete Cloud old business facade/lifecycle coupling

- **Goal:** Remove old remote facade, retained action/final-consumption and exclusive authority after new facade and
  runtime are proven.
- **Dependency sufficiency:** TURN-39 and TURN-41 are necessary, but deletion must coordinate atomically with
  TURN-45 route severing.
- **Write set/mutex:** Current delete set includes `CloudTaskRunAuthorityAssembly`, which is still referenced by
  `RemoteTaskRunRoutes`, execution gate/current slot and server wiring. A standalone TURN-44 cannot compile before
  those references are removed.
- **Reachable caller:** Current `CloudTaskServiceExecutionContext` and `CloudTaskServicePort` directly construct/use
  old `RemoteGameClientPort`, final coordinator, retained state and exclusive authority.
- **Acceptance/zero-ref:** Zero exact old facade/authority symbols outside the deletion cohort; business result/model
  ownership recorded before any `remote/` deletion.
- **13H/host/context:** Indirect; requires the replacement context/facade and active host.
- **Risk/recommendation:** Reverse the sever/delete order or merge TURN-44/45 into one exclusive build cohort.

### TURN-45 - Delete Cloud broker/routes/task-run transport

- **Goal:** Remove broker, old task-run routes/endpoints, command/final DTOs and old server route registration.
- **Dependency sufficiency:** It should sever server/routes before or atomically with TURN-44, not after a prior card
  deletes their dependencies.
- **Write set/mutex:** Exact manifest plus `CloudBrainServer.java`; mutually exclusive with any 13H/TURN-40 server
  change and with TURN-44's remote package delete.
- **Reachable caller:** `CloudBrainServer.java:12,48,77-93` still creates and owns old routes alongside `/turn`.
- **Acceptance/zero-ref:** Zero exact old `/api/v1/client/commands/poll`, outcome/final-consumed/task-run route
  constants and old route classes. Do not remove new `CloudTurnExchange` outcomes or unrelated NPC/route endpoints
  merely because they contain `/outcome`.
- **13H/host/context:** Must preserve the same `/turn` exchange and host capability introduced by 13H/40.
- **Risk/recommendation:** Add a route table before/after snapshot to prove only old transport routes are removed.

### TURN-46 - Configuration/dependency/document cleanup

- **Goal:** Remove only dependencies/config keys used exclusively by the old chain and synchronize protocol docs.
- **Dependency sufficiency:** TURN-42..45 after their combined build gate.
- **Write set/mutex:** Both `pom.xml`, both application properties where applicable, protocol/matrix/CR/dashboard.
  One integration owner; do not overlap parent documentation writers.
- **Reachable caller:** Current DHXY properties contain both `cloud.turn.*` and extensive legacy `cloud.*` service/
  sidecar keys. Jackson, JDK HTTP and image dependencies may still be required by the new turn path.
- **Acceptance/zero-ref:** Every removed key/dependency has zero code/config consumer; new HTTPS URI/token/timeout/
  template-root remain; docs consistently say HTTPS long-wait, no WebSocket/short poll/auto business retry.
- **13H/host/context:** Preserve 13H/40 host and turn properties.
- **Risk/recommendation:** Do not treat all `cloud.*` keys as old transport; classify decision APIs separately from
  poll/outcome/task-run transport.

### TURN-47 - Parent final source review and dual build

- **Goal:** Final responsibility, protocol, dedupe, zero-reference and build gate.
- **Dependency sufficiency:** TURN-46 plus resolved 13H/context/facade/task-host findings and completed runtime gate.
- **Write set/mutex:** Review/docs only; Java writers must already be stable.
- **Reachable caller:** Verify production construction and invocation, not only source references: explicit user
  action -> DHXY loop -> `/turn` -> Cloud host/task -> turn command -> DHXY mechanics -> outcome -> same Task.
- **Acceptance/zero-ref:** Use exact symbol/path scans across both `src/main` trees; separately classify
  `migration-preserved` archives and comments; run Cloud `mvn -q clean package` and DHXY
  `mvn -q -DskipTests compile`; no runtime/tests are agent-started.
- **13H/host/context:** Final audit must prove one exchange, exact host/context identity and no second activation path.
- **Risk/recommendation:** Add an explicit “all three Task constructors/factory registrations reachable” gate and a
  “no `TaskExecutionContext.builder()` fallback in Cloud Tasks” gate to the final checklist.

## 4. Recommended dependency-graph repair before dispatching TURN-18+

1. Add `TURN-13H` to the authoritative plan and make every action-publishing card depend on it directly or through
   a named early facade/context card.
2. Split current `TURN-39` into:
   - an early create-only typed turn client/context facade, usable by TURN-18..37;
   - a late removal of `CloudGameClient`, `CloudTaskServicePort`, retained/final-consumption wiring.
3. Split current `TURN-38` into early context adaptation and late old-authority cleanup; include
   `TaskExecutionContextHolder` and any checkpoint signatures actually changed.
4. Materialize `TURN-34A/B/C` with the exact file-level caller checklists above. If finer accounting is retained,
   serialize subcards that touch the same file and never run two owners on one Service/Task.
5. Freeze TURN-40's Cloud task factory/registry, exact invocation context, authenticated scope/state-root and close
   lifecycle. The activation card must show a real construction path for Wubei/FiveRing/Xiuluo.
6. Make TURN-44/45 one atomic deletion/build cohort or sever routes before deleting their authority dependencies.
7. Require a named zero-reference manifest for every deletion card; generic substring counts are supporting evidence
   only.

## 5. Scope statement

This report changed no Java, task runtime, main plan, CR card, matrix, dashboard, build output, Git state, test,
application, server, capture or input. It is a non-binding first-round plan/source audit only.
