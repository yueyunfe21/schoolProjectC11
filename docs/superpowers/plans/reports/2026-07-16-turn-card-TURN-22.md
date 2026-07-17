# TURN-22 - TeamReturn HTTPS turn cutover

## READY / PARENT FROZEN IMPLEMENTATION BRIEF - 2026-07-16 01:49 EDT

- 状态：`READY / PARENT BRIEF FROZEN`；类型：真实业务 Service cutover。历史去重键仅为
  `legacyCoverageKey=AutoBattleTask -> TeamReturnService::clickReturnTeamIfPresent`，不把 `countDelta`
  当作 CR271 主进度。父级是唯一 manager/final reviewer，Worker 不是 reviewer。
- startDependsOn：TURN-14、TURN-18、TURN-23 均已由父级独立源码/测试源码审查通过；TURN-23
  Repair #1 为 `P0/P1/P2=0/0/0`，所以本卡已经 READY。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `TeamReturnService` 全 public surface；同时核对 `docs/业务逻辑.md` 的当前本地队伍边界、
  `WAIT_TEAM_RETURN` 信号在则保持/信号消失按来源继续、已验证回城快照不得被归队等待清除，以及
  stop/pause 不得包装成业务失败。
- 模板与几何：成员 `images/template/status/gui.png` 的双仓 SHA-256 均为
  `5B4C2C43F84A9FF9CEF26F8BE22BE40872C192698244A9840D01C3DEA25E4E21`；队长
  `images/template/status/zhao.png` 的双仓 SHA-256 均为
  `2468C531D25C980061473BE7BAF5918D910499E51D096C5417C4652E880ECBD3`。ROI 固定为 exact
  bound window origin 加 `(342,57)`、尺寸 `272x69`，阈值 `0.85`，一像素对一像素，不缩放。

### Exact write set

- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnService.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`。
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`。
- Append only this fixed report true EOF。

`CloudTeamReturnPort.java`、`BotProperties.java`、`TurnGameClient`/protocol/action factory/command port、
`PlayerStateService`、Task/caller、DHXY、POM、Spring configuration、template resources、旧 fact/DTO/route 与其它
报告/测试全部只读。现有 port records 已足够，不得为了改名扩协议或新建 facade/helper chain。保护两仓全部
dirty/untracked，不回滚、覆盖、清理、提交或执行其它 Git mutation。

### Frozen production contract

1. **Exact one-frame observation。** `observeButton` 与 `observeLeaderSignal` 每次调用都先从
   `TaskExecutionContext.getTurnInvocationContext()` 绑定 exact `TurnGameClient`，读取一次 latest metadata，
   在创建 UUID/action 前核对 deviceId、windowId、初始 native HWND 与 processId；重绑/错 context 直接
   fail closed，command/UUID=`0`。latest STOP 返回现有 `Terminal.STOPPED`，也不得创建 command。
2. **Raw PNG 上云、Cloud 判定。** 每次 observation 只发一个现有 `CAPTURE` JSON step，region 精确为
   `windowRect.left+342, windowRect.top+57, 272x69`，`fullWindowFailureEvidence=false`。completed frame 必须严格
   核 action/window/step、purpose=`CAPTURE`、sourceStepIndex=`0`、region/dimensions、PNG signature、SHA 和真实
   decoded pixels；outcome metadata 必须与 command 前 exact snapshot 相同。不得扩大 ROI、第二 capture、旧
   `readWindowFact`、DHXY local match 或自动 retry。
3. **两个 template 都只在 Cloud match。** member 只按 packaged `gui.png`、leader 只按 packaged `zhao.png`
   在同一 raw frame 上调用现有 `ImageFinder`，阈值精确 `0.85`。首个合法 hit 的 local center 加 ROI origin 得到
   screen-absolute point；normal miss=`ABSENT`，packaged template 缺失=`TEMPLATE_UNAVAILABLE`，matcher 机械异常=
   `MECHANICS_FAILED`，confirmed CAPTURE failure=`CAPTURE_UNAVAILABLE`。这些仍通过现有
   `Terminal.OBSERVED + ObservationState` 投影，保持 Service 当前分支；uncertain/correlation 错误绝不能伪成
   ABSENT/PRESENT。
4. **成员顺序逐步不变。** `clickReturnTeamIfPresent` 必须继续：第一次 member observe；PRESENT 后调用一次
   `playerStateService.ensureSheYaoXiangActive(context)`；第二次 member observe；仍 PRESENT 才对匹配中心做 X/Y
   各 `[-3,+3]` 随机偏移并 click。first miss、incense 后消失均返回 false；不得提前点击、跳过补香或把第二次
   observation 当 transport retry。no-match throttle 下现有 leader diagnostic observation 继续存在且只用于日志。
5. **单 JSON 点击动作。** `clickReturnButton` 在 UUID/command 前重新做第 1 条 exact metadata/STOP/坐标窗内
   校验，然后只发一个 action、一个 UUID，ordered steps 精确为
   `CLICK_LEFT(screenAbsolute) -> WAIT(150ms) -> WAIT(500ms)`。两个 WAIT 分别恢复 baseline
   `InputAction.clickLeft(...,150)` 与后续 `InputAction.sleep(500)`，必须留在同一全局 input queue 原子 fragment；
   不合并成第二 command、不附加截图、不自动重发。
6. **点击 terminal 不伪成功。** COMPLETED 且三步 exact correlation 才映射 `EXECUTED`；FAILED 映射现有
   `NOT_EXECUTED`；STOPPED 映射 `STOPPED`；`DUPLICATE_OR_UNCERTAIN`/command uncertainty 映射 `UNKNOWN`。
   frame 必须为 null；wrong action/window/step/result shape fail closed。Service 现有 checkpoint/fatal/false 分支
   保持，不新增 catch 将 uncertainty 吞成 false。
7. **队长 wait 与 precheck 不变。** live signal observation、初次判断、`120000ms` timeout、`3000ms` poll、
   signal 消失/timeout 返回值和 source 分流不改。`beginLeaderSignalPrecheck` 必须在返回 handle 前完成唯一一次
   exact raw capture + Cloud match，随后 future 只投影这份 immutable typed result；`consumeLeaderSignalPrecheck`
   只核 scope/window/HWND/taskRunId 与 future，不发 command、不抓第二帧。missing/stale/not-ready/failed 仍 inconclusive
   并让 caller 按现有 live fallback；不得加 TTL、第二验证或新 park/yield。
8. **Active-path source gate。** 本卡两个 production 文件对 `TEAM_RETURN_BUTTON`、
   `TEAM_RETURN_LEADER_SIGNAL`、`readWindowFact`、`WindowFact`、`executeInputBundle`、Cloud 进程内
   `GameClientTracker/CoordinateHelper/InputSequences/InputProvider/WindowTaskContextHolder` active path 零引用。
   不新增本地 Service、OCR、本地业务判断、owner/session/ledger/TTL/durable workflow 或自动 retry。

### Named-test acceptance

唯一 `TeamReturnTurnContractTest` 必须实例化 production `TeamReturnService` 与 production
`CloudTeamReturnPortAssembly`，以 fake/scripted bound `TurnGameClient` 捕获真实 production action；不得只测复制 mapper。
至少覆盖：

- 非零 window origin 的 member/leader `272x69` raw PNG，双 template byte/hash 与 `0.85` match；hit absolute center、
  normal miss、template unavailable、matcher failure、坏 PNG/SHA/dimensions/region/sourceStep、错 outcome metadata；
- 每次 observation 恰好一个 UUID/command；wrong current context、same logical window 的 wrong HWND/process、latest STOP
  均 command/UUID=`0`；FAILED/STOPPED/UNCERTAIN/correlation 不伪 PRESENT/success，零 transport retry；
- 成员 happy path 的 `member observe -> incense once -> member refresh -> one click action` 顺序；first miss 零 incense/click，
  refresh miss 零 click；随机点必须落在 matched center 的 `+-3` 范围；点击 action 精确三步
  `CLICK_LEFT/WAIT150/WAIT500`、一 UUID/command、无 frame；FAILED/STOPPED/UNCERTAIN 四态逐项断言；
- leader wait 初次 miss、signal 后消失、timeout 与 poll cadence；precheck 在 bag 前只 capture 一次，future/consume
  command 增量为零，same-scope conclusive，missing/stale/not-ready/failed 保持 inconclusive/live fallback；
- active-path source gate 第 8 条全部零引用，模板双仓 SHA parity，四个永久本地 Service 清单未扩张。

Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级待所有 Java writers 稳定后
只运行用户授权 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 的
`mvn -q -Dtest=TeamReturnTurnContractTest test` 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 与用户确认的最小 HTTPS JSON turn 等价迁移。**

<!-- TRUE_EOF: TURN-22 parent-frozen-brief -->

## CLAIMED - 2026-07-16T01:51:41-04:00

