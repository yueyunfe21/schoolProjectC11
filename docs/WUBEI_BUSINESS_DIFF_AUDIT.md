# 五倍本地业务逻辑差异审计

审计时间：2026-06-18

基准版本：`origin/codex/migrate-runner-dialog` / `3f0a2e79007121c98a15ad90d5ed7b8902033068`

基准提交：`3f0a2e7 Add short first-aid gate for Wubei chained combat`

审计对象：当前本地工作区相对云端 latest push 的未提交修改。这里只记录会影响五倍业务判断、点击顺序、寻路事实、弹窗消费或重试节奏的差异；纯日志、文档、配置记忆和测试图片不作为业务问题。

## 当前相关改动范围

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiStepOutcome.java`
- 新增 `src/main/java/com/bot/dhxy/task/wubei/WubeiWaitSpec.java`
- 新增 `src/main/java/com/bot/dhxy/task/wubei/WubeiWaitReason.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/model/WindowReadyEventType.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowReadyEventBus.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`

## 已校正：显形镜 ACTIVE 抢 STORY 不是当前代码的活跃差异

当前本地 `WubeiTask.java:2098-2142` 已经恢复到接近云端语义：

- `snapshot.getState() == ACTIVE` 或 `snapshot.isProbeInProgress()` 时，直接返回 `probe runner pathing still active`。
- 当前不会在仍然 ACTIVE 的时候调用 `beginProbeStoryWaitFromVisibleDialog(...)`。
- `WUBEI_PROBE_STORY_WAIT_TIMEOUT_MS = 15_000L` 和 `waitForPreparedProbeStory(...)` 的 15 秒总等待窗口还在。

因此，旧报告里“显形镜链路在路上被 STORY 抢走”和“15 秒变无限 park”这两条，不能再作为当前本地代码的结论。

但是本地仍保留了未使用方法：

- `WubeiTask.java:2227-2232` `hasFreshVisibleProbeStory(...)`
- `WubeiTask.java:2234-2249` `beginProbeStoryWaitFromVisibleDialog(...)`

建议：如果后续确认不再使用，删掉这两个死代码，避免以后又被接回 ACTIVE 分支。

## P1：五倍 phase 的 park/wake 调度改变了业务重试节奏

本地位置：

- `WubeiStepOutcome.java:17-74`
- `WubeiTask.java:408-476`
- `WubeiTask.java:775-892`

本地逻辑：

- `WubeiStepOutcome` 新增 `waitSpec`。
- PATHING / SHARED_STATE / MUST_YIELD 后，`runRoundPhases()` 会调用 `parkAfterYieldIfNeeded(...)`。
- pathing 等待最多 park 5 秒，prepared dialog 等待最多 park 1.5 秒，combat state 等待最多 park 1.5 秒。

云端逻辑：

- phase 返回后只释放 task turn，不额外在五倍内部 await ready event。
- 下一轮是否继续，主要靠原来的 phase loop / sleep / turn 调度节奏。

业务风险：

- 这不是纯框架差异，因为它改变了五倍的 fallback 时间点。
- 如果 ready event 漏发、类型不匹配，或者只有 visible dialog 没有 prepared action，本地会按新的 park timeout 节奏恢复，而不是云端的直接下一轮节奏。
- 这会影响“进战斗 unresolved 后多久重试绿字/AutoJIA”、“战斗中队长多久再检查一次”、“寻路 ACTIVE 多久再读 snapshot”。

建议：

- 如果目标是严格恢复云端业务，先去掉五倍内部 `waitSpec` park，只保留云端原来的 release turn 语义。
- 如果保留 park/wake，必须逐项证明每个 wake type 都不会替代业务 fallback。

## P1：ENTER_BATTLE prepared action 现在会在更多位置抢占正常流程

本地位置：

- `WubeiTask.java:706-720`
- `WubeiTask.java:733-747`
- `WubeiTask.java:3043-3095`
- `WubeiTask.java:3181` 附近

本地逻辑：

- `runResolveAfterPathingPhase()` 在处理 ACTIVE / ARRIVED / STOPPED_AWAY 前，会先调用 `consumePreparedEnterBattleDuringPathing(...)`。
- `tickEnterBattle()` phase-start、combat tick 前、combat tick 后都会尝试 `consumeFreshEnterBattlePreparedAction(...)`。
- 如果吃到 `WUBEI_ENTER_BATTLE` prepared action，会清 pathing signal，并直接进 `WAIT_BATTLE_FINISH`。

云端逻辑：

- 云端已有 phase-boundary prepared action 优先级处理。
- 但云端没有在 `resolve-after-pathing` 内部新增一次 “prepared enter-battle preempts pathing wait”。
- 云端 `tickEnterBattle()` 注册 interest 后继续跑原来的同轮业务检查，没有这些额外的 before/after combat-tick prepared action 抢占点。

业务风险：

- 如果 prepared action 确实新鲜且匹配，这能更快点进战斗。
- 但如果 prepared action 来源于上一轮、上一条绿字、旧 pathing intent，或者 runner 刚好准备了错误目标，本地会比云端更早消费它，并跳过原本的近目标 smart click / direct fallback / tracker retry 判断。

建议：

- 严格按云端恢复时，先移除 `consumePreparedEnterBattleDuringPathing(...)` 和 `tickEnterBattle()` 内新增的额外 prepared 抢占点。
- 或至少要求日志里同时满足：operation、target、intentId、当前 phase、当前 tracker hint 都一致，才允许抢占。

## P1：WAIT_BATTLE_FINISH 的战斗中轮询被改成 COMBAT_STATE_CHANGED park

本地位置：

- `WubeiTask.java:3213-3233`
- `WindowReadyEventType.java:12-15`
- `WindowTaskRunner.java` 新增 `COMBAT_STATE_CHANGED` 发布

本地逻辑：

- `AutoCombatService.TickResult.IN_COMBAT` 时，返回 `waitForCombatStateWake(sharedState(...))`。
- 队长释放 task turn 后会等 `COMBAT_STATE_CHANGED` 或 1.5 秒超时。

云端逻辑：

- `IN_COMBAT` 时直接 `sharedState(state, "combat still running")`。

业务风险：

- 这会改变队长在战斗中的检查频率。
- 如果 `COMBAT_STATE_CHANGED` 没按预期发布，队长依赖 1.5 秒超时恢复；如果事件过多，又可能让队长更频繁抢回 turn。

建议：

- 这块可以作为多窗口调度优化保留，但它不是云端业务 baseline。
- 如果现在目标是查“为什么队长不动/不回合推进”，这块应列入验证项。

## P1：phase-boundary priority 从事务外移到了事务内

本地位置：

- `WubeiTask.java:421-432`
- `WubeiTask.java:529-575`

本地逻辑：

- `checkReadyPriorityBeforePhase(...)` 在 `taskTransactionRunner.run(...)` 的 callback 内执行。
- 如果有 visible dialog / dialog interest / recent ready event，还会在持有当前 task turn 的情况下最多等 80ms。

云端逻辑：

- `checkReadyPriorityBeforePhase(...)` 在进入 `taskTransactionRunner.run(...)` 之前执行。
- 如果 priority 命中，云端不会进入普通 phase transaction。

业务风险：

- 这是调度边界变化，不只是代码位置变化。
- 本地可能在持有 turn 时消费 prepared action 或等待 ready settle，改变其他窗口插队、补给、prepared dialog 消费的时机。

建议：

- 如果没有明确测试证明这对多窗口更稳，应恢复云端事务外 priority。

## P1：Navigation 放宽了 prepared route 的 intentId 匹配

本地位置：

- `NavigationService.java:1038-1071`
- `WindowRuntimeContext.java:868-917`

本地逻辑：

- `NavigationService.matchesActivePreparedRouteIntent(...)` 中，如果 active intentId 和 prepared action intentId 不同，只要都是 `ROUTE_TRANSFER` 且目标地图相同，也允许使用。
- `WindowRuntimeContext.preparedActionMismatchReason(...)` 也把 route intent recovery 放宽为：当前 active intent 存在时，只要 active target、expected target、action target 相同，就可以绕过 intentId 不一致。

云端逻辑：

- active intent 存在时，prepared action 必须匹配同一个 intentId。
- 只有 current intent 已清空时，才允许 cleared route intent recovery。

业务风险：

- 五倍连续点击同地图、同目标地图时，旧 route dialog 可能被新业务动作消费。
- 这会表现为：本来应该重新走第一个链接/第一个路线，但当前动作吃了上一段 route prepared action。

建议：

- 先恢复云端严格 intentId 匹配。
- 如果必须支持同目标恢复，需要额外绑定 source、phase、createdAt 和当前 tracker 目标，而不是只看 target map。

## P1：Navigation 同目标地图 re-entry 可能复用旧 pathing intent

本地位置：

- `NavigationService.java:2195-2283`
- `NavigationService.java:2286-2313`

本地逻辑：

- `registerWindowPathingIntent(...)` 如果发现旧 active intent 和本次 target map 相同，且旧 snapshot 仍可信，会直接复用旧 intent。
- 复用时不会 `markPathingStarted(...)`，也不会生成新的 intentId/source。

云端逻辑：

- 每次 navigation handoff 都新建 `WindowPathingIntent` 并 `markPathingStarted(...)`。

业务风险：

- 五倍连续同地图任务会让旧 pathing intent/source/age 继续挂着。
- runner 可能把上一段 pathing terminal、route dialog、visible dialog 归给新 phase。
- 这和“还没到第一个链接，行为上像已经切到下一段/下一点”的症状有重叠风险。

建议：

- 按云端恢复：五倍相关 navigation handoff 每次都新建 intent。
- 如果要保留复用，只能作为明确新行为，并且要用 testcase/log 证明不会串 phase。

## P2：WindowTaskRunner 的 attention 命名容易误导，但当前不直接改变 pathing state

本地位置：

- `WindowTaskRunner.java:1768-1799`
- `WindowTaskRunner.java:1824-1868`
- `WindowTaskRunner.java:2088-2107`

结论：

- 本地把 `PathingDialogBlock.blocking(...)` 改名成 `attention(...)`，日志也写 `attention-only`。
- 但 `attention(...)` 仍然返回 `blocking=true`。
- 当前 `classifyPathingState(...)` 也没有真正使用 `dialogBlock` 去改变 ACTIVE / ARRIVED / STOPPED_AWAY。

风险：

- 这块目前更像命名/日志误导，不是“runner 不 blocking 导致停下”的直接证据。
- 真正会改变业务的是五倍和 Navigation 如何消费这些 ready/prepared/intent 信号。

建议：

- 不把它当 P0 修。
- 后续可以把日志字段改准，避免继续误判。

## 当前最可能影响白龙马/五倍的排序

1. `NavigationService` 的同地图 route intent 放宽和旧 intent 复用：最容易造成上一段路线/弹窗事实串到下一段。
2. `WubeiTask` 内部新增的 prepared enter-battle 抢占点：可能跳过云端原有 fallback 顺序。
3. `WubeiStepOutcome.waitSpec` 的 park/wake：可能改变 fallback 触发时间。
4. `WAIT_BATTLE_FINISH` 改成 `COMBAT_STATE_CHANGED` park：可能改变战斗中/战斗后推进节奏。
5. phase-boundary priority 移进事务内：主要是多窗口 turn/priority 时机风险。

## 建议给卸帅的处理顺序

1. 先恢复 `NavigationService` 和 `WindowRuntimeContext` 的 route intentId 严格匹配，以及每次 navigation handoff 新建 intent 的云端行为。
2. 再回退 `WubeiTask` 新增的 `consumePreparedEnterBattleDuringPathing(...)` 和 `tickEnterBattle()` 内额外 prepared 抢占点，保留云端 fallback 顺序。
3. 再决定是否保留 `waitSpec` park/wake。如果保留，必须证明它只是调度优化，不改变五倍业务 deadline 和 fallback。
4. 最后清理未使用的 visible story takeover 方法，避免后续误接回 ACTIVE 路径。

