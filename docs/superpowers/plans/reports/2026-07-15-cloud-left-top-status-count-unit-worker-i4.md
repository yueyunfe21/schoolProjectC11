# Cloud LeftTop Status Count Unit Worker I4

## CLAIMED

- task: `W-COUNT-LEFT-TOP-CHECK-AND-CLOSE-1`
- role: Internal Count Worker I4, implementation only; not a reviewer
- claimedAt: `2026-07-15T00:41:35.0561846-04:00`
- countUnit: `LeftTopStatusSwitchService::checkAndMaybeClose`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- baseline Service blob: `a46fde69e7d11bca315b75600fd737ef7f924912`
- pre-edit Cloud Service SHA-256: `615AC8B4D7FC90D58F99B0396824F02EF5984639FDB3150815B27BA504669FE6`
- branches: DHXY `thin-client-design` at `0114604e1ff5f15491d2910959c45252e893d04f`; Cloud `navigation-migration` at `3b988caa010254973e03342272e6d1d6a9685b01`
- shared worktree: both repositories are heavily dirty/untracked; this worker will not revert, overwrite, clean, stage, commit, or run Maven/runtime/tests
- gate: only parent source review plus the parent's fresh applicable builds may apply the count delta

## Baseline Method Map

| `696a12b0` method/type | Count-unit disposition |
|---|---|
| `handleLeaderStartup` | Preserve the supported-task gate, click-enabled startup check, and resolved-pending consume. |
| `probeMemberStartup` | Preserve no-click observation; OPEN marks pending, CLOSED clears it, UNKNOWN/CAPTURE_FAILED leave it unchanged. |
| `consumeFollowerSafeWindow` | Preserve the capability-owned member safe window; OPEN+clicked and CLOSED consume pending, all unresolved paths retain an existing pending marker. |
| `handleCombatMaintenance` | Preserve requested-task-code priority and sparse combat-maintenance entry into the same close gate. |
| `isSupportedTaskCode` | Preserve only `xiuluo_v2`, `wubei`, and `wuhuan_v2`, case-insensitively. |
| `checkAndMaybeClose` | Count unit: observe once; click only for OPEN + `allowClick` + non-null point; preserve logs, result, and no-click branches. |
| `detect` / `scoreTemplate` / `resolveState` | DHXY retains exact-window capture and same-frame OPEN/CLOSED template mechanics; Cloud consumes one closed typed observation. |
| `clearPendingIfResolved` / `resolveTaskCode` | Preserve pending completion and requested-code-first behavior without new state, retry, TTL, or fallback. |
| `safe` / `formatRect` / `formatPoint` | Preserve diagnostics; remote observation has no fabricated rect/path. |
| `SwitchState` / `SwitchActionResult` / `DetectionResult` / `TemplateScore` | Preserve baseline business states and returned fields; transport terminals remain distinct from visual UNKNOWN. |

## Pre-Edit Boundary Evidence

- Real Cloud caller: `AutoCombatService.maybeRunCombatMaintenance` reaches
  `handleCombatMaintenance(context, source)` under the unchanged leader/member capability and pending-leader gates.
- Real member safe-window caller: `AutoBattleTask` reaches `consumeFollowerSafeWindow` only while the local-team
  `LEFT_TOP_STATUS` capability is open.
- Existing approved observation terminal: Cloud `WindowFactKind.LEFT_TOP_STATUS` maps through the DHXY handler's
  exact registration/binding access to `LeftTopStatusSwitchService.probeLeftTopStatusFact`.
- Existing approved input terminal: one `SCREEN_ABSOLUTE_PX` bundle reaches the DHXY single `InputActionQueue` after
  fresh registration/binding/safety fences.
- The Cloud Service pre-edit SHA matches the 2026-07-14 parent-approved source exactly. This worker will preserve
  its STOPPED/UNKNOWN/interrupt handling and honest `rect=null` diagnostics while adding only a LeftTop-specific
  typed port/assembly.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 - SOURCE APPROVED / COUNT PENDING BUILD - 2026-07-15T00:54:19-04:00

父级独立核对真实 caller（`AutoCombatService` combat maintenance 与 `AutoBattleTask` member safe window）、
Cloud 专属 port/assembly、existing `LEFT_TOP_STATUS` exact-window fact、单个 ordered input bundle 及 DHXY producer。
`checkAndMaybeClose` 仍只 observe 一次，仅 `OPEN && allowClick && point != null` 点击；supported task allowlist、
pending consume/retain、120/250ms 顺序、STOPPED/UNKNOWN fail-closed 与 `696a12b0` 的 `11 x 19` ROI 均保留。
assembly 位于 `com.bot.dhxy.service.lefttop`，在现有 Cloud component scan 内可达。

