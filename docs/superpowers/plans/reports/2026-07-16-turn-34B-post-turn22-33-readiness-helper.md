# TURN-34B post-TURN-22/33 readiness delta helper

- role: `readiness delta helper only`
- notRole: `implementation Worker / reviewer / manager / approver`
- snapshotAt: `2026-07-16T05:55:38.383-04:00`
- authority: 本报告只记录当前证据与后续派发输入，不改变任何卡片状态，不构成 `READY`、`APPROVED`、`CLOSED` 或 review 结论。
- onlyWrite: `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-34B-post-turn22-33-readiness-helper.md`
- prohibitedAndNotRun: 未改 Java、测试、CR271 主卡、`ACTIVE_WORK`、权威计划或 dashboard；未运行 Maven、JUnit、compile/package、runtime/application/server/Task/UI/capture/input；未执行 Git 命令或 Git mutation。

## 1. Evidence boundary

本 helper 已完整读取并交叉核对：

1. `AGENTS.md` 与 `docs/DHXY_CONTEXT.md`。
2. `docs/ACTIVE_WORK.md` 当前 CR271 顶部，以及权威计划
   `2026-07-15-https-turn-complete-migration-card-plan.md` 顶部和第 14-19 节。
3. TURN-22 原固定卡当前全部正文，最新 R1/R2 独立报告，以及父级 `PARENT DELIVERY REVIEW #4`。
4. TURN-33 原固定卡当前全部正文，最新父级 `PARENT SOURCE+TEST-SOURCE REVIEW #3`。
5. 两份既有 TURN-34B readiness 材料：
   `2026-07-16-turn-34B-readiness-preflight-helper.md` 与
   `2026-07-16-turn-34B-launch-preflight-helper-r2.md`。
6. HTTPS turn foundation、thin-client protocol design 与 `docs/业务逻辑.md`。
7. 当前 Cloud `TaskMaintenanceService`、`SummonSkillService`、`TaskExecutionContext`、maintenance models、turn scope/window models，以及所有真实 production caller。
8. 当前 TURN-28P、External A/B/C/D lane 与 TURN-34A/34C 写集材料。并发父级推进已按固定卡物理 EOF 重新核对。

当前源码锚点：

- `TaskMaintenanceService.java`: `1130` 行，SHA-256
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`。
- `SummonSkillService.java`: `1428` 行，SHA-256
  `D28E62A56C170BC26A6D16035670515E4FB8F55EEBF5D8356515D1565F1C1A46`。
- 未来 named test `TaskMaintenanceTurnContractTest.java`: 当前不存在。
- 未来固定实施卡 `2026-07-16-turn-card-TURN-34B.md`: 当前不存在。

## 2. Current readiness result

```text
TURN-34B readiness = NOT_READY
TURN-34B claimable = NO
TURN-34B fixed card = ABSENT
parent-written TURN-34B READY = ABSENT
External D = ONLINE_HOLDING, reserved implementation lane only
```

直接原因不是 TURN-33。TURN-33 的 source/test-source 启动门已经通过。当前唯一未满足的显式
`startDependsOn` 是 TURN-22；此外第 14 节要求父级把仍为 `PLANNED` 的卡明确转成 `READY`，该动作也尚未发生。

## 3. `startDependsOn = 21+22+23+26+33` current facts

| Dependency | 当前固定证据 | Source-side start fact | 对 TURN-34B 的准确影响 |
|---|---|---|---|
| TURN-21 | 固定卡 true EOF 为 Repair #1 父级 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING` | `SATISFIED` | build pending 属后续 cohort，不阻塞 34B source start |
| TURN-22 | 最新父级 Review #4 为 `P0/P1/P2=0/2/1 / REPAIR #3 REQUIRED / PREREQUISITE BLOCKED BY TURN-28P` | `NOT SATISFIED` | 旧 Review #3 的 source pass 已被父级明确覆盖；这是当前 READY 的决定性阻断 |
| TURN-23 | 固定卡 true EOF 为 Repair #1 父级 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD BUILD PENDING` | `SATISFIED` | named test/build pending 不阻塞 34B source start |
| TURN-26 | 固定卡 true EOF 为父级 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD BUILD PENDING` | `SATISFIED` | named test/build pending 不阻塞 34B source start |
| TURN-33 | 最新父级 Review #3 为 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING` | `SATISFIED` | TURN-33 最终 source contract 已可作为 34B 的 source 前置；仍不等于 TURN-33 或 34B 已批准 |

