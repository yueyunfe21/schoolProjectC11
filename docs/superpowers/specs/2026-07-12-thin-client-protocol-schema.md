# A-2 协议 Schema（THIN_CLIENT_V1）

工件编号：A-2（终审 Final #1 工件计划）
来源共识：草案 §5、Q2/Q3/Q5、Final #2-#5、B Final #2、B Final #3、B Final #5（A-2 v2 复审 4P1+2P2）
状态：设计工件 v3
约束：设计级；目标=可据此生成 codec/validator/executor。与草案冲突以草案已收口共识为准并回报。

---

## 1. 传输拓扑

- 每设备一条 WebSocket，binary frame（§2）；图片走 HTTPS + upload grant + frameId 引用；inline ROI ≤ 协商上限。
- 断开=本地不得新业务输入（CLOUD_SUSPENDED）。

## 2. Wire Layout（唯一定义）

```
MAGIC        : 2B = 0x54 0x43 ("TC")
FRAME_KIND   : 1B = 0x01 BOOTSTRAP | 0x02 FENCED
DOMAIN_TAG   : 1B = 协议域标识（版本+用途，防跨协议/跨 kind 重解释）
SIG_ALG      : 1B = 签名算法 id（仅服务端从设备预注册 allowlist 选定；帧内标识用于验签选路，不用于协商）
SIG_LEN      : 2B (≤ MAX_SIGNATURE_BYTES)
SIGNATURE    : SIG_LEN B（detached）
SIGNED_LEN   : 4B (≤ MAX_SIGNED_BYTES)
SIGNED_BYTES : SIGNED_LEN B = 帧体 UTF-8 JSON 精确字节
```

- **被签内容 = MAGIC ‖ FRAME_KIND ‖ DOMAIN_TAG ‖ SIG_ALG ‖ SIGNED_BYTES**（B Final#5 P1-1：签名覆盖帧类型与域标签，防跨 frame-kind/跨协议重解释）。
- 长度门在分配/解析**之前**校验：SIG_LEN≤MAX_SIGNATURE_BYTES、SIGNED_LEN≤MAX_SIGNED_BYTES，越界=`FRAME_MALFORMED`。
- JSON 解析限深/限长（§11 的 JSON 资源上限）；payload 字段=base64，解码后≤MAX_DECODED_PAYLOAD_BYTES。

## 3. FENCED Envelope（SIGNED_BYTES JSON）

```
Envelope {
  envelopeVersion, tenantId, userId, deviceId, clientSessionId,
  connectionFence : long, direction : C2S|S2C,
  channelClass : CRITICAL|NORMAL|BULK,        // §5 分类，决定队列/保留容量
  streamKind : DEVICE_CONTROL|WINDOW|TASK_RUN, streamKey : string,
  sequence : long,                             // (fence,direction,channelClass,streamKey) 内单调
  messageId, timestamp, expiry, messageType,
  scope : ScopeObject, payloadSchemaVersion, payloadDigest,
  payload : string(base64)
}
```

解码顺序：帧长度门 → 验签 → envelopeVersion → fence → channelClass/stream/sequence → expiry → scope → base64+digest → 按 type 解析。EXEC 型未知字段/enum/重复=拒绝；非 EXEC 仅忽略注册表登记的 `x-*`。

## 4. Bootstrap Transcript（B Final#5 P1-1：精确 wire schema + 防降级）

FRAME_KIND=BOOTSTRAP，签名/MAC 输入同 §2（覆盖 MAGIC/KIND/DOMAIN_TAG/SIG_ALG）。三段各有精确 JSON schema：

```
HELLO (C→S, 设备私钥签名):
  { deviceId, clientSessionId, clientNonce(32B b64),
    envelopeVersionRange:{min,max}, supportedSigAlgs[](仅供服务端参考),
    tlsChannelBinding(exported keying material hash),   // 绑定本次 TLS
    clientBuildHash, clientAllowlistHash }

FENCE_GRANT (S→C, 服务端 key 签名):
  { serverNonce(32B b64), chosenSigAlg(从该设备预注册 allowlist 选,非客户端提议),
    newConnectionFence, protocolVersion, minCompatibleVersion,
    buildHash, allowlistHash, policyEpoch, assetEpoch,
    echo:{clientNonce, tlsChannelBinding} }         // 回显证明同一 TLS/会话

FENCE_ACK (C→S, 设备私钥签名):
  { deviceId, echo:{serverNonce, newConnectionFence},
    acceptedProtocolVersion, installedFence,
    tlsChannelBinding }                              // 再次绑定,证明非重放
```