- Agent id：`019f6979-7699-7fc2-b50b-0c35c1d3ace2`；nickname：`Averroes`；角色：TURN-22 implementation Worker，非 reviewer；父级是唯一 manager/final reviewer。
- Exact unique write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`
  - Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
  - 本固定报告 true EOF append only。
- 禁止项：其余双仓源码、协议、DHXY、caller/Task、POM、配置、资源与报告全部只读；不回滚、覆盖、清理、提交、暂存或执行其它 Git mutation；不运行 Maven、JUnit、compile、runtime、application、server、Task、poller、UI、capture 或 input。

<!-- TRUE_EOF: TURN-22 claimed agent=019f6979-7699-7fc2-b50b-0c35c1d3ace2 nickname=Averroes -->

## SOURCE+TEST DELIVERED - 2026-07-16T02:33:00-04:00

- Agent id：`019f6979-7699-7fc2-b50b-0c35c1d3ace2`；nickname：`Averroes`；角色仍为 TURN-22
  implementation Worker，非 reviewer；父级保留唯一 manager/final reviewer 权限。
- 实际 production 文件：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnService.java`，
    `522` 行，SHA-256=`CD1CD365BFF90B16817C15831A2685F2FEAE84E2D49893B9B975362D4EC4EDAF`。
    `52..95` 保持 `observe -> incense once -> observe -> random +-3 -> click`；`150..174` 保持
    leader initial/poll/timeout；`256..289` 在返回 precheck handle 前完成唯一 observation，future 只投影
    immutable typed result；`301..337` consume 只读 handle；`491..513` 按当前 turn-native `String taskRunId`
    核 window/HWND/run scope。旧 process-local runtime 详情只改为日志文字 `turn-native`，不参与业务判断。
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`，
    `540` 行，SHA-256=`7450B3B8D76D8F7D467078C437480E498E0002E9F61E7F47CA3363EC8734C3F1`。
    `47..59` 固定 `gui/zhao`、`0.85`、`(342,57,272x69)`、`150/500`；`76..101` 分别投影 member/leader；
    `104..168` 在 metadata/STOP/point preflight 后仅发一个三步 click action；`171..289` 每 observation
    仅发一次 raw-PNG capture 并在 Cloud match；`292..430` 严格核 capture/input step、frame purpose/region/
    dimensions/sourceStep/PNG/SHA/decoded pixels；`432..484` 核 exact device/window/HWND/process/window bounds。
- 实际 named test：
  - 新建 `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`，
    `1652` 行，SHA-256=`CEDD8FA6878A39CB2231A9DE7905F4C944ADE80370CE06FC31DCCAB140CC1E21`；共 `14`
    个 `@Test`。测试直接实例化 production `TeamReturnService`、production `CloudTeamReturnPortAssembly` 与
    production bound `TurnGameClient`，scripted command port 捕获真实 production `TurnAction`，并独立计数 UUID。
  - `117..350` 覆盖非零 origin 双模板 raw frame、absolute center、miss/template/matcher 三态、坏 PNG/SHA/
    dimensions/region/sourceStep/purpose/pixels、错 action/window/step/outcome metadata、wrong current context、
    same-window wrong HWND/process、latest STOP、point outside 与 metadata 后 rebind 的零 UUID/command 门。
  - `353..501` 覆盖 capture FAILED/STOPPED/uncertain/transport uncertainty 零 retry，成员 happy/miss 顺序、
    random `+-3`、单 command `CLICK_LEFT/WAIT150/WAIT500`、无 frame、click 四态及 correlation fail closed。
  - `504..638` 覆盖 leader initial miss、signal disappear、timeout/poll cadence，precheck capture-before-bag、
    future/consume command delta=`0`、same-scope conclusive，以及 missing/window/HWND/run stale/not-ready/failed
    inconclusive 后 live fallback。
  - `640..704` 锁 active-path source zero gate、双仓 template byte/SHA parity 与四个永久本地 Service allowlist。
    当前 cloud `.gitignore:15` 忽略 `src/test/`，文件已真实创建但依领取禁令未执行 staging/Git mutation。
- 静态 evidence：
  - 两个 production 文件对 `TEAM_RETURN_BUTTON`、`TEAM_RETURN_LEADER_SIGNAL`、`readWindowFact`、
    `WindowFact`、`executeInputBundle`、`GameClientTracker`、`CoordinateHelper`、`InputSequences`、
    `InputProvider`、`WindowTaskContextHolder` 的逐项计数均为 `0`。
  - Cloud/DHXY `gui.png` SHA-256 均为
    `5B4C2C43F84A9FF9CEF26F8BE22BE40872C192698244A9840D01C3DEA25E4E21`；Cloud/DHXY `zhao.png`
    SHA-256 均为 `2468C531D25C980061473BE7BAF5918D910499E51D096C5417C4652E880ECBD3`。
  - production Assembly 的 source occurrence 为 `turnGameClient.bind=2`、
    `boundClient.latestWindowMetadata=2`、`boundClient.capture=1`、`boundClient.execute=1`；共享 observation
    path 与 click path 各自没有第二 command 或 transport retry。
- 基线等价：已逐项对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `TeamReturnService`、
  `AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2` TeamReturn caller chain，以及 `docs/业务逻辑.md` 的本地队伍边界、
  `WAIT_TEAM_RETURN` 信号在则保持/信号消失按来源继续、已验证回城快照不由归队等待清除、stop/pause 不包装成
  业务失败。caller/Task/DHXY 均未改；无 TTL、第二验证、额外 retry/park/yield/cleanup、owner/session/ledger 或
  durable workflow。**无已批准业务差异；按 `696a12b0` 与冻结 brief 等价迁移。**
- 门禁声明：Worker 按禁令未运行 Maven、JUnit、compile、package/build、runtime、application、server、Task、
  poller、UI、capture 或 input；未启动任何进程。父级应在所有 Java writers 稳定后运行固定报告指定的
  `mvn -q -Dtest=TeamReturnTurnContractTest test` 与适用 Cloud compile/build，并据实际输出作最终判断。

<!-- TRUE_EOF: TURN-22 source-test-delivered agent=019f6979-7699-7fc2-b50b-0c35c1d3ace2 nickname=Averroes -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-16T02:40:17-04:00

- 父级已独立读取三份交付源码/测试源码、`696a12b0` 的 `TeamReturnService`、DHXY production
  `TurnInputActionMapper` 与 `LocalTurnActionExecutor`，并独立复核交付 SHA-256。Worker 自述未作为批准依据。
- 审查结论：`P0/P1/P2=0/1/0`；状态为 `REPAIR REQUIRED / NOT SOURCE APPROVED`。本轮未运行
  Maven/JUnit/compile/runtime/input；TURN-33 仍是活动 Java writer。

### P1-1 - baseline click delay/queue 原子边界没有落到真实 DHXY executor

- 精确证据：`696a12b0:TeamReturnService.java:86-89` 在一次
  `inputSequences.submitAndWait(...)` 内执行 `InputAction.clickLeft(...,150)` 与 `InputAction.sleep(500)`。
  当前 `CloudTeamReturnPortAssembly.java:124-133` 下发
  `CLICK_LEFT -> WAIT(150) -> WAIT(500)`；但 DHXY
  `TurnInputActionMapper.java:34-35` 把 `CLICK_LEFT` 固定映射为 `InputAction.clickLeft(...,0)`，且
  `LocalTurnActionExecutor.java:136-160` 明确只截到最后一个 mouse INPUT，尾随 WAIT 留在 queue transaction
  之外。`TeamReturnTurnContractTest.java:441-478` 只断言三步 JSON 和 Cloud terminal，没有穿透真实 DHXY mapper/queue，
  因而不能证明冻结 brief 第 5 条所述的同一全局 input queue 原子片段。
- 影响：真实执行会在零 click delay 后释放鼠标 queue，再分别等待 150ms/500ms；与 baseline 的 150ms click
  delay 加 500ms 同 queue hold 不等价，也允许其它窗口输入在两个等待期间插入。本卡不能据此记为
  `SOURCE+TEST SOURCE REVIEW PASSED`。
- 返修条件：不得在 TURN-22 私有写集内伪造 no-op mouse、第二 click 或前置 move。先由父级冻结并完成通用 turn
  input mechanics 前置，使 JSON 能显式表达 queue-owned post-click delay；随后原 Worker 仅在
  `CloudTeamReturnPortAssembly.java` 与 `TeamReturnTurnContractTest.java` 内改为该 typed 能力，并由 named
  contract 穿透真实 mapper/queue，证明单 UUID/command、一次 queue submission、150ms+500ms 顺序、零 transport
  retry。前置落盘前 TURN-22 保持 blocked，不允许扩大写集。

### 其余独立审查结论

- `272x69` exact ROI、双模板 `0.85` Cloud match、raw PNG/SHA/dimensions/sourceStep、exact
  device/window/HWND/process、member observe/incense/refresh、leader wait/precheck、FAILED/STOPPED/uncertain 映射和
  legacy active-path zero gate均与冻结 brief 相符。
- `CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED` 在 port 上保持 typed state；Service 对 closed
  `OBSERVED` 非 PRESENT 延续 `696a12b0` 的 false 分支，不新增 fail-closed 业务语义。
- 无已批准业务差异；唯一阻断是上述真实 input timing/queue mechanics 接缝。

<!-- TRUE_EOF: TURN-22 parent-review-1 repair-required p0=0 p1=1 p2=0 -->

## REPAIR #1 READY FOR EXTERNAL A - 2026-07-16T04:49:00-04:00

- Shared prerequisite `TURN-28P` has passed the parent source/test-source gate with `P0/P1/P2=0/0/0`; its typed
  `CLICK_LEFT/CLICK_RIGHT clickDelayMs + queueHoldMs` now expresses the missing 696a12b0 queue-owned mechanics.
- External A may now append a true-EOF `REPAIR #1 CLAIMED` and implement this repair. Exact write set is only:
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
  - this append-only report.
- Replace the old three-step click payload `CLICK_LEFT -> WAIT150 -> WAIT500` with one `CLICK_LEFT` whose typed input
  contains `clickDelayMs=150` and `queueHoldMs=500`. The named test must pass through the production turn mapper/executor
  boundary and prove one UUID, one command, one input-queue submission, exact 150+500 timing, no frame and zero transport
  retry. Do not modify TURN-28P shared files, `TeamReturnService`, DHXY, protocol, caller/Task or any other report.
- Preserve every other parent-review-passed behavior and the full frozen brief. Worker does not run Maven/runtime/input and
  must not write `APPROVED/CLOSED`; delivery is `SOURCE+TEST DELIVERED` only.

<!-- TRUE_EOF: TURN-22 REPAIR-1 READY EXTERNAL-A gate=TURN-28P-source-pass 2026-07-16T04:49:00-04:00 -->

## REPAIR #1 CLAIMED - 2026-07-16T04:50:39-04:00

- Worker: CR271 External Worker A（implementation Worker，非 reviewer）。平台/session 身份
  `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`，model `claude-opus-4-8`；平台 spawn nickname 未在本会话回传，不自造，
  以父级 identity correction 为准。固定 lane 报告 `reports/2026-07-16-cr271-external-worker-a.md`。
