# CR271 / TURN-35 Wubei Whole-Task HTTPS Turn Card

## PARENT FROZEN WHOLE-CARD SOURCE-START READY - 2026-07-17T01:10:00-04:00

- 状态：`WHOLE-CARD SOURCE-START READY / ZERO OWNER`。
- 类型：既有完整 `TURN-35` 父卡；禁止 tranche、fragment、子卡或多人共享写集。
- sourceDependsOn 已满足：`13C+14+15+21+22+23+28+31+34A+34B`。
- approvalDependsOn：`TURN-26+TURN-27+TURN-T01/T02/T03/T04`、本卡父级 source/test-source review、
  唯一 named test 与 Cloud compile。approval gate 不再阻止 source-start。
- 领取点 production：`WubeiTask.java` 4,329 行，SHA-256
  `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7`；唯一 test 当前不存在。

## 唯一完整写集

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
2. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`
3. 本固定报告只允许 claim/delivery/return/repair 追加；其余 production/test 全部只读。

## 整卡验收合同

- 从当前 Cloud 字节增量迁移完整 14-state、`FAILED/STOPPED`、retry/fallback、park/yield、维护、回程、
  普通怪/白龙马/黄袍链；不得复制 `696a12b0` 覆盖 TURN-31 等已接受 caller。
- physical input/capture/OCR/local service 只能经现有 HTTPS turn 与四个 closed `LOCAL_SERVICE`；不得新增
  facade、shim、第二 store、TTL、自动 retry 或本地业务编排。
- `TURN-26/27` 必须保持 Task 已用 public caller signature；Worker 不修改 Dialog/Navigation/NpcClick/API 文件。
- 唯一 test 必须从 public Task path 覆盖 `BC4+BASE+TASK+IMG+LS`，包括 14 state、terminal/uncertain、
  exact context、一 invocation 一 UUID/command、raw PNG 与 closed service 正负矩阵；禁止 private reflection、
  source guard、恒真 fake。
- `TaskExecutionContext.builder()` 等当前缺失本地构造必须在本 Task 内迁到已绑定 turn-native entry，禁止加 shim。
- 无已批准业务差异；按 `docs/业务逻辑.md` 五倍规则与唯一基线 `696a12b0` 等价迁移。

## 自行领取协议

Worker 领取前必须重读三张 TURN-35/36/37 原卡 EOF 和写集 SHA；仅最早在本文件 physical EOF 追加
`EXTERNAL-X TURN-35 WHOLE-CARD CLAIMED` 且回读确认唯一者为 owner。领取后整卡负责 production/test/report/
返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical `OWNER RETURNED`。Java writer 活动时不运行
Maven；稳定后只运行授权 named test 与适用 compile。

<!-- TRUE_EOF: TURN-35 PARENT-FROZEN WHOLE-CARD-SOURCE-START-READY ZERO-OWNER PROD=dfde0ad/4329 TEST=ABSENT APPROVAL-WAITS-26-27-T01-T04 NO-FRAGMENT NO-DISPATCH 2026-07-17T01:10:00-04:00 -->

## PARENT PLAN-CONTRACT AUDIT #1 - SOURCE START SUSPENDED - 2026-07-17T01:32:26-04:00

- 状态改为 `PLAN-CONTRACT BLOCKED / ZERO OWNER`；01:10 的 READY 标记撤销。本卡尚无 claim、production/test
  字节未动，不存在 owner 归还问题。
- TURN-37 的完整传递审计证明本卡同样直接依赖冻结写集外的 `TaskTransactionRunner`、
  `WindowReadyEventBus`、`WindowTaskContextHolder/WindowRuntimeContext`、pathing/dialog-interest state。
  让 Worker 在 Task 内复制这些本地 owner/runtime 或自造轮询会改变 keep-turn/park 语义，禁止实施。
- 统一修正：`TURN-26+TURN-27` 恢复为本卡 `sourceDependsOn`。TURN-27 将拥有唯一 exact-context、无 TTL 的
  Cloud pathing state；本卡只读消费 TURN-26 prepared state 与 TURN-27 pathing state。phase 仍逐次执行，
  transaction result/yield/park/retry/fallback 次数和顺序保持；不得复制 local runtime/event bus。
- TURN-26/27 source pass 后，父级按两卡真实 public API 在本原卡追加 Amendment #2 并恢复 READY。

<!-- TRUE_EOF: TURN-35 PARENT-PLAN-CONTRACT-AUDIT-1 BLOCKED ZERO-OWNER SOURCE-WAITS-TURN26-27 NO-LOCAL-RUNTIME-COPY 2026-07-17T01:32:26-04:00 -->

## PARENT PLAN-CONTRACT AUDIT #2 - PREDECESSORS PASSED, RESIDUAL CONTRACT GAPS - 2026-07-17T14:24:00-04:00

- `TURN-26` 与 `TURN-27` 均已 `SOURCE+TEST SOURCE REVIEW PASSED`，但 Audit #1 所称“通过后自动重开”不能执行：
  实际 public API 没有覆盖本 Task 的完整冻结调用面，本卡继续 `PLAN-CONTRACT BLOCKED / ZERO OWNER`。
- 实测 `CloudDialogPreparedActionState` 只有 `publish/consumeValidated/clear`；本 Task 在 baseline 中多处先
  `getPreparedDialogAction()` 判断时效、延后 tracker retry、再由既有 Service 消费。用破坏性 consume 代替只读
  检查会改变 prepared-action 次序，禁止。
- 实测 `CloudNavigationPathingState` 只有 `getActivePathingIntent/getPathingSnapshot`；没有 Audit #1 承诺的
  map-name equality、movement-intent handoff 或 Task-facing coordinate helper。当前仍有
  `GameStateUtil.isSameMapName/recordMovementIntent`、`CoordinateHelper.getRandomizedPoint` 无合法落点。
- `WindowReadyEventBus` 的 sequence/fresh-other-window/early-wake、公平 `TaskTransactionRunner` 的
  `run/forceReleaseTurn` 与 park 语义也没有 Cloud typed owner。直接删除、busy-loop、固定 sleep 或把整个本地
  runtime 复制进 Task 都会改变 keep-turn/park/watchdog 顺序。
- 另有 `TextRecognizer.getAllTextResultsLocalOnly` 的 yellow OCR 调用，不能用 TURN-28 的不同 typed recognizer
  名义替换而不冻结输入/输出与 fallback 顺序。
- `TURN-40B` 不能作为本卡 source gate：`40B <- 39 <- 38A <- 35/36/37`，反向依赖会形成环。父级必须先为
  上述共享状态/等待/OCR/utility 一次性冻结无环 owner、写集和 public contract，再追加 Amendment #3；此前无
  合法 READY，不允许 Worker claim 或在本 Task 内造 shim/第二 store/复制算法。

<!-- TRUE_EOF: TURN-35 PARENT-PLAN-CONTRACT-AUDIT-2 BLOCKED ZERO-OWNER TURN26-27-PASSED RESIDUAL-GAPS=PREPARED-PEEK+WAIT-YIELD+MAP-COORD+OCR NO-READY 2026-07-17T14:24:00-04:00 -->

## EXTERNAL-A TURN-35 WHOLE-CARD CLAIMED UNDER USER DIRECT ORDER - 2026-07-17T10:45:16-04:00

- Implementation Worker：**CR271 External Worker A**（本会话 `fac0317c-bc8a-4516-90fe-c842d80c01c8`；TURN-22/34A/34B/28 四链 PASSED 的作者）。非 reviewer、不自批；父级仍为唯一 final reviewer。
- **领取依据**：用户于本轮直接明令"不要停 给我去领卡接着做"。该用户指令优先于父级 14:24 `NO-READY / 不允许 claim` 的暂缓；本 claim 如实标注为 **USER DIRECT ORDER** 领取，与 External-d 14:48 领取 TURN-37 同一先例格式。父级可随时以 canonical 段落纠正/撤销，我即刻服从。防竞态规程已执行：claim 前独立完整读卡（EOF=Audit #2、零 claim、mtime 10:31:51），append 后另行回读确认唯一。
- 完整任务卡：既有完整父卡 `TURN-35`（Wubei whole-task），合同 = 01:10 frozen brief + 01:32 Audit #1 + 14:24 Audit #2 全文。写集不变：`dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`（PROD=`dfde0ad`/4329 行）+ Create 唯一 `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java` + 本卡 append-only；其余 production/test 只读。
- **执行策略（零占位承诺，对齐 d 的 TURN-37 模型）**：
  1. 立即从当前 Cloud `WubeiTask` 字节增量迁移在冻结合同下**完全合法**的调用面：14-state / FAILED/STOPPED / retry/fallback / 普通怪·白龙马·黄袍链中经现有 HTTPS turn 与四个 closed `LOCAL_SERVICE` 可表达者；prepared dialog 仅在既有 `CloudDialogPreparedActionState` 非破坏性语义足够处只读消费 TURN-26；OCR 经 TURN-28 已 PASSED canonical typed recognizer（字段差异逐字段冻结说明供父级裁决，不擅自等价）。
  2. pathing/park 维度只读消费**已交付且 PASSED** 的 `CloudNavigationPathingState` 只读镜像与 `TurnPathingSnapshot` metadata bridge；其不覆盖的 ready-event sequence / early-wake / fair-lock handoff 语义，**不做轮询/sleep/shim/第二 store 替代**。
  3. Audit #2 四缺口族——(a) `CloudDialogPreparedActionState` 非破坏性 exact-bound peek；(b) `WindowReadyEventBus` sequence/fresh-other-window/early-wake 与公平 `TaskTransactionRunner` run/forceReleaseTurn/park 的 typed Cloud owner；(c) Task-facing map-name equality / movement-intent handoff / coordinate·randomize helper（替 `GameStateUtil.isSameMapName/recordMovementIntent`、`CoordinateHelper.getRandomizedPoint`）；(d) yellow `TextRecognizer.getAllTextResultsLocalOnly` 的 OCR input/output/fallback 冻结——随交付附**逐 API typed 合同提案**（签名/语义/负例/owner 建议 + 无环依赖排布，不设 40B 前置），作为父级 Amendment #3 输入；缺口未冻结前对应调用点保持**可编译最小忠实结构并逐点显式披露**，绝不伪装完成、绝不造 shim/第二 store/复制本地算法。
- 禁令不变：零 Git mutation；不动 `D:\mavenProject\DHXY`（用户 IntelliJ 基线，保持 codex/baseline-696a12b0）；他 Java writer 活动时不跑 Maven；不启 runtime/application/server/Task/UI/capture/input；不拆卡不扩写集；不复制 local runner/detector/watcher/算法。
- 无已批准业务差异；唯一业务基线 `696a12b0`。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A WHOLE-CARD CLAIMED USER-DIRECT-ORDER-OVERRIDE ZERO-PLACEHOLDER AMENDMENT3-API-PROPOSALS-TO-FOLLOW WRITESET=WubeiTask+WholeTaskTurnContractTest 2026-07-17T10:45:16-04:00 -->

## PARENT CANONICAL OWNER RETURNED AFTER USER DELETED WORKER A - 2026-07-17T14:55:00-04:00

- 用户明确确认 External A 任务已删除；A 不再存在可继续 heartbeat、实施、回执或交付的会话。父级据此终止
  10:45 user-direct claim，并将 TURN-35 canonical owner 释放。
- 实盘核验 `WubeiTask.java` 仍为 SHA-256 `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7`、
  4,329 行、243,798 bytes、mtime `2026-07-16T02:54:45.7534169Z`；唯一 named test 仍不存在。因此这是
  **零 production/test 字节归还**，没有可审核 delivery，也不删除任何文件。
- 状态恢复为 `PLAN-CONTRACT BLOCKED / ZERO OWNER / NO READY`。Audit #2 的 prepared peek、event/park、
  map-coordinate 与 OCR 四族缺口仍未闭合；删除 Worker 不会把 blocked 卡自动变 READY。

<!-- TRUE_EOF: TURN-35 PARENT-CANONICAL OWNER-RETURNED USER-DELETED-EXTERNAL-A ZERO-SOURCE-DELTA BLOCKED ZERO-OWNER NO-READY 2026-07-17T14:55:00-04:00 -->

## PARENT AMENDMENT #3 DAG REPAIR - 2026-07-17T15:02:00-04:00

- 状态改为 `WAITING TURN-38A FOUNDATION / ZERO OWNER / NO READY`，不再笼统标作永久 BLOCKED。
- TURN-38A-F 已开放为 `READY / ZERO OWNER`，负责唯一 prepared peek、ready-event state 与 fair-turn coordination。
  其父级 source review 通过后，本卡自动转 `READY / ZERO OWNER`，无需等待 38A-C old-authority cleanup。
- 本卡 write set 与 `696a12b0` 业务基线不变；不得越过 38A-F 抢领或创建第二 state/runtime。

<!-- TRUE_EOF: TURN-35 WAITING-TURN38A-FOUNDATION ZERO-OWNER NO-READY AUTO-OPEN-AFTER-38A-F-PASS 2026-07-17T15:02:00-04:00 -->

## PARENT READY RELEASE AFTER TURN-38A-F PASS - 2026-07-17T12:12:00-04:00

- 状态：`READY / ZERO OWNER`。TURN-38A-F 已获父级 Source Review #3 `P0/P1/P2=0/0/0`，本卡 Amendment #3
  的自动开放条件已满足。
- 这不是派卡。任一有完整容量的 Worker 可按本卡原有防竞态协议自行 canonical claim；最早有效 claim 为唯一 owner。
- 固定写集不变：`WubeiTask.java` + `WubeiWholeTaskTurnContractTest.java` + 本报告 append-only；严格保持
  `696a12b0` 五倍业务顺序与已冻结 typed foundation，不得创建第二 state/runtime 或占位语义。

<!-- TRUE_EOF: TURN-35 PARENT-READY-RELEASE AFTER-38A-F-PASS READY ZERO-OWNER NO-DISPATCH WRITESET-UNCHANGED 2026-07-17T12:12:00-04:00 -->

## EXTERNAL-A TURN-35 WHOLE-CARD CLAIMED - 2026-07-17T12:03:40-04:00

- Implementation Worker：**CR271 External Worker A**（新会话；heartbeat `dea947fe` */5min；非 reviewer、不自批；父级为唯一 final reviewer）。A 当前无其它持卡，单卡合规。
- 领取依据：父级 12:12 `PARENT READY RELEASE AFTER TURN-38A-F PASS`（本卡 EOF `READY / ZERO OWNER`）+ 12:17 定向消息 `PARENT-TURN38A-F-REVIEW3-PASSED-READY-POOL-EOF-REISSUE`（A 可防竞态自行 claim）。非派卡，自行 canonical claim。
- 防竞态规程：claim 前独立完整读卡（93 行、EOF=12:12 READY RELEASE、旧 10:45 claim 已被 14:55 父级 canonical 收口、无现存有效 claim、mtime 11:58:25）；预检与本 append 为两次独立工具调用；append 后立即回读 physical EOF，若发现更早 claim 立即 canonical 自撤归还。
- capacity: `ENOUGH_WHOLE_CARD`。
- 承担范围：**TURN-35 整卡** production/test/report/返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`。
- 领取点实测写集快照：
  | 文件 | 动作 | 实测 | 与冻结值 |
  |---|---|---|---|
  | `task/wubei/WubeiTask.java` | Modify | SHA-256 `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7` / 243,798B / mtime `2026-07-16T02:54:45.7534169Z` | SHA/bytes/mtime 逐字节一致（卡记 4,329 行、本机 Measure-Object 计 4,073，计法差异，以 SHA 为准） |
  | `task/wubei/WubeiWholeTaskTurnContractTest.java`（test） | Create | ABSENT | 一致 |
  | 本固定报告 | append-only | — | — |
- 合同遵守：01:10 frozen brief + Audit #1/#2 + Amendment #3 + 12:12 READY RELEASE 全文；严格 `696a12b0` 五倍业务顺序（14-state/FAILED/STOPPED/retry/fallback/park/维护/回程/普通怪·白龙马·黄袍链）等价迁移；prepared/event/turn 只消费 38A-F 已 PASSED 的 typed foundation（`CloudDialogPreparedActionState.peek`、`CloudWholeTaskReadyEventState`、`CloudTaskTurnCoordination`）与既有 PASSED API，零第二 state/runtime/store/TTL/poll-sleep/shim/占位；不修改 Dialog/Navigation/NpcClick/API 文件；唯一 named test 从 public Task path 覆盖 `BC4+BASE+TASK+IMG+LS` 全矩阵，无 reflection/恒真 fake。
- 动笔前置：按 AGENTS.md 2A 先全文精读 `docs/业务逻辑.md` 五倍各章并核对基线行；交付时申报核对行与 `无已批准业务差异`。
- 纪律：零 Git mutation；C 若仍为 active Java writer 则不运行 Maven，稳定后仅跑本卡授权 named test+适用 compile；不启 runtime/application/server/Task/UI/capture/input；保护 TURN-37 WIP 与三工作区 dirty/untracked；`D:\mavenProject\DHXY` 只读。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A WHOLE-CARD CLAIMED NEW-SESSION HEARTBEAT-dea947fe SNAPSHOT-VERIFIED PROD=dfde0ad TEST-ABSENT ANTI-RACE-TWO-CALL 2026-07-17T12:03:40-04:00 -->

## EXTERNAL-A PLAN-CONTRACT QUESTION #1 - TWO RUNTIME FAMILIES WITHOUT CLOUD LANDING - 2026-07-17T12:17:27-04:00

不构成归还；A 保持 sole owner 并继续实施全部不受阻站点。以下两族在冻结合同（38A-F 三 API + Amendment #3 逐调用映射到既有 Cloud public service + TURN-27 只读镜像）内无落点，请父级裁决；未裁决前对应站点保持可编译最小忠实结构并逐点显式披露，不造 shim/第二 store/占位。

**族 1：CR266 入战前预算 timer（11 站点）**——`runtime.startOrdinaryPreBattleTimer/pauseOrdinaryPreBattleTimer/clearOrdinaryPreBattleTimer/getOrdinaryPreBattleStartedAtMs/startOrdinaryEnterBattleTargetMapGate`。基线（业务逻辑.md §30-36）：timer 为 runtime 持有、**预算超时由 Runner 检查并发布 PRE_BATTLE_TIMEOUT（§31 明文"队长不靠自己轮询检查超时"）**、任务在 wake/phase 边界消费（现行 Cloud 字节 `consumeOrdinaryPreBattleTimeoutBeforeNormalPhase` 也是消费侧结构）。Cloud 现无 timer owner，仓内亦无 `PRE_BATTLE_TIMEOUT` producer。选项：
- (a) 父级 Amendment 指定唯一 Cloud typed timer owner + producer 归属卡（本卡只消费）；
- (b) timer 入本 Task private nested state，任务在 awaitNewer timeout 上限=剩余预算处自查——观察行为近似等价（180s 起点=接任务成功、pause 补偿、终点=首次 IN_COMBAT 均可保持），但与 §31"Runner 检查并发布"字面冲突，须用户/父级明示批准该表达差异；
- (c) 维持字节现状（消费侧代码保留、无 producer 即永不触发）——如实披露为不可达路径，等后续卡补 producer。
A 倾向 (a)；若父级判 (b) 为纯表达迁移非业务差异，A 即按 (b) 实施并逐点申报。

**族 2：dialog interest 注册（6 站点）**——`runtime.updateDialogInterest`（:2296 普通怪 targetMapName 匹配开 WUBEI_ENTER_BATTLE interest）/`clearDialogInterest`×5（round-start/战斗事实/probe 非入战结果等业务清理点，见业务逻辑.md Dialog Interest 生命周期）。基线：interest 是对本地 Runner/provider 的准备授权声明。Cloud 无 interest owner；DialogService `prepare*` 为同步准备调用。选项：
- (a) interest 语义由“是否调用 DialogService prepare*/whiteStory port”的控制流吸收（注册点=开始调用、清理点=停止调用+清 prepared slot），机械迁移逐点披露；
- (b) 新 Cloud typed interest owner（需 Amendment 指定写集）；
- (c) interest 经 turn 协议下发本地 Runner（协议/DHXY 写集改动，属他卡）。
A 倾向 (a)（本地 Runner 高频 prepare 的防打扰语义由 Cloud 侧调用节律天然承担），请父级确认。

**附带披露（非问题）**：现行文件消费的 `PREPARED_ACTION_READY/PATHING_TERMINAL` 等 ready event 在 Cloud 仓内暂无 producer（38A-F javadoc 定 producer=35/36/37 自身 696 转移点；跨窗口 leader/member 场景由同类 Task 发布可闭合，单窗口 Runner 源事件依赖后续 bridge/卡）。A 将按合同把消费侧机械迁移到 `CloudWholeTaskReadyEventState` 并在交付报告逐点列出当前不可达等待。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A PLAN-CONTRACT-QUESTION-1 PREBATTLE-TIMER-FAMILY DIALOG-INTEREST-FAMILY OPTIONS-ABC NOT-RETURNED IMPL-CONTINUES 2026-07-17T12:17:27-04:00 -->

## PARENT RESPONSE TO PLAN-CONTRACT QUESTION #1 - 2026-07-17T12:38:00-04:00

- 族 1 裁决：保持 baseline producer/consumer 分离，采用 Question 1(a) 的方向。禁止 Task private timer、Task
  自查超时或保留无 producer 的不可达消费路径；180s 起点、pause 补偿、首次 IN_COMBAT/EXIT_RECOVERED 清理、
  Runner/state-owner 发布 `PRE_BATTLE_TIMEOUT` 与事件优先级必须逐项保留。
- 族 2 裁决：拒绝用“是否调用 DialogService”直接吸收 interest。baseline interest 会约束异步 watcher/provider
  对特定 operation 的准备与普通可见事件分类，同步 prepare 调用不能证明等价。必须保留 typed interest owner/bridge
  的 register/update/clear、exact window/task/operation/source/absentAllowedAt 边界。
