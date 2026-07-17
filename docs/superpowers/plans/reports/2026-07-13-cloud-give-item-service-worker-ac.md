# GiveItemService Cloud Lift - Internal Worker AC

## Parent Task Brief #1 - `W-GIVE-D1` - 2026-07-13T16:00:00-04:00

Internal Worker AC 只做 `GiveItemService` 整类迁云 Design #1，不改 Java。先读 `D:\mavenProject\DHXY\AGENTS.md`、
`docs\DHXY_CONTEXT.md`、`docs\ACTIVE_WORK.md`、迁移矩阵、DHXY committed baseline `0114604e` 的
`GiveItemService.java`/`BagService.java`/调用者，以及 Cloud 当前 retained context/port/Full R0/turn/exclusive authority。

### 唯一写集

- 仅本 append-only 日志。
- 两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不运行 Maven，不启动任何 runtime，不做 Git mutation。

### 必须回答

1. 列出 HEAD `executeGive` 与 `executeGiveDirectForExclusive` 的逐步业务顺序、sleep、template threshold、bag selection、
   click 与失败返回；不得改变任何 phase/retry/fallback/timing。
2. 明确永久留本地的能力：window binding、capture/template match、bag UI inspection、screen coordinate/randomization、
   physical input、single input worker 与安全拒绝；Cloud 只能拥有业务编排和 retained action state。
3. 设计 Cloud business API 如何复用同一个 per-run context、turn 与稳定 retained action identity，机械调用只走 typed
   `CloudTaskServicePort`/generic exclusive（若 RX3 未通过须列顺序 blocker）；不得把 `isInputWorkerThread` 或本地线程名搬云。
4. 给出 normal 与 already-inside-exclusive 两条路径的单一语义，不允许 queue-in-queue；说明 BagService 依赖、整个 give flow
   是否必须作为同一 retained exclusive session，以及 UNKNOWN/NOT_EXECUTED/final-consume 后是否允许重试。
5. 给出最小精确 New/Modify 文件表、依赖 DAG、与 A resolver/B TeamReturn/AB RX3 的写集冲突、容量/tenant/window/revision/
   stop fence、Cloud package + DHXY compile 门，以及静态代码不能证明的真实双窗口验收证据。

先在本日志追加 `CLAIMED`（task、claimedAt、uniqueWriteSet=仅本日志），再写 Design #1。Worker self-QA 不是父级批准；
父级 DESIGN APPROVED 前不得改 Java。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## RESUMED/CLAIMED - W-GIVE-D3 - 2026-07-13T16:48:49-04:00

- task：`W-GIVE-D3`，继续原 Internal Worker AC 会话，只关闭 `Parent Design Review #2` 的 P1/P2。
- claimedAt：`2026-07-13T16:48:49-04:00`。
- uniqueWriteSet：仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-give-item-service-worker-ac.md`，真实 EOF append-only。
- 前置读取：已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、上述 Parent Review #2，并读取两仓 `git status --short --branch`。DHXY 当前 `thin-client-design`、Cloud 当前 `navigation-migration`，两仓已有大量 dirty/untracked；全部视为其它 owner 工作并保持原样。
- 当前冻结：两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；本任务不运行 Maven/runtime，不执行 Git mutation。

## Design Repair #2 Delta - W-GIVE-D3 - 2026-07-13T16:48:49-04:00

本 Delta 只闭合未来 `W-GIVE-F0` 的 fixed Give button `WINDOW_FACT` 双仓协议与真实文件/方法表。Repair #1 已通过的 exact `BindingAccess`、单次 `BoundWindowCaptureService` capture、同帧 scale bracket、physical px 到 `WINDOW_CLIENT_PX`、唯一 frame/template flush owner、frame 后不稳定为 `UNKNOWN`，以及 Design #1 已通过的 Give 业务顺序/阈值/sleep/失败/retry/fallback，均不重写、不改变。RX3 仍为 design approved、`W-TTR-RX3-IMP1` implementation in flight；以下未来文件在 AB 双构建并获父级 source/build pass 前仍不可写。

### 1. 源码现实与包路径纠正

- Cloud 权威包实际为 `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\`；Cloud `WindowFactKind`、sealed `WindowFact`、`WindowFactOutcome` 和 `RemoteCommandOutcomeEnvelope` 均在此目录。
- DHXY 对称 remote 权威包实际为 `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\`。此前出现的 `com/bot/dhxy/remote` 及把 Cloud 写成 `com/bot/dhxy/cloud/remote` 的路径均作废；禁止在错误包新造第二套 DTO。
- 唯一现存协议 schema 权威文件为 `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md`；Cloud 仓当前没有第二份 schema 文件，不新增镜像副本。

### 2. 双仓逐字段同构 closed fact

新 kind 两仓 wire enum 名完全相同：`GIVE_ITEM_BUTTON_MATCH`。

#### 2.1 Request closed shape

Cloud typed request 仍为既有 `WindowFactRequest { context, factKind }`。扁平 command 的 operation payload **必须且只能**含一个 key：

```text
allowed = required = { factKind }
factKind = GIVE_ITEM_BUTTON_MATCH
```

request 不得携带 template 路径/id、threshold、capture region、scale、screen/window base、坐标、随机量、match score 或 retry 参数；它们不是 Cloud 可调业务输入。`RemoteCommandEnvelope.payload(RemoteRequest)` 继续只把 enum `.name()` 写入 `factKind`。因此 `requestDigest` 的 typed tree 仍严格是 `WindowFactRequest.context`（去掉 `context.requestDigest`）加 `factKind=GIVE_ITEM_BUTTON_MATCH`，没有新 optional/default key。

#### 2.2 Outcome envelope 与 fact exact keys

`WINDOW_FACT` outcome 的 operation payload 对所有 execution state 都必须恰有两个 key，缺失/额外 key 均拒绝：

```text
allowed = required = { factKind, fact }
factKind = GIVE_ITEM_BUTTON_MATCH
```

当 `fact` 非 null 时，它必须是 immutable typed `GiveItemButtonMatchFact`，Cloud/DHXY 字段名、wire 类型和顺序语义逐项同构：

```text
GiveItemButtonMatchFact {
  matchStatus: MATCHED | NOT_FOUND,
  coordinateSpace: WINDOW_CLIENT_PX | null,
  x: integer | null,
  y: integer | null,
  observedWindow: ObservedWindowBinding
}

ObservedWindowBinding {
  windowId: non-blank string,
  nativeHandle: normalized unsigned-decimal string,
  processId: positive long,
  playerIdentityEpoch: non-negative long
}
```

`GiveItemButtonMatchFact` 外层必须恰有 `{matchStatus,coordinateSpace,x,y,observedWindow}` 五个 key，`observedWindow` 必须恰有上述四个 key。不得增加 `JsonNode` extension、raw map、score、template、physical point、screen base、scale 或诊断字段。

#### 2.3 MATCHED / NOT_FOUND closed matrix

| `matchStatus` | `coordinateSpace` | `x/y` | `observedWindow` | 语义 |
|---|---|---|---|---|
| `MATCHED` | 必须非 null 且只可为 `WINDOW_CLIENT_PX` | 两者必须为 JSON integer；不 clamp、不改随机合同 | 必须非 null、四字段闭合 | 已按 Repair #1 的稳定同帧 scale 完成换算并应用唯一一次 `(20,8)` 随机。 |
| `NOT_FOUND` | 必须显式 null | 两者必须显式 null | 必须非 null、四字段闭合 | 只有稳定 exact frame/scale/binding 下阈值 `0.85` 未命中；没有点，不触发 click。 |

`NOT_FOUND` 不是 transport/error code；它与 `MATCHED` 都是 `OBSERVED/OK` 下的 closed fact status。模板/provider/matcher 异常仍按 Repair #1 为 `UNKNOWN/INTERNAL_ERROR`，不得伪装为 `NOT_FOUND`。

#### 2.4 execution-state / null matrix

| `executionState` | `factKind` | `fact` wire key/value | 允许性 |
|---|---|---|---|
| `OBSERVED` | 必须 `GIVE_ITEM_BUTTON_MATCH` | key 必须存在且为上述合法 `MATCHED` 或 `NOT_FOUND` object | 唯一可发布事实的状态；code 为 `OK`。 |
| `NOT_EXECUTED` | 必须保留请求的 kind | key 必须存在且显式 null | capture 前未开始或其它既有 clean rejection；不得带 stale fact。 |
| `UNKNOWN` | 必须保留请求的 kind | key 必须存在且显式 null | frame 后不稳定/内部不确定；不得带点或 NOT_FOUND。 |
| `STOPPED` | 必须保留请求的 kind | key 必须存在且显式 null | 沿用既有 `WindowFactOutcome` 非 OBSERVED 规则。 |
| `EXECUTED` | 任意 | 任意 | `WINDOW_FACT` 固定禁止。 |

JsonNode 只允许作为 `RemoteGameCommand`/`RemoteGameOutcomeEnvelope` 的瞬时 wire tree：进入 DHXY 后先经 operation codec 的 exact-key/enum decode，进入 Cloud 后先经 `RemoteCommandOutcomeEnvelope.windowFactOutcome()/parseFact()`；provider、handler fact、Cloud Service 和 retained state 之间只传 immutable typed object。任何未知/缺失/explicit-null 违规在业务消费前 fail closed，禁止 raw map/JsonNode 直传逃逸 closed contract。

### 3. outcomeDigest 精确字段与双仓字节

`requestDigest` 算法不变。`outcomeDigest` 继续是 RFC 8785/JCS 的 typed `WindowFactOutcome`，删除且仅删除 `common.outcomeDigest` 后的 SHA-256；它不是直接 hash 外层 transport envelope。其字段闭包为：

```text
common {
  contractVersion, operation, requestId, actionId, taskRunId,
  requestDigest, executionState, code,
  message (仅 non-null 时进入 typed tree),
  acceptedAtEpochMs, finishedAtEpochMs
}
factKind
fact (仅 OBSERVED typed tree 非 null 时进入)
```

- `MATCHED` digest 必须覆盖 `matchStatus,coordinateSpace,x,y` 以及 `observedWindow` 四字段。
- `NOT_FOUND` wire 的 `coordinateSpace/x/y` 虽要求显式 null以闭合 schema，但 Cloud 现有 `RemoteProtocolDigests` 使用 `NON_NULL` typed serialization；canonical typed tree 只覆盖 `matchStatus` 与 `observedWindow`，不覆盖三个 null member。
- `NOT_EXECUTED/UNKNOWN/STOPPED` wire 的 `fact` 必须显式 null，但 typed digest tree 省略 `fact`；`factKind` 始终参与 digest。
- frame/image、physical match point、template、threshold、`systemScaleRatio` 不属于 outcome fact，均不进入该 outcomeDigest；`requestDigest` 已间接绑定原请求的 semantic address/window/stop/revision。
- Cloud `RemoteProtocolDigests.computeOutcomeDigest(...)` 已从 typed `WindowFactOutcome` 生成 NON_NULL tree，且 `withComputedOutcomeDigest(...)` 已按 outcome 类型处理，无 fact-kind switch，因此是 `0 Modify`。
- DHXY `RemoteProtocolDigests.computeOutcomeDigest(...)` 当前直接把 raw outcome payload 合并进 typed tree；若 NOT_FOUND 的 nested explicit null 未先归一化，会与 Cloud NON_NULL bytes 分叉。因此未来必须先调用 strict `RemoteOperationPayloadCodec.readWindowFactOutcome(...)` 得到 typed payload，再用同一 NON_NULL mapper 重建 `factKind/fact` 后 canonicalize；只改 WINDOW_FACT typed normalization，不改 RFC 8785、SHA-256、其它 operation 或 imageBytes 规则。

### 4. Cloud 未来原子文件/方法表（当前全部冻结）

| 动作 | 真实文件 | 精确方法/类型锚点 | 必要变更 |
|---|---|---|---|
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFactKind.java` | `enum WindowFactKind` | 增加 `GIVE_ITEM_BUTTON_MATCH`，与 DHXY enum 逐字同名。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFact.java` | sealed permits；nested `GiveItemButtonMatchFact` 与 `MatchStatus` | permits 新 immutable record；实施五字段及 MATCHED/NOT_FOUND constructor matrix，复用 `CoordinateSpace.WINDOW_CLIENT_PX` 与 `ObservedWindowBinding`。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFactOutcome.java` | `matches(WindowFactKind, WindowFact)` | exhaustive 增 `GIVE_ITEM_BUTTON_MATCH -> fact instanceof WindowFact.GiveItemButtonMatchFact`；既有 OBSERVED/non-OBSERVED null gate 不变。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandOutcomeEnvelope.java` | `windowFactOutcome(CommonOutcome)`、`parseFact(...)`、exact-key constants/helpers | outer `{factKind,fact}` exact keys；按 execution state 校验 fact null；新 kind 分支先校验五个 fact keys及四个 observedWindow keys，再 strict decode immutable record。既有四 kind 也保留各自 closed key sets，不得 default/raw fallback。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteGameCommandBroker.java` | `validateAgainstPending(RemoteRequest, RemoteOutcome)` | 对 OBSERVED 新 fact 比较 `fact.observedWindow` 与 pending `context.window` 四字段；kind correlation 既有检查保留。 |
| Future Modify | `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md` | `§6 WINDOW_FACT`、`§8 outcomeDigest` | 同一双仓原子波登记新 enum、request/outcome exact keys、五字段 fact、两张 null matrix 与上述 canonical digest字段；当前 D3 不写 schema。 |