- 门已满足：TURN-28P `PARENT SOURCE+TEST-SOURCE REVIEW #1` = `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`
  （04:48:07.493）；本卡 `REPAIR #1 READY FOR EXTERNAL A`（04:49:00）明示 External A 可领取实施。
- Exact write set（仅此三项，不扩大）：
  1. `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`
  2. `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
  3. 本 append-only 报告
- 实施口径：把旧三步 `CLICK_LEFT -> WAIT150 -> WAIT500` 换成**同一个 `CLICK_LEFT`** 携带 typed
  `clickDelayMs=150` + `queueHoldMs=500`；named test 穿透 production turn mapper/executor 边界，证明单 UUID、单
  command、一次 input-queue submission、精确 150+500 时序、无 frame、零 transport retry。保留其余已通过复审的行为
  与冻结 brief 全文。
- 禁令遵守：不改 TURN-28P 共享文件、`TeamReturnService`、DHXY、protocol、caller/Task 或任何其它报告；不伪造
  no-op mouse / 第二 click / 前置 move；保护两仓全部 dirty/untracked；不执行任何 Git mutation；不运行
  Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；交付只写 `SOURCE+TEST DELIVERED`，
  不写 `APPROVED/CLOSED`，不冒充 reviewer。

<!-- TRUE_EOF: TURN-22 REPAIR-1 CLAIMED External-A session 76eac05a-e5cd-46a2-a58f-5a07c6573ccc 2026-07-16T04:50:39-04:00 -->

## SOURCE+TEST DELIVERED (REPAIR #1) - 2026-07-16T04:56:07-04:00

External Worker A（session `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`）交付 Repair #1，闭合 Parent Source Review #1 的
唯一 P1-1（baseline click delay/queue 原子边界未落到真实 DHXY executor）。**仅改授权的两个 Java 文件 + 本报告**；
未运行 Maven/JUnit/compile/package/runtime/input（门留父级）；未做任何 Git mutation。本节不是 APPROVED/CLOSED。

### 交付 SHA-256

| 文件 | SHA-256 |
|---|---|
| `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java` | `4435b30c4bfc923e222b12de3cda5be9aeec766aa1f826f26ea534bc1a5cfd66` |
| `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java` | `774b18aaecc32b6ae1ac7130c28a7d828de6240516deabd24966ba0af721e12e` |

### P1-1 修复：三步 payload → 同一个 typed CLICK_LEFT

- `CloudTeamReturnPortAssembly.java:122-128`：旧 `CLICK_LEFT -> WAIT(150) -> WAIT(500)` 三步已删除，改为
  `new TurnInputSpec(screenX, screenY, null, null, null, null, null, CLICK_DELAY_MS, POST_CLICK_WAIT_MS)`（:122-124，
  即 TURN-28P 已通过的 9 参 typed 构造，`clickDelayMs=150`/`queueHoldMs=500`），`steps` 现为**单个** index 0 的
  `TurnStepType.INPUT / TurnInputAction.CLICK_LEFT`（:125-128）。常量 `CLICK_DELAY_MS=150`(:57)、
  `POST_CLICK_WAIT_MS=500`(:58) 原值未动。
- 真实 DHXY 侧因此不再零 delay：`TurnInputActionMapper.java:39-49` 对 `CLICK_LEFT` 产出
  `InputAction.clickLeft(x, y, 150)` 与 `InputAction.sleep(500)` 的**同一个有序 list**；
  `TurnInputStepExecutor.java:166-177` 对该 list 恰好调用一次 `inputActionQueue.submitAndWait(...)`。这与
  `696a12b0:TeamReturnService.java:86-89` 在一次 `submitAndWait` 内 `clickLeft(...,150)` + `sleep(500)` 等价，
  尾随 WAIT 不再留在 queue transaction 之外，其它窗口输入无法在 click 与 500ms hold 之间插入。
- 其余保持不变：一 action 一 UUID、单 command、零 transport retry、input action 不得带 frame、
  `requireInputSteps` 按 `expectedSteps.size()` 泛化（3→1 自动适配）、STOPPED/FAILED/DUPLICATE_OR_UNCERTAIN/
  COMPLETED 终态映射、exact device/window/HWND/binding 校验与 `696a12b0` 顺序。

### named contract 穿透真实 mapper（证据）

- `TeamReturnTurnContractTest.java:444` `clickUsesOneTypedClickActionAndMapsAllClosedTerminals`（原名
  `clickUsesOneThreeStepAction...`，随 payload 改名）覆盖 completed/failed/stopped/outcome-uncertain/
  transport-uncertain 五个 case；每例断言 `harness.port.executeCalls==1`（单 command、零 transport retry）与
  `harness.uuids.calls.get()==1`（单 UUID），failed case 索引由 `1` 改为 `0`（:452，现仅 index 0 存在）。
- `assertAtomicClick`（:1301-1332）：断言 `action.steps().size()==1`；该步为 index 0 / INPUT / CLICK_LEFT；
  `waitMs/capture/match/localService` 全 null（无尾随 WAIT、无 frame）；`input().clickDelayMs()==150`、
  `input().queueHoldMs()==500`；`endX/endY/scrollDelta/key/text` 全 null。
- `assertClickMapsToOneQueueSubmission`（:1334-1347）：把**同一个** emitted `TurnInputSpec` 喂给**真实 production**
  `com.bot.dhxy.cloud.turn.TurnInputActionMapper.mapMouse(CLICK_LEFT, input, EXACT_WINDOW.windowRect())`，断言
  mapped list 恰为 2 项且有序：`InputActionType.CLICK_LEFT` 且 `getX/getY` == spec 点、`getDelayMs()==150`；随后
  `InputActionType.SLEEP` 且 `getDelayMs()==500`。即真实 mapper 输出的**一个有序 list**（executor 一次
  `submitAndWait` 的原子片段），直接反证旧 `clickLeft(...,0)` + queue 外尾随 WAIT。
- 新增 import 仅三条：`com.bot.dhxy.cloud.turn.TurnInputActionMapper`（:3）、`com.bot.dhxy.input.action.InputAction`（:17）、
  `com.bot.dhxy.input.action.InputActionType`（:18）。

### 范围与保护

- 写集恰为授权二 Java + 本报告。**未改** TURN-28P 共享文件（`TurnInputSpec`/`TurnCaptureSpec`/validator/mapper/
  executor/protocol tests/fixtures 均只读引用）、`TeamReturnService`、DHXY production、caller/Task、其它报告。
- 未伪造 no-op mouse / 第二 click / 前置 move / 第二 command/UUID/frame；未加 retry/TTL/owner/session/wrapper。
- 两文件均为**既有** in-flight 落盘内容，按当前字节增量编辑，非新建/覆盖：Cloud `remote/` 为 untracked 目录
  （`git status --porcelain` 折叠为 `?? .../cloudbrain/remote/`，`-uall` 可展开），`src/test/` 被
  `.gitignore:15` 忽略；故二者不出现在常规 `git status` 列表。两仓全部既有 dirty/untracked 均已保护未动，
  `git diff --check` exit 0，无任何 Git mutation。
- 未运行门（保留父级）：Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input 全未执行；
  本 Worker 不自评通过，`SOURCE+TEST DELIVERED` 仅表交付，不构成 reviewer 结论。

<!-- TRUE_EOF: TURN-22 REPAIR-1 SOURCE+TEST DELIVERED External-A 2026-07-16T04:56:07-04:00 assembly=4435b30c test=774b18aa -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - 2026-07-16T05:07:00-04:00

- 父级已独立读取 Repair #1 的 production/test 源码、真实 DHXY
  `TurnInputActionMapper`、`TurnInputStepExecutor` 与其 contract test，并复核两份交付 SHA-256：
  assembly=`4435B30C4BFC923E222B12DE3CDA5BE9AEEC766AA1F826F26EA534BC1A5CFD66`，
  test=`774B18AAECC32B6AE1AC7130C28A7D828DE6240516DEABD24966BA0AF721E12E`。
  Worker 自述未作为批准依据。
- 审查结论：`P0/P1/P2=0/1/0`；状态仍为 `REPAIR REQUIRED / NOT SOURCE APPROVED`。
  本轮未运行 Maven/JUnit/compile/runtime/input；TURN-33 Repair #1 仍是活动 Java writer。

### P1-1 - named test 只穿透 mapper，没有穿透 executor/真实单次 queue submission

- 精确证据：production payload 已在 `CloudTeamReturnPortAssembly.java:122-128` 正确收敛为单个
  `CLICK_LEFT`，携带 `clickDelayMs=150` 与 `queueHoldMs=500`；这部分无返修项。
  但 `TeamReturnTurnContractTest.java:1334-1346` 的
  `assertClickMapsToOneQueueSubmission(...)` 只直接调用
  `new TurnInputActionMapper().mapMouse(...)` 并检查返回的两项 `List<InputAction>`。
  它没有实例化或调用 production `TurnInputStepExecutor.execute(...)`，没有 recording/fake
  `InputActionQueue`，也没有断言 `submitAndWait(...)` 的真实调用次数。因此方法名和注释声称的
  “one queue submission”不是该 named test 实际观察到的结果。
- 对照证据：真实 `TurnInputStepExecutor.java:60-67,166-177` 才是把 mapper 结果交给
  `InputActionQueue.submitAndWait(...)` 的 production 边界；DHXY 通用
  `TurnInputStepExecutorContractTest.java:83-110` 已单独覆盖该 mechanics，但冻结的 TURN-22 Repair #1
  验收明确要求本卡 named contract 将**本卡实际发出的同一个 spec**穿透 production mapper/executor，
  并直接证明一次 queue submission。不能用另一张共享卡的通用测试替代本卡端到端关联证据。
- 影响：当前源码审查可以推导 production 应当一次提交，但 TURN-22 的卡片级 JSON -> local executor
  验收链仍有一段仅靠注释/推断，未满足用户要求的动作合同 unit test，也未满足本卡冻结返修条件。
- 返修条件：External A 仅修改
  `TeamReturnTurnContractTest.java` 与本报告；production assembly 保持当前 SHA/语义不动。
  用本卡 emitted `TurnInputSpec` 调用真实 production `TurnInputStepExecutor.execute(...)`，以 recording/fake
  `InputActionQueue` 直接断言：`submitAndWait` 恰好 `1` 次；该次 actions 顺序为
  `CLICK_LEFT(delay=150) -> SLEEP(500)`；坐标为 emitted spec 的 exact absolute point；executor result 为
  completed；同一 card case 仍保持一 UUID、一 HTTPS command、无 frame、零 transport retry。
  不得用源码字符串/source guard、复制 mapper、mock executor 返回值或只检查 list 大小凑证据；不得改
  TURN-28P/DHXY/protocol/production/caller/Task，不得新增业务语义。

### 其余复审结论

- 单个 typed `CLICK_LEFT` payload、150/500 typed timing、terminal fail-closed、exact binding、一 UUID/command、
  零 transport retry 与其余 Review #1 已通过项均保持通过；本轮唯一阻断是上述 test-source 证据缺口。
- 无已批准业务差异；继续按 `696a12b0` 与冻结 brief 等价迁移。

<!-- TRUE_EOF: TURN-22 parent-review-2 repair-required p0=0 p1=1 p2=0 -->

## REPAIR #2 CLAIMED - 2026-07-16T05:05:40-04:00

- External Worker A（session `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`）领取 Parent Source Review #2 的唯一 P1-1 返修。
- 认可复审判定：Repair #1 的 `assertClickMapsToOneQueueSubmission`（`TeamReturnTurnContractTest.java:1334-1346`）
  只直调 `new TurnInputActionMapper().mapMouse(...)` 检查两项 list，**未实例化/调用 production
  `TurnInputStepExecutor`、无 recording/fake `InputActionQueue`、未断言 `submitAndWait` 调用次数**；方法名与注释
  声称的 "one queue submission" 并非该 named test 实际观察结果，属超范围声明，返修成立。
- 本轮 exact write set（仅两项）：
  1. `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
  2. 本 append-only 报告
  production `CloudTeamReturnPortAssembly.java` **保持当前 SHA `4435b30c…` 与语义不动**。
