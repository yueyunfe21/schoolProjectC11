# PRECHECK

**0. 角色、范围与只读快照**

- 本文是 CR271 Internal 非绑定 helper 对 `TURN-44A post-45A old facade/authority/context/final-consumption
  SCC deletion` 的 R1 只读预检。唯一写入是本文；本文不是最终 delete manifest，不给任何生产文件写
  `KEEP/REWIRE/DELETE`，不改变卡片状态，也不产生 owner/claim。
- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第
  14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、TURN-39 readiness、两份 TURN-44M45M 报告及最新
  TURN-45A 报告，并重扫 Cloud 全部 `src/main/java` / `src/test/java` 对目标 symbol 的当前引用。
- 快照时间 `2026-07-16T06:59:39-04:00`。DHXY=`thin-client-design` /
  `0114604e1ff5f15491d2910959c45252e893d04f`，tracked dirty `44`、untracked `601`；Cloud=
  `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`，tracked dirty `9`、untracked
  `541`。两仓全部 dirty/untracked 都是受保护输入；17 个目标文件和下文主要边界文件当前均为 Cloud
  untracked source，不能把 HEAD 或文件存在性当作删除授权。
- `TURN-45A` 当前只有 PRECHECK 报告，尚不是已落地源码事实：当前 `CloudBrainServer.java:12,51,58,86`
  仍引用/构造 `RemoteTaskRunRoutes`，`RemoteTaskRunRoutes.java:10,51-63,87-130` 仍构造 old
  coordinator/ledger/final/broker，并注册 `api/RemoteTaskRunEndpoint`。因此本文的“post-45A”图是明确的条件投影：
  从当前图中减去未来 45A 的 server registration、`remote/RemoteTaskRunRoutes.java` 与
  `api/RemoteTaskRunEndpoint.java`，不是声称这些源码已经消失。
- 当前新 ingress 证据仍在：`CloudBrainServer.java:96-107` 构造 `CloudTurnRoutes`，并注册
  `CloudTurnHttpHandler.PATH` 与 `CloudTemplateHttpHandler.PATH_PREFIX`。44A 不得触碰或替代这条 HTTPS turn
  ingress。
- 本 helper 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或
  input；未执行任何 Git mutation，未修改 Java/test/计划/CR/ACTIVE_WORK/矩阵/dashboard。

**1. 图计算口径与 45A 的精确影响**

- production 编译边：去除 block/line comment、string/char literal 后，按 Java type token 建边；import、字段、
  参数、返回值、构造和 nested type 都算结构边。
- 文档/字符串边单独保留，因为它们不一定阻止 `clean compile`，但可能让 source guard、反射测试或未来完整测试族
  仍要求旧类型存在。
- 对下列 17 个文件的 stripped graph 做逐点可达检查，每个起点都可达 `17/17`。45A 只移除 route 对
  `CloudTaskRunActionLedger`、`CloudTaskRunAuthorityAssembly`、`RemoteFinalConsumptionCoordinator`、
  `RemoteGameCommandBroker` 的外部入边；它不移除任何 17 文件内部边，所以 post-45A 投影仍是同一个 SCC。
- 45A 与 44A 之间的唯一合法静态中间形状是“old route 已断、old SCC 仍完整存在”；不能在 45A 顺手删 SCC
  文件，也不能在 44A 之前按名称拆 broker/final。

**2. post-45A 最小 17-file SCC：精确路径与当前结构边**

以下路径前缀均为 `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/`；当前 17 项均为 `??`。

