# CR271 TURN-34B retained production minimal source-slice helper

- Role: `CR271 Internal helper`，只做 TURN-34B retained production 的 production-repair slice 边界审计。
- 本报告不是 implementation delivery、reviewer 结论、parent 裁决、批准或阻断。
- Snapshot: `2026-07-16T09:36:23.7959315-04:00`。
- Retained `TaskMaintenanceService.java` SHA-256:
  `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC`。
- Current `TaskExecutionContext.java` SHA-256:
  `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003`。
- Sole `TaskMaintenanceTurnContractTest.java` 在 snapshot 时仍不存在。
- 已完整读取用户点名材料，并对照 `docs/业务逻辑.md` 的本地队伍 session、CommonBox、召唤兽三技能及
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线规则。
- 本轮未改 Java、原卡、计划、`ACTIVE_WORK` 或已有文档；未运行 Maven/JUnit/compile/package、runtime、
  application、server、Task、UI、capture、input；未做 Git mutation。

## 1. 最小拆分结论

最多拆成以下两个 production slice。两个 slice 的 production/test 路径均为零重叠，可以各自 claim、交付、
源码审查和测试源码审查；它们不需要靠同一 writer 同时完成。

| Working label | 唯一 production write set | 唯一 test write set | 独立交付内容 |
|---|---|---|---|
| `34BP-FENCE` | Cloud `runner/context/TaskExecutionContext.java` | 既有 Cloud `runner/context/TaskExecutionContextTurnContractTest.java` | 在现有一次 latest metadata read 内闭合 HWND/process/title drift fence |
| `34BP-SCOPE` | Cloud `service/TaskMaintenanceService.java` | 唯一 Cloud `service/TaskMaintenanceTurnContractTest.java` | tuple-safe keys、formal team/local session scoping、A -> B -> A 全状态清理 |

不建议把 fence 放进 `TaskMaintenanceService` 再调用一次
`TurnGameClient.latestWindowMetadata()`。`runOpportunisticMaintenance()` 已先走
`TaskExecutionContext.throwIfStopRequested()`，后者已经读取 latest metadata；Service 再读一次会新增 observation，
并可能把同一 delegate 前的两份 metadata 混在一起。最小边界是在
`TaskExecutionContext.latestExactTurnMetadata()` 的原读取上补齐比较。

## 2. Slice `34BP-FENCE`

### 2.1 Exact production boundary

只修改：

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`

只改 private method：

- `latestExactTurnMetadata()`，当前 `:412-430`。

只读取既有 fields，不新增 field/public API：

- `turnInvocationContext`，当前 `:37`；
- `initialTurnWindowMetadata`，当前 `:38`；
- `turnGameClient`，当前 `:41`。

在现有 `latestWindowMetadata()` 单次读取和现有 device/window 比较之后，按以下 exact 顺序补比较：

1. `initialTurnWindowMetadata.nativeHandle()` 与 latest `nativeHandle()` 使用
   `Objects.equals(...)` 精确相等；
2. `initialTurnWindowMetadata.processId()` 与 latest `processId()` 精确相等；
3. `initialTurnWindowMetadata.windowTitle()` 与 latest `windowTitle()` 使用
   `Objects.equals(...)` 精确相等。

任一不等，包括 latest null/blank component，沿用现有
`TaskCheckpointDecision.turnWindowMismatch()` 构造
`TaskCheckpointTransitionException`。不新增 outcome、reason enum、DTO 或第二 decision file。

明确不改：

- `throwIfStopRequested()`、`checkpointTurnMetadata()`、`isStopRequested()`、`isPauseRequested()`；
- pause `250ms` cadence、STOP/interrupt projection；
- `requireInitialWindowMetadata()`；
- `windowRect`，因为 34B frozen contract 只点名 device/window/HWND/process/title，不能顺手新增 geometry drift
  业务门；
- `TaskMaintenanceService.checkpoint(...)` 与 `runOpportunisticMaintenance(...)` 的 baseline 顺序。

现有调用顺序已经给出 production placement：

- `runOpportunisticMaintenance():584` 的 checkpoint 在 Dialog delegate `:605-607` 前；
- Summon delegate `:758` 前还有原有 checkpoint `:746`；
- 因此本 slice 不需要在 Service 增加 metadata read、helper 或 wrapper。

### 2.2 Slice-local test acceptance

只修改既有 `TaskExecutionContextTurnContractTest.java`，扩充
`turnCheckpointCoversActiveStopPauseAndIdentityFailures()` 或新增一个同层 test method。必须使用真实
`TaskExecutionContext.turnNative(...)` 与 scripted `TurnGameClient`，不使用 source scan/private production
reflection。

Slice-local required cases：

1. exact metadata 通过，`throwIfStopRequested()` 返回 baseline 结果；一次非 pause checkpoint 的
   `metadataReads == 1`、`executeCalls == 0`；
2. missing metadata、device mismatch、window mismatch 保持现有 outcome；
3. native handle、process id、window title 分别漂移时均抛
   `TaskCheckpointTransitionException`，且每例 `metadataReads == 1`、`executeCalls == 0`；
4. 同 device/window/HWND/process/title、只改变 `windowRect` 时不触发新增 fence；
5. exact STOP、pause -> active、pause -> STOP、thread interrupt 的既有断言和 metadata read 次数不变；
6. legacy constructor path 不调用 turn metadata，既有 public surface 不变。

Slice-local command 仍是计划已授权的 Cloud named test 形式：

`mvn -q -Dtest=TaskExecutionContextTurnContractTest test`

本 helper 不运行该命令。TaskMaintenance 的 Dialog/Summon 零 delegate 联调不塞进本 slice 的 test write set，留到
第 5 节 final integration gate，保证本 slice 可以独立交付。

## 3. Slice `34BP-SCOPE`

### 3.1 Exact production boundary

只修改：

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`

