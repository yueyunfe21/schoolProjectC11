# PRECHECK

- 范围：`TURN-44M45M Cloud deletion-manifest` 的当前生产源码图、生产引用、Cloud 删除顺序
  `45A -> 44A -> 45B`、`CloudBrainServer` 注册面以及未来 manifest/test 合同预检。本报告不生成
  `docs/superpowers/plans/reports/2026-07-15-turn-44-45-cloud-old-wire-delete-manifest.md`，不对任何生产文件作最终
  `KEEP/REWIRE/DELETE` 裁决，也不执行删除。
- 已完整读取：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、
  `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`、
  `docs/业务逻辑.md`、两轮删除/互斥审计，以及两仓当前目标源码。
- 读取快照：DHXY 位于 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`，
  tracked dirty `44`、untracked `588`；Cloud 位于 `navigation-migration`，HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`，tracked dirty `9`、untracked `541`。两仓均无 upstream。
  这些 dirty/untracked 全部视为受保护输入；本次没有 Git mutation。
- Cloud 目标源码状态不是稳定 hash 基线：`CloudBrainServer.java` 为 tracked modified；
  `RemoteTaskRunRoutes.java`、`RemoteTaskRunEndpoint.java`、`remote/` 下当前旧图和 host 相关源码大多为 untracked。
  因而本次不记录 byte size/SHA-256，不把当前字节冻结成未来 manifest。