### 5. DHXY 未来原子文件/方法表（当前全部冻结）

| 动作 | 真实文件 | 精确方法/类型锚点 | 必要变更 |
|---|---|---|---|
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactKind.java` | `enum RemoteWindowFactKind` | 增加 `GIVE_ITEM_BUTTON_MATCH`；strict Jackson enum decode 因此接受且只接受该 canonical 名。 |
| New | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactPayload.java` | sealed marker interface | permits 既有四个 immutable fact 与新 Give fact，使 `RemoteWindowFactOutcomePayload.fact` 不再以 JsonNode/raw map 表示。 |
| Modify | `...\com\bot\dhxy\cloud\remote\RemoteBindingFact.java`、`RemoteGeometryFact.java`、`RemoteFocusFact.java`、`RemoteStopFact.java` | class declaration | 仅 implements `RemoteWindowFactPayload`，字段与现行业务不变；用于闭合 sealed union。 |
| New | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGiveItemButtonMatchFact.java` | immutable Lombok value/builder；nested `MatchStatus` | 与 Cloud 五字段逐名同构，constructor/build path强制 MATCHED/NOT_FOUND matrix；不暴露 frame/provider/JsonNode。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactOutcomePayload.java` | `fact` field/type | `JsonNode fact` 改为 nullable `RemoteWindowFactPayload fact`；`factKind` 保留。wire JsonNode 只由 codec 序列化产生。 |
| New | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalGiveItemButtonFactProvider.java` | `match(BufferedImage, double)`（最终签名按父级重锚） | 只返回 immutable `RemoteGiveItemButtonMatchFact` 的 MATCHED/NOT_FOUND事实；继续严格服从 Repair #1 的单帧、0.85、scale换算和一次随机，不拥有 handler frame。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java` | `executeWindowFact(...)` exhaustive switch；新 `executeGiveItemButtonMatchFact(...)`；`terminal(...)` WINDOW_FACT serialization；`emptyOutcomePayload(...)` | 新 kind 走 Repair #1 exact-capture helper；OBSERVED发布 typed fact，非 OBSERVED发布同 kind/null fact；terminal 对 WINDOW_FACT 调 codec closed encoder。既有 BINDING/GEOMETRY/FOCUS/STOP 分支不变。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteOperationPayloadCodec.java` | `readWindowFact(...)`、新 `readWindowFactOutcome(...)`/`toWindowFactOutcomeTree(...)`、exact field sets | request 继续 exact `{factKind}`；outcome exact `{factKind,fact}`，按 kind strict decode sealed payload，实施 Give 五字段/observedWindow/状态矩阵；未知 key/kind/type/null fail closed。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteProtocolDigests.java` | `computeOutcomeDigest(RemoteGameOutcomeEnvelope)` | WINDOW_FACT 先 strict decode并以 NON_NULL typed payload重建后 hash，确保 NOT_FOUND/non-OBSERVED null 与 Cloud byte-for-byte 同构；request digest与 canonicalizer均不改。 |

