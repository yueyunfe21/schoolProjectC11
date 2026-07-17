# TURN-28P Repair #1 Production PRECHECK

## PRECHECK 身份与边界

- 平台：Codex Desktop
- helper：Anscombe
- platform id：`019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142`
- 角色：TURN-28P Repair #1 非绑定 production-preflight helper；不实施 Java，不代替父级 delivery judgment。
- 源码快照时间：`2026-07-16 05:29:35 -04:00`。Maxwell 正在并发返修，以下文件/行号均指该时刻磁盘真实内容。
- 本轮仅写本报告；未运行 Maven/JUnit/compile/runtime/input，未执行 Git mutation，未修改 production、test、原卡报告或 Cloud 仓文件。

## PRECHECK 权威输入

- 原卡 `2026-07-16-turn-card-TURN-28P.md:415-496` 冻结父级 delivery review #2；其中 `:478-494` 要求通用 frozen exact-window exclusive queue、enqueue/focus 零 refresh、drift 时 callback/capture/Ctrl/MOVE 全零、started waiter completion barrier、probe checkpoints 与真实 queue/worker named coverage。
- 同卡 `:498-516` 将 Ctrl UP 非 `RuntimeException` release uncertainty 纳入同一 repair，并要求所有会被 outer worker 正常化的 UP throwable 先闭合为 `CTRL_RELEASE_FAILED`。
- R2 报告 `2026-07-16-turn-28P-delivery-reviewer-r2.md:28-75,76-120,121-153` 分别给出 refresh 混绑、terminal 早发布、非 Runtime UP throwable typed 丢失的独立源码证据。
- 当前 production 快照 SHA-256：
  - `InputActionQueue.java`：`95572c202d1cff73732fecebfb7710aa07dc770a27940b3a85577c212031866e`
  - `InputActionRequest.java`：`2c23ca1d7163d2a42c3f05552357fbafa9fa50e036f1b7b18ce6a3367329f595`
  - `InputActionWorker.java`：`3b8bc23d5639d8ddb471aaf8456d4d4d650c1be661813999b0116e52c8b4fb2d`
  - `InputSequences.java`：`fa2f17bfb8b0ab672e986abcadc7c316b0eff1d3c9781424f7839a1b0f06fdd2`
  - `WindowAwareInputCoordinator.java`：`4325fd1c7a428318ea0d27c4f7adcd8373e1005857ea1b884b61bb198c9332c4`
  - `TurnCaptureStepExecutor.java`：`2f4c1f09b7a70c07c104e183151d526ddb6f0584c5b02ae39940a2e3630f4ddc`

## PRECHECK 当前可保留形状

1. `InputActionQueue.java:319-346` 已出现通用、非业务命名的 frozen exclusive 入口；该方法本身没有调用 `refreshAndCommit`。legacy refresh 仍位于 `:596-623`，不得被 frozen 分支触达。
2. `InputSequences.java:65-83` 仅作一次 established facade 直委托；这一层是既有 queue ownership 边界，不构成 wrapper chain。
3. `InputActionRequest.java:27-61,206-257` 已复用现有 `windowContext/windowId/nativeBinding/playerIdentityEpoch`，只新增 `frozenExactWindow` mode；无需增加 owner/session/ledger/TTL 字段。
4. `InputActionRequest.java:839-867,876-886` 已覆盖 windowId、identity suspension/epoch、HWND、process 与 x/y/width/height；`InputActionWorker.java:112-166` 在 focus 与 callback 前调用 request safety gate。
5. `WindowAwareInputCoordinator.java:148-185` 的 frozen focus 直接使用显式 binding 调用 `focusWithoutLock`，没有 refresh/locate/title-search；legacy refresh 仍隔离在 `:188-216`。
6. `InputActionQueue.java:699-730` 对 frozen timeout/interruption 已采用 remove-first；remove 失败时只请求 cooperative cancellation，并 `join()` terminal result 后返回。
7. `InputActionRequest.java:557-575` 在 frozen step 已开始后只记录 cancellation、不发布 terminal；`:702-735` 将 cancel 与 step-start 放在同一 `progressLock`；worker 仅在 callback 返回或 throwable 已穿过 callback finally 后于 `InputActionWorker.java:196-216` 完成 request。
8. `TurnCaptureStepExecutor.java:227-233,298-354` 在 DOWN 调用前置 `ctrlDownInvoked=true`，finally 中只存在一个 UP 调用点；`:313-317,328-330` 已把 UP throwable/definite failure 写入 `releaseFailed`，`:363-366` 给予 `CTRL_RELEASE_FAILED` 最高投影优先级。
9. `TurnCaptureStepExecutor.java:283-293` 只采集一个 after raw image；`:385-396` 在 queue 返回后才编码/关闭该 after image。只要 terminal barrier 保持，after PNG 与任何失败投影均发生在 release attempt/settle 之后。

## PRECHECK 精确风险

### R-01：API 的 epoch 不是 action-resolved frozen epoch

