# Cloud Task/Service Execution Context - Shared Work Log

> Append-only handoff for the external implementation worker and the local 5-minute monitor.
> Do not rewrite or delete earlier entries. Every design, repair, build result, blocker, and final
> conclusion must be appended here rather than left only in chat.

## Gate

- Task: establish the minimum Cloud-side execution-context/Service-host compatibility boundary
  needed to migrate existing `GameTask`/`TaskStep`/Service classes without moving local window,
  input, pause-token, or runner authority into Cloud.
- Status: `IMPLEMENTATION IN PROGRESS`.
- Host state: dormant. Do not construct/start a Task host, poller, UI, capture, or input path.
- Build gate: Cloud `mvn -q clean package`, without skipping tests. DHXY compile is required only
  if the approved design proves a local Java change is unavoidable.
- Test policy: do not add, restore, modify, or cite a new local test family.

## Frozen Inputs

- DHXY baseline: `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`.
- Cloud baseline: `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`.
- Both worktrees are heavily dirty. Preserve every unrelated/user change; no reset, checkout,
  clean, revert, add, commit, or push.
- Existing execution-context/lifecycle gates in
  `2026-07-12-cloud-execution-context-review.md` are CLOSED/APPROVED and frozen: stable retained
  request/action identity, immutable redelivery, trusted broker outcome, digest-covered
  `runRevision`, Cloud enqueue/final-dispatch fences, DHXY pre-side-effect/worker-admission fence,
  strict payload-before-ledger decoding, and package-internal client-ingress authority.

## Required Design Invariants

1. Do not exact-copy DHXY `TaskExecutionContext` or `WindowRuntimeContext`. They carry local
   `TaskStopToken`, `TaskPauseToken`, HWND/geometry, identity suspension, runner state, and other
   mechanical authority.
2. Existing Cloud `CloudTaskRunExecutionContext` and retained-authority `RemoteGameClientPort`
   remain the only route to client facts/capture/input. No public raw request, poll, or completion
   bypass may be introduced.
3. A future migrated Task/Service must receive one context tied to exact tenant/user/device/session,
   taskRunId, window tuple, stopEpoch, and runRevision. Lifecycle changes must stale the context;
   no TTL, takeover, auto retry, or replacement identity path may be added.
4. Cloud business context may carry immutable business metadata such as task code/name, startup
   mode, team-session fields, and startedAt, but it must not expose a fake local HWND/window runner
   object as authority.
5. Stop/pause behavior for Cloud orchestration must be expressed through the existing task-run
   coordinator/revision contract or a narrowly defined checkpoint collaborator. Do not move local
   thread interruption/pause token semantics into Cloud and do not alter existing business
   timeout/checkpoint meaning.
6. This slice must be minimal. Do not migrate a business Task/Service yet; do not edit phase,
   retry, fallback, OCR/template, click, navigation, sleep, or success conditions.
7. Before editing source, append `External Worker - Design #1` with: exact proposed types/files,
   caller graph, field/method compatibility table against DHXY `TaskExecutionContext`, authority
   analysis, and explicit deferred gaps. If a safe boundary cannot be implemented without broader
   semantic changes, append `BLOCKED` with evidence instead of improvising.

## Acceptance Evidence

- Exact changed-file list and why each file belongs to this boundary.
- Public/package API reachability proof: business/host code cannot access client ingress completion
  capabilities or construct an authorized context/retained action identity on its own.
- Compatibility table covering every `TaskExecutionContext` member used by currently remaining
  Task/Service sources, classifying it as Cloud business metadata, remote mechanical fact, local-only
  and deferred, or unsupported/fail-closed.
- Fresh Cloud `mvn -q clean package`: exit 0 and exact Surefire totals.
- No test-source changes, no host/poller/UI/capture/input execution, and no Git mutation.
- Final entry must be either explicit `READY FOR LOCAL REVIEW` with no known P0/P1/P2, or explicit
  `BLOCKED` with severity, evidence, impact, and repair/decision condition.

## Next Writer