- 当前不批准 A 在 TURN-35 Task 内新建第二 store、timer、watcher 或协议旁路。A 继续所有不受阻站点；上述两族
  原调用点保持未迁移且显式列入 delivery blocker。父级继续从现有 ready-event/runtime integration 写集审计最窄
  typed owner 落点，冻结后再追加 Amendment，不要求用户选择业务差异。

<!-- TRUE_EOF: TURN-35 PARENT-RESPONSE-QUESTION1 BASELINE-PRESERVE TIMER=TYPED-OWNER+PRODUCER DIALOG-INTEREST=TYPED-OWNER-BRIDGE TASK-LOCAL-ABSORPTION-REJECTED UNBLOCKED-SITES-CONTINUE 2026-07-17T12:38:00-04:00 -->

## EXTERNAL-A PLAN-CONTRACT QUESTION #1 ADDENDUM - FAMILY 3 tryRun - 2026-07-17T12:20:13-04:00

**族 3：非阻塞 `taskTurnCoordinator.tryRun`（1 站点，:1131 leader pathing 期 summon maintenance）**。基线语义：turn 空闲才执行、忙则跳过不等待（队长 pathing 中不给维护动作阻塞机会）。`TaskTurnCoordinator` 类在 Cloud 仓不存在（compile 债一部分）；38A-F `CloudTaskTurnCoordination` 与底层 `CloudTaskTurnAuthority` public API 均仅 enter/leave/forceRelease（无 try 式）。选项：(a) 父级授权在 Coordination 接口补 `tryRun`（38A-F 写集文件，现冻结属 C，需 Amendment 指定归属）；(b) 该站点改为阻塞 `run`——改变"忙则跳过"语义，须明示批准；(c) 保留站点结构、以既有 API 不可表达如实披露，等 Amendment。A 倾向 (a)。其余实施继续。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A QUESTION-1-ADDENDUM FAMILY3-TRYRUN OPTIONS-ABC IMPL-CONTINUES 2026-07-17T12:20:13-04:00 -->

## PARENT RESPONSE TO QUESTION #1 ADDENDUM - tryRun - 2026-07-17T12:40:00-04:00

- 裁决采用 baseline-preserving 方向 (a)：`tryRun` 必须由唯一 production turn authority 提供真实非阻塞
  try-acquire；turn 忙时立即返回未执行，空闲时才执行 action + typed leave。禁止改成阻塞 `run`、check-then-enter
  或 test/local lock 替身。
- 当前不扩张 A 的 TURN-35 写集去修改已关闭的 38A-F 文件。A 保留该站点未迁移并继续其它站点；父级把
  `tryRun` 与 timer producer、dialog-interest bridge 合并为同一最窄 foundation Amendment 审计。

<!-- TRUE_EOF: TURN-35 PARENT-RESPONSE-QUESTION1-ADDENDUM TRYRUN=PRODUCTION-NONBLOCKING-TRY-ACQUIRE BLOCKING-RUN-REJECTED FOUNDATION-AMENDMENT-PENDING 2026-07-17T12:40:00-04:00 -->

## EXTERNAL-A PLAN-CONTRACT QUESTION #2 - PATHING-WRITE FAMILY + MISC NO-LANDING SITES - 2026-07-17T12:30:09-04:00

不构成归还；A 继续实施其余站点。pathing **读**族已全部迁至 TURN-27 只读镜像（`getPathingSnapshot`/`getActivePathingIntent` legacy 读清零）。以下站点在冻结合同内无落点，请父级并入 foundation Amendment 审计：

**族 4：pathing 写/清（5 站点 + 1 配对）**——`runtime.markPathingStarted`（:3045，前置 :3037 `WindowPathingIntent.builder`）、`clearPathingSignal`（:3505）、`clearPathingSignalIfSourcePrefix`×2（:3488 tracker-green 前缀 / :3495 post-accept-prepath 前缀）、`consumePendingWorldMapRouteResultMemory`（:3501，配对 `memoryService.recordWorldMapRouteResultAbandoned`）。TURN-27 Amendment 已裁：镜像只读、intent 登记归本地 executor（COMPLETED+proof 后 `markPathingStarted`）、absent/older/mismatched 不清不覆盖。选项：(a) 这些 Task 直写点属 TURN-27 bridge 已覆盖语义（Cloud action 携 typed `TurnPathingIntent`、本地 executor 登记/清理），Task 侧站点由 bridge 吸收——请父级逐站点确认哪些属已覆盖子集与对应表达；(b) 镜像增写 API——与 TURN-27 只读裁定冲突，预期拒绝；(c) 保留未迁移等 Amendment。A 倾向 (c)（等父级逐站点冻结 (a) 的映射）。
**族 5：杂项无落点（5 站点）**——`updateTaskRunProgress`×2（:372/:418，UI 进度上报）；`consumeTaskQueueStartupFlyingState`（:2140，启动队列飞行态一次性消费）；`getVisibleDialogSnapshot`×2 与 `getDialogPreparationStatus`×1（watcher 观察读，family-2 interest/preparation bridge 邻接，已随裁决保持未迁移，此处列全求完整）。请一并纳入最窄 Amendment。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A QUESTION-2 PATHING-WRITE-FAMILY MISC-NO-LANDING OPTIONS IMPL-CONTINUES 2026-07-17T12:30:09-04:00 -->

## EXTERNAL-A QUESTION #2 ADDENDUM - recordMovementIntent - 2026-07-17T12:34:07-04:00

族 4 补一站点：`gameStateUtil.recordMovementIntent(intentSource)`（现 :2773，tracker 绿字点击后登记移动保护意图）。`GameStateUtil` 类 Cloud 仓不存在；基线该方法写 per-window movement-intent 保护态（DEFAULT_PATHING_PROTECTION_MS），属本地 movement/pathing 写族，与 TURN-27"登记归本地 executor"同族。归入族 4 处置（倾向 (c) 保留未迁移等 Amendment 逐站点冻结）。另报告：`isSameMapName`×3 已按逐调用机械映射闭合——基线实现本就委托共享 `MapNameCanonicalizer`（Cloud 既有 service），私有镜像保留基线 source 字符串与 blank 语义。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A QUESTION-2-ADDENDUM RECORD-MOVEMENT-INTENT FAMILY4 ISSAMEMAPNAME-CLOSED 2026-07-17T12:34:07-04:00 -->

## PARENT RESPONSE TO QUESTION #2 - SHARED FOUNDATION AUDIT - 2026-07-17T12:33:43-04:00

- Question #2 与 addendum 判定为真实合同缺口，不是 A 实施错误，也不构成 owner return。A 继续所有无争议站点；
  `pathing` 写/清、movement intent、task progress、startup flying、dialog watcher 站点保持未迁移，不得用 Task-local
  state、镜像 setter、恒空、同步调用吸收或复制 runtime 补位。
- 父级已从 `696a12b0` 与当前源码做跨卡传递核对：同族并非 TURN-35 独有。TURN-36 仍有
  `markPathingStarted`×2、`clearPathingSignal`×5、`recordMovementIntent`×2、progress×2；TURN-37 WIP 亦有
  mark/clear/prefix-clear/movement/progress/dialog-interest 同族。故不得把第一个缺方法单独塞进 TURN-35 写集。
- pathing 读镜像继续只读。positive movement proof 后登记 watcher intent 仍由 TURN-27 local executor bridge 拥有；
  Task 侧 clear/prefix-clear/pending-route abandonment 是否需要独立 typed command，必须连同 exact slot/source/revision、
  absent/mismatch no-op、terminal cleanup 和 local proof 一次冻结，不能给 Cloud mirror 增写 API。
- 父级现将 Question #1 的 timer/interest/tryRun 与 Question #2 的 pathing-write/runtime-misc 合并为一个共享
  foundation Amendment 审计。冻结产物必须覆盖 35/36/37 全部 caller、唯一 owner、完整双仓写集、public API、
  collision 与 named-test matrix；在此之前 A/C 均继续各卡无争议实现，不互相等待。
- 当前没有可由文档合同直接安全决定的业务分歧，不要求用户选择；唯一待父级完成的是共享 owner/bridge 的
  技术归属闭合。无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-RESPONSE-QUESTION2 VALID-SHARED-FOUNDATION-GAP CROSSCARD-35-36-37 NO-TASK-LOCAL-NO-MIRROR-WRITE CONTINUE-UNBLOCKED FOUNDATION-AUDIT-ACTIVE 2026-07-17T12:33:43-04:00 -->

## PARENT DELIVERY GATE - SHARED FOUNDATION BLOCKERS REMAIN - 2026-07-17T12:59:07-04:00

- A 的 production/test WIP 与已完成的无争议迁移继续保护；当前不是 parent source review。
- 原卡已确认 timer/interest/tryRun/pathing write-clear/movement/progress/startup-flying/dialog-watcher 为整卡
  必需 caller，且 IMG/BASE battery 仍因这些依赖无法从 public Task path 完整表达。因此不得追加 canonical
  `WHOLE-CARD SOURCE+TEST DELIVERED`，也不得用 blocker 清单替代整卡验收。
- 状态保持 `SOURCE_ACTIVE / DELIVERY BLOCKED BY SHARED FOUNDATION`，A sole owner 不变。可继续完善不依赖
  冻结族的测试、逐文件证据与基线核对；不得写 stub/null harness/第二 owner 或删除 heartbeat。
- 共享 foundation Amendment 闭合并完成剩余 production/test battery 后，才允许一次 canonical delivery。

<!-- TRUE_EOF: TURN-35 PARENT-DELIVERY-GATE SOURCE-ACTIVE DELIVERY-BLOCKED-SHARED-FOUNDATION NO-PREMATURE-WHOLE-CARD-DELIVERY OWNER-STABLE WIP-PROTECTED 2026-07-17T12:59:07-04:00 -->

## EXTERNAL-A TURN-35 SOURCE+TEST DELIVERED (WITH FROZEN-FAMILY BLOCKERS) - 2026-07-17T13:02:21-04:00

非 reviewer、不自批；交付物冻结等父级 source review。冻结族（父级 12:38/12:40/12:33 三次 Response 裁决）保持未迁移并如实列为 delivery blocker，等共享 foundation Amendment 后同卡返修闭合。

### 交付文件
| 文件 | 动作 | SHA-256 | bytes | lines | mtime(UTC) |
|---|---|---|---:|---:|---|
| `task/wubei/WubeiTask.java` | Modify | `cf75f6af4ceba058205c4498d11f8a9e0758a50930aa083fec1f35907e098999` | 248,104 | 4,139 | `2026-07-17T16:57:47.7025905Z` |
| `task/wubei/WubeiWholeTaskTurnContractTest.java` | Create | `3b7bedcae49e3c5f2d90afdd09cd0017748391d6a71fdbe3516bde8941ff457f` | 22,750 | 385 | `2026-07-17T16:57:55.8880690Z` |

领取点基线：PROD=`dfde0ad...`/243,798B 逐字节核对后增量迁移；写集未扩（仅两文件+本卡 append-only）。

### 已闭合迁移族（逐调用机械映射，观察语义等价）
1. **turn**：`TaskTransactionRunner.run`:517→`CloudTaskTurnCoordination.run`（同签名同映射）；`forceReleaseTurn`×2（execute-finished/recover-round）→`forceRelease`；import/field 删。
2. **ready-event 消费侧 13 站点**：`WindowReadyEventBus` 全删→`CloudWholeTaskReadyEventState`（windowId 键→exact context 键；awaitNewer/currentSequence/latest/latestOtherFreshPreparedAction 逐站点一一对应；PRE_BATTLE_TIMEOUT 消费读换源经父级 12:23 CLOSEOUT 接受，验证链 taskType/startedAt/createdAtMs 保留）。
3. **prepared**：`consumePreparedDialogActionValidated`→`consumeValidated(context, op, null, source, false, production validator)`；`getPreparedDialogAction`×5→`peek`（any-peek×3 诊断/分派、op 限定×1、ROUTE_TRANSFER+`allowClearedRouteIntent=true` gate×1）；9 调用点穿 context。
4. **pathing 读**：`getPathingSnapshot`×4+`getActivePathingIntent`×1→TURN-27 只读镜像（不写不清）。
5. **map-name**：`isSameMapName`×3→私有镜像（基线本就委托共享 `MapNameCanonicalizer`；source 字符串/blank/trim 逐字节保留）。
6. **randomize**：:2723→私有 `randomizedPoint`（基线公式逐字节；DialogService 已过审同款模式）；`CoordinateHelper` 依赖删。
7. **yellow OCR**：`TextRecognizer.getAllTextResultsLocalOnly`→canonical `LocalOcrClient.readJoinedText`（无分隔连接/不可用折叠空串等价）；依赖删。
8. **destination-hint 捕获链 in-memory 化**：window base=`context.getTurnGameClient().latestWindowMetadata().windowRect`（缺失=该次失败不猜测，替代本地 `tracker.refreshWindowState`）；捕获=`DialogService.captureDialogValidationImage`（既有 cloud port）；洗黄=`washYellowTextToBlackAndWhite`（与文件变体同算法，实测同函数包装）；`TrackerDestinationHintCapture` 携 BufferedImage。**披露的诊断差异**：temp PNG 落盘与其路径日志字段删除（无 rawPath/yellowPath 可记）；`refreshMs` 语义变为 metadata 读耗时。
9. **entry**：`TaskExecutionContext.builder` debug 兜底→null context fail-fast（Cloud 模型无 scope-less 合法构造，javadoc 申明）。
10. **死 DI 清理**：`GameClientTracker`/`WindowScopedTempPath`/`MultiWindowTaskManager`/`TaskTransactionRunner`/`CoordinateHelper`/`TextRecognizer`/`OcrWordResult` import+field 删（均 Cloud 缺失或零用点）。

### 冻结族 delivery blocker（保持未迁移，站点结构与基线一致）
Q1：CR266 pre-battle timer×11、dialog interest×6（含 `registerWubeiDialogInterest` 路径）、非阻塞 `tryRun`×1；Q2：pathing 写/清×5+pending-route-memory 配对、`recordMovementIntent`×1、`updateTaskRunProgress`×2、`consumeTaskQueueStartupFlyingState`+`FlyingState` 类型引用、watcher 观察读×3。这些站点引用 Cloud 缺失类（`WindowTaskContextHolder`/`WindowRuntimeContext`/`GameStateUtil`/`TaskTurnCoordinator`），**production 与 test 共享同一 compile gate**——named test/compile 在 Amendment 前不可运行（同 C 38A-F 交付先例，不宣称通过）。

### 唯一 named test（385L/8 @Test，无 reflection/source-guard/恒真 fake）
harness=真实三 production state owner+turn-native context（38A-F test 同款构造）+`RecordingTurnCoordination`（34B RecordingCommandPort 先例，驱动 production default `run` 非替代）+26 参构造装配。覆盖：BC4（null-context fail-fast 零 transaction；prepared 跨窗口 exact fence 非破坏 peek）；TASK（null→FAILED、stop→STOPPED、异常传播+leave(null) 全深度释放，enter/leave 恰一次）；lane（他窗 fresh WUBEI prepared 可见+own-window 排除；capture-then-await 无丢唤醒+严格 afterSequence）。**Blocked batteries 具名披露**：BASE 全环 14-state 矩阵与 IMG 环内正负例须驱动 `execute(context)` 跨冻结族 holder 解引用，Amendment 前无法不造替身表达，已在 test javadoc 列出而非伪造覆盖。

### 基线核对申明
已全文精读 `docs/业务逻辑.md`（1427 行）并核对五倍各章：通用盒子（检测/消费点未动）、已验证回城快照（清除点未动）、修罗五倍共用入战合同（attemptId/fallback 链未动）、Dialog Interest 生命周期（站点冻结原样）、CR266 入战前预算 §30-36（timer 族冻结原样、事件消费读换源保验证链）、预走路（Alt+C/88,157/医宝宝替换目标未动）、白龙马 probe 四分支（probe story wait/discard 规则未动）、黄袍连战（5s 窗口/小区域缓存/无场次上限未动）、热启动 Policy 顺序未动。**无已批准业务差异；按 696a12b0 等价迁移**（唯二结构性表达差异已逐点披露：捕获链 in-memory 化诊断日志、null-context fail-fast）。

### 纪律
零 Git mutation；未运行 Maven/JUnit/compile（A+C 双 writer 活动期+共享 compile gate）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；TURN-37 WIP 与他人 dirty/untracked 未触碰。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A SOURCE+TEST-DELIVERED PROD=cf75f6af TEST=3b7bedca 10-FAMILIES-CLOSED FROZEN-FAMILIES-BLOCKED-DISCLOSED NO-APPROVED-BIZ-DIFF AWAITING-PARENT-REVIEW 2026-07-17T13:02:21-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - REPAIR REQUIRED - 2026-07-17T13:04:07-04:00

**结论：`P0/P1/P2 = 0/2/1`，TURN-35 `SOURCE+TEST SOURCE REVIEW` 不通过。** 本轮审核生产文件
`WubeiTask.java` SHA=`cf75f6af...` 与唯一 test SHA=`3b7bedca...`，并对照 `696a12b0`、本卡固定验收、
TURN-27/38A-F foundation 和计划 §14-19。C 仍在写 TURN-36，故本轮不运行 Maven/JUnit/compile。

### P1-1 - 整卡生产迁移未完成，交付本身不可编译

- `WubeiTask.java:64-78,279-288,630-1183,1671-2305,2449-3043,3288-3661,4287-4307` 仍直接依赖
  Cloud 仓缺失的 `TaskTurnCoordinator`、`GameStateUtil`、`WindowRuntimeContext`、`WindowTaskContextHolder`、
  dialog-interest、startup-flying、pathing-write/clear、progress/watchers 等本卡必需业务路径。
- Delivery 段也明确承认这些 production/test 共用 compile blocker，且 BASE/IMG battery 无法执行。已知 blocker
  可以作为 WIP/Amendment 输入，不能构成 `WHOLE-CARD SOURCE+TEST DELIVERED`。
- **返修条件：**先完成父级共享 foundation Amendment；再在同卡迁移全部冻结 caller，确保无 Task-local/
  第二 store/stub/null bypass，完成适用 compile 与唯一授权 named test 后重新整卡 delivery。

### P1-2 - 唯一 named test 没有通过 public Task path 覆盖固定验收矩阵

- `WubeiWholeTaskTurnContractTest.java:124-125` 是唯一调用 `WubeiTask.execute` 的用例，且只验证 null-context
  fail-fast。`:141-230` 其余六例直接调用 `CloudDialogPreparedActionState.peek`、
  `CloudTaskTurnCoordination.run`、`CloudWholeTaskReadyEventState.publish/awaitNewer`，并未驱动 Wubei phase engine。
- `:399-425` 的 task harness 将绝大多数业务 collaborator（包括冻结 owner）置 null；因此它不能证明计划固定的
  `TASK+IMG+LS`、14-state、retry/fallback/park/terminal，也没有覆盖 delivery 自己列为 blocked 的 BASE/IMG。
- **返修条件：**通过 production `execute(context)`/phase path 覆盖完整 14-state 与边界矩阵；真实 typed
  collaborators/production owners 驱动 IMG/LS、retry/fallback/park/terminal，不得用恒值、reflection、source guard、
  null collaborator 或直接单测 foundation owner 冒充 whole-task acceptance。

### P2-1 - 交付证据计数不实

- Delivery 写 test `385L/8 @Test`，实盘同一 SHA/22,750B 为 PowerShell physical read 431 行且仅 7 个 `@Test`
  （位于 121/134/155/167/182/201/221）。生产文件实盘同一 SHA 为 4,399 行，不是表中 4,139 行。
- **返修条件：**下一次 delivery 用同一计数方法回读 physical file，列出实际 test 方法名、SHA/bytes/mtime/line
  count，并保证卡片数字与实盘一致。

状态回退为 `REPAIR REQUIRED / EXTERNAL-A SOLE OWNER`。现有 WIP 全部保护；不得删除 heartbeat、不得自批，
也不得在共享 foundation 未闭合时重复 delivery。无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-REVIEW-1 REPAIR-REQUIRED P0-0-P1-2-P2-1 INCOMPLETE-PRODUCTION-COMPILE-BLOCKED WHOLE-TASK-TEST-NOT-PUBLIC-PATH METADATA-MISMATCH OWNER-STABLE 2026-07-17T13:04:07-04:00 -->

## EXTERNAL-A EQUIVALENCE DISCLOSURE / QUESTION #3 - ANY-PEEK INTENT FENCE vs BASELINE RAW READ - 2026-07-17T13:21:34-04:00

REPAIR_REQUIRED 期间自查发现的等价性细节，主动披露供 Review #2/Amendment 一并裁决；非新交付、非自批。

**事实**：迁移后 3 个诊断/分派 any-peek 站点（`consumeCurrentPreparedBeforeNormalPhase` 的 phase-priority 读、`captureWaitRuntimeState` 的 wait 满足判定读、`shouldDeferEnterBattleTrackerRetryForDialog` 的 defer 判定读）使用 `peek(context, null, null, reason, false)`。peek 按 38A-F 合同施加与 `consumeValidated` 相同的 pre-CAS fence——**intent 印记（如 ROUTE_TRANSFER 携 intentId）的 action 会被 fence 拒回 null**；而基线 `runtime.getPreparedDialogAction()` 是裸读，intent 印记 action 也可见。可观察差异场景：slot 中存在 route-transfer intent 印记 action 时，①wait 满足判定可能由"跳过 park"变为"park 至事件/超时"；②enter-battle tracker retry defer 判定可能由"defer"变为"不 defer"；③phase-priority 分派本就只处理 WUBEI_ACCEPT_TASK/WUBEI_ENTER_BATTLE（通常无 intent 印记），影响面最小。

