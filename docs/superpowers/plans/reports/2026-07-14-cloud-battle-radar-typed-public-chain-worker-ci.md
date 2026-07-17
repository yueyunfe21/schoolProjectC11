# Cloud BattleRadar Typed Public Chain - Internal Worker CI

## Parent Task Brief #1 - `W-BRADAR-TYPED-PUBLIC-CHAIN-IMP1` - 2026-07-14T10:27:00-04:00

### Objective

Directly implement the three remaining committed `0114604e` BattleRadar public workflow entries as Cloud
typed-consumer overloads. This is implementation work, not a Design round. DHXY permanently retains battle capture,
template matching, minimap readability observation, avatar ROI capture/diff, watcher scheduling, and all physical input.
Cloud owns the existing action-state transition, signal priority, exit-miss counter, fast-exit timing, and terminal
result.

### Unique Write Set

- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\BattleRadarService.java`
- this append-only report

Do not modify DHXY Java, shared remote/schema, `AutoCombatService`, caller/host/assembly, tests, or another worker's
report. You are not alone in the worktree: preserve all dirty/untracked and concurrent edits; do not revert, clean,
commit, or overwrite another writer.

### Required Public Entries

1. `checkAndSyncCombatState` typed overload: consume one passive observation of the baseline Stage 1 auto flag,
   Stage 2 selection capture/signal, Stage 3 top capture/signal, and Stage 4 minimap readability. Preserve this exact
   priority, reset misses when a combat signal recovers, keep `IN_COMBAT` on Stage 2/3 capture failure, require the
   committed two consecutive misses plus readable minimap before exit, and preserve the original boolean return.
2. `checkFastExpectedCombatExitByAvatarDiff` typed overload: consume a closed local result such as
   unavailable/baseline-captured/unchanged/changed. Preserve the `IN_COMBAT` guard, first-baseline behavior, 15-second
   combat-age gate, 1-second probe interval, unavailable=false, and only changed=>`updateCombatState(false)`.
3. `refreshFastExpectedCombatExitAvatarBaseline` typed overload: consume the local refresh success/failure; clear the
   old readiness first, require `IN_COMBAT`, and on success preserve combat-start initialization plus last-probe time.

Use the smallest shared passive nested record/enum needed by all three entries; do not create one DTO per method and
do not add wrapper chains. Since pixel comparison remains local, replace the currently unusable Cloud
`BufferedImage fastExpectedExitBaselineImage` placeholder with minimal readiness state if needed. Restore the exact
committed `markCombatSignalSeen` behavior. Keep current public helpers and transition/signal semantics unchanged.

### Prohibitions

- No HWND, image path, `BufferedImage`, capture/template/OCR/minimap reader/input provider in Cloud.
- No new owner/permit/session/ledger/parent-child/compaction/durable workflow/business TTL/automatic retry.
- No fixed success, stub, zero-Java delivery, no-arg method that pretends to observe the client, or production wiring.
- No behavior change to 2 misses, 15s/1s/4s timing, signal consumption, pause markers, battle count, or fallback order.

### Delivery

Append `CLAIMED`, then implement directly. Run Cloud `mvn -q compile` without `clean`; concurrent D files may cause
foreign errors, which must be reported by exact file and never edited. Append public API/call graph, baseline mapping,
changed files, SHA-256, compile result, and `无已批准业务差异；按 0114604e 基线等价迁移`. Parent is the only reviewer.

## CLAIMED - 2026-07-14T10:36:04-04:00

- Worker: Internal CI（implementation only；不承担 reviewer/approval）
- Task: `W-BRADAR-TYPED-PUBLIC-CHAIN-IMP1`
- Write set accepted: Cloud `BattleRadarService.java` + 本固定报告 append-only。

## IMPLEMENTED - 2026-07-14T10:44:33-04:00

### Worker status

- Role remains Internal CI implementation Worker only；本节不构成 reviewer/approval。
- 已直接实现 Parent Task Brief #1 的三个 baseline-name typed-consumer public entry；未写 Design，未接 caller/host/assembly。
- 保护两个仓库既有 dirty/untracked；未 revert、clean、commit、stage 或覆盖其他 writer。

### Baseline and source identity

- DHXY baseline branch/commit: `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`。
- committed `BattleRadarService.java` blob: `52b439febeb01b4d9ddb4afb4e00a04076c1aa4f`。
- Cloud branch/HEAD during implementation: `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`。
- Cloud target was already untracked before this task and remains untracked；pre-edit SHA-256: `BD65920071FA392C6074D97A1BA404C2BA39835EEEA4C10876A9FB01C5CC03BC`。
- Final Cloud target SHA-256: `8B1B4AF439BC8F29B223C1009714E4C61731C3F929F2275E38ED370CA34D7D1D`（528 lines / 23612 bytes）。

### Public API and call graph

- `CombatObservation(boolean autoFlagVisible, boolean selectionCaptureSucceeded, boolean selectionSignalVisible, boolean topCaptureSucceeded, boolean topSignalVisible, boolean minimapReadable)`：仅承接 DHXY 已完成的被动观测事实。
- `AvatarObservationResult`：`UNAVAILABLE` / `BASELINE_CAPTURED` / `UNCHANGED` / `CHANGED`；Cloud 不持有头像像素或图片。
- `checkAndSyncCombatState(CombatObservation)`：`state()` -> Stage 1 自动战斗标志 -> Stage 2 选择区 capture/signal -> Stage 3 顶部区 capture/signal -> Stage 4 miss/minimap gate -> `updateCombatState(false)`。任一信号恢复时调用 committed 等价的 `markCombatSignalSeen(...)` 清零 miss；Stage 2/3 capture 失败且当前 `IN_COMBAT` 时保持战斗态；仅连续 2 misses 且 minimap readable 才允许退出。
- `checkFastExpectedCombatExitByAvatarDiff(String, AvatarObservationResult)`：`state()` -> `IN_COMBAT` guard -> 首次 baseline readiness -> 15s combat-age gate -> 1s probe interval -> 仅 `CHANGED` 调用 `updateCombatState(false)`；`UNAVAILABLE` / `UNCHANGED` 均返回 false。
- `refreshFastExpectedCombatExitAvatarBaseline(String, AvatarObservationResult)`：先清旧 readiness -> `IN_COMBAT` guard -> 仅 `BASELINE_CAPTURED` 成功 -> 保留 combat-start 初始化及 last-probe timestamp。
- `javap -classpath target/classes com.bot.dhxy.service.BattleRadarService` 已确认上述三个 public signature 出现在本次 freshly compiled class 中。

### `0114604e` baseline mapping

- committed lines 77+ `checkAndSyncCombatState()`：原 Stage 1/2/3/4 优先级、capture-failure keep、`markCombatSignalSeen` 和 two-miss + readable-minimap exit gate，迁移为 typed facts consumer；capture/template/minimap read 均留在 DHXY。
- committed lines 154+ `checkFastExpectedCombatExitByAvatarDiff(String)`：原 `IN_COMBAT`、first-baseline、15s、1s、unavailable=false、changed-only transition 等价保留；avatar ROI capture/diff 留在 DHXY。
- committed lines 242+ `refreshFastExpectedCombatExitAvatarBaseline(String)`：原 clear-first、combat guard、成功后 start/probe timestamp 等价保留；Cloud 仅以 readiness boolean 取代不可用的 `BufferedImage` placeholder。
- committed lines 323+ `markCombatSignalSeen(String)`：恢复原有 recovered-miss 日志与 miss 清零行为。
- 既有 `FULL_RADAR_INTERVAL_MS = 4_000`、signal consumption、pause marker、battle count、transition helper 和 fallback helper未改。
- Cloud imports 仅为 `GameContext`、`TaskExecutionContext`、Lombok、`Map`、`ConcurrentHashMap`；无 `BufferedImage`、HWND、路径、capture/template/OCR/minimap reader/input provider。
- 无已批准业务差异；按 0114604e 基线等价迁移。

### Changed files

- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\BattleRadarService.java`
- `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-battle-radar-typed-public-chain-worker-ci.md`（append-only）
- 未修改 External D shared remote/schema、`AutoCombatService`、caller、DHXY Java、tests 或其他报告。

