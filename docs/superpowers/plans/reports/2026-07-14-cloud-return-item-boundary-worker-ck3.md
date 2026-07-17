# Cloud Phase 3 ReturnItemPrescan Local Boundary - Internal CK3

## Parent Task Brief #1 - 2026-07-14T12:22:00-04:00

Task: `W-696-RETURN-ITEM-BOUNDARY-1`

恢复已在旧 active Cloud 中通过父级源码与 fresh 双仓构建的 `BAG_RETURN_ITEM` typed local-macro 适配，但本轮
必须重新以 `696a12b0` 完整类为对照。唯一 Java 写集：

- Cloud `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`

源现场：active 与 baseline blob 必须仍同为 `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057`；候选适配体为
`migration-preserved/pre-696a12b0-whole-service-cutover-20260714T1129/src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`，
blob `f76196b7562060c95346e4b143ebb5e57f948f75`。先在本报告追加 `CLAIMED`。前置不符立即 `BLOCKED`，不得覆盖。

允许把候选适配体原字节恢复到 active，但必须在报告逐项证明相对 `696a12b0` 的差异只包括：删除
`BagService` 注入；增加现有 Cloud `BAG_RETURN_ITEM` typed macro imports/constants/helper；把原三处 Bag 调用
分别映射到同顺序的 `PRESCAN_MAIN_BAG_TASK_PAGE`、`PRESCAN_MAIN_BAG_FROM_BACK`、
`USE_CACHED_MAIN_BAG_RETURN_ITEM`；把 typed cache point 等价映射回原 `ReturnItemCachePoint`。不得改变
strategy 候选、随机范围、4s/8..18s 时钟、inProgress/done/fallback/cache/invalidate/completeRound、日志位置、
public/private API 或状态更新。

禁止改任何其它 Java、remote/schema、pom、其它报告；禁止 build/test/runtime/Git mutation。交付记录
active pre/post blob、SHA-256、baseline-vs-candidate 差异 disposition。Worker 自审只算 QA；父级审查后才可
APPROVED，统一 build 由父级在并发 writer 稳定后执行。

## Worker Claim - 2026-07-14

`CLAIMED` - Internal CK3 已领取 `W-696-RETURN-ITEM-BOUNDARY-1`；先执行三方 blob 门禁与完整等价性对照。

## Worker Result - 2026-07-14

`BLOCKED` - 未把 preservation 候选复制到 active，Java 零覆盖。Worker 自审只记 QA，不构成
`APPROVED`。

### Blob / SHA-256 门禁

