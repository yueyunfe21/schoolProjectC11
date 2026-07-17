# TURN-34AT2 Readiness Preflight（Internal helper）

日期：2026-07-16  
角色：CR271 Internal helper  
用途：给 External C 在 TURN-34AT0 / TURN-34AT1 完成后的下一小片提供可直接冻结的 test-only 合同。  
结论性质：readiness / preflight，不是 review，不是批准，不改变任何 CR 状态。

## 1. 本轮边界

本轮只准备 TURN-34AT2，不修改 Java，不运行 Maven、runtime 或物理输入，也不执行任何 Git mutation。

本轮唯一实际写入文件是本报告：

`D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-34at2-readiness-preflight-helper.md`

External C 的未来 AT2 实现应继续保持 test-only：生产源码、资源、POM、其它测试和调用方全部只读。

## 2. 已完整读取的权威材料

- `AGENTS.md`
- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md` 顶部 CR271
- `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- `docs/业务逻辑.md`
- `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md`
- `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT0.md`
- `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md`
- 当前 `AutoCombatService.java`
- 当前 `AutoCombatServiceTurnContractTest.java`

补充读取了 `BattleRadarService.java` 和既有 `BattleRadarTurnContractTest.java`，仅用于核对真实雷达退出语义、坐标模板和协议断言；二者不进入 AT2 写集。

## 3. 锚点与可信度

### 3.1 已接受锚点

- TURN-34A 生产源码：`AutoCombatService.java`
  - SHA-256：`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`
  - 852 行
  - AT2 必须保持只读。
- TURN-34AT0 最终测试源码：
  - SHA-256：`4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc`
  - 762 行
  - 父级 Review #2 已记录通过，P0/P1/P2=`0/0/0`。
- `BattleRadarService.java` 读取快照：
  - SHA-256：`fb606fc590a9a33dbd9fd1e4f5f2b67aa1e1b10612e908379c37ec792b276202`
  - 仅作为真实退出流程的只读证据，不是 AT2 可改锚点。

### 3.2 AT1 活跃快照不是 AT2 基线

本 preflight 读取期间，`AutoCombatServiceTurnContractTest.java` 仍处于 AT1 写入窗口；观测到的快照为：

- SHA-256：`04be925e9cdd7ce8d1503fb378abcf812e8bdb9fe4706dc169f41325f9e084c3`
- 767 行
- 已看到普通窗口矩形调整为 `TurnWindowRect(100, 50, 1280, 800)`。

该值只是并发观察，不得写成 AT2 的 initial SHA，不得据此 claim。AT2 卡必须等待 AT1 真正 `TRUE_EOF`、父级 source/test review 结论和 owner release，再填写 AT1 最终接受的 SHA、行数和测试数量。

## 4. 证据分片选择

### 4.1 各语义域归属

| 语义域 | 已有/当前归属 | AT2 处理 |
|---|---|---|
| enter | AT1：FREE + Stage1 真旗标进入 `IN_COMBAT` | 不重复 |
| terminal / uncertain | AT1：首 capture 的 `BUSY`、`DUPLICATE_ACTION_ID`、`TIMED_OUT_UNCERTAIN`、`INTERRUPTED_UNCERTAIN` | 不重复，不向后续 stage 扩张 |
| exit | 尚未进入 AT1 写集；真实逻辑要求两轮完整雷达 miss，随后坐标图可读才退出 | **AT2 主体** |
| caller | `AutoCombatService.probeWindowCombatStateReadOnly(...)` 是公开只读入口 | **AT2 只走该入口** |
| UUID | AT1 只需证明首 capture；退出闭环需要七次真实 command | **AT2 证明 7 个合法且唯一 UUID** |
| recovery | `handleCombatTick(...)` 消费 exit signal 并恢复战斗前任务 | AT3+，AT2 只证明 signal 未被只读入口消费 |
| timing | `+4s`、`40s`、`30s`、`10s` 等调度/维护节奏 | AT3+ |
| maintenance | 面板、CommonBox、急救、香、FAST 节奏 | AT3+ |
| task callers | 五倍/修罗/五环/大雁塔等四类业务调用方 | AT3+ |

### 4.2 AT2 的最小独立语义

AT2 只覆盖：

`普通 COMPLETED capture -> Stage1/2/3 全 miss -> 两轮 debounce -> 坐标图可读/不可读分叉 -> public read-only probe 返回值、状态、exit signal 与 7-action UUID/协议序列`

这与 AT1 连续，因为它从 AT1 已建立的 capture harness、窗口矩形和协议断言继续；又与 AT1 语义独立，因为不再证明 enter 或首 capture terminal，而只证明普通成功响应下的 exit 决策。

## 5. AT2 开工前置条件

以下条件必须全部满足，External C 才能 claim AT2：

