# W-COUNT-AUTO-PANEL-MISSING-WATCHDOG-1 Worker I29 Report

## CLAIMED

- task: `W-COUNT-AUTO-PANEL-MISSING-WATCHDOG-1`
- claimedAt: `2026-07-15T04:05:33.2050420-04:00`
- countUnit: `AutoCombatPanelService::recordAutoPanelMissing`
- countDelta: `+1`
- 唯一 Java 写集: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
- 唯一报告: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-auto-panel-missing-watchdog-count-unit-worker-i29.md`

## BLOCKED

- completedAt: `2026-07-15T04:10:30.4704484-04:00`
- finalCountDelta: `0`
- 结论: active caller 与 watchdog 判断本体存在，但“exact current-window warning/metric closed terminal”未闭合；修复必须越过唯一 Java 写集。未修改任何 Java，未做局部 state-key 补丁、stub、filler 或 wrapper。

### Active chain 逐跳证据

1. **AutoBattleTask caller 为 active**：Cloud `AutoBattleTask.java:139-149` 的 patrol loop 每轮调用 `handleAutoCombatTick(context)`；`:162-163` 真实调用 `autoCombatService.handleCombatTick(context, "auto-battle", false)`。
2. **handleCombatTick 到 panel 两条 active 分支存在**：Cloud `AutoCombatService.java:126-175` 是三参 caller 落入的完整 tick；`:152` 调 `maybeHandleCombatEnter(source)`，`:171-173` 在战斗态调 `maybeRunCombatMaintenance(...)`。
3. **ensurePanelVisible 分支存在**：`AutoCombatService.java:332-343` 在消费真实 combat-enter signal 后，按既有顺序写 4 秒 entry-maintenance 状态、日志，再于 `:342` 调 `autoCombatPanelService.ensurePanelVisible(source + ":combat-enter", 500)`。
4. **verifyAndAlignPanel 分支存在**：`AutoCombatService.java:645-662` 的 entry maintenance 于 `:657` 调 `ENTRY_MAINTENANCE`；`:688-732` 的既有 rounds maintenance 于 `:696-697`、`:727-728` 调 `VERIFY_AND_REFRESH`。Cloud `AutoCombatPanelService.java:88-102` 两种 mode 都先经 `ensurePanelMatchVisible(...)`，成功后才进入 `alignPanelIfNeeded(...)` 与既有 rounds 分支。
5. **watchdog transition callsites 存在且没有重复计 observation/input/alignment**：Cloud `AutoCombatPanelService.java:117-268` 先读 typed `AUTO_COMBAT_PANEL` fact，缺失时只发一次既有 Alt+8 bundle，再读一次 fact；仅 input 非 `EXECUTED` 的 `:206-208` 和 Alt+8 后仍 `NOT_FOUND` 的 `:255-258` 调 `recordAutoPanelMissing(...)`。首次 observation、第二次 observation、input bundle 本身、`:270-402` alignment 均没有独立 watchdog 计数。
6. **baseline 判断本体逐句保留**：当前 `AutoCombatPanelService.java:65-66` 仍为 10 分钟 attention threshold 与 60 秒 repeat limit；`:495-523` 保留 first miss 仅写 `autoPanelMissingSinceAt` 后 return、阈值前 still-missing 日志、阈值后 repeat guard、原 message/error log、warning+metric 调用顺序。仓内 `migration-baseline/696a12b0/.../AutoCombatPanelService.java:52-53,220-247` 与之逐句一致。
7. **成功清理仍等价**：当前 `AutoCombatPanelService.java:167-173,261-267` 在首次/Alt+8 后识别成功时清 streak；`:525-532` 同时清 `autoPanelMissingSinceAt` 与 `lastAutoPanelMissingAttentionAt`。基线对应 `:98-105,124-130,250-257`。

### 精确 blocker

1. **Cloud active task 绑定的是另一套 holder**：`AutoBattleTask.java:111-113` 仅调用 Cloud `TaskExecutionContextHolder.callWith(...)`；该 holder 源码 `TaskExecutionContextHolder.java:17-35` 只维护 `ThreadLocal<TaskExecutionContext>`。全量检索 Cloud active `src/main/java`，没有任何 `WindowTaskContextHolder.callWith/runWith/bind/set` 调用。
2. **streak 当前不是 exact-window**：`AutoCombatPanelService.java:733-737` 从 `windowTaskContextHolder.rawCurrent()` 取 key，空时落到共享 `"default"`。Cloud 的 exact window id 实际在 `TaskExecutionContext.java:62-65`，但当前 `state()` 未使用它；并发 Cloud 窗口会共享 missing/attention state。
3. **warning/metric terminal 当前不可达**：`recordAutoPanelMissing` 的 `AutoCombatPanelService.java:516-522` 把 `markRuntimeWarning` 与 `recordWindowWarning` 全包在 `windowTaskContextHolder.rawCurrent().ifPresent(...)` 中。由于 Cloud active chain 未绑定该 holder，10 分钟到期后只会写 error log，闭包不会执行。
4. **不存在可在唯一文件内复用的 closed remote warning terminal**：Cloud `CloudGameClient.java:40-53,68-84,98-123,139+` 公开的是 typed fact/capture/input/local-macro lanes；`RemoteGameClientPort.java:24-100` 同样没有 runtime-warning/metric 操作。当前仅有未接线的纯值类型 `RuntimeWarningNotification.java` / `RuntimeWarningIdentity.java`，全量 active 源码无生产者、route、DHXY handler 或 ACK consumer；它们不能代替 closed terminal。
5. **越写集要求**：要保持 `696a12b0` 的当前窗口 warning + metric 效果，至少需要 Cloud warning producer/route，以及 DHXY exact-window handler 调 `WindowRuntimeContext.markRuntimeWarning(...)` 和 `AutomationMetricsService.recordWindowWarning(...)`。DHXY 现有终点源码分别在 `WindowRuntimeContext.java:2042-2050`、`AutomationMetricsService.java:359-377,399-410`，均在禁止写集外。因此不能只改 `AutoCombatPanelService.java` 完整闭环。

### Baseline 与边界

- `696a12b0` 本地基线使用真实绑定的 `WindowTaskContextHolder`：`migration-baseline/696a12b0/.../AutoCombatPanelService.java:220-247,458-468`，其 per-window streak、10 分钟阈值、60 秒重复告警限流、日志、message、warning 后 metric 顺序是本轮必须保持的合同。
- 当前 Cloud watchdog 的数值判断与顺序没有业务差异；缺口是迁移后的窗口绑定/通知 transport plumbing。没有改动 baseline phase、fallback、state 清理、panel observation、Alt+8、alignment、round estimate 或日志文本。
- 没有把 `ensurePanelVisible`、panel observation、input bundle、`alignPanelIfNeeded` 重复计入本 countUnit。

### 修改文件

- `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-auto-panel-missing-watchdog-count-unit-worker-i29.md`
- Java 修改：无

### 未执行

- 按任务约束未运行 build、test、runtime、input 或 Git。

## Parent Source Review #1 - 2026-07-15T04:13:00-04:00

父级独立确认 active caller 和 10 分钟/60 秒 watchdog 本体存在，但当前 Cloud task 只绑定
`TaskExecutionContextHolder`；`state()` 仍从未绑定的 `WindowTaskContextHolder` 取 key 并回落共享
`default`，warning/metric 也包在该空 holder 的 `ifPresent` 中。结论 **P0=0/P1=1/P2=0，
BLOCKED_BY_SCOPE / countDelta=0**。完整修复必须增加与 C 当前 29-Java shared lane 冲突的 closed warning
transport 与 DHXY exact-window consumer，本轮不得并发扩单；Java 保持未改，本 Worker 关闭，待 shared lane
释放后以同一完整 `+1` 单重发。
