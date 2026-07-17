# CR271 TURN-36 latest dependency / collision delta

## 0. Role and snapshot

This is a non-binding `PRECHECK` for `TURN-36 FiveRingTaskV2 complete turn wiring`. It does not create a
TURN-36 owner, claim, fixed card, implementation permission, review result, or parent status transition. Only the
CR271 parent may freeze a future `READY` state after the conditions below are independently rechecked.

Final dynamic snapshot used by this report:

- `docs/ACTIVE_WORK.md` and the authoritative plan were reread through their `2026-07-16 07:26:52 -04:00`
  bytes.
- `TURN-28P` fixed card was reread through the `07:26:05` parent replacement assignment and physical EOF at
  `07:26:34`.
- `TURN-34A` fixed card was reread through the `07:18:45` parent resume observation. Its active named test was
  reread from the `07:25:48` bytes; those bytes are still being written and are not treated as a stable dependency.
- No source, test, original card, plan, `ACTIVE_WORK`, matrix, dashboard, or other report was modified.

## 1. Complete read set and repository protection

The following governing files were completely read:

| Material | Snapshot evidence |
|---|---|
| `AGENTS.md` | 392 lines; SHA-256 `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md` | 1,349 lines; SHA-256 `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `docs/ACTIVE_WORK.md` top CR271 | latest top through `:40`, including the `07:25` B return and `07:26` A replacement assignment |
| authoritative plan sections 14-19 | 681 lines (`:1010-1690`); latest SHA-256 `650B9357146A8063223A542A7D2FB92B1433C126B3472004587DC9846480E067` |
| HTTPS turn protocol | 383 lines; SHA-256 `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| `docs/业务逻辑.md` | 1,426 lines; SHA-256 `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` |
| prior TURN-36 readiness report | 352 lines; SHA-256 `F27B886E5FF026E9E24B0939BE5665D70BDD1BB54E252A603DF827BD415BF39C` |

All direct-dependency fixed cards were completely read: `TURN-13C`, `TURN-14`, `TURN-15`, `TURN-23`,
`TURN-26`, `TURN-32`, `TURN-28P`, and `TURN-34A`. The complete dependency report set was also read, including
both TURN-13C helpers, TURN-27 readiness, all three TURN-28 readiness/launch reports, both TURN-28P delivery
reviews, the TURN-28P mechanics and Repair #1 production/test preflights, all three TURN-34A readiness/launch
reports, and the prior TURN-36 readiness report. `TURN-23P` was read as the transitive protocol prerequisite.

