# CR271 TURN-27 NavigationService readiness preflight helper R1

## 0. Role and status

- Role: non-binding readiness helper for `TURN-27` only.
- Status: `PRECHECK_COMPLETE / PARENT_FREEZE_REQUIRED / NOT_A_REVIEW_DECISION`.
- This report supplies source evidence, risks, and a suggested frozen brief. It does not make a parent/reviewer judgment and does not change card state.
- No Java, test, main plan, `ACTIVE_WORK`, CR271, migration matrix, or dashboard file was edited.
- No Maven, JUnit, runtime, application, server, Task, UI, capture, input, or Git mutation was run.
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` plus only user-approved rules explicitly recorded in `docs/业务逻辑.md`; current dirty Cloud behavior is not an independent business baseline.

## 1. Materials read

1. `D:/mavenProject/DHXY/AGENTS.md` in full.
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md` in full.
3. The current CR271 block at the top of `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md`.
4. Sections 14-19 of `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`, plus the earlier TURN-27/TURN-28 definitions at lines 667-680.
5. `D:/mavenProject/DHXY/docs/业务逻辑.md` in full, especially lines 241-253, 1168-1251, and 1255-1286.
6. `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` in full.
7. Baseline source from `git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, principally:
   - `src/main/java/com/bot/dhxy/service/NavigationService.java`
   - `src/main/java/com/bot/dhxy/tools/CoordinateHelper.java`
8. Current Cloud production sources and callers, including the four planned TURN-27 production files, `TurnGameClient`, `TurnInvocationResult`, the three Task callers, `NpcClickService`, and the current old macro protocol references in both repositories.
9. Read-only `git status` for both repositories and the current fixed reports for active TURN-22/TURN-33 write sets.

## 2. Authoritative dependency state

The current registry in the authoritative plan says:

- `TURN-28`: `PLANNED`, starts after `TURN-23 + TURN-24 + TURN-26` (`plan:1055`).
- `TURN-27`: `PLANNED`, starts after `TURN-15 + TURN-18 + TURN-23 + TURN-24 + TURN-26 + TURN-28` (`plan:1056`).
- Wave order explicitly says `TURN-28` waits for 23/24/26 and `TURN-27` is last, after 28 (`plan:1332`).

Current plan evidence also records source/test-source completion for TURN-15, TURN-18, TURN-23, TURN-24A, and TURN-26, with build cohort work still pending. TURN-24 is a split parent whose first subcard `TURN-24A` owns the only count unit and integrates the remaining public methods (`plan:628-639`). The parent should explicitly record whether this satisfies TURN-27's symbolic `TURN-24` start dependency before claim; the helper must not infer that state transition.

TURN-28 has no fixed implementation card or `NpcClickTurnContractTest` in the current tree. Its three production paths are dirty (`NpcClickService.java` untracked; `ObjectiveTextRecognizer.java` and `SmartClickRecognizer.java` modified), so TURN-27 must not freeze an assumed final NpcClick/recognizer API before TURN-28's actual source delivery is reviewed.

## 3. Exact TURN-27 write set

### 3.1 Production write set

The later section 17 registry overrides the earlier generic wording and fixes exactly four Cloud production files (`plan:1197-1199`):

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/MiniMapPointResolver.java`
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NavigationRoutePlanResolver.java`

No fifth production Java/model/DTO/assembly/port file is presently authorized.

### 3.2 Test write set

The sole test write set is (`plan:1507-1510`, `plan:1532`):

1. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`

The class does not currently exist. The plan grants no additional fixture/resource file; the test should use existing fixtures or in-test deterministic raw-PNG bytes/fakes unless the parent first amends the card.

### 3.3 Explicitly read-only/out of scope

- All DHXY production/test files, including old local-macro handler/codec/types.
- Cloud `NpcClickService.java`, `ObjectiveTextRecognizer.java`, `SmartClickRecognizer.java`, and `ImageAlgorithms.java` (TURN-28 ownership).
- Cloud `DecisionEngine.java`, protocol models, `TurnGameClient`, `CloudTurnActionFactory`, command port/exchange, `DialogService`, `PlayerStateService`, `BattleRadarService`, `MemoryService`, all Task callers, and all activation files.
- Main plan, CR271, `ACTIVE_WORK`, matrix, dashboard, and other reports.

