# TURN-34A AutoCombatService 前置源码证据

日期：2026-07-16  
角色：CR271 非绑定 readiness helper  
用途：供父级冻结 TURN-34A brief；本文只陈列源码证据、边界、验收条件和风险，不作实现或最终裁决。

## 1. 审计边界与证据锚点

本次已按只读方式核对：

- `D:/mavenProject/DHXY/AGENTS.md`
- `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`
- `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271
- `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节及 TURN-34A
- `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- `D:/mavenProject/DHXY/docs/业务逻辑.md` 中 auto-combat、五倍、修罗基线
- DHXY 与 `D:/mavenProject/dhxy-cloud-brain` 当前生产源码、真实 production caller
- TURN-19/20/21/23/24A 当前生产实现及相关固定报告
- 基线提交 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`

审计快照：

| 项 | 证据 |
|---|---|
| DHXY | 分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`，无 upstream |
| Cloud | 分支 `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`，无 upstream |
| 工作树 | 两仓均有既存 dirty/untracked；本 helper 未清理、覆盖或暂存任何内容 |
| 基线提交 | `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，提交时间 2026-06-30，主题 `chore: remove obsolete debug tooling` |
| 基线 AutoCombat blob | `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a` |
| 当前 Cloud AutoCombat SHA-256 | `80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D` |
| 当前 DHXY AutoCombat SHA-256 | `97786B80702E8CF89B40897F141E3746EC25E1AC57767645EDFD055B9A90BB52` |

当前 Cloud `AutoCombatService` 相对基线的业务可见差异，仅是把 `UICleanerService` 换成 `CloudUiCleanerPort`，并给两处关闭动作补入 phase/slot 参数。当前 DHXY 文件相对基线已有约 465 行新增、85 行删除，包含 CR243/252 后增 API 与决策；这些本地后增行为不能自动成为 TURN-34A 迁移基线。

权威计划给出的依赖集合是 `TURN-19 + TURN-20 + TURN-21 + TURN-23 + TURN-24A + TURN-33`。其中 TURN-33 的源码形态尚未稳定，因此当前证据只够父级冻结 brief，不能据此派发 TURN-34A 实现。

## 2. 基线 public surface 与真实 caller

基线及当前 Cloud 的 public surface 一致。下表以当前 Cloud 行号定位，并补充真实 production caller、调用时机和返回消费。

| Public API | 当前 Cloud 定位 | 真实 caller 与返回消费 |
|---|---:|---|
| `TickResult { NONE, IN_COMBAT, EXIT_RECOVERED }` | 53 | `AutoBattleTask` 把任意非 `NONE` 映射为本轮 sleep/continue；`FiveRingTaskV2` 只把 `IN_COMBAT` 继续视为 shared wait，其余进入 `SYNC_TASK_PANEL`；`WubeiTask`、`XiuluoTaskV2` 按下述 phase 分支消费三值。 |
| `PostCombatRecoveryPolicy { FULL_RECOVERY, FULL_RECOVERY_WITH_LEADER_INCENSE, FAST_EXPECTED_EXIT }` | 62 | `WubeiTask` 和 `XiuluoTaskV2` 显式选择；boolean overload 只做基线兼容映射。 |
| `initializeForCurrentWindow()` | 82 | `AutoBattleTask.execute` 启动初始化；`WubeiTask` 在启动、priority prepared enter-battle、direct-combat success、known-dialog click 后调用；`XiuluoTaskV2` 在 confirmed-dialog click、prepared click、direct combat、recovered-dialog 后调用。返回 `void`，调用者依赖其重置 watcher 状态。当前 Cloud `FiveRingTaskV2` 无直接调用。 |
| `handleCombatTick(context, source, boolean recoverLeaderIncense)` | 107 | `AutoBattleTask` 传 `false`；`FiveRingTaskV2` 传 `true`。返回按 `TickResult` 消费。boolean 只映射到两种 full policy。 |
| `handleCombatTick(context, source, PostCombatRecoveryPolicy policy)` | 126 | `WubeiTask`、`XiuluoTaskV2` 的 phase 主调用；也是 boolean overload 的落点。返回决定等待战斗、战后恢复、返家或继续进战判定。 |
| `handleWindowCombatGuardTick(context, source)` | 199 | 当前 Cloud production source 无 caller；基线/current DHXY `WindowTaskRunner` watcher 调用。只消费 `IN_COMBAT`/`NONE`，不得消费 exit signal，也不得制造 `EXIT_RECOVERED`。 |
| `probeWindowCombatStateReadOnly(context, source)` | 223 | 当前 Cloud task 中由 `WubeiTask`、`XiuluoTaskV2` 的返家校正失败路径调用；基线 DHXY `WindowTaskRunner` 热启动循环也调用。仅 `IN_COMBAT` 被当作可信纠正，`NONE` 不提升成新业务事实。 |
| `getDynamicPollingIntervalMs()` | 236 | `AutoBattleTask` 在非 `FREE` 时决定 radar polling；基线/current DHXY `WindowTaskRunner` watcher 也消费。仅返回 delay，不执行 action。 |
| `nextCombatMaintenanceDelayMs()` | 252 | production 外部无直接 caller；由 `nextCombatWakeDelayMs()` 汇总。返回下一条基线 maintenance deadline，不执行 action。 |
| `nextCombatWakeDelayMs()` | 301 | `WubeiTask`、`XiuluoTaskV2` 消费后再夹在各任务自己的最小/最大 sleep 范围内。它取 maintenance 与 fast probe deadline 的较小值。 |
| `hasPendingFollowerFirstAidForCurrentWindow()` | 320 | `AutoBattleTask` polling 优先检查；为真时使用 `500ms` 唤醒。只读，不消费 pending。 |
| `hasPendingLeaderPostCombatRecoveryForCurrentWindow()` | 328 | `WubeiTask`、`XiuluoTaskV2` 的返家/进度安全点先检查，再决定是否调用 consume。只读，不执行恢复。 |
| `refreshFastExpectedExitBaselineAfterTrustedInCombat()` | 423 | `WubeiTask`、`XiuluoTaskV2` 仅在返家校正失败且 read-only probe 确认仍在战斗后调用；刷新 fast baseline，再回各自 WAIT phase，保留 deferred leader recovery。 |
| `consumePendingLeaderPostCombatRecoveryIfAllowed(context, source)` | 442 | `WubeiTask` 在 tracker green 后的 maintenance window、`XiuluoTaskV2` 在已确认的安全点调用；当前 caller 不利用 boolean 作新 phase 判定，返回只表示本次是否消费。 |
| `RefreshDuePanelVerifyDecision` public record | 808 | production 外部无 caller；由 enclosing service 的 refresh-due gate 返回并在类内消费。record accessors 只承载“本次是否允许 panel verify/为何不允许”的类内判定，不产生 turn。 |
| `RefreshDuePanelVerifyGate` public class | 817 | production 外部无构造/调用；由 enclosing service 持有。 |
| `RefreshDuePanelVerifyGate.reserveIfAllowed(...)` | 821 | 仅 `AutoCombatService` 内部 refresh-due 路径消费返回 decision；负责 30 秒 team gate 预约，不点击、不探测。 |

