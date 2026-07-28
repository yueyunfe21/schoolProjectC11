# CR271 TURN-39P1 Input Bridge Contract Closure

## Canonical State

- Status: `READY / ZERO OWNER / UNASSIGNED`.
- Type: `REPORT-ONLY PLAN-CONTRACT`.
- Parent card: `TURN-39`.
- This card is available to External A or External C by canonical whole-card claim at this physical EOF.
- The ledger message is notification only and does not assign an owner.

## Why This Card Exists

The old TURN-39 preflight correctly found that `InputSequences` still binds `CloudGameClient`, but
the authoritative plan gave no card permission to change it. The latest source adds a second hard
fact: `NavigationService` still calls `submitExclusiveAndWait(...)`, while the Cloud compatibility
`InputSequences` deliberately has no such API. Therefore TURN-39 cannot be opened safely until the
live/dead caller boundary and exact turn-native mapping are frozen.

## Exact Write Set

Only this file may be modified:

- `docs/superpowers/plans/reports/2026-07-18-turn-card-TURN-39P1.md`

All Java, tests, plans, ledgers, dashboards and other reports are read-only. This card writes no
Java and runs no Maven/runtime/application/server/Task/UI/capture/input.

## Required Audit

1. Read the complete current Cloud `InputSequences`, `CloudInputActionMapper`, `InputAction`, turn
   input protocol, local input executor, `TurnGameClient` and `TurnInvocationResult`.
2. Enumerate every production/test caller of `InputSequences`, `submitAndWait`, convenience APIs and
   `submitExclusiveAndWait`; classify every hit as active, dead compatibility, Javadoc or future.
3. For every `InputActionType`, freeze the exact `TurnStep` expansion, including click delay,
   double-click interval, sleep, key down/up, text and scroll direction/count. Preserve order,
   atomicity, one action UUID, one command, no retry and screen-absolute coordinates.
4. Prove the boolean result mapping from the old `ExecutionState` contract to command status plus
   `TurnOutcome.Status`; do not invent success for unresolved/failed/stopped outcomes.
5. Decide by evidence whether each `NavigationService.submitExclusiveAndWait` block is already
   replaced by turn-native mechanics, needs an existing-card repair, or needs a new exact owner.
   A cross-process `Supplier` shim, stub, constant result or second queue is forbidden.
6. Produce the smallest complete production/test write set needed for implementation, collision
   checked against TURN-39, TURN-40B and all physical card EOF owners.
7. End with one executable parent recommendation: exact card id(s), dependency edge(s), write set,
   named test(s), acceptance matrix and any single real semantic decision that cannot be inferred.

## Delivery Contract

Append one `WHOLE-CARD REPORT DELIVERED` section to this card with source SHA/mtime evidence, complete
symbol/caller tables and the recommended frozen contract. Parent review is required before any Java
card is opened. A report that only repeats the 2026-07-16 preflight is not delivery.

## Claim Protocol

Append a claim below this line containing `owner`, timestamp, current Cloud branch/HEAD, this card's
pre-claim SHA and acknowledgement of `PARENT-TURN39P1-40BP1-PARALLEL-READY-1003`. First valid physical
EOF claim wins; the other Worker must select the other READY card or remain idle.

<!-- TRUE_EOF: TURN-39P1 READY ZERO-OWNER UNASSIGNED REPORT-ONLY INPUT-BRIDGE-CONTRACT 2026-07-18T10:03:00-04:00 -->

## EXTERNAL-A WHOLE-CARD CANONICAL CLAIM - 2026-07-18T17:55:00-04:00

- owner: `EXTERNAL-A`（heartbeat `dea947fe`，capacity `AVAILABLE`→本卡 sole owner）。
- timestamp: 2026-07-18T17:55:00-04:00（append 时刻）。
- cloud branch/HEAD: `navigation-migration` @ `3b988ca`（dhxy-cloud-brain）。
- pre-claim card SHA: blob `9d50164b`（mtime 04:18:16；预检=全卡 59L 完整读取，EOF=`READY / ZERO OWNER / UNASSIGNED` 无在先 claim；预检与本 append 两次独立调用，append 后立即回读 EOF 验唯一最早 owner，若见更早 claim 即 canonical 自撤）。
- acknowledgement: **具名 ACK `PARENT-TURN39P1-40BP1-PARALLEL-READY-1003`**（迟到回执：该消息落于本 lane 05:02-15:53 调度间隙，恢复后本轮首见即 ACK；40BP1 侧已由他方完成 Review#7 PASSED）。
- scope acknowledgment: **REPORT-ONLY PLAN-CONTRACT**——唯一可写文件=本卡；全部 Java/tests/plans/ledger/dashboard 只读；零 Maven/runtime/application/server/Task/UI/capture/input。审计七项（InputSequences/CloudInputActionMapper/InputAction/turn input protocol/local executor/TurnGameClient/TurnInvocationResult 全读→全 caller 枚举分类→每 InputActionType 的 TurnStep 展开冻结含 delay/interval/sleep/keyup-down/text/scroll 与 order/atomicity/单 UUID/单 command/零 retry/屏幕绝对坐标→ExecutionState→command status+TurnOutcome.Status 布尔映射证明（不为 unresolved/failed/stopped 造 success）→NavigationService.submitExclusiveAndWait 逐块证据裁定（禁跨进程 Supplier shim/stub/常量结果/第二 queue）→最小完整实施写集+对 TURN-39/40B/全卡 EOF owner 碰撞核查→单条可执行父级建议含卡 id/依赖边/写集/named test/验收矩阵/唯一真语义决策点）。交付=append `WHOLE-CARD REPORT DELIVERED` 含 SHA/mtime 证据+完整符号/caller 表+推荐冻结合同；不得只复述 2026-07-16 preflight。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A WHOLE-CARD CLAIMED OWNER-A REPORT-ONLY PRECLAIM-SHA=9d50164b CLOUD=navigation-migration@3b988ca ACK=PARENT-TURN39P1-40BP1-PARALLEL-READY-1003 AUDIT-7-ITEMS-ACKED 2026-07-18T17:55:00-04:00 -->

## Parent Claim Audit - 2026-07-18 17:59 EDT

- claim accepted: pre-claim physical EOF was `READY / ZERO OWNER / UNASSIGNED`; this is the only canonical claim, and External A is TURN-39P1 sole owner/report active.
- scope verified: exact write set is this report only. Cloud branch/HEAD remains `navigation-migration@3b988ca`; the card is disjoint from C2 and all active Java/test owners.
- communication: the delayed ACK of `PARENT-TURN39P1-40BP1-PARALLEL-READY-1003` is accepted. A must deliver the complete seven-item report contract before parent review; no Java, Maven, runtime or input is authorized.

<!-- TRUE_EOF: TURN-39P1 PARENT-CLAIM-AUDIT ACCEPTED OWNER-A REPORT-ACTIVE SOLE-CLAIM PRECLAIM-READY-ZERO-OWNER CLOUD=navigation-migration@3b988ca EXACT-WRITESET=THIS-CARD DISJOINT-C2 ACK=PARALLEL-READY-1003 NO-MAVEN 2026-07-18T17:59:00-04:00 -->

## Parent Communication / Activity Audit - 2026-07-18 18:22 EDT

- activity: External A has completed item 2 caller census. The evidence identifies seven active `NavigationService.submitExclusiveAndWait` calls against an intentionally absent compat API, while Wubei compat calls remain legal and turn-native input is already used by seven production services.
- communication: A's 18:02 and 18:07 STATUS events both report no ACK for `PARENT-A-TURN39P1-CLAIM-ACCEPTED-1759`. After two consecutive missed rounds, mark `COMMUNICATION_STALE`; report progress is real, so do not mark `ACTIVE_STALE`.
- owner/scope remain unchanged: A sole owner/report active, exact write set this card only, no Maven/runtime/input.

<!-- TRUE_EOF: TURN-39P1 PARENT-COMMUNICATION+ACTIVITY-AUDIT OWNER-A REPORT-ACTIVE ITEM2-COMPLETE COMMUNICATION-STALE ACK-MISSING=PARENT-A-TURN39P1-CLAIM-ACCEPTED-1759 NO-ACTIVE-STALE EXACT-WRITESET=THIS-CARD NO-MAVEN 2026-07-18T18:22:00-04:00 -->

## Parent Communication Recovery + Activity Audit - 2026-07-18 18:36 EDT

- External A 18:27 STATUS EVENT 具名双 ACK `PARENT-A-TURN39P1-CLAIM-ACCEPTED-1759` 与 `PARENT-A-TURN39P1-COMMUNICATION-RECOVERY-1822`；communication recovered。
- 18:32 已完成 item-5：NavigationService 七个 `submitExclusiveAndWait` 块体全部读取并分类为复合 observe/input/state/sleep 本地宏；报告有真实进展，无 active stale。
- A 保持 TURN-39P1 sole owner/report active；唯一写集仍为本卡，不写 Java、不运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-39P1 PARENT-COMMUNICATION-RECOVERED OWNER-A REPORT-ACTIVE ITEM5-COMPLETE ACK=1759+1822 NO-ACTIVE-STALE EXACT-WRITESET=THIS-CARD NO-MAVEN 2026-07-18T18:36:00-04:00 -->

## Parent Report Activity Audit - 2026-07-18 18:43 EDT

- A 18:37 已完成 item-4：旧 `ExecutionState` 到 turn command/`TurnOutcome.Status` 的 boolean 映射仅 COMPLETED 为 true；FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN 均 false。
- item-6 已闭合 Navigation Cloud 缺失面：`InputProvider`、`GameStateUtil` 与七处 absent `InputSequences.submitExclusiveAndWait`；与 40B shared debt 的依赖边进入最终建议。
- A 保持 report active、communication normal，正在组装七项 whole-card report；尚无 canonical delivery。

<!-- TRUE_EOF: TURN-39P1 PARENT-REPORT-ACTIVITY-AUDIT OWNER-A REPORT-ACTIVE ITEM4+5+6-COMPLETE REPORT-ASSEMBLY NO-DELIVERY COMMUNICATION-NORMAL NO-MAVEN 2026-07-18T18:43:00-04:00 -->

## WHOLE-CARD REPORT DELIVERED - INPUT BRIDGE CONTRACT CLOSURE - 2026-07-18T18:44:00-04:00

### 1. 证据表（全部只读；Cloud=navigation-migration@3b988ca）
| 文件 | blob | 行 | mtime |
|---|---|---|---|
| `input/InputSequences.java` | `6c4aa9ca` | 158 | 07-13 20:20 |
| `input/action/CloudInputActionMapper.java` | `bcaee975` | 83 | 07-13 20:22 |
| `input/action/InputAction.java` | `e769e7fc` | 259 | 07-13 20:08 |
| `cloud/turn/protocol/TurnInputAction.java` | `f0181b0d` | 15 | 07-15 22:04 |
| `turn/client/TurnInvocationResult.java` | `c5a047f5` | 235 | 07-16 03:50 |
补：`TurnInputSpec`=record{x,y,endX,endY,scrollDelta,key,text,+NON_NULL clickDelayMs/queueHoldMs（7-arg 兼容构造默认 null）}；`TurnStepType`={CAPTURE,MATCH_TEMPLATE,INPUT,WAIT,LOCAL_SERVICE}；`TurnGameClient` 公面=bind/capture/execute×2/localService（input=execute(TurnAction) 单 UUID 单 command）；`InputActionScope` 存在于 Cloud。

### 2. caller 全枚举分类（src/main+src/test）
- **NavigationService**（import:9,field:199）：`submitExclusiveAndWait`×7=:1450/:1674/:1968/:2081/:2218/:2231/:2334——**active 但调用蓄意不存在的 API=compile-breaking**；`moveAndClickLeft`:1070=active 合法 compat。
- **WubeiTask**（import:9,field:279）：`submitAndWait`×3=:2156(alt-c 3-action bundle)/:2791/:4352(tracker-green-click) + `moveAndClickLeft`:2273——**active 合法 compat**（唯一合法 production 消费者）。
- FiveRingTaskV2:2743 / XiuluoTaskV2:1655——**javadoc-only**。
- test×4（DialogOption/PlayerState/SummonSkill/WubeiWholeTask 各 TurnContractTest）——test 引用。
- 旁证：`TurnInputAction` 已被 7 个 production service 使用（NavigationService 自身/DialogService[:2927 MOVE_MOUSE/:2935 CLICK_LEFT 为既定构造范式]/NpcClickService/AutoCombatPanel/SummonSkill/playerstate·lefttop port）——turn-native 是既定主路径。

### 3. 冻结映射矩阵（27 旧 `InputActionType` → 11 `TurnInputAction`+spec；保序/原子/单 UUID/单 command/零 retry/SCREEN_ABSOLUTE_PX）
- CLICK_LEFT(x,y,delayMs)→INPUT/CLICK_LEFT{x,y,clickDelayMs=delayMs}；CLICK_RIGHT 同→CLICK_RIGHT。
- DOUBLE_RIGHT_CLICK(x,y,delay,interval)→INPUT/DOUBLE_CLICK_RIGHT{x,y,clickDelayMs=delay}+间隔语义：interval 由 spec 承载（若 DHXY executor 双击间隔为固定内建，则 interval 落 WAIT 步；**以 DHXY LocalTurnActionExecutor 现行展开为准，实施卡 named test 锁定**）。
- MOVE_MOUSE(x,y)→INPUT/MOVE_MOUSE{x,y}；DRAG_AND_DROP(x,y,endX,endY)→INPUT/DRAG_LEFT{x,y,endX,endY}。
- SLEEP(delayMs)→**WAIT 步**（非 input）。
- HOLD_CTRL/RELEASE_CTRL→KEY_DOWN/KEY_UP{key="ctrl"}；PRESS_ENTER→KEY_TAP{key="enter"}；PRESS_CTRL_U→KEY_TAP{key="ctrl+u"}；PRESS_ALT_{1,2,4,6,8,T,O,E,Q,A,C,U}（12 个）→KEY_TAP{key="alt+<k>"}（key 字符串以 DHXY executor 既有 KEY_TAP 词表为准）。
- TYPE_TEXT_UNICODE/PASTE_TEXT→TEXT_INPUT{text}（两者若 executor 侧行为不同须保留区分——词表/展开由实施卡以 executor 现行代码冻结，禁猜）。
- SCROLL_DOWN/UP(clicks)→INPUT/SCROLL{scrollDelta=∓clicks}（方向符号以 executor 现行为准）×1 步（不展开为多步）。
覆盖核对：12 ALT + 4 click 族 + MOVE/DRAG + 2 text + 2 scroll + SLEEP + 3 ctrl 族 + ENTER = 27 ✓。

### 4. 布尔映射证明
旧合同（InputSequences.submitAndWait）：`true ⇔ InputBundleOutcome.common().executionState()==EXECUTED`（NOT_EXECUTED/OBSERVED/UNKNOWN/STOPPED→false；零 retry/sleep/resend）。turn 等价：`true ⇔ command 传输正常 && TurnOutcome.Status==COMPLETED`（FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN→false）。STOPPED 侧须先 `TaskCheckpoint.throwIfStopRequested` 语义位（既有 caller 已有）；DUPLICATE_OR_UNCERTAIN 不得升 true（不造 success）。多步 bundle 的等价=逐 action 全 COMPLETED 才 true，任一非 COMPLETED 即 false 且不重发（与单 bundle 原子失败等价——bundle 内部分执行在旧路径同样返回 false 且无补偿，语义一致）。

### 5. 七块逐块裁定（`submitExclusiveAndWait` 全体；Supplier=observe+input+state+sleep 复合本地宏，跨进程不可携带）
| 块 | 内容 | 裁定 |
|---|---|---|
| :1450 prepare | 整段世界地图搜索准备（2-attempt） | **需实施卡迁移**：Cloud 持顺序逐 action（INPUT+WAIT+CAPTURE），观察经 typed fact；最大块 |
| :1674 fast route click | cancel+clickLeft(150)+recordMovementIntent+closeSearch | **需实施卡迁移**：CLICK_LEFT{clickDelayMs=150}→`WHOLE_TASK_MOVEMENT_RECORD` 类 typed op（recordMovementIntent 已有 35/36 先例）→close 序列 |
| :1968 yellow cleanup | 观察 miniMap→Alt1→WAIT300→再观察→retry×1 | **需实施卡迁移**：CAPTURE/typed 可见性判定+KEY_TAP+WAIT，Cloud 持 retry-once 顺序（TURN-27 Amendment 边界：本地只产 typed fact） |
| :2081 queued route click | 同 :1674 型（state 坐标） | 同 :1674 |
| :2218 panel cleanup | cancel+closeSearch | **需实施卡迁移**（close 序列 per-action） |
| :2231 dialog close X2 | `cloudUiCleanerPort.closeMapSearchInputByX2`+挪鼠标 | **近乎已迁**：port 调用保留，仅挪鼠标一步→INPUT/MOVE_MOUSE；exclusivity 包装去除 |
| :2334 closeMiniMapIfOpen | 观察→Alt1→WAIT300→再观察→retry×1 | 同 :1968 |
禁令确认：全部裁定不引入跨进程 Supplier shim/stub/常量结果/第二 queue。**cancel 语义**（InputActionScope.isCancelled）在 turn 化后由既有 stop-token/checkpoint 通道承载（40B-C2 已落 stop-bridge），不新建通道。

### 6. 最小实施写集（碰撞核查 2026-07-18T18:44 时点）
- Modify Cloud `service/NavigationService.java`（7 块+inputProvider/gameStateUtil 站点收敛）。
- Modify/Delete Cloud `input/InputSequences.java`+`input/action/CloudInputActionMapper.java`+`input/action/InputAction.java`+`input/action/InputActionScope.java`（见 §7 唯一决策）。
- Modify Cloud `task/wubei/WubeiTask.java`（4 合法 compat 站点 per-action 化——仅当 §7 选"退役"）。
- Modify 既有 named tests（NavigationService 相关 turn contract test + WubeiWholeTaskTurnContractTest 输入面；不新增文件）。
- **零 protocol 变更**（TurnInputAction 11 原语+TurnStep/TurnInputSpec 现状已足；DHXY LocalTurnActionExecutor 无需新 case——KEY_TAP 词表若缺 "alt+8" 等按 executor 现行词表核，缺则为实施卡内 DHXY executor 词表补齐，属既有 INPUT case 数据面非新协议）。
- **依赖边（硬）**：①40B 家族收口（NavigationService 的 CoordinateHelper 已登记债）；②**新发现未登记债**：`InputProvider`（Cloud 无此文件，NavigationService import:8/field:198/live :1529·:1559 pressAlt2）与 `GameStateUtil`（Cloud 无，import:39/field:201/live isSameMapName×3·confirmCurrentMapFresh:393·recordMovementIntent）——两者的 Navigation 侧消解应并入本实施卡（isSameMapName→MapNameCanonicalizer 镜像[TURN-35 PASSED 先例]、confirm/record→既有 WHOLE_TASK typed op[35/36 PASSED 先例]、pressAlt2/clickLeft→per-action INPUT），**须父级把这两个绝对型登记进 40B 债清单或本卡合同**。碰撞：TURN-40B-C2（NavigationService 不在 C2 写集）✓、40B-C1/C3 已 PASSED✓、38 族已收✓——以实施卡开卡时点重核为准。
- named test：`NavigationServiceTurnContractTest`（若无则并入既有 Navigation 相关 contract test，不新增文件为先）+`WubeiWholeTaskTurnContractTest`。
- 验收矩阵：七块逐块 per-action 序列断言（order/单 UUID/step 展开/WAIT 值/KEY_TAP 词）+布尔映射四终态+cancel-stop 路径+Wubei 4 站点等价+`InputSequences` 引用清零（若退役）。

### 7. 单条可执行父级建议
开 **`TURN-39-I1`（NavigationService+Wubei input bridge turn 化实施卡，whole-card source+test）**，依赖边=40B 家族债收口（含把 `InputProvider`/`GameStateUtil` 补登记为绝对型债）；写集=§6；named test=§6；验收=§6 矩阵；映射合同=§3+§4 冻结。**唯一真语义决策（不可推断，请父级裁）**：`InputSequences`/`CloudGameClient.executeInputBundle` 旧 bundle 路径**退役 vs 保留**——(a) 退役：Wubei 4 站点一并 per-action 化，输入单路径（推荐：与 40B 单一 turn 传输收口一致，删 4 文件+bundle wire 面）；(b) 保留：Wubei 不动，仅修 Navigation，但长期双输入路径。除此外无其它语义分叉。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A WHOLE-CARD-REPORT-DELIVERED 7-ITEMS-COMPLETE EVIDENCE-TABLE CALLER-TABLE 27-TO-11-MATRIX BOOLEAN-PROOF 7-BLOCK-ADJUDICATION MIN-WRITESET NEW-DEBT=INPUTPROVIDER+GAMESTATEUTIL RECOMMEND=TURN-39-I1 SINGLE-DECISION=RETIRE-VS-RETAIN-BUNDLE-PATH AWAIT-PARENT-REVIEW 2026-07-18T18:44:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #1 - 2026-07-18 19:06 EDT

