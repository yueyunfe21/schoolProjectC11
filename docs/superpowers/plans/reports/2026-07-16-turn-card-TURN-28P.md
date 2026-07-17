# TURN-28P - Generic exact-window probe and queue-owned click timing mechanics

## READY / PARENT FROZEN IMPLEMENTATION BRIEF - 2026-07-16 03:08 EDT

- 状态：`READY / PARENT BRIEF FROZEN`；类型：共享 mechanics prerequisite；`countDelta=0`。父级是唯一
  manager/final reviewer，Worker 不是 reviewer。
- startDependsOn：TURN-09R、TURN-11、TURN-23P 的 parent source gates 已通过；approvalDependsOn：本卡 parent
  source/test-source review、两名独立 reviewer、点名 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 与双仓适用 compile。
- 本卡同时解除两个真实阻断：TURN-22 缺少同一 queue ownership 内的 `150ms click delay + 500ms hold`；
  TURN-28 缺少 exact-HWND `Ctrl before/after capture + finally release`。两者共享 protocol/validator/executor 写集，
  必须由唯一 owner 一次增量完成，不能拆成并发双写。
- 父级已独立对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`：
  `TeamReturnService` 使用一次 `InputAction.clickLeft(...,150)+sleep(500)` queue request；`NpcClickService` 的 Ctrl
  probe 顺序是 before capture、Ctrl DOWN、80ms、MOVE、280ms、after capture、fixed RGB tolerance 15 + ratio
  `0.05`、finally Ctrl UP、100ms。无已批准业务差异。

### Exact production write set

#### DHXY

1. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java`。
2. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`。
3. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`。
4. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`。
5. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java`。
6. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`。
7. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`。
8. Modify `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`。

#### Cloud

1. Modify
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java`。
2. Modify
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`。
3. Modify
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`。
4. Modify
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`。

双仓三个 protocol 文件各自必须 byte-identical。其余 production 全部只读，尤其 TURN-22 的 TeamReturn、
TURN-28 的 NpcClick/recognizer、`InputActionQueue`、`InputActionWorker`、`InputSequences`、`ImageFinder`、任何 Service/
Task/caller/host/application/POM/config/resource 不得修改。不得新增 production Java 文件、facade、wrapper chain、
第五个本地 Service、OCR、业务判断、自动 retry、owner/session/ledger/TTL/compaction/durable workflow。

### Exact test and fixture write set

#### 双仓 byte-identical

- Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`。
- Modify `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`。
- Create `src/test/resources/cloud-turn/v1/action-input-click-timing.json`。
- Create `src/test/resources/cloud-turn/v1/action-capture-pixel-change.json`。
- Create `src/test/resources/cloud-turn/v1/outcome-capture-pixel-change.json`。

#### DHXY

- Modify `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`。
- Create `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`。
- Modify `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`。
- Modify `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java` only where the new mutually
  exclusive field requires fixture/constructor updates; existing assertions must remain.
- Modify `src/test/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutorContractTest.java` only for the executor result
  shape and legacy behavior assertions; existing cases must remain.
- Modify `src/test/java/com/bot/dhxy/window/runtime/WindowIdentityDriftP2WiringTest.java` only to replace stale
  Alt+A/Alt+C unsupported assertions with the frozen exact-HWND contract.

#### Cloud

- Create
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java`。
- Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java` only for real
  multipart/correlation coverage of the new probe result.

Append only this fixed report true EOF。其它测试、fixtures 与报告只读。Worker 不运行 Maven/JUnit/compile；父级在
Java writers 稳定后执行点名 tests 与适用双仓 compile。

### Frozen protocol contract A - queue-owned click timing

1. 双仓 `TurnInputSpec` 只增加两个 nullable JSON 字段：`clickDelayMs` 与 `queueHoldMs`；保留现有七参数
   compatibility constructor，legacy JSON 缺字段时两者均为 null，不改变旧 action bytes/语义。
2. 只有 `CLICK_LEFT`/`CLICK_RIGHT` 可以携带任一 timing 字段；每项独立闭区间 `[0,5000]`。MOVE、double click、
   drag、scroll、keyboard、text 携带任一字段必须 validator fail closed。null 等价 legacy `0`，不得自动套默认值。
3. DHXY mapper 将 `clickDelayMs` 直接传给现有 `InputAction.clickLeft/right(..., delayMs)`；`queueHoldMs>0` 时，
   在同一 mapped list 尾部追加一个 `InputAction.sleep(queueHoldMs)`。`TurnInputStepExecutor` 必须把整份 list 恰好
   一次提交给现有全局 input queue。
4. 本能力不修改 `LocalTurnActionExecutor::findMouseSequenceEndExclusive`，也不把 timing 恢复成外围 WAIT step。
   TURN-22 后续返修将改为一个 CLICK INPUT step：`clickDelayMs=150, queueHoldMs=500`；本卡不得抢改 TeamReturn。
5. queue false/interruption 仍投影现有 FAILED/STOPPED；不得因 timing 增加第二 command、第二 UUID、自动重发、
   no-op MOVE/click 或前台 keyboard fallback。

### Frozen protocol contract B - exact-HWND pixel-change probe

1. 双仓 `TurnCaptureSpec` 增加 nullable `pixelChangeProbe`，并保留现有二/三参数 compatibility constructors。
   V1 probe 是一个显式 `CAPTURE` step 内的通用机械观察，不是 `MATCH_TEMPLATE`、OCR 或 NPC 业务。
2. `pixelChangeProbe` 仅包含 exact unscaled screen-absolute `targetX/targetY`、`ctrlDownSettleMs`、
   `afterMoveSettleMs`、`ctrlUpSettleMs` 与 `differenceRatioThreshold`。三个 timing 均为 `[0,5000]`；ratio 必须 finite
   且为 `[0.0,1.0]`。capture region 必须 non-null、正尺寸、`UPLOAD_IMAGE`，target 必须位于该 ROI 和 action
   window 内。
3. probe 与 `clearPointerIfOverRegion` 在 V1 互斥。含 probe 的 action 必须只有一个 step，且该 step 为 index `0`
   的 CAPTURE；before 只留内存，after 是该 action 唯一 requested raw PNG。禁止 Base64、第二 multipart frame、缩放、
   full-window probe 或额外 WAIT/INPUT step。
4. DHXY 在现有全局 input queue 的一次 `submitExclusiveAndWait` callback 中按唯一顺序执行：同一冻结 HWND/ROI
   before background capture -> Ctrl DOWN exact HWND -> `ctrlDownSettleMs` -> foreground mouse MOVE exact target ->
   `afterMoveSettleMs` -> 同一 HWND/ROI after background capture ->
   `ImageFinder.isMatch(before, after, differenceRatioThreshold)` -> finally Ctrl UP exact HWND ->
   `ctrlUpSettleMs`。callback 内禁止嵌套 queue；probe 期间禁止 refresh/locate/title-search。
5. `ImageFinder` 现有 RGB channel tolerance `15` 保持只读。match=true 投影 completed code
   `PIXELS_UNCHANGED`；match=false 投影 `PIXELS_CHANGED`。两种 completed 结果都携带 after PNG，Cloud 再做 OCR/
   template/FIFO/下一动作决定，本地不点击、不识别 NPC、不循环 probe。
6. Ctrl DOWN 只要尝试过，所有退出路径都必须 finally 尝试一次 Ctrl UP；down 不确定/失败也要 release。release
   未确认成功时不得返回 changed/unchanged，必须 typed `CTRL_RELEASE_FAILED`。stop/interruption 在 release 后投影
   STOPPED；其它 mechanics failure 投影 typed `PIXEL_PROBE_FAILED`，不得伪 unchanged。
7. `TurnCaptureStepExecutor.execute` 可直接改为一个小型 immutable nested `Execution`，携带现有 step status、closed
   code、单 frame 与 detail；`LocalTurnActionExecutor` 直接投影该结果，不新增同义 wrapper/DTO 文件。legacy capture
   completed code 仍为 `OK`，现有 public `capture(...)` 保持。
8. `BoundWindowKeyboardService` 只开放现有 Alt+A/Alt+C background flag，并增加接受 immutable
   `WindowNativeBinding + windowId` 的 exact shortcut overload，以及 exact Ctrl DOWN/UP typed transition。旧 public
   shortcut API保持；turn path 不得二次 refresh，也不得回退 `InputProvider.holdCtrl/releaseCtrl`。

### Terminal, frame and Cloud correlation

- completed probe code 只能是 `PIXELS_CHANGED`/`PIXELS_UNCHANGED`，且必须有一份 purpose=`CAPTURE`、
  sourceStepIndex=`0`、region/width/height/contentType/SHA/PNG bytes 与 action requested ROI 精确一致的 raw frame。
- plain CAPTURE 不能伪造 probe code；probe completed 缺 frame、错 code、错 action/device/window/step/ROI/dimension/SHA/
  bytes 都是 fatal contract error，不能降级为 ordinary miss/unchanged。
- probe FAILED/STOPPED 不得携带 completed probe code。`fullWindowFailureEvidence=true` 时，现有 failure evidence 只能
  在 Ctrl UP 尝试之后获取，并继续占唯一 frame slot；它的 purpose/sourceStep 仍按现有 failure contract，不算 second
  requested capture。
- command busy/duplicate/timeout/transport uncertainty 保持 `DUPLICATE_OR_UNCERTAIN`，同 actionId 零重执行。

### Named-test acceptance

1. 双仓 golden/validator 证明 legacy JSON 不漂移、新 input/probe fixtures byte parity、null/unknown/wrong action/
   timing/ratio/region/target/mutual-exclusion/multi-step 全 fail closed。
2. `TurnInputStepExecutorContractTest` 穿透 production mapper，证明 `CLICK_LEFT(150)+SLEEP(500)` 为一次 queue
   submission、顺序和 delay 精确；right click 同合同；旧 click null timing 不多 sleep；其它 action 拒绝 timing。
3. `TurnCapturePixelChangeProbeContractTest` 用 fake capture/keyboard/input trace 覆盖 changed、unchanged、before/down/
   move/wait/after/compare/up 每个 failure、stop/interruption、down uncertain 与 release failure；DOWN 尝试后 UP 恰好
   一次、唯一 queue callback、唯一 after frame、零 nested queue、同 HWND/process/ROI、坐标不缩放。
4. `LocalTurnActionExecutorContractTest` 证明 changed/unchanged code + after frame 进入 outcome；mechanics failure/stop
   不伪 completed；failure evidence 只在 release 后替换唯一 frame。pointer-clear 与 legacy capture 回归全保留。
5. Cloud invocation test 与真实 `TurnGameClientContractTest` 证明 valid changed/unchanged raw multipart 通过；缺 frame、
   plain/probe code 混用、错 sourceStep/ROI/dimension/SHA/PNG 均 fatal；一 command 一 UUID、无 Base64/第二 frame/retry。
6. 所有测试只使用 fake mechanics 与内存 PNG；不得启动 runtime/application/server/Task/UI、真实 capture/input。

### Worker rules

- 领取前先在本报告 true EOF 追加 `CLAIMED`、agent id、nickname、完整 exact write set；没有 true EOF claim 不得写。
- 适配当前 dirty/untracked 与 TURN-23P 已落盘内容；不得 checkout/reset/覆盖/清理/提交/暂存/Git mutation。
- 不写 `APPROVED/CLOSED`，不冒充 reviewer。交付只追加 `SOURCE+TEST DELIVERED`、逐文件 SHA/行证据、未运行门
  和新的 true EOF；父级独立审查后再安排两名独立 reviewer。

**无已批准业务差异；按 `696a12b0`、最小 HTTPS JSON turn、单 queue ownership 与 Cloud-owned OCR/业务等价迁移。**

<!-- TRUE_EOF: TURN-28P parent-frozen-brief -->

## REPLACEMENT CLAIMED - 2026-07-16T03:29:43.596-04:00

- agent id：`019f69ce-9359-71a1-8402-cb7ee7d34404`
- nickname：`Noether`
- 角色：TURN-28P replacement implementation Worker；不是 reviewer；父级仍是唯一 manager/final reviewer。
- replacement 原因：原 Raman 会话 `019f69c4-3ef0-7ff3-a5db-ebfc7c541130` 已 `not_found`，且未在本卡留下
  true EOF `CLAIMED`。本 Worker 从当前落盘字节接续，不回滚、不重做、不假设半成品不存在。
- claim 基线：DHXY=`thin-client-design` / HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；Cloud=
  `navigation-migration` / HEAD `3b988caa010254973e03342272e6d1d6a9685b01`。两仓均已有大量 dirty/untracked；这些内容全部受保护。

### Exact write set（唯一允许修改/创建）

DHXY production：

1. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java`
2. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
3. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
4. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
5. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java`
6. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
7. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
8. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`

Cloud production：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`

DHXY protocol tests/fixtures：

1. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
2. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
3. `D:/mavenProject/DHXY/src/test/resources/cloud-turn/v1/action-input-click-timing.json`
4. `D:/mavenProject/DHXY/src/test/resources/cloud-turn/v1/action-capture-pixel-change.json`
5. `D:/mavenProject/DHXY/src/test/resources/cloud-turn/v1/outcome-capture-pixel-change.json`

Cloud protocol tests/fixtures：

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/test/resources/cloud-turn/v1/action-input-click-timing.json`
4. `D:/mavenProject/dhxy-cloud-brain/src/test/resources/cloud-turn/v1/action-capture-pixel-change.json`
5. `D:/mavenProject/dhxy-cloud-brain/src/test/resources/cloud-turn/v1/outcome-capture-pixel-change.json`

DHXY mechanics tests：

1. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
2. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`
3. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
4. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java`
5. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutorContractTest.java`
6. `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/window/runtime/WindowIdentityDriftP2WiringTest.java`

Cloud invocation tests：

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`

固定报告（append-only）：

1. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md`

### 半成品保护与禁令

- 保护两仓全部既有 dirty/untracked、TURN-23P pointer-clear、原 Raman 可能留下的任何落盘半成品及并行
  TURN-33 replacement/helper 内容；所有目标文件必须从领取时当前字节增量编辑，禁止 checkout/reset/restore、
  覆盖式重建、清理、删除、回滚或改写他人内容。
- 双仓三个 protocol production 文件、两个 protocol tests 和三个 fixtures 必须逐对 byte-identical；保留 compatibility
  constructors、legacy JSON/旧断言和 pointer-clear 断言。
- `TeamReturnService`、`NpcClickService`、recognizer、`InputActionQueue`、`InputActionWorker`、`InputSequences`、
  `ImageFinder`、其它 Service/Task/caller/host/application/POM/config/resource/report 全部只读。
- 禁止扩写集、新 production Java、wrapper chain、OCR/NPC/业务判断、auto retry、第二 command/UUID/frame、
  owner/session/ledger/TTL/compaction/durable workflow；本地只实现冻结的共享 mechanics。
- 禁止任何 Git mutation：不提交、不暂存、不切分支、不 merge/rebase/cherry-pick、不 checkout/reset/restore/clean。
- 本 Worker 不运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；所有门保留给父级。
- 交付只追加 `SOURCE+TEST DELIVERED`、SHA/行证据/基线/未运行门和新 true EOF；不得写 `APPROVED/CLOSED`，
  不冒充 reviewer。

<!-- TRUE_EOF: TURN-28P REPLACEMENT CLAIMED Noether 019f69ce-9359-71a1-8402-cb7ee7d34404 2026-07-16T03:29:43.596-04:00 -->

## CLAIM IDENTITY CORRECTION - 2026-07-16T03:31:19.576-04:00

- 平台本次 spawn 返回的权威身份为 agent id `019f69ce-9359-71a1-8402-cb7ee7d34404`、nickname `Locke`。
- 前一段 replacement claim 中 nickname `Noether` 不作为权威；其 agent id、replacement ownership、完整 exact
  write set、半成品保护与全部禁令继续有效，且均由上述平台身份 `Locke` 承接。
- 本段仅追加身份校正，不改写已有 claim，不改变 Worker 非 reviewer、父级唯一 manager/final reviewer 的角色边界。

<!-- TRUE_EOF: TURN-28P CLAIM IDENTITY CORRECTION Locke 019f69ce-9359-71a1-8402-cb7ee7d34404 2026-07-16T03:31:19.576-04:00 -->

## REPLACEMENT CLAIMED - 2026-07-16T04:02:43.783-04:00

- 平台权威身份：agent id `019f69f0-014a-7543-bfbf-b18c8864e411`，nickname `Maxwell`。
- 角色：TURN-28P replacement implementation Worker；不是 reviewer；父级仍是唯一 manager/final reviewer。
- replacement 原因：父级已实时确认前一 Locke 会话
  `019f69ce-9359-71a1-8402-cb7ee7d34404` 为 `not_found`。Maxwell 保护 Raman/Locke 与所有并行 Worker
  已落盘字节，只从当前文件状态增量接续，不回滚、不覆盖式重建、不清理、不删除。
- 沿用本报告已冻结并在前一 replacement claim 完整枚举的 exact write set：DHXY 八个 production、Cloud 四个
  production、双仓各两个 protocol tests 与三个 fixtures、DHXY 六个 mechanics tests、Cloud 两个 invocation tests，
  以及本 append-only 报告；未列文件全部只读，不扩大范围。
- 沿用全部冻结合同：双仓 `TurnInputSpec`/`TurnCaptureSpec`/`TurnProtocolValidator` byte-identical；nullable
  `clickDelayMs/queueHoldMs` 只用于 CLICK_LEFT/RIGHT 且整份 mapped list 恰好一次 queue submission；单 CAPTURE
  exact-HWND `pixelChangeProbe` 严格执行 before/Ctrl DOWN/MOVE/after/finally UP，仅返回
  `PIXELS_CHANGED/PIXELS_UNCHANGED + after raw PNG`。
- 沿用全部禁令：不改 TeamReturn/NpcClick/recognizer/input queue/ImageFinder/其它 Service、Task、caller、host、
  application、POM、config 或 resource；不新增 OCR/业务、自动 retry、第二 command/UUID/frame、owner/session/ledger/
  TTL/compaction/durable workflow；不执行 Git mutation，也不运行 Maven/JUnit/compile/runtime/application/server/
  Task/UI/capture/input。
- 交付只能在本报告追加 `SOURCE+TEST DELIVERED`、逐文件证据/SHA、未运行门与新的 true EOF；不得写
  `APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-28P REPLACEMENT CLAIMED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T04:02:43.783-04:00 -->

## REPLACEMENT CLAIM CONFIRMATION - 2026-07-16T04:03:36.992-04:00

- `REPLACEMENT CLAIMED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411`。
- 本确认仅把 Maxwell 的真实 replacement ownership 再次置于报告 true EOF；上段完整 exact write set、半成品保护、
  Worker 非 reviewer 边界和全部禁令原样有效。

<!-- TRUE_EOF: REPLACEMENT CLAIMED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T04:03:36.992-04:00 -->

## SOURCE+TEST DELIVERED - 2026-07-16T04:31:59.517-04:00

- 实施 Worker：Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`；不是 reviewer。本节仅表示 production 与
  named-test 源码交付，不表示父级批准、卡片关闭、测试通过或构建通过。
- Maxwell 从 Raman/Locke 已落盘半成品增量接续；未回滚、覆盖式重建、清理、删除、提交、暂存或修改 exact write
  set 外文件。两仓既有 dirty/untracked 与并行 Worker 内容原样保护。
- 已复核 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的一次
  `clickLeft(..., 150) + sleep(500)` queue ownership，以及 Ctrl probe 的 before、Ctrl DOWN、80ms、MOVE、280ms、
  after、RGB tolerance 15/ratio `0.05`、finally Ctrl UP、100ms 顺序。无已批准业务差异；按基线等价迁移。

### Production 证据

1. 双仓 `TurnInputSpec.java:9-27` 仅增加 nullable `clickDelayMs/queueHoldMs` 并保留七参数 compatibility constructor；
   `TurnCaptureSpec.java:8-65` 增加 nullable `pixelChangeProbe` 并保留二/三参数 constructors。双仓三个 protocol
   production 文件逐对 SHA 相同。
2. 双仓 `TurnProtocolValidator.java:76-88,177-219,242-263` 对 click timing、single CAPTURE、ROI/window target、
   ratio/timing、result mode 和 pointer-clear 互斥 fail closed。legacy/null 路径保持。
3. DHXY `TurnInputActionMapper.java:30-58` 将 click delay 直接放入 CLICK_LEFT/RIGHT，并只在同一 mapped list 追加
   queue hold；`TurnInputStepExecutor.java:163-172` 对整份 list 恰好一次 `submitAndWait`，没有第二 command 或重发。
4. DHXY `BoundWindowKeyboardService.java:106-168` 使用调用方冻结的 immutable binding 提供 exact shortcut 与 Ctrl
   DOWN/UP typed transition；`:304-306` 只开放卡片批准的 Alt+A/Alt+C flag，turn 路径不 refresh、不前台 fallback。
5. DHXY `TurnCaptureStepExecutor.java:164-377` 在一次 `submitExclusiveAndWait` callback 内执行同 HWND/ROI before、
   Ctrl DOWN、wait、unscaled MOVE、wait、after、pixel compare 和 finally Ctrl UP/wait；DOWN 尝试后的退出路径都释放，
   release 不确定投影 `CTRL_RELEASE_FAILED`，stop 在释放后投影 STOPPED，其它 mechanics failure 不伪 unchanged。
6. DHXY `LocalTurnActionExecutor.java:89-128,194-225` 直接投影 capture `Execution`，completed probe 只携带 after raw
   PNG；failure evidence 在 step execution 返回后才获取，因此位于 Ctrl UP 尝试之后并继续占唯一 frame slot。
7. Cloud `TurnInvocationResult.java:115-214` 对 probe/plain/terminal code、purpose、source step、ROI、尺寸、
   `image/png`、SHA、PNG signature 与解码尺寸做 fatal correlation，valid changed/unchanged frame defensive copy。

### Named-test 源码证据

- 双仓 `TurnActionGoldenJsonTest`、`TurnProtocolValidatorContractTest` 与三个 fixtures 逐对 byte-identical，覆盖 legacy
  JSON、新字段、wrong action、范围、target、互斥和 multi-step fail-closed。
- DHXY `TurnInputStepExecutorContractTest.java:88-132` 穿透 production mapper/executor，覆盖 left/right click timing、
  单 queue submission、legacy null timing 与拒绝非 click timing。
- 新 `TurnCapturePixelChangeProbeContractTest.java:45-238` 全部使用 fake/in-memory mechanics，覆盖 changed、unchanged、
  before/down/wait/move/after/compare/up failure、stop/interruption、down uncertain、release failure、唯一 callback、
  唯一 after frame、exact binding/ROI 与不缩放坐标。
- `LocalTurnActionExecutorContractTest.java:334-422` 覆盖 probe code/frame 投影、failure/stop 不伪 completed，以及
  failure evidence 严格晚于 `key:UP`；既有 pointer-clear 与 legacy capture cases 保留。
- `TurnCapturePointerClearContractTest` 和 `TurnCaptureStepExecutorContractTest` 仅适配互斥字段/`Execution` result shape，
  既有断言保留。`WindowIdentityDriftP2WiringTest.java:70-90` 仅把 Alt+A/Alt+C 改为 exact-HWND contract 并增加 exact
  shortcut/Ctrl source evidence。
- Cloud 新 `TurnCapturePixelChangeInvocationContractTest.java:36-158` 覆盖 valid raw PNG 与所有 fatal mismatch；
  `TurnGameClientContractTest.java:94-133` 通过真实 client path 证明一次 UUID、一次 command、一个 CAPTURE step 和唯一
  raw after frame。

### 逐文件 SHA-256

| Repo | 文件 | 行数 | SHA-256 |
|---|---|---:|---|
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java` | 28 | `3d3dd1c516fc7777a8513fdb04fbfbea1c6a3aef14d001ae76aa0c84626f25cc` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java` | 66 | `216c8f51b7b08702365e7c9ca8f2e2f43e4f9f12aa6e63febbad495fd545472c` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java` | 483 | `3929cfb6afad86ffeccf1573e108a59878541d0d06d439a4ce95f6046252b2b0` |
| DHXY | `src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java` | 354 | `37d97cfb569bcca49d0b955d0ec462bf811ef7c49fda156f6457b5642f1330fe` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java` | 149 | `b5c6f173ba9a5c40774e24446e6726108701ab47a89a0c80434f15415319303a` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java` | 229 | `0ee95cbd48d3ec76fb9e50385108f9898f2979a33966487b39065352af1f43fd` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java` | 540 | `0c8a7dd3d46e1ce03f338fa57a43cf7897de79f5fda0f19d4d753f2677fa4cbb` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java` | 277 | `9e92cde9a9f68455a178d6d71bb771a7480ad088aedb7cb77321561afa3428f0` |
| Cloud | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java` | 28 | `3d3dd1c516fc7777a8513fdb04fbfbea1c6a3aef14d001ae76aa0c84626f25cc` |
| Cloud | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java` | 66 | `216c8f51b7b08702365e7c9ca8f2e2f43e4f9f12aa6e63febbad495fd545472c` |
| Cloud | `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java` | 483 | `3929cfb6afad86ffeccf1573e108a59878541d0d06d439a4ce95f6046252b2b0` |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java` | 235 | `052d9c80a2bfe575514886d1d4eef30af6b474f70a713e132fb6d9ef910024a7` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java` | 154 | `0fd97d5ec571ab2128241b9437108c50bad6131b579dc83092f4f04b52e488a2` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | 497 | `7a60f6577735b084769935af3cbebb78a4f7e058c51787ac47e5059954602beb` |
| DHXY | `src/test/resources/cloud-turn/v1/action-input-click-timing.json` | 29 | `8ff49960de2f215d1278f9b120f65b993fea23543e6b84168db2afd15a3d28f4` |
| DHXY | `src/test/resources/cloud-turn/v1/action-capture-pixel-change.json` | 35 | `59ac719c1f773675b15c05c6d493f85b5de5d0f9697318bf8ac0ab05904c92f7` |
| DHXY | `src/test/resources/cloud-turn/v1/outcome-capture-pixel-change.json` | 47 | `2fa30c6643b69861438c35c92088c1c082f1e9d21ea893dcf13e78fa23d42c25` |
| Cloud | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java` | 154 | `0fd97d5ec571ab2128241b9437108c50bad6131b579dc83092f4f04b52e488a2` |
| Cloud | `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` | 497 | `7a60f6577735b084769935af3cbebb78a4f7e058c51787ac47e5059954602beb` |
| Cloud | `src/test/resources/cloud-turn/v1/action-input-click-timing.json` | 29 | `8ff49960de2f215d1278f9b120f65b993fea23543e6b84168db2afd15a3d28f4` |
| Cloud | `src/test/resources/cloud-turn/v1/action-capture-pixel-change.json` | 35 | `59ac719c1f773675b15c05c6d493f85b5de5d0f9697318bf8ac0ab05904c92f7` |
| Cloud | `src/test/resources/cloud-turn/v1/outcome-capture-pixel-change.json` | 47 | `2fa30c6643b69861438c35c92088c1c082f1e9d21ea893dcf13e78fa23d42c25` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java` | 394 | `bb1ccc432020a8acd61c82abe207e13fb7959d94e9f8f6f27db28b43dafb738d` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java` | 545 | `6384f7392e4c9ecbe02e9dbaa661f54b7211e706daf22fa265825f1955e18def` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` | 1057 | `6190d0429ba071eed396b776004a91e009e26038dddb425fe275853e8538fc07` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java` | 410 | `3508e3e8ac767ab6e5e18a413fa9b9b7ae18ff76a362f812734b4ba8ff0a1d4f` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutorContractTest.java` | 232 | `d371342167a97c66f24de9d2eb7f190c4b645022629321cec924aa77d3763309` |
| DHXY | `src/test/java/com/bot/dhxy/window/runtime/WindowIdentityDriftP2WiringTest.java` | 161 | `2c4b446e850f7e010b46af80cf6bf2e3d0ba03b84a608ce948bec0125afba556` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java` | 265 | `f0f81e0855b98b6f418d8f21826dad0287f87e7b5e6e3b3de157bd93b4ad3f04` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java` | 531 | `c457099f0af6dfc1288e4f1d8bb90063ecc58bc6bc6fe9f55718458753096f66` |

