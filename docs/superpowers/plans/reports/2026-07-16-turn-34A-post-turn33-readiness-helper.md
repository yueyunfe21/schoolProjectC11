# TURN-34A post-TURN-33 source-gate readiness delta

- 日期：2026-07-16
- 角色：TURN-34A post-TURN-33 source-gate readiness helper；不是实现者、reviewer、manager 或 approver。
- 唯一写入：本报告。
- 结论口径：只判断 `startDependsOn` 的 source gate、冻结材料和写集是否足以供父级直接发出 `READY`；不创建固定卡、不写 `READY/CLAIMED/APPROVED`，不替代父级裁决。
- 执行边界：未修改 Java、测试、CR271 主卡、权威计划、External lane 报告或 dashboard；未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，也未运行或修改 Git。

## 1. 本轮 delta 结论

按权威计划第 14-19 节对 `S=` 的明确定义，TURN-34A 当前 concrete source-start 前置已经全部满足：

```text
TURN-19 + TURN-20 + TURN-21 + TURN-23 + TURN-24A + TURN-33
              -> source gates all passed
```

其中注册表的 TURN-34A 行仍写 `S=19+20+21+23+24+33`，而 TURN-24 已拆成不可直接领取的父卡，实际 AutoCombat concrete gate 是 TURN-24A。既有两份 TURN-34A readiness、CR271 记录和当前源码都按 TURN-24A 解释。父级固定卡仍应把这一点显式规范化，不能让 Worker 猜 `TURN-24` 是否另有未完成实体。

当前状态应分层记录：

| 层级 | 只读结论 |
|---|---|
| `startDependsOn` source gate | `MET`；TURN-33 Parent Review #3 是本轮唯一新增释放信号 |
| TURN-33 独立 reviewer / named test / build | 仍 pending；按计划是后续 approval/build gate，不是既有 `S=` source-start gate |
| TURN-34A 父级 fixed card | 不存在：`2026-07-16-turn-card-TURN-34A.md` 当前未创建 |
| TURN-34A 父级 `READY` | 尚未写出 |
| External C 对固定卡的真实 `CLAIMED` | 尚未发生；External C 只有 lane claim，且其最新正文仍停留在旧 TURN-33 gate-closed 快照 |
| 本 helper 权限 | 仅给 READY 证据和冻结缺口；不自行开放写集 |

有一处流程口径必须由父级在发卡时消歧：权威计划第 16、18 节与既有 External C launch-preflight 明确把 `S=` 定义为 source-start gate；但 `docs/ACTIVE_WORK.md` 当前顶部又写了“TURN-34A/C 只在 TURN-33 reviewer/build 与依赖门满足后继续”。如果这句话只是提醒最终 approval gate，则父级可基于本报告直接冻结并标记 TURN-34A `READY`；如果它是有意升级 start gate，父级必须明确写成对第 16/18 节的覆盖规则。External C 在父级消歧并写固定卡前仍不得开工。

## 2. 各前置 source gate 的当前字节证据

本轮不只复述旧状态，还只读重算了当前 Cloud production/test SHA。除 TURN-20 的共享 `LocalOcrClient` 被后续获批 TURN-26 串行提升 typed visibility 外，相关字节均与各卡 Parent source/test-source pass 记录一致。

| Gate | 最新父级 source 结论 | 当前字节核验 | 对 TURN-34A 的结论 |
|---|---|---|---|
| TURN-19 | `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | `LeftTopStatusSwitchService=03E43188...1EF2`、assembly=`9B767117...F7E1`、test=`C9D0B21A...EF8A`，均与父级记录一致 | `MET`；保留一个 command 内 `MOVE/WAIT120/CLICK/WAIT250`、零 retry |
| TURN-20 | `0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | `AutoCombatPanelService=E32C1AA9...0982`、test=`D6016392...1DD8` 与 TURN-20 一致；`LocalOcrClient` 当前为 `0E41A18B...4CAA`，差异来自随后 TURN-26 获批的 public typed API/JavaDoc，父级明确确认 `readJoinedText` 及 endpoint/codec/timeout/失败语义未改 | `MET`；不是未审漂移 |
| TURN-21 | `0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | `CommonBoxService=93E93321...E68`、assembly=`BC60C098...AE7D`、test=`6C3FFA9E...6D50` 一致 | `MET`；30 秒 pending、current identity fence、`MOVE/WAIT80/CLICK/WAIT120` 保持 |
| TURN-23 | `0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | `PlayerStateService=865A66B7...548`、first-aid port=`F66624A9...895`、incense port=`8CD5A67B...5C0`、test=`FAD55239...1D1` 一致 | `MET`；initial HWND/process pre-UUID fence 与 first-aid/incense fallback 保持 |
| TURN-24 / 24A | TURN-24 已拆分；TURN-24A Repair #1 为 `0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | `BattleRadarService=FB606FC5...6202`、test=`C353DFE9...C8A0` 一致 | concrete gate `TURN-24A=MET`；父级固定卡需把注册表 `24` 规范化为 `24A` |
| TURN-33 | Parent Review #3：`0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING` | `SummonSkillService=D28E62A5...1A46`、capability tombstone=`3EE97295...A6D`、authority=`91349697...BCC`、test=`68312D38...B7DC`，与最新交付/Review #3 一致 | `MET` for `S=`；真实 ultimate click 后当前 pass 结束，后续 static scan/command/UUID=0 |

