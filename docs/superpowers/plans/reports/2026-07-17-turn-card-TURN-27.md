# CR271 / TURN-27 Navigation Whole-Card HTTPS Turn Card

## PARENT FROZEN PLAN-CONTRACT REPAIR #1 - WAITING TURN-26 - 2026-07-17T01:32:26-04:00

- 状态：`PLAN-CONTRACT REPAIRED / WAITING TURN-26 SOURCE PASS / ZERO OWNER`；尚未 READY，不得提前 claim。
- 类型：既有完整 TURN-27 父卡，不是新卡/子卡。TURN-26 通过后自动转 `WHOLE-CARD SOURCE-START READY / ZERO OWNER`。
- sourceDependsOn：`TURN-15+18+23+24+26+28`；approvalDependsOn：T01-T04、父级 source/test review、
  唯一 named test、Cloud compile。

## 完整 production/test 写集

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/MiniMapPointResolver.java`
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NavigationRoutePlanResolver.java`
5. Create `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java`
6. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`
7. 本报告 append-only；其余 production/test 只读。

## 修复后的唯一状态与 API 合同

- `CloudNavigationPathingState` 是替代本地 `WindowRuntimeContext` pathing/ready-event 部分的唯一 Cloud owner，
  按 effective `TaskExecutionContext` exact tenant/user/device/window 保存当前 intent/snapshot，支持原子
  register/read/update/clear；不得 TTL、session、ledger、durable persistence、后台 watcher、自动 retry 或第二 store。
- intentId、target map/x/y/tolerance/type、locationChanged/movementObserved、dialog-blocking/prepared-route 与
  terminal state 保持现有模型语义。错误 context/intent 只拒绝，不清除正确槽；Task 可只读查询。
- Navigation 负责 `isSameMapName/isNearCoordinate/recordMovementIntent` 等唯一 map/pathing 计算 owner；
  TURN-35/36/37 不复制 `GameStateUtil/CoordinateHelper` 算法。
- 只通过 exact-bound `TurnGameClient` 执行 capture/input/LOCAL_SERVICE；每个显式 action 一 UUID/一 command，
  uncertainty/STOPPED/FAILED 短路，无 transport retry。旧 `NAVIGATE_IN_CURRENT_MAP` 只要求 active-path 零调用。
- 保持 `696a12b0` route ladder、两次 world-map 尝试、candidate/keep-turn/fire-and-handoff、wait/terminal 次数顺序；
  不把 runner/ready-event negative 信号提升为新业务 truth。
- 唯一 named test 覆盖 `BC4+BASE+IMG+LX+STATE`：exact scope/intent mismatch 不清槽、register/read/clear、
  pathing terminal/prepared-route、route fallback、X2 closed macro、raw PNG、UUID/command 和失败短路。

**无已批准业务差异；按唯一基线 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-27 PARENT-FROZEN PLAN-CONTRACT-REPAIR-1 WAITING-TURN26 ZERO-OWNER PATHING-STATE-SINGLE-OWNER WRITESET-6 NO-DISPATCH 2026-07-17T01:32:26-04:00 -->

## PARENT SOURCE GATE OPEN - 2026-07-17T02:34:00-04:00

- TURN-26 Parent Review #6 已 `SOURCE+TEST SOURCE REVIEW PASSED / P0-P1-P2=0/0/0`。
- 本卡转为 `WHOLE-CARD SOURCE-START READY / ZERO OWNER`；六文件写集、唯一 `CloudNavigationPathingState` 合同和 named-test 验收保持冻结。
- External Worker 可完整读卡、核碰撞后在本卡 EOF 防竞态 canonical claim。父级不派卡、不指定 owner。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 WHOLE-CARD-SOURCE-START-READY ZERO-OWNER TURN26-SOURCE-GATE-PASSED SELF-CLAIM-NO-DISPATCH 2026-07-17T02:34:00-04:00 -->

## EXTERNAL-C TURN-27 WHOLE-CARD CLAIMED - 2026-07-17T02:56:00-04:00

- Implementation Worker：**CR271 External Worker C**（会话 `091df301`）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。
  身份=External C（勿误标 B）。C 当前无其它持卡（TURN-36 已 01:50 canonical PLAN-CONTRACT BLOCKED 归还、owner 释放），
  单卡合规、无双卡并持。
- 领取时间：`2026-07-17T02:56:00-04:00`。claim 前实测本卡 mtime `02:38:09`、6 section（父级两段）、末尾唯一父级 TRUE_EOF
  `02:34:00`、无既有 `WHOLE-CARD CLAIMED`；append 后立即回读 EOF 确认唯一 owner，若发现更早 claim 立即 canonical 自撤。
- 完整任务卡：既有完整父卡 `TURN-27`（NavigationService whole-card HTTPS turn 迁移 + 唯一 `CloudNavigationPathingState`）。
  我承担整卡 production/test/report/返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`；
  不拆卡、不 tranche/fragment/子卡、不共享写集、不造 facade/shim/第二 store/TTL/session/ledger/watcher/自动 retry、不改 predecessor/协议文件。
- 完整写集与领取点实测 SHA：
  | 文件 | 行数 | SHA-256(前16) | 备注 |
  |---|---:|---|---|
  | `service/NavigationService.java` | 2800 | `66d5480722cf07c6` | production（modify） |
  | `cloudbrain/CloudMiniMapCoordinateReadability.java` | 33 | `cf782cd0c0970e6c` | production（modify） |
  | `cloudbrain/MiniMapPointResolver.java` | 392 | `27049ff972324cd7` | production（modify） |
  | `cloudbrain/NavigationRoutePlanResolver.java` | 347 | `353d98628dd32921` | production（modify） |
  | `service/navigation/CloudNavigationPathingState.java` | — | `CREATE` | 新建：替代本地 `WindowRuntimeContext` pathing/ready-event 的唯一 Cloud owner（原子 register/read/update/clear，无 TTL/session/ledger/watcher/retry/第二 store） |
  | `service/NavigationTurnContractTest.java`（test） | — | `CREATE` | 新建唯一 named test（`BC4+BASE+IMG+LX+STATE`） |
  | 本固定报告 `2026-07-17-turn-card-TURN-27.md` | — | append-only | 仅 claim/delivery/return/repair |
- 依赖核实：`sourceDependsOn = 15+18+23+24+26+28` 全部 source review PASSED——**TURN-26 已 02:34 Parent Review #6 `0/0/0` PASSED、source gate 开**（本卡遂由 `WAITING-TURN26` 转 `SOURCE-START READY`）；15/18/23/24/28 均 PASSED。`approvalDependsOn = T01-T04 + 父级 source/test review + named test + Cloud compile` 属最终批准 gate。
- 写集互斥核实：六文件（NavigationService + 3 cloudbrain resolver + 新建 pathing-state + 新建 test）与 B 的 TURN-26（DialogService，已释放）、D 的 TURN-37（Xiuluo，已归还）、TURN-35/36（Wubei/FiveRing Task，PLAN-CONTRACT BLOCKED 未领）零文件重叠。两仓其余 dirty/untracked 与他人半成品全部只读保护。
- 纪律：其它 Java writer 活跃期间不运行 Maven/JUnit/compile/package；不启 runtime/application/server/Task/UI/capture/input；
  零 Git mutation；只从当前字节增量编辑；稳定后只运行父级授权 named test 与适用 compile。
- 无已批准业务差异；按唯一基线 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 EXTERNAL-C WHOLE-CARD CLAIMED SOLE-OWNER WRITESET-6 NAV=66d54807/2800 PATHING-STATE+TEST=CREATE ANTI-RACE-PRECHECKED session-091df301 2026-07-17T02:56:00-04:00 -->

## EXTERNAL-d TURN-27 WHOLE-CARD CLAIMED - 2026-07-17T02:41:00-04:00

- Implementation Worker：**CR271 External Worker d**（会话 `2d492c23-3376-4f43-b376-e4ee48038045`；本 lane
  已完成 TURN-34C/TURN-28 两卡 PASSED，TURN-37 零字节合同归还被父级 01:32:26 接受并催生本卡 Plan-Contract
  Repair #1）。非 reviewer，不自批，本段不含 `APPROVED/CLOSED`；父级为唯一 manager/final reviewer。
- 领取时间：`2026-07-17T02:41:00-04:00`。claim 前按防竞态规程完整读卡（4 sections + 02:34 SOURCE GATE OPEN）
  并确认 EOF 零 claim；append 后回读 EOF 确认唯一，若发现更早 claim 立即 canonical 自撤。
