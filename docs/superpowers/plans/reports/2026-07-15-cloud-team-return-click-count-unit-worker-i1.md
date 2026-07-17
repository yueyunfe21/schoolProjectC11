# Cloud TeamReturn Click Count Unit Worker I1

## CLAIMED

- task: `W-COUNT-TEAM-RETURN-CLICK-WHOLE-1`
- role: Internal Count Worker I1, implementation only; not a reviewer
- claimedAt: `2026-07-15T01:03:47-04:00`
- countUnit: `TeamReturnService::clickReturnTeamIfPresent`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- baseline Service blob: `286c5a85f01d010e883f8c4321ea1793776c932f`
- branches: DHXY `thin-client-design` at `0114604e1ff5f15491d2910959c45252e893d04f`;
  Cloud `navigation-migration` at `3b988caa010254973e03342272e6d1d6a9685b01`
- gate: only parent source review plus the parent's unified fresh DHXY/Cloud build may apply the delta

## Result

`CLICK METHOD/PORT/MECHANICS SOURCE CHAIN PRESENT; COUNT BLOCKED BY FROZEN PRODUCTION BEAN REGISTRATION`。

I2 已落下的 TeamReturn Service/port/assembly 对本 count unit 的方法级源码顺序无需修正，因此 I1 没有制造
无意义 Java diff。全树生产 host 复核发现唯一 `CloudTeamReturnPortAssembly` 尚未进入
`CloudServiceHost -> CloudServiceConfiguration` bean 图；本单又明确冻结 host/config，故 I1 不越权接线，
也不把静态 call site 冒充可执行生产链。

## 696 Method And State Map

| `696a12b0` click step | Active Cloud evidence |
|---|---|
| first `findReturnTeamButton()` | `teamReturnPort.observeButton(context, "detect-return-button")`; only typed `OBSERVED/PRESENT` proceeds |
| no first button | throttled no-match diagnostics then `false`; no incense, click, or clicked timestamp |
| found timestamp | `lastReturnButtonFoundAtByWindow.put(windowKey(context), now)` immediately after first PRESENT |
| incense check | exactly one `playerStateService.ensureSheYaoXiangActive(context)` after found timestamp |
| second fresh button read | distinct `observeButton(..., "detect-return-button-refresh")`; first point is never reused |
| no refreshed button | warning then `false`; no click and no clicked timestamp |
| randomized point | X and Y independently use `ThreadLocalRandom.nextInt(-3, 4)`, preserving inclusive `[-3,+3]` |
| physical input | one port call with one ordered bundle: `CLICK_LEFT(screenX,screenY,150) -> SLEEP(500)` |
| clicked timestamp | written only after terminal `EXECUTED`; `NOT_EXECUTED` returns false and STOPPED/UNKNOWN cannot write it |

The baseline ordering is therefore unchanged:

`first observation -> found timestamp -> ensureSheYaoXiangActive -> second fresh observation -> independent X/Y +/-3 -> CLICK_LEFT(150) -> SLEEP(500) -> clicked timestamp`.

## Real Caller Evidence

1. `AutoBattleTask.maybeRunIdleMaintenance` at active Cloud line 194 calls
   `clickReturnTeamIfPresent(context, "auto-battle")` before lower-priority idle maintenance and returns when clicked.
2. `AutoBattleTask.tryRunLocalTeamReturnRelease` at active Cloud line 253 calls
   `clickReturnTeamIfPresent(context, "auto-battle:local-team-return-release")` after the existing CommonBox-first
   capability point and returns `consumedBox || clickedReturn`.
3. `AutoBattleTask` is frozen and unchanged by I1; current SHA-256 is
   `E13BFFF740570B9C7B833F7EDCE336BFFE39FB89E410B630FF2156B69410264A`.

## Typed Mechanics And Closed Terminal

- `CloudTeamReturnPortAssembly.observeButton` reads `WindowFactKind.TEAM_RETURN_BUTTON` from the caller's exact
  `TaskExecutionContext`. `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED` remain
  one-to-one closed observation states; only PRESENT carries screen-absolute X/Y and score.
- DHXY `LocalRemoteGameCommandHandler` handles `TEAM_RETURN_BUTTON` under the exact registration/binding context,
  calls the existing `TeamReturnButtonLocalObservationMechanics`, and converts the mechanics window-client point
  with that exact binding origin. Capture/template/mechanics failures are not disguised as ABSENT.
