# Seven-Lane Delivery Preflight Helper 3

> 时间：2026-07-15  
> 角色：非绑定 Delivery Preflight + Next-Task Queue helper  
> 状态：进行中，已建立报告；正在完整读取规定证据

## 优先交付快照（2026-07-15 03:21 EDT）

> 本节响应父级最新优先级，只做下一单队列与 I15/I16/I17 去重预检。以下任务均为非绑定候选；
> 只有对应 lane 当前 EOF 材料被父级释放后，才可由父级选择主单或备选之一。主单与备选不能同时领取。

### 当前 true EOF

| Lane | 物理 EOF | 当前末项 | 下一单起排条件 |
|---|---:|---|---|
| A | `8012` | `TaskStartupCheckService::checkAutoBattle` 已交 `NO_CODE_CHANGE`，等待父级后续门 | 当前单位释放后 |
| B | `9650` | `BaseTaskTemplate::sleepSafely` 已交 `NO_CODE_CHANGE`，等待父级后续门 | 当前单位释放后 |
| C | `7382` | TaskTracker 21 文件扩单仍缺 Cloud emitter/port/assembly；声明 21 个 Java 均未改 | 父级处理当前 scope 缺口并明确释放后 |
| D | `7612` | `SmartClickEvidenceConfirmationService::confirmExpectedOptionProof` 已交 `NO_CODE_CHANGE`，等待父级后续门 | 当前单位释放后 |

### A/B/C/D 下一张主单与备选

四张主单的唯一业务 Java 写集依次为 Cloud `AutoCombatService.java`、`PlayerStateService.java`、
`CommonBoxService.java`、`AutoCombatPanelService.java`，彼此互斥，也不命中 C 当前 TaskTracker 21 文件写集。
每个备选只替换本 lane 主单；同 lane 主备共享或替换该 lane 写集，不与主单并行。

| Lane | 顺位 | countUnit | countDelta | 真实 active caller / final consumer | 非绑定预检 |
|---|---|---|---:|---|---|
| A | 主单 | `AutoCombatService::maybeHandleCombatEnter` | `+1` | `AutoBattleTask:163 -> handleCombatTick:152/202 -> maybeHandleCombatEnter:332`；消费 enter one-shot 后写 4s maintenance 状态并调用 panel visible typed 链，后续 tick 消费状态 | `CLEAR` |
| A | 备选 | `AutoCombatService::initializeForCurrentWindow` | `+1` | `AutoBattleTask:137` 直接调用；重置当前 window 的 combat/runtime 状态后 patrol 继续 | `NEEDS_PARENT_DECISION`：纯状态 count unit 无独立远端 mechanics，需确认 ledger 按矩阵方法单列，而非并入 AutoCombat whole tick |
| B | 主单 | `PlayerStateService::ensureSheYaoXiangActive` | `+1` | `TeamReturnService:67` 直接调用，另由 leader post-combat wrapper 接入；status observation -> Cloud 决策 -> DHXY bag-use mechanics -> boolean consumer | `RISK`：此前同 countUnit 仅领取后因 typed status/执行闭包不足停在 `countDelta=0`；重发必须一次带齐旧缺口 |
| B | 备选 | `PlayerStateService::probeAndConsumeHealthyFirstAidNoFocus` | `+1` | `AutoCombatService:382/397/462` 直接消费 `HEALTHY/SUPPLY_NEEDED/UNKNOWN`，随后决定 pending/cached-plan 分支 | `NEEDS_PARENT_DECISION`：当前矩阵以 bars-probe 算法 cohort 描述，需先确认该 public wrapper 就是唯一 ledger countUnit，且不与 startup/cached-first-aid 已列单位重叠 |
| C | 主单 | `CommonBoxService::clearPendingForRole` | `+1` | `CommonBoxService:115/260` 的 role switch-off active 分支调用；清除对应 role pending 后原 detect/consume caller 返回 | `NEEDS_PARENT_DECISION`：矩阵单列且未见既有 `+1`，但它嵌在已列 member-detect/consume 图中，需确认不是同一 pending 链重复计数 |
| C | 备选 | `BattleRadarService::updateCombatState` | `+1` | `BattleRadarService:71/89/107/130/193` 的 active radar 路径调用；产生 enter/exit one-shot state，最终由 AutoCombat tick 消费 | `RISK`：它是 private state core；whole-Service 计划禁止把 private helper 单独当进度，只有父级确认矩阵该行是独立完整状态链时才可使用 |
| D | 主单 | `AutoCombatPanelService::verifyAndAlignPanel` | `+1` | `AutoCombatService:657/696/727` active maintenance 调用；panel observation -> align -> rounds decision/typed refresh -> boolean consumer | `RISK`：此前领取后停在 `countDelta=0`；I7/I12/I15 已补部分依赖，但当前 `alignPanelIfNeeded` 仍含 Cloud-local tracker/input，必须整链闭合后才是同一 `+1` |
| D | 备选 | `AutoCombatPanelService::alignPanelIfNeeded` | `+1` | `verifyAndAlignPanel:96` 直接调用并消费返回 match；距离阈值决定 drag/skip，结果继续 rounds 分支 | `RISK`：当前仍依赖 Cloud-local window base、`InputSequences` 与复查；必须作为完整 typed geometry/drag/result 链，不能交 helper-only 或只改调用名 |

**未计数核对口径：** A 主/备、C 主/备未在当前 source-pending 清单中发现 exact `+1` 记录；B 主、D 主及其
相关备选只有历史领取/缺口材料或 helper queue 材料，没有 fresh-build 后 ledger 应用记录。候选启用前仍需父级以最新
ledger 原子复核，避免 EOF 之后的并行父级更新造成重复。

### I15/I16/I17 计数边界去重预检

