# CR271 TURN-16 / TURN-23 non-binding readiness helper

- 角色：CR271 非绑定 readiness helper；不是 reviewer，不是 manager，不领取实现写集。
- 审计时间：`2026-07-15T22:13:23.722-04:00`（America/New_York）。
- 权威计划：`docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- 总结论：`NEEDS_PARENT_DECISION`。
- 执行边界：两仓源码、计划、报告和 Git 状态全程只读；未运行 Maven、JUnit、compile、package、runtime、
  application、server、Task、UI、capture 或 input；未执行 Git mutation。唯一写入是本报告。

## 1. 两仓只读快照

| Repo | Branch | HEAD | tracked dirty | untracked | 说明 |
|---|---|---|---:|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 39 | 39 | status 共 78 项；全部保护 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 8 | 19 | status 共 27 项；`src/main/java/com/bot/` 整树仍为 untracked |

本次关注的 Cloud `DialogService.java`、`PlayerStateService.java`、`ClientIdentityService.java`、
`TurnGameClient.java`、`CloudBagUseIncensePort.java` 均显示为 untracked，不能用 Cloud HEAD 当其业务基线；相关
业务段直接与 DHXY `696a12b0` 的 `git show` 内容对账，没有覆盖、清理或格式化任何并发 worker 文件。

## 2. 适用权威规则

1. `docs/DHXY_CONTEXT.md:7-25` 固定 CR271 主链、named-test 门、四个永久本地 Service 和 `696a12b0` 基线。
2. 权威计划 `:881-889` 明确第 16-18 节覆盖旧第 5-13 节冲突内容；新文件名和写集必须以第 17 节为准，
   Worker 不能临时扩写集。
3. 权威计划 `:891-915` 的合法调用链是当前 exact `TaskExecutionContext` -> bound `TurnGameClient` -> shared
   `CloudTurnCommandPort` -> 同一个 `CloudTurnExchange` -> DHXY exact-window action；一次显式 client 调用只生成
   一个 UUID，uncertain 不自动重发业务动作。
4. 协议规格 `:56-67,83-113` 固定五种 step、closed local operation、单 frame 和无隐式 retry；其中
   `:58-60` 尚未列 `MOVE_MOUSE`，这是权威计划 `:351-360` 已识别并交给 TURN-09R 修复的已知规格漂移。
5. 权威计划 `:1265-1305` 固定 `HTTPS_TURN_CONTRACT_TEST_FAMILY`、fake-only 测试、逐卡 named test、
   `IMG/LX/LS/STATE/BASE` 语义和最终测试/构建门。本 helper 不执行这些门。

## 3. TURN-16 前置核对

### 3.1 四个前置的真实源码审查状态

| 前置 | 直接证据 | readiness 解释 |
|---|---|---|
| TURN-02R | `TURN-02R-PROD.md:88-110`：父级 `P0/P1/P2=0/0/0`，`SOURCE REVIEW PASSED / TEST + BUILD PENDING`，owner 释放 | source start gate 已满足；测试/构建仍待 cohort |
| TURN-13G | `TURN-13G.md:358-374`：Repair #2 父级 `0/0/0`，production/test source 通过，owner 释放 | source start gate 已满足；标准 Maven/Cloud compile 尚未到达该卡 |
| TURN-13H | `TURN-13H.md:134-159`：Repair #1 父级 `0/0/0`，shared command-port/catalog 与 host 注入 source 通过 | source start gate 已满足；named test/compile 尚待 cohort |
| TURN-13C | `TURN-13C.md:187-209`：父级 `0/0/0`，exact context/bound client source/test source 通过，owner 释放 | source start gate 已满足；named test/compile 被共享缺类债截在 JUnit 前 |

权威注册表 `:933,953-955` 与四份父级报告一致。因此，旧 TURN-16 报告 `:32-88` 所记的 command-port/host
前置已经在 source 层由 13H/13C 关闭，不能继续把它当当前唯一等待条件。它仍不是最终卡关闭证据，且 production
Task factory/真实 host activation 仍归 TURN-40B；这不妨碍业务卡做 source cutover，但禁止声称当前可运行。

### 3.2 合法新 client 路径已经存在

当前 source 路径可闭合为：

```text
DialogService
  -> CloudGiveItemLocalServiceClient (应创建在 turn/client)
  -> TurnGameClient.localService(...)
  -> CloudTurnActionFactory.localService(...)
  -> shared CloudTurnCommandPort / same CloudTurnExchange
  -> DHXY LocalServiceStepDispatcher
  -> GiveItemLocalOperationExecutor
  -> GiveItemService
```

证据：

- Cloud `TurnGameClient.java:20-25,64-84,128-168`：无 retry/cache/lifecycle，bound view 复用同一 provider/factory/
  command port；`localService` 一次显式调用只走一次 `invoke`、一个 UUID、一个 command。
- Cloud `CloudBrainServer.java:49-65,96-107`：server 现在保留 routes bundle 的同一 `commandPort` 和 catalog；
  HTTP `/turn` handler 与保留 capability 来自同一 bundle。
- Cloud `CloudServiceHost.java:39-60`：host 把传入的同一 `CloudTurnCommandPort`/catalog 注册进 Spring context。
- Cloud `CloudServiceConfiguration.java:22-40,69-72`：只扫描 `com.bot.dhxy.service` 与窄
  `com.yueyunfe.dhxy.cloudbrain.turn.client`，并提供 stateless action factory。
- 权威写集 `:1086-1087` 要求新文件为
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudGiveItemLocalServiceClient.java`。旧 TURN-16 报告
  `:8,22-23` 的 `turn/CloudGiveItemLocalServiceClient.java` 是被第 17 节覆盖的陈旧路径，不能沿用。

