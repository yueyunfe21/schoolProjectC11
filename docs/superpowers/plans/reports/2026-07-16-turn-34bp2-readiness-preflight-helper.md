# CR271 TURN-34BP2 readiness preflight helper

## 1. 角色与结论边界

- 角色：`CR271 Internal helper`，只为 External D 在 TURN-34BP1 之后的下一张 production implementation slice 做 readiness。
- 本报告不是 implementation delivery、review、批准、父级裁决或 owner claim，也不创建 TURN-34BP2 卡。
- 快照时间：`2026-07-16T10:23:42.9850344-04:00`。
- 本轮唯一写入是本报告。未修改 DHXY/Cloud Java、测试、CR 卡、`ACTIVE_WORK.md`、权威计划、协议、业务文档或其它文件。
- 未运行 Maven/JUnit/compile/package，未启动 runtime/application/server/Task/UI/capture/input，未执行 Git mutation。

本报告中的 `READY` 只表示下一张小卡可以由父级冻结，不代表 TURN-34B、TURN-34BP1 或未来 TURN-34BP2 已通过。

**无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。**

## 2. 完整读取范围与当前物理事实

本轮已完整读取并交叉核对：

- `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271 全段；
- 权威计划 Sections 14-19；
- HTTPS thin-client protocol design 与 protocol foundation；
- `docs/业务逻辑.md`；
- TURN-34B、TURN-34BP1、TURN-34BT1 卡及其当前 physical true EOF；
- TURN-34B retained-production、test-slice、minimal-source-slices、BP1 与后续 test tranche helper；
- 当前 Cloud `TaskExecutionContext.java`、`TaskMaintenanceService.java`；
- 现有 named tests：`TaskExecutionContextTurnContractTest`、`AutoCombatServiceTurnContractTest`、
  `SummonSkillTurnContractTest`、`TeamReturnTurnContractTest`、`CommonBoxTurnContractTest`、
  `PlayerStateTurnContractTest`。

当前物理状态：

| 项目 | 当前事实 |
|---|---|
| TURN-34BP1 true EOF | `STALE-D-REVOKED / ZERO-OWNER / EXTERNAL-D-REPLACEMENT-READY / CLAIM-REQUIRED` |
| `TaskExecutionContext.java` | 491 行，SHA-256 `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` |
| `TaskExecutionContextTurnContractTest.java` | 753 行，SHA-256 `D667D6958DBC38A6FCCF2BA5E562CECD4EF60629DF7A4CD55E347C9DBD9ED945` |
| retained `TaskMaintenanceService.java` | 1224 行，SHA-256 `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC` |
| `TaskMaintenanceTurnContractTest.java` | 不存在 |
| TURN-34BT1 | `READY / CLAIM REQUIRED / PRODUCTION PRESERVED`，当前卡面没有 delivery |

因此 BP2 现在只能做 readiness，不能先于 BP1 claim/交付，更不能把 retained WIP 误写成 source-passed。

## 3. BP1 之后仍冻结的缺口

| 缺口 | 当前归属 | BP2 是否处理 |
|---|---|---|
| latest title/HWND/process 与 initial exact binding 不一致时首 delegate 前停止 | TURN-34BP1 | 否，BP2 只消费 BP1 的稳定 checkpoint 结果 |
| 同一 `TaskExecutionContext` 内 A -> B -> A' value-equal 后不能恢复有效 | TURN-34BP1 sticky fence | 否，不重复实现 |
| formal team round/window 使用 raw task/round key，跨 tenant/user/device/window 串态 | TURN-34B P1-2 | 是 |
| local-team session/capability 使用 raw session key，等值 session 跨 scope 串态 | TURN-34B P1-2 | 是 |
| formal/local claim 外层 key 用 `#`/`local-team:` 拼接，claim value 用 `|` 拼接 | TURN-34B P1-2/P2 | 是，仅关闭 team/session/claim 域 |
| 四个 per-window Summon map 与 `currentIdentityToken` 仍用 delimiter string 和 broad fallback | TURN-34B P2 | 否，留给 BP3 |
| 多个合法新 context 的 A -> B -> A rebind 后旧 cooldown/round claim/capability 仍可能存活 | TURN-34B P1-2 | 否，留给 BP3 |
| 唯一 `TaskMaintenanceTurnContractTest` 缺席及完整 behavior matrix | TURN-34BT 串行测试链 | 否，本 slice 不写测试 |

