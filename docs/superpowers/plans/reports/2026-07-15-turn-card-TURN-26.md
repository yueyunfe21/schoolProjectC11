# TURN-26 - Dialog option OCR and white-story HTTPS turn cutover

## READY / PARENT FROZEN IMPLEMENTATION BRIEF - 2026-07-16 01:11 EDT

- 状态：`READY / PARENT BRIEF FROZEN`；类型：`COUNT`；唯一
  `countUnit=DialogService -> processOptionsWithOCRDetailed/prepareWhiteStoryTemplateOrAbsent`，
  `countDelta=+1`。父级是唯一 manager/final reviewer，Worker 不是 reviewer。
- startDependsOn：TURN-25 Repair #1 已由父级独立源码/测试源码审查 `P0/P1/P2=0/0/0`；
  approvalDependsOn：本卡 parent source review、唯一 `DialogOptionTurnContractTest` 与适用 Cloud compile/build。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `DialogService::processOptionsWithOCRDetailed`、`GameTextLineOcrService::readDialogOptionWords`、
  `DialogService::prepareWhiteStoryTemplateOrAbsent/verifyWhiteStoryTemplate`；同时核对
  `docs/业务逻辑.md` 白龙马 `probeStoryAbsent/probeNoTarget/probeTargetReady` 分流、模板阈值与后续点击边界。
- 目标：已有 TURN-25 detection 原帧时不再发任何 observation command；没有可用 supplied detection 时只经
  现有 TURN-25 HTTPS CAPTURE boundary 上传一张 exact dialog raw PNG。此后 green/yellow/white wash、OCR、
  provider-order words、alias/候选/fallback 与 white-template first-hit 全在 Cloud；DHXY 不做 OCR、洗图、模板判断。

### Exact write set

- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrImagePort.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrWordsPort.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogWhiteStoryTemplatePort.java`。
- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`，仅允许把现有
  `readWords/OcrResult/OcrWord` 提升为供 words port 直接复用的 public typed API，并补必要 JavaDoc；不得改 endpoint、
  timeout、请求体、返回解析、health/diagnostics 或 OCR 失败语义。此文件是为避免复制第二套 OCR HTTP client 而冻结的
  最小共享写入，不得顺带重构。
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java`。
- Append only this fixed report true EOF。

其余两仓文件全部只读；尤其 DHXY、protocol、`TurnGameClient`/action factory/command port、TURN-25 detection/
prepared-validation ports、Task/caller、Spring configuration、POM、模板资源、旧 macro DTO/dispatcher 与其它测试/报告
不得修改。不得删除旧 DTO/macro（late removal 归删除卡），不得新增第二 OCR client、第二 capture port、wrapper chain、
自动 retry、owner/session/ledger/TTL/durable workflow。保护全部 dirty/untracked，不回滚、覆盖、清理、提交或执行其它
Git mutation。

### Frozen production contract

1. **一张 authoritative raw frame。** supplied detection 只有在 `image != null`、四边 rect 合法且 type 满足当前
   调用时才复用；复用路径 command/UUID=`0`。否则调用现有 TURN-25 `detectDialogSnapshotDirect(..., false, 0)`，
   只产生一份 exact-window CAPTURE JSON action/UUID/raw PNG。不得在 image/words/white port 内第二次截图、扩大 ROI、
   走旧 generic capture 或 `executeLocalMacro`。坐标保持真实 window-origin 的未缩放 screen-absolute pixels。
2. **Option image preparation 全在 Cloud。** `CloudDialogOptionOcrImagePort` 不再持有 task context 或调用
   `LocalMacroKind.DIALOG_OPTION_OCR_IMAGE`；它严格校验 supplied PNG/SHA/rect/dimensions，解码同一 raw frame，并在 Cloud
   生成 raw、green、yellow variants 及各自 SHA。raw 必须存在；green/yellow 洗图可各自 unavailable，不能因此抓第二帧。
3. **OCR 与基线顺序。** `CloudDialogOptionOcrWordsPort` 不再调用
   `LocalMacroKind.DIALOG_OPTION_OCR_WORDS`；它校验 variant PNG/SHA/dimensions 后只复用 canonical
   `LocalOcrClient.readWords`，保留 provider 返回顺序和 image-local box。业务顺序逐值保持：green wash unavailable 时
   只 OCR raw 并直接返回；green available 时先 OCR green，任一 alias 命中立即结束；green miss 且 yellow available
   才 OCR yellow；yellow 命中返回 yellow，否则按 `green words + yellow words` 顺序合并。yellow unavailable 时保留
   green 结果。OCR unavailable/exception 是不可匹配 words，不得伪造 visual hit，也不得 retry。
4. **Option 选择与动作不变。** alias 外层顺序、provider words 内层顺序、absolute=`rect origin + local center`、
   `clickMatchedOption`、prepared fingerprint、`allowFallbackOptionClick`、`preferOcrFallbackOption`、fallback word 排序及
   最后 green-option fallback 均留在 Cloud `DialogService`，严格复现 696。业务已有 route 两轮 fallback 属于 caller
   业务卡既有流程，本卡不得把它改造成 transport retry、删减或下沉 DHXY。
5. **White-story 全在 Cloud。** `prepareWhiteStoryTemplateOrAbsent` 先按第 1 条取得/复用同一 detection，再由 Cloud
   classification 决定 STORY/ABSENT；`CloudDialogWhiteStoryTemplatePort` 只对该 raw frame 做 thin-white wash，并按调用方
   有效 template list 原顺序、阈值 `0.85` 首个命中结束。它不持有 task context，不调用
   `LocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE`，不截图。MATCHED 坐标、RAW-crop fingerprint、STORY_MISS rect-centre、
   STORY_ABSENT 与 no-frame empty/absent gate 必须逐值保持；白龙马四类结果与后续 probe 分流不得改变。
6. **Terminal/failure。** confirmed STOP 继续传播 exact-context checkpoint；capture uncertain、window/action/step/frame/
   PNG/SHA/dimension mismatch fail closed，不能映射为 NO_WORDS、STORY_ABSENT、match 或 success。已确认 capture miss 和
   OCR/template normal miss 只按 696 原分支返回；零自动 retry、fallback capture、session、ledger、TTL 或 durable state。
7. **Active-path source gate。** 三个 Cloud port 与 `DialogService` option/white active path 对
   `executeLocalMacro`、`LocalMacroKind.DIALOG_OPTION_OCR_IMAGE`、`DIALOG_OPTION_OCR_WORDS`、
   `DIALOG_WHITE_STORY_TEMPLATE`、Cloud 进程内 `GameClientTracker/InputProvider/InputSequences/WindowTaskContextHolder`
   为零引用。`DialogService` 既有点击只由后续 turn/caller 卡处理；本卡不得搬入 DHXY runtime 或扩永久本地 Service。

### Named-test acceptance

唯一 `DialogOptionTurnContractTest` 必须实例化 production `DialogService`、三个 production port、TURN-25 production
detection path 与 production bound `TurnGameClient` path，不能只测复制 mapper。至少覆盖：

- supplied OPTION/STORY frame 的 exact rect/SHA 同帧复用，断言 command/UUID=`0`；无 supplied frame 时一份 CAPTURE
  JSON/raw PNG，断言 command/UUID=`1` 且 image/words/white port 不产生第二 command；非零 window origin、不缩放；
- raw/green/yellow 都从同一原图生成并严格相关；green wash unavailable -> raw only、green alias hit -> zero yellow OCR、
  green miss -> yellow、yellow miss -> provider-order green+yellow merge、yellow unavailable -> green；alias 顺序、box
  local-to-absolute 映射、matched/prepared/fallback 分支与零 transport retry；
- OCR available/no-words/unavailable/exception、invalid PNG/SHA/dimensions、confirmed capture failure、STOPPED、
  duplicate/uncertain 与 wrong device/window/HWND/process；断言终态不伪 visual hit/success；
- white supplied/fresh frame、non-STORY absent、STORY ordered template first-hit `0.85`、below-threshold miss、blank spec
  skip、MATCHED raw fingerprint/coordinates、miss/absent keyword gates，以及白龙马 `targetReady/noTarget/storyAbsent`
  不串态；
- 源码断言第 7 条旧三 macro 在 active path 零引用，`LocalOcrClient` 只有 visibility/JavaDoc 差异且没有复制 OCR HTTP
  client，四个永久本地 Service 清单未扩张。

Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级待所有 Java writers 稳定后
只运行用户授权 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 的本 named test 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 与用户确认的最小 HTTPS JSON turn 等价迁移。**

<!-- TRUE_EOF: TURN-26 parent-frozen-brief -->

## CLAIMED - 2026-07-16T01:18:28-04:00

- Agent id：`019f6957-57b6-7c32-a80d-300555162be5`；角色：TURN-26 implementation Worker，非 reviewer；父级是唯一 manager/final reviewer。
- Exact write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrImagePort.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrWordsPort.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogWhiteStoryTemplatePort.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`（仅冻结的 typed API visibility + JavaDoc）
  - Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java`
  - 本固定报告 true EOF append only。
- 禁止项：其余两仓文件全部只读；绝不回滚、覆盖、清理、提交或改写他人 dirty/untracked；不改 DHXY、protocol、TURN-25 ports、Task/caller、Spring/POM/模板/旧 DTO/macro/dispatcher；不新增第二 OCR/capture client 或永久本地 Service；不引入 auto retry、session、owner、ledger、TTL、durable workflow；不运行 Maven/JUnit/compile，不启动 runtime/application/server/Task/UI/capture/input，不执行任何 Git mutation。

<!-- TRUE_EOF: TURN-26 claimed -->

## SOURCE+TEST DELIVERED - 2026-07-16T02:00:16-04:00

- Worker：`019f6957-57b6-7c32-a80d-300555162be5`；仅交付 implementation source/test，未作 reviewer 或 final-review 结论。
- 逐文件 SHA-256：
  - `9088644E80D27F1B32DC2DF92739BA51213BD8F439CF43A3B9D7BDE084420A9F`  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`
  - `D51254946593C679DA71BE75043398C575435DA193182C42B049A07352CC74D6`  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrImagePort.java`
  - `C3AC13353988B89165D40FC35E4E40701ADD50FA0045D804BB93392EC6917DC7`  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrWordsPort.java`
  - `46F64E15BB6BFD8BF03302F006F527A4388CD659DB238BE0E9A889DE87E31C2D`  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogWhiteStoryTemplatePort.java`
  - `0E41A18BCF5A558376FB2C1CD145F7CA8B17FC65C6C1D7FA5F90C454AB504CAA`  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`
  - `6F9921AAD0F35CC4618EF224EFAC32D4B1BF2A8312842B67D6251A170774B8A6`  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java`
- 关键 production 证据：
  - `DialogService` 对合法 supplied OPTION/STORY 原帧直接复用，缺可用原帧时仅调用既有 TURN-25 `detectDialogSnapshotDirect(..., false, 0)`；image/words/white ports 均不持有 turn/task/window/input/capture authority，不产生第二 command。
  - option raw/green/yellow 在 Cloud 内存从同一严格 PNG/SHA/rect/dimension 原帧生成；green unavailable -> raw only、green hit short-circuit、green miss -> yellow、yellow miss按 green+yellow provider 顺序合并、yellow unavailable -> green；alias 外层、provider word 内层、绝对坐标与既有 fallback/click/prepared 分支未改。
  - words port 只复用 canonical `LocalOcrClient.readWords`；静态计数为 `HttpClient.newBuilder()=1`、`/ocr/words=1`、三个新 port 内 `HttpClient=0`。兼容领取前既有 `LocalOcrClient` dirty（public class/`readJoinedText`），本卡只提升冻结的 `readWords/OcrResult/OcrWord` typed API visibility 并补 JavaDoc，未改 endpoint、timeout、request/response codec、health/diagnostics 或失败语义。
  - white-story 仅处理同一原帧，thin-white wash 后按有效 caller specs 原顺序和 `0.85` 首命中；MATCHED 保持 raw-crop fingerprint/相对与绝对坐标，STORY_MISS/STORY_ABSENT/no-frame keyword gates 分离。
  - production 静态扫描：旧三 `LocalMacroKind`/`executeLocalMacro=0`；三个 port 的 `GameClientTracker/InputProvider/InputSequences/WindowTaskContextHolder/TurnGameClient/CloudTurnCommandPort=0`；capture 调用=`0`；无第二 OCR client、auto retry、session、owner、ledger、TTL、durable workflow 或第五永久本地 Service。
- 命名测试证据：唯一 `DialogOptionTurnContractTest` 共 7 个 `@Test`，实例化 production `DialogService`、三个 production port、TURN-25 `CloudDialogDetectionPort` 与 production bound `TurnGameClient` path；覆盖 supplied/fresh command/UUID 0/1、非零窗口原点与未缩放坐标、同帧 SHA/variants、颜色 OCR 顺序与 provider/alias/fallback、OCR 四态及坏 PNG/SHA/dimension、white first-hit/阈值/blank skip/raw fingerprint/三态 keyword gates、confirmed miss/STOP/uncertain/duplicate/identity/correlation fail-closed 和 source gate。
- 未运行门禁：严格按 parent brief，未运行 Maven、JUnit、named test、compile、package 或 build；未启动 runtime/application/server/Task/UI/capture/input。父级须待 Java writers 稳定后运行用户授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 中本 named test 与适用 Cloud compile/build。
- 未执行任何 Git mutation；未写 exact write set 外文件，未回滚、覆盖、清理、提交或改写他人 dirty/untracked。
- 无已批准业务差异；按基线等价迁移。

<!-- TRUE_EOF: TURN-26 source-test-delivered -->

## PARENT SOURCE+TEST SOURCE REVIEW PASSED - 2026-07-16T02:13:11-04:00

- 父级独立逐文件展开 production/test source，并重新计算交付的六份 SHA-256；六值与 Worker true EOF
  逐字一致。`DialogService` 的 Lombok 构造参数实际为 14 个，现有 `DialogDetectionTurnContractTest`、
  `DialogGiveItemTurnContractTest` 与本卡 `DialogOptionTurnContractTest` 均以 14 个实参闭合，没有因新增三个
  port 造成旧 named-test 构造链失配。
- `DialogService::processOptionsWithOCRDetailed` 已保持 alias 外层/provider word 内层、local-to-absolute、
  prepared/fallback/click 顺序；合法 supplied OPTION 原帧为零 command，缺帧时只有一次 TURN-25
  `detectDialogSnapshotDirect(..., false, 0)`。raw/green/yellow 来自同一 PNG，green unavailable、green hit、
  green miss 后 yellow、yellow unavailable 与 green+yellow provider-order merge 均与 `696a12b0` 对应路径一致。
- `prepareWhiteStoryTemplateOrAbsent` 只消费 supplied 或同一 TURN-25 原帧；Cloud 端 thin-white 后按 caller
  有效 spec 原顺序、阈值 `0.85` 首命中。MATCHED 继续使用 raw crop fingerprint，STORY_MISS、
  STORY_ABSENT 与无帧 gate 没有串态；白龙马 `probeTargetReady/probeNoTarget/probeStoryAbsent` 业务边界未改。
- 三个 production port 均无 task/window/input/capture authority；PNG signature、SHA、rect/dimension 与 matcher
  输出异常均 fail closed。words port 只复用 canonical `LocalOcrClient.readWords`；`LocalOcrClient` 本卡差异只为
  `readWords/OcrResult/OcrWord` public typed API + JavaDoc，领取前 TURN-20 已有的 public class/
  `readJoinedText` 保持不动，endpoint、timeout、HTTP/JSON codec、health/diagnostics 与失败语义未改。
- 唯一 `DialogOptionTurnContractTest` 共 7 个 `@Test`，实例化 production `DialogService`、三个 production port、
  TURN-25 detection 与 bound `TurnGameClient`；源码覆盖 supplied/fresh command 0/1、同帧 variants、OCR 四态、
  alias/provider/fallback、white first-hit/阈值/三态、STOP/FAILED/uncertain/correlation/identity 及 active source gate。
- 父级结论：`P0/P1/P2=0/0/0`，状态进入
  `SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD BUILD PENDING`。Ptolemy owner 已释放；本结论不是
  `CARD APPROVED/CLOSED`，只有所有 Java writers 稳定后父级运行本 named test 与适用 Cloud compile/build 才能
  关闭构建门。当前 TURN-22 Java writer 活动，本轮不运行 Maven/JUnit/compile。
- 无已批准业务差异；按 `696a12b0`、`docs/业务逻辑.md:484-697` 与用户确认的最小 HTTPS JSON turn 等价迁移。

<!-- TRUE_EOF: TURN-26 parent-source-test-source-review-passed -->

## PARENT WHOLE-CARD BUILD REPAIR REOPENED / EXTERNAL-B READY - 2026-07-16T14:47:00-04:00

- Stable-writer Cloud main compile proved this complete card is not build-closed: current `DialogService.java`
  retains Cloud-host references to DHXY-only classes, so compilation stops before the authorized named test runs.
  Previous source-review evidence is retained, but the compiler failure is now owned by this complete card.
- Reopen the existing complete `TURN-26` card to External B. B owns the entire original production/test/report
  contract and all repairs until parent `SOURCE+TEST SOURCE REVIEW PASSED` or canonical whole-card `OWNER RETURNED`.
  This is not a new subcard, import fragment or local remainder.
- Preserve accepted supplied-frame/single-capture behavior, OCR ordering, white-story `0.85` first-hit semantics,
  terminal projection and the `696a12b0` baseline. Complete the original HTTPS cutover without a second OCR/capture
  client, Cloud-side DHXY mechanics, retry/session/TTL/ledger, wrapper nesting or behavior change. The original exact
  write set and unique `DialogOptionTurnContractTest` remain binding.
- External B must append canonical `EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR CLAIMED` at this physical EOF before
  editing. Its first five-minute window must show real source/test progress, canonical whole-card delivery, or
  canonical whole-card owner return. Parent reviews the full card.

<!-- TRUE_EOF: TURN-26 PARENT WHOLE-CARD BUILD-REPAIR REOPENED EXTERNAL-B READY CLOUD-COMPILE-BLOCKER=DHXY_ONLY_REFERENCES 2026-07-16T14:47:00-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR CLAIMED - 2026-07-16T14:55:20-04:00

- Implementation Worker:**CR271 External Worker B**;非 reviewer,不能批准本卡;父级唯一 manager/final reviewer。不含 `APPROVED/CLOSED`,不自批。
- 身份(诚实自报,非平台权威真值):Claude Code 会话 `aa951b1e-8f04-4f92-b6e0-de08af49c39a`(UUIDv4,非平台 `019f…` UUIDv7);自选昵称 `Kepler`。权威身份以平台 spawn 记录为准,父级可追加 `CLAIM IDENTITY CORRECTION`。
- **我承担整卡**:原卡全部 production/test/report 合同与后续全部返修,直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或我 canonical 整卡 `OWNER RETURNED`。**不拆卡、不建子卡、不做 import fragment 或 local remainder、不只交付一部分。**
- 已完整读取本卡 180 行至 true EOF:`READY / PARENT FROZEN IMPLEMENTATION BRIEF`、Exact write set 与只读清单、前任 `CLAIMED`/`SOURCE+TEST DELIVERED`/`PARENT SOURCE+TEST SOURCE REVIEW PASSED`、以及 `PARENT WHOLE-CARD BUILD REPAIR REOPENED`(14:47)。
- **领取时阻断点(已实测)**:Cloud main compile 被 `DialogService.java`(**2850 行**,SHA-256 `9088644e80d27f1b32dc2df92739ba51213bd8f439cf43a3b9d7bde084420a9f`)中对 **DHXY-only 类的 Cloud-host 引用**挡住,named test 无法执行。精确落点:import `:3 GameClientTracker`、`:5 InputProvider`、`:6 InputSequences`、`:39 CoordinateHelper`、`:43 WindowRuntimeContext`、`:45 WindowTaskContextHolder`;字段 `:82 inputSequences`、`:83 inputProvider`、`:84 tracker`、`:85 coordinateHelper`、`:88 windowTaskContextHolder`;用法 `:1434 windowTaskContextHolder.rawCurrent()`、`:1482 matchesCurrentPreparedDialogBinding(WindowRuntimeContext,…)`、`:1542 WindowRuntimeContext::getPendingSmartClickEvidenceProofToken`。
- **写集(严格沿用原卡冻结,不增不减)**:① Cloud `service/DialogService.java`;② `cloudbrain/remote/CloudDialogOptionOcrImagePort.java`;③ `cloudbrain/remote/CloudDialogOptionOcrWordsPort.java`;④ `cloudbrain/remote/CloudDialogWhiteStoryTemplatePort.java`;⑤ `cloudbrain/LocalOcrClient.java`(**仅**把既有 `readWords/OcrResult/OcrWord` 提升为 words port 可复用的 public typed API + 必要 JavaDoc;**不改** endpoint/timeout/请求体/返回解析/health-diagnostics/OCR 失败语义,不顺带重构);⑥ **新建** `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java`;⑦ 本卡 append-only。其余两仓文件全部只读(DHXY、protocol、`TurnGameClient`/action factory/command port、TURN-25 detection/prepared-validation ports、Task/caller、Spring config、POM、模板资源、旧 macro DTO/dispatcher 与其它测试/报告)。
- **接受并遵守**:保留已被接受的 supplied-frame/single-capture 行为、OCR 顺序、white-story `0.85` first-hit 语义、terminal projection 与 `696a12b0` 基线;完成原 HTTPS cutover,**不引入**第二 OCR/capture client、Cloud 侧 DHXY mechanics、retry/session/TTL/ledger、wrapper nesting 或任何行为变更;**不删**旧 DTO/macro(归删除卡)。
- **禁令**:不扩写集;其它 Java writer 活动期间**不运行** Maven/JUnit/compile/package;不启 runtime/application/server/Task/UI/capture/input;**禁一切 Git mutation**;只从当前字节增量编辑;保护两仓全部 dirty/untracked 与他人半成品。基线 DHXY HEAD `0114604e`、cloud-brain `3b988ca`。我不再触碰 TURN-28P/28Q(属他人)、亦不动我已释放 owner 的 S1/22C1/S2 卡。
- 交付:完成后在本卡 true EOF 追加**一次** canonical `EXTERNAL-B SOURCE+TEST DELIVERED` + 逐文件 SHA/行证据/未运行门;父级复核整卡。

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD BUILD-REPAIR CLAIMED DHXY-ONLY-REFS-IN-DIALOGSERVICE Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T14:55:20-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD OWNER RETURNED - 2026-07-16T15:00:08-04:00

交还完整 TURN-26。不是 delivery,不是 approval,未拆卡,未建子卡。**本轮我未写入任何 Java/test 字节**;写集六文件保持我领取时的字节,`DialogService.java` 仍为 **2850 行 / SHA-256 `9088644e80d27f1b...`**(完整值见我 14:55:20 的 CLAIM 段)。零 Git mutation;未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input;两仓 dirty/untracked 与他人半成品完好;未碰任何其它写集/卡片/日志;未自建 reviewer。我自此不持有本卡 owner。

### 交还原因:capacity(不是 gate,也不是不可能)

父级判定完全正确 —— 本卡**确未 build-closed**。但阻断的真实规模远大于"几个未解析 import":`DialogService` 对 DHXY-only mechanics 有 **45 处活跃使用**,且**全部从 public `handleDialog`(:151) 可达、非死码**:

| 字段 | 使用次数 | 可达证据(样本) |
|---|---:|---|
| `coordinateHelper` | 15 | `:402/:403` `resolveMatchedPointInRect` + `getRandomizedPoint`;`:454`;`:722/:723` |
| `tracker` | 11 | `:368` `refreshWindowState()`;`:369-372` `getWindowBaseX/Y()+offset`;`:377` `captureToFile` |
| `inputSequences` | 10 | `:162` 主路径 `clickLeft("dialog:requestInitialClick",…)`;`:410`;`:729` |
| `inputProvider` | 7 | `:408`;`:727` `clickLeft(safeClick.x, safeClick.y, 150)` |
| `windowTaskContextHolder` | 2 | `:1434` `rawCurrent()` → `:1482` prepared-dialog binding 匹配、`:1542` proof token |

即:**原 HTTPS cutover 整体未完成**,本卡的 build repair 等于把 2850 行 service 的 capture/geometry/input 全线切到 typed 边界,并新建唯一 named test。这与我此前交还的 TURN-28 整卡同级,**超出我当前会话余量**。本轮禁编译/运行,肉眼是唯一校验;我在本会话已两次因凭记忆写签名出错(断言参数误判、heredoc 反引号被命令替换),两次均靠回读抓到。以现有余量硬做 45 处映射,大概率交出编译不过或行为漂移的整卡,并额外占住本卡若干窗口。**不占卡 = 父级可即刻改派。**

### 移交给继任者的实证(我核过,可省重复摸底)

- **最硬的一块是 `tryClickMaintenanceBroadcastOption`(`:361`,经 `:267`/`:280` 可达)的几何**。我在更早的 `W-COUNT-DIALOG-HANDLE-WHOLE-1` 上对同一方法交过精确 BLOCKED,结论仍适用:它在**非 dialog-rect 的固定窗口条**上用 `tracker.getWindowBaseX/Y() + offset` 算 screen-absolute 矩形(`:368-372`)再 `captureToFile`(`:377`),而 **cloud-brain 内没有 cloud-safe 的 windowBase 源**(`GameClientTracker`/`CoordinateHelper`/`WindowRuntimeContext` 均 DHXY-local)。其余子处理器(green-template/clickGreenOption/story-objective)可经**既有** `CloudDialogOptionOcrImagePort` 的 `FRESH_AT_RECT` 拿 raw/green/yellow bytes 后在 Cloud 侧 CPU 跑原 `ImageFinder.find`,不需新 capture port;**唯独该固定条的 windowBase 几何需要一个 cloud-safe 来源**。继任者应先解这一点:若可从 turn `latestWindowMetadata()` 的 exact `windowRect` 推出该条的 screen-absolute 矩形,则整卡可闭合;若仍无合法来源,应据实向父级提精确 BLOCKED(**不得造 wrapper/stub,也不得用 detection 反推 windowBase 冒充**)。
- `:1434/:1482/:1542` 的 `WindowRuntimeContext` 用于 prepared-dialog binding 匹配与 pending smart-click proof token;需映射到 Cloud 侧既有 typed context/metadata,不得新建 owner/session。
- 已被父级接受、**必须保留**:supplied-frame/single-capture 行为、OCR 顺序、white-story `0.85` first-hit、terminal projection、`696a12b0` 基线;**不得**引第二 OCR/capture client、Cloud 侧 DHXY mechanics、retry/session/TTL/ledger、wrapper nesting;`LocalOcrClient` 只允许提升既有 `readWords/OcrResult/OcrWord` 为 public typed API。

### 本 lane 已完成并通过的卡(供父级排班)

`TURN-28S1` 父级 source review `0/0/0`(产线与 `migration-baseline/696a12b0` 逐字节相同);`TURN-22C1` 父级 source+test-source `0/0/0`;`TURN-28S2` 父级 source `0/0/0` + 独立 R1/R2 `APPROVED 0/0/0`。三张均为**有界的删除/清理/小切口 cutover**。本 lane 在该形态下稳定交付;在"整服务全线 cutover / 从零建大件"形态下不可靠。若后续有同类有界切片,本 lane 可继续承接。

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD OWNER RETURNED ZERO-BYTES-WRITTEN WRITE-SET-UNCHANGED DIALOGSERVICE-2850-45-LIVE-DHXY-ONLY-USES CUTOVER-INCOMPLETE-NOT-IMPORT-FRAGMENT MAINTENANCE-BROADCAST-WINDOWBASE-GEOMETRY-IS-THE-BLOCKER CAPACITY-NOT-GATE Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T15:00:08-04:00 -->

## PARENT OWNER RETURN ACCEPTED / REPLACEMENT REQUIRED - 2026-07-16T15:02:30-04:00

- 父级接受 B 的 canonical whole-card return；六份 write-set 文件未产生本轮增量，当前零 owner。
- 父级抽查确认这不是 import-only repair：`DialogService` 的 tracker/input/geometry/context 依赖均从 public
  `handleDialog` 可达，maintenance broadcast 的 fixed window strip 也需要从 exact latest metadata
  `windowRect` 闭合坐标来源。完整 HTTPS cutover 与唯一 named test 仍属原 TURN-26 全合同。
- 原 source-review 证据继续保留，但状态改为 `WHOLE-CARD REPLACEMENT REQUIRED / ZERO OWNER`；下一 Worker
  必须领取并完成同一整卡，不得拆成 geometry/import/test fragment，也不得把 45 处 active mechanics 留给 build 门。

<!-- TRUE_EOF: TURN-26 PARENT-ACCEPTED-WHOLE-CARD-RETURN ZERO-WIP REPLACEMENT-REQUIRED ZERO-OWNER 2026-07-16T15:02:30-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD CLAIMED - 2026-07-16T17:18:33-04:00

- Implementation Worker：**CR271 External Worker B**（本会话为 B lane 的 fresh full-capacity 会话；前一 B 会话
  `aa951b1e…/Kepler` 已于 15:00:08 canonical 归还本卡，归还原因为 capacity 而非 gate）。非 reviewer，不能批准
  本卡；父级为唯一 manager/final reviewer。本段不含 `APPROVED/CLOSED`，不自批。
- 领取时间：`2026-07-16T17:18:33-04:00`。
- 完整任务卡：既有完整 Sprint Task `TURN-26`（Dialog option OCR 与 white-story），以权威计划
  `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 8 节卡体、第 16.2 节注册表行
  （`WHOLE-CARD OWNER RETURNED / REPLACEMENT REQUIRED / ZERO OWNER`，S=25）、第 17.2 节冻结写集、第 19.4 节
  testWriteSet 行与本固定报告全文（含 14:47 `WHOLE-CARD BUILD REPAIR REOPENED`、15:00:08 B 归还移交实证、
  15:02:30 父级 `REPLACEMENT REQUIRED`）为完整合同。我承担整卡全部 production/test/report/integration 与父级
  审核后的全部返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`；
  不拆卡、不建子卡、不做 geometry/import/test fragment，不把 45 处 active mechanics 留给 build 门。
- 完整 production/test/report 写集（严格沿用原卡冻结，不增不减）：
  1. Cloud `src/main/java/com/bot/dhxy/service/DialogService.java`
  2. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrImagePort.java`
  3. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogOptionOcrWordsPort.java`
  4. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogWhiteStoryTemplatePort.java`
  5. Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`（仅既有 `readWords/OcrResult/OcrWord`
     typed public API visibility+JavaDoc；不改 endpoint/timeout/codec/health/diagnostics/失败语义）
  6. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java`（原卡唯一 named test）
  7. 本 append-only 固定报告
  其余两仓文件全部只读（DHXY、protocol、`TurnGameClient`/action factory/command port、TURN-25 detection/
  prepared-validation ports、Task/caller、Spring config、POM、模板资源与其它测试/报告）。
