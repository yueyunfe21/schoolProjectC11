# CR271 Internal TURN-34BP3 Readiness Preflight

> 日期：2026-07-16 EDT  
> 角色：Internal readiness helper；非 implementation owner、非 reviewer、非 approver  
> 目标：承接 TURN-34BP2 已冻结的 scoped team/session/claim typed-key，收敛 TURN-34BP3 的 per-window 与合法 successive-context A -> B -> A generation lifetime  
> 实际写集：仅本报告  
> 纪律：不修改 Java/卡片状态，不运行 Maven/JUnit/compile/runtime/input，不执行 Git mutation，不裁定 READY/Blocked/Approved

## 1. Authority 与本报告边界

本报告已完整核对：

- `AGENTS.md`；
- `docs/DHXY_CONTEXT.md`；
- `docs/ACTIVE_WORK.md` 顶部 CR271；
- 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节；
- `2026-07-15-https-turn-thin-client-protocol-design.md`；
- `docs/业务逻辑.md`，包括本地组队 session 边界、Summon/maintenance 规则与修罗
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 严格基线；
- TURN-34B、TURN-34BP1、TURN-34BT1 fixed card 的最新 physical true EOF；
- TURN-34BP1 implementation preflight、TURN-34BP2 readiness preflight、retained-production preflight、
  sole named-test preflight 与较早的 minimal-source-slices 报告；
- Cloud 当前 `TaskExecutionContext`、`TaskExecutionContextHolder`、scope/invocation/window metadata 类型、
  `TaskMaintenanceService`、其 19 个 production caller surface 及 `696a12b0` baseline source。

第 14-19 节覆盖此前冲突的卡片细节。HTTPS Turn 协议继续固定：一个 action 一枚 actionId/UUID、同窗口单
in-flight、terminal/uncertain 不自动重执行、Cloud 拥有业务 retry/fallback/phase，DHXY 只执行 mechanics。

本报告只给父级提供可落卡的 BP3 合同，不创建 BP3 fixed card，不写 claim，不给父卡增加 finding，也不把前置事实
翻译成卡片批准或阻断结论。

## 2. Physical snapshot 与 true EOF 事实

### 2.1 Cloud 当前字节

| Artifact | 当前事实 |
|---|---|
| `TaskMaintenanceService.java` | 1224 physical lines，66012 bytes，SHA-256 `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC`，mtime `2026-07-16T08:17:40.6760891-04:00` |
| `TaskExecutionContext.java` | 491 physical lines，19979 bytes，SHA-256 `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` |
| `TaskMaintenanceTurnContractTest.java` | 不存在 |
| `migration-baseline/696a12b0/.../TaskMaintenanceService.java` | 1123 physical lines，SHA-256 `4BEAFFD08314F694B41A841DFF236C4CE00DC335CBE75DE74A9F667A53803EDA` |

Cloud 工作树位于 `navigation-migration`，DHXY 工作树位于 `thin-client-design`；两边均已有大量
dirty/untracked。本 helper 未触碰、移动、清理或回滚任何现有文件。

当前 retained WIP 仍是 BP2/BP3 之前的形状：四个 per-window map、formal team round/window、local session 与
claim 仍以 `String` 为 key；`currentIdentityToken(...)` 仍使用 delimiter-concatenated identity；A -> B -> A
只会重置 Summon cache/cooldown 的一部分，不会清 formal claim/local capability participation。也就是说，BP2
readiness 是冻结合同，不是已经进入当前 Java 的实现。

### 2.2 最新 physical true EOF

1. TURN-34B 最新 EOF：
   `PARENT RETAINED-PRODUCTION-REVIEW-1 P0P1P2=0/2/1 TURN-34BP1-EXTERNAL-D-READY TURN-34BT1-EXTERNAL-B-PARALLEL 2026-07-16T09:26:55.020-04:00`。
