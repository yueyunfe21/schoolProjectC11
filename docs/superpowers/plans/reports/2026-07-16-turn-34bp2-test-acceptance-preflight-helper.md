# TURN-34BP2 test/acceptance gate preflight checklist

## 1. Helper boundary

- [x] Role is `CR271 Internal` read-only helper for `TURN-34BP2` test/acceptance gate preflight.
- [x] The only writable artifact is this report.
- [x] No DHXY/Cloud Java, test, card, plan, `ACTIVE_WORK.md`, dashboard, protocol, or business document was modified.
- [x] No Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture, input, or Git command was run.
- [x] No mid-edit BP2 production bytes are reviewed or used as delivery evidence.
- [x] This checklist makes no card verdict and does not create, claim, review, or close any implementation/test tranche.
- [x] Business authority remains `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- [x] Expected business-difference statement remains `无已批准业务差异；按 696a12b0 等价迁移`.

## 2. Authority read checklist

- [x] Read `AGENTS.md`.
- [x] Read `docs/DHXY_CONTEXT.md`, including the CR271 HTTPS turn/test gate summary.
- [x] Read the top CR271 entries in `docs/ACTIVE_WORK.md`.
- [x] Read Sections 14-19 of `2026-07-15-https-turn-complete-migration-card-plan.md`; these sections override conflicting older card details.
- [x] Read `2026-07-15-https-turn-thin-client-protocol-design.md`.
- [x] Read `2026-07-15-https-turn-protocol-foundation.md` as a non-authoritative Foundation appendix.
- [x] Read `docs/业务逻辑.md`, including local-team scope, CommonBox priority, Summon static-tail/UNKNOWN rules, stop semantics, and the `696a12b0` baseline table.
- [x] Read the current `TURN-34B`, `TURN-34BP1`, `TURN-34BP2`, and `TURN-34BT1` cards.
- [x] Read BP1 implementation readiness and BP2 readiness/delta reports.
- [x] Read post-BP2 BP3 readiness material only to classify deferred ownership; it is not used to pre-judge BP2.
- [x] Read the current Cloud `TaskMaintenanceService.java` only to understand the public workflow and identify that the file is still an unstable BP2 work surface.

## 3. Current test ownership facts

- [x] BP2's exact implementation write set is production-only: Cloud `TaskMaintenanceService.java` plus append-only BP2 child card.
- [x] BP2 has **no independent `testWriteSet`**: `testWriteSet = empty`.
- [x] BP2 explicitly keeps every test file read-only.
- [x] `TaskExecutionContextTurnContractTest.java` belongs to BP1, not BP2.
- [x] Section 19.4 assigns TURN-34B one sole named test: Cloud `service/TaskMaintenanceTurnContractTest.java`.
- [x] Exact future path is `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`.
- [x] At this preflight snapshot, that sole named test does not exist.
- [x] Existing `TURN-34BT1` has no test bytes and targets the pre-BP2 retained production SHA; it cannot be final acceptance evidence without a parent-refrozen post-BP3 target SHA.
- [x] All later 34B test tranches must modify the same sole named-test file serially; no second test class or parallel writer is permitted.
- [x] The future stable-writer command remains `mvn -q -Dtest=TaskMaintenanceTurnContractTest test` from `D:/mavenProject/dhxy-cloud-brain`.

## 4. WIP exclusion gate

- [x] Frozen BP2 starting production identity is 1,224 lines / SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`.
- [x] The currently readable Cloud file differs from that frozen start and the BP2 card has no canonical source delivery/parent receipt at this snapshot.
- [x] Current mid-edit line count, hash, private types, field declarations, and method bodies are therefore not classified as pass/fail evidence.
- [ ] Begin BP2 source inspection only after the BP2 card contains canonical delivery, final line/byte/SHA identities, changed-method/type index, owner release, and a stable physical tail.
- [ ] Recompute the delivered file from disk before source inspection; do not rely on worker prose, this helper, or an earlier WIP hash.
- [ ] If a BP2 repair follows, discard the earlier delivery snapshot and repeat this gate against the latest canonical delivery.

