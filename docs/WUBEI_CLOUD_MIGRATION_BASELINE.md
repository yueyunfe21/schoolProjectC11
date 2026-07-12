# 五倍（Wubei）云端迁移 · 现状 Baseline

> CR265 第一步产物。**目的**：在动手把五倍迁成"云端单脑"之前，把 `WubeiTask` 当前的本地行为（相位机 / 决策点 / 已迁 OCR / 独有业务）精确锁定成一份对照基线，作为后续逐条迁移的验收参照——迁移后云端行为必须**逐条复现**本文件描述的当前行为，不新增 TTL / 校验 / park / retry / cleanup / fallback（沿用修罗迁移契约 `XIULUO_CLOUD_MIGRATION_PLAN.md` §12.1 的"严格复现基线"原则）。
>
> **本文件不改任何业务代码**，只做只读盘点。

---

## 0. Baseline 锚点

| 项 | 值 |
|---|---|
| 行为基线 commit | **`91d3b070`**（"当前工作状态基础快照（开分支前）"，用户于开五倍迁移分支前所打）。历史链：`696a12b→dc4394f→9aa987d1(修罗云端能跑)→91d3b070`。⚠️ 此链**不含** CR257 的 OCR 清零（`bf3bf387` 在另一分支），故本基线本地 OCR 腿仍在（见 §7 R5）|
| 已知脏 delta | `WubeiTask.java` 相对 HEAD 仍有 **9+/23-** 未提交改动；`CloudDecisionServiceId.java`、`MiniMapLocation*` 亦 dirty。**冻结为正式参照前，应先提交或复核这些 delta**，否则"基线"不可复现（见 §7 R3）|
| 工作区基线副本 | `D:\mavenProject\DHXY-local-baseline`、`DHXY-917ba16`、`DHXY-xiuluo` 为已有的基线快照工作副本；五倍迁移若需要"离线对照运行"，可比照修罗建立同类副本 |
| 修罗对照范本 | 修罗已迁到 `XIULUO_BRAIN` 云端单脑，是本次迁移的架构范本与验收标准（见 §1）|

主文件：`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java`（约 4943 行）、`WubeiPhase.java`、`WubeiWaitReason.java`、`WubeiRoundContext`。

---

## 1. 目标架构（修罗范本，一句话标准）

修罗的迁移形态 = **单云脑持有全部决策，本地是"哑执行壳"**，三段循环：`start` → 云端下发 `XiuluoBrainActionType` 指令（`EXECUTE_PHASE` / `RUN_CLEANUP` / `WAIT_FOR_EVENT` / `RESTART_ROUND` / `COMPLETE_ROUND` / `FAIL_TASK` / `STOP_TASK` 共 7 类）→ 本地只执行物理动作 + 采集结构化 facts → `actionOutcome` 上报 → `step` 取下一条指令。

范本关键文件（迁移时逐一对标建 wubei 版）：
- 客户端壳 + facts：`XiuluoTaskV2.java`（循环 `runRoundWithXiuluoBrain` `:604`、指令壳 `executeXiuluoBrainCommandShell` `:864`、事件 park `waitForXiuluoBrainEvent` `:1202`、facts `xiuluoBrainOutcomeFacts` `:1384`、纯执行 `runPhase` `:2701`）
- 客户端协议/状态：`com.bot.dhxy.cloud.xiuluo.*`（9 个 `XiuluoBrain*` 文件）+ `XiuluoBrainRoundState`、`XiuluoWaitSpec`、`XiuluoWaitReason`、`XiuluoPhase`
- 云脑：`dhxy-cloud-brain\...\DecisionEngine.java`（hooks `:337`、分派 `nextXiuluoBrainCommand` `:718`、每 phase 一个 `*Next` `:797-1444`、有状态 `XiuluoBrainSession` 内类 `:3878`）
- 契约测试：`XiuluoCloudBrainContractTest.java`

**迁移终态判据**：本地 `WubeiTask` 不再持有任何"下一步 phase / 重试 / restart / maintenance 判定"决策；云端 `WUBEI_BRAIN` session 持有全部决策并只据 facts（非日志串）判定；断云一律 fail-closed，无第二本地脑。

---

## 2. 当前相位机（本地权威 · 待迁云端）

`runRoundPhases`（`WubeiTask.java:538-641`，注释自认"采用修罗 V2 轻量 phase runner"）驱动，分派在 `runPhase` 的 `switch`（`:1601-1622`）。终态三元组 `ROUND_DONE / FAILED / STOPPED` + `isTerminal()`（`WubeiPhase.java:27-29`）。

