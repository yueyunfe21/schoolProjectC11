# CR271 TURN-34BP3 fixed-card freeze preflight helper

> 日期：2026-07-16 EDT  
> 角色：CR271 Internal 只读 helper；非 implementation owner、非 reviewer、非 approver  
> 目标：为父级准备 TURN-34BP3 fixed-card 的可直接落卡字段模板  
> 唯一写集：本报告  
> 边界：不创建/claim BP3 卡，不审 BP2 WIP，不改 Java/其它文档，不运行 Maven/runtime/Git，不给出批准或阻断裁决

## 1. 已读取权威与适用基线

本次已读取并交叉核对：

1. 仓库 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271 当前段。
2. 权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
3. HTTPS turn 协议 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
4. `docs/业务逻辑.md` 的本地队伍 session 边界（第 5-67 行）、Summon 静态格/维护边界（第 170-211 行）、
   五倍/修罗基线门（第 215-224 行）、五倍维护顺序（第 412-479 行）、修罗维护点（第 1178-1187 行）及
   `696a12b0` 失败/STOP 基线表（第 1253-1288 行）。
5. `TURN-34BP1`、`TURN-34BP2` fixed card 全文及真尾。
6. 两份现有 BP3 readiness：
   `2026-07-16-turn-34bp3-readiness-preflight-helper.md` 与
   `2026-07-16-turn-34bp3-post-bp2-readiness-preflight-helper.md`。
7. Cloud 当前 `TaskMaintenanceService.java` 全文、其 19 个 public method、五个 constructor collaborator、
   TURN-34A 六个生产调用 API、四个零生产 caller lifecycle API，以及 `migration-baseline/696a12b0` 同名源码。

本模板的业务权威固定为
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。HTTPS turn 边界仍是一次显式 command 一枚新 actionId/UUID，
terminal/uncertain 不自动重放，Cloud 拥有业务 retry/fallback/phase；`TaskMaintenanceService` 不获得第二套命令、
session、owner、ledger 或 durable workflow 权力。

## 2. Freeze 前提与当前字节隔离

未来 BP3 fixed card 只能在以下两件事都已经写入 BP2 卡物理真尾后冻结：

1. BP2 worker 已写 canonical `SOURCE DELIVERED`，包含 production starting/final SHA-256、physical lines、bytes、
   实际 changed method/type index、精确写集和 owner release。
2. 父级已从磁盘重新读取并复算同一 final `TaskMaintenanceService.java`，随后写入明确的 source receipt，包含
   exact final SHA-256/lines/bytes、实际 private typed-key 名称/结构及该轮未解决项记录。

Worker 自报 SHA、聊天文本、helper 报告、starting SHA、source increment 或编辑中 SHA 均不能替代父级 source
receipt。父级冻结卡前和未来 owner claim 前必须再次复算，且两次结果都必须与 BP2 parent receipt 相同。

本次只读窗口看到的 Cloud `TaskMaintenanceService.java` 为 BP2 活跃编辑中的暂态字节：1286 physical lines、
SHA-256 `8363a765da6c4f0aec840a707f824131366985c1fc75c06f3e9f02ce7353ae28`、mtime
`2026-07-16T11:55:42.6687008-04:00`。该值仅证明文件正在变化，**不得抄入 BP3 Frozen Inputs**；本 helper 不对
这些中途字节作 BP2 source review、finding 或 verdict。

## 3. 父级可直接使用的 fixed-card 字段模板

以下是未来 `2026-07-16-turn-card-TURN-34BP3.md` 的候选正文模板。所有
`[PARENT_FILL_*]` 必须由父级在 BP2 canonical delivery + parent source receipt 后从物理文件和卡片真尾填写；
任一占位符未替换时，不应生成 claim surface。本节本身不是 BP3 卡、不是 claim、不是状态变更。

### 3.1 Header / authority / source-start gate

