# TURN-28P Delivery Review - Independent Reviewer R1

## REVIEW CLAIMED - 2026-07-16T04:44:40.574-04:00

- platform agent/thread id: `019f6a15-8906-7331-aeb4-3f03aaeff31c`
- nickname: `R1`（本线程平台元数据未暴露另一个独立 nickname 字段）
- role: CR271 TURN-28P independent delivery reviewer R1；不是实现者，也不是父级 manager/final reviewer。
- scope: 独立逐文件审查 TURN-28P 冻结 production 与 named-test source，重点核对 protocol/input byte parity、click timing 单 queue ownership、`150ms + 500ms` 基线、单 command/UUID/no retry，以及 capture probe/outcome 的致命合同问题。
- constraints: 不修改 Java、测试、原 TURN-28P 报告或任何其它既有文件；不运行 Maven/runtime；不执行 Git mutation；保护两仓全部 dirty/untracked。

<!-- TRUE_EOF: TURN-28P DELIVERY REVIEW R1 CLAIMED 019f6a15-8906-7331-aeb4-3f03aaeff31c 2026-07-16T04:44:40.574-04:00 -->

## REVIEW COMPLETED - 2026-07-16T05:01:19.420-04:00

### Verdict

- 结论：`REVIEW REQUIRED / NOT APPROVED`。
- 精确计数：`P0/P1/P2 = 0/2/1`。
- 本报告是独立 delivery reviewer R1 结论，不代替父级最终判定。

### Findings

#### P1-1 - probe 通过 legacy exclusive queue 入口触发第二次 binding refresh，破坏一次冻结 HWND 合同

**合同证据**

- 原卡冻结合同 `2026-07-16-turn-card-TURN-28P.md:103-107` 要求一次 callback 内始终使用同一冻结 HWND/ROI，且 probe
  期间禁止 refresh/locate/title-search；`:117-119` 再次规定 turn path 不得二次 refresh。
- `TurnExecutionWindow.java:18-22,41-42,68-87` 明确 action 只 refresh 一次，并保存 immutable `binding`/metadata snapshot。

**源码证据**

- `TurnCaptureStepExecutor.java:204-208` 把 probe callback 交给
  `InputSequences.submitExclusiveAndWait(...)`。
- `InputSequences.java:59-60` 无条件转调 legacy `InputActionQueue.submitExclusiveAndWait(...)`。
- `InputActionQueue.java:303-315` 在构造 exclusive request 之前调用
  `refreshAndValidateNativeBinding(context, description, true)`；`:554-593` 中该方法于 `:573` 再次调用
  `bindingRefreshService.refreshAndCommit(context)` 并读取新的 context binding。
- worker 随后按 request 捕获的新 binding 执行 focus/transaction
  (`InputActionWorker.java:119-152`)，但 callback 的 before/after capture 仍使用旧的 frozen `window.binding()`
  (`TurnCaptureStepExecutor.java:213,268,470-482`)，Ctrl DOWN/UP 也仍使用旧 binding (`:220-226,290-295`)。
- 两个 probe test double 都绕过真实 queue：
  `TurnCapturePixelChangeProbeContractTest.java:357-377` 与
  `LocalTurnActionExecutorContractTest.java:897-913` 直接在测试线程同步调用 `callback.get()`，因此没有覆盖第二次 refresh。

**影响**

若 action snapshot 与入队之间发生 HWND/process/title/geometry drift，queue 可 focus 新 binding，而 capture、Ctrl 和
screen-absolute MOVE 仍按旧 snapshot 执行。结果可能把真实鼠标输入送向新窗口，却从旧窗口采样并向旧 HWND 发 Ctrl，
形成跨窗口输入或混合窗口证据；即使 queue 因 refresh 失败而拒绝，probe 也已经违反“期间零 refresh”的冻结合同。

**返修条件**

1. 父级先重新冻结写集：原卡 `:41-44` 明确把 `InputActionQueue`、`InputSequences` 设为只读，当前写集内没有可用的
   no-refresh exclusive queue API，worker 不得自行越权改 queue。
2. 提供并使用一个仍由全局单队列持有、但接受/校验 frozen binding snapshot 且绝不 refresh 的 exclusive 提交边界；
   drift 必须在任何 capture/Ctrl/MOVE 前 fail closed，focus、capture、keyboard、ROI/target 必须证明属于同一
   HWND/process/rect snapshot。