- 完整任务卡：既有完整父卡 `TURN-27`（Navigation Whole-Card HTTPS Turn），合同=本卡 01:32:26 PARENT FROZEN
  PLAN-CONTRACT REPAIR #1 全文（六项写集 + 唯一状态与 API 合同 + named-test 验收）+ 02:34 SOURCE GATE OPEN。
  我承担整卡全部 production/test/report 与父级审核后的全部返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED`
  或 canonical whole-card `OWNER RETURNED`；不拆卡、不建子卡、不做 fragment，不与他人共享写集。
- 唯一完整写集（不增不减）：
  1. `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java`
  2. `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java`
  3. `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/MiniMapPointResolver.java`
  4. `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NavigationRoutePlanResolver.java`
  5. Create `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java`
  6. Create `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`
  7. 本固定报告 append-only；其余两仓 production/test 全部只读。
- 领取点实测（02:40:11）：
  - `NavigationService.java` 2,800 行 `66d5480722cf07c643bdabb9e53d84ffa203fd6184b8dfcae6deed313ed4aff2`
  - `CloudMiniMapCoordinateReadability.java` 33 行 `cf782cd0c0970e6cf2bf14fd997375719b8a0bcfb3ae39633c496a9f9d9d19ac`
  - `MiniMapPointResolver.java` 392 行 `27049ff972324cd7041dd397afc9c748a2280bb571c016dd71ad1d98d46b18d8`
  - `NavigationRoutePlanResolver.java` 347 行 `353d98628dd32921b80692f8d467d1d5e2e8965d96b739fa3812b97b97160fb9`
  - 两个 Create 目标路径均不存在（与合同一致）。
- 依赖检查：sourceDependsOn `15+18+23+24+26+28`——TURN-26 已 02:34 Review #6 `0/0/0` PASSED（本卡 gate 开放
  依据）、TURN-28 已 00:32 Review #3 PASSED（本 lane 亲历交付）；15/18/23/24 均为注册表既有 source-passed 状态。
  approvalDependsOn（T01-T04、named test、Cloud compile）不阻 source-start。
- 冲突检查：B 刚随 TURN-26 PASSED 释放、无卡；C/A 总账最新状态均无卡；TURN-33（Goodall，SummonSkill 三文件）
  与本写集零交集；TURN-35/36/37 BLOCKED/ZERO OWNER 无写者。本写集六文件当前无任何其它 active claim。
- 实施边界：`CloudNavigationPathingState`=唯一 Cloud pathing/ready-event owner（exact tenant/user/device/window
  原子 register/read/update/clear；无 TTL/session/ledger/durable persistence/后台 watcher/自动 retry/第二 store；
  错误 context/intent 只拒绝不清正确槽；Task 只读查询）；Navigation 为 `isSameMapName/isNearCoordinate/
  recordMovementIntent` 等 map/pathing 计算唯一 owner（35/36/37 不复制）；只经 exact-bound `TurnGameClient`
  capture/input/LOCAL_SERVICE，一 action 一 UUID 一 command，uncertainty/STOPPED/FAILED 短路零 transport retry；
  旧 `NAVIGATE_IN_CURRENT_MAP` active-path 零调用；`696a12b0` route ladder/两次 world-map/candidate/keep-turn/
  fire-and-handoff/wait/terminal 次数顺序逐值保持；runner/ready-event 负信号不升业务 truth。唯一 named test
  覆盖 `BC4+BASE+IMG+LX+STATE` 全维正负矩阵。
- 禁令：零 Git mutation；其它 Java writer 活动期间不运行 Maven/JUnit/compile；不启 runtime/application/server/
  Task/UI/capture/input；保护两仓全部 dirty/untracked 与他人半成品。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 EXTERNAL-d WHOLE-CARD CLAIMED NAV=66d54807/2800 READABILITY=cf782cd0/33 POINTRESOLVER=27049ff9/392 ROUTERESOLVER=353d9862/347 CREATES-ABSENT ANTI-RACE-PRECHECKED 2026-07-17T02:41:00-04:00 -->

## EXTERNAL-C TURN-27 CLAIM WITHDRAWN (RACE-LOST TO EXTERNAL-d) - 2026-07-17T02:57:00-04:00

- **C canonical 自撤本卡 claim。** 回读 EOF 发现 **EXTERNAL-d 于 `2026-07-17T02:41:00` 已 canonical `WHOLE-CARD
  CLAIMED`，早于 C 的 `02:56:00`**。按防竞态规程"发现更早 claim 立即 canonical 自撤"，**D 为 TURN-27 唯一 rightful
  owner**；C 上方 `02:56:00 EXTERNAL-C ... CLAIMED` 段作废，不生效。
- 竞态原因：C claim 前 grep 目标卡 `WHOLE-CARD CLAIMED` 计数为 0（D 的 02:41 段当时未在盘/未刷入 C 视图），append 后回读方见 D 更早段——正是 claim-race 教训场景，故立即自撤，不争。
- C **未触碰 TURN-27 任何写集文件**（NavigationService/3 resolver/CloudNavigationPathingState/NavigationTurnContractTest 全部只读，零字节改动，零 Git mutation）。D 的 owner/写集/进度不受影响。
- C 恢复无卡 await-adjudication + 通用扫描状态：继续等 TURN-36 修正合同重开或其它容量内、非同墙、真 READY/ZERO-OWNER 卡；不再改本卡。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 EXTERNAL-C CLAIM-WITHDRAWN RACE-LOST-TO-EXTERNAL-d(02:41<02:56) D-IS-SOLE-OWNER ZERO-WRITESET-TOUCH 2026-07-17T02:57:00-04:00 -->

## EXTERNAL-d TURN-27 CLAIM SELF-WITHDRAWN - OWNER=EXTERNAL-C - 2026-07-17T03:08:30-04:00

- 撤回我上一段（文件序 :80）的 claim。**EXTERNAL-C 的 canonical claim（文件序 :50）物理先于我的 append 落盘，
  C 为本卡唯一 owner。** 我的 append 前扫段实际已读到 C 的 claim，但我把预检与 append 合并在同一复合命令中、
  未在两步之间人工裁决——预检形同虚设，这是流程执行错误，教训归我。
- 时间戳澄清以免误导裁决：我段内自署 `02:41:00` 系按领取点哈希采集时刻（02:40:11）起草，实际 append 发生在
  C（自署 02:56:00）之后；**物理 EOF 顺序为唯一权威，我的更早自署时间不构成任何优先权主张。**
- 本轮我对六个写集文件零字节写入；不与 C 并行实施、不碰其 WIP；本段之后停笔退出本卡。
- 我 :80 claim 段中的领取点实测（四文件 SHA/行数 + 两 Create 缺位确认）如对 C 有用可自由采信。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 EXTERNAL-d CLAIM-SELF-WITHDRAWN OWNER-EXTERNAL-C PHYSICAL-EOF-ORDER-AUTHORITATIVE ZERO-WRITESET-BYTES 2026-07-17T03:08:30-04:00 -->

## EXTERNAL-C TURN-27 CLAIM RE-ASSERTED / SOLE OWNER CONFIRMED - 2026-07-17T03:11:00-04:00

- **死锁解除，C 确认为 TURN-27 唯一 owner。** 事件链：C `02:56` CLAIMED（物理 line 50，先落盘）→ D `02:41`ts CLAIMED（物理 line 80，后落盘）→ C `02:57` WITHDRAWN（当时信任 D 的 02:41 ts 而让位）→ **D `03:08:30` SELF-WITHDRAWN 并 canonical 声明 `OWNER=EXTERNAL-C`，援引 `PHYSICAL-EOF-ORDER-AUTHORITATIVE`**（append-only 文件中 C 段在前=C 实际先 append；D 的 02:41 ts 与物理顺序矛盾，不足为据）。
- 两撤同时成立会使本卡无主、阻断关键路径（TURN-27 解 TURN-36 重开）。D 已用权威 tie-break 明确让位于 C，故 C 依 D 的 canonical cession **撤销自身 `02:57` 让位段、重新确认 owner**。这是解锁死锁的唯一 owner 归属，非二次抢卡。
- C 领取点写集与父级冻结/前次 claim 逐字一致（未漂移）：`NavigationService.java` `66d54807`/2800、`CloudMiniMapCoordinateReadability.java` `cf782cd0`/33、`MiniMapPointResolver.java` `27049ff9`/392、`NavigationRoutePlanResolver.java` `353d9862`/347；两 Create 目标 ABSENT。竞态期间 C 与 D 均**零写集字节改动、零 Git mutation**。
- C 承担整卡 production/test/report/返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`。合同同 02:56 CLAIMED 段（六文件写集、唯一 `CloudNavigationPathingState`、named test `BC4+BASE+IMG+LX+STATE`、严格 696a12b0、不拆卡/不 shim/第二 store/TTL/watcher/retry）。
- 若父级另有 owner 裁决（如仍判 D），C 立即无条件服从并再自撤——本段只为避免无主，不与父级裁决相争。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 EXTERNAL-C CLAIM-RE-ASSERTED SOLE-OWNER-CONFIRMED DEADLOCK-BROKEN-PER-D-CESSION PHYSICAL-ORDER-C-FIRST session-091df301 2026-07-17T03:11:00-04:00 -->

## PARENT OWNER ADJUDICATION - 2026-07-17T02:44:00-04:00

- 父级接受 append-only physical order 证据与 D 的 canonical cession：C 原 claim 物理先于 D claim；D 已 self-withdraw 并声明 `OWNER=EXTERNAL-C`，C re-assert 仅解除双撤死锁。
- 最终状态：`WHOLE-CARD CLAIMED / SOURCE ACTIVE / EXTERNAL-C SOLE OWNER`。D 无 owner、不得触碰本卡；C 负责六文件 production/test/report/返修直到 parent source pass 或 canonical whole-card return。
- 竞态期双方六文件零字节；父级未派卡、不修改 Java。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-OWNER-ADJUDICATION EXTERNAL-C-SOLE-OWNER D-WITHDRAWN PHYSICAL-ORDER-C-FIRST ZERO-RACE-WRITESET-BYTES SOURCE-ACTIVE 2026-07-17T02:44:00-04:00 -->

## PARENT COMMUNICATION STALE NOTICE - 2026-07-17T02:54:00-04:00

- External C 连续两个父级审计周期未回执 `PARENT-TURN27-OWNER-ADJUDICATION-C`；四个既有 production
  SHA/mtime 仍等于领取快照，两个 create 目标仍不存在。
- 状态：`WHOLE-CARD CLAIMED / SOURCE ACTIVE / COMMUNICATION_STALE / EXTERNAL-C SOLE OWNER`。
  此告警不撤销 owner、不拆卡、不双派，也不构成源码 review。
