# W-PSS-SUPPLY-PUBLIC-CHAIN-IMP1

## CLAIMED

- task: `W-PSS-SUPPLY-PUBLIC-CHAIN-IMP1`
- claimedAt: `2026-07-14T09:48:45.3398516-04:00`
- worker: `Internal CG`（实现 Worker，非 reviewer）
- unique write set:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-player-state-public-chain-worker-cg.md`
- baseline: DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f` 的 `PlayerStateService`
- business differences: `无已批准业务差异；按基线等价迁移`

## Implementation Result

- finalAt: `2026-07-14T10:16:32.0012550-04:00`
- status: `IMPLEMENTED / COMPILE PASSED`
- Java source: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
- pre-edit SHA-256: `7BC1172AF264C8FB7D71D3C66EB601ACB0099605EF7250E84AE5DEBC32FB9991`
- final SHA-256: `A562AB9FD9C7295C1A4BD49870B09D77EADA161E632DB91A6331E6E9D55A5786`
- final size: `1540` lines / `78774` bytes
- implementation role: `Internal CG`，只做实现与自审，不构成 reviewer approval。

## Public Definitions

公开可调用闭包共 1 个 constructor、24 个 public method、1 个 public enum：

1. `PlayerStateService(TaskExecutionContext, CloudPlayerSupplySettings, SheyaoxiangStatusDecisionFacade, long)`
2. `resetCheckCounter()`
3. `performStartupFirstAidCheck(TaskExecutionContext)`
4. `prepareStartupFirstAidNoFocus(TaskExecutionContext, String)`
5. `performStartupFirstAidCheckFromPrecheckOrRun(TaskExecutionContext, long)`
6. `probeAndConsumeHealthyFirstAidNoFocus(TaskExecutionContext, String)`
7. `probeFirstAidSupplyNoFocus(TaskExecutionContext)`
8. `hasPendingNoFocusFirstAidPlanForCurrentWindow()` 及 exact-context overload
9. `performCachedFirstAidPlanNow(TaskExecutionContext)`
10. `areStatusBarsVisibleNoFocus(String)` 及 exact-context overload
11. `healAll()` / `healAll(TaskExecutionContext)`
12. `healPlayer()` / `healPlayer(TaskExecutionContext)`
13. `healPet()` / `healPet(TaskExecutionContext)`
14. `checkAndHeal(...)` 两个 bound-context overload 及 exact-context overload
15. `ensureSheYaoXiangActive()` / `ensureSheYaoXiangActive(TaskExecutionContext)`
16. `ensureSheYaoXiangActiveForLeaderTask(String)` / exact-context overload
17. `FirstAidNoFocusProbeResult { SUPPLY_NEEDED, HEALTHY, ALREADY_DONE, UNKNOWN }`

## Real Call Graph

- startup -> no-focus probe -> typed `WINDOW_CLIENT_PX` bar capture -> committed four-bar classifier ->
  `HEALTHY` consumes quota，`SUPPLY_NEEDED/UNKNOWN` retains exact/conservative plan -> one ordered
  `InputBundle` consumes the plan。
- precheck consume -> freshness check -> healthy/already-done skip；low/unknown consumes cached plan；cache
  missing/stale falls back to the committed foreground startup chain。
- `healAll` -> one initial typed bar-strip capture -> committed threshold/higher-sample checks -> one 350ms
  confirmation capture for every initial low target -> one ordered supply bundle。
- `healPlayer` / `healPet` -> committed enabled-target order -> one typed 1x1 capture per target, with no added
  confirmation read -> one ordered supply bundle。
- every ordinary supply bundle -> ordered `CLICK_RIGHT(delay=100)` + `SLEEP(800)` per target -> one safe
  `MOVE_MOUSE` + `SLEEP(300)` tail, all in `WINDOW_CLIENT_PX`。
- status-bar visibility -> typed exact-window strip capture -> committed red/blue pixel count gate，no input。
- incense -> committed quiet-window gate -> canonical Cloud `TICK` -> optional typed status ROI capture ->
  canonical Cloud `STATUS_IMAGE` decision -> only `USE_INCENSE` invokes closed local
  `BAG_USE_INCENSE` macro -> typed `USED/NOT_FOUND` outcome report；macro result never decides whether use was due。
- leader incense -> exact-context support-member guard -> full incense chain。

## Baseline Comparison

- Threshold normalization remains `<=40 -> 30`, `<=60 -> 50`, otherwise `70`；candidate order remains
  人物血、人物法、宝宝血、宝宝法。
- No-focus outcomes, once-per-idle quota, `UNKNOWN` conservative all-enabled plan, startup cache/fallback,
  max-age semantics, cache-clear timing, and incomplete-bundle quota consumption match committed `0114604e`。
- `healAll` retains the committed initial strip read, higher-threshold false-positive guard, one 350ms second
  read per initial low target, and second-read failure skip。`healPlayer/healPet` retain their distinct committed
  single-pixel/no-second-read policy。
