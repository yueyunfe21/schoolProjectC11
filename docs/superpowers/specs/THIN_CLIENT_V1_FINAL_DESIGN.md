# THIN_CLIENT_V1 全量云端业务大脑与 Thin Client 最终设计

日期：2026-07-12
状态：Final Design（自包含单文档；整合目标边界 §1-§10、分题共识 Q1-Q7、设计工件 A-1~A-7、A/B 双代理终审全部修正）
性质：本文是唯一自包含交付物，实现者无需跨文件或跨版本拼接。凡"实现期确定"的项均显式标注 `impl-tuning-only`（仅物理类型/索引参数/压测数值），一切安全/身份/状态/重放/租户/窗口/lane 约束在本文设计期定死。

---

# 第一部分 · 目标与硬边界

## 1. 目标

云端成为唯一权威业务大脑，持有全部业务状态、规则、视觉理解、任务编排与跨窗口调度；本地退化为 Thin Client，只负责绑定游戏窗口、采集原始画面、执行受限动作、报告执行事实、实施本地安全拒绝。云端通过图片与结构化事实观察本地，通过类型化动作计划控制本地。现有本地程序在新体系建成前继续作为生产基线，新体系完成后以整体版本原子切换，不做逐 Service 生产切换。

## 2. 硬边界（不可推翻）

- **唯一业务大脑**：云端是每窗口每任务运行的唯一权威状态持有者；本地不保留第二套业务状态机，云端超时/失败时本地不自选 phase/fallback/业务路径。
- **本地只有拒绝权**：LocalSafetyGate 可因 pause/stop、过期/重复/序号回退/签名无效、身份不匹配、坐标/ROI 越界、非法按键、lane 不可得、frame/模板 hash 不符而拒绝；拒绝后只报结构化事实，不得改写坐标/扩 ROI/降阈值/换模板/走 fallback/选下一 phase/重启流程。
- **Service 边界不重拆**：现有业务 Service 整体迁入同一 Cloud Brain 应用，第一版不拆微服务；仅拆物理 I/O 边界（云端决定 / 本地 executor 执行）。
- **整体建设、原子切换**：所有云端 Service 一起建设；建设期旧程序为基线；全部依赖完成后以 THIN_CLIENT_V1 整体切换；运行时无单 Service 本地回退；严重问题部署级整体回滚。

## 3. 本地最终组件（tier A/D，无业务语义）

| 组件 | 允许 | 禁止 |
|---|---|---|
| CloudConnection | 控制流/上传图/重连/认证 | 解释业务响应、选 fallback |
| WindowRegistry | windowId/HWND/尺寸/DPI/绑定生命周期（HWND 事实权威） | 判断业务状态 |
| CaptureExecutor | 原始窗口/云端指定 ROI 截图 | 洗图/OCR/颜色分类/模板策略 |
| ActionPlanExecutor | 校验并原子执行类型化动作 | 改写计划/插业务动作/决定下一 phase |
| ObservationExecutor | 按计划采样上报机械事实 | 把事实解释为业务状态 |
| GenericMatchExecutor | 固定 ROI 内一次签名模板匹配+点击/报告 | fallback/扩 ROI/降阈值/换模板 |
| LocalSafetyGate | 拒绝不安全/过期/错窗/重复 | 修正云端动作/生成替代业务动作 |
| InputLane | 串行化物理输入、报占用/释放 | 决定哪个业务窗口下次获 turn |
| LocalActionLedger | 保存最近 action/frame/sequence/结果（连接内易失） | 保存完整业务 phase/本地恢复策略 |
| SignedAssetCache | 按 hash 缓存签名模板 | 持有可独立运行的业务模板库 |
| DesktopUI | 登录/设备/窗口注册/任务命令/状态展示 | 本地解释业务规则 |

## 4. 云端组件

任务/导航/交互/维护/队伍/视觉/记忆等现有业务 Service + Session/Window/TaskRun 状态仓库、Task Orchestrator、Cloud Task Turn Scheduler、ActionPlan/ObservationPlan 生成器、OCR/OpenCV 资源池、记忆与资产版本、认证/授权/租户隔离/设备身份、指标/trace/审计/后台管理。

## 5. 通信与数据归属（详见第三、四部分）

双通道：WebSocket 控制流（每设备一条，多路复用多窗口）+ HTTPS 图片上传（frameId 引用）。云端权威保存全部业务数据；本地只存连接设置、认证材料、UI 布局、当前会话窗口绑定与 action ledger、可删除签名资产缓存。

## 6. 生产部署方向

单体模块化 Cloud Brain；多用户/设备共享服务端但按 tenant/user/device/session 严格隔离；Redis 存活跃 session/窗口/租约/短期去重/心跳；PostgreSQL 存用户/设备/配置/策略/记忆/版本/审计；对象存储存图片/ROI/模板/资产；有界 OCR/OpenCV worker pool；状态外置后不依赖 sticky 实例。

---

# 第二部分 · Service 迁移矩阵（A-1 摘要 + 完备性门）

> 完整方法级底账见 `2026-07-12-service-migration-matrix.md`（本轮 workflow 产出：11 业务包 fan-out 扫描）。以下为摘要与完备性状态。

## 7. 盘点结果

- 覆盖 **191 类 / 450 关键方法 / 337 条隐式状态**（主库 navigation-migration 分支源码基线）。
- tier 分布：**A 44 类/133 方法**（状态/协议/身份/lease/输入/stop-pause 安全层）、**B 32 类/200 方法**（影响 phase/动作/retry/fallback/timeout/memory 的业务决策）、**C 22 类/62 方法**（视觉/OCR/模板解释）、**D 93 类/55 方法**（纯 DTO/搬运/枚举）。
- 隐式状态：fallback 98、timer 63、memory 58、cache 38、lock 23、other 57。
- **反向扫描候选 56 个**：tier=D 却携隐式状态或命中游戏语义关键词的可疑类——"伪装成 DTO 的本地大脑残留"重点复核清单。

