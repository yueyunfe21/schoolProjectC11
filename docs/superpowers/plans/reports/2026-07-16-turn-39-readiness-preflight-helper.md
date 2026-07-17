# CR271 TURN-39 readiness preflight helper

## 0. 角色、范围与结论口径

- 角色：CR271 Internal 非绑定 readiness helper，只做 TURN-39 开工前只读审计。
- 本文不实施 TURN-39，不修改卡状态，不冻结父级设计，不给出卡片通过结论。
- 唯一写入是本文。未修改 Java、测试、权威计划、CR271、`ACTIVE_WORK.md` 或其它报告。
- 未执行 Git mutation，未运行 Maven/JUnit/compile/package，也未启动 runtime、application、server、Task、UI、capture 或 input。
- 当前证据存在真实开工阻断，故本文 true EOF 使用 `REAL_BLOCKER_CONFIRMED`，不是把 TURN-39 卡片改成某个状态。

## 1. 已读权威输入与覆盖规则

本轮完整读取并对照：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。其中 `:37-38` 给出真实主链顺序，`:52-66` 固定 Cloud/local 业务边界，`:85-93` 固定 per-window state/HWND/临时文件边界。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271，尤其当前 `:3-18`：TURN-34A 仍在实现，Internal Dewey 槽为 TURN-39 readiness，Java writer 活动期间不运行 Maven/JUnit/compile，且无已批准业务差异。
4. 权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14 至 19 节。
5. HTTPS turn 协议 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` 全文。
6. `docs/业务逻辑.md` 全文，包含本地队伍边界、五环、五倍、修罗 `696a12b0`、pause/stop、retry/fallback/park/TTL 与 NPC FIFO 规则。
7. 两仓当前完整 `git status --short --branch --untracked-files=all`，以及下文列出的 Cloud 生产/测试源码和全部 symbol reference。
8. 前置证据 `reports/2026-07-16-turn-38M-authority-classification-preflight-helper.md` 与 `reports/2026-07-16-turn-40B-readiness-preflight-helper.md`。

覆盖规则必须先应用：

- 权威计划 `:1035-1043` 明确第 16 节状态/依赖、第 17 节写集、第 18 节波次覆盖第 5 至 13 节冲突内容。
- 因此旧段 `:866-879` 中“Create `turn/TurnGameClient` / `TurnTaskServicePort` / `TurnTaskServiceExecutionContext`”不是当前执行写集。TURN-39 不得据此新增第二套 facade/context。
- 当前唯一 production write set 是 `:1336-1343` 的六文件，唯一 test write set 是 `:1607-1610,1651` 的 `OldFacadeRemovalContractTest`。
- `:1040-1042` 明确 `PLANNED`、`CLASSIFICATION_PENDING` 等未满足状态不可领取；额外文件必须先由父级修订计划，Worker 不得临时扩写集。
- `docs/业务逻辑.md:217-223` 要求迁移不得自行新增 TTL、验证、park/yield、retry、cleanup、fail-closed 或改变 phase/fallback/输入顺序。TURN-39 是 facade/context 收口，不是业务改写卡。

## 2. 双仓只读快照

快照时间：`2026-07-16T06:33:59-04:00`。

| Repo | Branch | HEAD | 完整 porcelain 状态计数 |
|---|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 638 行：44 tracked dirty、594 untracked、其中 1 tracked deletion |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 550 行：9 tracked dirty、541 untracked、0 tracked deletion |

- DHXY `src/main/java` 对 `CloudGameClient`、`CloudTaskServicePort`、`CloudTaskServiceExecutionContext`、`CloudTaskServiceMetadata`、`TurnGameClient`、`LegacyTaskExecutionTurnContextProvider` 均为零命中。
- Cloud 六个 TURN-39 production 文件当前全部是 `??`，不在当前 HEAD；本文只能证明 dirty/untracked 工作树快照，不能把 HEAD 当作这些文件的 baseline。
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java` 当前不存在。

