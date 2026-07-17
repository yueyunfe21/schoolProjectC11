# CR271 External A/B/C/D fresh restart command preflight

## Helper boundary and snapshot

- Role: CR271 Internal scheduling helper only; not implementation owner, reviewer, manager or parent.
- Snapshot refreshed through `2026-07-16T11:24:36.430-04:00`.
- Unique write set: this report only.
- Read authority: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, current CR271 head in `docs/ACTIVE_WORK.md`, authoritative
  plan sections 14-19, all four fixed lane reports, and the physical EOF of the four current fixed cards.
- Read-only evidence also includes current target line counts, SHA-256 values and mtimes. No Git command, Maven,
  JUnit, compile, package, runtime, application, server, Task, UI, capture or input was run.
- This report does not claim/release an owner, edit a card/lane/plan, approve/reject code, or change a CR gate.

## Preflight result

At the snapshot, fresh launch commands are immediately copyable for External A, B and D. External C's 11:15
fresh-restart command became stale before this report was written: C claimed Repair #1 at `11:21:17`, made a real
first-window increment, and canonically delivered source+test at `11:23:42`. No parent review/owner-release/next
assignment follows that delivery yet. A second fresh C would race a delivered card awaiting adjudication, so no C
launch command is issued here.

The three still-launchable cards and C's active card remain pairwise write-set disjoint. The safe current shape is:

| Lane | Current fixed card | Snapshot state | Launch disposition |
|---|---|---|---|
| A | `TURN-28Q Repair #3` | zero owner; card EOF requires fresh A claim | copyable now |
| B | `TURN-28S2` | zero owner; strict-696 source unchanged | copyable now |
| C | `TURN-34BP1 Repair #1` | source+test delivered; latest parent disposition pending | do not relaunch |
| D | `TURN-34AT1 Repair #3` | zero owner; card EOF requires fresh D claim | copyable now |

## Current true-EOF evidence

### External A

- Lane EOF: `EXTERNAL-A RESTART-REQUIRED NEXT=TURN-28Q-REPAIR-3 ... OLD-TASK-NOT-OWNER` at `11:03:03`.
- Card EOF: `TURN-28Q PARENT-REVIEW-6 REPAIR-3-REQUIRED ... EXTERNAL-A-FRESH-RESTART` at `11:03:03`.
- Exact modify write set:
  - DHXY `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
  - DHXY `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
  - DHXY `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`
  - `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28Q.md`
- Current snapshots: queue `c53a423e...`, worker `225a9f3b...`, test `f72c7db0...`.
- `InputActionRequest.java` is explicitly read-only at `7f4f8fdc...`.

### External B

- Lane EOF: `EXTERNAL-B RESTART-REQUIRED NEXT=TURN-28S2 ... ZERO-OWNER` at `11:03:03`.
- Card EOF: `TURN-28S2 PARENT-RESTART FRESH-EXTERNAL-B-NEXT ZERO-OWNER ... STRICT-696` at `11:03:03`.
- Exact modify write set:
  - Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md`
- Current snapshot: 3374 lines, `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`.
- This child has no test write set; no recognizer, caller, protocol, context or helper file may be added.

### External C

- Lane EOF at `11:15:00` still says `FRESH-RESTART TURN-34BP1-REPAIR-1 CLAIM-REQUIRED`.
- The card subsequently changed: C appended `EXTERNAL-C REPAIR #1 CLAIMED` at `11:21:17`, made a real first-window
  production increment, then appended canonical `REPAIR #1 SOURCE+TEST DELIVERED` at `11:23:42`.
- Delivery snapshots are production 524 lines /
  `f278460ba9dc664974a98ea5ef19532e60514b29015a2e9b25b8f49bf0eba895` and test 843 lines /
  `7caf01272346b2f647e67c825b11b1606ba38b81ee1e29ff65b56c3bc6b9dbbf`.
- No parent review, owner release, repair or next assignment appears after that delivery at this snapshot. The
  worker has stopped editing, but a fresh writer still has no current claim authority.
- Active exact modify write set:
  - Cloud `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
  - Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`
  - `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34BP1.md`

### External D

