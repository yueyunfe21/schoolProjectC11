# TURN-28P Independent Delivery Review R2

## REVIEW CLAIMED - 2026-07-16T04:44:49.773-04:00

- Platform id: `019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142`
- Nickname: `Codex / TURN-28P-R2`
- Role: independent delivery reviewer R2; not the implementation Worker and not the parent/final reviewer.
- Scope: read-only independent review of TURN-28P production and named-test source in DHXY and Cloud, with findings appended only to this report.
- Restrictions acknowledged: no Java/test/existing-report edits, no Maven/runtime, no Git mutation, and no rollback/overwrite/cleanup of dirty or untracked work.

<!-- TRUE_EOF: TURN-28P R2 REVIEW CLAIMED Codex 019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142 2026-07-16T04:44:49.773-04:00 -->

## REVIEW IDENTITY CORRELATION

- 权威计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:408-410`
  将本 platform id 对应的 nickname 记为 `Anscombe`。初始 claim 中的
  `Codex / TURN-28P-R2` 是本 reviewer 的角色标签；本报告最终身份以
  `Anscombe / 019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142` 为准。

## R2 REVIEW RESULT - 2026-07-16T05:06:52.171-04:00

- 结论：`REPAIR REQUIRED`。
- 精确计数：`P0/P1/P2 = 0/2/1`。
- 本结论仅是独立 delivery reviewer R2 的交付审查意见，不作父级批准或关闭裁决。

## Findings

### P1-1：真实 queue/focus 路径在 action 快照冻结后又刷新两次，exact HWND/process/ROI/geometry 不成立

**文件/行证据**

1. `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java:18-22,42-47,68-87`
   明确把一次 `refreshAndCommit` 后的 binding、process、windowRect 冻结为整份 action 的 immutable snapshot，
   并声明后续 mechanics 不得再次 resolve/refresh。
2. probe 在 `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java:204-208`
   把 `window.context()` 交给 `InputSequences.submitExclusiveAndWait`；普通 mouse input 同样从冻结 rect 映射坐标后，
   在 `TurnInputStepExecutor.java:166-177` 调真实 queue。
3. 真实 `InputActionQueue.submitExclusiveAndWait` 却在
   `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java:303-315` 再调用
   `refreshAndValidateNativeBinding`；该方法在 `:554-594`，尤其 `:573`，再次
   `bindingRefreshService.refreshAndCommit(context)` 并修改 mutable context。普通
   `submitAndWait` 在 `:67-80` 也有同一额外刷新。
4. worker 随后在 `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java:119-152`
   对 exclusive callback 先做 focus；focus 又在
   `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java:147-170`，尤其 `:159`，
   第三次 `refreshAndCommit(context)`。之后 callback 仍使用第一次冻结的
   `window.binding()/window.metadata().windowRect()` 做 capture、Ctrl 与绝对 MOVE
   (`TurnCaptureStepExecutor.java:213-278,290-295`)。
5. named tests 绕过了这条 production path：
   `TurnCapturePixelChangeProbeContractTest.java:357-377` 和
   `LocalTurnActionExecutorContractTest.java:900-913` 都在 caller 测试线程同步执行 callback；
   `TurnInputStepExecutorContractTest.java:348-360` 也完全 override 真实 queue submission。

**影响**

- 窗口在三次刷新之间移动/缩放时，protocol metadata、capture base/ROI、Ctrl binding 与 mouse 绝对坐标仍属于
  第一次快照，而 worker focus 使用后来写入 context 的 geometry。鼠标可落到旧屏幕位置，返回像素/输入却被标记成
  第一次快照的 exact action 结果。
- HWND 数值被系统复用时，第二次刷新可把新 process/title 接受进 context/request，而 probe 仍上报第一次快照的
  process/window metadata；没有 snapshot-vs-live 的 HWND/process/rect 等值门。这是 Cloud 无法从 multipart
  correlation 检出的本机来源错配，也破坏多窗口物理输入安全。
- 同一缺口同时影响本卡新增的 click timing input：坐标先按冻结 rect 校验/映射，enqueue/focus 后 geometry 可漂移，
  即使 click delay 与 hold 的单 queue ownership 本身正确，点击对象仍不再是被冻结的 exact target。

**返修条件**

1. 父级先明确扩大/调整写集；当前冻结写集把 `InputActionQueue`、`InputActionWorker`、
   `WindowAwareInputCoordinator` 作为只读，不能用 TURN-28P 文件内的旁路伪造 exact queue ownership。
2. 为 turn action 提供真正的 frozen-binding queue admission/focus 路径：`TurnExecutionWindow` resolve 后不得再
   refresh/locate；queue request 和 focus 必须使用同一 frozen HWND/process/rect。若必须做 live validation，则只能
   比较且在任何 capture/Ctrl/mouse 前 fail closed，不能 commit 新 binding 后继续旧坐标 mechanics。
3. named test 必须穿透 production queue + worker/focus admission（mechanics 仍用 fake），让 refresh fake 在第二次调用
   改 geometry/process，证明 refresh 总次数为一次，或证明 drift 在 before capture/Ctrl DOWN/mouse 前终止；probe 与
   click timing 两条路径都要覆盖。

### P1-2：started exclusive callback 的 waiter cancellation 会提前发布 terminal，STOP/失败可早于 Ctrl UP

**文件/行证据**

1. probe 使用 legacy、无 deadline 的 `submitExclusiveAndWait`
   (`TurnCaptureStepExecutor.java:204-208`；`InputActionQueue.java:303-316`)。worker 在
   `InputActionWorker.java:146-156` 标记 step started 后直接运行 callback，callback 内没有 request cancellation
   checkpoint。
2. waiter 超时在 `InputActionQueue.java:669-674`、waiter interruption 在 `:678-697`；legacy 分支即使
   `queue.remove(request)` 为 false（已被 worker 取走/正在执行），仍调用 `request.cancel(...)` 后立即 join 已完成的
   result 并返回。
3. `InputActionRequest.cancel` 在
   `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java:506-520` 立即置 terminal 并
   `result.complete(...)`，并不等待 worker callback 的 `finally`。真正 callback 返回及 worker terminal/finally 位于
   `InputActionWorker.java:152-198`，可晚于 waiter 已观察到的返回。
4. `TurnCaptureStepExecutor.java:340-376` 把 queue 返回当作 probe 已结束并读取普通、非 volatile 的
   `ProbeState`；FAILED 且要求 evidence 时，`LocalTurnActionExecutor.java:117-128` 随即抓 full-window frame。
5. named interruption cases 只让同步 fake callback 所在的 caller 线程自中断
   (`TurnCapturePixelChangeProbeContractTest.java:184-199,357-377`；
   `LocalTurnActionExecutorContractTest.java:385-400,900-913,978-996`)；它们没有独立 waiter/worker，无法制造
   “callback 已 started、waiter 被 interrupt/cancel”竞态。

**影响**

- 正常 stop 会中断 task/waiter（`RunningTaskHandle.java:227-245`）。若 callback 已 started，task 可先投影并返回
  STOPPED，而 input worker 仍可能处于 Ctrl DOWN、MOVE、after capture 或 Ctrl UP settle；这不满足“stop/
  interruption 只能在 release 后投影”。
- 120 秒 legacy waiter timeout 或 wait failure 若发生于卡住的 HWND capture/native release，`submitted=false`
  会先投影 `PIXEL_PROBE_FAILED`；`fullWindowFailureEvidence=true` 随后可在 worker 尚未执行 Ctrl UP 时抓证据，直接
  违反 failure-evidence-after-release 合同。
- 提前完成 future 也切断正常的 worker-completion happens-before；上层会和 worker 并发读写 `ProbeState`，已转移的
  `afterImage` 还可能在上层 finally 检查之后才写入，形成不确定结果/资源生命周期。

**返修条件**

1. exclusive callback 一旦 `tryStartStep` 成功，terminal publication 必须由 worker 拥有；waiter interruption、stop、
   timeout 或 wait failure只能请求 cooperative cancellation，不能在 callback `finally` 完成 Ctrl UP 尝试与 settle
   前让 `submitExclusiveAndWait` 返回。调用线程的 interrupted flag 可在 cleanup barrier 后恢复。
2. probe callback 的 stop/cancellation checkpoints 与最终 projection 必须保证：不再执行可跳过的后续 mechanics，
   但无论何处停止都先完成唯一 Ctrl UP 尝试；release 未确认仍优先 `CTRL_RELEASE_FAILED`。failure evidence 只能在该
   worker-owned cleanup barrier 之后。
3. 增加并发 named fixture：真实 request/worker ownership、fake capture/keyboard/input；在 Ctrl DOWN 后阻塞 callback，
   分别 interrupt/cancel waiter，并断言 waiter/outcome/evidence 在 `key:UP` 与 up-settle 前均不能出现，UP 恰好一次，
   无 nested queue/死锁。另需可控模拟 running-callback timeout/wait failure，不能用 120 秒真实等待。

### P2-1：非 RuntimeException 的 Ctrl UP failure 被 outer worker 正常化后丢失 `CTRL_RELEASE_FAILED`

**文件/行证据**

1. `TurnCaptureStepExecutor.java:285-325` 对 Ctrl UP 的异常边界只 catch `RuntimeException`
   (`:300-302`)；因此 `Error`/其它 `Throwable` 会越过 `state.releaseFailed=true`。
2. 但 outer worker 明确在 `InputActionWorker.java:194-196` catch `Throwable` 并把它转换成普通 failed request。
   上层随后经 `TurnCaptureStepExecutor.java:355-363` 返回 generic `PIXEL_PROBE_FAILED`。
3. 冻结合同 `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md:111-113`
   要求任何未确认成功的 release 必须 typed `CTRL_RELEASE_FAILED`；现 named release tests
   (`TurnCapturePixelChangeProbeContractTest.java:162-181`) 只覆盖返回失败和 `RuntimeException`。

**影响**

- 该路径仍会丢弃 completed probe frame，故不升级为 P1；但 native/JNA linkage 等被 outer worker 吞并继续运行的
  non-Runtime failure 会被误报为普通 mechanics failure，Cloud/日志无法识别 Ctrl 可能仍处于按下状态，违反闭口
  typed release-uncertainty contract。

**返修条件**

1. 对齐两层 throwable policy：凡 outer worker 会转换为普通 request failure 的 Ctrl UP throwable，probe 必须先记录
   release uncertainty 并投影 `CTRL_RELEASE_FAILED`；若项目决定某类 JVM-fatal Error 必须透传终止，则 outer worker
   也不能把同类 Error 正常化后继续。
2. named fake 增加一个与最终 policy 一致的 non-Runtime UP failure，证明无 changed/unchanged/frame，且 typed code
   为 `CTRL_RELEASE_FAILED`。

## Verified Controls / Non-findings

- 两仓当前 30 个 production/test/fixture 目标 SHA-256 与原卡 `:329-360` 全部逐项一致；本 review 期间没有发现交付
  源被并行改写。双仓三个 protocol production、两个 protocol tests 和三个 JSON fixtures 继续 byte-identical。
- nullable `clickDelayMs/queueHoldMs` 仅允许 CLICK_LEFT/RIGHT；mapper 生成
  `CLICK(delay)+SLEEP(hold)`，executor 对整份 list 一次 submit。该局部映射保持 `696a12b0` 的
  `clickLeft(...,150)+sleep(500)` 顺序，无第二 command/UUID/retry。
- 在绕过真实 queue lifecycle 的同步 callback 内，before、Ctrl DOWN、80ms、unscaled MOVE、280ms、after、compare、
  finally Ctrl UP、100ms 顺序正确；正常 RuntimeException/typed failure 会丢弃 completed frame，唯一上传图为 after raw
  PNG。
- Cloud `TurnInvocationResult.java:78-170,189-215` 对 action/device/window/step、plain-vs-probe code、purpose、
  source step、ROI、dimensions、content type、SHA、PNG signature 和 decoded dimensions 均 fatal；本轮未发现 Cloud
  correlation、单 UUID/command 或 raw-frame defensive-copy 的 P0/P1/P2。
- `WindowIdentityDriftP2WiringTest.java` 的旧 `ALT_U(...,false)` 与 production 不一致已被原卡 `:369-373` 明确列为
  TURN-28P 前既有且范围外；本 reviewer 不重复计入本卡 findings。
- 对照 `docs/业务逻辑.md` 的 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` baseline：无已批准业务差异；
  上述 findings 是 queue/correlation/terminal plumbing 缺口，不授权改变 phase、probe 判定、RGB tolerance 15、ratio
  0.05、输入顺序或 Cloud 业务判断。