### 静态门与诚实阻断

- 30 个目标文件均存在，逐文件 trailing-whitespace 计数为 `0`；两个 tracked DHXY 目标与 Cloud 目标的
  `git diff --check` 均通过，仅出现 Git 的 LF/CRLF 提示，不是 whitespace error。
- 双仓三个 protocol production、两个 protocol tests、三个 fixtures 的逐对 SHA 完全相同。目标 production 静态扫描
  未发现新增 OCR/业务判断、自动 retry、session/owner/ledger/TTL/compaction/durable workflow；没有第二 command、
  UUID 或 requested frame。
- 诚实记录一项写集内但卡片范围外的既有 source-guard 不一致：当前
  `WindowIdentityDriftP2WiringTest.java:72` 仍断言 `ALT_U(..., false)`，而当前及 HEAD
  `BoundWindowKeyboardService.java:306` 都是 `ALT_U(..., true)`。该不一致在 TURN-28P 前已存在；本卡只获准替换
  Alt+A/Alt+C stale assertions，因此 Maxwell 未越权修改 Alt+U。父级若点名运行完整该旧 source guard，可能先在此旧
  断言失败；这不是 TURN-28P 新增业务/代码返修项，需父级另行裁决。

### 未运行门

- 按 Worker 禁令，未运行 Maven、JUnit、compile/package，也未启动 runtime/application/server/Task/UI/capture/input，
  未执行真实 command 或任何 Git mutation。
- 父级仍需独立逐文件源码/测试源码审查，并在 Java writers 稳定后运行用户授权的
  `HTTPS_TURN_CONTRACT_TEST_FAMILY` named tests 与适用双仓 compile；两名独立 reviewer 与这些门通过前，本卡不能
  写 `APPROVED` 或 `CLOSED`。

<!-- TRUE_EOF: TURN-28P SOURCE+TEST DELIVERED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T04:31:59.517-04:00 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #1 - 2026-07-16T04:48:07.493-04:00

- 父级独立逐文件审查结论：`P0/P1/P2=0/0/0`，`SOURCE+TEST SOURCE REVIEW PASSED`。本结论不是
  `CARD APPROVED/CLOSED`；两名独立 reviewer、点名测试和适用双仓 compile/build 仍待完成。
- Protocol/input：双仓 `TurnInputSpec`、`TurnCaptureSpec`、`TurnProtocolValidator`，两份 protocol tests 与三个
  fixtures 的 SHA 逐对相同。`TurnProtocolValidator.java:171-220` 只允许 `CLICK_LEFT/CLICK_RIGHT` 携带 nullable
  timing；`TurnInputActionMapper.java:30-58` 把 click delay 与 queue hold 组成同一 mapped list；
  `TurnInputStepExecutor.java:166-177` 对该 list 恰好调用一次 `submitAndWait`。这保持 696a12b0 的
  `clickLeft(..., 150)` 后同 queue `sleep(500)`，不增加 command、UUID 或 transport retry。
- Exact-HWND probe：`TurnCaptureStepExecutor.java:164-381` 在同一次 `submitExclusiveAndWait` 内按 exact binding/ROI
  执行 before capture、后台 Ctrl DOWN、80ms、未缩放前台 MOVE、280ms、after capture、像素比较、finally Ctrl UP、
  100ms；`ctrlDownInvoked` 后每条退出路径均先尝试一次 UP，release uncertainty 优先投影
  `CTRL_RELEASE_FAILED`，stop/interruption/mechanics failure 均不携带 completed probe frame。
- Outcome/correlation：`LocalTurnActionExecutor.java:109-128,199-213` 直接投影 typed capture execution，失败证据只在
  probe 返回（即 finally release 已完成）后抓取；Cloud `TurnInvocationResult.java:123-170` 拒绝 plain capture 冒充
  probe、terminal probe 冒充 completed code，并对 completed probe 的 purpose/source step/ROI/尺寸/content type/SHA/
  可解码 raw PNG 做 fatal correlation。没有本地 OCR/NPC/业务判断或第二 frame。
- Named-test source 覆盖：DHXY probe test 覆盖 changed/unchanged、before/down/wait/move/after/compare/up failure、
  interruption、down uncertainty、release failure、单 exclusive callback、exact binding/ROI 与唯一 after PNG；input test
  穿透 production mapper/executor 断言一次 queue submission；Cloud invocation test 覆盖 completed/terminal/plain、
  metadata/SHA/PNG/尺寸错误；Local executor test 断言 failure evidence 晚于 `key:UP`。
- 写集内旧 source guard 的 `ALT_U(..., false)` 与当前/HEAD production `ALT_U(..., true)` 不一致在本卡前已存在，且
  TURN-28P 只批准替换 Alt+A/Alt+C stale assertions；不要求 Worker 越权改 Alt+U，也不计入本卡 P0/P1/P2。
- Java writer Leibniz 仍在 TURN-33 Repair #1，因此父级本轮未运行 Maven/JUnit/compile。Faraday
  `019f6a15-8906-7331-aeb4-3f03aaeff31c` 与 Anscombe `019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142` 正在做两份独立
  delivery review。该 source gate 通过后可立即把 External A 的 TURN-22 Repair #1 与 External B 的 TURN-28 标记
  `READY`；最终 `CARD APPROVED` 仍须等待 reviewer 与构建门。

<!-- TRUE_EOF: TURN-28P PARENT SOURCE+TEST SOURCE REVIEW PASSED P0P1P2=0/0/0 2026-07-16T04:48:07.493-04:00 -->

## PARENT DELIVERY REVIEW #2 - 2026-07-16T05:08:30-04:00

- 独立 reviewer R1 已交付 `P0/P1/P2=0/2/1 / REVIEW REQUIRED`。父级未以 reviewer 自述代替裁决，已重新读取
  `TurnExecutionWindow`、`TurnCaptureStepExecutor`、`InputSequences`、`InputActionQueue`、
  `InputActionRequest`、`InputActionWorker`、`InputActionScope`、`WindowAwareInputCoordinator` 与点名测试源码。
- 父级独立结论同样为 `P0/P1/P2=0/2/1`；前一轮
  `SOURCE+TEST SOURCE REVIEW PASSED` 被本轮新证据推翻。本卡立即回退为
  `REPAIR #1 REQUIRED / NOT SOURCE APPROVED / NOT CARD APPROVED`。R2 尚在独立审查，其后续新材料继续并入同一
  repair，不因本轮已确认阻断而空等。
- 本轮未运行 Maven/JUnit/compile/runtime/input；TURN-33 Repair #1 仍是活动 Java writer。

### P1-1 - probe 入队和 focus 均会再次 refresh，混用了新 focus 与旧 frozen snapshot

- `TurnExecutionWindow.java:41-87` 已为 action 做唯一一次 refresh，并冻结 context/binding/metadata。
  但 `TurnCaptureStepExecutor.java:204-208` 经 `InputSequences.submitExclusiveAndWait(...)` 进入 legacy
  `InputActionQueue.submitExclusiveAndWait(...)`；后者在 `InputActionQueue.java:303-315,554-593` 再次执行
  `refreshAndCommit(context)`。input worker 在 focus 前又经
  `WindowAwareInputCoordinator.java:136-170` 第三次 `refreshAndCommit(context)`。
- callback 的 capture、Ctrl DOWN/UP 与 ROI 始终使用旧 `window.binding()`/metadata，而 queue/focus 使用 refresh 后的
  mutable context。binding 在 resolve 与 worker 执行间漂移时，会出现“focus 新窗口、从旧 HWND capture/发 Ctrl、按旧
  absolute point MOVE”的混合证据和跨窗口物理输入风险。现有 fake `InputSequences` 同步执行 callback，未穿透该路径。

### P1-2 - waiter 中断会先发布 STOPPED，input worker 可能随后才释放 Ctrl

- legacy `InputActionQueue.await(...)` 在 `InputActionQueue.java:678-697` 对非 deadline request 先
  `request.cancel(...)` 完成 terminal future，再尝试 remove 并立即返回。若 worker 已取得 exclusive callback，remove
  失败不会停止 worker。
- probe callback 仅检查 frozen `metadata.stopRequested` 与 input-worker thread interrupt，未使用已存在的
  `InputActionScope.checkpoint()` 观察 waiter cancellation；因此 task thread 可先在
  `TurnCaptureStepExecutor.java:348-353` 投影 STOPPED，而 worker 仍在 MOVE/capture，finally 的唯一 Ctrl UP 与 100ms
  release settle 尚未完成。这违反“先 cleanup，后 closed result”。现有同步 test double 无法复现 task waiter/input worker
  双线程竞态。

### P2-1 - 缺 probe-specific uncertainty regression evidence

- Cloud 点名测试只对 plain capture/WAIT 覆盖 command/outcome uncertainty；没有让同一
  `pixelChangeProbe` action 走 `DUPLICATE_OR_UNCERTAIN`，也没有断言 uncertainty 携带
  `PIXELS_CHANGED/PIXELS_UNCHANGED` 或 after frame 时 fatal rejection。当前 production 未见直接伪成功，但本卡的
  一 UUID/command、零 retry、uncertain 不伪 completed 仍缺 action-specific evidence。

### Repair #1 frozen implementation contract

原 Worker Maxwell 继续负责；不得由 reviewer 实施。父级将本卡 write set 仅扩展为下列共享 mechanics 接缝，除此以外
全部只读：

Production：

1. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
2. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`
3. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
4. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/InputSequences.java`
5. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java`
6. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`

Tests：

1. Create `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`
2. Modify `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`
3. Modify `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
4. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java`
5. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`
6. 本 append-only 报告。

冻结行为要求：

1. 增加一个**通用、非业务命名**的 frozen exact-window exclusive queue 边界；它仍由现有单全局 input queue 持有，
   但接收 action 已冻结的 context/binding snapshot。enqueue 与 focus 都不得 refresh/locate/title-search；worker 在任何 callback/
   物理输入前必须核 exact windowId/HWND/process/rect/identity epoch，drift 时 fail closed 且 callback/capture/Ctrl/MOVE 全为零。
   focus、capture、keyboard 与 absolute target 必须使用同一 frozen binding，不得混用 mutable context 的新 binding。
2. 该边界必须使用实时 queue-request cancellation checkpoint。若 waiter interrupt 时请求尚未开始，remove 后零 mechanics；
   若 worker 已开始 callback，waiter 必须请求 cooperative cancellation 并形成 completion barrier，直到 callback 退出、finally
   恰好一次 Ctrl UP 已尝试且 `ctrlUpSettleMs` 完成后才返回/投影 STOPPED。finally cleanup 不得被 checkpoint 跳过。
3. probe 在 before capture、Ctrl DOWN 后、每次 settle 后、MOVE 前、after capture 前后均检查现有
   `InputActionScope.checkpoint()` 语义与 frozen binding；不新增本地 OCR/NPC/业务判断，不把 pause/stop 包成业务失败。
4. named tests 必须穿透真实 queue/worker（desktop mechanics 可 fake）：证明 action resolve 总 refresh 次数恰好 `1`；模拟
   resolve 后 drift 时零 callback/零物理输入；双线程在 Ctrl DOWN 后 interrupt waiter，断言 queue/outcome 返回严格晚于唯一
   Ctrl UP + release settle，且返回后无任何 mechanics；补 probe action 的 command/outcome uncertainty、同 actionId、一 UUID、
   一 command、零 retry、无 fabricated completed code/frame 与非法 completed payload fatal rejection。
5. 不新增 deadline/TTL、retry、owner/session/ledger/compaction/durable workflow，不改 protocol/JSON/Cloud 业务、
   `TeamReturnService`/`NpcClickService`/Task/caller，不新增 wrapper chain，不运行 Maven/runtime/input，不做 Git mutation。

<!-- TRUE_EOF: TURN-28P PARENT DELIVERY REVIEW-2 REPAIR-1 REQUIRED P0P1P2=0/2/1 2026-07-16T05:08:30-04:00 -->

## PARENT DELIVERY REVIEW #2 ADDENDUM - 2026-07-16T05:12:00-04:00

- Independent reviewer R2 已在独立报告 true EOF 交付 `P0/P1/P2=0/2/1`；其两项 P1 与 R1、父级独立源码结论一致，
  不新增重复计数。R2 另发现一项不同 P2，父级已独立复核成立，因此本轮父级总计数更正为
  **`P0/P1/P2=0/2/2`**，状态继续为 `REPAIR #1 REQUIRED`。
- 新增 **P2-2**：`TurnCaptureStepExecutor.java:285-325` 的 Ctrl UP cleanup 只捕获
  `RuntimeException`；但 `InputActionWorker.java:194-196` 会捕获任意 `Throwable` 并把 request 正常化为 failed。
  因此 Ctrl UP 若抛出会被 worker 吞下的非 `RuntimeException`，probe 未先置 `releaseFailed`，最终会在
  `TurnCaptureStepExecutor.java:355-363` 错投影为 `PIXEL_PROBE_FAILED`，而不是冻结合同要求的
  `CTRL_RELEASE_FAILED`。这会丢失“Ctrl 可能仍按下”的 typed 风险，虽未伪造 completed frame，故计 P2。
- Repair #1 的 production write set 已包含 `TurnCaptureStepExecutor.java`，无需再次扩写集。返修必须对齐 worker 与
  cleanup 的 throwable policy：任何会被 outer worker 正常化并继续运行的 Ctrl UP throwable，probe 都要先记录
  release uncertainty，使 closed code 为 `CTRL_RELEASE_FAILED`；若某类 fatal error 选择透传，则 worker 也不得将其
  正常化。named fake 增加相同 policy 的 non-Runtime UP failure，断言无 changed/unchanged/frame，typed code 精确为
  `CTRL_RELEASE_FAILED`。
- 原 P2-1 probe-specific command/outcome uncertainty evidence 仍须补齐；两项 P2 均不得用 source guard、mock result 或
  重试规避。无已批准业务差异。

<!-- TRUE_EOF: TURN-28P PARENT REVIEW-2 ADDENDUM REPAIR-1 REQUIRED P0P1P2=0/2/2 2026-07-16T05:12:00-04:00 -->