2. TURN-34BP1 最新 EOF：
   `PARENT STALE-D-REVOKED ZERO-OWNER EXTERNAL-D-REPLACEMENT-READY CLAIM-REQUIRED 2026-07-16T09:38:31.235-04:00`。
   当前没有 replacement claim/source/test delivery。
3. TURN-34BT1 最新 EOF：
   `PARENT OWNER-RETURN-ACCEPTED ZERO-OWNER ZERO-TEST-BYTES FUTURE-REPLACEMENT 2026-07-16T09:38:31.235-04:00`。
4. TURN-34BP2 readiness 报告以 `TRUE_EOF PRECHECK_COMPLETE` 结束；当前没有 BP2 fixed child card、claim、
   source delivery 或 parent source receipt。

这些是 BP3 source-start 的输入事实，不是本 helper 的 review verdict。

## 3. BP3 精确责任

BP2 只解决共享 state 的 typed ownership：

```text
Map<ScopedTeamKey, Integer> activeTeamRoundByKey
Map<TeamRoundKey, TeamMaintenanceWindowState> teamMaintenanceWindowStateByRound
Map<ScopedLocalSessionKey, LocalTeamSessionState> localTeamSessions
Map<MaintenanceClaimKey, Set<ScopedWindowKey>> summonSkillClaimsByTeamRound
```

BP3 只解决同一 `ScopedWindowKey` 的 generation lifetime：

1. 四个 per-window map 改用 BP2 的 `ScopedWindowKey`；
2. 使用 exact native/legacy/null typed fingerprint，不再使用 identity 字符串；
3. 合法 successive contexts A -> B -> A 每次跨 fingerprint 都先清上一 generation，第一份 A 永不复活；
4. 清理范围同时覆盖 per-window cooldown/cache、formal team owner/round/claim 和 local-session participation；
5. 明确 existing local-session completion 的 terminal cleanup，但不新增 task terminal hook；
6. 不改变任何 maintenance/Summon/team business gate、priority、delegate、result、retry 或 expiry。

BP3 不是 protocol generation-id 卡。当前 `TurnWindowMetadata` 没有 server-issued generation sequence；这里的
generation 是一个已通过既有 context authority 的 exact binding fingerprint。不得虚构协议字段、lease、ledger、
task-run generation 或 durable registry。

## 4. Exact source-start dependencies

父级只有在 BP3 child card 中逐项记录以下事实后，才具备可审计的 source-start 输入：

1. **BP1 真实交付。** TURN-34BP1 physical EOF 已有 fresh replacement claim、真实
   `TaskExecutionContext.java`/`TaskExecutionContextTurnContractTest.java` 增量、`SOURCE+TEST DELIVERED`、
   final SHAs 与 owner release；当前 `REPLACEMENT READY` 不等于交付。
2. **BP1 parent receipt。** 父级已静态核对 missing -> device -> window -> title/HWND/process 顺序、同一旧
   context 的 sticky A -> B -> A' invalidation、一次 checkpoint 一次 metadata read、零 command/UUID/retry，
   并记录 source/test-source receipt。完整 Maven/build 可留到 stable-writer final gate。
3. **BP2 真实交付。** BP2 fixed card 已先创建并 true-EOF claim；四个 shared maps 和全部 formal/local/claim
   path 已按 BP2 typed-key 合同交付，physical EOF 给出 final `TaskMaintenanceService.java` SHA、修改方法索引、
   owner release 与 parent source receipt。当前 `963B028C...` 只能作为 BP2 起点，不能作为 BP3 起点。
4. **BP2 shape reconciliation。** 父级把 BP2 最终实际 private type/field 名称逐项抄入 BP3 child card。若 BP2
   使用合同等价但不同名称，BP3 复用实际类型，不得再建别名/adapter/第二套 key。
5. **唯一 production owner。** BP3 claim 前 `TaskMaintenanceService.java` 必须零 active writer，且 SHA 与 BP2
   parent receipt 完全一致；有任何漂移就重新做窄 preflight，不能套用本报告的旧行号或旧 SHA。