第 14 节的排班规则是：`startDependsOn` 全满足且文件写集与 active writer 不重叠后，父级才把 `PLANNED`
转为 `READY`。因此即使 21/23/26/33 的 source facts 已满足，也不能用四项通过推导或伪造 TURN-34B READY。

## 4. TURN-22 review blocker and exact READY effect

### 4.1 What changed after the old readiness reports

TURN-22 曾在 Parent Source Review #3 进入 `0/0/0 / source passed`，但最新两名独立 reviewer 分别写入：

- R1: `BLOCKED P0/P1/P2=0/2/0`。
- R2: `BLOCKED P0/P1/P2=0/1/1`。

父级没有直接采用 reviewer 自述，而是复读源码后在 Review #4 合并为
`P0/P1/P2=0/2/1 / REPAIR #3 REQUIRED`，并明确写明 05:14 的 source pass 被新证据覆盖。因此该阻断不是
`approvalDependsOn` 或普通 build-pending 注记，而是重新打开了 TURN-22 的 source/test-source 启动门。

### 4.2 Three adopted defects

1. Cloud `TeamReturnTurnContractTest` 直接导入 DHXY-only executor/mapper/queue/window/runtime 类；Cloud source tree 与
   POM 没有这些类型或依赖，点名测试无法 test-compile。
2. DHXY `TurnInputStepExecutor` 先按 frozen `TurnExecutionWindow` 映射绝对坐标，随后仍调用会再次 refresh mutable
   binding 的 legacy queue。旧坐标可能在新 HWND/rect/epoch 上执行。
3. context restore 证据是 empty-to-empty 伪阳性，没有 sentinel，也没有在真实 frozen queue 边界记录 exact
   windowId/HWND/process/rect/epoch。

### 4.3 Transitive TURN-28P status

TURN-22 Review #4 原文把 Repair #3 绑定到 TURN-28P Repair #1 的 frozen API。该 Repair #1 后续又被 TURN-28P
Parent Delivery Review #3 退回：`P0/P1/P2=0/2/1 / REPAIR #2 REQUIRED`。Maxwell 曾领取 Repair #2，但父级随后改交
External B；Maxwell 明确声明未修改 11 个冻结目标并释放 internal owner。External B 已在原固定卡真实领取。截至本快照，
TURN-28P 固定卡物理 EOF 是：

```text
TURN-28P EXTERNAL-B REPAIR-2 CLAIMED P0/P1/P2=0/2/1
session aa951b1e-8f04-4f92-b6e0-de08af49c39a
platform id/nickname pending parent correction
2026-07-16T05:55:04-04:00
```

所以 TURN-22 卡中较早写下的 “Repair #1 source gate” 不能视为已满足。当前有效前置是 TURN-28P Repair #2
由 External B 交付并经父级重新通过 source/test-source gate，形成最终 frozen API 后，External A 才能领取
TURN-22 Repair #3。External B 当前是该 11 文件 Repair #2 的 active implementation writer。

### 4.4 Exact READY consequence

```text
TURN-28P Repair #2 parent source/test-source pass
  -> External A claims and delivers TURN-22 Repair #3
  -> parent re-reviews TURN-22 source/test source and restores its S gate
  -> parent rechecks mutex and explicitly writes TURN-34B READY/fixed card
  -> External D may append real CLAIMED and start implementation
```

