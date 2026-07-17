# TURN-19 - LeftTopStatusSwitchService HTTPS turn cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15 23:12 EDT

- 状态：`READY`；类型：`COUNT`；唯一
  `countUnit=AutoCombatService -> LeftTopStatusSwitchService::handleCombatMaintenance`，`countDelta=+1`。
  Service 其余既有 public caller 同卡集成但不得重复计数。父级是唯一 manager/final reviewer，Worker 不是 reviewer。
- startDependsOn：`TURN-13C`、`TURN-09R` 均已过父级源码门；approvalDependsOn：本卡 parent source review、
  `LeftTopStatusTurnContractTest` 与适用 Cloud compile/build。
- 目标：保留 `696a12b0` 的 task/role/pending/safe-window 决策；把旧 `LEFT_TOP_STATUS` fact 与
  `executeInputBundle` transport 换成最小 HTTPS turn：exact ROI raw PNG 上传后在 Cloud 同帧匹配；只有 OPEN 且
  caller 允许 click 才下发一份原子 `MOVE_MOUSE -> WAIT -> CLICK_LEFT` JSON action。

### Exact write set

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java`
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`
- 本报告 true EOF append。

其余两仓文件全部只读；尤其 `CloudLeftTopStatusPort.java`、`LeftTopStatusObservationResult.java`、
`LeftTopStatusClickResult.java`、protocol、DHXY、caller、Task、host/routes、POM、模板与其它测试/报告不得修改。
现有 port signature 足够，不得新增第二 port/model/helper/wrapper。保护共享 dirty/untracked，不回滚、覆盖、清理、
提交或执行其它 Git mutation。

### Frozen production contract

- exact window metadata 只读一次并校验 bound device/window；窗口基准是 metadata 返回的真实
  `windowRect.left/top`，不是固定 `(0,0)`。坐标严格不缩放。
- ROI 固定为 `left+8, top+147, width=11, height=19`；一份 `CAPTURE/UPLOAD_IMAGE` command 返回 raw PNG。
  校验 action/window/step/frame、region、PNG SHA 与像素尺寸；同一原图分别匹配 Cloud live templates
  `images/template/status/left_top_open.png`、`left_top_closed.png`。
- 保持基线判定：OPEN=`open>=0.90 && open>=closed+0.02`；CLOSED=`closed>=0.90 && closed>open`；否则 UNKNOWN。
  OPEN click point 为 open template 在 ROI 内的真实中心换算成 screen-absolute 坐标；capture 不可用映射
  CAPTURE_FAILED。confirmed STOPPED 走 checkpoint；未确认 STOPPED/transport uncertain/correlation mismatch
  fail closed 抛错，不得映射 ordinary miss、false 或 success。
- probe-only caller 在 OPEN 时只维护既有 pending，零 input command；CLOSED/UNKNOWN/CAPTURE_FAILED 均零 click。
  allow-click OPEN 路径再发恰好一份 ordered action：同一点 `MOVE_MOUSE`、`WAIT 120ms`、`CLICK_LEFT`，连续 mouse
  fragment 由 TURN-09R 保证只进一次全局 queue。input known failure 保持 clicked=false；STOPPED/uncertain 按上条
  处理。零自动 retry、第二 capture、fallback、TTL、session 或 ledger。
- 原 public API、supported task gate、requested-over-task 选择、pending mark/consume、combat cadence、日志语义与
  caller 顺序不变；不得使用 `LeftTopStatusDecision` 中过时的 `16x29` 几何。

### Named-test acceptance

- `LeftTopStatusTurnContractTest` 必须实例化 production Service 与 production assembly/`TurnGameClient` 路径，
  不能只测复制的 mapper。使用实际 Cloud template 或确定性同字节 fixture 生成 same-frame scores。
- 覆盖 OPEN leader/combat click、OPEN member probe zero-input、CLOSED、UNKNOWN、capture unavailable、known input
  failure、confirmed STOPPED、uncertain/correlation mismatch、unsupported task/context-null 零 command。
- 逐案断言 exact ROI 与真实非零 window origin、不缩放、raw PNG metadata/hash/dimensions、OPEN 阈值+margin、
  ordered `MOVE -> WAIT(120) -> CLICK`、pending mark/clear/retain、每个 command 唯一 UUID、零 retry。OPEN click 路径
  为一次 capture command 加一次 input command；非 action 路径只能有冻结合同允许的 command 数。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级待 writers 稳定后只运行
  用户已授权的 named test 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-19 parent-frozen-brief -->