Actual read-only source inspection covered both repositories, the `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
FiveRing/PlayerState baseline, current Cloud FiveRing and its phase/context types, all direct service callers,
turn protocol/client/adapter code, `TaskExecutionContext`, host configuration, and the live DHXY task factory/runner.

Read-only Git snapshot before this report was created:

| Repository | Branch / HEAD | Protected worktree |
|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 650 status entries: 44 tracked dirty and 606 untracked |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 status entries: 9 tracked dirty and 541 untracked |

Cloud `FiveRingTaskV2.java` is currently untracked. The future whole-task test does not exist. Cloud `src/test/`
is ignored by `.gitignore:15`, so the active TURN-34A test exists on disk without appearing in normal short status.
Every dirty/untracked byte remains protected; no Git mutation was performed.

## 2. PRECHECK delta

1. The authoritative `startDependsOn` remains exactly
   `S=13C+14+15+23+26+27+28+32+34A` (`plan:1152`). Six of nine source-start gates are currently satisfied:
   `13C`, `14`, `15`, `23`, `26`, and `32`.
2. The three not-yet-satisfied direct gates are `27`, `28`, and `34A`. `27` cannot stabilize before the final
   `28` public API; `28` cannot stabilize before the `28P` repair source gate.
3. Latest TURN-28P delta: External B returned its owner at `07:25:04`. The parent assigned the remaining two-test
   harness work to External A at `07:26:05`, but the fixed card did not yet contain External A's required claim at
   this snapshot. There is no TURN-28P delivery or parent source/test-source pass for this repair.
4. Latest TURN-34A delta: production `AutoCombatService.java` exists, and the named test now exists and is actively
   changing. The fixed card still has no completed two-file delivery or parent source/test-source pass. TURN-36
   therefore must not freeze against the current in-flight constructor/test bytes.
5. The unique TURN-36 production/test paths remain physically disjoint from all current External and Internal
   write sets. Dependency ordering, not a same-file collision, is the current concurrency constraint.
6. Two parent-freeze gaps remain independent of those source gates: the unrepresented open-main-bag supply
   boundary and the one-file ownership of FiveRing's old runtime/mechanics callers. Neither is a frozen interface.

## 3. `startDependsOn` evidence

| Direct dependency | Current source-start evidence | TURN-36 consequence |
|---|---|---|
| `TURN-13C` | Fixed card physical EOF records parent source/test-source review; turn-native context/holder APIs exist | Source gate satisfied; final shared build debt is not a source-start gate |
| `TURN-14` | Parent source/test-source review recorded; open-main-bag FiveRing boundary explicitly excluded | Source gate satisfied, but it does not supply TURN-36's combined supply contract |
| `TURN-15` | Parent source/test-source review recorded; UI local client is stable | Source gate satisfied |
| `TURN-23` | Parent source/test-source review recorded; card says open-main-bag caller session remains for TURN-36 (`plan:1130`) | Source gate satisfied; retained interface is not itself a migrated contract |
| `TURN-26` | Parent source/test-source review recorded; typed Dialog option/story API is present | Source gate satisfied |
| `TURN-27` | Readiness only; final public API waits TURN-28. Current `NavigationService.java` SHA-256 is `66D5480722CF07C643BDABB9E53D84FFA203FD6184B8DFCAE6DEED313ED4AFF2`; named test absent | Source gate not yet satisfied; do not guess navigation signatures |
| `TURN-28` | Readiness/launch preflights only. Current `NpcClickService.java` SHA-256 is `F4E3842CDB5F59580D8F25F0191ADE4847BFE8CA6C7939AC73A70BD561BFD870`; named test absent | Source gate not yet satisfied; do not freeze its unfinished API |
| `TURN-32` | Parent source/test-source review recorded; typed prepared tracker result is already the only reviewed delta in FiveRing | Source gate satisfied |
| `TURN-34A` | Unique External C owner is active. Production SHA-256 is `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`; named test was still changing | Source gate not yet satisfied; wait for stable delivery and parent source/test-source review |

Transitive TURN-28P evidence is newer than the plan's older activity text. Its fixed card `:847-870` assigns only
these replacement modification paths to External A after a true-EOF claim:

1. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`.
2. DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`.
3. The append-only TURN-28P fixed card.

The other nine B-returned files are protected read-only at their returned SHA. Until that repair is delivered and
parent-reviewed, neither `TURN-28` nor downstream `TURN-27` has a final source contract for TURN-36.

## 4. Real current caller and registration state

### 4.1 Current runnable caller

Cloud FiveRing is not currently registered or runnable:

- `CloudServiceConfiguration.java:27-34` scans only `com.bot.dhxy.service` and the narrow turn-client package, not
  `com.bot.dhxy.task`.
- `CloudServiceHost.create(...)` at `CloudServiceHost.java:39-64` creates and refreshes an unregistered service
  context. There is no production `new FiveRingTaskV2(...)` or Cloud task factory caller.
- Real Cloud task construction/queue activation remains a later `TURN-40B` responsibility. TURN-36 is a source
  migration card, not activation or HTTP registration.
- The only live production caller remains DHXY local execution:
  `DefaultTaskFactory.java:35-41` maps `WUHuan_V2` to the local provider, and
  `WindowTaskRunner.java:608,766` creates and executes that local task.

The future whole-task named test must therefore directly instantiate and execute production Cloud
`FiveRingTaskV2` through its public API. Host/factory registration cannot be used as proof for TURN-36.

### 4.2 Current Cloud FiveRing bytes

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java` is 2,775 lines,
SHA-256 `287FF0EBE4F3CECF9820A10D2FFCBF0F7AED2A26BEB7A5F510D92F540E8A4BDB`. A read-only no-index comparison
against `696a12b0` shows only the reviewed TURN-32 typed tracker cutover; the remainder is still the local baseline
shape.

