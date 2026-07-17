# TURN-12 — DHXY long-wait loop and in-memory duplicate suppression

## CLAIMED

- 领取时间：`2026-07-15T16:22:25-04:00`；状态：`CLAIMED`；`countDelta=0`。
- 角色：CR271 Internal implementation Worker；父级是唯一 manager/final reviewer。
- 前置：`TURN-06`、`TURN-11` 已父级 SOURCE APPROVED，TURN-11 owner 已释放后续派本卡。
- 唯一 Java 写集：`cloud/turn/WindowTurnLoop.java`、`TurnLoopRegistry.java`、`TurnLoopFactory.java`；
  唯一报告为本文件。
- 只读复用：`TurnClient`、`TurnExchangeResult`、`LocalTurnActionExecutor`、冻结 protocol DTO/validator 及
  `TurnExecutionWindow`。禁止修改上游 executor/client/protocol/window/config/UI/Task/Service/计划/CR。
- 目标：显式 lifecycle、一个 windowId 一个 loop、一次 exchange、成功响应 ACK 后清空 previous、transport failure
  保留 previous、同 actionId 只返回内存缓存且绝不二次物理执行；无自动启动、scheduler、短 polling、retry/reconnect。
- 当前 writers 活动，不运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，不执行 Git mutation；
  保护两仓全部 dirty/untracked。

`CLAIMED`

## SOURCE DELIVERED

- 交付时间：`2026-07-15T16:28:57-04:00`；状态：
  `SOURCE DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。
- 唯一 Java 交付及 SHA-256：
  - `WindowTurnLoop.java`：`A515378DBE3BB8051EB0CEC58AC3A341C467B6DD3A50E8E9EDDB6EE5EFA0AB1A`
  - `TurnLoopRegistry.java`：`8D3AFE1BEBC7F51BED3C7102C07E553651CEF505BD94CB7257537575ACADD067`
  - `TurnLoopFactory.java`：`48094621EF0E57CA8268964EE2D2DB8B8F3A637DC18BE858603DC8AFB4971076`

### Explicit lifecycle and one-loop boundary

- `WindowTurnLoop.java:42-57/:59-113` 冻结非空 `deviceId/windowId` 与正数 long-wait milliseconds，公开且仅公开
  显式 `start/stop/awaitStopped(Duration)` 生命周期；start 不清空 previous/cached action，允许显式重启继续发送未 ACK
  outcome。无 Spring lifecycle、自动启动或 scheduler。
- `TurnLoopRegistry.java:31-46` 在同步 create 边界对精确 `windowId` 冲突 fail closed，并只注册 stopped loop；
  `:61-72` 对不存在或仍 running 的 remove fail closed。Registry/factory 均不调用 `start()`。
- `TurnLoopFactory.create` 为 package-private inert construction boundary，外部显式 caller 通过 Registry create/remove，
  不能绕开 Registry 直接制造第二个同 windowId loop。

### Exchange, acknowledgement, and duplicate suppression

- `WindowTurnLoop.java:176-192` 每轮调用 metadata supplier 恰好一次，先严格核对其 immutable
  `deviceId/windowId`，再由冻结 validator 校验真实完整 metadata；没有构造 `TurnWindowMetadata`、伪 action、旧 metadata
  或 `(0,0)` fallback。PNG 从 retained byte array clone 后传入，`turnClient.exchange` 只有一个调用点。
- `:194-210` 任何成功 `TurnExchangeResult` 均在读取 `ACTION/IDLE` 前先把本次 previous outcome/PNG 同时清空；
  transport exception 在 `:141-150` 直接终止本次显式 loop，因此 fields 保持原值，没有自动 retry/reconnect。
- `:212-234` ACTION 先校验冻结 action 合同与 immutable identity。相同 actionId 复用唯一
  `lastExecutedTurn` 并重新携带其 outcome/防御性 PNG；不同 action 只会在一次成功 ACK 响应后进入唯一
  `actionExecutor.execute` 调用点。调用物理 executor 前先记录 actionId，若返回结果前异常，则 cache 保持 null，后续
  任意 action 都 fail closed，绝不二次物理执行或伪造 outcome。
- `ExecutedTurn.optionalPng()` 已提供 clone；loop 保存该 clone，并在每次 transport 调用前再次 clone，未向 caller/client
  暴露 retained mutable byte array。

### Static scope checks

- 调用点计数：`turnClient.exchange=1`、`actionExecutor.execute=1`、`windowMetadataSupplier.get=1`。
- 禁令扫描均为 `0`：`@PostConstruct`、`@Scheduled`、`ScheduledExecutor`、fixed-rate/fixed-delay、WebSocket、
  raw Socket、`Thread.sleep`、retry/reconnect、ledger/session/owner/TTL、`new TurnWindowMetadata`、`new TurnAction`。
- 三文件 `git diff --no-index --check -- /dev/null <file>` 无 whitespace error，仅有 Git LF/CRLF 工作区提示。
- 未修改 `TurnClient`、`LocalTurnActionExecutor`、protocol、`TurnExecutionWindow`、配置/UI/Task/Service、计划、CR 或
  其它 dirty/untracked；未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。
- Worker 到此停止，不自批、不领取下一卡，等待父级独立源码复审。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE REVIEW #1

- 审查时间：`2026-07-15T16:32:00-04:00`；父级独立逐行读取三份交付源码，并交叉核对
  `TurnClient`、`HttpsTurnClient`、`TurnExchangeResult` 与 Cloud `CloudTurnExchange`。
- Helper-R1 的 `PRECHECK_CLEAR` 仅为非绑定证据；父级确认 ACK/previous 清理、transport uncertainty、
  actionId 内存去重与生产 HTTPS response validation 本身成立，但 lifecycle/registry 仍有两个 P1。
- 结论：`P0/P1/P2=0/2/0 / REPAIR #1`，不得写 `SOURCE APPROVED`，不得续派 `TURN-13`。

