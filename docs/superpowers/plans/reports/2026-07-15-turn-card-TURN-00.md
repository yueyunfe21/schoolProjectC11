# TURN-00 Report — 协议闭口与附录纠偏

## CLAIMED

- 领取时间：`2026-07-15T14:22:42-04:00`
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA documentation-only contract closure)`
- `countDelta`：`0`
- `dependsOn`：无
- 精确写集：
  - `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
  - `docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md`
  - 本报告（主计划要求的状态/证据记录）
- 禁止触碰：所有 Java、Maven、配置、CR271、仪表盘、主计划，以及上述两份文档之外的任何文件。
- 基线证据：DHXY 当前分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；远端不存在
  `origin/thin-client-design`，已核对 `origin/master` 为可用的最新推送参考。该卡只改协议文档，不改变业务逻辑。

## 当前两仓 git status（领取瞬间）

### DHXY — `thin-client-design`

- 已修改：`config/dialog_choice_memory.json`、`config/maps.json`（删除）、`docs/ACTIVE_WORK.md`、
  `docs/DHXY_CONTEXT.md`、`docs/HYBRID_CLOUD_WORKFLOW.md`、`docs/PACKAGE_ARCHITECTURE.md`、
  `docs/cr-dashboard-data.js`、6 份既有 thin-client spec、`pom.xml`，以及 23 个既有 Java 源文件。
- 未跟踪：既有 7 份计划、`plans/briefs/`、既有 `plans/reports/`、两份 2026-07-15 spec/plan、模板目录、
  `cloud/remote/` 与多个既有 Java/Service 目录和文件（完整列表以领取时 `git status --porcelain=v1` 为准）。
- 本卡只新增本报告；不回滚、不覆盖、不清理、不提交任何既有 dirty/untracked 项。

### Cloud Brain — `navigation-migration`

- 已修改：`pom.xml`、`CloudBrainServer.java`、`DecisionEngine.java`、`ImageAlgorithms.java`、
  `ObjectiveTextRecognizer.java`、`SmartClickRecognizer.java`、`gateway/CloudApiGateway.java`、
  `src/main/resources/config/maps.json`。
- 未跟踪：`logs/`、`migration-baseline/`、`migration-preserved/`、`scratch_baseline_MapNameCanonicalizer.java`、
  既有 `com/bot/`、Cloud turn/host/remote 相关 Java 文件及三个既有模板 PNG。
- 本卡不写 Cloud Brain 文件；全部既有 dirty/untracked 保持原状。

## 执行边界

- 只把权威主计划第 2 节的补充合同完整写入 spec/附录，并移除附录中“另写 Cutover B”的遗留表述。
- 不运行 runtime/application/Task/poller/UI/capture/input/tests；该卡无 Java 修改，故无 Maven 编译门。
- 交付后等待父级唯一裁决；只有报告和 CR271 均出现 `PARENT APPROVED，P0/P1/P2=0，card CLOSED` 才停止 heartbeat。

## DELIVERED

- 交付时间：`2026-07-15T14:25:02-04:00`
- 状态：`DELIVERED`
- 完成内容：
  - spec 新增“Locked Minimum Contract”，逐项写入主计划 §2.1--§2.6：五种 step 与 typed input、单 outcome frame、
    当前窗口 metadata、closed typed `LOCAL_SERVICE`、合法 `200` 确认 previous outcome 的边界，以及 Cloud PNG
    bytes 与 `contentHash` 的单一权威。
  - Foundation 附录移除“另写 Cutover B”、独立结束点和后续独立计划表述；其 DTO 草案同步为 `INPUT` +
    `TurnInputAction`、完整 local operation allowlist 与 operation-specific typed DTO；后续范围仅引用主计划卡片。
- 交付证据：`rg` 对两份文档的 `Cutover B|另写 Cutover B|CLICK,|KEY_PRESS,` 无命中；
  `git diff --check` exit `0`。
- Maven：不适用。本卡只改文档，未改 Java/Maven/config，且未运行 tests/runtime/application/Task/poller/UI/capture/input。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 仍待：父级唯一 reviewer 在 CR271 与本报告写明 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`；在此之前保持 heartbeat，
  不领取后续卡。

## PARENT REVIEW

- 审查时间：`2026-07-15T10:40:00-04:00`
- 独立审查范围：两份权威文档的 locked contract、Foundation scope decomposition、TURN-00 报告与主计划第 2 节。
- P0：`0`
- P1：`0`
- P2：`0`
- 预检返修：非绑定排班 helper 在最终关卡前指出 `TurnWindowMetadata`/`TurnFramePurpose` 的文字合同未落到
  Foundation 文件表和 record 草案（P1=1），以及本地 adapter 缺单一共享结果边界（计划 P1=1）。父级独立复核
  后已补齐前者，并在主计划新增 `TURN-10P` 处理后者；两项均已关闭，不留未解决 P1。
- 精确证据：
  - spec 已锁定 `CAPTURE/MATCH_TEMPLATE/INPUT/WAIT/LOCAL_SERVICE`、完整 typed input enum、单 outcome frame、
    当前 bound-window metadata、closed local-Service arguments、合法 `200` 确认 previous outcome 和模板 hash 单一权威；
  - Foundation 附录明确从属于完整主计划，不得再创建独立 Cutover 计划；DTO 草案与 allowlist 已同步；
  - `TurnWindowMetadata` 固定携带实际窗口身份/矩形/stop 状态，`TurnFramePurpose` 固定四值；request/outcome 均通过
    同一 metadata 类型承载，frame metadata 显式携带 purpose；
  - `rg` 未发现遗留 `CLICK/KEY_PRESS` step enum 或肯定式“另写 Cutover B”安排；`git diff --check` exit `0`；
  - 本卡未修改 Java/Maven/config，不适用 Maven 门，且未运行 runtime/tests/input/capture。
- 影响：协议实施合同已经闭口，可以按精确互斥写集并行启动 Foundation Java 卡；不产生 407 ledger 增量。
- 返修条件：无。

**PARENT APPROVED，P0/P1/P2=0，card CLOSED**