表中 `...\com\bot\dhxy\cloud\remote\` 仍唯一展开为 `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\`，不是省略包层级或授权相对路径猜测。

### 6. 明确 `0 Modify` 项与源码理由

| 仓库/文件 | `0 Modify` 源码理由 |
|---|---|
| Cloud `RemoteRequest.java` | sealed union 已 permits `WindowFactRequest`；新增的是既有 request 内的 enum variant，不是新 operation/request type。 |
| Cloud `WindowFactRequest.java` | constructor 已 generic require non-null `WindowFactKind`，字段只有 `context,factKind`，无需新分支。 |
| Cloud `RemoteCommandEnvelope.java` | `from(RemoteRequest)`/`payload(RemoteRequest)` 已对任何 `WindowFactRequest` 写唯一 `factKind().name()`；request allowed keys不变。 |
| Cloud `CloudTaskRunExecutionGate.java` | `newWindowFactRequest(...)` 与 `newPausedObservationWindowFactRequest(...)` 已 generic 接收 kind、计算 digest并绑定 retained identity；本 Give 业务不新增 paused 使用或 builder字段。 |
| Cloud `RemoteGameClientPort.java`、`CloudTaskRunCommandExecutor.java` | `readWindowFact(...)` 已按 generic kind构造、重投校验并返回 `WindowFactOutcome`；不需 Give-specific port。 |
| Cloud `RemoteProtocolDigests.java` | `computeRequestDigest/computeOutcomeDigest` 从 typed request/outcome整树生成；`withComputedRequestDigest(WindowFactRequest)` 和 `withComputedOutcomeDigest(WindowFactOutcome)` 均无 fact-kind switch，新 sealed record自动进入 canonical tree。 |
| Cloud `RemoteOperation.java`、`RemoteOutcome.java` | operation仍是 `WINDOW_FACT`，outcome仍是 `WindowFactOutcome`；禁止新造 Give operation/outcome旁路。 |
| Cloud `CoordinateSpace.java`、`ObservedWindowBinding.java` | 已有 `WINDOW_CLIENT_PX` 与四字段 exact observed binding，字段足够且不得复制。 |
| DHXY `RemoteWindowFactCommandPayload.java` | 已是 immutable `{factKind}`，新增 enum 后 `readWindowFact(...)` 可 strict decode；无第二 request DTO。 |
| DHXY `RemoteGameOperation.java` | operation仍是既有 `WINDOW_FACT`，不新增业务 operation。 |
| DHXY `RemoteGameCommand.java`、`RemoteGameOutcomeEnvelope.java` | 顶层 transport envelope字段不变；其 JsonNode 仅为 wire边界，closed fact必须由 codec即时 typed decode/encode，不能传给 provider/业务。 |
| DHXY `RemoteCoordinateSpace.java`、`RemoteObservedWindowBinding.java` | 已与 Cloud 的 coordinate/binding wire字段同构；新 fact直接复用。 |
| DHXY `HttpRemoteCommandTransport.java` | poll/outcome顶层 transport、duplicate detection与 digest验证路径不变；operation payload闭合由 `RemoteOperationPayloadCodec` 和 Cloud parser承担。 |
| DHXY `BoundWindowCaptureService.java`、`CoordinateHelper.java`、`GameClientTracker.java` | Repair #1 已确认只复用前者现有单次 `captureRegion(...)`；后两者不参与 fixed fact，均不改。 |
| DHXY `InputActionRequest/Queue/Worker`、所有 host/caller/Task | F0 只形成事实；点击仍由后续已批准 retained input路径消费，且 RX3在途 fence禁止当前或抢先修改。 |
| 两仓 Maven/resources/tests | 不新增 dependency；固定模板资源、阈值与测试策略不变。当前及未来 F0均不得借此改模板或业务证据。 |

### 7. 原子实施顺序与 fence（状态不变）

未来 `W-GIVE-F0` 必须把上述 Cloud enum/sealed fact/matches/parser/broker、DHXY enum/typed fact/handler/codec/digest 以及唯一 schema 作为一个双仓 closed protocol cohort；禁止先发单边 enum、允许 unknown field、临时 JsonNode adapter 或 mixed-version兼容。Cloud package 与 DHXY compile 必须都通过后才有双仓 wire 证据。

但当前顺序仍是：`W-TTR-RX3-IMP1 implementation in flight -> AB Cloud+DHXY 双构建 -> 父级 source/build pass -> 重读/重锚以上共享源码 -> 父级另行批准 W-GIVE-F0 -> 才可原子实施`。本 Delta 不解除 RX3 的 shared remote/handler/registry/input-worker/digest/schema fence，不预批准 F0/C0，也不授权 host/caller/runtime。

### Worker AC self-QA（不构成父级批准）

- P1：已给出 Cloud/DHXY 对称 enum、sealed/immutable fact、matches/parser、handler、codec、必要 digest/schema 的 closed shape、exact keys、状态矩阵与真实方法表；未用 JsonNode/raw map代替 typed contract。
- P2：Cloud 与 DHXY 实际包路径已分别纠正，DHXY 所有未来 remote 文件统一位于 `com/bot/dhxy/cloud/remote`，并逐项列出 Modify/New/0 Modify及源码理由。
- 保持 Repair #1 exact capture/scale/flush 与 RX3 implementation-in-flight fence；未重写 Give 业务。
- 本轮实际仅向本日志真实 EOF 追加；未修改 Java/Maven/schema/resources/tests/host/caller，未运行 Maven/runtime，未执行 Git mutation。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

Internal Worker AC 到此停手，等待父级复审；本 self-QA 不是 review/approval。

## Parent Design Review #3 - DESIGN APPROVED / Implementation remains gated - 2026-07-13T16:58:00-04:00

父级按 `0114604e:GiveItemService`、当前 Cloud `WindowFactKind/WindowFact/WindowFactOutcome/
RemoteCommandOutcomeEnvelope/RemoteGameCommandBroker/RemoteProtocolDigests` 与 DHXY
`RemoteWindowFactKind/RemoteWindowFactOutcomePayload/RemoteOperationPayloadCodec/RemoteProtocolDigests/
LocalRemoteGameCommandHandler/BoundWindowCaptureService` 复审 D3。结论：**DESIGN APPROVED，P0=0/P1=0/P2=0**。

批准依据：

1. `GIVE_ITEM_BUTTON_MATCH` 复用唯一 `WINDOW_FACT`，不新增 business/raw operation；两仓 enum、sealed fact、strict
   outer/nested keys、state/null matrix、broker pending-window correlation、codec/digest/schema 与 0-Modify 项已经闭合。
2. `OBSERVED/MATCHED|NOT_FOUND` 与 `NOT_EXECUTED/UNKNOWN/STOPPED` 分离正确；NOT_FOUND 只允许稳定 exact frame/scale/
   binding 下的 0.85 零命中，frame 后漂移与 provider/matcher 异常保持 UNKNOWN，不制造业务假阴性。
3. baseline 的模板中心、`round(physical/scale)`、唯一一次 `±20/±8` 随机、无 clamp/额外 capture/verify/retry、normal/
   direct 输入边界均保持；Cloud 只消费 `WINDOW_CLIENT_PX` typed fact，本地继续永久拥有 capture/template/randomization。
4. digest 方向正确：Cloud 继续从 NON_NULL typed `WindowFactOutcome` 生成 RFC 8785 tree；DHXY 对 WINDOW_FACT 先 strict
   decode 后重建同一 typed tree，wire 必须保留的 explicit null 不直接进入 typed digest。其它 operation digest 不改。

### 父级权威实施重锚（解决文件表中的签名歧义，不构成新 blocker）

- `LocalGiveItemButtonFactProvider.match(BufferedImage frame, double stableSystemScaleRatio)` 只返回 package-private immutable
  `MatchResult{MATCHED|NOT_FOUND, clientX, clientY}`；MATCHED 才有 x/y。provider 不接收、持有或伪造 observedWindow，也不
  flush handler-owned frame。
- `LocalRemoteGameCommandHandler.executeGiveItemButtonMatchFact(...)` 在后置 registration/revision/exact binding fence 通过后，
  以 command-bound binding 构造唯一 `RemoteObservedWindowBinding`，再把 `MatchResult` 组装成公开
  `RemoteGiveItemButtonMatchFact`。因此公开 fact 的五字段完整，而 provider 仍是纯单帧 matcher；不得让 provider 返回缺
  observedWindow 的半成品公开 fact。
- 实施时以该重锚覆盖 D3 表中“provider 直接返回 RemoteGiveItemButtonMatchFact/最终签名待重锚”的旧措辞；其余 D3 与
  Repair #1 不变量保持原样。

当前只批准设计，**不授权现在写 Java**：`W-GIVE-F0` 与 AB RX3 共享 handler/digest/schema，必须等待
`W-TTR-RX3-IMP1` 完整交付、父级双仓源码复审及 fresh Cloud package + DHXY compile 通过后，重新读取共享源码并由父级
发布实施任务。AC 当前任务完成并关闭，不自行实施、不运行 Maven/runtime、不做 Git mutation。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED - W-GIVE-D2 - 2026-07-13T16:19:08-04:00

- task: `W-GIVE-D2`
- claimedAt: `2026-07-13T16:19:08-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-give-item-service-worker-ac.md`
- scope: 仅关闭 Parent Design Review #1 的 `P1-1/P2-1`；两仓 Java/Maven/schema/resources/tests/host/caller 全冻结。

## CLAIMED - 2026-07-13T16:03:38-04:00

- task: `W-GIVE-D1`
- claimedAt: `2026-07-13T16:03:38-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-give-item-service-worker-ac.md`

## Internal Worker AC - Design #1 - 2026-07-13T16:10:55-04:00

### 0. 结论、基线与本轮边界

**结论：Design #1 完成；Java 实施当前有顺序 blocker。** GiveItem 的 Cloud 业务主体可以按 committed
baseline 0114604e 等价迁移，但不得在 generic retained exclusive RX3、完整 Cloud BagService retained adapter 与 W-TTR-1
稳定前实施。当前 RX3 D2 只是 Worker AB 的 repair 设计，尚无 Parent DESIGN APPROVED，更没有 Java；本设计不会用
SummonSkill 专用 whole-pass、普通 input bundle 拼接或第二 session authority 临时代替。

- DHXY 权威：branch=thin-client-design，HEAD=0114604e1ff5f15491d2910959c45252e893d04f。已用 git show 读取
  GiveItemService、BagService 相关完整调用闭包、DialogService 唯一直接 caller，并用 git grep 证明两个 public API
  除自归一化外只有 DialogService.executeDialogCloudPreClick -> executeGiveDirectForExclusive 一条生产直接边。
- Cloud 只读事实：branch=navigation-migration，HEAD=3b988caa010254973e03342272e6d1d6a9685b01，工作树已有他人
  Full R0/retained context/turn/whole-pass 在途文件。CloudTaskServicePort 当前只有 WINDOW_FACT、CAPTURE、
  EXECUTE_INPUT_BUNDLE 与 SummonSkill 专用 capability；没有 generic transaction runner，也没有 Give/Bag 运行主体。
- 已核对同日 Parent 已批准的 Worker T GiveItem Design #1 与 Worker S Bag D3。本 AC 设计保持其业务边界，只补入当前
  RX3 D2 的显式 business action handle、固定 120 秒非暂停预算、admitted/terminal 双 completion、无 wire REBIND、
  UNKNOWN 不续代约束；若本文与旧设计的自动 occurrence/旧 RX3 表述冲突，以本节及后文为准。
- 当前两仓 dirty/untracked 全部保留。本轮除本 append-only 日志外没有写任何文件；不运行 Maven、test、application、
  server、Task、poller、UI、capture 或 input，不执行 Git mutation。

**按钮模板说明：** D:\mavenProject\DHXY\images\template\300huan\btn_give.png 是本地“给予”按钮匹配模板，
属于模板资产，不是 live incident/historical screenshot；本设计不修改、替换或删除其像素。

### 1. 0114604e 两路逐步业务合同

#### 1.1 executeGive（normal）

1. 若本地实际已处于唯一 dhxy-input-action-worker，立即转 direct body；该判断只是在本地识别既有 owner，不能迁成
   Cloud thread-name/ThreadLocal 规则。
2. 记录 started 日志。
3. TaskSleep.sleep(800) 一次；完整结束才继续，中断返回 false。无 retry、补时或第二次 sleep。
4. 调 BagService.findAndSelectItem(GIVE_BAG,targetItemTemplate,knownBagIndex)。
5. GIVE_BAG 固定 autoManageUI=false，以 refreshWindowState 后的 exact window base 为 anchor，不 Alt+E 开关包。
6. knownBagIndex 在 0..4 时先扫该页；未成功再按 visible-page cache、item-page cache、0..4 顺序扫余页并跳过 known 页。
   每页先点 tab，hold=100ms，等待 500ms；grid 为 base+(359,276)，大小 308x206；截图写 window-scoped bag_scan.png。
7. item template 固定从 images/template/<targetItemTemplate> 读取，ImageFinder.findAll threshold=0.85、
   minDistance=24.0；命中点按 (10,10) 随机化后左键 hold=100ms，再等待 500ms。成功才更新 visible/item page cache。
8. Bag 任何确定 false 立即记录 aborted 并返回 false；GiveItem 不补页、不 retry、不 fallback。
9. 只调用一次 findImageAbsoluteCoordinate(images/template/300huan/btn_give.png,0.85)；null 立即 false。
10. 命中模板中心后只随机一次 (20,8)；normal 通过一个 submitAndWait 原子 bundle 执行
    CLICK_LEFT(clickX,clickY,hold=100ms) -> SLEEP(1000ms)。
11. bundle true 才记录 finished 并返回 true；queue submit/await false 返回 false。

normal 的 Bag 整段由 Bag 自己 acquire/release。Bag release 后到 give-button bundle acquire 前允许另一个窗口插入输入；
这是 HEAD 的真实调度边界，禁止为了“更原子”把 normal 扩成 select+give 的一个 whole-pass。

#### 1.2 executeGiveDirectForExclusive（already-inside-exclusive）

1. 若本地实际不在 input worker，立即转 normal；Cloud 迁移后改为 typed capability reject/normal projection，不能检查线程名。
2. 记录 exclusive started 日志。
3. 在 caller 已持有的同一 exclusive owner 内执行 TaskSleep.sleep(800)；中断返回 false。
4. 调 BagService.findAndSelectItemDirectForExclusive(GIVE_BAG,target,index)。它复用与 normal 完全相同的 base、页序、
   threshold=0.85、minDistance=24、tab/click/sleep/cache 业务，但所有输入都直接由当前 owner 执行，绝不再 submit。
5. Bag false 立即返回 false。
6. 同样只匹配一次 btn_give.png、threshold=0.85；null 返回 false；命中后随机一次 (20,8)。
7. 当前 owner 直接左键 hold=100ms，再 TaskSleep.sleep(1000)；sleep 完整才记录 finished/返回 true，中断返回 false。

direct 从 outer caller acquire 到 outer caller release/abort 始终是同一个 retained exclusive session：dialog option click、
800ms、Bag tab/capture/select、give-button match/click、1000ms 之间都不能插入其它窗口 physical input。GiveItem 只 join，
不 acquire、不 release、不 abort outer owner。

### 2. Cloud business owner 与永久 DHXY mechanics

| Owner | 唯一职责 | 明确禁止 |
|---|---|---|
| Cloud GiveItemService | 800ms -> Bag select -> button fact interpretation -> click/1000ms -> boolean 的业务阶段、显式 occurrence、阶段 retained state、是否继续与 terminal result | HWND/window search、截图、模板读取/匹配、Bag UI geometry/page input、随机屏幕点、物理输入、queue/session mint、安全降级 |
| Cloud BagService（W-BAG-D3 批准合同） | GIVE_BAG 页序/cache/match 业务与 true/false；normal/self-exclusive 与 direct/outer-child 业务入口 | GiveItem 不复制 BagLayout、cache、模板算法或 page retry |
| DHXY fixed button fact provider | exact bound window capture、固定 btn_give.png/0.85、模板中心、命中后一次 (20,8) 随机化、screen->WINDOW_CLIENT_PX correlation | 不解释 NOT_FOUND 后业务结果，不推进 Give phase，不 retry/extra verify |
| DHXY Bag/input mechanics | exact-window capture、window-scoped artifact、Bag UI inspection、tab/item click、client-px 最终换算、focus/input safety、单 InputActionWorker、normal FIFO 与 outer-session join | 不持 GiveItem stage/W/boolean，不从线程名向 Cloud声明 mode，不产生第二 input queue/worker/session registry |

Cloud public business入口只保留一个：

    boolean executeGive(
            GiveItemServicePortAdapter.Invocation invocation,
            String targetItemTemplate,
            Integer knownBagIndex)

Invocation 是 non-mintable business handle，不是 raw session id。trusted retained task/phase state必须显式提供
固定 ActionAddress(dialog,give-item) + occurrence W；same address+W 永远返回同一 Invocation，参数 bytes/mode 不同即冲突。
只有 W 的 terminal result 及所有 child exact final 已完成 final-consume、且 caller phase 明确推进下一次业务动作时，才可声明
W+1。adapter、Service、authority 都不得按方法调用次数、线程、transactionName 或 UUID 自动推进。

InvocationMode 只有 NORMAL 与 ALREADY_EXCLUSIVE：

- NORMAL：没有 outer TaskTransactionAction；复用同一个 per-run CloudTaskServiceExecutionContext、同一个 task turn 和 W，
  但不创建覆盖整个 Give 的 local exclusive session。Bag normal 与最后 click 各走自己批准的机械边界。
- ALREADY_EXCLUSIVE：caller 转交 RX3/W-TTR-1 已校验的同一个 TaskTransactionAction/opaque session projection；Give、Bag 和
  CloudTaskServicePort 都只能 join 该 handle。stale/foreign/terminal handle typed reject，绝不降级 NORMAL。

Cloud per-run context始终逐字段绑定 tenant/user/device/clientSession、taskRunId/taskType、windowId/nativeHandle/processId/
playerIdentityEpoch、stopEpoch 与 current runRevision；resume 只发布新 revision runtime并复用同一个 workflow state/W/outer stable
session lineage。旧 runtime/adapter/handle永久 stale。

### 3. queue-in-queue 与同一 session 规则

- normal：800ms 不占 local input worker；Bag self-exclusive 完成并释放；button fact只读本地截图事实，不入 input queue；
  click+1000ms 是后续独立普通 bundle。不得把 normal 升格为全流程 exclusive。
- direct：outer RX3 ACQUIRE 已让唯一 InputActionWorker 持有同一个 InputActionRequest；Bag direct 与 click bundle通过容量1
  step lane交给该 request，或 fixed fact在 handler侧做无输入 capture。任何 step 内调用 submitAndWait/
  submitExclusiveAndWait 都必须被结构上禁止，否则 worker等待自己消费队列会死锁。
- GiveItem 不复用 SUMMON_SKILL_WHOLE_PASS，也不新增 Give 专用 queue/worker/registry。它只使用 RX3 的单 authority、
  single request、same session、same nextStep 与 typed CloudTaskServicePort。
- direct 的 button WINDOW_FACT 可在 worker持有 outer request时由 handler执行，但必须绑定同一 W/current context，且前后通过
  exact binding/revision fence；它不能调用任何 input submit。最终 click必须成为 outer session 的 INPUT_BUNDLE child step。

### 4. retained state、child identity 与防重

GiveItemWorkflowState 每个 taskRun 只保留一个 active/current + 一个可重放 terminal记录，不建全局 map、TTL 或 restart restore：

- stable owner key：exact scope + taskRunId/taskType + exact window tuple + admission stopEpoch；
- business key：ActionAddress(dialog,give-item) + explicit W + InvocationMode；
- immutable intent：targetItemTemplate 原值、knownBagIndex 原值、outer action handle（仅 direct）；
- cursor：PRE_WAIT_800、BAG_SELECT、BUTTON_MATCH、BUTTON_CLICK_SETTLE、TERMINAL；
- preWaitCompleted、Bag definitive result、fact handle/outcome、input handle/outcome、terminal boolean；
- 不持 HWND base、screen absolute point、Path、image bytes、thread name、queue对象或 local session内部字段。

fixed child address：

| child slot | retained operation | owner |
|---|---|---|
| give-item/bag-select-normal | W-BAG normal root child，occurrence=W | Cloud Bag workflow |
| give-item/bag-select-outer | W-BAG direct outer child，occurrence=W | Cloud Bag workflow + same TaskTransactionAction |
| give-item/button-match | WINDOW_FACT，occurrence=W | Give adapter / CloudTaskServicePort |
| give-item/button-click-settle | EXECUTE_INPUT_BUNDLE，occurrence=W | Give adapter；direct 附 same RX3 step ref |

每个 child 的 requestId/actionId/attempt 由 Full R0 ledger从上述固定地址产生；payload bytes首次绑定后不可变化。重复调用同
Invocation只 join/读取 retained outcome，绝不重新截图、重新随机、重新选物或重新点击。checked stage mutation -> final ACK ->
local receipt/compaction 完成后才允许 cursor 前进；response到达本身不是 final-consume。

### 5. false、NOT_EXECUTED、UNKNOWN、stop/pause

| 证据 | 业务收口 | 是否允许重试/续代 |
|---|---|---|
| 800ms 被 interrupt | HEAD false；typed stop同时向 caller unwind | Give 内不重睡；W不自动+1 |
| Bag definitive false | false；不声明 button child | Give 内不重试；caller以后明确新业务调用才可 W+1 |
| button OBSERVED/NOT_FOUND | false；不发 click | 不重拍、不 fallback |
| button fact confirmed NOT_EXECUTED | typed mechanical/stale unwind，不伪装 NOT_FOUND | 不 auto-renew；同 W 仅可读 exact final |
| click normal confirmed NOT_EXECUTED 且 startedStepIndex=-1 | 对齐 submitAndWait=false，返回 false | 无 Give 内重投；final-consume 后由 caller决定新 W |
| direct child pre-start NOT_EXECUTED | outer capability/safety typed unwind；不能退普通 queue | 不重投、不降级 normal |
| 任一 side effect可能已开始后的 UNKNOWN | 不返回普通 false/true，W与原 action/fence保留 | 绝不新 attempt/W、绝不重复物理动作；只接原 request exact late final |
| STOPPED after click start / direct 1000ms中断 | 保留 partial-progress + typed stop；最终 caller stop unwind | 不能把 false当“未点击”重做 |
| exact terminal + all child final-consumed | same W duplicate返回缓存 boolean | 只有上层phase明确下一 occurrence后才可 W+1 |

RX3 direct session固定 unpaused budget=120_000ms，从唯一 session request enqueue起覆盖 queued/admitted/steps/terminal；
ACTIVE网络断连与Cloud计算（包括800ms）计入，PAUSED按同一 pause snapshot只补偿一次。budget到期：未开始动作才
NOT_EXECUTED；capture/input可能已开始则 UNKNOWN。不得新增 Give TTL、重试、cleanup或第二 deadline。

pause落在800ms时，该次 sleep允许像 HEAD一样完成，随后安全门park，preWaitCompleted不回退；pause落在Bag/fact/input时保留同W、
同child、同session/step。resume无 wire REBIND，只由 lifecycle publication原子发布新 context/generation，旧 in-flight request只收
late final。stop/terminal先使local registration/session safety失效并唤醒worker；Cloud unresolved仍保持UNKNOWN，不能据此造 FAILED。

### 6. 三道 fence与容量

1. **Cloud/current fence：**每次调用前后 revalidate exact scope、run、task type、window tuple、stopEpoch、ACTIVE confirmed
   runRevision；Invocation/child handle provenance必须属于同 retained state与同 TaskTransactionAction。
2. **Handler/ledger fence：**strict payload/digest、operation+requestId claim、exact registration/bound window、same W/session/
   generation/nextStep；duplicate只join同future。button provider前后再次核 registration/revision/binding，命中随机点只产生一次并
   转成 WINDOW_CLIENT_PX。
3. **Input-worker fence：**normal bundle与direct session child在focus/第一action、每action、sleep/checkpoint前后验证 full scope、
   taskRun/window/stopEpoch/revision、identity epoch、geometry与session step；WINDOW_CLIENT_PX只在副作用前按current exact binding
   转screen absolute。任何 mismatch fail closed且不得普通queue fallback。

容量沿用既有 owner，不加 Give 私有 quota：

- Cloud turn：10,000 retained lanes；10,000 global admitted contenders；64 per tenant/device lane。
- Full R0 action ledger：10,000 retained actions；broker 10,000 global retained requests、1,000 per owner、64 pending per owner、
  10,000 global retained input actions、1,000 per owner、1,000 global routes、64 per owner/route queue。
- RX3：1 live interaction/taskRun，64 live/tenant，1,000 live/global；direct step lane容量1。
- DHXY：RemoteTaskRunRegistry 10,000 global/1,000 owner；RemoteOperationLedger 1,000 semantic slots/64 current details/
  64 receipt outbox；全进程仍一个 InputActionWorker。
- Give state本身每run只允许一个 active fixed callsite occurrence。任何结构容量拒绝保持 typed capacity/NOT_EXECUTED，
  不等待、不淘汰、不业务 retry。

### 7. 最小精确文件表（Parent批准后的未来波次；本轮零 Java）

#### W-GIVE-F0：固定 button fact，双仓原子

Cloud Modify：

- D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFactKind.java
  - 增 closed GIVE_ITEM_BUTTON_MATCH。
- ...\remote\WindowFact.java
  - permits/nested GiveItemButtonMatchFact；MATCHED必须有WINDOW_CLIENT_PX点与exact observed window，NOT_FOUND必须无点。
- ...\remote\WindowFactOutcome.java
  - matches switch穷尽新variant。
- ...\remote\RemoteCommandOutcomeEnvelope.java
  - strict decode新fact variant。

DHXY New：

- D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGiveItemButtonMatchFact.java
  - immutable local DTO：MATCHED/NOT_FOUND、client point、observed binding。
- D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalGiveItemButtonFactProvider.java
  - 固定btn_give.png/0.85/一次(20,8) provider；只调用既有CoordinateHelper，不含业务分支。

DHXY Modify：

- ...\cloud\remote\RemoteWindowFactKind.java
  - 同构新closed kind。
- ...\cloud\remote\LocalRemoteGameCommandHandler.java
  - 注入provider、closed fact switch、前后 exact binding/revision/geometry fence与generic payload发布。
- D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md
  - 在父级批准后的同一原子波登记closed fact shape/version；当前任务不写。

零修改：template/resource、CoordinateSpace、InputBundle request/codec、digest算法、RemoteOperation union、Maven/tests。

#### W-GIVE-C0：Cloud retained/body，dormant

Cloud New：

- ...\remote\GiveItemWorkflowState.java
  - package-private single current/terminal、explicit W、cursor、child handle/outcome；无map/TTL。
- ...\remote\GiveItemServicePortAdapter.java
  - public facade + non-mintable nested Invocation；fixed child declaration、Bag normal/direct投影、checked final-consume；
    不暴露raw port/ledger/session id。
- D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\GiveItemService.java
  - 单一executeGive业务主体；不加@Component，不读thread/window/path/input。

W-GIVE-C0 不修改 Bag、RX3、turn、assembly、host或caller；只有 W-BAG-D3 与 RX3/W-TTR-1 的最终 API 已冻结后才可编译落地，
禁止先放 placeholder adapter。

#### W-GIVE-A0：per-runtime publication/caller/cutover，另卡

- Cloud Modify ...\remote\CloudTaskRunAuthorityAssembly.java：每个taskRun创建一次 GiveItemWorkflowState；initial/resume分别构造
  新revision adapter/service并复用state，随TaskServiceRuntime原子发布；复用同一turn/retained context。
- Cloud未来 com.bot.dhxy.service.DialogService：唯一caller使用固定 DIALOG_GIVE_ITEM business action handle；有outer
  TaskTransactionAction传ALREADY_EXCLUSIVE，否则NORMAL；原GIVE_ITEM_DONE/GIVE_ITEM_FAILED映射不改。
- DHXY src/main/java/com/bot/dhxy/service/DialogService.java 与 GiveItemService.java：只在全体THIN_CLIENT_V1原子cutover时
  移除本地business call/body；fixed provider、Bag UI/capture/input mechanics永久保留。
- host/UI/Task caller不由W-GIVE-C0激活；五环phase、pathing、retry/fallback保持原样。

### 8. DAG与并行写集冲突

    Full R0 final + current client-px input/fence stable
      -> AB W-TTR-RX3-D2 Parent DESIGN APPROVED
      -> RX3双仓core/wire/local single-worker实现 + Cloud package + DHXY compile
      -> W-TTR-1 runner/assembly/lifecycle实现
      -> B TeamReturn mount（先按RX3重锚）
      -> W-GIVE-F0 fixed fact（与B/RX3共享handler，必须串行）
      -> W-BAG-F0/C0/S0/A0完整实现并冻结public/direct child API
      -> W-GIVE-C0 dormant body
      -> W-GIVE-A0 assembly + Cloud Dialog caller + 全局cutover
      -> fresh双窗口runtime验收

- **External A resolver D3：当前无直接写集冲突。** A只处理DialogChoice trusted scope/state resolver及自己的固定日志；W-GIVE-F0/C0
  不改其resolver/storage/host。W-GIVE-A0只能消费已认证scope，不能另建resolver。
- **External B TeamReturn：未来直接冲突。** B mount与Give会共享LocalRemoteGameCommandHandler，并可能共享
  CloudTaskServicePort/RemoteGameClientPort/operation/codec/lifecycle。按当前父级指令固定 RX3先、B mount后、Give F0再后；
  禁止并发实现或复制协议。
- **Internal AB RX3：直接且硬顺序依赖。** RX3修改Cloud retained action/authority/state/port/broker/digest与DHXY registry/
  handler/input worker。Give不得抢写这些文件，不得以当前Summon专用whole-pass顶替。AB D2未Parent Approved时本卡只停在设计。
- **Bag：API顺序依赖、非Give写集。** Give只调用批准后的Cloud BagService normal/direct API；不得复制Bag页序/cache/layout。
  Bag A0与W-TTR-1都触及assembly，必须先原子收口，再做W-GIVE-A0。
- **A0 assembly冲突：** W-TTR-1、Bag A0、Give A0均修改CloudTaskRunAuthorityAssembly，固定顺序
  W-TTR-1 -> Bag A0 -> Give A0，每波基于前一冻结源码重锚，禁止merge覆盖。

当前没有可脱离 RX3、Bag、fixed fact的真实Give CPU/type leaf；单独创建enum、boolean policy、wrapper或空state都是无消费者壳，
本设计不造。

### 9. build与runtime gates

当前 design-only：**不运行 Maven，不启动runtime。**

未来实施门：

1. 任一 Cloud Java/protocol波稳定后，在 D:\mavenProject\dhxy-cloud-brain fresh运行 mvn -q clean package，不可skip tests/enforcer。
2. 任一 DHXY Java/protocol波稳定后，在 D:\mavenProject\DHXY fresh运行 mvn -q -DskipTests compile。
3. 双仓wire/fact同波必须两边都成功，且schema/enum/codec/digest/closed switch逐项同构；不能用单边build或stale jar交付。
4. no-local-test模式保持；未获用户点名不新增/恢复/运行test、source guard、replay或marked testcase。
5. host/caller/Task/runtime在W-GIVE-A0及整体cutover前保持dormant。

静态源码/构建不能证明的真实双窗口验收：

- NORMAL：窗口A Bag select session确实release；窗口B输入可在A button click入队前合法插入；A随后仍在自己的exact binding点击，
  不串窗、不用stale screen point。
- ALREADY_EXCLUSIVE：A从dialog option、800ms、Bag tab/select、button fact/click到1000ms均为同一session；B物理输入只排队，
  日志无nested submit、第二owner或worker deadlock。
- 两窗各自button fact均使用本窗口HWND/window-scoped frame；MATCHED点在固定模板中心的(20,8)范围内，输入前才从
  WINDOW_CLIENT_PX换算到current screen point。模板资产见：
  ![本地给予按钮模板，模板资产，不是现场截图](D:\mavenProject\DHXY\images\template\300huan\btn_give.png)
- 在800ms、Bag、fact、click started、1000ms各阶段分别pause/resume/stop；same W/action/session不重放，direct pause补偿120秒预算，
  stop后single worker可释放。
- click开始后制造transport loss：Cloud保持UNKNOWN且无新request/action/W；exact late final只解析原请求，final-consume后才terminal。
- 日志必须可关联tenant/device/session/taskRun/window/HWND/process/playerEpoch/stopEpoch/runRevision/W/action/attempt/
  exclusiveSession generation/step、startedStepIndex与lastCompletedStepIndex；最终仍只映射原GIVE_ITEM_DONE/GIVE_ITEM_FAILED，
  无额外retry/fallback/verify。

### 10. Self-QA（不构成批准）

- P0自检：0。没有Cloud HWND/capture/template/input owner，没有第二queue/worker/registry，没有queue-in-queue或UNKNOWN重放设计。
- P1自检：0。800ms、GIVE_BAG、页序/threshold/minDistance、item随机(10,10)、button模板/0.85/(20,8)、
  click100ms、post1000ms、normal可插入/direct不可插入及五个false点均按0114604e冻结。
- P2自检：0。两个API/caller、typed mode、explicit W、final-consume、NOT_EXECUTED/UNKNOWN、120秒budget、容量、三道fence、
  精确文件表/DAG/冲突/build/runtime gate均已覆盖。
- 外部 blocker：AB RX3 D2尚未Parent DESIGN APPROVED/实现；Cloud Bag主体尚未实现。它们阻止Java，不阻止本Design #1交付。
- 本轮实际写入只有本日志CLAIMED与本Design #1；未改Java/Maven/schema/resources/tests/host/caller/其它报告/Git。

**无已批准业务差异；按0114604e基线等价迁移。**

Internal Worker AC 到此停止，等待父级审查；本self-QA不是review/approval。

## Parent Design Review #1 - BLOCKED - 2026-07-13T16:17:00-04:00

父级独立对照 DHXY committed baseline `0114604e` 的 `GiveItemService`、`BagService`、唯一生产 caller
`DialogService.executeDialogCloudPreClick(...)`，以及当前 `LocalRemoteGameCommandHandler`、
`BoundWindowCaptureService`、`CoordinateHelper` 和 AB RX3 权威日志复审。本稿对 800ms、`GIVE_BAG`、0.85、
randomized `(20,8)`、click hold 100ms、post-click 1000ms、normal 可插入/direct whole-pass 不可插入、
stable W/final-consume/UNKNOWN 不续代的冻结方向正确；但当前结论为 **BLOCKED，P0=0/P1=1/P2=1**，不得落 Java。

### P1-1：fixed button fact 仍通过 `CoordinateHelper`/tracker 读取非 exact-command 窗口

- **证据：**Design #1 `:231-232` 明确要求 `LocalGiveItemButtonFactProvider` “只调用既有
  `CoordinateHelper`”；而 `CoordinateHelper.findImageAbsoluteCoordinate(...)` `:136-145` 依次调用
  `tracker.updateGlobalVision()`、`tracker.getLatestVisionPath()`、`tracker.refreshWindowState()`、
  `tracker.getWindowBaseX/Y()`。远程 handler 的命令权威则是
  `requireBoundWindow(...) -> BindingAccess`，当前仅在 input bundle 的 `callWith(...)` 内临时安装
  `WindowTaskContextHolder`；普通 WINDOW_FACT handler 线程没有合同保证 tracker 当前窗就是该命令的 exact HWND。
  同类风险已在 TeamReturn mount D21 中由父级以相同源码证据判定并修为 handler-bound
  `BoundWindowCaptureService` capability。
- **影响：**双窗口并发时可能捕获/匹配 A 窗，却把结果减去 B 窗 base 后发布为 B 的
  `WINDOW_CLIENT_PX`；三道 fence 只能证明 B 的 binding 未变，不能证明像素来自 B，最终可在错误窗口点击“给予”。
- **精确返修条件：**追加 `Design Repair #1 Delta`，删除 fixed provider 对 tracker/
  `CoordinateHelper.findImageAbsoluteCoordinate*` 的 capture/base 权威。由 handler 在已取得 exact `BindingAccess` 后，使用
  `BoundWindowCaptureService` 对该 binding 做一次 capture，并在交付/发布前后复验 registration、runRevision、完整 binding 与
  geometry；provider 只消费该次不可持久化的 exact frame/capture-scale snapshot，执行既有模板、0.85、模板中心和唯一一次
  `(20,8)` 随机化。必须明确 physical-pixel match 坐标如何按同一次稳定 `systemScaleRatio` 还原为
  `WINDOW_CLIENT_PX`，capture/scale/binding 不稳定返回 typed NOT_EXECUTED/UNKNOWN，不能伪装 NOT_FOUND；frame 的 flush owner
  与所有退出分支必须唯一。更新 W-GIVE-F0 精确文件/方法表和 runtime 证据，禁止新增第二 capture、额外 verify 或业务 retry。

