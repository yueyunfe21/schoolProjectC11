# Worker 1 RemoteGameClientPort 最小契约交付报告

## Status

`DONE`

完成日期：2026-07-12

方向：用户已选择直接 lift-and-shift；完整形式化协议不再作为开工门。本次交付只收口同步 `RemoteGameClientPort` 的最小类型化通信契约。

## Exact Changed Files

1. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`
2. `docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md`
3. `docs/superpowers/plans/reports/2026-07-12-a2-protocol-closure-report.md`

未修改 Java、总稿、shared draft、final design、migration matrix、data model、CR、dashboard 或其它文件；未回退其他 worker 的变更。

## Delivered Contract

### 1. RemoteGameClientPort

只保留三个同步方法：

- `capture(CaptureRequest) -> CaptureOutcome`
- `readWindowFact(WindowFactRequest) -> WindowFactOutcome`
- `executeInputBundle(InputBundleRequest) -> InputBundleOutcome`

云端现有 Service 保留业务判断、调用顺序、sleep、重试/fallback、点击顺序和完成条件；本地只返回机械事实或执行结果。

### 2. Common Typed Identity

三个 request 统一要求：`contractVersion,operation,requestId,actionId,taskRunId,WindowBindingRef,StopRef,timeoutMs,requestDigest`。

WindowBindingRef 直接对应当前本地权威字段：

- `WindowRuntimeContext.getWindowId()`
- `WindowNativeBinding.getNativeHandle()`
- `WindowNativeBinding.getProcessId()`
- `WindowRuntimeContext.getPlayerIdentityEpoch()`

任一不一致都在副作用前返回 `NOT_EXECUTED/WRONG_WINDOW`；禁止标题搜索替代窗口。

### 3. Capture And Facts

- CAPTURE 支持现有 `SCREEN_ABSOLUTE_PX` ROI，并允许明确的 `WINDOW_CLIENT_PX`；结果带 bytes/hash/尺寸/provider/observed binding。
- WINDOW_FACT 只允许 `BINDING/GEOMETRY/FOCUS_STATE/STOP_STATE` sealed variants，不允许自由业务 map。
- 两者读取后再次核验 binding；发生漂移时丢弃结果。

### 4. Input Bundle

- InputActionDto 覆盖当前 `InputActionType` 全部枚举及 required/forbidden 字段。
- 坐标保持当前 `SCREEN_ABSOLUTE_PX` 语义，不移动既有坐标换算职责。
- 整个 bundle 一次进入 `InputSequences.submitAndWait(...)` / `InputActionQueue`；move+sleep+click 不拆包。
- callback、循环、条件、候选和业务 fallback 不跨协议。
- 入队前、worker 开始前和 step 间检查 window/stop/timeout；已开始但结果不确定时为 UNKNOWN，禁止物理重发。

### 5. Outcome And Idempotency

统一 execution state：`NOT_EXECUTED/EXECUTED/OBSERVED/UNKNOWN/STOPPED`，并冻结 OutcomeCode。

本地台账规则：

- `(operation,requestId)` 同 digest 返回同一 future/terminal outcome。
- 相同 requestId 不同 digest 拒绝 `IDEMPOTENCY_CONFLICT`。
- EXECUTE_INPUT_BUNDLE 的 actionId 唯一绑定一个 requestId/digest，冲突拒绝 `ACTION_ID_REUSE`。
- `EXECUTED/UNKNOWN/STOPPED` 不自动重发物理输入。
- 客户端重启后无法证明未开始的 actionId 返回 `UNKNOWN/CLIENT_RESTARTED`。

### 6. Minimal Security

- TLS 1.3 + device credential 是 v1 transport trust boundary。
- middleware 注入 tenant/user/device/session，request body 不能自报身份。
- permission 仅 `game.capture`、`game.window_fact.read`、`game.input.execute`。
- credential lifecycle 仅 `UNENROLLED/ACTIVE/SUSPENDED/REVOKED`，撤销后不 fallback。
- request digest、日志脱敏、text/image bytes 禁止入审计。

## Removed Complexity

以下内容不再作为直迁开工门，已从两份工件删除：自定义 binary frame、frame signer/session algorithm registry、TLS exporter、HKDF/FENCE_ACK、CRITICAL/NORMAL 调度、lease/outbox/resync 总状态机、HTTPS upload grant、execution budget registry、MATCH recipe/derived bundle 和全消息 wire registry。

保留的安全核心只有：认证身份、request/action 幂等、错窗拒绝、stop/timeout、单输入队列和结构化 outcome。

## Verification

```powershell
rg -n "same v|同 v|TBD|TODO|实现者|按需|以后决定" docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md
```

结果：无输出，exit `1` 表示没有匹配。

```powershell
git diff --check -- docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md docs/superpowers/specs/2026-07-12-thin-client-security-key-lifecycle.md docs/superpowers/plans/reports/2026-07-12-a2-protocol-closure-report.md
```

结果：最终验证要求 exit `0`，无 whitespace error。

附加定点检查覆盖：三个 operation、RemoteGameClientPort、requestId/actionId、四字段 window binding、stop/timeout、五态 outcome、幂等冲突、错窗拒绝、`InputSequences.submitAndWait` 原子 bundle。

## Current Hashes

| File | SHA-256 |
|---|---|
| protocol spec | `e37fb8fea793bc0237af2a09e1a9c3b884515e0aa0be45bfb9c04c1d15c5bd00` |
| security spec | `a90e02813a86918d8554b8b8fee88d7156aa07498996b75d13497f1b34fb3fc5` |

## Remaining Concern

无文档范围 blocker。未修改或编译 Java；后续实现应逐个现有 Service 做同步 lift-and-shift，并以 latest pushed business path 为行为基线。