### 2.1 Caller 调用顺序与 phase 消费

#### `AutoBattleTask`

1. `execute` 先绑定 `TaskExecutionContextHolder`。
2. startup check 后把任务置入运行态，执行 first aid、`TaskMaintenanceService` 初始化，再调用 `initializeForCurrentWindow()`（当前约第 137 行）。
3. 主循环先 checkpoint，再调用 `handleCombatTick(context, "auto-battle", false)`（约第 163 行）。
4. 任意非 `NONE` 都直接 sleep/continue；只有 `NONE + FREE` 执行 idle maintenance。
5. polling 优先级：follower first-aid pending 为 `500ms`；否则 `FREE` 为 `3000ms`；否则使用 `getDynamicPollingIntervalMs()`。

#### `FiveRingTaskV2`

1. 仅在此前已观察到窗口战斗、watcher 又报告不再 active 后调用 `handleCombatTick(context, "wuhuan-v2", true)`（约第 1853 行）。
2. `IN_COMBAT` 保持 shared waiting。
3. `NONE` 只记录异常提示，仍与 `EXIT_RECOVERED` 一样进入 `SYNC_TASK_PANEL`。
4. 当前 Cloud 文件没有直接调用 `initializeForCurrentWindow()`；基线 DHXY 由 `WindowTaskRunner` 在 task watcher 启动时初始化。这一 owner 缺口要由父级冻结，不能在 34A 内猜测。

#### `WubeiTask`

1. 在 execute 开始（约第 351 行）初始化；priority prepared enter-battle（约第 788 行）、direct-combat success（约第 3447 行）、known-dialog click（约第 3624 行）后再次初始化。
2. tracker green 后依次记录 intent、打开 maintenance window、return-item prescan、消费 `CommonBox`、再消费 deferred leader recovery（约第 2777 行）；consume boolean 不被用来改变 phase。
3. `tickEnterBattle`（约第 3595 行）使用 `FULL_RECOVERY_WITH_LEADER_INCENSE`：`IN_COMBAT -> WAIT_BATTLE_FINISH`；prepared action 仍有既有优先级；`EXIT_RECOVERED -> POST_BATTLE_RECOVER`；`NONE` 继续既有 enter-resolution。
4. `tickWaitBattleFinish`（约第 3756 行）使用 `FAST_EXPECTED_EXIT`：`EXIT_RECOVERED -> POST_BATTLE_RECOVER`；`IN_COMBAT` 记录 saw-combat、做 prescan 并按 shared park；`NONE` 只在“尚未 saw + 既有 retry 到期”时回 `ENTER_BATTLE`，否则继续等。
5. return verification 失败后调用 read-only probe（约第 4164 行）；只有 `IN_COMBAT` 才刷新 fast baseline（约第 4167 行）并回 `WAIT_BATTLE_FINISH`，deferred recovery 保留。
6. sleep 使用 `nextCombatWakeDelayMs()`（约第 918 行）并套用 Wubei 自己的既有上下界。