| TURN-39 Cloud production file | lines | bytes | SHA-256 |
|---|---:|---:|---|
| `remote/CloudGameClient.java` | 169 | 8128 | `6C6E3610AD37163C22D8EDC0A34CA4F45C458264B3A61F9CF27DF673E904E9CE` |
| `remote/CloudTaskServicePort.java` | 328 | 15241 | `CC8E8256853BC1310D5D92F830267542FE0ECB2E733D3BB9BAA6C75B86BED3C9` |
| `remote/CloudTaskServiceExecutionContext.java` | 227 | 10597 | `A66E156FDE85BCF58FAB4330CCAFB2774A9F78214F17C63B5BD698D1D90F2599` |
| `remote/CloudTaskServiceMetadata.java` | 59 | 2747 | `A3FE6615BD0D4F571C3618EE45C679B6E28CC08891FA560B7907EDE357C91C93` |
| `turn/client/TurnGameClient.java` | 193 | 8493 | `A8F64D8DBB5F9ED2852975D518836E25AF92073F9C818D5F7E9DA7CF18056CB9` |
| `turn/client/LegacyTaskExecutionTurnContextProvider.java` | 31 | 1231 | `96827E3179054DF7878D45F9D56B7955F64DD91C25526E1B2AFEB60167008A8B` |

## 3. 权威依赖顺序 PRECHECK

权威注册表 `:1151-1163` 和波次 `:1430-1433` 给出的真实顺序是：

```text
TURN-34C + TURN-35 + TURN-36 + TURN-37
  -> TURN-38A
  -> TURN-38B1 / TURN-38B2 / TURN-38B3 / TURN-38B4
  -> TURN-38M parent classification freeze -> TURN-38C
  -> TURN-39
  -> TURN-40B
```

更精确的 predecessor ownership：

| Card | TURN-39 需要的交付 | 当前权威状态/证据 |
|---|---|---|
| TURN-34C/35/36/37 | 四个真实 Task/caller 全部只消费 turn-native API | 注册表仍为 `PLANNED`；ACTIVE_WORK 顶部显示当前仅 TURN-34A 在实现 |
| TURN-38A | 从 `TaskExecutionContext`、checkpoint/template path 清除 old retained authority 最后引用 | `:1154,1302-1312`，仍为 `PLANNED` |
| TURN-38B1 | Bag workflow/state owner rewire，并清理 `CloudBagStateOwner` old port 引用 | `:1155,1316`，仍为 `PLANNED` |
| TURN-38B2 | ReturnItem workflow/state owner rewire | `:1156,1317-1318`，仍为 `PLANNED` |
| TURN-38B3 | startup gate/direct caller rewire | `:1157,1319-1320`，仍为 `PLANNED` |
| TURN-38B4 | artifact store/configuration 脱离 old service execution context | `:1158,1321-1322`，仍为 `PLANNED` |
| TURN-38M/38C | 父级冻结五个 authority-bound state 分类和每个 KEEP 行 exact consumer/test | helper 报告存在，但权威固定分类文件 `2026-07-15-turn-38-authority-state-classification.md` 不存在；38C 仍 `CLASSIFICATION_PENDING` |
| TURN-39 | 六 production 文件汇合，active business caller 只见 turn client/context | `:1161,1336-1343,1651`，仍为 `PLANNED` |

**真实阻断 R39-DAG：** TURN-39 的全部直接前置均未形成 source-stable 交付，38C 甚至没有父级固定分类文件。按 `:1040-1042` 当前不可领取实现。

## 4. 当前 old path 与 turn path 的真实 owner

### 4.1 old retained-authority path

当前 old path 是：

```text
CloudBrainServer:86-91
  -> RemoteTaskRunRoutes.create:45-64
  -> CloudTaskRunAuthorityAssembly
  -> CloudTaskServiceExecutionContext
  -> CloudTaskServicePort + CloudGameClient
  -> TaskExecutionContext legacy delegate
  -> old business compatibility callers
```

- `RemoteTaskRunRoutes.java:50-64` 构造 coordinator、broker、action ledger、final-consumption coordinator 和 `CloudTaskRunAuthorityAssembly`，并保留旧 poll/outcome/final-consumed/task-run routes。
- `CloudTaskServiceExecutionContext.java:30-64,67-103` 是 package-private construction boundary；它创建 retained action state、`CloudTaskServicePort` 和 `CloudGameClient`。
- `CloudTaskRunAuthorityAssembly.java:206-220,288-300` 创建/恢复 `CloudTaskServiceExecutionContext`，再包装成 old `TaskExecutionContext`。
- `CloudTaskServicePort.java:17-43` 直接持有 old execution context、retained state、raw remote port、final-consumption coordinator、exclusive authority/projection；`:190-253` 保留 final-consumption mutation。
- `CloudGameClient.java:40-161` 为每个 fact/capture/input/local macro retain handle，并在非 UNKNOWN 后 final-consume。它不是 HTTPS turn facade。

