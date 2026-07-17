# CR271 TURN-34B post-BP3 whole-card closure/readiness preflight

> 角色：CR271 Internal helper，仅做 whole-card closure/readiness PRECHECK。  
> 快照：2026-07-16T11:56:36.951-04:00。  
> 结论：**PRECHECK / NOT READY / NOT APPROVED / NO CARD CLAIM**。  
> 唯一业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。  
> 本报告不能代替 future fixed child card、source/test delivery、parent receipt、independent review、named test、compile/build 或最终批准。

## 1. 边界与权威顺序

本 helper 不是 TURN-34B/BP1/BP2/BP3/BT implementation owner，不是 reviewer/approver，也不创建、领取、
改状态或关闭任何卡。本轮唯一写项是本报告；Cloud 仓只读。未运行 Maven/JUnit/compile/package/runtime/
application/server/Task/UI/capture/input，未执行 Git mutation，未覆盖、移动或清理任何 dirty/untracked 字节。

本报告按以下顺序解释冲突：

1. `AGENTS.md`、`docs/DHXY_CONTEXT.md` 与 CR271 当前工作边界；
2. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节；
3. HTTPS turn thin-client protocol；
4. `docs/业务逻辑.md` 与迁移前 `696a12b0`；
5. 各 fixed card 的当前 physical true EOF、最新 canonical delivery/parent receipt；
6. 当前物理 production/test 字节及 SHA。

任何 helper/readiness 文本只冻结检查口径，不覆盖后来创建的 fixed card，也不授权实现者选择新的业务语义。

## 2. 当前物理快照与判定

| 项目 | 2026-07-16T11:56:36.951-04:00 的事实 | 本报告判定 |
|---|---|---|
| TURN-34BP1 | production `TaskExecutionContext.java` 527 行 / `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e`；test 872 行 / `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785`；true EOF 为 Parent Review #3 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED / INDEPENDENT REVIEW-BUILD PENDING` | BP1 最新源码门可作为 BP2 起点；**不得写成 BP1 card approved、双 reviewer passed 或 build passed** |
| TURN-34BP2 card | 11:46:43 已追加 External C claim 正文，但当前物理末行没有新的规范 `TRUE_EOF` claim/delivery；卡内最末规范 true EOF 仍是 `CLAIM-REQUIRED` | 按受保护的 provisional single-writer 处理，禁止第二 owner；不能冒充 canonical claim 或 delivery |
| TURN-34BP2 production | `TaskMaintenanceService.java` 已从 frozen start 1,224 行 / `963b028c...` 变为 volatile WIP 1,286 行 / `8363a765da6c4f0aec840a707f824131366985c1fc75c06f3e9f02ce7353ae28` / mtime 11:55:42；当前四个 shared map 仍是 String 泛型，文件底部已出现未接收的 typed-key scaffold | BP2 已有真实 source increment，但仍是 active/provisional WIP：无 canonical delivery、无 parent receipt、无 owner release，不审、不冻结、不供 BP3 起步 |
| TURN-34BP3 | fixed child card 不存在 | 未 claim、未实现、未审查；BP3 起始 SHA 尚不能冻结 |
| TURN-34B sole named test | `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java` 不存在 | 测试字节为零；不能声称任一 TURN-34B acceptance 已由该 named test 覆盖 |
| TURN-34BT1 | true EOF 为 `OWNER-RETURN-ACCEPTED / ZERO-OWNER / ZERO-TEST-BYTES / FUTURE-REPLACEMENT` | 旧卡不能按 `963b...` 目标直接复活；post-BP3 必须由 parent 重新冻结 final production SHA 和 replacement/continuation 边界 |
| TURN-34B parent card | true EOF 仍停在 09:26 的 retained-production Review #1 `0/2/1` | 尚未聚合 BP1 Review #3，更未聚合 BP2/BP3/test；不能作为 whole-card pass |
| TURN-34AT1 | child true EOF 为 Parent Review #4 `0/3/0 / REPAIR #3 REQUIRED / TEST-ONLY / CLAIM-REQUIRED` | TURN-34A 六 API caller gate 未闭合；34A parent 10:43 的旧 pass 尾已被 child 后续结论 supersede |
| TURN-22 | parent true EOF 为 `C1-SOURCE-PASSED / D1-EXTERNAL-A-READY / PARENT-REPAIR3-PENDING`；D1 card 为 independent `2/2`、build pending；C1 source passed，独立报告虽各自 approved，parent 未最终聚合 | TURN-22 未最终通过；其传递依赖 TURN-28Q 当前仍是 Repair #3 `0/2/0 / CLAIM-REQUIRED` |
| TURN-33 | true EOF 为双 reviewer `2/2 APPROVED / 0/0/0 / BUILD-PENDING`，production/test SHA 分别为 `991db945...` / `6a755b0f...` | Summon 合同静态审查完成，但 named-test/build 实证仍不得假定 |

