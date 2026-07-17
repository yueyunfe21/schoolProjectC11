# BattleRadar Expected-Exit Consume Delivery Preflight

> 角色：Delivery Preflight Helper，仅做非绑定预检；不是 reviewer，不作最终裁决。
>
> 范围：External A 最新 `W-COUNT-BATTLE-RADAR-EXPECTED-EXIT-CONSUME-1` 交付。
>
> 操作边界：只读两仓源码、固定日志与既有计划；未修改 A 日志、CR271 或任何源码；未运行 build、test、runtime、Git。

## 1. 真实 EOF 与基线

- A 固定日志真实 EOF 为第 `7760` 行。最新块是 `7735-7760` 的
  `W-COUNT-BATTLE-RADAR-EXPECTED-EXIT-CONSUME-1 NO_CODE_CHANGE` 证据，不是前面的 baseline-refresh 或 whole-service 块。
- 可直接读取 Cloud 仓保留的
  `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/{AutoCombatService,BattleRadarService}.java`，
  因此本预检没有用 Git 重建基线。
- 当前 Cloud `AutoCombatService` 的 expected-policy arm、radar tick、consume 分流与 696 快照逐行同构：
  当前 `126-175/345-375` 对应基线 `125-174/344-374`。
- 当前 Cloud `BattleRadarService.armExpectedCombatExitWait`、`updateCombatState`、
  `onEnterCombat/onExitCombat`、`consumeCombatExitSignalForExpectedWait` 与 696 快照行为一致：
  当前 `221-233/331-378/416-435` 对应基线 `213-225/338-385/423-442`。

## 2. 风险清单

### RISK-1：Cloud expected-exit 真实 caller 当前不可达

A 把 `AutoCombatService.java:352` 记为真实 caller，但该行只是
`consumeExitAndRecover(...)` 内部的条件分支：只有 `recoveryPolicy == FAST_EXPECTED_EXIT` 才会调用目标方法
（Cloud `AutoCombatService.java:345-353`）。

当前 Cloud `src/main` 的真实调用图为：

- 唯一调用 `AutoCombatService.handleCombatTick(...)` 的任务是 `AutoBattleTask`。
- `AutoBattleTask.java:162-164` 固定传入布尔 `false`。
- `AutoCombatService.java:107-111/178-181` 将该 `false` 映射为 `FULL_RECOVERY`，不是 `FAST_EXPECTED_EXIT`。
- Cloud `src/main` 中 `FAST_EXPECTED_EXIT` 除 enum、方法内部判断和 JavaDoc 外，没有任何 caller 传入该策略。
- 反证来自 DHXY 本地业务：`WubeiTask.java:4427-4429` 与
  `XiuluoTaskV2.java:4283-4284/4401-4410` 才是真正选择 `FAST_EXPECTED_EXIT` 的调用方；这些调用的是 DHXY
  本地 `AutoCombatService`，不是 Cloud 目标方法。

这与严格计数门冲突：
`docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md:500-504`
要求 count unit 从真实 public caller 经 Cloud Service、typed DHXY mechanics 到 closed terminal 全链可达。
H6 排班报告 `2026-07-15-seven-lane-next-count-queue-helper-h6.md:46-47` 所写“public combat tick 图可达”只证明
public overload 理论上可被调用，不能替代当前源码中的实际策略 caller。

### RISK-2：A 所称 battle identity policy 实际只有记录，没有身份判定

- 退出生产时，Cloud `BattleRadarService.java:345-347` 保存
  `combatExitPendingAtMs` 和 `combatExitPendingBattleCount = battleCount`。
- expected consume 在 `421-422` 只比较 `pendingAtMs` 与 `expectedCombatExitWaitArmedAtMs`；
  `combatExitPendingBattleCount` 只用于 `423-425` 日志，不参与 fresh/stale predicate。
- 新战斗进入会在 `361-366` 清除旧 pending，所以正常连续 transition 下不会把旧 pending 带进新 battle；
  但这仍是“enter 时清旧 + 时间边界”，不是独立的 battle identity 校验。
- 该形状与 696 基线一致，因此不建议在本 count unit 内擅自新增 battle-count gate；A 的交付文字应降格为
  “arm/time boundary，并记录 pending battle count 供诊断”。

### RISK-3：state 清理是 one-shot pending 清理，不是完整 wait/identity reset

- stale 与 fresh 两条 consume 分支均清除
  `combatExitPending/combatExitPendingAtMs/combatExitPendingBattleCount`
  （Cloud `BattleRadarService.java:426-433`），可防同一 pending 被重复消费。
- `onExitCombat` 同时清 `combatStartedAtMs`、probe/full-radar 时间和 avatar baseline-ready
  （`372-378`）；`onEnterCombat` 重置本战计时并清旧 exit pending（`354-369`）。
- `expectedCombatExitWaitArmedAtMs` 在 stale/fresh consume 后不清零；AutoCombat 侧
  `expectedCombatExitWaitArmed` 只在成功消费后于 `AutoCombatService.java:358-360` 清除，stale/absent 返回时保持。
  这也与 696 基线一致，不能在本迁移单里新增 cleanup；但交付不能描述为“完整 battle identity/state 已全部清理”。