#### `XiuluoTaskV2`

1. tracker shortcut 路径（约第 1828 行）使用 `FULL_RECOVERY_WITH_LEADER_INCENSE`；仅 `IN_COMBAT` 转 incidental `WAIT_COMBAT`，其余结果继续既有 pathing。
2. confirmed dialog click 后初始化（约第 2052 行），sleep `1200ms`，进入 pending-confirmation `WAIT_COMBAT`。
3. `waitCombat`（约第 2063 行）仅当 `enteredBattleByXiuluo && TRACKER_CONFIRM` 时使用 `FAST_EXPECTED_EXIT`，其余使用 full+incense。
4. `EXIT_RECOVERED`：incidental combat 先 cleanup 再 `TRY_TRACKER_SHORTCUT`；unknown combat 走 correction；expected combat 走 `RETURN_HOME`。
5. `IN_COMBAT`：prescan、记录 source/entry，并按 shared park；`NONE` 保持现有 confirm retry/fallback 顺序。
6. prepared click（约第 2229 行）、direct combat（约第 2793 行）、recovered dialog（约第 2803 行）后初始化；sleep 使用 `nextCombatWakeDelayMs()`（约第 2248 行）并套用 Xiuluo 既有上下界。
7. return verification 失败后 read-only probe（约第 2436 行）；仅 `IN_COMBAT` 刷新 baseline（约第 2439 行）并回 `WAIT_COMBAT`，deferred recovery 保留。
8. deferred helper 先查 pending（约第 2468 行），再 consume（约第 2471 行）；返回不被提升为新 phase truth。

### 2.2 基线 `WindowTaskRunner` 顺序

- 热启动：read-only probe；若为 `IN_COMBAT`，循环执行 checkpoint、按 `500..4000ms` clamp 的动态 sleep、再 read-only probe。
- Wuhuan/Xiuluo/Wubei watcher：先 initialize，再 guard；guard 为 `IN_COMBAT` 时更新动态 interval 并跳过其它观察。
- guard/read-only path 都不消费 exit。
- 当前 Cloud 没有 `WindowTaskRunner`，`CloudServiceConfiguration` 只扫描 service 与 turn client；生产源码也没有 `CloudServiceHost.create`、task registry/factory、`new Task` 或 `getBean(Task)` 激活证据。因此这些是 production-source 真实 caller，但尚未从 HTTPS task host 形成运行可达链；host 激活属于 TURN-40。

## 3. DHXY 后增 public API：34A 默认排除

当前 DHXY `AutoCombatService` 还包含下列基线和 Cloud 均不存在的 public API：

| 后增 API | 当前真实 caller | 34A 处理边界 |
|---|---|---|
| `authorizeCombatDetectionAfterEnterBattleAction(...)` | `WubeiTask`、`XiuluoTaskV2` 在实际 enter-battle action 后 | CR243/252 后增 authority 语义；不得静默移入 Cloud。 |
| `revokeCombatDetectionAuthority(...)` | DHXY `WindowTaskRunner` watcher 起点及 `XiuluoTaskV2` 相关结束点 | 同上。 |
| `probePausedWindowCombatStateReadOnly(...)` | DHXY `WindowTaskRunner` pause 路径 | 基线无此 pause probe；不得加入 34A。 |
| `consumeQueuedLeaderPostCombatFirstAidIfHead(...)` | DHXY `XiuluoTaskV2` tracker green 后 | 本地 queue/head 新语义；基线只规定 deferred consume。 |
| `reportXiuluoLeaderFirstAidAfterVerifiedReturn(...)` | DHXY `XiuluoTaskV2` verified return 后 | 本地 report/authority 新语义；基线无对应 Cloud caller。 |
| `reconcileReturnHomeVerifiedCombatState(...)` | DHXY `WubeiTask`、`XiuluoTaskV2` verified return 后 | 本地 reconcile 新语义；不属于 `696a12b0`。 |

父级必须明确冻结“34A 只保留 `696a12b0` public surface；以上六项不迁移”。如需保留任一项，应另行给出获准的业务差异、caller 同迁范围及独立验收，不应挤入一文件 migration。

## 4. `696a12b0` exact 业务决策

### 4.1 Tick 入口、policy 与探测优先级

1. 每个 tick 先通过 `context.throwIfStopRequested` 执行 stop/pause checkpoint；pause 是等待，stop 传播，二者都不是业务失败。
2. `policy == null` 映射到 `FULL_RECOVERY`。
3. `FAST_EXPECTED_EXIT` 第一次进入时 arm expected-exit baseline；当前仍记忆为战斗中时，优先执行 `20x20` avatar diff。只有 fast 未命中且 `4s` full-radar fallback 到期才执行完整 radar。
4. 其它 policy 每 tick 执行完整 radar。
5. 完整 radar 顺序固定：
   - 自动战斗 ROI：`(974,630,51x20)`
   - 战斗选项 ROI：`(927,302,100x225)`
   - 顶部战斗状态 ROI：`(456,62,123x39)`
   - 若历史状态为 `IN_COMBAT`，必须连续两次 miss，且小地图 ROI `(46,59,178x35)` 可读，才确认 exit
