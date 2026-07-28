# CR271 TURN-38M Authority-State Classification

## 1. Parent Decision

- Status: `PARENT CLASSIFICATION FROZEN / COMPLETE`.
- Evidence cutoff: current Cloud source at `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`.
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- This is a parent plan-contract decision only. It assigns no Worker and changes no Java.
- Decision: all five named old-authority files are `DELETE`; none may be dual-wired into HTTPS turn. The two still-required states receive replacements only at already-authorized turn-native ownership boundaries.

## 2. Frozen Classification

| Old authority file | SHA-256 | Current production references | Classification | Replacement / target card |
|---|---|---|---|---|
| `remote/CloudGameContextStateOwner.java` | `8D5BBEFAC713DA2AD8FFF1C95E4A79701DF184EFFC8EA022FA4228B15E584DBF` | `CloudTaskRunAuthorityAssembly` (10 sites), `CloudTaskRunRetainedLifecycleActivationAdapter` (2 sites); real `callWithState` has no production caller | `DELETE` | Old file stays byte-unchanged through 38C and is deleted with its old SCC in TURN-44A. TURN-40B `CloudTurnTaskRuntime` directly owns one `GameContext.State` per accepted runtime and is the sole outer Task-stack projection consumer. |
| `remote/CloudLeftTopStatusSwitchState.java` | `FC3C859C767300F3899B611A72B08B439D0CADC2D8113B02955E83B321337CFC` | `CloudTaskRunAuthorityAssembly` (6 sites), `CloudTaskServiceExecutionContext` (5 direct type sites plus pending API); all are old graph | `DELETE` | Old file stays byte-unchanged and goes to TURN-44A. TURN-38C implements the existing four context APIs with one private context-local boolean state in turn-native `TaskExecutionContext`. |
| `remote/CloudPausedReadOnlyObservationContext.java` | `BE02F23DB41CEA7F4342FF6B2FFC6757D6FDB16BE8882131F8818F676791CAE3` | `CloudTaskRunExecutionGate` (6), `CloudTaskRunAuthorityAssembly` (2), retained adapter (9), action ledger (7), exclusive authority (3), retained action state (5); all old authority SCC | `DELETE` | No replacement. Pause only blocks Cloud Task progression under the HTTPS-turn contract. Delete in TURN-44A with the final manifest cohort. |
| `remote/CloudPlayerStateStateGovernor.java` | `B5E17B474C11EC6D2FBBD0B01814E78D807CA4E47982A2D51B1597FD1702F713` | zero external production caller | `DELETE` | Active owner remains `PlayerStateService.runtimeStates`; preserve its baseline fields/order. Delete governor plus companion `CloudPlayerStateStateOwner.java` in TURN-44A after final reference rescan. |
| `remote/CommonBoxStateGovernor.java` | `DD4C8CCA5D020CF729820414CEF10B70C6082B9DE66A448C257FCC0FA6B11465` | zero external production caller | `DELETE` | Active owner remains `CommonBoxService.pendingByKey`; preserve `BotProperties` switches and the approved 30-second pending TTL. Delete governor plus companion `CloudCommonBoxProperties.java` in TURN-44A after final reference rescan. |

## 3. Exact Replacement Consumers

### GameContext state, TURN-40B/40C

- Direct state owner/projection consumer: `turn/runtime/CloudTurnTaskRuntime.java` only.
- Lifecycle consumers: `CloudTurnTaskRegistry.java` and `CloudTurnControlPort.java` only through the concrete runtime; neither owns a second state map.
- `CloudTurnRuntimeConfiguration.java` must provide one host-local `GameContext` shared by runtime, Task and Service construction; `CloudServiceHost.java` loads and closes that host graph.
- One accepted runtime creates one state. Pause/resume retains it. Terminal/new accepted start discards the old runtime/state reference and creates a fresh one. No handle, session, revision, ledger, TTL, cleanup retry, nested projection or static/window lookup.
- Tests remain in existing TURN-40B/40C ownership: `CloudTurnTaskRuntimeContractTest`, `CloudTurnTaskFactoryAllowlistTest`, and `CloudTurnActivationContractTest`.

### Left-top pending state, TURN-38C

- Sole direct production consumer/write target: `com/bot/dhxy/runner/context/TaskExecutionContext.java`.
- Indirect read-only business consumer: `LeftTopStatusSwitchService` through the existing four context methods. `AutoBattleTask`, `AutoCombatService`, `TaskMaintenanceService` and all whole Tasks remain read-only.
- State scope is one concrete turn-native context, initially false. Pause/resume keeps the same bit; terminal/exception/stop drops the context reference; a new concrete Task context starts false. No constructor parameter, static map, provider, identity key, history, count, TTL or terminal business action.
- Exact TURN-38C tests: create `runner/context/LeftTopStatusSwitchTurnStateTest.java`; modify `LeftTopStatusTurnContractTest.java` to replace the old reflection fixture; modify `TaskExecutionContextTurnContractTest.java` to replace the obsolete turn-native-unavailable assertions.

## 4. Baseline And Deletion Boundaries

- Left-top truth table remains exact: member startup OPEN marks pending; CLOSED clears; unresolved keeps prior value; safe-window/leader/combat success consumes; known failure does not retry. Safe-window probing is not gated by the bit. `source` remains diagnostic only. The 120-second command timeout is not a state TTL.
- `CloudPausedReadOnlyObservationContext`, dormant Player/CommonBox governors and companions receive no turn replacement.
- Every `DELETE` file remains byte-unchanged until the final TURN-44M45M manifest and TURN-44A whole-cohort delete. TURN-45A must not delete these files.
- Final manifest must rescan source/test references and hashes after TURN-39/40/45A; this classification fixes semantics and ownership, not a stale physical deletion list.
- 无已批准业务差异；按 `696a12b0` 与 HTTPS turn 协议等价迁移。

<!-- TRUE_EOF: TURN-38M PARENT-CLASSIFICATION-FROZEN COMPLETE FIVE-DELETE LEFTTOP-38C-CONTEXT-REPLACEMENT GAMECONTEXT-40B-RUNTIME-OWNER OLD-SCC-44A NO-BUSINESS-DIFFERENCE 2026-07-18T09:05:00-04:00 -->