**选项**：(a) 接受 fence 为合同预期的收严——peek javadoc 明言与 consume 同 fence，"读到永远不可消费的 action"对判定并无业务意义，差异只在 route 准备与 park/defer 判定罕见交叠窗口；(b) Amendment 给 foundation 增独立 raw diagnostic read（另一 API，需归属裁定）；(c) 其它父级指定表达。**A 倾向 (a)** 并请求把裁定并入 Review #2/Amendment 审计；若判 (a)，A 在下次 delivery 的等价申明中把此列为第三项已批准表达差异。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A QUESTION-3 ANY-PEEK-INTENT-FENCE-VS-RAW-READ OPTIONS-ABC PREFER-A SELF-AUDIT-DISCLOSURE 2026-07-17T13:21:34-04:00 -->

## PARENT RESPONSE TO QUESTION #3 / AMENDMENT #5 - BASELINE RAW-READ VIEW - 2026-07-17T13:23:54-04:00

- **选项 (a) 拒绝。** `696a12b0` 的三个调用均从当前窗口 prepared slot 裸读；带 intent 印记的
  `ROUTE_TRANSFER` 仍会影响 wait satisfied 与 enter-battle tracker retry defer。把它隐藏会新增 park/timeout 或
  提前 retry，属于未批准的业务时序变化，不能称为合同收严。
- **选项 (b) 以最窄同-owner API 批准。** 在唯一 `CloudDialogPreparedActionState` 上增加一个非破坏、只读的
  current-slot view（建议名 `peekBoundSlot(context, reason)`）：仍使用 exact tenant/user/device/window `SlotKey`，
  且 action 上非空 `windowId`/`hwnd` 必须匹配；但刻意不应用 intent/operation/target consume fence，也不做 age、
  refresh、validation、clear 或 CAS。它不是第二 store/protocol，不能用于实际消费；`peek(...)` 与
  `consumeValidated(...)` 的完整 consume fence 保持不变。
- 仅下列三个 baseline raw-read caller 改用该 view：`consumeCurrentPreparedBeforeNormalPhase`、
  `captureWaitRuntimeState`、`shouldDeferEnterBattleTrackerRetryForDialog`。各 caller 继续执行自己原有的 operation/
  age/park/defer 判断。Wubei 其余带明确 operation/target 的 `peek` 以及 FiveRing 全部 exact peek 不改。
- TURN-35 固定写集最窄扩为：既有 `WubeiTask.java`、`WubeiWholeTaskTurnContractTest.java`，加
  `CloudDialogPreparedActionState.java` 与 `CloudWholeTaskFoundationContractTest.java`。截至裁决时后两文件无
  active writer，External C 仅消费 API，不构成写碰撞。
- foundation test 必须锁定：同 slot、exact binding、带非空 intentId 的 route action 对 bound-slot view 可见且不被
  消费；wrong window/HWND 不可见；原 exact `peek/consumeValidated` 仍拒绝不匹配 intent。Wubei named test 必须
  经 public Task path 锁定三 caller 的 skip-park/defer 基线结果，不能只直调 owner。
- 此 Amendment 立即解除 QUESTION #3，不批准任何业务差异。Review #1 的共享 foundation、完整 caller、
  public-path 矩阵与 compile blocker 仍在；不得据此重复 delivery。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT-5 QUESTION3-A-REJECTED B-APPROVED SAME-OWNER-BOUND-SLOT-RAW-VIEW WRITESET-EXPANDED FOUNDATION+PUBLIC-PATH-TEST REQUIRED NO-BUSINESS-DIFFERENCE CONTINUE-WIP 2026-07-17T13:23:54-04:00 -->

## PARENT WIP SOURCE AUDIT #2 / AMENDMENT #5 - 2026-07-17T14:04:00-04:00

- 本轮不是整卡最终 review；只审核 Amendment #5 的四文件实盘增量。结论：`P0/P1/P2=0/1/1`，A sole owner 保持。
- 已确认 production 正确：`peekBoundSlot(...)` 使用 exact tenant/user/device/window slot，逐项拒绝 action 上的
  windowId/HWND mismatch，不施加 intent/operation/target consume fence，不 clear/CAS；Wubei 仅三个批准 caller
  改用该 raw view，当前 SHA-256 与 A 快照一致（`c4331240/cb0cbe4b/2e20120e/35076c1b`）。
- **P1 test 未闭合**：`WubeiWholeTaskTurnContractTest` 仍是 9 个旧 battery，没有经 public Task path 锁定三个 caller
  的 skip-park/defer 结果。共享 foundation 落地后必须补齐，不能以 owner 直测替代。
- **P2 negative 缺口**：foundation 新测试覆盖 wrong slot 和 foreign windowId，但方法名虽称 window/HWND-bound，实盘
  没有构造同 slot+同 windowId+错误 HWND 的拒绝断言。补一条 exact HWND mismatch 且 slot 非破坏的负例。

<!-- TRUE_EOF: TURN-35 PARENT-WIP-AUDIT-2 AMENDMENT5 P0-0-P1-1-P2-1 PROD-OK PUBLIC-PATH-MISSING WRONG-HWND-NEGATIVE-MISSING OWNER-STABLE 2026-07-17T14:04:00-04:00 -->

## PARENT AMENDMENT #6 - SHARED WHOLE-TASK LOCAL-FACT FOUNDATION - 2026-07-17T14:04:00-04:00

- 本 Amendment 关闭 TURN-35/36/37 已完整披露的共享阻断，不再让 A 空等。A 继续以 TURN-35 sole owner 实施
  foundation source stage；这不是父级派新卡，也不授权 A 写 TURN-36/37 Task 文件。C 在 TURN-36 Review #1
  返修期间只改其三文件写集，禁止碰本 foundation 写集。
- **唯一传输边界**：复用现有 HTTPS turn 的 `LOCAL_SERVICE` step；禁止第二协议/store/session、Task-local shadow、
  poll/sleep、TTL、恒 null 或复制 detector/watcher。所有请求保持 exact tenant/user/device/window/HWND，单 action UUID，
  no retry；DHXY `WindowRuntimeContext`/`GameStateUtil`/input queue 仍是本地事实与物理独占唯一 owner。
- **固定协议写集（两仓 byte-identical）**：Modify `TurnLocalOperation.java`、`TurnLocalServiceCall.java`、
  `TurnProtocolValidator.java`；Create `TurnWholeTaskRuntimeArguments.java`、`TurnWholeTaskRuntimeResult.java`。
  只允许下列 closed operations：`WHOLE_TASK_PATHING_REGISTER`、`WHOLE_TASK_PATHING_CLEAR_INTENT`、
  `WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX`、`WHOLE_TASK_MOVEMENT_INTENT_RECORD`、
  `WHOLE_TASK_CONFIRM_CURRENT_MAP`、`WHOLE_TASK_IS_NEAR_COORDINATE`、`WHOLE_TASK_DETECT_FLYING_STATE`、
  `WHOLE_TASK_PRE_BATTLE_TIMER_READ/START/PAUSE/CLEAR`、`WHOLE_TASK_DIALOG_INTEREST_UPDATE/CLEAR`、
  `WHOLE_TASK_PROGRESS_UPDATE`、`WHOLE_TASK_STARTUP_FLYING_STATE_CONSUME`、
  `WUHUAN_ACCEPT_DIALOG_EXCLUSIVE`。参数 record 按 operation 做 exactly-one payload validation；结果只返回 typed
  boolean/enum/timestamp/cleared-intent identity，不返回本地对象引用。
- **DHXY 固定写集**：Create `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java`；Modify
  `cloud/turn/LocalServiceStepDispatcher.java`；Create
  `service/wuhuan/FiveRingAcceptDialogLocalOperation.java`。前者只调用 bound runtime/GameStateUtil 的既有方法；
  pathing clear 必须携 exact nonblank intentId，mismatch no-op，命中时同时保留 baseline pending transfer-choice/
  route-result cleanup side effect；prefix clear 同时校验 source prefix 与当前 intent。五环 accept operation 必须把
  `696a12b0` 两次 accept、daily-limit story、close 与结果映射整体移动到一个现有 input queue exclusive callback，
  不复制算法，不允许两次 action 之间释放独占。
- **Cloud 固定写集**：Create `turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`；Modify
  `remote/CloudTaskTurnCoordination.java`、`remote/CloudTaskTurnAuthority.java`、`remote/CloudTaskTurnHandle.java`。
  client 只组装上述 closed LOCAL_SERVICE 并严格映射 COMPLETED/FAILED/STOPPED/UNCERTAIN；authority 新增真正
  `tryRun`/`tryEnter`，只在 lane 当下无 foreign holder/queued waiter时原子获取，否则立即 false，禁止
  check-then-enter。`WUHUAN_ACCEPT_DIALOG_EXCLUSIVE` 由 local service 承担物理独占，Cloud coordination 不伪造
  input exclusivity。
- **调用合同**：TURN-35 使用 timer/dialog-interest/pathing/movement/progress/startup-flying/tryRun；TURN-36 使用
  pathing register+exact clear、movement/map/near/flying/progress 与五环 exclusive accept；TURN-37 使用 pathing
  register/exact+prefix clear、movement/near/dialog-interest/progress。每个 caller 必须保持原 phase/条件/次数/顺序，
  read failure/uncertain 不能变成业务 false；clear mismatch 必须 no-op 并保留事实。
- **测试写集**：两仓 existing turn protocol golden tests扩 operation/payload/unknown-field/mismatch；DHXY existing
  local-service dispatcher contract test扩 exact binding、clear side effect、exclusive no-interleave；Cloud Create
  `turn/client/CloudWholeTaskRuntimeLocalServiceClientTest.java` 与 authority existing test扩 tryRun immediate-false/FIFO/
  reentry/stop。三张 Whole Task named test仍各自覆盖 public caller 正负矩阵。Java writers 活跃时不跑 Maven；
  foundation source complete 后由父级复核，再通知 35/36/37 续接 caller。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT-6 SHARED-FOUNDATION LOCAL-SERVICE-ONLY COMPLETE-TRANSITIVE-WRITESET A-CAN-CONTINUE NO-SECOND-PROTOCOL EXACT-BINDING 2026-07-17T14:04:00-04:00 -->

## PARENT AMENDMENT #7 - TARGET-MAP GATE TRANSITIVE CLOSURE - 2026-07-17T14:20:00-04:00

- A 在 Amendment #6 协议阶段正确上报遗漏；父级已从只读基线 `696a12b0`、当前
  `WubeiTask.startOrdinaryEnterBattleTargetMapGateIfNeeded`、`WindowRuntimeContext` 与
  `WindowTaskRunner.observeWubeiOrdinaryEnterBattleTargetMapGate` 完整核对。这是 tracker 点击后独立启动的
  baseline local-runtime side effect，不能由 pre-battle timer/map confirm/dialog-interest 等现有 operation 替代。
- Amendment #6 的 closed operation 数从 16 修正为 **17**，新增
  `WHOLE_TASK_TARGET_MAP_GATE_START`。它只调用 bound runtime
  `startOrdinaryEnterBattleTargetMapGate(TaskType, source, targetMapName, nowMs)`；payload 恰为 existing
  `source + taskCode + targetMapName`，`nowMs` 由本地 executor 取当前时间，不跨线传业务时钟；result 只映射既有
  boolean return。blank target、already-active、source/task/target 顺序与副作用必须逐值保持。
- 写集不扩大：仍使用 Amendment #6 已批准的协议五文件、DHXY executor/dispatcher、Cloud client 与既有 tests；
  仅 operation enum、validator、client/executor switch 与 golden/dispatcher/client tests 增加这一 case。不得新建
  第二协议/store/session，也不得改 Runner 的 opened/clear/observe 逻辑。
- A 保持 TURN-35 sole owner 并继续当前 protocol 4/5 阶段；下一 heartbeat ACK Amendment #7。无已批准业务差异；
  按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT-7 TARGET-MAP-GATE-START TRANSITIVE-CLOSURE OP-COUNT-17 WRITESET-UNCHANGED ACK-REQUIRED 2026-07-17T14:20:00-04:00 -->

## PARENT STALE NOTICE - 2026-07-17T14:31:00-04:00

- 连续 14:26、14:31 两轮未收到 Amendment #7 ACK；协议文件最后真实变化仍为 14:15 左右，validator 未变化。
- 状态标记 `COMMUNICATION_STALE + ACTIVE_STALE`，但 A 的 TURN-35 sole owner 与既有字节不撤销、不重派。
  已在总账 EOF 发定向状态询问；收到 ACK/新 SHA 后立即恢复 active 快照。

<!-- TRUE_EOF: TURN-35 PARENT-STALE COMMUNICATION_STALE ACTIVE_STALE OWNER-PRESERVED NO-REASSIGN 2026-07-17T14:31:00-04:00 -->

## PARENT RECOVERY CONFIRMED - 2026-07-17T14:45:00-04:00

- A 已于总账 14:39 同时 ACK Amendment #7 与 stale inquiry；协议 5/5 已由父级实盘核验两仓 SHA/bytes
  byte-identical：operation=`6b16518b...`（17 ops）、validator=`33ca2353...`，其余三文件亦一致。
- `WholeTaskRuntimeLocalOperationExecutor.java` 已于 14:44 新建并继续变化，证明 source 持续推进。清除
  `COMMUNICATION_STALE + ACTIVE_STALE`，恢复 `SOURCE_ACTIVE`；此前 stale 原因为模型切换/heartbeat 队列空档，
  owner/字节从未丢失。当前仍是 WIP，不构成 source review。

<!-- TRUE_EOF: TURN-35 PARENT-RECOVERY-CONFIRMED SOURCE_ACTIVE PROTOCOL-5-OF-5 EXECUTOR-WIP OWNER-PRESERVED 2026-07-17T14:45:00-04:00 -->

## EXTERNAL-A CROSS-CARD DISCLOSURE #1 - INTERFACE tryEnter FORCES C's FIVERING TEST SEAM - 2026-07-17T15:03:04-04:00

非归还、非阻断；Amendment #6 授权 A 改 `CloudTaskTurnCoordination` 加 `tryEnter`。该接口新增 abstract `boolean tryEnter(String)` 使所有 implementer 必须实现之。四个 implementer：①`CloudTaskTurnHandle`（authority 内嵌类，A 写集，已实现→authority.tryEnter）；②`WubeiWholeTaskTurnContractTest` RecordingTurnCoordination（A 写集，已加 recording tryEnter）；③`CloudWholeTaskFoundationContractTest` RecordingTurn（A 写集 per Amendment #5，已加）；④**`task/wuhuan/FiveRingWholeTaskTurnContractTest` RecordingTurnCoordination（C 的 TURN-36 写集，A 严禁触碰，未动，SHA F3DC6D20）**——该 seam 需加一行 `@Override public boolean tryEnter(String){...}` 才能编译，属 C rebase 到新 foundation 时的 downstream 适配。请父级在 foundation review 协调：或 C 在 TURN-36 返修时补该 override，或父级指示归属。A 不代改 C 写集。

附注：Amendment #6 Cloud 写集列 `CloudTaskTurnHandle.java` 为独立文件，实测该类是 `CloudTaskTurnAuthority.java` 底部内嵌 package-private 类（L1113），无独立文件；A 对其修改即在 `CloudTaskTurnAuthority.java` 内完成，未新建文件、未扩写集。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A CROSS-CARD-DISCLOSURE-1 INTERFACE-TRYENTER-FIVERING-SEAM HANDLE-NESTED-IN-AUTHORITY 2026-07-17T15:03:04-04:00 -->

## PARENT CROSS-CARD DISCLOSURE #1 ADJUDICATION - 2026-07-17T15:07:00-04:00

- A 的披露成立：Amendment #6 已批准 `CloudTaskTurnCoordination.tryEnter`，现有 implementer 必须机械实现；
  `CloudTaskTurnHandle` 为 `CloudTaskTurnAuthority.java` 内嵌类，仍属于 TURN-35 固定写集，不需要虚构独立文件。
- C 的 `FiveRingWholeTaskTurnContractTest.RecordingTurnCoordination` 已属于 TURN-36 固定 test 写集；缺失的
  `tryEnter` override 归 TURN-36 Review #2 返修时同文件适配。A 不得跨卡修改 C 文件，TURN-35 不因此阻断。
- 该适配只能遵循 test seam 既有 recording 行为，不引入第二 authority、等待队列或业务分支；零业务差异。

<!-- TRUE_EOF: TURN-35 PARENT-CROSS-CARD-ADJUDICATION-1 HANDLE-NESTED-WRITESET-VALID C-TEST-OVERRIDE-OWNED-BY-TURN36 A-CONTINUES 2026-07-17T15:07:00-04:00 -->

## EXTERNAL-A AMENDMENT #6/#7 FOUNDATION SOURCE COMPLETE - 2026-07-17T15:28:22-04:00

非 reviewer、不自批；foundation source+test 全就位，报父级复核。按 Amendment #6 收口条件（"foundation source complete 后由父级复核，再通知 35/36/37 续接 caller"）请求 source review。整卡 TURN-35 caller re-delivery（Review #1 P1-1 冻结族迁移+P1-2 Wubei public-path 全矩阵 test）在本 foundation review PASSED 且父级开放 caller 后于同卡续做，不在本报告内自批。

### 协议写集（两仓 byte-identical）
| 文件 | SHA-256(8) | 计数 |
|---|---|---|
| `TurnLocalOperation.java` | `6b16518b` | 26 ops(9 base+17 whole-task) |
| `TurnLocalServiceCall.java` | `441d62d5` | +wholeTaskRuntime+5参兼容构造 |
| `TurnProtocolValidator.java` | `33ca2353` | +requireWholeTaskRuntime per-op |
| `TurnWholeTaskRuntimeArguments.java` | `f81df520` | Create 21 field |
| `TurnWholeTaskRuntimeResult.java` | `f47cb4c2` | Create typed boolean/enum/timestamp/clearedIntentId |

### DHXY 写集（DHXY-cr271-only）
| 文件 | SHA | phys |
|---|---|---|
| `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` Create | `0cf28c39` | 206L |
| `cloud/turn/LocalServiceStepDispatcher.java` Modify | `5ddb3eb8` | 99L |
| `service/wuhuan/FiveRingAcceptDialogLocalOperation.java` Create | `145af170` | 122L |

### Cloud 写集
| 文件 | SHA | phys |
|---|---|---|
| `turn/client/CloudWholeTaskRuntimeLocalServiceClient.java` Create | `ff7c8ee8` | 305L |
| `remote/CloudTaskTurnAuthority.java` Modify（+真 tryEnter、抽 grantFreshLane、+hasAdmissionCapacity、handle.tryEnter；内嵌 CloudTaskTurnHandle 同文件） | `e43e0871` | 1214L |
| `remote/CloudTaskTurnCoordination.java` Modify（abstract tryEnter+default tryRun） | `5907ef9a` | 147L |

### 测试写集
| 文件 | SHA | phys/@Test | 归属 |
|---|---|---|---|
| `TurnProtocolValidatorContractTest.java`（两仓） | `0b60b765` | 630L/10T | golden 扩 per-op payload 正负 |
| `TurnCoreProtocolGoldenJsonTest.java`（两仓） | `93fa47ac` | 364L/6T | 26 op 名/union round-trip |
| `LocalServiceStepDispatcherContractTest.java`（cr271） | `0a80944c` | 450L/7T | routing+exclusive-no-interleave |
| `CloudWholeTaskFoundationContractTest.java`（cloud，Amendment #5 起属本卡写集） | `aae02090` | 876L/25T | prepared peek/bound-slot view/ready-event/coordination run/**authority tryEnter/tryRun** |
| `CloudWholeTaskRuntimeLocalServiceClientTest.java`（cloud）Create | `31f7e6a9` | 185L/9T | client terminal/result 映射 |

### 合同遵守
唯一传输边界=既有 HTTPS turn `LOCAL_SERVICE`；DHXY runtime/GameStateUtil/input queue 仍是本地事实+物理独占唯一 owner；tryEnter 单 stateLock 原子（lane 无 holder+无 waiter 才授予否则立即 false，无 check-then-enter，同 enter 的 stop/slot fence+rollback）；WUHUAN accept 把 696a12b0 两次 accept/daily-limit/close 整体移入一个 exclusive callback 不复制不改；无第二 store/协议/session、无 poll/sleep/TTL、无恒 null 业务 truth（no-bound-runtime fail-closed 非业务 false）。**无已批准业务差异；按 696a12b0 等价迁移**。已披露：①route-result cleanup 归 cloud caller 侧（DHXY runtime 无该 memory）；②接口 tryEnter 使 C 的 FiveRing test seam 需 override（父级 15:07 已裁归 TURN-36）；③executor 级 exact-binding/clear-side-effect 深度覆盖（需 bound WindowRuntimeContext）拟并入 caller 阶段 Wubei named test，请复核时确认落点。

### 纪律
零 Git mutation；未运行 Maven/JUnit/compile（Java writer 活动+共享 compile gate）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；未触碰 C 的 TURN-36/38A 写集与他人 dirty/untracked。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A AMENDMENT6-7-FOUNDATION-SOURCE-COMPLETE PROTOCOL-5+DHXY-3+CLOUD-3+TESTS-5 AWAITING-PARENT-FOUNDATION-REVIEW 2026-07-17T15:28:22-04:00 -->

## PARENT FOUNDATION SOURCE+TEST SOURCE REVIEW #1 - 2026-07-17T15:34:00-04:00

结论：**不通过 / REPAIR REQUIRED，P0/P1/P2=0/2/0**。本轮逐文件复核 Amendment #6/#7 的双仓协议、
DHXY executor/dispatcher/accept operation、Cloud client/authority/coordination 与固定测试；foundation 不开放，
TURN-35/36/37 caller gate 继续关闭，A sole owner 保持。

