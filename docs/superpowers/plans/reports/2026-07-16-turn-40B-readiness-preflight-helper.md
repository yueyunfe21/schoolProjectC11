# CR271 TURN-40B Cloud Real Task Factory/Queue Runtime Readiness Preflight

- 日期：2026-07-16
- 角色：CR271 Internal 非绑定 readiness helper；不是 implementation Worker、reviewer 或父级
- 性质：仅 PRECHECK、只读证据与建议冻结条件，不产生卡片结论
- 唯一写入：本报告
- 未执行：Maven、JUnit、compile/package、runtime/application/server、Task、UI、capture、input
- 未执行：Git add/commit/checkout/reset/clean/stash/rebase/merge 或任何其它 Git mutation

## 1. PRECHECK 摘要

1. 权威计划给 TURN-40B 冻结了 **5 个 Cloud production 新文件 + 2 个 Cloud test 新文件**。当前七个路径均不存在；若保持该写集，40B 必须通过构造注入消费四个真实 Task 的 prototype provider，并把 Spring 注册与 HTTP/server 激活留给 TURN-40C。
2. 直接依赖目前没有形成可消费的最终合同：
   - TURN-40A 的 8 对 production protocol 当前仍为双仓同字节，但 lifecycle 点名测试与 fresh Cloud build 尚未闭合。
   - TURN-13H 的 dormant host/routes 能力源码已存在，但点名测试与 Cloud compile 尚未闭合；现有 host 不扫描 Task 包，也没有 runtime/Task 激活。
   - TURN-39 仍在后续波次；它又传递等待 TURN-34C、35、36、37、38A/B/C。当前三大 Task 仍引用 Cloud 缺失的本地窗口类、旧 `TaskExecutionContext.builder()` / `getWindowRuntimeContext()`；当前 turn-native context 仍要求旧 `CloudTaskServiceMetadata`。
3. 当前生产调用链有两个必须由父级先冻结的接缝：
   - **Task 装配接缝**：`CloudServiceConfiguration` 只扫描 `com.bot.dhxy.service` 与 `turn.client`，40B 不能从现有 host 取得四个 Task prototype。
   - **首轮 turn 时序接缝**：`CloudTurnExchange.exchange(...)` 在内部写入 `latestWindowMetadata` 后立即进入 `awaitAction`。现有 40C 写集不含 `CloudTurnExchange.java`；handler 若在 exchange 前启动 Task，首个 checkpoint 可读到空 metadata，若等 exchange 返回后再启动，首轮会先 long-wait。不得用第二份 metadata、TTL 或自动轮询悄悄绕过。
4. 40B 可以拥有的状态只应是：每个 exact `deviceId + windowId` 的当前易失 runtime，以及该窗口最后一次接受的 `startRequestId/ack`。Task queue 是显式请求授权的当前内存执行，不是 durable workflow；不得引入 start history、session、ledger、TTL、自动 Task/action retry 或旧 remote task-run authority。

因此，当前最有价值的交付是冻结下文接口、时序与验收点；不是提前领取 Java 写集。

## 2. 权威依据与只读基线

### 2.1 已完整读取

