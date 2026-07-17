# TURN-06 DHXY HTTP/2 Turn Client Worker Report

## CLAIMED

- 状态：`CLAIMED`
- Worker：Internal implementation Worker（仅 TURN-06；非 manager/reviewer）
- 领取时间：`2026-07-15 14:47:11 -04:00`
- 类型：`INFRA`
- `countDelta=0`
- `startDependsOn`：`TURN-00`（已 CLOSED）
- `approvalDependsOn`：`TURN-01D`（待父级依赖汇合）
- DHXY 基线：branch `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`
- Cloud 只读基线：branch `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`

### 精确写集

唯一 Java 写集只允许以下六个 DHXY 新文件：

1. `src/main/java/com/bot/dhxy/cloud/turn/TurnClient.java`
2. `src/main/java/com/bot/dhxy/cloud/turn/HttpsTurnClient.java`
3. `src/main/java/com/bot/dhxy/cloud/turn/TurnMultipartBody.java`
4. `src/main/java/com/bot/dhxy/cloud/turn/TurnExchangeResult.java`
5. `src/main/java/com/bot/dhxy/cloud/turn/TurnTemplateDownload.java`
6. `src/main/java/com/bot/dhxy/cloud/turn/TurnTransportException.java`

本报告仅追加。禁止修改 protocol、`pom.xml`/config、Cloud/server/routes、Service、主计划、CR271、
`ACTIVE_WORK.md`、dashboard 及其他 worker 的 dirty/untracked 文件。

### 领取合同

- 本地主动 `POST /api/v1/client/turn`；无 PNG 使用 JSON，有 PNG 使用 `metadata` JSON + `frame` raw PNG multipart。
- 非 loopback 强制 HTTPS；复用项目现有 JDK HTTP/Jackson 依赖，不扩大 `pom.xml`。
- 响应严格 bounded；客户端内部不做 transport 或业务自动 retry。
- 任一合法 `200 TurnResponse` typed 表达 carried `previousOutcome` 已被服务端接受，包括 `IDLE`。
- 模板 GET typed 表达 `200`/`304`、ETag、SHA-256 与 PNG；本卡不负责模板落盘缓存。
- 网络、HTTP、响应上限和 JSON parse/contract 错误抛 typed `TurnTransportException`，不得伪造业务 outcome。

### 两仓领取时 `git status --short --branch`

DHXY：

```text
## thin-client-design
 M config/dialog_choice_memory.json
 D config/maps.json
 M docs/ACTIVE_WORK.md
 M docs/DHXY_CONTEXT.md
 M docs/HYBRID_CLOUD_WORKFLOW.md
 M docs/PACKAGE_ARCHITECTURE.md
 M docs/cr-dashboard-data.js
 M docs/superpowers/specs/2026-07-12-full-cloud-thin-client-architecture-draft.md
 M docs/superpowers/specs/2026-07-12-service-migration-matrix.md
 M docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md
 M docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md
 M docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md
 M docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md
 M pom.xml
 M src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudDecisionService.java
 M src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudRequest.java
 M src/main/java/com/bot/dhxy/input/action/InputActionQueue.java
 M src/main/java/com/bot/dhxy/input/action/InputActionRequest.java
 M src/main/java/com/bot/dhxy/input/action/InputActionScope.java
 M src/main/java/com/bot/dhxy/input/action/InputActionWorker.java
 M src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java
 M src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java
 M src/main/java/com/bot/dhxy/service/AutoCombatService.java
 M src/main/java/com/bot/dhxy/service/BagService.java
 M src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java
 M src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java
 M src/main/java/com/bot/dhxy/service/NavigationService.java
 M src/main/java/com/bot/dhxy/service/NpcClickService.java
 M src/main/java/com/bot/dhxy/service/PlayerStateService.java
 M src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java
 M src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java
 M src/main/java/com/bot/dhxy/tools/CoordinateHelper.java
 M src/main/java/com/bot/dhxy/tools/ImagePreprocessor.java
 M src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java
 M src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java
?? docs/superpowers/plans/2026-07-12-direct-cloud-lift-and-shift.md
?? docs/superpowers/plans/2026-07-12-remote-task-run-lifecycle.md
?? docs/superpowers/plans/2026-07-12-thin-client-design-closure.md
?? docs/superpowers/plans/2026-07-13-dialog-choice-memory-cutover-runbook.md
?? docs/superpowers/plans/2026-07-13-direct-service-input-bundle-migration.md
?? docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md
?? docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md
?? docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md
?? docs/superpowers/plans/briefs/
?? docs/superpowers/plans/reports/
?? docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md
?? docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md
?? docs/新手任务流程草案.md
?? images/template/xinshou/
?? src/main/java/com/bot/dhxy/cloud/remote/
?? src/main/java/com/bot/dhxy/cloud/turn/
?? src/main/java/com/bot/dhxy/core/TextRecognizer.java
?? src/main/java/com/bot/dhxy/input/action/InputActionExecutionResult.java
?? src/main/java/com/bot/dhxy/input/action/InputActionSafetyReason.java
?? src/main/java/com/bot/dhxy/model/ocr/OcrWordResult.java
?? src/main/java/com/bot/dhxy/service/autocombat/
?? src/main/java/com/bot/dhxy/service/bag/
?? src/main/java/com/bot/dhxy/service/battleradar/
?? src/main/java/com/bot/dhxy/service/commonbox/
?? src/main/java/com/bot/dhxy/service/dialog/DialogDetectionLocalMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrImageLocalObservationMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogPreparedActionValidationLocalMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogStoryAdvanceLocalMacroMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java
?? src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java
?? src/main/java/com/bot/dhxy/service/npc/
?? src/main/java/com/bot/dhxy/service/playerstate/
?? src/main/java/com/bot/dhxy/service/tasktracker/
?? src/main/java/com/bot/dhxy/service/teamreturn/
?? src/main/java/com/bot/dhxy/vision/SheyaoxiangDigitTemplateReader.java
```

