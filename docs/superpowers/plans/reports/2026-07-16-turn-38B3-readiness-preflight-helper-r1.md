# CR271 TURN-38B3 startup gate / direct startup caller exact-boundary PRECHECK R1

## 0. 角色、范围与快照

- 角色：CR271 非绑定 readiness/preflight helper；不是 implementation Worker、reviewer 或父级。
- 审计快照：`2026-07-16T07:22:19-04:00`；并发写入持续存在，所有 SHA/size/ref 只对本快照成立。
- 本轮唯一写入：
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-38B3-readiness-preflight-helper-r1.md`。
- 本报告只记录 PRECHECK、依赖、真实 caller、精确写集、UNKNOWN/stop/pause、metadata/context authority、
  写集冲突、未来 named-test 与 stop-work 证据；不改变卡片状态，不产生 owner/claim，不替父级选择 DAG、兼容层、
  metadata 来源、构造入口或测试归属。
- 未修改 Java/test/计划/卡片/`ACTIVE_WORK`/CR271/矩阵/dashboard；未运行 Maven、JUnit、compile/package、
  runtime/application/server/Task/UI/capture/input；未执行 Git mutation。
- 两仓既有 dirty/untracked 全部保护；任何 `??` 只表示当前物理状态，不表示本 helper 或任何人的 ownership。

## 1. 已读取的权威输入

| 输入 | 本轮快照证据 |
|---|---|
| `AGENTS.md` | 392 行；SHA-256 `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md` | 1349 行；SHA-256 `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `docs/ACTIVE_WORK.md` 顶部 CR271 | 落笔前 07:21 再读；SHA-256 `17E8CE72098E1998FFB2C68BBCA4E8F3E1AA1AF0AB20375F68D0430F6A0C84D3` |
| 权威计划第 14-19 节 | 落笔前 07:21 再读；SHA-256 `BF9FC9D0D528C1F1769990A89CB8653D53A52098EC2680E8692243C14800DC13` |
| HTTPS turn 协议 | 383 行；SHA-256 `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| `docs/业务逻辑.md` | 1426 行；SHA-256 `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` |
| TURN-23 固定报告 | 345 行；SHA-256 `113B880E061A9D3F14FBE4DD5F483000FD9436FF2FB7C2F12B3DA50D82C94B17` |
| TURN-34C 最新 PRECHECK | 245 行；SHA-256 `C496960655ACC7D2BA871B7F9270AAA89B354B372A76CD69AE118B976B9A7882` |
| TURN-35 latest dependency PRECHECK | 278 行；SHA-256 `3D8DA8CF611EAB7045E334A7B96E52BFC2A5A25EB427D14D9F10D7470A5E2500` |
| TURN-36 PRECHECK | 352 行；SHA-256 `F27B886E5FF026E9E24B0939BE5665D70BDD1BB54E252A603DF827BD415BF39C` |
| TURN-37 PRECHECK | 322 行；SHA-256 `4E86925D338F570FB06C230F920477A68E00B55B4D357D205BFF53329D8C3ABD` |
| TURN-38A 最新 PRECHECK | 362 行；SHA-256 `04C8C2722A3D6E2C62ED876DB0CB6073DC309595F35A96A76EC63D692CFE456F` |
| TURN-38B2 最新 PRECHECK | 353 行；SHA-256 `E3080A47F8097A6B6E86F0B2C8D9EAF0758051E3D3981842EAE0B4794BD5BAEE` |

时间关系：TURN-38A PRECHECK 的审计时点是 06:55:24，报告 mtime 是 07:00:16；权威计划又在 07:21 漂移。
本报告已按 07:21 当前计划复核 38A/B1/B2/B3/B4 注册表、17.3 写集、R5 顺序及 19.4 named-test 行；
38A 报告中的行号或 SHA 不能替代未来领取前重算。

## 2. 双仓只读状态

| Repo | Branch / HEAD | 展开全部 untracked 的 pre-write status |
|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 650 项：`M=43`、`D=1`、`??=606`；status SHA-256 `1667C72C26CA8BA4698C036C481F7DB1C5BB1F87CD2D315671CFE817421470A6` |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 项：`M=9`、`??=541`；status SHA-256 `3AD9F64BDF868DDA5DF09DE95DBB6C1F4C7E56CA973A9A80CDB0611C2DFC55C0` |

- 本报告创建前目标文件不存在；上述 DHXY 650 项不含本报告。
- Cloud 的 B3 两个实际生产文件、两个真实 Task caller、38A context 文件、B1/B2/B4 文件和协议/metadata 文件均在
  大型 untracked 工作树中；不得从 HEAD、旧报告或 baseline 整文件覆盖。
- DHXY `WindowTaskRunner.java` 为 `M`。本轮 `git diff` 显示其 68+/26- 变化集中在 paused read-only observer/pathing，
  当前 role/session/startup context 代码同时存在于 HEAD；本 helper没有修改该 dirty 文件。
- 顶部 CR271 明确 External B/C 仍是活动 Java/test writer；本轮没有并行运行任何构建门。

## 3. 依赖链与当前可观察事实

权威计划当前固定：

```text
startDependsOn(TURN-38B3) = TURN-23 + TURN-38A
startDependsOn(TURN-38A)  = TURN-13C + TURN-34C + TURN-35 + TURN-36 + TURN-37
Wave R5                 = TURN-35/36/37 -> TURN-38A -> TURN-38B1/B2/B3/B4
```

| 依赖 | 当前证据 | 对 38B3 的精确影响 |
|---|---|---|
| TURN-23 | 当前计划记录 production/test-source 复审证据已形成；named test/build 仍待 stable-writer cohort | `PlayerStateService` startup first-aid/incense source 可只读消费；不能把未运行门写成通过事实 |
| TURN-34C | 仍等待 22、34A、34B；`AutoBattleTask` 是它的唯一 production 文件 | B3 的 AutoBattle caller 在 34C 最终交付前不是稳定 SHA/调用序列 |
| TURN-35 | 最新报告列出 22/27/28/34A/34B 等未满足 source 项 | 38A 的直接前置仍未形成最终 source surface |
| TURN-36 | 仍等待 27/28/34A 与 open-main-bag local boundary；`FiveRingTaskV2` 是其唯一 production 文件 | B3 的 FiveRing caller 在 36 最终交付前不是稳定 SHA/调用序列 |
| TURN-37 | 仍等待 22/27/28/34A/34B 等最终接口 | 38A 的直接前置仍未形成最终 source surface |
| TURN-38A | 最新 PRECHECK 记录后继 caller 顺序倒置、old API caller 未清零、production turn-native factory/metadata constructor 为零 | B3 依赖的 post-38A context API 尚无父级冻结的可编译形状 |

38A 与 B3 当前存在精确的编译顺序问题：38A 写集要求只删除已经零 caller 的 old retained-authority 依赖，
但 `CloudStartupGateAuthority` 仍调用 `getScope/getPlayerIdentityEpoch/getStopEpoch/getRunRevision`，而 B3 又依赖 38A。
若 38A 字面删除这些 API，B3 尚未实施时当前 main source 会先失去编译面；若 38A 保留兼容 shell，shell 的 exact
allowlist 和 B3 可消费 API 又必须先由父级冻结。本 helper不选择这两条路径。

## 4. TURN-38B3 精确写集核验

### 4.1 计划路径与真实路径不一致

权威计划 17.3 当前写为：

```text
C:com/bot/dhxy/task/startup/CloudStartupGateAuthority.java
C:com/bot/dhxy/service/TaskStartupCheckService.java
```

物理源码事实：

```text
存在: src/main/java/com/bot/dhxy/task/startup/CloudStartupGateAuthority.java
存在: src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java
缺失: src/main/java/com/bot/dhxy/service/TaskStartupCheckService.java
```

两个真实 caller 都 import `com.bot.dhxy.task.startup.TaskStartupCheckService`。因此当前计划第二条不是可编辑实体，
也不能由 Worker 临场搬包、复制第二个 Service 或猜测计划意图。父级先纠正计划路径后，物理候选写集才能唯一化为：

1. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/startup/CloudStartupGateAuthority.java`。
2. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java`。
3. Create 唯一 named test：按 `C_TEST` alias 与 19.4 行解析为
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/startup/StartupGateTurnStateTest.java`。