TURN-33 对 TURN-34A 只是 source/architecture sequencing gate。当前 `AutoCombatService.java` 对 `SummonSkillService`、whole-pass capability、Summon exclusive authority、`summonSkillWholePass` 和 `executeSummonSkillWholePass` 全部零引用；TURN-34A 不得因为前置来自 TURN-33 而新增 Summon coupling。

## 3. 当前 AutoCombat 源码事实

当前 Cloud 文件：

```text
D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java
lines=836
sha256=80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D
```

该 SHA 与两份既有 TURN-34A readiness 相同，说明 TURN-33 Repair #2 没有改动 AutoCombat 字节。当前文件的业务结构仍是 `696a12b0` AutoCombat 加两处已闭合的 `CloudUiCleanerPort.closeAllGenericWindows(...)` 适配；本轮没有发现 post-TURN-33 新 public surface 或新 caller。

当前一文件迁移缺口也未变化：

1. 仍 import/inject 不存在于 Cloud main source 的 `TaskTurnCoordinator`、`WindowRuntimeContext`、`WindowTaskContextHolder`。
2. runtime state 仍只按 `windowId`，且可回退 `"default"`；identity 仍依赖 turn-native 不可用的 legacy `playerIdentityEpoch`/`epoch=0` 形状。
3. CommonBox 与 follower first-aid 仍包在本地 `TaskTurnCoordinator.enter/forceRelease` 中。
4. 除上述三个旧 runtime/coordination symbol 外，当前 AutoCombat 对 old facade/fact/macro/direct input/capture、七个 `BATTLE_RADAR_*` fact 与 Summon authority 均为零直接引用。

因此 TURN-34A 不需要第二个 production 文件即可移除剩余旧 runtime imports、重建 exact state ownership，并保持 typed collaborator 调用顺序。

## 4. 最终 public surface 与真实 production callers

最终 public surface 必须保持当前 Cloud / `696a12b0` 形状。当前 Cloud main source 的真实直接 caller 只有四个 Task：`AutoBattleTask`、`FiveRingTaskV2`、`WubeiTask`、`XiuluoTaskV2`。Cloud Task host 尚未激活属于 TURN-40，不影响这些是 production source caller，但 TURN-34A 不能把 host 激活伪装成自己范围。