6. capture unavailable、机制结果不可判定时保守维持 `IN_COMBAT`；不得把缺证据转成 exit。
7. fast 参数固定：arm 后 `15s` gate、`1s` probe interval、`4s` full fallback、ROI `20x20`、pixel-diff `> 0.35`、RGB tolerance `15`。

### 4.2 Enter、exit、恢复与返回值

1. 首个 enter signal：安排 `now + 4000ms` entry maintenance，清空 last generic-clean time，立即确保 combat panel visible，并使用既有 `500ms` wait。
2. 若当前 radar 仍为 `IN_COMBAT`，丢弃 stale exit signal，不能消费旧 exit。
3. 消费 exit 时：fast policy 使用 fresh expected-wait signal；full policy 使用 ordinary signal。随后清 arm/entry pending；panel `recordCombatExit` 把 estimated rounds 减 `3`；first-aid counter 复位；执行 member `CommonBox` detect。基线可无条件调用 detect，由 `CommonBoxService` 自身 role gate 保证 leader 不建 member pending。
4. fast exit：清 follower first-aid pending；置 deferred leader recovery；关闭 fast watch；任务状态设为 `FREE`；当下不执行 leader first aid/incense。
5. full exit 的 follower 分支：
   - `auto_battle + MEMBER + reassigned`：no-focus probe；`SUPPLY_NEEDED` 或 `UNKNOWN` 建 pending，`HEALTHY` 清 pending。
   - 其它 follower：no-focus probe；`SUPPLY_NEEDED` 消费 cached plan；`UNKNOWN` 只记录，不虚构恢复成功。
6. full exit 随后 checkpoint；只有 `FULL_RECOVERY_WITH_LEADER_INCENSE` 执行 leader incense；最后置 `FREE`。
7. exit 后 action priority 固定：先尝试 `CommonBox`，再 follower first aid。若 box 成功点击，本 tick 返回 `EXIT_RECOVERED`，first aid 留到下一 tick；否则可继续 first aid。只要本 tick消费了 exit，最终仍返回 `EXIT_RECOVERED`。
8. 没有新 exit 时也先消费 pending `CommonBox`，再消费 pending follower first aid；任一动作实际成功时返回 `EXIT_RECOVERED`。两者均未执行且仍在战斗则做 maintenance 并返回 `IN_COMBAT`；否则返回 `NONE`。

### 4.3 Pending 与 deferred 精确语义

- `CommonBox` 仅在 `FREE` 消费；pending 必须匹配 exact task/window/role/run，TTL 固定 `30s`，还受 local team gate 约束。click 返回 false 时 pending 保留到 TTL；不能新增 retry TTL 或另一次确认。
- follower first aid 仅在 pending 且 `FREE` 时消费；team gates 最长等待 `3s`。先用 cached plan，无 plan 才允许一次 re-probe；`UNKNOWN` 保留 pending，其它结果清 pending。
- deferred leader consume：无 pending 或仍 `IN_COMBAT` 时返回 false；否则先清 pending，再执行 first-aid probe/cached plan、checkpoint、incense，最后返回 true。Wubei/Xiuluo 只在既有返家或进度安全点调用。
- `initializeForCurrentWindow()` 会清 follower pending、fast watcher 和 entry timestamps，但基线特意不清 `pendingLeaderPostCombatRecovery`；测试必须锁定这个非对称行为。

### 4.4 Maintenance 顺序、priority 与 deadline

1. 先计算 panel reason，再处理 entry maintenance。
2. panel reason priority 固定：`UNKNOWN -> LOW_ROUNDS (<=10) -> REFRESH_DUE`。
3. enter 后 `+4s`：先 generic UI clean，再 panel verify；若同时 refresh due，可合并该次 verify，不能额外插入 probe。
4. 周期 maintenance 固定每 `40s`：先 generic UI clean，再走 left-top gate/action。
5. panel refresh 是可选动作；`REFRESH_DUE` team gate 为 `30s`，urgent `UNKNOWN/LOW_ROUNDS` 的 per-window retry 也是 `30s`。
6. panel 对齐目标为屏幕相对窗口原点的 `(left + 489, top + 726)`；偏差 `>20px` 才 drag。
7. `nextCombatMaintenanceDelayMs()` 返回上述 deadline 的最小剩余值；`nextCombatWakeDelayMs()` 再取 maintenance 与 fast probe 的最小值。不得添加任意额外 TTL、轮询或 retry。

### 4.5 Stop/pause、click 与 fallback 不变量

