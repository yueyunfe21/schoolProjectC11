# CR271 TURN-38M CloudLeftTopStatusSwitchState owner 路线风险 helper PRECHECK R1

## 1. 角色、范围与结论边界

- 日期：2026-07-16。
- 角色：CR271 Internal 非实现、非 reviewer、非父级的独立路线风险 helper。
- 唯一问题：比较两条 `CloudLeftTopStatusSwitchState` replacement 路线：
  1. TURN-38C 对现文件做 `KEEP_REWIRE`；
  2. 由既有 turn runtime/context 直接持有 replacement，现文件保持不动至 TURN-44A。
- 本文只记录 `PRECHECK`、源码证据、exact consumer/write-set、DAG 风险、生命周期和非绑定建议；不替父级冻结分类，不改变任何卡片状态。
- 本轮唯一写入是本报告；没有修改 Java、权威计划、CR 卡、`ACTIVE_WORK.md` 或其它报告。
- 本轮没有运行 Maven、JUnit、application、server、runtime、Task、UI、capture 或 input，也没有执行 Git mutation。

## 2. 完整读取材料与只读快照

### 2.1 权威材料

本轮完整读取或复核：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271 当前区段。
4. `D:/mavenProject/DHXY/docs/业务逻辑.md` 全部 1426 行，包含 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线门、维护顺序、pause/stop 与禁止新增 TTL/retry 的规则。
5. `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
6. `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
7. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-38M-authority-classification-preflight-helper.md`。
8. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-39-readiness-preflight-helper.md`。
9. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-40B-readiness-preflight-helper.md`。
10. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-34C-readiness-preflight-helper.md`，用于核对 AutoBattle 的实际 left-top 编排边界。
11. 已交付的 GameContext owner 路线风险 R1，用于核对 40B runtime 生命周期与“不新增第二 owner”的共同约束。

仓库中没有独立命名的 TURN-38A readiness 报告。38A 的当前依据来自权威计划 `:1154,1302-1312,1645`、TURN-39 readiness 对 context/test ownership 的展开，以及当前源码；本文不虚构不存在的报告。

### 2.2 相关源码与测试

已完整读取 Cloud 当前：

- `remote/CloudLeftTopStatusSwitchState.java`
- `runner/context/TaskExecutionContext.java`
- `service/LeftTopStatusSwitchService.java`
- `service/lefttop/CloudLeftTopStatusPortAssembly.java`
- `remote/CloudTaskServiceExecutionContext.java`
- `remote/CloudTaskRunAuthorityAssembly.java`
- `task/AutoBattleTask.java`
- `service/AutoCombatService.java` 的完整 left-top maintenance 调用段
- `service/TaskMaintenanceService.java` 的完整 `LEFT_TOP_STATUS` capability 开关段
- `service/LeftTopStatusTurnContractTest.java`
- `runner/context/TaskExecutionContextTurnContractTest.java` 的 old/turn left-top contract 段

同时读取/对照：

- Cloud `migration-baseline/696a12b0/.../LeftTopStatusSwitchService.java`。
- DHXY `WindowRuntimeContext` 的 pending 字段、四个 API 与 player-scoped cleanup。
- DHXY `DefaultWindowTaskStartupInitializer.java:99-109` 的 696 startup caller。
- 四个 real Task 到 `AutoCombatService.handleCombatTick(...)` 的全部真实引用。
- 当前及未来 40B runtime/factory 路径、现有 tests 的全部 left-top symbol/reflection 引用。

### 2.3 两仓工作树保护快照

| 仓库 | branch | HEAD | dirty 摘要 |
|---|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 85 项：44 tracked changes，41 untracked |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 28 项：9 tracked changes，19 untracked |

当前关键快照：