### 4.2 当前 turn-native path

当前新 path 是：

```text
CloudServiceHost.create
  -> CloudServiceConfiguration component scan
  -> TaskExecutionContextHolder
  -> LegacyTaskExecutionTurnContextProvider
  -> TurnGameClient
  -> CloudTurnActionFactory + same CloudTurnCommandPort
  -> typed TurnInvocationResult / TurnLocalServiceResult
```

- `CloudServiceHost.java:39-60` 注入 exact scope、state root、同一个 `CloudTurnCommandPort` 和 template catalog，再注册 `CloudServiceConfiguration`。
- `CloudServiceConfiguration.java:27-40` 扫描 `com.bot.dhxy.service` 与 `turn.client`，并 import `TaskExecutionContextHolder`。
- `LegacyTaskExecutionTurnContextProvider.java:15-30` 是 Spring component；唯一行为是 Holder `current()` 后读取 `context.getTurnInvocationContext()`，没有 old scope fallback、cache 或 window fallback。
- `TurnGameClient.java:27-61` 是 Spring component；`:73-84` 提供 exact-context bound view；`:95-169` 的每次 public action 创建一个 UUID、提交一个 command；`:171-180` 在 port 前检查 bound context。
- 当前生产树没有 `CloudServiceHost.create(...)` caller；真实 Task runtime/factory 属于 TURN-40B/C。因此新 path 的 API 已存在并被业务源码引用，但尚未由最终 runtime 激活。

协议约束与此一致：协议 `:20-30` 要求 outcome 不得被静默转成 success/retry，`:108-126` 规定无 local business retry、无 durable workflow/session/ledger，`:151-157` 规定 uncertain action 不得重执行，`:335-356` 固定 Cloud 业务与 DHXY mechanics 边界。

## 5. 四个 old 类型的全部生产引用

以下清单以 Cloud `src/main/java` 为根，排除目标文件自身的声明命中，但保留 Javadoc-only 命中并明确标注。

### 5.1 `CloudGameClient`

真实构造 owner：`CloudTaskServiceExecutionContext.java:27,64,103,183-185`。

全部外部生产引用：

1. `com/bot/dhxy/input/InputSequences.java:5,39,46,63`：真实字段/构造器/调用，`executeInputBundle` 仍走 old client。
2. `com/bot/dhxy/runner/context/TaskExecutionContext.java:10,319-320`：old public `getGameClient()`。
3. `remote/CloudTaskServiceExecutionContext.java:27,64,103,183-185`：构造并公开 old client。

当前直接 active 调用还包括 `NavigationService.java:533-569`：`navigateInCurrentMap` 通过 `taskContext.getGameClient().executeLocalMacro(...)` 走 old path。该 caller 文件属于 TURN-27，不在 TURN-39 写集。

### 5.2 `CloudTaskServicePort`

真实构造 owner：`CloudTaskServiceExecutionContext.java:26,61-63,100-102,178-180`。

全部外部生产引用：

1. `TaskExecutionContext.java:13,331-332`：old public `getRemoteGameClient()`。
2. `CloudBagStateOwner.java:4,617,623`：仅 Javadoc/type-name 引用，归 TURN-38B1 清理。
3. `CloudGameClient.java:17,22,45-50,75-81,107-114,145-153`：真实 action handle/final-consumption 调用。
4. `CloudTaskExclusiveInteractionAuthority.java:401,442,468,490-492,633,1094`：old SCC 的 action/final mutation 类型。
5. `CloudTaskRetainedActionState.java:62-86,247-268,273-555`：old SCC 的 action handle mint/renew/retain owner。
6. `CloudTaskRunCommandExecutor.java:24`：仅 Javadoc 引用。
7. `CloudTaskServiceExecutionContext.java:15,26,61,100,178-180`：真实 construction/exposure。
8. `RemoteFinalConsumptionCoordinator.java:26-49,144-187`：old final-consumption SCC。
9. `RemoteGameClientPort.java:9`：仅 Javadoc 引用。

其中 4、5、8 属于待 TURN-44A/45B 按 manifest 原子删除的 old authority/final-consumption SCC；TURN-39 不能越过删除波次改写或拆坏该 SCC。