```text
# CR271 TURN-34BP3 - exact per-window native-generation lifetime

Card type: bounded Cloud production implementation prerequisite for TURN-34B; not helper/reviewer.
Parent: CR271 / TURN-34B.
Business authority: 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7.
Source-start gate:
- BP2 canonical delivery marker: [PARENT_FILL_BP2_DELIVERY_TRUE_EOF]
- BP2 parent source receipt marker: [PARENT_FILL_BP2_PARENT_RECEIPT_TRUE_EOF]
- BP2 implementation/repair owner released: [PARENT_FILL_YES_AND_EVIDENCE]
- TaskMaintenanceService has no other active writer: [PARENT_FILL_YES_AND_EVIDENCE]
- BP3 card freeze-time SHA equals BP2 parent-received final SHA: [PARENT_FILL_EXACT_MATCH]

无已批准业务差异；按 696a12b0 等价迁移。
```

### 3.2 Frozen Inputs：所有 final SHA/lines 必填

| Artifact / receipt | Exact frozen field to fill |
|---|---|
| BP1 parent source receipt marker | `[PARENT_FILL_LATEST_BP1_PARENT_RECEIPT_TRUE_EOF]` |
| BP1 `TaskExecutionContext.java` | SHA-256 `[PARENT_FILL_BP1_PROD_SHA]`; lines `[PARENT_FILL_BP1_PROD_LINES]`; bytes `[PARENT_FILL_BP1_PROD_BYTES]` |
| BP1 `TaskExecutionContextTurnContractTest.java` | SHA-256 `[PARENT_FILL_BP1_TEST_SHA]`; lines `[PARENT_FILL_BP1_TEST_LINES]`; bytes `[PARENT_FILL_BP1_TEST_BYTES]` |
| BP2 starting `TaskMaintenanceService.java` | SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`; lines `1224`; bytes `66012` |
| BP2 canonical delivery marker | `[PARENT_FILL_BP2_DELIVERY_TRUE_EOF]` |
| BP2 parent source receipt marker | `[PARENT_FILL_BP2_PARENT_RECEIPT_TRUE_EOF]` |
| **BP2 parent-received final `TaskMaintenanceService.java`** | SHA-256 **`[PARENT_FILL_BP2_FINAL_PROD_SHA]`**; lines **`[PARENT_FILL_BP2_FINAL_PROD_LINES]`**; bytes **`[PARENT_FILL_BP2_FINAL_PROD_BYTES]`** |
| BP2 final actual scope/window/session/team/round/claim private types | `[PARENT_FILL_EXACT_TYPE_NAMES_AND_FIELDS_FROM_DELIVERED_SOURCE]` |
| BP2 final four shared-map declarations | `[PARENT_FILL_EXACT_DECLARATIONS_AND_LINE_INDEX]` |
| Read-only `AutoCombatService.java` | SHA-256 `[PARENT_FILL_34A_PROD_SHA]`; lines `[PARENT_FILL_34A_PROD_LINES]` |
| Read-only `AutoCombatServiceTurnContractTest.java` | SHA-256 `[PARENT_FILL_34A_TEST_SHA]`; lines `[PARENT_FILL_34A_TEST_LINES]` |
| Caller/API scan | `[PARENT_FILL_SCAN_TIME_AND_19_5_6_4_COUNTS]` |

Freeze note to copy verbatim:

```text
BP3 starts only from the BP2 parent-received final production identity above. The BP2 starting SHA, any WIP SHA,
helper-observed SHA, or unreceipted latest bytes are not valid BP3 inputs. At claim time any mismatch is NO CLAIM /
RETURN TO PARENT; do not layer over different bytes.
```

### 3.3 Exact implementation write set：单一 production + 本卡

未来 implementation owner 的唯一可写项恰为：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。
2. append-only 更新父级预建的
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34BP3.md`，仅写 claim、
   first-window、delivery/return 证据。

全部其它文件只读，尤其：BP1 context/test、holder/client/protocol/result/model/POM/config/resources、
`TaskMaintenanceTurnContractTest.java` 及全部 tests、`AutoCombatService`/其 test、`AutoBattleTask`、Wubei/Xiuluo
Task、Dialog/Summon/TeamReturn/CommonBox/PlayerState、DHXY Java、父卡、计划、`ACTIVE_WORK` 和 dashboard。
不得新增第二个 production/test 文件、production test hook 或替代卡片。

