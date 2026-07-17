# RemoteGameClientPort 最小认证与凭据生命周期

工件编号：A-6

状态：lift-and-shift 开工契约 v1

本文只定义三种同步调用 `CAPTURE`、`WINDOW_FACT`、`EXECUTE_INPUT_BUNDLE` 开工所需的最小认证边界。它不引入自定义 frame signer、session HKDF、资产 keyring 或控制消息总协议。

## 1. 认证边界

- transport 使用 TLS 1.3 HTTPS；客户端必须验证服务端证书和主机名，服务端必须认证 device credential。
- 认证上下文至少解析出 `tenantId,userId,deviceId,clientSessionId`，由 server middleware 注入，不能信任 request body 自报身份。
- 每个 device credential 只允许调用绑定到该 device 的 RemoteGameClientPort；跨 tenant、user 或 device 一律 `AUTH_FAILED`。
- request body 中的 `windowId/nativeHandle/processId/playerIdentityEpoch` 仍必须经过本地 `WindowRuntimeContext` 核对。认证成功不代表窗口绑定正确。
- access token、refresh token、device private key 和完整 Authorization header 禁止进入日志、metrics、截图和 outcome message。

## 2. Credential 生命周期

```text
UNENROLLED -> ACTIVE -> SUSPENDED -> ACTIVE
ACTIVE|SUSPENDED -> REVOKED
```

| 状态 | 新调用 | 已开始调用 |
|---|---|---|
| ACTIVE | 允许，继续做 binding/stop/timeout 门 | 按请求执行 |
| SUSPENDED | 拒绝 AUTH_FAILED | CAPTURE/FACT 丢弃未返回结果；未开始输入取消，已开始输入返回 UNKNOWN 或 STOPPED |
| REVOKED | 永久拒绝 AUTH_FAILED | 与 SUSPENDED 相同，并关闭 session |

device private key 优先存 Windows DPAPI/TPM 支持的不可导出存储。服务端只存验证材料或 credential hash。credential 轮换采用先激活新 credential、确认客户端可用、再撤销旧 credential的顺序；旧 credential 撤销后不得 fallback。

## 3. Token 规则

- access token 短期有效，建议不超过 60 分钟；refresh token 绑定 device 并单次滚动。
- refresh token 重放时吊销该 token family 并暂停对应 device session。
- 客户端不得内置共享永久 token。
- transport 断线、token 过期或 credential 撤销不是 `NOT_EXECUTED` 证明。EXECUTE_INPUT_BUNDLE 已开始而结果未知时必须返回或由云端映射为 `UNKNOWN`。

## 4. 请求完整性与幂等身份

三种请求都携带：

```text
contractVersion, operation, requestId, actionId, taskRunId,
window, stop, timeoutMs, requestDigest
```

`requestDigest = hex(SHA-256(JCS(request 去掉 requestDigest 字段)))`。服务端和本地 adapter 都校验 digest；相同 `(operation,requestId)` 不同 digest 拒绝 `IDEMPOTENCY_CONFLICT`。EXECUTE_INPUT_BUNDLE 的 actionId 还必须唯一映射到一个 requestId/digest，冲突拒绝 `ACTION_ID_REUSE`。

`JCS` 固定指 RFC 8785 canonical JSON 的 UTF-8 bytes；两端不得用字段插入顺序或平台默认 JSON bytes 计算 digest。

本契约不要求每个 request 再做自定义 detached signature；TLS + device credential 是 v1 transport trust boundary。未来更换 transport 时必须保留同样的认证上下文、digest、幂等和错窗门，不能仅凭 requestId 授权。

## 5. 窗口与输入授权

认证 middleware 只决定“哪个 device 在调用”；本地 safety gate 决定“该 request 是否仍属于这个窗口和 task run”。执行前必须同时满足：

1. device credential ACTIVE。
2. taskRunId 与当前窗口任务一致。
3. stop.taskRunId 一致、stopEpoch 未落后且本地未 stop。
4. windowId、nativeHandle、processId、playerIdentityEpoch 全部等于当前 WindowRuntimeContext。
5. timeout 尚未到期。
6. requestId/actionId 未发生幂等冲突。

任何失败都不得标题搜索替代窗口、降低绑定字段、忽略 stop 或在另一窗口执行。

## 6. 最小权限

device credential 只授予三项 permission：

| permission | operation | 限制 |
|---|---|---|
| `game.capture` | CAPTURE | 仅已注册窗口和请求 ROI |
| `game.window_fact.read` | WINDOW_FACT | 仅 BINDING/GEOMETRY/FOCUS_STATE/STOP_STATE |
| `game.input.execute` | EXECUTE_INPUT_BUNDLE | 仅 typed InputAction 列表、当前 taskRun、单 input queue |

没有 `game.input.execute` 时不得通过 CAPTURE/WINDOW_FACT payload 夹带输入命令。没有 capture permission 时不得通过 WINDOW_FACT 返回图片 bytes。

## 7. 审计与日志

每次调用记录：`tenantId,userId,deviceId,clientSessionId,operation,requestId,actionId,taskRunId,windowId,nativeHandle后8位,processId,playerIdentityEpoch,requestDigest,executionState,outcomeCode,elapsedMs`。

EXECUTE_INPUT_BUNDLE 额外记录 `actionCount,startedStepIndex,lastCompletedStepIndex,inputQueueRequestId`，但不得记录 TYPE_TEXT_UNICODE/PASTE_TEXT 的 text 内容。CAPTURE 只记录 ROI、provider、尺寸和 imageSha256，不记录图片 bytes。

审计只用于追踪，不改变 outcome，不生成业务重试或 successor。

## 8. 撤销与故障处理

- AUTH_FAILED：本地不调用 capture/fact/input provider。
- credential 在排队期间变为 SUSPENDED/REVOKED：未开始 request 取消；物理输入已开始则停止余下步骤并标记 UNKNOWN 或 STOPPED，禁止重发。
- transport timeout/断线：CAPTURE/FACT 未返回的数据丢弃；EXECUTE 是否开始不明时标记 UNKNOWN。
- 客户端重启：无法从幂等台账证明 input 未开始的 actionId 返回 UNKNOWN/CLIENT_RESTARTED。
- 恢复认证后，云端继续使用现有 Service 的业务恢复逻辑；本地不推断下一动作。