## REPAIR #1 CLAIMED - 2026-07-16T05:10:32.146-04:00

- Implementation Worker：Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`；不是 reviewer，父级仍是唯一
  manager/final reviewer。
- 已完整读取最新 `PARENT DELIVERY REVIEW #2 / REPAIR #1 REQUIRED`，接受其
  `P0/P1/P2=0/2/1` 与冻结 Repair #1 合同。
- 本轮 exact write set 仅限父级列出的 6 个 production、5 个 tests 与本 append-only 报告；从领取时当前字节增量
  修复，不回滚、覆盖式重建、清理、删除、暂存、提交或执行任何 Git mutation。
- 实施目标仅为：通用 frozen exact-window exclusive queue；enqueue/focus 零 refresh；drift 时零 callback/input；
  waiter 已开工时 cooperative cancellation + worker cleanup completion barrier；probe 使用现有
  `InputActionScope.checkpoint()` 且 finally Ctrl UP/settle 不被跳过；真实 queue/worker 双线程测试与 probe uncertainty。
- 沿用全部禁令：不新增 deadline/TTL、retry、owner/session/ledger/compaction/durable workflow、OCR/业务；不改
  protocol/JSON/Task/caller/TeamReturn/NpcClick；不运行 Maven/JUnit/compile/runtime/input；交付只写
  `REPAIR #1 SOURCE+TEST DELIVERED`，不得自批或关闭卡片。

<!-- TRUE_EOF: TURN-28P REPAIR-1 CLAIMED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:10:32.146-04:00 -->

## REPAIR #1 CLAIM ADDENDUM - 2026-07-16T05:12:00.156-04:00

- Maxwell 已完整读取父级 `PARENT DELIVERY REVIEW #2 ADDENDUM`；本 Repair #1 的权威总计数更正为
  `P0/P1/P2=0/2/2`。
- 原两项 P1 与 P2-1 继续有效；新增 P2-2 同轮纳入：Ctrl UP cleanup 的 throwable policy 必须与 outer worker 对齐，
  non-Runtime UP throwable 也必须先记录 release uncertainty，closed code 精确为 `CTRL_RELEASE_FAILED`，且不携带
  `PIXELS_CHANGED/PIXELS_UNCHANGED` 或 frame。
- Exact write set 与全部禁令不变；本段只校正领取范围，不构成 delivery、review 或 approval。

<!-- TRUE_EOF: TURN-28P REPAIR-1 CLAIMED ADDENDUM P0P1P2=0/2/2 Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:12:00.156-04:00 -->

## REPAIR #1 SOURCE+TEST DELIVERED - 2026-07-16T05:42:47.115-04:00

- Implementation Worker：Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`；本段仅为返修源码/测试源码交付，
  不是 reviewer，不写 `APPROVED/CLOSED`。父级 `P0/P1/P2=0/2/2` 四项均在冻结 write set 内增量处理。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。未增加 deadline/TTL、
  transport/business retry、owner/session/ledger/compaction/durable workflow、OCR/NPC/业务判断、第二 command/UUID/frame。

### P1-1：frozen exact-window queue/focus

1. `InputActionQueue.java:330-349` 新增通用 `submitFrozenExactWindowExclusiveAndWait`，显式接收 action resolver
   已冻结的 context/binding/identity epoch；该方法不读取 `WindowTaskContextHolder`、不调用 refresh/locate/title search，
   仍进入原单全局 queue。
2. `InputActionRequest.java:245-258,384-392,839-892` 保存 immutable binding snapshot，并逐次核对 exact
   windowId/HWND/process/screen-absolute rect/player identity epoch；`InputActionWorker.java:129-143` 在 callback 前只走
   frozen focus；`:153-164` 在 callback cleanup 返回后再做一次 frozen safety gate，才允许发布 completed。
3. `WindowAwareInputCoordinator.java:158-191` 在 context monitor 内原子核对并 focus 调用方冻结 binding，零 refresh；
   `InputSequences.java:62-83` 只暴露同一通用边界。capture、Ctrl、MOVE 继续使用同一个 `TurnExecutionWindow.binding()`。
4. 新 `InputActionFrozenExclusiveContractTest.java:44-190` 穿透真实 `TurnExecutionWindow.resolveForAction`、queue 与 daemon
   worker，断言 resolve 总 refresh=`1`、focus 使用同一 binding；resolve 后 geometry drift 时零 focus/callback/physical input，
   且 queue/focus 不增加 refresh。

### P1-2：cooperative cancellation 与 cleanup completion barrier

1. `InputActionQueue.java:700-758` 对 frozen request 先 `queue.remove`：未开工则立即 terminal/零 mechanics；worker 已取得
   ownership 则只请求 cooperative cancellation，并 `join` 同一 request terminal，等待 worker callback/finally 完整退出。
   legacy 非 frozen 分支保持原行为。
2. `InputActionRequest.java:556-570,635-654,703-736` 对已开始 frozen callback 的 cancel 只记录 cancellation，不提前
   publish future；exact drift/stop 也使用 worker-owned cooperative terminal。最终 completion 仍由 worker 在 callback cleanup
   返回后发布。
3. `TurnCaptureStepExecutor.java:206-405` 改走 frozen queue；before capture、Ctrl DOWN 前后、每次 settle 后、MOVE 前后、
   after capture 前后均调用 `InputActionScope.checkpoint()`。所有 checkpoint 都在 try body，finally 中唯一 Ctrl UP 与
   `ctrlUpSettleMs` 不会被 checkpoint 跳过。
4. `InputActionFrozenExclusiveContractTest.java:67-147` 用真实 queue/worker 双线程分别覆盖 queued remove 与 callback 已在
   Ctrl DOWN 后 waiter interrupt；后者明确断言 waiter 在 `CTRL_UP_SETTLED` 前仍存活，closed result 返回后 mechanics 数量
   不再变化。

### P2-1/P2-2：probe uncertainty 与 Ctrl release throwable

1. Cloud `TurnCapturePixelChangeInvocationContractTest.java:113-151` 增加 probe-specific
   `DUPLICATE_OR_UNCERTAIN` canonical no-frame 与非法 completed code+after-frame fatal rejection；
   `TurnGameClientContractTest.java:138-207` 穿透真实 client，断言同 actionId、一 UUID、一 command、零 retry、无 fabricated
   completed code/frame，非法 payload 同样 fatal 且不重发。
2. `TurnCaptureStepExecutor.java:300-336` 的 Ctrl UP cleanup 现与 outer worker 的 throwable policy 对齐：UP 抛出任意
   会被 worker 正常化的 `Throwable` 时，先记录 `releaseFailed`，再完成 settle，closed result 优先精确投影
   `CTRL_RELEASE_FAILED`，不编码 changed/unchanged after frame。
3. `TurnCapturePixelChangeProbeContractTest.java:157-196,405-452` 增加 `AssertionError` non-Runtime UP fake，断言
   `CTRL_RELEASE_FAILED`、唯一 UP attempt、无 completed frame；`LocalTurnActionExecutorContractTest.java:897-930` 保持
   whole-action probe fake 绑定新的 frozen API，既有 failure-evidence-after-UP 断言不变。

### 返修后逐文件 SHA-256

| Repo | 文件 | 行数 | SHA-256 |
|---|---|---:|---|
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | 778 | `dceca233def81afd3377f65cf4f5514efbc4044a2a81659ece3ee4ea46b1f407` |
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | 1060 | `b6702f65a56cf78f24df2e52fe2164e4c6be4d7aead57650e5ea5e2c35db9000` |
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | 597 | `8f52f456b1e74ab1e199f2ecfbf9ff2757908bda0aac8eb2eebfa46e5df3593b` |
| DHXY | `src/main/java/com/bot/dhxy/input/InputSequences.java` | 176 | `fa2f17bfb8b0ab672e986abcadc7c316b0eff1d3c9781424f7839a1b0f06fdd2` |
| DHXY | `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java` | 268 | `ce40d510055919d98278d6c416bfb6acaca3a74374071b283ddbb2bbe152eb36` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java` | 570 | `16e53f3727dbc171d9465b918c89762c806b888deca893687393988a260dd6a9` |
| DHXY | `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | 409 | `d39b9a7236a66b15f170d33f458237e18efe8005358a5573c0da62d37fc57e61` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java` | 579 | `80d4fe80292409396a52a77620637ba6ae57d0c1e73922a13bd0a68c7218986e` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` | 1074 | `6a90d47c4e52c635b603a906ef338068f6ee9e9a0a8a2dcf759fde3f14f91f93` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java` | 304 | `e7f055378c3fc23b904012663643c3deeb87a1655a61076817193e2dd6ecac70` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java` | 603 | `b03eebe30f719e869ddf6453552678bbfbf0bad7841c981adbbe85d9a597ed04` |

### 静态门与未运行门

- 11 个返修 production/test 文件 trailing whitespace 均为 `0`；tracked 目标 `git diff --check` 无 whitespace error。
  两仓其它 dirty/untracked 与并行 Worker 内容均原样保护，未回滚、覆盖、清理、删除、暂存、提交或执行其它 Git mutation。
- 按父级 Worker 禁令，本轮未运行 Maven、JUnit、compile/package，也未启动 runtime/application/server/Task/UI/
  capture/input；这些门必须由父级在 Java writers 稳定后执行。此 Worker 交付不能冒充测试通过、source approval 或 card close。

<!-- TRUE_EOF: TURN-28P REPAIR-1 SOURCE+TEST DELIVERED P0P1P2=0/2/2 Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:42:47.115-04:00 -->

## PARENT DELIVERY REVIEW #3 - 2026-07-16T05:48:39.927-04:00

- 父级已独立逐文件读取 Repair #1 的 6 个 DHXY production、3 个 DHXY named tests、2 个 Cloud named tests，
  并核对两份 PRECHECK、`696a12b0`、当前调用点与交付表 11 个 SHA-256；交付 SHA 全部与磁盘当前字节一致。
- 结论：**`P0/P1/P2=0/2/1 / REPAIR #2 REQUIRED`**。本轮已经关闭旧 P1“frozen path 再次
  refresh”与“waiter 在 callback finally/Ctrl-UP settle 前返回”，也已把 Ctrl-UP `Throwable` 先记录成 release
  uncertainty；但 exact-window 原子边界、typed STOP 投影和对应真实链测试仍未闭合，不能进入 reviewer/build 门。

### P1-1 - action snapshot 与 callback 仍不是同一原子 exact-window generation

- `TurnExecutionWindow.java:26-39,68-87` 只冻结 `context/binding/metadata`，不冻结 identity epoch；但新 public API
  `InputActionQueue.java:330-349` / `InputSequences.java:75-83` 仍要求 caller 另传 `long playerIdentityEpoch`，而
  `TurnCaptureStepExecutor.java:206-211` 在 action resolve 后再次读取 mutable context epoch。若 binding 在 resolve 后
  发生 A -> B -> A，旧 action binding 可与新 epoch 拼成从未原子存在过的 snapshot；`sameExactWindow` 又不含对象
  generation witness，可能错误放行。
- `InputActionWorker.java:112-165` 只在 focus 前分段检查。`WindowAwareInputCoordinator.java:171-190` 的
  `synchronized(context)` 在 focus 返回即释放，真正 callback 位于随后 `InputActionWorker.java:156-162`；因此另一个
  `WindowNativeBindingRefreshService.refreshAndCommit` 可在 focus 后、before capture/Ctrl/MOVE 前提交 drift。
  callback 内的 checkpoint 也只是离散检查，检查返回到下一次 mechanics 之间仍有同一 TOCTOU。结果可能在旧
  absolute ROI/target 与新 context generation 间混绑，违反本卡 exact-HWND 零漂移合同。

**Repair #2 条件：** frozen public boundary 不再接收 caller-supplied epoch；在 `synchronized(context)` 内要求
`context.getNativeBinding() == frozenBinding`（generation witness）并核 windowId/HWND/process/rect/非 suspended，随后
同锁读取 epoch 写入 request。worker 取得 global input transaction 后，在同一 context monitor 内完成唯一 authoritative
exact check、显式 frozen focus、`tryStartStep(0)`、callback 及其 Java `finally`，释放 monitor 前不得允许 binding commit。
coordinator 只 focus 调用方显式 binding，不再维护第二套 mutable-context comparator。不得增加 snapshot DTO、wrapper
chain、refresh、retry、deadline/TTL 或业务判断。

### P1-2 - boolean frozen facade 丢失 worker 已有 typed STOP

- `InputActionExecutionResult.java:19-29` 已携带 terminal status、`InputActionSafetyReason` 与 reason；但 frozen queue/
  facade 在 `InputActionQueue.java:330-349`、`InputSequences.java:75-83` 仍压成 boolean。
- stop token 在 worker admission 前关闭时，`InputActionRequest.java:839-859` 会形成 `STOP_REQUESTED`；然而
  `TurnCaptureStepExecutor.java:363-383` 只看到 `submitted=false`。action resolve 时的
  `window.metadata().stopRequested()` 仍可能为 false、caller 线程也未 interrupt，于是 STOP 被错误投影为
  `FAILED/PIXEL_PROBE_FAILED`，而不是冻结合同要求的 `STOPPED/STOPPED`。

**Repair #2 条件：** frozen queue/facade 直接返回既有 `InputActionExecutionResult`；capture 在 cleanup barrier 后按
`releaseFailed -> callback/caller stopped 或 safetyReason=STOP_REQUESTED -> queue non-completed -> mechanics failure ->
changed/unchanged` 顺序投影。不得新增同义 result DTO，也不得用 detail 字符串猜 STOP。

### P2-1 - named tests 没有穿透三条关键 production 接缝

- `InputActionFrozenExclusiveContractTest.java:44-188` 仅覆盖一个 geometry-x drift；没有逐项 HWND/process/x/y/width/
  height/epoch 与 A -> B -> A generation drift，也没有完整 public `LocalTurnActionExecutor.execute` 的 resolver ->
  queue -> worker -> focus/capture/keyboard/input 链。`LocalTurnActionExecutorContractTest.java:335-421,897-930` 仍用同步
  `ProbeInputSequences` 直接调用 callback。