## 8. Q1 六门完备性状态（诚实，阻塞切换）

| # | 门 | 状态 | 说明 |
|---|---|---|---|
| 1 | 方法级 inventory 全覆盖 | PARTIAL | 191 类逐类底账；全量私有/lambda/继承/条件注册闭包待 ASM 字节码扫描 |
| 2 | production-reachability 闭包 | NOT_DONE | 需从 Runner/TaskFactory 入口做可达图，当前按包枚举 |
| 3 | 反向语义扫描全命中映射 | PARTIAL | 已列 56 候选；机械化规则+全命中映射待建 |
| 4 | 配置/资源完整归属 | PARTIAL | 隐式状态穷举；resources 树（模板/FXML/JSON/ROI/阈值/反射字符串）待逐文件 |
| 5 | 每行 owner+disposition+依赖+基线+隐式+客观验收条件 | PARTIAL | 摘要级已具，逐行验收条件待补 |
| 6a | allowlist 真实构建证据 | NOT_EVALUATED（实现后） | tier=D+localRetained=无 集合为候选；按 Q7 阻塞 S6/S7 |
| 6b | 迁后业务流人工反向抽查 + fresh runtime | NOT_EVALUATED（实现后） | 按 Q7 阻塞 S6/S7 |

门 1-5 为**设计期当前必须完成**（B 终审边界），门 6a/6b 为**实现后产生**、不阻止 Final Proposed 但按 Q7 阻塞切换。**门 1-5 未全绿前 A-1 不通过。**

---

# 第三部分 · 协议 Schema（A-2）

## 9. Wire Layout（唯一定义）

WebSocket binary frame（网络字节序）：
```
MAGIC(2B "TC") | FRAME_KIND(1B: 01 BOOTSTRAP|02 FENCED) | DOMAIN_TAG(1B 协议域,防跨协议重解释)
| SIG_ALG(1B 签名算法id,仅服务端从设备预注册allowlist选) | SIG_LEN(2B ≤MAX_SIGNATURE_BYTES)
| SIGNATURE(detached) | SIGNED_LEN(4B ≤MAX_SIGNED_BYTES) | SIGNED_BYTES(Envelope UTF-8 JSON 精确字节)
```
- **被签内容 = MAGIC‖FRAME_KIND‖DOMAIN_TAG‖SIG_ALG‖SIGNED_BYTES**（覆盖帧类型与域标签）。
- 长度门在分配/解析前校验；JSON 限深/限长；`payload` 字段=base64，解码后 ≤MAX_DECODED_PAYLOAD_BYTES。
- Envelope JSON 内无 signature 字段（detached 在帧头）。

## 10. Bootstrap Transcript（防降级/防重放）

三段精确 schema，签名域同 §9（覆盖 MAGIC/KIND/DOMAIN_TAG/SIG_ALG）：
```
HELLO(C→S,设备私钥签): {deviceId, clientSessionId, clientNonce(32B), envelopeVersionRange,
  tlsChannelBinding(exporter keying material hash), clientBuildHash, clientAllowlistHash}
FENCE_GRANT(S→C,服务端key签): {serverNonce(32B), chosenSigAlg(从该设备预注册allowlist选),
  newConnectionFence, protocolVersion, minCompatibleVersion, buildHash, allowlistHash,
  policyEpoch, assetEpoch, echo:{clientNonce, tlsChannelBinding}}
FENCE_ACK(C→S,设备私钥签): {deviceId, echo:{serverNonce, newConnectionFence},
  acceptedProtocolVersion, installedFence, tlsChannelBinding}
```
- 算法由服务端从设备 enrollment 预注册 allowlist 选（客户端不得强制降级）。
- 三段各含双 nonce + TLS channel binding → 抗重放/抗跨会话拼接。
- FENCE_ACK 完成前零 dispatch；BOOTSTRAP 帧不携业务 payload；客户端安装 fence 后拒一切旧 fence 帧；版本/hash 不匹配→CLOUD_SUSPENDED。

## 11. FENCED Envelope

```
{envelopeVersion, tenantId, userId, deviceId, clientSessionId, connectionFence(long),
 direction(C2S|S2C), channelClass(CRITICAL|NORMAL|BULK), streamKind(DEVICE_CONTROL|WINDOW|TASK_RUN),
 streamKey, sequence(long), messageId, timestamp, expiry, messageType, scope(ScopeObject),
 payloadSchemaVersion, payloadDigest, payload(base64)}
```
- sequence 作用域 = `(connectionFence, direction, channelClass, streamKey)`——一个 class/stream 的 gap 不连坐其他。
- 解码顺序：帧长度门→验签→envelopeVersion→fence→channelClass/stream/sequence→expiry→scope→base64+digest→按 type 解析。执行型（EXEC）未知字段/enum/重复=拒绝；非 EXEC 仅忽略注册表登记的 `x-*`。

**streamKey 生命周期**：DEVICE_CONTROL 常量 `"device"`；WINDOW=`w:{windowRegistrationId}#{incarnation}`（注销/incarnation 变即关，旧 key 不复用）；TASK_RUN=`t:{taskRunId}`（START_TASK_ACK 创建，终态关，rebind 由 RESYNC_DECISION 显式重开）。

## 12. Scope Object（随帧被签）

