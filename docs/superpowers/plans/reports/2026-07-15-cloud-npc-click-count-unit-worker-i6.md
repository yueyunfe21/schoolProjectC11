# Internal Count Worker I6 - NpcClickService::clickNpcSmart

## CLAIMED

- task: `W-COUNT-NPC-CLICK-SMART-WHOLE-1`
- claimedAt: `2026-07-15T00:56:00-04:00`
- countUnit: `NpcClickService::clickNpcSmart`
- countDelta: `+1`（仅申报；父级源码审查和统一 fresh build 通过后才可实际记账）
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- 唯一常规业务 Java 写集: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
- 条件写集: 仅当仓内已经存在 NPC 专属 typed adapter/handler 且只有一个精确缺口时，才允许修改该专属 adapter/handler。
- 冻结: `NavigationService`、`AutoCombatService`、`AutoCombatPanelService`、`DialogService`、shared remote 12、host/config，以及全部其他 Worker dirty/untracked。

## Baseline / Workspace Gate

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 的 NPC Click/五倍/修罗基线、whole-Service 计划、迁移矩阵和两仓 `git status`。
- 两仓均有大量既存 dirty/untracked；本 Worker 未回滚、覆盖、清理或提交任何既存改动。
- active Cloud `NpcClickService.java` 与
  `migration-baseline/696a12b0/.../NpcClickService.java` 的 `git diff --no-index --stat` 为空，说明当前类仍是
  `696a12b0` byte-exact 原类，不存在需要恢复的中间业务逻辑。
- `docs/业务逻辑.md` 的 NPC Click 权威顺序为
  `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`；本地只负责 exact-window
  事实、输入和 verifier outcome，Cloud 保留策略、顺序、去重、fallback、记忆与指标。

## Real Caller

- active Cloud 真实 caller 已存在：
  `NavigationService.java:772` 调用
  `npcClickService.clickNpcSmart(ZHANG_WEN_NPC.toClickRequest(me))`，随后严格保留基线语义：false 只告警，
  再执行 stop checkpoint 和 route-dialog fallback（`:773-787`）。
- active Cloud 当前只有这一处 `.clickNpcSmart(...)` caller；本 countUnit 因而可以用该真实 reachable path
  验收，不需要伪造 task caller。

## 696a12b0 Method / Branch / Timing / State Map

- public entry `clickNpcSmart`（active `NpcClickService.java:599-634`）仍逐字保留：
  1. 建立 dialog verifier；
  2. 第一次 `runNpcClickPipeline`；
  3. stop 返回 false；
  4. `COMBAT_TARGET` 禁止通用 `Alt+C` retry；
  5. 普通目标执行 `Alt+C + 700ms`，失败/stop 返回 false；
  6. 第二次完整 pipeline。
- `runNpcClickPipeline`（`:778` 起）仍保留 request/name 校验、cached player location、ROI recommendation、
  既有 dialog gate、一次 name-layer preparation，以及 learned/tooltip/yellow/player-anchor/Ctrl 的原顺序和
  success 早退。
- 本地输入时序仍在原类中逐字存在：move `150ms` -> left-click hold `150ms` -> caller first wait；retry 时
  click 后 `1000ms`；name-layer `Alt+4` 后 `400ms`；Ctrl probe 在单一 exclusive callback 内 direct input，
  避免 queue-in-queue。
- evidence/state 仍由 `recordSmartClickEvidence`、`pendingSmartClickEvidence`、proof token 与
  `OcrRoiMemoryService` 的原调用图维护；本 Worker未增加 TTL、retry、owner、session、ledger 或状态门。

## Typed Mechanics Inventory

DHXY 已存在并保持冻结的专属 mechanics：

- `NpcClickPreparedPointLocalMacroMechanics`
- `NpcClickTaskTooltipLocalMacroMechanics`
- `NpcClickYellowTargetLocalObservationMechanics`
- `NpcClickPlayerAnchorLocalObservationMechanics`
- `NpcClickCtrlProbeLocalMacroMechanics`

Cloud 也已存在四组 typed command/result/port：prepared point、task tooltip、yellow target、player anchor。
但是它们当前全部只是接口/DTO：

- `CloudNpcPreparedPointPort` 全仓引用仅其声明文件自身；JavaDoc 明确写着 concrete transport、
  `LocalMacroKind`、sealed command/outcome、handler 尚未集成。
- `CloudNpcTaskTooltipPort` 全仓引用仅其声明文件自身，状态相同。
- `CloudNpcYellowTargetPort` 全仓引用仅其声明文件自身，JavaDoc 明确写着 pure interface、非 bean、shared
  integration 尚未落地。