- 返修目标：用本卡 emitted `TurnInputSpec` 调真实 production `TurnInputStepExecutor.execute(...)`，以 recording/fake
  `InputActionQueue` 直接断言 `submitAndWait` 恰好 1 次、该次 actions 顺序为 `CLICK_LEFT(delay=150) -> SLEEP(500)`、
  坐标为 emitted spec 的 exact absolute point、executor result completed；同一 card case 仍保持一 UUID、一 HTTPS
  command、无 frame、零 transport retry。
- 禁令遵守：不用源码字符串/source guard、不复制 mapper、不 mock executor 返回值、不以 list 大小凑证据；不改
  TURN-28P/DHXY/protocol/production/caller/Task；不新增业务语义；保护两仓 dirty/untracked；无 Git mutation；
  不运行 Maven/JUnit/compile/runtime/input；交付只写 `SOURCE+TEST DELIVERED`，不写 `APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-22 REPAIR-2 CLAIMED External-A session 76eac05a-e5cd-46a2-a58f-5a07c6573ccc 2026-07-16T05:05:40-04:00 -->

## SOURCE+TEST DELIVERED (REPAIR #2) - 2026-07-16T05:08:32-04:00

External Worker A（session `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`）交付 Repair #2，闭合 Parent Source Review #2 的
唯一 P1-1（named test 只穿透 mapper，未穿透 executor/真实单次 queue submission）。**仅改测试 + 本报告**；
production assembly 未动。未运行 Maven/JUnit/compile/runtime/input；无 Git mutation。本节不是 APPROVED/CLOSED。

### SHA-256

| 文件 | SHA-256 | 状态 |
|---|---|---|
| `…/remote/CloudTeamReturnPortAssembly.java` | `4435b30c4bfc923e222b12de3cda5be9aeec766aa1f826f26ea534bc1a5cfd66` | **与 Repair #1 逐字节相同，本轮未改** |
| `…/service/TeamReturnTurnContractTest.java` | `cb41a6dd4ac931eabd470e67e25c9a5f653c55e1bba240f4367e7d267ccf508b` | 本轮唯一 Java 改动 |

### P1-1 修复：named contract 现在真正穿透 production executor + 真实 queue

- **撤回**上一轮的 `assertClickMapsToOneQueueSubmission`（旧 :1334-1346）：它只直调
  `new TurnInputActionMapper().mapMouse(...)` 检查两项 list，从未实例化 executor、无 recording queue、未断言
  `submitAndWait` 次数；其方法名/注释宣称的 "one queue submission" 并非实际观察结果。父级判定成立，已整段删除。
- **新** `assertClickReachesOneQueueSubmission(TurnStep click)`（:1348-1376），由 `assertAtomicClick`（:1311）对
  **本卡实际 emitted 的同一个 `TurnStep`/`TurnInputSpec`** 调用（非重建、非复制）：
  - 构造**真实 production** `TurnInputStepExecutor`（:1351-1356），注入真实 `TurnInputActionMapper`、真实
    `TurnKeyMapper`、真实 `WindowTaskContextHolder(new WindowIsolationProperties())`，以及 recording
    `RecordingInputQueue`（:1403-1415，`extends InputActionQueue`，仅 override `submitAndWait` 记录真实调用）。
  - `executor.execute(exactExecutionWindow(), click.inputAction(), click.input(), click.index())`（:1358-1359）——
    走 production 边界，未 mock executor、未桩返回值。
  - 直接断言：`result.status()==COMPLETED` 且 `result.code()==OK`（:1361-1362）；
    **`queue.submissions.size()==1`**（:1363-1364，"one wire click must be exactly one input-queue submission"）；
    该次 actions 顺序恰为 `[CLICK_LEFT, SLEEP]`（:1366-1367，按 `InputAction::getType` 列表比较，非仅 size）；
    `clickAction.getX()/getY()` == emitted spec 的 exact absolute point（:1369-1370）；
    `clickAction.getDelayMs()==150`（:1371）；`submitted.get(1).getDelayMs()==500`（:1372）；
    临时 exact-window 绑定已恢复 `contextHolder.rawCurrent().isEmpty()`（:1373-1374）。
  - `exactExecutionWindow()`（:1378-1401）用**本卡自身**的 `EXACT_WINDOW`/`WINDOW_RECT`/`NATIVE_HANDLE`/
    `PROCESS_ID`/`WINDOW_ID` 反射构造 `TurnExecutionWindow`，故 executor 执行的就是本卡 emitted 的绝对坐标；
    `UnusedKeyboardService`（:1417-1421）证明 click 路径不触键盘。
- 关联链因此端到端闭合：本卡 JSON（单 `CLICK_LEFT` + `clickDelayMs=150`/`queueHoldMs=500`）→ production
  mapper/executor → **一次** `InputActionQueue.submitAndWait([clickLeft(pt,150), sleep(500)])`，与
  `696a12b0:TeamReturnService.java:86-89` 的同 queue `clickLeft(...,150)` + `sleep(500)` 等价；不再依赖注释/推断，
  也不借用 TURN-28P 通用测试代替本卡证据。

### 保持不变（Review #1/#2 已通过项）

- `clickUsesOneTypedClickActionAndMapsAllClosedTerminals`（:454）五个 case 仍断言 `executeCalls==1`（单 HTTPS
  command、零 transport retry）、`uuids.calls==1`（单 UUID）、`assertAtomicClick` 单步 typed payload、无 frame；
  completed/failed/stopped/outcome-uncertain/transport-uncertain 终态映射未动。
- production `CloudTeamReturnPortAssembly.java` 逐字节未改（SHA 同上）；未改 TURN-28P/DHXY/protocol/caller/Task；
  未新增业务语义、retry/TTL/owner/session/wrapper；未用源码字符串/source guard、未复制 mapper、未 mock executor、
  未以 list 大小凑证据。

### 范围与保护

- 写集恰为 `TeamReturnTurnContractTest.java` + 本报告。测试文件位于 `.gitignore:15 src/test/` 忽略树内，按当前
  字节增量编辑，非新建/覆盖。两仓全部既有 dirty/untracked 保护未动；`git diff --check` exit 0；无任何 Git mutation。
- 未运行门（保留父级）：Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input 全未执行。
  `SOURCE+TEST DELIVERED` 仅表交付，不构成 reviewer 结论，不自评通过。

<!-- TRUE_EOF: TURN-22 REPAIR-2 SOURCE+TEST DELIVERED External-A 2026-07-16T05:08:32-04:00 assembly=4435b30c(unchanged) test=cb41a6dd -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 - 2026-07-16T05:14:54-04:00

- 父级已独立逐行读取 Repair #2 的 named test、production `TurnInputStepExecutor`、
  `TurnInputActionMapper`、`InputActionQueue` 与未变更的 `CloudTeamReturnPortAssembly`，并复核当前 SHA-256：
  assembly=`4435B30C4BFC923E222B12DE3CDA5BE9AEEC766AA1F826F26EA534BC1A5CFD66`，
  test=`CB41A6DD4AC931EABD470E67E25C9A5F653C55E1BBA240F4367E7D267CCF508B`。
  Worker 自述未作为通过依据。
- 独立结论：`P0/P1/P2=0/0/0`，状态进入
  `SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING`；External A 的本卡 implementation
  owner 释放，可等待父级续派下一张 READY 卡。
- Repair #2 已关闭 Review #2 的唯一 P1：`TeamReturnTurnContractTest.java:1348-1375` 将本卡实际 emitted
  `TurnStep`/`TurnInputSpec` 交给 production `TurnInputStepExecutor.execute(...)`；
  `RecordingInputQueue` 只在真实 queue 边界记录 `submitAndWait(...)`，直接断言恰好一次 submission、动作按
  `CLICK_LEFT -> SLEEP` 排序、绝对坐标与 emitted spec 精确一致、delay=`150ms`、hold=`500ms`、typed result
  `COMPLETED/OK`，且临时 exact-window context 已恢复。测试没有复制 mapper、mock executor 结果或用源码字符串
  凑证据。
- production payload 仍是一 command/一 UUID/单 typed `CLICK_LEFT`/无 frame/零 transport retry；terminal、
  uncertain、exact binding 与 `696a12b0` 原子 queue 语义保持。无已批准业务差异。
- 本轮未运行 Maven/JUnit/compile/runtime/input：TURN-28P 与 TURN-33 Java writer 仍活动。卡片尚未
  `CARD APPROVED/CLOSED`；按 `AGENTS.md` 仍需两名非实现者独立 reviewer 最新轮均 `APPROVED`，以及 writers
  稳定后的点名测试和适用 compile/build 门。

<!-- TRUE_EOF: TURN-22 parent-source-test-review-3 passed p0=0 p1=0 p2=0 independent-review-build-pending -->

## PARENT DELIVERY REVIEW #4 - 2026-07-16T05:38:00-04:00

- R1/R2 最新轮分别交付 `BLOCKED 0/2/0` 与 `BLOCKED 0/1/1`。父级未采用 reviewer 自述，已独立读取两份
  reviewer 报告、Cloud named test/POM/source tree、DHXY `TurnInputStepExecutor`、`InputActionQueue`、
  `WindowTaskContextHolder` 与现有 DHXY executor contract test。结论为
  **`P0/P1/P2=0/2/1 / REPAIR #3 REQUIRED / PREREQUISITE BLOCKED BY TURN-28P REPAIR #1`**；
  05:14 的 `0/0/0` source 初审被本轮可编译性与真实 queue 边界证据覆盖。

