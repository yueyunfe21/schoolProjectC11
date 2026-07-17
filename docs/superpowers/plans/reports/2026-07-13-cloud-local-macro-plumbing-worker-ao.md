# Cloud LOCAL_MACRO Plumbing Worker AO

- status: `IMPLEMENTED_SELF_QA`
- owner: `Internal AO`
- claimedAt: `2026-07-13T21:39:51-04:00`
- scope: DHXY 全量云端迁移简化路线中的 Cloud 通用 `LOCAL_MACRO` retained-authority plumbing。

## 唯一写集

Cloud 已授权按实际穷尽性修改以下文件，可少改，不扩大：

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunCommandExecutor.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameClientPort.java`
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java`
6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
7. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`
8. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java`
9. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumptionCoordinator.java`
10. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`
11. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
12. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumedAck.java`
13. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandEnvelope.java`
14. 本报告（仅本 Worker 维护）。

不修改 External A 所有的 `RemoteOperation`、`RemoteRequest`、`RemoteOutcome`、`LocalMacro*` 合同类型；不修改 Service、DHXY Java、schema、tests、host，也不触碰旧 exclusive authority。严禁回滚、覆盖、清理、删除、提交任何并行 Worker 的 dirty/untracked。

## 扩展写集必要性证据

- `RemoteFinalConsumedAck.validateTrackerArtifactControl(...)` 当前仅把 `CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE` 归入 `trackerArtifactControl == null` 的普通 pre-tracker 矩阵；新增 `LOCAL_MACRO` 若不显式加入，会落入 tracker materialize 分支并错误要求 `trackerArtifactControl`，因此必须修改。
- `RemoteCommandEnvelope.payload(RemoteRequest)` 当前仅识别 capture/window fact/input bundle/tracker request；`LOCAL_MACRO` 会直接抛出 `unsupported remote request type`，导致 broker final dispatch 无法生成命令 JSON，因此必须增加闭合 `macroKind + command` 映射。

## 基线证据

- DHXY 当前分支/HEAD：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`；HEAD 即用户批准业务基线。分支无 upstream。
- Cloud 当前分支/HEAD：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`；分支无 upstream，`origin/main` 当前为 `48e3781`。Cloud `remote/**` 为并行未跟踪实现，最新 pushed Cloud 不含本链路，故以当前 shared remote authority 与用户冻结合同为 plumbing 基线。
- 两仓 `git status --short --branch` 均有大量他人 dirty/untracked；本 Worker 不处理唯一写集外任何状态。
- External A 已于 `2026-07-13T21:38:45-04:00` 领取闭合 Cloud 类型写集；开工时 `LocalMacro*`/`BagReturnItemMacro*` 尚未落盘，不创建替代类型。
- 业务行为不在本写集；只接入通用 transport/retained/final-consume plumbing。无已批准业务差异；按 `0114604e` 基线等价迁移。

## 冻结不变量

1. `LOCAL_MACRO` 使用普通 retained action state：稳定 `phaseCode/actionSlot/occurrence/attempt` 取得同一 retained identity；同一合法业务 occurrence 的断线重查复用同一 `requestId/actionId/semanticAddress` 和已绑定 request，不随机重建。
2. gate 只消费 retained identity 提供的 ID/address 并计算 canonical digest；不自行铸 ID，不自动 retry，不重建 request。
3. broker 沿用 scope/window/taskRun/runRevision/stopEpoch 的 enqueue-time 与 final-dispatch fence，并把 `LOCAL_MACRO` 纳入同窗口 physical-input flight/actionId 约束。
4. executor 沿用 `prepareActiveInvocation -> bind/verify -> broker dispatch/poll/terminal -> recordOutcome -> final-consume`；UNKNOWN 保留原 attempt，绝不自动续发或 renewal。
5. strict outcome：仅 `EXECUTED` 携带 typed `BagReturnItemMacroResult`；`NOT_EXECUTED/STOPPED/UNKNOWN` 必须 `result=null`。
6. request digest 覆盖 `macroKind` 与 `BagReturnItemMacroCommand` 全字段；outcome digest 覆盖 typed result 全字段。不得排除 macro 字段。
7. 不依赖 per-Service owner/permit/session/ledger/parent-child/compaction/TTL/自动 retry，也不进入旧 RX3/generic exclusive interaction。
8. 不启动 host、线程、任务、UI 或输入；不新增/运行 tests；不执行 `mvn clean`。源码稳定后最多运行 Cloud `mvn -q compile`；并行合同缺失则记录 `WAITING_DEPENDENCY`。

