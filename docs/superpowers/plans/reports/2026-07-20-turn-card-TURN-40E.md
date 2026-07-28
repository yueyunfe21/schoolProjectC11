# TURN-40E：Post-696 本地逻辑等价迁移整卡

## Canonical Card State

- Status: `SOURCE DELTAS REVIEW PASSED / COMPLETION CLAIM REOPENED / TURN-40F DEFAULT-CUTOVER+THICK-TASK-RETIREMENT REQUIRED / ZERO OWNER`
- Created: `2026-07-20T00:05:33-04:00`
- Parent reviewer: 当前父级 Codex（唯一 final reviewer）
- Implementation owner: `ZERO OWNER`（原 Worker 已在 Parent Review #3 后释放）
- Depends on: TURN-40D source/build review passed；用户已明确批准迁移当前本地 workspace 全部逻辑
- Blocks: TURN-41 fresh runtime
- Design contract: `docs/superpowers/plans/2026-07-20-cr271-post-696-local-delta-cloud-migration-plan.md`

## 1. 唯一业务基线

`D:\mavenProject\DHXY` 当前 workspace 是本卡唯一业务真相：

- branch: `codex/baseline-696a12b0`
- HEAD: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- 创建卡时 dirty/untracked individual entries: 128
- 权限：严格只读；禁止切分支、写文件、格式化、生成构建产物、clean/reset/checkout

Worker 必须把该 workspace 相对 `696a12b0` 的现有逻辑逐项映射到 CR271 最终架构。等价指业务条件、phase
顺序、候选优先级、fallback 顺序、重试/次数、等待、pause/stop、输入原子性、模板/ROI/阈值和窗口绑定均一致；
不能只保证类名或最终返回值相似。

## 2. 架构硬边界

- Cloud 唯一拥有任务 phase、业务决策、识别策略、候选排序、重试/恢复语义。
- 本地唯一拥有 HWND capture、物理 input、输入队列、窗口短期 cache 和复合原子动作。
- `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService` 永久本地。
- `WindowTaskRunner` 永久本地，只观察事实/消费 prepared action，不建立第二 task phase。
- `WindowRuntimeContext` 是窗口状态唯一属主；禁止 Cloud mirror store。
- 复用现有 HTTPS turn/remote payload；禁止第二协议、第二 store、第二 OCR/模板/候选算法。
- 不允许把旧类整文件覆盖到 CR271 或 Cloud；必须在当前架构中方法级吸收。
- 不能安全裁决的真实业务差异必须停止并在本卡 EOF 写 `PLAN-CONTRACT BLOCKED`，不得 stub/恒 null。

## 3. 必须闭合的行为簇

1. 输入请求同时携带 pause/stop；pause 在 focus/action 前阻塞并恢复同一请求；暂停时长不计 120s waiter；
   stop/identity drift 仍取消；exclusive callback pause checkpoint 保留。
2. 修罗/五倍次数 `0=无限`，无正数上限，UI 显示“无限”；五环计数规则不变。
3. Tracker：新 raw 标题、窗口级 anchor cache、局部 ROI 优先、masked full-window fallback、固定位置拖动、
   紧凑新字体绿链、五环 title-only；不得改变 task phase/order。
4. NPC：灵兽使者/白龙马/降魔守卫/default 黄点 profile；profile mask；玩家紫 anchor 上方 50px direct
   candidate；同一 Alt+A session 中 direct→Ctrl/FIFO，全部策略耗尽后才退出。
5. Maintenance/dialog/supply：固定 raw ROI `(260,373)-(378,413)`、阈值 0.85、150ms/800ms；删除 broad
   dialog fallback；只做目标关闭；仅真实 HP/MP click 后在同一 atomic input sequence 移鼠标。
6. Summon：LOCKED/EMPTY/OCCUPIED/UNKNOWN 静态槽；只 hover OCCUPIED；删除后静态复核；IF8 ROI、0.80、
   inactive 色距 12、ultimate 生成后 2500ms 等待；无 broad cleanup。
7. FiveRing：Runner 拥有 tracker scan/click；`RUNNER_PREPARED_NOT_READY`；title-only hot start；stale action
   等 Runner refresh；combat recovery 清旧 tracker intent。
8. Xiuluo：maintenance hook 上限 2；失败不重置 round；选择性 dialog cleanup；无 generic accept fallback；
   world-map yellow destination mini-map pathing confirmed 可进入既有 FIRST_AID window。
9. Auto-combat：`auto_remaining.png` 原图阈值 0.80、中心偏移 +43/+28，保留 Alt+8 retry。
10. runtime JSON 只做 schema/单一属主/一次性 import 说明；不得覆盖用户现场数据。生产资产按真实消费者打包，
    debug/evidence 图片不得激活为业务资源。

## 4. 冻结写集

### 4.1 严格只读证据源

`D:\mavenProject\DHXY` 全部路径只读，尤其是相对 696 的 22+1 production Java、图片和 runtime JSON。

### 4.2 DHXY-cr271 production 允许写入

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskStartRequest.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionScope.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillTailBoundaryScanner.java`
- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- `src/main/java/com/bot/dhxy/service/UICleanerService.java`
- `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java`
- `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`
- `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelRectLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/model/WindowTrackerAnchorMemory.java`（可新增）
- 既有 `RemoteTaskTracker*`、`RemoteNpc*`、`RemoteSummonSkill*`、`RemotePlayerStateFirstAid*`、
  `RemoteAutoCombatPanelFact` payload 文件，仅在字段确实不足时修改；不得新建平行 payload 族。
- 与上述真实消费者直接对应的 production template resources。

### 4.3 dhxy-cloud-brain production 允许写入

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskStartRequest.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillStaticSlotPolicy.java`
- `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
- `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudDecision.java`
- `src/main/java/com/bot/dhxy/cloud/task/TrackerPanelReaderCloudRequest.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- 既有 `CloudNpcPlayerAnchorPort`、`CloudNpcYellowTargetPort`，仅在既有 payload 字段不足时修改。
- 与上述真实消费者直接对应的 production template resources。

若完整传递依赖需要写入未列路径，Worker 不得自行扩大范围；先在本卡 EOF 报精确符号、调用链和建议写集。

### 4.4 文档允许写入

- 本卡 physical EOF（claim/progress/delivery）
- `docs/ACTIVE_WORK.md`
- `docs/PACKAGE_ARCHITECTURE.md`
- `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`
- `docs/superpowers/plans/2026-07-20-cr271-post-696-local-delta-cloud-migration-plan.md`
- 两份迁移/验收矩阵与 `docs/cr-dashboard-data.js`

## 5. 禁止项

- 不创建、恢复或运行本地 tests/debug/replay/source guards；用户本轮未点名授权 test family。
- 不运行 application/server/Task/UI/capture/input，不操作游戏窗口。
- Java writer 活动时不运行 Maven。
- 不修改/删除任何卡外 dirty/untracked，不做 Git mutation。
- 不把 runtime JSON 或 evidence 图片直接复制进 Cloud runtime。
- Worker 不得自审为 Approved，不得改 TURN-41 为 READY。

## 6. Worker 交付证据

Worker 必须在本卡 physical EOF 追加：

1. canonical `WHOLE-CARD CLAIMED`，含 agent id、三仓 branch/HEAD/status count。
2. 23 个 production Java 本地差异的逐项 ledger：源方法/语义、目标路径/方法、分类和证据。
3. 每个行为簇的调用链、窗口/身份/phase/输入顺序等价说明。
4. 实际修改路径及 before/after SHA256；卡外路径零修改证明。
5. production/evidence/data manifest；runtime JSON 零覆盖证明。
6. writer 稳定后运行：
   - DHXY-cr271: `mvn -q -DskipTests compile`
   - dhxy-cloud-brain: 当前启动路径要求的 `mvn -q -DskipTests compile` 或更强 package
7. 最终只能写 `SOURCE+COMPILE DELIVERED` 或精确 `PLAN-CONTRACT BLOCKED`，不得写 Approved。

## 7. 父级审核门

父级收到 canonical delivery 后，逐文件对照 `D:\mavenProject\DHXY` 当前实现与
`docs/业务逻辑.md`，给出 `P0/P1/P2`、精确文件/方法/证据、遗漏项和返修条件：

- 任一业务条件、顺序、次数、fallback、等待、输入原子性或模板参数不等价：整卡 `REPAIR REQUIRED`。
- 任一 local/cloud 双算法、第二 store/protocol、本地 task phase：至少 P1。
- 任一卡外写入、基线 workspace 写入、构建失败：至少 P1，禁止 TURN-41。
- 仅当 `P0/P1/P2=0/0/0`、双端编译成功且 ledger 无遗漏，写 `SOURCE REVIEW PASSED`。
- source review passed 后仍需用户 fresh runtime，父级/Worker 均不代替用户启动。

<!-- TRUE_EOF: TURN-40E WHOLE-CARD READY ZERO-OWNER POST696-EQUIVALENCE-MIGRATION 2026-07-20T00:05:33-04:00 -->

## TURN-40E WHOLE-CARD CLAIMED

- Agent id: `Codex-TURN-40E-implementation-worker-20260720`
- Claimed at: `2026-07-20`（America/New_York）
- 唯一业务基线（严格只读）：`D:\mavenProject\DHXY`，branch=`codex/baseline-696a12b0`，HEAD=`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，status count=`128`
- Thin client：`D:\mavenProject\DHXY-cr271`，branch=`thin-client-design`，HEAD=`59b85e0bb494f43ad7e7434f3d2170deb373c6ef`，pre-claim status count=`83`
- Cloud：`D:\mavenProject\dhxy-cloud-brain`，branch=`navigation-migration`，HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`，status count=`588`
- Claim 边界：本次 claim 前未写 production；三个仓既有 dirty/untracked 全部受保护，禁止 revert/reset/clean/checkout/覆盖。

<!-- TRUE_EOF: TURN-40E WHOLE-CARD CLAIMED CODEX-TURN-40E-IMPLEMENTATION-WORKER-20260720 -->

## PLAN-CONTRACT BLOCKED

- Blocker id: `TURN-40E-PCB-01 / REMOTE TASK RUN COUNT NOT TRANSPORTED`
- 发现阶段：Wave 0 / LD-02 方法级对账；尚未写入任何 production Java/resource，尚未运行 Maven。
- 必须保持的基线语义：`MainWindowController.normalizeRunCount` 接受 `0` 且不封顶，`formatTaskCountSummary` 把
  `0` 显示为“无限”；修罗/五倍 Cloud task 必须读取该 exact 用户值，`0` 无限、正数按原值执行；五环仍保持
  自己的 1/2 规则。
- 当前真实调用链：
  `MainWindowController.syncTaskRunCountToProperties`（本地 `BotProperties`）
  -> `WindowTaskControlService.toTurnTaskCodes`
  -> `WindowTaskControlService` 构造 `TurnTaskStartRequest(startRequestId, taskCodes, failurePolicy)`
  -> HTTPS `TurnRequest`
  -> `TurnProtocolValidator.requireTaskStartRequest`
  -> `CloudTurnTaskRuntime.start/runQueue`
  -> `CloudTurnTaskFactory.resolve` 创建 per-window/per-run prototype
  -> `WubeiTask.execute` / `XiuluoTaskV2.execute` / `FiveRingTaskV2.execute`
  -> 各 Task 读取 Cloud 进程自己的全局 `BotProperties.get*MaxRuns()`。
- 缺口：现有唯一 HTTPS start protocol 完全没有 run-count 字段；本地 UI 的 exact 值没有跨端传递。Cloud 全局
  `BotProperties` 既不是该 window/run 的事实，也无法在多窗口并发时安全代替 per-run 值。把缺失值默认为 1、
  恒 null、读取 Cloud 本机配置或新增第二协议/store，都会违反本卡等价与唯一属主合同。