- `TurnExecutionWindow.java:18-22,26-39,68-87` 冻结 context/binding/metadata，但没有冻结 identity epoch 字段。
- 当前 capture 在 `TurnCaptureStepExecutor.java:206-211` 将 `window.binding()` 与调用当下现读的 `window.context().getPlayerIdentityEpoch()` 混合传入。
- queue 在 `InputActionQueue.java:330-339` 接受该任意 `long`，request 在 `InputActionRequest.java:217-227` 原样保存。若 resolve 后发生同 HWND identity commit，旧 binding 与新 epoch 可组成从未原子存在过的 snapshot。
- `WindowRuntimeContext.java:166-208` 在同一 synchronized binding commit 中处理 native/player drift；`WindowNativeBindingRefreshService.java:80-84` 则在 context monitor 内提交并返回 context 当前 binding。`TurnExecutionWindow.java:68-87` 保存的正是该返回对象。

收敛接口：公共 facade/queue API 不接收 caller-supplied epoch；queue 在 `synchronized(context)` 内确认 `context.getNativeBinding() == frozenBinding`，同时核 exact fields、windowId、非 suspended，然后读取并写入 request 的 `playerIdentityEpoch`。对象身份只作为“该 epoch 与 action binding 同代”的 generation witness，正式 worker gate 仍核 windowId/HWND/process/rect/epoch。这样不新增 snapshot DTO，也不扩写 `TurnExecutionWindow`。

### R-02：final worker check 与 callback 之间仍有 binding-commit TOCTOU

- worker 当前顺序是 safety check `InputActionWorker.java:112-115`、transaction 内 check/focus `:121-139`、再次 check 后 callback `:141-166`；这些步骤没有持有 context monitor。
- coordinator 在 `WindowAwareInputCoordinator.java:168-175` 自己再读 mutable context 并校验，随后 `:179` focus；request 在 `InputActionRequest.java:839-867` 又有另一套校验。任一校验返回后、下一 capture/Ctrl/MOVE 前仍可发生 commit。
- 所有正常 binding commit 已由 `WindowRuntimeContext.java:166-208` 与 `WindowNativeBindingRefreshService.java:80-84` 使用同一 context monitor 串行化。

收敛调用流：frozen request 在进入 global input transaction、完成 pause/cancel precheck 后，worker 才取得 `synchronized(request.getWindowContext())`；在该 monitor 内依次做唯一 authoritative exact check、显式 frozen focus、`tryStartStep`、`InputActionScope.callWith(callback)`，并让 callback 的 Java finally 完整退出后才释放 monitor。不要在排队、入队前 pause 或等待 global queue 时持有该 monitor。已提交 drift 在 worker 入 monitor后立即拒绝，callback/capture/Ctrl/MOVE 为零；callback 期间的 refresh commit 等到 release/settle 结束后再提交。

### R-03：boolean facade 丢失现有 typed stop 结果

- `InputActionExecutionResult.java:19-29` 已携带 `status/safetyReason/reason`。
- frozen detector 在 `InputActionRequest.java:850-853` 已把 stop token 关闭分类为 `STOP_REQUESTED`。
- 但 queue `InputActionQueue.java:330-346` 与 facade `InputSequences.java:75-83` 只返回 boolean；capture `TurnCaptureStepExecutor.java:203-211,363-383` 因而只能依赖 callback state、frozen metadata 或 caller interrupt。callback 尚未开始时的 typed stop 会被压成 `submitted=false`，存在落入 `PIXEL_PROBE_FAILED` 的空间。

收敛接口：复用既有 `InputActionExecutionResult` 作为 frozen API 返回值，不新增 outcome DTO。capture 在 queue 已完成 cleanup barrier 后按固定顺序投影：`releaseFailed`；callback/caller stopped 或 `safetyReason==STOP_REQUESTED`；queue 非 completed；probe mechanics failure；最后才允许 changed/unchanged + after frame。waiter interruption仍由 `InputActionQueue.java:718-730` 在 join 后恢复 caller interrupt。

### R-04：nullable callback 可退化成空 action 成功

- frozen factory `InputActionRequest.java:245-257` 未拒绝 null callback；`hasExclusiveCallback()` 在 `:377-381` 仅检查非 null。
- 若 exact snapshot 有效而 callback 为 null，worker 会跳过 `InputActionWorker.java:150-167` 的 callback 分支，再以空 action list 从 `:168-197` 返回成功。

收敛接口：在 frozen queue public boundary 对 context、binding、callback 使用 `Objects.requireNonNull`；description 保持现有 nullable-normalized 语义。不要为该校验再加 wrapper/helper。

### R-05：request/coordinator 的 exact comparator 已重复

- request comparator 位于 `InputActionRequest.java:876-886`；coordinator comparator 位于 `WindowAwareInputCoordinator.java:227-237`。
- 两套字段清单未来可独立漂移，也使 worker 的“先校验、再 focus”所有权不清晰。