19 个 public method 的名称、参数、返回类型和 caller-visible business semantics 全部保持不变。特别不改 TURN-34A
使用的六个 API：

- `isPendingLocalSupportLeaderDetection`；
- `isLocalSupportMemberSession`；
- `isLocalTeamSupportCapabilityOpen`；
- `awaitLocalTeamSupportCapabilityOpen`；
- `isLocalSupportMemberCandidate`；
- `awaitTeamFirstAidMaintenanceWindowOpen`。

### 3.2 Tuple-safe private key model

删除字符串拼接身份路径：

- `DEFAULT_IDENTITY_TOKEN`；
- `scopePrefix(...)`；
- `currentIdentityToken(...)`；
- `identityTail(...)`；
- `nativeHandleOrNull(...)`；
- `nativeProcessIdOrNull(...)`；
- `teamKey + "#" + round`、`startsWith(prefix)` 和 substring/parse pruning；
- `"local-team:" + session + "#" + capability + "#" + epoch`。

在文件底部新增 private nested enum/record，固定为 tuple identity，不使用可碰撞 delimiter：

```java
private enum ScopeKind { EXACT, LEGACY_OR_NULL }
private enum CoordinationKind { LOCAL_SESSION, WINDOW, LEGACY_FALLBACK }
private record ExecutionScopeKey(
        ScopeKind kind, String tenantId, String userId, String deviceId) {}
private record LogicalWindowKey(ExecutionScopeKey scope, String windowId) {}
private sealed interface ContextFingerprint permits NativeFingerprint, LegacyFingerprint, NullFingerprint {}
private record NativeFingerprint(String windowTitle, String nativeHandle, long processId)
        implements ContextFingerprint {}
private record LegacyFingerprint(long playerIdentityEpoch) implements ContextFingerprint {}
private record NullFingerprint() implements ContextFingerprint {}
private record WindowBindingKey(LogicalWindowKey logicalWindow, ContextFingerprint fingerprint) {}
private record LocalSessionScopeKey(ExecutionScopeKey scope, String sessionKey) {}
private record TeamScopeKey(
        ExecutionScopeKey scope,
        CoordinationKind coordinationKind,
        String coordinationId,
        String teamMaintenanceKey) {}
private sealed interface ClaimRoundKey permits TeamRoundKey, LocalCapabilityRoundKey {}
private record TeamRoundKey(TeamScopeKey team, int round) implements ClaimRoundKey {}
private record LocalCapabilityRoundKey(
        LocalSessionScopeKey session,
        TeamSupportCapability capability,
        int epoch) implements ClaimRoundKey {}
```

