# CR271 TURN-34BP2 delivery/source-review acceptance preflight

## 1. 角色、用途与严格边界

- 角色：`CR271 Internal` 只读 helper，仅为父级将来收到 `TURN-34BP2` canonical delivery 后的逐文件 source review 准备检查清单和证据索引。
- 本文不是实现、review、批准、父级裁决、owner claim、CR 状态变更或 build 结果；不对任何当前中途源码字节作结论。
- 本轮唯一写入是本报告：
  `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-34bp2-delivery-source-review-preflight-helper.md`。
- 未修改 DHXY/Cloud Java、测试、TURN-34B/BP1/BP2 卡、权威计划、`ACTIVE_WORK.md`、dashboard、协议或业务逻辑文档。
- 未运行 Maven/JUnit/compile/package，未启动 runtime/application/server/Task/UI/capture/input，未执行 Git mutation。
- 两仓全部 dirty/untracked 都是受保护输入；不得以 reset、checkout、clean、stash、add/commit 或覆盖文件的方式制造“干净基线”。

本文只冻结未来 review procedure。任何实际 P0/P1/P2 数量、source receipt 或返修结论，必须由父级在 canonical delivery 之后针对同一最终 SHA 独立产生。

**业务裁决基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。无已批准业务差异；按基线等价迁移。**

## 2. 权威输入与时间切片

### 2.1 已完整读取的权威材料

1. `D:\mavenProject\DHXY\AGENTS.md`。
2. `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`。
3. `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` 顶部 CR271 连续区，含 `11:36`、`11:47`、`11:50`、`11:55`、`11:58` 最新父级记录。
4. `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
5. `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-15-https-turn-thin-client-protocol-design.md`。
6. `D:\mavenProject\DHXY\docs\业务逻辑.md` 全文。
7. TURN-34B、TURN-34BP1、TURN-34BP2 原卡至读取时 physical EOF。
8. 两份 BP2 readiness 材料：
   - `2026-07-16-turn-34bp2-readiness-preflight-helper.md`；
   - `2026-07-16-turn-34bp2-readiness-delta-helper.md`。
9. Cloud 当前 `TaskExecutionContext.java`、`TaskExecutionContextHolder.java` 与 `TaskMaintenanceService.java` 全文。
10. strict `696a12b0` 的 `TaskMaintenanceService.java` 全文，以及 Cloud `AutoCombatService` 的六 API caller 和 CommonBox-before-first-aid 调用点。

### 2.2 冻结身份

| Artifact | 冻结身份/用途 |
|---|---|
| BP2 起始 `TaskMaintenanceService.java` | **1,224 行 / SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`**；未来 exact diff 的唯一 BP2 起点 |
| BP1 `TaskExecutionContext.java` | 527 行 / SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e`；BP2 只读 |
| BP1 `TaskExecutionContextTurnContractTest.java` | 872 行 / SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785`；BP2 只读 |
| strict 696 source | `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`，1,123 行 / SHA-256 `4beaffd08314f694b41a841dff236c4ce00dc335cbe75de74a9f667a53803eda` |
| strict 696 Git identity | 上述 preserved file 的 Git blob 为 `e93cfd01d9c282f98881a6311b8bb806bbc3e359`，与 `git -C D:\mavenProject\DHXY rev-parse 696a12b0:src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java` 完全一致 |

strict 696 文件是业务顺序裁决依据；`963b028c...` 是 BP2 字节 diff 起点。两者职责不同，不能用 strict 696 文件冒充 BP2 的直接 byte baseline，也不能用 retained WIP 覆盖 strict 696 的业务裁决。

### 2.3 中途 WIP 排除规则

- 本 helper 取证期间，External C 已把目标文件从冻结的 `1224/963b028c...` 改成持续变化的 provisional source WIP；`ACTIVE_WORK.md` 已记录首窗 `1261/c37a0186...`，之后物理文件仍继续变化。
- 读取时 BP2 子卡只有 claim 正文，没有其后的规范 `TRUE_EOF` claim/delivery 终止标记，也没有 canonical `SOURCE DELIVERED` 段。
- 上述 WIP 的行数、SHA、类型或方法内容全部排除出本报告的通过/失败证据。父级不得拿某次中途快照与最终文件拼成 review，也不得把本 helper 已读源码误写成 source receipt。
- 父级只在 BP2 子卡 physical EOF 出现 canonical delivery、final SHA/line、changed method/type index 和 owner release 后开始 review；若 delivery 后又有 repair/新 delivery，则前一快照立即失效。

## 3. Exact write set 与 byte-diff 前置门

### 3.1 唯二允许写项

1. Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`。
2. Append-only `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP2.md`。

所有测试、BP1 两文件、holder/context/client/protocol/model/POM、AutoCombat/AutoBattle/Task caller、Dialog/SummonSkill/TeamReturn/CommonBox、DHXY Java、父卡、计划、`ACTIVE_WORK.md` 和 dashboard 均不在 BP2 implementation 写集。

### 3.2 canonical delivery 后先做的四项身份核验

1. 重读 BP2 子卡完整 physical EOF，确认最后一轮是 canonical source delivery 或 repair delivery，不是 claim、heartbeat、中途 source-start 或 owner-return 文本。
2. 复算最终 production SHA-256、line count、byte count、mtime，并与 delivery 段逐位比较。
3. 复算 BP1 两个只读 SHA，必须仍为 `a9c34d4e...` / `3b117895...`；任何漂移都先查 owner，不把它并入 BP2。
4. 记录 BP2 子卡自身最终 SHA，保证后续父级/双 reviewer 审的是同一张卡尾与同一 production SHA。

只读命令：

```powershell
$src = 'D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java'
$card = 'D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP2.md'
$bp1 = 'D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java'
$bp1Test = 'D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\runner\context\TaskExecutionContextTurnContractTest.java'

Get-Content -LiteralPath $card -Tail 120
Get-FileHash -Algorithm SHA256 -LiteralPath $src,$card,$bp1,$bp1Test
Get-Item -LiteralPath $src,$card,$bp1,$bp1Test | Select-Object FullName,Length,LastWriteTime
(Get-Content -LiteralPath $src).Count
```

### 3.3 exact diff 必须使用真实 `963b028c...` artifact

目标 production 在 Cloud 仓是 untracked 路径，`git diff HEAD` 不能重建 BP2 起始字节。父级必须取得一个**实际字节 SHA 为完整 `963b028c...`、行数为 1224** 的只读 base artifact，再执行 no-index diff；只引用卡片里的 SHA 文本、strict 696 文件或某次 WIP 快照都不等价。

```powershell
$base = '<父级持有的 byte-identical 963b028c... baseline path>'
$final = 'D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java'

(Get-FileHash -Algorithm SHA256 -LiteralPath $base).Hash.ToLower()
(Get-Content -LiteralPath $base).Count
git --no-pager diff --no-index --no-ext-diff --unified=80 -- $base $final
git --no-pager diff --no-index --no-ext-diff --check -- $base $final
```

`git diff --no-index` 对有差异文件可能返回非零；父级应保存并审阅 diff 内容，不把“有差异”的退出语义误报成构建失败。若父级没有可验证的 `963b028c...` base artifact，不能声称完成“exact diff”；按第 10 节 P1 evidence 条件处理并要求补齐可复现基线/patch provenance。

### 3.4 允许出现 delta 的 symbol index

未来 exact diff 的 production delta 应收敛在以下位置；任何其它方法体变化必须逐条解释并对照业务基线：

- imports：仅 typed-key 实现机械所需；不得引入 command/action/UUID/runtime/clock/scheduler/input collaborator。
- 四个共享 map 字段声明。
- formal team round/window 路径：
  `beginTeamMaintenanceRound`、`openTeamPathingMaintenanceWindow`、
  `openTeamFirstAidMaintenanceWindow`、`closeTeamMaintenanceWindow`、
  `isTeamPathingMaintenanceWindowOpen`、`isTeamFirstAidWindowOpen`。
- local-session path：
  `registerLocalTeamSessionCandidate`、`markLocalTeamWindowRoleDetected`、
  `isPendingLocalSupportLeaderDetection`、`markLocalTeamLeaderDetected`、
  `isLocalTeamSupportCapabilityOpen`、`hasDetectedLocalLeader`、
  `openLocalTeamSupportCapability`、`closeLocalTeamSupportCapabilities`、
  `completeLocalTeamSessionWindow`。
- claim path：
  `maybeCleanSummonSkill` 中只允许 key/value type replacement，
  `releaseSummonSkillRoundClaimIfOwned`、`resolveTeamRoundKey`、
  `resolveLocalSupportCapabilityRoundKey`、`pruneOlderTeamRoundClaims`，以及旧 string `teamRoundKey` 的删除/替换。
- context-to-key mechanics：既有 `effectiveContext` 的 supplied-first 语义，及一个清晰的 typed scope/window/session/team key construction boundary。
- 文件底部 private record/enum/sealed key types。

`runOpportunisticMaintenance`、`handleMaintenanceBroadcast`、Summon gate/cache/result methods、四个 per-window cache helpers 和 caller 文件原则上不需要业务 delta。若 diff 触及它们，父级必须证明只是类型流转且条件、顺序、次数和 side effect 字节级可解释；不能因“顺手整理”接受逻辑移动。

## 4. 四迁四留的字段合同

### 4.1 必须迁成 scoped typed key 的四个 map

| Field | 最终泛型 |
|---|---|
| `activeTeamRoundByKey` | `Map<ScopedTeamKey, Integer>` |
| `teamMaintenanceWindowStateByRound` | `Map<TeamRoundKey, TeamMaintenanceWindowState>` |
| `localTeamSessions` | `Map<ScopedLocalSessionKey, LocalTeamSessionState>` |
| `summonSkillClaimsByTeamRound` | `Map<MaintenanceClaimKey, Set<ScopedWindowKey>>` |

每个 map 的所有 `get/put/computeIfAbsent/remove/keySet` 路径都必须一次性迁完。不能只改字段泛型而留 raw lookup，也不能保留第二个 string compatibility map、typed-first/raw-fallback、dual read 或 dual write。

### 4.2 BP2 必须保持原型、留给 BP3 的四个 per-window map

```java
Map<String, Long> lastSummonSkillCleanAtByWindow
Map<String, Long> lastSummonSkillNotDueLogAtByWindow
Map<String, Long> summonSkillUnknownRetryAfterByWindow
Map<String, SummonSkillWindowState> summonSkillStateByWindow
```

同时保持 `currentWindowKey`、`scopePrefix`、`currentIdentityToken`、fingerprint/generation/cache invalidation 工作不变。最终文件中仍可能存在 BP3 所属的 `scopePrefix + "|" + windowId` 和 identity-token delimiter；它们不是 BP2 自动 finding。真正的门是：四个已迁 team/session/claim map 的 key construction、lookup、prune 和 owner set 中不得再使用 delimiter/prefix/alias。

`LocalTeamSessionState` 内部的 candidate/detected/completed raw window-id sets 可以保留；scope 隔离由 outer `ScopedLocalSessionKey` 提供，它们不是 tuple map key。

### 4.3 private typed-key 结构必须表达的维度

以下是 readiness 与 fixed card 共同冻结的等价结构。名称应优先保持卡载名称；实现即使采用等价 sealed subtype，也不能丢任何维度：

```java
private sealed interface ExecutionScopeKey
        permits ExactExecutionScopeKey, NoContextScopeKey {}

