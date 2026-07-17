# Cloud LeftTop Combat Maintenance Count Unit - Worker I4

## CLAIMED

- claimedAt: `2026-07-15T01:14:14.9526342-04:00`
- worker: `Internal Count Worker I4`
- role: `implementation-only（不是 reviewer）`
- task: `W-COUNT-LEFT-TOP-COMBAT-MAINTENANCE-1`
- countUnit: `LeftTopStatusSwitchService::handleCombatMaintenance`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- disposition: `NO_CODE_CHANGE_CANDIDATE`
- parentGate: `只有父级源码审查和统一 fresh build 通过后才实际计数`

## Initial Source Fingerprints

| File | SHA-256 at claim |
|---|---|
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | `EAF02F735DA4E1E4B7C5B3CEE442B1A050AE3E00E9AD5910971688CE201F54E3` |
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java` | `69486CED0535BE20428566B18E79B605EB0C65851E853AC8BA128F56E83BF42A` |
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudLeftTopStatusPort.java` | `12BFE5CD9E0D55668722D90A071366C98E75F00217405B79307E47836B28AC6C` |
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java` | `80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D` |

## Disposition

- delivery: `NO_CODE_CHANGE_DELIVERED_FOR_PARENT_REVIEW`
- reason: 当前真实 caller、Cloud business owner、专属 typed port/assembly、DHXY exact-window producer 与单队列输入终态已经完整可达；再造 adapter 或重复 Java 会扩大边界并制造双实现。
- Java changes: `0`
- report changes: 本固定报告一份。

## Real Caller

当前 Cloud `AutoCombatService.java:664-685` 保留稀疏 combat UI maintenance 外层时机，并有两条真实调用：

| Caller | 进入条件 | 调用/状态 |
|---|---|---|
| `AutoCombatService.java:669-672` | 当前窗口是 local-support member，且 `TeamSupportCapability.LEFT_TOP_STATUS` 已打开 | `leftTopStatusSwitchService.handleCombatMaintenance(context, source)` |
| `AutoCombatService.java:678-683` | 不是 local-support member，且不是 pending local-support leader detection | 同一调用 |

member capability 关闭时只记录 deferred；pending leader detection 时同样 deferred。调用分支完成后，既有
`state.lastCombatUiCleanAt = System.currentTimeMillis()` 仍在 `AutoCombatService.java:685` 执行。本任务没有修改 caller、
leader/member safe-window、安全门、稀疏 interval 或 caller state。

## 696 Public/Private Method Map

行为权威：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:LeftTopStatusSwitchService.java`；只核对本
count unit 的完整可达业务图，不把底层 adapter 单独申报计数。

| 696 方法/分支 | 当前 Cloud 对应 | 等价证据 |
|---|---|---|
| public `handleCombatMaintenance(context, source)` | `LeftTopStatusSwitchService.java:112-120` | `resolveTaskCode` -> supported gate -> `checkAndMaybeClose(..., "combat-maintenance:" + safe(source), true)` -> resolved pending consume -> 原结果返回 |
| public `isSupportedTaskCode(taskCode)` | `:126-130` | allowlist 仍仅 `xiuluo_v2` / `wubei` / `wuhuan_v2` |
| private `checkAndMaybeClose(...)` | `:132-158` | 单次 observe；仅 `OPEN && allowClick && point != null` 进入 click；返回 `SwitchActionResult` |
| private `detect(...)` | `:160-193` + typed port | 本地截图/模板职责替换为一次 `LEFT_TOP_STATUS` exact-window fact；视觉状态仍为 `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED` |
| private click boundary | `moveAndClickLeft`, `:206-233` + typed port | 一次 ordered bundle，screen-absolute point，settle `120ms`，click delay `250ms`，无 retry/fallback 重排 |
| private `clearPendingIfResolved(...)` | `:235-243` | 仅 `OPEN && clicked` 或 `CLOSED` 消费 pending；其它结果不清理 |
| private `resolveTaskCode(...)` | `:245-253` | 非空 `requestedTaskCode` 优先，否则 `taskCode` |
| private `safe(...)` | `:255-260` | null/blank -> `unknown`，其它 source 仅作诊断安全化 |
| result/state types | `SwitchState` / `SwitchActionResult`, `:273-296` | 保留 `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED/SKIPPED`、`clicked`、scores 与 OPEN point |