上述第 1-3 项是源码实体与计划 shorthand 的精确映射证据，不是本 helper 对计划的修改或开工授权。

### 4.2 当前实体 SHA/size

| 文件 | Status | Bytes | SHA-256 |
|---|---:|---:|---|
| `task/startup/CloudStartupGateAuthority.java` | `??` | 11701 | `5648EEA3F47665ABF8DD0ED680DC57A698F75D00DC5D33BD49D95FD80B5397ED` |
| `task/startup/TaskStartupCheckService.java` | `??` | 4672 | `289E3930E6CF3A935443A41CAEA02A70377AF9ECF10B521093AF56A0856638B1` |
| `task/AutoBattleTask.java` | `??` | 14159 | `E13BFFF740570B9C7B833F7EDCE336BFFE39FB89E410B630FF2156B69410264A` |
| `task/wuhuan/FiveRingTaskV2.java` | `??` | 162296 | `287FF0EBE4F3CECF9820A10D2FFCBF0F7AED2A26BEB7A5F510D92F540E8A4BDB` |
| `runner/context/TaskExecutionContext.java` | `??` | 19979 | `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` |
| `remote/CloudTaskServiceMetadata.java` | `??` | 2747 | `A3FE6615BD0D4F571C3618EE45C679B6E28CC08891FA560B7907EDE357C91C93` |
| `protocol/TurnWindowMetadata.java` | `??` | 681 | `E1430169AAE3E35AC9F6295E41EA401E66EFF910E0B4B4DFF954E72C9416AF1B` |
| `protocol/TurnTaskStartRequest.java` | `??` | 338 | `D4AF7B55DD1B4A6B01DF5EED4E9F2468B745A31241314762242C340D0FF03117` |
| `host/CloudServiceConfiguration.java` | `??` | 4620 | `B047D9F910C724083B9594D431ED31DB1601BEFCC18F179ABF263D4D23A8199D` |
| `runner/context/TaskExecutionContextTurnContractTest.java` | tracked/clean | 37301 | `D667D6958DBC38A6FCCF2BA5E562CECD4EF60629DF7A4CD55E347C9DBD9ED945` |
| future `StartupGateTurnStateTest.java` | absent | - | - |