收敛接口：request 是唯一 exact-state authority；worker 在 context monitor 内调用它。coordinator 的 frozen 方法只接收 `actionName/windowId/frozenBinding`，验证 active transaction 后做 best-effort `focusWithoutLock(binding)` 与 metrics，不再读取 context、不再维护第二套 comparator。

## PRECHECK 最小通用 API

```text
InputActionExecutionResult submitFrozenExactWindowExclusiveAndWait(
        String description,
        WindowRuntimeContext context,
        WindowNativeBinding frozenBinding,
        Supplier<Boolean> callback)
```

- `InputSequences`：同签名直委托 `InputActionQueue`，无第二个 helper。
- `InputActionQueue`：在 context monitor 内把 action binding 与当前 generation 对齐并冻结 epoch；零 refresh；之后进入现有 `await(request)`。
- `InputActionRequest`：继续使用 `frozenExactWindow`、现有 frozen binding/epoch/progress/cancellation/terminal 字段；不增加 deadline、TTL、retry、owner、session、ledger 或 durable state。
- `InputActionWorker`：单 global queue ownership 不变；仅 frozen branch 在最终 admission 到 callback finally 期间持有已有 context monitor。
- `WindowAwareInputCoordinator`：只 focus 显式 frozen binding；legacy current-window refresh 方法保持原路径。
- `TurnCaptureStepExecutor`：只传 context + action-frozen binding；不再现读 epoch；继续使用同一 `window.binding()` 做 capture、Ctrl DOWN/UP 与 absolute MOVE。

## PRECHECK cancellation/terminal 不变量

1. request 尚在 queue：`queue.remove(request)==true` 后才可 `cancel()` 并发布 NOT_STARTED；此后 mechanics 为零。
2. remove 失败：queue 不得调用会立即发布 terminal 的路径，只设置 cooperative cancellation 并等待同一 request future。
3. `tryStartStep(0, ...)` 与 cancellation 必须继续共用 `progressLock`；cancel 先提交则 callback 不启动，start 先提交则任何 cancel 只置 cancellation state。
4. started callback 的唯一 terminal publisher 是 worker 在 callback 返回或 throwable 穿过 callback finally 之后执行的 `complete(...)`；waiter interruption/现有 120 秒 timeout 都必须 `join()` 此结果。
5. timeout 仍只是既有 waiter cancellation 点，不转成 action deadline/TTL；不得新增计时字段或 retry。
6. `InputActionScope.checkpoint()` 可以请求退出，但 finally 内不得调用 checkpoint。Ctrl DOWN 已 invoked 时，finally 始终尝试且仅尝试一次 UP，再完成既有 `ctrlUpSettleMs`。

## PRECHECK Ctrl UP typed 与证据顺序

- outer worker 仍在 `InputActionWorker.java:201-206` 正常化 `TaskStopRequestedException` 与任意 `Throwable`，因此 UP transition 的 `RuntimeException` 和 `Error` 都必须在 callback finally 内先写 `releaseFailed`。当前 `TurnCaptureStepExecutor.java:302-317` 的 `catch (Throwable)` 与该 policy 对齐，不应缩窄为仅 `RuntimeException`。
- UP 返回 attempted/success false 同样必须保持 `releaseFailed`，证据为 `TurnCaptureStepExecutor.java:309-312,328-330`。
- release uncertainty 的 closed code 优先于 stop、queue failure、probe failure 与 completed payload，证据为 `TurnCaptureStepExecutor.java:363-383`。
- after raw image 只允许 `TurnCaptureStepExecutor.java:283` 一次 capture；`:291-292` 转交 state，`:385-396` 在 queue barrier 后编码/flush。不得在 callback finally 前新增 failure-evidence capture、PNG encode 或 Cloud result publication。
- Cloud production/protocol 不随此 API 改动；原卡 `:448-453,489-492,503-514` 点名的 probe command/outcome uncertainty 与 fatal correlation 仅由既定 named tests穿透验证，不能用 retry、mock result 或 fabricated frame 绕过。

## PRECHECK 文件落点

- `InputActionQueue.java:319-346,626-750`：收窄 API、queue-side generation capture、structured result、保留 remove-first + join。
- `InputActionRequest.java:27-61,206-257,557-575,634-735,839-886`：冻结字段、exact gate、start/cancel 原子性与 terminal 延迟。
- `InputActionWorker.java:112-166,196-216`：frozen monitor/admission/focus/callback/finally 的单段 ownership。
- `InputSequences.java:65-83`：唯一 facade 直委托。
- `WindowAwareInputCoordinator.java:148-185,227-237`：显式 binding focus，删除重复 mutable-context exact policy。
- `TurnCaptureStepExecutor.java:203-396`：同 binding mechanics、typed stop/result、唯一 after image、release-first projection。

<!-- TRUE_EOF: TURN-28P REPAIR-1 PRODUCTION PREFLIGHT PRECHECK_COMPLETE Anscombe 019f6a15-c5cb-7d71-b1b1-cd1f1dd2e142 2026-07-16T05:29:35-04:00 -->