6. **测试 owner 串行。** sole `TaskMaintenanceTurnContractTest.java` 仍应在 BP2/BP3 source 收口后针对最终 SHA
   冻结。若 replacement BT1 已先领取，父级必须记录其 target production SHA 和 explicit handoff，禁止旧断言与
   BP3 production 并写。
7. **卡片先于 claim。** BP3 implementation owner 只能在父级预建的 fixed child card physical EOF 领取；本
   helper 报告不是 claim surface。
8. **最终门不冒充 source-start 门。** TURN-22、TURN-33、TURN-34A 六 API、sole named test、两独立 reviewer、
   Cloud compile/build 是 TURN-34B 最终门；除非它们暴露直接 source blocker，不要求 BP3 source writer先运行。

本节只定义证据依赖。是否据此创建/领取卡片由父级决定。

## 5. Future BP3 exact write set

BP3 implementation owner 的写集必须恰好两项：

1. 修改 Cloud
   `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`。
2. append-only 更新父级预先创建的
   `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP3.md`
   的 claim/heartbeat/source-delivery 段。

以下全部只读：

- `TaskExecutionContext.java`、`TaskExecutionContextHolder.java` 及 BP1 test；
- 不存在的 `TaskMaintenanceTurnContractTest.java` 及所有其它 test；
- `AutoCombatService`、`AutoBattleTask`、Wubei/Xiuluo task 与 caller tests；
- `SummonSkillService`、TURN-33、Dialog/CommonBox/TeamReturn/PlayerState；
- protocol/client/result/model/POM/config/resources；
- DHXY 其它 Markdown、dashboard 与全部 Java。

本 helper 的实际写集仍只有本报告；它不代建上述 child card。

## 6. Exact typed generation model

### 6.1 必须复用的 BP2 types

BP3 直接复用 BP2 的 `ExecutionScopeKey`、`ExactExecutionScopeKey`、`NoContextScopeKey`、
`ScopedWindowKey`、`ScopedLocalSessionKey`、`TeamCoordinationKind`、`ScopedTeamKey`、
`MaintenanceClaimKey`、`TeamRoundKey` 与 `LocalCapabilityRoundKey`。不得恢复 raw key、delimiter tuple、
prefix parse、exact+fallback dual lookup 或兼容双 map。

### 6.2 BP3 只新增的 private types

类型放在 `TaskMaintenanceService` 文件底部：

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
        ScopedWindowKey windowKey,
        WindowGenerationFingerprint fingerprint) {}