### 4.3 全部只读边界

38B3 不得修改 `TaskStartupCheckResult`、`TaskTeamAssignmentPolicy`、两个 Task caller、38A 七个文件、
`CloudTaskServiceMetadata`、turn protocol/client/exchange、B1/B2/B4 文件、host/configuration、POM/resource 或其它 test。
如果实现需要改 `AutoBattleTask`、`FiveRingTaskV2`、context、protocol、`CloudServiceConfiguration` 或增加第二个 test，
必须停止并由父级先纠正 write set/依赖；不能把“direct caller”测试语义误写成 direct caller production ownership。

## 5. 当前 source graph 与真实 caller

| 节点 | Inbound production refs | Outbound authority/行为 |
|---|---|---|
| `CloudStartupGateAuthority` | 除同包 `TaskStartupCheckService` 类型依赖外，无生产装配 caller | seed policy；bind evaluation；当前读取 old `RemoteTaskRunScope` 和 epoch/revision |
| `TaskStartupCheckService` | `AutoBattleTask` 与 `FiveRingTaskV2` 各持有一个字段并各调用一次 | 先 `context.throwIfStopRequested()`，再校验 bound evaluation，再作 role gate |
| `TaskStartupCheckResult` | startup service 产生；两个 Task 消费 | ALLOW 或 `SKIPPED/FAILED/STOPPED` result 容器；B3 当前只产生 ALLOW/SKIPPED |
| `TaskTeamAssignmentPolicy` | 当前 Cloud production 无调用；虽有 `@Component`，现有 host scan 不包含 `com.bot.dhxy.task` | requested task + role 到 effective task；不是 B3 写集 |
| `AutoBattleTask` | 当前 Cloud 无 factory/registry/runtime/host 生产 caller | `:122 checkAutoBattle(context)`；属于 TURN-34C 的 caller 文件 |
| `FiveRingTaskV2` | 当前 Cloud 无 factory/registry/runtime/host 生产 caller | `:255 checkFiveRing(context)`；属于 TURN-36 的 caller 文件 |

全量只读扫描的零调用证据：