### 3.4 BP2 actual types 复用门

卡内先逐字列出 BP2 parent-received source 的实际 private types：

```text
Execution scope type: [PARENT_FILL_BP2_ACTUAL_SCOPE_TYPE]
Scoped window type: [PARENT_FILL_BP2_ACTUAL_SCOPED_WINDOW_TYPE]
Scoped local-session type: [PARENT_FILL_BP2_ACTUAL_SCOPED_SESSION_TYPE]
Scoped team type: [PARENT_FILL_BP2_ACTUAL_SCOPED_TEAM_TYPE]
Formal round type: [PARENT_FILL_BP2_ACTUAL_FORMAL_ROUND_TYPE]
Local capability round type/shape: [PARENT_FILL_BP2_ACTUAL_LOCAL_CAPABILITY_ROUND_TYPE]
Claim kind/type: [PARENT_FILL_BP2_ACTUAL_CLAIM_KIND_AND_KEY_TYPE]
```

BP3 只能复用这些真实类型及其 field-wise equality；不得按 readiness 猜测另建 alias、adapter、wrapper、raw
String shadow、typed+String 双 map、dual lookup/write、delimiter parse、prefix scan 或 compatibility fallback。

### 3.5 四个 per-window map 与 cache 边界

BP2 明确留给 BP3 的四个字段只把 logical-window key 收敛为 BP2 实际 scoped-window type，value 和时序语义不变：

```text
Map<[BP2_ACTUAL_SCOPED_WINDOW_TYPE], Long> lastSummonSkillCleanAtByWindow
Map<[BP2_ACTUAL_SCOPED_WINDOW_TYPE], Long> lastSummonSkillNotDueLogAtByWindow
Map<[BP2_ACTUAL_SCOPED_WINDOW_TYPE], Long> summonSkillUnknownRetryAfterByWindow
Map<[BP2_ACTUAL_SCOPED_WINDOW_TYPE], SummonSkillWindowState> summonSkillStateByWindow
```

`SummonSkillWindowState` 的 skill-count、next-start-slot、slot-status、last-effective-slot、tail-safe-cache、ultimate
success 字段仍是原有 cache 语义。保留既有 2h tail/count cache 与 configured UNKNOWN retry-after；不得新增、删除、
延长、缩短或在 same-fingerprint 调用上重置任何 TTL/interval。删除只为 String identity 服务的
`identityToken`、`DEFAULT_IDENTITY_TOKEN`、`currentIdentityToken(...)`、`identityTail(...)`、
`nativeHandleOrNull(...)`、`nativeProcessIdOrNull(...)` 及 delimiter key 拼接职责；不得用一行 wrapper 留下旧路径。

### 3.6 Fingerprint / generation model

未来卡可固定以下 private model；若 BP2 最终实际类型名不同，只替换被引用的 BP2 type 名，不改变字段语义：

```java
private sealed interface WindowGenerationFingerprint
        permits ExactNativeWindowFingerprint,
                LegacyPlayerWindowFingerprint,
                NoContextWindowFingerprint {}

private record ExactNativeWindowFingerprint(
        String windowTitle,
        String nativeHandle,
        long processId) implements WindowGenerationFingerprint {}

private record LegacyPlayerWindowFingerprint(
        long playerIdentityEpoch) implements WindowGenerationFingerprint {}

private enum NoContextWindowFingerprint implements WindowGenerationFingerprint {
    INSTANCE
}

private record WindowBindingKey(
        [BP2_ACTUAL_SCOPED_WINDOW_TYPE] windowKey,
        WindowGenerationFingerprint fingerprint) {}
```

最小 current-generation 索引：

```text
Map<[BP2_ACTUAL_SCOPED_WINDOW_TYPE], WindowGenerationFingerprint> currentFingerprintByWindow
Map<[BP2_ACTUAL_SCOPED_TEAM_TYPE], WindowBindingKey> formalTeamBindingByKey
LocalTeamSessionState.participantFingerprints:
    Map<[BP2_ACTUAL_SCOPED_WINDOW_TYPE], WindowGenerationFingerprint>
```

