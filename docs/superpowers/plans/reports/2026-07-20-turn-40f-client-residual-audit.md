# TURN-40F 客户端残余全面审计与收薄迁移计划

## 1. 审计结论

当前 CR271 不是“Cloud 厚 Task + client 仅机械执行”。真实生产入口仍启动本地 Task，客户端同时保留大量
业务 Service、OCR/识别算法、旧 remote lifecycle/handler 以及新的 HTTPS turn 机械层。用户在 IntelliJ 中看到
大量本地源码并非错觉。

本轮只做只读源码、调用链和目录盘点；未修改 Java，未启动应用、服务、Task、UI、截图或输入，未运行测试。
`D:\mavenProject\DHXY` 旧基线保持只读。

## 2. 物理盘点

CR271 `src/main/java/com/bot/dhxy` 当前共有：

- `586` 个 production Java 文件，`124,233` 行。
- `261` 个文件在 Cloud 仓有同相对路径 peer，客户端仍保留 `48,744` 行。
- `325` 个文件没有同相对路径 Cloud peer，客户端保留 `75,489` 行。

主要目录：

| 客户端目录 | 文件数 | 行数 | 初步裁决 |
|---|---:|---:|---|
| `task` | 49 | 18,335 | 厚 Task/phase/context 必须断开并退役；少量本机系统命令另行裁决 |
| `service` | 63 | 30,843 | 混合目录；业务门面迁云/删除，capture/input 宏机械层保留 |
| `vision` | 7 | 3,612 | 业务 OCR/识别 owner 迁云；纯遮罩/裁剪可保留 |
| `cloud/remote` | 129 | 18,627 | 旧 transport/poller/lifecycle/handler/DTO，按 SCC 分批退役 |
| `cloud/task` | 55 | 10,918 | 旧局部 Cloud-decision client/shadow，remote thick-task 成为唯一 owner 后退役 |
| `cloud/decision` | 14 | 1,079 | 旧 generic decision sidecar，需随 caller 零引用退役 |
| `cloud/xiuluo` | 8 | 1,002 | 修罗旧 decision sidecar，需随厚 Task 零引用退役 |
| `cloud/turn` | 74 | 7,185 | 混合；HTTPS transport/protocol/local executor 保留，旧桥接逐 symbol 删除 |

“Cloud peer 存在”不等于客户端文件可以保留；同一业务算法双端存在仍是双 owner。反过来，“client-only”也不
等于应删除，Win32、HWND、截图和物理输入天然只应存在客户端。

## 3. 已确认的生产入口错误

1. `MainWindowController` 的生产启动调用 `WindowTaskControlService.start(...)`。
2. `WindowTaskControlService.startSameQueue(...)` 与 `startSelectedTasks(...)` 使用
   `TurnModeGuard.startLocal(...)`。
3. `WindowTaskRunner` 调用 `taskFactory.createTask(...)`，随后调用本地 `task.execute(...)`。
4. `DefaultTaskFactory` 仍构造 `XiuluoTaskV2`、`FiveRingTaskV2`、`WubeiTask`。
5. `startRemoteSameTask(...)` 与 `startRemoteSelectedTask(...)` 只有声明，无 production caller。

因此，在任何删除前必须先把 UI/control/lifecycle 唯一生产入口切到现有 HTTPS turn；否则直接删 Task 只会让
当前应用无法启动任务。

## 4. 厚 Task 退役 cohort

已确认三个主要本地业务状态机：

| 文件 | 当前行数 | 最终裁决 |
|---|---:|---|
| `task/xiuluo/XiuluoTaskV2.java` | 6,853 | Cloud factory/runtime 可构造并成为唯一入口后删除 |
| `task/wubei/WubeiTask.java` | 4,853 | 同上 |
| `task/wuhuan/FiveRingTaskV2.java` | 3,361 | 同上 |

同一 cohort 还包括只服务本地 phase machine 的 `*RoundContext`、`*PhaseContext`、`*StepOutcome`、旧 brain
state/provider、task template/transaction/pause wrapper。每个文件必须以 production 引用为准标记
`REWIRE` 或 `DELETE`，禁止仅删除三个大类留下第二套业务算法。