## 永久本地归属

- 用户于 `2026-07-13` 明确决定：真实 `BagService` 与 `UICleanerService` 永久留在 DHXY 本地。
- 本 Worker 只让 Cloud `CloudGameClient` 暴露通用、闭合、typed 的 `LOCAL_MACRO` facade；不新增 Cloud `BagService`，也不把 facade 描述为 Cloud Bag 业务所有权。
- `BAG_RETURN_ITEM` 只是当前首个闭合 macro kind；Cloud 仅选择 command 并消费 typed result，包裹截图、模板识别、缓存点机械使用及输入交错过程仍由 DHXY 本地实现闭合。

## 实现交付

### 实际修改文件

1. `CloudTaskRunExecutionGate.java`
   - 新增 `newLocalMacroRequest(...)`；从 retained identity 原样取 `requestId/actionId/semanticAddress`，经当前 context gate 后构造 `LOCAL_MACRO` request 与 digest，再 `bindOrVerifyRequest`。
2. `RemoteProtocolDigests.java`
   - 新增 `LocalMacroRequest` 的 digest-bearing 重建；`withComputedOutcomeDigest(...)` 支持 `LocalMacroOutcome.withCommon(...)`。
3. `CloudTaskRunCommandExecutor.java`
   - 新增同步 `executeLocalMacro(...)`；沿用 build-or-redeliver、broker entry 标记、terminal outcome 记录；同 identity 重查必须 kind/command/timeout 与已绑定 bytes 完全一致。
4. `RemoteGameClientPort.java`
   - 新增同步 typed `executeLocalMacro(...)` port。
5. `RemoteGameCommandBroker.java`
   - 新增 `executeLocalMacro(...) -> dispatchAndAwait(...)`；strict pending/outcome correlation；broker 合成终态 typed result 为 null；加入 physical-input actionId ledger 与同窗口 in-flight fence。
6. `RemoteCommandOutcomeEnvelope.java`
   - `LOCAL_MACRO` strict parse：payload 精确为 `macroKind/operation/state/cachePoint`，cache point 精确五字段；仅 EXECUTED 重建 `BagReturnItemMacroResult`，其它三态要求 operation/state/cachePoint 显式 null。
7. `CloudTaskRetainedActionState.java`
   - 新增普通 `retainLocalMacro(...)`、显式 verified-NOT_EXECUTED renewal handle 路径和 `LOCAL_MACRO -> LocalMacroAction` 构造分支。
8. `CloudTaskRunActionLedger.java`
   - verified-NOT_EXECUTED 显式 renewal 时，`LOCAL_MACRO` 与 input bundle 一样使用新 actionId，避免旧 actionId 与旧 request 永久绑定后的 reuse 冲突；UNKNOWN 重查不走 renewal，identity 不变。
9. `CloudTaskServicePort.java`
   - 新增 public typed `executeLocalMacro(...)` 与 opaque `LocalMacroAction`；宏调用和 final-consume 都不调用 `CloudTaskExclusiveInteractionAuthority`。
10. `CloudGameClient.java`
    - 新增通用 stable-address typed macro facade；UNKNOWN 不 final-consume，其它 terminal outcome 走普通 occurrence complete。
11. `RemoteFinalConsumedAck.java`
    - 将 `LOCAL_MACRO` 纳入普通 pre-tracker matrix：`captureId=null`、`observationMode=null`、`trackerArtifactControl=null`。
12. `RemoteCommandEnvelope.java`
    - 将内部 typed `LocalMacroRequest(context, macroKind, bagReturnItem)` 映射为 DHXY strict codec 使用的扁平 command payload：`macroKind/operation/templatePath/maxBackPage/source/cachedPoint`。

允许写集中的 `RemoteFinalConsumptionCoordinator.java` 未修改：其 generic `consumeFinal(...)` 已按 retained identity/outcome 工作，renewal 限制仅排除旧 whole-pass/exclusive control，天然覆盖 `LOCAL_MACRO`，无需新增宏分支或第二套 compaction。

### 调用链