这些字段只保存 current value；不保存 history、counter、tombstone、TTL、lease、owner、ledger、task-run generation
或后台 compaction。

Fingerprint authority 顺序固定为：

1. `effectiveContext(context)` 保持 supplied context 优先；只有 supplied-null 才读取 holder，二者都空才进入 typed
   no-context variant。
2. 先由 BP2 actual resolver 得到 exact execution scope + logical `windowId`。
3. non-null context 先读取 initial `getNativeWindowTitle()`；成功时使用原始 title、
   `getNativeWindowHandle()`、`getNativeWindowProcessId()` 构造 exact native fingerprint，不 trim、不拼接。
4. 只允许捕获 title accessor 对 legacy path 明确抛出的 `IllegalStateException`，随后读取
   `getPlayerIdentityEpoch()` 构造 legacy variant；epoch/其它 authority failure 原样传播。
5. `windowRect`、pause/stop、taskRunId、task/round/session、role、requested task、wall clock 都不进入 fingerprint。
6. BP3 不读取 `latestWindowMetadata()`、不复制 BP1 monotonic checkpoint fence、不增加 metadata observation。
7. 两个无 context public lifecycle API 继续使用 BP2 final source 冻结的 holder scope 规则；holder 为空才进入
   typed no-context。显式 `windowId` 仍是 session 内 logical participant id，不得被 default/global alias 覆盖。

### 3.7 Legitimate successive-context A-B-A 不复活

对同一 scoped logical window，`currentFingerprintByWindow.compute(...)` 是唯一 generation transition 点；generation
compare、old-binding 最小清理和 incoming replacement 必须在同一个现有 service 同步边界中完成：

1. 无 current fingerprint：登记 incoming，不清理。
2. current 与 incoming record-equal：保留全部 state，不清理、不抖动。
3. current 与 incoming 不同：先且仅一次清理 old binding 的最小闭包，再把 incoming 设为唯一 current。
4. A 建 state 后进入 B：B 不继承 A；其它 scope/window/session/team 保持。
5. B 建 state 后再次进入值等价 A：再次清 B，并建立 fresh A；第一次 A 的 cache/cooldown/claim/formal/local
   evidence 不得从 history、fallback map 或 alias 复活。
6. generation transition 自身不注册 candidate、role、leader、completed、capability，不伪造业务事实。

这只是合法 successive contexts 的 observed-current latch。协议没有 server-issued generation sequence；不得把它扩展为
并发 stale invocation 排序器、全方法 long-wait mutex、永久 generation registry 或 retry 机制。

### 3.8 Old-binding 精确清理闭包

`invalidateBindingScopedState(oldBinding)` 只能在异代 transition 中清理：

1. **四个 per-window map**：只按 old scoped-window key 删除第 3.5 节四项，包括完整 Summon layout/tail/
   ultimate/slot cache；不碰其它 scoped window。
2. **Claim owner**：从 BP2 actual formal/local claim owner set 精确移除 old scoped-window；只删除因此变空的 exact
   claim entry，不删除其它 owner、team、session 或 scope。
3. **Formal team**：只处理 `formalTeamBindingByKey` value 与 old binding record-equal 的 exact team；删除该 binding、
   active round、该 team 的 window-state entries 与 formal claims。formal state 写入口登记 current binding；query/await
   不篡改 owner。普通 close 仍只把当前 round 置 `CLOSED`，不变成 terminal teardown。
4. **Local session**：只扫描同 execution scope 的 exact session；participant fingerprint 必须等于 old fingerprint 才
   移除该 participant，并移除该 raw window id 的 role-detected/completed evidence；candidate 保留。旧 leader 漂移时
   清其 leader evidence、open capabilities/capability epochs 及该 exact session local claims；普通 member 漂移不得清
   仍有效 leader/其它 member/capability。