### 3.3 真实 production caller 与当前旧路径

真实 source caller 是 Cloud `FiveRingTaskV2`：

1. `FiveRingTaskV2.java:2389-2396` 调用
   `dialogService.handleDialog(DialogHandleRequest.giveItemIfAvailable(...))`。
2. `DialogService.java:217-228` 在 `GIVE_ITEM_IF_AVAILABLE` policy 进入
   `tryGiveItemFromCurrentOptionDialog(item,index)`。
3. `FiveRingTaskV2.java:2058-2125` 消费全部业务 terminal：
   `GIVE_ITEM_DONE -> SYNC_TASK_PANEL`；`NO_DIALOG -> SYNC_TASK_PANEL`；
   `GIVE_OPTION_NOT_FOUND -> cleanup + SYNC_TASK_PANEL`；`INTERRUPTED -> STOPPED`；其余失败累加
   `uiErrorCount`，到 6 次才失败，否则 yield 后重读 tracker。

当前 GiveItem 分支没有调用旧 remote transport：

- `DialogService.java:81-94` 直接注入 DHXY-only `InputSequences`、`InputProvider`、`GameClientTracker`、
  `CoordinateHelper` 和 Cloud 中不存在源码的 `GiveItemService`。
- `DialogService.java:1503-1535` 直接执行 desktop mechanics；Cloud 全仓 GiveItem 引用扫描没有
  `CloudGiveItem*` port/client，也没有 `GiveItemService.java`。
- 同一个 `DialogService` 的 detection/OCR 周边仍使用旧 port；例如
  `CloudDialogDetectionPort.java:18-35,47-63` 经 `context.getGameClient().executeLocalMacro(...)`。这些周边旧
  transport 归 TURN-25/26，不是 TURN-16 可以顺手替换的路径。

所以 TURN-16 的旧路径准确描述应是：GiveItem branch 仍是“Cloud 中无法装配的 lifted local dependency”，不是
一个已经存在、可直接替换的 GiveItem old-transport client。

### 3.4 `696a12b0` 不可拆顺序

Cloud 当前 `DialogService.java:1503-1535` 与
`git show 696a12b0:src/main/java/com/bot/dhxy/service/DialogService.java:1350-1382` 文本等价。必须完整保留：

1. `itemToGive == null -> GIVE_ITEM_FAILED`。
2. 非 input worker 时只获取一次 `dialog:giveItemFlow` exclusive input ownership，随后在同一 worker callback
   内继续；acquire 失败/中断映射 `INTERRUPTED`。
3. 在 small-dialog exact rect 内以
   `images/template/dialog/maintenance/dialog_opt_give.png`、threshold `0.85` 查 Give entry。
4. 未找到时返回独立 terminal `GIVE_OPTION_NOT_FOUND`，不能折叠成普通 give failure。
5. 命中点先做 `getRandomizedPoint(point,20,5)`，再 `clickLeft(x,y,150)`。
6. 固定等待 `800ms`；sleep 中断映射 `INTERRUPTED`。
7. 仍在同一个 exclusive ownership 内调用
   `GiveItemService.executeGiveDirectForExclusive(itemToGive,knownBagIndex)`。
8. DHXY `GiveItemService.java:50-68,89-102` 再按既有顺序等待 `800ms`、在已打开的 GIVE_BAG 选择物品、
   以 `+/-20,+/-8` 随机点点击 Give 按钮 `100ms`、等待 `1000ms`，真实 boolean 映射
   `GIVE_ITEM_DONE/GIVE_ITEM_FAILED`。

### 3.5 新发现的精确缺口

DHXY `LocalServiceStepDispatcher.java:64-74` 的确为 `GIVE_ITEM_FROM_OPEN_DIALOG` 获取一次 exclusive ownership，
但 `GiveItemLocalOperationExecutor.java:44-53` 只调用
`GiveItemService.executeGiveDirectForExclusive(template,index)`；现有 test
`GiveItemLocalOperationExecutorContractTest.java:30-53,94-110` 也只证明这一次 delegate。该 delegate 从
“GIVE_BAG 已经打开”开始，不包含上节第 3-6 步的 Give entry 查找/随机点击/800ms，也只能返回 `given:boolean`，
无法保留 `GIVE_OPTION_NOT_FOUND` 与普通 item/give failure 的区别。

因此，按当前写集只做 `DialogService -> CloudGiveItemLocalServiceClient.localService(...)` 会跳过外层步骤；先让
Cloud 用一个 turn 点击 Give entry，再发第二个 local-service turn，又违反 `DialogGiveItemTurnContractTest` 的
“open-dialog GiveItem 一个闭合 action，不拆二次命令”。把 `MATCH_TEMPLATE(onMatch=CLICK)` 与
`LOCAL_SERVICE` 塞进同一个 JSON action 也不能证明一个 global-input exclusive：两种 step 当前分别取得 queue
ownership，而且 match center click 会丢失基线随机点和独立 `GIVE_OPTION_NOT_FOUND` terminal。

### 3.6 当前权威写集与需要父级裁决的扩集

当前权威 production/test write set 是：

- Cloud production：
  - `src/main/java/com/bot/dhxy/service/DialogService.java`
  - Create `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudGiveItemLocalServiceClient.java`
- Cloud named test：
  - Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogGiveItemTurnContractTest.java`
- 不含 DHXY Java；不含 `GiveItemService.java`、协议、Task、Server/routes。

这个写集不足以闭合第 3.4 节的完整 baseline。建议父级选择并冻结以下路径：

**推荐路径：扩 TURN-16 的唯一写集。** 额外加入：

- DHXY production：`src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
- DHXY named test：`src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`

让 DHXY adapter 在 dispatcher 已持有的同一个 exclusive callback 内完整执行 Give entry template match、随机点击、
800ms、现有 `GiveItemService.executeGiveDirectForExclusive`，并返回严格 typed JSON state，至少区分
`GIVEN`、`GIVE_OPTION_NOT_FOUND`、`GIVE_FAILED`；stop/interruption 仍走机械 terminal。Cloud client 严格解析该
state，`DialogService` 只做既有 `DialogResultStatus` 映射。无需修改 `GiveItemService`、协议或 Task，也没有第二
command/retry。

若父级不扩 DHXY adapter/test 写集，只能明确批准“先点 entry、再发 local-service”的输入边界/terminal 变化；
这不是当前 `696a12b0` 等价迁移，也不满足现有 TURN-16 named-test 文义，不建议这样冻结。

### 3.7 可直接冻结的 TURN-16 next brief

```text
TURN-16 start dependencies: TURN-02R/13G/13H/13C source review passed; source owners released.
Status for dispatch: wait for parent write-set decision below.
Production write set (recommended):
  C DialogService.java
  C Create turn/client/CloudGiveItemLocalServiceClient.java
  D GiveItemLocalOperationExecutor.java
Test write set:
  C Create service/DialogGiveItemTurnContractTest.java
  D local/GiveItemLocalOperationExecutorContractTest.java
Acceptance:
  - one Dialog public give invocation -> one UUID -> one command -> one LOCAL_SERVICE step;
  - DHXY one exclusive ownership contains exact 696 order:
    option template 0.85 -> random(20,5) -> left click 150 -> wait 800 ->
    existing GiveItemService direct-exclusive sequence;
  - preserve GIVE_OPTION_NOT_FOUND, GIVE_ITEM_DONE, GIVE_ITEM_FAILED, INTERRUPTED separately;
  - exact target template/knownBagIndex, strict completed JSON, wrong operation/window/step/result fail closed;
  - COMPLETED/FAILED/STOPPED/UNCERTAIN and malformed JSON make zero retry/second command;
  - no old CloudGameClient/local-macro fallback, no GiveItemService/protocol/Task/server change.
Named commands are parent-run after stable writers; implementation worker must not run runtime/input.
无已批准业务差异；按 696a12b0 基线等价迁移。
```

## 4. TURN-23 readiness 核对

### 4.1 已满足和未满足的真实 start 条件

已满足：

- TURN-14 Repair #1：`TURN-14.md:297-310` 父级重审 `P0/P1/P2=0/0/0`，production/test source 通过，
  owner 已释放；named test/compile 仍待 stable-writer cohort。
- TURN-18：`TURN-18.md:74-103` 父级 `0/0/0`，`ClientIdentityService` exact metadata 读取与 named-test source
  通过，owner 已释放；named test/compile 仍待 cohort。
- 权威注册表 `:965,969,974` 与 `ACTIVE_WORK.md:3-15,62-74` 同步。因此原 `S=14+18` 在 source start
  gate 层已经满足。

尚未满足的真实前置：

1. **TURN-09R 必须补入 TURN-23 的 source start 依赖。** 权威计划 `:351-360` 明写连续
   `MOVE_MOUSE -> WAIT -> mouse input` 要一次提交，并明确“含多 click first-aid closed action”。TURN-09R 报告
   `:45-67` 当前只有 `CLAIMED` 记录；Cloud `TurnInputAction.java:3-13` 在本次读取时仍没有 `MOVE_MOUSE`。
   TURN-23 若先下发 cached-plan/foreground first-aid 的多次 right-click，会重新产生可跨窗口插入的非原子序列。
2. **snapshot 前的条件式鼠标清障没有可表达的等价 source。** `696a12b0` 与当前
   `PlayerStateService.java:792-853,1086-1089,1183-1186` 都先读取游戏机当前 pointer，只有 pointer 覆盖 bar/
   incense ROI 时才移动到安全窗口点并等待 `300ms`。Cloud `TurnWindowMetadata.java:3-11` 没有 pointer 坐标；
   Cloud 自己的 `MouseInfo` 读到的是 Cloud 主机鼠标，不是 DHXY 游戏机鼠标。TURN-09R 只解决“能原子 move”，
   没解决“何时需要 move”。
3. 权威计划 `:875,1006` 与 TURN-13H 报告 `:151-159` 都说明当前仍无 production Task factory/host caller；
   它归 TURN-40B。下节列的是可达 source caller，不是当前已经启动的 runtime caller。

第 2 点需父级先冻结一种方案：

- 严格等价方案：扩 foundational metadata/protocol/executor，让 DHXY 提供 pointer-over-ROI 条件或 generic
  capture 前清障能力；这超出 TURN-23 当前写集。
- 简化方案：明确授权 affected focused snapshot 前总是发送 baseline-safe in-window `MOVE_MOUSE + WAIT 300`；
  这会增加原本 pointer 未遮挡时不存在的输入，必须作为显式输入顺序差异记录，不能由 Worker自行推断。