- checkpoint 在既有调用点阻塞 pause、传播 stop；不得把 pause/stop 计入 Xiuluo/Wubei phase retry、round failure、loop guard 或任何业务失败计数。
- 不新增 checkpoint、park/yield、cleanup 或 verification；这些都会改变基线输入和 phase 次序。
- 点击失败只按对应 typed collaborator 的既有返回映射；AutoCombat 不自行补 transport retry。
- fast probe 是推测证据；返家 verification 是第一可信证据。只有返家失败后才做 trusted radar；若仍在战斗，恢复 WAIT phase 并保留 deferred recovery。
- 修罗既有阈值保持：phase retry `1`、recovery `2`、round failures `10`、loop guard `32`、precombat watchdog `180s`。

## 5. 可复用 typed ports/actions

### 5.1 TURN-19 LeftTopStatus

- `LeftTopStatusSwitchService.handleCombatMaintenance(...)` 已通过 `CloudLeftTopStatusPort.observe/click` 承担观察与输入。
- observe ROI：`(left+8, top+147, 11x19)`；上传 raw PNG，Cloud match；仅 `OPEN` 可点击。
- click steps：`MOVE_MOUSE -> WAIT 120ms -> CLICK_LEFT -> WAIT 250ms`。
- 可由 AutoCombat 继续调用 service，不应自行组 HTTPS command。
- 当前源码缺口：`requireExactBinding` 只比较 device/window；click 在生成 UUID 前没有重新读取 metadata 并对比初始 HWND/process。该缺口不能在 34A 唯一 production 文件里修补。

### 5.2 TURN-20 AutoCombatPanel

- `AutoCombatPanelService.ensurePanelVisible/verifyAndAlignPanel/recordCombatExit/resolveRoundsRefreshReason` 可直接复用。
- full-window/derived rounds 使用 raw capture、Cloud template/OCR。
- 打开 panel：`KEY_TAP ALT_8 + WAIT`，等待沿用 `500/1000ms` 场景值；drag：`DRAG_LEFT + WAIT 500ms`。
- panel open/drag 后的显式 re-observe 是业务级新 turn，属于基线验证，不是 transport retry。
- 已校验 action/window/step/frame/SHA/dim correlation。
- 当前源码缺口：latest metadata fence 只比 device/window，未与初始 HWND/process 做 exact identity 对比。

### 5.3 TURN-21 CommonBox

- `CommonBoxService` 通过 `CloudCommonBoxPort` 完成 detect/click；30 秒 pending 状态归 Cloud 生产实现所有。
- ROI：`(left+623, top+590, 59x28)`；capture 前有 exact device/window/HWND/process fence；raw PNG/SHA/dim 相关性校验。
- click steps：`MOVE_MOUSE -> WAIT 80ms -> CLICK_LEFT -> WAIT 120ms`。
- 每次 detect/click 均为独立业务 turn；无自动重发。

### 5.4 TURN-23 PlayerState first aid / incense

- first-aid ROI：`(left+823, top+85, 198x17)`；可在同一 capture request 中清 pointer；上传 raw PNG，Cloud 返回 threshold/target plan。
- 每个 target 的输入：`CLICK_RIGHT -> WAIT 800ms`。
- incense 使用 full status ROI `(901,123,123x34)` 和 cached `48px` ROI；Cloud template/OCR；Bag 使用走已闭合的 `LOCAL_SERVICE` turn。
- first-aid/incense action port 均在 action 前比较初始 HWND/process，并校验 action/window/steps/raw frame；无 transport retry。
- 依赖现状需要原样消费：first-aid command 已发出而 confirmed input 失败时，现实现仍会记录计数并返回 true。34A 不得据此补发或重解释为另一业务结果。

### 5.5 TURN-24A BattleRadar

- `BattleRadarService` 已把 capture 与 Cloud compute 全部放入 typed path；不再依赖七个 radar fact；每 stage 一次 capture，无 INPUT/execute。
- outcome/frame correlation 与 confirmed stop 传播已存在。
- 当前源码缺口：capture preflight 只比 device/window，state key 也仅是 windowId；尚未在 UUID 前比较初始 HWND/process。

### 5.6 已闭合的 UI cleaner

- `CloudUiCleanerPort.closeAllGenericWindows(...)` 是一个 `LOCAL_SERVICE` action；调用前后有 checkpoint，无 retry。
- 当前 client 使用注入的 singleton `TurnGameClient` 和 current provider，但没有 AutoCombat 层可证明的初始 HWND/process fence；父级需把它与 TURN-19/20/24A 的 identity 缺口一起处理。

## 6. TURN-33 依赖与当前具体接口

TURN-33 当前不稳定的生产 surface 是：

- `SummonSkillService.cleanSummonSkillsOnce(SummonSkillCleanupRequest)` 仍调用 `context.getRemoteGameClient().summonSkillWholePass().execute(...)`
- `CloudSummonSkillWholePassCapability`
- Summon 专用 `CloudTaskExclusiveInteractionAuthority.executeSummonSkillWholePass(...)`