`ExecutionScopeKey` 的 exact path 取既有 tenant/user/device；`LogicalWindowKey` 再加入 window。显式
`localTeamSessionKey` 是跨窗口共享 capability 的唯一 coordination namespace，符合业务文档“同批启动共享同一个
session”；没有 session 的 exact context 使用 `WINDOW` coordination，不能仅因 task/round 同名而跨窗口。legacy/null
保留独立 `LEGACY_OR_NULL` fallback namespace，不与 turn-native exact tuple 混用。

### 3.3 Exact field changes

以下八个现有 fields 保留业务含义，只替换 key type：

| Field | New key/value shape |
|---|---|
| `lastSummonSkillCleanAtByWindow` | `Map<LogicalWindowKey, Long>` |
| `lastSummonSkillNotDueLogAtByWindow` | `Map<LogicalWindowKey, Long>` |
| `summonSkillUnknownRetryAfterByWindow` | `Map<LogicalWindowKey, Long>` |
| `summonSkillStateByWindow` | `Map<LogicalWindowKey, SummonSkillWindowState>` |
| `activeTeamRoundByKey` | `Map<TeamScopeKey, Integer>` |
| `teamMaintenanceWindowStateByRound` | `Map<TeamRoundKey, TeamMaintenanceWindowState>` |
| `localTeamSessions` | `Map<LocalSessionScopeKey, LocalTeamSessionState>` |
| `summonSkillClaimsByTeamRound` | `Map<ClaimRoundKey, Set<LogicalWindowKey>>` |

只增加两个当前绑定索引，不保留历史：

- `Map<LogicalWindowKey, ContextFingerprint> currentFingerprintByWindow`；
- `Map<TeamScopeKey, WindowBindingKey> formalTeamBindingByKey`。

它们用于原子替换和清理，不是 owner/session/lease/ledger/generation history/TTL/compaction/durable workflow。
`SummonSkillWindowState.identityToken` 删除，由上述 current fingerprint + 同 logical key 清理替代。
`LocalTeamSessionState` 只增加
`Map<LogicalWindowKey, ContextFingerprint> participantFingerprints`，现有 capability/candidate/role/completed/
leader fields 的业务意义不变。

### 3.4 Exact method changes

Key resolver/cleanup methods：

- `effectiveContext(...)` 保留 supplied-context-first；
- `currentWindowKey(...)` 改为一次解析 `WindowBindingKey`/`LogicalWindowKey`，不再返回 `String`；
- `summonSkillState(...)` 改用 `LogicalWindowKey`，不自行再造 identity string；
- 新增一个真正边界方法 `resolveCurrentBinding(...)`，负责 exact/legacy/null tuple 与 fingerprint；
- 新增一个真正边界方法 `invalidateBindingScopedState(...)`，只在同一 `LogicalWindowKey` 的 fingerprint 改变时
  清理旧状态；不得叠加 prepare/resolve/handle wrapper 链；
- `resolveTeamRoundKey(...)` 返回 `ClaimRoundKey`；
- `resolveLocalSupportCapabilityRoundKey(...)` 返回 `LocalCapabilityRoundKey`；
- `teamRoundKey(...)` 返回 `TeamRoundKey`；
- `pruneOlderTeamRoundClaims(...)` 按 record equality + numeric round 比较，不做 prefix parsing；
- `releaseSummonSkillRoundClaimIfOwned(...)` 改用 `ClaimRoundKey` + `LogicalWindowKey`。

Formal-team state methods全部切到 `TeamScopeKey/TeamRoundKey`：

- `beginTeamMaintenanceRound`；
- `openTeamPathingMaintenanceWindow`；
- `openTeamFirstAidMaintenanceWindow`；
- `closeTeamMaintenanceWindow`；
- `isTeamPathingMaintenanceWindowOpen`；
- `awaitTeamFirstAidMaintenanceWindowOpen`；
- private `isTeamFirstAidWindowOpen`。

Local-session state methods全部切到 `LocalSessionScopeKey`，并在 context-bearing access 上登记/核对当前 window
fingerprint：