| WubeiPhase | 本地做什么 | 入口 | 修罗对应 |
|---|---|---|---|
| `HOT_START_DETECT` | 热启动：有活动任务→READ_TRACKER；轮次已完成→WAIT_TEAM_RETURN；否则→ROUTE_TO_MAIN_TASK | `:1624` | (hot-start facts) |
| `ROUTE_TO_MAIN_TASK` | 寻路到接任务 NPC「降魔侍卫」(86,87) | `:2179` | ACCEPT_TASK_NAVIGATE_TO_NPC |
| `ACCEPT_TASK` | 点 NPC + 接任务对话 | `:2276` | ACCEPT_TASK_CLICK_NPC/DIALOG |
| `READ_TRACKER` | **权威快照边界**：读追踪面板、判暗雷 reroll、黄袍连战、探测任务计时 | `:1883` | READ_OBJECTIVE |
| `AFTER_ACCEPT_MAINTENANCE_CHECK` | 接任务后维护检查（医宝宝广播前置）| `:1652` | AFTER_ACCEPT_MAINTENANCE_CHECK（**同名**）|
| `BEFORE_TRACKER_PATHING_MAINTENANCE_CHECK` | 寻路前维护检查（修装备广播前置）| `:1671` | BEFORE_ROUTE_MAINTENANCE_CHECK |
| `TRACKER_PATHING` | 点绿链发起寻路 | `:1959` | TRY_TRACKER_SHORTCUT / WAIT_TRACKER_SHORTCUT_PATHING |
| `RESOLVE_AFTER_PATHING` | 寻路终态解析 → ENTER_BATTLE | `:1988` | NAVIGATE_TO_TARGET / CLICK_TARGET_NPC |
| `ENTER_BATTLE` | 点进战斗对话（消灭它/证明实力/魁星归位模板）| `:2042` | CONFIRM_ENTER_BATTLE |
| `WAIT_BATTLE_FINISH` | 等战斗结束 | `:2064` | WAIT_COMBAT |
| `POST_BATTLE_RECOVER` | 战后恢复 | `:2077` | (post-combat) |
| `RETURN_HOME` | 开返回道具回宝象国 | `:2114` | RETURN_HOME（**同名**）|
| `WAIT_TEAM_RETURN` | 等队伍归队 | `:2118` | WAIT_TEAM_RETURN（**同名**）|

相位粒度：wubei 更粗（接任务/寻路各一个 phase），修罗更细。迁移时 phase 集大体沿用现有 `WubeiPhase`，不必强行拆到修罗粒度。

---

## 3. 本地做的「决策」逻辑（迁移标的 · 逐项要上云）

以下都是当前写死在本地、迁移后必须由 `WUBEI_BRAIN` 的 `*Next` 决策函数持有的分支：

1. **下一步 phase**：各 `run*Phase` 里几十处 `state.next(WubeiPhase.*, reason)` 硬编码（`:1601-2225` 等）。
2. **失败重试 / round restart**：`recoverRoundAfterFailure`（`:660`，`recoverTo(ROUTE_TO_MAIN_TASK)` 或超限 FAILED）；超时重接 `consumePostCombatIdleTimeoutBeforeNormalPhase`（`:849`）、`consumeOrdinaryPreBattleTimeoutBeforeNormalPhase`（`:883`）、`timeoutProbeTaskBeforeBattleIfNeeded`（`:1522`）。常量：`MAX_CHAINED_COMBAT_ATTEMPTS=5`、`MAX_TRACKER_CLICK_ATTEMPTS=12`、`MAX_TRACKER_ANCHOR_RECOVERY_ATTEMPTS=5`、`PROBE_ENTER_BATTLE_TIMEOUT_MS=300_000`（`:190-194`）。
3. **maintenance 判定**：`runAfterAcceptMaintenanceCheck` / `runBeforeTrackerPathingMaintenanceCheck`（`:1652-1681`）、`triggerHealPetBroadcastBeforeTracker`（`:1683`）、`triggerRepairEquipmentBroadcastBeforeTracker`（`:1710`）、`triggerMaintenanceBroadcastBeforeTracker`（`:1738`）。常量 `MAX_MAINTENANCE_HOOK_ATTEMPTS=5`、`MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES=3`（`:179-180`）。
4. **任务分类判定**（基于云端 OCR 结果再做本地分支）：暗雷 `isTrackerDarkThunderTask`（`:1894`）、黄袍 `isTrackerChainedCombatTask`（`:1909`）、探测 `isTrackerProbeTask`（`:1912`）、5 子任务 key `isTrackerDianqianXianyi/BaoxiangMiqing/ZhidouHuangpao`（`:2643-2651`）。
5. **坐标/目的地解析正则**（本地仍活）：`TRACKER_DEST_HINT_PATTERN`（`:238`）、`TRACKER_COMBAT_TARGET_PATTERN`（`:240`）——即便云端有同款，本地这份仍在消费。