### Compile evidence

- Command: `mvn -q compile`，cwd `D:\mavenProject\dhxy-cloud-brain`，未使用 `clean`，未运行 tests。
- 首次执行被并行 External D 暂时阻塞，唯一诊断为 `src/main/java/com/bot/dhxy/service/NavigationService.java:[87,24]`：`NavigateInCurrentMapMacroCommand` 构造器 required 14 args（`String,Integer,Integer,String,boolean,int,boolean,int,String,String,Integer,Integer,long,boolean`），caller found 4 args（`int,int,int,String`）。该次没有 `BattleRadarService.java` 诊断；本 Worker 按写集约束未修改该文件。
- External D 随后在并行工作树自行消除上述 mismatch；fresh rerun `mvn -q compile`：`exit 0`，wall time 4.9s，无编译输出。
- 产物复核：三个目标 public method 均各定义一次并出现在 `target/classes`；最终源码 SHA-256 仍为 `8B1B4AF439BC8F29B223C1009714E4C61731C3F929F2275E38ED370CA34D7D1D`。

## Parent Source Review #1 - `W-BRADAR-TYPED-PUBLIC-CHAIN-IMP1` - 2026-07-14T10:49:59-04:00

**SOURCE APPROVED，P0/P1/P2=0；本块可保留，但尚不计整类 same-path 完成。** 父级逐行对照
committed `0114604e:src/main/java/com/bot/dhxy/service/BattleRadarService.java:77-140,154-201,242-265,323-330`
与 Cloud `BattleRadarService.java:42-208,412-419`：