5. **Monitor**：只有 formal/local shared state、claim 或 capability 实际变化时，才对既有
   `teamMaintenanceWindowMonitor` 做 `notifyAll()`；不启 timer/thread/queue，不 sleep，不在 monitor 内执行 delegate。

### 3.9 Existing terminal cleanup 边界

`completeLocalTeamSessionWindow(sessionKey, windowId, sourceTask)` 仍只在 candidate 为空或 completed 覆盖全部
candidates 时 compare/remove exact scoped session。成功移除时只同步删除该 exact session 的 local capability claims
并通知既有 monitor；不清其它 session/scope、formal team、per-window cache 或 current fingerprint。

单个 leader/member 完成不得提前移除共享 session；generation drift 不得把 candidate 标成 completed。Task STOP、
`TaskStopRequestedException`、`TaskFatalException`、UNCERTAIN 或 Task 方法返回不自动调用该 API，不新增 terminal/
restart hook，不激活现有零 caller lifecycle 路径。

### 3.10 Exact 19 public APIs / constructor / TURN-34A 六 API

以下 19 个 public instance method 的名称、参数顺序、返回类型、可见性及既有异常/等待语义保持：

```text
void initializeForTaskStart(TaskExecutionContext,String)
void beginTeamMaintenanceRound(TaskExecutionContext,String,int,String)
void openTeamPathingMaintenanceWindow(TaskExecutionContext,String,int,String)
void openTeamFirstAidMaintenanceWindow(TaskExecutionContext,String,int,String)
void closeTeamMaintenanceWindow(TaskExecutionContext,String,int,String)
void openLocalTeamReturnSupportWindow(TaskExecutionContext,String)
void closeLocalTeamReturnSupportWindow(TaskExecutionContext,String)
boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext,String)
boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext,String,long)
boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability,long)
boolean isLocalSupportMemberSession(TaskExecutionContext)
void registerLocalTeamSessionCandidate(String,Collection<String>,String)
void markLocalTeamWindowRoleDetected(TaskExecutionContext,String,String,String)
boolean isLocalSupportMemberCandidate(TaskExecutionContext)
boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext)
void markLocalTeamLeaderDetected(TaskExecutionContext,String,String)
boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability)
void completeLocalTeamSessionWindow(String,String,String)
TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext,TaskMaintenanceRequest)
```

Lombok constructor 的五个 collaborator 保持且不增第六项：`BotProperties`、`GameContext`、`DialogService`、
`SummonSkillService`、`TaskExecutionContextHolder`。

TURN-34A/`AutoCombatService` 使用的六 API 必须单列冻结：

```text
isPendingLocalSupportLeaderDetection
isLocalSupportMemberSession
isLocalTeamSupportCapabilityOpen
awaitLocalTeamSupportCapabilityOpen
isLocalSupportMemberCandidate
awaitTeamFirstAidMaintenanceWindowOpen
```

以下四个 lifecycle API 当前 service 外 production caller 为零，BP3 不新增 caller/host/factory/runtime 激活：

```text
registerLocalTeamSessionCandidate
markLocalTeamWindowRoleDetected
markLocalTeamLeaderDetected
completeLocalTeamSessionWindow
```

### 3.11 `runOpportunisticMaintenance` 与 `696a12b0` 保持

主序必须保持：

```text
normalize request
-> existing first checkpoint
-> resolve typed current binding
-> optional broadcast
-> handled / BROADCAST_FAILED / INTERRUPTED short-circuit
-> optional at-most-one Summon delegate
-> no-action
```

不得把 binding/generation mutation 移到 first checkpoint 前；Summon 前既有 second checkpoint 保留，不增加第三个。
同时保持：CommonBox 优先、TeamReturn capability-only、Summon feature/interval/FREE/due/UNKNOWN/static-tail/
2h-cache gates、formal/local claim acquire/duplicate/max/release/retain、capability epoch、`GameContext.ActionState`
恢复及 capability open/close 集合 `5/1/5/2`。五倍/修罗的 prompt、OCR/template/click/navigation、phase、
keep-turn/park、retry/fallback、verification count、expiry 与 STOP 语义均不改。