- `CloudNpcPlayerAnchorPort` 全仓引用仅其声明文件自身，JavaDoc 明确写着无 bean、无 transport call、
  implementation 是后续 integration。
- Cloud `LocalMacroKind.java:4-15` 与 DHXY `RemoteLocalMacroKind.java:7-18` 均没有任何 NPC kind。
- `LocalRemoteGameCommandHandler`、`RemoteOperationPayloadCodec`、`RemoteProtocolDigests` 对上述 NPC payload
  没有 dispatch/codec/digest 引用；本地 mechanics 因此不可从 Cloud 到达。

## BLOCKED

- status: `BLOCKED_SHARED_LANE`
- P0: `0`
- P1: `1`
- P2: `0`

### P1 - closed typed transport 不存在，当前允许写集无法闭合 terminal

**精确证据**

1. 四个 Cloud NPC port 的引用计数均为 `1`，且唯一命中就是各自接口声明；没有 concrete assembly/bean。
2. Cloud/DHXY 两侧 closed macro allowlist 均无 NPC kind。
3. DHXY 五个 NPC mechanics 没有被 `LocalRemoteGameCommandHandler` 调用。
4. active `NpcClickService` 仍直接依赖 `InputProvider`、`InputSequences`、`GameClientTracker`、本地 OCR/image、
   `DialogService` 等桌面对象；只改该文件无法产生真实 DHXY terminal。

**影响**

- 若只在 `NpcClickService.java` 注入现有 port，Spring 没有实现 bean，host 无法构造 Service。
- 若在 Service 内手工 new/直连 transport，会违反 Spring ownership、稳定 scope/request identity 和本任务冻结
  host/shared remote 的边界。
- 若对缺失 terminal 返回 false/空结果，则是 stub，会把完整基线链静默降级并伪造 `countDelta=+1`。

**精确返修条件**

必须由拥有 shared remote/host 写集的单一后续计数任务原子提供以下前置，而不是拆成零计数 filler：

1. Cloud/DHXY closed NPC macro allowlist 与 command/outcome/envelope/codec/digest parity；
2. 生产可注入的四个现有 Cloud NPC port concrete assembly；
3. DHXY handler 对 prepared-point、tooltip、yellow、player-anchor、Ctrl-probe 的 exact-binding dispatch，复用现有
   mechanics 并返回 closed terminal；
4. verifier/name-layer/stop/geometry 等本地事实与输入的 typed closure；
5. 然后由同一 `countUnit=NpcClickService::clickNpcSmart` 任务在 active Service 内替换原调用点，逐项保持上述
   方法、分支、delay、fallback、evidence/state，不新增业务差异。

在 shared/host 明确冻结且不存在“已有专属 adapter/handler 的一个精确缺口”的当前任务范围内，I6 不得合法修改
任何 Java。故本次不申报完成、不增加 ledger；等待父级裁决并重新发包含完整前置写集的 `countDelta=+1` 整链单。

## Delivery

- Java changed: `0`
- report changed: 本文件
- Maven/test/runtime: 按任务禁令均未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；按 696a12b0 等价迁移`

## Parent Blocker Review #1 / Replacement Count Task - 2026-07-15T00:58:00-04:00

父级独立确认四个 NPC port 均只有 interface/DTO、两侧 allowlist/codec/digest/handler 无 kind/dispatch，现有
DHXY mechanics 从 Cloud 不可达。只改 `NpcClickService.java` 必然产生 unsatisfied bean 或 stub。

结论：**P0=0 / P1=1 / P2=0，BLOCKED_SHARED_LANE；NpcClick countDelta=0。** 该单保留，等待 generic
shared lane 释放后由同一完整计数任务闭合；不拆零计数 filler。

替换任务：`W-COUNT-CLIENT-IDENTITY-WHOLE-1`，`countUnit=ClientIdentityService::scanAndSyncIdentity`，
`countDelta=+1`。一次闭合真实 `PlayerStateService::syncMyIdentity` caller -> Cloud identity parse/update policy ->
existing typed `WindowFactKind.BINDING` exact DHXY binding/title fact -> closed observation -> `PlayerCharacter` state。
只改 Cloud `ClientIdentityService.java` 与必要 identity-specific pure adapter；复用现有 BINDING fact，不新增 shared
kind/handler/owner/session/TTL/retry。`PlayerStateService` caller、generic shared、host/config、DHXY Java 与其它 Service
全部冻结。父级源码审查和统一 fresh build 通过同轮才 `+1`。