```

若 BP2 final source 已冻结合同等价的实际名称，父级在 child card 中用实际名称一对一替换以上名称；语义与字段
不得变化，也不得保留两组类型。

### 6.3 Exact field delta

四个 BP2 明确留给 BP3 的字段只替换 key type：

```text
Map<ScopedWindowKey, Long> lastSummonSkillCleanAtByWindow
Map<ScopedWindowKey, Long> lastSummonSkillNotDueLogAtByWindow
Map<ScopedWindowKey, Long> summonSkillUnknownRetryAfterByWindow
Map<ScopedWindowKey, SummonSkillWindowState> summonSkillStateByWindow
```

只新增两个当前绑定索引：

```text
Map<ScopedWindowKey, WindowGenerationFingerprint> currentFingerprintByWindow
Map<ScopedTeamKey, WindowBindingKey> formalTeamBindingByKey
```

`LocalTeamSessionState` 只增加：

```text
Map<ScopedWindowKey, WindowGenerationFingerprint> participantFingerprints
```

同时删除 `SummonSkillWindowState.identityToken` 以及 current source 中只为字符串 identity 服务的
`DEFAULT_IDENTITY_TOKEN`、`currentIdentityToken(...)`、`identityTail(...)`、
`nativeHandleOrNull(...)`、`nativeProcessIdOrNull(...)`。BP2 若仍留下 `scopePrefix(...)`/String
`currentWindowKey(...)`，BP3 删除其 identity/key 拼接职责；不得把它们改成一行 wrapper 继续嵌套。

两个索引只保留当前值，不保留 history、counter、tombstone、owner lease、task-run ledger、TTL 或后台 compaction。

## 7. Fingerprint authority 与 A -> B -> A transition

### 7.1 Authority resolution

保留 BP2 的 `effectiveContext(context)` supplied-context-first 规则：显式参数胜过 holder；只有 null supplied +
empty holder 落入 typed no-context namespace。已有 context 的 authority failure 必须传播，不能 broad-catch 后降级。

唯一真实边界 `resolveCurrentBinding(...)` 按以下顺序构造：

1. 先用 BP2 resolver 得到 `ExecutionScopeKey + windowId`，构造 `ScopedWindowKey`；
2. non-null context 先调用 `getNativeWindowTitle()`：成功即构造
   `ExactNativeWindowFingerprint(title, getNativeWindowHandle(), getNativeWindowProcessId())`；
3. 只允许捕获该 title accessor 在 legacy path 明确抛出的 `IllegalStateException`，随后读取
   `getPlayerIdentityEpoch()` 构造 `LegacyPlayerWindowFingerprint`；epoch authority failure 原样传播；
4. null/empty-holder path 使用 `NoContextWindowFingerprint.INSTANCE`；
5. title/HWND 文本使用原始 exact value，不 trim、不拼接；process id 使用原始 `long`；
6. `windowRect`、pause、stop、taskRunId、task/round/session、role、requested task 与 wall clock 均不属于
   fingerprint。

BP3 不读取 `latestWindowMetadata()`，也不复制 BP1 fence。`runOpportunisticMaintenance` 继续先执行现有
checkpoint；该 checkpoint 是 latest metadata/STOP/pause authority。其它 public API 继续依赖传入的既有
`TaskExecutionContext` authority，不为 BP3 增加第二 observation 或 checkpoint。

### 7.2 Linearized sequential transition

对同一 `ScopedWindowKey`，`currentFingerprintByWindow.compute(...)` 是唯一 transition 点：

1. 无 current fingerprint：登记 incoming fingerprint，不清理；
2. current 与 incoming record-equal：保留，不清理；
3. 不相等：在该 compute transition 中调用一次
   `invalidateBindingScopedState(new WindowBindingKey(windowKey, oldFingerprint))`，随后只保存 incoming；
4. 返回 incoming `WindowBindingKey`，本次 public invocation 后续只使用这个 typed binding/key；
5. B -> A 仍因 current=B、incoming=A 而再次清理 B。第一份 A 已在 A -> B 时删除，不能从任何 history 恢复。

这里冻结的是 BP2 报告所说的 **legitimate successive-context** 合同。协议当前没有不可复用的 server generation
number，因此不能宣称可识别“从未被调用观察到的 B”，也不能把任意并发 stale context 判为新旧绝对顺序。
TURN-native `runOpportunisticMaintenance` 的旧 context 先受 BP1 sticky latest-metadata fence 约束；其它 public
API 仍依赖既有 single-window task/context ownership。若父级要求跨并发 stale invocation 的强线性化，需要协议
generation 或独立 lifecycle 卡，不能在 BP3 偷加锁住 long wait 的全方法 mutex、counter 或 retry。

## 8. Exact invalidation write set

`invalidateBindingScopedState(oldBinding)` 只能做下面五组删除，并在一个 transition 中至多执行一次：

### 8.1 Per-window state

按 `oldBinding.windowKey()` 删除：

- `lastSummonSkillCleanAtByWindow`；
- `lastSummonSkillNotDueLogAtByWindow`；
- `summonSkillUnknownRetryAfterByWindow`；
- `summonSkillStateByWindow` 的完整 layout/tail/ultimate/slot cache。

不得保留 identity-token fallback，也不得清其它 `ScopedWindowKey`。

### 8.2 Claim owner

从所有 `summonSkillClaimsByTeamRound` owner set 精确删除 `oldBinding.windowKey()`；只删除因此变空的 entry。
这一步同时适用于 formal `TeamRoundKey` 与 local `LocalCapabilityRoundKey`，但不删除其它 window owner。

### 8.3 Formal team binding

只处理 `formalTeamBindingByKey` value 与 `oldBinding` record-equal 的 `ScopedTeamKey`：

1. compare/remove 该 exact binding entry；
2. 删除该 `ScopedTeamKey` 的 `activeTeamRoundByKey`；
3. 删除所有 `teamMaintenanceWindowStateByRound` 中 teamKey 等于它的 `TeamRoundKey`；
4. 删除所有 formal `TeamRoundKey` claim；不得借此删除别的 team 或 local capability claim；
5. `beginTeamMaintenanceRound`、两个 formal open 与 `closeTeamMaintenanceWindow` 作为 formal state 写入口，
   必须把其 current `WindowBindingKey` 关联到实际 `ScopedTeamKey`；query/await 不得改写 owner。

`closeTeamMaintenanceWindow` 继续只把当前 round window 置为 `CLOSED`；它不是新增 terminal teardown，不能顺手删
active round/claim。上述删除只发生于 exact generation transition。

### 8.4 Local-session participation

只扫描与 `oldBinding.windowKey().scope()` 相同的 `ScopedLocalSessionKey`：

1. 只有 `participantFingerprints.get(oldWindowKey)` 等于 old fingerprint 才移除该 participant；
2. 移除该 raw window id 的 `roleDetectedWindows` 与 `completedWindows`；
3. `candidateWindows` 保留，因为它代表同一 UI batch 选中的 logical window，不代表旧 native generation；
4. 若 `leaderWindowId` 等于该 window id：清 leader evidence、设 `leaderAbsent=false`、清 open capabilities 与
   capability epochs，并删除该 exact session 的全部 `LocalCapabilityRoundKey` claims；
5. 若旧 binding 只是 member，不清 leader/其它 member/capability，只由 8.2 移除该 window 的 claim owner。

incoming generation 只有在当前 public operation 本来就具有 register/role/leader 语义时才重新登记对应证据；
generation transition 本身不能伪造 candidate、role、leader、completed 或 capability。

### 8.5 Monitor

只要 formal/local shared state、claim 或 capability 实际变化，就在既有 `teamMaintenanceWindowMonitor` 上
`notifyAll()`。不启动 timer/thread/queue，不 sleep，不在 monitor 内执行 delegate，也不新增 periodic cleanup。

## 9. Terminal cleanup 的精确含义

BP3 的 `terminal cleanup` 只补齐现有 public local-session lifecycle 的 typed terminal 边界：

- `completeLocalTeamSessionWindow(sessionKey, windowId, sourceTask)` 继续只在
  `candidateWindows` 为空或 `completedWindows` 覆盖全部 candidates 时 compare/remove 该 exact
  `ScopedLocalSessionKey`；
- 成功移除 session 时，同时删除 key.session 等于该 session 的 `LocalCapabilityRoundKey` claims，并通知现有
  monitor；不清 per-window cooldown/cache、current fingerprint、formal team state、其它 scope/session；
- 单个 leader/member 完成时仍不得提前移除共享 session；generation transition 也不得把 candidate 标成完成；
- `registerLocalTeamSessionCandidate`、`markLocalTeamWindowRoleDetected`、
  `markLocalTeamLeaderDetected`、`completeLocalTeamSessionWindow` 当前四个 production caller 仍为零，BP3 不新增
  host/factory/task/runtime caller。

Task STOP、`TaskStopRequestedException`、`TaskFatalException`、UNCERTAIN 或 task 方法返回 **不自动调用**
`completeLocalTeamSessionWindow`，也不触发 task-global purge。为 task terminal/restart 新增 caller、hook 或
lifecycle registry 会改变现有运行边界，超出 BP3 单文件写集。这里不把 STATE profile 的一般 terminal 目标偷换成
一个未授权的 TaskMaintenance teardown。

## 10. Public acceptance

### 10.1 Exact 19-signature set

BP3 后下面 19 个 public instance method 的名称、参数顺序、返回类型、可见性必须完全不变：

```text
void initializeForTaskStart(TaskExecutionContext,String)
void beginTeamMaintenanceRound(TaskExecutionContext,String,int,String)
void openTeamPathingMaintenanceWindow(TaskExecutionContext,String,int,String)
void openTeamFirstAidMaintenanceWindow(TaskExecutionContext,String,int,String)
void closeTeamMaintenanceWindow(TaskExecutionContext,String,int,String)
void openLocalTeamReturnSupportWindow(TaskExecutionContext,String)
void closeLocalTeamReturnSupportWindow(TaskExecutionContext,String)
boolean isTeamPathingMaintenanceWindowOpen(TaskExecutionContext,String)
boolean awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext,String,long) [34A]
boolean awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability,long) [34A]
boolean isLocalSupportMemberSession(TaskExecutionContext) [34A]
void registerLocalTeamSessionCandidate(String,Collection<String>,String)
void markLocalTeamWindowRoleDetected(TaskExecutionContext,String,String,String)
boolean isLocalSupportMemberCandidate(TaskExecutionContext) [34A]
boolean isPendingLocalSupportLeaderDetection(TaskExecutionContext) [34A]
void markLocalTeamLeaderDetected(TaskExecutionContext,String,String)
boolean isLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability) [34A]
void completeLocalTeamSessionWindow(String,String,String)
TaskMaintenanceResult runOpportunisticMaintenance(TaskExecutionContext,TaskMaintenanceRequest)
```

Production constructor shape也不变。TURN-34A 六个标记 API 的 closed/open/zero-timeout/await 行为必须保持。

### 10.2 Binding placement

- 每个 context-bearing public entry 在首次读写 per-window/formal/local state 前解析 current binding；private path
  传递 typed binding/key，不重复制造 String identity，也不形成 resolve/prepare/handle wrapper ladder。
- `runOpportunisticMaintenance` 保持 `normalize request -> existing checkpoint -> resolve binding -> broadcast ->
  short-circuit -> optional Summon`；不得把 resolve 移到 checkpoint 前。initial STOP 因而仍是零 state mutation。
- Summon 前的第二个既有 checkpoint 保留；BP3 不增加第三个 checkpoint/metadata read。
- 两个无 context public APIs 继续使用 BP2 的 holder scope；holder empty 使用 typed no-context。显式 `windowId`
  仍是 session 内 logical participant id，不允许用 default/global alias 覆盖。
- query/await 只消费当前 typed state；timeout、interrupt、monitor wait 与返回语义保持现值，不增加 sleep 或线程。

### 10.3 Observable A -> B -> A acceptance

只通过 public API 可观察地证明：

1. A 建立 cooldown、not-due/unknown interval、Summon cache、formal round/window/claim、local role/leader/capability；
2. 同 `ScopedWindowKey` 的合法 B 第一次进入即不继承任何 A-owned state；其它 window/session/scope 保持；
3. B 再建立同类 state 后，合法 A 再进入也不继承第一份 A 或 B；
4. same exact fingerprint 的连续调用不误清 2h cache/cooldown/claim/capability；
5. same scope + same explicit local session 的不同 window 仍按 BP2/业务基线共享 capability；无 explicit session 的
   不同 window 仍隔离；
6. tenant/user/device/window 任一 scoped tuple 改变、以及 identifier 含 `|`/`#`/`local-team:`，均不碰撞；
7. supplied context 胜过冲突 holder；turn-native path 不调用 legacy-only epoch；legacy/null 不与 exact tuple alias。

