# A-4 组件、信任边界与核心时序（THIN_CLIENT_V1）

工件编号：A-4（终审 Final #1 工件计划）
来源共识：草案 §4/§6/§7、Q2/Q3/Q5/Q6 收口、A-2 v2、A-3 v2
状态：设计工件 v1
约束：设计级。时序中的消息名/状态名与 A-2 v2、表名与 A-3 v2 一一对应。

---

## 1. 组件与信任边界图

```
┌─ 用户设备（不受信执行环境，只被授予"执行与拒绝"权）───────────────────┐
│                                                                        │
│  DesktopUI ── WindowRegistry ──┐                                       │
│      │             │           │ HWND 事实权威（compare-only 上送）      │
│      │      CaptureExecutor ───┤                                       │
│      │      ObservationExecutor┤                                       │
│      │      GenericMatchExecutor┤        LocalActionLedger(易失,fence 内)│
│      │             │           │              │                        │
│      └── LocalSafetyGate ══════╪══ InputLane（物理串行,唯一输入出口）    │
│                    │           │                                       │
│              ActionPlanExecutor┘        SignedAssetCache(hash 验签)     │
│                    │                                                   │
│              CloudConnection（bootstrap→FENCED; binary frame 验签）     │
└────────────────────┼───────────────────────────────────────────────────┘
                     │ TLS + 设备签名 + fence     【信任边界 T1：网络】
┌─ Cloud Brain（唯一业务权威）────────────────────────────────────────────┐
│  Gateway（验签/scope/配额/upload grant） ── ConnectionRegistry(Redis)    │
│      │                                                                 │
│  Session/TaskRun StateStore ══ PG（correctness, RPO=0）                 │
│  Task Orchestrator ── 业务 Service 图（§3.4 原边界整体迁入）              │
│  CloudTaskTurnScheduler（lease 权威） ── Outbox Dispatcher（fenced）     │
│  Vision WorkerPool（class pool + DRR）   MemoryService（三池+verifier）  │
│  AdminAPI（RBAC+再认证）【信任边界 T2：租户 RLS】【T3：admin/维护通道】    │
└────────────────────────────────────────────────────────────────────────┘
```

信任规则：T1 内(本地)一切输入视为"设备事实"，业务解释权 0；跨 T1 消息全量验签+fence；T2 由 A-3 §0 RLS 强制；T3 高危操作再认证（A-6）。

## 2. 时序一：正常动作循环（决策→执行→回报）

```
本地                          云端
 │── FRAME_REF(frameId,hash) ──▶│ protocol_fact(FRAME_META)
 │                              │ 业务 Service 决策（读 PG 状态+记忆+资产）
 │                              │ T1'事务: action_ledger(DISPATCHED,全景快照)
 │                              │          + outbox(PENDING) 同 commit
 │◀─ ACTION_PLAN(签名,栅栏全字段)─│ Dispatcher 按 fence 路由
 │ MESSAGE_RECEIVED ───────────▶│ T-receipt: outbox→CLIENT_RECEIVED
 │ LocalSafetyGate 整批校验      │
 │ 启动门: declared≥derived      │
 │   && now+derived≤expiry      │
 │ InputLane exclusive callback  │
 │  (每步: expiry+leaseEpoch 检) │
 │── ACTION_OUTCOME(EXECUTED) ──▶│ T1'事务: ledger 推进+outbox→OUTCOME_RECEIVED
 │                              │   +revision CAS+successor+lease 决定
 │◀─ ACTION_OUTCOME_ACK(+successor)│
```

## 3. 时序二：用户 stop（本地先行，永不 FAILED）

```
用户 stop → 本地立即: InputLane 拒新请求; 执行中 plan 按检查点停 → STOPPED
 │── TASK_COMMAND(stop,requestId) ─▶│ T0: request_ledger 幂等
 │── ACTION_OUTCOME(STOPPED,steps) ─▶│ ledger 推进; 终态转换 canonical trigger=USER_STOP
 │◀─ TASK_COMMAND_ACK ──────────────│ taskRun→STOPPED(非 FAILED; Q7 零不变量监控)
 │── INPUT_LANE_DRAINED ────────────▶│ lane REVOKING→FREE
```

