# CR271 TURN-20 delivery preflight helper

## 角色与只读边界

- 身份：`CR271 TURN-20 delivery preflight helper`，不是 reviewer；本文只列静态证据、交付风险和父级复核点，不作最终裁决。
- 本轮未修改 Java、测试、权威计划、CR 卡或 `docs/ACTIVE_WORK.md`，未运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，未执行 Git mutation。
- 已完整读取：
  - `D:/mavenProject/DHXY/AGENTS.md`（392 行）；
  - `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`（1326 行）；
  - `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`（1492 行）；
  - `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-20.md`（117 行，真实 EOF 为第 117 行）；
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`（1082 行）；
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatPanelTurnContractTest.java`（1090 行）；
  - `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`（556 行，blob `bf63d2c78873afd8a0781d97f080a59b2b327942`）。
- TURN-20 权威边界证据：总计划 `:541-546`、审计后注册表 `:971`、精确 production write set `:1093`、named test/profile `:1427`、测试硬门 `:1265-1289`、状态机 `:1479-1492`；固定报告 frozen brief `:3-38`、claimed true EOF `:40-49`、delivery true EOF `:51-117`。

## 交付物静态对账

- 固定报告 `:55-67` 所列规模与 SHA-256 和当前磁盘一致：
  - production：49171 bytes，SHA-256 `28ea03e2cddbce0a54310baac89ea341eb31922d4c1de31815345e038b11b742`；
  - test：48226 bytes，SHA-256 `7e1cc3259c8df334cc8c512b843be413c119616c3c8e9267f7ad7cec787c40b4`。
- 基线 public 业务 surface 位于 baseline `:69-93`、`:158-172`、`:259-267`、`:471-536`；当前对应 surface 位于 production `:140-177`、`:268-290`、`:374-383`、`:889-965`。方法名、返回类型、两个 enum、record 和 team guard 的源码级业务 surface 对齐。
- exact metadata/ROI/raw PNG：production `:530-570` 从 `TurnGameClient.capture(...)` 接收 raw frame，并校验 outcome、purpose、sourceStepIndex、region、SHA、PNG signature/decode、像素尺寸；`:647-659` 只从当前 bound client 取 metadata 并校验 device/window/rect。
- Cloud visibility/round/drag/refresh：production `:180-220` 保持“观察 -> miss 后 Alt+8 -> 再观察”；`:222-265` 保持位置换算、`distance > 20`、drag、再观察；`:293-331` 先提交 visible estimate、再解 refresh reason、输入成功后才 reset；`:334-340` reset 为 `25` 并写 timestamp；`:375-383` 每次 combat exit 减 `3`。
- ordered input：production `:842-858` 将 `KEY_TAP(ALT_8)` 与 WAIT 放在一次 `execute(...)` 的 ordered steps；`:860-872` 将 screen-absolute `DRAG_LEFT` 与 `WAIT 500ms` 放在一次 ordered action。测试 `:130-147`、`:150-177`、`:180-220` 对 700ms/1000ms Alt+8、绝对 drag 点与 500ms 做了正向断言。
- terminal/frame correlation：production `:591-645` 对 command 非 completed、outcome FAILED/STOPPED/uncertain、完整 metadata、step count/index/type/status、unexpected/missing frame fail closed；测试 `:315-393` 覆盖 capture invocation 的 failed/stopped/uncertain/busy/duplicate、错 action/window/step、缺 frame、错 region/SHA/purpose/sourceStepIndex/像素尺寸和坏 PNG。
- 每 invocation 一个 UUID/command：production 只有 `capture(...)` `:536-539`、`execute(...)` `:578-581` 与只读 metadata `:648` 三个 TurnGameClient 调用点；UUID 由 `TurnGameClient.java:161-168` 每次 action invocation 生成一次并调用 command port 一次。测试 `:867-878` 校验所有 actionId 为互异 UUID、`executeCalls == actions.size()`、exact device/window，scripted port `:1071-1079` 每次只消费一个 reply。
- 对 production 的 scoped symbol scan 未发现 `AUTO_COMBAT_PANEL`、`GEOMETRY`、`WindowFact`、`readWindowFact`、`executeInputBundle`、`RemoteGameClientPort`、`CloudGameClient`、`GameClientTracker`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`TextRecognizer`、`InputProvider`、`InputSequences`、`CoordinateHelper`、`ImagePreprocessor`、`Files`/`Path` 或 direct UUID。源码没有 action retry 循环；`retry` 仅出现在 baseline 日志文案和 public `retryAfterMs` 值对象语义。