private record ExactExecutionScopeKey(
        String tenantId,
        String userId,
        String deviceId) implements ExecutionScopeKey {}

private enum NoContextScopeKey implements ExecutionScopeKey {
    INSTANCE
}

private record ScopedWindowKey(
        ExecutionScopeKey scope,
        String windowId) {}

private record ScopedLocalSessionKey(
        ExecutionScopeKey scope,
        String sessionKey) {}

private enum TeamCoordinationKind {
    WINDOW,
    LOCAL_SESSION
}

private record ScopedTeamKey(
        ExecutionScopeKey scope,
        TeamCoordinationKind coordinationKind,
        String coordinationKey,
        String maintenanceKey) {}

private sealed interface MaintenanceClaimKey
        permits TeamRoundKey, LocalCapabilityRoundKey {}

private record TeamRoundKey(
        ScopedTeamKey teamKey,
        int round) implements MaintenanceClaimKey {}

private record LocalCapabilityRoundKey(
        ScopedLocalSessionKey sessionKey,
        TeamSupportCapability capability,
        int epoch) implements MaintenanceClaimKey {}
```

验收重点不是 record 数量，而是：

1. exact scope 为 `(tenantId,userId,deviceId)`；no-context 是显式不同类型，不是可与真实 scope 混淆的 raw/null 文本。
2. claim owner 是 `(scope,windowId)` 的 `ScopedWindowKey`，不是 raw window id。
3. formal claim 必含 `ScopedTeamKey + round`。
4. local claim 必含 `ScopedLocalSessionKey + capability + epoch`。
5. formal/local claim 由 Java type 区分，永不靠 `local-team:` 或 `#` 前缀分流。
6. private helper types 位于文件底部，在主要 public/private workflow 之后；不把它们塞回主流程中间。

## 5. Context authority、共享与隔离合同

### 5.1 supplied context precedence

既有 `effectiveContext(context)` 合同必须保持：

```text
supplied context != null -> 只使用 supplied context
supplied context == null && holder nonempty -> 使用 holder context
supplied context == null && holder empty -> 显式 NoContextScopeKey
```

不得先读 holder 再让 holder 覆盖 supplied context；不得把 supplied context 与 holder 的字段混拼成一个 key。

### 5.2 authority failure 不降级

- valid turn-native 和 valid legacy context 都通过公开 `getTurnServiceScope()` 与 `getTurnInvocationContext()` 取得 tenant/user/device/window authority。
- 已有 context 若 scope/invocation/window authority 抛异常、为空或非法，必须沿既有 typed context/checkpoint 路径传播；不得 `catch (RuntimeException)` 后退到 no-context、bare task key、bare session key 或 global `default`。
- 只有 supplied-null 且 holder-empty 才能使用 no-context。已有 context 的 authority failure 与“根本没有 context”是两个不同事实。
- BP2 不读取 BP1 private latch，不复制 `latestExactTurnMetadata()`，不增加第二次 latest metadata observation。

### 5.3 exact isolation matrix

