# RemoteGameClientPort 最小类型化通信契约

工件编号：A-2

状态：lift-and-shift 开工契约 v1

适用范围：把现有云端 Service 以同步调用方式迁移到云端，同时保留 DHXY 本地窗口截图、机械窗口事实读取和物理输入执行能力。

## 1. 边界与非目标

- 云端现有 Service 保留当前业务判断、调用顺序、重试顺序、fallback、sleep、点击顺序和完成条件。
- 本地只执行四种机械操作：`CAPTURE`、`WINDOW_FACT`、`EXECUTE_INPUT_BUNDLE`、`LOCAL_MACRO`，并返回结构化 outcome。前三者为纯 primitive；`LOCAL_MACRO` 是业务中立的固定本地复合宏（单一输入队列内 capture/template/input 交错，见 §7A），同样不做业务决策。
- 本地不得根据截图、窗口事实或输入结果选择 NPC、对话、导航、任务阶段、重试或后继动作。
- 本契约不要求自定义 frame、消息总线、lease、outbox、HKDF、MATCH recipe、上传 grant 或全局状态机。同步 transport 可先使用 HTTPS；Java 侧以 `RemoteGameClientPort` 接口隔离 transport。
- Java `Supplier<Boolean>` exclusive callback 不跨边界。只有能表达为本契约 typed request 的调用才迁移；不能表达的 callback 暂时留在本地，直到单独定义一个业务中立的 typed operation。

## 2. Java 端口形状

Service-facing 接口只有三个同步方法。它不接收 caller-built raw request；每次调用必须同时携带
coordinator-minted execution context 与 retained action-identity owner 当前 handle：

```java
interface RemoteGameClientPort {
    WindowFactOutcome readWindowFact(
            CloudTaskRunExecutionContext context,
            WindowFactKind factKind,
            RetainedActionIdentity identity,
            long timeoutMs);

    CaptureOutcome capture(
            CloudTaskRunExecutionContext context,
            CaptureRegion region,
            ImageFormat imageFormat,
            CapturePurpose capturePurpose,
            RetainedActionIdentity identity,
            long timeoutMs);

    InputBundleOutcome executeInputBundle(
            CloudTaskRunExecutionContext context,
            String description,
            CoordinateSpace coordinateSpace,
            List<InputActionDto> actions,
            RetainedActionIdentity identity,
            long timeoutMs);
}
```

端口实现独占 `context + retained identity + payload -> immutable raw request -> internal broker ->
correlated outcome -> retained ledger`。`RequestContext` 与三种 raw request 仍是下述 wire schema，但只允许
package-internal broker submission；host/Service 不得持有 raw send、client poll 或 outcome completion capability。
`requestId/actionId/captureId` 只能由 retained owner 为一个业务动作铸造，调用方不能借重建 raw request 换新 ID。

调用返回时必须已有 terminal outcome。transport timeout、断线或客户端进程退出不能伪装成 `NOT_EXECUTED`；只要无法证明没有开始物理输入，`EXECUTE_INPUT_BUNDLE` 返回或映射为 `UNKNOWN`。

## 3. 共同请求字段

三个 request 都包含以下 closed object；没有列出的字段禁止：

```text
RequestContext {
  contractVersion: 1,
  operation: CAPTURE | WINDOW_FACT | EXECUTE_INPUT_BUNDLE | LOCAL_MACRO,
  requestId: non-blank string,
  actionId: non-blank string,
  taskRunId: non-blank string,
  runRevision: non-negative long,
  observationMode?: PAUSED_READ_ONLY,   // 可选键；键缺失=普通请求（唯一 canonical 缺省表示）
  semanticAddress: RemoteSemanticAddress,
  window: WindowBindingRef,
  stop: StopRef,
  timeoutMs: positive long,
  requestDigest: SHA-256 hex
}

WindowBindingRef {
  windowId: non-blank string,
  nativeHandle: normalized unsigned decimal string,
  processId: positive long,
  playerIdentityEpoch: non-negative long
}

StopRef {
  taskRunId: non-blank string,
  stopEpoch: non-negative long
}

RemoteSemanticAddress {
  phaseCode: non-blank string,
  actionSlot: non-blank string,
  occurrence: non-negative long,
  attempt: non-negative int
}
```

规则：

1. `requestId` 标识一次逻辑 RPC。transport 重试必须复用相同 requestId 和完全相同 request bytes。
2. `actionId` 标识云端当前业务动作。CAPTURE/WINDOW_FACT 可在同一 actionId 下使用不同 requestId；每个 EXECUTE_INPUT_BUNDLE actionId 只能对应一个 requestId 和一个 requestDigest。云端 requestId/actionId/captureId 一律由 retained action-identity owner（`CloudTaskRunActionLedger`）一次铸造并随 outcome 留存；请求构造只接受该 owner 的不可伪造 handle，重投必须复用同一 handle 的原字节身份。`UNKNOWN`/`STOPPED`/`EXECUTED`/`OBSERVED` 之后禁止换新身份；唯一换新路径是已记录的可信 `NOT_EXECUTED`（EXECUTE_INPUT_BUNDLE 换新 actionId，CAPTURE/WINDOW_FACT 保留 actionId 换 requestId/captureId）。
3. `stop.taskRunId` 必须等于顶层 taskRunId，并映射到该窗口当前 `TaskStopToken`。未知 run、stopEpoch 落后或本地已 stop 时不得开始新操作。
4. `timeoutMs` 从本地收到并通过 schema 校验后用单调钟计时。云端 RPC timeout 必须大于该值；超时不是业务重试指令。
5. `requestDigest = hex(SHA-256(JCS(request 去掉 requestDigest 字段)))`。相同 requestId 不同 digest 拒绝 `IDEMPOTENCY_CONFLICT`。`runRevision` 与 required `semanticAddress` 都属于 context，一律参与 requestDigest；本地缺失、负值或错误类型按 strict schema 直接拒绝。
6. `runRevision` 是构造请求时云端 coordinator 的当前 run revision。三道门都必须精确相等：云端入队门与最终 dispatch 持锁门要求 `runRevision == 当前 runRevision == confirmedExecutionRevision`；本地在任何副作用前及 input worker 开始前要求 `command.runRevision == 本地 registration.runRevision`。任一不等以 `NOT_EXECUTED/TASK_RUN_MISMATCH` 拒绝。revision R 构造的请求在 pause→resume→重新 CONFIRM（revision R+2）后必须稳定拒绝，不得因 window/stopEpoch 未变而复活。
7. `observationMode`（PAUSED 只读观察）：可选 context 键。键缺失 = 普通请求，canonical 字节与授权零变化；出现时必须为 canonical 枚举文本 `PAUSED_READ_ONLY`，且仅允许 `WINDOW_FACT`/`CAPTURE`（`EXECUTE_INPUT_BUNDLE` 携带该键：云端构造期拒绝，本地 strict schema `SCHEMA_MISMATCH`）。显式 `"observationMode": null` 与未知枚举值在本地反序列化即失败，按既有 `DESERIALIZATION` typed failure fail-closed（wire 只有一种 canonical 表示参与 digest）。digest：该键属 context，NON_NULL canonical——缺失不写键，出现按枚举名参与 `requestDigest`，双仓字节一致。授权：携带该标记的请求仅当 binding/registration `status==PAUSED` 且 `runRevision == 当前 PAUSED revision` 且 exact scope/window 四元组/stopEpoch 全等时放行（云端入队门、dispatch 持锁门与本地副作用前门三道对称，绝不查询 execution confirmation）；ACTIVE/终态/身份漂移/revision 不等 → `NOT_EXECUTED/TASK_RUN_MISMATCH`。observation 身份由 retained 权威独立铸造（与 ACTIVE 身份互斥类型/互斥 key），结构性禁止 renewal；同字节重投沿用既有幂等台账；resume/stop/complete 后旧观察请求被 revision/status 门永久拒绝。不携带该键的请求语义与字节完全不变。

本文的 `JCS` 固定指 RFC 8785 canonical JSON 的 UTF-8 bytes；重复字段、浮点特殊值和非 canonical 编码拒绝。protocol v1 的 canonical numeric subset 为整数加**有限 binary64**：非 finite 拒绝；finite 值的 token 采用 **RFC 8785 / ECMAScript number serialization**，由 RFC 8785 官方 Java 参考实现 `io.github.erdtman:java-json-canonicalization` 的 `org.erdtman.jcs.NumberToJSON.serializeNumber(double)` 生成（与 ECMAScript `JSON.stringify` 一致，例如 `Double.MIN_VALUE -> 5e-324`、`1.0 -> 1`、`1.25 -> 1.25`）。两仓 canonicalizer 的 floating token 生成器逐字同构、整树其余（integral/string/boolean/object/array）分支不变；当前只有 OBSERVED capture 的 `systemScaleRatio` 使用该 floating 分支，其余 request/outcome 无合法 floating 字段，故其 digest byte-for-byte 不变。