3. named test 必须穿透 production queue 路径（desktop mechanics 仍可 fake），计数证明整个 action 仅初始 resolve
   refresh 一次，并覆盖入队前 binding drift 时零 callback/零物理输入。

#### P1-2 - waiter interruption 可在 callback finally 释放 Ctrl 前先返回 STOPPED

**源码证据**

- probe 的 stop checkpoints 只读取 action 创建时冻结的 `window.metadata().stopRequested()` 和 input worker 自身的
  `Thread.currentThread().isInterrupted()` (`TurnCaptureStepExecutor.java:194,214,235,244,253,262,269,317-329`)。
  它没有读取当前 queue request 的 cancellation/stop token。
- queue 已有专门用于 exclusive callback 的 `InputActionScope`：
  `InputActionScope.java:8-12,43-73` 会观察 waiter cancellation、task stop/pause 和 worker interruption；本 probe
  callback 没有调用该 checkpoint。
- legacy waiter 被中断时，`InputActionQueue.java:678-697` 在 `:692` 先 `request.cancel("waiter interrupted")`，再尝试
  `queue.remove(request)`，随后立即返回。`InputActionRequest.java:496-520` 说明并实现 `cancel` 会直接完成 future、
  unblock submitter；如果 worker 已在 callback 中，`queue.remove` 不能停止 callback。
- callback 实际运行在 input worker (`InputActionWorker.java:131,146-156`)；task/waiter 的 interrupt 不会自动中断该
  worker。外层在 queue 返回后可立即于 `TurnCaptureStepExecutor.java:348-353` 投影 STOPPED，而 worker 仍可能继续
  settle/MOVE/capture，最后才在 `:285-332` 尝试 Ctrl UP 与 release settle。
- `TurnCapturePixelChangeProbeContractTest.java:184-199,357-377,468-477` 把“中断”设置在同步执行 callback 的同一测试
  线程；`LocalTurnActionExecutorContractTest.java:897-913` 也同步执行 callback。两者都无法复现 task waiter 与 input
  worker 分离时的竞态和提前 outcome。

**影响**

真实 task stop/interruption 可能先向 Cloud 发布 STOPPED，随后旧 callback 仍执行物理 MOVE/capture，且 Ctrl UP/100ms
cleanup 尚未完成。这直接违反原卡 `:111-113` 的“release 后投影 STOPPED”，也使 stop 后零物理动作和 Ctrl 不滞留的
性质不可证明；`ProbeState` 还会在 outcome 返回后继续被 worker 写入。

**返修条件**

1. callback 在每个可中断机械边界观察 queue request 的实时 cancellation/Task stop（现有
   `InputActionScope.checkpoint()` 可作为语义依据），但 finally Ctrl UP 不得被 cancellation 跳过。
2. exclusive queue 的 interruption 语义必须形成 completion barrier：若 worker 已取得 callback，waiter 只有在 callback
   退出、恰好一次 Ctrl UP 已尝试且 `ctrlUpSettleMs` cleanup 结束后才能返回并投影 STOPPED；若请求尚未开始，则保持
   零 capture/Ctrl/MOVE。不得用 retry、TTL 或第二 command 修补。
3. 增加真实双线程 queue contract test：task waiter 在 Ctrl DOWN 后被 interrupt，production queue/input worker 继续用
   fake mechanics；断言 outcome/queue return 严格晚于唯一 Ctrl UP + release settle，且 return 后没有任何 mechanics。
   这项修复同样可能需要父级先扩展原冻结 queue 写集。

#### P2-1 - 点名测试没有覆盖 probe action 的 DUPLICATE_OR_UNCERTAIN

**证据**

