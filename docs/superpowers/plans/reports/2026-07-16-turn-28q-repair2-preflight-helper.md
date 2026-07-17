# TURN-28Q Repair #2 Preflight Helper

## 角色与结论边界

- 角色：CR271 内部 repair-preflight helper，只冻结最小可实施切片；不是 implementation owner、reviewer、
  parent/final reviewer，也不批准 TURN-28Q。
- 本报告开始读取时，TURN-28Q 原卡物理 EOF 尚为 Parent Review #2 `0/0/0`；读取期间父级追加了
  `PARENT ADJUDICATION / SOURCE REVIEW #3`。已重新读取新的物理 EOF，当前权威状态是
  **`P0/P1/P2=0/4/0 / REPAIR #2 REQUIRED`**（原卡 `:399-435`），Review #2 已被覆盖。
- 用户点名的三项现已由父级分别登记为 P1-2 frozen Alt、P1-3 paused cancellation、P1-4 pause proof。
  本 helper 独立复核后结论：**三项均为当前源码上的真实问题，可实施，不能仅靠改断言关闭。**
- R1 物理 EOF 还提出了 typed stop closure；父级已作为独立 P1-1 采纳。它与本三项共享 Worker/test，
  因而不能再把“三项”拆给第二个并发 writer。Repair #2 必须保持原卡冻结的单 owner 四文件 allowlist。

## 读取切面

- 已完整读取：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划
  第 14-19 节、HTTPS turn 协议、TURN-28Q 原卡最新物理 EOF、R1/R2 最新物理 EOF，以及用户点名的
  `InputActionWorker.java`、`BoundWindowKeyboardService.java`、`InputActionQueue.java`、
  `TaskPauseToken.java`、`InputActionFrozenExclusiveContractTest.java`。
- 当前关键快照：

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `InputActionWorker.java` | 748 | `7489084b773e6066213d383af86c82ac9c3431fb9e2d1d5acf3e9c11d423eac0` |
| `BoundWindowKeyboardService.java` | 354 | `37d97cfb569bcca49d0b955d0ec462bf811ef7c49fda156f6457b5642f1330fe` |
| `InputActionQueue.java` | 850 | `66fa536ef8b4c6cbf8874cd94d8842fd8b0f9d3f4e74bc52719f31f39e4660bf` |
| `TaskPauseToken.java` | 184 | `6706360e84be8ed7888225ead3212739ff74b4f71fee20a55c7cc9cde8538604` |
| `InputActionFrozenExclusiveContractTest.java` | 852 | `475399ef8656c7d193bfeb6f18ba69b7e01d4c531710367e74d00165ded03c44` |
| TURN-28Q 原卡（Review #3 后） | 435 | `95c2afdd1e4b03cb051dff5f3f46787ce66bef7cd38683432be178b0821e2f5d` |

## 三项核实

### 1. Frozen Alt exact binding：确认成立

精确证据：

1. `InputActionWorker.java:653-666` 将仅含受支持 Alt 与 `SLEEP` 的 list 选为 background keyboard。
2. frozen list 虽先在 `InputActionWorker.java:483-493` 持有 context monitor 并 exact-focus，但 Alt dispatcher
   在 `:556-563` 调用的是 `boundWindowKeyboardService.pressShortcut(shortcut)` 可变 context overload。
3. 该 overload 在 `BoundWindowKeyboardService.java:65-95` 读取 current context 并执行
   `refreshAndCommit(context)`。同类已经有完全够用的无 refresh exact overload：`:98-108` 的
   `pressShortcut(binding, windowId, shortcut)`。
4. background 未执行或非 terminal 失败时，Worker `:578-591` 又调用 mutable-current
   `focusCurrentWindowInActiveTransaction(...)`；该路径会重新解析/刷新 current binding，而 frozen factory
   没有 deadline，`:585-588` 的 after-focus check 也不会执行。
5. 所以单 Alt 可以在自行替换 frozen binding object 后仍完成；Alt+后续 action 则可能先向旧/新混合目标发 Alt，
   到下一 action 才发现 drift。R1 与 R2 对此结论一致。

最小 production 修复冻结在 `InputActionWorker.pressAltShortcut(...)` 内：

- `request.isFrozenExactWindow()` 时只调用现有 exact keyboard overload，传入 request 已冻结的同一个
  `WindowNativeBinding` object 与 `windowId`；不得调用 context-resolving overload。