## 4. 窗口绑定门

本地在操作开始前读取 `WindowRuntimeContext` 当前值，并逐项核对：

| request 字段 | 本地权威值 |
|---|---|
| `windowId` | `WindowRuntimeContext.getWindowId()` |
| `nativeHandle` | `WindowRuntimeContext.getNativeBinding().getNativeHandle()` |
| `processId` | `WindowRuntimeContext.getNativeBinding().getProcessId()` |
| `playerIdentityEpoch` | `WindowRuntimeContext.getPlayerIdentityEpoch()` |

任一不一致、native binding 缺失、窗口已关闭或 processId 不再对应 HWND，均在副作用前返回 `NOT_EXECUTED/WRONG_WINDOW`。禁止按标题全局搜索另一个窗口补位。

CAPTURE 和 WINDOW_FACT 在读取完成后再次核对绑定；中途变化时丢弃数据并返回 `NOT_EXECUTED/WINDOW_BINDING_CHANGED`。EXECUTE_INPUT_BUNDLE 在入队前和 worker 开始时核对；执行中绑定变化时停止余下步骤并返回 `UNKNOWN/WINDOW_BINDING_CHANGED`。

## 5. CAPTURE

### 5.1 Request

```text
CaptureRequest {
  context: RequestContext(operation=CAPTURE),
  captureId: non-blank string,
  region: CaptureRegion,
  imageFormat: PNG,
  capturePurpose: DIAGNOSTIC | CLOUD_SERVICE_INPUT
}

CaptureRegion {
  coordinateSpace: SCREEN_ABSOLUTE_PX | WINDOW_CLIENT_PX,
  x: int,
  y: int,
  width: positive int,
  height: positive int
}
```

`SCREEN_ABSOLUTE_PX` 保留现有 `GameClientTracker` ROI 语义；`WINDOW_CLIENT_PX` 由本地使用已核验 binding 唯一换算。CAPTURE 必须使用 request.window 绑定的 HWND capture；只有现有配置允许时才走同一窗口的 Robot fallback，不能切换到标题匹配窗口。

### 5.2 Outcome

```text
CaptureOutcome {
  common: CommonOutcome,
  captureId: string,
  imageBytes: byte[] | null,
  imageSha256: SHA-256 hex | null,
  width: int | null,
  height: int | null,
  captureProvider: HWND_PRINTWINDOW | HWND_BITBLT | ROBOT | null,
  systemScaleRatio: finite positive JSON number | null,
  observedWindow: ObservedWindowBinding | null
}
```

capture payload 是**闭合对象**：allowed/required key 恰为 `captureId,imageBytes,imageSha256,width,height,captureProvider,systemScaleRatio,observedWindow` 八个；缺 key、额外 key 或错误类型一律 `INVALID_REQUEST`。`captureId` 恒为 non-blank；只有 `executionState=OBSERVED` 时其余七个 observation 字段（含 `systemScaleRatio`）必填 non-null，其它状态其余七个 key **必须显式 null**（缺失不可伪装、显式 `null` 不可进入 OBSERVED）。

`systemScaleRatio` 是本地执行时现场读取的系统缩放事实，绝不进入 request，因此所有普通 request 与非 capture outcome 的 `requestDigest`/canonical bytes 逐字不变。成功域严格为有限 IEEE-754 binary64 正数 `Double.isFinite(v) && v > 0.0`；`NaN`、±infinity、`±0.0`、缺失、JSON null、字符串/布尔 coercion 均不构成成功 scale，也不允许“缺值=1.0”兜底。唯一生产 owner 是 DHXY typed CAPTURE outcome producer，用与 `CoordinateHelper#initScaleRatio()` 相同的 `defaultScreenDevice -> defaultConfiguration -> defaultTransform.getScaleX()` 现场读取，不读缓存、不从图像/坐标反推。它在同一 handler invocation 内以 `scaleBefore -> 唯一现有截图 -> scaleAfter` 夹住同一帧，`Double.doubleToLongBits(scaleBefore)==Double.doubleToLongBits(scaleAfter)` 时发布该相等值。

**scale failure 映射**：`scaleBefore` 不可读/非法（截图尚未开始）→ `NOT_EXECUTED/FACT_UNAVAILABLE`，captureId 保留、其余字段 null；`scaleAfter` 不可读/非法或与 `scaleBefore` bitwise 不同（截图已发生）→ 丢弃/flush 本地图像并返回 `UNKNOWN/FACT_UNAVAILABLE`，云端不得降级为 template/OCR miss，也不得换 ID 重拍。原有 `无图=NOT_EXECUTED/CAPTURE_FAILED`、`截图后 timeout=UNKNOWN/TIMEOUT`、post-binding typed mapping 均不变。

**同帧/同 binding 证明链**：一个可消费 OBSERVED 结果同时具备 bracket 的两次相等 scale 采样与唯一帧、capture 前后 exact window 四元组验证、`observedWindow` 由 post-fence binding 生成、云端 broker 对 OBSERVED capture 新增 `observedWindow == pending.context.window` 四字段精确比较，且 `captureId+imageSha256+width+height+provider+systemScaleRatio+observedWindow` 一起进入同一 `outcomeDigest`（`imageBytes` 仍由 `imageSha256` 绑定、唯一排除于 digest）。云端 Service 对图片的 OCR、模板匹配和业务解释顺序保持现状。

**coordinated cutover**：`contractVersion` 保持 1；因新 DHXY producer + 旧 Cloud（digest/字段不符）与新 Cloud + 旧 DHXY（OBSERVED 缺 required scale）都会互相拒绝，CAPTURE 不支持 mixed-version rolling，必须 8 文件同波、双仓 canonicalizer 逐字同构落地；切换/回滚均先 quiesce CAPTURE，禁止靠“缺值=1.0”、忽略字段或跳过 digest 做兼容。

## 6. WINDOW_FACT

WINDOW_FACT 只读取无需截图的机械窗口事实。业务新事实不能塞入自由 map；新增 factKind 时增加 sealed request/result variant。

### 6.1 Request

```text
WindowFactRequest {
  context: RequestContext(operation=WINDOW_FACT),
  factKind: BINDING | GEOMETRY | FOCUS_STATE | STOP_STATE | COMMON_BOX | TEAM_RETURN_BUTTON
          | TEAM_RETURN_LEADER_SIGNAL | TASK_TRACKER_PANEL_RECT
}
```

### 6.2 Result union

```text
WindowFactOutcome {
  common: CommonOutcome,
  factKind: BINDING | GEOMETRY | FOCUS_STATE | STOP_STATE | COMMON_BOX | TEAM_RETURN_BUTTON
          | TEAM_RETURN_LEADER_SIGNAL | TASK_TRACKER_PANEL_RECT,
  fact: BindingFact | GeometryFact | FocusFact | StopFact | CommonBoxFact | TeamReturnButtonFact
      | TeamReturnLeaderSignalFact | TaskTrackerPanelRectFact | null
}

BindingFact {
  windowId, nativeHandle, processId, playerIdentityEpoch,
  title, className
}

GeometryFact { x, y, width, height, coordinateSpace: SCREEN_ABSOLUTE_PX }

FocusFact { state: FOREGROUND | BACKGROUND | UNKNOWN }

StopFact { taskRunId, stopEpoch, stopRequested: boolean }

CommonBoxFact {
  state: MATCHED | NOT_MATCHED | CAPTURE_UNAVAILABLE | TEMPLATE_UNAVAILABLE | MECHANICS_FAILED,
  clickX, clickY, matchScore, matchedAtEpochMs,
  coordinateSpace: SCREEN_ABSOLUTE_PX
}

TeamReturnButtonFact {
  state: PRESENT | ABSENT | CAPTURE_UNAVAILABLE | TEMPLATE_UNAVAILABLE | MECHANICS_FAILED,
  clickX, clickY, matchScore,
  coordinateSpace: SCREEN_ABSOLUTE_PX
}

TeamReturnLeaderSignalFact {
  state: PRESENT | ABSENT | CAPTURE_UNAVAILABLE | TEMPLATE_UNAVAILABLE | MECHANICS_FAILED,
  signalX, signalY, matchScore,
  coordinateSpace: SCREEN_ABSOLUTE_PX
}

TaskTrackerPanelRectFact {
  state: PRESENT | ABSENT | CAPTURE_UNAVAILABLE | TEMPLATE_UNAVAILABLE | REPOSITION_REQUIRED | MECHANICS_FAILED,
  anchorClientX, anchorClientY,
  panelClientLeft, panelClientTop, panelClientRight, panelClientBottom,
  matchScore,
  coordinateSpace: WINDOW_CLIENT_PX
}
```