- `new TaskStartupCheckService(...)` production 调用数 `0`。
- `CloudStartupGateAuthority.bind(...)` production 调用数 `0`。
- `seedBaselineNoOverride/seedControlPlanePolicy` 除声明/类内构造外，production 调用数 `0`。
- `new CloudTaskServiceMetadata(...)` production 调用数 `0`。
- `TaskExecutionContext.turnNative(...)` production 调用数 `0`。
- `CloudServiceHost.create(...)` production 调用数 `0`；仅现有 host contract test 调用。
- TURN-40B 的 `CloudTurnTaskFactory/Runtime/Registry/StartResult/ControlPort` 当前全部不存在。
- Wubei/Xiuluo 没有 `TaskStartupCheckService` direct call；B3 direct caller 只有 AutoBattle 与 FiveRing 两条。

所以当前 B3 是 dormant source graph：Task 文件已经引用 service，但 authority/evaluation/service 没有生产构造链，
Task 本身也没有 Cloud runtime 激活 caller。source existence、Spring annotation 或 named test 不能冒充 runtime reachability。

## 6. 两条 direct startup caller 的精确边界

### 6.1 AutoBattle

`AutoBattleTask:111-137` 当前顺序：

```text
resolve exact context
-> TaskExecutionContextHolder.callWith(exact context)
-> checkAutoBattle(context)
-> gate 拒绝时原样返回拒绝结果
-> RUNNING
-> startup first aid
-> maintenance initialize
-> AutoCombat initialize
-> patrol
```

- Gate 位于所有 startup mutation/action 之前；gate 拒绝分支对 RUNNING、first-aid、maintenance、AutoCombat、tick
  的调用数必须为零。
- `TaskStopRequestedException` 和 `TaskCheckpointTransitionException` 当前都没有在 AutoBattleTask 内捕获，
  会穿过 `callWith` 向外传播；Holder 必须恢复先前 context。
- B3 只保持 `checkAutoBattle(TaskExecutionContext)` public call surface 与返回语义，不修改该 caller；34C 最终 SHA、
  startup 顺序和它自己的 named test 先稳定后，B3 才可重算 direct-caller 断言。

### 6.2 FiveRing

`FiveRingTaskV2:243-318` 当前顺序：

```text
resolve exact context
-> try
   -> checkFiveRing(context)
   -> gate 拒绝时原样返回拒绝结果
   -> RUNNING / progress / round loop
-> catch TaskStopRequestedException: mark idle + return STOPPED
-> catch Exception: mark failed + return FAILED
-> finally forceReleaseTurn
```

- Gate 同样在 RUNNING、progress、round/phase/action 之前。
- 即使 gate 返回 SKIPPED，当前 `finally` 仍执行一次 `forceReleaseTurn`；未来测试不能把既有 cleanup 次数误写成零，
  也不能把它扩展为业务 action。
- confirmed stop 当前映射 `STOPPED`；missing/mismatched context 的 `TaskCheckpointTransitionException` 继承
  `RuntimeException`，会落入 generic `Exception` 分支并映射 `FAILED`。
- 这与 AutoBattle 的“异常向外传播”形成真实 terminal asymmetry。B3 不拥有 caller 文件，不能自行统一；
  TURN-34C/TURN-36 与父级必须先冻结各自 direct-caller named-test 的预期，B3 test 只能按最终 caller 合同验收。

### 6.3 Holder 边界

- `TurnGameClient.latestWindowMetadata()` 先要求当前线程 Holder 的 device/window 等于 bound view；未绑定或嵌套错 context
  会在 metadata port 前失败。
- AutoBattle 自己用 `holder.callWith` 包住 lifecycle；FiveRing 不自行绑定，必须由未来 Cloud runtime 或 named-test harness
  在 public `execute(context)` 外绑定同一 exact Holder context。
- B3 不得通过绕过 Holder、换 unbound client、反射调用 private gate helper 或构造 fallback context 来制造正向用例。

## 7. UNKNOWN role 的 exact 矩阵

当前 Cloud policy defaults 与 DHXY `TeamTaskProperties` 完全一致：

```text
fiveRingRequiresLeader=false
autoBattleRequiresMember=false
allowFiveRingWhenRoleUnknown=true
allowAutoBattleWhenRoleUnknown=true
```