| 输入关系 | 必须结果 |
|---|---|
| tenant 不同，其余文本相同 | 隔离 |
| user 不同，其余文本相同 | 隔离 |
| device 不同，其余文本相同 | 隔离 |
| 同 scope、同显式 `localTeamSessionKey`、不同 window | local session/capability/对应 local claim 按基线共享 |
| 同 scope、无显式 session、不同 window | formal coordination 与 claim 隔离，不能自动并队 |
| 同 session 文本、不同 scope | 隔离 |
| identifier 内含 `|`、`#`、`local-team:` | 仍按 record 字段 equality 区分，不碰撞、不解析 |
| formal `TeamRoundKey` 与 local `LocalCapabilityRoundKey` | 类型上永不别名 |
| null supplied + 同一个 nonempty holder | 使用 holder scope/window/session；不落 no-context |
| null supplied + empty holder | 仅此路径落显式 no-context namespace |

### 5.4 maintenance key 与 coordination 规则

`maintenanceKey` fallback 顺序保持：

```text
explicit teamMaintenanceKey
-> requested task code
-> task code
-> existing default
```

有显式 local-team session 时，`ScopedTeamKey` 使用 `LOCAL_SESSION + sessionKey`；无显式 session 时使用 `WINDOW + exact windowId/default`。本卡只替换 key representation，不改变 fallback 条件、优先级或调用时机。

## 6. 19 public APIs、五 collaborator、六个 TURN-34A API 与四个 dormant API

### 6.1 19 个 public method signature 必须逐项不变

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

未来父级先执行 `rg -n '^    public ' $src`，结果必须恰好 19；然后按上表比较名称、visibility、返回类型、参数类型、顺序和 arity。只比 method name 或 count 不足以证明 byte-compatible surface。

### 6.2 Lombok constructor collaborator 面保持五项

```java
private final BotProperties botProperties;
private final GameContext gameContext;
private final DialogService dialogService;
private final SummonSkillService summonSkillService;
private final TaskExecutionContextHolder taskExecutionContextHolder;
```

不得加第六 collaborator、手写第二 constructor、service locator、static holder、clock/sleeper、gateway、UUID supplier、executor 或 scheduler。

### 6.3 TURN-34A 正在消费的六个 API

```text
isPendingLocalSupportLeaderDetection
isLocalSupportMemberSession
isLocalTeamSupportCapabilityOpen
awaitLocalTeamSupportCapabilityOpen
isLocalSupportMemberCandidate
awaitTeamFirstAidMaintenanceWindowOpen
```

Cloud `AutoCombatService.java` 当前生产调用点集中在约 `485-654` 行。BP2 不改 caller，也不能改变这六个 API 的 gate、wait、null、return 或 capability 语义。

### 6.4 不得被 BP2 激活的四个 local-session lifecycle API

```text
registerLocalTeamSessionCandidate
markLocalTeamWindowRoleDetected
markLocalTeamLeaderDetected
completeLocalTeamSessionWindow
```

目标类之外的 Cloud production caller 数必须保持零。`markLocalTeamWindowRoleDetected` 在目标类内部调用 `markLocalTeamLeaderDetected` 不算新 host/runtime activation；任何新 external caller、factory、host、runtime、startup hook 或 Task construction 都超出 BP2。

只读 caller 证据：

```powershell
rg -n --glob '*.java' --glob '!TaskMaintenanceService.java' `
  '\.(registerLocalTeamSessionCandidate|markLocalTeamWindowRoleDetected|markLocalTeamLeaderDetected|completeLocalTeamSessionWindow)\(' `
  'D:\mavenProject\dhxy-cloud-brain\src\main\java'
```

预期零命中。

## 7. strict 696a12b0 业务顺序护栏

### 7.1 顶层 maintenance 顺序

`runOpportunisticMaintenance` 必须保持：

```text
normalize request
-> first checkpoint
-> optional maintenance broadcast
-> handled / BROADCAST_FAILED / INTERRUPTED short-circuit
-> optional Summon maintenance
-> no-action
```

广播短路时 Summon delegate 为零；eligible Summon pass 最多一次 `cleanSummonSkillsOnce(...)`。不得把 checkpoint 移到 broadcast 之后，不得让 failed/interrupted broadcast 落入 Summon，不得增加 loop、retry 或 second delegate。

strict 696 证据：baseline `TaskMaintenanceService.java:579` 的 public entry、`:600` broadcast、`:625` Summon path。

### 7.2 CommonBox priority 与 TeamReturn capability-only

- `TaskMaintenanceService` 只维护 `COMMON_BOX`/`TEAM_RETURN` capability，不 import、构造或调用 `CommonBoxService`、`TeamReturnService`，不消费盒子，不执行归队 input。
- `AutoCombatService` 的 production 顺序保持 CommonBox 在 follower first-aid 之前：当前约 `155-168` 先调用 `runPendingMemberCommonBoxIfAllowed(...)`，再调用 `runPendingFollowerFirstAidIfAllowed(...)`。
- `docs/业务逻辑.md` 的 CommonBox pending 为当前窗口最高优先级，优先于 first-aid、三技能、香、医宝宝、修装备等维护。BP2 不能因 key migration 改写此 caller-visible gate。
- TeamReturn 只通过 `TEAM_RETURN + COMMON_BOX` capability 放权；BP2 不下发 TeamReturn action、command、UUID 或 queue mechanics。