The external worker owns this slice. It must keep reading this file while working and append new
material here. The local heartbeat reviews only newly appended material and will not accept a chat-
only conclusion.

## External Worker - Design #1 (2026-07-12)

### Current write gate

- Parent requested an immediate Java-write pause while migration waves 3/4 are integrated and one
  Cloud clean package is running. This entry is documentation-only. No Cloud or DHXY Java file has
  been changed by this worker. Implementation remains frozen until the parent appends or sends
  `package 完成，可继续实现`.
- Read baseline completed: `AGENTS.md`, full `docs/DHXY_CONTEXT.md`, this log,
  `docs/ACTIVE_WORK.md` top CR271 slice, both worktree statuses, all required Cloud authority/host
  files, DHXY `TaskExecutionContext`/`GameTask`/`TaskStep`/`TaskCheckpoint`/`TaskSleep`, and the
  current Task/Service call surface. Both worktrees are heavily dirty and remain protected.

### Design invariants

1. The Cloud compatibility type must be backed by the already non-mintable
   `CloudTaskRunExecutionContext`; metadata alone never authorizes a run or a client operation.
2. No Cloud compatibility type contains or exposes local `TaskStopToken`, `TaskPauseToken`,
   `WindowRuntimeContext`, HWND object, input queue, thread interruption authority, or identity-
   suspension waiter. The already copied Cloud `TaskStopToken` remains unused by this boundary.
3. Exact remote `taskRunId` remains a `String`. The local process-only `long taskRunId` sequence is
   not recreated, parsed from UUID text, hashed, or treated as a second run authority. Existing
   numeric callers must be adapted explicitly in their owner migration slice.
4. `windowId`, normalized native-handle text, process id, player-identity epoch, stop epoch, and
   run revision are read-only projections of the exact coordinator snapshot. Title, class name,
   x/y/width/height are not cached into the context; they remain live remote `BINDING`/`GEOMETRY`
   facts obtained through retained actions.
5. Safe business metadata may include task/requested-task names/codes, window role, local-team
   session fields, retry policy, startup mode, and startedAt. The requested task code must equal the
   coordinator binding's exact `taskType`; other display/business metadata grants no authority.
6. A lifecycle transition permanently stales the wrapped run snapshot. This slice does not fake
   local pause-wait behavior. `throwIfStopRequested`, `isStopRequested`, `isPauseRequested`, and
   `TaskSleep` compatibility stay deliberately absent until a reviewed activation/rehydration
   design can preserve pause compensation and resume at the same persisted business phase.
7. The compatibility wrapper does not expose `RemoteGameClientPort`, the action ledger, or an
   arbitrary action-key acquire API. Future mechanical adapters inside the trusted `remote` package
   may use package-private access to the same assembly's port/ledger, but migrated business/host
   code cannot mint retained identities or submit raw requests.
8. Per-run context is an explicit method argument, never a singleton or mutable Spring bean inside
   `CloudServiceHost`. One tenant/user host may later serve multiple exact device/session/window
   runs without replacing host-global state.
9. This slice creates no Task, thread, poller, scheduler, HTTP route, capture, or input action and
   changes no phase/retry/fallback/sleep/click/navigation/lifecycle transition semantics.

### Exact proposed Java files

1. **Create**
   `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java`
   - Immutable, powerless business metadata record.
   - Fields: `taskCode`, `taskName`, `requestedTaskCode`, `requestedTaskName`, `windowRole`,
     `localTeamSessionKey`, `localLeaderWindowId`, `localLeaderPresent`, `localSupportMember`,
     `TaskRetryPolicy`, `TaskStartupMode`, `LocalDateTime startedAt`.
2. **Create**
   `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`
   - Package-private constructor; wraps exact `CloudTaskRunExecutionContext`, the existing gate,
     same-assembly command port/ledger, and metadata.
   - Public surface is read-only identity/metadata plus one atomic `revalidate()` snapshot. Raw run
     context, port, and ledger accessors remain package-private for a later retained-action adapter.
