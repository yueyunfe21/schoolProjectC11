# CR271 TURN-42M/43M DHXY deletion-manifest PRECHECK

## PRECHECK

- 观察时点：`2026-07-16T06:20:09.827-04:00`。本报告是非绑定、只读预检，不是最终 deletion manifest，也不授权源码删除。
- 已完整读取：仓库 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/PACKAGE_ARCHITECTURE.md` 的 CR271 当前卡、权威计划第 14-19 节、HTTPS turn 协议设计、
  `docs/业务逻辑.md`，以及两轮 mutex/delete audit 的删除清单规则。
- 业务基线仍是权威计划指定的 pre-cloud 行为基线；五倍/修罗涉及的默认参考提交为
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。本预检不引入任务 phase、重试、fallback、验证次数、
  park/keep-turn 或 expiry 差异。
- DHXY：分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；没有配置 upstream，
  本地也没有 `origin/thin-client-design` remote-tracking ref。本次未 fetch。工作树为 `D=1 / M=43 / ??=588 / total=632`，
  全部现有 dirty/untracked 均按原字节保护。
- Cloud：分支 `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；没有配置 upstream，
  本地也没有 `origin/navigation-migration` remote-tracking ref。本次未 fetch。工作树为 `M=9 / ??=541 / total=550`，
  全部现有 dirty/untracked 均按原字节保护。
- DHXY 旧包 `src/main/java/com/bot/dhxy/cloud/remote/` 当前有 `129` 个 Java 文件、合计 `921275` bytes，
  `129/129` 全是 `??`。因此它们没有当前 HEAD blob 可作为删除基线；未来 manifest 必须逐文件在冻结时现算
  byte size 与 SHA-256，不能把本报告的哨兵值当删除凭证。
- 本次没有执行 Maven、JUnit、compile、runtime、application/server、Task、UI、capture 或 input；也没有任何
  Git mutation。

### 当前 source graph

1. 新 HTTPS turn 图：
   `TurnConfiguration -> HttpsTurnClient / TurnLoopFactory / TurnLoopRegistry / TurnModeGuard`；
   `WindowTurnLoop -> LocalTurnActionExecutor -> LocalServiceStepDispatcher -> 四个 local operation executor -> 四个永久本地 Service`。
   `WindowTaskControlService` 当前只调用 `TurnModeGuard.startLocal(...)`；全生产源码对
   `TurnModeGuard.startRemote(...)` 的调用数为 `0`。
2. 新 turn 图对 `com.bot.dhxy.cloud.remote` 的生产 import、构造和反射式配置引用数为 `0`。
3. 旧 remote 主图：
   `RemoteTaskRunLifecycleService -> RemoteCommandPollingLoop + RemoteTaskRunRegistry + RemoteTaskRunApiClient`；
   `RemoteCommandPollingLoop -> RemoteCommandTransport + RemoteCommandHandler + RemoteTaskRunRegistry + RemoteOperationLedger + RemoteTaskRunApiClient`；
   `LocalRemoteGameCommandHandler -> RemoteTaskRunRegistry + RemoteOperationLedger + RemoteOperationPayloadCodec + 旧 DTO/fact/macro + 本地 mechanics/Service`；
   `RemoteTaskRunRegistry <-> RemoteOperationLedger` 是双向 SCC；
   `HttpRemoteCommandTransport -> RemoteCommandTransport + RemoteCommandTransportException + 旧 wire DTO`。
4. 旧 remote 包没有 Spring `@Component/@Service/@Configuration/@Bean`；也没有发现生产源码中的
   `new LocalRemoteGameCommandHandler(...)`、`new RemoteCommandPollingLoop(...)`、
   `new RemoteTaskRunLifecycleService(...)`、`new HttpRemoteCommandTransport(...)` 或
   `new HttpRemoteTaskRunApiClient(...)`。
5. `LeaderPrecheckMechanics` 与 `BoundLeaderPrecheckCaptureCapability` 对旧 handler/lifecycle/registry 的部分命中是
   Javadoc 文本，不是当前编译入边。后续 manifest 必须同时记录“编译引用”和“注释/设计引用”，不能把二者混算。

## 依赖

