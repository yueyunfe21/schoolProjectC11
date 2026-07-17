# W-COUNT-BATTLE-RADAR-UPDATE-STATE-1

- Status: `CLAIMED`
- Worker: Internal I19 implementation-only Worker（非 reviewer）
- Count unit: `BattleRadarService::updateCombatState`
- Count delta: `+1`（仅父级源码审查 + fresh Maven 同轮应用）
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\BattleRadarService.java`
- Report write set: 本文件
- Guardrails: 不 build/test/runtime/input/Git；不新增 owner/session/TTL/retry/wrapper；不重算已批准 `checkAndSync` 本体；保护两仓他人 dirty。

## Authority And Workspace Evidence

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 适用规则、
  `docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`、
  `docs/superpowers/specs/2026-07-12-service-migration-matrix.md` 及 BattleRadar 既有 worker/preflight 报告。
- 业务权威：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。适用规则包括：进入战斗事实清理五倍 dialog 准备态；
  五倍入战前预算只在 Runner 首次确认 `IN_COMBAT`/`EXIT_RECOVERED` 时结束；战斗中启动只等待真实脱战后再跑前置；
  修罗迁云必须保留 `WAIT_COMBAT`、退战恢复和 stop/pause 语义。本文未批准任何行为差异。
- whole-Service 门：真实 caller -> Cloud Service -> typed DHXY mechanics -> closed terminal/result；父级源码审查与
  applicable fresh Maven 同轮才可应用 `+1`。缺写集外前置必须 `BLOCKED`，不得造 stub、自调用、默认 policy 或零计数 filler。
- 矩阵把 `BattleRadarService::updateCombatState` 单列为“状态跃迁核心：置 `IN_COMBAT/FREE` 并发 enter/exit
  一次性信号”；本任务的显式 countUnit 选择按该独立矩阵行执行，不把其它 private helper 另行计数。
- 两仓状态仅采用已记录快照：DHXY `thin-client-design@0114604e`、Cloud
  `navigation-migration@3b988ca`，两仓既有 dirty/untracked 全部只读保护。按任务禁止项未执行 Git，故不伪称取得新的
  `git status`；本 Worker 未修改、回滚、清理、暂存或提交任何既有 dirty。
- 当前 Cloud 目标 SHA-256：
  `E90E99FB9444BAD960BC5C0B648EEA51501CED1AAA8ED26B8061F53B46B86405`（26,025 bytes）。
  `696a12b0` 镜像 SHA-256：
  `F224F83A723A9B0741B909301BED030BDDF31C7DE25A4F3DB65B0FEC4856DD29`。

## Existing State Core

本 Worker 未重算、重审或改写已由父级批准的 `checkAndSyncCombatState` 本体，只沿其已批准调用边界核对
`updateCombatState` 的输入与输出：

1. `AutoBattleTask:163 -> AutoCombatService.handleCombatTick:107/126 -> AutoCombatService:150 ->
   BattleRadarService.checkAndSyncCombatState:65` 是真实 active caller；`NpcClickService:262` 另有 direct-combat
   verifier caller。
2. `checkAndSyncCombatState:71/89/107` 在 auto-flag/selection/top 任一已批准 typed signal 可见时调用
   `updateCombatState(true)`；`:130` 在重复 miss + minimap readable 的已批准保守退战门后调用
   `updateCombatState(false)`。
3. `updateCombatState(true)` 仅在 remembered state 不是 `IN_COMBAT` 时：清 exit misses -> 写
   `GameContext.ActionState.IN_COMBAT` -> `onEnterCombat`；后者 battleCount `+1`、初始化战斗/fast-probe 时间、
   清旧 exit pending、发布 `combatEnterPending=true`，最后返回 `true`。
4. `updateCombatState(false)` 仅在 remembered state 是 `IN_COMBAT` 时：清 exit misses -> 写
   `GameContext.ActionState.FREE` -> 发布 exit pending/时间/battleCount -> `onExitCombat` 清本战 fast-probe 状态，
   最后返回 `true`。没有跃迁时返回 `false`，不制造 enter/exit truth。
5. enter one-shot 由 `AutoCombatService.maybeHandleCombatEnter:333` 消费；普通 exit one-shot 由
   `AutoCombatService.consumeExitAndRecover:353` 消费。两个 public consumer 均成功后清 pending，closed boolean
   防止同一 signal 重复消费。
6. 当前 `updateCombatState`、enter/exit publication、consumer 清理顺序与 `696a12b0` 一致；唯一既有 Cloud
   机械适配是把不允许上云的头像 `BufferedImage` baseline 清空改为 `fastExpectedExitBaselineReady=false`，该适配
   已在既有 BattleRadar whole-Service 父级源码审查范围内批准，本 Worker 未修改。

## Precise Blocker

所需 fast-exit incoming edge 当前不是 active caller chain：

- `AutoCombatService:146` 只有在 `recoveryPolicy == FAST_EXPECTED_EXIT` 时才调用
  `checkFastExpectedCombatExitByAvatarDiff`，其 `CHANGED` terminal 才在 `BattleRadarService:193` 调用
  `updateCombatState(false)`。
- 当前 Cloud `src/main` 中唯一真实 `handleCombatTick(...)` 任务 caller 是 `AutoBattleTask:163`，固定传 boolean
  `false`；`AutoCombatService.legacyPostCombatRecoveryPolicy(false)` 映射为 `FULL_RECOVERY`。全树没有实际 caller
  传入 `PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT`。
- 因此 `checkAndSync -> updateCombatState -> enter/exit one-shot -> closed boolean` 已真实闭合，但任务明确要求的
  `fast-exit caller -> updateCombatState` 仍 dormant。所缺的是五倍/修罗 Cloud task/adapter 在既有业务条件下选择
  `FAST_EXPECTED_EXIT` 的 caller，必须修改本任务冻结且写集外的 Task/adapter/AutoCombat caller。
- 不能在 `BattleRadarService.java` 内用自调用、默认 fast policy、额外 owner/session/TTL/retry 或固定 terminal
  伪造可达性；也不能把 `AutoBattleTask` 的普通 FULL recovery 擅改为 fast recovery。

## Delivery

- Status: `BLOCKED - MISSING ACTIVE FAST_EXPECTED_EXIT CALLER`
- Java change: `NO_CODE_CHANGE`
- Requested count delta: `+1`
- Applied count delta: `0`
- Severity for parent review: `P0=0 / P1=1 / P2=0`
- Exact unblock: 父级先授权并闭合真实五倍/修罗 Cloud caller，使其在原业务条件下调用
  `AutoCombatService.handleCombatTick(..., FAST_EXPECTED_EXIT)`；随后复核该 caller -> fast avatar typed fact ->
  `updateCombatState(false)` -> exit one-shot -> expected/normal closed consumer。不得改 `checkAndSync` 本体。
- Fresh Maven: 未运行；按用户禁令留给父级在所有 Java writer 稳定后与源码审查同轮执行。本 blocker 不是 Maven
  可消除的缺口，未解锁前不得应用本 count unit 的 `+1`。
- 未运行 build/test/runtime/Task/UI/capture/input，未执行 Git，未修改任何 Java。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-15T03:37:07-04:00

父级独立复核 `BattleRadarService.updateCombatState` 的普通 enter/exit 路径与 active Cloud caller，并确认
Worker 报告的 fast-exit 缺口成立：当前唯一 active `AutoBattleTask -> handleCombatTick(..., false)` 只选择
`FULL_RECOVERY`，没有真实 Task caller 选择 `FAST_EXPECTED_EXIT`。在本单唯一 `BattleRadarService.java`
写集内无法闭合要求的 fast-exit incoming edge，且不得伪造自调用或改变普通 auto-battle policy。
结论：**P0=0/P1=1/P2=0，BLOCKED / countDelta=0**。解锁条件为先由独立 Task/adapter count unit
按 `696a12b0` 原条件接入 `FAST_EXPECTED_EXIT`；本轮不计数、不进构建池。