If implementation proves that one of these files must change, the worker must stop at the boundary and ask the parent to amend the exact write set; it must not create a convenience DTO or modify a caller silently.

## 4. Current tree state and preservation evidence

At precheck time:

- Cloud branch: `navigation-migration`; 28 dirty/untracked status entries.
- DHXY branch: `thin-client-design`; 80 dirty/untracked status entries.
- All four TURN-27 production files are currently untracked in the Cloud repository and therefore contain protected workspace work that must be edited in place, never recreated from a baseline blob:

| File | Lines | Current SHA-256 |
|---|---:|---|
| `NavigationService.java` | 2800 | `66d5480722cf07c643bdabb9e53d84ffa203fd6184b8dfcae6deed313ed4aff2` |
| `CloudMiniMapCoordinateReadability.java` | 33 | `cf782cd0c0970e6cf2bf14fd997375719b8a0bcfb3ae39633c496a9f9d9d19ac` |
| `MiniMapPointResolver.java` | 392 | `27049ff972324cd7041dd397afc9c748a2280bb571c016dd71ad1d98d46b18d8` |
| `NavigationRoutePlanResolver.java` | 347 | `353d98628dd32921b80692f8d467d1d5e2e8965d96b739fa3812b97b97160fb9` |

These hashes are orientation evidence only, not an ownership lock or reviewer verdict.

The active TURN-22 and TURN-33 implementation write sets are file-disjoint from TURN-27:

- TURN-22: `TeamReturnService.java`, `CloudTeamReturnPortAssembly.java`, `TeamReturnTurnContractTest.java`.
- TURN-33: `SummonSkillService.java`, `CloudSummonSkillWholePassCapability.java`, `CloudTaskExclusiveInteractionAuthority.java`, `SummonSkillTurnContractTest.java`.

No current implementation owner is recorded for the four TURN-27 production files. TURN-28 is nevertheless a semantic/API predecessor and must finish first even though its exact filenames do not overlap TURN-27.

## 5. Current source call graph

### 5.1 `NavigationService` is not on the minimum HTTPS turn path yet

Current `NavigationService.java:3-51` imports direct tracker, input, OCR, coordinate, runtime, and window collaborators. Its final fields at lines 174-195 include direct `GameClientTracker`, `InputProvider`, `InputSequences`, `GameTextLineOcrService`, `MiniMapCoordinateReader`, `GameStateUtil`, `CoordinateHelper`, old `CloudUiCleanerPort`, `DialogService`, `NpcClickService`, window runtime/event objects, and `MemoryService`.

There is no `TurnGameClient` reference in current `NavigationService`. By contrast, the shared turn client already guarantees one UUID/one command/no retry (`TurnGameClient.java:20-25`, `95-125`, `161-179`) and exposes exact-context bound views (`TurnGameClient.java:64-84`). TURN-27 should consume that existing boundary rather than add another client, transport, retry wrapper, or action-ID generator.

### 5.2 Current-map navigation is currently the wrong ownership shape

Current `NavigationService.java:524-615`:

1. Requires an active task context.
2. Builds `NavigateInCurrentMapMacroCommand` (`549-563`).
3. Calls `executeLocalMacro(... LocalMacroKind.NAVIGATE_IN_CURRENT_MAP ...)` with a 120-second transport ceiling (`564-569`).
4. Maps the local macro terminal to `NavigationResult` (`570`, `584-615`).

The comment at `543-547` explicitly says the complete 60-second candidate/click/confirmation/retry/cleanup business loop runs in DHXY and Cloud does none of it. This directly conflicts with TURN-27's target boundary: Cloud owns map OCR, coordinate math, route ladder, and fallback; DHXY performs only the ordered mechanics supplied by Cloud.

### 5.3 Existing decision files are not integrated into the Service

