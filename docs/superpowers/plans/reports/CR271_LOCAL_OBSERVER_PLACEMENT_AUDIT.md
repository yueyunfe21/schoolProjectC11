# CR271 本地 Observer 职责与部署边界审计

## 1. 审计目的

本报告先回答部署决策之前的前提问题：旧基线 `WindowTaskRunner` 的 Observer 实际做了什么，CR271/Cloud Brain 当前把这些职责放在哪里，哪些职责可以安全放回本地，以及哪些职责若留在 Cloud 必须通过独立的 observation request/response，而不能继续争用单槽 command turn。

审计对象：

- 只读业务基线：`D:\mavenProject\DHXY`
- 权威客户端：`D:\mavenProject\DHXY-cr271`
- Cloud Brain：`D:\mavenProject\dhxy-cloud-brain`

本报告只做架构审计，不修改 Java 行为。

## 2. 旧基线 Observer 的运行模型

旧基线在 `WindowTaskRunner.startCombatWatcherIfNeeded(...)` 中为五环、五倍和修罗启动独立后台线程。它始终存活到该任务结束，但并非无约束地与任务线程同时操作窗口：

1. 截图绑定当前 `WindowRuntimeContext` 和 HWND。
2. `GameClientTracker.captureToMemory(...)` / `captureToFile(...)` 通过 `globalInputLock` 与物理输入短暂串行。
3. HWND 后台捕获优先；只有显式允许时才退回需要聚焦的 Robot 捕获。
4. Observer 根据当前 interest/pathing/combat 状态动态选择探测项和频率。
5. 识别结果写入 `WindowRuntimeContext`，边沿变化通过 `WindowReadyEventBus` 软唤醒任务。
6. Observer 只准备候选动作；任务重新取得 turn 后复核并消费。旧代码中的战斗自动维护是例外，需要单独拆分为“检测”和“主动输入”。

因此旧模型的关键不是“只有一个线程”，而是“Observer 常驻、短截图与输入共享安全锁、业务任务按事件被唤醒”。

## 3. 完整职责清单与当前映射