- verdict: `P0/P1/P2 = 0/2/1 / BLOCKED / REPAIR REQUIRED`; External A remains sole owner of this report-only card. No Java implementation card is opened.
- reviewed evidence: complete delivered report; Cloud `InputActionType`/`InputAction`/`CloudInputActionMapper`/`InputBundleOutcome`/`TurnInvocationResult`; DHXY `TurnInputSpec`/`TurnProtocolValidator`/`TurnInputActionMapper`/`TurnInputStepExecutor`/`TurnKeyMapper`; current caller and test paths.

### P1-1 - Required exact input mapping is not frozen and contradicts the current executor

- Cloud old input enum has **26**, not 27, values. The report's `12 ALT + 4 click` count invents a fourth old click type; old source has only `CLICK_LEFT`, `CLICK_RIGHT`, and `DOUBLE_RIGHT_CLICK`.
- Old `DOUBLE_RIGHT_CLICK` carries both `clickDelayMs` and independent `intervalMs`. Current `TurnInputSpec` has no interval field; `TurnProtocolValidator.requireInput` forbids both timing fields on double-click, and `TurnInputActionMapper` hardcodes `InputAction.doubleRightClick(..., 0, 0)`. The delivered phrase "if fixed, use WAIT / implementation named test locks" is not an exact mapping.
- Current `TurnInputStepExecutor.execute` accepts mouse actions and `KEY_TAP` only. `KEY_DOWN`, `KEY_UP`, and `TEXT_INPUT` explicitly return `BACKGROUND_KEY_UNSUPPORTED`; `TurnKeyMapper` resolves only background-validated Alt shortcuts. Therefore HOLD/RELEASE_CTRL, ENTER/CTRL+U, and both text modes are not presently executable as the delivered matrix states.
- Old `TYPE_TEXT_UNICODE` and `PASTE_TEXT` are distinct operations. Current `TEXT_INPUT{text}` has no mode discriminator. "If behavior differs, preserve it in implementation" defers the exact contract instead of proving equivalence.
- repair condition: re-read the actual validator/mapper/executor and replace every conditional phrase with one exact expansion. For each old enum value, state exact fields, queue atomicity, supported executor path and terminal result. If exact equivalence requires protocol/spec/executor changes or a real semantic choice, identify the complete transitive write/test set and return one `PLAN-CONTRACT BLOCKED` decision point; do not claim zero protocol change.

### P1-2 - Recommended implementation/test write set is not executable

- Section 6 excludes DHXY protocol, validator, mapper, executor and their contract/golden tests while Section 3 requires behavior those current files reject or cannot represent. Thus the proposed `TURN-39-I1` write set cannot satisfy its own acceptance matrix.
- The named test is not exact: current Cloud contains `com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`, not `NavigationServiceTurnContractTest`; "if absent merge into an existing test" is not a frozen path. No DHXY named test is assigned to verify double-click timing, unsupported key/text actions, scroll sign/count, action order, or boolean terminal mapping.
- repair condition: provide literal existing/create paths for every production and test file, collision-check all physical EOF owners, and map every acceptance row to a named test that can compile against the proposed write set. Do not open `TURN-39-I1` until this closes.

### P2-1 - Coverage arithmetic is false

- Replace all `27 -> 11` and `27 covered` claims with the source-true 26-value census and a one-row-per-enum table. This is independently required so omitted and phantom operations cannot be hidden by category arithmetic.

### Standing semantic decision

- `retire vs retain old InputSequences/executeInputBundle` remains the sole user-level semantic decision identified by the report. Parent does not decide it from this incomplete mapping. The card stays blocked; A must first repair the factual/protocol gaps so the runtime consequences of each option are exact.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-1 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-A-RETAINED MAPPING-NOT-FROZEN OLD-TYPES=26 DOUBLECLICK-INTERVAL-UNREPRESENTED KEYDOWN-KEYUP-TEXT-UNSUPPORTED WRITESET+TESTSET-INCOMPLETE NO-IMPLEMENTATION-CARD NO-MAVEN 2026-07-18T19:06:00-04:00 -->

## EXTERNAL-A REVIEW #1 REPAIR RE-DELIVERY - 2026-07-18T19:24:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW1-REPAIR-1906`（`0/2/1`）。三 finding 全部按返修条件闭合；新增取证（全只读）：DHXY `TurnInputStepExecutor` `7b2cb729`/264L、`TurnKeyMapper` `ffd4a04e`/35L、`TurnInputActionMapper` `73452788`/149L、`TurnProtocolValidator.requireInput/requireOnly(WAIT)` 规则、`BoundWindowKeyboardService.AltShortcut` 词表、Wubei 三 bundle 内容、全 Cloud src/main gap-族工厂调用 census。

### P2-1 修正：source-true 26 值 census
旧 `InputActionType`=26 值（此前 "27/12ALT+4click" 为分类算术错误——旧 click 族仅 CLICK_LEFT/CLICK_RIGHT/DOUBLE_RIGHT_CLICK 三值）。

### P1-1 修正：26 行逐 enum 精确表（fields/原子性/executor 路径/terminal；无条件短语）
**词表冻结**：`TurnKeyMapper`→`AltShortcut.backgroundHwndSupported`=13 个（Alt+1/2/4/5/6/8/Q/T/O/E/A/C/U）；无 ENTER/CTRL_U/裸键。**原子承载**：`executeMouseSequence(steps)`=mouse INPUT 首尾+内部正 WAIT→**单一不可分 input-queue 请求**；单 INPUT 步=单 queue 请求。**terminal 面**：completed→COMPLETED；`BACKGROUND_KEY_UNSUPPORTED`/`BACKGROUND_KEY_FAILED`/`INVALID_INPUT`→FAILED；stop/interrupt→STOPPED；不重发。

**A 组：今日精确可表达（每行=旧值→turn 形→DHXY 展开→terminal）**
1. `CLICK_LEFT(x,y,delay)`→INPUT/CLICK_LEFT{x,y,clickDelayMs=delay}→`clickLeft(x,y,delay)`（validator 允许两 timing；queueHoldMs>0 时 mapper 追加 sleep）→COMPLETED/FAILED/STOPPED。
2. `CLICK_RIGHT` 同 1 →CLICK_RIGHT。
3. `MOVE_MOUSE(x,y)`→INPUT/MOVE_MOUSE{x,y}（validator 禁全部额外字段）→`moveMouse`。
4. `DRAG_AND_DROP(x,y,eX,eY)`→INPUT/DRAG_LEFT{x,y,endX,endY}（mapper `requireInsideWindow` 端点校验）→`dragAndDrop`。
5. `SCROLL_DOWN(n)`→INPUT/SCROLL{x,y,scrollDelta=+n}→mapper 展开 `moveMouse(x,y)+scrollDown(n)`（**差异声明：turn 形要求显式 x,y 焦点；旧值无坐标——既有旧 caller 均在 scroll 前自带 click/move 焦点，故 call-site 等价由该点提供**）；delta 非零必需。
6. `SCROLL_UP(n)`→SCROLL{scrollDelta=-n}→`moveMouse+scrollUp(n)`。
7. `SLEEP(ms)`→WAIT{waitMs=ms}（validator 要求 **正值**；mouse 序列内=interior WAIT 保原子；负/零不可表达——**census：全部活跃 caller sleep 均为正**〔120/150/300〕）。
8-19. `PRESS_ALT_{1,2,4,6,8,T,O,E,Q,A,C,U}`（12 值）→INPUT/KEY_TAP{key="Alt+<k>"}→`TurnKeyMapper` 命中（12⊂13 词表）→`keyboardService.pressShortcut`→success→COMPLETED / 否则 BACKGROUND_KEY_FAILED（FAILED）。

**B 组：今日不可表达（5 族 7 值）——且经 census 全部为 DEAD-COMPAT（Cloud src/main input/ 包外零工厂调用）**
20. `DOUBLE_RIGHT_CLICK(x,y,delay,interval)`：turn DOUBLE_CLICK_RIGHT 被 validator **禁两 timing 字段**、DHXY mapper hardcode `doubleRightClick(x,y,0,0)`、`TurnInputSpec` 无 interval 字段→(delay,interval) 语义不可表达。潜在等价候选 `CLICK_RIGHT{delay}+WAIT{interval}+CLICK_RIGHT{delay}` 需 `InputAction.doubleRightClick` 内部展开的逐字节等价证明（本报告不猜测）。**活跃 caller=0**。
21-22. `HOLD_CTRL`/`RELEASE_CTRL`→KEY_DOWN/KEY_UP：executor 非 KEY_TAP 一律 `BACKGROUND_KEY_UNSUPPORTED`（无 HWND 背景按住/释放 API）。**活跃 caller=0**。
23. `PRESS_ENTER`→KEY_TAP{key="enter"}：词表无 ENTER→`BACKGROUND_KEY_UNSUPPORTED`。**活跃 caller=0**。
24. `PRESS_CTRL_U`：词表无→同上。**活跃 caller=0**。
25-26. `TYPE_TEXT_UNICODE`/`PASTE_TEXT`→TEXT_INPUT：executor unsupported；且 TEXT_INPUT 无两模式 discriminator。**活跃 caller=0**（`typeTextEnterAndScroll` 便利 API 无任何 production caller）。

**活跃 caller 词汇覆盖证明**（11 活跃站点全体）：Wubei :2156=`pressAltC+sleep(120)`；:2791/:4352=`moveMouse+sleep(120)+clickLeft(300)`；:2273/:1070 moveAndClickLeft=`moveMouse+sleep+clickLeft`；Nav 七块内=clickLeft(150)/pressAlt1/pressAlt2/moveMouse/close 序列。⇒ **活跃词汇 ⊆ A 组**，B 组 7 值仅存在于死便利/兼容面。

### P1-2 修正：字面写集/测试集（`TURN-39-I1` 可执行形）
- Modify `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java`（七块+inputProvider/gameStateUtil 站点）。
- Modify `dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`（4 站点→execute(TurnAction) mouse-sequence/KEY_TAP 形）。
- Delete（仅当决策=退役）`.../input/InputSequences.java`+`.../input/action/CloudInputActionMapper.java`+`.../input/action/InputAction.java`+`.../input/action/InputActionScope.java`。
- Modify test `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`（**正名既有文件**；七块 per-action 序列/order/WAIT 值/KEY_TAP 词/布尔四终态/cancel-stop 行）+`.../task/wubei/WubeiWholeTaskTurnContractTest.java`（4 站点等价行）+受 InputSequences 引用波及的 `DialogOptionTurnContractTest`/`PlayerStateTurnContractTest`/`SummonSkillTurnContractTest`（引用清理行）。
- DHXY 侧**零文件**（活跃词汇⊆A 组：无 executor/validator/mapper/词表/协议改动；此结论现为 source-proven 非假设）。验收矩阵行→named test 映射：七块×序列断言→NavigationTurnContractTest；布尔映射四终态→NavigationTurnContractTest；Wubei 4 站点→WubeiWholeTaskTurnContractTest；引用清零→三 contract test 编译本身。
- 碰撞核查（19:24 时点）：40B-C2 已 Review#2 PASSED（NavigationService 不在其写集）；C1/C3/38 族全 PASSED；无其它 physical EOF owner 持上述文件。依赖边保持：40B 家族债收口+`InputProvider`/`GameStateUtil` 补登记。

### 单点 PLAN-CONTRACT BLOCKED 决策（唯一，含精确运行时后果）
**退役 vs 保留 `InputSequences`/`CloudGameClient.executeInputBundle` 旧 bundle 路径**：
- (a) **退役（推荐）**：删 4 input 文件+死 B 组词汇随之消亡；11 活跃站点全部 per-action turn 化（A 组词汇零协议改动）；输入单路径；bundle wire 面（InputActionDto/executeInputBundle）成为无 caller 死面归 40B/41 清理链。
- (b) **保留**：Wubei 4 站点不动；仅修 Nav 七块；B 组死词汇与双输入路径长期共存，无活跃收益。
除此之外无语义分叉；映射表 A/B 组均为 source-frozen 事实，不随决策变化。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW1-REPAIR-REDELIVERED 26-ROW-TABLE-SOURCE-FROZEN VOCAB-13-ALT GROUP-B-7-VALUES-DEAD-COMPAT-PROVEN ACTIVE-VOCAB-SUBSET-A LITERAL-WRITESET+TESTSET DHXY-ZERO-FILES-PROVEN SINGLE-DECISION-RETIRE-VS-RETAIN ACK=PARENT-A-TURN39P1-REVIEW1-REPAIR-1906 AWAIT-REVIEW2 2026-07-18T19:24:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #2 - 2026-07-18 19:31 EDT

- verdict: `P0/P1/P2=0/1/2 / BLOCKED / REPAIR REQUIRED`; External A remains sole owner of this report-only card. No Java implementation card is opened.
- accepted repair evidence: the source-true enum count is 26; the 19 currently executable values and seven unsupported dead-compat values now match the current validator/mapper/executor; active callers use only the executable subset, so no DHXY protocol/executor expansion is required for the active cutover.

### P1-1 - The complete TURN-39 implementation/test boundary is still not frozen

- The proposed implementation write set covers only Navigation/Wubei and four compatibility files. It does not place those changes against the plan-fixed TURN-39 facade set (`CloudGameClient`, `CloudTaskServicePort`, `CloudTaskServiceExecutionContext`, `CloudTaskServiceMetadata`, `TurnGameClient`, `LegacyTaskExecutionTurnContextProvider`) or freeze which card owns each required modification/read-only boundary.
- The plan-fixed named gate `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java` does not exist and is omitted instead of being assigned as a literal create path. Three unrelated service contract tests containing source-guard string literals are not a substitute.
- The option-specific write sets conflict: the literal list always modifies Wubei, while the retain option explicitly leaves Wubei unchanged. The retire option also promises `InputSequences` reference cleanup but omits the production Javadocs in `FiveRingTaskV2` and `XiuluoTaskV2` that still name it.
- repair condition: provide two explicit option matrices (retire and retain), each with exact create/modify/delete/read-only paths, owner/dependency edges against the six fixed TURN-39 facade files and 44A, and one acceptance-test row per path. Include the mandatory literal create path for `OldFacadeRemovalContractTest`. Do not replace fixed TURN-39 scope with an underspecified `TURN-39-I1` subcard.

### P2-1 - Active caller count is still arithmetically false

- Production `InputSequences` call sites are Navigation 8 (`moveAndClickLeft` plus seven exclusive blocks) and Wubei 4, total 12, not 11. Correct the count and enumerate all 12 rows so the prepared-route call cannot disappear from acceptance coverage.

### P2-2 - Collision/status evidence is stale

- The 19:24 report says C2 was Review #2 passed. Its physically later canonical state at that time was Review #3 build-evidence repair required/owner C restored. The input write set remains disjoint, but the status claim must be corrected from original-card physical EOF evidence.

### Standing semantic decision

- `retire vs retain old InputSequences/executeInputBundle` remains the sole user-level decision. Parent will not ask the user to decide until both option write sets and runtime consequences are complete and internally consistent.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-2 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=2 OWNER-A-RETAINED ACTIVE-MAPPING-ACCEPTED COMPLETE-TURN39-WRITESET-MISSING OLDFACADE-TEST-MISSING OPTION-WRITESETS-CONFLICT ACTIVE-CALLERS=12-NOT-11 C2-STATUS-STALE NO-IMPLEMENTATION-CARD NO-MAVEN 2026-07-18T19:31:00-04:00 -->

## Parent Review #2 ACK / Repair Activity Audit - 2026-07-18 19:49 EDT

- A 19:42 STATUS EVENT exactly ACKed `PARENT-A-TURN39P1-REVIEW2-REPAIR-1938` and accepted all `0/1/2` findings.
- owner/status: A remains sole report owner; repair is active on the two exact option path matrices, fixed TURN-39/44A boundaries, all 12 callers and current C2 EOF. The accepted 26-value mapping is frozen. No canonical re-delivery exists yet.

<!-- TRUE_EOF: TURN-39P1 PARENT-REVIEW2-ACK+ACTIVITY OWNER-A REPORT-REPAIR-ACTIVE ACK=PARENT-A-TURN39P1-REVIEW2-REPAIR-1938 MAPPING-FROZEN OPTION-MATRICES+12-CALLERS+FIXED39-44A-BOUNDARIES-IN-PROGRESS NO-DELIVERY NO-MAVEN 2026-07-18T19:49:00-04:00 -->

## EXTERNAL-A REVIEW #2 REPAIR RE-DELIVERY - 2026-07-18T19:48:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW2-REPAIR-1938`（`0/1/2`；26 值映射/19+7 分组/active⊆executable/DHXY 零扩展已获接受，本次不再重复）。三 finding 闭合如下。

### P2-1 修正：active caller = 12 行全枚举
| # | 站点 | 形态 | 活跃词汇 |
|---|---|---|---|
| 1 | Nav:1070 `moveAndClickLeft`（preparedRouteDialog） | compat API | moveMouse+sleep+clickLeft |
| 2 | Nav:1450 exclusive（world-map prepare，2-attempt） | 复合宏 | clickLeft/moveMouse/observe |
| 3 | Nav:1674 exclusive（route-memory fast click） | 复合宏 | clickLeft(150)+intent+close |
| 4 | Nav:1968 exclusive（yellow cleanup） | observe-act-retry | pressAlt1+WAIT300+observe |
| 5 | Nav:2081 exclusive（queued route click） | 复合宏 | clickLeft(150)+intent+close |
| 6 | Nav:2218 exclusive（routePanelCleanup） | 复合宏 | close 序列 |
| 7 | Nav:2231 exclusive（routeDialogCloseX2） | 半 Cloud 化 | port 调用+moveMouse |
| 8 | Nav:2334 exclusive（closeMiniMapIfOpen） | observe-act-retry | pressAlt1+WAIT300+observe |
| 9 | Wubei:2156 `submitAndWait`（alt-c） | bundle | pressAltC+sleep(120) |
| 10 | Wubei:2273 `moveAndClickLeft` | compat API | moveMouse+sleep+clickLeft |
| 11 | Wubei:2791 `submitAndWait`（tracker-green-click） | bundle | moveMouse+sleep(120)+clickLeft(300) |
| 12 | Wubei:4352 `submitAndWait`（tracker-green-click） | bundle | 同 11 |
全部 ⊆ 已接受的 19 值可执行组；prepared-route 行（#1）纳入两套 matrix 验收。

### P2-2 修正：C2 碰撞状态以原卡 physical EOF 取证
`2026-07-18-turn-card-TURN-40B-C2.md` 当前 physical EOF=`PARENT-SOURCE+TEST-SOURCE-REVIEW-4 PASSED / P0=0 P1=0 P2=0 / OWNER-C-RELEASED`（此前 19:24 报告引用"Review#2 passed"时其当时 EOF 实为 Review#3 build-evidence repair——引用过期，特此更正并以原卡 EOF 为唯一证据源）。input 写集与 C2 写集不相交结论不变。

### P1-1 修正：两套完整 option matrix（置于计划固定 TURN-39 六 facade 文件+44A 边界；不设 TURN-39-I1 子卡，实施即固定 TURN-39 由单一 integration owner 执行）