这里有两种不同的 A-B-A，不能混淆：

1. BP1 处理同一个 context 在 latest metadata 上先漂移、后恢复为 value-equal 的 sticky invalidation。
2. 后续 BP3 处理同一 logical window 依次收到三个各自合法的新 context A、B、A 时，旧 maintenance state 不得复活。

BP2 只先建立准确、无碰撞的共享边界。若在同一小卡中再加入 fingerprint generation、全状态 purge 和
A-B-A monotonic invalidation，就会重新形成已经导致 External D 归还 owner 的大上下文单元。

## 4. 建议父级冻结的唯一下一片

建议子卡：`TURN-34BP2 - scoped team/session/claim typed-key foundation`。

### 4.1 本片准确关闭的范围

只在现有 `TaskMaintenanceService` 内把以下四组状态改为 typed scoped keys：

1. formal team active round；
2. formal team maintenance-window state；
3. local-team session/capability state；
4. formal team round claim 与 local capability epoch claim，包括 claim owner window。

本片必须同时处理 formal 与 local claim。两者当前共用
`summonSkillClaimsByTeamRound`，若再拆开，会引入临时双 map/adapter，或让同一 claim/release 分支连续重写两次，
不是真正更小的可落盘边界。

### 4.2 本片明确不关闭的范围

- 不迁移四个 per-window Summon cache/cooldown maps；
- 不修改 `currentWindowKey`、`scopePrefix`、`currentIdentityToken` 或 `summonSkillState`；
- 不增加 native fingerprint registry、generation counter、state purge 或 A-B-A cleanup；
- 不写唯一 named test；
- 不声明 TURN-34B source passed/card approved。

这使 BP2 成为单 production 文件、可静态 source-review、可真实 delivery 的小片；最终通过仍等待 BP3、唯一 named
test、独立 review、named test command 与 Cloud compile/build。

## 5. Exact start dependencies

External D 只有同时满足以下条件才可领取父级新建的 BP2 卡：

1. **BP1 真实交付。** TURN-34BP1 physical EOF 已有 `EXTERNAL-D REPLACEMENT CLAIMED`、真实 source/test increment、
   `SOURCE+TEST DELIVERED`、final SHAs 和 owner release；不能以当前 `REPLACEMENT READY` 代替交付。
2. **BP1 parent source receipt。** 父级已核对 BP1 的 initial/latest title/HWND/process fence、sticky A-B-A、
   zero command/UUID/retry 与 legacy API stability。完整两 reviewer、named test、compile 可留作最终 gate，
   但不能在 BP1 仍有 source/test-source blocker 时开始 BP2。
3. **retained production 字节未漂移。** BP2 claim 前
   `TaskMaintenanceService.java` 必须仍为 1224 行、SHA-256
   `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC`；否则停止并重做 preflight。
4. **唯一 production owner。** `TaskMaintenanceService.java` 没有其它 active writer；原 TURN-34B owner 已释放。
5. **测试写手协调。** `TaskMaintenanceTurnContractTest.java` 当前不存在。父级应先串行完成 BP2/BP3 再冻结针对
   post-repair source 的测试；若 TURN-34BT1 在 BP2 claim 前已被领取，必须先记录其 target production SHA 和
   owner handoff，禁止用旧断言偷偷适配新 source。
6. **卡片先冻结后 claim。** 父级先创建 BP2 fixed card，记录本报告的 exact write set、initial SHA、禁止项和
   source-only delivery 口径；External D 只能在该卡 physical EOF 领取，不能在 TURN-34B 原卡或 helper 报告上自领。
7. **最终依赖不冒充 start gate。** TURN-22 Repair #3、TURN-34A 六 API compatibility、TURN-33 typed delegate、
   两独立 reviewer、唯一 named test 与 Cloud compile/build 仍是 TURN-34B 最终门，不要求 External D 在 BP2
   source-start 前运行它们。

## 6. Exact implementation write set

父级应冻结 External D 的写集为恰好两项：

