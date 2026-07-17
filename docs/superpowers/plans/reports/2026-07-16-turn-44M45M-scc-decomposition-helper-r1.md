# PRECHECK

- 任务范围：`TURN-44M45M Cloud 17-file SCC compile-safe cohort decomposition risk audit`。本报告只对当前
  Cloud production source graph 给出预检证据，不生成最终 delete manifest，不写 byte size/SHA-256，不作最终
  `KEEP/REWIRE/DELETE` 分类，不修改任何 Java/test/权威计划/ACTIVE_WORK/CR271/矩阵/dashboard。
- 已重新完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第
  14-19 节、上一轮
  `docs/superpowers/plans/reports/2026-07-16-turn-44M45M-readiness-preflight-helper.md`，并重新扫描当前 Cloud
  `src/main/java/**/*.java` 的生产引用。
- 当前只读 Git 快照：DHXY=`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`，
  tracked dirty `44`、untracked `593`；Cloud=`navigation-migration` /
  `3b988caa010254973e03342272e6d1d6a9685b01`，tracked dirty `9`、untracked `541`。全部 dirty/untracked
  都是受保护输入；本次没有 Git mutation。
- Cloud 目标状态仍不适合冻结 manifest：`CloudBrainServer.java` 是 tracked modified；旧 route、endpoint、17 文件
  SCC 及其多数依赖目前是 untracked。并行 Java writer 仍在活动，本 helper 不读取“文件存在”作为可删除授权。
- 扫描方法：对 Cloud `src/main/java` 做 file/type-level graph；排除 block/line comment 与 string/char literal 后，以
  production type reference 建边。逐文件证据中的 `IN[SCC]`/`OUT[SCC]` 是 17 文件内部边，`IN[EXT]` 是组件外
  production caller，`OUT[EXT]` 是组件指向项目内 context/run/DTO/transport 的边。Spring 动态配置与最终 Java
  编译仍须由未来 source guard/compile gate 复核。
- 本次没有运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input。

## 证据

**1. 当前 compile-safe 顺序，不把命名分组冒充源码分组**

| 阶段 | 17 文件处理数 | 当前源码要求 | 阶段结束后的未来 gate |
|---|---:|---|---|
| Predecessor rewire | `0/17` 删除 | 先完成 whole Task caller、38A、38B1/B4、38M/38C 与 39；移除 active context/Service/host 入边。40C 先稳定 `CloudBrainServer`/host/runtime wiring。 | 各 predecessor 自己的 named-test/compile；本 helper不执行。 |
| 45A route disconnect | `0/17` 删除 | 只改 `CloudBrainServer.java`，删除 `RemoteTaskRunRoutes.java`、`RemoteTaskRunEndpoint.java`。这会移除 SCC 对 ledger/assembly/final/broker 的 route 入边，但不改变任何 SCC 内边。 | `OldRemoteRouteRemovalGuardTest`，随后 Cloud `mvn -q clean compile`。 |
| 45A 后重算 SCC | `0/17` 删除 | 45A 后当前 17 文件仍为同一个 SCC。必须基于 39/40C final source 重跑完整图，不能沿用本报告的 hash/行号。 | 任一 internal edge/外部 caller 变化都先刷新 cohort 证据。 |
| 44A authority/context cohort | 当前图要求 `17/17` 同 cohort | 若源码边保持当前形状，17 文件必须一次处理；另将仍引用组件的 `CloudTaskTurnAuthority.java`、`LeaderPrecheckAction.java` 同卡删除或在此前重接。不能把 broker/final 留给 45B。 | `OldAuthorityRemovalGuardTest`，随后 Cloud `mvn -q clean compile`。 |
| 45B broker/routes/task-run DTO residue | 当前图对这 17 文件为 `0/17` | 45A 已删除 route source；44A 已处理 broker/final/ledger SCC。45B 只能接收 44A 后逐文件证明零引用的 run/command/final/DTO/transport residual。 | `OldRemoteWireRemovalGuardTest`，随后 Cloud `mvn -q clean compile`。 |