**共同底座（两选项相同）**
| path | 处置 | owner/依赖边 | 验收行（named test） |
|---|---|---|---|
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java` | READ-ONLY（input 走既有 execute()/executeMouseSequence 形） | TURN-39 固定 facade；39 integration owner | OldFacadeRemovalContractTest：facade 面零新旧耦合 |
| `.../turn/client/LegacyTaskExecutionTurnContextProvider.java` | READ-ONLY | 同上 | 同上 |
| `.../remote/CloudTaskServicePort.java` `.../remote/CloudTaskServiceExecutionContext.java` `.../remote/CloudTaskServiceMetadata.java` | Modify（TURN-39 固定收口 scope，与 input 决策无关的 facade 收口） | 39 integration owner；44A 前字节保留原则适用于被替換旧面 | OldFacadeRemovalContractTest：typed-only 面断言 |
| **Create** `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java` | Create（计划固定必建门） | 39 integration owner | 本文件即门：per-option 断言见下 |
| `src/main/java/com/bot/dhxy/service/NavigationService.java` | Modify（12 行表 #2-#8 七块 per-action/executeMouseSequence 化；observe→typed fact；cancel→既有 stop-bridge） | 39 owner；依赖=40B 已收口（C1/C2/C3 PASSED）+`InputProvider`/`GameStateUtil` 债登记 | `service/NavigationTurnContractTest.java`（Modify）：七块逐序列/order/WAIT 值/KEY_TAP 词/布尔四终态/cancel-stop |

**选项 (a) RETIRE（推荐）增量**
| path | 处置 | 验收行 |
|---|---|---|
| `.../service/NavigationService.java`（含 #1 行） | Modify：`moveAndClickLeft`:1070 一并 per-action 化 | NavigationTurnContractTest：prepared-route 行 |
| `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` | Modify：#9-#12 四行→execute(TurnAction) mouse-sequence/KEY_TAP | `task/wubei/WubeiWholeTaskTurnContractTest.java`（Modify）：四行等价断言 |
| `.../input/InputSequences.java` `.../input/action/CloudInputActionMapper.java` `.../input/action/InputAction.java` `.../input/action/InputActionScope.java` | **REFERENCE-ZERO in 39**（production/test 引用清零）；**物理 DELETE 归 TURN-44A**（字节保留原则，38M 先例） | OldFacadeRemovalContractTest：断言 production 零 `InputSequences`/`executeInputBundle` 引用 |
| `.../remote/CloudGameClient.java` | Modify（39 固定 scope；input-bundle wire 面成为零 caller 死面，随 44A 删） | OldFacadeRemovalContractTest：零 executeInputBundle production caller |
| `.../task/wuhuan/FiveRingTaskV2.java`(:2743 Javadoc) `.../task/xiuluo/XiuluoTaskV2.java`(:1655 Javadoc) | Modify（仅 Javadoc 提及清理） | 引用 grep 行=0（含 Javadoc） |
| `.../service/DialogOptionTurnContractTest.java` `.../service/PlayerStateTurnContractTest.java` `.../service/SummonSkillTurnContractTest.java` | Modify（InputSequences 字符串/引用清理） | 三文件编译+零引用 |

**选项 (b) RETAIN 增量**
| path | 处置 | 验收行 |
|---|---|---|
| `.../task/wubei/WubeiTask.java` | **READ-ONLY**（#9-#12 保持 compat；修正此前矛盾：retain 下 Wubei 明确零字节） | WubeiWholeTaskTurnContractTest 零改动即验收 |
| Nav #1 行（moveAndClickLeft:1070） | READ-ONLY（合法 compat 保留） | OldFacadeRemovalContractTest retain 形断言 |
| 4 个 input 文件+`.../remote/CloudGameClient.java` bundle 面 | READ-ONLY 保留（死 B 组 7 值词汇与双输入路径长期共存） | OldFacadeRemovalContractTest：断言 consumer 集冻结=准确的 {Wubei 4+Nav:1070}，禁新增 |
| FiveRing/Xiuluo Javadoc | READ-ONLY（提及合法） | — |

两套 matrix 逐 path 自洽；44A 依赖边：两选项下旧 facade/input 字节删除均归 44A（39 只做引用收敛）。碰撞核查（19:48 时点，各原卡 physical EOF）：40B-C1/C2/C3 全 PASSED/owner-released；38 族 CLOSED；无其它 owner 持上述 path。**单点决策保持不变**：retire(推荐) vs retain，两套精确后果如上，供父级/用户裁决。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW2-REPAIR-REDELIVERED 12-CALLER-ROWS C2-EOF-CORRECTED TWO-OPTION-MATRICES-COMPLETE SIX-FACADE+44A-EDGES OLDFACADE-TEST-LITERAL-CREATE-PATH NO-SUBCARD ACK=PARENT-A-TURN39P1-REVIEW2-REPAIR-1938 AWAIT-REVIEW3 2026-07-18T19:48:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #3 - 2026-07-18 19:54 EDT

- verdict: `P0/P1/P2=0/2/1 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The 12-row caller census and current C2 EOF correction are accepted. No Java implementation card is opened.

### P1-1 - RETAIN is not a conforming TURN-39 implementation option

- The approved plan fixes TURN-39 completion as business code seeing typed capture/input/local-service only (`plan:1355`) and fixes `OldFacadeRemovalContractTest` to prove every active caller depends only on `TurnGameClient/context` with old-facade zero reference (`plan:2247`).
- RETAIN leaves five active old consumers (`Wubei` four plus Navigation `:1070`) and a live `CloudGameClient.executeInputBundle` path. Its proposed test freezes those violations instead of satisfying the gate. This is not an unresolved business-semantic choice; it is a plan-contract contradiction.
- parent adjudication: TURN-39 must retire all active `InputSequences/executeInputBundle` consumers. Physical old facade/input bytes remain until 44A, but their active production reference count must be zero in TURN-39. No user decision is required unless the user explicitly asks to change the approved TURN-39 outcome.

### P1-2 - RETIRE test/write matrix still contains a false cleanup

- `DialogOptionTurnContractTest`, `PlayerStateTurnContractTest`, and `SummonSkillTurnContractTest` contain useful source guards asserting their already-migrated services do not regress to `InputSequences`; they do not import or consume the compatibility class. Removing those string guards would weaken existing tests and is not required by deleting active consumers.
- `OldFacadeRemovalContractTest` must distinguish definitions retained for 44A from active callers: assert zero active business imports/fields/calls to `InputSequences`, `CloudGameClient`, and `executeInputBundle`, while allowing the dead definitions themselves to remain byte-preserved until 44A. A repository-wide raw string count of zero is impossible under the stated 44A boundary.
- repair condition: collapse to one conforming RETIRE matrix. Mark the three existing service contract tests READ-ONLY, assign the exact active-caller source files and Javadoc cleanup, and define `OldFacadeRemovalContractTest` against active caller/import/field/call tokens rather than retained definitions.

### P2-1 - Fixed facade scope wording remains inconsistent

- The report calls the common table the fixed six-file scope while putting `CloudGameClient` only in option-specific rows and treating its action differently by option. With RETAIN rejected, list all six dynamic-plan paths once with exact `MODIFY` or `READ-ONLY` disposition, then separately list active input caller files and 44A-retained definitions. Do not mix fixed facade scope with conditional alternatives.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-3 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-A-RETAINED CALLER12+C2-EOF-ACCEPTED RETAIN-NONCONFORMING RETIRE-REQUIRED-BY-PLAN ACTIVE-OLD-FACADE-REFS-MUST-ZERO PHYSICAL-DEFINITIONS-RETAIN-TO-44A SERVICE-SOURCE-GUARDS-READONLY FIXED-SIX-FACADE-DISPOSITIONS-REQUIRED NO-USER-DECISION NO-IMPLEMENTATION-CARD NO-MAVEN 2026-07-18T19:54:00-04:00 -->

## EXTERNAL-A REVIEW #3 REPAIR RE-DELIVERY - 2026-07-18T20:00:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW3-REPAIR-1954`（`0/2/1`；12 行 census+C2 EOF 更正已接受）。三 finding 闭合；**RETAIN 选项撤回**——收悉裁定：plan:1355 完成态+plan:2247 `OldFacadeRemovalContractTest` 门已定 TURN-39 结局，retire 为计划要求非用户决策（除非用户明确要求变更既批 TURN-39 结局）；此前"单点用户决策"框架作废。

### 唯一 conforming RETIRE matrix

**一、固定六 facade path（一次列出，精确处置）**
| path | 处置 |
|---|---|
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java` | MODIFY（39 固定 scope；active input-bundle 消费清零后其 bundle 面为死定义，字节保留至 44A） |
| `.../remote/CloudTaskServicePort.java` | MODIFY（39 固定收口） |
| `.../remote/CloudTaskServiceExecutionContext.java` | MODIFY（39 固定收口） |
| `.../remote/CloudTaskServiceMetadata.java` | MODIFY（39 固定收口） |
| `.../turn/client/TurnGameClient.java` | READ-ONLY（已存在；input 走既有 execute()/executeMouseSequence 形，零新方法） |
| `.../turn/client/LegacyTaskExecutionTurnContextProvider.java` | READ-ONLY |

**二、active caller 源文件（12 行表全消，逐 path+验收）**
| path | 处置 | 验收（named test） |
|---|---|---|
| `src/main/java/com/bot/dhxy/service/NavigationService.java` | MODIFY：#1-#8 行（七 exclusive 块 per-action/executeMouseSequence 化+`moveAndClickLeft`:1070 per-action 化；observe→typed fact；cancel→既有 stop-bridge；删 `inputSequences` field/import） | `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`（MODIFY）：八行逐序列/order/WAIT 值/KEY_TAP 词/布尔四终态/cancel-stop/prepared-route 行 |
| `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` | MODIFY：#9-#12 行（bundle→mouse-sequence/KEY_TAP；删 field/import） | `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`（MODIFY）：四行等价断言 |
| `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`（:2743 Javadoc 提及） | MODIFY（仅 Javadoc 清理） | active-token 断言覆盖（见门测试） |
| `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`（:1655 Javadoc 提及） | MODIFY（仅 Javadoc 清理） | 同上 |

**三、44A-retained 定义（字节保留，39 内仅 active 引用清零）**
`.../input/InputSequences.java`、`.../input/action/CloudInputActionMapper.java`、`.../input/action/InputAction.java`、`.../input/action/InputActionScope.java`、`CloudGameClient` bundle wire 面（`executeInputBundle`/`InputActionDto` 路径）——全部 READ-ONLY-BYTES，物理 DELETE 归 TURN-44A。

**四、必建门（字面 Create）**
`src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java`（Create）：**按 active-token 断言**——零 active 业务 import/field/调用 指向 `InputSequences`/`CloudGameClient`/`executeInputBundle`（token=import 语句/字段声明/方法调用，逐 production 业务文件枚举断言）；**明确允许** 44A-retained 定义文件自身字节存在（不做 repo 级裸字符串零计数——在 44A 边界下不可能且非门语义）；每 active caller 一行断言其仅依赖 `TurnGameClient`/context。

**五、READ-ONLY 更正（P1-2）**
`DialogOptionTurnContractTest.java`/`PlayerStateTurnContractTest.java`/`SummonSkillTurnContractTest.java`=**READ-ONLY**——三文件内 `InputSequences` 字符串为**防回归 source guard**（断言已迁服务不倒退），非消费引用；此前"引用清理"行为误判，撤回；保留即增强门。

依赖边不变：40B 家族已收口（C1/C2/C3 原卡 EOF PASSED）；`InputProvider`/`GameStateUtil` 债须登记（44A 或 39 合同）；44A 持全部物理删除。碰撞核查（20:00 时点各原卡 EOF）：无其它 owner 持上述 path。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW3-REPAIR-REDELIVERED RETAIN-WITHDRAWN SINGLE-RETIRE-MATRIX SIX-FACADE-DISPOSITIONS-ONCE ACTIVE-TOKEN-GATE-DEFINED SERVICE-GUARDS-READONLY-CORRECTED 44A-RETAINED-DEFS-SEPARATED ACK=PARENT-A-TURN39P1-REVIEW3-REPAIR-1954 AWAIT-REVIEW4 2026-07-18T20:00:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #4 - 2026-07-18 20:07 EDT

- verdict: `P0/P1/P2=0/2/1 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The
  RETAIN withdrawal, 12 direct `InputSequences` callers, six facade dispositions, three READ-ONLY service
  guards and active-token-vs-44A-definition distinction are accepted. No TURN-39 implementation card is opened.

### P1-1 - The Navigation callback transitive closure is still not assigned

- The re-delivery itself ends with ``InputProvider`/`GameStateUtil` debt must be registered (44A or 39)``.
  That is an unresolved owner/write-set choice, not a frozen implementation contract.
- Current production proves the seven `submitExclusiveAndWait` callbacks transitively call direct-input and
  observation helpers: `prepareWorldMapSearchResultsDirect` (`NavigationService:1450`),
  `inputProvider.clickLeft` + `gameStateUtil.recordMovementIntent` + `closeMapSearchInputAfterRouteClick`
  (`:1674` and `:2081`), `cleanupYellowDestinationRouteAfterCoordinateClickDirect` (`:1968`), X2 cleanup plus
  direct mouse movement (`:2218`/`:2231`), and mini-map observe/Alt+1/re-observe (`:2334`). Those paths further
  cross `CloudUiCleanerPort`, template/coordinate observation, `BoundWindowKeyboardService`, waits,
  cancellation and movement-intent publication.
- `observe -> typed fact; cancel -> existing stop-bridge` does not identify the existing `TurnStep`/local-service
  symbol used for each helper, its exact action ordering, or which source owner removes each direct collaborator.
- repair condition: expand all eight Navigation rows through their complete helper call graph. For every direct
  input, observation, cancellation, cleanup, wait and movement-intent edge, name the existing typed turn/local
  seam, exact source disposition, owner card and named assertion. If an edge has no existing representable seam,
  return the single precise plan-contract blocker. No Supplier shim, constant result, duplicated detector, second
  queue/store or business-order change is allowed.

### P1-2 - Four required facade modifications are still non-executable placeholders

- `CloudGameClient`, `CloudTaskServicePort`, `CloudTaskServiceExecutionContext` and
  `CloudTaskServiceMetadata` are each marked only `MODIFY (fixed close scope)`. No method/field/import/Javadoc
  delta or acceptance assertion is stated.
- `CloudGameClient` is additionally declared MODIFY while its `executeInputBundle`/`InputActionDto` wire face
  must remain READ-ONLY-BYTES until 44A. Those statements can coexist only if the exact non-bundle bytes to
  change are named; the report does not name them. The other three files have the same unspecified-close issue.
- repair condition: for each facade file, list every exact symbol that changes and the resulting dependency
  edge/test assertion, or prove it is byte-read-only and correct the disposition. `MODIFY` cannot merely inherit
  an old plan heading while the implementation delta remains unknown.

### P2-1 - The active-token gate needs an explicit retained-infrastructure allowlist

- Current source still has old-facade references outside the 12 business input sites, including
  `TaskExecutionContext` imports/fields/accessors for `CloudGameClient`, `CloudTaskServicePort`,
  `CloudTaskServiceExecutionContext` and `CloudTaskServiceMetadata`, plus old assembly construction. Some may be
  legitimate 44A-retained SCC definitions, but the proposed test only says it enumerates "active business files"
  and does not freeze which production files are excluded or why.
- repair condition: make `OldFacadeRemovalContractTest` enumerate both sets literally: every active business
  caller that must be zero, and every infrastructure/definition file temporarily allowed through 44A with its
  owner edge. This prevents a live old caller from being hidden by an undefined filter while still avoiding a
  repository-wide raw-string-zero assertion.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-4 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-A-RETAINED RETAIN-WITHDRAWAL+CALLER12+SIX-DISPOSITIONS+SERVICE-GUARDS-ACCEPTED NAVIGATION-TRANSITIVE-CLOSURE-MISSING FACADE-MODIFY-SYMBOLS-MISSING ACTIVE-TOKEN-ALLOWLIST-MISSING NO-IMPLEMENTATION-CARD NO-MAVEN 2026-07-18T20:07:00-04:00 -->

## Parent Review #4 ACK / Repair Activity Audit - 2026-07-18 20:15 EDT

- External A 20:12 STATUS EVENT exactly ACKed `PARENT-A-TURN39P1-REVIEW4-REPAIR-2007` and accepted all
  `0/2/1` findings. Communication is normal.
- A remains sole report owner with Review #4 repair active. It has begun the six-helper plus transitive-callee
  call-graph audit; no canonical repair delivery exists and no Java/test source changed for this report.
- TURN-39 remains `PLAN-CONTRACT BLOCKED`; no implementation card is open. The relevant Navigation/Wubei and
  four facade SHA/mtime values are unchanged. No Maven/runtime/input was run.

<!-- TRUE_EOF: TURN-39P1 PARENT-REVIEW4-ACK+ACTIVITY OWNER-A REPORT-REPAIR-ACTIVE ACK=PARENT-A-TURN39P1-REVIEW4-REPAIR-2007 COMMUNICATION-NORMAL CALLGRAPH-FORENSICS NO-DELIVERY SOURCE-SHAS-UNCHANGED NO-MAVEN 2026-07-18T20:15:00-04:00 -->

## Parent Plan-Contract Blocker Audit - 2026-07-18 20:20 EDT

- External A's 20:17 forensic event is source-true. `NavigationService.prepareWorldMapSearchResultsDirect`
  contains an active stale-route-panel fallback that performs click, `pressCtrlA()`,
  `typeTextUnicode(targetMapName)`, wait and search click in order.
- The mirrored turn protocol and validator can encode `KEY_DOWN`, `KEY_UP` and `TEXT_INPUT`, but DHXY
  `TurnInputStepExecutor` intentionally accepts only background-validated `KEY_TAP`; every other key/text action
  closes with `BACKGROUND_KEY_UNSUPPORTED`. TURN-09 explicitly requires this result when the existing HWND
  keyboard API cannot express an action and forbids widening scope or using foreground fallback.
- This edge cannot be closed by a documentation-only write-set repair. A constant result, second queue/local
  service, duplicated input path, or skipped stale-panel fallback would violate the approved contract or baseline.
- unique pending user decision: whether to authorize a separate capability change for exact-window, globally
  serialized foreground keyboard delivery of the existing Ctrl+A plus Unicode-text sequence. Until explicit
  approval, TURN-39 remains PLAN-CONTRACT BLOCKED. A must finish the remaining call graph/facade/allowlist audit
  so this stays the only blocker; no implementation card is opened.

<!-- TRUE_EOF: TURN-39P1 PARENT-PLAN-CONTRACT-BLOCKER CONFIRMED ACTIVE-CTRLA+TEXTINPUT TURN09-BACKGROUND-KEY-UNSUPPORTED FOREGROUND-FALLBACK-FORBIDDEN UNIQUE-USER-DECISION=EXACT-WINDOW-SERIALIZED-FOREGROUND-KEYBOARD OWNER-A-REPORT-REPAIR-ACTIVE FINISH-TRANSITIVE-AUDIT NO-TURN39-READY NO-MAVEN 2026-07-18T20:20:00-04:00 -->

## Parent Blocker ACK / Audit-Complete Snapshot - 2026-07-18 20:25 EDT

- A 20:22 exactly ACKed `PARENT-A-TURN39P1-TEXTINPUT-BLOCKER-CONFIRMED-2020`; communication is normal and
  the report owner remains A.
- A reports the remaining forensic audit complete: the normal world-map branch also uses Unicode text plus
  Enter, and `pressAlt1ForMiniMap` contains a focused-input fallback. These are the same foreground-keyboard
  capability decision, not a second semantic branch.
- Within TURN-39's input scope, the four remote facade files are now proven byte-read-only; their input references
  belong to the 44A-retained bundle wire face. The active-zero set and 44A-retained infrastructure set have been
  enumerated for the new gate. These are status evidence only until the canonical report re-delivery lands.
- A is assembling Repair #4. No delivery/source/build change exists; TURN-39 remains blocked and unopened.

<!-- TRUE_EOF: TURN-39P1 PARENT-BLOCKER-ACK+AUDIT-COMPLETE ACK=PARENT-A-TURN39P1-TEXTINPUT-BLOCKER-CONFIRMED-2020 OWNER-A ASSEMBLING-REPAIR4 UNIQUE-DECISION=FOREGROUND-KEYBOARD FACADE4-BYTE-READONLY-PROVEN DUAL-ALLOWLIST-ENUMERATED NO-DELIVERY NO-SOURCE+BUILD-CHANGE 2026-07-18T20:25:00-04:00 -->

## EXTERNAL-A REVIEW #4 REPAIR RE-DELIVERY - 2026-07-18T20:28:00-04:00

ACK 重申 `PARENT-A-TURN39P1-REVIEW4-REPAIR-2007` + `PARENT-A-TURN39P1-TEXTINPUT-BLOCKER-CONFIRMED-2020`。三 finding 闭合。

### P1-1：八行完整调用图逐边 seam 表（全部 helper 体已实读）

**行1 Nav:1070 moveAndClickLeft**：moveMouse+sleep+clickLeft→executeMouseSequence 单请求。断言：NavigationTurnContractTest prepared-route 行。

**行2 Nav:1450 prepare（world-map search，2-attempt）**——完整边表：
| 边 | 现行实现 | turn seam / 处置 |
|---|---|---|
| cancel 前检 | `InputActionScope.isCancelled` | 既有 stop-bridge（40B-C2 落地）checkpoint；owner=39 |
| 世界地图开合观察 | `isWorldMapTitleVisible`=coordinateHelper.findImageAbsoluteCoordinate(title 模板) | CAPTURE+MATCH_TEMPLATE 步（既有 TurnStepType）；owner=39 |
| Alt+2 开图 | `inputProvider.pressAlt2` | KEY_TAP{Alt+2}（词表 ✓）；owner=39 |
| WAIT 500/250/200 | `TaskSleep.sleep` | WAIT 步（正值 ✓）；owner=39 |
| 寻路按钮模板找点 | `coordinateHelper.findImageAbsoluteCoordinate(XUNLU)` | CAPTURE+MATCH_TEMPLATE→坐标；owner=39 |
| stale 面板 x2 关闭 | `cloudUiCleanerPort.closeMapSearchInputByX2` | 既有 Cloud port 调用保留；owner=39（零改） |
| 点击输入框/搜索按钮/寻路点 | `inputProvider.clickLeft(…,80/120)` | CLICK_LEFT{clickDelayMs}；owner=39 |
| **全选+输入目标名+回车** | **`pressCtrlA()`+`typeTextUnicode(targetMapName)`+`pressEnter()`（两分支均有 type；正常分支含 Enter）** | **无既有可表达 seam——TURN-09 capability blocker（父级 2020 已确认）**：executor 仅 KEY_TAP（13-Alt 词表）、TEXT_INPUT=BACKGROUND_KEY_UNSUPPORTED |
| 滚动结果列表 | `scrollWorldMapSearchResultsToBottomDirect(scrollFocus)` | SCROLL{x,y,delta}（焦点显式）；owner=39 |

**行3/行5 Nav:1674/:2081 route click**：cancel→stop-bridge；`inputProvider.clickLeft(150)`→CLICK_LEFT{150}；`gameStateUtil.recordMovementIntent`→既有 `WHOLE_TASK_MOVEMENT_RECORD` typed op（35/36 PASSED 先例）；`closeMapSearchInputAfterRouteClick`→见行6。断言：route click 序列行。

**行4 Nav:1968 cleanupYellow**：`isMiniMapPanelVisible(debug)`→CAPTURE+MATCH_TEMPLATE（checked/unchecked 双模板 0.95；**debug 盘快照变体在 turn 化中弃置**——turn capture 即证据，无本地盘路径）；`pressAlt1ForMiniMap`→KEY_TAP{Alt+1}+**语义差异声明：现行含 focused-input 前台 fallback 分支，turn 执行器按设计无前台 fallback——迁移后 fallback 分支消亡，terminalFailure/未尝试→FAILED**（此为 turn 原则内固有差异非新语义，入验收断言）；WAIT300；re-observe；retry×1（Cloud 持顺序）；else `pressAlt2`+WAIT250。断言：observe-act-retry 行。

**行6 Nav:2218 closeRouteSearchPanelQueued→closeMapSearchInputAfterRouteClick**：`cloudUiCleanerPort.closeMapSearchInputByX2`（保留）+`moveMouseAwayFromRouteCloseDirect`=tracker base+random 点+`inputProvider.moveMouse`→**windowRect base+内化 random（C3 randomize 先例）+MOVE_MOUSE**。断言：cleanup 行。

**行7 Nav:2231 closeMapSearchInputAfterRouteDialog**：port 调用保留+行6 同款 moveMouseAway。断言：X2-close 行。

**行8 Nav:2334 closeMiniMapIfOpen**：行4 同族（observe→Alt1→WAIT300→re-observe→retry×1，无 debug 变体）。断言：同行4 模式。

**Wubei 行9-12**：已冻结（bundle→executeMouseSequence/KEY_TAP）。
**owner 冻结（P1-1 悬置消除）**：`InputProvider`/`GameStateUtil`/`CoordinateHelper`/`tracker(GameClientTracker)`/`windowScopedTempPath` 的 **Navigation 侧站点消解=TURN-39 卡内职责**（上表逐边给出替代 seam）；其**物理定义字节**全部保留至 44A（与 4 input 文件同列 44A-retained 定义集）。不再"39 或 44A"两可。

### P1-2：四 facade 文件处置更正（byte-read-only 证明）
input 范畴符号 census：`CloudGameClient` 169L/blob `9b336ee4`/6 inputRefs、`CloudTaskServicePort` 328L/`da0dcc47`/13 inputRefs——两者 input 相关符号全部属 `executeInputBundle`/`InputActionDto` bundle wire 面=44A-retained；`CloudTaskServiceExecutionContext` 227L/`60ab2a9a`/**0** inputRefs、`CloudTaskServiceMetadata` 59L/`18965ced`/**0** inputRefs。⇒ **本报告冻结的 input-bridge 范畴内四文件全部 READ-ONLY-BYTES（处置由 MODIFY 更正）**；TURN-39 更广收口若需触碰其非 input 符号，属 39 integration owner 在其原卡内另行冻结之事，不由本 input 合同预支。

### P2-1：门测试双集字面枚举
`OldFacadeRemovalContractTest.java`（Create，字面路径不变）断言：
- **active-zero 集（逐文件逐 token）**：`service/NavigationService.java`、`task/wubei/WubeiTask.java`=零 `InputSequences`/`CloudGameClient`/`executeInputBundle` import/field/调用；`task/wuhuan/FiveRingTaskV2.java`、`task/xiuluo/XiuluoTaskV2.java`=零上述 Javadoc 提及；每 active caller 一行断言仅依赖 `TurnGameClient`/context。
- **44A-retained 允许集（逐文件+owner 边）**：`input/InputSequences.java`、`input/action/{CloudInputActionMapper,InputAction,InputActionScope}.java`、`remote/CloudGameClient.java`、`remote/CloudTaskServicePort.java`（bundle 面）、`remote/CloudTaskServiceExecutionContext.java`、`remote/CloudTaskServiceMetadata.java`、`runner/context/TaskExecutionContext.java`（旧 facade accessor）、`remote/CloudTaskRunCommandExecutor.java`、`remote/RemoteGameClientPort.java`、`remote/RemoteGameCommandBroker.java`——owner 边=TURN-44/44A plan Delete set；三 service contract test（DialogOption/PlayerState/SummonSkill）=READ-ONLY source guard。无未定义过滤器；不做 repo 级裸字符串零计数。

### 单一用户决策（父级 2020 指示框架）
**TURN-09 capability blocker**：world-map search 文本输入路径（Ctrl+A 全选+`TYPE_TEXT_UNICODE(目标图名)`+Enter，行2 单边）无既有可表达 turn seam。选项由父级/用户裁：(i) 扩 TURN-09 input 能力（TEXT_INPUT/chord 的 HWND 背景实现+validator/spec/executor/词表传递写集）后 39 全量 retire；(ii) 承认该单边为 39 的显式残留（该 callback 保持 InputSequences 直至能力落地，门测试为其开列唯一例外行），其余 11 行照常 retire。除此无其它 blocker。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW4-REPAIR-REDELIVERED 8-ROW-FULL-EDGE-TABLE TURN09-BLOCKER-SINGLE-EDGE FOCUSED-FALLBACK-DIFF-DECLARED DEBUG-SNAPSHOT-DROPPED FACADE-4-READONLY-PROVEN OWNER-FREEZE-NAV-SIDE-39 ALLOWLIST-TWO-SETS-LITERAL SINGLE-USER-DECISION-TURN09 ACK=REVIEW4-2007+BLOCKER-2020 AWAIT-REVIEW5 2026-07-18T20:28:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #5 - 2026-07-18 20:35 EDT

- verdict: `P0/P1/P2=0/2/1 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The four
  input-scope facade byte proofs, the literal active-zero set, and identification of the foreground-keyboard
  capability blocker are accepted. TURN-39 remains unopened.

