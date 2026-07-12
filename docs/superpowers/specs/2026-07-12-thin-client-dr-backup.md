# A-7 权威数据灾备与恢复（THIN_CLIENT_V1）

工件编号：A-7（终审 Final #1 工件计划）
来源共识：草案 §12.6（Q6 回滚/S8/S9）、Final #6/#8、B Final #2 P1-3/P1-4、A-3 v2 §1/§6
状态：设计工件 v1
约束：设计级规格。原则：**任何恢复路径都不得从陈旧快照直接续跑执行**。

---

## 1. 灾备分级（与 A-3 v2 §1 严格一致）

| 级别 | 数据 | RPO | 手段 |
|---|---|---|---|
| correctness | action_ledger、protocol_fact、connection_fence、input_lane_lease、outbox、task_run.revision、tombstone、authority_epoch/transfer、绑定 correctness 迁转的审计 | **0** | 同步耐久：`synchronous_commit=on` + 同步 standby（quorum ≥1）；V1 单节点开发期替代=同主机独立介质同步 WAL 镜像 + 生产前必须升级为真同步副本，此项为 S7 切换前置门 |
| durable-audit | 独立运营审计 | ≤5min | WAL 连续归档 + PITR |
| evidence | 帧/ROI/性能 trace | §10 批准保留期 | 对象存储版本化 + 定期 manifest 对账 |

**correctness RPO=0 不可达时的唯一合法姿态**：全局停机 + 潜在丢失窗口人工外部对账（对照游戏世界实况与 cutover journal），禁止自动 re-admission（B Final#2 P1-4）。

## 2. 备份策略

| 项 | 策略 |
|---|---|
| PG 基础备份 | 每日全量 + WAL 连续归档（PITR 粒度到事务） |
| 同步副本 | correctness 库同步 standby；副本延迟>0 即告警（它不是异步灾备,是 RPO=0 的构成部分） |
| 对象存储 | 版本化开启；object_metadata（A-3 §7）与实际对象每日 manifest 交叉核对，差异=审计事件 |
| 加密 key/cert | 与数据备份**分开托管**（A-6 §4.3）；key 托管自身有恢复演练——数据可恢复而 key 不可恢复=数据丢失，等同灾难 |
| 备份完整性 | 每次备份带 checksum + 恢复抽验（不是只备不验） |

## 3. RTO 目标

| 场景 | RTO 目标 | 说明 |
|---|---|---|
| 主 PG 故障切副本 | ≤5min | 同步副本提升；恢复序列 §4 照跑（副本提升也算恢复事件,fence 换代不豁免） |
| PITR 整库回滚 | ≤2h | 仅 durable-audit/evidence 级损坏场景;correctness 若需 PITR 即触发 §1 停机对账姿态 |
| 对象存储区域故障 | ≤4h | 证据类可降级运行（verdict 记 INCONCLUSIVE），任务不停 |
| 全量灾难重建 | ≤24h | 含 key 恢复 + 全设备重新 bootstrap |

数值随 quotaProfile 版本化,S6 压测校准后冻结（Q7#4 同源规则）。

## 4. 恢复后强制序列（Final #8 定案,任何恢复事件一律执行）

```
1. authority 校验：只读 PG 已提交状态（authority_epoch ACTIVE 行）;
   处于 TRANSFER PREPARED 中断态 → 保持全系统停止,人工按 A-3 §6 T5' 记录裁决
2. 全局 connectionFence 强制换代（每设备 generation+1,PG CAS）
3. 全部设备 CLOUD_SUSPENDED（活跃连接立即失效,客户端按 §7.1 挂起）
4. 恢复点之后的所有 DISPATCHED/在途 action 一律标 UNKNOWN
   （correctness RPO=0 保证"恢复点之后"有完整台账可标;禁止 NOT_EXECUTED 推断）
5. 全部 input_lane_lease 置 REVOKING → 等各设备重连对账
6. 设备逐台重新 bootstrap（A-2 v2 §4）→ LOCAL_LEDGER_RESET 对账
   → 逐窗口 fresh full frame + 身份 rebind + RESYNC_DECISION
7. 云端逐窗口确认后才恢复该窗口 admission;无法对账的窗口保持暂停
8. 恢复事件全程审计:恢复点 LSN、受影响 action 数、UNKNOWN 标记清单、重开时间线
```

## 5. 整体回滚（S8）与灾备的关系

- 整体回滚（Q6 S8：THIN_CLIENT_V1 → 旧本地系统）走 **authority transfer**（A-3 §6 T5'：PREPARED→COMMITTED→manifest 后置签发）,不是灾备恢复;云端数据封存只读、不删除。
- 灾备恢复（本工件）是**同一系统内**的时间点恢复;两者共享的只有"fence 换代 + 全设备挂起 + fresh 对账"的安全骨架。
- 回滚窗口内云端 schema 只许 additive/独立 namespace（Q6#7 expand-contract）,保证封存数据对恢复工具可读。

## 6. 演练（restore drill,进 S9 稳定门与常态运维）

| 演练 | 频率 | 通过标准 |
|---|---|---|
| 备份恢复抽验 | 每次备份 | checksum + 抽样表行数/digest 对 |
| 同步副本切换 | S6 前一次 + 季度 | RTO 达标 + §4 序列完整执行 + 零 correctness 丢失 |
| PITR 演练 | S6 前一次 + 半年 | 恢复点精确 + 审计缺口显式标注 |
| key/cert 恢复 | S6 前一次 + 半年 | 独立托管可用性 |
| 全量灾难重建 | S6 前一次（staging） | ≤24h + §4 序列 + 人工对账流程走通 |
| 回滚预演（S8） | 切换前必做 | 干净环境工件 checksum + authority transfer + 预输入健康门（Q6#7） |

每次演练产出证据入 evidence_manifest（requirementId 挂 Q7 主验收矩阵对应行）。

## 7. 监控挂钩（对齐 Q7 零不变量）

- 同步副本延迟>0、WAL 归档中断、备份 checksum 失败、manifest 对账差异 → 告警（severity 按 correctness/evidence 分级,带 runbook,Q7 P2-2）。
- 恢复序列执行期间,Q7 的"恒为零"监控全程在线——恢复路径本身不豁免任何不变量。