| # | 精确 source | stripped graph 中的直接 SCC outbound | post-45A 组件外结构 inbound |
|---:|---|---|---|
| 1 | `CloudGameClient.java` | retained state、service port | `InputSequences`、`TaskExecutionContext` |
| 2 | `CloudSummonSkillWholePassCapability.java` | exclusive authority、retained state | 无 production type-site |
| 3 | `CloudTaskExclusiveInteractionAuthority.java` | exclusive state、retained state、ledger、slot、gate、service port、final coordinator、remote port、transaction action | 无 production type-site |
| 4 | `CloudTaskExclusiveInteractionState.java` | retained state | 无 production type-site |
| 5 | `CloudTaskRetainedActionState.java` | ledger、gate、retained adapter、service port、transaction action | `LeaderPrecheckAction` |
| 6 | `CloudTaskRunActionLedger.java` | retained state、broker | 45A 投影后无 route caller |
| 7 | `CloudTaskRunAuthorityAssembly.java` | authority、retained state、ledger、executor、slot、gate、adapter、service context、final、remote port、broker | 45A 投影后无 route caller |
| 8 | `CloudTaskRunCommandExecutor.java` | summon capability、ledger、gate、remote port、broker | 无 production type-site |
| 9 | `CloudTaskRunCurrentContextSlot.java` | exclusive authority、assembly | `CloudTaskTurnAuthority` |
| 10 | `CloudTaskRunExecutionGate.java` | ledger | 无 production structural type-site |
| 11 | `CloudTaskRunRetainedLifecycleActivationAdapter.java` | assembly、slot | 无 production type-site |
| 12 | `CloudTaskServiceExecutionContext.java` | game client、authority、retained state、ledger、gate、service port、final、remote port | `TaskExecutionContext`、`CloudArtifactStore`、`ScopedPngArtifactStore` |
| 13 | `CloudTaskServicePort.java` | summon capability、authority、retained state、ledger、final、remote port | `TaskExecutionContext`、`CloudBagStateOwner` import |
| 14 | `RemoteFinalConsumptionCoordinator.java` | ledger、service port、broker | 45A 投影后无 route caller |
| 15 | `RemoteGameClientPort.java` | summon capability、ledger | 无 production type-site |
| 16 | `RemoteGameCommandBroker.java` | ledger | 45A 投影后无 route caller |
| 17 | `TaskTransactionAction.java` | retained state | 无 production structural type-site |

不可拆证据不是命名相似，而是双向真实 Java 类型依赖：

- `CloudTaskRunActionLedger.java:676-734,697,719-720,798-803` 直接消费 broker 及其
  `ControlReservation`、`ControlPublication`、`ControlRelease`、`ReceiptAcceptance` nested types。
- `RemoteGameCommandBroker.java:385-419,637` 直接消费 `CloudTaskRunActionLedger.CompactionPlan`。
- `CloudTaskRunCommandExecutor.java:30-35,379-393` 同时消费 ledger 与 broker。
- `RemoteFinalConsumptionCoordinator` 不是可延后的 wire DTO：它被
  `CloudTaskExclusiveInteractionAuthority.java:28,37`、`CloudTaskServiceExecutionContext.java:35-83`、
  `CloudTaskServicePort.java:20-29`、`CloudTaskRunAuthorityAssembly.java:47-90` 直接持有。

所以当前图不允许把 `RemoteGameCommandBroker` 与 ledger 分属并发卡，也不允许把
`RemoteFinalConsumptionCoordinator` 输出到 45B。

**3. 当前最小原子生产边界：17 core + 2 个条件 compile-closure leaf**

若 38/39/40 已先清掉第 4 节列出的 active 入边、45A 已真实落地，且 17 内边保持当前形状，则当前最小的单次
production compile boundary 是以下 `19` 个文件：

1. 第 2 节完整 `17/17` SCC；
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java`：
   `:58,1000,1004` 结构依赖 `CloudTaskRunCurrentContextSlot`；除本文件自身和
   `CloudTaskTurnCoordination.java` 外，当前 production/test 对 authority/handle 均无 caller；
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeaderPrecheckAction.java`：
   `:16-33` 结构依赖 `CloudTaskRetainedActionState`；当前 production/test 无其它 caller。

这只是当前 post-45A 图的**最小原子边界证据**，不是最终 delete cohort。两份 leaf 若在 38A 后已被正式 rewire，
则以 final source graph 为准；若仍保持当前边，就必须和 17 core 同一 patch 处理，不能留作引用已删除类型的孤立源码。

另有 38M 分类控制的五项，不能由本 helper提前归入或排除：