| Lane | 当前材料 | 去重边界 | 非绑定结论 |
|---|---|---|---|
| I15 | `AutoCombatPanelService::resolveRoundsRefreshReason`，`NO_CODE_CHANGE` | 矩阵 `:1334` 与 I12 `refreshAutoCombatRoundsIfNeeded` 的 `:1335` 是两个文字行；但 I15 的 caller-to-terminal 证明复用了 I12 同一 visible-read、typed `Alt+8` 和 success-only reset 链，且两单共享同一 Java 文件/同一 active SHA | `NEEDS_PARENT_DECISION`：若 ledger 按矩阵方法行逐项计数，可与 I12 分开；若按 strict caller-to-typed-terminal 完整链计数，I15 是 I12 决策子段重数风险，暂不得同时形成两个 `+1` |
| I16 | `TaskMaintenanceService::shouldSuppressIdleMaintenanceBroadcast`，无 Java 变化、当前 `countDelta=0` | 矩阵 `:1395` 与 `runOpportunisticMaintenance` 的 `:1394`、I13 due/cache 的 `:1396` 文字上独立；但 active Cloud 无 AutoBattle consumer，也无 tooltip-group/leader producer/state | `CLEAR`（仅对当前零计数处置）：没有新增计数可重复。后续若重发，必须同时闭合 AutoBattle request 前消费点与唯一 tooltip-group owner，且不得把该 30s suppression 规则折入 `runOpportunisticMaintenance` 或 I13 |
| I17 | `SummonSkillTailBoundaryScanner::scanLockedBoundary`，`NO_CODE_CHANGE` | 矩阵 `:1393` 单列 scanner，但 I17 的完整证明全部穿过已列 SummonSkill whole-pass，同一两个 scanner caller、同一 cleanup value、同一 typed whole-pass terminal；whole-Service 计划 `:485-487` 明确 private/helper-only 不独立计进度 | `RISK`：当前证据更像已列 whole-pass 的算法子段。除非父级确认 ledger 的 `:1393` 是独立 countUnit 且 whole-pass 先前未包含该行，否则与 I5 whole-pass 同轮 `+1` 有重复计数风险 |

### 立即交接点

1. A/B/D 当前 EOF 都是交付等待态，C 当前 EOF 是 scope 缺口；本 helper 不替父级释放 lane。
2. 父级若立即续派，先按上表逐 lane 选主单；主单不可用时只用本 lane 备选，不同时启用。
3. I15 与 I17 在计数模型未澄清前保留 `countApplied=0` 最稳妥；I16 当前继续保持 `countDelta=0`，其缺口不是构建可解决的问题。

## Parent Resolution - 2026-07-15T03:31:00-04:00

父级按当前 407 方法级 ledger 复核矩阵独立行：I15 pure reason 与 I12 physical refresh 分开；I17 locked-tail
algorithm 与 whole-pass orchestration/adapter 分开。两项保持 `SOURCE APPROVED / COUNT PENDING BUILD`；I16 仍
`BLOCKED/countDelta=0`。本 resolution 不把 helper 变成 reviewer。

## 权限与结论词边界

- 只做交付预检、写集监控、下一任务排队和计数边界去重。
- 不是 reviewer、manager、implementer；不写 Java，不运行 Git/build/test/runtime/input，不清理、回滚、覆盖或提交共享工作区内容。
- B 线预检只使用 `CLEAR` / `RISK` / `NEEDS_PARENT_DECISION`，不使用审批或阻断结论词。
- 当前只确认报告已创建；所有业务/契约结论等待规定材料完整读取后补写。

## 当前进度

- [x] 已读取本轮用户约束与 `superpowers:using-superpowers`、`superpowers:writing-plans` 工作规范。
- [ ] 完整读取 `AGENTS.md`。
- [ ] 完整读取 `docs/DHXY_CONTEXT.md`。
- [ ] 完整读取 `docs/ACTIVE_WORK.md` 顶部 CR271。
- [ ] 完整读取 `696a12b0` whole-Service 计划与服务迁移矩阵。
- [ ] 定位并完整读取 A/B/C/D true EOF。
- [ ] 完整读取 I15/I16/I17 报告。
- [ ] 预检 B Navigation current-map 已批准改动和 I12 typed refresh 的编译/契约风险。
- [ ] 监控 C TaskTracker 21 文件写集与 `READ -> Cloud算法 -> MATERIALIZE -> final-consumed` 闭合。
- [ ] 为 A/B/C/D 各准备一张主单和一张备选单，均满足尚未计数、真实 active caller、互斥、`countDelta=+1`。
- [ ] 对 I15/I16/I17 做计数边界去重预检。

## 暂定判定口径

### B 线

- `CLEAR`：现有静态证据未发现编译形状、类型、调用契约或计数重复风险。
- `RISK`：存在可具体定位的编译/契约/重复计数风险，但本 helper 不作审批或实现处理。
- `NEEDS_PARENT_DECISION`：证据存在语义选择、写集冲突或计数归属冲突，需要父协调者决定。

### C 线

- 写集必须限制在该交付声明的 21 个文件内；只记录越界事实，不修改共享文件。
- “真闭合”必须同时出现：真实 active caller 的输入读取、Cloud 侧算法决策、客户端物化/执行、最终结果被调用方消费；仅 DTO/port/adapter 存在不算闭合。

### 下一任务队列

- 每线主单与备选必须互斥，且不得与已计数交付、I15/I16/I17 边界或其它线候选重复。
- 每个候选必须能指向真实 active caller，并能说明唯一 `countDelta=+1` 的计数归属。

## 待补证据与结论

规定材料读取完成后在本节追加具体文件、符号、调用链、风险等级、主单/备选和去重矩阵。
