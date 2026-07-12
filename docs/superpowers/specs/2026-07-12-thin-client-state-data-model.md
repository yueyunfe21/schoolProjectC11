# A-3 权威状态与数据模型（THIN_CLIENT_V1）

工件编号：A-3（终审 Final #1 工件计划）
来源共识：草案 §8/§9、Q3/Q4、Final #3/#6/#8、B Final #2、B Final #4、B Final #6（A-3 v2 复审 8P1+3P2）
状态：设计工件 v3（**自包含**：全表列/复合键/唯一约束/状态/事务边界均在本文，不引用被覆盖草稿）
约束：设计级，列类型为语义类型。

---

## 0. 全局强制约束

- **复合键规约**：所有租户内实体 PK/FK 形如 `(tenant_id, <id>)`；跨表引用一律复合 FK——DB 层不可能跨租户引用。
- **RLS 与 tenant context**：全部租户表启用 RLS，策略读 `current_setting('app.tenant_id')`。该值由**连接认证后经受信入口设置为 transaction-local**（`SET LOCAL`），业务 SQL/payload 无法改写；应用连接使用受 RLS 的角色（`app_rw`），无 `BYPASSRLS`。跨租户仅两条受控路径：`admin_ops`（RLS + 强制审计触发，限本租户管理域）、`maintenance`（离线维护通道，只读/归档，禁业务写，全审计）。平台无任何角色可业务级跨租户读写。
- **enum/迁转**：所有 status/state 列 CHECK；合法迁转由事务内断言维护，非法迁转=事务失败。

## 1. 数据分级（与 A-7 严格一致）

| 级别 | 数据 | RPO |
|---|---|---|
| correctness | action_ledger、protocol_fact、outbox_envelope、connection_fence、input_lane_lease、lease_entity、task_run.revision、authority_epoch/transfer、outstanding_action、绑定 correctness 迁转的 audit | **0** |
| durable-audit | 独立运营审计 | ≤5min |
| evidence | 帧/ROI/性能 trace | §12 批准值 |

## 2. 身份与连接

- **tenant** `(tenant_id) PK, name, status CHECK, created_at`
- **app_user** `(tenant_id, user_id) PK, login_identity, status, trusted_publisher BOOL, created_at`；UNIQUE `(tenant_id, login_identity)`
- **device** `(tenant_id, device_id) PK, FK(tenant_id,user_id), public_key, enrollment_status CHECK(ENROLLED|REVOKED), enrolled_at, revoked_at`
- **client_session** `(tenant_id, client_session_id) PK, FK(tenant_id,device_id), started_at, ended_at`
- **connection_fence** `(tenant_id, device_id) PK+FK, generation BIGINT, updated_at`；CAS `WHERE generation=:expected`；单调永不复用

## 3. 窗口、任务、请求幂等

