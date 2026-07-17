# TURN-34BP2 post-BP1 readiness delta

## 1. 角色、用途与边界

- 角色仅为 `CR271 Internal helper`。本文不是 implementation delivery、owner claim、review、父级裁决或 CR 状态变更。
- 本文只冻结 `TURN-34BP2` 在 `TURN-34BP1` 获得显式 parent source receipt 之后可采用的依赖、精确写集、公共 API、terminal/UUID 验收以及同文件冲突。
- 本轮唯一写入为本文件：
  `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-34bp2-readiness-delta-helper.md`。
- 本轮未修改 DHXY/Cloud Java、测试、CR 卡、`docs/ACTIVE_WORK.md`、权威计划、协议、业务逻辑文档、dashboard 或其他文件。
- 本轮未运行 Maven/JUnit/compile/package，未启动 runtime/application/server/Task/UI/capture/input，未执行任何 Git mutation。
- 本文不输出 review verdict。卡内已有判断只按来源和时间引用，不转化为本 helper 的批准或否决。

**无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。**

## 2. 已完整读取的权威材料

本 helper 已完整读取并交叉核对以下材料：

1. `D:\mavenProject\DHXY\AGENTS.md`。
2. `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`。
3. `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` 中 CR271 当前连续工作区及其历史基线段。
4. `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-15-https-turn-complete-migration-card-plan.md` 权威 Sections 14-19。
5. `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-15-https-turn-thin-client-protocol-design.md`。
6. `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-15-https-turn-protocol-foundation.md`；其冲突处按 Sections 14-19 优先。
7. `D:\mavenProject\DHXY\docs\业务逻辑.md`，包括五倍/修罗 `696a12b0...` 基线、local-team session、CommonBox、Summon static-tail/UNKNOWN 规则。
8. TURN-34B 父卡、TURN-34BP1 子卡、TURN-34BT1 子卡及其 physical true EOF。
9. 既有 BP2/BP3 readiness helper、TURN-34B retained-production/minimal-source/test-slice 材料。
10. Cloud 当前 `TaskExecutionContext.java`、`TaskExecutionContextHolder.java`、`TaskMaintenanceService.java`。
11. 当前 `TaskExecutionContextTurnContractTest.java`、`AutoCombatServiceTurnContractTest.java`、`SummonSkillTurnContractTest.java`，并核对相关 named-test 清单与 `TaskMaintenanceTurnContractTest.java` 的存在性。
12. DHXY 与 Cloud 两仓只读 `git status`、branch、HEAD，以及相关文件 SHA-256/行数。

## 3. 快照与相对旧 preflight 的 delta

初始只读快照：`2026-07-16T11:30:31.184-04:00`；写后并发复核：`2026-07-16T11:34:17.825-04:00`。