- 需要 real-input fallback 时，只调用现有 `focusFrozenBindingInActiveTransaction(...)`，不得调用 mutable-current
  focus；fallback 前后都必须先保留 typed safety，再确认 exact generation，任何 drift/stop/cancel 都在 real input
  前终止。
- 非 frozen 请求继续走原 mutable refresh/focus 语义；不得顺手改变普通队列 keyboard 行为。
- `BoundWindowKeyboardService.java` 与 `WindowAwareInputCoordinator.java` 不改；需要的 exact 原语已经存在。

### 2. Paused cancellation completion：确认成立

精确证据：

1. frozen action-list factory 在 `InputActionRequest.java:278-289` 不带 deadline，所以 Worker 进入
   `InputActionWorker.java:378-386` 的 non-deadline pause 分支。
2. 该分支调用 `TaskPauseToken.waitIfPaused(stopToken)`；其实现 `TaskPauseToken.java:71-96` 只观察 resume、
   stop token 和 worker interrupt，不观察 `request.isCancelled()`。
3. waiter 在 Worker 已 take request 后被中断时，`InputActionQueue.java:793-805` 会调用
   `requestDetailedCancellation("waiter interrupted")`，随后立即 `request.getResult().join()` 等 Worker terminal。
4. cancellation 只设置 cooperative flag，不会 resume pause token；因此 Worker 可继续卡在 pause wait，持有全局
   input transaction 与 context monitor，waiter 又卡在 join，形成确定的 completion barrier hang。
5. 已有 `TaskPauseToken.waitIfPausedRevision(stopToken, wakeCondition)`（`:107-137`）会在 pause 仍为 true 时观察
   wake condition；`InputActionRequest.shouldAbortPauseWait()` 已把 request cancellation 纳入 wake 条件。无需新 API。

最小 production 修复同样只在 `InputActionWorker.waitIfPaused(...)`：

- 仅对 `frozenExactWindow && !hasExclusiveCallback()` 的 non-deadline action-list 分支复用
  `waitIfPausedRevision(request.getStopToken(), request::shouldAbortPauseWait)`。
- wake 后沿用现有 `request.isCancelled()` / worker interrupt 返回门，由 Worker 冻结 truthful terminal result，
  使 queue waiter 的 join 完成；pause token 不需要也不得被伪 resume。
- 不释放 context monitor/input transaction，不重入 queue，不创建新 request，不重放已完成 prefix。
- 保持 frozen callback 与普通 legacy request 的现有 pause/completion 边界；`InputActionQueue.java` 和
  `TaskPauseToken.java` 对本项均只读。

### 3. Pause-test race：确认成立

精确证据：

- 当前用例 `InputActionFrozenExclusiveContractTest.java:458-508` 在 first-click hook 的 `:471-475` 请求 pause
  后立即 count down；主测试线程在 `:489` 只等这个 callback 内 latch，`:491-493` 检查 calls，随后 `:495`
  resume。
- latch 不来自 Worker 的下一 action pause gate。主线程可以在 Worker 从 click provider 返回前就 resume；即使删掉
  per-action pause gate，该测试仍存在通过窗口。它不是 `Thread.sleep`，但仍是 scheduling false positive。

最小 test 修复：

- 在现有 named test 内增加 test-private `TaskPauseToken` subclass/decorator，override Worker 实际调用的
  `waitIfPausedRevision(...)`，在方法真实入口 count down `pauseGateEntered`，然后委托 `super`。
- first-click hook 只请求 pause；主线程必须等待 `pauseGateEntered` 后才断言 later provider action=0，再 resume。
- 不加 production test hook，不读 private field，不用 `Unsafe`、源码扫描、`Thread.sleep`、queue-size polling 或
  线程状态猜测。旧 callback fixture 的历史 `Unsafe`/sleep polling 不在本切片重构范围。

## 最小且不双写的 Write Set

父级 Review #3 已冻结 Repair #2 exact allowlist，helper 不得缩成会漏掉 P1-1 的两文件卡，也不得扩写：

| 类别 | 文件 | Repair #2 唯一职责 |
|---|---|---|
| Production | `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | 父级 P1-1：generation-only witness 与 typed safety 分离；不是本三项的新 abstraction |
| Production | `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | 父级 P1-1：pre-enqueue typed stop 不得翻译为 drift；保留现有 waiter completion ownership |
| Production | `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | P1-1 final/step safety、P1-2 exact Alt、P1-3 cancellation-aware frozen pause |
| Test | `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | 四项 P1 的 deterministic real public facade/queue/worker 验收 |
| Process | TURN-28Q 原卡 | 仅 implementation owner 按父级要求 append claim/delivery；不是本 helper 的可写卡 |

