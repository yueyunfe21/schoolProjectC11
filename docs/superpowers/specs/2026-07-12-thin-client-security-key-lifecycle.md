# A-6 认证、密钥与管理授权生命周期（THIN_CLIENT_V1）

工件编号：A-6（终审 Final #1 工件计划）
来源共识：草案 §9.1、Q2 签名条款、Q4 资产撤销、Q5 认证路由、Final #7、B Final #2 复核（#7 无新增 P0/P1/P2）
状态：设计工件 v1
约束：设计级规格，非实现。密码学原语选型标注为语义要求。

---

## 1. Principal / Role / Permission 模型

### 1.1 Principal 类型

| Principal | 载体 | 认证方式 |
|---|---|---|
| USER | 登录会话 | 登录凭据/license → 短期 access token |
| DEVICE | 设备密钥对 | 设备私钥签名（enrollment 注册公钥） |
| ADMIN | 管理会话 | USER 认证 + admin 角色 + 高危操作再认证 |
| SERVICE | 云端内部组件 | 部署期服务凭据（不出云端边界） |

### 1.2 Role → Permission 矩阵

| Role | 权限 |
|---|---|
| viewer | 只读：自有租户的任务状态、记忆、统计、审计摘要 |
| operator | viewer + 任务命令、窗口注册管理、私有记忆编辑（自有用户范围） |
| admin | operator + 记忆强制发布/取消发布/quarantine/回滚、资产撤销、quotaProfile 变更、trustedPublisher 配置、设备强制解绑、用户管理 |
| （不存在） | 跨租户读写——任何角色都没有；平台运维走独立 maintenance 通道（A-3 §0）且全审计 |

高危操作清单（要求**再认证** + 同事务审计，A-3 §1 correctness 级）：强制发布、取消发布、quarantine、回滚、删除、trustedPublisher 变更、资产 REVOKE、设备解绑、quotaProfile 降低安全上限方向的变更、authority transfer。

## 2. 设备生命周期

```
ENROLL:  用户在已认证会话内发起 → 客户端在 OS 密钥库（Windows DPAPI/TPM 优先）生成设备密钥对
         → 公钥 + 设备指纹上送 → 服务端写 device 行（ENROLLED）→ 审计
ACTIVE:  bootstrap 握手用设备私钥签名 HELLO 应答 challenge（A-2 v2 §4）
UNBIND:  用户/admin 发起 → device 行置 REVOKED → 同事务：该设备全部活跃 token 吊销
         + connection_fence 强制换代（在线连接立即断开走 CLOUD_SUSPENDED）→ 审计
```

- 私钥永不离开设备密钥库；服务端只存公钥。
- 设备指纹仅用于异常检测提示，不作为认证因子（可伪造）。

## 3. Token 生命周期

| 属性 | 规则 |
|---|---|
| 形态 | 短期 access token（建议 ≤1h）+ refresh token（绑定设备，可撤销） |
| 签发 | 登录/license 验证 + 设备签名双因子 |
| 刷新 | refresh token 单次滚动（旧 refresh 用后即废，重放=吊销全链并审计） |
| 撤销 | 服务端吊销列表即时生效；吊销传播到活跃连接=强制 fence 换代断连 |
| 禁止 | 客户端内置共享永久 token（§9.1 硬边界）；token 入日志/metrics label（Q7 P2-3） |

## 4. 签名密钥三域分治（Final #7 定案）

### 4.1 连接消息签名（会话域）
- bootstrap 协商派生会话签名密钥，绑定 {connectionFence, 算法}（A-2 v2 §2/§4）；帧头不携算法字段（防 confusion）。
- fence 换代=会话密钥作废重派生。泄露影响半径=单连接单 fence 周期。

### 4.2 资产签名（发布域，跨连接缓存）
- 服务端资产签名密钥带 `signing_key_id`，客户端持 keyring（多把可验）。
- **轮换**：新 key 签新资产 → 旧 key 置 RETIRING（可验不可签，期限=最长客户端缓存刷新周期）→ 到期置 RETIRED（不再可验，其资产自然升版重签）。
- **Compromise**：keyId 置 REVOKED → 该 keyId 全部资产按 Q4/Q5 REVOKED 强路径（停 outbox 重投 → REVOKING/RESYNC → drain 后恢复）→ 客户端 keyring 更新经 bootstrap 版本协商下发。
- 缓存资产每次使用前重 hash + 对照当前签名 plan/manifest（Q4#5），REVOKED keyId 验签直接失败=`ASSET_HASH_MISMATCH` 类拒绝。

### 4.3 服务端 secret/cert（部署域）
- TLS cert、PG/Redis/对象存储凭据、token 签发密钥：专用 secret 管理（环境注入，不入镜像/仓库）；轮换计划随部署版本；**与数据备份分开托管**（A-7：否则备份即泄露）。

## 5. Upload Grant（单次消费）

- 签发：CAPTURE_SPEC 触发，grant 绑定 {tenant, device, window, frameId, captureSpecDigest, expiry}。
- 消费：五条件原子 UPDATE（A-3 §7）；二次使用/条件不符=拒绝+审计。
- grant 本身短时效（≤ 上传预期时间 ×3），过期未用自动作废。

## 6. Key/Epoch 变化对运行态的影响矩阵

| 事件 | 活跃连接 | outbox 在途 | 客户端缓存资产 | 任务 |
|---|---|---|---|---|
| token 撤销 | fence 换代断连 | 按 fence 换代屏障（A-2 v2 §9.3） | 不受影响 | CLOUD_SUSPENDED→RESYNC |
| 设备解绑 | 同上且永久 | 全标 UNKNOWN | 作废（下次验签失败） | 终止 |
| 连接密钥泄露 | 强制 fence 换代 | 同屏障 | 不受影响 | RESYNC |
| 资产 key RETIRING | 无影响 | 无影响 | 可验可用 | 无影响 |
| 资产 key REVOKED | 无影响 | 停止相关重投 | 该 keyId 全体失效 | 相关 run REVOKING/RESYNC |
| authority transfer | 全断（切换/回滚流程） | drain 后封存 | 按新 epoch 重协商 | 干净终态 |

## 7. trustedPublisher 治理

- 仅 admin 角色可写 `trusted_publisher` 标志（服务端配置，§8.2 硬边界），同事务审计。
- 授予不追溯：只影响此后产生的因果强验证数据进入候选池；撤销即时：未发布候选冻结，已发布版本不自动下架（管理员可手动 quarantine）。

## 8. 审计要求（与 A-3 §1 对齐）

本工件全部生命周期事件（enroll/unbind/token 签发撤销/key 轮换撤销/grant 消费失败/RBAC 变更/高危操作）落 audit_event；其中绑定 correctness 迁转者同事务写入（RPO=0），独立运营类按 durable-audit 级。