- Lane EOF: `EXTERNAL-D RESTART-REQUIRED NEXT=TURN-34AT1-REPAIR-3 ... OLD-TASK-NOT-OWNER` at `11:03:03`.
- Card EOF: `TURN-34AT1 PARENT-REVIEW-4 REPAIR-3-REQUIRED ... EXTERNAL-D-FRESH-RESTART` at `11:03:03`.
- Exact modify write set:
  - Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`
  - `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md`
- Current test snapshot: 1026 lines / `b5438da5...`.
- Production `AutoCombatService.java` is explicitly read-only at `532e6f84...`.

## Mutex and first-window check

The four Java write sets have an empty pairwise intersection:

- A: DHXY queue + worker + one DHXY test.
- B: Cloud `NpcClickService.java` only.
- C: Cloud task context + its context test.
- D: Cloud AutoCombat contract test only.

Each card report is also a distinct Markdown path. `ACTIVE_WORK`, the authoritative plan and all four lane reports
are read-only for these implementation starts. A/B/D must claim at their assigned card's physical EOF before Java;
within the first five-minute window they must create a real target increment, deliver, or canonically return owner.
C has already satisfied claim + first-window increment + delivery and must remain stopped on BP1 until the parent
writes the next disposition.

After one card passes, the lane does not retire. It continues one five-minute, no-change-silent heartbeat, reads the
latest lane/card/CR271 assignment, and claims only the next parent-written READY card. A historical claim, lane text
or single-card pass never authorizes replay of an older assignment.

## Stale gate, owner and scope conflicts

1. Authoritative plan section 18 near lines 1528-1532 still lists the old queue heads A=`TURN-22 Repair #3`,
   B=`TURN-28`, C=`TURN-34A ACTIVE`, D=`TURN-34B`. That assignment paragraph is stale. The plan header, section 16
   rows, `ACTIVE_WORK` 11:15 and current card EOFs supersede it.
2. Plan section 17.2 contains parent-card write sets. It must not be copied into these child repairs: A's current
   Q repair is the three DHXY files above; C's BP1 is context + context test; D's AT1 is test-only. Expanding to
   parent production files would violate the fixed cards and create conflicts.
3. Append-only lane reports contain many old READY/CLAIMED sections. Only their last physical section is usable.
   The old one-minute heartbeat text is superseded by one five-minute silent heartbeat per External lane.
4. `ACTIVE_WORK` 11:15 and the plan header call all four lanes fresh. That was true at 11:15, but is stale for C
   after the 11:21 claim and 11:23 delivery. It remains current for A/B/D at this snapshot.
5. C's claim initially lacked a trailing HTML marker, but the later canonical delivery has a proper `TRUE_EOF`.
   That delivery is still not a parent owner-release or a new READY assignment; it cannot authorize a second writer.
6. B starts from the untouched strict-696 SHA and must not use A's capacity-return discussion as a new scope grant.
   A must not replay S2; D must not replay BP1/BT1; C must not return to AT1 while D owns the next AT1 repair.

## Copyable fresh launch command - External A

```text
你是 fresh CR271 External Worker A，只是 implementation Worker，不是 reviewer、父级、helper。工作区
D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。先完整读取 AGENTS.md、docs/DHXY_CONTEXT.md、
docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、A lane report 物理 EOF，以及固定卡
docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28Q.md 的当前物理 EOF。你的唯一当前卡是
TURN-28Q Repair #3；不要回放 TURN-22、QP1、QT1 或 S2。

启动瞬间先确认卡尾仍是 Parent Review #6、没有新 owner，并确认 queue=c53a423e...、worker=225a9f3b...、
test=f72c7db0...；任一已变化就不得抢写，改读最新 EOF。确认无变化后，先在原卡物理 EOF 追加
EXTERNAL-A REPAIR #3 CLAIMED，再立即开始源码/测试增量。唯一写集是 InputActionQueue.java、
InputActionWorker.java、InputActionFrozenExclusiveContractTest.java 与该原卡；InputActionRequest.java
7f4f8fdc... 及其它文件全部只读。

只闭合卡载两项 typed-order：两个 frozen queue public entry 都先跑 typed safety、后跑 pure witness；worker
frozen preamble 不得让 legacy epoch comparator 抢在 typed safety 前，取得 context monitor 后在 exact focus 前
再次按 typed safety -> witness 检查。补确定性 public-path 用例：pre-enqueue STOP+A-B-A' 必须
NOT_STARTED/STOP_REQUESTED 且 zero take/focus/input/refresh；queued/taken STOP+identity/generation drift 必须
NOT_STARTED/STOP_REQUESTED、one take、zero focus/input，并用 latch/event、无 polling sleep。

首个 5 分钟窗口必须出现真实源码/测试增量、正式 delivery 或 OWNER RETURNED。不得扩写集、不得自批或创建
reviewer；不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，不得执行 Git。
按卡形状交付并停止编辑本卡；随后继续同一 5 分钟静默 heartbeat，单卡通过不停止，仅读取父级下一张 READY。
```

## Copyable fresh launch command - External B

