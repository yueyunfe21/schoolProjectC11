# Internal Worker AI - Shared Cloud Game-Client Facade

## Parent Task Brief #1 / `W-GCF-IMP1` - 2026-07-13T19:54:00-04:00

直接实现，不写 Design #N。你不是仓库中唯一 Worker；保护两仓全部 dirty/untracked，不回滚、覆盖、清理、
重命名或提交他人改动。先读 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、
`docs\superpowers\plans\2026-07-13-direct-service-input-bundle-migration.md`，并核对 Cloud 当前源码。

目标：给后续原样迁移的 Service 提供一个很薄的 Cloud 机械端口 facade，隐藏现有 opaque handle/retained
identity；Service 只提供稳定的 `phaseCode + actionSlot`、原 description、coordinateSpace、原有 ordered
actions 和 timeout。不得新增 per-Service owner/permit/ledger/TTL/线程/轮询/自动 retry。

唯一 Java 写集：

1. Cloud New `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
2. Cloud Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`

实现约束：

- `CloudGameClient` 由 context 在 `.remote` 包内构造，public API 至少覆盖 `readWindowFact(...)`、
  `capture(...)`、`executeInputBundle(...)`，底层只调用现有 `CloudTaskRetainedActionState.retain*` 和
  `CloudTaskServicePort`；不开放 raw request/poll/outcome completion/ledger/handle。
- 相同 `phaseCode + actionSlot + operation` 在 UNKNOWN 时必须复用同一 retained handle/request bytes；不得铸新
  ID 或自动重投。调用方再次显式调用才允许查询/重交现有请求。
- OBSERVED/EXECUTED/可信 NOT_EXECUTED/STOPPED 等现有 terminal outcome 可通过现有 final-consumption API
  做 no-op business mutation 后关闭 occurrence；UNKNOWN 不消费、不推进 occurrence。不要改变 broker/ledger
  现有语义。
- action list 原样 `List.copyOf`，不重排、不合并、不改 delay；返回 typed outcome，不替 Service 做业务
  fallback/retry。
- `CloudTaskServiceExecutionContext` 只新增一个稳定 `CloudGameClient` 字段与 getter；两个构造器都必须绑定
  各自 exact current context + retained state + service port。
- 不改 DHXY、broker、ledger、coordinator、host、tests、schema；不新增/恢复测试。

完成后运行 Cloud `mvn -q clean package`（不得 skip tests）。向本日志真实 EOF 追加
`Internal Worker AI - W-GCF-IMP1 Implementation #1`，列出精确 diff、UNKNOWN/terminal 行为和构建统计。
自审仅 QA，不算父级批准。无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #1 - APPROVED - 2026-07-13T20:09:00-04:00

父级已独立逐行复核实际两文件及其现有 retained-state/service-port 调用链，结论
`P0=0 / P1=0 / P2=0`：

- `CloudGameClient` 构造保持 package-private，三个 public 方法只接受业务已决定的稳定地址和 closed typed
  mechanical payload；没有暴露 request builder、raw port、ledger、poll、completion 或 retained handle。
- `retainWindowFact/retainCapture/retainInputBundle` 继续由同一 taskRun 的既有 retained state 管理 identity；
  `UNKNOWN` 不 final-consume、不续代、不自动重投，调用方再次显式调用相同地址时仍复用同一 handle/request。
- 非 `UNKNOWN` terminal 只走既有 final-consumption coordinator 的 no-op
  `OCCURRENCE_COMPLETE`；facade 不解释业务结果、不新增 fallback/retry。输入列表只有 `List.copyOf`，顺序、字段、
  delay 均不改变。
- `CloudTaskServiceExecutionContext` 的两个构造路径都把 exact current run context、原 retained state 与同 context
  service port 绑定到一个稳定 `CloudGameClient` 字段；没有 host/runner/caller 激活。
- Worker 的 Cloud `mvn -q clean package` 证据为 exit 0、4 suites/21 tests 全绿。父级 fresh 双仓构建仍在本批
  其它 Java 写入稳定后统一执行，不把 Worker 自建替代父级最终门。

结论：`W-GCF-IMP1 SOURCE APPROVED`。无已批准业务差异；按 `0114604e` 基线等价迁移。

## Internal Worker AI - API Lock Follow-up - 2026-07-13

- `CloudGameClient` 现公开精确兼容签名（无额外 checked exception）：
  `InputBundleOutcome executeInputBundle(String phaseCode, String actionSlot, String description, CoordinateSpace coordinateSpace, List<InputActionDto> actions, long timeoutMs)`。
- 仅移除该方法的 `throws InterruptedException` API 暴露；输入顺序、`List.copyOf`、retained handle 复用、UNKNOWN 不消费/不自动重投、terminal final-consumption 行为和其它 facade/context 边界均未改变。
- final-consumption 被中断时恢复线程中断标记并抛 `IllegalStateException`，不新增业务 fallback 或重投。
- 复建命令：Cloud 工作目录执行 `mvn -q clean package`，退出码 `0`，未跳过测试；4 个 Surefire 测试类共 21 项，failures=0、errors=0、skipped=0。