### P1-1 - Repair #2 把 DHXY-only production 类直接导入 Cloud test，测试不可编译

- `dhxy-cloud-brain/.../TeamReturnTurnContractTest.java:3-6,20-39` 直接导入 DHXY 的 executor/mapper/queue/
  window/runtime 类，`:1348-1421` 实例化这些类。Cloud source tree 不拥有这些类，Cloud `pom.xml:27-82`
  也没有 DHXY artifact/test dependency；build plugins 未增加 sibling source root/classpath。因此本卡点名 Cloud
  test 会在 test-compile 阶段、进入 JUnit 前失败，不能作为 production-through 证据。
- 修复不得复制同 FQN 类、依赖 sibling stale `target/classes`、增加临时 source root 或伪造共享 artifact。Cloud
  test 只保留 Cloud assembly 的 emitted spec/command/terminal 断言；DHXY executor/queue 证据必须落在拥有这些
  production 类的 DHXY test module。

### P1-2 - 当前 executor 仍把 frozen action 坐标交给会再次 refresh 的 legacy queue

- `TurnInputStepExecutor.java:60-67,166-171` 先按 `TurnExecutionWindow.metadata().windowRect()` 映射绝对坐标，
  随后仅 `callWith(window.context())` 并调用 legacy `InputActionQueue.submitAndWait(...)`。
- `InputActionQueue.java:67-79` 会对 mutable context 再执行 `refreshAndValidateNativeBinding(...)`；它没有把 refresh
  后 binding 与 `TurnExecutionWindow.binding()/metadata` 比较。若 resolve 与 enqueue 间 HWND/process/rect/epoch
  漂移，旧坐标可能在新 focus/binding 上执行。Repair #2 的 recording queue override 完全绕过该 production body。
- 该缺口与 TURN-28P Repair #1 正在实现的通用 frozen exact-window queue 是同一共享前置。TURN-28P source/test-source
  门通过前，本卡不得 READY；通过后 `TurnInputStepExecutor` 必须一次性把完整
  `[CLICK_LEFT(delay=150), SLEEP(500)]` 列表交给 frozen snapshot boundary，drift 时零 physical input/typed fail closed。

### P2-1 - exact context/恢复断言是 empty-to-empty 伪阳性

- Cloud test `:1350,1373-1374` 只从空 holder 开始并断言结束仍为空；`:1380-1396` 构造 binding 却没有安装到
  runtime context；`:1403-1414` override queue 时也不读取当前 holder。即使 executor 没有建立 exact context，该
  断言仍会通过。
- DHXY named test 必须预装不同 sentinel context，在 frozen queue 调用内记录 exact windowId/HWND/process/rect/
  epoch，并在返回后断言 sentinel 原样恢复；与 TURN-28P 的真实 queue/worker drift test 共同闭合，不重复造业务 test。

### Repair #3 frozen write set and gate

1. Cloud：`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`，删除非法跨仓
   mechanics imports/fixture，只保留本卡真实 emitted `TurnInputSpec` 的 `150/500`、一 command/UUID、terminal/
   uncertain/无 frame/零 retry 断言。
2. DHXY：`src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`，在 TURN-28P Repair #1 通用 frozen
   queue API 最终签名落盘后改用该 API；不得自造第二 frozen wrapper。
3. DHXY：`src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`，补本卡 `150/500`
   exact snapshot、sentinel context restore、一次 frozen queue submission 与 drift 零 input 证据；通用双线程/
   cancellation 仍由 TURN-28P 点名 test 负责。
4. 本 append-only 报告。Assembly、mapper、protocol、POM、Task/caller/Service 其余代码只读。

External A 等 TURN-28P Repair #1 父级 source/test-source 门通过后才可在本报告 true EOF 领取 Repair #3；当前保持
lane 在线但零 Java mutation。无自动 retry/session/owner/ledger/TTL/durable workflow。Maxwell 仍为活动 Java writer，
本轮未运行 Maven/JUnit/compile/runtime/input。

**无已批准业务差异；按 `696a12b0` 的一次 queue `CLICK_LEFT(150)+SLEEP(500)` 等价迁移。**

<!-- TRUE_EOF: TURN-22 PARENT DELIVERY REVIEW-4 REPAIR-3 REQUIRED PREREQUISITE-BLOCKED-BY-TURN-28P P0P1P2=0/2/1 2026-07-16T05:38:00-04:00 -->

## PARENT SOURCE-START GATE REASSESSMENT - REPAIR #3 READY FOR EXTERNAL-A - 2026-07-16T08:01:03-04:00

- 父级重新把“允许开始源码返修”与“本卡最终 source/build 通过”拆成两个门。TURN-28P 的 frozen exact-window
  production API 已落盘：`InputSequences.submitFrozenExactWindowExclusiveAndWait(...)` 公开转发到
  `InputActionQueue.submitFrozenExactWindowExclusiveAndWait(...)`，队列在 context monitor 内冻结
  `(binding object, playerIdentityEpoch)`，返回 typed `InputActionExecutionResult`。TURN-28P 当前剩余工作仅是两份
  通用 contract test 的同步 fake -> real queue/worker harness，不再改 production API。
- TURN-22 Repair #3 exact modify write set（Cloud `TeamReturnTurnContractTest.java`、DHXY
  `TurnInputStepExecutor.java`、DHXY `TurnInputStepExecutorContractTest.java` 与本卡）与 TURN-28P 剩余两测试
  完全互斥；当前 External C 的 TURN-34A 写集也互斥。因此旧 `PREREQUISITE BLOCKED` 仅继续约束最终
  source/test-source 通过与 build，不再阻止 External A 按冻结 API 开始 Repair #3。
- External A 下一次 5 分钟 heartbeat 必须先完整读取本卡 Review #4 和本段，再在本卡物理 EOF 追加规范
  `EXTERNAL-A REPAIR #3 CLAIMED`；未 claim 前不得改源码。领取后的 exact目标不变：
  1. Cloud test 删除 DHXY-only imports/fixture，只测真实 assembly/JSON 的 `150/500`、一 command/UUID、
     terminal/uncertain/无 frame/零 retry；
  2. DHXY executor 直接使用上述冻结 public API，一次提交完整
     `[CLICK_LEFT(clickDelay=150), SLEEP(500)]`，不得再走 legacy refresh queue或自造 wrapper；
  3. DHXY named test 预装不同 sentinel context，验证 exact windowId/HWND/process/rect/epoch、返回后 sentinel
     原样恢复、一次 queue submission，以及 drift 时零 input。
- 本段是 `REPAIR #3 READY / SOURCE-START OPEN`，不是 source pass 或 CARD APPROVED。TURN-28P 两份测试正式
  delivery 并经父级复审前，本卡不得越过最终 source/test-source 与 build 门。无已批准业务差异；按
  `696a12b0` 一次 queue `CLICK_LEFT(150)+SLEEP(500)` 等价迁移。

<!-- TRUE_EOF: TURN-22 PARENT SOURCE-START-GATE OPEN REPAIR-3 EXTERNAL-A READY FINAL-SOURCE-BUILD-GATED-BY-TURN-28P-TESTS 2026-07-16T08:01:03-04:00 -->

## PARENT CLAIM ESCALATION - EXTERNAL-A FINAL WINDOW - 2026-07-16T08:17:00-04:00

- Source-start 已开放超过三个 External 5 分钟检查窗口，但本卡物理 EOF 仍无
  `EXTERNAL-A REPAIR #3 CLAIMED`；三份目标文件 SHA/mtime 均未出现本 assignment 后写入，当前本卡仍为零
  implementation owner。lane 在线不等于持卡，旧 gated 文本不能覆盖 08:01 的 READY。
- External A 必须在 `2026-07-16T08:22:00-04:00` 前于本卡 true EOF 规范 CLAIM 并开始 exact write set，或明确
  `OWNER RETURNED`。截止后仍无 claim 时，父级先在本卡撤销 A assignment，再释放一条 Internal helper 容量安全
  replacement；撤销落盘前禁止第二 writer。
- source-start/final-gate 两层口径、三文件写集、TURN-28P production API、`150/500`、sentinel restore、drift
  零 input 与全部禁令保持不变。

