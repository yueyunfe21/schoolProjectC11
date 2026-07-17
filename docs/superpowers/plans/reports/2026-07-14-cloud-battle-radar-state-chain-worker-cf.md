# Internal Worker CF - Cloud BattleRadar State Chain

CLAIMED

- task: `W-CF-BATTLE-RADAR-PUBLIC-STATE-CHAIN`
- claimedAt: `2026-07-14T09:27:31.719-04:00`
- role: Internal Worker CF; implementation only, not reviewer
- uniqueJavaWriteSet: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- uniqueReportWriteSet: `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-14-cloud-battle-radar-state-chain-worker-cf.md`

## Parent Source Review #1 - `W-BRADAR-STATE-SIGNAL-PUBLIC-CHAIN-IMP1` - 2026-07-14T09:35:00-04:00

**SOURCE APPROVED，P0/P1/P2=0。** 父级完整读取当前 Cloud `BattleRadarService.java:1-340` 并与
committed `0114604e` 对照。`:38-231` 的 11 个 public API 覆盖外部 verdict、expected-exit arm/fast-probe/
full-radar gate、enter/exit one-shot consume、stale discard、paused observation 与动态 polling；`:233-321`
保留 baseline 的 transition、pending publication、battle-count、timer reset、current-cycle eligibility 与 clear
顺序。`15_000/1_000/4_000ms` 常量及 `<` 同毫秒 freshness 边界未漂移。

唯一 Cloud adaptation 位于 `:287-293`：baseline holder window id 改为显式
`TaskExecutionContext.getWindowId()`，null/blank 才使用原 `default` fallback；per-window
`ConcurrentHashMap<String, BattleRuntimeState>` 与 baseline passive state shape 保持一致。类中没有
WindowRuntimeContext/HWND/capture/template/OCR/minimap/watcher/input 调用，也没有新增 owner/session/ledger/
TTL/retry。`GameContext` action-state 读写仍要求未来 caller 运行在已经批准的 assembly state-activation
边界内；本单没有伪造 caller 或绕过该边界。

Worker 非 clean Cloud `mvn -q compile` exit 0、源码 SHA
`BD65920071FA392C6074D97A1BA404C2BA39835EEEA4C10876A9FB01C5CC03BC` 已记录。因其他五个互斥 Java
writer 尚未稳定，父级 fresh clean package 暂缓；集成门通过后转 FINAL APPROVED，不要求 CF 追加 Design。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Delivery

- deliveredAt: `2026-07-14T09:32:13.901-04:00`
- workerStatus: `IMPLEMENTED_FOR_PARENT_REVIEW`
- reviewerStatus: not reviewed by CF; CF is implementation-only
- businessDifference: `无已批准业务差异；按 0114604e 基线等价迁移`

## Baseline And Workspace Evidence

- DHXY branch/HEAD: `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`.
- Baseline BattleRadar blob: `52b439febeb01b4d9ddb4afb4e00a04076c1aa4f`.
- `git diff --numstat 0114604e -- src/main/java/com/bot/dhxy/service/BattleRadarService.java` returned no rows; the committed file is the authority.
- Cloud branch/HEAD: `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`.
- Cloud target was already untracked before this task and remains untracked; pre-edit SHA-256 was
  `1BAA3A18D8C5207DABCD156FB33515F50F2A322C3867BE074FB3E0EF8C3A3190`.
- Final Cloud BattleRadar SHA-256: `BD65920071FA392C6074D97A1BA404C2BA39835EEEA4C10876A9FB01C5CC03BC`.
- Both repositories contained unrelated dirty/untracked files. None were reset, overwritten, deleted, cleaned, staged, committed, or otherwise mutated by CF.

## Public Definitions

The Cloud same-path class now has exactly 11 public methods:

1. `applyExternalCombatStateVerdict(boolean inCombat, String source)`
2. `armExpectedCombatExitWait(String source)`
3. `nextFastExpectedCombatExitProbeDelayMs()`
4. `shouldRunFullRadarForFastExpectedExitFallback()`
5. `consumeCombatEnterSignal()`
6. `consumeCombatExitSignal()`
7. `consumeCombatExitSignalForExpectedWait(String source)`
8. `discardStaleCombatExitSignalIfInCombat(String source)`
9. `discardCombatEnterSignalIfNotInCombat(String source)`
10. `markCombatExitObservedDuringPause(String source)`
11. `getDynamicPollingIntervalMs()`

Lombok generates the callable constructor from the two explicit required fields:
`BattleRadarService(GameContext context, TaskExecutionContext taskExecutionContext)`.

## Public Call Graph

- `applyExternalCombatStateVerdict` -> `updateCombatState`.
  - enter -> `state` -> `GameContext.setCurrentActionState(IN_COMBAT)` -> `onEnterCombat` -> optional `clearCombatExitPending`.
  - exit -> `state` -> unconsumed-enter snapshot -> `GameContext.setCurrentActionState(FREE)` -> pending exit publication -> `onExitCombat`.
