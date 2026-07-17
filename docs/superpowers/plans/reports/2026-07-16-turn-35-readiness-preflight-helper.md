# TURN-35 Wubei whole-task HTTPS turn readiness preflight

## REPLACEMENT CLAIMED - 2026-07-16T03:29:30-04:00

- Agent identity: `Codex / TURN-35 readiness replacement helper / current task`.
- Role boundary: non-binding readiness helper only; not a reviewer and not authorized to write `APPROVED` or
  `BLOCKED`.
- Previous helper session for this card is `not_found`. Any existing workspace content remains protected; this
  replacement does not assume or reuse an unrecorded conclusion.
- Unique and exclusive write set:
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-35-readiness-preflight-helper.md`.
- Both repositories and every other file are read-only. No Java/config/test/plan/`ACTIVE_WORK`/CR/matrix/dashboard
  edit, Git mutation, Maven/JUnit, runtime/application/server/Task/poller/UI/capture/input is authorized.
- Baseline and architecture gates: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; Cloud owns decisions, OCR and
  business ordering; DHXY executes only explicit HTTPS-turn mechanics and the four approved local Services;
  no automatic retry/session/ledger/TTL/durable workflow.

## Identity correction - 2026-07-16

- Platform-confirmed agent id: `019f69d0-a377-7232-8d72-390ac1d8bc96`.
- Platform-confirmed nickname: `Pauli`.
- This identity correction supersedes the generic agent identity recorded in `REPLACEMENT CLAIMED`; the role and
  unique write-set boundaries remain unchanged.

## Precheck scope and evidence

This replacement pass completed read-only inspection of:

- D:/mavenProject/DHXY/AGENTS.md.
- docs/DHXY_CONTEXT.md and the CR271 top section of docs/ACTIVE_WORK.md.
- Authority plan docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md,
  especially sections 14 through 19.
- HTTPS-turn protocol design/specification and docs/业务逻辑.md.
- Both repository status snapshots, every direct TURN-35 dependency report, the active TURN-28P and TURN-33
  reports, and their actual production/test sources.
- Current Cloud WubeiTask, WubeiPhase, turn protocol/client APIs, TaskTrackerPanelService, DialogService,
  ReturnItemPrescanService and CloudBagLocalServiceClient.
- Baseline WubeiTask at 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7.

No Maven/JUnit/runtime/input/capture path was invoked. No file other than this fixed report was modified.

## Repository snapshot

- DHXY branch: thin-client-design. The worktree contains extensive existing dirty/untracked content.
- Cloud branch: navigation-migration. The worktree contains extensive existing dirty/untracked content.
- No reset/restore/checkout/clean/stage/commit or other Git mutation was performed.
- Current Cloud WubeiTask:
  - path: D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java
  - lines: 4329
  - SHA-256: DFDE0AD08900F2553088A7D304556A2B5A754C4980305199DB7B9C9035B720D7
- A direct in-memory comparison with baseline shows that the current Cloud file's intended business delta is
  TURN-31's exact TaskExecutionContext binding for the asynchronous post-accept tracker read. That delta adds
  TaskExecutionContextHolder and binds taskTrackerPanelService.readWubeiTrackerPanel to the exact context;
  it does not itself complete the whole-task turn cutover.
- The planned TURN-35 named test and implementation card do not yet exist:
  - D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java
  - D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-35.md

## Authority-plan contract

- Card: TURN-35, one COUNT card with one unique countUnit assigned only when the implementation card is claimed.
- Start dependency set:
  S=TURN-13C+TURN-14+TURN-15+TURN-21+TURN-22+TURN-23+TURN-26+TURN-27+TURN-28+TURN-31+TURN-34A+TURN-34B.
- Production write set: only Cloud task/wubei/WubeiTask.java. Any necessary DTO must be a private nested type
  in that same file.
- Test write set: only task/wubei/WubeiWholeTaskTurnContractTest.java.
- Acceptance profiles: default BC4+BASE plus TASK+IMG+LS.
- Completion boundary: all 14 Wubei business states remain baseline-equivalent; all local effects use an
  explicit HTTPS turn or one of the four permanent local Services; no old remote port remains.
- Whole-task approval additionally depends on the applicable TURN-T01/T02/T03/T04 foundation test-debt gates.
  TURN-35's own named test cannot substitute for those four gates.

## Dependency gate audit

| Dependency | Latest durable evidence | TURN-35 readiness consequence |
|---|---|---|
| TURN-13C | Parent source/test-source review P0/P1/P2=0/0/0; named test/compile cohort still pending shared build debt. | Source API can be inspected, but card-level build evidence remains pending. |
| TURN-14 | Repair #1 parent re-review P0/P1/P2=0/0/0; source/test source passed, named test/build pending. | Bag typed operations are available as source, subject to the API gap below. |
| TURN-15 | Repair #1 parent re-review P0/P1/P2=0/0/0; source/test source passed, named test/build pending. | UI cleaner typed boundary is available as source. |
| TURN-21 | Repair #1 parent re-review P0/P1/P2=0/0/0; source/test source passed, build pending. | Player-state typed boundary is available as source. |
| TURN-22 | Parent review P0/P1/P2=0/1/0. Baseline 150ms click delay plus 500ms same-queue hold is missing from the real executor. | Dependency is not yet source-complete; repair waits for TURN-28P generic click timing. |
| TURN-23 | Repair #1 parent re-review P0/P1/P2=0/0/0; source/test source passed, named test/build pending. | Common-box typed boundary is available as source. |
| TURN-26 | Parent source/test-source review P0/P1/P2=0/0/0; named test/build pending. | Dialog typed OCR/decision source is available; Task still owns explicit prepared click. |
| TURN-27 | Readiness PRECHECK_COMPLETE only. Implementation must wait for TURN-28 final API and parent scope freeze. | Navigation dependency has not been implemented. |
| TURN-28 | Readiness PRECHECK_COMPLETE only. Alt+A/Alt+C and Ctrl probe require TURN-28P. | NpcClick dependency has not been implemented. |
| TURN-31 | Parent source/test-source review P0/P1/P2=0/0/0; build pending. | Exact-context tracker caller delta is present in WubeiTask and must be retained. |
| TURN-34A | Readiness PRECHECK_COMPLETE only; waits for TURN-33 and other dependency evidence. | AutoCombat whole-service implementation is not final. |
| TURN-34B | Readiness PRECHECK_COMPLETE only; waits for TURN-22 and TURN-33. | TaskMaintenance whole-service implementation is not final. |

Indirect shared gates at this report's true EOF:

- TURN-28P replacement Worker Locke, agent 019f69ce-9359-71a1-8402-cb7ee7d34404, has only a durable
  replacement claim. No SOURCE+TEST DELIVERED true EOF exists yet.
- TURN-33 replacement Worker Faraday, agent 019f69ce-d84c-7a11-a832-3ce77f8f739a, has only a durable
  replacement claim. No SOURCE+TEST DELIVERED true EOF exists yet.
- Therefore TURN-22 repair, TURN-27, TURN-28, TURN-34A and TURN-34B are not yet a stable implementation surface
  for TURN-35. The parent should re-audit these final APIs and hashes before opening the TURN-35 count card.

## Real caller and activation chain

Current source-level chain:

1. WubeiTask is a prototype Spring component implementing GameTask.
2. execute(TaskExecutionContext) resolves the exact task context, performs startup boundaries, loops configured
   rounds and enters runRoundPhases.
3. runRoundPhases serializes one business phase at a time through TaskTransactionRunner.
4. runPhase dispatches the 14 business states to typed Services and, today, several direct local mechanics.
5. Typed Services use TaskExecutionContext.getTurnGameClient or their own bound TurnGameClient assembly to
   submit one HTTPS JSON action and consume a typed closed result.
6. The DHXY turn executor performs only the ordered mechanics and returns typed outcome plus optional raw PNG.

There is no current Cloud production host/API/turn source reference that constructs or starts WubeiTask. The
real runtime activation/start/stop path remains a later TURN-40 responsibility. TURN-35 can close a real
source-level task-to-turn chain and its test, but it must not claim current server/runtime activation and must
not edit host/application/config/startup files.

## Fourteen-state baseline

WubeiPhase is byte-equivalent to the baseline and contains 14 business states plus two non-success terminals:

1. HOT_START_DETECT
2. ROUTE_TO_MAIN_TASK
3. ACCEPT_TASK
4. READ_TRACKER
5. AFTER_ACCEPT_MAINTENANCE_CHECK
6. BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK
7. TRACKER_PATHING
8. RESOLVE_AFTER_PATHING
9. ENTER_BATTLE
10. WAIT_BATTLE_FINISH
11. POST_BATTLE_RECOVER
12. RETURN_HOME
13. WAIT_TEAM_RETURN
14. ROUND_DONE

Non-success terminals: FAILED and STOPPED.

Current WubeiTask dispatches those states at lines 1255-1268. runRoundPhases keeps TaskCheckpoint, transaction,
yield/park and failure recovery visible at lines 504-609. TURN-35 must not add another state, hidden workflow,
wrapper state machine, owner/session/ledger, or a new timeout/TTL/retry layer.

## Current direct-local mechanics that TURN-35 must remove

The following are reachable in the current Cloud WubeiTask and violate the final thin-task boundary unless
replaced by an already-approved typed Service or one explicit turn action:

| Current source | Current behavior | Required TURN-35 ownership |
|---|---|---|
| imports/fields 4-8, 67-69, 273-278 | Direct GameClientTracker, TextRecognizer, InputSequences/InputAction, CoordinateHelper and ImagePreprocessor ownership. | Remove reachable task-local capture/OCR/input ownership; retain only pure Cloud calculation if it does not duplicate a predecessor Service. |
| lines 2091-2105 | Direct Alt+C submit before post-accept prepath. | Use TURN-28P/TURN-28 approved background key mechanics in one explicit turn result; no foreground fallback or auto retry. |
| lines 2148-2225 | Prepared dialog is validated by DialogService but clicked directly by InputSequences. | Preserve Cloud validation/operation decision; send exactly one ordered task action for the prepared absolute point and map terminal honestly. |
| lines 2636 and surrounding White Dragon probe path | Direct BagService.findAndUseItemFromBack. | Use only the permanent BagService typed local-Service boundary after the parent resolves the operation gap below. |
| lines 2709-2786 | Task computes tracker click, directly runs MOVE/WAIT120/CLICK300, probes dialog before/after and schedules local destination-hint work. | Cloud keeps link selection/randomization/business; one ordered HTTPS input action performs the click; observations return through explicit capture/typed Services. |
| lines 3044-3210 | Three local tracker destination captures, local yellow wash and local OCR. | Each requested observation is an explicit raw-PNG capture; Wubei Cloud parses/OCRs the returned frame. No DHXY/local OCR. |
| line 3902 | Direct BagService.findAndUseMainBagTaskPageItem fallback. | Use only a parent-frozen baseline-equivalent Bag local-Service operation; do not silently drop fallback. |
| lines 3983-3989 | Fixed MAX_CHAINED_COMBAT_ATTEMPTS=5 failure. | Does not match the approved Huangpao rule; see baseline conflict below. |
| lines 4211-4243 | Cached Huangpao action directly runs MOVE/WAIT120/CLICK300. | Same one-command ordered HTTPS input boundary as the normal tracker click. |

Old remote macro/fact/input ports must be zero on the final Wubei reachable path. Removing imports alone is not
enough: the named test must execute every affected branch and count real production TurnAction objects.

## Cloud/local responsibility after cutover

Cloud WubeiTask remains responsible for:

- phase order, hot-start priority and recovery/fallback decisions;
- template/OCR interpretation, tracker link selection, random point selection and dialog classification;
- deciding whether a capture is ROI or full window;
- deciding whether a prepared point should be clicked;
- deciding the next action only after consuming the previous closed terminal/result;
- White Dragon four-result branching, ordinary-monster fallback budget, Huangpao continuation, return verification
  correction, team-return and maintenance ordering.

DHXY remains responsible only for:

- exact-window background capture and raw PNG upload;
- ordered keyboard/mouse mechanics explicitly named in one JSON action;
- strict, unscaled screen-absolute/window-relative coordinate execution against the bound window;
- the four permanent local Services: BagService, UICleanerService, GiveItemService and QuestManagerService.

No OCR, tracker parsing, NPC selection, dialog decision or Wubei phase decision may be moved into DHXY.

## Exact implementation write set recommendation

Production:

1. Modify only:
   D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java
2. Necessary DTO/value helpers, if unavoidable, must be private nested types at the bottom of WubeiTask.
3. Do not modify WubeiPhase, Services, protocol, client, host, configuration, POM, resources or DHXY.

Test:

1. Create only:
   D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java
2. Test-only fakes/helpers remain private in that test file.

Process report:

1. Parent creates/finalizes:
   D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-35.md
2. This readiness report is evidence only and is not the implementation card.

## Baseline-equivalence matrix

| Business boundary | Acceptance requirement |
|---|---|
| Hot start | Preserve priority: combat, known Wubei dialog, team-return signal, Wubei tracker, return item, approved saved context, accept entry. Wubei currently has no independent approved saved-context fact. |
| Round/phase | Preserve 14 states, TaskCheckpoint placement, turn/yield/park boundaries and the current explicit terminal mapping. |
| Failure recovery | Preserve baseline recovery target/order/count and cleanup timing unless docs/业务逻辑.md explicitly supersedes it. No new retry or fail-closed branch. |
| Post-accept prepath | Preserve target selection, startup-flying Alt+C skip, default 宝象国 88,157 direction and heal-pet substitution. Tracker read remains exact-context and asynchronous as delivered by TURN-31. |
| Tracker | One Cloud-selected link; exact action ordering; targetMapName handling; no local OCR/scanner. Dark-thunder reroll and probe/ordinary/Huangpao classification remain unchanged. |
| Ordinary monster | Apply the approved shared ordinary-monster contract: Cloud static-frame fallback with at most three effective fallback executions. The stale PATHING_TERMINAL immediate foreground green re-click is not acceptance authority. |
| White Dragon | Keep first/second probe order, at most two mirror uses per link, no post-mirror park/sleep/yield, and exactly the four provider results: target ready, wrong position, story absent, no target. |
| Dialog interest | No 15-second business TTL. Target-map gate opens ordinary/Huangpao-first interest; Huangpao continuation registers interest before click; battle transition and defined non-battle outcomes clear it. |
| Enter battle | Prepared action must match exact window/task/operation and be clicked only when clickRequired. Signal-only results remain non-click results. |
| Wait battle | Preserve expected-combat fast exit, trusted correction only after failed return verification, 180-second business timeout where already approved, and no extra pre-return full-radar gate. |
| Return item | Preserve two-attempt ordering, 500ms verification, cached use then baseline fallback, trusted combat correction and return to WAIT_BATTLE_FINISH when still in combat. |
| Verified home | Starting-map/coordinate snapshot has no TTL and is cleared only after a new accept option actually succeeds. |
| Huangpao | First post-battle full tracker may build fast cache; later fast-cache miss ends the chain without full-tracker fallback. Keep the 5-second supply window and leader first aid before continuation click. No fixed fight-count cap. |
| Team return/common box | Preserve typed TURN-22/TURN-23 ordering and the 30-second CommonBox pending lifecycle; no task-local template match. |
| Maintenance | Preserve CommonBox priority, TaskMaintenance/AutoCombat caller order, cooldowns, windows and result projections from the final TURN-34A/B APIs. |
| Stop/pause | STOP is propagated, pause does not invent business progress, and no action UUID is generated after a pre-action stop/mismatch gate. |

## Two explicit baseline conflicts requiring parent freeze

### 1. Huangpao fixed-count cap

Current WubeiTask lines 169 and 3983-3989 enforce MAX_CHAINED_COMBAT_ATTEMPTS=5. The approved business document
states that Huangpao continuation has no fixed fight-count limit and continues until the post-battle tracker no
longer shows Huangpao. TURN-35 cannot preserve both. The implementation brief should explicitly select the
approved business-document rule, remove the fixed cap from the active path and record this as migration to the
current approved contract rather than an agent-invented behavior change.

### 2. Ordinary-monster immediate green re-click

The current source still contains the older pathing-terminal behavior that can immediately re-click the green
link. docs/业务逻辑.md explicitly supersedes it with the common Xiuluo/Wubei ordinary-monster Cloud fallback and
three-effective-execution ceiling. TURN-35 should follow the approved common contract and must not copy the
stale branch merely because it exists at 696-era source.

These are the only identified intentional differences from the raw Wubei source. Every other business decision
must carry the fixed card statement:

无已批准业务差异；按 696a12b0 与 docs/业务逻辑.md 已确认覆盖条款等价迁移。

## Unresolved predecessor/API gate: Bag use

CloudBagLocalServiceClient currently exposes three return-item intents:

1. PRESCAN_TASK_PAGE
2. PRESCAN_FROM_BACK
3. USE_CACHED_RETURN_ITEM

It does not expose one atomic baseline-equivalent scan-and-use operation. Current WubeiTask still directly calls:

- findAndUseItemFromBack for the White Dragon mirror;
- findAndUseMainBagTaskPageItem for the uncached return fallback.

TURN-35 must not silently replace these with cache-only behavior, skip the uncached fallback, add a fifth local
Service, or expand protocol/Service files inside its one-file write set. Before claim, the parent should freeze
one of these evidence-backed routes:

- prove that the final TURN-14/ReturnItemPrescan API always performs the required baseline prescan and provides a
  same-round cache point before every use branch, then use USE_CACHED_RETURN_ITEM; or
- amend the predecessor/API card to provide an approved baseline-equivalent Bag operation before TURN-35 starts.

This helper does not choose that business/API route.

## JSON/raw-PNG/UUID/terminal acceptance matrix

| Scenario | Action contract | Required result assertion |
|---|---|---|
| Full-window observation | One CAPTURE step, region null, UPLOAD_IMAGE. | One raw image/png frame for the exact bound window; SHA/dimensions/sourceStep/action/window metadata all match. |
| ROI observation | One CAPTURE step with unscaled screen-absolute TurnRegion. | Raw PNG dimensions equal ROI; nonzero/negative window origins are handled without coordinate scaling. |
| Prepared/green click | One action with ordered MOVE_MOUSE, WAIT, CLICK_LEFT and any baseline queue-owned hold represented by the final shared protocol. | Exactly one UUID and one command; no image unless the payload explicitly requests evidence; mechanical failure is not success. |
| Alt+C/Alt+A | One explicit background-capable key action using final TURN-28P/TURN-28 mechanics. | No foreground fallback, no local decision, one UUID/command, STOP/uncertain mapped distinctly. |
| Capture after action | Capture is included in the same action when requested by the Cloud payload and supported by the frozen contract. | The frame belongs to the declared source step and same exact window; no extra Cloud round trip merely to request routine evidence. |
| Failure evidence | fullWindowFailureEvidence only where the frozen action asks for it. | Failure identifies the exact failed step and returns the permitted full-window raw PNG; later steps are NOT_RUN. |
| Completed | Command and outcome correlate to action/device/window and every expected step. | Consume typed result; only then may Cloud decide the next business action. |
| FAILED | Exact mechanical failure. | No success state/cache/cooldown/phase advance; use evidence only for Cloud's next explicit decision. |
| STOPPED | Confirmed cooperative stop. | Return STOPPED through task/phase boundary; zero automatic retry. |
| DUPLICATE_OR_UNCERTAIN or command uncertainty | Completion is unknown. | Never infer success and never resubmit the same action; zero transport retry. |
| Business fallback | Cloud consumes a conclusive result and chooses another approved probe/fallback. | New action gets a new UUID; it is a business step, not transport retry. |
| Identity drift/missing metadata | Exact device/window/HWND/process/window bounds preflight fails. | Zero new action UUID/command and no state mutation. |

## WubeiWholeTaskTurnContractTest acceptance matrix

The named test should instantiate production WubeiTask and production typed collaborators/TurnGameClient path
with private scripted fakes. It must not copy the Wubei decision mapper into a test-only implementation.

Minimum groups:

1. State graph
   - exercise all 14 business states and FAILED/STOPPED;
   - assert exact next phase, transaction result and yield/park boundary;
   - assert no hidden fifteenth state or wrapper workflow.

2. Hot-start priority
   - combat, known dialog, team return, tracker, return item and accept-entry ordering;
   - no stale OCR/coordinate/saved-context fact inserted.

3. Post-accept prepath
   - default and heal-pet target, startup-flying Alt+C skip, exact-context TURN-31 background tracker binding;
   - one key action where required and zero action on the skip branch.

4. Tracker and image
   - raw full-window tracker frame, first/second White Dragon link, ordinary, Huangpao and dark-thunder results;
   - nonzero and negative origins, unscaled absolute points, bad PNG/SHA/dimensions/correlation;
   - local OCR/ImagePreprocessor/GameClientTracker/InputSequences reachable counts are zero.

5. Prepared dialog
   - clickRequired true produces one ordered action;
   - clickRequired false produces no click;
   - wrong window/task/operation, stale/consumed action and terminal uncertainty cannot advance phase.

6. Ordinary-monster fallback
   - normal entry, static-frame Cloud hit/miss, at most three effective fallback executions;
   - entering combat stops further local/template work;
   - stale immediate foreground green re-click is absent.

7. White Dragon
   - both links, two mirror opportunities per link, four explicit provider outcomes;
   - no post-mirror park/sleep/yield;
   - target-ready smart click and approved direct-combat fallback sequence.

8. Huangpao
   - first post-battle full tracker builds cache;
   - subsequent fast cache hit continues and miss ends chain without full fallback;
   - no fixed fight-count cap;
   - 5-second maintenance window and leader recovery order before continuation click.

9. Combat and return
   - fast expected exit, no pre-return full-radar gate, return success, trusted still-in-combat correction back to
     WAIT_BATTLE_FINISH, trusted not-in-combat failure path;
   - exactly two approved return attempts and 500ms verification;
   - verified-home snapshot survives time, team-return wait and failed accept until accept option succeeds.

10. Permanent local Services
    - Bag/UICleaner calls use only typed LOCAL_SERVICE actions;
    - GiveItem/Quest are not invoked unless a real Wubei branch requires them;
    - permanent local allowlist remains exactly four;
    - uncached Bag fallback is explicitly tested after the parent resolves the API gate.

11. Maintenance and team
    - TURN-34A/B typed return projections drive existing phase decisions;
    - TURN-22 team-return random point, timing and polling behavior;
    - TURN-23 CommonBox detect-after-verified-return and consume-before-next accept, with existing 30-second TTL
      only for CommonBox pending.

12. Terminal/correlation/counts
    - COMPLETED/FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN and command timeout/transport uncertainty;
    - exact action/window/step/frame correlation;
    - one UUID per command, zero resend and zero automatic retry;
    - every business fallback has a distinct new UUID and its baseline attempt count.

13. Source/reachability gate inside this named test
    - final Wubei reachable path has zero old remote macro/fact/input port;
    - zero direct InputSequences/InputAction/GameClientTracker/TextRecognizer/ImagePreprocessor capture/OCR;
    - no runtime/application/server/startup activation was added by TURN-35.

The implementation Worker may run only the parent-authorized named test:

mvn -q -Dtest=WubeiWholeTaskTurnContractTest test

Applicable Cloud compile/build and the four foundation debt tests remain separate parent gates. This helper ran
none of them.

## Potential write-set conflicts

- TURN-31 already modified WubeiTask. TURN-35 must preserve that exact-context async tracker delta.
- No other Worker may own WubeiTask or WubeiWholeTaskTurnContractTest while TURN-35 is claimed.
- TURN-27, TURN-28, TURN-34A and TURN-34B must finish before TURN-35 because the task's one-file write set cannot
  repair their APIs.
- TURN-28P protocol/executor work and TURN-33 Summon work are separate write sets but transitively gate TURN-35.
- TURN-38 and TURN-40 must not start editing WubeiTask under TURN-35 ownership; context removal/activation stays
  in their own planned files.
- Existing dirty/untracked bytes in both repositories remain protected; implementation must incrementally edit
  current WubeiTask rather than restore/copy a baseline file.

## Executable dispatch brief for the parent

Before dispatch:

1. Confirm TURN-28P and TURN-33 true EOF deliveries and parent source/test-source review.
2. Complete TURN-22 repair and re-review.
3. Freeze and complete TURN-27, TURN-28, TURN-34A and TURN-34B production APIs/tests.
4. Resolve the Bag scan-and-use API gate without expanding TURN-35's write set.
5. Freeze the two docs/业务逻辑.md supersessions: ordinary-monster Cloud fallback and no fixed Huangpao count.
6. Re-read WubeiTask and recalculate its hash after all predecessor writers are stable.

Implementation assignment:

- One Worker, one unique TURN-35 countUnit/countDelta=+1.
- Modify only WubeiTask and create only WubeiWholeTaskTurnContractTest plus append its fixed card report.
- Retain TURN-31 exact-context asynchronous tracker read.
- Replace each direct local mechanic listed above with one explicit typed turn or an approved predecessor Service.
- Keep Cloud as sole Wubei decision/OCR/business owner.
- Do not add automatic retry/session/ledger/TTL/durable workflow, new public DTO/service, wrapper nesting or host activation.
- Deliver source/test-source evidence only; parent runs authorized tests/build after Java writers are stable.

Parent review focus:

- compare every phase and fallback against 696a12b0 plus the explicit docs/业务逻辑.md supersessions;
- inspect real production TurnAction objects and exact UUID/command/frame counts;
- verify terminal uncertainty never becomes success;
- verify all direct local capture/OCR/input and old remote ports are unreachable;
- verify the four permanent local Service allowlist is unchanged.

## Precheck conclusion - 2026-07-16T03:46:23-04:00

TURN-35 has a clear one-production-file implementation shape and a concrete named-test acceptance surface. Its
real caller, phase graph and remaining local-mechanics leaks are identified. The direct dependency graph is not
yet stable at this true EOF: TURN-28P and TURN-33 have only replacement claims, TURN-22 needs repair, and
TURN-27/28/34A/34B are readiness-only. The Bag scan-and-use boundary also needs a parent-frozen predecessor/API
decision before implementation claim.

This is evidence and a dispatch brief only. It does not implement TURN-35 and does not issue a review decision.

PRECHECK_COMPLETE