| 文件 | 行数 | SHA-256 | 状态 |
|---|---:|---|---|
| `remote/CloudLeftTopStatusSwitchState.java` | 88 | `FC3C859C767300F3899B611A72B08B439D0CADC2D8113B02955E83B321337CFC` | untracked shared work |
| `runner/context/TaskExecutionContext.java` | 491 | `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` | untracked shared work |
| `service/LeftTopStatusSwitchService.java` | 306 | `03E43188B52E6F07C50E7975B7EEE3C53BDDC4C12D9866FF130A869D5CFE1EF2` | untracked shared work |
| `service/LeftTopStatusTurnContractTest.java` | 1063 | `C9D0B21AEC3637452E0507F0F716C43E9F8CD21010368EBA249012BE3C66EF8A` | tracked current test |

未来五个 40B production path 当前全部不存在：`CloudTurnTaskFactory`、`CloudTurnTaskRuntime`、`CloudTurnTaskRegistry`、`CloudTurnTaskStartResult`、`CloudTurnControlPort`。本 helper 未创建占位文件，也不占用其写集。

## 3. 权威 DAG、写集与协议硬约束

1. 权威计划 `:1154-1163` 的顺序是：
   `34C/35/36/37 -> 38A -> 38M -> 38C -> 39 -> 40B -> 40C`。
2. `:1327-1334` 要求 38C 只实施 38M 的 `KEEP_REWIRE` 行；`DELETE` 文件保持不动到 44A，并要求父级先冻结全部新 context consumer。
3. `:1336-1343` 把 `CloudTaskServiceExecutionContext.java` 给 TURN-39，但没有把 `TaskExecutionContext.java` 或 old assembly 给 TURN-39。
4. `:1355-1362` 与 40B readiness `:55-77` 把 40B production 固定为五个新 runtime 文件；40B 不修改 `TaskExecutionContext`、old remote state/facade 或 business Service。
5. `:1497` 要求 pause/resume 使用同一易失 state，terminal/restart 释放，不加 TTL。
6. `:1645,1650,1660` 分别把 context old-authority removal、每个保留行的独立 `*TurnStateTest`、runtime/factory tests 分给 38A、38C、40B。
7. HTTPS turn 协议 `:117-126` 只允许 current runtime 与 last accepted `startRequestId/ack`；这不是 business retry、durable workflow、session 或 ledger。Pause 只在既有 checkpoint 停住同一 Task progression。
8. 任何路线都不得新增 owner permit、session key、retained generation、ledger/history、TTL/expiry、cleanup retry record、自动 Task/action retry 或第二 exchange。

因此，“旧文件留 44A，replacement 到 40B 再说”不是自动可行的第三条路线。`LeftTopStatusSwitchService` 只收到 `TaskExecutionContext`；若 38C 前没有可用状态 seam，40B runtime 单靠自己的五文件无法让四个 context API 生效。

## 4. 当前状态模型与全部真实引用

### 4.1 `CloudLeftTopStatusSwitchState` 当前语义

`CloudLeftTopStatusSwitchState.java` 当前：

- `:9-16` 把自己定义为一个 authoritative task run 的 retained pending state。
- `:18-22` 保存一个 `StableRunKey` 和一个 `AtomicReference<String> pendingSource`。
- `:25-43` 提供 `is/mark/consume/clear`；consume 是 `getAndSet(null)`，source 只存诊断文本，不参与消费匹配。
- `:45-49` 每次操作都要求传入 old `CloudTaskRunExecutionContext` 与 stable key 完全一致。
- `:60-85` 的 key 包含 tenant/user/device、`clientSessionId`、taskRunId、window/native handle/process、player identity epoch 与 stop epoch；刻意排除 runRevision，故 old resume generation 共享 marker。
- 没有 timestamp、TTL、scheduler、retry counter、history 或 persistence。

这个类当前不是 turn-native state。它的唯一构造参数和每个状态 API 都依赖 old run context；类本身还是 package-private，`turn.runtime` 与 `runner.context` 不能直接引用。

### 4.2 目标类型的全部直接 production consumer

| Consumer | 精确引用 | 当前职责 |
|---|---|---|
| `CloudTaskRunAuthorityAssembly.java` | `:197-198,214,226,298,306,401,412,424-425,451-452` | initial 创建、装入 Service context/runtime、old resume 复用同一 state |
| `CloudTaskServiceExecutionContext.java` | `:24,38,57-58,76,98-99,189-190,194-195,203-204,208-209,220-221` | 字段/构造、四个 pending API、给 old runtime 暴露 accessor |

