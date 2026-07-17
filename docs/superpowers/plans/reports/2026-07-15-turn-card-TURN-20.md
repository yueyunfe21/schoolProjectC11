# TURN-20 - AutoCombatPanelService HTTPS turn cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15 20:52 EDT

- 状态：`READY`；类型：`INTEGRATION`；startDependsOn：`TURN-13C` 与 `TURN-18` source/test-source review passed。
- Worker 是 implementation Worker，不是 reviewer；父级是唯一 manager/final reviewer。
- 唯一 production write set：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`。
  新 model/algorithm 只能作为该文件 private nested type，不得新建第二 production 文件。
- 唯一 test write set：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java`。
  本固定报告可写；其余两仓文件全部只读。
- 业务基线固定为 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。保留全部现有 public API、caller
  可见返回值、panel 可见性/对齐、round estimate、missing attention、team burst guard、阈值、延迟、日志原因、
  TaskCheckpoint/terminal 与状态更新顺序；不得借迁移改变 retry/fallback/刷新时机。
- 迁移边界：
  - exact window rect 只从当前 turn metadata 取得；截图只用 `TurnGameClient.capture(...)` 返回的同一 raw PNG；
  - panel/template/OCR/rounds/drag/refresh decision 全在 Cloud 当前 Service 内完成；不得调用 DHXY tracker、
    temp screenshot path、本地 OCR 或旧 `WindowFact`；
  - 需要 Alt+8、drag 或其它物理动作时只用 `TurnGameClient.execute(...)` 下发一个 ordered action；每次 client
    调用一个 UUID/一个 command，严格保留 baseline step 顺序和 delay；
  - 非 COMPLETED、错 action/window/step、缺 frame、坏 PNG、STOPPED/uncertain 必须保持 typed fail-closed，
    不得折叠为成功/false，也不得自动 retry、第二 exchange、旧 fact/input fallback。
- 交付后生产源码必须对旧 `AUTO_COMBAT_PANEL`/`GEOMETRY` fact、`readWindowFact`、`executeInputBundle`、
  `GameClientTracker`/本地 capture/input/temp-path 依赖零引用；不得修改 caller、Task、协议、Server/routes、DHXY。
- Named test `AutoCombatPanelTurnContractTest` 至少覆盖：
  1. panel 已可见与缺失后 baseline Alt+8 再观察；
  2. exact raw PNG/ROI 与 Cloud visibility/round decision；
  3. baseline drag ordered steps、screen-absolute point 和 delay；
  4. low-rounds/refresh-due/healthy/unknown、estimate/timestamp/team-burst 状态顺序；
  5. COMPLETED/FAILED/STOPPED/uncertain、错 identity/step/frame fail-closed、每次 client 调用单 UUID/command、
     任一状态均无自动 retry。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行 Git mutation；
  父级在全部 Java writers 稳定后运行 named test 与适用 compile cohort。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-20 parent frozen brief -->

## CLAIMED - 2026-07-15 20:54:42 -04:00

- 身份：`CR271 TURN-20 implementation Worker`；不是 reviewer；父级为唯一 manager/final reviewer。
- Exact write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java`
  - `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-20.md`
- 约束确认：其余两仓路径只读；不执行 Git mutation；不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input。

<!-- TRUE_EOF: TURN-20 claimed by CR271 implementation Worker -->

## SOURCE+TEST SOURCES DELIVERED - 2026-07-15 21:49:19 -04:00

- 身份：`CR271 TURN-20 implementation Worker`；本记录仅交付 source/test source，父级仍是唯一
  manager/final reviewer；未宣称 `APPROVED` 或 `CLOSED`。
- Production source：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  （1082 行，49171 bytes）。
- Named test source：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java`
  （1090 行，48226 bytes，7 个 `@Test` 方法）。

### SHA-256

- `AutoCombatPanelService.java`：
  `28ea03e2cddbce0a54310baac89ea341eb31922d4c1de31815345e038b11b742`
- `AutoCombatPanelTurnContractTest.java`：
  `7e1cc3259c8df334cc8c512b843be413c119616c3c8e9267f7ad7cec787c40b4`

### 实施与静态证据