The current file still directly holds or calls:

- `NavigationService`, `NpcClickService`, `DialogService`, `PlayerStateService`, `AutoCombatService`, and the
  TURN-32 `TaskTrackerPanelService` (`:200-209`).
- old transaction/runtime/metrics authority at `:210,214-218`, `:263-317`, `:420-487`, and numerous
  `WindowRuntimeContext`/ready-event sites through `:2574`.
- direct local input, coordinate/template/capture mechanics at `:1146-1184`, `:1287-1485`, and `:2566-2569`.
- the context-free `TaskExecutionContext.builder()` fallback at `:2751-2756`.

Static source resolution finds these imported FiveRing types absent from the Cloud main source tree:

`GameClientTracker`, `TextRecognizer`, `AutomationMetricsService`, `BagService`, `UICleanerService`,
`TaskTransactionRunner`, `CoordinateHelper`, `GameStateUtil`, `WindowRuntimeContext`, `WindowReadyEventBus`,
`WindowScopedTempPath`, `WindowTaskContextHolder`, and `OcrWindowScanService`.

Current `TaskExecutionContext.java:96-109` exposes `turnNative(...)`, but it has neither
`getWindowRuntimeContext()` nor `builder()`. FiveRing nevertheless calls the former at `:263-264,296-297` and the
latter at `:2751`. This is direct static evidence, not a compile invocation.

The plan gives TURN-38A only its seven context/checkpoint/base-task files (`plan:1302-1312`), and the 38A preflight
assigns FiveRing's context-free caller removal back to TURN-36. Because TURN-38A cannot later edit FiveRing, the
parent must freeze the one-file TURN-36 mapping for every old runtime/transaction/ready-event/metrics caller before
issuing the card. Silently deleting phase/yield/wakeup behavior or postponing Task edits to 38A would not satisfy the
current write-set plan.

## 5. `696a12b0` FiveRing business invariants

The migration has no approved business difference. At minimum the future card and named test must retain:

1. Phase ownership and order:
   `PREPARE -> BUY_SHOES -> HANDOVER_DETECT -> ACCEPT_TASK -> WAIT_PATHING -> HANDLE_DIALOG -> SYNC_TASK_PANEL`,
   plus distinct `FINISHED/FAILED/STOPPED` terminals.
2. PREPARE order at baseline `FiveRingTaskV2:757-809`: UI cleanup, startup first aid, one-bag supply check, quick-buy
   only when shoes are short, exactly one same-boundary verification after quick-buy, then shop-owner fallback only
   if still short.
3. The supply callback at baseline `:1094-1116`: open main bag once, run incense status/use logic, count shoes up to
   the required number in the same `MainBagSession`, then close once.
4. Incense logic at baseline `PlayerStateService:529-643`: memory/status/full-probe decisions remain Cloud business;
   item use occurs only after those decisions, exactly once when required; the existing one-minute failed-use
   cooldown is preserved. No new TTL, verification, retry, or fail-closed business rule may be added.
5. Existing quick-buy, shop owner, return navigation, NPC, Dialog, combat, tracker, retry/fallback, park/yield, and
   expiry conditions and counts. Migration may change transport plumbing only.
6. `docs/业务逻辑.md:1038-1060`: the phase loop and outside-turn phases remain; `ROUTE_TRANSFER` remains consumed by
   `NavigationService`, not FiveRing.
7. `docs/业务逻辑.md:1064-1085`: typed `TASK_TRACKER_PATHING/wuhuan` is distinct from generic attention; plain
   OPTION/STORY/attention is not FiveRing business truth.
8. Completion stories remain distinct: final-story stops all configured runs; once-story stops a one-run
   configuration but advances a multi/unlimited configuration (`baseline:2399-2436`; business logic `:1077-1082`).

