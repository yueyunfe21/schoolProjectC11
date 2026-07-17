# Internal I7 - AutoCombatPanelService::ensurePanelVisible

## CLAIMED

- task: `W-COUNT-AUTO-COMBAT-PANEL-ENSURE-VISIBLE-1`
- claimedAt: `2026-07-15T01:50:48-04:00`
- countUnit: `AutoCombatPanelService::ensurePanelVisible`
- countDelta: `+1`（仅申报；仍待父级源码审查与统一 fresh build 后记账）
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- 唯一 Java 写集: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
- 唯一报告写集: 本文件
- 冻结: 既有 `AUTO_COMBAT_PANEL` fact/codec/handler、DHXY mechanics、generic shared、caller、其它 Service、align、rounds、window-origin。

## Baseline Gate

- 已读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 适用基线门、whole-Service 计划、迁移矩阵、任务排班报告、相关 Cloud/DHXY 源码和
  `migration-baseline/696a12b0` 的 `AutoCombatPanelService`/`AutoCombatService`。
- 因任务明确禁止 Git，未执行 `git status/diff/show`；业务对照使用仓内只读
  `migration-baseline/696a12b0` 镜像与 active caller/source。
- `696a12b0` 可见性顺序固定为：首次观察 -> miss 时唯一 `Alt+8 + waitAfterOpenMs` -> 二次观察 ->
  二次 miss 记 missing watchdog；二次 found 后依次 refresh state、clear missing、日志、返回坐标。
- 无已批准业务差异；按基线等价迁移。

## Implementation

唯一修改 Cloud `AutoCombatPanelService.java` 的现有 `ensurePanelVisible -> ensurePanelMatchVisible` 路径：

1. 从既有 `TaskExecutionContextHolder` 取得当前 exact task-run context；没有默认/global window fallback。
2. 原首次 `findAutoCombatBox()` 改为 `WindowFactKind.AUTO_COMBAT_PANEL` typed observation，
   `FOUND` 原样重建 screen-absolute `AutoCombatPanelMatch`；`NOT_FOUND/CAPTURE_FAILED` 保持 baseline miss。
3. 首次 miss 后仅发送一个 ordered `InputBundle`：`PRESS_ALT_8`、`SLEEP(waitAfterOpenMs)`；没有拆包、retry、
   owner、TTL、额外读取或新 wrapper。
4. 输入 `EXECUTED` 后执行第二次独立 `AUTO_COMBAT_PANEL` typed observation；不是只迁首次观察。
5. 二次 found 后原 `recordAutoCombatRefresh -> clearAutoPanelMissing -> visible log -> return` 顺序未变；
   输入未执行和二次 miss 继续进入原 `recordAutoPanelMissing` 分支并返回 null。
6. `STOPPED` 和读取中断通过 exact context 的 `TaskCheckpoint`/transition 退出；错误 terminal 显式拒绝，
   不伪装成成功 observation 或输入。
7. 给 public `ensurePanelVisible` 补充 source、毫秒 wait 和 screen-absolute `Point/null` JavaDoc。

## Real Caller / Typed Terminal

真实闭合链：

`AutoCombatService::maybeHandleCombatEnter`
-> `ensurePanelVisible(source + ":combat-enter", 500)`
-> `ensurePanelMatchVisible`
-> initial `AUTO_COMBAT_PANEL`
-> miss 时唯一 ordered `PRESS_ALT_8,SLEEP(500)`
-> after-open `AUTO_COMBAT_PANEL`
-> `Point/null`
-> 原 caller void continuation。

Typed DHXY terminal 复用链：

`WindowFactKind.AUTO_COMBAT_PANEL`
-> DHXY `LocalRemoteGameCommandHandler`
-> exact bound `AutoCombatPanelService::probeAutoCombatPanelFact`
-> `RemoteAutoCombatPanelFact(FOUND/NOT_FOUND/CAPTURE_FAILED)`
-> Cloud `WindowFact.AutoCombatPanelFact`
-> `AutoCombatPanelMatch/null`。

输入继续复用既有 remote `EXECUTE_INPUT_BUNDLE`、DHXY 单输入队列和 closed
`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` terminal；没有修改 producer、handler、codec 或 input mechanics。