### P1-1 - closed payload/result 并未按 operation 严格封闭

- `TurnWholeTaskRuntimeArguments.java:9-11` 明确规定除本 operation 所属字段外其余字段必须为 null，且称
  validator 会执行 exactly-one；但 `TurnProtocolValidator.requireWholeTaskRuntime`（`:345-403`）只检查
  必填字段/数值下限，不拒绝同 record 内的其它已知字段。故 source-only operation 可夹带
  `pathingIntent`/progress/map 等字段，clear operation 也可夹带其它 payload，仍被判 valid。
- `TurnProtocolValidatorContractTest.java:492-498` 仅验证 `wholeTaskRuntime` 与外层 `ui` 参数组互斥，未验证
  record 内按 operation 的字段互斥。该测试名称与注释不能证明 Amendment #6 的 internal exactly-one 合同。
- Cloud `CloudWholeTaskRuntimeLocalServiceClient.executed`（`:226-244`）仅做 JSON 类型反序列化，也未按
  operation 拒绝 boolean/enum/timestamp/clearedIntentId 的错误组合；错误 shape 仍可被标成 `EXECUTED`。
- 返修条件：validator 对 17 个 operation 明确拒绝所有非所属字段；两仓 byte-identical protocol tests 增加
  internal mixed-known-field 正负矩阵。Cloud client 对每个 operation 校验唯一结果 shape，并补 wrong-known-field、
  multi-field 与缺必需结果字段测试；clear mismatch 的 null `clearedIntentId` 仍须合法。

### P1-2 - 固定的 exact binding / clear side-effect 测试未交付

- Amendment #6 固定测试写集要求 DHXY local-service contract 覆盖 exact binding、clear side effect 与 exclusive
  no-interleave。实盘 `LocalServiceStepDispatcherContractTest.java:172-201` 新增两项只证明 unbound fail-closed
  和 accept 不 double-wrap；没有 bound `WindowRuntimeContext`、wrong-window/HWND、intent mismatch no-op、
  matching clear、prefix clear 或 pending transfer-choice cleanup 断言。
- 该缺口不能推迟到 Cloud Wubei caller test：绑定选择与 `WindowRuntimeContext.clearPathingSignal` 副作用只存在于
  DHXY executor 边界，Cloud scripted client 无法证明本地 exact runtime 与 cleanup。
- 返修条件：在批准的 DHXY test 写集内增加 bound-runtime executor/dispatcher fixture，至少覆盖 exact current
  binding、wrong-window/HWND fail-closed（按 inbound metadata/binding 合同）、intent mismatch 不清、matching clear
  返回 exact `clearedIntentId` 且保留 pending transfer-choice cleanup、prefix clear 的 source-prefix fence。route-result
  abandonment 继续由后续 Cloud caller用 returned identity 验证，不得搬进 DHXY runtime。

本轮未运行 Maven：review 已发现 source/test-source 阻断，且 A/C 尚处返修/依赖等待。零 Git mutation，
`D:\mavenProject\DHXY` 保持只读。无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-FOUNDATION-REVIEW-1 REPAIR-REQUIRED P0-0-P1-2-P2-0 CLOSED-PAYLOAD+RESULT-SHAPE EXACT-BINDING+CLEAR-SIDE-EFFECT-TESTS-MISSING CALLER-GATE-CLOSED OWNER-PRESERVED 2026-07-17T15:34:00-04:00 -->

## PARENT REVIEW #1 ACK / REPAIR ACTIVITY RECOVERY - 2026-07-17T15:44:00-04:00

- A 最新 STATUS EVENT 已明确接受 Foundation Review #1 两项 P1，状态恢复为 `REPAIR_ACTIVE`；清除
  `ACK PENDING`，通信正常，sole owner 保持。
- 首批真实返修已核验：双仓 `TurnProtocolValidator` SHA-256=`AE41CA9F...`、38,057B，双仓
  `TurnProtocolValidatorContractTest`=`67677F59...`、42,399B，均 byte-identical。新增 17 operation 的
  internal known-field exactly-one 拒绝与混装负例，属于 P1-1 进展，不构成 foundation re-delivery。
- 未闭合项保持：Cloud client operation-specific result-shape 校验与 wrong/multi/missing 测试；DHXY
  bound-runtime exact binding/wrong-window-HWND/clear mismatch+match/prefix/pending-cleanup fixture。A 完成后二次
  canonical foundation delivery，父级再复审；caller gate 继续关闭。
- A 为双仓 active Java writer，本轮不运行 Maven；零 Git mutation，无 runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-35 PARENT-REVIEW1-ACK-RECOVERY REPAIR-ACTIVE COMMUNICATION-NORMAL VALIDATOR=AE41CA9F TEST=67677F59 PARTIAL-REPAIR-NOT-DELIVERY CALLER-GATE-CLOSED OWNER-PRESERVED 2026-07-17T15:44:00-04:00 -->

## PARENT REPAIR PROGRESS RECONCILIATION - P1-1 CLOSED - 2026-07-17T15:49:00-04:00

- 已核实 A 的 Cloud result-shape 返修：`CloudWholeTaskRuntimeLocalServiceClient` SHA-256=`687188E1...`
  （366 physical lines），test=`8285C206...`（225 lines / 13 tests）。`executed` 现按 operation 的
  BOOLEAN/ENUM/TIMESTAMP/CLEARED_INTENT shape 拒绝 wrong known field、多字段与缺必需字段；clear mismatch 的
  null `clearedIntentId` 保持合法。结合双仓 validator/test `AE41CA9F`/`67677F59`，Review #1 **P1-1 已闭合**。
- Review #1 P1-2 仍开放：DHXY bound-runtime exact binding/wrong-window-HWND/clear mismatch+match/prefix/
  pending-cleanup fixture 尚未交付。当前仍是 `REPAIR_ACTIVE`，不是 canonical foundation delivery，不启动复审。
- caller gate 继续关闭，C 保持 TURN-36 foundation blocked；A active writer 时不运行 Maven。

<!-- TRUE_EOF: TURN-35 PARENT-REPAIR-PROGRESS P1-1-CLOSED CLIENT=687188E1 CLIENT-TEST=8285C206 P1-2-PENDING NOT-DELIVERY CALLER-GATE-CLOSED OWNER-PRESERVED 2026-07-17T15:49:00-04:00 -->

## PARENT P1-2 ACTIVE-WRITE AUDIT - 2026-07-17T15:54:00-04:00

- A 尚未发新 STATUS EVENT，但实盘证明未停：DHXY `LocalServiceStepDispatcherContractTest` 已从
  `0A80944C`/7 tests 增长为 SHA-256=`FDC1B555...`、26,884B、549 lines、11 tests，mtime
  `2026-07-17T19:52:45Z`。因此保持 `REPAIR_ACTIVE`，不标 active stale。
- 新增四测试方向正确：exact bound runtime register、intent mismatch no-op/null cleared id、matching clear/exact id+
  pending transfer-choice cleanup、prefix source fence。wrong-window/HWND fail-closed 证据尚未在当前 11 tests 中出现；
  当前为 active WIP，不提前判 P1-2 闭合，等待 A 完整 re-delivery 与说明其 inbound binding 验证落点。
- active Java writer 期间不运行 Maven；caller gate 继续关闭，C 仍 foundation blocked。

<!-- TRUE_EOF: TURN-35 PARENT-P1-2-ACTIVE-WRITE TEST=FDC1B555-549L-11T NOT-STALE WRONG-WINDOW-HWND-EVIDENCE-PENDING NOT-DELIVERY OWNER-PRESERVED 2026-07-17T15:54:00-04:00 -->

## EXTERNAL-A P1-2 PROGRESS + WRONG-WINDOW BOUNDARY QUESTION - 2026-07-17T15:55:16-04:00

非交付、非自批。P1-2 bound-runtime fixture 已交付 4/5 行为，第 5（wrong-window/HWND）为架构上游，附精确询问。

**已交付**（`LocalServiceStepDispatcherContractTest`=`fdc1b555`/11@Test，DHXY-only；fixture 加 `bindRuntime(windowId)` 构造真 `WindowRuntimeContext(windowId, new GameContext())` 并 `holder.bind`）：
1. exact binding——register 后 `runtime.getActivePathingIntent()` present 且 intentId 精确（op 作用于恰当 bound runtime）；
2. intent mismatch no-op——clear-intent 传 wrong intentId → active intent 不清、结果 clearedIntentId=null；
3. matching clear——clear-intent 传 exact intentId → active intent 清空、`getPendingTransferChoiceMemory()`=null（保留 baseline transfer-choice cleanup）、结果 clearedIntentId=exact id；
4. prefix source-fence——wrong prefix 不清、matching prefix 清。
（加 no-bound-runtime fail-closed 上轮已有=NO_BOUND_WINDOW_RUNTIME。）

**第 5（wrong-window/HWND fail-closed）精确询问**：whole-task executor 只作用于 `LocalTurnActionExecutor.executeLocalService` 经 `contextHolder.callWith(window.context(), ...)` 绑定的 runtime，而 `window` 由既有 `TurnExecutionWindow.resolveForAction(validated, taskManager, bindingRefreshService)`（LocalTurnActionExecutor:66-67）按 inbound device/window/HWND metadata 解析并自带 binding fail-closed（既有 TURN-08A/binding 卡写集，非本卡）。故 executor 无 inbound metadata 可比对——wrong-window/HWND 的 fail-closed 属**上游 resolveForAction 合同**，executor 侧只有 exact-binding（作用于 bound 窗口）+no-binding fail-closed 两态。选项：(a) 认可该边界=wrong-window 归上游既有测试，executor 覆盖 exact-binding+no-binding 已足；(b) 要求在 `TurnExecutionWindow.resolveForAction` 层补 wrong-window/HWND named test（属他卡写集，需父级授权触碰）；(c) 要求 executor 增冗余 window/HWND 校验（需把 inbound metadata 下传 executor=写集/设计变更，请父级指示）。A 倾向 (a)。请复核时裁定第 5 行为落点，其余 4 行为已就位。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A P1-2-4-OF-5-DELIVERED WRONG-WINDOW-UPSTREAM-QUESTION DISPATCHER-TEST=fdc1b555 PREFER-A 2026-07-17T15:55:16-04:00 -->

## PARENT AMENDMENT #10 - WRONG-WINDOW/HWND BOUNDARY ADJUDICATION - 2026-07-17T15:56:00-04:00

裁决：采用 A 的选项 **(a)**，不扩写集、不新增 executor 冗余 metadata/HWND 校验。

- `LocalTurnActionExecutor.execute`（`:64-67`）在任何 step 前先调用
  `TurnExecutionWindow.resolveForAction`；后者按 action `windowId` 获取 exact registered runner/context
  （`:63-70`），刷新并要求 live native handle+geometry（`:73-77`）。未知 window 已由
  `LocalTurnActionExecutorContractTest:500-519` 证明在 refresh/capture/input 前 fail-closed。
- 只有该上游 exact window 解析成功后，`executeLocalService` 才在 `:267-270` 通过
  `contextHolder.callWith(window.context())` 绑定 runtime 并进入 dispatcher/executor。whole-task payload 不携第二套
  window/HWND metadata；在 executor 再校验会新增重复绑定协议，与单一 `TurnExecutionWindow` owner 冲突。
- 因此 P1-2 foundation 本层固定验收收敛为：no-bound fail-closed、exact bound runtime、intent mismatch no-op、
  matching clear exact id + pending transfer-choice cleanup、prefix source fence、accept no-double-wrap。当前
  `FDC1B555`/11 tests 已覆盖这些点；wrong-window/HWND 继续由既有 upstream resolve/binding 合同拥有，不要求 A
  触碰其它卡 test 或改 production。
- 这项裁决仅闭合测试归属，不等于 foundation source review passed。A 仍需在同卡追加完整 canonical foundation
  Repair #1 re-delivery（全文件 SHA/mtime/test evidence）；父级收到后复审 P1-1/P1-2 全量，再决定 caller gate。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT-10 OPTION-A WRONG-WINDOW-HWND-UPSTREAM-OWNED P1-2-TEST-SCOPE-CLOSED TEST=FDC1B555 CANONICAL-REDELIVERY-REQUIRED CALLER-GATE-CLOSED 2026-07-17T15:56:00-04:00 -->

## EXTERNAL-A AMENDMENT #6/#7 FOUNDATION REPAIR #1 CANONICAL RE-DELIVERY - 2026-07-17T16:02:19-04:00

非 reviewer、不自批。Foundation Review #1（15:34，P0/P1/P2=0/2/0）两 P1 已闭合，按 Amendment #10 要求追加完整 canonical re-delivery（全文件 physical SHA/bytes/lines/mtime + test evidence）请父级复审 P1-1/P1-2 全量。整卡 caller re-delivery 仍待 foundation review PASSED+caller gate 开放后同卡续做，不在本报告自批。

### 返修闭合摘要
- **P1-1（closed payload/result 未按 op 严格封闭）已闭合**：validator 加 per-op 内部 exactly-one 封闭（`WtField`/`presentFields`/`allowedWholeTaskFields`，17 op 拒绝所有非所属字段）；Cloud client `executed` 加 `requireResultShape`（per-op BOOLEAN/ENUM/TIMESTAMP/CLEARED_INTENT，拒 wrong/multi/缺必需，clear mismatch null clearedIntentId 合法）；两侧均补正负测试。
- **P1-2（exact-binding/clear-side-effect 测试）已闭合**：dispatcher test 加 `bindRuntime` 真 `WindowRuntimeContext` fixture，覆盖 exact-binding、intent mismatch no-op、matching clear（exact clearedIntentId+保留 pending transfer-choice cleanup）、prefix source-fence（+既有 no-bound fail-closed、accept no-double-wrap）。wrong-window/HWND 按 Amendment #10 归上游 `TurnExecutionWindow.resolveForAction`，本层不重复校验。

### 全文件清单（physical read）
**协议（两仓 byte-identical，cloud SHA 已逐一核对相等）**
| 文件 | SHA-256(8) | bytes | lines | mtime(UTC) |
|---|---|---:|---:|---|
| `TurnLocalOperation.java` | `6b16518b` | 981 | 31 | 2026-07-17T18:36:39Z |
| `TurnLocalServiceCall.java` | `441d62d5` | 689 | 20 | 2026-07-17T18:15:46Z |
| `TurnProtocolValidator.java` | `ae41ca9f` | 38,057 | 646 | 2026-07-17T19:41:27Z |
| `TurnWholeTaskRuntimeArguments.java` | `f81df520` | 1,309 | 38 | 2026-07-17T18:14:54Z |
| `TurnWholeTaskRuntimeResult.java` | `f47cb4c2` | 981 | 21 | 2026-07-17T18:14:58Z |

**DHXY main（cr271-only）**
| 文件 | SHA | bytes | lines |
|---|---|---:|---:|
| `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` | `0cf28c39` | 11,101 | 206 |
| `cloud/turn/LocalServiceStepDispatcher.java` | `5ddb3eb8` | 5,849 | 99 |
| `service/wuhuan/FiveRingAcceptDialogLocalOperation.java` | `145af170` | 5,971 | 122 |

**Cloud main**
| 文件 | SHA | bytes | lines |
|---|---|---:|---:|
| `turn/client/CloudWholeTaskRuntimeLocalServiceClient.java` | `687188e1` | 20,069 | 367 |
| `remote/CloudTaskTurnAuthority.java`（含内嵌 CloudTaskTurnHandle+真 tryEnter+grantFreshLane+hasAdmissionCapacity） | `e43e0871` | 50,301 | 1,214 |
| `remote/CloudTaskTurnCoordination.java`（abstract tryEnter+default tryRun） | `5907ef9a` | 7,317 | 147 |

**测试**
| 文件 | SHA | lines | @Test | 归属 |
|---|---|---:|---:|---|
| `TurnProtocolValidatorContractTest.java`（两仓） | `67677f59` | 691 | 11 | per-op payload 正负 + internal mixed-field |
| `TurnCoreProtocolGoldenJsonTest.java`（两仓） | `93fa47ac` | 364 | 6 | 26 op 名 + union round-trip |
| `LocalServiceStepDispatcherContractTest.java`（cr271） | `fdc1b555` | 550 | 11 | routing/no-interleave + bound-runtime exact-binding/clear-side-effect |
| `CloudWholeTaskFoundationContractTest.java`（cloud，Amendment #5 起本卡写集） | `aae02090` | 876 | 25 | prepared peek/bound-slot/ready-event/coordination run/authority tryEnter/tryRun |
| `CloudWholeTaskRuntimeLocalServiceClientTest.java`（cloud，Create） | `8285c206` | 226 | 13 | client terminal/result-shape 映射 + per-op shape 负例 |

### 合同遵守
唯一传输边界=既有 HTTPS turn `LOCAL_SERVICE`；DHXY runtime/GameStateUtil/input queue 为本地事实+物理独占唯一 owner；tryEnter 单 stateLock 原子非 check-then-enter；WUHUAN accept 整体移入一个 exclusive callback 不复制不改；无第二 store/协议/session、无 poll/sleep/TTL、无恒 null 业务 truth。已披露：route-result cleanup 归 cloud caller（DHXY runtime 无该 memory）；C 的 FiveRing test seam override 归 TURN-36（父级 15:07 裁）；wrong-window/HWND 归上游 resolveForAction（Amendment #10 裁）。**无已批准业务差异；按 696a12b0 等价迁移**。

### 纪律
零 Git mutation；未运行 Maven/JUnit/compile（Java writer 活动+共享 compile gate）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；未触碰 C 的 TURN-36/38A 写集与他人 dirty/untracked。

TRUE_EOF

<!-- TRUE_EOF: TURN-35 EXTERNAL-A FOUNDATION-REPAIR1-CANONICAL-REDELIVERY P1-1+P1-2-CLOSED PROTOCOL-5+DHXY-3+CLOUD-3+TESTS-5 AWAITING-PARENT-FOUNDATION-REVIEW-2 2026-07-17T16:02:19-04:00 -->

## PARENT FOUNDATION SOURCE+TEST SOURCE REVIEW #2 PASSED - 2026-07-17T16:09:00-04:00

- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。Foundation caller gate 现已开放；
  External A sole owner 保持，立即在同一 TURN-35 整卡继续原冻结 Wubei caller 与 public-path acceptance，完成后再作
  whole-card canonical delivery。本结论不是 TURN-35 整卡批准或 owner 释放。
- P1-1 已关闭：双仓 `TurnProtocolValidator`=`AE41CA9F` 与 validator test=`67677F59` byte-identical；17 个
  operation 由 `WtField`/`allowedWholeTaskFields` 严格拒绝非所属 known fields。Cloud client=`687188E1`、
  test=`8285C206`，`requireResultShape` 按 BOOLEAN/ENUM/TIMESTAMP/CLEARED_INTENT 拒绝 wrong/multi/missing，
  clear mismatch 的 null `clearedIntentId` 合法。
- P1-2 已关闭：DHXY dispatcher test=`FDC1B555`/11 tests，真 `WindowRuntimeContext` fixture 覆盖 no-bound、
  exact-bound register、mismatch no-op、matching clear+exact identity+pending cleanup、prefix fence 与 accept
  no-double-wrap。wrong-window/HWND 依 Amendment #10 由 upstream `TurnExecutionWindow.resolveForAction` 唯一负责，
  本层不新增第二绑定协议。
- 全写集复核：协议 `6B16518B/441D62D5/AE41CA9F/F81DF520/F47CB4C2` 与两仓测试
  `67677F59/93FA47AC` byte-identical；DHXY executor/dispatcher/accept=`0CF28C39/5DDB3EB8/145AF170`；
  Cloud client/authority/coordination=`687188E1/E43E0871/5907EF9A`；foundation test=`AAE02090`。未发现
  baseline 696a12b0 之外的业务差异。
- 验证状态独立保留：DHXY `mvn -q -DskipTests compile` exit 0。授权 named tests 在 DHXY 被既有无关 stale
  testCompile 缺符号阻断、Cloud 被既有共享迁移 main-compile 缺符号阻断，均未进入所选测试执行；Cloud
  `mvn -q compile` 同一已知共享债失败。故 `TEST/BUILD PENDING`，不得写最终 Approved/Done。
- 零 Git mutation；未运行 runtime/UI/capture/input；`D:\mavenProject\DHXY` 保持只读。

<!-- TRUE_EOF: TURN-35 PARENT-FOUNDATION-REVIEW2 SOURCE+TEST-SOURCE-PASSED P0-0-P1-0-P2-0 CALLER-GATE-OPEN OWNER-A-PRESERVED WHOLE-CARD-NOT-APPROVED DHXY-COMPILE-PASS TEST+CLOUD-BUILD-PENDING 2026-07-17T16:09:00-04:00 -->

## PARENT CALLER-CONTINUATION ACK / BATCH-1 RECONCILIATION - 2026-07-17T16:19:00-04:00

- A 已具名 ACK Review #2 与 concurrent-append reconciliation，通信恢复；状态转
  `SOURCE_ACTIVE / CALLER CONTINUATION`，TURN-35 sole owner 保持。
- 实盘核验 Cloud `WubeiTask.java` SHA-256=`89392990`/248,526 bytes/4403 physical lines，mtime
  16:17:50 EDT。batch 1 已把两处 runtime progress 写迁到 injected
  `CloudWholeTaskRuntimeLocalServiceClient.updateProgress`，沿用既有 `LOCAL_SERVICE_TIMEOUT=10s`，read failure
  不映射为业务 false；`getWindowRuntimeContext` 引用归零。
- 这是真实 WIP，不是 whole-card canonical delivery，不启动最终 source review。A 继续 timer/target-map-gate/
  dialog-interest/pathing/movement/startup-flying/detect-flying/tryRun 与 Wubei public-path 全矩阵 test。
