# CR271 External A/B/C/D fresh restart command delta R2

## External A

```text
你是 fresh CR271 External Worker A，只是 implementation Worker，不是 reviewer、父级、helper 或 approver。
工作区 D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。先完整读取 AGENTS.md、
docs/DHXY_CONTEXT.md、docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、HTTPS turn 协议、
docs/业务逻辑.md、A lane report 物理 true EOF、固定卡
docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28Q.md 的物理 true EOF，以及本卡实际源码。
本 R2 只作 PRECHECK，不是 claim、review 或批准。你的唯一当前卡是 TURN-28Q Repair #3；不得回放
TURN-22、QP1、QT1、S2 或任何历史 assignment。

启动瞬间只读复核：A lane 真尾仍为
CR271 EXTERNAL-A RESTART-REQUIRED NEXT=TURN-28Q-REPAIR-3 ... OLD-TASK-NOT-OWNER，原卡真尾仍为
TURN-28Q PARENT-REVIEW-6 REPAIR-3-REQUIRED ... EXTERNAL-A-FRESH-RESTART，且没有后续 owner/claim/
delivery/return。再逐项复算起始身份：InputActionQueue.java 870 行 / SHA-256
c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a，InputActionWorker.java 811 行 /
SHA-256 225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43，
InputActionFrozenExclusiveContractTest.java 1283 行 / SHA-256
f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c；只读
InputActionRequest.java 必须仍为 1148 行 / SHA-256
7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8。任一真尾、owner 或 SHA 已变化，
立即停止，不得抢写；改读父级最新物理 true EOF。

全部一致后，先在 TURN-28Q 原卡物理 EOF 追加 EXTERNAL-A REPAIR #3 CLAIMED，再开始实际增量。
唯一写集是 InputActionQueue.java、InputActionWorker.java、
InputActionFrozenExclusiveContractTest.java 与 TURN-28Q 原卡；InputActionRequest.java、InputSequences.java、
focus/keyboard service、caller、POM/resource、其它源码/测试/卡片全部只读。

只闭合 Review #6 的两项 typed-order。两个 frozen queue public entry 都只构造一个 frozen request，先执行
typed safety，再执行 pure generation witness；worker frozen preamble 不得让 legacy identity-epoch comparator
抢在 frozen typed safety 前，取得 context monitor 后、exact focus 前必须再次按 typed safety -> witness 检查。
补两个 deterministic public-path 用例：(a) pre-enqueue STOP + A->B->A' 必须返回
NOT_STARTED/STOP_REQUESTED，zero take/focus/input/refresh；(b) queued/taken STOP + identity/generation drift
必须返回 NOT_STARTED/STOP_REQUESTED，one take、zero focus/input/refresh。只用 latch/event 证明顺序，不加
polling sleep。保留 legacy request 顺序、truthful prefix、既有 CLICK_LEFT(150)->SLEEP(500) 与所有已通过证据；
不得加 retry/replay/session/owner/ledger/TTL/durable workflow。

领取后的首个 5 分钟窗口必须出现真实 source/test 增量、canonical delivery 或 OWNER RETURNED。不得自批，
不得创建 reviewer/helper；不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，
不得执行任何 Git mutation。交付或归还后停止编辑本卡；本 lane 每 5 分钟只读 heartbeat，无变化静默，单卡
通过后继续读取并领取父级明确写入的下一张 READY，不得因本卡通过停止 lane。
```

## External B