| 项目 | 当前只读事实 |
|---|---|
| DHXY | branch `thin-client-design`；HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；helper 写入前 `git status --short` 85 项，随后只新增本报告这一 untracked 项 |
| Cloud | branch `navigation-migration`；HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；`git status --short` 28 项 |
| `TaskExecutionContext.java` | 最终复核为 527 行 / 22,204 bytes / SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` |
| `TaskExecutionContextTurnContractTest.java` | 最终复核为 872 行 / 43,936 bytes / SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` |
| retained `TaskMaintenanceService.java` | 1,224 行 / 66,012 bytes / SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc` |
| `TaskMaintenanceTurnContractTest.java` | 不存在 |
| fixed `TURN-34BP2` child card | 不存在；旧 readiness helper 不是 implementation card |

相对 `2026-07-16-turn-34bp2-readiness-preflight-helper.md`，有以下新事实：

1. BP1 已不再是零 owner/零 delivery。External C 已先后交付初版、Repair #1 和 Repair #2；Repair #1 卡内 canonical delivery 为 production `f278460b...`、test `7caf0127...`。
2. `docs/ACTIVE_WORK.md` 11:23 段记录的 test `844` 行 / `2ed...` 是写入中的中间快照；BP1 子卡随后写明的 canonical Repair #1 delivery 是 `843` 行 / `7caf...`，本报告只把后者当作该次交付快照。
3. BP1 physical true EOF 在初始快照先追加父级 `PARENT DELIVERY REVIEW #2 / REPAIR #2 REQUIRED`，时间为 `2026-07-16T11:26:00-04:00`。该段冻结 context-local monotonic native-generation latch 的 production 逻辑，并要求 class-level description 和确定性测试证据增量。
4. 并发复核发现 External C 于 `11:31:10` claim Repair #2，并于 `11:33:19` 写入 canonical `SOURCE+TEST DELIVERED`：production 527 行 / `a9c34d4...`，test 872 行 / `3b117895...`。卡内声明 production 仅改 class JavaDoc，逻辑零改；本 helper 只把这些记录成待父级读取的最新 delivery facts，不独立复核其结论。
5. `2026-07-16T11:34:17.825-04:00` 的 physical true EOF 正是上述 Repair #2 delivery，尚无其后的 parent-authored source receipt。External C 的自述 delivery 不能替代父级 receipt。
6. 因此 BP1 的候选只读 artifact 已从 `f278...` / `7caf...` 更新为 `a9c...` / `3b117...`；只有父级 receipt 才能把相应最终 SHA 冻结给 BP2 使用。若后续再要求 repair，继续以最新 canonical delivery 为准。
7. retained `TaskMaintenanceService.java` 仍保持旧 preflight 冻结的 `963b...`，尚未出现 BP2 Java 增量。

因此，本 helper 仍不 claim BP2。这里只定义将来满足条件时的机械入口，不把 BP1 的部分冻结解释成完整 source pass。

## 4. BP2 精确启动依赖

未来 BP2 implementation owner 只能在下列条件同时满足后 claim：

1. **BP1 最新 canonical delivery 已落卡。** 当前候选是 Repair #2 production `a9c34d4...` / test `3b117895...`；若父级再要求 repair，则必须等待新的 canonical delivery，不能回用 Repair #1 或较早 SHA。
2. **BP1 parent source receipt 已落卡。** 在最新 canonical delivery 之后，父级必须于 physical true EOF 明确记录 source/test-source pass 或同义 receipt，且其后没有新的 repair-required 段。完整双 reviewer、named test、compile/build 可作为 CR271 后续 gate，但 BP2 不跨过仍开放的 BP1 source/test-source review。
3. **BP1 最终只读基线已冻结。** BP2 卡记录 receipt 对应的最终 `TaskExecutionContext.java` 与 `TaskExecutionContextTurnContractTest.java` SHA；BP2 不修改这两个文件。
4. **上下文公共合同稳定。** BP2 只消费最终 BP1 的既有 `getTurnServiceScope()`、`getTurnInvocationContext()` 和 `throwIfStopRequested()`；不复制 latest metadata fence，不读 private latch，不另建 generation truth。
5. **retained production 未漂移。** claim 前重新计算 Cloud `TaskMaintenanceService.java`。若仍为 1,224 行 / `963b...`，可把它写成 BP2 起始 SHA；若不一致，先重新读取完整 diff、最新父卡/BP3/测试卡，再冻结新起点，不在旧快照上叠写。
6. **fixed child card 已创建。** 父级先创建固定 `TURN-34BP2` implementation card，写明 owner、起始 SHA、两项写集、业务不变声明、source-delivery acceptance 和 owner release 规则；本 helper 文件不充当该卡。
7. **同文件 owner 唯一。** claim 时不得有 TURN-34B retained-production owner、BP3 owner或其他 lane 正在写 `TaskMaintenanceService.java`。旧 retained WIP 是输入基线，不是并行 owner。
8. **测试 lane 状态显式。** 当前 `TaskMaintenanceTurnContractTest.java` 不存在且 BT1 已 owner-returned。若其后测试 lane 已 claim，BP2 卡必须记录测试针对的 production SHA 与串行 rebase/handoff 点；source owner 不写该测试。
9. **最终 gates 不前置伪造。** TURN-22/33/34A 的最终门、两 reviewer、唯一 named-test 命令、Cloud compile/build 仍由相应卡负责。BP2 source slice 不把未运行项写成通过，也不自行扩大 scope 去关闭它们。