父级可继续要求独立 reviewer/build 门，但这些门的最终排班裁决仍归父级。本 helper 只确认：在 TURN-22 source gate
恢复且父级明确写出 TURN-34B READY 之前，External D 不得领取。

TURN-22 阻断不扩大 TURN-34B 的实现范围。34B 不得修 TeamReturn test、`TurnInputStepExecutor`、frozen queue、
`150/500` mechanics 或 sentinel fixture；文件写集互斥也不能绕过语义依赖。

## 5. Post-TURN-33 delta

既有 TURN-34B readiness 的 R3 “等待 TURN-33 最终 Summon contract” 已关闭：

- 正式 public boundary 仍是
  `SummonSkillService.cleanSummonSkillsOnce(SummonSkillCleanupRequest)`。
- 一次 due maintenance 只允许调用该 public API 一次。fresh static-tail observation、五次普通删除 budget、
  ultimate click 后结束本 pass、terminal/uncertain/STOP 与恰好一次 lightweight cleanup 都由 TURN-33 内部拥有。
- `TaskMaintenanceService` 不复制 Summon loop、action、UUID、PNG、OCR、click、cleanup 或 retry，也不吞 terminal exception。

既有 readiness 的两个 TaskMaintenance 源码接缝仍然存在，未被 TURN-33 修改：

1. turn-native first-due path 仍在 `TaskMaintenanceService.currentPlayerIdentityEpoch(...)` 调用 legacy-only
   `TaskExecutionContext.getPlayerIdentityEpoch()`，会在委托 Summon 前失败。未来修复只能留在
   `TaskMaintenanceService.java`，以 supplied exact turn context/metadata 建立 identity fence，不扩 context/protocol/model 写集。
2. singleton maps 的 key 仍以 plain `windowId`、task key、round 或 local session text 为主，尚未按既有
   tenant/user/device/window scope 隔离。未来只给现有 context-bearing state 加 namespace，不创建 owner/session/ledger/TTL
   或 durable workflow；四个无 context 且零 production caller 的 local-session API 不伪造 activation。

## 6. Final exact TURN-34B write set

父级未来写 `TURN-34B READY` 时，唯一允许的实施写集应冻结为三项：

