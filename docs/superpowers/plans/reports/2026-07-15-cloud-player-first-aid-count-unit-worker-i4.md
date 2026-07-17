# Cloud Player First-Aid Count Unit Worker I4

## CLAIMED

- task: `W-COUNT-PLAYER-FIRST-AID-WHOLE-1`
- role: Internal Count Worker I4, implementation only; not a reviewer
- claimedAt: `2026-07-15T00:57:29.3757155-04:00`
- countUnit: `PlayerStateService::performStartupFirstAidCheck`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- baseline Service blob: `096d8917b0372422b3ed141300419f9b71c1392c`
- branches: DHXY `thin-client-design` at `0114604e1ff5f15491d2910959c45252e893d04f`; Cloud
  `navigation-migration` at `3b988caa010254973e03342272e6d1d6a9685b01`
- pre-edit SHA-256: DHXY `PlayerStateService.java`
  `2CF9DD766A86C3F8C9747176B52A39EC79E20A4AD5F648FA87EA506BD0681A1E`; Cloud
  `PlayerStateService.java` `6954F8EF93083536B3565938931BFC80E8B222D15F01325D0B8C39667A3F738B`;
  Cloud `CloudPlayerStateFirstAidPort.java`
  `51D614EB8445F6D7C9D0F3E798EA971A4ADF73A03035EFBC8D6A99B68CC89B9B`
- shared worktree: both repositories are heavily dirty/untracked; this worker will not revert, overwrite, clean,
  stage, commit, run Maven/tests, or start runtime/application/server/Task/poller/UI/capture/input
- frozen lane: `incense-status` / `ensureSheYaoXiangActive` is `BLOCKED_SHARED_LANE` and outside this task
- gate: only parent source review plus the parent's fresh applicable builds may apply `countDelta=+1`

## Investigation Gate

Before Java edits, this worker will compare the complete `696a12b0` startup/first-aid public/private method graph
against the real Cloud `AutoBattleTask:135` caller, current Cloud `PlayerStateService`, the existing
`CloudPlayerStateFirstAidPort` command/result contracts, and DHXY exact-window continuous mechanics/handler.
DHXY Java may be touched only if that read proves one precise first-aid-specific typed-chain gap and the evidence is
recorded here first.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 / Next Count Task - 2026-07-15T01:12:00-04:00

父级独立复核 relocation、active bean scan、旧路径和完整 caller：旧 adapter 路径已不存在，新类位于固定
`com.bot.dhxy.service` 扫描根且仅一个 implementation；`PlayerStateService` 只改 import。三 operation、exact current
context、前后 checkpoint、kind/timeout、EXECUTED typed operation 校验、NOT_EXECUTED/STOPPED/UNKNOWN 接受域与既有
DHXY single-exclusive mechanics 均未漂移。手工展开原 package-private `require` 保持同一条件与异常类型。

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。**
`countUnit=PlayerStateService::performStartupFirstAidCheck` 仅在统一 fresh Cloud package 通过当轮 `+1`；ledger 暂不动。

下一任务另记固定报告 `docs/superpowers/plans/reports/2026-07-15-cloud-left-top-combat-maintenance-count-unit-worker-i4.md`：
`W-COUNT-LEFT-TOP-COMBAT-MAINTENANCE-1`，`countUnit=LeftTopStatusSwitchService::handleCombatMaintenance`，
`countDelta=+1`。一次闭合真实 `AutoCombatService:672/683 caller -> Cloud LeftTopStatusSwitchService -> existing
typed left-top observation/click port -> DHXY exact-window template mechanics/single input queue -> closed SwitchActionResult`，
保留 696 combat-maintenance task-code gate、open/closed/unknown 判断、点击、delay/fallback/state。唯一 Java 写集：Cloud
`LeftTopStatusSwitchService.java` + service-specific lefttop adapter（仅必要时）+ 新报告；caller、DHXY、generic shared 12、
其它 Service 冻结。现有真链已闭合可交源码证据而不造重复 Java；父级源码审查 + fresh build 通过同轮才 `+1`。

## 696 Public / Private Method Map

