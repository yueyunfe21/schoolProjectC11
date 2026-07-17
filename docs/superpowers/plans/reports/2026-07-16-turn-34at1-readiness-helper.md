# CR271 TURN-34AT1 readiness helper - owner-return 后首个语义测试小片冻结

## 1. Helper 身份与结论

- 身份：CR271 内部 readiness helper；只做 TURN-34A owner-return 后的测试拆片预检。
- 本报告不是 `CLAIM`、不是 implementation delivery、不是 reviewer 结论，也不批准 TURN-34A/AT0/AT1。
- 本 helper 的唯一 write set 是本报告；未改 Java、POM、原卡、dashboard 或其它 Markdown。
- 最新父级事实：External C 已在 TURN-34A 原卡 true EOF 归还 owner；父级于
  `2026-07-16T09:38:31.235-04:00` 接受归还，当前 TURN-34A implementation owner 为零。
- 当前先行卡 `TURN-34AT0` 已于 `2026-07-16T09:47:27.553-04:00` 交付仅含 package/constructor
  编译表面的 test-source 增量；父级 Review #1 于 `09:50:00-04:00` 判定
  `P0/P1/P2=0/1/0 / REPAIR #1 REQUIRED`，External C 保持唯一 owner。AT1 可以独立于
  exit/caller/timing/recovery/maintenance 实施，但**必须串行等待 AT0 Repair #1 交付及父级接受**。
- AT1 建议冻结为：**真实 Stage-1 battle-flag 入战 + 首个 capture 的 closed command/outcome 保守语义**。
- Production 继续保持父级已通过且只读；无已批准业务差异，严格按 `696a12b0` 等价测试。

状态：`PRECHECK COMPLETE / BLOCKED BY ACTIVE AT0 REPAIR #1 / NO AT1 CLAIM / NO APPROVAL`。

## 2. 已完整读取的权威材料

本 helper 已完整读取并交叉核对：

1. `D:/mavenProject/DHXY/AGENTS.md`；
2. `docs/DHXY_CONTEXT.md`；
3. `docs/ACTIVE_WORK.md` 顶部 CR271，最新为 `09:38 External 掉线撤单与四条可执行小片`；
4. `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节；
5. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`；
6. `docs/业务逻辑.md`；
7. TURN-34A 原卡，包括 External C owner-return true EOF 与父级 owner-return accepted true EOF；
8. TURN-34AT0 当前先行卡；
9. Cloud 实际 `AutoCombatService.java` 与唯一 named test
   `AutoCombatServiceTurnContractTest.java`；
10. DHXY commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 中的
    `AutoCombatService.java` 基线全文。

权威约束归并如下：

- TURN-34A production 只迁 exact-context/HTTPS ownership，不改变战斗判断、phase 顺序、fallback、重试、
  recovery 或定时语义。
- 一次明确 capture 是一个 action、一个 command、一个 UUID；Cloud 不自动重做物理/业务动作。
- 网络不确定只能在协议层继续等待/重取同一 outcome，不得让 AutoCombat 再发同一或新 action。
- 只有已完成且可信的普通 miss 才能按 `696a12b0` 进入下一个 radar probe；terminal/uncertain 不能伪造
  combat enter/exit。
- 测试只能使用 scripted/fake turn result 与内存 PNG；不得启动真实 runtime、Task、UI、capture 或 input。

## 3. 初始证据、SHA 与行数

以下是本 helper 冻结时的只读快照：

| 对象 | 状态 | 行数 | SHA/基线 |
|---|---|---:|---|
| Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java` | 父级 production source review 已通过；AT0/AT1 只读 | 852 | `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` |
| Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java` | AT0 Review #1 的 P1 repair 起点；两条 import 尚未修复 | 762 | `98e655860873a640d96c2b528a19a18fd3c361f69f654c1237cf93ede869ac3a` |
| TURN-34A 原卡 | 最新父级 true EOF 已确认 zero owner / test tranches required | 451 | `ff1215e43d4ff76a8a35ea6b2fb0f20f17579c677dea1a31f2bc8201b1ba2490` |
| TURN-34AT0 卡 | 父级 Review #1：P1 / Repair #1 required；External C 保持 sole owner | 97 | `e940adbab734ca02306ca46dd0cc626bae50b0cd4b3088283339973cec30cd94` |
| `696a12b0` DHXY `AutoCombatService.java` | 用户确认的 pre-cloud 业务基线 | 835 | Git blob `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a` |
| TURN-34AT1 子卡 | 尚不存在；本 helper 不创建、不 claim | - | `ABSENT` |

仓库锚点仅用于辨识工作树，不是业务基线：DHXY 为 `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`，
Cloud 为 `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`。两仓已有 dirty/untracked 均为受保护现场。