### P2-1：RX3 依赖状态已过期

- **证据：**Design #1 `:289` 仍写 “AB D2 未 Parent Approved”；真实权威日志已在本稿交付前写入
  `Parent Design Review #2 - DESIGN APPROVED / Implementation Published`，AB 当前正在实施 `W-TTR-RX3-IMP1`。
- **影响：**不会直接改业务，但会让 Give 的 DAG、可实施时点和共享 handler 写集排序失真。
- **精确返修条件：**Repair 只更新状态与顺序：RX3 **design 已批准、implementation 在途**；Give 仍不得抢写 AB 的
  remote/handler/registry/input-worker 写集，必须等待 AB implementation + 双构建父级通过后重新锚定。不要重写已通过的
  Give 业务合同，也不要因此提前批准 W-GIVE-F0/C0。

原 Worker AC 仅追加上述 Delta；两仓 Java/Maven/schema/resources/tests/host/caller 继续冻结。Worker self-QA 不算批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED - W-GIVE-D2 - 2026-07-13T16:20:25-04:00（真实物理 EOF 权威副本）

- task：`W-GIVE-D2`，继续原 `W-GIVE-D1`，仅关闭 `Parent Design Review #1 - BLOCKED` 的 P1/P2。
- claimedAt：`2026-07-13T16:20:25-04:00`。
- uniqueWriteSet：仅 `docs/superpowers/plans/reports/2026-07-13-cloud-give-item-service-worker-ac.md`，append-only。
- 说明：此前同名认领因补丁锚点重名误落在文件前部；为遵守 append-only 不删除、不搬移。以本段真实物理 EOF 副本为唯一权威认领。