### 5.3 `CloudTaskServiceExecutionContext`

真实 construction/lifecycle owner：`CloudTaskRunAuthorityAssembly.java:191-220,279-300,398-455`。

全部外部生产引用：

1. `TaskExecutionContext.java:11,35,51,442-446`：legacy delegate、legacy constructor 与 helper。
2. `host/CloudArtifactStore.java:3,12,30,37,44`：artifact API 直接以 old context 授权，归 TURN-38B4。
3. `host/ScopedPngArtifactStore.java:3,62,153,174,197,209`：artifact implementation 直接读取 old context，归 TURN-38B4。
4. `remote/CloudTaskRunAuthorityAssembly.java:204-220,288-300,398-439`：old SCC 构造/持有，晚删归 TURN-44A。

### 5.4 `CloudTaskServiceMetadata`

当前它本身是 powerless record，不是 authority；`CloudTaskServiceMetadata.java:29-50` 持有 task/requested-task、role/team、retry policy、startup mode 和 startedAt。

全部外部生产引用：

1. `TaskExecutionContext.java:12,39,65,78-80,96-109,454-455`：turn-native path 仍直接以 old remote metadata 作为字段和 factory 参数。
2. `CloudTaskRunAuthorityAssembly.java:191-194,398-455`：old runtime initial metadata owner。
3. `CloudTaskRunRetainedLifecycleActivationAdapter.java:61-64,468-481`：old resume/activation state 保存 initial metadata。
4. `CloudTaskServiceExecutionContext.java:28,39,51-54,77,89-92,173-175`：old context 字段/校验/exposure。

生产源码中没有 `new CloudTaskServiceMetadata(...)`；当前没有最终 Task factory。现有创建都在测试 fixtures 中。

## 6. `TurnGameClient` / Legacy provider 的真实生产引用

### 6.1 `TurnGameClient` direct type sites

除定义自身外，共 15 个 production 文件直接引用类型：

