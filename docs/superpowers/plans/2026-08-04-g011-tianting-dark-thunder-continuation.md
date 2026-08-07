# G011 天庭暗雷战后连续巡逻

## 状态

- 状态：`SOURCE+TEST REVIEW PASSED / P0-P1-P2=0-0-0 / 待 fresh runtime`。
- 建卡日期：`2026-08-04`。
- Client 工作树：`D:\mavenProject\DHXY-cr271`，分支/HEAD：
  `thin-client-design` / `2f083c14152106ba6ad418a0c29e3e0e2148e14a`。
- Cloud 工作树：`D:\mavenProject\dhxy-cloud-brain`，分支/HEAD：
  `navigation-migration` / `363d0e3fae73c0c55f4920f6f1c61338a0458d73`。
- `D:\mavenProject\DHXY` 是保护基线，本卡不得修改。

## 用户确认的业务规则

当前一场战斗满足以下全部条件时，脱战后不得点击 Tracker 绿色链接：

1. 本场战斗由天庭暗雷左右巡逻触发；
2. 脱战后读取到 fresh 天庭 Tracker；
3. 脱战后的天庭任务内容与进战前保存的内容指纹完全相同。

三项同时成立，证明当前暗雷小任务尚未完成。Cloud 必须直接让现有暗雷巡逻继续：

- 不点击绿色链接；
- 不登记新的绿链 pathing intent；
- 不重新检查飞行器；
- 不重新解析或初始化巡逻点；
- 复用进战前已经算好的左右巡逻点，从下一侧继续右键移动遇怪。

若 Tracker 内容发生变化，则说明原小任务已经推进，继续走现有 Tracker 分析和绿色链接流程。若 fresh
Tracker 尚未得到，不得拿旧截图猜测内容相同，也不得提前点击绿链，只能等待 fresh 事实。

## 现状与根因

现有 `G005` 和《天庭任务流程大MD》第 7 节把所有“脱战后 title 仍存在”的情况统一写成“继续点击绿色
链接”。Cloud 生产代码同样没有暗雷连续分支：

- `TiantingTask.runPostCombat()` 在战后清除 `legStopped`，随后等待普通 fresh fact；
- `TiantingSubtaskDecision` 看到 fresh Tracker 绿链后只能选择 `CLICK_TRACKER_LINK`；
- `CLICK_TRACKER_LINK` 点击完成后才重新读取 `darkThunder`，因此上一场暗雷来源和进战前 Tracker 指纹没有
  参与战后决策。

这不是图片匹配错误，而是缺少已经约定的业务分支。

## 现场证据

窗口：`hwnd-F99187E`。

- `2026-08-04 02:45:36.062`：当前 leg 已进入 `tianting:dark-thunder`。
- `2026-08-04 02:46:16.389`：Client Runner 确认 `COMBAT_EXITED`，`generation=4`。
- `2026-08-04 02:46:20.897`：Cloud 又建立 `tianting:tracker-green-click:advance`。
- `2026-08-04 02:46:21.142`：Client 在 `(385,317)` 实际执行绿链左键点击。
- 后续重新进入暗雷分支并再次巡逻，证明中间绿链导航属于多余动作。

## 实施边界

### Cloud

1. 在暗雷巡逻真正进入战斗前，保存本次战斗来源和进战前 Tracker 内容指纹；不能只保存一个会被后续
   Tracker 读取覆盖的 `darkThunder=true`。
2. `COMBAT_EXITED` 后仍先保留既有自动战斗恢复、队员归队和引妖处理；这些不是本卡删除对象。
3. 战后取得 fresh Tracker 后，在普通 `CLICK_TRACKER_LINK` 决策之前判断暗雷连续条件：
   `combatSource == DARK_THUNDER_PATROL && freshFingerprint == preCombatFingerprint`。
4. 命中后直接恢复现有 patrol 游标与左右点，不走飞行检测、绿链点击、pathing intent 或 dialog interest。
5. 未命中时完整保留当前普通 Tracker/绿链业务，不把所有暗雷标签都粗暴跳过绿链。
6. 任务完成、暂停、停止、重新接任务、Tracker 内容变化或 run identity 变化时清除该连续性证据，禁止跨
   小任务或跨 run 复用。

### Client

- Runner 继续只负责上报 exact `IN_COMBAT` / `COMBAT_EXITED`、fresh Tracker 事实和执行 Cloud 动作。
- 不在 Client 新增“暗雷战后是否点绿链”的业务判断。
- 不改暗雷模板、Tracker ROI、内容指纹算法、巡逻坐标、鼠标输入实现或战斗检测算法。

## 连通性测试与验收

必须覆盖真实决策链，而不只是孤立 helper：

1. 暗雷巡逻进战 + 脱战后 fresh Tracker 指纹相同：下一动作是继续 patrol；绿链点击 `0` 次、飞行检查
   `0` 次、新 pathing intent `0` 个。
2. 暗雷巡逻进战 + 脱战后 Tracker 指纹变化：走普通 Tracker/绿链推进。
3. 非暗雷来源进战 + 战后 Tracker 标记为暗雷：仍按普通流程先点击该小任务绿链，不能错误继承上一腿。
4. 暗雷来源进战 + 战后没有 fresh Tracker：park 等待，不得使用 stale 指纹直接巡逻或点绿链。
5. 暂停/停止/新 run 后迟到的 `COMBAT_EXITED` 或 Tracker 事实不得恢复旧巡逻。
6. fresh runtime 日志必须呈现：`COMBAT_EXITED -> dark-thunder-continuation -> patrol click`，两者之间不得
   出现 `tracker-green-click`、飞行检测或新 pathing intent。

## 审核门

- 父级逐文件审核生产调用路径、状态清理和测试，不接受只在决策 helper 中通过但生产未接线。
- Client/Cloud 相关 named tests 和双仓 compile 均通过。
- `P0/P1/P2=0/0/0` 后才可标为 `SOURCE+TEST REVIEW PASSED`；实机通过前仍保留 fresh-runtime gate。

## Repair #1 交付（2026-08-06）

- fresh 失败：`hwnd-140E163C` 于 `08:48:21.528 EDT` 从暗雷战斗脱战，同一 task-box 继续路径仍在
  `08:48:27.116 EDT` 发送 `Alt+U`。
- 根因：RUN_DARK_THUNDER 决策与方法调用之间若发生入战竞态，旧代码会 full reset 暗雷 attempt；即使没有
  reset，`UNKNOWN` fallback 也缺少“真实脱战必为落地”的显式收敛。
- 修复：入战竞态只结束本次巡逻调用；同图暗雷连续性成立时锁定 `NOT_FLYING`，保留左右 click cursor、巡逻点
  和 arrival-dialog 已清状态。异图、新绿链、新 round 仍走原 full reset。
- 用户校准：巡逻间隔由 `1000ms` 改为 `700ms`，右键按压 `100ms` 不变。
- 静态门：Cloud compile PASS；`TiantingDarkThunderPlanTest` `13/13`、
  `TiantingSubtaskLoopContractTest` `27/27`，合计 `40/40 PASS`；主线程审核 `P0/P1/P2=0/0/0`。
- 未关闭：仍需 fresh 证明同图 `COMBAT_EXITED -> patrol` 之间飞行检查、绿链点击和新 pathing intent 均为
  `0`，并核对右键间隔约 `700ms`。
