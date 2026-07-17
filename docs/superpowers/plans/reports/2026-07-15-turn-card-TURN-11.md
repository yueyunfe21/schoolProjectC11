# TURN-11 DHXY Local Turn Action Executor

## CLAIMED

- 领取时间：`2026-07-15T16:06:51-04:00`；状态：`CLAIMED`；`countDelta=0`。
- 角色：CR271 Internal implementation Worker；父级是唯一 manager/final reviewer。
- 前置：`TURN-08B`、`TURN-09`、`TURN-10E` 均已父级 SOURCE APPROVED，`TURN-10E` owner 释放后续派本卡。
- 唯一 Java 写集：`cloud/turn/LocalTurnActionExecutor.java`、`ExecutedTurn.java`、
  `TurnOutcomeAssembler.java`、`TurnStepExecution.java`；唯一报告为本文件。
- 只读：冻结 protocol DTO/validator、`TurnCaptureStepExecutor`、`TurnMatchStepExecutor`、
  `TurnInputStepExecutor`、`LocalServiceStepDispatcher`、`TurnExecutionWindow`、窗口 manager/refresh 服务。
- 禁止修改上游 executor、window/input/capture Service、协议、计划、CR 或其它 dirty/untracked；保护全部既有改动。
- 当前 writers 活动，不运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，不执行 Git mutation。

`CLAIMED`

## SOURCE DELIVERED

- 交付时间：`2026-07-15T16:12:11-04:00`；状态：`SOURCE DELIVERED / PARENT REVIEW PENDING`；
  `countDelta=0`。Worker 到此停止，不自批、不领取下一卡。
- 唯一 Java 交付及 SHA-256：
  - `LocalTurnActionExecutor.java`：`CEDF8373B3293F159FC6D610F6A8AD58793F992025A5A9EDF3430285C6288A9A`
  - `ExecutedTurn.java`：`DB8150EC916715AD83DF31D9F129BD68B12E937C4A30C899CAE9545A17AA79B9`
  - `TurnOutcomeAssembler.java`：`A9CB72B829ECCF2F767DDCE8E5BBB2FD306C96295992593B0E2A3DA51D7D2C21`
  - `TurnStepExecution.java`：`65D56C896040E9912671EB817D754F55355E80E14F15D3890FAE55A1C58E7FCB`

### Ordered action execution

- `LocalTurnActionExecutor.java:54-58` 先 `TurnProtocolValidator.requireValid(action)`，再且仅一次
  `TurnExecutionWindow.resolveForAction(...)`；后续全部 step/capture/input/local 调用复用同一个 window snapshot，未调用
  refresh/title search。
- `:60-75` 严格按 list index 执行；第一个 FAILED/STOPPED 后只为剩余声明 step 生成 NOT_RUN，非空 code 固定
  `NOT_RUN`，无 retry/continue mechanics。
- `:95-115` 按 closed `TurnStepType` 分派。mechanics exception 统一成为当前 FAILED
  `STEP_EXECUTION_EXCEPTION` 与诊断 message；不重试、不执行后续 step。
- `:118-124` CAPTURE 始终执行既有 capture executor，但仅 `UPLOAD_IMAGE` 保留 frame。
- `:126-149` MATCH 始终保留 typed `TurnMatchResult`；miss 的 `clickRequested=false` 不产生 input。明确 click 时只构造
  returned absolute center 的单个 `CLICK_LEFT`，通过 `TurnInputStepExecutor` 在同一 step/action 执行；click
  FAILED/STOPPED 保留 match 与候选 frame 并终止 action，从未调用 `InputProvider`。
- `:152-175` WAIT 与 INPUT 只映射 `TurnInputStepExecutor.Result` 的原始 status/code；STOPPED 当前 step 为 FAILED +
  `STOPPED` code 并带 stop marker。LOCAL_SERVICE 仅调用
  `LocalServiceStepDispatcher.execute(call, step.index())`，机械 status/code/local JSON/frame 原样闭合。

### Outcome and frame invariants

- `TurnStepExecution.java:11-83` 只承载 closed `TurnStepResult`、可选 `TurnFrame`、STOPPED marker 与诊断；只有
  MATCH 可含 match，只有 LOCAL_SERVICE 可含 local JSON，只有 CAPTURE/MATCH/LOCAL_SERVICE 可含 frame。
- `TurnOutcomeAssembler.java:28-86` 要求 execution 数量与 action steps 完全一致且 index/type 对齐；COMPLETED 必须
  全 COMPLETED；FAILED 必须 completed prefix/current FAILED/later NOT_RUN 且 failedStepIndex=current；STOPPED 使用相同
  prefix/terminal/tail，但 failedStepIndex=null。组装后再次调用冻结 validator；没有 DUPLICATE 逻辑。