3. **Create**
   `dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
   - Cloud-only compatibility wrapper under the original FQCN so safe-subset Services can migrate
     without changing their parameter/class boundary.
   - Constructor accepts the non-mintable `CloudTaskServiceExecutionContext`; callers cannot build
     an authorized wrapper from metadata alone.
   - Exposes only the safe subset listed below. It intentionally has no Lombok builder and no local
     token/runtime/geometry methods.
4. **Modify**
   `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java`
   - Add one package-private `createTaskExecutionContext(scope, taskRunId, metadata)` path.
   - It calls the existing gate's `createContext`, verifies exact requested-task-code/taskType, then
     builds the unforgeable service context and original-FQCN compatibility wrapper from this same
     assembly's gate, command executor, and retained ledger.

No change is proposed for `RemoteTaskRunRoutes`, `CloudServiceHost`,
`CloudServiceConfiguration`, `CloudBrainServer`, DHXY Java, or any test source in this slice.

### Caller and authority graph

```text
future authenticated activation adapter (deferred; trusted remote package)
  -> CloudTaskRunAuthorityAssembly.createTaskExecutionContext(...)
       -> CloudTaskRunExecutionGate.createContext(scope, taskRunId)
            -> coordinator.find + coordinator.authorize exact confirmed-ACTIVE revision
       -> CloudTaskServiceExecutionContext (non-public construction)
       -> com.bot.dhxy.runner.context.TaskExecutionContext safe wrapper
            -> future migrated GameTask / TaskStep / Service explicit argument

future retained-action state adapter (deferred; trusted remote package)
  -> package-private serviceContext.runContext/port/ledger
  -> current retained identity only
  -> RemoteGameClientPort
  -> CloudTaskRunCommandExecutor
  -> gate request build + broker enqueue/final-dispatch fences
  -> local pre-side-effect/worker-admission revision fences

remote client ingress
  -> private route endpoints -> package-private broker poll/outcome
  (no reference reaches Task/Service/host compatibility objects)