<!-- TRUE_EOF: TURN-22 PARENT CLAIM-ESCALATION EXTERNAL-A DEADLINE 2026-07-16T08:22:00-04:00 ZERO-OWNER -->

## EXTERNAL-A REPAIR #3 CLAIMED - 2026-07-16T08:15:29-04:00

- Implementation Worker：CR271 External Worker A，session `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`，model
  `claude-opus-4-8`；非 reviewer，父级为唯一 manager/final reviewer。本段不含 `APPROVED/CLOSED`，不自批。
- 已完整读取本卡 `PARENT DELIVERY REVIEW #4`（05:38，P0/P1/P2=0/2/1，REPAIR #3 REQUIRED）与
  `PARENT SOURCE-START GATE REASSESSMENT`（08:01:03，`REPAIR #3 READY / SOURCE-START OPEN`）。理解并接受：
  本段只开"允许开始源码返修"门；最终 source/test-source 与 build 仍被 TURN-28P 两份 contract test 的正式
  delivery + 父级复审阻断，本卡不得越门。
- 接受 Review #4 全部判定，特别是 P1-1 属我 Repair #2 自身缺陷：Cloud test 直接 import DHXY-only
  `TurnInputStepExecutor`/`InputActionQueue`/`TurnExecutionWindow` 等，而 Cloud `pom.xml` 无 DHXY artifact 依赖、
  Cloud source tree 也不含这些类（仅 protocol 类为双仓 byte-identical 副本），故该测试在 test-compile 阶段即失败。
  此缺陷由我引入，本轮优先移除。
- 本轮 exact **修改**写集（仅四项，父级冻结）：
  1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
  2. DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
  3. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
  4. 本 append-only 原卡
  Assembly（`CloudTeamReturnPortAssembly.java`，SHA `4435b30c…`）、mapper、protocol、POM、Task/caller/Service 其余
  代码只读；TURN-28P 冻结文件与其两份在修 contract test 只读，不触碰（当前 owner 为 internal replacement）。
- 执行顺序（先修我自己的破坏）：① Cloud test 删除跨仓 mechanics imports/fixture，只保留真实 assembly/JSON 的
  `150/500`、一 command/一 UUID、terminal/uncertain/无 frame/零 retry 断言；② DHXY executor 改用冻结 public
  `InputSequences.submitFrozenExactWindowExclusiveAndWait(...)`，一次提交完整 `[CLICK_LEFT(clickDelay=150),
  SLEEP(500)]`，不再走 legacy refresh queue、不自造 wrapper；③ DHXY named test 预装不同 sentinel context，验证
  exact windowId/HWND/process/rect/epoch、返回后 sentinel 原样恢复、一次 queue submission、drift 时零 input。
- 诚实声明（context 风险）：本会话余量有限。若在 ③ 完成前耗尽，我**不会**提前交付、**不会**自批，将按 B/我在
  TURN-28P 的同一口径在本卡 true EOF 追加规范 `OWNER RETURNED` + 精确 WIP/SHA 交接，由父级改派 fresh lane；
  但 ① 一旦落盘即已移除我引入的不编译缺陷，树只会变好不会变坏。
- 禁令：不 sleep 猜竞态、不反射 private helper、不扫描源码凑证据、不复制 mapper、不 mock executor 返回值；
  不新增 production hook/wrapper/retry/session/ledger/TTL/业务语义；保护两仓全部 dirty/untracked；**零 Git
  mutation**；不运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input。

<!-- TRUE_EOF: TURN-22 EXTERNAL-A REPAIR-3 CLAIMED session 76eac05a-e5cd-46a2-a58f-5a07c6573ccc 2026-07-16T08:15:29-04:00 -->

## EXTERNAL-A REPAIR #3 PARTIAL + BLOCKING FINDING (ITEM 2 API GAP) - 2026-07-16T08:18:34-04:00

本段**不是** delivery、**不是** `SOURCE+TEST DELIVERED`，**不含** `APPROVED/CLOSED`，不自批。External A 报告
Repair #3 的**已完成项**与一个会阻断任何 worker 的**冻结 API 缺口**，请父级裁决。

### ① 已完成（我引入的不编译缺陷已移除）

- Cloud `TeamReturnTurnContractTest.java`：删除全部跨仓 DHXY-only mechanics import 与 fixture——
  `TurnExecutionWindow`/`TurnInputStepExecutor`/`TurnKeyMapper`/`TurnInputActionMapper`、
  `InputAction`/`InputActionQueue`/`InputActionType`、`WindowIsolationProperties`/`BoundWindowKeyboardService`/
  `WindowTaskRunner`/`WindowNativeBinding`/`WindowRuntimeContext`/`WindowTaskContextHolder`，以及
  `assertClickReachesOneQueueSubmission`/`exactExecutionWindow`/`RecordingInputQueue`/`UnusedKeyboardService`
  整块（原 :1326-1411）。
- 现存 `com.bot.dhxy` import 仅剩 protocol 与 Cloud tree 内既有类型；protocol 类是双仓 byte-identical 副本，
  合法可用。`assertAtomicClick` 现只断言**本模块拥有**的 assembly/JSON 合同：单步 index 0 `INPUT/CLICK_LEFT`、
  `input().clickDelayMs()==150`、`input().queueHoldMs()==500`、`waitMs/capture/match/localService` 全 null（无尾随
  WAIT、无 frame）、`endX/endY/scrollDelta/key/text` 全 null；五 case 仍断言 `executeCalls==1`（一 command、零
  transport retry）与 `uuids.calls==1`（一 UUID）；DHXY 侧 queue 证据在 javadoc 中明确指向拥有该 production 的
  DHXY test module，不在本模块伪造。
- 结果 SHA-256：`2d2907592e96d3c44e4ae239a8f569adba785568b19309d3f35ce90cb49e9496`；
  Cloud `git diff --check` exit 0。**Review #4 P1-1（Cloud test 不可编译）据此闭合。**

### ② BLOCKING FINDING —— 冻结 public API 无 list 形态，item 2 在本写集内不可实现

- 父级 item 2 要求：executor「直接使用上述冻结 public API，一次提交完整 `[CLICK_LEFT(clickDelay=150), SLEEP(500)]`，
  不得再走 legacy refresh queue 或自造 wrapper」。
- 实际 API 现状（只读核对，未改）：
  - `InputActionQueue.java:337-341` / `InputSequences.java:80-87`
    `submitFrozenExactWindowExclusiveAndWait(String description, WindowRuntimeContext context,
    WindowNativeBinding binding, Supplier<Boolean> callback)` —— **冻结，但只接受 callback，不接受
    `List<InputAction>`**（为 exclusive capture probe 设计）。
  - `InputActionQueue.java:99-102` `submitAndWaitDetailed(String, List<InputAction>, long deadlineNanos,
    BooleanSupplier stopRequested)` —— **接受 list 且返回 typed `InputActionExecutionResult`，但不是冻结形态**
    （无 context/binding 参数，仍是会再次 refresh binding 的 legacy 路径，正是 Review #4 P1-2 所指缺口）。
  - `InputSequences` 其余 public 均为 `submitAndWait/submit`(boolean, 非冻结) 与 `submitExclusiveAndWait`(callback)。
- 因此三条路都越界：(a) 把 list 塞进 exclusive callback = queue 内再 queue，语义错误且违反"一次 submission"；
  (b) 自造 frozen list wrapper = 父级明令禁止；(c) 给 `InputActionQueue`/`InputSequences` 增加 frozen list 重载 =
  **修改 TURN-28P 冻结文件**，既不在我 exact write set，且该卡当前 owner 为 internal replacement（`INTERNAL
  REPLACEMENT CLAIMED` 08:04:28），并发双写被明令禁止。
- 结论：**item 2（进而依赖其最终 API 形态的 item 3）在本卡 exact write set 内不可实现**，需父级裁决：
  1. 授权 TURN-28P owner 增加冻结 list 形态（如 `submitFrozenExactWindowExclusiveAndWait(description, context,
     binding, List<InputAction>)` 返回 typed result），TURN-22 待其落盘后再做 item 2/3；或
  2. 把该重载纳入 TURN-22 exact write set 并明确与 TURN-28P owner 的互斥/时序；或
  3. 重定义 item 2 的可接受形态。
- 我不猜、不自造、不碰他人 owner 文件，故就地停在此处报告。

### ③ 未动文件（只读，SHA 佐证）

| 文件 | SHA-256 | 状态 |
|---|---|---|
| Cloud `CloudTeamReturnPortAssembly.java` | `4435b30c…` | 未改（Repair #2 起逐字节不变） |
| DHXY `cloud/turn/TurnInputStepExecutor.java` | `0ee95cbd48d3ec76fb9e50385108f9898f2979a33966487b39065352af1f43fd` | 未改（item 2 阻断） |
| DHXY `test .../TurnInputStepExecutorContractTest.java` | `bb1ccc43…` | 未改（item 3 依赖 item 2） |
| DHXY `input/action/InputActionQueue.java` | `bcd1e64a…` | 只读，等于 TURN-28P 交还字节 |
| DHXY `input/InputSequences.java` | `2d1768e6…` | 只读，等于 TURN-28P 交还字节 |

### ④ 诚实记录

- 本轮**未运行** Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；**零 Git mutation**；
  两仓全部既有 dirty/untracked 保护未动；未触碰任何他人 owner 的写集/日志。
- 本 Worker 会话余量已近耗尽。若父级裁决前我无法继续，将按同一口径追加 `OWNER RETURNED` + 精确交接；目前
  item 1 已落盘，树状态严格优于领取时。

<!-- TRUE_EOF: TURN-22 EXTERNAL-A REPAIR-3 PARTIAL item1-done item2-BLOCKED-frozen-api-has-no-list-form 2026-07-16T08:18:34-04:00 -->

## PARENT BLOCKER ADJUDICATION #2 - API GAP CONFIRMED / OWNER RELEASED - 2026-07-16T08:23:11.657-04:00

