# A-5 主验收矩阵（THIN_CLIENT_V1）

工件编号：A-5（终审 Final #1 工件计划）
来源共识：Q7 全部收口 + B Final#1 P1-1（完整追踪 §3-§10 与 Q1-Q7）
状态：设计工件 v1 —— 需求追踪骨架完整；方法级行（REQ-M-*）待 A-1 交付后注入，该分区显式 NOT_EVALUATED 并阻塞切换
规则：状态 ∈ NOT_EVALUATED / BLOCKED / PASS / APPROVED_DIFFERENCE（后者必须链接用户裁决）；证据必须绑定不可变 release identity（buildHash/allowlistHash/baseline commit/policy+asset+quotaProfile+normalizer version/环境/时间/证据 content hash）；依赖项变化自动使受影响行回 NOT_EVALUATED。

---

## 0. 图例

| 列 | 说明 |
|---|---|
| requirementId | 稳定 ID，永不复用 |
| 源 | 草案章节/分题/终审条目 |
| 证据类型 | INV=不变量+故障注入(A档) / SEM=normalized 语义序列比对(B档) / CON=typed 契约+容差(C档) / SCH=schema+round-trip+allowlist(D档) / LOAD=负载压测 / DRILL=演练 / HUMAN=真人真机 / AUDIT=审计链抽验 / STATIC=静态扫描 |
| 环境 | LOCAL / STAGING / PROD-SWITCH（切换日）|
| 状态 | 初始一律 NOT_EVALUATED |

## 1. 硬边界（§3-§10）

| requirementId | 需求 | 源 | 证据类型 | 环境 |
|---|---|---|---|---|
| REQ-HB-001 | 本地无第二业务状态机；云端超时/失败本地不自选 phase/fallback | §3.1 | STATIC+INV | STAGING |
| REQ-HB-002 | 本地组件职责/禁止清单逐项符合 §4.1 表 | §3.2/§4.1 | STATIC+人工双审 | LOCAL |
| REQ-HB-003 | LocalSafetyGate 只拒绝不修正（六类拒因全覆盖） | §3.3 | INV | STAGING |
| REQ-HB-004 | 云端 Service 保持原业务边界（非微服务化） | §3.4 | STATIC | LOCAL |
| REQ-HB-005 | 无逐 Service 生产切换；运行时无单 Service 本地回退 | §3.5 | STATIC+SEM | STAGING |
| REQ-HB-006 | 双通道通信 + 消息按 scope 携带身份（A-2 v2 §3/§6 修正版） | §5.1 | SCH | STAGING |
| REQ-HB-007 | 本地不洗图/不 OCR/不选模板 | §5.2 | STATIC | LOCAL |
| REQ-HB-008 | ActionPlan 原子边界 + 观察点交回 | §5.3 | INV+SEM | STAGING |
| REQ-HB-009 | MATCH_AND_CLICK/REPORT 无业务语义（A-2 v2 §8.3 时序） | §5.4 | INV | STAGING |
| REQ-HB-010 | 机械事实/业务解释分离（IN_COMBAT 等只在云端） | §5.5 | STATIC | LOCAL |
| REQ-HB-011 | task turn 决策 100% 云端；本地锁仅竞态保护 | §6 | SEM+INV | STAGING |
| REQ-HB-012 | 断云=CLOUD_SUSPENDED 非 FAILED；重连云端定夺 | §7.1 | INV+HUMAN | STAGING+PROD-SWITCH |
| REQ-HB-013 | pause/stop 本地立即生效且不污染 FAILED | §7.2 | INV+HUMAN | STAGING+PROD-SWITCH |
| REQ-HB-014 | 数据归属（云权威/本地五项） | §8.1 | STATIC | LOCAL |
| REQ-HB-015 | 多用户记忆规则（私有隔离/可信发布/3 次自动发布不改人工） | §8.2 | AUDIT+INV | STAGING |
| REQ-HB-016 | 生产形态（单体+Redis/PG/对象存储+隔离） | §9 | LOAD | STAGING |
| REQ-HB-017 | 认证方向（短 token/设备密钥/无共享永久 token） | §9.1 | INV+STATIC | STAGING |
| REQ-HB-018 | 图片保留策略六级 | §10 | AUDIT | STAGING |

## 2. Q1 门（迁移完备）——分母依赖 A-1