- `D:/mavenProject/DHXY/AGENTS.md`
- `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`
- `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部当前 CR271 记录
- `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节
- `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- `D:/mavenProject/DHXY/docs/业务逻辑.md` 的本地队伍/session 边界及五倍/修罗基线门
- 本报告后续列出的 DHXY/Cloud production 与 test source

### 2.2 业务与协议基线

- 业务迁移基线仍是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 所对应的用户确认语义；TURN-40B 不获授权改变 Task phase、fallback、retry 次数、park/yield、验证顺序或过期语义。
- HTTPS turn spec `:117-126` 冻结：stable `startRequestId`、ordered allowlist、两种 queue failure policy、exact ack、当前 runtime + 最后一次 ack 的纯内存去重、pause/stop metadata、固定进程级 tenant/user/stateRoot，以及 `SLEEP_COMPUTER` 不进入 remote Task。
- 权威计划 `:1040-1043` 要求新文件只能使用第 17 节列出的名字；需要额外文件时必须先由父级修计划，Worker 不能临时扩写集。
- 权威计划 `:1047-1070` 冻结真实链路：显式 start -> exact ack -> 固定 scope host -> real Task queue -> exact holder/context -> shared `CloudTurnCommandPort` / 同一 `CloudTurnExchange`；只有显式业务 retry 才能产生新 actionId。
- `docs/业务逻辑.md:217-254` 明确禁止迁移顺手新增 TTL、额外验证、park/yield、retry、cleanup 或 fail-closed 业务规则；已验证事实不得因排队、暂停或 Cloud 命令经过几秒而过期。

### 2.3 两仓脏工作区保护快照

PRECHECK 开始时只读快照：

| Repo | Branch | HEAD | Modified/staged | Untracked | Upstream |
|---|---|---|---:|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 44 | 41 | 未配置 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 9 | 19 | 未配置 |

本 helper 不拥有任何 Java 写集，未修改、还原、暂存或移动上述既有 dirty/untracked 内容。

## 3. TURN-40B exact write set

权威计划 `:1355-1362` 与 `:1660` 给出的唯一 40B 写集如下；当前七个路径经 `Test-Path` 均为不存在。

### 3.1 Production：只创建以下 5 个文件

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactory.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRegistry.java`
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskStartResult.java`
5. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnControlPort.java`