Cloud（只读）：

```text
## navigation-migration
 M pom.xml
 M src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java
 M src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java
 M src/main/java/com/yueyunfe/dhxy/cloudbrain/ImageAlgorithms.java
 M src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java
 M src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java
 M src/main/java/com/yueyunfe/dhxy/cloudbrain/gateway/CloudApiGateway.java
 M src/main/resources/config/maps.json
?? logs/
?? migration-baseline/
?? migration-preserved/
?? scratch_baseline_MapNameCanonicalizer.java
?? src/main/java/com/bot/
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudNativeImageProcessor.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/MiniMapPointResolver.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/NavigationRoutePlanResolver.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/PackagedTemplateAssets.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/SheyaoxiangStatusDecisionFacade.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/api/RemoteTaskRunEndpoint.java
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/host/
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/
?? src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/
?? src/main/resources/images/template/battle/auto_remaining.png
?? src/main/resources/images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png
?? src/main/resources/images/template/dialog/maintenance/maintenance_repair_confirm_raw.png
```

## SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15 14:53:07 -04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- `countDelta=0`

### 精确实现

- `TurnClient`：闭合为两个传输入口：单次 turn exchange，以及带 nullable `If-None-Match` 的单模板 GET；
  两者只返回 typed transport result，失败只抛 `TurnTransportException`。
- `HttpsTurnClient`：
  - 构造一个复用的 JDK `HttpClient.Version.HTTP_2` client，禁用 redirect；connect/request timeout 均要求为正；
  - base URI 只接受 HTTPS，或 host 精确为 `127.0.0.1`、`localhost`、`::1`/`[::1]` 的 loopback HTTP；
    拒绝 user-info、非根 path、query 与 fragment；
  - 单次 `POST /api/v1/client/turn`：无 frame 使用 `application/json`，有 frame 使用两 part multipart；
    源码中只有一个共享 `httpClient.send(...)` 调用点，没有 send/reconnect/retry 循环；
  - request metadata 限 `256 KiB`，PNG 限 `8 MiB`；frame metadata 与 PNG 强制同现同缺，并核验 PNG signature、
    `image/png` 与 SHA-256；
  - response 使用 `BodyHandlers.ofInputStream()` 做 Content-Length 预检和实际流式限长，不使用 unbounded
    `ofByteArray/ofString` response handler；
  - 只接受 `200 application/json`；Jackson defensive copy 启用 unknown field、trailing token、duplicate field
    严格失败；`ACTION` 必须含 action 并交 `TurnProtocolValidator`，`IDLE` 必须无 action；
  - 任一合法 200 返回 `TurnExchangeResult(..., PreviousOutcomeStatus.ACCEPTED)`，包括 `IDLE`；网络不确定、
    interruption、HTTP 非 200、过大、content-type、parse/contract 错误均 typed exception，不伪造 outcome；
  - 模板 key 作为一个 UTF-8 percent-encoded path segment 请求 `/api/v1/templates/`；只接受 200/304；
    304 必须无 body 且 ETag 精确等于请求 `If-None-Match`；200 必须 `image/png`，并以实际 PNG bytes 重算
    SHA-256，与精确 quoted `"sha256:<64 lowercase hex>"` ETag 一致。
- `TurnMultipartBody`：按冻结合同精确写入 `metadata` JSON part 与 `frame` raw PNG part、CRLF 和 closing
  boundary；PNG 不进行 Base64 或其它转换；随机 boundary 在 metadata/PNG 中做冲突扫描后才采用。