696 常量也保持：ROI window-relative `(8,147,11,19)`；move/click 时序 `120ms/250ms`。视觉判断的
`0.90` threshold、`0.02` margin、同帧 open/closed score 与 OPEN screen-absolute center 继续由已存在 DHXY
`LeftTopStatusSwitchService` mechanics 生产，并经 typed fact 无损返回。

## Complete Reachable Chain

1. `AutoCombatService.java:672/683` 在既有 combat-maintenance 时机和角色安全门内调用 Cloud
   `LeftTopStatusSwitchService.handleCombatMaintenance`。
2. `handleCombatMaintenance` 先以 requested-task 优先解析 task code，只允许三种 supported task，然后以
   `allowClick=true` 调用同类 `checkAndMaybeClose`。
3. `checkAndMaybeClose` 调用专属 `CloudLeftTopStatusPort.observe(context, "left-top-status",
   "probe:" + source, 120000)`；只观察一次。
4. `CloudLeftTopStatusPortAssembly.java:31-32` 调用现有
   `context.getGameClient().readWindowFact(phase, slot, WindowFactKind.LEFT_TOP_STATUS, timeout)`。
5. DHXY `LocalRemoteGameCommandHandler.java:820-823` 在已验证的 `BindingAccess` 上通过
   `windowTaskContextHolder.callWith(access.context(), ...)` 执行
   `leftTopStatusSwitchService.probeLeftTopStatusFact(...)`；该 producer 使用当前精确窗口截图、既有 ROI/
   模板阈值与同帧 OPEN/CLOSED 比较，只对 OPEN 返回 `SCREEN_ABSOLUTE_PX` 点击点。
6. Cloud assembly 将 typed fact 一对一映射为 `LeftTopStatusObservationResult`。CLOSED/UNKNOWN/
   CAPTURE_FAILED/transport terminal 都不会生成 click。
7. 仅 OPEN 且 point 存在时，Cloud `moveAndClickLeft` 调用专属 port click。assembly 在
   `CloudLeftTopStatusPortAssembly.java:74-87` 构造唯一 ordered bundle：
   `MOVE_MOUSE(x,y)` -> `SLEEP(120)` -> `CLICK_LEFT(x,y,250)`，坐标空间固定
   `SCREEN_ABSOLUTE_PX`。
8. DHXY `LocalRemoteGameCommandHandler.executeInputBundleMechanical:2326-2479` 在队列提交前重新校验
   registration、exact binding、deadline 和 pause token，将整包动作一次提交到唯一
   `InputActionQueue.submitRemoteAndWaitDetailed`；只有全部三步完成且 post-execution binding 仍有效，才返回
   `EXECUTED/OK`。
9. typed click terminal 回到 Cloud `SwitchActionResult`；`OPEN+clicked` 或 `CLOSED` 消费 pending，其余状态保留；
   `handleCombatMaintenance` 将 closed result 原样返回给真实 caller。

以上链没有 generic `LOCAL_MACRO`，没有第二输入队列，没有 queue-in-queue，没有 owner/session/TTL/retry，也没有
新增 fallback 或状态机。

## Typed Mechanics And Closed Terminals

### Observation

| DHXY/transport terminal | typed observation | Cloud business result/click |
|---|---|---|
| `OBSERVED + OPEN` | `OPEN` + finite scores + screen point | `OPEN`; 满足 allowClick 后提交一次 bundle |
| `OBSERVED + CLOSED` | `CLOSED` + finite scores，无 point | `CLOSED`, `clicked=false`; 消费 pending |
| `OBSERVED + UNKNOWN` | visual `UNKNOWN` + finite scores，无 point | `UNKNOWN`, `clicked=false`; pending 不变 |
| `OBSERVED + CAPTURE_FAILED` | `CAPTURE_FAILED`，无 point | `CAPTURE_FAILED`, `clicked=false`; pending 不变 |
| `NOT_EXECUTED` | `NOT_EXECUTED` | 映射为 baseline capture-failed/no-click result |
| `STOPPED` | `STOPPED` | `TaskCheckpoint` 确认 stop；否则 fatal，不伪造业务状态 |
| transport `UNKNOWN` | `TRANSPORT_UNKNOWN` | fatal，不降格成视觉 UNKNOWN |

