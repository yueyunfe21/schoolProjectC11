# G003b 新手任务：五段拖拽按住会话 + 战斗策略覆盖（两个新原语）

> 依附 G003a。依据草案 §4/§8 + 2026-07-28 用户决议。

## 原语一：跨操作按住拖拽会话（轮回五段，草案 §8）

用户决议：**进度读取用 OCR，不截进度模板**；每次读取重新截 ROI（数字会变），往返不可避免。

设计（云端编排 + 本地按住会话）：

1. 本地新 local-service 宏 `XINSHOU_DRAG_SWEEP`：在**保持左键按住**的独占保持会话内（复用 retained-session/frozen-exclusive 机制，按住状态跨操作维持），执行一段蛇形扫（左右两来回 + 下移 7px 至段终止 Y），随后截进度 ROI，把 ROI bytes 随结果带回；**不松手返回**。
2. 云端收到 ROI → 走 CR257 契约 OCR 读进度（如 `1/2`）→ 判定：未达标 → 再发同段 sweep；达标 → 发 `XINSHOU_DRAG_RELEASE`（松手）→ 下一段。不设固定重试上限（草案 §8.2.4）。
3. 五段参数（起点/右边界/终止 Y/目标进度）与两个进度 ROI 按草案 §8.2 表，经 XinshouGeometry 换算窗口相对。
4. 段间衔接、8.1 道具使用（lunhui_item + shengsi 校验 + 双击 (1776,838)）、8.3 收尾（点 (1630,674) + 10s 内 title 变化事件唤醒 + 1s 后点新绿链）均在云端任务内。
5. 安全：会话因任何异常中断必须先松手（fail-safe release）；暂停/停止请求到达时同样先松手再退出。

## 原语二：战斗策略覆盖（草案 §4）

任务启动即关闭 runner 自动战斗执行与自动补 Alt+8，**保留进出检测**（通用战斗边沿）。AutoCombatService 新增任务级策略（第一个"检测归检测、执行全由任务接管"的消费者）：

- **抓 title 完成前**的任意战斗：进战边沿 → 按 Alt+A；此后每 10s 一次直至退战边沿；
- **抓捕战斗**（zhua_title 下经绿链+option 进战）：Alt+B → 复用自动面板判定（看不见"自动"=成功，否则重试）→ 点 (1246,777 换算) → 等退战边沿；本段不用 10s Alt+A；
- **抓后首战**：进战边沿 → 两次 Alt+A 间隔 1s → 面板判定（重见"自动"=成功，否则重试）→ 恢复 runner 自动战斗与自动补 Alt+8，此后全走既有逻辑。

等待契约同 G003a 铁律：战斗期间的 10s 节拍用有界 park（wake=COMBAT_STATE_CHANGED，timeout=10s），退战即醒。

## Status

- 2026-07-28 建卡。待 G003a 完成后实施。

## Status

- 2026-07-28 Claude 实施完成（用户要求"全部做完才能测试"）。双仓编译 0 错、协议目录 diff 零差异、客户端 xinshou 测试 10/10 绿。
- **战斗策略（§4）**：新 `XinshouCombatPolicy` 四相位（BEFORE_CAPTURE→CAPTURE→RESTORE_PENDING→RUNNER_OWNED）。主循环 `inCombat` 时战斗独占该轮（不读 tracker/不点绿链，修掉了"战斗中仍点绿链导致 input incomplete"的实机 bug），按相位执行：抓前 Alt+A 每 10s（有界 park 到 ALT_A_REPEAT_MS 或退战边沿）；抓捕 Alt+B→`AutoCombatPanelService.isAutoPanelVisible` 新增只读探针验证面板消失→点 (255,408)（abs 1246,777）；抓后双 Alt+A 间隔 1s→面板重现验证→交还 runner。退战边沿 `onCombatExit` 复位并推进相位。zhua_title 观察到即置 CAPTURE 相位。
- **轮回拖拽（§8）**：协议新增 `XINSHOU_DRAG_SWEEP`/`XINSHOU_DRAG_RELEASE` + `TurnXinshouDragArguments`（`TurnLocalServiceCall` 加第 9 字段，保留旧构造器兼容；validator 双仓同步）。客户端 `WinApiMouseController.holdSweepWithoutRelease`（按下不放、每行左右两来回、下移 7px 至终止 Y）+ `releaseLeftButton`；`XinshouDragLocalOperationExecutor` 在输入 worker 独占内执行扫动并回传进度 ROI 帧（异常路径必先松手）。云端 `XinshouDragPlan`（五段参数+两个进度 ROI，均已按 rel=abs-(991,369) 换算）+ `XinshouDragSession`：逐段循环"扫一次→OCR 读进度→未达标再扫/达标松手换段"，OCR 走 `LocalOcrClient.readJoinedText` + 数字/斜杠归一化；每段 60 次 sanity 上限；`finally` 保证释放。§8.1 道具（lunhui_item→shengsi ROI 验证→(785,469) 双击间隔 1s）落在客户端 title handler；§8.3 完成点 (639,305) 在云端任务。
- 已知待实机校准：五段坐标与两个进度 ROI 全部来自草案换算，未经实机验证；OCR 进度读数若不稳需按日志 `xinshou drag segment N sweep M: progress='...'` 调整 ROI。

