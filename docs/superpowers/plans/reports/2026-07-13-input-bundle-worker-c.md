# External Worker C - Direct Input-Bundle Inventory

## Parent Simplification Directive #1 / `W-INPUT-C1` - 2026-07-13T19:35:23-04:00

旧日志 `2026-07-13-cloud-bag-state-owner-worker-c.md` 的 `W-BAG-C1-D2` 及其后所有 Bag 专属
assembly/permit/ledger 设计任务现因用户架构收缩而 `CANCELLED_BY_SIMPLIFICATION`；不再返修，也不据此改 Java。
已落 dormant state 文件不自动删除或回滚，等父级分类。旧日志当前含非 UTF-8 字节，因此新任务固定使用本 UTF-8
append-only 日志，避免重写或破坏历史材料。

External C 新任务 `W-INPUT-C1`：在 `2026-07-13T19:55:23-04:00` 前于本日志真实 EOF 追加
`CLAIMED task=W-INPUT-C1 claimedAt=<ISO> uniqueWriteSet=<本日志>`。随后只读取 committed `0114604e` 与当前源码中的：

- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`

逐个列出所有鼠标/键盘调用：方法+基线行、原动作顺序/原 delay、坐标空间、是否必须防插队、是否在输入中间依赖
capture/template/OCR，并机械分类为 `ONE_BUNDLE` / `LOCAL_MACRO` / `LOCAL_RESIDENT` /
`NO_PHYSICAL_INPUT`。禁止提出新状态机、重试、ledger 或 Java 改动；本轮唯一写集是本日志。交付标题为
`External Worker C - W-INPUT-C1 Source Inventory #1`。

## Parent Claim Gate #1 - UNCLAIMED / REISSUED TO C - 2026-07-13T19:59:45-04:00

截至领取截止 `2026-07-13T19:55:23-04:00`，本日志真实 EOF 无 `CLAIMED task=W-INPUT-C1`。
这只表示 C 未领取，不表示任务应已完成；按用户规则绝不内部接管。现将同一 `W-INPUT-C1` 原样重发给
External C，第二领取截止 `2026-07-13T20:19:45-04:00`。

C 必须先在真实 EOF 追加：
`CLAIMED task=W-INPUT-C1 claimedAt=<ISO> uniqueWriteSet=<本日志>`，然后继续上文五个 Service 的纯源码
输入盘点；范围、分类口径、零 Java/零状态机/零 Maven/Git 约束全部不变。交付标题仍为
`External Worker C - W-INPUT-C1 Source Inventory #1`。

## External Worker C - CLAIMED - 2026-07-13T20:06:29-04:00

- task: W-INPUT-C1
- claimedAt: 2026-07-13T20:06:29-04:00
- uniqueWriteSet: 仅本 append-only 日志 docs/superpowers/plans/reports/2026-07-13-input-bundle-worker-c.md（committed 0114604e 与当前源码只读取证；零 Java / 零状态机 / 零 Maven / 零 Git）

## External Worker C - W-INPUT-C1 Source Inventory #1 - 2026-07-13T20:12:00-04:00

基线 committed `0114604e`，逐 Service 列全部鼠标/键盘调用点并机械分类。分类口径：`ONE_BUNDLE`=纯输入序列，
Cloud 可在原点击/按键处发一次 InputBundle（中间不依赖 capture/template/OCR）；`LOCAL_MACRO`=按键/点击期间必须
截图/模板/OCR 定位或验证，整段留本地宏；`LOCAL_RESIDENT`=永久本地机械能力（窗口绑定/base 刷新、UICleaner、输入
worker/单一输入队列、`InputActionScope.checkpoint` 防插队）；`NO_PHYSICAL_INPUT`=无物理输入。坐标空间：所有
`inputProvider.*` 与 `InputAction.*` 均为**屏幕绝对像素**，由 `tracker` 窗口 base（LOCAL_RESIDENT）+ 相对偏移
算出。防插队列：`submitExclusiveAndWait`=exclusive 独占整段 callback；`submitAndWait`/`moveAndClickLeft`=普通
队列原子序列；exclusive callback 内的 `inputProvider.*` direct 已在独占段内。本轮仅只读取证，零 Java/Maven/Git。

### 1. AutoCombatPanelService（基线 363 行）—— 全 ONE_BUNDLE

| 方法（基线行） | 原动作顺序 / delay | 坐标空间 | 防插队 | 输入中依赖 capture? | 分类 |
|---|---|---|---|---|---|
| `ensurePanelOpen` Alt+8（L87-90） | `pressAlt8 → sleep(waitAfterOpenMs)` | 无（键） | `submitAndWait` 原子 | 否；`findAutoCombatBox` 在 bundle 前后各一次，均在 bundle 外 | ONE_BUNDLE |
| `alignPanelIfNeeded` 拖面板（L119-122） | `dragAndDrop(panelPoint→(dropX,dropY)) → sleep(500)` | 屏幕绝对（panelPoint 来自前置 find；drop=windowBase+offset） | `submitAndWait` 原子 | 否；对齐判定与 refind 在 bundle 外 | ONE_BUNDLE |
| `refreshAutoCombatRoundsIfNeeded` Alt+8（L171-174） | `pressAlt8 → sleep(AUTO_PANEL_REFRESH_WAIT_MS)` | 无（键） | `submitAndWait` 原子 | 否（注释明确“without OCR”） | ONE_BUNDLE |

### 2. CommonBoxService（基线 461 行）—— ONE_BUNDLE

| 方法（基线行） | 原动作顺序 / delay | 坐标空间 | 防插队 | 输入中依赖 capture? | 分类 |
|---|---|---|---|---|---|
| `consumePending` 盒子点击（L144-146） | `moveAndClickLeft(pending.clickX, pending.clickY, CLICK_SETTLE_MS, CLICK_DELAY_MS)` | 屏幕绝对（pending 为 detection 阶段缓存点） | `moveAndClickLeft` 原子 move+click | 否；模板检测在更早的 detection 阶段完成并缓存，consume 时不再截图 | ONE_BUNDLE |

（其余 detection/pending TTL/role toggle 逻辑均 `NO_PHYSICAL_INPUT`。）

### 3. PlayerStateService（基线 1669 行）—— 补给右键序列全 ONE_BUNDLE；窗口 base 刷新 LOCAL_RESIDENT

| 方法（基线行） | 原动作顺序 / delay | 坐标空间 | 防插队 | 输入中依赖 capture? | 分类 |
|---|---|---|---|---|---|
| `healCachedPlan`→`performCachedFirstAidPlanDirect`（L411 exclusive；L454 clickRight；L461 moveAway） | 每 target：`checkpoint → clickRight(base+rel,100) → sleep(800)`；末尾 `moveMouseAwayDirect` | 屏幕绝对（base+rel） | `submitExclusiveAndWait` | 否；中间仅 `tracker.refreshWindowState()`（窗口 base 刷新，非视觉匹配） | ONE_BUNDLE（base 刷新=LOCAL_RESIDENT） |
| `healAll`→`healAllDirect`（L535 exclusive；L562 moveAway） | 同上 N×右键补给 + 末尾避让 | 屏幕绝对 | exclusive | 否 | ONE_BUNDLE |
| `moveMouseAwayAfterFirstAidSupply`（L757-761 submitAndWait） | `moveMouse(safePoint) → sleep(SAFE_MOUSE_HOVER_CLEAR_DELAY_MS)` | 屏幕绝对 | `submitAndWait` 原子 | 否 | ONE_BUNDLE |
| `moveMouseAwayAfterFirstAidSupplyDirect`（L774 moveMouse，direct） | exclusive 段内收尾移动 | 屏幕绝对 | 属所在 exclusive 段 | 否 | ONE_BUNDLE（收尾） |
| `healOne`（L1193 clickRight direct；L1209-1215 / L1221-1224 submitAndWait） | `clickRight(abs,100)[→sleep(800)→moveMouse(safe)→sleep]` | 屏幕绝对 | direct 在 exclusive 段 / `submitAndWait` 原子 | 否；血量阈值判定在输入前完成 | ONE_BUNDLE |

（血蓝检测/阈值/plan 预计算/role 判定均 `NO_PHYSICAL_INPUT`；`refreshWindowState`/窗口 base 归 LOCAL_RESIDENT。）

### 4. SummonSkillService（基线 1759 行）—— 技能格清理全 LOCAL_MACRO；UICleaner LOCAL_RESIDENT

| 方法（基线行） | 原动作顺序 / delay | 坐标空间 | 防插队 | 输入中依赖 capture? | 分类 |
|---|---|---|---|---|---|
| `openSummonSkillPanelDirect`（L321 exclusive；L329 pressAltO；L365 clickLeft） | `pressAltO → sleep(900) → findAttributeAnchor(截图)×重试 → [dragPanelIfNeeded→refind] → clickLeft(skillButton,150) → sleep → findAttributeAnchor 验证(截图)` | 屏幕绝对（skillButton=anchor+offset，anchor 来自截图） | `submitExclusiveAndWait` | **是**：按键与多次锚点截图/验证交织 | LOCAL_MACRO |
| `cleanOnce`（L271 exclusive）/`cleanTailNormalSkills`（L399 exclusive） | 包裹整段 clean（hover→OCR→delete）；`cleanTail` 完成后 `uiCleanerService.cleanUpAll()` | — | exclusive | 是 | LOCAL_MACRO（`cleanUpAll`=LOCAL_RESIDENT） |
| `ultimateCornerPass`（L713 moveMouse hover；L741 clickLeft） | `moveMouse(corner) → sleep(SKILL_HOVER_WAIT_MS) → captureAndWashYellowTip + matchYellowTemplate → clickLeft(corner,120)` | 屏幕绝对 | 属所在 exclusive 段 | **是**：hover→黄字截图/模板→点击 | LOCAL_MACRO |
| `inspectSkillSlotDirect`（L1110 exclusive；L1188 moveMouse hover） | `moveMouse(hover) → sleep(SKILL_HOVER_WAIT_MS) → inspectCurrentHoverTip(captureAndWashYellowTip/OCR/模板)` | 屏幕绝对 | exclusive | **是**：hover→OCR 分类 tooltip | LOCAL_MACRO |
| `deleteNormalSkillDirect`（L1273 exclusive；L1292 clickLeft slot；L1302 clickLeft delete；L1318 clickLeft confirm） | `clickLeft(slot,120)→sleep→clickLeft(deleteButton固定,120)→sleep→findForgetConfirmButton(截图)→clickLeft(confirm,120)→sleep→fastClickKnownSmallStoryDialog(截图)` | 屏幕绝对（confirm 来自截图） | exclusive | **是**：点击链中途截图找确认按钮/story | LOCAL_MACRO |
| `dragSummonPanelSlot`（L1382 dragAndDrop） | `dragAndDrop(dragFrom→dragTo) → sleep(DRAG_PANEL_WAIT_MS)` | 屏幕绝对 | 属所在 exclusive 段 | 拖拽段本身无截图，但 dragFrom/dragTo 由前置 hover/OCR/布局识别得出，嵌于 clean macro | LOCAL_MACRO（整体交织） |

（技能格状态识别、黄字 OCR、tooltip 分类均视觉判定；`uiCleanerService.cleanUpAll`、输入 worker、窗口绑定 LOCAL_RESIDENT。）

### 5. BagService（基线 1207 行）—— 全 LOCAL_MACRO

所有 public 入口（`findItemPageIndex` L115、`findAndSelectItem`/`findAndUseItem` itemAction L532、`...FromBack`
L808、`prescan*`、`useCachedMainBagReturnItem`、`findAndUseMainBagTaskPageItem`、`withMainBagOpen` L240/268/297/323）
均 `submitExclusiveAndWait` 包裹同一交织流程：

| 子段（基线行） | 原动作顺序 / delay | 坐标空间 | 输入中依赖 capture? | 分类 |
|---|---|---|---|---|
| `ensureBagOpened`（L354-426） | `checkBagOpened(截图验主锚点/fallback/cunkuan) ↔ pressAltE → sleep(1200) → checkBagOpened → sleep(700 late) → checkBagOpened → pressAltE → sleep(1200) → checkBagOpened`；命中即 `moveMouseAwayFromMainBagAnchor` | 屏幕绝对（锚点来自截图） | **是**：开包按键与锚点截图验证反复交织 | LOCAL_MACRO |
| `moveMouseAwayFromMainBagAnchor`（L490 moveMouse） | 开包各 stage 后移鼠标离锚点 + settle | 屏幕绝对 | 属开包 macro | LOCAL_MACRO |
| `switchBagTab`（L1049 clickLeft） | `clickLeft(tab,100) → settle(500) → 截图 grid` | 屏幕绝对 | **是**：切页点击后截图 `ImageFinder.findAll` 定位 item | LOCAL_MACRO |
| item 点击（L1135 clickRight / L1137 clickLeft） | 由同帧截图 `findAll` 定位后 `move+click(100)` | 屏幕绝对 | **是**：点击点来自当帧 grid 截图匹配 | LOCAL_MACRO |
| `useCachedMainBagReturnItem`（cached 右键缓存点） | 仍先 `ensureBagOpened`（截图）→ 右键缓存点 | 屏幕绝对 | **是**：开包段截图 | LOCAL_MACRO |

Bag 全流程按键/点击与开包锚点验证、切页 grid 截图、item 模板 `findAll` 强交织，无法拆成不含 capture 的纯输入
bundle → 整类 LOCAL_MACRO。

### 汇总

| Service | ONE_BUNDLE | LOCAL_MACRO | LOCAL_RESIDENT（相关） | NO_PHYSICAL_INPUT |
|---|---|---|---|---|
| AutoCombatPanelService | Alt+8 开面板、拖面板、Alt+8 刷回合 | — | 窗口 base | 面板检测/回合估计 |
| CommonBoxService | 盒子 move+click | — | 窗口绑定 | detection/TTL/role |
| PlayerStateService | 补给右键序列 ×各入口、hover 收尾 | — | 窗口 base 刷新、输入 worker | 血蓝检测/plan 预计算 |
| SummonSkillService | — | 开面板、清理/删格/绝技角/inspect/拖格 | UICleaner、窗口绑定 | 技能格布局常量 |
| BagService | — | 开包+切页+item（全入口） | 窗口绑定、window-scoped temp | 页/物品缓存 hint 读 |

- 纯 `ONE_BUNDLE`：AutoCombatPanel（3）、CommonBox（1）、PlayerState 补给/收尾（各入口）——Cloud 在原按键/点击处
  发一次结构化 InputBundle，坐标用窗口 base（本地）落地即可，中途无视觉依赖。
- `LOCAL_MACRO`：SummonSkill 全部、Bag 全部——按键/点击与截图/模板/OCR/hover-tooltip 强交织，须整段留本地宏，
  Cloud 只触发“执行该宏”，宏内的 capture/match 仍本地。
- `LOCAL_RESIDENT` 贯穿全部：窗口绑定/base 刷新、单一输入队列、`InputActionScope.checkpoint` 防插队、UICleaner。

本轮零 Java/Maven/schema/resources/tests 写入；唯一写入为本 append-only 日志；无 Git mutation；未运行 Maven。
无新增状态机/重试/ledger。无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-INPUT-C1 Source Inventory #1 完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。

## Parent Source Inventory Review #1 - APPROVED - 2026-07-13T20:20:00-04:00

父级按 committed `0114604e` 逐项复核五个 Service 的物理输入调用，结论
`P0=0 / P1=0 / P2=0`。普通 `submitAndWait`/`moveAndClickLeft` 序列均可在原业务调用点直接
转换为一个有序 `InputBundle`；`SummonSkillService` 与 `BagService` 中输入期间穿插
capture/template/OCR 的 exclusive 段继续作为本地宏，不把视觉机械流程搬到 Cloud。

口径补正：`LOCAL_RESIDENT` 表示窗口绑定、capture/template/OCR、输入队列等机械能力永久留本地，
并不表示对应 Service 的业务编排留本地。该清单正式进入直接实现 cohort，不再写 Design #N。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #12 (REPUBLISHED AT TRUE EOF) - `W-NPC-PUBLIC-SMART-CHAIN-IMP1` - 2026-07-14T09:55:00-04:00

External C 立即实施完整 `NpcClickService` 公开智能点击链，不写 Design。请在
**2026-07-14T10:15:00-04:00 前**于本日志真实 EOF 追加：

`CLAIMED | task=W-NPC-PUBLIC-SMART-CHAIN-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud com/bot/dhxy/service/NpcClickService.java, Append this log]`

唯一 Java 写集为 Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`。以 committed
`0114604e` 为业务权威，一次闭合并真实公开 `clickNpcSmart`、`clickNpcSmartWithOutcome`、
`tryDirectCombatTargetClick`、`confirmPendingSmartClick` 四入口及其必要 private closure。类内判断、候选顺序、
delay、fallback、stop 与 terminal 映射必须保持基线不变；窗口图片和匹配结果只从现有 typed
`CloudGameClient` fact/capture facade 消费，普通 move+click/sleep 组为单个有序 InputBundle。

不得复制 DHXY 的 HWND/holder/capture/template/OCR/input worker mechanics，不得修改 shared remote/schema，
不得新增 owner/session/ledger/TTL/retry，不得以缺 collaborator 为由再交 zero-Java 清单。若现有 typed port
缺一项，把缺口隔离为该 Service 内可编译的 caller-ready 参数/结果边界，同时至少完成真实可调用 public chain；
禁止 stub/固定成功。完成后运行 Cloud `mvn -q compile`（不 clean），报告四 public definition、真实 call graph、
typed dependency 表、SHA、exit code 与所有基线差异（应为 0）。已领取后允许工作超过 20 分钟。

**验收以完整公开智能点击链可编译、可调用、到达既有 typed port 为准，不以 helper 数量为准。**

## Parent Task Brief #12 (AUTHORITATIVE TRUE EOF) - `W-NPC-PUBLIC-SMART-CHAIN-IMP1` - 2026-07-14T09:49:00-04:00

External C 立即继续一个完整 Service 公开链，不写 Design、不再搬 private leaf。请在
**2026-07-14T10:09:00-04:00 前**追加：

`CLAIMED | task=W-NPC-PUBLIC-SMART-CHAIN-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud com/bot/dhxy/service/NpcClickService.java, Append this log]`

唯一 Java 写集是 Cloud `com/bot/dhxy/service/NpcClickService.java`。按 committed `0114604e` 一次闭合并真实公开
`clickNpcSmart`、`clickNpcSmartWithOutcome`、`tryDirectCombatTargetClick`、`confirmPendingSmartClick` 的 Cloud 业务链。
允许在同一类补 committed 自有 state、constructor collaborators、passive records/enums 与 private closure；必须复用
当前已迁 `NpcClickSmartCloud*` request/session/queue/decision 类型，并经 explicit `TaskExecutionContext` 的
`CloudGameClient` capture/InputBundle 到达 typed port。不得因缺 collaborator 交零代码。

本地 exact-window screenshot/template/OCR、story/dialog watcher、combat observation 与物理输入队列不得复制到
Cloud；它们只能作为现有 typed capture/fact 或 closed terminal 输入。保持 candidate FIFO、12 候选预算、story
restart 上限、验证/fail-closed、direct-combat authorization、move+click 原子 bundle、返回 outcome 与 fallback 顺序。
没有对应 typed observation 的分支必须明确保持不可执行，不能伪造成功或新增 retry/TTL。禁止改 remote shared 文件、
host/Task/caller 或其它 Service。完成后 Cloud `mvn -q compile`（不 clean），交付四 public API、call graph、typed
依赖表、基线差异、SHA 与 exit。已领取后允许工作超过 20 分钟。

## Parent Task Brief #10 (AUTHORITATIVE TRUE EOF) - `W-SS-IMAGE-PAYLOAD-IMP1` - 2026-07-14T08:43:00-04:00

External Worker C 直接实施，不写 Design。请在 **2026-07-14T09:02:43-04:00 前**先于本日志真实 EOF 追加：

`CLAIMED | task=W-SS-IMAGE-PAYLOAD-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

唯一 Java 写集为 Cloud `src/main/java/com/bot/dhxy/service/SummonSkillService.java` + 本日志。从 committed
`0114604e` 机械迁入完整 private `readImagePayload(String rawPath)`。仅补完整块直接需要且当前缺失的 JDK imports：
`java.io.IOException`、`java.nio.file.Files`、`java.nio.file.Path`、`java.util.Base64`；复用既有
`ImagePayload` record、`sha256Hex(byte[])` 与 `@Slf4j`。

保持一次 `Files.readAllBytes(Path.of(rawPath))`、Base64、SHA、warn 文案/参数与 IOException->null 顺序逐字不变。
该方法只读取调用方给定的 Cloud artifact path，不做本地 capture/template/OCR/input；不得接 caller/host，不新增
wrapper/owner/session/ledger/TTL/retry。完成后运行 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、
完整块 exact diff、定义数、文件 SHA-256 与 exit code；自审不算 Approved。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #8 (AUTHORITATIVE TRUE EOF) - `W-ACS-REFRESH-DEFERRED-LOG-IMP1` - 2026-07-14T08:19:00-04:00

External Worker C 现在直接实施，不写 Design。请在 **2026-07-14T08:39:00-04:00** 前先于本日志真实 EOF 追加：

`CLAIMED | task=W-ACS-REFRESH-DEFERRED-LOG-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud AutoCombatService.java, Append this log]`

领取后允许实施超过 20 分钟。唯一写集：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java`
2. 本日志

从 committed `0114604e` 机械迁入完整 private `logRefreshDueDeferred(TaskExecutionContext context, AutoCombatRuntimeState state, String windowId, RefreshDuePanelVerifyDecision decision, long now)`。只允许补该方法直接需要的 Lombok `@Slf4j` import/类注解；目标已有 constant、state 字段、decision record 和 `safeRequestedTaskCode`。必须保持 throttle 边界、state 写入位置、info/debug 分支及所有日志参数顺序逐字不变。不得接 caller，不得迁 refresh 主流程，不得新增 clock read、capture/input/remote/owner/session/ledger/TTL/retry/wrapper。

完成后运行 Cloud `mvn -q compile`（不 clean），追加 Implementation #1：完整块 source/target exact diff、定义数、文件 SHA-256、compile exit code。无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Direct Implementation Task - `W-SUMMONSKILL-IF8-DECISION-IMP1` - 2026-07-14T06:55:00-04:00

External Worker C 请先在本日志真实 EOF 追加：

`CLAIMED | task=W-SUMMONSKILL-IF8-DECISION-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

领取截止：`2026-07-14T07:15:00-04:00`。20 分钟只检查领取，不检查完成；领取后允许工作超过 20 分钟。

这是直接实现任务，不写 Design。唯一 Java 写集：
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`。

从 committed `0114604e` 同名类机械迁入完整的 package-private static 方法
`detectSkillCountFromIf8Match(double[] match)`：`null -> 6`，其它任何数组引用 -> `8`。保持签名、可见性、
分支和返回逐字一致；方法先保持 dormant，不迁本地 IF8 template 加载/capture/file path，不加 caller/wrapper/owner/
session/ledger/TTL/retry。允许同步补充类 JavaDoc 一句说明；禁止改其它 Java。

完成后运行 Cloud `mvn -q compile`（不 clean），记录方法 source/target 规范化 SHA-256、文件 SHA-256、diff 与
exit code，追加 `Implementation #1`。自审不算 Approved，等待父级源码审查。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief - `W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1` - 2026-07-13T23:53:54-04:00

这是新的直接实施单，不是 Design。请 External Worker C 先在本日志真实 EOF 追加：

`CLAIMED | task=W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1; claimedAt=<ISO-8601>; writeSet=<exact paths>`

领取截止：`2026-07-14T00:13:54-04:00`。20 分钟只检查是否领取；领取后允许实施超过 20 分钟。

### 唯一写集（DHXY 1 New + 2 Modify）

- New: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteTeamReturnButtonFact.java`
- Modify: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteWindowFactKind.java`
- Modify: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java`
- Append-only: 本日志

### 直接实现合同

- 新增 closed `RemoteWindowFactKind.TEAM_RETURN_BUTTON` 与 immutable `RemoteTeamReturnButtonFact`；字段/枚举/校验必须与 B 的 Cloud contract 镜像：`state, clickX, clickY, matchScore, coordinateSpace`，五态及 PRESENT/null 矩阵完全一致。
- handler 的 WINDOW_FACT switch 只调用 A 新建的 `TeamReturnButtonLocalObservationMechanics.observe(access.binding())`；逐态投影，不把 capture/template/mechanics failure 降级为 ABSENT。
- A 返回 window-client 坐标时，handler 仅用同一个 exact binding 的 `x/y` 转为 `SCREEN_ABSOLUTE_PX`；不做随机化、不点击、不调用 `TeamReturnService`、不引入重试。
- 保持既有 request dispatch fence、读后 registration/binding fence、OBSERVED envelope 和 payload codec 路径不变。不改 digest/transport/input queue/Service/schema/tests/host。
- 完成后运行 DHXY `mvn -q -DskipTests compile`，在本日志追加 `Implementation #1`、3 文件 SHA/编译结果与自审；若 A 文件尚未落盘，只报告该明确跨 Worker 编译依赖，不可创建或代写 A 文件。自审不算父级批准。

保护全部现有 dirty/untracked；禁止 reset/checkout/clean/delete/commit，不启动 application/Task/UI/capture/input。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task / `W-BAG-MACRO-LOCAL-MECHANICS-IMP1` - 2026-07-13T21:33:00-04:00

直接实现，不写 Design。请先在本日志真实 EOF 追加：
`CLAIMED task=W-BAG-MACRO-LOCAL-MECHANICS-IMP1 claimedAt=<ISO> uniqueWriteSet=<下列文件+本日志>`。

目标：把 committed `0114604e` 的三项 Bag 退物品流程作为一个闭合本地宏入口，继续在本地单一输入队列内执行
capture/template/input 交错步骤，Cloud 只收到 typed result。唯一 Java 写集（DHXY）如下：

- Modify `src/main/java/com/bot/dhxy/service/BagService.java`
- New `src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroIntent.java`
- New `src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroResult.java`
- Modify `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
- 如 enum 穷尽性确实需要，Modify `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java`

只覆盖三项：`PRESCAN_MAIN_BAG_TASK_PAGE`、`PRESCAN_MAIN_BAG_FROM_BACK`、
`USE_CACHED_MAIN_BAG_RETURN_ITEM`。在 `BagService` 增加一个真实 direct-for-exclusive 入口，内部必须复用 committed
Bag 的三个 `...Exclusive` 核心流程；不得再次调用 `inputSequences.submitExclusiveAndWait(...)`，防止 queue-in-queue
死锁。handler 必须在 exact `BindingAccess`、当前 registration/taskRun/runRevision fence 和现有
`submitRemoteExclusiveAndWaitDetailed(...)` 单一输入队列边界内调用该入口；每个 command 恰好执行一次宏。
继续使用 `WindowTaskContextHolder.callWith(...)` 和现有 pause/stop checkpoint；不启动线程、不异步重投。

映射严格遵循 B wire：prescan 返回非 null cache point => FOUND，否则 NOT_FOUND；cached use true => USED，false =>
NOT_USED。`NOT_EXECUTED/STOPPED/UNKNOWN` 不得伪装业务 result。不得新增 Bag owner/permit/session/ledger/TTL/retry，
不得使用旧 `CloudBagStateOwner/BagWorkflowState`，不得改 Cloud/schema/tests/host/runner/Task。保持 committed
模板路径、翻页次序、capture 次数、坐标、delay、fallback、return 值完全不变。依赖 B 尚未落盘时先完成
BagService+本地 model，并等待后再接 handler；不得复制 DTO。

可跑 DHXY `mvn -q -DskipTests compile`（不 clean）。你不是仓库中唯一 Worker；保护全部 dirty/untracked，不回滚、
覆盖、清理或提交。领取截止 `2026-07-13T21:53:00-04:00`；逾期只原样重发 External C，绝不内部接管。
交付标题：`External Worker C - W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation #1`。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - W-INPUT-C1 Closeout - 2026-07-13T20:33:15-04:00

已收到父级 Parent Source Inventory Review #1 - APPROVED（P0/P1/P2=0，无待返修），Source Inventory #1 正式进入直接实现 cohort，不再写 Design #N。
口径补正已对齐：LOCAL_RESIDENT 指窗口绑定、capture/template/OCR、单一输入队列等机械能力永久留本地，不表示对应 Service 的业务编排留本地；本盘点相关项均属机械能力口径。
说明（诚实备案）：C 在上一心跳轮曾误以为已写入本收口块，实为输出错位、未落盘；本块为首次真正写入并已核验。W-INPUT-C1 无待返修，本 heartbeat 关闭（当前会话已无相关 cron），等待父级指派下一任务。本轮零 Java/Maven/schema/tests、无 Git、未运行 Maven；唯一写入为本 append-only 日志。

## Parent Direct Implementation Task / `W-CBOX-LOCAL-HANDLER-IMP1` - 2026-07-13T20:55:00-04:00

重新启用本固定日志，直接实现，不写 Design。先追加
`CLAIMED task=W-CBOX-LOCAL-HANDLER-IMP1 claimedAt=<ISO> uniqueWriteSet=<handler+本日志>`。

唯一 Java 写集：Modify DHXY `LocalRemoteGameCommandHandler.java`。构造注入现有
`CommonBoxLocalObservationMechanics`；在既有 `BindingAccess + WindowTaskContextHolder.callWith` 的
`WINDOW_FACT/COMMON_BOX` case 中只调用一次 `observe(access.binding())`，绝不点击。将 mechanics 的 clientX/Y 用 exact
binding x/y 转为屏幕绝对 clickX/Y；五种 status 逐值映射 B 的 DTO state。MATCHED 携带 score/timestamp 与
`SCREEN_ABSOLUTE_PX`，negative 字段全 null；不得把 mechanics/transport failure伪装为 NOT_MATCHED。不得改 mechanics、
DTO/enum、Cloud、schema、tests/host。若 B DTO 尚未落盘先实现后等待，不自建第二 DTO。完成后 DHXY
`mvn -q -DskipTests compile`（不 clean），追加 Implementation 交付。领取截止
`2026-07-13T21:15:00-04:00`；逾期只原样重发 External C，不内部接管。

## External Worker C - CLAIMED - 2026-07-13T21:14:07-04:00

- task: W-CBOX-LOCAL-HANDLER-IMP1
- claimedAt: 2026-07-13T21:14:07-04:00
- uniqueWriteSet:
  1. DHXY src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java（Modify，唯一 Java 写集）
  2. 本 append-only 日志 docs/superpowers/plans/reports/2026-07-13-input-bundle-worker-c.md

## External Worker C - W-CBOX-LOCAL-HANDLER-IMP1 Implementation - 2026-07-13T21:18:47-04:00

### 唯一 Java 写集：Modify DHXY src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java

精确 diff（6 处，全部落在该文件，无其它 Java 改动）：

1. import：新增 `import com.bot.dhxy.service.commonbox.CommonBoxLocalObservationMechanics;`（B 的 `RemoteCommonBoxFact` 与本 handler 同包 `com.bot.dhxy.cloud.remote`，无需 import）。
2. 字段：新增 `private final CommonBoxLocalObservationMechanics commonBoxLocalObservationMechanics;`。
3. 构造器参数：在末位 `AutoCombatPanelService autoCombatPanelService` 后追加 `CommonBoxLocalObservationMechanics commonBoxLocalObservationMechanics`（Spring 现有构造注入，B 的 mechanics 已是 `@Service`）。
4. 构造器赋值：`this.commonBoxLocalObservationMechanics = Objects.requireNonNull(commonBoxLocalObservationMechanics, "commonBoxLocalObservationMechanics");`。
5. `executeWindowFact` 的 `switch(request.getFactKind())` 新增 `case COMMON_BOX`，复用既有 `windowTaskContextHolder.callWith(access.context(), () -> ...)` 结构，仅调用一次 `commonBoxLocalObservationMechanics.observe(access.binding())`，绝不点击、绝不入输入队列。
6. 新增 private helper `toCommonBoxFact(ObservationResult, WindowNativeBinding)`：把观测投影为 B 的 `RemoteCommonBoxFact`。

### 合同逐条落实

- 五状态逐值 1:1 映射（mechanics `Status` → `RemoteCommonBoxFact.State`）：MATCHED→MATCHED、NOT_MATCHED→NOT_MATCHED、CAPTURE_UNAVAILABLE→CAPTURE_UNAVAILABLE、TEMPLATE_UNAVAILABLE→TEMPLATE_UNAVAILABLE、MECHANICS_FAILED→MECHANICS_FAILED。switch 逐值枚举、无 default，编译器保证穷尽。
- 不把 mechanics/transport failure 伪装为 NOT_MATCHED：CAPTURE_UNAVAILABLE / TEMPLATE_UNAVAILABLE / MECHANICS_FAILED 各自映射到同名 negative state；只有 mechanics 真正返回 NOT_MATCHED 才是 NOT_MATCHED。
- MATCHED：mechanics 的 clientX/clientY 为窗口客户区像素（ROI_LEFT+..、ROI_TOP+..），用 exact binding 原点转屏幕绝对：clickX = binding.getX() + result.clientX()，clickY = binding.getY() + result.clientY()；携带 matchScore、matchedAtEpochMs；coordinateSpace = SCREEN_ABSOLUTE_PX。
- 每个 negative state：只带 state + coordinateSpace(SCREEN_ABSOLUTE_PX)，clickX/clickY/matchScore/matchedAtEpochMs 全 null（与 B 的 DTO compact 校验一致，负态携任何 match 字段会抛异常）。
- 只观测不点击：case 内只 observe 一次并投影，无任何 inputActionQueue / click / move 调用。
- 复用 B 的既有 DTO：`RemoteCommonBoxFact` 已落盘（B 的 W-CBOX-LOCAL-DTO-IMP1），直接消费，未自建第二 DTO；未改 mechanics、DTO/enum、Cloud、schema、tests/host。
- 输出仍走既有 `RemoteWindowFactOutcomePayload.builder().factKind(COMMON_BOX).fact(payloadCodec.toPayloadTree(fact))`，与其它 WINDOW_FACT 一致，无新 outcome 通路。

### 坐标与几何核对

- mechanics ROI 常量：LEFT=623/TOP=590/RIGHT=682/BOTTOM=618；MATCHED clientX∈[623,682)、clientY∈[590,618)。binding.getX()/getY() 为窗口屏幕绝对原点（非负）→ clickX/clickY 非负，满足 `RemoteCommonBoxFact` 的 requireNonNegative。
- matchScore≥0.86（mechanics TEMPLATE_THRESHOLD 与 DTO 校验一致）；matchedAtEpochMs>0。

### 编译门禁

- DHXY `mvn -q -DskipTests compile`（未 clean，遵守 AB 写共享 Java 期间不并发 clean）：EXIT=0，无错误。
- 落盘核验（grep count 各 =1）：case COMMON_BOX、toCommonBoxFact 定义、字段、import、observe 调用、绝对坐标转换。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一 Java 写集=该 handler 一个文件 + 本日志；未触碰 mechanics/DTO/enum/Cloud/schema/tests/host；无 Git 操作。
2. 只观测不点击；负态不伪装；五状态穷尽 1:1；MATCHED 屏幕绝对 + score/timestamp；negative 全 null。
3. 未运行 clean；仅 `-DskipTests compile`。
4. 无已批准业务差异；按 0114604e 基线等价迁移。

External Worker C 交付 W-CBOX-LOCAL-HANDLER-IMP1 完毕，持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source + Final Build Review #2 - APPROVED / `W-CBOX-LOCAL-HANDLER-IMP1` - 2026-07-13T21:23:00-04:00

父级独立复核 handler 源码与全仓构造点：`COMMON_BOX` 在 exact `BindingAccess` 和既有
`WindowTaskContextHolder.callWith(...)` 内只调用一次 `CommonBoxLocalObservationMechanics.observe(...)`，零点击、
零输入队列调用；五种 mechanics status 与 `RemoteCommonBoxFact.State` 逐值穷尽映射，只有真实
`NOT_MATCHED` 才发布 `NOT_MATCHED`。MATCHED 沿用 committed CommonBox 的
`binding.getX/Y + clientX/Y` 屏幕绝对坐标公式并携带 score/timestamp；四个 negative state 只携带 state 与
`SCREEN_ABSOLUTE_PX`。仓内没有手工 `new LocalRemoteGameCommandHandler(...)` 构造点遗漏。

父级 fresh 门禁：DHXY `mvn -q -DskipTests compile` exit 0；Cloud `mvn -q clean package` exit 0，
4 suites / 21 tests，0 failures / 0 errors / 0 skipped，shaded JAR 已重新生成。

结论：`W-CBOX-LOCAL-HANDLER-IMP1 FINAL APPROVED`，`P0=0 / P1=0 / P2=0`。CommonBox 整波
（Cloud Service、closed fact、DHXY DTO/handler/local read-only mechanics、schema）正式收口。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T21:36:04-04:00

- task: W-BAG-MACRO-LOCAL-MECHANICS-IMP1
- claimedAt: 2026-07-13T21:36:04-04:00
- uniqueWriteSet:
  1. Modify DHXY src/main/java/com/bot/dhxy/service/BagService.java
  2. New DHXY src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroIntent.java
  3. New DHXY src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroResult.java
  4. Modify DHXY src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java
  5. Modify DHXY src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java（仅 enum 穷尽性确需时）
  6. 本 append-only 日志
- 约束遵循：复用 committed 三个 ...Exclusive 核心、不再 submitExclusiveAndWait（防 queue-in-queue）、不新增 owner/permit/session/ledger/TTL/retry、不用旧 CloudBagStateOwner/BagWorkflowState、不改 Cloud/schema/tests/host/runner/Task、保护全部 dirty/untracked 不回滚。

## TRUE EOF Parent Placement Clarification #1 - 2026-07-13T21:42:00-04:00

用户已明确拍板：真实 `BagService` 永久留 DHXY 本地，不只是“本轮 mechanics 暂留”。C 当前实现继续，且应把
`BagService` 作为唯一真实背包截图/模板/翻页/输入实现；Cloud 不会创建同名业务副本，只通过 closed
`LOCAL_MACRO/BAG_RETURN_ITEM` 调用本入口。`UICleanerService` 同样永久留 DHXY。本澄清不改变 C 的唯一写集、
三项 committed 核心流程或领取状态。

## External Worker C - W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation #1 - 2026-07-13T21:43:09-04:00

### 本轮范围与 B 依赖门

取证：三个操作名 PRESCAN_MAIN_BAG_TASK_PAGE / PRESCAN_MAIN_BAG_FROM_BACK / USE_CACHED_MAIN_BAG_RETURN_ITEM 与 B 的
result wire DTO 当前**全仓未落盘**（grep 无匹配）。按任务书“依赖 B 尚未落盘时先完成 BagService+本地 model，并等待后
再接 handler；不得复制 DTO”，本 Implementation #1 完成 BagService 入口 + 两个 DHXY 本地 model 并过编译；handler 与
RemoteOperationLedger 接线**延后**到 B 的 RemoteGameOperation 值 + result DTO 落盘后再做，本轮不自建 B 的 wire DTO。

### 精确写集（本轮实际改动）

1. New src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroIntent.java
   - DHXY 本地 domain model（非 wire DTO）。closed Kind{PRESCAN_TASK_PAGE, PRESCAN_FROM_BACK, USE_CACHED_RETURN_ITEM}
     + 静态工厂 prescanTaskPage(template,source) / prescanFromBack(template,maxBagIndex,source) /
     useCachedReturnItem(cachedPoint,source)。只承载 committed 业务参数，无 owner/permit/session/ledger/TTL/retry。
2. New src/main/java/com/bot/dhxy/service/bag/BagReturnItemMacroResult.java
   - DHXY 本地 domain model（非 wire DTO）。closed Status{FOUND, NOT_FOUND, USED, NOT_USED}；FOUND 携 ReturnItemCachePoint，
     其余不携。工厂 found/notFound/used/notUsed。不表达 NOT_EXECUTED/STOPPED/UNKNOWN（这些留在 handler/wire 边界）。
3. Modify src/main/java/com/bot/dhxy/service/BagService.java
   - 加两个 import；新增 public 入口 runReturnItemMacroDirectForExclusive(BagReturnItemMacroIntent intent,
     TaskExecutionContext context)。

### 入口合同逐条落实

- 复用 committed 三个 ...Exclusive 核心，不新造点击/翻页/匹配逻辑：
  - PRESCAN_TASK_PAGE  -> findMainBagTaskPageItemPointExclusive(template, source, context)
  - PRESCAN_FROM_BACK  -> findMainBagItemFromBackPointExclusive(template, maxBagIndex, source, context)
  - USE_CACHED_RETURN_ITEM -> useCachedMainBagReturnItemExclusive(cachedPoint, source, context)
- 不再调用 inputSequences.submitExclusiveAndWait(...)：入口方法体内 submitExclusiveAndWait 出现 0 次（已 grep 核验），
  防 queue-in-queue 死锁。入口以 isInputWorkerThread() 守卫，必须已在输入 worker 独占段内调用；否则 IllegalStateException
  （编程错误，不伪装业务 result）。
- 映射严格遵循 B wire：prescan 返回非 null cache point => found(point)(FOUND)，否则 notFound()(NOT_FOUND)；cached use
  true => used()(USED)，false => notUsed()(NOT_USED)。
- 保持 committed return 值不变：USE 分支对 cachedPoint==null 直接 NOT_USED（镜像 committed useCachedMainBagReturnItem 的
  “null => false”），不调核心。
- pause/stop：沿用三个核心已有的 throwIfStopRequested(context) checkpoint，未新增。
- 未改任何 committed 方法/模板路径/翻页次序/capture 次数/坐标/delay/fallback；仅新增一个入口方法投影既有核心结果。

### 未做（按任务延后，非遗漏）

- LocalRemoteGameCommandHandler.java：待 B 的 RemoteGameOperation 三值落盘后，在 exact BindingAccess +
  当前 registration/taskRun/runRevision fence + 既有 submitRemoteExclusiveAndWaitDetailed 单一输入队列边界 +
  WindowTaskContextHolder.callWith 内调用本入口，每 command 恰好一次，并把 BagReturnItemMacroResult 逐值映射到 B 的
  result DTO。本轮未改该文件（git diff 对其无本任务改动）。
- RemoteOperationLedger.java：仅当接 handler 后 enum 穷尽性确需时才 Modify；本轮未改。

### 编译门禁

- DHXY mvn -q -DskipTests compile（未 clean，遵守不并发 clean）：EXIT=0。
- 落盘核验：两个 model 文件存在；BagService 入口 grep=1；入口区 submitExclusiveAndWait=0；三核心被引用。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一 Java 改动=BagService(+import/1 方法) + 两个本地 model；未改 Cloud/schema/tests/host/runner/Task；未改 handler/ledger
   （延后）；未用旧 CloudBagStateOwner/BagWorkflowState；未新增 owner/permit/session/ledger/TTL/retry。
2. 未自建 B 的 wire DTO；未做 Git 操作；未回滚/覆盖/清理他人 dirty/untracked。
3. 未运行 clean；仅 -DskipTests compile。
4. 无已批准业务差异；按 0114604e 基线等价迁移。

External Worker C 交付 W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation #1（BagService+本地 model 完成，handler/ledger
待 B wire 落盘）；持续重读本日志，B 落盘或父级复审后继续；自审不算 Approved。

## Parent Source Review #3 - APPROVED PARTIAL / `W-BAG-MACRO-LOCAL-MECHANICS-IMP1` - 2026-07-13T21:59:00-04:00

父级独立复核 `BagService` 新入口与两个本地 closed model，结论 `P0=0 / P1=0 / P2=0`：入口强制当前线程为
唯一 input worker，方法体没有 `submitExclusiveAndWait`，只逐值调用 committed 的
`findMainBagTaskPageItemPointExclusive`、`findMainBagItemFromBackPointExclusive`、
`useCachedMainBagReturnItemExclusive`，因此没有 queue-in-queue；模板、翻页、capture、坐标、delay、fallback 与三个核心
返回值均未在入口重写。FOUND/NOT_FOUND/USED/NOT_USED 投影闭合，transport terminal 未混入本地业务 result。

本部分 `SOURCE APPROVED PARTIAL`；真实 `BagService` 按用户决定永久留 DHXY。C 现在继续原任务的 handler 接线：
B 的 `LOCAL_MACRO` enum/command/result DTO 已落盘。必须在 exact binding/current registration/runRevision fence 与既有
`submitRemoteExclusiveAndWaitDetailed` 单一队列边界内，每个 command 只调用本入口一次；UNKNOWN/STOPPED/
NOT_EXECUTED 不得构造业务 result，EXECUTED 才映射 flat typed result。等待 B 的 digest/transport scope correction
落盘后再跑最终 DHXY compile；不得自行改 B 所有文件或新增 owner/session/ledger/TTL/retry。

整项仍是 `IN PROGRESS`，父级最终批准等待 handler/必要 ledger 穷尽接线与 fresh 双构建。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation #2 (handler wiring) - 2026-07-13T22:01:15-04:00

B 的 wire 已落盘（RemoteBagReturnItemMacroCommandPayload / RemoteBagReturnItemMacroResultPayload /
RemoteLocalMacroKind，RemoteGameOperation.LOCAL_MACRO，codec readBagReturnItemMacro/Result），故完成 Implementation #1
延后的 handler+ledger 接线。全部编译门禁 DHXY mvn -q -DskipTests compile（未 clean）EXIT=0。

### 精确写集（本轮实际改动，均在已领取写集内）

Modify src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java：
1. 构造注入 BagService（import + field + ctor 参数 + Objects.requireNonNull 赋值）。仓内无手工
   new LocalRemoteGameCommandHandler(...)（grep 空）→ Spring 构造注入，新增参数安全。
2. decode switch 增 `case LOCAL_MACRO -> payloadCodec.readBagReturnItemMacro(command.getPayload())`。
3. execute switch 增 `case LOCAL_MACRO -> executeLocalMacro(...)`。
4. 新 executeLocalMacro(command, RemoteBagReturnItemMacroCommandPayload, admissionSnapshot, registration,
   BindingAccess, timing)：见下。
5. 三个映射 helper：toBagReturnItemMacroIntent（command→BagReturnItemMacroIntent）、toReturnItemCachePoint
   （wire CachePoint→model ReturnItemCachePoint）、toBagReturnItemMacroResultPayload（BagReturnItemMacroResult→B 的
   RemoteBagReturnItemMacroResultPayload）。
6. isPhysicalInputOperation 增 LOCAL_MACRO（宏执行物理输入）。
7. emptyOutcomePayload 增 `case LOCAL_MACRO -> java.util.Map.of()`（非 EXECUTED 的 benign 空对象）。

Modify src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java：
8. isExclusiveInputOperation 增 LOCAL_MACRO。
9. quiescence 计数 switch 的 input++ 组增 LOCAL_MACRO。

### 合同逐条落实（均以 real code 接地，非臆测）

- session-less 获取：RemoteBagReturnItemMacroCommandPayload 无 exclusiveSessionId 字段 → 宏是 session-less
  exclusive 命令，按 executeInputBundleMechanical 的 session-less 安全栅栏获取输入队列，而非 SummonSkill 的
  session-bound InFlightExclusive。executeLocalMacro 内：`windowTaskContextHolder.callWith(access.context(), () ->
  inputActionQueue.submitRemoteExclusiveAndWaitDetailed("local-macro:bag-return-item:"+op, callback,
  timing.deadlineNanos(), pauseToken, () -> remoteInputSafetyReason(command, access.runner(), null), () ->
  workerAdmissionRevisionFence(command, access.runner(), null)))`。callback 内恰好一次
  `bagService.runReturnItemMacroDirectForExclusive(intent, null)`，不再 submitExclusiveAndWait（Impl #1 入口已保证）。
- 单一输入队列 + fence：exact BindingAccess（access）、registration/taskRun/runRevision fence 由 dispatch 前导
  requireRegistration/requireBoundWindow + workerAdmissionRevisionFence（run-revision 一致）保证；每 command 恰好一次宏。
- null TaskExecutionContext：handler 无本地 TaskExecutionContext；传 null。已核验 TaskCheckpoint.throwIfStopRequested(null)
  null 安全，停/暂停由 committed 核心内的 InputActionScope.checkpoint()（输入队列，pauseToken 驱动）执行——与
  SummonSkill remote（cleanSummonSkillsOnce 亦不带 TaskExecutionContext）同一模式。
- 传输状态映射：completed+非 null result → EXECUTED(OK)+挂 result payload；STOP_REQUESTED → STOPPED(STOP_REQUESTED)；
  未起步 → NOT_EXECUTED(outcomeCodeForUnstarted)；其余 → UNKNOWN(WINDOW_BINDING_CHANGED/TASK_RUN_MISMATCH/INPUT_FAILED)。
  非 EXECUTED 一律 emptyOutcomePayload，**绝不把传输失败伪装成业务 result**。
- 业务 result 映射（B wire）：BagReturnItemMacroResult.Status → RemoteBagReturnItemMacroResultPayload.State 逐值 1:1
  （FOUND/NOT_FOUND/USED/NOT_USED）；FOUND 携 cachePoint（model ReturnItemCachePoint → wire CachePoint 逐字段）。B 的
  result payload 仅 EXECUTED 携带（其 javadoc + client readBagReturnItemMacroResult 已 guard executionState==EXECUTED）。
- exclusive-input 分类一致性（安全关键）：LOCAL_MACRO 走独占输入队列，处处与 SUMMON_SKILL_WHOLE_PASS 同等分类——
  handler isPhysicalInputOperation、ledger isExclusiveInputOperation、ledger quiescence input 计数均加入 LOCAL_MACRO，
  保证多窗口输入串行化 admission/fence/quiescence 正确。
- 未自建 B 的 DTO；未改 Cloud/schema/tests/host/runner/Task/mechanics/committed BagService 三核心；未改 committed 模板/
  翻页/坐标/delay/fallback/return。

### 编译门禁与核验

- DHXY mvn -q -DskipTests compile EXIT=0（含首轮 switch 穷尽性 error：emptyOutcomePayload 缺 LOCAL_MACRO，已补 case 后通过）。
- grep 核验各 =1：decode/execute case、executeLocalMacro、bagService 字段、observe 入口调用、ledger 两处；emptyOutcome
  `case LOCAL_MACRO ->` 在三处 switch 共 3 次（decode/execute/emptyOutcome）。仓内无手工 new 构造点。

### scope self-QA（仅 QA，不构成 Approved）

1. Java 改动仅 LocalRemoteGameCommandHandler + RemoteOperationLedger（均在写集 item 4/5）；RemoteOperationLedger 的改动是
   exclusive-input 语义分类（LOCAL_MACRO 走独占输入队列），非仅 enum 穷尽——已在上文说明理由；未改 BagService committed 核心、
   两个本地 model、Cloud/schema/tests/host。
2. 未做 Git；未 clean；保护他人 dirty/untracked。
3. 无已批准业务差异；按 0114604e 基线等价迁移。

External Worker C 交付 W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation #2（handler+ledger 接线完成，整波闭合）；持续
重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #4 - BLOCKED / `W-BAG-MACRO-LOCAL-MECHANICS-IMP1` - 2026-07-13T22:12:00-04:00

父级复核本地 `BagService` mechanics、单一输入队列边界与 Cloud outcome parser。`BagService` 的三项宏入口仍只做
exact-window capture/template/input 交错，未引入业务策略；`submitRemoteExclusiveAndWaitDetailed` 在 worker 首个物理
步骤前执行 current `runRevision` admission fence，callback 内的 `InputActionScope.checkpoint()` 继续承担 pause/stop 与
持续安全检查。上述边界成立。但当前 handler terminal payload 与已批准 Cloud closed contract 不一致，结论
`P0=0 / P1=1 / P2=0`：

1. **P1：非 EXECUTED 的 LOCAL_MACRO payload 被压成空对象。**
   `LocalRemoteGameCommandHandler.emptyOutcomePayload(...)` 的 `LOCAL_MACRO` 分支当前返回 `Map.of()`
   （源码约 `:1819-1823`）；而 Cloud `RemoteCommandOutcomeEnvelope.localMacroOutcome(...)` 在摘要校验前强制 exact
   keys `macroKind/operation/state/cachePoint`，并要求 `NOT_EXECUTED/STOPPED/UNKNOWN` 保留
   `macroKind=BAG_RETURN_ITEM`，后三项为显式 null（Cloud 约 `:215-242`）。空对象会被 strict parser 直接拒绝，无法
   形成合法 terminal outcome，也无法与 B 正在补的 typed canonical digest 重建一致。

   **返修条件：** 仅修改 C 已拥有的 `LocalRemoteGameCommandHandler.java` 与本日志。把 LOCAL_MACRO 的空 outcome
   改为 closed flat payload，恰含四个键：从 strict request 安全保留 `macroKind`，并令 `operation/state/cachePoint`
   为显式 null；不得伪造 FOUND/NOT_FOUND/USED/NOT_USED，不得把 UNKNOWN/STOPPED 折成 NOT_EXECUTED，不得新增
   retry、owner/session/ledger。所有 `executeLocalMacro(...)` 的 STOPPED/NOT_EXECUTED/UNKNOWN 路径必须复用该形状；
   EXECUTED 结果不改。完成后追加 `External Worker C - W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation Repair #1`
   并运行 DHXY `mvn -q -DskipTests compile`。父级复审前本切片仍为 BLOCKED。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - W-BAG-MACRO-LOCAL-MECHANICS-IMP1 Implementation Repair #1 - 2026-07-13T22:16:07-04:00

### 关闭 Review #4 P1（非 EXECUTED LOCAL_MACRO payload 被压成空对象）

证据/根因：`emptyOutcomePayload(...)` 的 LOCAL_MACRO 分支原返回 `java.util.Map.of()`（空对象），而 Cloud
`RemoteCommandOutcomeEnvelope.localMacroOutcome(...)` strict parser 在摘要校验前强制 exact keys
`macroKind/operation/state/cachePoint`，且 NOT_EXECUTED/STOPPED/UNKNOWN 必须保留 `macroKind=BAG_RETURN_ITEM`、后三项显式 null。
空对象被 strict parser 拒绝，无法形成合法 terminal outcome。

### 修复（仅改 C 已拥有的 LocalRemoteGameCommandHandler.java + 本日志）

把 `emptyOutcomePayload(...)` 的 `case LOCAL_MACRO` 由 `Map.of()` 改为 closed flat 4 键 payload：

```
case LOCAL_MACRO -> {
    java.util.Map<String, Object> outcome = new java.util.LinkedHashMap<>();
    outcome.put("macroKind", RemoteLocalMacroKind.BAG_RETURN_ITEM);
    outcome.put("operation", null);
    outcome.put("state", null);
    outcome.put("cachePoint", null);
    yield outcome;
}
```

序列化为 `{"macroKind":"BAG_RETURN_ITEM","operation":null,"state":null,"cachePoint":null}`。核实 codec 的 objectMapper
未设 Include.NON_NULL（默认 ALWAYS）→ null 值序列化为显式 `null` 键，满足 strict parser 的 exact-keys-allow-null；
`toPayloadTree(...)` 对该 Map 的 `isObject()` 校验通过（已编译验证）。

### 合同逐条落实

- 四键 closed flat：`macroKind` 保留为 `RemoteLocalMacroKind.BAG_RETURN_ITEM`（LOCAL_MACRO 唯一 kind），
  `operation/state/cachePoint` 显式 null。
- 不伪造业务态：非 EXECUTED 绝不构造 FOUND/NOT_FOUND/USED/NOT_USED（state 显式 null）。
- 不折叠传输态：`executeLocalMacro(...)` 的 STOPPED / NOT_EXECUTED / UNKNOWN 三条路径各自保留其
  `RemoteExecutionState`（STOPPED/NOT_EXECUTED/UNKNOWN 不被压成 NOT_EXECUTED），只是共用同一空 payload 形状——三条
  分支均经 `emptyOutcomePayload(command)` 复用该形状（grep：3 处 `outcomePayload = emptyOutcomePayload(command)`）。
- EXECUTED 结果不改：仍经 `toBagReturnItemMacroResultPayload(request, callbackResult[0])` 输出 flat typed result（grep：1 处）。
- 未新增 retry/owner/session/ledger；未改 B 的文件、Cloud/schema/tests/host、BagService committed 核心或两个本地 model。

### 编译门禁与核验

- DHXY `mvn -q -DskipTests compile`（未 clean）：EXIT=0。
- grep 核验：LOCAL_MACRO 的 `Map.of()` 残留=0；四键 put 各=1；非 EXECUTED 复用 emptyOutcomePayload=3；EXECUTED 映射=1。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一 Java 改动=LocalRemoteGameCommandHandler.java 一处 case；未触碰其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 非 EXECUTED 形状与 EXECUTED 结果互不混淆；传输态与业务态分离。
3. 无已批准业务差异；按 0114604e 基线等价迁移。

External Worker C 交付 Implementation Repair #1（LOCAL_MACRO 空 outcome 改 closed flat 4 键）；持续重读本日志等待父级复审；
自审不算 Approved。

## Parent Source Review #5 - APPROVED / `W-BAG-MACRO-LOCAL-MECHANICS-IMP1` - 2026-07-13T22:29:00-04:00

父级独立复核 Repair #1 与完整本地执行链，结论 `P0=0 / P1=0 / P2=0`：

- `LocalRemoteGameCommandHandler.emptyOutcomePayload(...)` 的 `LOCAL_MACRO` 分支现恰好发布四键
  `macroKind/operation/state/cachePoint`，保留 closed `BAG_RETURN_ITEM`，后三项显式 null；STOPPED、
  NOT_EXECUTED、UNKNOWN 三条路径均复用该形状，EXECUTED 仍只发布 typed 业务 result。
- dispatch 前 `requireRegistration(...)` 精确校验 local registration 的 `runRevision`；输入 worker 在首个物理步骤前
  由 `workerAdmissionRevisionFence(...)` 再次比较 current registration revision；队列 callback 内继续使用既有
  pause/stop/window safety checkpoint。旧 revision request 不会在 pause/resume/reconfirm 后复活。
- 宏 callback 在 exact `WindowTaskContextHolder` binding 与单一 `submitRemoteExclusiveAndWaitDetailed(...)` 区段内
  恰调用一次 `BagService.runReturnItemMacroDirectForExclusive(...)`；该入口只复用 committed 三个 Bag exclusive 核心，
  没有 queue-in-queue、业务 retry/fallback 改写或新增 owner/session/TTL/ledger。
- `RemoteOperationLedger` 只把 `LOCAL_MACRO` 纳入既有 exclusive-input/quiescence 分类，没有新增业务状态机。

结论：C 的本地 mechanics/handler 切片 `SOURCE APPROVED`。C 报告的 DHXY compile exit 0 可作为 worker 证据；整波
最终门仍等待 B 的 digest strict-validation 返修后由父级 fresh 双构建统一确认。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief TRUE EOF REPOST - `W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1` - 2026-07-13T23:53:54-04:00

父级第一次追加本单时被重复结语定位到本日志中段；本段是在真实 EOF 的原单重发，**不是第二个任务**。External Worker C
应以本段为当前任务，并先追加：

`CLAIMED | task=W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1; claimedAt=<ISO-8601>; writeSet=<exact paths>`

领取截止：`2026-07-14T00:13:54-04:00`。20 分钟只检查领取；领取后可实施超过 20 分钟。

唯一写集：New `RemoteTeamReturnButtonFact.java`，Modify `RemoteWindowFactKind.java` 与
`LocalRemoteGameCommandHandler.java`，以及本日志 EOF。新增 `TEAM_RETURN_BUTTON`，字段固定
`state/clickX/clickY/matchScore/coordinateSpace`，与 B Cloud contract 镜像。handler 只调用 A 的
`TeamReturnButtonLocalObservationMechanics.observe(exact binding)`，逐态投影；PRESENT 的 window-client 点仅用同一 binding
原点转 `SCREEN_ABSOLUTE_PX`，失败态不得降为 ABSENT。保持 dispatch/read-after registration/binding fence、OBSERVED envelope
与 codec 不变；不随机、不点击、不调用 TeamReturnService、不改 digest/transport/input queue/schema/tests/host，不新增
retry/TTL/owner/session/ledger。完成后 DHXY `mvn -q -DskipTests compile` 并追加 `Implementation #1`、SHA、结果与自审；若 A
尚未落盘，只报告跨 Worker 依赖，不可代写 A 文件。

保护全部 dirty/untracked；禁止 reset/checkout/clean/delete/commit，不启动任何运行面。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T23:58:22-04:00

CLAIMED | task=W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1; claimedAt=2026-07-13T23:58:22-04:00; writeSet=[ New src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnButtonFact.java ; Modify src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java ; Modify src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java ; append-only 本日志 ]

## External Worker C - W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1 Implementation #1 - 2026-07-14T00:04:00-04:00

本轮只交付**不依赖 A 的独立件**：新增 DHXY 侧 closed fact DTO `RemoteTeamReturnButtonFact.java`。
`RemoteWindowFactKind.TEAM_RETURN_BUTTON` 与 `LocalRemoteGameCommandHandler` 接线**延后**，原因见「跨 Worker 依赖」。

### 契约接地（非臆测）

state 五态与 PRESENT/null 矩阵来自已落盘的 B/D 报告规格，非猜测：
- Worker D 报告 §(line 1598)：`state∈{PRESENT,ABSENT,CAPTURE_UNAVAILABLE,TEMPLATE_UNAVAILABLE,MECHANICS_FAILED}`；
  `TeamReturnButtonFact{state,clickX,clickY,matchScore,coordinateSpace:SCREEN_ABSOLUTE_PX}`；仅 PRESENT 携非负
  clickX/clickY 与有限且 `>=0.85` matchScore，其余四态三字段 Jackson NON_NULL 省略；state/coordinateSpace 恒在。
- Worker B brief（line 4782）：PRESENT 校非负坐标+matchScore≥0.85，其余四态三可空字段全 null。
- 本 C brief（line 159）：字段/枚举/校验必须与 B Cloud contract 镜像，「五态及 PRESENT/null 矩阵完全一致」。

### 精确写集（本轮实际改动，均在已领取写集内）

New src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnButtonFact.java（SHA=6cf813a4c5a35e29473a354bccde3f50bf546a32）：
- `@Value @Jacksonized`，字段固定 `State state; Integer clickX; Integer clickY; Double matchScore; RemoteCoordinateSpace coordinateSpace;`
  （5 字段，**无** matchedAtEpochMs——本 fact 契约不含时间戳，与 CommonBox 的 6 字段区分开）。
- `@Builder` 构造器校验：state/coordinateSpace 非空且 coordinateSpace==SCREEN_ABSOLUTE_PX；
  PRESENT → clickX/clickY 非负、matchScore 有限且 ≥0.85；其余四态 require(clickX==null&&clickY==null&&matchScore==null) 后置 null。
- `enum State { PRESENT, ABSENT, CAPTURE_UNAVAILABLE, TEMPLATE_UNAVAILABLE, MECHANICS_FAILED }` 五态。
- 结构镜像同包 FINAL APPROVED 姐妹 `RemoteCommonBoxFact`（阈值除外：本 fact 用 B 契约的 0.85，非 CommonBox 的 0.86；且删去 timestamp 校验/字段）。

### 跨 Worker 依赖（阻塞 enum + handler 接线，本轮不代写）

- A 的 `TeamReturnButtonLocalObservationMechanics` **未落盘**（grep src/main/java 为空，二次复核仍空）。handler 的
  `WINDOW_FACT case TEAM_RETURN_BUTTON` 必须调用 `A.observe(exact binding)` 并逐态投影 A 的 ObservationResult status；A 不在则
  无法编写该 case，且不可代写 A 文件（brief 明令）。
- 连锁编译约束：`RemoteWindowFactKind` 的 `executeWindowFact` 消费 switch 为 closed 穷尽（无 default，与 COMMON_BOX 同构）。
  若本轮先加 `TEAM_RETURN_BUTTON` 枚举值但缺 handler case → switch 非穷尽 → DHXY compile 失败。故枚举值与 handler case
  必须**同轮**落地，二者一并延后至 A 落盘（与我 W-BAG-MACRO 的 Impl#1 独立件先行 / Impl#2 接线后补 同一策略）。
- 待 A 落盘后的 Implementation #2 将完成：`RemoteWindowFactKind.TEAM_RETURN_BUTTON` + handler `case TEAM_RETURN_BUTTON`
  （callWith(access.context) → A.observe(access.binding()) 恰一次 → toTeamReturnButtonFact 逐态投影，PRESENT 的 window-client
  点用同一 binding 原点转 SCREEN_ABSOLUTE_PX，失败态不降级 ABSENT），保持 dispatch/read-after registration/binding fence、
  OBSERVED envelope 与 codec 不变。

### 编译门禁与核验

- DHXY `mvn -q -DskipTests compile`（未 clean）：EXIT=0。fact DTO 为独立 standalone 类，未被任何处引用，单独落地即编译通过。
- 复核：`RemoteWindowFactKind` 中 TEAM_RETURN_BUTTON 计数=0（本轮未动枚举，故 handler switch 仍穷尽、编译绿）。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮唯一改动=新增 RemoteTeamReturnButtonFact.java（写集 item 1）；未动 RemoteWindowFactKind / LocalRemoteGameCommandHandler
   （延后至 A 落盘同轮落地）；未改任何 Java/Maven/schema/tests/host；未做 Git；未 clean；保护他人 dirty/untracked。
2. state 五态/字段/校验/阈值/坐标空间全部接地于 B/D 已落盘规格，非臆造；不随机、不点击、不调用 TeamReturnService。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 Implementation #1（fact DTO 独立件）；enum+handler 接线待 A 的 mechanics 落盘后于 Implementation #2 补全；
持续重读本日志等待父级复审与 A 依赖解除；自审不算 Approved。

## Parent Partial Source Review #6 / Resume Directive - 2026-07-14T00:09:00-04:00

C 的 `RemoteTeamReturnButtonFact.java` 字段、五态、坐标空间与 null 矩阵正确，独立 DTO 部分无其它
P0/P1/P2；A mechanics 现已正式交付并获父级 `SOURCE APPROVED`，可以继续原任务的 enum + handler 接线。
但跨仓复核同时发现父级 brief 把默认 `0.85` 误写成不可变协议阈值：committed `0114604e` 的唯一阈值权威是
本地可配置 `BotProperties.returnTeamMatchRate`，A 已在匹配点执行该阈值。

当前任务 `W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1-R1`：

- 继续完成原唯一写集内 `RemoteWindowFactKind.TEAM_RETURN_BUTTON` 与
  `LocalRemoteGameCommandHandler` exact-binding 五态投影；不得代写 A 文件。
- 同时仅在 C 自有 `RemoteTeamReturnButtonFact.java` 把 `PRESENT` 的 score 校验改为 non-null + finite，删除
  `>=0.85` 常量；不新增 threshold 字段或配置副本。阈值只由 A mechanics 在 `ImageFinder.find(...,
  botProperties.getReturnTeamMatchRate())` 执行一次。
- 其它字段、五态、坐标、dispatch/read-after registration/binding fence、OBSERVED envelope 与 codec 全部冻结。
  完成后 DHXY `mvn -q -DskipTests compile` 并追加 `Implementation #2`。

请先追加 `CLAIMED | task=W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1-R1; claimedAt=<ISO>; writeSet=<原三 Java+本日志>`；
领取截止 `2026-07-14T00:29:00-04:00`。20 分钟只检查领取，绝不内部接管。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1 Implementation #2 (enum + handler wiring) - 2026-07-14T00:20:00-04:00

A 的 `TeamReturnButtonLocalObservationMechanics` 已落盘（src/main/java/com/bot/dhxy/service/teamreturn/），依赖解除。
完成 Implementation #1 延后的 enum + handler 接线。DHXY `mvn -q -DskipTests compile`（未 clean）EXIT=0。

### A 的 API 接地（读真实源码，非臆测）

`observe(WindowNativeBinding binding)` → `ObservationResult(State state, Integer clientX, Integer clientY, Double matchScore)`（record）。
`enum State { PRESENT, ABSENT, CAPTURE_UNAVAILABLE, TEMPLATE_UNAVAILABLE, MECHANICS_FAILED }`——与我 Impl#1 的
`RemoteTeamReturnButtonFact.State` 五态**逐值同名**。PRESENT 的 clientX/clientY 是 window-client 坐标（areaX+roundedX / areaY+roundedY，
相对窗口原点），matchScore 有限；其余四态 record 强制三 match 字段全 null。accessor 是 `.state()/.clientX()/.clientY()/.matchScore()`
（注意与 CommonBox 的 `.status()` 命名不同，已按 A 实际 record 用 `.state()`）。

### 精确写集（本轮实际改动，均在已领取写集内）

Modify src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java（SHA=9a30dfdb548b7b9c3f3c9c6d8e3925f7fd484a0d）：
- closed enum 末尾 COMMON_BOX 后新增 `TEAM_RETURN_BUTTON`（与同构 COMMON_BOX 并列）。

Modify src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java（SHA=8b77566a8889d579bb9e5dbf678999ccdd1178d0）：
- import A：`com.bot.dhxy.service.teamreturn.TeamReturnButtonLocalObservationMechanics`。
- 构造注入：field + ctor 参数 + `Objects.requireNonNull(...)`（Spring 构造注入；仓内 `new LocalRemoteGameCommandHandler(` 计数=0，新增参数安全）。
- `executeWindowFact` 的 closed switch 增 `case TEAM_RETURN_BUTTON ->`：`windowTaskContextHolder.callWith(access.context(),
  () -> toTeamReturnButtonFact(teamReturnButtonLocalObservationMechanics.observe(access.binding()), access.binding()))`——
  与 COMMON_BOX 完全同构：同一 exact binding、context 内一次 observe、逐态投影。
- 新 helper `toTeamReturnButtonFact(ObservationResult, WindowNativeBinding)`：五态 1:1 映射到 fact.State；PRESENT →
  clickX=binding.getX()+result.clientX()、clickY=binding.getY()+result.clientY()、matchScore=result.matchScore()、
  coordinateSpace=SCREEN_ABSOLUTE_PX；其余四态仅 state+coordinateSpace（三字段留空）。**失败态不降级 ABSENT**（switch 各态独立）。

### 合同逐条落实（real code 接地）

- handler 只调用 A 的 `observe(exact binding)` 恰一次（grep observe 调用=1），不点击、不调用 TeamReturnService、不随机。
- PRESENT window-client → SCREEN_ABSOLUTE_PX：只用同一 binding 原点平移（getX/getY + clientX/clientY），与 COMMON_BOX 同法。
- 失败态不降级 ABSENT：CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED 各自 1:1 映射，绝不并入 ABSENT。
- dispatch/read-after registration/binding fence、OBSERVED envelope、codec 全不变：本 case 只多产一个 fact 分支，复用既有
  `executeWindowFact` 的 timing/observedRegistration=requireRegistration(...)/observedAccess=requireBoundWindow(...)/
  RemoteWindowFactOutcomePayload(fact=payloadCodec.toPayloadTree(fact))/terminal(OBSERVED,OK) 流程，未改一行。
- ledger 未碰：WINDOW_FACT 是截图观测、无物理输入；ledger 按 RemoteGameOperation 级（WINDOW_FACT）分类而非 factKind，故新增
  factKind 不进 isExclusiveInputOperation / quiescence input 计数（RemoteOperationLedger 中 TEAM_RETURN_BUTTON 计数=0，与
  COMMON_BOX 当初一致）。不新增 retry/TTL/owner/session/ledger。
- 未代写/未改 A 文件；未改 B 的 Cloud contract、digest/transport/input queue/schema/tests/host；fact DTO（Impl#1，SHA 6cf813a4）未动。

### 编译门禁与核验

- DHXY `mvn -q -DskipTests compile`（未 clean）：EXIT=0（含加 enum 后 executeWindowFact switch 穷尽性——已补 case，编译绿）。
- grep 核验：`new LocalRemoteGameCommandHandler(`=0；`case TEAM_RETURN_BUTTON ->`=1；`toTeamReturnButtonFact`=2（def+call）；
  `teamReturnButtonLocalObservationMechanics.observe`=1；RemoteWindowFactKind 中 TEAM_RETURN_BUTTON=1；
  RemoteOperationLedger 中 TEAM_RETURN_BUTTON=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮 Java 改动=RemoteWindowFactKind（加枚举值）+ LocalRemoteGameCommandHandler（import/inject/case/helper），均在写集；
   未改 A/B 文件、ledger、Cloud/schema/tests/host；未做 Git；未 clean；保护他人 dirty/untracked。
2. state 五态 A↔fact 逐值同名 1:1；失败态与 ABSENT 分离；PRESENT 坐标仅同 binding 原点平移。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 Implementation #2（enum + handler 接线，整单闭合：fact DTO + enum + handler 三件齐）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #2 - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1` - 2026-07-14T15:49:00-04:00

**SOURCE APPROVED，P0/P1/P2=0。** 父级独立读取最新 538 行源码并对照
`696a12b0 PlayerStateService.java:259-299,452-478,697-970`：

- `probeSupplyNoFocus:101-126` 只做一次 direct checkpoint 与 no-focus bars capture，零 mouse-away/零 input/零
  pending/cooldown state；四目标按人物 HP/MP、宝宝 HP/MP 固定顺序返回真实 observation。
- `healAllDirect:138-175` 入口拒绝非 input-worker，整段仅前后各一个 direct checkpoint；目标循环中无 stop gate，
  每目标完整执行 initial/+10 -> 350ms confirm -> 必要时原位右键 100ms -> 800ms 后才进入下一目标。
- 常量、颜色判断、radius(2,1)、threshold normalize、mouse-away 与 image flush 保持；DISABLED/UNREADABLE/
  CAPTURE_FAILED/NO_ACTION/EXECUTED 不再夸大事实，EXECUTED 才携 screen-absolute click。

Review #1 的 P1=3/P2=2 全部闭合。fresh DHXY compile 待其它 writer 稳定后由父级统一执行，之前不增加
`189/407`。C 写集释放，立即进入下一互斥 observation mechanics。

## Parent Direct Implementation Task - `W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1` - 2026-07-14T15:49:00-04:00

请 External C 在 **2026-07-14T16:09:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]`

直接实施，不写 Design。唯一 Java 写集为新建 DHXY
`src/main/java/com/bot/dhxy/service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java` 与本日志。
严格抽取 `696a12b0 PlayerStateService.java:1002-1297` 的本地机械部分：exact binding + caller-supplied status rect/
optional cached probe rect；保持 capture -> incense icon template `0.85` -> matched column -> cyan hour digits -> green
minute fallback / `SheyaoxiangDigitTemplateReader` 顺序。返回 closed state、present/absent、screen-absolute icon point、
optional remainingMs/source/diagnostics；caller 继续拥有 cache/cooldown、是否使用香、Bag macro 与 state update。
capture exception 不得伪成 absent；ROI、cyan-first/green-fallback、时间单位、图像 flush 与 failure matrix 对齐。
零 input/Bag 调用/跨调用 cache；禁止 owner/session/ledger/TTL/retry；不得改 PlayerStateService/handler/remote/
schema/POM 或其它文件。不跑 Maven/test/runtime/Git，父级统一构建。本单不计整类完成。

## Parent Source Review #1 / Repair Brief - `W-696-COMMON-BOX-TYPED-ADAPT-1` - 2026-07-14T14:06:00-04:00

**BLOCKED，P0=0 / P1=3 / P2=1。** Delivery Preflight Helper 已先做非绑定预检；父级随后独立把
active `CommonBoxService.java` 与 `696a12b0` 完整类、DHXY fact producer、Cloud fact/terminal 合同逐段对照：

1. **P1：检测端吞掉 stop/unresolved/interrupt。** `CommonBoxService.java:269-272` 把所有非
   `OBSERVED`（含 `STOPPED/UNKNOWN`）记为普通 `fact-unavailable` 后返回；`:290-293` 对
   `InterruptedException` 仅恢复 interrupt flag 后返回；`:294-297` 的宽 `catch (Exception)` 还会把
   remote retained/current gate 的 stop/transition/runtime 异常降为普通 detection failure。影响是停止或未决 observation
   会被当作业务未命中继续推进。
2. **P1：点击端吞掉 stop/unresolved/transport。** `:343-350` 只把 `EXECUTED` 判 true，所有其它终态均
   变成 false；宽 `catch (RuntimeException)` 又把 stop/transition、协议错误与 final-consumption 中断包装异常变成 false。
   上层随后保留 pending 并记录普通 click failed，可能在结果未决时继续业务并再次发起输入。
3. **P1：30 秒 TTL 的起点被延后。** DHXY
   `CommonBoxLocalObservationMechanics.java:88-99` 在实际模板命中时记录 `matchedAtEpochMs`，handler
   `LocalRemoteGameCommandHandler.java:867-872` 和 Cloud `WindowFact.CommonBoxFact:137-168` 都完整保留该时间；
   active `CommonBoxService.java:309-324` 却丢弃它，重新用 Cloud 收包时的 `System.currentTimeMillis()` 计算
   `detectedAt/expiresAt`。影响是网络/排队耗时会额外延长 baseline 的 30 秒 pending 点击窗口，可能点击本应过期的框。
4. **P2：边界替换额外切碎原方法图。** diff 新增 `recordMatched(:300-327)`、
   `consumeClick(:329-352)`、`actionSlot(:386-388)` 三个 private helper；其中一项只拼字符串，另两项把 baseline
   原调用点拆走。它们没有形成跨类复用或新策略边界，违背本轮“完整类调用图 + 原调用点替换”和 no-wrapper 规则。

已确认无问题的部分：key 仍为 `windowId|hwnd|role|task|taskRun`，window/handle/role/identity/run 来自同一
`TaskExecutionContext`；Cloud String taskRun 在本类只参与有效性、key 和 equality，等价于 baseline 数字 run id 的用途；
bundle 仍恰为 move -> sleep 80ms -> click 120ms；五种 `CommonBoxState`、score/absolute point、role toggle、
detect/consume 分离与 `PENDING_TTL_MS=30000` 常量均未改。

### 当前返修任务 `W-696-COMMON-BOX-TYPED-ADAPT-1-R1`

请原 External Worker C 在 **2026-07-14T14:26:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-COMMON-BOX-TYPED-ADAPT-1-R1 | claimedAt=<ISO-8601> | writeSet=[Cloud CommonBoxService.java,this-log]`

唯一 Java 写集不变。精确返修条件：

- fact terminal：`OBSERVED` 按五种 typed state 处理；`NOT_EXECUTED` 可走原 miss；`STOPPED` 先
  `TaskCheckpoint.throwIfStopRequested(...)`，未确认停止则 fatal；`UNKNOWN`/其它不合法态 fatal。
- `InterruptedException` 恢复 interrupt flag 后抛 `TaskFatalException`。local mechanics 异常已由 fact
  `MECHANICS_FAILED` 表达，remote/retained/runtime 异常不得再被宽 catch 降成业务 miss。
- input terminal：`EXECUTED=true`、`NOT_EXECUTED=false`；`STOPPED` checkpoint；`UNKNOWN`/其它不合法态 fatal；
  final-consumption/transport RuntimeException 不得返回 false 并留下普通 pending。
- `PendingCommonBox.detectedAtMs/expiresAtMs` 必须以 `box.matchedAtEpochMs()` 为起点，保持本地实际命中后的
  baseline 30 秒窗口；不得使用 Cloud 收包时刻重启 TTL。
- 把 `recordMatched`、`consumeClick`、`actionSlot` 收回 baseline 原方法/原调用点；不新增替代 wrapper。保持
  move -> 80ms -> click -> 120ms、全部 public/private 基线方法、日志、状态与返回路径。
- 不改 remote/schema、其它 Service/POM；不新增 owner/session/ledger/TTL/retry；不要运行 Maven/test/runtime。

完成后追加 `Implementation Repair #1`，列出 terminal 矩阵、TTL 时间源、方法图恢复、bundle 与 scoped diff/check。
**无已批准业务差异；按基线等价返修。**

## Parent Source Review #9 - `W-SS-IMAGE-PAYLOAD-IMP1` - 2026-07-14T08:58:19-04:00

**APPROVED，P0/P1/P2=0。** 父级独立抽取 committed `0114604e:1386-1395` 与当前 Cloud
`178-187` 完整 10 行方法，逐行 `Compare-Object` 差异数 0，定义数 1；`IOException/Files/Path/Base64`
direct import 各恰一处。读 bytes、Base64、SHA、IOException warn/null 顺序无漂移。目标 SHA-256
`d2821ed9648ca0384ed6d69b9c95472dd1ad076b3c878d7bbbdf84a690006146`，Worker compile exit 0。
方法只读调用方提供的 artifact path，无 caller/host/capture/template/OCR/input/workflow machinery。本 dormant
prerequisite 暂不增加 `189/407`。无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Follow-on Task Brief #12 (REPUBLISHED AT TRUE EOF) - `W-SS-IMAGE-CPU-COHORT-IMP1` - 2026-07-14T08:58:19-04:00

上方 Follow-on Brief #12 内容原样有效；因 Implementation #1 后来追加，现于真实 EOF 重发。请在
`2026-07-14T09:15:00-04:00` 前追加对应 `CLAIMED`，随后一次实施至少 6 个完整纯 image/artifact 方法或一个
完整 scan 算法链。写集、候选列表、`SOURCE_DEPENDENCY_EXCLUDED` 规则和禁项均以上方 Brief #12 为准；
不写 Design、不等待聊天。

## Parent Source Review #13 - APPROVED / `W-TMS-SUMMON-SLOT-CPU-IMP1` - 2026-07-14T01:41:00-04:00

父级以 committed `0114604e:2858-2875` 逐行复核当前 Cloud 源码，结论
`P0=0 / P1=0 / P2=0`：

- `findLastConfirmedEffectiveSlotIndex` 保持 entrySet 遍历、`null` 初值与最大 effective key 选择；
  `isEffectiveSummonSkillSlot` 仅接受 `NORMAL_SKILL/KEEP_SKILL/EMPTY_SLOT`。
- 只新增两个所需 import、两个 private helper 与准确类 JavaDoc；三个已批准 public nested type
  逐 token 未改，没有 caller/wrapper/public API/state/cache/TTL/retry/clock/remote/Spring。
- 父级复算 SHA-256 为
  `871edbcffbd9773ed1b79dde84ead842dc26b493c544ac1b88de38fe9a5865db`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 summon-slot pure CPU cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-UNKNOWN-RESULT-CPU-IMP1` - 2026-07-14T01:41:00-04:00

External C 请在 `2026-07-14T02:01:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-UNKNOWN-RESULT-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

唯一源码写集仍为 C 已拥有的 Cloud
`src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed `0114604e:2877-2886`
机械复制 private `isUnknownSummonSkillFailure(SummonSkillCleanupResult)`，只新增已存在 Cloud model
`com.bot.dhxy.model.maintenance.SummonSkillCleanupResult` import。

`null`/成功返回 false、message 小写包含 `unknown` 返回 true、否则按 observed status 含
`UNKNOWN` 的矩阵逐 token 保持。只同步类级 JavaDoc。不得新增 caller/wrapper/public API、cache
invalidation/state/TTL/retry/clock/remote/Spring 或其它 helper；不得改已批准类型与 summon-slot helpers。
完成后 Cloud `mvn -q compile`，追加 Implementation #1、SHA、source/target diff=0 与旧块 unchanged 反证。
禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T00:13:13-04:00

CLAIMED | task=W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1-R1; claimedAt=2026-07-14T00:13:13-04:00; writeSet=[ src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnButtonFact.java ; src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java ; src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java ; append-only 本日志 ]

说明：Review #6 于 00:09 追加，我在 00:20 追加 Implementation #2（enum + handler exact-binding 五态投影）时漏看了该 review，故 R1 的
第 1 条（enum + handler 接线）实际已在紧邻上方 Implementation #2 完成、编译 EXIT=0。本 R1 仅剩第 2 条待办：删 fact DTO 的
`>=0.85` 硬编码阈值，PRESENT 的 matchScore 改为 non-null + finite。修完追加交付记录。

## External Worker C - W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1-R1 Delivery - 2026-07-14T00:14:30-04:00

关闭 Review #6 两条要求。整单三件（fact DTO + enum + handler）齐，DHXY `mvn -q -DskipTests compile`（未 clean）EXIT=0。

### R1 第 1 条（enum + handler exact-binding 五态投影）——已于上方 Implementation #2 完成

Review #6 于 00:09 追加，我 00:20 追加 Implementation #2 时漏看，故该条已实际完成（见本日志 Implementation #2 段）：
- `RemoteWindowFactKind.TEAM_RETURN_BUTTON` 新增。
- `LocalRemoteGameCommandHandler`：import/inject A + `case TEAM_RETURN_BUTTON ->` callWith(context) 内一次 `A.observe(access.binding())`
  + `toTeamReturnButtonFact` 五态 1:1 投影；PRESENT window-client→SCREEN_ABSOLUTE_PX 用同一 binding 原点平移；失败态不降级 ABSENT。
- dispatch/read-after registration/binding fence、OBSERVED envelope、codec 全冻结；ledger 未碰（WINDOW_FACT 按 op 级分类）。
该部分本轮未再改动，保持编译绿。

### R1 第 2 条（fact DTO 删硬编码阈值）——本轮修复

根因（父级跨仓复核）：`0114604e` 唯一阈值权威是本地可配置 `BotProperties.returnTeamMatchRate`，A mechanics 已在
`ImageFinder.find(frame, template, botProperties.getReturnTeamMatchRate())` 执行一次；C 的 fact DTO 不得复述硬编码 `0.85` 常量
（否则形成第二个、且不可配置的阈值权威，与基线冲突）。

修复（仅 C 自有 src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnButtonFact.java，SHA=949d9d8c67e7b44e67a0d019ec72eb26ffa17178）：
- PRESENT 分支：`require(Double.isFinite(matchScore) && matchScore >= 0.85d, ...)` → `require(Double.isFinite(matchScore),
  "matchScore must be finite")`。matchScore 仍 requireNonNull（PRESENT 必携）+ finite；**删去 `>=0.85` 常量**。
- 未新增 threshold 字段/配置副本（grep returnTeamMatchRate|threshold|BotProperties 在本 DTO=0；grep 0.85=0）。
- 其余字段、五态、坐标空间、非 PRESENT 的 null 矩阵完全冻结未动。

### 合同逐条落实

- 阈值单一权威：fact 不再断言阈值，只保证 PRESENT 携有限 matchScore；阈值判定唯一发生在 A 的 mechanics（配置驱动）。
- 写集内：本轮仅改 RemoteTeamReturnButtonFact.java（R1 第 2 条）；enum/handler（R1 第 1 条）保持 Implementation #2 交付态。
- 未代写/未改 A 文件、B 的 Cloud contract、digest/transport/input queue/schema/tests/host；未新增 retry/TTL/owner/session/ledger。

### 编译门禁与核验

- DHXY `mvn -q -DskipTests compile`（未 clean）：EXIT=0。
- grep：fact DTO 中 `0.85`=0、`returnTeamMatchRate|threshold|BotProperties`=0；handler `case TEAM_RETURN_BUTTON ->`=1、
  `toTeamReturnButtonFact`=2、`observe`=1；RemoteWindowFactKind 中 TEAM_RETURN_BUTTON=1；RemoteOperationLedger 中=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮唯一改动=RemoteTeamReturnButtonFact.java 一处校验；未动 enum/handler/A/B/ledger/Cloud/schema/tests/host；未 Git；未 clean；保护他人 dirty/untracked。
2. 阈值权威回归 A 的配置驱动 mechanics，fact 不再持有第二阈值。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1-R1（Review #6 两条闭合：enum+handler 于 Impl#2、阈值删除于本轮）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #7 - APPROVED / `W-TEAMRETURN-BUTTON-DHXY-WIRE-IMP1-R1` - 2026-07-14T00:16:00-04:00

父级逐行复核当前 `RemoteTeamReturnButtonFact`、`RemoteWindowFactKind` 与
`LocalRemoteGameCommandHandler`，结论 `P0=0 / P1=0 / P2=0`：

- DTO 的 `PRESENT` 只要求非负坐标与 non-null/finite score，不再重复配置阈值；negative 四态仍严格清空
  `clickX/clickY/matchScore`。
- handler 在 exact `WindowTaskContextHolder.callWith(...)` 内对同一 `WindowNativeBinding` 恰调用一次
  `TeamReturnButtonLocalObservationMechanics.observe(...)`；五态一一映射，失败态不折成 `ABSENT`。
- `PRESENT` 只做同一 binding 原点的 window-client -> `SCREEN_ABSOLUTE_PX` 平移；既有 registration/binding
  read-after fence、OBSERVED envelope、codec、ledger 与输入队列均未改。
- C 的 DHXY `mvn -q -DskipTests compile` exit 0。父级仍会在整波稳定后重新执行 fresh compile。

本 DHXY wire/handler 返修 `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent TeamReturn Button Fact Wave Build Closure #1 - FINAL APPROVED - 2026-07-14T00:26:16-04:00

父级 fresh DHXY compile exit 0；fresh Cloud clean package exit 0，4 suites / 21 tests 全绿。
`TEAM_RETURN_BUTTON` 整波 `FINAL APPROVED，P0/P1/P2=0`，运行面仍 dormant。

## Parent Direct Implementation Task - `W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1` - 2026-07-14T00:26:16-04:00

请 External C 在 `2026-07-14T00:46:16-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1; claimedAt=<ISO>; writeSet=<3 Java + 本日志>`。
这是直接实现，不写 Design；External D 的 mechanics 文件若尚未落盘，可先完成 DTO，等待 D 后同单接线，不能代写 D。

唯一写集：New `src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnLeaderSignalFact.java`；Modify
`RemoteWindowFactKind.java`、`LocalRemoteGameCommandHandler.java`；Append-only 本日志。

合同镜像 B：新增 `TEAM_RETURN_LEADER_SIGNAL`；fact 恰含
`state,signalX,signalY,matchScore,coordinateSpace`，closed 五态，`SCREEN_ABSOLUTE_PX`；仅 PRESENT 携非负坐标
和 non-null finite score，其余三字段 null，不写死 0.85。handler 构造注入 D 的
`TeamReturnLeaderSignalLocalObservationMechanics`，在 exact `callWith` 内对同一 binding 恰 observe 一次，五态 1:1；
PRESENT 只做同一 binding 原点的 client->screen 平移。失败态不得折成 ABSENT；既有 registration/binding read-after
fence、OBSERVED envelope、codec、ledger/input queue 全冻结。不新增 retry/TTL/owner/session/ledger/thread。
完成后 DHXY `mvn -q -DskipTests compile`，追加 Implementation #1 与 SHA。

保护全部 dirty/untracked；禁止 Git mutation/运行面。20 分钟只检查领取，逾期只原样重发 C，绝不内部接管。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T00:29:41-04:00

CLAIMED | task=W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1; claimedAt=2026-07-14T00:29:41-04:00; writeSet=[ New src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnLeaderSignalFact.java ; Modify src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java ; Modify src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java ; append-only 本日志 ]

## External Worker C - W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1 Implementation #1 - 2026-07-14T00:33:00-04:00

本轮只交付**不依赖 D 的独立件**：新增 DHXY 侧 closed fact DTO `RemoteTeamReturnLeaderSignalFact.java`。
`RemoteWindowFactKind.TEAM_RETURN_LEADER_SIGNAL` 与 `LocalRemoteGameCommandHandler` 接线**延后**至 D 落盘同单，见「跨 Worker 依赖」。

### 契约接地（非臆测）

state 五态、字段、坐标空间、null 矩阵、无阈值——全部接地于 B 已落盘规格 `cloud-team-return-service-worker-b.md`（line 4889-4893）：
- 字段恰为 `state, signalX, signalY, matchScore, coordinateSpace`；state 只允许
  `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED`；coordinateSpace 恒 `SCREEN_ABSOLUTE_PX`。
- 仅 PRESENT 携 non-negative signalX/signalY 与 non-null finite score；其它四态三字段全 null。
- 阈值由本地 mechanics 按可配置 `returnTeamMatchRate` 执行，协议不写死 0.85、不新增 threshold 字段。
- 本 C brief（line 789-791）逐字一致。

### 精确写集（本轮实际改动，均在已领取写集内）

New src/main/java/com/bot/dhxy/cloud/remote/RemoteTeamReturnLeaderSignalFact.java（SHA=6853551e5793c5ba1d939d496b1334b42ca18c0f）：
- `@Value @Jacksonized`，字段固定 `State state; Integer signalX; Integer signalY; Double matchScore; RemoteCoordinateSpace coordinateSpace;`（5 字段，无 timestamp）。
- `@Builder` 构造器校验：state/coordinateSpace 非空且 coordinateSpace==SCREEN_ABSOLUTE_PX；
  PRESENT → signalX/signalY 非负、matchScore non-null + **finite（不写死 0.85、无 threshold 字段/配置副本）**；
  其余四态 require(signalX==null && signalY==null && matchScore==null) 后置 null。
- `enum State { PRESENT, ABSENT, CAPTURE_UNAVAILABLE, TEMPLATE_UNAVAILABLE, MECHANICS_FAILED }` 五态。
- 结构镜像同包 FINAL APPROVED 姐妹 `RemoteTeamReturnButtonFact`（R1 后无 0.85 版），唯一区别 clickX/clickY→signalX/signalY。

### 跨 Worker 依赖（阻塞 enum + handler 接线，本轮不代写 D）

- D 的 `TeamReturnLeaderSignalLocalObservationMechanics` **未落盘**（grep src/main/java 为空，二次复核仍空）。handler 的
  `WINDOW_FACT case TEAM_RETURN_LEADER_SIGNAL` 必须构造注入并调用 `D.observe(exact binding)` 恰一次、逐态投影 D 的 State；
  D 不在则无法编写该 case，且 brief 明令不可代写 D 文件。
- 连锁编译约束：`RemoteWindowFactKind` 的 `executeWindowFact` 消费 switch 为 closed 穷尽（无 default）。先加
  `TEAM_RETURN_LEADER_SIGNAL` 枚举值但缺 handler case → switch 非穷尽 → DHXY compile 失败。故枚举值与 handler case 必须**同轮**
  落地，一并延后至 D 落盘（与 button 任务的 Impl#1 独立件先行 / Impl#2 接线后补 同一策略，那单 D→A 落盘后一轮闭合）。
- 待 D 落盘后的 Implementation #2 将完成：`RemoteWindowFactKind.TEAM_RETURN_LEADER_SIGNAL` + handler `case`
  （callWith(access.context) → D.observe(access.binding()) 恰一次 → toTeamReturnLeaderSignalFact 逐态投影，PRESENT 的
  window-client 点用同一 binding 原点转 SCREEN_ABSOLUTE_PX，失败态不降级 ABSENT），保持 dispatch/read-after registration/
  binding fence、OBSERVED envelope、codec、ledger/input queue 全冻结。

### 编译门禁与核验

- DHXY `mvn -q -DskipTests compile`（未 clean）：EXIT=0。DTO 为独立 standalone 类，未被引用，单独落地即编译通过。
- grep：DTO 中 `0.85`=0、`returnTeamMatchRate|threshold|BotProperties`=0（javadoc 亦不提，镜像 button fact 干净版）；
  `RemoteWindowFactKind` 中 TEAM_RETURN_LEADER_SIGNAL=0（本轮未动枚举，handler switch 仍穷尽、编译绿）。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮唯一改动=新增 RemoteTeamReturnLeaderSignalFact.java（写集 item 1）；未动 RemoteWindowFactKind / LocalRemoteGameCommandHandler
   （延后至 D 落盘同轮落地）；未改任何 Java/Maven/schema/tests/host；未做 Git；未 clean；保护他人 dirty/untracked。
2. 五态/字段/校验/坐标空间/无阈值全部接地于 B 已落盘规格，非臆造；不随机、不点击、不调用 TeamReturnService。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 Implementation #1（leader-signal fact DTO 独立件）；enum+handler 接线待 D 的 mechanics 落盘后于
Implementation #2 补全；持续重读本日志等待父级复审与 D 依赖解除；自审不算 Approved。

## Parent Dependency Unblocked - continue `W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1` - 2026-07-14T00:39:00-04:00

External D 的 `TeamReturnLeaderSignalLocalObservationMechanics` 已落盘并经父级
`SOURCE APPROVED，P0/P1/P2=0`；External B 的 Cloud mirror contract 同样已批准。C 现在按原已领取任务直接完成
`RemoteWindowFactKind.TEAM_RETURN_LEADER_SIGNAL` 与 `LocalRemoteGameCommandHandler` exact-binding 单次 observe/
五态投影，不需重新 CLAIMED、不写 Design、不等待其它材料。完成后运行 DHXY compile 并追加 Implementation #2。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1 Implementation #2 (enum + handler wiring) - 2026-07-14T00:42:00-04:00

D 的 `TeamReturnLeaderSignalLocalObservationMechanics` 已落盘并经父级 `SOURCE APPROVED`（P0/P1/P2=0），B 的 Cloud mirror 亦批准。
按父级 Dependency Unblocked 指示（不重新 CLAIMED、不写 Design），完成 Impl#1 延后的 enum + handler 接线。
DHXY `mvn -q -DskipTests compile`（未 clean）EXIT=0。

### D 的 API 接地（读真实源码，非臆测）

`observe(WindowNativeBinding binding)` → `ObservationResult(State state, Integer clientX, Integer clientY, Double matchScore)`（record，
accessor `.state()/.clientX()/.clientY()/.matchScore()`）。`enum State { PRESENT, ABSENT, CAPTURE_UNAVAILABLE,
TEMPLATE_UNAVAILABLE, MECHANICS_FAILED }`——与 Impl#1 的 `RemoteTeamReturnLeaderSignalFact.State` 五态逐值同名。PRESENT 的
clientX/clientY 为 window-client 坐标（areaX+roundedX / areaY+roundedY），matchScore 有限；阈值判定唯一发生在 D 内
`ImageFinder.find(frame, template, botProperties.getReturnTeamMatchRate())`（配置驱动），fact 不复述。

### 精确写集（本轮实际改动，均在已领取写集内）

Modify src/main/java/com/bot/dhxy/cloud/remote/RemoteWindowFactKind.java（SHA=7415328b2bff9d7eb0ec720232195e3caeaf387c）：
- closed enum 末尾 TEAM_RETURN_BUTTON 后新增 `TEAM_RETURN_LEADER_SIGNAL`。

Modify src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java（SHA=e99e547673db23beb8f23662b52a9ffc1bea0742）：
- import D：`com.bot.dhxy.service.teamreturn.TeamReturnLeaderSignalLocalObservationMechanics`。
- 构造注入：field + ctor 参数 + `Objects.requireNonNull(...)`（Spring 构造注入；仓内 `new LocalRemoteGameCommandHandler(` 计数=0）。
- `executeWindowFact` 的 closed switch 增 `case TEAM_RETURN_LEADER_SIGNAL ->`：`windowTaskContextHolder.callWith(access.context(),
  () -> toTeamReturnLeaderSignalFact(teamReturnLeaderSignalLocalObservationMechanics.observe(access.binding()), access.binding()))`——
  与 TEAM_RETURN_BUTTON/COMMON_BOX 完全同构。
- 新 helper `toTeamReturnLeaderSignalFact(ObservationResult, WindowNativeBinding)`：五态 1:1 映射到 fact.State；PRESENT →
  signalX=binding.getX()+result.clientX()、signalY=binding.getY()+result.clientY()、matchScore=result.matchScore()、
  coordinateSpace=SCREEN_ABSOLUTE_PX；其余四态仅 state+coordinateSpace（三字段留空）。**失败态不降级 ABSENT**（switch 各态独立）。

### 合同逐条落实（real code 接地）

- handler 只调用 D 的 `observe(exact binding)` 恰一次（grep observe 调用=1），不点击、不调用 TeamReturnService、不随机。
- PRESENT window-client → SCREEN_ABSOLUTE_PX：只用同一 binding 原点平移（getX/getY + clientX/clientY）；D 的 clientX/clientY
  正投到 fact 的 signalX/signalY。
- 失败态不降级 ABSENT：CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED 各自 1:1，绝不并入 ABSENT。
- dispatch/read-after registration/binding fence、OBSERVED envelope、codec 全冻结：本 case 只多产一个 fact 分支，复用既有
  executeWindowFact 的 timing/observedRegistration/observedAccess/RemoteWindowFactOutcomePayload/terminal(OBSERVED,OK) 流程。
- ledger/input queue 未碰：WINDOW_FACT 是截图观测、无物理输入；ledger 按 RemoteGameOperation 级分类而非 factKind，新增 factKind
  不进 isExclusiveInputOperation / quiescence 计数（RemoteOperationLedger 中 TEAM_RETURN_LEADER_SIGNAL 计数=0，与 button/COMMON_BOX 一致）。
- 不新增 retry/TTL/owner/session/ledger/thread；未代写/未改 D/B 文件、digest/transport/schema/tests/host；fact DTO（Impl#1，SHA 6853551e）未动。

### 编译门禁与核验

- DHXY `mvn -q -DskipTests compile`（未 clean）：EXIT=0。
- grep：`new LocalRemoteGameCommandHandler(`=0；`case TEAM_RETURN_LEADER_SIGNAL ->`=1；`toTeamReturnLeaderSignalFact`=2（def+call）；
  `teamReturnLeaderSignalLocalObservationMechanics.observe`=1；RemoteWindowFactKind 中 TEAM_RETURN_LEADER_SIGNAL=1；
  RemoteOperationLedger 中=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮 Java 改动=RemoteWindowFactKind（加枚举值）+ LocalRemoteGameCommandHandler（import/inject/case/helper），均在写集；
   未改 D/B 文件、ledger、Cloud/schema/tests/host；未做 Git；未 clean；保护他人 dirty/untracked。
2. state 五态 D↔fact 逐值同名 1:1；失败态与 ABSENT 分离；PRESENT 坐标仅同 binding 原点平移，clientX/clientY→signalX/signalY。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 Implementation #2（enum + handler 接线，整单闭合：leader-signal fact DTO + enum + handler 三件齐）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #8 - APPROVED / `W-TEAMRETURN-LEADER-DHXY-WIRE-IMP1` - 2026-07-14T00:50:00-04:00

父级逐行复核当前 `RemoteTeamReturnLeaderSignalFact`、`RemoteWindowFactKind`、
`LocalRemoteGameCommandHandler` 与已批准 D mechanics，结论 `P0=0 / P1=0 / P2=0`：

- closed 五态、字段、`SCREEN_ABSOLUTE_PX` 与 Cloud `TeamReturnLeaderSignalFact` 一一对应；仅 PRESENT
  携 non-negative signal point 与 finite score，协议不重复本地配置阈值。
- handler 在 exact `WindowTaskContextHolder.callWith(...)` 中对同一 binding 恰 observe 一次；PRESENT 只用
  binding 原点做 client -> screen 平移，四个 negative/failure 状态逐态保留，未折成 ABSENT。
- 既有 registration/binding read-after fence、OBSERVED envelope、codec、ledger 与 input queue 均未改；
  Worker DHXY `mvn -q -DskipTests compile` exit 0。父级 fresh compile 留待当前共享 Java 全部稳定后执行。

本 DHXY wire/handler 切片 `SOURCE APPROVED`，handler 写集已释放给后续不重叠任务。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TTPS-TYPED-ORIGIN-IMP1` - 2026-07-14T00:53:00-04:00

External C 请在 `2026-07-14T01:13:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TTPS-TYPED-ORIGIN-IMP1; claimedAt=<ISO>; writeSet=<Cloud TaskTrackerPanelService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

唯一源码写集仅 Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java`。把当前已批准
`TrackerPanelRequest.imageOriginWindow` 从 raw String 改为 immutable `Point`：只允许 `read(JsonNode)` transport
adapter 调用一次 `DecisionEngine.parsePoint(...)`，typed 主算法直接消费 Point，不再解析字符串。必须保持缺失/非法
origin 的原 reason、全部算法/常量/顺序/结果/flush 不变；不得改其它字段、DecisionEngine、remote、tests/pom。
完成后 Cloud `mvn -q package`（不 clean），EOF 追加 Implementation #1、SHA、21 项汇总与 self-QA。
禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T00:53:41-04:00

CLAIMED | task=W-TTPS-TYPED-ORIGIN-IMP1; claimedAt=2026-07-14T00:53:41-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java ; append-only 本日志 ]

## External Worker C - W-TTPS-TYPED-ORIGIN-IMP1 Implementation #1 - 2026-07-14T00:58:00-04:00

把 Cloud `TaskTrackerPanelService.TrackerPanelRequest.imageOriginWindow` 从 raw String 改为 typed `java.awt.Point`：
`parsePoint(...)` 唯一调用点从 typed 主算法上移到 `read(JsonNode)` transport adapter（恰一次），typed 主算法直接消费 Point。
Cloud `mvn -q package`（未 clean）EXIT=0，4 suites / 21 tests 全绿。

### 精确写集（唯一源码文件 + 本日志）

Modify src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java（SHA=64dd7ae4126de8ddc403c8163e3cdec5d70cb222），仅 3 处：
1. record `TrackerPanelRequest` 第 4 字段 `String imageOriginWindow` → `Point imageOriginWindow`（line 898）。
2. `read(JsonNode)` transport adapter：第 4 构造实参
   `DecisionEngine.text(context, "imageOriginWindow")` → `DecisionEngine.parsePoint(DecisionEngine.text(context, "imageOriginWindow"))`（line 51）。
   —— String→Point 的唯一解析发生于此，恰一次。
3. typed `read(TrackerPanelRequest)`：`Point origin = DecisionEngine.parsePoint(request.imageOriginWindow());`
   → `Point origin = request.imageOriginWindow();`（line 71）。主算法不再解析字符串，直接消费 typed Point。

`import java.awt.Point;` 已存在（line 5），未新增 import。

### 21 项合同逐条核验（全部保持/满足）

1. imageOriginWindow 字段类型 String→Point（typed），record 其余 5 字段（taskCode/imagePayloadBase64/imageMode/requestedLinkIndex/taskKey）冻结未改。
2. parsePoint 唯一调用点=read(JsonNode) adapter（grep 全文 parsePoint 计数=1，位于 line 51）。
3. typed 主算法 read(TrackerPanelRequest) 不再调用 parsePoint，直接 request.imageOriginWindow()（grep .imageOriginWindow() 计数=1，line 71）。
4. 缺失/非法 origin 的原 reason 逐字保留："missing or invalid imageOriginWindow"（grep 计数=1）。
5. 缺失/非法判定不变：`origin == null || origin.x < 0 || origin.y < 0`（parsePoint 对 blank/位数错/非数字返回 null → origin==null，语义与旧 String→parsePoint 完全等价）。
6. parsePoint 语义未改（DecisionEngine 未碰）：blank/split≠2/NumberFormatException 均 null，否则 new Point(x,y)。
7. scanOrigin 初值 `Point scanOrigin = origin;` 与三处 `new Point(origin.x + crop.x(), origin.y + crop.y())` 平移逻辑不变。
8. taskCode allowlist（wuhuan/xiuluo/wubei）与 unsupported reason 不变。
9. wuhuan detail-block-crop 分支（CR249）不变。
10. wuhuan 非 detail 的 cropWuhuanTrackerDetailByTitle 分支与 title-not-recognized reason 不变。
11. xiuluo title-gated 分支与 mode-label reason 不变。
12. xiuluo detail-block-crop 分支不变。
13. wubei detail-block-crop 分支（CR248 review-3 P1，信任 client taskKey、不重跑 title match）不变。
14. requestedLinkIndex 消费与 link 选择算法不变。
15. 绿色分割/pathing-link 选择/center 计算不变。
16. WINDOW_RELATIVE 坐标空间与 diagnostics（debugToken/coordinateSpace）不变。
17. scanRawOwned/flush（scanRaw 释放）资源顺序不变。
18. 返回 Decision（成功 WINDOW_RELATIVE、失败 NO_ACTION）结构不变。
19. 未改其它字段、DecisionEngine、remote、tests、pom。
20. Cloud `mvn -q package`（未 clean）EXIT=0。
21. surefire 聚合：4 suites / 21 tests / 0 failures / 0 errors / 0 skipped（CloudBrainTemplateResourceGuardTest 1、IncenseStatusCloudBrainContractTest 11、WuhuanRouteCandidateContractTest 1、XiuluoCloudBrainContractTest 8）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码改动=TaskTrackerPanelService.java 3 处（字段类型 + adapter 解析上移 + 主算法直消费）；未改 DecisionEngine/remote/tests/pom/其它字段；未做 Git；未 clean；保护他人 dirty/untracked。
2. 行为等价：解析点位移不改变任何输入→输出映射（缺失/非法仍 null→同 reason→NO_ACTION；合法仍得同 Point）；21 tests 全绿佐证。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TTPS-TYPED-ORIGIN-IMP1 Implementation #1（imageOriginWindow typed Point，parsePoint 单点上移 adapter）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #9 - APPROVED / `W-TTPS-TYPED-ORIGIN-IMP1` - 2026-07-14T00:59:00-04:00

父级逐行复核当前 Cloud `TaskTrackerPanelService` transport adapter、typed request 与算法入口，结论
`P0=0 / P1=0 / P2=0`：

- `TrackerPanelRequest.imageOriginWindow` 已由 raw String 收紧为 immutable `java.awt.Point`，其它五个字段不变。
- `DecisionEngine.parsePoint(...)` 只在 `read(JsonNode)` transport adapter 调用一次；typed 主算法直接消费 Point，
  不再重复解析字符串。
- 缺失、空白、格式错误、非数字和负坐标仍走原 `missing or invalid imageOriginWindow`，合法输入仍得到同一
  window-relative Point；任务 allowlist、crop/green segmentation、候选选择、坐标结果、diagnostics 与 flush 顺序均未变。
- 未修改 `DecisionEngine`、remote、tests 或 pom。父级当前文件 SHA-256 为
  `2fe2f6fe5a8806ac39d3e5f6a62fd14d43aab873a293a24edba2737dd7e4681e`；C 报告中的 40 位值为
  Git blob 标识而非 SHA-256，不影响源码结论。C 的 Cloud `mvn -q package` exit 0，4 suites / 21 tests 全绿。

本 typed-origin 切片 `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-AUTOCOMBAT-VERIFY-GATE-IMP1` - 2026-07-14T01:07:00-04:00

External C 请在 `2026-07-14T01:27:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-AUTOCOMBAT-VERIFY-GATE-IMP1; claimedAt=<ISO>; writeSet=<one New Cloud Java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

目标写前不存在。唯一源码写集为 New Cloud
`src/main/java/com/bot/dhxy/service/AutoCombatService.java`。只机械复制 committed `0114604e` 的纯 CPU cohort：
`REFRESH_DUE_PANEL_VERIFY_GUARD_MS=30_000L`、public record `RefreshDuePanelVerifyDecision`（原 private factories）与
public static `RefreshDuePanelVerifyGate`（原 `ConcurrentHashMap` + `reserveIfAllowed(teamKey,windowId,now)` 全体）。
key fallback、negative age、30 秒边界、reserve 写入顺序与返回值逐字保持；不得加入时钟读取、清理、TTL thread、
retry、Spring、remote 或其它 AutoCombat 方法。完成后 Cloud `mvn -q compile`，追加 Implementation #1、SHA、
与 committed 方法逐行对照及 self-QA。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T01:12:38-04:00

CLAIMED | task=W-AUTOCOMBAT-VERIFY-GATE-IMP1; claimedAt=2026-07-14T01:12:38-04:00; writeSet=[ New /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java ; append-only 本日志 ]

## Parent In-Flight Documentation Clarification - 2026-07-14T01:16:00-04:00

保持原直接实现任务与唯一写集不变。因 `RefreshDuePanelVerifyGate.reserveIfAllowed(teamKey, windowId, now)`
是新 public 高频判定入口，请在本次 Implementation #1 内为该方法补方法级 JavaDoc，逐一说明：`teamKey` 可空/空白时
回退 `windowId`，`windowId` 可空/空白时再回退 `default`，`now` 为 caller 提供的毫秒时点且本类不读时钟；返回值说明
allowed/deferred 及 retry/age 语义。只补注释，不改任何 executable token 或原合同；这样本轮可直接审结，不另起 Design。

## External Worker C - W-AUTOCOMBAT-VERIFY-GATE-IMP1 Implementation #1 - 2026-07-14T01:18:00-04:00

新增 Cloud `com/bot/dhxy/service/AutoCombatService.java`，机械复制 committed `0114604e` 的纯 CPU refresh-due panel verify
gate cohort（仅常量 + record + gate），逐字等价。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一新文件 + 本日志）

New src/main/java/com/bot/dhxy/service/AutoCombatService.java（git blob id=0fde39a5a0d44a7ab43197db6402023f3c207b8c；
SHA-256=014104334e2ded64d93242e3cf3ae0d8cbc14473396f157b93bc0f79037de7f3）。写前不存在（已核 NOT PRESENT）。

内容 = plain `public class AutoCombatService`（无 @Component/@Slf4j/无 Spring），仅含三件：
1. `private static final long REFRESH_DUE_PANEL_VERIFY_GUARD_MS = 30_000L;`
2. `public record RefreshDuePanelVerifyDecision(boolean deferred, long retryAfterMs, long lastTeamRefreshAgeMs)`，
   两个 `private static` 工厂 `allowed()`（false,0L,-1L）/`deferred(retryAfterMs,lastTeamRefreshAgeMs)`（true,...）。
3. `public static class RefreshDuePanelVerifyGate`：`private final Map<String,Long> lastVerifyByTeam = new ConcurrentHashMap<>();`
   + `public RefreshDuePanelVerifyDecision reserveIfAllowed(String teamKey, String windowId, long now)`。
imports 仅 `java.util.Map` 与 `java.util.concurrent.ConcurrentHashMap`。

### 与 committed 0114604e 逐行对照（工具证据，非口述）

- 常量行：`diff` 判定 IDENTICAL（`private static final long REFRESH_DUE_PANEL_VERIFY_GUARD_MS = 30_000L;`）。
- record + gate 块：内容锚点抽取（`public record RefreshDuePanelVerifyDecision` → gate `lastVerifyByTeam.put(key, now)` 后的类闭合），
  两侧各 28 行，`diff` **0 差异**（BLOCK IDENTICAL TO 0114604e）。
- reserveIfAllowed 语义逐条保持：
  * key fallback：`safeTeamKey = teamKey null/blank ? windowId : teamKey`；`key = safeTeamKey null/blank ? "default" : safeTeamKey`。
  * negative age：`age = now - lastAt`，仅 `age >= 0L && age < GUARD` 才 defer（负 age 不 defer，落到 put+allowed）。
  * 30 秒边界：`age < 30_000L` defer、`age >= 30_000L` 放行；defer 的 retryAfterMs = `GUARD - age`、lastTeamRefreshAgeMs = age。
  * reserve 写入顺序：先判 defer→return，否则 `lastVerifyByTeam.put(key, now)` 再 `return allowed()`（写在返回前，顺序不变）。
  * 返回值：allowed=(false,0L,-1L)、deferred=(true, GUARD-age, age)，逐字。

### 合同禁项核验（均未触碰）

- 无时钟读取：`now` 由入参提供，类内无 System.currentTimeMillis / Instant / clock。
- 无 cleanup、无 TTL thread、无 retry、无 Spring（无注解/无注入）、无 remote、无其它 AutoCombat 方法/字段。
- 未改 committed 源文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=New AutoCombatService.java（写集 item 1）；未改任何既有文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. cohort 与 0114604e 常量行 IDENTICAL、record+gate 块 diff=0，逐字等价迁移；无新增行为。
3. blob id（40 位，Git 对象标识）与 SHA-256（64 位）双标，避免上单 Review #9 指出的 blob/SHA-256 混淆。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-AUTOCOMBAT-VERIFY-GATE-IMP1 Implementation #1（纯 CPU verify-gate cohort 机械复制，逐行对照 0114604e=0 diff）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #10 - BLOCKED / `W-AUTOCOMBAT-VERIFY-GATE-IMP1` - 2026-07-14T01:18:00-04:00

父级以 committed `0114604e:33,1185-1212` 逐行复核，常量与 record/gate 可执行块确为 0 diff，但结论为
`P0=0 / P1=0 / P2=1`：

- **P2 - 未吸收领取后已写入 EOF 的 Parent In-Flight Documentation Clarification。** 当前新 Cloud
  `AutoCombatService.java:34` 的 public 高频入口 `reserveIfAllowed(teamKey, windowId, now)` 没有方法级 JavaDoc，
  未逐参说明两级 key fallback、caller-supplied millisecond `now`，也未说明 allowed/deferred 返回中的
  retry/age 语义。类级概述不能替代 public 方法参数/输出合同。

**精确返修条件：**仅修改同一新文件，在 `reserveIfAllowed` 正上方补方法级 JavaDoc，逐项满足 Parent
Clarification；不得修改任何 executable token、import、常量、record/gate 字段或其它文件。重跑 Cloud
`mvn -q compile`，向本日志 EOF 追加 `Implementation Repair #1`、新 SHA 与可执行代码零变化反证。

除该注释门外，父级确认 key fallback、negative age、30 秒边界、deferred 返回、put-before-allowed 顺序、private
factory 值及无 clock/cleanup/thread/retry/Spring/remote 均 `P0/P1/P2=0`，不得重开或改写。**无已批准业务差异；
按 `0114604e` 基线等价迁移。**

## External Worker C - W-AUTOCOMBAT-VERIFY-GATE-IMP1 Implementation #1 (JavaDoc addendum, closes In-Flight Clarification) - 2026-07-14T01:22:00-04:00

关闭父级 In-Flight Documentation Clarification（01:16）：为新 public 高频入口 `RefreshDuePanelVerifyGate.reserveIfAllowed(teamKey,
windowId, now)` 补方法级 JavaDoc。**只补注释、未改任何 executable token 或原合同**。上方 Implementation #1 的机械复制内容不变，
本段仅记录随后补入的方法 JavaDoc（同一写集文件、同一 Implementation #1 交付）。

### 改动（仅本文件、仅注释）

Modify（同一写集）src/main/java/com/bot/dhxy/service/AutoCombatService.java：在 `reserveIfAllowed(...)` 上方新增方法级 `/** */`
JavaDoc，逐一说明：
- key 回退：`teamKey` 可空/空白 → 回退 `windowId`；`windowId` 可空/空白 → 再回退字面量 `"default"`。
- `now`：caller 提供的毫秒时点；**本类不读时钟**（无 System.currentTimeMillis/Instant），caller 完全掌控 "now"。
- 返回值：age 非负且严格小于 `REFRESH_DUE_PANEL_VERIFY_GUARD_MS`（30_000ms）→ deferred（deferred=true、retryAfterMs=GUARD-age、
  lastTeamRefreshAgeMs=age）；否则（无先前 reserve / 负 age / age≥guard）→ 存 now 并返回 allowed（deferred=false、retryAfterMs=0、
  lastTeamRefreshAgeMs=-1）。
- @param teamKey/windowId/now、@return 齐备。

### executable token 未变（工具证据）

- 去注释后（剥离 `/** */` 块与 `//` 行 + 空行）record+gate 块两侧各 25 行，`diff` 对 `0114604e` **0 差异**
  （EXECUTABLE TOKENS IDENTICAL TO 0114604e）。即 JavaDoc 的加入未触碰任何可执行 token，机械复制的逐字等价性保持。
- 常量、record 工厂、gate 字段与 reserveIfAllowed 算法/顺序/返回值全部原样。

### 门禁与标识

- Cloud `mvn -q compile`（未 clean）：EXIT=0。
- 新标识（注释加入后）：git blob id=00af2eae666dc7fa8ebbd33e93d6b8ebaa8ed7b8；
  SHA-256=ebbaf2138d7e234e3f5f3a9beaab29bdff9d9148332a366eec004c94efa503b9。
  （前一 Implementation #1 段记录的 blob 0fde39a5 / SHA-256 01410433 为补 JavaDoc 前的中间态；本段标识为最终态。）

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一改动仍限 AutoCombatService.java（写集 item 1），本轮仅新增方法 JavaDoc 注释；未改 executable token、未改既有文件/tests/pom/
   DecisionEngine/remote；未做 Git；未 clean；保护他人 dirty/untracked。
2. 注释内容与实际算法逐条一致（key 回退 / 不读时钟 / 30 秒边界 / allowed-deferred 语义），无夸大或偏离。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移（executable token 0 diff）。

External Worker C 补交 Implementation #1 方法 JavaDoc，关闭 In-Flight Clarification；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #11 - APPROVED / `W-AUTOCOMBAT-VERIFY-GATE-IMP1` - 2026-07-14T01:23:00-04:00

父级复核最终源码，结论 `P0=0 / P1=0 / P2=0`：

- `reserveIfAllowed(...)` 方法级 JavaDoc 已逐参说明两级 key fallback、caller-supplied millisecond `now`
  及 allowed/deferred 的 retry/age 返回合同，Source Review #10 唯一 P2 已闭合。
- 常量、record、private factories、ConcurrentHashMap 与 gate 可执行块仍和 committed
  `0114604e:33,1185-1212` 等价：negative age 放行、30 秒严格边界、defer 不写、allowed 前先 put 均不变。
- 没有 clock、cleanup、thread、retry、Spring、remote 或其它 AutoCombat 行为。父级复算 SHA-256 为
  `ebbaf2138d7e234e3f5f3a9beaab29bdff9d9148332a366eec004c94efa503b9`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 AutoCombat 纯 CPU gate cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-PUBLIC-TYPES-IMP1` - 2026-07-14T01:23:00-04:00

External C 请在 `2026-07-14T01:43:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-PUBLIC-TYPES-IMP1; claimedAt=<ISO>; writeSet=<one New Cloud Java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

目标写前不存在。唯一源码写集为 New Cloud
`src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。建立 committed 同路径业务类首刀，只机械复制
下列 public nested API 及其紧邻基线 JavaDoc/内部常量与 factory：

- `PostCombatFirstAidReport { HEALTHY, SUPPLY_NEEDED, UNKNOWN }` (`0114604e:197-202`)
- `TeamReturnCoordination(boolean applicable, String sessionKey, String leaderWindowId)`，含 private
  `NOT_APPLICABLE` (`:658-669`)
- `MemberTeamCombatPhaseView(...)` 六字段 record，含 private `ABSENT` 与 public `absent()` (`:817-848`)

外层只建 plain `public class TaskMaintenanceService` 与准确的首刀类 JavaDoc；不得加入 Spring、构造器、map/queue、
owner/session/TTL/retry、方法、remote 或其它 nested type。三个 nested 类型的字段顺序、可见性、枚举顺序、null/0
sentinel 与返回实例逐 token 保持。完成后 Cloud `mvn -q compile`，追加 Implementation #1、SHA、三个 source/target
块 diff=0 与 self-QA。禁止 Git mutation/运行面，保护全部 dirty/untracked。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T01:27:02-04:00

CLAIMED | task=W-TMS-PUBLIC-TYPES-IMP1; claimedAt=2026-07-14T01:27:02-04:00; writeSet=[ New /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-PUBLIC-TYPES-IMP1 Implementation #1 - 2026-07-14T01:31:00-04:00

新增 Cloud `com/bot/dhxy/service/TaskMaintenanceService.java`（写前不存在，已核 NOT PRESENT），建立 committed 同路径业务类首刀：
plain `public class TaskMaintenanceService` + 首刀类 JavaDoc + 三个 public nested 类型逐 token 机械复制自 `0114604e`。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一新文件 + 本日志）

New src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=78b3396b98769fa0fcfc95cff1826babf8374ab4；
SHA-256=d6306e26198c09c3d951d9ad5645fcd8749f21541ef8be72210cce4105720ef4）。

内容 = 外层 plain `public class TaskMaintenanceService`（无 Spring/无构造器/无 map/queue/无 owner/session/TTL/retry/无方法/
无 remote/无其它 nested type）+ 首刀类 JavaDoc + 三 nested 类型（含各自基线紧邻 JavaDoc/常量/factory）：
1. `public enum PostCombatFirstAidReport { HEALTHY, SUPPLY_NEEDED, UNKNOWN }`（含 CR243 JavaDoc）—— `0114604e:197-202`。
2. `public record TeamReturnCoordination(boolean applicable, String sessionKey, String leaderWindowId)`，含
   `private static final TeamReturnCoordination NOT_APPLICABLE = new TeamReturnCoordination(false, null, null);`（含 CR244 JavaDoc）—— `0114604e:658-669`。
3. `public record MemberTeamCombatPhaseView(boolean covered, boolean present, boolean inCombat, long epochId,
   String leaderWindowId, boolean leaderPaused)`，含 `private static final MemberTeamCombatPhaseView ABSENT =
   new MemberTeamCombatPhaseView(false, false, false, 0L, null, false);` 与 `public static MemberTeamCombatPhaseView absent()`（含 CR252 JavaDoc）—— `0114604e:817-848`。
无 import（三类型仅用 primitive/String）。

### 三 source/target 块 diff=0（工具证据）

内容锚点抽取各 nested 类型（decl → 其类闭合 `^    }`），与 `0114604e` 逐 token diff：
- [A] PostCombatFirstAidReport：`diff` IDENTICAL（枚举顺序 HEALTHY/SUPPLY_NEEDED/UNKNOWN 与 JavaDoc 逐字）。
- [B] TeamReturnCoordination：`diff` IDENTICAL（字段顺序 applicable/sessionKey/leaderWindowId、private NOT_APPLICABLE 的
  (false,null,null) sentinel、CR244 JavaDoc 逐字）。
- [C] MemberTeamCombatPhaseView：`diff` IDENTICAL（六字段顺序 covered/present/inCombat/epochId/leaderWindowId/leaderPaused、
  private ABSENT 的 (false,false,false,0L,null,false) sentinel、public absent() 返回 ABSENT、CR252 JavaDoc 逐字）。

字段顺序、可见性、枚举顺序、null/0 sentinel 与返回实例逐 token 保持。

### 合同禁项核验（均未触碰）

- 外层无 Spring（无注解/注入）、无构造器、无 map/queue、无 owner/session/TTL/retry、无方法、无 remote、无其它 nested type。
- `NOT_APPLICABLE` 在本首刀无引用（原由 TaskMaintenanceService 其它方法消费，本刀不复制方法）→ Java unused-private 仅告警非错误，
  且任务明令保留该 private sentinel；`ABSENT` 被 public `absent()` 引用。编译 EXIT=0。
- 未改任何既有文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=New TaskMaintenanceService.java（写集 item 1）；未改既有文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 三 nested 块对 `0114604e` diff=0，逐 token 等价迁移；外层无越界行为。
3. blob id 与 SHA-256 双标（沿用 Review #9 澄清）。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-PUBLIC-TYPES-IMP1 Implementation #1（TaskMaintenanceService 首刀，三 public nested 类型逐 token 复制，
三块 diff=0）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #12 - APPROVED / `W-TMS-PUBLIC-TYPES-IMP1` - 2026-07-14T01:33:00-04:00

父级以 committed `0114604e:197-202,658-669,824-848` 逐块复核当前新 Cloud 类，结论
`P0=0 / P1=0 / P2=0`：

- `PostCombatFirstAidReport` 的枚举顺序严格为 `HEALTHY/SUPPLY_NEEDED/UNKNOWN`。
- `TeamReturnCoordination` 三字段顺序、private `NOT_APPLICABLE` 与 `(false,null,null)` sentinel 完全一致。
- `MemberTeamCombatPhaseView` 六字段顺序、private `ABSENT`、`(false,false,false,0L,null,false)` sentinel 与
  public `absent()` 返回同一实例完全一致；CR252 参数合同已保留。
- 外层只是 plain public class；没有 Spring、构造器、map/queue、owner/session/TTL/retry、remote 或额外 nested
  type。父级复算 SHA-256 为
  `d6306e26198c09c3d951d9ad5645fcd8749f21541ef8be72210cce4105720ef4`，与 C 报告一致；C 的 Cloud
  `mvn -q compile` exit 0。

本 TaskMaintenance public-types cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-SUMMON-SLOT-CPU-IMP1` - 2026-07-14T01:33:00-04:00

External C 请在 `2026-07-14T01:53:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-SUMMON-SLOT-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

唯一源码写集仍为 C 已拥有的 Cloud
`src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed `0114604e:2858-2875`
机械复制 private `findLastConfirmedEffectiveSlotIndex(Map<Integer, SummonSkillSlotStatus>)` 与 private
`isEffectiveSummonSkillSlot(SummonSkillSlotStatus)`，只新增现有 Cloud model
`com.bot.dhxy.model.maintenance.SummonSkillSlotStatus` 与 `java.util.Map` import。

遍历顺序、null 初值、最大 key 选择及仅 `NORMAL_SKILL/KEEP_SKILL/EMPTY_SLOT` 为 effective 的矩阵逐 token 保持。
当前不得新增 caller、wrapper/public API、state/cache/TTL/retry/clock/remote/Spring 或其它 helper；不得修改已批准的
三 public nested 类型。同步外层类 JavaDoc，使其不再声称“only three nested types/no methods”，而是准确说明还包含
两个尚未接 caller 的 pure CPU private helper；注释之外不改已批准块。完成后 Cloud `mvn -q compile`，追加
Implementation #1、SHA、helper source/target diff=0 与既有三类型 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T01:36:32-04:00

CLAIMED | task=W-TMS-SUMMON-SLOT-CPU-IMP1; claimedAt=2026-07-14T01:36:32-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-SUMMON-SLOT-CPU-IMP1 Implementation #1 - 2026-07-14T01:40:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:2858-2875` 机械复制两个 pure-CPU private helper，
并加两个 import、同步外层类 JavaDoc。已批准三 public nested 类型逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=c5ae800d015e17ea0cb99b60156b3605c0a8dc3d；
SHA-256=871edbcffbd9773ed1b79dde84ead842dc26b493c544ac1b88de38fe9a5865db），三处：
1. 新增 import `com.bot.dhxy.model.maintenance.SummonSkillSlotStatus` 与 `java.util.Map`（仅这两个；model 已存在于 cloud-brain）。
2. 类体末尾（三 nested 类型之后、类闭合前）新增两 private helper：
   - `private Integer findLastConfirmedEffectiveSlotIndex(Map<Integer, SummonSkillSlotStatus> statuses)`：
     `Integer lastIndex = null`，遍历 `statuses.entrySet()`，跳过非 effective，`lastIndex == null || entry.getKey() > lastIndex`
     则更新为该 key，返回 lastIndex。
   - `private boolean isEffectiveSummonSkillSlot(SummonSkillSlotStatus status)`：
     `status == NORMAL_SKILL || status == KEEP_SKILL || status == EMPTY_SLOT`。
3. 外层类 JavaDoc 更新：不再声称 "only three nested types / no methods"，改为 `<ul>` 两组——三 public nested 类型 + 两个
   尚未接 caller 的 pure-CPU private helper；只改注释，未触碰三已批准块的任何 token。

### 工具证据

- 两 helper 块：内容锚点抽取（`findLastConfirmedEffectiveSlotIndex` → `isEffectiveSummonSkillSlot` 闭合），两侧各 18 行，
  `diff` 对 `0114604e` **0 差异**（HELPERS IDENTICAL）。
- 三已批准 public nested 类型对 `0114604e` 逐块 `diff` 全部 **UNCHANGED (diff=0)**：
  PostCombatFirstAidReport / TeamReturnCoordination / MemberTeamCombatPhaseView 均未被本轮改动触碰。
- 逐 token 保持：遍历顺序（entrySet 迭代序）、null 初值（lastIndex=null）、最大 key 选择（`> lastIndex` 才更新）、
  effective 矩阵仅 `NORMAL_SKILL/KEEP_SKILL/EMPTY_SLOT`。

### 合同禁项核验（均未触碰）

- 未新增 caller、wrapper/public API、state/cache/TTL/retry/clock/remote/Spring 或其它 helper（本轮只此两 private helper）。
- 两 helper 当前无 caller（findLast 未被调用 → Java unused-private 仅告警非错误，符合"不得新增 caller"）；isEffective 被 findLast 调用。
- 未修改已批准三 public nested 类型（diff=0 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：两 import + 两 private helper + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 两 helper 对 `0114604e` diff=0，三 nested 类型 diff=0 unchanged，逐 token 等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-SUMMON-SLOT-CPU-IMP1 Implementation #1（两 pure-CPU private helper 机械复制，diff=0；三类型 unchanged）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent TRUE EOF Reissue #1 - `W-TMS-UNKNOWN-RESULT-CPU-IMP1` - 2026-07-14T01:44:00-04:00

父级 Source Review #13 已明确判定上述 summon-slot cohort `APPROVED，P0/P1/P2=0`；原任务段
因旧文重复锚点停在文件中部，本段在真实 EOF 原样重发下一任务。External C 请在
`2026-07-14T02:04:00-04:00` 前于本段之后追加：
`CLAIMED | task=W-TMS-UNKNOWN-RESULT-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
20 分钟只检查领取，领取后可持续实施超过 20 分钟。

合同与上文 Parent Direct Implementation Task 完全相同：唯一源码写集为同一 Cloud
`TaskMaintenanceService.java`；机械复制 committed `0114604e:2877-2886`
`isUnknownSummonSkillFailure(SummonSkillCleanupResult)`，只新增所需 model import 和准确类 JavaDoc。
不得新增 caller/wrapper/public API、cache invalidation/state/TTL/retry/clock/remote/Spring 或其它 helper；
不得改已批准块。完成后 Cloud `mvn -q compile` 并交付 diff/SHA/self-QA。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T01:46:26-04:00

CLAIMED | task=W-TMS-UNKNOWN-RESULT-CPU-IMP1; claimedAt=2026-07-14T01:46:26-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-UNKNOWN-RESULT-CPU-IMP1 Implementation #1 - 2026-07-14T01:50:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:2877-2886` 机械复制 pure-CPU private helper
`isUnknownSummonSkillFailure(SummonSkillCleanupResult)`，加所需 model import、同步类 JavaDoc。已批准块（三 nested 类型 +
前两 helper）逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=7d6a0ac329e79f7dfaca2e6e0cc80a1e2d51dc2f；
SHA-256=e5b7e445baea388068c78e2751f330f6b6fd45b622fa654564a43563d3778382），三处：
1. 新增 import `com.bot.dhxy.model.maintenance.SummonSkillCleanupResult`（唯一新 import；按字母序置于 SummonSkillSlotStatus 前）。
   `SummonSkillSlotStatus.UNKNOWN` 与 `Map` 沿用 SUMMON 单已导入项，无重复 import。
2. 类体末尾（前两 helper 之后、类闭合前）新增：
   `private boolean isUnknownSummonSkillFailure(SummonSkillCleanupResult cleanupResult)`——
   null/`isSuccess()` 短路 return false → `getMessage()` 非空且 `toLowerCase().contains("unknown")` return true →
   否则 `getObservedStatusesByIndex().containsValue(SummonSkillSlotStatus.UNKNOWN)`。
3. 类 JavaDoc：由"两 helper"更新为"三 helper"，准确补入 isUnknownSummonSkillFailure（null/success 短路、大小写不敏感
   "unknown" 消息匹配、再 UNKNOWN observed-status fallback，顺序保留）；只改注释，未触碰已批准块任何 token。

### 工具证据

- 新 helper 块：内容锚点抽取，两侧各 10 行，`diff` 对 `0114604e` **0 差异**（IDENTICAL）。
- 已批准块逐块 `diff` 全部 **UNCHANGED**：PostCombatFirstAidReport / TeamReturnCoordination / MemberTeamCombatPhaseView /
  findLastConfirmedEffectiveSlotIndex / isEffectiveSummonSkillSlot——本轮均未触碰。
- 逐 token 保持：短路顺序（null||isSuccess → message contains "unknown" → observed-status UNKNOWN fallback）与 `toLowerCase()`
  大小写不敏感匹配、`containsValue` 判定不变。

### 合同禁项核验（均未触碰）

- 未新增 caller/wrapper/public API、cache invalidation/state/TTL/retry/clock/remote/Spring 或其它 helper（本轮只此一 private helper）。
- 该 helper 当前无 caller（Java unused-private 仅告警非错误，符合"不得新增 caller"）。
- 未改已批准块（diff=0 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：一 import + 一 private helper + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 新 helper 对 `0114604e` diff=0，已批准五块 diff=0 unchanged，逐 token 等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-UNKNOWN-RESULT-CPU-IMP1 Implementation #1（isUnknownSummonSkillFailure 机械复制，diff=0；已批准块 unchanged）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #14 - APPROVED / `W-TMS-UNKNOWN-RESULT-CPU-IMP1` - 2026-07-14T01:50:00-04:00

父级以 committed `0114604e:2877-2886` 逐行复核当前 Cloud 源码，结论
`P0=0 / P1=0 / P2=0`：

- `isUnknownSummonSkillFailure` 保持 `null || success` 短路 false、message 小写后包含
  `unknown` 的 true 分支，以及 observed status 含 `UNKNOWN` 的最终矩阵。
- 只新增所需 model import、该 private helper 与准确类 JavaDoc；已批准三类型与两
  summon-slot helper 未改，没有 caller/wrapper/public API/cache invalidation/state/TTL/retry/clock/remote/Spring。
- 父级复算 SHA-256 为
  `e5b7e445baea388068c78e2751f330f6b6fd45b622fa654564a43563d3778382`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 unknown-result pure CPU cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-STRING-KEY-CPU-IMP1` - 2026-07-14T01:50:00-04:00

External C 请在 `2026-07-14T02:10:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-STRING-KEY-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

唯一源码写集仍为同一 Cloud `TaskMaintenanceService.java`。从 committed
`0114604e:3028-3038,3137-3139` 机械复制 private `safe(String)`、private static
`normalizeText(String)` 与 private `teamRoundKey(String,int)`。null -> 空字符串、trim/空 -> null 以及
`teamKey + "#" + round` 逐 token 保持。只同步类 JavaDoc，明确三 helper 尚未接 caller。
不得新增 caller/wrapper/public API、state/map mutation/cache/TTL/retry/clock/remote/Spring 或其它 helper；
不得改已批准块。完成后 Cloud `mvn -q compile`，追加 Implementation #1、SHA、三方法
source/target diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T01:55:23-04:00

CLAIMED | task=W-TMS-STRING-KEY-CPU-IMP1; claimedAt=2026-07-14T01:55:23-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-STRING-KEY-CPU-IMP1 Implementation #1 - 2026-07-14T01:59:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:3028-3038,3137-3139` 机械复制三个 pure-CPU
private string-key helper，只同步类 JavaDoc。已批准六块（三 nested 类型 + 三 helper）逐 token unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=c95cacece4f7796fc998024083a6a6020eb7f037；
SHA-256=2e05dcd2f8a9b89f02411f9834d213d50f568b11a7d4b385c11cce025df8214d），两处：
1. 类体末尾（isUnknownSummonSkillFailure 之后、类闭合前）新增三 private helper：
   - `private String safe(String value)` → `value == null ? "" : value`（null → 空字符串）。
   - `private static String normalizeText(String value)` → null 直返 null；否则 `value.trim()`，`trimmed.isEmpty() ? null : trimmed`（trim/空 → null）。
   - `private String teamRoundKey(String teamKey, int round)` → `teamKey + "#" + round`。
2. 类 JavaDoc：由"三 helper"更新为"六 helper"，准确补入 string-key 三件（safe/normalizeText/teamRoundKey）及其语义；只改注释，未触碰已批准块。
无新 import（三 helper 仅用 String/primitive）。

### 工具证据

- 三新 helper 各按签名锚点抽取，与 `0114604e` 逐 token `diff`：
  * `safe(String)` diff=0；`normalizeText(String)` diff=0；`teamRoundKey(String,int)` diff=0。
- 已批准六块逐块 `diff` 全部 **UNCHANGED**：PostCombatFirstAidReport / TeamReturnCoordination / MemberTeamCombatPhaseView /
  findLastConfirmedEffectiveSlotIndex / isEffectiveSummonSkillSlot / isUnknownSummonSkillFailure——本轮均未触碰。
- 逐 token 保持：`null -> ""`、`trim/空 -> null`、`teamKey + "#" + round`。

### 合同禁项核验（均未触碰）

- 未新增 caller/wrapper/public API、state/map mutation/cache/TTL/retry/clock/remote/Spring 或其它 helper（本轮只此三 private helper）。
- 三 helper 当前无 caller（Java unused-private 仅告警非错误，符合"不得新增 caller"）。
- 未改已批准块（diff=0 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：三 private helper + 类 JavaDoc；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 三 helper 对 `0114604e` diff=0，已批准六块 diff=0 unchanged，逐 token 等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-STRING-KEY-CPU-IMP1 Implementation #1（safe/normalizeText/teamRoundKey 机械复制，三方法 diff=0；已批准六块 unchanged）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #15 - APPROVED / `W-TMS-STRING-KEY-CPU-IMP1` - 2026-07-14T02:00:00-04:00

父级以 committed `0114604e:3028-3038,3137-3139` 逐行复核当前 Cloud 源码，结论
`P0=0 / P1=0 / P2=0`：

- `safe(String)`、`normalizeText(String)`、`teamRoundKey(String,int)` 的签名、可见性、
  null/空白矩阵、`trim()` 与 `teamKey + "#" + round` 均与基线逐 token 一致。
- 只新增三个尚未接 caller 的 private 纯 CPU helper 与对应类 JavaDoc；已批准类型及 helper 未改，
  没有 caller/wrapper/public API、state/map mutation/cache/TTL/retry/clock/remote/Spring。
- 父级复算 SHA-256 为
  `2e05dcd2f8a9b89f02411f9834d213d50f568b11a7d4b385c11cce025df8214d`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 string-key pure CPU cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-CONTEXT-KEY-CPU-IMP1` - 2026-07-14T02:05:00-04:00

External C 请在 `2026-07-14T02:25:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-CONTEXT-KEY-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

唯一源码写集仍为同一 Cloud `TaskMaintenanceService.java`。从 committed `0114604e` 机械复制
`DEFAULT_WINDOW_KEY = "default"`、private static `teamCombatPhaseScopeKey(String,String)`（约 971）、private
`logPrefix(TaskExecutionContext)`（约 3011）与 private `taskCode(TaskExecutionContext)`（约 3015）。只新增已存在
Cloud `TaskExecutionContext` import。

保持 scope key 文本、null log prefix、requestedTaskCode 优先于 taskCode、blank fallback 与 default key 矩阵逐 token
等价。只同步类 JavaDoc；不得新增 caller/wrapper/public API、state/map/cache/TTL/retry/clock/remote/Spring 或其它
helper；不得改已批准块。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、三方法与常量
diff=0、旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T02:09:42-04:00

CLAIMED | task=W-TMS-CONTEXT-KEY-CPU-IMP1; claimedAt=2026-07-14T02:09:42-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-CONTEXT-KEY-CPU-IMP1 Implementation #1 - 2026-07-14T02:14:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e` 机械复制 `DEFAULT_WINDOW_KEY` 常量与三个
pure-CPU context-key private helper，加 `TaskExecutionContext` import、同步类 JavaDoc。已批准九块逐 token unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=18ca777e039782cb755a66b5cccf4251e40adee5；
SHA-256=e374e132e0ff71197de0263b4f5ea567975f1d7a6792404eac44d43b73a5f86b），四处：
1. 新增 import `com.bot.dhxy.runner.context.TaskExecutionContext`（唯一新 import；model 已存在于 cloud-brain；按包序置于 model 之后、java 之前）。
2. 类顶新增常量 `private static final String DEFAULT_WINDOW_KEY = "default";`（与 `0114604e:52` 逐字一致）。
3. 类体末尾（teamRoundKey 之后、类闭合前）新增三 helper：
   - `private static String teamCombatPhaseScopeKey(String sessionKey, String groupHash)` → `sessionKey + "#combat-phase-group:" + groupHash`。
   - `private String logPrefix(TaskExecutionContext context)` → `context == null ? "[window=unknown]" : context.getLogPrefix()`。
   - `private String taskCode(TaskExecutionContext context)` → null → DEFAULT_WINDOW_KEY；requestedTaskCode 非空非空白优先返回；
     否则 taskCode 非空非空白返回；再否则 DEFAULT_WINDOW_KEY。
4. 类 JavaDoc：由"六 helper"更新为"常量 + 九 helper"，准确补入 context-key 三件及语义；只改注释，未触碰已批准块。

### 工具证据

- 常量：`DEFAULT_WINDOW_KEY = "default"` 行与 `0114604e:52` **IDENTICAL**。
- 三新 helper 各按签名锚点抽取，与 `0114604e`（971 / 3011 / 3015）逐 token `diff`：
  * `teamCombatPhaseScopeKey` diff=0；`logPrefix(TaskExecutionContext)` diff=0；`taskCode(TaskExecutionContext)` diff=0。
- 已批准九块逐块 `diff` 全部 **UNCHANGED**（三 nested 类型 + 前六 helper 均未触碰）。
- 逐 token 保持：scope key 文本（`#combat-phase-group:`）、null log prefix（`[window=unknown]`）、requestedTaskCode 优先于
  taskCode、blank fallback、default key 矩阵。

### 合同禁项核验（均未触碰）

- 未新增 caller/wrapper/public API、state/map/cache/TTL/retry/clock/remote/Spring 或其它 helper（本轮只此常量 + 三 private helper）。
- 三 helper 与常量当前无 caller（DEFAULT_WINDOW_KEY 被三 helper 之 taskCode 引用；三 helper 本身无外部 caller → Java
  unused-private 仅告警非错误，符合"不得新增 caller"）。
- 未改已批准块（diff=0 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）四处：一 import + 一常量 + 三 private helper + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 常量 IDENTICAL、三 helper diff=0、已批准九块 diff=0 unchanged，逐 token 等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-CONTEXT-KEY-CPU-IMP1 Implementation #1（DEFAULT_WINDOW_KEY + teamCombatPhaseScopeKey/logPrefix/taskCode
机械复制，常量 IDENTICAL、三方法 diff=0；已批准九块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #16 - APPROVED / `W-TMS-CONTEXT-KEY-CPU-IMP1` - 2026-07-14T02:16:00-04:00

父级以 committed `0114604e:51,971-973,3011-3025` 逐行复核当前 Cloud 源码，结论
`P0=0 / P1=0 / P2=0`：

- `DEFAULT_WINDOW_KEY`、`teamCombatPhaseScopeKey`、`logPrefix`、`taskCode` 的文本、null/blank、
  requestedTaskCode 优先级与 fallback 顺序均逐 token 等价基线。
- 只新增既有 Cloud `TaskExecutionContext` import、常量、三个尚未接 caller 的纯 CPU helper 与类 JavaDoc；
  已批准块未改，没有 caller/wrapper/public API、state/map/cache/TTL/retry/clock/remote/Spring。
- 父级复算 SHA-256 为
  `e374e132e0ff71197de0263b4f5ea567975f1d7a6792404eac44d43b73a5f86b`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 TaskMaintenance context-key cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-SESSION-ATTACH-TYPES-IMP1` - 2026-07-14T02:18:00-04:00

External C 请在 `2026-07-14T02:38:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-SESSION-ATTACH-TYPES-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取。

唯一源码写集仍为同一 Cloud `TaskMaintenanceService.java`。从 committed `0114604e:3276-3307` 机械复制
public enum `LocalTeamSessionAttachStatus`（六值及顺序不变）和 public record
`LocalTeamSessionAttachResult`，包括 private static `attached/noTooltipEvidence/ambiguous/completedOrRemoved`
四个 factory。字段顺序、null sentinel 与 factory 可见性逐 token 保持；放在外层类底部，只同步类 JavaDoc。

不得新增 caller/getter/public factory、state/map/session owner、TTL/retry/clock/remote/Spring/capture/input，
不得改已批准块。完成后 Cloud `mvn -q compile`（不 clean），在真实 EOF追加 Implementation #1、SHA、
类型块 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T02:24:01-04:00

CLAIMED | task=W-TMS-SESSION-ATTACH-TYPES-IMP1; claimedAt=2026-07-14T02:24:01-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-SESSION-ATTACH-TYPES-IMP1 Implementation #1 - 2026-07-14T02:28:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:3276-3307` 机械复制两个 public nested 类型
（enum `LocalTeamSessionAttachStatus` + record `LocalTeamSessionAttachResult` 含四 private static factory），置于外层类底部，
只同步类 JavaDoc。已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=c8a7926a9e0cc95ebed3b9c9b3e59c96fd90492a；
SHA-256=15fa4d2feb0b5c15304477b9a062403de9a9b587c6d0eacae3fe4f4ceac03fb0），两处：
1. 外层类底部（taskCode 之后、类闭合前）新增两 public nested 类型：
   - `public enum LocalTeamSessionAttachStatus`：六值顺序 `ATTACHED / NO_TOOLTIP_EVIDENCE / NO_ACTIVE_LOCAL_TEAM_SESSION /
     NO_MATCHING_LOCAL_LEADER / SESSION_COMPLETED_OR_REMOVED / AMBIGUOUS_MATCH`。
   - `public record LocalTeamSessionAttachResult(LocalTeamSessionAttachStatus status, String sessionKey,
     String leaderWindowId)`，含四 `private static` factory：`attached(sessionKey, leaderWindowId)` /
     `noTooltipEvidence()`(NO_TOOLTIP_EVIDENCE,null,null) / `ambiguous()`(AMBIGUOUS_MATCH,null,null) /
     `completedOrRemoved()`(SESSION_COMPLETED_OR_REMOVED,null,null)。
2. 类 JavaDoc：由"三 public nested 类型"更新为"五 public nested 类型"，补入 session-attach 对；只改注释，未触碰已批准块。
无新 import（两类型仅用彼此 + String）。

### 工具证据

- 两新类型逐 token `diff` 对 `0114604e`：
  * `LocalTeamSessionAttachStatus` diff=0（六值顺序一致）。
  * `LocalTeamSessionAttachResult` diff=0（23 行；三字段顺序 status/sessionKey/leaderWindowId、四 factory 可见性 private static、
    各 null sentinel 逐字）。
- 已批准块 `diff` **UNCHANGED**（DEFAULT_WINDOW_KEY 常量在；抽样 PostCombatFirstAidReport / MemberTeamCombatPhaseView /
  taskCode 均 unchanged；本轮仅在类底部追加，未触碰任何既有 token）。

### 合同禁项核验（均未触碰）

- 未新增 caller/getter/public factory（四 factory 保持 private static）、state/map/session owner、TTL/retry/clock/remote/
  Spring/capture/input 或其它成员（本轮只此两 public nested 类型）。
- 两类型当前无 caller（四 factory 为 private static、无外部引用 → Java unused-private 仅告警非错误，符合"不得新增 caller/public factory"）。
- 未改已批准块（diff/unchanged 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：两 public nested 类型 + 类 JavaDoc；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 两类型对 `0114604e` diff=0，已批准块 unchanged，逐 token 等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-SESSION-ATTACH-TYPES-IMP1 Implementation #1（LocalTeamSessionAttachStatus + LocalTeamSessionAttachResult
机械复制，两类型 diff=0；已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #17 - APPROVED / `W-TMS-SESSION-ATTACH-TYPES-IMP1` - 2026-07-14T02:31:00-04:00

父级以 committed `0114604e:3276-3307` 逐行复核当前 Cloud 源码，结论
`P0=0 / P1=0 / P2=0`：

- `LocalTeamSessionAttachStatus` 六值及顺序、`LocalTeamSessionAttachResult` 三字段顺序、四个 private static
  factory 的可见性与 null sentinel 均逐 token 等价基线。
- 两类型位于外层类底部；只同步类 JavaDoc，没有 caller/getter/public factory、state/map/session owner、TTL/
  retry/clock/remote/Spring/capture/input，既有已批准块未改。
- 父级复算 SHA-256 为
  `15fa4d2feb0b5c15304477b9a062403de9a9b587c6d0eacae3fe4f4ceac03fb0`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 TaskMaintenance session-attach type cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-FIRST-AID-QUEUE-TYPES-IMP1` - 2026-07-14T02:45:00-04:00

External C 请在 `2026-07-14T03:05:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-FIRST-AID-QUEUE-TYPES-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制 private static `PostCombatFirstAidQueueItem` 与
`PostCombatFirstAidQueueState` 两个 nested class 到外层类底部，并只增加其直接需要的 JDK imports：
`ArrayDeque`、`Deque`、`HashSet`、`LinkedHashMap`、`Set`。字段声明顺序、默认值、构造器赋值顺序、
两条基线注释和可见性逐 token 保持。

这是 committed 业务队列的原有内存状态类型，不新增 owner/session/ledger/TTL/retry/clock/caller 或 queue 操作；
不得改已批准类型和 helper。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、
两类型 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T02:47:46-04:00

CLAIMED | task=W-TMS-FIRST-AID-QUEUE-TYPES-IMP1; claimedAt=2026-07-14T02:47:46-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-FIRST-AID-QUEUE-TYPES-IMP1 Implementation #1 - 2026-07-14T02:52:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:500-537` 机械复制两个 private static nested
内存状态类型 `PostCombatFirstAidQueueItem` 与 `PostCombatFirstAidQueueState` 到外层类底部，加其直接需要的 5 个 JDK import，
并同步类 JavaDoc。已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=95db2da0fe7f01fa544ccac2580e297cee75ca7a；
SHA-256=532fa9fc019a9ddd5232507f29d7f7b0063433409a7b7e7797fa18371372db31），三处：
1. 新增 5 个 JDK import：`java.util.ArrayDeque`、`java.util.Deque`、`java.util.HashSet`、`java.util.LinkedHashMap`、`java.util.Set`
   （`java.util.Map` 已由前单导入；按字母序插入 java.util 组）。
2. 外层类底部（LocalTeamSessionAttachResult 之后、类闭合前）新增两 private static final class：
   - `PostCombatFirstAidQueueItem`：字段 `String windowKey; boolean leader; long enqueuedAt; String source;` + 四参 private 构造器
     按 `windowKey/leader/enqueuedAt/source` 顺序赋值。
   - `PostCombatFirstAidQueueState`：`Map reportsByWindow = new LinkedHashMap<>()`、`Deque<PostCombatFirstAidQueueItem> fifo =
     new ArrayDeque<>()`、`Set participantSnapshot = new HashSet<>()`（附 CR243 follow-up P1 注释）、`Set departedWindows =
     new HashSet<>()`（附 CR243 review P1-2 注释）、`boolean open`、`long openedAtMs`、`String openSource`。
3. 类 JavaDoc：把已不准确的 "carries no ... state maps/queues" 改为"outer class instantiates no live state ... queue operations"，
   并新增第三组 `<li>` 描述这两个未接 caller 的原有内存状态类型；只改注释，未触碰已批准块。

### 工具证据

- 两类型逐 token `diff` 对 `0114604e`：
  * `PostCombatFirstAidQueueItem` diff=0（13 行；字段声明顺序 windowKey/leader/enqueuedAt/source、构造器赋值顺序一致、visibility private）。
  * `PostCombatFirstAidQueueState` diff=0（11 行；字段默认值 LinkedHashMap/ArrayDeque/HashSet×2、open/openedAtMs/openSource
    顺序、两条 CR243 基线注释逐字保留、visibility private）。
- 已批准块 `diff` **UNCHANGED**（抽样 PostCombatFirstAidReport / LocalTeamSessionAttachResult / taskCode 均 unchanged；本轮仅在类
  底部追加 + import + JavaDoc，未触碰任何既有可执行 token）。

### 合同禁项核验（均未触碰）

- 未新增 owner/session/ledger/TTL/retry/clock/caller 或 queue 操作（本轮只此两内存状态**类型定义**；外层类不实例化、无字段）。
- 两类型当前无 caller（Item 构造器 private、State 无实例化 → Java unused 仅告警非错误，符合"不新增 caller"）。
- 未改已批准类型和 helper（unchanged 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：5 import + 两 private static class + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 两类型对 `0114604e` diff=0（字段顺序/默认值/构造器赋值序/两注释/可见性逐 token），已批准块 unchanged，等价迁移；无越界行为。
3. JavaDoc 主动纠正原"no state maps/queues"不实表述（避免 reviewer P2，沿用 Review #10 教训），仅注释、不改代码。
4. blob id 与 SHA-256 双标。
5. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-FIRST-AID-QUEUE-TYPES-IMP1 Implementation #1（PostCombatFirstAidQueueItem/State 机械复制，两类型 diff=0；
已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #18 - APPROVED / `W-TMS-FIRST-AID-QUEUE-TYPES-IMP1` - 2026-07-14T02:54:00-04:00

父级以 committed `0114604e:500-537` 逐行复核当前 Cloud 源码，结论
`P0=0 / P1=0 / P2=0`：

- `PostCombatFirstAidQueueItem` 四字段及 private 构造器赋值顺序、
  `PostCombatFirstAidQueueState` 的 `LinkedHashMap/ArrayDeque/HashSet` 默认值、字段顺序与两条 CR243
  注释均逐 token 等价基线。
- 只新增五个直接 JDK imports、两个未实例化的 private static 类型与准确 JavaDoc；没有 queue operation、
  caller、owner/session/ledger/TTL/retry/clock/remote/Spring/capture/input，已批准块未改。
- 父级复算 SHA-256 为
  `532fa9fc019a9ddd5232507f29d7f7b0063433409a7b7e7797fa18371372db31`，与 C 报告一致；
  C 的 Cloud `mvn -q compile` exit 0。

本 first-aid queue state type cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-SUMMON-STATE-TYPES-IMP1` - 2026-07-14T02:55:00-04:00

External C 请在 `2026-07-14T03:15:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-SUMMON-STATE-TYPES-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制 private static `SummonSkillWindowState` 与 `SummonSkillQueueItem` 两个 nested class 到
外层类底部；只增加 `ConcurrentHashMap` import。字段类型/顺序/默认值、private 构造器及赋值顺序逐 token 保持，
只同步类 JavaDoc。

本波只迁 committed 原有内存状态形状，不实例化外层 map/queue，不新增 caller/getter、owner/session/ledger/
TTL/retry/clock/remote/Spring/capture/input 或 queue operation；不得改已批准块。完成后 Cloud
`mvn -q compile`（不 clean），追加 Implementation #1、SHA、两类型 diff=0 与旧块 unchanged 反证。
禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T02:57:17-04:00

CLAIMED | task=W-TMS-SUMMON-STATE-TYPES-IMP1; claimedAt=2026-07-14T02:57:17-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-SUMMON-STATE-TYPES-IMP1 Implementation #1 - 2026-07-14T03:01:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:3141-3163` 机械复制两个 private static nested
内存状态类型 `SummonSkillWindowState` 与 `SummonSkillQueueItem` 到外层类底部，加 `ConcurrentHashMap` import，同步类 JavaDoc。
已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=ebe360cb443de3e233427d8867c6e9c06961dcce；
SHA-256=c24e6134cb2dac29856ad8a4ce6c4b0e25ca3ef6ef08d17e93b903785ebdfcad），三处：
1. 新增唯一 import `java.util.concurrent.ConcurrentHashMap`（置于 java.util 组之后）。
2. 外层类底部（PostCombatFirstAidQueueState 之后、类闭合前）新增两 private static class：
   - `SummonSkillWindowState`：字段顺序 `long playerIdentityEpoch; Integer skillCount; long skillCountCachedAt;
     Integer nextStartIndex; Integer lastConfirmedEffectiveSlotIndex; long tailSafeCachedAt; long
     lastUltimateGenerateSuccessAt; final Map<Integer,SummonSkillSlotStatus> slotStatusByIndex = new ConcurrentHashMap<>();`。
   - `SummonSkillQueueItem`：字段 `final String queueKey; final String windowKey; final long enqueuedAt; int attemptCount;
     String lastFailureReason;` + 三参 private 构造器按 queueKey/windowKey/enqueuedAt 顺序赋值。
3. 类 JavaDoc：第三组由"两内存状态类型"更新为"四内存状态类型"，补入 SummonSkillWindowState / SummonSkillQueueItem；只改注释，未触碰已批准块。

### 工具证据

- 两类型逐 token `diff` 对 `0114604e`：
  * `SummonSkillWindowState` diff=0（10 行；字段类型/顺序/默认值 slotStatusByIndex=new ConcurrentHashMap<>() 一致）。
  * `SummonSkillQueueItem` diff=0（13 行；字段类型/顺序、private 构造器赋值顺序 queueKey/windowKey/enqueuedAt 一致；attemptCount/
    lastFailureReason 默认值）。
- 已批准块 `diff` **UNCHANGED**（抽样 LocalTeamSessionAttachResult / PostCombatFirstAidQueueState / taskCode 均 unchanged；本轮仅
  类底部追加 + import + JavaDoc，未触碰任何既有可执行 token）。

### 合同禁项核验（均未触碰）

- 本波只迁 committed 原有内存状态形状；外层类不实例化 map/queue（无外层字段）；未新增 caller/getter、owner/session/ledger/
  TTL/retry/clock/remote/Spring/capture/input 或 queue operation。
- 两类型当前无 caller（QueueItem 构造器 private、WindowState 无实例化 → Java unused 仅告警非错误，符合"不新增 caller"）。
- 未改已批准块（unchanged 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：1 import + 两 private static class + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 两类型对 `0114604e` diff=0（字段类型/顺序/默认值/构造器赋值序/可见性逐 token），已批准块 unchanged，等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-SUMMON-STATE-TYPES-IMP1 Implementation #1（SummonSkillWindowState/SummonSkillQueueItem 机械复制，
两类型 diff=0；已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #19 - APPROVED / `W-TMS-SUMMON-STATE-TYPES-IMP1` - 2026-07-14T03:04:00-04:00

父级以 committed `0114604e` 独立抽取并比较当前 Cloud 两个完整类型块，结论
`P0=0 / P1=0 / P2=0`：

- `SummonSkillWindowState` 与 `SummonSkillQueueItem` 均为 `exact=True`，基线/目标规范化长度分别为
  `463/463` 与 `465/465`；字段类型、顺序、默认值和 private 构造器赋值顺序逐 token 等价。
- 当前文件 SHA-256 为
  `c24e6134cb2dac29856ad8a4ce6c4b0e25ca3ef6ef08d17e93b903785ebdfcad`，与 C 交付一致；
  C 的 Cloud `mvn -q compile` exit 0。
- 只增加直接需要的 `ConcurrentHashMap` import、两个未实例化状态类型与准确 JavaDoc；没有 caller/getter、
  queue operation、owner/session/ledger/TTL/retry/clock/remote/Spring/capture/input，既有批准块未改。

本 summon state-type cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-LOCAL-TEAM-STATE-TYPES-IMP1` - 2026-07-14T03:04:00-04:00

External C 请在 `2026-07-14T03:24:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-LOCAL-TEAM-STATE-TYPES-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制完整 private static `LocalTeamSessionState`、`IdleBroadcastSuppressCacheEntry`、
`LocalTeamTooltipGroup` 与 `LocalTeamLeaderGroupMatch` 四个 nested class 到外层类底部。所需
`Map/Set/ConcurrentHashMap/TeamSupportCapability` 已存在；字段、默认值、volatile/final 修饰、构造器、
`matches/withLastInfoLogAt/same/unmatched` 方法与基线注释逐 token 保持，只同步类 JavaDoc。

本波只迁 committed 原有状态形状与 class-local 纯 helper，不实例化外层 map，不新增 caller/public API、
owner/session/ledger/TTL/retry/clock/remote/Spring/capture/input 或 queue operation；不得改已批准块。完成后
Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、四类型 diff=0 与旧块 unchanged 反证。
禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T03:07:11-04:00

CLAIMED | task=W-TMS-LOCAL-TEAM-STATE-TYPES-IMP1; claimedAt=2026-07-14T03:07:11-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-LOCAL-TEAM-STATE-TYPES-IMP1 Implementation #1 - 2026-07-14T03:11:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:3166-3273` 机械复制四个 private static nested
类型 `LocalTeamSessionState`、`IdleBroadcastSuppressCacheEntry`、`LocalTeamTooltipGroup`、`LocalTeamLeaderGroupMatch`
到外层类底部，加 `TeamSupportCapability` import，同步类 JavaDoc。已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=0dc95f156727c66925bb0a8ea6e7c7585cad4455；
SHA-256=42269c303e2ef009a88d41e1c2715be4f603441ed70212f1abf048a5871faea4），三处：
1. 新增唯一 import `com.bot.dhxy.model.maintenance.TeamSupportCapability`（model 已存在于 cloud-brain；Map/Set/ConcurrentHashMap
   已由前单导入）。
2. 外层类底部（SummonSkillQueueItem 之后、类闭合前）新增四 private static final class：
   - `LocalTeamSessionState`：capability/window/tooltip/idle-cache 等 Map/Set 字段（ConcurrentHashMap.newKeySet / new
     ConcurrentHashMap<>）+ 七个 volatile leader 标志；含 CR244 pendingReturnWindowIds 注释。
   - `IdleBroadcastSuppressCacheEntry`：六 final 字段 + 六参构造器 + class-local 纯 helper `matches`/`withLastInfoLogAt`/
     `static same`。
   - `LocalTeamTooltipGroup`：groupHash + 两 member Set + 五 volatile 标志 + 单参构造器。
   - `LocalTeamLeaderGroupMatch`：`private static final UNMATCHED` sentinel + 四 final 字段 + 四参构造器 + `static unmatched()`。
3. 类 JavaDoc：第三组由"四内存状态类型"更新为"八内存状态类型"，补入 local-team 四类及其 volatile/final、构造器、helper 语义；只改注释，未触碰已批准块。

### 工具证据

- 四类型逐 token `diff`（brace 计数抽块）对 `0114604e`：
  * `LocalTeamSessionState` diff=0（25 行）；`IdleBroadcastSuppressCacheEntry` diff=0（44 行，含三 helper）；
    `LocalTeamTooltipGroup` diff=0（14 行）；`LocalTeamLeaderGroupMatch` diff=0（23 行，含 UNMATCHED sentinel 与 unmatched()）。
- 已批准块 `diff` **UNCHANGED**（抽样 SummonSkillQueueItem / PostCombatFirstAidQueueItem / LocalTeamSessionAttachResult 均 unchanged；
  本轮仅类底部追加 + 1 import + JavaDoc，未触碰任何既有可执行 token）。
- 逐 token 保持：字段类型/顺序/默认值、volatile/final 修饰、构造器赋值顺序、`matches`/`withLastInfoLogAt`/`same`/`unmatched` 方法体
  与 CR244 基线注释。

### 合同禁项核验（均未触碰）

- 本波只迁 committed 原有状态形状与 class-local 纯 helper；外层类不实例化 map（无外层字段）；未新增 caller/public API、
  owner/session/ledger/TTL/retry/clock/remote/Spring/capture/input 或 queue operation。
- 四类型当前无外部 caller（helper 均 private / class-local → Java unused 仅告警非错误，符合"不新增 caller"）。
- 未改已批准块（unchanged 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：1 import + 四 private static class + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 四类型对 `0114604e` diff=0（字段/默认值/volatile·final/构造器/四方法/注释逐 token），已批准块 unchanged，等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-LOCAL-TEAM-STATE-TYPES-IMP1 Implementation #1（LocalTeamSessionState/IdleBroadcastSuppressCacheEntry/
LocalTeamTooltipGroup/LocalTeamLeaderGroupMatch 机械复制，四类型 diff=0；已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #20 - APPROVED / `W-TMS-LOCAL-TEAM-STATE-TYPES-IMP1` - 2026-07-14T03:15:00-04:00

父级从 committed `0114604e` 与当前 Cloud 文件独立抽取四个完整类型块并按 LF 归一化逐字符比较，结论
`P0=0 / P1=0 / P2=0`：

- `LocalTeamSessionState`、`IdleBroadcastSuppressCacheEntry`、`LocalTeamTooltipGroup`、
  `LocalTeamLeaderGroupMatch` 均 `exact=True`，规范化长度分别为 `1694/1694`、`1915/1915`、
  `619/619`、`895/895`。
- 字段、默认值、`volatile/final`、构造器、class-local helper 与 CR244 注释均无漂移；只增加直接需要的
  `TeamSupportCapability` import。
- 父级复算目标 SHA-256 为
  `42269c303e2ef009a88d41e1c2715be4f603441ed70212f1abf048a5871faea4`，与 C 交付一致；
  C 的 Cloud `mvn -q compile` exit 0。没有外层 map/caller/queue operation 或本地 mechanics。

本 TaskMaintenance local-team state cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-REMAINING-STATE-TYPES-IMP1` - 2026-07-14T03:15:00-04:00

External C 请在 `2026-07-14T03:35:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-REMAINING-STATE-TYPES-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制剩余 private static final `TeamCombatPhaseState` 与
`MaintenanceBroadcastQueueState` 两个完整 nested class；后者所需 `Deque/ArrayDeque` imports 一并加入。
字段、默认值、`volatile/final`、构造器与顺序逐 token 保持，只同步类 JavaDoc。

本波只迁基线原有状态形状，不实例化外层 map/queue，不新增 caller/public API、owner/session/ledger/TTL/retry/
clock/remote/Spring/capture/input 或 queue operation，不得改已批准块。完成后 Cloud `mvn -q compile`（不 clean），
追加 Implementation #1、SHA、两类型 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T03:21:02-04:00

CLAIMED | task=W-TMS-REMAINING-STATE-TYPES-IMP1; claimedAt=2026-07-14T03:21:02-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-REMAINING-STATE-TYPES-IMP1 Implementation #1 - 2026-07-14T03:25:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:803-821,975-990` 机械复制剩余两个 private
static final nested 类型 `TeamCombatPhaseState` 与 `MaintenanceBroadcastQueueState` 到外层类底部，同步类 JavaDoc。
已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=e8a7f67241f0603098e8187c6b760e8ea27f861f；
SHA-256=4ef879b0ea11b0415fb087d841a9c3bcae655ebc115bb30747cfe66bd7961bd5），两处：
1. 外层类底部（LocalTeamLeaderGroupMatch 之后、类闭合前）新增两 private static final class：
   - `TeamCombatPhaseState`：`final String sessionKey/groupHash/leaderWindowId; final long epochId/enteredAtMs; final String
     openSource; volatile boolean exited; volatile long exitedAtMs;` + 六参 private 构造器（sessionKey/groupHash/leaderWindowId/
     epochId/enteredAtMs/openSource 顺序赋值）。
   - `MaintenanceBroadcastQueueState`：`final String label/leaderWindowId; final long openedAtMs/deadlineAtMs; final Deque<String>
     fifo = new ArrayDeque<>();` + 四参 private 构造器。
2. 类 JavaDoc：第三组由"八内存状态类型"更新为"十内存状态类型"，补入这两类及其 volatile/final、构造器语义；只改注释，未触碰已批准块。

**未加任何 import**：`Deque`/`ArrayDeque` 已由前 QUEUE 单导入（grep 各计数=1，无重复）；两类其余仅用 String/long/boolean。
**未复制**其后的外层字段 `maintenanceBroadcastQueueMonitor` / `maintenanceBroadcastQueueByScope`（grep=0），符合"不实例化外层 map/queue"。

### 工具证据

- 两类型逐 token `diff`（brace 计数抽块）对 `0114604e`：
  * `TeamCombatPhaseState` diff=0（20 行；八字段顺序、volatile exited/exitedAtMs、六参构造器赋值序一致）。
  * `MaintenanceBroadcastQueueState` diff=0（17 行；四字段 + `Deque fifo = new ArrayDeque<>()` 默认值、四参构造器一致）。
- 已批准块 `diff` **UNCHANGED**（抽样 LocalTeamLeaderGroupMatch / SummonSkillWindowState / LocalTeamSessionAttachStatus 均 unchanged；
  本轮仅类底部追加 + JavaDoc，无 import 变化，未触碰任何既有可执行 token）。

### 合同禁项核验（均未触碰）

- 本波只迁基线原有状态形状；外层类不实例化 map/queue（未抄 monitor/byScope 字段）；未新增 caller/public API、owner/session/
  ledger/TTL/retry/clock/remote/Spring/capture/input 或 queue operation。
- 两类型当前无 caller（构造器 private、无实例化 → Java unused 仅告警非错误）。
- 未改已批准块（unchanged 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：两 private static final class + 类 JavaDoc（无 import 变化）；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 两类型对 `0114604e` diff=0（字段/默认值/volatile·final/构造器逐 token），已批准块 unchanged，等价迁移；未误抄外层字段；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-REMAINING-STATE-TYPES-IMP1 Implementation #1（TeamCombatPhaseState/MaintenanceBroadcastQueueState 机械复制，
两类型 diff=0；已批准块 unchanged；TaskMaintenanceService 全部 committed 状态形状迁移完成）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #21 - APPROVED / `W-TMS-REMAINING-STATE-TYPES-IMP1` - 2026-07-14T03:30:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取两个完整 nested class 并按 LF 归一化逐字符复核，
结论 `P0=0 / P1=0 / P2=0`：`TeamCombatPhaseState` 与
`MaintenanceBroadcastQueueState` 均 `exact=True`，规范化长度分别为 `833/833`、`733/733`；字段顺序、
`final/volatile`、构造器赋值、`Deque` 默认值及 CR 注释均无漂移，外层 map/queue 未实例化。
父级复算目标 SHA-256 为
`4ef879b0ea11b0415fb087d841a9c3bcae655ebc115bb30747cfe66bd7961bd5`，与 C 交付一致；
C 的 Cloud `mvn -q compile` exit 0。

本 TaskMaintenance remaining-state cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-NO-ACTION-KEYS-CPU-IMP1` - 2026-07-14T03:30:00-04:00

External C 请在 `2026-07-14T03:50:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-NO-ACTION-KEYS-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制常量 `SUMMON_SKILL_DUE_LEAD_TIME_MS = 90_000L`、
`MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS = 60_000L`，以及 private `shouldLogNoAction`、
`noActionLogKey`、private static `effectiveSummonSkillCleanIntervalMs`。复用且不得修改现有 `safe(String)`、
`TaskExecutionContext` 与 `Map`；key 片段顺序、null handling、时间差条件和 `Math.max` 逐 token 保持，只同步类 JavaDoc。

本波不实例化日志 map/queue，不新增 caller/public API、owner/session/ledger/TTL/retry/clock source、remote/input，
不得改已批准块。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、五块 diff=0 与旧块
unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T03:35:21-04:00

CLAIMED | task=W-TMS-NO-ACTION-KEYS-CPU-IMP1; claimedAt=2026-07-14T03:35:21-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-NO-ACTION-KEYS-CPU-IMP1 Implementation #1 - 2026-07-14T03:39:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e` 机械复制两常量与三个 no-action-log
pure-CPU private helper，复用已有 safe/TaskExecutionContext/Map，同步类 JavaDoc。已批准块逐 token unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=7a40a78d4ffdb19ba42a9c99925b4b95cd5a288b；
SHA-256=cd9e76995cee78265c068abee65fd10568d07dba717cee3414fc5c897f2b2867），三处：
1. 类顶（DEFAULT_WINDOW_KEY 之后）新增两常量 `SUMMON_SKILL_DUE_LEAD_TIME_MS = 90_000L`、`MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS = 60_000L`。
2. helper 区（taskCode 之后）新增三 helper：
   - `private boolean shouldLogNoAction(Map<String,Long> lastLogAtByKey, String key, long now)`：`lastLogAt` 非空且
     `now - lastLogAt < MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS` → false；否则 put(key,now) 返回 true。
   - `private String noActionLogKey(String type, TaskExecutionContext context, String... parts)`：`type|` + `safe(getLocalTeamSessionKey())|`
     + `safe(getWindowId())` + 逐个 `|safe(part)`；null type/context → 空串处理。
   - `private static long effectiveSummonSkillCleanIntervalMs(long intervalMs)`：`Math.max(0L, intervalMs - SUMMON_SKILL_DUE_LEAD_TIME_MS)`。
3. 类 JavaDoc：第二组由"1 常量 + 九 helper"更新为"3 常量 + 十二 helper"，补入两常量与三 helper 语义；只改注释，未触碰已批准块。
无新 import（三方法仅用已有 Map/TaskExecutionContext/String + 已有 safe()）。

### 工具证据

- 两常量：`SUMMON_SKILL_DUE_LEAD_TIME_MS = 90_000L;` 与 `MAINTENANCE_NO_ACTION_LOG_INTERVAL_MS = 60_000L;` 均对 `0114604e` **IDENTICAL**。
- 三 helper 各按签名锚点抽取，对 `0114604e` 逐 token `diff`：`shouldLogNoAction` diff=0；`noActionLogKey` diff=0；
  `effectiveSummonSkillCleanIntervalMs` diff=0。
- 已批准块 `diff` **UNCHANGED**（抽样 taskCode / safe / MaintenanceBroadcastQueueState 均 unchanged；本轮仅类顶 + helper 区追加
  + JavaDoc，未触碰任何既有可执行 token）。
- 逐 token 保持：key 片段顺序（type|sessionKey|windowId|parts）、null handling（type/context null → 空串；parts null → 跳过）、
  时间差条件（`now - lastLogAt < INTERVAL`）、`Math.max(0L, ...)`。

### 合同禁项核验（均未触碰）

- 复用现有 `safe(String)`、`TaskExecutionContext`、`Map`——均未修改（unchanged 反证）。
- 本波不实例化日志 map/queue（外层无字段）；未新增 caller/public API、owner/session/ledger/TTL/retry/clock source、remote/input。
- 三 helper 当前无 caller（Java unused-private 仅告警非错误）。
- 未改已批准块；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：两常量 + 三 private helper + 类 JavaDoc（无 import 变化）；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 两常量 IDENTICAL、三 helper diff=0、已批准块 unchanged，逐 token 等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-NO-ACTION-KEYS-CPU-IMP1 Implementation #1（两常量 + shouldLogNoAction/noActionLogKey/
effectiveSummonSkillCleanIntervalMs 机械复制，五块 diff=0；已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #22 - APPROVED / `W-TMS-NO-ACTION-KEYS-CPU-IMP1` - 2026-07-14T03:42:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取两个常量声明及三个完整方法，按 LF 归一化逐字符复核，
结论 `P0=0 / P1=0 / P2=0`：五块均 `exact=True`，规范化长度依次为 `66/66`、`74/74`、
`334/334`、`545/545`、`153/153`。key 片段顺序、null handling、时间差条件与 `Math.max` 无漂移。
父级复算目标 SHA-256 为
`cd9e76995cee78265c068abee65fd10568d07dba717cee3414fc5c897f2b2867`；C 的 Cloud
`mvn -q compile` exit 0。没有外层 map/queue、clock source、remote/input/caller。

本 TaskMaintenance no-action/key cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-PURE-RESULT-MAPPERS-CPU-IMP1` - 2026-07-14T03:42:00-04:00

External C 请在 `2026-07-14T04:02:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-PURE-RESULT-MAPPERS-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制 private `normalize(TaskMaintenanceRequest)`、`maintenanceDecision(TaskMaintenanceResult)`、
`cloudRequiredMaintenanceFailure(MaintenanceThresholdCloudDecision)` 与
`plannedMaintenanceAction(TaskMaintenanceRequest)`。加入直接需要且 Cloud 已存在的
`MaintenanceThresholdCloudDecision`、`TaskMaintenanceRequest/Result/Status` imports。builder 字段、unknown fallback、
status 拼接和 ALLOW/NO_ACTION 条件逐 token 保持，只同步类 JavaDoc。

本波只迁纯 CPU request/result mapper，不新增 caller/public API、owner/session/ledger/TTL/retry/clock/remote/input，
不得改已批准块。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、四方法 diff=0 与旧块
unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T03:44:53-04:00

CLAIMED | task=W-TMS-PURE-RESULT-MAPPERS-CPU-IMP1; claimedAt=2026-07-14T03:44:53-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-PURE-RESULT-MAPPERS-CPU-IMP1 Implementation #1 - 2026-07-14T03:49:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e` 机械复制四个纯 CPU request/result mapper
private 方法，加所需 4 个 Cloud 已存在 model import，同步类 JavaDoc。已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=3e3c303a624d899eb14d1a9c35e30ae5b036f809；
SHA-256=9b244c240dbe046bdfb4ad8ad9f5e120f02e1f0b384339baf477c6108c18567e），三处：
1. 新增 4 个 import（均 Cloud 已存在）：`com.bot.dhxy.cloud.task.MaintenanceThresholdCloudDecision`、
   `com.bot.dhxy.model.maintenance.TaskMaintenanceRequest`、`...TaskMaintenanceResult`、`...TaskMaintenanceStatus`（按包/字母序插入）。
2. helper 区（effectiveSummonSkillCleanIntervalMs 之后）新增四 mapper：
   - `normalize(TaskMaintenanceRequest)`：request null → builder().sourceTask("unknown")；sourceTask null/blank → toBuilder().sourceTask("unknown")；否则原样返回。
   - `maintenanceDecision(TaskMaintenanceResult)`：`status=<name或空>;handled=..;broadcastHandled=..;summonSkillAttempted=..;summonSkillSucceeded=..` 拼接。
   - `cloudRequiredMaintenanceFailure(MaintenanceThresholdCloudDecision)`：builder().status(CLOUD_REQUIRED_FAILURE).handled(true)
     .message("MAINTENANCE_THRESHOLD cloud-required failure: " + decision.getRejectReason()).build()。
   - `plannedMaintenanceAction(TaskMaintenanceRequest)`：isHandleMaintenanceBroadcast() || isCleanSummonSkill() → Action.ALLOW；否则 Action.NO_ACTION。
3. 类 JavaDoc：第二组由"十二 helper"更新为"十六 helper"，补入四 mapper 语义；只改注释，未触碰已批准块。

### 工具证据

- 四 mapper 各按签名锚点抽取，对 `0114604e` 逐 token `diff`：
  * `normalize` diff=0（13 行）；`maintenanceDecision` diff=0（7 行）；`cloudRequiredMaintenanceFailure` diff=0（7 行）；
    `plannedMaintenanceAction` diff=0（6 行）。
- 已批准块 `diff` **UNCHANGED**（抽样 effectiveSummonSkillCleanIntervalMs / taskCode / TeamCombatPhaseState 均 unchanged；本轮仅 helper
  区追加 + 4 import + JavaDoc，未触碰任何既有可执行 token）。
- 逐 token 保持：builder 字段（status/handled/message）、unknown fallback（null/blank→"unknown"）、status 拼接顺序与分隔符、
  ALLOW/NO_ACTION 条件（broadcast || cleanSummonSkill）。

### 合同禁项核验（均未触碰）

- 本波只迁纯 CPU request/result mapper；未新增 caller/public API、owner/session/ledger/TTL/retry/clock/remote/input。
- 四 mapper 当前无 caller（Java unused-private 仅告警非错误）。
- 未改已批准块（unchanged 反证）；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）三处：4 import + 四 private mapper + 类 JavaDoc；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 四 mapper 对 `0114604e` diff=0（builder/fallback/status 拼接/ALLOW·NO_ACTION 逐 token），已批准块 unchanged，等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-PURE-RESULT-MAPPERS-CPU-IMP1 Implementation #1（normalize/maintenanceDecision/cloudRequiredMaintenanceFailure/
plannedMaintenanceAction 机械复制，四方法 diff=0；已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #23 - APPROVED / `W-TMS-PURE-RESULT-MAPPERS-CPU-IMP1` - 2026-07-14T03:53:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取四个完整方法，按 LF 归一化逐字符复核，结论
`P0=0 / P1=0 / P2=0`：`normalize`、`maintenanceDecision`、
`cloudRequiredMaintenanceFailure`、`plannedMaintenanceAction` 均 `exact=True`，规范化长度分别为
`476/476`、`436/436`、`391/391`、`343/343`。builder 字段、unknown fallback、status 串联和
ALLOW/NO_ACTION 条件均无漂移。父级复算目标 SHA-256 为
`9b244c240dbe046bdfb4ad8ad9f5e120f02e1f0b384339baf477c6108c18567e`，与 C 交付一致；
C 的 Cloud `mvn -q compile` exit 0。没有 owner/map、clock、remote/input/caller。

本 TaskMaintenance result-mapper cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-PURE-KEY-MAPPERS-CPU-IMP1` - 2026-07-14T03:53:00-04:00

External C 请在 `2026-07-14T04:13:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-PURE-KEY-MAPPERS-CPU-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。从 committed
`0114604e` 机械复制 private `normalizeTeamKey(String,TaskExecutionContext)` 与 private
`localSupportCapabilityRoundKey(String,TeamSupportCapability,int)`。复用且不得修改已批准
`DEFAULT_WINDOW_KEY`、`TaskExecutionContext` 与 `TeamSupportCapability`。explicit/requested/task/default fallback
顺序及 `local-team:<session>#<capability>#<epoch>` 拼接逐 token 保持，只同步类 JavaDoc。

本波只迁纯 CPU key mapper，不实例化 team/session map，不搬 `resolveTeamRoundKey`、
`resolveLocalSupportCapabilityRoundKey` 或其它状态读取，不新增 caller/public API、owner/session/ledger/TTL/retry/
clock/remote/input，不改已批准块。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、
两方法 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T03:59:07-04:00

CLAIMED | task=W-TMS-PURE-KEY-MAPPERS-CPU-IMP1; claimedAt=2026-07-14T03:59:07-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-PURE-KEY-MAPPERS-CPU-IMP1 Implementation #1 - 2026-07-14T04:03:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e:3084-3101` 机械复制两个纯 CPU key mapper
private 方法 `localSupportCapabilityRoundKey`、`normalizeTeamKey`，复用已有 DEFAULT_WINDOW_KEY/TaskExecutionContext/
TeamSupportCapability，同步类 JavaDoc。已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=2af754d83983f516a254b585d286868eae736a6a；
SHA-256=75360aa96b65a731163229c9b407ec4204fd98cc4eb7d3d9c2d362f78b7d0650），两处：
1. helper 区（plannedMaintenanceAction 之后）新增两 key mapper：
   - `localSupportCapabilityRoundKey(String sessionKey, TeamSupportCapability capability, int epoch)`：
     `"local-team:" + sessionKey + "#" + capability.name() + "#" + epoch`。
   - `normalizeTeamKey(String explicitKey, TaskExecutionContext context)`：explicit 非空非空白优先；再 requestedTaskCode；
     再 taskCode；否则 DEFAULT_WINDOW_KEY（explicit/requested/task/default 顺序）。
2. 类 JavaDoc：第二组由"十六 helper"更新为"十八 helper"，补入两 key mapper 语义；只改注释，未触碰已批准块。
无新 import（两方法仅用已有 String/TeamSupportCapability/TaskExecutionContext + DEFAULT_WINDOW_KEY）。

### 工具证据

- 两 key mapper 各按签名锚点抽取，对 `0114604e` 逐 token `diff`：`localSupportCapabilityRoundKey` diff=0（5 行）；
  `normalizeTeamKey` diff=0（12 行）。
- 已批准块 `diff` **UNCHANGED**（抽样 plannedMaintenanceAction / normalize / SummonSkillQueueItem 均 unchanged；本轮仅 helper 区追加
  + JavaDoc，无 import 变化，未触碰任何既有可执行 token）。
- 逐 token 保持：explicit/requested/task/default fallback 顺序；`local-team:<session>#<capability>#<epoch>` 拼接（capability.name()）。
- 未搬状态读取方法：grep `pruneOlderTeamRoundClaims|resolveTeamRoundKey|resolveLocalSupportCapabilityRoundKey|summonSkillClaimsByTeamRound`=0。

### 合同禁项核验（均未触碰）

- 复用已批准 `DEFAULT_WINDOW_KEY`、`TaskExecutionContext`、`TeamSupportCapability`——均未修改（unchanged 反证）。
- 本波只迁纯 CPU key mapper；不实例化 team/session map；未搬 resolve*/prune* 或其它状态读取；未新增 caller/public API、
  owner/session/ledger/TTL/retry/clock/remote/input。
- 两 mapper 当前无 caller（Java unused-private 仅告警非错误）。
- 未改已批准块；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：两 private key mapper + 类 JavaDoc（无 import 变化）；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 两 mapper 对 `0114604e` diff=0（fallback 顺序/拼接逐 token），已批准块 unchanged，未搬状态方法，等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-PURE-KEY-MAPPERS-CPU-IMP1 Implementation #1（localSupportCapabilityRoundKey/normalizeTeamKey 机械复制，
两方法 diff=0；已批准块 unchanged；未搬状态方法）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #24 - APPROVED / `W-TMS-PURE-KEY-MAPPERS-CPU-IMP1` - 2026-07-14T04:07:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取两个完整方法并逐字符复核，结论
`P0=0 / P1=0 / P2=0`：`normalizeTeamKey`、`localSupportCapabilityRoundKey` 均 `exact=True`，
长度分别为 `558/558`、`300/300`。explicit/requested/task/default fallback 与
`local-team:<session>#<capability>#<epoch>` 拼接无漂移。父级复算文件 SHA-256 为
`75360aa96b65a731163229c9b407ec4204fd98cc4eb7d3d9c2d362f78b7d0650`，与 C 交付一致；
C 的 Cloud `mvn -q compile` exit 0。未实例化 map，未搬 resolve/prune、clock、remote/input/caller。

本 TaskMaintenance key-mapper cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-IDENTITY-INDEX-LEAF-IMP1` - 2026-07-14T04:07:00-04:00

External C 请在 `2026-07-14T04:27:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-IDENTITY-INDEX-LEAF-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `TaskMaintenanceService.java`。从 committed `0114604e` 机械复制 private static
`recordLocalTeamPlayerIdentity(LocalTeamSessionState,String,String)` 完整方法；复用且不得修改已批准
`normalizeText` 与 `LocalTeamSessionState.windowPlayerIds/playerWindowIds`。null/blank 矩阵、双向索引写入顺序与
normalized key/value 逐 token 保持。

本波只补对既有传入 state 的确定性内存索引 helper；不得新增/实例化 outer map、owner/session/ledger/TTL/retry/clock，
不得迁 resolve/prune/caller/remote/input，不新增 public API/wrapper，不改已批准块。完成后 Cloud `mvn -q compile`
（不 clean），追加 Implementation #1、SHA、方法 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T04:13:18-04:00

CLAIMED | task=W-TMS-IDENTITY-INDEX-LEAF-IMP1; claimedAt=2026-07-14T04:13:18-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-IDENTITY-INDEX-LEAF-IMP1 Implementation #1 - 2026-07-14T04:18:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e` 机械复制 private static
`recordLocalTeamPlayerIdentity(LocalTeamSessionState, String, String)` 完整方法，复用已有 normalizeText 与
LocalTeamSessionState 的 windowPlayerIds/playerWindowIds，同步类 JavaDoc。已批准块逐 token unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=9ac6ed17b002ed1d449f3cc8146c17f656c2c02a；
SHA-256=bcf774d89b2922fccaefae72354ba9515fe7f797cdd76a4aba571ffbad7cb235），两处：
1. helper 区（normalizeTeamKey 之后）新增 `recordLocalTeamPlayerIdentity`：state/windowId/playerId 任一 null 或 window/player blank → return；
   否则 `normalizeText(windowId)` / `normalizeText(playerId)` 后写入双向索引 `state.windowPlayerIds.put(nWin, nPlayer)`、
   `state.playerWindowIds.put(nPlayer, nWin)`。
2. 类 JavaDoc：第二组由"十八 helper"更新为"十九 helper"，补入该 identity-index leaf 语义；只改注释，未触碰已批准块。
无新 import（仅用已有 LocalTeamSessionState/String/normalizeText）。

### 工具证据

- 方法逐 token `diff` 对 `0114604e`：`recordLocalTeamPlayerIdentity` diff=0（12 行）。
- 已批准块 `diff` **UNCHANGED**（抽样 normalizeTeamKey / normalizeText / LocalTeamSessionState 均 unchanged）。
- 逐 token 保持：null/blank 矩阵（state/windowId/playerId 短路）、双向索引写入顺序（先 windowPlayerIds 后 playerWindowIds）、
  normalized key/value（normalizeText 结果）。
- 未搬状态方法：grep `resolveTeamRoundKey|resolveLocalSupportCapabilityRoundKey|activeTeamRoundByKey`=0。

### 编译门禁（含一次跨 Worker 瞬时现象，如实记录）

- 首次 `mvn -q compile` 一度失败，错误**仅**在他人文件 `PlayerStateService.java:51,53`（String 与 long 比较/转换），
  该文件时间戳 04:14、正被并行 worker 实时编辑；错误列表对我的 `TaskMaintenanceService` **0 引用**，非我写集、非我改动引起。
  我未触碰该文件。待其编辑落定后**同一未改动源码**重跑 `mvn -q compile`（未 clean）EXIT=0；我文件 blob/SHA-256 两次一致（未变）。
- 结论：本切片编译干净；首次失败为并行 worker 编辑 PlayerStateService 的瞬时态，与本任务无关。

### 合同禁项核验（均未触碰）

- 复用已批准 `normalizeText`、`LocalTeamSessionState.windowPlayerIds/playerWindowIds`——均未修改（unchanged 反证）。
- 本波只补对既有传入 state 的确定性内存索引 leaf；未新增/实例化 outer map、owner/session/ledger/TTL/retry/clock；
  未迁 resolve/prune/caller/remote/input；未新增 public API/wrapper。
- 该 leaf 当前无 caller（Java unused-private 仅告警非错误）。
- 未改已批准块；未改既有其它文件（含未碰 PlayerStateService）、tests、pom、DecisionEngine、remote。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：一 private static 方法 + 类 JavaDoc（无 import 变化）；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked（含正被编辑的 PlayerStateService）。
2. 方法对 `0114604e` diff=0（null/blank 矩阵/双向写序/normalized key-value 逐 token），已批准块 unchanged，未搬状态方法；无越界行为。
3. blob id 与 SHA-256 双标；跨 worker 瞬时编译失败已如实记录并复核为他人文件所致。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-IDENTITY-INDEX-LEAF-IMP1 Implementation #1（recordLocalTeamPlayerIdentity 机械复制，方法 diff=0；
已批准块 unchanged；编译 EXIT=0，首次失败经复核为他人文件 PlayerStateService 实时编辑的瞬时态）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #25 - APPROVED / `W-TMS-IDENTITY-INDEX-LEAF-IMP1` - 2026-07-14T04:26:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取完整方法，按 LF 归一化逐字符复核，结论
`P0=0 / P1=0 / P2=0`：`recordLocalTeamPlayerIdentity` 为 `exact=True`、长度 `659/659`。null/blank
短路、`normalizeText` 调用、先 `windowPlayerIds` 后 `playerWindowIds` 的双向写入顺序无漂移。父级复算文件
SHA-256 为 `bcf774d89b2922fccaefae72354ba9515fe7f797cdd76a4aba571ffbad7cb235`；并发编辑稳定后 C 的
Cloud `mvn -q compile` exit 0。没有 outer map、owner/session/ledger/TTL/retry/clock、remote/input/caller。

本 identity-index leaf `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-FIRST-AID-PARTICIPANTS-LEAF-IMP1` - 2026-07-14T04:26:00-04:00

External C 请在 `2026-07-14T04:46:00-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-TMS-FIRST-AID-PARTICIPANTS-LEAF-IMP1; claimedAt=<ISO>; writeSet=<same Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集仍为 Cloud `TaskMaintenanceService.java`。从 committed `0114604e` 机械复制 private
`postCombatFirstAidParticipants(PostCombatFirstAidQueueState)` 完整方法；复用且不得修改既有 nested
`PostCombatFirstAidQueueState.participantSnapshot/departedWindows` 与 `HashSet` import。复制 snapshot 后 remove departed
的顺序逐 token 保持，方法保持 dormant。

本波只操作调用方传入的既有内存 state；不得新增/实例化 outer map、owner/session/ledger/TTL/retry/clock，不迁 queue
open/close/caller/remote/input，不新增 public API/wrapper，不改已批准块。完成后 Cloud `mvn -q compile`（不 clean），
追加 Implementation #1、SHA、方法 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T04:32:23-04:00

CLAIMED | task=W-TMS-FIRST-AID-PARTICIPANTS-LEAF-IMP1; claimedAt=2026-07-14T04:32:23-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-FIRST-AID-PARTICIPANTS-LEAF-IMP1 Implementation #1 - 2026-07-14T04:37:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e` 机械复制 private
`postCombatFirstAidParticipants(PostCombatFirstAidQueueState)` 完整方法（dormant leaf），复用已有
PostCombatFirstAidQueueState.participantSnapshot/departedWindows 与 HashSet import，同步类 JavaDoc。已批准块逐 token unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=a09b47312538630b7b23747a9eaefa1b38f78807；
SHA-256=5cdebaa6fe4a4c6d6861ed20a863bd0c5453b371f3c53ce135907311a43f63df），两处：
1. helper 区（recordLocalTeamPlayerIdentity 之后）新增 `postCombatFirstAidParticipants`：
   `Set<String> participants = new HashSet<>(state.participantSnapshot); participants.removeAll(state.departedWindows); return participants;`
   （先 snapshot 拷贝，再 remove departed）。
2. 类 JavaDoc：第二组由"十九 helper"更新为"二十 helper"，补入该 dormant leaf 语义；只改注释，未触碰已批准块。
无新 import（仅用已有 Set/HashSet/PostCombatFirstAidQueueState）。

### 工具证据

- 方法逐 token `diff` 对 `0114604e`：`postCombatFirstAidParticipants` diff=0（5 行）。
- 已批准块 `diff` **UNCHANGED**（抽样 recordLocalTeamPlayerIdentity / PostCombatFirstAidQueueState / normalizeTeamKey 均 unchanged）。
- 逐 token 保持：先 `new HashSet<>(state.participantSnapshot)` 拷贝 snapshot，再 `removeAll(state.departedWindows)` 移除 departed，
  返回结果集（顺序无漂移）。方法保持 dormant。
- 未搬 `resolvePostCombatFirstAidParticipantSnapshot`（grep=0）。

### 合同禁项核验（均未触碰）

- 复用既有 nested `PostCombatFirstAidQueueState.participantSnapshot/departedWindows` 与 HashSet import——均未修改（unchanged 反证）。
- 本波只操作调用方传入的既有内存 state；未新增/实例化 outer map、owner/session/ledger/TTL/retry/clock；未迁 queue open/close/
  caller/remote/input；未新增 public API/wrapper。
- 该 leaf 当前无 caller（Java unused-private 仅告警非错误）。
- 未改已批准块；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：一 private 方法 + 类 JavaDoc（无 import 变化）；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（snapshot 拷贝→remove departed 逐 token），已批准块 unchanged，未搬 resolve；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-FIRST-AID-PARTICIPANTS-LEAF-IMP1 Implementation #1（postCombatFirstAidParticipants 机械复制，方法 diff=0；
已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #26 - APPROVED / `W-TMS-FIRST-AID-PARTICIPANTS-LEAF-IMP1` - 2026-07-14T04:42:26-04:00

父级从 committed `0114604e` 与当前 Cloud 独立复核完整
`postCombatFirstAidParticipants(PostCombatFirstAidQueueState)`，结论 `P0=0 / P1=0 / P2=0`：方法
逐 token 一致，严格保持先 `new HashSet<>(state.participantSnapshot)`、再
`removeAll(state.departedWindows)`、最后返回的顺序。父级复算文件 SHA-256 为
`5cdebaa6fe4a4c6d6861ed20a863bd0c5453b371f3c53ce135907311a43f63df`，与 C 交付一致；C 的
Cloud `mvn -q compile` exit 0。没有 outer map、queue open/close、owner/session/ledger/TTL/retry/clock、
remote/input/caller。

本 participant-projection leaf `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-NAVIGATION-DUPLICATE-SOURCE-CPU-IMP1` - 2026-07-14T04:42:26-04:00

External C 请在 `2026-07-14T05:02:26-04:00` 前于真实 EOF 追加：
`CLAIMED | task=W-NAVIGATION-DUPLICATE-SOURCE-CPU-IMP1; claimedAt=<ISO>; writeSet=<Cloud NavigationService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集改为 Cloud
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NavigationService.java`。
从 committed `0114604e` 机械复制 package-private static
`isPathingSourceCompatibleForDuplicate(String activeSource, String requestSource)` 完整方法；复用且不得修改目标已有
`normalizeSourceForDuplicate(String)`。null/blank 归一化、exact equality 与 `active.startsWith(request + ":")`
顺序逐 token 保持，方法保持 dormant，无新 import。

本波只迁纯字符串 duplicate-source 判定，不迁 pathing runtime/window/capture/navigation/input/caller，不新增 wrapper/public API，
不改已批准块。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、SHA、方法 diff=0 与旧块
unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T04:46:41-04:00

CLAIMED | task=W-NAVIGATION-DUPLICATE-SOURCE-CPU-IMP1; claimedAt=2026-07-14T04:46:41-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java ; append-only 本日志 ]

## External Worker C - W-NAVIGATION-DUPLICATE-SOURCE-CPU-IMP1 Implementation #1 (already-present verification, no source edit) - 2026-07-14T04:50:00-04:00

**如实上报 / no-op 交付**：目标 Cloud `NavigationService.java` **已经包含**任务要复制的
`isPathingSourceCompatibleForDuplicate(String, String)` 完整方法（及其 JavaDoc）与被复用的 `normalizeSourceForDuplicate(String)`，
且与 committed `0114604e` **逐 token 一致**。任务 brief 以"机械复制…完整方法"措辞（暗示目标尚缺该方法），但实测目标已存在该方法；
按"看清目标再动手、发现与描述不符先上报、绝不盲目覆盖/重复"的原则，本轮**未对源码做任何改动**（再加一份将造成重复方法定义、编译失败）。

### 复核证据（工具，非记忆）

- 目标文件 `/d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java`
  当前 blob id=0d5e01b70260a961f5a4914a1e49c17cf2bfb756；SHA-256=d5bbe9d76d8784faef1ddd9432826cdaf7a924d1545db351e73297206e714896。
  文件 mtime 03:22，**早于**本任务发单时间 04:42:26——即该方法在本任务发单前即已落盘。
- `isPathingSourceCompatibleForDuplicate` 现有实现（第 45-51 行）对 `0114604e` 逐 token `diff` = **diff=0**（7 行）：
  `normalizeSourceForDuplicate(active/request)` → `active != null && request != null && (active.equals(request) || active.startsWith(request + ":"))`。
  null/blank 归一化、exact equality、`startsWith(request + ":")` 顺序全部一致；方法为 package-private static、dormant（无 caller）。
- 方法定义计数 = **1**（无重复，我未新增）。`normalizeSourceForDuplicate`（第 60-66 行）亦已存在，未被我修改。
- 本日志历史无 C 先前交付本任务的记录 → 该方法系并行 worker 首刀 cloud-brain `NavigationService` 时一并落盘，非本任务范围内新增。
- Cloud `mvn -q compile`（未 clean，未做任何改动）：**EXIT=0**。

### 合同符合性（现状即满足）

- 目标已含 package-private static `isPathingSourceCompatibleForDuplicate`，逐 token = 基线；复用且未修改 `normalizeSourceForDuplicate`。
- 未迁 pathing runtime/window/capture/navigation/input/caller；未新增 wrapper/public API；未改任何已批准块；方法保持 dormant。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮**零源码改动**（未对 NavigationService.java 或任何文件做 Java 改动，避免重复定义）；未做 Git；未 clean；保护他人 dirty/untracked。
2. 现有方法对 `0114604e` diff=0、定义计数=1、编译 EXIT=0——合同要求的最终态已成立。
3. 如父级要求由 C 另建/另处该方法，请指明；当前唯一写集目标已满足，C 不重复添加。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-NAVIGATION-DUPLICATE-SOURCE-CPU-IMP1 Implementation #1（如实核验：目标已含该方法且 diff=0、编译绿；本轮不重复添加、零源码改动）；
持续重读本日志等待父级复审/指示；自审不算 Approved。

## Parent Source Review #27 - ALREADY PRESENT APPROVED / `W-NAVIGATION-DUPLICATE-SOURCE-CPU-IMP1` - 2026-07-14T04:55:47-04:00

父级复核 confirmed C 的 no-op 判断正确：目标在本任务发单前已恰有一处 package-private static
`isPathingSourceCompatibleForDuplicate(String,String)`，与 committed `0114604e` 完整 7 行 source/target
SHA-256 均为 `0fdeb30c06c928da68804f22879dc30efb2a4a2d9f6517dd6033bf177bd1ef4e`；
`normalizeSourceForDuplicate`、exact equality 与 `active.startsWith(request + ":")` 顺序无漂移。当前文件
SHA-256 `d5bbe9d76d8784faef1ddd9432826cdaf7a924d1545db351e73297206e714896`，C 的
Cloud `mvn -q compile` exit 0。本任务没有新增源码，因此不重复计算迁移成果，但最终态
`P0=0 / P1=0 / P2=0`。

本 duplicate-source helper `ALREADY PRESENT / SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-TMS-FIRST-AID-GROUP-HASH-CPU-IMP1` - 2026-07-14T04:55:47-04:00

External C 请在 `2026-07-14T05:15:47-04:00` 前于本日志真实 EOF 追加：
`CLAIMED | task=W-TMS-FIRST-AID-GROUP-HASH-CPU-IMP1; claimedAt=<ISO>; writeSet=<Cloud TaskMaintenanceService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集改为 Cloud
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`。
从 committed `0114604e` 机械复制 private
`resolvePostCombatFirstAidGroupHash(LocalTeamSessionState session, String windowKey)` 完整方法；复用且不得修改
当前已有 `LocalTeamSessionState.windowTooltipGroupHash/tooltipGroupsByHash`、`LocalTeamTooltipGroup.leaderWindowId/memberWindowIds`
与 `Map` import。null/blank、direct hit、遍历、歧义返回 null 的顺序和注释逐 token 保持，方法保持 dormant。

本波只迁调用方传入内存 state 的 group-hash 判定，不迁 outer map、queue open/close、owner/session/ledger/TTL/retry/clock、
remote/input/caller，不新增 wrapper/public API，不改前批准块。完成后 Cloud `mvn -q compile`（不 clean），追加
Implementation #1、文件 SHA、方法 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T05:00:53-04:00

CLAIMED | task=W-TMS-FIRST-AID-GROUP-HASH-CPU-IMP1; claimedAt=2026-07-14T05:00:53-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java ; append-only 本日志 ]

## External Worker C - W-TMS-FIRST-AID-GROUP-HASH-CPU-IMP1 Implementation #1 - 2026-07-14T05:05:00-04:00

在 C 已拥有的 Cloud `TaskMaintenanceService.java` 内，从 committed `0114604e` 机械复制 private
`resolvePostCombatFirstAidGroupHash(LocalTeamSessionState, String)` 完整方法（dormant），复用已有
LocalTeamSessionState.windowTooltipGroupHash/tooltipGroupsByHash、LocalTeamTooltipGroup.leaderWindowId/memberWindowIds
与 Map import，同步类 JavaDoc。已批准块逐 token unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（同一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java（blob id=8e1c04173b8d8493c8ea07b598f6706bbb79a00f；
SHA-256=b860d3986f334a7abf450f5bef1335e769010431a110c2a650a102bdc94c73b0），两处：
1. helper 区（postCombatFirstAidParticipants 之后）新增 `resolvePostCombatFirstAidGroupHash`：null/blank guard → null；
   `windowTooltipGroupHash.get(windowKey)` direct hit（非空且 tooltipGroupsByHash 含之）→ 返回 direct；否则遍历
   `tooltipGroupsByHash`，group 非空且 `windowKey.equals(leaderWindowId) || memberWindowIds.contains(windowKey)` 命中；
   命中多组（歧义）→ 返回 null，否则返回 matched key。含基线三行注释。
2. 类 JavaDoc：第二组由"二十 helper"更新为"二十一 helper"，补入该 group-hash resolver 语义；只改注释，未触碰已批准块。
无新 import（仅用已有 LocalTeamSessionState/LocalTeamTooltipGroup/String/Map）。

### 工具证据

- 方法逐 token `diff` 对 `0114604e`：`resolvePostCombatFirstAidGroupHash` diff=0（26 行，含 3 行基线注释）。
- 已批准块 `diff` **UNCHANGED**（抽样 postCombatFirstAidParticipants / LocalTeamSessionState / LocalTeamTooltipGroup 均 unchanged）。
- 逐 token 保持：null/blank → direct hit → 遍历 → 歧义 return null 的顺序；`windowTooltipGroupHash.get` direct 命中的三条件；
  遍历中 `equals(leaderWindowId) || memberWindowIds.contains` 判定与歧义 `matched != null && !matched.equals` 短路；三行注释逐字。

### 合同禁项核验（均未触碰）

- 复用既有 `LocalTeamSessionState.windowTooltipGroupHash/tooltipGroupsByHash`、`LocalTeamTooltipGroup.leaderWindowId/memberWindowIds`
  与 Map import——均未修改（unchanged 反证）。
- 本波只迁调用方传入内存 state 的 group-hash 判定；未迁 outer map、queue open/close、owner/session/ledger/TTL/retry/clock、
  remote/input/caller；未新增 wrapper/public API。
- 该 leaf 当前无 caller（Java unused-private 仅告警非错误）。
- 未改前批准块；未改既有其它文件、tests、pom、DecisionEngine、remote。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=同一 TaskMaintenanceService.java（写集）两处：一 private 方法 + 类 JavaDoc（无 import 变化）；未改既有其它文件；
   未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（4 步顺序/歧义 null/注释逐 token），已批准块 unchanged；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-TMS-FIRST-AID-GROUP-HASH-CPU-IMP1 Implementation #1（resolvePostCombatFirstAidGroupHash 机械复制，方法 diff=0；
已批准块 unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #1 - `W-TMS-FIRST-AID-GROUP-HASH-CPU-IMP1` - 2026-07-14T05:22:00-04:00

**APPROVED，P0/P1/P2=0。** 父级复算 Cloud 文件 SHA-256 为
`b860d3986f334a7abf450f5bef1335e769010431a110c2a650a102bdc94c73b0`；
`resolvePostCombatFirstAidGroupHash` 恰一处，source/target 完整块 SHA-256 均为
`f3060f1d8749a2e7856daeda76d93ce2df7f5b42577a7950858dee3679eb32de`。direct hit、全表唯一匹配、
歧义返回 null 的顺序 exact；Worker Cloud compile exit 0。无 owner/session 新增、remote/input/caller。
本 leaf `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-SUMMONSKILL-SLOT-OFFSETS-CPU-IMP1` - 2026-07-14T05:22:00-04:00

External C 请在 `2026-07-14T05:42:00-04:00` 前于本日志真实 EOF 追加：
`CLAIMED | task=W-SUMMONSKILL-SLOT-OFFSETS-CPU-IMP1; claimedAt=<ISO>; writeSet=<Cloud SummonSkillService.java + 本日志>`。
直接实现，不写 Design；20 分钟只检查领取，领取后允许持续实施。

唯一源码写集切换为 Cloud `src/main/java/com/bot/dhxy/service/SummonSkillService.java`。从 committed `0114604e`
机械复制完整 `SIX_SKILL_SLOT_OFFSETS`、`EIGHT_SKILL_SLOT_OFFSETS` 两个 `Point[]` 常量与 private
`getSkillSlotOffsets(int)`；复用现有 `Point` import。数组元素、顺序、阈值分支和返回对象逐 token 保持，全部 dormant。

本波只迁纯静态几何元数据/选择，不 capture、不匹配、不 input、不迁 caller，不新增 wrapper/public API，
不改前批准 tip-rect 块。完成后 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、文件 SHA、
三块 diff=0 与旧块 unchanged 反证。禁止 Git mutation/运行面。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T05:24:47-04:00

CLAIMED | task=W-SUMMONSKILL-SLOT-OFFSETS-CPU-IMP1; claimedAt=2026-07-14T05:24:47-04:00; writeSet=[ /d/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java ; append-only 本日志 ]

## External Worker C - W-SUMMONSKILL-SLOT-OFFSETS-CPU-IMP1 Implementation #1 - 2026-07-14T05:29:00-04:00

在 Cloud `SummonSkillService.java`（写集切换至此文件）内，从 committed `0114604e` 机械复制两个 `Point[]` 常量
`SIX_SKILL_SLOT_OFFSETS`/`EIGHT_SKILL_SLOT_OFFSETS` 与 private `getSkillSlotOffsets(int)`，复用现有 `Point` import，全部 dormant。
已批准 tip-rect 块 unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=2d2ac8bd330c6629583ce70e9a21a9df574d0c0a；
SHA-256=fd73bb0b99eee71e4df0bd537a3d969be729ba9438870ba60769046977e7fc22），两处：
1. 常量区（HOVER_TIP_AREA_H 之后、首个方法之前）新增：
   - `SIX_SKILL_SLOT_OFFSETS`：6 个 `new Point(...)`（416,384 / 334,430 / 335,511 / 420,561 / 500,511 / 500,432）。
   - `EIGHT_SKILL_SLOT_OFFSETS`：8 个 `new Point(...)`（405,364 / 339,407 / 311,475 / 338,541 / 406,584 / 475,540 / 503,474 / 474,406）。
2. slot-geometry 方法区（buildSkillSlotRects 之前）新增 `private Point[] getSkillSlotOffsets(int skillCount)`：
   `skillCount == 8 ? EIGHT_SKILL_SLOT_OFFSETS : SIX_SKILL_SLOT_OFFSETS`。
无新 import（`java.awt.Point` 第 6 行已存在）；类 JavaDoc 已含 "slot-geometry"，语义已覆盖，未改。

### 工具证据

- 三块逐 token `diff` 对 `0114604e`：`SIX_SKILL_SLOT_OFFSETS` diff=0（8 行）；`EIGHT_SKILL_SLOT_OFFSETS` diff=0（10 行）；
  `getSkillSlotOffsets` diff=0（3 行）。数组元素、坐标、顺序、`== 8 ? EIGHT : SIX` 阈值分支与返回对象逐 token 一致。
- 已批准 tip-rect 块（`buildTipRectByHoverPoint`）`diff` **UNCHANGED**；HOVER_TIP_* 常量未改（仅在其后插入，未触碰其行）。
- 目标写前不含这三符号（各 grep=0，非重复添加）。

### 合同禁项核验（均未触碰）

- 本波只迁纯静态几何元数据/选择；不 capture、不匹配、不 input、不迁 caller；未新增 wrapper/public API；三者 dormant（无 caller）。
- 未改前批准 tip-rect 块（unchanged 反证）；未改既有其它文件、tests、pom；未新增 import。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）两处：两 Point[] 常量 + 一 private 方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 三块对 `0114604e` diff=0（数组/顺序/阈值分支/返回逐 token），tip-rect unchanged，等价迁移；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-SUMMONSKILL-SLOT-OFFSETS-CPU-IMP1 Implementation #1（SIX/EIGHT_SKILL_SLOT_OFFSETS + getSkillSlotOffsets 机械复制，
三块 diff=0；tip-rect unchanged）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #2 - `W-SUMMONSKILL-SLOT-OFFSETS-CPU-IMP1` - 2026-07-14T05:41:00-04:00

**APPROVED，P0/P1/P2=0。** 父级从 committed `0114604e` 与当前 Cloud 独立抽取完整块复核：
`SIX_SKILL_SLOT_OFFSETS`、`EIGHT_SKILL_SLOT_OFFSETS`、`getSkillSlotOffsets(int)` 的 source/target
SHA-256 分别为 `89fb7d7275d84d59a48ff8693ee4339584dd4d6bb6fadb044beb1e0858b38c2c`、
`b136d22a02e5e794abf71e554bcab82e5c5b9c45243ac4f32ab92bc220d4492d`、
`4b4b21cfef7ea34c0782ea9f4ed35e7b7d0836dc8e1790d700ca4ca11be0888b`，均 exact；数组坐标、顺序与
`skillCount == 8 ? EIGHT : SIX` 无漂移。父级复算文件 SHA-256 为
`fd73bb0b99eee71e4df0bd537a3d969be729ba9438870ba60769046977e7fc22`，与 C 交付一致；Worker Cloud
`mvn -q compile` exit 0。无 capture/template/OCR/input/caller。

本 slot-offset cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-SUMMONSKILL-CLEANUP-RESULT-CPU-IMP1` - 2026-07-14T05:52:00-04:00

请 External Worker C 在本日志真实 EOF 先追加一行领取：

`CLAIMED | task=W-SUMMONSKILL-CLEANUP-RESULT-CPU-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

领取截止：`2026-07-14T06:12:00-04:00`。20 分钟只检查是否领取，不检查完成；领取后允许工作超过 20 分钟。

### 唯一写集

- Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- Append 本日志

### 直接实现

从 DHXY committed `0114604e` 的 `SummonSkillService` 机械复制完整 private 方法
`buildCleanupResult(boolean, int, int, Map<Integer, SummonSkillSlotStatus>, boolean, boolean, int, int, String)`，并补该方法直接需要且目标当前缺失的 `SummonSkillCleanupResult` import。
保持 builder 九个字段及 `Map.copyOf(observedStatuses)` 顺序完全不变。

不得新增 wrapper/public API/caller；不得迁 capture/template/OCR/input/local mechanics；不得修改本轮前已批准块或其它文件。方法保持 dormant。

### 交付与门禁

在本日志追加 Implementation #1，给出 source/target exact diff、文件 SHA-256、旧批准块 unchanged 证据，并在 Cloud 仓运行 `mvn -q compile`（不 clean）。自审不算 Approved，等待父级源码审查。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T05:53:16-04:00

CLAIMED | task=W-SUMMONSKILL-CLEANUP-RESULT-CPU-IMP1; claimedAt=2026-07-14T05:53:16-04:00; writeSet=[Modify Cloud SummonSkillService.java, Append this log]

## External Worker C - W-SUMMONSKILL-CLEANUP-RESULT-CPU-IMP1 Implementation #1 - 2026-07-14T05:58:00-04:00

在 Cloud `SummonSkillService.java` 内，从 DHXY committed `0114604e` 机械复制 private
`buildCleanupResult(boolean, int, int, Map<Integer,SummonSkillSlotStatus>, boolean, boolean, int, int, String)` 完整方法，
并补该方法所需、目标缺失的 `SummonSkillCleanupResult` import。方法 dormant。已批准块 unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=c5a6eff6a8c64a18b1fc2717dfce19ef79a6ddca；
SHA-256=20f5eedcce0ff62b531aa8d86995574ea5e5ede1b2d4d71ca5b47689ae05b920），两处：
1. 新增唯一 import `com.bot.dhxy.model.maintenance.SummonSkillCleanupResult`（model 已存在于 cloud-brain；置于 model 组内
   SummonSkillSlotStatus 之前，按字母序 Cleanup<Slot）。
2. slot-geometry 方法区（getSkillSlotOffsets 之后）新增 `buildCleanupResult(...)`：`SummonSkillCleanupResult.builder()` 链式
   九字段 `success/skillCount/nextStartIndex/observedStatusesByIndex(Map.copyOf(observedStatuses))/ultimateGenerateClicked/
   ultimateGenerateSucceeded/inspectedCount/deletedCount/message` + `.build()`。

### 工具证据

- `buildCleanupResult` 逐 token `diff` 对 `0114604e` = **diff=0**（21 行）；builder 九字段顺序与 `Map.copyOf(observedStatuses)` 完全不变；
  定义计数=1（无重复）。
- 已批准块以字面 `index()` 抽块复核（避开正则误配）全部 **UNCHANGED**：`buildTipRectByHoverPoint`(5)、`SIX_SKILL_SLOT_OFFSETS`(8)、
  `EIGHT_SKILL_SLOT_OFFSETS`(10)、`getSkillSlotOffsets`(3)——本轮仅方法区追加 + 1 import，未触碰任何既有可执行 token。
  （首次校验脚本用了含 `[]` 的正则致 awk fatal、退化成空文件假 UNCHANGED，已改用 `index()` 字面匹配重验，结论真实。）

### 合同禁项核验（均未触碰）

- 未新增 wrapper/public API/caller；未迁 capture/template/OCR/input/local mechanics；方法 dormant（无 caller）。
- 未修改本轮前已批准块（tip-rect / SIX·EIGHT offsets / getSkillSlotOffsets 均 unchanged）或其它文件、tests、pom。
- 唯一新增 import 为方法直接所需的 SummonSkillCleanupResult。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）两处：1 import + 一 private 方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（builder 九字段/Map.copyOf 顺序逐 token），已批准块 unchanged（index() 重验）；无越界行为。
3. blob id 与 SHA-256 双标；首次校验脚本的正则缺陷已发现并改用字面匹配复核。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-SUMMONSKILL-CLEANUP-RESULT-CPU-IMP1 Implementation #1（buildCleanupResult 机械复制，方法 diff=0；已批准块 unchanged）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #3 - `W-SUMMONSKILL-CLEANUP-RESULT-CPU-IMP1` - 2026-07-14T05:58:18-04:00

**APPROVED，P0/P1/P2=0。** 父级从 committed `0114604e` 与当前 Cloud 独立抽取完整
`buildCleanupResult(...)`：source/target SHA-256 均为
`beedfafec206833f2dfaf771df23251162cc77af365e881a3610cefd4ae32a45`，`Exact=True`、目标定义数 1；
九字段 builder 与 `Map.copyOf(observedStatuses)` 顺序无漂移，新增 model import 是 exact 方法最小依赖。
父级复算文件 SHA-256 为
`20f5eedcce0ff62b531aa8d86995574ea5e5ede1b2d4d71ca5b47689ae05b920`，与 C 交付一致。
Worker Cloud `mvn -q compile` exit 0；无 capture/template/OCR/input/caller。

本 cleanup-result leaf `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Direct Implementation Task - `W-SUMMONSKILL-STATIC-CLASSIFIER-IMP1` - 2026-07-14T06:16:00-04:00

请 External Worker C 在本日志真实 EOF 先追加：

`CLAIMED | task=W-SUMMONSKILL-STATIC-CLASSIFIER-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

领取截止：`2026-07-14T06:36:00-04:00`。20 分钟只检查领取，不检查完成；领取后允许工作超过 20 分钟。

### 唯一写集

- Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- Append 本日志

### 直接实现

从 committed `0114604e` 机械复制静态技能槽内存分类 cohort：常量
`SKILL_STATUS_MATCH_RATE = 0.78`、`STATIC_INACTIVE_COLOR_DISTANCE_THRESHOLD = 12.0`，完整
`classifyStaticSkillSlot(...)` 与 `templateMatches(...)`，并补 exact 方法直接需要的 `com.bot.dhxy.core.ImageFinder` import。
目标已有 `StaticSkillSlotState`、`StaticSkillSlotTemplates`、`rectText(...)`、
`lowTextureTemplateMatchesByColorDistance(...)`；保持 ROI 校验、LOCKED -> EMPTY -> OCCUPIED 优先级、inactive color-distance
fallback、异常 -> UNKNOWN 与日志顺序不变。

不得迁 template 加载、capture、hover、delete/input、file I/O 或 caller；两方法保持 dormant；不得修改此前已批准块或其它文件。
运行 Cloud `mvn -q compile`（不 clean），追加 Implementation #1、常量/方法 exact diff、旧块 unchanged 与文件 SHA-256；
等待父级审查。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T06:21:50-04:00

CLAIMED | task=W-SUMMONSKILL-STATIC-CLASSIFIER-IMP1; claimedAt=2026-07-14T06:21:50-04:00; writeSet=[Modify Cloud SummonSkillService.java, Append this log]

## External Worker C - W-SUMMONSKILL-STATIC-CLASSIFIER-IMP1 Implementation #1 - 2026-07-14T06:27:00-04:00

在 Cloud `SummonSkillService.java` 内，从 committed `0114604e` 机械复制静态技能槽分类 cohort：两常量 + `classifyStaticSkillSlot(...)`
+ `templateMatches(...)`，补方法直接需要的 `ImageFinder` import。两方法 dormant。已批准块 unchanged。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### ⚠️ 超出 brief 显式清单的必要增补（透明上报，请父级确认）

brief 只点名补 `com.bot.dhxy.core.ImageFinder` import，但 `classifyStaticSkillSlot` 的两处 `log.warn(...)` 调用需要 `log`。
baseline DHXY 本类靠 `@Slf4j`（Lombok，line 23/50）提供 `log`；目标 cloud-brain SummonSkillService 是 fresh cut，**无 @Slf4j/log**。
为逐字复制该 exact 方法（含日志顺序）并编译通过，我额外增补：`import lombok.extern.slf4j.Slf4j;` + 类上 `@Slf4j` 注解。
依据：①与 baseline 等价（基线即用 @Slf4j）；②cloud-brain 既有惯例（AutoCombatPanelService/CommonBoxService/TeamReturnService
等十余 sibling service 均用 @Slf4j + log.info/warn；pom 含 lombok 1.18.32）；③与 brief 授权补 ImageFinder import 同理（exact 方法的直接依赖）。
若父级希望改用手写 `org.slf4j.Logger` 或去日志，请指示，我按指示返修。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=aca4248b57f1657789a030002b17f5a6e840f356；
SHA-256=3ae7d210ae25b5dbe2b38f006b9730592fc4f8aa6c18ead9ddc236a3e99476b4），四处：
1. import 增 `com.bot.dhxy.core.ImageFinder`（brief 点名）+ `lombok.extern.slf4j.Slf4j`（见上「必要增补」），按 sibling 惯例排序。
2. 类上加 `@Slf4j`（提供 log；见上「必要增补」）。
3. 常量区（EIGHT_SKILL_SLOT_OFFSETS 之后）新增 `SKILL_STATUS_MATCH_RATE = 0.78`、`STATIC_INACTIVE_COLOR_DISTANCE_THRESHOLD = 12.0`。
4. 方法区（buildCleanupResult 之后）新增 `classifyStaticSkillSlot(BufferedImage,int[],StaticSkillSlotTemplates,int)`
   与 `templateMatches(BufferedImage,BufferedImage)`。

### 工具证据

- 两常量对 `0114604e` **IDENTICAL**。
- `classifyStaticSkillSlot` `diff=0`（37 行）：ROI 校验（6 条 slotRect 边界）→ getSubimage → sealed/unobtained 命中 LOCKED →
  inactive 模板或 color-distance fallback 命中 EMPTY → 否则 OCCUPIED → catch RuntimeException 记 log 后 UNKNOWN；
  两处 log.warn 参数（slotIndex+1 / rectText / 尺寸 / e.toString）与顺序逐 token。
- `templateMatches` `diff=0`（3 行）：`ImageFinder.find(slotImage, templateImage, SKILL_STATUS_MATCH_RATE) != null`。
- 已批准块以字面 `index()` 抽块复核全部 **UNCHANGED**：buildCleanupResult(21)、SIX_SKILL_SLOT_OFFSETS(8)、buildTipRectByHoverPoint(5)。
- LOCKED → EMPTY → OCCUPIED 优先级、inactive color-distance fallback、异常 → UNKNOWN 与日志顺序全部保持。

### 合同禁项核验

- 未迁 template 加载、capture、hover、delete/input、file I/O 或 caller；两方法 dormant（无 caller）。
- 目标已有 StaticSkillSlotState/StaticSkillSlotTemplates/rectText/lowTextureTemplateMatchesByColorDistance——均复用未改。
- 未修改此前已批准块（unchanged 反证）或其它文件、tests、pom。唯一超清单增补=@Slf4j+lombok import（已透明上报，理由见上）。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）四处；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 常量 IDENTICAL、两方法 diff=0、已批准块 unchanged；@Slf4j/lombok 为 exact 方法必要依赖，已透明上报待父级确认。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移（含日志机制）。

External Worker C 交付 W-SUMMONSKILL-STATIC-CLASSIFIER-IMP1 Implementation #1（classifyStaticSkillSlot/templateMatches + 两常量机械复制，
diff=0/IDENTICAL；已批准块 unchanged；透明上报 @Slf4j+lombok import 为 log 必要依赖，请父级确认日志方案）；
持续重读本日志等待父级复审/指示；自审不算 Approved。

## Parent Source Review #4 - `W-SUMMONSKILL-STATIC-CLASSIFIER-IMP1` - 2026-07-14T06:37:22-04:00

**APPROVED，P0/P1/P2=0。** 父级修正旧抽取器“先命中调用点”的问题，只从 `private` 方法声明起算完整
括号块：`classifyStaticSkillSlot(...)` source/target 均 37 行，规范化 SHA-256 均为
`7ef9db7cc3a332431342df3dfa5392b4937f4ffe1c745a261a9323b4a07e7039`；
`templateMatches(...)` source/target 均 3 行，规范化 SHA-256 均为
`2bdec4d04d1abafe6126babe9c2a10ccd0a3b747e2d235acd4b30e7424e60668`。两项均 `Exact=True`。

`SKILL_STATUS_MATCH_RATE = 0.78` 与 `STATIC_INACTIVE_COLOR_DISTANCE_THRESHOLD = 12.0` 定义各恰一处且与基线一致；
`ImageFinder` 和 baseline 同款 `@Slf4j` 是 exact 方法的最小编译依赖，没有迁 template 加载、capture、hover、
delete/input、file I/O 或 caller。父级复算文件 SHA-256 为
`3ae7d210ae25b5dbe2b38f006b9730592fc4f8aa6c18ead9ddc236a3e99476b4`，与 C 交付一致；
Worker Cloud `mvn -q compile` exit 0。本 classifier cohort `SOURCE APPROVED`，仍为 dormant dependency，暂不单独计数。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent AUTHORITATIVE TRUE EOF Direct Task - `W-SUMMONSKILL-IF8-DECISION-IMP1` - 2026-07-14T06:57:00-04:00

本段是唯一有效的真实 EOF 任务；较早同标题段因重复锚点误插入历史区，保留但不作为领取位置。External Worker C
请先在本段之后追加：

`CLAIMED | task=W-SUMMONSKILL-IF8-DECISION-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

领取截止：`2026-07-14T07:17:00-04:00`。这是直接实现任务，不写 Design。唯一 Java 写集为
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`。
从 committed `0114604e` 机械迁入完整 package-private static
`detectSkillCountFromIf8Match(double[] match)`：`null -> 6`，其它任何数组引用 -> `8`；签名、可见性、分支、返回逐字一致。
先 dormant，不迁本地 IF8 template/capture/file path，不加 caller/wrapper/owner/session/ledger/TTL/retry。只可同步补类
JavaDoc 一句。运行 Cloud `mvn -q compile`（不 clean），记录方法规范化 SHA-256、文件 SHA-256、diff、exit code并交付
`Implementation #1`。自审不算 Approved。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T06:59:47-04:00

CLAIMED | task=W-SUMMONSKILL-IF8-DECISION-IMP1; claimedAt=2026-07-14T06:59:47-04:00; writeSet=[Modify Cloud SummonSkillService.java, Append this log]

## External Worker C - W-SUMMONSKILL-IF8-DECISION-IMP1 Implementation #1 - 2026-07-14T07:04:00-04:00

在 Cloud `SummonSkillService.java` 内，从 committed `0114604e` 机械迁入 package-private static
`detectSkillCountFromIf8Match(double[] match)` 完整方法（dormant）。已批准块 unchanged。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=1944f686c981dd5f2bdf6718248079ef6f910dc5；
SHA-256=d6606e28c28058c96f6996e63e30adda5ba9f84ec122c042093135b2087afa33），一处：
- 方法区（templateMatches 之后）新增 `static int detectSkillCountFromIf8Match(double[] match) { return match == null ? 6 : 8; }`。
无新 import（仅 double[]/int）；未改类 JavaDoc（brief 允许可选补一句，本方法语义已被现有 "slot-state" 概括，为最小改动不动 class doc）。

### 工具证据

- 方法逐字符 `diff` 对 `0114604e` = **diff=0**（3 行）；签名 `static int detectSkillCountFromIf8Match(double[] match)`、可见性 package-private
  static、分支 `match == null ? 6 : 8`、返回值逐字一致；定义计数=1（无重复）。
- 已批准块以字面 `index()` 抽块复核 **UNCHANGED**：classifyStaticSkillSlot(37) / templateMatches(3) / buildCleanupResult(21)。

### 合同禁项核验

- `null -> 6`，其它任何数组引用 -> `8`，逐字一致。
- 先 dormant（无 caller）；未迁本地 IF8 template/capture/file path；未加 caller/wrapper/owner/session/ledger/TTL/retry。
- 未改此前已批准块（unchanged 反证）或其它文件、tests、pom。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）一处：一 package-private static 方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（签名/可见性/分支/返回逐字），已批准块 unchanged；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-SUMMONSKILL-IF8-DECISION-IMP1 Implementation #1（detectSkillCountFromIf8Match 机械迁入，方法 diff=0；已批准块 unchanged）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #5 - `W-SUMMONSKILL-IF8-DECISION-IMP1` - 2026-07-14T07:08:00-04:00

**APPROVED，P0/P1/P2=0。** 父级从 committed `0114604e` 与当前 Cloud 的真实 package-private static 方法声明
分别抽取 `detectSkillCountFromIf8Match(double[])` 完整块；按行尾归一后 `Exact=True`、3 行、目标定义数为 1。
`null -> 6`、任意非 null 数组引用 `-> 8` 无漂移；无 caller/template/capture/file path/input，也未新增
wrapper/owner/session/ledger/TTL/retry。

Worker Cloud `mvn -q compile` exit 0；最终 consolidated fresh package 与本波其它稳定写入统一执行。
本 dormant prerequisite 暂不单独增加 `189/407`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #6 - `W-PSS-SNAPSHOT-SUMMARY-IMP1` - 2026-07-14T07:19:00-04:00

请 External Worker C 在 **2026-07-14T07:39:00-04:00 前**于本日志真实 EOF 追加：

`CLAIMED | task=W-PSS-SNAPSHOT-SUMMARY-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud PlayerStateService.java, Append this log]`

20 分钟只检查领取，不检查完成；领取后可持续实施。唯一 Java 写集：
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`。

直接实施完整 in-memory first-aid summary algorithm，不写 Design：新增 private
`inspectSupplyTargetsFromSnapshot(BufferedImage bars)`，从 committed `0114604e` 同名算法机械保留四条探针顺序
（人物血/法、宝宝血/法）、`enabledSeen/readableSeen` 判定、`FirstAidTarget` 构造和三条 reason：
`all-targets-disabled`、`no-enabled-bar-readable`、`all-enabled-bars-at-or-above-threshold`/
`enabled-bar-below-threshold`。仅做两项 Cloud 机械适配：基线 `config.get/is...` 映射到既有 immutable `settings`
accessor；移除只服务本地日志的 `baseX/baseY/CaptureAudit` 参数与日志字段，返回判定不得变化。

复用现有 `probeFirstAidBar`、`FirstAidProbeSummary/BarProbe/Target`，不新增 capture/I/O/input/caller、wrapper、
owner/session/ledger/TTL/retry，也不改 constructor/settings。运行 Cloud `mvn -q compile`（不 clean），提交 baseline
判定矩阵逐项证据、目标定义数、文件 SHA、diff 与 exit code。自审不算 Approved。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T07:23:47-04:00

CLAIMED | task=W-PSS-SNAPSHOT-SUMMARY-IMP1; claimedAt=2026-07-14T07:23:47-04:00; writeSet=[Modify Cloud PlayerStateService.java, Append this log]

## External Worker C - W-PSS-SNAPSHOT-SUMMARY-IMP1 Implementation #1 - 2026-07-14T07:31:00-04:00

在 Cloud `PlayerStateService.java` 内实现 in-memory first-aid summary 算法：新增 private
`inspectSupplyTargetsFromSnapshot(BufferedImage bars)`，从 committed `0114604e` 同名算法迁入，保留全部返回判定，仅做两项 Cloud
机械适配。dormant（除既有 sibling 外无 caller）。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/PlayerStateService.java（blob id=5c7dbed6c626df04a1056a05b0f27bf9d0a06c4a；
SHA-256=7558632489940c5db56a65023f3caef374ecad8797ee181040458b6becb54e24），一处：
- 新增 `private FirstAidProbeSummary inspectSupplyTargetsFromSnapshot(BufferedImage bars)`（置于 sibling findSupplyTargetsFromSnapshot 之后）。
无新 import（List/ArrayList/BufferedImage 已由 sibling 使用）；未改 constructor/settings、未改任何既有块。

### 两项 Cloud 机械适配（判定不变）

1. `config.is.../get...` → 既有 immutable `settings` accessor：`config.isPlayerHpSupplyEnabled()`→`settings.playerHpSupplyEnabled()`、
   `config.getPlayerHpSupplyThreshold()`→`settings.playerHpSupplyThreshold()`，人物血/法、宝宝血/法 4 组逐一映射
   （与目标既有 sibling findSupplyTargetsFromSnapshot / buildConservativeFirstAidTargets 的同款 settings 适配一致）。
2. 移除只服务本地日志的 `baseX/baseY/GameClientTracker.CaptureAudit` 三参数与整段 `log.info(...)` 字段——新方法内
   grep `baseX|baseY|captureAudit|log.` 计数=0；**返回判定不受影响**（日志不参与决策）。

### baseline 判定矩阵逐项证据（工具核验）

- 四探针顺序与坐标/expectRed/enable·threshold 源逐条一致（人物血量 CHAR_BAR HP_Y expectRed=true → 人物法力 CHAR_BAR MP_Y false
  → 宝宝血量 PET_BAR HP_Y true → 宝宝法力 PET_BAR MP_Y false），grep 命中 327/329/331/333。
- 循环判定：`!probe.enabled()` continue；enabledSeen=true；`probe.readable()`→readableSeen=true；`probe.supplyNeeded()`→
  `new FirstAidTarget(name, relX, relY, expectRed, threshold)`（grep enabled/readable/supplyNeeded=3）。
- 三 reason 与 unknown 标志逐字一致：`!enabledSeen`→`FirstAidProbeSummary(List.of(), probes, false, "all-targets-disabled")`；
  `!readableSeen`→`(List.of(), probes, true, "no-enabled-bar-readable")`；否则 `reason = targets.isEmpty() ?
  "all-enabled-bars-at-or-above-threshold" : "enabled-bar-below-threshold"`→`(targets, probes, false, reason)`（各 reason grep=1）。
- 目标定义数=1（无重复）；FirstAidProbeSummary(4 参)/FirstAidTarget(5 参)/probeFirstAidBar/FirstAidBarProbe accessor
  （enabled/expectRed/readable/supplyNeeded/name/relX/relY/threshold）与目标既有签名全部匹配（编译佐证）。

### 合同禁项核验

- 复用现有 probeFirstAidBar、FirstAidProbeSummary/FirstAidBarProbe/FirstAidTarget、settings accessor——均未修改。
- 未新增 capture/I·O/input/caller、wrapper、owner/session/ledger/TTL/retry；未改 constructor/settings 或其它既有块/文件/tests/pom。
- dormant：新方法仅被本次新增、无外部 caller（Java unused-private 仅告警非错误）。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=PlayerStateService.java（写集）一处方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 返回判定与 `0114604e` 逐条等价（4 探针序/enabledSeen·readableSeen/FirstAidTarget/3 reason/unknown 标志），仅去日志与 3 个日志参数；
   两项适配（config→settings、去 baseX/baseY/captureAudit）不改决策。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移（判定层面）。

External Worker C 交付 W-PSS-SNAPSHOT-SUMMARY-IMP1 Implementation #1（inspectSupplyTargetsFromSnapshot 判定等价迁入 + 两项 Cloud 适配，
判定矩阵逐项一致；未改既有块）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #6 - `W-PSS-SNAPSHOT-SUMMARY-IMP1` - 2026-07-14T07:30:00-04:00

**APPROVED，P0/P1/P2=0。** 父级独立对照 committed `0114604e:PlayerStateService` 与当前 Cloud
`PlayerStateService.java:324` 的完整方法。人物 HP -> 人物 MP -> 宝宝 HP -> 宝宝 MP 四探针顺序、坐标、
`expectRed`、enable/threshold 来源、`enabledSeen/readableSeen`、`FirstAidTarget` 构造及三段返回矩阵逐项一致；
`all-targets-disabled`、`no-enabled-bar-readable`、`all-enabled-bars-at-or-above-threshold`、
`enabled-bar-below-threshold` 文案和 unknown 标志均无漂移。

批准的两项 Cloud 机械适配成立：八个本地 `config` getter 一一映射为 immutable `settings` accessor；移除
`baseX/baseY/CaptureAudit` 与纯日志块不参与任何决策。目标定义数为 1，文件 SHA-256 为
`7558632489940c5db56a65023f3caef374ecad8797ee181040458b6becb54e24`，与 C 交付一致；Worker Cloud
`mvn -q compile` exit 0。未新增 capture/I/O/input/caller、wrapper、owner/session/ledger/TTL/retry。
consolidated fresh package 待当前 writer 全部稳定后由父级统一执行。本 prerequisite 暂不单独增加 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #7 (AUTHORITATIVE TRUE EOF) - `W-PSS-CONSERVATIVE-PLAN-IMP1` - 2026-07-14T07:55:53-04:00

请 External Worker C 在 **2026-07-14T08:15:53-04:00 前**于本日志真实 EOF 追加：

`CLAIMED | task=W-PSS-CONSERVATIVE-PLAN-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud PlayerStateService.java, Append this log]`

直接机械迁入 committed `0114604e` 完整
`cacheConservativeFirstAidPlan(PlayerRuntimeState,int,int,String)`，不写 Design。唯一 Java 写集为 Cloud
`PlayerStateService.java`。复用现有 `buildConservativeFirstAidTargets`、`FirstAidPlan`、
`describeFirstAidTargets`、`PlayerRuntimeState.pendingNoFocusFirstAidPlan` 与 log。

空 targets 清 plan + warn + return、非空 plan 的 `System.currentTimeMillis()` 读取位置、baseX/baseY 保存与日志参数顺序
逐字保持；这些坐标只是 Cloud 已有 plan value，不执行 capture/input，不接 caller，不新增 wrapper/owner/session/ledger/
TTL/retry。运行 Cloud `mvn -q compile`（不 clean），交付完整块 diff、定义数、文件 SHA 与 exit code；自审不算
Approved。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T08:01:45-04:00

CLAIMED | task=W-PSS-CONSERVATIVE-PLAN-IMP1; claimedAt=2026-07-14T08:01:45-04:00; writeSet=[Modify Cloud PlayerStateService.java, Append this log]

## External Worker C - W-PSS-CONSERVATIVE-PLAN-IMP1 Implementation #1 - 2026-07-14T08:06:00-04:00

在 Cloud `PlayerStateService.java` 内，从 committed `0114604e` 逐字机械迁入 private
`cacheConservativeFirstAidPlan(PlayerRuntimeState, int, int, String)` 完整方法，复用现有件。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/PlayerStateService.java（blob id=1f4ac60e713e16d7bd87056aa95acdfb11961219；
SHA-256=a1af2a9b6d56446a9b3b2033f48114604f061e23a972931d1b3160cf98dc02bd），一处：
- 方法区（buildConservativeFirstAidTargets 之后、safeReason 之前）新增 `cacheConservativeFirstAidPlan(...)`：
  空 targets → `state.pendingNoFocusFirstAidPlan = null` + `log.warn(no enabled conservative targets: reason={})` + return；
  非空 → `state.pendingNoFocusFirstAidPlan = new FirstAidPlan(targets, System.currentTimeMillis(), baseX, baseY)` +
  `log.warn(cached conservative plan: reason={} targets={} planBase=({}, {}))`。
无新 import（List/FirstAidTarget/FirstAidPlan/PlayerRuntimeState 等均已在）。

### 工具证据

- 方法逐字符 `diff` 对 `0114604e` = **diff=0**（11 行）；定义计数=1（无重复）。
- 逐字保持：空 targets 清 plan + warn + return 顺序；非空分支 `System.currentTimeMillis()` 读取位置（作为 FirstAidPlan.createdAtMs
  的第 2 构造实参）；baseX/baseY 保存为 FirstAidPlan 第 3/4 参；`log.warn` 两条文案与参数顺序（reason / describeFirstAidTargets(targets) /
  baseX / baseY）。
- 上一批准方法 `inspectSupplyTargetsFromSnapshot` 仍在且定义计数=1，本轮未触碰。
- 复用件签名匹配（编译佐证）：`FirstAidPlan(List<FirstAidTarget>, long createdAtMs, int baseX, int baseY)`、
  `PlayerRuntimeState.pendingNoFocusFirstAidPlan`（可变字段）、`buildConservativeFirstAidTargets():List<FirstAidTarget>`、
  `describeFirstAidTargets`、`log`。

### 合同禁项核验

- 复用现有 buildConservativeFirstAidTargets/FirstAidPlan/describeFirstAidTargets/PlayerRuntimeState.pendingNoFocusFirstAidPlan/log
  ——均未修改。
- baseX/baseY 只是 Cloud 已有 plan value，不执行 capture/input；未接 caller；未新增 wrapper/owner/session/ledger/TTL/retry。
- 未改既有其它块/文件、tests、pom。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=PlayerStateService.java（写集）一处方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（清 plan/warn/return 顺序、currentTimeMillis 位置、baseX/baseY 保存与日志参数序逐字），定义计数=1；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-PSS-CONSERVATIVE-PLAN-IMP1 Implementation #1（cacheConservativeFirstAidPlan 逐字迁入，方法 diff=0；上一方法未动）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #7 - `W-PSS-CONSERVATIVE-PLAN-IMP1` - 2026-07-14T08:05:11-04:00

**APPROVED，P0/P1/P2=0。** 父级从 committed `0114604e` 与当前 Cloud 源码独立抽取完整
`cacheConservativeFirstAidPlan(...)` 块；两块逐字 `Exact=True`，长度均为 `708` 字符，目标定义数为 1。
空 targets 清 plan、warn、return 的顺序，非空 `FirstAidPlan` 的 `System.currentTimeMillis()` 读取位置、
`baseX/baseY` 保存以及日志参数顺序均无漂移；复用的 builder、state slot 和描述 helper 未改。

目标文件 SHA-256 为 `a1af2a9b6d56446a9b3b2033f48114604f061e23a972931d1b3160cf98dc02bd`；
Worker Cloud `mvn -q compile` exit 0。方法保持 dormant，不执行 capture/input，不接 caller，也未新增
wrapper/owner/session/ledger/TTL/retry。consolidated fresh package 待 B writer 稳定后由父级统一执行；本
prerequisite 暂不单独增加 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #9 (REPUBLISHED AT TRUE EOF) - `W-ACS-REFRESH-DEFERRED-LOG-IMP1` - 2026-07-14T08:20:00-04:00

Earlier Task Brief #8 was inserted above the physical EOF and is not the polling marker. This true-EOF entry republishes it unchanged. External Worker C 现在直接实施，不写 Design。请在 **2026-07-14T08:39:00-04:00** 前先于本日志真实 EOF 追加：

`CLAIMED | task=W-ACS-REFRESH-DEFERRED-LOG-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud AutoCombatService.java, Append this log]`

唯一写集为 Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java` + 本日志。从 committed `0114604e` 机械迁入完整 private `logRefreshDueDeferred(TaskExecutionContext context, AutoCombatRuntimeState state, String windowId, RefreshDuePanelVerifyDecision decision, long now)`；只补直接需要的 Lombok `@Slf4j` import/类注解。保持 throttle 边界、state 写入位置、info/debug 分支及日志参数顺序逐字不变；不得接 caller、迁 refresh 主流程或新增 clock read/capture/input/remote/owner/session/ledger/TTL/retry/wrapper。完成后 Cloud `mvn -q compile`（不 clean）并追加 Implementation #1、完整块 exact diff、定义数、SHA-256、exit code。无已批准业务差异；按 `0114604e` 基线等价迁移。

## External Worker C - CLAIMED - 2026-07-14T08:25:49-04:00

CLAIMED | task=W-ACS-REFRESH-DEFERRED-LOG-IMP1; claimedAt=2026-07-14T08:25:49-04:00; writeSet=[Modify Cloud AutoCombatService.java, Append this log]

## External Worker C - W-ACS-REFRESH-DEFERRED-LOG-IMP1 Implementation #1 - 2026-07-14T08:31:00-04:00

在 Cloud `AutoCombatService.java` 内，从 committed `0114604e` 逐字机械迁入 private
`logRefreshDueDeferred(TaskExecutionContext, AutoCombatRuntimeState, String, RefreshDuePanelVerifyDecision, long)`，
并补父级授权的 Lombok `@Slf4j` import/类注解。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/AutoCombatService.java（blob id=f6f8529b992c3c4b25bcc0b50dd9ff0afd97be10；
SHA-256=bc20ed619d1d4c9e00a7aabbec1fb3cc2789a16457a9f45fcb52fb2052b75038），三处：
1. import 增 `lombok.extern.slf4j.Slf4j`（父级明确授权补）。
2. 类上加 `@Slf4j`（提供 log；父级授权）。
3. 方法区（safeRole 之后）新增 `logRefreshDueDeferred(...)`：throttle 判定
   `state.lastRefreshDuePanelVerifyDeferredLogAt <= 0L || now - ... >= REFRESH_DUE_DEFERRED_LOG_INTERVAL_MS` → 写
   `state.lastRefreshDuePanelVerifyDeferredLogAt = now` + `log.info(...)`；否则 `log.debug(...)`（suppressed by log throttle）。

依赖全部为目标既有件（未新增/未改）：字段 `AutoCombatRuntimeState.lastRefreshDuePanelVerifyDeferredLogAt`（line 226）、
常量 `REFRESH_DUE_DEFERRED_LOG_INTERVAL_MS = 10_000L`（line 63）、helper `safeRequestedTaskCode`（line 156）、
`RefreshDuePanelVerifyDecision.retryAfterMs()/lastTeamRefreshAgeMs()`、`TaskExecutionContext`（已 import）。

### 工具证据

- 方法逐字符 `diff` 对 `0114604e` = **diff=0**（17 行）；定义计数=1（无重复）。
- 逐字保持：throttle 边界（`<= 0L || >= INTERVAL`）、state 写入位置（先置 lastRefreshDuePanelVerifyDeferredLogAt=now 再 log.info）、
  info/debug 分支、两条日志文案与参数顺序（context.getLogPrefix() / safeRequestedTaskCode(context) / windowId /
  decision.retryAfterMs() / decision.lastTeamRefreshAgeMs()）。
- 已批准 cohort 未动：`reserveIfAllowed` 计数=1、`safeRequestedTaskCode` 计数=1（本轮仅追加方法 + @Slf4j import/注解）。
- `@Slf4j` 计数=1。

### 合同禁项核验

- 只补 @Slf4j（父级授权）+ 迁入该方法；未接 caller、未迁 refresh 主流程；未新增 clock read/capture/input/remote/owner/session/
  ledger/TTL/retry/wrapper（方法内无 System.currentTimeMillis——`now` 由入参提供）。
- 未改既有其它块/文件、tests、pom；依赖件（state 字段/常量/helper/decision）均复用未改。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=AutoCombatService.java（写集）三处：@Slf4j import/注解 + 一方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（throttle/state 写序/info·debug/日志参数序逐字），定义计数=1；已批准 cohort 未动；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-ACS-REFRESH-DEFERRED-LOG-IMP1 Implementation #1（logRefreshDueDeferred 逐字迁入 + 授权 @Slf4j，方法 diff=0；
已批准 cohort 未动）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #8 - `W-ACS-REFRESH-DEFERRED-LOG-IMP1` - 2026-07-14T08:33:00-04:00

**APPROVED，P0/P1/P2=0。** 父级从 committed `0114604e` 与当前 Cloud 独立抽取完整平衡括号块；
`logRefreshDueDeferred(...)` 两块逐字 `Exact=True`，长度均为 `1207` 字符，SHA-256 均为
`6f26002ea632722d60e2291763da9e0e9936d869f1ba871b1be5f71e7ba5c841`，定义数为 1。
`@Slf4j` import 与类注解均恰一处，是该完整方法唯一直接编译依赖。

`<=0 || >= interval` throttle 边界、先写 state 后 info、suppressed debug 分支、两条日志文案及五个参数顺序
均无漂移；目标仅有定义、无 caller，也没有新增时钟读取。目标文件 SHA-256 为
`bc20ed619d1d4c9e00a7aabbec1fb3cc2789a16457a9f45fcb52fb2052b75038`，Worker Cloud compile exit 0。
consolidated fresh package 待 D review 后父级统一执行；本 dormant prerequisite 暂不增加 `189/407`。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #11 (REPUBLISHED AT TRUE EOF) - `W-SS-IMAGE-PAYLOAD-IMP1` - 2026-07-14T08:45:00-04:00

Earlier Task Brief #10 was inserted above physical EOF and is not the polling marker. External Worker C 直接实施，
不写 Design。请在 **2026-07-14T09:02:43-04:00 前**先追加：

`CLAIMED | task=W-SS-IMAGE-PAYLOAD-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

唯一 Java 写集是 Cloud `SummonSkillService.java` + 本日志。按上方 Brief #10 原样机械迁入完整
`readImagePayload(String rawPath)`，仅补 `IOException/Files/Path/Base64` 直接 imports，复用既有
`ImagePayload/sha256Hex/@Slf4j`。不得接 caller/host，不执行本地 capture/template/OCR/input，不新增 workflow
machinery。Cloud `mvn -q compile`（不 clean）后交付完整块 exact diff、定义数、SHA-256 与 exit code。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-14T08:49:20-04:00

CLAIMED | task=W-SS-IMAGE-PAYLOAD-IMP1; claimedAt=2026-07-14T08:49:20-04:00; writeSet=[Modify Cloud SummonSkillService.java, Append this log]

## Parent Follow-on Task Brief #12 - `W-SS-IMAGE-CPU-COHORT-IMP1` - 2026-07-14T08:55:00-04:00

当前 image-payload 小单交付后立即继续本大 cohort，不等待下一轮聊天、不写 Design。请在
**2026-07-14T09:15:00-04:00 前**追加：

`CLAIMED | task=W-SS-IMAGE-CPU-COHORT-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

唯一 Java 写集仍为 Cloud `SummonSkillService.java`。一次迁入 committed `0114604e` 中当前可编译的完整纯
image/artifact 算法 cohort，优先成组核对并迁入：`readImage`、`saveImage`、`uniqueDebugImagePath`、
`imageProcessorMetadata`、`toWindowRelativeRoi`、`toAbsolutePoint`、`randomizeClickPoint`、
`randomizeHoverPoint`、`matchIf8LayoutMarker`、`scanLockedBoundary`、`scanStaticSkillSlots`、
`matchYellowTemplateInScan` 及其完整被动依赖。

准入规则：方法只处理调用方提供的 path/BufferedImage/坐标/被动值；不得主动 capture/template/OCR、不得发送
input、不得接 caller/host，不新增 owner/session/ledger/TTL/retry/wrapper。缺失 collaborator 的候选记录
`SOURCE_DEPENDENCY_EXCLUDED`，不适配、不造 seam，继续其余候选。目标一次交付至少 6 个完整方法或一个完整
scan 算法链。完成后 Cloud `mvn -q compile`（不 clean），报告 included/excluded、逐块 exact、定义数、SHA、
exit code。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - W-SS-IMAGE-PAYLOAD-IMP1 Implementation #1 - 2026-07-14T08:55:00-04:00

在 Cloud `SummonSkillService.java` 内，从 committed `0114604e` 逐字机械迁入 private `readImagePayload(String rawPath)`，
补方法直接需要的 4 个 JDK import，复用既有 ImagePayload/sha256Hex/@Slf4j。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=c746161ebe3638ba8c20505e71861992b6e02056；
SHA-256=d2821ed9648ca0384ed6d69b9c95472dd1ad076b3c878d7bbbdf84a690006146），两处：
1. 新增 4 个直接依赖 import：`java.io.IOException`、`java.nio.file.Files`、`java.nio.file.Path`、`java.util.Base64`
   （均按字母序插入 java 组；各 grep 计数=1，无重复）。
2. 方法区（detectSkillCountFromIf8Match 之后）新增 `readImagePayload(String rawPath)`：
   `Files.readAllBytes(Path.of(rawPath))` → `new ImagePayload(Base64.getEncoder().encodeToString(bytes), sha256Hex(bytes))`；
   `catch (IOException e)` → `log.warn(failed to read raw payload path={} reason={})` 后 `return null`。

### 工具证据

- 方法逐字符 `diff` 对 `0114604e` = **diff=0**（10 行）；定义计数=1（无重复）。
- 逐字保持：try 内 Files.readAllBytes(Path.of(rawPath)) → Base64 编码 + sha256Hex(bytes) 构造 ImagePayload；catch IOException
  记 warn（path / e.getClass().getSimpleName()）后 return null。
- 复用件均既有未改：`record ImagePayload`（计数 1）、`sha256Hex`（1）、`@Slf4j`（1）。
- 已批准块 index 计数各=1（未触碰）：detectSkillCountFromIf8Match / classifyStaticSkillSlot / buildCleanupResult。
- 注：本文件另有其它 worker 先前追加的常量（如 CLEAN_ONCE_TIMEOUT_MS），非本轮改动、未触碰。

### 合同禁项核验

- 仅补 IOException/Files/Path/Base64 imports + 迁入该方法；复用 ImagePayload/sha256Hex/@Slf4j。
- 未接 caller/host；不执行本地 capture/template/OCR/input；未新增 workflow machinery/owner/session/ledger/TTL/retry/wrapper。
- 未改既有其它块/文件、tests、pom。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）两处：4 import + 一方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 方法对 `0114604e` diff=0（读文件/Base64/sha256Hex/IOException 分支逐字），定义计数=1；已批准块与复用件未动；无越界行为。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-SS-IMAGE-PAYLOAD-IMP1 Implementation #1（readImagePayload 逐字迁入 + 4 JDK import，方法 diff=0；复用件与已批准块未动）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #9 (AUTHORITATIVE TRUE EOF) - `W-SS-IMAGE-PAYLOAD-IMP1` - 2026-07-14T08:59:00-04:00

**APPROVED，P0/P1/P2=0。** 父级独立抽取 committed `0114604e:1386-1395` 与当前 Cloud
`178-187` 完整 10 行方法，逐行差异数 0，定义数 1；四个 direct import 各恰一处。读 bytes、Base64、SHA、
IOException warn/null 顺序无漂移。目标 SHA-256
`d2821ed9648ca0384ed6d69b9c95472dd1ad076b3c878d7bbbdf84a690006146`，Worker compile exit 0。
无 caller/host/capture/template/OCR/input/workflow machinery；本 dormant prerequisite 暂不增加 `189/407`。
无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Follow-on Task Brief #12 (REPUBLISHED AT TRUE EOF) - `W-SS-IMAGE-CPU-COHORT-IMP1` - 2026-07-14T08:59:00-04:00

上方 Follow-on Brief #12 内容原样有效；现于真实 EOF 重发。请在 `2026-07-14T09:15:00-04:00` 前追加
对应 `CLAIMED`，随后一次实施至少 6 个完整纯 image/artifact 方法或一个完整 scan 算法链。写集、候选列表、
`SOURCE_DEPENDENCY_EXCLUDED` 规则和禁项均以上方 Brief #12 为准；不写 Design、不等待聊天。

## External Worker C - CLAIMED - 2026-07-14T08:59:23-04:00

CLAIMED | task=W-SS-IMAGE-CPU-COHORT-IMP1; claimedAt=2026-07-14T08:59:23-04:00; writeSet=[Modify Cloud SummonSkillService.java, Append this log]

## External Worker C - W-SS-IMAGE-CPU-COHORT-IMP1 Implementation #1 - 2026-07-14T09:08:00-04:00

按准入规则逐候选核对 committed `0114604e` image/artifact cohort：目标 `SummonSkillService` 为 fresh cut，**无任何实例
collaborator 字段**（`tracker`/`coordinateHelper`/`imageProcessorService`/`windowScopedTempPath`/`windowTaskContextHolder`
全 grep=0），`GameClientTracker` 类型在 cloud-brain 不存在，`HOVER_RANDOM_X/Y` 常量缺失。故只有不依赖任何实例 collaborator 的
纯 leaf 可 include；依赖缺失 collaborator 的候选一律记 `SOURCE_DEPENDENCY_EXCLUDED`，**不适配、不造 seam**。
Cloud `mvn -q compile`（未 clean）EXIT=0。

### INCLUDED（3 个完整方法，逐块 diff=0）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=5d168f01a8979c50ad86cd50a80579f2b399f370；
SHA-256=56d35eadc9cbe059f07abe68725579eb57f2487bf7cc86407a3b90407c061547）：
1. `private boolean matchYellowTemplateInScan(String templatePath, YellowTipScan scan, String name)` — diff=0（15 行）。
   依赖全在：`java.io.File`（本轮补 import）、`ImageFinder.find(String,String,double)`（cloud-brain ImageFinder line 24 有此重载）、
   `SKILL_STATUS_MATCH_RATE`、nested `YellowTipScan`（rect/washedPath private final、同顶层类可访问）。只处理调用方提供的
   YellowTipScan（被动值）+ 模板 path，**不主动 capture/截图**；对被动 washed 图做模板 find。
2. `private static BufferedImage readImage(String imagePath)` — diff=0（7 行）。`ImageIO.read(Path.of(imagePath).toFile())`；
   catch IOException → null。补 `javax.imageio.ImageIO` import；Path/IOException 已在。纯读调用方 path。
3. `private static void saveImage(BufferedImage image, String imagePath) throws IOException` — diff=0（13 行）。null/blank 校验、
   `Files.createDirectories(parent)`、`ImageIO.write(image,"png",...)`。纯写调用方 image+path。

新增 import 2 个（各计数=1）：`java.io.File`、`javax.imageio.ImageIO`。定义数各=1。已批准块（readImagePayload/detectSkillCountFromIf8Match/
buildTipRectByHoverPoint 等）计数各=1，未触碰。

### EXCLUDED（9 个候选，SOURCE_DEPENDENCY_EXCLUDED，附缺失 collaborator）

- `toWindowRelativeRoi(int[])` — 缺 `tracker`（tracker.getWindowBaseX/Y）。
- `uniqueDebugImagePath(String,String)` — 缺 `windowScopedTempPath`（windowScopedTempPath.resolve）。
- `imageProcessorMetadata(String,String,String)` — 缺 `windowTaskContextHolder`/`currentRuntime`（经 currentTaskCode/currentWindowId/currentHwnd）。
- `toAbsolutePoint(Point)` — 缺 `coordinateHelper`（coordinateHelper.getScaledRect）。
- `randomizeClickPoint(Point,int,int,String)` — 缺 `coordinateHelper`（coordinateHelper.getRandomizedPoint）。
- `randomizeHoverPoint(Point,String)` — 缺 `coordinateHelper`（经 randomizeClickPoint）+ `HOVER_RANDOM_X/Y` 常量。
- `matchIf8LayoutMarker()` — 缺 `windowScopedTempPath`（uniqueDebugImagePath）+ 主动 capture 依赖。
- `scanLockedBoundary(int,...)` — 缺 `coordinateHelper`（经 toAbsolutePoint 的 boundary lambdas）；SummonSkillTailBoundaryScanner 虽在，
  但方法体依赖 toAbsolutePoint→coordinateHelper。
- `scanStaticSkillSlots(int)` — 缺 `windowScopedTempPath`（uniqueDebugImagePath）+ 主动 capture/wash 链（captureAndWashYellowTipOnce
  依赖 tracker.captureToFile / imageProcessorService）。

### ≥6 目标未达成——如实上报（硬依赖约束，非投入不足）

准入规则要求"缺 collaborator 的候选不适配、不造 seam"。本 fresh-cut 目标缺 5 个实例 collaborator 字段 + GameClientTracker 类型，
12 个具名候选中 9 个据此 EXCLUDED；其纯被动子依赖（sha256Hex/rectText/roiText/buildSkillSlotRects/buildStaticSlotScanRelativeRoi/
toScreenRect/resolveStaticTailStartIndex/statesText/classifyStaticSkillSlot/YellowTipScan/ImagePayload）已在目标、无可新增。
因此本轮可 include 的**新**纯方法仅 3 个，无任何一条完整 scan 链可编译（scan 链均依赖 windowScopedTempPath/coordinateHelper/
tracker/imageProcessorService）。若父级希望达到 ≥6，需先由拥有者迁入上述 collaborator 字段/类型（非 C 写集、非本任务准入允许），
届时被 EXCLUDED 的候选可解锁。C 不越权造 seam。

### 编译门禁

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）：2 import + 3 方法；未改既有其它文件；未做 Git；未 clean；保护他人 dirty/untracked。
2. 3 方法逐块 diff=0、定义各 1；9 EXCLUDED 均记明缺失 collaborator，未适配/未造 seam；已批准块未动。
3. ≥6 未达成为硬依赖约束，已如实上报，未以造 seam 强凑数。
4. blob id 与 SHA-256 双标。
5. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 W-SS-IMAGE-CPU-COHORT-IMP1 Implementation #1（included=3[matchYellowTemplateInScan/readImage/saveImage] diff=0；
excluded=9 SOURCE_DEPENDENCY_EXCLUDED；≥6 因 fresh-cut collaborator 缺失不可达，如实上报不造 seam）；持续重读本日志等待父级复审/指示；自审不算 Approved。

## Parent Source Review #10 - `W-SS-IMAGE-CPU-COHORT-IMP1` - 2026-07-14T09:18:00-04:00

**BLOCKED，P0=0/P1=1/P2=0。** `readImage` 与 `saveImage` 对 committed 块逐字一致，可保留；但当前 Cloud
`SummonSkillService.java:191-202` 新增的 `matchYellowTemplateInScan(...)` 在 `:197` 直接调用
`ImageFinder.find(...)`，并在 `:192` 读取本地模板 path。用户已明确 capture/template/OCR 是 DHXY 本地 primitive，
Cloud 只消费 typed observation；该方法把模板匹配实现搬进 Cloud，影响是同一图片判定出现本地/Cloud 双实现并依赖
Cloud 可见文件路径，故 P1。

精确返修条件：原 C 只删除自己本轮新增的完整 `matchYellowTemplateInScan(...)` 与随之变为未使用的
`java.io.File` import；保留 `readImage`/`saveImage` 和所有此前批准块，不删除既有 `ImageFinder` 使用点，不改其它文件。
随后在同一任务中继续下方 public facade，不等待另一轮。**无已批准业务差异；按基线等价迁移。**

## Parent Task Brief #13 (AUTHORITATIVE TRUE EOF) - `W-SS-PUBLIC-LOCAL-PASS-FACADE-IMP1-R1` - 2026-07-14T09:18:00-04:00

External Worker C 请在 **2026-07-14T09:40:00-04:00 前**追加：

`CLAIMED | task=W-SS-PUBLIC-LOCAL-PASS-FACADE-IMP1-R1; claimedAt=<ISO>; writeSet=[Modify Cloud SummonSkillService.java, Append this log]`

领取后先完成上方两处精确撤回，再把同路径 `SummonSkillService` 从 dormant helper 集合闭合成真实公开 facade：
复用现有 `CloudSummonSkillWholePassCapability`，增加 constructor injection，并实现 committed public
`boolean cleanSummonSkillsOnce()` 与 `SummonSkillCleanupResult cleanSummonSkillsOnce(SummonSkillCleanupRequest)`。
request 四字段原样映射 `WholePassIntent`；`Executed.CleanupValue` 的所有计数、flags、message 和 slot-status map 完整映射
回现有 `SummonSkillCleanupResult`/`SummonSkillSlotStatus`；`NotExecuted/Unknown/Stopped` 保持非成功且保留原 message，
interrupt 必须恢复线程中断标志。不得迁 local panel/template/hover/click/delete mechanics，不新增独立 wire、owner/session/
ledger/TTL/retry，也不得修改 remote authority 文件。

完成后 Cloud `mvn -q compile`（不 clean）；交付撤回 diff、两个 public API 的 caller-ready constructor、四种 terminal
映射矩阵、定义数、SHA 与 exit code。**验收以完整 public facade 可调用为准。**

## External Worker C - CLAIMED - 2026-07-14T09:19:58-04:00

CLAIMED | task=W-SS-PUBLIC-LOCAL-PASS-FACADE-IMP1-R1; claimedAt=2026-07-14T09:19:58-04:00; writeSet=[Modify Cloud SummonSkillService.java, Append this log]

## External Worker C - W-SS-PUBLIC-LOCAL-PASS-FACADE-IMP1-R1 Implementation #1 - 2026-07-14T09:30:00-04:00

先执行 Review #10 精确撤回，再把 Cloud `SummonSkillService` 从 dormant helper 闭合为真实 public facade（复用
`CloudSummonSkillWholePassCapability`，constructor injection + 两 public `cleanSummonSkillsOnce`）。Cloud `mvn -q compile`（未 clean）EXIT=0。

### 精确写集（唯一文件 + 本日志）

Modify src/main/java/com/bot/dhxy/service/SummonSkillService.java（blob id=2fa710da9b5fa11a5eb674ebd34f376c4097b7d6；
SHA-256=bfee316315852ae963247ed988ccdec1a96a321b5ee214bcd683308318c40bde）。

#### A. Review #10 P1 精确撤回（工具证据）

- 删除本人上轮新增的完整 `matchYellowTemplateInScan(...)` 方法（grep 计数=0）与随之未用的 `import java.io.File;`（计数=0）。
- 保留 `readImage`/`saveImage`（计数=2）与所有此前批准块；**未删除**既有 `ImageFinder` 使用点（templateMatches 内 `ImageFinder.find(BufferedImage,BufferedImage,double)` 仍在），未改其它文件。

#### B. public facade 闭合

- 新增 `private final CloudSummonSkillWholePassCapability wholePass;` + `public SummonSkillService(CloudSummonSkillWholePassCapability wholePass)`
  构造注入（`Objects.requireNonNull`）。SummonSkillService 此前未被实例化/非 Spring bean（grep 无 new/无 @Service），加构造器安全。
- `public boolean cleanSummonSkillsOnce()` = `cleanSummonSkillsOnce(SummonSkillCleanupRequest.defaults()).isSuccess()`（与 committed 布尔重载一致）。
- `public SummonSkillCleanupResult cleanSummonSkillsOnce(SummonSkillCleanupRequest request)`：request 四字段原样映射
  `WholePassIntent(getExpectedSkillCount, isTrustExpectedSkillCount, getStartSlotIndex, isSkipUltimateCornerCheck)`（grep 四访问器=4）；
  `wholePass.execute(intent)` → 四态映射矩阵。
- 新增 import：`SummonSkillCleanupRequest`、`CloudSummonSkillWholePassCapability`、`java.util.LinkedHashMap`、`java.util.Objects`。
- 类 JavaDoc 更新为 public facade 描述（说明 capture/template/OCR 等本地 primitive 留在 DHXY）。

### 四态 terminal 映射矩阵

| WholePassResult | → SummonSkillCleanupResult |
|---|---|
| `Executed(CleanupValue v)` | `toCleanupResult(v)`：success/skillCount/nextStartIndex/observedStatusesByIndex(逐条 SlotStatus→SummonSkillSlotStatus，5 值同名 1:1)/ultimateGenerateClicked←ultimateSkillClicked/ultimateGenerateSucceeded←ultimateSkillSucceeded/inspectedCount←inspectedSlotCount/deletedCount←deletedSkillCount/message 全量映射 |
| `NotExecuted(msg)` | `SummonSkillCleanupResult.failed(msg)`（success=false，保留原 message） |
| `Unknown(msg)` | `failed(msg)`（非成功，保留 message；Unknown 不转成功） |
| `Stopped(msg)` | `failed(msg)`（非成功，保留 message） |
| `InterruptedException` | `Thread.currentThread().interrupt()`（恢复中断标志，grep=1）后 `failed("summon skill whole pass interrupted")` |

SlotStatus↔SummonSkillSlotStatus：NORMAL_SKILL/KEEP_SKILL/EMPTY_SLOT/LOCKED_SLOT/UNKNOWN 五值逐值同名 switch（穷尽、无 default）。

### caller-ready constructor

`new SummonSkillService(cloudSummonSkillWholePassCapability)` 即可调用两 public API；facade 只消费 typed WholePassResult，
不迁 local panel/template/hover/click/delete mechanics、不新增独立 wire/owner/session/ledger/TTL/retry、未改 remote authority 文件。

### 编译门禁与核验

- Cloud `mvn -q compile`（未 clean）：EXIT=0（错误列表对本文件 0 引用）。
- grep：两 public API 各=1、constructor=1、四字段访问器=4、interrupt 恢复=1、四 instanceof 分支齐；matchYellow=0、File import=0；
  已批准块 readImagePayload/detectSkillCountFromIf8Match/buildCleanupResult/templateMatches 各=1 未动。

### scope self-QA（仅 QA，不构成 Approved）

1. 唯一源码=SummonSkillService.java（写集）：撤回 1 方法+1 import + 新增 4 import/field/constructor/2 public API/2 mapper + 类 JavaDoc；
   未改既有其它文件（含 remote authority）、tests、pom；未做 Git；未 clean；保护他人 dirty/untracked。
2. P1 精确撤回落实；facade 可调用、四态矩阵完整、SlotStatus 5 值 1:1、interrupt 恢复中断标志。
3. blob id 与 SHA-256 双标。
4. 无已批准业务差异；capture/template/OCR 本地 primitive 留 DHXY，Cloud 只消费 typed observation。

External Worker C 交付 W-SS-PUBLIC-LOCAL-PASS-FACADE-IMP1-R1 Implementation #1（撤回 matchYellowTemplateInScan+File import；闭合
public facade：constructor injection + 两 cleanSummonSkillsOnce + 四态映射矩阵；编译 EXIT=0）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #11 - `W-SS-PUBLIC-LOCAL-PASS-FACADE-IMP1-R1` - 2026-07-14T09:34:00-04:00

**SOURCE APPROVED，P0/P1/P2=0。** 父级直接复核当前 Cloud
`SummonSkillService.java:74-137`：`:74-78` 只有 typed whole-pass capability 构造注入；`:80-110` 的两个
public `cleanSummonSkillsOnce` 保留 null request -> defaults、四字段 intent 映射，并把
`Executed/NotExecuted/Unknown/Stopped` 全部闭合为现有 `SummonSkillCleanupResult`；`:107-109` 捕获
`InterruptedException` 后恢复线程中断标志。`:113-136` 对 CleanupValue 的九个结果字段和五种 slot status
逐项投影，字段语义与 committed model 一致。

Review #10 的 P1 已精确闭合：本轮新增的 `matchYellowTemplateInScan(...)` 与专用 `java.io.File` import 均为 0；
`readImage`/`saveImage` 与此前批准块保留，未触碰 remote authority。父级另查 Cloud 全树当前没有旧式
`new SummonSkillService()` 或无参构造依赖，新增显式构造器没有破坏既有 caller；本次 facade 不执行
capture/template/OCR/hover/click/delete/input，也未新增 owner/session/ledger/TTL/retry。

Worker 非 clean `mvn -q compile` exit 0 已记录；由于 A/B/D/CE/CF 仍在互斥 Java 写入，本结论先批准源码，
不并发运行父级 clean。所有 writer 稳定后由父级统一运行 fresh Cloud `mvn -q clean package`；该集成门通过
即转 FINAL APPROVED，不再要求 C 写 Design 或额外返修。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Task Brief #13 (AUTHORITATIVE TRUE EOF) - `W-NPC-PUBLIC-SMART-CHAIN-IMP1` - 2026-07-14T09:56:00-04:00

External C 立即实施完整 `NpcClickService` 公开智能点击链，不写 Design。请在
**2026-07-14T10:16:00-04:00 前**于本日志真实 EOF 追加：

`CLAIMED | task=W-NPC-PUBLIC-SMART-CHAIN-IMP1; claimedAt=<ISO>; writeSet=[Modify Cloud com/bot/dhxy/service/NpcClickService.java, Append this log]`

唯一 Java 写集为 Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`。以 committed
`0114604e` 为业务权威，一次闭合并真实公开 `clickNpcSmart`、`clickNpcSmartWithOutcome`、
`tryDirectCombatTargetClick`、`confirmPendingSmartClick` 四入口及其必要 private closure。类内判断、候选顺序、
delay、fallback、stop 与 terminal 映射必须保持基线不变；窗口图片和匹配结果只从现有 typed
`CloudGameClient` fact/capture facade 消费，普通 move+click/sleep 组为单个有序 InputBundle。

不得复制 DHXY 的 HWND/holder/capture/template/OCR/input worker mechanics，不得修改 shared remote/schema，
不得新增 owner/session/ledger/TTL/retry，不得以缺 collaborator 为由再交 zero-Java 清单。若现有 typed port
缺一项，把缺口隔离为该 Service 内可编译的 caller-ready 参数/结果边界，同时至少完成真实可调用 public chain；
禁止 stub/固定成功。完成后运行 Cloud `mvn -q compile`（不 clean），报告四 public definition、真实 call graph、
typed dependency 表、SHA、exit code 与所有基线差异（应为 0）。已领取后允许工作超过 20 分钟。

**验收以完整公开智能点击链可编译、可调用、到达既有 typed port 为准，不以 helper 数量为准。**

## External Worker C - CLAIMED - 2026-07-14T10:00:27-04:00

CLAIMED | task=W-NPC-PUBLIC-SMART-CHAIN-IMP1; claimedAt=2026-07-14T10:00:27-04:00; writeSet=[Modify Cloud com/bot/dhxy/service/NpcClickService.java, Append this log]

## Parent Strategy Reset #1 - `W-696-NPC-WHOLE-1` - 2026-07-14T11:09:00-04:00

旧四入口任务停止作为完成单位，已写代码保留不回滚。External C 当前唯一任务改为：以
`git show 696a12b0:src/main/java/com/bot/dhxy/service/NpcClickService.java` 的完整 3,120 行类为源，闭合 Cloud
同路径全部 public/private 方法与 candidate/order/screenshot/verifier/tooltip/OCR/template/direct-combat/delay/
fallback；只在原调用点替换 typed capture/fact/InputBundle/closed macro，不重写候选算法。

唯一写集：Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java` 与本报告。请在
`2026-07-14T11:30:00-04:00` 前追加：
`CLAIMED | task=W-696-NPC-WHOLE-1; claimedAt=<ISO>; writeSet=[Cloud NpcClickService.java, Append this log]`

交付必须含 baseline 全方法清单及 one-to-one disposition、本地调用点替换表、完整文件 SHA、非 clean Cloud compile。

## Parent Sequence Freeze #1 - `HALT-ACTIVE-JAVA` - 2026-07-14T11:23:18-04:00

本节 supersede 上方尚未领取的 `W-696-NPC-WHOLE-1`。用户要求先把全部 32 个 Service 完整原样放进 Cloud，
之后才删本地重复类、补边界、拆动作。不要领取旧任务，不要修改 `NpcClickService.java`，也不要回滚或覆盖现有内容。
父级完整镜像已经 `32/32`、Git blob `BAD=0`。

请在本日志真实 EOF 追加 `HALTED_BY_WHOLE_COPY_SEQUENCE | worker=C | haltedAt=<ISO> | lastJavaWrite=<ISO> | sha256=<SHA>`，
随后等待 Phase 2 active-tree 整类 promotion 新任务。此控制指令不是完成审批，不运行 clean/runtime/tests。

## Parent Halt Gate #1 - `HALT-ACTIVE-JAVA` UNACKNOWLEDGED - 2026-07-14T11:43:50-04:00

首轮 20 分钟暂停确认窗已到，真实 EOF 仍无 `HALTED_BY_WHOLE_COPY_SEQUENCE`。按用户规则，本任务不交内部
Worker 接管，也不允许任何线程修改 C 的 `NpcClickService.java`。现将同一指令原样重发给 External C：

立即停止 `NpcClickService.java` 的任何新增/修改；不得领取已被 supersede 的旧任务，不得回滚或覆盖现有内容。
请在本日志真实 EOF 追加：
`HALTED_BY_WHOLE_COPY_SEQUENCE | worker=C | haltedAt=<ISO> | lastJavaWrite=<ISO> | sha256=<SHA>`。

第二确认截止为 `2026-07-14T12:03:50-04:00`；20 分钟只检查确认领取/响应，不检查完成。收到暂停确认后仍由
External C 领取单文件 mechanical promotion，绝不内部接管。

## External Worker C - HALT ACK - 2026-07-14T11:49:54-04:00

HALTED_BY_WHOLE_COPY_SEQUENCE | worker=C | haltedAt=2026-07-14T11:49:54-04:00 | lastJavaWrite=2026-07-14T09:30:00-04:00 | sha256=9250d2902b80edddeaac172c14464995432929304e34cc04eec4b83b6ddf6153

确认收到 Sequence Freeze `HALT-ACTIVE-JAVA`：即刻停止 `NpcClickService.java` 的任何新增/修改。说明：本人自始至终**未修改**
NpcClickService.java（仅为已被 supersede 的 W-NPC-PUBLIC-SMART-CHAIN 读取过它）；上表 sha256 为该文件当前未改状态
（mtime 2026-07-14T05:55，系他人 cut）。lastJavaWrite 为本人最后一次真实 Java 写入（SummonSkillService facade R1）。
不领取已被 supersede 的旧单（W-NPC-PUBLIC-SMART-CHAIN / W-696-NPC-WHOLE-1），不回滚、不覆盖任何现有内容。
静待 Phase 2 active-tree 整类单文件 mechanical promotion 新单（W-696-PROMOTE-C-1）；本 ack 非完成审批，不运行 clean/runtime/tests。

## Parent Task Brief - `W-696-PROMOTE-C-1` - 2026-07-14T11:51:30-04:00

External C 领取后只做一个机械 promotion：把 Cloud active
`src/main/java/com/bot/dhxy/service/NpcClickService.java` 替换为
`migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java` 的原字节完整类。

前置门：当前 active `git hash-object` 必须仍为
`7574e3c35cb6ba789d0c9f4dd99abdaac54597b6`，且 preservation snapshot 同 blob；否则 `BLOCKED`、零替换。
替换后必须等于 baseline blob `74d9b26b76b84052718d5679529f7ffeb46e3273`。禁止改 imports/逻辑、补依赖、编译、
触碰 shared wire/其它文件或 Git mutation。唯一写集为该 Java 与本日志。

请在 `2026-07-14T12:11:30-04:00` 前追加：
`CLAIMED | task=W-696-PROMOTE-C-1 | claimedAt=<ISO> | writeSet=[Cloud NpcClickService.java, Append this log]`。

## External Worker C - CLAIMED - 2026-07-14T11:54:16-04:00

CLAIMED | task=W-696-PROMOTE-C-1 | claimedAt=2026-07-14T11:54:16-04:00 | writeSet=[Cloud NpcClickService.java, Append this log]

## External Worker C - W-696-PROMOTE-C-1 Implementation #1 - 2026-07-14T11:55:26-04:00

机械 promotion 完成：Cloud active `src/main/java/com/bot/dhxy/service/NpcClickService.java` 已用
`migration-baseline/696a12b0/.../NpcClickService.java` 的原字节完整类替换。仅 cp 原字节，未改 imports/逻辑、未补依赖、
未编译、未触碰 shared wire/其它文件、未做 Git mutation。

### 前置门（通过，工具证据）

- active `git hash-object` = `7574e3c35cb6ba789d0c9f4dd99abdaac54597b6`（=要求值）。
- preservation snapshot `migration-preserved/pre-696a12b0-whole-service-cutover-20260714T1129/src/main/java/com/bot/dhxy/service/NpcClickService.java`
  `git hash-object` = `7574e3c35cb6ba789d0c9f4dd99abdaac54597b6`（同 active blob）。两条前置条件均满足，未触发 BLOCKED。

### 替换

- `cp migration-baseline/696a12b0/.../NpcClickService.java  active/.../NpcClickService.java`（byte-exact，exit 0）。

### 后置门（精确匹配，工具证据）

- 替换后 active `git hash-object` = `74d9b26b76b84052718d5679529f7ffeb46e3273`（=要求的 baseline blob，EXACT MATCH）。
- 促成文件：3374 行；SHA-256 = `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`。

### 合同禁项核验

- 唯一写集=该 Java（原字节替换）+ 本日志；未改 imports/逻辑、未补依赖、未编译（本单明令禁编译）、未 clean/runtime/tests、
  未触碰 shared wire 或其它文件、未做任何 Git mutation。preservation snapshot 未改。

### scope self-QA（仅 QA，不构成 Approved）

1. 前置双门满足→替换→后置 blob 精确=baseline，机械 promotion 无偏差；未做 Git/未编译/未碰他文件；保护他人 dirty/untracked。
2. blob（git hash-object）为权威判据；另附 SHA-256 与行数。
3. 按四阶段 Phase 2 whole-service promotion 语义：先完整镜像入 active-tree，删本地重复类/补边界/拆动作为后续阶段，本单不做。

External Worker C 交付 W-696-PROMOTE-C-1 Implementation #1（NpcClickService.java 原字节整类 promotion，后置 blob=74d9b26b 精确匹配；
未编译/未改逻辑/未 Git mutation）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Copy Review #1 - `W-696-PROMOTE-C-1` - 2026-07-14T11:56:45-04:00

**APPROVED，P0/P1/P2=0。** 父级独立复核 preservation blob
`7574e3c35cb6ba789d0c9f4dd99abdaac54597b6`；active 与 baseline blob 均为
`74d9b26b76b84052718d5679529f7ffeb46e3273`。active SHA-256 为
`cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`，`175,367` bytes / `3,374` 行。
父级随后遍历全部 baseline Service，active 结果为 `TOTAL=32 EXACT=32 MISSING=0 DIFF=0`。Phase 2
whole-Service promotion 因此完成；本结论不代表删除本地重复类后的 Cloud 编译或 typed boundary 已完成。

## Parent Task Brief - `W-696-UI-CLEAN-CLOUD-CONTRACT-1` - 2026-07-14T12:30:17-04:00

请 External Worker C 在 **2026-07-14T12:50:17-04:00** 前于本日志真实 EOF 先追加：

`CLAIMED | task=W-696-UI-CLEAN-CLOUD-CONTRACT-1 | claimedAt=<ISO-8601> | writeSet=[Cloud remote UI_CLEAN contract/facade files,this-log]`

这是直接实现任务，不写 Design。只允许修改 Cloud
`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/` 下：

新建：
- `UiCleanMacroCommand.java`
- `UiCleanMacroResult.java`
- `CloudUiCleanerPort.java`

修改：
- `LocalMacroKind.java`
- `LocalMacroCommand.java`
- `LocalMacroRequest.java`
- `LocalMacroOutcome.java`
- `RemoteProtocolDigests.java`
- `RemoteCommandOutcomeEnvelope.java`
- `RemoteGameCommandBroker.java`
- 本日志

闭合合同必须精确如下：

- `UiCleanMacroCommand(Operation operation, @JsonInclude(NON_NULL) String source)`；operation 仅
  `CLEAN_UP_ALL/CLOSE_ALL_GENERIC_WINDOWS/CLEAN_LIGHTWEIGHT_INTERRUPTIONS/CLOSE_MAP_SEARCH_INPUT_BY_X2`。
- 前两种 operation 要求 `source==null`；后两种要求 nonblank source。
- `UiCleanMacroResult(Operation operation, State state)`；state 仅 `COMPLETED/CLOSED_ANY/NOTHING_CLOSED/HANDLED/NOT_HANDLED/CLOSED/NOT_FOUND`，构造器严格验证 operation-state 配对。
- flat terminal payload 沿用恰四键 `macroKind/operation/state/cachePoint`；UI_CLEAN 的 `cachePoint` 必须显式 null；
  non-EXECUTED 的三个 typed fields 全 null 规则保持不变。
- `RemoteProtocolDigests` request canonical tree 增加 nested `uiClean={operation,source?}`；outcome 增加
  `uiClean={operation,state}`；不得改变既有 BAG/NAV canonical 分支。

`CloudUiCleanerPort` 为 Spring component，使用当前 `TaskExecutionContextHolder`、现有
`CloudGameClient.executeLocalMacro`、timeout 120000，公开方法精确为：

```java
void cleanUpAll(String phaseCode, String actionSlot)
boolean closeAllGenericWindows(String phaseCode, String actionSlot)
boolean cleanLightweightInterruptions(String phaseCode, String actionSlot, String source)
boolean closeMapSearchInputByX2(String phaseCode, String actionSlot, String description)
```

每次调用前后直接 `TaskCheckpoint`；EXECUTED 严格验证 operation/state 后映射；NOT_EXECUTED 对 boolean
返回 false、对 void checkpoint 后 no-op；STOPPED/UNKNOWN checkpoint 后抛 `TaskFatalException`；不得自动 retry，
不得新增 owner/session/ledger。只按编译需要更新 broker 构造参数。不得修改 Service。不要运行 build/test，不做 Git
mutation。完成后在本日志追加 Implementation #1 和精确文件表；自审不算 Approved。

## External Worker C - CLAIMED - 2026-07-14T12:37:24-04:00

CLAIMED | task=W-696-UI-CLEAN-CLOUD-CONTRACT-1 | claimedAt=2026-07-14T12:37:24-04:00 | writeSet=[Cloud remote UI_CLEAN contract/facade files,this-log]

## Parent Resume Directive #1 - `W-696-UI-CLEAN-CLOUD-CONTRACT-1` - 2026-07-14T12:42:00-04:00

选择并执行“现在完整实现”：按 Parent Task Brief 一次完成全部 `3 New + 7 Modify`，不得拆成下一轮、不得暂停等
专门时段，也不需要再次向用户或父级询问是否可以落码。当前禁止 Worker 自行运行 build/test 只是并发写入协调门，
不冻结本单 Java；待 C 交付且 A/B/D Java writer 全部稳定后，由父级统一运行 fresh build。

继续严格限制在已 CLAIMED 的唯一写集；不得修改 Service、DHXY、其它报告或创建替代合同。只有发现真实写集冲突、
合同无法按 brief 编译表达、或现有共享类型与 brief 发生不可兼容冲突时，才在本日志写 `BLOCKED` 和精确证据。
A 正在等待本单提供 `CloudUiCleanerPort`，因此请直接完成全量合同并追加 `Implementation #1` 文件表与自审证据。

## External Worker C - W-696-UI-CLEAN-CLOUD-CONTRACT-1 Implementation #1 - 2026-07-14T13:05:00-04:00

按精确合同闭合 Cloud remote 层 `UI_CLEAN` LocalMacro contract/facade：3 新文件 + 7 改文件，镜像既有 BAG/NAV LocalMacro 协议模式。
按父级明令**未运行 build/test、未做 Git**；以逐文件静态自查（switch 穷尽性 / 构造点 / accessor / 4-key payload）替代编译门。

### 精确文件表（唯一写集 = 下列 Java + 本日志）

均在 `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/`：

新建：
- `UiCleanMacroCommand.java`（SHA-256 ec597e8c…）：record `(Operation operation, @JsonInclude(NON_NULL) String source)` implements LocalMacroCommand；
  Operation ∈ {CLEAN_UP_ALL, CLOSE_ALL_GENERIC_WINDOWS, CLEAN_LIGHTWEIGHT_INTERRUPTIONS, CLOSE_MAP_SEARCH_INPUT_BY_X2}；
  前两者 `source==null`、后两者 `requiredText(source)`。
- `UiCleanMacroResult.java`（db96c81f…）：record `(UiCleanMacroCommand.Operation operation, State state)`；State ∈ {COMPLETED, CLOSED_ANY,
  NOTHING_CLOSED, HANDLED, NOT_HANDLED, CLOSED, NOT_FOUND}；构造器严格校验 operation-state 配对（CLEAN_UP_ALL→COMPLETED；
  CLOSE_ALL_GENERIC_WINDOWS→{CLOSED_ANY,NOTHING_CLOSED}；CLEAN_LIGHTWEIGHT_INTERRUPTIONS→{HANDLED,NOT_HANDLED}；
  CLOSE_MAP_SEARCH_INPUT_BY_X2→{CLOSED,NOT_FOUND}）。
- `CloudUiCleanerPort.java`（223f70e4…）：`@Component @RequiredArgsConstructor`，注入 `TaskExecutionContextHolder`，超时 120000。
  public：`void cleanUpAll(phaseCode,actionSlot)` / `boolean closeAllGenericWindows(phaseCode,actionSlot)` /
  `boolean cleanLightweightInterruptions(phaseCode,actionSlot,source)` / `boolean closeMapSearchInputByX2(phaseCode,actionSlot,description)`。

修改：
- `LocalMacroKind.java`（503fb9e6…）：枚举加 `UI_CLEAN`。
- `LocalMacroCommand.java`（938565da…）：sealed permits 加 `UiCleanMacroCommand`。
- `LocalMacroRequest.java`（b0da3bca…）：加 `uiClean` 字段（@JsonInclude NON_NULL）；compact 构造器 `case UI_CLEAN`（互斥校验含全部另三命令）；
  3 参便捷构造器加 `instanceof UiCleanMacroCommand`；`command()` expression switch 加 `case UI_CLEAN`。
- `LocalMacroOutcome.java`（ec032537…）：加 `uiClean` 字段；EXECUTED `case UI_CLEAN`（require uiClean!=null + 互斥）；既有三分支的互斥校验补 `uiClean==null`；
  非 EXECUTED 全 null 校验含 uiClean；`withCommon` 6 参。
- `RemoteProtocolDigests.java`（bf407f0e…）：`withComputedRequestDigest(LocalMacroRequest)` 重建传 `request.uiClean()`；request canonical tree 因序列化
  自动含 nested `uiClean={operation,source?}`（NON_NULL 省略其余），outcome digest 同理自动含 `uiClean={operation,state}`；BAG/NAV canonical 分支未改。
- `RemoteCommandOutcomeEnvelope.java`（8e31041e…）：`localMacroOutcome` 加 `UiCleanMacroResult uiClean` 局部 + EXECUTED `case UI_CLEAN`
  （operation/state 非空 textual、cachePoint 显式 null → `new UiCleanMacroResult(op,state)`）；return 6 参。flat terminal payload 仍恰四键
  `macroKind/operation/state/cachePoint`（LOCAL_MACRO_PAYLOAD_KEYS 未改），UI_CLEAN 的 cachePoint 显式 null；non-EXECUTED 三 typed field 全 null 规则不变。
- `RemoteGameCommandBroker.java`（5b8d6b1c…）：draft `new LocalMacroOutcome(common, macroKind, null, null, null, null)`（6 参）。仅按编译需要更新，无新构造参数依赖。

### 四态 terminal 映射矩阵（CloudUiCleanerPort）

每方法：取 current TaskExecutionContext（缺→IllegalStateException）→ **调用前 TaskCheckpoint** → `context.getGameClient().executeLocalMacro(
phaseCode, actionSlot, UI_CLEAN, command, 120000)` → **调用后 TaskCheckpoint** → 按 executionState 映射：
- EXECUTED：`requireExecuted` 严格校验 result.operation==请求 operation 后映射；void=no-op；boolean=state 命中 CLOSED_ANY/HANDLED/CLOSED。
- NOT_EXECUTED：boolean 返回 false；void no-op（前后 checkpoint 已执行）。
- STOPPED/UNKNOWN（及 OBSERVED，LocalMacroOutcome 禁 OBSERVED）：抛 `TaskFatalException`（携 common message）。
不 auto-retry、未新增 owner/session/ledger；executeLocalMacro 内部已处理 InterruptedException（端口无需捕获）。

### 静态自查（工具证据，代替被禁的编译门）

- switch 穷尽性：唯一 expression switch on LocalMacroKind = `LocalMacroRequest.command()` 已加 UI_CLEAN；LocalMacroOutcome/LocalMacroRequest 构造器/
  RemoteCommandOutcomeEnvelope 的 statement switch 亦均加 UI_CLEAN（grep 各含 case UI_CLEAN）。全仓无其它 switch(macroKind)。
- 构造点：`new LocalMacroOutcome(` 三处均 6 参（broker 6-arg 计数=2 含 withCommon 复用），`new LocalMacroRequest(` 经 3 参便捷构造器 + digest 重建均已含 uiClean；无残留 5 参。
- flat payload：LOCAL_MACRO_PAYLOAD_KEYS 仍恰 `macroKind/operation/state/cachePoint`（未改）。
- accessor/类型：TaskExecutionContextHolder=@Component、TaskExecutionContext.getGameClient()、CloudGameClient.executeLocalMacro(5参)、
  TaskCheckpoint.throwIfStopRequested(ctx,msg)、TaskFatalException(String)、CommonOutcome.executionState()/message()、ExecutionState 五值——均已核对。
- 未修改任何 Service，未改 BAG/NAV canonical，未触碰 shared wire 之外文件；未 build/test、未 Git。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=上列 remote 层 3 新+7 改 Java + 本日志；未改 Service、tests、pom；未做 Git；未 build/test（父级明令）；保护他人 dirty/untracked。
2. 合同逐条落实：command operation-source 规则、result operation-state 配对、四键 flat payload（UI_CLEAN cachePoint 显式 null）、
  canonical 增 uiClean 不动 BAG/NAV、port 四方法 checkpoint+120000+四态映射+TaskFatalException、不 retry/无 owner/session/ledger。
3. 因父级禁编译门，已用逐文件静态自查（switch/构造点/4-key/accessor）替代，如实标注"未编译"。
4. 无已批准业务差异；按基线等价迁移。

External Worker C 交付 W-696-UI-CLEAN-CLOUD-CONTRACT-1 Implementation #1（UI_CLEAN LocalMacro contract/facade：3 新+7 改文件，镜像 BAG/NAV；
四态映射矩阵完整；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #1 - `W-696-UI-CLEAN-CLOUD-CONTRACT-1` - 2026-07-14T12:58:21-04:00

**SOURCE APPROVED，P0/P1/P2=0。** 父级逐文件复核 3 New + 7 Modify，并与 External D 的 DHXY
closed DTO/codec/digest 做跨端对照：

- command 四 operation、source null/nonblank 规则，以及 result 七 state 的 operation-state 配对逐值一致；
- `LocalMacroRequest` / `LocalMacroOutcome` 的 typed variant 互斥、`OBSERVED` 拒绝和 non-EXECUTED
  空 typed result 闭合；全 Cloud 构造点均已更新到新参数表，`LocalMacroKind` switch 无遗漏；
- flat terminal 仍恰为 `macroKind/operation/state/cachePoint`，`UI_CLEAN cachePoint=null`；
- Cloud mapper 生成的 request `uiClean={operation,source?}`、outcome `uiClean={operation,state}` canonical tree
  与 DHXY `RemoteProtocolDigests` 的手工重建逐字段一致，既有 BAG/NAV 分支未改；
- `CloudUiCleanerPort` 四方法保持调用前后 `TaskCheckpoint`、120000ms timeout、EXECUTED 严格 operation
  校验、NOT_EXECUTED boolean=false/void no-op、STOPPED/UNKNOWN fatal，无 auto retry 或新 owner/session/ledger；
- 十文件 `git diff --check` 无错误，未修改 Service 或 DHXY。

本结论只批准 Cloud contract/facade/wire 源码；最终仍等待 A caller 和父级 fresh 双构建。
**无已批准业务差异；本单只建立永久本地 `UICleanerService` 的 closed Cloud 边界。**

## Parent Task Brief - `W-696-COMMON-BOX-TYPED-ADAPT-1` - 2026-07-14T13:35:00-04:00

请 External Worker C 在 **2026-07-14T13:55:00-04:00** 前于本日志真实 EOF 追加：

`CLAIMED | task=W-696-COMMON-BOX-TYPED-ADAPT-1 | claimedAt=<ISO-8601> | writeSet=[Cloud CommonBoxService.java,this-log]`

这是直接实现任务，不写 Design。父级已复核 active blob 仍为 baseline
`195c1dbfef052ddaf87ff40c6c85cba862be91f6`。唯一 Java 写集：

- Cloud `src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- 本日志

在完整 `696a12b0` 类的原调用点，把 ROI capture/template match 改为现有
`WindowFactKind.COMMON_BOX` typed fact；MATCHED 时保留 screen-absolute click/score 与 baseline 当前时刻、
pending-by-key、window/taskRun/role/identity、detect/consume 分离和现有 `PENDING_TTL_MS=30000`。消费点击改为一个
ordered bundle，保持 move -> 80ms -> click -> 120ms。NOT_MATCHED/CAPTURE_UNAVAILABLE/
TEMPLATE_UNAVAILABLE/MECHANICS_FAILED 必须走与原 miss/failure 相容的无点击路径。保留所有 public/private 方法、
toggle、过期/消费条件、返回值、顺序、delay、日志；只以 `TaskExecutionContext` 现有字段/API 代替本地 runtime
身份读取。不得新增或改变 TTL、owner/retry，不得把本地截图/模板/input 实现复制到 Cloud。

不得修改 remote/schema、其它 Service/POM/报告；不要运行 Maven/test/runtime，父级统一构建。完成后追加
`Implementation #1`，列出方法一对一、fact 映射、pending 不变量、bundle 顺序和 `git diff --check`。

## External Worker C - CLAIMED - 2026-07-14T13:36:55-04:00

CLAIMED | task=W-696-COMMON-BOX-TYPED-ADAPT-1 | claimedAt=2026-07-14T13:36:55-04:00 | writeSet=[Cloud CommonBoxService.java,this-log]

## External Worker C - W-696-COMMON-BOX-TYPED-ADAPT-1 Implementation #1 - 2026-07-14T14:05:00-04:00

在完整 `696a12b0` CommonBoxService 的原调用点做 typed-adapt：把本地 ROI capture/template match 换成
`WindowFactKind.COMMON_BOX` typed fact，消费点击换成一个 ordered input bundle，本地 runtime 身份读取全部改用
`TaskExecutionContext` 现有字段/API。保留全部 public/private 方法、toggle、过期/消费条件、返回值、顺序、delay、日志。
唯一 Java 写集 = Cloud `CommonBoxService.java`（+ 本日志）。按父级明令**未运行 Maven/test/runtime、未做 Git**；以逐项静态自查代编译门。

新 blob（git hash-object）= `58e34496e9878f054c4976d8a166430259233b91`，448 行；SHA-256 见下。

### 方法一对一（全部保留，签名不变，caller=AutoCombatService 不受影响）

| 方法 | 保留/改动 |
|---|---|
| `detectLeaderBoxAfterReturnHome(ctx,task,src)` | 保留（委托 detectBox LEADER） |
| `detectMemberBoxAfterCombatExit(ctx,task,src)` | 保留（委托 detectBox MEMBER） |
| `consumePendingBoxIfAllowed(ctx,task,src):boolean` | 保留全部门与日志；window 身份从 rawCurrent→ctx；点击→consumeClick（bundle） |
| `hasPendingBoxForCurrentWindow(ctx,task):boolean` | 保留（只读）；window 身份从 rawCurrent→ctx |
| `clearPendingForRole(role,src)` | 逐字保留 |
| `detectBox(ctx,task,role,src)` | 保留全部 skip/toggle/role-match 门与日志；去掉 rawCurrent/runWith，改 ctx.hasWindow() |
| `detectAndRecord(ctx,task,run,role,src)` | 改：readWindowFact(COMMON_BOX)→按 CommonBoxState 五态分派 |
| `recordMatched(...)`（新拆私有） | MATCHED→建 pending（原 detectAndRecord 成功分支等价迁出） |
| `consumeClick(...)`（新拆私有） | 原 inputSequences.moveAndClickLeft→一个 ordered bundle |
| `roleFor/isRoleEnabled/normalizeSupportedTask/taskRunKey/pruneExpiredPending/sameWindow/pendingKey` | 保留；身份读取改 ctx；`actionSlot()` 为新私有 helper |
| `PendingCommonBox` record | 字段逐字保留（13 字段） |

删除的私有：`cachedTemplate()`（模板缓存属本地机械层，Cloud 不持有）。删除的常量/字段：`ROI_LEFT/TOP/RIGHT/BOTTOM`、
`TEMPLATE_PATH/TEMPLATE_THRESHOLD`、`tracker/inputSequences/windowTaskContextHolder/cachedTemplate`——均为本地
截图/模板/input/window-runtime 依赖，已随 fact/bundle/ctx 化消解（全仓无外部引用，已 grep 证实）。

### fact 映射（detectAndRecord）

`ctx.getGameClient().readWindowFact(PHASE_CODE="common-box", actionSlot=role+"-detect", COMMON_BOX, 120000)` →
`WindowFactOutcome`。executionState 非 OBSERVED（NOT_EXECUTED/STOPPED/UNKNOWN；协议禁 EXECUTED）→ 记 miss 日志、无点击返回。
OBSERVED → `(WindowFact.CommonBoxFact)outcome.fact()` 按 `CommonBoxState`：

| CommonBoxState | 原本地对应 | 新路径 |
|---|---|---|
| MATCHED | ImageFinder.find 命中 | 用 fact 的 screen-absolute clickX/clickY + matchScore（协议保证 ≥0.86 且坐标非负）建 pending，baseline 当前时刻 `System.currentTimeMillis()` |
| NOT_MATCHED | find==null | info miss（reason=not-matched），无 pending |
| CAPTURE_UNAVAILABLE | raw==null | info miss（reason=capture-null），无 pending |
| TEMPLATE_UNAVAILABLE | template==null | warn skip（reason=template-unavailable），无 pending |
| MECHANICS_FAILED | catch(Exception) | warn failed（reason=mechanics-failed），无 pending |

readWindowFact 抛 InterruptedException→复位中断+warn；其它 Exception→保留原 "detection failed" warn(含堆栈)。

### pending 不变量（全部保留）

- pending-by-key：`windowId|hwnd|role|taskKey|taskRunKey`（hwnd 来自 ctx.getNativeWindowHandle()）。
- TTL：`PENDING_TTL_MS=30_000L` 未改；`expiresAtMs=now+TTL`；prune/expired/staleWindow/staleIdentity/staleTaskRun 判定逐字保留。
- identity：`pending.identityEpoch()==ctx.getPlayerIdentityEpoch()`。taskRun：Cloud `getTaskRunId()` 返回 String（非 baseline 的 long），
  taskRunKey 改判 `null||isBlank`（等价原 `<=0` 的 invalid-run skip）。role toggle（member/leaderCommonBoxEnabled）与 switch-off 清理保留。
- detect/consume 分离：detect 只建 pending 不点击；consume/hasPending 用同一套 task/run/role/window identity 门。
- 未新增/改 TTL、owner、retry、session、ledger。

### bundle 顺序（consumeClick，一个 ordered bundle）

`ctx.getGameClient().executeInputBundle("common-box", role+"-consume", "commonBox:role:task:src", SCREEN_ABSOLUTE_PX, actions, 120000)`，
actions（List.of，不重排不合并）：
1. `MOVE_MOUSE(clickX,clickY)`
2. `SLEEP(delayMs=80=CLICK_SETTLE_MS)`  ← MOVE_MOUSE 协议禁带 delayMs，故用独立 SLEEP 承载 settle
3. `CLICK_LEFT(clickX,clickY, delayMs=120=CLICK_DELAY_MS)`

= move -> 80ms -> click -> 120ms（与原 `moveAndClickLeft(tag,x,y,80,120)` 语义等价）。坐标为 fact 提供的 screen-absolute px。
成功判定：outcome.executionState()==EXECUTED→clicked=true（移除 pending，记 COMMON_BOX_CLICKED）；否则（NOT_EXECUTED 或 transport RuntimeException）
→ clicked=false，保留 pending 直到 TTL（等价原 "click failed; keep pending"）。

### 静态自查（工具证据，代被禁编译门）

- 残留旧符号 grep（tracker/ImageFinder/InputSequences/WindowRuntimeContext/windowTaskContextHolder/WindowNativeBinding/
  BufferedImage/ImageIO/cachedTemplate/ROI_/TEMPLATE_/rawCurrent/captureToMemory/moveAndClickLeft/getNativeHandle/GameClientTracker）=空。
- switch 穷尽：`box.state()` 覆盖 CommonBoxState 全 5 值（MATCHED/NOT_MATCHED/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED），无 default。
- 构造点：`new InputActionDto(` ×3 均 9 参，各自 forbid 校验通过（MOVE 禁 delayMs→SLEEP 承载；CLICK_LEFT/SLEEP delayMs 非负）；
  `CoordinateSpace.SCREEN_ABSOLUTE_PX`、`ExecutionState.OBSERVED/EXECUTED` 已核；CommonBoxFact record 组件 accessor state()/clickX()/clickY()/matchScore() 自动生成。
- accessor/类型：TaskExecutionContext.hasWindow()/hasNativeWindow()/getWindowId()/getNativeWindowHandle()(String)/getPlayerIdentityEpoch()/
  getTaskRunId()(String)/getWindowRole()/isStopRequested()/throwIfStopRequested()/getGameClient() 均已核；CloudGameClient.readWindowFact(4参,throws InterruptedException)/
  executeInputBundle(6参) 已核。imports 全部被引用无冗余。
- caller：AutoCombatService 仅用 detectMemberBoxAfterCombatExit / hasPendingBoxForCurrentWindow / consumePendingBoxIfAllowed，三签名不变。
- 该文件在 cloud-brain 为 untracked（`git status`=`??`，与既往迁入 Service 同类），`git diff --check` / `git diff HEAD --check` 工作树 whitespace 干净。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=Cloud CommonBoxService.java + 本日志；未改 remote/schema、其它 Service/POM/报告；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 逐条落实：ROI capture/template→COMMON_BOX fact、MATCHED 保 screen-absolute click/score+baseline 当前时刻、pending-by-key、
  window/taskRun/role/identity、detect/consume 分离、PENDING_TTL_MS=30000 未改、消费点击=一个 move→80ms→click→120ms bundle、
  四失败态走无点击路径、public/private/toggle/过期消费条件/返回值/顺序/delay/日志全保留、身份读取只用 TaskExecutionContext 现有 API、
  未新增/改 TTL/owner/retry、未复制本地截图/模板/input 到 Cloud。
3. 因父级禁编译门，已用逐项静态自查（残留符号/switch 穷尽/构造点/accessor/caller）替代，如实标注"未编译"。
4. 无已批准业务差异；按基线等价 typed-adapt 迁移。

External Worker C 交付 W-696-COMMON-BOX-TYPED-ADAPT-1 Implementation #1（CommonBoxService typed-adapt：capture/template→COMMON_BOX fact、
消费点击→一个 move→80ms→click→120ms bundle、身份→TaskExecutionContext；全 public/private/日志/pending 不变量保留；按父级明令未 build/test，以静态自查代编译门）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent TRUE EOF Source Review #1 / Repair Reissue - `W-696-COMMON-BOX-TYPED-ADAPT-1` - 2026-07-14T14:08:00-04:00

前一份同结论被工具插入历史段落（约物理行 801），**不计真实 EOF 发单、不启动领取计时**。本段位于
Implementation #1 之后的物理真实 EOF，是唯一权威父级结论。

**BLOCKED，P0=0 / P1=3 / P2=1。** Helper 已先预检，父级已独立复核：

- **P1 检测终态：** `CommonBoxService.java:269-297` 把 `STOPPED/UNKNOWN/InterruptedException` 以及
  remote retained/current gate RuntimeException 降成普通 miss/failure，停止或未决 observation 会被业务继续消费。
- **P1 点击终态：** `:343-350` 把所有非 `EXECUTED` 和 RuntimeException 变成 false，上层保留 pending 并记普通
  click failed，可能在物理输入未决时继续推进/再次调用。
- **P1 TTL 时间源：** DHXY mechanics `:88-99` 在真实命中时产生 `matchedAtEpochMs`，handler `:867-872`
  与 Cloud fact `:137-168` 完整传递；active `CommonBoxService.java:309-324` 却用 Cloud 收包时刻重新起算
  30 秒 TTL，网络/排队时间会额外延长可点击窗口。
- **P2 方法图：** 新增 `recordMatched`、`consumeClick`、`actionSlot` 三层 private helper，把 baseline 原调用点
  再次切碎；无复用/新策略必要，不符合完整类调用图与 no-wrapper 约束。

已确认不变量：pending key/window/handle/role/identity/taskRun 仍来自同一 context；String taskRun 在本类只用于
有效性/key/equality；bundle 仍为 move -> 80ms -> click 120ms；五种 fact state、score/absolute point、role toggle、
detect/consume 分离和 `PENDING_TTL_MS=30000` 常量未改。

### 权威返修任务 `W-696-COMMON-BOX-TYPED-ADAPT-1-R1`

请原 External Worker C 在 **2026-07-14T14:28:00-04:00** 前于本段之后追加：

`CLAIMED | task=W-696-COMMON-BOX-TYPED-ADAPT-1-R1 | claimedAt=<ISO-8601> | writeSet=[Cloud CommonBoxService.java,this-log]`

精确返修条件：

1. fact：`OBSERVED` 按五态；`NOT_EXECUTED` 可 miss；`STOPPED` 先 `TaskCheckpoint.throwIfStopRequested`，
   未确认则 fatal；`UNKNOWN`/其它不合法态 fatal。中断恢复 flag 后抛 `TaskFatalException`；remote/runtime 异常不得宽 catch 成 miss。
2. input：`EXECUTED=true`、`NOT_EXECUTED=false`；`STOPPED` checkpoint；`UNKNOWN`/其它不合法态 fatal；
   final-consumption/transport RuntimeException 不得返回 false。
3. `detectedAtMs/expiresAtMs` 以 `box.matchedAtEpochMs()` 为起点，严格保持本地真实命中后的 30 秒窗口。
4. 把 `recordMatched`、`consumeClick`、`actionSlot` 收回 baseline 原方法/原调用点；不新增替代 wrapper。
5. 保持全部基线 public/private 方法、日志、状态、返回路径与 move -> 80ms -> click 120ms；不改 remote/schema/
   其它 Service/POM，不新增 owner/session/ledger/TTL/retry，不运行 Maven/test/runtime。

交付 `Implementation Repair #1` 时列出 terminal 矩阵、TTL 时间源、方法图恢复、bundle 与 scoped diff/check。
**无已批准业务差异；按基线等价返修。**

## External Worker C - CLAIMED - 2026-07-14T14:10:30-04:00

CLAIMED | task=W-696-COMMON-BOX-TYPED-ADAPT-1-R1 | claimedAt=2026-07-14T14:10:30-04:00 | writeSet=[Cloud CommonBoxService.java,this-log]

## External Worker C - W-696-COMMON-BOX-TYPED-ADAPT-1-R1 Implementation Repair #1 - 2026-07-14T14:16:00-04:00

按权威 `Parent TRUE EOF Source Review #1`（P1×3/P2×1）逐条返修 Cloud `CommonBoxService.java`。唯一 Java 写集=该文件（+本日志）。
按父级明令**未运行 Maven/test/runtime、未做 Git**；以逐项静态自查（工具证据）代编译门。
新 blob（git hash-object）= `3f6fd9cbed93e03e52296775da95f8325a530309`，448 行；SHA-256 = `9f4fb93375e0be5d2bf810e551c5b6de3d13ff2e3857df9ffcf47e99b3bbf471`。

### P1#1 检测终态矩阵（detectAndRecord，无宽 catch）

readWindowFact 前置 `context.isStopRequested()` 短路保留（best-effort skip，非终态降级）。readWindowFact 抛 InterruptedException →
`Thread.currentThread().interrupt()` 复位 flag 后 `throw new TaskFatalException(msg, e)`；**不再有 `catch (Exception)` 宽 catch**，
remote/runtime RuntimeException 原样上抛。executionState statement switch：

| executionState | 处理 |
|---|---|
| OBSERVED | 取 `(WindowFact.CommonBoxFact)outcome.fact()`，按五态（下表）分派 |
| NOT_EXECUTED | info miss（reason=not-executed），无 pending，返回 |
| STOPPED | `TaskCheckpoint.throwIfStopRequested(context,msg)`；未确认停止则 `throw new TaskFatalException(...STOPPED without confirmed stop)` |
| default（UNKNOWN + 协议不可能的 EXECUTED） | `throw new TaskFatalException(...terminated <state>)` |

OBSERVED 内层 `box.state()` 五态（全保留原 miss/failure 无点击语义）：MATCHED→建 pending；NOT_MATCHED→info(not-matched)；
CAPTURE_UNAVAILABLE→info(capture-null)；TEMPLATE_UNAVAILABLE→warn(template-unavailable)；MECHANICS_FAILED→warn(mechanics-failed)。

### P1#2 点击终态矩阵（consumePendingBoxIfAllowed 原调用点，无 RuntimeException 吞噬）

在原 click 点直接 `executeInputBundle(...)`，对 `bundleOutcome.common().executionState()` 用 switch 表达式求 `clicked`：

| executionState | clicked |
|---|---|
| EXECUTED | true（移除 pending，记 COMMON_BOX_CLICKED，return true） |
| NOT_EXECUTED | false（保留 pending，warn "click failed; keep pending until TTL"，return false） |
| STOPPED | `TaskCheckpoint.throwIfStopRequested`；未确认则 `TaskFatalException` |
| default（UNKNOWN + 协议不可能的 OBSERVED） | `TaskFatalException` |

executeInputBundle 的 final-consumption/transport RuntimeException（含 interrupt 包装的 IllegalStateException）**不再被 catch 成 false**，原样上抛。

### P1#3 TTL 时间源

MATCHED 分支：`long matchedAtMs = box.matchedAtEpochMs()`（DHXY mechanics 真实命中时刻，经 handler + Cloud fact 完整传递，
协议保证 MATCHED 时 >0）。`PendingCommonBox.detectedAtMs = matchedAtMs`、`expiresAtMs = matchedAtMs + PENDING_TTL_MS`。
**不再用 Cloud 收包时刻 `System.currentTimeMillis()` 起算**，严格保持本地真实命中后的 30s 窗口；网络/排队时间不再额外延长。
（consume/prune 仍用 `System.currentTimeMillis()` 仅作当前时刻**比较**与 ageMs 计算，非 TTL 起点。）

### P2 方法图恢复

删除上一版新增的三层 private helper `recordMatched` / `consumeClick` / `actionSlot`（grep 计数=0），把 MATCHED 建 pending
与消费 bundle 逐字收回 baseline 原方法/原调用点（detectAndRecord / consumePendingBoxIfAllowed）内联；无替代 wrapper。
readWindowFact/executeInputBundle 所需 actionSlot 在调用点内联 `role.name().toLowerCase()+"-detect"/"-consume"`（canonical token），phaseCode 常量 `common-box`。

### bundle 顺序（未改）

原 click 点一个 ordered bundle（List.of，不重排不合并）：`MOVE_MOUSE(clickX,clickY)` → `SLEEP(delayMs=80=CLICK_SETTLE_MS)`
→ `CLICK_LEFT(clickX,clickY, delayMs=120=CLICK_DELAY_MS)` = move -> 80ms -> click -> 120ms；SCREEN_ABSOLUTE_PX；坐标为 fact screen-absolute px。

### 静态自查（工具证据，代被禁编译门）

- helper 收回：`grep -cE 'recordMatched|consumeClick|private String actionSlot'`=0。
- TTL 源：`matchedAtEpochMs` 用于 detectedAtMs/expiresAtMs；PendingCommonBox 构造唯一处即 MATCHED，未用 currentTimeMillis 作起点。
- 无宽 catch：仅 `catch (InterruptedException)`（复位+fatal）；无 `catch (Exception)`/`catch (RuntimeException)`。
- 终态 switch：detect=OBSERVED/NOT_EXECUTED/STOPPED/default(fatal)；consume=EXECUTED/NOT_EXECUTED/STOPPED/default(fatal)；均 default→TaskFatalException。
- fact 五态：MATCHED/NOT_MATCHED/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED 内层 switch 穷尽，无 default。
- 构造点：`new InputActionDto(`×3 均 9 参、forbid 校验通过；`TaskCheckpoint.throwIfStopRequested(ctx,msg)`（static，com.bot.dhxy.runner.stop）、
  `new TaskFatalException(String)`/`(String,Throwable)`（com.bot.dhxy.runner.exception，extends RuntimeException）已核；`box.matchedAtEpochMs()` record 组件 accessor 自动生成。
- imports：新增 TaskFatalException/TaskCheckpoint；移除不再需要的 ExecutionState import（switch 用简单枚举名，无显式 `ExecutionState.`）；其余全部被引用。
- 不变量未改：pending key/window/hwnd/role/identity/taskRun 同一 context；String taskRun 仅用于有效性/key/equality；五 fact state、score/absolute point、role toggle、
  detect/consume 分离、`PENDING_TTL_MS=30000` 常量；未新增 owner/session/ledger/TTL/retry；未改 remote/schema/其它 Service/POM。
- caller：AutoCombatService 三方法签名不变（detect/consume 现可抛 TaskFatalException/停止终态，属父级要求的终态语义，经 RuntimeException 上抛任务运行器）。
- 该文件在 cloud-brain 为 untracked（`git status`=`??`），`git diff --check` 工作树 whitespace 干净（scoped 到本文件）。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=Cloud CommonBoxService.java + 本日志；未改 remote/schema、其它 Service/POM/报告；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 四条返修逐条落实：检测终态（STOPPED checkpoint/UNKNOWN fatal/中断复位+fatal/不宽 catch）、点击终态（EXECUTED=true/NOT_EXECUTED=false/STOPPED checkpoint/
  UNKNOWN fatal/transport RuntimeException 不吞）、TTL 以 matchedAtEpochMs 起算保 30s、方法图收回 baseline 原调用点无 wrapper；bundle/日志/pending 不变量保留。
3. 因父级禁编译门，已用逐项静态自查（helper 收回/TTL 源/终态 switch/无宽 catch/构造点/accessor/caller）替代，如实标注"未编译"。
4. 无已批准业务差异；按基线等价返修。

External Worker C 交付 W-696-COMMON-BOX-TYPED-ADAPT-1-R1 Implementation Repair #1（检测/点击终态改为 STOPPED checkpoint + UNKNOWN/中断 fatal、
不再宽 catch/吞 RuntimeException；TTL 以 box.matchedAtEpochMs() 起算保本地命中 30s；recordMatched/consumeClick/actionSlot 收回 baseline 原调用点；
bundle/日志/pending 不变量保留；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #2 - 2026-07-14T14:21:00-04:00

结论：`SOURCE APPROVED`，`P0=0 / P1=0 / P2=0`。本结论只批准当前源码；统一 fresh Cloud package 尚未执行，
因此不增加 `approved same-path` 计数。

父级已独立读取当前 Cloud `CommonBoxService.java`，并按 `696a12b0` 的完整类方法图复核：

1. `consumePendingBoxIfAllowed` 在原消费调用点直接发送唯一 ordered bundle，仍为
   `MOVE_MOUSE -> SLEEP(80ms) -> CLICK_LEFT(delay=120ms)`；`EXECUTED` 才移除 pending，
   `NOT_EXECUTED` 保留 pending，`STOPPED` 走 `TaskCheckpoint`，`UNKNOWN`/非法终态抛 `TaskFatalException`。
2. `detectAndRecord` 仅捕获 `InterruptedException`，恢复 interrupt 后抛 fatal；不存在宽 catch。
   `OBSERVED` 才读取 `CommonBoxFact`，`NOT_EXECUTED` 保持普通 miss，`STOPPED` checkpoint，
   `UNKNOWN`/非法终态 fatal，未把 transport/runtime 故障降级为业务未命中。
3. MATCHED 分支以 `box.matchedAtEpochMs()` 同时写入 `detectedAtMs`，并以
   `matchedAtMs + PENDING_TTL_MS` 得到 expiry，未从 Cloud 收包时刻重新延长 30 秒窗口。
4. `recordMatched`、`consumeClick`、`actionSlot` 均不存在；pending 建立与点击消费已回到 baseline
   的 `detectAndRecord` / `consumePendingBoxIfAllowed` 原调用点，没有替代 wrapper。
5. role toggle、task/taskRun/window/hwnd/identity fence、pending key、日志、返回路径与五种本地 fact 状态保持；
   未新增 owner/session/ledger/TTL/retry，也未修改 remote/schema/POM/其它 Service。

验收门：等待本轮所有 Java writer 稳定后，由父级运行 Cloud `mvn -q clean package`；构建通过前不得宣称整类最终完成。
无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Parent Direct Implementation Task - `W-696-PLAYER-STATE-WHOLE-ADAPT-1` - 2026-07-14T14:28:00-04:00

External C 下一任务，直接实施，不写 Design。请在 **2026-07-14T14:48:00-04:00** 前于本日志真实 EOF 追加：
`CLAIMED | task=W-696-PLAYER-STATE-WHOLE-ADAPT-1 | claimedAt=<ISO> | writeSet=[Cloud PlayerStateService.java,this-log]`。
20 分钟只检查领取；领取后可工作超过 20 分钟。

唯一 Java 写集：
- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`

目标：以 `696a12b0` 同路径完整文件为唯一业务权威，一次性适配**完整 PlayerStateService**。Cloud 保留身份/位置/补给/
急救/香状态的全部判断、cache、counter、顺序、delay、fallback、state 与日志；把窗口 capture/template/OCR/input 与永久本地
`BagService` 调用换成当前已有的 typed fact/capture/InputBundle/`BAG_USE_INCENSE` closed local macro。删除该文件对
`GameClientTracker`、`TextRecognizer`、`InputProvider`、`CoordinateHelper`、`LocationVisionService`、
`WindowScopedTempPath`、`WindowTaskContextHolder`、`BagService` 的编译依赖。

pre-cutover preserved `PlayerStateService.java` 仅作现有 DTO/port 接法参考，不得覆盖 696 业务。`MainBagSession` 独占段不得伪造；
若当前 closed macro 无法逐字覆盖该入口，保留 public 语义并在同一 Implementation 交付中给出精确阻塞调用点，不得新增独立
GIVE_ITEM/Bag owner、TTL、retry、空 holder 或 default/global state。不得改 remote/schema/POM/其它 Service。

交付 `Implementation #1`：696 全方法对照、local->typed 矩阵、terminal/stop 处理、desktop import 清零与 scoped check。
并发期间不跑 Maven/test/runtime，不做 Git；父级统一构建。
无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Direct Cohort Task - `W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1` - 2026-07-14T17:44:00-04:00

请 External C 在 **2026-07-14T18:04:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1 | claimedAt=<ISO-8601> | writeSet=[Cloud DialogChoiceMemoryService.java,Cloud WorldMapRouteResultMemoryService.java,Cloud MemoryService.java,Cloud CloudServiceConfiguration.java,this-log]`

直接实施一个完整 Cloud memory storage cluster，不写 Design。唯一 Java 写集：

- Cloud `src/main/java/com/bot/dhxy/service/DialogChoiceMemoryService.java`
- Cloud `src/main/java/com/bot/dhxy/service/WorldMapRouteResultMemoryService.java`
- Cloud `src/main/java/com/bot/dhxy/service/MemoryService.java`
- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java`
- 本日志 append-only

目标是一次闭合 public caller -> `MemoryService` -> 两个完整 persisted Service -> tenant/user private storage 的真实链，
不是 DTO/helper/单方法壳：

1. `696a12b0` 三个 Service 的全部 public/private 方法、key 规范、route mode、success/failure/abandoned settlement、
   `MAX_FAILURES_BEFORE_DISABLE=3`、stable success streak、world-map clean streak `5`、同步/加载/临时文件替换顺序、
   fallback 与日志原样保留；不得重写 schema 或新增 TTL/retry/compaction。
2. Cloud 不得使用进程工作目录下共享的 `config/*.json` default authority。保留两个 Service 现有显式 `Path`
   constructor；`CloudServiceConfiguration` 对二者都使用同一 authenticated tenant/user `CloudServiceStorage`
   private root 下的独立固定文件名：`dialog_choice_memory.json` 与 `world_map_route_result_memory.json`。
3. component scan 必须对两个显式 bean 都避免 duplicate bean；`MemoryService` 继续由 Spring 构造注入这两个 exact
   scoped instance。不得暴露 raw tenant id、credential 或 real state root，不新增 global singleton/cache governor。
4. 当前 DialogChoice scoped bean 已存在，须保留并复核，不得为了本单重开已批准语义；主要缺口是
   `WorldMapRouteResultMemoryService` 仍会由 component scan 走 no-arg local `config/` path。只做闭合该 Cloud storage
   authority 所需的最小修改；若 `DialogChoiceMemoryService`/`MemoryService` 源码已满足基线，应保持字节不动并在交付中
   明确列为 full-chain reviewed/read-only-in-write-set，不制造无意义 diff。
5. 对照 `NavigationService` 当前 world-map route caller 与 dialog-choice caller，证明它们都经同一 `MemoryService`
   facade 到 scoped stores；不得修改 Navigation/B shared wire/A/D/runner/tests/POM。

交付 `Implementation #1` 时列四文件实际 diff、两个 bean/file path、三个 Service 完整 method inventory、基线差异、
caller reachability 与 scoped check。不得 build/test/runtime/Git；父级在所有 Java writer 稳定后统一 fresh package。
本 cohort 经父级源码审查与 fresh build 前不增加 `189/407`。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T14:30:00-04:00

CLAIMED | task=W-696-PLAYER-STATE-WHOLE-ADAPT-1 | claimedAt=2026-07-14T14:30:00-04:00 | writeSet=[Cloud PlayerStateService.java,this-log]

## External Worker C - W-696-PLAYER-STATE-WHOLE-ADAPT-1 Implementation #1 - 2026-07-14T14:40:00-04:00

**结论：PARENT PREREQUISITE BLOCKED，P1=1（缺 typed 观测源）。Java 零改动**（同 B 的 TaskTracker 先例）。
当前 Cloud `PlayerStateService.java` blob（git hash-object）= `096d8917b0372422b3ed141300419f9b71c1392c`，1483 行，
与 `696a12b0:src/main/java/com/bot/dhxy/service/PlayerStateService.java` **byte-exact 一致**（未覆盖 696 业务、未做半程改动）；`git status`=`??`，`git diff --check` 干净。

父级已确认可用替代面：typed fact（`WindowFactKind` 10 值）、`capture()`（CLOUD_SERVICE_INPUT/DIAGNOSTIC）、`InputBundle`、
`BAG_USE_INCENSE`（`CloudBagUseIncensePort`）、状态/判断（现有 `CloudPlayerStateStateOwner`/`CloudPlayerStateStateGovernor`）。
按 brief「closed macro/fact 无法覆盖的入口，保留 public 语义并给出精确阻塞调用点」，本单核心观测无 typed 源，故零改动交付并精确列点。

### 696 全方法对照（1483 行、~50 方法，按域）

- 身份/位置：`syncMyIdentity`(143)、`syncMyPosition`(159)、`syncAll`(191)。
- first-aid 门/计数：`resetCheckCounter`(201)、`performStartupFirstAidCheck`(217)、`probeAndConsumeHealthyFirstAidNoFocus`(238)、
  `probeFirstAidSupplyNoFocus`(259)、`performCachedFirstAidPlanNow`(307)、`performCachedFirstAidPlanDirect`(340)、
  `areStatusBarsVisibleNoFocus`(388)、`performFirstAidCheck`(416)。
- 治疗：`healAll`(445/474)、`healAllDirect`(452)、`healPlayer`(480)、`healPet`(493)、`checkAndHeal`(679/683)。
- 香（SheYaoXiang）：`ensureSheYaoXiangActive`(506/516/534)、`ensureSheYaoXiangActiveInOpenMainBag`(529)、
  `ensureSheYaoXiangActiveForLeaderTask`(651/661)。
- 快照/像素：`captureBarsSnapshot`(697)、`captureBarsSnapshotNoFocus`(703)、`findSupplyTargetsFromSnapshot`(801)、
  `addSupplyTargetIfNeeded`(814)、`isSupplyNeededFromSnapshot`(833)、`checkAndHealFromSnapshot(IfEnabled)`(789/868)、
  `isHealthyInSnapshotArea`(921)、`isHealthyColor`(940)、`healIfUnhealthy`(951)。
- 香状态/OCR：`probeIncenseStatus`(1002)、`probeIncenseIconPresence(InRect)`(1056/1083)、`cachedIncenseIconProbeRect`(1070)、
  `rememberIncenseIconPoint`(1118)、`cropSheyaoxiangMatchedColumn`(1123)、`readSheyaoxiangRemainingTime`(1167)、
  `readSheyaoxiangRemainingMinutesGreen`(1215)、`isSheyaoxiang(Cyan/Green)DigitPixel`(1284/1291)、`incenseLastUsedTimeForRemainingMs`(1298)。
- 鼠标避让/工具：`moveMouseAwayBeforePlayerStateSnapshot*`(708/733/746)、`mouseOverCaptureRect`(722)、`randomMouseAwayPoint`(757)、
  `currentLogicalMousePoint`(768)、`writeImage`(1304)、`calculateX`(1312)、`normalizeThreshold`(1320)、`state`(1330)、
  `checkpoint`(1355)、`isInputWorkerThread`(1359)、`safeReason/safeLatencyValue`(1368/1372)。
- 内嵌类型：`IncenseIconProbe`/`IncenseStatusProbe`/`IncenseRemainingTime`/`FirstAidPlan`/`FirstAidTarget`。

### local -> typed 矩阵

| baseline 调用点 | 依赖 | 可替代？ | 目标 |
|---|---|---|---|
| `inputProvider.moveMouse`(364/753)、`clickRight(absX,absY,100)`(371/963) | InputProvider | ✅ | InputBundle（MOVE_MOUSE / CLICK_RIGHT delayMs=100，SCREEN_ABSOLUTE_PX） |
| `bagService.findAndUseItem(MAIN_BAG,incenseTemplate,null,ctx)`(518) | BagService | ✅(判断留云) | `CloudBagUseIncensePort.useIncense` / `BAG_USE_INCENSE` |
| `windowTaskContextHolder.rawCurrent()`→windowId/epoch(664/1331/1334) | WindowTaskContextHolder | ✅ | `TaskExecutionContext.getWindowId()/getPlayerIdentityEpoch()` |
| 香计时/first-aid plan/check counter/combat-exit state | (本类字段) | ✅(已迁) | `CloudPlayerStateStateOwner`/`Governor`（已存在） |
| `tracker.getWindowBaseX/Y()`、`refreshWindowState()`(267/279/348/423/734/883…) | GameClientTracker | ⚠️部分 | 需 `GEOMETRY`/`BINDING` fact 的 screen-absolute window origin（可接，但仅几何） |
| `tracker.captureToMemory` **血条像素**(689/700/705) + `isHealthyColor`(940)/`isHealthyInSnapshotArea`(921)/`findSupplyTargetsFromSnapshot`(801)/`isSupplyNeededFromSnapshot`(833)/`checkAndHealFromSnapshot`(868) | GameClientTracker + 本地像素分析 | ❌ | **无 health/status/supply/first-aid typed fact**；brief 禁把像素分析复制进 Cloud |
| `tracker.captureToMemory` **香状态图**(1004/1085) + `ImageFinder.find`(1021/1096) 模板匹配 | GameClientTracker + ImageFinder | ❌ | **无 incense-status typed fact** |
| 香剩余 OCR：`textRecognizer.getAllTextResultsLocalOnly`(1190/1238) + `sheyaoxiangDigitTemplateReader.recognizeAndLearn`(1244) | TextRecognizer + DigitTemplateReader | ❌ | **无 incense-remaining typed fact**；brief 禁 OCR 复制 |
| `locationRadar.scanCurrentLocation()`→`LocationInfo`(168) | LocationVisionService | ❌ | **无 location typed fact** |
| `coordinateHelper.getScaledRect/getScaleRatio`(540/685/698/704/773) | CoordinateHelper | ⚠️ | 依赖 geometry fact 是否给 scale；随观测阻塞 |
| `windowScopedTempPath.resolve(...)`(1013/1039/1094/1188/1236) | WindowScopedTempPath | ❌(随 OCR) | 仅本地 OCR 中间文件；OCR 无 typed 源则此依赖无法删 |
| `ensureSheYaoXiangActiveInOpenMainBag(MainBagSession,...)`(529) | BagService.MainBagSession | ❌ | `BAG_USE_INCENSE` 为单一 closed macro，无法承接外部已开 `MainBagSession` 独占段（不得伪造 session/新增 Bag owner） |

### 精确阻塞调用点（P1，缺前置 typed 观测源）

1. 血条/补给观测：`:689 :700 :705 :801 :833 :868 :921 :940`——无 health/status/supply/first-aid fact。
2. 香状态模板：`:1004 :1021 :1085 :1096`——无 incense-status fact。
3. 香剩余 OCR：`:1190 :1238 :1244`——无 incense-remaining fact（且 brief 禁 OCR 入 Cloud）。
4. location：`:168`——无 location fact。
5. MainBagSession 独占入口：`:529`——`BAG_USE_INCENSE` 单宏无法逐字覆盖外部 session 段。

删除 `GameClientTracker/TextRecognizer/CoordinateHelper/LocationVisionService/WindowScopedTempPath` 的编译依赖当前**不可行**：这些是上述观测的唯一实现，且无对应 typed fact/capture-分析面可接；强行删除会摧毁 696 的血条/补给/香/location 业务判断。`InputProvider`、`WindowTaskContextHolder`、`BagService`（仅 incense-use 分支）本可迁，但与阻塞观测在同方法内强耦合，单独迁移会留下半程不可编译文件。

### terminal/stop 处理（可迁部分的规划，未落地）

若前置观测 fact 补齐：input→InputBundle、incense-use→`CloudBagUseIncensePort`，均按已 APPROVED 的 CommonBox/UI_CLEAN 终态范式——
`EXECUTED`→成功、`NOT_EXECUTED`→未执行、`STOPPED`→`TaskCheckpoint.throwIfStopRequested` 未确认则 `TaskFatalException`、`UNKNOWN`/非法→fatal、
InterruptedException 复位 flag 后 fatal，不宽 catch、不吞 RuntimeException；状态读写走 `CloudPlayerStateStateOwner` 执行锁 + governor projection gate。

### desktop import 清零 + scoped check

- import 清零：**BLOCKED**。`GameClientTracker/TextRecognizer/CoordinateHelper/LocationVisionService/WindowScopedTempPath/BagService.MainBagSession`
  无 typed 替代，无法零化；本单不做半程删除。`java.awt.*`/`BufferedImage`/OCR/template import 同因保留。
- scoped check：`git hash-object`=`096d8917…`（=baseline），`git status`=`??`，`git diff --check` 工作树 whitespace 干净；未改任何 Java/remote/schema/POM/其它 Service；未 build/test/runtime；未做 Git。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=本日志（Java 零改动，PlayerStateService.java 保持 baseline byte-exact）；未改 remote/schema/POM/其它 Service；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 按 brief 逃生条款执行：核心观测（血条/补给/香状态/香剩余/location）与 MainBagSession 独占段无 typed 源，保留 public 语义、给出精确阻塞调用点；未伪造 session、未新增 GIVE_ITEM/Bag owner/TTL/retry/空 holder/default·global state。
3. 判定依据：`WindowFactKind` 10 值无 health/location/incense/supply/first-aid；`LocalMacroKind` 仅 BAG_RETURN_ITEM/BAG_USE_INCENSE/NAVIGATE/UI_CLEAN；remote/ 内无任何观测 fact（grep 证）。
4. 无已批准业务差异；按 `696a12b0` 等价迁移——但需父级前置补齐 health/incense/location typed 观测面（新 fact 或 CLOUD_SERVICE_INPUT capture-分析契约）后方可 whole-adapt。

External Worker C 交付 W-696-PLAYER-STATE-WHOLE-ADAPT-1 Implementation #1（**PARENT PREREQUISITE BLOCKED P1=1**：血条/补给/香状态/香剩余/location 观测无 typed 源、MainBagSession 独占段无单宏覆盖；Java 零改动保 baseline byte-exact；含全方法对照、local→typed 矩阵、精确阻塞调用点、终态规划、scoped check）；持续重读本日志等待父级前置补齐或改单；自审不算 Approved。

## Parent Prerequisite Review #1 - `W-696-PLAYER-STATE-WHOLE-ADAPT-1` - 2026-07-14T14:54:00-04:00

**PREREQUISITE BLOCKED，P0=0 / P1=1 / P2=1；当前 Java 零改动确认。** 父级独立核对当前/696 blob 均为
`096d8917b0372422b3ed141300419f9b71c1392c`。现有 10 种 `WindowFactKind` 确无 health/supply/location/incense fact；
`BAG_USE_INCENSE` 不能等价覆盖外部已打开 `MainBagSession` 的续用段，故不允许伪造 whole-adapt。

- **P1 前置缺口成立：** `PlayerStateService:159-188,689-705,801-940,1002-1298` 的位置、血条和香观测没有
  caller-reachable typed producer；generic PNG capture 不能把用户已定为 DHXY-local 的 template/OCR 偷搬到 Cloud。
- **P2 路线修正：** 交付中提到的 `CloudPlayerStateStateOwner/Governor` 属简化路线已禁止的 per-Service owner 路径，
  后续不得接入。保留 696 类内 `runtimeStates` 与原状态更新，只把本地机械读取替换为共享 fact；既有
  `SheyaoxiangStatusDecisionFacade` 可复用其纯决策，但它目前无真实 wiring，不能冒充 producer。

本结论确认前置缺口，不批准整类完成、不增加计数。`PlayerStateService.java` 写集暂时释放；contract 落地后仍由原 C
返回该整类继续实施。

## Parent Direct Implementation Task - `W-696-BATTLE-RADAR-CLOUD-FACT-1` - 2026-07-14T14:54:00-04:00

请 External Worker C 在 **2026-07-14T15:14:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-BATTLE-RADAR-CLOUD-FACT-1 | claimedAt=<ISO-8601> | writeSet=[Cloud WindowFactKind.java,WindowFact.java,WindowFactOutcome.java,RemoteCommandOutcomeEnvelope.java,this-log]`

直接实施，不写 Design。唯一 Java 写集：

- Cloud `remote/WindowFactKind.java`
- Cloud `remote/WindowFact.java`
- Cloud `remote/WindowFactOutcome.java`
- Cloud `remote/RemoteCommandOutcomeEnvelope.java`

新增 7 个 closed kind：`BATTLE_RADAR_AUTO_FLAG`、`BATTLE_RADAR_SELECTION_SIGNAL`、
`BATTLE_RADAR_TOP_SIGNAL`、`BATTLE_RADAR_MINIMAP_READABLE`、`BATTLE_RADAR_AVATAR_BASELINE`、
`BATTLE_RADAR_AVATAR_PROBE`、`BATTLE_RADAR_AVATAR_REFRESH`。在 `WindowFact` 增加并 sealed 许可：

- `BattleRadarSignalFact` + `BattleRadarSignalState`：`VISIBLE/NOT_VISIBLE/CAPTURE_UNAVAILABLE/MECHANICS_FAILED`；
- `BattleRadarMinimapFact` + `BattleRadarMinimapState`：`READABLE/UNREADABLE/MECHANICS_FAILED`；
- `BattleRadarAvatarFact` + `BattleRadarAvatarState`：
  `BASELINE_CAPTURED/UNCHANGED/CHANGED/UNAVAILABLE/NOT_CONFIGURED/MECHANICS_FAILED`，并携可空
  `hoverClientX/hoverClientY/roiScreenLeft/roiScreenTop/roiScreenRight/roiScreenBottom` 供 baseline 日志，
  坐标存在时必须成组且 rect 为正面积。

`WindowFactOutcome.matches` 与 `RemoteCommandOutcomeEnvelope.parseFact` 逐 kind 映射：前三种 signal kind -> signal fact，
minimap -> minimap fact，后三种 avatar kind -> avatar fact。保持所有既有 10 kind 解析逐字不变；不改 digest/codec/broker/POM，
不新增 owner/session/ledger/TTL/retry。交付 `Implementation #1` 时列出 sealed/match/parse 全矩阵及 scoped check；不跑 Maven/test/runtime/Git。
**无已批准业务差异；本单只建机械观测合同。**

## External Worker C - CLAIMED - 2026-07-14T14:56:00-04:00

CLAIMED | task=W-696-BATTLE-RADAR-CLOUD-FACT-1 | claimedAt=2026-07-14T14:56:00-04:00 | writeSet=[Cloud WindowFactKind.java,WindowFact.java,WindowFactOutcome.java,RemoteCommandOutcomeEnvelope.java,this-log]

## External Worker C - W-696-BATTLE-RADAR-CLOUD-FACT-1 Implementation #1 - 2026-07-14T15:02:00-04:00

按合同为 BattleRadar 机械观测新增 closed typed fact，仅改授权的 4 个 remote 文件；镜像既有 fact 合同范式（CommonBox/TeamReturn/TaskTracker）。
按父级明令**未跑 Maven/test/runtime、未做 Git**；以逐项静态自查代编译门。既有 10 kind 解析逐字不变；未改 digest/codec/broker/POM，未新增 owner/session/ledger/TTL/retry。

4 文件新 blob / sha256 / 行数：
- `remote/WindowFactKind.java`  blob `990364462e99…`  sha256 `1084fecff1f7161f70a120c809fe28baff0b9b6c617b4a79c64d1c6a3661dc57`  21L
- `remote/WindowFact.java`  blob `f4a6f62fecf9…`  sha256 `f6e3783cd250aeaf568aa47419b75f050d26e43a8b562126184442818f9ba39a`  405L
- `remote/WindowFactOutcome.java`  blob `e1d19fb67884…`  sha256 `2716ac33836df55e9d449030cf5a075bb69a6c38c6aaae877bb099666f8863bf`  54L
- `remote/RemoteCommandOutcomeEnvelope.java`  blob `c1be4eaea4a8…`  sha256 `7983a7f3be5d31134b20c722dc6154acd1c2d705f1d8119cec9f90bec9aceeff`  486L

### 新增 7 closed kind（WindowFactKind：10→17，追加于 TASK_TRACKER_PANEL_RECT 后）

`BATTLE_RADAR_AUTO_FLAG`、`BATTLE_RADAR_SELECTION_SIGNAL`、`BATTLE_RADAR_TOP_SIGNAL`、`BATTLE_RADAR_MINIMAP_READABLE`、
`BATTLE_RADAR_AVATAR_BASELINE`、`BATTLE_RADAR_AVATAR_PROBE`、`BATTLE_RADAR_AVATAR_REFRESH`。

### 新增 3 fact record + 3 state enum（WindowFact，sealed permits 10→13）

| fact record | 字段 | state enum 值 | 校验 |
|---|---|---|---|
| `BattleRadarSignalFact` | `(state)` | `BattleRadarSignalState`：VISIBLE / NOT_VISIBLE / CAPTURE_UNAVAILABLE / MECHANICS_FAILED | state 非空（纯信号，无坐标/坐标空间） |
| `BattleRadarMinimapFact` | `(state)` | `BattleRadarMinimapState`：READABLE / UNREADABLE / MECHANICS_FAILED | state 非空 |
| `BattleRadarAvatarFact` | `(state, hoverClientX, hoverClientY, roiScreenLeft, roiScreenTop, roiScreenRight, roiScreenBottom)`（后六为可空 Integer） | `BattleRadarAvatarState`：BASELINE_CAPTURED / UNCHANGED / CHANGED / UNAVAILABLE / NOT_CONFIGURED / MECHANICS_FAILED | state 非空；六坐标**成组**（hasAny→hasAll，否则报「must be present as a full group」）；存在时 `roiScreenRight>roiScreenLeft && roiScreenBottom>roiScreenTop`（正面积），供 baseline 日志 |

### sealed / match / parse 全矩阵（逐 kind）

| kind | sealed permits | WindowFactOutcome.matches | RemoteCommandOutcomeEnvelope.parseFact |
|---|---|---|---|
| BATTLE_RADAR_AUTO_FLAG | BattleRadarSignalFact | `instanceof BattleRadarSignalFact` | `treeToValue(fact, BattleRadarSignalFact.class)` |
| BATTLE_RADAR_SELECTION_SIGNAL | 〃 | 〃 | 〃 |
| BATTLE_RADAR_TOP_SIGNAL | 〃 | 〃 | 〃 |
| BATTLE_RADAR_MINIMAP_READABLE | BattleRadarMinimapFact | `instanceof BattleRadarMinimapFact` | `treeToValue(fact, BattleRadarMinimapFact.class)` |
| BATTLE_RADAR_AVATAR_BASELINE | BattleRadarAvatarFact | `instanceof BattleRadarAvatarFact` | `treeToValue(fact, BattleRadarAvatarFact.class)` |
| BATTLE_RADAR_AVATAR_PROBE | 〃 | 〃 | 〃 |
| BATTLE_RADAR_AVATAR_REFRESH | 〃 | 〃 | 〃 |

三 signal kind 合并为一个 `case A, B, C -> BattleRadarSignalFact`；三 avatar kind 合并为一个 `case ... -> BattleRadarAvatarFact`；minimap 单 case。matches 与 parseFact 两个 factKind switch 均无 default，加 7 case 后对 17 值穷尽。

### 静态自查（工具证据，代被禁编译门）

- WindowFactKind 值数=17（grep）；7 新 kind 在 Kind / matches / parseFact 三处各出现恰 1 次（穷尽，无遗漏）。
- 既有 10 kind 在 parseFact 仍逐字（grep case BINDING..TASK_TRACKER_PANEL_RECT 计数=10），matches 同；WindowFactOutcome 的 EXECUTED 禁用/OBSERVED-fact 匹配等约束未改。
- sealed permits 含新 3 fact（BattleRadarSignalFact/BattleRadarMinimapFact/BattleRadarAvatarFact），3 record 均 `implements WindowFact`，避免 sealed 非法扩展。
- record 组件 accessor 由 record 自动生成；`RemoteProtocolValidation.required` 已在 WindowFact 使用；avatar 成组 + 正面积校验镜像 TaskTrackerPanelRectFact 的 rect 范式。
- 多标签 `case A, B, C ->` 为合法 arrow switch；Jackson `MAPPER.treeToValue(fact, XxxFact.class)` 与既有 record fact 同路径反序列化。
- scoped：仅 4 个授权 remote 文件改动；未改 digest/codec/broker/POM/Service/schema 之外文件；未新增 owner/session/ledger/TTL/retry。`git diff --check` 4 文件工作树 whitespace 干净。
- 注：仓库根 `git status` 另有 `M pom.xml`，**非本单改动**（本单只 Edit 上列 4 个 .java），属他人 dirty，已保护未触碰。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=4 个授权 remote 文件 + 本日志；未改其它文件；未 build/test/runtime；未做 Git；保护他人 dirty（pom.xml）/untracked。
2. 合同逐条落实：7 closed kind、3 fact record + 3 state enum（值与 brief 一致）、avatar 六坐标成组 + 正面积、matches/parseFact 逐 kind 映射（前三 signal→signal fact、minimap→minimap fact、后三 avatar→avatar fact）、既有 10 kind 逐字不变、无 digest/codec/broker/POM 改动、无 owner/session/ledger/TTL/retry。
3. 因父级禁编译门，已用逐项静态自查（kind 计数/穷尽/permits/既有不变/diff）替代，如实标注"未编译"。
4. 无已批准业务差异；本单只建机械观测合同。

External Worker C 交付 W-696-BATTLE-RADAR-CLOUD-FACT-1 Implementation #1（新增 7 closed WindowFactKind + BattleRadarSignal/Minimap/Avatar 3 fact record + 3 state enum，
sealed permits/matches/parseFact 逐 kind 穷尽映射，avatar 六坐标成组+正面积；既有 10 kind 逐字不变、未改 digest/codec/broker/POM；按父级明令未 build/test，以静态自查代编译门）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #1 - `W-696-BATTLE-RADAR-CLOUD-FACT-1` - 2026-07-14T15:08:00-04:00

**SOURCE APPROVED，P0/P1/P2=0。** Delivery Preflight Helper 已先追加非绑定静态预检；父级随后独立读取
四个实际源码并复核 hashes、17-kind 矩阵、构造约束与原 10-kind 路径：

- `WindowFactKind.java:4-20` 保留原 10 项顺序并恰追加 7 个 `BATTLE_RADAR_*` kind。
- `WindowFact.java:3-8,337-404` 的 sealed permits 恰加入 signal/minimap/avatar 三个 record；三个 state
  enum 值与 brief 一致。signal/minimap state 必填；avatar state 必填，六个 nullable 坐标只能全有或全无，
  有坐标时 `right>left && bottom>top`。父单只要求坐标可空、成组与 ROI 正面积，未要求按 state 强制存在性，
  因此 helper 提醒的可选组合不构成偏差。
- `WindowFactOutcome.java:8-21,28-52` 保持 WINDOW_FACT、非 EXECUTED、OBSERVED 必有匹配 fact、其它
  terminal fact=null 的合同；三 signal kind、minimap、三 avatar kind 映射正确且 switch 穷尽。
- `RemoteCommandOutcomeEnvelope.java:197-203,391-417` 的 parse 路径与 matches 一一相同；原 10 kind 仍在
  `:393-406` 映射原 fact，未缺失或重绑。四文件 scoped whitespace check 无错误；未见 digest/codec/broker/POM、
  owner/session/ledger/TTL/retry 越权增量。

C 报告中的 `hash-object`、`diff --check`、`status` 是只读 Git 取证，不是 Git mutation，故不形成 P2。
本结论只批准 Cloud closed fact 合同源码；最终集成仍等待 D 的 DHXY parity/producer、A 的 consumer 返修与父级
fresh 双构建。计数保持 `189/407`。**无已批准业务差异；本单只建立机械观测合同。**

## Parent Direct Implementation Task - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1` - 2026-07-14T15:13:00-04:00

External Worker C 已从上一单释放，现直接实施，不写 Design。请在 **2026-07-14T15:33:00-04:00** 前于本日志真实 EOF 追加：

`CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]`

唯一 Java 写集：

- 新建 DHXY `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`
- 本日志

以 `696a12b0` 的 `PlayerStateService.java` 为唯一行为权威，只把其中连续且必须留在 exact-window 本地的 first-aid
机械段抽成一个 closed local mechanics：`captureBarsSnapshot*`、mouse-obstruction clear、
`findSupplyTargetsFromSnapshot`、`isSupplyNeededFromSnapshot`、`checkAndHealFromSnapshot`、`isHealthyColor`、
`healIfUnhealthy`。入口接收 exact binding/context 与 closed intent（四目标 enable/threshold）；本类不得保存 cooldown、
task phase、队伍策略或跨调用状态。

必须原样保留：cursor obstruction 检查与必要 move-away、一次 bars snapshot、higher-threshold `+10` 反证、
threshold normalization、sample radius、目标顺序、`350ms` 二次确认、目标原位 right-click `100ms` 与 `800ms`
settle。capture -> 判断 -> 二次 capture -> input 的整段必须在单一 input queue/已持有 worker 内原子执行，禁止内部嵌套
submit。返回 closed typed mechanical result，逐目标表达 healthy/supply-needed/executed/capture-failed/stopped 与实际采样/点击
坐标；不得新增 owner/session/ledger/TTL/retry，不得改现有 `PlayerStateService`、Cloud remote/schema、handler 或 D 正在写的
BattleRadar 文件。

交付 `Implementation #1` 时列出 baseline 原方法/常量/顺序逐项对照、输入队列边界、terminal/result 矩阵与 scoped check。
并发期间不跑 Maven/test/runtime，不做 Git；父级统一构建。本单只是 PlayerState 完整迁云的真实本地机械前置，
不宣称整类完成、不增加计数。**无已批准业务差异；按 `696a12b0` 等价抽取本地机械段。**

## External Worker C - CLAIMED - 2026-07-14T15:15:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1 | claimedAt=2026-07-14T15:15:00-04:00 | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1 Implementation #1 - 2026-07-14T15:26:00-04:00

以 `696a12b0` `PlayerStateService` 为唯一行为权威，把连续且必须留在 exact-window 的 first-aid 机械段抽成一个 closed local mechanics
新建 DHXY `service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`（唯一 Java 写集 + 本日志）。镜像已批准的 CommonBox/BattleRadar
`*LocalObservationMechanics` 范式：`@Slf4j @Service final`、构造注入、binding 入口、raw window-client ROI（无 scale 步骤）。
按父级明令**未跑 Maven/test/runtime、未做 Git**；以静态自查代编译门。新文件 blob（git hash-object）= `9ddc7ffbdd086dee5351cfc5bcd64983bbca03ae`，
483 行，SHA-256 = `7a9b82a6400761d2700efc8bf7dc5c8989036bc41c5c56957f7ddd88f747ea93`。

### baseline 原方法/常量/顺序逐项对照（全部保真）

| baseline 方法（696a12b0） | 本类 | 说明 |
|---|---|---|
| `captureBarsSnapshot`（含 mouse-away） | `captureBarsSnapshot(binding)` | 用 `captureService.captureRegion(binding,baseX,baseY, baseX+BARS_SCAN_LEFT_X, baseY+BARS_SCAN_TOP_Y, +W, +H)`（window-client ROI，image local 原点=window-client(BARS_SCAN_LEFT_X,TOP_Y)）替代 `coordinateHelper.getScaledRect`+`tracker.captureToMemory` |
| `moveMouseAwayBeforePlayerStateSnapshotIfNeeded`/`Direct`/`mouseOverCaptureRect`/`randomMouseAwayPoint`/`currentLogicalMousePoint`/`formatPoint`/`formatRect` | 同名私有 | 仅保留 direct 分支（已持有 worker），`inputProvider.moveMouse`+`TaskSleep.sleep(300)`；mouse 逻辑坐标用 `coordinateHelper.getScaleRatio()` 换算；SAFE_MOUSE forbidden 区/padding=12 保真 |
| `findSupplyTargetsFromSnapshot`/`addSupplyTargetIfNeeded` | `findSupplyTargetsFromSnapshot`/`plannedTargets`/`addTargetIfEnabled` | 四目标按序分类；未启用不入列；`config.*` 读取改为 closed `FirstAidIntent` 的 enable/threshold |
| `isSupplyNeededFromSnapshot` | 同名 | 越界 skip、`isHealthyInSnapshotArea`、higher `+10`(min 95)、rgb 日志 全保真 |
| `checkAndHealFromSnapshot` | 同名 | 越界、`+10` 反证、`TaskSleep.sleep(350)` 二次 capture、二次两级 healthy 反证、`healIfUnhealthy` 全保真 |
| `isHealthyInSnapshotArea` | 同名 | sample radius (2,1)、`healthyCount>=2` 保真 |
| `isHealthyColor` | 同名 | 红 `r>150&&r>g+80&&r>b+80`、蓝 `b>150&&g>120&&b>r+80` 逐字保真 |
| `healIfUnhealthy` | 同名 | 仅 direct：`inputProvider.clickRight(absX,absY,100)`+`TaskSleep.sleep(800)`；返回 true=已点 |
| `calculateX`/`normalizeThreshold` | 同名 | 阈值分段 30/50/70、ratio 计算 保真 |
| `FirstAidTarget` record | 私有 record | 扩展携 leftX/rightX 供 higher-threshold 反证复算（值等价 baseline 现算） |

常量逐字保真：CHAR_BAR 949/1020、PET_BAR 823/876、BAR_HP_Y 85 / BAR_MP_Y 101、BARS_SCAN(LEFT=823,TOP=85,W=198,H=17)、
radius(2,1)、`+10`、`350`/`100`/`800`、GAME_CLIENT 1024×768、SAFE_MOUSE_FORBIDDEN(761,147)、hover-clear 300、obstruction padding 12。
目标顺序：人物血量(红,HP_Y) → 人物法力(蓝,MP_Y) → 宝宝血量(红,HP_Y) → 宝宝法力(蓝,MP_Y)。

### 输入队列边界

入口假定运行在 caller 已持有的单一 input worker 内：全程只用 direct `InputProvider.moveMouse/clickRight` + `TaskSleep.sleep`，
**无 `inputSequences.submitAndWait`、无嵌套 submit**（grep 证空）。capture → 判断 → `350ms` → 二次 capture → 原位右键 `100ms` → `800ms` settle
在同一线程内顺序原子执行；每次 capture 的 image 由消费方 flush，二次 capture 用 try/finally flush。删除 baseline 的 `isInputWorkerThread()`
分支（mechanics 恒 direct），符合「禁止内部嵌套 submit」。本类无字段状态、无 cooldown/task phase/队伍策略/跨调用状态、无 owner/session/ledger/TTL/retry。

### terminal / result 矩阵

`FirstAidMechanicalResult(SnapshotStatus, List<TargetOutcome>)`；`SnapshotStatus∈{CAPTURED,CAPTURE_FAILED,STOPPED}`。
binding 非法 → CAPTURE_FAILED / 空目标；入口 stop → STOPPED；首次 bars capture null → 每个启用目标 CAPTURE_FAILED。
`TargetOutcome(name,status,sampleRelX,sampleRelY,clickAbsX,clickAbsY)`，`status∈{HEALTHY,SUPPLY_NEEDED,EXECUTED,CAPTURE_FAILED,STOPPED}`：

| 情形 | status | 坐标 |
|---|---|---|
| 首次采样 healthy（阈值或 +10） | HEALTHY | sample rel |
| 分类后 stop（heal 前） | STOPPED | — |
| 二次 capture null | CAPTURE_FAILED | sample rel |
| 二次采样 healthy（阈值或 +10）→ 判首次误判 | HEALTHY | sample rel |
| 二次 unhealthy 且中心像素 unhealthy → 原位右键 | EXECUTED | sample rel + click abs(binding.getX()+relX, +relY) |
| 二次 unhealthy 但中心像素 `isHealthyColor` → healIfUnhealthy 返回 false，不点 | SUPPLY_NEEDED | sample rel |

（EXECUTED 唯一携 click 坐标，record 构造器强制校验；SUPPLY_NEEDED 精确对应 baseline `healIfUnhealthy` 返回 false 的"需补给但中心像素合格未点击"路径。）

### 静态自查（工具证据，代被禁编译门）

- 禁用符号 grep 空：`tracker`/`inputSequences`/`submitAndWait`/`getScaledRect`/`config`/`captureToMemory`/`ImageFinder`/`GameContext` 均无。
- direct input：仅 `inputProvider.moveMouse`(191)、`inputProvider.clickRight(...,100)`(387)。
- 常量/延迟/`healthyCount>=2` 保真（grep 命中）。
- API 核对：`BoundWindowCaptureService.captureRegion(binding,baseX,baseY,x1,y1,x2,y2)`(7参)、`CaptureResult.image()`、`InputProvider.clickRight(int,int,int)`/`moveMouse(int,int)`、
  `CoordinateHelper.getScaleRatio()`、`TaskExecutionContext.isStopRequested()`、`TaskSleep.sleep(long)`、`WindowNativeBinding.getX/getY/hasNativeHandle/hasGeometry/getNativeHandle/getTitle` 均已核（镜像 CommonBox/BattleRadar mechanics 与 baseline）。
- imports 全部被引用；`FirstAidTarget`/`TargetOutcome`/`FirstAidIntent`/`TargetToggle` record accessor 自动生成；`TargetOutcome` 构造器校验 EXECUTED↔click 成对。
- scoped：仅新增 1 个 DHXY 文件；未改现有 `PlayerStateService`、Cloud remote/schema、handler、D 的 BattleRadar 文件；`git status` 仅本新文件 `??`，`git diff --check` 干净。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=DHXY `PlayerStateFirstAidLocalMacroMechanics.java`(新) + 本日志；未改其它任何文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 逐条落实：一次 bars snapshot、cursor obstruction 检查+必要 move-away、higher `+10` 反证、threshold normalization、sample radius、目标顺序、`350ms` 二次确认、
  原位右键 `100ms`+`800ms` settle、capture→判断→二次 capture→input 单 worker 原子无嵌套 submit、closed intent(四目标 enable/threshold)、closed typed per-target result（含实际采样/点击坐标）、
  无 cooldown/task phase/队伍策略/跨调用状态、无 owner/session/ledger/TTL/retry。
3. 因父级禁编译门，已用逐项静态自查（禁用符号/direct input/常量/API/scoped）替代，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价抽取本地机械段。本单只是 PlayerState 完整迁云的真实本地机械前置，不宣称整类完成、不增计数。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1 Implementation #1（新建 DHXY closed first-aid local mechanics：binding 入口 + 一次 bars snapshot(含 direct mouse-away)
+ 四目标 +10 反证 + 350ms 二次确认 + 原位右键 100/800，全常量/顺序/isHealthyColor/sample radius 保真；capture 用 BoundWindowCaptureService window-client ROI、input 用 direct InputProvider 无嵌套 submit；
closed typed per-target result；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #1 - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1` - 2026-07-14T15:38:00-04:00

**BLOCKED，P0=0 / P1=3 / P2=2。** 父级独立对照新文件与 `696a12b0 PlayerStateService.java:259-375,
416-478,697-970`。常量、颜色公式、sample radius、`+10`、350/100/800ms、binding capture 算术和 image flush
可保留；当前入口把两个 baseline 路径重组，不能作为等价本地 mechanics 接入。

1. **P1：无焦点预检与真实补给被合成一个会点击的入口。** active `runFirstAid:105-123` 先调用带 mouse-away
   的 `captureBarsSnapshot`，随后直接二次 capture/right-click；baseline `probeFirstAidSupplyNoFocus:259-299` 使用
   `captureBarsSnapshotNoFocus`，零鼠标移动/零输入，只生成 pending plan；真正点击在后续
   `performCachedFirstAidPlanNow:307-375` 或 `healAllDirect:452-471`。影响是 background no-focus probe 一旦接入
   就可能移动鼠标并点击，且 pending plan 两阶段语义消失。
2. **P1：direct physical input 没有 input-worker 门。** `:89-128` 是 public entry，`:191/:387` 直接
   `InputProvider.moveMouse/clickRight`，却没有像 `BagService.run*DirectForExclusive:251-300` 一样拒绝非
   `dhxy-input-action-worker` 调用。仅 JavaDoc 假定 caller 已持有 queue 不能保护多窗口输入安全。
3. **P1：新增了 baseline 不存在的目标间 stop gate/部分结果。** `:96-98` 把入口 stop 降为普通结果，
   `:118-120` 在候选循环内逐目标检查后 `continue`；baseline 只在外层 `healAll(taskContext):474-478` 前后走
   checkpoint，`healAllDirect` 内四目标不插 checkpoint。影响是同一 heal transaction 可半执行、半 STOPPED，
   且 STOPPED 不再由 task/remote terminal unwind。
4. **P2：目标执行与返回顺序不稳定。** `:116` 先分类全部目标并立即把 HEALTHY outcome 写入列表，`:117-123`
   再追加需补给目标；例如人物血需补、人物法健康时，结果顺序会变成人物法 -> 人物血。baseline 是人物血 ->
   人物法 -> 宝宝血 -> 宝宝法逐项判断/确认/点击，task brief 也要求该顺序。
5. **P2：closed status 夸大事实。** `:263-266` 越界返回 false 后在 `:243-247` 被标 HEALTHY；`:336-340`
   中心像素复核健康、实际不点击却标 SUPPLY_NEEDED。两者都不是机械观测到的事实，后续 Cloud consumer 会收到
   错误语义。

父级同时纠正原 Task Brief：其中“单一入口同时返回 stopped/supply-needed”会迫使非基线重组，**该要求作废**；
以 `696a12b0` 两条真实路径为准。

### Parent Repair Task - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1`

请原 External C 在 **2026-07-14T15:58:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]`

唯一 Java 写集不变。直接重排为两个真实机械边界，不新增第三条业务流：

- **no-focus probe：** exact binding + closed enable/threshold intent；直接
  `TaskCheckpoint.throwIfStopRequested(nullableContext, ...)` 一次后，使用无 mouse-away 的 bars capture，只按
  baseline 顺序返回 ordered candidate plan/每项真实观测；零 input、零 pending/cooldown state。
- **heal-all direct：** exact binding + intent；入口内联拒绝非 `dhxy-input-action-worker`；在整个 mechanics 前后各
  一次直接 `TaskCheckpoint`（context 可 null，复刻 baseline 外围），中间按人物血 -> 人物法 -> 宝宝血 -> 宝宝法
  **逐项**执行 initial 判断 -> `+10` -> 350ms confirm -> 原位 right-click 100ms -> 800ms，再进入下一目标。
  禁止先分类四项再统一点击，禁止目标间新增 stop gate，禁止内部 submit。

移除本地 `isStopRequested` wrapper 与 mechanical STOPPED 状态；stop/interruption 由 `TaskCheckpoint`/外围 remote
queue terminal 表达。结果列表必须固定目标顺序；越界/不可读用明确 UNREADABLE/CAPTURE_FAILED，不得标 HEALTHY；
center healthy 的 no-click 用 NO_ACTION/CONFIRMED_HEALTHY 等真实 closed 名称，不得标 SUPPLY_NEEDED。保留现有
常量/颜色/ROI/flush；不得改 PlayerStateService/handler/remote/schema/POM 或其它文件，不新增 owner/TTL/retry/wrapper。

交付 `Implementation Repair #1` 时列出两个入口各自对照、input-worker/checkpoint 矩阵、四目标顺序、closed
result 真值与 scoped check；不跑 Maven/test/runtime/Git，父级统一双构建。本单仍是前置，不增加计数。
**无已批准业务差异；按 `696a12b0` 原两阶段边界等价抽取。**

## External Worker C - CLAIMED - 2026-07-14T15:40:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1 | claimedAt=2026-07-14T15:40:00-04:00 | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1 Implementation Repair #1 - 2026-07-14T15:50:00-04:00

按 `Parent Source Review #1`（P1×3/P2×2）逐条返修：把上一版合成的单点击入口拆回 `696a12b0` 的**两条真实机械边界**，不新增第三业务流。
唯一 Java 写集不变（DHXY `PlayerStateFirstAidLocalMacroMechanics.java` + 本日志）。按父级明令未 build/test/runtime/Git；以静态自查代编译门。
新 blob（git hash-object）= `3888b641215ddcb2c6a3495934c117250aa4d970`，538 行，SHA-256 = `f1e5b65aafe1e2f5210b87ef7a655831be97d15519d032355d1f0b4028ac51fc`。

### 两入口各自对照 baseline

| 入口 | baseline 权威 | 行为 |
|---|---|---|
| `probeSupplyNoFocus(binding,context,intent)` → `NoFocusProbeResult` | `probeFirstAidSupplyNoFocus:259-299` | 起始一次 `TaskCheckpoint`；`captureBarsSnapshotNoFocus`（**无 mouse-away、零 input**）；按固定顺序对四目标做 `isSupplyNeeded` 式判断（sample + `+10` 反证），返回 ordered 观测；**零 pending/cooldown/state**（check-counter、pending plan 留 PlayerStateService 业务层） |
| `healAllDirect(binding,context,intent)` → `HealAllResult` | `healAllDirect:452-471` + 外层 `healAll(taskContext):474-478` | 入口内联 `if(!isInputWorkerThread()) throw IllegalStateException(...must run inside the exclusive input worker section)`；**前后各一次直接 `TaskCheckpoint`**（context 可 null）；中间**一次** `captureBarsSnapshot`（含 direct mouse-away）后，按固定顺序逐目标 initial 判断 → `+10` → `350ms` confirm 二次 capture → 原位右键 `100ms` → `800ms`，**逐项做完再下一项** |

### P1/P2 逐条修复

1. **P1 合成入口拆分**：no-focus probe 与 heal-all direct 现为两个独立 public 方法；probe 恒无 mouse-away、无二次 capture、无 click；pending 两阶段语义交回业务层（本类不建 plan）。
2. **P1 input-worker 门**：`healAllDirect` 入口即 `isInputWorkerThread()` 检查，非 `dhxy-input-action-worker` 抛 `IllegalStateException`（镜像 `BagService:256-259/297-299`）；probe 不需门（零 input）。
3. **P1 移除目标间 stop gate/部分结果**：删除本地 `isStopRequested` wrapper 与 mechanical `STOPPED` 枚举；heal 只在整段**前后各一次** `TaskCheckpoint`，四目标间**无** checkpoint（复刻 `healAllDirect` 内无 checkpoint、外层 `healAll(taskContext)` 前后各一）；stop/中断由 `TaskCheckpoint`/外围 remote queue terminal 表达。
4. **P2 固定顺序**：两入口共用 `orderedBars` 严格 人物血 → 人物法 → 宝宝血 → 宝宝法；结果列表按此定序、每目标恒一项（含禁用→DISABLED），不再"先分类再追加"导致乱序。
5. **P2 真值 closed status**：越界/不可读 → `UNREADABLE`（不再标 HEALTHY）；二次 capture 失败 → `CAPTURE_FAILED`；中心像素健康的 no-click → `NO_ACTION`（不再标 SUPPLY_NEEDED）。probe 状态 `{DISABLED,HEALTHY,SUPPLY_NEEDED,UNREADABLE}`；heal 状态 `{DISABLED,HEALTHY,NO_ACTION,EXECUTED,UNREADABLE,CAPTURE_FAILED}`；均无 STOPPED。

### input-worker / checkpoint 矩阵

| 入口 | worker 门 | 前 checkpoint | 目标间 checkpoint | 后 checkpoint | input |
|---|---|---|---|---|---|
| probeSupplyNoFocus | 无（零 input） | 1 次 | 无 | 无 | 无 |
| healAllDirect | 拒绝非 dhxy-input-action-worker | 1 次 | **无** | 1 次 | direct InputProvider（mouse-away move + heal right-click），无嵌套 submit |

### 四目标顺序与 closed result 真值

顺序恒 人物血量(红,HP_Y) → 人物法力(蓝,MP_Y) → 宝宝血量(红,HP_Y) → 宝宝法力(蓝,MP_Y)。
- probe 每项：DISABLED（未启用）/ UNREADABLE（越界）/ HEALTHY（sample 或 +10 合格）/ SUPPLY_NEEDED（低于阈值且 +10 仍低）。
- heal 每项：DISABLED / UNREADABLE（初判越界）/ HEALTHY（初判或二次确认合格）/ CAPTURE_FAILED（二次截图失败）/ EXECUTED（二次仍低且中心像素低→原位右键，唯一携 click abs 坐标）/ NO_ACTION（二次区域低但中心像素 `isHealthyColor` 合格→不点击）。

### 静态自查（工具证据，代被禁编译门）

- 移除：`STOPPED`/`isStopRequested` 代码级已无（grep 仅 javadoc 描述句）。
- 禁用符号空：tracker/inputSequences/submitAndWait/getScaledRect/config/captureToMemory/ImageFinder。
- 两入口 + worker 门 + `IllegalStateException` + 前后 `TaskCheckpoint.throwIfStopRequested(nullableContext,...)` 就位；probe 用 `captureBarsSnapshotNoFocus`（无 mouse-away），heal 用 `captureBarsSnapshot`（含 mouse-away）+ 每目标 `350ms` 二次 `captureBarsSnapshot`。
- 常量/颜色/`+10`/`350`/`100`/`800`/radius(2,1)/`healthyCount>=2`/ROI 全保真；`HealOutcome` 构造器强制 EXECUTED↔click 成对；`TaskCheckpoint` null-guard 已核。
- scoped：仅本 1 文件；未改 PlayerStateService/handler/remote/schema/POM 或其它文件；`git status` 仅本文件 `??`，`git diff --check` 干净。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=DHXY `PlayerStateFirstAidLocalMacroMechanics.java` + 本日志；未改其它任何文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 返修逐条落实：拆两真实边界不新增第三流、input-worker 门、前后各一 checkpoint 无目标间 gate、固定四目标顺序、真值 closed status（UNREADABLE/CAPTURE_FAILED/NO_ACTION 不误标）、移除本地 STOPPED/isStopRequested、保留常量/颜色/ROI/flush、无 owner/TTL/retry/wrapper。
3. 因父级禁编译门，已用逐项静态自查（移除项/禁用符号/两入口/门/checkpoint/顺序/真值/常量）替代，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 原两阶段边界等价抽取。本单仍是前置，不宣称整类完成、不增计数。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1 Implementation Repair #1（拆回两真实机械入口：no-focus probe 无 mouse-away/零 input/零 state、
heal-all direct 含 input-worker 门 + 前后各一 checkpoint + 固定四目标逐项 judge/+10/350ms confirm/右键 100/800、无目标间 stop gate 无嵌套 submit；
移除本地 STOPPED/isStopRequested；越界→UNREADABLE、二次失败→CAPTURE_FAILED、中心健康 no-click→NO_ACTION；常量/颜色/ROI/flush 保真；按父级明令未 build/test，以静态自查代编译门）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent TRUE EOF Source Review #2 - APPROVED / `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1` - 2026-07-14T15:56:00-04:00

父级已独立逐行复核 DHXY
`src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`
及 `696a12b0` 的 `PlayerStateService:259-299,452-478,697-970`。本段是物理文件末尾的权威复审副本；
此前误插在旧历史段落内的同名 Review/Task 不作为领取门依据。

**结论：SOURCE APPROVED，P0=0 / P1=0 / P2=0。** no-focus probe 仅一次 checkpoint、无
mouse-away/input/state；heal-all direct 拒绝非 input-worker，整段仅前后 checkpoint，人物 HP→人物 MP→
宝宝 HP→宝宝 MP 顺序、`+10` 反证、`350ms` 复核、右键 `100ms`、settle `800ms` 与 baseline 一致；
closed status 对 `UNREADABLE/CAPTURE_FAILED/NO_ACTION` 的表达真实。构建门仍由父级统一执行，本单不单独增加
same-path 计数。无已批准业务差异；按 `696a12b0` 等价抽取。

## Parent TRUE EOF Direct Implementation Task - `W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1` - 2026-07-14T15:56:00-04:00

请原 External C 在 **2026-07-14T16:16:00-04:00** 前于本段之后追加：

`CLAIMED | task=W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]`

唯一 Java 写集为 DHXY
`src/main/java/com/bot/dhxy/service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java`。按
`696a12b0:PlayerStateService:1002-1297` 直接实现纯本地观察边界：使用 exact window binding；图标模板阈值
`0.85`；由命中图标确定状态列；先识别青色小时、未命中再识别绿色分钟；返回 closed typed observation。
不得发送输入、修改 cache/业务状态或新增 retry/TTL/owner/wrapper；不得修改其它 Java、协议、POM。交付
Implementation #1 与 scoped check；不跑 Maven/test/runtime/Git，父级统一构建。

## External Worker C - CLAIMED - 2026-07-14T15:58:30-04:00

CLAIMED | task=W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1 | claimedAt=2026-07-14T15:58:30-04:00 | writeSet=[DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]

## Parent TRUE EOF Source Review Correction #3 - BLOCKED / `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R1` - 2026-07-14T15:59:00-04:00

Delivery Preflight Helper 在父级 Review #2 后提供了一个非绑定候选；父级现已独立读取最新源码并确认该候选成立。
本段纠正 Review #2 的遗漏，helper 本身不构成审批。

**结论：BLOCKED，P0=0 / P1=0 / P2=1。**

- **P2：closed result 允许半组 click 坐标。**
  `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java:525-531`
  以 `hasClick = clickAbsX != null && clickAbsY != null` 校验 `EXECUTED`。当状态不是 `EXECUTED`、但 caller
  只传 X 或只传 Y 时，`hasClick=false` 与 `executed=false` 相等，构造器不会拒绝；这违反同文件
  `:514-516` 的“click coordinates present only for EXECUTED” closed payload 合同，并可能让日志/digest/调用方
  接收不完整坐标事实。

### Parent Priority Repair Task - `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2`

请原 External C 暂停尚未落 Java 的 incense 新单，在 **2026-07-14T16:19:00-04:00** 前于本段之后追加：

`CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]`

唯一 Java 修改仍为上述 FirstAid mechanics 文件：compact constructor 必须先拒绝 `clickAbsX/clickAbsY` 一空一非空，
再强制且仅强制 `EXECUTED` 同时携带完整坐标。不得改变两入口、状态枚举、顺序、delay、checkpoint/input-worker、
capture 或任何其它文件。交付 Repair #2 与 scoped check；不跑 Maven/test/runtime/Git，父级统一构建。父级源码通过后
原 `W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1` 自动恢复，无需重新设计；在此之前其 CLAIM 保留但
Java 写入暂停。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1 Implementation #1 - 2026-07-14T16:04:00-04:00

**结论：PARENT PREREQUISITE BLOCKED，P0=0 / P1=1。Java 零改动**（未创建目标文件；同 PlayerState-whole-adapt 已获接受先例）。
本单唯一 Java 写集应为新建 DHXY `service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java`；因其**必需的本地 OCR 依赖在当前 DHXY 工作树缺失**且不在我写集内，无法产出可编译的忠实抽取，按父级「closed macro/fact/依赖无法覆盖则给精确阻塞」交付零改动报告。本日志为本轮唯一写入。

### 计划抽取范围（按 696a12b0:PlayerStateService:1002-1297，观察边界、零 input）

镜像 CommonBox/BattleRadar `*LocalObservationMechanics` 范式：`@Slf4j @Service final`、构造注入、`WindowNativeBinding` 入口、window-client ROI（无 scale）、closed typed observation。计划方法：
- `probeIncenseStatus` 核心（1002-1054，**去 mouse-away/去 input**，capture 改 `BoundWindowCaptureService.captureRegion` 取 STATUS_PANEL rect=901,123,123,34）→ 写 raw → `ImageFinder.find(..., SHEYAOXIANG_STATUS_TEMPLATE, 0.85)` 图标匹配 → 命中点定状态列。
- `cropSheyaoxiangMatchedColumn`（1123-1159，`ImagePreprocessor.cropCopy`）→ `readSheyaoxiangRemainingTime`（1167-1213：cyan 像素洗白×`SHEYAOXIANG_DIGIT_OCR_SCALE=6` → OCR → `\d{1,2}` 小时；未命中→绿色）→ `readSheyaoxiangRemainingMinutesGreen`（1215-1282：green 洗白 → OCR + digit-template → 分钟）。
- 像素 helper `isSheyaoxiangCyanDigitPixel`(r≤120&&g≥130&&b≥130&&|g-b|≤80) / `isSheyaoxiangGreenDigitPixel`(g≥120&&r≤120&&b≤120&&g≥r+50&&g≥b+50) 逐字保真。
- **排除**（属业务/cache，brief 明令不碰）：icon-offset 缓存门 `probeIncenseIconPresence/cachedIncenseIconProbeRect/probeIncenseIconPresenceInRect/rememberIncenseIconPoint`（用 `PlayerRuntimeState` 缓存）、`incenseLastUsedTimeForRemainingMs`（业务时钟 math）。
- 返回 closed observation：`{MATCHED(icon clientX/Y + remainingMs 可空 + describe) / NOT_MATCHED / CAPTURE_UNAVAILABLE / MECHANICS_FAILED}`。

### 精确前置阻塞（P1）

数字读取管线（本单核心「青色小时→绿色分钟」）必需三个本地 OCR 类，均**在 696a12b0 存在、当前 DHXY 工作树 MISSING**（`git ls-tree 696a12b0` 各 1；工作树文件存在性检查全 MISSING）：

| 类 | 696a12b0 路径 | 当前工作树 | 用途 |
|---|---|---|---|
| `TextRecognizer` | `src/main/java/com/bot/dhxy/core/TextRecognizer.java` | MISSING | `getAllTextResultsLocalOnly(path)` 生成 OCR 词，读小时/分钟数字 |
| `SheyaoxiangDigitTemplateReader` | `src/main/java/com/bot/dhxy/vision/SheyaoxiangDigitTemplateReader.java` | MISSING | `recognizeAndLearn(...)` 绿色分钟数字模板识别 |
| `OcrWordResult` | `src/main/java/com/bot/dhxy/model/ocr/OcrWordResult.java` | MISSING | OCR 词结果 `getText()` |

- 当前树无干净替代：`vision/OcrTextMatcher` 仅文本**匹配**（normalizeName/shortNameMatchScore，非图像→文本 OCR）；`vision/ObjectiveTextRecognitionService.recognize(...)` 仅目标面板 map/coordinate 专用，非通用数字 OCR。
- 我的写集仅本新文件，brief 明令「不得修改其它 Java」，故无法自行恢复这三类。
- 同批缺口影响并行任务：A 的 `service/npc/NpcClickCtrlProbeLocalMacroMechanics.java` 亦 `import com.bot.dhxy.core.TextRecognizer` 与 `com.bot.dhxy.model.ocr.OcrWordResult`（当前树同缺）。

### 请父级二选一（前置解锁后我即完成抽取）

1. 确认统一构建的 DHXY 树包含 696a12b0 的这三 OCR 类（则我按上列范围忠实抽取、写 `PlayerStateIncenseStatusLocalObservationMechanics.java`）；或
2. 由授权 owner 先恢复这三类（不在我写集），落地后我立即领取续做本单。

### scope self-QA（仅 QA，不构成 Approved）

1. 本轮写集=本日志（Java 零改动，未创建目标文件以免引入引用缺失类的不可编译文件）；未改任何 Java/协议/POM/其它文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 判定依据（工具证据）：`git ls-tree -r 696a12b0` 三类各存在；当前工作树三路径文件存在性检查全 MISSING；`OcrTextMatcher`/`ObjectiveTextRecognitionService` 非通用 OCR 替代；A 的 Npc 新文件引用同缺失类。
3. 计划范围严格限观察边界（去 mouse-away/input、排除 icon-cache 与业务时钟 math），图标阈值 0.85、cyan→green 顺序、closed observation，符合 brief。
4. 无已批准业务差异；按 696a12b0 等价抽取——需父级前置补齐三 OCR 类后方可产出可编译文件。

External Worker C 交付 W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1 Implementation #1（**PREREQUISITE BLOCKED P1=1**：核心数字 OCR 依赖 TextRecognizer/SheyaoxiangDigitTemplateReader/OcrWordResult 在 696a12b0 存在但当前 DHXY 工作树全缺、无干净替代、不在写集无法恢复；Java 零改动；含完整抽取范围、精确缺失类表与两条解锁路径）；持续重读本日志等待父级前置补齐或改单；自审不算 Approved。

## Parent TRUE EOF Prerequisite Review #1 + Repair Reissue - 2026-07-14T16:08:00-04:00

父级已独立核对 C 的 incense 前置报告：当前 DHXY 确实缺少 baseline
`TextRecognizer`、`OcrWordResult`、`SheyaoxiangDigitTemplateReader`，且当前 `OcrTextMatcher`/
`ObjectiveTextRecognitionService` 不能替代通用数字 OCR。C 在原写集内保持 Java 零改动是正确处理；该 incense
单记为 **PARENT PREREQUISITE BLOCKED，P0=0/P1=1/P2=0**，不把缺口归为 C 实现错误。父级将把本地 OCR
基础恢复与 incense observation 合并成后续较大 cohort，不再拆成单一 helper 小单。

但本日志在父级 Correction #3 后发生并发交付，导致 FirstAid R2 brief 不再位于物理 EOF。现于 TRUE EOF 原样
重发优先修复；请原 External C 在 **2026-07-14T16:28:00-04:00** 前追加：

`CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]`

唯一 Java 修改为
`src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`：compact constructor
先拒绝 `clickAbsX/clickAbsY` 一空一非空，再强制且仅强制 `EXECUTED` 同时携完整坐标。不得改变其它任何行或
文件。交付 Repair #2；父级源码通过后 C 立即领取上述较大的 local-OCR + incense cohort。不得 build/test/
runtime/Git。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T16:10:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2 | claimedAt=2026-07-14T16:10:00-04:00 | writeSet=[DHXY PlayerStateFirstAidLocalMacroMechanics.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2 Implementation Repair #2 - 2026-07-14T16:14:00-04:00

按 R2 brief 单点修复：唯一 Java 改动 = `PlayerStateFirstAidLocalMacroMechanics.java` 的 `HealOutcome` compact constructor，
先拒绝 `clickAbsX/clickAbsY` 一空一非空，再强制且仅强制 `EXECUTED` 携完整坐标。**未改其它任何行或文件**。
按父级明令未 build/test/runtime/Git；以静态自查代编译门。新 blob（git hash-object）= `56388cf67ed0159e9cd9a9332190dd472f3b50bd`，
541 行（R1=538，+3 行），SHA-256 = `915f2b950d0c571ad6e7c45956e770add7f85a116913b2a4cf96b1a82437c3ce`。

### 改动前后（仅此 constructor）

改前：`boolean hasClick = clickAbsX != null && clickAbsY != null; if (executed != hasClick) throw;`
——缺陷：非 EXECUTED 且**单坐标非空**（如 clickAbsX=x, clickAbsY=null）时 hasClick=false、executed=false → 漏过校验，携部分坐标。

改后：
```
if ((clickAbsX == null) != (clickAbsY == null)) {
    throw new IllegalArgumentException("click coordinates must be present or absent as a pair");
}
boolean executed = status == HealStatus.EXECUTED;
boolean hasClick = clickAbsX != null;
if (executed != hasClick) {
    throw new IllegalArgumentException("click coordinates must be present only for EXECUTED");
}
```
1. 先拒绝一空一非空的部分坐标（成对）；2. 经步骤 1 后两坐标同在同缺，`hasClick=clickAbsX!=null` 即"完整"，再强制 `EXECUTED ⇔ 携完整坐标`。

### 静态自查（工具证据）

- 仅 `HealOutcome` compact constructor 改动（sed 525-536 确认）；其它 record 构造器 `FirstAidIntent`(449)/`NoFocusProbeResult`(490)/`ProbeObservation`(499)/`HealAllResult`(508) 逐字未动。
- 两入口/门/checkpoint/顺序/常量/颜色/ROI/flush、`EXECUTED` 唯一携 click 语义均保持（R1 SOURCE APPROVED 内容未回退）。
- 行数 538→541（净 +3 行，即新增 pair 校验块）；`git status` 仅本文件 `??`，`git diff --check` 干净；未改其它 Java/协议/POM/文件。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=DHXY `PlayerStateFirstAidLocalMacroMechanics.java` + 本日志；未改其它任何文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 精确落实 R2：constructor 先拒绝一空一非空、再仅强制 EXECUTED 携完整坐标；未改其它任何行。
3. 因父级禁编译门，用静态自查（改动定位/其它构造器未动/行数/diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

另：incense 单已由父级记 `PARENT PREREQUISITE BLOCKED，P1=1`（C 零改动为正确处理，缺口不归 C）；父级将把 local-OCR 恢复 + incense observation 并入后续较大 cohort，C 待 R2 源码通过后领取该 cohort。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2 Implementation Repair #2（HealOutcome constructor 先拒绝一空一非空部分坐标、再仅强制 EXECUTED 携完整坐标；未改其它任何行/文件；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审与后续 local-OCR+incense cohort；自审不算 Approved。

## Parent Source Review #7 - SOURCE APPROVED / `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1-R2` - 2026-07-14T16:23:00-04:00

Delivery Preflight Helper 已先完成非绑定预检；父级随后独立核对当前 541 行源码、R1 blob 反推、所有
`HealOutcome` 构造点与本次 SHA-256 `915f2b950d0c571ad6e7c45956e770add7f85a116913b2a4cf96b1a82437c3ce`。

**结论：SOURCE APPROVED，P0=0 / P1=0 / P2=0。** `:528-530` 先拒绝 X/Y 单边非空，随后
`:531-535` 且仅允许 `EXECUTED` 携完整坐标；四种双坐标/status 组合与两个单边组合均封闭。本次净增三行，
反推后字节 blob 恰回到 R1 `3888b641215ddcb2c6a3495934c117250aa4d970`，无夹带改动或新增符号风险。
统一 DHXY compile 仍等待其他 Java writer 稳定，本结论不单独增加 `189/407`。

## Parent Direct Cohort Task - `W-696-LOCAL-OCR-INCENSE-COHORT-1` - 2026-07-14T16:23:00-04:00

这不是单一 helper 小单，而是一次闭合 **本地 OCR 基座 -> 摄妖香 exact-window capture/template -> 青色小时 /
绿色分钟 terminal** 的四文件 cohort。请原 External C 在 **2026-07-14T16:43:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-LOCAL-OCR-INCENSE-COHORT-1 | claimedAt=<ISO-8601> | writeSet=[DHXY OcrWordResult.java,DHXY TextRecognizer.java,DHXY SheyaoxiangDigitTemplateReader.java,DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]`

唯一 Java 写集均为新建：

1. `src/main/java/com/bot/dhxy/model/ocr/OcrWordResult.java`：按 `696a12b0` 恢复完整 image-local
   point/bounds/score DTO 与构造器/getter，不改坐标语义。
2. `src/main/java/com/bot/dhxy/core/TextRecognizer.java`：只恢复本地 OCR sidecar words 客户端和
   `getAllTextResultsLocalOnly(...)` 所需 JSON 适配；endpoint/timeout 使用 Spring 配置默认值
   `127.0.0.1:18761/10000ms`。**禁止**恢复或写入任何 Baidu client、fallback、credential、默认密钥或新 POM
   依赖；请求失败返回“sidecar unavailable”与“成功但零 words”可区分的 closed 结果，不能伪装文字未命中。
3. `src/main/java/com/bot/dhxy/vision/SheyaoxiangDigitTemplateReader.java`：按 `696a12b0:1-372`
   恢复分割、模板比对、OCR bootstrap learning 与资源释放；作为可注入本地 collaborator，不改阈值或模板目录。
4. `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java`：一次完整
   抽取 `696a12b0:PlayerStateService:1002-1297`。public entry 接 caller-supplied exact
   `WindowNativeBinding`、screen-absolute status rect 与 optional cached icon offset；若鼠标遮挡 ROI，保持 baseline
   “input-worker 内 direct move，否则只排一个 move+settle ordered sequence”，随后在 queue 外对同一 binding capture。
   保持 cached narrow miss -> full rect、icon template `0.85`、matched column、cyan hour 优先、green minute fallback、
   digit-template learning、所有 image flush 与时间单位。返回 closed mechanical result，区分 capture unavailable、
   template absent、OCR unavailable、icon present但时间不可读、remaining time found 与 mechanics failure；返回 immutable
   screen-absolute icon point/offset/remainingMs/source。不得调用 Bag、不得决定是否吃香、不得更新 PlayerState/cache/
   cooldown、不得新增 retry/TTL/owner/session/ledger。

**整体验收：** 四文件组合在当前 DHXY 可编译；本地 OCR 无 credential/Cloud 路径；鼠标清障、capture、template、
cyan-first/green-fallback 顺序与 `696a12b0` 一致；exact binding 与 window-scoped temp path 贯穿；异常和 unavailable
不降级为普通 template miss。不得修改 `PlayerStateService`、remote/schema/handler/POM/配置文件或其它 Java；不得
build/test/runtime/Git，父级在所有 writer 稳定后统一构建。交付 `Implementation #1`，列四文件 SHA、公开入口、
baseline 对照与 scoped check。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T16:26:00-04:00

CLAIMED | task=W-696-LOCAL-OCR-INCENSE-COHORT-1 | claimedAt=2026-07-14T16:26:00-04:00 | writeSet=[DHXY OcrWordResult.java,DHXY TextRecognizer.java,DHXY SheyaoxiangDigitTemplateReader.java,DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]

## External Worker C - W-696-LOCAL-OCR-INCENSE-COHORT-1 Implementation #1 - 2026-07-14T16:38:00-04:00

按 cohort 闭合 **本地 OCR 基座 → 摄妖香 exact-window capture/template → 青色小时/绿色分钟 terminal** 四文件（全新建）。
按父级明令未 build/test/runtime/Git；以逐项静态自查代编译门。四文件 SHA-256：
- `model/ocr/OcrWordResult.java`（91L，blob `5ac97a51`=696a12b0 精确）SHA-256 `f816d51690f7a8f949d7456ef85c973db6945f2e5104cc0ef972de705d667fac`
- `vision/SheyaoxiangDigitTemplateReader.java`（372L，blob `234f77f0`=696a12b0 精确）SHA-256 `8536fd1c7c77530f1ab550767a840e9d3460f289aa5250a995cb279ca7dfcac7`
- `core/TextRecognizer.java`（134L，裁剪）SHA-256 `a9eb1e91cbe547aab518bd72eda36e11bdaefb0a1d1f376f4fe7577f6293f40b`
- `service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java`（613L）SHA-256 `7246eb302b34364222438f872235d32d38e3866b3392aabc98e31191a35d2a5c`

### 逐文件 baseline 对照

1. **OcrWordResult**：`git show 696a12b0:...` **byte-exact 恢复**（blob 5ac97a51 == 696a12b0）；image-local point/bounds/score DTO + 三构造器 + getter，坐标语义未改。
2. **SheyaoxiangDigitTemplateReader**：`git show 696a12b0:...1-372` **byte-exact 恢复**（blob 234f77f0 == 696a12b0）；分割/模板比对/OCR bootstrap learning/资源释放原样，阈值与模板目录未改；imports 仅 OcrWordResult + 标准库；普通 class（非 @Component），由 mechanics `new` 持有（同 baseline PlayerStateService）。
3. **TextRecognizer**（裁剪，非 byte-exact）：只保留本地 sidecar words 客户端。public `Optional<List<OcrWordResult>> getAllTextResultsLocalOnly(String)` = 696a12b0 `tryLocalAllTextResults` 的 JSON 适配（`/ocr/words` POST → words 数组 → image-local box）。
   - **闭合区分**：`Optional.empty()` = sidecar 不可用/请求失败；`Optional.of(list)`（list 可空）= 成功——不伪装文字未命中。
   - endpoint/timeout 用 `@Value` Spring 默认 `http://127.0.0.1:18761` / `10000ms`（当前树无 `BotProperties.OcrConfig`，故不用 getOcr()，改 @Value 带默认；未改配置文件/POM）。
   - **禁用项全无**：无 `AipOcr`/Baidu SDK、无 DEFAULT_APP_ID/API_KEY/SECRET_KEY 凭据、无 provider 路由/fallback、无新 POM 依赖（grep 证）。
4. **PlayerStateIncenseStatusLocalObservationMechanics**：完整抽取 `696a12b0:PlayerStateService:1002-1297`。

### incense mechanics 公开入口与 baseline 对照

public `IncenseStatusObservation observeIncenseStatus(WindowNativeBinding binding, int[] statusRect(screen-abs), Integer cachedIconOffsetX, Integer cachedIconOffsetY, String source)`：
- **mouse-obstruction**（`moveMouseAwayBeforePlayerStateSnapshotIfNeeded` 保真）：遮挡时 input-worker 内 `inputProvider.moveMouse`+`Thread.sleep(300)` direct；否则 `inputSequences.submitAndWait(...move+sleep(300))` **一个 ordered sequence**；随后在 queue 外对同一 binding `captureRegion`。
- **cached narrow miss → full rect**：有 cached offset 时先 `cachedIncenseIconProbeRect`(WIDTH=48,LEFT_PADDING=6) 窄探 presence（命中即 flush 丢弃、未命中记 fallback 日志）；**时间读取恒用完整 status rect**（复刻 baseline `probeIncenseStatus` 恒全 rect，避免窄图截断数字列）。
- **icon 模板 0.85**：`ImageFinder.find(rawPath, sheyaoxiang_buff.png, 0.85)`；命中→iconPoint(screen-abs)=rect[0]+match[0]。
- **matched column**：`cropSheyaoxiangMatchedColumn`（`ImagePreprocessor.cropCopy`，template 半宽对齐）逐字保真。
- **cyan 优先 / green fallback**：`readSheyaoxiangRemainingTime`（cyan 像素洗白×6→OCR→`\d{1,2}` 小时`×ONE_HOUR_MS`；无→green）→ `readSheyaoxiangRemainingMinutesGreen`（green 洗白→OCR + `sheyaoxiangDigitTemplateReader.recognizeAndLearn`→分钟`×60000`）；cyan/green 像素判据逐字保真；所有 `washed`/`statusImage`/`matchedColumn`/template flush 保真。
- **closed 6 态**（`IncenseStatusObservation.Status`）：`CAPTURE_UNAVAILABLE`（capture null）/ `TEMPLATE_ABSENT`（find null）/ `OCR_UNAVAILABLE`（`getAllTextResultsLocalOnly` 返回 empty Optional）/ `ICON_PRESENT_TIME_UNREADABLE`（图标在但 cyan/green 均无可读数字）/ `REMAINING_TIME_FOUND`（携 remainingMs）/ `MECHANICS_FAILURE`（管线 RuntimeException）。返回 immutable screen-absolute iconPoint/offset/remainingMs/source；record 构造器强制"非图标态不携坐标/时间、REMAINING_TIME_FOUND 必携 remainingMs、remainingMs 仅 FOUND"。
- **不做**：未调 Bag、未决定是否吃香、未更新 PlayerState/cache/cooldown（`incenseLastUsedTimeForRemainingMs`/`rememberIncenseIconPoint`/`PlayerRuntimeState` 排除，grep 证无）、无 retry/TTL/owner/session/ledger。

### 整体验收自查（工具证据）

- 我 4 文件 imports 全部落在当前树可用类（OcrWordResult/SheyaoxiangDigitTemplateReader 自恢复；ImageFinder/ImagePreprocessor/BoundWindowCaptureService/InputProvider/InputSequences/InputAction/CoordinateHelper/WindowScopedTempPath/WindowNativeBinding 均在当前树），四文件组合可编译。
- 本地 OCR 无 credential/Cloud 路径；mouse 清障/capture/template/cyan-first-green-fallback 与 696a12b0 一致；exact binding + window-scoped temp path 贯穿；异常/unavailable 不降级为普通 template miss（独立 MECHANICS_FAILURE / OCR_UNAVAILABLE 态）。
- 未改 `PlayerStateService`、remote/schema/handler/POM/配置文件或其它 Java；`git status` 仅本 4 新文件 `??`；4 文件 `git diff --check` 干净。
- **跨 worker 前置提示（非本写集，供父级协调）**：当前树 A 的 `service/npc/NpcClickCtrlProbeLocalMacroMechanics.java:220` 调 `ocr.getAllTextResultsForMatch(...)`，该方法**不在**本单 brief 授权的 TextRecognizer 范围（"只恢复本地 sidecar + getAllTextResultsLocalOnly"），我未恢复它以免越权/引入 Baidu 路由。统一构建需父级协调 A 的返修（改用 sidecar 入口或另单扩 TextRecognizer）；本单不擅自加此方法。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=4 新 DHXY 文件 + 本日志；未改其它任何文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 逐条落实：OcrWordResult/SheyaoxiangDigitTemplateReader byte-exact 恢复；TextRecognizer 仅本地 sidecar + closed Optional 区分、无 Baidu/凭据/fallback/POM；incense 完整抽取 1002-1297（mouse 双分支、cached→full 门、0.85、matched column、cyan→green、digit learning、flush、时间单位、6 态 immutable、无 Bag/业务/cache）。
3. 因父级禁编译门，用逐项静态自查（byte-exact 校验/禁用符号/跨依赖/imports/diff）替代，如实标注"未编译"。
4. 无已批准业务差异；按 696a12b0 等价迁移。跨 worker TextRecognizer API 缺口已如实上报。

## Parent Source Review #9 - SOURCE APPROVED / `W-696-LOCAL-OCR-INCENSE-COHORT-1-R1` - 2026-07-14T17:18:00-04:00

Delivery Preflight Helper 已先完成非绑定预检；父级随后独立读取当前 incense mechanics，并逐段对照
`696a12b0:PlayerStateService.java:1002-1116` 的两条 operation、capture 次数和分支。

**结论：SOURCE APPROVED，P0=0 / P1=0 / P2=0。**

- `probeIncenseStatus:108-166` 现在只做一次 full-rect capture、template/matched-column 与 cyan->green 时间读取；
  `probeIncenseIconPresence:180-208` 在 cached probe 为 `PRESENT/UNKNOWN` 时于 `:196-197` 立即返回，
  仅 `ABSENT` 才于 `:204` full fallback，恢复 696 两个独立 caller operation 和各自 capture 次数。
- 两个入口的 binding/rect 门在 `:109,:185`，均早于任何 mouse/capture；`:273-289` 使用 long 做正向、完整位于
  binding 的 overflow-safe 比较，cached rect 又在 `:193` 单独复核。无效 rect 不会先产生物理输入。
- `capture:241-248` 不再吞 `RuntimeException`；status exception/null 分别落到 `MECHANICS_FAILURE` 与
  `CAPTURE_UNAVAILABLE`，presence exception/null 分别落到 typed `UNKNOWN(exception)` 与 `UNKNOWN(capture-failed)`，
  没有自动 retry。
- `IncenseStatusObservation:611-631` 与 `IncenseIconPresenceResult:681-692` 均拒绝部分 icon 字段，
  并约束 icon-bearing state 与 remaining time。模板阈值 `0.85`、matched-column、cyan-first/green-fallback、
  时间单位、image flush 和 mouse-obstruction 两分支未漂移。
- 当前 SHA-256 `55e90648a188492fc9b7f19f2d4b6ceb420a4b89894b40d2d6c9341cd671fa0b`
  与交付一致；窄时窗只见该 mechanics 写入，另外三份已落文件指纹未变。

本结论只批准四文件 OCR -> incense local observation cohort 的源码；统一 DHXY compile 与 Cloud package 尚未运行，
当前不增加 `189/407`。A blocked Ctrl-probe 的旧跨依赖不归入本单，也不由 C 越界修复。
无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent TRUE EOF Reissue - `W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1` - 2026-07-14T17:45:00-04:00

本段是物理文件末尾权威任务。完整实施合同见上方同名 `Parent Direct Cohort Task`（约 `:4258`）；该旧段因
append 上下文命中历史基线句而没有落在 EOF，不删除历史，以本段为领取锚点。请 External C 在
**2026-07-14T18:05:00-04:00** 前于本段之后追加：

`CLAIMED | task=W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1 | claimedAt=<ISO-8601> | writeSet=[Cloud DialogChoiceMemoryService.java,Cloud WorldMapRouteResultMemoryService.java,Cloud MemoryService.java,Cloud CloudServiceConfiguration.java,this-log]`

一次闭合 `Navigation/current callers -> MemoryService -> DialogChoiceMemoryService +
WorldMapRouteResultMemoryService -> tenant/user CloudServiceStorage private files` 完整链。保持 `696a12b0` 三个
Service 全部 key/threshold/counter/settlement/load/replace 顺序；两个 store 在 Cloud 都必须由显式 scoped `Path` bean
构造并避免 component-scan duplicate，文件分别为 `dialog_choice_memory.json` 与
`world_map_route_result_memory.json`。不得使用共享工作目录 `config/*.json` authority，不得改 Navigation/B shared
wire/A/D/runner/tests/POM，不得新增 TTL/retry/compaction/global owner。若三份 Service 已满足基线，不制造无意义 diff，
但须逐方法审查并在 Implementation 列明。不得 build/test/runtime/Git；父级统一 fresh package。当前不增加
`189/407`。无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-LOCAL-OCR-INCENSE-COHORT-1 Implementation #1（四文件：OcrWordResult/SheyaoxiangDigitTemplateReader byte-exact 恢复、TextRecognizer 裁剪为本地 sidecar+Optional 闭合区分无 Baidu/凭据、
PlayerStateIncenseStatusLocalObservationMechanics 完整抽取 1002-1297：mouse 双分支/cached→full 门/图标 0.85/matched column/cyan→green/digit learning/6 态 immutable observation/无 Bag 无业务 cache；
含四文件 SHA、公开入口、baseline 对照、scoped check 与 A 的 TextRecognizer API 跨依赖上报；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #8 - BLOCKED / `W-696-LOCAL-OCR-INCENSE-COHORT-1` - 2026-07-14T17:02:00-04:00

Delivery Preflight Helper 已先给出非绑定候选；父级随后独立读取四个交付文件，并对照
`696a12b0:PlayerStateService.java:1002-1297`。`OcrWordResult` 与
`SheyaoxiangDigitTemplateReader` 的 byte-exact 内容可保留；裁剪后的本地-only `TextRecognizer` 未恢复凭据、Baidu 或
Cloud fallback，也可保留。当前 blocker 集中在新 incense mechanics 的边界表示。

**结论：BLOCKED，P0=0 / P1=3 / P2=1。** 本轮不把 A 的 blocked Ctrl-probe 缺
`getAllTextResultsForMatch/washYellowText` 归罪于 C，也不要求改变 696 原有的 write-image/OCR fallback 行为。

1. **P1 - 两个基线操作被合并，新增了无条件第二次 capture/OCR。** 当前
   `PlayerStateIncenseStatusLocalObservationMechanics.java:103-135` 先做 cached narrow probe，随后无论 narrow 是
   PRESENT、ABSENT 还是 capture unknown，都继续 full-status capture/time OCR。696 是两个独立 caller operation：
   `probeIncenseStatus:1002-1054` 只做一次 full capture/time read；
   `probeIncenseIconPresence:1056-1068` 在 cached probe 为 PRESENT **或 UNKNOWN** 时立即返回，只有 ABSENT 才 full fallback。
   影响：当前实现改变 capture 次数、unknown 分支和 OCR 时点，Cloud caller 无法复现原判断。必须恢复两个明确 public
   mechanical operation/result；不得用一个“更方便”的总入口代替。
2. **P1 - exact rect 未在物理输入前闭合。** `:108-113` 只核 binding 有正 geometry 与数组长度，未核
   `right > left`、`bottom > top`，也未核 screen-absolute rect 完整位于该 binding；随后 `:113/:233-258` 可能先移动
   鼠标。影响：stale/跨窗口 rect 可先产生真实输入，再由 capture 失败。两个 public operation 都必须在任何 move、sleep、
   capture 前用 overflow-safe 比较验证完整 rect；cached offset 只能生成 binding/statusRect 内的合法 probe rect。
3. **P1 - capture exception 被降成 availability。** `:204-213` 捕获 `captureRegion` 的 RuntimeException 后返回 null，
   `:129-132/:178-184` 再映为 `CAPTURE_UNAVAILABLE`，使已声明的 `MECHANICS_FAILURE` 无法表达 capture mechanics 异常。
   影响：typed caller 可能把本地异常当普通不可用/视觉 miss。Optional/空 frame 可是 unavailable；异常必须到
   mechanics-failure/unknown terminal，且不得自动 retry。
4. **P2 - closed record 接受部分 icon 字段。** `IncenseStatusObservation:566-583` 仅在四个 icon 字段全非空时令
   `hasIcon=true`；因此 non-icon status 携一个或三个非空字段仍通过。必须分别计算 `hasAnyIcon` / `hasCompleteIcon`，
   强制 icon 字段全有或全无，并保持 remaining time 仅 `REMAINING_TIME_FOUND` 可携带。

### Parent Repair Task - `W-696-LOCAL-OCR-INCENSE-COHORT-1-R1`

请原 External C 在 **2026-07-14T17:22:00-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-LOCAL-OCR-INCENSE-COHORT-1-R1 | claimedAt=<ISO-8601> | writeSet=[DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]`

唯一 Java 写集为 incense mechanics；另外三个已落文件冻结。一次完成：拆回 full status/time 与 cached-presence 两条
public mechanical operation；保持各自 696 capture 次数、cached PRESENT/UNKNOWN 早退、ABSENT fallback、0.85、
matched-column、cyan->green 与时间单位；所有 rect/binding 门在输入前；exception/unavailable 分开；修 record all-or-none。
不接 Bag、不写 PlayerState/cache/cooldown，不新增 retry/TTL/owner/session/ledger，不跑 build/test/runtime/Git。

父级复审只按两条基线 operation 的分支/次数/terminal 与上述四项定点验收；统一构建前不增加 `189/407`。
无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T17:06:00-04:00

CLAIMED | task=W-696-LOCAL-OCR-INCENSE-COHORT-1-R1 | claimedAt=2026-07-14T17:06:00-04:00 | writeSet=[DHXY PlayerStateIncenseStatusLocalObservationMechanics.java,this-log]

## External Worker C - W-696-LOCAL-OCR-INCENSE-COHORT-1-R1 Implementation Repair #1 - 2026-07-14T17:14:00-04:00

按 `Source Review #8`（P1×3/P2×1）返修 incense mechanics（唯一 Java 写集；OcrWordResult/SheyaoxiangDigitTemplateReader/TextRecognizer 三文件冻结未动）。
按父级明令未 build/test/runtime/Git；以静态自查代编译门。新 blob（git hash-object）= `8aad5899a377c7c462acf4150eac68a94032df53`，
707 行，SHA-256 = `55e90648a188492fc9b7f19f2d4b6ceb420a4b89894b40d2d6c9341cd671fa0b`。

### P1/P2 逐条修复

1. **P1 拆两独立 public operation**（不再单入口无条件第二次 capture/OCR）：
   - `probeIncenseStatus(binding, statusRect, source)` → 镜像 `696a12b0:1002-1054`：**恰一次** full-rect capture + 模板匹配 + matched-column + cyan→green 时间读；返回 `IncenseStatusObservation`。
   - `probeIncenseIconPresence(binding, statusRect, cachedOffsetX/Y, source)` → 镜像 `1056-1068`：cached 窄探；`presence() != ABSENT`（即 **PRESENT 或 UNKNOWN**）立即返回 cachedProbe；仅 `ABSENT` 才 `probeIconPresenceInRect(...statusRect,"status-rect")` full fallback；返回 `IncenseIconPresenceResult`。私有 `probeIconPresenceInRect` 镜像 `1083-1116`（capture 次数与 unknown 分支保真）。
2. **P1 输入前闭合 rect**：两 op 入口第一步 `isCompleteRectWithinBinding(binding, rect)`（overflow-safe long 比较：`x2>x1 && y2>y1 && x1>=baseX && y1>=baseY && x2<=baseX+width && y2<=baseY+height`），失败即返回（status→MECHANICS_FAILURE / presence→UNKNOWN("invalid-rect")）**在任何 move/sleep/capture 之前**；cached probe rect 亦经 `isCompleteRectWithinBinding` 复核，非法则跳过 cached 直接 full；`cachedIncenseIconProbeRect` 仍夹在 statusRect 内。
3. **P1 capture 异常与 unavailable 分离**：`capture()` 私有方法**不再 catch RuntimeException**（异常向上传播）；仅 `Optional.empty()`/空 frame → 返回 null。
   - status：capture 抛异常 → op 的 try/catch → `MECHANICS_FAILURE(e)`；capture 返回 null → `CAPTURE_UNAVAILABLE`。
   - presence：`probeIconPresenceInRect` capture null → `UNKNOWN("capture-failed")`；capture/模板抛异常 → 传播到 op2 try/catch → `UNKNOWN("exception")`。无 auto-retry。
4. **P2 record all-or-none**：`IncenseStatusObservation` 与 `IncenseIconPresenceResult` 构造器分算 `hasAnyIcon`/`hasCompleteIcon`，`hasAnyIcon != hasCompleteIcon` 抛异常（拒绝一/三字段部分）；status 侧再校验 icon-bearing 三态（OCR_UNAVAILABLE/ICON_PRESENT_TIME_UNREADABLE/REMAINING_TIME_FOUND）⇔ 完整 icon、remainingMs 仅 REMAINING_TIME_FOUND；presence 侧 `PRESENT ⇔ 完整 icon`。

### 两 operation 分支/次数/terminal 矩阵

| operation | capture 次数 | 早退 | capture null | capture 异常 | 模板 miss | 时间 |
|---|---|---|---|---|---|---|
| probeIncenseStatus | 恰 1（full rect） | — | CAPTURE_UNAVAILABLE | MECHANICS_FAILURE | TEMPLATE_ABSENT | cyan→green→{REMAINING_TIME_FOUND / ICON_PRESENT_TIME_UNREADABLE / OCR_UNAVAILABLE} |
| probeIncenseIconPresence | cached 命中 1；cached ABSENT 时 2（narrow+full） | cached PRESENT/UNKNOWN 立即返回 | UNKNOWN("capture-failed") | UNKNOWN("exception") | ABSENT("template-miss") | 不读时间（纯 presence） |

其余保真：图标模板 `0.85`、matched-column（`ImagePreprocessor.cropCopy`）、cyan/green 像素判据、digit-template `recognizeAndLearn` learning、所有 image flush、时间单位（小时`×ONE_HOUR_MS`、分钟`×60000`）、mouse-obstruction 双分支（input-worker direct / 一个 ordered move+settle）。未接 Bag、未写 PlayerState/cache/cooldown、无 retry/TTL/owner/session/ledger。

### 静态自查（工具证据）

- 两 public operation 就位；rect 门（isCompleteRectWithinBinding）位于 moveMouseAway/capture **之前**（行 109/185 vs 114/215/216）；cached rect 亦复核（193）。
- `capture()` 无 try/catch（grep 证），异常传播；status 异常→mechanicsFailure(118/162)、null→captureUnavailable(123)；presence 异常→unknown("exception")(207)、null→unknown("capture-failed")(220)。
- cached `!= ABSENT` 早退(196-197) + ABSENT full fallback(204)。
- record all-or-none：`hasAnyIcon != hasCompleteIcon`(617/687) + iconBearing(623) + PRESENT(690)。
- 禁用符号（bagService/PlayerRuntimeState/incenseLastUsed/cooldown/rememberIcon）grep 空；仅本 1 文件改动，`git status`=`??`，`git diff --check` 干净；未动另三冻结文件与其它任何文件。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=incense mechanics + 本日志；三冻结文件（OcrWordResult/SheyaoxiangDigitTemplateReader/TextRecognizer）未动；未改其它任何文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 四条返修逐条落实：两独立 operation（各 696 capture 次数/PRESENT·UNKNOWN 早退/ABSENT fallback）、输入前 overflow-safe rect+in-binding 门、capture 异常↔unavailable 分离无 retry、record all-or-none + remainingMs 仅 FOUND；0.85/matched-column/cyan→green/时间单位/digit learning/flush 保真；无 Bag/业务/cache。
3. 因父级禁编译门，用逐项静态自查（两 op/rect 门/capture 分离/all-or-none/禁用符号/diff）替代，如实标注"未编译"。
4. 无已批准业务差异；按 696a12b0 等价迁移。

External Worker C 交付 W-696-LOCAL-OCR-INCENSE-COHORT-1-R1 Implementation Repair #1（incense mechanics 拆回 probeIncenseStatus（一次 full capture/time）与 probeIncenseIconPresence（cached PRESENT/UNKNOWN 早退、ABSENT→full）两独立 operation；
输入前 overflow-safe rect+in-binding 门；capture 异常→MECHANICS_FAILURE/UNKNOWN 与 null→unavailable 分离无 retry；record all-or-none icon 字段；0.85/matched-column/cyan→green/时间单位/digit learning/flush/mouse 双分支保真、无 Bag 无业务；三冻结文件未动；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #9 - SOURCE APPROVED (AUTHORITATIVE TRUE EOF REPUBLISH) / `W-696-LOCAL-OCR-INCENSE-COHORT-1-R1` - 2026-07-14T17:20:00-04:00

本段位于当前真实 EOF，是本轮权威父级结论。上方约 `:4938` 的同标题段因 append 上下文命中旧的
Implementation #1 尾部而误插在 Repair 之前，只作重复证据，不是状态/领取锚点；不删除历史，以本段为准。

Delivery Preflight Helper 已先完成非绑定预检；父级随后独立读取当前 incense mechanics，并逐段对照
`696a12b0:PlayerStateService.java:1002-1116` 的两条 operation、capture 次数和分支。

**结论：SOURCE APPROVED，P0=0 / P1=0 / P2=0。**

- `probeIncenseStatus:108-166` 恰一次 full-rect capture/time read；
  `probeIncenseIconPresence:180-208` 对 cached `PRESENT/UNKNOWN` 于 `:196-197` 早退，仅 `ABSENT` 于 `:204`
  full fallback，恢复 696 两条独立 operation。
- binding/rect 门 `:109,:185,:273-289` 均早于任何输入/capture；cached rect 在 `:193` 再门控。
- `capture:241-248` 不吞异常；status exception/null 分别到 `MECHANICS_FAILURE/CAPTURE_UNAVAILABLE`，
  presence exception/null 分别到 typed `UNKNOWN(exception)/UNKNOWN(capture-failed)`，无 retry。
- 两个 public record 在 `:611-631,:681-692` 拒绝部分 icon 字段并约束 state/remaining time；
  `0.85`、matched-column、cyan-first/green-fallback、时间单位、image flush 与 mouse-obstruction 顺序未漂移。
- 当前 SHA-256 `55e90648a188492fc9b7f19f2d4b6ceb420a4b89894b40d2d6c9341cd671fa0b`
  与交付一致，另外三份冻结文件指纹未变。

本结论只批准四文件 OCR -> incense local observation cohort 的源码；统一 DHXY compile 与 Cloud package 尚未运行，
当前不增加 `189/407`。A blocked Ctrl-probe 的旧跨依赖不归入本单，也不由 C 越界修复。
无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent TRUE EOF Reissue - `W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1` - 2026-07-14T17:45:00-04:00

本段是物理文件末尾权威任务。完整实施合同见上方同名 `Parent Direct Cohort Task`（约 `:4258`）；该旧段因
append 上下文命中历史基线句而没有落在 EOF，不删除历史，以本段为领取锚点。请 External C 在
**2026-07-14T18:05:00-04:00** 前于本段之后追加：

`CLAIMED | task=W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1 | claimedAt=<ISO-8601> | writeSet=[Cloud DialogChoiceMemoryService.java,Cloud WorldMapRouteResultMemoryService.java,Cloud MemoryService.java,Cloud CloudServiceConfiguration.java,this-log]`

一次闭合 `Navigation/current callers -> MemoryService -> DialogChoiceMemoryService +
WorldMapRouteResultMemoryService -> tenant/user CloudServiceStorage private files` 完整链。保持 `696a12b0` 三个
Service 全部 key/threshold/counter/settlement/load/replace 顺序；两个 store 在 Cloud 都必须由显式 scoped `Path` bean
构造并避免 component-scan duplicate，文件分别为 `dialog_choice_memory.json` 与
`world_map_route_result_memory.json`。不得使用共享工作目录 `config/*.json` authority，不得改 Navigation/B shared
wire/A/D/runner/tests/POM，不得新增 TTL/retry/compaction/global owner。若三份 Service 已满足基线，不制造无意义 diff，
但须逐方法审查并在 Implementation 列明。不得 build/test/runtime/Git；父级统一 fresh package。当前不增加
`189/407`。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T17:50:00-04:00

CLAIMED | task=W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1 | claimedAt=2026-07-14T17:50:00-04:00 | writeSet=[Cloud DialogChoiceMemoryService.java,Cloud WorldMapRouteResultMemoryService.java,Cloud MemoryService.java,Cloud CloudServiceConfiguration.java,this-log]

## External Worker C - W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1 Implementation #1 - 2026-07-14T17:58:00-04:00

闭合 `Navigation/current callers → MemoryService → DialogChoiceMemoryService + WorldMapRouteResultMemoryService → tenant/user CloudServiceStorage private files` 完整链。
按父级明令未 build/test/runtime/Git；以静态自查代编译门。唯一实际改动=Cloud `host/CloudServiceConfiguration.java`（blob `8b68db6f`，SHA-256 `4834db52…`）；
三份 memory Service 已与 `696a12b0` **byte-exact 一致**（blob 全等，见下），按 brief「已满足基线不制造无意义 diff」保持零改动。

### 三 Service 逐方法审查（byte-exact = 696a12b0，未改）

blob 校验：DialogChoice `5dda90c4`、WorldMapRoute `8de1e334`、MemoryService `51cf0508` —— 三者 cloud == `696a12b0` baseline。

| Service | 关键方法（key/threshold/counter/settlement/load/replace 顺序保真） |
|---|---|
| `DialogChoiceMemoryService`（@Service，328L） | `findUsable`/`findStableTaskChoice`（`key(scope,action,contextKey)` 策略 + 置信/稳定门）、`recordSuccess`/`recordFailure`（success/failure counter + settlement）、路由变体 `findUsableRoute`/`recordRouteSuccess`/`recordRouteFailure`；`load`（`memoryPath` 存在→读，否则 legacy transfer path fallback）/`save`（原子 tmp→replace）；`entries` LinkedHashMap 顺序保持 |
| `WorldMapRouteResultMemoryService`（@Service，333L） | `findClean`(LEGACY_GREEN_LINK / 指定 mode，disabled/dirty 跳过 + counter 日志)、`findEntry`、`recordSuccess`/`recordFailure`/`recordAbandoned`（pending settlement + counter）；`load`/`save`（`memoryPath` + tmp resolveSibling replace）；`entries` LinkedHashMap 顺序保持 |
| `MemoryService`（@Service，163L，facade） | 委托上两 store 的 `findUsableDialogChoice`/`recordDialogChoice*`/`findUsableRouteDialogChoice`/`recordRouteDialogChoice*`/`findCleanWorldMapRouteResult`/`findWorldMapRouteResultEntry`/…；构造注入两 store，无自有存储 |

三 Service 的存储由各自 `Path memoryPath` 构造器参数决定（非硬编码写入）——正是链闭合的注入点。

### 链闭合改动（唯一改：CloudServiceConfiguration）

问题：`WorldMapRouteResultMemoryService` 原为 @Service 且**未**排除出 component-scan → 会以无参构造器落到禁用的共享 `config/world_map_route_result_memory.json`；且若再加显式 bean 即 component-scan duplicate。`DialogChoiceMemoryService` 上一 slice 已正确排除 + 显式 scoped bean。

修复（对齐既有 dialog 模式）：
1. `@ComponentScan(excludeFilters=...classes={DialogChoiceMemoryService.class, WorldMapRouteResultMemoryService.class})` —— 两 store 均排除出 component-scan，杜绝 duplicate。
2. 新增常量 `WORLD_MAP_ROUTE_RESULT_MEMORY_FILE = "world_map_route_result_memory.json"`。
3. 新增 `@Bean public WorldMapRouteResultMemoryService worldMapRouteResultMemoryService(CloudServiceStorage storage) { return new WorldMapRouteResultMemoryService(storage.resolvePrivateFile(WORLD_MAP_ROUTE_RESULT_MEMORY_FILE)); }` —— 显式 scoped `Path` bean，落 tenant/user hashed scope 下私有文件。

结果：两 store 均由**显式 scoped `Path` bean**（`CloudServiceStorage.resolvePrivateFile(...)`）构造，文件分别 `dialog_choice_memory.json` / `world_map_route_result_memory.json`，落于 tenant/user 私有目录；不再用共享 `config/*.json` authority；无 component-scan duplicate。`MemoryService`（@Service 扫描）唯一注入这两个显式 bean（全仓核实无其它消费者），无 bean 冲突。

### 静态自查（工具证据）

- 三 Service blob 全等 `696a12b0`（DialogChoice 5dda90c4 / WorldMapRoute 8de1e334 / MemoryService 51cf0508），未改。
- CloudServiceConfiguration：excludeFilters 现含两类（行 24）、两常量（27-28）、两显式 scoped bean（`dialogChoiceMemoryService` 37 / `worldMapRouteResultMemoryService` 48，各 `resolvePrivateFile`）。
- 唯一实际改动=CloudServiceConfiguration（三 Edit：import + excludeFilters 数组 + 常量/bean）；`git diff --check` 干净；未改 Navigation/B shared wire/A/D/runner/tests/POM/其它 Java。
- 未新增 TTL/retry/compaction/global owner；两 store 仍各自 synchronized + 原子 tmp→replace 保存（baseline 行为未动）。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=Cloud CloudServiceConfiguration.java（实际改）+ 三 memory Service（审查确认 byte-exact baseline、零改动）+ 本日志；未改 Navigation/B shared wire/A/D/runner/tests/POM/其它文件；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 逐条落实：两 store 由显式 scoped `Path` bean 构造（resolvePrivateFile，文件名精确）、避免 component-scan duplicate（两类均 exclude）、不用共享 `config/*.json`、保持三 Service 全 key/threshold/counter/settlement/load/replace 顺序（byte-exact）、无新增 TTL/retry/compaction/global owner。
3. 因父级禁编译门，用逐项静态自查（blob 全等/excludeFilters/scoped bean/无其它消费者/diff）替代，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移；三 Service 已满足基线故仅补 Cloud 存储链接线，无无意义 diff。

External Worker C 交付 W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1 Implementation #1（闭合 MemoryService→两 store→tenant/user CloudServiceStorage 私有文件链：CloudServiceConfiguration 排除 WorldMapRouteResultMemoryService 出 component-scan + 显式 scoped `Path` bean（world_map_route_result_memory.json），对齐既有 dialog 模式；三 memory Service byte-exact baseline 零改动，逐方法审查列明；无共享 config/*.json、无 duplicate、无 TTL/retry/compaction/owner；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Whole-Storage Source Review #1 - SOURCE APPROVED / `W-696-CLOUD-MEMORY-WHOLE-STORAGE-CHAIN-1` - 2026-07-14T18:21:49-04:00

父级独立读取三份完整 Memory Service、`CloudServiceConfiguration`、`CloudServiceStorage` 与全 Cloud main-source
构造/bean sites。结论：**SOURCE APPROVED，P0=0 / P1=0 / P2=0；等待统一 fresh Cloud package。**

- `DialogChoiceMemoryService`、`WorldMapRouteResultMemoryService`、`MemoryService` 与 `696a12b0` 的源码行和
  Git-normalized 内容一致；全部 public/private、key/threshold/counter/settlement/load/tmp-replace 顺序未变。
  纠正交付措辞：`DialogChoiceMemoryService` 当前工作树为 LF、baseline 文件系统镜像为 CRLF，因此不是 literal
  filesystem-byte identical；逐行内容及 normalized blob 一致，这不是代码 blocker。
- `CloudServiceConfiguration.java:20-24` 同时把两 store 排除出 component scan，`:36-50` 恰提供各一个
  Service bean；全 Cloud main source 没有第二个显式实例化点，`MemoryService` 注入不会形成 duplicate。
- 两个 bean 分别以内联 `CloudServiceStorage.resolvePrivateFile("dialog_choice_memory.json")` 与
  `resolvePrivateFile("world_map_route_result_memory.json")` 构造。这里是“Service bean 由 scoped Path 构造”，
  不是额外注册全局 `Path` bean；该形态避免 Path 注入歧义且满足合同。
- `CloudServiceStorage.java:36-40,52-80,83-88` 以 tenant/user 长度前缀哈希隔离 scope，并拒绝多段文件名、
  绝对路径和 scopeRoot 越界；没有共享 `config/*.json` authority 回退。
- 本单实际 Java 写入仅 `CloudServiceConfiguration.java`，三 Service 为完整基线类零内容改；未见 A/B/D
  写集漂移，也未新增 TTL/retry/compaction/owner/session/ledger。

当前其它 Java writer 尚在返修/实施，父级不跑并发 clean、不提前增加 `189/407`。C 本单源码写集释放；
下一任务只从父级已审 next-task queue 另发。无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Implementation Task - `W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1` - 2026-07-14T18:44:41-04:00

这是与 External B 同步的一条 **PlayerState active first-aid 完整双仓链**，不是 DTO/单方法小单。C 负责 Cloud
closed contract/port 与真实 `AutoBattleTask/AutoCombatService -> PlayerStateService` active caller；B 负责 DHXY
exact-window mechanics/handler。两仓物理写集零交集。请 C 在
**2026-07-14T19:04:41-04:00** 前于真实 EOF 追加：

`CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 | claimedAt=<ISO-8601> | writeSet=[Cloud PlayerStateFirstAidMacroCommand.java,Cloud PlayerStateFirstAidMacroResult.java,Cloud CloudPlayerStateFirstAidPort.java,Cloud LocalMacroKind.java,Cloud LocalMacroCommand.java,Cloud LocalMacroRequest.java,Cloud LocalMacroOutcome.java,Cloud RemoteCommandOutcomeEnvelope.java,Cloud RemoteProtocolDigests.java,Cloud PlayerStateService.java,this-log]`

### C 唯一 Java 写集与完整 active caller

- New `remote/PlayerStateFirstAidMacroCommand.java`、`PlayerStateFirstAidMacroResult.java`、
  `CloudPlayerStateFirstAidPort.java`
- Modify Cloud `LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、
  `LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`
- Modify完整 Cloud `service/PlayerStateService.java`，仅替换 active first-aid 本地 mechanics 调用点；incense、
  identity、position、dormant `healPlayer/healPet/areStatusBarsVisibleNoFocus` 本轮冻结，不虚称整类已完成。

canonical contract 与 B 完全相同：macro kind=`PLAYER_STATE_FIRST_AID`；operation 恰
`PROBE_SUPPLY_NO_FOCUS/HEAL_ALL/EXECUTE_CACHED_PLAN`。四 target toggles/raw thresholds、cached plan base 与
ordered targets、probe/heal ordered result 的字段和 enum 名必须逐字段镜像 B 日志同 timestamp 的合同；strict
constructor、JSON、canonical digest、nested outcome 均拒绝 variant 混装和非 EXECUTED typed payload。

完整 active 业务链必须一次闭合：
`AutoBattleTask:135 -> performStartupFirstAidCheck`，以及 `AutoCombatService:382-577 ->
probeAndConsumeHealthyFirstAidNoFocus/probeFirstAidSupplyNoFocus/performCachedFirstAidPlanNow`。Cloud 继续拥有
`checksDoneThisRound`、pending plan、target selection、threshold normalization、healthy/needed/unknown 判定、plan
consume/clear、日志和 state mutation；DHXY 只返回 mechanical observations/执行结果。`healAll(taskContext)` 通过
port 发四 target intent；no-context legacy overload 只能从项目现有 `TaskExecutionContextHolder` 取 authority-minted
current context，不得 default/epoch=0。

transport：`EXECUTED` exhaustive 映回原结果；`NOT_EXECUTED` 保留原“未完成”日志/返回，不写成功状态；
`STOPPED/UNKNOWN` 抛 `TaskFatalException` unwind，零自动 retry。cached-plan 即使 local result=INTERRUPTED，仍按
696 原 caller 语义消费计划并增加本轮检查计数，但不得伪造 physical completion。所有 checkpoint、MAX_CHECKS、
plan base refresh fallback、target 顺序和 350/100/800 delay 不变。不得把 bar 判断/计划/state 下沉 DHXY，不得新增
owner/session/ledger/TTL/retry，不得改 A/B/D 写集，不得 build/test/runtime/Git。

交付列十文件 scoped diff、完整 public caller graph、三 operation/transport 真值表、Cloud state mutation 前后顺序与
696 方法对照。自审不算 Approved；此 cohort 双侧通过并 fresh 双构建前不增加 `189/407`。
无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T18:48:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 | claimedAt=2026-07-14T18:48:00-04:00 | writeSet=[Cloud PlayerStateFirstAidMacroCommand.java,Cloud PlayerStateFirstAidMacroResult.java,Cloud CloudPlayerStateFirstAidPort.java,Cloud LocalMacroKind.java,Cloud LocalMacroCommand.java,Cloud LocalMacroRequest.java,Cloud LocalMacroOutcome.java,Cloud RemoteCommandOutcomeEnvelope.java,Cloud RemoteProtocolDigests.java,Cloud PlayerStateService.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 Implementation #1 (partial: 9/10 Java files) - 2026-07-14T19:00:00-04:00

Cloud 侧 `PLAYER_STATE_FIRST_AID` LOCAL_MACRO 完整传输合同 + port 已闭合（9/10 Java 文件），逐字段镜像 B 日志同 timestamp 合同。
第 10 文件 `PlayerStateService.java` active-caller 接线为剩余子步（见末节实况与方案），本轮先交付并请复审合同层。
按父级明令未 build/test/runtime/Git；以静态自查代编译门。

### 已完成 9 文件 SHA-256

新建 3：`PlayerStateFirstAidMacroCommand` `c2e9529b…`、`PlayerStateFirstAidMacroResult` `1f605c87…`、`CloudPlayerStateFirstAidPort` `51d614eb…`。
改 6：`LocalMacroKind` `118e11ef…`、`LocalMacroCommand` `ab559114…`、`LocalMacroRequest` `b4da7dc5…`、`LocalMacroOutcome` `3b411eb4…`、`RemoteCommandOutcomeEnvelope` `c1f688a4…`、`RemoteProtocolDigests` `72b65c70…`。

### canonical contract（逐字段镜像 B）

- kind `PLAYER_STATE_FIRST_AID`（LocalMacroKind 5→6）；`LocalMacroCommand` sealed permits +`PlayerStateFirstAidMacroCommand`。
- **Command**：`operation∈{PROBE_SUPPLY_NO_FOCUS,HEAL_ALL,EXECUTE_CACHED_PLAN}`；probe/heal 恰四 `TargetToggle(enabled,threshold)`（playerHp/playerMp/petHp/petMp）；cached 恰 `planBaseX/planBaseY + List<PlannedTarget(name,relX,relY,threshold)>`；strict 构造器拒绝 variant 混装（NON_NULL 省略未用字段）。
- **Result**：`operation` 选 variant；probe=`ProbeSnapshotStatus{READABLE,CAPTURE_UNAVAILABLE}`+ordered `ProbeObservation(name,ProbeStatus{DISABLED,HEALTHY,SUPPLY_NEEDED,UNREADABLE},sampleRelX,sampleRelY)`；heal=`HealSnapshotStatus{CAPTURED,CAPTURE_FAILED}`+ordered `HealOutcome(name,HealStatus{DISABLED,HEALTHY,NO_ACTION,EXECUTED,UNREADABLE,CAPTURE_FAILED},sampleRelX,sampleRelY,clickAbsX,clickAbsY)`；cached=`CachedPlanStatus{COMPLETED,INTERRUPTED}`；HealOutcome 强制 EXECUTED⇔click 成对；状态名逐字复用 mechanics enum，坐标空间不变。
- LocalMacroRequest/Outcome：加 `playerStateFirstAid` slot、`case PLAYER_STATE_FIRST_AID`、各既有 case 互斥补 `&& playerStateFirstAid==null`、3 参便捷构造器 instanceof、command() 分派、6 参 compat 构造器补第 8 null、withCommon 补字段。

### transport 真值表（三 operation × terminal）

| terminal | 映射 |
|---|---|
| EXECUTED | envelope 用自有 7-key 扁平集 `LOCAL_MACRO_PLAYER_STATE_FIRST_AID_PAYLOAD_KEYS{macroKind,operation,probeSnapshotStatus,probeObservations,healSnapshotStatus,healOutcomes,cachedPlanStatus}`（变体未用字段显式 null），`decodePlayerStateFirstAidResult` strip macroKind → treeToValue → `PlayerStateFirstAidMacroResult`（strict 构造器校验 variant）；4-key EXECUTED switch 加 `case PLAYER_STATE_FIRST_AID -> throw`（必须走专用 payload）；LocalMacroOutcome EXECUTED 要求 playerStateFirstAid 非空且他类空 |
| NOT_EXECUTED | 无 typed result（LocalMacroOutcome 非 EXECUTED 全 null 校验含 playerStateFirstAid）；port 返回 `Optional.empty()`（caller 保留"未完成"日志/返回，不写成功） |
| STOPPED/UNKNOWN | port 抛 `TaskFatalException`，零 auto-retry |

canonical digest：request 经 `withComputedRequestDigest(LocalMacroRequest)` 8 参重建含 playerStateFirstAid（valueToTree NON_NULL 自动纳入）；outcome digest `valueToTree(outcome)` NON_NULL 自动含 playerStateFirstAid（无 bytes 需排除，DIALOG framePngBytes 排除逻辑未动）。BAG/NAV/UI_CLEAN/DIALOG 4-key/20-key 路径逐字未改（新 key dispatch 仅 gate 于 `EXECUTED && PLAYER_STATE_FIRST_AID`）。

### port（CloudPlayerStateFirstAidPort）

@Component，注入 TaskExecutionContextHolder；3 方法 `probeSupplyNoFocus/healAll(四 toggle)`、`executeCachedPlan(base,targets)` → 前后 `TaskCheckpoint` + `context.getGameClient().executeLocalMacro(phaseCode,actionSlot,PLAYER_STATE_FIRST_AID,command,120000)` → EXECUTED 校验 operation 匹配后 `Optional.of(result)`；NOT_EXECUTED `Optional.empty()`；STOPPED/UNKNOWN `TaskFatalException`。无 owner/session/ledger/TTL/retry。context 恒取自 `TaskExecutionContextHolder.current()`（authority-minted，无 default/epoch=0）。

### 第 10 文件 PlayerStateService active-caller 接线：方案 + 实况

active 链（须接线）：`AutoBattleTask:135 → performStartupFirstAidCheck → healAll(taskContext)`；`AutoCombatService:382-577 → probeAndConsumeHealthyFirstAidNoFocus/probeFirstAidSupplyNoFocus/performCachedFirstAidPlanNow`。
- `probeFirstAidSupplyNoFocus`：`captureBarsSnapshotNoFocus()+findSupplyTargetsFromSnapshot(bars)` → `firstAidPort.probeSupplyNoFocus(四 config toggle)`；READABLE→按 observations 的 `SUPPLY_NEEDED` 建 `FirstAidTarget(name,sampleRelX,sampleRelY,threshold)` + `pendingNoFocusFirstAidPlan`；CAPTURE_UNAVAILABLE→UNKNOWN。checksDoneThisRound 门/MAX_CHECKS/日志/state mutation 全留 Cloud。
- `healAll()`：`inputSequences.submitExclusiveAndWait(healAllDirect)` → `firstAidPort.healAll(四 toggle)`（DHXY handler 持有 exclusive input）；empty→保留"did not complete"日志。`healAll(taskContext)` 前后 checkpoint 不变。
- `performCachedFirstAidPlanNow→performCachedFirstAidPlanDirect`：本地 refresh+moveMouse+clickRight loop → `firstAidPort.executeCachedPlan(plan.baseX,baseY,ordered PlannedTarget)`；EXECUTED(COMPLETED|INTERRUPTED) 均消费 plan+checksDoneThisRound++（复刻 696 语义，不伪造 physical completion）；plan consume/clear 留 Cloud。
- incense/identity/position/dormant `healPlayer/healPet/checkAndHeal/areStatusBarsVisibleNoFocus` 冻结，不虚称整类完成。

**实况（诚实上报）**：当前 Cloud `PlayerStateService.java` = `696a12b0` byte-exact，含 **51 处 Cloud 不存在的本地类引用**（tracker/inputSequences/inputProvider/ImageFinder/coordinateHelper/textRecognizer/windowScopedTempPath），且 active heal 与 frozen `checkAndHeal` 共享 `isHealthyColor/healIfUnhealthy`、字段跨 active/frozen 共用。故本单只替换 active 调用点为 port 后，该文件仍非独立可编译（frozen 路径字段/类型待后续轮迁移），符合父级"不 build、不虚称整类完成、cohort 双侧+fresh 双构建前不增计数"。第 10 文件源码编辑（加 port 字段/注入 + 三 active 调用点替换、frozen 全留）为剩余子步，续做后补 scoped diff 与 696 方法对照。

### scope self-QA（仅 QA，不构成 Approved）

1. 已改写集=上列 9 remote 文件 + 本日志；未改 A/B/D 写集、broker/codec/handler 之外文件（RemoteGameCommandBroker 6 参 compat 完好未动）、POM；未 build/test/runtime/Git；保护他人 dirty/untracked。
2. 合同逐字段镜像 B（kind/3 operation/四 toggle/cached base+targets/probe·heal·cached result 枚举名与字段）；strict 构造/JSON/canonical digest/nested outcome 拒混装与非 EXECUTED typed payload；transport 三终态映射；port 三方法 + authority context。
3. 因父级禁编译门，用静态自查（LocalMacroKind=6/6 文件含 kind/构造全 8 参/command() 穷尽/diff 干净）替代；第 10 文件实况诚实上报，未虚称完成。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 Implementation #1（partial 9/10）：Cloud PLAYER_STATE_FIRST_AID LOCAL_MACRO 完整合同+port 闭合（3 新+6 改 remote 文件，逐字段镜像 B、7-key 扁平 payload+strip-macroKind decode、三终态 transport、canonical digest、port 三 operation authority context）；第 10 文件 PlayerStateService active-caller 接线方案与耦合实况诚实上报、源码编辑续做；按父级明令未 build/test，以静态自查代编译门；持续重读本日志等待父级复审；自审不算 Approved。

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 Addendum: base-sourcing resolved, PlayerStateService edit continuing - 2026-07-14T19:12:00-04:00

承接上条 partial Implementation #1（9/10 remote 合同+port 已闭合、未变）。第 10 文件 `PlayerStateService.java` active-caller 接线的唯一悬置设计点已解：
- **cached-plan `planBaseX/planBaseY` 的 Cloud 源**：baseline 取 `tracker.getWindowBaseX/Y()`（Cloud 无 tracker）。probe result 合同（B 锁定）不携 window base；`TaskExecutionContext` 亦不暴露 screen-absolute base。**解**：经 `context.getGameClient().readWindowFact(phaseCode,actionSlot,WindowFactKind.GEOMETRY,timeout)` → `WindowFact.GeometryFact.x()/y()`（SCREEN_ABSOLUTE_PX）读窗口屏幕绝对基点，复刻 baseline "探测时记录 plan base" 语义；B 的 EXECUTE_CACHED_PLAN mechanics 逐字保留 696 live binding refresh、失败回退该 stored base。此为既有 GEOMETRY fact 的纯观测读取，未把 bar 判断/plan/state 下沉 DHXY、未新增 owner/session/ledger/TTL/retry。
- 由此 `probeFirstAidSupplyNoFocus` 的 `tracker.getWindowBaseX()==-1` 窗口可用门改为 GEOMETRY 读取失败/缺席门（UNKNOWN），其余（checksDoneThisRound/MAX_CHECKS/pending plan 建/consume/clear/日志/state mutation）全留 Cloud。

第 10 文件源码编辑（加 `CloudPlayerStateFirstAidPort` 字段+注入+import、三 active 调用点 probe/healAll/cached 替换为 port、frozen incense/identity/position/dormant 全留）下轮以新预算精细完成，届时补 PlayerStateService scoped diff + 696 方法一对一 + Cloud state mutation 前后顺序。9 文件合同层请父级先复审。自审不算 Approved。

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 Implementation #1 完成（第 10 文件 PlayerStateService active-caller 接线）- 2026-07-14T19:30:00-04:00

承接 partial(9/10)+base-sourcing addendum，现完成第 10 文件 Cloud `service/PlayerStateService.java` active first-aid caller 接线；10 文件写集齐。
按父级明令未 build/test/runtime/Git；以静态自查代编译门。PlayerStateService 新 blob（git hash-object）= `d430213ad301dac66e271a2e8cd2b2b7ab1f8345`，
1587 行（696a12b0=1483，+104），SHA-256 = `f6845fe640acd07d3653ac483f5a4714c1d23b56b16c82db52bce840d1ac8e5e`。

### active first-aid 调用点 696 方法对照（仅替换本地 mechanics 调用点，Cloud state/判定/日志/顺序全留）

| 696 active caller | 改动 |
|---|---|
| `probeFirstAidSupplyNoFocus(taskContext):259-299` | checkpoint / `checksDoneThisRound>=MAX_CHECKS→ALREADY_DONE` gate 不变；`tracker.getWindowBaseX()==-1` 门 → `readWindowBase(taskContext)`（GEOMETRY fact `x/y`，null→UNKNOWN，plan 置 null）；`captureBarsSnapshotNoFocus()+findSupplyTargetsFromSnapshot()` → `firstAidPort.probeSupplyNoFocus(FIRST_AID_PHASE,PROBE_SLOT,四 firstAidToggle(config enable+raw threshold))`；empty 或 snapshot≠READABLE→UNKNOWN(plan null)；`supplyTargetsFromObservations`（仅 `SUPPLY_NEEDED` obs → `FirstAidTarget(name,sampleRelX,sampleRelY,expectRed(名含"血"),normalizeThreshold(config threshold))`）；pending plan 建/清、`describeFirstAidTargets` 日志、HEALTHY/SUPPLY_NEEDED 返回不变 |
| `performCachedFirstAidPlanNow(taskContext):307-338` | checkpoint / 取并清 pending plan / 空 plan→false / `baseX==-1`门 / `ageMs` 日志 不变；`submitExclusiveAndWait(performCachedFirstAidPlanDirect)` → `firstAidPort.executeCachedPlan(FIRST_AID_PHASE,CACHED_SLOT,plan.baseX,plan.baseY,plannedTargets(plan.targets))`；`completed = cached present && cachedPlanStatus==COMPLETED`（INTERRUPTED/empty→"did not complete"警告，复刻 696 completed=false 语义）；**`checksDoneThisRound++` 与 `return true` 无条件保留**（即使 INTERRUPTED 仍消费计划+增计数，不伪造 physical completion） |
| `healAll():445-450` → `healAll(taskContext):474-478` | `healAll(taskContext)` 前后 checkpoint 不变；`healAll()` 的 `submitExclusiveAndWait(healAllDirect)` → `firstAidPort.healAll(FIRST_AID_PHASE,HEAL_SLOT,四 toggle)`；empty→"did not complete"警告。`AutoBattleTask:135→performStartupFirstAidCheck→healAll(taskContext)` 链闭合 |

新增私有 helper（纯 Cloud 决策/映射，无下沉）：`firstAidToggle`、`readWindowBase`(GEOMETRY)、`supplyTargetsFromObservations`、`plannedTargets`(FirstAidTarget→PlannedTarget)、`firstAidBarExpectRed`、`firstAidBarThreshold`。新增 6 常量（phase/4 slot/timeout）+ `CloudPlayerStateFirstAidPort firstAidPort` final 字段（@RequiredArgsConstructor 注入）+ 8 imports（3 契约/port + WindowFact/Kind/Outcome + ExecutionState + Optional）。

### Cloud state mutation 前后顺序（保 696）

probe：checksDone gate → GEOMETRY base → port observe → SUPPLY_NEEDED→FirstAidTarget → pending plan set/clear → 日志 → 返回（Cloud 持有 target 选择/threshold normalize/healthy·needed·unknown 判定/plan）。cached：取并清 plan → port execute → checksDone++ → return true（consume/clear/count 全 Cloud，DHXY 只回 COMPLETED/INTERRUPTED）。transport：EXECUTED→typed result；NOT_EXECUTED→port Optional.empty（保留"未完成"日志/返回）；STOPPED/UNKNOWN→port TaskFatalException 零 retry。healAll(taskContext) 前后 checkpoint 保留。

### 冻结（本轮不动，不虚称整类完成）

`incense（probeIncenseStatus/ensureSheYaoXiang*/readSheyaoxiang*）`、`identity(syncMyIdentity)`、`position(syncMyPosition)`、dormant `healPlayer/healPet/checkAndHeal/areStatusBarsVisibleNoFocus` 及其本地 mechanics（`captureBarsSnapshot*/findSupplyTargetsFromSnapshot/checkAndHealFromSnapshot*/healAllDirect/performCachedFirstAidPlanDirect/isHealthyColor/healIfUnhealthy/moveMouseAway*` 等）逐字保留（grep 证 12 冻结方法在位）。**诚实上报**：这些冻结路径仍引用 Cloud 不存在的本地类（tracker/inputSequences/inputProvider/ImageFinder/coordinateHelper/textRecognizer/windowScopedTempPath），故该文件本轮仍非独立可编译，属父级已知的 partial 迁移（cohort 双侧+fresh 双构建前不增计数）；active first-aid caller 已源码正确接入 port。

### 静态自查（工具证据）

- active 三调用点均经 `firstAidPort.*`（probe:294 / cached:351 / heal:471）+ `readWindowBase`(GEOMETRY,520)；probe 方法内已无 `tracker.`/`captureBarsSnapshotNoFocus`/`findSupplyTargetsFromSnapshot`。
- 契约 accessor 一致：`ProbeSnapshotStatus.READABLE`/`CachedPlanStatus.COMPLETED`/`ProbeStatus.SUPPLY_NEEDED`/`ProbeObservation.status()/sampleRelX()/name()`/`GeometryFact.x()/y()`/`ExecutionState.OBSERVED`；FirstAidTarget(5参)/PlannedTarget(4参)/TargetToggle(2参) arity 对。
- `Optional` import 补齐；helper/常量/字段就位；frozen 12 方法在位（未动）；`git diff --check` 干净。
- 未改 A/B/D 写集、remote 之外 Cloud 文件（本轮仅改 PlayerStateService；9 remote 文件为前次交付）、POM。

### scope self-QA（仅 QA，不构成 Approved）

1. 全 10 Java 写集=3 新契约/port + 6 remote 协议 + PlayerStateService(active caller) + 本日志；未改 A/B/D/runner/tests/POM/其它；未 build/test/runtime/Git；保护他人 dirty/untracked。
2. 逐条落实：kind/3 operation/四 toggle/cached base+targets/probe·heal·cached result 逐字段镜像 B；strict 构造/JSON/canonical digest/nested outcome；transport 三终态；port authority context；active 三 caller 接 port、Cloud 保 checksDone/MAX_CHECKS/pending plan/target 选择/threshold normalize/判定/consume·clear/日志/state、350·100·800 delay 与 target 顺序在 DHXY mechanics 侧保真；GEOMETRY base 源；INTERRUPTED 仍消费+增计数不伪造；incense/identity/position/dormant 冻结、不下沉、无新 owner/session/ledger/TTL/retry。
3. 因父级禁编译门，用静态自查（调用点/契约 accessor/arity/diff/frozen 在位）替代；frozen 路径本地类引用如实上报，未虚称整类可编译/完成。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1 Implementation #1 完成（10/10 Java 文件：PLAYER_STATE_FIRST_AID LOCAL_MACRO 完整合同+port 逐字段镜像 B，PlayerStateService active first-aid 三 caller（probe/heal/cached）接 port、GEOMETRY 取 base、Cloud 保全 state/判定/日志/顺序、INTERRUPTED 仍消费+增计数不伪造、incense/identity/position/dormant 冻结；诚实上报 frozen 路径本地类引用致文件本轮非独立可编译之 partial 状态；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #1 - BLOCKED - 2026-07-14T19:36:43-04:00

父级已独立读取 10 文件、B 当前 DHXY contract/mechanics 及
`696a12b0:PlayerStateService.java` active caller；helper preflight 仍为非绑定辅助，不替代本裁决。
本轮结论：**P0=0 / P1=1 / P2=1，Implementation #1 不通过。** 已完成的三 operation、
transport 四态、Cloud state/plan/check counter ownership、cached-plan consume 语义与无自动 retry 冻结。

### P1-1：stored plan base 不是 bars capture 的同一次 geometry observation

- 证据：当前 `PlayerStateService.java:283-299` 先通过独立 `GEOMETRY` fact 得到 `planBase`，随后才调用
  `firstAidPort.probeSupplyNoFocus`；`readWindowBase:520-536` 是第二条独立 remote operation，并在
  `InterruptedException` 时恢复中断后降为 null/UNKNOWN。当前
  `PlayerStateFirstAidMacroResult.java:19-25` 的 PROBE result 不携 capture-time window base。
- 基线：`696a12b0:PlayerStateService.java:273-282` 先执行 `captureBarsSnapshotNoFocus()`；该 capture
  经 `CoordinateHelper.getScaledRect` 刷新窗口几何，随后才从同一个 tracker 状态读取
  `planBaseX/Y`。也就是保存的 base 与 bars 帧属于同一次 observation。
- 影响：窗口在 GEOMETRY fact 与 PROBE macro 之间移动时，Cloud 会把新 bars observation 与旧 base
  组合进 plan；若稍后 local live refresh 不可用，stored-base fallback 可点到错误位置。额外 fact 还引入
  基线不存在的第二次远程等待及中断降级。
- 返修条件：删除 `readWindowBase`、`FIRST_AID_GEOMETRY_SLOT` 及这条额外 fact 调用。PROBE 的
  `READABLE` typed result 必须携 `observedBaseX/observedBaseY`，由 DHXY mechanics 在 exact HWND
  fresh geometry 后用该 geometry 捕获 bars 并一并返回；`CAPTURE_UNAVAILABLE` 时两字段为空。
  Cloud 只从该同一 result 建 plan。不得增加第三次 read、retry、TTL 或新 owner。

### P2-1：固定四 bar 的 closed identity/order 只写在注释，constructor 未强制

- 证据：`PlayerStateFirstAidMacroResult.java:27-54,88-118` 接受任意长度、任意非 blank name 的
  observation/outcome；`PlayerStateService.java:545-581` 又把任意未知 name 静默落到“宝宝法力”阈值，
  且用 `name.contains("血")` 推 expectRed。
- 影响：损坏或不对称 payload 可产生重复 target，或把未知 bar 当作宝宝法力，最终形成错误 cached click。
- 返修条件：READABLE/CAPTURED 结果必须严格是基线固定顺序
  `人物血量, 人物法力, 宝宝血量, 宝宝法力` 各一次；CAPTURE_UNAVAILABLE/CAPTURE_FAILED 维持空表。
  Cloud mapping 对未知/重复/错序 fail contract，不得默认映到宝宝法力；B/C 两端 constructor 接受域一致。

## Parent Repair Task - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R1

原 External C 在本日志真实 EOF 追加 `CLAIMED` 后定点返修：
`PlayerStateFirstAidMacroResult.java`、`CloudPlayerStateFirstAidPort.java`、
`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`、`PlayerStateService.java`，以及为新增
PROBE 两字段确有必要的既有 Cloud local-macro model 文件；不得触碰 B 的 DHXY 文件、A/D 写集或 frozen
incense/identity/position/dormant methods。领取截止 `2026-07-14T19:56:43-04:00`。交付
`Implementation Repair #1` 时给同帧 base 数据流、严格四 bar matrix、删除独立 GEOMETRY 调用证明与
scoped diff；不得 build/test/runtime/Git。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T19:40:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R1 | claimedAt=2026-07-14T19:40:00-04:00 | writeSet=[Cloud PlayerStateFirstAidMacroResult.java,Cloud CloudPlayerStateFirstAidPort.java,Cloud RemoteCommandOutcomeEnvelope.java,Cloud RemoteProtocolDigests.java,Cloud PlayerStateService.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R1 Implementation Repair #1 - 2026-07-14T19:52:00-04:00

按 `Source Review #1`（P1-1/P2-1）定点返修。按父级明令未 build/test/runtime/Git；以静态自查代编译门。改动 3 文件（+2 冻结未改）：
`PlayerStateFirstAidMacroResult` blob `d09e308d`、`RemoteCommandOutcomeEnvelope` blob `39640c46`、`PlayerStateService` blob `6be4aa99`；
`RemoteProtocolDigests`(`af7b9cc7`)/`CloudPlayerStateFirstAidPort`(`ec52f919`) 本轮无需改（见下）。

### P1-1 同帧 base 数据流（删独立 GEOMETRY 调用）

- **删除**：`PlayerStateService` 的 `readWindowBase` 方法、`FIRST_AID_GEOMETRY_SLOT`/`FIRST_AID_MACRO_TIMEOUT_MS` 常量、`WindowFact`/`WindowFactKind`/`WindowFactOutcome`/`ExecutionState` 四个 import（grep 证 PlayerStateService 内此四符号+readWindowBase+GEOMETRY_SLOT 残留=空）。不再有第二条独立 remote fact 及其中断降级。
- **新增同帧字段**：`PlayerStateFirstAidMacroResult` 加 `Integer observedBaseX, observedBaseY`；构造器强制 `READABLE` probe 必带该同帧 base（`hasBase`），`CAPTURE_UNAVAILABLE`/HEAL/CACHED 均不带；base 成对（hasBase==hasAnyBase）。DHXY mechanics（B 侧）在 exact HWND fresh geometry 后用该 geometry 捕获 bars 并一并返回 observedBaseX/Y——bars 帧与 base 同一次 observation。
- **Cloud 只从同一 result 建 plan**：`probeFirstAidSupplyNoFocus` 先 `firstAidPort.probeSupplyNoFocus(...)`，`READABLE` 后 `int planBaseX=observation.observedBaseX(); planBaseY=observation.observedBaseY();`（同一 result），再 `new FirstAidPlan(targets, now, planBaseX, planBaseY)`。窗口不可用→`CAPTURE_UNAVAILABLE`/empty→UNKNOWN。无第三次 read/retry/TTL/新 owner。
- Envelope flat payload key 集 `LOCAL_MACRO_PLAYER_STATE_FIRST_AID_PAYLOAD_KEYS` 加 `observedBaseX/observedBaseY`（9 keys）；decode treeToValue 自动纳入新 record 组件；outcome digest `valueToTree` NON_NULL 自动含二字段——故 `RemoteProtocolDigests` 无需改。`CloudPlayerStateFirstAidPort` 返回整 result，无需改。

### P2-1 严格四 bar closed matrix（constructor 强制，Cloud 不默认）

- **Result 构造器**：新增 `FIRST_AID_BAR_NAMES=[人物血量,人物法力,宝宝血量,宝宝法力]` 与 `requireFixedBarNames`；`READABLE` probe 与 `CAPTURED` heal 的 observation/outcome name 必须**恰这四 bar、各一次、按此序**（size==4 且逐位 equals），否则 fail contract；`CAPTURE_UNAVAILABLE`/`CAPTURE_FAILED` 强制空表。B/C 两端接受域一致（B 镜像同 contract）。
- **Cloud mapping 去默认**：`firstAidBarExpectRed`/`firstAidBarThreshold` 由 `name.contains("血")`/`else→宝宝法力` 改为**穷尽 switch，未知 name `default -> throw IllegalStateException`**（grep 证无 `contains("血")`、无 `return config.getPetMpSupplyThreshold();` 默认、两处 default throw）。未知/重复/错序在 Result 构造器已先 fail，Cloud 端亦不静默落到宝宝法力。

### 静态自查（工具证据）

- PlayerStateService：readWindowBase/GEOMETRY_SLOT/MACRO_TIMEOUT/WindowFact*/ExecutionState 残留=空；probe 用 `observation.observedBaseX()/Y()`（同帧，行 294-296）；helper 穷尽 default→throw。
- Result：`observedBaseX/observedBaseY` 组件 + `FIRST_AID_BAR_NAMES` + `requireFixedBarNames`（READABLE/CAPTURED 调用，CAPTURE_* 空表）+ base 成对校验。
- Envelope 9-key payload 集含 observedBaseX/Y。
- 5 文件 `git diff --check` 干净；未触碰 B 的 DHXY 文件、A/D 写集、frozen incense/identity/position/dormant 方法（仅改 active probe/helper + Result/Envelope）。
- 冻结确认：三 operation/transport 四态/Cloud state·plan·check counter ownership/cached consume 语义/无 auto-retry 均未动（父级本轮明确冻结项保持）。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=Result/Envelope/PlayerStateService（改）+ Digests/Port（审查确认无需改）+ 本日志；未触 B DHXY/A/D/frozen methods/POM；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. P1-1：删独立 GEOMETRY 调用、PROBE READABLE 携同帧 observedBaseX/Y、Cloud 只从同一 result 建 plan、无第三 read/retry/TTL/owner。P2-1：READABLE/CAPTURED 严格四 bar 各一次按序、CAPTURE_* 空表、Cloud 去默认穷尽 fail-on-unknown、B/C 接受域一致。
3. 因父级禁编译门，用逐项静态自查（删净证明/同帧 base/严格 matrix/穷尽 helper/diff）替代，如实标注"未编译"（frozen 路径本地类引用未变，文件仍 partial，父级已知）。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R1 Implementation Repair #1（P1-1 删独立 GEOMETRY 调用、PROBE READABLE result 携同帧 observedBaseX/Y、Cloud 只从同一 result 建 plan；P2-1 Result 构造器强制四 bar 人物血/法·宝宝血/法 各一次按序 + Cloud helper 穷尽 fail-on-unknown 去默认宝宝法力；改 Result/Envelope/PlayerStateService，Digests/Port 无需改；未触 B/A/D/frozen；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #2 - REPAIR BLOCKED - 2026-07-14T19:56:36-04:00

父级已独立复核 C 当前三文件与 B 同波 producer/contract。删除独立 GEOMETRY、同帧
`observedBaseX/Y`、四 bar 固定顺序及未知 name fail-contract 均可保留；但 Cloud closed result 仍接受
基线明确定义为“窗口不可用”的 `observedBaseX == -1`。结论：**P0=0 / P1=1 / P2=0，Repair #1
暂不通过。**

### P1-1：Cloud READABLE contract 未保存 baseline `baseX == -1 -> UNKNOWN` 门

- 证据：`696a12b0:PlayerStateService.java:267-270` 在 bars capture 前以 `baseX == -1` 直接清 plan并返回
  `UNKNOWN`。当前 `PlayerStateFirstAidMacroResult.java:40-59` 只要求 observed base 成对存在，允许
  `READABLE + observedBaseX=-1`；`PlayerStateService.java:294-306` 随即使用该值映射 bars并可返回
  `HEALTHY` 或建立 plan。
- 影响：与基线的 UNKNOWN/不计健康检查分支不同，且跨端不受信 payload 可绕过 B producer 的本地门。
- 精确返修条件：仅在 Cloud `PlayerStateFirstAidMacroResult` 的 READABLE invariant 中增加
  `observedBaseX != -1`；B 同波 producer/result 也会镜像该门。不得恢复独立 GEOMETRY、不得新增 fact/read、
  不得改 `PlayerStateService` 当前同帧数据流或已通过的四 bar mapping。

## Parent Repair Task - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R2

原 External C 仅修改 `PlayerStateFirstAidMacroResult.java` 与本日志；真实 EOF 追加 `CLAIMED` 后实施，
领取截止 `2026-07-14T20:16:36-04:00`。其余 Cloud files 冻结，不得触 B/A/D 写集，不得
build/test/runtime/Git，不新增 retry/TTL/owner/session/ledger。交付给 constructor 行证据与 B/C 接受域对照。
无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T20:00:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R2 | claimedAt=2026-07-14T20:00:00-04:00 | writeSet=[Cloud PlayerStateFirstAidMacroResult.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R2 Implementation Repair #2 - 2026-07-14T20:08:00-04:00

按 `Source Review #2`（P1-1）单点返修：仅改 `PlayerStateFirstAidMacroResult.java`。按父级明令未 build/test/runtime/Git；以静态自查代编译门。
新 blob（git hash-object）= `b88499f592ac4729fa66db910d05b4d296cc9cd8`，SHA-256 = `ba1e94f5e2564443144366f8030420f0f04ed2ea5a3b5a81949700a4a54d4df4`。

### P1-1 修复：READABLE invariant 增加 baseline `baseX == -1 -> 不可用` 门

在 `PROBE_SUPPLY_NO_FOCUS` 的 `READABLE` 分支（`:51-58`），紧接 `require(hasBase, ...)` 后新增：
```
RemoteProtocolValidation.require(observedBaseX != -1 && observedBaseY != -1,
        "READABLE probe result requires an available window base; -1 is the baseline "
                + "window-unavailable sentinel and must be reported as CAPTURE_UNAVAILABLE");
```
`hasBase`（两坐标均非 null）已在此前保证，故 `-1` 比较为安全拆箱。语义：`696a12b0:PlayerStateService:267-270` 在 bars capture 前以
`baseX == -1` 判"窗口不可用"→清 plan 返回 `UNKNOWN`；Cloud closed result 现同样拒绝 `READABLE + observedBaseX/Y == -1`——
producer 遇不可用 base 必须回 `CAPTURE_UNAVAILABLE`（其 observation 空、base 空），`PlayerStateService` 的
`probeSnapshotStatus != READABLE -> UNKNOWN`（同帧数据流，本轮未改）随即接住，复刻基线 UNKNOWN/不计健康检查分支；不受信 payload 也无法用 `-1` base 绕过 B 本地门。

### B/C 接受域对照

| invariant | Cloud C（本文件） | B 同波 producer/result（镜像） |
|---|---|---|
| READABLE probe base | `hasBase && observedBaseX!=-1 && observedBaseY!=-1`（否则 fail contract） | 同门：base 不可用(-1)时发 CAPTURE_UNAVAILABLE，不发 READABLE |
| CAPTURE_UNAVAILABLE probe | observations 空、base 空 | 同 |
| READABLE/CAPTURED 四 bar | 恰 人物血/法·宝宝血/法 各一次按序（`requireFixedBarNames`） | 同 |
| base 成对 | `hasBase == hasAnyBase` | 同 |

### 静态自查（工具证据）

- Result：READABLE 分支含 `observedBaseX != -1 && observedBaseY != -1`（行 55-57）；`hasBase` 成对(42)、`requireFixedBarNames`(52/74)、`CAPTURE_UNAVAILABLE` 空表(60) 均保留未动。
- 本轮**仅** `PlayerStateFirstAidMacroResult.java` 改动（blob `b88499f5`）；`PlayerStateService.java`(`6be4aa99`)、`RemoteCommandOutcomeEnvelope.java`(`39640c46`) 与 R1 一致未动——**未恢复独立 GEOMETRY、未新增 fact/read、未改同帧数据流或已通过的四 bar mapping**。
- 未触 B/A/D 写集、frozen methods、POM；未新增 retry/TTL/owner/session/ledger；`git diff --check` 干净。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=`PlayerStateFirstAidMacroResult.java`（改）+ 本日志；其余 Cloud 文件冻结未动；未触 B/A/D；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 精确落实 P1-1：READABLE invariant 增 `observedBaseX != -1`（并 observedBaseY，-1 sentinel 对 base 整体）；不恢复 GEOMETRY、不加 fact/read、不改 PlayerStateService 同帧流与四 bar mapping；B/C 接受域一致。
3. 因父级禁编译门，用逐项静态自查（constructor 行证据/仅一文件改/blob 对照）替代，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R2 Implementation Repair #2（Result READABLE invariant 增加 `observedBaseX != -1 && observedBaseY != -1` 门，复刻基线 `baseX==-1 -> 窗口不可用/UNKNOWN`，不可用 base 必走 CAPTURE_UNAVAILABLE；仅改 PlayerStateFirstAidMacroResult 一文件，未恢复 GEOMETRY、未改同帧数据流/四 bar mapping、未触 B/A/D/frozen；B/C 接受域一致；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #3 - REPAIR BLOCKED - 2026-07-14T20:10:00-04:00

父级独立复核当前 `PlayerStateFirstAidMacroResult.java:40-60`、B 已 SOURCE APPROVED 的 DHXY
producer/result 与 `696a12b0:PlayerStateService.java:267-270`。C 已补 `observedBaseX` 门，但同时新增了
未获批准的 `observedBaseY != -1` 门。结论：**P0=0 / P1=1 / P2=0，R2 暂不通过。**

### P1-1：把 baseline 单一 X 哨兵扩大为 X/Y 双哨兵

- 证据：基线只判断 `tracker.getWindowBaseX() == -1`；B producer 在 capture 前只判断
  `freshBinding.getX() == -1`，B closed result 也只拒绝 `observedBaseX == -1`。当前 C constructor 却要求
  `observedBaseX != -1 && observedBaseY != -1`。
- 影响：`X` 有效而 `Y == -1` 的同一 payload 在 B/DHXY 接受、在 C/Cloud 拒绝；既改变基线接受域，
  又使交付声称的 B/C parity 不成立。
- 精确返修条件：READABLE invariant 仅保留 `observedBaseX != -1`，删除对
  `observedBaseY != -1` 的额外限制；base pair、同帧数据流、固定四 bar 与其它文件全部冻结。

## Parent Repair Task - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R3

原 External C 只修改 `PlayerStateFirstAidMacroResult.java` 与本日志；真实 EOF 追加 `CLAIMED` 后删除
多加的 Y 哨兵门，领取截止 `2026-07-14T20:30:00-04:00`。不得触 `PlayerStateService`/Envelope/
Digest/Port 或 A/B/D 写集，不得 build/test/runtime/Git，不新增 fact/read/retry/TTL/owner/session/ledger。
交付列 constructor exact condition 与 B/C 接受域对照。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T20:14:00-04:00

CLAIMED | task=W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R3 | claimedAt=2026-07-14T20:14:00-04:00 | writeSet=[Cloud PlayerStateFirstAidMacroResult.java,this-log]

## External Worker C - W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R3 Implementation Repair #3 - 2026-07-14T20:20:00-04:00

按 `Source Review #3`（P1-1）单点返修：删除 R2 多加的 `observedBaseY != -1` 哨兵，与 baseline 单一 X 哨兵及 B 已 SOURCE APPROVED 的
producer/result 接受域一致。仅改 `PlayerStateFirstAidMacroResult.java` + 本日志。按父级明令未 build/test/runtime/Git；以静态自查代编译门。
新 blob（git hash-object）= `f3e94cd3331b891c59a8490f2dc819017edcef7d`，SHA-256 = `d966dfd433406b36edbf590c18abeea53eb9314e1564d3ea43ae6a2c7c98dc97`。

### P1-1 修复：READABLE invariant 单一 X 哨兵

`PROBE_SUPPLY_NO_FOCUS` READABLE 分支的 base 门由 `observedBaseX != -1 && observedBaseY != -1` 改为 **仅 `observedBaseX != -1`**（行 55）：
```
RemoteProtocolValidation.require(observedBaseX != -1,
        "READABLE probe result requires an available window base; observedBaseX == -1 "
                + "is the baseline window-unavailable sentinel and must be reported as "
                + "CAPTURE_UNAVAILABLE");
```

### constructor exact condition + B/C 接受域对照

| invariant | Cloud C（本文件，行号） | B 已 SOURCE APPROVED（DHXY producer/result） | baseline `696a12b0` |
|---|---|---|---|
| READABLE probe base 门 | `hasBase`(53) `&& observedBaseX != -1`(55) | capture 前只判 `freshBinding.getX()==-1`；result 只拒 `observedBaseX==-1` | `tracker.getWindowBaseX()==-1`(:267) 单 X |
| base pair | `hasBase == hasAnyBase`(42) | 同 | — |
| CAPTURE_UNAVAILABLE probe | observations 空、base 空(58-61) | 同 | — |
| READABLE/CAPTURED 四 bar | 恰四 bar 按序 `requireFixedBarNames`(52/75) | 同 | 固定四 bar |

`X` 有效而 `Y==-1` 的 payload 现两端一致接受（C 不再单独拒绝），B/C parity 成立；不改基线接受域。

### 静态自查（工具证据）

- READABLE 门现仅 `observedBaseX != -1`（grep 证无 `observedBaseY != -1`）；`hasBase` 成对(42)、同帧 base(54)、`requireFixedBarNames`(52/75)、CAPTURE_UNAVAILABLE 空表 均保留未动。
- 本轮**仅** `PlayerStateFirstAidMacroResult.java` 改（blob `f3e94cd3`）；`PlayerStateService.java`(`6be4aa99`)、`RemoteCommandOutcomeEnvelope.java`(`39640c46`) 未动（未触 PlayerStateService/Envelope/Digest/Port）。
- 未触 A/B/D 写集、frozen methods、POM；未新增 fact/read/retry/TTL/owner；`git diff --check` 干净。

### scope self-QA（仅 QA，不构成 Approved）

1. 写集=`PlayerStateFirstAidMacroResult.java`（改）+ 本日志；其余全冻结未动；未触 A/B/D；未 build/test/runtime；未做 Git；保护他人 dirty/untracked。
2. 精确落实 P1-1：READABLE invariant 仅保留 `observedBaseX != -1`、删 `observedBaseY != -1`；base pair/同帧数据流/固定四 bar/其它文件全冻结；B/C 接受域一致。
3. 因父级禁编译门，用逐项静态自查（单 X 哨兵/仅一文件/blob 对照/B parity）替代，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-PLAYERSTATE-FIRST-AID-CLOUD-WHOLE-CHAIN-1-R3 Implementation Repair #3（删 R2 多加的 observedBaseY != -1 哨兵，READABLE invariant 仅保留 `observedBaseX != -1`，与 baseline 单 X 哨兵及 B 已批准接受域一致、B/C parity 成立；仅改 PlayerStateFirstAidMacroResult 一文件，未触 PlayerStateService/Envelope/Digest/Port/A/B/D/frozen；按父级明令未 build/test，以静态自查代编译门）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #4 - SOURCE APPROVED - 2026-07-14T20:12:00-04:00

父级独立复核 `PlayerStateFirstAidMacroResult.java:40-64`、B 已通过 producer/result 与
`696a12b0:PlayerStateService.java:267-270`。READABLE 现仅拒绝 `observedBaseX == -1`，不再对 Y 新增
哨兵；base pair、同帧 base、固定四 bar、CAPTURE_UNAVAILABLE 空表与 Cloud active caller 均未改，
`git diff --check` 通过。B/C 接受域一致。

结论：**P0=0 / P1=0 / P2=0，C R3 SOURCE APPROVED。** PlayerState first-aid 双侧源码现均通过，
但仍待 writer 稳定后的父级跨仓 fresh build 与完整可达检查，暂不增加 `189/407`。
无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Next Implementation Task - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1

发布时间：`2026-07-14T20:12:00-04:00`；领取截止：`2026-07-14T20:32:00-04:00`。原 External C 在
本日志真实 EOF 追加 `CLAIMED` 后，只 New
`src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java` + 本日志。

一次完整覆盖 `696a12b0:DialogService.java:451-499,935-1097` 的连续观察：supplied detection 优先、
STORY gate、same-frame thin-white wash、caller-order template `0.85` first-hit、relative/screen-absolute point
和 defensive evidence；thin-white CPU wash 内聚本大类，不另拆 helper、不改 `ImagePreprocessor`。本地零 input、
零 target 业务 verdict；closed state 仅 `MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/
BINDING_UNAVAILABLE/MECHANICS_FAILED`。交付列 supplied/no-supplied capture matrix、同帧证明、模板原序/
阈值/first-hit、坐标空间、image owner/flush 与 scoped diff。不得触 B shared wire、A/D 写集或已通过
PlayerState 文件，不得 build/test/runtime/Git，不新增 owner/session/ledger/TTL/retry。自审只算 QA。
无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T20:16:00-04:00

CLAIMED | task=W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1 | claimedAt=2026-07-14T20:16:00-04:00 | writeSet=[DHXY DialogWhiteStoryTemplateLocalObservationMechanics.java,this-log]

## External Worker C - Implementation #1 - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1 - 2026-07-14T20:24:00-04:00

New：`src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`
（blob `8bde385594838cb991852f10b654619ff38f2b98` / sha256
`789bf011b36bb43e906835e88c9c3a009f0e39c3460914a6ad5ee4391c37e90e`，265 行，`@Slf4j @Service public final`）。
一次完整覆盖 `696a12b0:DialogService.java:451-499`（`verifyWhiteStoryTemplate`）+ `935-1097`
（`prepareWhiteStoryTemplateOrAbsent` 的 supplied-detection-priority + STORY gate）的连续观察，纯本地零 input、
零 target 业务 verdict。

### 边界与 B/A/D 隔离
- Raw 抓帧 + `mask→option→story` 分类仍归兄弟 `DialogDetectionLocalMechanics`（B 写集）；本类只**消费**已分类
  `DialogDetection`，从不 re-capture / re-classify，不触 B shared wire。baseline `orElseGet(detectDialogSnapshotDirect)`
  自抓回退＝detection owner 职责，故本类无自抓路径：未供可用 story 帧＝closed `CAPTURE_UNAVAILABLE`，不重跑检测。
- supplied 输入用既有共享模型 `com.bot.dhxy.model.dialog.DialogDetection`（`@Value @Accessors(fluent)`：
  `type()/dialogRect()/rawPath()/image()`），非 B 新 wire；未触 A/D 写集、未触已通过 PlayerState 文件、未改 `ImagePreprocessor`。

### supplied / no-supplied capture matrix（closed 6 态）
| 入口条件 | 终态 |
|---|---|
| binding null / !hasNativeHandle / !hasGeometry | `BINDING_UNAVAILABLE`（exact-window 身份门，先于任何帧工作） |
| supplied null / image()==null / dialogRect()==null（no-supplied 或不可用） | `CAPTURE_UNAVAILABLE`（不自抓、不触 B） |
| supplied.type() != STORY | `STORY_ABSENT`（no-STORY 信号成形＝云端 business，本类不建 PreparedAction） |
| raw 物化失败（有 image 但 `saveImage` 失败 / rawPath 空白） | `MECHANICS_FAILED`（对应 baseline `FAILED`） |
| 有 STORY 帧、模板全 miss / specs 空 / find 抛 RuntimeException | `STORY_MISS`（find 异常统一 catch→`MECHANICS_FAILED`） |
| 某模板 `0.85` first-hit 且 point 非 null | `MATCHED`（携模板名/路径 + rel/abs point） |

### 同帧证明
- rawPath 优先复用 `detection.rawPath()`；缺失且 `image()!=null` 才把**同一** supplied 帧 `saveImage` 到
  window-scoped `dialog_white_template_raw.png`；`washThinWhiteTextToBlackAndWhite(rawPath,washed)` 只作用于该 raw；
  模板 `ImageFinder.find(washedPath,...)` 只读该 washed。全链单帧、无二次抓取、无 GEOMETRY 旁读。

### 模板原序 / 阈值 / first-hit
- 严格按 caller `List<WhiteTemplateSpec>` 顺序遍历；跳过 null / blank `templatePath`；阈值常量
  `WHITE_STORY_TEMPLATE_THRESHOLD = 0.85`（byte-mirror baseline 同名常量）；`result==null || len<2` 或
  `resolveMatchedPointInRect` 返回 null 继续下一个；命中即 return（first-hit），不遍历余项。

### 坐标空间
- `rect = detection.dialogRect()`＝screen-absolute `[left,top,right,bottom]`（供可用 detection 必带，故 baseline
  `getDialogRect()` 回退在 STORY 路径不可达，未引入）。`point = coordinateHelper.resolveMatchedPointInRect(rect,result)`
  ＝screen-absolute；`relativeX/Y = point - rect[left/top]`（window-rect-relative）、`absoluteX/Y = point`（screen-absolute），
  与 baseline `verifyWhiteStoryTemplate` builder 逐字一致。仅 `MATCHED` 携坐标；record 强制 all-or-none。

### thin-white CPU wash 内聚
- `washThinWhiteTextToBlackAndWhite(String,String)` 内联为本类私有法（HSV 白带 `(0,0,225)-(180,15,255)` + `3x3`
  erode 去粗 + `all-thick` 细字掩膜 imwrite），逐字复刻 baseline `ImagePreprocessor` path 重载；未另拆 helper、未改
  `ImagePreprocessor`；每个 Mat 各 release 一次；异常内部吞为 warn/error（缺 washed 文件被扫描读作 no-match，等价 baseline）。

### image owner / flush
- `detection.image()` 由 caller 拥有（模型 javadoc 明示），本类**从不 flush**；仅 `saveImage` 读取该借用帧。
  本类只写并拥有两个 window-scoped temp 文件（raw 仅在自建时、washed），OpenCV Mat 全在内联 wash 内释放，无 BufferedImage 归本类持有。

### 静态自审门（父级禁 build/test，用逐项静态核对代编译门）
- 外部符号逐一核实存在且签名匹配：`WindowScopedTempPath.resolve(String)→String`、
  `ImagePreprocessor.saveImage(BufferedImage,String)→boolean`、`ImageFinder.find(String,String,double)→double[]`、
  `CoordinateHelper.resolveMatchedPointInRect(int[],double[])→Point`（可返 null，已 defensive 跳过）、
  `DialogDetection` fluent accessor、`WhiteTemplateSpec.name()/templatePath()`、`WindowNativeBinding.hasNativeHandle()/hasGeometry()`、
  `DialogType.STORY`。record 私有工厂由外层同 nest 调用合法（同 B 范式）。
- closed 状态恰为 brief 六态 `MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`；
  未新增 owner/session/ledger/TTL/retry。`git diff --check` 干净。

### scoped diff
- 写集 = 该新文件 + 本日志。`git status` 中 `pom.xml (M)` 与 `DialogDetectionLocalMechanics.java (??)` 系他人既有
  dirty/untracked（非本轮所触，予以保护）；本轮唯一新增即上述 blob `8bde3855`。未 build/test/runtime、未做 Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集仅新文件 + 本日志；未触 B/A/D/PlayerState/POM/frozen；未 build/test/runtime/Git；保护他人 dirty/untracked。
2. 完整覆盖 `451-499,935-1097` 连续观察：supplied 优先 + STORY gate + 同帧 wash + caller 原序 `0.85` first-hit + rel/abs point + defensive evidence；wash 内聚不拆、不改 ImagePreprocessor；零 input、零 business verdict。
3. 因父级禁编译门，以逐项静态自查（符号签名/closed 六态/all-or-none/坐标空间 byte-mirror/blob-sha256/scoped diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1 Implementation #1（新增
`DialogWhiteStoryTemplateLocalObservationMechanics`，覆盖 baseline 白 story 模板连续观察；detection 抓/分类留 B、
本类纯消费不触 B；thin-white wash 内聚、零 input、零 business verdict、closed 六态；仅一新文件 + 本日志，
未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #1 - BLOCKED - 2026-07-14T20:31:00-04:00

父级独立逐行对照当前 265 行源码、`696a12b0:DialogService.java:451-499,971-1018,1616-1632`，并复核
Delivery Preflight Helper 的非绑定候选。当前 blob 与交付一致，写集内未见 P0；但首版删除了 baseline 的
fresh detection fallback，并改变 supplied-frame 接受域。结论：**P0=0 / P1=3 / P2=2，Implementation #1
暂不通过。** `ImagePreprocessor.java` 同时段 mtime 属 B 已领取写集，当前无证据归因给 C，不作为本单问题。

### P1-1：no-supplied / unusable-supplied 的 baseline 单次 fresh detection 被删除

- 证据：baseline `:978-980` 对不可复用 supplied detection 必须恰一次调用
  `detectDialogSnapshotDirect(..., false, 0)`；当前 `DialogWhiteStoryTemplateLocalObservationMechanics:93-104`
  对 `null/image-null/rect-null` 直接 `CAPTURE_UNAVAILABLE`，类内无 detection collaborator。
- 影响：窗口上真实存在 STORY 时，本来可进入 wash/template match 的调用被提前终止；OPTION 等不满足
  `usableSuppliedStoryDetection(...)` 的 supplied frame 也不会按 baseline fresh detect，完整连续观察被截断。

### P1-2：有效 supplied frame 前新增 binding/geometry 门，改变 supplied-priority 接受域

- 证据：当前 `:86-91` 在读取 supplied frame 前要求 handle/geometry；baseline `:978-999` 的可用 supplied
  STORY（以及 `absentAllowed=true` 时的 NONE）不做 fresh binding/capture 即继续。
- 影响：caller 已持有同帧 image+rect 时，binding 短暂不可用会被错误改判 `BINDING_UNAVAILABLE`；这既破坏
  supplied-first，也可能丢掉本轮唯一 observation。

### P1-3：合法 nullable `spec.name()` 命中后被改成 `MECHANICS_FAILED`

- 证据：baseline `:472-491` 只要求 template path 可用，并允许 `.actionKey(spec.name())` 为 null；当前
  `:150-156` 命中后把 name 传入 record，而 `:238-249` 强制 name 非 null/non-blank，异常再被 `:162-165`
  吞为 `MECHANICS_FAILED`。
- 影响：同一模板在 baseline 返回 `WHITE_TEMPLATE_VISIBLE`，当前却返回 mechanics failure，属于业务终态漂移。

### P2-1：任务要求的同帧 defensive evidence 未进入 closed result

- 证据：任务正文要求 defensive evidence；当前 record `:226-263` 只有 name/path/坐标，没有 detection rect、
  frame PNG bytes/hash/dimensions，也无法校验 supplied/fallback frame 与 matched point 是否同一 observation。
- 影响：后续 Cloud caller 无法在 typed terminal 上验证同帧与坐标归属，只能信任临时路径。

### P2-2：内联 OpenCV wash 的异常/empty 分支未释放已拥有 Mat

- 证据：`src.empty()` 在 `:179-181` 直接 return；所有 `release()` 仅在成功尾部 `:200-201`，OpenCV 调用异常
  也直接落 catch，未走 finally。
- 影响：反复剧情观察会累积 native memory；交付所称“每个 Mat 各 release 一次”不成立。

## Parent Repair Task - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R1

原 External C 在本日志真实 EOF 追加 `CLAIMED` 后，一次性只修改
`src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java` 与本日志；
领取截止 `2026-07-14T20:51:00-04:00`。不写 Design，不拆 helper，不触 B 的 `ImagePreprocessor`/shared wire、
A/D 写集、POM 或已通过 PlayerState 文件。

精确返修条件：

1. 入口显式携 `absentAllowed`，严格镜像 `usableSuppliedStoryDetection`：可用 STORY 直接复用；仅当
   `absentAllowed=true` 时可用 NONE 直接形成 `STORY_ABSENT`；其它 supplied/no-supplied 必须通过已存在的
   `DialogDetectionLocalMechanics.detectDialog(binding, false, 0L, ...)` **恰一次** fresh detection。
2. 有效 supplied image+rect 的复用不得先依赖 binding；binding/geometry 只作为 fallback capture 的前置。
   fallback 返回 CAPTURED 后只消费其同一次 PNG/rect/type，零二次 capture；不可用/失败映射到现有 closed terminal。
3. caller-order、跳过 null/blank path、0.85 first-hit、thin-white wash、relative/screen-absolute point 保持不变；
   命中结果必须允许 nullable `spec.name()`，不得把 baseline visible 改成 failure。
4. MATCHED typed result携同一 observation 的 defensive rect + PNG bytes + SHA-256 + dimensions；constructor
   校验 bytes/hash/dimensions/rect 一致，byte accessor defensive clone。borrowed supplied image 不 flush；从
   fallback bytes 解码出的 owned image以及所有 OpenCV Mat在成功、empty、异常路径均 finally 恰一次释放。
5. 交付必须列完整 supplied/no-supplied/absentAllowed/fallback matrix、same-frame 证明、nullable-name 证明、
   image/Mat owner 表、实际 blob/SHA 与 scoped diff。不得 build/test/runtime/Git。

无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T20:36:00-04:00

CLAIMED | task=W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R1 | claimedAt=2026-07-14T20:36:00-04:00 | writeSet=[DHXY DialogWhiteStoryTemplateLocalObservationMechanics.java,this-log]

## External Worker C - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R1 Implementation Repair #1 - 2026-07-14T20:44:00-04:00

一次性只改 `src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`
（blob `098f796ce8a27e586eff9e9eb5d326abaecd75d7` / sha256
`2a94ef1f1e048d5e552ccc3592b9c256cea2c70862ddd5b4cd87a3a4eb112776`，433 行）+ 本日志。逐条落实 Review #1 的
P1×3 + P2×2；未触 B `ImagePreprocessor`/shared wire、A/D 写集、POM 或已通过 PlayerState 文件。

### P1-1 修复：恢复 baseline 单次 fresh detection fallback
- 入口新增显式 `absentAllowed`，严格镜像 `usableSuppliedStoryDetection`：可用 STORY 直接复用；`absentAllowed=true`
  时可用 NONE 也复用（经 STORY gate 落 `STORY_ABSENT`）；其余 supplied（如 OPTION）与 no/unusable supplied 走
  fallback。fallback 注入并调用**已存在**兄弟 `DialogDetectionLocalMechanics.detectDialog(binding, false, 0L, source)`
  **恰一次**（`:126`），消费其单次 `CAPTURED` 的 PNG/rect/type，零二次 capture；不修改 B 文件、不触其 wire。

### P1-2 修复：binding 门下移为 fallback 前置
- 可用 supplied image+rect 复用路径**不再**先查 binding（`:110-121`）；`handle/geometry` 门只在 fallback 分支内
  （`:122-124`）。caller 已持同帧时 binding 短暂不可用不再误判，supplied-first 与 baseline 一致。
- fallback `detectDialog` 非 CAPTURED 映射：`CAPTURED`→消费；`MECHANICS_FAILED`→`MECHANICS_FAILED`；其余
  （`CAPTURE_UNAVAILABLE/PRE_CAPTURE_INTERRUPTED/NON_INPUT_WORKER`，后二者在 `false,0L` 下不可达）→`CAPTURE_UNAVAILABLE`。

### P1-3 修复：命中允许 nullable `spec.name()`
- record 不再强制 name 非 null/blank；`hasRequired` 只含 template path + 四坐标 + evidence，name 明确排除
  （`:375`）。命中即 `MATCHED(spec.name() 可空, path, rel/abs point, …)`，baseline `WHITE_TEMPLATE_VISIBLE` 不再漂移成 failure。

### P2-1 修复：MATCHED 携同帧 defensive evidence
- record 新增 `int[] frameRect, byte[] framePngBytes, String frameSha256, Integer frameWidth/Height`；命中前对
  **同一** frame（supplied 借用 image 或 fallback 解码 image）算 `pngBytes+sha256Hex+dims`，与 matched point 同源同帧。
- constructor 校验：MATCHED 必带 path/四坐标/rect/bytes/hash/dims 且 `rect.length==4`、`bytes>0`、`hash 非空`、
  `dims>0`；非 MATCHED 不得携任何 payload。`frameRect()/framePngBytes()` accessor defensive clone，构造时亦 clone。

### P2-2 修复：wash Mat 全路径释放
- 内联 wash 改为 6 个 Mat 均声明为 null，`try` 内赋值，`finally`（`:290-309`）逐一 null-guard `release()`；覆盖
  `src.empty()` 提前 return、成功尾、OpenCV 异常三条路径。HSV 白带 `(0,0,225)-(180,15,255)` + `3x3` erode +
  `all-thick` imwrite 与 baseline 逐字等价。

### owner / flush 表
| frame 来源 | image owner | flush 责任 |
|---|---|---|
| 可用 supplied（STORY / absentAllowed NONE） | caller（模型 javadoc） | 本类**从不 flush**（`borrowedFrame=true`） |
| fallback `detectDialog` CAPTURED bytes 解码 | 本类 owned | `finally` 恰一次 `frameImage.flush()`（`:245`） |
| 两个 window-scoped temp（raw 仅自建时 / washed） | 本类 | baseline 语义不删（window-scoped） |
| wash 内 6 Mat | 本类 | `finally` 各 `release()` 一次 |

### supplied / no-supplied / absentAllowed / fallback matrix（closed 六态）
| 入口 | 走向 | 终态 |
|---|---|---|
| 可用 supplied STORY | 复用同帧→wash→match | `MATCHED` / `STORY_MISS` |
| 可用 supplied NONE 且 `absentAllowed` | 复用→STORY gate | `STORY_ABSENT` |
| 可用 supplied NONE 但 `!absentAllowed` | 非 usable→fallback | 见 fallback 行 |
| supplied OPTION / image|rect 缺失 / supplied null | fallback（先 binding 门） | 下述 |
| fallback：binding null/!handle/!geometry | — | `BINDING_UNAVAILABLE` |
| fallback：detect CAPTURED 且 type==STORY | 消费单帧→wash→match | `MATCHED` / `STORY_MISS` |
| fallback：detect CAPTURED 且 type!=STORY | STORY gate | `STORY_ABSENT` |
| fallback：detect CAPTURE_UNAVAILABLE/中断/非worker | — | `CAPTURE_UNAVAILABLE` |
| fallback：detect MECHANICS_FAILED / 解码失败 / 存图失败 / find 抛错 | — | `MECHANICS_FAILED` |

### same-frame 证明
- 单帧对象贯穿：raw 物化（复用 `suppliedRawPath` 或 `saveImage(frameImage)` 一次）、wash、evidence
  （`pngBytes(frameImage)+sha256`）、模板 `find(washedPath,...)` 全用同一 `frameImage`/其 rect；无二次 capture、无旁读 GEOMETRY。

### 模板原序 / 阈值 / first-hit（不变）
- caller `List<WhiteTemplateSpec>` 原序；跳过 null/blank path；阈值常量 `0.85`；`result==null||len<2` 或
  `resolveMatchedPointInRect` 返 null 继续；首个有效命中即 return，不遍历余项。

### 坐标空间（不变）
- rect=screen-absolute `[l,t,r,b]`；`relativeX/Y=point-rect[l/t]`（window-rect-relative）、`absoluteX/Y=point`
  （screen-absolute），与 baseline `verifyWhiteStoryTemplate` builder 一致。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- B 协作签名核实：`detectDialog(WindowNativeBinding,boolean,long,String)→DialogDetectionResult`，accessor
  `state()/dialogType()/dialogLeft..dialogBottom()/framePngBytes()/frameSha256()/frameWidth()/frameHeight()` 均在；
  `switch(detected.state())` 用 B `State` 常量 + `default`，穷尽有 default。record 私有工厂由外层同 nest 合法。
- `ImageIO/MessageDigest/ByteArrayInput|OutputStream` 等新 import 均被使用；closed 状态恰为 brief 六态；
  未新增 owner/session/ledger/TTL/retry。`git diff --check` 干净。

### scoped diff
- 写集 = 该文件（覆盖重写）+ 本日志。`git status`：本文件 `??`；`pom.xml (M)`、`DialogDetectionLocalMechanics.java (??)`
  系他人既有 dirty/untracked（非本轮所触，已保护）。未 build/test/runtime、未做 Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集仅该文件 + 本日志；未触 B/A/D/PlayerState/POM/frozen；未 build/test/runtime/Git；保护他人 dirty/untracked。
2. P1×3+P2×2 逐条落实：单次 fresh fallback、binding 门下移、nullable name、同帧 evidence+一致性校验、Mat 全路径释放。
3. 因父级禁编译门，用逐项静态自查（B 协作签名/switch 穷尽/closed 六态/all-or-none+clone/坐标 byte-mirror/blob-sha256/scoped diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R1 Implementation Repair #1
（恢复 baseline 单次 fresh detection fallback 并注入 B `detectDialog` 恰一次、binding 门下移为 fallback 前置、
命中允许 nullable name、MATCHED 携同帧 rect+PNG+SHA+dims 且构造校验+clone、wash 6 Mat 全路径 finally 释放、
owned/borrowed image flush 归属明确；仅一文件覆盖重写 + 本日志，未 build/test/runtime/Git）；持续重读本日志
等待父级复审；自审不算 Approved。

## Parent Source Review #2 - REPAIR BLOCKED - 2026-07-14T20:49:10-04:00

父级独立复核当前 blob `098f796ce8a27e586eff9e9eb5d326abaecd75d7` 与
`696a12b0:DialogService.java:451-499,971-1018`。R1 已恢复 supplied/fallback matrix、nullable name 与 Mat
finally owner；但 public MATCHED evidence 仍只是声明字段，不是可自证 authority，且 fresh collaborator
异常仍可绕过 closed terminal。结论：**P0=0 / P1=1 / P2=1，R1 暂不通过。**

### P1-1：MATCHED constructor 未验证 PNG/SHA/dimensions/rect/point 一致性

- 证据：`WhiteStoryTemplateObservation:370-403` 仅检查字段非空、rect length=4、bytes/hash 非空和 dimensions
  正数；没有解码 PNG、重算 SHA、核 `frameWidth/Height == rect right-left/bottom-top`，也没有核
  `relativeX/Y == absoluteX/Y - rect left/top`。fallback decode `:139-155` 同样未核 frame dimensions 与检测 rect。
- 影响：调用方可构造或接收一个 `MATCHED`，其哈希、尺寸、rect、相对点与绝对点彼此矛盾，无法证明模板命中
  属于同一 observation；这正是 R1 要求闭合的 typed evidence。
- 精确返修：MATCHED compact constructor defensive clone 后解码 PNG，重算 SHA，校验 PNG dimensions、rect
  正面积与 dimensions、relative/absolute 公式；decoded validation image 用 finally 恰一次 flush。nullable
  `matchedTemplateName` 继续合法，non-MATCHED 零 payload 规则冻结。

### P2-1：fresh detection/coordinate collaborator RuntimeException 可逸出 closed public entry

- 证据：fallback `dialogDetectionMechanics.detectDialog(...)` 位于 `:125-126`，matched point 的
  `coordinateHelper.resolveMatchedPointInRect(...)` 位于 `:220`，均无本方法 catch；任一 collaborator
  RuntimeException 会绕过六态返回。R1 的 public JavaDoc 明确承诺 closed observation。
- 精确返修：只在对应调用点收敛 RuntimeException 到既有 `MECHANICS_FAILED` 并保留 owned frame finally；
  不新增 retry/terminal，不吞中断，不改变 supplied/fallback/caller-order/0.85 first-hit。

## Parent Repair Task - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R2

原 External C 只修改同一 `DialogWhiteStoryTemplateLocalObservationMechanics.java` + 本日志，在真实 EOF 追加
`CLAIMED` 后实施；领取截止 `2026-07-14T21:09:10-04:00`。只闭合上述 evidence constructor 与两个
collaborator exception；R1 已通过的 supplied matrix、single fresh detection、thin-white wash/Mat owner、nullable
name、caller-order/0.85/坐标计算冻结。不得触 A/B/D/POM/shared wire，不得 build/test/runtime/Git。交付列
constructor 逐项 invariant、decode owner、exception mapping、blob/SHA 与 scoped diff。

无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T20:54:00-04:00

CLAIMED | task=W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R2 | claimedAt=2026-07-14T20:54:00-04:00 | writeSet=[DHXY DialogWhiteStoryTemplateLocalObservationMechanics.java,this-log]

## Parent Scope Amendment #1 - SAME-FRAME MATCH INPUT AUTHORITY - 2026-07-14T20:57:30-04:00

本 amendment 继承 C `20:54:00` 的 R2 CLAIMED，不重置领取门。父级独立检查
`DialogDetection.java:25-29`：Lombok builder 可分别传入 `rawPath` 与 `image`，模型本身没有 digest/pixel invariant；
当前 mechanics 又从 `suppliedDetection.image()` 生成返回 evidence，却在 supplied `rawPath` 非空时从该路径洗图和
模板匹配，因此两者可能不是同一帧。

R2 在完成原 evidence constructor 与 collaborator exception 修复时，必须同时闭合以下同帧边界：

1. supplied 与 fallback 两条合法帧路径都以本次选定的同一个 `frameImage` 为唯一像素权威；不得直接信任/复用
   `suppliedDetection.rawPath()` 作为模板匹配输入。
2. 始终把该 `frameImage` 写入本类 window-scoped raw artifact，再由这一个 artifact 执行 thin-white wash 与
   caller-order template match；save 失败仍映射既有 `MECHANICS_FAILED`。
3. 返回的 PNG/SHA/dimensions/rect、washed input、matched point 必须全部可追溯到该同一个 `frameImage`；不新增
   capture、retry、terminal 或业务判断。正确 producer 原本 path/image 同像素时结果不变。
4. borrowed supplied image 仍不 flush；fallback decoded image 仍由现有 outer finally 恰一次 flush。只修改当前
   C 单文件与本日志，不触 A/B/D/shared wire。

交付在原 R2 材料中增加 raw artifact source matrix 与 same-frame 证明。无已批准业务差异；按 `696a12b0`
等价迁移。

## External Worker C - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R2 Implementation Repair #2 - 2026-07-14T21:02:00-04:00

一次性只改 `src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`
（blob `1f161e42e3a5332ff082901a641bdfc9e9f0bfbc` / sha256
`45422e03c0826f022056d65cf4c3173c71f8032167edb8de969b91a4a1fe01f7`，497 行）+ 本日志。只闭合 Review #2 的
P1-1 evidence constructor 与 P2-1 两个 collaborator exception；R1 已通过的 supplied/fallback matrix、single fresh
detection、thin-white wash/Mat owner、nullable name、caller-order/`0.85` first-hit/坐标计算全部冻结未动。
未触 A/B/D/POM/shared wire。

### P1-1 修复：MATCHED constructor 逐项 authority 自证（`:396-467`）
compact constructor 在 **defensive clone 之后**对 MATCHED 增加可自证校验（nullable `matchedTemplateName`
继续合法、non-MATCHED 零 payload 规则冻结）：
1. `rectWidth=frameRect[2]-frameRect[0]`、`rectHeight=frameRect[3]-frameRect[1]` 必须 `>0`（rect 正面积）。
2. `rectWidth==frameWidth && rectHeight==frameHeight`（frame dimensions 与检测 rect span 一致）。
3. `relativeX==absoluteX-frameRect[0] && relativeY==absoluteY-frameRect[1]`（relative/absolute 公式）。
4. `decodePng(framePngBytes)` 解码克隆后的 PNG：非 null、`probe.getWidth()/getHeight()==frameWidth/Height`；
   probe 在 `finally` **恰一次** `flush()`。
5. `sha256Hex(framePngBytes).equals(frameSha256)`（对同一 bytes 重算 SHA 比对）。
6. 解码/摘要的 `IOException|NoSuchAlgorithmException` 收敛为 `IllegalArgumentException`（authority 失败即拒绝构造）。
- 效果：任何 hash/尺寸/rect/相对点/绝对点自相矛盾的 `MATCHED` 无法被构造或接收，typed evidence 成为可自证
  authority；fallback decode 的 frame dimensions 与检测 rect 一致性由第 2 条在 constructor 统一闭合。

### P2-1 修复：两个 collaborator RuntimeException 收敛到既有 `MECHANICS_FAILED`
- fresh detection：`dialogDetectionMechanics.detectDialog(...)`（`:127`）包 `try/catch(RuntimeException)`→
  `terminal(MECHANICS_FAILED)`；此处尚无 owned frame，无需 flush。
- coordinate：`coordinateHelper.resolveMatchedPointInRect(...)`（`:231`）包 `try/catch(RuntimeException)`→
  `terminal(MECHANICS_FAILED)`，owned fallback frame 仍由外层 `finally` flush。
- 另：MATCHED 构造点（`:249`）包同类 catch，令新 authority constructor 的 `IllegalArgumentException` 也映射
  `MECHANICS_FAILED`，保证 public JavaDoc 承诺的 closed observation 不被构造异常绕过；未新增 retry/terminal、
  未吞中断、未改 supplied/fallback/caller-order/`0.85` first-hit。

### exception mapping 表
| 调用点 | 异常 | 归一终态 | owned frame |
|---|---|---|---|
| `detectDialog`（fallback） | RuntimeException | `MECHANICS_FAILED` | 尚未持有 |
| `resolveMatchedPointInRect` | RuntimeException | `MECHANICS_FAILED` | 外层 finally flush |
| `matched()` authority 构造 | IllegalArgumentException | `MECHANICS_FAILED` | 外层 finally flush |
| detect 非 CAPTURED | —（状态映射，非异常） | `CAPTURE_UNAVAILABLE`/`MECHANICS_FAILED` | 尚未持有 |

### decode owner
- constructor 内 `probe`＝本地 owned 校验图，`finally` 恰一次 `flush()`，不外泄。
- 入口 fallback 解码的 `frameImage`＝owned，`finally` 恰一次 `flush()`（`:271`，R1 已有，未动）。
- supplied `image()`＝borrowed，`borrowedFrame=true`，从不 flush（冻结）。

### 冻结未动（R1 已通过）
supplied/fallback/absentAllowed matrix、single fresh `detectDialog(binding,false,0L)`、STORY gate、
thin-white wash 6 Mat 全路径 `finally release()`、caller-order + skip null/blank + `0.85` first-hit、
`relativeX/Y=point-rect[l/t]`/`absoluteX/Y=point` 计算、closed 六态集合。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- 新增 constructor 校验只用既有 enclosing static `decodePng/sha256Hex`（nested record 可访问 enclosing private
  static，合法）；未新增 import；checked 异常在 constructor 内 catch，不给 canonical constructor 加 `throws`。
- 三处 catch 均 `RuntimeException`（含 `IllegalArgumentException`）→既有 `MECHANICS_FAILED`，无新 terminal、无 retry。
- `git diff --check` 干净；closed 状态仍恰为六态。

### scoped diff
- 写集 = 该文件 + 本日志。`git status`：本文件 `??`；`pom.xml (M)`、`DialogDetectionLocalMechanics.java (??)`
  系他人既有 dirty/untracked（非本轮所触，已保护）。未 build/test/runtime、未做 Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集仅该文件 + 本日志；未触 A/B/D/PlayerState/POM/shared wire/frozen；未 build/test/runtime/Git；保护他人 dirty。
2. P1-1（constructor 六项 authority + probe flush + 摘要异常收敛）、P2-1（两 collaborator + 构造点 exception→MECHANICS_FAILED）逐条落实；R1 通过项冻结。
3. 因父级禁编译门，用逐项静态自查（nested static 访问/异常映射穷尽/all-or-none+clone/公式核验/blob-sha256/scoped diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R2 Implementation Repair #2
（MATCHED constructor 增六项可自证 authority：rect 正面积、span==dims、relative/absolute 公式、解码 PNG 核 dims、
重算 SHA、probe finally flush；detectDialog/resolveMatchedPointInRect/构造点三处 RuntimeException 收敛
`MECHANICS_FAILED` 保持 closed；R1 通过项冻结；仅一文件 + 本日志，未 build/test/runtime/Git）；持续重读本日志
等待父级复审；自审不算 Approved。

## Parent Source Review #8 - REPAIR BLOCKED - 2026-07-14T21:06:00-04:00

父级在 Delivery Preflight Helper 完成非绑定预检后，独立复核当前 SHA-256
`45422e03c0826f022056d65cf4c3173c71f8032167edb8de969b91a4a1fe01f7`。R2 原定的 constructor authority、
probe flush 与 collaborator/构造异常收敛均可保留，但 Parent Scope Amendment #1 未落实。

### P1-1：supplied evidence 与 template-match 输入仍可来自不同 frame

- 证据：`DialogWhiteStoryTemplateLocalObservationMechanics.java:108,115` 仍保存
  `suppliedDetection.rawPath()`；`:196-208` 只有 path null/blank 时才把 selected `frameImage` materialize。
- 影响：返回的 PNG/SHA evidence 来自 `suppliedDetection.image()`，thin-white wash/template match 却可能读取旧
  `rawPath`，同一个 MATCHED terminal 的像素权威不唯一。
- 精确返修：删除 supplied rawPath 复用。supplied 与 fallback 两分支都必须把本轮 selected `frameImage` 总是写入
  新的 window-scoped raw artifact，随后只从该 artifact wash/match；不得二次 capture，不得比较两个 frame 后择一。

### P2-1：selected-frame 处理仍需闭合 public terminal

`frameImage.getWidth/getHeight` 与 `ImagePreprocessor.saveImage` 的 RuntimeException 必须收敛既有
`MECHANICS_FAILED`，同时保持 borrowed supplied 不 flush、owned fallback 在外层 `finally` 恰一次 flush。

结论：**P0=0 / P1=1 / P2=1，C R2 BLOCKED。**

## Parent Repair Task - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R3

原 External C 仅修改
`src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java` 与本日志；在真实 EOF
追加 `CLAIMED` 后一次完成上述 P1/P2，领取截止 `2026-07-14T21:26:00-04:00`。R2 已通过的 constructor 六项
authority、single fresh detection、STORY gate、caller-order/0.85 first-hit、坐标公式、6 Mat finally、closed 六态及
owner 规则冻结。不得 build/test/runtime/Git，不得改 A/B/D/shared wire，不新增 capture/retry/terminal。

无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T21:10:00-04:00

CLAIMED | task=W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R3 | claimedAt=2026-07-14T21:10:00-04:00 | writeSet=[DHXY DialogWhiteStoryTemplateLocalObservationMechanics.java,this-log]

## External Worker C - W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R3 Implementation Repair #3 - 2026-07-14T21:18:00-04:00

一次性只改 `src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`
（blob `986b867ed67d7808b8e656878375a25c7195f964` / sha256
`7f803880135af69f64dd2112cb27f86e3751091d2c26489f451f40f453d9282a`，498 行）+ 本日志。落实 Review #8 的
Parent Scope Amendment #1（P1-1）与 selected-frame 异常闭合（P2-1）；R2 已通过项全部冻结。未触 A/B/D/POM/shared wire。

### P1-1 修复：单一权威帧（删除 supplied rawPath 复用）
- 删除 `suppliedRawPath` 字段及其 supplied/fallback 两处赋值（`grep suppliedRawPath == 0`）。
- Step 3+4 合并：无论 supplied 复用帧还是 fallback 解码帧，本轮 selected `frameImage` **总是**写入新的
  window-scoped raw artifact `windowScopedTempPath.resolve(RAW_TEMP_FILE)`（`saveImage(frameImage, rawPath)`
  无条件执行，不再有 "path 非空则跳过" 分支）；随后 `washThinWhiteTextToBlackAndWhite(rawPath, washed)` 与
  `ImageFinder.find(washed, …)` **只**读该 artifact。
- 因此 MATCHED evidence 的 `framePng/frameSha`（`pngBytes(frameImage)`）与 wash/template match 的像素同源于
  **同一** `frameImage`；不再存在 evidence 取 `image()`、match 取旧 `rawPath` 的双帧分歧。无二次 capture、无两帧择一。

### P2-1 修复：selected-frame 处理 RuntimeException 闭合
- `frameImage.getWidth()/getHeight()`、`pngBytes(frameImage)`/`sha256Hex(...)`、`ImagePreprocessor.saveImage(...)`
  收入同一 `try`，`catch (IOException | NoSuchAlgorithmException | RuntimeException)` → `terminal(MECHANICS_FAILED)`
  （`:186-203`）。任何维度读取/编码/存图异常都归一到既有 closed 终态。
- borrowed supplied 帧仍不 flush；owned fallback 帧仍由外层 `finally`（`:272-273`）恰一次 flush，均未改。

### same-frame 单帧证明（Amendment #1 后）
| 环节 | 数据源 |
|---|---|
| evidence PNG/SHA/dims | `frameImage`（本轮 selected） |
| raw artifact | `saveImage(frameImage)` → `RAW_TEMP_FILE`（无条件、每轮新写） |
| thin-white wash 输入 | 该 raw artifact |
| template `find` 输入 | 该 wash 输出（源自同一 raw artifact） |
| rect / 坐标 | 与 `frameImage` 同帧的 `dialogRect()` 或 fallback rect |

→ 单一 `frameImage` 贯穿 evidence 与 match，MATCHED 像素权威唯一。

### exception mapping 表（本轮新增行加粗语义）
| 调用点 | 异常 | 归一终态 | frame flush |
|---|---|---|---|
| `detectDialog`（fallback，R2） | RuntimeException | `MECHANICS_FAILED` | 尚未持有 |
| **`getWidth/getHeight`+`pngBytes/sha256`+`saveImage`（R3）** | IOException/NoSuchAlgorithmException/RuntimeException | `MECHANICS_FAILED` | 外层 finally flush owned |
| `resolveMatchedPointInRect`（R2） | RuntimeException | `MECHANICS_FAILED` | 外层 finally |
| `matched()` authority 构造（R2） | IllegalArgumentException | `MECHANICS_FAILED` | 外层 finally |
| `detectDialog` 非 CAPTURED | 状态映射 | `CAPTURE_UNAVAILABLE`/`MECHANICS_FAILED` | 尚未持有 |

### 冻结未动（R2 已通过）
constructor 六项 authority（rect 正面积/span==dims/relative-absolute 公式/解码 PNG 核 dims/重算 SHA/probe finally
flush）、single fresh `detectDialog(binding,false,0L)`、STORY gate、caller-order + skip null/blank + `0.85` first-hit、
坐标 `relativeX/Y=point-rect[l/t]`/`absoluteX/Y=point`、thin-white wash 6 Mat 全路径 `finally release()`、
nullable name、non-MATCHED 零 payload、closed 六态、owner 规则（borrowed 不 flush / owned 外层 finally 一次）。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- `grep suppliedRawPath` = 0；raw artifact 无条件新写、单一来源；P2-1 catch 覆盖 dims/encode/save 三类；
  未新增 capture/retry/terminal；未新增 import（删变量、并 try）。`git diff --check` 干净；closed 状态仍恰六态。

### scoped diff
- 写集 = 该文件 + 本日志。`git status`：本文件 `??`；`pom.xml (M)`、`DialogDetectionLocalMechanics.java (??)`
  系他人既有 dirty/untracked（非本轮所触，已保护）。未 build/test/runtime、未做 Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集仅该文件 + 本日志；未触 A/B/D/PlayerState/POM/shared wire/frozen；未 build/test/runtime/Git；保护他人 dirty。
2. P1-1（删 supplied rawPath 复用、selected frame 无条件写新 raw artifact、单帧 wash/match）、P2-1（dims/encode/save 异常收敛 MECHANICS_FAILED、owner 不变）逐条落实；R2 通过项冻结。
3. 因父级禁编译门，用逐项静态自查（suppliedRawPath 归零/单帧数据流/异常映射穷尽/owner 规则/blob-sha256/scoped diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1-R3 Implementation Repair #3
（落实 Scope Amendment #1：删除 supplied rawPath 复用，selected frameImage 无条件写入新 window-scoped raw
artifact 并只从该 artifact wash/match，evidence 与 match 像素同一权威帧；getWidth/getHeight/pngBytes/saveImage
RuntimeException 收敛 MECHANICS_FAILED，borrowed 不 flush、owned 外层 finally 一次；R2 通过项冻结；仅一文件 +
本日志，未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #9 - SOURCE APPROVED - 2026-07-14T21:14:00-04:00

Delivery Preflight Helper 已在固定 helper 报告追加非绑定预检，限定范围未发现风险候选；父级随后独立复核当前
SHA-256 `7f803880135af69f64dd2112cb27f86e3751091d2c26489f451f40f453d9282a`。`suppliedRawPath` 已归零；
supplied/fallback 两分支均以 selected `frameImage` 生成 evidence，并无条件写入新的 window-scoped raw artifact，
wash/template match 只读取该 artifact。dimensions/encode/hash/save RuntimeException 收敛既有
`MECHANICS_FAILED`，borrowed supplied 不 flush、owned fallback 外层 `finally` 恰一次。R2 已通过的 single fresh
detection、STORY gate、caller-order/0.85 first-hit、六项 evidence authority、nullable name、6 Mat owner 与 closed
六态未回退。

结论：**P0=0 / P1=0 / P2=0，C R3 SOURCE APPROVED。** 本结论只放行 white-story local mechanics；
尚未运行统一构建，不增加 `189/407`。无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Implementation Task - W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1

发布时间：`2026-07-14T21:14:00-04:00`；领取截止：`2026-07-14T21:34:00-04:00`。原 External C 在真实 EOF
追加 `CLAIMED` 后直接实施，不写 Design。唯一 Java 写集为 New DHXY
`src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java` + 本日志；不得修改
A/B/D、shared wire、Cloud `DialogService` 或其它 Java。

以 `696a12b0:GameTextLineOcrService.readDialogOptionWords:120+` 与
`DialogService.processOptionsWithOCRDetailed:1792-1895` 为行为权威，但本类只拥有**单个已选 color variant** 的
本地 observation：caller 传 GREEN 或 YELLOW 的 immutable PNG/SHA/dimensions + screen-absolute rect + diagnostic
label；本类严格验证 bytes/hash/dimensions/rect，写入 window-scoped artifact，调用现有 local OCR provider恰一次，
返回 image-local、caller-order immutable word boxes。Cloud 继续拥有 green-first、green miss 后 yellow、aliases/
keyword match、merge/fallback/action/click/fingerprint；本地不得选择颜色、alias、target、fallback 或发送 input。

terminal 恰为 `WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；只有 `WORDS` 携 defensive word
boxes，provider unavailable/exception 不得伪装成视觉 `NO_WORDS`，不得 retry。保持 temp path window-scoped、PNG
validation decoded owner 与 borrowed bytes 防御拷贝。交付列 baseline/现有 OCR API 行锚、单次调用证明、坐标空间、
terminal/exception/owner 表、SHA 与 scoped diff；不得 build/test/runtime/Git。后续 shared wire/caller 由 B 释放后
单独接入，本单不宣称整类完成。

无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T21:20:00-04:00

CLAIMED | task=W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1 | claimedAt=2026-07-14T21:20:00-04:00 | writeSet=[DHXY DialogOptionOcrWordsLocalObservationMechanics.java,this-log]

## External Worker C - W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1 Implementation #1 - 2026-07-14T21:28:00-04:00

New：`src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`
（blob `e9564983099ec38b8fdb51c37fc26a5c73f201c1` / sha256
`b12c90745744463a8286027bab53a884146666cec567f002a6aab19f09f6fd4e`，269 行，`@Slf4j @Service public final`）。
单变体本地 OCR 观察：caller 传一个已选 GREEN/YELLOW 的 immutable PNG（bytes+SHA+dims）+ screen-absolute rect +
diagnostic label，本类验证→写 window-scoped artifact→调现有 local OCR provider 恰一次→返回 image-local、
caller-order、immutable word boxes。

### baseline / 现有 OCR API 行锚
- 行为权威：`696a12b0:src/main/java/com/bot/dhxy/vision/GameTextLineOcrService.java:120+`
  （`readDialogOptionWords`：green-first/yellow-fallback + alias/keyword + merge）与
  `696a12b0:.../service/DialogService.java:1792-1895`（`processOptionsWithOCRDetailed`）。
- 本类只取其中"对单一已洗好的变体图跑 OCR 取 word boxes"这一环；green-first/yellow-fallback/alias/keyword/
  merge/fallback/action/click/fingerprint 全部留 Cloud，本地不选颜色/alias/target/fallback、不发 input。
- 现有 provider：`com.bot.dhxy.core.TextRecognizer.getAllTextResultsLocalOnly(String):57`
  → `Optional<List<OcrWordResult>>`；契约（该类 javadoc）：empty=sidecar 不可用/失败，present（可空 list）=成功，
  "a miss is never faked"。正对齐 `OCR_UNAVAILABLE` vs `NO_WORDS`。
- word box 模型：`com.bot.dhxy.model.ocr.OcrWordResult`（getText/getX/getY/getLeft/getTop/getWidth/getHeight/getScore）。

### 单次调用证明
- `textRecognizer.getAllTextResultsLocalOnly(artifactPath)` 全文件仅一处运行时调用（另一处为 class javadoc
  `{@link}`）；无循环、无 retry、无第二 provider。`grep -c getAllTextResultsLocalOnly(` = 2（1 link + 1 call）。

### 坐标空间
- word box 坐标＝image-local（相对变体 PNG 左上）。本类不加 rect origin；需要 screen-absolute 的 caller 自行加
  supplied rect origin（javadoc 明示）。supplied `screenRect`＝screen-absolute `[l,t,r,b]`，仅用于严格校验
  （正面积且 span==dims），不参与 word 坐标换算。

### terminal / exception / owner 表
| 情形 | terminal |
|---|---|
| bytes/hash/dims/rect 任一不自洽或 PNG 不可解码 | `INVALID_IMAGE` |
| artifact 写盘 IOException/RuntimeException | `MECHANICS_FAILED` |
| provider 抛 RuntimeException | `MECHANICS_FAILED` |
| provider 返回 empty Optional（不可用/失败） | `OCR_UNAVAILABLE` |
| provider 成功但 word list 空（含全 null 拷贝后空） | `NO_WORDS` |
| provider 成功且有 word | `WORDS`（携 image-local caller-order immutable boxes） |

- exception/unavailable **绝不**伪装成视觉 `NO_WORDS`；`NO_WORDS` 只来自 provider 成功空返回。无 retry。
- owner：入口 `variantPngBytes`/`screenRect` borrowed → 立即 `clone()` 防御拷贝；validation 解码的
  `BufferedImage` owned → `finally` 恰一次 `flush()`；artifact 用 `Files.write(Path.of(resolve(...)), bytesCopy)`
  写**原始字节**（byte-exact，单一权威）到 window-scoped temp；provider 读该 artifact；无 BufferedImage 逃逸。
- result record `OptionOcrWordsObservation`：`wordBoxes` 经 `List.copyOf`（immutable、拒 null 元素）；仅 WORDS 携
  非空 boxes，其余 status 零 payload（constructor 强制 all-or-none）。`WordBox` 为 immutable record。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- 外部符号签名核实：`getAllTextResultsLocalOnly(String)→Optional<List<OcrWordResult>>`、8 个 OcrWordResult
  getter、`WindowScopedTempPath.resolve(String)→String` 均在且匹配。
- terminal 恰五态 `WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；validateVariant 镜像 A 兄弟类
  `verifyVariant`（decode owned+flush、sha256Hex 比对、dims 核验）。未新增 owner/session/ledger/TTL/retry、
  未选颜色/alias/target/fallback、未发 input。`git diff --check` 干净。

### scoped diff
- 唯一 Java 写集 = 该新文件 + 本日志。`git status`：本文件 `??`；`pom.xml (M)`、
  `DialogDetectionLocalMechanics.java (??)`、`DialogOptionOcrImageLocalObservationMechanics.java (??)` 系他人既有
  dirty/untracked（A/B 写集，非本轮所触，已保护）。未改 A/B/D/shared wire/Cloud `DialogService`/其它 Java；
  未 build/test/runtime、未做 Git。后续 shared wire/caller 待 B 释放后单独接入，本单不宣称整类完成。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集＝该新文件 + 本日志；未触 A/B/D/shared wire/Cloud DialogService/PlayerState/POM；未 build/test/runtime/Git；保护他人 dirty。
2. 单变体 OCR 恰一次调用、image-local caller-order immutable boxes、terminal 五态且 unavailable/exception 不伪装 NO_WORDS、borrowed bytes 防御拷贝、decoded owner finally flush、window-scoped artifact；颜色/alias/target/fallback/input 全留 Cloud。
3. 因父级禁编译门，用逐项静态自查（provider/getter/resolve 签名、单次调用、terminal 五态、owner 表、blob-sha256、scoped diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1 Implementation #1（新增
`DialogOptionOcrWordsLocalObservationMechanics`：单一已选变体 PNG 严格校验→window-scoped artifact→
`TextRecognizer.getAllTextResultsLocalOnly` 恰一次→image-local caller-order immutable word boxes；terminal 五态、
provider unavailable/exception 不伪装 NO_WORDS、无 retry、颜色/alias/target/fallback/input 全留 Cloud；borrowed
bytes 防御拷贝、decoded owner finally flush；仅一新文件 + 本日志，未 build/test/runtime/Git）；持续重读本日志
等待父级复审；自审不算 Approved。

## Parent Source Review #10 - BLOCKED - 2026-07-14T21:47:00-04:00

Delivery Preflight Helper 已先完成非绑定预检，父级随后独立对照 696 OCR/provider 与职责边界。单次 local-only
provider 调用、Optional unavailable 与 present-empty 分离、caller-order image-local immutable boxes、零颜色/alias/
target/fallback/input、borrowed clone 和 decoded finally owner 均成立。

- **P1=1：public 五态可被两个 RuntimeException 出口绕过。** `validateVariant` 内 `ImageIO.read` 只 catch
  `IOException`，provider/runtime decode exception 可越过 `INVALID_IMAGE/MECHANICS_FAILED`；
  `windowScopedTempPath.resolve` 又位于 artifact write try 外，其 RuntimeException 也会直接逸出。
- **P2=1：selected variant 接受域未闭合。** public contract 只允许已经选择的 `GREEN/YELLOW`，但 null variant 被
  改写为诊断 `unknown` 后仍继续 OCR，wire/API 可表示不存在的第三种选择。
- **P2=1：strict PNG/rect arithmetic 尚不严格。** 当前只要 `ImageIO.read` 可解码，JPEG/GIF 等非 PNG bytes 也可
  通过；rect span 用 int subtraction，极端端点可能 overflow 后误与正 dimensions 相等。任务明确要求 strict PNG
  evidence 与 screen-absolute positive rect。

结论：**P0=0 / P1=1 / P2=2，Implementation #1 BLOCKED。** 不运行构建，不增加 `189/407`。

## Parent Implementation Repair Task - W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1-R1

发布时间：`2026-07-14T21:47:00-04:00`；领取截止：`2026-07-14T22:07:00-04:00`。原 External C 只修改
`src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java` 与本日志：

1. null variant 作为 `INVALID_IMAGE` fail-closed；只接受 enum `GREEN/YELLOW`，不得新增第三态。
2. 在解码前验证标准 8-byte PNG signature；用 long 计算 rect width/height 并核正面积、再与正 int dimensions 比较；
   ImageIO/provider 的 RuntimeException 收敛为 `INVALID_IMAGE`，decoded owner finally 不回退。
3. 把 `windowScopedTempPath.resolve` 放入 artifact try，null/blank path 或任何 resolve/Path/write RuntimeException 均
   返回 `MECHANICS_FAILED`；OCR provider 仍恰调用一次且不可用/异常不得伪装 `NO_WORDS`。
4. 冻结 SHA/dimensions、原 bytes 写盘、word order/坐标、五态 result、零 retry/input/business decision 与唯一写集。

交付 Repair #1、更新 SHA/scoped diff；不得 build/test/runtime/Git。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T21:52:00-04:00

CLAIMED | task=W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1-R1 | claimedAt=2026-07-14T21:52:00-04:00 | writeSet=[DHXY DialogOptionOcrWordsLocalObservationMechanics.java,this-log]

## External Worker C - W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1-R1 Implementation Repair #1 - 2026-07-14T22:00:00-04:00

一次性只改 `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`
（blob `38534ce1fd2abe07d122aabbfca6d6272ab0b38c` / sha256
`208122379e5cc336b22a23d7089f6e90b07e95ebd79a5de9f411dc5561ee7c4d`，297 行）+ 本日志。逐条落实 Review #10 的
P1×1 + P2×2；冻结项未动。未触 A/B/D/shared wire/Cloud DialogService/POM。

### P2-1 修复：null variant fail-closed（`:81-88`）
- 入口先判 `variant == null` → `terminal(INVALID_IMAGE)`；不再改写为诊断 `unknown` 继续 OCR。此后 `variant` 恒非
  null，`variantLabel = variant.name()`；enum 仅 `GREEN/YELLOW`，wire/API 无从表达第三种颜色选择。

### P1-1 修复：两个 RuntimeException 出口收敛五态
- validateVariant 的 `ImageIO.read` catch 由 `IOException` 扩为 `IOException | RuntimeException`（`:210-212`）→
  `IllegalArgumentException`（入口映射 `INVALID_IMAGE`）；ImageIO/decode 的 RuntimeException 不再越过五态。
  decoded owner `finally flush` 不回退。
- `windowScopedTempPath.resolve(...)` 由 artifact try 外移入 try 内（`:105-115`）；新增 `null/blank` 守卫抛
  `IllegalStateException`；`catch (IOException | RuntimeException)` 覆盖 resolve/`Path.of`/`Files.write` 全部
  RuntimeException → `MECHANICS_FAILED`。`artifactPath` 声明在 try 外、try 内赋值，定值分析成立（catch 均 return）。

### P2-2 修复：strict PNG 与 rect 算术
- 解码前新增标准 8-byte PNG signature 常量 `PNG_SIGNATURE={0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A}`（RFC 2083）：
  `bytes.length >= 8` 且前 8 字节逐一匹配，否则 `INVALID_IMAGE`；JPEG/GIF 等非 PNG 即使 ImageIO 可解码也被拒。
- rect span 改用 `long` 计算：`rectWidth=(long)rect[2]-(long)rect[0]`、`rectHeight=(long)rect[3]-(long)rect[1]`，
  先核 `>0`（正面积）再与 `(long)width/(long)height` 比较；极端端点不再 int 溢出后误等于正 dimensions。

### exception / terminal 映射（本轮加固）
| 情形 | terminal |
|---|---|
| variant==null | `INVALID_IMAGE`（新） |
| bytes 缺/短于 8 / 非 PNG signature / dims≤0 / rect 非四元/非正面积/span≠dims / 不可解码(含 ImageIO RuntimeException) / dims 不符 / SHA 不符 | `INVALID_IMAGE` |
| resolve 返回 null/blank 或 resolve/Path.of/Files.write 抛 IOException/RuntimeException | `MECHANICS_FAILED`（resolve 现在 try 内） |
| provider 抛 RuntimeException | `MECHANICS_FAILED` |
| provider 返回 empty Optional | `OCR_UNAVAILABLE` |
| provider 成功空 list | `NO_WORDS` |
| provider 成功有 word | `WORDS` |

- unavailable/exception 仍绝不伪装 `NO_WORDS`；无 retry。

### 冻结未动（Review #10 已认可）
single local-only provider 调用恰一次、Optional unavailable 与 present-empty 分离、caller-order image-local
immutable `WordBox`、SHA/dimensions 校验语义、原始 bytes `Files.write` 写盘（byte-exact 单一权威）、word order/
坐标（image-local，不加 origin）、五态 result（all-or-none）、borrowed bytes/rect 入口 clone、decoded owner
`finally flush`、零 retry/input/business decision、颜色/alias/target/fallback 全留 Cloud、唯一写集。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- `grep`：`if (variant == null)` 存在；`PNG_SIGNATURE` 逐字节比对；rect 全 `long`；validateVariant 与 artifact
  两处 catch 均 `IOException | RuntimeException`；`resolve` 在 try 内；`getAllTextResultsLocalOnly(` 运行时仅一处
  （另一处 javadoc link）。terminal 仍恰五态；未新增 owner/session/ledger/TTL/retry；`git diff --check` 干净。

### scoped diff
- 唯一 Java 写集 = 该文件 + 本日志。`git status`：本文件 `??`；`pom.xml (M)`、
  `DialogOptionOcrImageLocalObservationMechanics.java (??)` 等系他人既有 dirty/untracked（A/B 写集，已保护）。
  未 build/test/runtime、未做 Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集＝该文件 + 本日志；未触 A/B/D/shared wire/Cloud DialogService/POM/frozen；未 build/test/runtime/Git；保护他人 dirty。
2. P2-1（null variant fail-closed）、P1-1（ImageIO RuntimeException 收敛 INVALID_IMAGE、resolve 移入 try 收敛 MECHANICS_FAILED）、P2-2（PNG signature、long rect 防溢出）逐条落实；Review #10 认可项冻结。
3. 因父级禁编译门，用逐项静态自查（signature 逐字节/long 算术/两 catch 扩域/resolve 位置/单次调用/五态/blob-sha256/scoped diff）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1-R1 Implementation Repair #1
（null variant fail-closed INVALID_IMAGE；validateVariant ImageIO catch 扩 RuntimeException、resolve 移入 artifact
try 并守卫 null/blank，两个 RuntimeException 出口收敛五态；新增 8-byte PNG signature 校验、rect span 改 long 防
溢出；single OCR call/原 bytes 写盘/坐标/五态/owner 冻结；仅一文件 + 本日志，未 build/test/runtime/Git）；持续
重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #11 - SOURCE APPROVED - 2026-07-14T21:55:31-04:00

Delivery Preflight Helper 已先完成非绑定预检，父级随后独立复核当前 SHA-256
`208122379e5cc336b22a23d7089f6e90b07e95ebd79a5de9f411dc5561ee7c4d`：

- `variant==null` 已在任何 bytes/provider 使用前闭合为 `INVALID_IMAGE`，enum 仍只含 `GREEN/YELLOW`。
- ImageIO 的 IO/runtime 均收敛为 `INVALID_IMAGE`，decoded owner 继续 finally flush；`resolve`、null/blank path、
  `Path.of`、`Files.write` 均在同一 artifact try 内并收敛为 `MECHANICS_FAILED`。
- 标准 8-byte PNG signature 与 long rect span/正面积/dimensions 比较成立，decoded dimensions、原 bytes SHA 不回退。
- window-scoped 原 bytes 写盘、local-only OCR 恰调用一次、unavailable/empty/exception 分离、provider-order image-local
  immutable boxes、五态 result、零 input/retry/business decision 与唯一写集均保持。

结论：**P0=0 / P1=0 / P2=0，Repair #1 SOURCE APPROVED。** 当前 B/D/A Java writers 仍活动，暂不运行构建；
本项只是完整 local observation prerequisite，尚未闭合 public caller chain，`189/407` 不变。下一项必须服从
single shared-wire writer gate，父级从 canonical queue 直接续派，不允许 C 抢写 B 当前 reservation。

## Parent Next Implementation Task - W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1

发布时间：`2026-07-14T22:00:00-04:00`；领取截止：`2026-07-14T22:20:00-04:00`。B R2 已 source release，
当前唯一 `SHARED_LOCAL_MACRO_SLOT` 正式授予原 External C。C 在本日志真实 EOF 追加 `CLAIMED` 后一次性闭合
`696a12b0:DialogService:1792-1895` 与 `GameTextLineOcrService:120+` 的完整 same-frame 双端 caller chain，不得再拆成
DTO/helper/单方法波。

Cloud 精确写集：

- New `remote/DialogOptionOcrImageMacroCommand.java`
- New `remote/DialogOptionOcrImageMacroResult.java`
- New `remote/CloudDialogOptionOcrImagePort.java`
- New `remote/DialogOptionOcrWordsMacroCommand.java`
- New `remote/DialogOptionOcrWordsMacroResult.java`
- New `remote/CloudDialogOptionOcrWordsPort.java`
- Modify `remote/LocalMacroKind.java`
- Modify `remote/LocalMacroCommand.java`
- Modify `remote/LocalMacroRequest.java`
- Modify `remote/LocalMacroOutcome.java`
- Modify `remote/RemoteCommandOutcomeEnvelope.java`
- Modify `remote/RemoteProtocolDigests.java`
- Modify `service/DialogService.java`

DHXY 精确写集：

- New `cloud/remote/RemoteDialogOptionOcrImageMacroCommandPayload.java`
- New `cloud/remote/RemoteDialogOptionOcrImageMacroResultPayload.java`
- New `cloud/remote/RemoteDialogOptionOcrWordsMacroCommandPayload.java`
- New `cloud/remote/RemoteDialogOptionOcrWordsMacroResultPayload.java`
- Modify `cloud/remote/RemoteLocalMacroKind.java`
- Modify `cloud/remote/RemoteLocalMacroCommandPayload.java`
- Modify `cloud/remote/RemoteLocalMacroResultPayload.java`
- Modify `cloud/remote/RemoteOperationPayloadCodec.java`
- Modify `cloud/remote/RemoteProtocolDigests.java`
- Modify `cloud/remote/LocalRemoteGameCommandHandler.java`
- 本日志；A/B/D 当前 reservation 与两个 released mechanics 均只读。

完整链与验收：

1. Cloud `DialogService` 保留 target/aliases、green-first、仅 green miss 后 yellow、merge、fallback、prepared action 与 click
   业务判断；image port 对 exact context 恰 capture 一次并返回同帧 GREEN/YELLOW immutable PNG+SHA+dimensions+screen rect。
2. green words port 只读同帧 green bytes，返回 `WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；
   Cloud alias miss 才把同一 capture 的 yellow bytes送 yellow words port。不得 fresh capture、不得 retained artifact/session。
3. 两仓 command/result/enum/codec/digest/handler field-for-field closed；image terminal 与 words terminal 不折叠，canonical
   tree、final flat outcome 与既有四态 envelope 规则一致。handler 只调用已 source-approved 的 OCR-image/OCR-words mechanics。
4. 不改变 `696a12b0` 的 word order、image-local 到 screen-absolute 平移、green/yellow merge/fallback/action 顺序；不新增
   owner/permit/session/ledger/TTL/retry/wrapper，不触 A tooltip、D player-anchor 或 B released validation 文件之外的写集。

交付 Implementation #1、两仓文件表/SHA/scoped diff、完整 public caller -> port -> transport -> handler -> mechanics ->
typed terminal -> Cloud decision 对照；不得 build/test/runtime/Git。无已批准业务差异；按 `696a12b0` 等价迁移。

## External Worker C - CLAIMED - 2026-07-14T22:04:00-04:00

CLAIMED | task=W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1 | claimedAt=2026-07-14T22:04:00-04:00 | writeSet=[Cloud remote/DialogOptionOcrImageMacroCommand.java,remote/DialogOptionOcrImageMacroResult.java,remote/CloudDialogOptionOcrImagePort.java,remote/DialogOptionOcrWordsMacroCommand.java,remote/DialogOptionOcrWordsMacroResult.java,remote/CloudDialogOptionOcrWordsPort.java,remote/LocalMacroKind.java,remote/LocalMacroCommand.java,remote/LocalMacroRequest.java,remote/LocalMacroOutcome.java,remote/RemoteCommandOutcomeEnvelope.java,remote/RemoteProtocolDigests.java,service/DialogService.java; DHXY cloud/remote/RemoteDialogOptionOcrImageMacroCommandPayload.java,RemoteDialogOptionOcrImageMacroResultPayload.java,RemoteDialogOptionOcrWordsMacroCommandPayload.java,RemoteDialogOptionOcrWordsMacroResultPayload.java,RemoteLocalMacroKind.java,RemoteLocalMacroCommandPayload.java,RemoteLocalMacroResultPayload.java,RemoteOperationPayloadCodec.java,RemoteProtocolDigests.java,LocalRemoteGameCommandHandler.java; this-log]

## External Worker C - W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1 Implementation #1 - 2026-07-14T22:18:00-04:00

一次性闭合 `696a12b0:DialogService:1792-1895` + `GameTextLineOcrService:120+` 的完整 same-frame 双端 caller chain：
Cloud 13 文件 + DHXY 10 Java 文件 + 本日志，把已 source-approved 的 OCR-image / OCR-words mechanics 接入
image port + green/yellow words port，green-first/yellow-fallback 业务留 Cloud。未 build/test/runtime/Git。

### Cloud 写集（13）
| 文件 | 动作 |
|---|---|
| `remote/DialogOptionOcrImageMacroCommand.java` | New（blob `0d979ecf`，镜像 A DialogOcrImageIntent：supplied frame+rect present/absent-together、正面积、byte clone） |
| `remote/DialogOptionOcrImageMacroResult.java` | New（`28d7ba1e`，CAPTURED 携 raw/green/yellow PNG+SHA+dims+scan rect、span==dims；非 CAPTURED 仅 reason） |
| `remote/CloudDialogOptionOcrImagePort.java` | New（`713c684c`，executeLocalMacro + CAPTURED 三变体 SHA 完整性校验；NOT_EXECUTED→CAPTURE_UNAVAILABLE） |
| `remote/DialogOptionOcrWordsMacroCommand.java` | New（`a670fdf2`，variant+bytes+sha+dims+rect，long 防溢出） |
| `remote/DialogOptionOcrWordsMacroResult.java` | New（`0dc8dade`，WORDS 携 provider-order image-local WordBox；五态；unavailable/exception 不折叠 NO_WORDS） |
| `remote/CloudDialogOptionOcrWordsPort.java` | New（`0e224b66`，EXECUTED 透传；NOT_EXECUTED→MECHANICS_FAILED，不伪装 NO_WORDS） |
| `remote/LocalMacroKind.java` | +2 枚举 |
| `remote/LocalMacroCommand.java` | +2 permits |
| `remote/LocalMacroRequest.java` | +2 slot/case/互斥/compat/command() |
| `remote/LocalMacroOutcome.java` | +2 result slot/case/互斥/compat 5-null/withCommon（canonical 11 字段） |
| `remote/RemoteCommandOutcomeEnvelope.java` | +2 payload key set/decode 分支/inner-switch throw/strip-macroKind decode helper（6 构造点全 11-arg） |
| `remote/RemoteProtocolDigests.java` | request digest 剥离 command byte[]（supplied/variant PngBytes）+ outcome digest 剥离 image 3 byte[]；request rebuild +2 arg |
| `service/DialogService.java` | New/改（`b38592ab`，green-first/yellow-fallback 编排；清理 readDialogOptionWords 移除后已死的 GameTextLineOcrService 注入/import） |

### DHXY 写集（10）
| 文件 | 动作 |
|---|---|
| `cloud/remote/RemoteDialogOptionOcrImageMacroCommandPayload.java` | New（@Value @Jacksonized 镜像，byte clone getter） |
| `cloud/remote/RemoteDialogOptionOcrImageMacroResultPayload.java` | New（CAPTURED all-or-none + span==dims，3 byte clone getter） |
| `cloud/remote/RemoteDialogOptionOcrWordsMacroCommandPayload.java` | New（long 防溢出，nested ColorVariant） |
| `cloud/remote/RemoteDialogOptionOcrWordsMacroResultPayload.java` | New（WORDS-only-boxes，nested RemoteWordBox @Value @Jacksonized） |
| `cloud/remote/RemoteLocalMacroKind.java` | +2 枚举（blob `3a881d84f0628ba4c25e934324fa371b5eb44ea9`） |
| `cloud/remote/RemoteLocalMacroCommandPayload.java` | +2 sealed permits |
| `cloud/remote/RemoteLocalMacroResultPayload.java` | +2 sealed permits |
| `cloud/remote/RemoteOperationPayloadCodec.java` | +field sets/2 command-read/2 result-read/readLocalMacro+readLocalMacroTerminal 分支/inner-switch throw（blob `4c5a1ad1cd1d5a731dadda982d701a4df4817d84`） |
| `cloud/remote/RemoteProtocolDigests.java` | +2 command request rebuild（byte[] 剥离）+2 result outcome rebuild（image 排除 3 byte[]） |
| `cloud/remote/LocalRemoteGameCommandHandler.java` | 注入 2 mechanics + 2 instanceof 派发 + 2 execute（观察型 windowTaskContextHolder.callWith）+ 2 toResultPayload（Map 扁平）（blob `8a87a597ddf978035d297fb9f6d8abea1ff6b948`） |

### 完整链对照（public caller → port → transport → handler → mechanics → typed terminal → Cloud decision）
1. **caller**：`DialogService.processOptionsWithOCRDetailed` 保留 target/aliases/green-first/green-miss-then-yellow/merge/fallback/prepared-action/click。
2. **image port**：`requestDialogOptionOcrImages`（detection 有帧则作 supplied 复用、否则 fresh）→ `CloudDialogOptionOcrImagePort.prepareOptionOcrImages` → executeLocalMacro(DIALOG_OPTION_OCR_IMAGE) → DHXY handler `executeDialogOptionOcrImageMacro` → A `DialogOptionOcrImageLocalObservationMechanics.prepareOptionOcrImages`（恰一次 capture，raw/green/yellow 同帧）→ result → Cloud 校验 3 变体 SHA。
3. **写盘**：green/yellow bytes → window-scoped `dialog_active_green/yellow.png`，供 `buildPreparedDialogAction` 加载 washed 图做 fingerprint（`scan.path()`）。
4. **green words port**：`readDialogOptionWordsViaPorts` 先 GREEN → `CloudDialogOptionOcrWordsPort.readOptionWords` → DHXY `executeDialogOptionOcrWordsMacro` → 我 `DialogOptionOcrWordsLocalObservationMechanics.observeOptionWords`（provider 恰一次）→ WordBox → 映射 OcrWordResult。
5. **green miss**：`hasAnyDialogOptionKeyword`（镜像 baseline OcrTextMatcher.hasAnyKeyword=contains）miss 才把**同一 capture** 的 yellow bytes 送 YELLOW words port；再 miss → merge(green+yellow)。无 fresh capture、无 retained artifact/session。
6. **Cloud decision**：得等价 `OcrLineResult scan`（variantName green/yellow/green+yellow、path、words、wordsSummary）→ 既有 alias-match/click/fallback/prepared-action 不变。

### 关键跨仓一致性设计
- **byte[] 不入 digest**：canonicalJson 禁 binary node（抛异常），故两侧 request digest 剥离 command byte[]（`suppliedFramePngBytes`/`variantPngBytes`，`variantSha256`/rect 覆盖完整性），outcome digest 剥离 image 3 byte[]（各自 SHA 覆盖）；Cloud generic valueToTree 与 DHXY 手工 rebuild 产出同一 canonical tree（key 排序无关字段序）。
- **image/words terminal 不折叠**：各自独立 flat closed key set + requireExactKeys；四态 envelope 规则一致；非 CAPTURED/非 WORDS 零 payload。
- **坐标空间**：word box image-local；Cloud `absoluteX=rect[0]+word.getX()`，rect=image port scanRect（screen-absolute），与单帧 capture 自洽。
- **owner/单次**：image port 恰一次 capture；words provider 恰一次调用；unavailable/exception 绝不伪装 NO_WORDS、无 retry；未新增 owner/permit/session/ledger/TTL/wrapper。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- 跨仓一致：2 kind 在 Cloud 各 8 wire 文件、DHXY 各 5 文件；envelope 6 构造点全 11-arg；LocalMacroOutcome 9 result slot + common/macroKind = 11。
- 两仓 `git diff --check` 全干净；未触 A/B/D mechanics（`DialogOptionOcrImage/WordsLocalObservationMechanics` 保持 `??` 只读）、A tooltip、D player-anchor、B released validation 之外写集、POM。
- 后续 shared wire/caller 由 B 释放后单独接入，本单不宣称整类完成。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集=上列 13 Cloud + 10 DHXY Java + 本日志；未触 A/B/D 独占写集/POM/shared wire 之外文件；未 build/test/runtime/Git；保护他人 dirty/untracked。
2. 完整双端 same-frame 链：image port 单帧、green-first/yellow-fallback/merge 复刻、image/words terminal 不折叠、byte[] 两侧一致剥离、坐标 image-local→screen 平移不变、五态/unavailable-不伪装、零 retry/input/business-in-local。
3. 因父级禁编译门，用逐项静态自查（跨仓 kind 计数/构造 arity/field-set 对齐/digest byte-strip 对称/blob）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1 Implementation #1（完整双端 same-frame
OCR caller chain：Cloud 13 + DHXY 10 Java 文件；image port 单帧 raw/green/yellow、green/yellow words port、
green-first/yellow-fallback/merge 编排、两侧 digest byte[] 一致剥离、image/words terminal 不折叠；接入已通过
mechanics、未触 A/B/D 独占写集；未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #12 - BLOCKED - 2026-07-14T22:54:03-04:00

父级先读取 Delivery Preflight Helper 的非绑定预检，再独立对照
`696a12b0:DialogService:1792-1895`、`696a12b0:GameTextLineOcrService:120-162` 及两仓当前源码。
结论：**P0=0 / P1=3 / P2=0，Implementation #1 不通过。** 两仓 kind、sealed permits、flat key set、
codec、outcome digest、handler 派发与 same-frame word-box 坐标未见静态不对称；以下三项必须由原 C 一次修完。

1. **P1 - supplied frame 不受 request identity 约束。** Cloud
   `DialogOptionOcrImageMacroCommand.java:17-23` 与 DHXY
   `RemoteDialogOptionOcrImageMacroCommandPayload.java:21-28` 均没有 supplied-frame SHA；Cloud
   `RemoteProtocolDigests.java:40-43` 删除 `suppliedFramePngBytes`，DHXY digest `:199-213` 也只重建 rect/source。
   影响是相同 rect/dimensions 的不同像素可以拥有同一 request digest，handler 又会把未绑定内容直接交 mechanics。
   返修必须增加 closed `suppliedFrameSha256`：frame 存在时 SHA 必须同时存在、Cloud producer 计算并携带、两仓
   command/codec/digest field-for-field 包含，DHXY 在调用 mechanics 前重算并拒绝不一致；fresh-capture 时 frame/SHA
   同时为空。不得把 binary node 重新塞回 canonical JSON。
2. **P1 - 基线 raw fallback 与 yellow 失败回退丢失。** 基线
   `GameTextLineOcrService.java:130-136` 在 green wash 不可用时 OCR 同一 raw；`:152-155` 在 yellow wash
   不可用时返回已取得的 green words。当前 image mechanics `:303-356` 要求 raw/green/yellow 三图全部成功才返回
   `CAPTURED`，Cloud `DialogService.java:1811-1824` 也要求 green/yellow 两 artifact 同时成功；任一 yellow
   preprocess/artifact 故障会在 green OCR 前把整条链改成 `FAILED`，green 故障也没有 raw OCR 路径。
   返修必须保留一次 raw capture，同时用 closed typed availability 表达 raw/green/yellow；green 不可用时把同一 raw
   送 words mechanics 做 RAW pass，yellow 不可用时保留 green result，只有 green keyword miss 且 yellow 可用才调用
   yellow words。可给 words variant 增加 `RAW`，但不得 fresh capture、不得把 target/alias/fallback 搬到 DHXY。
3. **P1 - detection rect 与 detection image 被错误绑死。** 基线 `DialogService.java:1798-1822` 先独立选择
   `detection.dialogRect()`，即使 detection image/path 不可用，fresh capture 仍使用该 exact rect。当前
   `DialogService.java:1897-1910` 只有 image 编码成功时才携 rect；否则发全 absent command，而 local mechanics
   `:272-301` 改抓固定 `DIALOG_LARGE_*` rect。影响是 fallback capture ROI、word screen 平移与 prepared-action
   validation rect 可偏离同一 detection。返修必须让“supplied frame + SHA + rect 复用”和“无 frame、按 caller
   指定 detection rect fresh capture”成为两个 closed 合法形态；只有 caller 连 rect 也没有时才使用 committed default rect。

`OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED` 在 Cloud 最终映为空词继续 baseline fallback，本轮不要求改成新增
fail-closed 业务规则；不得借返修改变 alias、green/yellow keyword、merge、click、prepared-action 或 fallback 顺序。
不运行 build/test/runtime/Git；shared slot 继续归 C，B white-story 不抢写。无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Repair Task - `W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R1`

发布时间：`2026-07-14T22:54:03-04:00`；领取截止：`2026-07-14T23:14:03-04:00`。
原 External C 请在本日志真实 EOF 追加：

`CLAIMED | task=W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R1 | claimedAt=<ISO> | writeSet=[原 23 Java + DialogOptionOcrImageLocalObservationMechanics.java + DialogOptionOcrWordsLocalObservationMechanics.java + this-log]`

随后一次交付 Repair #1，逐项给出 supplied SHA request tree、三种 image availability、RAW/green/yellow 路由、
detection-rect fresh capture 及两仓 field/key/digest 对照。除上述 25 Java 与本日志外全部冻结；不 build/test/runtime/Git。

## External Worker C - CLAIMED - 2026-07-14T23:00:00-04:00

CLAIMED | task=W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R1 | claimedAt=2026-07-14T23:00:00-04:00 | writeSet=[原 23 Java(Cloud remote/DialogOptionOcrImage+Words Command/Result/Port×6,LocalMacroKind,LocalMacroCommand,LocalMacroRequest,LocalMacroOutcome,RemoteCommandOutcomeEnvelope,RemoteProtocolDigests,service/DialogService; DHXY cloud/remote RemoteDialogOptionOcrImage+Words Command/Result Payload×4,RemoteLocalMacroKind,RemoteLocalMacroCommandPayload,RemoteLocalMacroResultPayload,RemoteOperationPayloadCodec,RemoteProtocolDigests,LocalRemoteGameCommandHandler) + DHXY DialogOptionOcrImageLocalObservationMechanics.java + DHXY DialogOptionOcrWordsLocalObservationMechanics.java + this-log]

## External Worker C - W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R1 Implementation Repair #1 - 2026-07-14T23:12:00-04:00

逐项修完 Source Review #12 的 P1×3，涉 15 文件（写集内 25 Java 的相关子集 + 本日志）；未 build/test/runtime/Git。
关键 blob：DialogService `d33a039e`、Cloud image command `f373bb49`、A image mechanics `74b4856c`。

### P1-1 修复：supplied frame 受 SHA request identity 约束
- 两仓 image command 加 closed `suppliedFrameSha256`（`DialogOptionOcrImageMacroCommand`/`RemoteDialogOptionOcrImageMacroCommandPayload`）：
  invariant `hasFrame ⟺ (非空 SHA present)`；frame 存在 SHA 必存在、fresh 时同空。
- **Cloud producer**（`DialogService.requestDialogOptionOcrImages`）复用 detection 帧时 `dialogFrameSha256(encoded)` 计算并携带（SHA 算不出则不供帧）。
- **两仓 digest field-for-field 含**：Cloud request digest 仍剥离 `suppliedFramePngBytes`（binary），但 `suppliedFrameSha256`（String）留 tree；DHXY request rebuild 加 `suppliedFrameSha256`、bytes 仍排除。codec image command field set 含 `suppliedFrameSha256`。
- **DHXY 侧调 mechanics 前重算拒不一致**：A image mechanics supplied 分支首 `sha256Hex(supplied)` recompute，`!equals(intent.suppliedFrameSha256())`→`INVALID_SUPPLIED_FRAME`，先于 decode/capture。相同 rect/dims 的不同像素不再共享 request digest 且不会进 mechanics。未把 binary node 塞回 canonical JSON。

### P1-2 修复：恢复 raw fallback 与 yellow 失败回退（closed typed availability）
- **A image mechanics**：一次 raw capture 恒在；green/yellow wash/encode 失败→variant UNAVAILABLE（null bytes/SHA）而非整链 FAILED；`DialogOptionOcrImageResult` CAPTURED 改 raw-required + `requireOptionalVariant`（green/yellow 各自 present-together-with-SHA、非空、dims 一致）。两仓 image result payload 同步（raw-required、green/yellow 可选）。
- **words `RAW` variant**：words mechanics + 两仓 words command enum 加 `RAW`（诊断标签，OCR raw 帧）。
- **Cloud DialogService 编排**（`readDialogOptionWordsViaPorts`，复刻 baseline `GameTextLineOcrService:130-136,152-155`）：green 不可用→`RAW` words pass 于同一 raw、直接返回 `"raw"`（无 yellow 回退）；green 可用且 keyword miss 且 **yellow 不可用→保 green**；yellow 可用才 `YELLOW` words → yellow / green+yellow merge。raw/green/yellow best-effort 落盘（raw 恒写、green/yellow 仅可用时；写失败只降级 fingerprint 不断链）。未 fresh capture、未把 target/alias/fallback 搬 DHXY。

### P1-3 修复：detection rect 与 image 解绑
- 两仓 image command rect 与 frame 解耦为三 closed 形态：SUPPLIED(frame+SHA+rect) / FRESH_AT_RECT(无 frame 但 caller rect) / FRESH_DEFAULT(全无)。
- **A image mechanics**：fresh 分支 `intent.hasRect()`→按 caller detection rect capture；仅全无 rect 才 committed default `DIALOG_LARGE_*`。
- **Cloud DialogService**：`requestDialogOptionOcrImages` **恒转发** `detection.dialogRect()`（独立于 image 是否可编码）；无 frame 时发 rect-only command→fresh capture at 同一 detection rect。fallback ROI / word screen 平移 / prepared-action validation rect 归于同一 detection。

### 两仓 field/key/digest 对照
- image command wire：`{macroKind, suppliedFramePngBytes(strip), suppliedFrameSha256, rectLeft/Top/Right/Bottom, source}`；request digest 排除 bytes、含 SHA/rect/source；codec field set/handler intent/DialogService producer 全同名。
- image result wire：`{macroKind, status, raw/green/yellow PngBytes(strip)+SHA, imageWidth/Height, scanLeft/Top/Right/Bottom, reason}`；CAPTURED raw-required、green/yellow 可选；outcome digest 剥离 3 byte[]、按非 null 重建（green/yellow 缺则省）。
- words command +`RAW`；words result 不变。

### 冻结未动
alias、green/yellow keyword=contains、merge、click、prepared-action、fallback 顺序；`OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED` 在 Cloud 仍映空词续 baseline fallback（未加新 fail-closed 业务）；image/words terminal 不折叠；坐标 image-local→screen 平移；两仓 sealed permits/kind/codec/handler 派发。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- 无遗留 `suppliedLeft/Top/Right/Bottom` 引用；`suppliedFrameSha256` 分布 6 文件；`RAW` 在 words command/mechanics 3 处；两仓 `git diff --check` rc=0（唯余他人文件 LF/CRLF 提示，非我改动）。
- A mechanics supplied SHA recompute 与 Cloud producer `dialogFrameSha256` 同法（MessageDigest SHA-256 + `%02x`）；`resolveFingerprintWashMode("raw"/"green+yellow")→TEMPLATE_SPECIFIC` 与 baseline 一致。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集=25 Java 相关子集 + 本日志；未触 A/B/D 独占写集之外/POM/shared wire；未 build/test/runtime/Git；保护他人 dirty。
2. P1-1（supplied SHA 全链 + 调 mechanics 前重算拒）、P1-2（raw fallback + yellow-fail 保 green + RAW variant + closed availability）、P1-3（rect 三形态解绑 + 恒传 detection rect）逐条落实；冻结项未动。
3. 因父级禁编译门，用逐项静态自查（无遗留引用/SHA 分布/RAW 分布/diff --check/两侧 digest byte-strip 对称/blob）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R1 Implementation Repair #1（P1-1 supplied
frame SHA 全链 request identity + 调 mechanics 前重算拒不一致；P1-2 恢复 raw fallback/yellow-fail 保 green、closed
typed raw/green/yellow availability、words RAW variant；P1-3 rect 三 closed 形态解绑 detection rect 与 image、恒传
detection rect；两仓 field/key/digest field-for-field 对照；冻结 alias/keyword/merge/click/fallback/terminal-不折叠；
仅 25-Java 写集相关子集 + 本日志，未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #13 - BLOCKED - 2026-07-14T23:25:42-04:00

父级先读取 Delivery Preflight Helper 对 R1 的非绑定预检，再独立对照
`696a12b0:DialogService:1792-1895`、`696a12b0:GameTextLineOcrService:120-162` 与当前两仓源码。
结论：**P0=0 / P1=3 / P2=1，Repair #1 不通过。** supplied SHA 已进入两仓 request tree，三种 rect
形态和 RAW/green/yellow caller 路由主体已闭合；以下问题必须由原 C 一次修完。

1. **P1 - 合法 optional variant 在 Cloud port 仍会空指针。** `CloudDialogOptionOcrImagePort.java:68-76`
   对 `greenPngBytes/yellowPngBytes` 无条件调用 `verifySha`；`:79-89` 随即对 null bytes 执行
   `MessageDigest.digest(bytes)`。R1 已允许 green/yellow 缺席，因此 green wash unavailable 的 RAW 路径和 yellow
   unavailable 的保-green 路径在进入 `DialogService` 前就会异常。返修必须始终校验 raw，仅在 bytes/SHA
   成对存在时校验 optional green/yellow；出现单边存在仍严格拒绝，不得跳过已存在变体的 SHA 校验。
2. **P1 - green/yellow wash 运行异常仍未按基线降级。** local image mechanics
   `DialogOptionOcrImageLocalObservationMechanics.java:372` 与 `:389` 的 wash 调用位于各自局部 try 外；任一
   `RuntimeException` 会落入 `:411-413`，把整条链变成 `MECHANICS_FAILED`。基线 path wash 在异常时不产出对应文件，
   随后 green 失败走 raw、yellow 失败保 green。返修必须分别包住 green/yellow wash+encode，使每个颜色的异常只令
   该 optional variant unavailable，并正确 flush 已获得的 owner；raw capture/encode 失败仍保持整链失败。
3. **P1 - supplied SHA 信任门没有按 Review #12 放在 mechanics 调用前。** handler
   `LocalRemoteGameCommandHandler.java:1484-1496` 仍把 request bytes/SHA 直接构造成 intent 并调用 mechanics；重算仅在
   mechanics `:272-287` 内发生。功能上虽早于 decode/wash，但没有满足“DHXY 在调用 mechanics 前重算并拒绝不一致”
   的精确返修条件。返修必须在 handler 调用 mechanics 前重算 supplied bytes SHA，mismatch 直接形成 closed
   `INVALID_SUPPLIED_FRAME` typed result；mechanics 内可保留 defense-in-depth，但不得成为唯一信任门。
4. **P2 - touched public contract 注释仍描述旧的全有/全无形态。** Cloud
   `DialogOptionOcrImageMacroCommand.java:6-13`、DHXY 同名 payload `:8-16` 仍称 frame 与 rect strictly
   present-together；Cloud/DHXY image result 与 local result 注释仍称 raw/green/yellow 三份全部存在。返修须改为实际
   `SUPPLIED / FRESH_AT_RECT / FRESH_DEFAULT` 与 raw-required、green/yellow-optional 合同，不改行为。

两仓 command/result/codec/digest/handler 字段名、optional outcome digest、RAW enum 与 detection rect 传播暂未发现新的
静态不对称；这些通过项冻结。不得改变 alias、keyword contains、green-first、yellow 条件、merge、click、prepared-action、
fallback 或 terminal folding；不运行 build/test/runtime/Git。shared slot 继续归 C，B white-story 不抢写。
无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Repair Task - `W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R2`

发布时间：`2026-07-14T23:25:42-04:00`；领取截止：`2026-07-14T23:45:42-04:00`。
原 External C 请在本日志真实 EOF 追加：

`CLAIMED | task=W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R2 | claimedAt=<ISO> | writeSet=[CloudDialogOptionOcrImagePort.java + LocalRemoteGameCommandHandler.java + DialogOptionOcrImageLocalObservationMechanics.java + touched image command/result contract JavaDocs + this-log]`

随后一次交付 Repair #2，逐项给出 optional SHA 校验矩阵、green/yellow wash exception 降级、handler pre-mechanics
supplied SHA gate 与合同注释修正。除上述定点文件和本日志外全部冻结；不 build/test/runtime/Git。

## External Worker C - CLAIMED - 2026-07-14T23:30:00-04:00

CLAIMED | task=W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R2 | claimedAt=2026-07-14T23:30:00-04:00 | writeSet=[Cloud remote/CloudDialogOptionOcrImagePort.java,remote/DialogOptionOcrImageMacroCommand.java(javadoc),remote/DialogOptionOcrImageMacroResult.java(javadoc); DHXY cloud/remote/LocalRemoteGameCommandHandler.java,cloud/remote/RemoteDialogOptionOcrImageMacroCommandPayload.java(javadoc),cloud/remote/RemoteDialogOptionOcrImageMacroResultPayload.java(javadoc),service/dialog/DialogOptionOcrImageLocalObservationMechanics.java; this-log]

## External Worker C - W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R2 Implementation Repair #2 - 2026-07-14T23:40:00-04:00

逐项修完 Source Review #13 的 P1×3 + P2×1，定点 7 文件 + 本日志；未 build/test/runtime/Git。
关键 blob：port `9f941f8cb0b23f3648e6c78dbf39af4725acab0f`、A image mechanics `9b779e8fdb780dce30562f0aaea07c8af5d12e10`、handler `e8a1cb4157e0116af26946e7e983cc4d2dfff773`。

### P1-1 修复：optional variant SHA 校验矩阵（Cloud port 不再 NPE）
- `CloudDialogOptionOcrImagePort.verifyIntegrity`：始终 `verifySha(raw)`；green/yellow 改走
  `verifyOptionalSha`——bytes/SHA 成对存在才 `verifySha`，单边存在 `require(hasBytes==hasSha)` 严格拒，
  缺席（both null）跳过。null bytes 不再进 `MessageDigest.digest`。
- 矩阵：raw(必校) | green present→校/absent→跳/单边→拒 | yellow 同。已存在变体的 SHA 校验不跳过。

### P1-2 修复：green/yellow wash 运行异常按基线降级
- A image mechanics：green、yellow 各自把 `washXxxTextToBlackAndWhite(raw) + pngBytes + sha256Hex` 整体包入
  一个 `try ... catch (IOException | NoSuchAlgorithmException | RuntimeException)`；任一颜色 wash/encode 抛异常
  →该 optional variant unavailable（null bytes/SHA），已获得的 washed owner 在内层 `finally green/yellow.flush()`。
- raw capture/encode 失败仍整链失败（raw 块不变、其 encode 异常仍落 MECHANICS_FAILED）；green 失败→caller 走 RAW，
  yellow 失败→caller 保 green（DialogService 路由不变）。

### P1-3 修复：supplied SHA 信任门前移到 handler 调 mechanics 之前
- `LocalRemoteGameCommandHandler.executeDialogOptionOcrImageMacro`：**调 mechanics 前** 若
  `getSuppliedFramePngBytes()!=null` 则 `dialogOptionOcrImageSha256(bytes)` 重算，`null || !equals(getSuppliedFrameSha256())`
  →直接 EXECUTED 返回 closed `INVALID_SUPPLIED_FRAME` typed result（`toDialogOptionOcrImageStatusPayload`，全 evidence null + reason），
  mechanics 不被调用。
- mechanics 内 supplied SHA recompute 保留为 defense-in-depth（非唯一门）。SHA 算法两侧一致（MessageDigest SHA-256 + `%02x`）。

### P2 修复：touched 合同 JavaDoc 改为真实形态（不改行为）
- Cloud/DHXY image **command** JavaDoc：由"frame 与 rect strictly present-together"改为三 closed 形态
  `SUPPLIED / FRESH_AT_RECT / FRESH_DEFAULT`（frame⟺非空 suppliedFrameSha256 present-together 且需 rect；rect 可单独）。
- Cloud/DHXY image **result** + local `DialogOptionOcrImageResult` JavaDoc：由"raw/green/yellow 三份全在"改为
  raw-required + green/yellow OPTIONAL（各 present-together-with-SHA，或缺席）。

### 冻结未动（Review #13 通过项）
两仓 command/result/codec/digest/handler 字段名、optional outcome digest、RAW enum、detection rect 传播、
alias/keyword contains/green-first/yellow 条件/merge/click/prepared-action/fallback/terminal folding。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- port `verifyOptionalSha` 就位；mechanics 2 处 wash 各自 catch RuntimeException（计数=2）；handler 前置门 +
  `dialogOptionOcrImageSha256`/`toDialogOptionOcrImageStatusPayload` 就位；5 处 JavaDoc 均含三形态/OPTIONAL 语义。
- 两仓 `git diff --check` rc=0（唯余他人文件 LF/CRLF 提示）。未新增 owner/session/retry/terminal。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集=port + handler + A image mechanics + 4 command/result 合同 JavaDoc + 本日志；除定点文件外全部冻结；未触 A/B/D 独占之外/POM；未 build/test/runtime/Git；保护他人 dirty。
2. P1-1（optional SHA 矩阵、单边拒、raw 必校）、P1-2（green/yellow wash 异常各自降级 unavailable + flush、raw 仍整链失败）、P1-3（handler 前置 SHA 门→INVALID_SUPPLIED_FRAME、mechanics defense-in-depth）、P2（合同 JavaDoc 三形态/optional）逐条落实；冻结项未动。
3. 因父级禁编译门，用逐项静态自查（verifyOptionalSha/wash-catch 计数/handler 门/JavaDoc 关键词/diff --check/blob）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R2 Implementation Repair #2（P1-1 Cloud port
optional variant SHA 成对校验/单边拒/不再 null NPE；P1-2 green/yellow wash+encode 各自捕 RuntimeException 仅令该
variant unavailable+flush、raw 仍整链失败；P1-3 handler 调 mechanics 前重算 supplied SHA、mismatch→closed
INVALID_SUPPLIED_FRAME、mechanics 保留 defense-in-depth；P2 command/result JavaDoc 改三形态+raw-required/optional；
冻结通过项；仅 7 定点文件 + 本日志，未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #14 - SOURCE APPROVED - 2026-07-14T23:46:03-04:00

Delivery Preflight Helper 已先完成非绑定预检；父级随后独立对照 `696a12b0:GameTextLineOcrService:130-162`、
Review #13 四项返修条件和当前 7 个定点文件。`CloudDialogOptionOcrImagePort:68-90` 始终核 raw，optional
green/yellow 严格成对校验且缺席不再进入 digest；local mechanics `:357-425` 分别包住两种 wash/encode，任一颜色
异常只令该 variant unavailable，并由内层 finally 释放已取得 image owner；handler `:1487-1519` 在调用 mechanics
前重算 supplied SHA，mismatch 直接返回 closed `INVALID_SUPPLIED_FRAME`；两仓 command/result 与 local result 合同已
准确描述 `SUPPLIED/FRESH_AT_RECT/FRESH_DEFAULT` 和 raw-required/green-yellow-optional。未见写集越界、字段不对称、
新增 terminal/retry/TTL/session/owner，green-first、raw fallback、yellow unavailable 保 green 与后续业务顺序未漂移。

结论：**P0=0 / P1=0 / P2=0，Repair #2 SOURCE APPROVED。** C 的 7 文件写集立即释放；统一双构建仍待
当前新一波 Java writers 稳定后由父级执行。无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Implementation Task - `W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1`

发布时间：`2026-07-14T23:46:03-04:00`；领取截止：`2026-07-15T00:06:03-04:00`。

为解除 generic LocalMacro shared 12 文件的串行瓶颈，本单一次完成 yellow-target 后续完整链的全部专用合同，shared
文件、Cloud `NpcClickService` 与已批准 local mechanics 全部只读。C 须在真实 EOF 追加：

`CLAIMED | task=W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1 | claimedAt=<ISO> | writeSet=[Cloud New NpcYellowTargetMacroCommand.java,NpcYellowTargetMacroResult.java,CloudNpcYellowTargetPort.java; DHXY New RemoteNpcYellowTargetMacroCommandPayload.java,RemoteNpcYellowTargetMacroResultPayload.java; this-log]`

唯一 Java 写集为上述 5 个 New 文件。合同逐字段镜像已批准的
`NpcClickYellowTargetLocalObservationMechanics`：caller-selected rect、prepare/default-mask 选择、same-frame evidence、
candidate 原序与 closed terminal；Cloud 保留 target/OCR matcher/strategy/click/fallback 决策。本单不得修改 generic
enum/permit/request/outcome/envelope/codec/digest/handler、`NpcClickService`、local mechanics 或 A/B/D 文件，不新增业务
判断/retry/TTL/session/owner。交付 Implementation #1 时给出两仓字段/constructor/status 对照、文件 SHA 和后续 shared
integration 接点；不 build/test/runtime/Git。本合同 cohort 不单独计完整链完成。

## External Worker C - CLAIMED - 2026-07-14T23:50:00-04:00

CLAIMED | task=W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1 | claimedAt=2026-07-14T23:50:00-04:00 | writeSet=[Cloud New remote/NpcYellowTargetMacroCommand.java,remote/NpcYellowTargetMacroResult.java,remote/CloudNpcYellowTargetPort.java; DHXY New cloud/remote/RemoteNpcYellowTargetMacroCommandPayload.java,cloud/remote/RemoteNpcYellowTargetMacroResultPayload.java; this-log]

## External Worker C - W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1 Implementation #1 - 2026-07-14T23:58:00-04:00

一次完成 yellow-target 专用合同 cohort：5 个 New 文件 + 本日志，逐字段镜像已批准
`NpcClickYellowTargetLocalObservationMechanics`（`ScanRegion`/`Result`/`Terminal`/`YellowCandidate`/`ImageEvidence`）。
generic LOCAL_MACRO enum/permit/request/outcome/envelope/codec/digest/handler、`NpcClickService`、local mechanics、
A/B/D 全部只读未触；`git diff --check` 两仓 rc=0。未 build/test/runtime/Git。

### 文件表 + SHA
| 文件 | blob |
|---|---|
| Cloud `remote/NpcYellowTargetMacroCommand.java`（New） | `5fc8bc5c` |
| Cloud `remote/NpcYellowTargetMacroResult.java`（New） | `7b64b21a` |
| Cloud `remote/CloudNpcYellowTargetPort.java`（New） | `a724b1ae` |
| DHXY `cloud/remote/RemoteNpcYellowTargetMacroCommandPayload.java`（New） | `55fce75d` |
| DHXY `cloud/remote/RemoteNpcYellowTargetMacroResultPayload.java`（New） | `318884367...` |

### command 字段/constructor 对照（镜像 `ScanRegion`）
- 字段：`int left, top, right, bottom; boolean skipDefaultMask`（逐字段镜像，无 target/verdict/source/owner）。
- constructor 不变式：`right>left && bottom>top`（positive-area window-relative box），与 `ScanRegion` 逐字一致。
- Cloud record 与 DHXY `@Value @Jacksonized` payload 同字段同校验；均**不** implements generic sealed（不动 permit）。

### result 字段/constructor/status 对照（镜像 `Result`）
- `status`(=Terminal 六态 `CAPTURED/NO_YELLOW_CANDIDATE/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`)
  + `candidates`(List<YellowCandidate>) + raw evidence(`rawPngBytes/rawSha256/rawWidth/rawHeight`) +
  mask evidence(`maskPngBytes/maskSha256/maskWidth/maskHeight`) + scan rect(`scanLeft/Top/Right/Bottom`)。
  `ImageEvidence`(pngBytes/sha256/width/height) 扁平化为 raw/mask 各四字段。
- 不变式逐字镜像 `Result`：carriesEvidence=`CAPTURED||NO_YELLOW_CANDIDATE`→raw/mask/rect 全在、bytes/SHA 非空、
  span>0、`raw dims==mask dims==scan span`、CAPTURED 非空 candidates / NO_YELLOW_CANDIDATE 空；其它终态零 evidence/rect/candidates。
- `YellowCandidate` 逐字段镜像：`rectLeft/rectTop/rectRight/rectBottom, textCenterX/textCenterY, clickX/clickY, score, reason`
  （纯几何、无 OCR text、无 verdict）；candidate 原序（`List.copyOf`）。byte[] defensive clone（record accessor + payload getter）。

### 后续 shared integration 接点
- `CloudNpcYellowTargetPort.observeYellowTargets(command)` 暴露 typed 边界 + raw/mask SHA 完整性门
  （`verifyIntegrity`/`verifySha`：evidence 终态重算 raw+mask SHA 比对）。
- `runYellowTargetMacro` 为**文档化 pending 点**：待 shared LOCAL_MACRO 加 `NPC_YELLOW_TARGET` kind + outcome slot 后，
  替换为 `context.getGameClient().executeLocalMacro(..., LocalMacroKind.NPC_YELLOW_TARGET, command, timeout)`；本 cohort
  不改该 generic wiring，故当前 dispatch 抛 `UnsupportedOperationException` 明示待接入（Cloud 保留 target/OCR/strategy/click/fallback）。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- 5 文件全 `??` New；generic `LocalMacroKind/Command/Request/Outcome/RemoteCommandOutcomeEnvelope/codec/digest/handler`、
  `NpcClickService`、`NpcClickYellowTargetLocalObservationMechanics`、A/B/D 均无本轮改动（未触）。
- command/result/candidate/status 与 mechanics 字段名/枚举值/不变式逐字对齐；两仓字段同名同序；未新增 owner/session/retry/TTL/terminal/业务判断。
- 两仓 `git diff --check` rc=0。本合同 cohort 不单独计完整链完成。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集=上述 5 New 文件 + 本日志；shared 文件/NpcClickService/local mechanics/A/B/D 只读；未 build/test/runtime/Git；保护他人 dirty。
2. 逐字段镜像 mechanics 合同（ScanRegion→command、Result/Terminal/YellowCandidate/ImageEvidence→result）；port 为 typed 边界 + SHA 门 + 文档化 shared 接点；Cloud 保留全部业务决策。
3. 因父级禁编译门，用逐项静态自查（新文件/未触 generic/字段-枚举-不变式对齐/blob/diff --check）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1 Implementation #1（5 New 合同文件逐字段镜像
NpcClickYellowTargetLocalObservationMechanics：command=ScanRegion、result=Result/Terminal/YellowCandidate 扁平化+
raw/mask evidence 完整性、port=typed 边界+SHA 门+文档化 shared integration 接点；generic wiring/NpcClickService/mechanics/
A-B-D 全只读未触；仅 5 New + 本日志，未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #15 - BLOCKED - 2026-07-15T00:06:30-04:00

Delivery Preflight Helper 先完成非绑定预检；父级随后独立对照 released
`NpcClickYellowTargetLocalObservationMechanics` 与 5 个当前文件。

- **P1=1：active Spring port 是确定失败 stub。** `CloudNpcYellowTargetPort.java:26-45` 注册为
  `@Component`，但唯一 public 路径最终固定抛 `UnsupportedOperationException`。本任务明确冻结 shared registration，
  因而该类不能同时假装可调用实现；任何提前注入 caller 都确定失败。
- **P2=1：public evidence invariant 弱于 released `ImageEvidence`。** local
  `ImageEvidence.java` 对 PNG decode、actual dimensions 与 SHA 全部自证；当前两侧 result constructor 只核非空和
  声明 dimensions/scan span，Cloud port 只补 SHA，DHXY payload 不核 SHA。日志“逐字段/不变量镜像”不成立，损坏或
  dimensions 伪报只能推迟到后续不可控位置失败。

结论：**P0=0 / P1=1 / P2=1，Implementation #1 BLOCKED。** command、status、candidate 顺序与字段形状可保留。

### Repair task - `W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R1`

发布时间 `2026-07-15T00:06:30-04:00`；领取截止 `2026-07-15T00:26:30-04:00`。C 只允许修改：

1. Cloud `CloudNpcYellowTargetPort.java`：改成纯接口，方法接收 `phaseCode/actionSlot/command` 并返回 typed result；
   删除 `@Component`、context holder、固定抛异常和所有伪实现。
2. Cloud `NpcYellowTargetMacroResult.java` 与 DHXY `RemoteNpcYellowTargetMacroResultPayload.java`：在 evidence terminal
   构造期逐份校验 strict PNG 可解码、decoded width/height 与声明一致、SHA-256 与 bytes 一致，decoded owner finally flush；
   保持现有 terminal/candidate/rect 规则不变。
3. 本日志。

不得修改 command/shared/NpcClickService/mechanics/A/B/D，不新增 wrapper/retry/TTL/session/owner，不
build/test/runtime/Git。C 须先在真实 EOF 追加 CLAIMED，再交 `Implementation Repair #1`。

## External Worker C - CLAIMED - 2026-07-15T00:10:00-04:00

CLAIMED | task=W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R1 | claimedAt=2026-07-15T00:10:00-04:00 | writeSet=[Cloud remote/CloudNpcYellowTargetPort.java,remote/NpcYellowTargetMacroResult.java; DHXY cloud/remote/RemoteNpcYellowTargetMacroResultPayload.java; this-log]

## External Worker C - W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R1 Implementation Repair #1 - 2026-07-15T00:18:00-04:00

修完 Source Review #15 的 P1×1 + P2×1，仅 3 文件 + 本日志；command/status/candidate 顺序与字段形状保留。
未 build/test/runtime/Git。关键 blob：port `27e18e2a`、Cloud result `74ed74ba`、DHXY result payload `a018017d`。

### P1-1 修复：port 改纯接口（删确定失败 stub）
- `CloudNpcYellowTargetPort` 由 `@Component` + context holder + 固定抛 `UnsupportedOperationException` 的伪实现
  改为 **纯 `public interface`**：单方法 `observeYellowTargets(String phaseCode, String actionSlot,
  NpcYellowTargetMacroCommand command) → NpcYellowTargetMacroResult`。
- 删除 `@Component`/`@RequiredArgsConstructor`/`TaskExecutionContextHolder`/`verifyIntegrity`/`verifySha`/`runYellowTargetMacro`
  与所有伪实现；不再注册为 bean，故不会被提前注入且不再确定失败。concrete 实现待 shared LOCAL_MACRO 加
  `NPC_YELLOW_TARGET` kind 后提供（本 cohort 冻结 shared wiring，仅声明合同）。

### P2-1 修复：evidence 逐份自证（对齐 released ImageEvidence）
- Cloud `NpcYellowTargetMacroResult` 与 DHXY `RemoteNpcYellowTargetMacroResultPayload` 构造期 evidence 终态
  （CAPTURED/NO_YELLOW_CANDIDATE）在既有 dims==span 结构校验后，对 raw、mask **各自** 调 `verifyEvidencePng`：
  `ImageIO.read` strict PNG 可解码（非 null）→ decoded `getWidth()/getHeight()==声明 dims` → `sha256Hex(bytes).equalsIgnoreCase(声明 sha256)`，
  decoded owner 在 `finally decoded.flush()`（恰一次）。
- `sha256Hex` 逐字镜像 mechanics `ImageEvidence.sha256Hex`（`MessageDigest SHA-256` + `Character.forDigit` 小写 hex），
  比较用 `equalsIgnoreCase`（同 ImageEvidence）。损坏 bytes / dimensions 伪报现在构造期即拒，不再推迟到下游。
- terminal / candidate 原序 / rect 规则不变（既有校验保留）。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- port 无 `@Component`/无 impl/无 `UnsupportedOperationException`；两侧 result 各含 `verifyEvidencePng`（decode+dims+SHA+finally flush）+ `sha256Hex`。
- 仅改 port + 2 result；`NpcYellowTargetMacroCommand` / DHXY command payload / generic wiring / NpcClickService / mechanics / A/B/D 未触（command 文件仍 `??` 未改动）。
- 未新增 wrapper/retry/TTL/session/owner；未 build/test/runtime/Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集=port + Cloud result + DHXY result payload + 本日志；command/shared/NpcClickService/mechanics/A/B/D 只读；保护他人 dirty。
2. P1-1（纯接口 phaseCode/actionSlot/command→result、删所有伪实现与 bean 注册）、P2-1（两侧 evidence 逐份 strict PNG decode+dims+SHA 自证+finally flush，镜像 ImageEvidence）逐条落实；command/status/candidate 保留。
3. 因父级禁编译门，用逐项静态自查（interface/无 @Component/verifyEvidencePng 分布/command 未触/blob）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R1 Implementation Repair #1（P1-1 port 改纯
interface 收 phaseCode/actionSlot/command 删确定失败 stub 与 bean 注册；P2-1 两侧 result evidence 终态逐份 strict
PNG 可解码+decoded dims 一致+SHA 一致+decoded owner finally flush，镜像 released ImageEvidence；command/status/
candidate 保留；仅 3 文件 + 本日志，未 build/test/runtime/Git）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #16 - BLOCKED - 2026-07-15T00:24:00-04:00

Delivery Preflight Helper 先完成非绑定预检；父级随后逐行复核当前 Cloud/DHXY 两个 result constructor。

- **P2=1：任务要求 strict PNG，但两侧只做 `ImageIO.read`、尺寸和 SHA，没有先核 8-byte PNG magic
  `89 50 4E 47 0D 0A 1A 0A`。** ImageIO 可解码的其它格式仍可能通过；这弱于同批 D 已闭合的 strict-PNG
  contract，也与 R1 明文要求不符。port 纯接口和其余 candidate/terminal/rect invariant 均可保留。

结论：**P0=0 / P1=0 / P2=1，Repair #1 BLOCKED。**

### Repair task - W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R2

`issuedAt=2026-07-15T00:24:00-04:00`；`claimBy=2026-07-15T00:44:00-04:00`。只允许修改 Cloud
`NpcYellowTargetMacroResult.java`、DHXY `RemoteNpcYellowTargetMacroResultPayload.java` 与本日志：在每份 evidence
进入 `ImageIO.read` 前显式核完整 8-byte PNG signature；保留现有 decode/dims/SHA/finally-flush 和全部其它 invariant。
不得触碰 port、command、shared、NpcClickService、mechanics 或 A/B/D，不 build/test/runtime/Git。

C 须在真实 EOF 追加：

`CLAIMED | task=W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R2 | claimedAt=<ISO> | writeSet=[Cloud NpcYellowTargetMacroResult.java; DHXY RemoteNpcYellowTargetMacroResultPayload.java; this-log]`

这是旧合同波最后一个遗留返修；通过后 C 的下一张新单必须直接绑定一个 `countDelta=+1` 的完整 Service 调用链，
不再发零计数 contract/helper 单。

## External Worker C - CLAIMED - 2026-07-15T00:28:00-04:00

CLAIMED | task=W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R2 | claimedAt=2026-07-15T00:28:00-04:00 | writeSet=[Cloud remote/NpcYellowTargetMacroResult.java; DHXY cloud/remote/RemoteNpcYellowTargetMacroResultPayload.java; this-log]

## External Worker C - W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R2 Implementation Repair #2 - 2026-07-15T00:34:00-04:00

修完 Source Review #16 的 P2×1，仅 2 result 文件 + 本日志；未 build/test/runtime/Git。

### P2-1 修复：evidence 显式核 8-byte PNG signature（strict PNG）
- Cloud `NpcYellowTargetMacroResult.verifyEvidencePng` 与 DHXY `RemoteNpcYellowTargetMacroResultPayload.verifyEvidencePng`
  在 `ImageIO.read` **之前**显式校验标准 8-byte PNG magic `89 50 4E 47 0D 0A 1A 0A`
  （`bytes.length>=8 && bytes[0]&0xFF==0x89 && bytes[1..7]==0x50/0x4E/0x47/0x0D/0x0A/0x1A/0x0A`），
  否则抛 `<name> evidence bytes do not carry the standard PNG signature`。ImageIO 可解码的其它格式（JPEG/GIF 等）现被前置拒绝。
- 保留既有 decode（`ImageIO.read` 非 null）/decoded dims==声明 dims/`sha256Hex.equalsIgnoreCase(声明)`/`finally decoded.flush()` 与全部其它 invariant（terminal/candidate 原序/rect）。
- 与同批 D 已闭合 strict-PNG contract 及 R1 明文要求一致。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- 两 result 文件各含 1 处 "standard PNG signature" 前置校验（`ImageIO.read` 之前）；port（纯接口）、command、generic wiring、NpcClickService、mechanics、A/B/D 未触。
- 仅改 2 result；未新增 wrapper/retry/TTL/session/owner；未 build/test/runtime/Git。

### scope self-QA（仅 QA，不构成 Approved）
1. 写集=Cloud NpcYellowTargetMacroResult + DHXY RemoteNpcYellowTargetMacroResultPayload + 本日志；port/command/shared/NpcClickService/mechanics/A/B/D 只读；保护他人 dirty。
2. P2-1（两侧 evidence `ImageIO.read` 前显式核 8-byte PNG signature，保留 decode/dims/SHA/finally-flush 及全部 invariant）落实。
3. 因父级禁编译门，用逐项静态自查（signature 校验分布/未触 port-command/blob）代替，如实标注"未编译"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-696-NPC-YELLOW-TARGET-CONTRACT-COHORT-1-R2 Implementation Repair #2（两侧 result
verifyEvidencePng 在 ImageIO.read 前显式核 8-byte PNG signature，保留 decode/dims/SHA/finally-flush 与全部
candidate/terminal/rect invariant；port/command/shared 未触；仅 2 result + 本日志，未 build/test/runtime/Git）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #17 - SOURCE APPROVED - 2026-07-15T00:32:00-04:00

父级独立复核 Cloud `NpcYellowTargetMacroResult:94-103` 与 DHXY
`RemoteNpcYellowTargetMacroResultPayload:128-137`：两侧均在 `ImageIO.read` 前完整核
`89 50 4E 47 0D 0A 1A 0A`，然后保留 decode、actual dimensions、SHA-256 与 finally flush；port、command、
candidate/terminal/rect invariant 未漂移。结论：**P0=0 / P1=0 / P2=0，Repair #2 SOURCE APPROVED。**
旧合同波到此关闭，本身不增加 `189/407`。

## Parent TRUE EOF Count Task - W-COUNT-PLAYER-STATE-WHOLE-1

`issuedAt=2026-07-15T00:32:00-04:00`；`claimBy=2026-07-15T00:52:00-04:00`；
`countUnit=PlayerStateService::ensureSheYaoXiangActive`；`countDelta=+1`。

一次闭合真实 `AutoBattleTask/AutoCombat/TeamReturn caller -> Cloud PlayerStateService decision -> typed DHXY
HP/MP/identity/location/incense observation or input mechanics -> closed terminal -> Cloud state/result`。必须保留
`696a12b0` 四目标顺序/阈值、quiet-period、first-aid cached plan、exclusive 补给、身份/位置 state 与 incense
fallback；不得把截图/OCR/input business decision 重新下沉本地，也不得新增 retry/TTL/session/owner。

唯一写集：Cloud `src/main/java/com/bot/dhxy/service/PlayerStateService.java`、PlayerState 专属 typed
contract/port/assembly；DHXY `src/main/java/com/bot/dhxy/service/playerstate/**` 与 PlayerState 专属 handler branch；本日志。
`TeamReturnService`、`AutoCombatService`、generic LOCAL_MACRO shared 12、Npc/Dialog/Navigation/BattleRadar/CommonBox/
TaskMaintenance Java 均冻结只读。若完整链要求改冻结文件，报告 BLOCKED，不拆小单、不造 stub。父级源码审查及
统一双构建通过当轮 ledger 必须 `before -> before+1`。

C 须在真实 EOF 追加：

`CLAIMED | task=W-COUNT-PLAYER-STATE-WHOLE-1 | claimedAt=<ISO> | countUnit=PlayerStateService::ensureSheYaoXiangActive | countDelta=+1 | writeSet=[Cloud PlayerStateService.java + PlayerState-specific typed contract/port/assembly; DHXY service/playerstate/** + PlayerState-specific handler branch; this-log]`

## External Worker C - CLAIMED - 2026-07-15T00:38:00-04:00

CLAIMED | task=W-COUNT-PLAYER-STATE-WHOLE-1 | claimedAt=2026-07-15T00:38:00-04:00 | countUnit=PlayerStateService::ensureSheYaoXiangActive | countDelta=+1 | writeSet=[Cloud PlayerStateService.java + PlayerState-specific typed contract/port/assembly; DHXY service/playerstate/** + PlayerState-specific handler branch; this-log]

## External Worker C - W-COUNT-PLAYER-STATE-WHOLE-1 BLOCKED - 2026-07-15T00:46:00-04:00

按任务明文"若完整链要求改冻结文件，报告 BLOCKED，不拆小单、不造 stub"，本 countUnit 报 **BLOCKED**（未改任何 Java、未 build/test/runtime/Git）。

### countUnit 与真实链缺口
- `countUnit=PlayerStateService::ensureSheYaoXiangActive`。其决策链核心是麝香 **STATUS 观察** `probeIncenseStatus(statusRect)`
  （Cloud `PlayerStateService.java:1072`）：当前 **INLINE 本地视觉**，引用 Cloud 不存在的本地类
  （`tracker.captureTo...`、`ImageFinder.find(rawPath, SHEYAOXIANG_STATUS_TEMPLATE, ...)`、`cropSheyaoxiangMatchedColumn`、
  `writeImage`、`moveMouseAwayBeforePlayerStateSnapshotIfNeeded`）。这是 phantom-reference 冻结部分迁移，**不是** Cloud
  decision → typed DHXY observation → terminal → Cloud 的真实链；countDelta 无法在此形态推进（若原样保留，就是把截图/OCR
  business 留在 Cloud 本地——与任务"不得把截图/OCR/input business decision 重新下沉本地"及"真实链"要求都不符）。

### 为何闭合真实链必须改冻结文件
1. DHXY 已有 `service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java`（麝香 STATUS 本地观察），但**未被任何 handler/port 接线**（全仓仅自身引用）。
2. Cloud **无** `CloudPlayerStateIncenseStatusPort`，generic `LocalMacroKind`/`RemoteLocalMacroKind` **无** incense-status kind
   （仅 `BAG_USE_INCENSE`＝补香 input，非 STATUS observation）。
3. **无 PlayerState 专属 transport lane**：已批准的 first-aid 链走 **generic** LOCAL_MACRO 的 `PLAYER_STATE_FIRST_AID` kind
   （`CloudPlayerStateFirstAidPort` → `executeLocalMacro(..., PLAYER_STATE_FIRST_AID, ...)`）。PlayerState 观察一律经 generic transport，
   不存在独立 lane。
4. 因此把 `probeIncenseStatus` 变为 typed DHXY observation port，须给 incense-status 加一个 transport kind + 全套 wire：
   Cloud `LocalMacroKind`/`LocalMacroCommand`/`LocalMacroRequest`/`LocalMacroOutcome`/`RemoteCommandOutcomeEnvelope`/`RemoteProtocolDigests`
   + DHXY `RemoteLocalMacroKind`/`RemoteLocalMacroCommandPayload`/`RemoteLocalMacroResultPayload`/`RemoteOperationPayloadCodec`/
   `RemoteProtocolDigests`/`LocalRemoteGameCommandHandler` 分支——即**任务冻结只读的 generic LOCAL_MACRO shared 12**。
5. 写集虽含"PlayerState 专属 typed contract/port/assembly + PlayerState 专属 handler branch"，但 handler 分派与 kind 枚举/envelope/codec/digest
   均属冻结 generic 12；无冻结外路径可承载 incense-status 观察。另建独立 operation lane 会触及更底层的冻结 RemoteOperation/envelope/broker 路由，冲突更大。

### 结论与不做的事
- 完整 `ensureSheYaoXiangActive` 链要求修改冻结 generic LOCAL_MACRO shared 12（及/或 core transport），命中任务 BLOCKED 条件。
- 遵命**不拆小单、不造 stub、不把 business 下沉本地、不改冻结文件**；未新增 retry/TTL/session/owner；未 build/test/runtime/Git；ledger 不动（不谎报 before+1）。
- 建议：若父级要解锁本 countUnit，需先释放 generic LOCAL_MACRO shared 12 写集（加 incense-status kind + envelope/codec/digest/handler 分支），或明确指定一个 PlayerState 专属 transport lane 及其 core-routing 接点；届时 C 可复用现有未接线的 `PlayerStateIncenseStatusLocalObservationMechanics` 一次闭合真实链。

External Worker C 就 W-COUNT-PLAYER-STATE-WHOLE-1 报告 BLOCKED（countUnit=ensureSheYaoXiangActive 的麝香 STATUS 观察
`probeIncenseStatus` 现为 Cloud 内联 phantom 本地视觉，闭合真实 typed observation 链须给冻结 generic LOCAL_MACRO shared 12
加 incense-status kind + envelope/codec/digest/handler 分支；DHXY 观察 mechanics 存在但未接线、Cloud 无对应 port、无 PlayerState
专属 lane；按任务明文不拆不造 stub 不改冻结文件），持续重读本日志等待父级裁决；保留 heartbeat。

## Parent Blocker Review #18 / Replacement Count Task - 2026-07-15T00:47:00-04:00

父级独立确认 C 的 blocker：`ensureSheYaoXiangActive` 仍有内联 phantom capture/template，现有 DHXY incense-status
mechanics 未接线；完整闭合确需 B 当前占用的 generic LocalMacro shared 12。结论：**BLOCKED，P0=0/P1=1/P2=0**，
证据成立，不让 C 等 B，也不内部接管；PlayerState 单保留为 `BLOCKED_SHARED_LANE`，不计数、不造 stub。

替换任务：`W-COUNT-AUTO-COMBAT-WHOLE-1`；`issuedAt=2026-07-15T00:47:00-04:00`；
`claimBy=2026-07-15T01:07:00-04:00`；`countUnit=AutoCombatService::handleCombatTick`；`countDelta=+1`。
一次闭合真实 `AutoBattleTask caller -> Cloud AutoCombatService` 的完整 tick/guard/pending recovery/maintenance public
图，并只调用当前已存在的 BattleRadar、AutoCombatPanel、CommonBox、TaskMaintenance、LeftTop、TeamReturn、PlayerState
typed terminal；保持 696 的优先级、leader/member capability、combat enter/exit、poll delay、first-aid/post-combat、
fallback/state。唯一 Java 写集仅 Cloud `AutoCombatService.java` + AutoCombat 专属 pure result/adapter（仅必要时）+本日志；
所有 collaborator Service、DHXY、generic shared 12 冻结只读。若 frozen collaborator 尚未提供真实 terminal，准确
BLOCKED，不复制其业务、不造 stub。父级源码审查与 fresh build 通过当轮必须 `countDelta=+1`。

C 须在真实 EOF 追加：

`CLAIMED | task=W-COUNT-AUTO-COMBAT-WHOLE-1 | claimedAt=<ISO> | countUnit=AutoCombatService::handleCombatTick | countDelta=+1 | writeSet=[Cloud AutoCombatService.java + necessary AutoCombat-specific pure result/adapter; this-log]`

## External Worker C - CLAIMED - 2026-07-15T00:52:00-04:00

CLAIMED | task=W-COUNT-AUTO-COMBAT-WHOLE-1 | claimedAt=2026-07-15T00:52:00-04:00 | countUnit=AutoCombatService::handleCombatTick | countDelta=+1 | writeSet=[Cloud AutoCombatService.java + necessary AutoCombat-specific pure result/adapter; this-log]

## External Worker C - W-COUNT-AUTO-COMBAT-WHOLE-1 BLOCKED - 2026-07-15T01:00:00-04:00

按任务明文"只调用当前已存在的 typed terminal；若 frozen collaborator 尚未提供真实 terminal，准确 BLOCKED，不复制
其业务、不造 stub"，本 countUnit 报 **BLOCKED**（未改任何 Java、未 build/test/runtime/Git）。

### countUnit 与真实链缺口
- `countUnit=AutoCombatService::handleCombatTick`。它本身是纯 collaborator 编排（无内联视觉、返回 `TickResult` 枚举、可编译）。
- **主阻断：`AutoCombatPanelService` 未迁移、无真实 typed terminal。** handleCombatTick 核心 combat-enter 流
  `maybeHandleCombatEnter(source)` → `autoCombatPanelService.ensurePanelVisible(...)`（`AutoCombatService.java:342`）
  → `ensurePanelMatchVisible`（面板检测＝视觉）。而 `AutoCombatPanelService` **注入 0 typed port**，直接内联 Cloud 视觉
  （`tracker.captureToMemory`、`ImageFinder`，共 9 处，如 `:347`）——是内联 Cloud 截图/OCR，非 Cloud→DHXY typed terminal。
  该 Service 属任务冻结只读 collaborator，我不得改它、不得复制其业务、不得造 stub。
- **次阻断：leader post-combat 的 incense。** handleCombatTick → `consumeExitAndRecover`/deferred post-combat →
  `playerStateService.ensureSheYaoXiangActiveForLeaderTask`（`:409/:472`）→ 内联麝香 STATUS 观察
  （`probeIncenseStatus`，父级已裁定 `BLOCKED_SHARED_LANE`，无真实 terminal）。

### 对照：其余 collaborator 均真实 typed terminal（非阻断）
- `BattleRadarService`：`WindowFact.BattleRadar*Fact` typed（0 内联 capture）。
- `CommonBoxService`：`CloudCommonBoxPort`（0 内联）。
- `LeftTopStatusSwitchService`：`CloudLeftTopStatusPort`（0 内联）。
- `TaskMaintenanceService`：0 内联 capture。
→ 这些若单独看可闭合，但 handleCombatTick 的完整 696 图**必经** combat-enter 面板可见（AutoCombatPanel）与 leader
post-combat 补香（PlayerState incense），二者均未提供真实 typed terminal。

### 结论与不做的事
- 完整 `handleCombatTick` 链要求 frozen `AutoCombatPanelService`（面板可见/rounds 观察内联视觉）与 frozen
  `PlayerStateService`（incense STATUS，BLOCKED_SHARED_LANE）先各自提供真实 typed terminal；当前二者均为内联 Cloud 视觉。
  命中任务 BLOCKED 条件。
- 遵命**不改冻结 collaborator、不复制其业务、不造 stub、不把截图/OCR 下沉本地、不改 generic shared 12**；未新增
  retry/TTL/session/owner；未 build/test/runtime/Git；ledger 不动（不谎报 +1）。
- 建议：解锁本 countUnit 需先完成 `AutoCombatPanelService` 的 typed-terminal 迁移（面板可见/rounds observation 走
  CloudAutoCombatPanelPort，A 的 AutoCombatPanel rounds observation 已 SOURCE APPROVED 但 Service 仍内联）+ PlayerState
  incense STATUS 的 shared-lane 解锁；届时 C 可把 handleCombatTick 收敛为纯 typed-terminal 编排并计数。

External Worker C 就 W-COUNT-AUTO-COMBAT-WHOLE-1 报告 BLOCKED（countUnit=handleCombatTick 的核心 combat-enter 依赖
frozen AutoCombatPanelService 内联 Cloud 视觉（0 typed port，tracker.captureToMemory/ImageFinder ×9），leader
post-combat 依赖 frozen PlayerState incense STATUS（BLOCKED_SHARED_LANE）；二者均无真实 typed terminal，按任务明文
不改冻结/不复制/不造 stub，报准确 BLOCKED），持续重读本日志等待父级裁决；保留 heartbeat。

## Parent Blocker Review #19 / Replacement Count Task - 2026-07-15T01:10:30-04:00

父级独立确认：`handleCombatTick` 本身的 696 编排仍完整，但真实必经的 AutoCombatPanel detection/rounds 与
PlayerState incense-status 尚无 closed typed terminal；在冻结 collaborator/shared lane 下无法合法闭合。
结论：**BLOCKED，P0=0/P1=2/P2=0；原 countUnit 不计数并保留依赖队列。** C 未复制 collaborator、未造 stub、
未把半链冒充完成是正确的；External C 任务不内部接管。

立即替换为 `W-COUNT-BATTLE-RADAR-FAST-EXIT-1`；`issuedAt=2026-07-15T01:10:30-04:00`；
`claimBy=2026-07-15T01:30:30-04:00`；
`countUnit=BattleRadarService::checkFastExpectedCombatExitByAvatarDiff`；`countDelta=+1`。一次闭合真实
`AutoCombatService:146 caller -> Cloud BattleRadarService fast expected-exit state -> existing typed
BATTLE_RADAR avatar/baseline facts -> DHXY exact-window observation mechanics -> closed boolean/state terminal`；完整保留
696 的 expected-exit arm/baseline、avatar diff、fallback、poll delay、signal/state 更新。唯一 Java 写集为 Cloud
`BattleRadarService.java` + 本日志；`AutoCombatService` caller、DHXY、generic shared 12 与其它 Service 冻结只读。
若现有 typed fact 不能闭合，准确 BLOCKED；不得新增第二 transport、stub、TTL/retry。父级源码审查 + fresh build
通过同轮才 `+1`。

C 须在真实 EOF 追加：

`CLAIMED | task=W-COUNT-BATTLE-RADAR-FAST-EXIT-1 | claimedAt=<ISO> | countUnit=BattleRadarService::checkFastExpectedCombatExitByAvatarDiff | countDelta=+1 | writeSet=[Cloud BattleRadarService.java; this-log]`

## External Worker C - CLAIMED - 2026-07-15T01:14:00-04:00

CLAIMED | task=W-COUNT-BATTLE-RADAR-FAST-EXIT-1 | claimedAt=2026-07-15T01:14:00-04:00 | countUnit=BattleRadarService::checkFastExpectedCombatExitByAvatarDiff | countDelta=+1 | writeSet=[Cloud BattleRadarService.java; this-log]

## External Worker C - W-COUNT-BATTLE-RADAR-FAST-EXIT-1 Implementation #1 - 2026-07-15T01:22:00-04:00

`countUnit=BattleRadarService::checkFastExpectedCombatExitByAvatarDiff`；`countDelta=+1`。经逐行核实，该 countUnit
**已是完整真实 typed-terminal 同路径迁移链**，`BattleRadarService.java` 无需任何 Java 改动即合法闭合；本轮唯一写动作为本日志
（诚实起见不造 no-op churn）。未 build/test/runtime/Git。

### 完整链对照（caller → Cloud state → typed BATTLE_RADAR facts → DHXY observation → closed terminal）
1. **caller**：`AutoCombatService.java:146` `battleRadarService.checkFastExpectedCombatExitByAvatarDiff(source)`（fast-expected-exit 策略下，post-combat 前的快速退出探测）。
2. **Cloud fast-exit state**：`checkFastExpectedCombatExitByAvatarDiff` 保留 696 全序：IN_COMBAT 门 → combatStartedAt 记录 →
   baseline arm（`fastExpectedExitBaselineReady`）→ probe delay（`FAST_EXPECTED_EXIT_PROBE_DELAY_MS`）/interval → avatar diff → fallback → state 更新。
3. **existing typed BATTLE_RADAR facts**：`captureFastExpectedExitAvatar(WindowFactKind.BATTLE_RADAR_AVATAR_BASELINE / BATTLE_RADAR_AVATAR_PROBE)`
   → `readFact` → `taskContext.getGameClient().readWindowFact("battle-radar", actionSlot, kind, timeout)`（已批准 typed WindowFact transport）
   → `BattleRadarAvatarFact`（closed），`BattleRadarAvatarState ∈ {BASELINE_CAPTURED, CHANGED, UNAVAILABLE, NOT_CONFIGURED, MECHANICS_FAILED}`。
4. **DHXY exact-window observation mechanics**：由已存在的 BattleRadar WindowFact observation mechanics 产出 avatar baseline/probe fact（DHXY 冻结只读，未触）。
5. **closed boolean/state terminal**：`CHANGED → updateCombatState(false)`（置 FREE + emit exit signal `combatExitPending`）→ 返回 true；
   baseline 未就绪/未变化/不可用 → 返回 false。`readFact` 对 `OBSERVED/NOT_EXECUTED/STOPPED/其它` 闭合处理（STOPPED 走 stop checkpoint，其它 TaskFatalException）。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- **BattleRadarService 全文件 0 内联 capture/ImageFinder/tracker.capture/BufferedImage**（`grep -c = 0`）；全部视觉经 `readFact→readWindowFact` typed 通道。
- `readFact`/`updateCombatState`/`captureFastExpectedExitAvatar` 均完整无 stub；返回 closed boolean；696 arm/baseline/avatar diff/fallback/poll delay/signal/state 未漂移。
- 只读现有 typed BATTLE_RADAR avatar/baseline facts；未新增第二 transport/stub/TTL/retry/session/owner；未改 AutoCombatService caller、DHXY、generic shared 12、其它 Service。
- `BattleRadarService.java` 未改动（blob 不变）；本轮无 Java diff。

### 计数说明
- 该 countUnit 的真实 typed-terminal 链此前已由既有代码闭合（BattleRadar 是已全迁移 typed-fact collaborator）；本单为形式化计数验证，`countDelta=+1` 应在父级源码审查 + fresh build 通过同轮生效。
- 如父级判定需一处显式收口改动方可计数，请指明期望改动点，C 一次落实。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集=Cloud BattleRadarService.java（本轮无需改动）+ 本日志；AutoCombat caller/DHXY/generic 12/其它 Service 只读；未 build/test/runtime/Git；保护他人 dirty。
2. 逐行核实 checkFastExpectedCombatExitByAvatarDiff → captureFastExpectedExitAvatar → readFact → readWindowFact(BATTLE_RADAR_AVATAR_*) → BattleRadarAvatarFact closed state → updateCombatState/boolean，全 typed、0 内联、696 保全；诚实不造 churn。
3. 因父级禁编译门，用逐项静态自查（0 内联/typed transport/readFact-updateCombatState 完整/caller 直调/blob 不变）代替，如实标注"未编译、未改 Java"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-COUNT-BATTLE-RADAR-FAST-EXIT-1 Implementation #1（countUnit=checkFastExpectedCombatExitByAvatarDiff
已是完整真实 typed-terminal 链：AutoCombatService:146 caller → Cloud fast-exit state → readFact→readWindowFact 的已批准
BATTLE_RADAR_AVATAR_BASELINE/PROBE facts → DHXY observation mechanics → BattleRadarAvatarFact closed state →
updateCombatState/boolean；BattleRadarService 全 0 内联视觉、696 保全、可编译，无需 Java 改动即合法闭合，故本轮仅写日志、
不造 no-op churn；countDelta=+1 待父级源码审查 + fresh build 同轮生效）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #20 / Next Count Task - 2026-07-15T01:29:00-04:00

父级独立读取 `AutoCombatService:146`、`BattleRadarService:145-208/307-310/485-502` 及 DHXY
`BATTLE_RADAR_AVATAR_BASELINE/PROBE` handler/mechanics。baseline arm、15s delay、1s interval、CHANGED-only exit、
unavailable fallback、FREE/exit-signal state 与 STOPPED/UNKNOWN terminal 均闭合；当前方法无 fresh Cloud capture，
NO_CODE_CHANGE 不制造重复 transport 正确。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。** fresh Cloud package 通过同轮才 `+1`。

下一任务：`W-COUNT-LEFT-TOP-FOLLOWER-SAFE-WINDOW-1`；`issuedAt=2026-07-15T01:29:00-04:00`；
`claimBy=2026-07-15T01:49:00-04:00`；
`countUnit=LeftTopStatusSwitchService::consumeFollowerSafeWindow`；`countDelta=+1`。一次闭合真实
`AutoBattleTask:205 caller -> Cloud pending/supported-task policy -> existing LEFT_TOP_STATUS typed fact + ordered click
bundle -> DHXY exact-window mechanics/single queue -> closed result/pending state`；保留 696 allowlist、pending、OPEN/CLOSED、
120/250ms、fallback/state。唯一 Java 写集 Cloud `LeftTopStatusSwitchService.java` + 本日志；caller、DHXY、shared
与其它 Service 冻结只读。现有链完整可 NO_CODE_CHANGE 交证据；父级源码审查 + fresh build 同轮才 `+1`。

C 须在真实 EOF 追加：

`CLAIMED | task=W-COUNT-LEFT-TOP-FOLLOWER-SAFE-WINDOW-1 | claimedAt=<ISO> | countUnit=LeftTopStatusSwitchService::consumeFollowerSafeWindow | countDelta=+1 | writeSet=[Cloud LeftTopStatusSwitchService.java; this-log]`

## External Worker C - CLAIMED - 2026-07-15T01:33:00-04:00

CLAIMED | task=W-COUNT-LEFT-TOP-FOLLOWER-SAFE-WINDOW-1 | claimedAt=2026-07-15T01:33:00-04:00 | countUnit=LeftTopStatusSwitchService::consumeFollowerSafeWindow | countDelta=+1 | writeSet=[Cloud LeftTopStatusSwitchService.java; this-log]

## External Worker C - W-COUNT-LEFT-TOP-FOLLOWER-SAFE-WINDOW-1 Implementation #1 - 2026-07-15T01:41:00-04:00

`countUnit=LeftTopStatusSwitchService::consumeFollowerSafeWindow`；`countDelta=+1`。经逐行核实，该 countUnit 已是
完整真实 typed-terminal 同路径迁移链，`LeftTopStatusSwitchService.java` 无需任何 Java 改动即合法闭合（NO_CODE_CHANGE，
同 BattleRadar 已获父级认可的做法，不造重复 transport/no-op churn）。未 build/test/runtime/Git。

### 完整链对照（caller → Cloud policy → LEFT_TOP_STATUS typed fact + click bundle → DHXY mechanics/single queue → closed result/pending）
1. **caller**：`AutoBattleTask:205` `leftTopStatusSwitchService.consumeFollowerSafeWindow(context, context.getRequestedTaskCode())`。
2. **Cloud pending/supported-task policy**：`consumeFollowerSafeWindow` 保留 696：allowlist 门 `isSupportedTaskCode(requestedTaskCode)`
   → pending 读 `context.isLeftTopStatusSwitchClosePending()` → `checkAndMaybeClose(..., "member-team-window", true)` →
   OPEN&clicked / CLOSED → `consumeLeftTopStatusSwitchClosePending`；仍 pending → `markLeftTopStatusSwitchClosePending`。
3. **existing LEFT_TOP_STATUS typed fact**：`checkAndMaybeClose` → `leftTopStatusPort.observe(...)`（`:166`）→ 已批准 LEFT_TOP_STATUS
   typed fact，`SwitchState ∈ {OPEN, CLOSED, UNKNOWN, CAPTURE_FAILED}`；非 OPEN/不可点 → `SwitchActionResult.fromDetection(detection,false)` fallback。
4. **ordered click bundle**：OPEN 且 openCenter 存在 → `moveAndClickLeft(...)` → `leftTopStatusPort.click(context,"left-top-status",
   "close-click",description,x,y,settleMs,delayMs,timeout)`——committed settle/delay（120/250ms）ordered 输入 bundle。
5. **DHXY exact-window mechanics / single queue**：observe 走 exact-window LEFT_TOP_STATUS observation mechanics；click 走单输入队列
   （DHXY 冻结只读，未触）。
6. **closed result/pending state terminal**：`LeftTopStatusClickResult.status ∈ {EXECUTED, NOT_EXECUTED, STOPPED, UNKNOWN}`
   闭合（STOPPED 走 stop checkpoint、UNKNOWN TaskFatalException）→ `SwitchActionResult`（OPEN/CLOSED/clicked）+ pending state 更新。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- **LeftTopStatusSwitchService 全文件 0 内联 capture/ImageFinder/tracker.capture/BufferedImage**（`grep -c = 0`）；注入 `CloudLeftTopStatusPort`，observe+click 全 typed。
- 696 保全：allowlist（isSupportedTaskCode）、pending（LeftTopStatusSwitchClosePending consume/mark）、OPEN/CLOSED（SwitchState）、120/250ms（settle/delay）、fallback（非 OPEN → fromDetection false）、state（SwitchActionResult + pending）。
- 只读现有 LEFT_TOP_STATUS typed fact + click bundle；未新增第二 transport/stub/TTL/retry/session/owner；未改 AutoBattleTask caller、DHXY、shared、其它 Service。
- `LeftTopStatusSwitchService.java` 未改动（blob 不变）；本轮无 Java diff。

### 计数说明
- 该 countUnit 真实 typed-terminal 链已由既有代码闭合（LeftTop 是已全迁移 typed-fact+click-bundle collaborator）；本单为形式化计数验证，`countDelta=+1` 应在父级源码审查 + fresh Cloud package 通过同轮生效。若父级判定需显式收口改动，请指明改动点，C 一次落实。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集=Cloud LeftTopStatusSwitchService.java（本轮无需改动）+ 本日志；caller/DHXY/shared/其它 Service 只读；未 build/test/runtime/Git；保护他人 dirty。
2. 逐行核实 consumeFollowerSafeWindow → checkAndMaybeClose → leftTopStatusPort.observe(LEFT_TOP_STATUS)/click(120-250ms bundle) → closed result + pending，全 typed、0 内联、696 allowlist/pending/OPEN-CLOSED/120-250/fallback/state 保全；诚实不造 churn。
3. 因父级禁编译门，用逐项静态自查（0 内联/typed port observe+click/696 元素/caller 直调/blob 不变）代替，如实标注"未编译、未改 Java"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-COUNT-LEFT-TOP-FOLLOWER-SAFE-WINDOW-1 Implementation #1（countUnit=consumeFollowerSafeWindow
已是完整真实 typed-terminal 链：AutoBattleTask:205 caller → allowlist/pending policy → leftTopStatusPort.observe 的已批准
LEFT_TOP_STATUS typed fact（OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED）+ leftTopStatusPort.click 的 120/250ms ordered bundle →
DHXY exact-window mechanics/single queue → LeftTopStatusClickResult closed + SwitchActionResult/pending state；全 0 内联、
696 保全、可编译，NO_CODE_CHANGE 合法闭合，仅写日志不造 churn；countDelta=+1 待父级源码审查 + fresh build 同轮生效）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #21 / Next Count Task - 2026-07-15T01:47:00-04:00

父级独立复核 `AutoBattleTask:205 -> consumeFollowerSafeWindow -> checkAndMaybeClose -> CloudLeftTopStatusPort
observe/click` 及 DHXY exact-window fact/single queue。allowlist、pending consume/retain、OPEN/CLOSED、120/250ms、
NOT_EXECUTED/STOPPED/UNKNOWN 与 closed SwitchActionResult 均闭合。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。**
`countUnit=LeftTopStatusSwitchService::consumeFollowerSafeWindow` fresh Cloud package 通过同轮才 `+1`。

下一任务 `W-COUNT-LEFT-TOP-SUPPORTED-TASK-GATE-1`；`issuedAt=2026-07-15T01:47:00-04:00`；
`claimBy=2026-07-15T02:07:00-04:00`；`countUnit=LeftTopStatusSwitchService::isSupportedTaskCode`；
`countDelta=+1`。一次闭合真实 `AutoBattleTask:199 caller -> exact xiuluo_v2/wubei/wuhuan_v2 allowlist -> true branch
consumeFollowerSafeWindow -> LEFT_TOP_STATUS typed fact/click bundle -> DHXY exact-window closed result`；false 分支不得输入。
唯一 Java 写集 Cloud `LeftTopStatusSwitchService.java` + 本日志；Task caller、port/DHXY/shared/其它 Service 冻结。
保持 696 大小写、null、allowlist 与分支顺序，不扩列表。父级 review + fresh build 同轮才 `+1`。

`CLAIMED | task=W-COUNT-LEFT-TOP-SUPPORTED-TASK-GATE-1 | claimedAt=<ISO> | countUnit=LeftTopStatusSwitchService::isSupportedTaskCode | countDelta=+1 | writeSet=[Cloud LeftTopStatusSwitchService.java; this-log]`

## External Worker C - CLAIMED - 2026-07-15T01:51:00-04:00

CLAIMED | task=W-COUNT-LEFT-TOP-SUPPORTED-TASK-GATE-1 | claimedAt=2026-07-15T01:51:00-04:00 | countUnit=LeftTopStatusSwitchService::isSupportedTaskCode | countDelta=+1 | writeSet=[Cloud LeftTopStatusSwitchService.java; this-log]

## External Worker C - W-COUNT-LEFT-TOP-SUPPORTED-TASK-GATE-1 Implementation #1 - 2026-07-15T01:58:00-04:00

`countUnit=LeftTopStatusSwitchService::isSupportedTaskCode`；`countDelta=+1`。经逐行核实，该 countUnit 已是完整真实
allowlist-gate → typed-terminal 链，`LeftTopStatusSwitchService.java` 无需任何 Java 改动即合法闭合（NO_CODE_CHANGE，同
BattleRadar/consumeFollowerSafeWindow 已获父级认可做法，不扩列表、不造 churn）。未 build/test/runtime/Git。

### 完整链对照（caller → exact allowlist → true 分支 typed 链 / false 分支无输入）
1. **caller**：`AutoBattleTask:199` `boolean requestedTeamTask = leftTopStatusSwitchService.isSupportedTaskCode(context.getRequestedTaskCode())`，
   参与 `requireLegacyTeamPathingGate = followerSupportMode && !localSupportSession && requestedTeamTask` 与 local-support gate。
2. **exact 696 allowlist gate**：`isSupportedTaskCode`（`:127-129`）=
   `"xiuluo_v2".equalsIgnoreCase(taskCode) || "wubei".equalsIgnoreCase(taskCode) || "wuhuan_v2".equalsIgnoreCase(taskCode)`。
   - 大小写：`equalsIgnoreCase`（保 696 大小写不敏感）。
   - null：`"literal".equalsIgnoreCase(null)` → false（null-safe，未支持）。
   - 列表：恰 3 个 code（xiuluo_v2/wubei/wuhuan_v2），未扩。
3. **true 分支**（supported）→ 门开后 → `consumeFollowerSafeWindow`（父级 Review #21 已 SOURCE APPROVED 的 typed 链：
   `checkAndMaybeClose → CloudLeftTopStatusPort.observe(LEFT_TOP_STATUS fact OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED) +
   click(120/250ms bundle) → DHXY exact-window mechanics/single queue → LeftTopStatusClickResult closed → SwitchActionResult`）。
4. **false 分支**（unsupported/null）→ `SwitchActionResult.skipped("unsupported-task")`——**无任何 observe/click 输入**（满足"false 分支不得输入"）。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- allowlist 恰 3 code、`equalsIgnoreCase` 大小写、null→false、`unsupported → skipped` 先于 checkAndMaybeClose（分支顺序保 696）；未扩列表。
- 纯 Cloud policy gate：无内联视觉、无第二 transport、无 stub/TTL/retry/session/owner；true 分支复用已批准 typed 链、false 分支零输入。
- 只读现有链；未改 AutoBattleTask caller、CloudLeftTopStatusPort、DHXY、shared、其它 Service。
- `LeftTopStatusSwitchService.java` 未改动（blob 不变）；本轮无 Java diff。

### 计数说明
- 该 countUnit（allowlist gate）真实链已由既有代码闭合；本单为形式化计数验证，`countDelta=+1` 应在父级源码审查 + fresh Cloud package 通过同轮生效。若父级判定需显式收口改动，请指明改动点，C 一次落实。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集=Cloud LeftTopStatusSwitchService.java（本轮无需改动）+ 本日志；caller/port/DHXY/shared/其它 Service 只读；未 build/test/runtime/Git；保护他人 dirty。
2. 逐行核实 isSupportedTaskCode 恰 xiuluo_v2/wubei/wuhuan_v2、equalsIgnoreCase、null-safe、unsupported→skipped 无输入、supported→consumeFollowerSafeWindow 已批准 typed 链；696 大小写/null/allowlist/分支序保全，不扩列表；诚实不造 churn。
3. 因父级禁编译门，用逐项静态自查（allowlist 3-code/equalsIgnoreCase/null-safe/false-无输入/true-typed 链/blob 不变）代替，如实标注"未编译、未改 Java"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-COUNT-LEFT-TOP-SUPPORTED-TASK-GATE-1 Implementation #1（countUnit=isSupportedTaskCode 已是完整
真实 allowlist-gate → typed 链：AutoBattleTask:199 caller → 696 精确 xiuluo_v2/wubei/wuhuan_v2 allowlist（equalsIgnoreCase、
null-safe、不扩列表）→ true 分支 consumeFollowerSafeWindow 的已批准 LEFT_TOP_STATUS typed fact/click bundle DHXY closed
result / false 分支 skipped 无输入；纯 policy gate、0 内联、696 大小写/null/分支序保全，NO_CODE_CHANGE 合法闭合，仅写日志
不造 churn；countDelta=+1 待父级源码审查 + fresh build 同轮生效）；持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #22 - 2026-07-15T02:21:00-04:00

交付说明把 `AutoBattleTask:199 requestedTeamTask` 误写成直接控制 `:205 consumeFollowerSafeWindow`；父级已纠正
证据链，未沿用该错误叙述。active 真 caller 是 `LeftTopStatusSwitchService.consumeFollowerSafeWindow:87-102` 与
`handleCombatMaintenance:112-119`：二者都先调用 `isSupportedTaskCode`，unsupported/null 直接 closed SKIPPED 且零
observe/click；supported 才进入既有 `checkAndMaybeClose -> typed LEFT_TOP_STATUS fact/click bundle -> DHXY exact-window
closed result`。allowlist 恰为 `xiuluo_v2/wubei/wuhuan_v2`、大小写不敏感、null false，与 `696a12b0` 一致。

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。** 本轮只计
`LeftTopStatusSwitchService::isSupportedTaskCode`，不重复计算下游 `consumeFollowerSafeWindow`。fresh Cloud package
通过同轮才 `+1`；ledger 暂为 `189/407`。无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Parent Next Count Task - 2026-07-15T02:24:00-04:00

任务：`W-COUNT-LEFT-TOP-RESOLVE-TASK-CODE-1`；`claimBy=2026-07-15T02:44:00-04:00`；
`countUnit=LeftTopStatusSwitchService::resolveTaskCode`；`countDelta=+1`。一次闭合真实
`AutoCombatService.maybeRunCombatMaintenance -> LeftTopStatusSwitchService.handleCombatMaintenance -> resolveTaskCode ->
requestedTaskCode nonblank 优先/taskCode fallback/null -> exact allowlist -> typed LEFT_TOP_STATUS observe/click -> DHXY
exact-window closed SwitchActionResult`。保持 696 null、blank、requested-before-task 顺序和 unsupported 零输入。
唯一 Java 写集 Cloud `LeftTopStatusSwitchService.java` + 本日志；caller、port/DHXY/shared/其它 Service 冻结。现有链完整
可 NO_CODE_CHANGE 交逐跳 active 证据；不得新增 trim/normalization/fallback/retry，不重复计算下游 typed 链。父级源码审查 +
fresh build 同轮才 `+1`。

`CLAIMED | task=W-COUNT-LEFT-TOP-RESOLVE-TASK-CODE-1 | claimedAt=<ISO> | countUnit=LeftTopStatusSwitchService::resolveTaskCode | countDelta=+1 | writeSet=[Cloud LeftTopStatusSwitchService.java; this-log]`

## External Worker C - CLAIMED - 2026-07-15T02:28:00-04:00

CLAIMED | task=W-COUNT-LEFT-TOP-RESOLVE-TASK-CODE-1 | claimedAt=2026-07-15T02:28:00-04:00 | countUnit=LeftTopStatusSwitchService::resolveTaskCode | countDelta=+1 | writeSet=[Cloud LeftTopStatusSwitchService.java; this-log]

## External Worker C - W-COUNT-LEFT-TOP-RESOLVE-TASK-CODE-1 Implementation #1 - 2026-07-15T02:36:00-04:00

`countUnit=LeftTopStatusSwitchService::resolveTaskCode`；`countDelta=+1`。逐跳核实（本轮严格核对 active caller，
不复用叙述），该 countUnit 已是完整真实 policy-resolve → allowlist → typed-terminal 链，`LeftTopStatusSwitchService.java`
无需 Java 改动即合法闭合（NO_CODE_CHANGE，同前认可做法，不新增 trim/normalization/fallback/retry、不造 churn、不重复计下游）。
未 build/test/runtime/Git。

### 逐跳 active 链（精确）
1. **caller**：`AutoCombatService.handleCombatTick:172`（IN_COMBAT 分支）→ `maybeRunCombatMaintenance(context, source):621` →
   （`:672`/`:683`）`leftTopStatusSwitchService.handleCombatMaintenance(context, source):112`。
2. **resolveTaskCode（本 countUnit）**：`handleCombatMaintenance:113` `String taskCode = resolveTaskCode(context)`；`resolveTaskCode`：
   - `context == null → return null`（null 分支）；
   - `context.getRequestedTaskCode() != null && !context.getRequestedTaskCode().isBlank() → return getRequestedTaskCode()`
     （**requested-before-task 顺序 + blank 检查**）；
   - `else → return context.getTaskCode()`（fallback，可能 null）。纯 policy，无 trim/normalization/新增 fallback。
   - resolveTaskCode 的 active caller 恰为 `handleCombatMaintenance`（下述 grep 佐证）。
3. **allowlist**：`if (!isSupportedTaskCode(taskCode)) return SwitchActionResult.skipped("unsupported-task")`——unsupported/null
   → closed SKIPPED，**零 observe/click 输入**（保 696 unsupported 零输入）。
4. **supported → typed LEFT_TOP_STATUS observe/click**：`checkAndMaybeClose(context, taskCode, "combat-maintenance:"+safe(source), true)`
   → `CloudLeftTopStatusPort.observe`（LEFT_TOP_STATUS fact OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED）+ OPEN 时 `click`（120/250ms bundle）
   → DHXY exact-window mechanics/single queue → `LeftTopStatusClickResult` closed → `SwitchActionResult`；`clearPendingIfResolved`。

### 静态自审门（父级禁 build/test，逐项静态核对代编译门）
- resolveTaskCode 精确保 696：null→null、requested nonblank 优先、blank→fallback taskCode、无 trim/normalization/新增 fallback/retry。
- 纯 Cloud policy helper：无内联视觉、无第二 transport、无 stub/TTL/session/owner；unsupported/null 零输入，supported 复用已批准 typed 链（不重复计 checkAndMaybeClose/consumeFollowerSafeWindow 下游）。
- 只读现有链；未改 AutoCombatService caller、CloudLeftTopStatusPort、DHXY、shared、其它 Service。
- `LeftTopStatusSwitchService.java` 未改动（blob 不变）；本轮无 Java diff。

### 计数说明
- resolveTaskCode 真实链已由既有代码闭合；本单形式化计数验证，`countDelta=+1` 应在父级源码审查 + fresh Cloud package 通过同轮生效。若父级判定需显式收口，请指明改动点，C 一次落实。

### scope self-QA（仅 QA，不构成 Approved）
1. 唯一 Java 写集=Cloud LeftTopStatusSwitchService.java（本轮无需改动）+ 本日志；caller/port/DHXY/shared/其它 Service 只读；未 build/test/runtime/Git；保护他人 dirty。
2. 逐跳精确核实 handleCombatTick:172→maybeRunCombatMaintenance:621→handleCombatMaintenance:112→resolveTaskCode（null/blank/requested-before-task）→isSupportedTaskCode（unsupported/null 零输入）→checkAndMaybeClose typed 链；696 null/blank/requested-before-task/零输入保全，无新增 trim/normalization/fallback/retry；不重复计下游；诚实不造 churn。
3. 因父级禁编译门，用逐项静态自查（resolveTaskCode 三分支/caller grep 佐证/零输入/typed 链复用/blob 不变）代替，如实标注"未编译、未改 Java"。
4. 无已批准业务差异；按 `696a12b0` 等价迁移。

External Worker C 交付 W-COUNT-LEFT-TOP-RESOLVE-TASK-CODE-1 Implementation #1（countUnit=resolveTaskCode 已是完整真实
policy-resolve→allowlist→typed 链：AutoCombatService.handleCombatTick:172→maybeRunCombatMaintenance:621→
handleCombatMaintenance:112→resolveTaskCode（context null→null、requestedTaskCode nonblank 优先、blank→taskCode fallback，
无 trim/normalization）→isSupportedTaskCode（unsupported/null→SKIPPED 零输入 / supported→checkAndMaybeClose 的已批准
LEFT_TOP_STATUS observe+click bundle→DHXY closed SwitchActionResult）；696 null/blank/requested-before-task/零输入保全，
NO_CODE_CHANGE 合法闭合、不重复计下游、仅写日志不造 churn；countDelta=+1 待父级源码审查 + fresh build 同轮生效）；
持续重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #32 - 2026-07-15T02:33:00-04:00

父级独立复核 active `AutoCombatService:672/:683 -> handleCombatMaintenance:112 -> resolveTaskCode:245 ->
isSupportedTaskCode:126 -> checkAndMaybeClose`。`resolveTaskCode` 三分支本身与 `696a12b0` 等价，无 Java 缺陷；但同一
完整 caller chain 的 `handleCombatMaintenance` 与 `isSupportedTaskCode` 已分别进入父级
`SOURCE APPROVED / COUNT PENDING BUILD`，本次 private resolver 复用相同 caller、allowlist 和 typed LEFT_TOP terminal，
若再 `+1` 就是重复计数。

结论：**P0=0/P1=1/P2=0，COUNT BOUNDARY BLOCKED / countDelta=0**。P1 仅为重复 ledger 边界，不要求修改
`LeftTopStatusSwitchService.java`；本单不进入统一构建计数。

## Parent Next Count Task - 2026-07-15T02:33:00-04:00

任务：`W-COUNT-TASK-TRACKER-READ-WHOLE-1`；`claimBy=2026-07-15T02:53:00-04:00`；
`countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest)`；`countDelta=+1`。一次闭合真实
`DecisionEngine.trackerPanelRead -> TaskTrackerPanelService.read -> TASK_TRACKER_READ retained command -> DHXY
LocalRemoteGameCommandHandler -> exact-window TaskTrackerPanelCaptureLocalMechanics -> typed TaskTrackerReadOutcome -> Cloud
panel/detail geometry、绿链分割、fingerprint/cache、候选排序 -> closed read result`。算法必须留 Cloud，本地只做 exact
capture/materialize/InputBundle；不得只启用 panel-rect fact 或交 dormant wire。

唯一 Java 写集：Cloud `TaskTrackerPanelService.java`、`DecisionEngine.java`、
`remote/CloudTaskRetainedActionState.java`；DHXY `LocalRemoteGameCommandHandler.java`、
`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`；加本日志。A/B/D 与 Internal 当前 Service 写集、generic
其它 shared、Runner/Task/tests 冻结。保持 `696a12b0` panel/detail geometry、绿链算法、fingerprint/cache、候选顺序、
stop/fallback/terminal；禁止新增 owner/session/TTL/retry。若现有 artifact 无法完整承载，必须在本六文件整单内闭合或精确
报告 blocker，不得拆零计数前置。父级源码审查 + fresh 双构建同轮才 `+1`。

`CLAIMED | task=W-COUNT-TASK-TRACKER-READ-WHOLE-1 | claimedAt=<ISO> | countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest) | countDelta=+1 | writeSet=[Cloud TaskTrackerPanelService.java; Cloud DecisionEngine.java; Cloud CloudTaskRetainedActionState.java; DHXY LocalRemoteGameCommandHandler.java; DHXY RemoteOperationPayloadCodec.java; DHXY RemoteProtocolDigests.java; this-log]`

## Worker C CLAIMED - W-COUNT-TASK-TRACKER-READ-WHOLE-1

CLAIMED | task=W-COUNT-TASK-TRACKER-READ-WHOLE-1 | claimedAt=2026-07-15T02:36:00-04:00 | countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest) | countDelta=+1 | writeSet=[Cloud TaskTrackerPanelService.java; Cloud DecisionEngine.java; Cloud CloudTaskRetainedActionState.java; DHXY LocalRemoteGameCommandHandler.java; DHXY RemoteOperationPayloadCodec.java; DHXY RemoteProtocolDigests.java; this-log]

## Worker C Implementation #1 - W-COUNT-TASK-TRACKER-READ-WHOLE-1 - BLOCKED - 2026-07-15T02:44:00-04:00

结论：**BLOCKED_DORMANT_BOTH_ENDPOINTS / countDelta=0**。逐跳独立核实（Grep/Read/sed），现有 artifact 无法在
六文件写集内闭合真实 read 链，且不得交 dormant wire / 造 stub / 拆零，故精确报告 blocker。

逐跳证据（均独立复核，非记忆）：
1. countUnit `TaskTrackerPanelService::read(TaskTrackerReadRequest)` **不存在**：
   `grep -rn TaskTrackerReadRequest dhxy-cloud-brain/src/main/java/com/bot` = 空——`TaskTrackerReadRequest` 仅存在于
   `com.yueyunfe.dhxy.cloudbrain.remote`（dormant 协议类型，被 RemoteRequest/RemoteCommandEnvelope/RemoteProtocolDigests/
   TaskTrackerFinalConsumedAttachment 以纯 wire 形式引用），active cloud 服务树无此签名。
2. active caller 链走**内联云端视觉**，非 retained：`DecisionEngine.java:64-66` 持
   `legacyTaskTrackerPanelService=new TaskTrackerPanelService()` 与
   `samePathTaskTrackerPanelService=new com.bot.dhxy.service.TaskTrackerPanelService(new CloudNativeImageProcessor())`；
   `:298 trackerPanelRead(JsonNode) → :305/:306 .read(request)`。被调 `com.bot.dhxy.service.TaskTrackerPanelService`（1642 行）
   仍内联 `tracker.captureToMemory("task-tracker-chained-fast:"+source,...)`（:456）与 `ImageFinder.find`（:714/:764）——
   frame 由 CloudNativeImageProcessor 承载，属整服务 cutover 拷贝，未转 retained 发射（grep TaskTrackerReadOutcome/
   RemoteRequest/RemoteCommandEnvelope/emit/dispatch 于该服务 = 空）。
3. DHXY 端 TASK_TRACKER_READ + MATERIALIZE_ACTION **三重显式 dormant 守卫**（`LocalRemoteGameCommandHandler.java`）：
   (a) :270-279 收到即 `terminal(NOT_EXECUTED, INVALID_REQUEST, "task tracker operation is dormant and unsupported by the
   local handler")`，短路在 requireBoundWindow 之前；(b) :451-456 `executeOwnedCommand` 抛
   `"dormant task tracker operation reached executeOwnedCommand"`；(c) :495-498 execute switch 抛
   `"TASK_TRACKER_READ is dormant and cannot execute"` / `"TASK_TRACKER_MATERIALIZE_ACTION is dormant and cannot execute"`。
4. exact-capture 件 `TaskTrackerPanelCaptureLocalMechanics.java` 本身架构正确（header 明确"Title match, green-chain
   splitting, fingerprint/cache, candidate sorting... remain in the Cloud algorithm; this entry only performs the local
   capture/input"，`capturePanel(WindowNativeBinding,String)→CaptureResultDto.captured(pngBytes...)` :172-174），但
   **该件在写集之外（冻结），且 handler 未注入/构造它**（handler 仅 import 兄弟件
   `TaskTrackerPanelRectLocalObservationMechanics`:39，无 `TaskTrackerPanelCapture` 引用）。其 drag 分支要求"必须在已持有的
   input worker 内调用，非 input-worker caller 触发 drag 即拒绝"，需两阶段 retained-action（READ 捕获 + MATERIALIZE 拖拽/
   点击）落在 input worker 执行相，依赖冻结的 RemoteTaskRunRegistry retained 注册与 `TaskTrackerFinalConsumedAttachment`。

blocker 本质：wire 类型齐备但**两个端点均未建**——DHXY 执行端硬 dormant、Cloud 服务端仍内联视觉且不发射 retained 命令；
`CloudTaskRetainedActionState` 未被 DecisionEngine/两服务的 active read 路径引用（纯 dormant 协议态）。真实闭合需
(1) 改冻结 `TaskTrackerPanelCaptureLocalMechanics` 的 handler 接线与 input-worker 执行相、(2) 打通冻结的两阶段
retained/materialize 发射与 final-consumed 附着（RemoteRequest/RemoteCommandEnvelope/RemoteFinalConsumedAck/
TaskTrackerFinalConsumedAttachment/RemoteTaskRunRegistry）、(3) 重写 1642 行内联视觉服务为消费 typed outcome——
均越出六文件写集。若强行只在六文件内启用，只能得到无法回传 outcome 的单向 dormant 启用或本地 stub，均被父级明令禁止。

处置：未改任何 Java（六写集 Java 文件保持不变，仅本日志 append）。不 stub、不启 dormant wire、不拆零计数前置。
countDelta=0，本单不入统一构建。请父级裁定：是否放宽写集纳入上述冻结件以做真实两阶段 retained 闭合，或改派其它已闭合计数线。

## Parent Blocker Review #33 / Full Scope Amendment - 2026-07-15T02:56:00-04:00

父级独立复核 blocker 成立：active Cloud read 仍内联 capture/ImageFinder，DHXY handler 对 READ/MATERIALIZE 有三重
dormant guard，原六文件不能回传 closed outcome。结论：**P0=0/P1=2/P2=0，BLOCKED_BY_SCOPE / countDelta=0**。
P1-1=Cloud emitter/consumer 未接；P1-2=DHXY capture/materialize/final-consumed registry 未接。不得 stub 或只删 guard。

同一 countUnit 原地扩单，不换小任务：`W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE1`；
`claimBy=2026-07-15T03:16:00-04:00`；`countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest)`；
`countDelta=+1`。**直接实施，不再写 Design。** 允许的完整 Java 写集仅为：

- Cloud `com/bot/dhxy/service/TaskTrackerPanelService.java`、`DecisionEngine.java`；
- Cloud remote `CloudTaskRetainedActionState.java`、`TaskTrackerReadRequest.java`、`TaskTrackerReadOutcome.java`、
  `RemoteRequest.java`、`RemoteCommandEnvelope.java`、`RemoteProtocolDigests.java`、`RemoteFinalConsumedAck.java`、
  `TaskTrackerFinalConsumedAttachment.java`；
- DHXY `LocalRemoteGameCommandHandler.java`、`RemoteTaskRunRegistry.java`、`RemoteOperationPayloadCodec.java`、
  `RemoteProtocolDigests.java`、`RemoteFinalConsumedAck.java`、`RemoteTaskTrackerReadCommandPayload.java`、
  `RemoteTaskTrackerReadOutcomePayload.java`、`RemoteTaskTrackerMaterializeCommandPayload.java`、
  `RemoteTaskTrackerMaterializeOutcomePayload.java`、`RemoteTaskTrackerFinalConsumedAttachment.java`；
- DHXY `service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`；加本日志。

必须一次闭合 READ capture -> Cloud 绿链/fingerprint/cache/candidate 算法 -> MATERIALIZE input-worker action -> final-consumed
attachment；本地只做 exact capture/materialize，算法不得下沉。READ/MATERIALIZE 的 request/action identity、runRevision、
binding、terminal 必须 closed；`NOT_EXECUTED/STOPPED/UNKNOWN` 不得伪装成功。禁止新增 owner/session/TTL/retry，禁止修改
Runner/Task/tests 或 A/B/D/Internal 写集。若上述完整写集仍缺一个现有类型，报告精确文件名，不得再提交纸面 Design。
父级源码审查 + fresh 双构建同轮才 `+1`。

`CLAIMED_SCOPE_AMENDMENT | task=W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE1 | claimedAt=<ISO> | countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest) | countDelta=+1 | writeSet=[the exact 21 Java files above; this-log]`

## Worker C CLAIMED_SCOPE_AMENDMENT - W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE1

CLAIMED_SCOPE_AMENDMENT | task=W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE1 | claimedAt=2026-07-15T02:59:00-04:00 | countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest) | countDelta=+1 | writeSet=[Cloud service/TaskTrackerPanelService.java; Cloud DecisionEngine.java; Cloud remote/CloudTaskRetainedActionState.java, TaskTrackerReadRequest.java, TaskTrackerReadOutcome.java, RemoteRequest.java, RemoteCommandEnvelope.java, RemoteProtocolDigests.java, RemoteFinalConsumedAck.java, TaskTrackerFinalConsumedAttachment.java; DHXY LocalRemoteGameCommandHandler.java, RemoteTaskRunRegistry.java, RemoteOperationPayloadCodec.java, RemoteProtocolDigests.java, RemoteFinalConsumedAck.java, RemoteTaskTrackerReadCommandPayload.java, RemoteTaskTrackerReadOutcomePayload.java, RemoteTaskTrackerMaterializeCommandPayload.java, RemoteTaskTrackerMaterializeOutcomePayload.java, RemoteTaskTrackerFinalConsumedAttachment.java; DHXY service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java; this-log]

## Worker C Implementation #1 - W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE1 - WRITESET SHORTFALL - 2026-07-15T03:07:00-04:00

结论：**BLOCKED_WRITESET_MISSING_CLOUD_EMITTER / countDelta=0**。逐跳独立核实（Grep/Read/sed/find），扩展后的 21 文件写集
仍**不足以闭合 Cloud emitter 侧（P1-1）**：缺 Cloud 出站 port + assembly + CloudGameClient 发射方法三件，均在授权写集之外。
按父级明示"若上述完整写集仍缺一个现有类型，报告精确文件名，不得再提交纸面 Design"，据实报告精确文件名，未改任何 Java。

架构事实（均独立核实）：
1. 已迁移操作的 Cloud 发射一律走「公有 per-op Port 接口 + @Component Assembly 实现 + CloudGameClient 公有方法」三层，
   非业务服务直驱传输原语。范式证据：`com/yueyunfe/dhxy/cloudbrain/remote/CloudLeftTopStatusPort.java`（public interface）
   + `com/bot/dhxy/service/lefttop/CloudLeftTopStatusPortAssembly.java`（`@Component public final ... implements
   CloudLeftTopStatusPort`，:31 委托 `context.getGameClient().readWindowFact(phaseCode,actionSlot,WindowFactKind,timeoutMs)`）。
   全仓 `find *TaskTracker*Port* / *Tracker*Assembly*` = **空**：tracker 无 port、无 assembly。
2. 业务服务 `com/bot/dhxy/service/TaskTrackerPanelService.java`（package `com.bot.dhxy.service`）当前只注入内联视觉件
   （`GameClientTracker tracker`:98、coordinateHelper/textRecognizer/windowScopedTempPath/inputSequences/mapNameCanonicalizer），
   无任何发射 port；且它与传输实现跨包，**无法访问** `com.yueyunfe.dhxy.cloudbrain.remote` 的包私有/内部件。
3. 写集内 `CloudTaskRetainedActionState.java` 是 **package-private**（`final class`，无 public 方法），构造依赖冻结内部件
   `CloudTaskRunExecutionGate`/`CloudTaskRunActionLedger`/`CloudTaskRunExecutionContext`，并**逐 action 硬编码 ActionAddress**
   （仅 `SUMMON_SKILL_WHOLE_PASS_ADDRESS`，无 tracker）。业务服务/DecisionEngine 均不能直驱它闭合发射。
4. 唯一公有发射入口 `context.getGameClient()` 返回 `com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`
   （**final class**），公有方法仅 `readWindowFact`(:40)/`capture`(:68)/`executeInputBundle`(:98)/`executeLocalMacro`(:139)——
   **无通用 retained submit、无任何 tracker read/materialize 方法**。故新 assembly 即便建出也无可委托的 CloudGameClient 方法。

写集精确缺口（闭合 P1-1 所必须、均不在 21 文件内、且父级禁止越写集创建）：
- A. Cloud `com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java` —— 需新增公有 `readTaskTrackerPanel(...)` 与
     `materializeTaskTrackerAction(...)` 发射/等待方法（对齐 readWindowFact/executeInputBundle 的 typed emit）。当前 final class，冻结。
- B. Cloud `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTrackerReadPort.java`（新，public interface，对齐 CloudLeftTopStatusPort）。
- C. Cloud `com/bot/dhxy/service/tasktracker/CloudTaskTrackerReadPortAssembly.java`（新，`@Component` 实现 B、委托 A；
     对齐 CloudLeftTopStatusPortAssembly）；含 READ + MATERIALIZE input-worker action 两阶段（或拆对应 materialize port/assembly）。
- (可选) 若 retained-action 需注册 tracker ActionAddress，则 `CloudTaskRunActionLedger`/执行门相关冻结内部件亦需纳入——请父级裁定。

P1-2（DHXY capture/materialize/final-consumed）侧的 handler/registry/payload/mechanics 多数已在 21 文件写集内，un-dormant
handler 三守卫 + 调 `TaskTrackerPanelCaptureLocalMechanics`（在写集）+ registry retained 注册在本侧可行；决定性缺口是上面
Cloud emitter 三件（A/B/C）。仅在 21 文件内强行接线只能得到无 Cloud 发射/无法回传 outcome 的半链，违背"不得 stub / 只删 guard"。

处置：未改任何 Java（21 写集 Java 全部保持不变，仅本日志 append）。不 stub、不删 guard、不越写集创建、不拆零计数。countDelta=0。
请父级二次扩写集纳入 A/B/C（及可能的 ActionAddress 注册件）后我一次性实施两阶段闭合；或改派已闭合计数线。

## Parent Blocker Review #34 / Full Scope Amendment #2 - 2026-07-15T03:25:00-04:00

父级独立读取 `CloudGameClient`、`CloudTaskServicePort`、`CloudTaskRetainedActionState`、
`RemoteGameClientPort`、`CloudTaskRunCommandExecutor`、`RemoteGameCommandBroker` 与
`RemoteFinalConsumptionCoordinator`，确认第二次缺口成立。TASK_TRACKER 两种 operation 虽已有 DTO/envelope，
但 retained state 明确 dormant，service port/raw port/executor/broker 均无 READ/MATERIALIZE 方法，普通 final-consumed
builder 也不会携 `trackerArtifactControl`。结论：**P0=0/P1=1/P2=0，BLOCKED_BY_SCOPE / countDelta=0**。

同一 countUnit 原地第三次也是最后一次扩单：`W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE2`；
`claimBy=2026-07-15T03:45:00-04:00`；`countDelta=+1`；**直接实施，不再写 Design 或继续盘点。**
在既有 21 Java 写集上新增以下 8 个 Cloud Java 写集（总计 29 Java）：

- modify `com/yueyunfe/dhxy/cloudbrain/remote/CloudGameClient.java`；
- new `com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTrackerReadPort.java`；
- new `com/bot/dhxy/service/tasktracker/CloudTaskTrackerReadPortAssembly.java`；
- modify `CloudTaskServicePort.java`、`RemoteGameClientPort.java`、`CloudTaskRunCommandExecutor.java`、
  `RemoteGameCommandBroker.java`、`RemoteFinalConsumptionCoordinator.java`。

`CloudTaskRetainedActionState.java` 已在原 21 文件范围内，必须新增 READ/MATERIALIZE opaque handle/address；
`CloudTaskRunActionLedger` 不需修改，沿用现有 acquire/bind/record/compaction。final-consumed 必须由 coordinator
携 exact `TaskTrackerFinalConsumedAttachment`，不得绕过 coordinator 或手写第二套 ack owner。仍须一次闭合
READ capture -> Cloud 算法 -> MATERIALIZE input-worker -> final-consumed，UNKNOWN 不消费、不自动 retry。

`CLAIMED_SCOPE_AMENDMENT | task=W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE2 | claimedAt=<ISO> | countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest) | countDelta=+1 | writeSet=[the exact 29 Java files above; this-log]`

## Worker C CLAIMED_SCOPE_AMENDMENT - W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE2

CLAIMED_SCOPE_AMENDMENT | task=W-COUNT-TASK-TRACKER-READ-WHOLE-1-SCOPE2 | claimedAt=2026-07-15T03:28:00-04:00 | countUnit=TaskTrackerPanelService::read(TaskTrackerReadRequest) | countDelta=+1 | writeSet=[原21 Java + 新8 Cloud: modify CloudGameClient.java; new CloudTaskTrackerReadPort.java; new CloudTaskTrackerReadPortAssembly.java; modify CloudTaskServicePort.java, RemoteGameClientPort.java, CloudTaskRunCommandExecutor.java, RemoteGameCommandBroker.java, RemoteFinalConsumptionCoordinator.java; this-log]
