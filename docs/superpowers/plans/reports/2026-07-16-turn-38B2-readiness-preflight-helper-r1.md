# TURN-38B2 ReturnItem Workflow-State Rewire Readiness Preflight Helper R1

## PRECHECK 0 - Role, Snapshot, And Evidence Boundary

- Role: CR271 non-binding readiness/preflight helper. This report is evidence for the parent; it does not implement,
  review, claim, assign, or decide the card.
- Snapshot time: `2026-07-16T07:14:59-04:00`.
- Only write performed by this helper:
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-38B2-readiness-preflight-helper-r1.md`.
- No Java, test, card, plan, `docs/ACTIVE_WORK.md`, CR271, matrix, dashboard, configuration, resource, or other report
  was modified. No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input was run. No Git
  mutation was performed.
- Both repositories' existing dirty/untracked content is protected. This report does not infer an owner or claim from
  a dirty path.

Authority read at this snapshot:

| Evidence | Snapshot identity |
|---|---|
| `AGENTS.md` | SHA-256 `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md` | SHA-256 `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| CR271 top of `docs/ACTIVE_WORK.md` | SHA-256 `C324C3828F239E2F69DB8F88D621CD2C7AB1B7568E7CBAB312BCF55139F682EE`; latest top section includes the `07:12` continuation |
| authoritative plan Sections 14-19 | SHA-256 `E471F811BAF55C0C58F126270C3D9462FE5A440378EF7A17AA8F6E357FD282B4` |
| HTTPS turn protocol | SHA-256 `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| `docs/业务逻辑.md` | SHA-256 `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` |
| TURN-14 fixed card | 310 lines; SHA-256 `EB9B17B34006C69DB5C966138AD61A62288350A4EBEB9E463566CD596899A9D6` |
| TURN-22 fixed card | 501 lines; SHA-256 `788A62BE80CF4CC4207BD3948715884F99767BCBD16351D9792253B69C55A786` |
| TURN-35 latest dependency report | 278 lines; SHA-256 `3D8DA8CF611EAB7045E334A7B96E52BFC2A5A25EB427D14D9F10D7470A5E2500` |
| TURN-36 readiness report | 352 lines; SHA-256 `F27B886E5FF026E9E24B0939BE5665D70BDD1BB54E252A603DF827BD415BF39C` |
| TURN-37 readiness report | 322 lines; SHA-256 `4E86925D338F570FB06C230F920477A68E00B55B4D357D205BFF53329D8C3ABD` |
| TURN-38A latest precheck | 362 lines; SHA-256 `04C8C2722A3D6E2C62ED876DB0CB6073DC309595F35A96A76EC63D692CFE456F` |

Repository snapshot:

| Repo | Branch / HEAD | Expanded status |
|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 649 entries: 605 untracked, 43 modified, 1 deleted |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 entries: 541 untracked, 9 modified |

## PRECHECK 1 - Exact Dependency Facts

The current registry at authoritative-plan lines `1154-1158` fixes:

```text
startDependsOn(TURN-38B2) = TURN-14 + TURN-22 + TURN-38A
```

Direct dependency disposition:

| Dependency | Current source fact | Meaning for TURN-38B2 source start |
|---|---|---|
| TURN-14 | Parent re-review records `P0/P1/P2=0/0/0` for production/test source. Its named test and Cloud build remain pending in the stable-writer cohort. | Its source surface is available read-only; later test/build evidence is not replaced by this report. |
| TURN-22 | Parent Delivery Review #4 records `P0/P1/P2=0/2/1` and requires Repair #3. Repair #3 waits for the final TURN-28P Repair #2 frozen queue API; External A is online but currently performs no TURN-22 Java mutation. | Direct source prerequisite is not yet satisfied. |
| TURN-38A | Latest precheck records unmet direct predecessors, old-authority caller/test ownership gaps, no production turn-native factory, and a real 38A -> 38B caller-order inversion. | Direct source prerequisite and the API consumed by 38B2 are not yet stable. |

Current transitive source order is:

```text
TURN-28P Repair #2 delivery and parent source/test-source gate
  -> TURN-22 Repair #3 delivery and parent source/test-source gate
  -> TURN-34B source gate
  -> TURN-34C source gate

TURN-28P -> TURN-28 -> TURN-27
TURN-34A final source gate

