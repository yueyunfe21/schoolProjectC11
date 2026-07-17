# CR271 TURN-34C - AutoBattleTask Whole-Card HTTPS Turn Cutover

## PARENT FROZEN CARD / SOURCE-START READY - 2026-07-16T21:50:16-04:00

- Status: `WHOLE-CARD SOURCE-START READY / ZERO OWNER`
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Parent role: final reviewer and plan-contract owner only; no dispatch and no additional reviewer.

### Source gates

`TURN-19`、`TURN-21`、`TURN-22`、`TURN-23`、`TURN-34A`、`TURN-34B` 均已有父级
`SOURCE+TEST SOURCE REVIEW PASSED` 或等价 source closure。Named tests/build 属交付后门，不反向阻塞本卡
source-start。

### Complete write set

1. Cloud `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
2. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/AutoBattleTaskTurnContractTest.java`
3. 本固定报告 append-only physical EOF

其余两仓 production/test/POM/config/resource 全部只读。不得修改 `AutoCombatService`、
`TaskMaintenanceService`、PlayerState/TeamReturn/CommonBox/LeftTop/startup、`BaseTaskTemplate`、context/holder/
checkpoint、protocol/client/action factory、host/factory/runtime。Cloud runtime activation 归 TURN-40B。

### Start snapshot

- `AutoBattleTask.java`: 294 lines, SHA-256
  `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a`
- `AutoBattleTaskTurnContractTest.java`: `ABSENT`

领取前必须重新复算上述状态；发现漂移或 owner 冲突，停止并在本卡 canonical 返回，不覆盖共享字节。

### Frozen 696 behavior

- public `execute(context)` 必须绑定同一 exact context 完整 lifecycle，并在 normal/terminal 两路恢复 prior sentinel；
  null/missing context 在任何 collaborator/action 前 fail-closed。
- startup 顺序固定：startup gate -> RUNNING -> first-aid -> maintenance init -> AutoCombat init ->
  checkpoint/tick。不得新增 probe、retry、TTL、park/yield、cleanup 或第二 observation。
- combat 非 `NONE` 时不跑 idle maintenance；`NONE + FREE` 只跑一次 baseline idle chain。
- local team return 保持 CommonBox gate/consume 在前，但 consume 不短路随后 TeamReturn call；closed capability 使用
  timeout `0L`，零 probe/wait/retry。
- pending leader 必须阻断剩余 standalone return、left-top、opportunistic maintenance；standalone/local follower/
  legacy follower 分支保持基线真值表。
- maintenance request 的 source/broadcast/fullFallback/cleanSummon/onePerRound/teamKey/openWindow/
  requiredCapability 全字段保持当前已通过 collaborator contract。
- polling 优先级固定 pending `500ms` -> free `3000ms` -> dynamic；不求和、不 backoff。
- confirmed stop/failed/uncertain 原样 terminal，不转 false/成功，不 compensation、不重试；本 Task 不创建
  action UUID，不拥有 capture/input/OCR/activation authority。

### Unique named-test contract

唯一 `AutoBattleTaskTurnContractTest` 必须从 production public `execute(...)`/`stop()` 进入，覆盖：missing
context、exact reference + sentinel restore、terminal restore、startup skip 零副作用、startup exact order、combat
跳过 idle、FREE idle chain、local return CommonBox/TeamReturn 顺序、closed capability、pending leader、三种
session/follower 分支、三种 maintenance request、polling priority、stop/terminal no retry、零 direct action/UUID/
activation authority。允许同测试文件 scripted ports 和 deterministic protected sleep seam；禁止 private reflection、
source-only 断言冒充行为、复制 lower JSON/PNG mapper或启动 Spring/host/runtime/input/capture。

### Claim and delivery

External implementation Worker 自行领取时，必须在本文件 physical EOF 追加 canonical：

`EXTERNAL-<lane> TURN-34C WHOLE-CARD CLAIMED <timestamp>`

并写明两目标 start SHA/ABSENT、完整写集、无 owner 冲突及 `TRUE_EOF`。完成 production/test/report 全卡后一次
追加 canonical `WHOLE-CARD SOURCE+TEST DELIVERED`；中途 WIP 不审、不拆卡。遇到合同缺口或容量问题必须整卡
`OWNER RETURNED`，保留并逐 SHA 申报 WIP。

