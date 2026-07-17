# Cloud Npc Click Smart Count Unit - Worker I4

## CLAIMED

- claimedAt: `2026-07-15T01:31:59.4148209-04:00`
- worker: `Internal implementation Worker I4`
- role: `implementation-only（不是 reviewer）`
- task: `W-COUNT-NPC-CLICK-SMART-WHOLE-1`
- countUnit: `NpcClickService::clickNpcSmart`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- initialDisposition: `INSPECTING_WHOLE_CHAIN`
- countGate: `只有父级源码审查与 fresh Maven 同轮通过后才实际计数`

## Initial Source Fingerprints

| File | SHA-256 at claim |
|---|---|
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java` | `CCE8F0203AC90A0D39F7CFF99DDA8D9A616656A55467ED4AE3AA053AD0923441` |
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java` | `F3035A485F05D9C2511E0068C7BE6750FD0B8030C1D1C6F6F9A5ED125210CACA` |

## Baseline And Current Source Identity

- active Cloud `NpcClickService.java` 与
  `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java` 的 SHA-256 均为
  `CCE8F0203AC90A0D39F7CFF99DDA8D9A616656A55467ED4AE3AA053AD0923441`。
- 两文件均为 `3374` 行，逐行 `Compare-Object` 结果 `DIFF_RECORDS=0`。当前业务类仍是 696 byte-exact
  全本地实现，不存在可只补一个 adapter 的中间迁移态。
- active Cloud 当前仅有一个 `.clickNpcSmart(...)` 真实 caller：`NavigationService.java:772`。
- 本 Worker 未修改 active/baseline Java，也未触碰其他 worker 的 dirty/untracked。

## Real Caller

`NavigationService.java:744-787` 的真实路径保持：

1. approach result 成功且 route option 尚未可见；
2. `:772` 调用 `npcClickService.clickNpcSmart(ZHANG_WEN_NPC.toClickRequest(me))`；
3. false 只在 `:773-775` 告警，不被改写为新的业务真值；
4. `:776` 继续 stop checkpoint；
5. `:778-787` 继续 route-dialog fallback 与原结果映射。

caller 已可达但冻结；本任务没有修改其 phase、false fallback 或 state。

## 696 Public/Private Map

| 696 方法/职责 | active Cloud 位置 | 必须保持的业务 |
|---|---|---|
| public `clickNpcSmart` | `NpcClickService.java:599-633` | dialog verifier -> 第一轮 pipeline -> stop false -> combat target 禁止 generic Alt+C retry -> 普通目标 `Alt+C + 700ms` -> 第二轮完整 pipeline |
| private `runNpcClickPipeline` | `:778-934` | request/name gate、cached player location、ROI recommendation、dialog cleanup/gate、一次 name-layer preparation、strategy 早退、异常重抛、latency result |
| learned-memory | `tryLearnedMemoryStrategy`, `:958-982` | 只在 known target coordinate；记录 evidence；失败点进入 Ctrl origin |
| task-tooltip | `tryNormalTooltipStrategy`, `:984-996` | 五倍在 dialog gate 前优先；其它任务按后续原位执行；结果记录 evidence |
| yellow-name | `tryYellowTargetStrategy`, `:1052-1082` | ordered regions；verified 早退；attempt/candidates 进入 Ctrl origin；仅允许的 miss 扩区 |
| player-anchor formula | `tryPlayerAnchorFormulaStrategy`, `:998-1050` | known-coordinate gate、预测点/window gate、prepared click/verify、失败后 immediate small Ctrl probe |
| Ctrl menu | `tryCtrlMenuStrategy`, `:1084-1128` + `clickNpcByCtrlMenuScan`, `:303-446` | 最后 fallback；non-combat 15px reference filter；origin/profile/offset 顺序；连续 Ctrl-hold capture/OCR/click/verify；finally release |
| prepared point click | `executeMoveClickAndVerify`, `:176-215` | atomic move -> `150ms` -> left-click hold `150ms` -> first wait；baseline retry click 后 `1000ms`；每次 verifier |
| dialog/combat verifier | `:241-301` | expected template/option or four-pass combat confirmation；不把 click-produced 丢失伪装成未尝试 |
| evidence/state | `recordSmartClickEvidence`、`pendingSmartClickEvidence`、confirmation methods | strategy/source/status/point/region/player snapshot、proof token、pending confirmation 与 ROI memory 更新 |

