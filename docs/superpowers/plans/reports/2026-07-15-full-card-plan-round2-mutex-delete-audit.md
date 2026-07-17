# CR271 Full Card Plan Round 2 Mutex/Delete PRECHECK

> Status: `PRECHECK_ONLY / NON_BINDING`  
> Scope: `TURN-00..47`, proposed `TURN-13H`, and the split cards required to make the plan file-exclusive and
> compile-preserving.  
> This report is not an approval, rejection, CR decision, or implementation completion record.

## 1. Audit boundary and source facts

This pass read the authoritative card plan, the first-round cutover audit, the TURN-13H readiness report, and the
current DHXY/Cloud Java reference graph. It did not modify Java, the main plan, CR271, the migration matrix, the
dashboard, or Git state, and it did not run Maven, tests, runtime, server, UI, capture, or input.

Path aliases used below:

- `D:` = `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/`
- `C:` = `D:/mavenProject/dhxy-cloud-brain/src/main/java/`
- `CT:` = `C:com/yueyunfe/dhxy/cloudbrain/turn/`
- `CR:` = `C:com/yueyunfe/dhxy/cloudbrain/remote/`
- `CH:` = `C:com/yueyunfe/dhxy/cloudbrain/host/`

Observed facts that constrain the schedule:

1. `CloudBrainServer` still constructs `RemoteTaskRunRoutes`, and `RemoteTaskRunRoutes.create(...)` constructs
   `RemoteGameCommandBroker`, `CloudTaskRunActionLedger`, `RemoteFinalConsumptionCoordinator`, and
   `CloudTaskRunAuthorityAssembly`. Deleting the authority graph before severing those routes cannot preserve a
   compiling intermediate tree.
2. DHXY `LeaderPrecheckMechanics`, `BoundLeaderPrecheckCaptureCapability`, and
   `LocalRemoteGameCommandHandler` still reference the old lifecycle/registry/ledger graph. Deleting TURN-42
   providers before TURN-43 consumers is therefore also not compile-preserving.
3. `TaskExecutionContext` still wraps `CloudTaskServiceExecutionContext` and exposes `CloudGameClient` /
   `CloudTaskServicePort`. `TaskExecutionContextHolder` calls `TaskExecutionContext.isPauseRequested()`, which the
   current Cloud context source does not expose. The current late `TURN-38 -> TURN-39` order cannot be used as an
   early compiling foundation for TURN-14..37.
4. No source constructs Wubei/FiveRing/Xiuluo for the new turn path, and no source calls
   `CloudServiceHost.create(...)`. `TurnRequest`/`TurnWindowMetadata` also carries no task type. TURN-40 therefore
   lacks a frozen task-selection and host-construction path.
5. TURN-13H is still an unmaterialized prerequisite: `CloudTurnRoutes.Bundle.commandPort()` is package-private,
   `CloudBrainServer` does not retain it, and `CloudServiceHost` cannot inject it into the dormant service graph.

## 2. PRECHECK conclusions

1. Keep TURN-00..13 closed/source-frozen. Their declared write sets are mutually coherent; do not reopen them merely
   to solve later integration.
2. Materialize TURN-13H before any Cloud business adapter. It is a four-file shared integration card and must be
   serial with every other `CloudBrainServer` or `CloudServiceConfiguration` writer.
3. Move the create-only half of TURN-39 and the compatibility half of TURN-38 before TURN-14. The current
   TURN-38/39 placement creates a dependency cycle.
4. Materialize TURN-34A/B/C as three file-exclusive cards. Never dispatch two method cards against the same Java
   file concurrently.
5. Split TURN-38 by independent state ownership. Do not keep thirteen unrelated files in one late card, and do not
   rewrite authority-bound state classes without all direct consumers in the same write set.
6. TURN-40 needs a parent design freeze for task identity, authenticated scope/state root, Task construction, and
   close semantics. Its current four-file write set is not exact enough to dispatch.