- `CloudMiniMapCoordinateReadability` has no current production caller. It correctly expresses template-first then OCR fallback on an already decoded image (`23-31`), but is not wired into Navigation.
- `MiniMapPointResolver` is currently reached only through `DecisionEngine` operations at `DecisionEngine.java:2351-2355`, plus the readability fallback at `DecisionEngine.java:2431`.
- `NavigationRoutePlanResolver` is currently reached only through `DecisionEngine.java:322`.
- `NavigationService` does not call either resolver. Thus TURN-27 is a real cutover/integration, not a rename-only card.

The current resolver entry signatures are referenced by read-only `DecisionEngine.java` and therefore must remain compile-compatible unless the parent amends the write set:

- `NavigationRoutePlanResolver.decideNextStep(JsonNode)`
- `MiniMapPointResolver.resolveMinimapClick(JsonNode)`
- `MiniMapPointResolver.checkCoordinatePlausible(JsonNode)`
- `MiniMapPointResolver.resolveApproachCoordinate(JsonNode)`
- `MiniMapPointResolver.ocrFallbackCoordinatePlausible(...)`

### 5.4 TURN-28 is an API dependency, not permission to merge NPC clicking into navigation

Current and baseline business paths keep navigation and NPC clicking separate:

- `NavigationService.navigateToNPC` performs `navigateToMap` then `navigateInCurrentMap`, returns immediately on either non-success, and finally returns coordinate arrival (`current NavigationService.java:216-260`; baseline `NavigationService.java:204-248`). It does not click the NPC.
- Current Xiuluo, Wubei, and FiveRing Task callers invoke `navigationService.navigateToNPC(...)` and later invoke `npcClickService.clickNpcSmart(...)` in their own phase/business branch. Examples include `XiuluoTaskV2.java:1473` then `1531`, and `WubeiTask.java:1407` then `1427`.
- The only `npcClickService` call inside Navigation is in private deprecated retained comparison code `navigateToLingShouVillageViaZhangWen` (`current NavigationService.java:622-690`; baseline `700-767`). The method's own JavaDoc says production must use the direct yellow route, and no production call to this private method exists.

Therefore the safest parent brief is: TURN-27 waits for TURN-28 so the final NpcClick public API and recognizer ownership are stable, but TURN-27 must not invent an active Navigation-to-NpcClick call or collapse Task `NAVIGATE_TO_TARGET` and `CLICK_TARGET_NPC` phases. The plan's `route/NPC/dialog/direct-input` test wording should be frozen as ordering/separation coverage, not interpreted as authorization to make `navigateToNPC` physically click an NPC.

### 5.5 Direct dependency on TURN-28-owned recognizer code

`MiniMapPointResolver` calls `ObjectiveTextRecognizer.mapTransform(...)` at lines 61, 118, 145, and 183. `ObjectiveTextRecognizer.java` is TURN-28-owned and currently modified. This direct API coupling is an additional reason to review TURN-28's actual final source before freezing TURN-27.

## 6. `696a12b0` business order that TURN-27 must preserve

### 6.1 Public `navigateToNPC`

Baseline order is exact:

1. Validate non-null request and complete target map/X/Y.
2. Stop checkpoint.
3. Call `navigateToMap` with `source + ":map"`.
4. Any non-success returns immediately; no current-map mechanics run.
5. Stop checkpoint.
6. Call `navigateInCurrentMap` with `source + ":currentMap"`.
7. Any non-success returns immediately.
8. Stop checkpoint.
9. Return `ARRIVED` for the NPC coordinate without dialog cleanup and without NPC click.

This is visible in current `NavigationService.java:216-260` and the matching baseline method.

### 6.2 Cross-map route ladder

Baseline `NavigationService.java:260-471` preserves this order:

1. Consume a prepared route-transfer dialog first when exact fresh evidence exists.
2. Inspect the current window pathing snapshot before any fresh map read.
3. If a compatible active route exists, preserve the pathing/dialog-preparing branches and clear only matching stale preparation as the baseline does.
4. If no usable snapshot exists, perform the fresh current-map confirmation; a stale cached map cannot produce `ARRIVED`.
5. If already on target, clear matching route preparation and return `ARRIVED`.
6. Clear route preparation only when it belongs to a different target.
7. Run the route-dialog gate before opening the world map.
8. Submit the world-map search route.
9. `DIALOG_PREPARING` and `PATHING_STARTED` return immediately with the same nested-intent ownership rules.
10. A failed first route submission returns `MAP_NOT_REACHED` quickly; Navigation does not add a hidden blocking arrival loop.