- The assignment-mandated mechanical projection collects already-decided ordinary clicks into one typed ordered
  `InputBundle`; target order and `100/800/300ms` delays are unchanged。Captures remain separate typed
  `CloudGameClient` operations because `InputBundle` contains input actions only。
- Incense constants remain duration `59m`, refresh threshold `20m`, quiet margin `2m`, success animation wait
  `1000ms`。Cloud still owns presence/remaining/use decisions and cached facts；local only executes the closed macro。
- Stable service binding uses scope/taskRunId/taskType/window/native tuple/stopEpoch。`runRevision` is deliberately
  accepted across pause/resume；`playerIdentityEpoch` drift reaches the committed state-reset branch instead of
  being rejected before reset。
- The v1 incense DTO still requires both ROI fields。Cloud has no screen-base authority, so
  `screenAbsoluteRoi` carries the same non-authoritative client rectangle solely for DTO compatibility；the
  recognizer consumes ROI-relative uploaded pixels and no input reads that field。
- `无已批准业务差异；按基线等价迁移`。

## Explicit Exclusions

- Identity/position cohort remains later work: `syncMyIdentity`、`syncMyPosition`、`syncAll` were not added。
- `ensureSheYaoXiangActiveInOpenMainBag(BagService.MainBagSession, ...)` was not projected：Cloud has no local
  `MainBagSession` type，and this assignment requires the existing closed `BAG_USE_INCENSE` macro to own bag mechanics。
- The committed but unreferenced private `performFirstAidCheck(...)` was not exposed or recreated。
- No host/Task/UI/assembly/caller wiring was added；no shared remote schema or other Java source was edited。
- No HWND object, tracker/title lookup, local template/OCR, local input queue/provider, owner/session/ledger,
  new TTL, auto retry, or local decision fallback was introduced。
- No tests were created or run；no application, server, worker runtime, or game-input surface was started。

## Compile Gate

- Initial integration compile: `mvn -q compile`, exit `1`。Errors were isolated to parallel writers' files:
  remote local-macro constructor arity at `RemoteGameCommandBroker:1905`,
  `RemoteCommandOutcomeEnvelope:265`, `RemoteProtocolDigests:56`; and then-missing symbols
  `TaskMaintenanceService.invalidateTeamCombatPhaseForLeader`,
  `TaskMaintenanceService.confirmTeamCombatPhaseExitedForLeader`,
  `BattleRadarService.refreshFastExpectedCombatExitAvatarBaseline` referenced by `AutoCombatService`。
  No foreign file was modified or stubbed；the worker waited as instructed。
- Final command: `mvn -q compile`
- Working directory: `D:\mavenProject\dhxy-cloud-brain`
- Final exit: `0` (quiet output, `4.4s`)
- `clean` was not used。`PlayerStateService.class` timestamp
  `2026-07-14T10:15:56.2183826-04:00` is newer than final source timestamp
  `2026-07-14T10:15:10.6640841-04:00`，confirming the updated class was generated。

## Parent Source Review #1 - 2026-07-14T10:22:56-04:00

**PARTIAL SOURCE APPROVED / BLOCKED，P0=0/P1=2/P2=0。** startup/no-focus plan、cached-plan、
single-bar `checkAndHeal`、status visibility 与摄妖香 typed macro 主链可以保留；但 `healAll`、`healPlayer`
和 `healPet` 尚未按 committed `0114604e` 保持 capture/input 的原子边界与执行顺序，因此本类不能计入
`189/407`。

1. **P1：`healAll` 丢失 baseline 的整段 exclusive，并把“确认后立即补给”改成“全部确认后统一补给”。**
   committed `PlayerStateService.java:534-565` 在一个 `submitExclusiveAndWait` 内按人物血、人物法、宝宝血、
   宝宝法顺序执行；`:1089-1139` 每个初判低目标等待 350ms、二次截图，确认仍低后在 `:1136` 立即进入
   `healIfUnhealthy`，其右键 100ms + sleep 800ms 完成后才检查下一个目标。当前 Cloud
   `PlayerStateService.java:610-649` 先完成所有目标的二次截图并积累 `confirmedTargets`，到 `:654` 才在
   `:703-735` 发一个点击 bundle。影响是后一个确认到真正点击之间出现额外截图/网络等待，确认事实可能变旧，
   且其它窗口输入可插入原本整段 exclusive 的 capture/click 链。

   **精确返修条件：** 用一个 shared closed local macro 表达 `HEAL_ALL` mechanics；Cloud 只传四目标 enabled/
   threshold 与 exact context。DHXY 在单一 input-worker exclusive callback 内保持初始 bars capture、四目标顺序、
   higher-sample guard、每目标 350ms 二次确认、确认后立即右键 100ms + sleep 800ms、全部结束后仅在实际补给过时
   safe-move 的原顺序。不得在 callback 内嵌套 `InputSequences.submit*`，不得新增 TTL/retry/owner/session/ledger。