1. 修改 Cloud
   `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`。
2. append-only 更新父级预先创建的
   `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP2.md`
   的 claim、heartbeat 与 source delivery 段。

以下全部只读：

- `TaskExecutionContext.java` 与 `TaskExecutionContextTurnContractTest.java`；
- 尚不存在的 `TaskMaintenanceTurnContractTest.java`；
- `AutoCombatService`、`AutoBattleTask`、Wubei/Xiuluo Tasks 及所有 caller tests；
- `SummonSkillService`、Dialog/CommonBox/TeamReturn/PlayerState 与现有 named tests；
- protocol/client/result/model/POM/config/resources；
- DHXY 其它 Markdown、dashboard 与全部 Java。

BP2 implementation owner 不得创建第二个 source/test/helper 文件。

## 7. Exact production contract

### 7.1 Typed key model

私有类型放在 `TaskMaintenanceService` 底部、主 workflow 之后。推荐冻结以下等价模型；命名可由父级保持原样：

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

必须保持以下含义：

- valid turn-native 与 valid legacy context 都直接读取现有 `getTurnServiceScope()` 和
  `getTurnInvocationContext()`，形成 `(tenant,user,device)`；这两个 API 当前双路径都可用。
- supplied context 继续通过现有 `effectiveContext(context)` 胜过 holder。
- 只有 null supplied + empty holder 使用 `NoContextScopeKey.INSTANCE`；不得 catch broad
  `RuntimeException` 后把一个已有 context 降级到无 scope。
- `ScopedWindowKey` 再加 exact `windowId`。
- `ScopedLocalSessionKey` 不带 window，才能让同一 tenant/user/device 下、同一显式 UI
  `localTeamSessionKey` 的 leader/member 窗口共享；相同 session 文本在不同 scope 下必须隔离。
- `ScopedTeamKey` 的 coordination 规则固定：有显式 local-team session 时用
  `LOCAL_SESSION + sessionKey`；否则用 `WINDOW + windowId/default`。没有显式 session 的不同窗口不能自动并队。
- `maintenanceKey` 的现有 fallback 顺序保持不变：explicit key -> requested task code -> task code -> `default`。

### 7.2 Four map conversions

只转换以下四个字段：

```text
Map<ScopedTeamKey, Integer> activeTeamRoundByKey
Map<TeamRoundKey, TeamMaintenanceWindowState> teamMaintenanceWindowStateByRound
Map<ScopedLocalSessionKey, LocalTeamSessionState> localTeamSessions
Map<MaintenanceClaimKey, Set<ScopedWindowKey>> summonSkillClaimsByTeamRound
```

以下四个字段本片保持原型，交给 BP3：

```text
Map<String, Long> lastSummonSkillCleanAtByWindow
Map<String, Long> lastSummonSkillNotDueLogAtByWindow
Map<String, Long> summonSkillUnknownRetryAfterByWindow
Map<String, SummonSkillWindowState> summonSkillStateByWindow
```

`LocalTeamSessionState` 内部的 candidate/detected/completed raw window-id sets 可保留，因为 outer
`ScopedLocalSessionKey` 已隔离 scope，且内部值不是 delimiter tuple key。

### 7.3 Formal team path

以下 public/private path 必须统一消费 `ScopedTeamKey`/`TeamRoundKey`，不得保留旁路 raw key：

- `beginTeamMaintenanceRound`；
- `openTeamPathingMaintenanceWindow`；
- `openTeamFirstAidMaintenanceWindow`；
- `closeTeamMaintenanceWindow`；
- `isTeamPathingMaintenanceWindowOpen`；
- `isTeamFirstAidWindowOpen` 及 await query；
- `resolveTeamRoundKey` formal branch；
- `pruneOlderTeamRoundClaims`。

删除 `teamKey + "#" + round`、prefix/substring/`Integer.parseInt` pruning。`pruneOlderTeamRoundClaims` 只删除
同一个 `ScopedTeamKey` 且 round 小于 active round 的 `TeamRoundKey`；不得碰 local capability claims。

### 7.4 Local-session path

所有 `localTeamSessions` 访问必须统一使用 `ScopedLocalSessionKey`：