```text
Cloud business caller
  -> CloudGameClient.executeLocalMacro(phaseCode, actionSlot, kind, command, timeout)
  -> CloudTaskRetainedActionState.retainLocalMacro(ActionAddress)
  -> CloudTaskServicePort.executeLocalMacro(LocalMacroAction, ...)
  -> CloudTaskRunCommandExecutor.executeLocalMacro(..., retainedIdentity, ...)
  -> CloudTaskRunExecutionGate.newLocalMacroRequest(...)
  -> CloudTaskRunActionLedger.bindOrVerifyRequest(...)
  -> RemoteGameCommandBroker.executeLocalMacro / dispatchAndAwait
  -> remote-client poll -> RemoteCommandEnvelope(flat closed command)
  -> DHXY local closed macro (External C owner)
  -> RemoteCommandOutcomeEnvelope(strict flat typed result)
  -> broker terminal correlation -> actionLedger.recordOutcome
  -> CloudGameClient terminal final-consume
  -> RemoteFinalConsumptionCoordinator -> RemoteFinalConsumedAck
```

该链没有 per-Service owner/permit/session/ledger/parent-child/TTL/自动 retry；也不进入 RX3、`bindGenericStepIfActive(...)` 或任何 generic exclusive owner/session/renewal。

### 稳定 Identity

- 稳定 caller address 为 `ActionAddress(phaseCode, actionSlot)`；ledger semantic address 为同 slot 的 `occurrence/attempt`。
- 首次合法 occurrence 由既有 `CloudTaskRunActionLedger` retained owner 一次性建立 identity；gate/factory 不调用 UUID/random，不自行生成或替换 ID。
- request 首次 build 后绑定 exact typed bytes/digest。transport UNKNOWN 不 final-consume；再次调用同 address 返回同 `LocalMacroAction`，executor 进入 `UseActiveBoundRequest`，复用同一 `requestId/actionId/semanticAddress/requestDigest`，且不会 requeue/rebuild。
- 只有已验证 `NOT_EXECUTED`、已 final-consume 为 renewal、且 receipt compaction 完成后，现有显式 renewal API 才能进入下一 attempt；本 facade 不自动调用它。
- 下一业务 occurrence 也必须等前一 occurrence final-consumed/compacted 后才由现有 retained state 前进。

### 三道 Revision Fence

1. **Gate/build-time**：`CloudTaskRunExecutionGate.newRequestContext(...) -> validate(...)` 校验 exact scope、window tuple、taskRunId、stopEpoch、runRevision、ACTIVE 与 confirmed current revision。
2. **Broker enqueue-time**：`RemoteGameCommandBroker.dispatchAndAwait(...) -> authorizationRejection(...)` 在登记/排队前再次用 request context 的 exact scope/window/taskRun/stop/revision 调 coordinator authorization。
3. **Broker final-dispatch-time**：`selectCommandLocked(...) -> authorizeAndMarkDispatch(...)` 在 long-poll 真正返回命令前，以同一 exact tuple 原子授权并标记 dispatch。

DHXY handler 的既有 pre-side-effect fence 属 External C/host 写集，本 Worker 没有绕过或替代它。

### Canonical Digest 字段

- Request canonical tree：`context` 全字段（计算时仅移除 `requestDigest`）+ `macroKind` + nested `bagReturnItem`：
  `operation/templatePath/maxBackPage/source/cachedPoint`；非 null cache point 再包含
  `templatePath/clickX/clickY/learnedAtMs/source`。
- Outcome canonical tree：`common` 全字段（计算时仅移除 `outcomeDigest`）+ `macroKind` + EXECUTED 时 nested
  `bagReturnItem.operation/state/cachePoint`；非 null cache point 同样包含上述五字段。
- `computeRequestDigest(...)` / `computeOutcomeDigest(...)` 没有删除任何 macro kind、command 或 result 字段；capture image-byte 特例与本 operation 无关。
- Transport 为 DHXY codec 所需扁平 payload，但 Cloud strict parser 会重建上述 typed nested tree后再验证 `outcomeDigest`；DHXY digest owner必须做同一 canonical tree 重建，不得直接把扁平字段当成另一套 digest schema。

#### Request：transport 与 digest tree

Cloud 发给 DHXY 的 `RemoteCommandEnvelope.payload` 是现有 B strict DTO 的扁平 wire shape（`cachedPoint` 为 null 时按 NON_NULL 省略）：