### AT1 正式初始 SHA 门

- External C owner-return 与 AT0 claim 的前置锚点是 763 行 / `60e49ed9...`；本 helper 写报告期间 AT0 并发
  完成首版交付，因此上表 762 行 / `98e65586...` 是父级 Review #1 的**只读 repair 起点**。Review #1 已确认
  两条 LocalServiceClient import 仍错误并要求 Repair #1；它不是 AT1 可直接 claim 的正式 initial anchor。
- 父级只有在 AT0 出现 canonical `TEST-SOURCE DELIVERED`、完成独立 source review 并接受交付后，才能创建
  TURN-34AT1 子卡，并把 AT0 最终 test SHA 和行数逐字写成 AT1 的正式 initial anchor。
- 若届时 production 不再是 `532e6f84...`，或 test SHA/行数与父级接受的 AT0 true EOF 不一致，AT1 必须
  `BLOCKED`，不得自行 rebase、合并或猜测起点。
- AT0 未被父级接受前不得出现 AT1 owner；AT1 与 AT0 不能同时写同一 test。

## 4. 当前 named test 已覆盖什么

当前文件有 17 个 `@Test`。这些是源码覆盖事实，不等于已通过 Maven，也不等于 TURN-34A test-source 通过：

| 已有覆盖 | 当前证据 |
|---|---|
| exact STATE scope | logical window 隔离；tenant/user/device 均入 key；native fingerprint replacement；A-B-A 不复活；same-scope continuity |
| fail-closed/stop | missing holder 零 collaborator action；六个 public tick entry 的 confirmed stop 真实传播且零 command；stop 后不留 runtime state |
| 无图 uncertainty | 无效 ROI 时，IN_COMBAT 不伪造 exit；FREE 不伪造 enter；均为零 command |
| cadence/surface | dynamic `4000/2000/10000`、wake `500..10000` clamp、两个 enum、30 秒 gate、冻结 public surface |
| post-baseline absence | 六个 DHXY post-baseline API 与 legacy authority collaborator 不进入 Cloud surface |
| fixture/scaffold | 已有真实 `PackagedTemplateAssets`、`ScriptedCommandPort`、battle-flag/blank PNG、raw frame SHA/correlation helpers |

注意：`battleFlagTemplate`、`battleFlagRoiPng`、`blankRoiPng`、`completedCapture` 等正向 fixture 当前没有被任何
`@Test` 消费；现有 uncertainty 只证明“发 command 之前 ROI 不可用”的零动作分支。现有
`initializeClearsPendingWorkButKeepsDeferredLeaderRecovery` 也没有先建立 deferred leader pending，因此不能替代
后续真实 recovery 测试。

AT0 首版 delivery 未完全解决 import 静态不一致，当前有 P1 Repair #1；在父级接受前不是 source pass，且
无论 Repair #1 是否接受都不关闭下列语义缺口。

## 5. 剩余缺口与 AT1 选择理由

未覆盖缺口包括：

1. 真实 committed battle-flag PNG 经 public AutoCombat entry 触发 `FREE -> IN_COMBAT`；
2. exact action/step/ROI、raw PNG correlation、canonical/unique UUID 与 one-command discipline；
3. command terminal/uncertain 和 outcome terminal 后不 retry、不假 exit、不误走 fallback；
4. 普通 `COMPLETED` miss 的 Stage1 -> Stage2 -> Stage3 基线顺序；
5. 两轮完整 miss + 可读小地图才确认 exit；
6. `handleCombatTick` 的 enter/exit signal 消费、三种 recovery、CommonBox/first-aid 优先级；
7. 15 秒/1 秒/4 秒 FAST 路径、4 秒 maintenance、40 秒 clean、30 秒/10 秒 panel；
8. Wubei/Xiuluo 四个真实 caller 的 phase/tick/wake 消费；
9. 后续 radar stage、recovery、panel、clean/box/aid action 的 correlation/terminal matrix。

AT1 选择缺口 1-3 的**首 capture 闭环**，原因是：

- 它直接消费 External C 已写但未使用的真实 template/raw-PNG fixture；
- 它只经 `probeWindowCombatStateReadOnly` 进入 production AutoCombat 与 real `BattleRadarService`，不需要构造
  小地图、panel、first-aid、CommonBox、caller 或时钟推进；
- Stage-1 命中和“IN_COMBAT 下 Stage-1 不可用”的 production 分支都会在第一次 capture 后返回，可精确证明
  one command / one UUID / no fallback；
- 它不改变也不预判 AT2 的 ordinary-miss fallback 与 exit 逻辑。

## 6. TURN-34AT1 exact write set

