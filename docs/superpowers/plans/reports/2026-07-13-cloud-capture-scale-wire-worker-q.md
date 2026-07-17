# Cloud Capture-Time Scale Wire - Worker Q

## Parent Task Brief #1 - 2026-07-13T02:00:00-04:00

### 目标

为 AutoCombatPanel 与 SummonSkill 主体共同缺失的 capture-time typed `systemScaleRatio` 形成可编译、跨两仓同构的协议设计。
该比例必须由 DHXY 本地 capture owner 在产生图像的同一事实边界读取并随 capture outcome 返回；Cloud 不得自行探测、缓存旧值、
从坐标反推或默认 `1.0`。

### Worker Q 唯一任务

先向本日志追加 `## Internal Worker Q - CLAIMED - <timestamp>`，再追加 `Internal Worker Q - Design #1`。父级明确
`DESIGN APPROVED` 前零 Java。完整读取：

- `D:/mavenProject/DHXY/AGENTS.md`、`docs/DHXY_CONTEXT.md`、thin-client protocol schema；
- 双仓全部 capture request/outcome/envelope/digest/Jackson DTO；
- DHXY capture handler、window registration/geometry/scale 的真实读取路径；
- Cloud broker/port 对 capture outcome 的 accepted/late/UNKNOWN/STOPPED 验证路径。

Design #1 必须给出：

1. 当前 capture 从 Cloud request 到 DHXY 副作用/截图、outcome 回传、Cloud typed port 返回的完整调用链与精确 FQCN/方法；
2. `systemScaleRatio` 的生产时刻、数值域、double canonical JSON/digest 表示、同帧/同 binding 证明与无可用值时的 typed
   fail-closed；不得把缺失值变成 `1.0` 或业务 miss；
3. Cloud/DHXY wire 字段顺序、strict schema、outcome digest/transport 重建的同构修改点；普通 request digest 与非 capture
   operation 必须零变化；
4. exact scope/taskRun/window/stopEpoch/runRevision 三道 fence、stale/乱序/重投/UNKNOWN/STOPPED/NOT_EXECUTED 行为；稳定
   request/capture identity 继续归 retained ledger，不在本切片重铸；
5. 精确 New/Modify 文件表、兼容/切换顺序、no-local-test 与双构建门。优先划出最小可编码波次，不接 host/caller/Service
   主体，不修改 P lifecycle adapter、A TMS、B Panel 或 N SummonSkill policy。

### 约束

- 仅追加本日志；父级批准前不改 Java/schema/resources/tests。
- 不启动 application/server/host/Task/poller/UI/capture/input，不发送任何命令。
- 不新增 TTL/takeover/retry/额外 capture；不改变业务坐标/ROI/template/click 顺序。
- 不回滚/覆盖/提交任何 dirty/untracked；Worker 自审仅 QA，不构成批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Q - CLAIMED - 2026-07-13T02:01:16-04:00

## Internal Worker Q - Design #1 - 2026-07-13T02:22:03-04:00

本节仅为 Parent Task Brief #1 的设计材料；未修改 Java/schema/resources/tests，未运行 Maven，未启动任何
application/server/host/Task/poller/UI/capture/input，也未执行 git mutation。全部既有 dirty/untracked 均按原样保护。

### 0. 只读基线与当前可达性事实

- DHXY 当前分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`，无 upstream；可见最新
  remote 基线 `origin/dev=e543d024bf900853944b36d27d0f736005d9eeb9`。本切片候选 Java 均已是他人留下的
  untracked，schema 已 dirty；本轮没有覆盖或整理它们。
- Cloud 当前分支 `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`，无 upstream；可见最新
  remote 基线 `origin/main=48e37813913094cacd1143fcae02704033eecb93`。本切片候选 remote Java 均为既有
  untracked；本轮只读。
- `origin/dev` 与当前 DHXY 都证明现有业务 scale 真值路径是
  `com.bot.dhxy.tools.CoordinateHelper#initScaleRatio()`：
  `GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
  .getDefaultTransform().getScaleX()`。其异常分支把缓存写成 `1.0`；因此
  `CoordinateHelper#getScaleRatio()` 无法区分“真实 1.0”和“探测失败后的 1.0”，不能直接进入新 wire。
- `WindowRegistrationRequest -> WindowRuntimeContext#setNativeBinding -> WindowNativeBinding` 只保存
  `nativeHandle/title/className/processId/x/y/width/height`；
  `WindowNativeBindingRefreshService#refreshAndCommit()` 只按 exact HWND 刷新上述 live state。当前注册/geometry 中没有
  scale owner。设计不把 scale 塞进 registration，也不缓存到 Cloud。
- 当前生产可达性仍为 **dormant**：`RemoteTaskRunRoutes#create()` 只创建 coordinator/broker 与 poll/outcome/lifecycle
  routes；当前 host 没有创建 `CloudTaskRunAuthorityAssembly`，也没有 Service/caller 能取得
  `CloudTaskServicePort.CaptureAction` 后发起 capture。下述是已落盘类型组成的完整潜在链路；本切片不接 host/caller，
  不把 dormant 变 reachable。

### 1. 当前 CAPTURE 完整调用链（精确 FQCN / 方法）

当未来受信 caller 已由独立切片持有 exact retained handle 时，链路按下列顺序同步返回；Q 不改变顺序：

1. Cloud Service-facing 入口：
   `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServicePort#capture(CaptureAction,
   CaptureRegion, CaptureRequest.ImageFormat, CaptureRequest.CapturePurpose, long)`；先经
   `CloudTaskRetainedActionState#invoke(...)` 锁住 current opaque handle/record。
2. package-internal port 实现：
   `CloudTaskRunCommandExecutor#capture(CloudTaskRunExecutionContext, CaptureRegion, ImageFormat,
   CapturePurpose, CloudTaskRunActionLedger.RetainedActionIdentity, long)`；调用
   `CloudTaskRunExecutionGate#validate(...)`、`CloudTaskRunActionLedger#prepareActiveInvocation(...)`。