- `CloudGameContextStateOwner.java`
- `CloudLeftTopStatusSwitchState.java`
- `CloudPausedReadOnlyObservationContext.java`
- `CloudPlayerStateStateGovernor.java`
- `CommonBoxStateGovernor.java`

权威计划指定的固定分类文件
`docs/superpowers/plans/reports/2026-07-15-turn-38-authority-state-classification.md` 当前不存在。当前 38M helper
只给候选：前三项分别仍由 17 core 消费，后两项是 dormant/零外部 caller；最终父级分类若要求删除，目标波次是
44A，若要求保留重接，则必须先有真实 38C/40B/40C consumer。缺少固定分类时不能把 `19` 扩写成最终集合。

**4. post-45A 仍在 SCC 外的七个结构 consumer 文件**

| SCC 外 production source | 当前结构引用 | 真实 caller / 影响 | 必须先闭合的 predecessor |
|---|---|---|---|
| `com/bot/dhxy/input/InputSequences.java` | `:5,39,46 -> CloudGameClient` | 六个 active Task/Service 持有并调用；不能删 | whole Task/Service caller 收口后仍需父级给 `InputSequences.java` 精确 owner；当前 38/39 写集均不含该文件 |
| `com/bot/dhxy/runner/context/TaskExecutionContext.java` | `:10-13,35,51,319,331,442 -> CloudGameClient/CloudTaskServiceExecutionContext/CloudTaskServicePort` | `NavigationService.java:533-569` 仍调用 `getGameClient()`；bag/return-item/artifact 仍调用 `revalidate()`；大量 Task/Service 仍读 metadata | 38A 清 context old authority surface；39 再收口三个 old facade/context 类型 |
| `com/bot/dhxy/service/bag/CloudBagStateOwner.java` | `:4` import `CloudTaskServicePort`，即使只剩 JavaDoc 也仍是编译边 | `:394` 调 `TaskExecutionContext.revalidate()`；`BagWorkflowState.java:619` 同样调用 | 38B1；不得把仍有 active state caller 的 bag owner并入删除集 |
| `cloudbrain/host/CloudArtifactStore.java` | `:3,30,37,44 -> CloudTaskServiceExecutionContext` | `CloudServiceConfiguration.java:89-90` 创建实现 | 38B4 改为 turn-native、powerless exact authority |
| `cloudbrain/host/ScopedPngArtifactStore.java` | `:3,62,153,174,197,209 -> CloudTaskServiceExecutionContext` | `:201-206` 还读 old scope/authorization | 38B4；40C 激活前必须确认 host wiring 使用新 authority |
| `remote/CloudTaskTurnAuthority.java` | `:58,1000,1004 -> CloudTaskRunCurrentContextSlot` | 当前无其它 caller | 若 38A 未重接，则进入第 3 节同一 compile closure |
| `remote/LeaderPrecheckAction.java` | `:16-33 -> CloudTaskRetainedActionState` | 当前无其它 caller | 若 38A 未重接，则进入第 3 节同一 compile closure |

另有五个只有 comment/string token 的 production 文件：
`BagWorkflowState.java:763`、`ReturnItemPrescanWorkflowState.java:17`、
`CloudPausedReadOnlyObservationContext.java:15,53`、`CloudTaskRunExecutionContext.java:47`、
`NavigationWorkflowState.java:10`。它们不是当前 `clean compile` 的结构入边，但 final exact source guard 必须明确是
只禁结构 symbol、还是连旧名文档也禁；不能让宽泛字符串规则误删 active model。

**5. `InputSequences`、`TaskExecutionContext` 与 metadata 的 active caller 证据**

1. `InputSequences` 当前不是 inert 文件。字段 caller 是
   `DialogService.java:82`、`NavigationService.java:178`、`NpcClickService.java:103`、
   `WubeiTask.java:278`、`FiveRingTaskV2.java:217`、`XiuluoTaskV2.java:279`；当前分别有
   `10/13/9/4/9/3` 个 `inputSequences.*` invocation。只要这些 caller 尚在，就不得把
   `InputSequences` 或其 `CloudGameClient` 依赖判为可删。