- 卡外必需 production 符号/路径（最小闭包，需父级先冻结具体表示）：
  1. 双仓 byte-identical `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskStartRequest.java`：承载 ordered
     queue element 的 exact run-count，不能只按 task code 建全局 map 而丢失重复队列项身份。
  2. 双仓 byte-identical `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`：校验修罗/五倍
     `count >= 0`、五环只允许 1/2，并保持 queue order/correlation。
  3. DHXY-cr271 `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`：从当前本地
     `BotProperties`/ordered `WindowTaskQueue` 投影 exact count 到同一个 start request。
  4. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java` 与
     `CloudTurnTaskFactory.java`：把当前 queue element 的 count 绑定到同一个 exact window/taskRun prototype，
     不能写 Cloud 全局配置或建立第二 store。
  5. Cloud `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`：接收 per-run frozen count；该文件不在本卡
     Cloud 冻结写集，但它是五倍 loop 唯一业务属主。
  6. Cloud `XiuluoTaskV2.java`、`FiveRingTaskV2.java` 需要消费同一 per-run frozen count；二者虽已在写集，
     但没有前述 protocol/runtime/factory 闭包时不能安全实现。
- 必要计划裁决：父级须先决定是在现有 `TurnTaskStartRequest` 内把 `taskCodes` 升级为 ordered typed entries，
  还是增加与每个 queue index 精确关联的 byte-identical count payload；无论哪种都必须仍是同一个 HTTPS start
  protocol、无第二 store、无 Cloud 全局 mutation，并补齐上述真实 caller/runtime/factory/Wubei 写集。
- 当前卡外零写入证明：唯一已写文件为本卡 Markdown；`D:\mavenProject\DHXY` 严格只读，Cloud 零写入，
  DHXY-cr271 production/resource 零写入。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED REMOTE-RUN-COUNT-TRANSPORT 2026-07-20 -->

## PARENT PLAN-CONTRACT REPAIR #1 / OWNER RETAINED

- Time: `2026-07-20T00:16:00-04:00`
- Parent adjudication: `TURN-40E-PCB-01` 已完成传递闭包审计；这是现有 HTTPS start request 的字段闭合，
  不涉及业务语义选择，也不建立第二协议/store。原 Worker owner 保留并继续整卡。
- 冻结表示：双仓 byte-identical `TurnTaskStartRequest` 在现有 `taskCodes` 旁新增
  `List<Integer> taskMaxRuns`，与 queue index 严格一一对应。允许保留 3-arg Java compatibility constructor，
  但真实 HTTPS start validator 必须拒绝 null、长度不等或非法值；不得默认为 1 后继续执行。
- 本地 producer：`WindowTaskControlService` 在每次 remote start 时从当前本地 `BotProperties` 按 ordered
  `WindowTaskQueue` 冻结同索引值：`WUBEI -> fivefoldMaxRuns`、`XIULUO_V2 -> xiuluoMaxRuns`、
  `WUHUAN_V2 -> wuhuanMaxRuns`、`AUTO_BATTLE -> 1`。selected-task 同样生成单元素 count list。
- validator：`WUBEI/XIULUO_V2 >= 0`；`WUHUAN_V2` 只允许 1/2；`AUTO_BATTLE` 只允许 1；
  必须同时验证 list size 与 index 对应 task code，不得改 queue order/correlation。
- Cloud per-run 传递：`CloudTurnTaskRuntime` 按 index 将 count 写入该 queue element 的 immutable
  `CloudTaskServiceMetadata`；`TaskExecutionContext` 只暴露 immutable configured max-runs view，不保存可变
  业务状态。`CloudTaskServiceMetadata` 可提供旧 constructor 兼容非 task binding context，但 turn-native task
  context 缺 count 必须 fail-closed。
- Task 消费：Cloud `WubeiTask`、`XiuluoTaskV2`、`FiveRingTaskV2` 在 `execute(context)` 开始读取该 exact
  per-run count；turn-native 路径禁止再读 Cloud 全局 `BotProperties` 作为次数真相。旧非 turn delegate 路径
  可保留既有配置读取以不改变 rollback 行为。
- 写集修复：本卡 4.2/4.3 已加入双仓 request/validator、local producer、Cloud context/metadata/runtime、
  Cloud `WubeiTask`；`XiuluoTaskV2`/`FiveRingTaskV2` 原已在写集。`CloudTurnTaskFactory` 无需修改，禁止通过
  prototype provider 或全局配置注入可变次数。
- 复验要求：23 路径 ledger 另增加 `LD-02 transport closure` 行，证明重复 task code 的两个 queue index
  各自保持 count；不创建/运行 test，compile gate 不变。

<!-- TRUE_EOF: TURN-40E PARENT-PLAN-CONTRACT-REPAIR-1 OWNER-RETAINED ALIGNED-TASK-MAX-RUNS SAME-HTTPS-PROTOCOL 2026-07-20T00:16:00-04:00 -->

## Blocked Handoff Ledger

### 23 个 production Java 路径（Wave 0 初步分类）

| # | 只读基线路径 | 本地差异语义 / 最终属主 | 分类 / 当前证据 |
|---|---|---|---|
| 1 | `input/action/InputActionQueue.java` | pause-aware await、暂停补偿 waiter；本地 input 基础设施 | `MIGRATE`；因 PCB-01 未写 |
| 2 | `input/action/InputActionRequest.java` | 同一请求携 pause/stop、恢复原请求；本地 input | `MIGRATE`；未写 |
| 3 | `input/action/InputActionScope.java` | exact task pause/stop scope；本地 input | `MIGRATE`；未写 |
| 4 | `input/action/InputActionWorker.java` | focus/action 前 checkpoint、exclusive callback pause；本地 input | `MIGRATE`；未写 |
| 5 | `runner/stop/TaskPauseToken.java` | pause duration/stop wakeup；本地 checkpoint | `MIGRATE`；未写 |
| 6 | `service/AutoCombatPanelService.java` | `auto_remaining.png` 0.80、+43/+28、Alt+8 retry；识别 Cloud/输入本地 | `ALREADY_EQUIVALENT` 候选；尚未完成 method proof |
| 7 | `service/BagService.java` | pause/stop-safe closed local mechanics | `MIGRATE` 到永久本地 Service；未写 |
| 8 | `service/DialogService.java` | 删除 broad maintenance fallback、目标化 dialog；策略 Cloud/动作本地 | `MIGRATE`；未写 |
| 9 | `service/NpcClickService.java` | direct candidate 与 Ctrl 原子本地动作；候选顺序 Cloud | `MIGRATE`；未写 |
| 10 | `service/PlayerStateService.java` | 真实 HP/MP click 后同序列 mouse-away；策略 Cloud/供给动作本地 | `MIGRATE`；未写 |
| 11 | `service/QuestManagerService.java` | pause/stop-safe tracker click/capture；永久本地 Service | `MIGRATE`；未写 |
| 12 | `service/SummonSkillService.java` | 静态槽四态、IF8、复核、2500ms；策略 Cloud/复合动作本地 | `MIGRATE`；未写 |
| 13 | `service/TaskMaintenanceService.java` | raw broadcast ROI/模板/等待、维护策略 | `MIGRATE` 到 Cloud policy + local operation；未写 |
| 14 | `service/TaskTrackerPanelService.java` | anchor cache/ROI/fallback/drag 本地；标题/绿链算法 Cloud | `MIGRATE`；未写 |
| 15 | `service/UICleanerService.java` | 仅目标化 close 与 pause-safe atomic input；永久本地 Service | `MIGRATE`；未写 |
| 16 | `service/dialog/DialogHandleRequest.java` | 删除 broad maintenance fallback 字段 | `MIGRATE`；未写 |
| 17 | `task/wuhuan/FiveRingTaskV2.java` | prepared-not-ready/title-only/stale refresh/combat intent cleanup；Cloud phase | `MIGRATE`；未写 |
| 18 | `task/xiuluo/XiuluoTaskV2.java` | hook=2、失败保 round、选择关闭、yellow pathing FIRST_AID；Cloud phase | `MIGRATE`；未写 |
| 19 | `ui/MainWindowController.java` | 修罗/五倍 0=无限、无正数上限、显示“无限”；本地 UI | `MIGRATE`，但跨端 count transport 被 PCB-01 阻断 |
| 20 | `vision/GameTextLineOcrService.java` | 黄点 profile/mask/direct anchor 候选识别 | `MIGRATE` 到既有 Cloud `SmartClickRecognizer`；禁止整类复制；未写 |
| 21 | `window/execution/WindowTaskRunner.java` | tracker scan/click、prepared freshness、事实发布；本地 Runner | `MIGRATE`；未写 |
| 22 | `window/runtime/WindowRuntimeContext.java` | per-window tracker anchor memory；本地唯一窗口 store | `MIGRATE`；未写 |
| 23 | `window/model/WindowTrackerAnchorMemory.java`（untracked） | window-relative anchor value；本地 window cache | `MIGRATE` 候选；未创建 |

说明：`OcrWindowScanService.java` 在 `git status` 显示工作树标记，但相对 `696a12b0` 无 content diff，不计入
22+1 production ledger；`application.properties` 是独立 resource 差异，不计入 23 Java。

### 10 个行为簇停点证据

| 簇 | Wave 0 结论 |
|---|---|
| 1 输入 pause/stop | 基线路径已定位到 queue/request/scope/worker/token；因 PCB-01 immediate-stop，未实施/未验收 |
| 2 无限次数 | **阻断已证实**：UI -> 本地 `BotProperties` -> `WindowTaskControlService` -> 三字段 start request -> Cloud runtime/factory -> Cloud 全局 `BotProperties`；exact count 丢失 |
| 3 Tracker | 本地 cache/mechanics 与 Cloud reader 唯一属主方向已分类；未实施/未验收 |
| 4 NPC | 本地 direct/Ctrl 原子动作与 Cloud profile/FIFO 唯一属主方向已分类；未实施/未验收 |
| 5 maintenance/dialog/supply | 固定 ROI/0.85/150ms/800ms 与 click-only mouse-away 已登记；未实施/未验收 |
| 6 Summon | 四态/IF8/0.80/色距12/2500ms 已登记；未实施/未验收 |
| 7 FiveRing | Runner-local fact 与 Cloud phase 边界已登记；未实施/未验收 |
| 8 Xiuluo | hook=2/保 round/选择关闭/yellow pathing 已登记；未实施/未验收 |
| 9 Auto-combat | 当前为 `ALREADY_EQUIVALENT` 候选，尚未完成 method/SHA 证明，不重写 |
| 10 data/assets | 已完成只读清单分类；未复制、未覆盖、未激活 |

### 资产与 data 分类

- `DATA_ONLY`：tracked `config/dialog_choice_memory.json`、`config/vision_memory.json`、
  `config/world_map_route_result_memory.json`；untracked `data/npc-click-memory.json`、`data/route-memory.json`、
  `data/vision_memory.json`。本轮零复制、零覆盖、零 import。
- `PRODUCTION_CANDIDATE`（须由真实 consumer/SHA 门复核后才可打包）：`battle/auto_remaining.png`、
  `task/wuhuan/panel_title_yellow.png`、`xiuluo/xiuluo_tracker_title.png`、maintenance 三张 raw 模板、
  `zhaohuanshou/if8.png` 与三张 slot-status 模板、五倍/修罗新增 dialog 标题模板及 tracked tracker 标题资产。
- `EVIDENCE_ONLY/UNRELATED_FUTURE`：`Snipaste_*`、`images/template/300huan/**`、`guzhu/**`、`xinshou/**`、
  `zhoumo/**`、本地 replay/debug 输出和未被本卡 production caller 证明引用的图片。本轮零激活。

### 写入、SHA 与构建证据

- 实际 production/resource 改动：`0`；因此 production before/after byte identity 不变。
- 实际 Cloud 改动：`0`；`D:\mavenProject\dhxy-cloud-brain` 保持只读。
- 实际唯一业务基线改动：`0`；`D:\mavenProject\DHXY` 保持 branch/HEAD/status count
  `codex/baseline-696a12b0` / `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` / `128`。
- 实际 DHXY-cr271 写入仅为允许文档/dashboard：本卡、`ACTIVE_WORK.md`、`PACKAGE_ARCHITECTURE.md`、
  两份权威计划、两份矩阵、`cr-dashboard-data.js`；dashboard generator 成功：`263 CR rows`。
- 最终观测文档 SHA256（卡片自身最终 SHA 由父级在交付后外部取值，避免自引用）：
  `ACTIVE_WORK=5B621935...`、`PACKAGE_ARCHITECTURE=F8A75F18...`、post-696 plan=`541C8E08...`、
  authority plan=`9D9688DF...`、service matrix=`E1C60C88...`、acceptance matrix=`5770C1BD...`、
  dashboard=`BDD6F6EE...`。`ACTIVE_WORK`、`PACKAGE_ARCHITECTURE`、authority plan 在首次同步后发生并发
  SHA 变化；本 Worker 未回滚/覆盖并发内容，也不把共享 dirty 文件的全部 bytes 归因于自己。所有这些文档在
  claim 前已有 dirty/untracked，未把 Git HEAD 冒充 session-before SHA。
- 卡外零写入证明：client status count claim 前后均为 `83`；所有本 Worker 新增差异均落在卡片 4.4 文档写集。
- 编译：**未运行**。原因是卡片要求遇到计划合同缺口立即停止，且 production Java writer 从未开始；不得用
  compile 伪装行为簇已交付。tests/debug/replay/source guards 也均未创建、恢复或运行。

## PLAN-CONTRACT BLOCKED

最终阻断仍为 `TURN-40E-PCB-01 / REMOTE TASK RUN COUNT NOT TRANSPORTED`。父级修订并冻结完整写集前，
不得恢复 Java writer；不得写 Approved；TURN-41 保持 BLOCKED。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED PCB-01 SOURCE-ZERO COMPILE-NOT-RUN 2026-07-20T00:13:38-04:00 -->

## WORKER ACK / PARENT PLAN-CONTRACT REPAIR #1

- Agent id: `Codex-TURN-40E-implementation-worker-20260720`
- ACK: 已完整回读并接受 `PARENT PLAN-CONTRACT REPAIR #1 / OWNER RETAINED`。
- 冻结实现：复用现有 HTTPS `TurnTaskStartRequest`，新增与 `taskCodes` index 一一对应的
  `List<Integer> taskMaxRuns`；真实 start 对 null/size/index/value 全部 fail-closed。
- 本地投影：`WindowTaskControlService` 按 ordered queue 从本地 `BotProperties` 冻结
  `WUBEI=fivefold`、`XIULUO_V2=xiuluo`、`WUHUAN_V2=wuhuan`、`AUTO_BATTLE=1`。
- Cloud 传递：`CloudTurnTaskRuntime` 把当前 index count 写入 immutable `CloudTaskServiceMetadata`，
  `TaskExecutionContext` 只读暴露；turn-native Wubei/Xiuluo/Wuhuan 读取 context，非-turn rollback 可保留旧配置。
- 明确禁止：不改 `CloudTurnTaskFactory`，不建第二 protocol/store，不修改 Cloud 全局 `BotProperties`，不默认 1，
  不创建/运行 tests/debug/replay/source guards。
- Owner：原 TURN-40E implementation owner 保留；PCB-01 计划阻断按父级 Repair #1 解除，继续整卡 source 实施。

<!-- TRUE_EOF: TURN-40E WORKER-ACK PARENT-PLAN-CONTRACT-REPAIR-1 OWNER-RETAINED 2026-07-20 -->

## TURN-40E PARTIAL SOURCE LEDGER / PCB-02

### Repair #1 已闭合的调用链

- Producer：`MainWindowController` 保留修罗/五倍 `0=无限`、任意非负正数及五环独立 1/2 规则 ->
  `WindowTaskControlService.toTaskMaxRuns` 按 ordered taskCodes 逐 index 冻结 -> 同一
  `TurnTaskStartRequest.taskMaxRuns`。
- Transport/runtime：双仓 validator 对 null/size/index/value fail-closed -> `CloudTurnTaskRuntime` 按 index 将值
  写入该 queue element 的 immutable `CloudTaskServiceMetadata` -> `TaskExecutionContext.getConfiguredMaxRuns()`
  对 turn-native 缐失值 fail-closed。
- Consumer：Cloud Wubei/Xiuluo/Wuhuan 从 exact context 读取；只有 legacy non-turn metadata 的 null 值回退
  Cloud `BotProperties`。`CloudTurnTaskFactory` 未修改；重复 task code 仍由 queue index 分别取各自 count。
- 双仓 `TurnTaskStartRequest` SHA256 均为
  `D16526892E94A4A94207FAE22B62EEEF0992ABFF0EF49D60262DD2559F090035`；双仓 validator 均为
  `09EB8738AB5A80D2F27A5E982C187C0C3A77FA866CC0065AC38452F59658CF10`，byte-identical。

### 23 路径 ledger（本次 immediate-stop 时点）

| # | 路径/业务语义 | 当前分类与证据 |
|---|---|---|
| 1 | `InputActionQueue` pause 补偿 waiter | `ALREADY_EQUIVALENT`：现有 `CapturedTaskTokens`、120s remaining budget、pause compensation 均存在，未重写 |
| 2 | `InputActionRequest` pause/stop 同请求 | `ALREADY_EQUIVALENT`：同时持有 pause/stop token，pause 不再等同 cancel |
| 3 | `InputActionScope` exclusive checkpoint | `ALREADY_EQUIVALENT`：`checkpoint/isCancelled` 在同一 callback 等待 resume，stop 仍取消 |
| 4 | `InputActionWorker` focus/action checkpoint | `ALREADY_EQUIVALENT`：`before-focus/before-actions/before-exclusive/action-N` 均调用 `waitIfPaused` |
| 5 | `TaskPauseToken` pause duration | `ALREADY_EQUIVALENT`：`requestedAtMs` 与 revision wait 已存在 |
| 6 | `AutoCombatPanelService` | PCB-02 immediate-stop 前未完成 method proof；不重写 |
| 7 | `BagService` | 输入簇证据只确认使用现有 pause-aware queue；其余未审完 |
| 8 | `DialogService` | 未审完；不重写 |
| 9 | `NpcClickService` | 未审完；不重写 |
| 10 | `PlayerStateService` | 未审完；不重写 |
| 11 | `QuestManagerService` | 输入簇证据只确认使用现有 pause-aware queue；其余未审完 |
| 12 | `SummonSkillService` | 未审完；不重写 |
| 13 | `TaskMaintenanceService` | 未审完；不重写 |
| 14 | `TaskTrackerPanelService` | `PLAN-CONTRACT BLOCKED`：Cloud 仍 full-window observe + Cloud `pendingRepositions`; local capture mechanics 零 caller |
| 15 | `UICleanerService` | 输入簇证据只确认使用现有 pause-aware queue；其余未审完 |
| 16 | `DialogHandleRequest` | 未审完；不重写 |
| 17 | `FiveRingTaskV2` | 仅 Repair #1 exact count 已改；LD-07 未审完 |
| 18 | `XiuluoTaskV2` | 仅 Repair #1 exact count 已改；LD-08 未审完 |
| 19 | `MainWindowController` | `MIGRATED`：0 不再归一成 1、不封顶 100、显示/解析“无限”、step 最低 0；五环仍 1/2 |
| 20 | `GameTextLineOcrService` | Cloud 唯一算法映射未审完；不复制旧类 |
| 21 | `WindowTaskRunner` | LD-07 未审完；不重写 |
| 22 | `WindowRuntimeContext` | `BLOCKED`：卡要求唯一 anchor cache，但现有 turn-native tracker 无法调用本地 cache mechanics |
| 23 | `WindowTrackerAnchorMemory` | `BLOCKED`：只读基线 untracked model 已读；未创建无 caller 的 dead production 类型 |

附加 `LD-02 transport closure`：`MIGRATED`；request/index validator/local producer/Cloud immutable
metadata+context/runtime/Wubei/Xiuluo/Wuhuan 已闭合并 compile。

### 10 行为簇停点

1. 输入 pause/stop：现有 CR 已等价，调用边界证据见 ledger 1-5，零重写。
2. 无限次数：Repair #1 + UI 已实现；同协议、index 对齐、turn-native context 唯一真相。
3. Tracker：**PCB-02 阻断**；未达到 local cache/cached ROI/masked fallback 唯一属主合同。
4. NPC：因 immediate-stop 未完成审计/实施。
5. Maintenance/dialog/supply：因 immediate-stop 未完成审计/实施。
6. Summon：因 immediate-stop 未完成审计/实施。
7. FiveRing Runner/Task：仅次数传递完成，其余因 immediate-stop 未完成。
8. Xiuluo：仅次数传递完成，其余因 immediate-stop 未完成。
9. Auto-combat：未完成 `ALREADY_EQUIVALENT` method/SHA proof，不重写。
10. data/assets：runtime JSON、production candidate、evidence 图片均零复制、零覆盖、零激活。

### 实际 production 修改与 SHA256

DHXY-cr271（before -> after）：

- `TurnTaskStartRequest.java`：`4E318AEEEE0838DFC0232416065E356250A1EDE5B8C3A2A6EFFA22C14DCF53CA` -> `D1652689...`
- `TurnProtocolValidator.java`：`56383C98D92713EF8C0CDD6F0D2DC560A0A4D7E6C9D09035B3477E086F0D978C` -> `09EB8738...`
- `WindowTaskControlService.java`：`A6AFC3CB8C4BB2E7CF8BB04AB0DB1FC3F820D3ECCEB702DB88217D927B2E93F5` -> `39D47844...`
- `MainWindowController.java`：session-before 已捕获前缀 `5A71F38D...` -> `FFAE0D7F...`。

Cloud（before -> after）：

- request `D4AF7B55... -> D1652689...`；validator `56383C98... -> 09EB8738...`；
  `TaskExecutionContext` `830E42C5... -> 2605A017...`；`CloudTaskServiceMetadata` `A3FE6615... -> A967BBF6...`；
  `CloudTurnTaskRuntime` `8368ED7E... -> 1E3E5A35...`；Wubei `018F2348... -> 19EAB860...`；
  Xiuluo `7691F295... -> EFCD85DB...`；Wuhuan `67AF905C... -> 6935AAD7...`。

实际 production 写入仅上述 4+8 个卡内路径。未创建/修改 production resource；未修改
`CloudTurnTaskFactory`；未创建 test/debug/replay/source guard；未运行 tests/runtime/application/server/
Task/UI/capture/input。文档仅写 4.4 列出的卡、ACTIVE_WORK、PACKAGE_ARCHITECTURE、两计划、两矩阵和
dashboard；generator 成功 `263 CR rows`。

### Compile 与三仓只读/dirty 证据

- DHXY-cr271：`mvn -q -DskipTests compile` 成功（协议换行 byte contract 修正后复跑仍成功）。
- Cloud：卡指定 skip 命令被 Maven Enforcer `require-tests-enabled` 拒绝；遵守本轮禁测，改用不会进入 test
  phase 的 `mvn -q compile`，成功。未绕过 Enforcer，未运行 tests。
- 唯一业务基线 branch/HEAD 始终为 `codex/baseline-696a12b0` /
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；本 Worker 未对其执行任何写入/build/format/git mutation。
- status count 在并发工作期间由 claim 的 baseline/client/cloud `128/83/588` 外部变为观测值 `93/85/32`；
  branch/HEAD 不变。本 Worker 未 revert/reset/clean/checkout，未把他人 dirty/untracked 归因于本卡。

## PLAN-CONTRACT BLOCKED

- Blocker id：`TURN-40E-PCB-02 / TRACKER LOCAL CACHE MECHANICS HAS NO TURN-NATIVE CALLER`。
- 当前真实调用链：Cloud `FiveRingTaskV2/WubeiTask/XiuluoTaskV2` -> Cloud
  `TaskTrackerPanelService.observe` -> `capture` -> bound `TurnGameClient.capture/execute` ->
  `CloudTurnActionFactory` -> `CloudTurnCommandPort` -> HTTPS `TurnAction` -> DHXY
  `LocalTurnActionExecutor` generic capture/input -> raw full-window PNG -> Cloud tracker algorithm；anchor unsafe
  状态进入 Cloud `pendingRepositions` map。该链没有读取/写入 `WindowRuntimeContext` anchor cache。
- 断链证据：DHXY `TaskTrackerPanelCaptureLocalMechanics.capturePanel` production caller 数为 0；现有
  `RemoteGameOperation.TASK_TRACKER_PANEL_RECT -> LocalRemoteGameCommandHandler ->
  TaskTrackerPanelRectLocalObservationMechanics` 是 dormant 旧 remote WindowFact 路径，turn-native context 无
  old remote authority。复活它会违反“禁止第二协议”。
- 必须由父级先冻结的最小表示选择：
  1. 若复用同一 turn local-service step：双仓 `TurnLocalOperation`、`TurnLocalServiceCall`、强类型 tracker
     arguments/result schema、`TurnProtocolValidator`；DHXY `LocalServiceStepDispatcher` + tracker local executor；
     Cloud `TaskTrackerPanelService` + 一个同一 turn 的 tracker local client。
  2. 若采用专用同一 turn step：双仓 `TurnStepType/TurnStep/TurnStepResult` 及 validator/action factory；DHXY
     `LocalTurnActionExecutor` 分发；Cloud `TurnGameClient/TaskTrackerPanelService` 消费。
  两者只能选其一，均须把 `TaskTrackerPanelCaptureLocalMechanics`、`WindowRuntimeContext`、
  `WindowTrackerAnchorMemory` 纳入真实 caller，并冻结 cached ROI miss -> masked full-window fallback -> 必要 drag
  -> post-drag panel capture 的原子顺序；Cloud 只保留 title/green segmentation/ranking，删除 Cloud anchor cache。
- 上述 `TurnLocalOperation/TurnLocalServiceCall/LocalServiceStepDispatcher` 或
  `TurnStepType/TurnStep/TurnStepResult/LocalTurnActionExecutor/TurnGameClient` 均不在当前 4.2/4.3 写集。
  Worker 不能安全裁决两种表示，也不能写 dead class、恒 null 或继续保留 Cloud second cache，因此立即停止。
- 后续 LD-04..LD-10 未审完，TURN-41 继续 BLOCKED；不得写 Approved。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED PCB-02 TRACKER-LOCAL-CACHE-TURN-CLOSURE 2026-07-20 -->

## PARENT PLAN-CONTRACT REPAIR #2 / TRACKER SAME-TURN LOCAL_SERVICE CLOSURE

- 裁决：只复用现有 HTTPS turn `LOCAL_SERVICE` step；禁止新增专用 tracker step、复活 dormant
  `RemoteGameOperation.TASK_TRACKER_PANEL_RECT`、保留 Cloud `pendingRepositions` 或形成第二协议/store/cache。
- 唯一属主：DHXY exact-window local mechanics 独占 HWND capture、anchor cache、ROI/full-window anchor matching、
  必要 drag 与 post-drag panel capture；Cloud `TaskTrackerPanelService` 独占 title template、green segmentation、
  fingerprint/candidate ranking/classification/result construction。双方不得复制对方算法。
- 本地基线精确顺序以当前只读 `D:\mavenProject\DHXY` 为准：读取当前
  `WindowRuntimeContext.taskTrackerAnchorMemory`；有缓存时只在 cached anchor 的
  `left=-100/top=-75/right=+100/bottom=+75` ROI 搜索；miss 必须清 cache；随后对 exact bound window 做
  `OcrWindowScanService.copyWithDefaultMasks` 后的 full-window anchor search；anchor 不在以
  `(baseX+119,baseY+221)` 为中心的同一 ROI 时，在一个已持有的 input exclusive callback 内执行一次
  `dragAndDrop -> sleep(500)`；记录 window-relative anchor；若 full-window anchor 原先不在默认 ROI，拖后再以
  cached ROI 单次确认；最后按 anchor offsets `[-112,+12,+102,+350]` 捕获 panel。不得增加 retry/TTL/额外验证。
- 唯一 wire 表示：新增 `TurnLocalOperation.TASK_TRACKER_CAPTURE_PANEL`；`TurnLocalServiceCall` 增加唯一
  `taskTracker` argument slot，双仓 request/validator 必须 byte-identical 且严格 one-of；同一 local-service
  result 用 nonblank JSON 携带 closed state/origin/dimensions/SHA，只有 `CAPTURED` 可附一张
  `TurnFramePurpose.TASK_TRACKER_PANEL` PNG。Cloud 必须逐项校验 operation、status/code、JSON shape、frame
  purpose/sourceStepIndex/region/dimensions/SHA/raw PNG；非 CAPTURED 不得附 frame。
- DHXY dispatcher 对该 operation 获取当前 exact `WindowRuntimeContext`/native binding，并以现有
  `InputSequences.submitExclusiveAndWait` 包住整个 mechanics call；mechanics 内只能直接调用
  `InputProvider`，禁止 queue-in-queue。即使本次无需 drag，也保持同一闭包以消除检查后插队。
- Cloud `TaskTrackerPanelService.observe` 通过新的 same-turn tracker local client 获取最终 panel bytes+origin，
  删除 generic full-window capture/reposition 与 Cloud anchor cache；后续 title/green 识别流程保持当前 Cloud
  唯一算法，不改变返回、fallback、排序、次数或 task phase 语义。

### Repair #2 追加 production 写集

双仓同字节协议：

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalServiceCall.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskTrackerOperationArguments.java`（new）
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskTrackerOperationResult.java`（new）
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnFramePurpose.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`

DHXY-cr271：

- `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceExecution.java`
- `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`
- `src/main/java/com/bot/dhxy/cloud/turn/local/TaskTrackerLocalOperationExecutor.java`（new）
- `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowTrackerAnchorMemory.java`（new）

Cloud：

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudTaskTrackerLocalServiceClient.java`（new）
- `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`

只有现有 local-service frame assembler/client 的实际编译错误证明上述列表仍缺必需闭包时，Worker 才可再次
`PLAN-CONTRACT BLOCKED` 并给出 exact symbol/caller；不得自行扩大到 `TurnStepType`/dedicated step 或测试。
Repair #1 production 保持冻结；LD-04..LD-10 继续按原整卡完成。无已批准业务差异；按当前本地 workspace
逻辑等价迁移。Owner retained，恢复 `SOURCE ACTIVE / REPAIR #2`。

<!-- TRUE_EOF: TURN-40E PARENT PLAN-CONTRACT REPAIR-2 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## STATUS / ACK — PARENT PLAN-CONTRACT REPAIR #2

- Agent id：`Codex-TURN-40E-implementation-worker-20260720`；canonical owner retained。
- Status：`SOURCE ACTIVE / REPAIR #2 ACKNOWLEDGED`；PCB-02 按父级 same-turn `LOCAL_SERVICE` 裁决解除。
- ACK：只新增 `TASK_TRACKER_CAPTURE_PANEL` local operation 与唯一 `taskTracker` argument/result slot；双仓协议
  byte-identical、strict one-of。DHXY 独占 exact-window cache/ROI/masked full fallback/drag/post-drag panel
  capture，Cloud 独占 title/green/fingerprint/ranking/classification。
- 原子边界：DHXY dispatcher 用现有 `InputSequences.submitExclusiveAndWait` 包住完整 mechanics；mechanics 内
  只直接调用 `InputProvider`，无 queue-in-queue；无 drag 时仍保持同一 exclusive closure。
- 明确禁止：不新增 dedicated step，不复活 dormant remote fact，不保留 Cloud `pendingRepositions`，不建立第二
  protocol/store/cache，不写/build `D:\mavenProject\DHXY`，不运行 tests/runtime/application/server/Task/UI/
  capture/input，不 revert/reset/clean/checkout 或覆盖他人 dirty/untracked。
- 执行：按 Repair #2 追加写集闭合 LD-03 后连续审计/实施 LD-04..LD-10；仅遇到新的精确写集或语义缺口才
  canonical `PLAN-CONTRACT BLOCKED`。

## PLAN-CONTRACT BLOCKED

- Blocker id：`TURN-40E-PCB-03 / DIRECT-COMBAT NPC PROFILE MASK FLAG CANNOT REACH SOLE CLOUD MASK OWNER`。
- Canonical owner：`Codex-TURN-40E-implementation-worker-20260720` 保留；Repair #2 ACK 后 LD-03 source 已闭合，
  本阻断在 LD-04 当前本地 workspace 等价审计中首次出现。
- 当前基线业务条件：只读 `D:\mavenProject\DHXY` 的
  `NpcClickService.clickNpcByYellowTargetName(..., skipDefaultOcrMask)` 调用
  `GameTextLineOcrService.findYellowTarget(..., !skipDefaultOcrMask)`；普通路径传 `true`，Alt+A direct-combat
  路径传 `false`。后者仍使用目标名 profile 的颜色阈值，但不得应用灵兽使者 ignore regions 或降魔守卫
  allowed regions，保证窗口边缘目标仍进入 direct candidate -> Ctrl/FIFO 的同一 Alt+A session。
- Cloud 当前完整调用链：
  `NpcClickService.clickNpcByYellowTargetName` -> package-private
  `NpcYellowTargetRecognizer.findYellowTarget(BufferedImage,String)` ->
  `SmartClickRecognizer.findYellowTarget` -> `collectYellowTargetCandidates` ->
  `buildLegacyYellowTargetMask` -> `ImageAlgorithms.npcYellowTargetMask(BufferedImage,String)`。
  最末方法是现有且唯一的 profile/mask 算法属主，但没有 `allowProfileRegionMasks` 参数，并无条件执行
  `isNpcYellowTargetIgnored` / `clearNpcYellowTargetIgnoredRegions` /
  `clearOutsideNpcYellowTargetAllowedRegions`。
- 精确缺口：获准的 `NpcClickService.java` 与 `SmartClickRecognizer.java` 可以传递该 boolean，但
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/ImageAlgorithms.java` 不在本卡 4.3 冻结写集。把 profile、阈值、
  connected-component、dilate/dark-outline、region mask 复制到 `SmartClickRecognizer` 会形成第二 NPC
  template/candidate mask 算法，违反本卡“Cloud 唯一识别属主、禁止第二算法/整类复制”。保持现状又会在
  direct-combat full-window frame 上错误屏蔽基线允许的边缘候选，不能宣称行为等价。
- 最小必要追加写集：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\ImageAlgorithms.java`。
  必要修改仅为给现有唯一 `npcYellowTargetMask` 增加/重载 `allowProfileRegionMasks`，并只在该值为 `true`
  时执行上述 region masks；颜色 profile、候选阈值、组件过滤和排序保持原实现。随后由已冻结写集内的
  `SmartClickRecognizer` 与 `NpcClickService` 原样传递 boolean，无新 protocol/store/cache。
- Immediate stop：未修改 `ImageAlgorithms.java`，未复制算法，未开始 LD-04 production 写入；LD-05..LD-10
  按合同未继续。未运行 tests/debug/replay/source guards、Maven、application/server/Task/UI/capture/input；
  `D:\mavenProject\DHXY` 始终只读且未构建。
- Repair #2 partial source 保留：同一 `LOCAL_SERVICE` tracker operation、client exact-window cache/ROI/masked
  fallback/drag/post-drag panel capture 与 Cloud title/green/ranking 接线已写入获准路径；writer 尚未进入稳定
  交付态，双端 compile 未运行，不能写 delivered/Approved。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED PCB-03 NPC-PROFILE-MASK-FLAG-CLOUD-OWNER 2026-07-20 -->

<!-- TRUE_EOF: TURN-40E WORKER STATUS-ACK REPAIR-2 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## PLAN-CONTRACT BLOCKED — CANONICAL EOF

- Blocker：`TURN-40E-PCB-03 / DIRECT-COMBAT NPC PROFILE MASK FLAG CANNOT REACH SOLE CLOUD MASK OWNER`。
- Owner retained：`Codex-TURN-40E-implementation-worker-20260720`。
- Exact chain：Cloud `NpcClickService.clickNpcByYellowTargetName(skipDefaultOcrMask)` ->
  `NpcYellowTargetRecognizer.findYellowTarget` -> `SmartClickRecognizer.findYellowTarget` ->
  `collectYellowTargetCandidates` -> `buildLegacyYellowTargetMask` -> sole mask owner
  `ImageAlgorithms.npcYellowTargetMask(BufferedImage,String)`；现有末端始终应用 profile region masks，无法传递
  基线 `allowProfileRegionMasks=!skipDefaultOcrMask`。
- Required write set：追加
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\ImageAlgorithms.java`，仅给现有
  `npcYellowTargetMask` 增加/重载 boolean 并条件执行 region masks。否则复制到获准的
  `SmartClickRecognizer` 会制造第二 mask/candidate 算法；保持现状则 Alt+A full-window 边缘候选不等价。
- Stop state：未写该卡外 production 路径，LD-04 production 与 LD-05..LD-10 均未继续；未运行 Maven/tests/
  runtime/application/server/Task/UI/capture/input；`D:\mavenProject\DHXY` 只读。完整证据及 Repair #2 partial
  source 状态见紧邻上一段 PCB-03 记录。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED PCB-03 NPC-PROFILE-MASK-FLAG-CLOUD-OWNER CANONICAL 2026-07-20 -->

## PARENT PLAN-CONTRACT REPAIR #3 / DIRECT-COMBAT PROFILE MASK FLAG

- 父级裁决：批准最小追加 Cloud production 写集
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/ImageAlgorithms.java`。此扩写无业务选择，只把当前本地
  workspace 已有 `allowProfileRegionMasks` 条件送达 Cloud 现有唯一 yellow-target mask 属主。
- `ImageAlgorithms.npcYellowTargetMask(raw,targetName)` 必须保留兼容并固定委托新三参数 overload 的
  `allowProfileRegionMasks=true`；新 overload 只在 true 时执行 profile ignore/allowed-region 清理。颜色 profile、
  threshold、dilate/dark-outline、connected-component filter、候选 score/order 全部不变。
- 已冻结写集内 `SmartClickRecognizer.findYellowTarget` 增加对应 boolean overload，旧两参数入口固定委托 true；
  `NpcClickService` normal path 传 true，且仅当前本地基线 Alt+A direct-combat pipeline 传 false。不得根据任务名、
  NPC 名、frame 尺寸或 OCR 结果重新推断该值，不得增加 protocol/store/cache/retry。
- 继续禁止把 mask/profile 算法复制进 `SmartClickRecognizer` 或 `NpcClickService`。LD-04 完成后连续推进
  LD-05..LD-10；Repair #2 tracker source 保留。Owner retained，恢复 `SOURCE ACTIVE / REPAIR #3`。

<!-- TRUE_EOF: TURN-40E PARENT PLAN-CONTRACT REPAIR-3 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## STATUS / ACK — PARENT PLAN-CONTRACT REPAIR #3

- Agent id：`Codex-TURN-40E-implementation-worker-20260720`；canonical owner retained。
- Status：`SOURCE ACTIVE / REPAIR #3 ACKNOWLEDGED`；PCB-03 按父级最小 Cloud 写集裁决解除。
- ACK：`ImageAlgorithms.npcYellowTargetMask(raw,targetName)` 固定委托三参数 `true`；三参数只以显式
  `allowProfileRegionMasks` 条件化现有 ignore/allowed-region masks，颜色 profile、threshold、dilate/
  dark-outline、component filter 全部零变化。
- 传递：`SmartClickRecognizer` 两参数固定 `true`、boolean overload 原样下传；`NpcClickService` normal
  path 传 `true`，仅 Alt+A direct-combat 传 `false`。不从 task/NPC/frame/OCR 推断，不增加 retry/store/
  protocol，不复制算法。
- 执行：保留 Repair #2 tracker source；闭合 LD-04 后连续实施 LD-05..LD-10，只有新的 exact blocker 才停。
  继续禁止写/build `D:\mavenProject\DHXY`、tests/runtime/application/server/Task/UI/capture/input 和任何 Git
  mutation。

<!-- TRUE_EOF: TURN-40E WORKER STATUS-ACK REPAIR-3 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## PLAN-CONTRACT BLOCKED — CANONICAL EOF

- Blocker id：`TURN-40E-PCB-04 / FIRST-AID CLICK-ONLY PORT CANNOT APPEND SAME-COMMAND MOUSE-AWAY`。
- Canonical owner：`Codex-TURN-40E-implementation-worker-20260720` 保留；Repair #3 已按裁决完成 LD-04
  profile-mask flag source 传递和 Alt+A purple-anchor `y-50` direct candidate 接线，Repair #2 tracker source 保留。
- 当前基线条件：只读 `D:\mavenProject\DHXY` 的 `PlayerStateService.checkAndHealFromSnapshot` 只在真实
  HP/MP 右键补给分支中，把 `CLICK_RIGHT -> WAIT(800) -> MOVE_MOUSE(safePoint) -> WAIT(300)` 放进同一
  `InputSequences.submitAndWait`；healthy/disabled/no-target 分支不移动。safe point 是窗口内随机点，排除
  `relX>=761 && relY<=147`。
- Cloud 当前完整调用链：`PlayerStateService.healTargets` / `executePendingNoFocusFirstAidPlan` ->
  `CloudPlayerStateFirstAidPort.executeTargets(List<SupplyTarget>)`。后者是实际且唯一 turn mechanics assembler，
  当前只为每个真实 target 生成 `CLICK_RIGHT -> WAIT(800)`，然后立即提交 command；获准的 Cloud
  `PlayerStateService.java` 只能传业务 target，无法在同一 command 尾部追加 mouse-away。
- 精确写集缺口：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\playerstate\CloudPlayerStateFirstAidPort.java`
  不在本卡 4.3 冻结写集。通过伪造 `SupplyTarget`、名称哨兵或第二次 turn 调用规避，会分别形成隐式第二协议
  或破坏输入原子性，均不允许。
- 最小必要追加写集：上述 `CloudPlayerStateFirstAidPort.java`。必要修改仅在 `ordered` 非空时，按当前基线
  safe-point 约束在既有 `executeTargets` 同一 `steps` 尾部追加一个 `MOVE_MOUSE` 和 `WAIT(300)`；目标顺序、
  每次右键后的 800ms、提交次数、validator、失败/STOP/uncertain 语义均不变，不新增 retry/store/protocol。
- LD-05 已完成的获准 source：Cloud maintenance fixed raw ROI 改为 `(260,373)-(378,413)`、raw templates、
  threshold `0.85`、单一 `MOVE -> WAIT(150) -> CLICK(delay=800)`，并删除 fixed-template miss 后的 broad
  dialog business-option fallback。上述修改保留，未触碰卡外 production。
- Immediate stop：未修改该未授权 port；LD-05 supply 未闭合，LD-06..LD-10 未继续。未运行 Maven/tests/
  debug/replay/source guards/runtime/application/server/Task/UI/capture/input；`D:\mavenProject\DHXY` 始终只读，
  未做任何 Git mutation。当前 writer 未稳定，不能写 delivered/Approved。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED PCB-04 FIRST-AID-SAME-COMMAND-MOUSE-AWAY 2026-07-20 -->

## PARENT PLAN-CONTRACT REPAIR #4 / FIRST-AID POST-SUPPLY MOUSE-AWAY

- 父级批准最小追加 Cloud production 写集
  `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateFirstAidPort.java`。完整传递审计同时确认该 port
  仍带旧的 capture-before pointer clear；当前本地 workspace 已删除此行为，因此本次必须在同一文件一起闭合，
  禁止只补第一个缺失 step。
- `captureBars()` 的唯一 CAPTURE step 不再附 `ClearPointerIfOverRegion`，不得在 healthy/disabled/no-target 或
  单纯截图路径移动鼠标；删除只为 pre-capture clear 服务的 padding/helper，不改变 bars ROI、raw PNG、单 capture、
  correlation/STOP/failure 语义。
- `executeTargets` 仅当 ordered targets 非空时，在同一 existing command 内保持每个 target
  `CLICK_RIGHT -> WAIT(800)` 原序，全部 targets 后恰好追加一次
  `MOVE_MOUSE(safePoint) -> WAIT(300)`。safePoint 为当前 exact window 内随机 window-relative 点，沿用本地
  `1024x768` 范围且排除 `relX>=761 && relY<=147`；必须 validate inside exact window。不得在每个 target 后移动、
  不得第二次 submit、不得复用 capture pointer-clear 固定点、不得新增 retry/store/protocol。
- 现有 submitted-step correlation/failure-index 校验自然覆盖新增尾部 steps；提交次数仍恰好一次。Repair #2/#3
  与 LD-05 已完成 source 保留；闭合 supply 后连续推进 LD-06..LD-10。Owner retained，恢复
  `SOURCE ACTIVE / REPAIR #4`。

<!-- TRUE_EOF: TURN-40E PARENT PLAN-CONTRACT REPAIR-4 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## STATUS / ACK — PARENT PLAN-CONTRACT REPAIR #4

- Agent id：`Codex-TURN-40E-implementation-worker-20260720`；canonical owner retained。
- Status：`SOURCE ACTIVE / REPAIR #4 ACKNOWLEDGED`；PCB-04 按父级完整 first-aid mechanics 写集裁决解除。
- ACK capture：`captureBars()` 删除 `ClearPointerIfOverRegion` 及其专用 padding/helper，保持 exact bars ROI、
  single raw-PNG capture 与既有 terminal/correlation 语义；纯 capture/healthy/disabled/no-target 零鼠标动作。
- ACK supply：仅 ordered 非空时，在同一 existing command 中保持每 target `CLICK_RIGHT -> WAIT(800)`，
  全部 target 后恰好一次随机 safe point `MOVE_MOUSE -> WAIT(300)`；relative 范围 `1024x768`，排除
  `relX>=761 && relY<=147`，并校验 exact window 内。无第二 submit/fixed point/retry/store/protocol。
- 执行：保留 Repairs #2/#3 与 LD-05 source；闭合 supply 后连续 LD-06..LD-10，仅遇新 exact blocker 停止。
  继续禁止写/build 基线、tests/runtime/application/server/Task/UI/capture/input 与 Git mutation。

<!-- TRUE_EOF: TURN-40E WORKER STATUS-ACK REPAIR-4 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## PLAN-CONTRACT BLOCKED — CANONICAL EOF

- Blocker id：`TURN-40E-PCB-05 / REPAIR-2 REQUIRED OCR MASK OWNER ABSENT FROM THIN CLIENT`。
- Canonical owner：`Codex-TURN-40E-implementation-worker-20260720` 保留。Repairs #2/#3/#4、LD-04、LD-05、
  LD-06、LD-08、LD-09 与 LD-10 production source 保留；LD-07 已等价路径只补正 stale/Runner 日志证据。
- Repair #2 冻结调用链要求：DHXY `TaskTrackerPanelCaptureLocalMechanics.findMaskedFullWindowAnchor` 必须调用
  现有 `OcrWindowScanService.copyWithDefaultMasks(raw)`，再执行唯一 full-window anchor search；不得复制 mask
  或形成第二 OCR/候选算法。
- 实际 compile 证据：writer 稳定后在 `D:\mavenProject\DHXY-cr271` 运行唯一获准命令
  `mvn -q -DskipTests compile`，失败于
  `TaskTrackerPanelCaptureLocalMechanics.java:[6,27]`：找不到
  `com.bot.dhxy.vision.OcrWindowScanService`。只读 `rg --files src/main/java` 与 symbol search 同样证明
  DHXY-cr271 production tree 无该类；只有本 Worker 新 mechanics 的 import/call。此为 Repair #2 指定可再次
  阻断的真实编译闭包错误，不是推测。
- 唯一业务基线证据：只读 `D:\mavenProject\DHXY` 存在
  `src/main/java/com/bot/dhxy/vision/OcrWindowScanService.java` 及其 canonical
  `copyWithDefaultMasks(BufferedImage)`；原 Wave 0 曾记录该路径相对 `696a12b0` 无 content diff，因此未列入
  23 路径 delta，却未发现 thin-client 目标树已经完全缺类。
- 精确缺口与必要写集：新增/恢复
  `D:\mavenProject\DHXY-cr271\src\main\java\com\bot\dhxy\vision\OcrWindowScanService.java`，或由父级指定
  已存在的唯一 local mask owner 路径并修订 Repair #2 调用。当前卡 4.2/Repair #2 均未授权该 production
  路径；在 `TaskTrackerPanelCaptureLocalMechanics` 或 NPC mechanics 再实现一份 masks 会违反唯一算法与禁止复制。
- Repair #4 闭合证据：`CloudPlayerStateFirstAidPort.captureBars` 已移除 pre-capture pointer clear；
  `executeTargets` 仅 ordered 非空时在同一 command 保持每 target `CLICK_RIGHT -> WAIT(800)`，尾部恰好一次
  random safe `MOVE_MOUSE -> WAIT(300)`，排除 `relX>=761 && relY<=147` 并校验 exact window。
- 后续状态：DHXY-cr271 compile `FAILED`（上述唯一 symbol）；Cloud compile 因 immediate-stop 未运行。
  未运行 tests/debug/replay/source guards/runtime/application/server/Task/UI/capture/input，未做 Git mutation；
  `D:\mavenProject\DHXY` 始终只读且未 build。不能写 delivered/Approved。

<!-- TRUE_EOF: TURN-40E PLAN-CONTRACT BLOCKED PCB-05 OCR-WINDOW-SCAN-OWNER-ABSENT COMPILE-FAILED 2026-07-20 -->

## PARENT PLAN-CONTRACT REPAIR #5 / PURE DEFAULT-WINDOW MASK OWNER

- 父级批准新增 DHXY-cr271 production 路径
  `src/main/java/com/bot/dhxy/vision/OcrWindowScanService.java`，但**不得**复制只读本地基线中带 capture、OCR、
  learned ROI、temp-file 和 Spring dependencies 的完整 Service。新增文件必须与 Cloud 现有同路径的 pure/stateless
  4KB subset byte-identical，只保留 `OcrWindowRegion`、固定 `1024x768` source region、五个 default masks、
  `defaultMaskedWindowRegion/isDefaultMaskedWindowRegion/copyWithDefaultMasks` 与 private `applyMasks`。
- 为满足唯一 local mask owner，原卡 4.2 已授权的
  `NpcClickYellowTargetLocalObservationMechanics.java` 与
  `NpcClickPlayerAnchorLocalObservationMechanics.java` 必须改用该静态 owner，并删除各自重复的
  `DEFAULT_MASKS`/private `copyWithDefaultMasks` 实现；mask rectangles、white fill、clamp、copy image type 与调用
  条件不得变化。`TaskTrackerPanelCaptureLocalMechanics` 保持 Repair #2 的同一调用。
- Cloud `OcrWindowScanService.java` 本轮只读，不修改；交付必须给出双仓 owner SHA byte-identical 证据。
  禁止新增 Spring bean、capture/OCR/client dependency、第二 mask helper 或把 Cloud recognition 移回客户端。
- 重新运行 DHXY-cr271 `mvn -q -DskipTests compile`；成功后再运行 Cloud `mvn -q compile`（Enforcer 禁止
  skip 参数且 compile phase 不执行 tests）。无命名测试授权，不运行 tests。Repairs #2-#4 与 LD-04..LD-10
  已完成 source 保留；compile 闭合后完成全 23 路径 ledger、双仓 SHA/mtime 和 canonical whole-card delivery。
  Owner retained，恢复 `SOURCE ACTIVE / REPAIR #5`。

<!-- TRUE_EOF: TURN-40E PARENT PLAN-CONTRACT REPAIR-5 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## STATUS / ACK — PARENT PLAN-CONTRACT REPAIR #5

- Agent id：`Codex-TURN-40E-implementation-worker-20260720`；canonical owner retained。
- Status：`SOURCE ACTIVE / REPAIR #5 ACKNOWLEDGED`；PCB-05 按父级 pure/stateless owner 写集裁决解除。
- ACK owner：DHXY-cr271 新增 `vision/OcrWindowScanService.java` 必须与 Cloud 现有同路径 4KB subset
  byte-identical，仅保留固定 window region/default masks 与 copy/apply 静态能力；不得引入 Spring、capture、
  OCR、learned ROI、temp-file 或 client dependency。Cloud owner 本轮只读。
- ACK callers：`NpcClickYellowTargetLocalObservationMechanics` 与
  `NpcClickPlayerAnchorLocalObservationMechanics` 改用该唯一 static owner并删除各自 `DEFAULT_MASKS`/
  `copyWithDefaultMasks`；tracker 保持 Repair #2 同一调用，mask 条件与像素语义零变化。
- 执行门：先闭合 DHXY-cr271 `mvn -q -DskipTests compile`，成功后运行 Cloud `mvn -q compile`；不运行
  tests/runtime/application/server/Task/UI/capture/input，不写/build `D:\mavenProject\DHXY`，不做 Git mutation。

<!-- TRUE_EOF: TURN-40E WORKER STATUS-ACK REPAIR-5 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## WHOLE-CARD SOURCE+TEST DELIVERED

### Canonical delivery state

- Agent id / owner：`Codex-TURN-40E-implementation-worker-20260720`；owner retained，等待父级本人审核；未写 Approved。
- 三仓最终观测：baseline `codex/baseline-696a12b0` / `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` /
  status `93`；client `thin-client-design` / `59b85e0bb494f43ad7e7434f3d2170deb373c6ef` / status `96`；
  Cloud `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` / status `33`。
- baseline 全程只读，未 write/build/format；三仓均未 checkout/reset/clean/revert/branch/commit/stage。共享 dirty/untracked
  数量变化只作观测，不归因、不覆盖。

### 23 路径逐项 ledger

| # | 本地差异路径 | 分类 | CR271 唯一属主与等价证据 |
|---|---|---|---|
| 1 | `InputActionQueue` | `ALREADY_EQUIVALENT` | local `CapturedTaskTokens` 保留 pause 补时后的 120s waiter；stop/identity drift 仍取消。 |
| 2 | `InputActionRequest` | `ALREADY_EQUIVALENT` | 同一 request 同时携带 pause/stop token，无第二请求或 store。 |
| 3 | `InputActionScope` | `ALREADY_EQUIVALENT` | exclusive callback checkpoint 原位等待 resume；stop 仍 fail-closed。 |
| 4 | `InputActionWorker` | `ALREADY_EQUIVALENT` | focus/action 前及 action-N checkpoint 已覆盖；未重写输入执行器。 |
| 5 | `TaskPauseToken` | `ALREADY_EQUIVALENT` | revision/requestedAt pause duration 已存在，等待预算不吞暂停时长。 |
| 6 | `AutoCombatPanelService` | `MIGRATED` | Cloud raw `auto_remaining.png` `0.80`，click `+43/+28`，保留 Alt+8 retry；无本地第二识别。 |
| 7 | `BagService` | `ALREADY_EQUIVALENT` | 四个永久 local Service 之一；现有 pause-aware atomic input 保留，业务决策未复制 Cloud。 |
| 8 | `DialogService` | `MIGRATED` | Cloud maintenance raw ROI `(260,373)-(378,413)`、raw template `0.85`、150/800ms；broad fallback 删除。 |
| 9 | `NpcClickService` | `MIGRATED` | Cloud 持 profile/OCR/candidate/direct→Ctrl/FIFO 决策；local 仅 capture、purple/yellow mechanics 与原子宏。 |
| 10 | `PlayerStateService` | `MIGRATED` | Cloud first-aid target 决策；唯一 port 非空时同 command 逐 target right+800，末尾一次 safe move+300。 |
| 11 | `QuestManagerService` | `ALREADY_EQUIVALENT` | 永久 local Service，仍只执行本地复合动作；未迁出第二 phase。 |
| 12 | `SummonSkillService` | `MIGRATED` | Cloud 静态 slot policy 保留，ultimate 生成后等待恢复为 2500ms；无 broad cleanup。 |
| 13 | `TaskMaintenanceService` | `ALREADY_EQUIVALENT` | 既有 Cloud maintenance phase + local mechanics 边界可承载 LD-05；未新增 protocol/store。 |
| 14 | `TaskTrackerPanelService` | `MIGRATED` | existing `LOCAL_SERVICE`：local cache/ROI/masked fallback/drag/post-capture；Cloud title/green/ranking，`pendingRepositions` 删除。 |
| 15 | `UICleanerService` | `ALREADY_EQUIVALENT` | 永久 local Service；只由 Cloud task policy 请求目标关闭，未扩 broad cleanup。 |
| 16 | `DialogHandleRequest` | `ALREADY_EQUIVALENT` | 既有 typed request 足够；Cloud Dialog 删除 generic accept fallback，未建第二 payload。 |
| 17 | `FiveRingTaskV2` | `MIGRATED` | exact context count、title-only hot start、Runner prepared/refresh；client stale 日志改为等待 Runner refresh，phase 不下沉。 |
| 18 | `XiuluoTaskV2` | `MIGRATED` | Cloud maintenance 上限 2、失败不清 round、选择性 cleanup、无 generic accept；yellow destination 进入 FIRST_AID window。 |
| 19 | `MainWindowController` | `MIGRATED` | 修罗/五倍 `0=无限` 且无正数封顶；五环仍 1/2；ordered queue index 投影到 start request。 |
| 20 | `GameTextLineOcrService` | `MIGRATED` | local 仅单帧 yellow/purple observation；Cloud `ImageAlgorithms/SmartClickRecognizer` 唯一 mask/profile/candidate owner。 |
| 21 | `WindowTaskRunner` | `ALREADY_EQUIVALENT` | Runner 仍唯一持 tracker scan/click 与 prepared action 消费；未增加 local task phase。 |
| 22 | `WindowRuntimeContext` | `MIGRATED` | exact window 唯一 tracker anchor cache owner；无 Cloud mirror cache。 |
| 23 | `WindowTrackerAnchorMemory` | `MIGRATED` | 新增 local window-relative immutable cache value，经 RuntimeContext 与 tracker local operation 唯一调用。 |

附加 `LD-02 transport closure`：双仓 request/validator 按 `taskCodes` index 校验 `taskMaxRuns`；WUBEI/XIULUO
`>=0`、WUHUAN `1/2`、AUTO `1`；Cloud runtime 写 immutable metadata，turn-native task 只读 context 值。

### 10 行为簇证据

1. 输入：queue/request/scope/worker/pause token 现有链等价；pause 在 focus/action 前等待，resume 同请求，stop/identity
   drift 取消，exclusive checkpoint 与 120s pause 补时保留。
2. 次数：UI -> `WindowTaskControlService` ordered projection -> HTTPS start request -> validator -> immutable metadata ->
   context -> Wubei/Xiuluo/Wuhuan；重复 task code 按 index，不改 `CloudTurnTaskFactory` 或 Cloud global config。
3. Tracker：Cloud service -> `CloudTaskTrackerLocalServiceClient` -> same-turn `LOCAL_SERVICE` -> dispatcher/executor ->
   exact RuntimeContext cache -> cached ROI -> masked full fallback -> drag -> post-drag capture；Cloud 唯一 title/green/ranking。
4. NPC：normal profile masks=true；仅 Alt+A direct-combat=false；purple anchor direct `y-50`，同 session 继续 Ctrl/FIFO，
   全部耗尽才退出。三处 client caller 共用唯一 static default-mask owner。
5. Maintenance/dialog/supply：固定 raw ROI/template/0.85 与 move150/click800；只关闭目标；bars capture/空/healthy/
   disabled 零鼠标，真实 targets 单 submit，末尾 safe point 排除 `relX>=761&&relY<=147` 且在 exact window 内。
6. Summon：LOCKED/EMPTY/OCCUPIED/UNKNOWN 静态槽、只 hover OCCUPIED、删除后复核、IF8 0.80、inactive 色距 12、
   ultimate 2500ms；其余现有 Cloud 策略已等价，不复制本地整类。
7. FiveRing：Runner 持 scan/click，task 消费 `RUNNER_PREPARED_NOT_READY`；title-only hot start；stale 等 Runner refresh；
   combat recovery 清旧 intent；count 仍 1/2。
8. Xiuluo：maintenance 最多 2；失败不重置 round；非终局只处理实际 dialog，终局只处理 OPTION 后关 generic windows；
   无 generic accept；mini-map confirmed 可进入既有 FIRST_AID window。
9. Auto-combat：Cloud 原图 template `0.80`、中心偏移 `+43/+28`，Alt+8 retry 次数/等待保留；旧 HSV/anchor fallback 删除。
10. 资产/data：四个 production consumer assets 按真实调用路径存在；evidence/debug 图片零激活；runtime JSON 零复制、
    零覆盖、零 schema/store 新增。

### 实际 production 路径 SHA / mtime ledger

`before` 为可复现的 repo `HEAD` blob SHA-256；新增/Cloud HEAD 未跟踪路径记 `ABSENT@HEAD`。共享 pre-claim dirty
不覆盖、不伪装为 Worker before；Repair #1 的真实 pre-write SHA 与 Repair #5 两个 NPC pre-write SHA 已在前文及
ACK 前记录。`after` 是交付时 workspace SHA-256。

#### DHXY-cr271

| 路径 | before | after / mtime |
|---|---|---|
| `cloud/turn/protocol/TurnTaskStartRequest.java` | `D4AF7B55DD1B4A6B01DF5EED4E9F2468B745A31241314762242C340D0FF03117` | `D16526892E94A4A94207FAE22B62EEEF0992ABFF0EF49D60262DD2559F090035` / `00:30:25` |
| `cloud/turn/protocol/TurnProtocolValidator.java` | `4E30F9F1FFB5E63603F958CD1042ADB8232F5E2072ED380ECF423FA12C855041` | `C79600CF6303ED729AE3F92DC8F1DB2EB5371AF3A3DF18ED05FC34963F83CEBC` / `00:43:43` |
| `window/control/WindowTaskControlService.java` | `A615C6E36299130A5A45F5EF80BADB7DD499236C775DD11621A6A21063F9B8FC` | `39D47844AC3928B55031B4D6AFCC5C5BC0629AF16EAE5263AA6EE24249E81802` / `00:22:15` |
| `ui/MainWindowController.java` | `E77C7ACB43232DBBBAFB7CFC9B852FEE231075A4072BA561C093CAA56F1DCA29` | `FFAE0D7F504D766106C27BB4E2A357C4A44C25520F4BB4FB655227FBF1DE03B8` / `00:25:06` |
| `protocol/TurnLocalOperation.java` | `A70DBFA3B60F681776D70D9DEAC518BD4AB3B0B69F5B12E379DD19197583FBD8` | `02D36BA42B52F8465F95121D1B6A1268992C5B71EF5A386B1D86AE6417B29FA5` / `00:43:27` |
| `protocol/TurnFramePurpose.java` | `90574E7FAFF4EC8790E43AAEA358C7963C3D198EF1E81321C125E07369C164A1` | `DD797656EE0F85F637E749AACFF004057AD0CE58DDD7F77D4088519A8A1DD758` / `00:43:27` |
| `protocol/TurnLocalServiceCall.java` | `E03F2B54B9F7EE1493FC5532D00C03B681A419BE59A425E076F16B4F3F6A2899` | `2733C2D672F8267DB595C9F5DD76AAF57E2896D7490C458AF6F32CC106F4276D` / `00:43:27` |
| `protocol/TurnTaskTrackerOperationArguments.java` | `ABSENT@HEAD` | `537EC9E29526327EE8105A4BC30650CB87CFAF8B4AA5B826354329212CA30788` / `00:43:27` |
| `protocol/TurnTaskTrackerOperationResult.java` | `ABSENT@HEAD` | `D8D7CB3AC9D653336A4371DBB2A30437E81802E36016622B75FF09736F153FDC` / `00:43:27` |
| `window/runtime/WindowTrackerAnchorMemory.java` | `ABSENT@HEAD` | `B915934FB85DDD588E9F9358173964D60A70C5DA24F82DC1A073E0349CF55720` / `00:44:24` |
| `window/runtime/WindowRuntimeContext.java` | `AF7A2CAA040343D1277F1D9C996A4E963643DEEFCAE46E5DB9BB2AE6E2C70069` | `546A724327393983BD8D315B860775A9F2AE283AB338A3C42DD4027DB5138AA6` / `00:44:24` |
| `service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java` | `D0279A2309154B61F06E94C5F14D4D49B100B4ED97EF7B36C627FF8AA94A826C` | `9ADA199FC1EE7E393CE6F5C215AD11AB7E4F4433670DF669B5FEE441F4B53023` / `00:45:46` |
| `cloud/turn/LocalServiceExecution.java` | `DD471D2E964AAB51FE9250EE098A13C55D324798AC543B6D13559E10749B7458` | `1EC59BAF2B8F02BE1481CE7CC4EC32B25BD41A8944751CC7F7F4178F91E9CF9D` / `00:45:57` |
| `cloud/turn/local/TaskTrackerLocalOperationExecutor.java` | `ABSENT@HEAD` | `60C1720D5F8ADE925B24C71EAEC864D0BBF0D33F4B7BF2850EF332167F772A55` / `00:46:29` |
| `cloud/turn/LocalServiceStepDispatcher.java` | `0056DDB966AEF036E345B272638158B3DDCBB34A12E06F5DFF134C54CF5DFB06` | `292BFE4C4041B7C3DDE57E2F1262FB6BC54975535F79755D8DFAEA82E990A6FC` / `00:46:29` |
| `task/wuhuan/FiveRingTaskV2.java` | `B48EAFB43CF8DC8D33AD18542DBD99197E9748132740EA1B7C1D6D51DDF4E412` | `24C63BFE7808A99E310B814D330824C91C52A19DCE932D3D8792FA375311876F` / `01:07:03` |
| `service/npc/NpcClickYellowTargetLocalObservationMechanics.java` | pre-write `7DFD68BEC548488972452EFAB4D0DB7565AD13CF373230CAA3DC58B6784A55F4` | `902F0C72675581C30275B591037D080010565E5BB738389A838EE5408EA738A0` / `01:17:48` |
| `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java` | pre-write `F3A92BC5AF4C034DA46700166F1A0E78EC5B737AEACD3B1BCE866845E460F1A6` | `61F4E5856ACF346B6FC2BDEA79C6B93A1FFF94A29D37FFB5D2E1D38BC2C19755` / `01:18:06` |
| `vision/OcrWindowScanService.java` | `ABSENT@HEAD` | `33035C81B30050A85D77A5C3A8C569AF178C3A619C86DC85F718EF6917676806` / preserved `2026-07-16T21:53:59-04:00` |

#### dhxy-cloud-brain

Cloud `com/bot/**` 与新增 `com/yueyunfe/**` 文件在 repo HEAD 均为 `ABSENT@HEAD`；以下为完整 after SHA/mtime：

| 路径 | after SHA-256 / mtime |
|---|---|
| `protocol/TurnTaskStartRequest.java` | `D16526892E94A4A94207FAE22B62EEEF0992ABFF0EF49D60262DD2559F090035` / `00:21:53` |
| `protocol/TurnProtocolValidator.java` | `C79600CF6303ED729AE3F92DC8F1DB2EB5371AF3A3DF18ED05FC34963F83CEBC` / `00:43:43` |
| `runner/context/TaskExecutionContext.java` | `2605A01739F11C490C2FAC48A197242042753AB6B92DDBB7BE9EBD4843C7ECE9` / `00:22:47` |
| `remote/CloudTaskServiceMetadata.java` | `A967BBF674AE2226EBE8763786C1406A574475CBC5DBBBF748058329A97E0B43` / `00:22:47` |
| `turn/runtime/CloudTurnTaskRuntime.java` | `1E3E5A35D1362541F864D40FFF61B804757919F1B670FB01ECF904730251D781` / `00:22:47` |
| `task/wubei/WubeiTask.java` | `19EAB860D2A50BF0ADE0C0F62E2389DD3EB9B2DDA2E37DCCC8FD13BC479A514A` / `00:23:23` |
| `task/wuhuan/FiveRingTaskV2.java` | `6935AAD7ADD579C9879E3270AB4F46CBC108B2BB341ED5E0AD1F246A8AB25C75` / `00:28:52` |
| `task/xiuluo/XiuluoTaskV2.java` | `69304115545E326449FE159F552384ED05E784FDEC82D7E889D22D1AB0B714E6` / `01:08:42` |
| `protocol/TurnLocalOperation.java` | `02D36BA42B52F8465F95121D1B6A1268992C5B71EF5A386B1D86AE6417B29FA5` / `00:43:27` |
| `protocol/TurnFramePurpose.java` | `C2F47953192A8BE9DB810B008C5CCA6A1E777D1259E59C635A702D2129739739` / `00:43:27` |
| `protocol/TurnLocalServiceCall.java` | `2733C2D672F8267DB595C9F5DD76AAF57E2896D7490C458AF6F32CC106F4276D` / `00:43:27` |
| `protocol/TurnTaskTrackerOperationArguments.java` | `537EC9E29526327EE8105A4BC30650CB87CFAF8B4AA5B826354329212CA30788` / `00:43:27` |
| `protocol/TurnTaskTrackerOperationResult.java` | `D8D7CB3AC9D653336A4371DBB2A30437E81802E36016622B75FF09736F153FDC` / `00:43:27` |
| `turn/client/CloudTaskTrackerLocalServiceClient.java` | `37B39F345983B0B1E92BD3A68710BCF4D1E54C73F8BCB24DD4D68DB3D6250A67` / `00:47:19` |
| `service/TaskTrackerPanelService.java` | `3407FA4C8689FC285B871FD6B88D08CD49E5932483B4B226080CA99D5E3F10A0` / `00:50:10` |
| `cloudbrain/TaskTrackerPanelService.java` | `44E823A2D242BD76C58E64EC471D81E15E3D5D91E4B9C0D30CDC16960E45A44D` / `01:11:01` |
| `cloudbrain/ImageAlgorithms.java` | HEAD `8F0B72BDC31F463F6B544B39F915089CAB938358F7FB6F9BB0AEAE5211C2B187` -> `60E6456445BCA19707438349F3BFBADD8DA19FC08F882199BFE909A698AF9482` / `00:57:02` |
| `cloudbrain/SmartClickRecognizer.java` | HEAD `CA2A2D41D95CA2DA01D860DEB9E2F9FAB5002342D81591191669276AF9BCBE2A` -> `1BF7A52552C2F2FD7D0949FAA7646A3114E9971EA14417706CB77008E5ED29D5` / `00:57:31` |
| `service/NpcClickService.java` | `A454DB2B0E17D7F03C59442FE7A80ED20DF6034B920EFB973DA92C895F240129` / `00:57:51` |
| `service/playerstate/CloudPlayerStateFirstAidPort.java` | `1F9F7C98845A42B27DAE41E9B2A25FB8D1B04ECF226BB101A9A5B6B77F2E1DCD` / `01:04:27` |
| `service/DialogService.java` | `60E756A0D8ED954378BB4D821EF04459DC29727E19F0FCEAB6092DE43514ED51` / `01:00:31` |
| `service/SummonSkillService.java` | `C7D43F75A84AACE5D726577E4EC1ABFC09D8874A0428F7E2F838A6E8D7E7F14C` / `01:05:13` |
| `service/AutoCombatPanelService.java` | `603AC3656C1A4AE01E2C4F739A35D3213DCFC96CC0B198CB55DC5F55F50534AB` / `01:10:18` |

### Asset / data / write-scope / compile proof

- production assets：`auto_remaining.png` `876B09F6...61C53`；maintenance heal/repair raw
  `A8DD70B9...F30E7` / `9E135727...BA1E`；新增 Xiuluo title `F3230B87...F82`。均由真实 Cloud consumer
  引用。debug/evidence 图片零复制、零激活。
- runtime JSON/config/maps/data 均零覆盖、零 schema 变更、零 import；未建立第二 protocol/store/cache/OCR/template/
  candidate algorithm。Cloud `OcrWindowScanService.java` 本轮只读，SHA `33035C81...76806`，与 client owner
  byte-identical。
- 实际文档写入仅卡 4.4：本卡、`ACTIVE_WORK`、`PACKAGE_ARCHITECTURE`、两计划、两矩阵及 generator 产物
  `cr-dashboard-data.js`；dashboard generator 成功生成 `263` rows。production 写入仅上列 Repair #1-#5 冻结/
  追加写集；未写 test/debug/replay/source guard，也未改 application/server/Task/UI/capture/input。
- DHXY-cr271：`mvn -q -DskipTests compile` exit `0`（32.1s）。Cloud：`mvn -q compile` exit `0`（28.4s）。
  两次 5s launcher timeout 均未形成编译结论，随后同命令长超时明确成功。named tests 未授权，零创建/恢复/运行。

<!-- TRUE_EOF: TURN-40E WHOLE-CARD SOURCE+TEST DELIVERED PARENT-REVIEW-PENDING OWNER-RETAINED 2026-07-20 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - BLOCKED / REPAIR REQUIRED

- Reviewer：CR271 parent final reviewer；结论 `P0/P1/P2 = 0/1/2`，不通过，不得恢复 TURN-41 或交给用户运行。
- `P1` tracker Cloud 识别算法未等价迁移。只读本地
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\TaskTrackerPanelService.java`
  当前差异明确要求：五环 raw `panel_title_yellow.png`、panel `-112/+102`、detail width `214`、
  `TRACKER_COORD_GLYPH_MAX_WIDTH=7`、pathing segment 最小宽 `10`、双段 progress-tail 优先、三窄 glyph
  progress tail 与 final glyph gap gate。Cloud
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java` 仍使用旧
  `panel_title.png` + `WASH_YELLOW`、`-96/+86`、`175`、`5`、最小宽 `18`，且缺三 glyph/gap 分支；
  live `analyzePanelSnapshot` 的 title/detail/link 识别会直接受影响，全窗口 snapshot 入口也仍走旧 geometry。
- `P1` 资源闭包同时缺失：本地生产模板
  `images/template/wuhuan/panel_title_yellow.png` SHA-256
  `B23087779FEE987EE0641BC0185665077061B68D29D953DEEC3EE00DC234000B`，Cloud production resources
  无该文件，现有 `panel_title.png` SHA-256
  `9C9D0211285346B4ED422C59C926A32CE0CF9A64D7755B8353B25D5AE9FEF6C1` 不是等价资产。
- `P2` 双仓 `TurnFramePurpose.java` 未达到卡内 byte-identical 门。DHXY-cr271 为 177 bytes / SHA
  `DD797656...DD758`（5 CR、9 LF），Cloud 为 172 bytes / SHA `C2F47953...9739`（0 CR、9 LF）；
  文字相同但物理字节不同，交付 ledger 已显示不同 SHA，不能声明协议 byte-identical。
- `P2` Cloud facade 的类/参数 JavaDoc 仍声称 Cloud 拥有 anchor/full-window geometry、
  `allowPanelReposition` 会驱动本次 reposition；当前真实生产链已改为 DHXY local mechanics 无条件完成
  cache/ROI/fallback/必要 drag 后只上传 final panel。注释会误导后续维护，须与唯一属主和实际行为同步；
  不借此新增第二状态或恢复 `pendingRepositions`。
- Repair 写集：Cloud `com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java` 完整移植上述当前本地
  title/detail/green-link/progress-tail 语义；Cloud facade 只修与真实链不符的说明/废弃参数语义，不改变
  local-mechanics 唯一属主；新增 Cloud production `images/template/wuhuan/panel_title_yellow.png` 并由真实
  consumer 引用；双仓 `TurnFramePurpose.java` 归一为物理 byte-identical。禁止写/build 本地 baseline、
  禁止复制第二 tracker/OCR 算法、禁止 runtime/input/capture。
- 返修后重新运行 DHXY-cr271 `mvn -q -DskipTests compile` 与 Cloud `mvn -q compile`；无 named tests
  授权，仍不创建/恢复/运行 tests。重新给出相关源码/资产 SHA、23 路径 ledger 更正和 canonical delivery。

<!-- TRUE_EOF: TURN-40E PARENT SOURCE-TEST SOURCE-REVIEW-1 BLOCKED REPAIR-REQUIRED P0-P1-P2-0-1-2 OWNER-RETAINED 2026-07-20 -->

## STATUS / ACK — PARENT SOURCE REVIEW #1 / REPAIR #6

- Agent id：`Codex-TURN-40E-implementation-worker-20260720`；canonical owner retained；不创建 reviewer/sub-agent。
- Status：`SOURCE ACTIVE / REPAIR #6 ACKNOWLEDGED`；Review #1 的 `P0/P1/P2=0/1/2` 不通过结论保持。
- P1 ACK：完整对账并迁移 baseline tracker raw title、panel/detail geometry、green-link segmentation、双段/
  三窄 glyph progress-tail 与 final-glyph gap gate；同步 Xiuluo raw-title/replay 以及 live/full-window 两入口，
  删除旧 production branch。新增并真实消费 `wuhuan/panel_title_yellow.png`，校验指定 SHA。
- P2 ACK：双仓 `TurnFramePurpose.java` 归一为 length/SHA byte-identical；Cloud facade 只修 JavaDoc 和
  `allowPanelReposition` legacy/ignored 说明，不恢复 `pendingRepositions`、第二 cache/算法或 caller phase。
- 执行门：baseline 严格只读且不 build；零 Git mutation/tests/runtime/application/server/Task/UI/capture/input。
  writer 稳定后依次 client `mvn -q -DskipTests compile`、Cloud `mvn -q compile`，再同步 ledger/dashboard 并
  canonical re-deliver，等待父级 Review #2，不写 Approved。

<!-- TRUE_EOF: TURN-40E WORKER STATUS-ACK REPAIR-6 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## WHOLE-CARD SOURCE+TEST RE-DELIVERED — REPAIR #6

### Review #1 closure

- Canonical owner：`Codex-TURN-40E-implementation-worker-20260720` retained；等待父级本人 Review #2；未写 Approved。
- P1 tracker algorithm：Cloud 唯一 owner 的 Wuhuan title 改为 raw
  `images/template/wuhuan/panel_title_yellow.png` direct match；Xiuluo raw title/replay 同步。共同 `analyzePanel`
  主干现在由 live `analyzePanelSnapshot`、full-window `analyzeFullWindow` 和 replay `analyzeSnapshot` 共用，
  不保留 Wuhuan/Xiuluo `WASH_YELLOW` 旧 production branch。
- P1 geometry/link：panel `left=-112,top=12,right=102,bottom=350`，detail width `214`，coord glyph max
  width `7`，pathing min width `10`；两 segment 时 progress-tail 判定 first-priority；三窄 glyph tail、final glyph
  separated-from-name gate 均按只读 baseline 原顺序进入唯一主干。
- P1 asset：Cloud production 新增并由上述 consumer 引用
  `src/main/resources/images/template/wuhuan/panel_title_yellow.png`，length `894`，SHA-256
  `B23087779FEE987EE0641BC0185665077061B68D29D953DEEC3EE00DC234000B`；旧 `panel_title.png` 保留但
  production owner 零引用。
- P2 protocol：DHXY-cr271 与 Cloud `TurnFramePurpose.java` 均为 length `177`、SHA-256
  `DD797656EE0F85F637E749AACFF004057AD0CE58DDD7F77D4088519A8A1DD758`，物理 byte-identical。
- P2 facade：只修 class/method/parameter JavaDoc；`allowPanelReposition` 明确为 ignored legacy compatibility。
  local mechanics 仍独占 cache/ROI/masked full fallback/必要 drag/final panel capture；无 `pendingRepositions`、
  第二 cache/算法或 caller phase 改动。

### 23 路径 ledger（Repair #6 re-delivery）

| # | 路径 | 最终分类/证据 |
|---|---|---|
| 1 | `InputActionQueue` | `ALREADY_EQUIVALENT`；pause 补时 120s waiter 保留。 |
| 2 | `InputActionRequest` | `ALREADY_EQUIVALENT`；pause/stop 同 request。 |
| 3 | `InputActionScope` | `ALREADY_EQUIVALENT`；exclusive checkpoint 原位 resume。 |
| 4 | `InputActionWorker` | `ALREADY_EQUIVALENT`；focus/action checkpoints 完整。 |
| 5 | `TaskPauseToken` | `ALREADY_EQUIVALENT`；pause duration/revision wait 保留。 |
| 6 | `AutoCombatPanelService` | `MIGRATED`；raw 0.80、+43/+28、Alt+8 retry。 |
| 7 | `BagService` | `ALREADY_EQUIVALENT`；永久 local Service。 |
| 8 | `DialogService` | `MIGRATED`；fixed raw ROI/template 0.85、150/800ms、无 broad fallback。 |
| 9 | `NpcClickService` | `MIGRATED`；Cloud 决策/识别，local capture/atomic mechanics。 |
| 10 | `PlayerStateService` | `MIGRATED`；真实 target 单 command，末尾一次 safe move。 |
| 11 | `QuestManagerService` | `ALREADY_EQUIVALENT`；永久 local Service。 |
| 12 | `SummonSkillService` | `MIGRATED`；静态槽与 ultimate 2500ms。 |
| 13 | `TaskMaintenanceService` | `ALREADY_EQUIVALENT`；既有 Cloud phase/local mechanics 边界。 |
| 14 | `TaskTrackerPanelService` | `MIGRATED / REPAIR #6`；local 唯一 cache/capture mechanics，Cloud 唯一 raw title/detail/green/progress recognition；live/full-window/replay 同主干。 |
| 15 | `UICleanerService` | `ALREADY_EQUIVALENT`；永久 local Service，无 broad cleanup。 |
| 16 | `DialogHandleRequest` | `ALREADY_EQUIVALENT`；既有 typed request，无第二 payload。 |
| 17 | `FiveRingTaskV2` | `MIGRATED`；Runner prepared/refresh、title-only hot start、exact count。 |
| 18 | `XiuluoTaskV2` | `MIGRATED`；maintenance 2、round/cleanup/yellow-destination 语义保留。 |
| 19 | `MainWindowController` | `MIGRATED`；0=无限、无正数封顶、五环 1/2。 |
| 20 | `GameTextLineOcrService` | `MIGRATED`；Cloud 唯一 profile/candidate owner，local 单帧 observation。 |
| 21 | `WindowTaskRunner` | `ALREADY_EQUIVALENT`；本地只观察/消费 prepared action。 |
| 22 | `WindowRuntimeContext` | `MIGRATED`；exact-window 唯一 anchor cache owner。 |
| 23 | `WindowTrackerAnchorMemory` | `MIGRATED`；window-relative immutable local cache value。 |

LD-02 ordered `taskMaxRuns` transport、Repairs #1-#5 和其余 9 行为簇证据均保持前次 delivery；Repair #6 只更正
行为簇 3 tracker 的 recognition/asset 完整性和协议物理一致性，不改变任务 phase、候选消费或 input 顺序。

### Repair #6 source / asset SHA ledger

| 仓/路径 | before | after / mtime |
|---|---|---|
| Cloud `cloudbrain/TaskTrackerPanelService.java` | `44E823A2D242BD76C58E64EC471D81E15E3D5D91E4B9C0D30CDC16960E45A44D` | `EE173525453D89A97C27EBC3F98D5AB2816A8F5BD49AB64ED28F787BD1C72526` / `2026-07-20T01:36:59-04:00` |
| Cloud facade `service/TaskTrackerPanelService.java` | `3407FA4C8689FC285B871FD6B88D08CD49E5932483B4B226080CA99D5E3F10A0` | `7342A338C4F77EC995F82C24611B149A1222634D52D88E10AD9974507C1C9140` / `2026-07-20T01:36:59-04:00` |
| Cloud `protocol/TurnFramePurpose.java` | length `172` / `C2F47953192A8BE9DB810B008C5CCA6A1E777D1259E59C635A702D2129739739` | length `177` / `DD797656EE0F85F637E749AACFF004057AD0CE58DDD7F77D4088519A8A1DD758` / preserved `00:43:27` |
| Client `protocol/TurnFramePurpose.java` | length `177` / `DD797656EE0F85F637E749AACFF004057AD0CE58DDD7F77D4088519A8A1DD758` | unchanged，作为 canonical byte source |
| Cloud `resources/images/template/wuhuan/panel_title_yellow.png` | `ABSENT` | length `894` / `B23087779FEE987EE0641BC0185665077061B68D29D953DEEC3EE00DC234000B` / preserved `2026-07-18T10:17:01-04:00` |

### Compile / scope / status proof

- DHXY-cr271 `mvn -q -DskipTests compile` exit `0`；Cloud `mvn -q compile` exit `0`（17.3s）。短 launcher
  timeout 不作结论，随后相同命令均取得明确 exit 0。无 named tests 授权，零创建/恢复/运行。
- baseline 最终只读观测：`codex/baseline-696a12b0` / `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` /
  status `93`；未 write/build/format/git mutation。client status `96`；Cloud status `34`；共享 dirty/untracked
  不清理、不覆盖、不归因。
- Repair #6 production 写入仅 Review #1 冻结的 Cloud 三个 Java 路径与一个 production resource；client
  `TurnFramePurpose` 只读作为 canonical bytes。文档仅卡 4.4 六份状态源、本卡和 dashboard data；generator
  成功 `263` rows。零 tests/debug/replay/source guards/runtime/application/server/Task/UI/capture/input。

## Parent Source Review #2 - 2026-07-20

结论：`P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`。Repair #6 的 tracker 参数、production asset、
双仓 `TurnFramePurpose` 物理字节和 facade ownership JavaDoc 均已闭合；仍有一个生产语义差异：

- **P1 / accept-time full-window snapshot 被错误增加 tracker-anchor 前置条件。** 当前只读本地基线
  `TaskTrackerPanelService.readWubeiTrackerPanelFromSnapshot(...)` 直接在 caller-supplied snapshot 中寻找
  五倍标题；`readXiuluoTrackerPanelFromSnapshot(...)` 委托 replay，同样直接寻找修罗标题。本地
  `XiuluoTaskV2.scheduleAcceptTrackerBackgroundParse(...)` 在接任务同帧后台解析中真实调用该入口。
  Cloud facade 的两个 `...FromSnapshot(...)` 却调用 `algorithm.analyzeFullWindow(...)`，先要求 tracker
  anchor 命中。于是同一张快照内标题和任务详情可识别、但 anchor 缺失或未命中时，本地返回 evidence，
  Cloud 返回 empty，改变接任务后的业务事实。

### Repair #7 frozen contract

- 唯一 production 写集：Cloud
  `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`。
- `readWubeiTrackerPanelFromSnapshot(...)` 与 `readXiuluoTrackerPanelFromSnapshot(...)` 必须复用现有
  `algorithm.analyzeSnapshot(...)` 的直接 title/detail/green-link 主干，不得经过 anchor-required
  `analyzeFullWindow(...)`；保留 caller supplied `absoluteLeft/absoluteTop` 到 screen-absolute 输出转换。
- live `observe(...)`、local mechanics、cache/ROI/masked fallback/drag、replay、Cloud recognition algorithm、
  protocol/resource 和任务 phase 均冻结，不得新增第二算法/store/protocol 或复制业务逻辑。
- 修正相邻 JavaDoc，使 full-window snapshot 的“直接识别”合同明确；Cloud `mvn -q compile` 必须 exit 0。
  DHXY-cr271 production 不应变化；无 named tests 授权，禁止 test/runtime/application/server/Task/UI/capture/input。
- 修复后由同一 owner canonical re-deliver；父级重新逐文件审核，只有 `P0/P1/P2=0/0/0` 才释放 owner
  并开放 TURN-41。

<!-- TRUE_EOF: TURN-40E BLOCKED REPAIR-7-REQUIRED PARENT-REVIEW-2-P0-0-P1-1-P2-0 OWNER-RETAINED 2026-07-20 -->

## STATUS / ACK — PARENT SOURCE REVIEW #2 / REPAIR #7

- Agent id：`Codex-TURN-40E-implementation-worker-20260720`；canonical owner retained；不创建 reviewer/sub-agent。
- Status：`SOURCE ACTIVE / REPAIR #7 ACKNOWLEDGED`；Review #2 `P0/P1/P2=0/1/0` 不通过结论保持。
- ACK：仅修改 Cloud facade `TaskTrackerPanelService.java`，将 Wubei/Xiuluo accept-time
  `...FromSnapshot(...)` 从 anchor-required `analyzeFullWindow(...)` 改为既有 `analyzeSnapshot(...)` direct
  title/detail/green-link 主干，原样传递 caller supplied `absoluteLeft/absoluteTop`；同步相邻 JavaDoc。
- Freeze：live observe/local mechanics/cache/ROI/masked fallback/drag、replay、Cloud algorithm、protocol/resource/
  task phase 均不改；不新增第二算法/store/protocol。baseline 严格只读且不 build；零 Git mutation/tests/
  runtime/application/server/Task/UI/capture/input。
- Gate：仅运行 Cloud `mvn -q compile`；同步既有状态文档/dashboard 后 canonical re-deliver Repair #7，等待父级
  Review #3，不自审、不写 Approved。

<!-- TRUE_EOF: TURN-40E WORKER STATUS-ACK REVIEW-2 REPAIR-7 SOURCE-ACTIVE OWNER-RETAINED 2026-07-20 -->

## WHOLE-CARD SOURCE+TEST RE-DELIVERED — REPAIR #7

- Canonical owner：`Codex-TURN-40E-implementation-worker-20260720` retained；等待父级本人 Review #3；不自审，
  未写 Approved。Review #2 `P0/P1/P2=0/1/0` 的唯一 P1 已按冻结单文件写集返修。
- Wubei chain：`readWubeiTrackerPanelFromSnapshot(path,absoluteLeft,absoluteTop,source)` -> read supplied image ->
  existing `algorithm.analyzeSnapshot(image,absoluteLeft,absoluteTop,"wubei",-1,source)` -> shared direct
  title/detail/green-link analysis。无 tracker-anchor 前置条件。
- Xiuluo chain：`readXiuluoTrackerPanelFromSnapshot(path,absoluteLeft,absoluteTop,source)` -> read supplied image ->
  existing `algorithm.analyzeSnapshot(image,absoluteLeft,absoluteTop,"xiuluo",-1,source)` -> 同一 direct analysis。
  caller supplied screen-absolute origin 原样传入，输出坐标转换未变。
- 相邻 JavaDoc 已明确 supplied snapshot direct recognition、无 anchor requirement、absolute coordinate unit 和
  nullable source。live `observe` 仍走 local final-panel mechanics + `analyzePanelSnapshot`；cache/ROI/masked full
  fallback/drag、replay、Cloud algorithm、protocol/resource/task phase 均零改动；无第二算法/store/protocol。
- 23 路径 ledger 更正：仅第 `14` 项补充 accept-time snapshot direct-title 合同；其最终分类仍为 `MIGRATED`，
  local 唯一拥有 cache/capture mechanics，Cloud 唯一拥有 recognition。其余 22 项和 10 行为簇证据不变。
- 唯一 production 文件：Cloud `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`，before length
  `55683` / SHA-256 `7342A338C4F77EC995F82C24611B149A1222634D52D88E10AD9974507C1C9140`；after
  length `56744` / SHA-256 `4D66812219BAA20709A0DE90B222D155F8499D5A73B1BBEE53CF900341B92B39` /
  mtime `2026-07-20T01:48:01.0660881-04:00`。
- Build：Cloud `mvn -q compile` exit `0`（18.7s）；首次 5s launcher timeout 不作结论。无 named tests 授权，
  零创建/恢复/运行；未运行 DHXY-cr271 Maven，未运行 runtime/application/server/Task/UI/capture/input。
- 最终只读状态观测：baseline `codex/baseline-696a12b0` / `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` /
  status `93`，未 write/build/format/Git mutation；client `thin-client-design` /
  `59b85e0bb494f43ad7e7434f3d2170deb373c6ef` / status `96`；Cloud `navigation-migration` /
  `3b988caa010254973e03342272e6d1d6a9685b01` / status `34`。共享 dirty/untracked 未清理、未覆盖、未归因。
- 文档写入仅本卡、`ACTIVE_WORK`、`PACKAGE_ARCHITECTURE`、两计划、两矩阵和 dashboard data；generator
  成功 `263` rows。Repair #7 无其他 production/resource 写入。

<!-- TRUE_EOF: TURN-40E WHOLE-CARD SOURCE+TEST RE-DELIVERED REPAIR-7 PARENT-REVIEW-3-PENDING OWNER-RETAINED 2026-07-20 -->

## Parent Source Review #3 - FINAL - 2026-07-20

结论：`SOURCE+TEST SOURCE REVIEW PASSED / P0/P1/P2=0/0/0`。

- Repair #7 唯一 production 文件 SHA-256 为
  `4D66812219BAA20709A0DE90B222D155F8499D5A73B1BBEE53CF900341B92B39`；修复窗口内 Cloud
  `src/main` 只有该文件发生 mtime 变化。
- `readWubeiTrackerPanelFromSnapshot(...)` 与 `readXiuluoTrackerPanelFromSnapshot(...)` 均读取 caller
  supplied image 后直接调用既有 `algorithm.analyzeSnapshot(...)`；该方法将 snapshot 与
  `absoluteLeft/absoluteTop` 原样交给统一 `analyzePanel(...)`，不经过 tracker anchor。当前只读本地基线的
  五倍/修罗 direct-title snapshot 语义及 `XiuluoTaskV2.scheduleAcceptTrackerBackgroundParse(...)` 生产调用已闭合。
- live observe/local final-panel mechanics、window cache/ROI/masked fallback/drag、Cloud recognition algorithm、
  protocol/resource、task phase 和 input 顺序均未被 Repair #7 改动。Review #1/#2 的全部 P1/P2 已关闭；
  23 路径、10 行为簇最终为 `无未批准业务差异；按用户确认的当前本地 workspace 逻辑等价迁移`。
- 父级在 Worker 停止写入后现场运行 Cloud `mvn -q compile` exit `0`，以及 DHXY-cr271
  `mvn -q -DskipTests compile` exit `0`。无 named tests 授权，零测试执行；未启动
  runtime/application/server/Task/UI/capture/input。
- canonical implementation owner 释放为 `ZERO OWNER`。TURN-40E source/compile gate 完成；TURN-41 开放为
  `READY / USER FRESH RUNTIME GATE`，只能由用户发起真实运行验收。

<!-- TRUE_EOF: TURN-40E SOURCE+TEST-SOURCE-REVIEW-PASSED PARENT-REVIEW-3-P0-0-P1-0-P2-0 ZERO-OWNER TURN-41-READY 2026-07-20 -->

## Parent Completion Audit Reopened - 2026-07-20

用户在 IntelliJ 实盘源码检查中指出客户端仍有 6,853 行 `XiuluoTaskV2` 和 3,361 行 `FiveRingTaskV2`。
父级重新审计 production call graph 后确认：UI 默认调用 local `start(...)`，`DefaultTaskFactory` 与
`WindowTaskRunner` 仍实例化/执行客户端厚 Task；两个 remote start API 只有声明、零 production caller。

因此 Review #3 只证明 TURN-40E 冻结的 post-696 差异已被吸收到当时双端结构并可编译，不能证明用户要求的
Cloud-default thin client 最终形态。原 `TURN-41 READY` 结论撤销；新增 TURN-40F 完成默认入口切换、完整依赖
manifest 与客户端厚 Task 零引用退役。当前不修改 Java、不运行 runtime。

<!-- TRUE_EOF: TURN-40E SOURCE-DELTAS-REVIEW-PASSED COMPLETION-CLAIM-REOPENED TURN-40F-REQUIRED ZERO-OWNER TURN-41-BLOCKED 2026-07-20 -->