### 3.2 Test：只创建以下 2 个文件

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactoryAllowlistTest.java`

### 3.3 明确不属于 40B 的写入

- 不修改 `CloudTurnHttpHandler`、`CloudTurnExchange`、`CloudTurnRoutes`、`CloudBrainServer`、`CloudBrainApplication`、`CloudServiceHost` 或 Spring configuration；激活接线属于 TURN-40C。
- 不修改四个 business Task、`TaskExecutionContext`、`TurnGameClient`、旧 remote facade/state owner；这些必须由 34C/35-39 交出最终 API。
- 不修改两仓 protocol；协议变化必须回到 TURN-40A/父级计划处理。
- 不增加第三个 40B 测试、fixture、source guard、helper production file、持久化 schema 或配置项。

若下文的首轮 metadata 时序最终需要修改 `CloudTurnExchange.java`，建议把它显式补进 **TURN-40C** 写集，而不是让 40B 越界。

## 4. TURN-40A 依赖 PRECHECK

### 4.1 当前已有证据

- `docs/ACTIVE_WORK.md:833-842`：父级已核对 8/8 production protocol 双仓同字节；DHXY compile 曾成功，Cloud fresh build 与 T01 lifecycle 点名测试仍待闭合。
- 本轮重新只读计算 SHA-256，八对文件当前仍同字节：

| Protocol file | SHA-256 |
|---|---|
| `TurnTaskCode.java` | `A116361EE173F37639967459111AB1CC595469EE1C8F8E3BA91E108BDB2895F7` |
| `TurnTaskQueueFailurePolicy.java` | `2B9DBFF0F75612DCC818B89825F2EC003A8BD08ADF05176D7592F4C570B5C97C` |
| `TurnTaskStartRequest.java` | `D4AF7B55DD1B4A6B01DF5EED4E9F2468B745A31241314762242C340D0FF03117` |
| `TurnTaskStartAck.java` | `B5C196C7084211AE917DB543411C08247611B0A962CD60219E478660BC1D299B` |
| `TurnWindowMetadata.java` | `E1430169AAE3E35AC9F6295E41EA401E66EFF910E0B4B4DFF954E72C9416AF1B` |
| `TurnRequest.java` | `BF7EA75A8FF44CBD5B7FB8F73ECF172BB7466B00EECC1407087688E5D74B571C` |
| `TurnResponse.java` | `646F9738FC296949BE6D1787481B8F946F2CF4CFA7D2971D5B4B987BA2F1E75F` |
| `TurnProtocolValidator.java` | `3929CFB6AFAD86FFECCF1573E108A59878541D0D06D439A4CE95F6046252B2B0` |

- `TurnTaskCode.java:3-8` 的 closed allowlist 恰为 `WUHUAN_V2`、`WUBEI`、`XIULUO_V2`、`AUTO_BATTLE`。
- `TurnTaskQueueFailurePolicy.java:3-6` 只有 `CONTINUE_ON_FAILURE` 与 `STOP_ON_FAILURE`。
- `TurnTaskStartRequest.java:5-12` 只携带 ID、ordered immutable task codes 与 policy；`TurnTaskStartAck.java:3` 只携带 ID。
- `TurnProtocolValidator.java:46-64` 只锁定同一 request/response 的 exact ack correlation；`:416-428` 锁定非空 ID、非空队列、非 null code/policy。
- 双仓 `TurnTaskLifecycleProtocolGoldenJsonTest.java` 当前 SHA 同为 `57AABF91B654A688A175BBAC7E3A7D1AC2D238852DC2D8A74A2FE24375BEE288`；DHXY 侧该测试当前为 untracked，Cloud 侧已在工作树中。

### 4.2 尚未形成 40B 可依赖结论的部分

1. T01 lifecycle test 与 fresh Cloud build 尚无本轮最终执行证据；40B 开工前应确认八对 SHA 未漂移并补齐两仓协议门。
2. Validator 不比较“同一个 ID 在不同 HTTP request 中是否携带相同 queue/policy”。协议又明确只留最后 ID/ack，故 40B 不应新增 request hash/history 作为隐形 ledger。
3. 建议冻结 **ID 是 V1 唯一幂等键**：同一 exact window 的相同 `startRequestId` 一律返回已保存 ack，绝不重新解释 payload、调用 factory 或启动 thread。DHXY 40D 必须保证未收到 ack 前只重发同一个 immutable request。
4. 新 ID 在旧 runtime 尚活跃时如何返回必须先冻结。由于 protocol ack 没有 status/error 字段，建议 40B 返回 typed internal conflict，40C 映射为 HTTP error；不得伪造 ack，也不得把新请求排成持久 backlog。

## 5. TURN-13H 依赖 PRECHECK

### 5.1 当前 dormant host 能力

- `CloudServiceHost.java:39-65` 用 exact fixed `CloudServiceScope`、state root、同一 `CloudTurnCommandPort` 与 template catalog refresh 私有 Spring context；`:74-80` 只提供 bean lookup 与 close。
- `CloudServiceConfiguration.java:27-40` 只扫描 `com.bot.dhxy.service`、`com.yueyunfe.dhxy.cloudbrain.turn.client`，并导入既有配置/assembly/holder；**没有扫描 `com.bot.dhxy.task`**。
- `CloudTurnRoutes.java:29-40` 让 handler 与 host capability 使用同一 exchange/catalog 对象。
- `CloudServiceHostTurnCapabilityContractTest.java:55-96` 的源码覆盖 same command port/catalog、`TurnGameClient`、零 command/metadata/template 调用与 refresh/close 前后零新增 live thread。
- `docs/ACTIVE_WORK.md:666-674` 记录 13H production/test source 已完成父级源码复核，但 Maven 与 Cloud compile 仍待闭合。

### 5.2 现有装配缺口

1. `CloudServiceHost` 当前没有 production caller；`CloudBrainServer.java:96-111` 创建 routes/server，但没有创建 host/runtime。
2. host 内没有四个 Task prototype。40B 若直接依赖 `host.getService(FiveRingTaskV2.class)`，按当前配置运行时无法解析；40B 又无权修改 host/config。
3. 当前 config 没有显式注册 `GameContext`。最终 per-window `GameContext.State` 的来源和 projection 必须等 38M/38C 分类与 39 收口，不能由 40B 临时 `new GameContext()` 或共享 default state。
4. 13H test 的“构造 host 不起线程”是 dormant 能力合同；40B 只能在显式、已接受的 start 后创建 worker，不能让 factory/registry/host 构造自动启动 Task。

### 5.3 建议冻结的 40B/40C provider 接缝

保持 40B 五文件不扩写的建议方案：

1. `CloudTurnTaskFactory` 是普通、无副作用 factory，构造时接收四个明确的 prototype provider/supplier；它不创建 host、不扫描 Spring、不持有 Task singleton。
2. TURN-40C 的新 `CloudTurnRuntimeConfiguration` 只注册这四个 real Task prototype 及 40B runtime beans，并通过 13H host 的同一个 private context 提供依赖。
3. TURN-40C 修改其已拥有的 `CloudServiceHost.java`，显式注册 `CloudTurnRuntimeConfiguration`；继续复用同一个 exchange-backed command port 与 catalog。
4. 不把 `com.bot.dhxy.task` 整包无筛选扫描进 host，以免把 `SleepComputerTask`、DHXY-only factory/window runtime 或其它非 allowlist Task 一并激活。
5. 每次 queue element 均由 provider 取得全新 prototype；同一 code 在同一 queue 重复出现时也不得复用同一个 mutable Task 实例。

## 6. TURN-39 与传递依赖 PRECHECK

### 6.1 权威 DAG

权威计划 `:1142-1164` 的真实传递顺序是：

```text
TURN-34C + TURN-35 + TURN-36 + TURN-37
  -> TURN-38A
  -> TURN-38B1/B2/B3/B4 + TURN-38M/38C
  -> TURN-39
  -> TURN-40B