```text
你是 fresh CR271 External Worker B，只是 implementation Worker，不是 reviewer、父级、helper。工作区
D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。先完整读取 AGENTS.md、docs/DHXY_CONTEXT.md、
docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、B lane report 物理 EOF，以及固定卡
docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28S2.md 的当前物理 EOF。你的唯一当前卡是
TURN-28S2；不要回放 TURN-34BT1、TURN-22C1 或 TURN-28 whole-card 旧任务。

启动瞬间先确认卡尾仍是 fresh External B restart、没有新 owner，并确认 Cloud NpcClickService.java 仍为
3374 行 / cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441；任一已变化就不得抢写，
改读最新 EOF。确认无变化后，先在 S2 子卡物理 EOF 追加 EXTERNAL-B RESTART CLAIMED，再立即开始 production
增量。唯一写集是 Cloud NpcClickService.java 与 S2 子卡；本卡没有 test write set，所有 test、recognizer、
protocol/context/client、DHXY、caller、POM/resource 和其它卡只读。

严格按 696a12b0 就地迁四个 active 顶层 mechanics：ALT_C+WAIT700、ALT_C+WAIT700、ALT_A+WAIT350、
ALT_4+WAIT400。每个 reached site 只发一次 public TurnGameClient.execute，单 fresh UUID，exact current
device/window/title/HWND/process/rect correlation，ordered INPUT KEY_TAP -> WAIT，无 frame；只有 correlated
COMPLETED/COMPLETED 两步齐全才继续。所有 terminal/uncertain/malformed/drift 零后续 action；confirmed stop 走
既有 checkpoint，其它失败走既有 fatal path。不得触 legacy private helper、mouse/Ctrl/capture/OCR/template/
dialog/BattleRadar/memory/navigation/caller，不加 retry/session/ledger/TTL/wrapper。

首个 5 分钟窗口必须出现真实 production 增量、正式 delivery 或 OWNER RETURNED。不得自批或创建 reviewer；
不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，不得执行 Git。按卡形状
交付并停止编辑；随后继续同一 5 分钟静默 heartbeat，单卡通过不停止，仅读取父级下一张 READY。
```

## External C launch command withdrawn at snapshot

Do not copy a fresh C launch command now. `TURN-34BP1 Repair #1` was claimed, incremented and canonically delivered
at `11:23:42`; the card now awaits the parent's latest review/disposition. A replacement command becomes safe only
after the card physically records a new repair/READY assignment or an owner release plus next card. The stale 11:15
lane/plan text does not permit a duplicate claim.

## Copyable fresh launch command - External D

```text
你是 fresh CR271 External Worker D，只是 implementation Worker，不是 reviewer、父级、helper。工作区
D:\mavenProject\DHXY，Cloud D:\mavenProject\dhxy-cloud-brain。先完整读取 AGENTS.md、docs/DHXY_CONTEXT.md、
docs/ACTIVE_WORK.md 当前 CR271 顶段、权威计划第 14-19 节、D lane report 物理 EOF，以及固定卡
docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md 的当前物理 EOF。你的唯一当前卡是
TURN-34AT1 Repair #3；不要回放 BP1、BT1 或旧 34B assignment。

启动瞬间先确认卡尾仍是 Parent Review #4、没有新 owner，并确认唯一 test 仍为 1026 行 / b5438da5...；
任一已变化就不得抢写，改读最新 EOF。确认无变化后，先在 AT1 子卡物理 EOF 追加
EXTERNAL-D AT1 REPAIR #3 CLAIMED，再立即开始 test 增量。唯一写集是 Cloud
AutoCombatServiceTurnContractTest.java 与 AT1 子卡；production AutoCombatService.java 必须保持只读
532e6f84...，POM/resources/callers/其它 tests/cards 全部只读。

只修三项卡载测试缺口：FAILED fixture 使用 legal failedStepIndex=0 且 step0=FAILED，STOPPED 与
DUPLICATE_OR_UNCERTAIN 保持各自合法 shape；同 team/同 window 的 now+10ms 第二次 reservation 按 strict
696a12b0 30 秒 gate 期待 deferred，不改 production；在现有 outer tagged-union null 断言之外，补
capture.clearPointerIfOverRegion()==null 与 capture.pixelChangeProbe()==null。保留共享 8-call service 的
8 commands、8 canonical distinct UUID、script exhausted、零 Stage2/3/retry。

首个 5 分钟窗口必须出现真实 test 增量、正式 delivery 或 OWNER RETURNED。不得扩写集、不得自批或创建
reviewer；不得运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，不得执行 Git。
按卡形状交付并停止编辑；随后继续同一 5 分钟静默 heartbeat，单卡通过不停止，仅读取父级下一张 READY。
```

TRUE_EOF PRECHECK_COMPLETE