```json
{
  "macroKind": "BAG_RETURN_ITEM",
  "operation": "USE_CACHED_MAIN_BAG_RETURN_ITEM",
  "templatePath": "...",
  "maxBackPage": 0,
  "source": "...",
  "cachedPoint": {
    "templatePath": "...",
    "clickX": 123,
    "clickY": 456,
    "learnedAtMs": 1,
    "source": "..."
  }
}
```

真正计算 `requestDigest` 的 Cloud typed tree 必须逐字等价于下列 shape；`context.requestDigest` 在 canonical hash 前移除，正常宏的 `observationMode` 因 NON_NULL 不出现：

```json
{
  "context": {
    "contractVersion": 1,
    "operation": "LOCAL_MACRO",
    "requestId": "...",
    "actionId": "...",
    "taskRunId": "...",
    "runRevision": 0,
    "semanticAddress": {
      "phaseCode": "...",
      "actionSlot": "...",
      "occurrence": 0,
      "attempt": 0
    },
    "window": {
      "windowId": "...",
      "nativeHandle": "...",
      "processId": 1,
      "playerIdentityEpoch": 0
    },
    "stop": {
      "taskRunId": "...",
      "stopEpoch": 0
    },
    "timeoutMs": 1
  },
  "macroKind": "BAG_RETURN_ITEM",
  "bagReturnItem": {
    "operation": "USE_CACHED_MAIN_BAG_RETURN_ITEM",
    "templatePath": "...",
    "maxBackPage": 0,
    "source": "...",
    "cachedPoint": {
      "templatePath": "...",
      "clickX": 123,
      "clickY": 456,
      "learnedAtMs": 1,
      "source": "..."
    }
  }
}
```

External B 的 `computeRequestDigest(...)` 必须先 strict parse flat payload，再把 command 字段放进 nested `bagReturnItem`；不能把 `operation/templatePath/...` merge 到 root。null `cachedPoint` 必须在 nested command 中省略，与 Cloud NON_NULL mapper 一致。

#### Outcome：transport 与 digest tree

DHXY 回 Cloud 的 EXECUTED flat outcome payload 必须精确为：

```json
{
  "macroKind": "BAG_RETURN_ITEM",
  "operation": "PRESCAN_MAIN_BAG_TASK_PAGE",
  "state": "FOUND",
  "cachePoint": {
    "templatePath": "...",
    "clickX": 123,
    "clickY": 456,
    "learnedAtMs": 1,
    "source": "..."
  }
}
```

`NOT_EXECUTED/STOPPED/UNKNOWN` flat payload 仍精确保留四个 key，但 typed result 三字段显式 null：

```json
{
  "macroKind": "BAG_RETURN_ITEM",
  "operation": null,
  "state": null,
  "cachePoint": null
}
```

真正计算 EXECUTED `outcomeDigest` 的 typed tree 是下列 shape；`common.outcomeDigest` 在 hash 前移除，`common.message` 为 null 时省略：

```json
{
  "common": {
    "contractVersion": 1,
    "operation": "LOCAL_MACRO",
    "requestId": "...",
    "actionId": "...",
    "taskRunId": "...",
    "requestDigest": "...",
    "executionState": "EXECUTED",
    "code": "...",
    "acceptedAtEpochMs": 0,
    "finishedAtEpochMs": 0
  },
  "macroKind": "BAG_RETURN_ITEM",
  "bagReturnItem": {
    "operation": "PRESCAN_MAIN_BAG_TASK_PAGE",
    "state": "FOUND",
    "cachePoint": {
      "templatePath": "...",
      "clickX": 123,
      "clickY": 456,
      "learnedAtMs": 1,
      "source": "..."
    }
  }
}
```

非 EXECUTED typed digest tree 只有 `common + macroKind`，整个 `bagReturnItem` 因 NON_NULL 省略。External B 的 `computeOutcomeDigest(...)` 必须按 envelope executionState 对 flat payload执行同一重建；不得把 flat `operation/state/cachePoint` 留在 root。Cloud `RemoteCommandOutcomeEnvelope` 已按此规则重建 typed `LocalMacroOutcome` 后调用 `verifyOutcomeDigest(...)`。

### Strict Outcome Matrix