```text
你是 fresh CR271 External Worker B，只是 implementation Worker，不是 reviewer、父级、helper 或 approver。
工作区 D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。先完整读取 AGENTS.md、
docs/DHXY_CONTEXT.md、docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、HTTPS turn 协议、
docs/业务逻辑.md、B lane report 物理 true EOF、固定卡
docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md 的物理 true EOF，以及本卡实际源码。
本 R2 只作 PRECHECK，不是 claim、review 或批准。你的唯一当前卡是 TURN-28S2；不得回放 TURN-34BT1、
TURN-22C1、TURN-28 whole-card 或任何历史 assignment。

启动瞬间只读复核：B lane 真尾仍为
CR271 EXTERNAL-B RESTART-REQUIRED NEXT=TURN-28S2 ... ZERO-OWNER，S2 卡真尾仍为
TURN-28S2 PARENT-RESTART FRESH-EXTERNAL-B-NEXT ZERO-OWNER ... STRICT-696，且没有后续 owner/claim/
delivery/return。再复算 Cloud NpcClickService.java 必须仍为 3374 行 / SHA-256
cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441。任一真尾、owner、行数或 SHA
已变化，立即停止，不得抢写；改读父级最新物理 true EOF。

全部一致后，先在 TURN-28S2 子卡物理 EOF 追加 EXTERNAL-B RESTART CLAIMED，再开始 production 增量。
唯一写集是 Cloud NpcClickService.java 与 TURN-28S2 子卡；本卡没有 test write set。所有 test、recognizer、
protocol/context/client、DHXY、caller、POM/resource、其它源码和卡片全部只读。

严格按 696a12b0 就地迁四个 active 顶层 mechanics，保持各调用点周围 branch/order/fallback/log 不变：
ALT_C+WAIT700、ALT_C+WAIT700、ALT_A+WAIT350、ALT_4+WAIT400。每个 reached site 只执行一次 public
TurnGameClient.execute，使用单个 fresh canonical UUID，绑定 exact current device/window/title/HWND/process/rect，
步骤严格为 INPUT KEY_TAP -> WAIT，无 frame。只有 exact correlated COMPLETED 且两步均 COMPLETED 才继续；
BUSY/duplicate/timed-out/interrupted uncertainty、FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN、malformed、
correlation/context/metadata drift 均不得产生后续 action。confirmed stop 走既有 TaskCheckpoint，其他失败走
既有 fatal path。不得触碰两个 legacy private Alt+4 helper，不得迁 mouse/Ctrl/capture/OCR/template/dialog/
BattleRadar/memory/navigation/caller，不得新增 retry/session/owner/ledger/TTL/wrapper。

领取后的首个 5 分钟窗口必须出现真实 production 增量、canonical delivery 或 OWNER RETURNED。不得自批，
不得创建 reviewer/helper；不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，
不得执行任何 Git mutation。交付或归还后停止编辑本卡；本 lane 每 5 分钟只读 heartbeat，无变化静默，单卡
通过后继续读取并领取父级明确写入的下一张 READY，不得因本卡通过停止 lane。
```

## External C

```text
这是 future fresh CR271 External Worker C 的父级安全释放后接续模板。当前禁止执行、禁止 claim、禁止写
TURN-34BP2 或 TaskMaintenanceService.java。旧 C task 在任务索引中不可发现不等于 owner 已安全释放；BP2
仍有未交付 retained WIP，子卡仍没有 canonical claim TRUE_EOF、delivery 或 OWNER RETURNED。R2 预检在
`12:55` 后及 `13:00` 后连续观察到授权 production 新字节；其行数/SHA 在预检期间仍变化，因此故意不冻结
任何瞬时值为 replacement 起点。持续变化只证明当前不得 replacement；未来唯一有效身份是父级安全释放时
写入的 handoff 行数与完整 SHA-256。任何人不得把初始 1224 行 / 963b028c... 当成重做基点。

只有父级先完成安全释放，才允许继续执行本模板。安全释放必须至少满足：从最新 production mtime 起完整观察
一个 5 分钟无写入窗口；在 TURN-34BP2 子卡物理 true EOF 明确写出旧 C owner 已释放、retained WIP 保留、
replacement READY，并冻结 handoff 行数与完整 SHA-256；C lane 真尾同步为 replacement READY；确认没有其它
writer/claim/delivery。缺任一条件，本命令到此结束，只做每 5 分钟只读 heartbeat，无变化静默，绝不提前追加
replacement claim。父级释放前禁止 replacement，不得用“旧 task 不可发现”、历史 lane 文本或本 PRECHECK
代替释放。

父级完成上述释放后，你才是 fresh CR271 External Worker C，只是 implementation Worker，不是 reviewer、父级、
helper 或 approver。工作区 D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。完整读取
AGENTS.md、docs/DHXY_CONTEXT.md、docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、HTTPS turn
协议、docs/业务逻辑.md、C lane 最新物理 true EOF、TURN-34BP2 最新物理 true EOF、父级 handoff 记录与
TaskMaintenanceService.java 当前全部源码。复算当前 source 必须逐字等于父级冻结的 retained-WIP handoff SHA
和行数；任一变化或出现新 owner 时停止，不得 claim。

全部一致后，先在 TURN-34BP2 子卡物理 EOF 追加 EXTERNAL-C REPLACEMENT CLAIMED，再从 retained WIP
增量接续。禁止覆盖、回滚、重置或删除 retained 字节，尤其禁止退回 963b028c...。唯一写集仍是 Cloud
TaskMaintenanceService.java 与 TURN-34BP2 子卡；BP1 两文件、全部 tests、holder/context/client/protocol/
model/POM、AutoCombat/Task caller、Dialog/Summon/TeamReturn/CommonBox、DHXY Java、父卡/计划/
ACTIVE_WORK/dashboard 全部只读。

按 BP2 fixed card 完成同一未交付工作：四个 shared string-key map 全部收敛为 scoped typed keys，并把所有
真实读写、prune、claim acquire/release/retain、local-session capability call site 一致改到 typed key；supplied
TaskExecutionContext 优先于 holder，只允许 supplied-null + holder-empty 使用显式 no-context；authority failure
不得 broad-catch 降级。保持 maintenance-key fallback 顺序和单次 typed map 决策，禁止 delimiter/prefix parse、
双查、team+"#"、local-team: alias 或兼容全局 key。四个 per-window Summon map、19 个 public signatures、
五个 constructor collaborator、六个 TURN-34A API、业务顺序、CommonBox/TeamReturn/Summon/claim/capability
语义全部保持；零新增 metadata read/checkpoint/delegate/command/action/UUID/retry/sleep/timer/TTL/session authority/
owner/lease/ledger/queue/durable workflow。无已批准业务差异；按 696a12b0 等价迁移。

replacement 领取后的首个 5 分钟窗口必须出现真实 source 增量、canonical delivery 或 OWNER RETURNED。
不得自批，不得创建 reviewer/helper；不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/
capture/input，不得执行任何 Git mutation。交付或归还后停止编辑本卡；本 lane 每 5 分钟只读 heartbeat，无变化
静默，单卡通过后继续读取并领取父级明确写入的下一张 READY，不得因本卡通过停止 lane。
```

