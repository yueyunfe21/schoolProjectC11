# CR271 TURN-38M/38C authority classification helper PRECHECK

## 0. 角色与边界

- 角色：CR271 Internal 非绑定 readiness helper，仅为 TURN-38M/38C 的父级分类提供当前源码证据。
- 本文只给 `PRECHECK`、风险、精确引用和非绑定候选，不冻结 `KEEP_REWIRE/DELETE`，也不替代父级写入固定分类表。
- 父级固定分类表仍是 `docs/superpowers/plans/reports/2026-07-15-turn-38-authority-state-classification.md`；本 helper 未创建或修改该文件。
- 唯一写入是本文。未修改 Java、权威计划、CR 卡、`ACTIVE_WORK.md` 或其它报告。
- 未执行 Git 写操作，未运行 Maven、JUnit、runtime、application、server、Task、UI、capture 或 input。

## 1. 已读权威输入

- 完整读取 `D:/mavenProject/DHXY/AGENTS.md`。
- 完整读取 `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。其中 `:52-66` 要求边界状态的 local/cloud 放置由用户或父级决定，`:85-93` 要求每窗口 `GameContext.State`。
- 完整读取 `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271。当前口径见 `:18-24`：Java writer 活动期间不运行 Maven，TURN-38M helper 只写唯一报告、不改 Java、不批准卡，且无已批准业务差异。
- 完整读取权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14 至 19 节。关键约束见 `:1154-1163`、`:1325-1334`、`:1430-1433`、`:1650`、`:1664`。
- 完整读取 HTTPS turn 协议 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。关键合同见 `:117-126`、`:151-162`、`:337-356`：Cloud 仅保留当前 runtime 与最后接受的 `startRequestId/ack`，不是 durable workflow/session/ledger；`pauseRequested` 暂停 Cloud Task 推进；每窗口只允许一个 action payload；业务与 retry/fallback 归 Cloud。
- 完整读取两仓当前真实源码中的五个目标文件及其生产引用、间接 consumer、当前替代 owner 和 turn-native context 路径。
- 对照第二轮审计 `docs/superpowers/plans/reports/2026-07-15-full-card-plan-round2-mutex-delete-audit.md:221-234`：五文件不是一个 mutex-safe implementation unit；保留候选必须带全量新 consumer，删除候选必须留到 old authority SCC 删除卡。

## 2. 当前工作树快照

快照时间：`2026-07-16T06:07:03-04:00`。

| 仓库 | 当前分支 | HEAD | 只读状态要点 |
|---|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | dirty/untracked 已保护；五目标 symbol 在 `src/main/java` 为零引用 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | dirty/untracked 已保护；五个目标文件当前均为 `??`，不是 HEAD 内受跟踪文件 |

| Cloud 目标文件 | bytes | SHA-256 |
|---|---:|---|
| `remote/CloudGameContextStateOwner.java` | 22174 | `8D5BBEFAC713DA2AD8FFF1C95E4A79701DF184EFFC8EA022FA4228B15E584DBF` |
| `remote/CloudLeftTopStatusSwitchState.java` | 3212 | `FC3C859C767300F3899B611A72B08B439D0CADC2D8113B02955E83B321337CFC` |
| `remote/CloudPausedReadOnlyObservationContext.java` | 5321 | `BE02F23DB41CEA7F4342FF6B2FFC6757D6FDB16BE8882131F8818F676791CAE3` |
| `remote/CloudPlayerStateStateGovernor.java` | 77998 | `B5E17B474C11EC6D2FBBD0B01814E78D807CA4E47982A2D51B1597FD1702F713` |
| `remote/CommonBoxStateGovernor.java` | 20044 | `DD4C8CCA5D020CF729820414CEF10B70C6082B9DE66A448C257FCC0FA6B11465` |

Cloud 相对源码根为 `D:/mavenProject/dhxy-cloud-brain/src/main/java`。五目标名称在 Cloud `src/test/java` 为零引用，在 DHXY `src/main/java` 也为零引用。本预检因此只描述上述未跟踪 Cloud 工作树快照；TURN-38M 真正执行时必须在 predecessor source stable 后重扫引用和 SHA。