`RemoteGameCommandBroker` 不能按名称留到 45B：
`CloudTaskRunActionLedger.java:676-803 -> RemoteGameCommandBroker`，而
`RemoteGameCommandBroker.java:385-637 -> CloudTaskRunActionLedger.CompactionPlan`。45A 只删外部 route caller，
TURN-39 的精确写集也不包含 ledger/broker，所以这条双向边不会由既定 predecessor 自动消失。除非未来另有用户确认
且先写入权威计划的显式 rewire card，否则当前唯一真实中间编译点是 ledger 与 broker 同在 44A cohort。

**2. 17 文件逐项 inbound/outbound production refs**

以下路径前缀均为 `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/`。

1. `CloudGameClient.java`
   - `IN[SCC]`：`CloudTaskServiceExecutionContext:27,64,103,184`。
   - `IN[EXT]`：`com/bot/dhxy/input/InputSequences.java:5,39,46`；
     `com/bot/dhxy/runner/context/TaskExecutionContext.java:10,319`。
   - `OUT[SCC]`：`CloudTaskRetainedActionState:16,21,164,167`；
     `CloudTaskServicePort:17,22,45,50,75,81,107,114,145,153`。
   - `OUT[EXT]`：`CloudTaskRunExecutionContext:15,20`，以及 capture/window-fact/input/local-macro DTO
     (`CaptureOutcome/Region/Request`、`CoordinateSpace`、`ExecutionState`、`InputActionDto`、
     `InputBundleOutcome`、`LocalMacro*`、`WindowFact*`)。
   - Active caller/前置：`InputSequences` 被 Wubei/FiveRing/Xiuluo、Dialog/Navigation/NpcClick 使用；
     `TaskExecutionContext` 被全部 Task 生命周期使用。必须先完成 35/36/37 -> 38A -> 39；当前不能删。

2. `CloudSummonSkillWholePassCapability.java`
   - `IN[SCC]`：`CloudTaskRunCommandExecutor:304,309`；`CloudTaskServicePort:23,42,51`；
     `RemoteGameClientPort:126`。
   - `IN[EXT]`：无组件外 production symbol caller。
   - `OUT[SCC]`：`CloudTaskExclusiveInteractionAuthority:20,21`；`CloudTaskRetainedActionState:22`。
   - `OUT[EXT]`：无 file-stem project type；其行为仍由 SCC authority/state 提供。
   - Active caller/前置：通过 old ServicePort/command executor 间接处于 active summon maintenance 图；TURN-33
     正在写本文件，后续还需 34B、whole Task、38A/39 清 caller。当前不能删，也不能与 TURN-33 并发取 hash。

3. `CloudTaskExclusiveInteractionAuthority.java`
   - `IN[SCC]`：`CloudSummonSkillWholePassCapability:20-21`、`CloudTaskRunAuthorityAssembly:45,71,201,267,
     286,403,414,459`、`CloudTaskRunCurrentContextSlot:248-263`、
     `CloudTaskServiceExecutionContext:25-224`、`CloudTaskServicePort:21-31`。
   - `IN[EXT]`：无组件外 production symbol caller。
   - `OUT[SCC]`：`CloudTaskExclusiveInteractionState`、`CloudTaskRetainedActionState`、
     `CloudTaskRunActionLedger`、`CloudTaskRunCurrentContextSlot`、`CloudTaskRunExecutionGate`、
     `CloudTaskServicePort`、`RemoteFinalConsumptionCoordinator`、`RemoteGameClientPort`、
     `TaskTransactionAction`（主要引用区间 `25-1132`）。
   - `OUT[EXT]`：`CloudPausedReadOnlyObservationContext`、`CloudTaskRunExecutionContext`、exclusive control/session
     DTO、`ExecutionState`、final-consumed receipt、`RemoteOperation/Outcome/ProtocolValidation` 及
     `remote/run` authorization/binding/scope/window。
   - Active caller/前置：TURN-33 正在写本文件；须等 summon/Task caller、38A/39 与 45A route sever。当前不能删。