- 当前生产库存边界：
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/` 直属 Java `137` 个，
  `remote/run/` Java `12` 个，另有 `api/RemoteTaskRunEndpoint.java`，共 `150` 个候选盘点文件。
  这是待逐行分类的库存，不是删除集合；尤其不能按目录、glob、类名前缀批量判定。
- 源图方法：只扫描两仓 `src/main/java/**/*.java`；以类型声明/引用建立当前生产图，并在 SCC 扫描中排除注释和
  字符串字面量。它能证明当前显式源码边，但不能替代未来 Spring 配置、反射、资源注册、编译器或 named-test
  对最终快照的复核。
- HTTPS turn 保留边界：Cloud 当前仍通过
  `CloudBrainServer.java:96-107` 创建并注册 `CloudTurnRoutes`、`/api/v1/client/turn` 与
  `/api/v1/templates/`。协议仍是单个客户端发起的 HTTPS request/response turn，不得通过旧 poll/outcome、session、
  workflow ledger 或旧 authority 恢复另一套权威。
- 业务保护边界：五倍、修罗、五环、AutoBattle 和召唤兽维护的 phase 顺序、fallback、次数、点击/识别顺序、
  stop/pause 语义以及 `696a12b0` 基线均不属于删除卡可改变的范围。静态技能格预识别只改变识别前置，不能借删除
  卡改变普通技能删除、终极角、保留、冷却或队列语义。
- 本次没有运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input。

## 依赖

1. 权威注册表给 `TURN-44M45M` 的前置是 `TURN-41 + TURN-39`。当前只能形成预检材料：
   `TURN-39` 尚会修改 `CloudGameClient.java`、`CloudTaskServicePort.java`、
   `CloudTaskServiceExecutionContext.java`、`CloudTaskServiceMetadata.java`、`TurnGameClient.java` 和
   `LegacyTaskExecutionTurnContextProvider.java`；这些正处于当前旧 context 图内，引用与 hash 必然可能变化。
2. `TURN-41` 是 fresh runtime 用户门，且依赖 `40B/40C/40D` 双构建。它不是本 helper 可执行或替代的门；本报告也
   不把当前静态图冒充 fresh runtime 证据。
3. `TURN-40C` 会写 `CloudBrainApplication.java`、`CloudBrainServer.java`、`CloudTurnHttpHandler.java`、
   `CloudTurnRoutes.java`、`CloudServiceHost.java` 并创建 `CloudTurnRuntimeConfiguration.java`。因此 45A 对
   `CloudBrainServer.java` 的精确行、引用、byte size 和 SHA-256 必须等 40C parent freeze 后再取。
4. `TURN-38A`、`38B1/B2/B3/B4`、`38M/38C`、`39` 负责先移除或重接 Task context、bag state、return state、startup、
   artifact/configuration 和五个 authority-bound state 的旧依赖。任何仍有 active Task/Service caller 的 port、result、
   macro、context 或 state 类型都必须继续保留到对应引用真正消失。
5. Cloud 删除顺序必须串行：
   - 45A 先从 `CloudBrainServer` 断 old route registration，并移除 route/endpoint；
   - 44A 再处理完整 old facade/authority/context/final-consumption source SCC；
   - 45B 最后只处理 44A 后的 old broker/task-run wire/transport 零引用残余。
6. 未来 manifest 每一行必须包含：精确 repo-relative path；文件主 symbol；`KEEP/REWIRE/DELETE`；全部剩余生产引用的
   精确 path+line；移除/重接每个引用的 card；manifest 时 byte size 与 SHA-256；删除前置与 compile cohort；
   permanent-local Service、NPC reference/shadow 等明确保护原因。任一 hash 或引用变化都必须重新生成该行，不能扩大
   原写集。

## 真实 caller

### old route 与 server registration

| 被引用对象 | 当前真实 caller/构造点 | 结论边界 |
|---|---|---|
| `RemoteTaskRunRoutes` | `CloudBrainServer.java:12,51,58,86-92` | Server 仍持有 `AuthorityRouteBundle` 并把其 routes 加入 gateway；45A 前不能把 route 图视为零 caller。 |
| old poll/outcome/task-run paths | `CloudBrainServer.java:42-44,87-89` | 当前注册 `/api/cloud/remote/poll`、`/api/cloud/remote/outcome`、`/api/cloud/remote/task-run`。 |
| `RemoteTaskRunEndpoint` | `RemoteTaskRunRoutes.java:10,63` | endpoint 的唯一生产注册 caller 是 old route factory；先删 route caller 后才能同卡移除 endpoint。 |
| `RemoteGameCommandBroker` | `RemoteTaskRunRoutes.java:51,87-112` | route factory 实例化 broker，poll/outcome endpoint 直接持有它。 |
| `CloudTaskRunActionLedger` | `RemoteTaskRunRoutes.java:52` | route factory 实例化 ledger。 |
| `RemoteFinalConsumptionCoordinator` | `RemoteTaskRunRoutes.java:53-54,127-130` | route factory 实例化并暴露 receipt ingress。 |
| `CloudTaskRunAuthorityAssembly` | `RemoteTaskRunRoutes.java:55-64` | route factory 构造 authority anchor，并由 bundle 强引用。 |
| new turn/template handlers | `CloudBrainServer.java:96-107` | 45A 必须保留 `CloudTurnRoutes`、`CloudTurnHttpHandler.PATH` 和 `CloudTemplateHttpHandler.PATH_PREFIX` 注册。 |

`CloudBrainServer`、`RemoteTaskRunRoutes`、`RemoteTaskRunEndpoint` 在当前强连通分析中各自是 singleton SCC；route
本身可以先与 server registration 一起切断，不需要先拆 17 文件 authority/broker SCC。

### 当前跨 44A/45B 的 source SCC

当前源码存在一个不可拆的 `17` 文件强连通分量，精确 repo-relative path 为：

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionState.java`
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`
6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java`
7. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java`
8. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java`
9. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCurrentContextSlot.java`
10. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`
11. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunRetainedLifecycleActivationAdapter.java`
12. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`
13. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`
14. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumptionCoordinator.java`
15. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java`
16. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java`
17. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTransactionAction.java`

关键互返边不是命名推断：`CloudTaskRunActionLedger.java:676-803` 直接消费
`RemoteGameCommandBroker` 及其 nested reservation/publication/acceptance；
`RemoteGameCommandBroker.java:385-637` 反向消费 `CloudTaskRunActionLedger.CompactionPlan`；
`CloudTaskRunCommandExecutor.java:29-35,377-393` 同时持有并协同 ledger/broker。故当前图中
`RemoteGameCommandBroker` 与 `RemoteFinalConsumptionCoordinator` 不能被留给一个独立的后续删除卡，同时在前卡删除
ledger/authority。

当前 17 文件 SCC 的外部生产 consumers 还包括：

| 外部 consumer | 当前引用 |
|---|---|
| `src/main/java/com/bot/dhxy/input/InputSequences.java` | `:5,39,46` 使用 `CloudGameClient`。 |
| `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java` | `:10-14,35,51,319,331,442` 使用 `CloudGameClient`、`CloudTaskServiceExecutionContext`、`CloudTaskServicePort` 及 old run scope/authorization。 |
| `src/main/java/com/bot/dhxy/service/bag/CloudBagStateOwner.java` | `:4` 使用 `CloudTaskServicePort`。 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudArtifactStore.java` | `:3,30,37,44` 使用 `CloudTaskServiceExecutionContext`。 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/ScopedPngArtifactStore.java` | `:3,62,153,174,197,209` 使用 `CloudTaskServiceExecutionContext`，并在 `:4-5,201-205` 使用 old run authorization/scope。 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java` | `:58,1000,1004` 使用 `CloudTaskRunCurrentContextSlot`。 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeaderPrecheckAction.java` | `:16-33` 使用 `CloudTaskRetainedActionState`。 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteTaskRunRoutes.java` | `:51-55,87-130` 构造/持有 broker、ledger、final coordinator、authority assembly。 |