### Click

| Input terminal | Cloud result |
|---|---|
| `EXECUTED` | `clicked=true`，返回 `OPEN` result 并消费 pending |
| `NOT_EXECUTED` | `clicked=false`，保持 `OPEN` 与 pending |
| `STOPPED` | checkpoint 确认 stop；否则 fatal |
| `UNKNOWN` | fatal，避免把可能部分执行误报为成功或未执行 |

## File Table

| File | Ownership | This task |
|---|---|---|
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | 唯一允许业务 Java 写集 | inspected; `NO_CODE_CHANGE` |
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java` | LeftTop 专属 adapter，必要时才允许 | inspected; 已完整，`NO_CODE_CHANGE` |
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudLeftTopStatusPort.java` | 已有 approved typed contract | inspected read-only |
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeftTopStatusObservationResult.java` | 已有 typed result | inspected read-only |
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeftTopStatusClickResult.java` | 已有 typed result | inspected read-only |
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java` | frozen caller | inspected read-only |
| `DHXY/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | frozen exact-window mechanics | inspected read-only |
| `DHXY/src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | frozen handler/input mechanics | inspected read-only |
| 本报告 | 唯一写入 | created/updated |

没有触碰 caller、DHXY、shared 12、generic `LOCAL_MACRO`、其它 Service、host/config、runtime 或测试文件。

## Prior Approved Foundation

父级已在 `2026-07-15-cloud-left-top-status-count-unit-worker-i4.md` 的 Source Review #1 独立确认：

- underlying `checkAndMaybeClose` 真链、专属 port/assembly、existing exact-window fact、单 ordered bundle 与
  DHXY producer 均可达；
- observe 一次且仅 OPEN 可点击；allowlist、pending、`120/250ms`、STOPPED/UNKNOWN 和 `11x19` ROI 保留；
- `P0=0 / P1=0 / P2=0`，`SOURCE APPROVED / COUNT PENDING BUILD`。

本 count unit 不重复申报该底层单位，只证明另一个真实 public caller
`handleCombatMaintenance` 已通过 approved foundation 闭合到 terminal/result/state。

## Scoped Verification

- `rg`/scoped reads 确认 Cloud `AutoCombatService` 真实调用位于 `672/683`，两条均进入同一 public method。
- scoped reads 确认当前 Cloud public/private map 与 696 mirror 的本单位控制流一致。
- scoped reads 确认 assembly action 顺序仅为 `MOVE_MOUSE -> SLEEP -> CLICK_LEFT`，且整包只走 DHXY 单队列。
- scoped reads 确认 DHXY handler 的 exact-window `callWith`、提交前 revalidation、完整步骤判定和
  `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` closed terminal。
- claim/final source fingerprints 一致；共享 writer 期间未改动上述 Java。
- 按任务约束未运行 Maven、test、runtime、application、server、Task、poller、UI、capture、input 或 Git。

## Business Difference And Count Gate

无已批准业务差异；按 `696a12b0` 等价迁移。

- worker 只申报 `countDelta=+1`，不自行增加计数。
- 当前状态：`NO_CODE_CHANGE_DELIVERED_FOR_PARENT_REVIEW / COUNT PENDING SOURCE REVIEW AND FRESH BUILD`。
- 只有父级源码审查确认本真实 caller 全链无 P0/P1/P2，并在共享 writers 稳定后完成统一 fresh build，才实际计入
  `LeftTopStatusSwitchService::handleCombatMaintenance +1`。

## Parent Source Review #1 - 2026-07-15T01:29:00-04:00

父级独立读取 Cloud `AutoCombatService:665-685`、`LeftTopStatusSwitchService:112-243`、专属 assembly，及 DHXY
`LEFT_TOP_STATUS` handler/producer/input queue。两个真实 combat-maintenance caller、supported-task gate、单次 observe、
OPEN-only ordered move/sleep/click、CLOSED/成功点击才清 pending、STOPPED/UNKNOWN fail-closed 均闭合；与
`696a12b0` 的判断、120/250ms、fallback/state 等价。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。** fresh Cloud package 通过同轮才 `+1`。