### 7.3 Summon gate、static-tail 与 UNKNOWN

门序保持：

```text
feature enabled
-> interval enabled
-> optional FREE state
-> due interval
-> existing UNKNOWN retry-after interval
-> existing 2h tail-safe / skill-count cache
-> formal team round or local capability round
-> required local capability or formal pathing window
-> same-window duplicate / max-cleaner claim
-> second checkpoint
-> exactly one TURN-33 cleanSummonSkillsOnce(request)
```

必须同时冻结：

1. existing `SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS` 与 `SUMMON_SKILL_COUNT_CACHE_TTL_MS` 都是 2h 基线；BP2 不新增、缩短、延长或复制 TTL。
2. fresh tail-safe cache 仍只在 `lastEffectiveSlot != null`、`nextStartIndex != null` 且 `nextStartIndex > lastEffectiveSlot` 时跳过 delegate，并按既有方式更新 cooldown/log state。
3. effective status 仍为 `NORMAL_SKILL`、`KEEP_SKILL`、`EMPTY_SLOT`；`UNKNOWN` 不能当 safe tail。
4. UNKNOWN 仍由 message 含 `unknown` 或 observed status 含 `UNKNOWN` 判定，沿既有 retry-after 与 layout-cache invalidation；不得加第二 observation、额外 fail-closed、cleanup 或 retry。
5. TaskMaintenance 只消费 TURN-33 whole-pass result；不得复制 `if8`、reverse static-slot scan、五次删除预算、ultimate corner、post-generated-delete observation、PNG/OCR/click/action/UUID mechanics。

strict 696 锚点：cache constants `:44-45`，gate/claim `:625-744`，delegate/result `:746-795`，tail/UNKNOWN helpers `:875-936`。

### 7.4 claim acquire/release/retain 与 ActionState

- acquire：同 typed claim key 上先拒 same-window duplicate，再拒 `claims.size >= maxClaims`，随后只 add 一次 `ScopedWindowKey`。
- release：known failure 且 `hasSummonSkillStateChange(result)==false` 时，只释放当前 window 持有的 claim；set 为空才移除 map entry。
- retain：success、已 delete、ultimate clicked/succeeded、以及强 terminal/uncertain 沿既有异常路径保留 claim；不得把 terminal 包装成 ordinary failure 后释放并重试。
- `hasSummonSkillStateChange` 仍精确为 ultimate clicked、ultimate succeeded 或 `deletedCount > 0`。
- delegate 前将 `GameContext.ActionState` 保存并置 `INTERACTING`；`finally` 只在当前仍为 `INTERACTING` 时恢复 previous state。异常传播一次，不吞、不伪造 success。

strict 696 锚点：claim acquire `:722-742`，ActionState/delegate/finally `:744-785`，release decision `:787-795`，release helper `:939-959`。

### 7.5 capability `5/1/5/2` 精确矩阵

| API | 必须 open/close 的 capability |
|---|---|
| `openTeamPathingMaintenanceWindow` | **5**：`FIRST_AID`, `PATHING_WINDOW`, `COMMON_BOX`, `SUMMON_SKILL`, `LEFT_TOP_STATUS` |
| `openTeamFirstAidMaintenanceWindow` | **1**：`FIRST_AID` |
| `closeTeamMaintenanceWindow` | **5**：关闭上述 pathing 五项 |
| `openLocalTeamReturnSupportWindow` / `closeLocalTeamReturnSupportWindow` | **2**：`TEAM_RETURN`, `COMMON_BOX` |

不得因 typed-key migration 增删 capability、改变 open-before-notify/close 时机、改变 epoch increment 条件或 overlap 行为。

## 8. 零新增面

BP2 相对 `963b028c...` 的净新增必须为零：

| Surface | 允许净新增 |
|---|---:|
| latest window metadata read / observation | 0 |
| checkpoint | 0 |
| Dialog delegate | 0 |
| Summon delegate | 0 |
| `TurnGameClient.execute` / command | 0 |
| action / actionId / UUID supplier call | 0 |
| retry / replay / resend | 0 |
| `Thread.sleep` / new monitor wait / timer / scheduler | 0 |
| TTL / expiry policy | 0 |
| session authority / owner / lease / ledger / queue / durable workflow | 0 |
| host/factory/runtime/startup activation | 0 |
| CommonBox/TeamReturn physical mechanics | 0 |

typed scope construction允许读取 context 已持有的 `getTurnServiceScope()` / `getTurnInvocationContext()`；这不是新的 latest metadata slot observation。禁止的是调用 `latestWindowMetadata`、复制 BP1 exact-generation fence、发 command 或新增 checkpoint。

从 strict 696 到 retained start 均保持的 source-count tripwire如下；最终 count 应继续一致，count 相同仍需逐行审查：

```text
checkpoint(context)                         = 4
dialogService.handleDialog(                 = 1
summonSkillService.cleanSummonSkillsOnce(   = 1
System.currentTimeMillis(                   = 12
Thread.sleep(                               = 0
.wait(                                      = 2
UUID/randomUUID                             = 0
TurnGameClient/.execute(                    = 0
```

