# CR271 TURN-34A AT3+ Remaining-Tranche Readiness Helper

## 0. 角色、边界与结论

- 角色：CR271 Internal helper；不是 implementation owner、reviewer、父级、派单人或批准人。
- 本报告只做 TURN-34A 在 AT1/AT2 之后的剩余测试合同分解。它不是 claim、delivery、review、READY、
  `TEST-SOURCE REVIEW PASSED`、`CARD APPROVED` 或 CR 状态变更。
- 当前结论：`CONTRACT CANDIDATE CONVERGED / CLAIM GATE CLOSED`。下一最小片可以精确描述，但当前不能领取。
- 本轮唯一写入是本报告。未修改 Java、测试、TURN-34A/AT1/AT2 卡、`ACTIVE_WORK.md`、权威计划、dashboard、
  POM、resource 或其它报告。
- 未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input，也未运行任何 Git 命令或
  Git mutation。

## 1. 已读取的权威与 true-EOF 材料

本 helper 在写入前读取并交叉核对：

1. `AGENTS.md`、`docs/DHXY_CONTEXT.md`。
2. `docs/ACTIVE_WORK.md` 顶部 CR271，最新为 `2026-07-16 11:33`。
3. `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节及其最新头部状态。
4. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
5. `docs/业务逻辑.md`，重点是通用盒子优先级/30 秒生命周期、Expected 战斗快脱战与回程验证失败纠正。
6. TURN-34A 父卡、TURN-34AT1 子卡到物理 true EOF，以及 AT1 两份独立 review。
7. AT1 readiness/delivery/Repair #3 preflight、AT2 readiness 与 parent-freeze preflight、旧 AT3 readiness preflight，
   均读取到各自 `TRUE_EOF`/`PRECHECK_COMPLETE` 尾标。
8. 当前 Cloud `AutoCombatService.java`、`AutoCombatServiceTurnContractTest.java`、`BattleRadarService.java`、
   `TaskMaintenanceService.java` 及 AutoBattle/FiveRing/Wubei/Xiuluo 四个真实 caller。
9. Cloud `migration-baseline/696a12b0` 中的 `AutoCombatService.java`/`BattleRadarService.java`。

Helper/preflight 只能提供证据和候选合同；发生冲突时，以权威计划、固定卡最新物理 true EOF、父级结论和
`docs/业务逻辑.md` 为准。

## 2. 两仓只读状态与当前字节锚点

按用户禁令，本轮没有重新执行 `git status`。分支/HEAD 由两仓 `.git/HEAD` 与对应 ref 只读确认；dirty/untracked
数量采用最新相关 preflight 已记录的 porcelain 快照，不能冒充本轮重新计算的实时状态。

| Repo | 只读确认的 branch / HEAD | 最新已记录 status 快照 |
|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 43 modified、1 deleted、685 untracked |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 9 modified、541 untracked |

所有既有 dirty/untracked 均受保护，不是清理或恢复清单。Cloud `.gitignore:15` 忽略整个 `src/test/`，因此共享 named
test 是受卡内 SHA 保护的物理工作区字节，不能依赖 Git 恢复旧 snapshot。

当前只读复算锚点：

| Artifact | 当前事实 |
|---|---|
| Cloud `AutoCombatService.java` | 852 行，SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`；父级 production source 已通过并冻结 |
| Cloud `AutoCombatServiceTurnContractTest.java` | 1,026 行 / 22 `@Test`，SHA-256 `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292`；仍是 AT1 Repair #3 前的被阻断 snapshot |
| TURN-34AT1 最新 true EOF | Parent Review #4：`P0/P1/P2=0/3/0 / REPAIR #3 REQUIRED / EXTERNAL-D FRESH RESTART CLAIM REQUIRED` |
| TURN-34AT2 固定子卡 | 不存在；现有两份 AT2 文件只是 helper/preflight，不是卡或批准 |
| CR271 最新顶层状态 | 11:33：External C 的 BP1 Repair #2 已有受保护 provisional source-active WIP，但 claim 尚缺规范 true-EOF；A/B/D 仍无新 claim/零 owner，其中 D 对应 AT1 Repair #3 |

因此，旧 AT3 preflight 在 10:34 形成的 owner/状态口径已经过时；其中可复核的业务切片可保留为候选，但不能据此
自解锁或指定未来 owner。

## 3. AT1/AT2 覆盖所有权与当前门