除此之外，Cloud production、DHXY production 与 Cloud tests 对目标类型名均为零直接引用。测试不是直接 new 目标类，而是通过反射启动整个 old authority assembly 间接取得其状态。

### 4.3 当前 old SCC 路径

```text
CloudTaskRunAuthorityAssembly.createCurrentContextSlotActivation(...)
  -> new CloudLeftTopStatusSwitchState(old runContext)
  -> CloudTaskServiceExecutionContext
  -> new TaskExecutionContext(legacy delegate)
  -> LeftTopStatusSwitchService
```

Resume 时，assembly `:288-306` 创建新 old context，却把 `previousRuntime.leftTopStatusSwitchState()` 原对象传给下一 generation。Terminal `:350-373` 没有单独 consume/clear left-top marker；它依赖整个 old runtime/SCC 释放后变得不可达。

TURN-39 只拥有 `CloudTaskServiceExecutionContext`，不拥有 assembly 或目标类。因此 39 不能单独删除其 left-top constructor/field 而维持编译；old direct refs 必须继续作为 dormant SCC 留到 44A，除非父级提前扩大写集。

### 4.4 全部间接 business consumer

状态 API 路径：

- `TaskExecutionContext.java:335-352` 暴露相同四个方法，但当前全部走 legacy delegate。
- `TaskExecutionContext.java:61-82,96-109` 的 turn-native path 设置 `delegate=null`。
- `TaskExecutionContext.java:442-451` 因而让四个方法在 turn-native context 上直接抛出 unavailable。
- `LeftTopStatusSwitchService.java:73,75,92,96,98,100,242` 是四个 API 的全部 production 调用点。

Service 的直接 production caller：

- `AutoBattleTask.java:199-206`：local follower + open `LEFT_TOP_STATUS` capability 时调用一次 `consumeFollowerSafeWindow(...)`，随后立刻 checkpoint。
- `AutoCombatService.java:645-659`：稀疏 combat maintenance 中按 local-support capability 或 standalone 分支调用 `handleCombatMaintenance(...)`。
- `TaskMaintenanceService.java:108-121,166-179`：leader pathing window 打开/关闭 `LEFT_TOP_STATUS` capability；它不直接改 pending bit。

经 AutoCombat 的真实 Task 间接 consumer：

- `AutoBattleTask.java:163`
- `WubeiTask.java:3595,3756`
- `XiuluoTaskV2.java:1828,2063`
- `FiveRingTaskV2.java:1853`

`handleLeaderStartup(...)` 与 `probeMemberStartup(...)` 当前没有 Cloud production caller。现有 696 local caller 是 DHXY `DefaultWindowTaskStartupInitializer.java:99-109`；Cloud 测试会直接调用这两个方法，但测试调用不构成 runtime reachability。TURN-34C readiness 固定的当前 AutoBattle production 顺序只覆盖 startup first-aid/maintenance 初始化与 idle/combat left-top gate，没有补出这个 startup initializer caller。

这是一项独立 consumer 缺口：owner 路线只能让 pending API 可用，不能暗中替 34C/38B3/40B 增加 startup 业务调用。父级必须在 40B 前另行冻结该 696 direct caller 的目标卡和 exact write set。

## 5. 当前测试闭包与重叠风险

### 5.1 现有 TURN-19 test 实际同时承担两类合同

`LeftTopStatusTurnContractTest.java` 当前覆盖：

- `:94-239`：leader/combat/member probe/safe-window 的状态真值表、单 capture、无 input retry。
- `:242-265`：11x19 ROI、真实模板尺寸与 decision threshold。
- `:268-464`：STOP/uncertain/correlation/wrong-current/unsupported task 终态。
- `:470-490`：机械 `CloudLeftTopStatusPortAssembly` 与 Service harness。
- `:528-621`：为 pending state 反射构造 `CloudTaskRunAuthorityAssembly`、old coordinator/session/binding、old `TaskExecutionContext`。
- `:797-843`：exact capture 与 `MOVE -> WAIT120 -> CLICK_LEFT -> WAIT250`。