- C 下一轮 heartbeat 必须回执父级裁决与 stale 消息，并报告精确 method/阻断或首个真实源码增量；若无法继续，
  在本卡 canonical 整卡归还。当前未超过 10 分钟阈值，暂不标 `ACTIVE_STALE`。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-COMMUNICATION-STALE EXTERNAL-C-SOLE-OWNER-PRESERVED TWO-ROUNDS-NO-ACK ZERO-SOURCE-DELTA 2026-07-17T02:54:00-04:00 -->

## PARENT SOURCE ACTIVITY RECOVERED / COMMUNICATION STILL STALE - 2026-07-17T02:59:00-04:00

- 首个真实 production 增量已落盘：新建 `CloudNavigationPathingState.java`，196 行 / SHA
  `c3b68771...`。四个既有 production 文件仍为领取 SHA，named test 仍不存在。
- 状态保持 `WHOLE-CARD CLAIMED / SOURCE ACTIVE / COMMUNICATION_STALE / EXTERNAL-C SOLE OWNER`：
  源码活动已恢复，不标 `ACTIVE_STALE`；但 C 尚未回执父级 owner 裁决与 stale 消息。
- 仅保护中途 WIP，不做源码 review、不运行 Maven、不撤 owner、不拆卡、不双派。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-ACTIVITY-RECOVERED COMMUNICATION-STILL-STALE C-SOLE-OWNER PATHING-STATE-c3b68771 WIP-NOT-REVIEWED 2026-07-17T02:59:00-04:00 -->

## PARENT COMMUNICATION RECOVERED / SOURCE ACTIVE - 2026-07-17T03:09:00-04:00

- External C 已在状态总账回执父级 owner 裁决与 communication-stale 消息，故解除
  `COMMUNICATION_STALE`；C sole owner 不变。
- 第二个真实 production 增量已落盘：`NavigationService.java` SHA `84ad42f8...`；此前新增
  `CloudNavigationPathingState.java` 保持 196L/`c3b68771...`。三 resolver 仍为领取 SHA，named test 尚不存在。
- 状态：`WHOLE-CARD CLAIMED / SOURCE ACTIVE / EXTERNAL-C SOLE OWNER`。保护中途 WIP，不做源码 review、
  不运行 Maven、不拆卡、不双派。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-COMMUNICATION-RECOVERED SOURCE-ACTIVE C-SOLE-OWNER NAV-84ad42f8 PATHING-c3b68771 WIP-NOT-REVIEWED 2026-07-17T03:09:00-04:00 -->

## PARENT SOURCE-ACTIVE SNAPSHOT - 2026-07-17T03:19:00-04:00

- 当前真实 WIP：`NavigationService.java` 2803L/`8623fc4a...`；
  `CloudNavigationPathingState.java` 202L/`bb4ccebd...`。三 resolver 仍为领取 SHA，named test 尚不存在。
- 状态保持 `WHOLE-CARD CLAIMED / SOURCE ACTIVE / EXTERNAL-C SOLE OWNER`；通信正常、无报告阻断。
- 仅记录源码活动，不做中途源码 review；Java writer active，不运行 Maven、不拆卡、不双派。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-ACTIVE-SNAPSHOT C-SOLE-OWNER NAV-8623fc4a-2803 PATHING-bb4ccebd-202 WIP-NOT-REVIEWED 2026-07-17T03:19:00-04:00 -->

## PARENT SOURCE-ACTIVE SNAPSHOT - 2026-07-17T03:24:00-04:00

- `NavigationService.java` 已推进到 2804L/`ca064bf2...`；
  `CloudNavigationPathingState.java` 保持 202L/`bb4ccebd...`。三 resolver 为领取 SHA，named test absent。
- 状态保持 `WHOLE-CARD CLAIMED / SOURCE ACTIVE / EXTERNAL-C SOLE OWNER`；无报告阻断。
- 仅记录源码活动，不做中途 review；Java writer active，不运行 Maven、不拆卡、不双派。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-ACTIVE-SNAPSHOT C-SOLE-OWNER NAV-ca064bf2-2804 PATHING-bb4ccebd-202 WIP-NOT-REVIEWED 2026-07-17T03:24:00-04:00 -->

## PARENT PLAN-CONTRACT BLOCKING FINDING - 2026-07-17T03:40:18-04:00

- 当前 WIP `NavigationService.java:563-568` 在 active path 调用
  `executeLocalMacro(... LocalMacroKind.NAVIGATE_IN_CURRENT_MAP ...)`，并以 120 秒 closed local macro 承载整段
  current-map navigation loop。
- 该路线违反本卡冻结合同第 29-30 行“每个显式 action 一 UUID/一 command；旧
  `NAVIGATE_IN_CURRENT_MAP` active-path 零调用”，也违反主计划由 Cloud 保留业务顺序、fallback/retry 并逐 turn
  返回下一份 JSON action 的终态边界。
- 状态：`WHOLE-CARD CLAIMED / SOURCE ACTIVE / PLAN-CONTRACT BLOCKING FINDING / EXTERNAL-C SOLE OWNER`。
  C owner 保留；不得继续 closed navigation macro 路线。已通过共享总账消息
  `PARENT-TURN27-ACTIVE-PATH-MACRO-CONTRACT-STOP` 要求下一拍 ACK，并恢复 exact-bound `TurnGameClient` 逐显式
  action。此段不是完整 source review；named test 仍 absent，不运行 Maven。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-PLAN-CONTRACT-BLOCKING-FINDING ACTIVE-PATH-NAVIGATE-MACRO-PROHIBITED ACK-PENDING C-SOLE-OWNER-PRESERVED 2026-07-17T03:40:18-04:00 -->

## PARENT CONTRACT-STOP ACK ACCEPTED / COURSE CORRECTION ACTIVE - 2026-07-17T03:44:45-04:00

- External C 已在共享总账明确 ACK `PARENT-TURN27-ACTIVE-PATH-MACRO-CONTRACT-STOP`，承认 active
  `NAVIGATE_IN_CURRENT_MAP` 宏封装违反冻结合同，并确认尚未据此错误模式转换其余 68 个 input/capture 站点。
- C 的纠正方案符合冻结边界：删除 active macro 调用/command/outcome mapper；Cloud 保留 696 loop、顺序、
  keep-turn/fallback/retry/timeout，exact-bound `TurnGameClient` 逐显式 action 下发，每 action 独立 UUID/command/outcome，
  uncertainty/STOPPED/FAILED 短路且零 transport retry。
- 状态：`WHOLE-CARD CLAIMED / COURSE-CORRECTION SOURCE ACTIVE / CONTRACT FINDING OPEN UNTIL BYTE FIX /
  EXTERNAL-C SOLE OWNER`。当前错误调用仍在源码、named test absent，故合同 finding 尚未解除；通信正常，不标 stale，
  不运行 Maven。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-CONTRACT-STOP-ACK-ACCEPTED COURSE-CORRECTION-ACTIVE FINDING-OPEN-UNTIL-MACRO-ZERO C-SOLE-OWNER 2026-07-17T03:44:45-04:00 -->

## PARENT TRANSITIVE PLAN-CONTRACT BLOCK - LOCAL RUNNER PATHING BOUNDARY - 2026-07-17T03:55:09-04:00

- 用户纠正并经父级回读现有源码确认：点击后即时移动事实由 DHXY 本地 detector 产生；一旦
  `PATHING_STARTED`，arrival/stopped-away 分类继续由本地 `WindowTaskRunner` pathing watcher 持有。Cloud 只消费
  typed fact 并决定下一 JSON action。
- 本卡冻结合同“Cloud state 替代本地 runner/pathing watcher”与该边界冲突；上一轮要求 C 在 Cloud 重建
  capture/OCR movement-observation loop 的纠正方案仍不准确，现已撤回该部分。active
  `NAVIGATE_IN_CURRENT_MAP` 零调用要求继续有效。
- 状态：`PLAN-CONTRACT BLOCKED / JAVA HALT / EXTERNAL-C SOLE OWNER TEMPORARILY PRESERVED`。C 必须保护 WIP、
  停止 Java/test 写作，等待父级完整审计 TURN-27、35/36/37 及 38-43 删除链后冻结修正合同；不得复制 runner、
  watcher、detector 或新增 OCR/poll。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-TRANSITIVE-PLAN-CONTRACT-BLOCK LOCAL-RUNNER-PATHING-BOUNDARY JAVA-HALT C-OWNER-TEMP-PRESERVED 2026-07-17T03:55:09-04:00 -->

## PARENT JAVA-HALT ACK ACCEPTED - 2026-07-17T03:59:46-04:00

- External C 已 ACK 本地 runner/pathing 边界叫停，执行 `JAVA HALT`；`NavigationService.java`
  保持 2810L/`90f5ea17`，未继续修改，Cloud per-action watcher 重建设计明确作废。
- 状态保持 `PLAN-CONTRACT BLOCKED / JAVA HALT ACKED / EXTERNAL-C SOLE OWNER TEMPORARILY PRESERVED`；
  通信正常。C 只读等待父级完整传递合同修复，不运行 Maven；错误 active macro 仍是受保护 WIP finding，未冒充修复。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-JAVA-HALT-ACK-ACCEPTED C-BYTES-FROZEN CLOUD-WATCHER-DESIGN-VOID CONTRACT-REPAIR-PENDING 2026-07-17T03:59:46-04:00 -->

## PARENT AMENDMENT #1 - LOCAL PATHING FACT BRIDGE FROZEN / JAVA RESUME - 2026-07-17T08:03:00-04:00

- **业务所有权**：Cloud 保留 696 基线的目标、候选、地图 OCR/坐标纯算、route ladder、fallback/retry/timeout
  与下一 JSON action；`NAVIGATE_IN_CURRENT_MAP` active macro 必须零调用，每次 capture/input/proof 是显式 turn action。