只有 `executionState=OBSERVED` 时 fact 必填且 variant 必须与 factKind 一致。title 只作为返回事实，不参与窗口选择或业务判断。

### 6.3 COMMON_BOX closed fact

`COMMON_BOX` 只读取「本地在一个 fixed ROI 内对 fixed template 做一次匹配」的机械观察结果——本地**只读、零点击**（不发 CLICK/任何输入），是否点击/何时点击是 Cloud 业务决策，不在本 fact 内。

`state` 是 closed 五值机械结果，仅在 `executionState=OBSERVED` 时随 `CommonBoxFact` 出现：
- `MATCHED`：模板在 ROI 命中。全部 observation 字段必填 non-null——`clickX`、`clickY`（screen-absolute 整数像素）、`matchScore`、`matchedAtEpochMs`——且 `coordinateSpace` 恒 `SCREEN_ABSOLUTE_PX`。
- negative（`NOT_MATCHED` / `CAPTURE_UNAVAILABLE` / `TEMPLATE_UNAVAILABLE` / `MECHANICS_FAILED`）：除 `state` 与 `coordinateSpace`（仍 `SCREEN_ABSOLUTE_PX`）外，`clickX`/`clickY`/`matchScore`/`matchedAtEpochMs` **必须显式 null**（缺失不可伪装、显式 null 不可进入 MATCHED）。

边界与语义：
- 这五个 `state` 是**机械观察事实**，不是 transport 执行态。transport 层的 `UNKNOWN`（不确定是否执行）**不等于** `NOT_MATCHED`：`UNKNOWN` 保留同一 action/request 身份、不消费、不重投，绝不折成 `NOT_MATCHED` 业务结果。
- Cloud 保留 committed `0114604e` 的常见盒子业务语义（含回城检测后 30 秒 pending 窗口等既有时序判断）；本 fact 只上报「这一刻 ROI 内是否命中及命中点」，pending/是否使用仍由 Cloud 业务决定。
- 无 owner/permit/ledger 语义、无额外 TTL/retry：本 fact 复用既有 `WINDOW_FACT` retained action-identity 与 final-consumption 台账，不新增台账或计时。

### 6.4 TEAM_RETURN_BUTTON closed fact

`TEAM_RETURN_BUTTON` 只读取「本地在 fixed ROI 内对 committed `0114604e` 的返队按钮 template 做一次匹配」的机械观察结果——本地**只读、零点击**（不发 CLICK/任何输入），是否点击、是否先补摄妖香、是否二次观察都是 Cloud 业务决策，不在本 fact 内；本地不得据此自行点击或推进业务。

`state` 是 closed 五值机械结果，仅在 `executionState=OBSERVED` 时随 `TeamReturnButtonFact` 出现：
- `PRESENT`：返队按钮模板在 ROI 命中。`clickX`、`clickY` 为**非负** screen-absolute 整数像素，`matchScore` 为 **non-null 且有限**的匹配分数（protocol v1 canonical binary64 数值）——阈值判断**已由本地 mechanics 按当前 `returnTeamMatchRate` 配置完成**（committed `0114604e` 默认 `0.85`，可配置），协议不重复该阈值、不设固定下限；`coordinateSpace` 恒 `SCREEN_ABSOLUTE_PX`。
- negative（`ABSENT` / `CAPTURE_UNAVAILABLE` / `TEMPLATE_UNAVAILABLE` / `MECHANICS_FAILED`）：`clickX`、`clickY`、`matchScore` 三个 observation 字段为 null，按既有 Jackson `NON_NULL` **省略该 key**（不写显式 `null`）；`state` 与 `coordinateSpace`（仍 `SCREEN_ABSOLUTE_PX`）保留。

边界与语义：
- 这五个 `state` 是**机械观察事实**，不是 transport 执行态。transport 层的 `UNKNOWN` **不等于** `ABSENT`：`UNKNOWN` 保留同一 action/request 身份、不消费、不重投，绝不折成 `ABSENT` 业务结果。
- 本 fact 只上报「这一刻 ROI 内返队按钮是否在场及其命中点」；committed `0114604e` 的返队业务时序（先补摄妖香、二次观察、发 click bundle 等）仍由 Cloud 现有 Service 编排保留，语义不变。
- 无 owner / session / ledger 语义、无额外 TTL / retry：本 fact 复用既有 `WINDOW_FACT` retained action-identity 与 final-consumption 台账，不新增台账或计时。

### 6.5 TEAM_RETURN_LEADER_SIGNAL closed fact

`TEAM_RETURN_LEADER_SIGNAL` 只读取「本地在 fixed ROI 内对 committed `0114604e` 的队长归队信号 template 做一次匹配」的机械观察结果——本地**只读、零点击**（不发 CLICK/任何输入），是否点击、如何编排归队都是 Cloud 业务决策，不在本 fact 内；本地不得据此自行点击或推进业务。

`state` 是 closed 五值机械结果，仅在 `executionState=OBSERVED` 时随 `TeamReturnLeaderSignalFact` 出现：
- `PRESENT`：队长信号模板在 ROI 命中。`signalX`、`signalY` 为**非负** screen-absolute 整数像素，`matchScore` 为 **non-null 且有限**的匹配分数（protocol v1 canonical binary64 数值）——阈值判断**已由本地 mechanics 按当前 `returnTeamMatchRate` 配置完成**（committed `0114604e` 默认 `0.85`，可配置），协议不重复该阈值、不设固定下限；`coordinateSpace` 恒 `SCREEN_ABSOLUTE_PX`。
- negative（`ABSENT` / `CAPTURE_UNAVAILABLE` / `TEMPLATE_UNAVAILABLE` / `MECHANICS_FAILED`）：`signalX`、`signalY`、`matchScore` 三个 observation 字段为 null，按既有 Jackson `NON_NULL` **省略该 key**（不写显式 `null`）；`state` 与 `coordinateSpace`（仍 `SCREEN_ABSOLUTE_PX`）保留。

边界与语义：
- 这五个 `state` 是**机械观察事实**，不是 transport 执行态。transport 层的 `UNKNOWN` **不等于** `ABSENT`：`UNKNOWN` 保留同一 action/request 身份、不消费、不重投，绝不折成 `ABSENT` 业务结果。
- 本 fact 只上报「这一刻 ROI 内队长归队信号是否在场及其命中点」；committed `0114604e` 的归队业务时序仍由 Cloud 现有 Service 编排保留，语义不变。
- 无 owner / session / ledger 语义、无额外 TTL / retry：本 fact 复用既有 `WINDOW_FACT` retained action-identity 与 final-consumption 台账，不新增台账或计时。

### 6.6 TASK_TRACKER_PANEL_RECT closed fact

`TASK_TRACKER_PANEL_RECT` 只读取「本地在 fixed ROI 内对 committed `0114604e` 的任务追踪面板 template 做一次匹配并解算锚点/面板矩形」的机械观察结果——本地**只读、零点击**（不发 CLICK/任何输入），是否点击、是否重定位、如何消费矩形都是 Cloud 业务决策，不在本 fact 内；本地不得据此自行点击或推进业务。

`state` 是 closed 六值机械结果，仅在 `executionState=OBSERVED` 时随 `TaskTrackerPanelRectFact` 出现：
- `PRESENT`：面板模板命中并解出完整矩形。全部 observation 字段必填 non-null——`anchorClientX`、`anchorClientY`、`panelClientLeft`、`panelClientTop`、`panelClientRight`、`panelClientBottom`（均为 window-client 整数像素）与 `matchScore`（non-null 且有限，protocol v1 canonical binary64）；六个坐标**非负**，矩形须**正尺寸**（`panelClientRight > panelClientLeft` 且 `panelClientBottom > panelClientTop`）；阈值判断**已由本地 mechanics 完成**，协议不设固定下限；`coordinateSpace` 恒 `WINDOW_CLIENT_PX`。
- negative（`ABSENT` / `CAPTURE_UNAVAILABLE` / `TEMPLATE_UNAVAILABLE` / `REPOSITION_REQUIRED` / `MECHANICS_FAILED`）：七个 observation 字段（`anchorClientX`/`anchorClientY`/`panelClientLeft`/`panelClientTop`/`panelClientRight`/`panelClientBottom`/`matchScore`）**一个都不得出现**，按既有 Jackson `NON_NULL` 省略该 key；`state` 与 `coordinateSpace`（仍 `WINDOW_CLIENT_PX`）保留。