```

TURN-39 当前仍是后续卡；它的 exact production write set 是：

- `remote/CloudGameClient.java`
- `remote/CloudTaskServicePort.java`
- `remote/CloudTaskServiceExecutionContext.java`
- `remote/CloudTaskServiceMetadata.java`
- `turn/client/TurnGameClient.java`
- `turn/client/LegacyTaskExecutionTurnContextProvider.java`

对应 test 是 `turn/client/OldFacadeRemovalContractTest.java`，当前路径尚不存在。

### 6.2 当前 source 仍不能供 40B real factory/runtime 消费

1. `TaskExecutionContext.java:96-109` 的 `turnNative(...)` 仍要求 `CloudTaskServiceMetadata`；`:204-225` 与 `:314-347` 仍保留 old scope/revision/facade public surface。
2. `CloudTaskServiceMetadata.java:29-41` 仍要求 `windowRole`、可选 `localTeamSessionKey/localLeaderWindowId`、leader/support flags、`TaskRetryPolicy`、startup mode 与 startedAt。40A start request 没有这些字段。
3. 按 `docs/业务逻辑.md:10-22,47-65`，40B 不得把缺失字段猜成 `LEADER`、自动创建 local team session，或把后加 standalone 窗口附着到已有 session。
4. 当前 `WubeiTask`、`FiveRingTaskV2`、`XiuluoTaskV2` 仍调用 `context.getWindowRuntimeContext()`；三者还保留本地 `TaskExecutionContext.builder()` 兼容构造。TURN-38A 明确负责消除 Task 对 old retained authority/context 的最后引用。
5. 当前 Cloud source 静态导入检查显示：
   - `WubeiTask` 仍引用 Cloud source tree 不存在的 `GameClientTracker`、`TextRecognizer`、`BagService`、`UICleanerService`、`TaskTransactionRunner/TaskTurnCoordinator`、`CoordinateHelper/GameStateUtil`、`MultiWindowTaskManager` 及多项 `Window*` runtime 类型。
   - `FiveRingTaskV2` 仍引用上述多项 DHXY-only 类型及 `OcrWindowScanService`。
   - `XiuluoTaskV2` 仍引用上述多项 DHXY-only 类型及 `QuestManagerService`、`ObjectiveTextRecognitionService`。
   - `AutoBattleTask` 的 direct imports 当前均能在 Cloud source tree 定位，但其最终 state/context/service graph 仍等待 TURN-34C/38/39。
6. 旧 `CloudGameContextStateOwner.java:18-25,42-103` 仍按 `RemoteTaskRunCoordinator/Scope/revision/handle` 管理状态；这是 38M/38C 要分类/重接的旧 authority，40B 不得复用为捷径。

### 6.3 TURN-39 必须交给 40B 的最终 postcondition

在 40B Worker 领取前，父级应逐项冻结并以 source/test/build 证据确认：

1. 四个 allowlisted Task 在 Cloud 可编译、可由 prototype provider 构造，并只消费最终 turn-native context/client/state API。
2. `TaskExecutionContext.turnNative(...)` 的最终参数能从固定 host scope、validated current window metadata、selected `TurnTaskCode` 与纯诊断 taskRunId 构造；不再要求 40B 猜测 team/session/old retry metadata。
3. 若业务仍真实需要 role/team/startup metadata，其权威来源必须由前置卡显式交付；40B 只透传，不生成、不默认、不推断。
4. per-window `GameContext.State` / mutable state owner 的最终 acquire-project-release API 已由 38M/38C 冻结，并且 key 至少 exact tenant/user + device/window，不依赖 old task-run owner/session/ledger。
5. active business caller 对 old `CloudGameClient`、`CloudTaskServicePort/ExecutionContext`、retained action、final-consumed 与 `RemoteTaskRun*` 为零；40B 五文件自身也必须零引用这些符号。
6. `OldFacadeRemovalContractTest` 与 fresh Cloud compile 已闭合，不能让 40B 通过旧 SCC 获得暂时可编译的假接线。

## 7. Factory allowlist 建议冻结条件

### 7.1 Exact mapping

| Protocol code | Real prototype class | Expected `getTaskCode()` |
|---|---|---|
| `WUHUAN_V2` | `com.bot.dhxy.task.wuhuan.FiveRingTaskV2` | `wuhuan_v2` |
| `WUBEI` | `com.bot.dhxy.task.wubei.WubeiTask` | `wubei` |
| `XIULUO_V2` | `com.bot.dhxy.task.xiuluo.XiuluoTaskV2` | `xiuluo_v2` |
| `AUTO_BATTLE` | `com.bot.dhxy.task.AutoBattleTask` | `auto_battle` |

### 7.2 Factory contract

1. 输入只接受 `TurnTaskCode`；不要做 string/reflection/class-name fallback，也不要映射 legacy `TaskType.XIULUO`。
2. `null` 或 provider 返回 `null` 必须在 start acceptance 前失败；不得发 ack 后才发现 queue 无 Task。
3. 每次 create 返回新 prototype，factory 本身不保存当前 Task、window state、queue history 或 thread。
4. `SLEEP_COMPUTER` 不在 enum、mapping、provider 或 config 中；不得通过 default branch 兜底到本地 Task。
5. 建议在 registry 原子接纳前同步 materialize 整个 ordered Task list。任一 provider 构造失败时不安装 runtime、不保存 accepted ack、不启动部分 queue。
6. factory 不执行 Task、不调用 `TaskExecutionContextHolder`、不读取最新 metadata、不创建 actionId，也不做 retry。

## 8. Queue runtime 建议冻结条件

### 8.1 Runtime ownership

每个已接受 window runtime 最多拥有：

- immutable ordered `List<GameTask>`；
- immutable start request ID 与 queue failure policy；
- exact `CloudServiceScope`、`TurnInvocationContext`、initial validated metadata；
- 每个 Task 对应的最终 turn-native `TaskExecutionContext`；
- 一个显式 start 后才创建的 worker thread、当前 active Task 引用与 terminal result；
- 前置卡交付的 exact per-window mutable state handle。

它不拥有 HTTP client、第二个 exchange、业务 action ledger、durable queue snapshot、resume revision、owner permit 或 retry executor。

### 8.2 Ordered execution 与 result policy

建议父级在 implementation brief 中明确锁定以下语义，避免 Worker 自选：

1. 每个 queue element 恰好调用一次 `task.execute(exactContext)`；不调用 context-free `execute()`。
2. 每次调用均由现有 `TaskExecutionContextHolder.callWith(...)` 绑定 exact context；`finally` 后 holder 恢复/清空。
3. `SUCCESS` 与 `SKIPPED` 继续下一个 Task。
4. `FAILED` 在 `CONTINUE_ON_FAILURE` 下继续，在 `STOP_ON_FAILURE` 下终止剩余 queue。
5. `STOPPED` 不受 failure policy 影响，立即终止剩余 queue。
6. `TaskStopRequestedException` / cooperative interruption 映射为 `STOPPED`；普通 Task exception 映射为 `FAILED` 后应用 policy；`Error` 的处理与 cleanup 顺序必须单独冻结，不得自动重启 Task/thread。
7. queue aggregate result 建议沿现有 DHXY `WindowTaskRunner.java:4014-4024` 的优先级：`STOPPED > FAILED > SUCCESS > SKIPPED`，但不要把 DHXY runner 的 role preflight、window manager、combat watcher、startup remap 或 local transaction/yield 复制进 Cloud runtime。
8. start ack 只表示 queue 已原子接纳并成功启动 worker，不表示第一个 Task 或整条 queue 成功。

### 8.3 Pause/stop

1. pause/resume 只消费同一 window 最新 `TurnWindowMetadata.pauseRequested`，保留同一个 runtime、Task/context/state；不创建 resume session、revision 或新 Task。
2. stop 只消费 `stopRequested` 与标准 `TaskCheckpoint`/`TaskSleep` cooperative semantics；不得新建 stop epoch/permit/owner。
3. 若 runtime 为唤醒标准 sleep/pause wait 而调用 `activeTask.stop()` 和/或 interrupt，父级应冻结调用顺序、幂等性以及“不会把 stop 解释为 failure 后继续 queue”。
4. pause 不停止 DHXY long-wait loop；stop/unregister 的本地先后由 40D 负责，Cloud 40B 只释放当前易失执行资源。

### 8.4 Terminal cleanup

无论 success、failure、stop、exception 或 close，runtime `finally` 至少释放：

- active Task 引用；
- holder/context projection；
- per-window mutable state handle；
- worker thread/lifecycle waiters；
- registry 的 current runtime 指针。

registry 仅保留最后 accepted `startRequestId/ack` 供 transport redelivery；不保留已完成 Task list、result history、exception history、时间戳 expiry 或 replay data。

## 9. Registry/dedupe 建议冻结条件

1. registry key 恰为 exact normalized `deviceId + windowId`；tenant/user 来自进程固定 host scope，不从 request body 选择，也不写进新的 session key。
2. 同一 key 同一 `startRequestId`：返回同一个 immutable ack，factory/create/worker/Task execute 调用次数均不增加；active 与 terminal 后都一样。
3. 同一 key 不同 ID 且 runtime active：返回 typed internal conflict，不替换、不 stop 旧 runtime、不排队、不保存第二条 request。
4. 同一 key 不同 ID且旧 runtime terminal：可原子 materialize/install 新 runtime，并把“最后 ID/ack”替换为新值。
5. 不同 key：各自独立 runtime/context/state；共享的 `CloudTurnCommandPort` 仍按 exact window slot 相关联。
6. registry 不保存所有历史 ID、request hash、payload copy journal、sequence、lease、deadline、last-seen time 或 completion ledger。
7. 因 V1 只保留最后一个 ID，父级应明确冻结系统假设：每个 window 只有一个串行 start issuer；40D 在收到 exact ack 前不生成下一 ID。不得为覆盖未获授权的乱序多 issuer 场景扩大为历史 ledger。
8. `CloudTurnTaskStartResult` 的内部 status/fields 与 40C HTTP 映射必须先冻结；不得通过新增 protocol ack status 绕过 40A。

## 10. 首轮 metadata / exchange 时序缺口

### 10.1 当前真实调用链证据

- `CloudTurnHttpHandler.java:152-170` 只调用 `turnExchange.exchange(...)` 并序列化其 response；没有 runtime/control 调用，也没有拼接 start ack。
- `CloudTurnExchange.java:118-132` 在一个方法内完成 request validation、previous outcome/metadata acceptance 与 `awaitAction`。
- 无 previous outcome 时，latest metadata 在 `CloudTurnExchange.java:140-147` 写入；有 outcome 时在 `:157-183` 写入。
- metadata 写入后，`CloudTurnExchange.java:190-202` 立刻在同一 HTTP 调用栈等待 action。
- `TaskExecutionContext.java:385-429` 的每次 turn checkpoint 都从 bound `TurnGameClient.latestWindowMetadata()` 读取 exchange；initial metadata 不替代该读取，empty 会变成 missing-binding transition。

因此，当前没有一个已冻结的调用点同时满足：

```text
validated request/outcome accepted
  -> exchange latest metadata 已可见
  -> runtime 原子接受 start 并启动 Task
  -> Task 可向同一 exchange 发布首个 action
  -> 原 HTTP long-wait 返回该 action + exact start ack