`runNpcClickPipeline` 的实际顺序保持为：允许时 early learned-memory -> 一次 `Alt+4 + 400ms` name-layer ->
五倍 tooltip-first -> dialog gate -> 未提前执行时 learned-memory -> 非五倍 tooltip -> yellow-name ->
player-anchor formula -> Ctrl menu。`TENTATIVE` light scan 继续跳过 yellow/formula/Ctrl。任何完整迁移必须保留该
顺序、delay、fallback、evidence 和 boolean terminal，不能只迁一个 helper/DTO/adapter。

## Existing Typed Mechanics Inventory

DHXY 已存在五个 Npc 专属 mechanics：

| Mechanics | 当前职责 | 可达状态 |
|---|---|---|
| `NpcClickPreparedPointLocalMacroMechanics` | prepared screen point click + local verify + retry terminal | Cloud 不可达 |
| `NpcClickTaskTooltipLocalMacroMechanics` | exact-window tooltip capture/match/click/verify | Cloud 不可达 |
| `NpcClickYellowTargetLocalObservationMechanics` | exact-window same-frame yellow raw/mask/candidate evidence | Cloud 不可达 |
| `NpcClickPlayerAnchorLocalObservationMechanics` | exact-window player-anchor observation/evidence | Cloud 不可达 |
| `NpcClickCtrlProbeLocalMacroMechanics` | continuous Ctrl hold -> capture/change/OCR/click/verify -> finally release | Cloud 不可达 |

Ctrl mechanics `:188-208` 明确要求运行在唯一 `dhxy-input-action-worker`；其顺序保持 before capture -> hold Ctrl ->
`80ms` -> move -> `280ms` -> after capture -> `0.05` change check -> OCR/fuzzy -> move + `100ms` -> click ->
verify -> finally release Ctrl + `100ms`，并保留 `800/1000ms` click verification 与 `4x350ms` combat verify。
这段不能拆成跨网络普通 input bundles，否则 Ctrl 生命周期、capture 和 click/verify 不再连续。

Cloud 已存在四组 command/result/port：prepared point、task tooltip、yellow target、player anchor。但 scoped
引用检查确认它们都只有声明/DTO，没有 concrete implementation：

- `CloudNpcPlayerAnchorPort` JavaDoc 明确写 `no Spring bean / no transport call`，implementation 是 downstream
  integration；
- `CloudNpcYellowTargetPort` 明确写 `pure interface, not a registered bean`，等待 shared enum/permit/envelope/
  codec/digest/handler wiring；
- `CloudNpcTaskTooltipPort` 与 `CloudNpcPreparedPointPort` 明确写 concrete transport-bound implementation 和
  handler 是 deferred shared integration；
- Ctrl probe 甚至尚无 Cloud typed command/result/port，仅有 DHXY local mechanics 和 closed local intent/result。

## BLOCKED

- status: `BLOCKED_SHARED_LANE`
- P0: `0`
- P1: `1`
- P2: `0`
- Java changes: `0`
- count applied: `0`

### P1 - Frozen shared transport has no NPC variants, so no closed terminal is reachable

**源码证据**

1. Cloud `LocalMacroKind.java:4-15` 只有现有十种 macro，无任何 NPC kind。
2. Cloud `LocalMacroCommand.java:4-8` sealed permits 无任何 `Npc*MacroCommand`。
3. DHXY `RemoteLocalMacroKind.java:7-18` 同样无 NPC kind；
   `RemoteLocalMacroCommandPayload.java:4-14` 与 `RemoteLocalMacroResultPayload.java:4-14` sealed permits 也无 NPC
   payload。
4. DHXY `LocalRemoteGameCommandHandler.executeLocalMacro:1155-1215` 只 dispatch navigation、UI、dialog、
   player-state 与 bag；无 prepared-point、tooltip、yellow、player-anchor 或 Ctrl-probe mechanics 调用。
5. scoped search 在 DHXY handler/codec 中对上述五类 NPC mechanics/payload/kind 返回
   `NO_DHXY_HANDLER_OR_CODEC_BRANCH`；Cloud 搜索返回 `NO_CLOUD_NPC_PORT_IMPLEMENTATION`。
6. active `NpcClickService.java:101-115` 仍注入全本地 `InputProvider`、`GameClientTracker`、`TextRecognizer`、
   `GameStateUtil`、`CoordinateHelper`、`LocationVisionService`、`OcrRoiMemoryService`、
   `GameTextLineOcrService` 等。静态文件存在性检查确认其中多项在 Cloud tree 缺失。
7. Cloud `InputSequences.java:27-30` 明确不提供 `submitExclusiveAndWait(Supplier)`；而 baseline Ctrl fallback
   `NpcClickService.java:370-429` 必须在该 exclusive callback 内 direct capture/input/verify。因此普通
   `INPUT_BUNDLE` 不能替代缺失 Ctrl local macro。