### 3.1 AT1 当前不是完成态

AT1 计划拥有：真实 Stage-1 battle flag 入战、最小单 CAPTURE/raw PNG/metadata/UUID 合同，以及首 CAPTURE 的
BUSY、duplicate、timeout/interrupted uncertainty、FAILED、STOPPED、DUPLICATE_OR_UNCERTAIN 零 fallback。

但最新父级已接受三个 P1，故当前 `b5438da...` 不能作为 AT2/AT3 起始锚点：

1. `FAILED` fixture 必须是合法 `failedStepIndex=0` + step 0 `FAILED`，不能继续走 generic exception fallback。
2. same team + same window 的 `+10ms` reservation 仍受严格 30 秒 gate，不能凭窗口相同放行。
3. 最小 CAPTURE 还必须显式锁住 `clearPointerIfOverRegion==null` 与 `pixelChangeProbe==null`。

### 3.2 AT2 只是已收敛的未来候选

AT2 两份 preflight 收敛的 test-only 语义是：两轮普通 COMPLETED Stage1/2/3 全 miss，第二轮后恰一次 minimap
读取；可读坐标发布 exit，不能读保持 `IN_COMBAT`；每条路径恰好 7 个相关 CAPTURE/UUID，read-only probe 不消费
exit signal。

AT2 尚无固定卡、claim、字节、父级 review 或 accepted SHA。以下所有“AT2 之后”的分解都只是以未来 AT2 按该合同
被父级接受为前提，不能把 preflight 写成已实现事实。

## 4. 假定 AT1/AT2 按合同通过后，TURN-34A 仍未覆盖的真实合同

| 剩余合同域 | 权威要求 | AT1/AT2 后仍缺的证据 |
|---|---|---|
| FAST recovery handoff | FAST exit 返回 `EXIT_RECOVERED`，只挂 deferred leader recovery；initialize 故意保留；可信 `IN_COMBAT` 纠正时不得消费 | 当前 initialize 测试从未先制造 pending，只证明初始 false；没有真实 public handoff/保留证据 |
| Deferred leader safe point | 非战斗时先清 pending，再做 first-aid 与 incense；stop/terminal 不得重放；成功只消费一次 | AT1/AT2 只负责 radar observation，不负责恢复动作 |
| 三 recovery policy | null=`FULL_RECOVERY`；boolean false/true=`FULL`/`FULL+INCENSE`；FAST 只 defer | 只有 enum/签名检查，没有三条 public runtime 路径和动作顺序 |
| CommonBox -> follower first-aid | box 永远先于 follower first-aid；box success 留 first-aid 到下一 tick；SUPPLY_NEEDED/UNKNOWN/HEALTHY 与唯一 re-probe 保持 | 无 pending box + follower pending 的同 tick 有序事件证据，无一次且仅一次 re-probe 证据 |
| Enter/maintenance timing | enter `+4s`，periodic clean `40s`，team/urgent `30s`，deferred log `10s`；reason `UNKNOWN -> LOW_ROUNDS(<=10) -> REFRESH_DUE` | 当前只测 dynamic interval 和一部分 30 秒 gate；其余 timing/priority 未走 public maintenance path |
| FAST cadence | 15 秒 delay、1 秒 fast probe、4 秒 full-radar fallback；不加额外 full-radar wait | AT1/AT2 不覆盖 avatar-diff cadence |
| Panel mechanics | target `(left+489, top+726)`，drag 仅在距离 `>20px`；entry 既有 500ms panel wait | 无 AutoCombat public path 到真实 panel action 的坐标/顺序/terminal 断言 |
| Window guard/public APIs | guard 可刷新 enter/状态但不消费 exit/recovery；trusted baseline refresh 的真实返回/动作保持 | 目前主要是方法存在性，未锁 runtime decision |
| 四 caller | AutoBattle/FiveRing/Wubei/Xiuluo 的 initialize、policy、phase、返回值消费、500/3000/dynamic 与 wake clamp | 尚未执行四个真实 Task caller phase；直接调用 Service 不能替代 caller 分支 |
| Later action terminal/correlation | 每个 action-capable branch 都要 fresh UUID/one command，STOP/FAILED/uncertain/correlation 错误不得变成功且零补偿/retry | AT1 只锁首个 radar CAPTURE；恢复、盒子、first-aid、incense、panel 等动作域仍缺 |
| Source/static gate | active AutoCombat 对旧 holder/coordinator、direct input/capture、七个旧 radar fact、Summon authority 零引用 | production 静态 review 已通过核心删除；最终 named-test/父级门仍需按固定卡闭合，不得用宽泛 source scan 假证明 |