---

## 4. 本地做的「纯执行」动作（迁移后保留本地）

截图/点击/导航/模板匹配/战斗 tick 保留本地（对标修罗 `runPhase` 只做物理动作）：
- 截图/采样调度：目的地黄字 3 帧固定采样 `{500,1000,1500}`ms、ROI `(350,370,679,463)`（`:213-216`）；tracker anchor/面板几何常量（`:201-210`）。
- 点击：`npcClickService.clickNpcSmart`（`:1779,2305,3901`）、`confirmPendingSmartClick`（`:965,2362`）、`tryDirectCombatTargetClick`（`:3942`）。
- 导航：`navigationService.navigateToNPC`（`:1759,2188`）、`navigateInCurrentMap`（`:1940,2453`）。
- 对话/模板：`dialogService.detectDialogTypeNoFocus`（`:3110,3141`）、`handleDialog`（`:3828`）；本地模板 `wubei_accept_chumoweiguo.png`、`wubei_enter_battle_{xiaomie,zhengming,kuixing}.png`、`wubei_probe_story_koukou.png`、`bag/wubei_probe_item.png`、`bag/wubei_return_item.png`、`wubei_tracker_anchor.png`（`:157-170`）。
- 战斗：`autoCombatService.handleCombatTick / probeWindowCombatStateReadOnly / initializeForCurrentWindow`（`:385,1020,4101`）。

---

## 5. 已经上云的部分（迁移前置工程 · 已具备）

wubei 与修罗共享同一套云端 decision 基础设施，OCR 已上云（CR248 / CR208-10 / CR249 成果）：

| 能力 | 客户端调用 | 云端 action / recognizer |
|---|---|---|
| Tracker 面板读取 | `taskTrackerPanelService.readWubeiTrackerPanel`（`:2391,2707,4601`）| `DecisionEngine` `wubei`+`DETAIL_BLOCK_CROP`：`matchWubeiTaskKey` / `wubeiYellowTextByOcr` / `wubeiGreenLinkMapNameByOcr`（`DecisionEngine.java:1596-1645,3559`）|
| 目的地黄字提示 OCR | `objectiveTextReaderCloudDecisionService.readWubeiDestHint`（`:3640`）| `WUBEI_DEST_HINT_READER` → `WubeiDestHintRecognizer`（cloud）|
| TASK_POLICY（**影子**）| `applyTaskPolicyCloudDecision`（`:691`）| 本地先决策、云端可否决/覆盖下一 phase |
| TASK_RECOVERY（**影子**）| `decideTaskRecovery`（`:772`）| 失败恢复上报 |
| TrackerLinkRanker（**影子**）| `trackerLinkRankerCloudShadowService.shadowTrackerLinkSelection`（`:3065,4795`）| 非 authoritative |

⚠️ **TASK_POLICY / TASK_RECOVERY 当前是"本地决策 + 云端否决"的影子语义**，与修罗"云端权威下发"相反。迁移必须把它切换为云端单脑，否则会留双脑（见 §7 R4）。

---

## 6. wubei 独有业务（修罗脑无对应命令语义 · 迁移必须新表达）

这些是照抄修罗指令集**盖不住**的，迁移设计必须先在协议/facts 里精确定义：

1. **暗雷怪 reroll**：`READ_TRACKER` 判暗雷 → `startDarkThunderAcceptNpcReroute`（`:1908,1939`）重接任务换目标。关键字 `暗雷怪`（`:187`）。
2. **显形镜双绿链探测 + 白龙马 story**：探测任务（`isTrackerProbeTask` `:1912`），`PROBE_TARGET_NPC_NAME="白龙马"`（`:189`），story 判定模板 `wubei_probe_story_koukou.png`；相关死代码 `hasFreshVisibleProbeStory`（`:2227`）、`beginProbeStoryWaitFromVisibleDialog`（`:2234`）见 WUBEI_BUSINESS_DIFF_AUDIT.md。
3. **黄袍连战 continue/return**：`isTrackerChainedCombatTask`（`:1909`），`MAX_CHAINED_COMBAT_ATTEMPTS=5`（`:190`），战后判定继续下一场还是回程。
4. **探测/返回道具流程**：`bag/wubei_probe_item.png`、`bag/wubei_return_item.png`（`:167-170`），`returnItemPrescanService`（`:289`）。
5. **5 子任务 taskKey 分支**：`dianqian_xianyi`（电前显形/显形镜）、`sancang_fengmo`（三藏封魔）、`baoxiang_miqing`（宝箱觅青）、`zhidou_huangpao`（智斗黄袍/连战）、`kuixing_guiwei`（魁星归位）（云端 `DecisionEngine.java:3561-3570`）。