1. TURN-34AT1 卡有完整交付尾标，并记录父级对最新 test source 的接受结论。
2. AT1 没有未解决 P0/P1/P2、返修要求或 owner 占用。
3. `AutoCombatServiceTurnContractTest.java` 没有活跃 writer。
4. 父级创建固定卡 `2026-07-16-turn-card-TURN-34AT2.md`，写入 AT1 最终接受的 SHA-256、行数和测试数量；不得使用本报告中的活跃快照替代。
5. `AutoCombatService.java` 仍为 SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`；如不一致，先回父级重新定界。
6. AT0、AT1 的测试与断言全部保留，不得改名、放宽、删除或改写为新的业务语义。
7. `TurnWindowRect`、AT1 helper 或 scripted port 若与本合同假设不一致，状态为 `BLOCKED`；External C 不自行 rebase、merge、扩卡或修改生产代码。

## 6. External C 的最小 test-only 写集

未来 AT2 固定卡应只允许写：

1. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\service\AutoCombatServiceTurnContractTest.java`
2. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34AT2.md`（append-only 交付记录）

明确只读：

- `AutoCombatService.java`
- `BattleRadarService.java`
- `BattleRadarTurnContractTest.java`
- 所有 `src/main/resources` 模板
- 所有 POM/构建文件
- 所有 task caller、runner、scheduler、recovery 和 maintenance 源码
- TURN-34A、AT0、AT1 卡及本 preflight

不得新增第二个测试类、生产 seam、resource、fixture 文件或生成图片输出。

## 7. 建议新增的两条测试

AT2 恰好新增两条 `@Test`。最终测试总数以父级接受的 AT1 数量 `N` 为准，AT2 预期为 `N + 2`，不能硬编码当前活跃快照的数量。

### 7.1 可读坐标确认退出

建议名称：

`twoCompletedFullRadarMissesAndReadableMinimapConfirmExitThroughReadOnlyProbe`

Arrange：

- 新建独立 harness，使用真实 `PackagedTemplateAssets`、真实 `BattleRadarService`、真实 `TurnGameClient` 和 scripted command port。
- 普通窗口矩形为 AT1 接受值 `left=100, top=50, width=1280, height=800`。
- `GameContext` 初始为 `IN_COMBAT`。
- 依请求 ROI 动态生成并排队 7 个有效 `COMPLETED` capture：
  - 第 1-6 帧均为合法 PNG、正确尺寸、全雷达 miss。
  - 第 7 帧为合法、可识别的坐标 ROI。
- 所有响应必须使用请求 actionId、windowId、region、sourceStepIndex、PNG bytes、width/height 和 SHA-256 形成完整关联；不能复用静态假响应。

Act：

- 在正确 `WindowTaskContextHolder` 绑定下，连续两次调用公开入口：
  - `probeWindowCombatStateReadOnly(context, "fivering")`

Assert，第 1 次调用后：

- 返回 `IN_COMBAT`。
- `GameContext` 仍为 `IN_COMBAT`。
- 恰好 3 个 action，顺序为 Stage1、Stage2、Stage3。
- 尚未请求 minimap/coordinate ROI。
- 没有 pending combat-exit signal。

Assert，第 2 次调用后：

- 返回 `NONE`。
- `GameContext` 更新为 `FREE`。
- 累计恰好 7 个 action；第 4-6 个仍为 Stage1、Stage2、Stage3，第 7 个才是 coordinate ROI。
- 真实 `BattleRadarService.consumeCombatExitSignal()` 第一次返回 `true`，第二次返回 `false`。
- 上一条断言证明 `probeWindowCombatStateReadOnly(...)` 没有偷消费 recovery signal；它不等于测试 recovery 执行。
- scripted reply 队列耗尽，且不存在第 8 个 action、自动重试或额外验证读取。

### 7.2 不可读坐标保持战斗态

建议名称：

`unreadableMinimapAfterTwoCompletedMissesKeepsInCombatWithoutExitSignal`

Arrange：

- 使用新的独立 harness，初始 `IN_COMBAT`。
- 排队 7 个正确关联的 `COMPLETED` capture，全部为合法尺寸的 blank/raw PNG；第 7 帧没有可识别坐标结构。

Act：

- 在正确窗口绑定下连续两次调用同一公开只读入口。

Assert：

- 两次均返回 `IN_COMBAT`。
- 两次后 `GameContext` 仍为 `IN_COMBAT`。
- pending combat-exit signal 为 false。
- 恰好执行 7 个 action，顺序与 ROI 完全匹配；无第 8 个 action、重试或额外读取。
- scripted reply 队列耗尽。

## 8. 七帧精确顺序与 ROI

以 `TurnWindowRect(100, 50, 1280, 800)` 为唯一接受前提，七次 capture 的 screen-absolute ROI 必须是：

| # | 语义 | ROI `(x,y,w,h)` |
|---:|---|---|
| 1 | 第一轮 Stage1 battle flag | `(1074,680,51,20)` |
| 2 | 第一轮 Stage2 summon/withdraw | `(1027,352,100,225)` |
| 3 | 第一轮 Stage3 anger/origin | `(556,112,123,39)` |
| 4 | 第二轮 Stage1 battle flag | `(1074,680,51,20)` |
| 5 | 第二轮 Stage2 summon/withdraw | `(1027,352,100,225)` |
| 6 | 第二轮 Stage3 anger/origin | `(556,112,123,39)` |
| 7 | coordinate/minimap scan | `(146,109,178,35)` |

第一轮全 miss 只将连续 miss 计数推进到 1，不得提前读取坐标图或退出。第二轮全 miss 达到 `REQUIRED_COMBAT_EXIT_MISSES=2` 后，才允许第 7 次读取。

## 9. 每个 action 的协议验收

两条测试都应复用 AT1 已接受的 action/result/raw-frame 断言，不另造宽松断言。对每一个 action 至少证明：

- actionId 可由 `UUID.fromString(...)` 解析。
- 同一测试内 7 个 actionId 两两不同。
- window/device 与 harness 绑定一致。
- `contractVersion=1`。
- command 只有一个 index `0` 的 `CAPTURE` step。
- delivery mode 为 `UPLOAD_IMAGE`。
- `input`、match/local service、pointer clear、pixel probe 等非 capture 字段为空。
- `fullWindowFailureEvidence=false`。
- command timeout 精确为 `120s`。
- result outcome 为普通 `COMPLETED`，result actionId/windowId 与请求一致。
- raw frame 的 actionId、windowId、region、sourceStepIndex=`0`、width、height、PNG bytes 和 SHA-256 全部与该次请求对应。
- 每个 PNG 均可解码，尺寸等于请求 ROI；不能只检查魔数或非空字节。
- command port 的 execute call、action 记录和 timeout 记录均恰好为 7。

本片不注入任何 terminal/uncertain response。AT1 已拥有首 capture terminal matrix；AT2 若重复该矩阵会把小片重新膨胀为大卡。

## 10. 可读坐标 fixture 合同

可读 coordinate ROI 必须在内存中构造，不新增或改写资源：

- 背景使用不会误命中模板的暗色/黑色像素。
- 左右方括号可用明确的白色几何线段构成。
- 数字和逗号必须读取现有 committed `coord/1.png`、`coord/2.png`、`coord/comma.png` 的真实像素后贴入 ROI。
- 可采用已核对的安全布局：左括号约 `x=60`，`1` 约 `x=68`，逗号约 `x=86`，`2` 约 `x=94`，右括号约 `x=114`；纵向位置按真实模板高度放入 178x35 ROI。
- 不使用系统字体、不自画数字、不改 threshold、不修改模板、不写 marked output 文件。
- 目标只需形成真实可读的 `[1,2]` 结构；本片不测试坐标值业务用途。

如现有 AT1 harness 尚未保留真实 `BattleRadarService` 引用，可在同一测试类的 `Harness` record/class 中增加该 collaborator 字段，以便断言 pending exit signal。不得用 reflection 访问 production 私有字段，也不得新增 wrapper 链。

## 11. 明确排除项（AT3+）

以下内容不属于 AT2，External C 遇到时应停下并回报父级，不得顺手加入：

- Stage2/Stage3 positive hit 的新增矩阵。
- 后续 stage 的 terminal/uncertain、错 actionId、错 windowId、坏 PNG、坏 SHA 或 region mismatch 矩阵。
- `handleCombatTick(...)` 对 exit signal 的消费、战斗前任务恢复或 recovery failure。
- `+4s`、`40s`、`30s`、`10s` 等 wall-clock/timing 规则。
- CommonBox、面板、急救、香、FAST `15/1/4`、维护调度。
- 五倍、修罗、五环、大雁塔等业务 task caller 的新增测试。
- TTL、额外验证/read、retry、park/yield、cleanup、fail-closed 或新 cloud gate。
- Mockito/Spring context、sleep、真实网络、真实云端、runtime、物理输入、截图落盘。
- 对 `docs/业务逻辑.md` 基线的任何业务差异。

业务口径保持：`无已批准业务差异；按基线等价迁移`。

## 12. External C 交付与父级验收

External C 交付卡必须记录：

- AT1 接受锚点与 claim 时间。
- 最终测试文件 SHA-256、行数、测试数量。
- 实际仅改动上述两项写集的证据。
- 两条新增测试名称及各自覆盖的退出分叉。
- 七帧 ROI/action/UUID/correlation 断言均已实现。
- 明确声明生产源码、资源、POM 和其它测试未改。
- `TRUE_EOF` 尾标。

父级在 writer 完整释放后，才可对稳定源码执行命名测试：

```powershell
Set-Location D:\mavenProject\dhxy-cloud-brain
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

本 Internal helper 没有运行该命令，也没有运行任何 Maven、runtime 或 input。命名测试通过仍只是父级验收证据，不代表本 helper review/批准。

## 13. Readiness 判定

AT2 合同已经可以冻结，但当前状态是**条件就绪**：必须先取得 AT1 最终接受锚点与 owner release。满足前置条件后，External C 可用“同一测试类新增两条测试、零生产改动”的小片直接开工；任一锚点漂移或写锁未释放则立即 `BLOCKED`，回父级重新定界。

TRUE_EOF PRECHECK_COMPLETE