业务基线没有批准任何差异。尤其不得为了补测试新增 TTL、extra read/verification、retry、cleanup、park/yield、
session/ledger/durable workflow 或新的 Cloud gate。

## 5. 推荐的下一最小独立小片：FAST deferred-recovery handoff

### 5.1 候选范围

未来父级如创建 AT3 固定子卡，建议只在现有 named test 新增一个方法：

```java
fastExpectedExitDefersLeaderRecoveryAcrossInitializeAndTrustedInCombat
```

令 `N` 为未来父级接受的 AT2 test 数量；本片只允许从 `N` 增至 `N+1`。不能把当前 22 或 AT2 preflight 的预测
数量硬编码成未来 claim 锚点。

本方法只闭合一个连续状态合同：

```text
IN_COMBAT + FAST_EXPECTED_EXIT
  -> 消费一个已由 AT2 证明合法的新鲜 exit handoff
  -> EXIT_RECOVERED + FREE + leader recovery pending
  -> initialize 保留 leader pending
  -> trusted correction 将状态改回 IN_COMBAT
  -> safe-point consume=false 且 pending 仍保留
```

它不重复 AT1 的 terminal matrix，也不重放 AT2 的七帧 radar/minimap。它不声称完成成功 recovery、FULL policy、
maintenance、panel 或 caller。

### 5.2 允许的 test-private handoff seam

同一测试文件底部可增加一个最小 `PreparedCompletedExitRadar extends BattleRadarService`，仅代表“AT2 已接受的
正向 exit 输出”进入 AutoCombat 的边界：

1. 记录 `armExpectedCombatExitWait(source)` 恰一次。
2. `checkFastExpectedCombatExitByAvatarDiff(source)` 恰一次并返回 false。
3. `shouldRunFullRadarForFastExpectedExitFallback()` 恰一次并返回 true。
4. `checkAndSyncCombatState()` 恰一次，将同一个 production `GameContext` 置 `FREE`，并准备一个只能消费一次的
   completed exit signal。
5. `consumeCombatEnterSignal()` 返回 false；`discardStaleCombatExitSignalIfInCombat(...)` 返回 false。
6. `consumeCombatExitSignalForExpectedWait(source)` 恰一次消费该 signal；普通 `consumeCombatExitSignal()` 调用数为 0。

该 seam 不得接收/构造 `TurnAction`、`CloudTurnCommandResult`、`TurnOutcome`、raw PNG、ROI、UUID、terminal status、
两轮 miss、minimap fact 或 wall-clock timestamp；不得复制 radar reducer。AT2 独占这些证明。它也不得成为 production
hook、第二测试文件、Mockito mock、private reflection 或 source scan。

### 5.3 精确 public 调用与 context

1. 用现有 production `TaskExecutionContextHolder.callWith(...)` 绑定 exact tenant/user/device/window/title/HWND/PID。
2. context 使用真实 Xiuluo leader 语义：`taskCode/requestedTaskCode=xiuluo_v2`、role=`LEADER`。
3. 调 `initializeForCurrentWindow()`；随后设置已知起点 `GameContext.ActionState.IN_COMBAT`。
4. 调 production `handleCombatTick(context, "xiuluo-v2", FAST_EXPECTED_EXIT)`。
5. 由 production `consumeExitAndRecover(...)` 执行既有 `recordCombatExit -> resetCheckCounter -> member-box hook ->
   deferred state -> FREE` 路径；leader context 必须使 member-box observation 为零 command。
6. 再调 production `initializeForCurrentWindow()`。
7. 以业务文档规定的可信回程失败纠正为前提，把 production `GameContext` 置回 `IN_COMBAT`，再调
   `consumePendingLeaderPostCombatRecoveryIfAllowed(context,
   "xiuluo-v2:return-failed:trusted-in-combat")`。

直接设置初始/纠正后的 `GameContext` 只表示测试前置事实和已经由 AT2/Task 证明的 handoff；本片不得拿它冒充 radar
检测或 caller phase 证明。

### 5.4 必须落在断言里的状态合同