**无已批准业务差异；按 `docs/业务逻辑.md` 与 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34C PARENT-FROZEN-WHOLE-CARD SOURCE-START-READY ZERO-OWNER SELF-CLAIM AutoBattleTask=e13bfff7-294L named-test=ABSENT NO-DISPATCH NO-REVIEWER 2026-07-16T21:50:16-04:00 -->

## EXTERNAL-d TURN-34C WHOLE-CARD CLAIMED - 2026-07-16T21:56:10-04:00

EXTERNAL-d TURN-34C WHOLE-CARD CLAIMED 2026-07-16T21:56:10-04:00

- 领取时间：`2026-07-16T21:56:10-04:00`。Worker：CR271 External implementation Worker d（本会话；34B 事件已由父级 19:00/Review #4 完全收口，D lane 自此无卡、无遗留义务，当前空闲合规、无双卡并持）。implementation only，非 reviewer；用户已取消额外 reviewer，交付后仅由 CR271 父级本人复审。本段不含 `APPROVED/CLOSED`，不自批。
- claim 前已完整读取：本卡 21:50:16 PARENT FROZEN CARD 全文（source gates / complete write set / start snapshot / frozen 696 behavior / unique named-test contract / claim and delivery），并于 21:55:39 复扫本卡完整 section 列表（仅父级冻结段一节、无任何他人 claim）+ mtime 21:51:40；claim 后立即回读 physical EOF 确认唯一。
- 完整任务卡：既有完整 `TURN-34C - AutoBattleTask Whole-Card HTTPS Turn Cutover`。我承担完整 production/test/report 合同与父级审核后的全部整卡返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`；不拆卡、不建子卡。
- 完整 production/test/report 写集（严格沿用冻结，不增不减）：
  1. Cloud `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
  2. Create Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/AutoBattleTaskTurnContractTest.java`（唯一 named test）
  3. 本固定报告 append-only physical EOF
  其余两仓 production/test/POM/config/resource 全部只读（尤其 `AutoCombatService`、`TaskMaintenanceService`、PlayerState/TeamReturn/CommonBox/LeftTop/startup、`BaseTaskTemplate`、context/holder/checkpoint、protocol/client/action factory、host/factory/runtime；Cloud runtime activation 归 TURN-40B）。
- 领取点文件行数与 SHA-256（21:55:39 实测，与父级 start snapshot 逐字一致）：
  - `AutoBattleTask.java` 294 行 / SHA-256 `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a`
  - `AutoBattleTaskTurnContractTest.java` ABSENT（由本卡新建）
- 依赖检查：卡面 source gates 明示 `TURN-19/21/22/23/34A/34B` 均父级 source review passed（等价 closure）；named tests/build 属交付后门不阻 source-start。起始依赖满足。
- 与其它 active owner 写集冲突检查：External A=`TURN-28 Build Repair #2`（21:52:53 claim，NpcClick 系 + vision-memory 四文件 + OcrWindowScanService 静态子集）零交集；B/C 当前无卡；`AutoBattleTask.java` 与新 test 路径无人持有；两仓既有 dirty/untracked 全部保护。
- 实施边界（按冻结 696 行为逐条）：execute(context) 同 exact context 全生命周期 + normal/terminal 双路 sentinel 恢复 + null/missing context 先于任何 collaborator fail-closed；startup gate→RUNNING→first-aid→maintenance init→AutoCombat init→checkpoint/tick 固定序；combat 非 NONE 不跑 idle maintenance、NONE+FREE 一次 baseline idle chain；local team return 的 CommonBox gate/consume 在前且 consume 不短路 TeamReturn、closed capability timeout 0L 零 probe/wait/retry；pending leader 阻断 standalone return/left-top/opportunistic maintenance、三分支真值表保持；maintenance request 全字段按已通过 collaborator contract；polling 500ms→3000ms→dynamic 不求和不 backoff；confirmed stop/failed/uncertain 原样 terminal 零补偿零重试；本 Task 零 action UUID/capture/input/OCR/activation authority。
- named test 边界：从 production public `execute(...)/stop()` 进入，覆盖卡列全部维度；允许同文件 scripted ports 与 deterministic protected sleep seam；禁止 private reflection、source-only 断言冒充行为、复制 lower JSON/PNG mapper、启动 Spring/host/runtime/input/capture。
- 纪律：不运行 Maven/JUnit/compile/package（A=TURN-28 为活跃 Java writer）；不启 runtime/application/server/Task/UI/capture/input；零 Git mutation；只从当前字节增量编辑；遇合同缺口或容量问题整卡 canonical `OWNER RETURNED` + 逐 SHA 申报 WIP；完成后一次 canonical `WHOLE-CARD SOURCE+TEST DELIVERED` 并停笔待审。
- 无已批准业务差异；按 `docs/业务逻辑.md` 与 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34C EXTERNAL-d WHOLE-CARD CLAIMED SOLE-OWNER PROD=e13bfff7/294L TEST=ABSENT-TO-CREATE WRITESET=AutoBattleTask+AutoBattleTaskTurnContractTest+card 2026-07-16T21:56:10-04:00 -->
## EXTERNAL-d TURN-34C WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED - 2026-07-16T22:01:30-04:00