```

### TaskExecutionContext compatibility table

| DHXY member / observed use | Cloud classification | Design #1 behavior |
|---|---|---|
| `taskCode`, `taskName` | business metadata | Compatible getters. `taskCode` is descriptive execution code, not run authority. Direct Task/Service users include AutoCombat, Navigation, PlayerState, Maintenance, TeamReturn and pause reconciliation. |
| `requestedTaskCode`, `requestedTaskName` | business metadata + binding cross-check | Compatible getters; `requestedTaskCode` must exactly equal coordinator `taskType`. Used by AutoCombat, LeftTopStatus, Maintenance, TeamReturn and AutoBattle. |
| `windowId` | exact authority projection | Compatible getter from `RemoteTaskRunWindow.windowId`, never caller metadata. Widely used for logs, maps and team keys. |
| `windowRole` | business metadata | Compatible getter. It is not accepted as mechanical/window authority. |
| `nativeWindowHandle` | exact authority projection | Compatible string getter from the bound tuple, usable only as identity/log text. No JNA/HWND object is exposed. |
| `nativeWindowProcessId` | exact authority projection | Compatible long getter from the bound tuple. |
| `nativeWindowTitle`, `nativeWindowClassName` | remote mechanical fact | Absent. Callers such as TeamReturn/BaseTask/Xiuluo must migrate to a retained `BINDING` fact adapter. |
| `nativeWindowX/Y/Width/Height`, `hasNativeWindowGeometry`, `getNativeWindowGeometryText` | remote mechanical fact | Absent. PlayerState/image consumers must migrate to one retained `GEOMETRY` fact and use its declared coordinate space. |
| `localTeamSessionKey`, `localLeaderWindowId`, `localLeaderPresent`, `localSupportMember`, `hasLocalTeamSession` | immutable business metadata | Compatible getters/helpers. No local runner/window object is retained. |
| `retryPolicy` | immutable business policy | Compatible getter using the already copied `TaskRetryPolicy`; the context does not execute retries. |
| local `long taskRunId` | incompatible process-local diagnostic sequence | Replaced by exact remote `String getTaskRunId()`. Numeric comparisons/`Long.toString` in CommonBox, Navigation, NpcClick, PlayerState, ReturnItem, Maintenance, TeamReturn and the three main Tasks must be adapted explicitly; compile failure is preferred to a fake numeric identity. |
| `startupMode`, `isAfterCombatExitStartup`, `isCleanQueueTransitionStartup` | immutable business metadata | Compatible getter/helpers using copied `TaskStartupMode`; no phase transition is performed here. |
| `startedAt` | immutable business metadata | Compatible `LocalDateTime` getter; timeout owners remain responsible for pause accounting. |
| `hasWindow`, `hasNativeWindow`, `getLogPrefix` | pure projections/formatting | Compatible helpers derived only from exact binding plus role metadata. |
| `stopToken`, `pauseToken`, `isStopRequested`, `isPauseRequested`, `throwIfStopRequested` | local runner authority / pause semantics | Absent and deferred. A read-only `revalidate()` can report current vs stale exact revision, but it is explicitly not advertised as the old cooperative pause checkpoint. |
| `windowRuntimeContext` | local mutable runner/window authority | Absent and deferred. Current ReturnItem, TeamReturn, pause reconciler, Wubei, Wuhuan and Xiuluo paths that dereference it cannot migrate in this slice. |
| `TaskCheckpoint` / `TaskSleep` integration | orchestration boundary | Deferred. Copying them would reintroduce thread interruption and local pause tokens; replacing pause-wait with throw/stop would change business timeout and resume semantics. |
| `GameTask.execute(TaskExecutionContext)` / `TaskStep.execute(TaskExecutionContext)` | class-boundary target | Signature can remain for safe-subset classes because Cloud supplies the same FQCN. Main Task execution remains blocked until checkpoint and mutable window-state dependencies are mapped. |

Unused local members are still classified rather than silently copied: `getPauseToken`,
`isPauseRequested`, native title/class/x/y, and requested task name currently have no direct main
Task/Service call or only runner/diagnostic use, but remain absent or metadata-only according to the
same authority rule.

### Authority reachability

- `RemoteGameCommandBroker`, `CloudTaskRunAuthorityAssembly`, and the command executor remain
  package-private. Poll/outcome endpoint implementations remain private nested route types.
- The new assembly factory is package-private. `CloudTaskServiceMetadata` is public but powerless.
- `CloudTaskServiceExecutionContext` has no public constructor. The original-FQCN wrapper can only
  be constructed with that non-mintable object and does not expose it afterwards.
- Business/host code receives no ledger `acquire`, no current-identity mint/renew method, no raw
  request constructor path, no broker, and no poll/outcome completion capability.
- Package-private raw run/port/ledger references are reserved for a later reviewed action-state
  adapter inside the same trusted package. This slice does not create that adapter or any action.

### Explicit deferred gaps

1. **Activation bridge:** `RemoteTaskRunRoutes.create` currently returns only opaque routes and does
   not expose the same coordinator/broker authority assembly to `CloudServiceHost`. Joining an
   authenticated route runtime to a tenant/user host requires a separate reviewed activation slice;
   this design does not create a second coordinator or secretly activate a host.
2. **Retained action state:** migrated services still need an internal adapter that owns canonical
   business-action keys and hands them existing retained handles. It must not expose free-form
   public identity minting. No screenshot/fact/input Service is runtime-ready before that adapter.
3. **Pause/resume rehydration:** every accepted lifecycle transition stales the revision snapshot.
   Resume must restore the persisted Task phase under a newly minted context while excluding the
   confirmed pause interval from existing business watchdogs. This cannot be faked by copying
   `TaskPauseToken`, waiting on a Cloud thread token, or continuing an old context.
4. **Window facts:** title/class/geometry consumers need typed retained fact adapters; no cached
   geometry is added to context metadata.
5. **Mutable runner state:** every `getWindowRuntimeContext()` caller needs an owner-specific Cloud
   state/ready-event mapping before migration. The compatibility wrapper deliberately fails to
   compile those callers today.
6. **Numeric run id:** all local `long` assumptions need owner-level edits to use the exact remote
   string; no compatibility hash/surrogate will be introduced.
7. **Sleep/checkpoint:** Task checkpoint and sleep migration remains blocked on item 3. Existing
   phase/retry/fallback/sleep order is frozen meanwhile.

### Design disposition

The minimum immutable context boundary is implementable without copying local authority or changing
business behavior, provided the absent/deferred members remain hard compile-time gaps. No known
P0/P1/P2 exists in this design itself. Java implementation is intentionally paused for the parent's
waves 3/4 package and may begin only after the explicit release message.

## Local Review #1 - BLOCKED (2026-07-12T20:59:10.966Z)

Review scope: complete `External Worker - Design #1`, the currently materialized four proposed
Cloud Java files, DHXY `WindowTaskRunner` role-preflight/context construction, the approved
`TaskTeamAssignmentPolicy`, and the existing `AutoBattleTask` consumer that distinguishes requested
from effective task identity. No new reviewer/agent was created.