- `TurnCapturePixelChangeProbeContractTest.java:183-193,370-407` 的 non-Runtime UP 用同步 fake 执行 callback，未穿透
  outer `InputActionWorker.catch(Throwable)`；started barrier 用 `Thread.sleep(25)` 猜返回后无 mechanics，没有冻结清单要求的
  原子 `postReturnMechanics` 证据。
- Cloud `TurnCapturePixelChangeInvocationContractTest.java:132-147` 与 `TurnGameClientContractTest.java:173-207` 只构造
  “completed code + frame”组合，code 先失败会遮住 frame-only uncertainty；未分别证明 code-only、valid frame-only 均
  fatal，也未在 probe caller 上分别锁住 timeout/interrupted uncertainty 的一 UUID/一 command/零 retry。

**Repair #2 条件：** 按两份 PRECHECK 的 public/real queue+worker harness 补齐上述三组证据；所有 capture/keyboard/
mouse/focus 都用内存 fake，禁止真实 input/capture/runtime。测试不得扫描源码或反射 private helper；返回后零 mechanics
用 latch/atomic 事件证明，不用 sleep 猜竞态。

### Repair #2 exact write set 与门禁

- 仅沿用本轮 11 个文件：`InputActionQueue`、`InputActionRequest`、`InputActionWorker`、`InputSequences`、
  `WindowAwareInputCoordinator`、`TurnCaptureStepExecutor`，DHXY 三个 named tests，Cloud 两个 named tests，以及本
  append-only 报告。其它 protocol/JSON/Service/Task/caller/POM/config/resource 全部只读。
- 保持已关闭项：零二次 refresh、remove-first/cooperative terminal barrier、DOWN invoked 后唯一 UP+settle、Ctrl-UP
  `Throwable -> CTRL_RELEASE_FAILED`、唯一 after raw PNG、一 command/UUID、零自动 retry。
- Maxwell 保持本卡 owner，收到本结论后只追加 `REPAIR #2 CLAIMED` 再增量返修；仍不得运行 Maven/JUnit/compile/
  runtime/application/server/Task/UI/capture/input 或 Git mutation。Java writer 活动期间父级不运行 Maven。

**无已批准业务差异；按 `696a12b0`、exact-window generation、最小 HTTPS JSON turn 与 Cloud-owned 业务等价迁移。**

<!-- TRUE_EOF: TURN-28P PARENT DELIVERY REVIEW-3 REPAIR-2 REQUIRED P0P1P2=0/2/1 2026-07-16T05:48:39.927-04:00 -->

## REPAIR #2 CLAIMED - 2026-07-16T05:50:50.008-04:00

- Implementation Worker：Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`；不是 reviewer，父级仍是唯一
  manager/final reviewer。
- 已完整读取最新 `PARENT DELIVERY REVIEW #3`，接受 `P0/P1/P2=0/2/1 / REPAIR #2 REQUIRED`，并沿用
  Repair #1 已关闭的 remove-first/cooperative cleanup barrier、唯一 Ctrl UP + settle、Ctrl-UP `Throwable` release
  uncertainty、唯一 after raw PNG、一 command/UUID 与零自动 retry。
- 本轮 exact write set 仅限父级冻结的 6 个 DHXY production、3 个 DHXY named tests、2 个 Cloud named tests与本
  append-only 报告；从当前字节增量返修，不回滚、覆盖式重建、清理、删除、暂存、提交或执行任何 Git mutation。
- Repair #2 目标仅为：在同一 `synchronized(context)` generation monitor 内冻结 binding object identity + exact fields
  与 epoch，并由 worker 持锁贯穿 authoritative check/focus/step/callback/finally；frozen queue/facade 保留既有 typed
  `InputActionExecutionResult` 并精确投影 `STOP_REQUESTED`；补齐 public resolver 到真实 queue/worker、逐项 drift、outer
  worker non-Runtime UP，以及 Cloud code-only/frame-only/timeout/interrupted uncertainty 的点名测试源码。
- 沿用全部禁令：不新增 refresh、deadline/TTL、retry、owner/session/ledger/compaction/durable workflow、OCR/业务，
  不改 protocol/JSON/Task/caller/POM/config/resource；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/
  capture/input。交付只追加 `REPAIR #2 SOURCE+TEST DELIVERED`，不得自批或关闭卡片。

<!-- TRUE_EOF: TURN-28P REPAIR-2 CLAIMED P0P1P2=0/2/1 Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:50:50.008-04:00 -->

## INTERNAL OWNER RELEASED / EXTERNAL-B HANDOFF - 2026-07-16T05:52:15.119-04:00

- 父级已把 TURN-28P Repair #2 关键阻塞卡改交 External B。Internal Maxwell
  `019f69f0-014a-7543-bfbf-b18c8864e411` 立即停止所有 Java/test 编辑并释放内部 owner；本段不代表
  `APPROVED/CLOSED`，External B 必须按父级最新 Review #3 与其后续指令另行领取。
- 自 `PARENT DELIVERY REVIEW #3` 后，Maxwell **未修改任何一个冻结的 11 个 production/test 目标文件**；仅在本
  append-only 原卡追加过 `REPAIR #2 CLAIMED`，收到重排后再追加本交接段。没有回滚、覆盖或清理任何已落盘半成品。
- 当前 11 文件 SHA-256 与 Repair #1 `SOURCE+TEST DELIVERED` 表逐项一致，External B 可从这些确切字节增量接续：

| Repo | 文件 | 行数 | 当前 SHA-256 |
|---|---|---:|---|
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | 778 | `dceca233def81afd3377f65cf4f5514efbc4044a2a81659ece3ee4ea46b1f407` |
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | 1060 | `b6702f65a56cf78f24df2e52fe2164e4c6be4d7aead57650e5ea5e2c35db9000` |
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | 597 | `8f52f456b1e74ab1e199f2ecfbf9ff2757908bda0aac8eb2eebfa46e5df3593b` |
| DHXY | `src/main/java/com/bot/dhxy/input/InputSequences.java` | 176 | `fa2f17bfb8b0ab672e986abcadc7c316b0eff1d3c9781424f7839a1b0f06fdd2` |
| DHXY | `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java` | 268 | `ce40d510055919d98278d6c416bfb6acaca3a74374071b283ddbb2bbe152eb36` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java` | 570 | `16e53f3727dbc171d9465b918c89762c806b888deca893687393988a260dd6a9` |
| DHXY | `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | 409 | `d39b9a7236a66b15f170d33f458237e18efe8005358a5573c0da62d37fc57e61` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java` | 579 | `80d4fe80292409396a52a77620637ba6ae57d0c1e73922a13bd0a68c7218986e` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` | 1074 | `6a90d47c4e52c635b603a906ef338068f6ee9e9a0a8a2dcf759fde3f14f91f93` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java` | 304 | `e7f055378c3fc23b904012663643c3deeb87a1655a61076817193e2dd6ecac70` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java` | 603 | `b03eebe30f719e869ddf6453552678bbfbf0bad7841c981adbbe85d9a597ed04` |

- 本轮未运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，未执行任何 Git mutation。Internal
  Maxwell 到此退出 TURN-28P；后续源码、测试、报告写入均由父级确认领取后的 External B 负责。

<!-- TRUE_EOF: TURN-28P INTERNAL OWNER RELEASED EXTERNAL-B HANDOFF Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T05:52:15.119-04:00 -->

## EXTERNAL-B REPAIR #2 CLAIMED - 2026-07-16T05:55:04-04:00

- Implementation Worker：**CR271 External Worker B**，不是 reviewer；父级仍是唯一 manager / final reviewer。本段不代表 `APPROVED/CLOSED`。
- 身份（诚实自报，非平台权威真值）：Claude Code 会话 `aa951b1e-8f04-4f92-b6e0-de08af49c39a`（UUIDv4 会话标识，**不是**平台 spawn 的 `019f…` UUIDv7）；自选临时 nickname `Kepler`。按父级「Worker 自报的非平台 UUID/nickname 不作为 owner 真值」，本 lane 权威 agent id/nickname 应以平台 spawn 记录为准，父级可比照 TURN-28P `CLAIM IDENTITY CORRECTION`（Locke）在本卡追加校正；ownership、写集、保护与禁令均原样由该权威身份承接。External-B lane 报告：`reports/2026-07-16-cr271-external-worker-b.md`。
- 领取依据：本卡 `INTERNAL OWNER RELEASED / EXTERNAL-B HANDOFF`（`2026-07-16T05:52:15.119-04:00`，Maxwell 释放内部 owner，父级将 TURN-28P Repair #2 改交 External B，并要求 External B 按最新 `PARENT DELIVERY REVIEW #3` 另行领取）。
- 已完整读取 `PARENT DELIVERY REVIEW #3`（`05:48:39.927`），**接受 `P0/P1/P2=0/2/1 / REPAIR #2 REQUIRED`**，并沿用 Repair #1 已关闭项：frozen path 零二次 refresh、queued remove 与 started callback 的 remove-first/cooperative terminal barrier、waiter 必须等 callback finally / Ctrl-UP settle、Ctrl-UP `Throwable` 先投影 `CTRL_RELEASE_FAILED`、唯一 after raw PNG、一 command/UUID、零自动 retry。

**接续基线（已逐项校验，与本卡 handoff 表一致，从这些确切字节增量返修）：**

| Repo | 文件 | 行数 | 当前 SHA-256（校验一致） |
|---|---|---:|---|
| DHXY | `input/action/InputActionQueue.java` | 778 | `dceca233def81afd3377f65cf4f5514efbc4044a2a81659ece3ee4ea46b1f407` |
| DHXY | `input/action/InputActionRequest.java` | 1060 | `b6702f65a56cf78f24df2e52fe2164e4c6be4d7aead57650e5ea5e2c35db9000` |
| DHXY | `input/action/InputActionWorker.java` | 597 | `8f52f456b1e74ab1e199f2ecfbf9ff2757908bda0aac8eb2eebfa46e5df3593b` |
| DHXY | `input/InputSequences.java` | 176 | `fa2f17bfb8b0ab672e986abcadc7c316b0eff1d3c9781424f7839a1b0f06fdd2` |
| DHXY | `input/WindowAwareInputCoordinator.java` | 268 | `ce40d510055919d98278d6c416bfb6acaca3a74374071b283ddbb2bbe152eb36` |
| DHXY | `cloud/turn/TurnCaptureStepExecutor.java` | 570 | `16e53f3727dbc171d9465b918c89762c806b888deca893687393988a260dd6a9` |
| DHXY | `test/.../InputActionFrozenExclusiveContractTest.java` | 409 | `d39b9a7236a66b15f170d33f458237e18efe8005358a5573c0da62d37fc57e61` |
| DHXY | `test/.../TurnCapturePixelChangeProbeContractTest.java` | 579 | `80d4fe80292409396a52a77620637ba6ae57d0c1e73922a13bd0a68c7218986e` |
| DHXY | `test/.../LocalTurnActionExecutorContractTest.java` | 1074 | `6a90d47c4e52c635b603a906ef338068f6ee9e9a0a8a2dcf759fde3f14f91f93` |
| Cloud | `test/.../TurnCapturePixelChangeInvocationContractTest.java` | 304 | `e7f055378c3fc23b904012663643c3deeb87a1655a61076817193e2dd6ecac70` |
| Cloud | `test/.../TurnGameClientContractTest.java` | 603 | `b03eebe30f719e869ddf6453552678bbfbf0bad7841c981adbbe85d9a597ed04` |

**Repair #2 exact write set = 上表 11 文件 + 本 append-only 原卡**；其它 protocol/JSON/Service/Task/caller/host/application/POM/config/resource/报告全部只读，不扩写集、不新增 production Java。

**本轮返修目标（严格按 Review #3 三项条件）：**
1. **P1-1 exact-window generation 原子性**：frozen public boundary 不再接收 caller-supplied epoch；在 `synchronized(context)` 内以 `context.getNativeBinding() == frozenBinding` 作 generation witness，并核 windowId/HWND/process/rect/非 suspended，同锁读取 epoch 写入 request；worker 取得 global input transaction 后在同一 context monitor 内完成唯一 authoritative exact check、显式 frozen focus、`tryStartStep(0)`、callback 及其 Java `finally`，释放 monitor 前不允许 binding commit；coordinator 只 focus 调用方显式 binding，不再维护第二套 mutable-context comparator。
2. **P1-2 typed STOP 投影**：frozen queue/facade 直接返回既有 `InputActionExecutionResult`；capture 在 cleanup barrier 后按 `releaseFailed -> callback/caller stopped 或 safetyReason=STOP_REQUESTED -> queue non-completed -> mechanics failure -> changed/unchanged` 顺序投影；不新增同义 result DTO，不用 detail 字符串猜 STOP。
3. **P2-1 点名测试穿透**：补齐 public resolver -> 真实 queue/worker 链、逐项 HWND/process/x/y/width/height/epoch 与 A->B->A generation drift、outer `InputActionWorker.catch(Throwable)` 的 non-Runtime UP seam，以及 Cloud code-only / valid frame-only 各自 fatal、timeout/interrupted uncertainty 的一 UUID/一 command/零 retry 独立负例；全部用内存 fake，禁真实 input/capture/runtime；不扫描源码、不反射 private helper；返回后零 mechanics 用 latch/atomic 证明，不用 sleep 猜竞态。

**保护与禁令（全部沿用）**：保护两仓全部既有 dirty/untracked 与 TURN-23P pointer-clear 等半成品，只从当前字节增量编辑，禁止 checkout/reset/restore/clean、覆盖式重建、删除或改写他人内容；不新增 snapshot DTO、wrapper chain、refresh、retry、deadline/TTL、owner/session/ledger/compaction/durable workflow、OCR/业务判断、第二 command/UUID/frame；不执行任何 Git mutation；不运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input（门全部保留给父级）。基线 `696a12b0`；DHXY committed HEAD `0114604e`、cloud-brain `3b988ca` 未动。
- 交付只在本卡追加 `REPAIR #2 SOURCE+TEST DELIVERED` + 逐文件 SHA/行证据/未运行门/新 true EOF；**不写 `APPROVED/CLOSED`、不自批、不冒充 reviewer**。