- `com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `com/bot/dhxy/service/BattleRadarService.java`
- `com/bot/dhxy/service/CommonBoxService.java`
- `com/bot/dhxy/service/SummonSkillService.java`
- `com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java`
- `com/bot/dhxy/service/playerstate/CloudPlayerStateFirstAidPort.java`
- `com/bot/dhxy/service/playerstate/CloudPlayerStateIncenseStatusPort.java`
- `remote/CloudCommonBoxPortAssembly.java`
- `remote/CloudDialogDetectionPort.java`
- `remote/CloudDialogPreparedActionValidationPort.java`
- `remote/CloudTeamReturnPortAssembly.java`
- `turn/client/CloudBagLocalServiceClient.java`
- `turn/client/CloudGiveItemLocalServiceClient.java`
- `turn/client/CloudQuestLocalServiceClient.java`
- `turn/client/CloudUiCleanerLocalServiceClient.java`

此外，不声明局部 `TurnGameClient` 类型而通过 context getter 调用的 active sites 包括：

- `AutoCombatPanelService.java:543,595,680`
- `ClientIdentityService.java:77`
- `TaskTrackerPanelService.java:530-531,609`
- `CloudDialogDetectionPort.java:81`
- `CloudDialogPreparedActionValidationPort.java:76`
- `BattleRadarService.java:536`
- `CommonBoxService.java:466`
- `SummonSkillService.java:881`
- 两个 PlayerState port 的 `:115/:154`

这证明早期 TURN-13G/13C facade 已成为迁移目标，不需要 TURN-39 再创建第二套 `TurnTaskServicePort`。

### 6.2 `LegacyTaskExecutionTurnContextProvider`

- 目标 symbol 的 direct production 命中只有其定义文件自身；这是 Spring interface injection 的正常结果，不等于没有 runtime role。
- `CloudServiceConfiguration.java:27-40` 扫描 `turn.client`，使该 `@Component` 与 `TurnGameClient` 一同进入 host context。
- 它只从 `TaskExecutionContextHolder` 投影 `TurnInvocationContext`。TURN-39 若保留这个文件，必须继续保持 Holder-only、no-cache、no-fallback；若改变其职责，父级必须先解释为何仍叫 Legacy 且不创建第二 provider。

## 7. 写集外 active reference 与 Metadata 阻断

### 7.1 `InputSequences` 是未归属的 old-client compatibility file

`com/bot/dhxy/input/InputSequences.java` 当前：

- `:5,39,46` 直接绑定 `CloudGameClient`；`:61-65` 每次 input bundle 走 old `executeInputBundle`。
- 文件到 `:158` 结束，只提供 old-client API；它不是 TURN-39 六文件之一。
- 真实 caller 仍有 `DialogService`、`NavigationService`、`NpcClickService`、`WubeiTask`、`FiveRingTaskV2`、`XiuluoTaskV2`，且不是只 import：这些文件存在大量 `inputSequences.*` 调用。
- 其中 `NavigationService`/`NpcClickService` 分别归 TURN-27/28，三个 whole Task 分别归 TURN-35/36/37；Dialog caller 由 TURN-25/26 路径收口。
- 部分 caller 还调用该 Cloud compatibility class 根本不存在的 `submitExclusiveAndWait(...)`，证明当前树是跨卡中间态，不能作为 TURN-39 可实现 baseline。

权威计划没有给 TURN-39 或其它已冻结 production 卡写 `InputSequences.java` 的权限。前置卡可以消除它的所有 caller，但文件自身仍会含 `CloudGameClient` token。父级必须冻结以下二选一边界，本文不代选：

1. 它在 TURN-39 时已为零 caller 的 inert compatibility file，`OldFacadeRemovalContractTest` 明确不把它算 active，并把它交给后续 manifest 精确删除；或
2. 它必须在 TURN-39 前移除/改写，则父级先补 exact owner/write set，不能让 TURN-39 Worker 越界修改。

### 7.2 `TaskExecutionContext` 仍是四个 old 类型的汇合点

当前 `TaskExecutionContext.java`：

- `:35-41` 同时保存 old delegate、old metadata 与 new `TurnGameClient`。
- `:51-58` 保留 old `CloudTaskServiceExecutionContext` constructor。
- `:96-109` 的 turn-native factory 仍要求 `CloudTaskServiceMetadata`。
- `:204-225,314-352` 仍公开 old scope/revision/client/service-port/pending APIs。
- `:442-455` 以 `legacyDelegate()` 和 old metadata helper 维持双路径。

TURN-13C 固定卡 `reports/2026-07-15-turn-card-TURN-13C.md:42-69` 明确这些旧签名当时必须保留，并把 late removal 留给 TURN-38A。TURN-39 不拥有 `TaskExecutionContext.java`，所以 38A 必须先交付最终无 old-facade active dependency 的 context surface。

### 7.3 Metadata 的业务字段没有 final authority source

`TaskExecutionContext` 的 metadata getters 不是死 API。当前 production 调用至少包括：

- `getTaskCode()` 15 处；
- `getRequestedTaskCode()` 15 处；
- `getWindowRole()` 16 处；
- `getLocalTeamSessionKey()` 20 处；
- `getLocalLeaderWindowId()` 7 处；
- `isLocalLeaderPresent()` 7 处；
- `isLocalSupportMember()` 4 处；
- `getRetryPolicy()` 4 处。

但协议只提供：

- `TurnWindowMetadata` 的 device/window/title/HWND/process/rect/pause/stop，见协议 `:75-81`；
- start request 的 stable request id、ordered task codes 与 failure policy，见 `:115-126`。

协议没有 role、local team session、leader window、support flag、old retry policy 或 startedAt authority。`docs/业务逻辑.md:10-22,47-65` 又明确禁止自动把后加窗口附着到已有 local-team session、猜 leader 或默认动态组队。

**真实阻断 R39-METADATA：** 父级尚未冻结 38A 后这些仍被业务读取的字段来自哪里。TURN-39 不能在自己的六文件内猜默认值，也不能修改协议、Task/context caller 或 40B runtime。若 final context 不再需要这些字段，必须由各 predecessor 以业务基线证据消除 caller；若仍需要，则父级必须交付 powerless、exact、可由 40B 构造的 authority source。

## 8. TURN-39 exact write set PRECHECK

### 8.1 唯一 production write set

相对 `D:/mavenProject/dhxy-cloud-brain/src/main/java`：

1. `com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
2. `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`
3. `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`
4. `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java`
5. `com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`
6. `com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`

### 8.2 唯一 named test write set

相对 `D:/mavenProject/dhxy-cloud-brain/src/test/java`：

7. `com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java`，当前不存在，允许由 TURN-39 创建。

