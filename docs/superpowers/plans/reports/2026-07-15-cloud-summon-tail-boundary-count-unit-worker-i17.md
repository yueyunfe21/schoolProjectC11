# W-COUNT-SUMMON-TAIL-BOUNDARY-SCAN-1

CLAIMED

- workerRole: Internal implementation-only Worker I17
- countUnit: `SummonSkillTailBoundaryScanner::scanLockedBoundary`
- countDelta: `+1`（仅父级源码审查与 fresh Maven 同轮通过后应用）
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillTailBoundaryScanner.java`
- report: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-summon-tail-boundary-count-unit-worker-i17.md`
- state: `NO_CODE_CHANGE / DELIVERED`; parent source review and fresh Maven pending

## 交付结论

- `SummonSkillTailBoundaryScanner.java` 已完整实现本 count unit；本 Worker 保持 Java 零改动。
- `countDelta=+1` 是本单请求值，当前 `countApplied=0`。只能由父级源码审查与 fresh Maven 同轮通过后原子应用。
- 源码已闭合真实 `AutoBattleTask -> TaskMaintenanceService -> Cloud SummonSkillService typed whole-pass -> DHXY exclusive whole-pass -> 两个 active locked-boundary caller -> scanner 五态规则 -> closed cleanup value -> typed whole-pass terminal -> Cloud result/state`。
- 未新增或修改 owner/session/TTL/retry/wrapper；未修改 `SummonSkillService`、`TaskMaintenanceService`、DHXY local mechanics、wire、handler、schema 或其它文件。
- 已核对 `docs/业务逻辑.md:170-211`：LOCKED 向前跳过、NORMAL 删除、KEEP 停、EMPTY 做绝技检查、UNKNOWN fail closed；没有已批准业务差异，按 `696a12b0` 等价迁移。

## 基线与工作区

| repository | read-only status | scoped evidence |
|---|---|---|
| `D:/mavenProject/DHXY` | branch `thin-client-design`，大量共享 dirty/untracked | `git show 696a12b0:.../SummonSkillTailBoundaryScanner.java` 与 Cloud current 逐行 `Compare-Object` 无输出 |
| `D:/mavenProject/dhxy-cloud-brain` | branch `navigation-migration`，`src/main/java/com/bot/**` 为共享 untracked 树 | current scanner SHA-256 `18027108a3b27d78c68c92a95fb3ff2e18af71a38e777bc4d2c9c5997bcde01d` |

Cloud current scanner 与 DHXY current scanner SHA-256 完全相同。Cloud migration-baseline 文件因 CRLF/LF 的物理字节差异具有另一文件 SHA，但逐行比较无差异；业务源码与 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价。

## Whole-pass 可达链

1. Cloud `AutoBattleTask.execute` 在 `TaskExecutionContextHolder.callWith(context, ...)` 内运行；idle maintenance request 明确设置 `cleanSummonSkill(true)` 并调用 `TaskMaintenanceService.runOpportunisticMaintenance`。
2. Cloud `TaskMaintenanceService.runOpportunisticMaintenance` 进入 `maybeCleanSummonSkill`；保留 due/free-state、team round/capability/claim、cache/cooldown 与 `INTERACTING` finally，在 `TaskMaintenanceService.java:755` 调用 `summonSkillService.cleanSummonSkillsOnce(cleanupRequest)`。
3. Cloud `SummonSkillService.cleanSummonSkillsOnce(request)` 从同一 current context 构造四字段 `WholePassIntent`，只调用一次 `summonSkillWholePass().execute(intent)`；没有默认 context、自动重发或新 TTL。
4. DHXY `LocalRemoteGameCommandHandler.executeSummonSkillWholePass` 在 exact-window/task-run admission 后，使用一次 `submitRemoteExclusiveAndWaitDetailed`；input-worker callback 调用 DHXY `SummonSkillService.cleanSummonSkillsOnce(request)`，直接进入既有 local direct mechanics，零 queue-in-queue。
5. DHXY direct tail pass 的两个 active caller 分别处理“NORMAL 删除后复检为 LOCKED”和“当前 actionable 尾槽直接为 LOCKED”；二者调用与 Cloud baseline whole-Service 中保留的两个 caller 同构，并共同消费同一 scanner closed `Result`。
6. scanner 结果先汇总 `inspected/deleted/nextStartIndex`；失败直接构造 `success=false` cleanup，需绝技检查时在同一 whole-pass 继续 `maybeClickUltimateCorner`，最终进入九字段 cleanup value。
7. DHXY handler 返回 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；Cloud retained authority 只 final-consume 前三种 closed terminal，UNKNOWN 保持 unresolved fence。Cloud `SummonSkillService` 再映回 `SummonSkillCleanupResult`，由 TaskMaintenance 原 finally 决定 cooldown/cache/backoff/state。

## 两个 Active Caller

