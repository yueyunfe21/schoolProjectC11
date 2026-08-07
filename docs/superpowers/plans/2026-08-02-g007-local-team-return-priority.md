# G007 本地队伍归队优先级与停等隔离

## 状态

- `REPAIR IN PROGRESS / REVIEW P1 OPEN`
- 适用范围：所有使用本地 `TEAM_RETURN` 支援窗口的任务。天庭本次复现只是证据来源，修罗、五倍及后续任务必须复用同一套规则，禁止另写天庭专用分支。
- Client：`D:\mavenProject\DHXY-cr271` / `thin-client-design`；Cloud：`D:\mavenProject\dhxy-cloud-brain` / `navigation-migration`。
- 两仓均有既有 dirty/untracked 修改；本卡只允许叠加精确写集，绝不回滚、覆盖、清理或提交其他人的改动。

## 现场证据

`2026-08-02 10:01` 天庭队伍归队窗口中，战斗边沿约 `10:01:11.46` 已被本地 Runner 确认，首次真实归队点击却直到 `10:01:39.763` 才发生。中间出现：

1. 过期的 combat-entry maintenance 在恢复后补跑：`10:01:12.726` generic UI cleanup，`10:01:14.531` / `10:01:18.282` Alt+8；
2. leader 反复读取相同归队信号并重复唤醒/日志；
3. observer 持续对同一绿链发布 `PREPARED_ACTION_READY`（sequence 98/99/100），使 leader 的归队停等被无关 action 打断；
4. `openLocalTeamReturnSupportWindow(...)` 只开启 `TEAM_RETURN` / `COMMON_BOX`，未开启 `FIRST_AID`，队员在归队前不能得到补血资格；
5. generic idle/entry maintenance 在归队优先期插队，且 return UI cleanup 早于归队按钮尝试。

## 用户裁决与不变量

1. **归队是通用本地队伍状态，不是天庭特例。** 所有本地 leader signal 统一经一个可复用 owner 管理 capability 与 active 状态。
2. leader 从“无归队信号”切到“有归队信号”时，**只一次**开启 `FIRST_AID` 与 `TEAM_RETURN`，并只一次唤醒队员；信号持续期间只 park/recheck，禁止重复 open、notify、重复日志风暴。
3. 信号清除时只一次关闭归队窗口并允许 leader 继续。不能靠固定 sleep 自旋。
4. 队员取得该窗口资格后的顺序固定为：先尝试 `FIRST_AID`，无论补给成功、失败、无药或无需补给，均继续尝试 `TEAM_RETURN`。归队点击优先于普通箱、三技能及所有普通维护。
5. UI cleanup 不是归队前置动作：仅当归队按钮找不到，或归队点击已真实执行但未观察到移动时，才走既有 cleanup/retry fallback。
6. local-team-return active 期间：
   - 禁止 combat-entry 的补跑 cleanup / Auto+8 面板校验 / rounds refresh；
   - observer 不得为同一 leader 继续创建或替换 tracker green-link `PREPARED_ACTION_READY`；已有同类 pending action 必须不再唤醒 leader；
   - leader 必须只因“可执行的归队结果”继续，不因重复绿链 action 被唤醒。
7. 本地 Runner 已确认 `COMBAT_EXITED` 后，不得由过期 cloud context 再把窗口当成 combat-entry 来补跑维护；应取消 stale entry-maintenance deadline。
8. 不改绿链、OCR、物理点击坐标、战斗业务判定或任务次数业务语义；本卡只改归队窗口的通用优先级、抑制与恢复。

## 实施设计

### 1. 唯一归队状态 owner

在 `TaskMaintenanceService` / local team session 的既有 owner 内表达 `inactive -> active -> inactive` 边沿；返回明确 transition result 给任务调用者。

- `inactive -> active`：开启 `FIRST_AID`、`TEAM_RETURN`（保持既有 common-box 兼容性），一次 notify；
- `active -> active`：不重复开 capability，不重复 notify；调用方仅按有界时间 park/recheck；
- `active -> inactive`：关闭归队相关 capability，并允许原任务 phase 继续。

任务仍可自己读取本地 leader-return template，但不得自己维护第二套“是否已打开”标志。天庭、修罗、五倍若有调用点，必须统一接入此边沿 API。

### 2. 队员 release 顺序

`AutoBattleTask` 的 local return release 以 capability 为门：`FIRST_AID attempt -> TEAM_RETURN attempt`。失败也视为该轮尝试完成并继续归队，避免无药造成 leader 永久等待。归队按钮成功且看到移动后不清 UI；按钮缺失或点击后未移动才准入既有 cleanup/retry。