## 3. 非绑定候选总表

| authority-bound state | 当前真实 owner / 状态路径 | 目标类型的外部生产引用 | 非绑定候选 | 候选去向 |
|---|---|---:|---|---|
| `CloudGameContextStateOwner` | 对象由 old route assembly 构造并强引用，但 projection 方法零生产调用；未绑定的业务 `GameContext` 实际回落到各 bean 的 `defaultState` | 12 个 type-site，分布于 2 个旧图文件 | **需要父级裁决候选**，存在 `KEEP_REWIRE` 压力，但当前文件和未来 runtime 的 owner seam 未闭合 | 父级若选保留则 TURN-38C；若另建 replacement 并选删除则留 TURN-44A |
| `CloudLeftTopStatusSwitchState` | old authority 的 per-run pending owner；legacy `CloudTaskServiceExecutionContext` 间接消费；turn-native context 无 owner 且调用会抛错 | 9 个直接 symbol site，分布于 2 个旧图文件；状态流行号见第 5 节 | **`KEEP_REWIRE` 候选，待父级冻结** | TURN-38C，必须冻结全部新 context consumer |
| `CloudPausedReadOnlyObservationContext` | old `RemoteTaskRunCoordinator` PAUSED binding 的 immutable snapshot capability，沿 old gate/ledger/retained/exclusive 图传递 | 32 个直接 symbol site，分布于 6 个旧图文件 | **`DELETE` 候选，待父级冻结** | 文件不在 TURN-38C 改写，留 TURN-44A old authority SCC |
| `CloudPlayerStateStateGovernor` | 该 governor 自称 session/projection/persistent owner，但没有外部生产 caller；当前 live state 在 `PlayerStateService.runtimeStates` | 0 个外部 type-site | **`DELETE` 候选，待父级冻结** | 文件不在 TURN-38C 改写，留 TURN-44A；伴生 owner 进入后续删除 manifest 复扫 |
| `CommonBoxStateGovernor` | 该 governor 没有外部生产 caller；当前 live pending owner 是 `CommonBoxService.pendingByKey`，开关 owner 是 `BotProperties` | 0 个外部 type-site | **`DELETE` 候选，待父级冻结** | 文件不在 TURN-38C 改写，留 TURN-44A；伴生 properties 进入后续删除 manifest 复扫 |

表中候选不是 TURN-38M 分类结果。权威计划 `:1327-1334` 明确要求父级在固定分类表冻结分类和 exact consumer write set 后，TURN-38C 才可领取。

## 4. `CloudGameContextStateOwner` 精确证据

### 4.1 声明 owner 与旧图路径

- 目标定义：`CloudGameContextStateOwner.java:21-27` 明确称其 dormant，`:34-46` 持有 `RemoteTaskRunCoordinator`、`GameContext` 和 per-run entry map。
- initial owner：`:59-103` 以 old `TaskExecutionContext`、run revision、full scope quota 创建一份 `GameContext.State`。
- resume owner：`:115-159` 依赖 old revision advance 和 execution lock 保留同一 State。
- projection owner：`:173-220` 最终调用 `GameContext.callWithState(...)`。
- terminal owner：`:233` 起由 exact old terminal binding 释放。

当前对象构造可达链是：

`CloudBrainServer.java:86-91`
→ `RemoteTaskRunRoutes.java:45-64`
→ `CloudTaskRunAuthorityAssembly.create(...)`
→ `CloudTaskRunAuthorityAssembly.java:48,73` 构造并持有 target。

该链只证明对象随 old routes 被构造并锚定，不证明 Task/Service stack 已投影到它。当前调用证据是：

- `CloudTaskRunAuthorityAssembly.retainedLifecycleActivationAdapter()` 只有定义 `:136-137`，生产代码没有调用者。
- `CloudTaskRunRetainedLifecycleActivationAdapter.activateInitial(...)` 只有定义 `:58`，生产代码没有调用者。
- `CloudTaskRunAuthorityAssembly.createCurrentContextSlotActivation(...)` 只被上述 adapter 在 `CloudTaskRunRetainedLifecycleActivationAdapter.java:112` 调用。
- `CloudGameContextStateOwner.callWithState(...)` 除定义 `:173` 和内部 `GameContext.callWithState` `:209` 外，生产代码没有调用者。