`AutoBattleTask`、`SleepComputerTask` 等不能凭名称盲删：前者要核对 Cloud task factory，后者涉及明确的本机
系统电源命令，必须与用户可见功能合同分别裁决。

## 5. Service 全面裁决

### 5.1 必须失去业务 owner 身份的本地门面

下列大类仍包含候选选择、阶段条件、fallback/retry、状态解释或业务 orchestration；最终不得作为本地业务
owner 继续被 Task/Runner 调用：

- `TaskMaintenanceService`：3,136 行
- `NavigationService`：2,999 行
- `TaskTrackerPanelService`：2,402 行
- `DialogService`：2,147 行
- `SummonSkillService`：1,619 行
- `PlayerStateService`：1,554 行
- `NpcClickService`：1,303 行
- `AutoCombatService`：1,139 行
- `BattleRadarService`、`TeamReturnService`、`CommonBoxService`、`AutoCombatPanelService`、
  `ReturnItemPrescanService` 等业务门面

迁移方式不是把这些类原样留在客户端供 Cloud 远程调用。正确形态是：Cloud 持有业务判断；客户端仅保留
强类型 local operation executor 和它直接需要的 capture/input mechanics。门面类在 caller 归零后删除，或收缩
为没有业务判断的机械 adapter；二者不能并存。

### 5.2 可永久留本地的机械能力

以下类型符合薄客户端边界，但仍需去除其中潜藏的业务判断：

- `*CaptureLocalMechanics`、`*ObservationMechanics`：只负责绑定 HWND 截图、ROI、遮罩、像素/模板原始事实。
- `*LocalMacroMechanics`：只负责 Cloud 已明确下达的一次原子输入序列及机械结果。
- `InputActionQueue`、`InputActionWorker`、`InputSequences`、Win32 mouse/keyboard、窗口聚焦与绑定。
- HTTPS turn protocol、client、loop、dispatcher、local operation executor。
- 只保存窗口短期事实、幂等/重试所需 wire 状态的 client runtime；不得复制 Cloud task phase/store。

### 5.3 既有合同明确永久本地的 Service

当前权威计划明确永久本地 Service **只有四个**：`BagService`、`UICleanerService`、
`QuestManagerService`、`GiveItemService`。禁止第五个 Service。原 `SystemPowerService` 的 Windows 睡眠能力
必须留在客户端，但应降为非 Service 的 host/local-operation executor；不能占用第五个永久本地 Service 名额。四个保留项也不得
承载 task phase 或 Cloud 应拥有的业务选择。

## 6. OCR / Vision 专项裁决

用户提到的 OCR 残余确实存在，production `service/vision/task` 中 OCR 相关符号仍有大量引用。

| 类 | 行数 | 当前 caller | 裁决 |
|---|---:|---|---|
| `ObjectiveTextRecognitionService` | 1,026 | 本地修罗 Task、`DialogService` | 业务 OCR owner；迁云并随本地 caller 删除 |
| `MiniMapCoordinateReader` | 392 | Runner、Navigation、Radar、PlayerState 等 | 混合；截图事实采集留本地，坐标/标签解释与选择迁云 |
| `LocationVisionService` | 271 | `PlayerStateService` | 位置业务识别迁云；本地只返回图像/机械 observation |
| `SheyaoxiangDigitTemplateReader` | 341 | incense local observation | 双端 peer；需验证是否只是本地原始数字读数，避免双算法 owner |
| `OcrTextMatcher` | 206 | map canonicalizer、NPC Ctrl probe | 字符串业务匹配应由 Cloud 决策；client 宏只能报告 OCR 原始事实 |
| `OcrWindowScanService` | 85 | tracker/NPC capture mechanics | 仅做固定遮罩/复制时可留本地，不得解释文字业务含义 |
| `MapSurveyService` | 1,291 | UI 手工地图测绘工具 | 独立工具功能；不能混入 task runtime，需单独决定保留本地工具还是迁云 |

薄客户端并不意味着客户端不能裁剪图片或生成 OCR 输入图；它意味着客户端不能依据识别结果决定“现在做哪一
步、点哪个业务候选、何时重试/结束”。

## 7. 旧 Cloud 客户端栈专项裁决

