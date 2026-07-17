# TURN-13 — Foundation wiring, exact-window mode exclusion, and build convergence

## READY / PARENT FROZEN BRIEF

- 时间：`2026-07-15T16:38:00-04:00`；状态：`READY`；类型：`INTEGRATION`；`countDelta=0`。
- dependsOn：TURN-05、TURN-12 均已父级 SOURCE APPROVED。Helper-R2 只提供非绑定 readiness；本节是父级冻结合同。
- 唯一写集：
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnClientProperties.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnConfiguration.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java`
  - 条件修改 `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
  - 条件修改 `src/main/resources/application.properties`
  - 本报告。
- 禁止修改 Cloud、TURN-12/协议/executor、Task/Service/UI、其它 window/runner/manager 文件；发现前置越界立即
  BLOCKED，不得顺手扩写集。保护两仓全部 dirty/untracked。

### Acceptance contract

1. `TurnConfiguration` 只装配 inert beans：`TurnClient`、`TurnTemplateCache`、`TurnMatchStepExecutor`、
   `TurnLoopFactory`、`TurnLoopRegistry`、`TurnModeGuard`。不得创建/start per-window loop，不得 `@PostConstruct`、
   runner、scheduler、poller、server、retry/reconnect。
2. `TurnClientProperties` 使用独立 turn 配置：base URI、bearer token、positive connect/request/long-wait timeout、
   existing template root；request timeout 必须严格大于 long-wait。非 loopback 明文 HTTP 继续由现有
   `HttpsTurnClient` fail closed；不得借 `cloud.dev-sidecar.auto-start-enabled` 激活 turn。
3. `TurnModeGuard` 只是一个 in-memory synchronized policy boundary，不保存 owner/permit/session/ledger/TTL。
   local start 在同一临界区检查所有 exact window **不存在任何 registered remote loop**，并在锁内执行真实
   `MultiWindowTaskManager` submit；不得 check 后释放再 submit。
4. remote start 在同一临界区检查 exact local runner 未运行，再 `registry.create(...)` + `loop.start()`；start
   失败只 remove 本次新建且仍 stopped 的 loop，不自动 retry。方法可以为 TURN-40 保留显式调用能力，但本卡不得
   添加用户入口或自动调用。
5. `WindowTaskControlService` 的 `startSameQueue`、`startSelectedTasks`、测试身份 start 真实入口均通过 guard；
   `startSameQueue` 临界区必须从 local-team registration 等 submit 前 side effect 之前开始。已有业务顺序、结果、
   日志与 task manager 调用保持等价。stop/unregister/remote activation 仍归 TURN-40。
6. metadata supplier 只按原样传给 TURN-12 loop 并由每轮调用读取；本卡不得冻结旧 metadata、构造 `(0,0)` 或
   绕过 exact window identity。
7. Worker 不运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，不执行 Git mutation。交付后
   停止，由父级独立源码审查；所有 Java writer 稳定后父级运行 Cloud package + DHXY compile。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

`READY`

## CLAIMED

- 领取时间：`2026-07-15T16:42:31-04:00`；状态：`CLAIMED`；`countDelta=0`。
- 角色：CR271 Internal implementation Worker；父级是唯一 manager/final reviewer。
- 唯一写集严格按父级 frozen brief：三个 `cloud/turn` 新 Java、真实
  `window/control/WindowTaskControlService.java`、`application.properties` 与本报告。
- 实施边界：仅 inert bean 装配与 exact-window local/remote start 互斥；不创建/start loop，不新增 TURN-40
  用户入口、stop/unregister/Cloud activation，不修改协议/executor/Task/Service/UI 或其它 dirty/untracked。
- 当前 writers 活动，不运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，不执行 Git mutation。

`CLAIMED`

## SOURCE DELIVERED / PARENT REVIEW PENDING

- 交付时间：`2026-07-15T16:49:20-04:00`；状态：`SOURCE DELIVERED / PARENT REVIEW PENDING`；
  `countDelta=0`。
- 新增 `TurnClientProperties`：独立绑定 `cloud.turn.*` 的 base URI、bearer token、connect/request/long-wait
  timeout 与既有 template root；全部 timeout 必须为正，且 request timeout 严格大于 long-wait。
- 新增 `TurnConfiguration`：仅装配 inert `TurnClient`、`TurnTemplateCache`、`TurnMatchStepExecutor`、
  `TurnLoopFactory`、`TurnLoopRegistry`、`TurnModeGuard` Bean；没有创建 per-window loop、生命周期 hook、
  scheduler、poller 或自动启动。