<!-- TRUE_EOF: TURN-28P EXTERNAL-B REPAIR-2 CLAIMED P0P1P2=0/2/1 Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T05:55:04-04:00 -->

## PARENT EXECUTION DIRECTIVE #1 - FINISH P2-1 WITHOUT INTERMEDIATE REPORT - 2026-07-16T07:06:11.254-04:00

- 父级已读取 External B 当前说明并核对实际磁盘写入。P1-1 generation monitor / exact binding witness 与 P1-2
  structured `InputActionExecutionResult` / ordered STOP projection 已有真实增量；当前不要求、也不接受用
  `PROGRESS` 段替代交付。Worker 不应停下来写中间汇报，只继续完成 Review #3 的 P2-1。
- 剩余工作按以下顺序一次闭合：
  1. 将 `TurnCapturePixelChangeProbeContractTest` 与 `LocalTurnActionExecutorContractTest` 中父级点名的两个同步
     `ProbeInputSequences` seam 改为穿透 public resolver -> 真实 `InputActionQueue` / `InputActionWorker` 的内存
     harness；必须覆盖 outer-worker non-Runtime Ctrl-UP 与返回后零 mechanics，禁止用 sleep 猜竞态。
  2. 在 Cloud 点名测试中分别补 probe caller 的 timeout 与 interrupted uncertainty；每个用例锁住一 UUID、一
     command、零 retry，不能用 plain capture caller 或合并成一个模糊用例。
  3. 完成后重算冻结 11 文件 SHA/行证据，在本卡物理 EOF 一次追加
     `REPAIR #2 SOURCE+TEST DELIVERED`；不得在缺口未闭合时提前交付，也不得自批。
- 保持 exact write set、全部禁令与未运行门不变；不新增 production、wrapper、retry/session/ledger/TTL，不运行
  Maven/runtime/input，不做 Git mutation。父级在正式 delivery true EOF 出现后立即独立逐文件审查。

<!-- TRUE_EOF: TURN-28P PARENT EXECUTION DIRECTIVE-1 FINISH-P2-1 NO-INTERMEDIATE-REPORT 2026-07-16T07:06:11.254-04:00 -->

## PARENT CRITICAL-PATH ESCALATION #2 - FINAL HARNESS MUST START - 2026-07-16T07:20:37.088-04:00

- 父级读取 External B 当前说明并再次核对真实磁盘 mtime：B 自述 P1-1/P1-2 与 P2-1 七项已闭合，但唯一剩余
  real queue/worker harness 尚未开始；两个被点名的关键文件仍分别停在
  `TurnCapturePixelChangeProbeContractTest.java=06:22:53.246`、
  `LocalTurnActionExecutorContractTest.java=06:08:33.100`。会话 context 余量不足不是继续占用关键 owner 而零写入的
  长期理由。
- External B 暂时仍是 TURN-28P 唯一 implementation owner，下一次 5 分钟 heartbeat 必须二选一：
  1. 立即开始修改上述两个文件，把同步 fake 换成 Directive #1 冻结的 public resolver -> real queue/worker 内存
     harness，并继续到一次正式交付；或
  2. 在本卡 true EOF 明确 `OWNER RETURNED`，停止全部 TURN-28P 写入，由父级改派。
- 若到 `2026-07-16T07:27:00-04:00` 两个文件仍均无新 mtime、且原卡无 owner-return/delivery，父级将先在本卡
  释放 External B，再优先把剩余 P2-1 安全改派给当前等待上游的 External A。释放前不允许任何第二 writer 触碰
  冻结 11 文件；不会并发双写。
- exact write set、一次 UUID/command、零 retry、无 Maven/runtime/input/Git mutation 与 `696a12b0` 等价约束不变。

<!-- TRUE_EOF: TURN-28P PARENT CRITICAL-PATH ESCALATION-2 EXTERNAL-B DEADLINE 2026-07-16T07:27:00-04:00 -->

## EXTERNAL-B OWNER RETURNED - 2026-07-16T07:25:04-04:00

回应 `PARENT CRITICAL-PATH ESCALATION #2`(07:20:37,deadline 07:27:00)。父级批评成立:context 余量不足不构成继续占用关键 owner 而零写入的理由。**选择选项 2:`OWNER RETURNED`** —— 我即刻停止对 TURN-28P 冻结 11 文件与本卡的一切写入,交还 owner 由父级改派(建议 External A,其正空等上游)。本段**不是** delivery,**不含** `APPROVED/CLOSED`,不自批。

**为何交还而非硬开工**:剩余唯一项(两个同步 fake → real queue/worker harness)约百余行、跨 2 文件、需逐项回读核对多个构造签名,且我被禁止编译自检。以我当前会话余量硬开工,极可能留下**不编译的测试树**并继续阻塞关键路径 —— 那比交还更坏。交还可让继任者从下述确切字节一次做完。

**交还时 11 文件确切字节(继任者请从此增量,勿回滚/覆盖)**:

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `DHXY input/action/InputActionQueue.java` | 794 | `bcd1e64a523ad258360cae4110c575e318acbb824ad1cdc49dd06ac0f3b1abc4` |
| `DHXY input/action/InputActionRequest.java` | 1085 | `1cff61300296ef42a4b6c2cd8cba89b40beaa27771178851cf6e52440e29f324` |
| `DHXY input/action/InputActionWorker.java` | 651 | `1359c2361e134829c98adf193a062019d59239b9642347dfb0bd35063be032bd` |
| `DHXY input/InputSequences.java` | 180 | `2d1768e67a12bf34d58fb64f14102614dc0c597eb41476dc60a49841089f2b6a` |
| `DHXY input/WindowAwareInputCoordinator.java` | 243 | `0f22571a5727248c34e26fdd8a7ed930c15b7b0106452050ccfaa3520f67e6b8` |
| `DHXY cloud/turn/TurnCaptureStepExecutor.java` | 587 | `5612b067e4a3f16b48845bd50dcc046cea3e15fc93781888637210e867ce59f0` |
| `DHXY test .../InputActionFrozenExclusiveContractTest.java` | 561 | `265fb5f25fa9ed0960de4bc04d05b8eabb3f0c719ca697df190852bf271fa2db` |
| `DHXY test .../TurnCapturePixelChangeProbeContractTest.java` | 649 | `ed2cd35cd419447d4f0d37cd0db4d05455115d184354b9469b3fd2f836cc9fe8` |
| `DHXY test .../LocalTurnActionExecutorContractTest.java` | 1086 | `20db8bb7a8fb3e9ae12ad8760c2191e2d570953a756b9f52878e5a291223520b` |
| `Cloud test .../TurnCapturePixelChangeInvocationContractTest.java` | 328 | `2b35046d14c3b0b822537474a07f34233ecb0333c1143fe1a2eec10a3b230520` |
| `Cloud test .../TurnGameClientContractTest.java` | 639 | `89da4fa3e61430dcfee39c313fc9cdb05d2905b3bbfd4a34bfac39f0a730ea67` |

