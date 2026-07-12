# A-1 Service 迁移矩阵（THIN_CLIENT_V1）

工件编号：A-1（终审 Final #1 工件计划）
来源共识：§12.1（Q1 六项验收）、Final #1、A-3 v3（tier 与归属术语）
状态：**骨架 v1** —— 顶层组件盘点完备（全业务包已枚举）；**方法级底账（REQ-M-*）与反向扫描证据为已知缺口，显式 NOT_EVALUATED，阻塞切换**。
基线锚定：DHXY 主库 `src/main/java`（344 个 .java）；冻结 commit 待批量提交后登记（本骨架为盘点,非冻结事实）。
诚实声明：本文是顶层完备盘点 + 已有资产引用 + tier 初判，**不是**完成的方法级底账。Q1 收口要求的方法级 inventory（含继承/lambda/监听器/条件注册）与反向静态扫描零命中，需全库字节码扫描（javap/ASM）或多代理 workflow 产出，属专项工作量，未在本骨架内完成。

---

## 0. 盘点范围与规模

| 包 | 类数 | 性质 |
|---|---|---|
| task | 5 | 任务基类/工厂/自动战斗/挂机 |
| task/wubei | 8 | 五倍（有 WUBEI_CLOUD_MIGRATION_BASELINE.md）|
| task/wuhuan | 5 | 五环 |
| task/xiuluo | 14 | 修罗（有 XIULUO_MIGRATION_LEDGER.md 243 方法分类）|
| task/hotstart, pause, startup, transaction, template, model | ~15 | 任务支撑 |
| service | 25 | 业务 Service 主力 |
| vision | 5 | OCR/地图/坐标识别 |
| model/navigation, model/tasktracker | ~18 | 导航/任务追踪数据模型 |
| cloud/decision | 14 | 云决策客户端框架（已迁移基础设施）|
| cloud/task | 55 | 已上云的决策服务 |
| cloud/xiuluo, cloud/runtime | 9 | 修罗云端+运行时 |

## 1. 已有可复用资产（种子，不重扫）

| 资产 | 覆盖 | 状态 |
|---|---|---|
| XIULUO_MIGRATION_LEDGER.md（DHXY-xiuluo worktree）| 修罗 243 方法 = cloud-policy 73 / local-safety 153 / transitional 17 | untracked，需先 commit 取 hash 才能作冻结种子（B Final#1 已确认）|
| WUBEI_CLOUD_MIGRATION_BASELINE.md | 五倍业务基线 | 引用 |
| cloud/decision + cloud/task（69 类）| 已上云决策框架 | 现有实现,矩阵标为"已迁移基础设施/参照" |

## 2. 顶层组件矩阵（tier 为初判，方法级细分待底账）

列：组件 → 当前权威 → 迁后云端 owner → 本地保留 → tier 初判 → 方法级底账状态。
tier：A=状态/协议/身份/lease/输入/stop-pause 安全层；B=影响 phase/动作/retry/fallback/timeout/memory 的业务决策；C=视觉解释；D=纯搬运/DTO。

### 2.1 任务编排层（tier B 为主）

| 组件 | 当前权威 | 云端 owner | 本地保留 | tier | 方法级 |
|---|---|---|---|---|---|
| GameTask / TaskFactory / DefaultTaskFactory | 本地任务生命周期 | Task Orchestrator | 无（executor 壳）| B | NOT_EVALUATED |
| AutoBattleTask | 本地自动战斗状态机 | 自动战斗 Service | 无 | B | NOT_EVALUATED |
| SleepComputerTask | 本地挂机 | 维护 Service | 无 | B | NOT_EVALUATED |
| WubeiTask + 7 支撑类 | 本地五倍 phase | 五倍 Service | 无 | B | 部分（baseline 种子）|
| FiveRingTaskV2 + 4 支撑类 | 本地五环 phase | 五环 Service | 无 | B | NOT_EVALUATED |
| XiuluoTaskV2 + 13 支撑类 | 本地修罗 phase | 修罗 Service | 无 | B | **已有 243 方法分类种子** |
| TaskHotStart* / TaskPauseResume* / TaskStartup* | 本地热启/暂停/启动检查 | 云端恢复+task turn | pause/stop 本地反射（tier A 部分）| A/B 混合 | NOT_EVALUATED |
| TaskTeamAssignmentPolicy | 本地队伍分配 | task turn scheduler | 无 | B | NOT_EVALUATED |

### 2.2 业务 Service 层（service/，25 类，tier B/C 混合）