1. 首次 tick 精确返回 `TickResult.EXIT_RECOVERED`，并把 `GameContext` 置 `FREE`。
2. leader deferred pending=true；follower pending=false。
3. 第二次 initialize 后 leader pending 仍为 true、follower pending 仍为 false。
4. trusted `IN_COMBAT` 下 safe-point consume 返回 false。
5. false 返回后 leader pending 仍为 true；不得先清 pending，也不得跑 first-aid/incense/CommonBox/maintenance。
6. handoff seam 计数：arm=1、fast probe=1、full-radar decision=1、prepared exit=1、expected consume=1、
   normal consume=0；prepared signal 已一次性耗尽。

### 5.5 本片的 closed-turn 合同是精确零命令

AT3 只消费 AT2 的已证明业务 signal，不产生新的观察或输入。因此必须断言：

- `ScriptedCommandPort.executeCalls==0`；actions/timeouts/results/replies 全为空。
- UUID 集合精确为空，而不是“未检查”。
- 无 CAPTURE/MATCH/INPUT/WAIT/LOCAL_SERVICE command。
- 无 retry/replay/resend/fallback/compensation/second action。
- 本方法不注入 BUSY/duplicate/timeout/interrupted/FAILED/STOPPED/uncertain/correlation-invalid；这些不能进入
  prepared-completed handoff。

## 6. AT3 claim 的全部依赖门

未来父级只有在以下条件全部成立后，才可考虑创建/开放固定 AT3 子卡：

1. AT1 Repair #3 已有真实 implementation claim、交付，并关闭 Parent Review #4 三个 P1。
2. 父级在最新 repaired AT1 SHA 上写明 `P0/P1/P2=0/0/0`；卡要求的最新独立 reviewer snapshot 已完成并释放。
3. 父级已创建真实 AT2 固定卡；AT2 两测试已交付并在最新 SHA 上通过父级 test-source review，且没有未解决
   P0/P1/P2、返修或同文件 owner/reviewer。
4. 磁盘 named test SHA 精确等于父级接受的 AT2 final SHA。任何 mtime/SHA 漂移都先回父级重新冻结。
5. production `AutoCombatService.java` 仍精确等于 `532e6f84...`；如漂移则重新做 production 定界，不能继承本报告。
6. 没有 AT1、AT2、其它 AT3+ 或 reviewer 正在拥有/读取同一 named-test snapshot。
7. 父级创建 append-only AT3 固定卡，记录 accepted AT2 SHA/行数/测试数、未来 owner、下节 exact write set、
   本片唯一 test 名称和 AT4+ exclusions。
8. 未来被指定的 implementation worker 在固定卡物理 true EOF 先 claim，并在首个窗口重新核对 initial SHA；本 helper
   不指定 C、D 或任何其它 owner，也不授权 self-unlock。

当前第 1-8 项均未全部满足，所以本报告不得被解释为 READY 或开工许可。

## 7. 未来 AT3 的精确互斥写集

父级若冻结该片，implementation write set 应恰为：

1. Modify only
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`。
2. Append claim/delivery evidence only to future
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT3.md`。

互斥规则：

- 与 AT1、AT2、其它 AT3+ tranche 共享同一个 Cloud test 文件，必须严格串行；同一时刻只能有一个 implementation
  owner，reviewer 固定 snapshot 未释放时也不得改字节。
- 未来 AT3 card path 是该片独占 append-only 日志；不得写 TURN-34A/AT1/AT2 或其它卡。
- 与当前 BP1 Repair #2 的 production/test 文件路径无交集；C 的 provisional single-writer WIP 仍受保护，且任何
  Java writer 活动时都不运行 Maven/clean/build。
- `AutoCombatService.java`、`BattleRadarService.java`、四 Task caller、`TaskMaintenanceService`、所有其它 Service、
  protocol/client/action factory/command port、POM/resources/fixtures/其它 tests、DHXY Java 全部只读。
- 不得新增 production 文件、第二测试类、resource/fixture/marked image、clock seam、wrapper/facade、session/owner/
  lease/ledger/TTL/queue/durable workflow 或自动 retry。

## 8. Future named-test 验收门（本 helper 未运行）

在所有适用 writer 释放、父级授权稳定 snapshot 后，先执行唯一新增方法，再执行完整 named class：

