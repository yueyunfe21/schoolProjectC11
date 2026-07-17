# CR271 TURN-35 post-BP2 source-start dependency delta helper R1

> 日期：2026-07-16 EDT  
> 角色：CR271 Internal 只读 helper；非 implementation owner、reviewer、approver 或父级  
> 唯一写项：本报告  
> 业务权威：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`  
> 快照截止：`2026-07-16T12:16:12-04:00`

## 1. 边界与核验口径

本轮只核实 TURN-35 在 BP1 双独立审查完成、BP2 provisional source-active 后的 source-start 依赖增量。
未修改 Java、test、TURN card、权威计划、`ACTIVE_WORK.md`、dashboard、POM、resource 或协议；未运行
Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，也未调用 Git。

冲突解释顺序固定为：

1. `AGENTS.md`、`docs/DHXY_CONTEXT.md`；
2. 权威计划第 14-19 节；
3. HTTPS turn thin-client protocol；
4. `docs/业务逻辑.md` 与 `696a12b0`；
5. fixed card 的最新 physical true EOF、canonical delivery 与 parent receipt；
6. 当前物理源码、测试、行数、SHA 与实际 public caller surface；
7. readiness/helper 只提供检查口径，不替代 fixed card 或 parent receipt。

本报告不把 final independent review、named test 或 build 自动提升为互斥 prerequisite 的 source-start 门；
同时也不把“文件不重叠”当成越过真实 API、parent source receipt 或 fixed-card claim 的理由。

## 2. 已完整核对的权威材料

已读至各文件物理末行：

1. `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\ACTIVE_WORK.md` 顶部 CR271。
2. `docs\superpowers\plans\2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
3. `docs\superpowers\specs\2026-07-15-https-turn-thin-client-protocol-design.md`。
4. `docs\业务逻辑.md` 全文，重点为五倍、local-team、CommonBox、Summon、STOP/terminal 与
   `696a12b0` 保持规则。
5. TURN-34A、TURN-34AT1、TURN-34B、TURN-34BT1、TURN-34BP1、TURN-34BP2 fixed cards；BP3 与
   TURN-35 fixed card 当前不存在。
6. BP1 delivery/Repair #2、两份 independent review、build-gate preflight；BP2 readiness、delta、test/
   acceptance 与 delivery/source-review preflight；BP3 readiness、post-BP2 readiness 与 fixed-card freeze
   preflight；34A AT3+ 与 34B post-BP3 whole-card readiness；TURN-35 三份既有 readiness 报告。
7. TURN-35 全部直接 `startDependsOn` 的最新 parent/child true-EOF 证据，及 TURN-27、TURN-28 最新
   readiness/decomposition 证据。
8. 当前 Cloud `WubeiTask.java`、`TaskExecutionContext.java`、`TaskMaintenanceService.java`、
   `AutoCombatService.java`、`NpcClickService.java` 和对应现有/缺失测试；相关 caller/字段/API 搜索结果。

所有 BP1/BP2/BP3/TURN-35 helper 报告均以各自 `TRUE_EOF` 尾标核对；本报告只采用后来事实覆盖旧快照，
不把旧 helper 的状态文字当成当前裁决。

## 3. 两仓只读身份与当前物理锚点

### 3.1 Repository identity

按本轮禁令没有运行 `git status`。branch/HEAD 只通过 `.git/HEAD` 与对应 ref 文件只读确认；dirty 数量采用
当前材料中最近的完整 status 快照，不能冒充本轮实时 Git 结果。

| Repo | `.git/HEAD` / ref 当前只读值 | 最近完整 status 证据 |
|---|---|---|
| `D:\mavenProject\DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 11:51 build preflight：740 项，43 modified、1 deleted、696 untracked |
| `D:\mavenProject\dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 11:51 build preflight：550 项，9 modified、541 untracked |

两仓全部既有 dirty/untracked 都是受保护输入；本报告没有清理、恢复、覆盖、stage 或提交任何字节。

### 3.2 当前关键 artifact