4. `CloudTaskExclusiveInteractionState.java`
   - `IN[SCC]`：仅 `CloudTaskExclusiveInteractionAuthority:79-1057`。
   - `IN[EXT]`：无。
   - `OUT[SCC]`：`CloudTaskRetainedActionState:473`。
   - `OUT[EXT]`：`RemoteProtocolValidation:47-485`、`RemoteTaskRunScope:3,468`、
     `RemoteTaskRunWindow:4,471`。
   - Active caller/前置：无直接 Task caller，但被 active authority 构造；必须随 authority cohort，当前不能单删。

5. `CloudTaskRetainedActionState.java`
   - `IN[SCC]`：`CloudGameClient`、summon capability、exclusive authority/state、ledger、assembly、
     service context/port 与 `TaskTransactionAction`（引用区间 `16-681`）。
   - `IN[EXT]`：`LeaderPrecheckAction.java:16,17,21,22,29,33`。
   - `OUT[SCC]`：`CloudTaskRunActionLedger`、`CloudTaskRunExecutionGate`、
     `CloudTaskRunRetainedLifecycleActivationAdapter`、`CloudTaskServicePort`、`TaskTransactionAction`
     （引用区间 `27-645`）。
   - `OUT[EXT]`：paused/run context、exclusive request、navigation workflow、`RemoteOperation`、
     `RemoteProtocolValidation`、`RemoteSemanticAddress` 与 run authorization/scope/window。
   - Active caller/前置：38A 清 retained-authority caller；`LeaderPrecheckAction.java` 若仍存在必须加入 44A
     compile closure。当前不能删。

6. `CloudTaskRunActionLedger.java`
   - `IN[SCC]`：exclusive authority、retained state、assembly、command executor、execution gate、service context/port、
     final coordinator、remote client port、broker（引用区间 `26-1590`）。
   - `IN[EXT]`：`RemoteTaskRunRoutes.java:52`。
   - `OUT[SCC]`：`CloudTaskRetainedActionState:78,86,91,434,435,1324,1393`；
     `RemoteGameCommandBroker:676,677,682,683,697,719,720,733,734,799,800,802,803`。
   - `OUT[EXT]`：paused/run context、common/execution outcome、final ack/receipt、remote
     operation/outcome/request/address/protocol、request context 及 run scope/window。
   - Active caller/前置：45A 先移除 route caller；broker 反向引用 ledger nested type，因此两者必须同 cohort。
     当前不能删，也不能先删 ledger 后留 broker。

7. `CloudTaskRunAuthorityAssembly.java`
   - `IN[SCC]`：`CloudTaskRunCurrentContextSlot:32-511`；
     `CloudTaskRunRetainedLifecycleActivationAdapter:30-293`。
   - `IN[EXT]`：`RemoteTaskRunRoutes.java:55`。
   - `OUT[SCC]`：exclusive authority、retained state、ledger、command executor、current slot、execution gate、
     retained-lifecycle adapter、service context、final coordinator、remote client port、broker
     (`42-459`)。
   - `OUT[EXT]`：`CloudGameContextStateOwner`、`CloudLeftTopStatusSwitchState`、paused/run context、service metadata、
     `GameContext`、run binding/coordinator/scope 与 `TaskExecutionContext`。
   - Active caller/前置：45A 移除唯一组件外构造 caller；38M/38C 先裁定三个 authority-bound state，39 清 old
     context/facade。当前不能删。

8. `CloudTaskRunCommandExecutor.java`
   - `IN[SCC]`：`CloudTaskRunAuthorityAssembly:44,70`。
   - `IN[EXT]`：无。
   - `OUT[SCC]`：summon capability、ledger、execution gate、remote client port、broker
     (`26-393`)。
   - `OUT[EXT]`：capture/input/local-macro DTO、run context/authorization、exclusive DTO、remote
     operation/protocol/request context、summon request/outcome、window-fact request/outcome。
   - Active caller/前置：由 route-created assembly 构造并承载 old command path；等 45A、Task migration、38A/39。
     当前不能单删。