7. Produce exact deletion manifests before any delete. The manifests must expand every glob and folder into files,
   record all production references, and classify each file as `KEEP`, `REWIRE`, or `DELETE`.
8. Reverse the effective Cloud deletion order: route sever first, authority/facade SCC second, broker/wire providers
   last. Current standalone TURN-44 followed by TURN-45 has no reliable compile point between them.
9. Interleave the DHXY deletion order: consumer sever (`TURN-43A`) first, transport/lifecycle SCC (`TURN-42`) second,
   residual DTO/mechanics cleanup (`TURN-43B`) last. Current TURN-42 followed by TURN-43 has the same provider-first
   problem.
10. Three implementation lines are safe through most business waves. During actual deletion, two Java lines are the
    honest maximum; a third line may perform a distinct read-only manifest/recheck but must not create a fake source
    card.

## 3. TURN-00..13 exact mutex audit

The following is the safe three-line historical ordering. These cards are already closed or source-frozen; the table
is a mutex audit, not a request to rerun them.

| Card | Exact write set | Required predecessor | Mutex / safe wave |
|---|---|---|---|
| TURN-00 | protocol design spec; foundation plan | none | docs-only; wave F0 |
| TURN-01A | both repos: `TurnStepType`, `TurnInputAction`, `TurnLocalOperation`, `TurnRegion`, `TurnWindowRect`, `TurnWindowMetadata`, `TurnFramePurpose` | 00 | parallel with 01B/01C; F1 |
| TURN-01B | both repos: `TurnAction`, `TurnStep`, `TurnInputSpec`, `TurnCaptureSpec`, `TurnMatchSpec`, `TurnLocalServiceCall`, four typed argument DTO families | 00 | parallel with 01A/01C; F1 |
| TURN-01C | both repos: `TurnOutcome`, `TurnStepResult`, `TurnMatchResult`, `TurnFrameMetadata`, `TurnRequest`, `TurnResponse` | 00 | parallel with 01A/01B; F1 |
| TURN-01D | both repos: `TurnProtocolValidator.java` | 01A/B/C | sole validator owner; F2 |
| TURN-02 | `CT:CloudTurnExchange`, `CloudTurnCommandPort`, `CloudTurnCommandResult`, `CloudTurnFrame`, `CloudTurnActionFactory` | 01D for integration | parallel with 03A; F2 |
| TURN-03A | `CT:CloudTemplateCatalog.java` | 00 | parallel with 02; F2 |
| TURN-03B | `CT:CloudTemplateHttpHandler.java` | 03A | parallel with 04/06; F3 |
| TURN-04 | `CT:TurnMultipartReader.java`, `CloudTurnHttpHandler.java` | 02,01D | parallel with 03B/06; F3 |
| TURN-05 | `CT:CloudTurnRoutes.java`, `C:com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java` | 03B,04 | shared Cloud integration; F4 |
| TURN-06 | `D:cloud/turn/TurnClient`, `HttpsTurnClient`, `TurnMultipartBody`, `TurnExchangeResult`, `TurnTemplateDownload`, `TurnTransportException` | 01D | parallel with 03B/04; F3 |
| TURN-07 | `D:cloud/turn/TurnTemplateCache.java` | 03B,06 | parallel with 05/08A; F4 |
| TURN-08A | `D:cloud/turn/TurnExecutionWindow`, `TurnFrame`, `TurnPngCodec`, `TurnCaptureStepExecutor` | 01D | parallel with 05/07; F4 |
| TURN-08B | `D:cloud/turn/TurnMatchStepExecutor.java` | 07,08A | parallel with 09/10P; F5 |
| TURN-09 | `D:cloud/turn/TurnInputStepExecutor`, `TurnInputActionMapper`, `TurnKeyMapper` | 01D | parallel with 08B/10P; F5 |
| TURN-10P | `D:cloud/turn/LocalServiceExecution.java` | 01C,08A | parallel with 08B/09; F5 |
| TURN-10A | `D:cloud/turn/local/BagLocalOperationExecutor.java` | 10P | parallel with 10B/10C; F6 |
| TURN-10B | `D:cloud/turn/local/UiLocalOperationExecutor.java` | 10P | parallel with 10A/10C; F6 |
| TURN-10C | `D:cloud/turn/local/GiveItemLocalOperationExecutor.java` | 10P | parallel with 10A/10B; F6 |
| TURN-10D | `D:cloud/turn/local/QuestLocalOperationExecutor.java`; its already-reviewed prerequisite owns only `QuestDetailCapture.java` and local `QuestManagerService.java` | 10P | separate from 10A/B/C; F7 |
| TURN-10E | `D:cloud/turn/LocalServiceStepDispatcher.java` | 10A/B/C/D | sole dispatcher integration; F8 |
| TURN-11 | `D:cloud/turn/LocalTurnActionExecutor`, `ExecutedTurn`, `TurnOutcomeAssembler`, `TurnStepExecution` | 08B,09,10E | sole action integration; F9 |
| TURN-12 | `D:cloud/turn/WindowTurnLoop`, `TurnLoopRegistry`, `TurnLoopFactory` | 06,11 | sole loop lifecycle owner; F10 |
| TURN-13 | `D:cloud/turn/TurnClientProperties`, `TurnConfiguration`, `TurnModeGuard`; `D:window/control/WindowTaskControlService`; `application.properties` | 05,12 | shared DHXY wiring; F11 |