## 5. 两层精确写集

### 5.1 本 helper 当前写集

恰好一个文件，即本报告。没有其他写入。

### 5.2 未来 BP2 implementation 写集

恰好两项：

1. 修改 Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`。
2. append-only 更新父级将来创建的固定 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP2.md`。

Future BP2 明确只读：

- `TaskExecutionContext.java`、`TaskExecutionContextHolder.java`、`TaskExecutionContextTurnContractTest.java`；
- `TaskMaintenanceTurnContractTest.java` 以及所有现有 tests；
- AutoCombat/AutoBattle/Wubei/Xiuluo callers；
- Dialog、SummonSkill、TeamReturn、CommonBox、PlayerState service/test；
- protocol/client/result/model/POM、DHXY Java、runtime 配置；
- TURN-34B/BP1/BP3/BT1 卡、`ACTIVE_WORK.md`、权威计划、协议和业务逻辑文档，除非父级另开明确文档 owner。

## 6. BP2 typed-key foundation 冻结

### 6.1 只迁移四个共享 team/session/claim map

| 当前字段 | BP2 冻结后的泛型 |
|---|---|
| `activeTeamRoundByKey` | `Map<ScopedTeamKey, Integer>` |
| `teamMaintenanceWindowStateByRound` | `Map<TeamRoundKey, TeamMaintenanceWindowState>` |
| `localTeamSessions` | `Map<ScopedLocalSessionKey, LocalTeamSessionState>` |
| `summonSkillClaimsByTeamRound` | `Map<MaintenanceClaimKey, Set<ScopedWindowKey>>` |

以下四个 per-window Summon map 在 BP2 保持现状，留给 BP3：

```java
Map<String, Long> lastSummonSkillCleanAtByWindow
Map<String, Long> lastSummonSkillNotDueLogAtByWindow
Map<String, Long> summonSkillUnknownRetryAfterByWindow
Map<String, SummonSkillWindowState> summonSkillStateByWindow
```

BP2 不修改 `currentWindowKey`、`scopePrefix`、`currentIdentityToken`、`summonSkillState`、cache purge 或 per-window fingerprint/generation。

### 6.2 私有 key types

私有 key types 位于 `TaskMaintenanceService` 文件底部，冻结为以下结构职责：

```java
sealed interface ExecutionScopeKey
record ExactExecutionScopeKey(String tenantId, String userId, String deviceId)
enum NoContextScopeKey { INSTANCE }

record ScopedWindowKey(ExecutionScopeKey scope, String windowId)
record ScopedLocalSessionKey(ExecutionScopeKey scope, String sessionKey)

enum TeamCoordinationKind { WINDOW, LOCAL_SESSION }
record ScopedTeamKey(
        ExecutionScopeKey scope,
        TeamCoordinationKind coordinationKind,
        String coordinationKey,
        String maintenanceKey)

sealed interface MaintenanceClaimKey
record TeamRoundKey(ScopedTeamKey teamKey, int round)
record LocalCapabilityRoundKey(
        ScopedLocalSessionKey sessionKey,
        TeamSupportCapability capability,
        int epoch)
```

名称可由 fixed BP2 card 在 claim 前最终核对，但结构语义不得退回 delimiter string。

### 6.3 key 解析与共享语义