```powershell
Set-Location D:\mavenProject\dhxy-cloud-brain
mvn -q '-Dtest=AutoCombatServiceTurnContractTest#fastExpectedExitDefersLeaderRecoveryAcrossInitializeAndTrustedInCombat' test
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

验收报告必须记录实际命令、exit code、tests run、failures/errors、最终 test SHA 与 production SHA。方法级 exit 0
只证明本片；完整 class exit 0 才证明 AT0/已修 AT1/已接受 AT2 未被破坏。适用 Cloud compile/build、父级 review 和
独立 reviewer gate 仍按权威计划第 18/19 节及未来固定卡执行；不得用 `-DskipTests`、IDE、旧 target 或 stale jar
替代 named test。

本 helper 没有运行上述命令，也不预测当前被阻断 snapshot 的结果。

## 9. AT4+ 剩余 tranche 地图（候选，不是卡/READY）

以下只是防止下一片越界的剩余所有权建议；每片仍需父级另行冻结 exact method、fixture、terminal matrix、initial SHA
和 owner。

| 候选后续片 | 可独立拥有的合同 | 当前 readiness 风险 |
|---|---|---|
| Deferred-success recovery | 非战斗 safe point 的 clear-before-work、first-aid、leader incense、一次消费与 stop/terminal 零重放 | 需冻结真实 PlayerState/Incense typed command 脚本和 terminal/correlation matrix；不能并入本 AT3 零命令片 |
| FULL policy matrix | null/boolean overload 与 `FULL_RECOVERY`、`FULL_RECOVERY_WITH_LEADER_INCENSE` 的精确映射、动作顺序和 leader incense 差异 | 需防止用 enum/直接 mock return 代替 public tick；至少两个 policy 分支，仍是同 named test 串行写 |
| Member box/follower recovery | exit 后 member box detect，box-before-first-aid，box success 留 pending，SUPPLY_NEEDED/UNKNOWN/HEALTHY，team gate 与唯一 re-probe | 六个 TaskMaintenance API 和多 typed collaborator 顺序尚需父级冻结；每个 action branch 要补 STOP/FAILED/uncertain/correlation 零 retry |
| Enter/window guard | enter signal 的 500ms panel bootstrap、guard 不消费 exit/recovery、read-only/guard/tick 三入口边界 | 不能把 AT1 的 read-only enter 直接当 guard 证明；panel command shape 需单独 fixture |
| Timing/maintenance/panel | `+4s/40s/30s/10s`、15/1/4、reason priority、`(489,726)`、`>20px` | **当前不能诚实冻结为纯 test-only 小片**：production 直接使用 `System.currentTimeMillis()` 与 private runtime state；sleep、reflection 或反推常量均不被接受。父级须先裁定复用前置卡证据，或明确批准最小可控 clock 边界；本 helper不扩 production 写集 |
| Four caller slices | AutoBattle、FiveRing、Wubei、Xiuluo 各自真实 initialize/policy/phase/return/delay/wake consumption | 四 Task 均只读且 phase/构造图很大；必须一 caller 一小片冻结真实 public execution seam。复制 switch 或 scripted `AutoCombatService` return 不是验收 |
| Final source/static gate | 旧 authority/direct mechanics/radar fact/Summon reference 零 active caller，所有 public surface 与后基线六 API 缺席 | 由父级静态源码审查和未来卡允许的窄 gate 闭合；不得新增宽泛 source-guard 测试或扫描字符串冒充业务运行证据 |

其中 timing/maintenance/panel 与四 caller 目前只有“真实缺口”，没有足够的可实施 exact seam；把它们写成 READY 会
违反权威卡和 investigation-first/业务基线门。下一实现者只能做父级已固定的一个小片，不能顺手把表内其它行并入。

## 10. Precheck 结论

AT1 最新仍在 Repair #3，AT2 仍无固定卡，所以 AT3 当前不可 claim。假定两者以后按各自固定合同被父级接受，
最小、非重复且可 test-only 落盘的下一片是一个零命令 FAST deferred-recovery handoff：真实调用 production
`handleCombatTick` 与 deferred consume public API，证明 `EXIT_RECOVERED -> pending -> initialize 保留 -> trusted
IN_COMBAT 不消费`。其余 successful recovery、FULL policies、CommonBox/follower、timing/maintenance/panel 与四
caller 继续留在明确列出的 AT4+ 候选域，不在本片冒充完成。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

TRUE_EOF PRECHECK_COMPLETE