### Proposed TURN-13H exact card

`TURN-13H` should be materialized as one inert Cloud construction-boundary card:

- Write set:
  - `CT:CloudTurnRoutes.java`
  - `C:com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`
  - `CH:CloudServiceHost.java`
  - `CH:CloudServiceConfiguration.java`
- dependsOn: TURN-05 and TURN-13 source completion.
- It must only expose/retain the same `CloudTurnCommandPort`, require it in
  `CloudServiceHost.create(scope,stateRoot,commandPort)`, register it as a Bean, and scan the dedicated turn-client
  package. It must not start a host, Task, thread, loop, or server path.
- Same-file order: `TURN-05 -> TURN-13H -> TURN-40 Cloud activation -> TURN-45A route sever` for
  `CloudBrainServer`; `TURN-13H -> TURN-38B4` for `CloudServiceConfiguration`.

## 4. TURN-14..37 write-set and same-file order

All cards in this section should also depend transitively on TURN-13H, the early TURN-39 facade, and the early
TURN-38 context compatibility card. The table lists the additional business predecessor.

| Card | Exact implementation write set | Additional predecessor | Mutex / order |
|---|---|---|---|
| TURN-14 | `CR:CloudBagUseIncensePort.java`; Cloud `ReturnItemPrescanService.java`; Cloud `PlayerStateService.java`; new `CT:client/CloudBagLocalServiceClient.java`; one named Bag result DTO | 13H,39B,38A | `PlayerStateService`: 14 before 23 |
| TURN-15 | `CR:CloudUiCleanerPort.java`; new `CT:client/CloudUiCleanerLocalServiceClient.java` | 13H,39B,38A | disjoint from 14/16 |
| TURN-16 | Cloud `DialogService.java`; new `CT:client/CloudGiveItemLocalServiceClient.java` | 13H,39B,38A | `DialogService`: 16 -> 25 -> 26 |
| TURN-17 | new `CT:client/CloudQuestLocalServiceClient.java`; one named Quest result DTO | 13H,39B,38A | disjoint from 14/15/16 |
| TURN-18 | Cloud `ClientIdentityService.java`; new `CT:TurnBindingMetadata.java` | 39B,38A | `ClientIdentityService`: 18 before 23 |
| TURN-19 | `CR:CloudLeftTopStatusPortAssembly.java`; Cloud `LeftTopStatusSwitchService.java` | 39B,38A | disjoint Service lane |
| TURN-20 | Cloud `AutoCombatPanelService.java`; explicitly enumerated panel model/algorithm files | 39B,38A | before 34A |
| TURN-21 | `CR:CloudCommonBoxPortAssembly.java`; Cloud `CommonBoxService.java` | 39B,38A | before 34A/34B |
| TURN-22 | `CR:CloudTeamReturnPortAssembly.java`; Cloud `TeamReturnService.java` | 14 | before 34B/34C |
| TURN-23 | Cloud `PlayerStateService.java`, `ClientIdentityService.java`; `CR:CloudPlayerStateFirstAidPort.java`, `CloudPlayerStateIncenseStatusPort.java` | 14,18 | serial after both 14 and 18 |
| TURN-24 | Cloud `BattleRadarService.java`; explicitly enumerated radar model/algorithm files | 39B,38A | before 28/34A |
| TURN-25 | Cloud `DialogService.java`; `CR:CloudDialogDetectionPort.java`, `CloudDialogPreparedActionValidationPort.java` | 16 | serial after 16 |
| TURN-26 | Cloud `DialogService.java`; `CR:CloudDialogOptionOcrImagePort.java`, `CloudDialogOptionOcrWordsPort.java`, `CloudDialogWhiteStoryTemplatePort.java` | 25 | serial after 25 |
| TURN-27 | Cloud `NavigationService.java`; explicitly enumerated navigation decision/model files | 15,26 | disjoint from 28 |
| TURN-28 | Cloud `NpcClickService.java`; explicitly enumerated NPC decision/model files | 24,26 | disjoint from 27 |
| TURN-29 | the two named TaskTracker Service files and the eight named TaskTracker model files already listed in the plan | 13H,39B,38A | exclusive dual-package card; before 30/31/32 |
| TURN-30 | only Cloud `task/xiuluo/XiuluoTaskV2.java` | 29 | same file: 30 before 37 |
| TURN-31 | only Cloud `task/wubei/WubeiTask.java` | 29 | same file: 31 before 35 |
| TURN-32 | only Cloud `task/wuhuan/FiveRingTaskV2.java` | 29 | same file: 32 before 36 |
| TURN-33 | Cloud `SummonSkillService.java`, `TaskMaintenanceService.java`; `CR:CloudSummonSkillWholePassCapability.java`, `CloudTaskExclusiveInteractionAuthority.java` | 15,26 | `TaskMaintenanceService`: 33 before 34B; defer physical old-authority deletion |
| TURN-35 | only Cloud `task/wubei/WubeiTask.java` plus DTOs named before claim | 31 and all relevant 14..34 cards | same file as 31; parallel with 36/37 |
| TURN-36 | only Cloud `task/wuhuan/FiveRingTaskV2.java` plus DTOs named before claim | 32 and all relevant 14..34 cards | same file as 32; parallel with 35/37 |
| TURN-37 | only Cloud `task/xiuluo/XiuluoTaskV2.java` plus DTOs named before claim | 30 and all relevant 14..34 cards | same file as 30; parallel with 35/36 |