| 目标 | 权威前置 | 当前预检事实 |
| --- | --- | --- |
| `TURN-42M` | `TURN-41` | `TURN-41` 需要 40B/40C/40D 双构建与用户运行证据；当前 `startRemote(...)` 仍无生产 caller。 |
| `TURN-43M` | `TURN-41 + TURN-35 + TURN-36 + TURN-37` | 三大 Task turn caller 与运行证据必须先闭合；当前快照不能冻结最终 mechanics/DTO 零引用图。 |
| `TURN-43A` | `TURN-43M` 父 manifest freeze | 先断开 old handler/lifecycle 的消费者；不得顺带删除四个永久本地 Service 或 NPC reference/shadow。 |
| `TURN-42A` | `TURN-42M + TURN-43A` 父 manifest freeze | 43A 后重新扫描，再处理 transport/poller/lifecycle/registry/ledger SCC。 |
| `TURN-43B` | `TURN-42A + TURN-43M` 父 manifest freeze | 只接收当时生产引用为零、SHA 未漂移的 DTO/codec/fact/macro 行。 |

- 固定物理顺序是 `43A -> 42A -> 43B`。43A 未完成时，42A 不能先移除 handler 所依赖的 registry/ledger；
  42A 未完成时，43B 不能凭当前 token 搜索提前清 DTO。
- `TURN-42M/43M` 本身只形成父 manifest；任何当前 caller、文件内容、byte size 或 SHA-256 在 freeze 前变化，
  都要求父级重跑完整生产引用图并重新生成对应行。

## 真实 caller

### 旧 remote 包外的生产入边

全 `src/main` 扫描后，旧 remote 包外只有以下四条 import，落到三种旧类型：

| 旧类型 | 当前包外生产 caller | 预检分类 |
| --- | --- | --- |
| `RemoteAutoCombatPanelFact` | `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java:4,85,90-115` | `REWIRE`；即使 `probeAutoCombatPanelFact()` 暂无调用者，方法签名和方法体仍是生产引用。 |
| `RemoteLeftTopStatusFact` | `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java:5,150-161`；其方法又被 `LocalRemoteGameCommandHandler.java:822` 调用 | `REWIRE`；43A 去掉 handler 后，Service 自身引用仍须另行移除。 |
| `RemoteCoordinateSpace` | `AutoCombatPanelService.java:5,98,115` 与 `LeftTopStatusSwitchService.java:6,161` | `REWIRE`；两个 caller 均消失或改型后才能重新判断。 |

- 未发现包外对 `RemoteGameCommand`、`RemoteCommandHandler`、`RemoteCommandTransport`、
  `RemoteTaskRunApiClient`、`RemoteTaskRunLifecycleService`、`RemoteTaskRunRegistry` 或
  `RemoteOperationLedger` 的生产引用。
- `LocalRemoteGameCommandHandler` 没有包外构造/Bean caller；`LeaderPrecheckMechanics` 与
  `BoundLeaderPrecheckCaptureCapability` 中的 handler/registry 命中只存在于注释。
- `RemoteCommandPollingLoop` 的真实 caller 是 `RemoteTaskRunLifecycleService.java:25,35,47`；
  `RemoteTaskRunRegistry` 的真实 callers 是 handler、polling loop、lifecycle 与 ledger；
  `RemoteOperationLedger` 的真实 callers 是 handler、polling loop 与 registry。
- `RemoteCommandTransport` 的真实 callers 是 `HttpRemoteCommandTransport` 与 `RemoteCommandPollingLoop`；
  `RemoteCommandTransportException` 的真实 callers 是 transport 实现与 polling loop。

### 四个永久本地 Service

| Service | 新 turn caller | 其它当前真实 caller 摘要 | 预检分类 |
| --- | --- | --- | --- |
| `BagService` | `cloud/turn/local/BagLocalOperationExecutor.java:8,21,24` | `GiveItemService.java:26,54,75`、`PlayerStateService.java:69,633`、`ReturnItemPrescanService.java:35,252`、五倍/五环/修罗 Task | `KEEP` |
| `UICleanerService` | `cloud/turn/local/UiLocalOperationExecutor.java:7,18,22` | `AutoCombatService.java:41`、`NavigationService.java:182`、`SummonSkillService.java:58`、窗口 startup/runner/manager、五倍/五环/修罗 Task | `KEEP` |
| `GiveItemService` | `cloud/turn/local/GiveItemLocalOperationExecutor.java:6,17,20,54` | `DialogService.java:72`；内部结果类型仍被新 adapter 使用 | `KEEP` |
| `QuestManagerService` | `cloud/turn/local/QuestLocalOperationExecutor.java:10,22,26` | `XiuluoTaskV2.java:75,335` | `KEEP` |