## Scoped Diff

- Java：只新增 typed remote/context imports、一个 transport timeout 常量、一个构造注入字段、public JavaDoc，
  并原地替换 `ensurePanelMatchVisible` 的两次视觉调用与一次开面板输入调用。
- 报告：只新增本 I7 报告。
- `findAutoCombatBox()` 仍供冻结的 align 路径使用；未修改其实现，rounds 路径也未触碰。
- 未修改 `alignPanelIfNeeded`、rounds OCR/估算、window origin、missing watchdog、refresh methods、runtime state、
  caller、DHXY 或 shared remote 文件。
- 未执行 Maven、test、runtime、application/server/host 或任何 Git 命令/写操作，符合本任务禁令。

## Self-Review

- 角色确认：I7 implementation worker，不是 reviewer；本节不是 `Approved` 结论。
- 两次 observation 都使用当前 exact task context 和现有 `AUTO_COMBAT_PANEL` typed fact。
- 唯一输入 bundle 顺序为 `PRESS_ALT_8 -> SLEEP(waitAfterOpenMs)`，没有第二次输入或隐式 resend。
- `FOUND` 坐标保持 `SCREEN_ABSOLUTE_PX`；没有 rounds、align 或 window-origin 计算。
- baseline 的 first-found、input-failed、second-miss、second-found 四条分支及 state/log 顺序均保留。
- 自审未发现需写集外前置；当前状态：`IMPLEMENTED / PARENT REVIEW + UNIFIED FRESH BUILD PENDING`。

## Parent Source Review #1 - 2026-07-15T01:58:00-04:00

结论：**BLOCKED，P0=0/P1=1/P2=0；countDelta=0，交原 I7 返修。** 主链的两次
`AUTO_COMBAT_PANEL` observation、唯一 `PRESS_ALT_8 + SLEEP(waitAfterOpenMs)` ordered bundle、FOUND 坐标、
missing watchdog 与 refresh/clear/log 顺序均可保留；阻断仅为 transport terminal folding。

- `AutoCombatPanelService.java:139-161`：首次 fact 只显式处理 `OBSERVED/STOPPED/EXECUTED`，使
  `NOT_EXECUTED/UNKNOWN` 都落入 `panelMatch == null`，随后在 `:171-184` 继续发送 `Alt+8`。未执行或未知的
  observation 不是 `NOT_FOUND/CAPTURE_FAILED` 业务负事实，不能触发物理输入。
- `AutoCombatPanelService.java:218-244`：第二次 fact 同样把 `NOT_EXECUTED/UNKNOWN` 折成
  `not-found-after-alt8` 并写 missing watchdog，制造未观察到的负事实。
- `AutoCombatPanelService.java:185-199`：bundle 的 `UNKNOWN` 与 `NOT_EXECUTED` 一起走 `sent=false`；其中
  `NOT_EXECUTED` 可保持基线 submit-false 的 input-failed 分支，但 `UNKNOWN` 必须 fail closed，不能记成普通面板缺失。

精确返修条件：只改本文件现有 `ensurePanelMatchVisible` 终态分支；两次 fact 的 `UNKNOWN` 必须 fatal，
`NOT_EXECUTED` 必须不发送后续输入、不写 panel-missing 负状态并返回 null（或同等 fail-closed）；bundle
`UNKNOWN` 必须 fatal。保留 `STOPPED` checkpoint、`EXECUTED/OBSERVED` 类型拒绝、现有 696 业务分支和唯一 bundle；
不得新增 wrapper/retry/TTL/第二次输入，也不得触碰 frozen align/rounds/caller/DHXY/shared 文件。

## Repair #1 - 2026-07-15T01:57:29-04:00

已按 Parent Source Review #1 只修改 Cloud `AutoCombatPanelService.java` 现有
`ensurePanelMatchVisible` 的 transport terminal 矩阵：

- 首次 fact `UNKNOWN` 立即抛 `TaskFatalException`；`NOT_EXECUTED` 立即返回 null，发生在 baseline
  `not found -> Alt+8` 日志与 bundle 之前，因此不会发送物理输入，也不会写 panel-missing 状态。