- `registerLocalTeamSessionCandidate`；
- `markLocalTeamWindowRoleDetected`；
- `markLocalTeamLeaderDetected`；
- `completeLocalTeamSessionWindow`；
- `isLocalSupportMemberSession`；
- `isPendingLocalSupportLeaderDetection`；
- `isLocalTeamSupportCapabilityOpen`；
- `awaitLocalTeamSupportCapabilityOpen`；
- private `hasDetectedLocalLeader`；
- private `openLocalTeamSupportCapability`；
- private `closeLocalTeamSupportCapabilities`；
- `openLocalTeamReturnSupportWindow` / `closeLocalTeamReturnSupportWindow` 通过上述 private path 自动使用 typed
  session key。

`registerLocalTeamSessionCandidate(...)` 与 `completeLocalTeamSessionWindow(...)` 没有 context 参数：它们只使用
holder 中已有 context；holder 为空时进入独立 legacy/null fallback namespace。保持四个零 production caller lifecycle
API 不可达，不新增 host/factory/runtime caller，也不改 public signature。

`initializeForTaskStart`、`maybeCleanSummonSkill`、`logSummonSkillNotDue`、
`updateSummonSkillWindowState`、`releaseSummonSkillRoundClaimIfOwned` 只接受 typed window/round key；
`runOpportunisticMaintenance` 的 checkpoint、broadcast、Summon gate/delegate/result 顺序不改。

### 3.5 A -> B -> A cleanup contract

同一 `LogicalWindowKey` 首次看到 fingerprint A 后，再看到 B 时，必须在一个
`currentFingerprintByWindow.compute(...)` transition 内完成以下清理，然后只保留 B；B -> A 同样先清 B，不能恢复
第一份 A：

1. 删除该 logical window 的 cooldown、not-due log、unknown-failure interval、Summon layout/cache state；
2. 从全部 formal/local `summonSkillClaimsByTeamRound` claim set 删除该 logical window，空 set 删除；
3. 对 `formalTeamBindingByKey` 中由该 binding 打开的 team scope，清 active round、该 team 的 round-window state 和
   round claims；
4. 对同 execution scope 的 local session，移除该 window 的旧 role/completed/fingerprint；若它是 leader，清
   leader evidence、open capabilities 和 capability epochs；candidate window id 可保留，因为它仍是同批选中窗口；
5. cleanup 后通知既有 `teamMaintenanceWindowMonitor` waiters；不启动 timer、queue、retry 或 background cleanup。

这套清理只处理 identity ownership plumbing，不改五/一/五/二 capability 集、round 数、max-claim 规则、Summon
gate、2h cache TTL 或 failure projection。

### 3.6 Slice-local test acceptance

只创建/修改 sole `TaskMaintenanceTurnContractTest.java`，直接 new 真实 public
`TaskMaintenanceService`，使用 test-private scripted collaborators。无 Spring/HTTP/runtime/Task/UI/input/capture，
无 Mockito、source scan、private production reflection、wall-clock sleep。

Slice-local required cases：

1. tenant、user、device、window 四个 tuple component 分别变化时，相同 window/task/round/session text 不共享
   cooldown、team window、claim、leader/capability state；
2. delimiter collision 负例：tenant/user/device/window/task/session 中含 `|` 或 `#` 的两个不同 tuple 不相等；
3. 同 tenant/user/device、同 explicit local session 的 leader/member 跨不同 window 仍共享 capability；没有 shared
   session 的同 task/round 不跨 window；
4. A 建立 cooldown/tail cache/unknown interval/round claim/formal team window/local leader+capability 后，B 同 logical
   tuple 不继承；B 建立同类状态后回 A，第一份 A 也不复活；
5. supplied context 优先于错误 holder context；turn-native 正常路径成功即证明没有调用 legacy-only
   `getPlayerIdentityEpoch()`；
6. legacy context 与 null/holder-empty fallback 保持原可调用结果，但不与 exact tuple alias；
7. pathing open、weak first-aid open、team close、return-support open/close 仍为 exact 五/一/五/二 capability set；
8. formal round duplicate/max claim、failed-no-state-change release、state-change claim retention保持 baseline；
9. 全部 19 public signatures 和 TURN-34A 六 API 仍可编译调用；四个零 caller lifecycle API 不新增 production
   activation；
10. scoping/rebind 本身 Dialog/Summon/action/UUID count 为零，`runOpportunisticMaintenance` 仍最多一次 TURN-33
    delegate，零自动 retry。

Slice-local command：

`mvn -q -Dtest=TaskMaintenanceTurnContractTest test`

本 helper 不运行该命令。

