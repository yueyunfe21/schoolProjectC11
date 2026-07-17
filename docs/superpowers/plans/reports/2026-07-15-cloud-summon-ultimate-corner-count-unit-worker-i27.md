# W-COUNT-SUMMON-ULTIMATE-CORNER-1

`CLAIMED | task=W-COUNT-SUMMON-ULTIMATE-CORNER-1 | worker=Internal implementation-only Worker I27 | claimedAt=2026-07-15T04:04:43-04:00 | countUnit=SummonSkillService::maybeClickUltimateCorner | countDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java; D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-summon-ultimate-corner-count-unit-worker-i27.md]`

## 交付结论

- 状态：`NO_CODE_CHANGE / DELIVERED_FOR_PARENT_SOURCE_REVIEW`。指定 Java 文件已完整闭合本 count unit；为保护共享 untracked 写集，本 Worker 未制造重复 diff。
- requested `countDelta=+1`；当前 `countApplied=0`。父级源码审查与统一 fresh Maven 门通过前不得记账。
- 真实链已闭合：`AutoBattleTask active caller -> TaskMaintenanceService.runOpportunisticMaintenance -> cleanSummonSkillsOnce -> existing typed SUMMON_SKILL_WHOLE_PASS -> DHXY exact-window remote exclusive -> direct mechanics 内四个 maybeClickUltimateCorner caller -> hover/template/click/generation recheck -> nine-field cleanup terminal -> Cloud result/state`。
- 本单只计 `SummonSkillService::maybeClickUltimateCorner`。不重复计算 I5 已批准的 `SummonSkillService::cleanSummonSkillsOnce` whole pass，也不重复计算 I17 已批准的 `SummonSkillTailBoundaryScanner::scanLockedBoundary`；不把 I10 已判重复的 `TaskMaintenanceService::maybeCleanSummonSkill` 重新计数。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。

## 必读与工作区基线

- 已完整读取仓库 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、`docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`。
- 已核对 `docs/业务逻辑.md:170-211`：6/8 布局、静态 locked/empty/occupied、`UNKNOWN` fail closed、仅在需要判断可删时 hover，以及普通技能删除、终极角检查、冷却、窗口开关和确认点击保持原语义。
- 已核对迁移矩阵 `SummonSkillService::cleanTailNormalSkillsDirect`、`SummonSkillService::maybeClickUltimateCorner`、`SummonSkillTailBoundaryScanner::scanLockedBoundary`、`TaskMaintenanceService::runOpportunisticMaintenance/...` 的独立职责和计数边界。
- DHXY：`thin-client-design@0114604e`；Cloud：`navigation-migration@3b988caa`。两仓 `git status --short --branch` 均有大量并行 dirty/untracked；全部保留，未回滚、覆盖、清理或提交。
- Cloud 指定 Java 当前 SHA-256 为 `2EE437F1B82470DA43FD94C02E934E4EB757E3CF04A43535AEE3E0E36CCBD1F5`；696 mirror Git blob 为 `d8afb9e2`。完整 no-index diff 只有既有 current-context/typed whole-pass adapter 和两个 UI-clean port substitution，四个 caller 与 `maybeClickUltimateCorner` 本体没有 diff hunk。

## Active Caller 到 Typed Whole Pass

1. Cloud `AutoBattleTask.execute(context)` 在 `TaskExecutionContextHolder.callWith(context, ...)` 内运行；`runAutoBattlePatrol` 仅在 combat tick 为 `NONE` 且 action state 为 `FREE` 时进入 `maybeRunIdleMaintenance`。
2. `maybeRunIdleMaintenance` 构造 `cleanSummonSkill=true` 的现有 request，并调用 `TaskMaintenanceService.runOpportunisticMaintenance`；broadcast-first 与既有队伍/capability/window gates 顺序不变。
3. `TaskMaintenanceService:755` 恰一次调用 `summonSkillService.cleanSummonSkillsOnce(cleanupRequest)`；cleanup request 的 `expectedSkillCount/trustExpectedSkillCount/startSlotIndex/skipUltimateCornerCheck` 四字段原样进入 Cloud `WholePassIntent`。
4. Cloud `SummonSkillService:194-225` 只调用一次 `context.getRemoteGameClient().summonSkillWholePass().execute(intent)`；未合成默认 context，未新增 TTL、自动 retry、owner、permit、session、ledger 或第二套 wire。
5. DHXY `LocalRemoteGameCommandHandler:2655-2783` 在 exact task-run/window admission 下用一次 `submitRemoteExclusiveAndWaitDetailed`；input-worker callback 调用现有 DHXY `SummonSkillService.cleanSummonSkillsOnce(request)`，由 direct 分支执行整个 pass，零 queue-in-queue。

## 四个真实 maybeClickUltimateCorner Caller