交还完整 TURN-34C。不是 delivery、不是 approval、未拆卡。**本轮零 Java/test 字节**：`AutoBattleTask.java` 保持 294 行 / SHA `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a`（22:00:03 复测与领取点/父级 start snapshot 逐字节一致）；唯一 named test `AutoBattleTaskTurnContractTest.java` 仍不存在（未创建任何半成品）。零 Git mutation；未运行 Maven/JUnit/compile/runtime/input；两仓 dirty/untracked 与他人写集（含 A 的 TURN-28 Repair #2 十文件）完好。我自此不持有本卡 owner。

### 交还原因：冻结写集内合同不可满足（gate，非 capacity）——与 TURN-23/TURN-28 同型的计划合同缺口

**缺口 1（致命，阻断整个 named test）：startup gate 对冻结 test 路径完全不可构造、不可脚本。**
- `checkAutoBattle` 是 `execute()` 进入后的**第一个协作者调用**（`AutoBattleTask.java:122`），且 `AutoBattleTask` 无任何 startup seam（唯一 protected seam 是 `sleepSafely`/`getRetryPolicy`）。构造 Task 时传 null startup service = 起步 NPE → **named test 无法从 public `execute(...)` 驱动任何一条路径**，卡面点名的 startup skip/exact order 更无从覆盖。
- `TaskStartupCheckService` 为 `public final` 类 + **package-private 构造**（`task/startup/TaskStartupCheckService.java:15,19`），唯一构造输入 `Evaluation` 是 **package-private** 嵌套类（private 构造，`CloudStartupGateAuthority.java:152,157`），只能经 **package-private final** 的 `CloudStartupGateAuthority`（`:19`）的 package-private `seedBaselineNoOverride/seedControlPlanePolicy/bind`（`:38,:61,:125`）铸造。冻结 test 路径在 `com.yueyunfe.dhxy.cloudbrain.task` 包——三层全部不可达；不可子类化（final）、不可 mock（仓内无 mocking 库）、反射被明令禁止、第二 test 文件/production hook 亦被禁。仓内现存 test 树对该 service 的引用为**零**（无任何先例可循）。
- 卡面"允许同测试文件 scripted ports"对此无效：不存在可注入的 startup 抽象。

**缺口 2（production cutover 同样被写集外拦死）：startup gate 的 context fence 是 legacy-only。**
- `Evaluation.requireExactContext`（`CloudStartupGateAuthority.java:162-171`）调 `context.getScope()`（`:164`）并经 `StartupRoleFact.matches`（`:253-263`）比对 `getPlayerIdentityEpoch/getStopEpoch/getRunRevision`（`:260-262`）——四个 getter 在 `TaskExecutionContext` 均为 legacyDelegate-only（`:217-219,:227-229,:232-234,:237-239`），对**任何 API 可构造（turn-native）context 直接抛 IllegalStateException**；`bind()` 同理（`:127` 调 getScope）。即：只要 `task/startup/*` 保持现字节，AutoBattleTask 的 turn-native 运行在 `:122` 就被写集外文件拒绝——本卡标题的 HTTPS turn cutover 在冻结写集内**不可能达成 turn-native 可运行性**。
- 注册表 17.3 已把 `CloudStartupGateAuthority.java` 与 `TaskStartupCheckService.java` 划归 **TURN-38B3**（S=23+38A，PLANNED/NOT READY）；本卡不得越权双写（另注：17.3 B3 行写的路径是 `service/TaskStartupCheckService.java`，实际文件在 `task/startup/`，请父级修计划时一并勘正）。
- legacy context 路线也不通：`TaskExecutionContext(CloudTaskServiceExecutionContext)` 的参数类型无 public 构造（仅 task-run authority assembly 内部铸造），test 无法取得。