1. supplied `TaskExecutionContext` 永远优先于 holder；只有 supplied 为 null 才读取 holder。
2. turn-native 和 legacy context 均通过公开 `getTurnServiceScope()` 与 `getTurnInvocationContext()` 取得 tenant/user/device/window authority。
3. 只有 supplied 为 null 且 holder 为空时使用 `NoContextScopeKey.INSTANCE`。已有 context authority 失败不得被 broad `catch (RuntimeException)` 降级成 unscoped/no-context。
4. 同 scope、同显式 local-team session 的不同窗口共享 local session/capability/round claim；同 session 文本但 tenant/user/device 不同必须隔离。
5. 没有显式 local-team session 时，coordination kind 为 `WINDOW`，不同窗口隔离；不能用 raw task/team 文本把窗口并成全局 team。
6. maintenance key fallback 顺序保持现有业务：显式 `teamMaintenanceKey`，然后 requested task，再 task，最后既有 default。BP2 只换 key 表示，不改 fallback 条件和顺序。
7. formal claim 使用 `TeamRoundKey`；local capability claim 使用 `LocalCapabilityRoundKey`。二者通过类型区分，不能别名。
8. claim acquire/release/retain 各自仍只有一个真实 map 决策，不加 exact/fallback dual map、dual lookup 或兼容旁路。
9. 标识符内出现 `|`、`#`、`local-team:` 时仍由 record equality 区分；禁止 tuple delimiter concat、prefix parse 或 raw global key。
10. BP2 不引入 owner/session authority、lease、ledger、TTL、compaction、queue、durable workflow、额外 cleanup 或 automatic retry。

## 7. 公共 API 与构造面验收

`TaskMaintenanceService` 的 19 个 public method 名称、可见性、参数类型、参数顺序和返回类型必须逐项不变：

```java
public void initializeForTaskStart(TaskExecutionContext context, String sourceTask)
public void beginTeamMaintenanceRound(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void openTeamPathingMaintenanceWindow(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void openTeamFirstAidMaintenanceWindow(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void closeTeamMaintenanceWindow(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void openLocalTeamReturnSupportWindow(TaskExecutionContext context, String sourceTask)
public void closeLocalTeamReturnSupportWindow(TaskExecutionContext context, String sourceTask)
public boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext context, String teamMaintenanceKey)
public boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext context, String teamMaintenanceKey, long timeoutMs)
public boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext context, TeamSupportCapability capability, long timeoutMs)
public boolean isLocalSupportMemberSession(TaskExecutionContext context)
public void registerLocalTeamSessionCandidate(String sessionKey, Collection<String> windowIds, String sourceTask)
public void markLocalTeamWindowRoleDetected(TaskExecutionContext context, String windowId, String roleName, String sourceTask)
public boolean isLocalSupportMemberCandidate(TaskExecutionContext context)
public boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext context)
public void markLocalTeamLeaderDetected(TaskExecutionContext context, String leaderWindowId, String sourceTask)
public boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext context, TeamSupportCapability capability)
public void completeLocalTeamSessionWindow(String sessionKey, String windowId, String sourceTask)
public TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext context, TaskMaintenanceRequest request)
```

TURN-34A 已冻结的六个 caller-visible API 是：

```java
awaitTeamFirstAidMaintenanceWindowOpen(...)
awaitLocalTeamSupportCapabilityOpen(...)
isLocalSupportMemberSession(...)
isLocalSupportMemberCandidate(...)
isPendingLocalSupportLeaderDetection(...)
isLocalTeamSupportCapabilityOpen(...)
```

Lombok `@RequiredArgsConstructor` 的 production collaborator 面保持五项且顺序不变：

```java
BotProperties
GameContext
DialogService
SummonSkillService
TaskExecutionContextHolder
```

以下四个 lifecycle API 当前均没有 production caller，BP2 不新增 host/factory/runtime caller，也不借 typed-key 改造激活它们：

```java
registerLocalTeamSessionCandidate(...)
markLocalTeamWindowRoleDetected(...)
markLocalTeamLeaderDetected(...)
completeLocalTeamSessionWindow(...)
```