前五个 consumer 必须由 38A/38B/39 等先重接；route consumer 由 45A 先移除；若
`CloudTaskTurnAuthority.java`、`LeaderPrecheckAction.java` 在 44A 前仍保持这些边，则它们必须随同一 compile cohort
处理，不能留作引用已删除类型的孤立源码。

### active Task/Service caller 保护

Cloud 仓当前 copied production Task 仍真实消费这些 Service/input 边界：

| active caller | Service/input fields |
|---|---|
| `task/wubei/WubeiTask.java:261-278` | `NavigationService`、`DialogService`、`AutoCombatService`、`PlayerStateService`、`TaskMaintenanceService`、`CommonBoxService`、`TeamReturnService`、`InputSequences` 等。 |
| `task/wuhuan/FiveRingTaskV2.java:200-217` | `NavigationService`、`DialogService`、`PlayerStateService`、`AutoCombatService`、`InputSequences` 等。 |
| `task/xiuluo/XiuluoTaskV2.java:253-279` | `NavigationService`、`DialogService`、`AutoCombatService`、`PlayerStateService`、`TaskMaintenanceService`、`CommonBoxService`、`TeamReturnService`、`InputSequences` 等。 |
| `task/AutoBattleTask.java:47-53` | `AutoCombatService`、`PlayerStateService`、`TaskMaintenanceService`、`TeamReturnService`、`CommonBoxService`、`LeftTopStatusSwitchService`。 |
| `service/TaskMaintenanceService.java:48-49` | `DialogService`、`SummonSkillService`。 |

这些 Service 当前又真实引用 old remote facade/port：

| Service/client | 仍在使用的 old remote 类型 |
|---|---|
| `PlayerStateService.java:23,104,117,140` | `CloudBagUseIncensePort` |
| `CommonBoxService.java:9,59,65,70` | `CloudCommonBoxPort` 及其结果模型 |
| `DialogService.java:10-14,91-95` | 五个 `CloudDialog*Port` 及对应 command/result |
| `LeftTopStatusSwitchService.java:6,40` 与 `service/lefttop/CloudLeftTopStatusPortAssembly.java:19,50` | `CloudLeftTopStatusPort` 及其结果模型 |
| `AutoCombatService.java:15,42`、`NavigationService.java:72,183`、`SummonSkillService.java:28,159-177` | `CloudUiCleanerPort` 及 UI-clean command/result |
| `TeamReturnService.java:11,39,54-371` | `CloudTeamReturnPort` 及 observation/click outcome |
| `turn/client/CloudBagLocalServiceClient.java:18,39` | `CloudBagUseIncensePort` |
| `turn/client/CloudUiCleanerLocalServiceClient.java:13,30,72` | `CloudUiCleanerPort` |
| `host/CloudServiceConfiguration.java:10-11,37-38` | `CloudCommonBoxPortAssembly`、`CloudTeamReturnPortAssembly` |

因此上述 facade/port/result/macro 类型本次全部只作保护对象，不得因为位于 `remote/` 或名称含 `Cloud`/`Remote`
就判为可删。必须等对应 business caller card 将 active Service 重接到最终 turn client，并由未来 manifest 证明生产
引用为零。

`CloudServiceHost` 当前没有 self/config 注释之外的生产构造 caller，但这不是删除依据：40C 将激活它并要求同一个
exchange/runtime/host lifecycle。当前“无 caller”会被未来实现改变，必须以 40C 后源码重新扫描。

## 精确写集

### 本 helper 实际写集

- 仅 `docs/superpowers/plans/reports/2026-07-16-turn-44M45M-readiness-preflight-helper.md`，首次创建后按 append-only
  规则写入。未写 Java、test、权威计划、ACTIVE_WORK、CR271、矩阵或 dashboard。

### 未来 45A 已知生产写集

1. Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`。
2. Delete `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteTaskRunRoutes.java`。
3. Delete `src/main/java/com/yueyunfe/dhxy/cloudbrain/api/RemoteTaskRunEndpoint.java`。

这是权威计划已经点名的生产边界；最终 worker 仍只能删除 manifest 中 hash 匹配的对应行。Server 修改必须移除
old route constants、bundle field/constructor wiring、`RemoteTaskRunRoutes.create(...)` 和 `routes.addAll(...)`，同时保留
最终 40C 形成的 turn/template/runtime/host wiring。这里不给出最终 patch 或当前行号替换方案。

### 未来 44A 当前最小不可拆 cohort

当前源图要求前述 17 个精确路径处于同一 compile cohort；这只是 44A 最小 SCC 证据，不是最终删除 manifest。
未来 44A 还必须对以下直接 consumer 作逐文件裁决：

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeaderPrecheckAction.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionContext.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java`
- 38M 点名的五个 state：`CloudGameContextStateOwner.java`、`CloudLeftTopStatusSwitchState.java`、
  `CloudPausedReadOnlyObservationContext.java`、`CloudPlayerStateStateGovernor.java`、`CommonBoxStateGovernor.java`

其中前两个若边未消失，就必须随同一 cohort 处理；后七个是否 `KEEP/REWIRE/DELETE` 只能由 38M/38C、39 和最终
source graph 决定。本报告不提前归类。

### 未来 45B 当前盘点边界

45B 的精确删除写集现在不能冻结。当前需在 44A 后重新逐文件展开并扫描的 wire/transport 候选至少包括：

- `remote/run/` 下当前 12 个 exact source：`ExecutionConfirmationRecord.java`、
  `RemoteTaskRunAuthorization.java`、`RemoteTaskRunBinding.java`、`RemoteTaskRunCapacityException.java`、
  `RemoteTaskRunCoordinator.java`、`RemoteTaskRunPrepareRequest.java`、`RemoteTaskRunScope.java`、
  `RemoteTaskRunSessionConflictException.java`、`RemoteTaskRunStatus.java`、`RemoteTaskRunValidation.java`、
  `RemoteTaskRunWindow.java`、`ResumeConfirmationRequirement.java`。
- task-run wire：`RemoteTaskRunAction.java`、`RemoteTaskRunActionRequest.java`、
  `RemoteTaskRunActionResponse.java`、`RemoteTaskRunError.java`、`RemoteTaskRunErrorCode.java`、
  `RemoteTaskRunReceipt.java`、`ResumeExecutorReadinessFact.java`。
- command/final wire：`RemoteCommandEnvelope.java`、`RemoteCommandOutcomeAck.java`、
  `RemoteCommandOutcomeEnvelope.java`、`RemoteCommandPollRequest.java`、`RemoteCommandPollResponse.java`、
  `RemoteFinalConsumedAck.java`、`RemoteFinalConsumedReceipt.java`、`RemoteFinalConsumedReceiptAck.java`。
- shared transport/value 候选：`RemoteClientScope.java`、`RemoteOperation.java`、`RemoteOutcome.java`、
  `RemoteProtocolDigests.java`、`RemoteProtocolValidation.java`、`RemoteRequest.java`、
  `RemoteSemanticAddress.java`、`RequestContext.java`、`StopRef.java`、`WindowBindingRef.java`。

上列是明确的未来扫描输入，不代表这些文件当前可删。特别是计划曾把 `RemoteGameCommandBroker.java` 和
`RemoteFinalConsumptionCoordinator.java` 作为 45B anchor，但当前二者已进入 17 文件 SCC；若最终图仍如此，二者
必须归入 44A 同一 cohort，而不能留到 45B。所有仍被 active Service 使用的 macro/result/port 不进入当前 45B
删除写集。

## 风险

1. **跨卡 SCC 风险：** 当前 17 文件 component 同时包含 authority/context 与原计划 45B 的 broker/final anchor。
   若照名字机械分卡，会在 44A 后留下对已删除 nested type 的引用，形成不存在的中间编译点。
2. **active caller 风险：** 五倍、修罗、五环、AutoBattle、维护以及多个 Service 仍通过 old facade/port/result/macro
   编译和执行业务。把目录库存当删除集会直接切断真实 caller，并可能改变用户确认的业务顺序与 fallback。
3. **hash 漂移风险：** Cloud 旧图大量 untracked，`CloudBrainServer.java` 已 dirty；DHXY/Cloud 还有并发工作。
   当前 byte/hash 没有未来删除授权意义，最终 manifest 必须在前置实现稳定后重新取值。
4. **40C 激活风险：** 当前 `CloudServiceHost` 无生产构造 caller，只说明尚未激活；40C 会新增 caller、生命周期和
   close order。基于当前 dormant 图删除 host/config/port 会误判未来生产引用。