- 不移动直接 capture 会改变 hover 遮挡时的检测/补香结果；在 Cloud 使用 `java.awt.MouseInfo` 则绑定错机器，
  两者都不应写进 frozen brief。

### 4.2 TURN-23 当前精确 production/test write set

按权威计划 `:1098-1100,1430`：

- Cloud production：
  - `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  - `src/main/java/com/bot/dhxy/service/ClientIdentityService.java`
  - Create `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudPlayerStateFirstAidPort.java`
  - Create `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudPlayerStateIncenseStatusPort.java`
- Cloud named test：
  - Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/PlayerStateTurnContractTest.java`
- 无 DHXY Java、Task/caller、协议、Server/routes 或额外 production model 文件。

上述两个 `remote/CloudPlayerState*.java` 和 named test 当前都不存在，必须是 Create。当前同名旧类实际位于
`src/main/java/com/bot/dhxy/service/playerstate/`：

- 旧 first-aid port `:19-30,81-107` 仍经
  `context.getGameClient().executeLocalMacro(LocalMacroKind.PLAYER_STATE_FIRST_AID,...)`。
- 旧 incense port `:13-24,43-71` 仍经 `context.getGameClient().capture(...)`。
- `PlayerStateService.java:31-32,83-84` 仍 import/inject 这两个旧 package 类。

TURN-23 应让 `PlayerStateService` 只引用计划中的新 `com.yueyunfe...remote` ports；旧类不在本卡写集，不能修改或
删除。计划“DHXY 无 first-aid macro”在本卡可执行的精确含义只能是 Cloud production 对
`PLAYER_STATE_FIRST_AID` old macro 零调用/零引用；物理删除旧 DHXY/Cloud retained authority 归后续 manifest/
deletion cohort，不能在 TURN-23 越权完成。

`ClientIdentityService.java:27-31,39-61,71-92` 已是 TURN-18 合法实现：只读 current exact
`TurnGameClient.latestWindowMetadata()` 一次，missing/blank/malformed/mismatched 均不改角色，不创建 action、
第二 cache/type 或 fallback。TURN-23 只能保留/集成它，不能重新发明 identity transport。

### 4.3 真实 source caller 清单

以下“真实”表示 production source 中存在从 Task `execute`/phase 或实际 Service flow 到该 public API 的直接调用；
不表示 TURN-40B 已经建立 runtime host。

#### Identity

- `PlayerStateService.java:159-163`：`syncMyIdentity -> ClientIdentityService.scanAndSyncIdentity(me)`。
- `PlayerStateService.java:207-210`：只有 `syncAll()` 内部调用 `syncMyIdentity()`；全 Cloud production 扫描没有
  `syncAll()` 或 `syncMyIdentity()` 外部 caller。
- `ClientIdentityService.java:39-62` 的 `scanAndSyncIdentity` 也没有其它 production caller。
- `ACTIVE_WORK.md:931-944` 已有父级历史核对：active Cloud 中 `syncAll` 零 caller，且没有 production host 把
  exact context 与 retained `GameContext.State/me` 投影到同一调用栈。因此 TURN-23 不能把 identity 写成“已有
  runnable caller 覆盖”；它只是必须保持可被后续 whole Task/startup wiring 使用的合法 Service surface。

#### First-aid / PlayerState recovery

- `AutoBattleTask.java:98-116,135`：`execute -> runAutoBattlePatrol -> performStartupFirstAidCheck(context)`。
- `WubeiTask.java:332-356`：`execute(context)` 先 `performStartupFirstAidCheck`，下一行才做 leader incense。
- `FiveRingTaskV2.java:228-241,743-765`：真实 phase path 调 `performStartupFirstAidCheck`。
- `XiuluoTaskV2.java:303-354`：首轮 execute path 调 `performStartupFirstAidCheck`。
- `AutoCombatService.java:345-409`：`consumeExitAndRecover` 调
  `probeAndConsumeHealthyFirstAidNoFocus`（`:382,397`）、`performCachedFirstAidPlanNow`（`:399`），随后 incense
  （`:409`）；`:362` 先 reset counter。
- `AutoCombatService.java:442-472`：deferred leader recovery 再调用 probe（`:462`）、cached plan（`:464`）、
  incense（`:472`）。
- `AutoCombatService.java:520-577`：follower pending flow 先消费 cached plan；没有 plan 时 probe，再消费新 plan。
- `WubeiTask.java:1768,4133`：任务 phase 直接 probe/消费 cached plan。

外部 production 扫描对 `healAll()`、`healAll(context)`、`healPlayer()`、`healPet()`、两个 `checkAndHeal(...)`
overload 均为零 caller；这些是现有 public surface，是否保留由基线/API compatibility 决定，不能冒充新的 runnable
coverage。

#### Incense

- `TeamReturnService.java:67`：第一次找到归队按钮后调用 `ensureSheYaoXiangActive(context)`；后续 fresh button
  重读顺序归 TURN-22，TURN-23 不改 caller。
- `AutoCombatService.java:409,472`：normal/deferred leader post-combat recovery 在 first-aid 后调用 leader incense。
- `WubeiTask.java:355-356`：startup 固定 `first-aid -> incense`。
- `FiveRingTaskV2.java:1109`：在 caller 已持有的 open-main-bag session 内调用
  `ensureSheYaoXiangActiveInOpenMainBag`；该 caller 的最终 BagService cutover 明确留 TURN-36，TURN-23 不得二次
  开包或改 Task。