边界与语义：
- 这六个 `state` 是**机械观察事实**，不是 transport 执行态。transport 层的 `UNKNOWN` **不等于** `ABSENT`：`UNKNOWN` 保留同一 action/request 身份、不消费、不重投，绝不折成 `ABSENT` 业务结果。`REPOSITION_REQUIRED` 是机械观察结论（面板在场但位置需重定位），非 transport 态，业务处置仍由 Cloud 决定。
- 与其它窗口事实不同，本 fact 坐标空间为 `WINDOW_CLIENT_PX`（window-client 相对像素），非 `SCREEN_ABSOLUTE_PX`；消费方须按 window-client 语义解释锚点与矩形。
- 无 owner / session / ledger 语义、无额外 TTL / retry：本 fact 复用既有 `WINDOW_FACT` retained action-identity 与 final-consumption 台账，不新增台账或计时。

## 7. EXECUTE_INPUT_BUNDLE

### 7.1 Request

```text
InputBundleRequest {
  context: RequestContext(operation=EXECUTE_INPUT_BUNDLE),
  description: non-blank diagnostic string,
  coordinateSpace: SCREEN_ABSOLUTE_PX | WINDOW_CLIENT_PX,
  actions: InputActionDto[1..256]
}
```

`SCREEN_ABSOLUTE_PX` 保持当前 `InputAction` 的屏幕绝对像素语义。`WINDOW_CLIENT_PX` 只允许 pointer action
（click/right/double/move/drag start+end）携带相对当前绑定 client 左上角的像素；DHXY 必须在 `callWith` 内完成
current registration/binding/runRevision 二次 fence 后，用 exact `binding.x/y + relative` 转为 screen absolute，并对
转换后的每个点做 current client geometry inside-window 验证。overflow 或 outside 必须在任何 input step 前返回
`NOT_EXECUTED`，不得 clamp；无坐标 action 原样进入同一个 bundle。`coordinateSpace` 已进入 canonical request digest，
现有 `SCREEN_ABSOLUTE_PX` payload、字段顺序和 digest 不变。

### 7.2 InputActionDto

type 必须来自当前 `InputActionType`：

`CLICK_LEFT`、`CLICK_RIGHT`、`DOUBLE_RIGHT_CLICK`、`MOVE_MOUSE`、`DRAG_AND_DROP`、`HOLD_CTRL`、`RELEASE_CTRL`、`PRESS_CTRL_U`、`TYPE_TEXT_UNICODE`、`PASTE_TEXT`、`PRESS_ENTER`、`PRESS_ALT_1`、`PRESS_ALT_2`、`PRESS_ALT_4`、`PRESS_ALT_6`、`PRESS_ALT_8`、`PRESS_ALT_T`、`PRESS_ALT_O`、`PRESS_ALT_E`、`PRESS_ALT_Q`、`PRESS_ALT_A`、`PRESS_ALT_C`、`PRESS_ALT_U`、`SCROLL_DOWN`、`SCROLL_UP`、`SLEEP`。

| type | 必填字段 | 其它动作字段 |
|---|---|---|
| CLICK_LEFT / CLICK_RIGHT | `x,y,delayMs` | 禁止 |
| DOUBLE_RIGHT_CLICK | `x,y,delayMs,intervalMs` | 禁止 |
| MOVE_MOUSE | `x,y` | 禁止 |
| DRAG_AND_DROP | `x,y,endX,endY` | 禁止 |
| TYPE_TEXT_UNICODE / PASTE_TEXT | `text` | 禁止 |
| SCROLL_DOWN / SCROLL_UP | `clicks` | 禁止 |
| SLEEP | `delayMs` | 禁止 |
| 其余无参数 key/control type | 无 | 全部禁止 |

数值沿用当前 Java `int` 范围；delay/interval/clicks 不得为负，text 不得为 null。禁止传 callback、脚本、条件、循环、候选列表或业务 fallback。

### 7.3 执行规则

1. 整个 actions 列表必须一次提交到现有 `InputSequences.submitAndWait(...)` / `InputActionQueue`，不得按 step 发多个 queue request。
2. move + sleep + click、输入文本 + Enter + click/scroll 等现有原子顺序必须保持在同一 bundle，防止其它窗口插入输入。
3. worker 使用 request.window 捕获的 `WindowRuntimeContext` 和 `WindowNativeBinding`；不得读取调用线程后来切换的窗口。
4. 入队前、worker 开始前、每个可中断 sleep/step 间检查 stop 与 timeout。stop 后不开始新 step；不得为了“完成 bundle”忽略 stop。
5. transport 断开或调用方取消时取消尚未开始的 request；已经开始的物理动作不自动重放。
6. 本地只报告完成到哪一步，不根据失败选择重试、替代坐标或后继动作。

### 7.4 Outcome

```text
InputBundleOutcome {
  common: CommonOutcome,
  actionCount: int,
  startedStepIndex: int,
  lastCompletedStepIndex: int,
  inputQueueRequestId: string | null,
  observedWindow: ObservedWindowBinding | null
}
```

未开始时两个 index 都为 `-1`。开始后 `startedStepIndex>=0`；成功时 `lastCompletedStepIndex=actionCount-1`。任一步是否已产生物理副作用无法确定时必须返回 UNKNOWN，云端不得重发该 actionId。

## 7A. LOCAL_MACRO（BAG_RETURN_ITEM / BAG_USE_INCENSE）

`LOCAL_MACRO` 是一条**业务中立的固定本地复合宏**：本地在既有**单一输入队列**（`InputSequences.submitAndWait(...)` / `InputActionQueue`）内串起 capture / template match / input 的交错，作为一个不可分割的机械单元完成，防止其它窗口在中途插入焦点或输入。它与 §5-7 的三个纯 primitive 的区别仅在于「一个宏内含多种机械步骤」；它**不**根据截图/模板/输入结果选择 NPC、对话、导航、任务阶段、重试或后继动作——这些仍由云端现有 Service 编排。`BAG_RETURN_ITEM` 覆盖回城前背包回程物品预扫/使用；`BAG_USE_INCENSE` 只执行一次固定摄妖香查找/使用。两者均逐字复用 committed `0114604e` 的 `BagService` 机械行为；每个 macroKind 使用对应 sealed command/result variant，不塞自由 map。

### 7A.1 Request

```text
LocalMacroRequest {
  context: RequestContext(operation=LOCAL_MACRO),
  macroKind: BAG_RETURN_ITEM | BAG_USE_INCENSE,
  bagReturnItem: BagReturnItemCommand | omitted,
  bagUseIncense: BagUseIncenseCommand | omitted
}

BagReturnItemCommand {
  operation: PRESCAN_MAIN_BAG_TASK_PAGE | PRESCAN_MAIN_BAG_FROM_BACK | USE_CACHED_MAIN_BAG_RETURN_ITEM,
  templatePath: non-blank string,
  maxBackPage: int,
  source: non-blank string,
  cachedPoint: BagReturnCachePoint | null
}

BagReturnCachePoint {
  templatePath: non-blank string,
  clickX: int,
  clickY: int,
  learnedAtMs: long,
  source: non-blank string
}

BagUseIncenseCommand {
  // closed empty object; no fields
}
```

`LocalMacroRequest` 是按 `macroKind` 区分的**闭合对象**，`macroKind` 在**顶层**。`BAG_RETURN_ITEM` 时 key 恰为 `context,macroKind,bagReturnItem`；`BAG_USE_INCENSE` 时 key 恰为 `context,macroKind,bagUseIncense`。不属于当前 kind 的 variant 必须省略。缺 key、额外 key 或错误类型一律 `INVALID_REQUEST`。规则：
- `BAG_RETURN_ITEM` 的内层 `bagReturnItem` 恰为 `operation,templatePath,maxBackPage,source,cachedPoint` 五个 key（不再重复 `macroKind`）；`operation` 为 closed 三值。
- `maxBackPage`：跟随真实 primitive `int` 合同——`PRESCAN_MAIN_BAG_FROM_BACK` 取 `0..4`；`PRESCAN_MAIN_BAG_TASK_PAGE` 与 `USE_CACHED_MAIN_BAG_RETURN_ITEM` 取 `0`（恒为整数，非 null）。
- `cachedPoint`：**仅** `USE_CACHED_MAIN_BAG_RETURN_ITEM` non-null；两个 `PRESCAN_*` 为 null。Jackson `NON_NULL` 下 null 时**省略该 key**（不写虚构的显式 `null` key）。`BagReturnCachePoint` 恰有 `templatePath,clickX,clickY,learnedAtMs,source` 五字段（与 committed `ReturnItemCachePoint` 同构）；`clickX/clickY` 为 screen-absolute 整数像素。
- `BAG_USE_INCENSE` 的内层 `bagUseIncense` 必须是**零字段空对象**。请求不允许 `templatePath`、`source`、坐标、页码或其它自由参数；DHXY `BagService` 固定使用 `bag/sheyaoxiang_item.png`。

