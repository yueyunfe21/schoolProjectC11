# TURN-18 - exact window metadata identity cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15 20:33 EDT

- 状态：`READY`；类型：`INTEGRATION`；startDependsOn：`TURN-02R`、`TURN-13C` source review passed。
- 唯一 production write set：
  `src/main/java/com/bot/dhxy/service/ClientIdentityService.java`。
- 唯一 test write set：
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ClientIdentityTurnMetadataContractTest.java`；本报告可写。
- 精确替换 `resolveCurrentWindowTitle()`：只通过当前 exact bound `TurnGameClient.latestWindowMetadata()` 读取
  已接收 metadata，不创建 actionId/command、缓存、第二 metadata type 或全局 window fallback。
- metadata 保持 `windowTitle/nativeHandle/processId/windowRect(left,top,width,height)/pauseRequested/stopRequested`；
  identity 只消费 nonblank `windowTitle`。首次请求前为空、标题空白或解析失败时不修改角色状态。
- 本卡是零 action metadata observation，验收特例为 `STATE+BASE`，明确不适用默认 `BC4`。named test 断言
  `latestWindowMetadata=1`、`execute=0`、exact device/window、missing/blank/malformed fail-closed、无 retry；不得为
  凑 COMPLETED/FAILED/STOPPED/uncertain 人造 action 或第二协议。
- 禁止修改 PlayerStateService/TurnGameClient/协议/DHXY/Server/routes，禁止 runtime/Task/input/capture 与 Git mutation。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-18 parent frozen brief -->

## CLAIMED - 2026-07-15

- Owner: CR271 TURN-18 implementation Worker（非 reviewer）；父级为唯一 manager/final reviewer。
- Scope: 仅领取固定 brief 中的 production/test/report 写集；共享 dirty workspace 中其余文件保持只读。

## SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING - 2026-07-15 20:47 EDT

### Baseline / scope evidence

- Cloud 工作分支读取结果：`navigation-migration`；共享工作区在领取前已 dirty，目标 `src/main/java/com/bot/**`
  位于未跟踪迁移树。本 Worker 未回滚、覆盖或清理任何既有改动，未执行 Git mutation。
- `origin/main` 不含目标 Cloud 文件；按权威卡使用固定业务基线
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。该提交中 `ClientIdentityService.java` blob 为
  `06db63306f163c4d51c1d7208e155f96b6fe8877`，与只读 `migration-baseline/696a12b0` 副本一致。
- 保留基线 `scanAndSyncIdentity()` 的 null/blank/parse-success/parse-failure 与三字段写入语义；唯一迁移点为
  `resolveCurrentWindowTitle()` 的 observation 来源。无已批准业务差异；按 `696a12b0` 基线等价迁移。

### Delivered files / SHA-256

- Production：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ClientIdentityService.java`
  - SHA-256：`9B5B8866ABFD22CBC3332430CAED1D1A7CE41FC21587D35F6A19A10D276F0DC7`
  - 只从当前 `TaskExecutionContext` 取得 exact-bound `TurnGameClient`，调用一次
    `latestWindowMetadata()`；empty 或 metadata device/window 与当前 binding 不一致时返回 null，不读取初始标题、
    retained fact、tracker 或全局窗口。返回标题后仍由基线 nonblank/parse 门决定是否修改角色。
- Test source：
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/ClientIdentityTurnMetadataContractTest.java`
  - SHA-256：`8A9AD239A965A6048E198D5433955C2BB77A3E9B19EE34DF56B76E4221D93C58`
  - `STATE+BASE`：有效 latest 标题覆盖刻意不同的 initial 标题；missing、blank、malformed 与错 device/window
    全部保持原角色；每个 observation 均断言 exact requested device/window、metadata read=1、execute=0。
  - 明确不适用 BC4；测试源码未构造 `COMPLETED/FAILED/STOPPED/UNCERTAIN` action outcome，未添加 retry。

### Static self-check

- Production 文本扫描：`latestWindowMetadata()` call=`1`；旧 `readWindowFact`、`WindowFact`、
  `BINDING_FACT_TIMEOUT_MS`、`getGameClient()` symbol 合计=`0`；`execute/capture/localService` action call=`0`。
- Test 文本扫描：JUnit methods=`3`；统一 observation 断言包含 metadata read=`1`、execute=`0`；BC4/outcome
  symbols=`0`。
- 本 Worker 的 write set 仅为上述 production、test source 与本固定报告；未修改
  `PlayerStateService`、`TurnGameClient`、协议、DHXY production、Server/routes 或其它文件。

### Gates intentionally not run

- 未运行 `mvn -q -Dtest=ClientIdentityTurnMetadataContractTest test`。
- 未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input。
- 原因：父级 brief 明确禁止本 Worker 运行上述门；需要父级完成 source/assertion review，并在获准 cohort 中执行
  named test 与适用 Cloud compile gate。本交付不声明 `CARD APPROVED` 或可运行。

**SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING**

<!-- TRUE_EOF: TURN-18 worker source delivery -->

## PARENT SOURCE + TEST-SOURCE REVIEW - 2026-07-15 20:52 EDT

- Parent independently read the full production source, named test source, current turn context/client contracts,
  and the `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` baseline. Worker self-checks were not used as approval.
- Verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`.
- Production evidence:
  - `ClientIdentityService.java:27-31` retains only `TaskExecutionContextHolder`; no tracker, native lookup,
    second metadata cache/type, capture, input, OCR, retry, or lifecycle dependency remains.
  - `ClientIdentityService.java:39-61` preserves the baseline null/blank/parse-success/parse-failure behavior and
    the exact server/name/id mutation order.
  - `ClientIdentityService.java:71-92` performs one `TurnGameClient.latestWindowMetadata()` observation, rejects
    missing or mismatched device/window metadata, and consumes only `windowTitle`; no action id or command is made.
- Test-source evidence:
  - `ClientIdentityTurnMetadataContractTest.java:48-81` covers latest-title success, missing, blank, malformed,
    and mismatched metadata while deliberately making the initial title different.
  - `ClientIdentityTurnMetadataContractTest.java:100-105,166-178` proves one exact device/window metadata read and
    zero `execute(...)` calls for every path.
- Parent recomputed SHA-256 and matched the delivered values:
  production `9B5B8866ABFD22CBC3332430CAED1D1A7CE41FC21587D35F6A19A10D276F0DC7`, test
  `8A9AD239A965A6048E198D5433955C2BB77A3E9B19EE34DF56B76E4221D93C58`.
- The Cloud test tree remains hidden by the existing shared `.gitignore:15` retention debt already tracked by
  TURN-T01; this card did not modify `.gitignore` and has no card-local repair for that shared gate.
- Maven/JUnit/compile were not run while the other Java implementation writers remain active. This verdict is
  not `CARD APPROVED/CLOSED`; named test and applicable Cloud compile remain cohort-pending.

**SOURCE+TEST SOURCE REVIEW PASSED / MAVEN+CLOUD COMPILE PENDING**

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-18 parent source and test-source review -->