- `XiuluoTaskV2.java:594,1110,1933`：startup、startup-prepare、before-target-nav 三个既有 leader incense 点。

#### 其它必须保留的 PlayerState source caller

- `NavigationService.java:821`、`WubeiTask.java:3878,3904`、`FiveRingTaskV2.java:1830`、
  `XiuluoTaskV2.java:1561,2302,2845,2867,2894` 调 `syncMyPosition()`。这些位置读取不属于 TURN-23 新
  legacy coverage key，但改 `PlayerStateService` 时必须保持 public API/返回/状态顺序。

### 4.4 与当前卡的写集冲突

| 当前卡 | production/test 写集 | 与 TURN-23 的关系 |
|---|---|---|
| TURN-14 | `CloudBagUseIncensePort`、`ReturnItemPrescanService`、`PlayerStateService`、`CloudBagLocalServiceClient`；`ReturnItemPrescanTurnContractTest` | 唯一真实重叠是 `PlayerStateService.java`；Repair #1 已父级 source 复审通过并释放 owner，当前不再并发冲突。TURN-23 必须以其当前内容为输入，`CloudBagUseIncensePort` 只读 |
| TURN-20 | `AutoCombatPanelService.java`；`AutoCombatPanelTurnContractTest.java` | 文件完全互斥；报告已交 source/test source，仍等待父级门。它不授权 TURN-23 改 `AutoCombatService` |
| TURN-24A | `BattleRadarService.java`；`BattleRadarTurnContractTest.java` | 文件完全互斥；报告已交 source/test source，仍等待父级门 |
| TURN-29 | 十个 TaskTracker core/model 文件；`TaskTrackerPanelTurnContractTest.java` | 文件完全互斥；Task caller 仍留 TURN-30/31/32 |
| TURN-18 | `ClientIdentityService.java`；`ClientIdentityTurnMetadataContractTest.java` | 与 TURN-23 重叠 `ClientIdentityService.java`，但 owner 已释放；TURN-23 必须保留 TURN-18 exact metadata 合同 |
| TURN-09R | 双仓 protocol enum/validator，DHXY mapper/executors 及各自 tests | 文件互斥但语义前置未满足；TURN-23 first-aid ordered multi-click 必须消费它，不能仅因无文件重叠就并发实施 |

所以在 TURN-14/20/24A/29 这四张用户指定卡中，只有 TURN-14 曾经占用 TURN-23 production 文件；该占用现已
source 交付并释放。当前真正阻止 TURN-23 发卡的不是 20/24A/29 写集，而是 TURN-09R 尚未 source 交付，以及
pointer-over-ROI 条件无法在现有协议/写集中等价表达。

### 4.5 建议唯一 `legacyCoverageKey`

建议冻结：

```text
legacyCoverageKey=AutoCombatService -> PlayerStateService::probeAndConsumeHealthyFirstAidNoFocus
```

理由：

1. 权威旧卡描述 `:562-568` 要求 TURN-23 首先绑定 `AutoCombatService -> PlayerStateService` 的真实 caller；
   当前直接调用确实存在于 `AutoCombatService.java:382,397,462`，且其结果决定 healthy consume、pending/cached
   plan 与后续 leader recovery。
2. 不使用 identity key：`syncAll/syncMyIdentity` 当前零外部 caller，`ClientIdentityService::scanAndSyncIdentity`
   已有历史覆盖，不能重复登记。
3. 不使用 `performStartupFirstAidCheck`、`performCachedFirstAidPlanNow`、`ensureSheYaoXiangActive`：这些名字在历史
   count-unit 报告中已经分别出现；本 key 只用于本次计划的去重/查漏，不写 `countDelta`，也不更新旧 ledger。
4. 不使用 `PLAYER_STATE_FIRST_AID`：它是待清除的旧 transport operation，不是“真实 caller”键。

### 4.6 `PlayerStateTurnContractTest` named-test acceptance

固定路径/命令（仅供后续 implementation/parent gate；本 helper未运行）：

```text
Cloud test path:
  src/test/java/com/yueyunfe/dhxy/cloudbrain/service/PlayerStateTurnContractTest.java
Parent-run named command after all Java writers stabilize:
  cd D:/mavenProject/dhxy-cloud-brain
  mvn -q -Dtest=PlayerStateTurnContractTest test
Profiles:
  IMG+STATE, plus applicable BASE/BC4 assertions from Section 19
```

最低验收应直接冻结为：

1. **Exact context/metadata**：每个 port 从 current `TaskExecutionContext.getTurnGameClient()` 取 exact bound
   device/window；missing/mismatched metadata、错 action/window/step、缺 frame、坏 PNG/SHA/dimensions/region
   均 typed fail-closed，零旧 client/fact/local-macro fallback，零自动 retry。
2. **Identity integration**：valid title 精确按 server -> name -> id 更新；null player、missing/blank/malformed/
   mismatched metadata 不改旧值；每次调用 metadata read=`1`、execute=`0`，不造第二 cache/type/action。测试只证明
   Service integration，不声称当前已有生产 caller。
3. **No-focus first-aid observation**：从 metadata window origin 构造 baseline window-relative bars ROI
   `(823,85,198,17)` 的 exact screen-absolute `TurnRegion`；只消费同 command raw PNG，在 Cloud 内按现有四 toggle/
   threshold、颜色和 sample 规则形成 ordered targets。healthy 只在 wrapper 中 `checksDoneThisRound++`；needed
   保存 plan；unreadable/uncertain 清 plan 并返回 UNKNOWN，不发 input action。