所以当前真实状态是“old graph 对象已构造，activation/projection dormant”，不是“业务调用已安全绑定每窗口 State”。

### 4.2 目标类型全部外部生产引用

- `CloudTaskRunAuthorityAssembly.java:48,73,131,237,269,331,350,365,405,467`。
- `CloudTaskRunRetainedLifecycleActivationAdapter.java:248,259`。
- DHXY `src/main/java`：零引用。
- Cloud `src/test/java`：零引用。

### 4.3 仍然存在的真实业务状态需求

- `GameContext.java:18-19` 是 singleton `defaultState` 加 `ThreadLocal.withInitial(() -> defaultState)`；`:115-133` 只有显式 `callWithState` 才临时隔离，未绑定线程会回落到共享默认对象。
- Cloud Task/Service 当前仍直接读写 `GameContext`：`AutoBattleTask.java:129,139,146,175-176,284`；`XiuluoTaskV2.java:329,393-415,2267-2268`；`WubeiTask.java:330,350,404-427,1167-1168`；`FiveRingTaskV2.java:262,290-291,763,2058,2768-2773`。
- Service consumer 仍包括 `AutoCombatService.java:38,140,171,204,227,374,412,447,477,525`、`BattleRadarService.java:105,170,202,291,318,371,1285-1296,1397,1419`、`TaskMaintenanceService.java:47,641,744-781`、`PlayerStateService.java:101`、`NavigationService.java:175`、`AutoCombatPanelService.java:89` 和 `BaseTaskTemplate.java:29,115-138`。
- `DHXY_CONTEXT.md:85-93` 的目标仍要求 per-window `GameContext.State`。直接删除 owner 需求而没有 replacement，会留下多窗口状态交叉风险。

### 4.4 需要父级裁决的两条路线

- 路线 A，`KEEP_REWIRE` 候选：保留这个文件承担 turn-native per-runtime/per-window State owner，但必须去掉 `RemoteTaskRunCoordinator`、session、runRevision 和 old terminal binding 语义，并冻结真正执行 `GameContext.callWithState` 的新 consumer。
- 路线 B，`DELETE` 候选：由 TURN-40B 的 runtime/factory 或另一个父级冻结的 turn-native owner 完整承担 State 创建、每次 Task stack 投影和 terminal cleanup，再把本文件留到 TURN-44A。
- 当前源码中不存在计划所列的 `CloudTurnTaskRuntime` 或 `CloudTurnTaskFactory` 文件；权威计划却让 TURN-38C 位于 TURN-40B 之前。父级必须明确是“38C 先交付可被 40B 消费的 owner API”，还是“40B 自己拥有 replacement，本文件删除”。本 helper 不替父级选择。
- 无论选哪条路线，consumer 盘点不可只写 target 自身。exact write set 至少覆盖实际投影入口和直接消费新 context 的 `TaskExecutionContext`/runtime 生命周期入口；通过 `GameContext` 间接读写状态的 Task/Service stack 必须列入行为 consumer 清单，但透明绑定时不应为凑写集而修改它们。权威计划 `:1650` 还要求每个保留行独立 `*TurnStateTest`。

## 5. `CloudLeftTopStatusSwitchState` 精确证据

### 5.1 声明 owner 与全部外部生产引用

- target `:9-22` 声明 retained per-run pending owner；`:60-85` 的 key 包含 scope、client session、task run、window tuple、identity epoch、stop epoch，故跨 resume 保留但仍绑定 old identity model。
- `CloudTaskRunAuthorityAssembly.java:197-198,214,226,298,306,401,412,424-425,451-452`：创建、装配进 Service context、跨 resume 复用并由 runtime 持有。
- `CloudTaskServiceExecutionContext.java:24,38,57-58,76,98-99,189-190,194-195,203-204,208-209,220-221`：字段、构造和 pending API。
- DHXY `src/main/java`：目标类型零引用。Cloud `src/test/java`：目标类型零引用。

### 5.2 当前真实 consumer 路径

legacy 路径是：