- 新增 `TurnModeGuard`：local 检查全部 exact window 无 registered remote loop 后，在同一 `modeMonitor`
  临界区执行传入的真实 registration/submit；remote 在同一临界区确认 exact local runner 未运行，再执行
  `registry.create(...)` 与单次 `loop.start()`。start 抛错时只对本次创建、仍 stopped 且仍为 registry exact
  instance 的 loop 执行 remove；无 retry。
- `WindowTaskControlService` 三个真实 local start 入口均通过 guard。`startSameQueue` 只在窗口/队列参数
  校验后进入临界区，原 local-team registration、leader submit、其余 submit、日志和结果顺序原样位于锁内；
  mode conflict 在任何 registration/submit 前返回整批失败结果。
- `application.properties` 仅新增独立 `cloud.turn.*` 参数；未读取或复用
  `cloud.dev-sidecar.auto-start-enabled`，未增加 turn enabled/auto-start 开关。
- metadata supplier 只做 non-null gate，并作为原对象直接传给 `TurnLoopRegistry.create(...)`；没有缓存 metadata、
  构造 `(0,0)` 或绕过 device/window identity。

### Source evidence

- `TurnClientProperties.java` SHA-256
  `434AC07507E3796E746D816AB04EB8FDC67CB6424F77D6F39A8D7C94E24F401D`
- `TurnConfiguration.java` SHA-256
  `33F2F81F72CCE681759CC79C932E794F02B926E3198C233659D452B5A1EFEE86`
- `TurnModeGuard.java` SHA-256
  `96FA2E9FC140F992B0B5618F7626D390D270618E2467370BDD2680FB59839030`
- `WindowTaskControlService.java` SHA-256
  `5BD3132A6BB8C387E23ED99FF59FCF05726161EA3ABAC7C4E06C6E1D7E059467`
- `application.properties` SHA-256
  `729D27F17365532E104A5E5D5DD0613E58811494CBA1B8BF0162DB87A249594E`
- `git diff --check` 对两个 tracked 授权文件无 whitespace error；三个新 Java 逐文件静态检查无
  `@PostConstruct`、`@Scheduled`、scheduler/poller/reconnect/auto-start。唯一 `.start()` 是
  `TurnModeGuard.startRemote` 在显式调用边界内的单次 `loop.start()`。
- 未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

`SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE REVIEW #1

- 审查时间：`2026-07-15T16:55:00-04:00`；父级独立读取五个交付文件，并交叉核对
  `MultiWindowTaskManager`、`WindowTaskRunner`、`TurnLoopRegistry`、`WindowTurnLoop`、
  `HttpsTurnClient` 与 `TurnTemplateCache`。Helper-R3/R4 只提供非绑定风险证据，不构成裁决。
- 结论：`P0/P1/P2=0/1/2 / REPAIR #1`。配置与 inert bean 装配、三个 local submit 入口、
  `startSameQueue` 注册副作用边界、local/remote 同 monitor 线性化和同步 start-failure cleanup 主链均可保留。

### P1-1：remote start 会接受不存在或已 shutdown 的 exact runner

- 证据：`TurnModeGuard.java:70-75` 仅以
  `taskManager.getRunner(exactWindowId).filter(runner -> runner.isRunning()).isPresent()` 拒绝正在运行的本地
  runner。runner 不存在时 Optional 为空，runner 已 shutdown 时 `isRunning()` 也为 false；两者都会继续在
  `:76-83` 创建并启动 remote loop。
- 影响：未注册或永久关闭的窗口可占用 remote mode；之后同 ID 的正常 local start 会被 registry entry 拒绝，
  而真正 action 执行只能在更晚的 exact-window resolution 才失败，控制边界 fail closed 过晚。
- Repair #1 条件：仅修改 `TurnModeGuard.java`。在同一 `modeMonitor` 内只解析一次 exact runner；runner 缺失或
  `isShutdown()` 必须 typed fail closed，随后再检查 `isRunning()`，只有已注册、未关闭且空闲才允许现有
  `registry.create(...) + loop.start()`。不得自动注册、fallback、retry 或扩展 TURN-40 activation。

### P2 follow-up（不扩大本卡写集）

- `MultiWindowTaskManager`/`WindowTaskRunner` 的 public submit API 仍是结构性绕过面；当前生产调用均只在已加
  guard 的 `WindowTaskControlService`。TURN-40 及后续 caller review 必须拒绝新增 direct submit caller。
