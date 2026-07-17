# Cloud ReturnItemPrescan Worker AP

- status: `CLAIMED`
- owner: `Internal AP`
- claimedAt: `2026-07-13 21:36:44 -04:00`
- scope: 简化路线下，仅迁移 Cloud `ReturnItemPrescanService`；真实 `BagService` 永久留在 DHXY 本地。

## 唯一写集

1. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ReturnItemPrescanService.java`
2. New/append-only `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-return-item-prescan-worker-ap.md`

除上述三项外不修改任何文件。尤其不修改或复制 remote plumbing/types、`TaskExecutionContext`、其它 Service/Task、DHXY Java、schema、tests、host；不回滚、覆盖、清理、删除或提交并行 Worker 的 dirty/untracked。

## 基线证据

- DHXY 当前分支/HEAD：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`；HEAD 即用户指定 committed 业务基线。该分支未配置 upstream，最新 pushed commit 不可从分支元数据确认。
- Cloud 当前分支/HEAD：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`；该分支未配置 upstream。
- 两仓 `git status --short --branch` 均显示大量并行 dirty/untracked；本 Worker 只认唯一写集，不处理其它改动。
- 开工前 Cloud `ReturnItemPrescanService.java` 不存在；`git status --short -- <target>` 与 `git diff -- <target>` 均为空。Cloud `BagService.java` 同样不存在，且用户现已明确禁止创建/保留该副本。
- `git show 0114604e:src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
  - blob：`9b468c958c387128f549fff810528c25b9ab47c5`
  - 迁移权威：完整类原样逻辑，包括策略候选、`ThreadLocalRandom`、4s + 8..18s combat due、`inProgress/done/combatFallback`、cache invalidation、`completeRound`、日志和返回语义。
- `git show 0114604e:src/main/java/com/bot/dhxy/service/BagService.java`
  - blob：`180f124de91d088cdb2ef6416bd78d62659d2ba8`
  - 本波只以 `prescanMainBagTaskPageItem(...)`、`prescanMainBagItemFromBack(...)`、`useCachedMainBagReturnItem(...)` 的调用/返回/stop 语义作为三项 `BAG_RETURN_ITEM` 映射基线；不迁移 Cloud `BagService`。

无已批准业务差异；按 `0114604e` 基线等价迁移。

## 2026-07-13 21:40:38 -04:00 / WAITING_DEPENDENCY

- `ReturnItemPrescanService.java` 已按 committed baseline 落入本 Worker 唯一写集；当前未编译，因为它还不能引用未落盘的 shared typed macro 合同/API。
- External A 已于 `2026-07-13T21:38:45-04:00` 领取 Cloud closed types，但 `LocalMacro*` / `BagReturnItemMacro*` 当前尚未落盘。
- Internal AO 已于 `2026-07-13T21:39:51-04:00` 领取普通 retained `LOCAL_MACRO` plumbing，但 `CloudGameClient` / `CloudTaskServicePort` 的 macro API 当前尚未落盘。
- AP 不复制、不替代上述 remote 类型/API，不触碰其写集。待二者真实落盘后，直接在 `ReturnItemPrescanService` 内映射三项 macro，并在依赖齐全时最多运行 Cloud `mvn -q compile`。

## 2026-07-13 21:42:04 -04:00 / 用户方向变更

- 用户明确拍板：真实 `BagService` 永久留 DHXY 本地，Cloud 不创建或保留 `com.bot.dhxy.service.BagService` 副本。
- AP 唯一写集立即缩为 Cloud `ReturnItemPrescanService.java` + 本报告；开工至此从未创建 Cloud `BagService.java`，因此没有本 Worker 文件需要删除。
- 三项本地能力由 `ReturnItemPrescanService` 直接经 `context.getGameClient().executeLocalMacro(...)`（或 AO 最终落盘的同一 typed shared facade）同步调用。最多使用该类内一个私有映射方法，不新增第二个 Service/port/wrapper 文件。
- `ReturnItemPrescanService` 的 public API、随机策略、4s + 8..18s 时序、cache、invalidation、fallback、`completeRound` 与日志继续保持 `0114604e`。
- 本波只迁 `ReturnItemPrescanService`；Bag 全类仍未迁，且按用户决定不迁 Cloud。

## Internal AP Implementation #1 - 2026-07-13 21:50:45 -04:00

