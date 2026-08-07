# G013 天庭脱战维护中止与 Tracker 新鲜度栅栏

## 状态

- 状态：`SOURCE+TEST REVIEW PASSED / P0-P1-P2=0-0-0 / fresh runtime pending`。
- Worker：`Ptolemy`（`019fcbea-91f2-7773-b93e-94d40c53639b`）；父级 reviewer：当前 Codex。
- 建卡日期：`2026-08-04`。
- Client 工作树：`D:\mavenProject\DHXY-cr271`，分支/HEAD：
  `thin-client-design` / `2f083c14152106ba6ad418a0c29e3e0e2148e14a`。
- Cloud 工作树：`D:\mavenProject\dhxy-cloud-brain`，分支/HEAD：
  `navigation-migration` / `363d0e3fae73c0c55f4920f6f1c61338a0458d73`。
- 保护基线：`D:\mavenProject\DHXY` / `codex/baseline-696a12b0` /
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，只读比较，不得修改。
- 两个活动仓已有大量 dirty/untracked；全部保护，不回滚、不覆盖、不清理、不提交。

## 现场与根因

目标 run：`remote-turn-e315af9f-f23e-4511-aeb6-fad210066e99:0:TIANTING`，窗口
`hwnd-F99187E`。

1. `03:41:52` 的 `25s` park 到期后，天庭按旧 `IN_COMBAT` facts 选择 `PARK_COMBAT`，进入
   `AutoCombatService.handleCombatTick(... CLIENT_RUNNER_EXIT)`。
2. 本轮满足 `COMBAT_UI_CLEAN_INTERVAL_MS=40000`，并同时满足自动面板 `REFRESH_DUE`（现场配置
   `intervalMs=120000`），故启动 generic-window 清理与面板刷新。
3. Client 于 `03:41:53.491` 确认脱战，Cloud 于 `03:41:53.590` 收到 exact exit；已开始的维护链没有
   脱战检查点，继续执行至 `03:42:00.195`。
4. 保护基线共享 `AutoCombatService.handleCombatTick()` 在维护前同步
   `battleRadarService.checkAndSyncCombatState()`；迁移版 `CLIENT_RUNNER_EXIT` 为避免 Cloud 二次裁决而只读
   Runner 异步写入的 `GameContext`。迁移方向正确，但缺少跨耗时阶段的退出检查。
5. 脱战后 Tracker action 于 `03:42:00.360` 已准备；当前代码在整个 recovery 完成后才设置
   `trackerLinkNotBeforeMs=03:42:02.743`，将正确 action 拒绝，等待 `1.79s` 重发。

## 用户批准合同

1. 不删除战斗维护，不改变进战 `4s`、周期 UI 清理 `40s` 或配置驱动的自动面板刷新频率。
2. `CLIENT_RUNNER_EXIT` 继续以 Client Runner exact combat edge 为唯一战斗权威，不恢复 Cloud Radar 二次
   裁决。
3. 每个可能耗时或产生输入的维护阶段开始前必须确认仍为 `IN_COMBAT`；一个已经开始的本地 generic
   清理允许返回，但返回后若已脱战，禁止继续左上探针、面板截图/拖拽或 `Alt+8`。
4. 自动面板流程在真正发送拖拽或 `Alt+8` 前必须有最后一道 `IN_COMBAT` gate，防止脱战发生在截图期间。
5. Tracker 新鲜度不得只看年龄，也不得以 recovery 结束时间粗暴截断。保留进战前 task-box fingerprint：
   - action 早于 exact `combatExitedAtMs`：拒绝；
   - fingerprint 缺失：未决，等待；
   - fingerprint 与进战前相同：旧画面/当前小任务未推进，拒绝普通绿链；暗雷由 G011 连续巡逻合同处理；
   - fingerprint 已变化：属于战后新任务框，立即允许，不必等待 recovery 后再次发布。
6. 不改模板、ROI、阈值、绿链坐标算法、天庭 dialog、引妖、暗雷巡逻点、导航、队伍恢复或其他任务。

## 精确写集

Cloud 生产代码：

- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- `src/main/java/com/bot/dhxy/task/tianting/TiantingTask.java`

Cloud 测试代码（优先扩展既有类；没有合适类时只允许建立以下命名类）：

- `src/test/java/com/bot/dhxy/service/AutoCombatMaintenanceExitFenceContractTest.java`
- `src/test/java/com/bot/dhxy/task/tianting/TiantingPostCombatTrackerFreshnessContractTest.java`

Client Java、双仓协议、图片、配置、其他任务以及上述清单外文件全部写集外。文档与 dashboard 由父级维护。

## 必须通过的测试

1. 脱战发生在维护开始前：不调用 generic 清理、左上探针、面板 verify、拖拽或 `Alt+8`。
2. 脱战发生在 generic 清理期间：当前清理可返回，之后不调用左上探针和面板逻辑。
3. 脱战发生在面板截图/匹配期间：不得再拖拽或发送 `Alt+8`。
4. 全程在战斗中：原 `4s/40s/REFRESH_DUE` 行为保持。
5. Tracker action 早于脱战：拒绝。
6. Tracker action 晚于脱战但 task-box fingerprint 与进战前相同：拒绝。
7. Tracker action 晚于脱战且 fingerprint 已变化：立即接受，即使 action 早于 recovery 完成。
8. fingerprint 缺失：保持未决，不得猜测。

## 编译与审核门

- worker 运行适用的定向单元测试及 `mvn -q -DskipTests compile`；不得启动 runtime/application/game、
  poller、UI、capture 或真实 input。
- 父级逐行审查全部 diff，核对没有 Cloud Radar 回流、没有频率变化、没有新 magic timeout、没有扩大
  天庭业务范围；随后独立重跑定向测试、compile 和精确写集 `git diff --check`。
- 只有父级确认 `P0/P1/P2=0/0/0` 才能标记源码交付；fresh runtime 仍需用户后续实测。