## 4. 时序三：断线重连（CLOUD_SUSPENDED→RESYNC）

```
WS 断 → 本地: 停新业务输入; 保留绑定/ledger/frame 引用; 后台探测
重连: BOOTSTRAP HELLO → 云端 T2 事务(fence CAS+1, 旧 fence 失效, lane REVOKING)
     → FENCE_GRANT → FENCE_ACK(安装最高 fence)
 │── LOCAL_LEDGER_RESET? + RESYNC_REPORT(最后 seq/action/事实/lane) ─▶│
 │                              │ 对账: ledger diff; DISPATCHED 无 outcome→UNKNOWN
 │◀─ CAPTURE_SPEC_W(完整帧) ────│
 │── FRAME_REF_W ──────────────▶│ 观察闭合业务不确定性(台账 UNKNOWN 不改写)
 │◀─ RESYNC_DECISION(RESUME/RESYNC/RESTART/END) │
 │  首条有副作用 plan 等: DRAINED 或 latestExpiry+skew（§9.3 屏障）
```

## 5. 时序四：lease 换手（正常 + 强制）

```
正常: W1 outcome(callback 退出+全步结束+lane 释放+三元绑定) 构成排空证明
     → 云端 T3: lease CAS epoch+1, holder=W2 → LEASE_GRANT(W2, epoch+1)
     → 本地 InputLane 记最高 epoch; W1 旧 epoch 请求一律拒

强制(fence 丢/stop/重启):
     REVOKE_LEASE → lane REVOKING; outstanding→UNKNOWN
     → 等 INPUT_LANE_DRAINED, 或 latestExpiry+CLOCK_SKEW_SAFETY_MS 硬边界
     → 才 CAS 授新 holder; 全程记 forced-handoff 审计
```

## 6. 时序五：THIN_CLIENT_V1 原子切换（Q6 S7）

```
关新 taskRun admission → 有界 drain deadline
→ 全部旧 plan 完成/取消/UNKNOWN; 每设备 INPUT_LANE_DRAINED
→ 旧端冻结 → 最终增量导入(cutover manifest: count+hash+schemaVersion+baseline commit 逐项核对)
→ 全对 → T5': authority_transfer PREPARED→COMMITTED; cloud writer epoch ACTIVE
→ 旧 connectionFence/协议 epoch 永久失效; 停旧进程
→ 新版本启动 → bootstrap 互验(protocolVersion/buildHash/allowlistHash/policy+asset epoch)
→ 设备 singleton lock + 服务端 fence 双保险 → 逐窗口 fresh 启动
※ drain 不干净的窗口不切换,停住人工处置; 任何核对不符→中止切换
```

## 7. 时序六：整体回滚（Q6 S8 + Final #6/B Final#2 P1-3）

```
停云端 admission → fence/drain 全部 lane → 云端权威快照+cutover journal 封存(只读,checksum)
→ T5': transfer PREPARED(manifest hash) → PG 事务{cloud epoch→SUPERSEDED; rollback epoch ACTIVE; COMMITTED+LSN}
→ COMMITTED 后签发 ACTIVE manifest(仅含已提交 LSN/snapshot hash/epochs/transferId)
→ 部署已验 checksum 的旧工件 → 旧系统启动核验 epoch manifest → 仅持有者恢复写权限
→ 每窗口: fresh full frame + 身份 rebind + 旧系统 hot-start 检查 → 一律 fresh run
→ cutover journal 交用户重排任务计划; 无法对账窗口保持暂停
※ 任一步崩溃→全系统保持停止,不回退猜测; 预输入健康门失败自动 abort
```

## 8. 时序与不变量的映射

| 时序 | 守护的零不变量（Q7） |
|---|---|
| 一 | 同 actionId 物理执行 ≤1；critical 通道永不 THROTTLED |
| 二 | stop 因果链永不翻译为 FAILED |
| 三 | 旧 fence 永不被接受；UNKNOWN 永不推断为 NOT_EXECUTED |
| 四 | 换手期间无双 holder；旧 epoch 计划不执行 |
| 五 | 新旧客户端永不同时输入 |
| 六 | 任何时刻至多一个 ACTIVE writer epoch |
