# CR271 TURN-38A 开工前 exact readiness preflight

## 0. 边界与当前结论

- 角色：CR271 Internal 非绑定 readiness helper。
- 审计时点：2026-07-16 06:55:24 -04:00；落笔前又复扫了实际 production/test refs。
- 唯一写入：本报告。
- 当前 precheck 结论：`REAL_BLOCKER`。
- `PRECHECK_CLEAR`：当前不成立；成立所需的父级冻结条件见第 11 节。
- 本结论只描述 TURN-38A 是否具备 exact 开工事实，不改变任何 CR/卡片状态，不代替父级冻结，也不自行选择兼容、扩写集或重排 DAG 的方案。
- 未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input；未做任何 Git mutation。

## 1. 已完整读取与复核的权威输入

1. `D:/mavenProject/DHXY/AGENTS.md` 全文。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md` 全文。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 全文，并在落笔前复核顶部 CR271 段 `:1-38`。
4. `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 全文，重点复核第 14-19 节及：
   - 覆盖规则 `:1035-1043`；
   - exact context 链 `:1047-1068`；
   - 卡注册表 `:1147-1165`；
   - TURN-13C 合同 `:1231-1244`；
   - TURN-38A exact write set `:1300-1312`；
   - R5 顺序 `:1428-1433`；
   - named test 表 `:1638-1650`。
5. `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` 全文，重点复核 client-initiated turn、`TurnWindowMetadata`、start/control、uncertain transport 与 Cloud/DHXY 权责边界。
6. `D:/mavenProject/DHXY/docs/业务逻辑.md` 全文，重点复核 `:215-224` 的迁移禁增项、`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 修罗基线及 stop/pause/terminal 语义。
7. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-39-readiness-preflight-helper.md` 全文及 TRUE_EOF。
8. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-38M-authority-classification-preflight-helper.md`，用于核对 TURN-38A 与 TURN-38C/LeftTop 的顺序交叉。
9. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-13C.md` 全文，重点复核 `:40-69` 与 `:190-211`。
10. DHXY 与 `D:/mavenProject/dhxy-cloud-brain` 两仓当前 status、相关 production/test 源码及所有命中的调用点。

## 2. 落笔前双仓和文件快照

### 2.1 仓库状态

| Repo | Branch | HEAD | `git status --short --untracked-files=all` 条目数 | 只读判断 |
|---|---|---|---:|---|
| DHXY | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 643 | dirty/untracked 很大；本 helper 未改动既有项 |
| Cloud | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 550 | dirty/untracked 很大；TURN-38A 七个 production 文件均为 `??` |

说明：643/550 是展开全部 untracked 文件后的只读计数。该快照期间另有 Java writer 活动，不能把当前工作树等同于 HEAD 基线，也不能据此认领文件 owner。

### 2.2 TURN-38A production exact write set 当前实体

权威计划 `:1302-1312` 只允许以下七个 production 文件：

| # | Cloud 相对路径 | 当前 Git 状态 | SHA-256 |
|---:|---|---|---|
| 1 | `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java` | `??` | `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` |
| 2 | `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java` | `??` | `3FA2729917449FBB75BF72614E46A223526EA2ACB53DC96351886559192C6F3B` |
| 3 | `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java` | `??` | `3F92DF4932C0D4C62556F121F7A056FC21CCA9C52F0C0C9ADE16B009D9A46E0F` |
| 4 | `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpointDecision.java` | `??` | `CB59EEB4BEC8CD4EF0A5AA3C0E770269F25BF77C0C4FE49095F6FF728DB99DE2` |
| 5 | `src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java` | `??` | `7942011CAC9053EDFDEC6C57251758398261D85FC4488662198E92AA35A08C44` |
| 6 | `src/main/java/com/bot/dhxy/task/GameTask.java` | `??` | `B4F575803E884A2297CB95FEABAA7B140E74A5F6FE9526047EED9BA6EAE46A48` |
| 7 | `src/main/java/com/bot/dhxy/task/template/BaseTaskTemplate.java` | `??` | `CD39187D89815CAF156737C283544FC3B9D587D3C9B52D74F5B1A09BAB0C7FBF` |