## 4. 两片独立交付与顺序

- 两片 production/test write set 为 `0` path overlap；不能把 `TaskExecutionContext` 顺手塞回原 34B one-file
  write set，必须由 parent 另冻 `34BP-FENCE` 边界。
- `34BP-FENCE` 不依赖 `34BP-SCOPE`：其 test 只验证 context checkpoint 的单读 exact fence。
- `34BP-SCOPE` 不依赖 `34BP-FENCE` 才能完成 tuple/scoping/A -> B -> A：其 fixture 可为 A/B/A 各提供 exact
  initial/latest metadata，slice-local assertions只验状态所有权。
- 两片可按任一顺序交付；最终整合前必须都基于上述 snapshot 后的最新文件 SHA 重新核对。若任一路径已有新 owner，
  先按原卡 owner 规则完成释放/接续，不能双写。
- 不把 shared cross-slice assertion 放进任一片的独立通过条件，从而避免一片因另一片未落盘而不能交付。

## 5. Cross-slice final integration gate

两片都交付后，仍在 sole `TaskMaintenanceTurnContractTest` 的后续 test-only tranche 增加 production-path 联调；这不是
第三个 production slice：

1. broadcast-only request 对 missing/device/window/HWND/process/title 每种 drift 都在第一次 checkpoint 停止，
   `DialogService`、`SummonSkillService`、action/UUID 均为零；
2. Summon-due request 第一份 metadata exact、第二个既有 pre-action checkpoint 才漂移时，Summon delegate 为零，
   metadata read 恰为两个既有 checkpoint，不新增第三读；
3. supplied context 与错误 holder 并存时，fence、typed key 和 cleanup 全部使用 supplied context；
4. exact path 执行原 broadcast -> short-circuit -> optional one Summon 顺序，证明 fence/scoping 未改变
   `696a12b0` business order；
5. A -> B -> A 后再跑 exact delegate，旧 cooldown/claim/capability 不复活，且没有自动 retry/second command。

## 6. 只属于 final gate 的依赖

以下项目不需要阻止这两个互斥 source slice 的 claim/source delivery，但 TURN-34B 整体进入最终结论前仍需满足：

| Dependency/gate | 本次 slice 边界 |
|---|---|
| TURN-22 Repair #3 / 28P integration | 34B production 不调用 TeamReturn mechanics，也不消费其 queue；只作为最终 source/integration/build gate |
| TURN-34A | `AutoCombatService` 及其 test 只读；六 API compatibility、其适用 test/compile 只作为最终 gate |
| TURN-33 | `cleanSummonSkillsOnce(SummonSkillCleanupRequest)` source contract 已可消费；TURN-33 named test/build 证据只进入最终 applicable gate，不复制其 mechanics |
| TURN-34BT1 与 sole named test | test-only tranche 不能把 retained WIP 变成 source acceptance；真实 assertions保留到 scope slice及 cross-slice final tranche，不得弱化 |
| 两名独立 reviewer | 两个 source slice 交付后分别审查，再做整合审查；不是 source-start dependency |
| Maven/JUnit/Cloud compile/build | Java writers 稳定后才运行；是 test/build final gate，本 helper 不运行 |
| 19 public API + TURN-34A 六 API full compatibility | `34BP-SCOPE` 做 source/test-local锁定，整卡仍在最终 compile/integration复核 |

以下不是本两片的前置或 final 依赖，不应借此扩大写集：TURN-40B/C/D runtime activation、TURN-41 real Win32
runtime、TURN-35/36/37 whole Tasks、DHXY input/capture/runtime。它们只能在 34B final public contract 后作为下游
consumer 继续。

## 7. 明确排除

- 不改 `TaskCheckpointDecision`/outcome、turn protocol/client/result、maintenance model、POM/config/resources；
- 不改 `AutoCombatService`、`AutoBattleTask`、Wubei/Xiuluo、SummonSkill、Dialog、CommonBox、TeamReturn 或 DHXY；
- 不加 metadata 第二读、windowRect fence、TTL、retry、cleanup timer、owner/session/lease/ledger、background queue；
- 不改变 checkpoint/pause/STOP、broadcast/Summon priority、五/一/五/二 capability、claim count、2h cache 或
  `696a12b0` 结果投影；
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

TRUE_EOF PRECHECK_COMPLETE
