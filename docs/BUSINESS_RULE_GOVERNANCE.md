# 业务规则治理与冲突停工门禁

## 1. 目的

业务规则的真实语义继续写在各任务业务 MD 中。本文件只规定如何让规则可追踪、可审核、可阻止半迁移，
不建立第二份业务逻辑。

核心目标只有三个：

1. 每次改动都能回答“依据业务 MD 的哪一条规则”；
2. 跨 Client/Cloud、Runner/task 或生命周期的规则能回答“生产者、传输、消费者和顺序分别在哪里”；
3. 发现文档与实现冲突时，Agent 停下来问用户，不替用户作决定。

## 2. 文件职责

- 业务 MD：唯一业务语义来源，规则标题带稳定 `BR-<DOMAIN>-NNN` 编号。
- `docs/business-rules.json`：导航索引，只保存规则 ID、标题、权威 MD 和标题锚点。
- `docs/rule-traceability/G*.json`：某张 G 卡对哪些规则、代码和连通性合同负责。
- `scripts/check-business-rule-gate.js`：检查索引和追踪信息是否完整，不判断业务选择是否正确。
- `docs/PACKAGE_ARCHITECTURE.md`：记录用户裁决、冲突、review 与验收结论。

索引和追踪文件不得复制业务步骤、阈值、坐标、fallback 或 phase 语义；这些内容只能存在于权威业务 MD。

## 3. 增量登记

不要求一次性给全部历史规则编号。任何新 G 卡一旦触及某条规则，必须在写业务代码前：

1. 在权威业务 MD 的对应标题加入稳定 ID；
2. 将 ID 登记到 `docs/business-rules.json`；
3. 创建该卡的 `docs/rule-traceability/Gxxx.json`；
4. 运行规则门禁。

规则 ID 一旦发布不得复用或换义。规则被替代时保留旧 ID，并在索引中标为 `superseded`，指向替代规则。

## 4. 冲突停工协议

以下任意两者存在实质差异，都属于冲突：权威业务 MD、最新推送业务基线、当前代码、拟议实现、用户新要求。

Agent 必须执行：

1. 停止业务实现，不先改 MD，不先改代码；
2. 在 G 卡写明规则 ID、冲突双方、至少两个选择、每个选择的运行影响和推荐；
3. 将追踪文件状态改为 `BLOCKED_USER_DECISION`，`businessDifference.kind` 设为 `UNRESOLVED`；
4. 向用户提出具体问题；
5. 用户明确选择后，把原话摘要和卡片位置写入 `userDecision` / `cardReference`；
6. 先更新权威业务 MD，再实施代码和合同测试。

“修好它”“完成这张卡”“按你说的做”等一般授权不能自动解决一个尚未披露的冲突。Agent 也不能以
“文档可能过时”“当前代码更安全”“测试更容易”为理由自行选择。

## 5. 跨边界规则的最低证据

每条跨边界规则必须在追踪文件中列出：

- `producer`：哪个可达生产入口产生事实或动作；
- `transport`：事件、HTTP metadata、DTO、队列或调用边界；
- `consumer`：哪个生产分支实际消费；
- `ordering`：不可颠倒的先后关系；
- `contracts`：会在生产者消失、metadata 丢失、顺序颠倒或 consumer 不可达时失败的命名合同。

仅存在 enum、DTO 字段、interest 类型、consumer 或业务方法返回值测试都不算链路已通。

## 6. 每卡状态

- `DRAFT`：登记中，禁止业务实现。
- `BLOCKED_USER_DECISION`：存在未决冲突，禁止业务实现。
- `APPROVED_IMPLEMENTATION`：规则与冲突均已明确，可以实施。
- `REVIEW_PASSED`：源码审核、连通性合同和必要 compile 均通过。

交付前运行：

```powershell
node scripts/check-business-rule-gate.js --card G016
```

审核所有已登记卡：

```powershell
node scripts/check-business-rule-gate.js --all
```

## 7. 测试边界

用户已在 G016 明确批准业务规则连通性/顺序合同作为长期保留测试族。该例外只证明链路和顺序，不能取代
模板回放、实机截图、输入验证或用户业务裁决，也不能借机扩张普通单元测试。