因此当前总判定是 **NOT READY**。BP1 的最新 parent source receipt 已放行 BP2 source-start，且 BP2 已产生受
保护的中途 source WIP；但它尚未 canonical delivery。BP1 final gate、BP2 receipt、BP3、sole test、
TURN-22/33/34A、双 reviewer 与 build 仍是独立债务。

## 3. Whole-card 串行闭合 DAG

必须按以下依赖推进；箭头表示“后项不得把前项当成已完成”：

```text
BP1 canonical Repair #2 bytes
  -> BP1 parent source+test-source receipt（当前已满足）
  -> BP2 one-file typed shared-key delivery
  -> BP2 parent source receipt + owner release
  -> parent 以 BP2 final private types/SHA 创建 BP3 fixed card
  -> BP3 one-file generation-lifetime delivery
  -> BP3 parent whole-production source receipt + owner release
  -> sole TaskMaintenanceTurnContractTest serial consolidation
  -> parent whole-test-source review
  -> TURN-22 / TURN-33 / TURN-34A applicable final gates
  -> two independent latest-snapshot reviewers
  -> required named test(s) + Cloud compile/build in stable-writer window
  -> parent final judgment
```

BP1 独立 reviewer/build 可以与不重叠的 BP2 source-start 并行，但它们不能被 BP2/BP3 的 parent source receipt
替代；同理，BP1 reviewer 只审 BP1 两文件，不能批准 post-BP3 `TaskMaintenanceService` 或最终 sole test。

## 4. 剩余 production source tranches

### SRC-0 - BP1 final stability gate（不授权新源码）

- 固定 BP1 当前两份 SHA；任何后续 Repair 都使旧 reviewer/build 证据失效。
- 完成两名独立非实现 reviewer 对最新 Repair #2 delivery 的明确结论，以及 stable-writer named test/Cloud gate。
- 未出现新 finding 前，不再修改 BP1 production/test，也不把 BP1 的 native-generation fence 复制进 maintenance。

### SRC-1 - TURN-34BP2 shared typed-key foundation

Exact Java write set 只能是：

```text
D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java
```

另只允许 append parent 已创建的 `TURN-34BP2` child card。当前 provisional C writer 未 canonical return 前，
不允许第二 writer。BP2 必须只完成下列四个 shared map 的 typed-key 迁移：

```text
Map<ScopedTeamKey, Integer> activeTeamRoundByKey
Map<TeamRoundKey, TeamMaintenanceWindowState> teamMaintenanceWindowStateByRound
Map<ScopedLocalSessionKey, LocalTeamSessionState> localTeamSessions
Map<MaintenanceClaimKey, Set<ScopedWindowKey>> summonSkillClaimsByTeamRound
```

私有 typed-key 职责冻结为：

```text
ExecutionScopeKey
  - ExactExecutionScopeKey(tenantId, userId, deviceId)
  - NoContextScopeKey.INSTANCE
ScopedWindowKey(scope, windowId)
ScopedLocalSessionKey(scope, sessionKey)
TeamCoordinationKind { WINDOW, LOCAL_SESSION }
ScopedTeamKey(scope, coordinationKind, coordinationKey, maintenanceKey)
MaintenanceClaimKey
  - TeamRoundKey(teamKey, round)
  - LocalCapabilityRoundKey(sessionKey, capability, epoch)
```

BP2 closure checklist：