## 5. BP2 source-only acceptance checklist

### 5.1 Exact write set

- [ ] Confirm the frozen start SHA and canonical final SHA are both recorded.
- [ ] Confirm the only production change is Cloud `TaskMaintenanceService.java`.
- [ ] Confirm the only DHXY-side change by the BP2 owner is append-only BP2 child-card evidence.
- [ ] Confirm BP1 context/test, all 34B tests, AutoCombat/AutoBattle/Task callers, Dialog/Summon/TeamReturn/CommonBox services, protocol/client/model/POM, and DHXY Java are byte-untouched by BP2.
- [ ] Confirm private key types remain at the file bottom after the main workflow.
- [ ] Confirm no wrapper/helper ladder or one-line compatibility adapter was added.

### 5.2 Four shared typed maps

- [ ] `activeTeamRoundByKey` is exactly a `Map<ScopedTeamKey, Integer>` or the parent-frozen same-semantics final type.
- [ ] `teamMaintenanceWindowStateByRound` is exactly a `Map<TeamRoundKey, TeamMaintenanceWindowState>`.
- [ ] `localTeamSessions` is exactly a `Map<ScopedLocalSessionKey, LocalTeamSessionState>`.
- [ ] `summonSkillClaimsByTeamRound` is exactly a `Map<MaintenanceClaimKey, Set<ScopedWindowKey>>`.
- [ ] Formal team-round identity carries exact execution scope, coordination identity, maintenance key, and round by typed fields.
- [ ] Local capability claim identity carries exact scope, explicit local-session key, capability, and existing epoch by typed fields.
- [ ] Formal and local claim identities are type-distinct and cannot alias.
- [ ] Claim owner identity is `ScopedWindowKey(scope + windowId)`, not a delimiter string.
- [ ] `LocalTeamSessionState` may retain raw window IDs internally only because its outer `ScopedLocalSessionKey` already isolates execution scope.

### 5.3 Four BP3-owned per-window maps remain untouched

- [ ] `lastSummonSkillCleanAtByWindow` remains `Map<String, Long>` with unchanged timing/value behavior.
- [ ] `lastSummonSkillNotDueLogAtByWindow` remains `Map<String, Long>` with unchanged logging-throttle behavior.
- [ ] `summonSkillUnknownRetryAfterByWindow` remains `Map<String, Long>` with unchanged UNKNOWN retry-after behavior.
- [ ] `summonSkillStateByWindow` remains `Map<String, SummonSkillWindowState>` with unchanged cache/state behavior.
- [ ] BP2 does not change `currentWindowKey`, native/player fingerprint handling, generation registry, cache invalidation, or per-window state purge.
- [ ] Any remaining delimiter logic solely inside these four deferred per-window paths is recorded as BP3 debt, not misreported as a BP2 shared-key failure.

### 5.4 Scope, context, session, and no-context shape

- [ ] Exact scope is typed from `tenantId + userId + deviceId`; exact fields cannot be null/blank aliases of no-context.
- [ ] `effectiveContext(supplied)` preserves supplied-context precedence.
- [ ] Holder is consulted only when the supplied context is null.
- [ ] Supplied-null plus a present holder uses the holder's exact scope/window/session.
- [ ] Supplied-null plus an empty holder uses one explicit typed no-context variant.
- [ ] A present context with unavailable/invalid authority is not broad-caught into no-context, bare-window, player-only, or default state.
- [ ] Same scope plus the same explicit local-team session can share leader/member capability and claim state.
- [ ] Same scope with no explicit local-team session isolates different windows.
- [ ] Different tenant, user, or device isolates equal window/team/session/round text.
- [ ] Maintenance-key fallback order remains: explicit key -> requested task code -> task code -> existing default.
- [ ] Shared paths perform one typed lookup/update decision only; there is no exact-plus-fallback dual lookup or dual write.

### 5.5 No delimiter/prefix compatibility path in BP2-owned domains