---

## 7. 清理欠债 & 现状矛盾（迁移中要一并处置）

| 编号 | 事项 | 证据 |
|---|---|---|
| R3 | baseline commit `91d3b070` 上 WubeiTask 仍有 9+/23- 脏改动，冻结前须提交/复核 | `git diff --stat` |
| R4 | TASK_POLICY/TASK_RECOVERY 影子语义与云端单脑相反，迁移须切换 + 加 no-dual-brain source guard（对标修罗 CR196/CR201）| §5 |
| R5 | **本地 OCR 遗留腿在本基线上确实还在**：`WubeiTask` 仍 `import`+持有 `TextRecognizer`（`:18,:299`），`parseTrackerDestinationHintCapture`（`:3610`）在 cloud inactive 时走 `@Deprecated(CR208-10)` 本地 OCR 兜底 `:3621/:3692`。CR257「本地 OCR 清零」的提交 `bf3bf387` **不是本基线 `91d3b070` 的祖先**（两分支已岔开；基线历史为 `696a12b→dc4394f→9aa987d1(修罗云端能跑)→91d3b070`，不含 CR257）。迁移团队须先决策：**先把 CR257(`bf3bf387`) 合入五倍迁移分支，还是在五倍迁移内一并下沉 OCR 引擎**——否则本地 OCR 腿会成为隐性 fallback | `WubeiTask.java:18,299,3610-3692`；`git merge-base --is-ancestor bf3bf387 HEAD` = 否 |
| R6 | 死代码 `hasFreshVisibleProbeStory`（`:2227`）、`beginProbeStoryWaitFromVisibleDialog`（`:2234`）| WUBEI_BUSINESS_DIFF_AUDIT.md |
| R7 | **cloud-brain 侧零 wubei 测试**；迁移须补 `WubeiCloudBrainContractTest` 对标 `XiuluoCloudBrainContractTest` | `dhxy-cloud-brain\src\test` 无命中 |

DHXY 侧现有 15 个 wubei wiring 测试（`task/wubei/` 13 + `window/execution/` 2），均不覆盖云端脑（因尚不存在）。

---

## 8. 迁移待补清单（= 与修罗范本的差距）

完全缺失、需新建（对标 `com.bot.dhxy.cloud.xiuluo.*` 9 文件 + 云端 session）：

- [ ] `CloudDecisionServiceId.WUBEI_BRAIN` 枚举
- [ ] 客户端协议包 `com.bot.dhxy.cloud.wubei`：`WubeiBrainActionType` / `WubeiBrainStartRequest` / `WubeiBrainStepRequest` / `WubeiBrainActionOutcomeRequest` / `WubeiBrainResponse` / `WubeiBrainDecision` / `WubeiBrainActionOutcomeDecision` / `WubeiBrainCloudDecisionService`
- [ ] 客户端 `WubeiTask.runRoundWithWubeiBrain` 循环 + `executeWubeiBrainCommandShell` + `waitForWubeiBrainEvent` + `wubeiBrainOutcomeFacts`
- [ ] 客户端状态 `WubeiBrainRoundState`（loop guard / 事件 park / pre-battle watchdog）、复用/扩展 `WubeiWaitSpec` / `WubeiWaitReason`
- [ ] 云脑 `DecisionEngine`：`wubeiBrain` start/step/actionOutcome hooks + `WubeiBrainSession` 有状态内类 + 每 phase 一个 `*Next`（键 facts、注 `// Baseline Lxxxx`）
- [ ] wubei 独有业务的命令/facts 语义（§6 五项）
- [ ] `wubei-brain` feature flag 默认 off，旧本地路径保留为 rollback（对标 CR195）
- [ ] no-dual-brain source guard（对标 CR196/CR201）
- [ ] `WubeiCloudBrainContractTest` + DHXY 壳 wiring 测试
- [ ] 收尾：切换/删除 §7 R4/R5/R6 遗留

---

## 9. 迁移契约（沿用修罗，禁止项）

- 云端只据**结构化 facts** 决策，绝不匹配日志串（修罗 CR200 已删串匹配）。
- 断云 / 云端无效决策 / 超时 → **fail-closed**，不回退本地旧业务（`cloud.required`）。
- stop / pause / window / input safety 永远本地，不得被包装成 cloud-required FAILED。
- 严格复现本 baseline 行为，**不新增** TTL / 校验 / park / retry / cleanup / fallback。
- 每个云端决策分支注明其 baseline 源行（`91d3b070` 的 `WubeiTask.java:Lxxxx`）。