- `LocalTurnActionExecutor.java:77-91` 维护唯一 candidate frame。普通 FAILED 且 payload 要求 failure evidence 时，只用
  exact bound window 做一次 background full-window `FAILURE_EVIDENCE` capture 并替换 candidate；capture 自身失败只保留
  原 failed outcome/candidate，不重试、不伪 metadata；STOPPED 完全不进 failure-evidence 分支。
- `ExecutedTurn.java:19-38` 强制 outcome metadata 与 PNG both-null-or-both-present，并在字段赋值前 clone byte[]；
  `:41-78` 校验 PNG signature、SHA-256、完整可解码像素 dimensions，accessor 再次 clone，确保 outcome.frame 精确命名
  optional PNG。

### Scope and gates

- 静态计数：`resolveForAction=1`、normal CAPTURE=1、failure-evidence capture=1、MATCH executor=1、普通 INPUT +
  match-click input=2、WAIT=1、LOCAL dispatcher=1、`InputProvider=0`；`git diff --check` exit `0`。
- 未修改 protocol DTO/validator、TURN-08B/09/10E、window/input/capture Service、计划、CR 或其它 dirty/untracked；
  无 arbitrary map/reflection/fifth Service/owner/session/ledger/TTL/durable workflow/auto retry。
- 当前 writers 活动，未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE REVIEW #1

- 审查时间：`2026-07-15T16:15:00-04:00`；结论：`BLOCKED / REPAIR REQUIRED`。
- 严重级别：`P0=0 / P1=2 / P2=0`。父级独立展开四个交付文件，并沿 exact-window、四个永久本地
  Service、单 frame/failure-evidence 合同回读既有实现；Worker 自述不作为批准依据。

### P1-1：LOCAL_SERVICE 未绑定 action 的 exact-window context

- 证据：`LocalTurnActionExecutor.java:56-57` 已解析唯一 `TurnExecutionWindow`，但 `:105/:159-164` 调用
  `LocalServiceStepDispatcher` 时既不传 window，也未使用 `WindowTaskContextHolder.callWith(window.context(), ...)`。
  对比 `TurnInputStepExecutor.java:61-65/:87-89` 明确在 exact context 下提交输入。
- 影响：四个永久本地 Service 的既有实现会读取线程绑定。`BagService.java:1310-1313` 无绑定时把缓存 key
  降级为 `global`；`UICleanerService.java:375-387` 无绑定时把成员身份/角色降级为 false/UNKNOWN。更关键的是其后
  `InputSequences` 只能捕获调用线程的当前绑定，因此 turn loop 线程执行 LOCAL_SERVICE 时可能选择错误窗口或丢失
  多窗口隔离，违反 TURN-11 “同一 refreshed exact window”边界。
- 返修条件：只在 TURN-11 写集内给 `LocalTurnActionExecutor` 注入现有 `WindowTaskContextHolder`，并让
  LOCAL_SERVICE 的完整 dispatcher 调用在 `callWith(window.context(), ...)` 下执行；不得修改 dispatcher、四 adapter、
  四个 Service 或另造 context wrapper。Bag/Give 的 exclusive 仍由 dispatcher 单次拥有，UI/Quest queue ownership 不变。

### P1-2：failure-evidence capture 失败时泄漏先前成功图

- 证据：`LocalTurnActionExecutor.java:80-86` 仅在 capture 成功时替换 `candidateFrame`；异常分支显式保留
  prior candidate。权威 spec `2026-07-15-https-turn-thin-client-protocol-design.md:47-50` 与主计划 `:55-57`
  规定：后续失败要求 full-window evidence 时，失败图替换此前尚未回传的成功图。
- 影响：payload 明确要求失败现场时，截图失败会上传更早的 CAPTURE/MATCH/QUEST 成功图并以其 metadata 命名，
  Cloud 会把过时成功画面误当本次失败证据，违反单 frame 语义。
- 返修条件：进入普通 FAILED + `fullWindowFailureEvidence=true` 分支时先清除 prior candidate；仅成功抓到
  `FAILURE_EVIDENCE` 才设置新 candidate。失败证据 capture 自身异常仍保留原 step failure，但 outcome/frame 和 PNG
  必须同时为空；不得 retry、不得伪 metadata。STOPPED 仍不得抓 failure evidence。

- 复验点：父级重新检查上述两处、四文件 SHA、`git diff --check`，并确认无 `InputProvider`、无自动 retry、无
  写集外改动。Java writer 恢复活动，暂不运行 Maven。

`BLOCKED / REPAIR REQUIRED`