无已批准业务差异；按 `0114604e` 基线等价迁移。

## Internal Worker AI - CLAIMED - 2026-07-13T20:00:00-04:00

- task=`W-GCF-IMP1`
- claimedAt=`2026-07-13T20:00:00-04:00`
- uniqueWriteSet=仅 `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudGameClient.java`、`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskServiceExecutionContext.java` 与本报告；两仓其它 dirty/untracked、broker、ledger、coordinator、host、tests、schema 全部冻结。
- 本轮不写 Design、不改 DHXY、不运行运行面；实施前先核对 Cloud 当前源码与两个目标文件的既有内容。

## Internal Worker AI - W-GCF-IMP1 Implementation #1 - 2026-07-13T20:04:00-04:00

### 实际写入

- Cloud 新增 `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`（125 行）。
- Cloud 修改 `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`：新增一个 `CloudGameClient` 字段、两个构造器内的 exact `(runContext, retainedActionState, servicePort)` 绑定，以及 `gameClient()` getter。
- 未修改 DHXY、broker、coordinator、host、schema、tests 或其它 Java 文件；未回滚、覆盖、清理或提交任何既有 dirty/untracked。

### Facade 行为证据

- `CloudGameClient.readWindowFact(...)`（约第 40-58 行）：用 `phaseCode + actionSlot` 构造现有 `ActionAddress`，只调用 `retainWindowFact` 与 `CloudTaskServicePort.readWindowFact`；非 `UNKNOWN` 通过现有 `consumeWindowFactFinal` 以 `OCCURRENCE_COMPLETE` no-op 关闭，`UNKNOWN` 原样返回且不消费。
- `CloudGameClient.capture(...)`（约第 68-91 行）：只调用现有 `retainCapture` 与 `CloudTaskServicePort.capture`；保留 `UNKNOWN` 的同一 retained handle/request，非 `UNKNOWN` 使用现有 `consumeCaptureFinal` 做 `OCCURRENCE_COMPLETE` no-op。
- `CloudGameClient.executeInputBundle(...)`（约第 99-124 行）：输入列表仅执行 `List.copyOf`，不重排、不合并、不改变 delay；只调用现有 `retainInputBundle` 与 `CloudTaskServicePort.executeInputBundle`；非 `UNKNOWN` 使用现有 `consumeInputBundleFinal` 做 `OCCURRENCE_COMPLETE` no-op，`UNKNOWN` 不消费、不自动重投。
- 三个 public API 均不暴露 raw request、poll、outcome completion、retained handle 或底层 broker；只有调用方再次显式调用同一 facade 方法才会再次查询/提交现有地址。
- `CloudTaskServiceExecutionContext` 两个构造器分别以自身 exact current context、对应 retained state 和新建的同 context service port 构造 facade；getter 只返回该绑定 facade。

### 构建证据

命令：`mvn -q clean package`

- 工作目录：`D:\mavenProject\dhxy-cloud-brain`
- 结果：退出码 `0`，成功完成 Cloud package，未使用 `-DskipTests`。
- 测试统计：4 个 Surefire 测试类、21 项测试；failures=0、errors=0、skipped=0。
  - `CloudBrainTemplateResourceGuardTest`: 1/1
  - `IncenseStatusCloudBrainContractTest`: 11/11
  - `WuhuanRouteCandidateContractTest`: 1/1
  - `XiuluoCloudBrainContractTest`: 8/8
- 产物：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，120,195,073 bytes；`target/original-dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，3,291,263 bytes。

自审仅 QA，不算父级批准。无已批准业务差异；按 `0114604e` 基线等价迁移。

## TRUE EOF CONTROL COPY - Parent Source Review #1 - APPROVED - 2026-07-13T20:09:00-04:00

此前同标题父级结论因重复锚点插入在 Implementation #1 之前；本段是最新真实 EOF 控制记录。父级已独立逐行
复核实际两文件及 retained-state/service-port 调用链，结论 `P0=0 / P1=0 / P2=0`：facade 不暴露 raw
request/ledger/handle，UNKNOWN 不消费、不续代、不自动重投，非 UNKNOWN 只用既有 coordinator no-op
final-consume；input actions 仅 `List.copyOf`，不改变顺序/字段/delay。两个 context 构造路径均绑定 exact run
context、同一 retained state 与同 context service port，未激活 host/runner/caller。

Worker Cloud package 为 exit 0、4 suites/21 tests 全绿；父级 fresh 构建在本波其它 Java 写入稳定后统一执行。
结论：`W-GCF-IMP1 SOURCE APPROVED`。无已批准业务差异；按 `0114604e` 基线等价迁移。