| 组件 | tier 初判 | 迁后 owner | 备注 |
|---|---|---|---|
| AutoCombatService / AutoCombatPanelService / BattleRadarService | B/C | 自动战斗+视觉 | 战斗决策 B、雷达识别 C |
| DialogService / DialogChoiceMemoryService + service/dialog(6) | B | 交互 Service | Dialog 策略 CR169/CR185 先例约束 |
| NavigationService | B | 导航 Service | 路线决策 |
| NpcClickService | B | 交互 Service | LEARNED_MEMORY 已生产禁用（CR169）|
| MemoryService | B | 记忆 Service | Q4 三池模型 |
| QuestManagerService / TaskTrackerPanelService | B | 任务 Service | |
| PlayerStateService / BagService / SummonSkillService(+Scanner) | B/C | 各业务 Service | |
| MapNameCanonicalizer / CommonBoxService / UICleanerService | C/D | 视觉/工具 | |
| ClientIdentityService | A | 设备身份 | 本地保留身份采集,解释在云 |
| 其余（GiveItem/LeftTopStatus/ReturnItemPrescan/SmartClickEvidence/SystemPower/TaskMaintenance/TeamReturn/PlayerState 等）| B/C | 对应 Service | 逐个待底账 |

### 2.3 视觉层（vision/，5 类，tier C）

| 组件 | tier | 迁后 owner | 本地保留 |
|---|---|---|---|
| LocationVisionService / MapSurveyService / MiniMapCoordinateReader | C | 视觉 WorkerPool | 仅截图+ROI 裁剪 |
| ObjectiveTextRecognitionService / OcrTextMatcher | C | OCR pool | 无（本地不 OCR）|

### 2.4 云端已迁移层（cloud/，参照，非待迁）

cloud/decision(14) + cloud/task(55) + cloud/xiuluo(8) + cloud/runtime(1)：现有云决策框架。矩阵中作为"目标态云端 owner 的现有实现参照"，其本地对应桩在切换后按 allowlist 清除。

### 2.5 数据模型层（model/，tier D 为主）

model/navigation(10) + model/tasktracker(8)：多为 DTO/结果类，tier D（schema+round-trip 验证）；其中含业务枚举（NavigationResultStatus/WorldMapRouteResultMode 等）需在反向扫描中确认无本地业务分支。

## 3. 隐式状态专项（Q1 点名，需底账逐项落实）

| 对象 | 位置 | 风险 |
|---|---|---|
| TaskTurnCoordinator fair-lock 排队顺序 | 任务编排 | 排队顺序=业务决策，迁云端（§6），矩阵需独立行 |
| 各 Task watchdog/超时重试参数 | 各 TaskV2 | 超时后"做什么"是业务，逐行记 owner |
| kanda2 毫秒探测模板+阈值 | 修罗/战斗 | 布设/撤销策略上云（PROVISION_DETECTOR），资产可本地缓存 |
| DialogChoiceMemory / PendingTransferChoiceMemory | service/model | 本地记忆残留,须迁云或证明纯缓存 |
| 各 Catalog（Wubei/XiuluoDialogCatalog）| task/* | 业务字典,迁云端配置 |

## 4. Q1 六项验收对本工件的完成度

| 验收项 | 状态 |
|---|---|
| 方法级 inventory 全覆盖 | **NOT_EVALUATED**（骨架仅到类级；需 javap/ASM 全库扫描）|
| 入口可达闭包无未知节点 | NOT_EVALUATED |
| 配置/资源零未归属 | NOT_EVALUATED（§3 列出已知隐式对象，未穷尽 resources 树）|
| Thin Client 产物 allowlist | NOT_EVALUATED（有 local-non-xiuluo-brain profile 先例可扩展）|
| 反向扫描零业务语义命中 | NOT_EVALUATED（扫描规则待建）|
| 人工按业务流反向抽查 | NOT_EVALUATED |

## 5. 完成路径（供用户决策）

方法级底账是本矩阵唯一未完成部分，也是唯一阻塞整份 Final PASS 的工件缺口。两条路径：
- **(a) 多代理 workflow**：参照修罗 243 方法账本的 6 代理模式，按包 fan-out 做方法级 inventory + tier 标注 + 反向扫描规则，1000-agent 上限内可覆盖 344 类。需用户明确 opt-in（token 成本高）。
- **(b) 实施会话专项**：留给建设期 S0 阶段（Q6 依赖图 S0=冻结与盘点，本就是 S1 前置），届时对冻结 commit 逐包底账。

本骨架已把"有哪些业务组件、初判 tier、已有种子、隐式风险点"盘全，为两条路径都提供了起点。**在方法级底账完成前，A-1 不通过，A Final PASS 不给。**