```

### 10.2 推荐冻结方案

首选由父级把 `CloudTurnExchange.java` 显式补入 TURN-40C exact write set，并只增加一个窄的、同步的 accepted-request hook：

1. exchange 先完成现有 request/outcome/frame correlation，并提交 latest metadata；
2. 在 `awaitAction` 前同步调用 40B `CloudTurnControlPort`；
3. control port 处理 start dedupe、pause/stop signal，返回 optional exact ack/internal conflict；
4. exchange/handler long-wait 同一个 action slot；
5. 40C handler 将 ack 拼进同一 `TurnResponse`，并用 `TurnProtocolValidator.requireValid(response, request)` 做最终 correlation 后序列化。

这个 hook 不保存新状态、不创建第二 transport，也不改变 action/outcome mechanics。

如果父级坚持不修改 exchange，则必须先写出并审查另一套 deterministic ordering 方案及对应 40C test。不可接受的替代包括：

- handler/exchange/runtime 各保存一份 latest metadata；
- runtime 用 TTL 等待 metadata；
- 通过 sleep/poll 自动重试首个 checkpoint；
- 对同一个 HTTP request 调用两次 exchange 以“预热” metadata；
- 等 long-wait 返回 IDLE 后才启动 Task；
- 创建第二个 `CloudTurnExchange` 或 wrapper authority 让 host 与 route 不再共享同一 capability。

## 11. Stop/unregister 与 exchange state 的待冻结边界

1. 当前 `CloudTurnExchange` 的 per-window state map 没有 remove API，并保留 latest metadata、last accepted action ID/hash 与 unresolved action fence。
2. 40B terminal cleanup 只能释放 Task runtime/state；它不应越权清除 unresolved action fence，否则可能把 uncertainty 变成重复物理执行。
3. 权威 `LIFE` profile 又要求 stop/unregister 两端释放。父级应在 40C/40D brief 中明确“释放”是否只指 Task runtime，还是还包括安全条件满足后的 exchange slot eviction。
4. 若要求 exchange eviction，必须给出无 unresolved action、无 waiter、runtime 已 terminal 的 exact 条件，并补进 40C 写集/test；不得用 TTL 清理，也不得让 40B 猜测 transport uncertainty 已结束。
5. Server close 顺序仍按计划 `:1364-1372`：先关闭 task runtime/registry，再关闭 host，随后 server/executor；close 必须幂等并等待 worker terminal cleanup。

## 12. 禁止机制冲突矩阵

| 容易出现的捷径 | 与权威边界的冲突 | 建议冻结 |
|---|---|---|
| 保存所有 `startRequestId`、payload hash、completion history | 形成 lifecycle ledger/history | 只保存 current runtime + last ID/ack；ID 是 V1 唯一幂等键 |
| 把 queue/task phase 序列化到 state root | 形成 durable workflow | queue 仅当前进程内存；进程重启不恢复 Task |
| 为 runtime/ack/state 加 N 秒过期 | 新增 TTL，且可能破坏 pause/已验证业务事实 | 无 TTL；只按 explicit terminal/new start/server close 释放 |
| Task exception 后自动 new Task/restart thread | 自动业务 retry | 一次 queue element 一次 execute；按 policy 返回/继续 |
| command timeout 后重新发布同 action | 破坏 actionId uncertainty fence | 直接沿 `CloudTurnCommandResult` 向 Task 返回，不重发 |
| 复用 `RemoteTaskRunCoordinator`、`CloudTaskRunActionLedger`、retained action/final consumption | 重新引入 old owner/session/ledger SCC | 等 TURN-39 零引用后只用 `TurnGameClient`/final context |
| 缺 role 时默认 `LEADER` 或创建 `localTeamSessionKey` | 改变本地队伍业务边界 | 前置卡给权威 metadata；没有就停止装配，不推断 |
| 每 window 创建一个 Cloud host | 把 window runtime 变成隐式 session，偏离 fixed single-scope host | 每进程一个 configured tenant/user/stateRoot host；registry 按 device/window 分 runtime |
| Spring 扫描整个 Task 包 | 可能激活 `SLEEP_COMPUTER`、local factory/window runtime | 40C 只注册四个 allowlisted prototype |
| 复制 `WindowTaskRunner` 全部流程 | 把 local role/startup/park/watcher workflow 搬成第二 runtime | 只复用已冻结的 queue result/policy 语义；业务留在迁移后的 Task |
| 用第二份 metadata cache 解决首轮竞态 | 多 source-of-truth，pause/stop 可漂移 | 在 shared exchange 接受 metadata 后、await action 前调用 control hook |
| terminal 时无条件清 exchange slot | uncertainty 下可能允许重复 physical action | runtime cleanup 与 transport fence cleanup 分开冻结 |

## 13. 两个 40B 点名测试的建议 exact scope

本 helper 未创建或运行测试。以下只是父级冻结 implementation brief 时的建议断言；仍只使用计划中的两个文件。

### 13.1 `CloudTurnTaskFactoryAllowlistTest`

1. `TurnTaskCode.values()` 恰好四项，并逐项映射到第 7.1 节的 real Task class/code。
2. 每项连续 create 两次返回不同 prototype identity。
3. queue 中重复 code 也获得不同 instance。
4. null code、null provider result 在 runtime acceptance 前拒绝。
5. 无 string/reflection fallback、无 `SleepComputerTask` provider、无 old facade/remote run dependency。
6. factory construction/create 均不启动 thread、不执行 Task、不调用 command port。

### 13.2 `CloudTurnTaskRuntimeContractTest`

1. scripted `GameTask` 按 request list 原顺序执行，exact holder context 在每次 execute 内可见，退出后恢复/清空。
2. 四种 `TaskRunResult` 的 aggregate 与两种 failure policy 按第 8.2 节执行；STOP 永远短路。
3. 同一 window 同一 ID 的串行与并发重复 submission 只 materialize/start/execute 一次，并返回同一个 ack。
4. active runtime 上的新 ID 不替换旧 runtime；terminal 后的新 ID 可启动新 runtime并替换 last ack。
5. 不同 window 可各有 runtime，但 context/state/device/window 不交叉。
6. pause 后不创建新 Task/context/state；resume 继续同一执行；stop 终止剩余 queue。
7. Task exception、factory failure、thread-start failure、explicit close 均按已冻结语义清理；无自动 retry。
8. terminal 后 current runtime、active Task、holder projection、state handle 与 live worker 均释放，只剩 last ID/ack。
9. construction inert：new factory/registry/runtime configuration 不启动 Task/thread；只有 explicit accepted start 启动。

首轮 shared exchange metadata -> runtime -> action -> ack 的真实接线断言应归 TURN-40C `CloudTurnActivationContractTest`，避免在 40B fake seam 中伪造已不存在的生产顺序。

## 14. 建议父级冻结清单

在派发 TURN-40B implementation Worker 前，建议父级逐项写入正式 card/brief：

1. TURN-40A 八对 SHA 未漂移，T01 lifecycle tests 与 fresh Cloud build 证据完整。
2. TURN-13H named test 与 fresh Cloud compile 证据完整，host final API 未漂移。
3. TURN-34C/35/36/37/38A/B/C/39 已交付最终 API；四 Task Cloud compile、old facade 零 active reference、`OldFacadeRemovalContractTest` 已闭合。
4. `TaskExecutionContext.turnNative(...)` 最终 signature，以及 role/team/startup metadata 的唯一权威来源。
5. 38M/38C 最终 per-window `GameContext.State` acquire/project/release API；40B 不得复用 old owner。
6. factory 四个 prototype provider 的 exact constructor/API；40C 只注册四个 allowlisted Task。
7. registry key、same-ID、active-different-ID、terminal-new-ID 与单一串行 start issuer 语义。
8. ack 的原子时点，以及 factory/thread-start/internal conflict 到 40C HTTP response/error 的映射。
9. queue result/exception/`Error`/aggregate semantics；一次 element 一次 execute，零自动 retry。
10. pause/stop 的 exact cooperative order、interrupt 幂等性及 STOP 永不继续 queue。
11. terminal cleanup、server close 与 optional exchange slot eviction 的 exact 顺序。
12. 首轮 metadata accepted-before-runtime 的生产时序；建议显式把 `CloudTurnExchange.java` 加入 40C 写集并采用 accepted-request hook。
13. 40B 仍严格只有第 3 节 5+2 文件；若任何条件需要新增 40B 文件，父级先修权威计划再派 Worker。
14. 明文写入：无 durable workflow/session/ledger、无 TTL、无 start/action/Task 自动 retry、无业务差异。

## 15. 本轮验证声明

- 只做了 source/document/status/hash/static-reference 读取。
- 没有启动应用、server、Task、UI、capture 或 input。
- 没有运行 Maven、JUnit、compile/package；因此本报告不把当前 Cloud source 描述为已构建可运行。
- 没有进行 Git mutation，也没有触碰另一 helper/Worker 的写集。
- 唯一新增文件是本报告。

PRECHECK_COMPLETE