- `EXECUTED`：payload 四个顶层字段必须齐全；`operation/state` 非 null；cache point 若非 null，必须正好五字段；typed record 再执行 FOUND/NOT_FOUND/USED/NOT_USED 组合校验。
- `NOT_EXECUTED/STOPPED/UNKNOWN`：`macroKind` 保留；`operation/state/cachePoint` 必须作为显式 null 出现，Cloud typed `bagReturnItem=null`。
- `OBSERVED`：拒绝；`LOCAL_MACRO` 不允许 paused observation。

## 外部等待依赖

- External A Cloud closed types 已落盘，本 Worker 已按实际字段 `bagReturnItem` 接线；AO Cloud compile 不再等待 A。
- External B 仍需完成其已分配的 DHXY `RemoteProtocolDigests` nested canonical tree 重建、`RemoteFinalConsumedAck` null matrix 和 `HttpRemoteCommandTransport` observation 禁止项。
- External C 仍需完成 DHXY handler 的 `LOCAL_MACRO` dispatch、flat terminal payload（非 EXECUTED 显式 null result fields）及本地 Bag macro 调用。
- 以上是整条 fresh-runtime 链的外部依赖，不是 AO Cloud 源码编译依赖；本 Worker不扩大写集代做。
- integrationStatus：`WAITING_DEPENDENCY`，直到 External B 的 request/outcome nested digest parity 与 External C handler 均落盘；AO 自身 source/compile 状态不受阻。

## Self-QA

- final fresh `mvn -q compile`（Cloud，未 clean、未运行 tests）在最后一处 Java 注释/格式调整后 exit `0`；核心 class 于 `2026-07-13 21:56:28..29 -04:00` 重新生成。
- 静态核对：`LOCAL_MACRO` 仅新增普通 retained handle/facade；无 generic exclusive 调用、无自动 retry、无 TTL、无 host/thread/task startup。
- 静态核对：broker physical input fence、actionId reuse fence、request/outcome type correlation与 final-consume null matrix均覆盖 `LOCAL_MACRO`。
- 本结论仅为 Worker self-QA，不构成父级 `Approved`。

无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #1 - APPROVED - 2026-07-13T22:02:00-04:00

父级独立复核 AO 实际修改的 12 个 Cloud remote 文件，结论 `P0=0 / P1=0 / P2=0`：

- gate 只从现有 retained identity 读取 requestId/actionId/semanticAddress，构造并绑定 exact typed request；没有随机铸 ID、
  自动 retry 或 request rebuild；同 address 的 UNKNOWN 重查只接受完全相同 macro kind/command/timeout 与已绑定 bytes。
- broker 的通用 enqueue 授权与 final-dispatch 持锁授权继续校验 exact scope/window/taskRun/stopEpoch/runRevision，
  `LOCAL_MACRO` 同时进入 actionId reuse 与同窗口 physical-input flight fence；非 UNKNOWN terminal 才释放 flight。
- Cloud typed digest tree 精确为 request `{context,macroKind,bagReturnItem}`、outcome
  `{common,macroKind,bagReturnItem?}`；flat transport 仅是 wire 表示，Cloud outcome parser 会先 strict 重建 typed tree再验 digest。
- 仅 EXECUTED 可携 `BagReturnItemMacroResult`；NOT_EXECUTED/STOPPED/UNKNOWN result=null。UNKNOWN 不 final-consume，
  其它 terminal outcome 走现有 ordinary final-consume；没有进入旧 RX3/generic exclusive，也没有新增 Service owner、
  permit、session、ledger、parent-child、TTL 或自动重试。
- `RemoteFinalConsumedAck` 将 LOCAL_MACRO 归入无 captureId/observationMode/tracker attachment 的普通机械操作；
  `RemoteCommandEnvelope` 只把 closed command 扁平化给 DHXY codec。真实 `BagService`/`UICleanerService` 均未迁 Cloud。

AO 的 Cloud `mvn -q compile` 已 exit 0。本切片 `SOURCE APPROVED`；整条 Bag 宏仍为
`WAITING_DEPENDENCY`，必须等 External B 完成 DHXY nested canonical digest 重建、External C 完成 local handler 后，
由父级复核双端 exact JSON parity 并运行 fresh Cloud package + DHXY compile，才可 FINAL APPROVED。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