## 6. Open-main-bag contract is not frozen

The current baseline boundary is real:

- DHXY `BagService.withMainBagOpen(...)` (`:145-167,205-216`) owns one exclusive open/callback/close cycle.
- `MainBagSession.countItemUpTo(...)` and `useItem(...)` (`:1394-1413`) reuse the same confirmed bag geometry.
- Current Cloud `PlayerStateService.java:556-565` intentionally retains `BagService.MainBagSession` until TURN-36.

The current turn contract cannot express that boundary:

- `TurnLocalOperation.java:3-12` has only `BAG_RETURN_ITEM` and `BAG_USE_INCENSE` for Bag.
- `CloudBagLocalServiceClient.java:73-125` exposes only return-item and closed incense-use actions; neither returns
  shoe count/page information.
- DHXY `BagLocalOperationExecutor.java:38-79` maps `BAG_USE_INCENSE` to
  `runUseIncenseMacroDirectForExclusive(...)`.
- That macro reaches `BagService.interactWithItemExclusive(...)` (`:708-745`), which opens and closes its own bag.
  It cannot be composed with a later shoe count in the same session.
- The HTTPS protocol (`:58-73,85-89,108-121,199-214`) permits five step kinds, one outcome frame, closed local
  operations, and no retained session/ledger. DHXY may execute mechanics but may not choose Cloud business order or
  fallback (`:337-366`).

Consequently, neither an unfinished TURN-40 interface nor a guessed operation such as a combined FiveRing supply
operation is a frozen contract. The parent must first freeze an exact predecessor/API route that proves all of the
following without changing the baseline:

1. one open/close bag cycle per supply check;
2. incense decision/use before shoe count, with the existing decision owner and cooldown;
3. returned typed shoe count and first page sufficient for the caller's current result;
4. one explicit post-quick-buy verification, not an automatic transport retry;
5. typed STOP/failure/uncertainty that cannot become zero shoes, false, or success;
6. one fresh action ID per explicit action, at most one raw PNG frame, and no session/owner/ledger/TTL.

If this requires protocol, validator, client, DHXY executor, DTO, or test changes, those files are outside TURN-36's
two-file code write set. The parent must add a predecessor or amend the plan before freezing TURN-36. If preserving
the current protocol requires changing bag/status/input order or moving a business decision to DHXY, that is a
separate user-approved behavior decision; none is currently recorded.

## 7. Unique future write set

Authoritative sections 17 and 19 freeze exactly:

1. Production modify only:
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`.
2. Test create only:
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingWholeTaskTurnContractTest.java`.

Any necessary production DTO must be a `private` nested type in `FiveRingTaskV2.java` (`plan:1298`). No DHXY file,
protocol/client/adapter, Service, context, phase-context, POM, resource, second test, or standalone DTO belongs to
this write set. The fixed TURN-36 card/report is process evidence, not an extra production/test path.

Plan `:1037-1043` supersedes the old section 9 `countUnit/countDelta` wording. A real caller card now needs one
unique `legacyCoverageKey`. No TURN-36 `legacyCoverageKey` is currently named anywhere in the authoritative plan.
This helper does not invent it; the parent must assign and duplicate-check it in the fixed card.

## 8. Collision delta

| Lane / active work | Latest physical write set | Direct overlap with TURN-36 production/test | Readiness effect |
|---|---|---:|---|
| External B | Owner returned at `07:25:04`; its 11-file TURN-28P bytes remain protected | 0 | No current writer collision; TURN-28P source gate still unfinished |
| External A | Parent replacement assignment only; after true-EOF claim, two DHXY TURN-28P tests plus original card | 0 | Transitive `28P -> 28 -> 27` dependency; re-snapshot after delivery |
| External C | Cloud `AutoCombatService.java`, `AutoCombatServiceTurnContractTest.java`, original TURN-34A card | 0 | Direct `34A` dependency; do not freeze against active bytes |
| External D | Waiting future TURN-34B: Cloud `TaskMaintenanceService.java`, `TaskMaintenanceTurnContractTest.java`, card | 0 | No direct TURN-36 dependency or path collision |
| Internal active helpers | Report-only TURN-38B1/B2/B3, TURN-38M companion/cohort, and TURN-45B readiness paths | 0 | Documentation-only; no Java/test ownership and no TURN-36 code permission |
| This helper | only this new TURN-36 readiness report | 0 with all code/test paths | Does not claim the future card or code files |