| requirementId | 需求 | 证据类型 | 依赖 |
|---|---|---|---|
| REQ-Q1-001 | 方法级 inventory 全覆盖（含继承/lambda/监听器/条件注册） | STATIC(工具) | **A-1** |
| REQ-Q1-002 | 生产入口可达闭包无未知节点 | STATIC | A-1 |
| REQ-Q1-003 | 配置/资源零未归属（三分法） | STATIC | A-1 |
| REQ-Q1-004 | Thin Client 产物 allowlist（只含 §4.1 映射包） | STATIC(构建) | A-1 |
| REQ-Q1-005 | 反向扫描零未映射业务语义命中 | STATIC | A-1 |
| REQ-Q1-006 | 人工按业务流反向抽查无断点 | 人工双审 | A-1 |
| REQ-Q1-007 | 矩阵绑定冻结基线（repo/branch/commit/worktree-diff/时间） | AUDIT | A-1 |
| REQ-M-* | 每方法行 tier 标注与对应档证据 | 按 A/B/C/D | **A-1 注入，当前整区 NOT_EVALUATED，阻塞切换** |

## 3. Q2/Q3 门（协议与状态）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q2-001 | E4 重试白名单三条件（含 STOPPED 不重放） | INV |
| REQ-Q2-002 | sequence 域 (fence,direction,streamKey)；gap 停+RESYNC | INV |
| REQ-Q2-003 | bootstrap→FENCED；客户端永不猜 fence | INV |
| REQ-Q2-004 | 精确字节帧签名；未知字段 fail-closed（EXEC） | SCH+INV |
| REQ-Q2-005 | stale-frame 全字段栅栏 + 本地单调时钟 | INV |
| REQ-Q2-006 | messageId/actionId/requestId 三分幂等 | INV |
| REQ-Q3-001 | lease PG 权威+设备 lane 作用域+单飞 | INV |
| REQ-Q3-002 | HELD→REVOKING→HELD 换手屏障（drain proof 完整限定） | INV |
| REQ-Q3-003 | fence 换代复用排空屏障（首条副作用 plan 等待） | INV |
| REQ-Q3-004 | T0-T6/T1'/T-receipt/T4'/T5' 事务原子性 | INV(crash 注入) |
| REQ-Q3-005 | UNKNOWN 证据不可改写；恢复另记 resyncDecision | AUDIT |
| REQ-Q3-006 | window incarnation/显式 rebind（永不凭 HWND 挂回） | INV |
| REQ-Q3-007 | tombstone/压缩后旧 actionId 不变"未见过" | INV |

## 4. Q4 门（记忆）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q4-001 | verifier 前态→后态转移证明（before 已满足=INCONCLUSIVE） | SEM |
| REQ-Q4-002 | canonical 无损/可逆/幂等导入（唯一键防重） | SCH+AUDIT |
| REQ-Q4-003 | 三池独立生命周期；普通用户永不入公共池 | INV+AUDIT |
| REQ-Q4-004 | run-level 聚合防刷票；发布事务唯一键防双发布 | INV |
| REQ-Q4-005 | scorePolicyVersion=1 公式与常数一致 | SEM |
| REQ-Q4-006 | DEMOTED/QUARANTINED 状态机与恢复路径 | INV+AUDIT |
| REQ-Q4-007 | 资产 REVOKED 强路径（停重投+REVOKING+drain） | INV |
| REQ-Q4-008 | 证据删除后 digest 锚点永存 | AUDIT |

## 5. Q5 门（容量）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q5-001 | 分层预算（global→…→window，多维度） | LOAD |
| REQ-Q5-002 | 关键控制流保留通道永不 THROTTLED | INV+LOAD |
| REQ-Q5-003 | 解压后大图全链防护（grant/尺寸/时限/native 信号量） | INV |
| REQ-Q5-004 | class pool + in-flight cap + DRR；deadline 真取消 | LOAD |
| REQ-Q5-005 | 三存储故障=可用性事件（含已下发动作闭环） | DRILL |
| REQ-Q5-006 | fenced delivery（enqueue≠delivered；签名 receipt/同 digest outcome） | INV |
| REQ-Q5-007 | 负载矩阵三零标准（零静默丢/零跨租户/零过载业务 FAIL） | LOAD |
| REQ-Q5-008 | quotaProfile 版本化；收紧只挡新 admission | AUDIT |

## 6. Q6 门（建设与切换）