- 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md:1575` 把 probe 的
  `release failure/stop/uncertain 不伪成功` 列为 TURN-28P named-test 专属断言；原卡 `:130,132-145` 也要求 uncertainty
  保持 typed、同 actionId 零重执行。
- `TurnCapturePixelChangeInvocationContractTest.java:101-108` 的 terminal probe 负例只枚举 `FAILED`、`STOPPED`。
- `TurnGameClientContractTest.java:94-135` 的真实 probe path 只覆盖 completed `PIXELS_CHANGED`；`:138-215` 的
  `DUPLICATE_OR_UNCERTAIN`/command uncertainty cases 使用 plain capture 或 WAIT action，不是 probe action。
- production `TurnInvocationResult.java:126-144` 当前看起来会拒绝 non-completed outcome 中的 completed probe code，
  但没有本卡点名的 probe-specific regression evidence。

**影响**

当前 production 未见把 uncertain 转 success 的直接缺陷，但 action-specific correlation 后续回归时，测试不能证明
probe uncertainty 不会携带 changed/unchanged 成功码/after frame，也不能把“一 UUID、一 command、零 retry”与该
probe terminal case 绑定起来。

**返修条件**

在现有两个 Cloud named-test 文件的批准范围内增加 probe action 的 command-level uncertainty 与 outcome-level
`DUPLICATE_OR_UNCERTAIN` cases，断言 typed uncertainty、无 fabricated probe success/frame、同 actionId、一次 UUID、
一次 command、零 retry；对携带 `PIXELS_CHANGED`/`PIXELS_UNCHANGED` 的 uncertain outcome 做 fatal rejection。

### Independently Confirmed Checks

- 已完整读取用户点名的项目/CR/协议/696a12b0 基线材料和两仓 status；以
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 TeamReturn 单 request
  `CLICK_LEFT(delay=150)+SLEEP(500)`、NpcClick before/Ctrl DOWN/80/MOVE/280/after/RGB tolerance 15 + ratio 0.05/
  finally UP/100 为行为基线。
- 当前 30 个 TURN-28P 目标文件逐项 SHA 与原 delivery 报告完全一致：DHXY `19/19`、Cloud `11/11`，审查期间没有
  source 漂移。
- 双仓 8 组要求 byte-identical 的 protocol/test/fixture pair 全部一致：3 production protocol、2 protocol tests、
  3 JSON fixtures；其 SHA 分别为 `3d3dd1...`、`216c8f...`、`3929cf...`、`0fd97d...`、`7a60f6...`、
  `8ff499...`、`59ac71...`、`2fa30c...`。
- input 正向合同成立：`TurnInputSpec.java:13-27` 的两个字段 nullable 且保留七参数 constructor；
  `TurnProtocolValidator.java:171-220,458-460` 只允许 CLICK_LEFT/RIGHT 且各自 `[0,5000]`；
  `TurnInputActionMapper.java:30-57` 将 delay 放入 click，并只在 hold `>0` 时把一份 SLEEP 追加到同一 list；
  `TurnInputStepExecutor.java:60-67,166-177` 对整份 list 恰好一次 queue submission。
- `TurnInputStepExecutorContractTest.java:83-110` 穿透 production mapper/executor，明确证明
  `CLICK_LEFT(150)+SLEEP(500)` 同一 submission，right click、legacy null 与非 click rejection 也有覆盖。
- Cloud `TurnGameClient.java:161-168` 每次 invocation 只取一次 UUID、构造一次 action、调用一次
  `commandPort.execute`，目标 production 静态扫描未发现新增 retry/session/owner/ledger/TTL/compaction/durable
  workflow 或第二 requested frame/command/UUID。
- 除上述 findings 外，capture/outcome 的 changed/unchanged、finally release 尝试、release failure 优先级、唯一 after
  raw PNG、purpose/source/ROI/dimension/contentType/SHA/PNG signature/decoded-size fatal correlation 源码路径未发现另一个
  P0/P1/P2。

### Validation And Workspace Integrity

- 严格按用户禁令，未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI 或任何真实 capture/input。
- 未执行 checkout/reset/clean/add/commit/merge/rebase 等 Git mutation；两仓原有 dirty/untracked 均保留。
- 本次唯一写入是新报告
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-28P-delivery-reviewer-r1.md`；未修改任何
  Java、测试、fixture、原 TURN-28P 报告或其它既有文件。
- 复审门：先按父级重新冻结的精确写集修复两项 P1，并补 P2 named-test evidence；随后由适用责任方执行点名 tests/
  双仓 compile。当前 R1 结论不允许把 TURN-28P 视为 delivery review 通过。

<!-- TRUE_EOF: TURN-28P DELIVERY REVIEW R1 REVIEW_REQUIRED P0=0 P1=2 P2=1 019f6a15-8906-7331-aeb4-3f03aaeff31c 2026-07-16T05:01:19.420-04:00 -->
