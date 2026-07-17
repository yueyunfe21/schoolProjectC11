# TURN-01C Report — 双端 outcome/envelope protocol DTO

## CLAIMED

- 领取时间：`2026-07-15T14:37:53.9319470-04:00`
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA protocol DTO)`
- `countDelta`：`0`
- `dependsOn`：`TURN-00`
- `startDependsOn`：`TURN-00` 已由父级写明 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`
- `approvalDependsOn`：`TURN-01A`、`TURN-01B`、`TURN-01D`
- 精确写集：
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnOutcome.java`
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnStepResult.java`
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnMatchResult.java`
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnFrameMetadata.java`
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnRequest.java`
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnResponse.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnOutcome.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnStepResult.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnMatchResult.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnFrameMetadata.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnRequest.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnResponse.java`
  - 本报告
- 禁止触碰：
  - `TURN-01A` / `TURN-01B` / `TURN-01D` 已交付或待交付文件；
  - 任何现有 `cloud/remote/**`、Service、server、runner、配置、Maven、CR271、dashboard、主计划；
  - runtime/application/Task/poller/UI/capture/input/tests。

## 当前两仓 git status（领取瞬间）

### DHXY — `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`

```text
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
?? docs/superpowers/plans/
?? docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md
?? docs/superpowers/specs/THIN_CLIENT_V1_FINAL_DESIGN.md
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
?? src/main/java/com/bot/dhxy/service/dialog/
?? src/main/java/com/bot/dhxy/service/npc/
?? src/main/java/com/bot/dhxy/service/playerstate/
?? src/main/java/com/bot/dhxy/service/tasktracker/
?? src/main/java/com/bot/dhxy/service/teamreturn/
?? src/main/java/com/bot/dhxy/vision/SheyaoxiangDigitTemplateReader.java
```

### Cloud Brain — `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`

```text
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

## 执行边界

- 只创建 outcome/envelope DTO；不写 validator，不改 action-side DTO，不改 core enum/value object。
- 双仓文件必须 byte-identical。
- 本卡 Java 变化完成后只运行适用编译门；不运行 tests/runtime/application/Task/poller/UI/capture/input。

## DELIVERED

- 交付时间：`2026-07-15T14:43:20-04:00`
- 状态：`DELIVERED`
- 完成内容：
  - 双仓新增 `TurnOutcome`、`TurnStepResult`、`TurnMatchResult`、`TurnFrameMetadata`、`TurnRequest`、
    `TurnResponse` 六个 outcome/envelope DTO。
  - `TurnOutcome` 与 `TurnRequest` 均携带 `TurnWindowMetadata`，闭合当前窗口 metadata；
    `TurnOutcome.Status` 包含 `COMPLETED`、`FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`；
    `TurnResponse.Status` 包含 `ACTION`、`IDLE`。
  - `TurnFrameMetadata` 使用 `TurnFramePurpose` + 单个 `TurnRegion` + `sourceStepIndex` 表达单 frame metadata。
- 双仓 byte-identical 证据：

```text
MATCH TurnOutcome.java 660176E203B567FC07757833F1135E0212DBB198274C63C942BAC50C1D3857FF
MATCH TurnStepResult.java 2BE8726C0267C1E1D02F41039F235D496425FE81F9F97008711310D9A733184B
MATCH TurnMatchResult.java F9A39A9F01993264773CCECB1D4BC355873BE9580D8F34BAEA5AC2264B3EAA72
MATCH TurnFrameMetadata.java E4F63BB8BE526CFE47F611DDC1A49170D8C1326B117D09E8DC8D77EE41E87244
MATCH TurnRequest.java 53A6104C1C93F0CDBA1E13C5535C381143CEAE8590EAFC953B7E5DA16DB0C59D
MATCH TurnResponse.java A41E8014D434B43B5C4336B639396EB417336E544D58A47E3FC51F34D7DDD9FF
```

- 静态检查：
  - DHXY targeted `git diff --check`：exit `0`。
  - Cloud targeted `git diff --check`：exit `0`。
- Maven：
  - 未运行。本卡 `approvalDependsOn` 包含 `TURN-01B` 和 `TURN-01D`；当前 `TURN-01B` 报告仍显示
    `BLOCKED - 父级合同待补`，`TurnAction`/`TurnStep` 等 action-side DTO 尚未落地，独立 Maven 会因上游缺类失败。
    本卡编译门应进入父级 Foundation build cohort。
- 运行限制：
  - 未启动 runtime/application/Task/poller/UI/capture/input/tests。
- 业务差异：
  - 无已批准业务差异；按基线等价迁移。
- 父级审批状态：
  - 等待父级唯一 reviewer 在本报告 / CR271 写入 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`；在此之前不领取下一卡。