2. **P1：`healPlayer/healPet` 把逐目标 `capture -> 立即 click/sleep/safe-move` 改成先 capture 全部目标。**
   committed `:573-596` 逐目标调用 `checkAndHeal`，`:715-730` 每次读取一个像素后立即进入
   `healIfUnhealthy`；`:1172-1227` 每个低目标自己的 click/sleep（以及 baseline safe-move）结束后才返回并读取
   下一个目标。当前 Cloud `:661-692` 先读取全部选择目标，到 `:696` 才统一执行 clicks，并且 safe-move 只剩一次。
   影响是第二个目标读取结果在第一个目标输入后才真正使用，时序、hover cleanup 与多窗口交错均不等价。

   **精确返修条件：** 复用同一个 closed first-aid macro，以 `PLAYER` / `PET`（或显式 target list）模式在本地
   按 baseline 顺序逐目标完成 capture、判色、立即 click/sleep/safe-move 后再进入下一目标。Cloud 仍拥有 enabled/
   threshold 与调用编排，本地只做 exact-window 机械观察和固定输入并返回 typed terminal。

External D 当前拥有 shared `LOCAL_MACRO` 双仓写集；CG 不得并发修改 D 文件。请原 CG 先停止 Java 写入并保留
当前可保留部分，待 D 交付并释放写集后由父级恢复同一 CG 做这两个精确返修。Worker compile exit 0 仅证明当前
源码可编译，不消除上述业务顺序差异；fresh package 仍等全部 writer 稳定。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## SUSPENDED_WAITING_SHARED_MACRO - 2026-07-14T10:24:34.3626276-04:00

- 已完整读取 `Parent Source Review #1` 到报告 EOF；确认保留当前获准保留的 startup/no-focus plan、
  cached-plan、single-bar `checkAndHeal`、status visibility 与摄妖香 typed macro 主链。
- 保持原 claim/write set 不变：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java` 与本报告。
- 本轮零新 Java、零 Java 修改；未触碰 External D 持有的 shared `LOCAL_MACRO` 双仓写集。
- 状态：`SUSPENDED_WAITING_SHARED_MACRO`。等待 D 释放写集后，由同一 CG 按父级 exact repair 将
  `healAll` 与 `healPlayer/healPet` 迁为 closed first-aid local macro，恢复 committed `0114604e` 的
  `capture -> immediate input` 顺序。

## Parent Strategy Reset #1 - `W-696-PSS-WHOLE-1` - 2026-07-14T11:09:00-04:00

恢复 CG，但旧 supply/public slice 停止作为完成单位。唯一任务：用
`git show 696a12b0:src/main/java/com/bot/dhxy/service/PlayerStateService.java` 对 Cloud 同路径做整类等价迁移；
全部 public/private、逐目标 capture 后立即 input 的顺序、first-aid/supply/incense/cache/delay 必须保留。本地调用
仅在原位置换 typed operation，不得 batching/reorder。唯一写集仍为 Cloud `PlayerStateService.java` 与本报告。
交付全方法 disposition、local substitution 表、SHA 与非 clean compile。

## RESUMED_CLAIMED - `W-696-PSS-WHOLE-1` - 2026-07-14T11:13:37.8859762-04:00

- role: `Internal CG` implementation worker，非 reviewer。
- authoritative baseline: DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
  `src/main/java/com/bot/dhxy/service/PlayerStateService.java` 完整类。
- unique Java write set:
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`。
- only other write: append 本固定报告。
- repository snapshot: DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`；
  Cloud `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`；两仓既有大量并行
  dirty/untracked，全部保护，不回滚、覆盖、清理、提交、切分支或做其它 Git mutation。
- business gate checked: `docs/业务逻辑.md` 的五倍移动中摄妖香/医宝宝边界及修罗
  `696a12b0` 业务基线条目；`无已批准业务差异；按基线等价迁移`。

## HALTED_BY_WHOLE_COPY_SEQUENCE - 2026-07-14T11:21:30.2114502-04:00

- parent sequence: 先将 `696a12b0` 的全部 32 个 `service/**` 完整原样放入 Cloud；再删除永久本地的
  `BagService` / `UICleanerService` / `GiveItemService` / `QuestManagerService`；随后按编译错误补
  Cloud -> 本地调用边界；最后统一抽离其余动作。
- status: 已立即停止当前 active Cloud Service Java 修改，等待父级新任务；未回滚、删除、覆盖或清理
  任何已落盘代码。
- Java file: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
- Java lastWriteTime: `2026-07-14T10:15:10.6640841-04:00`
- Java current SHA-256: `A562AB9FD9C7295C1A4BD49870B09D77EADA161E632DB91A6331E6E9D55A5786`
- this resumed pass: 零新 Java、零 Java 修改；未运行 compile/clean/runtime/tests。