- [ ] Formal team round/window/claim paths contain no `team + "#" + round` identity.
- [ ] Local capability claims contain no `local-team:` prefix identity.
- [ ] Claim owners contain no `scope|window` tuple identity.
- [ ] Shared-key pruning contains no `startsWith`, `substring`, or `Integer.parseInt` identity reconstruction.
- [ ] Identifiers containing `|`, `#`, or `local-team:` remain ordinary record field values and cannot collide.
- [ ] No compatibility alias, raw global key, prefix scan, second map, or fallback lookup remains in the four shared domains.

### 5.6 Claim branch source preservation

- [ ] Formal claim acquisition uses one typed `TeamRoundKey` path.
- [ ] Local capability claim acquisition uses one typed local capability/epoch path.
- [ ] Same-window duplicate detection remains in the same relative branch.
- [ ] Effective max-cleaner calculation remains `max(1, configured)`.
- [ ] Known failure with no state change releases only the caller's typed claim.
- [ ] Delete/ultimate state change retains the claim as before.
- [ ] Older formal-round pruning removes only older rounds of the same scoped team key.
- [ ] Formal pruning cannot remove local capability claims.
- [ ] No claim TTL, lease, owner authority, reference count, compaction pass, or background cleanup was added.

## 6. Public API compatibility checklist

- [ ] `public void initializeForTaskStart(TaskExecutionContext, String)`.
- [ ] `public void beginTeamMaintenanceRound(TaskExecutionContext, String, int, String)`.
- [ ] `public void openTeamPathingMaintenanceWindow(TaskExecutionContext, String, int, String)`.
- [ ] `public void openTeamFirstAidMaintenanceWindow(TaskExecutionContext, String, int, String)`.
- [ ] `public void closeTeamMaintenanceWindow(TaskExecutionContext, String, int, String)`.
- [ ] `public void openLocalTeamReturnSupportWindow(TaskExecutionContext, String)`.
- [ ] `public void closeLocalTeamReturnSupportWindow(TaskExecutionContext, String)`.
- [ ] `public boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext, String)`.
- [ ] `[34A] public boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext, String, long)`.
- [ ] `[34A] public boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext, TeamSupportCapability, long)`.
- [ ] `[34A] public boolean isLocalSupportMemberSession(TaskExecutionContext)`.
- [ ] `public void registerLocalTeamSessionCandidate(String, Collection<String>, String)`.
- [ ] `public void markLocalTeamWindowRoleDetected(TaskExecutionContext, String, String, String)`.
- [ ] `[34A] public boolean isLocalSupportMemberCandidate(TaskExecutionContext)`.
- [ ] `[34A] public boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext)`.
- [ ] `public void markLocalTeamLeaderDetected(TaskExecutionContext, String, String)`.
- [ ] `[34A] public boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext, TeamSupportCapability)`.
- [ ] `public void completeLocalTeamSessionWindow(String, String, String)`.
- [ ] `public TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext, TaskMaintenanceRequest)`.
- [ ] Confirm exactly 19 public instance methods: no addition, removal, visibility change, overload, parameter reorder, return-type change, checked-exception change, or wait-semantic change.
- [ ] Confirm the five constructor collaborators and their Lombok constructor order remain `BotProperties`, `GameContext`, `DialogService`, `SummonSkillService`, `TaskExecutionContextHolder`.
- [ ] Confirm the six `[34A]` methods remain source-compatible with all current `AutoCombatService` call sites.
- [ ] Confirm the four zero-production-caller lifecycle APIs gain no production host/factory/runtime/Task caller.

## 7. `696a12b0` business-order source checklist