9. `CloudTaskRunCurrentContextSlot.java`
   - `IN[SCC]`：exclusive authority、assembly、retained-lifecycle adapter（引用区间 `48-471`）。
   - `IN[EXT]`：`CloudTaskTurnAuthority.java:58,1000,1004`。
   - `OUT[SCC]`：`CloudTaskExclusiveInteractionAuthority:248,249,263`；
     `CloudTaskRunAuthorityAssembly:32,43,60,61,128,152,197,199,209,364,403,431,495,500,511`。
   - `OUT[EXT]`：run context/binding/coordinator/scope/status/window，以及 Task checkpoint/context/stop types。
   - Active caller/前置：38A 清 Task retained authority；`CloudTaskTurnAuthority.java` 若仍保留该边，必须加入
     44A compile closure。当前不能删。

10. `CloudTaskRunExecutionGate.java`
   - `IN[SCC]`：exclusive authority、retained state、assembly、command executor、service context
     (`22-121`)。
   - `IN[EXT]`：无。
   - `OUT[SCC]`：`CloudTaskRunActionLedger:40,54,146,174,185,188,222,233,263,280,301,305,368,
     394,396,413,417`。
   - `OUT[EXT]`：capture/input/local/exclusive DTO、paused/run context、remote operation/protocol/run types、
     request context、window facts 与 `TaskCheckpointDecision`。
   - Active caller/前置：经 assembly/service context active；须等 38A/39 与 45A，当前不能单删。

11. `CloudTaskRunRetainedLifecycleActivationAdapter.java`
   - `IN[SCC]`：`CloudTaskRetainedActionState:600,603,637,645`；
     `CloudTaskRunAuthorityAssembly:50,75,136`。
   - `IN[EXT]`：无。
   - `OUT[SCC]`：`CloudTaskRunAuthorityAssembly:30,31,37,38,110,293`；
     `CloudTaskRunCurrentContextSlot:152,470,471`。
   - `OUT[EXT]`：`CloudGameContextStateOwner`、paused context、service metadata、protocol validation 与
     run binding/scope/status/window。
   - Active caller/前置：38A 去除 retained lifecycle，38M/38C 处理 state owner，39 清 metadata/facade；当前不能删。

12. `CloudTaskServiceExecutionContext.java`
   - `IN[SCC]`：`CloudTaskRunAuthorityAssembly:204,206,288,289,398,409,439`。
   - `IN[EXT]`：`TaskExecutionContext.java:11,35,51,442`；`CloudArtifactStore.java:3,30,37,44`；
     `ScopedPngArtifactStore.java:3,62,153,174,197,209`。
   - `OUT[SCC]`：game client、exclusive authority、retained state、ledger、execution gate、service port、
     final coordinator、remote client port (`22-224`)。
   - `OUT[EXT]`：left-top state、run context、service metadata、run authorization/scope、checkpoint decision。
   - Active caller/前置：全部 Task 经 `TaskExecutionContext`；artifact host/config 仍消费该类型。必须完成
     38A、38B4、39；当前不能删。

13. `CloudTaskServicePort.java`
   - `IN[SCC]`：game client、exclusive authority、retained state、service context、final coordinator
     (`17-566`)。
   - `IN[EXT]`：`TaskExecutionContext.java:13,331`；`service/bag/CloudBagStateOwner.java:4`。
   - `OUT[SCC]`：summon capability、exclusive authority、retained state、ledger、final coordinator、remote client port
     (`18-324`)。
   - `OUT[EXT]`：capture/input/local macro/window fact DTO、run context、remote operation/outcome。
   - Active caller/前置：Task context 与 bag state 都是 active；必须完成 38A、38B1、39。当前不能删。