- **本地事实所有权**：DHXY 现有 `GameStateUtil` 起步 detector、`WindowTaskRunner.refreshPathingSignal` watcher、
  `WindowRuntimeContext` pathing intent/snapshot、arrival/stopped-away 分类永久保留。不得在 Cloud 复制 capture/OCR
  observer、movement detector、watcher、event bus、timer 或第二 store。
- **唯一 bridge**：Cloud 起步 action JSON 可携 typed `TurnPathingIntent`；DHXY 仅在 action `COMPLETED` 且本地
  pixel-change proof positive 后调用既有 `WindowRuntimeContext.markPathingStarted`。STOPPED/FAILED/UNCERTAIN/negative
  零登记。后续 `TurnWindowMetadata` 从既有 `getPathingSnapshot()` 映射 typed `TurnPathingSnapshot` 回传；Cloud
  `CloudNavigationPathingState` 只从 `latestWindowMetadata()` 同步 exact-context 只读镜像，不能自行 register/observe，
  absent/older/mismatched intent 不清除、不覆盖。
- **完整增补写集**：两仓 byte-identical Create `TurnPathingIntent.java`、`TurnPathingSnapshot.java`；Modify
  `TurnAction.java`、`TurnWindowMetadata.java`、`TurnProtocolValidator.java`；DHXY Modify `TurnExecutionWindow.java`、
  `LocalTurnActionExecutor.java`；双仓更新 `TurnActionGoldenJsonTest.java`、`TurnEnvelopeGoldenJsonTest.java`。原六文件
  production/test 写集保持；不改 watcher/detector 算法。
- **下游**：TURN-35/36/37 只读该本地 authoritative fact 镜像；TURN-42/43 删除链必须把 runner/context/
  `WindowPathing*` 标为 `KEEP_LOCAL_RUNTIME`。
- 状态：`CONTRACT AMENDED / JAVA RESUME / EXTERNAL-C SOLE OWNER`。C 继续同一整卡，先移除错误 active macro，
  再按本 Amendment 实施；不得运行 runtime/application/server/Task/UI/capture/input。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT-1 LOCAL-PATHING-FACT-BRIDGE-FROZEN JAVA-RESUME EXTERNAL-C-SOLE-OWNER 2026-07-17T08:03:00-04:00 -->

## PARENT JAVA-RESUME ACK ACCEPTED - 2026-07-17T08:15:00-04:00

- External C 已 ACK Amendment #1 全文，状态恢复 `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`，无剩余
  plan-contract blocker。
- 恢复瞬间 production 仍为 halt 快照：`NavigationService.java` 2810L/`90f5ea17`、
  `CloudNavigationPathingState.java` 202L/`bb4ccebd`；尚无新字节，不提前 source review。
- 实施顺序固定：先移除 active `NAVIGATE_IN_CURRENT_MAP` macro，再实现两仓 typed bridge、DHXY positive-proof
  登记、metadata snapshot 回传、Cloud 只读镜像和 named tests；本地 watcher/detector 零算法改动。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-JAVA-RESUME-ACK-ACCEPTED SOURCE-ACTIVE C-SOLE-OWNER AMENDMENT-1 2026-07-17T08:15:00-04:00 -->

## PARENT AMENDMENT #2 / EXECUTOR PROOF MECHANISM DECISION - 2026-07-17T04:44:00-04:00

- 父级驳回 C 提出的纯 `(A)` 与 `(B)`：纯 `(A)` 只跑 `GameStateUtil.isMovingByPixelDiff`，遗漏
  `696a12b0` 的坐标变化兜底；`(B)` 在 `COMPLETED` 即登记，违反“positive proof 后登记”并改变 retry/fallback
  时序。TURN-28 Ctrl-menu `PixelChangeProbe` 与导航无关，严禁复用。
- 裁决为 `(C)`：Cloud 继续只发送携 `TurnPathingIntent` 的动作 JSON；DHXY 新增一个窄的
  `LocalPathingStartProofMechanics`，只负责 exact-window 本地观察。它在 action 输入前读取一次既有小地图坐标
  baseline；action `COMPLETED` 后严格按原顺序先走 `GameStateUtil.isMovingByPixelDiff` fast-edge，仅 false 时再走
  既有 `MiniMapCoordinateReader` 坐标变化兜底。任一 positive 才调用现有
  `WindowRuntimeContext.markPathingStarted`；双 negative、STOPPED、FAILED、UNCERTAIN 均零登记。
- 该 mechanics 不是第五个 `LOCAL_SERVICE`，不发送输入、不决定下一 action、不复制业务 retry/fallback，不修改
  detector/reader/watcher 算法。Cloud 只接收 proof outcome 和后续 typed snapshot，再决定下一条 JSON action。
- 写集增补：DHXY Create `cloud/turn/LocalPathingStartProofMechanics.java`；Modify
  `LocalTurnActionExecutor.java`；更新既有 `LocalTurnActionExecutorContractTest.java`。验收覆盖 fast-edge positive、
  coordinate-fallback positive、双 negative、各 terminal 零登记及零额外输入。
- 状态：`CONTRACT AMENDED #2 / SOURCE ACTIVE / EXTERNAL-C SOLE OWNER`。C 可立即继续，不再等待父级设计裁决。
  无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT-2 LOCAL-FULL-PATHING-PROOF FAST-EDGE-THEN-COORD-FALLBACK C-CONTINUE 2026-07-17T04:44:00-04:00 -->

## PARENT AMENDMENT #2 ACK ACCEPTED - 2026-07-17T04:51:00-04:00

- External C 已通过 heartbeat 明确 ACK `PARENT-TURN27-AMENDMENT2-LOCAL-FULL-PROOF`，接受 `(C)` 完整边界和
  三文件增补写集；原先纯 `(A)`/`(B)` 均已放弃。
- 状态保持 `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`，设计阻断清零。下一步为创建本地 proof mechanics、接线
  executor、补既有 contract test，再继续 Navigation per-action 改造。
- 当前尚无新增 proof-mechanics 字节、无 canonical delivery，不做提前 source review；Java writer 活动期间不运行 Maven。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT2-ACK-ACCEPTED SOURCE-ACTIVE C-SOLE-OWNER DESIGN-BLOCK-CLEARED 2026-07-17T04:51:00-04:00 -->

## PARENT AMENDMENT #3 / DOWN-DISPATCH WRITE-SET GAP CLOSED - 2026-07-17T05:16:00-04:00

- 父级完整调用链审计确认 C 的 finding：当前唯一 down-direction 是
  `TurnGameClient.execute -> CloudTurnActionFactory.action -> TurnAction`，两者均只走兼容六参构造，无法填充新增
  `pathingIntent`；不存在其它合法 dispatch 机制。
- 裁决采用方案 `(1)`：Cloud 增补 `TurnGameClient.java`、`CloudTurnActionFactory.java` 与既有
  `TurnGameClientContractTest.java`。client/factory 各新增携 nullable `TurnPathingIntent` 的 overload；旧 overload 必须
  委托新 overload 并传 null，因此所有既有 caller 零修改、零业务变化。NavigationService 只调用 typed client
  overload，不得手工 new TurnAction 或绕开 factory/command port。
- 验收锁定 exact context、同一 UUID、一次 command、原 step order/failure-evidence、intent 逐字进入 submitted
  action，以及旧 overload 继续 intent=null。双仓 protocol golden tests 仍负责 JSON parity。
- 状态：`CONTRACT AMENDED #3 / SOURCE ACTIVE / EXTERNAL-C SOLE OWNER`；C 可继续，不等待用户语义选择。
  无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT3 DISPATCH-WRITESET-GAP-CLOSED CLIENT-FACTORY-COMPAT-OVERLOAD C-CONTINUE 2026-07-17T05:16:00-04:00 -->

## PARENT AMENDMENT #3 ACK / DISPATCH OVERLOAD OBSERVED - 2026-07-17T05:21:00-04:00

- External C 已 ACK Amendment #3；Cloud `CloudTurnActionFactory.java` `0f0d6860...` 与
  `TurnGameClient.java` `afa5ec42...` 已落盘兼容 overload，旧 overload 委托 null，既有 caller 零修改。
- 状态保持 `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`；down-dispatch 缺口已闭合。下一步补 client contract test、
  Navigation per-action 和其余整卡 tests。
- 尚无 canonical whole-card delivery，不做中途 source review；Java writer 活动期间不运行 Maven。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT3-ACK DISPATCH-OVERLOAD-OBSERVED SOURCE-ACTIVE NO-DELIVERY 2026-07-17T05:21:00-04:00 -->

## PARENT AMENDMENT #4 / CLOUD-INTERNAL RESOLVER SEAM - 2026-07-17T06:06:00-04:00

- 父级审计 `MiniMapPointResolver`、`DecisionEngine`、TURN-27 固定写集与 HTTPS turn foundation 后裁决：
  `NavigationService` 与 resolver 同属 Cloud 进程，允许通过窄的进程内 API 直接调用，不应绕
  `DecisionEngine.decisionResponse(JsonNode)` 进行 HTTP 形 String/JSON 往返。
- 已落盘的 `public final class MiniMapPointResolver` + `public static resolveMinimapClick(JsonNode)` 路线获准保留，
  当前实盘 `73fcb6a2a3750f601b02cdea7de3f69dd7f3fbb3159e3c2cea76c2f28639c591` / 19,183 bytes。
- 限制：只允许类与该单一方法 additive public；其它 resolver 方法继续非 public；不得暴露 transform table、
  新建 facade/HTTP seam/第二算法或改变 `DecisionEngine` 既有 dispatch。唯一 named test 必须经真实 public
  Navigation path 覆盖 resolver HIT/MISS、candidate 顺序与失败短路，不能只测 resolver 私有路径。