## 11. Terminal、UUID 与 no-retry acceptance

BP3 scoping/rebind/cleanup 自身必须满足：

- Dialog delegate `0`、Summon delegate `0`、`TurnGameClient.execute` `0`、actionId/UUID `0`；
- 无第二 observation、自动 retry、fallback、sleep、timer、queue 或 background cleanup；
- generation 记录在已观察到合法 incoming context 后保留；后续 business delegate 抛异常时不回滚为旧
  fingerprint，也不伪造 success cooldown/cache。

最终 sole `TaskMaintenanceTurnContractTest` 还必须锁定以下既有 coordinator 合同：

1. initial metadata STOP：原 `TaskStopRequestedException`；Dialog=0、Summon=0、execute=0、UUID=0，且因 resolve
   位于 checkpoint 后而无 generation cleanup。
2. scripted Summon 抛 `TaskStopRequestedException`：同一 exception 传播，delegate=1，无第二脚本消费，previous
   `ActionState` 恢复，TaskMaintenance coordinator execute/UUID=0。
3. scripted Summon 抛 `TaskFatalException("DUPLICATE_OR_UNCERTAIN")` 或 correlation fatal：同一 exception
   传播，delegate=1，零自动 retry，成功 cooldown 不刷新，previous state 恢复。
4. known failed/no state change：单次 explicit public invocation delegate=1、返回既有 failed status、claim 按
   baseline 释放；下一次 explicit invocation 只用于观察，不叫 transport retry。