3. 首次 attempt 由 `CloudTaskRunExecutionGate#newCaptureRequest(...)` 使用 retained
   `requestId/actionId/captureId` 构造 `CaptureRequest`，由
   `RemoteProtocolDigests#withComputedRequestDigest(CaptureRequest)` 计算 request digest，再由
   `CloudTaskRunActionLedger#bindOrVerifyRequest(...)` 绑定 immutable request；重投走原 bound request，随后
   `CloudTaskRunActionLedger#markActiveBrokerEntry(...)`。
4. `CloudTaskRunCommandExecutor#capture(...) -> RemoteGameCommandBroker#capture(...) ->
   #dispatchAndAwait(...)`。broker 入队门校验后保留 pending，并等待同一个 terminal future。
5. DHXY `HttpRemoteCommandTransport#poll(...)` POST 到 Cloud
   `RemoteTaskRunRoutes.PollEndpoint#handle(...) -> RemoteGameCommandBroker#poll(...)`；dispatch 持锁门成功后
   `RemoteCommandEnvelope#from(RemoteRequest)` 生成 flat command envelope，capture payload 仍为
   `captureId,region,imageFormat,capturePurpose`。
6. DHXY `RemoteCommandPollingLoop#runLoop()` 收到 COMMAND，调用
   `LocalRemoteGameCommandHandler#handle(RemoteGameCommand) -> #executeOwnedCommand(...) ->
   #executeCapture(...)`。`handle` 先做 request digest、strict request payload 与 local operation-ledger claim。
7. `executeOwnedCommand(...)` 在副作用前依次调用
   `#requireRegistration(...)`、`#requireBoundWindow(...)`、再次 `#requireRegistration(...)`；
   `requireBoundWindow` 通过 `WindowNativeBindingRefreshService#refreshAndCommit(WindowRuntimeContext)` 只刷新 request
   指定 HWND，禁止标题补位。
8. `executeCapture(...)` 用同一 `BindingAccess.binding` 计算 `#captureRectangle(...)`，再调用
   `com.bot.dhxy.driver.BoundWindowCaptureService#captureRegion(...) -> #captureWindow(...) ->
   #captureWithCompatibleBitmap(...)`，真实 provider 顺序保持 `PrintWindow(PW_RENDERFULLCONTENT)` 后
   `GetWindowDC + BitBlt`；不增加第二次截图，不改 ROI/坐标。
9. 图片经 `ImageIO.write(...,"png",...)` 编码，handler 做 timeout、post-capture
   `requireBoundWindow(command,true)` / `requireRegistration(...)` 及 exact geometry 比较，再构造
   `RemoteCaptureOutcomePayload`；`LocalRemoteGameCommandHandler#terminal(...)` 通过
   DHXY `RemoteProtocolDigests#computeOutcomeDigest(...)` 生成 terminal envelope并写入
   `RemoteOperationLedger#complete(...)`。
10. `RemoteCommandPollingLoop#validateOutcomeCorrelation(...)` 后，
    `HttpRemoteCommandTransport#submitOutcome(...)` POST 到 Cloud
    `RemoteTaskRunRoutes.OutcomeEndpoint#handle(...)`。
11. Cloud `RemoteCommandOutcomeEnvelope#toTypedOutcome()` 先重建 `CaptureOutcome`，再由
    `RemoteProtocolDigests#verifyOutcomeDigest(...)` 验 digest；`RemoteGameCommandBroker#completeOutcome(...)` 再做
    scope/session/pending/captureId/window correlation，进入 `#completeTerminalLocked(...)` 或
    `#acceptLateResolutionLocked(...)`。
12. `RemoteGameCommandBroker#dispatchAndAwait(...)` 返回 typed `CaptureOutcome`；
    `CloudTaskRunCommandExecutor#capture(...)` 只把该真实对象传给
    `CloudTaskRunActionLedger#recordOutcome(...)`，再经 `CloudTaskRetainedActionState#invoke(...)` 与
    `CloudTaskServicePort#capture(...)` 返回 Service。任何层都不解释图片或选择业务 fallback。

PAUSED read-only capture 仍复用相同 wire/handler/outcome：其 request builder 是
`CloudTaskRunExecutionGate#newPausedObservationCaptureRequest(...)`，identity 属
`CloudTaskRunActionLedger.ObservationActionIdentity`。当前没有 observer submit/caller 接线；Q 不补该接线，只保证未来同一
capture outcome schema 可被 ACTIVE 与 PAUSED_READ_ONLY 共用。

### 2. `systemScaleRatio` owner、时刻、数值域与 fail-closed

#### 2.1 唯一生产 owner 与采样边界

- 唯一生产点固定为 DHXY
  `com.bot.dhxy.cloud.remote.LocalRemoteGameCommandHandler#executeCapture(...)`，它就是 typed CAPTURE mechanical
  outcome owner；新增 private static `readSystemScaleRatioNow()`，不新增 public API/helper 层。
- `readSystemScaleRatioNow()` 每次都现场执行与 pushed `CoordinateHelper#initScaleRatio()` 相同的 AWT 真值读取：
  `defaultScreenDevice -> defaultConfiguration -> defaultTransform.getScaleX()`；**不读取**
  `CoordinateHelper.systemScaleRatio` 缓存，不读取 registration 旧值，不从 image/geometry/坐标反推，也不走任何 `1.0`
  fallback。
- 为把 scale 与实际 frame 放在同一事实边界，`executeCapture(...)` 顺序固定为：
  `pre binding/revision fence -> scaleBefore -> 一次现有 captureRegion -> scaleAfter -> PNG/hash -> post binding/revision
  fence -> 单一 OBSERVED outcome`。`scaleBefore/scaleAfter` 都必须可用且
  `Double.doubleToLongBits(scaleBefore)==Double.doubleToLongBits(scaleAfter)`；发布值为该相等值。
- 这不是额外 capture/业务 verify：只在同一个 handler invocation 内用同一系统事实源夹住原有唯一截图。无 TTL、缓存、
  retry、sleep、takeover 或额外图片。

#### 2.2 数值域

- Java/wire 类型：boxed `Double`（非 OBSERVED 时必须为 null）；成功域严格为
  `Double.isFinite(value) && value > 0.0d`，即有限 IEEE-754 binary64 正数
  `(0, Double.MAX_VALUE]`。