它现在既是 TURN-19 mechanical/Service contract，又被迫充当 old owner fixture。44A 删除 old SCC 前，必须把 `:528-621` 换成 turn-native context fixture；否则 old graph 删除会让一个与机械合同无关的反射夹具失效。

### 5.2 当前 context test 与未来行为相反

`TaskExecutionContextTurnContractTest.java:494-537` 当前明确断言 legacy surface 存在，并在 `:523-534` 断言 turn-native 的四个 left-top API 全部抛错。无论选择哪条 replacement 路线，这组断言都必须由 38A/38C 的父级明确分配修改归属，不能只新增一个 state test 而让旧断言继续要求失败。

计划点名的 `TaskExecutionContextOldAuthorityRemovalTest` 当前不存在；计划也没有为上述既有 context test 或 TURN-19 test 明列第二次写入。这个 test ownership 缺口应在派发 38A/38C 前冻结。

## 6. `696a12b0` 等价合同

当前 Cloud Service 相对 `migration-baseline/696a12b0` 只把本地 `WindowRuntimeContext` pending API 换成 `TaskExecutionContext` API，并把 capture/click mechanics 换成 TURN-19 port；以下业务真值表必须保持：

| 入口/结果 | 既有 pending 影响 | input/retry 边界 |
|---|---|---|
| member startup `OPEN` | mark pending | probe-only，不点击 |
| member startup `CLOSED` | clear pending | 不点击 |
| member startup `UNKNOWN/CAPTURE_FAILED` | 不改变 | 不点击、不补拍 |
| follower safe window `OPEN + clicked` | consume pending | 一次 exact click command |
| follower safe window `CLOSED` | consume pending | 不点击 |
| follower safe window `UNKNOWN/CAPTURE_FAILED` | 原先 pending=true 则保持；false 仍 false | 不点击、不立即 retry |
| follower safe window `OPEN + known click failure` | 原先 pending=true 则保持；false 仍 false | 不重发 input |
| leader/combat `OPEN + clicked` 或 `CLOSED` | consume pending | 最多一次 click command |
| leader/combat unresolved/known click failure | 不改变 | 不立即 retry |
| unsupported task | 不改变 | 零 command |

还必须冻结以下细节：

1. `consumeFollowerSafeWindow(...)` 当前无论 pending 是 true/false 都会做本次 safe-window probe；pending 只决定 unresolved 时是否重新保持。不得把 pending 改成“是否允许 probe/click”的新 gate。
2. `source` 是诊断文本，不是 key、token、action identity 或 consume 条件；不同 source 仍能 consume 同一个 bit。
3. 多次 mark 仍只表示一个 boolean pending，不累积次数、不保留历史。
4. `UNKNOWN`、capture failure、known click failure 不制造新的业务成功，也不自动重试。
5. `LeftTopStatusSwitchService.LEFT_TOP_STATUS_TIMEOUT_MS=120000` 是单次 HTTPS turn command timeout，不是 pending state TTL；不得用它让 marker 120 秒后过期。
6. 不增加终态额外 probe、cleanup click、验证次数、backoff、expiry 或新 maintenance 顺序。

结论口径：`无已批准业务差异；按 696a12b0 基线等价迁移`。

## 7. 路线 A PRECHECK：TURN-38C 对现文件做 `KEEP_REWIRE`

### 7.1 能在当前 DAG 内闭合的最小形状

路线 A 若要保持 old SCC 到 44A，不能在 38C clean-delete 旧 constructor/API。最小形状只能是双 contract：

- 保留现有 package-private old constructor 与带 `CloudTaskRunExecutionContext` 的 old 方法，供 assembly/Service context 编译。
- 另加无 session/key/revision 的 turn-native state API。
- `TaskExecutionContext` 每个 turn-native 实例创建并私有持有一个该 state；四个既有 public context 方法在 turn path 使用它，在 legacy path 暂时继续 delegate。
- target 只能是 context-owned mutable bit holder，不能拥有 runtime lifecycle、window map、permit、generation、terminal callback 或 cleanup authority。
- `LeftTopStatusSwitchService` 的四个 context 调用保持原签名和真值表，因而是只读间接 consumer。