- `TurnExchangeResult`：闭合 acknowledgement 枚举只有 `ACCEPTED`，不允许构造“合法 response 但上一 outcome
  未接受”的矛盾结果。
- `TurnTemplateDownload`：闭合 `OK_200` / `NOT_MODIFIED_304`，携带 HTTP status、ETag、同一 SHA-256 与 nullable
  raw PNG；构造时校验 ETag/SHA 一致并防御性复制 PNG。
- `TurnTransportException`：闭合配置、request contract、serialization、network、interrupted、HTTP status、
  response bound/content-type/parse/contract、template hash mismatch 类别；可携带 HTTP status，不携带业务 outcome。

### SHA-256

```text
DCB4D58802157AA934C12F2DDEC68DAE845A8E37CB780458046E29BCCD8DB0E1  TurnClient.java
0848A1ABB624BDB4D74DD3935A75FCFA2236E40F0BC31E534AA5CABB5617F143  HttpsTurnClient.java
FF4D3570CCAE87FC4885E59F54651F3DFF2C14146AAD2FC50D16FC1D69F0666E  TurnMultipartBody.java
B94EACB157DCDD7134D6333C90822A82D3D05981C020EEC9464B703AFDCF8DB4  TurnExchangeResult.java
5584F0E55B52A454477788ABCABB7F989D6664F949BC08122F7C45CB552B8A2D  TurnTemplateDownload.java
0EB62229670237BCAA39BD04067F51AA1C180C2ABFFE69B6F9C2CF81CB9FD31B  TurnTransportException.java
```

### Scoped diff/check

- `git status --short --untracked-files=all -- <六个精确 Java 文件> <本报告>`：只列出六个新 Java 文件与本报告；
  没有修改 protocol、Maven/config、Cloud、server/routes、Service、主计划、CR271、`ACTIVE_WORK.md` 或 dashboard。
- 六文件逐个 `git diff --no-index --check -- NUL <file>`：除 Windows 预期 LF/CRLF 提示和 no-index
  “内容不同” exit `1` 外，无 trailing whitespace、space-before-tab 或 EOF whitespace 诊断。
- 合同位置扫描确认：HTTP/2、redirect NEVER、精确 turn/template path、`256 KiB`/`8 MiB` bounds、
  `BodyHandlers.ofInputStream()`、validator、typed accepted、typed 200/304、两 part disposition 与 `image/png`
  均存在。
- 禁用能力扫描：无 `java.util.Base64`/`Base64.*` 调用，无 WebSocket/raw socket/scheduler/auto-start/sendAsync，
  无 input/Service/runtime 引用；`httpClient.send(...)` 调用点 `1`，network/business retry loop `0`。

### 未运行项与依赖

- Maven：未运行。当前其他 Foundation Java writers 仍活动，且 approval dependency `TURN-01D` 的
  `TurnProtocolValidator.java` 尚未落地；本实现按冻结接口调用其 `requireValid(TurnRequest)` 与
  `requireValid(TurnAction)`。遵守并行 build cohort 规则，不对半成品整仓运行 Maven，不越界补 validator。
- Tests：未创建、恢复或运行任何 automated/replay/source-guard test。
- Runtime：未启动 DHXY/Cloud application、server、Task、poller、UI、capture 或 input；未发送任何网络请求。
- Git：未 stage、commit、checkout、reset、clean、删除或覆盖他人文件；Cloud 仓全程只读。
- 业务差异：无已批准业务差异；按基线等价迁移。

### Worker 停止点

TURN-06 源码已交付，等待父级源码审查与 Foundation build cohort。当前 worker 不是 manager/reviewer，
不写 `APPROVED/CLOSED`、不自批、不领取下一卡。

## Parent Source Review #1

- 审查时间：`2026-07-15T15:00:00-04:00`。
- 父级独立读取并审查六个精确 Java 文件，不以 Worker 自述代替源码裁决。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`。
- 证据：非 loopback 强制 HTTPS；复用单一 HTTP/2 client；turn/template 均只有一个共享
  `httpClient.send(...)` 调用点且无 retry loop；PNG 以 raw multipart 传输且 request/response 分别受
  `256 KiB`/`8 MiB` 上限约束；合法 `200 ACTION/IDLE` 均显式返回 previous-outcome `ACCEPTED`；
  template `200/304` 的 ETag、SHA-256、PNG 合同 fail-closed；transport failure 不伪造业务 outcome。
- 影响：`TURN-07` 的 `startDependsOn=TURN-03B+TURN-06` 已满足，可立即并行领取；TURN-06 的源码
  owner 释放，不等待 cohort Maven 才开下一卡。
- 构建门：当前仍有 Java writers，按批准的 cohort 规则暂不运行 Maven；待 writers 稳定后由父级统一执行。
- 业务差异：无已批准业务差异；按 `696a12b0` 基线等价迁移。