- `LocalServiceStepDispatcher.java:3-6,18-27` 聚合四个 adapter，`LocalTurnActionExecutor.java:32,41`
  持有 dispatcher。这是四个 Service 在新 turn 链中的真实保留入口。
- 旧 handler 对 `BagService`、`UICleanerService` 的出边会随 43A 消失，但这不改变上述 Service 的 `KEEP` 分类；
  它们的 nested model/result、已批准本地机械 API 和 adapter 也必须一并保护。

### NPC reference/shadow 与当前生产链

- `docs/业务逻辑.md` 的 NPC Click 定稿明确要求旧本地 pipeline 物理保留为 shadow/reference/testcase replay 依据，
  即使生产不允许 full fallback，也不能用“零生产 caller”推导删除。
- 当前生产入口是 `NpcClickService -> NpcClickSmartCloudDecisionService` 的 FIFO consumer；
  `NpcClickService.java:3-9,217-500` 仍负责 exact-window 安全壳、真实输入和 verifier。
- `service/npc/NpcClick*Local*Mechanics.java`、其共享的
  `service/dialog/DialogDetectionLocalMechanics.java` 与
  `service/battleradar/BattleRadarLocalObservationMechanics.java` 属于 reference/shadow 保护面。
  后两者还被 `NpcClickCtrlProbeLocalMacroMechanics.java:74-83,368-488` 真实引用。
- 因此，上述 NPC reference/shadow 文件即使当前没有普通生产入口，也属于 `KEEP`；不得放入 43A/43B 候选集。

## KEEP / REWIRE / 候选 DELETE

以下只是当前图的 cohort 预分类，不是最终删除清单。

### KEEP

- `src/main/java/com/bot/dhxy/cloud/turn/**` 新 HTTPS turn transport/protocol/executor/loop/adapter 图。
- `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService`，连同其新 local operation adapter、
  当前 result/model 和安全边界。
- `NpcClickService` 当前生产安全壳，以及 `docs/业务逻辑.md` 指定的 NPC reference/shadow pipeline 与其必要共享 mechanics。
- 任何仍被 Task、Service、window runtime 或新 turn 图真实引用的 model/config/source。

### REWIRE

- `AutoCombatPanelService` 与 `LeftTopStatusSwitchService`：两者均有当前 Service/caller 图，且仍携带旧 remote fact API；
  先由上游 caller 收口卡移除旧 public API，再重新扫描。
- `RemoteAutoCombatPanelFact`、`RemoteLeftTopStatusFact`、`RemoteCoordinateSpace`：存在上述生产源码入边，当前不能进入
  候选 DELETE。未来 manifest 必须为每条入边写明具体移除卡，不能写模糊的“后续清理”。
- 任何旧 DTO/codec/fact/macro，只要未来扫描仍有 retained Service、Task、new turn 或 NPC reference/shadow caller，
  一律维持 `REWIRE` 或 `KEEP`。

### 候选 DELETE

- `43A` 候选 cohort：`LocalRemoteGameCommandHandler`；计划点名的
  `LeaderPrecheckMechanics`、`BoundLeaderPrecheckCaptureCapability`；以及仅服务于旧 handler/lifecycle 的专用 mechanics。
  其中任何 mechanics 若同时属于 NPC reference/shadow 或仍有其它 caller，立即退出候选 cohort。
- `42A` 候选 cohort：`HttpRemoteCommandTransport`、`RemoteCommandTransport`、
  `RemoteCommandTransportException`、`RemoteCommandPollingLoop`、`HttpRemoteTaskRunApiClient`、
  `RemoteTaskRunLifecycleService`、`RemoteTaskRunRegistry`、`RemoteOperationLedger` 及同一旧 SCC 的必要 provider/receipt/registration 类型。
  这是 cohort 观察，不是逐文件删除授权。
- `43B` 不在本报告点名文件；它只能从 43M 冻结行中筛出 42A 后仍存在且生产引用为零的 DTO/codec/fact/macro。
  `RemoteAutoCombatPanelFact`、`RemoteLeftTopStatusFact`、`RemoteCoordinateSpace` 当前明确不属于该集合。

## 精确写集

### 本次唯一写集

- `docs/superpowers/plans/reports/2026-07-16-turn-42M-43M-readiness-preflight-helper.md`

本次对 Java、test、主计划、`ACTIVE_WORK.md`、CR271、矩阵和 dashboard 的写集均为零。

### 未来父 manifest 固定报告