- `NaN`、正/负 infinity、`+0.0`、`-0.0`、缺失、JSON null、字符串/布尔 coercion 均不构成成功 scale。
  JSON number 的整数形式（例如 `1`）按 double `1.0` 解码并 canonical 为 `1`，但仍必须来自现场读取；不另加未经业务
  基线批准的 DPI 上下限，也不把合法的真实 `1.0` 排除。

#### 2.3 typed failure

- `scaleBefore` 不可读/非法：截图尚未开始，返回
  `NOT_EXECUTED/FACT_UNAVAILABLE`，captureId 保留，所有 observation 字段（含 scale）为 null。
- 截图之后 `scaleAfter` 不可读/非法或与 `scaleBefore` bitwise 不同：丢弃/flush 本地 image，返回
  `UNKNOWN/FACT_UNAVAILABLE`，所有 observation 字段为 null。因为截图已经发生，不能谎称 NOT_EXECUTED；Cloud 不得把它
  降级为 template/OCR miss，也不得换 ID 自动重拍。
- 原有 `captureRegion` 无图仍为 `NOT_EXECUTED/CAPTURE_FAILED`；已截图后 timeout 仍为
  `UNKNOWN/TIMEOUT`；post-binding 失败沿用当前 typed mapping。Q 不改变这些既有分支。
- Cloud `CaptureOutcome` 对 `EXECUTED` 继续非法；只有 `OBSERVED/OK` 才携带 image + scale，业务 CPU matcher 只能消费该分支。

#### 2.4 同帧 / 同 binding 证明链

一个可消费的 OBSERVED 结果同时具备：

1. 同一 `executeCapture` stack 内 bracket 的两次相等 scale 采样与唯一 frame；
2. capture 前 exact request window 四元组验证；
3. `BoundWindowCaptureService#captureRegion` 接收该同一个 immutable `WindowNativeBinding`，没有 title search；
4. capture 后 exact HWND/process/playerIdentityEpoch、registration/runRevision 与 geometry 再验证；
5. `observedWindow` 由 post-fence context/binding 生成；
6. Cloud `RemoteGameCommandBroker#validateAgainstPending(...)` 新增 OBSERVED capture 的
   `observedWindow == pending.context.window` 四字段精确比较；
7. `captureId + imageSha256 + width + height + provider + systemScaleRatio + observedWindow` 一起进入同一个
   `outcomeDigest`。图片 bytes 仍由 `imageSha256` 绑定。

因此 ratio 既不是 Cloud 环境的 scale，也不是跨帧缓存；binding/scale/frame 中任一无法闭合时都没有普通业务图片结果。

### 3. 双仓同构 wire、strict schema 与 digest

#### 3.1 request 零变化

`CaptureRequest` 与 command payload 顺序保持：

```text
CaptureRequest: context, captureId, region, imageFormat, capturePurpose
wire payload:   captureId, region, imageFormat, capturePurpose
```

`systemScaleRatio` 是本地执行时才产生的 observation，绝不进入 request，故所有普通 request bytes、`requestDigest`、retained
request identity 与 CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE 的构造路径零变化。

#### 3.2 outcome 精确字段顺序

Cloud typed record 固定顺序：

```text
CaptureOutcome:
  common,
  captureId,
  imageBytes,
  imageSha256,
  width,
  height,
  captureProvider,
  systemScaleRatio,
  observedWindow
```

DHXY `RemoteCaptureOutcomePayload` / flat envelope `payload` 固定顺序（没有 common）：

```text
captureId,
imageBytes,
imageSha256,
width,
height,
captureProvider,
systemScaleRatio,
observedWindow
```

outer `RemoteGameOutcomeEnvelope` / `RemoteCommandOutcomeEnvelope` 字段和顺序完全不变。

#### 3.3 capture payload strict schema

- Cloud `RemoteCommandOutcomeEnvelope#captureOutcome(CommonOutcome)` 在 Jackson `treeToValue` 前对 payload 做 capture-only
  closed-object 校验；allowed/required key **恰为上述八个**，缺 key、额外 key或错误类型均拒绝
  `INVALID_REQUEST`，不放宽 WINDOW_FACT/INPUT 的现状。
- `OBSERVED`：八个 key 均存在，`captureId` non-blank，另七个 observation 值全部 non-null；`systemScaleRatio` 必须是 JSON number，
  反序列化为有限正 `Double`。
- 非 `OBSERVED`：`captureId` 仍为 non-blank；其余七个 key 必须显式 null。Cloud broker 自建 terminal typed outcome 同样传
  null。显式 `systemScaleRatio:null` 不能进入 OBSERVED，缺失也不能伪装 `1.0`。
- DHXY 只从强类型 `RemoteCaptureOutcomePayload` 生成 payload，不接受 caller raw map；Cloud reconstruction 的 private
  `CapturePayload` 与 public `CaptureOutcome` 字段顺序/类型完全一致。
- 全局 gateway 已把 HTTP body 解析成 JsonNode 后才进入本类；Q 不修改当前 dirty
  `CloudApiGateway`，不借本切片改变其它 route 的 raw JSON 行为。capture payload 的 closed shape 在现有 typed boundary
  内完整执行。

#### 3.4 double canonical JSON

两仓 `RemoteProtocolDigests#appendCanonical(JsonNode,StringBuilder)` 在现有 integral 分支之后增加完全同构的 finite floating
分支，算法固定如下：

1. 非 finite 直接拒绝；`-0.0/+0.0` canonical token 为 `0`（成功 scale 本身已因 `>0` 排除 0）。
2. `BigDecimal.valueOf(double).stripTrailingZeros()` 取得 binary64 的 shortest round-trip decimal digits。
3. 令十进制指数 `e = precision - scale - 1`；`-6 <= e < 21` 用无指数 plain decimal；否则使用
   `firstDigit[.remainingDigits]e[+|-]exponent`，小写 `e`、正指数显式 `+`、指数无前导零。
4. 示例：真实 `1.0d -> 1`、`1.25d -> 1.25`、`1.5d -> 1.5`。两仓输出同一 UTF-8 preimage；对象 key 仍按现有
   Unicode/Java string comparator 排序。

这只把 protocol v1 的 canonical numeric subset 从 integral 扩到 finite binary64；既有 integral/string/boolean/object/array
分支逐字不动。当前所有 request 和非 capture outcome 都没有合法 floating 字段，因此其 digest byte-for-byte 不变。