TURN-33 计划将 active Summon path 改为逐 action closed turn，并让上述 whole-pass capability/authority branch 退出 active path，同时保持 `TaskMaintenanceService` read-only。

源码搜索没有发现 `AutoCombatService` 直接引用或调用这些 Summon surface。因此 TURN-33 对 34A 是架构稳定性和并行修改时序依赖，不是要求 AutoCombat 新增 Summon 调用。34A 禁止引入任何 Summon coupling。

当前 Cloud `AutoCombatService` 自身还有三个无法在 Cloud main source 解析的旧本地依赖：

- `TaskTurnCoordinator`
- `WindowRuntimeContext`
- `WindowTaskContextHolder`

它们才是 34A 一文件内必须移除或替换的直接接口。建议：

1. 状态上下文改用现有 `TaskExecutionContextHolder`。
2. state key 固定为 `deviceId + windowId + initial identity fingerprint(windowTitle, nativeHandle/HWND, processId)`；任一 identity drift 立即失效旧状态。
3. 禁止 fallback 到 `default` 或 `epoch=0`；turn-native `getPlayerIdentityEpoch()` 当前会抛异常，也不能用。
4. 移除本地 `TaskTurnCoordinator` wrapper，但必须保留同步调用顺序、`CommonBox` 优先于 first aid、以及各 typed turn/DHXY input queue 已有的物理串行化。

TURN-33/34A 并行期间，以下 `TaskMaintenanceService` API 必须由父级冻结签名和语义：

- `isPendingLocalSupportLeaderDetection(...)`
- `isLocalSupportMemberSession(...)`
- `isLocalTeamSupportCapabilityOpen(...)`
- `awaitLocalTeamSupportCapabilityOpen(...)`
- `isLocalSupportMemberCandidate(...)`
- `awaitTeamFirstAidMaintenanceWindowOpen(...)`

## 7. 唯一写集、named test 与并行冲突

### 7.1 TURN-34A 推荐独占写集

Production 只允许：

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java`

唯一 named test：

- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`

固定实现报告：

- `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md`

除上述 production file、named test、固定报告外，不建议打开任何 source/test/doc 写集。当前 helper 报告不是 implementation card。

### 7.2 与 TURN-22 的冲突隔离

TURN-22 当前写集必须保持不触碰：

- `TeamReturnService`
- `CloudTeamReturnPortAssembly`
- `TeamReturnTurnContractTest`
- `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-22.md`

AutoCombat 的返家校正只能消费既有 task/typed-service 结果，不得顺手改 TeamReturn 的 probe、verify、坐标、terminal mapping 或 retry。

### 7.3 与 TURN-26 的冲突隔离

TURN-26 当前写集也必须保持不触碰：

- `DialogService`
- `CloudDialogOptionOcrImagePort`
- `CloudDialogOptionOcrWordsPort`
- `CloudDialogWhiteStoryTemplatePort`
- narrowly scoped `LocalOcrClient`
- `DialogOptionTurnContractTest`
- `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-26.md`

34A 不得借 dialog-enter caller 调整 OCR、template、click 或 fallback 顺序。

## 8. HTTPS turn exact 验收合同

### 8.1 Window 与 native identity

- 每个 explicit business action 必须先读最新 metadata，再对比初始 `deviceId/windowId/windowTitle/HWND/processId` 和 stop token。
- metadata 缺失、任一 identity drift 或 stop 已确认：`0 UUID + 0 command`。
- 只有 preflight 全部通过后才能生成新 UUID；每个 business action 固定 `1 UUID + 1 command`。
- state 必须按 exact device/window/HWND/process identity 隔离；同 windowId 换 HWND/process 后不得继承 fast baseline、pending、maintenance deadline 或 panel gate。
- 坐标必须是最新窗口 rect 原点加未缩放的 relative coordinate；禁止 scaling、clamp、`(0,0)` fallback 或跨窗口复用旧 absolute point。

### 8.2 Capture/raw PNG/ROI

- capture turn 只允许一个与 command step 对应的 multipart frame，媒体类型必须是 `image/png`。
- 必须校验 exact purpose、ROI、sourceStep、frame index、SHA-256、width/height 和可解码性。
- ROI 必须保持基线原值：radar 四组 ROI、fast `20x20`、left-top、panel/full-window derived rounds、CommonBox、first-aid、incense；AutoCombat 不得自行裁新图或改坐标。
- capture 不执行 input；input outcome 不携带伪 frame。

### 8.3 UUID/command/step correlation

- outcome 必须逐字段匹配 actionId、device、window、完整 metadata snapshot。
- step 数量、顺序、type、terminal state 必须和 request 完全一致；一个 step `FAILED` 后的剩余 step 必须是 `NOT_RUN`。
- input bundle 必须保持 collaborator 已冻结的原子顺序；不能把 move 与 click 拆成两个可交错 action。
- metadata read 本身不分配 action UUID；业务 re-observe/re-probe 只有在 `696a12b0` 已明确存在时才使用新的 UUID。

### 8.4 Terminal、stop/pause 与零 transport retry