风险：七个文件没有可由 Cloud HEAD 直接恢复的 tracked baseline。父级在真正冻结 owner 前必须重新记录 hash/status，并确认当前 untracked 文件来自已交付 predecessor，而不是并行 writer 的临时中间态。

### 2.3 TURN-38A test exact write set

权威计划 `:1645` 只列：

- `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextOldAuthorityRemovalTest.java`

当前事实：该文件不存在。计划没有授权 TURN-38A 修改既有 13C test 或 LeftTop test。

## 3. `startDependsOn` exact 核验

权威注册表 `:1154` 冻结 TURN-38A 的直接依赖为：

`S=13C+34C+35+36+37`

### 3.1 直接依赖当前状态

| Dependency | 当前证据 | 对 38A 的结论 |
|---|---|---|
| TURN-13C | 卡 `:190-209` 已形成 source/test-source review 结论；named test 与 Cloud compile 未到达，但卡明确允许后续按 source dependency 滚动 | 可作为 source gate；不能伪造其 named test/compile 证据 |
| TURN-34C | ACTIVE_WORK `:3-28` 显示 34A writer 仍活动，34B 尚未 ready，34C 只能做 post-34A/34B readiness | 未满足 |
| TURN-35 | 注册表 `:1151` 为 `PLANNED`；当前 whole-task source 未稳定 | 未满足 |
| TURN-36 | 注册表 `:1152` 为 `PLANNED`；当前 whole-task source 未稳定 | 未满足 |
| TURN-37 | 注册表 `:1153` 为 `PLANNED`；当前 whole-task source 未稳定 | 未满足 |

因此，仅按已冻结的直接依赖，TURN-38A 当前也不是 `PRECHECK_CLEAR`。

### 3.2 直接依赖清单本身还不足

计划 `:1311` 限定 38A“只清掉已经没有 caller 的 old retained-authority 依赖”。实际源码中，以下 caller 由 **38A 的后继卡** 才计划处理：

- TURN-38B1：`CloudBagStateOwner`、`BagWorkflowState`；注册表 `:1155` 又依赖 38A。
- TURN-38B2：`CloudReturnItemPrescanStateOwner`；注册表 `:1156` 又依赖 38A。
- TURN-38B3：`CloudStartupGateAuthority`；注册表 `:1157` 又依赖 38A。
- TURN-38C：`LeftTopStatusSwitchService` 的 pending state consumer；注册表 `:1159-1160` 要求先 38A，再 38M 父级分类，再 38C。

这形成真实顺序倒置：38A 要求“零 caller”才能删，caller 的当前 owner 却依赖 38A 完成后才可改。它不是靠等待 34C/35/36/37 就会自然消失的瞬时问题。

## 4. `TaskExecutionContext` old authority surface 当前实物

`TaskExecutionContext.java` 当前同时包含 turn-native powerless context 和 old retained-authority delegate：

- `:9-15` 导入 old `CloudGameClient`、`CloudTaskServiceExecutionContext`、`RemoteGameCommandBroker`、`RemoteTaskRunAuthorization`、`RemoteTaskRunScope` 等类型。
- `:35` 持有 old `delegate`。
- `:51-59` 保留 `TaskExecutionContext(CloudTaskServiceExecutionContext)` 构造器。
- `:96-109` 提供 `turnNative(...)`，但 production 当前没有调用该 factory。
- `:204-227` 暴露 `getScope/getPlayerIdentityEpoch/getStopEpoch/getRunRevision`。
- `:229-244` 的 turn scope/invocation getter 在 legacy context 上仍回读 old delegate。
- `:254-315` checkpoint/revalidate 同时保留 turn-native 与 legacy 分支。
- `:319-332` 暴露 old game/remote client。
- `:335-352` 暴露 LeftTop pending state API。
- `:438-455` 通过 `legacyDelegate(...)` 和 `oldAuthorityUnavailable(...)` 维持双路由。

`TaskCheckpointDecision.java` 也仍有编译级 old authority coupling：

- `:3` 导入 `RemoteTaskRunStatus`；
- canonical record 的 `currentStatus` 在 `:20` 保留该类型；
- `CloudTaskRunCurrentContextSlot.java:347` 与 `RemoteTaskRunCoordinator.java:1065` 在 38A 写集外直接构造该 record。