The target production file and future named test have no recorded TURN-36 implementation owner in the latest
parent materials. That absence is not a claim invitation. The parent must recheck ownership and hashes at the exact
future transition time.

## 9. Future named-test acceptance matrix

The only test is `FiveRingWholeTaskTurnContractTest`, profile
`HTTPS_TURN_CONTRACT_TEST_FAMILY / BC4+BASE + TASK+IMG+LS` (`plan:1643`). These are proposed exact method names for
the parent to freeze in the fixed card; no test was created or run here.

| Future named case | Required production-path evidence |
|---|---|
| `executeUsesTurnNativeContextThroughProductionPublicApi` | Directly construct production FiveRing with production turn clients/facades, bind a real turn-native `TaskExecutionContext` through the production holder, and call public `execute(context)` |
| `missingOrWrongWindowStopsBeforeActionIdAndPort` | missing holder, wrong device/window, and A-to-B context drift produce zero UUID, zero command, zero local mechanics |
| `prepareKeepsCleanupFirstAidSupplyOrder` | exact UI cleanup -> startup first aid -> supply sequence; no extra observation or fallback |
| `oneBagSupplyKeepsIncenseThenShoeCountInOneOpenClose` | one main-bag open/close; incense decision/use precedes `countItemUpTo`; typed count/page returned |
| `quickBuySuccessPerformsExactlyOneSupplyVerification` | reveal and buy actions keep baseline ordering/delays; exactly one explicit rescan; no transport replay |
| `quickBuyMissOrShortVerificationFallsBackToShopOnce` | miss and still-short branches preserve shop transition and purchase count; no extra quick-buy attempt |
| `shoeShopOwnerAndReturnFallbacksKeepBaselineOrder` | existing NPC/dialog retry count, visual-map checks, and final Navigation fallback remain exact |
| `dismountAndNavigationKeepBaselineInputOrder` | conditional Alt+C/dismount and navigation mechanics retain conditions, order, and action counts |
| `acceptFlowKeepsNavigationNpcAndDialogOwnership` | Navigation navigates only; FiveRing performs NPC phase; `ROUTE_TRANSFER` remains Navigation-owned |
| `trackerPreparedActionKeepsTurn32PhaseParkContract` | typed prepared/live tracker results, fresh exact-window fence, atomic consume/click, phase transition, park/yield, and terminal mapping remain exact |
| `plainAttentionOptionOrStoryIsNotFiveRingTruth` | generic ready/dialog signals do not consume a FiveRing action or advance phase |
| `giveItemAndDialogFallbackOrderMatchesBaseline` | GiveItem local boundary, options, white-story checks, daily-limit handling, and fallback order remain exact |
| `finalAndOnceCompletionStoriesRemainDistinct` | final template stops all runs; once template distinguishes one-run from multi/unlimited continuation |
| `combatRecoveryPreservesObservedGateAndPhase` | production AutoCombat public result controls the same recovery/phase transition; negative/uncertain signal is not success |
| `stopPauseAndUncertaintyNeverIssueASecondBusinessAction` | STOP precedence, cooperative PAUSE, failed/uncertain terminal propagation, no compensation, no hidden retry |
| `everyExplicitActionUsesFreshUuidOneCommandAndAtMostOneRawPng` | action-to-UUID/command is 1:1; multipart raw PNG only; at most one frame; explicit business retry gets a new UUID |
| `wholeTaskCreatesNoSessionOwnerLedgerTtlOrAutomaticRetry` | no retained bag/window session, task owner/lease, ledger, TTL/expiry invention, or automatic business retry |
| `activeTaskPathHasNoOldLocalMechanicsOrAuthority` | active FiveRing path has zero direct InputSequences/capture/OCR/template/window-runtime/ready-event/old transaction authority; supplemental source check cannot replace behavioral cases |
| `preTurnImplementationFailsTheWholeTaskContract` | scripted production-path expectations must fail against the pre-TURN-36 implementation because it uses direct local/runtime mechanics or omits required typed actions; a copied reducer, private reflection, constant-only test, or source-only guard is insufficient |