The current `NavigationRoutePlanResolver.java:46-239` encodes much of this ladder, but its comments and directives still assume local observation booleans, a local compound world-map operation, and an older shell/terminal gate. Those ownership assumptions must be rewritten for closed HTTPS actions while preserving the stage order.

### 6.3 World-map search fallback order

Baseline `NavigationService.java:1433-1516`:

1. At most two business attempts.
2. Each attempt prepares the route UI first; prepare failure returns immediately and does not inject cleanup.
3. Yellow-destination mode tries remembered yellow route first, then fresh yellow OCR/coordinate path. Legacy green mode remains behind its explicit switch.
4. `CLICKED` returns success.
5. Only `WRONG_DESTINATION` on attempt 1 performs route-panel cleanup, waits 250 ms, and enters attempt 2.
6. `NOT_FOUND`, attempt-2 mismatch, or other failure closes the route panel once and returns failure.

Under the HTTPS turn protocol, attempt 2 is a Cloud-owned business fallback and must use a fresh action UUID. It is not a transport retry. A failed/uncertain command must not be silently re-executed as attempt 2.

### 6.4 Current-map loop and candidate order

Baseline `NavigationService.java:512-693`:

1. Validate target coordinates.
2. Enter a 60-second business loop; stop checkpoint at each loop boundary.
3. Synced `IN_COMBAT` returns interrupted.
4. A current cached coordinate already within tolerance returns arrived.
5. Candidate order is original target first, then edge-biased offsets, then deterministic logical rings up to radius 10. Already attempted logical coordinates are skipped; no duplicate point may be clicked.
6. The Xiuluo start-exit fire-and-handoff special case performs its one intended click/close handoff; if it fails, it does not enter alternate candidate retries.
7. Normal current-map click records a pre-click location, opens/checks the mini-map as the baseline specifies, clicks once, then confirms movement first by pixel-edge evidence and then by coordinate change.
8. `PATHING_STARTED` normally registers the exact pathing intent, closes the mini-map, and returns.
9. `keepTurnOnCurrentMapPathing=true` waits up to the baseline short deadline, polling at 250 ms; arrival returns, `STOPPED_AWAY` or deadline exhaustion advances to the next candidate.
10. A confirmed `NO_PATHING` advances the candidate count; `INCONCLUSIVE` returns non-arrived immediately.
11. Business candidate fallback waits the baseline 200 ms before the next iteration.
12. Timeout returns point-not-reached. Non-pathing exits perform the existing mini-map cleanup; no extra retry/fallback is added.

Baseline candidate math in `CoordinateHelper.java:178-330` is screen-absolute after applying the current window origin. The Cloud resolver may return window-relative candidate coordinates only if the action assembly converts them using the exact current non-scaled `windowRect.left/top` and tests that conversion. It must never assume `(0,0)` or scale coordinates.

### 6.5 Task-level order from `docs/业务逻辑.md`

- Verified return-home map/coordinate evidence is not a time-expiring cache (`业务逻辑.md:241-253`). TURN-27 must not add a new age/TTL gate that forces a redundant map read after the Task supplied this verified fact.
- Xiuluo tracker shortcut prepath is only a fire-and-handoff hint and does not own target navigation or NPC click (`业务逻辑.md:1183-1214`).
- Non-shortcut objective parsing may overlap the prepath, but once parsed the Task enters `NAVIGATE_TO_TARGET`; target navigation, NPC click, and battle confirmation remain separate later phases (`业务逻辑.md:1218-1251`).
- `NAVIGATE_TO_TARGET` failure first performs cleanup and objective reread/recovery according to the Task's route mode (`业务逻辑.md:1281`).
- `CLICK_TARGET_NPC` has its own ordered 看打/wild-monster/OCR/direct-combat/mount/objective-reread fallbacks (`业务逻辑.md:1282`). Navigation must not absorb or pre-run these fallbacks.