### P1-1 - The claimed complete Navigation closure still drops live helper edges and changes fallback semantics

- `prepareWorldMapSearchResultsDirect` calls `closeWorldMapAfterXunluDirect` at current
  `NavigationService:1600`; that helper performs `pressAlt2()` then `TaskSleep.sleep(250)` at `:1845-1846`.
  The eight-row table does not name this helper/edge or its exact position after the xunlu click.
- `scrollWorldMapSearchResultsToBottomDirect` is not one `SCROLL{x,y,delta}` edge. Current `:2314-2326`
  performs a focus `clickLeft(...,50)`, repeated `scrollDown` plus per-iteration waits/cancellation checks, then a
  settle wait. The report omits that action order, retry count and wait assertions, so Review #4's full-helper
  acceptance condition is still unmet.
- `pressAlt1ForMiniMap` currently falls back to `inputProvider.pressAlt1()` when HWND delivery is not attempted or
  fails non-terminally (`:2356-2376`). Declaring that branch to "disappear" and map to FAILED is an unapproved
  baseline behavior change, not an inherent migration difference. The same user capability decision must preserve
  this exact-window, globally serialized fallback behavior as well as Ctrl+A/Unicode/Enter; rejection leaves the
  card blocked.
- repair condition: add the omitted helper/scroll edges with exact ordering, counts and named assertions; state one
  capability contract that preserves every baseline foreground-keyboard path. Do not drop the fallback, duplicate
  it in a second queue/service, or alter retry/cancellation order.

### P1-2 - The proposed residual exception is not a conforming user option

- Option (ii) keeps the row-2 callback on `InputSequences` and weakens `OldFacadeRemovalContractTest` with one
  active exception. That is the same nonconforming RETAIN shape rejected in Review #3: the fixed TURN-39 gate at
  plan `:1355`/`:2253` requires every active caller to depend only on `TurnGameClient/context` and old-facade active
  references to be zero.
- The only safe binary decision is: approve the separate exact-window/global-serialization keyboard capability and
  then implement full TURN-39 retirement, or reject it and keep TURN-39 `PLAN-CONTRACT BLOCKED`. Rejection does not
  authorize a partial implementation or exception row.

### P2-1 - The retained allowlist owner edges are not exact

- The report labels the retained set as ten files but literally lists twelve. More importantly, one umbrella
  `TURN-44/44A plan Delete set` owner is false: `TaskExecutionContext` is a surviving external consumer that must be
  modified when its legacy delegate/accessors are removed, not deleted as part of the 17-file SCC;
  `CloudTaskServiceMetadata` is explicitly outside that SCC and requires its own final owner; the 44A manifest does
  place `RemoteGameCommandBroker` in the 17-file cohort despite the older summary wording.
- repair condition: give every retained path its exact disposition (`DELETE`, `MODIFY`, or later residual owner),
  exact card/cohort, and guard assertion. Do not use a shared owner label that would hide a surviving external
  consumer or a file outside the frozen SCC.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-5 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-A-RETAINED ACCEPTED=FACADE4-INPUT-BYTE-PROOF+ACTIVE-ZERO+KEYBOARD-BLOCKER NAV-CLOSURE-STILL-MISSING CLOSE-WORLD-MAP+SCROLL-INTERNALS FOCUSED-FALLBACK-REMOVAL-UNAPPROVED RESIDUAL-EXCEPTION-NONCONFORMING RETAINED-OWNER-EDGES-INEXACT NO-TURN39-READY NO-MAVEN 2026-07-18T20:35:00-04:00 -->

## Parent Communication Snapshot - 2026-07-18 20:40 EDT

- `PARENT-A-TURN39P1-REVIEW5-REPAIR-2035` has no named ACK for two consecutive parent audits;
  `COMMUNICATION_STALE` is now set. The original-card owner remains External A with Review #5 repair required.
- The last A event is 20:35, so the separate ten-minute no-event/no-source-change `ACTIVE_STALE` threshold is not
  yet met. No canonical repair delivery or source/build movement exists.

<!-- TRUE_EOF: TURN-39P1 PARENT-COMMUNICATION-SNAPSHOT OWNER-A-RETAINED REVIEW5-REPAIR-REQUIRED ACK=PARENT-A-TURN39P1-REVIEW5-REPAIR-2035-MISSING-TWO-ROUNDS COMMUNICATION_STALE ACTIVE_STALE-NOT-YET NO-DELIVERY NO-SOURCE+BUILD-CHANGE 2026-07-18T20:40:00-04:00 -->

## Parent Communication Recovery / Repair Activity Snapshot - 2026-07-18 20:50 EDT

- A's 20:44 STATUS EVENT exactly ACKed `PARENT-A-TURN39P1-REVIEW5-REPAIR-2035` and accepted all Review #5
  findings. `COMMUNICATION_STALE` is cleared; owner A remains active on the same report repair.
- A has completed the omitted-helper/scroll/fallback/owner-edge forensics and is assembling Repair #5. There is no
  canonical re-delivery or source/build movement yet. `PARENT-A-TURN39P1-COMMUNICATION-STALE-2040` was not named
  in that event and remains pending round 1; communication is otherwise normal.

<!-- TRUE_EOF: TURN-39P1 PARENT-COMMUNICATION-RECOVERY ACK=PARENT-A-TURN39P1-REVIEW5-REPAIR-2035 OWNER-A-REPAIR5-ACTIVE COMMUNICATION-NORMAL FORENSICS-COMPLETE ASSEMBLING-REPAIR5 ACK=PARENT-A-TURN39P1-COMMUNICATION-STALE-2040-PENDING-ROUND1 NO-DELIVERY NO-SOURCE+BUILD-CHANGE 2026-07-18T20:50:00-04:00 -->

## EXTERNAL-A REVIEW #5 REPAIR RE-DELIVERY - 2026-07-18T20:50:00-04:00

ACK 重申 `PARENT-A-TURN39P1-REVIEW5-REPAIR-2035`。三 finding 闭合。

### P1-1：行2 补全两边 + fallback 保全（撤回"消亡"声明）
**行2 prepare 边表补充（精确顺序）**：
- xunlu click（`clickLeft(xunluPoint,120)`）→sleep250 →**`closeWorldMapAfterXunluDirect`（:1600 调用；:1845-1846 体=`pressAlt2`+`sleep(250)`；紧随 xunlu click 之后、输入目标名之前；racy-reopen 注释语义保留）**→KEY_TAP{Alt+2}+WAIT250 seam；owner=39；断言=close-after-xunlu 顺序行。
- **`scrollWorldMapSearchResultsToBottomDirect` 完整内部序**（:2310-2326+常量 :143-146）：cancel 前检→`clickLeft(focusX,focusY,50)`→loop×**3**{cancel 检→`scrollDown(6)`→`sleep(80ms)`+cancel 检}→`sleep(200ms)`+!cancelled。turn seam=CLICK_LEFT{50}+3×(SCROLL{x,y,+6}+WAIT80)+WAIT200，cancel 检=stop-bridge checkpoint 逐步保留，**顺序/次数/等待逐字面**；断言=scroll 序行（3 次、6 units、80/200ms、cancel 序）。
**fallback 保全（更正）**：撤回"focused fallback 消亡→FAILED"声明——该分支（HWND 未尝试/非 terminal 失败→`inputProvider.pressAlt1()` 前台→true，:2356-2376 三分支保序）为基线行为，**不得由迁移单方面变更**。语义保全并入下方统一能力合同。

### P1-2：二元决策修正（residual exception 撤回）
撤回"行2 例外残留"选项（=Review#3 已拒的 RETAIN 形）。**唯一二元决策**：
- (i) **批准** `exact-window / 全局串行前台键盘能力`（一揽子合同，见下）→TURN-39 **全量 retire**（12 行全消、门测试无例外行）。
- (ii) **否决** →TURN-39 保持 `PLAN-CONTRACT BLOCKED`（不开卡、无部分实施、无例外行）。
**能力合同（单一、覆盖全部基线前台键盘路径）**：exact-window 绑定+全局输入串行下提供 ①`KEY_TAP` 前台 fallback 语义（Alt+1 的 attempted/terminalFailure/非 terminal 三分支逐字保留：HWND 成功→完成；terminal 拒→失败；未尝试/非 terminal→前台按键→成功）②`Ctrl+A` chord ③`TYPE_TEXT_UNICODE` ④`ENTER`。传递写集（能力批准时）：DHXY `TurnKeyMapper`（词表扩展）/`TurnInputStepExecutor`（KEY_TAP fallback 分支+TEXT_INPUT/chord 支持）/`TurnProtocolValidator`（字段规则）/`TurnInputSpec`（如需 mode 字段）/双仓 golden+validator+executor contract test；不改 retry/cancel 顺序、不建第二 queue/service、不复制检测器。

### P2-1：retained 集 12 文件逐一精确 disposition/card/guard
| # | path | disposition | 精确 owner | guard 断言 |
|---|---|---|---|---|
| 1-4 | `input/InputSequences.java`+`input/action/{CloudInputActionMapper,InputAction,InputActionScope}.java` | DELETE（零引用后） | TURN-44（旧 facade 解耦 Delete set） | allowlist 行：active-token=0 |
| 5 | `remote/CloudGameClient.java` | DELETE（bundle wire 面随卡） | TURN-44 | active-token=0；bundle 面无新 caller |
| 6 | `remote/CloudTaskServicePort.java`（bundle 面） | MODIFY（44 删 bundle 面；typed 面存续） | TURN-44 | bundle-token=0 after 44 |
| 7 | `remote/CloudTaskServiceExecutionContext.java` | DELETE（execution context 属 44 Delete set"execution gate/context/current slot"） | TURN-44 | active-token=0 |
| 8 | `remote/CloudTaskServiceMetadata.java` | **SURVIVING/READ-ONLY**（SCC 外；0 inputRefs；typed metadata 载体无删除 owner） | 无删除卡（幸存） | 无旧 facade token |
| 9 | `runner/context/TaskExecutionContext.java` | **MODIFY**（幸存外部消费者；legacy delegate/accessor 随 SCC 删除时移除） | TURN-44A cohort 伴随 MODIFY（非 17-SCC DELETE 成员） | legacy-accessor-token=0 after 44A |
| 10 | `remote/CloudTaskRunCommandExecutor.java` | DELETE | TURN-44（Delete set 字面成员） | active-token=0 |
| 11 | `remote/RemoteGameClientPort.java` | DELETE | TURN-44（Delete set 字面成员） | active-token=0 |
| 12 | `remote/RemoteGameCommandBroker.java` | DELETE | **TURN-44A 17-文件 cohort**（依 44A manifest；旧 TURN-45 概要措辞被 manifest 取代） | active-token=0 |
（更正：此前"十文件"计数错误，实为 12；伞式 `TURN-44/44A plan Delete set` 标签废除。）门测试 `OldFacadeRemovalContractTest` 引用本表逐行。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW5-REPAIR-REDELIVERED ROW2-CLOSEWORLDMAP+SCROLL-FULL-ORDER FALLBACK-PRESERVED-CAPABILITY-CONTRACT BINARY-DECISION-APPROVE-OR-BLOCKED 12-FILE-EXACT-OWNERS METADATA-SURVIVING TASKEXECCONTEXT-MODIFY-44A BROKER-44A-COHORT ACK=REVIEW5-2035 AWAIT-REVIEW6 2026-07-18T20:50:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #6 - 2026-07-18 20:55 EDT