1. Production, modify only:
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
2. Named test, create only:
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
3. Fixed implementation report, create/append only:
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34B.md`

第 2、3 项当前均不存在；本 helper 不提前创建。除以上三项外全部只读，尤其包括：

- `SummonSkillService`、`TeamReturnService`、`DialogService`、`CommonBoxService`、`PlayerStateService`；
- `TaskExecutionContext`、turn protocol/client/result、maintenance models、POM/config/resources/templates；
- `AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2`；
- DHXY 全仓及 TURN-21/22/23/26/28P/33 的 production/test/report。

`enqueueSummonSkillOnly` 当前只存在于 `TaskMaintenanceRequest` 声明/JavaDoc，production caller 为 `0`；34B 不得借本卡激活它。

## 7. Real public caller inventory

当前 `TaskMaintenanceService` 有 19 个 public 方法。真实 direct production caller 只来自四个类：
`AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2`。当前逐 API 调用事实如下，行号以本快照源码为准。

| Public API | Direct production call sites |
|---|---|
| `initializeForTaskStart` | `AutoBattleTask:136`; `WubeiTask:366`; `XiuluoTaskV2:330` |
| `beginTeamMaintenanceRound` | `WubeiTask:386`; `XiuluoTaskV2:371` |
| `openTeamPathingMaintenanceWindow` | `WubeiTask:2765`; `XiuluoTaskV2:3856` |
| `openTeamFirstAidMaintenanceWindow` | `WubeiTask:1763` |
| `closeTeamMaintenanceWindow` | `WubeiTask:600,771,1204,1710,3958`; `XiuluoTaskV2:3881` |
| `openLocalTeamReturnSupportWindow` | `WubeiTask:1801,1811`; `XiuluoTaskV2:2543,2553` |
| `closeLocalTeamReturnSupportWindow` | `WubeiTask:1793,1818`; `XiuluoTaskV2:2535,2560` |
| `isTeamPathingMaintenanceWindowOpen` | `WubeiTask:1119` |
| `awaitTeamFirstAidMaintenanceWindowOpen` | `AutoCombatService:554` |
| `awaitLocalTeamSupportCapabilityOpen` | `AutoCombatService:534`; `AutoBattleTask:239` |
| `isLocalSupportMemberSession` | `AutoCombatService:492,533,669`; `AutoBattleTask:193,198,236` |
| `registerLocalTeamSessionCandidate` | no external production caller |
| `markLocalTeamWindowRoleDetected` | no external production caller |
| `isLocalSupportMemberCandidate` | `AutoCombatService:551` |
| `isPendingLocalSupportLeaderDetection` | `AutoCombatService:485,545,678`; `AutoBattleTask:187` |
| `markLocalTeamLeaderDetected` | no external production caller |
| `isLocalTeamSupportCapabilityOpen` | `AutoCombatService:493,670`; `AutoBattleTask:203,245` |
| `completeLocalTeamSessionWindow` | no external production caller |
| `runOpportunisticMaintenance` | `AutoBattleTask:208`; `WubeiTask:1128,1433`; `XiuluoTaskV2:1289,1381,1538,3821` |

未来 34B 必须保持这 19 个方法的名称、参数、返回值和 caller-visible 语义，不改四个 caller 文件，不给四个零 caller API
制造 host/runtime/factory/Task activation。

## 8. Named test contract

- exact class: `com.yueyunfe.dhxy.cloudbrain.service.TaskMaintenanceTurnContractTest`
- exact file:
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
- profiles: 默认 `BC4+BASE`，额外 `TASK+STATE`
- future parent/stable-writer command: `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`
- current execution: `NOT RUN`

该测试应直接实例化 production `TaskMaintenanceService`，只用 test-private scripted collaborators/fakes，不启动 Spring、
HTTP、host、runtime、Task、UI 或物理 input/capture，不为测试增加 production helper/DTO/facade。

最小验收边界：

1. 锁住全部 19 个 public signature 与上述四个真实 caller 文件 byte-untouched；四个零 caller API 不冒充 runtime reachable。
2. `checkpoint -> maintenance broadcast -> handled/failed/interrupted short-circuit -> one Summon public call` 顺序精确；
   broadcast short-circuit 时 Summon 调用为 `0`，no-action 且 request 允许时恰好 `1`。
3. 保持 feature/interval/FREE/due/unknown interval/2h cache/team round/local capability/pathing/duplicate/max-claim 的现有
   顺序和逐值结果；不新增 TTL、额外 verification、retry、fallback 或 cleanup。
4. success、known failure、delete/ultimate state change、terminal/uncertain/STOP 的 cooldown/cache/claim 与 previous action-state
   投影精确；terminal 不伪 success、不自动重调，并原样传播 TURN-33 exception contract。
5. 真实 `TaskExecutionContext.turnNative(...)` first-due path不调用 legacy identity epoch；latest metadata missing 或
   device/window/HWND/process/title drift 在 Dialog/Summon 前停止，delegate/action/UUID 为 `0`；supplied context 优先于 holder。
6. 同 window/task/round 的不同 tenant/user/device scope 不共享 per-window cache/cooldown/formal team round/claim/window state；
   legacy/null fallback 保持基线。
7. pathing window 精确五项，first-aid weak window 仅 `FIRST_AID`，close 精确五项，return support 精确
   `TEAM_RETURN+COMMON_BOX`；不在 34B 消费 CommonBox 或执行 TeamReturn click。
8. 不重复 TURN-22 的 `150/500` JSON/queue fixture，也不重复 TURN-33 的 static-tail、PNG、action/UUID、五删 fixture；
   34B 只断言一次 typed Summon delegate 与 coordinator projection。

## 9. File mutex and lane ownership

| Lane/card | 当前/未来 exact write set | 与 TURN-34B 的关系 |
|---|---|---|
| External D / TURN-34B | `TaskMaintenanceService.java` + `TaskMaintenanceTurnContractTest.java` + fixed TURN-34B card | 与未来 34B 写集完全相同，故 External D 是唯一预留 implementation lane；其它 helper/worker 不得并写 |
| TURN-28P Repair #2 | DHXY `InputActionQueue`, `InputActionRequest`, `InputActionWorker`, `InputSequences`, `WindowAwareInputCoordinator`, `TurnCaptureStepExecutor`; DHXY 三个 named tests; Cloud 两个 named tests; fixed card | 文件级不重叠，但它是 TURN-22 的传递 source 前置；External B 已真实领取并为当前 active implementation writer |
| TURN-22 Repair #3 | Cloud `TeamReturnTurnContractTest`; DHXY `TurnInputStepExecutor`; DHXY `TurnInputStepExecutorContractTest`; fixed card | 文件级不重叠，语义依赖严格阻塞 34B READY；External A 尚不得领取 |
| TURN-33 | Cloud `SummonSkillService`, `CloudSummonSkillWholePassCapability`, `CloudTaskExclusiveInteractionAuthority`, named test, fixed card | 文件级不重叠；source contract 已通过，implementation owner 已释放；34B 只消费最终 public contract |
| TURN-34A / External C | Cloud `AutoCombatService.java`, `AutoCombatServiceTurnContractTest.java`, fixed card | 文件级互斥，可在各自 READY 后与 34B 并行；34B 不改 AutoCombat 状态机 |
| TURN-34C | Cloud `task/AutoBattleTask.java`, `task/AutoBattleTaskTurnContractTest.java`, fixed card | 文件级互斥，但逻辑上等待 34A+34B；34B 不提前实现 task-level startup/team/common-box 顺序 |
| TURN-21/23/26 | 各自固定 production/test/report，source owner 已释放 | 不与 34B 文件重叠；只读消费其已通过 public contract |
| 本 helper | 仅本 readiness delta 报告 | 与 External D 实施写集及未来固定卡完全分离 |

External D lane 报告仍保留较早的 `TURN-22 Repair #1 + TURN-33` 文案。当前权威固定卡已经推进为
`TURN-22 Repair #3`, transitive on `TURN-28P Repair #2`。旧 lane 文案不能开放 READY，也不能扩大 D 的三文件写集。