- [ ] `runOpportunisticMaintenance` remains `normalize -> first checkpoint -> optional broadcast`.
- [ ] Broadcast `handled`, `BROADCAST_FAILED`, and `INTERRUPTED` still short-circuit before Summon.
- [ ] Only an eligible non-short-circuited pass may reach at most one Summon delegate.
- [ ] No-action remains the final fall-through.
- [ ] Summon gates remain in order: feature -> interval -> FREE -> due -> UNKNOWN retry-after -> existing 2h cache -> team/local round -> capability/pathing -> duplicate/max claim -> second checkpoint -> one delegate.
- [ ] CommonBox keeps its existing higher maintenance priority; BP2 does not consume a box.
- [ ] TeamReturn remains capability-only; BP2 adds no TeamReturn input or business progression.
- [ ] Summon static-tail, skill-count, UNKNOWN, ultimate, cooldown, claim, and `GameContext.ActionState` projection remain unchanged.
- [ ] Capability sets remain exact: pathing opens `5`, first-aid opens `1`, team close closes those `5`, return support opens/closes `2`.
- [ ] Existing overlap remains unchanged: no new lease/reference-count semantics are introduced for shared `COMMON_BOX`.
- [ ] Existing 2-hour tail-safe and skill-count cache constants and comparisons remain source-identical; no test clock seam or replacement TTL is added.
- [ ] No phase, keep-turn/park, retry/fallback, verification count, delay, expiry, cleanup, or task decision moves into BP2.

## 8. Terminal, delegate, action, and UUID source checklist

- [ ] BP2 adds zero metadata reads.
- [ ] BP2 adds zero checkpoints.
- [ ] BP2 adds zero Dialog delegates.
- [ ] BP2 adds zero Summon delegates.
- [ ] BP2 adds zero `TurnGameClient.execute(...)` calls.
- [ ] BP2 adds zero `TurnAction` construction.
- [ ] BP2 adds zero actionId/UUID supplier/import/call.
- [ ] BP2 adds zero retry, replay, resend, sleep, timer, thread, queue, TTL, owner, lease, ledger, or durable workflow.
- [ ] Existing terminal/uncertain/stop exceptions are not caught and converted into false/success.
- [ ] Existing `finally` guarded `GameContext.ActionState` restoration remains unchanged.
- [ ] Source review distinguishes coordinator counts from delegate counts: eligible TURN-33 Summon may emit its own one action/UUID, while TaskMaintenance itself must emit none.

## 9. Evidence that remains source-review-only

- [ ] Exact BP2 write set, frozen/final SHAs, line/byte counts, and changed-method/type index.
- [ ] Four shared map generic declarations and private typed-key field shapes.
- [ ] Four per-window map declarations and deferred methods remaining byte-untouched.
- [ ] Private key types at file bottom and absence of wrapper/helper nesting.
- [ ] Absence of shared-domain delimiter/prefix parse, dual lookup/write, compatibility alias, or broad authority downgrade.
- [ ] Exact 19 public declarations, five constructor collaborators, and no new production caller for the four dormant lifecycle APIs.
- [ ] Source control-flow comparison for business order, capability sets, claim acquire/release/retain, and `ActionState` restoration.
- [ ] Zero new metadata-read/checkpoint/delegate/execute/action/UUID/retry/timer/TTL surface in the BP2 diff.
- [ ] Existing 2-hour cache constants/expiry comparisons remain source evidence because the frozen test boundary provides no clock seam and forbids a two-hour sleep/private-state reflection.
- [ ] These source facts are necessary but do not substitute for public behavioral tests.

## 10. Sole TURN-34B named-test checklist for BP2 behavior

### 10.1 Harness and sequencing

- [ ] Target the parent-received final BP3 production SHA, not BP2 start SHA or mid-WIP SHA.
- [ ] Create/extend only `TaskMaintenanceTurnContractTest.java`.
- [ ] Instantiate real public `TaskMaintenanceService` with test-private scripted collaborators.
- [ ] Use real `TaskExecutionContext.turnNative(...)`, service scope, invocation context, exact metadata, and counting command/UUID fixtures.
- [ ] Do not use Spring, HTTP, application/server/Task startup, Mockito, private-production reflection, source scan, wall-clock sleep, or fabricated service results.
- [ ] Serialize all test tranches by physical test-file handoff and record each target production/test SHA.

### 10.2 BP2 scope and shared-state behavior