4. **Startup order/state**：`performStartupFirstAidCheck` 先 checkpoint，再清
   `checksDoneThisRound=0,lastCombatExitTime=0`，以 `ignoreTimeInterval=true` 执行一次 foreground check；
   `MAX_CHECKS_BETWEEN_BATTLES=1`、`HEAL_TIME_INTERVAL=5000` 和现有 failure 后 counter/return 语义不变。
5. **Foreground/cached first-aid action**：等待 TURN-09R 后，一个 input action 内保持
   `MOVE_MOUSE -> WAIT 300 -> [CLICK_RIGHT target -> WAIT 800]...`；target 次序仍为人物血、人物法、宝宝血、
   宝宝法的 enabled/needed 子序列。一个 execute invocation 一个 UUID/command；DHXY TURN-09R named tests另证整个
   multi-click fragment 只提交一次 global queue request。
6. **Cached plan consume**：保持“先从 state 取出并清空 pending，再校验 plan/base，再发 action”的顺序；空 plan/
   invalid base 返回 false；有 plan 的调用仍按基线消费一次、更新 counter/返回语义，不因 failed/uncertain 增加
   second probe、retry 或恢复已清 plan。
7. **Incense capture/algorithm**：status panel baseline ROI `(901,123,123,34)` 映射成 exact screen-absolute region；
   raw PNG/template/digit 计算留 Cloud。保留 `duration=59m`、memory trust=`50m`、refresh line=`20m`、failed-use
   retry=`60s`、success 后 `1000ms` 等待，以及 cached icon -> full probe fallback 顺序。
8. **Incense terminal**：`UNKNOWN`、wrong identity/frame、undecodable PNG 不得当作 buff absent/present，不用香、
   不重置 timer；现有 benign capture-unavailable 与 transport-uncertain 的不同分支必须各有断言，不能合并成新
   business truth。只有 Cloud 已决定补香才调用 TURN-14 `CloudBagUseIncensePort`；USED 才更新时间并返回 true，
   NOT_FOUND/NOT_EXECUTED 保留 false/60s retry 语义，STOP 直接 unwind。
9. **Caller-owned bag/session boundary**：普通 incense 只调用 TURN-14 typed Bag client；
   `ensureSheYaoXiangActiveInOpenMainBag` 继续使用 caller 传入 session，不二次 open/acquire，FiveRing caller 留
   TURN-36。Task/caller 文件在本 test/write set 外，只做 source-order复核，不加 source-guard test。
10. **Strict terminals and call count**：对 capture/execute 分别脚本化 COMPLETED、FAILED、STOPPED、UNCERTAIN；
    每次显式 client invocation 精确一个 UUID/command，失败后无第二 exchange/action/retry。strict result parser
    拒绝 duplicate/unknown/coerced/null/wrong-shape 数据；不能把 negative runner signal 升格成 business success。
11. **Old-path zero use in touched production**：`PlayerStateService` 不再引用
    `com.bot.dhxy.service.playerstate.CloudPlayerState*`、`LocalMacroKind.PLAYER_STATE_FIRST_AID`、
    `context.getGameClient()`、Cloud 机 `MouseInfo`、DHXY `GameClientTracker/InputSequences/InputProvider/WindowScopedTempPath`
    作为生产 runtime。物理旧文件是否删除不属于本卡。

### 4.7 可直接冻结的 TURN-23 next brief

```text
TURN-23 dispatch gate:
  - TURN-14 Repair #1 source/test-source review passed; owner released.
  - TURN-18 source/test-source review passed; owner released.
  - ADD startDependsOn TURN-09R source/test-source pass + owner release.
  - Parent must first choose exact pointer-over-ROI mouse-clear contract; worker may not infer it.
legacyCoverageKey:
  AutoCombatService -> PlayerStateService::probeAndConsumeHealthyFirstAidNoFocus
Production write set:
  C PlayerStateService.java
  C ClientIdentityService.java
  C Create remote/CloudPlayerStateFirstAidPort.java
  C Create remote/CloudPlayerStateIncenseStatusPort.java
Test write set:
  C Create service/PlayerStateTurnContractTest.java
Read-only dependencies:
  TURN-14 CloudBagUseIncensePort/CloudBagLocalServiceClient;
  TURN-18 exact metadata implementation;
  TURN-09R protocol/executor result;
  all Task/caller files, DHXY, old retained ports/macros.
Acceptance:
  apply Section 4.6 verbatim; no extra production model file, no old transport fallback,
  no Task/caller edit, no second metadata cache/type, no retry/session/owner/ledger/TTL.
Identity is integration-only until a later real production caller/Task factory exists; do not count syncAll.
No approved business difference; preserve 696a12b0 and docs/业务逻辑 ordering.
```

## 5. Parent decision list

`NEEDS_PARENT_DECISION` 的最小原因只有以下三项：

1. TURN-16：是否按第 3.6 节把 DHXY `GiveItemLocalOperationExecutor` 及其 existing test 加入卡写集，使
   open-dialog option click 到最终 Give click 真正落在一个 local-service exclusive action 内。
2. TURN-23：把 TURN-09R 明确加入 start dependency；等它父级 source/test-source 通过并释放 owner 后再派发。
3. TURN-23：选择 pointer-over-ROI 的等价表达。若选择“总是 move-away”，必须把新增输入明确记录为获准差异；
   若坚持条件式完全等价，就先扩 foundational metadata/executor 写集，不能让 TURN-23 Worker临时发明。

