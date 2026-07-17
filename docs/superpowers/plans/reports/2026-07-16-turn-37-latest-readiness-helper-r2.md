# CR271 / TURN-37 Latest Readiness Refresh R2

> 角色：CR271 Internal helper，仅做 TURN-37 latest readiness refresh。本文不是 review、批准或开工授权。
>
> 快照时间：2026-07-16 10:06:46 -04:00。
>
> 本轮边界：只读源码、卡片、计划、协议、业务基线与两仓状态；未运行 Maven、JUnit、runtime、input，未执行任何 Git mutation，未修改 Java。

## 1. 本轮读取的权威材料

- `AGENTS.md`
- `docs/DHXY_CONTEXT.md`
- `docs/ACTIVE_WORK.md` 顶部 CR271 最新区段
- `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- `docs/业务逻辑.md` 中修罗确认的 pre-cloud 基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- TURN-37 既有 readiness 报告，以及全部直接 `startDependsOn` 卡和其当前相关子卡，均读到物理 EOF
- DHXY、`dhxy-cloud-brain` 两仓只读 `status`、分支、HEAD 与当前 `XiuluoTaskV2.java`

权威计划给出的 TURN-37 直接源码启动集合为：

```text
S = 13C + 14 + 15 + 17 + 21 + 22 + 23 + 26 + 27 + 28 + 30 + 34A + 34B
```

计划登记行当前仍是 `TURN-37 | PLANNED / LATEST READINESS ACTIVE`，尚未出现父级 `READY`、冻结后的实现卡或合法 owner claim。

## 2. 两仓与当前 Task 源码快照

### DHXY

- 分支：`thin-client-design`
- HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- 工作树：44 个 tracked change、41 个 untracked porcelain entry；不是 clean tree
- 根仓 Task：`src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- 状态：tracked modified，7176 行，438456 bytes
- SHA-256：`C34B92980E66ABC98676F414CF21451907C528713751ACC159564658702F647E`
- 相对 `696a12b0`：约 3349 insertions / 378 deletions，包含后续本地/迁移改动，不能作为 TURN-37 覆盖写入源，也不能在本卡中编辑

### dhxy-cloud-brain