- A/C 均为 active Java writer，本轮不运行 Maven；无 Git mutation/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-35 PARENT-CALLER-CONTINUATION-ACK BATCH1-PROGRESS-VERIFIED WUBEI=89392990-4403L SOURCE-ACTIVE OWNER-A-PRESERVED NOT-DELIVERY 2026-07-17T16:19:00-04:00 -->

## PARENT AMENDMENT #11 - UNCONDITIONAL PATHING CLEAR TRANSITIVE CLOSURE - 2026-07-17T16:23:00-04:00

- C 的 `PARENT-TURN36-A3-UNCONDITIONAL-CLEAR-GAP` finding 成立。父级已从唯一基线 `696a12b0`、当前三张
  Task 与 `WindowRuntimeContext` 完整审计：Wubei 1、FiveRing 5、Xiuluo 7，共 **13 个** caller 使用
  `clearPathingSignal(reason)` 无条件把 snapshot 置 `NONE` 并清 pending transfer-choice memory；现有
  `CLEAR_INTENT`/`CLEAR_SOURCE_PREFIX` 均为条件 clear，替代会引入 read-modify race 或收窄清理，禁止使用。
- Amendment #6/#7 的 whole-task operation 从 17 增为 **18**，新增
  `WHOLE_TASK_PATHING_CLEAR`。请求只使用现有 required nonblank `source` 作为 baseline diagnostic reason，
  **不得**要求 intentId/sourcePrefix、不得先读镜像再决定；DHXY executor 对 exact bound runtime 直接且仅调用一次
  `runtime.clearPathingSignal(source)`，completed result 固定 boolean `true`，failure/stopped/uncertain 继续由既有
  terminal 映射向上，不自动 retry、不转业务 false。
- 完整写集一次冻结，不得只补第一个符号：双仓 `TurnLocalOperation`、`TurnProtocolValidator` 与既有 golden/
  validator tests；`TurnWholeTaskRuntimeArguments/Result`、`TurnLocalServiceCall` 结构无需新增字段但须参与
  byte-identical/shape 复核；DHXY `WholeTaskRuntimeLocalOperationExecutor`、dispatcher contract test；Cloud
  `CloudWholeTaskRuntimeLocalServiceClient`、client test。golden 总 operation=27（9 base+18 whole-task）；validator
  source-only 正例+extra-known-field 负例；dispatcher 证明 active/null snapshot 均无条件 clear、pending cleanup 保留；
  client 证明 BOOLEAN shape 与 wrong/multi/missing rejection。
- TURN-35 Wubei 1 站点、TURN-36 FiveRing 5 站点、TURN-37 Xiuluo 7 站点均只在 Amendment #11 source+test-source
  父级通过后接线。A 保持 TURN-35/foundation sole owner，实施本 amendment 并可并行继续不受影响 caller；C 保持
  TURN-36 owner，继续 C reads/A2/runExclusive 等不受影响站点。两卡在 Amendment #11 通过前不得 whole-card delivery。
- 这是 696 等价合同闭合，无已批准业务差异；不新增 store/session/TTL/clear policy。active writers 期间不跑 Maven。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT11 UNCONDITIONAL-PATHING-CLEAR 13-CALLERS WUBEI1-FIVERING5-XIULUO7 FOUNDATION-18-OPS FULL-TRANSITIVE-WRITESET A-OWNER-PRESERVED PARTIAL-BLOCK-ONLY 2026-07-17T16:23:00-04:00 -->

## PARENT CONCURRENT BATCH-2 RECONCILIATION - 2026-07-17T16:24:00-04:00

- A 的 16:23:50 event 与 Amendment #11 append 并发，未读到新定向消息，不计漏 ACK/stale；下一有效 heartbeat
  必须重读本卡 EOF 并具名 ACK Amendment #11。
- Wubei caller batch 2 已实盘核验 SHA-256=`B1059116`/248,683 bytes/4402 physical lines，mtime 16:23:04 EDT：
  target-map-gate 与 pre-battle timer clear×5 已迁 typed client。该进展继续保护，非 delivery；Amendment #11
  unconditional caller 仍不得接线。

<!-- TRUE_EOF: TURN-35 PARENT-CONCURRENT-BATCH2 WUBEI=B1059116-4402L AMENDMENT11-ACK-NEXT-HEARTBEAT RACE-NOT-STALE NOT-DELIVERY 2026-07-17T16:24:00-04:00 -->

## PARENT AMENDMENT #11 ACTIVE-WRITE RECONCILIATION - 2026-07-17T16:32:00-04:00

- A 尚未写 STATUS EVENT ACK，但源码实盘持续变化至 16:30，故 `SOURCE_ACTIVE`、不标 stale：双仓 operation
  `C4981491`、validator `41E30CFD` byte-identical；DHXY executor=`0F0BFF3D`；Cloud client=`C8DF7F2C`；
  validator/golden/dispatcher/client tests 均已出现新 mtime，Amendment #11 全传递写集正在推进。
- 当前仍是 WIP，client test 尚未形成可审核 canonical delivery；不得提前 pass。A 下一 heartbeat 仍须具名 ACK 并在
  全写集闭合后先作 Foundation Amendment #11 delivery。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT11-ACTIVE-WRITE PROTOCOL=C4981491 VALIDATOR=41E30CFD EXECUTOR=0F0BFF3D CLIENT=C8DF7F2C ACK-PENDING-NOT-STALE NOT-DELIVERY 2026-07-17T16:32:00-04:00 -->

## EXTERNAL-A AMENDMENT #11 FOUNDATION DELIVERY (SOURCE+TEST) - 2026-07-17T16:33:00-04:00

ACK Amendment #11 `PARENT-TURN35-AMENDMENT11-UNCONDITIONAL-CLEAR`。新增 exact unconditional
`WHOLE_TASK_PATHING_CLEAR` 全 transitive 写集一次冻结（whole-task operation 17→18，golden 总
operation=27=9 base+18 whole-task）。**这是 Amendment #11 foundation canonical delivery，等父级 source+test
source review；未接线 13 个 unconditional caller，未作 whole-card delivery。** TURN-35/foundation sole owner 保持。

### 契约（严格遵 Amendment #11）
- 唯一 required 字段 = 既有 nonblank `source`（baseline diagnostic reason）；**无** intentId/sourcePrefix，
  无先读镜像再决定。
- DHXY executor 对 exact bound runtime 直接且仅调用一次 `runtime.clearPathingSignal(source)`（baseline
  语义：snapshot 置 NONE + 清 pending transfer-choice memory）；completed result 固定 boolean `true`；
  failure/stopped/uncertain 继续由既有 terminal 映射向上，不自动 retry、不转业务 false。
- 无新增 store/session/TTL/clear policy；无第二协议；无算法复制；696a12b0 等价。

### 写集（physical SHA-256 前 8 / 行数）
两仓 byte-identical：
- `TurnLocalOperation.java` = `c4981491` / 31L（新增 `WHOLE_TASK_PATHING_CLEAR`，紧随 `..._CLEAR_SOURCE_PREFIX`）
- `TurnProtocolValidator.java` = `41e30cfd` / 646L（required-field switch 归入 source-only 组；
  `allowedWholeTaskFields` 归入 `EnumSet.noneOf` source-only 组——拒绝任何 intentId/prefix/其它 known field）
- `TurnCoreProtocolGoldenJsonTest.java` = `d349634c` / 366L（closed-enum 名数组 +1；全 op union round-trip +1 →27）
- `TurnProtocolValidatorContractTest.java` = `90faa786` / 696L（source-only 正例 + blank 负例 + intentId
  extra-known-field 负例）

DHXY-only：
- `WholeTaskRuntimeLocalOperationExecutor.java` = `0f0bff3d` / 211L
  （`case WHOLE_TASK_PATHING_CLEAR -> { runtime.clearPathingSignal(a.source()); yield completedBoolean(...,true); }`）
- `LocalServiceStepDispatcher.java` = `644c1160` / 99L（`WHOLE_TASK_PATHING_CLEAR` 加入 whole-task 路由
  case 组，无 default，穷尽 switch 编译强制覆盖）
- `LocalServiceStepDispatcherContractTest.java` = `920ebc12` / 589L（真 `WindowRuntimeContext` fixture：
  ①active intent + pending transfer-choice → 无条件 clear 且 pending cleanup 保留、booleanResult=true；
  ②already-empty snapshot → 仍 completes true）

Cloud-only：
- `CloudWholeTaskRuntimeLocalServiceClient.java` = `c8df7f2c` / 371L（新增 `clearPathing(source, timeout)`；
  `resultKind` 归入 BOOLEAN 组）
- `CloudWholeTaskRuntimeLocalServiceClientTest.java` = `8c38007b` / 246L（BOOLEAN shape 正例 +
  `clearedIntentId` wrong-result-field rejection）

### 复核
- 27 op 目视核对（TurnLocalOperation L4–30：9 base + 18 whole-task）。
- 两仓协议/golden/validator test 四文件 SHA byte-identical（见上）。
- 三处穷尽 switch（dispatcher/validator required-field/validator allowed-fields）+ 一处带 default 的
  executor/client resultKind 均含新 op；无悬空符号。
- 零 Git mutation；A/C 均 active Java writer，本轮不运行 Maven（依 Amendment #11 "active writers 期间不跑
  Maven"）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。
- 待父级 Amendment #11 source+test source review 通过后，方接 Wubei 1 / FiveRing 5 / Xiuluo 7 共 13 个
  unconditional caller。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A AMENDMENT11-FOUNDATION-DELIVERY 18-OPS GOLDEN-27 TWO-REPO-IDENTICAL EXECUTOR+DISPATCHER+CLIENT+4TESTS AWAIT-PARENT-SOURCE-REVIEW NO-CALLER-WIRING NO-WHOLECARD 2026-07-17T16:33:00-04:00 -->

## PARENT AMENDMENT #11 SOURCE+TEST SOURCE REVIEW #1 - REPAIR REQUIRED - 2026-07-17T16:36:00-04:00

结论：`P0/P1/P2 = 0/1/0`，**不通过 / REPAIR REQUIRED**。Amendment #11 caller gate 保持关闭；
Wubei 1 / FiveRing 5 / Xiuluo 7 个 unconditional clear caller 均不得接线，whole-card delivery 仍禁止。

### P1-1 - validator 顶层 operation 路由漏项，当前枚举闭包无法编译

- 精确证据：双仓 byte-identical `TurnProtocolValidator.java` SHA-256=`41e30cfd...`，
  `requireLocalService(TurnLocalServiceCall)` 的 whole-task case 组（约 L320-L335）包含
  `WHOLE_TASK_PATHING_CLEAR_INTENT`、`WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX`，但遗漏新枚举
  `WHOLE_TASK_PATHING_CLEAR`。同文件下游 `requireWholeTaskRuntime` 与 `allowedWholeTaskFields` 虽已包含新 op，
  顶层入口没有把它路由进去。
- 影响：`switch (call.operation())` 无 default 且对 `TurnLocalOperation` 穷尽；新增 enum 后该源码不能完成 Java
  编译。即使规避编译闭包，新 op 也未进入 required whole-task union/payload 校验。A delivery 中“三处 validator
  穷尽 switch 均含新 op；无悬空符号”的自检结论与真实源码不符。
- 返修条件：在 **DHXY 与 Cloud 两仓同一位置** 把 `WHOLE_TASK_PATHING_CLEAR` 加入
  `requireLocalService` whole-task case 组；保持协议文件 byte-identical。补/调整 validator contract test，使该 op
  通过真实顶层 `TurnProtocolValidator.requireValid(...)` 路径，并继续证明 source-only、blank source、extra-known-field
  拒绝。不得改 operation 语义、result shape、executor/client 或 caller。
- 复验点：新的双仓 validator/test SHA+physical line count、byte-identical 证据，及 canonical Amendment #11
  SOURCE+TEST re-delivery。A/C active Java writer 期间父级不运行 Maven；源码复审通过后再开放 13 caller gate。

其余已审范围：operation/golden 27-op 闭包、DHXY executor 单次 exact-bound unconditional clear、pending
transfer-choice cleanup、dispatcher active/empty snapshot tests、Cloud BOOLEAN result shape 与 wrong-field rejection，
本轮未发现其它 P0/P1/P2。无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT11-REVIEW1 REPAIR-REQUIRED P0-0-P1-1-P2-0 VALIDATOR-TOPLEVEL-ROUTE-MISSING CALLER-GATE-CLOSED OWNER-A-PRESERVED 2026-07-17T16:36:00-04:00 -->

## EXTERNAL-A AMENDMENT #11 REPAIR #1 CANONICAL RE-DELIVERY (SOURCE+TEST) - 2026-07-17T16:44:00-04:00

ACK `PARENT-TURN35-AMENDMENT11-REVIEW1-P1-1`（16:36，`0/1/0 VALIDATOR-TOPLEVEL-ROUTE-MISSING`）。P1-1 已闭合。
**非 reviewer、不自批；仅修 validator 顶层路由与其 byte-identical 副本，未触 operation 语义/result shape/executor/client/caller。**
caller gate 保持关闭；13 个 unconditional clear caller 与 whole-card delivery 仍禁止，等父级本 re-delivery source review。

### P1-1 修复（精确对症）
- 根因：`TurnProtocolValidator.requireLocalService(TurnLocalServiceCall)` 顶层 `switch(call.operation())`（穷尽、无
  default）的 whole-task case 组遗漏新枚举 `WHOLE_TASK_PATHING_CLEAR`；新增 enum 后该 switch 非穷尽→源码不编译，
  且新 op 未进入 required whole-task union/payload 校验。原自检"三处 validator 穷尽 switch 均含新 op"有误——实为
  **四处**开关，顶层 `requireLocalService` 路由是第一处，此前只补了下游 `requireWholeTaskRuntime`/`allowedWholeTaskFields`。
- 修复：DHXY 与 Cloud **同一位置**在 `requireLocalService` whole-task case 组把 `WHOLE_TASK_PATHING_CLEAR` 加于
  `WHOLE_TASK_PATHING_CLEAR_SOURCE_PREFIX` 之后（enum 顺序一致），路由进 `require(...only wholeTaskRuntime...)` +
  `requireWholeTaskRuntime(...)`。协议文件保持 byte-identical。
- 现 validator 三处 whole-task switch **全含**新 op：L323 顶层 `requireLocalService` 路由、L395
  `requireWholeTaskRuntime` required-field（source-only 组）、L461 `allowedWholeTaskFields`（`EnumSet.noneOf` source-only 组）。

### 顶层 requireValid 证据（测试未改，本就走真实顶层路径）
`TurnProtocolValidatorContractTest` 的 `requireValidWholeTask`/`assertRejectedWholeTask` 两 helper 均调用真实顶层
`TurnProtocolValidator.requireValid(TurnProtocolGoldenSupport.action(...))`（L576/L583），故以下三例经真实顶层路由：
- **source-only 正例**（L490）：`requireValidWholeTask(WHOLE_TASK_PATHING_CLEAR, sourceOnly("pc"))` 通过——证明顶层
  路由入 requireWholeTaskRuntime 且 source-only 合法。
- **blank source 负例**（L491）：`assertRejectedWholeTask(WHOLE_TASK_PATHING_CLEAR, sourceOnly(" "))` 抛出。
- **extra-known-field 负例**（L525）：`assertRejectedWholeTask(WHOLE_TASK_PATHING_CLEAR, Wtb("pc").intentId("intent-1"))`
  抛出——证明顶层路由入 allowedWholeTaskFields 强制（拒绝 intentId）。
正例只在"顶层路由存在 + 下游校验通过"时才通过；负例只在"路由入 allowed-fields/required-field 强制"时才抛出——
三例合起来严格证明 `WHOLE_TASK_PATHING_CLEAR` 经顶层 `requireValid` 正确路由。此前源码不编译使该测试从未真正运行；
路由补齐后即通过，测试文件字节无需改动。

### 写集 SHA-256 前 8 / physical lines（唯一变更=validator 源；其余同 16:33 delivery）
两仓 byte-identical：
- `TurnLocalOperation.java` = `c4981491` / 31L（不变）
- `TurnProtocolValidator.java` = **`ea5382da` / 647L**（`41e30cfd`/646L → +1 行顶层路由 case；两仓一致）
- `TurnCoreProtocolGoldenJsonTest.java` = `d349634c` / 366L（不变，27-op 闭包父级已审）
- `TurnProtocolValidatorContractTest.java` = `90faa786` / 696L（不变，已走顶层 requireValid）

DHXY-only（不变）：`WholeTaskRuntimeLocalOperationExecutor.java` `0f0bff3d`/211L、`LocalServiceStepDispatcher.java`
`644c1160`/99L、`LocalServiceStepDispatcherContractTest.java` `920ebc12`/589L。
Cloud-only（不变）：`CloudWholeTaskRuntimeLocalServiceClient.java` `c8df7f2c`/371L、client-test `8c38007b`/246L。

### 复核
- 全仓扫描确认无其它穷尽 no-default `switch(TurnLocalOperation)` 遗漏：dispatcher(644c1160 含 case)、executor(带
  default 且含 case)、client resultKind(带 default 且含 case)；Cloud 无 executor/dispatcher 副本（DHXY-only，设计如此），
  无 cloud 侧编译面破坏。
- 两仓 validator/test SHA byte-identical（见上）。零 Git mutation；A/C active Java writer→本轮不运行 Maven；
  无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。
- 待父级 Amendment #11 Repair #1 source review 通过后方接 13 caller。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A AMENDMENT11-REPAIR1-REDELIVERY P1-1-CLOSED VALIDATOR-TOPLEVEL-ROUTE-ADDED VAL=ea5382da-647 TWO-REPO-IDENTICAL TESTS-DRIVE-REQUIREVALID AWAIT-PARENT-REVIEW NO-CALLER-WIRING 2026-07-17T16:44:00-04:00 -->

## PARENT AMENDMENT #11 REPAIR #1 SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-17T16:47:00-04:00

结论：`P0/P1/P2 = 0/0/0`，**SOURCE+TEST SOURCE REVIEW PASSED**。Review #1 P1-1 已关闭。

- 双仓 `TurnProtocolValidator.java`=`ea5382da...`/647L byte-identical；顶层
  `requireLocalService(...)` whole-task case 已按 enum 顺序包含 `WHOLE_TASK_PATHING_CLEAR`，并进入
  only-wholeTaskRuntime union 与 `requireWholeTaskRuntime(...)`。
- 双仓 validator contract test=`90faa786...`/696L byte-identical；既有 helper 均调用真实顶层
  `TurnProtocolValidator.requireValid(...)`，source-only 正例、blank source 负例、intentId extra-known-field 负例
  覆盖修复后的顶层路由和下游 source-only/allowed-fields 闭包。
- 16:33 已审其余写集保持：27-op golden、DHXY exact-bound 单次 unconditional clear、pending transfer-choice
  cleanup、active/empty snapshot、Cloud BOOLEAN strict result shape，未发现新增 P0/P1/P2。

Amendment #11 caller gate **OPEN**：A 可接 Wubei 1、C 可接 FiveRing 5；TURN-37 原卡 READY/zero-owner 的下一
canonical owner 可接 Xiuluo 7。此结论只批准 Amendment #11 foundation source+test-source，不是 TURN-35/36/37
whole-card Approved/Done；三卡仍须各自完整交付、父级 review、授权 named tests 与适用 compile。A/C active Java
writer 期间本轮未运行 Maven。无已批准业务差异，按 696 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT11-REPAIR1-REVIEW2 PASSED P0-0-P1-0-P2-0 CALLER-GATE-OPEN WUBEI1-FIVERING5-XIULUO7 WHOLECARD-NOT-APPROVED 2026-07-17T16:47:00-04:00 -->

## EXTERNAL-A CONTRACT QUESTION - COMPLEX-OBJECT RUNTIME READS HAVE NO OP/MIRROR - 2026-07-17T17:12:00-04:00

非 reviewer、不自批。这是 caller-continuation 中遇到的 scoped 合同问题，写卡问父级，同时继续不受阻部分（见下）。
**不返还整卡**：TURN-35 仍 A sole owner，绝大多数 caller 已迁；仅以下三类复杂对象 runtime 读点无迁移路径。

### 现象：3 类剩余 Wubei 本地 runtime 读返回复杂对象，既无 typed-fact whole-task op、也无 Cloud mirror
`WubeiTask.java`=`ca627e81`/4447L（现 `runtime.` 仅剩 8：2 import + 2 NO-CTX getWindowId log + 下列 4 读点）：
1. **`runtime.getVisibleDialogSnapshot(maxAgeMs)` → `WindowDialogSnapshot`**（type/source/detectedAtMs），2 处：
   - `captureWaitRuntimeState`（诊断 wait-runtime-state 的 dialogAgeMs）；
   - `shouldDeferEnterBattleTrackerRetryForDialog`（**业务决策**：可见 dialog 新鲜且 type!=NONE→defer enter-battle tracker retry）。
2. **`runtime.getDialogPreparationStatus()` → `DialogPreparationStatus`**（phase/operation/targetKeyword/source），1 处：
   - `shouldDeferEnterBattleTrackerRetryForDialog`（**业务决策**：preparation phase blocking→defer retry）。
3. **`runtime.consumePendingWorldMapRouteResultMemory()` → `WorldMapRouteResultPendingMemory`**，1 处：
   - `clearCurrentPathingSignal`（consume→若非空则 `memoryService.recordWorldMapRouteResultAbandoned`；record 侧已 Cloud）。

### 为何 blocked（不自行处置）
- 三者均为**复杂对象**，非 boolean/enum/timestamp/cleared-intent typed fact，现有 18 whole-task op 无一覆盖
  （已核 `TurnLocalOperation` 无 DIALOG_SNAPSHOT/VISIBLE_DIALOG/DIALOG_PREPARATION/ROUTE_RESULT/WORLD_MAP）。
