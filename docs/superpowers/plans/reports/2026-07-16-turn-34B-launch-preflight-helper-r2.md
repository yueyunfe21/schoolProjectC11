# CR271 TURN-34B launch-preflight helper r2

- role: launch-preflight helper only; not reviewer or implementation owner
- helper: Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`
- scope: collect evidence and freeze a future External D launch brief for TURN-34B
- restrictions: no approval/blocking judgment, no Java/test/plan/ACTIVE_WORK edits, no Maven/tests/runtime/input/capture, no Git mutation
- state: `HELPER CLAIMED`

<!-- TRUE_EOF: TURN-34B LAUNCH-PREFLIGHT HELPER R2 CLAIMED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T04:56:11.489-04:00 -->

## Launch-preflight evidence snapshot - 2026-07-16T05:05:16.862-04:00

- 本节只收敛未来 External D 可直接执行的开工 brief，不是 `READY`、`APPROVED`、`BLOCKED` 或父级裁决。
- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第
  14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、TURN-22/TURN-33 当前固定报告、既有 TURN-34B
  readiness 报告、TURN-34A/TURN-34C 边界报告，以及实际相关 production source。
- 两仓 status 已只读核对。DHXY 为 `thin-client-design`，Cloud 为 `navigation-migration`；两仓已有大量
  dirty/untracked。本 helper 未回滚、覆盖、清理、删除、暂存、提交或执行其它 Git mutation。
- 当前目标 production `TaskMaintenanceService.java` 为 1130 行，SHA-256
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`；对应
  `migration-baseline/696a12b0` 文件为 1123 行，SHA-256
  `4BEAFFD08314F694B41A841DFF236C4CE00DC335CBE75DE74A9F667A53803EDA`。完整 diff 的业务主体仍是
  696 顺序，差异集中在 context ownership 适配。
- 本 helper 未运行 Maven、JUnit、compile/package、runtime/application/server/Task/UI/capture/input，也未执行
  任何真实 command。

## 1. Start dependency gate

权威计划冻结 TURN-34B 的启动依赖为 `S=21+22+23+26+33`。本轮只按父级 source/test-source gate 判断是否可
领取，不把后续 named-test/build cohort 冒充 source gate。

| Dependency | 当前证据 | 对 External D 的开工含义 |
|---|---|---|
| TURN-21 | 父级最新 source/test-source 复审 `P0/P1/P2=0/0/0`，build cohort pending | source start gate 已满足 |
| TURN-22 | External A Repair #1 已于 `04:56:07` 交付 typed 单 `CLICK_LEFT`，携带 `clickDelayMs=150`、`queueHoldMs=500`；最新 true EOF 仍是 `SOURCE+TEST DELIVERED`，尚无父级 Repair #1 复审 | source start gate **尚未满足**；34B 不得抢跑或复制 TeamReturn timing |
| TURN-23 | 父级最新 Repair 复审 `P0/P1/P2=0/0/0`，build cohort pending | source start gate 已满足 |
| TURN-26 | 父级最新 source/test-source 审查 `P0/P1/P2=0/0/0`，build cohort pending | source start gate 已满足 |
| TURN-33 | 父级 Review #1 为 `0/2/0`；P1-1 已部分落盘。父级于 `05:00:25` 决定 P1-2 使用“每次成功删除后 fresh 静态倒扫、整次 pass 共用最多 5 次删除预算”，但尚无按该决定完成的 Repair #1 delivery/source pass | source start gate **尚未满足**；34B 不得猜最终 Summon result/exception contract |

因此当前证据结论是：

```text
implementationDispatchNow = NO
unblockPredicate = TURN-22 Repair #1 parent source pass
                  AND TURN-33 Repair #1 parent source pass
                  AND parent freezes/creates the fixed TURN-34B card
```

依赖未开只表示 External D 暂不能领取 TURN-34B，不表示 External D lane 停止。父级写出固定卡的 `READY` true
EOF 后，External D 才能在该固定卡 true EOF 追加真实 `CLAIMED`，然后开始改源码。

## 2. Future exact write set and mutex

父级开卡时应冻结为唯一三项写集：