这个最小变体的新 production direct consumer 只有 `TaskExecutionContext.java`。40B runtime 只是按既有职责创建最终 context，不应另存第二份 target，也不应让 factory/registry 建 state map。

若改成“40B runtime 显式 new target 并传进 context”，会修改 `turnNative(...)` signature，并影响当前二十余个 test call site；它不是最小写集，也会让 state 在 runtime/context 两处都可寻址。本 PRECHECK 不建议该 A 子变体。

### 7.2 A 的最小 exact write set

**TURN-38C production**

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudLeftTopStatusSwitchState.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`

**TURN-38C tests，建议父级冻结以下 exact 集合**

3. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudLeftTopStatusSwitchTurnStateTest.java`
4. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`，移除 old authority reflection fixture，改用 turn-native context。
5. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`，删除四个 API 必须 unavailable 的旧断言；若父级已明确把它放入 38A，则 38C 只复核最终结果、不重复写。

**明确只读**

- `LeftTopStatusSwitchService.java`
- `AutoBattleTask.java`
- `AutoCombatService.java`
- `TaskMaintenanceService.java`
- `CloudTaskRunAuthorityAssembly.java`
- `CloudTaskServiceExecutionContext.java`
- 五个未来 40B production 文件中的其余职责

**TURN-40B 既有 test 集成**

- `CloudTurnTaskRuntimeContractTest.java` 增加同 context pause continuity、terminal/new-start replacement 和 duplicate start 不重建 state 的断言；不新增第三个 40B test 文件。

### 7.3 A 的 clean-rewire 写集冲突

若 38C 立即删掉 target 的 old key/constructor/methods，编译闭包至少扩为：

1. `CloudLeftTopStatusSwitchState.java`
2. `TaskExecutionContext.java`
3. `CloudTaskRunAuthorityAssembly.java`
4. `CloudTaskServiceExecutionContext.java`

并要同步重写 `LeftTopStatusTurnContractTest` 的 reflection fixture。这样会提前修改原计划留给 44A 的 old SCC，并与 TURN-39 对 `CloudTaskServiceExecutionContext.java` 的串行 ownership 相撞。父级未先修改 DAG/write-set 时，不能把这个变体描述为两文件 38C。

### 7.4 A 的生命周期与 44A 风险

- Pause：同一个 Task/context/state，不 clear、不 copy、不 new generation。
- Resume：继续同一个对象；绝不调用 old resume API，也不引入 revision。
- Terminal/exception/stop：由 runtime 丢弃 context/state reference；不把 pending consume 当 terminal action，不新增 cleanup retry。
- New accepted start：新 runtime 创建新 context，初始 pending=false；同 `startRequestId` redelivery 不创建第二份。
- 44A：target 不能随 broad old-remote SCC 一起误删；必须删掉它的 old constructor/key/method half，同时保留 active turn half，或先由父级冻结合法迁移位置。

A 的主要债务是：同一文件从 38C 到 44A 长期承载 old session-key API 与 new no-session API，并在 44A 再次修改。若 turn path 误调 old overload，会重新把 client session/stop epoch 带入新 runtime。

## 8. 路线 B PRECHECK：context 直接持 replacement，旧文件留 44A

### 8.1 唯一窄可行形状

路线 B 的最小形状不是“40B runtime 到时再找办法”，而是：

- `TaskExecutionContext` turn-native half 直接持一个 private `AtomicReference<String>` 或等价单 bit；四个现有方法直接操作它。
- legacy half 在 old SCC 删除前仍可 delegate 到旧 Service context。
- state 不作为 constructor 参数暴露，不新增 interface/handle/provider，不改变所有 `turnNative(...)` call site。
- runtime 通过持有该 concrete context 间接拥有 state 生命周期；factory、registry、Service 均不再持第二份。
- `CloudLeftTopStatusSwitchState.java` 保持字节不动，作为 old SCC 文件留到 44A 删除。