2. 当前 `InputSequences.java:35-158` 不声明 `submitExclusiveAndWait(...)`，但 active source 仍在
   `DialogService.java:1904,2486,2814`、`NpcClickService.java:371,3326`、
   `NavigationService.java:1370,1594,1657,1866,1928,2041,2178,2191,2311,2363,2445,2522`
   调用该方法。这是静态可见的跨卡中间态；不能以当前树声称已有 production compile baseline。
3. `TaskExecutionContext` 的 old public authority surface仍在：`:314-315 revalidate()`、`:319-320
   getGameClient()`、`:331-332 getRemoteGameClient()`、`:442-455 legacyDelegate()/metadata()`。
   `revalidate()` 的 active consumer 还包括 `BagWorkflowState.java:619`、`CloudBagStateOwner.java:394`、
   `CloudReturnItemPrescanStateOwner.java:625`、`ScopedPngArtifactStore.java:205`，分别落在 38B1、38B2、38B4。
4. `CloudTaskServiceMetadata.java` 不在 17 core。它仍由 SCC 内 assembly/adapter/service context 以及
   `TaskExecutionContext.java:39,65,78,100,454-455` 消费；record 的 role/team/retry/startup/start-time 字段仍被
   `AutoBattleTask`、`XiuluoTaskV2`、`BaseTaskTemplate`、`TaskStepExecutor`、`AutoCombatService`、
   `TaskMaintenanceService`、`TeamReturnService`、`PlayerStateService`、`CommonBoxService`、
   `LeftTopStatusSwitchService`、`TaskTrackerPanelService`、`AutoCombatPanelService`、
   `CloudStartupGateAuthority` 等真实业务源码读取。当前不能把 metadata 当 45B 零引用 DTO。
5. TURN-39 精确 production 写集虽包含 `CloudGameClient`、`CloudTaskServicePort`、
   `CloudTaskServiceExecutionContext`、`CloudTaskServiceMetadata`、`TurnGameClient`、
   `LegacyTaskExecutionTurnContextProvider`，却不含 `TaskExecutionContext` 或 `InputSequences`。因此 44A 不能假定
   “39 修改 facade 文件”会自动消掉 SCC 外 caller；必须查看 38A final context 和另行冻结的 InputSequences owner。

**6. 38/39/40 必须先闭合的入边与消费替代**

| predecessor | 44A 前必须已有的 source fact | 当前事实 |
|---|---|---|
| 38A | `TaskExecutionContext` 不再 import/construct/return 17 core 类型；checkpoint/sleep/Task 调用仍保持原语义 | old delegate、old getters、old authorization 仍在 |
| 38B1 | bag workflow/state owner 不再依赖 old ServicePort/run authorization/scope/window | import 与 `revalidate()` 调用仍在 |
| 38B2 | return-item state owner 不再依赖 old authorization/binding/scope/window | `CloudReturnItemPrescanStateOwner.java:625` 及 old run type refs仍在 |
| 38B3 | startup gate 的 role/metadata authority 与 final context 一致，不从 old run scope 补权 | 当前仍消费 context role，并引用 old run scope |
| 38B4 | artifact store 使用 final turn-native exact authority，且 host 配置不再需要 old service context | interface/implementation仍直接接 old context |
| 38M/38C | 五个 authority state 的固定分类、每个保留行的新 consumer、每个删除行的 44A 归属均已冻结 | 固定分类文件不存在 |
| 39 | active caller 已只依赖 final TurnGameClient/context；old facade 只作为待删 SCC 存在且不再被 SCC 外生产引用 | TURN-39 报告列出的 active ref、metadata 与 test ownership 裂口仍存在；39 还会改 17 core 中三文件，故之后必须重算 SCC |
| 40B | final Cloud Task factory/runtime/registry/control port 已拥有所需 current in-memory state，不重新引入 session/ledger/old owner | 五个计划 production 文件及两个 named tests当前全部不存在 |
| 40C | final host/runtime configuration 注入同一 host-local state/context，server 只激活新 turn/template ingress，close order完整 | `CloudTurnRuntimeConfiguration.java` 和 activation test不存在；`CloudServiceHost.java` untracked，`CloudBrainServer.java` tracked modified |