只读复算命令：

```powershell
$patterns = @(
  'checkpoint\(context\)',
  'dialogService\.handleDialog\(',
  'summonSkillService\.cleanSummonSkillsOnce\(',
  'System\.currentTimeMillis\(',
  'Thread\.sleep\(',
  '\.wait\(',
  'UUID|randomUUID',
  'TurnGameClient|\.execute\('
)
foreach ($pattern in $patterns) {
  $count = (rg -o $pattern $src | Measure-Object).Count
  '{0}|{1}' -f $pattern,$count
}
```

## 9. 父级逐文件 source-review 执行清单

### A. Delivery provenance

- [ ] BP2 卡最后一轮为规范 canonical delivery，且有 final SHA、line、changed method/type index、owner release、未运行门和 `无已批准业务差异；按 696a12b0 等价迁移`。
- [ ] 最终 production bytes 与 delivery SHA/line 完全一致。
- [ ] review 开始后没有新 write；若 SHA 再变，停止本轮并从新 canonical delivery 重来。
- [ ] exact write set 只有 production + append-only BP2 card；BP1、tests、callers 与其它文档保持只读。
- [ ] 有实际 `963b028c...` base artifact，完成 no-index exact diff；没有时不冒充 exact diff。

### B. Map/type coverage

- [ ] 四个共享 map 的 final generic 精确为第 4.1 节。
- [ ] 四个 per-window map 仍为第 4.2 节 String-key 原型，`currentWindowKey/scopePrefix/currentIdentityToken/cache` 未提前进入 BP3。
- [ ] 所有四 map access site 已迁移；没有 raw map、第二 map、dual read/write 或 compatibility fallback。
- [ ] formal `TeamRoundKey` prune 只删除同一个 `ScopedTeamKey` 且 round 小于 active round 的 entry；不碰 local capability claims。
- [ ] local claim 含 session/capability/epoch，owner set 含 scope/window。
- [ ] private key types 在文件底部，无 trivial wrapper ladder、same-scope helper nesting 或只包一层 record constructor 的链。

### C. Scope and fallback

- [ ] supplied context 恒优先，null supplied 才查 holder。
- [ ] 仅 null supplied + empty holder 使用显式 no-context。
- [ ] context authority failure 不被 broad catch 降级。
- [ ] tenant/user/device/session/window isolation 符合第 5.3 节。
- [ ] maintenance-key fallback 条件和顺序不变。
- [ ] migrated domains 无 `team + "#"`、`local-team:`、prefix parse、tuple delimiter concat。

### D. API/caller surface

- [ ] 19 public signatures 逐项一致，不只 count 一致。
- [ ] 五 constructor collaborators 精确一致，无新增 constructor/service dependency。
- [ ] 六个 TURN-34A API 的 signature 和 caller-visible semantics 不变；AutoCombat caller 文件零改动。
- [ ] 四个 dormant lifecycle API 无新 production caller/host/factory/runtime activation。

### E. Business equivalence

- [ ] normalize/checkpoint/broadcast short-circuit/Summon/no-action 顺序不变。
- [ ] CommonBox priority 未移动；TaskMaintenance 仍不消费 CommonBox。
- [ ] TeamReturn 仍 capability-only，无 action/input。
- [ ] Summon gate、2h static-tail/count cache、UNKNOWN、一次 delegate 全部不变。
- [ ] claim acquire/release/retain、terminal/uncertain、STOP 与 ActionState 恢复不变。
- [ ] capability `5/1/5/2` 精确不变。
- [ ] 没有已批准业务差异；任何条件、次数、fallback、timing/expiry、cleanup 或 input/verification order 变化都不能作为 migration 顺手接受。

### F. Zero-new and structure evidence

- [ ] 第 8 节 source-count tripwire一致，并逐行确认没有同数替换/移动。
- [ ] 无 `latestWindowMetadata`、第二 checkpoint、第二 delegate、command/action/UUID/retry/sleep/timer。
- [ ] 无新 TTL/session authority/owner/lease/ledger/queue/durable workflow。
- [ ] 无 CommonBox/TeamReturn Service import/call；只有既有 one Dialog + one Summon delegate。
- [ ] delivery method/type index 与 exact diff 全部 hunk 对得上，无漏报 diff。

## 10. grep/结构证据命令与结果解释

### 10.1 字段、API、collaborator 和 caller

