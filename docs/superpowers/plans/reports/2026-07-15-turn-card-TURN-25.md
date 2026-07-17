# TURN-25 - Dialog detection and prepared-action validation

## PARENT FROZEN IMPLEMENTATION BRIEF - 2026-07-15 23:46 EDT

- 状态：`READY / PARENT BRIEF FROZEN`。
- 角色边界：实现 Worker 不是 reviewer；CR271 父级是唯一 manager/final reviewer。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `DialogService#detectDialogSnapshotDirect`、`captureDialogSnapshot`、`hasDialogMask`、
  `hasOptionInLowerHalf`、`hasStoryInUpperHalf`、
  `validatePreparedDialogActionForConsume` 与 `washPreparedValidationCrop`。
- 依赖：TURN-16 与 TURN-18 的 source gate 已通过；TURN-26 的 OCR/options/white-story 不属于本卡。

### Exact write set

- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java`。
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogDetectionTurnContractTest.java`。
- 本报告 true EOF append。

其余两仓文件全部只读。尤其 DHXY、protocol、`TurnGameClient`、Task/caller、Spring configuration、旧 macro
DTO/codec、TURN-26 ports/tests 与 POM 不得修改。不得新增第二 client/port/model/helper/wrapper；必要结果只能使用
既有类型或上述三个 production 文件内的 private nested type。保护全部 dirty/untracked，不回滚、覆盖、清理、
提交或执行其它 Git mutation。

### Frozen production contract

1. **一次 JSON action，本地只做 mechanics。** 每次 dialog observation 只生成一个 UUID、提交一个 HTTPS turn
   command，禁止自动 retry、fallback capture 或第二 action。Cloud 从 exact bound `TurnGameClient` 读取最新
   `TurnWindowMetadata` 一次，校验 device/window identity，并以真实 `windowRect.left/top` 计算未缩放 ROI：
   `left+250, top+312, width=529, height=208`。不得把窗口左上角写死为 `(0,0)`，不得使用
   `CoordinateHelper#getScaledRect` 重新缩放该 ROI。
2. **等待/后台按键/截图同 payload 有序执行。** 保持 696 基线顺序：若 `waitBeforeCaptureMs>0`，先
   `WAIT(waitBeforeCaptureMs)`；若 `hidePlayerNames=true`，随后在同一 action 中执行后台
   `INPUT KEY_TAP ALT_4 -> WAIT(220ms)`；最后一个 step 是上述 ROI 的
   `CAPTURE/UPLOAD_IMAGE`。无等待且不隐藏时 action 只有一个 CAPTURE step。不得使用 Cloud sleep、本地 OCR、
   本地 dialog 分类或旧 `DIALOG_DETECTION` local macro。
3. **raw PNG 严格关联。** 只接受 exact action/device/window、完整 step index/type/status、CAPTURE source step、
   exact ROI、`image/png`、SHA-256、width/height 与可解码像素全部一致的 frame。confirmed STOPPED 走既有
   checkpoint；未确认终态、transport uncertain、BUSY/DUPLICATE、correlation/hash/dimension mismatch 必须
   fail closed，不能伪装为 `NONE` 或成功。已确认的 mechanics/capture unavailable 可保持基线 benign
   `DialogDetection.none()`，但不得重发。
4. **Cloud 同帧分类。** Port 只把 raw frame 交给 Cloud；`DialogService` 在这一个 image 上按 696 顺序执行：
   dialog mask `stddev < 30.0`；随后 option lower-half `greenCount > 150`；option 未命中才按
   `STORY_MIN_TEXT_PIXELS=450`、rows `10`、max-row-white `40`、clusters `20`、span `120` 判断 story。
   OPTION 优先于 STORY；未命中为 NONE。返回的 `DialogDetection` 保留同一 image 和真实 screen-absolute
   dialog rect，供当前调用链同 tick 复用；分类不得二次截图。现有 public API、默认随机 700..799ms 等待、
   日志和 caller 顺序保持不变。