1. supplied `TaskExecutionContext` 胜过 holder；只有 supplied null + holder empty 才使用 typed no-context。
2. 已有 context authority 失败不得被 broad `RuntimeException` catch 降级为 unscoped/no-context。
3. 同 scope + 同显式 local session 可共享；无显式 session 的不同 window 隔离；tenant/user/device 任一不同均隔离。
4. maintenance-key fallback 顺序保持“显式 key -> requested task -> task -> 既有 default”。
5. formal/local claim 由类型区分；无 dual map、dual lookup、prefix parse、compatibility fallback 或 global alias。
6. `|`、`#`、`local-team:` 只是普通标识符内容，不能再作为 tuple delimiter 或 prefix authority。
7. 四个 per-window Summon map、fingerprint/generation/cache purge 保持不动，留给 BP3。
8. 零新增 metadata read/checkpoint/delegate/command/action/actionId/UUID/retry/sleep/timer/TTL/owner/lease/ledger/queue。
9. 19 public APIs、五 constructor collaborators、六个 TURN-34A API 与四个 zero-caller lifecycle API 可达性不变。
10. delivery 必须给出 start/final SHA、行数、方法/type 索引、严格写集和 `无已批准业务差异；按 696a12b0 等价迁移`；随后停止编辑等 parent source review。

### SRC-2 - TURN-34BP3 exact generation lifetime

只有在 BP2 canonical delivery、parent source receipt、owner release 后，parent 才能以 BP2 **实际 final SHA 与实际
private type 名称**创建 BP3 fixed card。BP3 与 BP2 写同一 `TaskMaintenanceService.java`，绝无并行空间；BP3
不能从旧 `963b...` 旁路起步。

BP3 必须复用 BP2 的全部 typed scope/team/session/claim keys，只新增以下 private generation 类型职责：

```text
WindowGenerationFingerprint
  - ExactNativeWindowFingerprint(windowTitle, nativeHandle, processId)
  - LegacyPlayerWindowFingerprint(playerIdentityEpoch)
  - NoContextWindowFingerprint.INSTANCE
WindowBindingKey(ScopedWindowKey windowKey, WindowGenerationFingerprint fingerprint)
```

四个 per-window 字段最终只换为：

```text
Map<ScopedWindowKey, Long> lastSummonSkillCleanAtByWindow
Map<ScopedWindowKey, Long> lastSummonSkillNotDueLogAtByWindow
Map<ScopedWindowKey, Long> summonSkillUnknownRetryAfterByWindow
Map<ScopedWindowKey, SummonSkillWindowState> summonSkillStateByWindow
```

只允许新增当前值索引：

```text
Map<ScopedWindowKey, WindowGenerationFingerprint> currentFingerprintByWindow
Map<ScopedTeamKey, WindowBindingKey> formalTeamBindingByKey
LocalTeamSessionState.participantFingerprints:
    Map<ScopedWindowKey, WindowGenerationFingerprint>
```

BP3 closure checklist：

1. exact fingerprint 只含 title/HWND/process；legacy 只含 identity epoch；no-context 为显式 singleton。
2. 不把 `taskRunId`、pause/stop、window rect、task/round/session 放进 fingerprint，不创建 server generation。
3. `currentFingerprintByWindow.compute(...)` 是同 `ScopedWindowKey` 的唯一 transition 点；A -> B 与 B -> A 都先精确清旧 generation，再只保存 incoming，第一份 A 永不复活。
4. same exact fingerprint 的连续调用不误清 cooldown、not-due log、UNKNOWN interval、2h cache、claim 或 capability。
5. per-window transition 只清旧 binding 的四个 per-window maps，不 global clear，不碰其它 scope/window。
6. claim owner 只从 exact owner set 移除 old window；空 set 才删 entry。
7. formal state 只在 `formalTeamBindingByKey` record-equal old binding 时清该 exact team 的 active round/window/formal claims；query/await 不改 owner。
8. local-session participant 只按 same scope/session + exact old fingerprint 清旧 participant/role/completion；candidate 保留；member drift 不清 leader/capability；leader drift 才精确清 leader evidence、capabilities、epochs 与该 session capability claims。
9. shared state 实际变化后只通知既有 monitor；不加 timer/thread/queue/background cleanup。
10. `completeLocalTeamSessionWindow` 仍只在全部 candidate 完成时删除 exact session，并只清该 session 的 capability claims；STOP/fatal/uncertain/task return 不自动调用它，不新增 task terminal/restart hook。
11. `runOpportunisticMaintenance` placement 保持 `normalize -> existing first checkpoint -> resolve typed binding -> broadcast...`；Summon 前只保留既有 second checkpoint。BP3 不读 `latestWindowMetadata()`，不复制 BP1 fence，不加第三次 observation/checkpoint。
12. 删除 String identity token/prefix/delimiter compatibility shadow，不保留 history/counter/tombstone/TTL/lease/ledger/compaction。
13. BP3 自身 Dialog/Summon/execute/actionId/UUID 均新增 `0`，不改业务 delegate 次数、结果、retry 或 expiry。
14. delivery 必须给出 BP2 parent-received start SHA、BP3 final SHA/行数、逐 map/method cleanup 索引、19 API 与 zero-caller 计数、未运行门和 owner release；不能自写 `APPROVED/DONE/0/0/0`。