父级在 AT0 通过后创建固定子卡时，AT1 implementation worker 的 exact write set 只能是：

1. Cloud
   `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`；
2. 父级届时创建的 append-only
   `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md`。

只读范围：

- Cloud `AutoCombatService.java` 必须保持 SHA `532e6f84...`；
- TURN-34A 原卡、TURN-34AT0 卡、本 readiness 报告只读；
- POM、resources、production collaborator、四个 task caller、其它 test 及 dashboard 全部只读；
- 不建第二测试、不改 `BattleRadarTurnContractTest`，不添加 production hook、Mockito、Spring context、source
  scan、private-production reflection、sleep/poll 或 testcase 输出文件。

若 AT1 worker 发现必须修改 production、resource、POM 或第二测试才能完成，应在 AT1 子卡写 `BLOCKED` 并归还
owner，不得扩大 write set。

## 7. TURN-34AT1 冻结测试合同

### 7.1 真实 Stage-1 入战

AT1 必须在现有 named test 内新增一条真实正向合同，且满足：

1. AT0 修复后的 production harness 原样复用 real `PackagedTemplateAssets`、real `BattleRadarService`、
   `CloudTurnActionFactory` 与 scripted `CloudTurnCommandPort`；不得复制 production reducer。
2. 当前共享 `RECT=(100,50,820,820)` 容不下 Stage-1 ROI；AT1 仅在 named test 中把正常 exact-window fixture
   调整为 `TurnWindowRect(100,50,1280,800)`。已有 invalid-ROI 用例继续使用自己的 `windowWithRect(...)`，
   不得因 fixture 调整而删除或弱化。
3. `latestWindowMetadata` 必须是与 context 完全相同、`stopRequested=false` 的 exact metadata。
4. 初态为 `GameContext.ActionState.FREE`；只调用 public
   `probeWindowCombatStateReadOnly(context, "fivering")`。
5. scripted reply 必须使用现有 `battleFlagRoiPng(...)` 与 `completedCapture(...)`，真实加载
   `images/template/battle/flag_battle.png`；不得把 match 结果直接 stub 成 true。
6. 结果必须为 `TickResult.IN_COMBAT`，且 `GameContext` 变为 `IN_COMBAT`。
7. Stage-1 命中后立即短路；总计只能发布一个 action/command，不得发布 Stage-2/Stage-3 capture。

### 7.2 exact command、raw frame 与 UUID

正向 action 必须逐项断言：

- `deviceId/windowId` 等于当前 `TurnInvocationContext`；
- `actionId` 可由 `UUID.fromString(...)` 解析，且 canonical string 往返不变；
- action 只有一个 index `0` 的 `CAPTURE` step；无 input/local-service/match step；
- screen-absolute、unscaled ROI 为 `TurnRegion(1074,680,51,20)`，即 window `(100,50)` 加基线 offset
  `(974,630)`；`ResultMode=UPLOAD_IMAGE`；`fullWindowFailureEvidence=false`；
- port 收到的 timeout 为 production 当前冻结值 `Duration.ofSeconds(120)`；
- outcome 使用同一 actionId 和完整 exact `TurnWindowMetadata`；step index/type/status 一致；
- raw frame 为 `image/png`，region/width/height/sourceStepIndex=`0` 与 action 一致，SHA-256 与 PNG bytes 一致；
- scripted reply 队列耗尽、execute count 精确为 1，证明没有隐式 retry/fallback。

`latestWindowMetadata(...)` 只是 exact slot 读取，不计 command，也不得生成 UUID。

### 7.3 首 capture closed-status/terminal/uncertain 矩阵

AT1 还必须在同一 named test 中，从 `IN_COMBAT` 初态覆盖首个 Stage-1 capture 的全部 closed negative status：

1. command status：`BUSY`、`DUPLICATE_ACTION_ID`、`TIMED_OUT_UNCERTAIN`、
   `INTERRUPTED_UNCERTAIN`；
2. completed outcome status：`FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`。

每个 case 必须使用 exact current metadata；`STOPPED` case 的 latest metadata 明确保持
`stopRequested=false`，用于区分已有的 confirmed-stop 零 command 合同。每个 case 都必须断言：

- public read-only probe 返回 `IN_COMBAT`，`GameContext` 仍为 `IN_COMBAT`；
- command count 只增加 1，且没有 Stage-2/Stage-3、compensation、local retry 或第二 action；
- 本次 actionId 为 canonical UUID；整个矩阵所有已发布 actionId 互不重复；
- outcome status case 使用当前 action 的 actionId 与 exact window；不得用错 correlation 制造假 terminal；
- reply 队列在预期次数后为空，任何额外 command 立即失败。