- 第二次 fact `UNKNOWN` 立即抛 `TaskFatalException`；`NOT_EXECUTED` 立即返回 null，发生在
  `still not found after Alt+8` 日志与 `recordAutoPanelMissing` 之前，因此不把未执行 observation 写成业务负事实。
- bundle `UNKNOWN` 立即抛 `TaskFatalException`；`NOT_EXECUTED` 仍由原 `sent=false -> input-failed ->
  recordAutoPanelMissing -> null` 分支处理，保持 `696a12b0` 的 submit-false 业务效果。
- 两次 fact 的 `STOPPED -> TaskCheckpoint -> contradictory fatal`、读取中断 transition、fact `EXECUTED`
  拒绝、bundle `OBSERVED` 拒绝均未改。
- `OBSERVED FOUND`、`OBSERVED NOT_FOUND/CAPTURE_FAILED`、唯一 `Alt+8 + waitAfterOpenMs`、首次 found 的
  clear/log/return、二次 miss 的 log/watchdog/null，以及二次 found 的
  `recordAutoCombatRefresh -> clearAutoPanelMissing -> log -> return` 顺序均未改。

### Repair #1 精确 Diff

- 初次 observation 分支新增两项：`UNKNOWN -> fatal`、`NOT_EXECUTED -> null`。
- open bundle 分支新增一项：`UNKNOWN -> fatal`。
- 二次 observation 分支新增两项：`UNKNOWN -> fatal`、`NOT_EXECUTED -> null`。
- 没有新增方法、wrapper、retry、TTL、owner、输入、observation 或状态字段；没有修改 align、rounds、caller、
  DHXY、shared remote 或其它文件。

### Repair #1 Self-QA

- 角色仍为 implementation worker，不是 reviewer；本节不构成 `Approved`。
- 静态方法范围核对：`readWindowFact=2`、`executeInputBundle=1`、`PRESS_ALT_8=1`、`SLEEP=1`、
  `findAutoCombatBox=0`。
- 终态核对：`UNKNOWN fatal=3`；fact 显式 `NOT_EXECUTED -> null=2`；原
  `recordAutoPanelMissing=2`，没有新增负状态写入点。
- 顺序核对：首次 `NOT_EXECUTED` 位于 bundle 之前；二次 `NOT_EXECUTED` 位于 second-miss 日志/watchdog 之前；
  bundle `NOT_EXECUTED` 仍到达原 input-failed 分支。
- 按任务禁令未运行 Maven、test、runtime/application/server/host 或 Git。
- 当前状态：`REPAIR #1 DELIVERED / PARENT RE-REVIEW + UNIFIED FRESH BUILD PENDING`；`countDelta` 仍未记账。

## Parent Source Review #2 - 2026-07-15T02:18:00-04:00

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。**

- 父级独立复核 `AutoCombatPanelService.java:117-263`，并读取非绑定 preflight 报告；初次 fact 的
  `UNKNOWN -> fatal`、`NOT_EXECUTED -> null` 均位于 Alt+8 之前，未制造业务 miss 或物理输入。
- bundle `UNKNOWN -> fatal`，`NOT_EXECUTED` 保持 baseline `input-failed -> recordAutoPanelMissing -> null`；仅
  `EXECUTED` 进入第二次 fact read。
- 第二次 fact `UNKNOWN -> fatal`、`NOT_EXECUTED -> null` 位于 second-miss 日志/watchdog 之前；只有
  `OBSERVED NOT_FOUND/CAPTURE_FAILED` 写 missing，FOUND 保持 refresh/clear/log/return 顺序。
- 静态范围仍为 `readWindowFact=2`、`executeInputBundle=1`、`PRESS_ALT_8=1`、`SLEEP=1`；无新增 retry、TTL、
  第二输入、额外 observation 或 state。
- `countUnit=AutoCombatPanelService::ensurePanelVisible`、`countDelta=+1` 仍须等待所有 writer 稳定后的 fresh Cloud
  package；双门前 ledger 保持 `189/407`。

无已批准业务差异；按 `696a12b0` 基线等价迁移。