| Public surface | 当前真实 caller / 消费 |
|---|---|
| `TickResult { NONE, IN_COMBAT, EXIT_RECOVERED }` | 四个 Task 的 phase/loop 分支；不得增删或重解释枚举值 |
| `PostCombatRecoveryPolicy { FULL_RECOVERY, FULL_RECOVERY_WITH_LEADER_INCENSE, FAST_EXPECTED_EXIT }` | Wubei、Xiuluo；boolean overload 只映射 full / full+incense |
| `initializeForCurrentWindow()` | AutoBattle `:137`；Wubei `:351,788,3447,3624`；Xiuluo `:2052,2229,2793,2803`；当前 Cloud FiveRing 无调用 |
| `handleCombatTick(context,source,boolean)` | AutoBattle `:163` 传 `false`；FiveRing `:1853` 传 `true` |
| `handleCombatTick(context,source,policy)` | Wubei `:3595` full+incense、`:3756` FAST；Xiuluo `:1828` full+incense、`:2063` 按 `TRACKER_CONFIRM` expected combat 选 FAST，否则 full+incense |
| `handleWindowCombatGuardTick(context,source)` | 当前 Cloud main source 无 caller；基线 DHXY runner watcher caller。保留 public/行为，不在 34A 新增 Cloud caller |
| `probeWindowCombatStateReadOnly(context,source)` | Wubei 直接点 `:4164`，由回程失败路径调用；Xiuluo 直接点 `:2436`，由回程失败路径调用；只有 `IN_COMBAT` 是可信纠正 |
| `getDynamicPollingIntervalMs()` | AutoBattle `:287` |
| `nextCombatMaintenanceDelayMs()` | 无外部 caller；仅由 `nextCombatWakeDelayMs()` 汇总 |
| `nextCombatWakeDelayMs()` | Wubei `:918`、Xiuluo `:2248`，各自再 clamp 到 `500..10000ms` |
| `hasPendingFollowerFirstAidForCurrentWindow()` | AutoBattle `:281`，pending 时使用 `500ms` polling |
| `hasPendingLeaderPostCombatRecoveryForCurrentWindow()` | **只有 Xiuluo `:2468` 直接调用**；既有 readiness 把 Wubei 也写成先查 pending 不准确，Wubei 没有该 direct caller |
| `refreshFastExpectedExitBaselineAfterTrustedInCombat(source)` | Wubei `:4167`、Xiuluo `:2439`；均只在 read-only probe 返回 `IN_COMBAT` 后调用 |
| `consumePendingLeaderPostCombatRecoveryIfAllowed(context,source)` | Wubei `:2777` 在 tracker-green progress 安全点直接调用；Xiuluo `:2471` 在 `hasPending...` 后调用；两者都不把 boolean 提升成新 phase truth |
| public `RefreshDuePanelVerifyDecision` / `RefreshDuePanelVerifyGate` / `reserveIfAllowed(...)` | 无外部 production caller；仅 AutoCombat 内 refresh-due path 使用。保留 public shape 和 30 秒 team-sharing 语义，不趁迁移收缩 API |

四个 caller 的关键返回消费必须原样保留：

- AutoBattle：任意非 `NONE` 都 sleep/continue；`NONE + FREE` 才做 idle maintenance。
- FiveRing：`IN_COMBAT` 继续 shared wait；`NONE` 只告警，随后和 `EXIT_RECOVERED` 一样进入 `SYNC_TASK_PANEL`；34A 不补 FiveRing initialize。
- Wubei：enter phase 的 full+incense 维持 `IN_COMBAT -> WAIT_BATTLE_FINISH`、`EXIT_RECOVERED -> POST_BATTLE_RECOVER`；FAST wait 维持 exit/recovered、in-combat prescan+park、`NONE` 的既有 never-saw-combat retry 条件。
- Xiuluo：tracker shortcut 只把 `IN_COMBAT` 当 incidental wait；expected `TRACKER_CONFIRM` 选择 FAST；exit 继续 incidental/unknown/expected 三分支，`NONE` 不成为新业务事实。

当前 DHXY 在 `696a12b0` 后增加的六个 API 仍不属于 TURN-34A Cloud public surface：

```text
authorizeCombatDetectionAfterEnterBattleAction
revokeCombatDetectionAuthority
probePausedWindowCombatStateReadOnly
consumeQueuedLeaderPostCombatFirstAidIfHead
reportXiuluoLeaderFirstAidAfterVerifiedReturn
reconcileReturnHomeVerifiedCombatState
```

父级若要其中任一项，必须另开已批准行为差异与 caller 写集；External C 不得顺手迁入。

## 5. 最终 exact write set 与 named test

TURN-34A implementation owner 的完整写集应严格只有三项：

1. Modify production：
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java`
2. Create named test：
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`
3. Create/append fixed card report：
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md`

当前 named test 与 fixed card 均不存在，路径无既存 owner 冲突。任何 private key/fingerprint/value type只能放在 `AutoCombatService.java` 底部；不得新增 production helper、protocol、fixture、shared test helper、第二 source guard、POM/config/resource、caller 或 DHXY 写集。External C 的 lane 报告是父级调度材料，不是 TURN-34A implementation write set；本 helper 报告也不能被实施者继续追加。

唯一点名命令（未来由获准 owner/父级按 writer-stable gate 执行，本 helper未执行）：

```text
cd D:/mavenProject/dhxy-cloud-brain
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

测试 profiles 是默认 `BC4+BASE` 加专属 `TASK+STATE`。测试必须实例化 production `AutoCombatService`，通过 production `TaskExecutionContextHolder.callWith(...)` 绑定 turn-native exact context，并执行全部 public surface及四个当前 Task caller 的真实返回消费；不能只反射常量、复制 reducer、调用 private helper或用 source text 代替行为断言。