### P1-1：并发 stop 可被 start 覆盖，显式停止请求丢失

- 证据：`WindowTurnLoop.java:60-78` 的 `start()` 持有 `lifecycleMonitor`，但 `:82-88` 的 `stop()`
  不持有同一 monitor。合法交错为：start 在 `:62` 把 `running` 置 true 后暂停；stop 在 `:83` 写 true，
  此时 `workerThread` 仍为 null；start 随后在 `:65` 又把 stop flag 清为 false并启动线程。
- 影响：调用者已经显式 stop，但 long-wait loop 仍会启动并继续接收/执行 action，违反显式 lifecycle 与停止安全门。
- Repair #1 条件：仅在本卡三文件写集内，让 start/stop 的 flag reset、thread publish 与 interrupt 使用同一
  lifecycle 原子边界；必须保证与一个已经进入 start 的并发 stop 不会被覆盖。不得新增 scheduler、retry、
  owner/permit/session/ledger/TTL 或自动 lifecycle。

### P1-2：registry remove 后泄漏的 loop 可重新启动，无法保证一窗口一个 loop

- 证据：`TurnLoopRegistry.java:61-71` 返回已移除的可启动 `WindowTurnLoop`；该实例的
  `WindowTurnLoop.start():60-78` 没有 retired/disposed 门。调用者可 remove 旧 loop、create 新 loop，再分别
  start 两个实例；并发 find/remove/start 也能形成同一结果。
- 影响：同一个 exact `windowId` 可出现两个运行 loop，重复 long-wait、重复 action delivery/物理执行与窗口隔离失效。
- Repair #1 条件：registry remove 必须与 loop start 在同一 lifecycle 原子边界完成永久退役；退役后的旧引用
  必须永远拒绝 start，start 先发生时 remove 必须拒绝。允许加入最小 in-memory retired lifecycle flag，
  但不得新增 owner/permit/session/ledger/durable state，也不得扩大三文件写集。

- 复验点：父级重新展开两个竞态的全部入口；确认原 ACK、previous、相同 actionId cache、无自动 retry/启动合同
  未漂移。Java writer 重新活动，本轮不运行 Maven/tests/runtime/application/server 或 Git mutation。

`REPAIR #1 / P0/P1/P2=0/2/0`

## REPAIR #1 — SOURCE DELIVERED