```
ScopeObject { windowId?, windowRegistrationId?, windowIncarnation?, bindingGeneration?, taskRunId?, actionId? }
```
| 档 | 必填 | 禁填 |
|---|---|---|
| DEVICE_SCOPE | （空对象） | 全部 scope 字段 |
| WINDOW_SCOPE | windowId, windowRegistrationId, windowIncarnation, bindingGeneration | taskRunId, actionId |
| TASKRUN_SCOPE | WINDOW_SCOPE 全部 + taskRunId | actionId（非动作类）|
| ACTION_SCOPE | TASKRUN_SCOPE 全部 + actionId | — |
违规=`SCOPE_VIOLATION`。

## 13. Channel Class（关键控制通道隔离）

| class | 消息 | 保障 |
|---|---|---|
| CRITICAL | TASK_COMMAND(stop/pause/emergency)、ACTION_OUTCOME、REVOKE_LEASE、LEASE_*、INPUT_LANE_DRAINED、MESSAGE_RECEIVED、RESYNC_*、FENCE 相关 | 保留容量，永不 THROTTLED；不受 NORMAL/BULK 背压或 stream gap 阻塞；独立 sequence |
| NORMAL | ACTION_PLAN、CAPTURE_SPEC、OBSERVATION_PLAN、START_TASK | 常规顺序门 |
| BULK | inline ROI 大 payload、非关键 observation 批 | 可背压/可重采 |

**传输隔离实现约束**：messageType→channelClass 由注册表固定，发送方自报错误 class=拒绝；每 class 独立有界队列 + 保留容量 + 写出前严格优先级调度；单个非 CRITICAL frame 有最大不可抢占字节/时间上界；若该上界不满足 critical-control SLO，则 BULK 移出控制 WS（不得宣称"互不阻塞"而不给上界）。

## 14. 消息注册表（一行一 wire type + scope + channelClass）

DEVICE_CONTROL：HEARTBEAT, WINDOW_REGISTER(+ACK), WINDOW_UNREGISTER, INPUT_LANE_STATE, INPUT_LANE_DRAINED[CRITICAL], LOCAL_LEDGER_RESET, THROTTLED, RESYNC_BEGIN/REPORT/DECISION[CRITICAL], REVOKE_LEASE[CRITICAL], MESSAGE_RECEIVED[CRITICAL]。
WINDOW：START_TASK(WINDOW_SCOPE 请求,requestId 幂等,taskRunId 服务端幂等生成)、START_TASK_ACK、CAPTURE_SPEC_W、FRAME_REF_W。
TASK_RUN：TASK_COMMAND[CRITICAL](+ACK,requestId 幂等,pause/stop 本地先行=事实上报)、CAPTURE_SPEC、FRAME_REF、ACTION_PLAN[EXEC]、ACTION_OUTCOME[CRITICAL](+ACK,同 actionId 同 digest 幂等/异 digest 冲突)、OBSERVATION_PLAN/FACT、PROVISION/RETIRE_DETECTOR。
Lease（四个唯一语义 wire type，均 CRITICAL）：LEASE_GRANT / LEASE_KEEP / LEASE_RELEASE / REVOKE_LEASE，各有 ACK/幂等键；lane 状态词汇统一 FREE|HELD|REVOKING（无 DRAINING 别名）。
**successor 不嵌 ACK**：ACTION_OUTCOME_ACK 不携后继；successor 由同一 PG 事务生成、经独立 ACTION_PLAN envelope（独立 messageId/actionId/outbox）正常签名+sequence+write-before-send 投递。

**MESSAGE_RECEIVED payload**：`{receivedConnectionFence, receivedStreamKey, receivedSequence, receivedMessageId, receivedPayloadDigest, receivedActionId?}`——五字段与原签发 envelope 全量比对，跨 stream 错配不可通过；回执自身走 DEVICE_CONTROL 用自己的 sequence；只证送达不证执行。

## 15. ActionPlan、栅栏与 MATCH recipe

```
ActionPlan { actionId, planKind(GENERIC_INPUT|MATCH_AND_CLICK|MATCH_AND_REPORT), inputLaneId,
  leaseId, leaseEpoch, frameBasis{basedOnFrameId,expectedFrameHash,frameCaptureTime}|{noFrameReason:FOCUS_ONLY|WINDOW_STATE_ONLY},
  expectedBindingGeneration, windowIncarnation, expectedWindowGeometry, expectedDpiScale, coordinateSpace,
  playerIdentityEpoch, policyVersion, assetVersions[], maxFrameAge(≤ABSOLUTE cap),
  declaredExecutionBudgetMs, executionBudgetPolicyVersion, steps[]?(仅GENERIC_INPUT), match?:MatchSpec(MATCH_*必填) }
MatchSpec { templateId?, templateHash, matchAlgorithmVersion, matchRecipeVersion, roi, threshold, clickOffset,
  resultMode(CLICK|REPORT_ONLY), maxMatchToClickAgeMs }
```
- **原子性**：一个有副作用 plan = 一个有界 input bundle，单 exclusive callback；callback 内禁 capture/HTTP/OCR/模板/业务分支。步骤词汇 FOCUS_WINDOW/KEY_PRESS/HOTKEY/MOUSE_MOVE/CLICK/SLEEP(≤MAX_STEP_SLEEP)；无裸 KEY_DOWN/KEY_UP。
- **硬 expiry**：callback 启动门 `now(服务端换算)+derivedBudget ≤ expiry` 且 `declared≥derived` 且步数≤ABSOLUTE；每物理步骤前复检 expiry+leaseEpoch，过期停余步报 STOPPED/UNKNOWN。derivedBudget 由版本化最坏耗时表（覆盖 focus/move/click provider 各自最坏耗时 + sleep）本地重算，budget 表 version/hash 绑 plan/handshake，不一致即拒。
- **MATCH（禁双坐标权威）**：MATCH_* 禁带 steps；GenericMatchExecutor 从 match.box+clickOffset 按 matchRecipeVersion 机械导出唯一固定 recipe（move→micro-sleep→click），坐标单一权威=本地导出；全程持 lane lease 且 capture/match 期 lane 无旧请求；callback 入口重验 {fence,binding,assetHash,matchAge≤cap,坐标窗口内}。outcome 映射：匹配失败/栅栏失效=NOT_EXECUTED+matchFact、完成=EXECUTED、中断=STOPPED、REPORT_ONLY=OBSERVED。