最小矩阵仍应覆盖：两 overload/null policy、initialize 非对称 reset、四阶段 radar/FAST、dynamic/wake delay、enter/exit/三 recovery policy、CommonBox-before-first-aid、follower pending/one re-probe、deferred leader clear-before-recovery、`4s/40s/30s/10s` maintenance、guard/read-only 零 exit consume、四 Task caller phase、scope/fingerprint isolation、STOP/uncertain/correlation、每业务 action 一 UUID/command与零 transport retry，以及同一测试内的 active-source zero-reference gate。

## 6. External C 与其它写集互斥核验

| Lane/card | 当前或预留写集 | 与 TURN-34A 路径交集 | 约束 |
|---|---|---|---|
| External C | 预留 TURN-34A；当前只持有独立 lane report，尚未 fixed-card claim | 将成为同一 owner，不是第二并发 owner | 父级 READY 后必须先在新 fixed card true EOF 真实 `CLAIMED` |
| TURN-28P Repair #1 / Maxwell | 双仓 protocol、DHXY keyboard/mapper/input/capture/local executor、Cloud `TurnInvocationResult` 及各自 tests/report | `empty` | 当前 Java writer，不得并发 clean；不阻断 34A 路径所有权，但影响 Maven 时机 |
| External A / TURN-22 | TeamReturn service/assembly/test/card | `empty` | 最新 review blocker 不在 TURN-34A `S=` 中；不得把 TURN-22 误加成 34A 前置 |
| External B / TURN-28 | NpcClickService、ObjectiveTextRecognizer、SmartClickRecognizer、named test/card | `empty` | 仍受 TURN-28P；与 34A 无文件交集 |
| External D / TURN-34B | `TaskMaintenanceService.java`、`TaskMaintenanceTurnContractTest.java`、34B card | `empty` path-level | 计划允许 34A/34B 并行，但有六个 public method 的语义耦合，必须先由父级冻结签名 |
| TURN-34C | `task/AutoBattleTask.java`、其 named test/card | `empty` | `S=34A+34B`，不得在 34A/34B source gate 前并发改 caller |
| TURN-35/36/37 | 分别 Wubei/FiveRing/Xiuluo 主文件、各自 test/card | `empty` | 都依赖 34A；当前 readiness 只能读 caller，不能先改 |
| TURN-19/20/21/23/24A/33 | 各自已审 production/test/card | `empty` with final 34A set | 34A 只消费，不返修依赖文件 |

与 TURN-34B 并行前必须冻结且保持不变的六个 `TaskMaintenanceService` API：

```text
isPendingLocalSupportLeaderDetection(TaskExecutionContext)
isLocalSupportMemberSession(TaskExecutionContext)
isLocalTeamSupportCapabilityOpen(TaskExecutionContext, TeamSupportCapability)
awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext, TeamSupportCapability, long)
isLocalSupportMemberCandidate(TaskExecutionContext)
awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext, String, long)
```

34B 可以迁移其内部 owner/turn plumbing，但不能在并行期改变上述返回、等待、timeout、capability 或 candidate 语义。若 34B 必须改签名，父级应串行重冻 34A，而不是让两个 Worker各自猜接口。

## 7. 父级仍需写入 fixed card 的冻结点

以下不是新增 source dependency；它们是父级发 `READY` 前必须写死、避免 External C 自行选路的合同：