| # | 旧 Observer 职责 | 旧基线实际行为 | CR271 / Cloud 当前位置 | 性质 | 初步部署判断 |
|---|---|---|---|---|---|
| 1 | 生命周期与节流 | 每任务启动/停止 watcher；按 combat/pathing/dialog interest 调整周期并支持 100ms 软唤醒 | Cloud `CloudWholeTaskObserver` 固定约 1s 循环；本地没有等价常驻 watcher | 调度基础设施 | 本地常驻 Sensor 更接近基线；Cloud 下发 observation interests |
| 2 | 战斗画面探测 | 截取自动战斗标志、右侧按钮、顶部图标；重复 miss 后结合小地图确认退出 | Cloud `BattleRadarService` 逐个通过 turn 请求 ROI，并在 Cloud 模板匹配/维护迟滞状态 | 机械视觉 + 状态迟滞 | ROI 捕获和固定模板信号可本地；是否发布任务事件可由 Cloud 消费信号后决定 |
| 3 | 战斗边沿事件 | `NONE <-> IN_COMBAT` 时发布 `COMBAT_STATE_CHANGED` | Cloud Observer 发布 Cloud ready event | 运行事实 | 本地可发送带序号的战斗信号/边沿；Cloud 持有任务事件槽与消费语义 |
| 4 | 战斗进入清理 | 五倍清 dialog interest/prepared/gate/pathing；修罗清 tracker shortcut pathing | Cloud Observer 检测边沿后再调用 `WHOLE_TASK_COMBAT_ENTRY_CLEANUP` 回本地，并清 Cloud 镜像 | 本地临时状态一致性 + Cloud 镜像 | 本地可在同一战斗边沿原子清本地 ephemeral state并回报清理事实；Cloud 清自己的镜像 |
| 5 | 自动战斗进入维护 | 旧 `handleWindowCombatGuardTick(...)` 可能执行 key-only 自动战斗 bootstrap | Cloud Observer 改为 read-only probe；任务代码另行调用 `AutoCombatService` | 主动输入/业务动作 | 不应混进被动 Observer；继续走唯一 command/input 队列 |
| 6 | 寻路 intent 读取 | 从 `WindowRuntimeContext` 读取当前 intent/snapshot | 本地仍存 intent/snapshot；Cloud 每轮用 `WHOLE_TASK_PATHING_READ` 读取 | 本地运行事实 | 本地 Sensor 可直接读取，无需 command request |
| 7 | 小地图位置探测 | 最少间隔约 2s 截 `178x35` 坐标条；模板优先识别地图和坐标 | Cloud 请求本地捕获坐标条；`PlayerStateLocationRecognizer` 在 Cloud 执行模板优先、OCR fallback、canonicalize 和 plausibility | 图像采集 + 识别算法 | 本地模板快路径可恢复；OCR fallback/不确定样本可走独立 observation analysis request |
| 8 | 寻路状态分类 | 比较 map/x/y、intent、tolerance、静止时间和 dialog block，得到 `ACTIVE/ARRIVED/STOPPED_AWAY/UNKNOWN` | Cloud Observer `classify(...)` 和 `CloudNavigationPathingState` | 任务调度语义 | 建议 Cloud 保留；本地只发有序 position/dialog samples，避免两份状态机 |
| 9 | `PATHING_TERMINAL` | 状态首次进入 ARRIVED/STOPPED_AWAY 时发布软事件 | Cloud Observer 发布 `CloudWholeTaskReadyEventState` | 任务事件 | Cloud 保留；由本地样本驱动，不再由 Cloud 主动抢 command slot 探测 |
| 10 | 五倍目标地图 gate | 当前地图匹配目标地图后打开 enter-battle gate 和 dialog interest | Cloud Observer读本地 timer/gate，再调用本地 gate open operation | 业务门控 | Cloud 保留决策；本地位置样本触发 Cloud 判断 |
| 11 | 战前 5 分钟超时 | 本地 timer 排除战斗/暂停时间并只发布一次 `PRE_BATTLE_TIMEOUT` | timer/state 仍在本地；Cloud 每秒调用 `WHOLE_TASK_PRE_BATTLE_TIMEOUT_MARK` | 本地计时事实 + 任务事件 | 本地计时和一次性边沿可直接发布；Cloud 消费事件，无需每秒 command round-trip |
| 12 | 通用对话框可见性 | 同一 tick 捕获一次对话框并识别 `OPTION/STORY/NONE`，供多个分支复用 | Cloud 先读本地 dialog runtime fact，再通过 turn 捕获 ROI，在 Cloud `DialogService` 分类 | 机械视觉 | 固定框型/类型模板可本地；若必须保持 Cloud 识别，则由本地 Observer 通过 observation request 上传一次 ROI |
| 13 | `TASK_ATTENTION_REQUIRED` | 有有效 task interest 且对话框可见时限频发布 | Cloud Observer判断 interest/新鲜度并发布 | 业务事件 | Cloud 保留，输入为本地 dialog sample |
| 14 | 路线对话框预准备 | 根据当前 pathing target、可见 OPTION、记忆坐标、OCR/模板生成 `PreparedDialogAction`，不点击 | Cloud `DialogService` 和 Cloud memory 生成 prepared action；本地只提供 runtime fact/ROI | 业务识别 + 候选动作 | Cloud 保留；本地 Observer可直接向独立 analysis endpoint 请求，返回 typed prepared candidate |
| 15 | 修罗进入战斗对话框预准备 | 按修罗 dialog interest 和绿字模板生成候选 | Cloud `prepareTaskInterest(...)` | 任务专属识别 | Cloud 保留，避免修罗业务重新落回本地 |
| 16 | 五倍接任务/进战斗/剧情预准备 | 使用稳定记忆、绿字模板、白字剧情模板和 absent 语义生成候选 | Cloud `prepareTaskInterest(...)` + Cloud memory | 任务专属识别/业务规则 | Cloud 保留；通过 observation analysis request 处理本地 ROI |
| 17 | Observer 后台任务追踪预准备 | 修罗、五倍、五环都会读取任务追踪面板；但旧 Observer 只有五环会在空闲且无高优先级动作时主动截取任务栏，并预生成 `TASK_TRACKER_PATHING` 候选。修罗和五倍主要由各自任务阶段主动读取 | 当前 Cloud Observer 也只主动调用五环预准备；三类任务的实际识别均复用 Cloud `TaskTrackerPanelService` | OCR + 任务语义 | Cloud 保留统一 tracker 分析能力；Observer 预准备和任务阶段读取使用同一分析合同，本地按 interest 上传 panel ROI，不占 command slot |
| 18 | prepared action 归属/防陈旧 | 校验 window/hwnd/taskRun/intent/interest/截图年龄；旧候选清除 | Cloud `CloudDialogPreparedActionState` 为主，本地仍有 runtime facts | 一致性/安全 | Cloud 保留最终 owner/CAS；请求和响应必须携带 taskRunId、intentId、sampleSeq |
| 19 | 路线选择学习结算 | ARRIVED 记 success，STOPPED_AWAY 记 failure；处理 transfer choice/world-map pending memory | pending 槽部分仍在本地，Cloud Observer消费后写 Cloud `MemoryService` | 持久业务记忆 | Cloud 保留；本地只回报 terminal sample 和 pending identity，不在本地决定学习结果 |
| 20 | prepared action 点击 | Observer 不直接消费；任务重新取得 turn 后复核并点击 | Cloud task消费，CR271执行物理输入 | 主动动作 | 必须继续走唯一 command/input 通道 |