## CLAIMED - 2026-07-15 23:41:26 EDT

- Role: CR271 TURN-19 implementation Worker; parent remains the sole manager/final reviewer.
- Exact write set accepted as frozen. All other files in both repositories remain read-only.
- Baseline checked: `docs/业务逻辑.md` relevant left-top/AutoCombat rules and
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` corresponding source.
- No Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input or Git mutation will be run by this Worker.

<!-- TRUE_EOF: TURN-19 claimed -->

## REPLACEMENT CLAIMED - 2026-07-16 EDT

- Previous implementation session `Godel 019f68ff-f67d-7ba0-958c-91d80ec087ce` is `not_found`; all possible
  on-disk partial work from that session is inherited in place and must not be reverted, overwritten, or redone.
- Replacement implementation Worker agent id: `019f6913-dc31-7e53-90a9-958f96285317`. Parent remains the sole
  manager/final reviewer; this Worker is not a reviewer.
- Inherited exact write set:
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`,
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java`,
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`,
  and this report at true EOF only.
- All other files remain read-only. No Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input or Git
  mutation will be run by this replacement Worker.

<!-- TRUE_EOF: TURN-19 replacement-claimed -->

## SOURCE DELIVERED + TEST DELIVERED - 2026-07-16 00:29 EDT

- Replacement implementation Worker: `019f6913-dc31-7e53-90a9-958f96285317`; predecessor
  `Godel 019f68ff-f67d-7ba0-958c-91d80ec087ce` remained `not_found`. The replacement claim was the
  first write of this session and its on-disk timestamp was `2026-07-16T00:01:40.0913652-04:00`.
  Parent remains the sole manager/final reviewer; this delivery is not a review or approval.
- Exact write set remained frozen. No protocol, DHXY source, caller, Task, host/routes, POM, template,
  other test, other report, dirty file, or untracked file was modified, reverted, cleaned, or replaced.

### Delivered files

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`
  - `306` lines; SHA-256 `03E43188B52E6F07C50E7975B7EEE3C53BDDC4C12D9866FF130A869D5CFE1EF2`.
  - Preserved the predecessor's Cloud-port migration and added only the two omitted baseline public
    template constants, restoring the `696a12b0` public surface.
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java`
  - `416` lines; SHA-256 `9540B58059CB1C3BFAC518AC6D30D7FF71B2ADA88252322A18B59747C6F9B5C8`.
  - This hash is unchanged from the inherited predecessor half-work; its correct content was retained
    in place rather than rewritten.
- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`
  - Created; `975` lines; SHA-256
    `D0ED3410C1FBD03DD6BC45EB6EE55F3DCBE6999FA3E62DB5D9A34C2BB84354CA`.
  - Read-only `git check-ignore -v` reports `.gitignore:15:src/test/`; therefore the delivered file is
    present on disk but intentionally absent from ordinary `git status`. No force-add or other Git
    mutation was performed by this Worker.
- This fixed report was appended only at the then-current true EOF.

### Source evidence

- Baseline checked against
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`:
  supported tasks, requested-over-task resolution, leader/member/combat entry points, member probe
  pending mark, safe-window consume/retain, `11x19` ROI, `0.90` threshold, `0.02` OPEN margin, and
  `120ms` move settle remain equivalent. The two baseline public template constants are retained.
- `docs/业务逻辑.md` current local-team/standalone boundary (especially rows 5-67 and the left-top
  foreground-action rule) and the no-new-TTL/fallback principles were checked. **无已批准业务差异；按
  `696a12b0` 基线等价迁移。**
- `observe(...)` reads exact bound metadata once, derives the unscaled screen-absolute ROI as
  `window.left+8, window.top+147, 11x19`, and submits exactly one `CAPTURE/UPLOAD_IMAGE` command.
  It validates action/window/step/frame correlation, exact snapshot metadata, region, PNG signature,
  SHA-256, metadata dimensions, and decoded pixel dimensions.
- The one validated raw PNG byte array is passed once to `CloudLeftTopTemplateMatcher`, whose paired
  scores load the actual packaged Cloud open/closed templates from the same frame. Only
  `LeftTopStatusDecision.resolveState(...)` is reused; its stale `16x29` geometry is never referenced.
- OPEN converts the real template center back to screen-absolute coordinates. Only an allow-click OPEN
  path submits one second command containing exactly `MOVE_MOUSE` at that point, `WAIT 120ms`, then
  `CLICK_LEFT` at the same point. Probe-only OPEN has zero input command; CLOSED, UNKNOWN, and
  CAPTURE_FAILED have zero click command.