### 7A.2 Outcome

```text
LocalMacroOutcome {
  common: CommonOutcome,
  macroKind: BAG_RETURN_ITEM | BAG_USE_INCENSE,
  bagReturnItem: BagReturnItemResult | omitted,
  bagUseIncense: BagUseIncenseResult | omitted
}

BagReturnItemResult {
  operation: PRESCAN_MAIN_BAG_TASK_PAGE | PRESCAN_MAIN_BAG_FROM_BACK | USE_CACHED_MAIN_BAG_RETURN_ITEM,
  state: FOUND | NOT_FOUND | USED | NOT_USED,
  cachePoint: BagReturnCachePoint | null
}

BagUseIncenseResult {
  state: USED | NOT_FOUND
}
```

`LocalMacroOutcome` 是按 `macroKind` 区分的**闭合对象**，`macroKind` 在顶层。规则：
- `common.executionState=EXECUTED` 时必须且只能携带与 kind 对应的一种 typed result：`BAG_RETURN_ITEM -> bagReturnItem`，`BAG_USE_INCENSE -> bagUseIncense`。`NOT_EXECUTED` / `UNKNOWN` / `STOPPED` 时两个 result variant 都必须为 null，并在 Jackson `NON_NULL` 下省略。
- `state`（`bagReturnItem` 内）是宏的**机械观察结果**，closed 四值，只与 operation 家族一致：`PRESCAN_*` 仅 `FOUND` / `NOT_FOUND`；`USE_CACHED_MAIN_BAG_RETURN_ITEM` 仅 `USED` / `NOT_USED`。`state` **不重复** `common.executionState`/mechanicalStatus：前者是宏业务观察（找到/用了），后者是 transport 执行态。
- `cachePoint`（`bagReturnItem` 内）：`PRESCAN_* + FOUND` 时为学到的点（`BagReturnCachePoint` non-null，`NON_NULL` 输出）；`PRESCAN_* + NOT_FOUND`、`USED`、`NOT_USED` 时为 null 并省略。
- `bagUseIncense` 恰有一个 `state` 字段，closed 两值：找到并完成既有使用点击为 `USED`；既有 `BagService` 返回 false 为 `NOT_FOUND`。它不允许 `NOT_USED`、路径、坐标或其它 payload。
- DHXY 发出的 flat terminal payload 对所有 local macro 恰为 `macroKind,operation,state,cachePoint` 四个 key：`BAG_USE_INCENSE` 的 `operation/cachePoint` 恒为显式 null；非 `EXECUTED` 的 `operation/state/cachePoint` 全为显式 null。Cloud 必须先按此矩阵 strict parse，再重建上面的 typed outcome 参与 digest。

### 7A.3 边界

- 本地只完成这一次固定预扫/使用宏并上报 `FOUND/NOT_FOUND/USED/NOT_USED`；业务编排仍由云端现有 `ReturnItemPrescanService` 保留，且严格等于 committed `0114604e`：策略选择（tracker-green/background/in-combat 三候选一次随机）、`4000ms` combat-entry maintenance 门、`8000..18000ms` combat 随机 due、`inProgress/done/combatFallback/cachePoint` 转移、`useCached` 失败即 `invalidate`、`completeRound` exact 移除。**不新增任何 TTL/pending/retry**（该 Service 无「30 秒 pending」——30 秒 pending 属 CommonBox 业务，不在本宏）。
- `BAG_USE_INCENSE` 只复用现有 main-bag 全页查找和 `ItemAction.USE` 机械路径。Cloud 仍独占 quiet-period、状态图标/剩余时间解释、是否需要用香、任务 phase 和后继动作；本宏不接 caller，不产生第二套判断。
- 复用 §3 context（scope/window/taskRun/runRevision）三道 fence、§4 窗口绑定门、retained action-identity 与 §10 final-consumed/terminal outcome 台账；`UNKNOWN`/`STOPPED` 沿用既有身份与幂等规则，绝不折成 `NOT_FOUND`/`NOT_USED`。
- **无** Bag 专属 owner / permit / session / ledger / 额外 TTL / retry。

## 8. CommonOutcome 与错误码

```text
CommonOutcome {
  contractVersion: 1,
  operation: CAPTURE | WINDOW_FACT | EXECUTE_INPUT_BUNDLE | LOCAL_MACRO,
  requestId: string,
  actionId: string,
  taskRunId: string,
  requestDigest: SHA-256 hex,
  outcomeDigest: SHA-256 hex,
  executionState: NOT_EXECUTED | EXECUTED | OBSERVED | UNKNOWN | STOPPED,
  code: OutcomeCode,
  message: short diagnostic string,
  acceptedAtEpochMs: long,
  finishedAtEpochMs: long
}
```

`outcomeDigest = hex(SHA-256(JCS(outcome 去掉 outcomeDigest 字段和 imageBytes)))`；CAPTURE 的 imageSha256 已绑定图片内容。

| executionState | 唯一语义 | 云端自动重发 |
|---|---|---|
| NOT_EXECUTED | 已证明未截图、未读取并提交 fact、未开始任何物理 step | 禁止自动重发；现有 Service 按原业务流程决定下一调用 |
| OBSERVED | CAPTURE/WINDOW_FACT 已产生完整结构化结果 | 不重发，消费结果 |
| EXECUTED | EXECUTE_INPUT_BUNDLE 全部 step 完成 | 永不重发该 actionId |
| STOPPED | 本地 stop 已生效；可能已有已完成 step，以 index 为准 | 不重发 |
| UNKNOWN | 无法证明物理输入是否发生或完成 | 永不重发该 actionId，交给云端现有恢复逻辑 |

OutcomeCode 固定为：`OK`、`FINAL_CONSUMED`、`INVALID_REQUEST`、`AUTH_FAILED`、`IDEMPOTENCY_CONFLICT`、`ACTION_ID_REUSE`、`WRONG_WINDOW`、`WINDOW_BINDING_CHANGED`、`TASK_RUN_MISMATCH`、`TASK_RUN_PAUSED`、`STOP_REQUESTED`、`BROKER_CAPACITY_EXCEEDED`、`TIMEOUT`、`CAPTURE_FAILED`、`FACT_UNAVAILABLE`、`INPUT_QUEUE_REJECTED`、`INPUT_FAILED`、`TRANSPORT_LOST`、`CLIENT_RESTARTED`、`INTERNAL_ERROR`。

## 9. 幂等台账

本地 port adapter 维护 `(operation,requestId) -> {requestDigest,terminal outcome}` 台账：

1. 首次 request 原子登记为 IN_PROGRESS，再调用本地 provider。
2. 同 requestId + 同 digest：IN_PROGRESS 时等待同一 future；terminal 时返回缓存的同一 outcome，不重复截图、fact read 或输入。
3. 同 requestId + 不同 digest：返回 `NOT_EXECUTED/IDEMPOTENCY_CONFLICT`。
4. EXECUTE_INPUT_BUNDLE 另建 `actionId -> {requestId,requestDigest}` 唯一映射；不同 requestId/digest 复用 actionId 返回 `NOT_EXECUTED/ACTION_ID_REUSE`。
5. 输入台账至少保留到对应 taskRun 明确终态且云端已收到 terminal outcome。进程重启后若无法恢复某 actionId 是否开始，云端查询结果只能得到 `UNKNOWN/CLIENT_RESTARTED`，不能重新执行。

该台账只防重复机械操作，不推进任务阶段，也不生成 successor。

## 10. Full R0 semantic frontier 与 final-consumed 收口

### 10.1 semantic slot 与 no-gap frontier

每个机械请求都必须携带 server-owned `semanticAddress`。三侧共享的 stable semantic slot key 为：

```text
tenantId/userId/deviceId/clientSessionId/taskRunId
+ windowId/nativeHandle/processId/playerIdentityEpoch/stopEpoch
+ operation/phaseCode/actionSlot
```