### 3. active 期间隔离后台噪音

`AutoCombatService`、`AutoCombatPanelService` 和 `CloudWholeTaskObserver` 读取同一个 generic active 状态：

- active 时不排/不补跑 combat-entry maintenance，不做 Alt+8/round-refresh；
- Runner `COMBAT_EXITED` 已确认时立即清除 stale entry-maintenance，而不是等待 due time；
- active 时 observer 不生产/替换 tracker pathing prepared action；已有同类 action 不得成为 leader wake reason。

不得把这套 gate 写成 `taskCode == tianting`。如果现有 API 只能拿到 taskCode，先扩展为 session/capability 查询，再接入。

## 精确写集

Cloud（预计）：

- `service/TaskMaintenanceService.java` 与它直接拥有的 local session/capability model；
- `task/AutoBattleTask.java`；
- `service/AutoCombatService.java`、`service/AutoCombatPanelService.java`；
- `turn/runtime/CloudWholeTaskObserver.java`；
- 天庭/修罗/五倍中**仅现有** local return wait 调用点；
- 对应已有测试类或新增聚焦合同测试。

Client：仅在现有 local-team-return active 状态确需客户端可见时改协议的双仓共享文件；默认不改 Client。若要改共享协议，必须 byte-identical 且在交付记录中写出理由。

## 验收

1. 任一任务首见 local return signal 时，日志只有一次 `TEAM_RETURN` capability open / wake；持续 signal 期间无重复 open/notify。
2. signal clear 后 capability 只关闭一次，leader 恢复原 phase。
3. 队员先运行 first-aid 尝试，随后必有 return 尝试；first-aid failure 不阻止 return。
4. return button 正常点击并观测到移动时，不出现 cleanup；no-match 或 click-no-movement 才出现 cleanup/retry。
5. active 期间零 `entry-maintenance-ui-clean`、零 `auto-combat panel` Alt+8、零 tracker green-link prepared action 重复发布。
6. Runner combat exit 已确认的窗口，恢复后不再执行过期 combat-entry maintenance。
7. 合同测试至少覆盖：边沿去重、clear、first-aid-failure-then-return、cleanup 准入条件、active suppresses Alt+8、active suppresses tracker action、post-exit clears stale entry work。
8. 适用 Maven compile 门通过；不启动 runtime/UI/capture/input。fresh runtime 验收由用户执行。

## 交付纪律

- 先在 `docs/ACTIVE_WORK.md` 记录 baseline、dirty 状态与实现范围，再改代码。
- 不得只写 Status；交付必须列出实际执行的测试命令、结果、未运行原因及 fresh runtime 观察关键字。
- 本卡未完成前不得宣称天庭专属问题已修复；结论必须表述为通用 local-team-return 机制。

## 2026-08-02 实施记录

- Cloud 实现写集：`TaskMaintenanceService`、`AutoBattleTask`、`AutoCombatService`、
  `CloudWholeTaskObserver`、以及天庭/修罗/五倍的既有 leader return-wait 调用点。
- `TaskMaintenanceService.updateLocalTeamReturnSupport(...)` 现在拥有 session 的
  `inactive -> active -> inactive` 边沿；active 首次只开 `FIRST_AID + TEAM_RETURN + COMMON_BOX` 并一次
  notify，持久 active 不重开、不 notify；clear 只关该归队窗口实际打开的 capability。
- `AutoBattleTask` 现在先进行 pending FIRST_AID best-effort，再无条件尝试 TEAM_RETURN；普通箱不再插队。
  `AutoCombatService` / observer 读取同一 active 标记，抑制 stale entry cleanup、Alt+8/round refresh 与
  tracker pathing prepared action；Runner 已非战斗时清除 stale entry deadline。
- 静态门：`D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q -DskipTests=false compile` 通过；定向
  `git diff --check` 通过。未启动 runtime/UI/capture/input。
- 未声称用户验收。G007 的 seven-point 合同测试与 fresh runtime 仍待 reviewer/后续补齐；本轮只完成主源码
  编译门和实现记录，不能以此替代实际队员归队场景验证。

## 2026-08-02 第一轮复核

通用 session 边沿、首开 `FIRST_AID + TEAM_RETURN`、修罗/五倍/天庭的调用接入以及 active 对 entry
maintenance/后续 tracker action 的抑制，静态阅读方向正确；但以下问题阻断交付。

### 已修：归队点击的移动确认与 cleanup/retry 闭环

