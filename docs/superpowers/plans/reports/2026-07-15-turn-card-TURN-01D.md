# TURN-01D Report — 双端 validator 与 byte-parity integration

## CLAIMED

- 领取时间：`2026-07-15T14:53:04-04:00`
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA validator/parity integration)`；`countDelta=0`。
- `startDependsOn`：`TURN-01A`、`TURN-01B`、`TURN-01C`，三者均为 `SOURCE APPROVED / BUILD COHORT PENDING`。
- 写集：DHXY 与 Cloud Brain 各新增 `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`，以及本报告。
- 禁止：修改 `TURN-01A/01B/01C` 已交付 protocol 文件和其它任何模块；不回滚、覆盖、清理或提交既有 dirty/untracked。
- 交付目标：验证五类 step 的互斥字段、最多一张上传 frame、typed operation 参数、尺寸/hash/actionId，并确保所有 protocol 文件两仓 byte-identical；Foundation Maven 仅在父级宣布 cohort 稳定后执行。

## 两仓领取瞬间 git status

- DHXY `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`：既有 config/docs/pom/input/service/task/window Java 修改，以及计划、spec、`cloud/remote/`、`cloud/turn/` 和多个 Service/model 路径未跟踪；全部保护。
- Cloud Brain `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`：既有 pom/server/engine/gateway/maps 修改，以及 logs、migration 目录、`com/bot/`、turn/host/remote 等路径未跟踪；全部保护。
- 当前卡只创建两仓 validator 与本报告，不处置上述任何既有状态。

## 执行边界

- 父级为唯一 reviewer。若出现 P0/P1/P2、BLOCKED 或 REPAIR，仅按父级精确证据在本卡写集内返修。
- 不启动 runtime/application/server/Task/poller/UI/capture/input/tests；不自行运行 Maven，直至父级宣布 Foundation build cohort 稳定。

## CLAIMED - Internal Worker 接续领取

- 接续领取时间：`2026-07-15T14:54:21.2072569-04:00`。
- 角色：`Internal implementation Worker`；不是 manager/reviewer，不自批。
- 状态：`CLAIMED`；`countUnit=N/A (INTEGRATION validator/parity)`；`countDelta=0`。
- `startDependsOn`：`TURN-01A`、`TURN-01B`、`TURN-01C` 当前均已由父级写明
  `SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING`，源码 owner 已释放。
- 唯一 Java 写集：
  - `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnProtocolValidator.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\turn\protocol\TurnProtocolValidator.java`
  - 两份 Java 必须 byte-identical。
- 文档写集：仅本报告，且只追加领取、交付、SHA/parity/scoped-check 证据。
- 只读：两仓 `TURN-01A/01B/01C` 全部 protocol 文件，以及现有 `TURN-02` validator 调用点。
- 禁止触碰：其它 protocol、Service、server/client、`pom.xml`、配置、权威计划、CR271、
  `docs/ACTIVE_WORK.md`、dashboard 及任一既有 dirty/untracked；不回滚、覆盖、清理、删除、提交。

## 接续领取状态与 parity 基线

- DHXY：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`；工作区已有大量他人
  config/docs/pom/input/service/task/window 与 `cloud/remote`/`cloud/turn` dirty/untracked，全部保护。
- Cloud Brain：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`；工作区已有他人
  pom/server/engine/gateway/maps、migration、host/remote/turn dirty/untracked，全部保护。
- 两仓目标 validator 在接续领取时均不存在；24 对 `TURN-01A/01B/01C` Java 当前逐文件 SHA-256 全部相等。
- 其他 Java writers 仍在活动；本 worker 不运行 Maven、tests/runtime/application/server/Task/poller/UI/capture/input。
- 交付状态固定为 `SOURCE DELIVERED / BUILD COHORT PENDING`，交付后停等父级源码审查。

## SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:02:00-04:00`。
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`；`countUnit=N/A (INTEGRATION validator/parity)`；`countDelta=0`。
- 精确写集：仅两仓各新增 `cloud/turn/protocol/TurnProtocolValidator.java`，及本报告；未修改
  `TURN-01A/01B/01C` 的任何已交付 protocol 文件，未触碰 Service、server/client、配置或其他 dirty/untracked。
- `TurnProtocolValidator` 仅暴露冻结的三项入口：`requireValid(TurnRequest)`、`requireValid(TurnAction)`、
  `requireValid(TurnOutcome)`。它校验 version=`1`、非空 action/window identity、窗口/ROI/frame 正尺寸、
  SHA-256 形状、ordered `step.index`、五种 step 的唯一字段组合、输入参数形状、threshold `[0.0,1.0]`、
  闭合 local operation 参数组，以及每 action 最多一个返回 frame。Bag 三种 intent 直接保持既有
  `BagReturnItemMacroIntent` factory 的 `-1/null` 组合；没有新增 wire 字段、wrapper、业务 DTO、
  workflow/session/TTL/retry 或 Service 调用。
- 双仓 validator 当前 SHA-256：`87AB8AB12742EE8C106523DCC5EE7BAA6B2E24C7BC8B2EDE4AAC4B8A33E12FCF`；
  `ByteIdentical=True`。
- parity：两仓 protocol 目录完整 25/25 个 `.java` 文件名一致，且逐文件 SHA-256 全部相等；本卡 validator
  与既有 24 个 `TURN-01A/01B/01C` 文件均未见 byte mismatch。
- scoped evidence：两仓 `git diff --check -- src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  均 exit `0`；跨仓 25 对文件的 `git diff --no-index --check` 均 exit `0`。Git 的 LF/CRLF 提示未改变当前
  bytes，且不构成 whitespace error。