| caller | trigger | scanner result consumption |
|---|---|---|
| Cloud `SummonSkillService.java:463` / DHXY `SummonSkillService.java:535` | 当前 NORMAL 已删除，post-delete slot 变成 `LOCKED_SLOT` | 累加 scanner inspected/deleted；失败立即 `success=false`；有 `ultimateCheckIndex` 则做绝技角检查；随后结束本 pass |
| Cloud `SummonSkillService.java:519` / DHXY `SummonSkillService.java:590` | 当前 actionable slot 直接判为 `LOCKED_SLOT` | 同样完整消费 counters、next index、failure 与 ultimate-check；不存在只看 LOCKED 就提前成功的旁路 |

Cloud `SummonSkillService` 对 `696a12b0` 的 scoped no-index diff 仅包含既有 current-context/typed whole-pass adapter 与两个 `CloudUiCleanerPort` substitutions；两个 scanner caller、scanner wrapper及其 branch/order/result consumption 没有 diff。

## Scanner 五态闭环

| observed status / condition | exact behavior | closed `Result` |
|---|---|---|
| abort before inspect | 不读槽、不删技能 | `success=false`，保留当前 backward index，`deleted=0` |
| `NORMAL_SKILL` | 删除前再检查 abort；删除失败 fail closed；成功恰删一次 | `success=true`，`deletedCount=1`，`deletedIndex=i`，`ultimateCheckIndex=i` |
| `KEEP_SKILL` | 安全停止，不删、不做绝技检查 | `success=true`，`nextStartIndex=i+1`，`ultimateCheckIndex=null` |
| `EMPTY_SLOT` | 停在该空格并要求 caller 做绝技角检查 | `success=true`，`nextStartIndex=i`，`ultimateCheckIndex=i` |
| `LOCKED_SLOT` | 继续向前扫描 | 不提前构造 terminal |
| `UNKNOWN` 或任何非四个已知状态 | 立即 fail closed | `success=false`，不刷新成功 cooldown |
| 扫到索引 0 之前仍无 opened slot | 安全停止 | `success=true`，`nextStartIndex=0`，零删除、零绝技检查 |

`inspectedCount` 只在 inspector 实际返回后递增。NORMAL 成功删除后 caller 必须继续同一 pass 的 forced ultimate-corner check；该规则与 scanner JavaDoc、`docs/业务逻辑.md` 及矩阵 `SummonSkillTailBoundaryScanner::scanLockedBoundary` 条目一致。

## Terminal 与状态后果

| terminal/result | Cloud mapping | TaskMaintenance consequence |
|---|---|---|
| `Executed(cleanup.success=true)` | 九字段、五 slot enum 原样映射 | 更新 window scan state 与 `lastSummonSkillCleanAt` |
| `Executed(cleanup.success=false)` | closed cleanup failure，非 transport UNKNOWN | 不写成功时间；按原失败/状态变化规则处理 |
| `NotExecuted` | `SummonSkillCleanupResult.failed(message)` | 不写成功 cooldown |
| `Stopped` / interrupted | `TaskFatalException` unwind | 不转普通成功/失败，不另启 pass |
| `Unknown` | `TaskFatalException`，retained unresolved fence | fail closed；不制造成功 cooldown truth，不自动重发 |

## 写集与验证纪律

- Java write set：零变化；scanner 已满足任务，故 `NO_CODE_CHANGE`。
- 唯一写入：本固定报告。
- 未运行 build、Maven、test、runtime/application/server/host/Task/poller/UI/capture/input。
- 未执行 reset/checkout/clean/delete/stage/commit/branch/worktree 或其它 Git mutation。
- fresh Maven 与 `countDelta=+1` 均明确留给父级同轮执行；本 Worker 不提前记账、不作 reviewer approval。

`DELIVERED | task=W-COUNT-SUMMON-TAIL-BOUNDARY-SCAN-1 | countUnit=SummonSkillTailBoundaryScanner::scanLockedBoundary | requestedCountDelta=+1 | countApplied=0 | Java=NO_CODE_CHANGE | businessDifference=NONE | sourceReview=PARENT_PENDING | buildGate=PARENT_PENDING`

## Parent Source Review #1 - 2026-07-15T03:25:00-04:00

父级独立读取 active 与 `696a12b0` 的 `SummonSkillTailBoundaryScanner.java`，文件逐行一致；active
`SummonSkillService:463/:519 -> :567` 两个 caller 均真实可达，NORMAL delete、KEEP safe-stop、EMPTY
ultimate-check、连续 LOCKED 后退、UNKNOWN/timeout/delete-failure 失败结果完整进入 whole-pass terminal。
结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；fresh Cloud package 前不记账。

### Parent Count-Boundary Resolution

helper 的重复风险已复核：矩阵独立列出 `SummonSkillService::cleanTailNormalSkillsDirect`、Service adapter
`scanLockedBoundary` 与 `SummonSkillTailBoundaryScanner::scanLockedBoundary`；I17 只计 locked-tail closed algorithm，
不计 whole-pass 编排/transport。按当前 407 方法级 ledger，这是唯一 countUnit，原 SOURCE APPROVED 保持。