| Artifact | 当前物理事实 | 依赖含义 |
|---|---|---|
| BP1 `TaskExecutionContext.java` | 527 行 / 22,204 bytes / SHA `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` | Parent Review #3 source passed；R1/R2 最新轮均明确通过；build pending |
| BP1 named test | 872 行 / 43,936 bytes / SHA `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` | 与双 reviewer 所审字节一致；不属于 BP2/BP3/TURN-35 写集 |
| BP2 `TaskMaintenanceService.java` | 1290 行 / 69,169 bytes / SHA `83431ed18ea7db427f765ec192cb8bc81cae2c45e6c499eb5e06e8d08242ab8c` / mtime `12:15:40` | 相对 1224 行 / `963b028c...` 有真实且持续变化的 WIP；无 canonical delivery/parent receipt |
| BP2 card | claim 正文存在，但 claim 段后仍无规范 `TRUE_EOF`；无 delivery | 按 provisional sole writer 保护；不能供 BP3 冻结 |
| BP3 fixed card | 不存在 | 不能 claim；实际 BP2 final type/SHA 尚未知 |
| `TaskMaintenanceTurnContractTest.java` | 不存在 | 34B sole test 尚未开写 |
| `WubeiTask.java` | 4329 行 / 243,798 bytes / SHA `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7` | TURN-31 后稳定 current bytes；未来必须原地增量编辑 |
| `WubeiTaskTrackerTurnContractTest.java` | 830 行 / SHA `5514fa3cded30f4c1daddde5bb06ae5ee3454d79f4256a4cbf61ae02fc5a097c` | TURN-31 所有，TURN-35 只读 |
| `WubeiWholeTaskTurnContractTest.java` | 不存在 | TURN-35 唯一 future named test |
| TURN-35 fixed card | 不存在 | 当前没有 claim surface |
| `AutoCombatService.java` | 852 行 / SHA `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` | production API 已 source-reviewed/frozen |
| AutoCombat named test | 1026 行 / SHA `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292` | AT1 最新 parent true EOF 已回到 Repair #3；AT2+ 仍未开 |

BP2 当前源码中四个 shared map 声明已经出现 typed 泛型，但部分方法仍保留旧 `String` key、prefix parse 和
`Set<String>` 调用形态。这只证明文件处于中间编辑态；本 helper 不把它写成 BP2 finding、delivery 或 final type
合同，也不让 BP3/TURN-35 按这些暂态名称接线。

## 4. TURN-35 权威 `startDependsOn`

第 16.2 节固定的直接集合保持不变：

```text
TURN-35 S = 13C + 14 + 15 + 21 + 22 + 23 + 26 + 27 + 28 + 31 + 34A + 34B
```

BP1/BP2/BP3 是 TURN-34B 内部 source-release 链，不应伪造成 TURN-35 新的平级直接依赖；Bag/entry 缺口也
不能由 helper 擅自创造一个新编号。它们必须在所属 predecessor 或未来 TURN-35 fixed card 中以真实接口决议闭合。

### 4.1 Latest source-start matrix