- `CloudTeamReturnPortAssembly.clickReturnButton` submits exactly one screen-absolute InputBundle containing
  `CLICK_LEFT(delay=150)` followed by `SLEEP(500)`. The existing DHXY generic typed InputBundle terminal remains
  the sole serialized physical-input path; I1 added no second queue or fallback.
- Observation `NOT_EXECUTED` stays false; STOPPED is rechecked by `TaskCheckpoint`; unresolved STOPPED/UNKNOWN is
  fatal. Click `EXECUTED` alone returns true and writes clicked timestamp; `NOT_EXECUTED` returns false;
  STOPPED/UNKNOWN is not folded into success.

## Frozen Blocked Cohorts

- `waitForMembersReturnIfNeeded`, leader signal live/precheck methods, `probeMemberReturnMarker`, their callers,
  120s/3s polling, and the baseline capture-before-async-analysis timing are frozen and remain blocked under I2
  Parent Source Review #1. I1 does not claim or repair those paths.
- `PlayerStateService`, `AutoBattleTask`, generic shared transport, host/config, other Services, and all DHXY Java
  are read-only for this task and were not edited.

## File Table

| Repository | File | I1 action | Current SHA-256 |
|---|---|---|---|
| Cloud | `src/main/java/com/bot/dhxy/service/TeamReturnService.java` | read-only; click method already source-equivalent | `32CE892ED267CDE4CA4E7F533B91E7380D6AAE7395F908E28F44A055C280B8A5` |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPort.java` | read-only; existing closed contract reused | `709914001AF7583EDA18687B4E8E9CEBD58DDF16C43FAB8056E0C272EF96FFCD` |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java` | read-only; existing typed assembly reused | `12974D2E2BDB90EC97F93BB32EECF08576A2003514734DD5A0EA75FE2E4A5FFC` |
| DHXY | `src/main/java/com/bot/dhxy/service/teamreturn/TeamReturnButtonLocalObservationMechanics.java` | read-only reuse | `85DDDE1B804FA17B9BCDD19C9D50F7A42F060D5CC9B561B04BB2B4A0AE9D1899` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | read-only existing TeamReturn/InputBundle branches | `B1CD28FA03F1F933E92BB037C09BA1E2922635149D32D4482637B1CD313BCDFC` |
| DHXY | `docs/superpowers/plans/reports/2026-07-15-cloud-team-return-click-count-unit-worker-i1.md` | new | this report |

## Exact Blocker

`CloudServiceHost.create` registers only `CloudServiceConfiguration`. That configuration scans only
`com.bot.dhxy.service` and currently imports `BotProperties` plus `CloudCommonBoxPortAssembly`; it does not import
`CloudTeamReturnPortAssembly`, which is under `com.yueyunfe.dhxy.cloudbrain.remote`. No second context,
`registerBean`, `@Import`, or explicit construction path exists in active Cloud source. Consequently the scanned
`TeamReturnService` cannot obtain its required `CloudTeamReturnPort` in the source-visible production bean graph.

Fixing this requires the explicitly frozen Cloud host/config registration surface. Moving the assembly package,
making the business Service import its transport implementation, or manually constructing the port would violate
the approved package/DI boundary; I1 did none of those. Parent must amend/own that exact registration before this
count unit can become build-eligible.

## Handoff Gate

- No Java file was edited by I1 because the allowed click implementation files already preserve the complete 696
  method order and the only remaining exact gap is outside the authorized write set.
- No Maven, javac, tests, runtime, application/server/host, Task/poller, UI/capture/input, or Git mutation was run.
- `countDelta=+1` is only claimed, not applied. Current ledger remains unchanged.
- Status: `BLOCKED P1=1 / COUNT NOT ELIGIBLE` pending parent scope decision and production bean registration;
  after repair it still requires parent source review and unified fresh DHXY compile + Cloud package.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Blocker Review #1 / Scope Amendment - 2026-07-15T01:10:30-04:00

父级独立确认 I1 的 P1 证据：`clickReturnTeamIfPresent`、两个真实 `AutoBattleTask` caller、typed
`TEAM_RETURN_BUTTON` observation、single InputBundle 和 closed terminal 均已闭合；唯一缺口是生产
`CloudServiceConfiguration` 未把扫描根外的唯一 `CloudTeamReturnPortAssembly` 注册进 bean 图。

结论：**BLOCKED，P0=0/P1=1/P2=0；允许同一 countUnit 原 Worker 精确返修。**