**已闭合(待父级独立复核,我不自评通过)**:
- **P1-1**:①`WindowAwareInputCoordinator` `focusFrozenWindowInActiveTransaction(...,context,...,epoch)` → `focusFrozenBindingInActiveTransaction(actionName,windowId,binding)`,去 `synchronized(context)` 与全部 mutable-context 比较,**删私有 `sameExactWindow`**(第二 comparator);②`InputActionWorker` 加 frozen 首分支 + 新 `runFrozenExactWindowExclusive`:pause/admission 在锁外,`synchronized(context)` 内贯穿唯一 authoritative check → frozen focus → `tryStartStep(0)` → callback(含其 finally)→ cleanup safety → `markStepCompleted(0)`;③`InputActionRequest` 新增 `isFrozenExactWindowGenerationCurrent()`(`windowContext.getNativeBinding() != nativeBinding` 即判假=**对象同一性 generation witness**,叠加原值比较);④`InputActionQueue` frozen 边界**去 caller epoch**,同一 `synchronized(context)` 内 witness + 读 epoch + 建 request。
- **P1-2**:`InputActionQueue`/`InputSequences` 返回既有 `InputActionExecutionResult`(**未新增 DTO**);`TurnCaptureStepExecutor` 按 `releaseFailed → stopped(含 safetyReason==STOP_REQUESTED) → queue non-completed → mechanics failure → changed/unchanged` 有序投影。
- **P2-1 已完成 7 项**:①`stopClosedBeforeWorkerAdmissionProjectsStoppedInsteadOfAMechanicsFailure` + ②`nonStopIncompleteQueueResultStillProjectsProbeFailureNotStopped`(顺序守卫);③`valueEqualRebindIsANewGenerationAndStillRejectsBeforeCallback`(**A→B→A**,真实 queue/worker);④`everyExactWindowFieldDriftIndependentlyRejectsBeforeCallbackOrInput`(逐项 handle/process/x/y/w/h,真实 queue/worker);⑤`startedCancellation...` 的 `Thread.sleep(25)` 猜竞态 → 改 latch+watermark **顺序事实**;⑥`nonRuntimeThrowableEscapingTheCallbackIsNormalizedByTheRealWorkerIntoAClosedResult`(穿透 `InputActionWorker:210 catch(Throwable)`);⑦Cloud `aValidFrameWithoutAnyCompletedProbeCodeIsFatalOnItsOwn` + `probeCallerTimeoutAndInterruptedUncertaintyStayTypedWithOneUuidOneCommandAndZeroRetry`(**Directive #1 第②步已闭合**:probe caller、各锁一 UUID/一 command/零 retry、未用 plain capture caller、未合并)。

**唯一剩余 = Directive #1 第①步(交给继任者)**:
- 两个同步 fake 均在写集文件内、无写集边界问题:`LocalTurnActionExecutorContractTest.ProbeInputSequences`(声明 ~:899,唯一实例化 :518 `new ProbeInputSequences(events)`,经 :523 传入 `TurnCaptureStepExecutor`);`TurnCapturePixelChangeProbeContractTest.` **`RecordingInputSequences`**(声明 :415-478,实例化 :385,经 :400-408 传入 executor)。**注意后者类名不是 ProbeInputSequences**。
- **真实 harness 配方**(照抄 `InputActionFrozenExclusiveContractTest.Harness`,已验可用):`WindowIsolationProperties`(isolationEnabled/inputFocusEnabled=true)→`WindowTaskContextHolder(properties)`→`GlobalInputLock`→`new InputActionQueue(contextHolder, refreshService, new TaskExecutionContextHolder())`→`new WindowAwareInputCoordinator(inputLock, contextHolder, focusService, properties, metricsService, refreshService)`→`new InputActionWorker(queue, new InputActionDeadLetter(), inputProvider, coordinator, contextHolder, null)`→`worker.start()`;再 `new InputSequences(realQueue)` 传给 `TurnCaptureStepExecutor` 取代 fake。
- 三个 stub 钩子需改由**真实机制**驱动:`submissions` 改真实计数;`stubAdmissionRejected`+`stubSafetyReason=STOP_REQUESTED` 改为**真实 stop token 在 worker admission 前关闭**;`executionWindow` 的 context 须注入真实 `contextHolder`。仍须覆盖 outer-worker non-Runtime Ctrl-UP 与返回后零 mechanics,**禁 sleep 猜竞态**(用 latch/atomic)。
- 已验事实(继任者勿重查):`WindowNativeBindingRefreshService.refreshAndCommit`(:72)在 `synchronized(context)`(:80)内 `setNativeBinding`(:83)→ worker 持 monitor 真阻断 drift;`TurnExecutionWindow.resolve`(:49)返回的 binding 与 `context.getNativeBinding()` **同一对象** → 同一性 witness 成立;`WindowNativeBinding` 无 `withNativeHandle/withProcessId`,HWND/process 漂移须走 8 参全构造;`TurnStepResult` 是无校验纯 record → null stepCode 可构造;测试 harness `context(windowId, handle, **processId**)` 第三参**是 processId 不是 epoch**。

**本轮及此前均未运行** Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input;**未执行任何 Git mutation**;未碰冻结 11 文件以外的任何 production/protocol/config/只读件;两仓全部既有 dirty/untracked 与他人半成品完好;DHXY committed HEAD `0114604e`、cloud-brain `3b988ca` 未动。自此我不再写入 TURN-28P 任何文件与本卡,等待父级改派。

<!-- TRUE_EOF: TURN-28P EXTERNAL-B OWNER RETURNED Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T07:25:04-04:00 -->

## PARENT REPLACEMENT ASSIGNMENT - EXTERNAL-A NEXT - 2026-07-16T07:26:05.172-04:00

- 父级已核验 External B 的规范 `OWNER RETURNED` true EOF、11 文件交还 SHA 与两个目标测试 mtime；B owner
  已释放且从 `07:25:04` 起不得再写 TURN-28P。当前没有第二 owner，External A 的原 TURN-22 Repair #3 仍被本卡
  source 门阻断，因此其 lane 可安全接续本卡。
- External A 下一次 heartbeat 必须先完整读取 Parent Review #3、Directive #1、Escalation #2 与 B 的 owner-return，
  再在本卡物理 EOF 追加规范 `EXTERNAL-A REPLACEMENT CLAIMED`；未 claim 前不得改源码。
- replacement exact **修改**写集仅为：
  1. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`；
  2. DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`；
  3. 本 append-only 原卡。
  B 交还表其余 9 个 production/test 文件全部只读并按其 SHA 保护，不得回滚、格式化或“顺手修复”。
- 唯一实现目标：把 `TurnCapturePixelChangeProbeContractTest.RecordingInputSequences`（约 `:415`，实例约 `:385`）
  与 `LocalTurnActionExecutorContractTest.ProbeInputSequences`（约 `:899`，实例约 `:518`）的同步 callback fake 换成
  穿透 public resolver -> real `InputActionQueue` / `InputActionWorker` / `InputSequences(realQueue)` 的内存 harness。
  复用现有 `InputActionFrozenExclusiveContractTest.Harness` 形状：真实 context holder/global lock/queue/coordinator/worker，
  但 focus/capture/keyboard/input/provider 全为内存 fake，不触发桌面。submission 由真实 queue 计数；STOP 用真实 stop
  token 在 worker admission 前关闭；execution window 注入真实 context holder。保留 outer-worker non-Runtime Ctrl-UP、
  latch/atomic 的返回后零 mechanics；禁止 sleep 猜竞态、private reflection、source scan 或新增 production hook。
- 完成后重算并列出全部 11 文件 SHA/行证据，在本卡 true EOF 一次追加
  `EXTERNAL-A REPLACEMENT SOURCE+TEST DELIVERED`；不得自批。未运行 Maven/JUnit/compile/runtime/input 与零 Git
  mutation 继续诚实记录，父级收到 delivery 后立即独立审查。通过后 External A 回到 TURN-22 Repair #3 队首。

<!-- TRUE_EOF: TURN-28P PARENT REPLACEMENT ASSIGNMENT EXTERNAL-A NEXT TWO-TEST WRITESET 2026-07-16T07:26:05.172-04:00 -->

## EXTERNAL-A REPLACEMENT CLAIMED - 2026-07-16T07:31:04-04:00

- Implementation Worker：CR271 External Worker A，session `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`，model
  `claude-opus-4-8`；平台 spawn nickname 未在本会话回传，不自造，以父级 identity correction 为准。固定 lane 报告
  `reports/2026-07-16-cr271-external-worker-a.md`。我是 implementation Worker，非 reviewer；父级为唯一 manager/final
  reviewer，本段不含 `APPROVED/CLOSED`、不自批。
- 领取依据：`PARENT REPLACEMENT ASSIGNMENT - EXTERNAL-A NEXT`（07:26:05.172）。已按其要求**完整读取**
  `PARENT DELIVERY REVIEW #3`（05:48:39，P0/P1/P2=0/2/1）、`PARENT EXECUTION DIRECTIVE #1`（07:06:11）、
  `PARENT CRITICAL-PATH ESCALATION #2`（07:20:37）与 `EXTERNAL-B OWNER RETURNED`（07:25:04）。确认 B 自
  07:25:04 起不再写本卡，当前无第二 owner，无并发双写。
- 接续基线：从 B owner-return 表的**确切字节**增量，不回滚/覆盖/格式化/"顺手修复"。B 已闭合 P1-1 generation
  monitor + exact binding witness、P1-2 typed `InputActionExecutionResult` + ordered STOP projection；本轮不重开。
- 本轮 exact **修改**写集仅三项：
  1. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`
  2. DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
  3. 本 append-only 原卡
  B 交还表其余 **9 个 production/test 文件全部只读**并按其 SHA 保护。
- 唯一实现目标（Review #3 P2-1 剩余项 / Directive #1 第 1 条）：把
  `TurnCapturePixelChangeProbeContractTest.RecordingInputSequences`（约 :415，实例约 :385）与
  `LocalTurnActionExecutorContractTest.ProbeInputSequences`（约 :899，实例约 :518）的同步 callback fake，换成穿透
  public resolver -> real `InputActionQueue` / `InputActionWorker` / `InputSequences(realQueue)` 的内存 harness；
  复用现有 `InputActionFrozenExclusiveContractTest.Harness` 形状（真实 context holder/global lock/queue/coordinator/
  worker；focus/capture/keyboard/input/provider 全内存 fake，不触桌面）；submission 由真实 queue 计数；STOP 用真实
  stop token 在 worker admission 前关闭；execution window 注入真实 context holder；保留 outer-worker non-Runtime
  Ctrl-UP 与返回后零 mechanics（latch/atomic 证明）。
- 禁令：不用 sleep 猜竞态、不反射 private helper、不扫描源码、不新增 production hook/wrapper/retry/session/ledger/
  TTL/业务；不改 protocol/JSON/Service/Task/caller/POM/config/resource；保护两仓全部 dirty/untracked；**零 Git
  mutation**；不运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input。
- 交付：完成后重算并列出全部 11 文件 SHA/行证据，在本卡 true EOF **一次**追加
  `EXTERNAL-A REPLACEMENT SOURCE+TEST DELIVERED`；缺口未闭合不提前交付。通过后回到 TURN-22 Repair #3 队首。

<!-- TRUE_EOF: TURN-28P EXTERNAL-A REPLACEMENT CLAIMED session 76eac05a-e5cd-46a2-a58f-5a07c6573ccc 2026-07-16T07:31:04-04:00 -->

## EXTERNAL-A OWNER RETURNED - 2026-07-16T07:36:08-04:00

本段**不是** delivery，**不含** `APPROVED/CLOSED`，不自批。External A（session `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`）
自 07:36:08 起停止对 TURN-28P 冻结 11 文件与本卡的一切写入，交还 owner 由父级改派。

### 交还理由（如实）

- 我领取后**未写入任何 Java**：下表 11 文件与 External B `OWNER RETURNED`(07:25:04) 交还表**逐项 SHA + 行数完全一致**，
  零漂移、零回滚、零覆盖、零格式化。本卡仅追加了 `EXTERNAL-A REPLACEMENT CLAIMED` 与本段。
- 我已按父级要求完整读取 Review #3 / Directive #1 / Escalation #2 / B 的 owner-return，并**已把剩余工作逐行定位到可执行规格**
  （见下）。据此估算：需在**两个**测试文件各移植约 140 行 harness 机械（Harness ctor、CountingRefreshService、
  CountingFocusService、NoOpInteractionMetricsService、TestTaskManager、BareWindowTaskRunner + Unsafe `allocate`/
  `findUnsafe`、`noInputProvider` Proxy、BlockingRequest、blockWorker），再把每个 stub 旋钮逐个改写为真实机制，
  跨约 1650 行既有测试，且**被禁止编译自检**。
- 以本会话剩余 context 硬开工，极可能在中途耗尽并留下**不编译的测试树**，继续阻塞关键路径——按父级对 B 的同一裁决口径，
  那比交还更坏。父级同时明确"context 不足不是零写入长期占用 owner 的理由"，故我不停滞：**立即交还**，让 fresh-context lane
  一次做完。

### 交还时 11 文件确切字节（继任者请从此增量，勿回滚/覆盖/格式化）

| Repo | 文件 | 行数 | SHA-256 |
|---|---|---:|---|
| DHXY | `input/action/InputActionQueue.java` | 794 | `bcd1e64a523ad258360cae4110c575e318acbb824ad1cdc49dd06ac0f3b1abc4` |
| DHXY | `input/action/InputActionRequest.java` | 1085 | `1cff61300296ef42a4b6c2cd8cba89b40beaa27771178851cf6e52440e29f324` |
| DHXY | `input/action/InputActionWorker.java` | 651 | `1359c2361e134829c98adf193a062019d59239b9642347dfb0bd35063be032bd` |
| DHXY | `input/InputSequences.java` | 180 | `2d1768e67a12bf34d58fb64f14102614dc0c597eb41476dc60a49841089f2b6a` |
| DHXY | `input/WindowAwareInputCoordinator.java` | 243 | `0f22571a5727248c34e26fdd8a7ed930c15b7b0106452050ccfaa3520f67e6b8` |
| DHXY | `cloud/turn/TurnCaptureStepExecutor.java` | 587 | `5612b067e4a3f16b48845bd50dcc046cea3e15fc93781888637210e867ce59f0` |
| DHXY | `test .../input/action/InputActionFrozenExclusiveContractTest.java` | 561 | `265fb5f25fa9ed0960de4bc04d05b8eabb3f0c719ca697df190852bf271fa2db` |
| DHXY | `test .../cloud/turn/TurnCapturePixelChangeProbeContractTest.java` | 649 | `ed2cd35cd419447d4f0d37cd0db4d05455115d184354b9469b3fd2f836cc9fe8` |
| DHXY | `test .../cloud/turn/LocalTurnActionExecutorContractTest.java` | 1086 | `20db8bb7a8fb3e9ae12ad8760c2191e2d570953a756b9f52878e5a291223520b` |
| Cloud | `test .../turn/client/TurnCapturePixelChangeInvocationContractTest.java` | 328 | `2b35046d14c3b0b822537474a07f34233ecb0333c1143fe1a2eec10a3b230520` |
| Cloud | `test .../turn/client/TurnGameClientContractTest.java` | 639 | `89da4fa3e61430dcfee39c313fc9cdb05d2905b3bbfd4a34bfac39f0a730ea67` |

### 继任者可直接照做的剩余规格（我已定位，无需重推）

1. **可复用的现成真链样板**：`InputActionFrozenExclusiveContractTest.java` 已是真 queue/worker harness。
   - `Harness` ctor（约 :361-393）：`WindowIsolationProperties`(setIsolationEnabled(true)/setInputFocusEnabled(true)) →
     `WindowTaskContextHolder(properties)` → `GlobalInputLock` → **真** `InputActionQueue(contextHolder, refresh,
     new TaskExecutionContextHolder())` → **真** `WindowAwareInputCoordinator(inputLock, contextHolder,
     CountingFocusService, properties, NoOpInteractionMetricsService, refresh)` → **真**
     `InputActionWorker(queue, new InputActionDeadLetter(), noInputProvider(inputCalls), coordinator, contextHolder, null)`
     → `worker.start()`。
   - 窗口解析（约 :406-419）：`BareWindowTaskRunner`(Unsafe `allocate`) + `TestTaskManager` + `refresh.binding=...` →
     `TurnExecutionWindow.resolveForAction(action, manager, refresh)`。
   - 真实 admission 阻塞（约 :420-435）：`blockWorker()` 用另一线程 `queue.submitFrozenExactWindowExclusiveAndWait(...)`
     配 `CountDownLatch entered/release`，`entered.await(2, SECONDS)` 后返回 `BlockingRequest`——**这就是"真实 admission
     rejected"的产生器，不需要 stub**。
   - helper 全文在同文件 :437-561（CountingRefreshService/CountingFocusService/NoOpInteractionMetricsService/
     TestTaskManager/BareWindowTaskRunner/allocate/findUnsafe/noInputProvider/BlockingRequest）。
2. **目标 A：`TurnCapturePixelChangeProbeContractTest.java`**
   - 现状：`Harness`(:380-414) 用 `RecordingInputSequences queue = new RecordingInputSequences(events)`；
     `RecordingInputSequences extends InputSequences { super(null) }`(:415-476) 同步直跑 callback，并 override
     `submitFrozenExactWindowExclusiveAndWait`(:436-452) 用 stub 旋钮伪造 typed result。
   - 需改为：`InputSequences(realQueue)` 注入 `TurnCaptureStepExecutor`(:407-415 构造处)，`contextHolder` 换成 harness 的
     **真** holder，`window` 经 `resolveForAction` 解析。
   - 逐个旋钮 → 真实机制：`stubAdmissionRejected=true`+`stubSafetyReason=STOP_REQUESTED`(:241-242) → **真 stop token 在
     worker admission 前关闭**；`stubAdmissionRejected=true`+`CLEAR`(:262-263) → **真 `blockWorker()` 占用 global input
     transaction**；`runCallback=false,result=false`(:136-137) → **真 queue 非完成**；`queue.submissions`
     (:114/:144/:225/:249/:269/:287/:308) → **真实 queue 计数**。
   - 保留：outer-worker non-Runtime Ctrl-UP 必须穿透 `InputActionWorker.catch(Throwable)`；返回后零 mechanics 用
     latch/atomic（**删除 :183-193/:370-407 一带的 `Thread.sleep(25)` 猜测**）。
3. **目标 B：`LocalTurnActionExecutorContractTest.java`**：同形改造 `ProbeInputSequences`（约 :899，实例约 :518），
   覆盖完整 public `LocalTurnActionExecutor.execute` 的 resolver -> queue -> worker -> focus/capture/keyboard/input 链
   （Review #3 P2-1 点名 :335-421,897-930）。
4. **禁令不变**：不 sleep 猜竞态、不反射 private helper、不扫描源码、不新增 production hook/wrapper/retry/session/
   ledger/TTL；capture/keyboard/mouse/focus 全内存 fake，不触真实 input/capture/runtime；只改这两个测试 + 原卡，
   其余 9 文件按上表 SHA 只读保护。

### 诚实记录

- 本轮**未运行** Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；**零 Git mutation**
  （无 commit/stage/branch/merge/rebase/cherry-pick/checkout/reset/restore/clean）；两仓全部既有 dirty/untracked 保护未动。
- External A 到此退出 TURN-28P，回到 lane 在线状态；其 TURN-22 Repair #3 仍被本卡 source 门阻断，等父级改派。

<!-- TRUE_EOF: TURN-28P EXTERNAL-A OWNER RETURNED zero-java-write 11-files-unchanged 2026-07-16T07:36:08-04:00 -->

## PARENT REPLACEMENT ASSIGNMENT #2 - EXTERNAL-D NEXT - 2026-07-16T07:38:20-04:00

- 父级已独立核验 External A 的规范 owner-return、两份目标测试 mtime 与 11 文件当前 SHA；全部逐项等于 A/B
  交还表，A 确实零 Java 写入并自 `07:36:08` 起释放。External B 也已释放；当前没有 implementation owner，
  External D 的 TURN-34B 仍被 TURN-22 阻断，因此可安全接续，零双写。
- External D 下一次 heartbeat 必须先完整读取 Parent Review #3、Directive #1、B/A 两次 owner-return 与本段，
  再在本卡物理 EOF 追加规范 `EXTERNAL-D REPLACEMENT CLAIMED`；未 claim 前不得改源码。
- exact modify write set 仍只有：
  1. DHXY `TurnCapturePixelChangeProbeContractTest.java`；
  2. DHXY `LocalTurnActionExecutorContractTest.java`；
  3. 本 append-only 原卡。
  其它 9 文件按 A 的 11-SHA 表只读；不得回滚、格式化或扩大写集。
- 唯一目标不变：将两份测试的同步 callback fake 换成 public resolver -> real `InputActionQueue` /
  `InputActionWorker` / `InputSequences(realQueue)` 内存 harness，并按现成
  `InputActionFrozenExclusiveContractTest.Harness` 形状用真实 admission/stop/context 机制证明；不触桌面，
  不使用 sleep 猜竞态、private reflection/source scan、production hook、retry/session/ledger/TTL。
- 完成后一次写 `EXTERNAL-D REPLACEMENT SOURCE+TEST DELIVERED`，列 11 文件 SHA/行证据；不得自批，不运行
  Maven/JUnit/compile/runtime/input，不做 Git mutation。父级收到正式 delivery 后独立逐文件审查。

<!-- TRUE_EOF: TURN-28P PARENT REPLACEMENT-2 ASSIGNMENT EXTERNAL-D NEXT TWO-TEST WRITESET 2026-07-16T07:38:20-04:00 -->

## PARENT CLAIM ESCALATION #3 - EXTERNAL-D FINAL CLAIM WINDOW - 2026-07-16T07:49:20-04:00

- 父级再次核对本卡、External D lane 报告和两份目标测试：`07:38:20` assignment 后已跨过至少一个完整
  External 5 分钟 heartbeat 窗口，但本卡物理 EOF 仍无 `EXTERNAL-D REPLACEMENT CLAIMED`；两份测试仍保持
  A/B 交还 SHA 与 `06:22:53` / `06:08:33` mtime，当前依然是**零 implementation owner、零源码写入**。
- External D 最后领取截止为 `2026-07-16T07:54:20-04:00`。下一次 heartbeat 必须先在本卡 true EOF 规范
  CLAIM；未 claim 不得修改源码。若截止时仍无 claim/owner-return，父级将撤销 D 的 NEXT assignment，并在本卡
  记录释放后把这两个测试安全改派给当刻可用 implementation 容量；撤销记录落盘前仍禁止第二 writer。
- exact write set、其余 9 文件 SHA 只读、无 Maven/runtime/input/Git mutation 与一次完整 delivery 口径均不变。

<!-- TRUE_EOF: TURN-28P PARENT CLAIM-ESCALATION-3 EXTERNAL-D DEADLINE 2026-07-16T07:54:20-04:00 ZERO-OWNER -->

## PARENT REPLACEMENT ASSIGNMENT #2 REVOKED - INTERNAL REPLACEMENT REQUIRED - 2026-07-16T07:58:25-04:00

- `07:54:20-04:00` 最终领取截止已过。父级在 `07:58:25-04:00` 再次核对本卡物理 EOF、External D lane
  报告和两份目标测试：仍无 `EXTERNAL-D REPLACEMENT CLAIMED`，目标文件仍为
  `ed2cd35cd419447d4f0d37cd0db4d05455115d184354b9469b3fd2f836cc9fe8`（mtime `06:22:53`）与
  `20db8bb7a8fb3e9ae12ad8760c2191e2d570953a756b9f52878e5a291223520b`（mtime `06:08:33`）。
- External D 的 TURN-28P NEXT assignment 自本段 true EOF 起正式撤销；D lane 保持在线但无卡、不是 owner，
  不得再写 TURN-28P。当前本卡仍为零 implementation owner，可安全改派一名 Internal replacement。
- Internal replacement 必须先在本卡物理 EOF 追加规范 `INTERNAL REPLACEMENT CLAIMED` 才可编辑；exact
  modify write set、其它 9 文件只读 SHA、真实 queue/worker harness 目标与全部禁令保持不变。

<!-- TRUE_EOF: TURN-28P PARENT REPLACEMENT-2 REVOKED EXTERNAL-D NO-CLAIM ZERO-SOURCE-CHANGE INTERNAL-REPLACEMENT-READY 2026-07-16T07:58:25-04:00 -->

## INTERNAL REPLACEMENT CLAIMED - 2026-07-16T08:04:28.103-04:00

- 平台权威身份：agent id `019f6acb-7722-7442-bd9a-f9204cf2e69c`，nickname `Euler`，role `worker`。
  本 Worker 是 CR271 / TURN-28P Repair #2 Internal implementation Worker，不是 reviewer；不批准、不关闭本卡。
- 领取依据：已完整读取要求的仓库规则、上下文、CR271 顶部记录、权威计划第 14-19 节、协议规格、
  `docs/业务逻辑.md`、两仓 `git status` 与本卡完整正文；领取时物理 EOF 为父级
  `INTERNAL-REPLACEMENT-READY`，且 `PARENT REPLACEMENT ASSIGNMENT #2 REVOKED` 已明确撤销 External D
  assignment。当前无第二 implementation owner。
- Exact modify write set 仅限：
  1. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`；
  2. DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`；
  3. 本 append-only 原卡。
  TURN-28P 其它 9 个冻结 production/test 文件严格只读；两仓全部既有 dirty/untracked 原样保护，不回滚、覆盖、
  清理、删除、格式化或顺手修改，不执行任何 Git mutation。
- 领取时 SHA-256：
  - `TurnCapturePixelChangeProbeContractTest.java` =
    `ed2cd35cd419447d4f0d37cd0db4d05455115d184354b9469b3fd2f836cc9fe8`（649 行）；
  - `LocalTurnActionExecutorContractTest.java` =
    `20db8bb7a8fb3e9ae12ad8760c2191e2d570953a756b9f52878e5a291223520b`（1086 行）。
- 唯一实施目标：把两份测试中的同步 callback fake 替换为 public resolver -> real `InputActionQueue` /
  `InputActionWorker` / `InputSequences(realQueue)` 的全内存 harness，并使用真实 admission/stop/context 机制保留
  outer-worker non-Runtime Ctrl-UP typed failure、A -> B -> A exact-window drift、started-callback cooperative
  cancellation completion barrier 与 probe uncertainty 契约。禁止 sleep 猜竞态、private reflection/source scan，
  禁止新增 production hook/wrapper/业务/OCR/retry/session/ledger/TTL/durable workflow。
- 本 Worker 不运行 Maven/JUnit/compile/package，不启动 runtime/application/server/Task/UI/capture/input；
  capture/keyboard/mouse/focus 仅允许内存 fake。完成后只追加一次
  `INTERNAL REPLACEMENT SOURCE+TEST DELIVERED` 与最终 SHA/精确行证据，不写中间 `PROGRESS`，不自批。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

<!-- TRUE_EOF: TURN-28P INTERNAL REPLACEMENT CLAIMED Euler 019f6acb-7722-7442-bd9a-f9204cf2e69c TWO-TEST WRITESET 2026-07-16T08:04:28.103-04:00 -->

## PARENT OWNERSHIP CLARIFICATION - TURN-28Q SPLIT - 2026-07-16T08:23:11.657-04:00

- TURN-22 独立暴露的 frozen action-list production 缺口已拆为真实共享 mechanics 子卡 `TURN-28Q`，其五文件
  production/test 写集由 External A 领取后独占；不把该关键阻塞实现塞给 Internal Euler，也不允许 TURN-22 猜接口。
- Euler 继续保持本卡唯一 owner，exact write set **仍仅**两份 DHXY contract test + 本原卡；已落盘的
  `TurnCapturePixelChangeProbeContractTest.java` 增量必须保留，不回滚。Euler 不修改 TURN-28Q 五文件。
- 两卡写集互斥：TURN-28P 闭合 callback/probe public resolver -> real queue/worker harness；TURN-28Q 闭合完整
  action-list 在同一 exact-window generation monitor 中的一次 submission。两者都通过后才解除 TURN-22 最终门。
- 本段不是 delivery/source pass；既有禁令与 `696a12b0` 等价合同不变。

<!-- TRUE_EOF: TURN-28P PARENT OWNERSHIP-CLARIFIED EULER-TWO-TESTS TURN-28Q-EXTERNAL-A-DISJOINT 2026-07-16T08:23:11.657-04:00 -->

## INTERNAL REPLACEMENT SOURCE+TEST DELIVERED - 2026-07-16T08:28:05.095-04:00

- Implementation Worker：Euler `019f6acb-7722-7442-bd9a-f9204cf2e69c`（nickname `Euler`，role `worker`）。
  本段仅交付 TURN-28P Repair #2 的两份 contract-test 源码；Euler 不是 reviewer，不写 `APPROVED/CLOSED`，
  不代表测试通过、构建通过或父级 source review 通过。
- 已完整读取 08:23:11 父级 `PARENT OWNERSHIP CLARIFICATION - TURN-28Q SPLIT`。本轮 exact write set 始终只有：
  1. `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`；
  2. `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`；
  3. 本 append-only 原卡。
  TURN-28Q 五文件及 TURN-28P 其余冻结文件均由本 Worker 零写入；未回滚、覆盖、格式化或强行恢复任何并行 owner 字节。

### `TurnCapturePixelChangeProbeContractTest` 源码证据

- `:485-652` 建立全内存真链 harness：真实 `WindowTaskContextHolder` / `GlobalInputLock` /
  `InputActionQueue` / `WindowAwareInputCoordinator` / `InputActionWorker`；`:549-556` 由 public
  `TurnExecutionWindow.resolveForAction(...)` 取得窗口后注入 `InputSequences(realQueue)`。`:626-647` 的 queue
  子类只做 probe submission 计数并调用 `super.submitFrozenExactWindowExclusiveAndWait(...)`，不运行 callback fake，
  不手工构造 typed result。
- `:72-95,421-445,918-921` 覆盖 changed/unchanged、唯一 after raw PNG、exact ROI/未缩放 MOVE、唯一 refresh/focus，
  并在真实 worker callback 内断言 exact context/binding。`:208-218` 保留 non-Runtime Ctrl UP 的唯一 release attempt、
  `CTRL_RELEASE_FAILED` 与无 frame；`:299-312` 让 non-Runtime callback failure 穿透真实
  `InputActionWorker.catch(Throwable)`，证明非 stop incomplete result 仍为 `PIXEL_PROBE_FAILED`。
- `:260-292` 用真实 `TaskExecutionContext` / `TaskStopToken` 在排队后、worker admission 前关闭 stop；`:315-353`
  用真实 blocker 排队后执行 A -> B -> A value-equal rebind，断言 generation drift 在 focus/capture/Ctrl/MOVE 前拒绝；
  `:356-385` 用 latch/atomic 证明 started callback cooperative cancellation 必须等待 Ctrl UP settle 和最终 mechanics
  watermark 后才返回，未使用 sleep 猜竞态。

### `LocalTurnActionExecutorContractTest` 源码证据

- `:579-661,1077-1101` 将原 `ProbeInputSequences` 同步 fake 替换为真实 queue/worker/coordinator 与
  `InputSequences(realQueue)`；计数 queue 始终委托 production `super`。`:824-875` 提供真实 task-context async waiter
  与 admission blocker；`:1126-1162` 的内存 keyboard fake 在 worker 回调内断言 exact context/binding。
- `:350-400` 从 public `LocalTurnActionExecutor.execute(...)` 穿透 resolver -> queue -> worker ->
  focus/capture/keyboard/input，断言一次 resolver/refresh/submission/focus、exact mechanics 顺序及唯一 after frame；
  `:403-432` 保留 mechanics failure/stop 不伪 completed code/frame；`:435-477` 证明 admission 前真实 stop token
  精确投影 whole-action `STOPPED` 且零 probe mechanics；`:480-498` 保留 failure evidence 严格晚于 Ctrl UP。

### 最终字节与静态门

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `TurnCapturePixelChangeProbeContractTest.java` | 965 | `5d563bbb08747c7b298ec6c7c0795a600269bc86d8f5769bcc67588268fda818` |
| `LocalTurnActionExecutorContractTest.java` | 1275 | `88011cf17b24e68b8dcf5c7ef11edd30fb8a9df2aac27e639e320e3bd4dd3709` |

- 两文件 trailing whitespace=`0`；`RecordingInputSequences` / `ProbeInputSequences` / `Thread.sleep` / 手工
  `InputActionExecutionResult.builder()` 命中均为 `0`。没有 private production helper reflection、source scan、
  production hook/wrapper、业务/OCR/retry/session/ledger/TTL/durable workflow。
- 按父级禁令，本 Worker **未运行** Maven/JUnit/compile/package，未启动 runtime/application/server/Task/UI，
  未触真实 capture/keyboard/mouse/focus/input；所有桌面 mechanics 均为内存 fake。**零 Git mutation**，未执行
  commit/stage/branch/merge/rebase/cherry-pick/checkout/reset/restore/clean。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。交付后 Euler 停止修改，等待父级独立审查。**

<!-- TRUE_EOF: TURN-28P INTERNAL REPLACEMENT SOURCE+TEST DELIVERED Euler 019f6acb-7722-7442-bd9a-f9204cf2e69c TWO-TEST REAL-QUEUE-WORKER HARNESS 2026-07-16T08:28:05.095-04:00 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #4 - PASSED - 2026-07-16T08:42:21.828-04:00

- 父级独立逐文件读取 Euler 交付的两份测试、所穿透的 public resolver、真实 `InputActionQueue` /
  `InputActionWorker` / `InputSequences(realQueue)`、TURN-28P 既有 production/test、协议与 Review #3 返修条件；
  复算 SHA 与交付一致：probe test=`5d563bbb08747c7b298ec6c7c0795a600269bc86d8f5769bcc67588268fda818`，
  local executor test=`88011cf17b24e68b8dcf5c7ef11edd30fb8a9df2aac27e639e320e3bd4dd3709`。
- 结论：**`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING`**。
- 两测试已从同步 callback fake 改为 public resolver -> real queue/worker 全内存 harness；真实 worker/admission
  覆盖 exact context/binding、A -> B -> A generation drift 零 input、admission 前 STOP、started callback
  cooperative cancellation 等待 finally/Ctrl-UP settle、outer-worker non-Runtime Ctrl-UP typed failure，以及
  production `LocalTurnActionExecutor.execute(...)` 的 STOP/failure/after-frame 投影。未发现手工 result、源码扫描、
  private production helper 反射或 sleep 猜竞态。
- Euler implementation owner 已释放。独立 reviewer R1 Epicurus
  `019f6af2-6e80-72c2-a9a3-c0b09b043e3d` 与 R2 Dirac
  `019f6af2-c39a-78a2-abb7-735991d6ad4a` 已派发；两 reviewer 最新轮均 APPROVED 后，仍须等 Java writers
  稳定再运行授权 named tests 与适用 compile/build，父级方可 CARD APPROVED。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

<!-- TRUE_EOF: TURN-28P PARENT REVIEW-4 PASSED P0P1P2=0/0/0 SOURCE-TEST-SOURCE-REVIEW-PASSED INDEPENDENT-REVIEW-BUILD-PENDING 2026-07-16T08:42:21.828-04:00 -->