- 当前 writers 活动，依父级 cohort 规则未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；
  未执行 reset/clean/checkout/commit 或其他 Git mutation。
- 等待父级唯一 reviewer。若出现 P0/P1/P2、BLOCKED、REPAIR 或明确返修，只在本卡精确写集内处理；未见
  `PARENT APPROVED，P0/P1/P2=0，card CLOSED` 前不自批、不领取另一张卡。

## Parent Scheduling Correction #1

- `2026-07-15T15:08:00-04:00` 父级核对线程与报告后确认：External 林明已于 `14:53:04` 先领取本卡；
  后来的 Internal 接续领取构成重复写集，不具备第二 owner 权。
- 父级已立即停止 Internal 重复 Worker；本卡唯一实现 owner 保留为 External 林明，继续完成两仓
  byte-identical validator。释放出的 Internal 槽改派互斥 `TURN-10P`。
- 本纠正只处理排班冲突，不构成源码 review/批准；最终仍由父级独立审查林明交付的当前 bytes。

## Parent Source Review #1

- 审查时间：`2026-07-15T15:39:00-04:00`；父级独立读取两仓 validator 并重算 SHA/parity，当前
  `87AB8AB...E12FCF` byte-identical。
- 结论：`REPAIR REQUIRED / BUILD COHORT PENDING`；`P0=0 / P1=1 / P2=0`。
- **P1 - outcome step-result 合同未闭合。** `TurnProtocolValidator.java:54-67` 未要求
  `TurnStepResult.code` 非空，未限制 `match` 只能出现在 `MATCH_TEMPLATE`、`localResultJson` 只能出现在
  `LOCAL_SERVICE`；`:237-246` 对 `COMPLETED` 只检查 `failedStepIndex==null`，因此仍接受含
  `FAILED/NOT_RUN` 的 completed outcome；FAILED 也未要求失败前 COMPLETED、失败后 NOT_RUN。Cloud 会把这些
  自相矛盾 outcome 记为已接受并释放单槽。
- 返修条件：两仓同字节修改 validator：所有 step result code 非空；match/localResultJson 只允许对应 step type
  （对应 step 自身在物理失败时可为 null）；COMPLETED 全部 result 必须 COMPLETED；FAILED 必须严格为失败前
  COMPLETED、failed index FAILED、失败后 NOT_RUN。不得改 DTO 或扩 wire；STOPPED/
  DUPLICATE_OR_UNCERTAIN 只保持现有已冻结规则，不自行发明新语义。
- 复验点：父级重读上述分支、重算 25/25 parity，并确认既有 action/input/local operation/frame 校验未弱化。
- Maven：writers 活动中不运行；本结论不允许 APPROVED/CLOSED。

## Repair #1 SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:41:00-04:00`；状态：`SOURCE DELIVERED / BUILD COHORT PENDING`。
- 仅修改本卡原批准写集的两仓
  `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`，并追加本报告；未修改 DTO/wire、
  Service、server/client、配置或任何既有 dirty/untracked。
- 父级 P1 精确处理：
  - 两仓 `:54-67`：每个 `TurnStepResult.code` 均经 nonblank 校验；`match` 仅允许
    `MATCH_TEMPLATE` result、`localResultJson` 仅允许 `LOCAL_SERVICE` result；这两个匹配类型自身仍允许在
    物理失败时为 null。
  - 两仓 `:239-267`：`COMPLETED` 要求每个 step result 均为 `COMPLETED`；`FAILED` 要求 failure index
    有效、此前全部 `COMPLETED`、该项恰为 `FAILED`、之后全部 `NOT_RUN`；`STOPPED` 与
    `DUPLICATE_OR_UNCERTAIN` 维持原有仅禁止 `failedStepIndex` 的冻结语义。
- 双仓当前 SHA-256：`1B4CC9E98B26D822CA44491FC13488118874071B1277DFB7CCB07183324DCDD9`；
  `ByteIdentical=True`。
- 静态证据：跨仓 validator `git diff --no-index --check` exit `0`；DHXY 与 Cloud Brain 各自 scoped
  `git diff --check -- src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java` 均 exit `0`。
- 当前 writers 活动，未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；未执行
  reset/clean/checkout/commit 或其他 Git mutation。等待父级唯一 reviewer 复审；不自称 APPROVED，
  不领取下一卡。

## Parent Re-review #1

- 复审时间：`2026-07-15T15:50:00-04:00`；父级独立读取两仓完整 validator、逐项复核 Repair #1 分支并
  重算 SHA/parity，没有以 Worker 自述代替源码审查。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`，Parent Review #1 的 P1 已关闭。
- 证据：`TurnProtocolValidator.java:54-67` 已强制每个 result `code` nonblank，并把 `match`/
  `localResultJson` 限于 `MATCH_TEMPLATE`/`LOCAL_SERVICE`；`:238-267` 已要求 COMPLETED 全部 step result
  为 COMPLETED，并要求 FAILED 严格满足 completed-before、failed-at、not-run-after；STOPPED 与
  DUPLICATE_OR_UNCERTAIN 的既有规则未扩大。
- 两仓当前 SHA-256 均为
  `1B4CC9E98B26D822CA44491FC13488118874071B1277DFB7CCB07183324DCDD9`，`ByteIdentical=True`。
- 影响与剩余门：validator 源码 owner 释放；Foundation Java writers 仍活动，本卡只进入统一双仓 Maven cohort，
  不在本轮单独构建。External 林明已立即续派互斥 `TURN-10B`，未留下规划空档。