## PRECHECK 风险与父级复核点

### R1 - test write set 当前被 ignore，production 仍为 untracked

- 证据：`D:/mavenProject/dhxy-cloud-brain/.gitignore:14-16` 整体忽略 `src/test/`；固定报告 `:112-113` 也已记录该事实。只读 scoped status 显示 production 为 `?? src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`，test 因 ignore 不出现在普通 status 中。
- 影响：父级若按普通 status/stage 收集交付，named test 可被静默遗漏；当前 SHA 只证明磁盘内容，不能证明最终交付集合包含它。
- 建议父级复核：按 frozen write set 显式收集这两个绝对路径，重新核对上述 SHA，并用能纳入 ignored test 的既定 Git 流程确认 test 被保留；不要按整个 `src/main/java/com/bot/` 目录宽收。

### R2 - “全部现有 public API”对 constructor 的口径未闭合

- 证据：baseline `:30-33` 使用 public 默认访问级别的 Lombok `@RequiredArgsConstructor`，其 required fields 为 `:57-65`；当前 production 改为显式 public 五依赖 constructor `:105-120`。固定报告 `:71-73` 的 public surface 清单没有列 constructor。
- 测试缺口：named test 不走 public constructor；它在 test `:454-480` 反射调用 private 七参数 constructor，因此既不约束 public constructor signature，也不证明 production OCR wiring。
- 建议父级复核：明确 frozen brief `:13-15` 的“保留全部现有 public API”是否包含 Spring constructor。若 constructor 属于兼容 surface，要求给出有意识的迁移口径及 caller/bean 证据；若明确排除 constructor，也应把该排除写进父级结论，不能由测试的 private seam 代替。

### R3 - baseline missing-attention 的窗口 warning/metrics 副作用未被等价承接

- 证据：baseline `:220-247` 在 10 分钟/60 秒门后除 `log.error` 外，还执行 `markRuntimeWarning(...)` 和 `automationMetricsService.recordWindowWarning(...)`（尤其 `:241-247`）。当前 production `:342-363` 只更新内部时间并写 `log.error`，没有 warning/metric 的 Cloud 等价输出。
- 测试缺口：test `:279-312` 只反射断言 `autoPanelMissingSinceAt`、`lastAutoPanelMissingAttentionAt` 和 clear，不断言任何用户可见 attention signal 或 metric。
- 建议父级复核：对照 frozen brief `:13-15` 的 `missing attention` 明确“只保留日志”是否已经获得业务授权。若没有，先确定不引入 DHXY mechanics 的 Cloud 等价承接点；若日志被认定为等价，父级需明确记录该解释及可观测性后果。

### R4 - production OCR 新建第二个 Cloud OCR transport，且 named test 完全绕开该路径

- 证据：现有 `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/LocalOcrClient.java:20-31` 自述为 Cloud Brain 所有 OCR 调用的 single choke point，并在 `:40-52`、`:73-130`、`:139-196` 维护协议/loopback/model identity、diagnostics 与 no-retry fail-closed。TURN-20 production 另建 private `CloudOcrWordsReader` `:995-1080`、第二个 static `HttpClient` `:997-1000`，并在 `:1026-1063` 自行编码/POST/解析。
- 测试缺口：public constructor 在 production `:105-119` 才绑定 `CloudOcrWordsReader::readText`；named test 通过 private constructor 注入 `Function<BufferedImage,String>`（test `:426-480`），所以正向 round 用例 `:180-220` 只证明注入字符串能驱动 decision，不证明 production OCR endpoint、一次 HTTP request、response parser、timeout/interruption、identity/diagnostics 或无 retry。
- 建议父级复核：先裁定 frozen write set 是否允许第二 OCR transport。若不允许，需由父级调整可用 Cloud OCR facade/写集后返修；若允许，named test 至少要用 loopback 走 public production wiring，逐项断言一请求、零 retry、成功 words、非 2xx、坏 JSON、`ok=false`、timeout/interruption 的状态后果。

### R5 - failed/stopped/uncertain 只覆盖第一条 CAPTURE，未覆盖 Alt+8/drag/refresh INPUT 终态和状态顺序