1. **状态机 gate 口径：**明确采用计划 `S=` source-start 语义，或明确声明 TURN-33 reviewer/build 是新的 start override。默认计划口径下，本轮证据足以直接 READY，review/build 继续作为卡批准门。
2. **依赖名称：**把 TURN-34A 的 `TURN-24` 具体化为 `TURN-24A`，并记录其 Parent Repair #1 source pass。
3. **public surface：**保留第 4 节全部 baseline public API/enum/record/gate，排除六个 DHXY 后增 API；不改 caller 文件。
4. **state ownership：**用 `TaskExecutionContextHolder` 取得必需的当前 context；logical key 固定为 `tenantId + userId + deviceId + windowId`，state 内保存 immutable `windowTitle + nativeHandle/HWND + processId` fingerprint。同 logical key fingerprint 改变时用 map 原子替换 state；`A -> B -> A` 不得复活旧 A state。禁止 `default`、`epoch=0`、`getPlayerIdentityEpoch()`、TTL 或旧 state resurrection。
5. **coordinator 移除：**删掉 `TaskTurnCoordinator` field/import/enter/release 和 transaction-name plumbing；typed collaborators按现有同步 source order 直接调用。不得用新 lock/session/owner/lease/ledger/queue/durable workflow 替代。CommonBox 仍先于 follower first aid，deferred recovery clear/call顺序不变。
6. **RefreshDue gate：**保留 public shape、30 秒 team-sharing 和 direct-call语义；production caller必须传当前 exact nonblank windowId作为 fallback，不能再由 `currentWindowId()` 产生 `default`。不要把 gate 改成新的 per-window TTL或另加 cleanup。
7. **caller/host owner：**保留 guard/read-only APIs；当前 Cloud无 guard caller不构成删除理由。FiveRing无 initialize保持不动，Cloud task factory/runner watcher/host activation全部留 TURN-40。
8. **34B并行合同：**冻结第 6 节六个 TaskMaintenance API；任何签名变化先回父级重排，不扩大 34A写集。
9. **typed collaborator identity责任：**当前 TURN-21/23 已有 initial HWND/process/current identity fence；TURN-19 click、TURN-20 panel、TURN-24A radar和 UI local-service path的已审合同强度并不完全相同。父级必须明确：34A只原样消费各依赖已通过的现有合同，还是先另开依赖 repair以统一更强 native fingerprint fence。External C不能在 `AutoCombatService.java` 增加统一 metadata pre-read来掩盖依赖差异，也不能修改依赖文件。
10. **行为不变量：**不新增 probe/read、verification、checkpoint、park/yield、retry、TTL、cleanup、phase transition或 terminal boolean 化；不把 `NONE`、capture unavailable、pause/stop、uncertain或 click failure变成新业务事实。

第 9 点尤其需要父级措辞准确：它不否定这些 dependency 的既有 source pass，也不把它们偷偷判回 failed；它只防止 34A fixed card一边承诺“所有 collaborator 都有同强度 initial fingerprint fence”，一边又不给相应依赖写集。若父级接受现有各卡合同，34A测试应验证 orchestration/state isolation并复用各依赖 named-test责任；若父级要求统一增强，则应先开独立 repair并重新评估 `S=`，不能让 External C越界。

## 8. 可直接 READY 的证据包

在父级采用计划既有 `S=` 语义并把第 7 节写入 fixed card的前提下，不需要第三份 readiness helper或新的源码调查即可直接发 TURN-34A `READY`：

1. 六个 concrete source gates均有 Parent `0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`，当前相关字节与审查 SHA一致；TURN-20共享 OCR 的唯一后续变化也由 TURN-26父级 source pass覆盖。
2. TURN-33 Review #3 true EOF 已出现，且当前 Summon production/test SHA与该结论一致；AutoCombat对 Summon active path零引用。
3. AutoCombat自身仍是既有 preflight核验的 `80380B8D...632D`，没有 post-TURN-33未知漂移。
4. 当前四个 production caller、每个 public method的 direct caller/无 caller状态及返回消费已经重新从当前 Cloud source枚举；旧报告里 Wubei先调 `hasPendingLeader...` 的误记已纠正。
5. 三项 exact write set明确，named test和 fixed card路径当前均不存在，不会覆盖他人半成品。
6. External C lane已上线但未 claim card；其余 active/reserved/planned卡与三项写集路径交集均为空。唯一并行语义耦合是 34B 的六个 TaskMaintenance API，已可由父级一句固定合同消除猜测。
7. 本轮没有发现要求改第二个 production文件、协议、POM/config、Task caller或 DHXY的编译前源码理由。

父级可执行的下一步是：在 `2026-07-16-turn-card-TURN-34A.md` 固定上述三文件写集、source-gate表、public caller表、state/fingerprint规则、34B API冻结和 named-test矩阵，明确 `READY`；External C下一次 heartbeat读取最新字节后，必须先在该固定卡物理 EOF追加真实 `CLAIMED`，之后才可编辑 source/test。该建议不是本 helper 的批准或派发。

## 9. 基线结论

已核对 `docs/业务逻辑.md` 的通用盒子优先级/30秒 pending、召唤兽 live `if8`与静态尾扫、expected战斗快脱战/回程可信纠正、五倍/修罗战斗等待与 stop/pause边界，以及修罗 `696a12b0` fallback表。TURN-34A没有获批业务差异：

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

PRECHECK_COMPLETE true EOF
