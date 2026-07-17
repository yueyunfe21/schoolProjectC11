# W-COUNT-BATTLE-RADAR-UPDATE-STATE-1

CLAIMED | task=W-COUNT-BATTLE-RADAR-UPDATE-STATE-1 | claimedAt=2026-07-15T04:16:50-04:00 | countUnit=BattleRadarService::updateCombatState | countDelta=+1 | writeSet=[D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\BattleRadarService.java; D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-battle-radar-update-state-count-unit-worker-i31.md]

## Implementation Result

- Worker: Internal implementation Worker I31（非 reviewer）。
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，直接读取 Cloud 仓库内保存的
  `migration-baseline/696a12b0` 镜像；按禁令未执行 Git。
- Status: `DELIVERED / NO_CODE_CHANGE`。
- Count unit: `BattleRadarService::updateCombatState`，本矩阵项只计状态跃迁核心；`countDelta=+1`。
- Java change: `0`。现有 active ordinary radar chain 已闭合，无需制造 Java churn。
- Target Java SHA-256 before/after: `E90E99FB9444BAD960BC5C0B648EEA51501CED1AAA8ED26B8061F53B46B86405`。
- 唯一写集遵守：仅本报告发生写入；允许的 `BattleRadarService.java` 未修改。DHXY、
  `AutoCombatService`、`AutoBattleTask`、其它文件均只读。

## Active Ordinary Radar Chain Evidence

1. `AutoBattleTask.java:139-149` 的 active patrol loop 每轮只调用一次
   `handleAutoCombatTick(context)`；`:162-164` 将其闭合到
   `autoCombatService.handleCombatTick(context, "auto-battle", false)`。
2. `AutoCombatService.java:107-111` 的 boolean overload 将 `false` 交给既有
   `legacyPostCombatRecoveryPolicy`；`:178-181` 映射为 `FULL_RECOVERY`。因此本链是 ordinary radar，
   不选择 `FAST_EXPECTED_EXIT`。
3. `AutoCombatService.java:144-151` 在 ordinary policy 下保持 `fullRadarDue=true`，且只执行一次
   `battleRadarService.checkAndSyncCombatState()`；`:145-147` 的 fast probe 分支不进入。本单没有重复计算
   `checkAndSyncCombatState` 的 fact probes。
4. `BattleRadarService.java:65-130` 依既有顺序读取 typed facts：auto flag `:67-71`、selection
   `:76-89`、top `:94-107`、需要退出确认时 minimap `:113-130`。前三种 `VISIBLE` 分别在
   `:71/:89/:107` 调 `updateCombatState(true)`；仅连续 miss 门和 readable minimap 已通过后在 `:130`
   调 `updateCombatState(false)`。`readFact:481-502` 是唯一 typed `readWindowFact` terminal；本 Worker
   未增加 fact read、retry、TTL、owner、session 或 wrapper。
5. `BattleRadarService.java:331-351` 的 count unit 闭合 transition/no-transition boolean：
   - 输入 `true` 且 remembered state 非 `IN_COMBAT`：清 miss -> 原日志 -> 写 `IN_COMBAT` ->
     `onEnterCombat()` -> `true`。
   - 输入 `false` 且 remembered state 为 `IN_COMBAT`：清 miss -> 原日志 -> 写 `FREE` -> 按
     `pending=true -> pendingAt -> pendingBattleCount -> onExitCombat()` 顺序发布退出事实 -> `true`。
   - 无状态跃迁直接 `false`，不发布 enter/exit truth。
6. `onEnterCombat:354-369` 严格保持 `battleCount++` 一次、战斗时间/探针状态复位、旧 exit one-shot
   清理、`combatEnterPending=true`、最后写原 enter 日志的顺序；`onExitCombat:372-378` 保持退出复位顺序。
7. enter one-shot 由 `AutoCombatService.maybeHandleCombatEnter:332-343` 经
   `BattleRadarService.consumeCombatEnterSignal:385-391` 一次消费；ordinary exit one-shot 由
   `AutoCombatService.consumeExitAndRecover:345-365` 的非 FAST 分支 `:353` 经
   `consumeCombatExitSignal:399-407` 一次消费。pending 清位后 closed boolean 阻止重复消费；随后
   `handleCombatTick:155-175` 返回 `EXIT_RECOVERED`、`IN_COMBAT` 或 `NONE` 给 `AutoBattleTask`。

## Baseline Equivalence

- 对当前文件与 `migration-baseline/696a12b0/.../BattleRadarService.java` 抽取方法块并逐字符比较：
  `updateCombatState` = `EXACT=True`（两侧均 1155 chars）；`consumeCombatEnterSignal` =
  `EXACT=True`（237 chars）；`consumeCombatExitSignal` = `EXACT=True`（324 chars）。
- 696 的 state/one-shot/battleCount/日志顺序全部在位。既有 Cloud typed mechanics 适配仅把本地图片
  baseline 清理表示为 `fastExpectedExitBaselineReady=false`，不改变本 count unit 的 ordinary transition
  决策、发布顺序或 consumer boolean；本 Worker未修改该适配。
- 矩阵 `docs/superpowers/specs/2026-07-12-service-migration-matrix.md:1105` 已将
  `BattleRadarService::updateCombatState` 单列为“状态跃迁核心：置 IN_COMBAT/FREE 并发 enter/exit
  一次性信号”；本单未重复计数 `checkAndSyncCombatState` fact producer 或其它 helper。

## Explicit Exclusions And Verification

- dormant `FAST_EXPECTED_EXIT` caller 不在本单 active ordinary radar chain；未宣称其闭合，未修改或据此阻断
  本 count unit。
- 未 build、未 test、未 runtime、未 input、未执行 Git；这些均由任务明确禁止。
- 未创建 Design，未做 reviewer 判断。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-15T04:20:00-04:00

父级独立读取 `AutoBattleTask.java:139-164`、`AutoCombatService.java:126-181,332-365` 与
`BattleRadarService.java:65-130,331-407`，确认 active ordinary radar 只经 FULL_RECOVERY 路径，typed fact
producer 之后的 `updateCombatState` 独立矩阵边界完整保持 transition/no-transition、IN_COMBAT/FREE、
battleCount 与 enter/exit one-shot 发布/消费顺序；未宣称 dormant FAST_EXPECTED_EXIT 可达，未重复计算
`checkAndSyncCombatState` fact probes。结论 **P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**，
`countDelta=+1`；Java 未改，本 Worker 关闭。