TURN-22 + TURN-27 + TURN-28 + TURN-34A + TURN-34B
  -> TURN-35 / TURN-37 source gates
TURN-27 + TURN-28 + TURN-34A plus the open-main-bag boundary
  -> TURN-36 source gate
TURN-34C + TURN-35 + TURN-36 + TURN-37
  -> TURN-38A source gate and parent-frozen context API
TURN-14 + TURN-22 + TURN-38A
  -> TURN-38B2 source start
```

The latest dependency reports provide the concrete intermediate facts:

- TURN-35 still lacks source gates for TURN-22, TURN-27, TURN-28, TURN-34A, and TURN-34B.
- TURN-36 still lacks final TURN-27/TURN-28/TURN-34A surfaces and a parent-owned open-main-bag local boundary.
- TURN-37 still lacks TURN-22/TURN-27/TURN-28/TURN-34A/TURN-34B and the final typed objective/NPC surfaces.
- TURN-38A itself depends on TURN-34C/TURN-35/TURN-36/TURN-37, and its latest precheck requires the parent to resolve
  the 38A/38B ordering and source-compatible context boundary before any claim.

Approval-time evidence is separate from source start. Authoritative-plan lines `1461-1499`, `1602-1610`, and
`1645-1649` require the future card to have:

- its one named test `service/returnitem/ReturnItemWorkflowStateTurnTest` with `BC4+BASE+STATE` coverage;
- parent source review and test-assertion review;
- two independent reviewer passes under the CR process;
- a fresh successful named-test command and applicable Cloud compile/build after Java writers stabilize;
- the portions of TURN-T01/T02/T03/T04 that the parent maps to the final actual invocation chain.

The applicable Foundation subset cannot be guessed while the parent has not decided whether 38B2 is a dormant
state-only rewrite or a live `ReturnItemPrescanService` public-path rewire. The helper does not silently choose one.

## PRECHECK 2 - Current Live Caller Versus Dormant State-Core

### 2.1 Live production authority today

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java` is the current
live public boundary:

- `:45-46` injects `CloudBagLocalServiceClient` and owns a private `ConcurrentHashMap<PrescanKey, PrescanState>`.
- `:71-216` exposes the seven baseline public methods `afterTrackerGreen`, `afterTrackerGreenRequired`,
  `whilePathing`, `whileInCombat`, `useCached`, `invalidate`, and `completeRound`.
- `:218-289` selects/retains strategy, combat due, cache, `done`, `inProgress`, and fallback in that private map.
- `:346-393` keys the map by task code, window id, HWND, task-run id, round, and template and defines the private
  business state. It does not obtain either TURN-38B2 target class from the context or holder.
- Current file identity: 394 lines, SHA-256
  `FE31D4C9F13C4347639707346088445429737CE106D3C2B04EE7D3890AC5BEE6`, untracked and protected.

There are 15 real production call sites, all through `ReturnItemPrescanService`:

| Caller | Exact call sites |
|---|---|
| `XiuluoTaskV2` | `:1682 afterTrackerGreen`, `:1842 whilePathing`, `:2085 whileInCombat`, `:2870 useCached`, `:2878/:2905 completeRound`, `:2884 invalidate` |
| `WubeiTask` | `:1664 whilePathing`, `:2768 afterTrackerGreenRequired`, `:2772 afterTrackerGreen`, `:3774 whileInCombat`, `:3883 useCached`, `:3891/:3917 completeRound`, `:3897 invalidate` |

The current task callers preserve the verified business boundary: cached click success is followed by one existing
map verification before `completeRound`; failed verification calls `invalidate`; uncached fallback remains the
existing Bag path. `docs/业务逻辑.md:253-254,470-471` forbids converting the accepted return-home fact into a timed
cache or adding a second verification.

### 2.2 TURN-38B2 targets today

| Target | Current identity | Reachability fact |
|---|---|---|
| `service/returnitem/CloudReturnItemPrescanStateOwner.java` | 1141 lines; SHA-256 `3E606C3BDCFB2A9F3F56A355B1B34F30BA7CA30298E39AEF58C8442BB1D124E4`; `??` | Constructor is private at `:81-88`; zero factory; `finishPrescan` and `completeRound` are private at `:267` and `:347`; no production caller outside these two target files. |
| `service/returnitem/ReturnItemPrescanWorkflowState.java` | 206 lines; SHA-256 `FB6901BB9454776C225A9951F06EAB5E6F5AB280B9F2E08ECB5407F4045BC55D`; `??` | Requires an owner in its public constructor at `:52-54`, but no caller can construct that owner; no production caller outside these two target files. |

