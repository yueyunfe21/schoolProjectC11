# CR271 TURN-34BP2 Repair #2 Independent Whole-Card Review R2

## Verdict

**APPROVED**

`P0/P1/P2 = 0/0/0`。未发现待返修项。本结论仅为 latest-SHA whole-card 独立源码审查通过；不替代另一名独立 reviewer、父级裁决或后续 stable-writer Cloud compile gate。

## Frozen Review Snapshot

- Reviewer: R2 Galileo, assignment `019f6c31-db0e-7c93-9509-cc538010f312`.
- Cloud production: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`，**1,400 行**，SHA-256 **`8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`**。
- 原卡：`docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34BP2.md`，审查时 **366 行**，SHA-256 `6f17dbc32313c42e04fb2328353568ab50e38be062dad1090bbc27e7edb9992d`。
- 原卡 canonical TRUE_EOF：`TURN-34BP2 PARENT-FRESH-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED R1-RAWLS-019f6c31-9411-74a1-b81b-911626bed1a6 R2-GALILEO-019f6c31-db0e-7c93-9509-cc538010f312 SHA=8d79d198 DUAL-REVIEW-IN-PROGRESS 2026-07-16T14:30:45-04:00`。
- 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；Cloud 保存副本 `TaskMaintenanceService.java` 为 1,123 行 / SHA-256 `4beaffd08314f694b41a841dff236c4ce00dc335cbe75de74a9f667a53803eda`。
- 已完整读取原卡至物理真尾、当前 1,400 行 production、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn protocol、`docs/业务逻辑.md` 及双仓状态。

## Exact Evidence

1. **单一 typed coordination discriminator 闭合。** `scopedTeamKey(...)` 在 `TaskMaintenanceService.java:1260-1265` 是 `ScopedTeamKey` 唯一构造入口；它固定组合 `executionScope(context)`、`teamCoordination(context)`、既有 `normalizeTeamKey(...)`。`teamCoordination(...)` 在 `:1273-1279` 仅二选一：显式 session 为 `SessionCoordination`，否则为 exact `WindowCoordination`，无双查、别名或兼容回退。

2. **active round/window state/formal claim/prune 使用同一地址。** `activeTeamRoundByKey` 与 `teamMaintenanceWindowStateByRound` 声明于 `:59-61`；begin/open/close/query 全部先取同一 `ScopedTeamKey`（`:98-102`, `:119-123`, `:152-156`, `:177-180`, `:230-236`, `:578-586`）。formal claim 在 `:1163-1171` 包装同一 `ScopedTeamKey -> TeamRoundKey -> FormalTeamRoundClaimKey`；prune 在 `:1300-1306` 以 `team().equals(teamKey)` 同时清理该 exact formal round 的 claim/window-state。claim acquire/release 在 `:713-768` 与 `:968-987` 保留原临界区、limit、retain/release 语义。

3. **四个冻结组合均由 record 类型等值保证。** 同 scope + 同显式 session 得到相同 `SessionCoordination` 并共享；同 scope + 无 session + 不同 `windowId` 得到不同 `WindowCoordination` 并隔离 round/window-state/formal slot；不同 tenant/user/device 因 `ExecutionScope` 首字段不同永不共享；`FormalTeamRoundClaimKey` 与 `LocalSessionCapabilityClaimKey` 为 sealed interface 下不同 record（`:1378-1390`），跨类型不相等、不可碰撞。

4. **四 typed shared maps 与四 BP3 maps 符合冻结面。** `:59-65` 恰为 `Map<ScopedTeamKey,Integer>`、`Map<TeamRoundKey,TeamMaintenanceWindowState>`、`Map<ScopedLocalSessionKey,LocalTeamSessionState>`、`Map<MaintenanceClaimKey,Set<ScopedWindowKey>>`。`:55-58` 四个 BP3 per-window map 仍为三项 `Map<String,Long>` 加一项 `Map<String,SummonSkillWindowState>`；本卡未把它们迁入新 typed coordination key。

5. **19 个 public API 保持。** 源码扫描恰得 19 个 public method declarations，名称和参数表与 `696a12b0` 一致：`initializeForTaskStart`、`beginTeamMaintenanceRound`、三项 team-window open/close、两项 local-support open/close、三项 await/query、六项 local-session candidate/role/capability、`completeLocalTeamSessionWindow`、`runOpportunisticMaintenance`。未新增 public API，五个构造 collaborator 保持。

6. **supplied-context precedence 与 fail-closed authority 正确。** `effectiveContext(...)` 在 `:1021-1026` 明确 supplied non-null 先于 holder。`executionScope(...)` 在 `:1239-1256` 仅 `effective == null` 返回一次 `ExecutionScope.NONE`；非空 context 缺 scope/invocation 直接抛 `IllegalStateException`，authority accessor 异常不被该路径 broad-catch。`normalizeTeamKey(...)` 的 explicit -> requestedTaskCode -> taskCode -> default 顺序在 `:1281-1292` 保持。

7. **业务与 terminal 面无扩张。** `runOpportunisticMaintenance(...)` 在 `:597-615` 仍为 normalize -> first checkpoint -> optional broadcast -> handled/failed/interrupted short-circuit -> at most one Summon delegate -> no-action。capability open/close 集仍为 `5/1/5/2`；CommonBox、TeamReturn capability-only、Summon gates/static-tail/UNKNOWN、`GameContext.ActionState` 与 claim acquire/release/retain 顺序未改变。与基线 diff 中的变化均属于已批准 context/typed-address 迁移，没有新增业务 decision、terminal projection 或成功伪造。

8. **禁止扩张项为零。** 全文件没有 `UUID`/`actionId`/新 command、Thread sleep、owner、lease、ledger、queue 或 durable workflow 实现；现存两个 cache TTL 常量及 UNKNOWN retry-backoff 是 `696a12b0` 已有业务字节，不是 BP2 新增。没有 delimiter parse、`team + "#"`、`local-team:`、prefix compatibility lookup；Repair #2 只增加 typed discriminator/type documentation，不新增 metadata read、checkpoint、delegate、timer、TTL、retry、session authority 或 resend/replay。

## Repository Protection And Gates

- DHXY status read at freeze: branch `thin-client-design`，存在大量既有 modified/deleted/untracked；目标报告写入前不存在。
- Cloud status read at freeze: branch `navigation-migration`，存在大量既有 modified/untracked；production SHA 保持 `8d79d198...`。
- 未编辑 Java、测试、计划、原卡、ACTIVE_WORK 或 dashboard；未扩合同；未执行 Git mutation。
- 未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input。Cloud compile 仍是后续 stable-writer gate，本 R2 不声称 build 通过。

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: CR271 TURN-34BP2 REPAIR-2 INDEPENDENT-WHOLE-CARD-REVIEW-R2 APPROVED P0-0-P1-0-P2-0 FROZEN-SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101 CARD-CANONICAL-TRUE-EOF=14:30:45-04:00 NO-PENDING-REPAIR NO-MAVEN-JUNIT-COMPILE-RUNTIME-INPUT-GIT-MUTATION 2026-07-16T14:34:22-04:00 -->
