# TURN-21 - CommonBoxService HTTPS turn cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15 23:12 EDT

- 状态：`READY`；类型：`COUNT`；唯一
  `countUnit=AutoCombatService -> CommonBoxService::detectMemberBoxAfterCombatExit`，`countDelta=+1`。
  Service 其余 leader/detect/consume/hasPending callers 同卡集成但不得重复计数。父级是唯一 manager/final reviewer，
  Worker 不是 reviewer。
- startDependsOn：`TURN-18`、`TURN-13C`、`TURN-09R` 均已过父级源码门；approvalDependsOn：本卡 parent
  source review、`CommonBoxTurnContractTest` 与适用 Cloud compile/build。
- 目标：Cloud 继续拥有 role gate、task/run/window/identity key、priority 与 30 秒 pending；DHXY 只执行 exact-window
  capture 和物理 click。旧 `COMMON_BOX` fact/`executeInputBundle` 从本 Service active path 归零。

### Exact write set

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java`
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/CommonBoxTurnContractTest.java`
- 本报告 true EOF append。

其余两仓文件全部只读；尤其 `CloudCommonBoxPort.java`、`CommonBoxObservationResult.java`、
`CommonBoxClickResult.java`、`CloudServiceConfiguration.java`、protocol、DHXY、caller、Task、POM、模板与其它
测试/报告不得修改。现有 port signature/bean wiring 足够，不得新增第二 port/model/helper/wrapper。保护共享
dirty/untracked，不回滚、覆盖、清理、提交或执行其它 Git mutation。

### Frozen production contract

- exact window metadata 只读一次并校验 bound device/window；窗口基准取真实 `windowRect.left/top`，坐标不缩放。
  ROI 保持 `left+623, top+590` 到 exclusive `left+682, top+618`，即 `width=59,height=28`。
- detect 发一份 `CAPTURE/UPLOAD_IMAGE` command，严格校验 action/window/step/frame、region、raw PNG SHA 与像素尺寸；
  Cloud 用 live template `images/template/common/leader_box_marker.png` 和 threshold `0.86` 在同一原图匹配。
  命中中心换算 screen-absolute，Cloud 在真实 match 完成时记录 `matchedAtEpochMs`，由 Service 建立 30,000ms pending。
- 保持基线 benign miss：capture unavailable、template unavailable、not matched、known mechanics/not-executed 均不建
  pending；confirmed STOPPED 走 checkpoint，未确认 STOPPED/transport uncertain/correlation mismatch fail closed，不能
  伪造成普通 miss/success。零自动 retry、第二 capture 或本地模板 match。
- pending key、role toggle、leader/member exact role、supported task、taskRunId、window/nativeHandle、identityEpoch、
  30 秒 expiry、clear-by-role 与 prune 语义不变。consume 前先检查全部 stale gate；无 pending/disabled/stale 路径零
  command。
- 有效 pending consume 只发一份 ordered action：同一 screen-absolute 点 `MOVE_MOUSE`、`WAIT 80ms`、
  `CLICK_LEFT`，连续 mouse fragment 只进一次全局 queue。known input failure 保留 pending 至 TTL；成功才删除；
  STOPPED/uncertain 按上条处理。caller 既有“盒子优先于 team-return/first-aid/其它 maintenance”顺序不得改变。

### Named-test acceptance

- `CommonBoxTurnContractTest` 必须实例化 production Service 与 production assembly/`TurnGameClient` 路径，不能只测
  复制 mapper。覆盖 member combat-exit detection 所绑定的真实 public path，并静态/运行断言既有 caller priority
  不被本卡改写。
- 至少覆盖 leader/member match、role mismatch/disabled、unsupported task、missing/changed taskRun、window/hwnd/
  identity mismatch、29,999ms valid 与 30,000ms expired、role clear、capture/template/match miss、known input failure
  retain、success remove、confirmed STOPPED、uncertain/correlation mismatch。
- 逐案断言非零 window origin 下 exact ROI、不缩放、raw PNG metadata/hash/dimensions、threshold/center、pending timestamp
  与 TTL、ordered `MOVE -> WAIT(80) -> CLICK`、每 command 唯一 UUID、零 retry。detect match 为一 capture command；
  valid consume 为一 input command；所有短路路径 command 数必须 exact。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级待 writers 稳定后只运行
  用户已授权的 named test 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-21 parent-frozen-brief -->