## 7. Minimum HTTPS turn mapping

The parent-frozen implementation should map the baseline to the existing protocol as follows:

1. Every physical/observation invocation uses the exact current `TurnInvocationContext` and a bound `TurnGameClient` before metadata read or command submission.
2. One explicit Cloud business action creates one UUID and one command. No local auto retry, no client-side business loop, no second command hidden in a helper.
3. Only `CAPTURE`, `MATCH_TEMPLATE`, `INPUT`, `WAIT`, and `LOCAL_SERVICE` steps are legal (`protocol:56-66`).
4. Default OCR/template/geometry decisions use a raw PNG returned to Cloud. Local matching occurs only when the action explicitly requests `MATCH_TEMPLATE` (`protocol:65-66`, `218-232`).
5. A turn returns at most one image frame, as raw multipart PNG, never Base64 (`protocol:68-73`). Action decomposition must respect this one-frame limit.
6. Mouse coordinates are exact screen-absolute values derived from current non-scaled window geometry. Keyboard and capture use background-capable mechanics; mouse remains the foreground mechanic (`protocol:75-80`, `199-207`, `261-265`, `323-330`).
7. Post-action evidence belongs in the same payload whenever the next decision needs the resulting frame (`protocol:234-247`).
8. A failed step stops all remaining steps and returns typed failure plus evidence; Cloud alone chooses the next explicit action (`protocol:272-286`). STOPPED and duplicate/uncertain remain non-success terminals.
9. X2 is the already closed `LOCAL_SERVICE` operation `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`, with nonblank source (`protocol:83-106`). Where the baseline X2 cleanup belongs to a larger route action, assemble that typed local-Service step into the same `TurnGameClient.execute` action. Do not call the one-operation convenience client and then issue a second command, and do not reopen the old macro wire.
10. Existing in-memory route/pathing state may be read according to the baseline, but TURN-27 must not create an owner, permit, session, ledger, compaction layer, durable workflow, new business TTL, or transport auto retry.

## 8. Suggested parent-frozen production brief

### 8.1 Public API and ownership

1. Preserve existing public `NavigationService` method signatures and `NavigationResult` semantics so TURN-35/36/37 caller cards can wire later without Task edits in TURN-27.
2. Preserve `navigateToNPC = navigateToMap -> navigateInCurrentMap -> coordinate-arrived`; do not click the NPC and do not consume Task dialog business.
3. Use the current Task execution context's existing `TurnGameClient`; bind exact context once for the invocation/leg and re-check exact metadata at each command boundary as required by existing turn contracts.
4. Remove the active `executeLocalMacro(NAVIGATE_IN_CURRENT_MAP)` path from Cloud Navigation. Cloud owns the loop and sends one closed action at a time.
5. Direct tracker/input/capture/OCR/template operations in production-reachable Navigation paths must be replaced by `TurnGameClient` actions and Cloud-side image/decision code. Read-only in-memory Task/window/pathing facts may remain only where the baseline requires them.

### 8.2 Decision files

1. `CloudMiniMapCoordinateReadability`: keep template-first then OCR fallback on the same decoded coordinate strip; no capture/file ownership and no second frame.
2. `MiniMapPointResolver`: preserve 696 candidate ordering, transform math, jitter/clamp, unknown-map plausibility, cave-map approach exception, and attempted-point de-dup. Do not let resolver-generated decision IDs become physical action IDs.
3. Remove or neutralize old contract-only expiry fields that are not traceable to a 696 rule. Current `MAX_OBSERVATION_AGE_MS=10_000`, `navigationDeadlineMs`, and `batchExpiresAtMs` (`MiniMapPointResolver.java:37-42`, `56-68`, `95-102`) must not silently create a new business TTL/durable batch. Baseline pathing-snapshot freshness is a separate existing rule and should remain exactly where the baseline uses it.
4. `NavigationRoutePlanResolver`: preserve the six-stage route ladder but replace the obsolete assumptions described in its lines 10-22 (local helper booleans, local compound world-map retry, client terminal re-verification) with Cloud-owned analysis and explicit closed actions.
5. Preserve the resolver signatures used by read-only `DecisionEngine.java`, or obtain a parent write-set amendment before changing them.