禁止新增 TTL、额外 verification/read/checkpoint、park/yield、retry、cleanup policy/pass、fail-closed business gate、
sleep、timer、thread、queue、session authority、owner、lease、ledger、transport/runtime/lifecycle authority。

固定业务差异字段：

```text
Intentional business differences: 无已批准业务差异；按 696a12b0 等价迁移。
Checked rules: docs/业务逻辑.md lines 5-67, 170-224, 412-479, 1178-1187, 1253-1288.
```

### 3.12 Terminal / UUID / zero-action 计数口径

卡内验收轴必须区分 coordinator 与 delegate：

1. 纯 scope/key/fingerprint/generation/query/cleanup 路径：Dialog delegate=0、Summon delegate=0、
   `TurnGameClient.execute`=0、command/action/actionId/UUID=0、额外 metadata read/checkpoint=0。
2. initial checkpoint 的 STOP/missing/device/window/native mismatch：existing typed result/exception 传播一次；generation
   registry 与全部 maintenance state 零变化，Dialog/Summon/execute/action/UUID 全零。
3. broadcast 既有 handled/failed/interrupted：按原 result 一次短路，不进入 Summon，不产生第二 delegate/command。
4. eligible Summon：`cleanSummonSkillsOnce` 恰一次；`TaskMaintenanceService` 自身不 mint UUID/action。真实 TURN-33
   delegate 可按自身合同产生一个 action/UUID，不得把这一分支错误写成 whole-chain zero action。
5. Summon 抛 STOP/terminal/`DUPLICATE_OR_UNCERTAIN`/correlation fatal：同一 exception 一次传播，delegate=1，
   无第二脚本消费、retry/replay/resend，previous `ActionState` 恢复，成功 cooldown/cache 不伪刷新。
6. known failure 且无 state change：按基线释放 claim；下一次 explicit public invocation 是新调用，不叫 transport retry。
7. delete/ultimate state-change failure：按基线保留 claim；同 round 后续 explicit invocation 不重复 delegate。
8. UNKNOWN：首次 delegate=1 并写既有 configured retry-after；紧接显式调用 deferred，累计 delegate 仍为 1。
9. success：delegate=1，既有 status/cooldown/cache 投影一次，previous state 恢复。
10. A-A、A-B-A、scope isolation、leader/member drift 与 all-candidate terminal cleanup 的 **transition/query/cleanup
    调用本身**均为 zero-delegate/zero-action/zero-UUID；为预先建立 cooldown/cache/claim 的显式 baseline setup 若按
    既有业务合同调用 delegate，必须单独计数，不能被误报为 BP3 transition action。不得用 mocked-away production
    boundary 或 source scan 代替 public observable 证据。

### 3.13 Owner / first-window / delivery gates

未来卡的 owner 字段模板：

```text
Assigned lane: [PARENT_FILL_EXTERNAL_LANE]
Owner before physical true-EOF claim: NONE
Claim requires:
- fixed card exists at physical EOF;
- claim-time TaskMaintenanceService SHA/lines/bytes exactly equal BP2 parent receipt;
- BP1 frozen source/test identities still match;
- no BP2/BP3/repair/other maintenance writer exists;
- caller/API frozen scan still matches.
```

Claim 后首个五分钟窗口必须满足三选一：

1. `TaskMaintenanceService.java` 出现真实 production source increment；
2. 写 canonical source delivery；
3. 写 `OWNER RETURNED`，包含当前 SHA/lines/bytes、零/已有增量说明并释放 owner。

仅有 claim 文本、heartbeat、私有笔记、helper 报告或 unchanged bytes 不构成 first-window progress。旧 heartbeat/旧
assignment 不得复活 owner；同一 production file 永远只允许一个 physical true-EOF owner。

Canonical delivery 必须填：