| Gate | Policy | Role | Exact result |
|---|---|---|---|
| FiveRing | leader gate disabled | 任意 | ALLOW；reason 中 role 固定投影为 UNKNOWN；不做 live role read |
| FiveRing | leader gate enabled | LEADER | ALLOW |
| FiveRing | leader gate enabled | MEMBER 或 SOLO | SKIPPED |
| FiveRing | leader gate enabled | UNKNOWN/null/非法文本 | 仅由 `allowFiveRingWhenRoleUnknown` 决定 ALLOW/SKIPPED |
| AutoBattle | member gate disabled | 任意 | ALLOW；reason 中 role 固定投影为 UNKNOWN；不做 live role read |
| AutoBattle | member gate enabled | MEMBER | ALLOW |
| AutoBattle | member gate enabled | LEADER | SKIPPED |
| AutoBattle | member gate enabled | SOLO/UNKNOWN/null/非法文本 | 一律投影 UNKNOWN，仅由 `allowAutoBattleWhenRoleUnknown` 决定 ALLOW/SKIPPED |

补充边界：turn-native `CloudTaskServiceMetadata` 构造器拒绝 blank/null `windowRole`，所以 null 主要是 legacy/防御性
解析合同；非法非空文本仍应 fail into UNKNOWN，不能升级为 LEADER/MEMBER，也不能触发 live detection。

DHXY baseline 的差异点必须显式理解：本地 FiveRing gate 在配置启用时调用 live `TeamRoleDetectionService`；
Cloud 当前注释则声明 role collection 留在 thin client，并只消费冻结 role fact。等价迁移的前提不是 Cloud 再做一次
live probe，而是 role fact 有真实、非推断的 authority source；该 source 当前尚未贯通 wire/runtime，见第 9 节。

## 8. STOP / PAUSE / exact-context 语义

`TaskStartupCheckService.requireCurrentContext` 当前固定顺序：

```text
nonnull context
-> context.throwIfStopRequested()
-> evaluation.requireExactContext(context)
-> policy/UNKNOWN decision
```

turn-native `TaskExecutionContext.throwIfStopRequested()` 当前合同：

1. 通过 bound `TurnGameClient` 读取一次 latest metadata；active fast path metadata read=`1`、action/UUID=`0`。
2. latest metadata 缺失 -> `TaskCheckpointTransitionException(MISSING_BINDING)`。
3. device 不同 -> `IDENTITY_OR_SESSION_MISMATCH`；window 不同 -> `WINDOW_MISMATCH`。
4. 同一 metadata 同时 pause+stop 时 STOP 优先，直接抛 `TaskStopRequestedException`；不得先进入 pause。
5. PAUSE 以现有 `250ms` cooperative cadence 在同一 context 内等待；每轮先 sleep、再读取同一 exact window metadata，
   STOP 仍优先；resume 返回真实暂停毫秒数。
6. pause wait 被 interrupt -> 保留 interrupt flag并抛 stop exception；不产生 gate result、业务失败、retry 或 action。
7. gate policy disabled 也不能绕过 checkpoint/exact-context；stop/pause/mismatch 必须先于 ALLOW。

B3 rewire 必须移除当前 evaluation 对 old `RemoteTaskRunScope/playerIdentityEpoch/stopEpoch/runRevision` 的依赖，
但不能在 startup gate 内另建 lifecycle owner、stop epoch、session、revision、TTL、重试、park/yield 或第二 metadata cache。
动态 stop/pause 的唯一 authority 是 `TaskExecutionContext` 通过 latest `TurnWindowMetadata` 的 checkpoint。

当前 checkpoint 只对 latest `deviceId/windowId` 做动态一致性校验；HWND/process/title 来自 context 创建时的 immutable
initial metadata。B3 不得暗中增加第二次 latest read 或新的 HWND/process drift 业务终态。若父级要求更强的 current-HWND
fence，应先在 38A/TurnGameClient context 合同中统一冻结，而不是只在 startup gate 私建一套规则。

## 9. Metadata / context authority 断点

### 9.1 目标 authority 表

| 字段/事实 | 允许的权威来源 | B3 可消费 API |
|---|---|---|
| tenant/user | 单进程 fixed `CloudServiceScope`；不得由 request body 选择 | `context.getTurnServiceScope()` |
| device/window | 当前 turn invocation identity | `context.getTurnInvocationContext()` |
| initial title/HWND/process/rect | 首个已验证、与 invocation exact 的 `TurnWindowMetadata` | context immutable getters |
| current pause/stop | 每次请求携带的 latest exact `TurnWindowMetadata` | `context.throwIfStopRequested()` |
| effective/requested task、role、local team、retry、startup mode、startedAt | immutable `CloudTaskServiceMetadata`，必须由既有业务 authority 显式构造 | context business getters |
| in-memory taskRunId | 未来 TURN-40B runtime 的一次 Task execution diagnostic identity | `context.getTaskRunId()` |
| startup policy | authenticated complete policy snapshot，或显式 `NO_OVERRIDE` 后已确认基线 | 当前 authority seed API；尚无生产 caller |