### P1 - `requestedTaskCode == binding.taskType` rejects valid role-based task reassignment

- **Evidence:** DHXY `WindowTaskRunner.runQueueWithBoundGameState` first keeps
  `requestedTaskType`, then calls `resolveTaskTypeBeforeStart` and constructs the actual `GameTask`
  from the resolved `taskType` (`WindowTaskRunner.java:585-608`).
  `TaskTeamAssignmentPolicy.resolveTaskForRole` deliberately maps a member window requesting
  `WUHuan_V2`, `WUBEI`, `XIULUO`, or `XIULUO_V2` to `AUTO_BATTLE`. The final context is then built
  from both values: concrete `task.getTaskCode()/getTaskName()` and the original
  `requestedTaskType` (`WindowTaskRunner.java:627,3927-3961`). `AutoBattleTask.java:325-328`
  explicitly relies on `requestedTaskCode != getTaskCode()` to recognize this support mode.
- **Conflicting design/source:** Design #1 says `requestedTaskCode` must equal coordinator
  `taskType`; the current `CloudTaskServiceExecutionContext.java:37-39` already implements that
  condition. A member request such as `requested=wubei`, `effective=auto_battle` therefore cannot
  obtain a Cloud Task/Service context if the coordinator binding correctly names the effective task.
- **Impact:** legitimate multi-window leader/member dispatch is rejected before migration starts,
  or an implementer is pushed to bind the coordinator to the requested leader task while actually
  running auto-battle. The latter creates two task authorities and breaks task-type diagnostics and
  future lifecycle/action ownership. This is a business-behavior regression, so the design and
  current implementation cannot pass.

### Required repair / next B-PASS condition

1. Define coordinator `binding.taskType` as the **effective concrete task code** selected after the
   role-assignment policy, and validate it exactly against `metadata.taskCode`, not
   `metadata.requestedTaskCode`.
2. Preserve `requestedTaskCode` separately and exactly as powerless origin metadata. It is allowed
   to differ from `taskCode` for the existing member-to-`AUTO_BATTLE` path; do not trim, normalize,
   overwrite, or infer it from the binding.
3. Update the metadata JavaDoc, compatibility table, authority graph/invariants, constructor check,
   and assembly documentation consistently. A mismatch between authoritative `taskType` and
   concrete `taskCode` must still fail closed before the wrapper is returned.
4. Record that the future dormant activation adapter must PREPARE/activate only after the effective
   task is selected. Do not activate a host or invent a second lifecycle run in this repair.
5. Append `External Worker - Repair #1` with exact diff/build evidence, no test-source changes, and
   fresh Cloud `mvn -q clean package` totals. Existing no-test and dormant-host constraints remain.

Disposition: **BLOCKED**. `P0=0, P1=1, P2=0`. All other reviewed Design #1 authority boundaries are
acceptable at this stage; this review does not reopen the frozen retained-action/revision gates.

## External Worker - Repair #1 (2026-07-12)

### Review verification and corrected invariant

- The P1 was reproduced against the current DHXY source rather than accepted from the review text
  alone. `WindowTaskRunner` preserves `requestedTaskType`, resolves an effective `taskType` through
  `TaskTeamAssignmentPolicy`, constructs that effective `GameTask`, and only then builds the final
  context from `task.getTaskCode()/getTaskName()` plus the original requested task metadata.