| Direct S | 当前 source 事实 | 对 TURN-35 source-start 的解释 | final gate 分离 |
|---|---|---|---|
| 13C | context source/test-source parent passed | public context source 可消费 | named test/compile 仍独立 |
| 14 | source/test-source passed | ReturnItem prescan client surface 可消费 | named test/build pending |
| 15 | source/test-source passed | UI cleaner surface 可消费 | build pending |
| 21 | parent source/test-source passed | CommonBox source surface可消费 | named test/build pending |
| 22 | C1/D1 子片已有稳定审查证据，但 parent true EOF 仍 `PARENT-REPAIR3-PENDING` | 子片不能由 helper提升成 parent source receipt；需 parent aggregate | review/build 不应阻止已聚合后的 disjoint consumer start |
| 23 | source/test-source passed | Bag/first-aid已有 API 可只读消费 | final build pending；但未解决的 uncached scan-and-use 仍需所属接口决议 |
| 26 | source/test-source passed | Dialog option/OCR surface 可消费 | named test/build pending |
| 27 | 无 fixed card、无 delivery/source receipt；且真实依赖 TURN-28 final recognizer/NpcClick API | 真实 API predecessor 尚未落盘，不能猜 Navigation return/action surface | 无 final 证据 |
| 28 | parent 只完成小片分解；whole NpcClick source/test 未通过 | Wubei 六个 NpcClick caller 不能按中途 S2/S3 shape 接线 | whole test/review/build 均待 |
| 31 | parent source/test-source passed | 当前 Wubei tracker caller必须保留；未来先复核 `dfde0ad0...` | build pending，不阻止同文件后续正式 owner在准确 SHA 上接续 |
| 34A | production `532e6f84...` 已 parent source-reviewed/frozen；当前债务在 test-only AT1 Repair #3 与 AT2+ | AutoCombat production API 本身可只读消费；父级仍应在 TURN-35 卡明确记录采用该 production receipt，不能把 test/build 债误写成 API 不存在 | 34A whole test/review/build 仍是 final 债务 |
| 34B | BP1 source + 双审已闭合；BP2 active，BP3/后续 whole source/test 未开 | TaskMaintenance final source semantics 尚未形成；必须走 BP2 -> BP3 -> 34B parent receipt | BP1/34B independent review与build属于各自 final gate，不替代 source receipt |

### 4.2 当前仍需真实 source evidence 的门

1. TURN-22 parent aggregate source receipt。
2. TURN-28 whole source surface，然后 TURN-27 fixed card、delivery 与 parent source receipt；二者顺序不可倒置。
3. TURN-34B 的 BP2 canonical delivery/parent receipt、BP3 canonical delivery/parent receipt，以及 post-BP3
   whole production source receipt。
4. Bag uncached “scan and actually use” closed route：当前 `WubeiTask` 仍调用
   `findAndUseItemFromBack(...)` 与 `findAndUseMainBagTaskPageItem(...)`，现有 ReturnItem intent 不能由 Task
   worker擅自扩成新 protocol/client API。
5. Wubei public entry：当前 `execute()` -> `execute(null)`，而 `resolveExecutionContext(null)` 仍调用已不存在的
   `TaskExecutionContext.builder()`；current BP1 context 只有真实 `turnNative(...)` factory。父级必须冻结 no-arg/
   nullable entry 行为，不能由实现者补 builder shim、manual context 或第二 client facade。
6. 父级创建 TURN-35 fixed card，复制所有 accepted source SHA、exact terminal mapping、initial Wubei SHA、
   exact write set 与 owner gate。

## 5. BP1 -> BP2 -> BP3 -> 34B 的正确 source-release 链

```text
BP1 parent source+test-source receipt                         [已存在]
  + BP1 R1/R2 latest-round independent review               [已存在]
  + BP1 build                                                [final-only pending]
        |
        +--> BP2 source-start                                [已发生，provisional active]

BP2 canonical delivery + parent-recomputed source receipt + owner release
        |
        +--> parent 用实际 BP2 final SHA/private types 创建 BP3 fixed card
              |
              +--> BP3 one-file claim/delivery + parent whole-source receipt + owner release
                    |
                    +--> 34B sole named-test serial tranches / parent whole-source+test-source receipt
                          |
                          +--> TURN-35 对 34B direct-S 的 source satisfaction 才可由父级落卡
```

### 5.1 不应错误等待的门

1. BP1 build pending 不阻止 BP2 source-start；BP1 与 BP2 生产写集互斥，父卡已明确开放该并行关系。
2. BP2 final independent review/build 不应自动阻止 BP3 source-start。现有 BP3 freeze 条件要求的是 BP2
   canonical delivery、parent source receipt、exact final SHA/types 与同文件 owner release；若 parent source review
   发现需返修，则自然没有可用 receipt。
3. BP3 final independent review/build 不应自动阻止 post-BP3 34B test/source consolidation。后者必须绑定 BP3
   parent-received final SHA，但不应等待一个只属于最终批准的无关 build lock。
4. 34B final two-reviewer/build 不应自动阻止文件互斥的 TURN-35 source-start；前提是 34B parent 已明确接收
   post-BP3 production source、所有 direct consumer API 已冻结且没有待返修项。最终门仍必须在 TURN-35/34B
   各自收口时完成。