## Design Repair #1 Delta - W-GIVE-D2 - 2026-07-13T16:20:25-04:00

本 Delta 只替换 Design #1 中 `W-GIVE-F0` 的 fixed-button capture/fact 技术设计，并校正 RX3 状态与排期。Design #1 已通过的 normal/exclusive、queue-in-queue、防重/`UNKNOWN`、BagService、顺序/阈值/sleep/失败/重试/fallback 等业务合同全部保持不变；不预批准 `W-GIVE-F0`、`W-GIVE-C0` 或任何实现。

### P1 closure：fixed button fact 的精确绑定、单次 capture、比例与 flush

#### 1. 唯一窗口与 capture 权威

- `LocalRemoteGameCommandHandler` 的当前命令 `BindingAccess` 是 fixed-button 事实的唯一窗口、HWND、registration/revision、geometry 和 base 权威。删除原设计中 `CoordinateHelper`、`GameClientTracker`、tracker capture/path/base 对该事实的任何权威或参与。
- handler fixed 分支调用私有执行边界（建议名 `executeGiveItemButtonMatchFact(...)`）；进入时以当前命令的 exact `BindingAccess` 做前置校验：registration/revision 仍为当前值，runner/context 身份一致，binding 的 `nativeHandle/processId/title/className/x/y/width/height` 全量一致且 geometry 有效。
- capture 前读取一次 `scaleBefore = readSystemScaleRatioNow()`；无效或 capture 前 deadline 已过时返回 typed `NOT_EXECUTED`，且 capture 调用次数为 0。
- 全流程只调用一次现有 `BoundWindowCaptureService.captureRegion(...)`：传入 `access.binding()`，窗口 base 使用同一 binding 的 `x/y`，区域为该 binding 的完整 client 范围。不得调用 tracker 刷新、latest vision path、临时截图路径、第二次 capture 或额外业务验证。
- `captureRegion(...)` 无返回帧时为 typed `NOT_EXECUTED`。取得帧后立即读取 `scaleAfter`；无效或与 `scaleBefore` 的 double bits 不同，归类 typed `UNKNOWN`，不得伪装为 `NOT_FOUND`。
- 发布 outcome 前重新读取 registration 与 exact `BindingAccess`，复核 revision、runner/context、完整 binding 与 geometry。帧已产生后的 deadline、registration/revision/binding/geometry 漂移一律为 typed `UNKNOWN`；不得发布该帧的 `NOT_FOUND` 或坐标。