**影响**

- 只在 `NpcClickService` 注入四个现有 port 会产生无实现 bean，无法构造真实 Service。
- 只做 Cloud adapter 也无法编码/发送 NPC macro，DHXY enum/sealed/codec/handler 不接受且不 dispatch。
- 将 Ctrl probe 拆成普通 bundle 会破坏唯一输入队列内连续 Ctrl-hold 与 exact-window capture/verify，产生跨窗和
  部分执行误判风险。
- 对缺失 mechanics 返回 `false`、空 evidence 或 fixed terminal 是 stub，会静默删除 696 fallback/state 并伪造
  `countDelta=+1`。
- 因此当前允许写集无法闭合 `NavigationService:772 -> clickNpcSmart -> exact DHXY mechanics -> closed
  boolean/evidence/confirmation terminal`。

**原子解锁条件**

必须由 shared-lane owner 先一次提供两仓对称的完整 Npc transport，不能拆零计数 filler：

1. Cloud/DHXY shared allowlist、sealed command/result、request/outcome/envelope、codec、digest 的五类 NPC parity；
2. 四个现有 Cloud port 的生产可注入 concrete assembly，保留 caller-supplied phase/action identity 与 closed
   `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` terminal；
3. 新增 Ctrl-probe typed command/result/port，并在 DHXY handler 的唯一 exclusive input worker 中一次调用现有
   `NpcClickCtrlProbeLocalMacroMechanics.probe`；
4. DHXY handler 对 prepared point、tooltip、yellow target、player anchor 的 exact-binding dispatch，复用现有
   mechanics，不复制业务策略；
5. shared integration 释放后，由同一个 `countUnit=NpcClickService::clickNpcSmart` 整链任务替换 active Service
   原调用点，逐段核对上述 696 public/private map、delay/fallback/evidence/confirmation，并由父级源码审查和 fresh
   Maven 同轮验收。

当前任务明确冻结 generic shared 12、handler 所在 shared lane、Navigation/Dialog/BattleRadar 和其它 Service；故不能
合法完成上述前置，也不能造 stub。

## Existing Parent Blocker Evidence

`2026-07-15-cloud-npc-click-count-unit-worker-i6.md` 已对同一 task/countUnit 记录
`BLOCKED_SHARED_LANE`。父级在其 `Parent Blocker Review #1` 独立确认：四个 NPC port 仅 interface/DTO，
两侧 allowlist/codec/digest/handler 无 kind/dispatch，DHXY mechanics 从 Cloud 不可达；结论
`P0=0 / P1=1 / P2=0`，NpcClick `countDelta=0`，等待 generic shared lane 释放后仍由同一完整计数任务闭合。

本 I4 独立 scoped inspection 看到的当前源码与该父级结论一致，没有出现可解除 blocker 的 shared integration。

## File Table

| File/group | This task |
|---|---|
| Cloud `NpcClickService.java` | inspected; baseline byte-exact; `NO_CODE_CHANGE` |
| Cloud four Npc typed port/command/result families | inspected read-only; contract-only |
| Cloud/DHXY generic shared 12 + handler | inspected read-only; frozen blocker |
| DHXY `service/npc/**` five mechanics | inspected read-only; existing but unreachable |
| Cloud `NavigationService.java` | frozen caller, inspected read-only |
| Other frozen Services/runner/task/host/config | untouched |
| 本固定报告 | only authored file |

## Scoped Verification

- active/baseline `NpcClickService.java` SHA-256 equal，`3374/3374` lines，`DIFF_RECORDS=0`。
- caller search: active Cloud 仅 `NavigationService.java:772` 一处 `.clickNpcSmart(...)`。
- port implementation search: `NO_CLOUD_NPC_PORT_IMPLEMENTATION`。
- DHXY handler/codec branch search: `NO_DHXY_HANDLER_OR_CODEC_BRANCH`。
- 未运行 build、Maven、test、runtime、application、server、Task、poller、UI、capture、input 或 Git mutation。
- 未回滚、覆盖、清理、提交或改动任何 shared writer 文件。

## Delivery And Count Gate

无已批准业务差异；按 `696a12b0` 等价迁移。

- delivery: `BLOCKED_SHARED_LANE / NO_JAVA_CHANGE`
- 本 Worker 不申报完成，不应用 `countDelta=+1`，当前实际 delta 为 `0`。
- shared lane 解锁后仍需一次闭合本完整 count unit；不得把 helper/DTO/单 adapter 算完成或拆零计数 follow-up。
- 只有父级源码审查与 fresh Maven 同轮通过后才可实际计数。
