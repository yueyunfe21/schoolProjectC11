# Cloud NpcClick Public Closure - Worker BT

## CLAIMED - 2026-07-14T06:30:16.6309584-04:00

- task: `W-NPCCLICK-DIRECT-PUBLIC-CLOSURE-IMP1`
- role: Internal Worker BT; implementation only, not reviewer
- uniqueWriteSet:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-npc-click-public-closure-worker-bt.md`
- baseline: committed DHXY `0114604e`, `NpcClickService.java` blob `cc858482e31ee4a352f59895054452ca28b61d6a`
- cloudSourcePreEditSha256: `9250d2902b80edddeaac172c14464995432929304e34cc04eec4b83b6ddf6153`
- scope: close committed public APIs `clickNpcSmart`, `clickNpcSmartWithOutcome`, `tryDirectCombatTargetClick`, and `confirmPendingSmartClick` through existing typed facts/closed local primitives and existing `InputBundle`/`RemoteGameClientPort` only.
- constraints: no wire/schema/caller/host changes; no per-Service owner/permit/session/ledger/TTL/retry; no runtime; no tests; no Git mutation; protect all concurrent dirty/untracked work.
- businessBaseline: `无已批准业务差异；按 0114604e 基线等价迁移。`

## BLOCKED - 2026-07-14T06:34:56.0997114-04:00

结论：指定单文件写集无法在保持 `0114604e` 调用、判断、顺序、delay、fallback 与 terminal
语义的前提下闭合四个 public API。现有 shared port 只足够承载整窗 capture、geometry fact、
`Alt+4`/`Alt+A` 和普通原子 move+click bundle；committed 流程必需的 NPC 本地闭环与 Cloud
同进程 decision facade 尚未存在。按任务边界禁止自行扩 wire/schema/caller/host，因此未修改 Cloud Java。

### Public 方法矩阵

| Public API | `0114604e` 基线 | 当前可机械替换 | 阻塞 |
|---|---|---|---|
| `clickNpcSmart(NpcClickRequest)` | L217，严格返回 `clickNpcSmartWithOutcome(request).isVerified()` | 无额外机械动作 | 依赖下行完整实现 |
| `clickNpcSmartWithOutcome(NpcClickRequest)` | L232；仅真 FIFO `END/CLOUD_NO_ACTION` 置 `normalFifoConsumedUnverified=true` | base capture 可用 `CloudGameClient.capture`；Alt+4 与普通候选 `[move, sleep(150), click(150), sleep(1500)]` 可用现有 `InputBundle` | 缺 start/poll/outcome typed decision facade、expected-dialog local verify、Ctrl local macro、CR255 story-event/fast-click primitive |
| `tryDirectCombatTargetClick(NpcClickRequest)` | L1204；先硬门 normal FIFO END，再 authorize，才发 `[Alt+A, sleep(350)]`，随后新 smart session + 4 次 350ms combat verify | Alt+A bundle 已由现有 action type 覆盖 | 缺 typed direct-combat authorization facade、combat-state local verify；且后半依赖完整 smart session |
| `confirmPendingSmartClick(...)` | L1342，CR169 后只写 debug log 的 no-op | 可原样复制 | 单独复制不能构成四 API closure，且不能用 no-op 掩盖前三项阻塞 |

### 现有机械替换与精确证据

- Cloud `CloudGameClient` 现有 public primitive 只有 `readWindowFact`、`capture`、
  `executeInputBundle`、`executeLocalMacro`（当前源码 L40/L68/L98/L139）。
- `LocalMacroKind` 当前只有 `BAG_RETURN_ITEM`（L5）；DHXY mirror
  `RemoteLocalMacroKind` 也只有 `BAG_RETURN_ITEM`（L8）。不存在 NPC Ctrl probe、NPC verify、
  story blocker/fast-click 的 closed macro variant、command、result 或 client method。
- Cloud `WindowFactKind` 与 DHXY `RemoteWindowFactKind` 均止于
  `TASK_TRACKER_PANEL_RECT`，没有 dialog verification、combat state 或 story-event fact。
- Cloud `InputSequences` 明确不提供 `submitExclusiveAndWait(Supplier)`（L27-L28）。因此 committed
  L733 的单 exclusive Ctrl hold -> move -> capture/template -> click -> release 序列不能被普通 bundle
  或多次远程调用等价替代。
- committed 本地闭环调用证据：expected-dialog verify L181/L187/L200；story fast click L306；
  combat watcher L160；Ctrl exclusive L733、hold L774、capture/template/click L818-L878。
- Cloud 源树不存在 `NpcClickSmartCloudDecisionService`。已有 `DecisionEngine` constructor 为
  package-private（L66），`npcClickSmart(...)` 与 `npcClickDirectCombatAuthorize(...)` 为 private
  （L2612/L2667），且当前 task/service context 未暴露 typed NPC decision collaborator。指定文件无法
  在不改 host/caller/其它 Cloud 文件的条件下调用既有 FIFO/authorize owner。
- Cloud `BattleRadarService` 仅是 private state-core scaffold，无
  `checkAndSyncCombatState()` public API；Cloud 源树也没有 `DialogService` 实现。
- `NpcVerifyMode`、`NpcLocalVerifyResult`、`CtrlMenuProbeTerminal`、`CtrlScanRectKind`、
  `NpcCtrlMenuTagSet` 虽已存在，但两仓 codec/operation/client/handler 均没有引用；它们不是可调用 primitive。

### 为什么不能单文件绕过

- 把端口缺失映射成 `false`/`REQUIRED_FAILURE` 会把 transport/能力缺失变成新业务 terminal，改变
  CR267 的真 END 授权门。
- 在 Cloud 临时复制 Dialog/Ctrl OCR/template 或把 Ctrl 段拆成多个 input/capture 请求，会移动本地
  mechanics、破坏 committed exclusive 原子边界。
- 在本类新增 nested port/adapter 或直接拼 JSON 调 `DecisionEngine` 会新增类边界并绕过当前 typed owner；
  同时仍解决不了 local verify/Ctrl/story/combat primitive。

### 最小下一刀

先由 shared wire/host owner 在现有 shared port 中闭合并两仓镜像以下已存在基线能力，然后原样重派本任务：

1. 一个 Cloud 同进程 typed NPC decision facade，暴露 committed `startSession`、`pollNext`、
   `reportOutcome`、`authorizeDirectCombat`，复用当前 `DecisionEngine` 的唯一 FIFO/memory owner，不新建队列。
2. 现有 `LOCAL_MACRO`/typed fact 边界中的 closed NPC mechanics 调用，至少覆盖：
   expected-dialog/combat local verify、一次完整 Ctrl-menu exclusive probe、CR255 fresh story-event 检查与
   已知小剧情快点结果；DHXY handler 复用当前本地实现，不增加 TTL/retry/session/ledger。

这是编译和等价迁移所需的最小跨文件前置，不是本 Worker 可在唯一 Java 写集内扩展的内容。

### SHA / changed files

- committed baseline blob: `cc858482e31ee4a352f59895054452ca28b61d6a`
- Cloud `NpcClickService.java` pre/post SHA-256:
  `9250d2902b80edddeaac172c14464995432929304e34cc04eec4b83b6ddf6153`（字节未改）
- changed by Worker BT:
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-npc-click-public-closure-worker-bt.md`
- not changed by Worker BT:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`

`无已批准业务差异；按 0114604e 基线等价迁移。`

## Compile Gate - 2026-07-14T06:36:29.7461068-04:00

- command: `mvn -q compile`
- repository: `D:\mavenProject\dhxy-cloud-brain`
- result: exit `0`
- tests: 未运行、未新增（按任务要求）
- interpretation: 当前共享 Cloud 工作树可编译；该结果不解除上述缺失端口，也不代表四个 public API 已收口。
- Cloud `NpcClickService.java` post-compile SHA-256:
  `9250d2902b80edddeaac172c14464995432929304e34cc04eec4b83b6ddf6153`（与 CLAIMED 时一致）
- final worker state: `BLOCKED`，等待父级/共享 wire owner 提供最小前置；Worker BT 自审不构成 `Approved`。

## Parent Blocker Review #1 - 2026-07-14T06:37:22-04:00

**BLOCKED，P0=0 / P1=2 / P2=0；Worker 的零 Java 停止判断正确。**

- `P1-1`：committed public flow 依赖同一个现有 NPC FIFO/authorize owner 的 typed start/poll/outcome/direct-combat
  facade；当前 `CloudGameClient` 只有 fact/capture/input-bundle/Bag macro，指定单文件无法合法调用该 owner。
- `P1-2`：expected-dialog/combat/story 与 Ctrl-menu hold/capture/template/click/release 是本地连续观察机械段；当前
  `LocalMacroKind` 只有 `BAG_RETURN_ITEM`，拆成多次远程调用会破坏 baseline exclusive 边界与 terminal 语义。

精确返修条件：先由共享端口独立切片闭合“复用现有 FIFO owner 的 typed decision facade”与“closed NPC local
mechanics macro/result”，不得新建第二队列、session/ledger/TTL/retry；前置通过后恢复原 BT 或新 worker 仅改
`NpcClickService.java` 收口 public API。当前 Java SHA 未变化，Cloud compile exit 0；不计成果。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