- `TURN-42M`：`docs/superpowers/plans/reports/2026-07-15-turn-42-dhxy-transport-delete-manifest.md`
- `TURN-43M`：`docs/superpowers/plans/reports/2026-07-15-turn-43-dhxy-handler-mechanics-delete-manifest.md`

两份未来报告每一行必须具备：

1. 精确 repo-relative path，不使用目录 wildcard。
2. 文件内 primary symbol；多 symbol 文件逐一列明。
3. `KEEP`、`REWIRE` 或 `DELETE`，三选一。
4. 当时全部 production reference，精确到 path 与 line；同时区分编译引用、反射/config 引用和仅注释命中。
5. 移除/改写每一条引用的具体 card；一条引用不能只写笼统阶段名。
6. manifest 冻结时现算的 byte size 与完整 SHA-256。
7. 删除前置、所在 `43A/42A/43B` cohort、同 cohort compile 门。
8. 四个永久本地 Service 及其 model/adapter 的保护原因；NPC reference/shadow 的保护原因。

未来源码写集只能等于父 manifest 中 SHA 未漂移的精确 `DELETE` 行，加上该 cohort 冻结的唯一 source-guard 文件；
不得以 `cloud/remote/**`、`service/**` 或符号前缀扩张。计划当前只冻结 guard 文件名，没有冻结其 repo-relative path，
后续不得在本报告中猜路径。

### SHA/size 漂移哨兵

下表只是当前图的漂移哨兵，不是 manifest 行。任何上游编辑、caller 变化或行号变化后，父 manifest 必须对其全部行重算；
旧 remote 包其余 `115` 个文件也必须在 freeze 时逐文件首次计算。