5. delete/ultimate state-change failure：claim 按 baseline 保留；下一次同 round explicit invocation zero delegate。
6. unknown result：第一次 delegate=1 并写既有 configured interval；紧接第二次 explicit invocation deferred，
   累计 delegate 仍为 1。
7. success：delegate=1，既有 success status/cooldown/cache 只投影一次，previous state 恢复。

这里的 execute/UUID=0 只证明 `TaskMaintenanceService` coordinator 不创建自己的 action。真实 TURN-33
`SummonSkillService` 一次 whole-pass action/UUID 1:1 仍由 `SummonSkillTurnContractTest` 负责；不得反向断言 eligible
Summon 的全链路 action/UUID 为零。

## 12. `696a12b0` 等价性

当前 retained WIP 与 `migration-baseline/696a12b0` 的 production 对照显示，既有业务方法的 decision/order 仍可
辨认为 context/scoping plumbing 迁移；BP3 必须继续保持下面的 strict baseline：

1. `checkpoint -> broadcast -> handled/failed/interrupted short-circuit -> optional one Summon -> no-action`；
2. Summon enabled/interval/FREE/due/unknown interval/2h cache/team/capability/pathing/duplicate/max/checkpoint 门序；
3. 一次 eligible maintenance 最多一次 TURN-33 typed delegate；
4. CommonBox priority、TeamReturn capability-only 边界、Summon static-tail ownership；
5. pathing open 精确 `FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS` 五项，weak
   first-aid 一项，team close 五项，return-support `TEAM_RETURN+COMMON_BOX` 两项；