所以 38A 不能在七文件内自行改变 record shape；这同时被 TURN-13C 卡 `:65-66` 明确冻结。

## 5. 所有 active production caller 与归属冲突

以下清单以落笔前 `rg -n` 全量扫描并逐个核对接收者类型为准。`ScopedPngArtifactStore.java:205` 的 `revalidate()` 接收者是 `CloudTaskServiceExecutionContext`，不误计为 `TaskExecutionContext` caller。

### 5.1 应由已声明 predecessor 消除，但当前仍存在

| Caller | old surface | 精确证据 | 预期 owner |
|---|---|---|---|
| `NavigationService` | `getGameClient()` | `NavigationService.java:564` | TURN-27，经 35/36/37 进入 38A 的传递前置 |
| `TaskMaintenanceService` | `getPlayerIdentityEpoch()` | `TaskMaintenanceService.java:1022` | TURN-34B，经 34C 进入 38A 的传递前置 |
| `FiveRingTaskV2` | context-free old builder | `FiveRingTaskV2.java:2751-2756` | TURN-36 |
| `WubeiTask` | context-free old builder | `WubeiTask.java:4250-4255` | TURN-35 |
| `XiuluoTaskV2` | context-free old builder | `XiuluoTaskV2.java:3889-3894` | TURN-37 |

三处 `TaskExecutionContext.builder()` 与当前 context 类本身没有 builder，说明 whole-task predecessor 尚未到达稳定交付态。38A 开工前必须重新扫描并确认这些调用已由各自 owner 真正清零。

### 5.2 38A 自有可清理 caller

| Caller | old surface | 精确证据 |
|---|---|---|
| `BaseTaskTemplate` | old revision/stop diagnostics | `BaseTaskTemplate.java:195-196` |
| `TaskExecutionContext` | old delegate、构造器、old getters、pending methods | `TaskExecutionContext.java:35,51-59,204-352,438-455` |

其余五个 38A 文件主要是语义保持面，不应为了删除 old authority 顺手改变 checkpoint/sleep/template 生命周期。

### 5.3 后继卡 caller，构成 DAG 顺序倒置

| Future owner | 当前 caller | old surface 精确证据 |
|---|---|---|
| TURN-38B1 | `CloudBagStateOwner` | scope `:107,113,155,205,224,242,263,280,285,318,324`；stop/revision `:116-117,158-159,397`；revalidate `:394`；identity epoch `:442` |
| TURN-38B1 | `BagWorkflowState` | revision `:539,622`；scope/stop/identity `:609-616`；revalidate `:619` |
| TURN-38B2 | `CloudReturnItemPrescanStateOwner` | revision `:237,421,501,630`；revalidate `:625`；scope/stop `:634-636`；identity epoch `:676` |
| TURN-38B3 | `CloudStartupGateAuthority` | scope `:127,164,241,254`；identity/stop/revision `:247-249,260-262` |
| TURN-38C 候选 | `LeftTopStatusSwitchService` | pending mark/clear/read/consume `:73,75,92,96,98,100,242` |

这些方法若在 38A 真删除，后继卡尚未运行时 Cloud main source 立即失去符号；若继续保留，则“old retained authority 最后引用清零”不能按字面完成。父级必须先选定顺序或兼容边界，helper 不代选。

### 5.4 当前没有明确 pre-38A owner 的 active caller

`CommonBoxService.java:448,460` 两处直接调用 `TaskExecutionContext.getPlayerIdentityEpoch()`。当前 38A、38B、38C exact write set 均未包含该文件；TURN-38M 对 `CommonBoxStateGovernor` 的 DELETE 候选也不会自动删除 `CommonBoxService` 的这两个 production caller。

这是独立的 owner 缺口，必须由父级明确归属；不能把它默认为 38A 七文件内可解决。

### 5.5 44A 前保留的 old SCC 仍依赖这些符号编译