三项点名问题本身的实际代码增量只有 Worker + named test；但父级已采纳的 P1-1 也必须改 Worker/test，故两组
在文件级不 disjoint。**唯一安全排班是一个 Repair #2 owner 串行修改上述四文件；不得另派第二 writer 处理 stop。**

明确只读：`InputSequences.java`、`BoundWindowKeyboardService.java`、`WindowAwareInputCoordinator.java`、
`TaskPauseToken.java`、所有 caller/Task/Service/protocol/Cloud 文件、POM/config/resources。已有 exact keyboard/focus、
revision wait 和 queue cancellation barrier 必须复用，不新建 wrapper/helper/result/token 文件。

## Named Test 验收冻结

所有新增/修复 case 必须继续走真实 public
`InputSequences -> InputActionQueue -> InputActionWorker`，使用 in-memory provider/fake keyboard；不得发送真实
`PostMessage`、桌面输入或 capture。至少闭合：

1. stop 在 public facade 前已关闭：`NOT_STARTED/STOP_REQUESTED`，take/focus/input/refresh 全为 0。
2. stop-only 在 action 0 内关闭：一份 taken request，truthful completed prefix，typed `STOP_REQUESTED`，later action=0。
3. stop 在 final action 内同步关闭：不得发布 `COMPLETED`，不 replay final action。
4. supported Alt background success：一份 request/transaction，exact overload 收到同一 frozen binding object，
   refresh=0，mutable overload=0，real provider input=0。
5. Alt non-attempted/failed fallback：只 exact-focus 原 binding，refresh=0；若 keyboard fake 在返回前制造 drift，
   必须 typed `WINDOW_BINDING_CHANGED` 且 fallback real input=0。
6. paused waiter cancellation：先由 observable pause-gate-entry latch 证明 Worker 已阻塞，再 interrupt/cancel waiter；
   不调用 resume 也必须得到 non-completed typed terminal、one take、truthful prefix、later action=0，Worker 可继续取请求。
7. pause/resume：等待实际 pause-gate entry 后才检查 later action=0；resume 后同一 request id/one take 继续，prefix
   不 replay，exact focus 与 refresh 计数不变。
8. 保留既有 immutable `[CLICK_LEFT(delay=150), SLEEP(500)]` one-take 成功证据、A->B->A typed rejection 和
   frozen callback finally/completion barrier non-regression。

授权后的精确 gate：

- DHXY named test：`mvn -q -Dtest=InputActionFrozenExclusiveContractTest test`
- DHXY Java compile：`mvn -q -DskipTests compile`
- 两者必须在所有并行 Java writer 稳定后由负责 gate 的 owner fresh 执行并记录 exit code/tests/failures/errors；
  本 helper 未执行。不得用 `-DskipTests` 跳过 named test，也不得用 runtime 代替 contract test。

## 禁止扩张与基线

- 不新增自动 retry、re-enqueue、replay、session、owner、operation ledger、TTL、deadline 或 durable workflow。
- 不新增第二 result DTO、pause token、binding comparator、refresh、focus abstraction、keyboard adapter 或 wrapper chain。
- 不改变一个 request/一个 global input transaction/一个 frozen context generation monitor；pause/resume 与 cancellation
  都不得拆 list。
- 不改变 `696a12b0` 的一次 queue `CLICK_LEFT(delay=150) -> SLEEP(500)`，不改 OCR、template、FIFO、phase、park、
  retry/fallback、业务决策或 protocol count。
- HTTPS turn 仍遵守一次显式调用一个 actionId、uncertain 不自动重发、Cloud 决定下一 action；本修复只闭合 DHXY
  frozen physical mechanics。

## 本 Helper 操作边界

- 未修改 Java、测试、原卡、计划、`ACTIVE_WORK`、CR271 matrix/dashboard 或任何既有 dirty/untracked 文件。
- 未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI、capture 或任何输入动作。
- 未执行 commit/stage/branch/merge/rebase/checkout/reset/restore/clean 等 Git mutation。
- 本报告是唯一写入，不构成 claim、delivery、review 或 approval。

TRUE_EOF PRECHECK_COMPLETE