### SRC-3 - post-BP3 parent whole-production review（只读门）

Parent 必须从 `963b... -> BP2 final -> BP3 final` 的真实 diff 逐段复读，并对最终整文件再做一次 whole-source
审查。BP2/BP3 helper 报告和 worker delivery 不能代替此门。审查至少确认：

- 只发生获批的 scope/generation ownership 迁移；无 task phase、prompt、OCR、click、navigation、fallback、timing、expiry 或 delegate order 变化；
- BP1 fence 仍只在 `TaskExecutionContext`，maintenance 没有第二套 metadata truth；
- 没有为了测试顺手加入 Clock、public overload、second constructor、reflection hook、wrapper ladder 或第二 production file；
- 最终 source identity 被 sole named-test fixed tranche 明确引用。

## 5. Exact public surface freeze

最终 source 和 sole named test 必须按**完整 signature set**锁定，而不是只断言数量：

```java
public void initializeForTaskStart(TaskExecutionContext context, String sourceTask)
public void beginTeamMaintenanceRound(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void openTeamPathingMaintenanceWindow(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void openTeamFirstAidMaintenanceWindow(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void closeTeamMaintenanceWindow(TaskExecutionContext context, String teamMaintenanceKey, int round, String sourceTask)
public void openLocalTeamReturnSupportWindow(TaskExecutionContext context, String sourceTask)
public void closeLocalTeamReturnSupportWindow(TaskExecutionContext context, String sourceTask)
public boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext context, String teamMaintenanceKey)
public boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext context, String teamMaintenanceKey, long timeoutMs)
public boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext context, TeamSupportCapability capability, long timeoutMs)
public boolean isLocalSupportMemberSession(TaskExecutionContext context)
public void registerLocalTeamSessionCandidate(String sessionKey, Collection<String> windowIds, String sourceTask)
public void markLocalTeamWindowRoleDetected(TaskExecutionContext context, String windowId, String roleName, String sourceTask)
public boolean isLocalSupportMemberCandidate(TaskExecutionContext context)
public boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext context)
public void markLocalTeamLeaderDetected(TaskExecutionContext context, String leaderWindowId, String sourceTask)
public boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext context, TeamSupportCapability capability)
public void completeLocalTeamSessionWindow(String sessionKey, String windowId, String sourceTask)
public TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext context, TaskMaintenanceRequest request)
```

Lombok `@RequiredArgsConstructor` 的 collaborator 顺序保持：

```text
BotProperties, GameContext, DialogService, SummonSkillService, TaskExecutionContextHolder
```

TURN-34A caller-visible 六 API 是：

```text
awaitTeamFirstAidMaintenanceWindowOpen
awaitLocalTeamSupportCapabilityOpen
isLocalSupportMemberSession
isLocalSupportMemberCandidate
isPendingLocalSupportLeaderDetection
isLocalTeamSupportCapabilityOpen
```

以下四 API 当前 production caller 必须继续为零；sole test 可以在 parent 明确冻结后通过 public API 做 test-only
状态驱动，但不能把测试调用写成 runtime activation：