`cloud/remote` 的 `129` 文件/`18,627` 行和 `cloud/task` 的 `55` 文件/`10,918` 行是第二个大残余源：

- `LocalRemoteGameCommandHandler`：3,530 行
- `RemoteTaskRunLifecycleService`：2,606 行
- `RemoteTaskRunRegistry`：1,598 行
- `RemoteOperationPayloadCodec`：1,186 行
- 各种 `*CloudDecisionService` / `*CloudShadowService`

这些不能与新 HTTPS turn 体系长期共存。退役顺序必须按引用 SCC：先切生产入口和 consumer，再删除 handler/
decision sidecar，再删除 lifecycle/transport，最后删除 DTO/codec/fact 残余。TURN-42/43 现有删除卡必须吸收本
报告的扩大范围，不能只删原先狭窄名单。

## 8. 实施波次与写集合同

### Wave A：Cloud-default 唯一入口

- 修改 UI/control start 路径，XIULUO/WUHuan/WUBEI 默认且唯一进入 HTTPS turn。
- 对齐 same-task、selected-task、queue、次数、role/team、failure policy、pause/resume/stop/unregister。
- 本地 thick start 不可达；不新增第二协议、第二 store 或 fallback 回本地 Task。

### Wave B：断开本地 Task factory/runner

- `DefaultTaskFactory` 不再构造三大厚 Task。
- `WindowTaskRunner` 仅保留窗口 runtime、watcher/capture/input/diagnostics 和 remote turn handle 所需职责。
- `MultiWindowTaskManager`、registration 与 running handle 统一到 remote lifecycle。

### Wave C：厚 Task 与专属模型退役

- 先生成逐文件引用清单，删除三个厚 Task。
- 删除只由它们消费的 phase/context/outcome/provider/template/transaction 残余。
- 任何仍被 Cloud/local mechanics 共享的 DTO 必须先迁到正确 protocol/model ownership，再删旧类。

### Wave D：Service 与 OCR 收薄

- Cloud 成为 Dialog/Navigation/Tracker/PlayerState/NPC/Combat/Maintenance 的唯一业务 owner。
- 客户端门面改由 local operation dispatcher 暴露机械事实和动作；caller 归零后删除门面。
- 删除本地 Objective OCR、matcher、位置解释等业务算法；保留固定 capture/preprocess primitive。

### Wave E：旧 remote/decision 栈退役

- 合并 TURN-42M/43M 的 exact manifest。
- 按 consumer -> handler/sidecar -> lifecycle/transport -> DTO/codec 的顺序删除。
- 新 HTTPS turn protocol/executor 与永久本地 mechanics 是保护集。

### Wave F：静态与构建门

- 逐文件零引用扫描，确认 UI 没有 local start、factory 没有厚 Task、client 没有 task phase owner。
- DHXY 与 Cloud 双端 compile 必须成功。
- 只运行用户明确授权的 HTTPS turn named contract test family；不得扩跑其它测试。
- 父级逐文件 source review 通过后才允许 TURN-41。

### Wave G：用户 fresh runtime

- 由用户启动真实应用；验证三任务从 UI 进入 Cloud、Cloud 日志出现 task/turn，client 日志只出现 capture/input/
  local operation，不出现本地 Task `execute`/phase。
- 运行证据通过后，再完成最终零引用删除与收尾。

## 9. 放行标准

TURN-40F 只有同时满足以下条件才能通过：

1. UI 生产启动 100% 进入 HTTPS turn，remote 方法不再是无 caller 的旁路。
2. 三大本地厚 Task 与专属 phase/context 源码删除，或有逐文件证据证明属于永久本机能力。
3. 本地 Service/OCR 不再拥有业务选择、phase、retry/fallback、候选排序和终止判断。
4. 旧 remote/decision sidecar 不再形成第二协议、第二 lifecycle 或第二算法。
5. 永久本地 capture/input/window mechanics 仍完整，且没有被误删。
6. 双端 compile、授权测试、父级 source review 全部通过。

当前状态：审计结论已冻结；Java 实施尚未开始，TURN-41 继续阻塞。

<!-- TRUE_EOF: TURN-40F CLIENT-RESIDUAL-AUDIT COMPLETE JAVA-NOT-STARTED TURN-41-BLOCKED 2026-07-20 -->