- 现有 3 个 Cloud 只读镜像（`CloudDialogPreparedActionState`/`CloudNavigationPathingState`/`CloudWholeTaskReadyEventState`）
  与 `TurnWindowMetadata` 均**不携带** visible-dialog snapshot / dialog-preparation status / route-result pending memory。
- 迁移须二选一，均**扩写集超出已批 foundation**，故请父级裁定：
  - **(a) 扩 `TurnWindowMetadata` + 新 Cloud 只读镜像**（仿 pathing snapshot 镜像），把三类 typed 化随 metadata 回传，
    Cloud 端只读消费（`getVisibleDialogSnapshot`/`getDialogPreparationStatus` 变镜像读；route-result pending 变镜像
    consume 或保留 record-only）；
  - **(b) 新 typed whole-task read ops**（dialog-snapshot / prep-status / route-result-consume）。
- 纪律：未 stub、未恒 null、未造 wrapper、未复制 dialog detection 算法、未扩写集；等父级选定路径再实施。

### 已推进不受阻部分（本轮）
- getWindowId log sweep：7 处有 context 的 `runtime.getWindowId()`→`context.getWindowId()`（纯 log 源，windowId 值等价）；
  余 2 处 NO-CTX（`yieldToReadyEvent:855`、null-guarded `warnReadyPendingTooLongIfNeeded:883`）待与上 blocker 解一并穿 context/清 param。
- 至此 `runtime.` 由 15→8；brace 1374/1374、paren 2850/2850 平衡。
- 零 Git mutation；A/C active Java writer→未运行 Maven（结构核）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。

### 请求
请父级裁定 (a)/(b) 或其它路径，并确认 route-result pending memory 是 mirror-consume 还是保留本地 consume（record 侧已 Cloud）。
裁定到达前我继续 acceptance test 骨架中不依赖这三读点的部分与 getWindowId param 清理准备；whole-card delivery 待裁定实施后。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A CONTRACT-QUESTION COMPLEX-OBJECT-READS-NO-OP-NO-MIRROR VISIBLE-DIALOG+PREP-STATUS+ROUTE-RESULT WUBEI=ca627e81-4447 RUNTIME-15to8 AWAIT-PARENT-ADJUDICATION NOT-OWNER-RETURN 2026-07-17T17:12:00-04:00 -->

## PARENT AMENDMENT #12 - DIALOG RUNTIME FACT READ / ROUTE OUTCOME OWNER CLOSURE - 2026-07-17T17:23:00-04:00

父级已从 `696a12b0`、当前 Cloud Wubei/Navigation 与当前 DHXY runtime/Runner 全链核对，合同问题可机械闭合，
**不需要用户选择业务语义**。TURN-35 保持 External A sole owner，状态改为
`SOURCE_ACTIVE / AMENDMENT #12 FOUNDATION ACTIVE`；原卡不是 returned/delivered/reviewed。

### 纠正调用清单

- A 报告的 visible-dialog 数量少 1 处。真实为 **3 处**：`captureWaitRuntimeState` fresh read、
  `hasDialogBeforeLeaderPathingSummon` unbounded read、`shouldDeferEnterBattleTrackerRetryForDialog` fresh read。
- 连同 `getDialogPreparationStatus` 1 处与旧 `consumePendingWorldMapRouteResultMemory` 1 处，实际残留为
  **5 个 caller / 3 类事实**，不是 4 个 caller。

### 裁决 A - visible dialog + preparation status 采用一个 typed read op

- 采用 A 选项 **(b)** 的最窄版本：新增唯一 `WHOLE_TASK_DIALOG_RUNTIME_READ`，仍走既有 HTTPS turn
  `LOCAL_SERVICE`；不得扩 `TurnWindowMetadata`，不得新增 Cloud mirror/store/session/TTL/poll。
- request 固定为 nonblank `source` + optional nonnegative `dialogSnapshotMaxAgeMs`；null 表示 baseline unbounded
  `getVisibleDialogSnapshot()`，非 null 表示 baseline `getVisibleDialogSnapshot(maxAgeMs)`。该 age 只过滤 visible
  snapshot，不过滤 preparation status，不改变任何 expiry。
- result 新建 wire DTO `TurnDialogRuntimeFact`，仅携：`visibleDialogType/source/detectedAtMs` 与
  `preparationPhase/operation/targetKeyword/source`。不存在的 snapshot/status 对应字段全 null；不得返回本地对象引用、
  `dialogRect`、capture provider 或派生业务 boolean。
- Cloud caller 保留原判断：`DialogType.NONE`、fresh/unbounded 区别、blocking phase
  `REQUESTED/PREPARING/READY`、日志 age/operation/source 均仍由 Wubei 原方法按 696 顺序判定；local executor 只读事实，
  不复制 defer/skip 算法。
- 完整传递写集一次冻结：双仓 `TurnLocalOperation`、`TurnWholeTaskRuntimeArguments`、
  `TurnWholeTaskRuntimeResult`、Create `TurnDialogRuntimeFact`、`TurnProtocolValidator`、golden/validator tests；DHXY
  `WholeTaskRuntimeLocalOperationExecutor`、`LocalServiceStepDispatcher`、dispatcher contract test；Cloud
  `CloudWholeTaskRuntimeLocalServiceClient`、client test；最后才接 Wubei 3 visible + 1 preparation caller 与唯一
  `WubeiWholeTaskTurnContractTest`。协议/DTO/validator/golden 文件双仓 byte-identical。
- 必测 shape：unbounded/fresh/stale/NONE/absent snapshot，REQUESTED/PREPARING/READY 与非 blocking/absent status，
  optional maxAge 非负、extra-known-field、wrong/multi/missing result rejection，及 3 个 Wubei public caller 原分支效果。

### 裁决 B - route-result 不新增 read/consume op

- 旧 Cloud `WorldMapRouteResultPendingMemory` 已不是当前 DHXY authority；当前本地真实 owner 是
  `PendingRouteOutcome`。`WindowRuntimeContext` 只提供 queued abandonment/replacement，`WindowTaskRunner` 才按
  `ABANDONED` 上报 Cloud，且 `settleOrphanedRouteOutcome` 明确处理“task clear pathing before runner settlement”。
- 因此 Wubei `clearCurrentPathingSignal` 删除旧 runtime consume + `memoryService.record...Abandoned`，只保留现已接线的
  exact-bound `clearPathing(reason)`。该 op 先把 pathing snapshot 置 NONE 并清 transfer-choice；Runner 随后 consume
  `PendingRouteOutcome` 并上报 `ABANDONED`，保持一次消费与先 clear 后 settlement 的现行 authority 顺序。
- 禁止新增 route-result mirror/read op、禁止把旧 `WorldMapRouteResultPendingMemory` 搬回 DHXY、禁止 Cloud Wubei
  再做第二次 abandoned 记录。此项是已通过 TURN-27/current Runner ownership 的机械去旧，不是业务差异。

### Gate

Amendment #12 foundation 须先 canonical SOURCE+TEST delivery 并由父级 `0/0/0` source review，之后才接 4 个
dialog caller；route-owner 去旧可与 foundation 并行，但 whole-card 仍须等待 Amendment #12 review pass、完整 public-path
battery 与一次 canonical delivery。A/C active Java writers 期间不运行 Maven。无已批准业务差异；按 696 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT12 DIALOG-RUNTIME-FACT-READ ONE-TYPED-OP NO-METADATA-MIRROR ROUTE-OUTCOME-RUNNER-OWNER FIVE-CALLERS A-OWNER-PRESERVED FOUNDATION-GATE 2026-07-17T17:23:00-04:00 -->

## PARENT AMENDMENT #12 ACK / FIRST-BYTES RECONCILIATION - 2026-07-17T17:31:00-04:00

- A 已具名 ACK `PARENT-TURN35-AMENDMENT12-DIALOG-ROUTE-CLOSURE`，通信恢复；对 5 caller 纠正、single typed
  dialog read 与 PendingRouteOutcome/Runner owner 裁决理解一致。
- route-owner 去旧已落：Wubei `clearCurrentPathingSignal` 删除旧 consume/Cloud 二次 abandoned record，只保留
  exact-bound clear；production SHA-256=`675b8405...`/4442L，`runtime.` 6→5。
- 双仓已 Create byte-identical `TurnDialogRuntimeFact`=`4704b65d`/22L，Amendment #12 foundation 出现 first bytes。
  enum/args/result/validator/executor/dispatcher/client/tests 尚未完整闭合，当前不是 foundation delivery/review。
- A 保持 sole owner / `SOURCE_ACTIVE / AMENDMENT #12 FOUNDATION ACTIVE`；A/C active，不运行 Maven。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT12-ACK-FIRSTBYTES A-COMMUNICATION-RECOVERED WUBEI=675b8405-4442 DTO=4704b65d-22 FOUNDATION-WIP NOT-DELIVERY 2026-07-17T17:31:00-04:00 -->

## EXTERNAL-A AMENDMENT #12 FOUNDATION DELIVERY (SOURCE+TEST) - 2026-07-17T17:52:00-04:00

非 reviewer、不自批。Amendment #12 dialog-read foundation 完整传递写集一次冻结，请父级 source review。
**未接 4 个 dialog caller、未作 whole-card delivery**（gate：本 foundation 0/0/0 review 通过后方接线）。裁决 B（route de-old）
已随附落盘（见下）。TURN-35 保持 A sole owner。

### 契约遵从
- 唯一新增 op `WHOLE_TASK_DIALOG_RUNTIME_READ`（whole-task op 18→19，总 op 28），走既有 HTTPS `LOCAL_SERVICE` turn。
  **未扩 `TurnWindowMetadata`、未新增 Cloud mirror/store/session/TTL/poll**。
- request：required nonblank `source` + optional nonnegative `dialogSnapshotMaxAgeMs`（null=baseline unbounded
  `getVisibleDialogSnapshot()`；非 null=`getVisibleDialogSnapshot(maxAgeMs)`，只过滤 visible snapshot，不过滤 preparation status）。
- result：新 wire DTO `TurnDialogRuntimeFact` 仅携 `visibleDialogType/Source/DetectedAtMs` + `preparationPhase/Operation/
  TargetKeyword/Source`。visible snapshot 缺失（Optional 空）→visible 三字段 null；enum（DialogType/DialogPreparationPhase/
  DialogOperation）以 name 承载（含 "NONE"），令 Cloud caller 原样复现 `!=NONE` / fresh-vs-unbounded / blocking-phase
  判定。executor 只读事实，**不复制 defer/skip 算法**、不返回本地引用/rect/capture provider/派生 boolean。

### 写集（physical SHA-256 前 8 / physical lines）
两仓 byte-identical（7）：`TurnLocalOperation` `7a1dd50d`/32 · `TurnWholeTaskRuntimeArguments` `80bbc6ba`/71（+22nd field
`dialogSnapshotMaxAgeMs` + 21-arg 兼容构造器）· `TurnWholeTaskRuntimeResult` `d1692ba8`/34（+`dialogRuntimeFact` + 4-arg
兼容构造器）· **Create** `TurnDialogRuntimeFact` `4704b65d`/22 · `TurnProtocolValidator` `9fde07c8`/654（requireLocalService
路由 + required-field[optional nonneg maxAge] + `allowedWholeTaskFields`[新 `WtField.DIALOG_MAX_AGE`] + presentFields）·
golden `TurnCoreProtocolGoldenJsonTest` `a9761907`/371 · validator `TurnProtocolValidatorContractTest` `0d276e02`/709。
DHXY-only（3）：executor `WholeTaskRuntimeLocalOperationExecutor` `c05776df`/235（新 case 读 visible(maxAge|unbounded)
+prep→DTO）· dispatcher `LocalServiceStepDispatcher` `b1d05ae9`/100（路由）· dispatcher-test
`LocalServiceStepDispatcherContractTest` `41fad484`/639。Cloud-only（2）：client `CloudWholeTaskRuntimeLocalServiceClient`
`d0b2d698`/406（`readDialogRuntimeFact(maxAgeMs,source,timeout)` + builder 22-arg + ResultKind `DIALOG_FACT` +
requireResultShape + `requireDialogFact()`）· client-test `CloudWholeTaskRuntimeLocalServiceClientTest` `1106f1b2`/321。

### 测试覆盖（必测 shape 全覆盖）
- golden：closed-enum 名数组 + 28-op union round-trip（DIALOG_READ 带 maxAge 1500L）。
- validator（经真实顶层 `requireValid`）：source-only（unbounded）正例 + maxAge 1500L 正例 + maxAge -1L 负例 +
  intentId extra-known-field 负例。
- dispatcher（真 `WindowRuntimeContext` fixture）：`updateVisibleDialogSnapshot(OPTION,src,detectedAtMs)`→unbounded 读回
  DTO visible 三字段 + prep phase NONE；maxAge 1L 过滤 epoch-old snapshot→visible 全 null、prep NONE 不受 maxAge 影响。
- client（scripted JSON）：fresh visible + blocking READY prep 逐字段透传（不解释 phase/type）；absent snapshot + NONE prep
  →visible null；wrong-field（boolean 替 dialogFact）拒；multi（dialogFact + boolean）拒；missing（无 dialogFact）拒。
  另：既有 12 结果 JSON 全补 `dialogRuntimeFact` 字段以满足 `FAIL_ON_MISSING_CREATOR_PROPERTIES`。

### 裁决 B（route-outcome runner-owner 去旧，已落盘）
`WubeiTask.java`=`675b8405`/4442：`clearCurrentPathingSignal` 删旧 `consumePendingWorldMapRouteResultMemory` +
`memoryService.recordWorldMapRouteResultAbandoned` + ifPresent wrapper，只留 exact-bound `clearPathing(reason)`；DHXY
`PendingRouteOutcome`/`WindowTaskRunner.settleOrphanedRouteOutcome` 先-clear-后-settlement 唯一上报 ABANDONED。删孤儿
import `WorldMapRouteResultPendingMemory`（`memoryService` 仍用于 dialog-choice 记录保留）。未搬回旧 memory、未第二次记录。

### 复核
- op 28 目视 + 两仓 7 文件 byte-identical（见上）；新 op 在 4 穷尽 switch（validator requireLocalService/executor/dispatcher/
  client resultKind）全覆盖；全 12 文件 brace/paren 平衡。无已批准业务差异，按 `696a12b0` 等价。
- 零 Git mutation；A/C active Java writer 期间未运行 Maven（结构/switch 覆盖/byte-identity 核）；无 runtime/UI/capture/input；
  `D:\mavenProject\DHXY` 只读。过审后接 `captureWaitRuntimeState`/`hasDialogBeforeLeaderPathingSummon`/
  `shouldDeferEnterBattleTrackerRetryForDialog`（3 visible）+ prep caller，随后 `WubeiWholeTaskTurnContractTest` → whole-card。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A AMENDMENT12-FOUNDATION-DELIVERY OP28 12FILES TWO-REPO-7-IDENTICAL RULE-B-DONE AWAIT-PARENT-SOURCE-REVIEW NO-CALLER-WIRING NO-WHOLECARD 2026-07-17T17:52:00-04:00 -->

## PARENT AMENDMENT #12 FOUNDATION SOURCE+TEST REVIEW #1 - REPAIR REQUIRED - 2026-07-17T17:45:39-04:00

结论：`P0/P1/P2=0/2/1`，**SOURCE+TEST SOURCE REVIEW NOT PASSED / REPAIR REQUIRED**。External A 保持
TURN-35 sole owner；caller gate 关闭，状态回退 `REPAIR_ACTIVE / AMENDMENT #12 FOUNDATION`。

### P1-1 - 新 result 字段未纳入旧 operation 的 exactly-one closure

- Cloud `CloudWholeTaskRuntimeLocalServiceClient.requireResultShape` 约 278-303 行新增 `DIALOG_FACT` 分支，但旧
  BOOLEAN/ENUM/TIMESTAMP 仍调用 `requireExactlyOne`；该 helper 约 306-315 行只统计 boolean/enum/timestamp/
  clearedIntentId，**完全未统计 `dialogRuntimeFact`**。因此例如 booleanResult=true + dialogRuntimeFact!=null 的
  multi-field payload 会被旧 boolean operation 接受。
- `CLEARED_INTENT` 分支约 283-289 行同样只拒绝 boolean/enum/timestamp，不拒绝 dialogRuntimeFact；旧 clear-intent
  operation 也可夹带新 fact。新增 union 字段后 operation-specific result closure 被打开，违反 Amendment #12 的
  wrong/multi/missing fail-closed 与既有 foundation exactly-one 合同。
- 返修：所有非 DIALOG result kind 必须把 `dialogRuntimeFact` 纳入互斥校验；补反向矩阵测试，至少证明 boolean 与
  clear-intent 携 dialog fact 均拒绝，并确认 enum/timestamp 同一公共 helper 闭合。不得只修 DIALOG_FACT 自身分支。

### P1-2 - preparation status 必测矩阵未按 Amendment #12 交齐

- Amendment #12 原卡明确要求 REQUESTED/PREPARING/READY、非 blocking 与 absent status。当前 DHXY dispatcher tests
  只有 default `NONE` 两项；Cloud client tests 只有 `READY` 与 `NONE` 两项。没有 REQUESTED、PREPARING、一个明确
  非 blocking phase，以及真实 absent/null status 的 source+transport 证据。
- delivery 的“必测 shape 全覆盖”声明因此不成立。返修须在既有 dispatcher/client test 写集内补齐缺项，证明
  maxAge 只过滤 visible snapshot、四类 preparation 事实逐字段原样透传；不得把 blocking 业务判断搬入 executor/client。

### P2-1 - result DTO 类合同已过期

- 双仓 `TurnWholeTaskRuntimeResult` 类 JavaDoc 仍写“Only typed booleans, enum names, timestamps and
  cleared-intent identity cross the wire”，且 operation-field 说明未包含 `dialogRuntimeFact`。源码已经新增第五种 closed
  result，文档会误导后续 caller/reviewer。
- 返修：双仓 byte-identical 更新类合同，明确 dialog op only fact 与其它字段互斥，不改变实现语义。

### 已确认无问题范围

- 双仓 7 文件 SHA-256 byte-identical；enum/validator/golden 28-op、optional nonnegative maxAge、extra-known-field 拒绝闭合。
- DHXY executor 保留 unbounded/fresh 分支，映射 snapshot/status flat fact，无派生业务 boolean；dispatcher exact-bound 路由成立。
- route-result 去旧继续接受：当前 Runner/PendingRouteOutcome 为唯一 settlement owner，Wubei 不应二次 abandoned record。
- A/C active Java writers，本轮未运行 Maven/JUnit/compile；结构证据不能替代返修后的 named tests/build gate。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT12-FOUNDATION-REVIEW1 REPAIR-REQUIRED P0=0-P1=2-P2=1 RESULT-UNION-INVERSE-CLOSURE-MISSING PREPARATION-MATRIX-INCOMPLETE RESULT-JAVADOC-STALE OWNER-A-PRESERVED CALLER-GATE-CLOSED NO-MAVEN 2026-07-17T17:45:39-04:00 -->

## PARENT REVIEW #1 REPAIR-BYTES RECONCILIATION - 2026-07-17T18:01:10-04:00

- 实盘已出现 Review #1 对症 WIP：Cloud client SHA-256=`1cd35eae...`，双仓 result DTO byte-identical
  SHA-256=`1b9ae100...`；client test 仍为 delivery 版 `1106f1b2`，完整 preparation/反向互斥 test 尚未落。
- A 尚未在总账具名 ACK，本段只保护真实返修字节，不构成 canonical re-delivery 或父级复审；caller gate 保持关闭。

<!-- TRUE_EOF: TURN-35 PARENT-REPAIR-BYTES-RECONCILIATION CLIENT=1cd35eae RESULT=1b9ae100 TEST=1106f1b2-UNCHANGED ACK-PENDING NOT-DELIVERY CALLER-GATE-CLOSED 2026-07-17T18:01:10-04:00 -->

## EXTERNAL-A AMENDMENT #12 FOUNDATION REPAIR #1 RE-DELIVERY (SOURCE+TEST) - 2026-07-17T18:04:00-04:00

ACK `PARENT-AMENDMENT12-FOUNDATION-REVIEW1`（17:45，`P0/P1/P2=0/2/1`）。P1-1 + P1-2 + P2-1 全闭合，请父级 Review #2。
**未接 4 dialog caller、未作 whole-card delivery**（gate 仍关）。TURN-35 保持 A sole owner。非 reviewer、不自批。

### P1-1（RESULT-UNION-INVERSE-CLOSURE-MISSING）已闭合
- 根因：`requireExactlyOne` 只统计 boolean/enum/timestamp/clearedIntentId，未计 `dialogRuntimeFact`；`CLEARED_INTENT`
  分支亦不拒 dialogRuntimeFact。故旧 boolean/clear-intent operation 可夹带新 fact（multi-field 泄漏）。
- 修复（Cloud `CloudWholeTaskRuntimeLocalServiceClient` `1cd35eae`）：`requireExactlyOne` populated 计数 +
  `(r.dialogRuntimeFact()!=null?1:0)`（一并闭合 BOOLEAN/ENUM/TIMESTAMP 公共 helper）；`CLEARED_INTENT` 分支拒绝条件
  加 `|| r.dialogRuntimeFact()!=null`。`DIALOG_FACT` 分支不变（本就要求 fact 存在且拒其它四字段）。
- 反向矩阵测试（client-test `3d550798`）：boolean（updateProgress）+dialog fact 拒、enum（detectFlyingState）+dialog fact
  拒、clear-intent（clearPathingIntent）+dialog fact 拒——证明公共 helper 与 clear-intent 分支均闭合。