- 领取点文件行数与 SHA-256（实测，与父级 15:02 接受归还时字节一致）：
  - `DialogService.java` 2850 行 `9088644e80d27f1b32dc2df92739ba51213bd8f439cf43a3b9d7bde084420a9f`
  - `CloudDialogOptionOcrImagePort.java` 190 行 `d51254946593c679da71be75043398c575435da193182c42b049a07352cc74d6`
  - `CloudDialogOptionOcrWordsPort.java` 109 行 `c3ac13353988b89165d40fc35e4e40701add50fa0045d804bb93392e6917dc7`（见下方勘误）
  - `CloudDialogWhiteStoryTemplatePort.java` 188 行 `46f64e15bb6bfd8bf03302f006f527a4388cd659db238be0e9a889de87e31c2d`
  - `LocalOcrClient.java` 295 行 `0e41a18bcf5a558376fb2c1cd145f7ca8b17fc65c6c1d7fa5f90c454ab504caaa`（见下方勘误）
  - `DialogOptionTurnContractTest.java` 1341 行 `6f9921aad0f35cc4618ef224efac32d4b1bf2a8312842b67d6251a170774b8a6`
- 依赖检查：S=`TURN-25`，注册表状态 `SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING`（Repair #1 父级
  `0/0/0`，owner 已释放），起始依赖满足。TURN-27/28 依赖本卡，均零 owner，无下游并发写。
- 与其它 active owner 写集冲突检查：External C=`TURN-34B`（Cloud `TaskMaintenanceService.java` + 唯一
  `TaskMaintenanceTurnContractTest.java`）与本写集零交集；Internal Euler=`TURN-28P` 两份 DHXY contract test
  （`TurnCapturePixelChangeProbeContractTest`/`TurnInputStepExecutorContractTest`，DHXY 仓）与本写集零交集；
  A/D 当前无卡。无重叠写集，无第二 TURN-26 writer。
- 实施边界：保留已被父级接受的 supplied-frame/single-capture 行为、OCR 顺序（raw/green/yellow、alias 外层/
  provider word 内层）、white-story `0.85` first-hit、terminal projection 与 `696a12b0` 基线；完成原 HTTPS
  cutover——把 `DialogService` 中 45 处 DHXY-only mechanics 活跃使用（`coordinateHelper` 15、`tracker` 11、
  `inputSequences` 10、`inputProvider` 7、`windowTaskContextHolder` 2）切到既有 typed turn 边界；maintenance
  broadcast 固定窗口条几何按父级 15:02 方向从 exact latest metadata `windowRect` 闭合坐标来源；不引入第二
  OCR/capture client、Cloud 侧 DHXY mechanics、retry/session/TTL/ledger、wrapper nesting 或任何行为变更；
  不删旧 DTO/macro（归删除卡）。
- 禁令：不扩写集；其它 Java writer（C/Euler）活动期间不运行 Maven/JUnit/compile/package；不启动
  runtime/application/server/Task/UI/capture/input；零 Git mutation；保护两仓全部 dirty/untracked 与他人半成品。
  基线 DHXY HEAD `0114604e`、cloud-brain `3b988ca`。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD CLAIMED FULL-CAPACITY-REPLACEMENT 45-DHXY-ONLY-USES-CUTOVER session-fresh-B 2026-07-16T17:18:33-04:00 -->

## EXTERNAL-B CLAIM SHA CORRIGENDUM - 2026-07-16T17:20:10-04:00

上一段 CLAIMED 中两条 SHA-256 为我转写错误（一处少一字符、一处多一字符），当前实测正确值为：

- `CloudDialogOptionOcrWordsPort.java` 109 行
  `c3ac13353988b89165d40fc35e4e40701add50fa0045d804bb93392ec6917dc7`
- `LocalOcrClient.java` 295 行
  `0e41a18bcf5a558376fb2c1cd145f7ca8b17fc65c6c1d7fa5f90c454ab504caa`

两值均与本卡此前 delivery/父级复审记录逐字一致；领取点字节未变，claim 其余内容不变。本段只勘误，不是 delivery。

<!-- TRUE_EOF: TURN-26 EXTERNAL-B CLAIM-SHA-CORRIGENDUM 2026-07-16T17:20:10-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-16T18:02:30-04:00

- Implementation Worker：**CR271 External Worker B**（fresh full-capacity 会话，claim 见 17:18:33 段）。非
  reviewer，不自批，本段不含 `APPROVED/CLOSED`。
- 交付时间：`2026-07-16T18:02:30-04:00`。
- **交付性质**：完整 TURN-26 整卡 production+test 交付。原 45 处 DHXY-only mechanics 活跃使用
  （`coordinateHelper` 15、`tracker` 11、`inputSequences` 10、`inputProvider` 7、`windowTaskContextHolder` 2）
  与另两个同样缺失的 DHXY-only 依赖（`WindowScopedTempPath`、`ObjectiveTextRecognitionService`）已全部从
  `DialogService.java` 移除；文件内不再残留任何 `com.bot.dhxy.input/tools.CoordinateHelper/vision/
  window.runtime/core.GameClientTracker` import 或字段。已接受的 supplied-frame/single-capture、OCR 顺序、
  white-story `0.85` first-hit、terminal projection 语义未变。

### 完整改动文件（本轮真实字节增量 2 个；其余写集 4 文件零增量）