- 分支：`navigation-migration`
- HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- 工作树：9 个 tracked change、19 个 untracked porcelain entry；不是 clean tree
- Cloud Task：`src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- 状态：当前为 untracked，4225 行，244508 bytes
- SHA-256：`46F9665999F644BE63B7F27E772429E68190322FBDE487641CBEFF0F747F519A`
- `696a12b0` 同文件 blob SHA-256：`B6DFA9F5F6F9B22DA853F3AA57CAF16E48511303BD379C38BB68F1CDD39CF3D8`
- 当前 Cloud Task 相对基线约 58 insertions / 38 deletions；主要是 TURN-30 typed tracker 迁移，包括 `TaskExecutionContextHolder.callWith(...)`、typed tracker panel 读取与异常完成传播。未来 TURN-37 必须从当前 Cloud bytes 增量编辑，不能用根仓文件或 `696a12b0` 整体覆盖
- 唯一计划测试 `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java` 当前不存在；Cloud `.gitignore` 第 15 行忽略 `src/test/`，后续 retained test 需要由父卡明确处理留存证据

## 3. 直接 startDependsOn 最新卡 true-EOF 快照

这里把“production/source surface 已可供映射”和“该依赖卡的正式门禁已闭合”分开记录。前者不自动等于 TURN-37 可以开工。

| 依赖 | 最新物理 EOF / 卡面结论 | 对 TURN-37 source-start 的当前含义 |
|---|---|---|
| TURN-13C | 有 `TRUE_EOF`；父级 source/test-source 已通过，named test 与 Cloud compile 仍被共享债务阻塞 | production surface 可用于映射；final gate 未闭合 |
| TURN-14 | 最新正文后段写明 source/test-source `P0/P1/P2=0/0/0`，计划 registry 也登记 passed；但文件物理末尾没有最新 `TRUE_EOF`，旧 marker 停在更早位置 | 内容层 source surface 可用，但 latest-card strict true-EOF seal 有缺口，不能把形式证据写成完整通过 |
| TURN-15 | 有 `TRUE_EOF`；source pass | source surface 可用；final gate 仍按总计划检查 |
| TURN-17 | 有 `TRUE_EOF`；source pass | source surface 可用；final gate 仍按总计划检查 |
| TURN-21 | 有 `TRUE_EOF`；source pass | source surface 可用；final gate 仍按总计划检查 |
| TURN-22 | 父卡有 `TRUE_EOF`，但仍停在 C1 passed、D1 ready、Repair-3 pending 的旧聚合状态；D1 最新子卡已记录两名独立 reviewer `Approved`、`0/0/0`，build pending | C1/D1 source surfaces 已有最新通过材料，但父卡未追加 Repair-3 聚合 source-pass；严格 parent source-start 尚未闭合 |
| TURN-23 | 有 `TRUE_EOF`；source pass | source surface 可用；final gate 仍按总计划检查 |
| TURN-26 | 有 `TRUE_EOF`；source pass | source surface 可用；final gate 仍按总计划检查 |
| TURN-27 | 最新只有 readiness/precheck `PRECHECK_COMPLETE`；受 TURN-28 final API 与父卡 freeze 阻塞，没有 implementation card/source delivery | 未满足 source-start，属于硬阻塞 |
| TURN-28 | 父卡只确认 S1 source passed 并完成拆卡；whole source 未通过。S2 截至快照无 claim；QT1 已由 External A claim 并完成首次窗口增量，但仍非 delivery | 未满足 source-start，属于硬阻塞 |
| TURN-30 | 有 `TRUE_EOF`；父级 source/test-source passed，build pending | typed tracker production surface 可用且必须保留；final gate 未闭合 |
| TURN-34A | 有 `TRUE_EOF`；production parent-passed，read-only SHA `532e6f84...`；AT0 passed，AT1 已有 owner/首次增量，后续测试矩阵仍未结束 | production API 可供 source mapping；整卡 final gate 未闭合 |
| TURN-34B | 有 `TRUE_EOF`；当前 production WIP 记录 `P0/P1/P2=0/2/1`；BP1 replacement ready 但截至快照无 owner/claim | 未满足 source-start，属于硬阻塞 |

相关子卡占用补充：

- TURN-28QT1 已由 External A 持有并有真实首次窗口增量，不可再分配第二 writer。
- TURN-34AT1 已由 External C 持有并有真实首次窗口增量，不可再分配第二 writer。
- TURN-28S2 在本快照时仍没有 owner/claim。
- TURN-34BP1 在本快照时仍没有 owner/claim；其范围大于单 Task slice，涉及 exact native metadata fence。

## 4. Source-start 结论

### 当前已满足的部分

- 已有可供未来父卡做精确 caller mapping 的 production/source surfaces：TURN-13C、15、17、21、23、26、30、34A。
- TURN-14 的内容结论和计划 registry 支持其 source surface 可用，但 strict latest true-EOF seal 仍需卡片维护者补齐。
- TURN-22 的 C1/D1 最新子材料已达到 source review 通过状态，D1 已有 2/2 独立 reviewer approval；但 TURN-22 父卡尚未把 Repair-3 聚合为最终 source-pass。
- TURN-30 对当前 Cloud `XiuluoTaskV2` 的 typed tracker 修改已经存在，未来实现必须原样保留其行为和异常传播。
- TURN-34A production API 已由父级标记 passed，可用于只读 caller 设计；其测试/构建终态不因此被视为完成。

### 当前未满足的硬门禁

1. TURN-27 没有实现交付，Navigation final public API 未冻结。
2. TURN-28 whole source 未通过；TURN-28S2 尚未 claim/delivery，最终 NPC/recognizer raw-frame API 未冻结。
3. TURN-34B 仍有现存 P1/P2 卡面问题，TURN-34BP1 尚未 claim/delivery，`TaskExecutionContext` latest exact native metadata fence 未完成。
4. TURN-22 父卡没有把最新 D1 结果聚合为 Repair-3/source-pass。
5. TURN-14 最新卡物理 EOF 缺少与最新结论对应的 `TRUE_EOF` seal。
6. TURN-37 父级尚未发布 `READY`、冻结后的 implementation card、owner、exact caller mapping 和合法 claim 窗口。
7. 当前 Cloud Task 仍引用多项 Cloud 仓不存在的旧 collaborator，不能在上述 API 未冻结时形成可编译的一文件迁移。

**TURN-37 source-start 总结：NOT READY。** 当前只能继续前置依赖和父卡冻结工作，不能 claim 或编辑 TURN-37 Java。

## 5. Final gate 结论

权威计划规定 Whole Task 必须满足 `TASK + IMG + LS`，并按 strict `696a12b0` 保持消息、数量、顺序、阶段、fallback、keep-turn/park、重试和超时语义。其审批流水为：

```text
CLAIMED
-> SOURCE + TEST DELIVERED
-> PARENT SOURCE REVIEW
-> PARENT TEST REVIEW
-> named test exit 0
-> applicable compile exit 0
-> CARD APPROVED
```

当前 final gate 未满足项：

- 直接依赖集合尚未全部达到父级 source pass，更未全部 final。
- Foundation T01-T04 均没有完成全部要求：现有卡面仍分别存在 retained test、named test、Cloud/DHXY compile 或 Maven gate 未闭合项。
- TURN-37 尚无合法 claim、source delivery、test delivery、父级 source review、父级 test review。
- TURN-37 所需独立 reviewer gate 和父级最终判断均未发生；本文不构成其中任何一票。
- 唯一命名测试文件当前不存在，也没有 `exit 0` 证据。
- 当前 Cloud Task 不能据现状证明 compile，且本轮按边界没有运行任何 Maven/compile。
- 卡片没有 `Approved/Done` 最终结论。

**TURN-37 final gate 总结：NOT READY / NOT APPROVED。**

## 6. TURN-37 exact Task-only 未来写集

只有父级在全部 source-start 门禁闭合并发布冻结卡后，未来实现者才可 claim 下列写集。

### Production

唯一 production 文件：

```text
D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java
```

- 如确有 DTO 需要，只允许在该文件内部定义 `private` nested type。
- 不允许新增第二个 production 文件、公共 DTO、adapter、compatibility wrapper 或同义 helper 层。
- 基础必须是当前 Cloud Task SHA-256 `46F966...`，保留 TURN-30 typed tracker 改动；`696a12b0` 只作为业务行为权威基线，不是覆盖当前 Cloud 文件的拷贝源。

### Test

唯一 named test 文件：

```text
D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\task\xiuluo\XiuluoWholeTaskTurnContractTest.java
```

- 测试必须通过真实 public `execute(...)` / whole-task 入口驱动真实 `TurnGameClient` fake，不能直调 private helper、做源码字符串扫描或建立另一套任务模型。
- 测试必须覆盖计划冻结的 `TASK + IMG + LS`、ordered action steps、raw PNG frame、terminal mappings、strict call count/order 和代表性 failure/stop/park 语义。
- `.gitignore` 当前忽略 `src/test/` 是 retained-test 留存风险，需由父卡在合法实现窗口给出可审计处理；本 helper 不做 Git mutation。

### Process artifact

- 只允许父级创建/更新计划指定的 TURN-37 implementation card/report。
- 除上述 Task、named test 和父级指定卡片外，不得触碰 protocol、POM、config、resource 或其他 production/test 文件。

## 7. 当前 caller / API 碰撞清单

### 7.1 当前 Cloud Task 仍引用 Cloud 仓缺失的旧本地类型

只读解析显示至少下列 16 个 import 对应的 Cloud source 当前不存在：

```text
GameClientTracker
TextRecognizer
AutomationMetricsService
BagService
QuestManagerService
UICleanerService
TaskTransactionRunner
TaskTurnCoordinator
CoordinateHelper
GameStateUtil
ObjectiveTextRecognitionService
MultiWindowTaskManager
WindowReadyEventBus
WindowRuntimeContext
WindowScopedTempPath
WindowTaskContextHolder
```

TURN-37 必须在唯一 Task 文件内把这些 caller 映射到已冻结 Cloud TURN API 或删除不再属于 Cloud 的本地机制引用；不能通过把旧类复制进 Cloud、增加 shim 或扩展未来写集来消除编译错误。

### 7.2 `TaskExecutionContext` 直接签名碰撞

- 当前 Cloud Task 调用 `context.getWindowRuntimeContext()`。
- 当前 Cloud `TaskExecutionContext` 没有该方法。
- TURN-34BP1 负责 latest exact title/HWND/process/native metadata fence，尚未交付。
- 因此现在提前改 Task 会绑定临时或错误上下文 API；必须等待 34B 父级 source pass 和父卡给出 exact replacement mapping。

### 7.3 Quest / OCR / raw-frame 碰撞

- 当前 Task 的 quest/tracker OCR 路径仍期待 `QuestDetailCapture`、`BufferedImage` 或本地 path。
- typed Quest local-service client 返回的是 `CloudTurnFrame` raw PNG frame。
- HTTPS 协议禁止 Base64、共享路径、Cloud 读取 client 临时文件和 client-side business OCR。
- TURN-28 的 Cloud-side raw-frame recognizer/public API 尚未冻结；不得先写 reflection、`JsonNode`、Base64 或本地 OCR fallback。

### 7.4 UI、Bag 与 terminal mapping 不是机械改名

- 当前 Task 有约 20 个 `UICleanerService` 直接调用，以及 `BagService` 的 boolean/直接调用语义。
- TURN typed clients 使用 closed `TurnLocalOperation` 和 typed terminal outcome。
- 每个旧 caller 都需要父卡逐点冻结：请求 operation、source/phase/action slot、terminal 到原分支的映射、是否继续持有 task turn。
- 在 mapping 未冻结前把旧调用机械替换为 client call，可能改变原来的阶段、fallback、失败计数或 park 边界。

### 7.5 Navigation / NPC API 尚未闭合

- 当前 Task 仍调用旧 `navigateToNPC(...)`、`navigateInCurrentMap(...)`、`clickNpcSmart(...)`、`confirmPendingSmartClick(...)` 和 direct combat click。
- TURN-27 没有 implementation source；TURN-28 whole 也未 source-pass。
- NPC FIFO 必须严格保持 `MEMORY -> TOOLTIP -> YELLOW_NAME -> PURPLE_FORMULA -> CTRL_CANDIDATES -> END`，Cloud 决策，client 不得新增 fallback。
- 在 27/28 final API 冻结前，任何 Task caller 改写都存在签名和业务顺序双重碰撞。

### 7.6 Maintenance / combat / runtime ownership 碰撞

- TURN-34A production API 已稳定，当前 read-only SHA 为 `532e6f84...`，可以设计 mapping，但其测试矩阵未 final。
- TURN-34B 当前仍有卡面 P1/P2；Task 中约 10 个 `TaskMaintenanceService` caller 不能提前绑定 WIP generation。
- 当前 Task 仍有 `TaskTransactionRunner`、`TaskTurnCoordinator`、window event/runtime holder、temp path、direct `InputSequences` 与 tracker/capture 依赖；父卡必须明确每个 caller 是删除、改为 HTTPS action，还是由既有 Cloud coordinator 拥有。
- 不得用新 wrapper 链、负 ready-event 信号或 diagnostics 结果制造新的 business truth。

### 7.7 已稳定但必须精确保留的 API

- TURN-30 typed tracker path 与异常传播。
- 已有 typed Bag、UI、Quest、CommonBox、TeamReturn、PlayerState、Dialog client 的 terminal contracts。
- TURN-34A AutoCombat production API。
- HTTPS long-wait `/api/v1/client/turn`：一个 action、ordered steps、最多一个 raw PNG outcome frame；一个 invocation 对应一个 UUID；不自动 business retry。
- physical input 仍由 client input queue 串行化；Cloud 只声明 step，不能持有或绕过本地输入机制。

## 8. strict `696a12b0` 业务保持点

未来 TURN-37 只是 ownership/transport 迁移，除非有用户批准的独立 behavior CR，不得改变：

- 修罗 hot-start 优先级、shortcut/non-shortcut 路线和 prompt/NPC/dialog 判定。
- `MAX_PHASE_RETRY=1`、`MAX_RECOVERY=2`、连续失败阈值 10、loop guard `>32`、watchdog 180000ms。
- STOP 不计 business failure。
- return verify 2 次、间隔 500ms；handoff 900ms；wakeup clamp 500..10000ms。
- maintenance hooks 5 个；enter no-combat ticks 4、retries 2；team waits 3000ms。
- return-home verified task fact 无 TTL，仅在 accept option 真正点击后清除。
- NPC 严格 FIFO、verification count、click/navigation/OCR/fallback 顺序。
- keep-turn、park、yield、retry、expiry 和 cleanup 边界。

本轮未发现任何用户批准的业务差异：`无已批准业务差异；按基线等价迁移`。

## 9. 可拆给 External 的最小先行片

### 建议：只推进 TURN-28S2，不开 TURN-37 Java

当前最小且直接解除 `28 -> 27 -> 37` production 路径阻塞的 External 先行片，是已存在的 TURN-28S2：

```text
D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java
```

范围应严格保持子卡已写的四个顶层 shortcut，每个 shortcut 只提交一个 `TurnGameClient` action：

- generic `ALT_C`，随后 `WAIT 700ms`
- confirmed-flying `ALT_C`，随后 `WAIT 700ms`
- grounded `ALT_A`，随后 `WAIT 350ms`
- ordinary `ALT_4`，随后 `WAIT 400ms`

只允许该 production 文件和 TURN-28S2 子卡追加；截至本快照没有 owner/claim，可由父级分配一个 fresh External writer。该片完成后仍需 TURN-28 父级 review/聚合，不能自动授权 TURN-27 或 TURN-37。

不建议把 TURN-37 的 Bag/UI、tracker、test skeleton 或某个 phase 提前拆给 External，原因是所有有意义改动都落在同一个 Cloud `XiuluoTaskV2.java`，且会绑定尚未冻结的 TURN-27、TURN-28、TURN-34B API。提前拆分会造成同文件 writer 冲突、不可编译中间态或未批准的 terminal/phase 语义变化。

其他当前占用/候选关系：

- TURN-28QT1 虽是单测试文件，但已经由 External A 持有，不可重复分配，且它本身不补齐 TURN-28 production source。
- TURN-34AT1 已由 External C 持有，不可重复分配。
- TURN-34BP1 也能解除 34B 阻塞，但它是两文件 shared-context prerequisite，范围比 TURN-28S2 大，不是最小 production 先行片。

## 10. 明确禁止提前实施项

在父级明确写入 TURN-37 `READY`、全部直接 source-start gate 闭合、exact caller mapping 冻结并完成合法 claim 前，明确禁止：

1. 编辑或 claim 任一仓的 `XiuluoTaskV2.java` 作为 TURN-37 实现。
2. 编辑 DHXY 根仓 Task，或把根仓/`696a12b0` Task 整体覆盖到 Cloud；必须保留当前 Cloud TURN-30 增量。
3. 创建 TURN-37 named test skeleton、占位 fake、私有 helper 直调测试或源码扫描测试。
4. 为缺失类型复制 DHXY 类到 Cloud，新增 public DTO、compatibility shim、adapter、wrapper nesting 或第二个 production 文件。
5. 提前猜测 TURN-27 Navigation、TURN-28 NPC/recognizer、TURN-34B context/maintenance 的最终签名。
6. 让 Cloud 直接调用 `InputSequences`、tracker、window holder、temp path、本地 OCR 或本地文件路径。
7. 使用 Base64、reflection、`JsonNode`、共享文件系统或 client-side business fallback 绕开 raw PNG frame contract。
8. 合并 Navigation 与 NPC click ownership，改变 NPC FIFO，或新增 TTL、验证、retry、park/yield、cleanup、fail-closed、ready-event business gate。
9. 把 UI/Bag/Quest 旧调用机械改名而不冻结 typed terminal 到原业务分支的逐 caller mapping。
10. 在 TURN-28QT1、TURN-34AT1 等已有 owner 的文件上安排第二 writer。
11. 把既有 CR230 sidecar/session/ledger/soft-restart 方案带回当前 HTTPS TURN 路径。
12. 在当前并行写窗口运行 Maven/JUnit/package/runtime/input，或执行 add/commit/reset/checkout/stash 等 Git mutation。
13. 将本 helper 报告表述为 reviewer approval、父级 source pass、开工授权或卡片 Done。

## 11. 父级下一次可判定的检查点

父级只有在以下证据都写回各自主卡最新 EOF 后，才适合重新做 TURN-37 source-start refresh：

- TURN-14 最新结论对应的 physical `TRUE_EOF` seal 已补齐。
- TURN-22 父卡聚合 C1/D1 Repair-3 并明确 source-pass。
- TURN-28S2、TURN-28QT1 及其余 required children 完成，TURN-28 父卡冻结 final public APIs 并 source-pass。
- TURN-27 基于 TURN-28 final API 完成 source delivery/review，并冻结 Navigation API。
- TURN-34BP1 与后续 34B source repair 完成，34B 父卡 source-pass。
- TURN-37 父卡列出当前 Cloud Task SHA、唯一写集、逐 caller replacement 表、terminal mapping、业务等价声明、owner/claim 规则，并明确写入 `READY`。

即使届时 source-start 变为 ready，final gate 仍需独立完成 Foundation、TURN-37 source/test reviews、唯一命名测试、适用 compile 和卡片 `Approved`；fresh runtime 是后续独立运行验收，不替代上述源码/测试门禁。

TRUE_EOF PRECHECK_COMPLETE