## 4. 当前冲突的结构来源

当前 Cloud Observer 的 `pathing/combat/prebattle/attention/tracker` 探测虽然各自使用 Cloud turn coordination，但它们最终仍会调用 `TurnGameClient`。`CloudTurnExchange` 对每个 device/window 只允许一个 unresolved `currentAction`。因此观察和主任务被错误地建模为两个 command producer：

- Observer 要截图/读本地事实时发布 action；
- 主任务同时发布业务 action；
- 任一方先占槽，另一方得到 `BUSY`。

扩大锁区只能降低发生概率，不能从结构上证明未来新增调用不会再次漏包。

## 5. 推荐边界：本地 Observer Runner + Cloud 业务解释

推荐恢复一个本地常驻 `WindowObservationRunner`，但不恢复第二套本地任务状态机。

### 本地职责

- exact HWND 绑定、后台小区域截图、`globalInputLock` 安全边界；
- 动态 interest/节流、一次捕获多分支复用；
- 固定模板/像素差等小型机械探测；
- 本地 runtime timer、intent、pause compensation 等事实读取；
- 维护有序 `sampleSeq`，发送 observation request；
- 缓存 latest snapshot，关键边沿在 Cloud ack 前保留；
- 绝不决定任务 phase，绝不直接点击。

### Cloud 职责

- 任务专属 OCR/模板语义与 dialog catalog；
- pathing terminal 分类和 ready-event 发布；
- prepared action 的 owner/CAS/过期校验；
- 五倍 gate、任务 interest、超时消费语义；
- 路线学习和持久 memory；
- 所有主动动作决策。

### 必须留在唯一 command 通道的内容

- 鼠标、键盘、窗口聚焦；
- 打开/关闭地图、面板调整；
- prepared action 的最终复核与点击；
- 自动战斗 bootstrap 和战后恢复；
- 任何会改变窗口或任务状态的操作。

## 6. Observation request/response 需求

本地 Runner 可以直接调用 Cloud，但不能复用 `/api/v1/client/turn` 的 unresolved command slot。需要明确的 observation plane。一个请求可以包含：