## 8. terminal、delegate、actionId/UUID 验收冻结

### 8.1 BP2 source slice 自身的零新增面

BP2 只替换内存 key 类型，不新增一次业务调用。相对起始 production，它必须保持：

- 额外 Dialog delegate `0`；
- 额外 Summon delegate `0`；
- 额外 `TurnGameClient.execute(...)` `0`；
- 额外 action `0`；
- 额外 actionId/UUID 消耗 `0`；
- 额外 metadata read/checkpoint/retry/sleep/timer `0`。

这不是说正常 eligible Summon 整链路没有 TURN-33 action/UUID。实际 `SummonSkillService` whole-pass 的 action/UUID 1:1、terminal/uncertain 不重放仍由 TURN-33 生产合同与 `SummonSkillTurnContractTest` 验证；BP2 不复制或接管 UUID supplier。

### 8.2 `runOpportunisticMaintenance` 顺序不变

必须原样保持：

```text
normalize
-> first checkpoint
-> optional maintenance broadcast
-> handled / BROADCAST_FAILED / INTERRUPTED short-circuit
-> optional one Summon delegate
-> no-action
```

Summon 内部门序保持：feature -> interval -> FREE -> due -> UNKNOWN retry interval -> 2h tail cache -> team/local round -> capability/pathing -> duplicate/max claim -> second checkpoint -> exactly one `cleanSummonSkillsOnce(...)`。

CommonBox priority、TeamReturn capability-only 边界、Summon static-tail ownership，以及 capability 精确 `5/1/5/2` open/close 集合均不因 typed-key 改造移动。

### 8.3 terminal 与 claim/ActionState 语义不变

后续唯一 `TaskMaintenanceTurnContractTest.java` 需要以公共 API 和真实 turn-native context 验证，但不属于 BP2 写集：

1. 首 checkpoint 的 STOP 或 exact metadata typed transition 在首 Dialog/Summon delegate 前终止；delegate/execute/action/UUID 均为零。
2. broadcast `handled`、`BROADCAST_FAILED`、`INTERRUPTED` 均短路，本次不再进入 Summon。
3. eligible Summon 一次 maintenance pass 最多调用一次 `cleanSummonSkillsOnce(...)`；TaskMaintenance 不做自动 retry，也不生成第二 actionId/UUID。
4. Summon 抛出的 `TaskStopRequestedException`、`TaskFatalException` 或协议 terminal/uncertain exception 沿既有路径只传播一次；`finally` 仍按当前规则恢复 `GameContext.ActionState`，不吞异常、不补发命令。
5. success 只投影一次既有 success/cooldown/state；known failure 且无 state change 释放自己持有的 claim；已有 state change 的 failure 保留 claim；UNKNOWN 继续使用既有 retry-after 与 cache invalidation，BP2 不新增 TTL 或第二 observation。
6. same-window duplicate、max cleaner、formal/local claim acquire/release/retain 的结果状态不变。typed keys 只隔离 scope，不改变谁可以 claim、何时 release 或保留。
7. 测试要钉 supplied context 胜过冲突 holder、null+holder/no-context fallback、不同 tenant/user/device/window 隔离和 same explicit session 共享；不得用 private map reflection 或 source scan 代替行为证据。

## 9. 同文件和跨卡冲突矩阵