| requirementId | 需求 | 证据类型 | 环境 |
|---|---|---|---|
| REQ-Q6-001 | 生产同构 staging（Shadow/故障注入/容量至少一轮） | DRILL | STAGING |
| REQ-Q6-002 | shadow 同帧 tee + 双端硬拒 + 禁学习 | INV | STAGING |
| REQ-Q6-003 | A/B/C/D 证据分档（observation 影响 A/B 不得自称 D） | 人工双审 | LOCAL |
| REQ-Q6-004 | S6 覆盖清单（未覆盖=NOT_EVALUATED=阻塞） | SEM | STAGING |
| REQ-Q6-005 | 零未解释分歧（每条=bug 或用户裁决差异） | SEM+AUDIT | STAGING |
| REQ-Q6-006 | S7 quiesce/版本握手/singleton 双保险/dirty-drain 不切换 | DRILL+HUMAN | PROD-SWITCH |
| REQ-Q6-007 | 二段式导入 + cutover manifest 逐项核对 | DRILL | PROD-SWITCH |
| REQ-Q6-008 | S8 回滚预演（干净环境 checksum+authority transfer+健康门） | DRILL | STAGING |
| REQ-Q6-009 | 回滚 fresh run + cutover journal 对账 | DRILL | STAGING |
| REQ-Q6-010 | S9 稳定门（7 天/每类 20 run/3 fresh 启动/降低须批准） | AUDIT | 切换后 |

## 7. Q7 门（验收与监控自身）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q7-001 | 证据绑定 release identity + 依赖变化自动失效 | AUDIT |
| REQ-Q7-002 | 因果元数据 100%（采样仅 payload/span）；因果图自动校验 | INV |
| REQ-Q7-003 | S6 前冻结数值 SLO profile（单调钟分段测量） | LOAD |
| REQ-Q7-004 | 零不变量清单 + 自动遏制（非零=事故响应） | INV |
| REQ-Q7-005 | split-brain 四要素证明 | INV |
| REQ-Q7-006 | stop 归因按 canonical trigger（非链上出现） | INV |
| REQ-Q7-007 | 真人真机七项集（四骨架+多窗口竞争+每任务族全程+真实重启） | HUMAN |
| REQ-Q7-008 | telemetry/正确性双路分离 | INV |
| REQ-Q7-009 | 告警可运营（severity/owner/runbook/自动动作） | AUDIT |
| REQ-Q7-010 | observability 租户隔离与最小披露 | AUDIT |

## 8. 终审跨章节决定（Final #2-#8 + 工件审查修正）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-F-001 | wire framing 唯一（binary frame+detached 签名+base64 payload） | SCH |
| REQ-F-002 | bootstrap 无 fence 握手（业务零 dispatch） | INV |
| REQ-F-003 | 硬 expiry 执行截止 + 本地预算重算门 | INV |
| REQ-F-004 | 租户隔离数据库强制（复合键+RLS） | INV(跨租户探针) |
| REQ-F-005 | protocol_fact append-only + causation 引用存在性 | INV |
| REQ-F-006 | authority append-only + 单 ACTIVE writer（partial unique） | INV+DRILL |
| REQ-F-007 | correctness RPO=0（同步副本延迟监控+恢复八步序列） | DRILL |
| REQ-F-008 | 密钥三域分治 + compromise 撤销影响矩阵 | DRILL+AUDIT |
| REQ-F-009 | 恢复后强制序列（fence 换代/UNKNOWN 标记/逐窗口重开） | DRILL |

## 9. 用户裁决登记区（P2-2：仍需用户拍板事项）

| 决策 | owner | 最迟裁决阶段 |
|---|---|---|
| S9 删除门槛数值确认 | 用户 | S9 前 |
| SLO/quotaProfile 冻结数值批准 | 用户 | S6 门 |
| 真人真机集执行安排 | 用户 | S6-S7 间 |
| 切换日 S7 最终放行签字 | 用户 | S7 |
| 迁移矩阵产出后的删除清单批准 | 用户 | S9 |

除上述五项无其他未决产品决策；实现层数值由证据门产生，降低门槛仍需用户批准。

## 10. 当前状态汇总

全部行初始 NOT_EVALUATED。REQ-M-* 分区（方法级）待 A-1 注入——**该分区空缺本身即阻塞切换项**。本矩阵为骨架 v1；A-1 交付后生成完整版并重登 hash。