- `checkAndSyncCombatState(CombatObservation)` 保持 auto flag -> selection -> top icons -> exit gate 的固定优先级；
  selection/top capture 失败且当前 `IN_COMBAT` 时仍立即保留战斗态；信号恢复仍先清 `combatExitMisses`；只有连续
  2 次 miss 且 minimap readable 才调用既有 `updateCombatState(false)`。`null` observation 的返回值与“全部事实
  unavailable”在 baseline 下的当前状态结果一致，不建立新的成功或退出事实。
- `checkFastExpectedCombatExitByAvatarDiff` 保持 `IN_COMBAT` guard、首次 baseline、15 秒 combat-age、1 秒 probe
  interval、unavailable=false、仅 changed 触发退出；probe timestamp 的更新位置与 baseline current-frame capture 前一致。
- `refreshFastExpectedCombatExitAvatarBaseline` 保持 clear-first、`IN_COMBAT` guard、capture failure 保持未 ready，
  成功后初始化 combat start 并更新 last-probe；boolean readiness 只替代不可上云的 `BufferedImage` 本体。
- `markCombatSignalSeen`、`updateCombatState`、enter/exit signal、battle count、pause marker 与 4 秒 full-radar fallback
  未被本 Worker 改写；Cloud 未引入 capture/template/OCR/minimap reader/input。

本结论只批准三个 typed-consumer 代码块及其最小 passive enum/record。按用户最新确认的整类复制门，必须在后续
`public caller -> 完整 Cloud BattleRadarService -> typed DHXY observation -> terminal result` 真链接通并完成整类
基线对照后，才能增加 `approved same-path` 计数；不得再用方法数或 helper 数宣称整类完成。父级 fresh
`mvn -q clean package` 等当前 A/C/D 等 Java writer 稳定后统一执行。

无已批准业务差异；按 committed `0114604e` 等价迁移。