```text
registerLocalTeamSessionCandidate
markLocalTeamWindowRoleDetected
markLocalTeamLeaderDetected
completeLocalTeamSessionWindow
```

## 6. Sole named-test consolidation

最终唯一测试路径固定为：

```text
D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java
```

旧 TURN-34BT1 是 zero-byte owner-return，且目标是 pre-BP2/BP3 `963b...`。Parent 应在 BP3 parent source receipt 后
冻结 replacement/continuation card、最终 production SHA 与 exact starting test state；本 helper 不代建该卡。

所有 test tranches 必须严格串行追加到这一文件。每片 physical EOF 记录 target production SHA、start/final test
SHA、delivery/owner release；下一片只有在上一片 canonical delivery + parent test-source receipt 后才能 claim。
同 worker continuation 也不能跳过边界，绝不允许第二测试类或并发 writer。

建议按现有 readiness 债务作以下**累积 consolidation**，标签只是 parent 未来切片名，不是本报告创建的卡：

| 串行 test tranche | 必须累计到 sole file 的证据 |
|---|---|
| T1 / replacement BT1 | real public `TaskMaintenanceService` + test-private scripted collaborators；real turn-native exact context；supplied-context-over-holder；missing/scope/native drift；full-state A -> B -> A；tenant/user/device/window/session/delimiter isolation；19 signatures、5-constructor、6 TURN-34A APIs；四个 lifecycle API 不冒充 runtime caller |
| T2 / suggested BT2 | broadcast priority/result spine；`BUSINESS_OPTION_CLICKED`、`FAILED`、`INTERRUPTED` short-circuit；Dialog request source/full-fallback 原值；all deterministic front gates；eligible path exactly one typed Summon delegate；result/exception/ActionState 投影 |
| T3 / suggested BT3 | due/cooldown、UNKNOWN interval、tail-safe/skill-count cache、formal/local round、capability/pathing、duplicate/max claim、known failure release、state-change retain、success projection；BP3 full generation invalidation 不得只测一张 Summon map |
| T4 / suggested BT4 | capability 精确 `5/1/5/2`、query/zero-timeout/await/interrupt、leader conflict/absent、candidate completion、exact local-session terminal cleanup；仅在 parent 明确许可后用四个 zero-caller public APIs 做 test-only state seeding |

### 6.1 Named test 的 exact terminal/UUID/no-retry matrix

1. initial STOP：抛原 `TaskStopRequestedException`；Dialog=0、Summon=0、execute=0、action/UUID=0、无 state mutation。
2. missing/device/window/title/HWND/process/generation typed checkpoint transition：在首 delegate 前原样 unwind；一次 checkpoint 只读一个 latest metadata slot；零 action/UUID。
3. broadcast `BUSINESS_OPTION_CLICKED` -> `BROADCAST_HANDLED` 且短路；`FAILED` -> `BROADCAST_FAILED` 且短路；`INTERRUPTED` -> `INTERRUPTED` 且短路；三者 Summon=0。
4. broadcast no-action 才能继续 optional Summon；无第三 fallback、第二 observation 或 background work。
5. disabled、interval<=0、non-FREE、not-due、existing UNKNOWN interval、fresh tail-safe、无 round、capability closed、pathing closed、same-window duplicate、max claim 均为 Summon delegate=0。
6. eligible due path：`cleanSummonSkillsOnce(...)` 恰一次；TaskMaintenance 自身 `TurnGameClient.execute` 与 UUID supplier 均为 0。TURN-33 的真实 action/UUID 1:1 由 `SummonSkillTurnContractTest` 拥有，本测试不复制其五删除/PNG/OCR fixture，也不反向声称整个 Summon 链 UUID=0。
7. Summon 抛 `TaskStopRequestedException`：同一 exception 传播，delegate=1，无第二脚本消费，previous `ActionState` 恢复，TaskMaintenance execute/UUID=0。
8. Summon 抛 `TaskFatalException("DUPLICATE_OR_UNCERTAIN")` 或 correlation fatal：同一 exception 传播一次，delegate=1，零自动 retry/replay/resend，success cooldown 不刷新，previous state 恢复。
9. known failed 且无 state change：单次 invocation delegate=1，返回既有 `SUMMON_SKILL_FAILED_RETRY_LATER`，释放自己 claim；下一次**显式** public invocation 可再次尝试，但不能称为 transport retry。
10. delete/ultimate 已发生 state change 后失败：claim 保留；下一次同 round 显式调用为 duplicate/zero delegate。
11. UNKNOWN：第一次 delegate=1，记录既有 configured retry-after 并 invalidates layout cache；紧接的第二次显式调用 deferred，累计 delegate 仍为 1；不添加新 TTL/读/重试。
12. success：delegate=1，只投影一次 `SUMMON_SKILL_CLEANED`、cooldown/cache/state，恢复 previous `ActionState`；不伪造额外 action/UUID。
13. `TaskMaintenanceStatus` closed surface不新增、不改名：`NO_ACTION`、`BROADCAST_HANDLED`、`BROADCAST_FAILED`、`INTERRUPTED`、`CLOUD_REQUIRED_FAILURE`、`SUMMON_SKILL_DISABLED`、`SUMMON_SKILL_NOT_DUE`、`SUMMON_SKILL_DEFERRED`、`SUMMON_SKILL_ROUND_ALREADY_CLAIMED`、`SUMMON_SKILL_CLEANED`、`SUMMON_SKILL_FAILED_RETRY_LATER`；测试不得用未发生的 `CLOUD_REQUIRED_FAILURE` 包装 STOP/uncertain。