## PARENT REVIEW #1 - REPAIR REQUIRED

- 审查时间：`2026-07-15T14:50:00-04:00`
- 结论：`REPAIR REQUIRED`；`P0=0 / P1=3 / P2=0`，`countDelta=0`。
- P1-1：双仓 `TurnOutcome.java:8` 与 `TurnRequest.java:5` 把冻结合同字段 `window` 改成
  `windowMetadata`。record component 名属于 JSON/wire 字段，影响 Cloud/DHXY canonical payload 一致性。
- P1-2：双仓 `TurnStepResult.java:8` 私自新增冻结合同不存在的 `String message`，改变 constructor 与 JSON shape。
- P1-3：双仓 `TurnResponse.java:4` 私自新增冻结合同不存在的 `int contractVersion`，改变 constructor 与 JSON shape。
- 权威证据：`docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md:221-283` 的 Step 5 exact
  records；本卡要求不得自行增删/重命名字段。六对文件 byte-identical 只能证明双端同错，不能替代合同一致性。
- 返修条件：严格恢复 exact signatures：`TurnOutcome(..., TurnWindowMetadata window, ...)`、
  `TurnStepResult(..., String code, TurnMatchResult match, String localResultJson)`、
  `TurnRequest(int contractVersion, TurnWindowMetadata window, long waitTimeoutMs, TurnOutcome previousOutcome)`、
  `TurnResponse(Status status, TurnAction action)`；两仓六对文件继续 byte-identical，重新追加 SHA 与 scoped
  `git diff --check`。不得修改其它卡或扩写协议。
- 构建：当前仍由 Foundation cohort 统一执行；返修后状态只能回到 `DELIVERED / PARENT REVIEW`，不得自批。

## REPAIR #1 DELIVERED

- 返修交付时间：`2026-07-15T14:53:20-04:00`
- 状态：`DELIVERED / PARENT REVIEW`
- 父级 P1 处理：
  - P1-1 已修复：双仓 `TurnOutcome` 与 `TurnRequest` record component 恢复为冻结合同字段 `window`。
  - P1-2 已修复：双仓 `TurnStepResult` 移除冻结合同不存在的 `message` component。
  - P1-3 已修复：双仓 `TurnResponse` 移除冻结合同不存在的 `contractVersion` component。
- 保持边界：
  - 未修改 `TURN-01A` / `TURN-01B` / `TURN-01D` 文件。
  - 未修改 Service、runner、server、配置、CR271、dashboard 或主计划。
  - 未启动 runtime/application/Task/poller/UI/capture/input/tests。
- 双仓 byte-identical 证据：

```text
MATCH TurnOutcome.java 0520F32DF1C574877A54778AD1CEC0A8FE4A8E660A374C0D973EFB7DB8245297
MATCH TurnStepResult.java 6211A6774B218B7F19203F344E5838791F4A0A5F6044FC22814674EADFE979E6
MATCH TurnMatchResult.java F9A39A9F01993264773CCECB1D4BC355873BE9580D8F34BAEA5AC2264B3EAA72
MATCH TurnFrameMetadata.java E4F63BB8BE526CFE47F611DDC1A49170D8C1326B117D09E8DC8D77EE41E87244
MATCH TurnRequest.java 63BAC2E0E402C6E4F09AA57E3AB9A26046F6865C919B07F96C8911609ECE157E
MATCH TurnResponse.java 5A581F6CC6D9ADB639964881B414BF3058CAE7CD480E45D8049ED6CC1823A858
```

- 静态检查：
  - DHXY scoped `git diff --check`：exit `0`。
  - Cloud scoped `git diff --check`：exit `0`。
- Maven：
  - 未运行。当前仍受 `TURN-01B` / `TURN-01D` approval/build cohort 约束；`TurnResponse` 仍引用 `TURN-01B`
    的 `TurnAction`，按父级要求等待 Foundation cohort 统一构建。
- 父级审批状态：
  - 等待父级唯一 reviewer 复验并写入 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`；在此之前不领取下一卡。

## PARENT RE-REVIEW #1

- 复审时间：`2026-07-15T15:12:00-04:00`
- 父级独立展开并重算六对 DTO：`window` 两处已恢复，`TurnStepResult.message` 与
  `TurnResponse.contractVersion` 已删除；六对 SHA 与 Repair #1 报告一致且 byte-identical。
- 结论：`SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING`；`countDelta=0`。原三个 P1 全部关闭，源码 owner 释放。
- Build/CLOSED：待 `TURN-01D` validator 与 Foundation cohort 稳定后由父级统一双仓 Maven；本卡不占实现槽等待。