OBSERVED capture digest preimage 仍为：

```text
SHA-256(UTF-8(JCS({
  common: common 去掉 outcomeDigest,
  captureId,
  imageSha256,
  width,
  height,
  captureProvider,
  systemScaleRatio,
  observedWindow
})))
```

`imageBytes` 继续唯一排除，null 继续不并入 digest。DHXY `computeOutcomeDigest(RemoteGameOutcomeEnvelope)` 从 payload 合并新
number；Cloud `computeOutcomeDigest(RemoteOutcome)` 从重建后的 typed `CaptureOutcome` 得到同一 number。只改 DTO 不改两侧
canonicalizer 将必然 digest mismatch，因此三点必须同波落地。

### 4. 三道 revision fence、重投/乱序与 terminal 语义

#### 4.1 ACTIVE 三道权威门

除 request builder 的额外 preflight `CloudTaskRunExecutionGate#validate(...)` 外，三道 authoritative fence 是：

1. **Cloud enqueue**：`RemoteGameCommandBroker#dispatchAndAwait(...) -> #authorizationRejection(...) ->
   RemoteTaskRunCoordinator#authorize(...)`，精确核
   `(tenantId,userId,deviceId,clientSessionId,taskRunId,windowId,nativeHandle,processId,
   playerIdentityEpoch,stopEpoch,runRevision)`，且 ACTIVE 的
   `runRevision == currentRunRevision == confirmedExecutionRevision`。
2. **Cloud dispatch held-lock**：`RemoteGameCommandBroker#poll(...) ->
   RemoteTaskRunCoordinator#authorizeAndMarkDispatch(...)` 在最终 dequeue/dispatch 线性化点再次核同一 tuple/revision。
3. **DHXY pre-side-effect**：`LocalRemoteGameCommandHandler#executeOwnedCommand(...)` 的
   `#requireRegistration(...) + #requireBoundWindow(...) + #requireRegistration(...)`，scope 来自 fixed
   `RemoteClientSessionRef`/local registration，要求 command taskRun/window/stopEpoch 全等且
   `command.runRevision == RemoteTaskRunRegistration.runRevision`，随后才进入 scale/capture。

capture 完成后已有 registration/binding/geometry post-fence；Q 只增加 Cloud pending observedWindow correlation，不减任何门。
input worker admission revision fence完全不改。

#### 4.2 PAUSED_READ_ONLY 三道对称门

- enqueue：`RemoteTaskRunCoordinator#pausedObservationDenialReason(...)`；
- dispatch：`#authorizePausedObservationAndMarkDispatch(...)`；
- local：`LocalRemoteGameCommandHandler#classifyRemoteRun(...)` 要求 marker + CAPTURE + local registration
  `PAUSED` + exact paused revision，再由 `requireRegistration` 比较 runRevision。

该路径不查询 confirmed execution revision，这是既有 observer 合同；resume/stop/complete 后 revision/status 改变，旧观察
capture 永久失效。

#### 4.3 stale、乱序、重投

- revision R 构造的 request 在 pause/resume/re-confirm 到 R+2 后，至少在 enqueue/dispatch/local 任一门得到
  `NOT_EXECUTED/TASK_RUN_MISMATCH`；window/stopEpoch 恰巧没变也不能复活。
- 同 `(scope,operation,requestId)` + 同 requestDigest 重投：Cloud broker 返回同 pending/terminal；DHXY
  `RemoteOperationLedger#claim(...)` 等待/返回同一 cached outcome，不重新读 scale、不重新截图。
- 同 requestId 不同 digest：`IDEMPOTENCY_CONFLICT`；wrong captureId、request/action/taskRun/digest、scope/session 或
  OBSERVED wrong observedWindow 的 outcome 在 Cloud pending correlation 前拒绝，不能完成 waiter。
- outcome 先于 pending、错误 session、错误 operation 的乱序回包返回 REJECTED；同 terminal digest 是 DUPLICATE；已有
  non-UNKNOWN terminal 后不同 outcome 是 `IDEMPOTENCY_CONFLICT`。
- broker 已记录 UNKNOWN 后，`#acceptLateResolutionLocked(...)` 只接受一次同 exact request 的 non-UNKNOWN typed final；
  第二个相同 digest为 duplicate，不同 final 为 conflict。下次同字节 port 调用由 `#awaitRetainedResolution(...)` 返回该 late
  final，不 enqueue/dispatch 新 capture。

#### 4.4 execution state 行为

| state | CAPTURE 本切片行为 | identity / 业务行为 |
|---|---|---|
| `OBSERVED` | image/hash/size/provider/scale/observedWindow 全部完整且 digest/correlation 通过 | 唯一可交给 matcher 的成功；不重拍 |
| `NOT_EXECUTED` | 副作用前 scale unavailable、stale fence、wrong binding 等；所有 observation 字段 null | 不自动 retry；只有既有 retained owner 在验证真实 NOT_EXECUTED 后可走既有 renewal，本切片不调用/新增该路径 |
| `UNKNOWN` | 已截图后 scale 不稳定、已 dispatch timeout/transport uncertainty；字段 null | 绝不当业务 miss，绝不换 ID；只允许同字节 retained resolution/late final |
| `STOPPED` | Cloud terminal gate或 DHXY stop 已生效；字段 null | 不 renew、不重发，按上层既有 stop typed unwind |
| `EXECUTED` | `CaptureOutcome` 构造期非法 | 不可能被 accepted |

稳定 identity 继续完全归
`CloudTaskRunActionLedger` / `CloudTaskRetainedActionState`：Q 不修改 `acquire`、`bindOrVerifyRequest`、
`renewAfterNotExecuted`、observation identity、broker/local ledger，也不添加任何 raw mint API。CAPTURE 重投继续复用原
`requestId/actionId/captureId/requestDigest`；本切片只给该 exact outcome 增加被 digest 保护的 mechanical fact。

### 5. 精确写集（批准后最小波次）

Design #1 提议 **New 0 / Modify 8**（DHXY 4、Cloud 4；其中 Java 7、schema 1）。固定日志追加不计代码写集。