6. formal/local claim acquire、same-window duplicate、max-cleaner、known-failure release、state-change retain、
   capability epoch 条件与顺序；
7. success/known failure/state-change/STOP/terminal/uncertain 的 status、exception、claim 与 ActionState 投影；
8. 修罗 phase、消息、probe/NPC/dialog/navigation、keep-turn/park、retry/fallback、verification/expiry 语义完全不动。

BP3 唯一允许的 plumbing outcome 是：不同 scoped tuple 或同 logical window 的不同 exact fingerprint 不再继承旧
ownership state；existing explicit local-session completion 同时清其 typed capability claims。不得借此新增 TTL、
额外 verification/read、park/yield、retry、cleanup pass、fail-closed gate 或 cloud business decision。

按仓库规则记录：`无已批准业务差异；按基线等价迁移`。若父级认为上述 scoped/generation lifetime 之外还需要任何
task terminal/restart 行为变化，必须另写具体行为选项与运行后果并取得用户明确决定，不能塞入 BP3。

## 13. Future sole named-test acceptance

BP3 source writer不写测试。后续 replacement test owner只追加唯一
`TaskMaintenanceTurnContractTest.java`，直接 new production service、使用 test-private scripted collaborators，
只从 public API 观察；禁止 source scan/private production reflection、Mockito/Spring/HTTP/runtime/input/capture、
wall-clock sleep 与第二测试类。