40D 不产生 Cloud 17 core 的直接 Java 入边，但仍是父级删除波次前的双端 activation 消费门；本 helper不以 Cloud
静态图替代它。

**7. 测试源码引用与唯一 named guard ownership**

- TURN-44A 唯一允许的新测试是
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldAuthorityRemovalGuardTest.java`，当前不存在。未来唯一命名命令是
  `mvn -q -Dtest=OldAuthorityRemovalGuardTest test`，随后才是 Cloud `mvn -q clean compile`；本文未运行。
- `OldAuthorityRemovalGuardTest` 只能对 final manifest 的逐 path/逐 symbol 做 ZERO source guard，并证明 production
  不再引用已删除 symbol；不能用 `remote/` 整体不存在、wildcard、文件名前缀或宽泛字符串替代 exact cohort。
- 两个现有测试有真实 test-compile type dependency：
  1. `runner/context/TaskExecutionContextTurnContractTest.java:24-27,497,503,505` 直接 import/引用
     `CloudGameClient`、`CloudTaskServiceExecutionContext`、`CloudTaskServicePort`，且 `:495-506` 明确要求 old
     constructor/getters 存在；原测试归属是 TURN-13C，38A/39/父级尚需冻结后续写 owner。
  2. `service/SummonSkillTurnContractTest.java:34,956-969` 直接 import/实例化
     `CloudSummonSkillWholePassCapability`，并读取待删 source path；原测试归属是 TURN-33，44A 的唯一 guard 无权
     顺手改写该文件。
- 两个现有测试只有 literal/reflection dependency：
  1. `service/LeftTopStatusTurnContractTest.java:540,546,549,556` 用类名字符串反射构造 broker/ledger/final/assembly；
     删除后 testCompile 可过，但未来执行该旧合同会失败。原测试归属 TURN-19，必须先冻结更新/退役 owner。
  2. `service/PlayerStateTurnContractTest.java:480` 只是断言 active Service source 不含 `CloudGameClient`；它不加载旧类，
     删除后该负向合同仍可保留。
- `CloudTaskServiceMetadata` 当前被 17 个 test class 直接作为 fixture。若 39 删除或改签名，Maven 会在运行 44A
  named guard 前先 test-compile 全部测试源码；只新增一个 guard 不能隐藏这些现存编译边。

**8. 44A 的 compile-safe 串行边界与未来 gate**

未来实施顺序必须保持：

1. 38A、38B1-4、38M/38C、39、40B、40C/40D 的 final source/test ownership 先稳定，所有 active SCC 外
   structural inbound 为零；
2. 44M45M 基于该 final source graph 冻结 exact path/symbol/reference/SHA/compile cohort；本报告不生成这些字段；
3. 45A 真实修改 `CloudBrainServer`、删除 old routes/endpoint，并先完成自己的 guard 与 Cloud compile；
4. 重新计算全 production/test graph。若仍是当前形状，则 17 core 与仍保留旧边的两个 leaf 在一个 44A patch 中
   原子处理；同时只加入父级固定分类中明确归 44A 的 state 文件；
5. 未来执行 `OldAuthorityRemovalGuardTest`，再执行 Cloud `mvn -q clean compile`；两者之间不得插入 45B 删除；
6. 只有 44A gate 结束后，才把第 9 节的 residual seed 交给 45B 重新做逐文件零引用图。

不存在以下中间 compile point：先删 ledger 后留 broker、先删 broker 后留 ledger、先删 authority/context 后留
final coordinator，或 44A/45B 并发各删半个 component。

**9. 输出给 45B 的当前 residual seed，不是最终 45B manifest**

在条件投影“45A 已移除 routes/endpoint，44A 最小 19 文件已原子移除”下，当前 stripped production graph 的
第一轮零外部引用 seed 是：

- `remote/CloudTaskTurnCoordination.java`
- `remote/NavigationWorkflowState.java`
- `remote/RemoteTaskRunActionResponse.java`
- `remote/RemoteCommandOutcomeAck.java`
- `remote/RemoteCommandOutcomeEnvelope.java`
- `remote/RemoteCommandPollRequest.java`
- `remote/RemoteCommandPollResponse.java`
- `remote/RemoteFinalConsumedReceiptAck.java`

同一计算还会让 `CloudGameContextStateOwner`、`CloudLeftTopStatusSwitchState`、
`CloudPausedReadOnlyObservationContext` 失去 17 core caller，但这三项受 38M 固定分类控制：若父级分类归 44A，就不
输出给 45B；若保留重接，则必须存在新 consumer，不能再称零引用 residual。

`remote/run/` 的 12 个 old run source 当前不能作为第一轮零引用输出：`TaskExecutionContext`、bag/return-item、
artifact store、38M state 文件及 run types 自身仍形成入边。删除上述八个 seed 后还可能让
`RemoteCommandEnvelope` 等下一层 DTO 变成零引用；45B 必须迭代重算而不是照目录或本报告列表删除。

明确不输出：`RemoteGameCommandBroker.java`、`RemoteFinalConsumptionCoordinator.java`、
`CloudTaskRunActionLedger.java`。三者都属于 44A 的 17 core。所有仍被 active Task/Service 使用的
capture/input/local-macro/task-tracker/result/port/model 也继续受保护。

**10. 风险与 STOP-WORK 条件**

出现任一项即停止 44A 删除动作并回到父级/前置卡补证据：

1. 45A 仍只是 PRECHECK，或 `CloudBrainServer`/gateway 仍注册 old task-run/poll/outcome/final-consumed route。
2. `InputSequences`、`TaskExecutionContext`、bag owner、artifact store 任一结构引用 17 core；不得把这些 active
   caller 强行吸收到 44A 删除集。
3. `InputSequences` 仍有任一 active Task/Service caller，或其 precise modify/delete owner仍未冻结。
4. 38M 固定分类文件仍不存在，或任一 state 的保留 consumer/删除归属未逐项冻结。
5. 39 尚未完成，或其对 `CloudGameClient`、`CloudTaskServicePort`、`CloudTaskServiceExecutionContext` 的 final
   修改后未重算 17-file SCC。
6. 40B runtime/factory/registry/control 与 40C runtime configuration/activation 仍不存在，或新 runtime 又引用 old
   session/ledger/final-consumed authority。
7. `TaskExecutionContextTurnContractTest`、`SummonSkillTurnContractTest` 的 test-compile 依赖没有明确 owner；
   `LeftTopStatusTurnContractTest` 的反射旧合同没有未来完整测试族处理方案。
8. final source graph 中 ledger 与 broker 仍双向引用，却有人计划拆卡、拆提交或与 45B 并发；final coordinator仍在
   17 core 却被留给 45B。
9. 任一拟删类型仍被 active Task/Service 调用，或仅凭“当前无 Spring caller”“untracked”“文件名含 remote”判定可删。
10. 45A/40C patch 改动或移除 `CloudTurnHttpHandler.PATH`、`CloudTemplateHttpHandler.PATH_PREFIX`、共享
    `CloudTurnRoutes`/exchange，未证明保留新 HTTPS turn ingress。
11. 任一 17 core、两个 compile-closure leaf、38M state、测试源码或 server wiring 在 manifest freeze 后发生变化；
    必须重扫引用与 cohort，不能沿用本文行号。
12. 任何步骤要求回滚、覆盖、清理、删除、暂存、提交或其它 Git mutation 来“整理”当前 dirty/untracked 工作树。

**11. 本 helper 写集与互斥确认**

- 唯一写路径：
  `docs/superpowers/plans/reports/2026-07-16-turn-44A-post-45A-deletion-readiness-helper-r1.md`。
- 与所有 Cloud/DHXY Java、test、主计划、ACTIVE_WORK、CR271、矩阵、dashboard 写集互斥；没有生成最终 manifest，
  没有删除或改写任何源文件。
- 本文只记录当前 PRECHECK 证据。未来任何 source/test/route/state-owner 变化都要求重新计算，不允许把本报告当作
  删除授权。

PRECHECK_COMPLETE true EOF