## CLAIMED - 2026-07-15 23:46:53 EDT

- 角色：`CR271 TURN-21 implementation Worker`；不是 reviewer；父级是唯一 manager/final reviewer。
- Exact write set 仅为 Cloud `CommonBoxService.java`、现有 `CloudCommonBoxPortAssembly.java`、新建
  `CommonBoxTurnContractTest.java` 与本报告 true EOF；其余两仓全部只读。
- 基线已核对：`docs/业务逻辑.md` 通用盒子检测/30 秒 pending/最高维护优先级规则，
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `CommonBoxService`、`AutoCombatService` 与
  `AutoBattleTask` 对应源码，以及 CR271 权威计划第 14-19 节和 HTTPS turn 协议规格。
- 禁令：不修改 existing port/result/config/protocol/DHXY/caller/Task/POM/template/其它测试；不新增第二
  port/model/helper/wrapper；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；
  不执行 Git mutation，不回滚、覆盖、清理或提交任何共享 dirty/untracked。

<!-- TRUE_EOF: TURN-21 claimed -->

## REPLACEMENT CLAIMED - 2026-07-16 00:01:30 EDT

- 前一会话 Parfit `019f6901-bbf6-7c00-913f-e03c87064fbf` 已返回 `not_found`；由 replacement implementation
  Worker `019f6914-252c-7500-ae9d-0249bdc94294` 接续 TURN-21，保留并核对其所有可能已落盘半成品。
- 继承 exact write set：Cloud production
  `dhxy-cloud-brain/src/main/java/com/dhxy/cloudbrain/service/CommonBoxService.java`、
  `dhxy-cloud-brain/src/main/java/com/dhxy/cloudbrain/turn/client/CloudCommonBoxPortAssembly.java`；唯一 named test
  `dhxy-cloud-brain/src/test/java/com/dhxy/cloudbrain/service/CommonBoxTurnContractTest.java`；以及本固定报告 true EOF。
- replacement 仍不是 reviewer；父级是唯一 final reviewer。写集外全部 dirty/untracked 只读保护，不回滚、不覆盖。

<!-- TRUE_EOF: TURN-21 replacement-claimed -->

## REPLACEMENT CLAIM CORRECTION - 2026-07-16

- replacement claim 中抄写的 Cloud production/test 路径有误；以下三条冻结 absolute path 覆盖并取代该错误
  claim 的 write-set 路径，除此之外写集不变：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/CommonBoxTurnContractTest.java`
- replacement Worker `019f6914-252c-7500-ae9d-0249bdc94294` 尚未创建或修改任何 production/test 文件，亦未创建或
  修改错误 claim 所写的 `com/dhxy/cloudbrain/...`、`turn/client/...` 路径；截至本修正仅追加了本固定报告。
- 前一会话可能落盘的内容仍按冻结路径核对并保护；若后续发现错误路径已有他人或前一会话内容，不删除、不回滚，
  先报告父级。

<!-- TRUE_EOF: TURN-21 replacement-claim-corrected -->

## SOURCE DELIVERED + TEST DELIVERED - 2026-07-16 00:36:47 EDT

- replacement implementation Worker `019f6914-252c-7500-ae9d-0249bdc94294` 已完成 TURN-21 源码与唯一
  named test 交付；仍不是 reviewer，父级保留唯一 final review/运行门禁权。
- 实际 production write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
    （468 行，SHA-256 `D7B36BE2D6471FB2199D3C9012484E93298E629817D3EC270D14D01B32B62C84`）
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java`
    （503 行，SHA-256 `0F04B0509B0CD1B89CC22DB0AF5D88EB15B571DB35A6B34AE6EF3A176CD8C88C`）