- 证据：failure matrix test `:315-369` 对每个 case 直接把失败 reply 放在第一次 `ensurePanelVisible(...)` command，因而全部停在首个 full-window CAPTURE。drag 用例 `:150-177`、low-round refresh `:180-220`、refresh-due/unknown `:223-256` 都只脚本化 INPUT completed。
- 生产风险面：open-panel INPUT 后才允许 re-observe/reset（production `:193-219`）；drag INPUT 后才允许 re-observe/`panelAligned=true`（`:235-265`）；refresh INPUT 后才允许 estimate/timestamp reset（`:317-340`）；input action unexpected frame 由 `:639-642` 拒绝。上述失败短路均未被 named test 锁定。
- 建议父级复核：要求分别增加“初次 miss 后 Alt+8 FAILED/STOPPED/uncertain”“drag FAILED/STOPPED/uncertain”“round refresh FAILED/STOPPED/uncertain/unexpected frame”用例；每例断言 command 数不增加、后续 capture 不发生、`panelAligned`/estimate/timestamp/missing state 不伪造成功。

### R6 - exact metadata 读取次数/漂移换算未形成测试断言

- 证据：production 在 visibility/capture、alignment、rounds、refresh 前分别读取 latest metadata（`:180-194`、`:222-234`、`:453-483`、`:530-570`、`:647-659`），并用 `translatePoint(...)` `:726-735` 将旧 frame 点换算到最新 rect。test scripted port 维护 `metadataReads`（test `:1060-1087`），但全文件没有任何 `metadataReads` assertion；所有正向用例使用同一 `WINDOW_RECT`。
- 已覆盖边界：test `:331-350` 的 wrong-window case 能证明 outcome rect 不同会 fail closed；这不能证明 capture 后窗口 rect 漂移时 drag/round ROI 使用最新 rect 的换算顺序。
- 建议父级复核：增加 metadata 脚本序列，断言每个 action 前的读取次数与顺序，并覆盖 capture rect A -> action rect B 的绝对 drag/round ROI；同时确认 stop/pause flag 在 command 往返期间变化时预期是 checkpoint 还是 metadata mismatch。

### R7 - `waitAfterOpenMs == 0` 时 ordered step 形状偏离 baseline

- 证据：baseline `:107-111` 无条件提交 `pressAlt8()` 后 `sleep(waitAfterOpenMs)`；当前 production `:842-858` 在 `waitMs == 0` 时只返回 KEY_TAP，删除 WAIT step。public API `ensurePanelVisible(String,int)` 没有限制该参数必须为正（baseline `:90-93`；current `:168-177`）。
- 测试缺口：named test 只断言 700ms 与 1000ms（test `:130-147`、`:180-220`），没有 0ms。
- 建议父级复核：检查全部实际 caller 是否永远传正值，并裁定 frozen brief 的“严格保留 baseline step 顺序和 delay”是否要求保留 `WAIT 0`。若要求 exact shape，补 0ms contract case；若不要求，父级需记录该可见差异为何不构成业务差异。

### R8 - shared dirty tree 中存在 write set 外第二个 public panel decision authority

- 证据：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java:6-16` 自述为较早的 W-ACP lift-and-shift，且是独立 public production class；`:26-141` 重复 refresh reason、missing state 与 team burst guard。该文件当前也是 untracked，实际 TURN-20 service 没有引用它。
- 影响：不能据 shared dirty tree 推断它由 TURN-20 worker 创建，但若父级宽收 `src/main/java/com/bot/`，会把 frozen write set 外的第二算法文件混入 TURN-20；也会削弱“新 model/algorithm 只能 private nested type”的交付可审计性。
- 建议父级复核：把该文件明确归属到既有其他工作或后续清理，不将其计入 TURN-20；确认 production caller 只消费实际 `AutoCombatPanelService` 的同名 public decision surface。

### R9 - named test 与 Cloud compile 尚无 fresh 执行证据

- 证据：固定报告 `:108-115` 明确未运行任何 Maven/JUnit/compile；本 helper 也受同一禁令，没有执行。测试源码包含真实 packaged templates、OpenCV path 和 reflective constructor（test `:426-480`、`:953-1053`），静态读取不能替代 fresh execution。
- 建议父级复核：Java writers 稳定后按总计划 `:1284-1289`、`:1427` 运行 Cloud named command `mvn -q -Dtest=AutoCombatPanelTurnContractTest test`，记录完整命令、exit code、tests run/failures/errors，再运行适用 Cloud compile cohort；同时先确认 ignored test 已实际进入测试源集合。

RISKS_FOUND