## Verification / Mutation Record

- 已完整读取用户点名 authority、原卡最新 append（含 04:48 parent source review）、两仓 status，以及 TURN-28P
  production/named-test source；另只读追踪真实 queue/request/worker/focus/stop 调用链。
- 未运行 Maven、JUnit、compile/package、runtime/application/server/Task/UI/capture/input；这是用户本轮明确禁令，
  因而本报告不声称任何测试或构建通过。
- 未执行 Git mutation；未修改 Java、测试、原卡/既有报告；未回滚、覆盖、清理、暂存或提交任何 dirty/untracked。
  本 reviewer 唯一写入仍是本新报告。

## R2 Decision

`REPAIR REQUIRED: P0/P1/P2 = 0/2/1`。

在 P1-1 exact frozen-binding queue/focus 与 P1-2 worker-owned cleanup/terminal barrier 完成返修并由新 named fixtures
穿透真实 lifecycle 证明前，TURN-28P delivery 不满足冻结合同。P2-1 亦须按同次返修关闭。本行不是父级最终状态，
不作父级批准或关闭裁决。

<!-- TRUE_EOF: TURN-28P INDEPENDENT DELIVERY REVIEW R2 REPAIR_REQUIRED P0P1P2=0/2/1 Anscombe 019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142 2026-07-16T05:06:52.171-04:00 -->