| 边界 | 文件重叠 | 冻结处理 |
|---|---|---|
| BP1 vs BP2 | production `0`；test `0`；child card `0` | BP1 写 `TaskExecutionContext.java` 及其 test；BP2 写 `TaskMaintenanceService.java`。但 BP2 语义依赖 BP1 的 public context/checkpoint 合同，所以仍等待 BP1 parent source receipt。 |
| TURN-34B retained production vs BP2 | production `1`，同为 `TaskMaintenanceService.java` | retained WIP 是 BP2 的起始字节；原 owner 必须已释放。BP2 claim 后只能有一个 production writer。 |
| BP2 vs BP3 | production `1`，同为 `TaskMaintenanceService.java` | 严格串行。BP3 从 BP2 canonical delivery 和 parent source receipt 对应的最终 SHA 开始，不能从 `963b...` 旁路叠写。 |
| BP2 vs BT1/后续 test tranches | production/test path `0`，语义目标相同 | 当前 named test 不存在、BT1 零 owner。若测试先后恢复，卡内必须记录 targeted production SHA；同一 named-test 的各 tranche 也只能串行。建议 source 顺序 BP2 -> BP3 -> final named-test consolidation。 |
| BP2 vs TURN-34A | Java 文件 `0` | 六个 API 的签名和 caller-visible semantics 是只读兼容门，不借 BP2 重构。 |
| BP2 vs TURN-33 Summon | Java 文件 `0` | BP2 保持一次 typed delegate；TURN-33 继续拥有实际 action/UUID、terminal/uncertain 与 no-replay 合同。 |
| BP2 vs future TaskExecutionContext work | Java 文件 `0` | 不复制或修改 BP1 native-generation fence；未来 context 工作也不能成为 BP2 绕过 BP1 receipt 的理由。 |

同文件锁的关键点是：BP1 与 BP2 没有字节冲突，但有先后语义依赖；BP2 与 BP3 没有可并行空间，因为二者都写 `TaskMaintenanceService.java`。

## 10. BP2 source-delivery 静态验收清单

未来 owner delivery 至少记录以下可复核事实：

1. 起始 `TaskMaintenanceService.java` SHA、最终 SHA、行数和修改方法索引。
2. 最终 production Java 和 append-only BP2 child card 是唯二写项。
3. 四个共享 map 使用第 6 节 typed keys；四个 per-window maps 保持 String key，未提前做 BP3。
4. formal/local claim 域不存在 `team + "#"`、`local-team:`、tuple delimiter concat、prefix parse、dual map 或 fallback lookup。
5. supplied context 优先；只有 null + empty holder 使用 no-context；context authority failure 不 broad downgrade。
6. 同 scope 同显式 session 共享、无 session 按 window 隔离、不同 tenant/user/device 隔离。
7. 19 public signatures、六个 TURN-34A API 和五 collaborator constructor 面不变；四个零 caller lifecycle API 未激活。
8. `runOpportunisticMaintenance`、broadcast/Summon gate order、最多一次 delegate、claim release/retain、ActionState、`5/1/5/2` 与业务 fallback 无变化。
9. 没有额外 metadata read/checkpoint/command/actionId/UUID/delegate/retry/sleep/timer/TTL/session authority/lease/ledger/queue/durable workflow。
10. 私有 key types 位于文件底部；不新增只转发一层的 wrapper/helper ladder。
11. delivery 明确保留 BP3 和唯一 named-test debt，不写本 helper 或 source owner 的 review verdict。
12. 未运行的 Maven/test/compile/build/runtime 项按事实写“未运行”，不能写成通过。

## 11. 当前交接状态

截至最终并发复核 `2026-07-16T11:34:17.825-04:00`：

- BP1 最新卡尾为 External C Repair #2 canonical delivery `a9c...` / `3b117...`；其后没有 parent source receipt。
- BP2 fixed child card 不存在，`TaskMaintenanceTurnContractTest.java` 也不存在。
- retained `TaskMaintenanceService.java` 仍为 `963b...`，可作为未来重新核验的候选起点，但不是本 helper 的 owner claim。
- 下一次 readiness 检查只需先读 BP1 physical true EOF 和最终 SHAs，再核对 `TaskMaintenanceService.java` 是否仍为 `963b...`，然后检查 fixed BP2 card 与同文件 owner 锁。

本 helper 到此停止，不继续观察卡片、不写 Java、不代替父级或 reviewer作结论。

TRUE_EOF PRECHECK_COMPLETE