`AutoBattleTask.tryRunLocalTeamReturnRelease(...)` 在 first-aid 后直接调用
`TeamReturnService.clickReturnTeamIfPresent(...)`。后者的 boolean 只表示按钮存在且物理 click 已提交，
不观察移动，也没有在 `no-match` 或 `click-no-movement` 时才进入 cleanup/retry 的路径。第二轮已以
`ReturnAttempt` 复用既有 local pathing/movement 事实补齐：已点击且观察到移动时不得 cleanup；按钮未命中
或 click 后无移动才允许 cleanup/retry。仍待 fresh runtime。

### P1：七项合同测试未完全交付

Maven compile 不替代合同测试。现有 `G007LocalTeamReturnContractTest` 覆盖 active/clear，
`G007IsolationGuardTest` 实际以 console isolation 通过 active suppresses Alt+8/entry work 与 observer
suppresses tracker action；但 first-aid failure 后仍 return、cleanup 准入、combat-exit clears stale entry
maintenance 仍缺可运行的独立证明。须继续补齐并记录真实命令及结果。

### 已核：旧 `PREPARED_ACTION_READY` 的唤醒语义

observer 已停止后续 publish 并清 tracker action。三任务的 return wait 不把 `PREPARED_ACTION_READY` 直接
当作归队成功；即使旧 ready event 使 wait 提前返回，其后的 prepared payload consume 已为空，不能推进
tracker click。此项仍待 fresh runtime 观察，但不再是独立 P2。

## 2026-08-02 第二轮返修记录

- `TeamReturnService.attemptReturnTeam(...)` 将原先“物理 click 已提交”的 boolean 收束为
  `MOVEMENT_OBSERVED`、`NO_MATCH`、`CLICKED_NO_MOVEMENT`。click 后最多 2 秒只读取既有 Runner
  pathing mirror；只有 `ACTIVE` 或 click 后的 `movementObservedAtMs` 才确认移动。成功移动不 cleanup；
  `NO_MATCH` 与 `CLICKED_NO_MOVEMENT` 才允许 `AutoBattleTask` 调用既有 generic UI cleaner，并在下一
  idle tick 重试。
- P2 核对：三任务的 team-return phase 不把 `PREPARED_ACTION_READY` 直接当作归队成功；active 期间
  observer 清掉对应 tracker prepared payload 后，旧 ready event 即使被唤醒也无法通过后续的 prepared
  payload 消费推进 tracker 点击。
- 静态门：`D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q -DskipTests=false compile` 通过，定向
  `git diff --check` 通过。
- 合同测试：新增 `G007LocalTeamReturnContractTest`（真实 `TaskMaintenanceService` 边沿与 capability
  断言）。执行 `mvn -q -Dtest=G007LocalTeamReturnContractTest test` 时**未进入测试执行**，因为当前
  worktree 的既有 test-compile 错误（`CloudTurnTaskFactoryAllowlistTest`、`YipinGuardClickPlanTest` 等）
  先行阻断。不得记为绿灯；seven-point 聚焦合同测试也尚未全部补齐，因此保持
  `REPAIR IN PROGRESS / REVIEW P1 OPEN`，不得交付或声称验收。

### Isolation 证据与剩余阻断

- 已实际运行 isolation：先以 `javac` 用 `target/classes + junit-platform-console-standalone-1.10.2.jar`
  编译 `G007IsolationGuardTest`，再以 console 加入 `slf4j-api/slf4j-simple 2.0.13` 执行，结果 `2/2` 通过。
  覆盖 `AutoCombatService.maybeRunCombatMaintenance` 所调用的 active gate（因此不走 entry cleanup/Alt+8/round
  refresh），以及 `CloudWholeTaskObserver.probeTrackerPreparation` 的 active gate（清 pending 后 return，publish
  分支不可达）。
- `Runner COMBAT_EXITED` 清 stale deadline 与 `WAIT_TEAM_RETURN` 的旧 ready-event 不能推进 tracker 的端到端
  isolation 仍被技术闭包阻断：前者的唯一生产入口在 `AutoCombatService.handleCombatTick`，需构造
  `AutoCombatPanelService`、`CloudUiCleanerPort`、`CloudTaskTurnAssembly` 等 11 个协作者；后者需完整的
  `TiantingTask/WubeiTask/XiuluoTaskV2` phase context、ready bus、prepared state 与 task-turn owner。现有 Maven
  test-compile 又被写集外 `CloudTurnTaskFactoryAllowlistTest` 与 `YipinGuardClickPlanTest` 阻断。未以 mock
  predicate 冒充这两条端到端证明，仍保持 P1 open。