- `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 中该文件的 Git blob：
  `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057`。
- active pre：blob `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057`，工作树原字节
  SHA-256 `f91110edbf2f75310c7ff62229e78d0ddf043382eb82b77fa3bc008f3355bcbf`，
  `14130` bytes。
- migration-baseline：blob `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057`，工作树原字节
  SHA-256 `f91110edbf2f75310c7ff62229e78d0ddf043382eb82b77fa3bc008f3355bcbf`，
  `14130` bytes。
- preservation 候选：blob `f76196b7562060c95346e4b143ebb5e57f948f75`，工作树原字节
  SHA-256 `b2d024e541c3fb58cc819e883627f0ff085bdc67e8992eb0f518ca6384689110`，
  `19848` bytes。
- active post：仍为 blob `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057`、SHA-256
  `f91110edbf2f75310c7ff62229e78d0ddf043382eb82b77fa3bc008f3355bcbf`；pre/post
  完全相同。

说明：PowerShell 直接对 active 的 CRLF 原始字节构造未过滤 Git object 会得到 `a4c33ada...`；
仓库属性过滤后的 `git hash-object`、migration-baseline 与 `696a12b0:<path>` 均为合同要求的
`c8f6d0b3...`。上述 SHA-256 均明确按当前文件原字节记录。

### baseline-vs-candidate 完整差异 disposition

对 `git diff --no-index --ignore-space-at-eol` 的全部 hunks 逐项结论如下：

1. **既有 typed Bag macro 边界，Worker QA 可等价保留：**删除 `BagService` 注入；增加
   `TaskFatalException`、`BagReturnItemMacroCommand/Result`、`ExecutionState`、`LocalMacroKind/Outcome`
   imports；增加 120 秒 timeout、phase/action-slot 常量和一个 private macro helper。120 秒与本地
   `InputActionQueue.await` 的 committed 120 秒同步等待一致；未发现自动 retry。
2. **三操作映射与原调用顺序，Worker QA 通过：**`useCached` 原调用点映射
   `USE_CACHED_MAIN_BAG_RETURN_ITEM`；`runPrescan` 原 switch 的 task-page、from-back 两臂分别映射
   `PRESCAN_MAIN_BAG_TASK_PAGE`、`PRESCAN_MAIN_BAG_FROM_BACK`。三处仍各自在原调用点同步完成，
   `template/maxBackPage/source/cachedPoint` 按原参数传入，没有重排调用或新增业务 fallback。
3. **cache point 投影，Worker QA 通过：**候选在 wire 两侧逐字段保留 `templatePath/clickX/clickY/
   learnedAtMs/source`；`USED` 仍映射为 true，`NOT_USED`/未执行仍进入原 false/invalidate 分支；
   prescan 无 point 仍进入原 `done=false` 与既有 `fallbackToCombat` 分支。
4. **业务算法未改，Worker QA 通过：**`Strategy` 候选及选择顺序、`ThreadLocalRandom` 选择、
   `COMBAT_ENTRY_MAINTENANCE_MS=4000`、`8..18s` 抖动、`inProgress/done/combatFallback/cachePoint`
   写入、`invalidate`、`completeRound`、原 ReturnItemPrescan 日志位置与既有 baseline 方法体，在上述
   三个 Bag 调用点以外均无差异。
5. **新增 public API，阻断：**候选新增
   `public boolean hasCached(TaskExecutionContext, String, int, String)`；`696a12b0` 无此方法，当前
   Cloud `src/**` 对该方法也无 caller。它不是三处 Bag terminal replacement，违反 brief 的 public/private
   API 等价门禁。若后续构造可接受适配体，必须删除该方法。
6. **window/context 访问改写，需父级裁决：**候选删除 `WindowRuntimeContext/WindowNativeBinding`
   读取，改用 Cloud `TaskExecutionContext#getWindowId()/hasNativeWindow()/getNativeWindowHandle()`。
   这是 Cloud 编译所需的 placement 适配，windowId/hwnd 投影表面对应原 identity，但它超出 brief 列举的
   Bag macro 差异；Worker 不自行批准。
7. **状态键类型/identity 改写，阻断：**baseline `PrescanKey.taskRunId` 为 `long`，候选改为
   `String`。Cloud `TaskExecutionContext#getTaskRunId()` 的签名确为 `String`，且该类 JavaDoc 明确写明
   它是 exact Cloud task-run identity、`intentionally not a local numeric sequence`；因此不存在可证明等价的
   `long` 强转。保留 `long` 无法从当前 Cloud context 无损取值，改成 `String` 又改变 private record 形状与
   state-map key identity 域。按父级预审要求，此项必须 `BLOCKED`，等待父级/用户明确裁决，不能由 Worker
   当作纯编译适配批准。

### 结论与门禁

- preservation 候选不是“只含三处 `BAG_RETURN_ITEM` typed macro 边界”的可原字节恢复体；至少
  `hasCached` 与 `PrescanKey.taskRunId` 两项违反明确门禁。
- 未改 active `ReturnItemPrescanService.java`，未改任何其它 Java、remote/schema、pom 或其它报告。
- 按任务禁令未运行 build、test、runtime，未执行 Git mutation。
- 业务基线已核对 `docs/业务逻辑.md` 的五倍/修罗基线使用规则（L215-L224）、回程不新增等待/验证
  边界（L228-L281）、五倍回城道具预扫保持既有时点（L470-L471），以及修罗 `696a12b0`
  fallback 基线表（L1253-L1294）。无已批准业务差异；本轮因未能证明状态键等价而零 Java 变更。

## Parent Source Review #1 - `W-696-RETURN-ITEM-BOUNDARY-1` - 2026-07-14T12:27:00-04:00

**BLOCKED，P0=0/P1=1/P2=0。** Worker 对 preserved 候选的差异识别正确，零覆盖处理也正确。

- **P1：**候选新增 `hasCached(...)`，而 `696a12b0` 没有该 public API，Cloud 全树也没有 caller；它不是
  四个永久本地 Service 删除后所需的 typed boundary。返修必须删除该方法，不得扩大公开面。
- `keyFor(...)` 改为直接读取 Cloud `TaskExecutionContext` 的 exact `windowId/nativeWindowHandle/taskRunId`
  **批准作为必要 placement adaptation**：这些字段逐一对应 baseline key 的 window/hwnd/run identity；Cloud
  `taskRunId` 的权威类型就是不可损失的 `String`，强压回 `long` 反而会破坏身份。`PrescanKey.taskRunId`
  同步改为 `String` 只改变表示类型，不改变 key 的组成、作用域、查找/失效/完成语义，不构成业务差异。
- 三处 Bag 调用的一对一 macro 映射、typed cache-point 投影、原 strategy/随机范围/4s 与 8..18s 时序、
  fallback/state/log/public baseline API 保持方式可按报告 disposition 保留。

**精确返修条件：**同一 CK3 以 preserved 候选为基础，只删除新增 `hasCached(...)`，其余差异严格限于本
review 已批准的三处 `BAG_RETURN_ITEM` 边界与 exact Cloud context identity 表示；不得改其它 Java、协议或
行为。完成后记录 active pre/post blob、SHA-256 及相对 baseline 的最终 diff disposition，交父级复审。

## Worker Repair #1 Claim - 2026-07-14

`CLAIMED` - Internal CK3 已领取 Parent Source Review #1 的精确返修：以 preservation 候选为基础，
仅排除 `696a12b0` 不存在的 public `hasCached(...)`，保留父级已批准的 typed Bag macro 与 Cloud exact
identity placement adaptation。

## Worker Repair #1 Result - 2026-07-14

`REPAIR_DELIVERED / READY_FOR_PARENT_REVIEW` - Worker QA 完成，不构成 `APPROVED`。

### Active pre/post 与候选证据

- active pre：Git blob `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057`，工作树原字节
  SHA-256 `f91110edbf2f75310c7ff62229e78d0ddf043382eb82b77fa3bc008f3355bcbf`，
  `14130` bytes。
- preservation 候选：Git blob `f76196b7562060c95346e4b143ebb5e57f948f75`，工作树原字节
  SHA-256 `b2d024e541c3fb58cc819e883627f0ff085bdc67e8992eb0f518ca6384689110`，
  `19848` bytes。
- active post：Git blob `61b6190f0ab5e49b82ed8c6281ffc619e66b03e5`，工作树原字节
  SHA-256 `3d78417e2834ad332fce26037e72116224ffc4727914c732287299caf81e21bd`，
  `19077` bytes、`406` 行、统一 LF。
- 逐字符构造门禁：active post 等于 preservation 候选原内容精确删除 `hasCached(...)` 的完整 JavaDoc/
  方法块；candidate-vs-active `git diff --no-index --ignore-space-at-eol` 仅显示该连续 14 行删除，
  index `f76196b..61b6190`，无第二处差异。

### 相对 `696a12b0` 的最终 diff disposition

1. **public/private API：已返修。** `hasCached(...)` 已删除；baseline 的
   `afterTrackerGreen`、`afterTrackerGreenRequired`、`whilePathing`、`whileInCombat`、`useCached`、
   `invalidate`、`completeRound` 公开面全部保留。仅增加已批准 typed boundary 的 private
   `executeBagReturnItemMacro(...)`。
2. **三处 Bag 调用：一对一保留。** `useCached` 在原位置映射
   `USE_CACHED_MAIN_BAG_RETURN_ITEM`；`runPrescan` 原 switch 两臂在原顺序分别映射
   `PRESCAN_MAIN_BAG_TASK_PAGE`、`PRESCAN_MAIN_BAG_FROM_BACK`。最终源码中三种 operation 各出现一次，
   未新增或重排调用。
3. **typed cache point：逐字段等价。** `templatePath/clickX/clickY/learnedAtMs/source` 在
   `ReturnItemCachePoint` 与 wire `CachePoint` 间完整双向投影；`USED/NOT_USED` 与 prescan null/non-null
   继续进入 baseline 原分支。
4. **Bag placement 边界：按批准保留。** 删除 Cloud `BagService` 注入，增加既有
   `BAG_RETURN_ITEM` imports、120 秒同步 timeout、phase/action-slot 常量及 private helper；没有新增 retry、
   strategy 或业务 fallback。
5. **exact Cloud identity：按 Parent Source Review #1 批准保留。** `keyFor(...)` 直接读取 Cloud
   `windowId/nativeWindowHandle/taskRunId`；`PrescanKey.taskRunId` 使用 Cloud 权威 `String`。key 的组成、
   per-window/per-run/per-round/template 作用域以及 lookup/invalidate/completeRound 语义不变。
6. **算法、时序与状态：无差异。** `Strategy` 候选和随机选择、4 秒维护窗、8..18 秒抖动、
   `inProgress/done/combatFallback/combatDueAtMs/cachePoint` 写入、miss fallback、invalidate、
   completeRound、原 ReturnItemPrescan 日志位置与顺序均保持 `696a12b0`。

### 执行边界

- 唯一 Java 修改为 Cloud `ReturnItemPrescanService.java`；另仅追加本固定报告。
- 未修改其它 Java、remote/schema、pom 或其它报告。
- 按 brief 未运行 build、test、runtime，未执行 Git mutation；统一构建仍由父级在并发 writer 稳定后执行。
- 无已批准业务差异；仅保留 Parent Source Review #1 明确批准的 placement adaptation，现交父级复审。

## Parent Source Review #2 - `W-696-RETURN-ITEM-BOUNDARY-1` - 2026-07-14T12:36:01-04:00

**APPROVED，P0/P1/P2=0。** 父级独立复核 active Git blob 为
`61b6190f0ab5e49b82ed8c6281ffc619e66b03e5`，工作树 SHA-256 为
`3d78417e2834ad332fce26037e72116224ffc4727914c732287299caf81e21bd`。preserved candidate 到
active 的完整 `git diff --no-index --ignore-space-at-eol` 只有 `hasCached(...)` JavaDoc/方法连续 14 行删除，
没有第二处差异；active 全文件 `rg hasCached` 零命中，`git diff --check` 零输出。

父级另行对照 `migration-baseline/696a12b0`：七个 baseline public 方法签名完整且无新增 public API；
`USE_CACHED_MAIN_BAG_RETURN_ITEM`、`PRESCAN_MAIN_BAG_TASK_PAGE`、
`PRESCAN_MAIN_BAG_FROM_BACK` 在 active 各只出现于一个原调用点。除 Parent Source Review #1 已批准的
Cloud exact context identity 表示、三处 closed `BAG_RETURN_ITEM` substitution、typed cache-point 投影和 private
helper 外，未发现 strategy、随机范围、4 秒与 8..18 秒时序、fallback、state、日志顺序或调用图差异。

本结论批准源码返修并关闭 CK3；不替代 A/B/C/D 并发 Java 全部稳定后的 fresh Cloud package 与 DHXY compile。
**无已批准业务差异；按 `696a12b0` 基线等价迁移。**