- status: `IMPLEMENTED / CLOUD_COMPILE_PASS / SELF_QA_ONLY`
- Cloud 唯一 Java 产物：`src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
- 最终 SHA-256：`781A21AA421C45BCD4F0C2DE87C3156A5E2BF536CA124112B4A9F123F560C090`
- Cloud `BagService.java` 存在性核验：`False`。本 Worker 从未创建该文件。

### 逐方法 baseline parity

| 方法 | `0114604e` 等价项 |
|---|---|
| `afterTrackerGreen(...)` | `stateFor(true, round > 1, null)`、`AFTER_TRACKER_GREEN` gate、`done/inProgress` gate、`:after-tracker-green` source 与 `fallbackToCombat=true` 原样保留。 |
| `afterTrackerGreenRequired(...)` | 强制 `AFTER_TRACKER_GREEN`、不降级 combat、source suffix 与 `fallbackToCombat=false` 原样保留。 |
| `whilePathing(...)` | `BACKGROUND_PATHING` gate、`done/inProgress` gate、`:background-pathing` 与失败后 combat fallback 原样保留。 |
| `whileInCombat(...)` | background opportunity missed 判定、4,000ms maintenance + `ThreadLocalRandom` 8,000..18,000ms inclusive due、首次只排期、到期才执行、fallback 判定原样保留。 |
| `useCached(...)` | 无 state/cache 立即 false；本地宏 `USED=true`、`NOT_USED` 或机械非执行=false；原日志、失败时 `invalidate("cached-click-failed:" + source)` 与返回语义不变。 |
| `hasCached(...)` | scoped key 查询与 `cachePoint != null` 判定逐字保持。 |
| `invalidate(...)` | `cachePoint=null`、`done=false`、`combatFallback=true`、日志逐字保持。 |
| `completeRound(...)` | 删除 scoped round state、`hadCache` 日志与空 state no-op 逐字保持。 |
| `runPrescan(...)` | 前置 `TaskCheckpoint`、`inProgress` try/finally、成功时 cache/done/fallback、失败时 done/fallback、两条日志与返回含义保持；唯一替换是三项本地调用改为 typed macro。 |

静态证据：脚本比较 committed blob 与 Cloud 目标，`strategy-public-flow=True`、`cache-state-through-strategy-choice=True`、`normalize-and-state=True`、`log-literals=True`；两边 `[return-item-prescan]` 日志字面量均为 8 条；public 方法签名均为 8 个且逐项相同。

### 唯一 remote boundary

- 类内仅一个私有 `executeBagReturnItemMacro(...)` 负责 domain/wire 映射与同步发送；没有第二个 Service、port、wrapper 文件或嵌套调用层。
- 调用固定 `context.getGameClient().executeLocalMacro(...)`：phase=`return-item-prescan`；三种固定 action-slot 前缀分别为 `prescan-main-bag-task-page`、`prescan-main-bag-from-back`、`use-cached-main-bag-return-item`，并拼接 command 已 canonicalize 的既有 deterministic `source`，区分同 operation 的 tracker/pathing/combat/return-home 业务机会。无 UUID、无 Service 私有机械状态。
- command 仅映射 A 的 closed `LOCAL_MACRO/BAG_RETURN_ITEM`：三种 operation、`templatePath/maxBackPage/source/cachedPoint`；cache point 五字段逐值双向映射，坐标保持 screen-absolute px。
- transport timeout=`120_000ms`，对应 committed `InputActionQueue.await(...)` 的 120 秒同步等待；没有新增超时业务决策。
- 仅 `ExecutionState.EXECUTED` 消费 typed result。prescan `FOUND(point)` 映射 `ReturnItemCachePoint`，`NOT_FOUND` 映射 null；cached use `USED` 映射 true，`NOT_USED` 映射 false。
- 机械 `NOT_EXECUTED/STOPPED/UNKNOWN` 先走既有 `TaskCheckpoint`，未抛 stop 时映射为基线同步失败的 null/false；没有自动重投、retry、TTL、fallback 或新 ledger。`ReturnItemPrescanService` 自身已有 fallback 逻辑未增删。
- Cloud key 的唯一必要适配：baseline 的本地 `WindowRuntimeContext/WindowNativeBinding/long taskRunId` 改读 Cloud `TaskExecutionContext.getWindowId()/getNativeWindowHandle()/getTaskRunId()`；key 仍包含 taskCode/window/hwnd/taskRun/round/template。

### 编译与静态核验

- Cloud `mvn -q compile`（不 clean、不 skip tests）第一次：exit 0，耗时约 4.4s。
- 注释清理后 fresh Cloud `mvn -q compile`：exit 0，耗时约 25.3s；最终 class `target/classes/com/bot/dhxy/service/ReturnItemPrescanService.class` 已于 `2026-07-13 21:51:20 -04:00` 重新生成。
- 未新增/运行 tests；未启动 host、线程、任务、UI、capture 或 input；未执行 Git mutation。
- 无 trailing whitespace；目标写集状态仅 Cloud `?? ReturnItemPrescanService.java` 与 DHXY `??` 本报告。

### 依赖状态与本波边界

- AP 的 Cloud 编译依赖已解除：External A closed types 与 AO `CloudGameClient.executeLocalMacro(...)` shared facade 均已真实落盘，并随 AP fresh compile 通过。
- 截至本次交付，AO 报告仍为 `CLAIMED`、尚未写正式 Implementation；External B wire 已交付首版但等待 C handler 补 enum 穷尽；External C 已交付本地 Bag direct-for-exclusive 入口与 model，handler/ledger 尚待 B wire 后续接线；External D schema 最新父级结论为 `BLOCKED P1=2`，等待文档返修。上述均不在 AP 写集，AP 不代改。
- 因此本报告只声明：**Cloud `ReturnItemPrescanService` 源码已实现且 Cloud compile 通过**。不声明 BAG_RETURN_ITEM 双端整波可运行，不声明 External A/B/C/D/AO 已获父级通过，不声明 `BagService` 整体迁移或完成。
- 真实 `BagService` 与其 capture/template/input 交错宏永久留 DHXY；本波只解锁 Cloud `ReturnItemPrescanService`。

### self-QA（不构成 reviewer approval）

- `P0=0 / P1=0 / P2=0`，仅为 Internal AP 自审。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #1 - BLOCKED - 2026-07-13T21:57:00-04:00

父级逐方法与 committed `0114604e` 对比，随机策略、4 秒 maintenance、8..18 秒 combat due、cache、
invalidation、fallback、`completeRound` 与三项 Bag 调用替换均保持；真实 `BagService` 未迁 Cloud。发现
`P0=0 / P1=1 / P2=0`：

- **P1：transport `UNKNOWN` 被折成业务 miss。** `executeBagReturnItemMacro(...)` 当前对所有非
  `EXECUTED` 统一 `return null`。因此 prescan UNKNOWN 会被 `runPrescan` 记录为失败并启用 combat fallback；cached-use
  UNKNOWN 会被 `useCached` 当 `false` 并清掉 cache。UNKNOWN 明确表示本地物理动作是否发生不确定，且 retained
  occurrence 仍保持未 final-consumed；业务继续或换新 source/slot 可能再次触发 Bag 宏，造成重复点击/重复使用。

**精确返修条件：** 只改本 Service。`EXECUTED` 继续消费 typed result；`NOT_EXECUTED` 在现有
`TaskCheckpoint.throwIfStopRequested(...)` 后保留当前 baseline null/false 路径；`STOPPED` 先 checkpoint，若 checkpoint
未抛则以 fatal/illegal-state 中止；`UNKNOWN` 在 checkpoint 后必须抛出 `TaskFatalException`（或仓内等价的明确终止异常），
不得返回 null/false、不得 invalidation、不得进入 fallback、不得 final-consume、不得自动 retry/换 ID。异常信息带
operation/action slot/outcome code 便于诊断。其它源码与行为冻结。

返修后运行 Cloud `mvn -q compile`（不 clean、不 skip），在本报告追加 `Internal AP Implementation Repair #1`。
父级复审前本切片为 BLOCKED。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal AP Implementation Repair #1 - 2026-07-13 21:56:48 -04:00