| `696a12b0` method/type | Count-unit disposition |
|---|---|
| `resetCheckCounter` | Cloud 保留 per-window `checksDoneThisRound=0` 与 `lastCombatExitTime=now`。 |
| `performStartupFirstAidCheck` | Count unit；保留 checkpoint、两状态字段归零、启动日志，并以 `ignoreTimeInterval=true` 进入原检查。 |
| `performFirstAidCheck` | 保留 `MAX_CHECKS_BETWEEN_BATTLES=1`、窗口可用门、`ignoreTimeInterval`/5000ms 门、四 toggle/threshold 日志、检查后计数与结果日志。 |
| `healAll(TaskExecutionContext)` / `healAll()` | 前后 checkpoint 与一次完整四目标事务保持；本地 direct mechanics 替换为 `HEAL_ALL` typed macro。 |
| `healAllDirect` | DHXY continuous mechanics 持有：一次 input-worker exclusive callback 内 capture、四目标顺序、确认、点击和结果。 |
| `probeAndConsumeHealthyFirstAidNoFocus` | Cloud 保留 HEALTHY 才计数并清 pending；其它结果不伪装健康。 |
| `probeFirstAidSupplyNoFocus` | Cloud 保留 checks gate、UNKNOWN/HEALTHY/SUPPLY_NEEDED、plan 建/清和同帧 base；DHXY `PROBE_SUPPLY_NO_FOCUS` 只返回有序观察。 |
| `performCachedFirstAidPlanNow` | Cloud 保留先取并清 plan、空/无 base 分支、age 日志、terminal 日志、消费后计数与 `return true`；DHXY `EXECUTE_CACHED_PLAN` 执行有序点列。 |
| `performCachedFirstAidPlanDirect` | DHXY mechanics 保留 live binding refresh、stored-base fallback、随机安全点、300ms、逐目标右键 100ms + settle 800ms。 |
| `findSupplyTargetsFromSnapshot` / `addSupplyTargetIfNeeded` / `isSupplyNeededFromSnapshot` | Cloud 仍决定 enabled/threshold/target/plan；typed observation 固定四 bar，Cloud 只选择 `SUPPLY_NEEDED`。 |
| `checkAndHealFromSnapshotIfEnabled` / `checkAndHealFromSnapshot` / `healIfUnhealthy` | DHXY mechanics 保留 disabled gate、30/50/70 normalize、+10 probe、350ms 二次确认、100ms right-click、800ms settle。 |
| `calculateX` / `normalizeThreshold` / `isHealthyInSnapshotArea` / `isHealthyColor` | 数学、2-pixel healthy 门及 RGB 公式保持 696。 |
| `state` / `PlayerRuntimeState` | Cloud 保留 per-window/identity-epoch state、check counter、combat-exit time 与 pending plan；无新 owner/TTL。 |
| `FirstAidPlan` / `FirstAidTarget` / `FirstAidNoFocusProbeResult` | Cloud 保留 plan/result/state；目标固定为人物血、人物法、宝宝血、宝宝法，顺序不变。 |
| `healPlayer` / `healPet` / `areStatusBarsVisibleNoFocus` | 不在本 count caller 闭包，保持冻结；本任务未改。 |
| `ensureSheYaoXiangActive*` / incense methods | `BLOCKED_SHARED_LANE`，完全冻结；本任务未改。 |

## Implementation

只修复上文记录的 Spring reachability 缺口：既有 `CloudPlayerStateFirstAidPort` 从未被
`CloudServiceConfiguration` 扫描的 `com.yueyunfe.dhxy.cloudbrain.remote` 迁到已扫描的
`com.bot.dhxy.service.playerstate`，Cloud `PlayerStateService` 只更新 import。adapter 内三 public operation、
current task context、前后 checkpoint、120000ms timeout、macro kind、terminal switch 与 fatal message 保持。

迁包后 remote 包的 `RemoteProtocolValidation` 不再可见；原来两处 `require(...)` 被等价展开成同样的
`IllegalArgumentException` 条件：EXECUTED 必须有 typed result，且 result operation 必须等于 command operation。
接受域与异常类型不变。

## File Table