The phrase “explicitly enumerated” is important: a worker brief must list every new model filename before claim. A
folder or “related files” is not an exact write set.

## 5. TURN-34 materialization

### TURN-34A - AutoCombat callers

- Exact write set: only Cloud `com/bot/dhxy/service/AutoCombatService.java`.
- dependsOn: TURN-19,20,21,23,24,33 plus the early facade/context.
- Caller checklist: initialization; both `handleCombatTick` overloads; guard/read-only ticks; dynamic delay;
  expected-exit baseline refresh; deferred recovery.
- No method-level 34A workers may run concurrently. If accounting requires multiple count units, serialize them
  under the same file lane.

### TURN-34B - maintenance callers

- Exact write set: only Cloud `com/bot/dhxy/service/TaskMaintenanceService.java`.
- dependsOn: TURN-21,22,23,25,26,33 plus the early facade/context.
- Caller checklist: `runOpportunisticMaintenance(...)`, task-start/team-window coordination, and Summon cleanup
  calls reachable from it.
- Must start only after TURN-33 releases `TaskMaintenanceService.java`.

### TURN-34C - AutoBattle Task caller

- Exact write set: only Cloud `com/bot/dhxy/task/AutoBattleTask.java`.
- dependsOn: TURN-19,21,22,23,33,34B plus the early facade/context. TURN-20/24 arrive through 34A where used.
- Caller checklist: `execute(context)` startup first aid, maintenance, TeamReturn, LeftTop, and CommonBox path.
- It is file-disjoint from 34A/34B but logically follows 34B.