- 返修交付时间：`2026-07-15T16:36:51-04:00`；状态：
  `SOURCE DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。
- 仍仅修改 TURN-12 三文件写集；本轮 `TurnLoopFactory.java` 内容未变。Worker 到此停止，不自批、不领取下一卡。

### P1-1 start/stop lifecycle linearization

- `WindowTurnLoop.java:61-94` 的 `start()` 与 `stop()` 现均在同一 `lifecycleMonitor` 下完成
  `stopRequested` reset/set、`workerThread` publish/read 与 interrupt。
- start 先获得边界时，running=true、stop flag reset、thread publish/start 均在释放 monitor 前完成；并发 stop 随后必然
  看到已发布 thread、写 stop=true 并 interrupt，不再存在 stop 在 thread 为 null 时返回、随后又被 start 清除的交错。
- stop 在线性化边界先完成时，后续 start 是一个明确晚于 stop 的新显式 start；没有新增自动 lifecycle、scheduler 或等待轮询。

### P1-2 permanent retirement linearization

- `WindowTurnLoop.java:35/:63-65/:141-149` 新增唯一最小内存 `retired` boolean，只在
  `lifecycleMonitor` 内读写。`start()` 先检查 retired；`retireIfStopped()` 在同一边界内检查 running 并永久置 retired。
- `TurnLoopRegistry.java:61-69` 删除原 `isRunning()` check-then-remove 竞态，改为持有 registry monitor 时调用
  `loop.retireIfStopped()`，成功后才从 map 移除。
- remove 获 loop lifecycle 边界先：retired=true，泄漏的旧引用永远拒绝 start；start 获边界先：running=true 且 thread
  已发布，remove 的 retire 原子检查抛错并保留 registry entry。Factory/Registry 仍从不调用 start。

### Repair #1 evidence

- 三文件 SHA-256：
  - `WindowTurnLoop.java`：`4232D3A766A7DF74E6839DC51C551C1919E29A6B7566A51AE8BAEBD494E7A6A5`
  - `TurnLoopRegistry.java`：`56AE549AA5C874EF09D21A47F881FEDD8DEA0F89E407B14CB035275C1E807103`
  - `TurnLoopFactory.java`：`48094621EF0E57CA8268964EE2D2DB8B8F3A637DC18BE858603DC8AFB4971076`
- 原交换合同未漂移：`turnClient.exchange=1`、`actionExecutor.execute=1`、metadata supplier=1、成功响应后的
  `previousOutcome=null`/`previousPng=null` 各一个；transport failure、actionId cache 与防御性 PNG 代码未改。
- 禁令扫描均为 `0`：自动启动/scheduler/短 sleep、retry/reconnect、owner/permit/session/ledger/TTL。
- 三 Java 文件 `git diff --no-index --check -- /dev/null <file>` 无 whitespace error，仅有 Git LF/CRLF 工作区提示。
- 未修改写集外文件，未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation；
  等待父级独立复审。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE REVIEW #2 — REPAIR #1 APPROVED

- 复审时间：`2026-07-15T16:38:00-04:00`；父级重新逐行展开三份 Java，交付 SHA 与 Repair #1 报告一致。
- 结论：`P0/P1/P2=0/0/0 / SOURCE APPROVED / BUILD COHORT PENDING`。
- P1-1 已关闭：`WindowTurnLoop.java:61-94` 让 start/stop 在同一 `lifecycleMonitor` 内线性化；start 先赢时
  thread 已 publish/start 后 stop 才进入并必然置 flag+interrupt，stop 先于 start 完成时后续 start 是明确的新显式调用。
- P1-2 已关闭：`WindowTurnLoop.java:141-149` 的最小 in-memory retired flag 与 start 共用同一 monitor；
  `TurnLoopRegistry.java:61-69` 只有 retire 成功才 remove。remove 先赢则旧引用永远拒绝 start，start 先赢则
  retire 抛错且 registry entry 保留。
- 原合同保持：一次 exchange、合法 200/IDLE 后清 previous、transport failure 保留、同 actionId 复用缓存、
  executor 调用点唯一；无自动启动、scheduler、短 polling、retry/reconnect、owner/permit/session/ledger/TTL。
- 源码 owner 已释放；Foundation 至 TURN-12 共 23 张已关闭或源码批准。父级已冻结 TURN-13 原子模式互斥
  brief 并立即续派；Java writer 继续活动，本轮不运行 Maven。

`SOURCE APPROVED / BUILD COHORT PENDING / P0/P1/P2=0/0/0`
