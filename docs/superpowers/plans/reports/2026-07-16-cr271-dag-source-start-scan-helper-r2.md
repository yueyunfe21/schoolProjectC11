# CR271 internal DAG source-start scan helper r2

## 1. 角色、时间点与裁决边界

- 扫描快照：`2026-07-16T10:40:02-04:00`。
- 角色：CR271 内部 DAG source-start readiness helper；不是 worker、reviewer 或 manager。
- 本文只给父级核实候选，不构成 `READY` 冻结、派工、claim、owner、source review、批准或关闭。
- 已完整回读 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 当前顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议与 `docs/业务逻辑.md`。
- 按权威计划区分 `S=startDependsOn` 与 approval/build gate：后者不能被冒充 source-start blocker，也不能被本文提前判定通过。
- 唯一业务 authority 为 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。无已批准业务差异；按基线等价迁移。

## 2. 当前真实 writer/owner 边界

| Lane | 当前 true EOF | 必须保护的 exact write set |
|---|---|---|
| External A / `TURN-28S2` | `PARENT-REASSIGNED EXTERNAL-A-NEXT ... CLAIM-REQUIRED`；QP1 已由父级 source review 通过并释放，A 已被正式改派到 S2 | Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`；`2026-07-16-turn-card-TURN-28S2.md` |
| External C / `TURN-34AT1` | `REPAIR #2 TEST-SOURCE DELIVERED`，test SHA `b5438da5...`；C 已停止编辑、等待父级审查，尚无父级接收/owner release | Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`；`2026-07-16-turn-card-TURN-34AT1.md` |

以下候选均在上述 A/C 写集之外。A 的 S2 虽尚待 replacement claim，但父级已明确把该 exact write set 改派给 A，必须立即排除；C 已交 delivery，在父级明确接收并释放 owner 前仍按受保护写集处理。

## 3. 扫描结论

完整 DAG 中只有 **1 张**同时满足以下条件的真实小片：已有父级冻结卡、当前零 owner、`startDependsOn` 已满足或该片本身可解除主链 source blocker、exact write set 与 A/C 均不重叠。`TURN-28S2` 原本满足技术 readiness，但在本报告落盘期间已被父级正式改派为 External A NEXT，因此按最新 true EOF 从候选撤下。没有为了凑满三张而把 helper、未来 tranche、同文件后继片或 final verification debt 算成 source-start。

### Priority 1 - `TURN-34BP1`

**当前锚点**

- 卡片 true EOF：`PARENT STALE-D-REVOKED ZERO-OWNER EXTERNAL-D-REPLACEMENT-READY CLAIM-REQUIRED`。
- 旧 D assignment 已被父级撤销，当前零 owner、零 WIP；fresh replacement 必须先在该卡追加 replacement claim。
- 冻结 SHA 未漂移：`TaskExecutionContext.java` = `6d4e4a20...`；`TaskExecutionContextTurnContractTest.java` = `d667d695...`。

**startDependsOn**

- 权威计划父卡 `TURN-34B`：`S=TURN-21 + TURN-22 production contract + TURN-23 + TURN-26 + TURN-33`；这些 production/source 合同已落盘可供本片实现。
- 当前 `TURN-34B` retained-production review 已把 exact native generation 缺口拆成 BP1；该卡自身明确为 `SOURCE-START OPEN`。
- `TURN-22` 最终 source/build、`TURN-34BT1`、双 review、named test 与 Cloud build 是后续批准门，不阻止 BP1 原位补 shared checkpoint。

**exact write set**

1. Cloud `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`。
2. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`。
3. Append-only `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34BP1.md`。

`TaskMaintenanceService.java`、未来 `TaskMaintenanceTurnContractTest.java`、A/C 文件、协议/client/result/POM 与其它文件全部只读。

**为何是真实且自解锁主链**

- 当前公共调用路径真实存在：`throwIfStopRequested()` -> `checkpointTurnMetadata()` -> `latestExactTurnMetadata()`。
- `latestExactTurnMetadata()` 当前只核 device/window 后即返回，尚未将 latest title/HWND/process 与 initial exact metadata 比较；这正是冻结卡要求的 generation fence 缺口。
- BP1 原位补齐 title/HWND/process 与 sticky `A -> B -> A'` 失效，并通过现有 public checkpoint path 测试；不新增 wrapper、service business gate、retry、session、ledger 或 TTL。
- 该片交付并被父级接收后，才能按顺序冻结/启动 `TURN-34BP2`、`TURN-34BP3`，因此它是 `TURN-34B` production 主链的首个自解锁 prerequisite。

**无 writer 冲突**

- 与 A 当前 `TURN-28S2` 的 `NpcClickService.java` 完全不同。
- 与 C 当前 `TURN-34AT1` 的 `AutoCombatServiceTurnContractTest.java` 完全不同。
- 卡片显式冻结 `TaskMaintenanceService.java` 与未来 BT1 named test，只修改 shared context 及其现有 named test。