计划 `:1478-1499,1607-1610,1651` 给它 `STATE` profile，预期命令是：

```text
mvn -q -Dtest=OldFacadeRemovalContractTest test
```

本 helper 按用户约束未运行该命令。

除以上七文件外，TURN-39 Worker 一律只读。尤其不得自行修改 `TaskExecutionContext.java`、`InputSequences.java`、已有 TURN-13G/13C tests、old authority SCC、CloudBrainServer/routes、protocol/POM 或三大 Task。

## 9. Named test 边界候选，不冻结

计划 `:1651` 同时使用“所有 active caller”与“old facade 零引用”。由于 old SCC 必须保留到 TURN-44A，这两个短语不能解释成“Cloud 全部 production source 对四个名称零 token”。父级必须在派发 TURN-39 前冻结精确扫描集合。

### 9.1 应纳入的 active roots 候选

至少应逐文件覆盖：

1. `com/bot/dhxy/task/**` 的四个最终 Task。
2. `com/bot/dhxy/service/**` 的 active Cloud business Services。
3. `com/bot/dhxy/runner/context/TaskExecutionContext.java` 及 holder-facing public surface。
4. `turn/client/**` 的 `TurnGameClient`、Legacy provider 与四个 local-Service typed clients。
5. 位于 `remote/` 但实际仍是 active migrated business adapter/assembly 的文件，例如 CommonBox、Dialog、TeamReturn ports；不能用“排除整个 remote 包”的宽泛 guard 隐藏它们。
6. TURN-40B future runtime/factory 在领取前还应自行证明零 old facade reference；TURN-39 test 不应假设尚不存在的文件已经通过。

active roots 中建议禁止：

- type tokens `CloudGameClient`、`CloudTaskServicePort`、`CloudTaskServiceExecutionContext`；
- old authority methods `getGameClient()`、`getRemoteGameClient()`、`legacyDelegate(...)`；
- retained/final-consumption tokens作为 active business dependency；
- 新建第二个 `TurnGameClient`、第二 command port/exchange、session/owner/ledger、隐式 retry。

`CloudTaskServiceMetadata` 是否也属于禁止 token，必须在 R39-METADATA 裁决后冻结；当前不能一边让 active `TaskExecutionContext.turnNative(...)` 依赖它，一边声明 active roots 对它零引用。

### 9.2 必须显式排除而不能误判为 active 的 old SCC 候选

全仓当前仍有这些合法的晚删引用：

- `CloudTaskRunAuthorityAssembly.java`
- `CloudTaskRunRetainedLifecycleActivationAdapter.java`
- `CloudTaskRetainedActionState.java`
- `CloudTaskExclusiveInteractionAuthority.java`
- `RemoteFinalConsumptionCoordinator.java`
- `RemoteGameClientPort.java`
- `CloudTaskRunCommandExecutor.java`
- TURN-39 自己修改但仍可能作为 compatibility SCC 壳存在的四个 old target 文件

这些文件由权威计划 `:1169-1175,1385-1403` 留给 `TURN-44M45M -> TURN-45A -> TURN-44A -> TURN-45B`。如果 named test 不使用 explicit allowlist，而是全仓禁止 token，TURN-39 在不越界删除 SCC 的前提下必然无法通过。

每个 exclusion 必须写明后续 manifest owner，不能只排除目录或前缀。`InputSequences.java` 不在上述 old SCC 清单中，必须由父级另行裁决，不能顺手塞入 exclusion。

### 9.3 正向 contract 候选

named test 除 source scan 外，至少应证明：

1. `LegacyTaskExecutionTurnContextProvider.currentContext()` 只走 Holder -> `getTurnInvocationContext()`，无 scope/window fallback。
2. active context/client path 只暴露 `TurnGameClient` typed capture/execute/localService/latest metadata。
3. 不通过反射/string guard 假装运行语义；一次 UUID、一次 command、uncertain 不重试等行为继续由已存在的 `TurnGameClientContractTest` 保证。
4. old SCC 仍可 source-compile，不因 TURN-39 在 SCC 中制造半删除假编译点；真正删除留给 44A。

### 9.4 现有 test-source ownership 冲突