`runRevision`、`observationMode`、`occurrence`、`attempt` 是 exact detail 字段，不从 stable key 消失。一个 slot 同时最多一个 current detail；无 open occurrence 时只允许 `occurrence=completedOccurrence+1,attempt=0`，可信 ACTIVE `NOT_EXECUTED` renewal 只允许同 occurrence 的 `attempt=compactedThroughAttempt+1`。PAUSED observation 的 attempt 恒为 0，且只允许 occurrence 完成。gap、回退、并发第二 detail、跨 ACTIVE/PAUSED mode 复用 slot 均在副作用前拒绝。

Cloud 收 outcome 时先按 authenticated `(scope,operation,requestId)` 命中 server-owned current detail 或 latest compacted witness，再逐项比较 semanticAddress、requestDigest、typed outcome 与 outcomeDigest；caller 提供的 semanticAddress 不得自行选择 frontier 或证明旧请求有效。DHXY handler 同样必须在任何截图/fact/input 副作用前完成 exact registration generation 与 frontier claim，input worker 开始前再复验当前 registration。

### 10.2 poll closed union 与 cloud incarnation

```text
RemoteCommandPollResponse {
  status: IDLE | COMMAND | FINAL_CONSUMED,
  cloudIncarnationId: non-blank string,
  command: RemoteCommandEnvelope | null,
  finalConsumedAck: RemoteFinalConsumedAck | null
}
```

这是 closed union：`IDLE` 两个 payload 都为空，`COMMAND` 只允许 command，`FINAL_CONSUMED` 只允许 finalConsumedAck。`cloudIncarnationId` 三态都必填；DHXY 首次见到后固定到当前本地 ledger，发现变化即进入 coordinated-restart fail-closed，禁止把单边重启后的 UNKNOWN 或旧 frontier 当作可重试结果。

command 与 control 共用一个 authenticated route。control lane 固定 64 个 slot，command/control 同时 ready 时 1:1 交替；coalesced wake 是 level signal，selector 的成功、reject、cleanup、generation mismatch、spurious、IDLE、interrupt 与异常退出都必须在 route lock 内重算两 lane并重新 arm。route 只有 terminal-ready、无 ready/SENDING/control 且无 in-flight poll 时才可条件移除，旧 RouteState 不得在新 route 已有 work 时继续睡到 timeout。

### 10.3 Cloud -> DHXY final-consumed acknowledgement

```text
RemoteFinalConsumedAck {
  contractVersion: 1,
  tenantId, userId, deviceId, clientSessionId,
  taskRunId,
  runRevision,
  observationMode?: PAUSED_READ_ONLY,
  window: WindowBindingRef,
  stopEpoch,
  operation,
  semanticAddress: RemoteSemanticAddress,
  requestId,
  actionId,
  captureId?: non-blank string,       // CAPTURE 必填，其余 operation 禁止
  requestDigest,
  outcomeDigest,
  executionState,                     // 禁止 UNKNOWN
  outcomeCode,
  disposition: OCCURRENCE_COMPLETE | ATTEMPT_RETIRED_FOR_RENEWAL,
  ackDigest
}

ackDigest = hex(SHA-256(JCS(ack 去掉 ackDigest 字段)))
```

ack 只能由 exact retained non-UNKNOWN outcome 与 server-owned request detail 构造，并在业务 mutation 正常返回后生成。`ATTEMPT_RETIRED_FOR_RENEWAL` 只允许 ACTIVE + exact `NOT_EXECUTED`；PAUSED 只允许 `OCCURRENCE_COMPLETE`。callback 抛异常或 post-callback generation 不确定时进入不可重放的 consumption-unknown，不发布 ack、不自动重做业务 mutation。

### 10.4 DHXY -> Cloud receipt 与 compact

```text
RemoteFinalConsumedReceipt {
  contractVersion: 1,
  tenantId, userId, deviceId, clientSessionId,
  taskRunId,
  semanticAddress: RemoteSemanticAddress,
  ackDigest,
  applyStatus: APPLIED | DUPLICATE_APPLIED | REJECTED,
  appliedCompletedOccurrence: long >= -1,
  appliedOpenOccurrence?: long,
  appliedThroughAttempt: int,
  code,
  message,
  receiptDigest
}

receiptDigest = hex(SHA-256(JCS(receipt 去掉 receiptDigest 字段)))

RemoteFinalConsumedReceiptAck {
  status: ACCEPTED_COMPACTED | DUPLICATE_COMPACTED | REJECTED,
  ackDigest,
  receiptDigest,
  code,
  message
}
```

DHXY 在一个 ledger mutation 中验证 exact ack/detail/frontier，应用 frontier、删除 local request/input detail并写入 bounded FIFO receipt outbox。receipt 状态只有 `READY/SENDING/REJECTED_RETAINED`：每个 poll turn只发送队首一次，uncertain failure 回 READY 并交给外层下一轮，permanent 4xx/serialization/digest reject 保留为 REJECTED_RETAINED；无内部自动 retry 或跳过队首。Cloud 只有 ACCEPTED_COMPACTED/DUPLICATE_COMPACTED 才同时 compact broker/action detail并推进镜像 frontier；任何 scope、address、ack/receipt digest 或 frontier 不一致都 REJECTED 且不释放状态。

三侧详细状态都受 bounded cap 约束；terminal cleanup 只在 exact run terminal/absent 且 current detail、unknown、consuming、notice、control、receipt outbox 全空时 O(1) 删除该 run bucket并退 semantic-slot quota，不扫描历史，不引入 TTL/LRU/takeover。`contractVersion` 仍为 1；semanticAddress、poll union 与 final-consumed wire 必须双仓原子切换，不支持 mixed-version 忽略新字段。

## 12. Dormant task-tracker strict protocol cohort

本节是 `W-TTPS-T1A-IMP1` 的 append-only 协议增量。新增
`TASK_TRACKER_READ` 与 `TASK_TRACKER_MATERIALIZE_ACTION`；它们在 handler/ledger/broker/assembly
接线前保持 dormant。外部伪造的新 operation 必须在未支持 handler 路径 fail-closed，不能执行截图、drag、runtime
publish、wake 或其它机械副作用。普通 `CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE` 的 request/outcome
字段、wire bytes、JCS 与 digest 路径不变。

### 12.1 TASK_TRACKER_READ request

Typed request property order：`context,captureId,readProfile,source,allowPanelReposition`。扁平 command
payload property order：

```text
captureId
readProfile
source
allowPanelReposition
```

`readProfile` 是 closed enum：`WUHUAN_TITLE_GATE | WUHUAN_PATHING | WUBEI_DETAIL |
XIULUO_DETAIL`。四个 profile 均固定 `maxArtifactCount=1`、`maxRetainedFrameCount=2`、
`maxRetainedEncodedBytes=1,048,576`、每 frame `maxEncodedBytes=524,288`、
`maxLocalWorkingImageCount=4`、`maxCloudTransientCopyCount=4`、
`maxCloudTransientBytes=7,820,640`，并由 command flag 决定是否执行最多一次既有 panel reposition。

| profile | PRIMARY_PANEL | DETAIL_BLOCK | maxLocalWorkingArgbBytes |
|---|---:|---:|---:|
| WUHUAN_TITLE_GATE | 182x338 | 175x65 | 3,683,356 |
| WUHUAN_PATHING | 182x338 | 175x65 | 3,683,356 |
| WUBEI_DETAIL | 182x338 | 175x65 | 3,683,356 |
| XIULUO_DETAIL | 182x338 | 175x40 | 3,665,856 |

`source` 是 closed union：

```text
{ kind: LIVE_BOUND_WINDOW }

{ kind: EXISTING_CAPTURE_ARTIFACT,
  sourceArtifact: { captureId, imageSha256, artifactId } }
```

`artifactId` 形状固定为 `tpa1:` + 64 位 SHA-256 hex。live source 禁止 `sourceArtifact`；existing
source 必须带 exact prior `captureId/imageSha256/artifactId` 且强制
`allowPanelReposition=false`。两个 tracker operation 都禁止 `observationMode`。request 不含 local path、template
path、HWND、screen-absolute Cloud click、request/action/capture mint 指令或 raw capability。

### 12.2 TASK_TRACKER_READ outcome

Typed outcome property order：
`common,captureId,readProfile,source,artifact,frames,mechanicalFact,observedWindow`；扁平 payload
固定且必须恰有以下七个 key：

```text
captureId
readProfile
source
artifact: { artifactId, artifactDigest }
frames: [{ ordinal, role, imageBytes, imageSha256, region }]
mechanicalFact: {
  templateId, taskKey?, templateScore,
  titleDisposition: HIT | MISSED | NOT_APPLICABLE,
  panelFingerprint, captureOccurrence
}
observedWindow
```