1. production：
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
2. named test：
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
3. 固定实施报告：
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34B.md`

本快照下第 2、3 项尚不存在；应由未来固定实施卡授权后创建，helper 不提前创建。除上述三项外全部只读，包括：

- `SummonSkillService`、`TeamReturnService`、`DialogService`、`CommonBoxService`、`PlayerStateService`；
- `TaskMaintenanceRequest/Result/Status`、`TeamSupportCapability`、`TeamMaintenanceWindowState`；
- `TaskExecutionContext`、turn protocol/client/action/result、POM/config/resources/templates；
- `AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2` 及所有其它 caller；
- DHXY 全仓，以及 TURN-21/22/23/26/33 的 production/test/report。

写集关系：

- TURN-34A 只写 `AutoCombatService.java` 和 `AutoCombatServiceTurnContractTest.java`；与 34B 文件互斥，可在各自
  依赖门满足后并行。
- TURN-34B 只拥有 maintenance coordinator/state 与一次 Summon public call projection；不得修改或测试
  AutoCombat 的 enter/exit/recovery/dynamic-delay 状态机。
- TURN-34C 只写 `task/AutoBattleTask.java` 和 `AutoBattleTaskTurnContractTest.java`，且依赖 34A+34B；它拥有
  startup、tick、TEAM_RETURN/CommonBox/left-top/maintenance 的任务级顺序。34B 不得提前实现该编排。
- TURN-22 与 TURN-33 的 production/test 写集均不与 34B 文件重合，但它们是严格语义前置；文件互斥不等于可以
  越过 source gate。

## 3. Public API and real caller boundary

`TaskMaintenanceService` 当前 19 个 public 方法必须保留名称、参数、返回值和 caller-visible 语义：

```text
initializeForTaskStart
beginTeamMaintenanceRound
openTeamPathingMaintenanceWindow
openTeamFirstAidMaintenanceWindow
closeTeamMaintenanceWindow
openLocalTeamReturnSupportWindow
closeLocalTeamReturnSupportWindow
isTeamPathingMaintenanceWindowOpen
awaitTeamFirstAidMaintenanceWindowOpen
awaitLocalTeamSupportCapabilityOpen
isLocalSupportMemberSession
registerLocalTeamSessionCandidate
markLocalTeamWindowRoleDetected
isLocalSupportMemberCandidate
isPendingLocalSupportLeaderDetection
markLocalTeamLeaderDetected
isLocalTeamSupportCapabilityOpen
completeLocalTeamSessionWindow
runOpportunisticMaintenance
```

真实 production caller 仍只来自 `AutoBattleTask`、`AutoCombatService`、`WubeiTask`、`XiuluoTaskV2`。其中
`registerLocalTeamSessionCandidate`、`markLocalTeamWindowRoleDetected`、`markLocalTeamLeaderDetected`、
`completeLocalTeamSessionWindow` 当前没有外部 production caller。TURN-34B 必须保留 public shape 和单 scope
基线状态机，但不得为这四个 API 制造 caller、host、runtime activation、session authority 或“可达性证明”；实际激活留
给后续 caller/activation 卡。

## 4. 696a12b0-equivalent business contract

### 4.1 Opportunistic maintenance priority

`TaskMaintenanceService.java:578-596` 的顺序逐值保持：

1. 先 `checkpoint(context)`；
2. request 允许时只调用一次 maintenance broadcast；
3. broadcast 为 handled、failed 或 interrupted 时立即返回，Summon 调用数必须为 0；
4. 只有 broadcast 为 no-action 且 request 要求 clean Summon 时才进入 Summon；
5. 两项都不执行时返回原 `NO_ACTION`，不得新增第三个 fallback 或 background queue。

`handleMaintenanceBroadcast:599-621` 只调用既有
`DialogService.handleDialog(DialogHandleRequest.handleMaintenanceBroadcastOption(...))`；source 与
`allowFullMaintenanceBroadcastFallback` 原样透传，`BUSINESS_OPTION_CLICKED/INTERRUPTED/FAILED/no-action` 映射不变。

### 4.2 Summon coordinator gate and result

`maybeCleanSummonSkill:624-796` 维持以下原顺序与数值：

1. feature enabled；
2. configured interval `> 0`；
3. request 要求时必须处于 `FREE`；
4. per-window due gate；
5. 既有 unknown-failure interval；
6. 既有 2h tail-safe/skill-count cache expiry 与 fresh shortcut；
7. team round、local capability 或 pathing-window gate；
8. same-window duplicate 与 max-claim gate；
9. action 前 checkpoint；
10. 保存原 action state，并设置 `INTERACTING`；
11. 构造既有 `SummonSkillCleanupRequest`，同步调用 **一次** 最终 TURN-33 public
    `cleanSummonSkillsOnce(request)`；
12. 按最终 typed result 更新既有 cache/cooldown/unknown interval；
13. 所有正常、failure、terminal/uncertain/STOP 退出恢复先前 action state；
14. known failed result 且没有 delete/ultimate state change 时释放 round claim；已有 delete/ultimate state change 时保留；
15. terminal/uncertain 的 claim/result 投影必须绑定 TURN-33 最终 source contract，不能 catch-all 后转 false、自动重调或伪
    success。

既有 2h cache、unknown-failure interval、ultimate cooldown、round/max claim、tail/start-slot 语义全部保留。这里的
“retry later/backoff”是 696 已有业务状态，不授权 transport auto retry、TTL 扩展或 durable workflow。

### 4.3 Team coordination exact

- pathing window 精确打开 `FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS`；
- first-aid weak window 只打开 `FIRST_AID`，不得顺带放行 Summon；
- close 精确关闭上述五项；
- return support window 精确打开/关闭 `TEAM_RETURN+COMMON_BOX`；
- capability epoch、leader conflict/absent、all-candidate completion、one-per-round claim 保持现有状态机；
- CommonBox pending 的任务级最高优先消费属于 34C/真实 caller；34B 只维护 capability，不在本卡消费 box 或新增点击。

固定业务结论：

```text
无已批准业务差异；按 696a12b0 与 docs/业务逻辑.md 已确认规则等价迁移。
```

## 5. Required in-file runnable seams

以下两项是当前 `TaskMaintenanceService` 在 turn-native/singleton Cloud 路径上的可运行性接缝。它们必须由父级在
TURN-34B 固定卡中逐字确认；实现只能留在本卡唯一 production 文件内，不能扩写 context/protocol/model。

### R1. turn-native player identity fence

当前精确链路：

- `TaskMaintenanceService.java:996-1026` 在取得 per-window Summon state 时调用
  `context.getPlayerIdentityEpoch()`；
- `TaskExecutionContext.java:214-216` 的该 API 只读 legacy delegate；
- turn-native context 会在 `TaskExecutionContext.java:442-451` 抛出 old-authority-unavailable；
- 因而真实 turn-native first-due path 会在调用 TURN-33 前失败。

未来 brief 应要求：

- legacy context 继续使用原 player identity epoch，零业务变化；
- turn-native 仅在 `TaskMaintenanceService` 内以 supplied explicit context 为准，绑定其现有
  `TurnGameClient + TurnInvocationContext`，读取 latest exact metadata；
- current metadata 必须与 initial device/window/HWND/process/title 一致，missing/mismatch/STOP 在 Dialog/Summon
  delegate 及任何 action/UUID 前 fail closed；
- turn-native identity key 使用 exact `windowTitle + processId + nativeHandle`，不修改
  `TaskExecutionContext`、`TurnWindowMetadata`、协议或 DTO；
- `CommonBoxService.java:446-499` 的 stored/current turn-native identity fence 是现成同类实现边界，可只读复用其
  规则，不创建 facade/wrapper 链。

### R2. singleton in-memory key namespace

当前 `TaskMaintenanceService.java:52-59` 的多个 `ConcurrentHashMap` 是 singleton bean state；
`currentWindowKey:986-993` 只用 `windowId`，`normalizeTeamKey:1069-1079` 只用 task key，
`teamRoundKey:1106-1107` 只拼 round。不同 tenant/user/device 复用相同 window/task/round 时会串 cooldown、claim、
window/capability state。

未来 brief 应要求：

- 对现有 context-bearing per-window key，使用既有 `tenantId|userId|deviceId|windowId` namespace；
- 对现有 context-bearing formal team key，使用同一 scope namespace 加原 task key/round；
- 这是给既有内存 map 加 Cloud scope 隔离，不创建 owner/session/ledger/TTL/compaction/durable workflow；
- legacy/null-context fallback 保持原 key 语义；explicit context 必须优先于 holder 中的错误 context；
- 四个无 context、零 production caller 的 local-session lifecycle API 不在 34B 改签名或伪造跨 scope activation。
  Named test 只能诚实锁其单 scope 基线状态机；跨 scope local-session 激活留给最终有 context 的 caller/activation 卡。

## 6. Upstream binding points

### 6.1 TURN-22 Repair #1 binding

父级 source pass 出现后，External D 开工前必须重新读取 TURN-22 最终报告和 production/test source，并在固定卡记录：

- upstream 仍以一个 typed `CLICK_LEFT` 携带 `clickDelayMs=150`、`queueHoldMs=500`，真实 DHXY mapper 形成同一
  ordered list/一次 queue submission；
- `TeamReturnService` 的 public behavior 与 terminal projection 未被 Repair 改签名；
- 34B **不直接调用或重建** TeamReturn action，不添加 task delay、WAIT、第二 click、UUID、frame 或 transport retry；
- 34B 只保持 `TEAM_RETURN+COMMON_BOX` capability/window coordination。其 named test 只断言 capability/status，不重复
  TURN-22 的 JSON/mapper/input-queue fixture。

若父级复审改变上述任一最终接口/语义，应先更新固定 TURN-34B 卡，External D 不得按本 preflight 猜测。

### 6.2 TURN-33 final API binding

父级 source pass 出现后，External D 开工前必须重新读取 TURN-33 最终四文件和 named test，并在固定卡记录：

- public boundary 仍为 `SummonSkillService.cleanSummonSkillsOnce(SummonSkillCleanupRequest)` 与最终
  `SummonSkillCleanupResult`/terminal exception contract；
- 父级 `05:00:25` 已冻结：每次成功删除后发起 fresh static observation，整次 public pass 共用最多 5 次删除预算；
  每轮只 hover 当轮唯一选中的 OCCUPIED，第 5 次后停止，第 6 次删除 command/UUID 为 0；
- 这些 fresh observations 是 TURN-33 内的 Cloud business continuation，不是 transport retry。34B 对一次 due
  maintenance 仍只调用 **一次** public Summon service，不在 coordinator 中复制五次 loop、计数、JSON、PNG、OCR 或
  UUID；
- TURN-33 负责所有 whole-pass 退出恰好一次 lightweight cleanup，并原样传播 fatal/uncertain/STOP；34B 不再次调用
  UICleaner 或吞异常；
- 34B 只按最终 result/exception 更新自身既有 cooldown/cache/claim 并恢复 action state。任何 final field/status 变化都
  必须先回写固定卡，不能由 External D 自行解释。

## 7. Named-test acceptance

唯一测试类：

```text
com.yueyunfe.dhxy.cloudbrain.service.TaskMaintenanceTurnContractTest
```

测试应直接实例化 production `TaskMaintenanceService`，使用 test-private scripted Dialog/Summon collaborators 和
deterministic context；不启动 Spring、host、HTTP、runtime、Task、UI 或真实 input/capture，不为测试新增 production
helper/DTO/facade。

### A. Priority and result projection

- broadcast handled/failed/interrupted 各自 short-circuit，Summon 调用数均为 0；
- broadcast no-action + `cleanSummonSkill=true` 时 Summon 恰好 1 次；false 时为 0；
- source/full-fallback/request hints 原样透传；
- stop checkpoint 位于 collaborator 前，Dialog/Summon 调用数均为 0；
- handled/failed/interrupted/no-action 与原 `TaskMaintenanceResult/Status` 逐值一致。

### B. Summon gate, cache and state

- disabled、interval disabled、non-FREE、not due、existing unknown interval、fresh tail-safe cache、无 round、capability
  closed、pathing closed、same-window duplicate、max claim：Summon 调用数均为 0；
- due + valid pathing/capability：只调用一次 final TURN-33 public service，TaskMaintenance 自身 action/UUID/frame=0；
- success 精确更新 skill-count/start-slot/tail/ultimate/cooldown 与原 success status；
- known failed/no state change 释放 claim；delete/ultimate state change 保留 claim；
- UNKNOWN/uncertain/throw/STOP 不伪 success、不刷新成功 cooldown，异常原样传播，previous action state 恢复；
- 2h cache、configured unknown interval、ultimate cooldown、max claim 值与顺序不漂移；零新 clock/transport retry。

### C. Turn-native identity and Cloud scope

- 用真实 `TaskExecutionContext.turnNative(...)` 跑 first-due path，证明不会调用 legacy-only identity epoch；
- latest metadata missing 或 device/window/HWND/process/title drift 时，在 Dialog/Summon 前终止，delegate/action/UUID=0；
- supplied explicit context 胜过 holder 中 wrong context；null/legacy fallback 保持基线；
- 两个 tenant/user/device scope 使用相同 windowId/task/round 时，per-window cooldown/cache、formal team round claim/window
  state 不串态；
- 不用零-caller local-session API 假造跨 scope production activation。

### D. Team coordination

- pathing 精确五项、first-aid 只一项、close 精确五项；
- return support 精确 `TEAM_RETURN+COMMON_BOX`；
- one-per-round max/duplicate/known-failure release、capability epoch、leader conflict/absent/all-candidate completion 保持现有
  状态机；
- public zero-caller lifecycle API 的 shape 保持，但测试不能声称 runtime 已激活。

### E. API/source boundary

- 锁住上述 19 个 public 方法签名；models 与四个 caller source byte-untouched；
- production reachable TaskMaintenance 对 old whole-pass/exclusive/fact/macro/input authority 为零引用；
- 不重复 TURN-22 的 click JSON/input-queue fixture、TURN-26 的 OCR/raw-PNG fixture、TURN-33 的 per-action/five-delete
  fixture，只断言一次 typed delegate 与 coordinator projection；
- 永久本地 Service 清单仍只有 `BagService/UICleanerService/GiveItemService/QuestManagerService`；
- 不用 private helper、DTO、facade、non-runnable caller 或重复父链凑计数/验收。

未来父级/授权 stable-writer gate 的点名命令是：

```text
mvn -q -Dtest=TaskMaintenanceTurnContractTest test
```

本 helper 未运行该命令。适用 Cloud compile/build 仍由父级在 Java writers 稳定后按当前 CR271 门执行。

## 8. Prohibitions for the future implementation card

- 不改 DHXY，不改 protocol/context/models/callers/dependencies/POM/config/resources/templates；
- 不新增 JSON action、UUID、command、frame、raw-PNG/OCR/template/input/capture authority；
- 不下沉 Cloud 业务/OCR 到本地，不扩大四个永久本地 Service；
- 不新增 owner、permit、session、ledger、TTL、compaction、durable workflow、background worker/queue、transport auto retry；
- 不新增业务 timer、verification read、park/yield、cleanup、fail-closed rule 或 fallback 顺序；R1 exact metadata fence 只
  替代 turn-native 不可调用的 legacy epoch，不授权额外 observation/action；
- 不激活 dormant `enqueueSummonSkillOnly`，不制造 local-team lifecycle caller，不添加 factory/host/runtime/Task start；
- 不 catch-all 后把 terminal/uncertain/STOP 映成 false，不把 upstream business continuation 当 transport retry；
- 不覆盖式重建当前 untracked Cloud source，不清理任何既有 dirty/untracked，不执行 Git mutation；
- Worker 只写 `SOURCE+TEST DELIVERED`，不得写 `APPROVED/CLOSED`，不得审核自己的交付。

## 9. External D direct-launch brief

父级在两个未满足依赖 gate 通过后，可把以下正文原样收敛进固定 TURN-34B 卡，再允许 External D 领取：

```text
TURN-34B implements only Cloud TaskMaintenanceService coordinator/state plus its named
TaskMaintenanceTurnContractTest, against baseline 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7.
Before CLAIMED, reread the final parent-source-passed TURN-22 Repair #1 and TURN-33 Repair #1 reports and
their actual production/test source. Preserve all 19 public TaskMaintenanceService APIs and all caller files.
Keep checkpoint -> broadcast -> short-circuit -> one Summon public call ordering; preserve existing cache,
unknown interval, cooldown, team round/capability, claim/release, action-state restore, and exact capability
sets. Bind turn-native identity to latest exact metadata within TaskMaintenanceService and namespace current
context-bearing singleton state by existing tenant/user/device/window scope, without changing legacy/null
fallback or activating zero-caller local-session APIs. Do not build TeamReturn/Summon JSON, PNG, OCR, input,
loops, UUIDs, retries, local business, runtime, or host authority. Add only the named production-exercising
contract test described in this card. No approved business difference; equivalent migration only.
```

## 10. Precheck result

- precheckResult: `PRECHECK_COMPLETE`
- implementationDispatchNow: `NO`
- remaining evidence gates: `TURN-22 Repair #1 parent source pass; TURN-33 completed Repair #1 delivery and parent source pass; parent fixed TURN-34B card/READY true EOF`
- External D status: `future implementation lane; no TURN-34B CLAIMED may be inferred from this helper report`
- authorityBoundary: `evidence and launch-brief input only; parent remains sole manager/final reviewer`
- filesWrittenByMaxwell: `only this append-only launch-preflight report`
- testsOrRuntimeExecuted: `none`

<!-- TRUE_EOF: TURN-34B LAUNCH-PREFLIGHT HELPER R2 PRECHECK_COMPLETE Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:05:16.862-04:00 -->

## PARENT REASSIGNED / PARTIAL PRESERVED - 2026-07-16T05:10:05.473-04:00

- 父级已立即停止并重新分配 TURN-34B launch-preflight helper r2；Maxwell 不再拥有或继续修改本报告。
- 上述已落盘 preflight 证据按 append-only 原样保留，仅供父级/后续 owner 复核；不构成 TURN-34B READY、批准或阻断。
- Maxwell 现返回原 TURN-28P，领取父级最新 `PARENT DELIVERY REVIEW #2 / REPAIR #1 REQUIRED`。

<!-- TRUE_EOF: TURN-34B LAUNCH-PREFLIGHT HELPER R2 PARENT_REASSIGNED PARTIAL_PRESERVED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:10:05.473-04:00 -->