## 6. TURN-38/39 split required for compilation

### TURN-39A - early create-only facade

- Exact create set:
  - `CT:TurnGameClient.java`
  - `CT:TurnTaskServicePort.java`
  - `CT:TurnTaskServiceExecutionContext.java`
  - `CT:TurnTaskServiceMetadata.java`
- dependsOn: TURN-13H.
- No old `remote/**` modification and no deletion. This card makes the target types exist before context and business
  callers migrate.

### TURN-39B - old-to-new compatibility adapter

- Exact modify set:
  - `CR:CloudGameClient.java`
  - `CR:CloudTaskServicePort.java`
  - `CR:CloudTaskServiceExecutionContext.java`
  - `CR:CloudTaskServiceMetadata.java`
- dependsOn: TURN-39A.
- Preserve compiling compatibility for still-unmigrated callers. Physical deletion belongs to the late Cloud delete
  manifest, not this card.

### TURN-38A - early execution-context compatibility

- Exact write set:
  - `C:com/bot/dhxy/runner/context/TaskExecutionContext.java`
  - `C:com/bot/dhxy/runner/context/TaskExecutionContextHolder.java`
  - `C:com/bot/dhxy/runner/stop/TaskCheckpointDecision.java`
- dependsOn: TURN-39B.
- Keep checkpoint public signatures compatible. If implementation proves that `TaskCheckpoint`, `TaskSleep`,
  `BaseTaskTemplate`, `AutoCombatPanelService`, `ClientIdentityService`, `CloudTaskRunCurrentContextSlot`,
  `CloudTaskRunExecutionGate`, or `RemoteTaskRunCoordinator` must change, do not expand the card informally. Freeze a
  separate compatibility subcard containing every affected file.

### TURN-38B independent state cards

These are candidates for three-line parallel work after their business predecessors and TURN-38A:

| Card | Exact write set | dependsOn | Parallel note |
|---|---|---|---|
| TURN-38B1 | `C:com/bot/dhxy/service/bag/BagWorkflowState.java`, `CloudBagStateOwner.java` | 14,38A | parallel with B2/B3 |
| TURN-38B2 | `C:com/bot/dhxy/service/returnitem/CloudReturnItemPrescanStateOwner.java`, `ReturnItemPrescanWorkflowState.java` | 14,22,38A | parallel with B1/B3; retained-action refs must be gone |
| TURN-38B3 | `C:com/bot/dhxy/task/startup/CloudStartupGateAuthority.java`, `TaskStartupCheckService.java` | 23,38A | direct consumer included |
| TURN-38B4 | `CH:CloudArtifactStore.java`, `ScopedPngArtifactStore.java`, `CloudServiceConfiguration.java` | 13H,17,38A | serial after 13H on configuration file |

### TURN-38C authority-bound remote state classification

The current plan lists five remote state files together, but they are not one mutex-safe implementation unit:

- `CloudGameContextStateOwner` is referenced by the old authority assembly and retained lifecycle adapter.
- `CloudLeftTopStatusSwitchState` is referenced by the old authority assembly and
  `CloudTaskServiceExecutionContext`.