最终至少覆盖：

- 第 10.1 节 exact 19 signatures、五参 constructor、TURN-34A 六 API；
- BP2 scope/session/team/claim/delimiter/supplied-context cases；
- 第 10.3 节 full-state A -> B -> A；不能只测 Summon cache；
- formal duplicate/max、known failure release、state-change retain；
- `5/1/5/2` capability 与 all-candidate terminal completion；
- broadcast short-circuit、all zero-delegate gates、eligible one delegate；
- 第 11 节 terminal/UUID/no-retry matrix；
- existing 2h expiry 的父级另行冻结确定性方式，不以即时调用冒充覆盖。

计划授权的 stable-writer 命令仍是 `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`，随后适用 Cloud
compile/build。本 helper没有运行、也不声称通过任何命令。

## 14. BP3 source-delivery evidence

future implementation physical EOF 至少记录：

1. BP2 parent-received initial production SHA 与 claim 时 SHA；
2. final `TaskMaintenanceService.java` SHA/bytes/physical lines；
3. exact 两项写集清单，证明其它 Java/test/doc 未写；
4. 四个 per-window field 泛型、两个 binding index、local participant map 与删除的 String identity symbols；
5. 19 public signature set和四个 zero-caller API production reference count；
6. `resolveCurrentBinding` authority/catch/fingerprint 字段与 placement；
7. A -> B、B -> A 五组 cleanup 的逐 map/method 索引；
8. explicit local-session terminal cleanup 只删 exact session capability claims；
9. `runOpportunisticMaintenance` gate/delegate/result diff 与 `696a12b0` 对照；
10. source 中未新增 `UUID`、`actionId`、`execute`、retry/TTL/timer/thread/queue/ledger/lease；
11. 未运行门、remaining sole-test/reviewer/build debt 与 owner release；
12. 不写 `APPROVED`、`DONE`、`P0/P1/P2=0` 或父卡关闭结论。

父级 source review 应以 post-BP2 initial SHA 到 BP3 final SHA 的实际 diff 为准，不能用本报告替代复读。

## 15. 禁止项与交接口径

- 禁止修改 BP1 context/fence 或在 maintenance 内复制 latest metadata observation。
- 禁止修改/创建 test、caller、protocol/client/result/model/POM 或第二 production file。
- 禁止改 19 public API、constructor、四个 zero-caller API 可达性或 wait semantics。
- 禁止 raw/delimiter key、dual map/read/write、broad exception fallback、identity string compatibility shadow。
- 禁止把 `taskRunId`、pause/stop、window rect、task/round/session 加进 generation fingerprint。
- 禁止 global clear、跨 scope/session cleanup、candidate 删除、member drift 清 leader capability。
- 禁止 task terminal hook、startup/restart cleanup、TTL/lease/ledger/history/counter/tombstone/background compaction。
- 禁止额外 delegate/action/UUID/retry/checkpoint/metadata read/sleep/timer/thread/queue。
- 禁止修改业务 priority、gate/order/count/fallback/expiry、五/一/五/二 capability 或修罗 `696a12b0` 语义。
- 禁止实现者自批、代 reviewer、修改父卡 verdict/status 或以 helper 报告领取 source ownership。

建议父级串行顺序保持：BP1 source+test delivery/receipt -> BP2 source delivery/receipt -> BP3 source
delivery/receipt -> sole named-test tranches -> parent test-source review -> TURN-22/33/34A gates -> two independent
reviewers -> named test + Cloud compile/build -> parent final judgment。

TRUE_EOF PRECHECK_COMPLETE