- status: `REPAIRED / CLOUD_COMPILE_PASS / AWAITING_PARENT_REREVIEW`
- 返修范围严格为 Cloud `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java` + 本固定报告；未修改 shared remote plumbing/types、其它 Service/Task、DHXY Java、schema、tests 或 host。
- 最终源码 SHA-256：`B2D024E541C3FB58CC819E883627F0FF085BDC67E8992EB0F518CA6384689110`。

### P1 精确修复

1. 新增既有 `com.bot.dhxy.runner.exception.TaskFatalException` import；未新增异常类型或 wrapper。
2. `executeBagReturnItemMacro(...)` 先保存完整 canonical action slot，仍只调用一次 `context.getGameClient().executeLocalMacro(...)`。
3. outcome 分流改为：
   - `EXECUTED`：继续读取并返回 typed `BagReturnItemMacroResult`，原 operation 一致性检查不变。
   - `NOT_EXECUTED`：先执行原 `TaskCheckpoint.throwIfStopRequested(context, "Bag operation was interrupted")`；checkpoint 未抛时才返回 null，继续保持 committed 同步未执行的 null/false 语义。
   - `STOPPED`：先执行同一 checkpoint；checkpoint 若抛 stop 则原样中止，若未抛则抛 `TaskFatalException`，绝不返回 null/false。
   - `UNKNOWN`：先执行同一 checkpoint；checkpoint 未抛时抛 `TaskFatalException`。异常发生在 helper 返回前，因此 prescan 不会进入 `runPrescan` 的 failed/fallback 分支，cached-use 不会进入 false/log/invalidate 分支。