#### 2. provider 的纯事实边界与坐标单位

- `LocalGiveItemButtonFactProvider` 只接收非 owning 的单帧 `BufferedImage` 与已稳定的 `systemScaleRatio` snapshot；不得注入或调用 `CoordinateHelper`、`GameClientTracker`、`BoundWindowCaptureService`，不得读取 HWND/base/path，也不得自行 capture。
- provider 只加载既有已批准按钮模板，并只执行一次 `ImageFinder.find(frame, template, 0.85)`。稳定 frame/scale/binding 下无匹配才是 `OBSERVED/NOT_FOUND`；模板加载或 matcher 异常是 `UNKNOWN/INTERNAL_ERROR`，不是 `NOT_FOUND`。
- `ImageFinder` 返回的中心点 `(physicalX, physicalY)` 是 capture 图像内 physical px。必须先用同一稳定 snapshot 转成 `WINDOW_CLIENT_PX`：`baseClientX = round(physicalX / systemScaleRatio)`、`baseClientY = round(physicalY / systemScaleRatio)`；不得加减 screen/window base。
- 转换后且仅一次应用原业务随机量：`clientX = baseClientX + (random.nextInt(41) - 20)`，`clientY = baseClientY + (random.nextInt(17) - 8)`。不得 clamp、再次随机、再次匹配、再次 capture、额外 verify 或新增 retry。成功 fact 明确携带 `coordinateSpace=WINDOW_CLIENT_PX`。

#### 3. 唯一 flush owner

- `BoundWindowCaptureService.captureRegion(...)` 继续拥有并 flush 其内部 full-window/candidate 生命周期；其返回的 copied region frame 只发生一次 ownership transfer，交给 handler。
- handler 是返回 frame 的唯一 flush owner：从 frame 取得成功开始，以一个外层 `try/finally { frame.flush(); }` 覆盖比例复核、provider、后置 binding fence、outcome 构造及异常出口。provider 不得 flush handler frame，outcome 不得持有 frame。
- provider 独自拥有模板 `BufferedImage`，在自己的 `finally` 中 flush；`ImageFinder` 继续负责释放自身 OpenCV Mats。禁止 double flush、frame escape 或跨请求缓存。

#### 4. fixed-button 退出矩阵

| 出口 | capture 次数 | typed 结果 | frame flush owner |
|---|---:|---|---|
| capture 前 deadline、binding/registration/revision/geometry 或 scale 无效 | 0 | `NOT_EXECUTED` | 无 handler frame |
| 唯一 capture 返回空 | 1 | `NOT_EXECUTED` | service 处理内部帧；无 handler frame |
| frame 后 scale 无效/变化 | 1 | `UNKNOWN` | handler 恰好一次 |
| frame 后模板/provider/matcher 异常 | 1 | `UNKNOWN/INTERNAL_ERROR` | handler 恰好一次；provider flush 模板 |
| frame 后 deadline 或 registration/revision/完整 binding/geometry 漂移 | 1 | `UNKNOWN` | handler 恰好一次 |
| 稳定 frame/scale/binding 且未匹配 | 1 | `OBSERVED/NOT_FOUND` | handler 恰好一次 |
| 稳定匹配并完成 physical px 到 `WINDOW_CLIENT_PX` 转换及一次随机 | 1 | `OBSERVED/MATCHED` | handler 恰好一次 |
| frame 后 outcome 构造或顶层异常 | 1 | 既有 handler error 映射的 `UNKNOWN` | handler 外层 finally 恰好一次 |

以上所有出口均禁止第二次 capture、额外业务读取、verify、retry 或 fallback 改序。

#### 5. `W-GIVE-F0` 精确文件/方法表 Delta

| 仓库 | 文件 | 未来动作 | 本 Delta 后唯一职责 |
|---|---|---|---|
| Cloud | `src/main/java/com/bot/dhxy/cloud/remote/WindowFactKind.java` | Modify | 增加 fixed-button 事实 kind；业务合同不变。 |
| Cloud | `src/main/java/com/bot/dhxy/cloud/remote/WindowFact.java` | Modify | 承载 match 与 `WINDOW_CLIENT_PX` 坐标。 |
| Cloud | `src/main/java/com/bot/dhxy/cloud/remote/WindowFactOutcome.java` | Modify | 承载 typed observed/not-executed/unknown 结果。 |
| Cloud | `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandOutcomeEnvelope.java` | Modify | 传输事实 outcome；不改变 command 判定。 |
| DHXY | `src/main/java/com/bot/dhxy/remote/RemoteGiveItemButtonMatchFact.java` | New | immutable fact model，坐标空间固定为 `WINDOW_CLIENT_PX`。 |
| DHXY | `src/main/java/com/bot/dhxy/remote/LocalGiveItemButtonFactProvider.java` | New | 仅对 handler 提供的单帧和稳定 scale 做阈值 `0.85` 匹配、中心换算与一次 `(20,8)` 随机；无 capture/tracker/base 权威。 |
| DHXY | `src/main/java/com/bot/dhxy/remote/LocalRemoteGameCommandHandler.java` | Modify | 用 exact `BindingAccess`、已有 `BoundWindowCaptureService` 单次 capture、scale bracket、前后 fence、typed exit 与唯一 frame flush owner。 |
| DHXY | `src/main/java/com/bot/dhxy/util/CoordinateHelper.java` | 0 Modify | 明确不参与 fixed-button fact。 |
| DHXY | `src/main/java/com/bot/dhxy/core/GameClientTracker.java` | 0 Modify | 明确不参与 fixed-button capture/path/base。 |
| DHXY | `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java` | 0 Modify | 复用现有 `captureRegion(...)` 及内部 ownership，不改实现。 |
| 两仓 | Maven/schema/resources/tests/host/caller | 0 Modify | 本任务继续冻结；模板资源不改。 |

