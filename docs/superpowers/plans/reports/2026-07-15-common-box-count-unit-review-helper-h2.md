# CommonBox Count Unit Review Helper H2

> 角色：Internal Review Helper H2，仅作非绑定源码预检。本文不作最终裁决，不改 Java、CR、External 日志，
> 也不以本报告替代父级的完整源码审查与统一构建门。

## Evidence scope

- 业务合同：`docs/业务逻辑.md` 的“通用盒子逻辑”，尤其是角色独立开关、队长/队员检测与消费时机、
  30 秒 pending、盒子优先级和修罗/五倍适用范围。
- 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，Cloud 保留副本
  `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
  （Git blob `195c1dbfef052ddaf87ff40c6c85cba862be91f6`）。
- Active Cloud：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
  及 CommonBox 专属 port/result/assembly、现有 generic `COMMON_BOX` fact 与 `INPUT_BUNDLE`。
- DHXY terminal：`CommonBoxLocalObservationMechanics`、`RemoteCommonBoxFact`、
  `LocalRemoteGameCommandHandler` 的 `COMMON_BOX` / `EXECUTE_INPUT_BUNDLE` 分支。
- 计划与矩阵：whole-service 计划的 countable-task gate、迁移矩阵中的 CommonBox 五项业务职责，以及 I1 报告。
- 只读检查；未构建、未测试、未运行、未提交。

## Observations

### O1. Public/private 方法图总体保持，只有本地 mechanics 的私有实现被下沉

Baseline public 图为：

1. `detectLeaderBoxAfterReturnHome -> detectBox -> detectAndRecord`
2. `detectMemberBoxAfterCombatExit -> detectBox -> detectAndRecord`
3. `consumePendingBoxIfAllowed`
4. `hasPendingBoxForCurrentWindow`
5. `clearPendingForRole`

Active Cloud 保留上述五个 public API 和 `detectBox`、`detectAndRecord`、`roleFor`、`isRoleEnabled`、
`normalizeSupportedTask`、`pendingKey`、`taskRunKey`、`pruneExpiredPending`、`sameWindow`、
`PendingCommonBox` 全部职责。Baseline 私有 `cachedTemplate` 被移到 DHXY
`CommonBoxLocalObservationMechanics.cachedTemplate`，ROI/template/capture 不再由 Cloud 拥有；未看到 I1 再加
`recordMatched`、`consumeClick`、`actionSlot` 等一层转发 wrapper。

### O2. 30 秒 TTL 的值、起点和比较边界一致

- Active `PENDING_TTL_MS = 30_000L`。
- `MATCHED` 使用 DHXY mechanics 产生的 `matchedAtEpochMs`，写入
  `detectedAt=matchedAt`、`expiresAt=matchedAt+30_000`；没有以 Cloud 收包时间重新起算。
- `pruneExpiredPending` 与 consume stale 分支仍以 `expiresAt <= now` 清理，`hasPending` 仍要求
  `expiresAt > now`。
- 没有新增第二 TTL、延期、额外验证、自动 retry 或 cleanup cadence。

### O3. role/task/window/run/identity gate 的源码顺序与 baseline 对齐

`consumePendingBoxIfAllowed` 顺序仍是：stop checkpoint -> prune -> 支持任务 -> taskRun -> window -> role ->
role toggle/clear -> pending lookup -> expired/window/identity/taskRun stale -> click。支持任务仍只接受 trim/lower 后的
`xiuluo_v2` 与 `wubei`；role 仍只接受 `LEADER` / `MEMBER`。

Pending key 仍由 `windowId + hwnd + role + task + taskRun` 组成，identity epoch 仍作为 pending 字段在消费前比较。
Active 只是把 baseline 的 `WindowRuntimeContext` 读取替换为 Cloud `TaskExecutionContext` 投影；未把缺失
window/taskRun/role 当作新业务真值。

### O4. 两个 role toggle 的默认值和互不影响逻辑保留

- `BotProperties.leaderCommonBoxEnabled=true`、`memberCommonBoxEnabled=false` 与业务文档一致。
- `isRoleEnabled` 仍按 MEMBER/LEADER 分别读取对应开关。
- detect/consume 发现本 role 关闭时仍按 role 清 pending；`clearPendingForRole(null, ...)` 仍 no-op，非 null
  只清同 role，不清另一 role。
- `hasPendingBoxForCurrentWindow` 在 role 关闭时仍只返回 false，不额外清理，保持 baseline 行为。

### O5. pending clear/retain 与 baseline 业务规则一致

- 过期或 stale window/identity/taskRun：删除当前 key 后返回 false。
- 点击 `EXECUTED`：删除 pending 并返回 true。
- 点击 `NOT_EXECUTED`：返回 false，pending 保留到原 TTL。
- detection 的 NOT_MATCHED/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED/NOT_EXECUTED 不创建
  新 pending，也不清已有 pending，和 baseline “探测 miss/失败不主动清旧 pending”一致。
- role 关闭、unknown role、requested role 不匹配的清理点与 baseline 相同。

### O6. 点击顺序是一个 atomic bundle，参数与 baseline 相同

Baseline `InputSequences.moveAndClickLeft` 为
`MOVE_MOUSE(x,y) -> SLEEP(80ms) -> CLICK_LEFT(x,y,120ms)`。Active
`CloudCommonBoxPortAssembly.click` 构造完全相同的三 action 列表，一次调用
`executeInputBundle(..., SCREEN_ABSOLUTE_PX, ...)`。DHXY handler 在 exact registration/binding 复验后把整份列表
一次提交给唯一 `InputActionQueue`；未看到拆分 move/click、queue-in-queue、自动重试或第二输入 owner。

### O7. observation 坐标、阈值和本地时间戳闭合

- DHXY mechanics 仍使用窗口相对 ROI `(623,590)-(682,618)`、模板
  `images/template/common/leader_box_marker.png`、阈值 `0.86`。
- mechanics 返回 window-client 点 `ROI origin + rounded match`；handler 用同一 exact binding origin 转成
  screen-absolute 点，再写入 `RemoteCommonBoxFact`。
- Cloud `WindowFact.CommonBoxFact` 与 CommonBox 专属 result 都要求 MATCHED 才能携 point/score/time，且 score
  至少 `0.86`、timestamp 为正；五个 mechanics 状态一对一投影。

### O8. STOPPED/UNKNOWN 没有被折成 miss 或普通 click failure

- observe：`NOT_EXECUTED` 保持独立 benign terminal；`STOPPED` 先走 `TaskCheckpoint`，未确认 stop 时转 fatal；
  `UNKNOWN` 走 fatal。
- click：`NOT_EXECUTED` 才映 false 并保留 pending；`STOPPED` 走 stop/fatal；`UNKNOWN` 走 fatal。
- observe 的 `InterruptedException` 恢复线程 interrupt flag 后转 fatal。上述路径均未触发额外 observation/input。

### O9. Active Cloud 中存在一条完整的 member 源码调用链

`AutoBattleTask.runTask -> handleAutoCombatTick -> AutoCombatService.handleCombatTick ->
consumeExitAndRecover` 在战斗退出时先调用 `detectMemberBoxAfterCombatExit`；同一 tick 随后在 first-aid 前调用
`runPendingMemberCommonBoxIfAllowed -> hasPendingBoxForCurrentWindow -> task-turn ->
consumePendingBoxIfAllowed`。空闲后续 tick 也先尝试 pending box，再处理 follower first-aid。

本地跟队 member 的另一消费点仍是 `AutoBattleTask.tryRunLocalTeamReturnRelease`：COMMON_BOX capability 开启后，
先消费 box，再调用 return-team。该顺序与 `696a12b0` 对应 caller 相同。

## Risks

### R1. Active Cloud 没有 leader detection/接任务前 consume 的外部 caller

`696a12b0` 中 leader 真 caller 位于：

- `WubeiTask`：回程确认后 `detectLeaderBoxAfterReturnHome`，接任务 NPC 前 `consumePendingBoxIfAllowed`；
- `XiuluoTaskV2`：同样的 detect/consume 两个业务点；
- `MainWindowController`：两个 UI toggle-off 的 `clearPendingForRole` caller。

Active Cloud 全树对 `detectLeaderBoxAfterReturnHome` 的唯一命中是方法定义本身；`WubeiTask`、`XiuluoTaskV2`
及 UI caller 尚不在 active Cloud task graph。因而当前源码只闭合 member producer/consumer，leader public 方法虽保留，
但未从 baseline 的真实 leader 业务点可达。父级需明确 count unit 的“完整 public caller”是否允许只以 member 链计数，
还是要求 leader 两个 baseline caller 同时可达。

### R2. 默认配置下，现有唯一 producer 链会被 member toggle 关闭

Active Cloud 的唯一外部 detection caller 是 member path，而 `memberCommonBoxEnabled` 默认 false；leader 默认 true，
但 leader caller 缺失。源码中未找到把 DHXY UI 的实时 leader/member toggle 投影到 Active
`CommonBoxService` 所读 `BotProperties` 的入口。若父级把“真实可达”理解为不仅有静态 call site，还要能在当前
authority/config 下实际产出 pending，则需要补查配置注入/同步证据。

### R3. CommonBox 目前存在两套互不相接的 Cloud 状态/开关设计

Active `CommonBoxService` 使用 `BotProperties + ConcurrentHashMap`；同时工作树还有
`CloudCommonBoxProperties + CommonBoxStateGovernor`，后者定义 tenant-scoped role toggle、pending、claim/settle、
identity fence 和 30 秒 TTL，但全树没有任何调用者。I1 链未使用 governor。

当前不会同时执行两套状态，但父级需确认权威选择：若 host/config 后续只接 governor，Active Service 看不到该开关；
若只接 BotProperties，governor 的 tenant scope/config revision 不参与本 count unit。不要把 governor 文件存在误当成
本链 toggle/pending 已接线。

### R4. Spring/host 层尚不能证明 CommonBox caller 到 assembly 的真实 bean 可达性

- `CloudCommonBoxPortAssembly` 有 `@Component`，但 `CloudServiceConfiguration` 只扫描
  `com.bot.dhxy.service`；它不扫描 `com.yueyunfe.dhxy.cloudbrain.remote`、`com.bot.dhxy.config` 或
  `com.bot.dhxy.task`。
- `CommonBoxService` 构造器需要 `CloudCommonBoxPort` 和 `BotProperties`；当前 host config 未显式注册两者。
- `AutoBattleTask` 在 `com.bot.dhxy.task`，同样不在该 component scan。
- `CloudServiceHost.create` 只注册 scope、storage、`CloudServiceConfiguration`；全树也未找到它被 server/task
  activation 调用。`RemoteTaskRunRoutes` 明确仍是 inert authority routes，不创建 Task executor。

因此“Java 源码调用链存在”与“当前 Cloud runtime 可实例化并执行该链”是两个不同结论。whole-service 计划允许
运行面暂时 dormant，但 CR271 countable-task gate 又要求 real caller reachability；父级应明确本轮计数采用哪一层门，
并核对统一 package 不能替代 bean/activation 可达性证明。

### R5. identity stale gate 依赖新 context/authority 重投影，本次 Service 比较本身不是 live read

Cloud `TaskExecutionContext` 文档和实现明确是 immutable authority snapshot；`getPlayerIdentityEpoch()` 返回创建该
context 时的 run/window tuple。Pending 创建和同一 context 下消费时，`pending.identityEpoch` 与
`context.playerIdentityEpoch` 天然相同。只有 activation/rehydration 为同 key 提供新 context，或 remote command
authority 在 terminal 前拒绝旧 snapshot，才能捕获后续 identity 变化。

DHXY input handler 确实会在提交前复验 registration/binding，这能防旧绑定输入；但这不等同于证明 Service 内
`staleIdentity` 分支仍像 baseline 的 live `WindowRuntimeContext` 一样可触发。父级需结合未来 task context producer
确认该 gate 的实际可达语义。

### R6. 本地 epoch timestamp 与 Cloud `System.currentTimeMillis()` 属于跨端时钟假设

按父级既定修复，TTL 必须锚定 DHXY 的真实 `matchedAtEpochMs`，Active 已做到。但 expire/prune 在 Cloud 用自己的
wall clock；若两端不在同一机器且时钟偏差明显，实际 30 秒窗口会缩短或延长。该点不建议在本 CR 自行改为收包时间，
只需父级确认部署时钟假设或已有同步约束。

## Parent-checklist

- [ ] 独立重跑 baseline/active 完整方法图核对：五个 public API；baseline 十个 private workflow/helper，active
  九个 private workflow/helper 加 DHXY-local `cachedTemplate`；确认它是唯一合理下沉的 baseline private owner。
- [ ] 逐行确认 consume 顺序：stop -> prune -> task -> taskRun -> window -> role -> toggle -> pending ->
  TTL/window/identity/taskRun -> click，未被 terminal adapter 改序。
- [ ] 确认 `30_000L` 从 DHXY `matchedAtEpochMs` 起算，边界仍为 `<= now` 过期 / `> now` 有效。
- [ ] 确认 role defaults/独立性，以及 toggle-off 的 pending clear 实际由哪一个 Cloud 配置权威驱动。
- [ ] 决定 `BotProperties` 与未接线 `CommonBoxStateGovernor` 哪个是本 count unit 的唯一状态/配置权威；不要双写。
- [ ] 核对 click terminal：仅 EXECUTED 清 pending；NOT_EXECUTED 保留；STOPPED/UNKNOWN 不降级、不重试。
- [ ] 核对 observation terminal：五个 mechanics 状态一对一；NOT_EXECUTED、STOPPED、UNKNOWN 均保持独立。
- [ ] 核对三 action 与单队列：MOVE -> SLEEP(80) -> CLICK_LEFT(120)，screen-absolute，同一 bundle，一次提交。
- [ ] 核对 member static caller 全链和优先级：战后 detect，box 在 first-aid/return-team 前消费，task-turn/capability
  顺序未漂移。
- [ ] 对 count gate 明确 leader caller 要求：active Cloud 缺 Xiuluo/Wubei leader detect + pre-NPC consume caller，
  不能仅以 public 方法定义存在替代业务可达性。
- [ ] 核对 host bean 图：`CloudCommonBoxPortAssembly`、`BotProperties`、`CommonBoxService`、`AutoCombatService`、
  `AutoBattleTask` 的真实注册与 task activation owner；若本阶段允许 dormant，卡片应明确这是后续运行门而非已闭合事实。
- [ ] 结合 authority rehydration 复核 identity stale：旧 identity context 是否会被替换/拒绝，Service pending 是否会被
  新 context 读到并清理，不能只看比较表达式存在。
- [ ] 统一构建由父级在 writers 稳定后执行；本报告没有提供任何 compile/test/runtime 证据。

## H2 scope close

本预检确认了 CommonBox Service 内部的主要 baseline 等价点，也标出了 caller/config/host/identity authority 的父级复核
缺口。以上均为非绑定观察与风险清单，不构成最终 review 结论或 ledger 变更依据。