- 父级独立逐行复核 `InputSequences.java:80-87`、`InputActionQueue.java:337-365`、
  `InputActionRequest.java:245-256`、`InputActionWorker.java:128-198,401-438` 与
  `TurnInputStepExecutor.java:166-177`，确认 External A 的 blocking finding 成立。现有 frozen API 只接受
  callback；完整 action list 仍只能走会再次 refresh 的 legacy queue。
- 队列内再提交 action list 会触发 AGENTS.md 明确禁止的 queue-in-queue deadlock；在 TURN-22 callback 内直接执行
  input 又会复制 worker 私有 dispatcher，并失去“一次提交完整 action list”的 typed progress/safety 合同。因此旧
  `PARENT SOURCE-START GATE REASSESSMENT` 的“production API 已足够冻结”前提错误，现由本段正式覆盖。
- 结论：**`P0/P1/P2=0/1/0 / REPAIR #3 PREREQUISITE BLOCKED BY TURN-28Q`**。External A 已完成的 Cloud
  `TeamReturnTurnContractTest.java` 跨仓 import 清理与 SHA `2d290759...` 原样保留，作为 Repair #3 WIP；它不是
  source pass。DHXY executor/test 尚未修改。
- External A 自本段 true EOF 起释放 TURN-22 implementation owner并停止本卡写入；不存在第二 TURN-22 writer。
  父级已建立真实共享 mechanics 子卡 `2026-07-16-turn-card-TURN-28Q.md`，由 External A 优先领取并补 frozen
  exact-window action-list API。TURN-28Q 父级 source/test-source 通过后，A 原路返回本卡继续 item 2/3，无需重做
  已落盘 Cloud test。
- 本卡 exact write set 与 `696a12b0` 的一次 queue `CLICK_LEFT(150)+SLEEP(500)` 目标不变；在 TURN-28Q 通过前
  不允许其它 Worker 猜接口、扩写集或并发修改。本段未运行 Maven/runtime/input，零 Git mutation。

<!-- TRUE_EOF: TURN-22 PARENT ADJUDICATION-2 P0P1P2=0/1/0 BLOCKED-BY-TURN-28Q EXTERNAL-A-OWNER-RELEASED WIP-PRESERVED 2026-07-16T08:23:11.657-04:00 -->

## PARENT PARALLEL SLICE STATUS - 2026-07-16T09:13:36.373-04:00

- TURN-28Q Repair #1 已由父级 `0/0/0` source/test-source 通过；旧 API prerequisite 已解除。
- Cloud test 清理由 External B 以 TURN-22C1 单文件切片交付，父级独立 Review #1 为
  `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。
- DHXY executor + named test 已拆成写集互斥的 TURN-22D1 并发给 External A READY；A/B 不再共同持有整张
  TURN-22，也不互相等待。父级仅在 D1 交付后聚合 parent Repair #3 复审；当前本卡仍非 source pass/approved。

<!-- TRUE_EOF: TURN-22 PARENT PARALLEL-SLICES C1-SOURCE-PASSED D1-EXTERNAL-A-READY PARENT-REPAIR3-PENDING 2026-07-16T09:13:36.373-04:00 -->

## EXTERNAL-A TURN-22 WHOLE-CARD CLAIMED - 2026-07-16T17:28:47-04:00

EXTERNAL-A[TURN-22] WHOLE-CARD CLAIMED

- 领取时间：`2026-07-16T17:28:47-04:00`。
- Worker：CR271 External implementation Worker A（本会话），implementation only，非 reviewer；父级是唯一
  manager/final reviewer。本段不含 `APPROVED/CLOSED`，不自批。
- 完整任务卡：权威计划第 16 节 `TURN-22`，当前注册表状态
  `REPAIR #3 SOURCE-START READY / EXTERNAL-A NEXT / FINAL GATE ON 28P TESTS`。领取的是**完整既有 TURN-22
  父卡**（当前整卡状态为 Repair #3），不拆 preflight/tranche/fragment/子卡；C1/D1 两个历史切片的已接受字节
  只作为本父卡内冻结证据，其子卡不再作为派单单位。
- 完整 production/test/report 写集：
  - Repair #3 modify 写集（Review #4 + 08:01 gate reassessment 冻结，第 17.1 节同）：
    1. Cloud `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
    2. DHXY `DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
    3. DHXY `DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
    4. 本 append-only 固定报告
  - 整卡冻结 production（Repair #3 内只读，已经过父级 Review #1-#3 接受）：
    `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnService.java`、
    `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`。
  - 其余双仓源码、协议、TURN-28P/28Q 文件、POM、caller/Task、模板资源与其它报告全部只读。
- 领取点文件行数与 SHA-256（本 Worker 逐一独立复算）：
  | 文件 | 行数 | SHA-256 |
  |---|---:|---|
  | Cloud `service/TeamReturnTurnContractTest.java` | 1612 | `d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab` |
  | DHXY `cloud/turn/TurnInputStepExecutor.java` | 264 | `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e` |
  | DHXY `cloud/turn/TurnInputStepExecutorContractTest.java` | 695 | `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81` |
  | Cloud `service/TeamReturnService.java`（只读） | 522 | `cd1cd365bff90b16817c15831a2685f2feae84e2d49893b9b975362d4ec4edaf` |
  | Cloud `remote/CloudTeamReturnPortAssembly.java`（只读） | 538 | `4435b30c4bfc923e222b12de3cda5be9aeec766aa1f826f26ea534bc1a5cfd66` |
  五个文件全部等于父级各轮 review 已接受/已复核的字节（C1 Review #1 PASSED `d270d7dc`；D1 Review #2 +
  独立 2/2 `a64422b0`/`f5a7992f`；原卡 Review #1-#3 接受 `cd1cd365`/`4435b30c`）。
- 依赖检查：`S=TURN-14+TURN-18+TURN-23+TURN-28P production API`。TURN-14/18 均
  `SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING`；TURN-23 的 source/test-source Repair #1 `0/0/0`
  证据在盘（其 14:47 重开的整卡 Cloud compile repair 已归还并计划合同阻断，属最终 build cohort 层，父级
  08:01 `SOURCE-START GATE REASSESSMENT` 与第 16 节注册表均维持本卡 source-start READY）；TURN-28P
  production frozen API（`InputActionQueue/InputSequences` frozen exact-window 边界 + TURN-28Q action-list
  形态 `submitFrozenExactWindowActionsAndWait`）已落盘且 28Q Repair #1 父级 `0/0/0`；TURN-28P 两份测试
  已由 Internal Euler 交付并经父级 Review #4 `0/0/0`（08:42，owner 已释放）。**最终门遵守：**在父级对
  28P 测试复审/named tests/适用 build 闭合前，本卡不得 source/build approved——本 Worker 只交付，不越门。
- 与其它 active owner 写集冲突检查：External C 持 TURN-34B（`TaskMaintenanceService.java` +
  `TaskMaintenanceTurnContractTest.java`）零重叠；TURN-28P Euler 两测试为
  `TurnCapturePixelChangeProbeContractTest.java` + `LocalTurnActionExecutorContractTest.java`，与本卡写集
  互斥且其 owner 已于 08:42 释放；External A/B/D 当前无其它持卡（ACTIVE_WORK 15:14/16:36 确认）；两仓
  `git status` 既有 dirty/untracked 全部保护，零 Git mutation。
- 禁令确认：不运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；只允许
  任务卡明确授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named tests 且当前有 Java writer（External C）活动，
  故本 Worker 一律不跑；不回滚/覆盖/清理/提交/暂存；不改写集外文件；不改业务阶段/判断条件/OCR/点击/
  导航顺序/重试/fallback/terminal/UUID/窗口绑定/输入顺序。
- 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-22 EXTERNAL-A WHOLE-CARD CLAIMED session-current 2026-07-16T17:28:47-04:00 cloudtest=d270d7dc executor=a64422b0 dhxytest=f5a7992f service=cd1cd365 assembly=4435b30c -->

## EXTERNAL-A TURN-22 WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-16T17:30:37-04:00

EXTERNAL-A TURN-22 WHOLE-CARD SOURCE+TEST DELIVERED

- 交付时间：`2026-07-16T17:30:37-04:00`。Implementation Worker delivery only；**不是** approval/review 结论，
  不含 `APPROVED/CLOSED`，不自批；父级是唯一 manager/final reviewer。
- 诚实声明（本轮字节变化范围）：本轮**未修改任何 Java 文件**，只写本报告的 claim/delivery 两段。原因：
  整卡 Repair #3 的三项修复（Review #4 P1-1/P1-2/P2-1）此前已分别以本卡 08:18 item ①、TURN-22C1、
  TURN-22D1 落盘并逐一通过父级 source/test-source review（C1 `0/0/0`；D1 `0/0/0` + 独立 review 2/2 +
  DHXY compile exit 0），且五个目标文件当前字节经本 Worker 逐一独立复算与父级接受值完全一致。在此状态上
  重写任何字节都只会制造无意义 churn 并破坏已审证据。本段的交付内容是：整卡聚合 + 本 Worker 对完整
  production/test 的独立复核证据，使父级可按 14:47 整卡纪律对**完整 TURN-22** 执行一次 whole-card review。
- 完整改动文件（整卡 Repair #3 modify 写集，字节即父级已接受状态）：
  1. Cloud `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
  2. DHXY `DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
  3. DHXY `DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
  4. 本固定报告（claim + delivery 两段 append）
- 每个文件行数与 SHA-256（交付时复算，与领取点一致、零漂移）：
  | 文件 | 行数 | SHA-256 |
  |---|---:|---|
  | Cloud `service/TeamReturnTurnContractTest.java` | 1612 | `d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab` |
  | DHXY `cloud/turn/TurnInputStepExecutor.java` | 264 | `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e` |
  | DHXY `cloud/turn/TurnInputStepExecutorContractTest.java` | 695 | `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81` |
  | Cloud `service/TeamReturnService.java`（整卡 production，Repair #3 只读） | 522 | `cd1cd365bff90b16817c15831a2685f2feae84e2d49893b9b975362d4ec4edaf` |
  | Cloud `remote/CloudTeamReturnPortAssembly.java`（整卡 production，Repair #3 只读） | 538 | `4435b30c4bfc923e222b12de3cda5be9aeec766aa1f826f26ea534bc1a5cfd66` |