#### 6. F0 runtime gate 增补（仅定义，不执行）

- 双窗口并发证据必须把 command window、exact HWND/binding、capture provider、稳定 scale、physical match center、换算后的 `WINDOW_CLIENT_PX` 关联到同一 request；每 request 只能出现一次 capture。
- 两个窗口 origin 不同时，不得出现 tracker refresh/latest-path/base 参与 fixed fact；同类按钮的 client 坐标应独立于 screen origin，最终点击仍由当次绑定解释。
- 人为制造 frame 后 scale 或 binding/revision/geometry 漂移时必须得到 `UNKNOWN`、不发布 `NOT_FOUND`/坐标且不点击；无第二次 capture、retry 或额外 verify。
- 连续运行证据需确认 handler 返回帧、provider 模板及 OpenCV Mats 的 owner 各自唯一，无 frame 累积或跨请求逃逸。

### P2 closure：RX3 最新状态与共享写集 fence

- 最新父级事实：`W-TTR-RX3-D2` 已于 `2026-07-13T16:05:00-04:00` 获得 `DESIGN APPROVED`，`P0=0/P1=0/P2=0`；当前为 `W-TTR-RX3-IMP1` implementation 在途。Design #1 中“D2 未批准”的旧状态作废，仅校正状态与排序。
- Give worker 在 AB 实现完成、Cloud `mvn -q clean package` 与 DHXY `mvn -q -DskipTests compile` 双构建证据齐全、且父级明确给出 source/build pass 前，不得进入或改动任何 AB 共享写集。AB worker 自测、单仓 build 或本 worker self-QA 均不能解除 fence。
- Cloud 共享冻结集：`CloudTaskExclusiveInteractionAuthority`、`CloudTaskExclusiveInteractionState`、`CloudTaskRetainedActionState`、`TaskTransactionAction`、task-run control request/outcome、`RemoteOperation`、`RemoteRequest`、`RemoteOutcome`、`RemoteGameClientPort`、`CloudTaskRunCommandExecutor`、`CloudTaskRunExecutionGate`、broker/outcome envelope/digests 及 protocol schema。
- DHXY 共享冻结集：`RemoteTaskRunRegistry`、`LocalRemoteGameCommandHandler`、`RemoteGameOperation`、task-run control payload/codec/digests、`RemoteOperationLedger`、`InputActionRequest`、`InputActionQueue`、`InputActionWorker` 及 protocol schema。
- fence 解除后必须先重读 AB 最终源码与双构建结果，再由父级重新锚定 Give 的精确文件表；不得沿用本设计时点的旧签名直接实施。

更新后的依赖 DAG 仅调整状态/门禁，不改已通过业务合同：

`RX3 Design Approved` -> `W-TTR-RX3-IMP1 implementation 在途` -> `AB Cloud+DHXY 双构建` -> `父级 source/build pass` -> `重读并重新锚定共享文件` -> `父级排定 TeamReturn mount` -> `W-GIVE-F0` -> `BagService/RX3 依赖满足后的后续 Give 实现`。

`W-GIVE-F0` 与 `W-GIVE-C0` 仍为未批准未来任务；本 Delta 不构成实现授权。

### Worker AC self-QA（不构成批准）

- P1：fixed fact 已删除 `CoordinateHelper`/tracker capture/path/base 权威，收敛到 handler exact `BindingAccess` + `BoundWindowCaptureService` 单次 capture；稳定 scale 转 `WINDOW_CLIENT_PX`、唯一 flush owner 与全退出矩阵已明确。
- P2：RX3 已校正为 design approved、implementation 在途；共享写集冻结至 AB 双构建且父级 source/build pass。
- 未重写已通过业务合同；未修改 Java/Maven/schema/resources/tests/host/caller；未运行 Maven/runtime；未执行 Git mutation。

Worker AC 到此停手，等待父级复审。

## Parent Design Review #2 - BLOCKED / `W-GIVE-D3` Published - 2026-07-13T16:26:00-04:00

父级复审 Repair #1。exact `BindingAccess` + 单次 `BoundWindowCaptureService`、同帧 scale bracket、physical px 转
`WINDOW_CLIENT_PX`、唯一 frame/template flush owner、frame 后不稳定映射 UNKNOWN 与 RX3 在途 fence 已关闭上一轮
P1/P2；但“精确文件表”仍不能形成双仓闭合协议，当前 **BLOCKED，P0=0/P1=1/P2=1**，Java 继续冻结。

### P1-1：W-GIVE-F0 文件表只改 Cloud kind，遗漏 DHXY 对称 enum/strict decode 与 Cloud closed fact parser

- **证据：**当前 Cloud `WindowFactKind` 与 DHXY `RemoteWindowFactKind` 都只有
  `BINDING/GEOMETRY/FOCUS_STATE/STOP_STATE`；DHXY handler `executeWindowFact` 对本地 enum 做 exhaustive switch；Cloud
  `WindowFact` 是 sealed permits，`WindowFactOutcome.matches(...)` 与 `RemoteCommandOutcomeEnvelope.parseFact(...)` 也按 kind
  exhaustive 解析。Repair 表只列 Cloud `WindowFactKind/WindowFact/WindowFactOutcome/Envelope`，没有列 DHXY
  `cloud/remote/RemoteWindowFactKind`、closed fact payload/codec/schema 对称修改，也未写 `matches/parseFact` 的 exact 分支。
- **影响：**Cloud 可以构造的新 kind 在 DHXY strict enum decode 直接失败，或 outcome 无法解析为 closed fact；单仓编译通过也不能
  证明 wire 可用，实施者仍需临场扩大写集。
- **返修条件：**补齐两仓逐字段同构表：Cloud kind/sealed fact/matches/envelope parser/必要 digest-schema，DHXY
  `RemoteWindowFactKind`、正确 package 下的 immutable fact、handler exhaustive branch、payload codec/strict schema/必要 digest；逐项标明
  哪些文件 0 Modify 及其源码理由。明确 request/outcome allowed keys、MATCHED/NOT_FOUND typed shape 和 outcome digest 字段，禁止用
  JsonNode/raw map 逃逸 closed contract。

### P2-1：DHXY 文件路径写错且方法锚点不精确

表内写成 `src/main/java/com/bot/dhxy/remote/...`，实际 remote authority/package 是
`src/main/java/com/bot/dhxy/cloud/remote/...`；`LocalRemoteGameCommandHandler` 也在该 package。Repair 必须用真实绝对/仓内路径、
真实类名和拟改方法名重列，避免后续在错误 package 新造第二套 DTO。

### 当前任务 `W-GIVE-D3`

原 Internal Worker AC 仅追加 `Design Repair #2 Delta`，唯一写集仍仅本日志；不得修改 Java/Maven/schema/resources/tests/
host/caller。只关闭以上 protocol/file-table 缺口，不重写已通过的 Give 业务与 exact-capture 结论。Worker self-QA 不算批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## RESUMED/CLAIMED - W-GIVE-D3 - 2026-07-13T16:52:55-04:00（真实物理 EOF 权威副本）

- task：`W-GIVE-D3`，继续原 Internal Worker AC，只关闭 Parent Design Review #2 的 P1/P2。
- claimedAt：`2026-07-13T16:52:55-04:00`。
- uniqueWriteSet：仅本固定日志，真实 EOF append-only。
- append-only 说明：`16:48:49` 的同名块因重复锚点误落历史前部；不删除、不搬移，以本真实物理 EOF 块为唯一权威交付。
- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、本日志 Parent Review #2 与两仓 git status。DHXY `thin-client-design`、Cloud `navigation-migration` 的全部 dirty/untracked 均保持原样。
- Java/Maven/schema/resources/tests/host/caller 全冻结；不运行 Maven/runtime，不执行 Git mutation。

## Design Repair #2 Delta - W-GIVE-D3 - 2026-07-13T16:52:55-04:00（真实物理 EOF 权威副本）

本 Delta 只闭合未来 `W-GIVE-F0` 的 fixed Give button `WINDOW_FACT` 双仓合同与文件表。Repair #1 已通过的 exact `BindingAccess`、单次 `BoundWindowCaptureService` capture、scale bracket、physical px 到 `WINDOW_CLIENT_PX`、唯一 flush owner、frame 后不稳定为 `UNKNOWN` 均保持；Give 的顺序、阈值、sleep、失败/retry/fallback 不重写。RX3 仍是 design approved、`W-TTR-RX3-IMP1` implementation in flight，以下未来写集继续冻结到 AB 双构建且父级 source/build pass。

### A. 真实 package 与单一 schema