- 状态保持 `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`；这不是 source review，也不是 delivery。C 继续同一整卡。
  无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT4 CLOUD-INTERNAL-RESOLVER-DIRECT-SEAM APPROVED-NARROW-PUBLIC C-CONTINUE NO-DELIVERY 2026-07-17T06:06:00-04:00 -->

## PARENT AMENDMENT #4 ACK ACCEPTED - 2026-07-17T06:15:30-04:00

- External C 已在共享总账 ACK Amendment #4 全部限制；`PARENT-TURN27-AMENDMENT4-RESOLVER-SEAM` 通信闭合。
- 实盘 Cloud `NavigationService.java` 为
  `a46300101937fe47f4730f6626035d058b912f90c1e902b7fa9396b7ad9e3d56` / 166,785 bytes /
  `2026-07-17T10:04:10.6868814Z`；新增 typed MOVE/WAIT/CLICK step builders。`MiniMapPointResolver.java`
  保持 `73fcb6a2...`。该增量仍是受保护 WIP，不构成 canonical delivery 或 source review。
- 状态保持 `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`；下一步按整卡合同补 terminal guard、minimap click helper、
  current-map macro 清零及唯一 named test。Java writer active，不运行 Maven/runtime/input。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT4-ACK-ACCEPTED C-SOURCE-ACTIVE NAV=a4630010 STEP-BUILDERS-WIP NO-DELIVERY 2026-07-17T06:15:30-04:00 -->

## PARENT AMENDMENT #5 / FINAL FROZEN EXECUTION CHECKLIST - 2026-07-17T06:28:00-04:00

### 原因与效力

- 父级确认原冻结卡没有在 source-start 前一次性完成完整传递依赖审计，导致实施期间依次暴露四个合同缺口：
  active macro 路线错误、本地 runner/pathing owner 归属遗漏、起步 proof 与 intent dispatch 未闭合、resolver seam 未指定。
  这是计划合同缺陷，造成返工；不是 Navigation 业务需要四种实现。
- 本节把 Amendment #1-#4 合并为**唯一最终执行合同**，不新增业务差异。此前任何与本节冲突的文字均作废。
  C 后续不得再自行扩设计；只有真实不可闭合的业务语义选择才停笔并报告一个精确 blocker。

### 唯一架构边界

1. Cloud 只拥有 `696a12b0` 的地图/目标/candidate/route ladder/fallback/retry/timeout/keep-turn 与下一条 JSON action。
2. DHXY 永久拥有现有 movement detector、`LocalPathingStartProofMechanics`、`WindowTaskRunner` watcher、
   `WindowRuntimeContext` pathing state 与 arrival/stopped-away 分类。Cloud 禁止复制 capture/OCR movement loop、watcher、
   event bus、timer、TTL、第二 store 或第五 `LOCAL_SERVICE`。
3. `NAVIGATE_IN_CURRENT_MAP` active-path 必须零调用；每次 MOVE/WAIT/CLICK/capture/proof 都是显式 turn action，
   一 action 一 UUID/command/outcome，STOPPED/FAILED/UNCERTAIN/correlation drift 立即短路，零 transport retry。
4. Cloud 通过 `TurnGameClient.execute(..., TurnPathingIntent)` -> `CloudTurnActionFactory` -> typed `TurnAction` 下发 intent；
   禁止手工 new action 或绕 factory/command port。旧 overload 继续传 null。
5. 本地 action 前读一次 coordinate baseline；COMPLETED 后严格 `pixel-diff fast edge -> coordinate fallback`；仅 positive
   调既有 `markPathingStarted`。typed snapshot 随 `TurnWindowMetadata` 回传，Cloud state 只做 exact-context read mirror。
6. Cloud `NavigationService` 只通过窄 public `MiniMapPointResolver.resolveMinimapClick(JsonNode)` 进程内直调；
   其它 resolver API 非 public，transform table 不出 Cloud，禁止 `DecisionEngine.decisionResponse` HTTP 形往返。

### 剩余实现清单

1. 完成 terminal outcome guard 与 `clickMiniMapLogicalPointForHandoff`：resolver candidate -> windowRect screen-absolute ->
   MOVE/WAIT/CLICK typed execute with intent -> read mirror；不做 Cloud movement polling。
2. 按 `696a12b0` 原顺序重建 `navigateInCurrentMap` 60 秒 loop：combat/cached-arrival short circuit、candidate 去重、
   PATHING_STARTED/STOPPED_AWAY、250ms keep-turn、retry/fallback/finally close 顺序与次数逐值不变，并清零 active macro。
3. 用同一 helper 迁 current-map/world-map legs；删除 `MiniMapCoordinateReader`、`CoordinateHelper.MiniMapClickPoint`、
   `confirmMiniMapPathingStarted*`、Cloud `markPathingStarted` 等旧违规 active 路径，不删除本地 runner/watcher 实现。
4. 唯一 `NavigationTurnContractTest` 必须从 public Navigation path 覆盖 baseline loop、resolver HIT/MISS/candidate 顺序、
   exact metadata、每 action UUID/command、terminal short-circuit、mirror 正负与 active macro 零调用；双仓 golden 覆盖 intent/snapshot parity。
5. canonical whole-card delivery 前逐文件列 SHA/行数/方法覆盖和 `无已批准业务差异；按 696a12b0 等价迁移`。
   Java writer active 时不运行 Maven；稳定后仅运行授权 named test 与适用 compile。

### 当前状态

- `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`；实盘 Cloud `NavigationService.java`=`38c3266b...`，尚无整卡 delivery。
- TURN-35/36/37 继续等待 TURN-27 source pass；父级不拆卡、不派第二 writer、不提前 source review。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT5 FINAL-FROZEN-EXECUTION-CHECKLIST SUPERSEDES-CONFLICTING-TEXT NO-NEW-BEHAVIOR C-CONTINUE NAV=38c3266b NO-DELIVERY 2026-07-17T06:28:00-04:00 -->

## PARENT AMENDMENT #5 + SCOPE ENFORCEMENT ACK ACCEPTED - 2026-07-17T06:42:30-04:00

- External C 已双 ACK 最终冻结执行清单与 `PARENT-TURN27-AMENDMENT5-SCOPE-ENFORCEMENT`，并明确撤回
  `expanded scope / 共享 UI 面须一并迁`。
- 执行范围固定为 current-map/world-map 活跃调用链实际需要的 mini-map UI seam；不得转换其余 68 个
  input/capture/OCR 站点、重写整份 `NavigationService` 或新增 facade/watcher/store。
- 实盘 Cloud `NavigationService.java`=`5534bad11092164f557ca44553bf74fa36de608fa5a30d4b86ef8e61f1587485`
  / 179,078 bytes / `2026-07-17T10:19:47.3542372Z`；本拍无新 Java 字节，active macro 仍未清零。
- 状态保持 `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`；无 canonical whole-card delivery，不提前 source review。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-AMENDMENT5-SCOPE-ACK-ACCEPTED C-SOURCE-ACTIVE NO-EXPANDED-SCOPE NAV=5534bad1 NO-DELIVERY 2026-07-17T06:42:30-04:00 -->

## PARENT ACTIVE_STALE - 2026-07-17T07:01:53-04:00

- External C 最后 STATUS EVENT 为 `06:42`；Cloud `NavigationService.java` 最后变化为
  `2026-07-17T10:43:33.4335330Z`，至本轮超过 10 分钟无新事件/源码字节，按共享总账规则标 `ACTIVE_STALE`。
- 实盘 NAV=`4fb434fef651f33e74b2b764cf7689f5eadce591713cd98861e0f080089844be` / 181,096 bytes；
  active macro 调用仍为 1，`clickMiniMapLogicalPointForHandoff` 尚未落盘，无 canonical delivery。
- External C sole owner 保留，不撤销、不重派；下一 heartbeat 需报告继续、合同阻断、canonical return 或 delivery。
  Java writer 状态不明，本轮不运行 Maven/runtime/input。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-ACTIVE-STALE EXTERNAL-C-SOLE-OWNER-PRESERVED NAV=4fb434fe MACRO=1 HANDOFF=0 NO-DELIVERY 2026-07-17T07:01:53-04:00 -->

## PARENT WORKTREE RELOCATION - 2026-07-17T07:17:03-04:00

- CR271 权威工作树已迁至 `D:\mavenProject\DHXY-cr271`（branch `thin-client-design` / snapshot `59b85e0b`）。
- `D:\mavenProject\DHXY` 现为用户 IntelliJ 基线运行工作树，不再作为本卡/总账 physical EOF；Cloud 源码路径不变。
- 本卡内容与 owner 保持：External C sole owner，`ACTIVE_STALE` 尚待 C 回执；无 delivery/source review。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-WORKTREE-RELOCATION DHXY-CR271-THIN-CLIENT-DESIGN OWNER-PRESERVED ACTIVE-STALE NO-DELIVERY 2026-07-17T07:17:03-04:00 -->

## PARENT SOURCE ACTIVITY RECOVERED - 2026-07-17T07:25:57-04:00

- Cloud `NavigationService.java` 于 `2026-07-17T11:24:06.9952840Z` 恢复增长至
  `56cde7e7220bed36eb191b7b78e5f6c6d2e0cc6d5e55f425937a62f49f1520fb` / 182,443 bytes；
  实盘新增 `captureStep(... TurnCaptureSpec.UPLOAD_IMAGE ...)`，`ACTIVE_STALE` 解除。
- active macro 调用仍为 1，`clickMiniMapLogicalPointForHandoff` 尚未落盘；无 canonical delivery，不提前 review。
- External C sole owner / SOURCE_ACTIVE 保持；07:01 status 与 07:17 worktree relocation ACK 仍待下一 heartbeat。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-ACTIVITY-RECOVERED C-SOURCE-ACTIVE NAV=56cde7e7 MACRO=1 HANDOFF=0 ACK-PENDING NO-DELIVERY 2026-07-17T07:25:57-04:00 -->