### 5.2 绝不能越过的门

1. BP2 与 BP3 写同一个 `TaskMaintenanceService.java`，所以 BP3 不能在 BP2 owner release 前 claim。
2. BP3 不能使用 `963b028c...`、`3a86f36d...` 或任意 helper-observed WIP SHA；只能使用 BP2 parent receipt 的
   exact final SHA 和实际 private type 名。
3. 34B sole test不能在 BP3 source receipt 前按旧 `963b...` 建 fixture，也不能猜 future generation types。
4. TURN-35 不能把 BP1 source pass误当成整个 34B source pass；Wubei 消费的是 post-BP3
   `TaskMaintenanceService` 语义。
5. TURN-27/28 的 API 依赖是实际源码调用关系，不因它们与 Wubei 文件不重叠而消失。

## 6. 同文件 owner 与写集冲突矩阵

| Artifact | 当前/未来 owner 事实 | TURN-35 约束 |
|---|---|---|
| BP1 `TaskExecutionContext.java` + named test | implementation owner released，双审固定，build pending | TURN-35 全只读；不得补 builder、factory、client 或 context shim |
| `TaskMaintenanceService.java` | External C 是受保护 provisional BP2 sole writer；BP3 与 34B production都写同文件 | 严格 BP2 -> BP3 串行；TURN-35 只读消费 final receipt |
| `TaskMaintenanceTurnContractTest.java` | 当前不存在；post-BP3 唯一 serial test owner | 不属于 TURN-35 写集；不得提前建壳或并发 test tranche |
| `AutoCombatService.java` | production frozen，无当前 writer | API 可读；TURN-35 不改它 |
| `AutoCombatServiceTurnContractTest.java` | AT1 Repair #3 等 fresh test-only owner，后续 AT2+ 同文件串行 | 与 TURN-35 文件不重叠；其 test/build 不应单独挡 consumer source-start，但 final gate独立保留 |
| `NpcClickService.java` / TURN-28 files | 28 whole source仍按子片推进 | 无文件重叠但有真实 API依赖；TURN-35 不猜、也不回写 |
| TURN-27 Navigation四 production + sole test | fixed card未创建 | 无文件重叠但依赖 TURN-28 final recognizer/API；先 28 后 27 |
| `WubeiTask.java` | 当前无 active writer；TURN-31 曾写同一文件并已释放 | future TURN-35 唯一 production writer须从 `dfde0ad0...` 原地增量，保留 TURN-31 |
| `WubeiTaskTrackerTurnContractTest.java` | TURN-31 frozen test | TURN-35 永久只读，不得复用为 Whole Task test |
| `WubeiWholeTaskTurnContractTest.java` | 当前不存在 | future TURN-35 唯一 named-test writer；始终单 owner |
| Cloud build cohort | BP2 等 Java writer 活动中 | build lock只延后命令执行，不改写各 prerequisite 的 source-start DAG |

当前 `WubeiTask.java` 物理上没有 owner冲突，但这只说明文件 lane 空闲，不构成 source-start 授权。

## 7. TURN-35 exact future write set

### 7.1 Java/test write set

未来 TURN-35 implementation Java 写集恰为：

1. Modify only  
   `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java`
2. Create/modify only  
   `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\task\wubei\WubeiWholeTaskTurnContractTest.java`

必要 DTO 只能是 `WubeiTask.java` 文件底部的 private nested record/enum/class，且必须代表真实 distinct boundary；
不得新增第三个 Java、test helper、fixture/resource、POM dependency、production hook 或 wrapper ladder。

### 7.2 Process evidence

父级未来创建的
`D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-35.md`
可由被授权 worker按卡片规则 append claim/delivery/return 证据。Parent 创建/冻结卡不属于 Java 写集；本 helper
不能代建、代 claim 或把本报告改造成卡片。

### 7.3 全部只读