### 6.2 2h deterministic expiry 是当前明确 blocker

现有 production 的两项常量必须保持：

```text
SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS = 2 * 60 * 60 * 1000L
SUMMON_SKILL_COUNT_CACHE_TTL_MS     = 2 * 60 * 60 * 1000L
```

但当前 fixed constraints 同时禁止 wall-clock 2h sleep、private-production reflection 与擅自加入 production Clock
seam；即时第二次调用只能证明 fresh，不能证明 expiry。因此在 parent 明确冻结下列二选一前，whole-card test
acceptance **不能写 READY/covered**：

1. 另开、明确批准一个不改变 public 19 APIs/五 collaborator/业务时间语义的 deterministic testability source story；或
2. 用户/parent 明确批准把 exact 2h expiry 的闭合证据改为 source-review + fresh-side executable evidence。

实现者不得自行选项、不得把 clock seam 偷塞进 BP2/BP3、不得用 reflection/sleep/即时调用伪造 expiry pass。

## 7. `696a12b0` 等价闭合清单

最终 parent review、sole test 与两名 independent reviewers 必须逐项签收：

1. 总顺序保持 `normalize -> first checkpoint -> optional broadcast -> handled/FAILED/INTERRUPTED short-circuit -> optional Summon -> no-action`。
2. Summon gate 顺序保持 `feature -> interval -> FREE -> due -> UNKNOWN retry interval -> 2h tail cache -> team/local round -> capability/pathing -> duplicate/max claim -> second checkpoint -> one delegate`。
3. CommonBox pending/消费优先级、30 秒业务生命周期及修罗/五倍接入点不被 maintenance scoping 改写；BP2/BP3 不新增或移动 box fallback。
4. 召唤兽静态尾识别、普通/保留/终极技能判断、删除预算、cooldown、UNKNOWN 与 TURN-33 ownership 保持；TaskMaintenance 不接管 OCR/PNG/action 细节。
5. pathing open 精确五项：`FIRST_AID`、`PATHING_WINDOW`、`COMMON_BOX`、`SUMMON_SKILL`、`LEFT_TOP_STATUS`。
6. first-aid window 只开一项 `FIRST_AID`。
7. team close 精确关闭上述五项，不顺手 teardown active round/claim。
8. local return support open/close 精确两项 `TEAM_RETURN + COMMON_BOX`。
9. 上述 `5/1/5/2`、formal/local sharing、max cleaner、claim acquire/release/retain、leader conflict/absent、all-candidate completion 只换 typed ownership，不改谁能做、何时做、做几次。
10. `GameContext.ActionState` 仍只在实际 Summon delegate 周围切 `INTERACTING`，finally 按既有条件恢复 previous state。
11. success、known failure、state-change failure、UNKNOWN 的 cooldown/cache/claim 投影及 expiry 条件保持当前 source 与基线；不加新 TTL、验证读、retry、cleanup 或 fail-closed business truth。
12. await/query 的 timeout=0、monitor wait、interrupt/false 返回语义保持；不加 polling sleep/thread。
13. STOP/paused/typed drift 是控制流终止，不包装成业务 FAILED；terminal/uncertain 不重放命令、不伪造成功。
14. task STOP/fatal/uncertain/return 不触发未批准的 global maintenance teardown；只保留 BP3 明确的 exact generation invalidation 与现有 local-session completion。
15. 四个 zero-caller lifecycle API 不由 BP2/BP3 新增 host/task/runtime caller。
16. 无新 public API、constructor collaborator、protocol type、action enum、owner/session authority、lease、ledger、queue、durable workflow、history、counter、tombstone 或 compaction。