- open/close/query/await capability；
- pending leader、detected leader、member candidate 相关查询；
- role detected、leader detected；
- local capability epoch claim resolution；
- register/complete lifecycle pair。

两个无 context public API `registerLocalTeamSessionCandidate` 与 `completeLocalTeamSessionWindow` 当前均为零
production caller：

- 不改签名、不激活 host/factory/runtime caller；
- 有 holder 时只使用现有 holder scope；empty holder 时两者落入同一个 typed no-context namespace；
- exact context path 不得为了兼容而 dual-read/no-context fallback，否则相同 session 文本仍会跨 scope 串态；
- 日后真实激活这两个 API 若需要显式 scope，必须另开 CR，不能在 BP2 偷加 public 参数或全局 registry。

### 7.5 Claim path

- formal round claim 使用 `TeamRoundKey`；
- local capability claim 使用 `LocalCapabilityRoundKey`，保留 capability + existing epoch；
- claim owner set 使用 `ScopedWindowKey`；
- acquire、same-window duplicate、max-cleaner、known-failure release、state-change retain 的条件和顺序逐字保持；
- 删除 `local-team:`、`#` 与 `|` 拼接在这两个 claim 域中的身份作用；
- 不增加 claim TTL、owner lease、reference count、cleanup pass 或后台 compaction。

### 7.6 No business-flow movement

下列行为必须与 `696a12b0` 和 retained WIP 保持不变：

1. `checkpoint -> broadcast -> handled/failed/interrupted short-circuit -> optional one Summon -> no-action`；
2. Summon feature/interval/FREE/due/unknown interval/2h cache/team/capability/pathing/duplicate/max/checkpoint 门序；
3. 一次 eligible maintenance 最多一次 TURN-33 typed delegate；
4. success/known failure/state-change/terminal/uncertain/STOP 的现有 result、claim 与 ActionState 投影；
5. capability 精确 `5/1/5/2` 集合及既有 overlap 行为；
6. CommonBox priority、TeamReturn capability-only 边界、Summon static-tail ownership；
7. 19 个 public API shape 与 TURN-34A 六 API caller-visible semantics。

## 8. BP2 source-delivery acceptance

External D 的 `SOURCE DELIVERED` 至少要给出以下可静态复核证据：

1. 起始 production SHA 与 `963B028C...` 一致，最终只改一份 production Java 和 append-only child card。
2. 四个目标 map 的 key/value 泛型与第 7.2 节一致；四个 per-window maps 未迁移。
3. formal/local claim 域不存在 `team + "#"`、`local-team:`、prefix parse 或 scoped tuple delimiter concat。
4. 不同 tenant、user、device 的同文本 team/session/round 由 record equality 隔离。
5. 同 scope、同显式 local-team session 的不同窗口共享；无 session 的不同窗口隔离。
6. supplied context 优先；null + holder 与 null + empty holder 走冻结 fallback；已有 context authority failure
   不被 broad catch 降级。
7. formal/local claim acquire/release/retain 分支仍各只有一个真实 map 决策，没有 dual map、dual read 或兼容旁路。
8. 19 个 public signatures 完全不变，尤其 TURN-34A 六 API；四个零 production caller API 没有新增 caller。
9. `runOpportunisticMaintenance`、broadcast/Summon gate order、delegate count、5/1/5/2 capability sets 与
   `696a12b0` 无业务差异。
10. 没有额外 metadata read、checkpoint、command、actionId/UUID、delegate、retry、sleep、timer、TTL、session
    authority、lease、ledger、queue 或 durable workflow。
11. 私有 key types 位于文件底部；没有 `prepare/handle/resolve` wrapper ladder，也没有保留只包一层 record
    constructor 的 `teamRoundKey(...)` 一行 helper。
12. child card delivery 记录 final SHA、行数、修改方法索引、未运行门、明确剩余 BP3/唯一 named-test debt；
    不写 `APPROVED/CLOSED`。

本 source-only slice 可以得到 parent source receipt，但不能独立得到 TURN-34B card approval。

## 9. 后续唯一 named-test acceptance

以下不是 BP2 写集，但父级必须把它们追加到唯一
`TaskMaintenanceTurnContractTest.java` 的后续串行 tranche，不能用 source scan/private map reflection 代替：