### 9.2 当前 wire 缺失 role/team 事实

当前双仓 byte-identical protocol 明确只有：

```text
TurnTaskStartRequest = startRequestId + ordered taskCodes + failurePolicy
TurnWindowMetadata   = deviceId + windowId + title + HWND + processId + rect + pauseRequested + stopRequested
```

两者都没有 `windowRole/localTeamSessionKey/localLeaderWindowId/localLeaderPresent/localSupportMember`，也没有 retry policy、
startup mode 或 startedAt。Cloud production 又没有 `new CloudTaskServiceMetadata(...)` 或 `turnNative(...)` caller。

DHXY 当前真实本地 authority 在另一条链上：

- `WindowTaskControlService:132-187` 为同批队伍候选建立 local-team session 并把 session/leader metadata 交给本地 runner。
- `WindowTaskRunner:3802-3892` 在 startup preflight 做 live role detection/assignment并同步 `WindowRole`。
- `WindowTaskRunner:3957-3982` 把 role/session/leader/support/startup/startedAt 写入本地 `TaskExecutionContext`。

现有 HTTPS turn request 没有把这份冻结 snapshot 送到 Cloud；TURN-40B runtime 也尚不存在。因此当前源码无法证明
`CloudTaskServiceMetadata.windowRole` 来自何处。`docs/业务逻辑.md:5-23` 又规定 local team session 来自同批启动和 live
role preflight，禁止把后加窗口、task code、window title 或 request text 自动猜成团队关系。

38B3 不得采取以下临时填充：固定写 LEADER/MEMBER、从 effective task code 反推 role、从窗口标题推 team、把 UNKNOWN
当 member/leader、复活 `RemoteTaskRunScope`、或在 Cloud 另建 durable session/owner/ledger。父级需先冻结
`CloudTaskServiceMetadata` 每字段生产来源和 40B/40D 的构造交接点；本 helper不扩协议或替其选择方案。

### 9.3 Policy/service 构造断点

- `CloudStartupGateAuthority` 为 package-private，构造器 private；两个 seed、`bind` 与 `Evaluation` 都是 package-private。
- `TaskStartupCheckService` 虽为 public final，但唯一 constructor 是 package-private。
- 现有 `CloudServiceConfiguration` 只 scan `com.bot.dhxy.service` 和 `turn.client`，不 scan `com.bot.dhxy.task`；
  B3 classes 和两个 Task 不会靠现有 Spring scan 自动装配。
- B4 独占 `CloudServiceConfiguration`，且计划明确 B1/B2/B3/B4 不得顺手并入 authority-bound remote state；
  B3 不能借 B4 config 文件偷偷增加 startup authority bean。
- 40B 才拥有 real Task factory/runtime/registry，但它尚未实施。

所以 source-only B3 可以保持 dormant，但未来 named test 和 40B production 都需要一个明确、非反射的 construction seam。
该 seam 是修改 B3 两文件内的公开/包内 factory，还是由 40B 在同包/其它边界消费，当前没有父级冻结；不得由 Worker
临场扩大 visibility、使用反射或修改 B4 config 猜测。

## 10. 与 34C / 38A / 38B1 / 38B2 / 38B4 的冲突矩阵