`captureId` 所有 executionState 均必填。仅 `OBSERVED` 允许且要求其余六项非 null；其它 state 要求这六个
key 均存在且为 explicit null。frames 有序且为 1..2：ordinal 必须等于数组下标，index 0 只能
`PRIMARY_PANEL`，index 1 只能 `DETAIL_BLOCK`；region 只能 `WINDOW_CLIENT_PX` 并受 profile 尺寸上限。
每帧 bytes 非空、SHA exact、单帧/aggregate bytes 不超过 profile bound；`captureOccurrence` 必须等于 outer
`semanticAddress.occurrence`。`MISSED` 禁止携带 taskKey，且机制异常不得编码为 MISSED。

tracker read outcome digest 为：

```text
SHA-256(JCS(typed outcome 去掉 common.outcomeDigest
            并去掉每个 frames[i].imageBytes))
```

每帧 `imageSha256`、ROI、role/order、artifact digest 与 mechanical fact 仍进入 digest。普通 CAPTURE 继续只
排除其原 root `imageBytes`，不走 tracker frame canonicalizer。

### 12.3 TASK_TRACKER_MATERIALIZE_ACTION request/outcome

Typed materialize request property order：
`context,artifact,observationDigest,dependencyLease,selectedLink,preparedOperation,targetKeyword,validationPolicy`；
扁平 payload 去掉 context 后保持同序。closed nested types：

```text
artifact: { artifactId, artifactDigest }
dependencyLease: {
  sourceReadActionId,
  sourceReadSemanticAddress,
  leaseDigest
}
selectedLink: {
  stableIndex >= 0,
  rect: CaptureRegion(coordinateSpace=WINDOW_CLIENT_PX),
  click: { x, y }                  // 必须严格位于 rect 内
}
preparedOperation: TASK_TRACKER_PATHING
validationPolicy: SAME_FRAME_GREEN_FINGERPRINT
```

request 中的 outer actionId/semanticAddress 是 materialize identity；codec 必须以 artifact、dependency 的 read
identity 与 outer materialize identity 重算并 exact 比较 leaseDigest。payload 不含 encoded frame、local path、
HWND、screen-absolute click 或 mint/renew 字段。

materialize outcome payload 必须恰有以下六个 key：

```text
artifact
observationDigest
preparedActionId
publishDisposition
validationFingerprintDigest
observedWindow
```

| executionState | required | explicit null | publishDisposition |
|---|---|---|---|
| EXECUTED | 全六项 | 无 | PUBLISHED / ALREADY_PUBLISHED |
| NOT_EXECUTED | artifact, observationDigest, publishDisposition | preparedActionId, validationFingerprintDigest, observedWindow | DEPENDENCY_NOT_READY / STALE / SAFETY_REJECTED |
| UNKNOWN / STOPPED | 无 | 全六项 | null |
| OBSERVED | 禁止 | - | - |

materialize 不携带 image bytes，outcome digest 沿用 common canonicalizer。它只描述 retained artifact 的 typed
publication/stale/not-ready 结果；本 cohort 不接 handler，不赋予它 runtime publish 权。

### 12.4 tracker final-consumed attachment

`RemoteFinalConsumedAck` exact property order 扩展为 22 fields：

```text
contractVersion, tenantId, userId, deviceId, clientSessionId,
taskRunId, runRevision, observationMode, window, stopEpoch,
operation, semanticAddress, requestId, actionId, captureId,
requestDigest, outcomeDigest, executionState, outcomeCode,
disposition, trackerArtifactControl, ackDigest
```

`trackerArtifactControl` 紧邻 `ackDigest`，typed attachment exact order：

```text
attachmentVersion = 1
directive
artifactId
artifactDigest
sourceReadActionId
sourceReadSemanticAddress
materializeActionId?          // RELEASE_AFTER_READ 必须 absent
materializeSemanticAddress?   // RELEASE_AFTER_READ 必须 absent
leaseDigest?                  // RELEASE_AFTER_READ 必须 absent
```

attachment、ack 的 nullable wire member均为“absent 或 value”，explicit JSON null 与 unknown field 必须拒绝。
directive matrix：

| outer operation/state/disposition | trackerArtifactControl |
|---|---|
| CAPTURE / WINDOW_FACT / EXECUTE_INPUT_BUNDLE | 必须 absent |
| TASK_TRACKER_READ + OBSERVED + OCCURRENCE_COMPLETE | required；RELEASE_AFTER_READ 或 RETAIN_FOR_MATERIALIZE；source read identity 必须等于 outer action/address |
| TASK_TRACKER_READ + non-OBSERVED（含 NOT_EXECUTED renewal） | 必须 absent |
| TASK_TRACKER_MATERIALIZE_ACTION + NOT_EXECUTED + ATTEMPT_RETIRED_FOR_RENEWAL | required；KEEP_FOR_MATERIALIZE_RENEWAL；materialize identity 必须等于 outer action/address |
| TASK_TRACKER_MATERIALIZE_ACTION + OCCURRENCE_COMPLETE | required；RELEASE_AFTER_MATERIALIZE 或 RELEASE_TRUSTED_CANCEL；后者另要求 NOT_EXECUTED/Cloud never-dispatched proof |

`TASK_TRACKER_READ` 的 captureId required；materialize 的 captureId forbidden。lease digest 不含 directive：

```text
leaseDigest = SHA-256(JCS({
  artifactId,
  artifactDigest,
  sourceReadActionId,
  sourceReadSemanticAddress,
  materializeActionId,
  materializeSemanticAddress
}))
```

`ackDigest = SHA-256(JCS(完整 typed ack 去掉 ackDigest))`，所以 non-null attachment 自动绑定到 ack。普通三类
operation 的 Java attachment 为 null，`NON_NULL` 使 key 完全不出现在 wire/JCS 中；旧 key set、canonical bytes 与
ackDigest 因而逐字不变。receipt/receiptAck 与其 digest 不新增 tracker 字段，receipt 的 ackDigest 已绑定 attachment。

### 12.5 deployment gate

两个 tracker operation 只能 Cloud/DHXY 双仓同波上线，不允许 mixed-version、unknown-field ignore、null fallback、
旧端降级为 CAPTURE 或跳过 digest。protocol DTO/codec 落地不等于 capability 激活；必须等后续
handler/ledger/broker/coordinator/assembly cohort 完整接线并通过 exact retain receipt causal gate 后，producer 才可生成这
两类 operation。

## 11. 同步调用顺序与编码门

迁移一个现有 Service 方法时，云端代码按原方法顺序同步调用 port：

```text
原本本地 capture/read fact
  -> port.capture / port.readWindowFact
  -> 云端原有判断不变
  -> 原本 InputSequences 调用
  -> port.executeInputBundle
  -> 云端原有结果分支不变
```

开工门只有以下四项：

1. 原调用点能映射到三种 typed request 之一；不能映射则暂不迁移该点。
2. request 带完整 window binding、taskRun/stop、timeout、requestId 和 actionId。
3. 本地 adapter 完成错窗门、幂等台账和单 input queue 接入。
4. 云端对 `EXECUTED/UNKNOWN/STOPPED` 不做自动物理重发；业务 Service 的判断和顺序保持当前 pushed baseline。

## 13. Dormant generic retained exclusive cohort（RX3）

本节是 `W-TTR-RX3-IMP1` 的 append-only 协议增量。它新增 generic retained exclusive 的机械投影，
但不激活 `TaskTransactionRunner`、Task/Service caller、host 或配置。现有
`SUMMON_SKILL_WHOLE_PASS` whole-pass 保持原 wire、原业务 callback 与原执行顺序；generic 与 whole-pass
只共享既有单一 authority/registry/ledger/input queue/input worker，不共享业务 intent/result。

### 13.1 closed control wire

`RequestContext.operation` 与 `CommonOutcome.operation` 增加
`EXCLUSIVE_INTERACTION_CONTROL`。control payload 必须恰有以下四个 key：

```text
ExclusiveInteractionControlRequest payload {
  command: ACQUIRE | RELEASE | ABORT,
  exclusiveSessionId: non-blank string,
  bindingGeneration: non-negative long,
  step: positive long
}
```

只有这三种 command；pause/resume 不产生第四种 control。resume 由既有 task-run lifecycle publication
唯一推进同一 local handle 与 Cloud H/K projection 的 `bindingGeneration`，并在 successor registration、
readiness、handle snapshot、generation 与 step cursor 全部发布后，最后一次恢复既有 pause token。