`WubeiPhase`、`WubeiRoundContext`、`WubeiStepOutcome`、`WubeiWaitSpec`、`WubeiWaitReason`、
`WubeiDialogCatalog`、TURN-31 tracker test、全部 Navigation/NpcClick/Dialog/AutoCombat/Bag/ReturnItem/
PlayerState/TaskMaintenance/CommonBox/TeamReturn/UI/TaskTracker Service/model、TaskExecutionContext、TurnGameClient、
protocol/executor/queue、POM/config/resources 及 DHXY Java 全部只读。

## 8. 当前 Wubei 实际源码依赖面

当前稳定 Wubei source仍直接包含：

| Surface | 当前调用事实 | 未来含义 |
|---|---|---|
| `InputSequences` / `InputAction` | Alt+C、prepared-dialog click、tracker-green click、黄袍 cached click；字段/调用仍存在 | 每处按 baseline exact order迁成 closed turn，不能拆 move/click、不能藏在 wrapper后继续 old queue |
| Bag | probe item `findAndUseItemFromBack`；uncached return `findAndUseMainBagTaskPageItem` | 在 predecessor给出真实 closed scan-and-use API前不能猜 intent或删 fallback |
| direct tracker/capture/OCR | destination hint直接 refresh/base rect/captureToFile/yellow wash/local OCR | 必须按 raw PNG one-frame turn 与 Cloud decision边界迁移；不能保留 local business OCR |
| Navigation | `navigateToNPC`、`navigateInCurrentMap` | 等 TURN-28 后的 TURN-27 final API；不复制 route/candidate/fallback |
| NpcClick | pending confirm、smart click、direct combat共六 caller | 等 TURN-28 final public shape；不复制 Ctrl/Alt/tooltip/FIFO/verifier |
| AutoCombat | initialize、tick、wake、trusted probe、recovery多处 | production API可读；不借 TURN-35重写 terminal/recovery |
| TaskMaintenance | initialize、round/window/capability/opportunistic maintenance共 16 次引用 | 等 post-BP3 parent source receipt；不按 BP2 WIP type接线 |
| context entry | no-arg -> null -> removed `builder()` | 父级必须冻结 strict turn-native/no-arg行为；禁止兼容壳 |

HTTPS 协议可提前固定的一般事实仍是：只允许 `CAPTURE/MATCH_TEMPLATE/INPUT/WAIT/LOCAL_SERVICE`；一次显式
invocation一枚新 UUID/一个 command、最多一张 raw multipart PNG；failed step 后续 `NOT_RUN`；STOPPED/
duplicate-or-uncertain不自动 retry/replay/resend。

## 9. 可提前冻结的内容

以下内容不依赖 future private API 名称，可由 parent 在 TURN-35 fixed card 前先整理为冻结输入：

1. 权威 direct `S=` 集合、source receipt 与 final gate分栏，不把 build pending写成 source API缺失。
2. 当前 `WubeiTask.java` 的 exact `4329/243798/dfde0ad0...` identity，以及 TURN-31 tracker test的只读身份。
3. exact two-Java-file future write set、private nested type限制和全部 predecessor只读边界。
4. 当前 direct mechanics/caller inventory：四组 direct input、两条 Bag use、destination-hint capture/OCR、六个
   NpcClick caller，以及 Navigation/AutoCombat/Maintenance调用面。
5. 已由业务文档批准的五倍 phase/order、普通怪三次真实 fallback execution、白龙马 probe/no-park、黄袍无固定
   战斗次数、首轮 full 后 fast-only、5 秒援助窗、180 秒 pre-battle budget、verified-home无 TTL、CommonBox优先。
6. 已知 action机械顺序可作为未来合同输入，例如 post-accept 非 startup-flying 分支为 `Alt+C -> WAIT 120ms`；
   这里只冻结 baseline order，不提前选择 terminal projection或代码接口。
7. Whole Task acceptance轴：`BC4+BASE+TASK+IMG+LS`、14 business states + FAILED/STOPPED、exact context、
   terminal/uncertain、park/yield、raw PNG、closed local Service、一 invocation一 UUID/command、零自动 retry。
8. Release receipt模板：每个 predecessor记录 canonical final SHA/lines/bytes、parent source receipt marker、owner
   release、实际 public signatures/types；TURN-35 claim时全部重新计算。