## REPAIR #1 — SOURCE DELIVERED

- 返修交付时间：`2026-07-15T16:17:51-04:00`；状态：
  `SOURCE DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。
- 仍仅修改 TURN-11 写集；本轮 Java 变化仅在 `LocalTurnActionExecutor.java`，其余三个 Java 文件保持
  Parent Review #1 时的内容不变。Worker 到此停止，不自批、不领取下一卡。

### P1-1 exact-window context repair

- `LocalTurnActionExecutor.java:14/:27/:34-49` 只注入既有 Spring bean
  `WindowTaskContextHolder`，未修改 dispatcher、adapter 或四个永久本地 Service。
- `:110/:164-171` 将 `LOCAL_SERVICE` 分支的完整
  `localServiceDispatcher.execute(step.localService(), step.index())` 调用放入
  `contextHolder.callWith(window.context(), ...)`；仍由原 dispatcher/adapter 保持 Bag/Give 单次 exclusive 及
  UI/Quest 自有 queue ownership，没有新增外层 queue、wrapper 或 fallback。
- 静态构造点核对：`rg "new LocalTurnActionExecutor|LocalTurnActionExecutor\\(" src/main/java` 只命中本类构造器，
  未发现需要同步修改的手工构造点。

### P1-2 failure-evidence replacement repair

- `LocalTurnActionExecutor.java:81-91` 仍以 `failed = !stopped && ...FAILED` 限定普通失败；进入
  `fullWindowFailureEvidence=true` 分支后先将 `candidateFrame` 清空，再且仅再尝试一次 exact-bound full-window
  `FAILURE_EVIDENCE` capture。
- 只有 capture 成功才重新设置候选 frame；capture 抛异常时保留原 step failure，候选保持 null，因此
  `TurnOutcome.frame` 与 `ExecutedTurn.optionalPng` 同时为空。STOPPED 仍不进入该分支，无 retry、无伪 metadata。

### Repair #1 source evidence

- 四个 TURN-11 Java 文件 SHA-256：
  - `LocalTurnActionExecutor.java`：`3FD08BBA8941DD439EF90E5A6FE60CFF5806BAB9BEFF36331329E41FC559C8FA`
  - `ExecutedTurn.java`：`DB8150EC916715AD83DF31D9F129BD68B12E937C4A30C899CAE9545A17AA79B9`
  - `TurnOutcomeAssembler.java`：`A9CB72B829ECCF2F767DDCE8E5BBB2FD306C96295992593B0E2A3DA51D7D2C21`
  - `TurnStepExecution.java`：`65D56C896040E9912671EB817D754F55355E80E14F15D3890FAE55A1C58E7FCB`
- `git diff --no-index --check -- /dev/null LocalTurnActionExecutor.java` 无 whitespace error；仅有 Git 的
  LF/CRLF 工作区提示。restricted-token 静态检查：`InputProvider=0`、`submitExclusiveAndWait=0`，无新增 retry。
- 未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation；等待父级独立复审。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE REVIEW #2 — REPAIR #1

- 复审时间：`2026-07-15T16:19:00-04:00`；结论：`SOURCE APPROVED / BUILD COHORT PENDING`。
- 严重级别：`P0=0 / P1=0 / P2=0`。父级独立回读 Repair #1 源码、四文件 SHA、权威 failure-frame 合同、
  `WindowTaskContextHolder` 与 Bag/UICleaner 的真实 context 消费点；不采信 Worker 自述代替审查。
- P1-1 已关闭：`LocalTurnActionExecutor.java:34-49` 只注入既有 `WindowTaskContextHolder`；`:164-171`
  将完整 dispatcher 调用置于 `callWith(window.context(), ...)`，没有修改 dispatcher、adapter、四个永久本地
  Service，也没有改变 Bag/Give 单次 exclusive 或 UI/Quest 自管 queue。
- P1-2 已关闭：`:84-91` 在要求 failure evidence 的普通失败分支先清空 prior candidate；仅 capture 成功才设置
  `FAILURE_EVIDENCE`，capture 异常时 frame/PNG 同时为空。STOPPED 不进入该分支，无 retry、无伪 metadata。
- 四文件 SHA 与 Repair #1 报告逐项一致；`git diff --check` 无 whitespace error；`InputProvider=0`、
  `submitExclusiveAndWait=0`（TURN-11 文件内）、无自动业务 retry、无写集外 Java 修改。
- owner 释放；唯一待项为 Foundation Java writers 稳定后的父级 Maven cohort。依赖解锁后同一 Internal lane
  立即续派 `TURN-12`，不等待构建。

`SOURCE APPROVED / BUILD COHORT PENDING`