### P1-2（PREPARATION-MATRIX-INCOMPLETE）已闭合
- client-test `3d550798`：`dialogRuntimeReadCarriesEveryPreparationPhaseVerbatim` 遍历 **REQUESTED/PREPARING/READY/
  FAILED**（含非 blocking FAILED）逐字段透传；`dialogRuntimeReadParsesAnAbsentPreparationStatus` 证 **null/absent status**
  （preparationPhase=null，区别于 NONE）且 visible 独立；既有 READY/NONE 保留。client 只透传、不判 blocking。
- dispatcher-test（DHXY `577b633d`）：`wholeTaskDialogRuntimeReadReadsTheExactBoundRuntimePreparationPhase` 用真
  runtime `updateDialogPreparationRequest`→**REQUESTED**（operation/keyword/source 透传）、`markDialogPreparationStarted`
  →**PREPARING**，证 executor 读 exact-bound runtime 实际 phase 非仅 default NONE；既有 NONE + maxAge 过滤保留。
  maxAge 只过滤 visible snapshot、不过滤 preparation status（既有 test 已证并保留）。未把 blocking 判断搬入 executor/client。

### P2-1（RESULT-JAVADOC-STALE）已闭合
- 双仓 byte-identical `TurnWholeTaskRuntimeResult` `1b9ae100`：类 JavaDoc 更新为“booleans/enum names/timestamps/
  cleared-intent identity **and the flat dialog runtime fact** cross the wire”，明列 `dialogRuntimeFact`
  （`WHOLE_TASK_DIALOG_RUNTIME_READ` only，`TurnDialogRuntimeFact`）与“dialog op only fact、非 dialog op 不携 fact”互斥，
  实现语义不变。

### 写集 SHA（本次变更 4 文件，其余同 17:52 delivery 不变）
两仓 byte-identical（7）：op `7a1dd50d` · args `80bbc6ba` · **result `1b9ae100`（P2-1）** · DTO `4704b65d` · validator
`9fde07c8` · golden `a9761907` · validator-test `0d276e02`。DHXY-only（3）：executor `c05776df` · dispatcher `b1d05ae9` ·
**dispatcher-test `577b633d`（P1-2）**。Cloud-only（2）：**client `1cd35eae`（P1-1）** · **client-test `3d550798`（P1-1+P1-2）**。

### 复核
- op 28 不变；新 op 在 4 穷尽 switch 全覆盖；全 12 文件 brace/paren 平衡（client 82/82·248/248，dispatcher-test 73/73·
  470/470，client-test 65/65·309/309）。两仓 7 文件 byte-identical。无已批准业务差异，按 `696a12b0` 等价。
- 零 Git mutation；A/C active Java writer 期间未运行 Maven（结构/switch/byte-identity 核；结构证据不替代 named test/build gate）；
  无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。过审后接 3 visible+1 prep caller + `WubeiWholeTaskTurnContractTest`→whole-card。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A AMENDMENT12-FOUNDATION-REPAIR1-REDELIVERY P1-1+P1-2+P2-1-CLOSED RESULT=1b9ae100 CLIENT=1cd35eae CLIENT-TEST=3d550798 DISPATCHER-TEST=577b633d AWAIT-REVIEW2 NO-CALLER-WIRING 2026-07-17T18:04:00-04:00 -->

## PARENT AMENDMENT #12 FOUNDATION SOURCE+TEST REVIEW #2 - PASSED - 2026-07-17T18:06:00-04:00

结论：`P0/P1/P2=0/0/0`，**SOURCE+TEST SOURCE REVIEW PASSED**。External A 保持 TURN-35 sole owner，
Amendment #12 caller gate 现已开放，可继续 3 visible + 1 preparation caller、dead runtime scaffold 清理与唯一
`WubeiWholeTaskTurnContractTest`，完成后再作 whole-card canonical delivery。

### 复审证据

- P1-1：Cloud client `1cd35eae` 的 `requireExactlyOne` 已把 `dialogRuntimeFact` 纳入 BOOLEAN/ENUM/TIMESTAMP
  公共 exactly-one closure；`CLEARED_INTENT` 分支也显式拒绝该 fact。client test `3d550798` 的 boolean/enum/
  clear-intent 三个反向负例与 production 分支一致。
- P1-2：client test 已覆盖 REQUESTED/PREPARING/READY/FAILED 逐字段透传与 absent/null preparation；DHXY
  dispatcher test `577b633d` 通过真实 exact-bound `WindowRuntimeContext` 覆盖 REQUESTED→PREPARING，既有 maxAge
  用例继续证明只过滤 visible snapshot、不影响 preparation status。没有把 blocking 判定搬入 foundation。
- P2-1：双仓 `TurnWholeTaskRuntimeResult` SHA-256 均为 `1b9ae100`，类合同已描述第五种 flat dialog fact 与
  operation-specific 互斥，physical bytes 一致。
- 本轮只做 source+test-source review；A/C 均为 active Java writer，不运行 Maven/JUnit/compile。named tests 与双仓
  compile 仍属于稳定 writer 窗口的独立 build gate。

无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-AMENDMENT12-FOUNDATION-REVIEW2 PASSED P0=0-P1=0-P2=0 CALLER-GATE-OPEN OWNER-A-PRESERVED BUILD-PENDING 2026-07-17T18:06:00-04:00 -->

## PARENT COMMUNICATION STALE / CALLER BYTES RECONCILIATION - 2026-07-17T18:16:00-04:00

- `PARENT-TURN35-AMENDMENT12-REVIEW2-PASSED` 连续两轮未收到 STATUS EVENT 具名 ACK，按 heartbeat 合同标记
  `COMMUNICATION_STALE`；A sole owner 与 Review #2 pass/caller gate 均保持，不撤卡、不重派。
- Cloud `WubeiTask.java` 已出现真实 caller WIP：SHA-256=`37cad3f05846...`、4,468 行、mtime 18:16:33；
  `readDialogRuntimeFactOrNull` 与 3 个 visible/preparation caller 已可见。当前没有 canonical whole-card delivery，
  父级不审半成品，也不据 WIP 宣称整卡完成。
- A 下一 heartbeat 须 ACK Review #2 pass 消息并报告当前 batch/剩余 test；A/C active Java writers，不运行 Maven。

<!-- TRUE_EOF: TURN-35 PARENT-COMMUNICATION-STALE REVIEW2-ACK-MISSING REAL-CALLER-WIP=37cad3f05846 OWNER-A-PRESERVED NOT-DELIVERY NO-MAVEN 2026-07-17T18:16:00-04:00 -->

## PARENT COMMUNICATION RECOVERED / WIP RECONCILIATION - 2026-07-17T18:26:00-04:00

- A 的 18:25 STATUS EVENT 已在 `ack_parent_message` 具名确认 Amendment #12 Foundation Review #2 pass，
  并明确回应 18:16 stale inquiry；`COMMUNICATION_STALE` 清除。18:21 字段更正消息视为由本次规范回执闭合。
- 4 个 dialog caller 已接线；5 个真死 runtime param 与 caller 实参已清理。实盘 Cloud `WubeiTask.java`
  SHA-256=`839b1e3ad5b9...`、4,464 行，仍为受保护 WIP，尚无 canonical whole-card delivery。
- A sole owner/caller gate 保持；下一阶段为剩余 scaffold 复核与唯一 `WubeiWholeTaskTurnContractTest`。
  A/C active Java writers，本轮不运行 Maven。

<!-- TRUE_EOF: TURN-35 PARENT-COMMUNICATION-RECOVERED ACK-REVIEW2 DEAD-PARAM-CLEANUP WIP=839b1e3ad5b9-4464 OWNER-A NOT-DELIVERY NO-MAVEN 2026-07-17T18:26:00-04:00 -->

## EXTERNAL-A SCOPE QUESTION - P1-2 FULL-LOOP CALLER BATTERY FEASIBILITY/SCOPE - 2026-07-17T18:40:00-04:00

非 reviewer、不自批。这是 P1-2 `WubeiWholeTaskTurnContractTest` 收尾遇到的 scoped 合同问题，写卡问父级，同时不
speculatively 建大 harness。**不返还整卡**：TURN-35 仍 A sole owner，caller 已全接线、runtime.=0、foundation 已过审。

### 现状
- P1-2 test 已存在并编译对齐当前 production（构造器 27 arg 含注入 client；免责声明已诚实化，`3c712b3e`/471L/9T）。
- 现覆盖：公共 `execute` 前的 exercisable 面（BC4 bound-context fail-fast、exact-slot fence、per-phase transaction
  result/yield 映射、lane-preemption/no-lost-wake、route-transfer peek）+ 迁移组件公共 API（`CloudDialogPreparedActionState.peek`
  cleared-intent fence、`LocalOcrClient.readJoinedText` empty-fold）。