- confirmed stop 必须传播 task stop；不能映射为 `NONE`、probe miss、click false 或业务失败。
- terminal failure 按对应 typed port 既有 contract fail closed；不确定 outcome 不得虚构业务 success/false。
- AutoCombat 不捕获 terminal exception 后重发同一 command，也不生成补偿 command。
- 所有 transport automatic retry 计数必须为零；业务 fallback 仅限基线已有的下一次 full radar、一次 first-aid re-probe、panel open/drag 后 re-observe，各自使用新 UUID。
- pause 只在 checkpoint 等待，不产生 command、不改变 phase/pending/counter/deadline。

## 9. Active-path scoped 零引用门

验收范围是 `AutoCombatService` 每个 public API 可达的 production chain，不是全仓字符串清零。该范围内直接引用必须为零：

- `TaskTurnCoordinator`、`WindowTaskContextHolder`、`WindowRuntimeContext`
- `CloudGameClient`、`CloudTaskServicePort`、`getRemoteGameClient`
- `WindowFact`、`readWindowFact`
- `executeInputBundle`、`executeLocalMacro`、`LocalMacroKind`、legacy `RemoteOperation`
- `CloudSummonSkillWholePassCapability`、`CloudTaskExclusiveInteractionAuthority`、`summonSkillWholePass`、`executeSummonSkillWholePass`
- `GameClientTracker`、`CoordinateHelper`、`InputSequences`、`InputProvider`、`WindowScopedTempPath`
- Java Robot、direct screenshot、direct keyboard/mouse input
- 七个 `BATTLE_RADAR_*` fact 常量、旧 `AUTO_COMBAT_PANEL/GEOMETRY` fact、旧 `COMMON_BOX/LEFT_TOP` fact 读取

以下不是旧 active-path authority，不应被字符串误伤：

- `GameContext`
- `TaskExecutionContextHolder`
- 已列出的 typed services/ports
- `TeamSupportCapability.COMMON_BOX`、`TeamSupportCapability.LEFT_TOP_STATUS` 业务 enum
- Cloud 侧纯计算 decision/result model
- 日志中用于说明业务能力的普通文本

## 10. 唯一 named test 最小验收矩阵

`AutoCombatServiceTurnContractTest` 至少覆盖：

| 组 | 最小场景与断言 |
|---|---|
| Public surface | 覆盖第 2 节每个 baseline public method、两种 enum、public record/gate；确认第 3 节六个 DHXY 后增 API 未进入 Cloud。 |
| State isolation | 两窗口交错 tick；相同 windowId 但 HWND/process drift；旧 fast/pending/deadline/gate 均不可泄漏。 |
| Initialize | follower/fast/entry 被重置；deferred leader recovery 不被清除。 |
| Overload | boolean `false/true` 精确映射；null policy 精确映射到 full recovery。 |
| Radar | 三阶段+minimap exit 顺序；两次 miss；unavailable 保守；fast `15s/1s/4s/20x20/0.35/15`。 |
| Enter/exit | `+4s` maintenance；stale exit 丢弃；estimated rounds `-3`；三种 policy 的 recovery 差异与 `TickResult`。 |
| Priority | `CommonBox` 先于 follower first aid；box click 成功时 first aid 留到下一 tick；无新 exit 的 pending action 仍映射 `EXIT_RECOVERED`。 |
| Follower | reassigned auto-battle 的 `SUPPLY_NEEDED/UNKNOWN/HEALTHY`；cached plan；唯一 re-probe；unknown 保留 pending。 |
| Deferred leader | 战斗中不消费；安全点先清 pending，再 first aid、checkpoint、incense；caller 不凭 boolean 创建新 phase。 |
| Maintenance | reason priority、`4s/40s/30s`、panel target 和 `>20px` drag；两个 delay API 的最小 deadline 计算。 |
| Read-only/guard | 零 signal consume、零 input、零 `EXIT_RECOVERED`；只有 trusted `IN_COMBAT` 能刷新 fast baseline。 |
| Stop/pause | pre-UUID stop 为零 action；confirmed stop 传播；pause 不改业务 state；既有 checkpoint 数量和位置不扩张。 |
| Identity | exact device/window/title/HWND/process preflight；metadata drift 为零 UUID；absolute coordinate 来自最新 rect。 |
| Correlation | action UUID 唯一；command/step/outcome/full metadata 严格相等；raw PNG、ROI、SHA、尺寸与 decode 严格校验。 |
| Terminal | terminal failure、uncertain result、`FAILED -> NOT_RUN`、confirmed stop 分别按 collaborator contract；transport retry 为零。 |
| Caller contract | AutoBattle、FiveRing、Wubei、Xiuluo 的第 2.1 节返回消费分别做情景测试，尤其锁定 `NONE` 不被提升为新业务事实。 |
| Active-path gate | 仅在这一个 named test 内静态检查第 9 节旧引用为零；不新增第二个 source guard 或共享 test helper。 |