| 仓库 | FQCN / 路径 | New/Modify | 精确改动 |
|---|---|---|---|
| DHXY | `com.bot.dhxy.cloud.remote.RemoteCaptureOutcomePayload` | Modify | 在 provider 与 observedWindow 间增加 boxed `Double systemScaleRatio`，维持声明/wire 顺序 |
| DHXY | `com.bot.dhxy.cloud.remote.LocalRemoteGameCommandHandler` | Modify | `executeCapture` 增加 scale bracket/failure mapping/payload；`emptyCapturePayload` 增加 null；新增 private `readSystemScaleRatioNow()`；不改其它 operation |
| DHXY | `com.bot.dhxy.cloud.remote.RemoteProtocolDigests` | Modify | canonicalizer 增加有限 binary64 数字分支；request/outcome tree 结构其余不变 |
| DHXY | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | Modify | 更新 5.2 outcome 字段/strict null规则、JCS double 表示、scale failure 与 coordinated cutover；不改 request schema |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CaptureOutcome` | Modify | 同顺序增加 boxed `Double`；OBSERVED finite-positive required，非 OBSERVED null；`withCommon` 保留值 |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.RemoteCommandOutcomeEnvelope` | Modify | private `CapturePayload` 同字段；capture-only exact key/type校验；typed reconstruction 传 scale |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.RemoteProtocolDigests` | Modify | 与 DHXY 逐字同构的 finite binary64 canonical 分支 |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.RemoteGameCommandBroker` | Modify | synthetic capture terminal 增加 null 参数；`validateAgainstPending` 对 OBSERVED capture 增加 exact observedWindow correlation |

明确 **零交叉/零改动**：

- P：不改 `CloudTaskRunRetainedLifecycleActivationAdapter`、`CloudTaskRunAuthorityAssembly`；
- 外部 A：不改 `MaintenanceProbeResult`、`MaintenanceUnresolvedException`、TMS/capability/caller/log；
- 外部 B：不改 `AutoCombatPanelDecision`、未来 `AutoCombatPanelService`/warning sink/log；
- N：不改 `SummonSkillStaticSlotPolicy`、未来 SummonSkill workflow/policy/log；
- 不改 `RemoteGameClientPort` / `CloudTaskServicePort` 方法签名，不改 action ledger/retained state/coordinator/registry/
  lifecycle adapter/host/caller/Service/poller/input queue/BoundWindowCaptureService/CoordinateHelper/resources/tests。

这里特意不修改当前 dirty `CoordinateHelper`：既保留全部现有本地业务行为与用户改动，又避免把其 fallback `1.0` 误当
wire truth。scale 现场读取只属于 typed capture owner。

### 6. 兼容、切换顺序与构建门

#### 6.1 最小可编码波次

父级明确写入 `DESIGN APPROVED` 后只做一个原子 **Q-SCALE-WIRE** 波次：上述 8 文件一起修改。原因是新 DHXY producer +
旧 Cloud 会因 outcome digest/字段不一致被拒绝，新 Cloud + 旧 DHXY 的 OBSERVED outcome 缺 required scale 也会被 strict
decoder 拒绝。不能把任一侧单独宣称可运行。

`contractVersion` 保持 1，以保证所有非 capture request/operation 零变化；代价是 CAPTURE 不支持 mixed-version rolling。
切换顺序固定：

1. 保持当前 host/caller/poller/capture producer dormant（或部署时先 quiesce CAPTURE）；
2. 同一源码 cohort 完成双仓实现与双构建；
3. 先部署 Cloud decoder/digest/broker，但仍不激活 capture caller；
4. 再部署 DHXY producer；
5. 仅在双侧版本一致后，由 B/N 各自独立批准切片接 caller/Service；Q 不执行第 5 步。

回滚同样先 quiesce CAPTURE，再双侧一起回滚；禁止靠“缺值=1.0”、忽略字段或跳过 digest 做兼容。

#### 6.2 no-local-test 与双构建门

- 遵守 no-local-test：不新增/恢复/运行/cite 单元测试、source guard、replay、testcase image 或 marked output。
- Java 实施后、全部并发 Worker 写入稳定且由构建 owner 确认不会争用 `target/` 时，必须 fresh 执行：
  - DHXY 根目录：`mvn -q -DskipTests compile`；
  - Cloud 根目录：`mvn -q -DskipTests clean package`。
- 任一失败都不交给用户运行；先修 compile/package。构建不启动 server/host/poller/UI/capture/input。
- future fresh-runtime 验收只看真实 capture outcome/log：OBSERVED 必须同时有 exact binding、image hash、正 finite scale 与双侧
  digest accepted；scale failure 必须出现 typed NOT_EXECUTED/UNKNOWN 且 B/N matcher 没有业务 miss。该 runtime 属后续 caller
  激活验收，不是本 design/编码波次的本地测试。

### 7. Worker Q 自审（仅 QA，不构成批准）

- Parent Task Brief #1 五项均有精确 owner/FQCN/方法/状态/写集；没有新增业务判断、TTL、takeover、retry、extra capture、
  ROI/template/click/sleep/phase/fallback 变化。
- request identity/digest、非 capture operation 与 P/A/B/N 写集零改；OBSERVED capture 才新增 required typed fact。
- 自审 `P0=0 / P1=0 / P2=0`，但父级未写 `DESIGN APPROVED` 前继续零 Java/schema/resources/tests/Maven/runtime。

**无已批准业务差异；按基线等价迁移。** Design #1 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## Parent Design Review #1 - DESIGN APPROVED / External A Implementation Published - 2026-07-13T02:34:00-04:00

父级已逐项对照双仓现有 CAPTURE handler/envelope/typed outcome/digest/broker correlation 与
`CoordinateHelper#initScaleRatio()` 的 pushed baseline。Design #1 的 producer、同一 handler 事实边界、双仓 strict wire、三道
revision fence、retained identity、UNKNOWN/STOPPED/NOT_EXECUTED 与原子切换顺序闭合。结论：
**DESIGN APPROVED，P0/P1/P2=0**，允许实施严格 **DHXY 4 Modify + Cloud 4 Modify** 的单一原子波次。

实施绑定如下：