`CloudTaskRunAuthorityAssembly.createCurrentContextSlotActivation(...)`
→ `CloudLeftTopStatusSwitchState`
→ `CloudTaskServiceExecutionContext`
→ `TaskExecutionContext` legacy delegate
→ `LeftTopStatusSwitchService`。

具体间接业务调用是 `LeftTopStatusSwitchService.java:73,75,92,96,98,100,242`。但 turn-native `TaskExecutionContext` 在 `TaskExecutionContext.java:61-82` 将 `delegate=null`，`:96-109` 创建新 context；left-top 方法 `:335-352` 仍全部调用 `legacyDelegate`，而 `:442-451` 对 turn-native 明确抛出 unavailable 异常。

DHXY 旧本地对照 owner 是 `WindowRuntimeContext.java:97,447,456,466,475,2224`。它只能说明迁移前每窗口状态语义，不能直接成为新权威，因为协议 `:337-356` 与 `DHXY_CONTEXT.md:58-66` 把 Task/Service 业务状态留在 Cloud。

### 5.3 非绑定候选与父级待定 consumer

- `KEEP_REWIRE` 压力来自真实业务调用仍在、pending 必须在同一 Task pause/resume 间连续、turn-native 当前会抛错。
- 父级需冻结唯一 turn-native owner 的生命周期：至少区分同 window 的新 Task run、pause/resume、stop/terminal cleanup，并不得重新引入 client session、runRevision ledger 或 DHXY business owner。
- exact write set 候选至少涉及 `TaskExecutionContext.java`、真正创建/持有 per-run state 的 runtime/factory，以及在 API 形状变化时的 `LeftTopStatusSwitchService.java`。如果保持 Service 的四个 context 方法签名不变，父级仍需写明它为何不在改动集而只是间接 consumer。
- 不应为了将来删除 old graph 而只重写 `CloudTaskServiceExecutionContext`；第二轮审计 `:231-234` 明确反对这种没有新 compile boundary 的临时改线。

## 6. `CloudPausedReadOnlyObservationContext` 精确证据

### 6.1 真实 owner 与全部外部生产引用

- target `:9-23` 是 immutable read-only PAUSED capability，不是独立业务状态容器。
- target `:50-66` 只能从 `RemoteTaskRunBinding` 的 `PAUSED` status 与 exact run revision 快照创建。真实 lifecycle owner 因而是 old `RemoteTaskRunCoordinator` binding，`CloudTaskRunExecutionGate` 只是 capability mint gate。
- `CloudTaskRunExecutionGate.java:345,353,366,390,411,415`。
- `CloudTaskRunAuthorityAssembly.java:152,163`。
- `CloudTaskRunRetainedLifecycleActivationAdapter.java:142,179,209,224,327,341-342,413,419`。
- `CloudTaskRunActionLedger.java:437,439,1162,1173,1197,1411,1752`。
- `CloudTaskExclusiveInteractionAuthority.java:70,72,1162`。
- `CloudTaskRetainedActionState.java:601,605,639,647,661`。
- 以上引用全部位于 old `remote` authority 图；DHXY production 和 Cloud test 均为零引用。

### 6.2 非绑定 `DELETE` 候选证据

- HTTPS turn 协议 `:119-126` 明确没有 durable workflow/session/ledger，pause 只暂停 Cloud Task progression 并保持 DHXY long-wait loop。
- 协议没有定义 PAUSED 期间另行 mint observation capability、retained action identity、paused revision 或 exclusive park 的通道。
- 将该 capability 接到 turn-native 会重新引入被新协议排除的 old binding/revision/ledger 语义，并让 pause 期间继续业务 observation，属于新行为路线。
- 因而当前证据支持“文件不动，随 old authority SCC 在 TURN-44A 删除”的候选。父级如认为 pause 期间必须继续观察，应先作为独立行为裁决处理，不能把它暗含进 TURN-38C migration。

## 7. `CloudPlayerStateStateGovernor` 精确证据

### 7.1 target 的声明语义与外部引用