结论：**P0=0 / P1=0 / P2=0，SOURCE APPROVED。** `countUnit=LeftTopStatusSwitchService::checkAndMaybeClose`
进入统一 fresh 双构建队列；构建前不加计数。I4 下一张计数单另发，不把本单拆成零计数 follow-up。

## Implementation

本次把 parent-approved 的 Cloud 直连 typed fact/input 适配收口为一个 LeftTop 专属端口，
并由 `checkAndMaybeClose` 的真实调用路径直接使用该端口。业务 gate、状态与 pending 所有权仍在
Cloud `LeftTopStatusSwitchService`；装配层只投影已经存在的 `LEFT_TOP_STATUS` observation 和一个
ordered `INPUT_BUNDLE`，没有新增 owner、session、ledger、TTL、retry、fallback 或第二套业务判断。

DHXY 侧复用已有 source-approved fact producer 与 input handler。唯一 mechanics 修正是把当前工作树中
未经本 CR 批准的 `16 x 29` ROI 恢复为行为权威 `696a12b0` 的 `11 x 19`；已有
`probeLeftTopStatusFact`、exact-window `WindowTaskContextHolder.callWith(...)` handler 和整 bundle
`InputActionQueue` 路径均保护未改。

## File Table

| Repository / file | Action | Count-unit role |
|---|---|---|
| Cloud `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | Modify | `checkAndMaybeClose` 保持一次 observation、仅 OPEN 可点；`detect`/click 改依赖 LeftTop 专属 port，保留已批准 terminal 分流。 |
| Cloud `src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java` | New | Spring 扫描可达的专属 assembly；一对一连接现有 `LEFT_TOP_STATUS` fact 与单个 ordered input bundle。 |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudLeftTopStatusPort.java` | New | exact-window observation/click 专属 port；不暴露 generic macro。 |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeftTopStatusObservationResult.java` | New | 闭合 visual/mechanical/transport observation；visual UNKNOWN 与 transport UNKNOWN 分离。 |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LeftTopStatusClickResult.java` | New | 闭合 input terminal：EXECUTED / NOT_EXECUTED / STOPPED / UNKNOWN。 |
| DHXY `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | Modify | 恢复 `696a12b0` ROI `8,147,11,19`；已有 typed fact producer 与模板决策保持。 |
| `docs/superpowers/plans/reports/2026-07-15-cloud-left-top-status-count-unit-worker-i4.md` | New | 本 worker 唯一报告。 |

未修改但属于完整可达链的既有文件：Cloud `AutoCombatService.java`、`AutoBattleTask.java`，DHXY
`LocalRemoteGameCommandHandler.java`、`RemoteLeftTopStatusFact.java` 及 input mapper/queue。它们均不在本 worker
写集内，只作源码可达性复核。

## Complete Reachable Chain

1. **真实 caller**：Cloud `AutoCombatService.maybeRunCombatMaintenance` 在 sparse periodic maintenance 内调用
   `handleCombatMaintenance(context, source)`。local-support member 只有在 `LEFT_TOP_STATUS` capability open 时进入；
   pending leader detection 仍 defer；其它 leader/standalone 路径保持原入口。
2. **member safe window**：Cloud `AutoBattleTask.maybeRunIdleMaintenance` 仅在 local-support follower mode 且
   capability open 时调用 `consumeFollowerSafeWindow`。OPEN pending 的消费/保留条件未移入 assembly。
3. **Cloud business owner**：`handleCombatMaintenance` 保持 requested-task-first 与
   `xiuluo_v2/wubei/wuhuan_v2` allowlist，进入同一个 `checkAndMaybeClose`。该 count unit 每次只 observe 一次，
   只有 `OPEN && allowClick && point != null` 才提交一次 close bundle；CLOSED/UNKNOWN/CAPTURE_FAILED 不点。
4. **typed observation**：`CloudLeftTopStatusPortAssembly.observe` 通过当前 `TaskExecutionContext.gameClient`
   请求 `WindowFactKind.LEFT_TOP_STATUS`。DHXY handler 先取得并复验 exact registration/native binding，再以
   `WindowTaskContextHolder.callWith(access.context(), ...)` 调用 `probeLeftTopStatusFact`；producer 在绑定窗口的
   window-scoped capture 上用同帧 open/closed 模板输出 OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED、两分数及仅 OPEN
   可携带的 `SCREEN_ABSOLUTE_PX` 点。
5. **typed input**：OPEN 分支经专属 `click` 只构造一个 bundle：
   `MOVE_MOUSE(x,y) -> SLEEP(120ms) -> CLICK_LEFT(x,y,delayMs=250)`，phase=`left-top-status`、
   slot=`close-click`、coordinate space=`SCREEN_ABSOLUTE_PX`、timeout=`120000ms`。DHXY handler 在 queue
   submission 前再次复验 registration/binding/safety fence，然后整 bundle 一次交给单一 `InputActionQueue`。
6. **closed result**：fact 与 input execution state 都在专属 assembly 映射成闭合 result，再由 Cloud Service
   恢复原 `SwitchActionResult`、pending 与 fatal/checkpoint 语义；没有结果字符串解析或隐式重试。

## Terminal Preservation

| Boundary terminal | Cloud Service behavior |
|---|---|
| Fact OBSERVED + OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED | 状态、分数、OPEN 点一对一保留；仅 OPEN 满足原 click gate。 |
| Fact NOT_EXECUTED | 保持 parent-approved `CAPTURE_FAILED`，不点击。 |
| Fact STOPPED | 先 `TaskCheckpoint.throwIfStopRequested`；若未确认 stop 则 fatal。 |
| Fact transport UNKNOWN / impossible EXECUTED | fatal，不降格为 visual UNKNOWN 或普通 miss。 |
| Fact read interrupted | 恢复 interrupt flag 后 fatal。 |
| Input EXECUTED / NOT_EXECUTED | 分别返回 `clicked=true/false`。 |
| Input STOPPED | 先 checkpoint；未确认 stop 则 fatal。 |
| Input UNKNOWN / impossible OBSERVED | fatal，不继续推进 pending。 |

## Scoped Verification

- Baseline re-read: `git show 696a12b0:.../LeftTopStatusSwitchService.java`; blob
  `a46fde69e7d11bca315b75600fd737ef7f924912`，method map 与常量逐项记录在上文。
- Earlier source-approved Cloud Service pre-edit SHA-256：
  `615AC8B4D7FC90D58F99B0396824F02EF5984639FDB3150815B27BA504669FE6`；其
  STOPPED/UNKNOWN/interrupt、`rect=null`、pending、allowlist、delay 与日志语义保留。
- Cloud scoped text check：五个目标 Java 文件无 trailing whitespace、无 conflict marker；专属写集中
  `LOCAL_MACRO` 及 Npc/Dialog/Navigation/BattleRadar/PlayerState/CommonBox/TeamReturn/TaskMaintenance/
  SummonSkill 引用为 0。
- Cloud wiring grep：一个 `CloudLeftTopStatusPortAssembly implements CloudLeftTopStatusPort`；Service 的
  `observe`/`click` 各一个调用点；bundle token 顺序为 MOVE_MOUSE、SLEEP、CLICK_LEFT，且只用
  SCREEN_ABSOLUTE_PX。
- DHXY scoped `git diff --check`：exit 0。该文件相对当前 Git index 的 earlier typed producer/import 保留；
  本 worker 只把工作树 ROI `16 x 29` 恢复为 `11 x 19`。
- 当前 SHA-256：Cloud Service `EAF02F735DA4E1E4B7C5B3CEE442B1A050AE3E00E9AD5910971688CE201F54E3`；
  Cloud assembly `69486CED0535BE20428566B18E79B605EB0C65851E853AC8BA128F56E83BF42A`；
  port `12BFE5CD9E0D55668722D90A071366C98E75F00217405B79307E47836B28AC6C`；
  observation result `DF9147ECEC0D6F9260E2D727BBFB1380832A17829BAF9E2EA421C1C581D3C796`；
  click result `8167404083DA448570965D4D2E36E664E90AE02705D254F5840D59C7D17668A5`；
  DHXY Service `6AC4CB59D82126BE606B519371F819166FFD9A1D3F063F477940D701354B977A`。
- 按任务约束未运行 Maven、测试、runtime/application/server/Task/poller/UI/capture/input；未执行任何 Git mutation。

## Handoff Gate

- Worker I4 实现状态：`CLAIMED_DELIVERED_FOR_PARENT_REVIEW`。
- 申报：`countUnit=LeftTopStatusSwitchService::checkAndMaybeClose`，`countDelta=+1`。
- 当前实际计数：**尚未增加**。只有父级源码审查确认完整链无 P0/P1/P2，并完成 DHXY 与 Cloud 的 fresh
  applicable build 后，才能应用 `+1`。
- 待父级统一 build；本 worker 不宣称 compile/package 通过，也不请求 fresh runtime。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**