- `tenantId/deviceId/windowId/hwnd/taskRunId`；
- `observerSeq/capturedAt/activeCommandActionId`；
- Cloud 下发的 `interestRevision`；
- 本地轻量事实：combat signal、position fast-path、timer edge；
- 仅在需要时携带一个或多个小 ROI：coordinate strip、dialog、tracker panel；
- 当前 intent/interest identity，用于拒绝陈旧响应。

Cloud 响应可以包含：

- 已确认的最高 `observerSeq`；
- 更新后的 observation interests/采样周期；
- Cloud OCR/模板分析结果；
- typed prepared candidate，但不包含立即执行权限；
- 是否需要补发更高质量 ROI。

请求必须 latest-wins、每窗口最多一个 in-flight analysis；旧响应按 `taskRunId + observerSeq + intentId/interestRevision` 丢弃。关键边沿与普通快照分开：普通快照可覆盖，战斗进入/退出、terminal 等事件需 ack。

## 7. 初步结论

1. 把完整旧 Observer 原封不动放回本地会带回任务专属 dialog、memory 和动作语义，不符合当前 Cloud 业务所有权。
2. 把所有 Observer 探测都留在 Cloud 并经 command turn 拉取，会继续制造 command-slot 竞争。
3. 最合理的切分是：本地恢复常驻 Observation Runner 和小型机械识别，Cloud 保留业务解释、事件状态机、学习与动作决策。
4. 小地图模板快路径、战斗固定模板和 timer 都足够小，可本地处理；OCR fallback、任务追踪、任务专属 dialog 可由 Runner 通过独立 observation request/response 调 Cloud。
5. 该设计允许 Observer 在主任务运行时继续观察；只有实际截图会像旧基线一样短暂经过 `globalInputLock`，不会占用 Cloud command slot。

任务追踪接口不应设计成五环专属。建议统一为 `TRACKER_PANEL_ANALYZE(taskType, purpose, roi, source, taskRunId, observerSeq)`：`OBSERVER_PREPARE` 目前由五环使用，`TASK_PHASE_READ` 则供修罗、五倍和五环的前台任务阶段共同使用，避免复制协议和识别算法。

## 8. 实施前仍需验证的合同点

- 当前 Cloud dialog/tracker analysis API 是否能抽成无副作用的 observation handler，避免复制算法；
- `DialogDetection` / `PreparedDialogAction` 的坐标空间和 frame fingerprint 是否足以跨请求校验；
- combat entry cleanup 哪些本地字段必须与边沿探测原子提交；
- observer event ack 是否可以纳入现有 HTTP 请求而不污染 command outcome；
- 多窗口下 observation analysis 的并发上限、背压和每窗口 in-flight 限制；
- 与唯一逻辑基线及当前 dirty baseline 的逐项回放等价测试。

## 9. 2026-07-21 用户批准后的部署决议

用户已批准把常驻 Observation Runner 放回本地，并要求参考 Git 最新已验证版本 CR232/253/256 恢复修罗
`local-kanda` 快速通道。本文第3节第15/20项及第5节“绝不直接点击”必须按以下**窄例外**解释：

- 通用 Observer 仍只观察和准备候选，不执行任务业务动作；
- 修罗 `local-kanda` 是唯一批准例外：本地 `41x21` 原图 ROI 匹配命中后，带 current attempt 做
  consume-time复验和一次性仲裁，再通过既有唯一 `InputActionQueue` 原子执行 move+click；
- 点击只上报 `ENTER_BATTLE_CLICKED`，真实战斗探测确认后另报 `IN_COMBAT`；点击不提前关闭 Observer；
- 单次 local miss不请求Cloud。仅 current attempt pathing terminal 时异步执行一次 stopped-static Cloud fallback；
- Cloud仍拥有phase、OCR/dialog业务解释、fallback verdict、路线学习和持久memory。

完整实施、写集、并发和验收合同以固定原卡
`docs/superpowers/plans/reports/2026-07-21-turn-card-TURN-40G.md`为准。