1. same window/team/round/session 文本下分别改变 tenant、user、device，formal/local state 和 claim 互不串态；
2. same scope、different window、无 session 时互不串态；
3. same scope、same explicit local session、different window 时 leader/member capability 与 round claim 按基线共享；
4. same session 文本、different scope 时隔离；
5. identifier 中含 `|`、`#`、`local-team:` 的不同 tuple 不碰撞；
6. formal `TeamRoundKey` 与 local `LocalCapabilityRoundKey` 永不别名；
7. same-window duplicate、max cleaner、known-failure release、state-change retain 保持原结果；
8. supplied context 胜过冲突 holder，null/holder/no-context fallback 保持冻结行为；
9. 19 API 与 TURN-34A 六 API、5/1/5/2、priority/delegate/no-retry 合同保持；
10. BP3 完成后再追加合法 successive-context A -> B -> A 的全状态 invalidation，不能在 BP2 测试中假称已闭合。

后续 stable-writer 命令仍是 `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`，并完成适用 Cloud
compile/build。本 helper 与 BP2 source writer 均不得把未运行命令写成通过。

## 10. BP2 禁止项

- 禁止修改 BP1 两个 Java 文件，禁止在 TaskMaintenance 内复制 latest metadata fence。
- 禁止创建/修改 `TaskMaintenanceTurnContractTest.java` 或任何第二测试类。
- 禁止修改 AutoCombat/AutoBattle/Wubei/Xiuluo caller、TURN-33/22/21 service/test、protocol/client/result/model/POM。
- 禁止改 19 public method 的名称、参数、返回类型或可见性；禁止激活四个零 production caller lifecycle API。
- 禁止在 BP2 修改四个 per-window Summon maps、`currentWindowKey`、`scopePrefix`、identity token 或 cache purge。
- 禁止在 BP2声称或实现 cross-context A-B-A full invalidation、generation registry、fingerprint registry 或
  terminal cleanup；它们属于 BP3。
- 禁止 raw task/session global key、delimiter tuple key、prefix parsing、exact+fallback dual lookup 或 broad
  exception-to-unscoped downgrade。
- 禁止新增 owner/session/lease/ledger/TTL/compaction/durable workflow、自动 retry、第二 observation、第二
  delegate、额外 checkpoint、fail-closed business rule 或 cleanup policy。
- 禁止改变 CommonBox priority、Summon gate/fallback/expiry、team capability overlap、STOP/pause 语义。
- 禁止 production test hook、private reflection seam、source guard/scan、Clock/sleeper seam、Mockito/Spring/HTTP fixture。
- 禁止 Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input 与 Git mutation，直到父级
  另行安排 stable-writer gate。
- 禁止实现者自批、代替 reviewer、写 P0/P1/P2=0、`APPROVED`、`DONE` 或关闭父卡。

## 11. 串行后续顺序

建议父级保持以下顺序：

1. `TURN-34BP1`：shared exact native-metadata checkpoint + sticky within-context fence。
2. `TURN-34BP2`：本报告冻结的 scoped team/session/claim typed-key foundation。
3. `TURN-34BP3`：复用 `ScopedWindowKey`，迁移四个 per-window maps，加入 exact native fingerprint 与
   legitimate successive-context A -> B -> A monotonic invalidation，并一次清理该 logical window 的
   cooldown/cache/formal claim/local participation state。BP3 必须单独 preflight，不能由本报告自动授权。
4. `TURN-34BT1+`：唯一 named test 的 exact scope、A-B-A、priority/Summon/team capability 串行 tranches。
5. Parent source/test-source review、TURN-22 final gate、TURN-34A compatibility、两独立 reviewer、named test、
   Cloud compile/build，最后才可能裁定 TURN-34B。

BP2 的最小性来自“只修共享 key ownership，不动 state lifetime”；BP3 再集中处理“同一 logical window 的
generation lifetime”。两片都只写同一 production 文件，但通过 physical EOF owner handoff 严格串行，既不引入
临时双实现，也不让 External D 再背一张 source+full-test 大卡。

TRUE_EOF PRECHECK_COMPLETE