```powershell
rg -n 'activeTeamRoundByKey|teamMaintenanceWindowStateByRound|localTeamSessions|summonSkillClaimsByTeamRound|lastSummonSkillCleanAtByWindow|lastSummonSkillNotDueLogAtByWindow|summonSkillUnknownRetryAfterByWindow|summonSkillStateByWindow' $src
rg -n '^    public ' $src
rg -n '^    private final (BotProperties|GameContext|DialogService|SummonSkillService|TaskExecutionContextHolder) ' $src
rg -n --glob '*.java' --glob '!TaskMaintenanceService.java' '\.(initializeForTaskStart|beginTeamMaintenanceRound|openTeamPathingMaintenanceWindow|openTeamFirstAidMaintenanceWindow|closeTeamMaintenanceWindow|openLocalTeamReturnSupportWindow|closeLocalTeamReturnSupportWindow|isTeamPathingMaintenanceWindowOpen|awaitTeamFirstAidMaintenanceWindowOpen|awaitLocalTeamSupportCapabilityOpen|isLocalSupportMemberSession|registerLocalTeamSessionCandidate|markLocalTeamWindowRoleDetected|isLocalSupportMemberCandidate|isPendingLocalSupportLeaderDetection|markLocalTeamLeaderDetected|isLocalTeamSupportCapabilityOpen|completeLocalTeamSessionWindow|runOpportunisticMaintenance)\(' 'D:\mavenProject\dhxy-cloud-brain\src\main\java'
```

父级需按 method 分组核 caller。四 dormant API 目标类外零命中；六 TURN-34A API 仍只由已有 AutoCombat/AutoBattle paths 消费。新 caller 即超出 BP2。

### 10.2 raw delimiter/prefix/dual-lookup 扫描

```powershell
rg -n '"local-team:"|\+\s*"#"|startsWith\(|substring\(|Integer\.parseInt\(|Map<String,\s*Integer>|Map<String,\s*TeamMaintenanceWindowState>|Map<String,\s*LocalTeamSessionState>|Map<String,\s*Set<String>>' $src
rg -n 'scopePrefix|currentWindowKey|currentIdentityToken|teamRoundKey|resolveTeamRoundKey|resolveLocalSupportCapabilityRoundKey|pruneOlderTeamRoundClaims' $src
```

第一条在四个 migrated domains 预期零命中。第二条用于人工分类：BP3 保留的 per-window `scopePrefix/currentIdentityToken` 可以存在；任何 migrated team/session/claim key 再借它们做 delimiter string、fallback alias 或 prefix parse 都不合格。

### 10.3 forbidden-new surface

```powershell
rg -n 'latestWindowMetadata|latestExactTurnMetadata|TurnWindowMetadata|TurnGameClient|randomUUID|UUID|Thread\.sleep|Timer|Scheduled|Executor|lease|ledger|durable|retry|replay|resend|CommonBoxService|TeamReturnService' $src
rg -n 'dialogService\.handleDialog|summonSkillService\.cleanSummonSkillsOnce|checkpoint\(context\)|\.wait\(' $src
```

不要把旧字段名里的 `UNKNOWN retry-after`、既有 2h `TTL`、现有 monitor `wait` 或 `LocalTeamSessionState` 词面误判成“新增”。判定必须基于 `963b028c... -> final` exact added/changed hunk；新增 policy/call 才是 finding。

### 10.4 private type placement

```powershell
Get-Content -LiteralPath $src -Tail 220
rg -n '^    private (sealed interface|record|enum|static final class|static class) ' $src
```

检查 key types 位于主 workflow 后；`SummonSkillWindowState`、`LocalTeamSessionState` 的既有内部状态不因迁移被重排或重写。

## 11. 未来 P0/P1/P2 分类条件

下表是父级未来 review 的分类准绳，不是本 helper 对当前 WIP 的 verdict。

| Severity | 触发条件 |
|---|---|
| **P0** | BP2 新增/重放物理或 business command/action/UUID；新增自动 retry/replay/resend；把 terminal/uncertain/STOP 伪造成 success/false 后继续；TaskMaintenance 开始消费 CommonBox/执行 TeamReturn input；或新增路径可在错误 window/context 下直接触发 delegate/物理动作。 |
| **P1** | canonical SHA/write set/exact diff provenance不成立；任一四迁 map 未完整 typed/scoped、任一四留 map 被提前迁移；tenant/user/device/session/window 串态；supplied context 被 holder 覆盖；authority failure broad downgrade；formal/local claim alias/dual lookup；19/5/6 API 面漂移；四 dormant API 被激活；business order、Summon gate/static-tail/UNKNOWN、claim、ActionState、`5/1/5/2` 改变；新增 checkpoint/latest observation/delegate/timer/TTL/session authority/owner/lease/ledger/queue；明显源码泛型/控制流不闭合。 |
| **P2** | 业务尚未证明改变，但 delivery evidence/index 漏项；private key types 位置不符；新增 trivial wrapper/helper nesting；JavaDoc/comment 对 scope/no-context/claim 语义错误；日志或命名使 formal/local key 无法审计；delimiter grep 命中仅属 BP3 保留路径但 delivery 未清楚分类。若进一步证明会串态或改变业务，升级 P1。 |

任何 P0/P1/P2 都必须记录 exact final SHA、文件/方法/行、证据、影响、修复方向和复验点。修复产生新 SHA 后，原 reviewer 结论不能沿用。

## 12. 后续独立 review、test 与 build 门