1. 只允许设计表列出的 8 个文件；不改 request schema、port、ledger、coordinator、lifecycle、Service/caller/host/tests。
2. 仍只截一帧；scale 前后读取只用于证明该帧期间事实稳定，不得缓存、默认 `1.0`、sleep、重试或另拍。失败映射和字段 null
   合同按 Design #1 原样实现。
3. 两仓 floating canonicalizer 必须同构；capture payload exact-key/type/null 校验与 OBSERVED `observedWindow` pending correlation
   必须在同波落地。普通 request digest 与非 capture outcome 的 canonical bytes 不得变化。
4. Design #1 §6.2 的 Cloud 命令文字有误，父级在此覆盖：Cloud 构建必须为 **`mvn -q clean package`（不可 skip tests）**；
   DHXY 为 `mvn -q -DskipTests compile`。不新增/恢复测试。
5. 生产/host/caller 继续 dormant；本任务只生成可编译双仓协议实现，不启动 capture/input/poller/application。

**实现任务 `Q-SCALE-WIRE-IMP1` 已交给现有外部 Worker A。** A 必须在
`2026-07-13T02:54:00-04:00` 前向本日志追加 `## External Worker A - CLAIMED - <timestamp>`，注明唯一 8 文件写集；领取后可工作
超过 20 分钟。完成后追加 `External Worker A - Q-SCALE-WIRE Implementation #1`，列精确 diff、双仓字段/摘要同构证据与双构建
结果，等待父级源码复审。Internal Worker Q 的设计任务到此关闭，不再改代码或日志。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T02:37:55-04:00

领取父级实施任务 **Q-SCALE-WIRE-IMP1**（Parent Design Review #1 放行的单一原子波次）。唯一写集 = **DHXY 4 Modify + Cloud 4 Modify（共 8 文件，见 Design #1 文件表）**，不改 request schema/port/ledger/coordinator/lifecycle/Service/caller/host/tests，不新增/恢复测试，生产/host/caller 继续 dormant。
CLAIMED 仅证领取不表完成。将据 Design #1 逐项实现（只截一帧、scale 前后读证稳定不缓存/不默认 1.0/不 sleep/不重试/不另拍；双仓 floating canonicalizer 同构；capture payload exact-key/type/null + OBSERVED observedWindow pending correlation 同波落地；普通 request digest 与非 capture outcome canonical bytes 不变；三道 revision fence + retained identity + UNKNOWN/STOPPED/NOT_EXECUTED + 原子切换顺序）。完成后运行 Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile`，追加 `External Worker A - Q-SCALE-WIRE Implementation #1`（精确 diff + 双仓字段/摘要同构证据 + 双构建结果）等父级源码复审。领取截止 2026-07-13T02:54:00-04:00，本回执在截止前。

## External Worker A - Q-SCALE-WIRE Implementation #1 - 2026-07-13T02:52:12-04:00

严格 **DHXY 4 Modify + Cloud 4 Modify（共 8 文件，0 New，Java 7 + schema 1）**，未改 request schema/port/ledger/coordinator/lifecycle/Service/caller/host/tests/CoordinateHelper/BoundWindowCaptureService，未新增测试，未启动运行面。P/A(TMS)/B/N 写集零触碰。

### 精确改动

**Cloud（4 Modify）**
1. `remote.CaptureOutcome`：record 在 `captureProvider` 与 `observedWindow` 间加 `Double systemScaleRatio`；OBSERVED 分支要求 `systemScaleRatio != null && Double.isFinite && > 0.0`；非-OBSERVED 分支纳入 `systemScaleRatio == null`；`withCommon` 传新字段。
2. `remote.RemoteCommandOutcomeEnvelope`：private `CapturePayload` record 同位加 `Double systemScaleRatio`；`captureOutcome(common)` 先 `requireClosedCapturePayload()`（closed-object：恰八 key、captureId non-blank、OBSERVED 七字段 non-null 且 scale 为 finite 正 number、非-OBSERVED 七字段显式 null），再 reconstruction 传 scale；加 `CAPTURE_PAYLOAD_KEYS`/`CAPTURE_OBSERVATION_KEYS` 常量 + `java.util.{HashSet,List,Set}` import。
3. `remote.RemoteProtocolDigests`：canonicalizer 在 integral 分支后加 `isFloatingPointNumber()` 分支 → `appendCanonicalDouble(...)`（有限 binary64：非 finite 拒绝、±0→"0"、`BigDecimal.valueOf(v).stripTrailingZeros()` shortest digits、`e=precision-scale-1`、`-6<=e<21` plain 否则小写 e 科学计数）；加 `java.math.BigDecimal` import。其余分支逐字不变。
4. `remote.RemoteGameCommandBroker`：`validateAgainstPending` 对 CAPTURE 且 `executionState==OBSERVED` 增加 `observedWindow` 与 `pending.context.window()` 的四字段（windowId/nativeHandle/processId/playerIdentityEpoch）精确比较；`terminalOutcome` synthetic `new CaptureOutcome(...)` 补一个 null（scale）保持非-OBSERVED 全 null。