14. `RemoteFinalConsumptionCoordinator.java`
   - `IN[SCC]`：exclusive authority、assembly、service context、service port (`20-90`)。
   - `IN[EXT]`：`RemoteTaskRunRoutes.java:53,54,127,130`。
   - `OUT[SCC]`：`CloudTaskRunActionLedger:12,17,24,28,33,71,88,133,142,162`；
     `CloudTaskServicePort:26,31,49,144,145,164,187`；`RemoteGameCommandBroker:13,18,34,72,89,134`。
   - `OUT[EXT]`：run context、common/execution/outcome code、final ack/receipt/receipt-ack、remote
     operation/outcome/protocol。
   - Active caller/前置：45A 移除 route ingress，但 coordinator 仍双向嵌在 authority/service/ledger/broker 图。
     必须随 44A SCC，不能留到 45B。

15. `RemoteGameClientPort.java`
   - `IN[SCC]`：exclusive authority、assembly、command executor、service context、service port (`19-126`)。
   - `IN[EXT]`：无。
   - `OUT[SCC]`：`CloudSummonSkillWholePassCapability:126`；
     `CloudTaskRunActionLedger:27,46,55,75,84,101,110,129`。
   - `OUT[EXT]`：capture/input/local macro/summon/window-fact DTO 与 run context。
   - Active caller/前置：old facade 的内部接口；须等 active Service/Task 完成 turn rewire 和 39。当前不能单删。

16. `RemoteGameCommandBroker.java`
   - `IN[SCC]`：ledger、assembly、command executor、final coordinator (`30-803`)。
   - `IN[EXT]`：`RemoteTaskRunRoutes.java:51,87,89,98,110,112`。
   - `OUT[SCC]`：`CloudTaskRunActionLedger.CompactionPlan:385,388,419,637`。
   - `OUT[EXT]`：command poll/outcome envelopes/acks、final receipt/ack、capture/input/local/summon DTO、
     run coordinator/authorization/scope/status/window、remote request/outcome/address/protocol/context 等。
   - Active caller/前置：45A 移除 route poll/outcome caller，但 broker -> ledger 仍存在；必须与 ledger 同在 44A。
     当前不能放入 45B residual cohort。

17. `TaskTransactionAction.java`
   - `IN[SCC]`：exclusive authority、retained state (`97-1102`)。
   - `IN[EXT]`：无。
   - `OUT[SCC]`：`CloudTaskRetainedActionState:11,12,13,17,18,19,27,31,35`。
   - `OUT[EXT]`：`RemoteProtocolValidation:24`。
   - Active caller/前置：虽无直接外部 caller，仍是 retained-action cycle 的类型边；必须随 44A，当前不能单删。

**3. active Task/Service caller 与必须先 rewire 的 predecessor**

| 直接组件外入边 | 实际 active 上游 | 必须先完成的 predecessor | 当前保护结论 |
|---|---|---|---|
| `InputSequences -> CloudGameClient` | `WubeiTask:278`、`FiveRingTaskV2:217`、`XiuluoTaskV2:279`；`DialogService:82`、`NavigationService:178`、`NpcClickService:103` | whole Task 35/36/37，随后 38A/39 | `CloudGameClient` 与它依赖的 SCC 当前不能删。 |
| `TaskExecutionContext -> CloudGameClient/ServiceExecutionContext/ServicePort` | AutoBattle、Wubei、FiveRing、Xiuluo、`GameTask`/`BaseTaskTemplate`/checkpoint 全生命周期 | 34C、35/36/37 -> 38A -> 39 | 三个 old context/facade 类型当前不能删。 |
| `CloudBagStateOwner -> CloudTaskServicePort` | `BagWorkflowState` 持有 state owner/handle | 38B1 -> 39 | ServicePort 当前不能删。 |
| `CloudArtifactStore`/`ScopedPngArtifactStore -> CloudTaskServiceExecutionContext` | `CloudServiceConfiguration` 构造 artifact store；40C 将激活 host | 38B4 -> 39 -> 40C | 不能依据当前 dormant host 判定 context 可删。 |
| `CloudTaskTurnAuthority -> CloudTaskRunCurrentContextSlot` | 当前无其它 production caller，但源码本身仍要编译 | 38A 后若未重接，则与 44A 同 cohort | 不能把它留在删除 current slot 之后。 |
| `LeaderPrecheckAction -> CloudTaskRetainedActionState` | 当前无其它 production caller，但源码本身仍要编译 | 38A 后若未重接，则与 44A 同 cohort | 不能把它留在删除 retained state 之后。 |
| `RemoteTaskRunRoutes -> ledger/assembly/final/broker` | `CloudBrainServer:12,51,58,86-92` 注册 old poll/outcome/task-run | 40C final source -> 44M45M freeze -> 45A | route 外部入边必须先断。 |