- registry mutation 与 loop start 当前只由 guard 调用；TURN-40 不得绕过 guard。若以后需要独立 remove，另开
  原子 `removeIfSameAndStopped` 设计，不在本卡新增 owner/session/ledger/retry。

### Build gate

- 首次 Cloud 尝试使用 `-DskipTests` 被 Maven enforcer `require-tests-enabled` 明确拒绝，exit 1；这不是源码
  compile 结论，也不计 Foundation build evidence。DHXY 并行命令结果未被该失败调用可靠回收，同样不得推定成功。
- Repair #1 完成并经父级复审前不运行下一轮 Maven。

`REPAIR #1 / P0/P1/P2=0/1/2`

## REPAIR #1 SOURCE DELIVERED / PARENT REVIEW PENDING

- 返修交付时间：`2026-07-15T16:58:11-04:00`；状态：
  `REPAIR #1 SOURCE DELIVERED / PARENT REVIEW PENDING`；`countDelta=0`。
- 唯一 Java 修改为 `TurnModeGuard.java`；未修改配置、其它 Java、计划或 CR。
- 精确证据：`TurnModeGuard.java:71-72` 在同一 `modeMonitor` 内只调用一次
  `taskManager.getRunner(exactWindowId)` 并保存为 exact `WindowTaskRunner` 局部值。
- 精确证据：`:73-77` 对 runner 缺失 typed fail closed；`:78-82` 对 `runner.isShutdown()` 单独 typed
  fail closed；`:83-87` 随后单独拒绝 `runner.isRunning()`。
- 精确证据：只有已注册、未 shutdown 且 idle 的 runner 才到达 `:88-95` 原有
  `loopRegistry.create(...)` + 单次 `loop.start()`；metadata supplier 仍原样传递。
- 原 start-failure stopped exact-loop cleanup 保持不变；没有自动注册、fallback 或 retry。
- `TurnModeGuard.java` SHA-256：
  `45B5708C39EC05774C8DA0E5BB17DBAC3FFCD64403ABB32F300FF7DF4DED8945`。
- 静态检查：`startRemote` 中 `getRunner(` 仅一处；`git diff --check` 无 whitespace error。
- 未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。

`REPAIR #1 SOURCE DELIVERED / PARENT REVIEW PENDING`

## PARENT SOURCE REVIEW #2 — REPAIR #1 APPROVED

- 复审时间：`2026-07-15T17:01:00-04:00`；父级独立读取当前 `TurnModeGuard.java`，当前 SHA
  `45B5708C39EC05774C8DA0E5BB17DBAC3FFCD64403ABB32F300FF7DF4DED8945` 与交付报告一致。
- 结论：`P0/P1/P2=0/0/0 / SOURCE APPROVED / BUILD COHORT BLOCKED`；源码 owner 已释放。
- 原 P1 已关闭：`:72` 在同一 `modeMonitor` 内只解析一次 exact runner；`:73-87` 依次拒绝 missing、shutdown、
  running；只有 registered/open/idle runner 到达 `:88-95` 原 create+单次 start。metadata supplier、同步失败清理、
  local 三入口及禁止 retry/activation 合同未漂移。
- 原两项 P2 已转为后续卡审查护栏，不是当前源码缺陷：TURN-40/后续 caller 不得 direct submit 或绕过 guard
  操作 registry/loop。

### Fresh build evidence

- DHXY `mvn -q -DskipTests compile`：exit 0。
- Cloud `mvn -q clean compile`：exit 1。失败集中在 TURN-13 写集外、早先原字节搬入 Cloud 的 whole
  Service/Task（例如 `TaskTrackerPanelService`、`WubeiTask`、`NavigationService`、`NpcClickService`、
  `PlayerStateService`）仍引用 DHXY-only tracker/input/window/runtime/四个本地 Service 类型；未出现 TURN-13
  新类编译错误。该阻断必须由后续 caller/local/vision/task cutover 卡逐步消除，不得在 TURN-13 复制本地 runtime。
- Cloud `mvn -q -DskipTests clean package` 另被 enforcer `require-tests-enabled` 拒绝，exit 1；当前 no-local-test
  模式下不擅自改为执行全测试 package。两次失败均不伪造成 build pass。

`SOURCE APPROVED / BUILD COHORT BLOCKED BY LEGACY CLOUD COPIES / P0/P1/P2=0/0/0`