- 实际 test write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/CommonBoxTurnContractTest.java`
    （1168 行、13 个 `@Test`，SHA-256
    `2A08952F23A2B6E9D3B0E267BFE605F6F8A696CB9A191E004D24ECFC4BB8A06C`）。该仓
    `.gitignore:15` 当前忽略整个 `src/test/`；本 Worker 遵守禁令未执行任何 Git mutation，文件本体已按 exact path
    保留供父级门禁使用。
- correction 复核：错误 claim 的三条 `com/dhxy/cloudbrain/...` / `turn/client/...` production/test 路径仍均不存在；
  replacement 未在错误路径创建或修改文件，也未删除、回滚或覆盖共享 dirty/untracked。

### Source evidence

- `CommonBoxService` active path 对 `WindowFactKind.COMMON_BOX`、`readWindowFact`、`executeInputBundle`、`InputBundle`
  均为零引用；Spring production constructor 已显式选择。Service 保留 role toggle、leader/member exact role、仅
  `xiuluo_v2`/`wubei`、taskRun/window/nativeHandle/identity stale gate、clear-by-role、29,999ms 有效与 30,000ms
  到期、known input failure 保留 pending、成功才删除。
- turn-native context 不再提供旧 `getPlayerIdentityEpoch()` authority；Service 对 legacy context 仍使用原
  `playerIdentityEpoch`，对 turn-native context 使用该 exact task context 的 immutable initial window title + process
  identity fingerprint，并与 window/nativeHandle/taskRun gate 同时校验；未增加 session、ledger 或第二 TTL。
- `CloudCommonBoxPortAssembly` 源码只有一处 `latestWindowMetadata()`、一处 `turnGameClient.capture(...)` 和一处
  `turnGameClient.execute(...)`。detect 使用非零 origin 的 screen-absolute `left+623,top+590,59x28`、
  `UPLOAD_IMAGE` raw PNG，并校验 exact snapshot/action/window/step/frame/region/SHA/实际像素尺寸；只在该 frame 上
  加载 live template `images/template/common/leader_box_marker.png` 并按 `0.86` 匹配，命中中心换算为
  screen-absolute，match 完成时生成 pending timestamp。
- valid consume 只构造一份 ordered action：同一 screen-absolute 点 `MOVE_MOUSE -> WAIT(80ms) -> CLICK_LEFT`；无
  第二 command、自动 retry、session、ledger 或新 TTL。known failure、confirmed STOPPED、unconfirmed STOPPED、
  transport uncertain 与 correlation mismatch 的闭合终态按 frozen brief 保持，STOPPED 亦校验真实 step/frame 形状。
- named test 实例化 production `CommonBoxService`、production assembly 与 `TurnGameClient`，覆盖 leader/member match、
  role disabled/mismatch、unsupported/missing/changed taskRun、window/hwnd/identity mismatch、29,999/30,000ms、role
  clear、capture/template/match miss、known input failure retain、success remove、STOPPED、transport uncertain、
  action/window/step/frame/region/SHA/pixel correlation、唯一 UUID 与零 retry；并只读断言 `AutoCombatService` 的
  member combat-exit public caller 唯一及 common-box 先于 first-aid/combat maintenance，`AutoBattleTask` 中先于
  team-return。
- 已核对 `docs/业务逻辑.md` 的“通用盒子检测、30 秒 pending 与最高维护优先级”基线及
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 对应 `CommonBoxService` / `AutoCombatService` /
  `AutoBattleTask`；**无已批准业务差异；按基线等价迁移。**

### Verification boundary

- 按父级冻结禁令，本 Worker **未运行** Maven、JUnit、compile、package、runtime、application、server、Task、UI、
  capture 或 input；未声称测试/编译通过。已完成的证据仅为逐行 source review、只读调用计数、路径/哈希/EOF 与
  caller priority 检查；named test 与适用 Cloud compile/build 留给父级 writers 稳定后的唯一运行门禁。

<!-- TRUE_EOF: TURN-21 replacement-source-test-delivered -->

## PARENT SOURCE REVIEW - REPAIR #1 REQUIRED - 2026-07-16 00:43 EDT

- Parent independently reviewed both production files, the complete named-test source, the real
  `AutoCombatService`/`AutoBattleTask` callers, TURN-13C bound-client contract, TURN-09R execution semantics, and
  the `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` baseline. Worker statements were not used as approval.
- Verdict: `P0/P1/P2=0/3/0`; `REVIEW REQUIRED / NOT SOURCE APPROVED`.
- **P1 exact-window pre-port guard is missing.** `CloudCommonBoxPortAssembly` calls the shared, unbound
  `TurnGameClient` for latest metadata, capture, and execute, then checks context only after port access/execution.
  Bind `exactContext.getTurnInvocationContext()` first and use only that bound view. Add a production-path test
  proving wrong current context fails before metadata/command port access.
- **P1 baseline click delay is dropped.** Baseline `696a12b0` calls `moveAndClickLeft(...,80,120)`, while DHXY maps
  protocol `CLICK_LEFT` with delay `0`. The assembly validates `clickDelayMs=120` but emits only three steps.
  Preserve the baseline in the same command as
  `MOVE_MOUSE -> WAIT(80) -> CLICK_LEFT -> WAIT(120)` and assert the exact four-step order. This is not a retry or
  second command.
- **P1 turn-native identity fence is stale.** `CommonBoxService.identityKey(...)` falls back to the immutable
  initial window title/process. If latest exact metadata changes title while the same turn-native
  `TaskExecutionContext` remains alive, the old pending passes the Service stale gate and can reach click. Preserve
  the baseline identity fence by validating the current bound metadata identity before consume/has-pending and
  clearing/rejecting old pending before any input. Add a same-context/latest-title-changed negative test with zero
  click command. Do not introduce an owner/session/ledger/cache/TTL or a second transport command.
- Repair #1 retains the original card write set only:
  `CommonBoxService.java`, `CloudCommonBoxPortAssembly.java`, `CommonBoxTurnContractTest.java`, and this report.
  Protocol/result/port signatures, DHXY, callers, Tasks, host/routes/POM/templates remain read-only. Keep one fresh
  UUID per command and zero automatic retry/session/ledger/TTL.
- Parent did not run Maven/JUnit/compile because TURN-19 Repair #1, TURN-25, and TURN-23 Java writers are active.

<!-- TRUE_EOF: TURN-21-parent-repair-1-required -->

## REPAIR DELIVERED - REPAIR #1 - 2026-07-16 00:56:18 EDT

- replacement implementation Worker `019f6914-252c-7500-ae9d-0249bdc94294` 已按父级
  `P0/P1/P2=0/3/0` 结论续修三项 P1；本 Worker 仍不是 reviewer，本段只声明 repair source/test delivery，等待父级
  独立复审。
- Repair #1 实际文件与 SHA-256：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
    （515 行，`93E93321AE4CBDD29C3D94AF4172D72AE8FEFE137CF417E7F2C570B93856CE68`）
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java`
    （510 行，`BC60C0980B5EDB3CFB220C68FAB98C808FDE3FF486E390D442588D7267D0AE7D`）
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/CommonBoxTurnContractTest.java`
    （1283 行、15 个 `@Test`，`6C3FFA9E9CA303A8A755668603533ABCA19D70EC6D58737403A5EE4E0EAE6D50`；
    文件仍受仓库 `.gitignore:15 src/test/` 影响，本 Worker 未执行 Git mutation）。

### P1 closure evidence

1. **exact-window pre-port guard**
   - `observe(...)` 与 `click(...)` 都在取得 non-null `exactContext` 后立即读取
     `exactContext.getTurnInvocationContext()`，并调用共享 singleton `turnGameClient.bind(binding)`；后续
     `latestWindowMetadata/capture/execute` 只通过该 `boundClient`。assembly 对共享 unbound client 的三类直接调用均为
     零，源码计数为 bind=2、bound metadata=1、bound capture=1、bound execute=1。
   - production assembly + real `TurnGameClient` 负例在 holder 中绑定 wrong current window、向 assembly 传 expected
     context；observe 在 metadata port 前、click 在 command port 前均抛出，精确断言 `metadataReads=0`、
     `executeCalls=0`。

2. **baseline click delay retained in the same command**
   - valid consume 仍只有一个 fresh UUID/command，ordered steps 精确为
     `0 MOVE_MOUSE(x,y) -> 1 WAIT(80ms) -> 2 CLICK_LEFT(x,y) -> 3 WAIT(120ms)`；末尾 120ms 是
     `696a12b0 moveAndClickLeft(...,80,120)` 的 post-click delay，不是 retry 或第二 command。
   - production `requireInputSteps` 对四步 `COMPLETED`、`FAILED`、`STOPPED` terminal 全量关联；named test 的 success、
     known failure 与 unconfirmed input STOPPED 均走 production assembly，逐字段断言四步 index/type/action/input/wait，
     pending 的 success-remove / failure-retain / STOPPED fail-closed 语义不变。

3. **turn-native current identity fence**
   - pending 建立仍不增加第二 detect metadata read；legacy context 的 stored/current identity 均继续使用
     `playerIdentityEpoch`。turn-native `consume`/`hasPending` 在 pending 命中后，通过 context 自带 bound
     `TurnGameClient` 的 `latestWindowMetadata()` 读取当前 exact device/window 的 immutable in-memory metadata，并用
     current title/process/nativeHandle 与 stored identity 比较。
   - same `TaskExecutionContext` 下只改变 latest title 的 production-path 负例：`hasPending` 返回 false，
     `metadataReads=2`、`executeCalls=1`（仅原 detect capture）；独立 consume 负例在 click 前移除旧 pending，
     `metadataReads=3`、`executeCalls=1`，恢复原 title 后 pending 仍为空且 metadata 读数不增加。这里没有新增 transport
     command、owner、session、ledger、cache 或 TTL。

### Preserved boundaries

- `WindowFactKind.COMMON_BOX`、`readWindowFact`、`executeInputBundle`、`InputBundle` 在 Service active path 继续零引用；
  detect 仍为一份 exact raw PNG capture，Cloud 同 frame `0.86` match，pending 仍为 30,000ms，role/task/run/window
  gate、clear-by-role、caller priority、known failure retain 与零自动 retry 不变。
- protocol/result/port signatures、DHXY、caller、Task、host/routes、POM、templates、其它测试/报告均未修改；三个错误
  replacement-claim 路径仍不存在，未回滚、删除或覆盖共享 dirty/untracked。
- **无已批准业务差异；Repair #1 恢复遗漏的 `696a12b0` exact-window fence、80/120 click delay 与 current identity
  fence。**

### Verification boundary

- 遵守父级禁令，本 Worker **未运行** Maven、JUnit、compile、package、runtime、application、server、Task、UI、
  capture 或 input，也未声称 test/build 通过。证据为完整 source review、源码调用计数、生产路径 scripted-port 精确
  assertions、文件 SHA/行数/true EOF 检查；运行门禁与最终判断留给父级。

<!-- TRUE_EOF: TURN-21-repair-1-delivered -->

## PARENT SOURCE + TEST-SOURCE RE-REVIEW - REPAIR #1 PASSED - 2026-07-16 01:02 EDT

- 审查角色：CR271 父级唯一 manager/final reviewer。父级独立复读两个 production 文件、完整 named-test source、
  `TaskExecutionContext` bound-client 合同、真实 caller 与 `696a12b0` CommonBox 基线，并重算交付文件 SHA；不以
  Worker 自述替代结论。
- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING`。Repair #1 的三项 P1
  全部关闭，Boole owner 可释放；这不是 named-test/build 已通过或 CARD CLOSED。