- verdict: `P0/P1/P2=0/2/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The completed
  close-world-map/scroll ordering, fallback preservation, corrected approve-or-block binary decision, and metadata/
  `TaskExecutionContext` distinction are accepted. TURN-39 remains unopened.

### P1-1 - The approved-capability write set cannot implement global serialization as written

- `TurnInputStepExecutor` currently sends supported keyboard taps directly through `BoundWindowKeyboardService`;
  only mouse sequences enter the frozen exact-window `InputActionQueue`. Adding text/chord/fallback branches only
  to the executor/key mapper/spec/validator therefore cannot guarantee one global physical-input transaction.
- The existing single worker already owns exact-window focus and global serialization. Its physical model includes
  `TYPE_TEXT_UNICODE`, `PRESS_ENTER`, Alt shortcuts and Ctrl hold/release, but has no Ctrl+A action or generic `A`
  tap. The report omits `TurnInputActionMapper`, `InputAction`/`InputActionType`, `InputActionWorker` and the exact
  queue/focus contract tests needed to route Ctrl+A, Unicode, Enter and Alt+1 fallback through that one worker.
- repair condition: freeze the exact existing-queue design and full production/test write set. State the single
  atomic request boundaries, exact-window witness/focus behavior, Ctrl+A representation, interruption/cancellation
  behavior, and named assertions proving no direct executor foreground input and no second queue/service.

### P1-2 - Five owner rows split the frozen 44A SCC using obsolete TURN-44 wording

- The post-45A manifest freezes `CloudGameClient` (#1), `CloudTaskRunCommandExecutor` (#8),
  `CloudTaskServiceExecutionContext` (#12), `CloudTaskServicePort` (#13), and `RemoteGameClientPort` (#15) inside
  the same 17-file TURN-44A SCC. Repair #5 instead assigns rows 5, 6, 7, 10 and 11 to TURN-44, which would delete or
  modify SCC members before their mutually dependent cohort and cannot preserve the required compile point.
- Rows 1-4 (`InputSequences` plus three action files) are external consumers of that SCC. The readiness manifest
  explicitly says their precise owner was not frozen; the old high-level TURN-44 summary is not evidence that they
  may be deleted there. Their final owner must be derived together with the full external-consumer closure before
  44A, not inferred from a generic "old facade" label.
- repair condition: use the section-17 `45A -> 44A -> 45B` manifest as authority, keep all 17 SCC members in 44A,
  and provide a source-true owner/compile closure for the four external input files. Do not split the SCC or reuse
  the superseded summary delete set.

## Parent Communication Snapshot - 2026-07-18 20:55 EDT

- `PARENT-A-TURN39P1-COMMUNICATION-STALE-2040` was not named in either the 20:44 or 20:52 A event;
  `COMMUNICATION_STALE` is set again. A is active and has just delivered, so `ACTIVE_STALE` is false.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-6 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED ACCEPTED=CLOSEWORLDMAP+SCROLL+FALLBACK+BINARY+METADATA/TASKCTX CAPABILITY-WRITESET-MISSING-SINGLE-QUEUE SCC-OWNERS-SPLIT-WRONG INPUT-EXTERNAL-OWNER-UNFROZEN COMMUNICATION_STALE ACTIVE_STALE-FALSE NO-TURN39-READY NO-MAVEN 2026-07-18T20:55:00-04:00 -->

## EXTERNAL-A REVIEW #6 REPAIR RE-DELIVERY - 2026-07-18T21:00:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW6-REPAIR-2055` + `PARENT-A-TURN39P1-COMMUNICATION-STALE-2040`（迟 ACK：2035 于 20:44/20:52 两轮未具名——本 lane 卡内已 ACK 但总账 STATUS EVENT 字段漏具名 message id，纪律修正=STATUS EVENT 的 ack_parent_message 字段必写全部未闭合 message id；communication 恢复）。两 P1 闭合。

### P1-1：能力批准路径的 exact-existing-queue 写集（经 physical worker，非仅 executor）
**关键 source 事实**：现行 `TurnInputStepExecutor` 仅 mouse 序列进 exact-window `InputActionQueue`；KEY_TAP 直发 `BoundWindowKeyboardService` **不经队列**——故仅改 executor/keymapper/spec/validator **不能**保证单一全局 physical-input 事务。`InputActionType`（20+ 值）含 `TYPE_TEXT_UNICODE`/`PRESS_ENTER`/Alt/`HOLD_CTRL`+`RELEASE_CTRL`/`PRESS_CTRL_U`，但 **无 Ctrl+A 亦无裸 A tap**（grep pressCtrlA/CTRL_A/selectAll=0）。
**冻结设计（能力批准时）**：全部键盘 turn 输入改**经既有单 worker exact-window `InputActionQueue`**（与 mouse 同一事务/焦点/串行），executor 键盘分支不再直发 BoundWindowKeyboardService。**单原子请求边界**=一个 turn INPUT step→一个 queue 请求（keyboard 与 mouse 同）；**Ctrl+A 表达**=复用既有 `HOLD_CTRL`+新增 `PRESS_A`（或 chord DTO）经 worker——需向 physical 模型补 A tap。**传递写集**：`input/action/InputActionType.java`（+A/chord 值）、`input/action/InputAction.java`（工厂）、`input/action/InputActionWorker.java`（键盘经队列执行）、`input/action/CloudInputActionMapper.java`（新值 DTO）、`cloud/turn/TurnInputActionMapper.java`（turn→InputAction 键盘扩展）、`cloud/turn/TurnInputStepExecutor.java`（KEY_TAP/TEXT/chord/Alt-fallback 全经 queue）、`cloud/turn/TurnKeyMapper.java`（词表）、`cloud/turn/protocol/TurnInputSpec.java`（如需 mode/chord 字段）、`cloud/turn/protocol/TurnProtocolValidator.java`、双仓 golden+`TurnProtocolValidatorContractTest`+`TurnInputStepExecutorContractTest`+新 exact-window/queue/focus contract test。**断言**：单 queue 事务边界；exact-window witness/focus；Ctrl+A 表达；中断/取消行为（既有 InputActionScope/stop-bridge 顺序逐字保留）；**零 executor 直发前台输入**；**无第二 queue/service**；Alt+1 fallback 三分支保序。
**否决路径**：不动这些文件，TURN-39 保持 PLAN-CONTRACT BLOCKED。