- `TaskTeamAssignmentPolicy` intentionally maps member requests for Wuhuan/Wubei/Xiuluo to
  `AUTO_BATTLE`; `AutoBattleTask.isFollowerSupportMode` detects that valid path from
  `requestedTaskCode != getTaskCode()`. The previous Design #1 equality therefore contradicted the
  existing approved dispatch behavior.
- **Authoritative correction:** coordinator `binding.taskType` names the effective concrete Task
  that actually owns lifecycle/actions and must equal `metadata.taskCode` exactly. The original
  `requestedTaskCode` remains powerless, exact origin metadata and may differ. This section
  supersedes the conflicting Design #1 invariant, file description, and compatibility-table row;
  earlier append-only history is not rewritten.

### Corrected future activation order

```text
authenticated request + window binding
  -> role detection / TaskTeamAssignmentPolicy
  -> resolve effective concrete GameTask
  -> PREPARE exactly one coordinator run with taskType = effective task.getTaskCode()
  -> ACTIVATE + CONFIRM_EXECUTION for that same effective run
  -> metadata(taskCode=effective, requestedTaskCode=original request)
  -> CloudTaskRunAuthorityAssembly.createTaskExecutionContext(...)
       -> require binding.taskType == metadata.taskCode
       -> return immutable compatibility context
```

The dormant future adapter must not PREPARE before role assignment and must not create a second run
for the original requested leader task. This repair adds no activation adapter and starts no host.

### Corrected compatibility rows

| DHXY member | Correct Cloud classification | Repair #1 rule |
|---|---|---|
| `taskCode`, `taskName` | effective business identity + display metadata | `taskCode` is the concrete Task actually executed and is fail-closed cross-checked against authoritative coordinator `taskType`; `taskName` remains powerless display metadata. |
| `requestedTaskCode`, `requestedTaskName` | powerless origin metadata | Preserved exactly and independently. It may differ from effective `taskCode` for the existing member-to-`AUTO_BATTLE` assignment; it is never normalized, overwritten from binding, or used to authorize the run. |

All other Design #1 compatibility classifications and deferred gaps remain unchanged.

### Exact source diff

The allowed four-file scope was retained; no file, test, host, route, or DHXY Java scope was added.

1. `CloudTaskServiceMetadata.java`
   - JavaDoc now defines `taskCode` as effective and binding-matched.
   - JavaDoc now defines `requestedTaskCode` as the exact original request that may differ after
     role assignment. Record fields and validation remain unchanged, including preservation of
     original text without trim/normalization.
2. `CloudTaskServiceExecutionContext.java`
   - The sole functional repair changed
     `runContext.taskType().equals(metadata.requestedTaskCode())` to
     `runContext.taskType().equals(metadata.taskCode())`.
   - A mismatch still throws before the compatibility wrapper is returned; no port/ledger/request
     capability changed.
3. `com.bot.dhxy.runner.context.TaskExecutionContext.java`
   - Getter JavaDoc now distinguishes effective binding-checked `getTaskCode()` from original
     `getRequestedTaskCode()`. Signatures and returned fields are unchanged.
4. `CloudTaskRunAuthorityAssembly.java`
   - Factory JavaDoc now freezes `role assignment -> effective task -> PREPARE` and original request
     metadata separation. The factory remains package-private and host-dormant.

### Fresh build and reachability evidence

- Valid post-repair command: Cloud `mvn -q clean package`, without skip flags, exit `0` in `62.7s`.
  The earlier parent waves 3/4 package was not used as this repair's evidence.
- Fresh Surefire XML totals: `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`:
  `CloudBrainTemplateResourceGuardTest=1`, `IncenseStatusCloudBrainContractTest=11`,
  `WuhuanRouteCandidateContractTest=1`, `XiuluoCloudBrainContractTest=8`.