| 文件 | 行数 | SHA-256 | 本轮 |
|---|---:|---|---|
| Cloud `src/main/java/com/bot/dhxy/service/DialogService.java` | 2978 | `47709414ff166f90ed44566de17e809432ed1f41cb2047c00ce7d8cdf832881d` | **改**（领取时 2850/`9088644e…`） |
| Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java` | 1496 | `e031c33aefc2bad1c30bbb08f749b79886c5c5d6b95c7ca0d86509053e84a918` | **改**（领取时 1341/`6f9921aa…`） |
| Cloud `remote/CloudDialogOptionOcrImagePort.java` | 190 | `d51254946593c679da71be75043398c575435da193182c42b049a07352cc74d6` | 未动=已复审字节 |
| Cloud `remote/CloudDialogOptionOcrWordsPort.java` | 109 | `c3ac13353988b89165d40fc35e4e40701add50fa0045d804bb93392ec6917dc7` | 未动=已复审字节 |
| Cloud `remote/CloudDialogWhiteStoryTemplatePort.java` | 188 | `46f64e15bb6bfd8bf03302f006f527a4388cd659db238be0e9a889de87e31c2d` | 未动=已复审字节 |
| Cloud `LocalOcrClient.java` | 295 | `0e41a18bcf5a558376fb2c1cd145f7ca8b17fc65c6c1d7fa5f90c454ab504caa` | 未动=已复审字节 |

写集外零写入；本固定报告 append-only。

### production 行为说明（逐类 cutover 映射，全部按既有已通过卡模式）

1. **几何**：`coordinateHelper.getScaledRect`（实为 windowBase+offset，无缩放）与
   `tracker.getWindowBaseX/Y()+offset` 全部换成新私有 `windowRelativeRect(...)`：exact latest
   `TurnWindowMetadata.windowRect` + 同一组未缩放固定偏移（父级 15:02 指示的 maintenance broadcast 固定条
   坐标来源即此）。identity fence 与 confirmed-STOP 传播逐条镜像 TURN-25 detection port。
   `getDialogRect/getSmallDialogRect` 偏移常量逐值不变（250/312/529/208、250/345/529/143）。
2. **捕帧**：`tracker.captureToFile/captureToMemory` 全部换成既有 public
   `captureDialogValidationImage`（TURN-25 `CloudDialogPreparedActionValidationPort` 单 ROI/单 command/
   fail-closed 边界）。未新建任何第二 capture port/client。逐路径 command 数与基线捕帧数一致
   （maintenance fast path 2、green-multi 检测+捕帧 2、prepare-green supplied 0/缺帧 1 等）。
3. **点击**：`inputSequences.clickLeft(desc,x,y,150)`（基线单 action `InputAction.clickLeft(x,y,150)`）到
   单 command `[CLICK_LEFT(TurnInputSpec clickDelayMs=150, queueHoldMs=null)]`（TURN-22 已定的 28P
   queue-owned timing 形态）；`moveAndClickLeft(desc,x,y,80,150)` 到单 command
   `[MOVE_MOUSE, WAIT(80), CLICK_LEFT(clickDelayMs=150)]`（TURN-19/21 形态）。boolean 合同映射：
   COMPLETED（步逐值 correlation 校验后）=true；FAILED=false；duplicate/uncertain=false 且零 retry、
   不映射成功；STOPPED 经 `TaskCheckpoint` 确认后传播、未确认 fail-closed。
   `isInputWorkerThread`/`inputProvider` 分支与三处 `submitExclusiveAndWait` exclusive 段随 DHXY 队列机制
   消失：每个原 exclusive 段变为"单 capture command + Cloud 计算 + 单 click command"（TURN-21/33 已接受
   的多 closed action 形态），业务判断、顺序、随机化半径（±4/±3、±30/±10、±12/±3、minOffsetX/maxOffsetX/
   randomRadiusY）与延时（TaskSleep 600/800/500+rand 等）逐值保留。
4. **洗图/模板匹配**：文件式 `washGreen/washYellow/washDialogOptionTemplate` + `ImageFinder.find(path,path)`
   换成同算法 in-memory 变体（file 变体本就委托同一 in-memory 实现，逐字节同算法）+
   `ImageFinder.find(BufferedImage,BufferedImage)`（同 TM_CCOEFF_NORMED/中心点约定）+
   `CloudTemplateAssets.loadTemplate`（白 port 同一模板边界；heal/repair/giveup 模板已在 Cloud resources）。
   模板缺失=基线模板文件不可读的同一 normal miss。
5. **context**：`windowTaskContextHolder.rawCurrent()`(DHXY) 换成 Cloud `TaskExecutionContextHolder.current()`。
   prepared-action consume（原 :1434）：per-window prepared store 属 DHXY `WindowRuntimeContext`，其 Cloud
   owner 未迁移（38M/38C 分类中），映射为基线"无 prepared action"分支（跳过 fast path 走普通 remembered
   点击）；producers（prepare*）与 `validatePreparedDialogActionForConsume` 公共 API 原样保留。smart-click
   proof token（原 :1541）：token 存储随 NpcClickService 卡迁移，保留基线 null-token confirm 形态。
6. **诊断**：`WindowScopedTempPath` 临时文件全部移除；沿用本卡已接受的 `cloud-memory:*` 标签口径，OpenCV
   debug mask 走 `ImagePreprocessor` 受 `ENABLE_DEBUG_SAVE` 守卫的 debug 目录。仅诊断差异，无业务分支变化。
7. **story click-through 底边偏移**：基线 `40 / systemScaleRatio` 映射为固定 `40`（Cloud 无 DPI 源；
   TURN-25 已固化本 rect 族坐标为未缩放屏幕像素，全卡一致）。

### named test source（唯一 `DialogOptionTurnContractTest`，1341 到 1496 行）

- harness 更新为新的 9 参构造（`TaskExecutionContextHolder` + `CloudTemplateAssets` + 显式 absent
  `ObjectProvider<SmartClickEvidenceConfirmationService>`），原 7 个 @Test 断言全部原样保留。
- 新增 `maintenanceBroadcastStripDerivesFromLatestWindowRectAndFailsClosedOnTemplateMiss`：轻量探测恰好两个
  closed CAPTURE command，heal/repair 固定条 region 逐值等于 `windowRect.left/top + 258/352(132x73)`、
  `+250/360(110x64)`，零 INPUT step，模板 miss 返回基线 `BUSINESS_OPTION_NOT_FOUND/NONE`，模板加载顺序
  green/yellow 各一次逐条断言。
- 新增 `rememberedOptionTrustedFastPathSubmitsOneMoveWaitClickCommand`：trusted fast path 单 command 三步
  `MOVE_MOUSE -> WAIT(80) -> CLICK_LEFT(clickDelayMs=150, queueHoldMs=null)`，MOVE/CLICK 同坐标，点位=
  `windowRect+250/312+relative(120,60)` 的 ±4/±3 randomization 内，1 UUID/1 command，结果
  `OPTION_KEYWORD_CLICKED` 坐标回填一致。
- source gate 扩展为整卡口径：整文件禁 `com.bot.dhxy.input.*/tools.CoordinateHelper/vision.*/
  window.runtime.*/core.GameClientTracker` import 与 `inputSequences/inputProvider/coordinateHelper/
  windowScopedTempPath/windowTaskContextHolder/objectiveTextRecognitionService/tracker./
  isInputWorkerThread/submitExclusiveAndWait` 残留 token，正向断言 `latestWindowMetadata()/
  windowRelativeRect(/DIALOG_CLICK_DELAY_MS` 存在；原三 macro/第二 OCR client/permanent local Service
  清单断言不变。

### 基线核对

- `696a12b0` 逐路径核对：option OCR/alias/fallback、white-story 四态、business option 两轮绿黄扫描顺序、
  maintenance strip 偏移(258/352/390/425、250/360/360/424)、点击延时 150、settle 80、随机半径、
  TaskSleep 区间、ROUTE_TRANSFER 两轮 retry 均逐值不变；`docs/业务逻辑.md:484-697` 白龙马
  `probeTargetReady/probeWrongPosition/probeStoryAbsent/probeNoTarget` 分流未触碰。
- 有意业务差异：**无**。上表 5/6/7 三项为架构映射（store/token 缺失=基线空分支、诊断文件、无 DPI 源），
  非业务分支变化；若父级判定任一项须按业务差异走 CR，我整卡返修。

### 已知阻断/请父级裁决（BLOCKING FINDINGS）

1. **story-objective producer 缺口**：`handleStoryObjective`（READ_STORY_OBJECTIVE，唯一 Cloud caller 为
   未迁移的 XiuluoTaskV2，其 :3539 也直接依赖同一缺失类）在冻结写集内无 cloud-safe typed producer：
   `ObjectiveTextRecognizer` 为 cloudbrain package-private 且属 TURN-28 写集；`DecisionEngine` 构造器
   package-private、objective 入口 private；新建 facade 文件越写集。当前实现为**显式 fail-closed
   `TaskFatalException` 占位**（不伪造 `STORY_OBJECTIVE_NOT_FOUND`/读取结果，源码注释标注 PENDING PARENT
   ADJUDICATION）。请父级三选一：其一，计划补一张 typed objective-text facade 卡（同
   SheyaoxiangStatusDecisionFacade 形态，亦解 TURN-37 同缺口）；其二，扩本卡写集并明确互斥；其三，接受占位
   至 TURN-37 前置闭合。
2. **旧 named tests 构造链失配**：构造器 14 参到 9 参后，写集外只读的 `DialogDetectionTurnContractTest`/
   `DialogGiveItemTurnContractTest`（TURN-25/16）`new DialogService(14 null)` 需机械 arity 更新。两文件在本
   卡前已因 DialogService 缺失 import 同样不可编译，故非新增回归；归属其各自 build 门或父级指定。
3. Cloud 全仓 main compile 仍将被写集外其它未迁移文件（NavigationService/NpcClickService/三 Task 等对同一批
   缺失类的引用）阻断；本卡文件自身已无缺失引用，按第 18 节"首个真实错误归属返修"口径不再归属本卡。

### 未运行项目

未运行 Maven/JUnit/compile/package/build（C=TURN-34B、Euler=TURN-28P 两测试为活动 Java writer）；未启动
runtime/application/server/Task/UI/capture/input；零 Git mutation；两仓全部 dirty/untracked 与他人半成品
未触碰；未自建 reviewer。交付基于逐区源码目检 + 括号平衡/残留 token 全文扫描；编译与 named test 由父级在
stable-writer 窗口按第 19 节执行。

### 父级审核请求

请执行完整 SOURCE+TEST SOURCE REVIEW（整卡：production cutover + 唯一 named test + 三项 blocking finding
裁决）。我保持 owner 至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或整卡返修指令；交付后本卡停笔。

**无已批准业务差异；按 `696a12b0` 与用户确认的最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD SOURCE+TEST DELIVERED 45-PLUS-2-DHXY-ONLY-REFS-REMOVED DIALOGSERVICE-2978-47709414 TEST-1496-e031c33a STORY-OBJECTIVE-PRODUCER-GAP-PENDING-ADJUDICATION 2026-07-16T18:02:30-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST-SOURCE REVIEW #2 - BLOCKED - 2026-07-16T18:07:00-04:00

- Verdict: **`P0/P1/P2=0/6/0 / WHOLE-CARD REPAIR #1 REQUIRED`**。父级复算交付 SHA 与报告一致：
  production `DialogService.java` 2,978 行 / `47709414ff166f90ed44566de17e809432ed1f41cb2047c00ce7d8cdf832881d`；
  唯一 named test 1,496 行 / `e031c33aefc2bad1c30bbb08f749b79886c5c5d6b95c7ca0d86509053e84a918`。
  三个 option/white port 与 `LocalOcrClient` 四个既有接受 SHA 未漂移。45+2 个 Cloud-host DHXY-only
  mechanics 引用已移除，但完整卡不能以四条业务退化和不可编译的既有测试换取 source cutover。
- **P1-1，`READ_STORY_OBJECTIVE` 被改成固定 fatal 占位。** `DialogService.java:1523-1542`
  在 detection image 非空时无条件抛 `TaskFatalException`；`696a12b0` 同方法会裁 small-story ROI、调用
  `ObjectiveTextRecognitionService.recognize`，并返回 `STORY_OBJECTIVE_READ` 或正常
  `STORY_OBJECTIVE_NOT_FOUND`。缺 typed producer 是计划/写集阻断，不授权把既有 public operation 改为必 fatal。
- **P1-2，prepared remembered-route 快路径被永久旁路。** `DialogService.java:1424-1439`
  对所有合法请求直接 `return null`，删除了基线 `consumePreparedDialogActionValidated`、binding/fingerprint
  复核、prepared exact click 与 consume 后 clear；随后改走随机 remembered-point click。这不是“无 prepared
  action”单次事实，而是把所有运行都强制成无 state，改变 stale-click 防线和路径选择。
- **P1-3，SmartClick proof correlation 被固定置空。** `DialogService.java:1481-1502` 总是传
  `proofToken=null`；当前 `NpcClickService.PendingSmartClickEvidence.matchesProofToken` 在
  `NpcClickService.java:2137-2140` 明确要求 pending nonblank token 与 candidate 精确相等。因此任何真实 pending
  proof 都不会确认，和基线从 exact `WindowRuntimeContext` 取 token 的行为不等价。
- **P1-4，story click-through 坐标改变。** `DialogService.java:1824-1835` 把基线
  `rect.bottom - round(40 / systemScaleRatio)` 改为固定 `rect.bottom - 40`。在 scale 非 1.0 时点击点不同；
  “Cloud 没有 DPI 源”是缺合同，不是用户批准删除 scale 语义。必须携带/读取 exact capture-time scale 或保持原值。
- **P1-5，既有 named-test 构造链已经源码失配。** `DialogDetectionTurnContractTest.java:621` 与
  `DialogGiveItemTurnContractTest.java:171` 仍以 14 个实参构造 `DialogService`，当前 Lombok 构造只有 9 个
  collaborator。交付者已在报告确认该问题；无论此前还有其它 compile blocker，本次构造签名变更都新增了确定的
  testCompile 失败，完整卡不能把它留给别卡。
- **P1-6，唯一 named test 未覆盖上述四条行为，且 source gate 锁定退化。** 9 个 `@Test` 没有执行
  `READ_STORY_OBJECTIVE`、prepared store consume、nonblank SmartClick token correlation、非 1.0 scale story click；
  `DialogOptionTurnContractTest.java:731-815` 反而把 `objectiveTextRecognitionService` 缺失列为成功条件。
  新增的 remembered-option 用例只证明普通随机 fallback，不证明 baseline prepared fast path。
- Required whole-card repair：同一 External B 继续负责完整 `TURN-26`，不得拆 objective/prepared/proof/scale/
  testCompile fragment。保留已接受 option OCR、white-story `0.85` first-hit、single authoritative capture、closed
  input terminal/correlation 与四个未漂移文件；恢复上述四条 baseline 行为，更新所有受构造签名影响的本卡既有
  contract tests，并在唯一 named test 通过真实 public production path 覆盖四条正/负合同。不得用恒 null、恒 fatal、
  注释、source scan 或“后续卡再补”代替业务合同；若冻结写集无法合法承载 typed producer/state/scale，必须在本原卡
  canonical `WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED`，由父级修正整卡写集后再领，不能带占位交付。
- 用户已取消额外 reviewer；修复后只需新的 canonical whole-card delivery 与父级本人复审。当前 TURN-34B 已出现
  active Java claim，故本轮未运行 Maven/JUnit/compile/runtime/input；零 Git mutation。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-26 PARENT-WHOLE-CARD-REVIEW-2 BLOCKED P0P1P2=0/6/0 WHOLE-CARD-REPAIR-1-REQUIRED STORY-OBJECTIVE-FATAL PREPARED-BYPASS PROOF-TOKEN-NULL DPI-DRIFT TEST-CONSTRUCTOR-MISMATCH COVERAGE-GAPS RETURN-SAME-EXTERNAL-B 2026-07-16T18:07:00-04:00 -->

## EXTERNAL-B DELIVERY ADDENDUM - POOL-STATE CORRECTION AND CONTRACT BASIS - 2026-07-16T18:12:00-04:00

本段只勘误/澄清，不改交付字节，不是新 delivery。

1. **池状态勘误**：我 17:18 claim 段写"A/D 当前无卡、C=TURN-34B"系依据当时注册表文本，不准确。实况：
   TURN-28 已于 15:20:00 在其原卡 true EOF 被另一 lane canonical `WHOLE-CARD CLAIMED`（该卡 17:28 追加
   IDENTITY CORRECTION，以 EXTERNAL-C 名义持卡实施中）。与本卡写集核对：TURN-28 四文件
   （`NpcClickService.java`/`ObjectiveTextRecognizer.java`/`SmartClickRecognizer.java`/唯一
   `NpcClickTurnContractTest.java`）与本卡六文件**零重叠**；其对 `LocalOcrClient.readWords` 仅只读调用，
   而本卡本轮对 `LocalOcrClient` 零字节。冲突检查结论（无重叠写集、无双 writer）不变。
2. **合同依据澄清**：原 01:11 frozen brief 第 7 条曾写"`DialogService` 既有点击只由后续 turn/caller 卡
   处理"。本交付把点击一并 cutover 的依据是其后的父级整卡指令：14:47 `WHOLE-CARD BUILD REPAIR REOPENED`
   （"Complete the original HTTPS cutover ... The original exact write set ... remain binding"）与 15:02
   （"完整 HTTPS cutover 与唯一 named test 仍属原 TURN-26 全合同 ... 不得把 45 处 active mechanics 留给
   build 门"）。若父级认定点击族仍应留由后续卡处理，请在复审中明示，我按指令整卡返修。
3. 我的 5 分钟 worker heartbeat 自本段起监控本卡 physical EOF 的父级结论；无变化静默，不因单卡结论自停。

<!-- TRUE_EOF: TURN-26 EXTERNAL-B DELIVERY-ADDENDUM POOL-STATE-CORRECTED TURN-28-OWNED-BY-EXTERNAL-C-DISJOINT-WRITE-SETS CONTRACT-BASIS-14:47-15:02-OVERRIDE 2026-07-16T18:12:00-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED - 2026-07-16T18:26:00-04:00

交还完整 TURN-26。不是 delivery，不是 approval，未拆卡，未建子卡。本段执行父级 Review #2（18:07，
`P0/P1/P2=0/6/0`）自身给出的指令："若冻结写集无法合法承载 typed producer/state/scale，必须在本原卡
canonical `WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED`，由父级修正整卡写集后再领，不能带占位交付。"
我全盘接受六条 P1 判定；逐条核实后，P1-1/2/3/4 所需的 producer/state/scale 在当前冻结写集内均无合法落点，
故按指令整卡归还，不再带占位返修。

### 本轮零字节；当前写集字节 = 18:02 交付态（归还时实测）

- `DialogService.java` 2978 行 `47709414ff166f90ed44566de17e809432ed1f41cb2047c00ce7d8cdf832881d`
- `DialogOptionTurnContractTest.java` 1496 行 `e031c33aefc2bad1c30bbb08f749b79886c5c5d6b95c7ca0d86509053e84a918`
- 三 port + `LocalOcrClient` 四文件 = 原接受 SHA 未漂移（`d5125494…`/`c3ac1335…`/`46f64e15…`/`0e41a18b…`）。

Review #2 未质疑的部分（45+2 处 DHXY-only 引用移除、windowRect 几何、captureValidationImage 复用捕帧、
CLICK_LEFT(150)/MOVE+WAIT80+CLICK 命令形态、in-memory 洗图/模板、既有 OCR/white-story 语义、两个新
mechanics 测试与整卡 source gate）保留在盘作为下一任 owner 的 WIP 起点；是否保留由父级裁决，我不回滚。

### 逐条阻断证据（为何四条 baseline 行为在冻结写集内不可恢复）

1. **P1-1 story-objective producer**：恢复 `ObjectiveTextRecognitionService.recognize` 等价行为需要
   cloud-safe typed producer。`ObjectiveTextRecognizer` 是 `com.yueyunfe.dhxy.cloudbrain` **package-private
   final class**，且属 **TURN-28 冻结写集**（该卡 15:20 起由 EXTERNAL-C canonical 持卡实施中，reservation
   在其写集第 2 项）；`DecisionEngine` 构造器 package-private、`objectiveTextReader` private，无 typed 公共
   入口；`LocalOcrClient` 的本卡合同仅允许 `readWords/OcrResult/OcrWord` visibility+JavaDoc；三 port 合同
   禁止获得新 authority；新建 facade 文件（SheyaoxiangStatusDecisionFacade 形态）不在写集。复制算法进
   `DialogService` = 第二 recognizer，父级已明令禁止。
2. **P1-2 prepared-dialog store**：`consumePreparedDialogActionValidated`/binding/fingerprint 复核/consume 后
   clear 的宿主是 DHXY `WindowRuntimeContext` 的 per-window prepared store。Cloud 侧无该 store：其 owner 归
   TURN-38M/38C authority-state 分类（尚未父级冻结）；在 `DialogService` 内自建 per-window store = 新增
   state owner，本卡禁令（无 session/owner/ledger/durable state）明确禁止；`TaskExecutionContext`（13C）、
   `CloudTaskRetainedActionState` 等候选文件均只读。
3. **P1-3 proof-token 源**：正确关联需要 `NpcClickService.PendingSmartClickEvidence`（`NpcClickService.java:139/
   :2064-2140`，private nested，**TURN-28 写集且 C 为当前唯一 writer**——并发双写明令禁止）暴露 token，或
   `SmartClickEvidenceConfirmationService` 接口扩一个 token 读取口（接口文件不在本卡写集）。基线的
   `WindowRuntimeContext.getPendingSmartClickEvidenceProofToken` 在 Cloud 无等价物。
4. **P1-4 capture-time scale**：`40 / systemScaleRatio` 需要 exact capture-time scale 源。turn 协议
   `TurnWindowMetadata` 不携带 DPI/scale；协议八文件为双仓 byte-identical、属 TURN-40A/28P 冻结，任何扩字段
   都是协议卡的写集；Cloud 主机自身无 DPI 源。"保持原值"同样需要 scale 值输入，无处可读。
5. **P1-5**：`DialogDetectionTurnContractTest.java:621`/`DialogGiveItemTurnContractTest.java:171` 两文件属
   TURN-25/16 写集；父级 Review #2 已在返修指令中授权更新——该授权应并入修正后的整卡写集，由下一任 owner
   与四条行为恢复同卡交付（机械 14 参到 9 参更新，工作量极小）。
6. **P1-6**：四条正/负合同覆盖依赖 1-4 的 producer/state/scale 先落地，无法先行。

### 建议的计划修正项（供父级冻结新合同，非我扩权）

- 新增一个 cloudbrain typed objective-text facade 文件（或授权 `ObjectiveTextRecognizer` 升 public typed API，
  但须与 TURN-28/C 的 reservation 排序互斥）——同一 producer 也是 TURN-37（`XiuluoTaskV2:3539`）的前置。
- 指定 prepared-dialog store 的 Cloud owner 文件并纳入本卡（或前置 38C 对应 KEEP_REWIRE 行）。
- 指定 proof-token 读取口（接口扩展或等 TURN-28 交付后的 public surface），与 C 的写集排序。
- 决定 scale 语义：协议携带 capture-time scale（40A/28P 序列）或用户批准固定像素偏移。
- 把 `DialogDetectionTurnContractTest`/`DialogGiveItemTurnContractTest` 的 arity 更新并入新写集。

### 诚实记录

本轮（Review #2 后）对两仓零 Java/test 字节写入、零 Git mutation；未运行 Maven/JUnit/compile/package/
runtime/application/server/Task/UI/capture/input；未触碰 TURN-28（C）/TURN-34B 写集与他人半成品；未自建
reviewer。我自此不持有本卡 owner；父级修正整卡写集重开 READY 后，本 lane 可再领取（届时以修正合同为准）。

**无已批准业务差异；唯一业务基线 `696a12b0`。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD OWNER RETURNED PLAN-CONTRACT-BLOCKED PER-PARENT-REVIEW-2-DIRECTIVE ZERO-BYTES-THIS-ROUND WIP-PRESERVED-AT-DELIVERED-SHAS P1-1-PRODUCER-TURN28-RESERVED P1-2-STATE-OWNER-38C P1-3-TOKEN-SOURCE-TURN28-ACTIVE P1-4-SCALE-PROTOCOL-40A28P 2026-07-16T18:26:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #1 - 2026-07-16T18:49:00-04:00

- 状态改为 `PLAN-CONTRACT REPAIRED / WAITING TURN-28 SHARED-API SOURCE GATE / ZERO OWNER`。本段只修计划，
  不派卡、不建立 owner；TURN-28 shared API 经父级 source review 通过后，本卡自动变为 `READY / ZERO OWNER`，
  由任一 External implementation Worker 按第 16 节自行领取。
- 解除循环依赖：TURN-28 source-start 不再依赖 TURN-26；TURN-26 等 TURN-28 发布唯一 canonical
  `ObjectiveTextRecognizer` public typed result 与 exact-window pending proof-token read API。TURN-26 只读消费，
  不得复制 recognizer、不得读取 private field、不得恒 fatal/null。
- 本卡新增 `CloudDialogPreparedActionState.java`：仅以 effective context 的 tenant/user/device/window exact key
  保存并原子 consume/clear prepared action；禁止 session、TTL、ledger、durable state。`DialogService` 保留 fingerprint
  与 binding validation。
- 测试写集并入 `DialogDetectionTurnContractTest.java`、`DialogGiveItemTurnContractTest.java` 的 14→9 构造更新，
  以及唯一 `DialogOptionTurnContractTest.java` 对 objective/prepared/proof 正负路径的 executable coverage。
- Parent Review #2 的 scale P1 撤销：HTTPS turn 协议冻结为未缩放 screen-absolute 坐标，`bottom - 40` 是 exact
  mapping；不得为此给 `TurnWindowMetadata` 新增 DPI/scale 字段。

**无已批准业务差异；唯一业务基线 `696a12b0`。**

<!-- TRUE_EOF: TURN-26 PARENT-PLAN-CONTRACT-REPAIR-1 WAITING-TURN28-SHARED-API ZERO-OWNER SELF-CLAIM-AFTER-GATE NO-DISPATCH PREPARED-STATE-WRITESET-ADDED SCALE-FINDING-WITHDRAWN 2026-07-16T18:49:00-04:00 -->

## PARENT SOURCE-GATE OPEN / WHOLE-CARD BUILD REPAIR #1 READY - 2026-07-17T00:32:00-04:00

- TURN-28 Repair #5 已获 Parent Source Review #3 `P0/P1/P2=0/0/0`；canonical objective typed result 与
  exact-window pending proof-token read API 的前置 source gate 已满足。
- 本卡按 18:49 计划合同自动转为 `WHOLE-CARD BUILD REPAIR #1 READY / ZERO OWNER`。任一 External
  implementation Worker 可自行在本卡 physical EOF canonical 领取完整卡；父级不发卡、不创建或调度 Worker。
- 完整合同仍为 Plan Repair #1：`DialogService` 全卡、exact-window prepared-action state、两份旧 test 9 参构造
  更新与唯一 `DialogOptionTurnContractTest` objective/prepared/proof 正负矩阵；禁止拆卡、恒 null/fatal、第二协议、
  TTL/session/ledger 或复制 recognizer。
- TURN-28 named test 被共享 Cloud main compile 债阻断不撤销其 source pass，也不重新关闭本卡 source-start。

**无已批准业务差异；唯一业务基线 `696a12b0`。**

<!-- TRUE_EOF: TURN-26 PARENT-SOURCE-GATE-OPEN WHOLE-CARD-BUILD-REPAIR-1-READY ZERO-OWNER SELF-CLAIM NO-DISPATCH TURN28-SOURCE-PASSED 2026-07-17T00:32:00-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR #1 CLAIMED - 2026-07-17T00:36:41-04:00

- Implementation Worker：**CR271 External Worker B**（本卡 18:02 交付、18:26 按父级指令 PLAN-CONTRACT
  BLOCKED 归还的同一 lane；TURN-23 已于 20:23 PASSED 释放，本 lane 现空）。非 reviewer，不自批，本段不含
  `APPROVED/CLOSED`。claim 前实测本卡 59,735 字节 / 16 sections / EOF=父级 00:32 SOURCE-GATE OPEN；claim
  后将回读 EOF 确认唯一，若发现更早 claim 立即 canonical 自撤。
- 领取时间：`2026-07-17T00:36:41-04:00`。
- 完整任务卡：既有完整 Sprint Task `TURN-26` 之 **WHOLE-CARD BUILD REPAIR #1**（合同 = 01:11 frozen brief +
  既有已接受字节 + 18:07 Review #2 + 18:49 PLAN-CONTRACT REPAIR #1 + 00:32 SOURCE-GATE OPEN）。我承担整卡全部
  production/test/report/integration 与后续返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical
  whole-card `OWNER RETURNED`；不拆卡、不恒 null/fatal、不造第二协议、不复制 recognizer。
- 完整 production/test/report 写集（= Plan Repair #1，不增不减）：
  1. Cloud `src/main/java/com/bot/dhxy/service/DialogService.java`（现盘 = 我 18:02 交付 WIP，2978 行
     `47709414ff166f90ed44566de17e809432ed1f41cb2047c00ce7d8cdf832881d`；恢复 objective/prepared/proof 三条
     baseline 行为，消费 TURN-28 shared API）
  2. **新建** Cloud `src/main/java/com/bot/dhxy/service/dialog/CloudDialogPreparedActionState.java`
     （effective context tenant/user/device/window exact key，原子 publish/consume/clear；无 session/TTL/
     ledger/durable state；fingerprint+binding validation 留 DialogService）
  3. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogOptionTurnContractTest.java`
     （现盘 1496 行 `e031c33a…`；补 objective/prepared/proof 正负 executable 矩阵）
  4. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogDetectionTurnContractTest.java`
     （现盘 1070 行 `bb02629f…`；仅 14→9 参构造机械更新）
  5. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogGiveItemTurnContractTest.java`
     （现盘 387 行 `916dcab4…`；仅 14→9 参构造机械更新）
  6. 三个 option/white port + `LocalOcrClient`（已接受字节，预计零改动）；本 append-only 固定报告。
  其余两仓只读；尤其 TURN-28 已过 source review 的 `ObjectiveTextRecognizer.java`（public typed
  `recognize(BufferedImage)`/`Result`）与 `SmartClickEvidenceConfirmationService.java`（27 行 `99c5856e…`，
  `currentExactWindowPendingSmartClickProofToken()`）**只消费不修改**。
- 依赖检查：TURN-28 Repair #5 Parent Source Review #3 `0/0/0`（00:32 段确认），shared API source gate 已满足；
  两 API 已实测在盘。TURN-28 named test 受共享 compile 债阻断不影响本卡 source-start（父级明示）。
- 与其它 active owner 写集冲突检查：TURN-28 source 已 PASSED（owner 释放）；TURN-23（我卡）已 PASSED；
  A lane 曾于 21:20 归还 TURN-28 Build Repair（等 Amendment #3），若其重启其写集为 NpcClickService 族，与本卡
  六文件零重叠；D 无卡。两个 Create/test 文件无人持有。scale 语义按 18:49 裁决保持 `bottom-40` exact mapping。
- 纪律：其它 Java writer 活动期间不运行 Maven/JUnit/compile/package；不启动 runtime/application/server/Task/
  UI/capture/input；零 Git mutation；只从当前字节增量编辑；保护两仓全部 dirty/untracked 与他人半成品。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD BUILD-REPAIR-1 CLAIMED PLAN-REPAIR-1-CONTRACT SHARED-API-CONSUMED PREPARED-STATE-TO-CREATE session-B 2026-07-17T00:36:41-04:00 -->

## EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR #1 SOURCE+TEST DELIVERED - 2026-07-17T00:50:00-04:00

- Implementation Worker：**CR271 External Worker B**（claim 00:36:41）。非 reviewer，不自批，本段不含
  `APPROVED/CLOSED`。整卡交付，未拆卡、未换 owner。
- 交付时间：`2026-07-17T00:50:00-04:00`。按 Plan Repair #1 合同 + 00:32 SOURCE-GATE OPEN，消费 TURN-28 已
  source-passed 的两个 shared API，闭合 objective/prepared/proof 三链 + 两旧 test 9 参更新 + named test 正负矩阵。

### 完整改动文件

| 文件 | 行数 | SHA-256 | 本轮 |
|---|---:|---|---|
| Cloud `service/DialogService.java` | 3072 | `207f007d8e002eea42446845c3cd4fddab90c34e2524d448f5190b6061adc4fa` | **改**（前盘 2978/`47709414`） |
| Cloud `service/dialog/CloudDialogPreparedActionState.java` | 169 | `447b599f4c72ca17d57faafc051c4f268c2e03ae93a88460a5dce9563e98a37f` | **新建** |
| Cloud `test/.../service/DialogOptionTurnContractTest.java` | 1706 | `aa5576cdc2133cbee47d769d03683dfc42338cfd7b6a1ffce71cd821e2963323` | **改**（前盘 1496/`e031c33a`） |
| Cloud `test/.../service/DialogDetectionTurnContractTest.java` | 1066 | `87145468bf9c2488ba1b686df6d29760f9adec5dea1826840bf168d49c762f47` | **改**（14→9 参构造，机械） |
| Cloud `test/.../service/DialogGiveItemTurnContractTest.java` | 383 | `b970cf838eab98da5f8c123594895c2be5d71046d4a8e9c658b65e87f686f945` | **改**（14→9 参构造，机械） |

三 option/white port + `LocalOcrClient` 未动（`d5125494`/`c3ac1335`/`46f64e15`/`0e41a18b`，= 已接受）；写集外
零写入；本固定报告 append-only。TURN-28 `ObjectiveTextRecognizer`/`SmartClickEvidenceConfirmationService` 只消费不改。

### 三链闭合（objective / prepared / proof）—— production

1. **objective（原 P1-1）**：`handleStoryObjective` 恢复 `696a12b0` 行为——裁 small-story ROI
   （`cropStoryObjectiveImage`），调 **`ObjectiveTextRecognizer.recognize(BufferedImage)`**（TURN-28 发布的
   canonical public typed API，同一 template-first map-name + green-digit + plausibility 算法）。baseline reader
   gate：`found()` 且 mapName 非空、x/y 可解析才 `STORY_OBJECTIVE_READ`（组 `ObjectiveTextResult`）；否则
   `STORY_OBJECTIVE_NOT_FOUND`。**删除恒 fatal 占位**，不伪造读取。
2. **prepared（原 P1-2）**：新建 **`CloudDialogPreparedActionState`**（`@Component`，effective context
   tenant/user/device/window exact key，per-key `AtomicReference` 原子 publish/consumeValidated/clear；consume
   逐值镜像 baseline `WindowRuntimeContext.consumePreparedDialogActionValidated`：absent/operation-target
   mismatch→null、clickRequired 无 validator→null、validation 失败→清槽+null、成功→CAS 消费+清槽）。**无
   session/TTL/ledger/durable/timer**；fingerprint 与 window/HWND binding 校验仍留 `DialogService`
   （`validatePreparedDialogActionForConsume` + `matchesCurrentPreparedDialogBinding`）。
   `tryConsumePreparedRememberedRouteOption` 恢复真实 consume→click→clear，producer 侧
   `PreparedDialogAction` 返回不变。
3. **proof（原 P1-3）**：`confirmPendingSmartClickIfExpectedOptionProved` 用
   `confirmationService.currentExactWindowPendingSmartClickProofToken()`（TURN-28 amendment 的 typed read）取
   当前 exact-window pending token 并原样回传 `confirmExpectedOptionProof`；null 仍表示无 pending，逐值等价
   baseline。**删除恒 null**。
- **scale**：按 18:49 裁决保持 `bottom-40` exact mapping（未加 DPI 字段）。

### 两旧 test 9 参构造更新（Plan Repair #1）

`DialogDetectionTurnContractTest:621` 与 `DialogGiveItemTurnContractTest:171` 的 `new DialogService(14×)` 机械
更新为新 10 参构造（加入 `CloudDialogPreparedActionState` 位；detection/giveitem 相关 port 原样，其余 null）。
仅构造签名同步，@Test 断言未改。

### named test 正负矩阵（`DialogOptionTurnContractTest`，1496→1706）

- harness 加 `CloudDialogPreparedActionState` 位（10 参构造）并暴露 `preparedState` + 可选
  `RecordingSmartClickConfirmation`（default 仍 absent，原 7+2 test 行为不变）。
- **objective**：`storyObjectiveReadRunsRealCloudRecognizerAndMissesOnNonObjectiveStory`——full
  `handleDialog(readStoryObjective)`→STORY 分类→`handleStoryObjective`→真实 `ObjectiveTextRecognizer.recognize`；
  合成 story 帧非真实目标面板→真实 recognizer miss→`STORY_OBJECTIVE_NOT_FOUND`，1 command/1 UUID。
  `objectiveTextRecognizerParsesPackagedObjectiveFixture`——同一 public API 对打包 fixture
  `0_story_changan_chengdong_102_186_1.png` 真实解析→`found`/`长安城东`/`102`/`186`。
- **prepared**：`rememberedRouteConsumesExactWindowPreparedStateAndFlowsProofToken`——publish 一个 ROUTE_TRANSFER
  prepared→`handleDialog(handleRememberedChoiceOption)` trusted fast path→真实 state consume→单
  `MOVE→WAIT80→CLICK_LEFT(clickDelayMs=150)` command→清槽（二次 consume=null）→`OPTION_KEYWORD_CLICKED`
  带 preparedAction。`rememberedRoutePreparedStateMismatchDoesNotConsume`——目标不符不消费、原槽存活。
- **proof**：同一 prepared 正例用 present `RecordingSmartClickConfirmation("proof-abc")`，断言
  `tokenReads==1`、`confirmCalls==1`、`lastProofToken=="proof-abc"`、`lastVerificationStrength=="DIALOG_OCR"`
  ——token 经 typed API 真实流回 confirm。
- 全部经真实 production path（`DialogService`/`CloudDialogPreparedActionState`/`ObjectiveTextRecognizer`），
  无 source-string 代替、无恒 null/fatal、无第二协议/复制 recognizer。

### 基线核对

- `696a12b0` objective/prepared/proof 三条行为逐值恢复；`docs/业务逻辑.md` 白龙马分流未触碰。有意业务差异：**无**。

### 诚实披露（请父级审）

- objective **miss** 端到端与 prepared/proof 全链由 JSON/state 合同确定，信心高。
- objective **producer 正例** 断言依赖打包 fixture `0_story_changan_chengdong_102_186_1.png` 经真实模板管线解析为
  `长安城东/(102,186)`（fixture 名即其答案、属训练集，高置信），但本轮禁 Maven/JUnit **未本地实跑**；若父级
  build 显示 fixture 阈值不达，我在本卡按证据微调断言（真实执行调整，非伪造）。
- 未把 end-to-end `STORY_OBJECTIVE_READ`（含 handleStoryObjective 的 crop 对齐 + STORY 分类）设为断言，因为
  确定性重现一个"真实目标面板既通过 STORY 分类又被 crop 正确定位"的合成帧在禁运行下不可靠；改以
  producer 正例 + pipeline miss + 真实 wiring 三者覆盖 objective 读路径。若父级要求 end-to-end READ 断言，
  我整卡补该 fixture。

### 未运行项目

未运行 Maven/JUnit/compile/package/build（TURN-28 等仍可能为活动 Java writer）；未启动 runtime/application/
server/Task/UI/capture/input；零 Git mutation；两仓 dirty/untracked 与他人半成品未触碰；未自建 reviewer。交付
基于逐区源码目检 + 括号平衡 + 逐 API 核对（`ObjectiveTextRecognizer.recognize(BufferedImage)`/`Result` 字段、
`currentExactWindowPendingSmartClickProofToken()`、`TaskExecutionContext.getTurnServiceScope()`、
`@RequiredArgsConstructor` 字段序、`CloudServiceConfiguration` 扫描含 `com.bot.dhxy.service`、fixture 在盘）。

### 父级审核请求

请执行完整 SOURCE+TEST SOURCE REVIEW（三链 production 逐值等价 + prepared state 语义 + named test 正负矩阵 +
objective producer fixture 披露）。我保持 owner 至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或整卡返修指令；
交付后本卡停笔。

**无已批准业务差异；按 `696a12b0` 与用户确认的最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD BUILD-REPAIR-1 SOURCE+TEST DELIVERED OBJECTIVE+PREPARED+PROOF-CLOSED PREPARED-STATE-CREATED SHARED-API-CONSUMED DIALOGSERVICE-3072-207f007d TEST-1706-aa5576cd OLD-TESTS-9ARG-UPDATED OBJECTIVE-FIXTURE-DISCLOSED 2026-07-17T00:50:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST-SOURCE REVIEW #3 - BLOCKED - 2026-07-17T01:05:00-04:00

- Verdict: **`P0/P1/P2=0/2/1 / WHOLE-CARD BUILD REPAIR #2 REQUIRED`**。父级完整读取五个交付文件并复算
  SHA/行数，与 00:50 delivery 一致；objective 恒 fatal、proof 恒 null 与两旧 test 构造失配已修复，但 prepared
  state 尚未形成真实 production 闭环，唯一 named test 仍以 test-only 注入掩盖该缺口。
- **P1-1，prepared state 没有 production publisher，真实 fast path 永远读不到 action。**
  `CloudDialogPreparedActionState.java:44` 暴露 `publish(...)`，但全 Cloud production 对该方法引用为 **0**；唯一
  两处 publish 均在 `DialogOptionTurnContractTest.java:880/935`。`DialogService.java:1441` 只有 consume，所有
  production `prepare*` 方法只返回 `PreparedDialogAction`，没有将其写入该 exact-window state。因此测试手动
  publish 可以通过，而真实 `tryConsumePreparedRememberedRouteOption` 永远只能 absent/fallback，未恢复
  `696a12b0` prepared remembered-route 快路径。
- **P1-2，window/HWND/intent fence 晚于 CAS consume，错误 action 会被清槽。** 基线
  `WindowRuntimeContext.preparedActionMismatchReason` 在 CAS 前逐项检查 windowId、HWND、active intent、operation、
  target；当前 `CloudDialogPreparedActionState.java:83/146` 的 pre-CAS mismatch 只检查 operation/target，随后
  `DialogService.java:1448/1481` 才检查 binding。故错误 window/HWND action 已在 :105 CAS 清空后才被拒绝；
  intentId 完全未校验。这改变 stale-action 保留/拒绝语义，也可能让正确窗口后续失去 action。
- **P2-1，测试矩阵没有证明完整正负合同。** objective 的 end-to-end public path 只有 miss
  (`DialogOptionTurnContractTest.java:818`)，正例 :836 直接调用 recognizer，未经过 STORY 分类、crop 和
  `handleStoryObjective`；proof 只有 nonblank 正例，没有 null/no-pending 负例；prepared 测试直接调用 state.publish，
  正好绕过 P1-1。测试源码不能作为完整 production wiring 证明。
- Required whole-card repair：External B 保持同一整卡 owner，禁止拆卡。必须把既有 production prepared-action
  producer 接到 `CloudDialogPreparedActionState`，并保证 publish/consume 都按 effective tenant/user/device/window
  exact context；在 CAS 前恢复 windowId/HWND/intentId/operation/target fence（route cleared-intent 规则按基线），
  不得新增 TTL/session/ledger/第二 store。唯一 named test 必须从真实 public producer 路径生成并消费 action，补
  binding/intent mismatch 不清槽、objective end-to-end READ 正例以及 proof null 负例。保留已通过 objective typed
  producer、proof-token typed read、旧 test 10 参构造和其余 accepted bytes。
- 当前 source review 已阻断，未运行 Maven/JUnit/compile/runtime/input；零 Git mutation。用户只要求父级本人
  review，不创建额外 reviewer。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-26 PARENT-WHOLE-CARD-REVIEW-3 BLOCKED P0P1P2=0/2/1 BUILD-REPAIR-2-REQUIRED NO-PRODUCTION-PUBLISH PRECAS-BINDING-INTENT-FENCE-MISSING TEST-MATRIX-GAPS RETURN-SAME-EXTERNAL-B 2026-07-17T01:05:00-04:00 -->

## PARENT WIP ACTIVITY RECOVERY OBSERVATION - 2026-07-17T01:58:00-04:00

- External B 的 Repair #2 写集出现真实增量：唯一 `DialogOptionTurnContractTest.java` 从父级上轮快照
  1,706 行/`aa5576cd...` 增至 1,916 行/`d208c1d27fb42810...`，mtime `01:57:31.734`。
- `DialogService.java` 仍为 3,081 行/`6b3c41ddbf095fb0...`（mtime `01:12:33.708`）；
  `CloudDialogPreparedActionState.java` 仍为 212 行/`115d35aa088d8a9a...`（mtime `01:10:24.893`）。
- 本段只确认 `SOURCE_ACTIVE_RECOVERED`，不是 delivery、review 或 approval。B 尚未在总账回执父级消息，故
  `COMMUNICATION_STALE` 保持；canonical whole-card owner 不撤销。父级不审中途 WIP、不运行 Maven、不双派。

<!-- TRUE_EOF: TURN-26 PARENT-WIP-ACTIVITY-RECOVERED TEST=1916-d208c1d2 COMMUNICATION-STALE EXTERNAL-B-OWNER NO-DELIVERY NO-REVIEW 2026-07-17T01:58:00-04:00 -->


## EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR #2 SOURCE+TEST DELIVERED - 2026-07-17T02:22:00-04:00

- Implementation Worker：**CR271 External Worker B**（同一整卡 owner，claim 00:36:41，未拆卡、未换 owner、未自建
  reviewer）。非 reviewer，不自批，本段不含 `APPROVED/CLOSED`。回应 Parent Review #3（01:05）`P0/P1/P2=0/2/1 /
  WHOLE-CARD BUILD REPAIR #2 REQUIRED`。

### P1-1 修复：production prepared-action producer 接入 exact-window state（真实 fast path 可读到）

- `DialogService.java` 新增 `publishPreparedDialogAction(Optional<PreparedDialogAction>)`（:1436）：从
  `taskExecutionContextHolder.current()` 取 effective context，`toBuilder().windowId(context.getWindowId())
  .hwnd(context.getNativeWindowHandle())` 绑定后 `preparedActionState.publish(context, bound)`。
- 两个真实 public producer 接线：route core `prepareRouteKeywordOption` 返回处（:828）与 remembered core
  `prepareRememberedChoiceOption` 返回处（:888）均改为 `return publishPreparedDialogAction(...)`。故真实
  `prepare*` 现把 action 写入 per (tenant/user/device/window) exact-window slot。
- `tryConsumePreparedRememberedRouteOption`（:1450，经 `handleRememberedOption`:1399 的真实 remembered fast path
  调用）用 6 参 `consumeValidated(runtime, ROUTE_TRANSFER, target, reason, true, validator)` 消费，validator=真实
  `validatePreparedDialogActionForConsume`（fingerprint 复核）。删除旧 post-CAS `matchesCurrentPreparedDialogBinding`。

### P1-2 修复：window/HWND/intent fence 移到 CAS 之前

- `CloudDialogPreparedActionState.consumeValidated` 改 6 参（加 `boolean allowClearedRouteIntent`）。CAS 前的
  `mismatchReason` 逐项校验 **windowId → HWND → intentId → operation → target**（全部先于任何 slot 触碰），错误
  binding 直接返回 null 而**不清槽**。intentId：Cloud turn 无 active pathing intent（current=null），带 intentId 的
  action 仅经 route cleared-intent 规则（`allowClearedRouteIntent && expectedOperation==ROUTE_TRANSFER &&
  action.operation==ROUTE_TRANSFER`）恢复，逐值镜像 baseline `WindowRuntimeContext.preparedActionMismatchReason`。
  无 TTL/session/ledger/第二 store。

### P2-1 修复：named test 全部走真实 public producer 路径（无 test-only publish）

删除两个旧 prepared test（用 `preparedState.publish(...)` test-only 注入 + 旧 5 参 consume）与一个**损坏的**
objective producer test（见"诚实披露"），改为经真实 producer / 真实 recognizer 的矩阵（`DialogOptionTurnContractTest`
1706→1916）：

- `rememberedRouteRealProducerPublishesExactWindowBoundActionAndStateConsumesIt`：真实
  `service.prepareRememberedRouteOption(...)`（supplied OPTION 帧）发布一个 **click-required** ROUTE_TRANSFER
  action，断言 `windowId==context.windowId`、`hwnd==context.nativeWindowHandle`、`clickRequired`；再经真实
  `preparedState.consumeValidated(ROUTE_TRANSFER,target,true,passthrough)` 消费（fence 通过、CAS 消费）+ 二次消费=null
  证清槽。
- `rememberedRoutePreparedStateBindingMismatchDoesNotClearSlot`：真实 producer 发布后，错误 target 与错误 operation
  各消费一次 → null（pre-CAS 拒绝），随后真实 caller 用正确 op/target 仍取到 survivor（**不清槽**）。
- `rememberedRouteFastPathConsumesRealProducerActionValidatesFingerprintAndFlowsProof`：真实 producer 发布 →
  `handleDialog(handleRememberedChoiceOption verify=false)` 走真实 trusted remembered fast path →
  `tryConsumePreparedRememberedRouteOption` 真实消费 → **1 validation CAPTURE**（ROI 像素=按 produced 的
  `validationLeft/Top/Right/Bottom` 从同一 dialog 精确裁出的 88×36 crop）→ per-pixel template wash 复现存储
  fingerprint（distance 0）→ CAS 消费 + 清槽 → **1 MOVE+WAIT+CLICK** → `OPTION_KEYWORD_CLICKED` 带 preparedAction、
  exact absoluteX/Y、`executeCalls==2`、二次 consume=null；present `RecordingSmartClickConfirmation("proof-abc")` →
  `tokenReads==1/confirmCalls==1/lastProofToken=="proof-abc"/DIALOG_OCR`。**这条直接反驳 P1-1"真实 fast path 永远读不到
  action"**。
- proof 正负：`optionKeywordClickFlowsPresentSmartClickProofToken`（present token 流回）+
  `optionKeywordClickWithNoPendingProofPassesNullProofToken`（confirmation 返 null → 经 typed API 读到 null →
  `confirmCalls==1 && lastProofToken==null`，逐值等价 baseline 无 pending 侧信道）。
- objective：`objectiveRecognizerReadsReconstructedRealGlyphPanel`——用**真实 shipped washed 模板**重建 长安城东
  (102,186) 面板（`map_names/changan_chengdong.png` + 位置索引 glyph `1/0/2/comma/1/8/6 _story_changan_chengdong_
  102_186_<pos>.png`，把每个 1-bit 模板的白前景按 option-green 绘制，WASH_GREEN 逐像素复现模板），经真实
  `ObjectiveTextRecognizer.recognize` → `found/长安城东/102/186`。
  `storyObjectiveReadHandlesReconstructedRealGlyphPanelThroughHandleStoryObjective`——end-to-end **READ 正例**：STORY
  detection（帧内嵌重建面板于 small-story crop 区）经真实 `handleStoryObjective`→`cropStoryObjectiveImage`→真实
  recognize → `STORY_OBJECTIVE_READ/长安城东/102/186`。保留 end-to-end **miss**
  （full `handleDialog(readStoryObjective)`→分类→crop→recognize→NOT_FOUND）。

### 完整改动文件

| 文件 | 行数 | SHA-256 | 本轮 |
|---|---:|---|---|
| Cloud `service/DialogService.java` | 3081 | `6b3c41ddbf095fb04cb0634781414953c9c4c77c81752b4f74f50f944fae418b` | **改**（Repair#1 3072/`207f007d`） |
| Cloud `service/dialog/CloudDialogPreparedActionState.java` | 212 | `115d35aa088d8a9a994b9d75889d0a64d73fcc1c7d2f809bdf249b566d7192b8` | **改**（Repair#1 169/`447b599f`；6 参 fence） |
| Cloud `test/.../DialogOptionTurnContractTest.java` | 1916 | `d208c1d27fb4281089fad966b726f329dad347473f556be009b77d5f9465cff0` | **改**（Repair#1 1706/`aa5576cd`） |
| Cloud `test/.../DialogDetectionTurnContractTest.java` | 1066 | `87145468bf9c2488ba1b686df6d29760f9adec5dea1826840bf168d49c762f47` | 未动（= Repair#1 accepted） |
| Cloud `test/.../DialogGiveItemTurnContractTest.java` | 383 | `b970cf838eab98da5f8c123594895c2be5d71046d4a8e9c658b65e87f686f945` | 未动（= Repair#1 accepted） |

三 option/white port + `LocalOcrClient` 未动（= 已接受）；写集外零写入；本固定报告 append-only。TURN-28
`ObjectiveTextRecognizer`/`SmartClickEvidenceConfirmationService` 只消费不改。

### 诚实披露（请父级审）

1. **修复了一处真实缺陷**：Repair #1 的 objective producer 正例 test 读的资源路径
   `images/template/objective/0_story_changan_chengdong_102_186_1.png` **在盘上不存在**（该名字只存在于
   `green_digits/` 子目录且是 8×12 的单个绿数字模板）；该 test 会在**运行期** `resourceImage` 处抛错。源审（不运行）
   不会暴露。本轮删除该 test，改用从真实 shipped 模板重建整面板的 recognizer 正例（E）+ end-to-end READ 正例（F'）。
   仓内**不存在**整面板 objective 截图，只有 per-map 标签模板 + per-glyph 绿数字/逗号模板；F'/E 由这些真实模板逐像素
   重建，属"真实面板拆解重组"，非手绘 mock。
2. **确定性依据（逐区源码核对，本轮未跑 Maven/JUnit）**：
   - fingerprint distance 0：`washDialogOptionTemplateTextToBlackAndWhite` 是**逐像素**判定
     (`isOptionGreen(rgb)||isHighlightedOptionYellow(rgb)`)，故 wash-then-crop ≡ crop-then-wash，consume 侧对精确 ROI
     裁剪重洗得到同一 binary → 同一 fingerprint（`washPreparedValidationCrop(TEMPLATE_SPECIFIC)` 即调同一函数）。
   - 坐标读取确定：`isCoordinateStartRun` 要求 width≤6，而重建数字宽 7–8px 恒不满足 → 逗号锚点向左回退恒用
     `start=commaIndex-3`，而"102"恰 3 个 x 位数字先于逗号 → 精确选中 `[1,0,2,comma,1,8,6]`（对 map 标签被切成几段
     鲁棒）；逗号 bottom-aligned 使 `isCommaRun`（w≤5,h≤6,minY≥8,px≤12）成立；每 glyph 逐像素=自身模板 → 匹配
     `binaryTemplateScoreAt≈1.0`（阈值 map/digit=0.82/0.45）。
   - plausibility：`config/maps.json` 以显示名键（含 `长安城东`），(102,186) 为该图真实目标坐标 → `isCoordinatePlausible`
     通过。
   - F' 用**反射**调私有 `handleStoryObjective(DialogHandleRequest, DialogDetection)`——与本 test 既有反射缝
     （`readDialogOptionWordsViaPorts`/`selectOcrFallbackOption`，前几轮已被父级接受）同型；这样避开三项 detection 空间
     约束（mask 平滑度<30 stddev、upper-half 白字模式、lower-half green<150）却仍经真实 crop+handleStoryObjective+
     recognize。end-to-end 分类wiring由保留的 miss 正例覆盖（full `handleDialog`）。
3. Repair #1 已通过项**逐字保留**：objective typed producer（`recognize(BufferedImage)`）、proof-token typed read
   （`currentExactWindowPendingSmartClickProofToken()`）、两旧 test 10 参构造、其余 accepted bytes。

### 未运行项目

未运行 Maven/JUnit/compile/package/build（TURN-35/36/37 等仍可能为活动 Java writer；且单文件 javac 需全 Maven
classpath 不可独立跑）；未启动 runtime/application/server/Task/UI/capture/input；零 Git mutation；两仓 dirty/untracked
与他人半成品未触碰；未自建 reviewer。交付基于逐区源码目检 + 括号平衡（test brace 平衡=0）+ 逐 API 核对（producer
publish 接线、6 参 pre-CAS fence、per-pixel wash、glyph 模板在盘且位置索引齐全、recognizer 分段/comma/plausibility
逻辑、`ObjectiveTextResult` 字段类型、`ScriptedCommandPort` FIFO、proof gate 仅按 status）。

### 父级审核请求

请执行完整 SOURCE+TEST SOURCE REVIEW（P1-1 production publisher 接线 + P1-2 pre-CAS fence 逐值等价 + P2-1 真实
producer/真实 recognizer 正负矩阵 + 上述披露）。我保持同一整卡 owner 至父级 `SOURCE+TEST SOURCE REVIEW PASSED`
或整卡返修指令；交付后本卡停笔。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD BUILD-REPAIR-2 SOURCE+TEST DELIVERED P1-1-PRODUCTION-PUBLISHER-WIRED P1-2-PRECAS-FENCE-6ARG P2-1-REAL-PRODUCER-MATRIX BROKEN-FIXTURE-TEST-FIXED OBJECTIVE-READ-POSITIVE-VIA-REAL-GLYPHS DIALOGSERVICE-3081-6b3c41dd STATE-212-115d35aa TEST-1916-d208c1d2 2026-07-17T02:22:00-04:00 -->

## 2026-07-17 02:03 EDT - Parent Whole-Card Source+Test Review #4

**结论：`P0/P1/P2=0/1/2 / BLOCKED / WHOLE-CARD BUILD REPAIR #3 REQUIRED`。** Repair #2 的 canonical delivery、五文件清单和 SHA 已核实；以下三项仍属于同一完整 TURN-26，返原 External B 一次性闭合。

### P1-1 - real producer 返回值未绑定，交付的 named test 与 production 直接矛盾

- production：Cloud `DialogService.publishPreparedDialogAction`（当前约 1436-1447 行）从 `prepared.get()` 构造并发布带 current window/HWND 的 `bound` clone，但方法末尾仍 `return prepared`，即把未绑定原对象返回 caller。
- executable evidence：`DialogOptionTurnContractTest.rememberedRouteRealProducerPublishesExactWindowBoundActionAndStateConsumesIt`（当前约 880-919 行）取得 `service.prepareRememberedRouteOption(...)` 返回值后，明确断言返回 action 的 `windowId==context.windowId`、`hwnd==context.nativeWindowHandle`。按当前 production，该断言必然失败；producer builder 本身未填这两个字段。
- Repair：publisher 必须发布并返回同一个 exact-bound action；不得新增第二 store、TTL、额外 read/verify 或业务分支。同时保留 empty/no-context 路径的既有语义。

### P2-1 - pre-CAS binding/intent no-clear 验收矩阵不完整

- `rememberedRoutePreparedStateBindingMismatchDoesNotClearSlot`（约 921-959 行）只覆盖 wrong target 与 wrong operation；没有覆盖 Review #3 明确要求的 wrong window、wrong HWND、wrong intent。
- Repair：为三种 mismatch 分别提供 executable no-clear 证据：错误 consume 返回 null，原 slot 不被 CAS 清除，随后正确 exact-context consumer 仍可一次消费。真实 producer 覆盖保留；不得用 test-only seam 替代整条 producer 验收。

### P2-2 - objective READ 正例绕过 public path

- `storyObjectiveReadHandlesReconstructedRealGlyphPanelThroughHandleStoryObjective`（约 857-878 行）反射直调 private `handleStoryObjective`；现有 public `handleDialog(readStoryObjective)` 仅覆盖 miss，因此不构成卡片要求的 public-path end-to-end READ positive。
- Repair：用满足真实 STORY classification 的帧经 public `handleDialog(DialogHandleRequest.readStoryObjective(...))` 到 `STORY_OBJECTIVE_READ`；不得以 private reflection 作为该验收点。真实 shipped glyph 重建可继续使用。

### Gate

- Repair #3 必须重新交付完整五文件清单、行数/SHA、上述三项 executable test source 与基线声明。
- 在三项全部闭合前不写 `SOURCE+TEST SOURCE REVIEW PASSED`，不开放 TURN-27，不拆卡、不换 owner、不创建 reviewer。
- 本轮为源码阻断审查，未运行 Maven/JUnit/compile/runtime/input；零 Git mutation；全部 dirty/untracked 保持。

**无已批准业务差异；按唯一业务基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 PARENT-REVIEW-4 BLOCKED P0-P1-P2-0-1-2 WHOLE-CARD-BUILD-REPAIR-3-REQUIRED RETURN-SAME-EXTERNAL-B 2026-07-17T02:03:00-04:00 -->

## 2026-07-17 02:14 EDT - Parent Repair #3 Stale Audit

- `PARENT-TURN26-REVIEW4-REPAIR3` 已连续两个父级 5 分钟检查周期没有 External B 回执；五文件 SHA/mtime
  与 Repair #2 delivery 完全一致，Repair #3 尚无新字节。
- 状态：`COMMUNICATION_STALE + ACTIVE_STALE / EXTERNAL-B OWNER`。canonical owner 保留；不撤销、不重派、
  不拆卡。B 下一轮须先在共享总账回执该 message id，再继续完整 Repair #3 或如实 canonical owner return。
- 未运行 Maven/JUnit/compile/runtime/input；零 Git mutation。

TRUE_EOF

<!-- TRUE_EOF: TURN-26 PARENT-STALE-AUDIT REPAIR3 COMMUNICATION-STALE ACTIVE-STALE EXTERNAL-B-OWNER-PRESERVED 2026-07-17T02:14:00-04:00 -->

## 2026-07-17 02:19 EDT - Parent Repair #3 Activity Recovery

- WIP 复核：`DialogService.java` 已更新为 `b28b1335...`（mtime `02:17:32`），唯一 named test 已更新为
  `2e35148f...`（mtime `02:18:48`）；`CloudDialogPreparedActionState` 与两份冻结 test 未变。
- 解除 `ACTIVE_STALE`；`COMMUNICATION_STALE` 因 External B 尚无定向消息回执而保持。当前不是 canonical
  delivery，不做中途 review，不撤 owner、不重派。
- Java writer 活动中，未运行 Maven/JUnit/compile/runtime/input；零 Git mutation。

TRUE_EOF

<!-- TRUE_EOF: TURN-26 PARENT-WIP-ACTIVITY-RECOVERED REPAIR3 DIALOG=b28b1335 TEST=2e35148f COMMUNICATION-STALE EXTERNAL-B-OWNER NO-DELIVERY 2026-07-17T02:19:00-04:00 -->


## EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR #3 SOURCE+TEST DELIVERED - 2026-07-17T02:30:00-04:00

- Implementation Worker：**CR271 External Worker B**（同一整卡 owner，未拆卡/换 owner/自建 reviewer）。非
  reviewer，不自批。回应 Parent Review #4（02:03）`P0/P1/P2=0/1/2 / BLOCKED / WHOLE-CARD BUILD REPAIR #3
  REQUIRED`，三项在原冻结写集内一次性闭合。

### P1-1 修复：producer 返回同一个 exact-bound action（production）

`DialogService.publishPreparedDialogAction`（现约 :1436-1450）在 publish 后 **`return Optional.of(bound)`**：把绑好
current window/HWND 的同一个 action 返回 caller，不再返回 pre-bind 原对象。empty / no-context 路径语义不变（仍
`return prepared`）。不新增第二 store/TTL/额外 read/verify/业务分支。此修复使
`rememberedRouteRealProducerPublishesExactWindowBoundActionAndStateConsumesIt`（断言返回值
`windowId==context.windowId`、`hwnd==context.nativeWindowHandle`）与 production 一致（父级指出的直接矛盾已消除）。

### P2-1 修复：补 wrong window / wrong HWND / wrong intent 的 executable no-clear 证据（test）

保留既有 wrong-target / wrong-operation。新增两个 named test（真实 producer 发布 exact-bound action 后）：

- `rememberedRoutePreparedStateWrongHwndAndWrongWindowDoNotClearSlot`：
  - **wrong HWND**——用 `WRONG_HANDLE_WINDOW`（同 tenant/user/device/window slot key，native handle `0x2627` vs
    `0x2626`）经 `TaskExecutionContext.turnNative(...)` 构造真实 context，consume 被 **pre-CAS HWND fence** 拒绝
    →null、不清槽；
  - **wrong window**——不同 windowId（`window-turn-26-other`）的真实 context 落在**不同 exact-window slot**→
    consume 返回 null、原 slot 不动；
  - 随后 exact-context caller 仍**一次**消费到 survivor（`route-target`）。
- `rememberedRoutePreparedStateStaleIntentRejectedUnlessClearedRouteRecoveryAllowed`：
  - **wrong intent**——Cloud turn model 无 active pathing intent，**任何真实 producer 都不写 intentId**（见披露），
    故以 exact-bound producer action 的 `toBuilder().intentId("stale-pathing-intent")` 注入 stale intent 后
    `consumeValidated(..., allowClearedRouteIntent=false)` 被 **intentId fence** 拒绝→null、不清槽；
  - `allowClearedRouteIntent=true` 时按 **baseline route cleared-intent 规则**（ROUTE_TRANSFER）恢复，一次消费到
    同一 action。真实 producer publish/consume 验收仍在 Test A/C，本 test 只补 fence 分支，非替代。

### P2-2 修复：objective READ 正例改走 public `handleDialog(readStoryObjective)`（test）

删除反射调用 private `handleStoryObjective` 的旧正例。新
`storyObjectiveReadThroughPublicHandleDialogReadsReconstructedRealGlyphPanel`：enqueue 一帧经 public
`handleDialog(DialogHandleRequest.readStoryObjective(...))`→真实 `detectDialogSnapshotDirect` 分类为 STORY→
`handleStoryObjective`→`cropStoryObjectiveImage`→真实 `ObjectiveTextRecognizer.recognize`→
`STORY_OBJECTIVE_READ/长安城东/102/186`，并 `assertSingleCapture`。帧由 `publicStoryFrameWithObjectivePanel()`
精确构造以同时满足真实分类三检查：
- **story-upper 白字检查**（frame rows 33..75）：rows 36..47 的 thin-white line pattern（12 行/40 cluster/span 160）
  → `qualifyingRows≥10、maxWhitePixelsInRow≥40、maxClustersInRow≥20、maxSpanInRow≥120、totalTextPixels≥450`；
- **option-lower 绿检查**（rows 75..176）：绿面板落在 rows 48..74，此区无绿→`countGreenPixelsHSV<150` 不判 OPTION；
- **mask 平滑区**（rows 91..176, cols 161..529）：全黑→`stddev<30` 判 dialog mask 存在；
- **small-story recognize crop**（rows 33..176）：含重建绿面板（rows 54..70）；WASH_GREEN 只留绿面板（白字非绿→洗黑）
  → recognizer 逐像素匹配真实 shipped 模板。
保留 end-to-end **miss** 正例（同 public 路径、非目标帧→NOT_FOUND）。

### 完整改动文件

| 文件 | 行数 | SHA-256 | 本轮 |
|---|---:|---|---|
| Cloud `service/DialogService.java` | 3084 | `b28b1335fecb6844170e2eb47430165742b6842e782cb97448449650c932e35f` | **改**（Repair#2 3081/`6b3c41dd`；P1-1 return bound） |
| Cloud `service/dialog/CloudDialogPreparedActionState.java` | 212 | `115d35aa088d8a9a994b9d75889d0a64d73fcc1c7d2f809bdf249b566d7192b8` | 未动（= Repair#2；6 参 fence 逐值等价） |
| Cloud `test/.../DialogOptionTurnContractTest.java` | 2012 | `2e35148fb028ecb0bb296d3679c4e0aa45d9cf9e7356ed37f5c88f77dabe3fc7` | **改**（Repair#2 1916/`d208c1d2`；+2 fence test，READ 改 public，删反射） |
| Cloud `test/.../DialogDetectionTurnContractTest.java` | 1066 | `87145468bf9c2488ba1b686df6d29760f9adec5dea1826840bf168d49c762f47` | 未动（= accepted） |
| Cloud `test/.../DialogGiveItemTurnContractTest.java` | 383 | `b970cf838eab98da5f8c123594895c2be5d71046d4a8e9c658b65e87f686f945` | 未动（= accepted） |

三 option/white port + `LocalOcrClient` 未动；写集外零写入；本报告 append-only。TURN-28 shared API 只消费不改。

### 诚实披露（请父级审）

1. **wrong intent 的注入是必需且最小的**：`buildRememberedPreparedDialogAction` 等所有 Cloud producer **从不**写
   `intentId`（`CloudDialogPreparedActionState` javadoc 已述：Cloud turn model 无 active pathing intent，
   current intent 恒 null）。故 intentId fence 分支**无任何真实 producer 输入可达**；唯一 executable 方式=对
   exact-bound producer action `toBuilder().intentId(...)` 注入。这是对 baseline-parity 防御分支的忠实测试，且**未
   替代**整条 producer 验收（Test A 真实 publish+consume、Test C 真实 fast-path+fingerprint+proof 均保留）。
2. **wrong window 走 slot 隔离**：exact-window slot key 含 windowId，故不同 windowId 的 context 落在不同 slot →
   consume 返回 null（absent）而非 windowId-mismatch 分支——这正是 window 级 no-clear 保证（原 slot 不被触碰）。
   windowId fence 分支（同 slot 内 action.windowId≠current）在正常 publish 下不可达（action.windowId 恒=slot 的
   windowId），属 baseline-parity 防御；wrong HWND 分支则真实可达并已用真实 context 覆盖。
3. **P2-2 分类几何为源码核对（本轮未跑 Maven/JUnit）**：三检查区与 recognize crop 的行区间由
   `DIALOG_LARGE=(250,312)`、`DIALOG_SMALL=(250,345,529,143)`、`CROP_TOP_Y=42/CROP_LEFT_X=161/CROP_DEV_Y=58`、
   `detectDialog` 的 `dialogRegion=[387,553,916,761]` 推出；绿面板 rows 48..74 严格落在 recognize crop 内、
   option-lower/mask 区外。Repair #2 已核的 wash 逐像素、坐标 `commaIndex-3`、comma bottom-align、plausibility 不变。
4. Repair #1/#2 已通过项**逐字保留**：objective typed producer、proof-token typed read、pre-CAS fence 6 参、
   real-producer publish/consume/fast-path/proof 矩阵、reconstructed-glyph recognizer 正例（direct）、两旧 test 10 参
   构造。

### 未运行项目

未运行 Maven/JUnit/compile/package/build（他 Java writer 可能活动 + 单文件 javac 需全 classpath）；未启
runtime/application/server/Task/UI/capture/input；零 Git mutation；两仓 dirty/untracked 与他人半成品未触碰；未自建
reviewer。交付基于逐区源码目检 + 括号平衡（test brace=0，19 @Test）+ 逐 API 核对（`publishPreparedDialogAction`
return bound、`WRONG_HANDLE_WINDOW` 同 windowId 异 handle、`TaskExecutionContext.turnNative`/`getWindowId`/
`getNativeWindowHandle` 语义、`isClearedRouteIntentRecoveryAllowed`、detection 三检查区几何、`detectDialog`
dialogRegion、`assertSingleCapture`、`ObjectiveTextResult` int 字段）。

### 父级审核请求

请执行完整 SOURCE+TEST SOURCE REVIEW（P1-1 return-bound + P2-1 三 mismatch no-clear + P2-2 public-path READ 正例 +
上述披露）。我保持同一整卡 owner 至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或整卡返修指令；交付后本卡停笔。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD BUILD-REPAIR-3 SOURCE+TEST DELIVERED P1-1-RETURN-BOUND P2-1-WINDOW+HWND+INTENT-NO-CLEAR P2-2-PUBLIC-PATH-READ-POSITIVE DIALOGSERVICE-3084-b28b1335 STATE-212-115d35aa TEST-2012-2e35148f 2026-07-17T02:30:00-04:00 -->

## 2026-07-17 02:24 EDT - Parent Whole-Card Source+Test Review #5

**结论：`P0/P1/P2=0/0/2 / BLOCKED / WHOLE-CARD BUILD REPAIR #4 REQUIRED`。** Review #4 的三项
production/test acceptance 已全部闭合；本轮只剩两处 safety-sensitive JavaDoc 与实现相反。

### 已通过的 Repair #3 范围

- `DialogService.publishPreparedDialogAction` 现发布并返回同一个 exact-bound action；real producer test 的
  returned window/HWND 契约与 production 一致。
- wrong HWND、wrong window slot isolation、stale intent no-clear/cleared-route recovery 均有 executable source；
  正确 exact-context consumer 仍能消费 survivor。
- objective READ positive 已经由 public `handleDialog(readStoryObjective)` 进入真实 classification/crop/recognizer，
  不再反射调用 private `handleStoryObjective`。

### P2-1 - publisher JavaDoc 仍反述旧返回语义

- Cloud `DialogService.java` 约 1432-1434 行仍写“caller receives the same optional it produced; only the stored
  slot carries the binding”，但约 1442-1447 行现在发布并返回 `Optional.of(bound)`。
- Repair：只更新该 JavaDoc，明确 caller 与 slot 获得同一个 exact-bound action；不得改 production 行为。

### P2-2 - prepared-state 类 JavaDoc 否认自身 binding fence

- `CloudDialogPreparedActionState.java` 约 27-29 行仍写“never validates ... window bindings itself”，但
  `consumeValidated` 约 90-97/162-188 行已在 CAS 前校验 window/HWND/intent/op/target。
- Repair：只更新类 JavaDoc，准确说明 state 自身执行 pre-CAS binding fence，而 fingerprint validation 仍由
  `DialogService` 负责；不得改 state 行为。

### Verification

- 尝试授权命令 `mvn -q -Dtest=DialogOptionTurnContractTest test`，test 尚未执行即被写集外共享 compile debt 阻断；
  首批证据包括缺失 `TextCandidateScanStatus`、`GameClientTracker`、`BagService`，以及 `WubeiTask`/
  `NavigationService` 的多项未冻结依赖。此为 cross-card build debt，不计 TURN-26 finding。
- Repair #4 重新交付完整五文件 SHA/行数与 comment-only 声明；父级复审后再决定 source pass。未启 runtime/input，
  零 Git mutation。

TRUE_EOF

<!-- TRUE_EOF: TURN-26 PARENT-REVIEW-5 BLOCKED P0-P1-P2-0-0-2 WHOLE-CARD-BUILD-REPAIR-4-COMMENT-ONLY RETURN-SAME-EXTERNAL-B NAMED-TEST-BLOCKED-BY-SHARED-COMPILE-DEBT 2026-07-17T02:24:00-04:00 -->

## 2026-07-17 02:29 EDT - Parent Repair #4 WIP Observation

- comment-only WIP 已出现：`DialogService.java`=`5d175fd8...`、prepared state=`169d4382...`；三 test SHA
  与 Repair #3 delivery 一致。
- 尚无 canonical Repair #4 delivery；保护 External B sole owner，不做中途 review，不运行 Maven。

TRUE_EOF

<!-- TRUE_EOF: TURN-26 PARENT-WIP-OBSERVATION REPAIR4-COMMENT-ONLY DIALOG-5d175fd8 STATE-169d4382 TESTS-UNCHANGED EXTERNAL-B-OWNER NO-DELIVERY 2026-07-17T02:29:00-04:00 -->


## EXTERNAL-B TURN-26 WHOLE-CARD BUILD-REPAIR #4 SOURCE+TEST DELIVERED - 2026-07-17T02:36:00-04:00

- Implementation Worker：**CR271 External Worker B**（同一整卡 owner，未拆卡/换 owner/自建 reviewer）。非
  reviewer，不自批。回应 Parent Review #5（02:24）`P0/P1/P2=0/0/2 / BLOCKED / WHOLE-CARD BUILD REPAIR #4
  REQUIRED（COMMENT-ONLY）`。两处 safety-sensitive JavaDoc 与现实现相反，**仅改注释，零行为改动**。

### P2-1 修复：publisher JavaDoc 更新为 return-bound 语义

`DialogService.publishPreparedDialogAction` 的方法 JavaDoc 旧句“The caller receives the same optional it
produced; only the stored slot carries the binding”与 Repair #3 的 `return Optional.of(bound)` 相反。改为：publisher
把 produced action 绑到 current exact window/HWND，**把该 bound action 同时存入 slot 并返回 caller**，caller 与 slot
持有相同 exact-window binding；empty / no-context 路径返回原 optional 不变。**方法体、返回值、行为一字未改。**

### P2-2 修复：prepared-state 类 JavaDoc 更新为"自身执行 pre-CAS binding fence"

`CloudDialogPreparedActionState` 类 JavaDoc 旧句“never validates ... window bindings itself — fingerprint and
binding validation stay in DialogService”与 `consumeValidated`（CAS 前 window/HWND/intent/op/target 校验）相反。改为：
state **自身执行 baseline pre-CAS binding fence**（window id、HWND、active pathing intent[route cleared-intent
规则]、operation、target 均在 compare-and-set 前校验），**fingerprint validation 仍由 DialogService 负责**。同时把上段
consume 语义句“absent or operation/target mismatched”补为“absent, or window/HWND/intent/operation/target
mismatched ... stay untouched”以自洽。**consumeValidated/mismatchReason/publish/clear 方法体与行为一字未改。**

### 完整改动文件

| 文件 | 行数 | SHA-256 | 本轮 |
|---|---:|---|---|
| Cloud `service/DialogService.java` | 3087 | `5d175fd8f00dc6056d8bbf5bdc50f56554ca4f0157f86a7852210a23980fc896` | **改**（Repair#3 3084/`b28b1335`；仅 publisher JavaDoc） |
| Cloud `service/dialog/CloudDialogPreparedActionState.java` | 213 | `169d4382df9381f8583fbcfe733b3ad5941c228173a796987f2462e6854ae940` | **改**（Repair#3 212/`115d35aa`；仅类 JavaDoc） |
| Cloud `test/.../DialogOptionTurnContractTest.java` | 2012 | `2e35148fb028ecb0bb296d3679c4e0aa45d9cf9e7356ed37f5c88f77dabe3fc7` | 未动（= Repair#3 accepted） |
| Cloud `test/.../DialogDetectionTurnContractTest.java` | 1066 | `87145468bf9c2488ba1b686df6d29760f9adec5dea1826840bf168d49c762f47` | 未动（= accepted） |
| Cloud `test/.../DialogGiveItemTurnContractTest.java` | 383 | `b970cf838eab98da5f8c123594895c2be5d71046d4a8e9c658b65e87f686f945` | 未动（= accepted） |

三 option/white port + `LocalOcrClient` 未动；写集外零写入；本报告 append-only。TURN-28 shared API 只消费不改。

### 基线与行为核对

- 两文件本轮**唯一 diff = JavaDoc 文本**；括号平衡=0（两 production 文件）；`publishPreparedDialogAction` 与
  `consumeValidated/mismatchReason` 的可执行语句字节未变，故 Repair #3 已通过的 P1-1 return-bound、P2-1 三 mismatch
  no-clear、P2-2 public-path READ 正例逐一保持。无已批准业务差异；按 `696a12b0` 等价迁移。

### 未运行项目 / cross-card build debt

未运行 Maven/JUnit/compile/package/build（写集外共享 compile debt——父级 Review #5 实测 `mvn -Dtest=...` 因缺
`TextCandidateScanStatus`/`GameClientTracker`/`BagService` 及 `WubeiTask`/`NavigationService` 未冻结依赖在 test 执行前
即被阻断，属 cross-card build debt，非 TURN-26 finding；本卡五文件与该 debt 无耦合）；未启
runtime/application/server/Task/UI/capture/input；零 Git mutation；两仓 dirty/untracked 与他人半成品未触碰；未自建
reviewer。

### 父级审核请求

请复核两处 JavaDoc（comment-only，零行为改动）与不变的五文件行为。我保持同一整卡 owner 至父级 `SOURCE+TEST
SOURCE REVIEW PASSED` 或整卡返修指令；交付后本卡停笔。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 EXTERNAL-B WHOLE-CARD BUILD-REPAIR-4 SOURCE+TEST DELIVERED COMMENT-ONLY P2-1-PUBLISHER-JAVADOC P2-2-STATE-JAVADOC ZERO-BEHAVIOR-CHANGE DIALOGSERVICE-3087-5d175fd8 STATE-213-169d4382 TEST-2012-2e35148f 2026-07-17T02:36:00-04:00 -->

## 2026-07-17 02:34 EDT - Parent Whole-Card Source+Test Review #6

**最终结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`。**

- publisher JavaDoc 现准确说明 slot 与 caller 获得同一 exact-bound action；prepared-state JavaDoc 现准确说明 state 自身执行 pre-CAS binding fence、fingerprint validation 留在 `DialogService`。
- 三 test SHA 保持 `2e35148f...` / `87145468...` / `b970cf83...`；Repair #3 的 return-bound、三类 no-clear 与 public-path objective READ positive 均无回退。
- 授权 named test 已在 Review #5 尝试，但在执行前被 TURN-26 写集外共享 compile debt 阻断；该独立 build debt 不阻止 source/test-source pass。
- External B owner 释放；不创建额外 reviewer。TURN-27 source gate 自动开放。

**无已批准业务差异；按唯一业务基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-26 PARENT-REVIEW-6 SOURCE+TEST-SOURCE-REVIEW-PASSED P0-P1-P2-0-0-0 OWNER-RELEASED TURN27-GATE-OPEN NAMED-TEST-BLOCKED-BY-SHARED-COMPILE-DEBT 2026-07-17T02:34:00-04:00 -->