| Repository / file | Action | Role |
|---|---|---|
| Cloud `src/main/java/com/bot/dhxy/service/PlayerStateService.java` | Modify one import | 注入扫描可达的 first-aid adapter；业务方法体零改。 |
| Cloud `src/main/java/com/bot/dhxy/service/playerstate/CloudPlayerStateFirstAidPort.java` | Relocated existing adapter | PlayerState 专属 Cloud-to-DHXY typed assembly，现位于 component-scan root。 |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudPlayerStateFirstAidPort.java` | Relocated from old path | 旧的不可扫描位置移除，避免双 bean/双实现。 |
| DHXY Java | No change | existing source-approved command/result/mechanics/handler 原样复用。 |
| `docs/superpowers/plans/reports/2026-07-15-cloud-player-first-aid-count-unit-worker-i4.md` | New | 本 worker 唯一报告。 |

## Complete Reachable Chain

1. Cloud `AutoBattleTask.runAutoBattlePatrol:135` 在 startup check 通过后、maintenance/combat 初始化前调用
   `performStartupFirstAidCheck(context)`。
2. `performStartupFirstAidCheck` checkpoint 后清 startup throttle state，调用
   `performFirstAidCheck(true, context)`；`true` 只绕过 5000ms interval，不绕过 one-check/window gate。
3. `performFirstAidCheck` 按 696 读取四项 enable/raw threshold，进入 `healAll(context)`；四项顺序固定为
   人物血量、人物法力、宝宝血量、宝宝法力。
4. `healAll()` 经扫描可达的 `CloudPlayerStateFirstAidPort` 发出唯一
   `PLAYER_STATE_FIRST_AID/HEAL_ALL` command；no-focus 与 cached-plan public paths 复用同一 adapter 的另外两种
   closed operation，Cloud 保留 target selection、plan、counter 和 result classification。
5. DHXY generic handler 在既有 registration/runRevision/native-binding safety gate 后分派
   `executePlayerStateFirstAidMacro`。`HEAL_ALL` 与 `EXECUTE_CACHED_PLAN` 各自在一个
   `submitRemoteExclusiveAndWaitDetailed` callback 中连续运行，零 nested queue；probe 为 no-input exact-window capture。
6. DHXY `PlayerStateFirstAidLocalMacroMechanics` 在 input worker 内 refresh 当前 binding，执行原 capture/颜色判断/
   二次确认/右键/delay；handler 返回 operation-specific closed typed payload。
7. Cloud adapter 校验 operation 后回传 result；NOT_EXECUTED 不伪造完成，STOPPED/UNKNOWN fatal；Service 按 696
   顺序更新 check counter/plan/state。

## Typed Mechanics

| Operation | Command | DHXY continuous mechanics | Closed result |
|---|---|---|---|
| `PROBE_SUPPLY_NO_FOCUS` | 四项 `TargetToggle(enabled, rawThreshold)` | exact-window same-frame bars capture，无 input；X=-1/capture unavailable fail closed | READABLE 携同帧 base + 固定四项 `ProbeObservation`；否则 CAPTURE_UNAVAILABLE 空表 |
| `HEAL_ALL` | 同四 toggle | one exclusive callback；fixed order；30/50/70；+10 probe；350ms confirm；right-click 100ms；settle 800ms | CAPTURED 携固定四项 `HealOutcome`；否则 CAPTURE_FAILED 空表 |
| `EXECUTE_CACHED_PLAN` | stored base + Cloud ordered targets | live binding refresh，失败 fallback stored base；safe move 300ms；逐目标 100/800ms | COMPLETED / INTERRUPTED |

## Closed Terminal Matrix

| Transport terminal | Adapter / Service behavior |
|---|---|
| EXECUTED | typed result non-null且 operation 必须匹配；返回给 Cloud 业务层。 |
| NOT_EXECUTED | `Optional.empty()`；保留 baseline did-not-complete 日志/返回，不写 physical success。 |
| STOPPED | 前后 checkpoint 优先响应 current stop；其余由 `TaskFatalException` 闭合。 |
| UNKNOWN | `TaskFatalException`，不降为 healthy、capture miss 或普通失败；零 retry。 |

## Prior Approval Protected

- 2026-07-14 parent Source Review #4 已确认 first-aid 双侧源码 `P0=0/P1=0/P2=0`：同帧 base、单 X
  unavailable sentinel、固定四 bar、CAPTURE_* 空表与 active caller 均通过，状态仅因 fresh build 未执行而未计数。
- 本次没有改该批准范围内的业务方法、contract、codec、digest、DHXY mechanics 或 handler；只使已批准 adapter
  在固定 Cloud host scan boundary 内真实可注入。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Pre-Edit Reachability Evidence

- Real caller is present and unchanged: Cloud `AutoBattleTask.runAutoBattlePatrol:135` calls
  `playerStateService.performStartupFirstAidCheck(context)` after startup checks and before maintenance/combat init.
- The complete active business path is present in Cloud `PlayerStateService`:
  `performStartupFirstAidCheck -> performFirstAidCheck(ignoreTimeInterval=true) -> healAll(taskContext) -> healAll()`.
  The `MAX_CHECKS_BETWEEN_BATTLES=1`, `HEAL_TIME_INTERVAL=5000`, startup reset, HP/MP enable/raw-threshold
  toggles, normalized thresholds, fixed target order, post-call check count, checkpoints, and logs match
  `696a12b0`.
- The previously source-approved typed chain is present: Cloud `CloudPlayerStateFirstAidPort` emits only
  `PLAYER_STATE_FIRST_AID` with closed operations `PROBE_SUPPLY_NO_FOCUS`, `HEAL_ALL`, and
  `EXECUTE_CACHED_PLAN`; DHXY handler reaches `PlayerStateFirstAidLocalMacroMechanics` under exact binding and
  the input-worker exclusive callback; typed results preserve ordered four-bar observations/outcomes and closed
  transport handling.
- **Precise gap before edit:** `CloudServiceConfiguration` scans only `com.bot.dhxy.service` and imports only
  the currently listed explicit assemblies. The existing `CloudPlayerStateFirstAidPort` is under
  `com.yueyunfe.dhxy.cloudbrain.remote`, has no explicit import/registration, and has no second implementation.
  Therefore the Service constructor dependency is not component-scan reachable even though the Java call graph and
  transport contracts exist.
- Repair boundary: relocate the existing first-aid-specific adapter, without changing its class or method behavior,
  into `com.bot.dhxy.service.playerstate`, which is already under the fixed component-scan root; update only the
  Cloud `PlayerStateService` import. No host/config, generic shared 12, DHXY Java, caller, incense, or other Service
  edit is needed.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Scoped Verification

- `PlayerStateService` import 反向重构并恢复原 CRLF 后 SHA-256 为
  `6954F8EF93083536B3565938931BFC80E8B222D15F01325D0B8C39667A3F738B`，与领取时 pre-edit SHA 完全一致；
  因而该共享大文件除 adapter import 外无逻辑改动，incense/identity/position/其它 first-aid 方法均受保护。
- wiring count：固定 scan root 命中 1；`CloudPlayerStateFirstAidPort` implementation 命中 1；adapter command
  operation 命中 3；EXECUTED/NOT_EXECUTED/fatal terminal 分支命中 3；旧不可扫描 adapter path 已不存在。
- authored adapter/report 无 trailing whitespace、无 merge marker；报告 `git diff --check` exit 0。
- Cloud Service 当前 SHA-256：
  `31831D8E3AF9D83FC6B08E3870A4519714A01661E9AFE2C623A7A1723518D6D9`；扫描可达 adapter：
  `1DB3A4B308F243EB1AD7A2AD98191FEDB4D3A957F1F39DBB2BC131A5AA7C7A6D`。
- DHXY `PlayerStateService` SHA-256 仍为领取时
  `2CF9DD766A86C3F8C9747176B52A39EC79E20A4AD5F648FA87EA506BD0681A1E`；first-aid mechanics 为
  `5D795AA1A0BAFB8A01C56F2E2DE21F4621B95EB0163F5E9A4D07FA8276C0BF8E`；handler 为
  `B1CD28FA03F1F933E92BB037C09BA1E2922635149D32D4482637B1CD313BCDFC`。本 worker 对三者零改。
- `PlayerStateService.java:1504` 的 frozen incense block 有一处领取前已存在的 trailing spaces；import 反向重构
  哈希证明其不是本 worker 引入。按禁止触碰 incense lane 的要求未清理。
- 未运行 Maven、测试、runtime/application/server/Task/poller/UI/capture/input；未执行 Git mutation。

## Handoff Gate

- Worker I4 implementation 状态：`CLAIMED_DELIVERED_FOR_PARENT_REVIEW`。
- 申报：`countUnit=PlayerStateService::performStartupFirstAidCheck`，`countDelta=+1`。
- 当前实际计数：**尚未增加**。只有父级复核本次 scan-reachability 修复及完整 caller/mechanics/terminal 链为
  `P0/P1/P2=0`，并完成统一 fresh applicable build 后，才可应用 `+1`。
- 待父级源码审查和统一 fresh build；本 worker 不宣称 compile/package 通过，也不请求 fresh runtime。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**