active Task/Service 仍使用的 old port/model 也必须保护，不属于当前 45B 可删集合：

- `CloudBagUseIncensePort`：`PlayerStateService`、`CloudBagLocalServiceClient`；
- `CloudCommonBoxPort`/assembly/result：`CommonBoxService`、host configuration；
- 五个 `CloudDialog*Port` 及 command/result：`DialogService`；
- `CloudLeftTopStatusPort`/result：`LeftTopStatusSwitchService`、left-top assembly；
- `CloudUiCleanerPort`/command/result：`AutoCombatService`、`NavigationService`、`SummonSkillService`、
  `CloudUiCleanerLocalServiceClient`；
- `CloudTeamReturnPort`/assembly/result：`TeamReturnService`、host configuration。

这些类型即使位于 `remote/`，也必须等各自 caller card 和 39 后逐项证明零引用。永久本地
Bag/UI/Give/Quest Service 的模型与 NPC reference/shadow 代码继续受权威计划保护。

**4. 45A route disconnect 的编译证据与 STOP-WORK 条件**

- 当前精确生产写边仍是：Modify `CloudBrainServer.java`；Delete `RemoteTaskRunRoutes.java`；Delete
  `api/RemoteTaskRunEndpoint.java`。17 文件不在 45A 删除范围。
- 45A 会移除四类 SCC 外入边：route -> ledger、route -> assembly、route -> final coordinator、route -> broker。
  它不会移除 ledger <-> broker 或其它 SCC 内边；45A 后旧 SCC 可以 unreachable 但仍应独立编译。
- 45A 必须保留 `CloudTurnRoutes`、`CloudTurnHttpHandler.PATH=/api/v1/client/turn`、
  `CloudTemplateHttpHandler.PATH_PREFIX=/api/v1/templates/` 以及 40C 最终 host/runtime close order。
- Future compile gate：先运行点名 `OldRemoteRouteRemovalGuardTest`，再在 Cloud 仓执行
  `mvn -q clean compile`。本 helper 不执行。
- 立即 STOP-WORK：40C 尚未稳定或 `CloudBrainServer` hash/reference 漂移；发现 route/endpoint 还有 Server 外
  production caller；补丁需要写第四个 production 文件；new turn/template context 被移除；route source guard 只能靠
  wildcard/目录不存在证明；任一受保护 dirty/untracked 出现无法归属的变化。

**5. 44A authority/context SCC 的编译证据与 STOP-WORK 条件**

- 45A 完成后必须重新计算全 Cloud source graph。若 17 文件仍保持当前边，44A 的最小 compile-safe cohort 是
  `17/17`，不是“15 authority + 2 broker 留后”。
- 最小外部 consumer closure 还包括 `CloudTaskTurnAuthority.java`、`LeaderPrecheckAction.java`：若它们的入边未由
  predecessor 消失，就必须同卡处理。它们不是本报告的最终删除行，只是不能留作引用已删除 symbol 的编译证据。
- 38M 五个 state、`CloudTaskRunExecutionContext`、`CloudTaskServiceMetadata` 是组件的 outward dependencies，
  并非当前 17 文件 SCC 的反向 consumer；它们可以在 44A 后继续编译，但只能按 38M/38C/39 与最终引用决定是否留到
  45B。不得因“被 SCC 引用”反推它们必须随 44A 删除。