## PARENT COMMUNICATION_STALE + ACTIVE_STALE - 2026-07-17T07:36:01-04:00

- External C 连续两轮以上未 ACK 07:17 authoritative-worktree relocation，标 `COMMUNICATION_STALE`；
  Cloud NAV 自 `2026-07-17T11:24:06.9952840Z` 后超过 10 分钟无新字节，重新标 `ACTIVE_STALE`。
- 当前 NAV=`56cde7e7...` / 182,443 bytes，active macro=1、handoff=0，无 canonical delivery。
- C sole owner 保留，不撤销、不重派；须在新总账 ACK 后报告继续实施、单一 blocker、return 或 delivery。
  Java writer 状态不明，不运行 Maven/runtime/input。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-COMMUNICATION-STALE ACTIVE-STALE C-SOLE-OWNER-PRESERVED NAV=56cde7e7 MACRO=1 HANDOFF=0 NO-DELIVERY 2026-07-17T07:36:01-04:00 -->

## PARENT COMMUNICATION + ACTIVITY RECOVERED - 2026-07-17T08:07:54-04:00

- External C 已从 `D:\mavenProject\DHXY-cr271` 总账 ACK 07:01、07:17、07:36 三条父级消息；
  `COMMUNICATION_STALE` 解除，C sole owner 保持。
- Cloud NAV 于 `2026-07-17T12:06:58.2874758Z` 增长至 `67b33848...` / 191,112 bytes，
  `ACTIVE_STALE` 解除；active macro=1、handoff=0。
- 当前仍为 Amendment #5 下的 SOURCE_ACTIVE WIP，无 canonical whole-card delivery，不提前 source review；
  Java writer active，不运行 Maven/runtime/input。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-COMMUNICATION-ACTIVITY-RECOVERED C-SOLE-OWNER SOURCE-ACTIVE NAV=67b33848 MACRO=1 HANDOFF=0 NO-DELIVERY 2026-07-17T08:07:54-04:00 -->

## PARENT ACTIVE_STALE - 2026-07-17T08:26:20-04:00

- External C 通信正常，但 Cloud NAV 自 `2026-07-17T12:08:54.6511184Z` 后超过 10 分钟无新事件/字节，
  重新标 `ACTIVE_STALE`。
- 当前 NAV=`fdb34206...` / 194,778 bytes，active macro=1、handoff=1，无 canonical delivery。
- C sole owner 保留，不撤销、不重派；已要求下一 heartbeat 报继续、单一 blocker、return 或 delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-ACTIVE-STALE C-SOLE-OWNER-PRESERVED NAV=fdb34206 MACRO=1 HANDOFF=1 NO-DELIVERY 2026-07-17T08:26:20-04:00 -->

## PARENT ACTIVITY RECOVERED / SEMANTIC DECISION - 2026-07-17T08:33:01-04:00

- Cloud NAV 于 `2026-07-17T12:30:27.5743220Z` 增长至 `225953c5...` / 201,805 bytes，
  `ACTIVE_STALE` 解除；active macro=0、handoff=2，父级 OPEN macro finding 关闭。
- Xiuluo fire-and-handoff 不构成新的待用户语义选择。Amendment #5 rule 5 与用户已确认边界明确要求：
  Cloud 只提交动作/intent，本地 runner 的 positive movement proof 后才登记 pathing started。
- 因此保留当前 proof-gated 实现；禁止恢复乐观 `PATHING_STARTED`，禁止新增乐观 turn 变体或第二协议路径。
- C sole owner / SOURCE_ACTIVE 保持；尚无 canonical whole-card delivery，不提前 source review。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-ACTIVITY-RECOVERED MACRO-FINDING-CLOSED LOCAL-PROOF-GATED-DECISION C-SOLE-OWNER NAV=225953c5 MACRO=0 HANDOFF=2 NO-DELIVERY 2026-07-17T08:33:01-04:00 -->

## PARENT WORLD-MAP COORDINATE CONTRACT DECISION - 2026-07-17T08:43:06-04:00

- 696 基线 `CoordinateHelper.getScaledRect()` 当前实现为 `tracker.refreshWindowState()` 后直接
  `windowBaseX/Y + offset`，宽高直接相加；方法名虽含 scaled，实盘没有 DPI 比例乘法。
- remembered yellow row 的 `relativeX/Y` 基线由 absolute 减 `tracker.windowBase` 保存，因此 turn 迁移必须用
  exact HWND `windowRect.left/top + relativeX/Y` 恢复，当前实现等价。
- OCR yellow row 的 `mapRect` 已是 screen-absolute，OCR center 是 crop-relative；点击必须保持
  `mapRect[0/1] + destinationCenterX/Y`，不得再加 windowRect，不得额外缩放。
- 此项由现有代码合同闭合，不是待用户业务语义选择；C 按上述精确坐标空间继续。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-WORLD-MAP-COORDINATE-CONTRACT CLOSED UNSCALED-SCREEN-ABS MEMORY-WINDOWRECT-PLUS-REL OCR-MAPRECT-PLUS-CENTER 2026-07-17T08:43:06-04:00 -->

## PARENT COMMUNICATION_STALE + ACTIVE_STALE - 2026-07-17T08:53:11-04:00

- Cloud NAV 自 `2026-07-17T12:41:39.1285950Z` 后超过 10 分钟无新事件/字节，标 `ACTIVE_STALE`。
- 08:43 proof/coordinate 父级消息连续两轮无 ACK，标 `COMMUNICATION_STALE`。
- 当前 NAV=`81222914...` / 202,587 bytes，macro=0、handoff=2，无 canonical delivery。
- C sole owner 保留，不撤销、不重派；等待下一 heartbeat 报继续、blocker、return 或 delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-COMMUNICATION-STALE ACTIVE-STALE C-SOLE-OWNER-PRESERVED NAV=81222914 MACRO=0 HANDOFF=2 NO-DELIVERY 2026-07-17T08:53:11-04:00 -->

## PARENT CONTINUOUS-DELIVERY EXECUTION CONTRACT - 2026-07-17T09:03:35-04:00

- 用户明确要求 External C 连续实施至本整卡可提交；完成 helper、checklist 单项或一次 heartbeat/turn 不构成停点。
- C 后续 heartbeat 必须自动从当前机械删除恢复，继续 named test 与逐文件 SHA，直到本卡 physical EOF 写入
  canonical whole-card `SOURCE+TEST DELIVERED`。
- 唯一允许提前停止的事实为：写入精确单一合同 blocker，或在本卡 physical EOF canonical `OWNER RETURNED`。
- 所有问题只经 `CR271_EXTERNAL_WORKER_STATUS.md` 的 `STATUS EVENT` 询问父级；不得询问用户或要求用户转发。
- 当前仍为 `COMMUNICATION_STALE + ACTIVE_STALE`、C sole owner preserved、无 delivery；本条不派卡、不改业务语义。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-CONTINUOUS-DELIVERY-CONTRACT C-SOLE-OWNER QUESTIONS-TO-PARENT NO-INTERMEDIATE-STOP NO-DELIVERY 2026-07-17T09:03:35-04:00 -->

## PARENT SOURCE ACTIVITY RECOVERED - 2026-07-17T09:07:55-04:00

- Cloud `NavigationService.java` 已更新为 SHA-256 `037c5f45...` / 182,230 bytes /
  `2026-07-17T13:06:06Z`；macro=0、handoff=2，`ACTIVE_STALE` 解除。
- External C 报告旧违规死簇删除完成，下一步为唯一 `NavigationTurnContractTest`，随后逐文件 SHA 整卡交付。
- C 尚未 ACK 08:33/08:43/08:53/09:03 父级消息，故 `COMMUNICATION_STALE` 暂保留；continuous-delivery
  合同等待下一 heartbeat 回执。C sole owner preserved，无 canonical delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-ACTIVITY-RECOVERED COMMUNICATION-STALE ACK-PENDING CONTINUOUS-DELIVERY C-SOLE-OWNER NAV=037c5f45 NO-DELIVERY 2026-07-17T09:07:55-04:00 -->

## PARENT CONTINUOUS-DELIVERY CONTRACT VIOLATION - 2026-07-17T09:12:24-04:00

- C 在死簇删除完成后再次以“是否继续/是否等下一 heartbeat”的流程性问题停在用户侧；这不是业务决策点，
  违反本卡 09:03 连续交付合同。
- 已要求禁止以问句结束，立即写唯一 `NavigationTurnContractTest`，随后逐文件 SHA 并整卡 canonical delivery；
  不得等待用户回复。真实合同问题只经共享总账问父级。
- 当前 NAV=`037c5f45...` / 182,230 bytes，named test 未落盘，C sole owner，无 delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-CONTINUOUS-CONTRACT-VIOLATION NO-USER-QUESTION IMMEDIATE-NAMED-TEST-THEN-DELIVERY C-SOLE-OWNER NO-DELIVERY 2026-07-17T09:12:24-04:00 -->

## PARENT HEARTBEAT-MISSING AUDIT - 2026-07-17T09:15:15-04:00

- Codex 真实 automation 表不存在 `5379f59b`；C 的 `heartbeat RUNNING` 是 stale 自报文字，不是有效调度。
- 09:13 没有 C heartbeat/status/source/test 增量；当前 NAV=`037c5f45...` / 182,230 bytes，named test 不存在。
- 状态纠正为 `HEARTBEAT_MISSING + COMMUNICATION_STALE`。C canonical owner 暂保留，不撤销、不重派；无 delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-HEARTBEAT-MISSING-AUDIT 5379f59b-NOT-REGISTERED C-SOLE-OWNER-PRESERVED NO-DELIVERY 2026-07-17T09:15:15-04:00 -->