- `TaskExecutionContextTurnContractTest.java:494-530` 当前明确反射断言 old constructor、`getGameClient()`、`getRemoteGameClient()` 仍存在，并断言 turn-native 调用 fail-closed。
- TURN-38A 的唯一新 test 是 `runner/context/TaskExecutionContextOldAuthorityRemovalTest`，当前不存在；计划没有明确把上述既有 13C test 加入 38A test write set。
- Cloud test tree 中 `CloudTaskServiceMetadata` 被 17 个 test class 直接用作 fixture；若 TURN-39 删除/改签名，test compile 会在 named test 运行前失败。
- `TurnGameClientContractTest` 已锁定 one UUID/one command/exact frame/bound context；TURN-39 的唯一 test write set不包含修改它。

**真实阻断 R39-TEST-OWNERSHIP：** 父级必须先冻结“保留 source-compatible API”或“扩写 predecessor/test owner”的路线。TURN-39 Worker 不能修改唯一 test write set之外的既有 tests，也不能用只运行 named class 掩盖 Maven test-compile 对全 test source 的编译。

## 10. 真实阻断条件汇总

1. **R39-DAG**：TURN-34C/35/36/37、38A、38B1-B4、38C 均未 source-stable；38C 固定分类文件不存在。
2. **R39-ACTIVE-REF**：`NavigationService.getGameClient()`、`TaskExecutionContext` 双路径、`InputSequences` 及其六类 caller 当前仍是真实 old-client production references。
3. **R39-WRITE-SET**：`InputSequences.java` 含 old client 但不在 TURN-39 写集，也未被当前权威卡明确分配修改/删除 owner。
4. **R39-ZERO-BOUNDARY**：44A/45B 前必须保留 old authority/final-consumption SCC；全仓字面零引用与删除波次矛盾，named test 的 active roots/exclusions 尚未冻结。
5. **R39-METADATA**：role/team/retry/startup metadata 仍有大量 active consumers，但最小 HTTPS request/window metadata 没有这些 authority fields；不得猜默认值。
6. **R39-TEST-OWNERSHIP**：既有 13C test 锁住 old public surface，17 个 tests 使用 old metadata；当前唯一 TURN-39 named test无法自行修复这些 test-source contracts。
7. **R39-SNAPSHOT**：六个目标 production 文件全部 untracked；任何 predecessor/并行 writer 变化都会使本文 hash/ref 失效，派发前必须重扫。

这些是开工前事实，不是对 TURN-39 最终实现质量的判决。

## 11. 父级派发前必须冻结的事项

1. 等全部直接 predecessor true EOF/source-stable 后，重跑六 symbol production/test refs 与六目标 SHA。
2. 冻结 `active caller` 的 exact file allowlist，以及每个 old SCC exclusion 对应的 44A/45B manifest owner。
3. 冻结 `InputSequences.java` 是 inert compatibility + 后续删除，还是由新增 predecessor 精确改写；不得交给 TURN-39 顺手处理。
4. 冻结 final `TaskExecutionContext` 是否彻底移除 old constructor/getters/type imports，并同步解决现有 13C test ownership。
5. 冻结 role/team/retry/startup metadata 的 powerless authority source，或提供 predecessor 消除全部真实 consumer 的证据。
6. 冻结 `CloudTaskServiceMetadata` 的兼容策略，使 production zero-ref目标与 17 个现有 test fixtures 可同时成立。
7. 保持 TURN-39 exact write set 为六 production + 一个 named test；若任何裁决要求第八文件，先由父级修订权威计划。
8. 继续保持协议 `:108-126,151-157,335-356`：无第二 facade/port/exchange，无 old broker identity/final-consumed business concept，无自动 retry/session/owner/ledger。
9. 继续保持 `docs/业务逻辑.md` 的 phase、次数、delay、fallback、park、pause/stop 与 local-team 边界；本卡没有业务变化授权。

## 12. 本 helper 验证边界

- 已完整读取指定文档、两仓 status、六个目标源码、`TaskExecutionContext`、Holder、Cloud `InputSequences`、当前 old route/assembly/host/configuration、相关 production refs 与 existing turn/context tests。
- 已做只读 `rg`、`Get-Content`、`Get-FileHash`、`git status`、`git rev-parse`/branch 查询。
- 未运行测试或构建；这不是遗漏，而是用户和 ACTIVE_WORK 的明确边界。
- 未保护性修改或清理任何 dirty/untracked 文件。

<!-- TRUE_EOF: CR271 TURN-39 READINESS PREFLIGHT REAL_BLOCKER_CONFIRMED -->
