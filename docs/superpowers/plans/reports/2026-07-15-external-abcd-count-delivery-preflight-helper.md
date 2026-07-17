# External A/B/C/D Count Delivery Preflight Helper

> 非绑定交付预检；本报告不是 reviewer 结论，不作计数或交付裁决。仅核指定 active 源码行段、
> `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 本地快照及四份固定日志真实 EOF。未运行
> build/test/runtime/Git，未修改 Java、External 固定日志、CR 或主计划。

## Observations

### A - `BattleRadarService::consumeCombatEnterSignal`

- 固定日志真实 EOF 为 `7833` 行，最新材料是 `W-COUNT-BATTLE-RADAR-COMBAT-ENTER-CONSUME-1`
  的 `NO_CODE_CHANGE` 逐跳证据。
- active caller 真实存在：`AutoBattleTask:163` 调 `handleCombatTick(..., false)`；
  `AutoCombatService:332-342` 在 `maybeHandleCombatEnter` 中调用 countUnit，false 立即返回，true 才依次写
  `pendingCombatEntryMaintenanceAt`、`lastCombatUiCleanAt`、日志并调用 `ensurePanelVisible`。
- `BattleRadarService:354-391` 保持 `onEnterCombat` 的 `battleCount++`、进入时间/快脱战状态复位、旧 exit
  pending 清理、`combatEnterPending=true`；countUnit 只做 one-shot：无 pending 返回 false，有 pending 清位后
  返回 true。与 696 对应段的分支、状态和顺序一致；baseline image 改为 typed-mechanics ready flag 属迁移载体替换，
  未改变本 countUnit 决策。
- A 不是“只有方法定义”：指定 caller 链可达。one-shot 清位也避免同一 enter signal 在后续 tick 重复消费。

### B - `DialogService::handleKeywordOption`

- 固定日志真实 EOF 为 `9399` 行。该 countUnit 的最新材料记录“方法体完整 typed，但 active Cloud 无 caller”；
  EOF 后续已换发另一 countUnit，因此本项仍是零计数 caller 缺口材料，不能与替代任务并记。
- active Cloud `NpcClickService:283/297` 只构造 `verifyExpectedOptionDialog`；`:817/868` 只构造
  `clickStory`。这些请求分别进入 verify/story 分支，不产生 `CLICK_KEYWORD`，因此不达
  `DialogService:761 handleKeywordOption`。
- `DialogService:151-230` 中 `handleKeywordOption` 只有两条内部入口：option-policy `CLICK_KEYWORD`
  非 `ROUTE_TRANSFER` 分支，以及 `WUHUAN_SHOE_SHOP_BUY_OPTION` 的 OCR fallback；当前 Cloud 搜索未发现
  `NpcClickService` 或其它 active task 构造这两类请求。工厂、enum、switch 和私有方法定义均不能当 caller。
- 方法体本身与 696 对应段一致：null keyword 返回 `OPTION_KEYWORD_NOT_FOUND`；否则按原参数调用
  `processOptionsWithOCRDetailed(request, allowFallback, false, true, detection)`，再映射 closed `DialogResult`。
  typed OCR/image/words 与 input-bundle mechanics 即使完整，也不能弥补入口不可达。

### C - `LeftTopStatusSwitchService::isSupportedTaskCode`

- 固定日志真实 EOF 为 `7166` 行，最新材料声称 `AutoBattleTask:199` 的 allowlist true branch 通向
  `consumeFollowerSafeWindow:205`。
- active `AutoBattleTask:197-205` 的真实数据流并非该叙述：`:199 requestedTeamTask` 只参与
  `requireLegacyTeamPathingGate = followerSupportMode && !localSupportSession && requestedTeamTask`；
  `:205 consumeFollowerSafeWindow` 则由独立的 `requireLocalSupportGate && LEFT_TOP_STATUS capability open`
  分支触发。也就是说，`:199` 的 true/false 不直接控制 `:205`。
- `requestedTeamTask` 的真实下游在 `AutoBattleTask:220-224`：控制 one-summon-per-round、
  `teamMaintenanceKey` 和 `requireOpenTeamMaintenanceWindow` 的 legacy maintenance gate。
- `consumeFollowerSafeWindow:87-102` 内部会再次调用 `isSupportedTaskCode`；不支持时返回
  `SwitchActionResult.skipped("unsupported-task")` 且不 observe/click。因此 countUnit 确有 active caller，
  但“caller true branch -> consume”交付链不成立；true legacy gate 与 local-support consume 是两条不同分支。
- allowlist 本身仍与 696 一致：仅 `xiuluo_v2/wubei/wuhuan_v2`、`equalsIgnoreCase`、null-safe false。
  active `AutoBattleTask` 的上述分支结构也与 696 快照一致，未发现状态或顺序漂移。

### D - `AutoCombatService::runPendingMemberCommonBoxIfAllowed`

- 固定日志真实 EOF 为 `7390` 行，最新材料是该 countUnit 的 `NO_CODE_CHANGE` 逐跳证据。
- `AutoCombatService:145-175` 有两个调用位置，但单 tick 不会重复执行成功消费：
  `consumeExitAndRecover` 为 true 时执行第一处并在该分支无条件返回；为 false 时才到第二处。
- `:476-517` 顺序为 `FREE -> pending -> leader-detection -> local capability -> task-turn enter -> consume ->
  finally forceRelease`。`consumePendingBoxIfAllowed` 的 closed boolean 原样返回；true 才由 caller 映射
  `EXIT_RECOVERED`，false 继续 first-aid。common-box 在 first-aid 前，未见顺序漂移。
- 该两段与 696 快照逐行内容一致（active 仅整体行号后移一行）。typed DHXY exact-window ordered click
  位于既有 CommonBox 下游；本 countUnit 没有另建 mechanics、owner、retry 或第二次 observation。

## Risks

1. **C 的交付链叙述与 active 数据流不符。** countUnit 是活 caller，但它在 `AutoBattleTask:199` 控制的是
   legacy maintenance gate，不是 `:205` local-support consume 分支。若父级按“true 直接通向已计 typed consume”作为
   计数前提，会把两条独立分支误并为一条。
2. **B 仍是方法定义/内部入口存在但 Cloud 业务 caller 不存在。** typed mechanics 与 closed result 只能证明方法体
   可执行条件下的完整性，不能证明 active 业务链；不得用 `DialogHandleRequest` 工厂、switch case 或私有方法互调代替 caller。
3. **下游重复计数风险。** A 引用既有 BattleRadar typed facts，C 引用既有
   `consumeFollowerSafeWindow`，D 引用既有 CommonBox has/consume/input bundle；这些仅是本 countUnit 的依赖证据，
   不应在本轮再次增加下游单位。B 为零计数材料，且 EOF 已出现替代任务，不能把旧单与替代单同时增加。
4. **终态折叠检查。** A 的 absent signal -> false、D 的 click boolean -> tick result、C 的 unsupported ->
   skipped 都与 696 保持；指定段未见把 STOPPED/UNKNOWN 或 mechanics failure 新折成业务成功/失败。
   B 虽保留 696 的 null/matched/fallback 映射，但因 caller 不可达，不能以该 closed result 宣称整链终态已交付。

## Parent-checklist

- [ ] A：只按 `consumeCombatEnterSignal` one-shot 单位核；确认 typed fact producer 仅作依赖，不重复计数。
- [ ] B：要求一个真实 Cloud `CLICK_KEYWORD`/shoe-buy caller 后再核整链；当前 NpcClick verify/story caller 不得替代。
- [ ] B：保持该旧单零计数，并与 EOF 后换发的 `PlayerStateService::syncMyIdentity` 分开记账。
- [ ] C：按真实链重写父级检查项：`:199 -> requireLegacyTeamPathingGate -> maintenance request`；不要写成
  `:199 true -> :205 consumeFollowerSafeWindow`。
- [ ] C：若计数定义必须落到 typed LEFT_TOP mechanics，明确是核纯 allowlist policy unit，还是核 service 内部
  `consumeFollowerSafeWindow:88` 的二次 gate；两者不可混写。
- [ ] D：确认两个调用位置由 `consumeExitAndRecover` 分支互斥，且只增加
  `runPendingMemberCommonBoxIfAllowed`，不重复增加 CommonBox has/consume/input-bundle 单位。
- [ ] 全部：父级最终复核时继续以 `696a12b0` 为业务基线；本报告只提供行级预检材料，不作绑定裁决。