The future named test
`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/returnitem/ReturnItemWorkflowStateTurnTest.java` is absent.
The existing TURN-14 test, `ReturnItemPrescanTurnContractTest.java`, is read-only for 38B2; it constructs and calls
the live `ReturnItemPrescanService` at `:533-551` and does not touch either 38B2 target.

Therefore the current facts are not a live rewire: the approved public business path and the dormant state-core are
two disconnected authorities. Editing only the two 38B2 files cannot, by itself, replace
`ReturnItemPrescanService.states` or make the 15 real Task call sites consume the new workflow state.

Before a future claim, the parent must freeze one exact interpretation:

1. 38B2 only removes old-authority dependencies from the dormant two-file state-core, with no claim that live
   ReturnItem behavior is rewired; or
2. 38B2 must reach the live public ReturnItem path, in which case the current production write set or an explicit
   predecessor/caller ownership assignment is insufficient and must be revised by the parent first.

This helper does not expand the write set or select an interpretation.

## PRECHECK 3 - Exact State And Cache Gaps To Freeze

### 3.1 Old retained-authority coupling

The owner still imports and consumes `RemoteTaskRunAuthorization`, `RemoteTaskRunBinding`, `RemoteTaskRunScope`, and
`RemoteTaskRunWindow` at `:8-11`. Its stale fence at `:615-676` calls old
`TaskExecutionContext.revalidate()`, reads old scope/stopEpoch/runRevision/playerIdentityEpoch, and reconstructs the
old run/window tuple.

The latest 38A precheck identifies the exact B2 old-context calls at owner lines `237,421,501,625,630,634-636,676`.
Current turn-native `TaskExecutionContext` instead has no workflow-state field, no terminal-state release seam, and
its old authority methods fail on the turn-native path. `TaskExecutionContextHolder` currently holds only a
`ThreadLocal<TaskExecutionContext>` and checkpoint helpers. No final 38A API name or lifecycle object exists to
write against.

Plan line `864` requires business source, outside the later old package deletion cohort, to have zero
`RemoteTaskRun*`, retained-action, and final-consumption dependencies and forbids a replacement session/ledger.
The earlier parent simplification directive in
`2026-07-13-cloud-return-item-prescan-state-worker-d.md:175-178` also cancelled the proposed ReturnItem-specific
permit/proof/settlement-ledger wave. A future 38B2 worker must not revive that cancelled `.remote` design under a new
name.

### 3.2 Cache payload mismatch

The live TURN-14 cache is `ReturnItemCachePoint`:

- `model/bag/ReturnItemCachePoint.java:7-22` fixes a screen-absolute `clickX/clickY`, exact `templatePath`,
  `learnedAtMs`, and `source`.
- `CloudBagLocalServiceClient.java:61-99,386-418` sends and receives those exact fields, validates nonnegative screen
  coordinates and a positive learned timestamp, and binds a FOUND result to the requested template.
- `ReturnItemPrescanTurnContractTest.java:93-170,481-516` checks the exact cached payload and template mismatch with
  one UUID, one command, and no retry.

The dormant owner instead defines `PrescanCachePoint(clientX, clientY, geometryGeneration)` at `:902-919` and its
JavaDoc calls those coordinates window-client space. It does not retain the TURN-14 template/timestamp/source
payload. No parent decision currently authorizes changing the live screen-absolute cache into this older shape.

The future boundary must preserve the exact TURN-14 payload and coordinate space unless a separate business-change
decision explicitly says otherwise. A hidden screen/client conversion, geometry-expiry rule, or timestamp-age gate
is outside 38B2 readiness.

### 3.3 Terminal mapping mismatch

The live TURN-14 service currently maps one mechanical invocation as follows:

- `ReturnItemPrescanService.java:306-343` returns null only for typed `NOT_EXECUTED`; `UNKNOWN` or unresolved
  `STOPPED` becomes fatal after the checkpoint check; no action is resent.
- `:250-289` treats a null prescan result as the existing failed-prescan branch and always releases `inProgress` in
  `finally`.
- `:170-195` treats cached-use `NOT_EXECUTED/NOT_USED` as false and performs the existing invalidate branch;
  unresolved terminals do not become false success.

