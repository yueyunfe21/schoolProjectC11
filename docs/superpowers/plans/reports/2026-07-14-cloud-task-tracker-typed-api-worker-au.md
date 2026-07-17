# Internal Worker AU - TaskTracker typed Cloud API

## Parent Direct Implementation Task - `W-TTPS-TYPED-API-IMP1` - 2026-07-14T00:31:00-04:00

这是直接实现，不写 Design。目标是让已经迁入 Cloud 的 `TaskTrackerPanelService` 主算法消费一个明确的
typed request，而不是在算法各处继续读取原始 `JsonNode`。不改变任何算法、阈值、顺序、诊断、坐标或结果。

### 唯一写集

- Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\TaskTrackerPanelService.java`
- Append-only 本报告

不得修改 `DecisionEngine.java`、DHXY Java、remote wire、schema、tests、pom 或任何其它文件。共享工作区还有
External A/B/C/D 和另一 Internal Worker；不得回滚、覆盖、清理、删除、提交他人 dirty/untracked。

### 实现合同

1. 在同一文件中新增一个真正承载当前输入的 typed request（record 或同等 immutable value）：字段只覆盖现有入口
   实际消费的 `taskCode`、`imagePayloadBase64`、`imageMode`、`imageOriginWindow`、`requestedLinkIndex`、`taskKey`。
2. 现有 `read(JsonNode request)` 只做一次 transport translation，然后调用 typed 主入口；不得形成多层 wrapper。
3. 现有全部 tracker 算法主体改为消费 typed request，不得再从算法分支读取 `JsonNode`。
4. 完整保持当前 invalid/missing reason、decision string、confidence、algorithm、diagnostics、坐标空间、候选顺序、
   requested-index fallback、task-key 规则、flush/finally、模板路径与所有数字常量。
5. 不新增 capture/input、owner/session/permit/ledger/TTL/retry/thread、协议字段或运行入口。

### 门禁与报告

- 完成后在 Cloud 运行 `mvn -q package`（不 clean，避免干扰共享构建目录），记录 exit 与 Surefire 汇总。
- 向本报告 EOF 追加 `CLAIMED`、`Implementation #1`、精确文件、SHA256、构建结果和 self-QA。
- 自审不算父级 Approved；父级会逐行审查并统一运行 fresh `mvn -q clean package`。

保护全部 dirty/untracked；禁止 Git mutation，禁止启动 host/application/Task/UI/input。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED

- Worker: Internal Worker AU
- Task: `W-TTPS-TYPED-API-IMP1`
- Status: CLAIMED
- Scope: 仅修改 Cloud `TaskTrackerPanelService.java`，并仅向本报告 EOF 追加。
- Baseline: `0114604e`；无已批准业务差异，按基线等价迁移。

## Implementation #1

- Worker status: `DONE`（仅实现与自审；不构成父级 Approved）。
- 精确实现文件：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\TaskTrackerPanelService.java`
- SHA256：`E07E499C34E4711C8B3B117B7EA706CDE096252F61CF9A549FA7DAACA5CE6BEE`
- 实现：在同文件底部新增 immutable `TrackerPanelRequest` record，且仅含 `taskCode`、
  `imagePayloadBase64`、`imageMode`、`imageOriginWindow`、`requestedLinkIndex`、`taskKey` 六字段。
- 实现：`read(JsonNode request)` 现在只从 transport/context 一次提取六字段并直接调用单个 typed
  `read(TrackerPanelRequest request)` 主入口；未增加其它 wrapper 层。
- 实现：tracker 算法主体只消费 typed request；`JsonNode` 仅保留在 transport 入口，不再出现在算法分支。

### Build

- 目录：`D:\mavenProject\dhxy-cloud-brain`
- 命令：`mvn -q package`（未 clean）
- Exit code：`0`
- 总耗时：约 `66.5s`
- Surefire 汇总：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`。
- Suite 明细：`XiuluoCloudBrainContractTest` 8、`IncenseStatusCloudBrainContractTest` 11、
  `WuhuanRouteCandidateContractTest` 1、`CloudBrainTemplateResourceGuardTest` 1；均 0 failure/error/skip。

### Self-QA

- 对照六字段逐项检查：transport 仅提取一次；typed 主入口直接使用对应 record accessor。
- invalid/missing reason、decision string、confidence、algorithm、diagnostics key/value、
  `WINDOW_RELATIVE` 坐标、候选选择顺序、模板路径与所有数字常量未改。
- `requestedLinkIndex` 仍为 null 时回退 `-1`，越界或负数仍选择 index `0`；合法 index 保持原选择。
- 五倍 detail 模式仍信任客户端 `taskKey`，缺失 reason 保持不变；其它 task-key 识别规则与分支顺序未改。
- `greenMask.flush()` 与 owned `scanRaw.flush()` 的原 `finally` 结构未改。
- 未新增 capture/input、owner/session/permit/ledger/TTL/retry/thread、协议字段或运行入口。
- 未修改 `DecisionEngine.java`、remote wire、schema、tests、pom、DHXY Java 或其它源码；未执行 Git mutation，
  未启动 host/application/Task/UI/input。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #1 - APPROVED - 2026-07-14T00:42:00-04:00

父级逐行复核当前 Cloud `TaskTrackerPanelService.java`，结论 `P0=0 / P1=0 / P2=0`：

- `read(JsonNode)` 只在 transport 边界读取 `taskCode` 与 context 中五个既有字段，并直接构造同文件底部
  immutable `TrackerPanelRequest`；没有新增 wrapper 链或协议字段。
- tracker 主算法仅使用六个 typed accessor；`JsonNode` 不再进入算法分支。invalid/missing reason、task code
  allowlist、requested-index fallback、坐标空间、diagnostics、模板路径、数值常量及 flush/finally 均保持原样。
- 唯一源码 SHA256 为 `E07E499C34E4711C8B3B117B7EA706CDE096252F61CF9A549FA7DAACA5CE6BEE`；
  Worker `mvn -q package` exit 0，21 tests 全绿。父级仍会在本波全部 Java 稳定后运行 fresh
  `mvn -q clean package`。

本切片 `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