**缺口 3（写集内可修，随附申报供合同修复后一并处理）：**
- `AutoBattleTask.java:120` 调 `BaseTaskTemplate.logWindowContext`（`:185-197`），其 `:195-196` 读 legacy-only `getRunRevision/getStopEpoch`——即使缺口 1/2 解决，turn-native 运行也会在此先崩。`BaseTaskTemplate` 只读，但调用点在本卡写集内可就地适配；先例：Xiuluo/Wubei/FiveRing 三个已通过的 Task 均不调用 `logWindowContext`（全 task 树 grep 仅 AutoBattleTask 一处）。

### 供父级修计划合同的选项（处置权在父级）

1. 仿 TURN-23 Repair #2 / TURN-28 Amendment #2-3 先例：把 `task/startup/` 两文件的 **dual-path context fence 最小适配**（turn-native 用既有 exact tenant/user/device/window/HWND/process/title 权威替代 legacy scope/epoch 比对，不改 policy/role 语义）加入本卡或先行小卡，并与 TURN-38B3 的归属作显式互斥/时序裁决；同时为 test 授权一条可构造路径（如 `TaskStartupCheckService` 增 public 静态 factory，或将 authority seeding 提升 public——均为 API visibility 适配，非业务变更）。
2. 或裁决 startup gate 留 legacy、named test 的 startup 维度改由 TURN-38B3/40B 侧闭合——但须同时给本卡一个可用的 service 构造/注入途径，否则 execute() 仍不可驱动。
3. 缺口 3 建议随合同修复明确：授权 AutoBattleTask 在写集内将 `:120` 替换为 dual-path 安全的等价诊断日志（或按兄弟 Task 先例移除）。

### 纪律确认

未自扩写集、未造 stub/wrapper、未触碰 `task/startup/*` 或 `BaseTaskTemplate`；本 lane 上下文余量充足，交还纯因合同 gate。合同修复转 READY 后本 lane 可即时重领。

<!-- TRUE_EOF: TURN-34C EXTERNAL-d WHOLE-CARD OWNER RETURNED PLAN-CONTRACT-BLOCKED ZERO-BYTES PROD=e13bfff7/294L-UNCHANGED TEST-ABSENT GAP1=TaskStartupCheckService-unconstructible-final-pkg-private GAP2=startup-fence-legacy-only-getScope-epochs-throws-turn-native-owned-by-TURN-38B3 GAP3=logWindowContext-legacy-getters-in-writeset-adaptable CAPACITY-OK-GATE-NOT 2026-07-16T22:01:30-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #1 / WHOLE-CARD BUILD REPAIR #1 READY - 2026-07-16T22:06:00-04:00

父级接受上方 External d 的 canonical zero-byte return，owner 释放。三个缺口成立，但正确处置不是扩大本卡去
修改 `task/startup/*`，而是修正验收分层，避免 `34C -> 38A -> 38B3 -> 34C` DAG 环。

### 修订后的完整写集