## 10. Exact future dispatch predicate

```text
TURN-21 source gate = PASS
AND TURN-23 source gate = PASS
AND TURN-26 source gate = PASS
AND TURN-33 source gate = PASS
AND TURN-28P Repair #2 parent source/test-source gate = PASS
AND TURN-22 Repair #3 delivered and parent source/test-source gate = PASS
AND no active writer overlaps the exact TURN-34B three-file write set
AND parent creates/freezes TURN-34B fixed card and writes READY at true EOF
```

只有上述谓词成立后，External D 才能在固定卡物理 EOF 追加真实 `CLAIMED`。本报告不得被当成其中任一 PASS、READY
或 CLAIMED 证据。

## 11. Business baseline and final helper state

```text
无已批准业务差异；按 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7
与 docs/业务逻辑.md 已确认规则等价迁移。
```

- readinessNow: `NOT_READY`
- blockingStartDependency: `TURN-22`
- transitiveActivePrerequisite: `TURN-28P Repair #2, External B CLAIMED and active`
- resolvedDelta: `TURN-33 source contract now passed; old TURN-34B R3 closed`
- exactWriteSetFrozen: `true, evidence only; parent must create the actual READY card`
- PRECHECK_COMPLETE: `true`

<!-- TRUE_EOF: TURN-34B POST-TURN-22-33 READINESS DELTA PRECHECK_COMPLETE true 2026-07-16T05:55:38.383-04:00 -->