| Git | Bytes | SHA-256 | Path |
| --- | ---: | --- | --- |
| `??` | 202112 | `b1cd28fa03f1f933e92bb037c09ba1e2922635149d32d4482637b1cd313bcdfc` | `cloud/remote/LocalRemoteGameCommandHandler.java` |
| `??` | 11454 | `acab2dff9025c1c491f7e18d8fe456fd4f9396dc66422a55ea75d2d395f2ea7a` | `cloud/remote/LeaderPrecheckMechanics.java` |
| `??` | 3823 | `db0cb72793c7b9b43f5f2ef70a8fdb8fec83c871733ea57d92595858e31aef45` | `cloud/remote/BoundLeaderPrecheckCaptureCapability.java` |
| `??` | 16588 | `577bea2e16a336993570f7ec4e5961e7f5aba6b7311f245a661a668b6a511a83` | `cloud/remote/RemoteCommandPollingLoop.java` |
| `??` | 126648 | `4b80aa5301b84f79d468d4e58c02b12b55a3bdc94285d0092d9bf263d2ec3781` | `cloud/remote/RemoteTaskRunLifecycleService.java` |
| `??` | 82149 | `8cc8089af75e4a3a9cddf83aca09fbe7441c4ec21c7860c4e5b3947952f05bb9` | `cloud/remote/RemoteTaskRunRegistry.java` |
| `??` | 45323 | `ab4e1a1782d9fe07b8a63370f40e6f2e4d0d45eafbd46fa12d54ffa919738bcd` | `cloud/remote/RemoteOperationLedger.java` |
| `??` | 30408 | `b6a93abfa5e7e48e3352a057a9771550ca2d53fe90b410e89b5068893503c445` | `cloud/remote/HttpRemoteCommandTransport.java` |
| `??` | 1152 | `dd6b237225c1496920f1e312f43e0b996452bb5849b6f9879fc65b5361a2f2c6` | `cloud/remote/RemoteCommandTransport.java` |
| `??` | 1216 | `fa4c09d8f1a836e931a4fd83b8a7041ff563f76ce3619d9ec0068fc87e017360` | `cloud/remote/RemoteCommandTransportException.java` |
| `??` | 38063 | `0ac1368d5ffb227eaefdd30c6c6c37debe800327ebd930358ef5969147ab1c15` | `cloud/remote/HttpRemoteTaskRunApiClient.java` |
| `??` | 2999 | `0e24ba0aa80c34425b362c4ccf123f6d781152676c794c492a22c4bc1eedc3f0` | `cloud/remote/RemoteAutoCombatPanelFact.java` |
| `??` | 440 | `1f22beed927675de5426ee4bf9e302c4a466ebee918fd7e27773ce9aa8954d63` | `cloud/remote/RemoteLeftTopStatusFact.java` |
| `??` | 119 | `70f307cb76d5edb5f60d7e5457eec03d4f9ac5286d7059d5db3590571c921ac9` | `cloud/remote/RemoteCoordinateSpace.java` |
| `M` | 68988 | `154d1a7fbd7cf0d7ca9c51b9eeccd63aca94fcc62779ecfaebf0dcee87cfba44` | `service/BagService.java` |
| clean | 16895 | `a3507f0b54506a0715599e965b5a5a20993bc66bc3ed987e5510000a1e95a947` | `service/UICleanerService.java` |
| `M` | 6630 | `a08736885e1f48a3eee6003c2a5863de2b769d9f9e5255c3536a58c4632655ab` | `service/GiveItemService.java` |
| `M` | 16014 | `7aa92acc824b72443819ec6f7d1d019e49f7774cc9ba4271591cd5693765291a` | `service/QuestManagerService.java` |
| `M` | 19157 | `e8439e1ee2c38892834477c1577e6e46fdfe3769a79449536f8463161e69864e` | `service/AutoCombatPanelService.java` |
| `M` | 14853 | `6ac4cb59d82126be606b519371f819166ffd9a1d3f063f477940d701354b977a` | `service/LeftTopStatusSwitchService.java` |
| `M` | 75684 | `c0bf8d4df24369a5b59b04bffade7835847287bf3ba26af108f9c09e76e6da52` | `service/NpcClickService.java` |
| `??` | 25698 | `7e6b2af1ae01400ae43e925cdf44ab11326da93dccda97cda7e3485c6cf431ac` | `service/npc/NpcClickCtrlProbeLocalMacroMechanics.java` |
| `??` | 18035 | `a3d3df4c37c573c3c7962ce409cd39fc190be5cb8ec7263ebefbbe4ffb12e806` | `service/battleradar/BattleRadarLocalObservationMechanics.java` |
| `??` | 22614 | `02def520f62e620d3b7744bd79a37c36f6a4708950541f64bb3de3b63ee256d8` | `service/dialog/DialogDetectionLocalMechanics.java` |
| `??` | 2087 | `33f2f81f72cce681759cc79c932e794f02b926e3198c233659d452b5a1efee86` | `cloud/turn/TurnConfiguration.java` |
| `??` | 6478 | `45b5708c39ec05774c8da0e5bb17dbac3ffcd64403abb32f300ff7df4ded8945` | `cloud/turn/TurnModeGuard.java` |
| `??` | 11451 | `4232d3a766a7df74e6839dc51c551c1919e29a6b7566a51ae8baebd494e7a6a5` | `cloud/turn/WindowTurnLoop.java` |
| `??` | 13101 | `9e92cde9a9f68455a178d6d71bb771a7480ad088aedb7cb77321561afa3428f0` | `cloud/turn/LocalTurnActionExecutor.java` |
| `??` | 4224 | `0056ddb966aef036e345b272638158b3ddcbb34a12e06f5dff134c54cf5dfb06` | `cloud/turn/LocalServiceStepDispatcher.java` |
| `M` | 24192 | `5bd3132a6bb8c387e23ed99ff59fcf05726161ea3abac7c4e06c6e1d7e059467` | `window/control/WindowTaskControlService.java` |

## 风险

1. 两仓均有大量并发 dirty/untracked；尤其旧 remote 129 文件全为 `??`。当前 SHA 只能当观察值，不能证明未来字节未漂移。
2. `TurnModeGuard.startRemote(...)` 当前没有生产 caller；在显式启动/暂停/恢复/停止/注销链闭合并完成用户运行证据前，
   旧 transport/lifecycle 的“已被新链替代”不能只靠 Spring bean 存在来推断。
3. `RemoteAutoCombatPanelFact`、`RemoteLeftTopStatusFact`、`RemoteCoordinateSpace` 有真实生产源码入边；任何把它们直接列为
   候选 DELETE 的未来行都必须先退回 `REWIRE` 并写明移除 caller 的具体卡。
4. token 命中不等于编译引用。Leader precheck 的旧类型命中目前是 Javadoc；TeamReturn 等类还存在同名 nested type。
   未来扫描必须解析 import/package/type，不得只按 simple-name 数量判定。