1. Cloud `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
2. Cloud `src/test/java/com/bot/dhxy/task/AutoBattleTaskTurnContractTest.java`
3. 本固定报告 append-only physical EOF

原 `com/yueyunfe/dhxy/cloudbrain/task` test 路径由本修订废止；该文件仍 ABSENT，无迁移字节。

### Startup collaborator 与测试所有权

- `AutoBattleTask` 可在类内增加一个最小 package-private functional collaborator（例如
  `AutoBattleStartupCheck`），签名只接 `TaskExecutionContext` 并返回 `TaskStartupCheckResult`。
- 现有 public production constructor 的参数与可见性保持不变，并机械绑定
  `taskStartupCheckService::checkAutoBattle`；生产路径仍调用真实 service，禁止 null bypass、恒 allow/skip、
  第二 policy 或复制 startup 判断。
- 可增加 package-private constructor 供同 package named test 注入 scripted collaborator；它必须复用同一字段、
  同一 `runAutoBattlePatrol` 调用点，不得形成 wrapper nesting 或第二 production 流程。
- 本卡 test 只证明 AutoBattleTask 对 startup result 的 orchestration：skip 零后续副作用、allow 后 exact startup
  顺序、startup terminal 原样传播与 context-holder restore。`TaskStartupCheckService` 自身的 policy/role/context
  fence 不在本卡重复测试。
- `task/startup/TaskStartupCheckService.java` 与 `CloudStartupGateAuthority.java` 的 dual-path exact-context、
  public production construction/activation 和真实 integration 继续归 TURN-38B3/40B；本卡 source pass 不表示
  turn-native startup runtime 已通过。

### Legacy diagnostic

授权仅在 `AutoBattleTask.java` 删除 `logWindowContext(context)` 调用。前后的
`context.getLogPrefix()` startup/end 日志保留；不得新增读取 legacy scope/epoch/runRevision/stopEpoch 的替代日志。
这是诊断兼容适配，零业务条件、顺序、次数、terminal 或 retry 差异。

原卡 Frozen 696 behavior 与其余 named-test matrix 全部继续有效。状态恢复：
`WHOLE-CARD BUILD REPAIR #1 READY / ZERO OWNER`。External Worker 自行 canonical 领取；父级不派卡、不建 reviewer。

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34C PARENT-PLAN-CONTRACT-AMENDMENT-1 BUILD-REPAIR-1-READY ZERO-OWNER TASK-LOCAL-SCRIPTED-STARTUP-SEAM SAME-PACKAGE-NAMED-TEST REAL-STARTUP-INTEGRATION-REMAINS-38B3-40B REMOVE-LEGACY-DIAGNOSTIC-ONLY NO-DISPATCH NO-REVIEWER 2026-07-16T22:06:00-04:00 -->

## EXTERNAL-d TURN-34C WHOLE-CARD BUILD REPAIR #1 CLAIMED - 2026-07-16T22:07:20-04:00

EXTERNAL-d TURN-34C WHOLE-CARD CLAIMED 2026-07-16T22:07:20-04:00