- **window_registration** `(tenant_id, window_registration_id) PK, FK(tenant_id,device_id), window_id, incarnation BIGINT, last_confirmed_hwnd, geometry, dpi_scale, coordinate_space, binding_generation, status CHECK(ACTIVE|GONE), registered_at`；UNIQUE `(tenant_id,device_id,window_id,incarnation)`
- **task_run** `(tenant_id, task_run_id) PK, FK(tenant_id,user_id), FK(tenant_id,device_id), FK(tenant_id,window_registration_id), task_type, status CHECK, revision BIGINT, state_snapshot JSONB, policy_revision, score_policy_version, asset_version_set_digest, quota_profile_version, created_at, updated_at, terminal_at`
  - `task_run_id`=H(tenant|device|windowRegistrationId#incarnation|requestId)（幂等锚，无独立 session 公式）
  - **partial UNIQUE `(tenant_id, window_registration_id) WHERE status NOT IN (终态集)`**——一窗口至多一活跃 run（B Final#6 P2-3）
- **request_ledger** `(tenant_id, device_id, request_id) PK, request_kind CHECK(START_TASK|TASK_COMMAND), request_payload_digest, request_schema_version, FK(tenant_id,window_registration_id) NULL, target_task_run_id NULL, command NULL, response_digest, response_snapshot, created_at`（B Final#6 P1-3）
  - CHECK：START_TASK 必有 window_registration_id、无 command；TASK_COMMAND 必有 target_task_run_id + command。
  - 幂等：命中 PK 时**先比 request_payload_digest**——相同才重放 response_snapshot；不同 digest 返回 `REQUEST_CONFLICT`，绝不静默重放旧结果。

## 4. 输入权、动作、单飞（correctness 核心）

- **lease_entity**（可历史引用的 lease 身份，B Final#6 P1-6）`(tenant_id, lease_id) PK, device_id, lane_id, granted_epoch, granted_at, released_at NULL`
- **input_lane_lease**（当前态，每 lane 一行）`(tenant_id, device_id, lane_id) PK, current_lease_id FK(tenant_id,lease_id) NULL, state CHECK(FREE|HELD|REVOKING), holder_window_registration_id NULL, holder_task_run_id NULL, holder_client_session_id NULL, holder_connection_fence NULL, lease_epoch BIGINT, updated_at`
- **outstanding_action**（单飞的 DB 强制，B Final#6 P1-6）`(tenant_id, device_id, lane_id) PK, action_id FK, since`
  - PK=每 lane 至多一条 outstanding——**数据库层面**禁止同 lane 两条未关闭 DISPATCHED action（不再靠冗余 partial unique）。action 关闭时删除该行。
- **action_ledger** `(tenant_id, action_id) PK, FK(tenant_id,task_run_id), FK(tenant_id,device_id), FK(tenant_id,window_registration_id), window_incarnation, binding_generation, input_lane_id, lease_id FK(tenant_id,lease_id), lease_epoch, connection_fence, plan_kind, message_id, payload_digest, frame_basis_kind CHECK(FRAME|NO_FRAME), based_on_frame_id NULL, expected_frame_hash NULL, frame_capture_marker NULL, max_frame_age NULL, no_frame_reason NULL, expected_geometry, expected_dpi_scale, coordinate_space, policy_version, asset_versions_digest, declared_budget_ms, state CHECK(DISPATCHED|NOT_EXECUTED|EXECUTED|OBSERVED|UNKNOWN|STOPPED), physical_input_started BOOL, completed_steps INT, outcome_digest NULL, successor_action_id FK(tenant_id,action_id) NULL, recovery_disposition NULL, late_resolution_fact_id NULL, dispatched_at, closed_at NULL, tombstone BOOL`
  - CHECK：frame_basis_kind=FRAME 则 based_on_frame_id/expected_frame_hash/frame_capture_marker/max_frame_age 必填；=NO_FRAME 则 no_frame_reason ∈ allowlist（B Final#6 P1-6 补 stale-frame 字段）
  - successor_action_id 自引用 FK；UNIQUE `(tenant_id,message_id)`
  - 索引：`(tenant_id,task_run_id,dispatched_at)`、`(tenant_id,state) WHERE closed_at IS NULL`

## 5. 协议事实账本（B Final#6 P1-4：冲突拒绝 + 完整因果链）

**protocol_fact**（append-only；无 UPDATE/DELETE 权限，仅保留期归档）
`(tenant_id, fact_id) PK, fact_kind CHECK(...), schema_version, natural_identity, content_digest, connection_fence, direction NULL, stream_key NULL, channel_class NULL, sequence NULL, payload JSONB, created_at`

- fact_kind 全集（补齐 C2S/S2C 全 envelope）：`CONTROL_ENVELOPE_C2S | CONTROL_ENVELOPE_S2C | ACTION_PLAN | ACTION_OUTCOME | FRAME_META | WINDOW_BINDING | CAPTURE_SPEC | OBSERVATION_PLAN | OBSERVATION_FACT | MESSAGE_RECEIVED | ACTION_CHECKPOINT | LEASE_TRANSITION | FENCE_TRANSITION | RESYNC_DECISION | VERDICT_REF`
- **UNIQUE `(tenant_id, fact_kind, natural_identity)`**（不含 digest）——natural_identity=该事实的自然主键（如 frameId、messageId、actionId+checkpointSeq）；同一自然身份**第二个不同 content_digest 插入=冲突拒绝** `FACT_DIGEST_CONFLICT`（B Final#6 P1-4：不再允许两条冲突 FRAME_META/receipt 并存）
- 覆盖全部 C2S/S2C envelope 的 fence/stream/sequence，支撑 `capture→plan→receipt→outcome→observation→verdict` 100% 闭包

**causation_link** `(tenant_id, link_id) PK, from_kind, from_id, to_kind, to_id, FK(tenant_id,task_run_id), created_at`；from/to 引用实际存在 immutable 行（事务内存在性断言）

## 6. Outbox（B Final#6 P1-5：保存原 envelope 全字段）

**outbox_envelope**（PG 权威，durable）
`(tenant_id, message_id) PK, action_id NULL, message_type, channel_class, issued_connection_fence, issued_direction, issued_stream_key, issued_sequence, payload_bytes, payload_digest, target_device_id, state CHECK(PENDING|CLIENT_RECEIVED|OUTCOME_RECEIVED|CLOSED), created_at, received_at NULL, closed_at NULL`
- 保存原 {fence, direction, stream_key, sequence, messageId, digest}——T-receipt 才能把 MESSAGE_RECEIVED 五字段与原签发 envelope **全量比对**（B Final#6 P1-5）
- 无 ENQUEUED 态

**route_attempt**（诊断，可丢，可 Redis）`(tenant_id, attempt_id) PK, message_id, owner_instance_id, connection_fence, enqueued_at, result`（补 tenant_id，B Final#6 P1-2）

## 7. 权威移交（B Final#6 P1-2 补 scope 语义 + P2-1 append-only 修正）

**authority_epoch**（身份行不可删除；状态迁转经 append-only fact，B Final#6 P2-1）
`(scope_kind CHECK(GLOBAL|TENANT), scope_ref, writer_epoch) PK, holder CHECK(CLOUD|ROLLBACK), created_at`
- scope_kind=GLOBAL（切换/回滚是平台级 singleton）时 scope_ref='*'，并有独立 global-singleton 断言；TENANT 级则 scope_ref=tenant_id。
- 状态不在本表 UPDATE；当前 ACTIVE 由 **authority_epoch_state**（append-only fact）最新行推导：
  `(scope_kind, scope_ref, epoch_state_id) PK, writer_epoch, state CHECK(ACTIVE|SUPERSEDED|REVOKED), fact_at`
  - **partial UNIQUE `(scope_kind, scope_ref) WHERE state='ACTIVE'` 由物化投影维护**——每 scope 至多一个 ACTIVE writer。

**authority_transfer** `(scope_kind, scope_ref, transfer_id) PK, source_epoch, target_epoch, state CHECK(PREPARED|COMMITTED|ABORTED), prepared_manifest_hash, committed_authority_revision NULL, snapshot_hash NULL, prepared_at, committed_at`
- `committed_authority_revision`（稳定 revision，非"提交前假装知道的 LSN"，B Final#6 P2-1）；manifest 在 COMMITTED 后引用该 revision + snapshot_hash 生成可验证投影。

## 8. 记忆（B Final#6 P1-7：owner/ACL + 幂等键修正）

**memory_canonical** `(tenant_id, canonical_id) PK, owner_user_id, source CHECK(IMPORTED_LOCAL_BASELINE|RUNTIME), raw_payload JSONB, schema_version, content_digest, baseline_commit NOT NULL DEFAULT '∅', imported_at`
- **UNIQUE `(tenant_id, owner_user_id, source, baseline_commit, content_digest)`**——含 owner_user_id（B Final#6 P1-7：同租户两用户相同内容不冲突）；baseline_commit NOT NULL 用哨兵 '∅' 替代 NULL（避免 PG "NULL 互不相等"致 RUNTIME 重投重复插入）

**memory_version** `(tenant_id, memory_version_id) PK, owner_user_id, canonical_id FK NULL, parent_version_id FK(tenant_id,memory_version_id) NULL, scope CHECK(PRIVATE|PUBLIC_CANDIDATE|PUBLIC_PUBLISHED), kind, context_key JSONB, context_digest, payload JSONB, payload_digest, state CHECK(ACTIVE|DEMOTED|QUARANTINED|RETIRED), provenance_source_user_id(受限列), created_at`
- **UNIQUE `(tenant_id, scope, owner_user_id, kind, context_digest, payload_digest)`**——PRIVATE 含 owner，两用户互不冲突；PUBLIC 行 owner_user_id 用发布命名空间常量
- **晋级=INSERT 新 immutable 行带 parent lineage**（删除"原行改 scope"表述，B Final#6 P1-7）；无跨 scope 的 UPDATE
- ACL：PRIVATE 行只该 owner 可读用（RLS 附加 owner 谓词）；provenance_source_user_id 服务端受限，公共消费者不可见

**memory_use** `(tenant_id, memory_use_id) PK, FK(tenant_id,memory_version_id), context_revision, FK(tenant_id,action_id), task_run_id, connection_fence, lease_epoch, before_frame_id, before_frame_hash, observation_budget_ms, created_at`

**memory_verdict** `(tenant_id, memory_version_id, memory_use_id, verifier_rule_version) PK, verdict CHECK(SUCCESS|FAIL|INCONCLUSIVE), transition_proof JSONB(precondition/before/after frameId+hash), evidence_digest, created_at`（append-only）

**memory_stats_projection** `(tenant_id, memory_version_id) PK, weighted_success, weighted_fail, distinct_task_runs, score, score_policy_version, last_recomputed_at`（纯投影；重建走 §11 离线通道）

## 9. 资产与对象（B Final#6 P2-2：引用关系可重建）

**asset_descriptor** `(tenant_id, asset_id, version) PK, content_hash, schema, context, signing_key_id, status CHECK(ACTIVE|RETIRING|RETIRED|REVOKED), asset_epoch, revoked_at NULL`
**upload_grant** `(tenant_id, grant_id) PK, device_id, window_id, frame_id, capture_spec_digest, consumed BOOL, expiry, created_at`；五条件原子消费（§10 T-grant）
**object_metadata** `(tenant_id, object_key) PK, kind CHECK(FRAME|EVIDENCE|ASSET|PINNED|ARCHIVE), content_hash, size_bytes, retention_class, encryption_key_id, legal_hold BOOL, created_at, deleted_at NULL`（无 ref_count 列）
**object_reference**（引用事实，B Final#6 P2-2）`(tenant_id, object_key, referrer_kind, referrer_id) PK, created_at`
- refCount = `object_reference` 按 object_key 计数的**投影**，可从事实全量重建；引用增减=INSERT/删除 reference 行（与 verdict/asset/frame 写入同事务）；GC 只在投影计数=0 且非 legal_hold 且过保留期时删对象，保留 object_metadata 行（digest 锚点）

## 10. 事务清单

| Tx | 内容 |
|---|---|
| T0 | request_ledger 幂等（含 digest 冲突判定）→ 创建/复用 task_run → 初始 revision/lease 需求/首 action/outbox_envelope → 存原响应 |
| T1' | outcome digest 校验 → action_ledger 推进 → outbox_envelope 推进（PENDING→OUTCOME_RECEIVED 直接边同原子）→ task_run.revision CAS → successor 记账 → lease 决定 → outstanding_action 删/插 → causation 断言 → protocol_fact(ACTION_OUTCOME/CHECKPOINT) |
| T-receipt | MESSAGE_RECEIVED 五字段 vs outbox_envelope 原字段全量比对 → PENDING→CLIENT_RECEIVED（幂等吸收）→ protocol_fact(MESSAGE_RECEIVED) |
| T-grant | upload_grant 五条件原子 `WHERE consumed=false AND device_id=… AND frame_id=… AND capture_spec_digest=… AND expiry>now()` |
| T2 | connection_fence CAS+1 + 旧 fence 失效 + 相关 lane REVOKING + 禁旧 fence 路由 |
| T3 | 排空证明核验 → lease_entity 释放旧+签发新 → input_lane_lease epoch CAS + holder 变更 |
| T4' | memory_verdict 插入（PK 防重）→ run-level 聚合（distinct taskRun=verdict 行 DISTINCT）→ 发布门 → INSERT 新 PUBLISHED 版本（UNIQUE 防并发双发布）→ audit（同事务）|
| T5' | authority_transfer PREPARED → {源 epoch_state SUPERSEDED；目标 epoch_state ACTIVE（partial unique 投影维护）；transfer COMMITTED + authority_revision} → COMMITTED 后签发 manifest |
| T6 | LOCAL_LEDGER_RESET 对账完成 → lane REVOKING→FREE → 重开该设备 dispatch |

## 11. UNKNOWN 迟到解除（B Final#6 P1-8：与 Q3 语义统一）

定案（保留旧证据不覆盖历史）：
- action_ledger.state 保持 **UNKNOWN 不被 UPDATE 覆盖**；
- 迟到的**可信同 outcome_digest** ACTION_OUTCOME 到达 → 追加 immutable `protocol_fact(ACTION_OUTCOME, late-resolution)` → action_ledger.`late_resolution_fact_id` 指向它 + `recovery_disposition=LATE_RESOLVED`；
- 业务不确定性据该 late fact 解除，但原 UNKNOWN 历史与 tombstone 永久保留、可审计。
- 不同 digest 的迟到 outcome = 冲突拒绝，保持 UNKNOWN。

## 12. 在线/离线路径分离

在线全部 O(1) 键定位无扫描；离线维护（projection 重建、归档、GC）走 maintenance 角色**有界分片扫描**（cursor/checkpoint 持久化 + 每片上限 + 限速 + 独立连接池）。

## 13. 唯一约束与索引清单（B Final#6 P2-3 完整化）

| 表 | 约束/索引 |
|---|---|
| app_user | UNIQUE(tenant,login_identity) |
| window_registration | UNIQUE(tenant,device,window_id,incarnation) |
| task_run | partial UNIQUE 活跃 run per (tenant,window_registration) |
| request_ledger | PK(tenant,device,request_id) + digest 冲突判定 |
| lease_entity | PK(tenant,lease_id)；action_ledger.lease_id FK 引用 |
| outstanding_action | PK(tenant,device,lane_id) 强制单飞 |
| action_ledger | UNIQUE(tenant,message_id)；idx(task_run,dispatched_at)；idx(state) partial；successor 自引用 FK；frame_basis CHECK |
| protocol_fact | UNIQUE(tenant,fact_kind,natural_identity) 冲突拒绝；idx(subject) |
| outbox_envelope | PK(tenant,message_id)；idx(state,created_at) WHERE PENDING；idx(target_device,issued_connection_fence) |
| authority_epoch_state | partial UNIQUE(scope_kind,scope_ref) WHERE ACTIVE |
| memory_canonical | UNIQUE(tenant,owner_user_id,source,baseline_commit,content_digest) |
| memory_version | UNIQUE(tenant,scope,owner_user_id,kind,context_digest,payload_digest)；parent 自引用 FK |
| memory_verdict | PK(tenant,version,use,ruleVersion) |
| object_reference | PK(tenant,object_key,referrer_kind,referrer_id) |
| 全 FK | 复合 (tenant_id,·)，均建索引（下列常用复合索引显式登记）：request(tenant,device,created_at)、outbox(tenant,target_device,state)、action(tenant,device,input_lane_id) |

## 14. Redis 键空间 / 对象存储布局

（键与前缀同设计意图；此处显式列出，非"同 v1"）
- Redis：`route:{t}:{d}`、`presence:{d}`、`dedup:{fence}:{class}:{stream}`、`leasehot:{d}:{lane}`、`sess:{sessionId}`——全可丢；投递仍以 PG fence 校验。
- 对象存储前缀：`frames/`、`evidence/fail/`、`evidence/memory/`、`assets/{contentHash}`、`pinned/`、`archive/cutover/{transferId}/`；每对象一行 object_metadata + 引用行 object_reference。