| Card/lane | 其 production + named-test write set | 与候选 B3 物理交集 | 逻辑/API 冲突 |
|---|---|---:|---|
| TURN-34C | `task/AutoBattleTask.java` + `task/AutoBattleTaskTurnContractTest.java` | 空 | AutoBattle 是 B3 真实 caller；34C 最终交付前 caller SHA、gate 顺序和 terminal test 仍会漂移，B3 只读 |
| TURN-38A | 七个 context/checkpoint/task/template 文件 + `TaskExecutionContextOldAuthorityRemovalTest` | 空 | B3 当前调用 38A 待删 old API；post-38A source-compatible API 和原子 compile 顺序必须先冻结 |
| TURN-38B1 | `BagWorkflowState`、`CloudBagStateOwner` + `BagWorkflowStateTurnTest` | 空 | 同样消费 post-38A identity/lifecycle；两卡不得各造 context registry/session/helper |
| TURN-38B2 | `CloudReturnItemPrescanStateOwner`、`ReturnItemPrescanWorkflowState` + `ReturnItemWorkflowStateTurnTest` | 空 | 07:19 PRECHECK 已确认物理互斥；同样等待 38A，不得共享/复制 owner、TTL、ledger |
| TURN-38B4 | `CloudArtifactStore`、`ScopedPngArtifactStore`、`CloudServiceConfiguration` + `ScopedPngArtifactStoreTurnTest` | 空 | B3 不得修改 config 来解决 startup construction；B4 不得吸收 startup authority state |

当前 B1/B2 是 report-only readiness 活动，B4 是未来写集；没有证据表明其 production writer 已领取。物理交集为零只证明
未来可文件级并行，不证明 predecessor、post-38A API、construction seam 或 metadata authority 已闭合。

## 11. Future named-test 的精确范围与尚待冻结项

计划唯一类：

```text
com.yueyunfe.dhxy.cloudbrain.task.startup.StartupGateTurnStateTest
```

当前文件不存在。计划 19.4 又规定每行默认包含 `BC4+BASE`，B3 行额外是 `STATE`，字面 profile 为
`BC4+BASE+STATE`。但 startup gate 本身发出 Turn action/UUID=`0`；若把 BC4 四类 outcome 强行放进 direct caller
allow path，会进入 34C/36 Whole Task/caller 测试所有权。父级须先明确：B3 的 BC4 是否记为不适用，或指定不侵占
34C/36 的哪一个真实 public action 边界。本 helper不通过 mock、复制 Task reducer或扩大 test write set消解该冲突。

另一个 test-access 事实：按计划 alias，该 test package 与 production `com.bot.dhxy.task.startup` 不同；当前 authority、
Evaluation 和 service constructor 均不可从该 package 直接构造，Cloud POM 只有 JUnit、无 Mockito。未来测试不得用
reflection/private helper/source scan。父级必须先冻结非反射 construction seam 或纠正 test package/path。

在上述两项冻结后，唯一 named test 至少应覆盖：

| Future case | 必须穿过的 production 边界 | 精确证据 |
|---|---|---|
| `disabledPoliciesStillCheckpointBeforeAllow` | 两个 public check | active metadata read=1；action/UUID=0；disabled 不绕 stop/context |
| `fiveRingRoleMatrixIsExact` | `checkFiveRing` | LEADER allow；MEMBER/SOLO skip；UNKNOWN/invalid 按唯一 flag；零 live role probe |
| `autoBattleRoleMatrixIsExact` | `checkAutoBattle` | MEMBER allow；LEADER skip；SOLO/UNKNOWN/invalid 同投影 UNKNOWN并按唯一 flag |
| `stopWinsOverPauseBeforeAnyPolicyDecision` | 两个 public check | pause+stop 直接 stop；policy/result/caller startup side effects=0 |
| `pauseResumeRetainsSameEvaluationAndRoleFact` | 两个 public check | pause->resume 为同一 context/evaluation；250ms cadence；不重 seed/bind、不新 UUID |
| `stopWhilePausedUnwindsWithoutGateResult` | 两个 public check | pause->stop 抛 stop；不产生 ALLOW/SKIP/FAILED 或 retry |
| `missingOrWrongMetadataIsTypedTransition` | 两个 public check | empty/wrong device/wrong window 各自 exact decision；零 action/policy mutation |
| `boundEvaluationRejectsForeignContext` | authority bind + public check | A evaluation 不接受 B scope/device/window/taskRun/task/role；A->B->A 无泄漏 |
| `autoBattleSkipShortCircuitsCallerStartup` | public AutoBattle `execute` | exact SKIPPED；RUNNING/first-aid/maintenance/AutoCombat/tick=0；Holder 恢复 sentinel |
| `fiveRingSkipShortCircuitsCallerStartup` | Holder-bound public FiveRing `execute` | exact SKIPPED；RUNNING/progress/round/action=0；保留当前一次 `forceReleaseTurn` |
| `directCallerStopAndTransitionProjectionMatchesFrozenOwners` | 两个 public caller | 按 34C/36 最终合同分别锁住 stop/transition，不由 B3 test 擅自统一 |
| `policySeedAndServiceConstructionAreExplicit` | 最终非反射 factory | baseline 只能在明确 NO_OVERRIDE 后 seed；control-plane snapshot complete；无 local property fallback |