4. fatal 诊断字符串精确携带 `operation`、完整 `actionSlot`、`executionState` 与 `outcomeCode`。
5. `UNKNOWN` 仍由 AO `CloudGameClient` 保持不 final-consume；本 Service 收到 UNKNOWN 后只中止当前任务，不 retry、不换 ID、不重建 command、不自动调用第二次 macro，retained identity 保持未决。
6. `runPrescan` 的 `finally` 仍只恢复 `inProgress=false`；UNKNOWN/STOPPED 异常路径不改 `done/combatFallback/cachePoint`。public API、随机策略、4 秒 + 8..18 秒时序、cache/invalidation/completeRound 与原 8 条业务日志均未修改。

### 门禁

- 命令：Cloud `mvn -q compile`（未运行 clean、未设置 skip）。
- 结果：exit 0，耗时约 21 秒；返修后的 `ReturnItemPrescanService.class` 已重新生成。
- 静态核验：helper Javadoc 现在声明“仅 NOT_EXECUTED 返回 null”；源码分支只有 `ExecutionState.NOT_EXECUTED` 可执行 `return null`；fatal 消息包含父级要求的四项诊断；无 trailing whitespace。
- 未新增或运行 tests；未启动 host、线程、任务、UI、capture 或 input；未执行 Git mutation。

### self-QA（不构成 reviewer approval）

- `P0=0 / P1=0 / P2=0`，仅为 Internal AP 返修自审；父级复审前不自行改写 BLOCKED 结论。
- 真实 `BagService` 继续永久留 DHXY；本返修仍只解锁 Cloud `ReturnItemPrescanService`，不声明 BAG_RETURN_ITEM 双端整波完成。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #2 - APPROVED - 2026-07-13T22:00:00-04:00

父级独立复核返修后的 `ReturnItemPrescanService.executeBagReturnItemMacro(...)`：每次仍只调用一次
`context.getGameClient().executeLocalMacro(...)`，稳定 `actionSlot` 未重建；`EXECUTED` 继续消费 closed typed result，
`NOT_EXECUTED` 只在既有 `TaskCheckpoint` 后返回基线 null/false。`STOPPED` 与 `UNKNOWN` 均先 checkpoint，若未抛停机
异常则抛现有 `TaskFatalException`，因此不会返回 `runPrescan(...)`/`useCached(...)`，不会清 cache、进入 fallback、
final-consume、自动 retry 或铸造新 identity。诊断包含 operation、完整 actionSlot、executionState 和 outcomeCode。

返修没有改变随机策略、4 秒 maintenance、8..18 秒 combat due、三种 Bag 操作、cache/invalidation、
`completeRound`、日志或 public API；真实 `BagService` 继续永久留 DHXY。本切片 `SOURCE APPROVED`，
`P0=0 / P1=0 / P2=0`。AP 的 Cloud `mvn -q compile` 已 exit 0；整波最终批准仍等待 AO/B/C 完成 wire、digest、
handler 后由父级运行 fresh 双构建。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