这条路线没有独立 state owner surface，source 也不会变成历史记录。它的代价是 `TaskExecutionContext` 不再是字面上的完全 immutable metadata view；父级需要明确把这个极小 mutable bit 视为既有四个 public API 的 turn-native implementation，而不是允许 context 继续吸收其它业务状态。

### 8.2 B 不能单独延期到 40B

若 replacement 只放在未来 `CloudTurnTaskRuntime`：

- Service 只拿到 `TaskExecutionContext`，没有 runtime 参数。
- 当前 context 没有 state handle/callback seam。
- 40B 被禁止修改 context、Service 和 old target，也不能添加第六个 bridge 文件。
- 使用 static map、ThreadLocal global holder、registry lookup 或 reflection 来绕过会制造第二 owner/session-like lookup，并扩大并发和 cleanup 风险。

因此，B 要么由父级明确把 context replacement 安排在 38C，要么先修订 40B exact write set；不能维持现计划同时声称“40B 五文件自然能接上”。

### 8.3 B 的最小 exact write set

**TURN-38C replacement exception，production**

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`

**TURN-38C tests，建议父级冻结以下 exact 集合**

2. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/LeftTopStatusSwitchTurnStateTest.java`
3. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`，切到 turn-native context fixture。
4. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`，或由 38A 先完成相同旧断言清理。

**目标 old 文件与 business consumers**

- `CloudLeftTopStatusSwitchState.java`：38C 零写入，44A 删除。
- `LeftTopStatusSwitchService.java`、AutoBattle/AutoCombat/TaskMaintenance：零写入。
- 40B production：仍只创建权威计划的五文件；不增加 state-specific production 文件。
- 40B test：只在既有 `CloudTurnTaskRuntimeContractTest.java` 增加 runtime lifecycle integration。

这里与权威计划 `:1333` 的字面冲突必须由父级显式处理：该 target 行若记为 `DELETE`，38C 原定义会对它“什么都不做”；但 replacement 又必须在 40B 前进入 context。父级需要把这项写成“DELETE old file + 38C context replacement 子卡”或修订卡边界，不能让 Worker自行解释。

### 8.4 B 的生命周期

- Pause/resume：同 context 的 private bit 原样保留；无 state copy、resume context、revision 或 callback。
- Stop/terminal/exception：runtime finally 解除 holder、丢弃 context；不要求调用 consume/clear 来伪造业务完成。
- New start：新 context 的 field 初始为空；同 ID redelivery 只返回既有 ack，不新建 context。
- Cross-window：两个 context 各自一份 field；不按 windowId 在 static map 查询。
- 44A：old target 与 assembly/Service-context direct refs 一起删除；active context 对 target 零引用。

## 9. 共同生命周期冻结点

### 9.1 State scope 不能由 Worker猜

当前 old target 是 one old taskRun state；40B 则是一条 accepted queue 内为每个 Task 创建最终 context。父级必须明确：

- 推荐最窄等价形状是每个 concrete TaskExecutionContext 一份 state；pause/resume 保持该份，下一 concrete Task/new start fresh。
- 如果 696 队列语义要求 pending 跨 queue element 延续，必须把“runtime-wide shared state + context projection”作为显式业务差异评估；不能默默让所有 Task context 共用一个 runtime bit。
- 禁止 factory singleton、registry window map、static state 或跨 accepted start 复用。

### 9.2 Terminal release 是不可达，不是新业务动作

旧 assembly terminal 也没有专门 click/consume pending。新 runtime 应在 finally 丢弃 context/state reference；不要新增：

- terminal `clearPending(...)` 业务日志作为“已处理”证据；
- stop 时补一次 observation/click；
- release failure record 或后台 cleanup retry；
- TTL cleaner、timer 或弱引用清扫器。

### 9.3 Source 不是 identity

`pendingSource` 目前只让 null/blank 归一为 `unknown`，没有 getter，也不参与 consume。新实现不得将 source 扩成：

- 多条 ledger/history；
- action id / request id；
- task/session/window ownership key；
- retry 次数或 expiry 起点。