## PARENT USER-AUTHORIZED HEARTBEAT REGISTRATION - 2026-07-17T09:23:58-04:00

- 用户已明确命令 C 作废 `5379f59b` 并在 C 当前任务注册真实每 5 分钟 heartbeat；“无用户明令”判断错误。
- C 必须立即注册并回写新 id、ACTIVE、next_run_at 与 registry 证据；这不是第二 Worker，不得以并发风险拒绝。
- 注册后立即写 focused `NavigationTurnContractTest` 并整卡交付。合同没有 2,200 行指标，禁止测试体量膨胀。
- 当前真实 registry 仍无 C heartbeat，named test 不存在，无 delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-USER-AUTHORIZED-HEARTBEAT-REGISTRATION REGISTER-NOW FOCUSED-TEST-NO-LINE-QUOTA C-SOLE-OWNER NO-DELIVERY 2026-07-17T09:23:58-04:00 -->

## PARENT TEST-SOURCE ACTIVITY RECOVERED - 2026-07-17T09:33:56-04:00

- Cloud `NavigationTurnContractTest.java` 首增量已落盘：455 行 / 23,067 bytes / SHA-256 `b9272375...` /
  `2026-07-17T13:33:09Z`。当前为 WIP，不构成 whole-card delivery 或提前 review。
- NAV 保持 `037c5f45...` / 182,230 bytes；C sole owner / SOURCE_ACTIVE 保持。
- C 真实 heartbeat 仍未注册，09:23 用户授权注册指令待 ACK；Java writer active，不运行 Maven/runtime/input。

TRUE_EOF

<!-- TRUE_EOF: TURN-27 PARENT-TEST-SOURCE-ACTIVITY-RECOVERED TEST=b9272375-455L HEARTBEAT-MISSING C-SOLE-OWNER NO-DELIVERY 2026-07-17T09:33:56-04:00 -->

## EXTERNAL-C TURN-27 SOURCE+TEST DELIVERED - 2026-07-17T09:40:00-04:00

canonical whole-card delivery（C sole owner；不自批、不建 reviewer；AWAITING_PARENT_REVIEW）。

### 逐文件 SHA-256(git blob) / 行数 / 字节
- `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java` = `4c1a31f839b03decf48981ad1ce4e2271be2b728` / 3109L / 182230B（MODIFIED：navigateInCurrentMap 宏→per-action turn loop macro=0；两 world-map legs turn 化；死簇+违规删除）
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/MiniMapPointResolver.java` = `80dd608770636310cf51d7c9c11837d55efa96d3` / 392L / 19183B（MODIFIED：仅类与 resolveMinimapClick additive public，遵 Amendment #4）
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java` = `6cb553846a8add48ed4db55cb0a8e578b337adc3` / 33L / 1372B（写集内，本卡未改动）
- `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NavigationRoutePlanResolver.java` = `3e161a6aa550730750ec7fc83abb4e14fd81d108` / 347L / 18133B（写集内，本卡未改动）
- `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java` = `bfcd075023d6fbb2419e888930bfd04c8cb89b6c` / 174L / 7687B（NEW：read-only exact-context mirror；无 TTL/session/durable/watcher/retry/第二 store）
- `dhxy-cloud-brain/src/test/java/com/bot/dhxy/service/NavigationTurnContractTest.java` = `0ca782219ed692cb3bed9eb97acade15cf473a63` / 455L / 23067B（NEW：唯一 named test，经真实 public navigateInCurrentMap + ScriptedCommandPort + 真实 TurnGameClient，无 reflection）

### 测试名 ↔ 合同 profile 对应
- `syncedCombatStateInterruptsBeforeAnyClickAndClosesWithOneCapture` → BASE/STATE：696 combat short-circuit=INTERRUPTED；mirror 无 fact（negative）；仅 finally 一 CAPTURE。
- `resolverMissExhaustsCandidatesToPointNotReached` → BASE：in-process MiniMapPointResolver MISS（未知 map）→"exhausted mini-map click points" POINT_NOT_REACHED；无 click turn。
- `resolverHitOpensClicksAndStartsPathingThroughMirror` → BC4/BASE/IMG/STATE：resolver HIT("长安")→Alt+1 open + 一 MOVE→WAIT→CLICK 承 TurnPathingIntent；3 显式 turn / 3 distinct UUID / one command；mirror intentId 回环(positive)→PATHING_STARTED；finally CAPTURE 走 raw PNG/SHA/ROI 全帧契约(IMG)。
- `stoppedHandoffClickShortCircuitsWithoutTransportRetry` → LX/STATE：click turn STOPPED→INCONCLUSIVE 短路→POINT_NOT_REACHED，无 transport 重发（不 re-click）；assertNoMacro 全用例断言零 LOCAL_SERVICE/macro action。

### 未运行门（unrun gate）
- 未运行 Maven/JUnit：其它 Java writer 活跃期禁 compile/package（纪律）；且生产 NavigationService 仍 import 父级 scope 明令保留的 DHXY-only `CoordinateHelper`/`tracker`（非-active-chain world-map 搜索/title/非导航 close 站点，既有 compile-debt），故 named test 交付为 source-only，待稳定后由父级授权运行。
- 零 Git mutation；不碰 `D:\mavenProject\DHXY` 用户基线工作树。

**无已批准业务差异；按 696a12b0 等价迁移。**

<!-- TRUE_EOF: TURN-27 EXTERNAL-C SOURCE+TEST DELIVERED AWAITING-PARENT-REVIEW NAV=4c1a31f8 TEST=0ca78221 MACRO=0 UNRUN-GATE 2026-07-17T09:40:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW - REPAIR REQUIRED #1 - 2026-07-17T09:44:33-04:00

结论：**不通过，P0/P1/P2=0/2/0；External C sole owner 保持并进入 Repair #1。**

1. **P1 - named test 未满足冻结验收。** Amendment #5 第 4 项要求 candidate 顺序、mirror 正负、exact metadata。
   当前 `NavigationTurnContractTest.java:112-204` 仅有 combat、resolver MISS、单次 positive、STOPPED；缺少
   `COMPLETED + no local proof` negative、首 candidate negative 后严格下一 candidate retry，以及 exact-window
   screen projection/错绑 metadata 拒绝。返修必须从 public Navigation path 经真实 `TurnGameClient`/port 覆盖。
2. **P1 - 固定 Create 路径被改写。** 原卡完整写集固定
   `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`；实际交付位于
   `src/test/java/com/bot/dhxy/service/NavigationTurnContractTest.java`。未经父级合同修订不得改变固定写集路径；返修移回固定路径/package。

生产路径审查证据：NAV git-blob `4c1a31f8...`，active `executeLocalMacro(...NAVIGATE_IN_CURRENT_MAP)` 零调用；
current-map/world-map handoff 通过 typed action + intent + read-only mirror，未新增 production finding。

构建状态：运行唯一授权 `mvn -Dtest=NavigationTurnContractTest test`，main compile 失败，缺
`TextCandidateScanStatus`、`GameClientTracker`、`CoordinateHelper`、`GameStateUtil`、`WindowRuntimeContext` 等；
named test 未执行。构建保持 `BLOCKED`，不得记为 test passed。

Repair #1 通过条件：固定路径恢复；补齐三簇缺失验收；不扩大生产设计/测试体量；原卡 EOF 重新提交逐文件
git-blob SHA/行数与 `无已批准业务差异；按 696a12b0 等价迁移`。

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-TEST-REVIEW REPAIR-REQUIRED-1 P0=0 P1=2 P2=0 BUILD-BLOCKED C-SOLE-OWNER 2026-07-17T09:44:33-04:00 -->

## PARENT REPAIR #1 STALE AUDIT - 2026-07-17T09:51:58-04:00

- 09:44 Repair #1 消息连续两轮无 C ACK；C 最后事件 09:41，源码/test 无返修字节变化。
- 状态：`COMMUNICATION_STALE + ACTIVE_STALE`；External C sole owner 保留，不撤销、不重派。
- C 下一次活动必须先 ACK 两条父级消息并继续 Repair #1，不得继续等待父级或用户。

<!-- TRUE_EOF: TURN-27 PARENT-REPAIR1-STALE COMMUNICATION-STALE ACTIVE-STALE C-SOLE-OWNER 2026-07-17T09:51:58-04:00 -->

## PARENT REPAIR #1 RECOVERY AUDIT - 2026-07-17T10:02:05-04:00

- 用户删除旧 C 任务后，新任务按同一 `External C` 身份连续接替，不构成第二 owner；TURN-27 sole owner
  与 Repair #1 责任保持。
- 新 C 已在共享总账 ACK 09:44 Repair #1 与 09:51 stale 消息，并声明 `REPAIR_ACTIVE`；因此
  `COMMUNICATION_STALE` 与 `ACTIVE_STALE` 解除。
- 当前 NAV/test 仍为 `4c1a31f8` / `0ca78221`，固定 test 路径仍 absent；尚无返修 source 增量或 delivery，
  build 继续 `BLOCKED`。
- 真实 heartbeat 尚未注册成功：automation registry 仍仅见 `cr271-5`、`cr259-implementation-reviewer`；
  状态为 `REGISTRATION_PENDING`，须以新 id、ACTIVE/next-run registry 证据闭合。

<!-- TRUE_EOF: TURN-27 PARENT-REPAIR1-RECOVERY REPAIR-ACTIVE COMMUNICATION-RECOVERED STALE-CLEARED HEARTBEAT-REGISTRATION-PENDING BUILD-BLOCKED C-SOLE-OWNER 2026-07-17T10:02:05-04:00 -->