## 16. Outcome 五态与重放白名单

| 态 | 语义 | 可自动重发 |
|---|---|---|
| NOT_EXECUTED | 未进入物理执行（三条件可证） | 可，换新 actionId |
| EXECUTED | 输入调用完成（≠业务成功） | 否 |
| OBSERVED | 仅无物理副作用消息 | n/a |
| UNKNOWN | 不可证（DISPATCHED 无 outcome/PG 故障窗/灾备恢复） | 否，重观察 |
| STOPPED | stop/pause/expiry 中途生效（可能已执行部分步骤） | 否 |

**E4 白名单**：仅 `NOT_EXECUTED && physicalInputStarted=false && completedInputSteps=0` 可换新 actionId 重试；EXECUTED/UNKNOWN/STOPPED 一律不自动重放。

## 17. 常数表（ABSOLUTE cap / SAFETY floor / 协商默认）

| 常数 | ABSOLUTE cap | SAFETY floor | 协商默认 |
|---|---|---|---|
| MAX_INPUT_STEPS | 16 | — | 12 |
| MAX_STEP_SLEEP_MS | 5000 | — | 2000 |
| MAX_FRAME_AGE_MS | 5000 | — | 800 |
| MAX_MATCH_TO_CLICK_AGE_MS | 2000 | — | 500 |
| CLOCK_SKEW_SAFETY_MS | — | ≥实测 uncertainty（协商只可更大） | 5000 |
| MAX_CLOCK_SKEW_MS | 5000 | — | 2000 |
| MAX_SIGNATURE_BYTES | 4096 | — | — |
| MAX_SIGNED_BYTES | 262144 | — | — |
| MAX_DECODED_PAYLOAD_BYTES | 131072 | — | 65536 |
| MAX_JSON_DEPTH / STRING_BYTES / COLLECTION | 32 / 65536 / 4096 | — | — |
| MAX_CONTROL_MSG_BYTES | 524288 | — | 262144 |

`impl-tuning-only`：具体字节数可在压测后于 [floor, cap] 内调，但 cap/floor 本身设计期定死。

## 18. 拒绝码

SIGNATURE_INVALID, FRAME_MALFORMED, SIG_ALG_NOT_ALLOWED, VERSION_MISMATCH, BOOTSTRAP_VIOLATION, BOOTSTRAP_REPLAY, FENCE_STALE, SEQUENCE_GAP, SEQUENCE_REGRESS, STREAM_CLOSED, EXPIRED, BUDGET_REJECTED, SCOPE_VIOLATION, CHANNEL_CLASS_VIOLATION, IDENTITY_MISMATCH, BINDING_MISMATCH, INCARNATION_MISMATCH, LEASE_EPOCH_STALE, FRAME_STALE, FRAME_HASH_MISMATCH, ASSET_HASH_MISMATCH, MATCH_AGE_EXCEEDED, MATCH_PLAN_HAS_STEPS, COORD_OUT_OF_ROI, KEY_NOT_ALLOWED, STEPS_EXCEEDED, JSON_LIMIT_EXCEEDED, DUPLICATE_MESSAGE, DUPLICATE_OUTCOME_EVICTED, OUTCOME_DIGEST_CONFLICT, REQUEST_CONFLICT, FACT_DIGEST_CONFLICT, USER_PAUSED, USER_STOPPED, LANE_UNAVAILABLE, SHADOW_REALM_REJECTED, UNSUPPORTED_TYPE。

---

# 第四部分 · 权威状态与数据模型（A-3）

## 19. 全局约束

- 复合键规约：租户内实体 PK/FK 均 `(tenant_id, id)`；跨表引用复合 FK——DB 层不可能跨租户引用。
- RLS：全部租户表启用；策略读 `current_setting('app.tenant_id')`，该值由认证入口 `SET LOCAL`（transaction-local，业务 SQL/payload 不可改写）；`app_rw` 无 BYPASSRLS；跨租户仅 `admin_ops`（RLS+审计触发，限本租户管理域）与 `maintenance`（离线只读/归档，禁业务写，全审计）。
- enum/迁转 CHECK；非法迁转=事务失败。
- Redis 键与对象存储前缀**全部含 tenant/device 作用域**，由服务端认证身份构造，禁 payload 自报；对象存储 bucket/prefix ACL + metadata tenant 交叉核验（RLS 只护 PG，缓存/对象串租户靠此挡）。

## 20. 数据分级与灾备（与第六部分一致）

| 级别 | 数据 | RPO |
|---|---|---|
| correctness | action_ledger、protocol_fact、outbox_envelope、connection_fence、input_lane_lease、lease_entity、outstanding_action、task_run.revision、authority_current/event、绑定 correctness 迁转的 audit | **0**（同步耐久副本/同步 journal）|
| durable-audit | 独立运营审计 | ≤5min PITR |
| evidence | 帧/ROI/性能 trace | 按 §33 保留期 |

correctness RPO=0 不可达时唯一合法姿态：全局停机 + 人工外部对账，禁自动 re-admission。

## 21. 核心表规格