The test may use in-memory command/mechanics fakes but must pass through production public APIs and production
turn mappers/clients. It must assert action ordering, UUIDs, terminals, frames, state, and phase outcomes rather than
reimplement FiveRing decisions in the fixture.

Foundation `T01/T02/T03/T04`, the named test, and Cloud compile/build remain later card-acceptance gates under
sections 19.1-19.4. Existing dependency cards with source/test-source review and shared build pending can satisfy a
source-start dependency; that distinction does not waive TURN-36's own eventual named-test and build evidence.

## 10. Conditions for the parent to freeze future `READY`

The parent can evaluate a future `READY` transition only after all of these are simultaneously true on fresh bytes:

1. Direct source-start gates are `9/9`: stable parent source/test-source evidence exists for `TURN-27`, `TURN-28`,
   and `TURN-34A` in addition to the current six.
2. The TURN-28P replacement has a valid claim, two-test delivery, and parent source/test-source review; then TURN-28
   has a parent-frozen final public API and delivery; then TURN-27 is delivered against that API. No unfinished API
   is inferred from preflight prose.
3. TURN-34A's final public surface, source, and named test are stable and parent-reviewed; target constructor/caller
   expectations are re-read after External C releases the write set.
4. The open-main-bag supply boundary is frozen by an exact predecessor/API and named-test contract, or the parent
   first records an explicit user-approved behavior decision. The current two Bag operations are not enough.
5. The fixed card maps every current old FiveRing runtime/mechanics caller to an already delivered turn-native API
   or an in-file removal with proven equivalent phase/yield/wakeup semantics. Any required extra production file
   causes a plan/write-set revision before implementation.
6. The parent assigns one unique `legacyCoverageKey`; no `countDelta` or guessed count unit is used.
7. The fixed card lists exactly the two code paths in section 7, the named cases in section 9, the exact baseline
   rows, and `无已批准业务差异；按 696a12b0 基线等价迁移`.
8. Fresh status/hash/owner checks show no active writer on either TURN-36 code path and no newer dependency bytes
   than the interfaces cited by the card. Only then may a real implementation worker claim at the fixed card EOF.

## 11. Stop-work conditions for a future worker

A future worker must stop before editing and return to the parent if any of these is observed:

- any of `27/28/34A` lacks stable parent source/test-source evidence or changes after the fixed-card snapshot;
- the TURN-28 final API differs from the API frozen for TURN-27/FiveRing;
- same-main-bag incense plus shoe count still has no exact typed route;
- preserving the bag boundary would move business decisions to DHXY, change open/status/count order, or add a
  session, owner, ledger, TTL, automatic retry, extra read, park, verification, or expiry;
- implementation needs a second production file, separate DTO, context/phase-context edit, protocol/client/adapter
  edit, POM/resource edit, or second test;
- another owner or writer occupies either TURN-36 code path, or the target SHA differs from the fixed-card SHA;
- old transaction/ready-event/window-runtime behavior cannot be removed within the single target file without
  changing phase, park/yield, retry/fallback, completion, or STOP semantics;
- the named test cannot execute the production public API or would only prove copied/source/reflective logic;
- there is no parent-frozen fixed card and true-EOF claim.

## 12. Unrun gates and PRECHECK boundary

No Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture, input, or user interaction was
started. No Git add/commit/checkout/reset/restore/clean/stash/branch operation or any other Git mutation was used.
This report records dependency, caller, write-set, collision, baseline, and future-test evidence only; it does not
make the parent status decision.

PRECHECK_COMPLETE TRUE_EOF