除这三项外：TURN-16 的 02R/13G/13H/13C source 前置、TURN-23 的 14/18 source 前置、TURN-23 与
20/24A/29 的 production/test 写集互斥性，以及两张卡的合法 `TurnGameClient` source 路径均已核清。

## 6. 读取证据索引

### DHXY 文档/计划

- `AGENTS.md:1-98,119-173,238-392`：基线门、no-local-test/compile 门、角色/流程、input 原子性、文档规则。
- `docs/DHXY_CONTEXT.md:7-25`：CR271 authority、主链、test family、四个永久本地 Service、`696a12b0`。
- `docs/ACTIVE_WORK.md:3-15,62-89,124-134,168-179,291-303,931-944`：TURN-14/18/13C/13H/13G/02R 与
  identity 零 caller 状态。
- 权威计划：`:338-360,478-568,856-918,932-1007,1022-1110,1225-1254,1265-1305,1414-1431`。
- 协议规格：`:54-113,313-334`。
- `docs/业务逻辑.md:343-470,1087-1190,1253-1300`：五环 dialog、五倍 prewalk/incense/maintenance、
  startup/修罗 fallback 基线。
- 前置/活动卡报告：
  - `TURN-02R-PROD.md:88-113`
  - `TURN-13G.md:358-376`
  - `TURN-13H.md:134-163`
  - `TURN-13C.md:187-213`
  - `TURN-14.md:297-310`
  - `TURN-18.md:74-103`
  - `TURN-09R.md:1-67`
  - `TURN-20.md:3-49,51-117`
  - `TURN-24A.md:3-52,54-89`
  - `TURN-29.md:3-71`
  - 旧 `TURN-16.md:1-88`（仅用于核对陈旧 blocker/path；权威第 16-18 节优先）。

### DHXY source / baseline

- `git show 696a12b0:DialogService.java:1350-1382` 与 Cloud current `DialogService.java:1503-1535`。
- `git show 696a12b0:PlayerStateService.java:210-478,506-676,699-849,1002-1100`。
- `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java:46-79`。
- `src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java:25-64`。
- `src/main/java/com/bot/dhxy/service/GiveItemService.java:28-109`。
- `src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java:18-111`。

### Cloud source

- `DialogService.java:75-98,205-234,1498-1538`。
- `FiveRingTaskV2.java:2058-2125,2389-2397`。
- `TurnGameClient.java:20-25,64-84,95-168,171-187`。
- `CloudBrainServer.java:49-65,68-111`；`CloudServiceHost.java:35-65`；
  `CloudServiceConfiguration.java:19-40,69-72`；`CloudTurnRoutes.java:43-75`。
- `CloudDialogDetectionPort.java:18-35,47-63`。
- `PlayerStateService.java:13-40,74-153,154-359,437-760,781-853,1024-1245,1488-1621`。
- `ClientIdentityService.java:15-94`。
- 旧 `service/playerstate/CloudPlayerStateFirstAidPort.java:18-115`；
  旧 `CloudPlayerStateIncenseStatusPort.java:13-107`。
- `CloudBagUseIncensePort.java:15-72`；`CloudBagLocalServiceClient.java:30-175`。
- Caller 扫描：`AutoBattleTask.java:98-163`、`WubeiTask.java:332-356,1768,3878-3904,4133`、
  `FiveRingTaskV2.java:228-241,743-765,1109,1830`、`XiuluoTaskV2.java:303-354,594,1110,1561,1933,
  2302,2845-2894`、`AutoCombatService.java:345-472,520-577`、`TeamReturnService.java:67`、
  `NavigationService.java:821`。
- Protocol source：`TurnWindowMetadata.java:3-22`、`TurnInputAction.java:3-13`、`TurnStep.java:3-11`、
  `TurnLocalOperation.java:3-12`、`TurnLocalServiceCall.java:3-8`、`TurnGiveItemOperationArguments.java:3-5`。

<!-- TRUE_EOF: CR271 TURN-16-23 readiness helper | 2026-07-15T22:13:23.722-04:00 | NEEDS_PARENT_DECISION -->

## 7. Immediate readiness addendum - 2026-07-15T22:20:34.888-04:00

本节按用户最新指令追加；它覆盖本报告第 3.6-3.7 节中“由 TURN-16 自身扩 DHXY 写集”的调度建议。父级已经把
该 Foundation 修复独立冻结为 TURN-10CR，所以 TURN-16 不再拥有 DHXY `GiveItemService`/adapter/test 写集。
本 helper 仍只给非绑定预检结论：`NEEDS_PARENT_DECISION`。

### 7.1 TURN-16 新增 TURN-10CR 前置已核实

最新权威计划已经形成一致的三处证据：

1. `:381-402`：TURN-10C 只完成 adapter route；TURN-10CR 独占 whole open-dialog mechanics repair，严格闭合
   `match give entry -> click 150ms -> wait 800ms -> existing direct give flow`，而且明确 TURN-16 只能消费这个
   local operation。
2. `:963-964,984`：注册表把 TURN-10CR 列为当前 READY Foundation repair；TURN-16 的 source dependencies
   已更新为 `02R+13C+10CR`。
3. `:1290-1293`：动态滚动规则再次明确 TURN-16 必须先等 TURN-10CR，不允许用后续 wiring 替代缺失 mechanics。