- `CloudPausedReadOnlyObservationContext` is referenced across the old ledger/gate/retained/exclusive graph.
- `CloudPlayerStateStateGovernor` and `CommonBoxStateGovernor` are currently much more isolated.

Before dispatch, classify each file as `KEEP_REWIRE` or `DELETE` in the TURN-44 manifest. For `KEEP_REWIRE`, the card
must include every new-context consumer in its exact write set. For `DELETE`, leave it untouched and delete it with
the old authority SCC. Rewriting an old authority consumer only to delete it in TURN-44 adds risk without creating a
useful compile boundary.

## 7. TURN-40 activation split and unresolved design input

The current TURN-40 write set cannot yet be exact because the wire request has no task type and Cloud has no Task
factory or host creation caller. The parent must first freeze one of these mutually exclusive activation contracts:

1. The first authenticated turn request carries the explicitly selected task code and causes one explicit Cloud Task
   start; or
2. A separate authenticated activation request starts the Task before the long-wait loop.

This report does not select between them. The choice changes protocol files, handler files, and the start/close
boundary, so it cannot be left to an implementation worker.

After that decision, split TURN-40 at least as follows:

- `TURN-40P` activation-contract card: exact two-repository protocol/validator/handler files selected by the parent.
  It must also state how authenticated tenant/user scope and state root are obtained. No owner/session/ledger or
  automatic retry may be introduced.
- `TURN-40A` Cloud construction card: `CloudBrainServer.java`, the named pure Task factory file, and every new
  explicitly-started Task-runner file. If `CloudServiceHost.java` must change beyond TURN-13H, include it here. The
  server must retain and close the exact host/runner it starts.
- `TURN-40B` DHXY lifecycle card: `WindowTaskControlService.java`, `TurnModeGuard.java`, `TurnLoopRegistry.java`,
  `TurnConfiguration.java`, and `application.properties`. This card owns remote start, stop, unregister, and
  unregister-all behavior for the exact window.
- `TURN-40C` user-action card, only if the existing start button/config cannot express remote mode: exact
  `MainWindowController.java` and any one request/model file needed by that control. Do not claim explicit user
  activation while no UI/controller caller reaches `startRemote(...)`.

Safe order is `40P -> (40A || 40B) -> 40C -> dual build -> TURN-41`. `40A` and `40B` can run in parallel because
they are in different repositories. `40C` follows `40B` because it consumes the final control-service API.

## 8. Exact deletion manifest protocol

Create these read-only inventory cards after TURN-41 evidence and before source deletion:

- `TURN-42M` writes only
  `docs/superpowers/plans/reports/2026-07-15-turn-42-dhxy-transport-delete-manifest.md`.
- `TURN-43M` writes only
  `docs/superpowers/plans/reports/2026-07-15-turn-43-dhxy-handler-mechanics-delete-manifest.md`.
- `TURN-44M/45M` writes only
  `docs/superpowers/plans/reports/2026-07-15-turn-44-45-cloud-old-wire-delete-manifest.md`.

These three manifest cards are mutually exclusive by output file and can run as a three-line wave. Each manifest row
must contain:

1. exact repository-relative path, no wildcard or directory shorthand;
2. primary symbols declared by the file;
3. `KEEP`, `REWIRE`, or `DELETE` classification;
4. every remaining production reference as exact path and line;
5. the card that removes or rewires each reference;
6. file byte size and SHA-256 at manifest time;
7. delete prerequisite and intended compile cohort;
8. explicit protection reason for permanent-local Service dependencies and retained NPC reference/shadow code.

A delete worker may delete only `DELETE` rows whose hash still matches and whose listed external references are zero.
If a file changed or a new reference appears, refresh the manifest through the parent; do not guess, glob-delete, or
silently widen the worker write set. `remote/run/**`, `RemoteCommand*`, `RemoteFinal*`, and service folders must all be
expanded file-by-file.

## 9. TURN-42..46 compile-preserving deletion order