- [ ] Change tenant only while keeping window/team/session/round text equal; formal/local state and claims remain isolated.
- [ ] Change user only with equal identifiers; state and claims remain isolated.
- [ ] Change device only with equal identifiers; state and claims remain isolated.
- [ ] Same scope, different windows, no explicit session: team/local state and claims do not merge.
- [ ] Same scope, same explicit local session, different windows: leader/member capability and local claim sharing follow the existing baseline.
- [ ] Same session text in different scopes remains isolated.
- [ ] A(scope) -> B(scope) -> A(scope) proves independent namespaces; B never sees A, and returning to A addresses A's own scope. This is scope isolation, not native-generation cleanup.
- [ ] Identifiers containing `|`, `#`, and `local-team:` prove different typed tuples do not collide.
- [ ] Formal `TeamRoundKey` and local capability claim keys never alias.
- [ ] Same-window duplicate, max-cleaner, known-failure release, and state-change retain produce the existing results.

### 10.3 Context authority and no-context behavior

- [ ] A supplied exact context beats a conflicting holder for scope, window, session, and claim decisions.
- [ ] Supplied null plus an exact holder uses the holder and does not enter no-context.
- [ ] Supplied null plus an empty holder uses only the explicit typed no-context namespace.
- [ ] A present context with authority failure propagates the existing typed failure and cannot read/write no-context state.
- [ ] The two public no-context APIs interoperate only inside their explicit no-context namespace when the holder is empty.
- [ ] Direct test calls to dormant lifecycle APIs do not create a production caller or activate runtime lifecycle.

### 10.4 Public API and 34A compatibility behavior

- [ ] Directly compile/call all 19 public APIs through typed Java calls; do not substitute a source scan.
- [ ] Exercise the six 34A APIs with open/closed state and timeout `0` behavior.
- [ ] Confirm no test changes `AutoCombatService` or its separate named test.
- [ ] Confirm all current AutoCombat call sites compile after the final Cloud compile gate.

## 11. Sole TURN-34B named-test checklist for baseline behavior

- [ ] Both maintenance request flags false -> exact no-action; Dialog=0, Summon=0, execute/action/UUID=0.
- [ ] Broadcast handled -> Dialog once, Summon zero, exact handled result.
- [ ] Broadcast failed -> Dialog once, Summon zero, exact failure projection.
- [ ] Broadcast interrupted -> Dialog once, Summon zero, not a business failure.
- [ ] Non-terminal broadcast miss plus eligible Summon -> ordered events `[dialog, summon]`, each once.
- [ ] Summon-only eligible pass -> Dialog zero, Summon delegate exactly once.
- [ ] Feature, interval, FREE, due, UNKNOWN retry-after, cache, team-round, capability/pathing, duplicate/max, and second-checkpoint gates are each proven in the frozen order.
- [ ] Success updates existing cooldown/cache/state once.
- [ ] Known failure without state change releases the typed claim and does not create success cooldown.
- [ ] Delete/ultimate state change retains the typed claim.
- [ ] UNKNOWN keeps the existing retry-after/cache-invalidation behavior without a new observation or TTL.
- [ ] Capability open/close matrix proves exact `5/1/5/2` sets and existing overlap behavior.
- [ ] CommonBox and TeamReturn remain capability facts only; no real box consumption or return input is reproduced.

## 12. Sole TURN-34B named-test checklist for terminal/UUID behavior

- [ ] Initial missing/device/window/title/HWND/process checkpoint failure stops before Dialog/Summon; delegate/execute/action/UUID counts are zero.
- [ ] Initial STOP propagates once before any maintenance state mutation or delegate.
- [ ] Broadcast short-circuit statuses never reach Summon or a second command.
- [ ] The second existing checkpoint can stop after claim/gates but before `INTERACTING`/Summon; no delegate or automatic retry follows.
- [ ] Eligible Summon calls `cleanSummonSkillsOnce(...)` at most once.
- [ ] Summon terminal/uncertain/stop channel propagates once; no false result, retry, replay, resend, or second delegate follows.
- [ ] `TaskMaintenanceService` itself consumes zero actionId/UUID in every branch.
- [ ] TURN-33 `SummonSkillTurnContractTest` remains responsible for the delegated whole-pass one-command/one-UUID and no-replay contract.
- [ ] TURN-22 tests remain responsible for TeamReturn input/queue mechanics; TURN-34B does not duplicate them.
- [ ] Prior `GameContext.ActionState` is restored only when the delegate leaves it at `INTERACTING`; a newer state is not overwritten.