- Future compile gate：先运行点名 `OldAuthorityRemovalGuardTest`，逐 symbol 证明全部删除行零生产引用，再在 Cloud
  仓执行 `mvn -q clean compile`。本 helper 不执行。
- 立即 STOP-WORK：任何 `InputSequences`/`TaskExecutionContext`/bag/artifact/route 外部入边仍存在；尝试把
  `RemoteGameCommandBroker` 或 `RemoteFinalConsumptionCoordinator` 延后到 45B；SCC 结果与冻结 manifest 不一致；
  `CloudTaskTurnAuthority`/`LeaderPrecheckAction` 被遗留；38A/B/39 未完成；需要通过修改 business Task/Service 才能
  让 44A 编译；hash 漂移或新增 production caller。

**6. 45B DTO/transport residue 的编译证据与 STOP-WORK 条件**

- 当前 17 文件中没有一个属于 compile-safe 45B residual。若 44A 后仍存在其中任一文件，必须先解释它为何不再引用
  已删除 SCC symbol，并重新计算 SCC；不能按名称直接搬入 45B。
- 45B 只能盘点 44A 后的 outward dependencies，例如：
  `remote/run` 12 文件、`RemoteTaskRunAction*`/error/receipt、`RemoteCommand*`、`RemoteFinalConsumed*`、
  `RemoteClientScope`、`RemoteOperation/Outcome/Request`、`RequestContext`、`RemoteSemanticAddress`、
  `StopRef`、`WindowBindingRef` 等。每个文件仍需 future manifest 独立行、独立生产引用与 hash。
- 当前明确不能删的 residual anchors：
  - `RemoteProtocolValidation.java` 仍被大量 active port/result/macro 类使用；
  - `RemoteTaskRunAuthorization/Binding/Scope/Status/Window` 仍被 Task context、bag/return/startup/artifact 与
    38M state 类使用；
  - capture/input/local-macro/window-fact DTO 仍被 active Service facade 和 turn adapter 使用；
  - 任何 Cloud-owned business result/model、permanent-local Service model、NPC reference/shadow 仍有生产 caller。
- 45A 已删除 route source，因此 45B 的“routes residue”只能指 route-related wire DTO 的零引用残余，不能再次列
  `RemoteTaskRunRoutes.java` 或 `RemoteTaskRunEndpoint.java`。
- Future compile gate：先运行点名 `OldRemoteWireRemovalGuardTest`，再在 Cloud 仓执行
  `mvn -q clean compile`。本 helper 不执行。
- 立即 STOP-WORK：任何候选仍有 production ref；44A compile 尚未成功；broker/final/ledger 仍跨卡互引；使用
  `remote/**`、`Remote*` 或目录整体删除；source guard 未逐项列 symbol/path；删除需要改 active Task/Service；
  final manifest hash 与工作树不一致。

**7. 互斥与保护证据**

- TURN-33 当前写 `CloudSummonSkillWholePassCapability.java` 与
  `CloudTaskExclusiveInteractionAuthority.java`；44M45M 只能读，不能取 hash、改写或删除。
- TURN-39 将写 `CloudGameClient.java`、`CloudTaskServicePort.java`、
  `CloudTaskServiceExecutionContext.java`、`CloudTaskServiceMetadata.java` 及新 turn client/provider；本 SCC 报告只能
  作为 39 前风险图，future manifest 必须后于 39 重算。
- TURN-40C 与 45A 同写 `CloudBrainServer.java`，严格串行；45A 不能基于当前 dirty Server 预制 patch。
- 45A、44A、45B 在同一 Cloud 仓只允许一条 Java 删除线，严格 `45A -> compile -> 44A -> compile -> 45B ->
  compile`。任何把 17 文件拆给并发 writer 的方案都没有当前 source graph 支持。
- 本 helper 唯一写路径是
  `docs/superpowers/plans/reports/2026-07-16-turn-44M45M-scc-decomposition-helper-r1.md`；没有写上一份报告、
  Java/test、主计划、ACTIVE_WORK、CR271、矩阵或 dashboard。

PRECHECK_COMPLETE true EOF