**DHXY（4 Modify）**
5. `cloud.remote.RemoteCaptureOutcomePayload`：`@Value @Builder @Jacksonized` 在 `captureProvider` 与 `observedWindow` 间加 `Double systemScaleRatio`。
6. `cloud.remote.LocalRemoteGameCommandHandler`：`executeCapture` 固定顺序 `captureRectangle -> scaleBefore -> 唯一 captureRegion -> (empty? CAPTURE_FAILED) -> scaleAfter -> PNG/hash -> post-fence -> OBSERVED`；`scaleBefore` 非法→`NOT_EXECUTED/FACT_UNAVAILABLE`（截图未始）；`scaleAfter` 非法或与 before `Double.doubleToLongBits` 不等→flush image + `UNKNOWN/FACT_UNAVAILABLE`；OBSERVED payload `.systemScaleRatio(scaleBefore)`；`emptyCapturePayload` 加 `.systemScaleRatio(null)`；新增 private static `readSystemScaleRatioNow()`（现场 `defaultScreenDevice->defaultConfiguration->defaultTransform.getScaleX()`，异常返 null，无缓存/无 1.0 兜底）与 `isValidSystemScaleRatio(Double)`；加 `java.awt.GraphicsEnvironment` import。其它 operation 未改。
7. `cloud.remote.RemoteProtocolDigests`：canonicalizer 加与 Cloud **逐字同构**的 `isFloatingPointNumber()`→`appendCanonicalDouble(...)` 分支 + `java.math.BigDecimal` import。
8. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`：更新 §5.2（八字段 closed schema、strict null、scale failure、同帧证明链、coordinated cutover）与 §3 的 JCS 有限 binary64 canonical 子集；request schema 未改。

### 双仓同构证据

- **canonicalizer 逐字同构**：`diff` 两仓 `appendCanonicalDouble` 方法体 = **IDENTICAL**（byte-for-byte 相同）。
- **outcome 字段顺序同构**：两仓均 `... captureProvider, systemScaleRatio, observedWindow`（Cloud record / DHXY payload 与 flat wire payload 同序）。
- **null 表示一致**：DHXY 产出侧 `RemoteOperationPayloadCodec` 默认 mapper（无 NON_NULL）→ 非-OBSERVED 全 8 key 显式 null，满足 Cloud closed-shape「恰八 key」；两仓 digest 侧均省略 null（DHXY `mergeNonNullFields` skip null / Cloud `MAPPER=NON_NULL`），systemScaleRatio 均走 DoubleNode→floating 分支，preimage 对称。
- **零变化确认**：普通 request 与非 capture outcome 无合法 floating 字段，digest byte-for-byte 不变（既有 21 Cloud 测试全绿即回归证据）。

### 构建结果（父级 Review #1 point 4 覆盖命令）

- DHXY 根目录 `mvn -q -DskipTests compile` → **exit 0**。
- Cloud 根目录 `mvn -q clean package`（不 skip tests）→ **exit 0**；Surefire **4 suites / Tests run: 21 / Failures 0 / Errors 0 / Skipped 0**；shaded JAR `dhxy-cloud-brain-0.1.0-SNAPSHOT.jar` 生成。

生产/host/caller 继续 dormant；本波只产出可编译双仓协议实现，未启动 capture/input/poller/application。Q-SCALE-WIRE Implementation #1 到此停止，等待父级源码复审。

## Parent Implementation Review #2 - BLOCKED / Repair #1 Published - 2026-07-13T03:02:43-04:00

父级已逐文件复核 A 的 8 文件写集，并在写入稳定后重新执行双构建：Cloud `mvn -q clean package` exit 0，
Surefire `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`，shaded JAR SHA-256
`875821FB3EC5A1F08260C5EB45C6B65B79C0889876C7892558F7094B5DB9C84F`；DHXY
`mvn -q -DskipTests compile` exit 0。字段顺序、strict 8-key capture shape、non-OBSERVED null、同帧 scale bracket、
OBSERVED window correlation、两仓方法体同构及三道 revision fence 均成立。**但源码仍 BLOCKED，P0=0 / P1=1 / P2=1；
绿色构建不覆盖该协议互操作错误。**

1. **P1：floating formatter 不符合本文宣称的 RFC 8785/JCS。** 两仓
   `RemoteProtocolDigests#appendCanonicalDouble` 都用
   `BigDecimal.valueOf(double).stripTrailingZeros()`。对 IEEE-754 最小正数 `Double.MIN_VALUE`，该实现生成
   `4.9e-324`；RFC 8785 Appendix B / ECMAScript `JSON.stringify(Number.MIN_VALUE)` 的 canonical token 是
   `5e-324`。父级已现场复核 Node 输出为 `5e-324`，Java `Double.toString` / `BigDecimal.valueOf` 输出为
   `4.9E-324`。影响是两套 Java 当前彼此能验过，但任何按 RFC 8785 实现的第三方会计算不同 digest；协议文档
   `thin-client-protocol-schema.md:97` 的 JCS 声明不成立。RFC 8785 本身列出的 Java 参考实现为
   `io.github.erdtman:java-json-canonicalization`，其公开 `NumberToJSON.serializeNumber(double)` 使用 ECMAScript
   number serialization。返修必须在两仓仅替换 floating token 生成器为该类或同等已验证 RFC 8785 实现；不得替换
   整棵现有 canonicalizer、不得改变 integral/string/object/array 分支，普通 request 与非 capture outcome bytes 必须继续不变。
2. **P2：拒绝诊断仍谎称禁止 floating。** DHXY `RemoteProtocolDigests.java:200` 与 Cloud
   `RemoteProtocolDigests.java:168` 的最终异常仍写 `forbids/does not allow floating or binary values`，但前一分支已经
   接受 finite floating。返修为只拒绝 unsupported binary node，并同步更新 schema 第 97 行，删除
   `BigDecimal.valueOf` 算法描述，明确采用 RFC 8785/ECMAScript finite binary64 serialization。

### 下一任务：`Q-SCALE-WIRE-IMP1-R1`

External Worker A 必须在 **2026-07-13T03:22:43-04:00** 前先追加
`## External Worker A - CLAIMED - <timestamp>`；20 分钟只检查领取，领取后可继续超过 20 分钟。允许写集为原 8 文件中
两仓 `RemoteProtocolDigests.java` + protocol schema，以及两仓 `pom.xml`（只增加相同 Apache-2.0 Maven 依赖
`io.github.erdtman:java-json-canonicalization:1.1`），合计 **5 Modify**；其余 6 个已正确 Java 文件不得重开。
两仓只调用 `org.erdtman.jcs.NumberToJSON.serializeNumber(double)` 生成 floating token，保留 finite 检查并把受检
`IOException` 收敛成 fail-closed `IllegalArgumentException`；禁止复制/手写 Ryu、禁止整树换 canonicalizer。完成后运行 Cloud
`mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，报告精确 diff、依赖坐标、两仓方法体同构和构建
结果；不新增/恢复测试，不启动任何运行面，不做 Git mutation。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T03:07:56-04:00

领取父级返修任务 **Q-SCALE-WIRE-IMP1-R1**（Parent Implementation Review #2 的 P1×1+P2×1）。唯一写集 = **5 Modify**：
- Cloud `com.yueyunfe.dhxy.cloudbrain.remote.RemoteProtocolDigests`
- DHXY `com.bot.dhxy.cloud.remote.RemoteProtocolDigests`
- DHXY `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`（§97 JCS 声明）
- Cloud `pom.xml` + DHXY `pom.xml`（仅增相同 Apache-2.0 依赖 `io.github.erdtman:java-json-canonicalization:1.1`）

其余 6 个已正确 Java 文件不重开。两仓 floating token 只调 `org.erdtman.jcs.NumberToJSON.serializeNumber(double)`，保留 finite 检查、受检 IOException 收敛成 fail-closed IllegalArgumentException；final 异常改为只拒绝 binary node；schema §97 删除 BigDecimal 算法、改述 RFC 8785/ECMAScript finite binary64。两仓方法体保持逐字同构。完成后 Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile`，追加 Implementation Repair #1（diff/依赖坐标/同构/构建）。领取截止 2026-07-13T03:22:43-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。