5. **route 保留风险：** 45A 若宽泛清除 server route wiring，可能连带移除 `/api/v1/client/turn` 或
   `/api/v1/templates/`；future source guard 必须验证 new routes 仍注册。
6. **Spring/动态引用风险：** `@Import`、configuration class 列表、component scan、nested type 和资源路径可能不被
   简单 symbol grep 完整覆盖。最终必须使用精确 source guard 加 Cloud compile 复核，不得只看文件名零命中。
7. **分类时序风险：** 38M 五个 state 和 39 四个 old context 文件尚会被重接/改写；现在做最终
   `KEEP/REWIRE/DELETE` 会把预迁移图误当最终图。
8. **协议回潮风险：** 保留 old broker/wire 过久不等于允许新 turn runtime 调用它；40B/40C 必须继续遵守最小
   HTTPS turn，不把旧 poll/outcome、authority ledger 或 session 概念接回新路径。

## 未来 named-test

权威计划只冻结了三个 class name；当前三份源码均不存在。本次没有创建或运行测试。按当前 Cloud test package
布局，未来精确路径需在 parent freeze 时确认；建议路径及合同如下：

| Card | future test path | future command | 必须证明的条件 |
|---|---|---|---|
| 45A | `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldRemoteRouteRemovalGuardTest.java` | 在 Cloud 仓运行 `mvn -q -Dtest=OldRemoteRouteRemovalGuardTest test` | production 对 `RemoteTaskRunRoutes`、`RemoteTaskRunEndpoint` 及三个 old route path 的注册零命中；`CloudTurnHttpHandler.PATH`、`CloudTemplateHttpHandler.PATH_PREFIX` 和各自 `createContext` 仍存在。 |
| 44A | `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldAuthorityRemovalGuardTest.java` | 在 Cloud 仓运行 `mvn -q -Dtest=OldAuthorityRemovalGuardTest test` | manifest 冻结的完整 authority/facade/context/final-consumption SCC 已整体移除，production 无引用已删除 symbol，且 source guard 不以宽泛 `remote/` 目录不存在作为替代。 |
| 45B | `src/test/java/com/yueyunfe/dhxy/cloudbrain/OldRemoteWireRemovalGuardTest.java` | 在 Cloud 仓运行 `mvn -q -Dtest=OldRemoteWireRemovalGuardTest test` | manifest 冻结的 broker/task-run/wire/transport residual 逐 symbol 零引用；仍被 Cloud-owned business model 或 active Service 使用的类型得到显式保护。 |

每张未来删除卡在自己的 named-test 后还需执行权威计划规定的 Cloud compile；本 helper 不执行这些命令。测试源码
本身必须读取 final manifest 的 exact symbol/path 集或等价的逐项常量，不能用 wildcard、目录整体不存在、前缀
扫描冒充逐文件证明。

## 互斥证据

1. `TURN-40C` 与 45A 都写 `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`；两者必须串行，
   且 45A 只能基于 40C final source 做 patch/hash/reference freeze。
2. 45A 的 route sever 与 44A 的 authority SCC 不能并行：当前 `RemoteTaskRunRoutes.java:51-55` 是 broker/ledger/
   final/authority assembly 的直接构造 caller。45A 先移除此 caller，44A 才能得到 unreachable 但仍可编译的旧图。
3. 44A 与 45B 不能并行：当前 `CloudTaskRunActionLedger <-> RemoteGameCommandBroker` 是双向源码边，且
   `RemoteFinalConsumptionCoordinator` 同在 17 文件 SCC。最终 manifest 必须把整个 component 放在同一卡；45B 只接
   收该卡之后真实零引用的 residual。
4. `TURN-39` 写 17 文件 SCC 中的 `CloudGameClient.java`、`CloudTaskServicePort.java`、
   `CloudTaskServiceExecutionContext.java`，并写相关 metadata/client provider；44M45M 必须后于 39，不能与其并行取
   final hash/reference。
5. `TURN-38B4` 写 `CloudArtifactStore.java`、`ScopedPngArtifactStore.java`、`CloudServiceConfiguration.java`，这些是
   当前 SCC 的外部 consumers；44A 的 final zero-reference 检查必须等其重接结果稳定。
6. 本 helper 的唯一写路径与两仓 Java/test、主计划、ACTIVE_WORK、CR271、矩阵和 dashboard 均不重叠；本次没有产生
   Git、构建或运行态互斥事件。

PRECHECK_COMPLETE true EOF