5. **prepared validation 只取一帧，算法在 Cloud。** `action==null`、`clickRequired=false`、缺 fingerprint 或
   非法 rect 的既有短路保持且零 command。有效 click-required action 对其既有 screen-absolute validation rect
   只发一个 CAPTURE command；Port 不得调用旧 `DIALOG_PREPARED_ACTION_VALIDATION` local macro。Cloud 对返回的
   同一 image 按既有 `YELLOW/GREEN/WHITE/TEMPLATE_SPECIFIC` wash，构建 binary fingerprint，并与 expected
   fingerprint 计算 distance；普通 maxDistance=`8`，`XIULUO_ENTER_BATTLE`=`16`。`distance<=max` 才刷新
   `lastVerifiedAtMs` 并返回原 action，mismatch/capture unavailable 按基线返回 null；不得二次 capture。
6. **范围禁令。** 本卡不迁移 option OCR、词序、green/white template、business-option fallback 或点击消费；
   不改变任何 retry/fallback/phase/park/terminal 业务顺序，不新增 session/ledger/TTL/durable workflow，不启动
   runtime/application/server/Task/UI/capture/input。

### Named-test acceptance

唯一测试类 `DialogDetectionTurnContractTest` 必须进入 production `DialogService` 与两个 production port/
`TurnGameClient` 路径，不能只测复制 mapper。至少覆盖：

- 非零 window origin 下 exact dialog ROI、不缩放；no-hide/no-wait 单 CAPTURE；positive-wait + hide 的同 action
  exact `WAIT -> ALT_4 -> WAIT(220) -> CAPTURE`，并断言每次调用一个 UUID/command、零 retry。
- 真实 PNG/metadata/hash/dimensions；mask miss、OPTION、STORY、mask-without-text；同帧同时满足时 OPTION 优先，
  每案只有一个 capture frame。
- capture unavailable benign none；confirmed STOPPED；uncertain/BUSY/DUPLICATE/correlation/hash/dimension mismatch
  fail closed，不伪成功。
- prepared 的 null/non-click/missing-fingerprint/invalid-rect 零 command；四种 wash mode、distance `<=8` 与 `>8`、
  Xiuluo `<=16`、exact absolute ROI、lastVerified refresh、known capture miss、STOPPED/uncertain/correlation；每个有效
  validation 恰好一个 capture command且零第二帧。
- 源码断言 active production path 对旧 `LocalMacroKind.DIALOG_DETECTION`、
  `DIALOG_PREPARED_ACTION_VALIDATION`、`executeLocalMacro` 零引用；TURN-26 OCR/template 行为与 public API 未改。

Worker 不运行 Maven/JUnit/compile；父级待所有 Java writers 稳定后只运行用户授权的
`HTTPS_TURN_CONTRACT_TEST_FAMILY` named test 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-25 parent-frozen-brief -->

## CLAIMED - 2026-07-15T23:51:20-04:00

- 角色：CR271 TURN-25 implementation Worker；不是 reviewer。CR271 父级是唯一 manager/final reviewer。
- Exact write set：
  - Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`。
  - Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java`。
  - Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java`。
  - Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogDetectionTurnContractTest.java`。
  - Append only `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-25.md` true EOF。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 Dialog detection、capture、mask/option/story classification 与 prepared-action validation/wash/fingerprint/distance 行为；按 TURN-25 parent-frozen brief 等价迁移，保留 public API、默认等待和业务顺序。
- 禁令：除上述 exact write set 外两仓全部只读；不得修改 DHXY、protocol、`TurnGameClient`、config/POM、Task/caller、旧 macro DTO 或 TURN-26 OCR/options/white-story；不得新增第二 client/port/model/helper/wrapper、session/ledger/TTL/durable workflow、自动 retry、fallback capture 或第二 action/frame；保护全部 dirty/untracked，不回滚、覆盖、清理、提交或执行任何 Git mutation；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input。

<!-- TRUE_EOF: TURN-25 implementation-worker-claimed -->

## REPLACEMENT CLAIMED - 2026-07-16T00:01:58-04:00

- 前会话：Volta `019f6909-790a-7750-8c08-e2a955167989` 已 `not_found`；保留其可能已落盘的全部半成品并从当前 bytes 接续，不回滚、不覆盖、不重做并发改动。
- Replacement implementation Worker：`019f6914-61fd-7270-89d6-a251e826f71c`；不是 reviewer，CR271 父级是唯一 final reviewer。
- 继承 exact write set：
  - Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`。
  - Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java`。
  - Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java`。
  - Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogDetectionTurnContractTest.java`。
  - Append only `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-25.md` true EOF。