## External Worker A - Q-SCALE-WIRE Implementation Repair #1 - 2026-07-13T03:12:50-04:00

严格 **5 Modify**（两仓 RemoteProtocolDigests + schema §97 + 两仓 pom.xml）；其余 6 个已批 Java 文件零触碰。关闭 Review #2 的 P1（floating formatter 不符 RFC 8785）与 P2（异常谎称禁止 floating）。

### 精确改动

1. Cloud `pom.xml` / DHXY `pom.xml`：在 jackson-databind 依赖后各新增**相同** Apache-2.0 依赖坐标
   `io.github.erdtman:java-json-canonicalization:1.1`（RFC 8785 官方 Java 参考实现），无其它依赖/version/plugin 改动。
2. Cloud `com.yueyunfe.dhxy.cloudbrain.remote.RemoteProtocolDigests` / DHXY `com.bot.dhxy.cloud.remote.RemoteProtocolDigests`：
   `appendCanonicalDouble(double,StringBuilder)` 方法体**整体替换**为——`if (!Double.isFinite(value)) throw IllegalArgumentException;`
   保留 finite 检查，随后 `target.append(NumberToJSON.serializeNumber(value));`，受检 `IOException` catch 收敛为
   fail-closed `IllegalArgumentException("cannot canonicalize finite number", e)`。删除旧 `BigDecimal.valueOf(...).stripTrailingZeros()`
   手写算法；`import java.math.BigDecimal` 换为 `import org.erdtman.jcs.NumberToJSON;` + `import java.io.IOException;`。
   integral/string/boolean/object/array 分支与 `appendCanonical` 调用点逐字不变。
3. 两仓 canonicalizer final 异常改为只拒绝 binary node：Cloud `"protocol v1 digest does not allow binary JSON values"`；
   DHXY `"remote protocol canonical JSON forbids binary values"`（floating 已由前一分支接受）。
4. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` §97：删除 `BigDecimal.valueOf` 算法描述，改述 finite
   binary64 token 采用 **RFC 8785 / ECMAScript number serialization**，由 `org.erdtman.jcs.NumberToJSON.serializeNumber(double)`
   生成（例 `Double.MIN_VALUE -> 5e-324`）。

### RFC 8785 正确性实证（隔离运行，使用 m2 已拉取的 1.1 jar）

`NumberToJSON.serializeNumber(...)` 输出：
```
Double.MIN_VALUE(Java toString 4.9E-324) -> 5e-324   ← 修复：旧 BigDecimal 实现产 4.9e-324（与 RFC 8785 不符）
1.0 -> 1        1.25 -> 1.25        1.5 -> 1.5        100.0 -> 100        0.0001 -> 0.0001
```
`5e-324` 与 RFC 8785 Appendix B / ECMAScript `JSON.stringify(Number.MIN_VALUE)` 一致。

### 双仓同构与构建

- **方法体逐字同构**：`diff` 两仓 `appendCanonicalDouble` = **IDENTICAL**。
- DHXY `mvn -q -DskipTests compile` → **exit 0**（依赖解析成功）。
- Cloud `mvn -q clean package`（不 skip）→ **exit 0**；Surefire **4 suites / Tests run: 21 / Failures 0 / Errors 0 / Skipped 0**。
- 普通 request 与非 capture outcome 无合法 floating 字段，digest byte-for-byte 不变（21 测试全绿即回归证据）。

生产/host/caller 继续 dormant；未新增测试，未启动运行面，未做 Git mutation。Q-SCALE-WIRE Implementation Repair #1 到此停止，等待父级源码复审。

## Parent Implementation Review #3 - FINAL APPROVED / Next Handoff - 2026-07-13T04:18:00-04:00

父级重新读取双仓最终源码、POM 与协议 schema，并在所有并发 Java 写入稳定后执行 fresh 构建。结论：
**Q-SCALE-WIRE-IMP1-R1 IMPLEMENTATION APPROVED，P0/P1/P2=0**。

- 双仓 `RemoteProtocolDigests.appendCanonicalDouble(...)` 都使用
  `org.erdtman.jcs.NumberToJSON.serializeNumber(double)`；finite 校验、`IOException` fail-closed 与方法体同构成立，
  `Double.MIN_VALUE -> 5e-324`，关闭旧手写 formatter 的 RFC 8785 偏差。
- 双仓 POM 均固定 `io.github.erdtman:java-json-canonicalization:1.1`；schema 明确 RFC 8785 / ECMAScript binary64
  token。普通 request 与非 capture outcome 的合法字段集未增加 floating number，既有 digest 语义不变。
- 父级 fresh Cloud `mvn -q clean package` exit 0；Surefire `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`。
  父级 fresh DHXY `mvn -q -DskipTests compile` exit 0。shaded JAR SHA-256 为
  `52AC214B7B82A9397F538F31C9F461D80E677F42C7E0382871E597DB4DAAFAD7`。
- host/caller/Task/poller/UI/capture/input 继续 dormant；本批准不构成生产切换。

外部 Worker A 的下一任务转到
`docs/superpowers/plans/reports/2026-07-13-cloud-quest-manager-service-worker-a.md` 的 `W-QM-D1`。
A 必须在 `2026-07-13T04:38:00-04:00` 前在新日志追加 `External Worker A - CLAIMED`（任务标题、领取时间、
唯一写集）；20 分钟只检查领取，领取后可工作超过 20 分钟。A 不再修改本 scale-wire 日志或已批准代码，除非父级明确重开。

**无已批准业务差异；按基线等价迁移。**