- 保留基线 public surface：`verifyAndAlignPanel()`、`verifyAndAlignPanel(PanelVerifyMode)`、
  `ensurePanelVisible(String,int)`、`resolveRoundsRefreshReason(...)`、`recordCombatExit()`、两个 public enum、
  `RefreshDueBurstDecision` 与 `TeamRefreshDueBurstGuard`。
- exact window rect 仅由当前 bound `TurnGameClient.latestWindowMetadata()` 取得；full-window/round ROI 都只通过
  `TurnGameClient.capture(...)` 接收同一 raw PNG。Service 对 exact outcome window、step、frame presence、
  `purpose=CAPTURE`、`sourceStepIndex=0`、region、SHA-256、PNG signature/decode 和像素尺寸逐项 fail-closed。
- panel anchor/template、OpenCV HSV `50..75 / 150..255 / 180..255` mask、round ROI、4x 红字清洗、Cloud OCR、
  refresh reason 和 drag decision 均在 Cloud 当前 Service 内完成；新增 runtime/match/capture/OCR 类型都为该文件
  private nested type。
- Alt+8 仅为一个 `TurnGameClient.execute(...)` ordered action：`KEY_TAP(ALT_8)` 后接 baseline wait；drag
  仅为一个 ordered action：screen-absolute `DRAG_LEFT` 后接 `WAIT 500ms`。源码统一调用点静态计数为
  `capture=1`、`execute=1`，没有第二 turn exchange 或自动 retry。
- 保留 `489/726` 对齐点、`distance>20`、template threshold `0.80`、round marker `top=-96 / height=30 /
  width=cancel-template-width/2`、estimate `25/10/-3`、refresh wait `1000ms`、missing attention
  `10min/60s`、team burst guard `30s` 及 visible-estimate-before-reason、completed-input-before-reset 的状态顺序。
- command 非 `COMPLETED`、outcome `FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN` 维持 typed fail-closed；
  `INTERRUPTED_UNCERTAIN` 与 `STOPPED` 先走 `TaskCheckpoint`，无 stop 证据时不折叠为成功/false。
- Production source 静态零命中：`AUTO_COMBAT_PANEL`、`GEOMETRY`、`readWindowFact`、
  `executeInputBundle`、`GameClientTracker`、`WindowScopedTempPath`、`InputProvider`、`InputSequences`、
  `TextRecognizer`、`java.nio.file`、`Path.of`、`Files.`；source/test brace count 分别为 `263/263`、
  `100/100`。
- Named test 使用真实 `TurnGameClient` + scripted `CloudTurnCommandPort` + raw `CloudTurnFrame`，使用 packaged
  anchor/green/cancel-auto templates 和真实 Cloud image processor；覆盖 anchor visible、green-HSV fallback、
  missing 后 Alt+8 再观察、exact rounds ROI/raw red PNG/OCR、绝对坐标 drag 与 `500ms`、low/refresh-due/
  healthy/unknown、estimate/timestamp/combat-exit/missing-attention/team-burst、BC4 COMPLETED/FAILED/STOPPED/
  uncertain、BUSY/duplicate、错 action/window/step/frame purpose/region/SHA/sourceStepIndex/pixel dimensions、缺 frame、
  坏 PNG，以及每个 client invocation 单 UUID/command 且失败无额外 action。

### 业务基线核对

- 已核对 `docs/业务逻辑.md` 的“业务基线使用规则（五倍 / 修罗）”、直接战斗模式的授权/执行边界，以及
  “失败 fallback 逐阶段规则”中 STOP/暂停中断不得包装为业务 FAILED 的通用恢复规则。
- 已逐段对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `AutoCombatPanelService.java` visibility/alignment/round estimate/missing attention/team burst guard、阈值、延迟、
  fallback 与状态顺序。
- **无已批准业务差异；按基线等价迁移。**

### 未运行门与父级接手点

- 按固定卡禁令，本 Worker **未运行** Maven、JUnit、compile、runtime、application、server、Task、UI、capture
  或 input；上述均不是通过声明。
- 本 Worker 未执行任何 Git mutation。`git check-ignore -v` 显示 named test 受
  `.gitignore:15:src/test/` 影响；父级收集/审查写集时需按既定流程显式纳入该文件，避免遗漏。