| Old SCC caller | 精确证据 |
|---|---|
| `CloudTaskRunAuthorityAssembly` | `new TaskExecutionContext(serviceContext)` 于 `:220`，restart 路径于 `:300` |
| `CloudGameContextStateOwner` | revision/scope/identity/stop 于 `:62,75,82,94,122,151,153,182,435,442-443` |
| `CloudPlayerStateStateGovernor` | revision/scope/identity/stop 于 `:89,250,289,944,1463,1468,1480-1482,1511,1518-1519` |
| `CloudTaskTurnAuthority` | revision/scope/identity/stop 于 `:675,817,824-826` |
| `CloudTaskRunCurrentContextSlot` | revision/scope/identity/stop 于 `:74,229-230,312,349,529,536-537` |
| `CloudTaskRunRetainedLifecycleActivationAdapter` | revision 于 `:159,298` |

这些旧类按当前迁移计划不是 38A 可写/可删文件，且总体删除在 44A cohort。即使部分方法尚未被未来 runtime 激活，它们仍是当前 main source 的真实编译 caller。

### 5.6 当前唯一 production context 构造路径仍是 old route

- production 中 `TaskExecutionContext.turnNative(...)` 调用数：`0`。
- production 中 `new CloudTaskServiceMetadata(...)` 调用数：`0`。
- production 中明确构造 context 的位置只有 `CloudTaskRunAuthorityAssembly.java:220,300` 的 old `CloudTaskServiceExecutionContext` 构造路径。
- `LegacyTaskExecutionTurnContextProvider.java:25-29` 从 Holder 取 context，再调用 `getTurnInvocationContext()`。
- `TurnGameClient.java:73-80,116-176` 的 bound client 会在 UUID/port 前读取 provider exact context；该保障成立的前提仍是 Holder 中已有可用 context。
- production Task factory 按计划留在 TURN-40B，见计划 `:1244` 与注册表 `:1163`。

因此，在 TURN-40B 之前直接删掉 old constructor/delegate，不只是测试冲突，还会让当前唯一 production assembly 无法构造 context。这个事实必须与 38A 位于 40B 之前的 DAG 一并由父级解决。

## 6. 现有 test ownership 冲突

### 6.1 TURN-13C named test 与 38A 字面删除冲突

现有 tracked clean 文件：

- `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`
- SHA-256：`D667D6958DBC38A6FCCF2BA5E562CECD4EF60629DF7A4CD55E347C9DBD9ED945`

其当前合同明确锁定：

- `:494-506` 反射要求 old constructor、`getScope()`、`getGameClient()`、`getRemoteGameClient()` 等 public surface 仍存在；
- `:516-534` 要求这些 old-authority API 在 turn-native context 上精确 fail-closed，而不是删除方法；
- `:208-245` 锁定 Holder nested A -> B -> A 与异常恢复；
- `:247-292` 锁定 bound client 在 UUID/port 前拒绝 missing/wrong context；
- `:338-398` 锁定 active/stop/pause/mismatch；
- `:400-492` 锁定 checkpoint/sleep overload、interrupt 及 combined same-context 的现有双 checkpoint 行为。

TURN-13C 卡 `:42-43,67-69` 同样明确要求保留旧构造器/旧 public surface；TURN-38A 新 test 却要求 old retained authority 零调用。若 38A 真删 API，而不获得既有 test 的修改/退役 ownership，Cloud 全测试源码会在新 named test 之外先发生合同冲突。

### 6.2 LeftTop 既有合同测试是第二个真实冲突

现有 tracked clean 文件：

- `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`
- SHA-256：`C9D0B21AEC3637452E0507F0F716C43E9F8CD21010368EBA249012BE3C66EF8A`

该测试直接调用 old pending API：

- `:96,108,117,127,146,160,171,181,193,202,208,217,223,233`。
- `:540-617` 的 fixture 还通过 old broker/ledger/authority assembly、`RemoteTaskRunScope` 与 `CloudTaskServiceMetadata` 构造 legacy `TaskExecutionContext`。

它属于既有 LeftTop/TURN-19 合同，不在 38A test write set。由于 38C 又排在 38A 后，38A 无法在现有授权内同步迁移此测试。

### 6.3 metadata fixture 影响面

`new CloudTaskServiceMetadata(...)` 当前 production 为 0，test 为 19 处、分布在 17 个测试文件：13C、AutoCombatPanel、BattleRadar、ClientIdentity、CommonBox、DialogDetection、DialogOption、LeftTop、PlayerState、ReturnItemPrescan、SummonSkill、TaskTrackerPanel、TeamReturn、UiCleaner、Wubei/FiveRing/Xiuluo tracker tests。