- target `:22-29` 自称 PlayerState physical entries、projections、sessions、quota 的 sole same-process owner。
- target `:38-46` 持有 old `RemoteTaskRunCoordinator`、physical entry map、exact session map 和 quota map；`:85-118` 由 old revision/context 创建 `CloudPlayerStateStateOwner`。
- 目标 symbol 在生产源码中的命中仅为本文件 `:29,49,53,61,1483`，外部生产引用为零。
- 伴生 `CloudPlayerStateStateOwner` 只在自身和 governor 内出现；没有 active Task/Service caller。

### 7.2 当前 live owner 与调用路径

- 当前 live owner 是 `PlayerStateService.java:111` 的 `runtimeStates`。
- `PlayerStateService.java:1216-1242` 以 turn invocation 的 `deviceId/windowId` 为 key，在 `GameContext.me` identity drift 时重建 entry。
- `PlayerStateService.java:1368-1376` 的 `PlayerRuntimeState` 实际保存 incense 时间/重试时间、图标偏移、首药检查计数、战斗退出时间和 pending first-aid plan。
- 真实 consumer 包括 `AutoBattleTask.java:135`、`AutoCombatService.java:362,382,397,399,409,462,464,472,573-577`、`WubeiTask.java:357-358,1770,3888,3914,4143`、`XiuluoTaskV2.java:354,594,1110,1561,1941,2310,2853,2875,2902`、`FiveRingTaskV2.java:768,1112,1833`、`TeamReturnService.java:64` 和 `NavigationService.java:821`。

### 7.3 非绑定 `DELETE` 候选证据与风险

- 将 dormant governor 接入会把当前简单 per-window runtime map 改成 full authenticated scope、client session、persistent entry、projection handle、quota 和 session-release 模型，直接冲突 HTTPS turn 协议 `:119-126`。
- 当前 target 不承载 live caller，故无需 TURN-38C 临时改写。候选路线是保持文件不动，后续与 old authority SCC 删除。
- 删除 cohort 不能只列 governor。父级在 TURN-44M45M/44A 前必须重扫 `CloudPlayerStateStateOwner.java`；当前它在 governor 删除后会成为外部零引用候选。
- `PlayerStateService.runtimeStates` 是否需要由 TURN-40B runtime 做 terminal cleanup，是未来 runtime 验收问题，不构成采用 dormant governor 的理由；父级应保持当前业务字段、reset 条件和调用顺序不变。

## 8. `CommonBoxStateGovernor` 精确证据

### 8.1 target 的声明语义与外部引用

- target `:20-34` 声明 tenant-scoped role toggle、pending、claim/seal、30 秒 TTL 与 capacity 语义；`:38-42` 持有 tenant state map。
- `:59-69` 返回 scope-bound `CloudCommonBoxProperties`，`:96-154` 具有 observation ticket、config revision、claim state 与 capacity gate，`:438-448` 使用 full old stable-run key。
- 目标 symbol 在生产源码中的命中仅为本文件 `:32,44`，外部生产引用为零。
- `CloudCommonBoxProperties` 只在其定义和 governor `:3,59,472` 出现，没有 active Service consumer。

### 8.2 当前 live owner 与调用路径

- 当前 live pending owner 是 `CommonBoxService.java:62` 的 `pendingByKey`；`:141-184` 校验 TTL/window/identity/task-run 后消费；`:320-341` 以 `PENDING_TTL_MS=30_000` 写入 pending。
- 当前开关 owner 是 `BotProperties.java:80-85` 的 leader/member flags，`CommonBoxService.java:58-74` 直接注入 `BotProperties`。
- active callers 是 `AutoBattleTask.java:247`、`XiuluoTaskV2.java:2412,2475`、`WubeiTask.java:2790,3935`、`AutoCombatService.java:366,481,507`。

### 8.3 非绑定 `DELETE` 候选证据与风险

- dormant governor 与 live Service 虽同为 30 秒 TTL，却增加 tenant incarnation、config revision、claim/seal、capacity 和 old run scope fence。接入它会改变当前并发、开关和消费语义，不是等价 plumbing。
- 当前 target 不承载 live caller，候选路线是保持文件不动，后续与 old authority SCC 删除。
- 删除 cohort 不能只列 governor。父级在 TURN-44M45M/44A 前必须重扫 `CloudCommonBoxProperties.java`；当前它在 governor 删除后会成为生产零引用候选。