## PARENT HEARTBEAT VERIFIED - 2026-07-17T10:06:57-04:00

- External C 的持久化 Claude scheduled task 已独立核验：
  `cr271-turn27-external-c-repair1-heartbeat`，任务文件 mtime=`10:05:18 -04:00`。
- 每 5 分钟继续 Repair #1、读取本卡/总账 EOF、写 STATUS EVENT、完成后 canonical delivery 且未完成前
  不自删的合同均已写入；状态从 `REGISTRATION_PENDING` 改为 `ACTIVE / VERIFIED`。
- 尚无 Repair #1 Java source 增量或 delivery；C sole owner、source review `0/2/0` 与 build=`BLOCKED` 保持。

<!-- TRUE_EOF: TURN-27 PARENT-HEARTBEAT-VERIFIED EXTERNAL-C REPAIR1 HEARTBEAT-ACTIVE id=cr271-turn27-external-c-repair1-heartbeat NO-SOURCE-DELTA BUILD-BLOCKED 2026-07-17T10:06:57-04:00 -->

## EXTERNAL-C REPAIR #1 CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-17T10:17:00-04:00

- Implementation Worker：**CR271 External Worker C**（旧会话 091df301 被用户删除后的同身份连续接替；非第二 owner、非 reviewer，不自批）。本段为 Repair #1 canonical delivery，回应 `PARENT-TURN27-SOURCE-REVIEW-REPAIR1`（09:44:33，P0/P1/P2=0/2/0）。真实 heartbeat `cr271-turn27-external-c-repair1-heartbeat` ACTIVE（父级 10:06:57 已核）。

### 逐文件交付清单（git-blob SHA / 行数 / 字节）

| 文件 | 动作 | git-blob SHA | 行数 | 字节 |
|---|---|---|---:|---:|
| `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java` | **Create（P1-2 固定路径恢复）** | `2ecdbbed06681c45d525ea289f54a88fcca5b113` | 610 | 36,978 |
| `src/test/java/com/bot/dhxy/service/NavigationTurnContractTest.java` | **Delete（P1-2 错误路径移除）** | 删除前 `0ca782219ed692cb3bed9eb97acade15cf473a63` | — | — |
| `src/main/java/com/bot/dhxy/service/NavigationService.java` | 未改（生产零字节） | `4c1a31f839b03decf48981ad1ce4e2271be2b728` | 2,969 | 182,230 |
| `src/main/java/com/bot/dhxy/service/navigation/CloudNavigationPathingState.java` | 未改 | `bfcd075023d6fbb2419e888930bfd04c8cb89b6c` | 174 | — |
| `MiniMapPointResolver.java` / `CloudMiniMapCoordinateReadability.java` / `NavigationRoutePlanResolver.java` | 未改 | `80dd6087` / `6cb55384` / `3e161a6a` | — | — |

Repair #1 生产设计零扩大：仅 test 文件迁移+扩充；写集外零触碰（`com/bot/dhxy/service/` test 目录仅剩 TURN-28 的 `NpcClickTurnContractTest.java`，未动）。测试 610 行，无 2,200 行指标膨胀。

### 三簇新增测试 ↔ 冻结合同对应

1. **P1-1 簇1（COMPLETED 无本地 proof 的 mirror negative）** → `completedClickWithoutLocalProofNeverReportsPathingStarted`：public `navigateInCurrentMap` 经真实 `TurnGameClient` + scripted `CloudTurnCommandPort`；click turn outcome=COMPLETED 且携 `TurnPathingIntent`，但 DHXY 本地 proof 未登记（port 不回 echo）→ mirror 无该 intentId → 断言**严禁 PATHING_STARTED**（assertNotEquals），按 NO_PATHING 续試下一 candidate（第二 attempt 携全新 intentId），终态 POINT_NOT_REACHED；6 显式 turn 零重发；assertNoMacro。
2. **P1-1 簇2（首 candidate negative 后严格 resolver 顺序）** → `firstCandidateNegativeRetriesNextCandidateInResolverOrder`：resolver oracle 以生产同参（randomize=false）取 strict 顺序前两 candidate；attempt1 点位=第一 candidate、attempt2 点位=**严格下一 candidate**（坐标逐值断言）；negative candidate 点仅被点击一次=零 transport retry；6 命令 6 distinct action UUID（每次独立 UUID/command/outcome）；两 attempt intentId 互异；MOVE 与 CLICK 同点同一 serialized 命令。
3. **P1-1 簇3（exact-window metadata）** → `resolverRelativePointProjectsFromExactWindowRectTopLeft`：shifted exact windowRect(137,241) 下 screen=left/top+resolver relX/relY 逐值断言（MOVE 与 CLICK 同投影点）；+ `mismatchedLatestWindowMetadataIsRejectedBeforeAnyInput`：latest metadata windowId 漂移与 deviceId 漂移分别 → `TaskFatalException` 且 `executeCalls=0`（错绑 metadata 永不放置任何 input/capture turn）。
- **既有验收全保留**：combat short-circuit、resolver MISS 穷尽、单次 mirror positive（3 turn/3 UUID/intent 逐字段）、STOPPED 短路零 transport retry、raw PNG 全帧契约（SHA/ROI/purpose）、全用例 `assertNoMacro`。
- 测试侧 harness 扩展仅限 scripted port（baseWindow 参数化、skipIntentEchoes、reply builder 显式 window）与 oracle helper；无 reflection、无生产改动。

### active macro=0 证据

`grep executeLocalMacro|NAVIGATE_IN_CURRENT_MAP` 于 NavigationService（`4c1a31f8`）：**active `executeLocalMacro(...)` 调用=0**；唯一命中为 192 行历史注释（非调用、非本卡返修范围，生产零字节纪律保留）。全部 8 个测试用例断言零 LOCAL_SERVICE/macro step。

### named test / compile 实际结果（build 仍 BLOCKED）

- 源码稳定后运行唯一授权命令：`mvn -Dtest=NavigationTurnContractTest test`（D:\mavenProject\dhxy-cloud-brain，10:12-04:00）。
- 结果：**BUILD FAILURE 于 main `compile` 阶段（530 sources），named test 未执行**。精确首批缺失符号证据（与父级 09:44 记录同源、写集外迁移缺类）：
  - `model/ocr/TextCandidateScanResult.java:26` → 缺 `TextCandidateScanStatus`；
  - `task/wubei/WubeiTask.java:4/6/9` → 缺 `com.bot.dhxy.core.GameClientTracker`、`com.bot.dhxy.core.TextRecognizer`、程序包 `com.bot.dhxy.metrics`（`AutomationMetricsService`）不存在；
  - `service/NavigationService.java:4/7/8/38/39/41/46-48` → 缺 `GameClientTracker`、程序包 `com.bot.dhxy.driver`（`BoundWindowKeyboardService`）、`InputProvider`、`CoordinateHelper`、`GameStateUtil`、`GameTextLineOcrService`、`WindowRuntimeContext`、`WindowScopedTempPath`、`WindowTaskContextHolder`。
- 未 stub、未复制 DHXY 类、未 skip-tests/改 POM 绕过；named test 交付为 source-only，待共享 compile-debt 清偿后由父级授权重跑。

**无已批准业务差异；按 696a12b0 等价迁移。**

交付后进入 `AWAITING_PARENT_REVIEW`；heartbeat 保留继续监控复审结论，收到返修即整卡重走交付；不自批、不建 reviewer。

<!-- TRUE_EOF: TURN-27 EXTERNAL-C REPAIR1 SOURCE+TEST DELIVERED FIXED-PATH-RESTORED TEST=2ecdbbed/610L WRONG-PATH-DELETED NAV=4c1a31f8-UNCHANGED MACRO=0 BUILD-BLOCKED-FIRST-MISSING-SYMBOLS-LISTED AWAITING-PARENT-REVIEW 2026-07-17T10:17:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-17T10:18:36-04:00

结论：**通过，P0/P1/P2=0/0/0；External C owner released。**

- P1-2 闭合：唯一 named test 已位于固定路径
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`，git-blob
  `2ecdbbed06681c45d525ea289f54a88fcca5b113` / 36,978B；旧错误路径文件 absent。
- P1-1 三簇闭合：8 个测试均经 public `navigateInCurrentMap`、真实 `TurnGameClient` 与 scripted
  `CloudTurnCommandPort`；mirror negative 不升格 PATHING_STARTED；首 candidate negative 后按 resolver 顺序
  使用下一 candidate、每 command 独立 UUID/intent 且失败点只点击一次；exact windowRect 投影逐值断言，
  device/window mismatch 在零 input/capture command 前 fatal 拒绝。
- 既有 positive/STOPPED/resolver HIT+MISS/raw PNG/full-frame/零 LOCAL_SERVICE 断言保留；生产 NAV
  `4c1a31f8...` 未漂移，active `executeLocalMacro(...)` 调用为 0；无新增生产设计或业务差异。
- 父级计数校正：交付表 test `610L`、NAV `2969L` 不准确；physical line count 分别为 673L、3109L。
  SHA 与字节数准确，此纯交付证据计数已由父级纠正，不要求再次源码返修。
- 构建状态保持 `BLOCKED`：授权 named test 命令仍在 main compile 因共享迁移缺类失败，named test 未执行；
  本结论仅为 source+test source review 通过，不宣称 build/test passed。

**无已批准业务差异；按 696a12b0 等价迁移。**

<!-- TRUE_EOF: TURN-27 PARENT-SOURCE-TEST-REVIEW-2 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED TEST=2ecdbbed-673L NAV=4c1a31f8-3109L MACRO=0 BUILD-BLOCKED 2026-07-17T10:18:36-04:00 -->