1. **父级逐文件 source review**：只对 canonical final SHA 执行本文清单；把 exact diff、19/5/6/4 surface、typed-key isolation 和 strict 696 对账结果写回 BP2 子卡。本文不替代该步骤。
2. **两名独立 reviewer**：必须在同一 final SHA 上分别完整 review，并在各自固定报告 physical EOF 明确结论、范围和依据。实现者、本文 helper 和父级自身都不替代两名 reviewer。
3. **repair 重置门**：任何 reviewer finding 导致 source 改动后，重新 canonical delivery、父级 source review 和两名 reviewer；不能只让发现问题的一个 reviewer看局部 patch。
4. **BP3 串行门**：BP2 与 BP3 都写 `TaskMaintenanceService.java`。BP3 只能从 BP2 canonical delivery + parent source receipt 的 final SHA 开始；不得从 `963b028c...` 旁路叠写，也不得与 BP2 review/repair 同时写。
5. **唯一 named test**：BP2 不写测试。BP3 后续统一由 `TaskMaintenanceTurnContractTest.java` 覆盖 scope/session/window/claim、19/6 API、priority/delegate、terminal 与 A->B->A lifetime。显式授权命令为：
   `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`，工作目录 `D:\mavenProject\dhxy-cloud-brain`。
6. **Cloud compile**：所有相关 Java writer 释放、source/test snapshot 稳定且 named-test owner 交付后，运行
   `mvn -q clean compile`。BP2 当前 source review 不得引用 stale target/classes 或先前 compile。
7. **package 边界**：`mvn -q clean package` 会运行现有 tests，仍需用户对该次 package/test run 的单独授权；不得以 `-DskipTests`、`maven.test.skip`、`enforcer.skip` 绕过。
8. **TURN-34B 最终门**：还须满足 TURN-22 final integration、TURN-34A 六 API compatibility、TURN-33 typed delegate、BP1 context gate、唯一 named test、两 independent reviews 与 applicable Cloud build。BP2 source receipt 不能单独关闭 TURN-34B。
9. **runtime 独立**：unit/compile 均不替代 TURN-41 user fresh runtime；本 preflight、BP2 source writer 和 source reviewers都不启动 application/server/Task/UI/capture/input。

## 13. 父级 review evidence index

| Evidence | 绝对路径 / 锚点 |
|---|---|
| Authority plan Sections 14-19 | `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-15-https-turn-complete-migration-card-plan.md`；尤其 17.2 `TURN-34A/B/C` write set、18 build cohort、19.4 `TaskMaintenanceTurnContractTest` |
| HTTPS turn no-retry/no-ledger boundary | `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-15-https-turn-thin-client-protocol-design.md` |
| Business contract | `D:\mavenProject\DHXY\docs\业务逻辑.md`；local-team session、CommonBox priority、Summon static-slot/UNKNOWN、五倍/修罗 696 baseline gate |
| TURN-34B parent freeze/review | `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34B.md` |
| BP1 exact-context predecessor | `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP1.md`；final production/test `a9c34d4e...` / `3b117895...` |
| BP2 fixed implementation contract | `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP2.md` |
| BP2 full readiness | `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-34bp2-readiness-preflight-helper.md` |
| BP2 post-BP1 delta | `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-34bp2-readiness-delta-helper.md` |
| BP2 target | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`；review only after canonical delivery |
| Strict 696 source | `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`；Git blob `e93cfd01...`, SHA-256 `4beaffd08...` |
| Context authority | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java`、`TaskExecutionContextHolder.java` |
| TURN-34A six-API callers / CommonBox priority | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java`；约 `155-168`、`485-654` |
| TURN-33 whole-pass boundary | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`；TaskMaintenance 只能调用 public `cleanSummonSkillsOnce(...)` once |

## 14. 交接摘要

- 起点固定为 `TaskMaintenanceService.java` 1224 行 / `963b028c...`；当前 provisional WIP 不作为任何 review evidence。
- BP2 只迁四个共享 team/session/claim map，保留四个 per-window map给 BP3。
- public/constructor/caller surface 固定为 `19 / 5 / 6`，四个 local-session lifecycle API 不得新增 production activation。
- key authority固定为 supplied-first、仅 null+empty holder no-context、tenant/user/device/session/window typed isolation、无 migrated-domain delimiter/prefix/dual lookup。
- strict 696 固定顶层顺序、CommonBox priority、TeamReturn capability-only、Summon gate/static-tail/UNKNOWN、claim acquire/release/retain、ActionState 与 capability `5/1/5/2`。
- BP2 净新增 metadata observation/checkpoint/delegate/command/action/UUID/retry/sleep/timer/TTL/session authority/owner/lease/ledger/queue/durable workflow 全为零。
- 父级收到 canonical delivery 后，先锁 final SHA 和真实 `963b028c...` exact diff，再执行本文 A-F 清单；之后仍需同 SHA 双独立 review、BP3 串行 handoff、唯一 named test 与 stable-writer Cloud compile。

TRUE_EOF PRECHECK_COMPLETE