## 9. 父级需要冻结的精确事项

1. `CloudGameContextStateOwner`：选择“本文件改造成 turn-native owner”或“TURN-40B 另有 replacement owner”。必须写明创建、projection、pause/resume continuity、terminal cleanup 和实际 invocation consumer，不能只保留 handle 容器。
2. `CloudLeftTopStatusSwitchState`：选择唯一 per-run owner 及生命周期，冻结 `TaskExecutionContext`、runtime/factory 和必要 Service consumer 的 exact write set。
3. 三个删除候选：在固定分类表写明不由 TURN-38C 改写，目标卡是 TURN-44A，并把其直接 old-graph/companion 文件交给后续 deletion manifest 复扫。
4. 若 `CloudGameContextStateOwner` 或 `CloudLeftTopStatusSwitchState` 最终进入保留行，按权威计划 `:1650` 为每一行分别冻结独立 `*TurnStateTest`，不能用一个宽泛状态测试覆盖二者。
5. TURN-38M 正式分类应在 TURN-38A source stable 后重跑双仓 production refs、文件状态和 SHA。当前五文件均未跟踪，任何 predecessor/并行 worker 修改都会让本快照失效。

## 10. PRECHECK 风险清单

- **快照漂移风险**：权威 DAG `:1154-1163` 显示 TURN-35/36/37、38A 尚在 38M 之前；当前 helper 不能证明未来 source stable 时引用仍相同。
- **构造等于使用的误判风险**：`CloudGameContextStateOwner` 随 old routes 构造，但 lifecycle adapter 和 projection 均无生产 caller。仅凭 assembly 字段会高估其真实覆盖。
- **共享默认状态风险**：若未来 Cloud Task stack 未由唯一 owner 包住 `GameContext.callWithState`，`GameContext.java:18-19` 会让多个未绑定线程共享 default State。
- **turn-native 缺口风险**：left-top 业务调用仍在，而 `TaskExecutionContext.java:442-451` 对 turn-native 直接抛错。它不能以“old state 尚在”作为接线完成证据。
- **协议回流风险**：paused capability 和 PlayerState governor 都依赖 session/revision/ledger 风格 old authority；直接改名接入会违反最小 HTTPS turn 合同。
- **业务漂移风险**：dormant Player/CommonBox governor 的字段、TTL/fence、claim/session 语义明显多于当前 live owner。采用它们不是无差异迁移。
- **删除 cohort 风险**：单删 governor 会遗留 `CloudPlayerStateStateOwner` 或 `CloudCommonBoxProperties`。单删 paused capability 会破坏 old ledger/gate/retained/exclusive SCC 编译闭包。
- **写集顺序风险**：38C 早于尚不存在的 40B runtime/factory。GameContext 路线若不先由父级明确 API/consumer 边界，38C 无法满足“全部新 context consumer”的计划要求。
- **本地回迁风险**：DHXY `WindowRuntimeContext` 虽有 left-top pending 对照实现，但业务状态迁回 DHXY 会违反 Cloud-owned Task/Service 决策边界。
- **验证边界**：本卡按权威计划 `:1664` 为 `ZERO`，本次也按用户约束未运行任何测试、编译或 runtime。正式父级分类只能依赖 source/ref/SHA 审查；未来保留行再各自承担命名测试。

## 11. PRECHECK 汇总

- 当前证据形成 `1` 个需要父级裁决候选：`CloudGameContextStateOwner`。
- 当前证据形成 `1` 个偏 `KEEP_REWIRE` 候选：`CloudLeftTopStatusSwitchState`。
- 当前证据形成 `3` 个偏 `DELETE` 候选：`CloudPausedReadOnlyObservationContext`、`CloudPlayerStateStateGovernor`、`CommonBoxStateGovernor`。
- 上述数量只用于父级复核排队，不是冻结分类。父级仍需在 predecessor source stable 后重扫，并在固定分类表逐行决定。

<!-- TRUE_EOF: CR271 TURN-38M AUTHORITY CLASSIFICATION HELPER PRECHECK_COMPLETE -->