测试 profile 按权威计划使用 `TASK+STATE`，并保留默认 `BC4+BASE`。named test 可使用 test-file private fakes；不得为测试新增 production helper。

未来由实现 owner 在依赖源码稳定后执行的唯一命令：

```text
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

Cloud compile/package 仍需由实现 owner 按启动链执行；本 helper 按指令没有运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input。

## 11. 父级必须冻结的行为点

1. 34A public surface 是否明确等于 `696a12b0`，并排除第 3 节六个 DHXY 后增 API。
2. state key 是否固定为 `deviceId + windowId + windowTitle + HWND + processId`，以及 identity drift 的原子失效时点。
3. 移除 `TaskTurnCoordinator` wrapper 后，仍以同步 source order 保持 `CommonBox -> follower first aid` 和 deferred 顺序，不新增 session/owner/ledger。
4. TURN-33/34A 并行期间，第 6 节六个 `TaskMaintenanceService` 方法的签名、等待语义和返回语义保持不变。
5. 当前 Cloud guard/init owner 如何在 TURN-40 激活前表达；尤其 `FiveRingTaskV2` 当前没有直接 initialize，不应由 34A 临时扩写 task 文件。
6. TURN-19/20/24A/UI cleaner 的 HWND/process preflight 缺口是先由各依赖一文件修复，还是父级提供已有的同等源码证据；34A 的单 production 文件无法替依赖构造该保证。
7. 每个 typed collaborator 的 terminal mapping 原样保留，AutoCombat 不做统一 catch/boolean 化。
8. AutoCombat 只做 orchestration/state，不直接注入 `TurnGameClient`，不自行构造 capture/input command。
9. 不新增 probe、verification、checkpoint、TTL、retry、park/yield、cleanup 或 phase transition。
10. production/test/report 写集严格固定为第 7.1 节三项，并与 TURN-22/26 完全隔离。

## 12. 未决问题

- TURN-33 最终退出 active path 的接口形态尚未形成稳定源码，34A 不能依据临时 whole-pass/authority API 开始绑定。
- TURN-19/20/24A/UI cleaner 尚缺统一的“初始 HWND/process 与 action 前 latest metadata”源码证明；若父级要求 exact native identity 为 34A 自含验收，必须先明确修复 owner 和写集。
- 当前 Cloud 没有 task host 激活链，无法从源码证明 startup owner 会调用 FiveRing watcher initialize；该问题属于 TURN-40，但 34A 测试必须用 caller scenario 锁住预期，不得假设 runtime 已连通。
- 当前 Cloud `AutoCombatService` 的三个旧本地 import 无可解析 main-source type；一文件迁移方案必须先冻结替换方式，不能通过复制本地 runtime/authority 类型扩写写集。
- `RefreshDuePanelVerifyDecision/Gate` 虽为 public，但没有外部 production caller；父级需决定保留 public 二进制 surface 还是只要求源码行为等价。默认建议保留，避免 34A 顺带做 API 收缩。

## PRECHECK EVIDENCE

- `696a12b0` 的 public surface、四个真实 task caller、基线 runner caller、phase/priority/fallback/probe/click/stop-pause 语义均已有可定位源码证据。
- TURN-19/20/21/23/24A 提供了 AutoCombat 可直接调用的 typed service/port；34A 无需也不应直接构造 HTTPS action。
- TURN-34A 可冻结为一个 Cloud production 文件、一个 named test、一个固定报告；TURN-22/26 写集可完全隔离。
- 当前 Cloud 文件仍含三个不可解析的旧本地依赖；TURN-33 的 active Summon surface 仍在变化，且没有 AutoCombat 直接调用理由。
- native identity 的 exact HWND/process 要求在 TURN-19/20/24A/UI cleaner 尚有依赖侧证据缺口；该项不能由 34A 一文件替代修复。
- 本次仅新增本报告；未改 Java、测试、主计划、ACTIVE_WORK、CR271、矩阵或 dashboard，未运行任何受限命令或运行路径。

## RISKS

- 若父级未先冻结第 11 节，implementation owner 可能在删除旧 runtime/turn coordinator 时误改 state ownership、动作优先级或 caller phase。
- 若把 DHXY CR243/252 后增 API 当成默认基线，会把未获单独授权的 authority、pause probe、queue/head 和 return reconcile 语义带入 Cloud。
- 若仅校验 device/window 而不校验 initial HWND/process，同一注册窗口发生 native rebind 时可能把 capture、坐标、pending 或 input 关联到错误进程。
- 若 TURN-33 同时改动 `TaskMaintenanceService` 的六个被调用方法，34A 的一文件边界与 named test fake 会失去稳定合同。
- 若为了通过编译复制 `WindowRuntimeContext`、`WindowTaskContextHolder` 或 `TaskTurnCoordinator` 到 Cloud，将重新引入旧 authority，并突破一文件写集。
- 若把 read-only miss、terminal uncertainty、pause/stop 或 click failure统一折叠为 `NONE/false`，会改变基线 exit、retry 和 phase 语义。

true EOF