**业务基线核对**

- `docs/业务逻辑.md:1-68`：当前本地队伍/session/窗口隔离边界。
- `docs/业务逻辑.md:69-169`：通用盒子按绑定窗口隔离、pending/消费优先级保持不变。
- `docs/业务逻辑.md:170-212`：召唤兽技能静态规则保持不变。
- `docs/业务逻辑.md:215-224`：强制基线等价门。
- `docs/业务逻辑.md:1253-1299`：修罗失败、STOP/pause 与恢复语义不得被 metadata fence 改写。
- 推荐：**当前唯一立即候选，最高优先重开并 claim**；它可以与 A 的 S2 并行，且应先于 BP2/BP3 与 TaskMaintenance 全量测试 tranche。

## 4. 为什么没有第二/第三张立即小片

### `TURN-34BT1` 不计入候选

- 最新 true EOF 是 `PARENT OWNER-RETURN-ACCEPTED ZERO-OWNER ZERO-TEST-BYTES FUTURE-REPLACEMENT`，并明确要求先有新的父级 assignment 与 fresh worker claim；目前不是可直接领取状态。
- 唯一目标 `TaskMaintenanceTurnContractTest.java` 当前确实不存在，但卡内 worker/父级共同记录这是从零构建、覆盖 19 个 public API + 6 个 TURN-34A API 的**数百行大测试**，不符合本次“下一张真实小片”。
- 它可在未来由有足够上下文的 replacement 承接，且 production `TaskMaintenanceService.java` 必须保持只读；本报告不把 future replacement readiness 提升为当前 source-start。

### 其余 DAG 的阻塞/排除证据

| DAG 区域 | 当前不能作为第二/第三张立即 source-start 小片的原因 |
|---|---|
| Foundation、`TURN-22D1`、`TURN-29..33` | 当前剩余是既有 source delivery 的 review/test/build debt，不是新的零 owner source slice；`TURN-22D1` 已双 review、仅 build pending。 |
| `TURN-28QP1` | 已完成父级 source review、owner released；剩余独立 review/build，不是新的 source-start 片。 |
| `TURN-28S2` | 最新 true EOF 已改派为 `EXTERNAL-A-NEXT`，其 `NpcClickService.java` + 子卡正是 A 当前写集，按用户条件排除。 |
| `TURN-34AT1` | C 已交 Repair #2，但父级尚未接收/释放；其 test + 子卡仍是 C 当前受保护写集。 |
| `TURN-28S3` | 与 S2 写同一 `NpcClickService.java`；必须等 S2 delivery、父级 receipt、owner release、final SHA 和新冻结卡。 |
| `TURN-34AT2/AT3` | 与 C 当前 AT1 写同一 `AutoCombatServiceTurnContractTest.java`；必须等 C Repair #2 最终接收及 owner release。 |
| `TURN-34BP2/BP3` | readiness helper 不是卡；必须先取得 BP1 delivery/父级 receipt，再按 BP1 final SHA 冻结，且后续会触及 `TaskMaintenanceService.java`。 |
| `TURN-27`、`TURN-34C`、`TURN-35..37` | 分别仍等 `TURN-28` final API、`TURN-34A/B` 完整 source 链或其上游 whole-task start gates。 |
| `TURN-38A/B/C`、`TURN-39` | 仍依赖三大 whole-task/context 分类与旧 facade 零引用闭合；当前 helper/precheck 不构成 READY。 |
| `TURN-40B/C/D` | 依次等待 `TURN-39` 与前一 activation tranche；不能越级 source-start。 |
| `TURN-41`、`TURN-42..47` | `TURN-41` 是用户 fresh-runtime gate；后续 manifest/deletion 只能在该门后进行。 |

## 5. 父级排班建议与复核点

1. A 的 `TURN-28S2` 已由父级改派，不再由本 helper 推荐给其它 lane；只需按卡等待 A 的 replacement claim/首窗真实增量。
2. 给 fresh External D/replacement 重新打开 `TURN-34BP1`，要求先 claim，再在冻结两文件内产生真实增量。
3. 暂不派第二/第三张 Java 卡。等 S2、AT1 或 BP1 的父级 receipt/owner release 产生新边界后，再从 S3、AT2 或 BP2 链重新冻结真正不冲突的小片。
4. 父级派工前必须重新读取候选卡 physical true EOF 与目标 SHA；本报告只代表 `10:40:02-04:00` 快照。若 owner、SHA、write set 或父级状态变化，应以新 true EOF 为准。

本 helper 未修改 Java，未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，未执行 Git mutation，未批准任何卡；除本报告外未写入其它文件，并保护了两仓全部 dirty/untracked。

TRUE_EOF PRECHECK_COMPLETE