9. Future stable-writer命令身份可预先写卡，但命令只能在所有相关 writer释放后执行；预先写命令不等于执行结果。

## 10. 绝不能提前实现或猜测的边界

1. 不创建、修改或占用 `WubeiTask.java` / Whole Task test，直到 parent fixed card和真实 source gates齐备。
2. 不做旧 R2 提出的 E0 试切：即使 `Alt+C -> WAIT120` 顺序已知，nullable/no-arg entry、exact client取得、
   FAILED/STOPPED/uncertain对 public Task结果的投影仍需 parent按最新 context/protocol固定。
3. 不删除 `TaskExecutionContext.builder()` 调用后留下不可运行入口，也不增加 builder shim、manual
   `TaskExecutionContext`、manual `TurnGameClient`、第二 field/facade 或 same-scope wrapper。
4. 不按 BP2 当前 `ExecutionScope/Scoped*` 暂态名称写 BP3、34B test或 Wubei；actual final types只能来自 BP2
   canonical delivery + parent receipt。
5. 不在 BP2 owner仍活动时创建/claim BP3，不让 test writer顺手修 production，也不从旧 `963b...` 旁路叠写。
6. 不按 TURN-28 S2/S3/Q 中途 helper猜 NpcClick/action-list API；不按 TURN-27 readiness candidate猜
   Navigation result/action API。
7. 不扩 ReturnItem intent、Bag protocol/client/executor来制造 scan-and-use；不删 uncached fallback、不把 prescan
   当 use、不增加自动 retry。
8. 不把 direct capture/OCR换成 temp-file helper、第二 facade或一 turn多图；不以 `MATCH_TEMPLATE`替代未请求的
   ordinary OCR。
9. 不把普通怪 immediate green re-click、黄袍固定五次上限或其它 current dirty行为当新业务权威；也不脱离完整
   phase/budget/fallback合同做单点删除。
10. 不提前创建空 Whole Task test、source guard、reflection/private-helper test、恒真 fake或第二测试类。
11. 不为“更安全”新增 TTL、extra read/checkpoint/verification、retry、cleanup、park/yield、fail-closed business
    gate、session/owner/lease/ledger/queue/durable workflow。
12. 不运行 build来“探接口”；接口必须先由源码 receipt/fixed card形成，不能用编译错误反向猜设计。

## 11. BP2/BP3/34B 释放后的优先领取清单

以下是 parent 可采用的条件队列，不是本 helper 的派单、claim 或 owner声明。

### P0 - 当前 BP2 owner 窗口

1. 保护 External C 的单一 `TaskMaintenanceService.java` writer；不派第二人。
2. C 先补 canonical claim true EOF，再完成 BP2 canonical delivery或规范 owner return。
3. Parent只在 delivery后复算 final SHA/lines/bytes、实际四 shared map/types、19/5/6/4 surface和业务等价；
   中途 `83431ed1...`、`12edcb1b...` 及此前 `3a86f36d...` 均不进入后续 Frozen Inputs。

### P1 - BP2 release 后第一优先

1. Parent以 BP2 parent-received final SHA与实际 private types创建 TURN-34BP3 fixed card。
2. Fresh BP3 worker先复算同一 SHA，确认同文件零 owner，再 claim唯一 production + card写集。
3. BP3只完成四个 per-window map、exact native fingerprint/current-generation、A -> B -> A fresh cleanup；
   不等待 BP1 build，也不写 test/caller/context。
4. Delivery后 parent重算 BP3 final SHA并写 whole-production source receipt；owner立即释放。

### P2 - BP3 release 后第一优先

1. Parent对 post-BP3 `TaskMaintenanceService.java`做 whole-source reread，冻结最终 19/5/6/4 surface。
2. Parent以该 final production SHA重新冻结唯一 `TaskMaintenanceTurnContractTest.java` 的 replacement/serial
   tranche；测试始终一个文件、一个 owner，不能复活 pre-BP2 BT1 target。