### 9.1 DHXY: split TURN-43 around TURN-42

Current source proves that provider-first `TURN-42 -> TURN-43` is unsafe. Use:

1. `TURN-43A` consumer sever/delete:
   - exact rows from TURN-43M for `LeaderPrecheckMechanics`, `BoundLeaderPrecheckCaptureCapability`,
     `LocalRemoteGameCommandHandler`, and any other specialized mechanic that still imports old lifecycle/registry/
     handler types;
   - include retained caller modifications only when the manifest names them;
   - preserve four permanent-local Services and their required models.
2. DHXY compile gate after 43A.
3. `TURN-42A` transport/lifecycle SCC deletion:
   - exact manifest rows for `HttpRemoteCommandTransport`, `RemoteCommandTransport`,
     `RemoteCommandTransportException`, `RemoteCommandPollingLoop`, `HttpRemoteTaskRunApiClient`,
     `RemoteTaskRunLifecycleService`, `RemoteTaskRunRegistry`, `RemoteOperationLedger`, and their old lifecycle/
     registration/receipt providers;
   - all wire DTOs still required by the SCC stay until the same card or TURN-43B.
4. DHXY compile gate after 42A.
5. `TURN-43B` residual DTO/codec/fact/macro deletion:
   - only exact zero-reference DELETE rows remaining in TURN-43M;
   - no folder-level deletion of `service/**`.
6. DHXY compile gate after 43B.

### 9.2 Cloud: TURN-45A before TURN-44A before TURN-45B

Current standalone TURN-44 followed by TURN-45 does **not** provide a compile-safe intermediate. Use:

1. `TURN-45A` old route sever:
   - modify only `C:com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`;
   - delete `CR:RemoteTaskRunRoutes.java`;
   - delete `C:com/yueyunfe/dhxy/cloudbrain/api/RemoteTaskRunEndpoint.java`;
   - preserve `/api/v1/client/turn`, template routes, and the same command-port/host path from TURN-13H/40.
2. Cloud package gate after 45A. The old authority graph may remain unreachable but still compiling.
3. `TURN-44A` authority/facade SCC deletion:
   - delete only exact manifest rows covering `RemoteGameClientPort`, old `CloudGameClient`, old
     `CloudTaskServicePort/ExecutionContext/Metadata`, `CloudTaskRunAuthorityAssembly`,
     `CloudTaskRunRetainedLifecycleActivationAdapter`, `CloudTaskRunCurrentContextSlot`,
     `CloudTaskRunExecutionGate`, `CloudTaskRunExecutionContext`, `CloudTaskRunCommandExecutor`, retained action/
     exclusive/final-consumption authority classes, and every same-SCC consumer;
   - do not split this SCC at a point where one deleted nested type remains referenced by another retained source.
4. Cloud package gate after 44A.
5. `TURN-45B` provider/wire deletion:
   - exact zero-reference rows for `RemoteGameCommandBroker`, `RemoteFinalConsumptionCoordinator`,
     `remote/run/**`, `RemoteCommand*`, `RemoteFinal*`, old task-run action/receipt/error and protocol-only DTOs;
   - keep any business result/model that the manifest classifies as Cloud-owned and still referenced.
6. Cloud package gate after 45B.

If the exact manifest finds a circular source component spanning the proposed 45A/44A boundary, place the entire SCC
in one card and require the package gate after that card. Do not claim a nonexistent compile point inside the SCC.

### 9.3 TURN-46 and TURN-47

- TURN-46 starts only after DHXY 43B and Cloud 45B compile/package gates. It remains one integration owner because it
  writes both POMs, properties, protocol docs, CR271, and dashboard data. Dependency/config removal must be backed by
  exact zero consumers; Jackson/JDK HTTP/image dependencies may still serve the new turn path.
- TURN-47 starts after TURN-46. It is review/report synchronization plus the final dual build, not another business
  implementation card.

## 10. Recommended three-line waves