如果父级冻结新的 metadata authority/factory 并改变 construction contract，这 17 个测试文件中受影响者也必须获得明确 owner；不能把 fixture 迁移隐含塞进 TURN-38A 唯一新 test。

## 7. metadata authority 来源核验

### 7.1 当前结构

`CloudTaskServiceMetadata.java:29-43` 持有：

- task code/name；
- requested task code/name；
- window role；
- local team session key、leader window id、leader present、support member；
- retry policy；
- startup mode；
- startedAt。

`TaskExecutionContext.java:100` 的 turn-native factory 要求调用者提供完整 `CloudTaskServiceMetadata`，其业务 getter 在 `:113-201,364-372` 直接读取这些字段。当前 production 没有该 record 的构造点，也没有 production turn-native context factory。

### 7.2 实际业务消费者

- `AutoCombatService.java:480-656` 读取 requested task、role、local-team session/leader/presence。
- `TeamReturnService.java:388-400` 读取 task/requested task、role、local-team fields。
- `TaskMaintenanceService.java:323-520,1061-1077` 读取 team presence/support/session/leader/task。
- `CommonBoxService.java:371-374` 读取 role。
- `LeftTopStatusSwitchService.java:139,250-253` 读取 role/requested task/task。
- `AutoBattleTask.java:189-270` 读取 session/requested task/role。
- `XiuluoTaskV2.java:351-352,3850,3970` 读取 startup mode/role。
- `WubeiTask.java:377,1285` 读取 startup mode。
- `TaskStepExecutor.java:72-73` 与 `BaseTaskTemplate.java:142-144` 读取 retry policy。

### 7.3 协议不能自动补齐这些字段

HTTPS turn 协议中的 `TurnWindowMetadata` 提供 device/window/title/HWND/process/rect/pause/stop 等 exact-window 状态；start request 只承载稳定身份、ordered task codes 与 failure policy。协议没有为 local team role/session/leader/support、task retry policy、startup transition、startedAt 定义权威字段。

`docs/业务逻辑.md` 的 local-team 基线又禁止根据请求或窗口动态推断/附着团队关系。因此不得在 38A 临时从 task code、window title 或 request body 伪造这些 metadata。

父级必须在 38A 开工前冻结以下来源映射，至少逐字段说明：

1. `taskCode/taskName/requestedTaskCode/requestedTaskName` 由哪个 queue/task definition/start snapshot 产生。
2. `windowRole/localTeamSessionKey/localLeaderWindowId/localLeaderPresent/localSupportMember` 由哪个既有、非推断的 local-team registration snapshot 产生。
3. `retryPolicy/startupMode/startedAt` 由哪个 task invocation/transition owner 产生，并证明不改变 696a12b0 次数、顺序、fallback 或 expiry。
4. 谁在 TURN-40B 之前或之后调用 production `TaskExecutionContext.turnNative(...)`，以及 38A 为何不会先删掉唯一可用构造路径。

在该映射未冻结前，“new context 可运行”只能由测试 fixture 证明，不能证明真实 production construction chain 闭合。

## 8. A -> B -> A / exact context 必须冻结的验收点

以下为根据当前 13C 语义、协议和业务基线得到的最低验收矩阵，不是对实现方式的批准。

### 8.1 Holder A -> B -> A

1. 初始 Holder 可以为空，也可以已有 sentinel；退出后必须恢复原值。
2. 绑定 A 时，provider 只能返回 A 的 exact `deviceId/windowId`。
3. A 内嵌套绑定 B 时，provider 只能返回 B。
4. B 作用域中调用 A-bound `TurnGameClient` 必须在 UUID、metadata port、command port 之前拒绝。
5. B 正常退出恢复 A；B 抛异常也恢复 A。
6. 恢复 A 后，A-bound client 仍可执行；不能残留 B 的 client、metadata 或 task identity。
7. A 正常返回、STOPPED/FAILED/SKIPPED terminal、`TaskStopRequestedException`、typed transition、普通异常的每条路径都必须恢复进入 A 前的 Holder。
8. 不创建 global fallback、第二 provider、ThreadLocal cache、替换 context 或跨线程继承。