TURN-10CR 固定报告 `2026-07-15-turn-card-TURN-10CR.md:1-55` 的 exact write set 为：

- DHXY `src/main/java/com/bot/dhxy/service/GiveItemService.java`
- DHXY `src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
- DHXY `src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
- Create DHXY `src/test/java/com/bot/dhxy/service/GiveItemServiceOpenDialogContractTest.java`
- TURN-10CR 固定报告

截至本次读取，TURN-10CR 固定报告 true EOF 只有父级 frozen brief，没有 implementation claim 或 source delivery
追加。因此当前顺序应冻结为：

```text
TURN-10CR claim/implement/source+test-source handoff
  -> parent source/test-source pass and owner release
  -> TURN-16 becomes the next Dialog-lane implementation card
```

TURN-16 到时只写权威计划 `:1113-1115,1455` 的 Cloud caller/client/test：

- `src/main/java/com/bot/dhxy/service/DialogService.java`
- Create `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudGiveItemLocalServiceClient.java`
- Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogGiveItemTurnContractTest.java`

### 7.2 TURN-23 最新状态

最新注册表 `:991` 已正式把 TURN-23 dependencies 修正为 `14+18+09R`，与本报告第 4.1 节识别的 first-aid
multi-click 原子前置一致。TURN-09R 固定报告 `:69-139` 已于 `22:16:42-04:00` 追加 worker source/test-source
delivery，但父级 source/test-source review 尚未追加；其报告还记录 `TurnCoreProtocolGoldenJsonTest` 的旧十枚 enum/
spec 断言需要由对应 owner/父级同步。故 TURN-23 现在仍不能立即 claim。其下一 gate 是父级复读 TURN-09R 最新
production/test source、处理该 test-family 关联点并释放 owner；随后再冻结本报告第 4.1 节的 pointer-over-ROI
合同，不让 TURN-23 Worker自行推断。

### 7.3 与五条指定写集的互斥核对

| Card | 当前 exact production/test 写集 | 与 TURN-10CR |
|---|---|---|
| TURN-20 Repair #1 | Cloud `AutoCombatPanelService.java`、`LocalOcrClient.java`、`AutoCombatPanelTurnContractTest.java`；报告 `:153-171` | 无重叠 |
| TURN-24A Repair #1 | Cloud `BattleRadarService.java`、`BattleRadarTurnContractTest.java`；repair source 已交，报告 `:116-154` | 无重叠 |
| TURN-29 | Cloud 十个 TaskTracker core/model 文件、`TaskTrackerPanelTurnContractTest.java`；报告 `:49-71` | 无重叠 |
| TURN-09R | 双仓 `TurnInputAction`/validator，DHXY mapper/executors 与对应 protocol/executor tests；报告 `:45-139` | 无重叠 |
| TURN-10CR | DHXY `GiveItemService`、Give adapter、两个 Give tests | 本卡自身 |

TURN-10CR 与上述四条既有工作线 production/test 文件完全互斥；它是当前唯一可立即 claim 的新
implementation card。

### 7.4 除 TURN-10CR 外的完整 READY 扫描

结论：`NO_ADDITIONAL_READY_CARD`。

直接证据：最新权威注册表中只有 TURN-09R 与 TURN-10CR 两行标为 READY（`:959,964`）。其中 TURN-09R 已由
原 Worker交 source/test source，正在等待父级 gate，不是可再领取的新实现；按用户问题排除 TURN-10CR 后，
READY 集合为空。

其余最近候选均有真实 dependency 尚未闭合：

| 候选 | 当前不能立即实施的原因 |
|---|---|
| TURN-16 | 等 TURN-10CR source/test-source pass 与 owner release |
| TURN-19 / TURN-21 / TURN-23 | 等 TURN-09R 父级 source/test-source pass 与 owner release |
| TURN-T04 addendum | 等 TURN-10CR 的新增 whole-macro production/tests |
| TURN-22 | 等 TURN-23 |
| TURN-25 -> TURN-26 | 先后等 TURN-16、TURN-25 |
| TURN-28 -> TURN-27 | 等 PlayerState/BattleRadar/Dialog 链闭合后再按依赖顺序实施 |
| TURN-30 / TURN-31 / TURN-32 | 等 TURN-29 source/test-source gate；不能在 TaskTracker core 仍在途时提前写三个 Task caller |
| TURN-33 及 34A/B/C | 至少等 TURN-26，且后续还分别等 19/20/21/22/23/24 |
| TURN-35 以后 | 依赖上述 Service/caller 汇合，不属于当前波次 |

因此现在不应再伪造一个“可实施”卡来填并发槽。可直接冻结的调度 brief 是：

```text
Immediate new implementation: TURN-10CR only.
No additional READY card is dependency-complete after excluding TURN-10CR.
Next unlocks:
  - TURN-10CR source gate -> TURN-16;
  - TURN-09R source gate -> TURN-19 / TURN-21 / TURN-23 readiness freeze;
  - TURN-29 source gate -> TURN-30 / TURN-31 / TURN-32 caller cards.
Do not claim downstream cards before the named predecessor owner is released.
```

本 addendum 未运行任何测试/构建/运行路径，未执行 Git mutation，未写其它文件。

<!-- TRUE_EOF: CR271 TURN-16-23 immediate readiness addendum | 2026-07-15T22:20:34.888-04:00 | NEEDS_PARENT_DECISION | NO_ADDITIONAL_READY_CARD -->