预期最终卡内业务差异文本必须是：

```text
无已批准业务差异；按 696a12b0 等价迁移
```

若任何实现选项会改变 priority、gate、condition、phase、fallback、delegate/input/verification order、retry、
expiry、capability set 或 terminal projection，必须停止并另写 CR 选项让用户批准，不能以“迁移安全”为由自行加入。

## 8. 同文件 owner 锁与 shared build 锁

| 文件/边界 | 当前/未来 owner 规则 | 闭合前禁止事项 |
|---|---|---|
| BP1 `TaskExecutionContext.java` + sole BP1 test | 当前 implementation owner released；冻结给独立 reviewers/build | BP2/BP3/test writer 不得修改；任何字节变化重置 BP1 reviewer/build |
| `TaskMaintenanceService.java` | 当前有 11:46 provisional C claim 正文与 11:55 volatile source WIP，虽无 canonical true EOF/delivery仍按单 writer 保护；BP2 delivery+receipt+release 后才可由 BP3 新 owner claim | 第二 BP2 writer、把中途 `8363...` 当 delivery/BP3 起点、BP3 并写、test writer 顺手修 production、从旧 `963b...` 旁路叠写 |
| `TaskMaintenanceTurnContractTest.java` | 当前不存在/zero owner；post-BP3 由 parent 冻结 replacement T1，T1->T2->T3->T4 每次 canonical handoff，始终一 owner | 复活旧 BT1 target、并发 tranche、第二测试类、未记录 target production SHA |
| TURN-34A `AutoCombatService.java`/test | 与 maintenance 文件不重叠，但 child AT1 仍 Repair #3 | 其旧 parent pass 不能替代 child 最新结论；Cloud build 必须等该 writer 稳定 |
| TURN-33 `SummonSkillService.java`/test | 当前双审通过、build pending，字节冻结 | maintenance test 不复制 TURN-33 action/UUID fixture；任何 Summon 字节变化重置其审查 |
| TURN-22 Cloud test / DHXY executor+test / TURN-28Q queue+worker+test | Java 路径与 maintenance 不重叠，但 TURN-22 parent 与 TURN-28Q 仍未最终闭合 | 不把 D1 或独立报告单片通过冒充 TURN-22 parent/build pass |
| Cloud build cohort | 只有所有相关 Cloud Java/test writer canonical release、final SHA 已审且无中途 WIP 时才进入 | writer 活动时不得 clean/compile/package；不得以 stale class/jar/旧命令日志交付 |

## 9. Two-reviewer 与 build gate

Whole-card final gate 必须满足全部条件，缺一项都不是 READY：

1. Parent 对 post-BP3 `TaskMaintenanceService.java` 做 whole-source review，对最终 sole test 做 whole-test-source review；结论绑定完整 SHA。
2. 两名互相独立、均非任何相关最终字节实现者的 reviewer，各自读取同一 latest production/test SHA、BP2/BP3 delivery、19 APIs、terminal/UUID/696 checklist，并在 durable review 记录中明确 `APPROVED / P0/P1/P2=0/0/0`。
3. 任一 reviewer 出现 P0/P1/P2 或源码/test 后续变动，旧双审立即失效；返修后重新 parent review + 两名 latest-round reviewer，不能只补一名。
4. BP1 两文件还需自己的双 reviewer/build closure；其 approval 不自动计入 TURN-34B final snapshot reviewer，反之亦然。
5. TURN-34A、TURN-22（含 TURN-28Q 传递依赖）、TURN-33 必须各按自身最新卡完成适用 source/test/reviewer/build；不能只靠文件不重叠跳过 final dependency。
6. Stable-writer 窗口中，从 `D:/mavenProject/dhxy-cloud-brain` 对最终 sole test 运行唯一指定命令：