### 8.2 exact device/window 与 action 前门

1. 初始 `TurnWindowMetadata` 的 device/window 必须与 `TurnInvocationContext` 完全相等，title/HWND 非空，pid/rect 合法。
2. 每次 bound public action 和 `latestWindowMetadata()` 都重新核对 Holder 中的 exact identity，且核对发生在 UUID/port 前。
3. missing holder、wrong device、wrong window、wrong nested context 都必须无 action、无 UUID、无 port call。
4. latest metadata empty 或 device/window mismatch 必须形成 typed transition；不能当 ACTIVE，不能重放业务动作。
5. STOP 优先于 PAUSE；PAUSE 以现有 250ms cooperative cadence 在同一 context 中等待 resume/stop，并返回真实 blocked milliseconds。
6. transport uncertainty 不得触发 Cloud 重执行；不得新增 retry、TTL、park/yield、额外 read/verification 或 replacement context。
7. fixed tenant/user 只能来自冻结的 Cloud service scope；不得从 request body 接受 tenant/user，也不得据窗口 metadata 推断 team role。

### 8.3 checkpoint 与 sleep exact 语义

1. `TaskCheckpoint.checkpoint(explicit, source)` 仍先检查 explicit context，再检查 Holder context，最后检查 thread interruption。
2. 当前 13C test `:479-491` 明确锁定 explicit 与 Holder 为同一 paused context 时发生两次 checkpoint；38A 不得擅自“去重优化”。
3. null explicit context 仍合法，仅由 Holder/interrupt 形成结果。
4. `TaskSleep.sleepOrStop(..., duration<=0)` 当前直接返回，不执行 checkpoint；正数 sleep 才执行前后 checkpoint。
5. interrupted flag 必须恢复，并映射为 `TaskStopRequestedException`；STOP 不能被改写成 business FAILED。

## 9. `BaseTaskTemplate` terminal 语义验收点

计划 `:1311-1312` 明确禁止改变业务条件、次数、顺序和异常语义。当前实现必须按以下实物保持：

1. `execute()` 无 context 时保持 typed missing-context 行为，不得新建 context 或回退 global runtime。
2. `execute(context)` 通过 Holder `callWith` 包裹完整 lifecycle，所有返回/异常路径都恢复旧 Holder。
3. `beforeTask` 后执行 checkpoint；空 step 列表返回 `SKIPPED`，当前不调用 `afterTask`。
4. step `SUCCESS/SKIPPED` 继续下一步；全部完成后 `afterTask(SUCCESS)` 恰好一次。
5. step `FAILED/STOPPED` 时 `afterTask` 恰好一次并返回同一 terminal。
6. `TaskStopRequestedException` 映射 `STOPPED`，`afterTask(STOPPED)` 恰好一次，不能映射 `FAILED`。
7. `TaskCheckpointTransitionException` 原样向外传播，当前不调用 `afterTask`，不得吞掉或改成普通 terminal。
8. 其他异常映射 `FAILED`，`afterTask(FAILED)` 恰好一次。
9. 不改变 step 数、retry/fallback、phase 顺序、navigation/click/OCR/input 次数，也不因 turn negative signal增加业务真值。

## 10. 当前 `REAL_BLOCKER` 证据汇总

| ID | 事实 | 为什么是开工前真实阻断条件 |
|---|---|---|
| R38A-DEP | 34C/35/36/37 尚未 source stable | 直接 `startDependsOn` 未满足 |
| R38A-DAG | 38B1/B2/B3/38C 的 production caller 仍调用待删 API，但这些卡反向依赖 38A | 单纯等待当前 predecessor 不能消除，必须父级重排或冻结兼容边界 |
| R38A-SCC | old assembly/current-slot/turn-authority/state owners 仍靠 old constructor/scope/revision/epoch 编译，计划到 44A 才删 | 七文件内真删会破坏写集外 main source |
| R38A-PROD | production `turnNative(...)` 与 metadata constructor 均为 0，唯一 context 构造来自 old assembly | 38A 位于 40B factory 前，当前没有可替代 production construction chain |
| R38A-OWNER | `CommonBoxService.java:448,460` 没有明确 pre-38A owner | “所有 active caller 清零”存在漏项 |
| R38A-TEST | 13C test 要求旧 public surface 存在；LeftTop test 直接调用 pending API；两者均不在 38A test 写集 | 新 named test 不能单独代表全测试源码兼容 |
| R38A-META | turn-native metadata 只有测试 fixture，没有 production authority/source mapping | new context 的真实业务身份、team、retry、startup 语义无法无损构造 |
| R38A-WORKTREE | 七个 production 文件全为 untracked，且并行 Java writer 活动 | 开工 owner/hash 尚不能安全冻结，必须保护当前 dirty/untracked |