## 10. Named test 边界

为避免宽测试重复计算证据，建议父级冻结四层边界：

| Test | 只负责什么 | 不负责什么 |
|---|---|---|
| 路线 A `CloudLeftTopStatusSwitchTurnStateTest` 或路线 B `LeftTopStatusSwitchTurnStateTest` | initial false；mark/is/consume-once/clear；不同 context 隔离；同 context pause/resume 连续；fresh context false；source 不作 identity；无 clock/TTL/history | ROI、模板、JSON action、Task 编排、old coordinator/session |
| `LeftTopStatusTurnContractTest` | 696 Service 真值表；11x19 raw ROI；OPEN/CLOSED/UNKNOWN；exact-context pre-port reject；单 UUID；`MOVE/WAIT120/CLICK/WAIT250`；known failure/uncertain 不重试 | old assembly fixture、session/revision、runtime terminal/new start |
| `AutoBattleTaskTurnContractTest` | follower/capability gate、left-top 调用次数、post-left-top checkpoint、与 common-box/team maintenance 的既有顺序 | pending owner internals、ROI/action bytes、runtime activation |
| `CloudTurnTaskRuntimeContractTest` | same context/state across pause/resume；terminal 丢弃；new start fresh；duplicate ID 不二启；跨 window 隔离 | Service 真值表、template/ROI、增加第三个 runtime test |

`TaskExecutionContextOldAuthorityRemovalTest` 只应证明 new context 可运行、old retained authority 零调用与 checkpoint 语义；它不能替代独立 left-top lifecycle test。现有 `TaskExecutionContextTurnContractTest` 的相反断言必须显式改掉。

## 11. No-owner/session/ledger/TTL/retry 对照

| 项目 | A：旧类双 contract | B：context private bit |
|---|---|---|
| 独立 active owner surface | 有一个 public turn state 类型；必须降格为 context-owned bit holder | 无；只有既有 context field |
| old session/key 残留 | old half 保留到 44A；new path 必须零调用 | 仅 untouched old SCC 保留到 44A，active path 零引用 |
| runtime/registry map | 禁止 | 禁止 |
| generation/permit/handle | new path 禁止；old overload 暂存 | 无 |
| durable workflow/history | 无，且不得新增 | 无，且不得新增 |
| TTL/expiry | 无；120 秒 command timeout 不得转义为 TTL | 无；不得加 clock/timestamp |
| 自动 retry | 无；保留 TURN-19 一次 command 终态 | 无；保留 TURN-19 一次 command 终态 |
| 44A 处理 | 保留 target，删除 old half，防 broad delete | target 随 old SCC 整体删除 |
| 当前计划适配 | 符合 `KEEP_REWIRE` 字面，但有 dual API 债务 | 更窄，但需父级明确 38C replacement exception |

## 12. DAG 路线比较

### 12.1 A 的真实顺序

```text
34C/35/36/37 最终 consumer surface
  -> 38A 最终 TaskExecutionContext API/test ownership
  -> 38M 父级冻结 target KEEP + 两个 production consumer files
  -> 38C target dual API + context rewire + 独立 state test + TURN-19 fixture cutover
  -> 39 清 active old facade refs，但保留 old SCC 编译 half
  -> 40B runtime 创建/执行 final context，runtime contract 补 lifecycle
  -> 40C 激活
  -> 44A 删除 old SCC，并清 target old half但保留 active half
```

### 12.2 B 的真实顺序

```text
34C/35/36/37 最终 consumer surface
  -> 38A 最终 TaskExecutionContext API/test ownership
  -> 38M 父级冻结 target DELETE + 38C context replacement exception
  -> 38C context private bit + 独立 state test + TURN-19 fixture cutover；target 零写入
  -> 39 清 active old facade refs，old SCC 保持 dormant
  -> 40B runtime 只做既有 context lifecycle integration
  -> 40C 激活
  -> 44A 删除 untouched target 与 old SCC
```

### 12.3 不成立的顺序