- Known capture failure maps CAPTURE_FAILED; known input failure maps `clicked=false`. Confirmed STOPPED
  reaches the existing checkpoint; unconfirmed STOPPED, uncertain transport, and correlation mismatch
  fail closed. There is no automatic retry, second capture, fallback, TTL, session, ledger, cache,
  task runtime, thread, or lifecycle owner in either delivered production file. The old
  `WindowFactKind.LEFT_TOP_STATUS`/legacy fact client and `executeInputBundle` have zero references in
  the two production files.

### Named-test evidence

- The named test instantiates the production Service, production assembly, and production
  `TurnGameClient`; it uses the actual packaged Cloud templates and an existing production legacy
  context only to exercise the retained pending-state owner. It creates no replacement production
  state model and starts no thread, server, Task, capture, or input runtime.
- Eight tests cover OPEN leader and combat click, member probe zero-input plus safe-window consume,
  known input failure/pending retain, CLOSED/UNKNOWN/capture unavailable, actual template dimensions
  and exact decision boundaries, confirmed/unconfirmed STOPPED and uncertain terminals,
  action/step/window/frame-SHA correlation mismatches, requested-over-task, unsupported task, and null
  context. Assertions include nonzero window origin, exact ROI/no scaling, raw PNG bytes and metadata,
  absolute click center, exact ordered three-step input, pending mark/clear/retain, one fresh UUID per
  command, one metadata read per capture command, exact command counts, and zero retry.

### Worker gate

- Per the parent-frozen brief, this Worker ran no Maven, JUnit, compile, package, runtime, application,
  server, Task, UI, capture, or input command. No test/build result is claimed. The parent must run only
  the authorized `LeftTopStatusTurnContractTest` and applicable Cloud compile/build after concurrent
  writers are stable.
- No Git mutation was performed. Read-only status shows both delivered production files and this report
  as untracked in their existing shared worktrees; the named test is present but ignored as noted above.

<!-- TRUE_EOF: TURN-19 source-test-delivered -->

## PARENT SOURCE REVIEW - REPAIR #1 REQUIRED - 2026-07-16 00:37 EDT

- Parent independently reviewed both production files, the complete named-test source, the real
  `AutoCombatService` caller, `TurnGameClient`, `TurnInputActionMapper`, TURN-09R execution semantics, and
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` baseline. Worker statements were not used as approval.
- Verdict: `P0/P1/P2=0/2/0`; `REVIEW REQUIRED / NOT SOURCE APPROVED`.
- **P1 exact-window pre-port guard is missing.** `CloudLeftTopStatusPortAssembly.click(...)` invokes the shared,
  unbound `turnGameClient.execute(...)` and only compares the returned outcome with `TaskExecutionContext` after
  execution. A mismatched holder/current context can therefore reach physical input before rejection. The same
  assembly already receives the exact `TurnInvocationContext`; bind once with
  `turnGameClient.bind(exactContext.getTurnInvocationContext())` and use that bound view for metadata, capture,
  and execute in both `observe(...)` and `click(...)`. Add a production-path negative test proving mismatched
  current context throws before the command port executes.
- **P1 baseline click delay is dropped.** Baseline `696a12b0` performs move, `120ms` settle, click, then the
  existing `250ms` click delay. The delivered assembly accepts `clickDelayMs=250` but emits only
  `MOVE_MOUSE -> WAIT(120) -> CLICK_LEFT`; DHXY maps `CLICK_LEFT` itself with delay `0`. Preserve the delay in the
  same ordered JSON command as a fourth `WAIT(250)` step. This is baseline action timing, not a retry or a second
  command. Update the named test to assert the exact four-step order and values.
- Repair #1 exact write set: only
  `CloudLeftTopStatusPortAssembly.java`, `LeftTopStatusTurnContractTest.java`, and this report. Do not change the
  protocol, DHXY, Service/caller/Task/host/routes/POM, UUID count, command count, or retry policy. Keep one fresh
  UUID per command and zero automatic retry/session/ledger/TTL.
- Java writers remain active, so parent did not run Maven/JUnit/compile in this review round.

<!-- TRUE_EOF: TURN-19-parent-repair-1-required -->

## SOURCE+TEST REPAIR DELIVERED - 2026-07-16 00:43 EDT

- Repair owner remains replacement implementation Worker
  `019f6913-dc31-7e53-90a9-958f96285317`. This is a repair delivery, not a reviewer conclusion;
  parent remains the sole final reviewer.
- Repair #1 exact write set was respected. No protocol, DHXY, Service, caller, Task, host/routes,
  POM, template, other source/test/report, or unrelated dirty/untracked content was modified.

### P1 closure evidence

- **Exact-window pre-port guard closed:** both production `observe(...)` and `click(...)` now read
  `exactContext.getTurnInvocationContext()`, immediately create
  `turnGameClient.bind(binding)`, and use only that bound view for
  `latestWindowMetadata()`, `capture(...)`, and `execute(...)`. The shared unbound client performs no
  port-facing operation. A wrong holder/current context is therefore rejected by
  `TurnGameClient.currentExactContext()` with `IllegalStateException` before metadata or command-port
  invocation.
- **Baseline click delay closed:** the one allowed input command now contains exactly four ordered
  steps with one UUID: `MOVE_MOUSE(x,y) -> WAIT(120ms) -> CLICK_LEFT(x,y) -> WAIT(250ms)`.
  `settleMs` is required to equal `120` and `clickDelayMs` is required to equal `250`; the final wait
  remains inside the same JSON action and adds no retry or second command.
- Existing action/window/frame correlation, same-frame raw PNG matching, command counts, known-failure
  mapping, STOPPED/uncertain handling, and pending-state behavior were left unchanged. One fresh UUID
  remains generated per actual command; there is still zero automatic retry, second capture,
  fallback, session, ledger, or TTL. **无已批准业务差异；按 `696a12b0` 基线等价迁移。**

### Repaired files

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java`
  - `423` lines; SHA-256 `9B767117E2903E32DB448773D823D7A0F527802D6BE0A29716B5FE4DE81DF7E1`.
- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`
  - `1063` lines; SHA-256 `C9D0B21AEC3637452E0507F0F716C43E9F8CD21010368EBA249012BE3C66EF8A`.
  - New production-path test
    `wrongCurrentContextFailsBeforeMetadataOrCommandPortForObserveAndClick` passes an exact operation
    context while binding a different current context, exercises both production Assembly operations,
    expects the bound-client `IllegalStateException`, and asserts `metadataReads=0`, `executeCalls=0`,
    zero submitted actions, and zero results after each attempt.
  - The common input assertion now requires four steps and exact `WAIT(120)` / `WAIT(250)` values, so
    every existing OPEN/known-failure/uncertain input path checks the repaired payload. The named class
    now contains nine tests. It remains present on disk under the existing `.gitignore:15:src/test/`
    rule; no force-add or Git mutation was performed.
- This report was appended only after the parent Repair #1 marker at the then-current true EOF.

### Worker gate

- Per the frozen instruction, this Worker ran no Maven, JUnit, compile, package, runtime, application,
  server, Task, UI, capture, or input command. No test/build result or approval is claimed.
- No Git mutation was performed. Parent must independently re-review these repaired sources and run
  the authorized named test plus applicable Cloud compile/build after concurrent writers stabilize.

<!-- TRUE_EOF: TURN-19-repair-1-source-test-delivered -->

## PARENT REPAIR #1 SOURCE + TEST SOURCE REVIEW - 2026-07-16T00:51:57-04:00

- 父级独立复读 repair 后的完整 assembly、named test 与 `TurnGameClient` bound-context guard；结论
  `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`，不是 Worker 自批。
- `CloudLeftTopStatusPortAssembly.java:76-101,192-225` 在任何 metadata/capture/execute 前用 exact invocation
  context 建立 bound view，observe/click 均不再让 shared unbound client 触达 port。
- `LeftTopStatusTurnContractTest.java:380-421` 让 holder current context 与传入 exact context 冲突，observe/click
  均在 port 前抛出并断言 `metadataReads=0/executeCalls=0/actions=0/results=0`。
- `CloudLeftTopStatusPortAssembly.java:207-225` 的单一 command 精确为
  `MOVE_MOUSE -> WAIT(120) -> CLICK_LEFT -> WAIT(250)`；named test `:814-843` 同时锁住四步 index、坐标和两个
  delay。一次 public action 仍一 UUID/command，零 retry/第二命令。
- 原两个 P1 已关闭，没有新 P0/P1/P2；owner 释放。named test 与适用 Cloud compile/build 留全部 Java writers
  稳定后的父级 cohort，本结论不冒充 `CARD APPROVED/CLOSED`。

**无已批准业务差异；按 `696a12b0` exact-window 与点击时序等价迁移。**

<!-- TRUE_EOF: TURN-19-parent-repair-1-review-passed -->