### 8.3 Closed action boundaries

1. Each observation action contains the baseline-required input/waits plus one post-action `CAPTURE` when useful; Cloud analyzes that returned frame before sending the next action.
2. Each click uses one closed `CLICK_LEFT` input step at the exact supplied coordinate; the existing DHXY executor remains responsible for atomic physical move+click mechanics. Do not send a separate speculative move command.
3. X2 cleanup uses the typed `LOCAL_SERVICE` step and stays in the same ordered action when baseline order requires adjacent input/wait mechanics.
4. World-map attempt 2, current-map next candidate, and any wider evidence request are explicit Cloud business fallbacks with new UUIDs. They occur only after a confirmed prior outcome that permits that branch.
5. Any command-level uncertain result is fatal/non-success for the current method invocation; it is never converted to false and then retried.

### 8.4 Scoped legacy removal

The plan's `旧 NAVIGATE_IN_CURRENT_MAP macro 零引用` cannot literally mean repository-wide string deletion within TURN-27's current write set:

- Cloud has 8 Java files containing `NAVIGATE_IN_CURRENT_MAP`/`NavigateInCurrentMapMacro`; only `NavigationService.java` is in TURN-27.
- DHXY has 8 additional handler/codec/type files containing it; none is in TURN-27.

Recommended frozen meaning for this card: **zero production-reachable invocation from the new Cloud Navigation path**, with no `executeLocalMacro(LocalMacroKind.NAVIGATE_IN_CURRENT_MAP, ...)` in `NavigationService`. Old protocol declarations/handlers stay untouched and inert until an explicitly scoped cleanup/activation card removes them. If the parent instead requires repository-wide zero textual references, the exact write set must be amended before claim.

## 9. Suggested `NavigationTurnContractTest` acceptance matrix

Section 19.4 makes `BC4+BASE` mandatory for every Cloud business card, with TURN-27 adding `IMG+LX` (`plan:1507-1510`, `1532`). The single named test should directly exercise production `NavigationService` and the production decision files through a fake/scripted exact-bound `TurnGameClient`; it must not test a copied mapper.

### 9.1 Mandatory common cases

1. Script `COMPLETED`, mechanical `FAILED`, `STOPPED`, and command/outcome uncertain for each representative action boundary.
2. Assert exact device/window/HWND/process context before command creation; a mismatched current context produces zero UUID and zero command.
3. Assert one UUID/one command per explicit action and no hidden command/retry after failure, STOPPED, or uncertainty.
4. Assert raw PNG bytes plus SHA/dimensions/absolute region/source-step metadata, not Base64; one command has at most one frame.
5. Assert exact step index order, the failed step, all following `NOT_RUN`, and full-window failure-evidence replacement when requested.

### 9.2 Navigation-specific cases

1. `navigateToNPC` calls map navigation before current-map navigation; a non-success map result produces zero current-map actions.
2. Successful map and current-map legs return coordinate arrival with zero `NpcClickService` call and zero dialog cleanup.
3. Prepared route-dialog priority precedes map snapshot/fresh map/world-map actions; click failure short-circuits exactly.
4. Fresh map confirmation is required before `ARRIVED`; caller-supplied verified map evidence does not gain a new TTL.
5. Stale route preparation is cleared only for a different target; matching active pathing/dialog-preparing branches retain baseline terminals.
6. World-map prepare failure performs no invented cleanup action.
7. Attempt-1 `WRONG_DESTINATION` produces exactly one X2 cleanup/wait branch and one new attempt-2 action ID; `NOT_FOUND`, STOPPED, uncertain, and attempt-2 mismatch do not produce an extra attempt.
8. X2 is a typed `UI_CLOSE_MAP_SEARCH_INPUT_BY_X2` local step in the frozen ordered action, has nonblank source, and causes no nested queue/second command/old macro call.
9. Current-map candidate sequence is original, edge-biased offsets, then deterministic rings; duplicate logical points are skipped.
10. Normal candidate click keeps baseline Alt+1/open settle/click settle/movement-confirm order. `NO_PATHING` permits the next business candidate; `INCONCLUSIVE`, STOPPED, and uncertain short-circuit.
11. Xiuluo start-exit fire-and-handoff failure does not enter alternate candidate retries.
12. Keep-turn pathing preserves baseline arrival/STOPPED_AWAY/deadline behavior and 250 ms poll order without adding a new TTL or transport retry.
13. Template-first/OCR-fallback readability and route-result OCR/coordinate decisions use the same uploaded frame and never trigger a second capture implicitly.
14. No production-reachable `executeLocalMacro(NAVIGATE_IN_CURRENT_MAP)` call remains in `NavigationService`.
15. The deprecated Zhang Wen comparison method is not made runnable and does not create an extra Navigation-to-NpcClick chain.