## 13. A-B-A ownership split

- [x] BP1 owns same-context latest-metadata `A0 -> B -> A'`: after B, value-equal A' remains a typed window mismatch.
- [ ] BP1 final gate must run `TaskExecutionContextTurnContractTest` and retain zero command/action/UUID and one-read-per-checkpoint evidence.
- [ ] BP2 owns typed scope separation only; it must not claim to close native-generation lifetime.
- [ ] The BP2 test axis `scope A -> scope B -> scope A` proves namespace isolation only and must not assert old A state was purged.
- [ ] BP3 owns four per-window maps, exact native fingerprint, old-generation cleanup, and legitimate successive valid contexts on the same logical window.
- [ ] BP3 test axis `fingerprint A -> B -> A` must prove the third context creates fresh A state and cannot revive the first A cooldown/cache/claim/formal/local evidence.
- [ ] Same fingerprint `A -> A` must preserve state and avoid spurious cleanup.
- [ ] Leader-generation drift must revoke only evidence opened by that old leader; member drift must not clear a valid leader.
- [ ] BP3 must not add a generation history, counter, TTL, lease, owner, ledger, retry, timer, or second latest-metadata observation.
- [ ] No 34B test tranche may report the full A-B-A matrix complete before BP3 canonical delivery and parent source receipt.

## 14. BP3-deferred production checklist

- [ ] Convert the four per-window map keys to BP2's final real `ScopedWindowKey` type.
- [ ] Add only the minimal current native-fingerprint state needed for valid-context generation transitions.
- [ ] Resolve logical window as exact scope `(tenant,user,device) + windowId`.
- [ ] Resolve fingerprint from exact initial `title + HWND + processId` without calling `latestWindowMetadata()` again.
- [ ] On fingerprint change, atomically clear only old-generation TaskMaintenance state and install the new current fingerprint.
- [ ] Clear old per-window cooldown/not-due/UNKNOWN/state entries.
- [ ] Remove old-generation typed claim ownership and exact formal/local participation without scanning string prefixes.
- [ ] Preserve other scope/team/window/member state.
- [ ] Keep the first existing checkpoint before generation mutation and keep the second existing checkpoint before Summon delegate.
- [ ] Keep all 19 public APIs, six 34A APIs, five constructor collaborators, business order, `5/1/5/2`, and `696a12b0` semantics unchanged.

## 15. Final stable-writer gate checklist

- [ ] BP2 canonical production delivery has a parent-recomputed source receipt.
- [ ] BP3 starts only from that exact BP2 final SHA and later has its own canonical delivery/source receipt.
- [ ] The sole named test is created/updated only against the parent-received BP3 final production SHA.
- [ ] Parent reviews both production assertions and test assertion strength before command execution.
- [ ] Run `mvn -q -Dtest=TaskMaintenanceTurnContractTest test` from Cloud only in a stable-writer window.
- [ ] Record exact command, exit code, tests run, failures, and errors; do not use skip/enforcer bypasses or IDE-only evidence.
- [ ] Run the applicable Cloud compile gate after the named test and record its exact command/exit code.
- [ ] Keep Cloud full `clean package` behind its separate user authorization; do not infer authorization from this preflight.
- [ ] Keep TURN-22, TURN-33, TURN-34A six-API compatibility, BP1 named test/build, two independent reviews, and final parent judgment as separate TURN-34B gates.
- [ ] Do not start runtime/application/server/Task/UI/capture/input as part of this unit/contract gate.
- [ ] Final handoff records `无已批准业务差异；按 696a12b0 等价迁移`.

TRUE_EOF PRECHECK_COMPLETE