**身份连接**：tenant、app_user`(UNIQUE(tenant,login_identity), trusted_publisher)`、device`(public_key, enrollment_status)`、client_session、connection_fence`((tenant,device) PK, generation BIGINT, CAS 单调永不复用)`。

**窗口任务**：
- window_registration`((tenant,window_registration_id) PK, window_id, incarnation, last_confirmed_hwnd, geometry, dpi_scale, coordinate_space, binding_generation, status)`；UNIQUE`(tenant,device,window_id,incarnation)`；**partial UNIQUE `(tenant,device,window_id) WHERE status=ACTIVE`**（禁双 ACTIVE incarnation）。
- task_run`((tenant,task_run_id) PK, task_type, status, revision BIGINT, state_snapshot, policy_revision, score_policy_version, asset_version_set_digest, quota_profile_version)`；task_run_id=H(tenant|device|windowRegistrationId#incarnation|requestId)；partial UNIQUE 活跃 run per (tenant,window_registration)。
- request_ledger`((tenant,device,request_id) PK, request_kind CHECK(START_TASK|TASK_COMMAND), request_payload_digest, request_schema_version, target_task_run_id, command, response_digest, response_snapshot)`；幂等命中先比 request_payload_digest，异则 REQUEST_CONFLICT。

**输入权/动作/单飞**：
- lease_entity`((tenant,lease_id) PK, device_id, lane_id, granted_epoch, granted_at, released_at)`。
- input_lane_lease`((tenant,device,lane_id) PK, current_lease_id FK, state CHECK(FREE|HELD|REVOKING), holder_window_registration_id, holder_task_run_id, holder_client_session_id, holder_connection_fence, lease_epoch)`。
- outstanding_action`((tenant,device,lane_id) PK, action_id FK, since)`——**PK per lane 从 DB 层禁双 DISPATCHED（单飞强制）**。
- action_ledger`((tenant,action_id) PK, 复合 FK 到 task_run/device/window_registration/lease_entity, window_incarnation, binding_generation, input_lane_id, lease_id, lease_epoch, connection_fence, plan_kind, message_id UNIQUE(tenant,message_id), payload_digest, frame_basis_kind CHECK(FRAME|NO_FRAME), based_on_frame_id, expected_frame_hash, frame_capture_marker, max_frame_age, no_frame_reason, expected_geometry, expected_dpi_scale, coordinate_space, policy_version, asset_versions_digest, declared_budget_ms, state CHECK 五态, physical_input_started, completed_steps, outcome_digest, successor_action_id FK 自引用, recovery_disposition, late_resolution_fact_id, dispatched_at, closed_at, tombstone)`；frame_basis CHECK：FRAME 则四字段必填、NO_FRAME 则 reason∈allowlist。

**协议事实账本**（Q7 100% 因果闭包）：
- protocol_fact`((tenant,fact_id) PK, fact_kind CHECK(CONTROL_ENVELOPE_C2S|CONTROL_ENVELOPE_S2C|ACTION_PLAN|ACTION_OUTCOME|FRAME_META|WINDOW_BINDING|CAPTURE_SPEC|OBSERVATION_PLAN|OBSERVATION_FACT|MESSAGE_RECEIVED|ACTION_CHECKPOINT|LEASE_TRANSITION|FENCE_TRANSITION|RESYNC_DECISION|VERDICT_REF), schema_version, natural_identity, content_digest, connection_fence, direction, stream_key, channel_class, sequence, payload)`；**append-only**（无 UPDATE/DELETE）；**UNIQUE(tenant,fact_kind,natural_identity)**——同自然身份第二个不同 digest=FACT_DIGEST_CONFLICT。每 fact_kind 的 natural_identity 组成固定（如 FRAME_META=frameId、MESSAGE_RECEIVED=messageId、ACTION_CHECKPOINT=actionId+checkpointSeq）。
- causation_link`((tenant,link_id) PK, from_kind, from_id, to_kind, to_id, task_run_id)`；from/to 引用实际存在 immutable 行（事务内存在性断言）。

**Outbox**（权威/诊断分离）：
- outbox_envelope`((tenant,message_id) PK, action_id, message_type, channel_class, issued_connection_fence, issued_direction, issued_stream_key, issued_sequence, envelope_version, client_session_id, expiry, scope, payload_schema_version, signed_bytes(不可变原字节), payload_digest, target_device_id, state CHECK(PENDING|CLIENT_RECEIVED|OUTCOME_RECEIVED|CLOSED))`；**持久化不可变 signed_bytes，重投原样发**（不重建 envelope，防延长 expiry/改被签字节）；UNIQUE`(tenant,target_device,fence,direction,channel_class,stream_key,issued_sequence)`（禁两消息占同 sequence）；无 ENQUEUED 态。
- route_attempt`((tenant,attempt_id) PK, message_id, owner_instance_id, connection_fence, enqueued_at, result)`——诊断，可丢，可 Redis。

**权威移交**（单 ACTIVE writer，可实现版）：
- authority_current`((scope_kind CHECK(GLOBAL|TENANT), scope_ref) PK, writer_epoch, holder CHECK(CLOUD|ROLLBACK), updated_at)`——**单行可变，唯一 active writer**（避免 append-only+partial UNIQUE 在 PG 不可实现的组合）。GLOBAL 走 singleton（scope_ref='*'）。
- authority_epoch_event`((scope_kind, scope_ref, event_id) PK, writer_epoch, state CHECK(ACTIVE|SUPERSEDED|REVOKED), fact_at)`——append-only 历史。
- authority_transfer`((scope_kind, scope_ref, transfer_id) PK, source_epoch, target_epoch, state CHECK(PREPARED|COMMITTED|ABORTED), prepared_manifest_hash, committed_authority_revision, snapshot_hash)`；manifest 在 COMMITTED 后引用稳定 authority_revision+snapshot_hash 生成（非提交前假装知道的 LSN）。

**记忆**（owner/ACL + 幂等）：
- memory_canonical`((tenant,canonical_id) PK, owner_user_id, source CHECK(IMPORTED_LOCAL_BASELINE|RUNTIME), raw_payload, schema_version, content_digest, baseline_commit NOT NULL DEFAULT '∅')`；**UNIQUE(tenant,owner_user_id,source,baseline_commit,content_digest)**（含 owner；哨兵解 NULL 互不相等）。
- memory_version`((tenant,memory_version_id) PK, owner_user_id, canonical_id FK, parent_version_id FK 自引用, scope CHECK(PRIVATE|PUBLIC_CANDIDATE|PUBLIC_PUBLISHED), kind, context_key, context_digest, payload, payload_digest, state CHECK(ACTIVE|DEMOTED|QUARANTINED|RETIRED), provenance_source_user_id 受限)`；**UNIQUE(tenant,scope,owner_user_id,kind,context_digest,payload_digest)**；晋级=INSERT 新 immutable 行带 parent（无跨 scope UPDATE）；PRIVATE 行 RLS 附加 owner 谓词；PUBLIC 行 owner nullable + scope CHECK 建模 principal namespace。
- memory_use`((tenant,memory_use_id) PK, UNIQUE(tenant,memory_version_id,memory_use_id), 复合 FK 到 memory_version/action/task_run, context_revision, connection_fence, lease_epoch, before_frame_id, before_frame_hash, observation_budget_ms)`。
- memory_verdict`((tenant,memory_version_id,memory_use_id,verifier_rule_version) PK, 复合 FK 引用 memory_use 保证归属, verdict CHECK(SUCCESS|FAIL|INCONCLUSIVE), transition_proof(precondition/before/after frameId+hash), evidence_digest)`；append-only。
- memory_stats_projection`((tenant,memory_version_id) PK, weighted_success, weighted_fail, distinct_task_runs, score, score_policy_version)`——纯投影，可由 verdict 全量重建。

**资产/对象**：asset_descriptor`((tenant,asset_id,version) PK, content_hash, signing_key_id, status CHECK(ACTIVE|RETIRING|RETIRED|REVOKED), asset_epoch, revoked_at)`；upload_grant`((tenant,grant_id) PK, device_id, window_registration_id, window_incarnation, binding_generation, frame_id, capture_spec_digest, consumed, expiry)`（消费五+条件原子核验含 windowRegistration/incarnation/bindingGeneration）；object_metadata`((tenant,object_key) PK, kind, content_hash, size_bytes, retention_class, encryption_key_id, legal_hold, deleted_at)`（无 ref_count 列）；object_reference`((tenant,object_key,referrer_kind,referrer_id) PK)`（引用事实，refCount=投影，GC 事务内重核验零引用+legal_hold）。

**配置/审计**：quota_profile/slo_profile/score_policy/verifier_rule/execution_budget_table 均 `((name,version) PK, body, created_by)` 不可变版本化；audit_event`((tenant,audit_id) PK, category, principal, action, subject, detail)` append-only；evidence_manifest`((manifest_id) PK, release_identity(buildHash/allowlistHash/baseline commit/policy+asset+quotaProfile+normalizer version/环境/时间), entries(requirementId→evidence hash), signed_digest)`。

## 22. 事务清单

T0 请求幂等（含 digest 冲突）+ run 创建 ｜ T1' outcome digest 校验→action_ledger 推进→outbox_envelope 推进（PENDING→OUTCOME_RECEIVED 直接边同原子）→revision CAS→successor 记账→lease 决定→outstanding_action 删/插→causation 断言→protocol_fact ｜ T-receipt 五字段比对原 outbox→CLIENT_RECEIVED（幂等吸收）→protocol_fact ｜ T-grant 上传凭据+条件原子消费 ｜ T2 fence 换代（CAS+1+旧失效+lane REVOKING+禁旧路由）｜ T3 排空证明→lease_entity 换+epoch CAS+holder ｜ T4' verdict 插入→run-level 聚合→发布门→INSERT 新 PUBLISHED（UNIQUE 防双发布）→审计（同事务）｜ T5' authority_transfer PREPARED→{source SUPERSEDED；target ACTIVE(authority_current 单行 CAS)；COMMITTED+revision}→COMMITTED 后签 manifest ｜ T6 fresh admission。

## 23. UNKNOWN 迟到解除

action_ledger.state 保持 UNKNOWN 不覆盖；迟到可信 outcome 到达（核验原 actionId/plan digest/fence/lease/stream/签名）→ 若尚无 late-resolution 则原子写入第一份可信 outcome protocol_fact → late_resolution_fact_id 指向 + recovery_disposition=LATE_RESOLVED；后续同 digest 幂等、异 digest 冲突。原 UNKNOWN/tombstone 永久保留、不改写历史。（首次迟到 outcome_digest 为 NULL 也能解除——门是"可信原 action 匹配"而非"同已有 digest"。）

---

# 第五部分 · 组件、信任边界与核心时序（A-4）

## 24. 信任边界

T1 网络（本地一切输入=设备事实，业务解释权 0；跨界全量验签+fence）；T2 租户 RLS；T3 admin/maintenance 高危再认证。

## 25. 六核心时序（消息名对齐第三部分，表名对齐第四部分）

- **正常动作**：FRAME_REF→(T1' DISPATCHED+outbox PENDING)→ACTION_PLAN→MESSAGE_RECEIVED(CLIENT_RECEIVED)→本地校验+启动门+exclusive callback(每步 expiry/epoch 检)→ACTION_OUTCOME→(T1' 推进+revision+successor+lease)→ACK。
- **用户 stop**：本地立即 lane 拒新+按检查点停(STOPPED)→TASK_COMMAND(stop)→终态 canonical trigger=USER_STOP→taskRun STOPPED(非 FAILED)→INPUT_LANE_DRAINED→lane FREE。
- **断线重连**：CLOUD_SUSPENDED→bootstrap(T2 fence CAS+lane REVOKING)→LOCAL_LEDGER_RESET?+RESYNC_REPORT→对账(DISPATCHED 无 outcome→UNKNOWN)→CAPTURE_SPEC_W 完整帧→RESYNC_DECISION→首条副作用 plan 等 DRAINED/硬 expiry 屏障。
- **lease 换手**：正常=holder outcome(callback 退出+全步结束+lane 释放+三元绑定)构成排空证明→T3 epoch CAS→LEASE_GRANT；强制=REVOKE_LEASE→REVOKING+outstanding UNKNOWN→等 DRAINED 或 latestExpiry+skew→授新+forced-handoff 审计。
- **S7 原子切换**：关 admission→drain deadline→旧 plan 完成/取消/UNKNOWN+每设备 DRAINED→旧端冻结+最终增量导入(count+hash+schemaVersion+baseline 核对)→T5' authority COMMITTED→旧 fence/epoch 永久失效→停旧起新→bootstrap 互验→singleton lock+fence 双保险→逐窗口 fresh。dirty-drain 窗口不切换。
- **S8 回滚**：停 admission→fence/drain→云端快照+journal 封存只读→T5' transfer(cloud→SUPERSEDED, rollback→ACTIVE, COMMITTED+revision)→COMMITTED 后签 manifest→部署已验旧工件→旧系统凭 epoch manifest 恢复写权限→每窗口 fresh frame+rebind+hot-start→一律 fresh run+cutover journal 对账。任一步崩溃全系统停止不猜。

## 26. 时序↔零不变量

正常动作→同 actionId 物理执行≤1、critical 永不 THROTTLED；stop→永不翻 FAILED；重连→旧 fence 永不受理、UNKNOWN 永不推断 NOT_EXECUTED；换手→无双 holder、旧 epoch 不执行；切换→新旧永不同时输入；回滚→至多一个 ACTIVE writer epoch。

---

# 第六部分 · 认证、密钥与灾备（A-6 + A-7）

## 27. Principal/Role

USER(登录→短 token)、DEVICE(设备私钥,OS 密钥库/TPM)、ADMIN(USER+admin+高危再认证)、SERVICE(部署凭据)。Role：viewer/operator/admin；无任何角色可业务级跨租户读写。高危操作（强制发布/取消发布/quarantine/回滚/删除/trustedPublisher/资产 REVOKE/设备解绑/authority transfer/降低安全上限）=再认证+同事务审计(RPO=0)。

## 28. 设备与 token 生命周期

enroll(OS 密钥库生成密钥对,公钥注册)→bootstrap 私钥签 challenge→unbind(REVOKED+同事务吊销全 token+fence 强制换代)。短 access token(≤1h)+refresh 单次滚动(重放=吊销全链);撤销传播=强制 fence 换代断连;禁内置共享永久 token,禁 token 入日志/label。

## 29. 签名密钥三域分治

连接会话域(bootstrap 协商派生,绑 fence+算法,泄露半径=单连接单 fence);资产发布域(signing_key_id+keyring,轮换 ACTIVE→RETIRING(可验不可签)→RETIRED,compromise→REVOKED→该 keyId 资产走 REVOKED 强路径);部署域(TLS/DB/token 密钥专用 secret 管理,与数据备份分开托管)。upload grant 单次消费。

## 30. 灾备备份

PG WAL 连续归档+PITR;correctness 库同步 standby(延迟>0 告警,是 RPO=0 构成);对象存储版本化+与 object_metadata 每日 manifest 交叉核对;加密 key/cert 独立托管+自身恢复演练;每备份 checksum+恢复抽验。

## 31. 恢复后强制序列（任何恢复事件）

authority 校验(只读 PG 已提交;PREPARED 中断态→全系统停止人工裁决)→全局 fence epoch 强制换代→全设备 CLOUD_SUSPENDED→恢复点后所有 action 标 UNKNOWN(RPO=0 保证有台账可标;禁 NOT_EXECUTED 推断)→全 lane REVOKING→逐设备 bootstrap+LOCAL_LEDGER_RESET 对账→逐窗口 fresh frame+rebind+RESYNC→才重开 admission。无法对账窗口保持暂停。

## 32. RTO/演练

RTO：主 PG 切副本≤5min、PITR≤2h、对象区域故障≤4h(证据降级 verdict INCONCLUSIVE)、全量灾难≤24h。演练：备份抽验(每次)、副本切换/PITR/key 恢复/全量重建/回滚预演(S6 前各一次+周期)，证据入 evidence_manifest。`impl-tuning-only`：RTO 具体数值压测后冻结。

## 33. 图片保留

普通决策图 30min、成功调用长期留 hash/结果/延迟摘要、失败证据 7d、公共候选 ROI 30d、已发布留最小 crop、手动 pin 到管理员删除。删除后保留 evidence_digest+frame metadata+verdict(锚点不悬空)。

---

# 第七部分 · 容量、背压与隔离（Q5）

## 34. 分层预算与背压

`global→tenant→user→device→window` 分层 admission/token budget，同时限 encoded bytes/s、decoded pixels/s、full-frame/s、in-flight decoded/native 内存、对象写带宽、并发作业；饱和先拒新 taskRun/CaptureSpec，保既有 headroom。背压=显式 THROTTLED(带 retryAfterMs≤上限+reason，仅作用未执行 CaptureSpec/上传/可重采事实，永不作用 CRITICAL)；解码前按字节拒大图，upload grant+native 信号量+可取消解码防 decode bomb。

## 35. 公平与故障

class pool(前台小 ROI/重视觉/后台学习)+per-user/tenant in-flight cap+成本感知 deficit round-robin+deadline 真取消释放 native；后台不占前台保留槽。三存储故障=可用性事件永不正确性事件：Redis 丢只停新 admission(活 WS 是受信活性,不伪造失联)、PG 故障停 dispatch(唯一停摆开关,禁"先发后补账")、对象故障 verdict INCONCLUSIVE。fenced delivery：enqueue≠delivered，仅签名 MESSAGE_RECEIVED 或同 digest outcome 才 delivered。

## 36. 容量数值

10 用户样例的分层配额、SLO profile 数值由 staging 压测校准后随 quotaProfile 冻结（`impl-tuning-only`）；负载矩阵三零标准：零静默丢消息、零跨租户串用、零过载业务 FAIL/本地 fallback。

---

# 第八部分 · 建设、切换、验收（Q6 + A-5）

## 37. 建设阶段（顺序，非生产开关）

S0 冻结盘点(迁移矩阵完备)→S1 协议+基础层(现有休眠原语/SessionStore/拆分构建 profile 为种子)→S2 业务 Service 迁移(叶序,挂矩阵行+R0 等价证据)∥S3 视觉/记忆/资产→S4 云端 task turn+多窗口→S5 管理 API/可观测/安全/容量(staging 建设,quotaProfile 产出)→S6 全链 shadow/replay 验证→S8 回滚就绪(S7 前置)→S7 原子切换→S9 旧代码删除。

## 38. S6 双模验证

Replay(帧回放+决策流机械比对,抓逻辑分歧)+Shadow(同帧 tee,SHADOW realm 独立 tenant/namespace,服务端禁签执行 plan/禁 lease/禁学习,客户端硬拒,只记录,抓时序/环境分歧)。**零未解释分歧**：每条=bug 或用户裁决差异；未覆盖=NOT_EVALUATED=阻塞切换。A/B/C/D 四档证据(按可观察影响,非类名)。

## 39. 主验收矩阵（requirementId 追踪）

全部硬边界(§2-§10)+Q1-Q7 门+终审跨章节决定，每行 requirementId(稳定)+owner+环境+证据类型+状态(NOT_EVALUATED|BLOCKED|PASS|APPROVED_DIFFERENCE，后者链接用户裁决)+依赖。证据绑不可变 release identity；依赖变化自动使受影响行回 NOT_EVALUATED。REQ-M-*(方法级)由第二部分底账注入。

**零不变量持续监控**（恒为零，非零=自动遏制+page）：同 actionId 物理执行>1、违规铸新 actionId、旧/错 fence 受理、错 window/binding 执行、CRITICAL 被 THROTTLED、跨租户读取、shadow 物理执行。趋势指标：UNKNOWN 率、transport redelivery 率、RESYNC 率。

## 40. 真人真机验收集（HUMAN，只有真机能暴露的）

真实窗口绑定+首次输入原子性；≥2 窗口竞争 InputLane(focus/切换/原子 move+click/DPI/错 binding 拒绝)；每任务族≥1 条完整 fresh run(队长/队员/单号物理差异)；客户端/服务端各一次真实重启后 rebind/hot-start/RESYNC；pause/stop/emergency；断云拔线；切换日 S7 放行签字。其余由 staging 自动化。

## 41. S9 删除门

allowlist 物理生效+矩阵行逐行核销+反向扫描零命中；稳定门(默认 7 天无 P0/P1/回滚、每业务类型≥20 成功 taskRun、≥3 fresh 启动、覆盖多窗口/pause-stop/可恢复故障各一次、restore drill 通过)；删除清单用户批准；删除前不可变 tag+archive manifest/SBOM/checksum，旧工件+证据归档永存。降低门槛须用户批准。

---

# 第九部分 · 仍需用户拍板 + 未尽事项

## 42. 待用户裁决（少量真产品决策）

| 决策 | 最迟阶段 |
|---|---|
| S9 删除门槛数值确认 | S9 前 |
| SLO/quotaProfile 冻结数值批准 | S6 门 |
| 真人真机集执行安排 | S6-S7 间 |
| 切换日 S7 最终放行签字 | S7 |
| 迁移矩阵产出后的删除清单批准 | S9 |
| 设计工件提交分支 + evidence_manifest 签发 | Final Proposed 时 |

除上述无其他未决产品决策；实现层数值由 SLO/quota/evidence 门产生，降低门槛仍须批准。

## 43. 当前未尽（阻塞 Final Proposed / 切换）

- **A-1 门 1-5**（设计期须完成）：方法级 inventory 全覆盖(ASM 闭包)、production-reachability 闭包、反向扫描全命中映射、配置资源完整归属、每行客观验收条件——当前 PARTIAL，需再补一轮。
- **A-1 门 6a/6b**（实现后产生）：allowlist 构建证据、fresh runtime 人工抽查——A-5 保持 NOT_EVALUATED，按 Q7 阻塞 S6/S7。
- **evidence_manifest 签发**：需绑不可变 release identity（设计工件冻结 commit），待提交分支确定后签发。

**结论**：本文已把目标边界、协议、状态/数据模型、组件时序、认证/密钥/灾备、容量、建设/切换/验收整合为自包含单文档，安全/身份/状态/重放/租户/窗口/lane 约束全部设计期定死。距 Final Proposed 仅剩 A-1 门 1-5 的机械化补全；距切换另需实现期的 allowlist/fresh runtime 证据（按 Q7 正确阻塞，不阻文档定稿）。