The named test may assert active-path source absence inside the four production files, but a repository-wide string-zero assertion would conflict with the fixed write set described in section 8.4.

## 10. Risk register

### R1 - TURN-28 contract not yet frozen (high)

`NpcClickService`, `ObjectiveTextRecognizer`, and `SmartClickRecognizer` are the TURN-28 write set and are currently dirty. `MiniMapPointResolver` directly calls the recognizer, while Navigation retains a deprecated NpcClick reference. Freezing TURN-27 first risks immediate API rework or an accidental business merge.

**Suggested control:** review TURN-28 source/test source first, then freeze TURN-27 against its actual final public API and recognizer visibility.

### R2 - Literal macro zero-reference requirement exceeds the card (high)

Sixteen old macro declaration/handler files across both repositories are outside TURN-27. A global source guard would force out-of-scope edits.

**Suggested control:** freeze active-path zero invocation for TURN-27, or create/amend a later cleanup card with explicit dual-repository write set.

### R3 - Existing resolver encodes obsolete ownership (high)

`NavigationRoutePlanResolver.java:10-22` explicitly keeps observations and two-attempt compound locally, contrary to the minimum HTTPS turn. Reusing it unchanged would preserve the wrong architecture even if the Service no longer calls the macro.

**Suggested control:** rewrite ownership comments and decision inputs/outputs while preserving 696 stage order and read-only caller signatures.

### R4 - Unapproved expiry/batch semantics (high)

`MiniMapPointResolver` contains a 10-second observation age rejection and batch expiry fields from an older contract. These are not the same as the baseline pathing snapshot age rule and can reject otherwise valid Cloud decisions.

**Suggested control:** require an exact `696a12b0`/business-doc citation for every retained expiry field; otherwise remove it within this file without replacing it with another TTL.

### R5 - Four production files are untracked protected work (high)

Baseline checkout/copy or file recreation would erase current workspace work.

**Suggested control:** claimant records current hashes/status, reads current content first, and edits in place with no reset/checkout/clean.

### R6 - Read-only `DecisionEngine` compile coupling (medium)

Both resolver classes are called from `DecisionEngine.java`, which is outside the write set. Signature/package visibility changes can break compile even if the new Navigation path works.

**Suggested control:** retain referenced entry signatures or amend the write set before implementation.

### R7 - One-frame protocol versus multi-observation legacy helpers (medium)

Several old helpers capture/check multiple times inside one local callback. Translating them naively into one turn can violate the one-frame validator; translating each helper into an automatic local loop violates Cloud ownership.

**Suggested control:** parent brief lists each observation boundary; each explicit action returns at most one frame, and Cloud chooses the next action.

### R8 - Extra DTO temptation (medium)

The four-file production write set contains no new public navigation action/result model file.

**Suggested control:** reuse existing protocol models and keep truly private decision records at the bottom of an allowed file. If a cross-package public type is genuinely needed, amend the plan rather than create a fifth file.

### R9 - Route memory/state outside write set (medium)

Current Navigation reads/writes existing `MemoryService`, window pathing snapshot/intent, pending route memory, and dialog-preparation state. Those collaborators are read-only for TURN-27.