- 领取时间：`2026-07-16T22:07:20-04:00`。Worker：CR271 External implementation Worker d（本会话；即 21:56 首领与 22:01 canonical zero-byte 归还的同一 lane，合同缺口三项已被父级 Amendment #1 全部采纳处置，上下文零摸底成本）。implementation only，非 reviewer；交付后仅由父级本人复审。本段不含 `APPROVED/CLOSED`，不自批。
- claim 前已完整读取 22:06:00 PARENT PLAN-CONTRACT AMENDMENT #1 全文并于 22:06:56 复扫本卡完整 section 列表（四节，Amendment 为最后一段、无他人 claim）+ mtime；claim 后立即回读 EOF 确认唯一。
- 完整任务卡：既有完整 `TURN-34C` 之 WHOLE-CARD BUILD REPAIR #1。合同 = 21:50 原冻结卡（Frozen 696 behavior + named-test matrix 继续有效）+ 22:06 Amendment #1（修订写集 / startup collaborator 与测试所有权 / legacy diagnostic 删除授权）。我承担完整 production/test/report 与后续整卡返修，直至父级 PASSED 或 canonical OWNER RETURNED；不拆卡。
- 完整 production/test/report 写集（Amendment #1 修订版，不增不减）：
  1. Cloud `src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
  2. Create Cloud `src/test/java/com/bot/dhxy/task/AutoBattleTaskTurnContractTest.java`（唯一 named test，同包注入 seam；原 cloudbrain/task 路径已废止、无迁移字节）
  3. 本固定报告 append-only physical EOF
  其余两仓全部只读（尤其 `task/startup/*`——dual-path fence 与真实 integration 归 TURN-38B3/40B）。
- 领取点实测（22:06:56）：`AutoBattleTask.java` 294 行 / SHA `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a`（与我 22:01 归还快照逐字节一致，期间无第三方写入）；新 test 路径 ABSENT。
- 依赖检查：Amendment #1 即本卡当前 READY 权威（source gates 原卡已列：19/21/22/23/34A/34B 均 source-passed）。
- 与其它 active owner 写集冲突检查：External A=`TURN-28 Build Repair #2`（NpcClick 系十文件）零交集；B/C 当前无卡；两个目标路径无人持有。
- 实施要点（按 Amendment 逐条）：① `AutoBattleTask` 增 package-private functional collaborator `AutoBattleStartupCheck`（仅 `TaskExecutionContext -> TaskStartupCheckResult`），public 构造签名/可见性不变并机械绑定 `taskStartupCheckService::checkAutoBattle`，增 package-private 构造供同包 test 注入 scripted 实现——同一字段、同一 `runAutoBattlePatrol` 调用点、零 wrapper nesting、生产路径零 null bypass/恒 allow/第二 policy；② 删除 `:120` `logWindowContext(context)` 调用（保留前后 getLogPrefix 日志，不新增 legacy scope/epoch 读取）；③ 唯一 named test 从 public `execute(...)/stop()` 进入，覆盖原卡全 matrix + Amendment 界定的 startup orchestration（skip 零后续副作用、allow 后 exact startup 顺序、startup terminal 原样传播、context-holder sentinel restore），scripted ports 同文件、protected sleep seam 确定性驱动，禁 private reflection/source-only 断言/复制 mapper/启动 runtime。
- 纪律：不运行 Maven/JUnit/compile/package（A 为活跃 Java writer）；不启 runtime/application/server/Task/UI/capture/input；零 Git mutation；只增量编辑；遇新合同缺口或容量问题整卡 canonical OWNER RETURNED + 逐 SHA 申报；完成后一次 canonical `WHOLE-CARD SOURCE+TEST DELIVERED` 停笔待审。
- 无已批准业务差异；按 `docs/业务逻辑.md` 与 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34C EXTERNAL-d WHOLE-CARD BUILD-REPAIR-1 CLAIMED SOLE-OWNER PROD=e13bfff7/294L TEST=ABSENT-TO-CREATE-SAME-PACKAGE SEAM=AutoBattleStartupCheck-pkg-private REMOVE-logWindowContext 2026-07-16T22:07:20-04:00 -->
## EXTERNAL-d TURN-34C WHOLE-CARD SOURCE+TEST DELIVERED (BUILD REPAIR #1) - 2026-07-16T22:20:00-04:00

EXTERNAL-d[TURN-34C] WHOLE-CARD SOURCE+TEST DELIVERED

- 交付时间：`2026-07-16T22:20:00-04:00`。Worker=CR271 External Worker d（22:07:20 canonical claim 的同一会话），非 reviewer，不自批，本段不含 `APPROVED/CLOSED`。
- 完整改动文件（Amendment #1 修订写集，逐文件实测）：
  | 文件 | 行数 | SHA-256 | 状态 |
  |---|---:|---|---|
  | `task/AutoBattleTask.java` | 326（原 294） | `e1879ed9c46a77fda8344ca8b272cdacf67cc2aa113b811bba92d2c86f4d455a` | 本轮 production 改动 |
  | `task/AutoBattleTaskTurnContractTest.java`（test，同包） | 1052 | `fa20cd2905738828bddf592b31c336dd93dd8509876ac662c531113f0eae2dad` | 新建，32 `@Test` |
- production 行为说明（恰两处，均为 Amendment #1 明文授权的机械适配，零业务差异）：
  1. **startup seam**：新增类内 package-private `@FunctionalInterface AutoBattleStartupCheck`（:323，唯一方法 `check(TaskExecutionContext)->TaskStartupCheckResult`，置于文件底部 per 布局规则）；public 构造签名/可见性逐字不变，委托新 package-private 构造并机械绑定 `taskStartupCheckService::checkAutoBattle`（:66）；两构造共用同一 `startupCheck` 字段与唯一调用点 `runAutoBattlePatrol`（:143 `startupCheck.check(context)`）。零 wrapper nesting、零 null bypass、零第二 policy；seam javadoc 写明存在原因与禁改边界。
  2. **legacy diagnostic 删除**：移除原 :120 `logWindowContext(context)` 一行（该基类方法读 legacy-only `getRunRevision/getStopEpoch`，turn-native 必抛）；前后 `getLogPrefix()` 启停日志保留，未新增任何 legacy scope/epoch 读取。
  其余 production 逐字节未动：startup gate→RUNNING→first-aid→maintenance init→AutoCombat init→checkpoint/tick 固定序、combat 非 NONE 跳 idle、NONE+FREE 单次 idle chain、local return CommonBox 先行且不短路、closed capability timeout 0L、pending leader 阻断、三分支 maintenance request 全字段、polling 500→3000→dynamic、stop/terminal 零补偿零重试、零 action/UUID authority——全部保持领取点字节。
- named test source（32 用例，全部从 production public `execute(...)/stop()` 进入，同文件 scripted collaborators + protected sleep seam，零反射、零 source-only 断言、零 mapper 复制、零 runtime/Spring/host/input）：
  - **frozen identity + fail-closed**：taskCode/taskName 冻结；`execute()`/`execute(null)` 双入口在任何 collaborator 前抛 `TaskCheckpointTransitionException`、零事件、BotStatus 不动。
  - **startup orchestration（Amendment 界定）**：skip→`SKIPPED` 且事件仅 `["startup"]`、永不 RUNNING；fail→`FAILED`；allow→事件精确序 `[startup, firstAid, maintenanceInit, combatInit, combatTick, ...]`，gate 时 BotStatus 仍 IDLE、first-aid 时已 RUNNING；startup 抛 `TaskStopRequestedException` 原样传播、零后续。
  - **exact context / sentinel restore**：五个 collaborator 逐一 `assertSame` 同一 context 引用；holder 预绑 sentinel 在正常退出与 stop unwind 两路均恢复。
  - **tick gating**：IN_COMBAT/EXIT_RECOVERED 迭代零 idle chain（事件序列直接断言）；NONE+非 FREE 亦跳过。
  - **idle chain 真值表**：local release 的 consume 先行且不短路 click（事件序 `[consumeBox, returnClick]`）+ await 恰以 `0L` 探测 + released 迭代零 maintenance；box capability 关→零 consume 仍 click；TEAM_RETURN 关→落入 maintenance 且 member 不做 standalone click；pending leader→standalone/left-top/maintenance 全零；standalone click true→短路 maintenance（source `auto-battle`）。
  - **三种 maintenance request 形状逐字段**：standalone（oneSummon=false/teamKey=null/openWindow=false/capability=null/broadcast=true/fullFallback=false/cleanSummon=true/max=1）；local-support gate（capability=SUMMON_SKILL、teamKey=null、LEFT_TOP 开时 consumeFollowerSafeWindow("wubei")、关时零 consume）；legacy pathing gate（teamKey="wubei"、openWindow=true、capability=null、零 left-top）；不支持的 requested task 与 LEADER 角色均回退 standalone 形状（isFollowerSupportMode 真值表）。
  - **polling 优先级**：pending→500、FREE→3000、非 FREE→dynamic（脚本 777）、逐迭代重评（[500,3000] 序列）。
  - **stop/terminal**：`stop()` 后 loop 以 `STOPPED` 退出且零下一 tick、IDLE+FREE 恢复；loop checkpoint 上的 confirmed stop 原样抛出零 retry；drifted native generation 抛 `TaskCheckpointTransitionException` 不伪 stop；handled maintenance 结果仅记录、loop 继续。
  - **零 action/UUID authority**：context 的 scripted command port `execute` 计数并抛错，全程 `executeCalls==0`（含最重的 local-release 全链迭代）。
  - 确定性：sleep seam 记录每次间隔并按脚本推进，脚本耗尽即经 production 自身 IDLE 条件退出；无 wall-clock、无真实 sleep。
- 基线核对：对照 `696a12b0` 与 `docs/业务逻辑.md` 维护优先级/caller 顺序，冻结行为八条逐条由测试断言锁定；production 仅上述两处机械适配。**有意业务差异：无。**
- 如实申报：
  1. 测试用例**未经执行验证**（A=TURN-28 活跃 Java writer，Maven/JUnit 禁用）；授权命令 `mvn -q -Dtest=AutoBattleTaskTurnContractTest test` 留待父级 stable-writer 门。
  2. scripted collaborator 的 super 构造已逐一核实真实签名（含 `PlayerStateService`/`CommonBoxService` 的 `requireNonNull` 全参陷阱——分别传真实无害 stand-in：`ClientIdentityService(new Holder)`、四个 lombok port、`CloudTemplateAssets` lambda、匿名 `CloudCommonBoxPort` 双方法抛错实现），无 34B 曾出现的构造 NPE 形态。
  3. 真实 turn-native startup gate 集成（`task/startup/*` dual-path fence）按 Amendment 归 TURN-38B3/40B；本卡 source pass 不代表 turn-native startup runtime 已通过。
- 未运行项目：Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input 全未运行；零 Git mutation；两仓其余 dirty/untracked（含 A 的 TURN-28 WIP）未触碰。
- 父级审核请求：请执行完整 SOURCE+TEST SOURCE REVIEW。交付后本 Worker 停止修改本卡，等待父级结论；REPAIR/BLOCKED 由本 Worker 整卡返修。
- 无已批准业务差异；按 `docs/业务逻辑.md` 与 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34C EXTERNAL-d WHOLE-CARD SOURCE+TEST DELIVERED BUILD-REPAIR-1 PROD=e1879ed9/326L TEST=fa20cd29/1052L/32T SEAM+DIAGNOSTIC-ONLY-PROD-DELTA FROZEN-696-MATRIX-CLOSED AWAITING-PARENT-REVIEW 2026-07-16T22:20:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-16T22:23:47-04:00

父级已完整读取本卡、交付报告 physical EOF、Cloud production 326 行与唯一 named test 1,052 行，并复算
SHA-256 与交付一致：`AutoBattleTask.java=e1879ed9c46a77fda8344ca8b272cdacf67cc2aa113b811bba92d2c86f4d455a`，
`AutoBattleTaskTurnContractTest.java=fa20cd2905738828bddf592b31c336dd93dd8509876ac662c531113f0eae2dad`。

### Verdict

`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD COMPILE PENDING`

- production delta 符合 Amendment #1：public constructor 仍机械绑定真实
  `TaskStartupCheckService::checkAutoBattle`；package-private seam 仅承载同一字段与同一
  `runAutoBattlePatrol` 调用点，没有恒 allow、第二 startup policy、wrapper nesting 或新增业务分支。
- 唯一 legacy-only `logWindowContext(context)` 已移除；startup/end `getLogPrefix()` 日志保留。相对
  `696a12b0` 的 startup -> RUNNING -> first-aid -> maintenance init -> combat init -> checkpoint/tick、idle
  chain、local return、maintenance request、polling 和 terminal 顺序均未改变。
- 32 个 public-path tests 覆盖卡面全部冻结维度：missing context、exact context/sentinel normal+terminal restore、
  startup skip/fail/allow/terminal、combat/idle gating、CommonBox -> TeamReturn、closed capability/pending leader、
  standalone/local/legacy follower request shapes、500/3000/dynamic polling、stop/drift terminal 与零 action/UUID。
  scripted collaborators 与 protected sleep seam 均局限在唯一 test；无 reflection、source-string 断言、runtime、
  capture 或 input。
- 真实 startup authority/dual-path construction 与 runtime activation 仍明确归 TURN-38B3/40B；本卡通过不提前
  批准该边界。

本轮未运行 Maven/JUnit/compile：TURN-28 Repair #3 owner 尚未 canonical 重交，仍按共享 Java writer 门等待稳定。
External d 的 TURN-34C owner 现释放；无需额外 reviewer。后续只欠稳定写者窗口中的授权 named test 与 Cloud compile。

**无已批准业务差异；按 `696a12b0` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-34C PARENT-SOURCE-TEST-REVIEW-1 PASSED P0-P1-P2=0-0-0 OWNER-RELEASED NAMED-TEST+CLOUD-COMPILE-PENDING NO-EXTRA-REVIEWER 2026-07-16T22:23:47-04:00 -->

## PARENT NAMED-TEST GATE ATTEMPT - 2026-07-16T22:26:47-04:00

稳定写者窗口成立后，父级在 Cloud 仓执行授权命令：

`mvn -q -Dtest=AutoBattleTaskTurnContractTest test`

结果 `exit 1`，在 main compile 阶段即被共享未闭合迁移债阻断，尚未进入 TURN-34C testCompile/test execution。
首个错误为 `model/ocr/TextCandidateScanResult.java` 缺 `TextCandidateScanStatus`；随后
`WubeiTask.java`、`NavigationService.java`、`FiveRingTaskV2.java` 缺
`GameClientTracker`、`TextRecognizer`、`BagService`、`UICleanerService`、`TaskTransactionRunner`、
`TaskTurnCoordinator`、`CoordinateHelper`、`GameStateUtil`、window runtime 等尚未迁移/删除的共享类型。
输出没有指向 `AutoBattleTask.java` 或 `AutoBattleTaskTurnContractTest.java` 的编译错误。

因此 Review #1 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` 保持；构建状态明确为
`NAMED TEST+CLOUD COMPILE BLOCKED BY SHARED CLOUD COMPILE DEBT`，不是 TURN-34C 返修。owner 保持释放。

TRUE_EOF

<!-- TRUE_EOF: TURN-34C NAMED-TEST-ATTEMPT EXIT-1 BLOCKED-BY-SHARED-CLOUD-MAIN-COMPILE-DEBT TEST-NOT-ENTERED SOURCE-REVIEW-PASS-UNCHANGED OWNER-RELEASED 2026-07-16T22:26:47-04:00 -->