Scope amendment：任务改为 `W-COUNT-TEAM-RETURN-CLICK-WHOLE-1-R1`，countUnit/countDelta 不变；在原写集基础上
仅新增 Cloud `com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java`。把现有唯一
`CloudTeamReturnPortAssembly.class` 加入当前 `@Import`，不得新建第二 context、手工 `new`、搬包、wrapper、
owner/session/TTL/retry，也不得修改 TeamReturn 业务、shared transport 或 DHXY Java。完成后交 `Repair #1`；
不运行 Maven/test/runtime/Git，等待父级复审与统一 fresh build。

## Implementation Repair #1 - Production Bean Registration - 2026-07-15T01:12:02-04:00

- task: `W-COUNT-TEAM-RETURN-CLICK-WHOLE-1-R1`
- countUnit: `TeamReturnService::clickReturnTeamIfPresent`
- countDelta: `+1`（仅申报，I1 未应用）
- 精确修复：Cloud `CloudServiceConfiguration` 在当前唯一 `@Import` 中加入现有
  `CloudTeamReturnPortAssembly.class`。生产链现在按源码可达：
  `CloudServiceHost -> CloudServiceConfiguration -> TeamReturnService + CloudTeamReturnPortAssembly`。
- 没有扩大 component scan，没有新建 context、wrapper、port implementation、owner/session/TTL/retry，
  也没有手工 `new` 或搬包。既有 `BotProperties`、`CloudCommonBoxPortAssembly` 注册保持不变。
- Cloud 配置文件修复后 SHA-256：
  `9065E5BF989C17BBC149B59392B540E46BCEB04A4849D4199C86C66080CCF911`。
- `TeamReturnService`、`CloudTeamReturnPort`、`CloudTeamReturnPortAssembly` 均未修改，SHA-256 仍分别为
  `32CE892ED267CDE4CA4E7F533B91E7380D6AAE7395F908E28F44A055C280B8A5`、
  `709914001AF7583EDA18687B4E8E9CEBD58DDF16C43FAB8056E0C272EF96FFCD`、
  `12974D2E2BDB90EC97F93BB32EECF08576A2003514734DD5A0EA75FE2E4A5FFC`。
- TeamReturn 点击业务、两个 `AutoBattleTask` caller、typed `TEAM_RETURN_BUTTON` mechanics、单 InputBundle、
  closed terminal 及 696 顺序均未改；leader wait/precheck/marker 与 async timing 继续冻结并保持原 BLOCKED。
- 未运行 Maven、javac、test、runtime/application/server/host、Task/poller、UI/capture/input；未做 Git mutation。

Status: `REPAIR #1 DELIVERED / COUNT PENDING PARENT SOURCE REVIEW AND UNIFIED FRESH BUILD`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。父级源码审查与统一双构建通过前不真正计数。**

## Parent Source Review #2 / Next Count Task - 2026-07-15T01:13:00-04:00

父级独立复核配置、唯一 implementation 与完整 click chain：现有 `@Import` 只增加
`CloudTeamReturnPortAssembly.class`，没有扩大 scan、第二 context、手工构造或业务改动；`TeamReturnService`、
两个 caller、typed fact、single InputBundle、closed terminal 均保持。结论：
**P0=0/P1=0/P2=0，REPAIR SOURCE APPROVED / COUNT PENDING BUILD。**
`countUnit=TeamReturnService::clickReturnTeamIfPresent` 仅在统一 fresh Cloud package 通过当轮 `+1`；ledger 暂不动。

下一任务另记固定报告 `docs/superpowers/plans/reports/2026-07-15-cloud-common-box-member-detect-count-unit-worker-i1.md`：
`W-COUNT-COMMON-BOX-MEMBER-DETECT-1`，`countUnit=CommonBoxService::detectMemberBoxAfterCombatExit`，
`countDelta=+1`。一次闭合真实 `AutoCombatService:366 caller -> Cloud CommonBoxService member detection -> existing
typed COMMON_BOX observation -> DHXY exact-window template mechanics -> closed pending-state mutation/result`，保留 696
member role/source、capture/template terminal、成功才写 pending、TTL/state；不碰 leader-only caller。唯一 Java 写集：Cloud
`CommonBoxService.java` + CommonBox service-specific adapter（仅必要时）+ 新报告；caller、DHXY、generic shared 12、
其它 Service 冻结。现有真链完整可 NO_CODE_CHANGE 交证据，不得造重复协议。父级源码审查 + fresh build 通过同轮才 `+1`。