- The shaded JAR contains all four boundary classes.
- Post-repair `javap -public CloudTaskServiceExecutionContext` still shows no public constructor and
  only read-only revalidation/identity/metadata methods; it exposes no command port, ledger,
  request builder, poll, or outcome completion method.
- `javap -p CloudTaskRunAuthorityAssembly` confirms the class and
  `createTaskExecutionContext(...)` remain package-private. Existing package-private port/ledger
  access is unchanged and reserved for the deferred retained-action adapter.
- Scoped `git status` shows only the four expected untracked Cloud source paths; `src/test` has no
  scoped change. No test source was added/restored/modified. DHXY Java was not modified.
- No Task host, poller, UI, capture, input, lifecycle action, or production switch was started or
  invoked. No `git add`, commit, push, checkout, reset, clean, or revert was executed.

### READY FOR LOCAL REVIEW

Repair #1 closes the known P1 while preserving the frozen stable-ID, immutable-redelivery, trusted-
outcome, and runRevision fences. Worker conclusion is **READY FOR LOCAL REVIEW** with no known open
`P0/P1/P2`; this is not an APPROVED judgment. Deferred activation, retained-action, pause-resume,
window-fact, mutable-runner-state, numeric-run-id, and sleep/checkpoint gaps remain explicit future
slices and no runtime readiness is claimed.

## Local Review #2 - APPROVED (2026-07-12T21:10:47.217Z)

Review scope: full Repair #1 entry and current four-file Cloud implementation; DHXY role-assignment
and `TaskExecutionContext` baseline; existing execution-gate/coordinator/retained-port authority;
public/package API reachability; test-source status; fresh Cloud build and shaded artifact.

### Findings

- The prior P1 is closed. `CloudTaskServiceExecutionContext` now rejects only
  `runContext.taskType != metadata.taskCode`; original `requestedTaskCode` is preserved separately
  and may differ for the approved member-to-`AUTO_BATTLE` path. JavaDoc, compatibility rows, and
  dormant activation order state the same effective-vs-requested contract.
- Exact scope, taskRunId, window tuple, stopEpoch, and runRevision still come only from the
  coordinator-authorized `CloudTaskRunExecutionContext`. A lifecycle change permanently stales the
  snapshot through the existing gate/coordinator revision check.
- No local `WindowRuntimeContext`, `TaskPauseToken`, HWND object, geometry cache, input queue,
  thread interruption, or runner authority was copied. Missing stop/pause/window-runtime methods
  remain compile-time migration gaps rather than changed semantics.
- `CloudTaskServiceExecutionContext` has no public constructor. `CloudTaskRunAuthorityAssembly`
  and `createTaskExecutionContext` remain package-private; business/host code receives no ledger
  acquire, raw request, broker poll, or outcome-completion surface. Mechanical facts/actions remain
  reserved for the same-assembly retained-authority `RemoteGameClientPort` path.
- Current call-site scan finds no host/route/Task construction or execution entry for the four new
  types. Task host, poller, UI, capture, and input remain dormant. The compatibility table covers
  every local `TaskExecutionContext` field/helper and explicitly classifies deferred members.
- `git status --short -- src/test` is empty. No local test source was added, restored, or modified.

### Independent verification

- Local reviewer ran fresh Cloud `mvn -q clean package` without skip flags: exit `0` in `58.9s`.
- Parsed Surefire totals: `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`.
- Fresh shaded JAR contains `CloudTaskServiceMetadata`, `CloudTaskServiceExecutionContext`,
  original-FQCN `TaskExecutionContext`, and `CloudTaskRunAuthorityAssembly`.
- `javap -public CloudTaskServiceExecutionContext` shows only read-only identity/metadata and
  revalidation methods, with no public constructor. `javap -p CloudTaskRunAuthorityAssembly`
  confirms package-private class/factory and no new public capability.

Disposition: **APPROVED**. `P0=0, P1=0, P2=0`. This approval closes only the minimum dormant
Task/Service context compatibility boundary. Activation, retained-action adapters, pause/resume
rehydration, typed window facts, mutable runner-state replacement, numeric-run-id callers, and
checkpoint/sleep remain separate future slices; no runtime or production-cutover approval is made.