### P1-2：17-file 44A SCC 整体保留 + 四外部 input 消费者 closure（依 section-17 manifest 权威）
撤回 Repair#5 把 SCC 成员拆给 TURN-44 的错误。以 `2026-07-16-turn-44A-post-45A-deletion-readiness-helper-r1.md` 17-file manifest 为权威：
- **17 SCC 成员全部归 TURN-44A 单卡**（不拆、不制造假编译点）：CloudGameClient(#1)、CloudSummonSkillWholePassCapability(#2)、CloudTaskExclusiveInteractionAuthority(#3)、…CloudTaskRunCommandExecutor(#8)、CloudTaskServiceExecutionContext(#12)、CloudTaskServicePort(#13)、RemoteGameClientPort(#15)、RemoteGameCommandBroker(#16)、TaskTransactionAction(#17) 等——本报告不逐一改派，仅引用 manifest；guard 断言=39 内对这些文件 active business token=0（字节留至 44A）。
- **四外部 input 消费者（`InputSequences`+`CloudInputActionMapper`+`InputAction`+`InputActionScope`）**：manifest 明言其精确 owner **未冻结**；且 #1 CloudGameClient 的外部 type-site 恰含 `InputSequences`+`TaskExecutionContext`（helper 表列）。⇒ 其最终 owner **须与完整 external-consumer closure 一并推导，不得在 TURN-39 内删、亦不得挂 TURN-44 概要标签**。本报告冻结为：**TURN-39 只做 active reference→0**（retire 后这四文件成零 caller 死定义）；**其物理删除 owner=一条精确 plan-contract 依赖**（"44A 前 external-consumer closure 卡"，未冻结，属父级计划职责，非本报告可指派）。
- **外部 type-site 消费者更正**（P1-2 幸存者）：`TaskExecutionContext`（#12/#13/#1 引用）、`CloudArtifactStore`/`ScopedPngArtifactStore`（#12）、`CloudBagStateOwner`（#13 import）、`CloudTaskTurnAuthority`（#9）、`LeaderPrecheckAction`（#5）=**SCC 外幸存消费者**，随各自 owner 卡在 SCC 删除时 MODIFY，非 SCC DELETE 成员。
门测试 allowlist=active-zero 集（Nav/Wubei/2 Javadoc）+ 44A-retained=17 SCC 逐行（manifest 权威）+ 四 input 文件（owner=未冻结 external-consumer closure）；三 service guard test READ-ONLY。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW6-REPAIR-REDELIVERED CAPABILITY-VIA-INPUTACTIONQUEUE-WORKER CTRLA-NEEDS-PHYSICAL-A-TAP FULL-KEYBOARD-WRITESET 17-SCC-WHOLE-44A FOUR-INPUT-CONSUMERS-OWNER-UNFROZEN-CLOSURE EXTERNAL-TYPESITES-MODIFY ACK=REVIEW6-2055+STALE-2040 AWAIT-REVIEW7 2026-07-18T21:00:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #7 - 2026-07-18 21:15 EDT

- verdict: `P0/P1/P2=0/2/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The exact-window
  single-worker direction and restoration of all 17 SCC members to TURN-44A are accepted. Communication recovered
  through the exact 21:02 ACKs. TURN-39 remains unopened.

### P1-1 - Per-INPUT-step requests do not preserve the baseline exclusive keyboard/edit transaction

- Baseline `NavigationService.executeWorldMapPrepareAndClick` submits the complete
  `prepareWorldMapSearchResultsDirect` callback once through `InputSequences.submitExclusiveAndWait`
  (`NavigationService:665-667`). Inside that one transaction, the stale-panel branch performs focus click ->
  Ctrl+A -> Unicode text -> wait -> search click (`:1937-1955`); the normal branch performs xunlu click -> close
  map -> Unicode text -> wait -> Enter -> scroll preparation (`:1970-1995`).
- The proposed boundary, "one turn INPUT step -> one queue request", releases the global input owner between those
  dependent steps. Current `LocalTurnActionExecutor.findMouseSequenceEndExclusive` coalesces only mouse/WAIT
  fragments and explicitly executes KEY_DOWN/KEY_UP/KEY_TAP/TEXT_INPUT one by one (`:154-202`). Another window may
  therefore focus and type between Ctrl+A, text and Enter/search click. `InputActionQueue`
  `submitFrozenExactWindowActionsAndWait` already preserves a complete action list in one request (`:386-405`),
  but Repair #6 omits the required keyboard/edit sequence grouping in `LocalTurnActionExecutor` and its tests.
- Ctrl+A is also not frozen: `HOLD_CTRL + PRESS_A (or chord DTO)` leaves two incompatible designs. The existing
  worker has no compensation that guarantees `RELEASE_CTRL` after interruption/cancellation between actions, so
  the hold/tap/release proposal may leak a physical modifier. Freeze one exact representation and cleanup rule;
  the source-minimal candidate is a single `PRESS_CTRL_A` physical action that calls the existing
  `InputProvider.pressCtrlA()` inside the worker, then place each baseline-dependent edit sequence in one frozen
  queue request.
- repair condition: enumerate the exact atomic sequence boundaries for both world-map branches and Alt+1 fallback;
  add `LocalTurnActionExecutor` plus sequence/anti-interleave/cancel tests to the write set; choose one Ctrl+A wire
  and physical representation; prove no modifier leak, no direct executor foreground input, and no second queue.
  Every production/test path must name its repository; the Cloud legacy `CloudInputActionMapper` is not part of
  DHXY turn execution and must either be justified by a real compile edge or removed from this capability delta.

### P1-2 - The four external input consumers still have no final owner or compile point

- Review #6 required a source-true owner/compile closure for `InputSequences`, `CloudInputActionMapper`,
  `InputAction` and `InputActionScope`. Repair #6 instead records their owner as "unfrozen external-consumer
  closure" and returns the decision to the parent. That accurately reports the gap but does not close it.
- `InputAction` is also proposed for the approved keyboard capability while the same report calls the four files
  zero-caller dead definitions after TURN-39. The plan must state whether the capability keeps this physical model
  live, which repository owns each same-named file, and the exact MODIFY/DELETE compile order before 44A. A
  placeholder future card is not an acceptance contract.
- repair condition: provide the complete external-consumer/type-site closure and freeze each of the four exact
  repository paths to an existing card or a fully specified new predecessor card with source/test write set,
  prerequisites and compile point. Keep all 17 SCC members together in 44A and do not invent an intermediate
  compile point.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-7 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED ACCEPTED=EXACT-WINDOW-SINGLE-WORKER+17-SCC-WHOLE-44A PER-INPUT-REQUEST-BREAKS-BASELINE-ATOMICITY CTRLA-REPRESENTATION-AMBIGUOUS MODIFIER-CLEANUP-MISSING LOCALTURNEXECUTOR-WRITESET-MISSING FOUR-EXTERNAL-INPUT-OWNERS-STILL-UNFROZEN COMMUNICATION-RECOVERED NO-TURN39-READY NO-MAVEN 2026-07-18T21:15:00-04:00 -->

## EXTERNAL-A REVIEW #7 REPAIR RE-DELIVERY - 2026-07-18T21:22:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW7-REPAIR-2115`。两 P1 闭合。

### P1-1：原子序边界 + 单一 Ctrl+A 表示 + LocalTurnActionExecutor 写集

**source 事实（DHXY-cr271 只读）**：`LocalTurnActionExecutor` blob `dfa450c2`/308L 的 `findMouseSequenceEndExclusive`（:156-179）**只 coalesce mouse+正 WAIT** 前缀；`isMouseInput`（:181-190）显式把 KEY_TAP/KEY_DOWN/KEY_UP/TEXT_INPUT 归 false→**逐个执行**。`InputActionQueue.submitFrozenExactWindowActionsAndWait`（859L）已支持"完整 action list 单请求"。`InputProvider.pressCtrlA()` **已存在**（:34）——故 Ctrl+A 无需新增 chord/PRESS_A，物理最小候选=单 `PRESS_CTRL_A` 直调既有 `pressCtrlA()`，**无 hold/release 泄漏风险**（父级指正的 modifier-leak 由此消除，无需中断补偿）。

**冻结原子序边界（两分支+fallback，逐 source 行）**：
- **stale-panel 分支**（baseline :1937-1955，单 exclusive 事务内）：focus `clickLeft` → `pressCtrlA()` → `typeTextUnicode` → WAIT → search `clickLeft` = **一个 frozen queue 请求**（不在中途释放全局 owner）。
- **normal 分支**（:1970-1995）：xunlu `clickLeft` → close-map(`pressAlt2`+WAIT) → `typeTextUnicode` → WAIT → `pressEnter` → scroll-prep = **一个 frozen queue 请求**（含 KEY_TAP/TEXT/ENTER 与 mouse 同事务）。
- **Alt+1 fallback**：HWND `pressShortcut(ALT_1)` 三分支（attempted+success/terminalFailure/未尝试或非 terminal→前台 `pressAlt1()`）——保序保全，落一个请求。

**冻结 Ctrl+A 表示（唯一）**：wire=新 `TurnInputAction.PRESS_CTRL_A`（或复用 KEY_TAP{key="Ctrl+A"}——**择一：取 PRESS_CTRL_A 显式值**，避免 keymapper 词表歧义）；physical=`InputAction.pressCtrlA()`（既有 `InputProvider.pressCtrlA()`）；无 modifier hold 状态跨 action。

**能力批准写集（逐 repo path）**：
- DHXY-cr271：`cloud/turn/LocalTurnActionExecutor.java`（**新增 keyboard/edit-sequence coalescing**：INPUT 键盘步与相邻 mouse/WAIT 同 frozen queue 请求；`isMouseInput`→`isQueueableInput` 扩含 KEY_TAP/TEXT/PRESS_CTRL_A/PRESS_ENTER）、`cloud/turn/TurnInputActionMapper.java`（PRESS_CTRL_A→InputAction）、`cloud/turn/TurnKeyMapper.java`（如取 KEY_TAP 路则词表；取 PRESS_CTRL_A 显式值则免）、`cloud/turn/protocol/{TurnInputAction,TurnInputSpec,TurnProtocolValidator}.java`、`input/action/{InputActionType,InputAction,InputActionQueue?}.java`（PRESS_CTRL_A 物理值+worker 分支）、`input/InputProvider.java`（`pressCtrlA` 已存在=READ-ONLY）。
- 测试（DHXY-cr271）：`LocalTurnActionExecutorContractTest`（**新增 edit-sequence 单请求/anti-interleave/cancel-no-leak 断言**）、`TurnInputStepExecutorContractTest`、`TurnProtocolValidatorContractTest`、双仓 golden。
- **CloudInputActionMapper 更正**：父级正确指出该 Cloud 类**不在 DHXY turn 执行链**——从本能力 delta **移除**（其仅服务旧 InputSequences bundle wire=44A-retained）。
- 断言：两分支 edit 序各为一 queue 请求（exact-window witness）；Alt+1 fallback 三分支保序；Ctrl+A 单表示无 modifier 泄漏；cancel/interrupt 在请求边界处理不跨窗；零 executor 直发前台；无第二 queue/service。否决→39 PLAN-CONTRACT BLOCKED。

### P1-2：四外部 input 消费者精确 closure

external-consumer 闭包（grep 全 Cloud src/main）：`InputSequences` 仅被 NavigationService+WubeiTask 消费（12 站点，retire 后=0 caller）；`CloudInputActionMapper`/`InputAction`/`InputActionScope` 仅被 `InputSequences` 内部+turn 执行链（`InputAction` 经 TurnInputActionMapper/executor/queue）消费。⇒ **`InputAction` 在能力批准下保持 live**（turn 物理模型载体，非死定义——更正 Repair#6 "四文件全死"的错误：`InputAction` 属 physical worker 模型，retire 后仍由 turn 链使用）；仅 `InputSequences`+`CloudInputActionMapper`+`InputActionScope` 成 InputSequences-only 死定义。
**四路精确冻结**：
| repo path | disposition | owner card | compile point |
|---|---|---|---|
| `input/action/InputAction.java` | **MODIFY**（+PRESS_CTRL_A 物理值；turn 链持续使用） | **TURN-39**（能力批准时；否则 READ-ONLY） | 39 内编译 |
| `input/InputSequences.java` | DELETE | **新前置卡 `TURN-39D`（InputSequences retirement，dependsOn 39 caller-zero）** 或 TURN-44 external-consumer 卡 | 零 caller 后 |
| `input/action/CloudInputActionMapper.java` | DELETE（仅服务 InputSequences bundle） | 同上 TURN-39D | 同上 |
| `input/action/InputActionScope.java` | DELETE（cancel 语义 retire 后由 stop-bridge 承载，零 caller） | 同上 TURN-39D | 同上 |
**建议**：开精确前置卡 **`TURN-39D`**（source+test：删三 InputSequences-only 文件+断言零引用；dependsOn=TURN-39 caller-zero；write set=三文件 DELETE+`OldFacadeRemovalContractTest` active-zero 行；**不制造 44A 内假编译点**——三文件非 17-SCC 成员，可在 39 后、44A 前独立删）。17 SCC 成员仍整体 44A，不动。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW7-REPAIR-REDELIVERED ATOMIC-SEQ-TWO-BRANCHES-FROZEN PRESS-CTRL-A-SINGLE-REP-NO-LEAK LOCALTURNEXECUTOR-COALESCE-WRITESET CLOUDINPUTMAPPER-REMOVED-FROM-DELTA INPUTACTION-MODIFY-LIVE THREE-FILES-TURN39D-DELETE 17-SCC-INTACT ACK=REVIEW7-2115 AWAIT-REVIEW8 2026-07-18T21:22:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #8 - 2026-07-18 21:30 EDT

- verdict: `P0/P1/P2=0/2/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The branch-level
  single-request requirement and explicit no-hold `PRESS_CTRL_A` direction are accepted. TURN-39 remains unopened.

### P1-1 - The external-consumer closure conflates two repositories and leaves two Cloud files orphaned

- Section 17's external-consumer files are Cloud-repository paths. The report changes `InputAction.java` in
  TURN-39 as though it were one shared file, but there are two independent classes:
  `DHXY-cr271/src/main/java/com/bot/dhxy/input/action/InputAction.java` is the live physical worker model and must
  be modified by the approved capability; `dhxy-cloud-brain/src/main/java/com/bot/dhxy/input/action/InputAction.java`
  is the legacy bundle model used by Cloud `InputSequences`/`CloudInputActionMapper`/Wubei and is not used by the
  DHXY turn executor.
- Cloud source census also shows a fifth member of that legacy mini-cohort,
  `dhxy-cloud-brain/.../input/action/InputActionType.java`. Deleting only `InputSequences`,
  `CloudInputActionMapper` and `InputActionScope` leaves Cloud `InputAction.java` and `InputActionType.java` as
  stranded definitions. Conversely, calling the Cloud file live because the DHXY same-package file is live is a
  false cross-repository compile edge.
- `TURN-39D` is only a suggested name, not a fixed original card or accepted section-16 contract. It cannot serve
  as an owner until the parent plan freezes the complete five-file Cloud deletion cohort, affected source guards,
  prerequisites and one real compile point. The current three-file proposal does not meet Review #7's closure.
- repair condition: qualify every path with `DHXY-cr271` or `dhxy-cloud-brain`; keep the DHXY physical
  `InputAction`/`InputActionType` MODIFY delta separate; derive the full Cloud legacy input mini-cohort including
  both Cloud `InputAction.java` and `InputActionType.java`, all production/test references, and propose an exact
  existing-card integration or complete plan amendment. Do not count same-package names across repositories as one
  type.

### P1-2 - The approved-capability write set is still conditional and omits required production owners

- The supposedly frozen list still contains `InputActionQueue?` and conditionally includes `TurnKeyMapper`; an
  acceptance write set cannot contain question marks or alternate routes after selecting explicit
  `TurnInputAction.PRESS_CTRL_A`.
- The new `InputActionType.PRESS_CTRL_A` cannot execute without changing
  `DHXY-cr271/.../InputActionWorker.java`, but that file is absent from the explicit Repair #7 DHXY list. The wire
  enum must also be mirrored in `dhxy-cloud-brain/.../cloud/turn/protocol/TurnInputAction.java`, and the Cloud
  Navigation turn producer and its contract tests must emit/assert the selected value; "dual-repo golden" alone
  is not a production write set.
- `LocalTurnActionExecutor` grouping is described as including `PRESS_ENTER`, but the actual turn protocol has no
  `PRESS_ENTER` action; Enter is currently a `KEY_TAP` value. The exact grouping predicate and mapper must use the
  real closed enum. For Alt+1, existing `InputActionWorker.canUseBackgroundKeyboard` permits the baseline HWND
  attempt only for an Alt/SLEEP-only request; a mixed edit request forces focused input directly. The report must
  freeze Alt+1 as its own eligible request and test the attempted/success, terminal failure and focused-fallback
  branches rather than merely saying "one request".
- repair condition: provide one unconditional, repository-qualified production/test write set; include the DHXY
  worker, both mirrored protocol enum sources, Cloud producer, exact grouping predicate and named tests. State
  READ-ONLY files without `?`, and prove Alt+1 background eligibility is preserved separately from mixed edit
  sequences.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-8 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED ACCEPTED=BRANCH-ATOMIC-REQUEST+PRESS-CTRL-A-NO-HOLD REPO-CONFLATION-DHXY-VS-CLOUD-INPUTACTION CLOUD-INPUTACTION+INPUTACTIONTYPE-ORPHANED TURN39D-NOT-FROZEN WRITESET-CONDITIONAL WORKER+CLOUD-PROTOCOL-PRODUCER-MISSING ALT1-BACKGROUND-ELIGIBILITY-UNFROZEN COMMUNICATION-NORMAL NO-TURN39-READY NO-MAVEN 2026-07-18T21:30:00-04:00 -->

## EXTERNAL-A REVIEW #8 REPAIR RE-DELIVERY - 2026-07-18T21:36:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW8-REPAIR-2130`（迟 ACK 纪律：ack 字段列全 id）。两 P1 闭合；全 path repo-qualified，无 `?`/无条件路。

**两仓同名类实证（只读 ls）**：`DHXY-cr271/.../input/action/` 有 `InputAction.java`+`InputActionType.java`+**`InputActionWorker.java`**（live physical worker，`canUseBackgroundKeyboard`:735）；`dhxy-cloud-brain/.../input/action/` 有 `InputAction.java`+`InputActionType.java`（legacy bundle；Cloud InputActionType 仅被 Cloud InputAction 消费）。**同包名跨仓=两独立类，不作一 compile edge**（更正 Repair#7 conflation）。

### P1-2：无条件 repo-qualified 能力批准写集（PRESS_CTRL_A wire 已定）
**DHXY-cr271 production（MODIFY，physical 模型+turn 执行）**：
- `.../input/action/InputActionType.java`（+`PRESS_CTRL_A` 物理值）
- `.../input/action/InputAction.java`（+`pressCtrlA()` 工厂→既有 `InputProvider.pressCtrlA()`）
- `.../input/action/InputActionWorker.java`（执行 PRESS_CTRL_A；**`canUseBackgroundKeyboard` 逻辑不变**：Alt/SLEEP-only→background、mixed edit→focused）
- `.../cloud/turn/protocol/TurnInputAction.java`（+`PRESS_CTRL_A` wire 值）
- `.../cloud/turn/TurnInputActionMapper.java`（PRESS_CTRL_A→InputAction.pressCtrlA）
- `.../cloud/turn/protocol/TurnProtocolValidator.java`（PRESS_CTRL_A 字段规则=无坐标/无 timing）
- `.../cloud/turn/LocalTurnActionExecutor.java`（grouping predicate `isQueueableInput`=既有 mouse ∪ {`KEY_TAP`,`TEXT_INPUT`,`PRESS_CTRL_A`}；**`ENTER` 更正=既有 `KEY_TAP{key="enter"}` 非 `PRESS_ENTER`**〔turn 协议无 PRESS_ENTER〕；edit-sequence 全落一 frozen queue 请求）
- `.../input/InputProvider.java`=**READ-ONLY**（pressCtrlA 已存在）
**dhxy-cloud-brain production（MODIFY，wire 镜像+producer）**：
- `.../cloud/turn/protocol/TurnInputAction.java`（**镜像 +`PRESS_CTRL_A`**，两仓 byte-identical）
- `.../service/NavigationService.java`（世界地图两分支 producer：发 PRESS_CTRL_A/TEXT/KEY_TAP{enter} + mouse 于**一 turn INPUT 序列**；Alt+1 fallback 独立请求）
**测试（无条件）**：DHXY-cr271 `.../cloud/turn/LocalTurnActionExecutorContractTest.java`（edit-seq 单请求/anti-interleave/cancel-no-modifier-leak/**Alt+1 独立 background-eligible 请求**三分支〔attempted+success/terminalFailure/focused-fallback〕）+`TurnInputStepExecutorContractTest`+`TurnProtocolValidatorContractTest`+双仓 golden `TurnCoreProtocolGoldenJsonTest`；dhxy-cloud-brain `.../service/NavigationTurnContractTest.java`（producer 发值+两分支原子序+布尔终态）。
**Alt+1 独立性冻结**：混合 edit 请求（含 Ctrl+A/TEXT）在 `canUseBackgroundKeyboard` 下强制 focused（=baseline exclusive 前台语义）；Alt+1 单独请求保 background-HWND 资格——**故 Alt+1 不并入 mixed edit 序列，为其自身 eligible 请求**，三分支各断言。

### P1-1：五文件 Cloud legacy mini-cohort + 精确 owner（撤回 TURN-39D 名/三文件案）
Cloud legacy cohort（全 `dhxy-cloud-brain`，InputSequences-only，非 17-SCC）：①`.../input/InputSequences.java` ②`.../input/action/CloudInputActionMapper.java` ③`.../input/action/InputActionScope.java` ④`.../input/action/InputAction.java` ⑤`.../input/action/InputActionType.java`。census：④⑤仅被 InputSequences/①②及彼此消费（Cloud InputActionType 仅 Cloud InputAction 引用）；retire（TURN-39 使 Nav/Wubei active-ref→0）后五文件成零 caller 死 cohort。**DHXY 同名 InputAction/InputActionType/InputActionWorker 保持 live（能力 MODIFY，属上 P1-2 DHXY 集）——与 Cloud 五文件无 compile edge**。
**owner 冻结**：五文件删除**须父级计划 amendment**（本 report-only 卡不能新建 section-16 卡）：建议 amend 计划新增精确前置卡 **`TURN-39C1`（Cloud InputSequences legacy cohort retirement）**：dependsOn=TURN-39 caller-zero；write set=五文件 DELETE（`dhxy-cloud-brain` 全限定）+`OldFacadeRemovalContractTest` active-zero 断言；prerequisite=39 retire 落地；**唯一 compile point=五文件同批删（互为唯一 caller，零外部残留 после Nav/Wubei retire）**；不入 44A、不制造 SCC 内假编译点。若父级选并入既有卡，须点名该卡。17-SCC 成员仍整体 44A 不动。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW8-REPAIR-REDELIVERED REPO-QUALIFIED-UNCONDITIONAL DHXY-3-PHYSICAL-MODIFY+WORKER CLOUD-WIRE-MIRROR+PRODUCER ENTER=KEY_TAP-CORRECTED ALT1-SEPARATE-BACKGROUND-REQUEST CLOUD-5-FILE-COHORT TURN39C1-AMENDMENT 17-SCC-INTACT ACK=REVIEW8-2130 AWAIT-REVIEW9 2026-07-18T21:36:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #9 - 2026-07-18 21:45 EDT

- verdict: `P0/P1/P2=0/2/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The repository
  qualification, complete five-file Cloud legacy cohort, proposed `TURN-39C1` predecessor shape, mirrored wire
  enum/Cloud producer, real `KEY_TAP{enter}` representation and separate Alt+1 request are accepted. Communication
  is normal. TURN-39 remains unopened.

### P1-1 - The write set still omits the two execution owners that must queue keyboard/edit steps

- Current `DHXY-cr271/.../TurnInputActionMapper.java` exposes only `mapMouse(...)`; its closed switch throws for
  `KEY_TAP`, `KEY_DOWN`, `KEY_UP` and `TEXT_INPUT`. Current `TurnInputStepExecutor.execute(...)` submits only mouse
  actions to `InputActionQueue`, sends a validated Alt `KEY_TAP` directly through `BoundWindowKeyboardService`, and
  returns `BACKGROUND_KEY_UNSUPPORTED` for every non-`KEY_TAP` keyboard action. Its sequence API is explicitly
  `executeMouseSequence(...)`, requires mouse first/last, and maps only mouse plus WAIT.
- Adding `isQueueableInput` to `LocalTurnActionExecutor` cannot by itself turn `KEY_TAP`, `TEXT_INPUT` or new
  `PRESS_CTRL_A` into `InputAction` objects or submit the resulting branch as one frozen request. Repair #8 lists
  neither `TurnInputStepExecutor.java` nor its required generic sequence API conversion. It lists
  `TurnInputActionMapper.java` only for `PRESS_CTRL_A`, but the mapper must also map the selected Alt/Enter
  `KEY_TAP` and Unicode text forms. `TurnKeyMapper.java` is likewise absent even though its current ownership is
  background-only key recognition and the final route must distinguish Alt shortcuts, Enter and unsupported keys.
- repair condition: add all three files to one unconditional DHXY production write set and freeze their exact
  responsibilities: mapper converts the complete approved physical vocabulary; step executor submits one generic
  frozen input/WAIT sequence with no direct keyboard service path; key mapper maps only the closed approved names.
  State exact handling for unsupported `KEY_DOWN`/`KEY_UP` and prove no second queue or per-step release.

### P1-2 - The named tests do not exercise the real worker's Alt+1 background/fallback policy

- `canUseBackgroundKeyboard`, exact-binding `pressShortcut`, terminal-failure rejection and focused real-input
  fallback all live in `DHXY-cr271/.../InputActionWorker.java` (`canUseBackgroundKeyboard` around line 735 and
  `executeAltShortcut` around lines 616-674). `LocalTurnActionExecutorContractTest` can prove grouping and one queue
  submission, but it cannot prove those private worker branches merely by naming three expected outcomes.
- The existing real-worker harness is `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`;
  it already records exact HWND calls, focus and physical input and is the appropriate retained test owner. The
  capability also changes physical `InputActionType`/`InputActionWorker`, so omitting a worker-level named test
  leaves the most safety-sensitive behavior unverified.
- repair condition: include `InputActionFrozenExclusiveContractTest.java` (and only any additional existing
  worker guard actually required) in the exact test write set. Require concrete assertions for Alt+1
  attempted+success, terminal failure with zero focused fallback, non-terminal/not-attempted focused fallback,
  mixed edit request skipping HWND, `PRESS_CTRL_A` dispatch, interruption/cancellation stopping later actions,
  exact binding and one physical execution. Keep LocalTurnActionExecutor tests for branch grouping separately.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-9 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED ACCEPTED=REPO-QUALIFIED+CLOUD-5-COHORT+TURN39C1-SHAPE+CLOUD-WIRE-PRODUCER+ENTER-KEYTAP+ALT1-SEPARATE TURNINPUTSTEPEXECUTOR+FULL-MAPPER+KEYMAPPER-MISSING WORKER-INTEGRATION-TEST-MISSING COMMUNICATION-NORMAL NO-TURN39-READY NO-MAVEN 2026-07-18T21:45:00-04:00 -->

## Parent Communication Snapshot - 2026-07-18 21:51 EDT

- `PARENT-A-TURN39P1-REVIEW9-REPAIR-2145` has no named ACK in two consecutive parent audits after the 21:44
  pre-review keepalive; `COMMUNICATION_STALE` is set. External A remains sole report owner under Review #9
  `0/2/0 / REPAIR REQUIRED`.
- A's last event is 21:44 and no source SHA/mtime changed, but the independent ten-minute threshold is not yet met;
  `ACTIVE_STALE=false`. TURN-39 remains unopened and no Java/Maven/runtime/input/build state changed.

<!-- TRUE_EOF: TURN-39P1 PARENT-COMMUNICATION-STALE REVIEW9-REPAIR-REQUIRED OWNER-A-RETAINED ACK=PARENT-A-TURN39P1-REVIEW9-REPAIR-2145-MISSING-TWO-ROUNDS ACTIVE_STALE-FALSE NO-SOURCE+BUILD-CHANGE NO-TURN39-READY NO-MAVEN 2026-07-18T21:51:00-04:00 -->

## EXTERNAL-A REVIEW #9 REPAIR RE-DELIVERY - 2026-07-18T21:54:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW9-REPAIR-2145` + `PARENT-A-TURN39P1-COMMUNICATION-STALE-2151`（stale 因 review 于 21:45 落卡、我 21:44 keepalive 之后，父级审计快于本 lane 5-min 心跳；本轮首见即 ACK 清 stale）。两 P1 闭合。已接受项（repo-qualified/五文件 cohort/`TURN-39C1` 形/wire 镜像+producer/`KEY_TAP{enter}`/Alt+1 独立）不复述。

### P1-1：三个 DHXY turn 执行 owner 无条件入写集 + 精确职责
现行事实（DHXY-cr271 只读）：`TurnInputActionMapper` 仅 `mapMouse(...)`，closed switch 对 KEY_TAP/KEY_DOWN/KEY_UP/TEXT_INPUT throw；`TurnInputStepExecutor.execute` 仅 mouse 入 `InputActionQueue`、Alt `KEY_TAP` 直发 `BoundWindowKeyboardService`、非 KEY_TAP 键盘→`BACKGROUND_KEY_UNSUPPORTED`；其序列 API=`executeMouseSequence`（首尾必 mouse、仅 mouse+WAIT）。⇒ 仅加 `LocalTurnActionExecutor.isQueueableInput` 不足；三 owner 精确职责冻结（能力批准时，均 `DHXY-cr271`）：
- `.../cloud/turn/TurnInputActionMapper.java`（MODIFY）：由 `mapMouse` 扩为映射**完整已批准物理词汇**——mouse ∪ `KEY_TAP{Alt+*}` ∪ `KEY_TAP{enter}` ∪ `TEXT_INPUT`(Unicode) ∪ `PRESS_CTRL_A`→对应 `InputAction`（clickLeft/pressAltX/pressEnter/typeTextUnicode/pressCtrlA）；closed switch 全覆盖不 throw approved 值。
- `.../cloud/turn/TurnInputStepExecutor.java`（MODIFY）：删"键盘直发 BoundWindowKeyboardService"路径；新增**泛型 frozen input/WAIT 序列 API**（取代仅 mouse 的 executeMouseSequence 约束——键盘/编辑步与 mouse/WAIT 同入一 `InputActionQueue` frozen 请求）；`KEY_DOWN`/`KEY_UP` **不在 approved 词汇**→保留 `BACKGROUND_KEY_UNSUPPORTED`（明确：能力不引入裸 modifier 按住/释放，Ctrl+A 由单 `PRESS_CTRL_A` 表达，无 per-step release）。
- `.../cloud/turn/TurnKeyMapper.java`（MODIFY）：由"仅 background Alt 识别"扩为映射**闭合已批准键名**——区分 Alt 快捷（13 词表）/`enter`/`PRESS_CTRL_A`；未批准键→unsupported。
证明：无第二 queue（全经既有 `InputActionQueue.submitFrozenExactWindowActionsAndWait`）；无 per-step modifier release（Ctrl+A 原子单 action）；键盘不再有 executor 直发前台路径。

### P1-2：worker-level named test 入写集
`canUseBackgroundKeyboard`(:735)/`executeAltShortcut`(~:616-674)/exact-binding `pressShortcut`/terminal-failure/focused fallback 均在 `DHXY-cr271/.../input/action/InputActionWorker.java` **private 分支**——`LocalTurnActionExecutorContractTest` 无法凭三 outcome 名证之。**测试写集（无条件，均 DHXY-cr271）**：
- `.../input/action/InputActionFrozenExclusiveContractTest.java`（MODIFY，既有 real-worker harness 记录精确 HWND/focus/physical input）：新增断言=Alt+1 attempted+success；terminal-failure **零 focused fallback**；non-terminal/未尝试→focused fallback；**mixed edit 请求跳过 HWND（直 focused）**；`PRESS_CTRL_A` dispatch（调 pressCtrlA 一次）；中断/取消停止后续 action；exact binding；单次 physical execution。
- `.../cloud/turn/LocalTurnActionExecutorContractTest.java`（MODIFY，**仅** branch grouping/单请求/anti-interleave——worker 私有分支归上文件，分离）。
- `.../cloud/turn/TurnProtocolValidatorContractTest.java` + 双仓 golden `TurnCoreProtocolGoldenJsonTest`（PRESS_CTRL_A 值）。
- dhxy-cloud-brain `.../service/NavigationTurnContractTest.java`（producer 两分支原子序+值）。
**最终无条件写集汇总**：DHXY-cr271 production=`input/action/{InputActionType,InputAction,InputActionWorker}.java`+`cloud/turn/{TurnInputAction(protocol),TurnInputActionMapper,TurnInputStepExecutor,TurnKeyMapper}.java`+`cloud/turn/protocol/TurnProtocolValidator.java`（`input/InputProvider.java`=READ-ONLY）；dhxy-cloud-brain production=`cloud/turn/protocol/TurnInputAction.java`(镜像)+`service/NavigationService.java`+Wubei/FiveRing/Xiuluo caller·Javadoc（12 caller retire）；test=上列 5。Cloud 五文件 cohort 归 `TURN-39C1` amendment；17-SCC 整体 44A。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW9-REPAIR-REDELIVERED THREE-TURN-EXEC-OWNERS-UNCONDITIONAL GENERIC-FROZEN-SEQUENCE-API KEYDOWN-KEYUP-UNSUPPORTED-EXPLICIT WORKER-INTEGRATION-TEST-ADDED FINAL-WRITESET-SUMMARY ACK=REVIEW9-2145+STALE-2151 AWAIT-REVIEW10 2026-07-18T21:54:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #10 - 2026-07-18 22:05 EDT

- verdict: `P0/P1/P2=0/2/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The three turn
  execution owners, complete approved mapper vocabulary, generic frozen input/WAIT direction, explicit unsupported
  `KEY_DOWN`/`KEY_UP`, and real-worker integration test responsibilities are accepted. The 21:56 exact double ACK
  clears `COMMUNICATION_STALE`; communication is normal. TURN-39 remains unopened.

### P1-1 - The final unconditional production manifest drops the branch grouping owner

- Repair #9 correctly says the branch-level edit sequence must be one frozen queue request, but its final
  unconditional summary omits `DHXY-cr271/src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`.
  Current `LocalTurnActionExecutor.findMouseSequenceEndExclusive(...)` and `isMouseInput(...)` group only mouse plus
  WAIT and route the result through `executeMouseSequence(...)`; keyboard/text steps are still released one by one.
- The new generic API in `TurnInputStepExecutor` is therefore unreachable for the complete stale-panel and normal
  world-map branches unless `LocalTurnActionExecutor` changes its grouping predicate and invocation. Listing that
  file in an earlier repair paragraph does not cure its omission from the later authoritative "final unconditional
  write set".
- repair condition: add the exact repository-qualified `LocalTurnActionExecutor.java` path to the final production
  manifest and freeze its call to the generic frozen input/WAIT sequence API for both world-map branches. Keep Alt+1
  as its separate eligible request and preserve one exact-window queue/worker with no per-step release.

### P1-2 - The final test manifest drops the direct contract for the changed executor API

- Repair #9's final `test=above 5` list omits
  `DHXY-cr271/src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`, although production
  changes `TurnInputStepExecutor` from mouse-only plus direct Alt keyboard dispatch to a generic frozen
  keyboard/text/mouse/WAIT sequence owner.
- `LocalTurnActionExecutorContractTest` proves branch grouping and `InputActionFrozenExclusiveContractTest` proves
  worker policy, but neither directly proves the executor removed `BoundWindowKeyboardService`, maps the approved
  vocabulary into one frozen request, or continues to reject `KEY_DOWN`/`KEY_UP` without a second queue.
- repair condition: restore `TurnInputStepExecutorContractTest.java` to the final unconditional test manifest and
  require those exact API/dispatch/unsupported assertions. Re-emit one complete final production/test manifest;
  earlier superseded lists must not be needed to reconstruct the implementation contract.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-10 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED ACCEPTED=THREE-EXEC-OWNERS+FULL-MAPPER+GENERIC-FROZEN+KEYDOWN-UP-UNSUPPORTED+REAL-WORKER-TEST LOCALTURNEXECUTOR-MISSING-FROM-FINAL-PRODUCTION-MANIFEST TURNINPUTSTEPEXECUTORCONTRACTTEST-MISSING-FROM-FINAL-TEST-MANIFEST COMMUNICATION-RECOVERED NO-TURN39-READY NO-MAVEN 2026-07-18T22:05:00-04:00 -->

## EXTERNAL-A REVIEW #10 REPAIR RE-DELIVERY — FINAL COMPLETE MANIFEST - 2026-07-18T22:10:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW10-REPAIR-2205`。两 finding=前段列过、"最终汇总"漏列的两文件；本节重发**单一自足最终 manifest**（无需回溯前段即可重建实施合同），补入 `LocalTurnActionExecutor.java`(prod) 与 `TurnInputStepExecutorContractTest.java`(test)。设计内容（三 exec owner/全词汇 mapper/泛型 frozen/KEY_DOWN·UP unsupported/real-worker test）已获接受，不变。

### 最终无条件 PRODUCTION 写集（能力批准路径；repo-qualified，无 `?`/无条件）
**DHXY-cr271（MODIFY 除注明）**：
1. `src/main/java/com/bot/dhxy/input/action/InputActionType.java` — +`PRESS_CTRL_A` 物理值。
2. `src/main/java/com/bot/dhxy/input/action/InputAction.java` — +`pressCtrlA()` 工厂→既有 `InputProvider.pressCtrlA()`。
3. `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` — 执行 `PRESS_CTRL_A`；`canUseBackgroundKeyboard` 逻辑不变（Alt/SLEEP-only→background、mixed edit→focused）；无 per-step modifier release。
4. `src/main/java/com/bot/dhxy/input/InputProvider.java` — **READ-ONLY**（`pressCtrlA` 已存在）。
5. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java` — +`PRESS_CTRL_A` wire 值。
6. `src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java` — 由 `mapMouse` 扩为全批准词汇（mouse ∪ `KEY_TAP{Alt+*}` ∪ `KEY_TAP{enter}` ∪ `TEXT_INPUT` ∪ `PRESS_CTRL_A`）→`InputAction`。
7. `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java` — 删键盘直发 `BoundWindowKeyboardService`；新增泛型 frozen input/WAIT 序列 API；`KEY_DOWN`/`KEY_UP` 保留 `BACKGROUND_KEY_UNSUPPORTED`。
8. `src/main/java/com/bot/dhxy/cloud/turn/TurnKeyMapper.java` — 扩闭合已批准键名（Alt 13 词表 / `enter` / `PRESS_CTRL_A`）；未批准→unsupported。
9. **`src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`**（**补入**）— grouping predicate `isMouseInput`→`isQueueableInput`（mouse ∪ {KEY_TAP,TEXT_INPUT,PRESS_CTRL_A}）+ 调用泛型 frozen input/WAIT 序列 API（取代仅 mouse 的 `findMouseSequenceEndExclusive`/`executeMouseSequence` 约束）；两世界地图分支的 edit 序列各落一 frozen queue 请求；Alt+1 为独立 eligible 请求（不并入 mixed edit）。
10. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java` — `PRESS_CTRL_A` 字段规则（无坐标/无 timing）。
**dhxy-cloud-brain（MODIFY）**：
11. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java` — 镜像 +`PRESS_CTRL_A`（与 #5 两仓 byte-identical）。
12. `src/main/java/com/bot/dhxy/service/NavigationService.java` — 世界地图两分支 producer（发 PRESS_CTRL_A/TEXT/`KEY_TAP{enter}`/Alt+mouse 于一 turn INPUT 序列；Alt+1 fallback 独立请求）；八行调用图消解 InputSequences/InputProvider/GameStateUtil/CoordinateHelper/tracker Nav 侧站点。
13. `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` — #9-#12 四 caller→turn 序列/KEY_TAP。
14. `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`(:2743) + `.../task/xiuluo/XiuluoTaskV2.java`(:1655) — Javadoc 提及清理。

### 最终无条件 TEST 写集
DHXY-cr271：
- **`src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`**（**补入**，MODIFY）— 断言 executor **删 `BoundWindowKeyboardService` 直发路径**、approved 词汇映射入**一** frozen 请求、`KEY_DOWN`/`KEY_UP` 仍 `BACKGROUND_KEY_UNSUPPORTED`、无第二 queue。
- `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`（MODIFY）— **仅** branch grouping/单请求/anti-interleave。
- `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`（MODIFY）— real-worker：Alt+1 attempted+success / terminal-failure 零 fallback / non-terminal·未尝试→focused fallback / mixed edit skip HWND / `PRESS_CTRL_A` dispatch(pressCtrlA 一次) / 中断取消停后续 / exact binding / 单 physical execution。
- `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`（MODIFY）+ 双仓 golden `TurnCoreProtocolGoldenJsonTest.java`（PRESS_CTRL_A 值）。
dhxy-cloud-brain：
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`（MODIFY）— producer 两分支原子序+发值+布尔终态。
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java`（**Create**）— active-zero 集 + 44A-retained/五文件 cohort allowlist（双集字面）。
- 三 service guard test（DialogOption/PlayerState/SummonSkill TurnContractTest）=**READ-ONLY**。

### 后续卡与边界（不变）
Cloud 五文件 legacy cohort（InputSequences/CloudInputActionMapper/InputActionScope/InputAction/InputActionType，全 `dhxy-cloud-brain`）→计划 amend `TURN-39C1`（dependsOn 39 caller-zero；同批 DELETE；唯一 compile point；不入 44A）。17-SCC 整体 44A 不动。DHXY 同名三 physical 文件 live（#1-3），与 Cloud 五文件无 cross-repo compile edge。**单一用户决策=TURN-09 前台键盘能力 批准→全量 retire / 否决→TURN-39 PLAN-CONTRACT BLOCKED**。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW10-REPAIR-REDELIVERED FINAL-COMPLETE-MANIFEST 14-PROD+8-TEST LOCALTURNEXECUTOR-ADDED TURNINPUTSTEPEXECUTORCONTRACTTEST-ADDED SELF-CONTAINED ACK=REVIEW10-2205 AWAIT-REVIEW11 2026-07-18T22:10:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #11 - 2026-07-18 22:17 EDT

- verdict: `P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. Review #10's two
  manifest omissions are closed: `LocalTurnActionExecutor.java` and `TurnInputStepExecutorContractTest.java` now
  have exact repository-qualified responsibilities. Communication is normal. TURN-39 remains unopened.

### P1 - The final test manifest omits the fixed owner for the four Wubei caller migrations

- The final production manifest modifies `dhxy-cloud-brain/.../task/wubei/WubeiTask.java` at four active
  `InputSequences`/`InputAction` caller sites and removes its injected `InputSequences` dependency. Earlier
  source-true matrices in this same card freeze
  `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`
  as the MODIFY test owner for those four equivalent sequences.
- The physical test still constructs `WubeiTask` with a positional `null // InputSequences` argument. Removing the
  production field/constructor dependency therefore requires a test construction change even before semantic
  assertions are considered. `OldFacadeRemovalContractTest` can prove active references are zero, but cannot prove
  Alt+C, move/wait/click ordering, exact waits, or the two tracker-click sequences remain baseline-equivalent.
- repair condition: add the exact Wubei whole-task test path to the final unconditional test manifest; update its
  construction for the retired dependency and retain explicit assertions for all four migrated call sites and
  action order/timing. Recount the self-contained manifest after adding the path. The three service guard tests may
  remain READ-ONLY because their physical references are source-guard strings, not constructor dependencies.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-11 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER-A-RETAINED REVIEW10-OMISSIONS-CLOSED WUBEIWHOLETASKTURNCONTRACTTEST-MISSING FOUR-WUBEI-CALLERS+CTOR-DEPENDENCY-UNTESTED COMMUNICATION-NORMAL NO-TURN39-READY NO-MAVEN 2026-07-18T22:17:00-04:00 -->

## EXTERNAL-A REVIEW #11 REPAIR RE-DELIVERY - 2026-07-18T22:22:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW11-REPAIR-2217`。单 P1 闭合（Review#10 两 omission 已确认 closed）。

### P1：Wubei whole-task test 入最终测试 manifest + 构造更正
`dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`（**MODIFY，补入最终 test manifest**）：
- **构造更正**：现行 harness 以定位实参 `null // InputSequences` 构造 `WubeiTask`；production 删注入 `InputSequences` field/ctor 参后，harness 须去该实参（by-type 工厂已按型匹配，随 arity 变化自适应，与 TURN-35 先例同）。
- **断言（四迁移站点+序/时序，逐字保留基线）**：①`:2156` post-accept-prepath alt-c=`PRESS_CTRL_A`? 否——**是 `pressAltC`+`sleep(120)`** bundle→turn `KEY_TAP{Alt+C}`+`WAIT{120}` 一 frozen 请求；②`:2273` moveAndClickLeft→`MOVE_MOUSE`+`WAIT`+`CLICK_LEFT` 序；③`:2791` tracker-green-click→`MOVE_MOUSE`+`WAIT{120}`+`CLICK_LEFT{300}`；④`:4352` 同 ③。断言 action 顺序/exact wait 值/单 frozen 请求/布尔终态（EXECUTED-only→true）。
- 三 service guard test（DialogOption/PlayerState/SummonSkill TurnContractTest）=保持 **READ-ONLY**（其 `InputSequences` 为 source-guard 字符串，非构造依赖，父级已确认无需改）。

### 重计后最终 TEST manifest（自足，9 文件）
DHXY-cr271：①`cloud/turn/TurnInputStepExecutorContractTest.java`（MODIFY）②`cloud/turn/LocalTurnActionExecutorContractTest.java`（MODIFY，仅 branch grouping）③`input/action/InputActionFrozenExclusiveContractTest.java`（MODIFY，real-worker 分支）④`cloud/turn/protocol/TurnProtocolValidatorContractTest.java`（MODIFY）⑤`cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`（MODIFY，golden）。dhxy-cloud-brain：⑥`service/NavigationTurnContractTest.java`（MODIFY，producer 两分支）⑦**`task/wubei/WubeiWholeTaskTurnContractTest.java`（MODIFY，补入，四站点+构造）**⑧`turn/client/OldFacadeRemovalContractTest.java`（Create，双集 allowlist）⑨双仓 golden 第二仓侧（若 ⑤ 为 DHXY 侧，Cloud 侧 `TurnCoreProtocolGoldenJsonTest.java` MODIFY 镜像 PRESS_CTRL_A）。READ-ONLY（非写集，仅记不倒退）：三 service guard test。
**production manifest 不变（14 文件，见 22:10 节）**。Cloud 五文件 cohort→`TURN-39C1`；17-SCC 44A；单一用户决策=TURN-09 键盘能力 approve→retire / reject→BLOCKED。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW11-REPAIR-REDELIVERED WUBEIWHOLETASKTEST-ADDED CTOR-NULL-INPUTSEQ-REMOVED FOUR-CALLERS-ASSERTED TEST-MANIFEST-9-RECOUNTED PROD-14-UNCHANGED ACK=REVIEW11-2217 AWAIT-REVIEW12 2026-07-18T22:22:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #12 - 2026-07-18 22:27 EDT

- verdict: `P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. The Wubei
  whole-task test path, constructor correction, and four caller sequence/timing responsibilities close Review #11.
  Communication is normal. TURN-39 remains unopened.

### P1 - The final manifest creates two owners for the C4 Navigation write set

- Repair #11 keeps `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java` and
  `.../src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java` in the TURN-39 final
  production/test manifest. The already parent-passed TURN-40BP1 contract fixes those exact two paths, plus
  `FiveRingTaskTrackerTurnContractTest.java`, to `TURN-40B-C4`; its dependency is explicitly `39P1 parent report
  review passed`.
- Passing this report would therefore unlock C4 while simultaneously assigning the same physical source/test paths
  to TURN-39, with no ordering edge, merge decision, or single compile point. The card's older collision statements
  predate the finalized C4 contract and are stale. This violates the single-owner rule even though neither Java
  implementation card is currently claimed.
- repair condition: reconcile the two parent-passed contracts into one exact implementation DAG. Freeze one owner
  for `NavigationService`/`NavigationTurnContractTest` (either amend C4 into TURN-39, or retain C4 and remove those
  paths from TURN-39 with explicit capability/source-gate prerequisites), account for C4's
  `FiveRingTaskTrackerTurnContractTest`, and state the one compile/test point. The unresolved user foreground-keyboard
  decision must remain the sole semantic gate; do not open C4 or TURN-39 by merely passing the report.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-12 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER-A-RETAINED WUBEI-TEST-CLOSED NAVIGATION+NAVTEST-DUAL-OWNER TURN39-VS-TURN40B-C4 DAG+COMPILE-POINT-MISSING USER-KEYBOARD-DECISION-STILL-GATE COMMUNICATION-NORMAL NO-TURN39-OR-C4-READY NO-MAVEN 2026-07-18T22:27:00-04:00 -->

## EXTERNAL-A REVIEW #12 REPAIR RE-DELIVERY — RECONCILED IMPLEMENTATION DAG - 2026-07-18T22:33:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW12-REPAIR-2227`（Wubei test/构造/四站点 closed）。单 P1（Navigation dual-owner）闭合，采**选项(b)：保留 C4、从 TURN-39 移除 Navigation 两 path**，并给出单一实施 DAG。此前 collision 语句（早于最终 C4 合同）作废。

### 一、owner 归属更正（单 owner 铁律）
- `dhxy-cloud-brain/.../service/NavigationService.java` + `.../service/NavigationTurnContractTest.java` + `.../task/wuhuan/FiveRingTaskTrackerTurnContractTest.java` = **归 TURN-40B-C4**（父级已过 40BP1 合同固定；dependsOn=`39P1 报告 review passed`）。**从 TURN-39 最终 manifest 移除此三 path**。
- TURN-39 = 计划固定 facade 六文件 scope（Create turn/{TurnGameClient,TurnTaskServicePort,TurnTaskServiceExecutionContext}+Modify remote/{CloudGameClient,CloudTaskServicePort,CloudTaskServiceExecutionContext,CloudTaskServiceMetadata}），单一 integration owner；**不含任何 input-caller 迁移**。

### 二、单一实施 DAG（能力批准路径；否决→全链 BLOCKED）
```
[USER GATE] TURN-09 前台键盘能力 approve  ── 唯一 semantic gate
   │
   ▼
(K) 能力 enablement（DHXY turn-exec+physical worker 写集：InputActionType/InputAction/InputActionWorker/
     cloud.turn.protocol.TurnInputAction[双仓镜像]/TurnInputActionMapper/TurnInputStepExecutor/TurnKeyMapper/
     TurnProtocolValidator + LocalTurnActionExecutor + 其 contract/golden/frozen-exclusive test）
     —— owner：请父级计划冻结为 C4 的前置能力卡（或并入 C4 首段）；compile point=(K) 单独编译通过
   │
   ├─▼ TURN-40B-C4（保留，已过合同）：NavigationService.java 八行 input-caller 迁移
   │     + NavigationTurnContractTest.java + FiveRingTaskTrackerTurnContractTest.java
   │     dependsOn=(39P1 passed)+(K)；compile/test point=C4
   │
   ├─▼ (W) Wubei input-caller 迁移：WubeiTask.java 四站点 + WubeiWholeTaskTurnContractTest.java（构造去 InputSequences 实参）
   │     dependsOn=(K)；compile point=(W)；owner=请父级冻结（C4 sibling caller 卡或并入 C4）
   │
   ▼ (retire 完成后) TURN-39C1：Cloud 五文件 cohort DELETE（InputSequences/CloudInputActionMapper/
     InputActionScope/InputAction/InputActionType，全 dhxy-cloud-brain）
     dependsOn=(K)+(C4)+(W) 使 active-ref→0；compile point=五文件同批删
```
- **TURN-39（facade 六文件）** 与上链**正交**（不碰 input/Navigation caller）；其 owner/依赖照计划 §10。
- **单 compile/test point**：(K) 自编译→C4/W 各自编译（依赖 (K)）→TURN-39C1 删（依赖三者 active-zero）。无中间假编译点；17-SCC 整体仍 44A 不动。
- FiveRing `FiveRingTaskTrackerTurnContractTest.java` 明确归 C4（已入 C4 test，本报告不重派）；`OldFacadeRemovalContractTest`（Create）归能力/收口链，断言 active-zero 双集 allowlist。

### 三、TURN-39P1（本报告）产出边界
本 REPORT-ONLY 卡产出=①InputSequences 全 action→turn 映射（26 值表）②caller live/dead 分类（12 直接 caller）③boolean mapping ④上述 owner DAG 与 (K)/C4/W/39C1/44A 边界。**通过本报告 review 仅解锁"依 (39P1 passed) 的下游卡按其自身合同推进"**，不直接开 TURN-39/C4/(K)/(W)——**唯一 semantic gate=用户前台键盘能力决策**。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW12-REPAIR-REDELIVERED NAV-DUAL-OWNER-RESOLVED-OPTION-B C4-RETAINS-NAV+NAVTEST+FIVERINGTRACKER TURN39-FACADE-6-ONLY RECONCILED-DAG K-C4-W-39C1 SINGLE-COMPILE-POINTS USER-KEYBOARD-SOLE-GATE ACK=REVIEW12-2227 AWAIT-REVIEW13 2026-07-18T22:33:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #13 - 2026-07-18 22:49 EDT

- verdict: `P0/P1/P2=0/2/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. Review #12's
  direct Navigation collision is closed: C4 retains its three fixed source/test paths and TURN-39 no longer owns
  them. The exact ACK is accepted and communication is normal. TURN-39/C4 remain unopened.

### P1-1 - `(K)` and `(W)` are still ownerless alternatives, not an executable card DAG

- Repair #12 says the capability node owner is "a C4 prerequisite card (or C4 first segment)" and the Wubei node
  owner is "a C4 sibling card or merge into C4". Neither node has a section-16 card id, fixed physical report,
  exact unconditional source/test write set, canonical owner/readiness state, or collision result. `TURN-39C1` is
  likewise still only a proposed amendment; the authoritative plan explicitly says it has not been created.
- Impact: two Workers cannot canonical-claim this graph without choosing different ownership shapes, and C4's
  parent-passed three-file write set would be silently expanded if either placeholder were merged into it. The
  stated per-node compile points therefore do not yet identify runnable named-test/compile contracts.
- repair condition: choose one shape, not alternatives. Freeze exact section-16 ids and fixed-card paths for K, W,
  and the five-file retirement cohort (or explicitly amend C4's passed contract), with repository-qualified
  unconditional production/test write sets, dependencies, collision exclusions, named tests and one compile point
  per card. Keep all implementation cards unopened until the user keyboard decision is explicit.

### P1-2 - the claimed TURN-39 facade scope is not source-true

- Repair #12 calls TURN-39 a "six-file" scope but enumerates three Create plus four Modify paths, i.e. seven. More
  importantly, the physical Cloud tree has no `turn/TurnGameClient.java`, `turn/TurnTaskServicePort.java`, or
  `turn/TurnTaskServiceExecutionContext.java`; it already has
  `turn/client/TurnGameClient.java` (`sha256 AFA5EC425031...`) and
  `turn/client/LegacyTaskExecutionTurnContextProvider.java` (`96827E317905...`). The authoritative plan's later
  source-true list names those two client paths plus the four remote files, while section 10's older seven-path
  Create list uses the stale package.
- Impact: "TURN-39 facade only" does not currently say which six/seven physical symbols survive, are modified, or
  are superseded. Passing it would leave the integration owner able to create a second facade package or omit the
  live legacy provider from the retirement boundary.
- repair condition: reconcile section 10 with the current physical tree and the later source-true list. Provide one
  repository-qualified TURN-39 manifest with exact CREATE/MODIFY/READ-ONLY/DELETE disposition for every live/stale
  facade path, correct count, symbols, named test ownership, and dependency on the chosen K/W/C4/39C1 sequence.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-13 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED REVIEW12-NAV-COLLISION-CLOSED K+W+39C1-OWNERLESS-ALTERNATIVES TURN39-FACADE-COUNT+PACKAGE-NOT-SOURCE-TRUE USER-KEYBOARD-SOLE-GATE COMMUNICATION-NORMAL NO-IMPLEMENTATION-CARD-READY NO-MAVEN 2026-07-18T22:49:00-04:00 -->

## EXTERNAL-A REVIEW #13 REPAIR RE-DELIVERY — ONE FROZEN CARD DAG + SOURCE-TRUE FACADE - 2026-07-18T22:55:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW13-REPAIR-2249`（Nav collision closed）。两 P1 闭合：选**单一形态**（新建 sibling 卡，不扩 C4 已过合同），并以物理树校正 facade。

### P1-2：source-true TURN-39 facade manifest（6 文件，repo-qualified，逐 disposition）
物理树校正（section-10 的 `turn/TurnGameClient` 等 Create 路径为 stale——该类已存在于 `turn/client/`）：
| # | dhxy-cloud-brain path | blob | disposition | 理由 |
|---|---|---|---|---|
| 1 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java` | `fc85dc1f` | **READ-ONLY** | turn-native facade 已存在（非 Create）；业务经此消费 typed capture/input/local-service |
| 2 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java` | `49a655c5` | **READ-ONLY**（39 内）；物理 DELETE 归 44A | live legacy provider，收口后零 business caller，字节留 44A |
| 3 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java` | `9b336ee4` | **READ-ONLY**（39 内）；DELETE 归 **44A**（17-SCC #1） | SCC 成员，不在 39 改 |
| 4 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java` | `da0dcc47` | **READ-ONLY**（39 内）；DELETE 归 **44A**（17-SCC #13） | SCC 成员 |
| 5 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java` | `60ab2a9a` | **READ-ONLY**（39 内）；DELETE 归 **44A**（17-SCC #12） | SCC 成员 |
| 6 | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java` | `18965ced` | **SURVIVING/READ-ONLY** | SCC 外幸存 typed metadata，无删除 owner |
**结论修正**：TURN-39 计划标题"facade 收口"的实际 source-true 内容=**上 6 文件在 39 阶段全 READ-ONLY**（业务侧 input/Navigation caller 迁移由 (K)/C4/(W) 完成使这些 facade 变 zero-business-caller；3 SCC 成员+legacy provider 物理删归 44A，Metadata 幸存）。故 TURN-39 作为独立 integration 卡在本 input-bridge 语境下**无新 production 写集**——其"收口"由下游 caller 卡达成；section 10 的 3-Create+4-Modify(=7) 计数/包名 stale，以物理树 6-READ-ONLY 为准，请父级据此更 section 10。

### P1-1：单一冻结卡 DAG（精确 section-16 id + 无条件写集；实施卡在用户决策前全不开）
形态=**新建三 sibling 卡 + 保留 C4**（不并入 C4，避免扩其已过 3-file 合同）：
- **`TURN-39K`（capability enablement，INTEGRATION）**：write set（无条件，repo-qualified）：DHXY-cr271 `input/action/{InputActionType,InputAction,InputActionWorker}.java`+`cloud/turn/protocol/TurnInputAction.java`+`cloud/turn/{TurnInputActionMapper,TurnInputStepExecutor,TurnKeyMapper,LocalTurnActionExecutor}.java`+`cloud/turn/protocol/TurnProtocolValidator.java`（`input/InputProvider.java` READ-ONLY）；dhxy-cloud-brain `cloud/turn/protocol/TurnInputAction.java`（镜像）。test：DHXY-cr271 `cloud/turn/{TurnInputStepExecutorContractTest,LocalTurnActionExecutorContractTest}.java`+`input/action/InputActionFrozenExclusiveContractTest.java`+`cloud/turn/protocol/TurnProtocolValidatorContractTest.java`+双仓 golden。dependsOn=**用户键盘能力 approve**。collision=与 C4/W 写集不交（C4=NavigationService+2test；W=WubeiTask+1test）。compile point=39K 自编译+其 named test。
- **`TURN-40B-C4`（保留，已过合同）**：`service/NavigationService.java`+`service/NavigationTurnContractTest.java`+`task/wuhuan/FiveRingTaskTrackerTurnContractTest.java`。dependsOn=`39P1 passed`+`39K`。compile/test point=C4。
- **`TURN-39W`（Wubei caller retirement，INTEGRATION）**：`task/wubei/WubeiTask.java`（四站点+删 InputSequences field/ctor）+`task/wubei/WubeiWholeTaskTurnContractTest.java`（构造去 InputSequences 实参）+`task/wuhuan/FiveRingTaskV2.java`(:2743 Javadoc)+`task/xiuluo/XiuluoTaskV2.java`(:1655 Javadoc)。dependsOn=`39K`。compile point=39W。
- **`TURN-39C1`（Cloud legacy cohort DELETE，DELETE）**：`input/InputSequences.java`+`input/action/{CloudInputActionMapper,InputAction,InputActionType,InputActionScope}.java`（5 文件，全 dhxy-cloud-brain）+`turn/client/OldFacadeRemovalContractTest.java`（Create，active-zero 双集 allowlist）。dependsOn=`39K`+`C4`+`39W`（使 active-ref→0）。compile point=五文件同批删。**非 17-SCC，故可 44A 前独立删，不造 SCC 假编译点。**
DAG：`[用户 gate]→39K→{C4, 39W}→39C1`；TURN-39（facade 6 READ-ONLY）正交、无 input 写集。三 service guard test 全程 READ-ONLY。**所有实施卡（39K/C4/39W/39C1）在用户前台键盘决策明确前不开**；请父级将 39K/39W/39C1 冻结入 section 16（或明示并入既有卡的精确形态）。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW13-REPAIR-REDELIVERED FACADE-6-SOURCE-TRUE-READONLY SECTION10-STALE-FLAGGED ONE-SHAPE-39K+C4+39W+39C1 EXACT-WRITESETS-DEPS-COMPILE-POINTS ALL-UNOPENED-UNTIL-USER-GATE ACK=REVIEW13-2249 AWAIT-REVIEW14 2026-07-18T22:55:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #14 - 2026-07-18 23:05 EDT

- verdict: `P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`; External A remains sole report owner. Review #13's
  two findings are materially closed: the live six-path facade census is source-true, and the single
  `39K -> {C4,39W} -> 39C1` shape removes the ownership alternatives. The exact ACK is accepted; communication is
  normal. No implementation card is opened.

### P1 - TURN-39K changes the mirrored enum but omits the mirrored exhaustive validator and test owner

- Current DHXY and Cloud `TurnProtocolValidator.java` are byte-identical and both have an exhaustive
  `switch (TurnInputAction)` in `requireInput` (lines 178-229). Repair #13 adds `PRESS_CTRL_A` to both repositories'
  `TurnInputAction.java`, but its unconditional 39K production set names only the DHXY validator; its tests likewise
  name the DHXY validator contract test plus mirrored golden tests, not the Cloud validator contract owner.
- Impact: the Cloud enum addition without a Cloud validator branch leaves the exhaustive switch non-compiling.
  Even if a default were added elsewhere, the card would not prove the required no-coordinate/no-key/no-text/no-
  timing field shape on the Cloud endpoint. "39K self compile" also does not identify both repository compile gates.
- repair condition: add the Cloud repository-qualified `TurnProtocolValidator.java` and
  `TurnProtocolValidatorContractTest.java` to 39K, keep the two production protocol files byte-identical, freeze the
  exact `PRESS_CTRL_A` empty-field validator branch and mirrored positive/negative cases, and state DHXY plus Cloud
  named-test/compile points. Recount 39K's complete production/test manifest. The user keyboard decision remains
  the sole semantic gate; do not open 39K/C4/39W/39C1.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE-REVIEW-14 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER-A-RETAINED REVIEW13-DAG+FACADE-CLOSED TURN39K-CLOUD-VALIDATOR+TEST-MISSING EXHAUSTIVE-SWITCH-COMPILE-FAIL DUAL-REPO-COMPILE-GATES-MISSING USER-KEYBOARD-SOLE-GATE COMMUNICATION-NORMAL NO-IMPLEMENTATION-CARD-READY NO-MAVEN 2026-07-18T23:05:00-04:00 -->

## EXTERNAL-A REVIEW #14 REPAIR RE-DELIVERY - 2026-07-18T23:10:00-04:00

ACK `PARENT-A-TURN39P1-REVIEW14-REPAIR-2305`（DAG+facade closed）。单 P1（39K 漏 Cloud validator+test）闭合。

### P1：39K 补 Cloud validator + test，双仓 byte-identical，双 compile gate
source 事实（只读）：DHXY 与 Cloud `TurnProtocolValidator.java` 现 **byte-identical**（两仓 blob 均 `3d5bd786`），`requireInput` 内 `switch(TurnInputAction)`(:178-229) 为**穷尽 no-default**；两仓 `TurnProtocolValidatorContractTest.java` 均在 `src/test/java/com/bot/dhxy/cloud/turn/protocol/`。⇒ 仅改 DHXY validator 会使 Cloud 侧穷尽 switch 缺 `PRESS_CTRL_A` case=**编译失败**。

**39K 最终 PRODUCTION manifest（重计，无条件，repo-qualified）**：
DHXY-cr271：①`input/action/InputActionType.java` ②`input/action/InputAction.java` ③`input/action/InputActionWorker.java` ④`cloud/turn/protocol/TurnInputAction.java` ⑤`cloud/turn/TurnInputActionMapper.java` ⑥`cloud/turn/TurnInputStepExecutor.java` ⑦`cloud/turn/TurnKeyMapper.java` ⑧`cloud/turn/LocalTurnActionExecutor.java` ⑨`cloud/turn/protocol/TurnProtocolValidator.java`（`input/InputProvider.java`=READ-ONLY）。
dhxy-cloud-brain：⑩`cloud/turn/protocol/TurnInputAction.java`（镜像 +PRESS_CTRL_A，与 ④ byte-identical）⑪**`cloud/turn/protocol/TurnProtocolValidator.java`（补入；镜像 +PRESS_CTRL_A case，与 ⑨ byte-identical）**。
**PRESS_CTRL_A validator 分支（双仓逐字同）**：`case PRESS_CTRL_A ->` `requirePoint` 不调用；`require(x==null&&y==null&&endX==null&&endY==null&&scrollDelta==null&&key==null&&text==null&&clickDelayMs==null&&queueHoldMs==null, "ctrl+a input has unexpected fields")`——**无坐标/无 key/无 text/无 timing**（同 `PRESS_ENTER`/Alt KEY_TAP 形，Ctrl+A 由物理单 action 表达故 spec 全空）。

**39K 最终 TEST manifest（重计）**：
DHXY-cr271：`cloud/turn/protocol/TurnProtocolValidatorContractTest.java`（MODIFY，PRESS_CTRL_A 正例 empty-field 通过+负例任一字段非空 throw）、`cloud/turn/{TurnInputStepExecutorContractTest,LocalTurnActionExecutorContractTest}.java`、`input/action/InputActionFrozenExclusiveContractTest.java`、`cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`（golden）。
dhxy-cloud-brain：**`cloud/turn/protocol/TurnProtocolValidatorContractTest.java`（补入，MODIFY，同上正/负例）**、`cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`（golden 镜像）。
**双 compile/test gate**：DHXY-cr271 编译+其 named test；dhxy-cloud-brain 编译+其 named test；两仓 validator/协议文件 **byte-identical 强制**（golden 双仓核）。

### DAG / 其余不变
`[用户键盘 gate]→39K→{TURN-40B-C4, TURN-39W}→TURN-39C1`；facade 6 文件 READ-ONLY（source-true）；17-SCC 44A；三 service guard test READ-ONLY；C4 保留三 path 不扩；39W=Wubei 四站点+test 构造；39C1=Cloud 五文件同批 DELETE。所有实施卡用户决策前不开。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-39P1 EXTERNAL-A REVIEW14-REPAIR-REDELIVERED 39K-CLOUD-VALIDATOR+TEST-ADDED BYTE-IDENTICAL-3d5bd786 PRESS-CTRL-A-EMPTY-FIELD-BRANCH DUAL-COMPILE-GATES 39K-11PROD+7TEST ACK=REVIEW14-2305 AWAIT-REVIEW15 2026-07-18T23:10:00-04:00 -->

## Parent SOURCE+TEST SOURCE REVIEW #15 - PASSED - 2026-07-18 23:15 EDT

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`; External A's report owner is released. Review #14's
  sole P1 is closed: 39K now owns both byte-identical validators, both validator contract tests, the mirrored enum
  and golden tests, with an explicit empty-field `PRESS_CTRL_A` branch and DHXY plus Cloud named-test/compile gates.
- reviewed contract: source-true six-facade classification; 26-value input mapping and 12-caller classification;
  COMPLETED-only boolean truth; exact single-owner DAG
  `[user foreground-keyboard gate] -> 39K -> {TURN-40B-C4,39W} -> 39C1`; C4 retains its passed three paths; 39W owns
  the four Wubei caller migration and test construction; 39C1 owns the five-file Cloud legacy cohort retirement;
  17-SCC remains wholly owned by 44A. No second queue/store, stub, constant result or copied business algorithm is
  authorized. `无已批准业务差异；按基线等价迁移`.
- this approval closes the report only. 39K/C4/39W/39C1 remain `NOT READY / ZERO OWNER` until the user explicitly
  approves the exact-window, globally serialized foreground Ctrl+A/Unicode/Enter and retained Alt+1 fallback
  capability. No Java, Maven, runtime, application, server, Task, UI, capture or input was run.

<!-- TRUE_EOF: TURN-39P1 PARENT-SOURCE+TEST-SOURCE-REVIEW-15 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED REPORT-CLOSED SOURCE-TRUE-FACADE+MAPPING+BOOLEAN+DAG-FROZEN 39K+C4+39W+39C1-NOT-READY-ZERO-OWNER USER-FOREGROUND-KEYBOARD-SOLE-GATE NO-JAVA NO-MAVEN NO-RUNTIME 2026-07-18T23:15:00-04:00 -->

## Parent PLAN-CONTRACT CORRECTION - USER KEYBOARD INVARIANT - 2026-07-19 00:30 EDT

- The user corrected the premise after Review #15: keyboard input is always exact-HWND background input; only
  mouse input may require foreground. The former `USER FOREGROUND-KEYBOARD DECISION REQUIRED` gate was a parent
  planning error, not an unresolved business choice.
- Superseding implementation shape:
  `TURN-39K -> {TURN-40B-C4, TURN-39W} -> TURN-39C1`. TURN-39K executes keyboard directly and concurrently per
  frozen HWND through `BoundWindowKeyboardService`; it does not use the global mouse/input worker, adds no keyboard
  queue/protocol/store, and permits no focused keyboard fallback. Only mouse remains foreground and globally serialized.
- The existing wire actions `KEY_TAP/KEY_DOWN/KEY_UP/TEXT_INPUT` are sufficient. Review #15's proposed mirrored
  `PRESS_CTRL_A` enum/validator expansion is superseded and removed from the implementation write set.
- TURN-39K now has a fixed original card and is `READY / ZERO OWNER`. C4/39W wait for 39K source pass; 39C1 waits
  for 39K+C4+39W active-zero. The report remains passed/closed and A remains released.

<!-- TRUE_EOF: TURN-39P1 PARENT-PLAN-CONTRACT-CORRECTION USER-INVARIANT=PER-WINDOW-PARALLEL-EXACT-HWND-BACKGROUND-KEYBOARD+ONLY-MOUSE-FOREGROUND-GLOBAL-SERIAL FALSE-FOREGROUND-DECISION-GATE-REMOVED DAG=39K-THEN-C4+39W-THEN-39C1 TURN39K-READY-ZERO-OWNER NO-PRESS_CTRL_A-WIRE-ENUM NO-KEYBOARD-QUEUE NO-JAVA NO-MAVEN 2026-07-19T00:34:00-04:00 -->

## Parent Downstream Source-Gate Release - 2026-07-19 03:05 EDT

- `TURN-39K` Repair #2 passed parent source+test source review `P0/P1/P2=0/0/0`; owner is released and main
  compile is green. The named Maven family remains blocked by unrelated dirty testCompile.
- The frozen DAG now advances to its sibling stage. Fixed original cards `TURN-39W` and `TURN-40B-C4` are
  `READY / ZERO OWNER / UNASSIGNED`; their physical EOFs are the only claim authority. This is not assignment.
- `TURN-39C1` remains `NOT READY / ZERO OWNER`, waiting for 39K + 39W + C4 active references to reach zero.
  User keyboard invariant remains exact-HWND background and cross-window parallel; only mouse is foreground/global-
  serial. No business difference or contract expansion is introduced.

<!-- TRUE_EOF: TURN-39P1 PARENT-DOWNSTREAM-GATE-RELEASE 39K-REVIEW2-PASSED 39W+C4-READY-ZERO-OWNER-UNASSIGNED 39C1-WAITS-ACTIVE-ZERO DAG-UNCHANGED 2026-07-19T03:05:00-04:00 -->

## Parent Parallel Claim Reconciliation - 2026-07-19 03:15 EDT

- Original-card physical EOFs establish External A as TURN-39W owner and External C as TURN-40B-C4 owner. Their
  frozen Cloud write sets are disjoint, so the sibling stage may proceed in parallel without assignment by parent.
- Section 16 has been restored to its contracted 88 Sprint rows: 39K/39W/39C1 remain fixed implementation subcards
  represented inside TURN-39's main row, not additional Sprint rows. Their original cards remain authoritative.
- 39C1 remains closed until both active caller cards reach source pass/active-zero. No contract or business change.

<!-- TRUE_EOF: TURN-39P1 PARENT-PARALLEL-CLAIM-RECONCILIATION 39W-OWNER-A C4-OWNER-C DISJOINT-WRITESETS SPRINT-REGISTRY=88 SUBCARDS-REMAIN-FIXED 39C1-WAITS 2026-07-19T03:15:00-04:00 -->

## Parent Downstream Contract Correction - TURN-39C1 - 2026-07-18 21:51 UTC

- C4 and 39W are now parent source-passed with owners released, but the expected 39C1 active-zero condition is not
  physically true: repository-wide symbol audit finds `NavigationService` as the sole active external consumer of
  `InputActionScope` (import plus prepare/OCR/scroll cancellation checks). The other four cohort types have no active
  external type edge; explanatory Javadoc/test strings are not compile ownership.
- This corrects the Review #15 downstream manifest without reopening its accepted keyboard/mouse architecture. The
  fixed 39C1 write set adds sequential MODIFY ownership of `NavigationService.java` and
  `NavigationTurnContractTest.java`; every current scope check must use the existing direct `TaskCheckpoint`/turn-
  outcome stop channel at the same workflow boundary. No helper wrapper, false constant, retry, fallback, TTL,
  second cancellation mechanism, queue, protocol, store or business algorithm is permitted.
- After active symbol reference reaches zero, delete the five Cloud legacy files together and create the frozen
  `OldFacadeRemovalContractTest`. The corrected card is `READY / ZERO OWNER / UNASSIGNED`; the parent did not assign
  it. `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-39P1 PARENT-DOWNSTREAM-CONTRACT-CORRECTION 39C1-SOURCE-TRUE NAVIGATION-INPUTACTIONSCOPE-SOLE-EXTERNAL-EDGE ADD-NAV+TEST-SEQUENTIAL-WRITESET EXISTING-TASKCHECKPOINT+TURN-OUTCOME-ONLY FIVE-DELETE-SAME-BATCH READY-ZERO-OWNER-UNASSIGNED NO-JAVA NO-MAVEN 2026-07-18T21:51:19Z -->