- Cloud：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\`。
- DHXY：`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\`。
- schema：`D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md`；Cloud 仓没有第二份 schema，不新建镜像。
- 旧表的 `com/bot/dhxy/remote` 路径及把 Cloud 写成 `com/bot/dhxy/cloud/remote` 均作废，禁止在错误 package 新造 DTO。

### B. fixed Give button closed wire

两仓 enum wire 名固定为 `GIVE_ITEM_BUTTON_MATCH`。

**Request：**Cloud typed object 仍为 `WindowFactRequest {context,factKind}`；扁平 command payload 的 `allowed=required={factKind}`，值只能是 `GIVE_ITEM_BUTTON_MATCH`。不得加入 template、threshold、capture region、scale、base、坐标、随机量、score 或 retry key。`requestDigest` 仍覆盖去掉 `context.requestDigest` 的 typed context 与该 factKind，无新 default/optional key。

**Outcome payload：**所有 execution state 下 `allowed=required={factKind,fact}`；`factKind` 保留 `GIVE_ITEM_BUTTON_MATCH`，`fact` key 不得缺失。OBSERVED 的 immutable fact 两仓逐字段同构：

```text
GiveItemButtonMatchFact {
  matchStatus: MATCHED | NOT_FOUND,
  coordinateSpace: WINDOW_CLIENT_PX | null,
  x: integer | null,
  y: integer | null,
  observedWindow: {
    windowId: non-blank string,
    nativeHandle: normalized unsigned-decimal string,
    processId: positive long,
    playerIdentityEpoch: non-negative long
  }
}
```

fact 必须恰有 `{matchStatus,coordinateSpace,x,y,observedWindow}` 五个 key；observedWindow 必须恰有上述四个 key。禁止 extension key、JsonNode/raw map、match score、template、physical point、screen base、scale 或诊断字段。

| 状态 | closed shape |
|---|---|
| `OBSERVED/OK + MATCHED` | fact non-null；`coordinateSpace=WINDOW_CLIENT_PX`；x/y 为 integer；observedWindow non-null。点只来自 Repair #1 同帧稳定 scale 换算与唯一一次 `(20,8)` 随机，不 clamp。 |
| `OBSERVED/OK + NOT_FOUND` | fact non-null；coordinateSpace/x/y 三个 key 均显式 null；observedWindow non-null。仅表示稳定 exact frame/scale/binding 下 0.85 未命中。 |
| `NOT_EXECUTED` | fact key 显式 null；不得携带 stale fact。 |
| `UNKNOWN` | fact key 显式 null；不得携带点或伪装 NOT_FOUND。 |
| `STOPPED` | fact key 显式 null，沿用既有 non-OBSERVED 规则。 |
| `EXECUTED` | 对 WINDOW_FACT 非法。 |

JsonNode 只可存在于 `RemoteGameCommand`/`RemoteGameOutcomeEnvelope` 的瞬时 wire 边界。DHXY 必须经 operation codec exact decode/encode，Cloud 必须经 `windowFactOutcome()/parseFact()` exact decode后才形成 immutable fact；provider、handler事实、Cloud Service与retained state不得接收 raw tree/map。

### C. digest 精确闭包

`outcomeDigest` 是 typed `WindowFactOutcome` 删除且只删除 `common.outcomeDigest` 后的 RFC 8785/JCS SHA-256，不直接 hash 外层 transport envelope。它覆盖：

```text
common {
  contractVersion, operation, requestId, actionId, taskRunId,
  requestDigest, executionState, code,
  message(仅 non-null), acceptedAtEpochMs, finishedAtEpochMs
}
factKind
fact(仅 typed fact 非 null)
```

- MATCHED 额外覆盖 `matchStatus,coordinateSpace,x,y` 和 observedWindow 四字段。
- NOT_FOUND 的 wire null keys仍必须存在；Cloud 当前 NON_NULL typed canonical tree只覆盖 `matchStatus,observedWindow`，省略 coordinateSpace/x/y 三个 null member。
- non-OBSERVED wire 的 fact 必须显式 null；typed canonical tree省略 fact，但始终覆盖 factKind。
- frame/image、physical point、template、threshold、systemScaleRatio 不属于该 fact/digest；请求的 window/revision/stop/semanticAddress 已由 requestDigest 绑定。
- Cloud `RemoteProtocolDigests` 是 `0 Modify`：现有 `computeRequestDigest/computeOutcomeDigest` 从 typed tree生成，`withComputed*` 只按 WindowFactRequest/Outcome 类型分支，新 sealed fact自动入树。
- DHXY `RemoteProtocolDigests.computeOutcomeDigest(...)` 必须 Modify：当前会合并 raw payload，NOT_FOUND nested explicit null会与 Cloud NON_NULL bytes分叉；未来先用 `RemoteOperationPayloadCodec.readWindowFactOutcome(...)` strict decode，再以同一 NON_NULL mapper重建 factKind/fact后 canonicalize。RFC 8785、SHA-256、其它 operation 与 imageBytes 规则不改。

### D. Cloud 未来精确文件/方法表（当前不写）

| 动作 | 真实文件 | 方法/类型锚点与职责 |
|---|---|---|
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFactKind.java` | `WindowFactKind` 增 `GIVE_ITEM_BUTTON_MATCH`。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFact.java` | sealed permits 墏 nested immutable `GiveItemButtonMatchFact`/`MatchStatus`，constructor 强制五字段和 MATCHED/NOT_FOUND matrix。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\WindowFactOutcome.java` | `matches(...)` exhaustive 新 case：kind 只匹配 `WindowFact.GiveItemButtonMatchFact`；既有 state/null gate 不变。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandOutcomeEnvelope.java` | `windowFactOutcome(...)` exact outer keys/state-null matrix；`parseFact(...)` exhaustive 新 kind；每 kind exact nested keys，新 fact再验五字段与 observedWindow 四字段后 strict parse。无 default/raw fallback。 |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteGameCommandBroker.java` | `validateAgainstPending(...)`：OBSERVED 新 fact 的 observedWindow 必须与 pending `context.window` 四字段相等；既有 factKind correlation保留。 |
| Future Modify | `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md` | `§6 WINDOW_FACT`/`§8 outcomeDigest` 登记 enum、exact keys、两张 matrix及 canonical字段；D3 当前不写。 |

### E. DHXY 未来精确文件/方法表（当前不写）

| 动作 | 真实文件 | 方法/类型锚点与职责 |
|---|---|---|
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactKind.java` | 增同名 `GIVE_ITEM_BUTTON_MATCH`，供 strict enum decode。 |
| New | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactPayload.java` | sealed payload union，permits 既有四 fact 与新 Give fact，消除 outcome 内 raw fact。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteBindingFact.java`、`RemoteGeometryFact.java`、`RemoteFocusFact.java`、`RemoteStopFact.java` | class declaration仅 implements `RemoteWindowFactPayload`；字段/行为不改。 |
| New | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGiveItemButtonMatchFact.java` | immutable Lombok value/builder + nested MatchStatus；五字段与 Cloud逐名同构，construction path强制 matrix。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactOutcomePayload.java` | `JsonNode fact` 改 nullable `RemoteWindowFactPayload fact`；wire tree只由 codec产生。 |
| New | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalGiveItemButtonFactProvider.java` | `match(BufferedImage,double)` 最终签名待父级重锚；只返回 immutable MATCHED/NOT_FOUND，继续完全服从 Repair #1。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java` | `executeWindowFact(...)` exhaustive 新 branch；新 `executeGiveItemButtonMatchFact(...)` 持有 exact capture/flush；`terminal(...)` 的 WINDOW_FACT走closed codec；`emptyOutcomePayload(...)` 保留 kind/null fact。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteOperationPayloadCodec.java` | `readWindowFact(...)` 保证 request exact `{factKind}`；新 `readWindowFactOutcome(...)`/`toWindowFactOutcomeTree(...)` 保证 outer/fact/observedWindow exact keys、kind/type/state matrix；未知值 fail closed。 |
| Modify | `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteProtocolDigests.java` | `computeOutcomeDigest(...)` 对 WINDOW_FACT strict typed normalization后hash，确保双仓 byte-for-byte；request digest/canonicalizer不改。 |

### F. `0 Modify` 项与源码理由

| 文件 | 源码理由 |
|---|---|
| Cloud `RemoteRequest.java`、`WindowFactRequest.java` | sealed request已包含 WindowFactRequest；constructor generic接收非 null kind。新增的是 kind，不是 request/operation。 |
| Cloud `RemoteCommandEnvelope.java` | `from(RemoteRequest)`/`payload(RemoteRequest)` 已把任何 WindowFactRequest编码为唯一 `factKind().name()`。 |
| Cloud `CloudTaskRunExecutionGate.java` | `newWindowFactRequest(...)`/`newPausedObservationWindowFactRequest(...)` 已 generic build、digest、retained bind；Give不新增 builder字段或 paused业务。 |
| Cloud `RemoteGameClientPort.java`、`CloudTaskRunCommandExecutor.java` | `readWindowFact(...)` 已 generic kind request/retained redelivery/outcome path，不新造 Give port。 |
| Cloud `RemoteProtocolDigests.java` | typed tree与 WindowFactOutcome级分支已足够，无 fact-kind switch。 |
| Cloud `RemoteOperation.java`、`RemoteOutcome.java` | 继续复用 WINDOW_FACT/WindowFactOutcome，禁止业务 operation旁路。 |
| Cloud `CoordinateSpace.java`、`ObservedWindowBinding.java` | 已提供 WINDOW_CLIENT_PX 和 exact四字段 binding。 |
| DHXY `RemoteWindowFactCommandPayload.java` | 已是 immutable `{factKind}`，新增 enum 后由现有 request codec strict decode。 |
| DHXY `RemoteGameOperation.java` | 继续复用 WINDOW_FACT。 |
| DHXY `RemoteGameCommand.java`、`RemoteGameOutcomeEnvelope.java` | 顶层 wire字段不变；JsonNode只留在 envelope边界，不作为 fact API。 |
| DHXY `RemoteCoordinateSpace.java`、`RemoteObservedWindowBinding.java` | 已与 Cloud wire同构，直接复用。 |
| DHXY `HttpRemoteCommandTransport.java` | 顶层 poll/outcome、duplicate detection和 digest验证不变；operation payload闭合由 codec/Cloud parser负责。 |
| DHXY `BoundWindowCaptureService.java`、`CoordinateHelper.java`、`GameClientTracker.java` | 前者只复用现有单次 capture；后两者按 Repair #1 不参与，均不改。 |
| DHXY input action/queue/worker、两仓 host/caller/Task | F0 只产事实；后续 retained click路径与 RX3 fence不在本波改。 |
| Maven/resources/tests | 无新 dependency；模板、0.85、测试策略不变。 |

### G. 原子 cohort 与 RX3 fence

未来 F0 必须把 Cloud enum/sealed fact/matches/parser/broker、DHXY enum/typed fact/handler/codec/digest和唯一 schema作为同一双仓 closed cohort；禁止单边 enum、unknown-field 容忍、placeholder JsonNode adapter或 mixed-version兼容。实施顺序仍为：

`W-TTR-RX3-IMP1 in flight -> AB Cloud+DHXY 双构建 -> 父级 source/build pass -> 重读/重锚共享源码 -> 父级另行批准 W-GIVE-F0 -> 双仓原子实施`。

本 Delta 不解除 shared remote/handler/registry/input-worker/digest/schema fence，不预批准 F0/C0，不授权 host/caller/runtime。

### Worker AC self-QA（不构成父级批准）

- P1：enum/sealed/immutable fact、matches/parser、request builder、handler、codec、必要 digest/schema、exact keys、MATCHED/NOT_FOUND 与 execution-state matrix 已闭合；无 JsonNode/raw map事实逃逸。
- P2：Cloud 与 DHXY 真实路径已分开列明；DHXY 新/改 remote 文件全部位于 `com/bot/dhxy/cloud/remote`，Modify/New/0 Modify均有方法或源码理由。
- exact capture/业务结论与 RX3 implementation-in-flight fence保持不变。
- 本轮只追加本日志；未修改 Java/Maven/schema/resources/tests/host/caller，未运行 Maven/runtime，未执行 Git mutation。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

Internal Worker AC 到此停手，等待父级复审；本 self-QA 不是 review/approval。

### Append-only Erratum - 2026-07-13T16:52:55-04:00

- 上述 Cloud 文件表 `WindowFact.java` 行中的“sealed permits 墏 nested”是排版字符错误，权威读法为“sealed permits **增加** nested immutable `GiveItemButtonMatchFact`/`MatchStatus`”；动作、文件、字段和方法边界均不变。

Internal Worker AC 到此停手，等待父级复审；本 self-QA 不是 review/approval。