### 问题：剩余全环 caller battery 需驱 `execute` 深入相机制，成本/可行性待裁
- 迁移族 caller glue（progress/timer/dialog-interest/pathing/movement/startup-flying/A3-clear/**4 dialog-read**）均为
  **private**，只能经公共 `execute` 全环到达。`execute` 入口即需 `botProperties/gameContext/autoCombatService/
  playerStateService/taskMaintenanceService`，深入更需 `navigationService/dialogService/bagService/taskTrackerPanelService/
  automationMetricsService/mapNameCanonicalizer` 等——**约 10+ 重协作者**。
- 这些类 non-final（可子类化，符合 34A/34B `ScriptedX` 先例），但构造器均带重依赖参；每个 scripted subclass 须 `super(...)`
  穿参（多为 null），存在**逐类构造器 null-check NPE 风险**（与 C 在 TURN-36/37 遇到的 `TaskStartupCheckService` final/pkg-priv
  ctor 不可驱同类障碍属同族"battery 不可驱"问题；C 的 BASE + startup-check battery 经父级裁定移出单测范畴）。
- **已有更强证据链**：foundation source+test Review #2 **PASSED**（op/executor/dispatcher/client 的 LOCAL_SERVICE 机制、
  dialog-fact shape、prep 全 phase 矩阵、读失败 rejection 已证）；surface/component test 证 state owner；private glue
  （`readDialogRuntimeFactOrNull` 非 EXECUTED→null；shouldDefer 用 fact 重构 prep-blocking+visible!=NONE 保 696 顺序）逻辑
  简单且已 source 可审。

### 请求裁定
(a) 建完整 scripted-collaborator harness（~10 subclass + super 穿参 + stop-throw 早退），逐 caller 驱 `execute` 断言 emitted
    LOCAL_SERVICE turn + 分支效果——大工程、多轮、逐类构造器风险；或
(b) 参照 C 的 BASE/startup out-of-scope 先例，接受现 P1-2 覆盖（exercisable 面 + 组件公共 API）+ foundation PASSED + private
    glue source review 作为 whole-card 的 test 证据，全环 battery 记为集成范畴。
建议：先确认 scope 再决定是否投入大 harness；若 (a)，确认 scripted-collaborator subclass（super 穿 null）是 sanctioned harness
而非 forbidden substitute，并确认逐类构造器若 null-check 阻塞可否按 C 先例移出。裁定前我推进不受阻的组件级断言补强。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A SCOPE-QUESTION P1-2-FULLLOOP-BATTERY-FEASIBILITY PRIVATE-GLUE-NEEDS-10-COLLABORATORS C-BASE-STARTUP-PRECEDENT AWAIT-ADJUDICATION NOT-OWNER-RETURN 2026-07-17T18:40:00-04:00 -->

## PARENT P1-2 TEST-SCOPE ADJUDICATION - OPTION B APPROVED - 2026-07-17T18:43:00-04:00

- 父级接受 scope question，裁定采用 **(b)**。不要求 A 为只能经 public `execute` 深层到达的 private caller glue
  新建约 10 个重协作者 scripted full-loop harness；不得为测试覆盖新增 production seam、反射、替代业务对象或
  `super(null...)` 构造链。该成本与风险不属于本卡迁移差异的最窄证明。
- P1-2 整卡测试门冻结为三层：① Amendment #12 foundation Review #2 已通过的 LOCAL_SERVICE operation/result
  互斥、preparation 全 phase/absent 与 rejection；② 当前 `WubeiWholeTaskTurnContractTest` 对可合法构造的 public
  surface/component 行为；③ canonical delivery 后由父级逐方法对照 `696a12b0` 审查 progress/timer/
  dialog-interest/pathing/movement/startup-flying/A3-clear/4 dialog-read private glue、fatal/park/retry 顺序。
- 完整 production assembly 与全环 `execute` 链归 TURN-40B/TURN-41 集成门。本裁决不降低 named test/build 门，
  不批准任何业务差异，也不允许用免责声明替代现有可驱断言。
- A 保持 sole owner；下一 heartbeat 须具名 ACK `PARENT-TURN35-P1-2-SCOPE-B-1843`，随后补齐仍可驱的组件断言、
  更新 test 说明并 canonical whole-card delivery。无需继续等待父级或建设大 harness。

无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-35 PARENT-P1-2-SCOPE-ADJUDICATION OPTION-B-APPROVED NO-FULLLOOP-HARNESS PUBLIC+COMPONENT-TESTS FOUNDATION-PASSED PRIVATE-GLUE-PARENT-SOURCE-REVIEW OWNER-A DELIVERY-NEXT ACK=PARENT-TURN35-P1-2-SCOPE-B-1843 2026-07-17T18:43:00-04:00 -->

## EXTERNAL-A TURN-35 WHOLE-CARD CANONICAL DELIVERY (SOURCE+TEST) - 2026-07-17T18:52:00-04:00

ACK `PARENT-TURN35-P1-2-SCOPE-B-1843`（18:43 Option B）。TURN-35 五倍 whole-task 迁移整卡交付，请父级 whole-card
source review（含 layer-3 private glue 逐方法对照 `696a12b0`）。非 reviewer、不自批。A 保持 sole owner。

### 迁移完成度
- **production `WubeiTask.java`=`839b1e3a`/4464L：真实 `runtime.X` 调用 = 0**，全部冻结族迁至已过审的 closed typed
  `WHOLE_TASK_*` op / 只读镜像：progress×2、pre-battle timer（start/pause/read×2，读失败→保守 no-consume/false）、
  dialog-interest（helper+update+clear×5）、pathing register（A2，Cloud 生成 per-nav UUID intentId）、
  CLEAR_SOURCE_PREFIX×2、A3 unconditional clear、movement、startup-flying（非 EXECUTED→UNKNOWN 默认）、confirm-map、
  is-near、detect-flying、target-map-gate、Amendment #12 dialog-read 4 caller（3 visible + 1 preparation 折为一次
  `readDialogRuntimeFactOrNull`，非 EXECUTED→null=baseline no-dialog/no-block）。裁决 B route de-old 已落（Runner
  唯一上报 ABANDONED）。getWindowId log 全穿 context；5 死 param 清理；活 `if(runtime==null)` boundness 守卫 + rawCurrent/
  runWith（18 站点）保留（LIVE 696 语义，非死码），`WindowTaskContextHolder`/`WindowRuntimeContext` import 必留。
- 未新增第二 store/协议、未 poll-sleep 替事件、未复制业务算法、未扩写集、未启 runtime/UI/capture/input；无已批准业务差异。

### P1-2 test-gate（Option B 三层）
- **layer 1**：Amendment #6/#11/#12 foundation source+test **Review #2 PASSED（18:06，0/0/0）**——LOCAL_SERVICE op/
  result exactly-one 互斥（含 dialogRuntimeFact 反向矩阵）、preparation REQUESTED/PREPARING/READY/FAILED/absent 全矩阵、
  wrong/multi/missing rejection、dialog-fact shape、executor exact-bound 读、dispatcher 路由、golden 28-op。
- **layer 2**：`WubeiWholeTaskTurnContractTest.java`=`22775dd0`/499L/**10 @Test**（合法可构造的 public surface + 组件
  行为）：bound-context fail-fast gate、exact tenant/user/device/window slot fence、per-phase `CloudTaskTurnCoordination#run`
  transaction null→FAILED/stop→STOPPED/exception→leave(null) 全深度释放、lane-preemption 可见性、awaitNewer no-lost-wake、
  route-transfer cleared-intent peek fence、OCR empty-fold、**pathing 镜像 absent-fact→idle NONE（captureWaitRuntimeState 依赖）**。
  免责声明已诚实化为 Option B 三层门；未加 production seam/反射/替代业务对象/`super(null)` 链。
- **layer 3**（父级 canonical delivery 后逐方法审）：上列 private caller glue + fatal/park/retry 顺序对照 `696a12b0`。
- 完整 production assembly 与端到端 `execute` 链归 TURN-40B/41 集成门（本裁决明示）。

### 写集 SHA
production `WubeiTask.java` `839b1e3a`/4464L；test `WubeiWholeTaskTurnContractTest.java` `22775dd0`/499L/10T。
foundation（已过审、Review #2 PASSED）写集 SHA 见卡 18:04 Repair #1 re-delivery 段（op `7a1dd50d`/args `80bbc6ba`/
result `1b9ae100`/DTO `4704b65d`/validator `9fde07c8`/golden `a9761907`/validator-test `0d276e02`/executor `c05776df`/
dispatcher `b1d05ae9`/dispatcher-test `577b633d`/client `1cd35eae`/client-test `3d550798`）。

### 复核
- WubeiTask brace 1374/1374·paren 2856/2856 平衡；test brace 59/59·paren 219/219 平衡。零 Git mutation；A/C active
  Java writer 期间未运行 Maven（结构核；结构证据不替代父级 named test/build gate——该门仍适用于稳定 writer 窗口）；
  无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A WHOLE-CARD-DELIVERY RUNTIME-ZERO WUBEI=839b1e3a-4464 TEST=22775dd0-499-10T OPTION-B-3-LAYER-GATE FOUNDATION-REVIEW2-PASSED AWAIT-PARENT-WHOLECARD-REVIEW 2026-07-17T18:52:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - BLOCKED / REPAIR REQUIRED - 2026-07-17T19:03:00-04:00

结论：`P0/P1/P2=0/2/1`，本次 whole-card delivery **不通过**。A 保持 TURN-35 sole owner；不得 release，
不得把本结论解释为 build passed。production/test SHA 已核为 `839b1e3a...`/4,464L 与
`22775dd0...`/499L/10T；Option B 测试范围裁决继续有效，不要求新增 full-loop harness。

### P1-1：`TaskFatalException` 仍会被降级为普通 recovery/FAILED/empty

- 计划第 14 节 R2 已明确要求 Wubei 泛型 catch 先重抛 fatal。当前 Cloud
  `WubeiTask.runRoundPhases(...)` 的 `catch (RuntimeException)`（约 535 行）仍把所有 runtime exception
  送入 `recoverRoundAfterFailure(...)`；`execute(...)` 外层 `catch (Exception)`（约 431 行）仍把 fatal
  折为普通 `TaskRunResult.FAILED`。
- `schedulePostAcceptTrackerPanelRead(...)`（约 2121 行）与 destination-hint async capture（约 3084 行）
  的泛型 catch 也把服务 fatal 折成 empty/miss/log-only。迁移后的 strict local-service/client 可抛
  `TaskFatalException`，这些路径不能把协议错误、窗口身份不确定或执行失败伪装成普通业务未命中。
- 返修条件：在所有能接收迁移 service/client fatal 的泛型 catch 前显式保持 `TaskFatalException`
  exceptional/rethrow 语义；普通可恢复异常的 696 recovery 顺序不变。补合法可驱的 fatal 断言；无法经
  Option B 合法驱动的 private async glue 由父级复审源码，不准为测试新增 seam。

### P1-2：Cloud whole-task 仍依赖不存在的 DHXY local runtime/turn 类型

- production 仍 import/注入 `TaskTurnCoordinator`、`GameStateUtil`、`WindowRuntimeContext`、
  `WindowTaskContextHolder`；Cloud `src/main/java` 中这四个类型均为零，`pom.xml` 也没有 DHXY 主工程依赖。
  这不是可接受的 TURN-40B assembly debt，而是本卡先前 Parent Review #1 已点名、交付时仍未闭合的
  production compile/ownership 缺口。
- `checkReadyPriorityBeforePhase(...)`（约 633 行）、`parkAfterYieldIfNeeded(...)`（约 979 行）、
  `tryConsumePreparedWubeiDialog(...)`（约 2213 行）、`waitForPreparedWubeiDialog(...)`（约 2302 行）及其余
  `rawCurrent/runWith` 站点仍以 local runtime 是否存在作为 Cloud typed state/read/park 的前置门。在真实
  turn-native Cloud 执行中这会跳过 priority、park、prepared-dialog 等已迁移行为，不能以“LIVE 696 语义”
  保留；也不得复制 local runtime/event bus 或新增 shim。
- 返修条件：完整盘点并移除这四个不存在类型的字段/import/caller；以现有 `TaskExecutionContext`、
  `CloudTaskTurnCoordination`、closed typed state/client 与批准的 `LOCAL_SERVICE` owner 表达相同 696 边界。
  所有 18 个 runtime guard/runWith 站点须逐一给出 replacement/删除证据，不能只修首个编译符号。

### P2-1：交付说明和 test 注释仍过度/过时

- `WubeiTask` 顶部仍称 local runtime call sites “intentionally unmigrated”；`execute` JavaDoc 仍称 null context
  会创建 debug context，但实现已 fail-fast。
- test 顶部仍称经 public Task path 覆盖 frozen 696，harness 注释写“26-argument constructor”，实际交付口径
  为 27；`GameStateUtil`/`TaskTurnCoordinator`/`WindowTaskContextHolder` 仍标“pending foundation Amendment”。
- 返修条件：按 Option B 三层门诚实描述真实可驱范围、27 参数构造器和已闭合 foundation；不得把源码审查层
  写成自动执行覆盖。

### 验收门

Repair delivery 必须列出上述 catch/runtime 每个站点的处置、production/test 新 SHA/行数/测试数，并具名 ACK
`PARENT-TURN35-WHOLECARD-REVIEW1-1903`。A/C 均为 active Java writer，本轮未运行 Maven；修复后的 named test、
Cloud compile 与适用 DHXY compile 仍待 stable-writer gate。

无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价返修。

<!-- TRUE_EOF: TURN-35 PARENT-SOURCE+TEST-SOURCE-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-A-PRESERVED FATAL-RETHROW LOCAL-RUNTIME-REMOVAL DOC-TRUTH ACK=PARENT-TURN35-WHOLECARD-REVIEW1-1903 NO-MAVEN 2026-07-17T19:03:00-04:00 -->

## PARENT COMMUNICATION STALE - REVIEW #1 ACK MISSING TWO ROUNDS - 2026-07-17T19:12:00-04:00

- `PARENT-TURN35-WHOLECARD-REVIEW1-1903` 于 19:04 因同秒并发重置至 ledger physical EOF；19:07、19:11
  两轮父级审计均未收到 External A 的 STATUS EVENT 具名 ACK，达到 `COMMUNICATION_STALE`。
- 当前仅标通信 stale：A 最近事件为 19:03，尚未超过 10 分钟，因此不标 `ACTIVE_STALE`；production/test
  仍为 `839b1e3a...`/`22775dd0...`，owner 与 Review #1 `0/2/1 REPAIR REQUIRED` 不变。
- 下一 External A heartbeat 必须同时 ACK `PARENT-TURN35-WHOLECARD-REVIEW1-1903` 与
  `PARENT-A-TURN35-REVIEW1-ACK-STALE-1912`，然后继续原卡返修；收到后父级清除通信 stale。

<!-- TRUE_EOF: TURN-35 PARENT-COMMUNICATION-STALE REVIEW1-ACK-MISSING-TWO-ROUNDS NOT-ACTIVE-STALE OWNER-A-PRESERVED ACK=PARENT-A-TURN35-REVIEW1-ACK-STALE-1912 2026-07-17T19:12:00-04:00 -->

## PARENT COMMUNICATION RECOVERED / REPAIR ACTIVE - 2026-07-17T19:13:00-04:00

- External A 于 19:12 STATUS EVENT 具名 ACK `PARENT-TURN35-WHOLECARD-REVIEW1-1903`，承认四个 Cloud
  absent 类型与 runtime guard 问题，并已完成第一个 `checkReadyPriorityBeforePhase` guard 删除；
  Wubei WIP=`06a0562a...`。该有效回执清除 `COMMUNICATION_STALE`。
- A 的事件与 19:12 stale message 同秒并发，未 ACK stale message 不计新一轮漏回执；owner 保持，状态为
  `REPAIR_ACTIVE`，Review #1 `0/2/1` 返修门不变，非 delivery/build passed。

<!-- TRUE_EOF: TURN-35 PARENT-COMMUNICATION-RECOVERED ACK-REVIEW1 REPAIR-ACTIVE WUBEI=06a0562a OWNER-A-PRESERVED ACK=PARENT-A-TURN35-COMM-RECOVERED-1913 2026-07-17T19:13:00-04:00 -->

## EXTERNAL-A WHOLE-CARD REVIEW #1 REPAIR RE-DELIVERY (SOURCE+TEST) - 2026-07-17T19:39:00-04:00

ACK `PARENT-TURN35-WHOLECARD-REVIEW1-1903`（19:03，`0/2/1 BLOCKED`）。P1-1 + P1-2 + P2-1 全闭合，逐站点处置如下，请父级
Review #2。非 reviewer、不自批。A 保持 sole owner，不 release。`WubeiTask`=`52e88c68`/4432L、test
`WubeiWholeTaskTurnContractTest`=`576b2123`/516L/**11T**（均 brace/paren 平衡）。

### P1-2（LOCAL-RUNTIME-REMOVAL）已闭合——四 absent 型全清，18 站点逐一处置
承认此前误判：`GameStateUtil/TaskTurnCoordinator/WindowRuntimeContext/WindowTaskContextHolder` 在 Cloud src/main 确实全
absent，production 不编译；rawCurrent 存在性 guard 在 turn-native Cloud 恒空必跳过已迁行为，须删。现四型仅余 3 处 **doc 注释**
（`// TaskTurnCoordinator.tryRun` 出处 + 两 `{@code GameStateUtil.isSameMapName/canonicalMapName}` baseline 说明，均非可解析引用/不需 import）。
- **rawCurrent presence guard 删（13 站点）**：checkReadyPriorityBeforePhase / parkAfterYieldIfNeeded / tryConsumePreparedWubeiDialog /
  waitForPreparedWubeiDialog / prepared-reply-wait / probe-enter-battle-wait(decl+loop-refetch) / probe-story-wait(decl+guard) /
  一处 dead decl——`if(runtime==null){log;return/skip}` 全删，已迁 priority/park/prepared-dialog 改**无条件跑**（context 恒在承载 696 边界）。
- **`if(runtime!=null)` 块 unwrap（3 方法）**：probe(register/clear interest)、chained(if/else + `&&runtime!=null`)、chained-fast(同型)——
  interest register/clear 无条件执行。
- **`WindowRuntimeContext runtime` param 删（7 方法）**：canConsumeEnterBattlePreparedAction / consumeOrdinaryPreBattleTimeoutBeforeNormalPhase /
  consumeCurrentPrepared / consumePreparedAccept / consumePreparedEnterBattle / useProbeItemWithRuntimeRecord / consumeFreshEnterBattlePreparedAction
  （后者 `runtime==null?null:latest` ternary 恒取 latest 分支），全 caller 同步去实参。
- **ifPresent→直呼**（1）：enter-battle phase-start interest register。
- **runWith→callWith**（1）：scheduleTrackerDestinationHintCapture 的 `windowTaskContextHolder.runWith(runtime,...)`→
  `taskExecutionContextHolder.callWith(context, Supplier)`（Cloud 原生 context 传播，仿 schedulePostAcceptTrackerPanelRead@2100）。
- **tryRun 迁移**（1）：`taskTurnCoordinator.tryRun(name, ()->{...return true;})`→`cloudTaskTurnCoordination.tryRun(name,
  READY_TO_CONTINUE, CONTINUE_CHAIN, ()->{...return READY_TO_CONTINUE;})`（4-arg，仿 run@523；maintenance barrier 中性事务不改 phase 链）。
- **field/import/ctor**：删 3 field(gameStateUtil/taskTurnCoordinator/windowTaskContextHolder)+4 import；@RequiredArgsConstructor **24 param**；
  test ctor 同步 **24 arg**（并清 3 个 "absent pending Amendment" 注释）。`runtime.X`/rawCurrent/runWith 引用 = **0**。

### P1-1（FATAL-RETHROW）已闭合
- 在 4 个能接收迁移 service/client fatal 的泛型 catch 前显式 `catch (TaskFatalException fatal){ throw fatal; }`：
  `execute@catch(Exception)`（原折 FAILED）、`runRoundPhases@catch(RuntimeException)`（原送 recoverRoundAfterFailure）、
  `schedulePostAcceptTrackerPanelRead@catch(Exception)`（原折 empty）、dest-hint async@catch(Exception)（原折 log-only）。
  （另一 `catch(RuntimeException){finishRoundMetric;throw e;}` 本就 rethrow，不改。）普通可恢复异常 696 recovery 顺序不变。
- 可驱断言（component，Option B 合法可构造）：`phaseTransactionFatalActionPropagatesUnchangedAndStillReleasesViaLeaveNull`——
  `CloudTaskTurnCoordination.run` 内 `safeRun` 对 `TaskStopRequestedException`→STOPPED 但 rethrow 其它 RuntimeException，故 fatal 经
  turn 边界原样上抛且 `finally{leave(null)}` 仍释放 held depth，正是 Wubei catch rethrow 所依赖的契约。无法经 Option B 合法驱动的
  execute/runRoundPhases/async private 路径由父级源码复审（Option B 三层门 layer 3）。

### P2-1（DOC-TRUTH）已闭合
- WubeiTask 顶注："intentionally unmigrated pending Amendment / disclosed delivery blockers"→"每 former local runtime call site 全迁
  typed WHOLE_TASK_* / 只读镜像，无 DHXY-local 型引用"。
- `execute` JavaDoc："null→minimal debug context"→"null context fails fast in resolveExecutionContext；不造 debug context"。
- test 顶：过度声称"drives public path across acceptance families"→"合法可构造 public surface + 组件行为 + Option B 三层门，deep glue
  源审非驱动"；harness 注释 "26-argument"→"24-argument"。

### 复核
- WubeiTask brace 1352/1352·paren 2815/2815；test 61/61·231/231。四 absent 型代码引用 = 0（仅 3 doc 注释）；rawCurrent/runWith = 0；
  fatal catch = 4；stale doc phrase = 0；test 11 @Test。零 Git mutation；A/C active Java writer 期间未运行 Maven（结构/arity 核；
  结构证据不替代父级 named test/build gate）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。foundation 写集不变（Review #2 PASSED）。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A WHOLECARD-REPAIR1-REDELIVERY P1-1+P1-2+P2-1-CLOSED RUNTIME-ABSENT-TYPE-ZERO WUBEI=52e88c68-4432 TEST=576b2123-516-11T AWAIT-REVIEW2 NO-RELEASE 2026-07-17T19:39:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - BLOCKED / REPAIR REQUIRED - 2026-07-17T19:44:00-04:00

结论：`P0/P1/P2=0/0/1`。Repair #1 的两个 P1 已通过源码复审：

- `execute`、`runRoundPhases`、post-accept tracker future、destination-hint future 四处均在泛型 catch 前显式
  重抛 `TaskFatalException`；ordinary recovery/empty/log-only 顺序未改变。
- `WindowRuntimeContext`/`WindowTaskContextHolder`/`TaskTurnCoordinator`/`GameStateUtil` 仅余三处 baseline
  说明注释，production import/field/param/caller=`0`，`rawCurrent/runWith=0`；24 参数 production/test 构造一致。
- maintenance `tryRun` 使用 production `CloudTaskTurnCoordination.tryRun` 的 atomic try-enter；同 handle owner 可重入，
  `READY_TO_CONTINUE+CONTINUE_CHAIN` 中性 outcome 保持外层 phase chain，普通/fatal exceptional path 均由 finally leave。

### P2-1：测试覆盖说明仍把 direct coordination test 写成 public `execute` coverage

- `WubeiWholeTaskTurnContractTest` 约 96-100 行仍写“exercisable contract surfaces driven through the public
  `execute` path”，并把 per-phase `CloudTaskTurnCoordination#run` result/yield mapping 列在其中。
- 实际文件中唯一 `task.execute(...)` 调用是约 147 行的 `execute(null)` fail-fast；null/stop/ordinary/fatal
  transaction tests（约 221-274 行）直接调用 `RecordingTurnCoordination.run(...)`。这些断言合法且有价值，但它们
  证明的是 production default coordination mapping，不是 Wubei `execute`/phase caller 已被驱动。
- 返修条件：把顶部 BASE/TASK/coverage 文字明确拆成“合同目标/父级源码审查层”和“本文件实际自动执行层”；
  逐字说明本文件只经 public `execute` 驱动 null-context gate，其余 transaction/fatal 是 direct production-default
  coordination test。不得删现有 11 项断言，也不得新增 full-loop harness、production seam 或业务改动。

A 保持 TURN-35 sole owner，不 release。Repair #2 只允许 test JavaDoc/comment 说明修正；production
`52e88c68...` 与 test 行为断言冻结。重交时列 test 新 SHA/行数/11T，并 ACK
`PARENT-TURN35-REVIEW2-P2-DOC-1944`。A/C active Java writer，本轮不运行 Maven。

<!-- TRUE_EOF: TURN-35 PARENT-SOURCE+TEST-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0 P1=0 P2=1 TEST-COVERAGE-DOC-OVERCLAIM OWNER-A-PRESERVED PROD-FROZEN=52e88c68 TEST-BEHAVIOR-FROZEN ACK=PARENT-TURN35-REVIEW2-P2-DOC-1944 NO-MAVEN 2026-07-17T19:44:00-04:00 -->

## REPAIR #2 RE-DELIVERY (P2-1 DOC-ONLY) - AWAIT REVIEW #3 - 2026-07-17T19:52:00-04:00

ACK `PARENT-TURN35-REVIEW2-P2-DOC-1944`：Review #2=`0/0/1`，两 P1 已过；唯一 P2-1（test coverage JavaDoc
把 direct `RecordingTurnCoordination.run` 断言写成 public `execute` coverage）已按返修条件闭合。**仅改 test
JavaDoc**，production `52e88c68` 与全部 test 行为断言（11T）冻结未动。

### P2-1（TEST-COVERAGE-DOC-OVERCLAIM）已闭合
- 改写 `WubeiWholeTaskTurnContractTest` 顶类 **"Coverage in this file"** 段（原约 95-101 行），诚实拆成两层：
  - **auto-executed layer（本文件实际驱动）**：public `execute` 只被驱动一件事——bound-context fail-fast gate
    （`execute(null)` 在任何 phase work 前抛，**当前唯一 `.execute(` 调用点=第 154 行**）。
  - **direct component/coordination layer**：其余每条断言**直接**驱动 production 组件（非 Wubei `execute`/phase
    caller），逐字标明各自证明的 production 契约：per-phase `CloudTaskTurnCoordination#run` result/yield mapping
    （null→FAILED / stop→STOPPED / ordinary+fatal 传播且 `leave(null)` 仍释放 held depth）经 recording
    `CloudTaskTurnCoordination` 实例驱动，跑的是 **production default `run`/`safeRun` mapping 本身**（direct
    `turn.run(...)` 断言=第 228/240/255/271 行），非 Wubei phase 机器；slot fences+route-transfer cleared-intent
    peek 经 `CloudDialogPreparedActionState`；lane-preemption+await-newer 经 `CloudWholeTaskReadyEventState`；
    yellow-hint OCR empty-fold 经 `LocalOcrClient`；pathing-mirror absent-fact idle-fold 经
    `CloudNavigationPathingState`。末句显式声明"do not drive the Wubei phase machinery or its private caller glue"。
- 保留其下既有 **"Test-gate scope (parent P1-2 adjudication, Option B)"** 段作为**父级源码审查层（layer 3）**说明——
  三层门文字未删，现与新拆分的 auto-executed / component layer 形成完整"合同目标·父级源审层 vs 本文件自动执行层"叙述。
- **未删任何现有 11 项断言；未新增 full-loop harness / production seam / 业务改动**；纯 JavaDoc 说明修正。

### 复核
- test=`defba451`/**523 行**/**11 @Test**（较 576b2123 仅顶类 JavaDoc +7 行；brace 0/0·paren 0/0 平衡）。
- production `WubeiTask`=`52e88c68`/4432L **未改**（冻结）；foundation 写集未改（Review #2 PASSED）。
- 唯一 `.execute(` 调用=154 行（`execute(null)`）；direct `turn.run(...)` transaction 断言=228/240/255/271 行——
  与新 JavaDoc 逐字一致，无过度声称。
- 零 Git mutation；A/C active Java writer 期间未运行 Maven（doc-only 变更，结构/行号核；不替代父级 build gate）；
  无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。A 保持 TURN-35 sole owner，不 release。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A REPAIR2-REDELIVERY P2-1-CLOSED DOC-ONLY TEST=defba451-523-11T WUBEI-FROZEN=52e88c68 ACK=PARENT-TURN35-REVIEW2-P2-DOC-1944 AWAIT-REVIEW3 NO-RELEASE 2026-07-17T19:52:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 - PASSED - 2026-07-17T19:54:26-04:00

结论：`P0/P1/P2=0/0/0`，**SOURCE+TEST SOURCE REVIEW PASSED**。External A 的 TURN-35 whole-card source owner
释放；卡转 `ZERO OWNER / BUILD GATE PENDING`。本结论不是 Maven/JUnit/build passed。

- P2-1 已闭合：test 顶类 JavaDoc 现准确拆分三层证据。自动执行层唯一 public task 调用是第 154 行
  `execute(null)` 的 bound-context fail-fast；component 层第 228/240/255/271 行直接驱动 production-default
  `CloudTaskTurnCoordination.run/safeRun`，并明确不驱动 Wubei phase machinery/private caller glue；第三层保留父级
  对 private caller 的 `696a12b0` 逐方法源码审查，不再过度声称自动覆盖。
- Repair #2 范围符合返修条件：production SHA-256=
  `52e88c68222371aba0dc0939ed298f399e29684eba53fbc74998a516eeefc3b1`/4432L 冻结未变；test SHA-256=
  `43e491e299a85c8d8f81d3aa6845d1df59fba0461624676b57b736765d24a2d9`/523L/11T。测试入口数量与断言驱动点未漂移，
  无 full-loop harness、production seam、反射、第二协议/store 或业务算法副本。
- Review #2 已通过的两个 P1 继续有效：四处 `TaskFatalException` 显式重抛；四个 absent local type、
  `rawCurrent/runWith`、runtime guard、旧构造器参数均已闭合，turn coordination 中性映射保持 baseline phase chain。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

### 独立 build gate

- External C 仍在 TURN-37 写 Java，本轮不并发运行 Maven/JUnit/compile。
- writer 稳定后仍须运行用户授权 `WubeiWholeTaskTurnContractTest` 与适用 Cloud compile；失败则按 build gate
  重新记录，不回滚本次 source review 结论。
- A owner 释放后，TURN-37 已冻结 Bag Amendment #3 的共享 protocol 写集碰撞门同步解除。

<!-- TRUE_EOF: TURN-35 PARENT-SOURCE+TEST-REVIEW3 PASSED P0=0-P1=0-P2=0 P2-DOC-TRUTH-CLOSED PROD=52e88c68-4432 TEST=43e491e2-523-11T OWNER-A-RELEASED ZERO-OWNER BUILD-PENDING TURN37-BAG-UNBLOCKED NO-MAVEN 2026-07-17T19:54:26-04:00 -->

## SHA PROVENANCE CORRECTION (production label, NO content change) - 2026-07-17T19:57:00-04:00

**发现并主动披露 production SHA 标签错误（非字节漂移、非外部篡改、非 code change）。** 本轮 heartbeat 对
on-disk `WubeiTask.java` 复核 `git hash-object` 得实际 SHA=`5d89ea9a`（4,432 行），而此前各次交付/复核一直标注
`52e88c68`。根因：Repair #1 的 P2-1 顶注 + `execute` JavaDoc 文档修正**改变了文件哈希**，但引用值未随之刷新，
沿用了 doc 编辑前的旧哈希。

**证据表明内容正确且稳定，仅标签需更正：**
- file mtime=`19:38:07`（Repair #1 最后一次 WubeiTask 编辑），**早于** Repair #2 的 test 编辑（19:50+）——
  即 Review #2（19:44）时刻 on-disk 已是 `5d89ea9a`；父级 Review #2 逐项确认的属性（4 fatal-rethrow catch、
  24 param ctor、`rawCurrent/runWith/runtime.`=0）全部是**当前 on-disk 文件**的属性 → **父级实审内容 = `5d89ea9a`**，
  只是沿用了我提交的错误标签 `52e88c68`。
- 自 19:38:07 起 WubeiTask **无任何写入**（无他人 writer 触碰，无漂移）。
- 本轮 on-disk 不变式复核全通过：`private final` 注入字段=24（行 264–288 共 25 个，减去 inline-init 的
  `random`；行 4409 为内部类字段，非 WubeiTask 字段，不计入 `@RequiredArgsConstructor`）；fatal-rethrow catch=4；
  `rawCurrent`/`.runWith(`/`runtime.`=0；brace 0/0·paren 0/0；4,432 行。

**更正后权威冻结引用：production `WubeiTask.java` = `5d89ea9a` / 4,432 行**（内容与 Review #2 所审一致，未做任何
code/业务改动）。test 标签 `defba451`/523 行/11T 经复核正确无需更正。请父级 Review #3 以 `5d89ea9a` 为准；
此更正**不触发返修**，仅修正 provenance 标签。零 Git mutation；未运行 Maven；`D:\mavenProject\DHXY` 只读。

<!-- TRUE_EOF: TURN-35 EXTERNAL-A SHA-PROVENANCE-CORRECTION PROD-ONDISK=5d89ea9a-4432 WAS-MISLABELED-52e88c68 NO-DRIFT-NO-TAMPER MTIME-19:38:07 INVARIANTS-INTACT TEST=defba451-523-11T AWAIT-REVIEW3 2026-07-17T19:57:00-04:00 -->

## PARENT HASH-PROVENANCE CLARIFICATION / REVIEW #3 REAFFIRMED - 2026-07-17T19:58:30-04:00

A 的 19:57 更正把两种不同哈希口径混在了一起；文件没有漂移，Review #3 结论保持：

- `Get-FileHash -Algorithm SHA256`：production=
  `52e88c68222371aba0dc0939ed298f399e29684eba53fbc74998a516eeefc3b1`，test=
  `43e491e299a85c8d8f81d3aa6845d1df59fba0461624676b57b736765d24a2d9`。
- `git hash-object` blob id：production=`5d89ea9a...`，test=`defba451...`。
- 两组值分别是 SHA-256 与 Git blob，对应同一份 production 4432L、test 523L/11T 字节；mtime 与已审控制流均未变化。

因此父级 Review #3=`P0/P1/P2=0/0/0 PASSED`、A owner release、TURN-37 Bag collision release 全部维持，
无需返修或重新领取。

<!-- TRUE_EOF: TURN-35 PARENT-HASH-CLARIFICATION SHA256-PROD=52e88c68 SHA256-TEST=43e491e2 GIT-BLOB-PROD=5d89ea9a GIT-BLOB-TEST=defba451 SAME-BYTES REVIEW3-PASSED-REAFFIRMED OWNER-A-RELEASED 2026-07-17T19:58:30-04:00 -->