| DHXY caller | 触发条件 | 结果消费 |
|---|---|---|
| `SummonSkillService:515` | 普通技能删除后复检为 `EMPTY_SLOT` | 覆盖 deleted/inspected、clicked/succeeded、nextStart；未完成立即九字段失败 terminal |
| `SummonSkillService:546` | 普通技能删除后复检为 `LOCKED_SLOT`，locked-tail scanner 返回 `ultimateCheckIndex` | 先消费 I17 scanner counters/result，再执行同一终极角逻辑并闭合 terminal |
| `SummonSkillService:573` | 当前 actionable slot 直接为 `EMPTY_SLOT` | 同样完整消费 `UltimateCornerResult`，完成后结束本 pass |
| `SummonSkillService:601` | 当前 actionable slot 为 `LOCKED_SLOT`，scanner 返回 `ultimateCheckIndex` | 同样先消费 scanner，再消费终极角全部字段；无只看 LOCKED 就提前成功的旁路 |

Cloud baseline-preserved direct body中也恰有同构四 caller（当前 Cloud `:443/:474/:502/:530`）；它们由 typed whole-pass 的 DHXY direct mechanics 实际执行，不另建 Cloud capture/input 路径。

## 终极角方法闭环

`maybeClickUltimateCorner` 保留以下 696 正常路径顺序、判断与 delay：

1. 先按现有 cooldown 请求判断 `skipUltimateCornerCheck`；再检查共享 40 秒 absolute deadline。
2. 以 slot absolute point 加固定右上角 offset，生成既有随机 hover 点；move 后等待 `SKILL_HOVER_WAIT_MS=700ms`。
3. 对该 hover 点的 tooltip ROI 做一次 capture+yellow wash；capture 失败 fail closed，yellow count 不足安全完成为未命中。
4. 只在 `click_ultimate_template` 命中后点击同一点，click hold `120ms`，等待 `ULTIMATE_CORNER_CLICK_WAIT_MS=2500ms`。
5. 生成后恰一次重读 slot 并更新 observed status/inspected count。`NORMAL_SKILL` 按原路径删除并再次重读；`KEEP_SKILL` 安全保留；`EMPTY/LOCKED/UNKNOWN` 返回对应 closed failure。
6. 已点击且生成成功后，即使后续删除或稳定性复检失败，`UltimateCornerResult` 仍保留 `clicked=true/succeeded=true`。Cloud `TaskMaintenanceService:767-770` 在 cleanup 总体失败时仍记录既有 ultimate success cooldown，保持 `ultimate-success-before-later-failure`。

DHXY current mechanics 仅复用既有 input-worker stop checkpoints 与已批准 post-delete slot classifier；本单没有新增 checkpoint、验证、读图、重试、TTL、park/yield、cleanup 或业务 gate，也没有改变上述正常路径顺序和 delay。

## Nine-field Terminal / Result

- DHXY `buildCleanupResult` 与 handler `cleanupValue(...)` 完整携带九字段：`success`、`skillCount`、`nextStartIndex`、有序 `observedSlotStatuses`、`ultimateSkillClicked`、`ultimateSkillSucceeded`、`inspectedSlotCount`、`deletedSkillCount`、`message`。
- DHXY handler 闭合 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；执行后 binding 漂移降为 `UNKNOWN`，并在 finally 关闭现有 in-flight handle。
- Cloud `CloudSummonSkillWholePassCapability.CleanupValue` 对九字段及五个 slot enum 做 closed validation；Cloud `SummonSkillService.toCleanupResult` 字段一一映回，保留 insertion order。
- `Executed` 返回 cleanup result；`NotExecuted` 返回现有 failed result；`Stopped/Unknown/Interrupted` typed unwind，不伪装业务成功，也不自动重发。

## 写集与验证纪律

- Java：`NO_CODE_CHANGE`。指定 Cloud `SummonSkillService.java` 已满足 I27，未覆盖共享 untracked 文件。
- 唯一实际写入：本固定报告；CLAIMED 位于真实 EOF 后，本交付结论继续追加于同一文件。
- 未修改 runtime/application/server/host/Task/poller/UI/capture/input/tests 或任何写集外源码/文档。
- 未运行 Maven、build 或 tests；统一构建由父级执行。
- 未执行 reset/checkout/clean/delete/stage/commit/branch/worktree 或其它 Git mutation。

`DELIVERED | task=W-COUNT-SUMMON-ULTIMATE-CORNER-1 | countUnit=SummonSkillService::maybeClickUltimateCorner | requestedCountDelta=+1 | countApplied=0 | javaChange=NO_CODE_CHANGE | businessDifference=NONE | sourceReview=PARENT_PENDING | buildGate=PARENT_PENDING`

## Parent Source Review #1 - 2026-07-15T04:13:00-04:00

父级独立对照迁移矩阵独立行 `SummonSkillService::maybeClickUltimateCorner`、active Cloud 四个 caller、
DHXY whole-pass direct mechanics 与九字段 terminal：cooldown/deadline、700ms hover、yellow/template gate、
120ms click、2500ms generation wait、恰一次重读、普通技能删除复检及 ultimate-success-before-later-failure
均闭合；未重复计算 I5 whole-pass 或 I17 locked-boundary scanner。结论 **P0=0/P1=0/P2=0，
SOURCE APPROVED / COUNT PENDING BUILD**，`countDelta=+1`；fresh 双构建前 ledger 仍 `189/407`。
Java 未改，本 Worker 关闭。