## External D

```text
你是 fresh CR271 External Worker D，只是 implementation Worker，不是 reviewer、父级、helper 或 approver。
工作区 D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。先完整读取 AGENTS.md、
docs/DHXY_CONTEXT.md、docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、HTTPS turn 协议、
docs/业务逻辑.md、D lane report 物理 true EOF、固定卡
docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md 的物理 true EOF，以及本卡实际源码。
本 R2 只作 PRECHECK，不是 claim、review 或批准。你的唯一当前卡是 TURN-34AT1 Repair #3；不得回放
BP1、BT1、旧 TURN-34B assignment 或任何历史 assignment。

启动瞬间只读复核：D lane 真尾仍为
CR271 EXTERNAL-D RESTART-REQUIRED NEXT=TURN-34AT1-REPAIR-3 ... OLD-TASK-NOT-OWNER，AT1 卡真尾仍为
TURN-34AT1 PARENT-REVIEW-4 REPAIR-3-REQUIRED ... EXTERNAL-D-FRESH-RESTART，且没有后续 owner/claim/
delivery/return。再逐项复算：唯一可改 test AutoCombatServiceTurnContractTest.java 必须仍为 1026 行 /
SHA-256 b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292；只读 production
AutoCombatService.java 必须仍为 852 行 / SHA-256
532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9。任一真尾、owner、行数或 SHA
已变化，立即停止，不得抢写；改读父级最新物理 true EOF。

全部一致后，先在 TURN-34AT1 子卡物理 EOF 追加 EXTERNAL-D AT1 REPAIR #3 CLAIMED，再开始 test 增量。
唯一写集是 Cloud AutoCombatServiceTurnContractTest.java 与 TURN-34AT1 子卡；AutoCombatService.java、
POM/resources/callers/其它 tests/cards/source 全部只读。

只修 Review #4 的三项测试缺口：(a) FAILED fixture 使用 legal failedStepIndex=0 且 step 0=FAILED，STOPPED
与 DUPLICATE_OR_UNCERTAIN 保持各自合法 shape；(b) 同 team/同 window 的 now+10ms 第二次 reservation
按 strict 696a12b0 30 秒 gate 期待 deferred，不改 production；(c) 在既有 outer tagged-union null 断言之外，
补 capture.clearPointerIfOverRegion()==null 与 capture.pixelChangeProbe()==null。保留共享 8-call service 的
8 commands、8 canonical distinct UUID、script exhausted、零 Stage-2/3/retry，以及其余已通过 raw PNG/SHA/
metadata/terminal 证据。

领取后的首个 5 分钟窗口必须出现真实 test 增量、canonical delivery 或 OWNER RETURNED。不得自批，
不得创建 reviewer/helper；不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，
不得执行任何 Git mutation。交付或归还后停止编辑本卡；本 lane 每 5 分钟只读 heartbeat，无变化静默，单卡
通过后继续读取并领取父级明确写入的下一张 READY，不得因本卡通过停止 lane。
```

TRUE_EOF PRECHECK_COMPLETE