```powershell
mvn -q -Dtest=TaskMaintenanceTurnContractTest test
```

7. 必须记录该命令真实 exit 0、tests run/failures/errors/skipped 及当前 production/test SHA；未执行、旧 SHA、只编译 main 或 stale report 均不算。
8. BP1、TURN-33、TURN-34A、TURN-22 各自的 named test 证据仍独立需要；`TaskMaintenanceTurnContractTest` 不替代它们。
9. 所有 non-test source 稳定后运行计划规定的 Cloud compile gate：

```powershell
mvn -q clean compile
```

10. 最终 Cloud build gate 为：

```powershell
mvn -q clean package
```

`clean package` 会运行测试，必须有当时用户/parent 的明确执行授权；本次用户明确禁止 build，因此本 helper 未运行，也不声称任何命令通过。
11. 只有 named test、compile/package、依赖卡与双 reviewer 都有当前 SHA 的真实成功证据后，parent 才能作 final judgment；helper、worker 或 reviewer 单方不能关闭 parent card。

## 10. Final closure checklist

Parent 在判断 whole-card 前应逐项打勾；本 PRECHECK 不替 parent 打勾：

- [ ] BP1 Repair #2 当前两 SHA 保持，双 independent reviewer 与 build gate 有明确最新证据。
- [ ] BP2 canonical claim/delivery/parent source receipt/owner release 完成；四 shared maps typed，四 per-window maps 未提前改。
- [ ] BP3 fixed card 从 BP2 final SHA 创建；canonical delivery/parent whole-source receipt/owner release 完成。
- [ ] post-BP3 source 满足 exact A -> B -> A、exact invalidation、no metadata duplication、no terminal hook、no behavior drift。
- [ ] 19 public signatures、五 collaborator、六 TURN-34A APIs、四 zero-caller lifecycle APIs 全部精确保持。
- [ ] sole `TaskMaintenanceTurnContractTest.java` 是唯一测试类，所有 serial tranches 聚合到一个 final SHA。
- [ ] supplied-context、scope/window/session/delimiter、full-state A -> B -> A 均由 public API 可执行证明。
- [ ] broadcast/Summon priority、zero-delegate gates、one delegate、result/ActionState/claim/cache 均由 public API 可执行证明。
- [ ] STOP、typed drift、fatal/uncertain、known/state-change/UNKNOWN/success terminal matrix和 TaskMaintenance zero UUID 面闭合。
- [ ] TURN-33 action/UUID 1:1 仍由其 own named test 证明，没有被 maintenance test 重复或误写为 zero。
- [ ] `5/1/5/2`、wait/query、leader/candidate/session completion 全部精确闭合。
- [ ] 2h expiry deterministic evidence 的 parent/user 决策已先冻结并按获批方式完成；无 sleep/reflection/即时调用伪证。
- [ ] `696a12b0` checklist 全项通过，卡内写明 `无已批准业务差异；按 696a12b0 等价迁移`。
- [ ] TURN-34A、TURN-22/28Q、TURN-33 的最新卡级依赖与各自 build gate完成。
- [ ] Parent whole source/test-source review 绑定 final SHA；两名 independent latest-round reviewer 均明确 0/0/0 approved。
- [ ] `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`、Cloud `clean compile`、获授权后的 `clean package` 均对当前字节 exit 0。
- [ ] 没有 active same-file owner、provisional WIP、未接收 delivery、stale jar/report 或 unresolved P0/P1/P2。

在以上全部由相应 owner/parent/reviewer/build 实证闭合前，TURN-34B 只能保持 **PRECHECK / NOT READY / NOT APPROVED**。

TRUE_EOF PRECHECK_COMPLETE