5. “零生产 caller”也不自动等于候选 DELETE：NPC reference/shadow 是用户批准的永久保留例外。
6. 43A 若把共享 Dialog/BattleRadar/NPC mechanics 当 handler 专用文件，会破坏 reference/shadow；43M 必须记录每个共享 caller。
7. 42A 内部存在 registry/ledger SCC，并由 polling/lifecycle/handler 共同消费；不按 `43A -> 42A` 顺序会产生半拆图。
8. 43B 的零引用必须在 42A 后重新算。当前任何残余 DTO 数量、line number、byte size 或 SHA 都会因 35/36/37、40D、
   43A、42A 继续写入而失效。
9. 当前无本地或远端 tracking ref 可证明分支已与服务器最新状态一致；本次又禁止 fetch。未来 freeze 需要由父级在允许的
   Git 边界内确认基线，再现算 manifest。
10. 本次按约束没有运行编译或测试；证据仅覆盖当前 source graph、生产引用和静态配置图，不代替后续 cohort gate。

## 未来 named-test 与互斥证据

- `TURN-42M`、`TURN-43M` 的权威 test profile 都是 `ZERO`：只生成逐文件 manifest，不运行 JUnit。
- `TURN-43A` 未来唯一点名 guard：`OldRemoteConsumerRemovalGuardTest.java`；计划命令为
  `mvn -q -Dtest=OldRemoteConsumerRemovalGuardTest test`，随后 `mvn -q -DskipTests compile`。
- `TURN-42A` 未来唯一点名 guard：`OldRemoteLifecycleRemovalGuardTest.java`；计划命令为
  `mvn -q -Dtest=OldRemoteLifecycleRemovalGuardTest test`，随后 `mvn -q -DskipTests compile`。
- `TURN-43B` 未来唯一点名 guard：`OldRemoteResidualRemovalGuardTest.java`；计划命令为
  `mvn -q -Dtest=OldRemoteResidualRemovalGuardTest test`，随后 `mvn -q -DskipTests compile`。
- 上述 guard 的 repo-relative test path 尚未由父 manifest 冻结；未来只能由对应父行给出，不能从文件名猜目录。
- 同一 DHXY Java deletion lane 必须严格串行：43A guard+compile 完成后才重扫并进入 42A；42A guard+compile 完成后才重扫并进入 43B。
- 42M 与 43M 的固定报告文件彼此不同，但两者读取同一旧 remote 图，且上游 35/36/37/40D/41 与当前 Java writer
  会共同改变 caller 和 SHA；各自 freeze 前必须独立重扫，不能复用另一份报告的旧快照。
- 两仓删除 lane 可在各自前置满足后彼此并行；DHXY 的 43A/42A/43B 与 Cloud 删除不得共享或覆盖对方 dirty/untracked。
- 当前存在 Java writer 时，本 helper 不占 Maven/JUnit/compile mutex；本报告也没有启动任何构建进程。

PRECHECK_COMPLETE true EOF

## PRECHECK append-only 分类勘误

- 上文“候选 DELETE”中的 `cohort` 名单只表达权威计划的未来施工分组，不构成当前逐文件候选分类。
  为严格执行“尚有真实 caller 的文件不得标可删”，当前 source graph 的逐文件分类以本节为准。
- 当前可写为“零真实 caller 的候选 DELETE 锚点”仅限本次已核实的：
  `LocalRemoteGameCommandHandler`、`LeaderPrecheckMechanics`、`RemoteTaskRunLifecycleService`、
  `HttpRemoteCommandTransport`、`HttpRemoteTaskRunApiClient`。它们仍须通过未来父 manifest 的全量引用、
  reflection/config、SHA 与依赖门复算；本节不形成最终清单。
- `BoundLeaderPrecheckCaptureCapability` 当前有 `LeaderPrecheckMechanics` caller；
  `RemoteCommandPollingLoop` 当前有 `RemoteTaskRunLifecycleService` caller；
  `RemoteTaskRunRegistry`、`RemoteOperationLedger`、`RemoteCommandTransport`、
  `RemoteCommandTransportException` 当前均有上文列明的旧图 caller。它们当前全部是 `REWIRE`，
  只能在前序 caller 真实移除后重新扫描，不能按同 cohort 关系提前标候选 DELETE。
- 仅由 `LocalRemoteGameCommandHandler` 使用的 mechanics/DTO 当前同样有 caller，先记 `REWIRE`；
  43A 移除 handler 后才能复算。NPC reference/shadow 及其共享 mechanics 始终维持 `KEEP`。
- 本 helper 未完成全部 129 文件的逐行最终 manifest，因此不点名任何 43B 候选 DELETE；
  43B 只能消费 42A 后重新证明零生产引用的父 manifest 行。

PRECHECK_COMPLETE true EOF