control outcome payload 必须恰有以下六个 key：

```text
ExclusiveInteractionControlOutcome payload {
  command: ACQUIRE | RELEASE | ABORT,
  exclusiveSessionId: non-blank string,
  bindingGeneration: non-negative long,
  step: positive long,
  mechanicalStatus: ACQUIRED | RELEASED | ABORTED |
                    NOT_EXECUTED | STOPPED | UNKNOWN,
  ownerReleased: boolean
}
```

`ACQUIRED/RELEASED/ABORTED` 分别只允许对应 `ACQUIRE/RELEASE/ABORT`，且映射
`executionState=EXECUTED`；`NOT_EXECUTED/STOPPED/UNKNOWN` 分别映射同名 execution state 并可用于三种
command。`RELEASED/ABORTED` 必须 `ownerReleased=true`；`ACQUIRED` 必须为 false。UNKNOWN 不证明物理
step 是否发生，但 exact local terminal snapshot 可以独立证明 worker owner 已释放，因此 UNKNOWN 的
`ownerReleased` 可为 true 或 false。control request/outcome 全部沿用 Full R0 request/outcome digest、late-final、
final-consumed acknowledgement、receipt 与 compact；control 不允许可信 NOT_EXECUTED renewal。

### 13.2 session-bound CAPTURE / INPUT_BUNDLE

新增闭合 cursor：

```text
ExclusiveSessionStepRef {
  exclusiveSessionId: non-blank string,
  bindingGeneration: non-negative long,
  step: positive long
}
```

`CaptureRequest` 与 `InputBundleRequest` payload 最后增加可选 `sessionRef`。键缺失表示普通调用；显式 null、
缺字段、额外字段或非法 cursor 均由 DHXY strict codec 拒绝。Cloud DTO 与 DHXY payload 均使用 NON_NULL，
所以普通 null 路径完全不写该键；其 payload bytes、JCS input 与 requestDigest 相对本 cohort 前逐字不变。
non-null 时三个 cursor 字段全部进入 canonical request bytes/digest，且只能由同一 retained authority/registry
内部路由生成或接受，不能由 public caller 提供 raw session id。

同一 session 同时最多一个 bound step。Cloud 在现有 `CaptureAction/InputBundleAction` retained request 上保存
exact operation/action handle/outcome；DHXY 在现有 `InFlightExclusiveHandle` 内保存 current session generation、
唯一 bound request generation/step/requestId/requestDigest 与唯一 next-step cursor。pause/resume 只推进 current
session generation，已绑定 request 的原 generation/step 不变；它可在 successor ACTIVE generation 下完成并只推进
一次 cursor。UNKNOWN 保留 bound request 与 cursor，禁止 ordinary fallback、换 request/action id 或重做截图/输入。

### 13.3 local 120-second owner and dual completion

ACQUIRE 只创建一个现有 `InputActionRequest` 的 retained-session mode，并放入现有全局
`InputActionQueue/InputActionWorker`。该 request 从成功 enqueue 起持有固定 `120_000ms` unpaused budget；只使用
`System.nanoTime()` 单调 deadline。既有 pause progress 的 cumulative nanos 在同一 `progressLock` 下 exact-once
补偿，ACTIVE 计算/断连继续扣减，任何 wire timeout 或 step 都不能重置预算。无 timer、线程、executor、第二 queue、
TTL/LRU/takeover 或自动 retry。

同一 request 有两个 exact-once completion：

```text
sessionAdmitted: ADMITTED | REJECTED_NOT_EXECUTED | ADMISSION_UNKNOWN
terminal: InputActionExecutionResult
```

worker 通过 admission/identity/safety、进入同一 input transaction并完成 focus 后，才原子发布 ADMITTED；ACQUIRE
handler 只等待 `sessionAdmitted` 并立即返回 closed control outcome，所以同步 polling loop 可继续取得 CAPTURE、
INPUT、RELEASE 或 ABORT。worker 在后台继续持有同一 request 与唯一全局 input transaction。RELEASE/ABORT
向容量 1 的内部 signal lane 投递 terminal signal，并且只有这两个 control 等待 terminal completion。

queue reject、remove-before-admit、worker-before-admit failure、deadline 与 interruption 路径必须先补齐 admission
future，再补齐 terminal future；terminal commit 与 lane offer 共用同一 progress lock，worker finally 必须完成任何
未消费 step future。未 admission/未开始可证明时映射 NOT_EXECUTED；ADMITTED 或任何 physical action 可能已开始时
映射 UNKNOWN。预算归零停止接收新 step并释放同一 worker；已 exact final-consumed 的旧 step 不因后续 session
到期回退 UNKNOWN。

若 current step 已进入 UNKNOWN，RELEASE/ABORT 仍可沿同一 session/cursor 等待 retained request 的 terminal snapshot，
并以 `ownerReleased=true` 独立证明 local input owner 已释放；该 terminal proof 与 unresolved step 同时保留，绝不推进、
清除或合成该 step/transaction outcome。Cloud 只有在 step exact late final 已消费且 terminal child receipt 已 compact 后
才退休 session/quota；任一侧未闭合都继续持有 UNKNOWN fence。

RELEASE/ABORT 对 returned terminal snapshot 的 closed 映射为：`COMPLETED` 才返回对应
`RELEASED/ABORTED + EXECUTED + OK`；`NOT_STARTED` 且 safety reason 为 `STOP_REQUESTED` 时返回
`STOPPED`，其它可证明未开始返回 `NOT_EXECUTED`；`PARTIALLY_COMPLETED` 或 `STARTED_UNKNOWN` 返回
`UNKNOWN`。上述四类在 worker terminal 已发布后均可独立携带 `ownerReleased=true`，但 outcome code 必须保留
deadline、stop、task-run/window mismatch、transport 或 input failure 的 exact 分类，不能由 owner-release 事实改写为 `OK`。

### 13.4 stable action identity, fences, capacity

generic transaction 必须消费 retained task/phase state 铸造的 non-mintable `TaskTransactionAction`：稳定
`ActionAddress(phaseCode,actionSlot)` + 上层显式 occurrence。same address+occurrence 返回同一 handle；只有前一
terminal child 已 exact final-consumed/compacted且上层显式提交 `occurrence+1` 才可铸下一动作。三个 child slot固定为
`<actionSlot>:exclusive-acquire|release|abort`；RELEASE/ABORT 互斥。本 cohort 没有默认 handle，transactionName、
线程与随机 session UUID 都不能推进业务 occurrence。

ACQUIRE child 每个 parent occurrence 都出现，因此保持普通连续 semantic frontier。RELEASE/ABORT 是互斥分支，未选
分支不产生 wire detail；仅这两个 terminal-control child slot 允许 occurrence 在已 compacted frontier 之后严格递增但
跳过未选择的 parent occurrence。首次选择也可从当前 parent occurrence 开始。该稀疏规则不适用于普通 action、ACQUIRE、
attempt renewal 或其它 control slot；Cloud retained state 仍逐个验证 parent `occurrence+1`，所以稀疏 child 不能推进或
伪造 parent transaction identity。

所有副作用继续同时受三道 fence：handler 的 strict payload/digest + admission snapshot + ledger claim；input worker
在 pause wait、focus与 admission 前后的 exact registry/window/identity检查；每个 physical action、focused fallback与
分段 sleep 的 immediate checkpoint。session CAPTURE 在唯一 frame 紧前检查 bound step和 retained worker仍 live，
frame 后再次检查；post fence失败丢弃 frame并返回 UNKNOWN。任何 session/generation/step/window/stopEpoch/
runRevision 不匹配都 fail closed，不能转普通 queue/capture路径。

Cloud 保持每 run 一个 active closed variant、每 tenant 64、全局 1000；Summon 与 generic 共用 quota。DHXY 保持
每 run 一个 `inFlightExclusiveHandle`、一个 operation ledger、一个全局 input queue/worker及每 session容量 1 的 lane。
Cloud live quota 只在对应 terminal child 的 DHXY final-consumed receipt 已 compact 后释放；同一 coordinator 的单一
同步 compaction observer 通知现有 authority 退休 owner，不创建 queue、thread、timer 或第二 registry。terminal
publication 若先发生则保留 entry/fence，compaction 后再原子退休；没有 exact receipt 时继续 fail closed。
两侧 process restart 都不恢复 session；confirmed cloud incarnation变化沿既有 lifecycle invalidation 唤醒并退出旧
worker，Cloud 保留 UNKNOWN/final-consume fence且不自动 acquire。无已批准业务差异；按 `0114604e` 基线等价迁移。