- 继续遵守原卡全部边界：其余两仓只读；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；不执行 Git mutation；保护全部 dirty/untracked。

<!-- TRUE_EOF: TURN-25 replacement-claimed -->

## SOURCE DELIVERED + TEST DELIVERED - 2026-07-16T00:41:00-04:00

- 交付角色：Replacement implementation Worker `019f6914-61fd-7270-89d6-a251e826f71c`；不是 reviewer，等待 CR271 父级唯一 final review。
- 接管核验：前会话 Volta `019f6909-790a-7750-8c08-e2a955167989` 已 `not_found`；替代接管时三个 production 文件与首次检查 hash/mtime 一致，唯一 named test 尚不存在，因此没有发现可继承的 Volta source/test 半成品。全程从已存在 bytes 增量修改，没有回滚、清理或覆盖其他 dirty/untracked 内容。
- Source delivered：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`，2879 行，SHA-256 `4B5F21DDE030FFEDB6777D013BF7766564E0E243FB041C5A02B56FF273C307BF`。Dialog observation 仅消费 port 返回的一帧 raw PNG，并在 Cloud 同帧按 mask `stddev < 30.0`、OPTION `greenCount > 150`、STORY `450/10/40/20/120` 顺序分类，OPTION 优先；prepared validation 保留零 command 短路，并对单帧执行四种 wash、binary fingerprint、普通 `8`/修罗 `16` distance 与 `lastVerifiedAtMs` refresh。
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java`，338 行，SHA-256 `4AB6C3EBA67432E828B7B177DED9234AC64D9D9A62B6AF4F1BF1A1F8D0DEB1A2`。每次 observation 从 exact bound `TurnGameClient` 读取真实 window origin，构造 `left+250/top+312/529x208` 未缩放 ROI；一个 UUID/command 内按可选 WAIT、可选 `KEY_TAP ALT_4 -> WAIT 220`、最终 CAPTURE 排序，并严格校验终态、step、window、frame、ROI、SHA、PNG 与尺寸；已确认 mechanics/capture unavailable 为 benign none，uncertain/correlation 为 fail closed，confirmed STOPPED 进入既有 checkpoint，零 retry/fallback capture。
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java`，260 行，SHA-256 `6E104A356782B18988A7476F123D35D21C462B65BCD04EEE164D1E02FCFC44C2`。有效 prepared action 的既有 screen-absolute validation rect 只发一个 CAPTURE command，返回严格关联并解码的 fresh raw PNG；无旧 macro、无 wash/分类下沉、无 retry/fallback/第二帧。
- Test delivered：`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogDetectionTurnContractTest.java`，952 行，SHA-256 `3A4C5A890BD8ABD28D053649ECFF24670C2DA5F5281AA4F4906F5B434F6F0B67`。测试进入 production `DialogService`、两个 production port 与 production `TurnGameClient`，覆盖 exact nonzero-origin ROI/action 顺序、单 UUID/command/frame、真实 PNG 分类与 OPTION 优先、失败/不确定/correlation/STOPPED、prepared 零 command 短路、四 wash mode、distance `8/>8/16`、absolute ROI、refresh 与 TURN-26/source guard。
- 静态证据：四个 Java 文件花括号分别为 `794/794`、`45/45`、`39/39`、`84/84`；三个 production 文件对 `LocalMacroKind.DIALOG_DETECTION`、`DIALOG_PREPARED_ACTION_VALIDATION`、`executeLocalMacro`、`captureDialogSnapshot(`、`hidePlayerNamesBeforeDialogCapture(` 扫描均为零 active reference。named test 是该类唯一文件；当前仓库 `.gitignore:15` 的 `src/test/` 规则会隐藏它，因此 `git status --short` 不显示该测试，但文件、行数与 hash 已在磁盘确认，父级运行 named gate 时须直接按路径/类名取用。
- Worker 门禁：遵照 frozen brief 第 79-80 行及用户硬禁令，未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input；上述为源码/协议/结构静态证据，不冒充测试通过或编译通过。父级待 Java writers 稳定后运行用户授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named test 与适用 Cloud compile/build。
- 范围确认：除冻结的三个 Cloud production 文件、唯一 named test 与本报告 true EOF 外未写其他文件；未修改 DHXY、protocol、`TurnGameClient`、Task/caller、Spring config、POM 或 TURN-26，未执行任何 Git mutation，未新增自动 retry、fallback capture、session、ledger 或 TTL。
- 业务基线：`docs/业务逻辑.md` 的五倍/修罗 shared Dialog flow、普通怪 OPTION、五环 prepared action 与无 TTL 规则已核对。**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-25 source-and-test-delivered -->

## PARENT SOURCE + TEST SOURCE REVIEW - 2026-07-16T00:49:20-04:00

- 审查角色：CR271 父级唯一 manager/final reviewer；已独立逐文件审查三个 production 文件、完整
  `DialogDetectionTurnContractTest`、`TaskExecutionContext` exact-window/checkpoint 语义、真实 caller 表面与
  `696a12b0` Dialog detection/prepared-validation 基线，不以 Worker 自述代替结论。
- 结论：`P0/P1/P2=0/3/0 / REPAIR #1 REQUIRED`；本卡不得进入 named-test/build cohort，owner 保持原
  McClintock，返修后由父级重新逐文件复审。
- **P1-1：prepared validation 把 transport/correlation fail-closed 降成普通 mismatch。** 证据：
  `DialogService.java:1363-1369` 只单独传播 `TaskStopRequestedException`，随后宽捕获全部
  `RuntimeException` 并返回 `null`；两个 production port 对 timeout/uncertain/BUSY/DUPLICATE、window/frame/
  SHA/region/dimension correlation 失败抛出的 `TaskFatalException` 因而被吞掉。named test
  `DialogDetectionTurnContractTest.java:423-447` 反而断言 uncertain、wrong hash、wrong region 返回 null。
  影响：不确定或串窗响应会被 caller 当成“图片已确认不匹配”，破坏 frozen contract 的 terminal/uncertain
  fail-closed 边界。返修必须显式传播 `TaskFatalException`；只有 port 已确认的 capture FAILED 返回 null，以及
  Cloud 同帧 fingerprint 真实 mismatch 才保持基线 null，并把上述三类测试改成 fatal 断言。
- **P1-2：latest metadata 已请求 STOP 时仍创建 UUID 并执行 CAPTURE。** 证据：
  `CloudDialogDetectionPort.java:81-95` 与 `CloudDialogPreparedActionValidationPort.java:76-92` 在读取/校验
  metadata 后未做 stop checkpoint，直接构建或执行 command；现有测试 `:449-458` 只模拟 command 后
  STOPPED，并明确接受 `uuid=1/execute=1`，没有 preflight STOP 的零命令用例。影响：已停止的 task 仍可产生
  background Alt+4/capture 或 prepared capture。返修必须在 UUID/action/port 前处理 latest STOP：用既有
  `TaskCheckpoint` 确认并抛 stop；若 checkpoint 未确认则 typed fatal，不得继续发 command；两条 port 均补
  latest-STOP `uuid=0/execute=0` 负例。
- **P1-3：exact binding 只核 device/window，遗漏 immutable HWND/process。** 证据：
  `CloudDialogDetectionPort.java:283-292` 与 `CloudDialogPreparedActionValidationPort.java:204-214` 的
  `requireExactBinding` 只比较 `TurnInvocationContext.deviceId/windowId`，但
  `TaskExecutionContext.java:143-161` 已保留初始 exact `nativeHandle/processId`。同 logical windowId 被新 HWND/
  process 替换时，当前实现会在发现不了旧绑定的情况下先执行 capture。返修须在 UUID/action/port 前同时比较
  latest metadata 的 nativeHandle/processId 与 context 初始 exact 值，并分别补 handle/process mismatch
  `uuid=0/execute=0` 负例；outcome correlation 继续保留。
- Repair #1 精确写集仍仅限原三 production 文件、唯一 named test 与本报告；不改 DHXY/protocol/
  `TurnGameClient`/Task/caller/config/POM/TURN-26，不新增 command/frame、自动 retry、session、ledger、TTL 或
  durable workflow。Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，
  不执行 Git mutation。

**无已批准业务差异；返修用于恢复 `696a12b0` 与冻结 HTTPS turn 的 stop、exact-window、uncertain 边界。**

<!-- TRUE_EOF: TURN-25 parent-review-repair-1 -->

## SOURCE+TEST REPAIR DELIVERED - 2026-07-16T00:56:00-04:00

- 返修角色：Replacement implementation Worker `019f6914-61fd-7270-89d6-a251e826f71c`；不是 reviewer，Repair #1 等待 CR271 父级唯一 final reviewer 复审。
- **P1-1 repaired：**`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`，2877 行，SHA-256 `C1CF351C6353EA0BBC7BDC92CD822D4BE1D7BD9177CEFD7802D2FF565D013A2A`。`validatePreparedDialogActionForConsume` 现显式重抛 `TaskStopRequestedException` 与 `TaskFatalException`，已移除把其余 `RuntimeException` 降成 `null` 的宽捕获。有效 validation 中只有 production port 已确认 `FAILED` 返回 raw `null` 后的 capture miss，以及真实 binary-fingerprint distance mismatch 返回 `null`；零 command 的既有 null/non-click/missing-fingerprint/invalid-rect 短路保持。
- **P1-1 test repaired：**`DialogDetectionTurnContractTest#preparedCaptureFailureUncertaintyCorrelationAndStopStayClosedWithoutRetry` 中 confirmed capture miss 仍 `assertNull`；timeout uncertain、wrong SHA、wrong ROI 现均经 production `DialogService`/prepared port/`TurnGameClient` 路径 `assertThrows(TaskFatalException.class)`，且各自仍断言一个 UUID、一个 command、action 内一个 CAPTURE step、零 retry/第二 action；timeout uncertain 不伪造返回 frame，wrong SHA/ROI 的唯一返回 frame 被严格拒绝。普通 distance `>8` 的真实 fingerprint mismatch 仍 `assertNull`。
- **P1-2 repaired：**`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java`，358 行，SHA-256 `3577B5B54D27EDCCC982420D75EBAC8670600870D655F3A2541B019CD480F486`；`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java`，281 行，SHA-256 `A17588775F147601F2B4E1FC8EEFBFE1E6A1DC39E8E2AAAAC15EB2422C7CAD3E`。两 port 在 latest exact metadata 的 `stopRequested=true` 分支内、`client.execute/capture` 之前调用既有 `TaskCheckpoint`；confirmed STOP 显式传播 `TaskStopRequestedException`，checkpoint 未确认/transition 则 typed `TaskFatalException`，所有分支均不创建 UUID/action 或提交 command。
- **P1-2 test added：**`latestStopIsResolvedBeforeUuidAndCommandForBothPorts` 分别覆盖 detection/prepared 的 `STOP -> STOP` confirmed stop 与 `STOP -> ACTIVE` unconfirmed fatal；四案均断言 `uuid=0`、`execute=0`、`actions=0`，并断言 checkpoint 所需的两次 metadata read。
- **P1-3 repaired：**两个 port 复用并扩展原 `requireExactBinding`，latest pre-port fence 与 outcome correlation 均同时核对 `deviceId/windowId`，以及 latest `nativeHandle/processId` 对 `TaskExecutionContext` 初始 exact handle/process；不匹配在 UUID/action/command 前抛 `TaskFatalException`。既有完整 outcome snapshot、step/frame/ROI/SHA/PNG/dimension correlation 未删除。
- **P1-3 test added：**`latestHandleAndProcessMismatchFailBeforeUuidAndCommandForBothPorts` 分别覆盖 detection/prepared 的 wrong handle 与 wrong process 共四案；每案均经 production Service/port 路径断言 `TaskFatalException`、`metadataReads=1`、`uuid=0`、`execute=0`、`actions=0`。
- Test delivered：`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogDetectionTurnContractTest.java`，1070 行，SHA-256 `BB02629F2CF4BAD078E42B8F7A0C31EA3CB9B2A5304691144E205D708B2547B3`。当前 `.gitignore:15` 的 `src/test/` 规则仍隐藏该文件，但磁盘文件、唯一类路径、行数与 hash 已确认。
- 保留合同：普通 observation 仍仅一个 HTTPS JSON action/UUID/command 与一个 raw PNG frame；prepared 有效 observation 仍仅一个 CAPTURE；Cloud-only mask/OPTION/STORY 与 wash/fingerprint/distance 算法、OPTION 优先、默认等待、confirmed mechanics/capture FAILED benign 结果、outcome correlation 和零 retry/fallback 均保持。未新增 helper/wrapper/command/frame/session/ledger/TTL/durable workflow。
- 静态证据：四个 Java 文件花括号计数分别为 `789/789`、`49/49`、`43/43`、`86/86`；三个 production 文件对旧 `LocalMacroKind.DIALOG_DETECTION`、`DIALOG_PREPARED_ACTION_VALIDATION`、`executeLocalMacro`、`captureDialogSnapshot(`、`hidePlayerNamesBeforeDialogCapture(` 扫描仍为零 active reference。
- Worker 门禁：遵照父级 Repair #1 与用户禁令，未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input；未执行 Git mutation。上述是源码与测试断言静态交付，不冒充 test/compile 通过，等待父级复审后决定 named-test/build cohort。
- 范围确认：仅写原三个 Cloud production 文件、唯一 named test 与本报告 true EOF；未修改 DHXY、protocol、`TurnGameClient`、Task/caller、Spring config、POM 或 TURN-26，并保护两仓其余并行 dirty/untracked。

**无已批准业务差异；Repair #1 仅恢复 `696a12b0` 与 frozen HTTPS turn 的 fatal、preflight STOP、immutable exact-window 边界。**

<!-- TRUE_EOF: TURN-25 source-test-repair-1-delivered -->

## PARENT SOURCE + TEST-SOURCE RE-REVIEW - REPAIR #1 PASSED - 2026-07-16 01:02 EDT

- 审查角色：CR271 父级唯一 manager/final reviewer。父级独立复读三个 production 文件、完整
  `DialogDetectionTurnContractTest`、turn-native bound context/checkpoint 和 `696a12b0` Dialog detection/prepared
  validation 基线，并重算全部交付 SHA；不以 Worker 自述替代结论。
- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING`。Repair #1 的三项 P1
  全部关闭，McClintock owner 可释放；这不是 named-test/build 已通过或 CARD CLOSED。
- **fatal 边界已关闭：**`DialogService.java:1332-1375` 只让 confirmed capture miss 和真实 fingerprint distance
  mismatch 返回 null，并显式传播 `TaskStopRequestedException`/`TaskFatalException`；测试
  `DialogDetectionTurnContractTest.java:523-565` 已将 uncertain、wrong SHA、wrong ROI 改为 fatal，同时保留每案
  一个 UUID/command/frame、零 retry。
- **latest STOP 预检已关闭：**两个 port 在 latest exact metadata 后、UUID/action/command 前调用既有
  `TaskCheckpoint`；confirmed STOP 传播 stop，未确认 transition typed fatal。测试 `:347-392` 覆盖 detection 与
  prepared 的 STOP->STOP、STOP->ACTIVE，四案均为 `uuid=0/execute=0/actions=0`。
- **immutable exact binding 已关闭：**`CloudDialogDetectionPort.java:300-312` 与
  `CloudDialogPreparedActionValidationPort.java:222-235` 在 latest pre-port 和 outcome correlation 同时核对
  device/window/HWND/process；测试 `:394-438` 覆盖两 port 的 handle/process mismatch，均在 command 前失败。
- `TaskExecutionContext` 保存的是 exact-context bound `TurnGameClient`，因此两 port 的 metadata/capture/execute 仍只
  走该 bound view；一次 dialog action/raw PNG、Cloud 同帧 OPTION/STORY 分类与 prepared wash/fingerprint/distance、
  零 fallback capture/自动 retry 均保持。
- 交付哈希与报告一致：`C1CF35...`、`3577B5...`、`A17588...`、`BB0262...`。父级未运行 Maven/JUnit/compile，
  因为 TURN-T04 与 TURN-23 writer 仍活动；named test 与适用 Cloud compile/build 进入 stable-writer cohort。

**无已批准业务差异；Repair #1 恢复 `696a12b0` 与冻结 HTTPS turn 的 fatal、STOP、exact-window 边界。**

<!-- TRUE_EOF: TURN-25-parent-repair-1-passed -->