## 11. 父级冻结清单与 `PRECHECK_CLEAR` 条件

只有以下项目全部得到父级明确、持久化冻结并经最新源码复扫成立，helper 才能把后续 precheck 记为 `PRECHECK_CLEAR`：

1. 确认 TURN-13C 的 source gate 使用口径，同时不伪造其尚未到达的 named test/compile 证据。
2. 34C、35、36、37 各自完成 source-stable 交付；复扫确认 Navigation、Maintenance、三个 whole-task builder 的旧 caller 已清零。
3. 对 38A 与 38B1/B2/B3/38C 的顺序倒置做唯一选择：重排 caller rewire、扩大前置写集，或冻结一个精确定义的 source-compatible shell。不得由 38A worker临场选择。
4. 对 44A 前 old SCC 做编译策略冻结：哪些旧符号必须保留到删除 cohort，哪些可在 38A 真删；同时重写 38A “old authority removal”的负断言边界。
5. 为 `CommonBoxService.java:448,460` 指定唯一 owner 和在 38A 前后的精确顺序。
6. 冻结 13C test 与 LeftTop test 的 owner：修改、拆分、退役或迁移必须进入明确 test write set；不得让 38A 只新增一个测试却暗中破坏既有测试源码。
7. 冻结 `TaskExecutionContextOldAuthorityRemovalTest` 的 exact assertions：source symbol removal、runtime zero-call、允许的 old SCC 清单、A -> B -> A、exact context、checkpoint/sleep、terminal 每项分别说明。
8. 冻结 `CloudTaskServiceMetadata` 每字段 production authority、构造 owner、factory 调用时点与 17 个 fixture tests 的 ownership。
9. 冻结 production context 构造切换点：明确 38A 与 TURN-40B factory 的先后如何保证任何中间 revision 都可编译，并且不会创建第二 authority。
10. 重新记录七个 production 文件与相关 tests 的最新 SHA/status，确认无并行 writer ownership 冲突后再 claim。
11. 固定“无已批准业务差异；按 `696a12b0` 与 exact-context HTTPS turn 等价迁移”，并逐项确认无 TTL、额外 read/verification、retry、park/yield、cleanup/fail-closed 扩张或 terminal 次数变化。

## 12. 非绑定建议供父级选择时核对

这不是分类冻结，只是避免父级漏看真实后果：

- 若父级坚持 38A 字面删除 old constructor/delegate/public authority APIs，则 caller rewire、old SCC 编译边界、13C/LeftTop tests 和 production factory 必须先于或并入可编译的原子 cohort。
- 若父级坚持当前 DAG 不变，则 38A 只能删除“已无 caller”的子集，并保留写集外 caller 所需的兼容符号；此时 named test 不能笼统断言仓库级 old symbol 为零，必须冻结精确 allowlist 和后续最终删除卡。
- 这两条不能同时被含混采用；否则 worker 无法知道“最后引用删除”指 source symbol、turn-native runtime zero-call，还是 old SCC 之外的 active caller zero-call。

## 13. 只读执行记录

- 只读使用了 `Get-Content`、`rg`、`git status`、`git branch --show-current`、`git rev-parse HEAD`、`Get-FileHash`、`Get-Item` 与 `Get-Date`。
- 未执行 Maven/JUnit/compile，也没有启动任何 runtime/application/server/Task/UI/capture/input。
- 未执行 add/commit/checkout/reset/clean/stash/rebase/merge 或任何 Git mutation。
- 未改 Java、测试、权威计划、CR271、ACTIVE_WORK 或其他报告。
- 除本报告外没有写入文件。

<!-- TRUE_EOF: CR271 TURN-38A READINESS PREFLIGHT REAL_BLOCKER -->