已有“confirmed stop 从六个 entry 在发 command 前传播且零 command”的测试必须保留。AT1 的 unconfirmed
`STOPPED` 只锁定“不把终态误解释为战斗退出”；它不能弱化 confirmed stop，也不能新增业务重试。

### 7.4 fallback 边界

AT1 只冻结以下 fallback 事实：

- Stage-1 真实命中：立即结束 radar，本次无 fallback；
- 已在 `IN_COMBAT` 时，Stage-1 command/outcome terminal 或 uncertain：保守保持 `IN_COMBAT`，本次无
  Stage-2/Stage-3、无 retry；
- transport 层若重取 outcome，只能针对同一 actionId，不能表现为 AutoCombat 新 command。

AT1 **不测试也不改变**普通、可信、`COMPLETED` blank frame 的基线 fallback。该结果不是 terminal/uncertain，
后续 radar stage 必须使用新的明确 capture action 和 fresh UUID；这条正常顺序由 AT2 接管。

### 7.5 AT1 明确非目标

- 不做 Stage-2/Stage-3 正向模板命中；
- 不做普通 miss fallback、两轮 exit miss 或小地图 OCR；
- 不调用 `handleCombatTick`/guard entry 去消费 enter/exit/recovery；
- 不做 panel/clean/CommonBox/first-aid/incense/maintenance；
- 不做 FAST 15 秒/1 秒/4 秒与其它时序推进；
- 不实例化或修改 Wubei/Xiuluo caller；
- 不覆盖 malformed actionId/window/step/frame correlation；这些是后续 terminal/correlation 片；
- 不以 AT1 单片声称 TURN-34A source+test passed、Approved、Done 或可运行。

## 8. TURN-34AT2 串行分界

AT2 的首个边界是**普通可信 `COMPLETED` miss 后的 baseline fallback 与 full-radar exit confirmation**，不得与
AT1 并行写同一 named test。建议父级在 AT1 delivery/source review 接受后另建固定 AT2 卡，正式 initial anchor
取 AT1 最终 SHA/行数。

AT2 只接下列相邻闭环：

1. 从 `IN_COMBAT` 开始，第一轮依次返回 Stage1/Stage2/Stage3 三张 valid blank raw PNG；三条 action 必须
   exact、UUID 唯一、顺序固定，结果仍为 `IN_COMBAT`；
2. 第二轮再次三张 valid blank raw PNG；只有完成两轮全 miss 后才发第七条
   `COORD_SCAN=(46,59,178,35)` capture；
3. 小地图帧直接复用 committed coord digit/comma pixels 并满足真实括号/逗号/左右坐标识别，不自绘字体、不改
   threshold；可读后才允许 `GameContext IN_COMBAT -> FREE`，read-only probe 返回 `NONE`；
4. 任一普通 stage 命中、capture 不可用或小地图不可读时必须保持 `IN_COMBAT`，不得伪造 exit。

AT2 不接 `handleCombatTick` recovery consumption、三种 recovery policy、CommonBox/first-aid 顺序、panel/
maintenance/FAST timing 或四个 task caller；这些继续留给 AT3+。后续 stage 的 malformed correlation 与
terminal-at-each-stage 矩阵也需另行冻结，不能塞回 AT1。

## 9. 命令与验证门

本 helper 未运行任何 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input 命令。

AT1 worker 只交 test source 与子卡证据；当前多 writer 保护期内不得自行运行 Maven。待父级确认 Java writer 全部
停止并允许 test gate 后，AT1 对应的唯一 named command 是：

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

- 不得加 `-DskipTests`，不得用 stale class/jar 代替当前 source。
- named test exit `0` 是后续父级 test gate 证据，不是本 readiness helper 的结论。
- Cloud compile/package、双 reviewer 与 TURN-34A parent closure 仍按权威计划第 19 节由父级统一编排。
- fresh runtime 是独立验收，不属于 AT1 scripted unit contract，也不得由本片启动。

## 10. 保护与交接

- 本轮只做只读检查与新增本报告；未改 Java、原卡、ACTIVE_WORK、dashboard 或任何现有 dirty/untracked。
- 未执行 Git add/commit/checkout/reset/clean/stash/switch/merge/rebase 或其它 Git mutation。
- AT1 当前没有 owner、没有子卡、没有可 claim 的 parent-accepted post-AT0 initial SHA；External C 当前只拥有
  AT0 Repair #1，不因此拥有 AT1。
- 父级下一步只能先接收并审查 AT0 Repair #1、明确接受 AT0，再以其最终 SHA/行数创建 AT1 子卡；AT1 worker
  true EOF claim 后才可写 exact test file。
- 本报告仅证明拆片边界可实施且与 AT2 可串行分离，不批准任何卡。

TRUE_EOF PRECHECK_COMPLETE