- **exact pre-port binding 已关闭：**`CloudCommonBoxPortAssembly.java:91-100,223-225` 在 observe/click 任何
  metadata/capture/execute 前先取 exact invocation context 并建立 bound `TurnGameClient`；测试
  `CommonBoxTurnContractTest.java:154-188` 用 wrong current context 锁住
  `metadataReads=0/executeCalls=0`。
- **基线点击时序已关闭：**`CloudCommonBoxPortAssembly.java:238-256` 在一个 UUID/command 内精确发出
  `MOVE_MOUSE -> WAIT(80) -> CLICK_LEFT -> WAIT(120)`；`CommonBoxTurnContractTest.java:1029-1061` 逐 step
  断言四步坐标、类型与 80/120ms，FAILED/STOPPED/COMPLETED 仍走完整 step correlation；无第二 command 或 retry。
- **current identity fence 已关闭：**`CommonBoxService.java:141-156,446-478` 对 turn-native pending 在
  has/consume 时读取同一 exact bound client 的当前 metadata，并以 title/process/HWND 对 stored identity；旧 pending
  在 click 前被拒绝并由 consume 清除。`CommonBoxTurnContractTest.java:258-302` 覆盖同 context latest title 改变、
  零 click command 及恢复标题后 pending 仍为空；legacy identity epoch、30 秒 TTL、role/task/window gate 均未改。
- 交付哈希与报告一致：`93E933...`、`BC60C0...`、`6C3FFA...`。父级未运行 Maven/JUnit/compile，因为
  TURN-T04 与 TURN-23 writer 仍活动；named test 与适用 Cloud compile/build 进入 stable-writer cohort。

**无已批准业务差异；按 `696a12b0` exact-window、identity fence 与点击时序等价迁移。**

<!-- TRUE_EOF: TURN-21-parent-repair-1-passed -->