The dormant owner was designed for the cancelled retained-action settlement model: at `:526-529` a trusted
`NOT_EXECUTED` keeps the same attempt open for upper resubmission, and `UNKNOWN` also freezes the open attempt. That
may be internally coherent for the old ledger design, but it is not the current live TURN-14 terminal mapping and
cannot silently become business truth. HTTPS protocol lines `108-113,151-157,294-308` prohibit automatic action
re-execution; every explicit business retry is a new Cloud decision and new action id.

The parent must freeze the exact public-terminal-to-state matrix before implementation. No B2 worker should infer
that old `PrescanAttemptHandle` retention authorizes another command or same-action replay.

### 3.4 Continuity, stale, release, and no-TTL invariants that can be frozen now

The following behavioral invariants are supported by the plan, current baseline, and existing source evidence:

1. Exact tenant/user/device/window/task-runtime isolation. Another scope, device, window, or runtime cannot read or
   mutate the cache, strategy, combat due, fallback, progress flag, or open workflow cursor.
2. Pause/resume continuity within the same runtime. Strategy and combat jitter are drawn once; combat due is not
   recomputed; the exact cache payload and unresolved workflow cursor survive pause/resume without a second action
   or UUID.
3. Stale-context rejection is zero mutation and zero action. It must cover admission/lookup, entry, cache use,
   invalidate, settlement, round completion, and terminal cleanup, not only one selected method.
4. UNKNOWN never becomes FOUND/USED/NOT_FOUND/NOT_USED, never clears an accepted cache, and never creates an
   implicit retry. STOP remains stop/unwind semantics, not a business failure or success.
5. `completeRound` is allowed only at the existing caller point after the one accepted return-map verification and
   cannot discard an open/uncertain operation. A missing exact state is an idempotent no-op, not cross-runtime
   cleanup.
6. The `STATE` profile also requires exact terminal/restart release. Current owner lines `378-388` and workflow lines
   `31-39` explicitly say terminal removal is absent, so this acceptance surface is not currently implemented.
7. No TTL, expiry, LRU, age rejection, scheduled cleanup, compaction, durable restore, or restart recovery is added.
   Cache/state disappears only at an exact approved lifecycle event, not because wall-clock time passed.
8. Existing fixed capacity (`GLOBAL_LIMIT=1000`, `PER_RUN_LIMIT=64`) and fail-closed admission at owner
   `:60-73,391-438`, plus workflow's structural group bound at `:175-187`, cannot be silently changed or converted
   into eviction.

## PRECHECK 4 - Exact Future Write Set

Authoritative plan lines `1314-1323` and `1645-1649` fix the complete 38B2 Java write set.

Production, Cloud repository only:

1. Modify
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/returnitem/CloudReturnItemPrescanStateOwner.java`.
2. Modify
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/returnitem/ReturnItemPrescanWorkflowState.java`.

Test, Cloud repository only:

1. Create
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/returnitem/ReturnItemWorkflowStateTurnTest.java`.

Read-only boundary includes, at minimum:

- `ReturnItemPrescanService.java`, `ReturnItemPrescanDecision.java`, `ReturnItemCachePoint.java`,
  `CloudBagLocalServiceClient.java`, and `ReturnItemPrescanTurnContractTest.java`;
- `WubeiTask.java`, `XiuluoTaskV2.java`, FiveRing and every other Task/caller;
- all TURN-38A context/holder/checkpoint/task files;
- all `.remote`, host/configuration, turn protocol/client/exchange, local-Service adapter, POM/resource files;
- all B1/B3/B4 production/tests, cards, plans, CR271, `ACTIVE_WORK`, matrix, and dashboard.

No second test, source guard, fixture, DTO file, facade, wrapper chain, owner registry, session, ledger, permit family,
TTL, retry helper, or cleanup scheduler belongs to the card. If the final parent-frozen API requires any read-only
file, the future worker must stop and return to the parent for a plan/write-set correction.

Because both production targets are currently untracked, a future claimant must re-record their full SHA/status and
edit the current bytes incrementally. Recreating either file from an older report or baseline would overwrite
protected work.

## PRECHECK 5 - Collision Delta

Physical path intersection with the 38B2 production/test set is zero for every current or adjacent lane below.
Logical/API ordering is a separate gate.

| Lane/card | Exact other write set | Physical intersection with B2 | Ordering/collision evidence |
|---|---|---:|---|
| External B, TURN-28P Repair #2 | DHXY six production files (`InputActionQueue`, `InputActionRequest`, `InputActionWorker`, `InputSequences`, `WindowAwareInputCoordinator`, `TurnCaptureStepExecutor`), three DHXY tests, two Cloud turn-client tests, and its fixed card | empty | Active Java/test writer. Its final API gates TURN-22, then the B2 direct chain. No concurrent file collision, but predecessor facts are still moving. |
| External A, TURN-22 Repair #3 | Cloud `TeamReturnTurnContractTest`; DHXY `TurnInputStepExecutor` and its contract test; TURN-22 card | empty | Waits for External B's final source gate. TURN-22 must finish before B2 source start. |
| External C, TURN-34A | Cloud `AutoCombatService`, `AutoCombatServiceTurnContractTest`, and its card | empty | Active writer; indirectly gates 35/36/37 and therefore 38A. |
| External D, TURN-34B | Cloud `TaskMaintenanceService`, `TaskMaintenanceTurnContractTest`, and its card | empty | Waits for TURN-22; indirectly gates 34C/35/37 and therefore 38A. |
| TURN-38A | Seven context/checkpoint/task production files plus `TaskExecutionContextOldAuthorityRemovalTest` | empty | Hard API ordering dependency. B2 cannot compile against a guessed context/workflow/lifecycle seam; 38A latest precheck also identifies the reverse caller-order problem. |
| TURN-38B1 | `BagWorkflowState`, `CloudBagStateOwner`, `BagWorkflowStateTurnTest` | empty | May be file-parallel only after 38A final API. Both cards must use one parent-frozen context identity/lifecycle model, not create separate registries. |
| TURN-38B3 | `CloudStartupGateAuthority`, startup check service, `StartupGateTurnStateTest` | empty | May be file-parallel only after 38A. Plan line `1320` says `service/TaskStartupCheckService`, while the real source is `task/startup/TaskStartupCheckService`; parent must normalize B3's path before its claim, but neither path intersects B2. |
| TURN-38B4 | `CloudArtifactStore`, `ScopedPngArtifactStore`, `CloudServiceConfiguration`, `ScopedPngArtifactStoreTurnTest` | empty | May be file-parallel after its own predecessors and 38A; B2 must not absorb artifact/config cleanup. |
| TURN-35/36/37 | One Whole Task production file and one card-specific Whole Task named test each | empty | These precede 38A. Wubei and Xiuluo are live ReturnItem callers, so their final public call shape must be re-scanned before B2; B2 cannot edit them. |

The claimed plan statement that B1/B2/B3/B4 are mutually exclusive by file is true. It does not make them
startable now and does not resolve the shared 38A API, the B2 live-caller gap, or the cache payload mismatch.

## PRECHECK 6 - Future Named-Test Matrix

Exact test class:

```text
com.yueyunfe.dhxy.cloudbrain.service.returnitem.ReturnItemWorkflowStateTurnTest
```

The future test must call a real public production boundary supplied by the final B2 design. It must not construct
private state with reflection, copy the reducer/terminal matrix into the test, scan source text, or mock away the
state owner being accepted. Current code must fail the cases for a production reason, not merely because the test
expects a renamed private method.

| Exact future case name | Required public-path evidence |
|---|---|
| `pauseResumeKeepsExactCacheStrategyDueAndOpenWorkflowWithoutExtraAction` | A1 admits/prescans; A2 is the same runtime after pause/resume. Same strategy, due, fallback, exact cache payload, and unresolved cursor are observed. Resume emits zero UUID/command until the caller explicitly invokes a new business action. |
| `staleContextRejectsEveryReadAndMutationWithZeroAction` | After A2 becomes current, stale A1 cannot admit, enter, read/use cache, invalidate, settle, complete, or terminal-clear. Snapshot all state before/after and assert equality plus UUID/command=`0`. |
| `tenantUserDeviceWindowAndTaskRuntimeStayIsolated` | A, B, then A sequence varies tenant, user, device, window, and runtime independently. B sees no A cache/cursor; returning to exact A sees the original state. No caller-supplied text may select another tenant's state. |
| `prescanTerminalMatrixPreservesTurn14AndNeverRetries` | Through the public ReturnItem path, script COMPLETED FOUND, COMPLETED NOT_FOUND, FAILED/NOT_EXECUTED, STOPPED, and UNCERTAIN separately. Assert exact baseline mutation/unwind, one UUID and one command for an invoked action, and zero automatic resend. |
| `cachedUseTerminalMatrixPreservesInvalidateAndUncertaintyRules` | USED returns true without auto-completing the round; NOT_USED and trusted NOT_EXECUTED follow the existing false/invalidate branch; STOPPED/UNCERTAIN never become false success or clear accepted state. Each invoked action is one UUID/command and no retry. |
| `oldLearnedTimestampDoesNotExpireExactTurn14CachePoint` | Retain a valid FOUND point with `learnedAtMs=1`; much later public cached-use still sends the exact template/screen-absolute coordinates/timestamp/source. No sleep or clock seam is used; only explicit complete/terminal events may remove it. |
| `openOrUncertainWorkflowCannotBeCompletedOrEvicted` | An open/uncertain operation makes round completion refuse with zero mutation. Capacity pressure fails closed and does not evict another exact round/cache/cursor. |
| `completeRoundAndTerminalRestartReleaseOnlyExactRuntimeState` | Exact verified-round completion removes only that round/template; task terminal releases all state for that exact runtime; a restart receives fresh state and does not restore old cache/cursor; foreign/stale terminal attempts are zero mutation. |
| `workflowStateUsesNoTtlLruSessionLedgerOrImplicitRetry` | Dynamic assertions show state survives arbitrary cache age and pause duration, only exact lifecycle calls remove it, and no background action appears. Source/test review separately confirms there is no TTL/LRU/session/ledger/scheduler mechanism. |

Required matrix shape:

```text
A1 current -> pause -> A2 same runtime -> stale A1 reject -> B isolated -> A2 continuity
-> exact round completion -> exact terminal release -> restart fresh
```

The named test must use fake `TurnGameClient`/scripted outcomes only. State-only operations expect UUID/command=`0`;
each explicitly invoked Bag action expects exactly one UUID and one command. No real capture/input/runtime is involved.

## PRECHECK 7 - Stop-Work Conditions And Real Start Blockers

A future implementation must stop before editing when any of these remains true:

1. TURN-22 Repair #3 lacks its latest parent source/test-source gate after TURN-28P Repair #2.
2. TURN-38A lacks final parent-frozen source, hashes, context/workflow identity API, stale witness, terminal/restart
   release ownership, or a resolved 38A/38B ordering.
3. TURN-34C/TURN-35/TURN-36/TURN-37 have not supplied the source gates required by 38A.
4. Either B2 production target hash/status differs from the snapshot without a new ownership/collision check.
5. The implementation needs `ReturnItemPrescanService`, a Task/caller, context/holder, `.remote`, host/configuration,
   protocol/client, POM/resource, or a second test file.
6. The proposed design revives the cancelled ReturnItem permit/proof/settlement ledger, creates a second owner/state
   map/registry, or adds session, TTL, expiry, LRU, compaction, retry, restart restore, or scheduled cleanup.
7. Cache representation changes from the TURN-14 screen-absolute five-field payload, or adds a geometry/timestamp age
   rejection, without a separate parent business-change decision.
8. NOT_EXECUTED/STOPPED/UNCERTAIN mapping, strategy draw count/order, `4000 + 8000..18000ms` combat due, fallback,
   invalidate, completeRound, verification count, phase/park, or action/UUID count would differ from the accepted
   baseline.
9. The sole named test cannot traverse a real public production boundary and fail against the current old behavior,
   or relies on reflection/private helpers/source strings/copied business logic.
10. Any physical overlap appears with External A/B/C/D, 38A, or B1/B3/B4 after a fresh exact-path scan.

At this snapshot, the two direct source-start blockers are TURN-22 and TURN-38A. In addition, the parent must freeze
the live-caller ownership and exact cache/terminal mapping before a B2 implementation brief is mechanically safe.
TURN-14's source gate is satisfied; its pending named test/build remains later acceptance evidence, not permission to
change its public cache or terminal contract.

无已批准业务差异；按 `0114604e` ReturnItem baseline、TURN-14 HTTPS turn contract、`696a12b0` Whole Task boundary
与最小 HTTPS JSON turn 等价迁移。

PRECHECK_COMPLETE TRUE_EOF