## 2026-07-29 战斗独占分支回归修复

- Fresh `19:41` 证明原实现已偏离本卡：普通战斗动作走
  `BoundWindowKeyboardService` 后台 `PostMessage`，虽然日志返回 `true`，游戏未可靠执行；
  战斗期间旧 Tracker/pathing 仍继续存活，脱战后又被判为 `STOPPED_AWAY`，前台没有下一动作。
- 修复恢复本卡的任务专属战斗所有权：`IN_COMBAT` 时清除非战斗 PreparedAction，禁止
  Dialog/Tracker/title 抢占；普通战斗首个 `Alt+A` 按用户确认改为进战 `5s` 后，之后保持
  `10s` 节拍；抓捕和恢复动作顺序不变。
- Client 的普通/恢复 `Alt+A` 改为 frozen exact-window focused transaction 内的真实
  `InputProvider.pressAltA()`；不再用“HWND 消息发送成功”冒充游戏已执行。
- 首个普通/抓捕战斗动作执行前，复用既有 `WHOLE_TASK_COMBAT_ENTRY_CLEANUP` 清理本任务旧
  pathing，并清 Cloud 镜像与 `activeGreenLink`。Observer 记录战斗期间最后 pathing
  `observerSeq`，脱战后忽略不晚于该序号的旧事实，exact `COMBAT_EXITED` 后恢复处理新鲜
  Dialog/Tracker。
- 父级最终门禁：Client focused combat tests `20/20`、Cloud focused 新手 tests `93/93`，
  双仓 compile 与 `git diff --check` 均 exit `0`。本轮没有修改修罗/五环业务决策。
- 审核结论：`SOURCE+FOCUSED CONTRACT REVIEW PASSED`。仍需双端重启后 fresh run 验证
  `5s Alt+A` 的真实游戏输入、战斗独占期间无绿链/Dialog 抢占，以及脱战后新鲜动作立即恢复；
  在 fresh run 前不得宣称运行时已通过。

## 2026-07-29 Client-Cloud 生产连通性门禁

- 新增测试编排 `scripts/test-xinshou-connectivity.ps1`，使用三个新鲜 JSON 工件串联真实
  Client Runner、Cloud HTTP ingress/Inbox/Observer/ReadyEvent/`XinshouTask` exact consume、
  Cloud command wire、Client validator/dispatcher；不直接向中间层伪造 PreparedAction。
- 正向合同覆盖 exact `IN_COMBAT -> PRESS_ORDINARY_AUTO_COMBAT -> FOCUSED Alt+A`，并继续
  输入 exact `COMBAT_EXITED` 验证战斗动作槽清空。
- 反向合同把同一 Client wire 的 `taskRunId` 改为旧 run；该迟到事实不得唤醒当前任务或生成
  PreparedAction。脚本逐跳删除和重建工件，缺任一新工件立即失败。
- 门禁结果：脚本 exit `0`；Client 相关 `23/23`、Cloud 相关 `94/94`、双仓 compile 和
  `git diff --check` 均 exit `0`。没有运行 runtime/UI/capture/物理输入。
- 本合同只证明战斗进入/退出这条跨仓接力已连通；绿链和 Dialog 后续改动必须各补同结构的
  生产连通性场景，仍需 fresh runtime 验收实际游戏行为。

## 2026-07-29 普通战斗节奏修订

- 用户重新定稿普通战斗节奏：Runner 发布 exact `IN_COMBAT` 后不再固定等待 `5s`，Cloud
  可立即准备首轮 `PRESS_ORDINARY_AUTO_COMBAT`。
- 每轮普通战斗动作在同一个 frozen exact-window focused transaction 内线性执行两次
  `Alt+A`，两次之间不增加额外等待；成功后的维护周期由 `10s` 改为 `5s`，失败后的
  `1s` 重试不变。
- 捕捉 `Alt+B`、捕捉后恢复双 `Alt+A`（间隔 `1s`）、Runner `Alt+8` 维护及本地
  `IN_COMBAT/COMBAT_EXITED` 判定均不变。
- focused 门禁：Client `22/22`、Cloud `60/60`，双仓 compile 通过。