| Delivery field | Required value |
|---|---|
| Parent-received BP2 starting identity | SHA `[PARENT_FILL_BP2_FINAL_SHA]`; lines `[PARENT_FILL_BP2_FINAL_LINES]`; bytes `[PARENT_FILL_BP2_FINAL_BYTES]` |
| Claim-time identity | SHA `[WORKER_FILL_CLAIM_SHA]`; lines `[WORKER_FILL_CLAIM_LINES]`; bytes `[WORKER_FILL_CLAIM_BYTES]`; must equal BP2 receipt |
| BP3 final production identity | SHA `[WORKER_FILL_BP3_FINAL_SHA]`; lines `[WORKER_FILL_BP3_FINAL_LINES]`; bytes `[WORKER_FILL_BP3_FINAL_BYTES]` |
| Exact write set proof | `TaskMaintenanceService.java + append-only BP3 card`; every other file unchanged |
| Changed index | exact field/method/private-type line index after final bytes |
| Four per-window maps | exact final declarations and typed key |
| Fingerprint/generation | exact types, current registry, transition point and authority/catch path |
| A-B-A cleanup | per-window/claim/formal/local/monitor method index; no revival |
| Public surface | 19 signatures; five constructor collaborators; TURN-34A six APIs; four zero-caller APIs |
| Zero surface | no new metadata read/checkpoint/delegate/command/action/UUID/retry/TTL/timer/thread/queue/ledger/lease |
| Baseline | `无已批准业务差异；按 696a12b0 等价迁移` |
| Unrun/later gates | sole named test, parent source reread, independent reviews and Cloud compile/build remain separate |
| Owner | released immediately after canonical delivery; worker stops editing and does not self-approve |

Worker delivery 真尾模板：

```text
<!-- TRUE_EOF: TURN-34BP3 [LANE] SOURCE DELIVERED START=[BP2_PARENT_RECEIPT_SHA]/[LINES]
FINAL=[BP3_FINAL_SHA]/[LINES] WRITESET=[TaskMaintenanceService.java,this-card] OWNER_RELEASED
NO_SELF_APPROVAL [TIMESTAMP] -->
```

父级后续必须从磁盘独立复算 BP3 final SHA/lines/bytes 并写 parent source receipt；后续 sole
`TaskMaintenanceTurnContractTest.java` writer 只能以该 BP3 parent-received SHA 为 production target。BP3 production
worker 不创建/修改测试，不运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，不执行
Git mutation，也不写 `APPROVED`、`DONE`、P0/P1/P2 verdict 或父卡关闭结论。

## 4. 父级冻结前的机械填空清单

- [ ] BP2 canonical delivery 已在 BP2 卡物理真尾，且 owner 已释放。
- [ ] 父级已从磁盘复算 BP2 final source，并把 exact SHA/lines/bytes 与 receipt marker 写入 BP2 卡。
- [ ] 当前 Cloud source 精确等于 BP2 parent receipt，且同文件零 active writer。
- [ ] BP2 final actual private types/四 shared maps 已逐字抄入模板，未使用 readiness 猜测别名。
- [ ] BP1 latest parent receipt 的 source/test SHA/lines/bytes 未被后续 Repair 取代。
- [ ] AutoCombat source/test 与 caller scan 已重新冻结，19/5/6/4 计数均已填写。
- [ ] 单一 production + 卡的写集已写死，tests/context/callers/其它 Services 全只读。
- [ ] 四 per-window maps、fingerprint、generation current latch、cache/invalidation 闭包均已写死。
- [ ] A-B-A 不复活、same-A 不误清、scope/session/window isolation 与 leader/member drift 已写死。
- [ ] terminal/UUID/zero-action 的 coordinator-vs-delegate 计数口径已写死。
- [ ] `696a12b0`、`5/1/5/2`、无业务差异与禁止新增 TTL/retry/checkpoint/authority 已写死。
- [ ] owner、claim-time recompute、first-window 三选一、canonical delivery 与 owner release 模板已填写。
- [ ] 所有 `[PARENT_FILL_*]` 占位符均已替换为物理证据；卡片真尾才可成为未来 claim surface。

本 helper 只提供上述模板，不把清单状态转换成 READY/Blocked/Approved，也不创建、claim 或审查 BP3/BP2。

TRUE_EOF PRECHECK_COMPLETE