规则：
- 算法只能由服务端从**设备 enrollment 时预注册的 allowlist** 选定（A-6 §4.1），客户端不得强制降级到弱算法。
- 三段签名域各含 clientNonce+serverNonce+tlsChannelBinding → transcript 抗重放、抗跨会话拼接。
- FENCE_ACK 完成前服务端零 dispatch；BOOTSTRAP 帧不得携业务 payload；客户端安装 fence 后拒一切旧 fence 帧。
- 版本/hash 不匹配 → CLOUD_SUSPENDED。

## 5. Channel Class 与关键控制通道（B Final#5 P1-2）

每消息类型在注册表标 channelClass，独立队列 + 独立 sequence 路径，**互不阻塞**：

| class | 消息 | 保障 |
|---|---|---|
| CRITICAL | TASK_COMMAND(stop/pause/emergency)、ACTION_OUTCOME、REVOKE_LEASE、INPUT_LANE_DRAINED、MESSAGE_RECEIVED、RESYNC_*、FENCE 相关 | 保留容量，永不 THROTTLED；不受 NORMAL/BULK stream gap 或背压阻塞；独立 sequence(不与普通 fact 混序) |
| NORMAL | ACTION_PLAN、CAPTURE_SPEC、OBSERVATION_PLAN、START_TASK 等 | 常规顺序门 |
| BULK | inline ROI 大 payload、非关键 observation 批 | 可背压/可重采 |

- 关键正确性事实（stop/outcome/drain）走 CRITICAL，本地 stop/pause/emergency 仍立即生效，CRITICAL 通道只负责**及时把事实+目标 task/action 因果键送达**，不新增本地业务决策。
- sequence 作用域含 channelClass：`(fence, direction, channelClass, streamKey)`——一个 class 的 gap 不连坐另一个 class。

## 6. Scope Object 与 Stream（同 v2，channelClass 正交叠加）

ScopeObject/四档 required-forbidden 表、streamKind+streamKey 生命周期同 v2 §5/§6。sequence 域按 §5 扩展为四元组。

## 7. 消息注册表（一行一 wire type + scope + channelClass）

（DEVICE_CONTROL/WINDOW/TASK_RUN 消息表同 v2 §7，每行增列 channelClass；下列为 v3 变更/新增项）

- TASK_COMMAND / ACTION_OUTCOME / MESSAGE_RECEIVED / INPUT_LANE_DRAINED / RESYNC_* / REVOKE_LEASE → **CRITICAL**。
- MESSAGE_RECEIVED payload 五字段全核验（同 v2 §7.5）。
- **ACTION_OUTCOME_ACK（B Final#5 P2-1 修正）**：ACK 本身不携后继动作；successor 由**同一 PG 事务生成、经独立 ACTION_PLAN envelope（独立 messageId/actionId/outbox 行）投递**，正常走签名+sequence+write-before-send，不嵌入 ACK 绕过。
- **lease 消息拆分（B Final#5 P2-2）**：
  - `LEASE_GRANT`（S→CRITICAL）：授予，唯一 leaseId+epoch。
  - `LEASE_KEEP`（S→CRITICAL）：续持 fact，幂等键=(leaseId,epoch)。
  - `LEASE_RELEASE`（S→CRITICAL）：释放命令，需 ACK。
  - `REVOKE_LEASE`（S→CRITICAL）：强制回收命令，需 ACK。
  - 本地 lane 状态词汇统一为权威机 `FREE|HELD|REVOKING`（删除 DRAINING 别名，INPUT_LANE_STATE 用 REVOKING 表述"正在排空"）。

## 8. ActionPlan、栅栏与 MATCH recipe

```
ActionPlan {
  actionId, planKind : GENERIC_INPUT | MATCH_AND_CLICK | MATCH_AND_REPORT,
  inputLaneId,                              // B Final#5 P1-3：显式,证明 lease 授权的是本 lane
  leaseId, leaseEpoch,
  frameBasis {basedOnFrameId, expectedFrameHash, frameCaptureTime}
           | {noFrameReason : FOCUS_ONLY|WINDOW_STATE_ONLY},
  expectedBindingGeneration, windowIncarnation,
  expectedWindowGeometry, expectedDpiScale, coordinateSpace,
  playerIdentityEpoch, policyVersion, assetVersions[],
  maxFrameAge,                              // ≤ MAX_FRAME_AGE absolute cap
  declaredExecutionBudgetMs,
  steps[]?                                  // 仅 GENERIC_INPUT；MATCH_* 禁带
  match? : MatchSpec                        // MATCH_* 必填
}
```