- `armExpectedCombatExitWait` -> `state` -> `isCurrentExpectedWaitAllowedExit` ->
  `isCurrentUnconsumedEnterExit` / `isCurrentPausedObservedExit` -> optional `clearCombatExitPending`.
- `nextFastExpectedCombatExitProbeDelayMs` -> `GameContext.getCurrentActionState` -> `state` -> committed 15-second delay and 1-second interval gates.
- `shouldRunFullRadarForFastExpectedExitFallback` -> `state` -> committed 4-second full-radar fallback gate.
- `consumeCombatEnterSignal` -> `state` -> clear `combatEnterPending` once.
- `consumeCombatExitSignal` -> `state` -> `clearCombatExitPending` once.
- `consumeCombatExitSignalForExpectedWait` -> `state` -> expected-wait eligibility predicates -> `clearCombatExitPending` on stale or consumed exit.
- `discardStaleCombatExitSignalIfInCombat` -> `GameContext.getCurrentActionState` -> `state` -> `clearCombatExitPending`.
- `discardCombatEnterSignalIfNotInCombat` -> `GameContext.getCurrentActionState` -> `state` -> clear `combatEnterPending`.
- `markCombatExitObservedDuringPause` -> `state` -> copy current pending battle count into the paused-observed marker.
- `getDynamicPollingIntervalMs` -> `GameContext.getCurrentActionState` -> unchanged `4000/2000/10000` switch.
- `state` -> explicit `TaskExecutionContext.getWindowId()` -> `runtimeStates.computeIfAbsent`; null/blank preserves the committed `default` fallback. It does not read a holder, native binding, or HWND.

## Block-By-Block Baseline Comparison

- Transition block: `applyExternalCombatStateVerdict`, `updateCombatState`, `onEnterCombat`, and `onExitCombat` compare `EXACT` to committed `0114604e` method blocks. Action-state writes, battle count, timer resets, pending-signal publication, and log order are unchanged.
- Expected-wait block: `armExpectedCombatExitWait`, `nextFastExpectedCombatExitProbeDelayMs`, and `shouldRunFullRadarForFastExpectedExitFallback` compare `EXACT`. Constants remain `15_000L`, `1_000L`, and `4_000L`; the strict `<` stale check still preserves same-millisecond exit/arm evidence.
- Signal block: both consume methods, expected-wait consume, both discard methods, and pause observation marking compare `EXACT`. No consume/discard order or one-shot boundary changed.
- Eligibility/reset closure: `isCurrentUnconsumedEnterExit`, `isCurrentPausedObservedExit`, `isCurrentExpectedWaitAllowedExit`, and `clearCombatExitPending` compare `EXACT`.
- Polling block: `getDynamicPollingIntervalMs` compares `EXACT`; existing cadence did not drift.
- Passive state block: existing `BattleRuntimeState` shape is retained, including battle count, miss counter, combat/probe timers, one-shot signals, epoch-millisecond freshness fields, pause marker, and the pre-existing passive avatar-baseline slot. No avatar capture/read method was added.
- Window-key adaptation: only baseline `WindowTaskContextHolder.rawCurrent()` was replaced. The Cloud helper reads the explicit task-run context's logical `windowId`, with the same `default` fallback. No new owner, permit, session, ledger, TTL, retry, or per-Service workflow machinery exists.

The scripted comparison checked 18 public/private migrated method blocks and returned `EXACT` for all 18. Static definition scan returned `PUBLIC_COUNT=11` and `FORBIDDEN_IMPORT_COUNT=0`.

## Explicit Exclusions

- Not migrated: `checkAndSyncCombatState`, `checkFastExpectedCombatExitByAvatarDiff`, `refreshFastExpectedCombatExitAvatarBaseline`, avatar capture, four-stage radar capture, template matching, OCR, minimap reads, local watcher mechanics, input, host/caller/UI wiring.
- No remote schema, transport, or other Service changed.
- No application, host, Task, UI, capture, or input path was started.
- No tests were created or run, per repository no-local-test mode and the user's explicit compile-only gate.

## Compile Gate

- Command: `mvn -q compile`
- Working directory: `D:/mavenProject/dhxy-cloud-brain`
- Exit code: `0`
- Duration: `33.2s`
- `clean` was not used; tests were not run.
- baseline: DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f`
- guard: protect all dirty/untracked work; no Git mutation; no host/Task/UI/capture/input startup

## Authoritative Claim

CLAIMED

- task: `W-BRADAR-STATE-SIGNAL-PUBLIC-CHAIN-IMP1`
- claimedAt: `2026-07-14T09:27:58.758-04:00`
- uniqueJavaWriteSet: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- uniqueReportWriteSet: `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-14-cloud-battle-radar-state-chain-worker-cf.md`
