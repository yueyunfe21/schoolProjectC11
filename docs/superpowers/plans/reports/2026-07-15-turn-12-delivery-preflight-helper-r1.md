# TURN-12 Delivery Preflight Helper-R1

## PRECHECK_CLEAR

- 时间：`2026-07-15T16:31:00-04:00`。
- 角色边界：本报告仅为非绑定 delivery preflight / second-look source risk review；Helper-R1 不是
  manager/reviewer，不写 `APPROVED`、`BLOCKED` 或 `CLOSED`，不派任务。
- 审查材料：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、HTTPS turn
  权威主计划、thin-client protocol spec、TURN-12 原报告、三份交付源码，以及只读上游
  `TurnClient` / `HttpsTurnClient` / `TurnExchangeResult` / `ExecutedTurn` / protocol validator 合同。
- 交付 SHA-256 与 TURN-12 原报告一致：
  - `WindowTurnLoop.java`：`A515378DBE3BB8051EB0CEC58AC3A341C467B6DD3A50E8E9EDDB6EE5EFA0AB1A`
  - `TurnLoopRegistry.java`：`8D3AFE1BEBC7F51BED3C7102C07E553651CEF505BD94CB7257537575ACADD067`
  - `TurnLoopFactory.java`：`48094621EF0E57CA8268964EE2D2DB8B8F3A637DC18BE858603DC8AFB4971076`

## Second-look evidence

1. **ACK 与 previous 清理边界**：`WindowTurnLoop.java:176-196` 每轮只读取一次 live metadata、构造一次
   request、调用一次 `turnClient.exchange(...)`；只有该调用正常返回 `TurnExchangeResult` 后才同时清空
   `previousOutcome` / `previousPng`。`TurnClient.java:10-18` 将成功返回定义为 validated response 且 previous
   ACK accepted；`TurnExchangeResult.java:17-21` 又拒绝任何非 `ACCEPTED` 成功结果；生产
   `HttpsTurnClient.java:125-142/:258-284` 在构造 accepted result 前完成 HTTP 200、content type、严格 JSON、
   `IDLE/ACTION` shape 与 action validator 校验。因此当前清空顺序与“合法成功响应先 ACK”合同一致。
2. **transport uncertainty**：`WindowTurnLoop.java:189-196` 在 transport 调用抛错时尚未清空 retained outcome/png；
   `:135-173` 捕获一次 `TurnTransportException` 后结束显式 loop，没有内部 retry/reconnect。显式再次
   `start()` 时 `:59-79` 不清除 previous/cache，因此保留同一 outcome 供下一次人工/上层显式启动后的 exchange。
3. **actionId 去重与新 action 门**：`WindowTurnLoop.java:217-234` 对相同 `actionId` 只复用
   `lastExecutedTurn`；物理 executor 仅在不同 id 分支的 `:228` 存在一个调用点。新 action 只可能出现在
   当前 exchange 已成功 ACK previous 后。执行前先冻结 id、执行或相关性校验异常时保持
   `lastExecutedTurn=null`，后续由 `:218-220` fail closed，避免不确定动作二次执行。
4. **metadata exactness**：`WindowTurnLoop.java:176-186` 每次 request 只调用一次 supplier，先核对 immutable
   loop `deviceId/windowId`，再由 frozen validator 校验完整 window metadata；三文件没有自行构造 metadata、
   `(0,0)` fallback 或旧快照复用。真实 supplier 的来源属于 TURN-13 wiring，见下方父级复验点。
5. **显式 lifecycle**：`WindowTurnLoop.java:59-113/:135-173` 只有显式 `start/stop/awaitStopped`，使用
   monitor、atomic running/stop flag 与 volatile worker thread；重复 start fail closed，stop 中断当前 long wait/action，
   finally 在同一 monitor 内清理 thread/running 并唤醒 await。未发现自动 lifecycle hook。
6. **一窗口一 loop**：`TurnLoopRegistry.java:31-46` 同步检查 exact `windowId` 后创建并登记 inert loop；
   `:61-71` 禁止移除运行中 loop。`TurnLoopFactory.java:28-39` 为 package-private inert construction，factory 与
   registry 均不调用 `start()`。
7. **禁令扫描**：三文件未命中 `@PostConstruct`、`@Scheduled`、scheduler/fixed-rate/fixed-delay、WebSocket、
   raw socket、`Thread.sleep`、retry/reconnect、ledger/session/owner/TTL、`new TurnWindowMetadata` 或
   `new TurnAction`。`git diff --check` 无 whitespace error；未运行 Maven/tests/runtime/application/server。

## Parent revalidation points

- TURN-12 父级复审时应确认生产调用仍只能使用 `HttpsTurnClient` 或同等满足 `TurnClient`“validated response +
  accepted ACK”合同的实现；若未来引入弱实现，`WindowTurnLoop.java:194-198` 的清空前提将不再成立。
- TURN-13 wiring 必须独立证明传入 `TurnLoopRegistry.create(...)` 的 supplier 每轮来自当前 exact bound window，
  `deviceId/windowId/nativeHandle/processId/windowRect/stopRequested` 均为真实即时值；TURN-12 三文件只能验证，
  不能单独证明 supplier 来源。
- 父级仍需自行审查源码并给出 P0/P1/P2 与最终裁决；本报告不构成批准或关闭。