测试只允许 fake metadata port/TurnGameClient、fake Task collaborators 与 scripted context；不启动 Spring、host、server、
runtime 或 Task thread，不触发真实 capture/input。未来命令按计划为
`mvn -q -Dtest=StartupGateTurnStateTest test`，本 helper未运行。

## 12. 实施前必须重算的 SHA/size/ref

| 漂移来源 | 必须重算 |
|---|---|
| 34C 最终写入 | `AutoBattleTask` SHA/size；`checkAutoBattle` call count/位置；gate 拒绝/stop/transition/startup order |
| 36 最终写入 | `FiveRingTaskV2` SHA/size；`checkFiveRing` call count/位置；catch/finally/gate 拒绝 side effects |
| 38A 最终写入 | 七个 production 文件及其 named test SHA；old getter 是否仍存在；turn-native factory/Holder/checkpoint API |
| B1/B2/B4 开工或交付 | 三卡全部 production/test exact-path intersection；是否出现共享 context/config helper |
| metadata/runtime 计划变化 | `CloudTaskServiceMetadata`、`TurnWindowMetadata`、`TurnTaskStartRequest`、40B/40D 构造 caller 和全部 production refs |
| 权威文档变化 | 当前计划/协议/ACTIVE top 与 23/34C/35/36/37/38A dependency evidence SHA |
| B3 文件变化 | 两个目标的 full SHA/size/status；全部 inbound/outbound refs；planned-vs-real path 是否已纠正 |
| named-test 创建 | test 物理 path/package、construction access、唯一 owner、profile 与 34C/36 tests 的物理/语义交集 |

任何上述变化都使本报告中的行号、hash、caller count 或 test seam 失效；未来领取前必须从当前磁盘重新扫描，不能只引用
本 PRECHECK。

## 13. Stop-work 条件与真实 start blockers

未来 implementation 在下列任一事实仍存在时必须停止 source/test 写入并返回父级刷新合同：

1. TURN-23 或 TURN-38A 的当前 source predecessor 证据未满足；尤其 34C/35/36/37 尚未形成 38A 所需最终 source。
2. 38A 与 B1/B2/B3 后继 caller 的删除/兼容/原子 compile 顺序仍未唯一化。
3. 权威计划仍指向不存在的 `com/bot/dhxy/service/TaskStartupCheckService.java`。
4. AutoBattle/FiveRing final caller SHA、startup gate 顺序、stop/transition projection 或 test ownership 尚未冻结。
5. `CloudTaskServiceMetadata.windowRole/team/retry/startup/startedAt` 的生产 authority 与 40B/40D 构造交接点仍未知。
6. startup policy authority、Evaluation、service 的非反射 construction seam 仍无 production/test 可达入口。
7. named-test package 无法访问当前 package-private construction API，或 `BC4+BASE+STATE` 与 34C/36 scope 仍冲突。
8. 实现需要第三个 production 文件、修改 direct caller/context/protocol/config/POM/resource、第二个 test 或共享 fixture。
9. 方案增加 live role probe、额外 metadata read、TTL、retry、session/owner/ledger、durable restore、park/yield、
   自动 task reassignment，或改变 UNKNOWN/STOP/PAUSE 的条件、顺序、次数或 terminal projection。
10. 两仓 status/目标 SHA 与本快照不同却未重算全部 refs/collision，或任何既有 dirty/untracked 将被覆盖。

当前最小结论仅是：B3 的真实 production direct callers 已唯一定位为 AutoBattle/FiveRing，候选生产文件物理上与相邻卡
互斥；但 post-38A API、计划真实路径、role/team metadata authority、construction/test access 以及 caller terminal/test
边界仍需上游或父级先冻结。本报告不将这些证据转成卡片裁决。

无已批准业务差异；按当前 DHXY startup role/queue 基线、`696a12b0` 控制终态和最小 HTTPS JSON turn 等价迁移。

PRECHECK_COMPLETE TRUE_EOF