**Suggested control:** preserve their current baseline-visible contracts without adding persistence, compaction, session, ledger, or TTL and without changing their files.

### R10 - Action-ID confusion (medium)

Current `MiniMapPointResolver` generates UUIDs for candidate/decision records. Physical action IDs must remain exclusively owned by `TurnGameClient` once per actual command.

**Suggested control:** tests count command UUIDs independently from candidate IDs and prove that skipped candidates create no action UUID.

### R11 - Post-696 dirty behavior is not automatically authoritative (medium)

Current Navigation contains later route/pathing work and comments. Some may be explicitly approved and documented; others may be migration artifacts.

**Suggested control:** parent brief maps each retained difference to a user-approved row/CR. Anything without that evidence follows `696a12b0`; do not silently preserve current dirty behavior or silently revert an explicitly approved rule.

## 11. Write-set conflict matrix

| Other lane/card | File overlap with TURN-27 | Semantic/API overlap | Precheck recommendation |
|---|---|---|---|
| TURN-22 | None | None material | May run in parallel if still active |
| TURN-33 | None | None material | May run in parallel if still active |
| TURN-28 | No exact TURN-27 file overlap | Direct: NpcClick final API and `ObjectiveTextRecognizer` used by `MiniMapPointResolver` | Must complete source gate before TURN-27 claim |
| TURN-24A/parent TURN-24 | None | BattleRadar facts consumed by current Navigation | Parent records symbolic dependency satisfaction explicitly |
| TURN-23 / TURN-26 | None | Player-state/dialog APIs consumed by Navigation | Use reviewed APIs read-only; no back-edit |
| TURN-35/36/37 | Task files only | Future real callers consume Navigation/NpcClick | Must wait; no caller edits in TURN-27 |
| Old DHXY macro cleanup | Many out-of-scope DHXY protocol/handler files | Needed only for literal repository-wide deletion | Separate/amended cleanup scope, not concurrent hidden work |

## 12. Parent freeze checklist

Before issuing a TURN-27 implementation claim, the parent should record all of the following in the fixed card:

1. TURN-28's actual final source/test-source public API and recognizer visibility.
2. Whether TURN-24A satisfies the symbolic TURN-24 start dependency.
3. Exact four-file production write set and one-file test write set from section 3.
4. Preservation of `navigateToNPC` navigation-only semantics and separate Task `CLICK_TARGET_NPC` phase.
5. The exact `696a12b0` route ladder, two-attempt world-map rule, current-map candidate/keep-turn/fire-and-handoff order, waits, and terminal mappings.
6. X2 as typed `LOCAL_SERVICE` in the same ordered action where required, with no nested queue or second command.
7. “Old macro zero reference” meaning: production-reachable invocation zero for this card, unless the write set is explicitly expanded.
8. No direct tracker/input/capture/OCR business path in Cloud Navigation; all mechanics through the existing exact-bound `TurnGameClient`.
9. One action/one UUID/one command, one frame maximum, raw PNG, failure short-circuit, and no auto retry/session/ledger/TTL/durable workflow.
10. Resolver compatibility with read-only `DecisionEngine`, or an explicit write-set amendment.
11. The full `BC4+BASE+IMG+LX` matrix in the sole `NavigationTurnContractTest`.
12. Claimant must preserve all current untracked content and be the sole writer for the five implementation/test paths.

## 13. PRECHECK summary

- The exact implementation/test write set is known and currently file-disjoint from active TURN-22/TURN-33 writers.
- The `696a12b0` navigation order is recoverable and can be expressed as explicit minimum HTTPS turns without adding another transport or workflow layer.
- TURN-27 is not ready to be claimed until TURN-28's actual final source contract is available and the parent resolves the two scope ambiguities: navigation/NPC separation and active-path versus repository-wide old-macro zero references.
- Once those points are frozen, the implementation can remain inside the four production files plus the single named test; no new Java file is presently justified.

<!-- TRUE_EOF: TURN-27 readiness-preflight-helper-r1 PRECHECK_COMPLETE -->