- production 行为说明（整卡端到端，本 Worker 独立复核）：
  - `TeamReturnService.java`：成员路径保持 `observe -> ensureSheYaoXiangActive once -> observe -> 随机 +-3 ->
    click`；leader initial/poll(3000ms)/timeout(120000ms) 与 precheck（handle 返回前唯一一次 capture+match、
    consume 只读 future、missing/stale/not-ready/failed inconclusive 走 live fallback）均按 `696a12b0`。
  - `CloudTeamReturnPortAssembly.java`：每次 observation 恰一个 `CAPTURE` JSON（`(left+342,top+57,272x69)`
    raw PNG、`0.85` 双模板 Cloud match、frame purpose/region/dimensions/sourceStep/PNG/SHA/decoded pixels 严格
    核验）；点击为**单个 typed `CLICK_LEFT`**（`clickDelayMs=150`+`queueHoldMs=500`，单 UUID/command、无
    frame、零 transport retry）；COMPLETED->EXECUTED、FAILED->NOT_EXECUTED、STOPPED->STOPPED、
    DUPLICATE_OR_UNCERTAIN->UNKNOWN，uncertainty 不伪 ABSENT/PRESENT/成功。
  - `TurnInputStepExecutor.java`（Review #4 P1-2 闭合）：mouse 路径 `:186-190` 把完整 immutable action list
    一次交给 frozen exact-window 边界 `inputActionQueue.submitFrozenExactWindowActionsAndWait(description,
    window.context(), window.binding(), actions)`；本文件 legacy `submitAndWait` 出现次数=0；typed
    `STOP_REQUESTED->STOPPED`，其余 incomplete/uncertain 一律 failure，不伪成功、不 retry。真实 DHXY 侧
    `CLICK_LEFT(150)+SLEEP(500)` 因此与 `696a12b0:TeamReturnService.java:86-89` 同 queue 原子片段等价。
  - 静态零门（本 Worker grep 复算，全为 0）：两个 production 文件对 `TEAM_RETURN_BUTTON/
    TEAM_RETURN_LEADER_SIGNAL/readWindowFact/WindowFact/executeInputBundle/GameClientTracker/
    CoordinateHelper/InputSequences/InputProvider/WindowTaskContextHolder` active path 零引用。
  - 模板双仓 parity（复算一致）：`gui.png`=`5B4C2C43...E4E21`、`zhao.png`=`2468C531...ECB D3`
    （完整值见 01:49 冻结 brief，两仓四文件逐字节同 SHA）。
- named test source：
  - Cloud `TeamReturnTurnContractTest.java`（Review #4 P1-1 闭合）：无任何 DHXY-only mechanics
    import（本 Worker 复核：`com.bot.dhxy` import 仅 protocol/双仓同字节类与 Cloud tree 既有类型；
    `TurnInputStepExecutor/InputActionQueue` 仅出现于 javadoc 指引）；保留真实 assembly/JSON 合同断言：
    单步 `INPUT/CLICK_LEFT`、`clickDelayMs()==150`/`queueHoldMs()==500`（`:1281-1282`）、无 WAIT/frame、
    一 command/一 UUID、closed terminal/uncertain fail-closed、零 retry、member/leader PNG byte+SHA parity 与
    `TurnLocalOperation` 九值 allowlist（C1 行为化清理后无 source-string scan）。
  - DHXY `TurnInputStepExecutorContractTest.java`（Review #4 P2-1 闭合）：经 production public seam
    `TurnExecutionWindow.resolveForAction(...)`（`:494`，全文件 `getDeclaredConstructor`=0）；预装**不同**
    sentinel context 并断言逐字节恢复（`:298-314`）；单次 frozen submission（`:320`）、`assertSame` exact
    context/binding、`[CLICK_LEFT(150), SLEEP(500)]` 顺序、typed STOP 零 retry（`:366`）、真实 production
    queue 上 A->B->A drift 零 enqueue/零 input。
- 基线核对：已对照 `docs/业务逻辑.md` 本地队伍边界与 `WAIT_TEAM_RETURN` 规则（信号在则保持等待、信号
  消失按来源继续、已验证回城快照不得被归队等待清除、stop/pause 不包装成业务失败、轮询 3000ms 不受
  watchdog）及 `696a12b0` `TeamReturnService` 全 public surface；点击原子性与
  `696a12b0:TeamReturnService.java:86-89` 一次 `submitAndWait` 内 `clickLeft(...,150)+sleep(500)` 等价。
- 有意业务差异：无。
- 未运行项目：Maven、JUnit、compile、package、runtime、application、server、Task、poller、UI、capture、
  input 全部未运行（External C 为活动 Java writer，共享 Maven 禁用；named tests 留给父级 stable-writer
  cohort）。零 Git mutation；两仓既有 dirty/untracked 全部保护未动。
- 已知阻断（不由本卡写集解决）：
  1. 注册表最终门：`28P 测试复审前不得 source/build approved`——TURN-28P 两测试已经父级 Review #4
     `0/0/0`（08:42），是否满足该最终门由父级裁决；
  2. DHXY 全仓 reactor testCompile 债（Summon/Xiuluo/NPC/Dialog/CR138/TeamRole 等 stale tests）阻断
     `TurnInputStepExecutorContractTest` 点名运行（父级 14:40 D1 build gate #1 已记录，写集外）；
  3. Cloud 整仓 compile 债（TURN-23/26/28 等归还卡）阻断 `TeamReturnTurnContractTest` 点名运行，写集外。
- 父级审核请求：请执行完整 SOURCE+TEST SOURCE REVIEW（whole-card：TeamReturnService + Assembly +
  Cloud named test + DHXY executor + DHXY named test 五文件聚合）。交付后本 Worker 立即停止修改本卡，
  等待父级结论；REPAIR REQUIRED/BLOCKED 时由本 Worker 整卡返修。

TRUE_EOF

<!-- TRUE_EOF: TURN-22 EXTERNAL-A WHOLE-CARD SOURCE+TEST DELIVERED 2026-07-16T17:30:37-04:00 zero-java-drift aggregated-repair3 cloudtest=d270d7dc executor=a64422b0 dhxytest=f5a7992f service=cd1cd365 assembly=4435b30c AWAITING-PARENT-WHOLE-CARD-REVIEW -->

## PARENT WHOLE-CARD SOURCE+TEST SOURCE REVIEW #5 - PASSED - 2026-07-16T17:41:00-04:00

- 父级已完整读取本卡、权威计划第 14-19 节、HTTPS turn protocol/foundation、`docs/业务逻辑.md`、
  两仓 status、五个 production/test source，并与唯一业务基线
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 逐项核对。
- 结论：**`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`**。本轮是零 Java 漂移的整卡聚合：
  Cloud test `d270d7dc...` 等于 TURN-22C1 Parent Review #1 通过字节；DHXY executor/test
  `a64422b0...`/`f5a7992f...` 等于 TURN-22D1 Parent Review #2 与独立 review 2/2 通过字节；
  `TeamReturnService`/`CloudTeamReturnPortAssembly` `cd1cd365...`/`4435b30c...` 等于本卡此前父级接受字节。
- 完整调用链保持：member `observe -> ensureSheYaoXiangActive once -> observe -> +-3 -> click`；leader
  precheck 只消费同 window/taskRun 快照，missing/stale/not-ready/failed 回 live detector；poll 为 3000ms、
  timeout 为 120000ms；stop 不包装为业务失败。点击仍为一个 typed `CLICK_LEFT(150/500)` command，
  DHXY executor 一次提交完整 `[CLICK_LEFT(150), SLEEP(500)]` 到 frozen exact-window queue；resolve/enqueue
  漂移零输入，STOP typed 传播，uncertain 不伪成功、无 transport retry。
- Test-source 覆盖 member/leader raw PNG+SHA/template parity、单 command/UUID、closed terminal、exact
  device/window/title/HWND/process/rect、sentinel context 恢复、一次 frozen submission 与 A-B-A drift 零 enqueue。
  本次没有新增 source-string scan、跨仓 mechanics import、第二 frozen wrapper、session/ledger/TTL/retry。
- TURN-28P Parent Review #4 已为 `0/0/0`，本卡 final source gate 已满足。External A implementation owner
  自本段 true EOF 起释放；进入两名 fresh independent whole-card reviewer 门，尚非 CARD APPROVED。
- 未运行 Maven/JUnit/compile：TURN-26/TURN-28/其他 Java writer 仍活动，且 DHXY reactor testCompile 与
  Cloud compile 的共享债务已另卡记录。后续 stable-writer 只运行授权 named tests 与适用 compile。

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-22 PARENT WHOLE-CARD REVIEW-5 PASSED P0P1P2=0/0/0 SOURCE-TEST-SOURCE-REVIEW-PASSED OWNER-RELEASED INDEPENDENT-WHOLE-CARD-REVIEW-0OF2 BUILD-PENDING 2026-07-16T17:41:00-04:00 -->

## USER REVIEW-GATE OVERRIDE - PARENT REVIEW ONLY - 2026-07-16T17:43:00-04:00

- 用户明确要求本轮及后续 CR271 只由父级 final reviewer 审核，不创建两名独立 reviewer。
- 因此本卡 Parent Review #5 的 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` 即为完整源码/
  测试源码审核结论；independent review 门按用户指令取消。未创建任何 reviewer。
- 本卡仍待 stable-writer authorized named tests/适用 compile；该构建门与 reviewer 数量无关。

<!-- TRUE_EOF: TURN-22 USER-OVERRIDE PARENT-REVIEW-ONLY NO-INDEPENDENT-REVIEW SOURCE-REVIEW-PASSED BUILD-PENDING 2026-07-16T17:43:00-04:00 -->