- 父级在全部 Java writers 稳定后运行 named test 与适用 compile cohort，并独立完成 source/test-source review
  与最终判断。

<!-- TRUE_EOF: TURN-20 source and test sources delivered; parent gates pending -->

## PARENT SOURCE/TEST-SOURCE REVIEW #1 - 2026-07-15 22:05 EDT

- 结论：**不通过 / P0/P1/P2=0/2/0**。父级独立读取 production、named test、Cloud 既有 OCR authority、
  `696a12b0` 原 Service，并核对本卡 fixed brief；主截图、模板、ROI、拖动坐标、500ms/1000ms、rounds 状态、
  UUID/command 与无自动 retry 主链未发现其它阻断。
- **P1-1，明确 input failure 改写了既有 public 返回/fallback 语义。** 当前
  `AutoCombatPanelService.java:195-204,238-253,319-329,591-618` 将 turn `FAILED` 与 uncertain 一律抛
  `TaskFatalException`；named test `AutoCombatPanelTurnContractTest.java:315-369` 也把 `FAILED` 固定成 fatal。
  但 `696a12b0` 的同路径是：open Alt+8 `sent=false -> record missing + return null`，round refresh
  `sent=false -> return false`，drag 的 boolean 不改变随后 re-observe/fallback。影响是普通已知物理失败会终止 Task，
  不再保留原 caller-visible `null/false` 与 alignment fallback，属于未批准业务差异。
- **P1-2，绕开 Cloud OCR 单一入口并新造第二套 HTTP client/config。** production
  `AutoCombatPanelService.java:23-24,45-53,995-1080` 内嵌 `CloudOcrWordsReader`，自行创建静态
  `HttpClient`、PNG/Base64、endpoint/timeout 与 JSON parser；而既有
  `com.yueyunfe.dhxy.cloudbrain.LocalOcrClient` 明确是 cloud-brain OCR single choke point。影响是同一 Cloud
  进程出现第二 selector/client、第二配置与第二 diagnostics/failure mapping，后续 OCR endpoint/身份门更新会漂移。

### Repair #1 - 原 Plato 继续持有

- 允许写集：原 `AutoCombatPanelService.java`、原 named test、原报告，外加唯一既有 production
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`；其余文件只读。
- 在 `LocalOcrClient` 暴露最小 Cloud-only joined-text public API，内部仍复用现有 `readWords`、唯一
  `HTTP_CLIENT`、endpoint、限制与 diagnostics；`AutoCombatPanelService` 删除全部自建 HTTP/OCR codec，默认构造只
  引用该 canonical API。不得新建 production 文件或第三层 facade。
- 将 ordered input 结果区分为 `COMPLETED`、已知 `FAILED` 与 `STOPPED/uncertain`：已知 `FAILED` 必须按
  `696a12b0` 分别恢复 open `null`、refresh `false`、drag 后继续同一次 re-observe/fallback；不得追加 action/retry。
  `STOPPED` 先走 `TaskCheckpoint`，未确认 stop 与其它 uncertain 仍 fatal，不得折叠为 false/success。
- named test 补三条直接回归：open/refresh 的已知 FAILED 返回与状态不伪成功；drag FAILED 后仍按基线只做一次
  re-observe/fallback；STOPPED/uncertain 继续 checkpoint/fatal。每个 public invocation 仍一 UUID/command、零 retry。
- 返修交付前不得运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input 或 Git mutation；父级复审
  后统一进入 writer-stable cohort。

<!-- TRUE_EOF: TURN-20 PARENT REVIEW #1 BLOCKED P1=2; REPAIR #1 TO ORIGINAL WORKER -->

## REPAIR #1 CLAIMED - 2026-07-15 22:08:39 EDT

- 身份：`CR271 TURN-20 implementation Worker`（原 Plato）；仅负责 Repair #1 实施与 source/test-source
  返修交付，不是 reviewer；父级仍是唯一 manager/final reviewer。
- Exact production write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`
- Exact test write set：
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java`
- Exact report write set：
  - `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-20.md`
- 其余两个仓库路径全部只读；保护现有 dirty/untracked，不回滚、覆盖、清理或改写他人内容；不执行任何
  Git mutation。
- 本轮恢复 `696a12b0` 已知 input `FAILED` 的 open `null`、refresh `false`、drag 后同次一次
  re-observe/fallback；`STOPPED` checkpoint、uncertain fatal，零额外 action/retry；移除 Service 自建 OCR
  HTTP/client/codec，改用 `LocalOcrClient` 最小 joined-text API 与既有 single choke point。
- 按卡不运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input。

<!-- TRUE_EOF: TURN-20 REPAIR #1 CLAIMED; implementation pending -->

## REPAIR #1 SOURCE+TEST SOURCES REDELIVERED - 2026-07-15 22:19:31 EDT

- 身份：`CR271 TURN-20 implementation Worker`（原 Plato）；仅交付 Repair #1 source/test source，
  **不是 reviewer**；父级仍是唯一 manager/final reviewer。本记录不宣称 `APPROVED` 或 `CLOSED`。

### 返修文件与 SHA-256

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  - 1032 行，47337 bytes
  - SHA-256：`e32c1aa9ea9def6f99fb64552e058123d1e03c420b05919cb18e5e547ce50982`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java`
  - 287 行，13270 bytes
  - SHA-256：`f706e58b83cd4dfe9dd296bb41772a59f678e9181483c6d304371a37b65f934d`
- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java`
  - 1189 行，54049 bytes，9 个 `@Test`
  - SHA-256：`d6016392377e2bd5353db0bc6af9ecdbe588f5dd4437e33499144c0f2da61dd8`

### Repair #1 实施证据

- `LocalOcrClient.java:65` 暴露唯一最小 public `readJoinedText(BufferedImage,String)`；该 API 直接调用既有
  `readWords` 并按返回顺序拼接非空 word text，因此继续复用唯一 `HTTP_CLIENT`、endpoint、timeout、PNG/Base64
  限制、diagnostics 与既有 unavailable/no-retry mapping。没有新 production 文件或第三层 facade。
- `AutoCombatPanelService.java:111` 的默认构造只引用上述 canonical API；Service 中
  `CloudOcrWordsReader`、`HttpClient/HttpRequest/HttpResponse`、`ObjectMapper/JsonNode`、Base64、endpoint、timeout、
  PNG encode codec 静态总命中为 **0**。整个 `LocalOcrClient` 中 `HttpClient.newBuilder(...)` 仍恰好 **1** 处。
- ordered input 的单一边界返回 private `COMPLETED/FAILED` 类型；exact outcome window、step count/index/type、
  COMPLETED/FAILED step status shape、无 input frame 仍逐项验证。capture 调用不允许 known failure，capture `FAILED`
  继续 fatal。
- `AutoCombatPanelService.java:199`：open Alt+8 已知 `FAILED` 记录 missing 并返回 `null`，不 re-observe、不 retry、
  不重置 rounds/timestamp；`AutoCombatPanelService.java:332`：refresh Alt+8 已知 `FAILED` 返回 `false`，保留此前
  visible estimate 与旧 refresh timestamp；`AutoCombatPanelService.java:241`：drag 已知 `FAILED` 后继续同一次且仅一次
  full-window re-observe，miss 时沿用既有 drag-target fallback，不追加 input/action/retry。
- terminal 顺序保持：command `INTERRUPTED_UNCERTAIN` 先 checkpoint；outcome `STOPPED` 在
  `AutoCombatPanelService.java:631` 先走 `TaskCheckpoint`，未确认 stop 后 fatal；
  `DUPLICATE_OR_UNCERTAIN` 与其它 non-completed/uncertain 继续 fatal，均不折叠为 false/success。
- Service 静态统一调用点仍为 `TurnGameClient.capture=1`、`TurnGameClient.execute=1`；Alt+8 仍为单 ordered action
  `KEY_TAP(ALT_8)+WAIT`，drag 仍为单 ordered action `DRAG_LEFT+WAIT 500ms`。无第二 exchange、auto retry、
  session/ledger/TTL/fallback transport。

### Named test source 证据

- `AutoCombatPanelTurnContractTest.java:153` 直接覆盖 open/refresh 已知 `FAILED` 的 `null/false`、missing/estimate/
  timestamp 不伪成功及零 retry；`AutoCombatPanelTurnContractTest.java:215` 覆盖 drag `FAILED` 后恰好一次 full-window
  re-observe，再以 exact fallback rounds ROI 继续，且 input command 总数仍为 1；
  `AutoCombatPanelTurnContractTest.java:445` 直接覆盖 ordered input `STOPPED` 的 checkpoint/fatal 分流及 uncertain fatal，
  均无后续 command。
- 原 raw PNG、packaged template/HSV、exact rounds ROI/red wash/OCR、绝对 drag 坐标、500ms/1000ms、rounds
  estimate/timestamp/combat-exit/missing-attention/team burst、BC4 command/outcome/malformed frame、UUID/单 command
  覆盖保留；capture `FAILED` fatal 已在 `AutoCombatPanelTurnContractTest.java:388` 明确区分。
- 每个 scripted `TurnGameClient` invocation 仍消费一个 command reply并持有唯一 UUID；所有返修 failure case 都断言
  command 数，无额外 action/retry。

### 只读静态门

- 三文件花括号计数分别为 `248/248`、`48/48`、`102/102`。
- Service 对旧 authority/local 路径静态零命中：`AUTO_COMBAT_PANEL`、`GEOMETRY`、`readWindowFact`、
  `executeInputBundle`、`GameClientTracker`、`WindowScopedTempPath`、`InputProvider`、`InputSequences`、
  `TextRecognizer`、`java.nio.file`、`Path.of`、`Files.`。
- 已执行只读文本断言并全部 PASS：single capture boundary、single execute boundary、canonical OCR reference、
  no Service OCR HTTP codec、one canonical OCR HTTP client、one public joined-text API、open/refresh/drag known FAILED
  mapping、STOPPED checkpoint、uncertain fatal、9 named tests、3 组 Repair #1 直接回归。
- 已再次对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 open/refresh/drag 返回与 fallback 顺序；
  **无已批准业务差异；按基线等价迁移。**

### 未运行门与父级接手点

- 严格按卡，本 Worker **未运行** Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或
  input；以上均不是通过声明。
- 本 Worker 未执行任何 Git mutation，未触碰 exact write set 以外的 dirty/untracked。
- 请父级执行 Repair #1 source/test-source 复审；复审通过后再由父级统一进入 writer-stable named-test/compile cohort。

<!-- TRUE_EOF: TURN-20 REPAIR #1 SOURCE+TEST SOURCES REDELIVERED; PARENT RE-REVIEW PENDING -->

## PARENT RE-REVIEW #1 - 2026-07-15T22:27:36-04:00

- 结论：`SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD BUILD PENDING`；父级独立复审
  `P0/P1/P2=0/0/0`。这不是 `CARD APPROVED/CLOSED`，不得绕过点名测试和适用 build gate。
- P1-1 已关闭：`AutoCombatPanelService.java:173-217,220-266,294-338,590-676` 现在只把结构严格合法的
  ordered-input `FAILED` 映射为 baseline known failure；open 返回 `null` 并记录 missing，refresh 返回 `false`
  且不重置状态，drag 仍只执行一次原有 re-observe/fallback。STOPPED/uncertain 继续 checkpoint/fatal，没有
  第二 command 或自动 retry。与 `696a12b0` 的三个 caller-visible 返回/顺序一致。
- P1-2 已关闭：Service 默认构造仅调用 `LocalOcrClient.readJoinedText(...)`；新增 API 在
  `LocalOcrClient.java:57-77` 直接复用既有 `readWords`、唯一 HTTP client、endpoint、限制和 diagnostics。
  Service 内不再有第二 HttpClient/OCR codec/config。
- named test source 在 `AutoCombatPanelTurnContractTest.java:153-249,388-479` 直接覆盖 known FAILED 的
  `null/false/re-observe`、STOPPED checkpoint、uncertain fatal、单 UUID/command 与零 retry；原 raw PNG、ROI、
  500/1000ms、round/state 顺序覆盖保留。
- SHA-256 与 Repair #1 交付一致：Service `E32C...0982`、LocalOcrClient `F706...934D`、test
  `D601...61DD8`。owner 已释放；所有 Java writers 稳定后由父级运行本卡 named test 与适用 Cloud build。

<!-- TRUE_EOF: TURN-20 REPAIR #1 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