The table shows a conservative schedule. A wave starts only after all listed predecessors have completed their source
review and the previous same-file owner has released the file.

| Wave | Lane 1 | Lane 2 | Lane 3 |
|---|---|---|---|
| A0 | TURN-13H | idle | idle |
| A1 | TURN-39A | idle | idle |
| A2 | TURN-39B | idle | idle |
| A3 | TURN-38A | idle | idle |
| B1 | TURN-14 | TURN-15 | TURN-16 |
| B2 | TURN-17 | TURN-18 | TURN-19 |
| B3 | TURN-20 | TURN-21 | TURN-24 |
| B4 | TURN-22 | TURN-23 | TURN-25 |
| B5 | TURN-26 | TURN-29 | TURN-38B1 |
| B6 | TURN-27 | TURN-28 | TURN-38B2 |
| B7 | TURN-33 | TURN-38B3 | TURN-38B4 |
| B8 | TURN-34A | TURN-34B | TURN-30 |
| B9 | TURN-34C | TURN-31 | TURN-32 |
| B10 | TURN-35 | TURN-36 | TURN-37 |
| B11 | TURN-38C classification/rewire cards actually selected by manifest | remaining TURN-38C disjoint card | idle |
| A4 | TURN-40P | idle | idle |
| A5 | TURN-40A | TURN-40B | idle |
| A6 | TURN-40C if required | dual source/build review | idle |
| U1 | TURN-41 user gate | no worker | no worker |
| M1 | TURN-42M | TURN-43M | TURN-44M/45M |
| D1 | TURN-43A DHXY consumer sever | TURN-45A Cloud route sever | read-only manifest recheck |
| D2 | TURN-42A DHXY lifecycle SCC | TURN-44A Cloud authority SCC | read-only zero-ref recheck |
| D3 | TURN-43B DHXY residual DTOs | TURN-45B Cloud broker/wire | read-only zero-ref recheck |
| D4 | TURN-46 | idle | idle |
| F1 | TURN-47 | idle | idle |

The business waves provide real three-line implementation parallelism. The deletion waves intentionally expose only
two Java writers because each repository has a dependency-ordered delete SCC. A third Java delete worker would either
share files or remove a provider before its consumers.

## 11. Same-file serialization ledger

This is a planning mutex list, not a runtime ledger:

- `CloudBrainServer.java`: TURN-05 -> TURN-13H -> TURN-40A -> TURN-45A.
- `CloudServiceConfiguration.java`: TURN-13H -> TURN-38B4.
- `PlayerStateService.java`: TURN-14 -> TURN-23.
- `ClientIdentityService.java`: TURN-18 -> TURN-23.
- `DialogService.java`: TURN-16 -> TURN-25 -> TURN-26.
- `TaskMaintenanceService.java`: TURN-33 -> TURN-34B.
- `XiuluoTaskV2.java`: TURN-30 -> TURN-37.
- `WubeiTask.java`: TURN-31 -> TURN-35.
- `FiveRingTaskV2.java`: TURN-32 -> TURN-36.
- old Cloud facade/context files: TURN-39B -> business caller migrations -> TURN-44A.
- `TaskExecutionContext.java`: TURN-38A -> any explicitly named late compatibility card; never concurrent.
- DHXY loop/control files: TURN-12 -> TURN-13 -> TURN-40B -> deletion manifest decision.

## 12. Parent decisions still required before dispatch

1. Materialize TURN-13H in the main plan with its four-file boundary.
2. Choose the TURN-40 task-selection contract and freeze its exact protocol/start/close write set.
3. Decide `KEEP_REWIRE` versus `DELETE` for each authority-bound TURN-38C state class.
4. Freeze named model/DTO filenames for TURN-20/24/27/28/35/36/37 before those cards are claimable.
5. Replace current TURN-42..45 broad delete sets with the manifest-first, compile-preserving sequence above.

This helper report issues no parent decision.