```text
38M 把 target 记 DELETE
  -> 38C 什么都不做
  -> 39 不改 TaskExecutionContext
  -> 40B 只写五个 runtime 文件
  -> 期待 LeftTopStatusSwitchService 自动得到 state
```

这个顺序在当前类型图中没有编译/API 通道，会留下 turn-native 四个 pending API 继续抛错。

## 13. 非绑定路线建议

本 helper 条件性倾向路线 B 的 **`TaskExecutionContext` private per-context bit** 变体，理由是：

1. active path 不增加 public owner/state capability，不需要 session/key/handle/map。
2. production replacement 只改一个已有 context 文件；Service 与业务 Task 的真值表保持透明。
3. old target 可以原样随 44A SCC 删除，不需要同一文件在 38C/44A 两次清旧 half，也没有 broad-delete 保留风险。
4. pause/resume/terminal/new-start 直接等同于既有 context 生命周期，40B 不必维护第二份 state。
5. 路线 A 虽符合 38C 字面分类，但只能接受 dual API 到 44A；clean rewire 会提前撞 old SCC 与 TURN-39 写集。

这项倾向有一个不可省略的父级前提：必须先把“DELETE old file + 38C context replacement exception”写进冻结分类/consumer write set。若父级不接受这个计划边界调整，则路线 A 的双 contract 是当前 DAG 下唯一能保编译的实现形状；不能改用“B 延期到 40B runtime-only”作为折中。

本文不替父级选择 A/B，也不把上述建议写成卡片结论。

## 14. 建议父级冻结清单

1. 先完整交付并复读 38A 最终 `TaskExecutionContext`；当前 491 行 untracked snapshot 不能当未来稳定 API。
2. 34C/35/36/37 完成后重跑 production ref closure，尤其确认 startup `handleLeaderStartup/probeMemberStartup` 的 696 direct caller 归属。
3. 明确 A 或 B；禁止写成“38C 暂不处理，40B 自然接上”。
4. 若选 A，冻结 target public turn API、context-only creation、old overload 存续期、两个 production files、三个 test files及 44A old-half cleanup。
5. 若选 B，冻结 38C replacement exception、context private field 边界、目标文件零写入、三个 test files及 44A 整体删除。
6. 冻结 state scope：per concrete context 还是 runtime-wide；不得让 Worker因 queue 有多个 Task 自选共享/清理语义。
7. 冻结 pause/resume 保留同一对象，零 session、零 revision、零 replacement context。
8. 冻结 terminal/new-start：通过释放旧 context/state reference实现；不补 observation/click，不加 cleanup retry。
9. 冻结 696 真值表，尤其 safe-window 无论 pending 都 probe、unresolved 只保留既有 bit、known failure 不重发。
10. 明确 120 秒是 command timeout，不是 state TTL。
11. 冻结 source 仅为诊断字符串，不进入 identity/history/retry/expiry。
12. 明确 `LeftTopStatusTurnContractTest` 移除 old authority reflection fixture；它继续负责 mechanics/Service，不负责生命周期。
13. 明确独立 `*TurnStateTest` exact path，且不以 38A broad context test 或 40B broad runtime test替代。
14. 明确现有 `TaskExecutionContextTurnContractTest.java:523-534` 的修改卡，避免相反断言残留。
15. 39 必须保持 old direct SCC 的编译闭包至 44A；不得只改 `CloudTaskServiceExecutionContext` 造成中间假编译点。
16. 明文禁止 owner permit、session、ledger/history、durable workflow、TTL/expiry、自动 retry、static/window map、第二 exchange 或额外 runtime production 文件。

## 15. 本 helper 操作确认

- 只新增本报告。
- 未修改 Java、计划、CR、`ACTIVE_WORK.md`、dashboard 或其它报告。
- 未占用任何 Java production/test 写集。
- 未还原、移动、清理、暂存、提交或切换两仓既有 dirty/untracked 内容。
- 未运行 Maven、JUnit、runtime、application、server、Task、UI、capture 或 input。

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-38M LEFT TOP OWNER ROUTE RISK HELPER R1 PRECHECK_COMPLETE -->