3. Test/source tranches闭合 scope/session/claim、A-B-A generation、priority/delegate、terminal/UUID、`5/1/5/2`
   与 `696a12b0`；2h deterministic evidence需父级/用户先选择已批准路径，不能偷加 Clock/reflection/sleep。
4. Parent写 34B whole production/test-source receipt后，才把 34B direct-S source满足事实提供给 TURN-35。
5. BP1/BP2/BP3/34B独立 review和stable-writer build继续作为 final队列；只要没有 finding触发新字节，
   它们不回头阻塞已明确接收的互斥 source tranche。

### P3 - 与 P1/P2 可并行推进的 TURN-35 其它直接 prerequisite

1. TURN-28Q/S2/后续小片先完成 TURN-28 whole source/API；TURN-28 source receipt后再创建/claim TURN-27。
2. TURN-27按实际 TURN-28 recognizer/NpcClick API完成四 production + sole test source receipt。
3. TURN-22 parent聚合已审 C1/D1 字节，形成明确 parent source receipt；build留最终 cohort。
4. TURN-34AT1 Repair #3、AT2+ 与 whole test继续串行；对于 TURN-35 source-start，parent单独记录
   production `532e6f84...` 已冻结，不让 test-only/build gate伪装成 production API缺失。
5. Parent把 Bag uncached scan-and-use 与 Wubei no-arg/nullable entry放回所属 predecessor/fixed-card裁决；
   若需要新 public API或行为选择，先写 CR选项并获用户明确批准。

### P4 - 34B source release 后的 TURN-35 claim

只有同时具备以下事实时，TURN-35 才排到领取队列首位：

1. 全部直接 `S=` 均有 parent-recorded source satisfaction；尤其 22、27、28、34A production receipt、34B。
2. Bag scan-and-use与 Task entry合同有真实 public source或父级明确决议，不存在接口猜测。
3. 当前 Wubei SHA仍等于 fixed card initial SHA，TURN-31 caller未漂移，同两个 future Java/test路径零 owner。
4. Parent已创建 TURN-35 fixed card，写明 exact two-file write set、terminal matrix、业务基线、source SHA与
   claim-time recompute。
5. 一个 implementation worker一次 claim并完成 whole Task source + sole WholeTask test；不并发拆成两个 writer，
   不用 E0半成品替代完整交付。

## 12. TURN-35 后续 final gate（不反向污染 source DAG）

TURN-35 source交付后仍需：parent whole-source review、parent whole-test-source review、适用的两个 independent
latest-snapshot reviews、`WubeiWholeTaskTurnContractTest` fresh command、Cloud mandatory compile、Foundation
T01/T02/T03/T04 与 parent final judgment。任何 review finding导致源码变化时，旧 SHA证据失效并重走对应 receipt。

这些门决定最终交付，不应被提前用来阻止 BP2/BP3/34B、TURN-28/27或其它文件互斥 prerequisite 的合法
source-start；相反，没有真实 source receipt、实际 public API或 fixed-card claim时，也不能以“最终以后再测”为由
提前写 Wubei。

## 13. 本轮事实结论

1. BP1 增量只改变 final-gate事实：parent source pass + R1/R2 双独立通过已存在，build仍待；它没有增加 BP2
   source-start等待项。
2. BP2 已有真实 production进度，但仍是 provisional active WIP；其当前 SHA、类型与未闭合调用点都不能成为
   BP3或 TURN-35输入。
3. BP3 的最早合法起点是 BP2 canonical delivery + parent receipt + owner release；不是 BP1 build，也不是任何
   helper-observed WIP。
4. 34B 的最早 post-BP3收敛点是 BP3 parent whole-source receipt；final review/build随后独立完成。
5. TURN-35 future写集仍精确为 `WubeiTask.java + WubeiWholeTaskTurnContractTest.java`，但当前尚缺
   TURN-22 parent aggregate、TURN-28 -> TURN-27、post-BP3 TURN-34B、Bag scan-and-use、entry合同与 fixed card。
6. 因此当前可做的是冻结事实、SHA、业务矩阵与领取条件；不能提前实现、建测试壳、猜接口或形成 owner。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

TRUE_EOF PRECHECK_COMPLETE