- Cloud `BattleRadarService.state()` 只按 `TaskExecutionContext.windowId` 建立状态
  （`510-516`），不按 `playerIdentityEpoch` 失效；DHXY avatar mechanics 的 baseline key 则包含
  `windowId + nativeHandle + playerIdentityEpoch`
  （`BattleRadarLocalObservationMechanics.java:270-271/387`）。这是基线状态所有权边界，若父级要求跨角色重绑清理，需另立明确行为范围。

## 3. Typed Fact、Handler 与 Closed Terminal

这部分未发现结构缺口：

- Cloud `WindowFactKind.java:14-20` 与 DHXY `RemoteWindowFactKind.java:14-20` 的七个 BattleRadar kind 对齐。
- Cloud `WindowFact.java:337-404` 将 signal/minimap/avatar 定义为 closed enum；失败态不会伪装成未命中或未变化。
- Cloud `WindowFactOutcome.java:8-21/42-51` 强制 `factKind` 与 fact variant 对应；只有 `OBSERVED` 可带 fact。
- Cloud `BattleRadarService.readFact` 在 `481-502` 将 `OBSERVED`、`NOT_EXECUTED`、`STOPPED`、其它终态分别收敛，
  没有把 unresolved/失败映射成新的战斗负面事实。
- DHXY `LocalRemoteGameCommandHandler.java:846-882` 在 exact `BindingAccess` 下分派七个 kind；
  `883-912` 复核 timeout、task registration 与 bound window 后才返回 `OBSERVED`。
- DHXY `BattleRadarLocalObservationMechanics.java:89-143/151-180/240-299` 使用传入 binding 采集；
  signal、minimap、avatar 均返回 closed result。handler `1083-1140` 做穷尽一对一映射。

需要准确表述：typed facts 是上游视觉/可读性 observation，不是“typed enter/exit fact”；enter/exit pending 是 Cloud
`updateCombatState(...)` 根据这些 facts 生成的 per-window 内存状态。目标 count unit 的最终结果是 one-shot boolean，closed
但不是独立远程 fact。

## 4. Arm、Stale 与重复消费矩阵

| 场景 | 当前行为 | 与 696 | 预检判断 |
|---|---|---|---|
| expected wait 首次进入 | AutoCombat 先 arm，再做 avatar/full radar | 一致 | 方法内部正确；当前 Cloud 无真实策略 caller |
| arm 前已有 pending | `armExpectedCombatExitWait` 清 `pendingAt <= now` | 一致 | stale 不会被新 wait 消费 |
| 无 pending | consume 返回 `false`，不造 negative truth | 一致 | 清晰 |
| pending 早于 arm | 清三项 pending 元数据后返回 `false` | 一致 | stale discard 正确 |
| pending 不早于 arm | 清三项 pending 元数据后返回 `true` | 一致 | one-shot，不会重复消费 |
| 新 battle 进入 | 清旧 exit pending，再递增/保留新 battle 状态 | 一致 | 可防上一战 pending 跨入下一战 |
| battle identity | 记录 `pendingBattleCount`，consume 不比较 count | 一致 | A 的“identity policy”表述过强 |

## 5. 是否重复计数

- 文档树内没有找到该 exact `countUnit=BattleRadarService::consumeCombatExitSignalForExpectedWait` 的更早
  `countDelta=+1` 完成记录；出现位置是 A 最新发单/交付与 H6 预排。
- 已在等待统一构建的 BattleRadar 单元是不同方法：
  `checkAndSyncCombatState`、`checkFastExpectedCombatExitByAvatarDiff`、
  `refreshFastExpectedCombatExitAvatarBaseline`。因此按“方法矩阵行”看，不是 exact countUnit 重复。
- 但该方法早已作为 whole-service/state-chain 源码的一部分被迁入和对账；本次是零 Java 变化。若仍按 strict
  caller-to-terminal 计数，本次不能只凭已有方法再次记为完整链，必须先解决 RISK-1。

## 6. 写集外前置与建议

当前唯一 Java 写集 `Cloud BattleRadarService.java` 无法修复 RISK-1，因为目标方法和其内部 caller 分支已经存在，缺的是
选择 `FAST_EXPECTED_EXIT` 的真实 Cloud 任务 caller。需要父级在以下两条中明确选择：

1. 先授权并闭合写集外 caller 前置：让实际 expected 战斗任务（五倍/修罗的 Cloud task/adapter）在现有业务条件下调用
   `AutoCombatService.handleCombatTick(..., FAST_EXPECTED_EXIT)`，再重新预检本 count unit。不得把 `AutoBattleTask` 的
   `false` 随意改成 fast policy，因为那会改变普通自动战斗恢复语义。
2. 若暂不迁 caller，则把本单明确降为 dormant/source-parity 证据，不作为当前 strict `+1` caller-to-terminal 单元；
   这需要父级调整计数定义，helper 不代作该决定。

不建议为满足计数在 `BattleRadarService` 内增加自调用、默认策略、TTL、retry、额外 observation、battle-count gate 或新的
negative truth；这些都会越过 696 基线和当前写集。

## 结论

PREFLIGHT_RISK