### 8.1 GENERIC_INPUT
扁平 InputStep 列表（FOCUS_WINDOW/KEY_PRESS/HOTKEY/MOUSE_MOVE/CLICK/SLEEP），单 exclusive callback，无循环分支。

### 8.2 MATCH_AND_CLICK / MATCH_AND_REPORT（B Final#5 P1-3：禁任意 steps，固定 recipe）
- **planKind=MATCH_* 时 `steps[]` 必须缺省**；本地绝不接受云端给的 move/click 坐标（云端签 plan 时不知 match box，给了也是错的）。
- GenericMatchExecutor 从 `match.box + match.clickOffset` **机械导出唯一固定 recipe**：`MOUSE_MOVE(box+offset) → SLEEP(固定微延迟) → MOUSE_LEFT_CLICK`（recipe 由 assetVersion+算法版本决定，非云端逐次下发）。
- 坐标单一权威=本地导出（不存在云端/本地坐标双权威）；outcome 记录 {box, 最终窗口相对点, 屏幕绝对点, derivedBundleDigest}。
- 时序：全程持 lane lease 且 capture/match 期 lane 无旧请求 → callback 外 capture+match → 成功且 resultMode=CLICK → 单 callback 执行导出 recipe，入口重验 {fence,binding,assetHash,matchAge≤cap,坐标窗口内}。失败只报事实。

### 8.3 执行门与预算（本地重算）
- 启动门：`declaredBudget ≥ derivedBudget` 且 `now(服务端换算) + derivedBudget ≤ expiry` 且 步数≤ABSOLUTE。
- **derivedBudget 由版本化最坏耗时表推导**（B Final#5 P1-4）：覆盖 focus/move/click provider 各自固定最坏耗时 + sleep 声明，不只算 sleep；耗时表版本随 quotaProfile 登记。
- 硬 expiry：callback 进入前 + 每物理步骤前复检，过期停余步报 STOPPED/UNKNOWN。

## 9. 状态机

Outbox（含 ENQUEUED→OUTCOME_RECEIVED 直接边、receipt 不改执行态）、Lease（FREE→HELD→REVOKING→…，drain proof 完整限定）、connectionFence 换代（复用排空屏障）—— 均同 v2 §9，不复述；lease 命令拆分见 §7。

## 10. 时钟与时效

同 v2 §10。补：frame/match 年龄本地单调钟；expiry 服务端时间。

## 11. 常数表（B Final#5 P1-4：cap / floor / 协商 三列）

| 常数 | ABSOLUTE cap（编译期硬顶，不可上调） | SAFETY floor（不可下调） | 协商默认 |
|---|---|---|---|
| MAX_INPUT_STEPS | 16 | — | 12 |
| MAX_STEP_SLEEP_MS | 5000 | — | 2000 |
| MAX_FRAME_AGE_MS | 5000 | — | 800 |
| MAX_MATCH_TO_CLICK_AGE_MS | 2000 | — | 500 |
| CLOCK_SKEW_SAFETY_MS | — | **≥ 实测 uncertainty（floor，协商只可更大）** | 5000 |
| MAX_CLOCK_SKEW_MS | 5000 | — | 2000 |
| MAX_SIGNATURE_BYTES | 4096 | — | — |
| MAX_SIGNED_BYTES | 262144 | — | — |
| MAX_DECODED_PAYLOAD_BYTES | 131072 | — | 65536 |
| MAX_JSON_DEPTH | 32 | — | — |
| MAX_JSON_STRING_BYTES | 65536 | — | — |
| MAX_JSON_COLLECTION | 4096 | — | — |
| MAX_INLINE_ROI_BYTES | 131072 | — | 65536 |
| MAX_CONTROL_MSG_BYTES | 524288 | — | 262144 |

`CLOCK_SKEW_SAFETY_MS` 是 forced-handoff 安全垫：**floor 语义**（协商只可更大，绝不更小），且必须 ≥ 部署实测时钟 uncertainty。

## 12. 拒绝码

（同 v2 §12，增 `FRAME_MALFORMED`、`SIG_ALG_NOT_ALLOWED`、`BOOTSTRAP_REPLAY`、`JSON_LIMIT_EXCEEDED`、`CHANNEL_CLASS_VIOLATION`、`MATCH_PLAN_HAS_STEPS`。）

## 13. 旧协议兼容与删除

同 v2 §13。
