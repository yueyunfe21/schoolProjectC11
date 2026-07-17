# I44 BattleRadar expected-exit consume count unit

- Status: `DELIVERED_NO_CODE_CHANGE / PARENT_REVIEW_PENDING`
- Worker: Internal implementation Worker I44; not a reviewer
- Count unit: `BattleRadarService::consumeCombatExitSignalForExpectedWait`
- Requested delta: `+1`
- Worker result: complete active source chain; `countDelta=+1` candidate pending parent source review and the parent's unified fresh build
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\BattleRadarService.java`
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Java changes: none

## Scope And Baseline Gates

Read before closure:

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- the current CR271 head in `docs/ACTIVE_WORK.md`
- `docs/业务逻辑.md`, especially `Expected 战斗快脱战与回程验证兜底`
- `docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`
- the complete current migration matrix file, `3449` lines / `276548` normalized characters, normalized SHA-256
  `ff8461f64db0c64c2fefd83aa0519ab00fa860ae3b276c2ccbd3a59f3cedb5b3`; the exact count row is now at
  matrix line `1371`
- both repository status snapshots: DHXY is on `thin-client-design`; Cloud is on `navigation-migration`; both are
  heavily dirty/shared, and no existing change was reverted, overwritten, cleaned, staged, committed, or otherwise
  mutated by I44

Applicable business contract:

1. Fast expected exit is only an acceleration probe, not final business truth.
2. No extra pre-return full-radar confirmation, observation, TTL, retry, cleanup, or negative-business-truth may be added.
3. Absent/stale exit remains `false`; only a fresh armed pending exit returns `true`.
4. Deferred recovery must remain pending while the trusted state still says `IN_COMBAT`.

`无已批准业务差异；按基线等价迁移`.

## Prior Blocker Resolution

The earlier delivery preflight correctly blocked this same count unit because active Cloud then had no task caller
that selected `FAST_EXPECTED_EXIT`. That source-graph prerequisite now exists:

- `WubeiTask.java:3746-3748` calls `AutoCombatService.handleCombatTick(..., FAST_EXPECTED_EXIT)` from
  `tickWaitBattleFinish`; `3749-3758` consumes `EXIT_RECOVERED` and continues to `POST_BATTLE_RECOVER`.
- `XiuluoTaskV2.java:2055-2056` calls `handleCombatTick` with
  `postCombatRecoveryPolicyForXiuluoWait(state)`; `2147-2151` selects `FAST_EXPECTED_EXIT` only for an entered
  Xiuluo battle whose source is `TRACKER_CONFIRM`.
- Both active Cloud Task files are byte-exact to the fixed business baseline:
  `WubeiTask.java` blob `7c85ca645494623f102ca0ccd873bb4ef74e41c3`,
  `XiuluoTaskV2.java` blob `a010a0f5b267b02e0b202c2addf4a8bcc2c9600f`.

This resolves the old source-caller P1 without changing Task, runtime, poller, or host code. The whole Task execute
units retain their separately recorded runtime/turn/event boundary blockers; I44 neither claims nor modifies those
units. This count unit starts at the explicitly assigned active `AutoCombatService` branch.

## Active Caller To Closed Result

1. **FAST arm before observation** - `AutoCombatService.java:126-150` normalizes the policy, arms exactly once at
   `135-137`, then runs the existing avatar probe/full-radar fallback. There is no new observation in the consume unit.
2. **Active branch** - `AutoCombatService.java:345-355` selects
   `consumeCombatExitSignalForExpectedWait(source)` only for `FAST_EXPECTED_EXIT`; `false` closes immediately with no
   recovery continuation.
3. **Arm boundary** - `BattleRadarService.java:221-232` records
   `expectedCombatExitWaitArmedAtMs=now` and clears an already-pending exit at or before the arm instant. This is the
   baseline stale boundary, not a TTL.
4. **Fresh pending production** - `BattleRadarService.java:331-350` creates the one-shot pending exit only on the
   remembered `IN_COMBAT -> FREE` transition, stamping `combatExitPendingAtMs` and diagnostic
   `combatExitPendingBattleCount`. `354-369` clears an old exit pending on the next combat enter.
5. **Closed consume** - `BattleRadarService.java:416-434` returns `false` when no pending exists; unarmed or older
   pending clears all three pending fields and returns `false`; fresh `pendingAt >= armedAt` clears the same fields and
   returns `true`. `pendingBattleCount` remains diagnostic only, exactly as in `696a12b0`; no new battle-count gate was
   invented.
6. **Recovery continuation** - after `true`, `AutoCombatService.java:358-377` clears its local arm flag, records panel
   exit/reset state, keeps the existing common-box detection, sets deferred leader recovery for the fast policy, and
   returns `true`; `handleCombatTick:155-160` closes as `EXIT_RECOVERED`.
7. **Still-in-combat guard** - `AutoCombatService.java:442-473` leaves deferred recovery pending and returns `false`
   while `ActionState.IN_COMBAT`; only a later safe point consumes the existing first-aid/incense continuation.

## Existing Typed DHXY Observation Reuse

The target consume method performs no screenshot/fact read. Its pending input is produced by the already-existing
typed radar chain:

- Cloud `BattleRadarService.checkAndSyncCombatState` reads the existing signal/minimap facts; the fast path reads the
  existing avatar baseline/probe facts. `readFact:481-502` distinguishes `OBSERVED`, `NOT_EXECUTED`, `STOPPED`, and
  failure terminals; it does not turn failure into a negative combat fact.
- DHXY `LocalRemoteGameCommandHandler.java:846-882` dispatches all seven BattleRadar fact kinds through the exact
  bound `WindowTaskContext`; `885-912` rechecks timeout, registration, and binding before returning `OBSERVED`.
- DHXY handler mapping at `1083-1140` is closed and one-to-one: capture/mechanics failure is not mapped to
  `NOT_VISIBLE`, `UNREADABLE`, or `UNCHANGED`.
- DHXY `BattleRadarLocalObservationMechanics.java:89-180` reuses exact-binding signal/minimap capture and
  `240-299` reuses the existing avatar observation. Avatar baseline ownership remains keyed by
  `windowId + nativeHandle + playerIdentityEpoch` at `270-271/387`.

No DHXY source, capture, input, handler, protocol, Task, runtime, poller, host, UI, or test file was changed.

## Baseline Equality Evidence

Normalized method-block comparison against `696a12b0`:

| Method block | Exact | SHA-256 |
|---|---:|---|
| `BattleRadarService::armExpectedCombatExitWait` | yes | `5ce3595b0d966a309a17f8ba1ce7da5092393fa90cf380f3e315d7ae7954a860` |
| `BattleRadarService::consumeCombatExitSignalForExpectedWait` | yes | `79b438d376368d48806b1f18cd79f7dc86c9410baab2487980c64dedb0f2eba9` |
| `AutoCombatService::consumeExitAndRecover` | yes | `3a59c6cd1ee881410e8052a0f9c961a1cef9de5f26efc442df04abff4e1d5aa1` |

## Delivery

- Result: `NO_CODE_CHANGE`; the active source chain is complete for the assigned count unit.
- Worker self-QA found no required Java change and no write-set blocker. This is evidence, not reviewer approval.
- Requested count result: `countDelta=+1`, pending the parent manager/final reviewer and its unified fresh build/ledger
  update. I44 did not edit the ledger, CR card, dashboard, or any shared planning file.
- Maven/tests/runtime/application/server/host/poller/UI/capture/input were not run, as explicitly required.
- Git mutation was not performed.

## Parent Source Review #1 - 2026-07-15T05:28:00-04:00

父级独立复核方法块与 typed radar producer，确认方法自身保持 baseline；但 worker 把“Task 文件已落盘”误作
active caller 解锁。当前 `WubeiTask`/`XiuluoTaskV2` 因缺 `WindowReadyEventBus`、合法 task runtime/turn 与永久本地
Service typed boundaries 仍不能通过 Cloud package/运行，AutoBattle 正常链又从不选择 `FAST_EXPECTED_EXIT`。
因此当前没有可执行 production caller 到本方法，不能以 non-compiling source caller 计数。

结论：**P0=0/P1=1/P2=0，BLOCKED_MISSING_RUNNABLE_CLOUD_CALLER，countDelta=0**。Java 保持不变；
待任一 Wubei/Xiuluo whole Task execute 真正通过 typed boundary 与 fresh package 后，原地复核即可，不得复制
Runner owner、伪造 caller 或自调用。无已批准业务差异。
