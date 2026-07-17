CR271 INTERNAL HELPER CLAIMED | agentId=`019f6acc-732b-7fa1-95fc-8b2460cd6148` | nickname=`Laplace` | role=`TURN-28 parent-freeze preflight helper` | claimedAt=`2026-07-16T08:20:21.3592036-04:00`

# CR271 TURN-28 Parent Freeze 696 Readiness PRECHECK

> 身份边界：我是 Internal helper，不是 implementation owner，不是 reviewer，不能批准、阻断、关闭或重开 TURN-28/TURN-28P。本报告只提供只读证据、依赖与建议，明确属于**非父级批准**。父级固定卡和父级状态记录高于本 helper 结论。

## 1. 只读证据快照

### 1.1 已完整读取的权威材料

| 文件与精确范围 | SHA-256 | mtime | 用途 |
|---|---|---|---|
| `D:/mavenProject/DHXY/AGENTS.md:1-392` | `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` | `2026-07-11T17:24:59.6508563-04:00` | 仓库规则、696 gate、no-local-test/compile、CR 卡和角色边界 |
| `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md:1-1349`，CR271 见 `:7-48` | `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` | `2026-07-16T03:32:25.4942933-04:00` | 当前 CR271 总上下文 |
| `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md:1-37` | `F17DC1659C897A227CA25E92E0081D0EE8DFD2D2CFC43F4B852B1F63A6A02318` | `2026-07-16T08:09:54.2102522-04:00` | 最新父级 source-start/final-gate 分层与 owner 状态 |
| `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:1016-1697` | `D9D65F476200E3C5DD281BD00C239F3954B2A77C18BDDADD41CB45F83D6C3CD8` | `2026-07-16T08:09:54.2122469-04:00` | 权威计划第 14-19 节完整正文 |
| `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:1-383` | `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` | `2026-07-16T03:13:40.4741505-04:00` | 最小 HTTPS turn、click timing、pixel probe、Cloud/DHXY 所有权 |
| `D:/mavenProject/DHXY/docs/业务逻辑.md:1-1426`，强制 gate `:215-224`，NPC FIFO `:1301-1426` | `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` | `2026-07-11T19:40:58.4813186-04:00` | 用户批准业务合同与 696 等价迁移边界 |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28.md:1-153` | `7BDE6F13486CE8531FCCE569CA47556342019754B29B8670C158EBC675AD5782` | `2026-07-16T08:08:31.3066923-04:00` | 当前父级固定卡及 External B true-EOF claim |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md:1-1058` | `0B232D3B3903068C6ACDF90DED2F3CFB2444A437FC47D4D7E7119C494CB3E153` | `2026-07-16T08:05:05.4191799-04:00` | TURN-28P Review #3、Repair #2 与 Euler claim |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-28-launch-preflight-helper-r2.md:1-464` | `AF43367D9F8EAFE4582A0C97C77760155736A9FCCD99671BFBAFF1828D5BF2A8` | `2026-07-16T05:11:16.6070894-04:00` | 最新 TURN-28 launch helper |

### 1.2 两仓 dirty/untracked 保护快照

| Repo | 分支 / HEAD | `git status --short --untracked-files=all` 只读快照 | 目标状态 |
|---|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | `2026-07-16T08:16:14.3244038-04:00`，659 行，`D=1/M=43/??=615`，状态文本 SHA-256 `851BED09984BB5DE7A6AE9F850DDEBFFEF4F694F9C15D23C15134A9968B24079` | TURN-28/28P 两卡、TURN-28P mechanics/test 均为既有 `??`；本报告当时不存在 |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | `2026-07-16T08:16:25.5633295-04:00`，550 行，`M=9/??=541`，状态文本 SHA-256 `E85B93E8CE8DD62EF5FA8D729E9E723F458BD4A9E8351081636D8DC2BD132C72` | `ObjectiveTextRecognizer`/`SmartClickRecognizer` 为既有 `M`；Cloud `NpcClickService`、turn protocol/client 为既有 `??`；named test 尚不存在 |

建议：所有实现必须在这些当前字节上增量编辑，不能用 HEAD、baseline mirror 或另一 worktree 覆盖 dirty/untracked；本 helper 未执行任何 Git mutation。

## 2. 当前父级状态和依赖

| 项目 | 精确证据 | 当前含义与建议 |
|---|---|---|
| TURN-23 / 24A / 26 | 权威计划 `:1135-1140` | 三项 source/test-source review 已通过，build 仍 pending；可作为 TURN-28 source dependency，不可冒充最终构建通过。 |
| TURN-28P production API | 权威计划 `:1276-1281`；当前 mechanics 见本报告第 4 节 | nullable click timing、single CAPTURE pixel probe 和 typed result 已落盘；其生产 API 与 TURN-28 Cloud 四文件写集互斥。 |
| TURN-28P 剩余测试 | TURN-28P 卡 `:620-690`、`:1028-1058` | Review #3 为 `0/2/1 / REPAIR #2 REQUIRED`；Euler 仅独占两个 DHXY real queue/worker contract test 和原卡，当前不是最终 source/test/build pass。 |
| TURN-22 frozen-executor integration | 权威计划 `:1134`、`:1269-1272` | 父级允许其 source-start，但 TURN-28 最终 integration/build 仍依赖该链闭合。 |
| TURN-28 固定卡 | TURN-28 卡 `:3-17`、`:19-34`、`:120-125` | 父级已明确 `SOURCE-START OPEN / FINAL INTEGRATION+BUILD GATED`，并选择 strict 696。 |
| TURN-28 owner | TURN-28 卡 `:127-153` | External B 已在 true EOF claim；本 helper 不是第二 owner，不得触碰其四文件或原卡。 |
| TURN-27 shared dependency | 权威计划 `:1143`；launch helper `:393-398` | TURN-27 等 TURN-28 final API；`ObjectiveTextRecognizer` 的 map/coordinate API 是共享边界。 |

## 3. 逐文件 696 对照

### 3.1 Cloud 当前 `NpcClickService.java` 对 DHXY `696a12b0`

| 版本 | SHA-256 / git blob | mtime / 行数 |
|---|---|---|
| Cloud current `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java` | SHA `F4E3842CDB5F59580D8F25F0191ADE4847BFE8CA6C7939AC73A70BD561BFD870`；blob `4d5339cc7b4c2836cc5461e911056d75938318b6` | `2026-07-15T03:48:56.8864033-04:00`；3406 行 |
| Cloud mirror `D:/mavenProject/dhxy-cloud-brain/migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java` | SHA `CCE8F0203AC90A0D39F7CFF99DDA8D9A616656A55467ED4AE3AA053AD0923441`；blob `74d9b26b76b84052718d5679529f7ffeb46e3273` | `2026-06-30T01:43:39-04:00`；3374 行 |
| DHXY commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/NpcClickService.java` | blob `74d9b26b76b84052718d5679529f7ffeb46e3273`，与 Cloud mirror byte-identical | commit object，不使用工作树 mtime |

下文 `BASE696:<line>` 唯一指向绝对文件 `D:/mavenProject/dhxy-cloud-brain/migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/NpcClickService.java:<line>`；该文件与 DHXY commit blob byte-identical。

只读 `git diff --no-index --numstat` 为 `34 additions / 2 deletions`。全部差异只在 normalized `sourceTask` pending-proof gate：

- Current `:66` 增加 `Locale`。
- Current `:2057-2188` 给 `PendingSmartClickEvidence` 增加 `sourceTask`、归一化和 equality gate；696 对应 `BASE696:2056-2164` 没有这些字段/条件。
- Current `:2294-2326` 在 token/option proof 前拒绝 null/blank/mismatched source；696 对应 `BASE696:2271-2295` 只检查 pending、proof token 和 option proof。
- 父级已在 TURN-28 卡 `:12-17`、`:82-83` 明确选择 strict 696，不保留该 gate。建议实施时删除/不迁移这项条件，同时保持六参数 public signature，因为 caller contract 与 696 本来就带 `sourceTask` 参数。

除该 hunk 外，当前 Cloud 文件与 696 blob 业务体相同。必须逐项保持的源基线位置：

- 原子 move/wait/click/hold、一次 verifier 与显式 retry：`BASE696:170-238`。
- dialog verifier 一读且只接受两状态、combat 四读四次 350ms false wait：`:241-301`、`:253-271`。
- Ctrl exact before/down80/move280/after/diff0.05/finally-up100 与 menu OCR/click：`:303-446`。
- `clickNpcSmart` 一次 pipeline，非 combat 恰一次 Alt+C/700 后第二次 pipeline：`:599-634`。
- direct combat flying gate、Alt+A/350、同 pipeline、三次 right-click exit：`:653-755`。
- 条件顺序、dialog gates、Wubei、TENTATIVE、yellow -> formula -> Ctrl：`:778-933`。
- tooltip 全部去重 hit 依序验证：`:1147-1260`。
- yellow region expansion、word center Y-50、800 + 一次 1000 retry：`:1933-2047`。
- formula固定变换、Y-50、1500、miss 后额外 1500 和 immediate SMALL_RING Ctrl：`:998-1050`、`:2865-3048`。
- pending evidence 无 TTL/sourceTask gate：`:1285-1400`、`:2056-2164`、`:2247-2305`。

建议：迁移可以改变 transport/ownership，但不能把上述条件、候选顺序、点击/验证预算、fallback、等待或 memory commit 条件改成 recognizer 或 mechanics 的新判断。

### 3.2 Cloud `ObjectiveTextRecognizer.java`（用户任务中的 `ObjectTextRecognizer` 实际文件名）

- 路径：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java`。
- SHA-256 `D3DC3CC247058AE85A6258E6173F8D9B56D7BE119443C90A24C4BF6F180F3FE1`；blob `ef9185772bb044aeb22443a64c40b6d5a20c6780`；mtime `2026-07-11T00:48:59.5236818-04:00`；914 行。
- 现有兼容面：package-private class `:24`；`recognize(JsonNode)` `:45-55`；私有 `recognize(BufferedImage)` `:57-115`；`coordinatePlausible` `:175-178`；`mapTransform` `:180-187`；result record `:833-850`。
- 活跃依赖：`DecisionEngine.java:2501` 调 `recognize`；`QuestDetailTextRecognizer.java:53` 调 plausibility；`MiniMapPointResolver.java:61,118,145,183` 调 transform/plausibility。
- 父级把此文件定义为 reservation-only，零 production diff 有效：TURN-28 卡 `:27-29`、`:51-53`；launch helper `:118-129`、`:393-398`。

建议：TURN-28 不需要 objective map-text recognition 来实现 NPC image candidates，首选零 diff。若 typed facade 真实需要复用，也只能增加行为等价的最小可见面，不能改 map loader、阈值、coordinate repair、`Result` 意义或 TURN-27 caller；不应为了“写集列了文件”制造无用途改动。

### 3.3 Cloud `SmartClickRecognizer.java`

- 路径：`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java`。
- SHA-256 `FFBD984A4ED5841CCBA6B87BF3378A1E0CB1E7D2BEA68BE3EED656BE7324F102`；blob `bb8ce2bb64cd92d5bc3609fed8fb91935021ce73`；mtime `2026-07-11T22:23:04.0827006-04:00`；3026 行。
- 现有入口是 legacy package/session API：class `:27`；`recognize(JsonNode,...)` `:64-133`；`recognizeQueueMessages`/`produceQueueMessages` `:135-197`；session-shaped `Result` `:2906-3025`。
- legacy `recognize` 在 `:80-129` 混入 dialog/memory/Ctrl/menu/metadata/retry/screenshot 决策；legacy FIFO 在 `:148-193` 读取 `sessionId/windowId/taskRunId` 并推 queue message。新 Npc path 调用任一入口都会违反 TURN-28 卡 `:49-53`、`:64-66`、`:115-118`。
- 可复用纯 mechanics：tooltip OpenCV hit `:271-329`；formula constants与 anchor `:512-548`；正式 purple wash `:554-593`、OCR `:634-657`；yellow OCR `:1002-1050`；Ctrl menu image OCR `:2539-2575`。紫名必须继续复用 `ImageAlgorithms.wash(...,"WASH_PURPLE")`，业务合同 `docs/业务逻辑.md:1382-1404`。
- 现有 whole recognizer 不能直接当 696 实现：tooltip `:291-317` 返回第一个 candidate，而 696 要 verifier miss 后继续全部去重 hit；yellow `:1017-1049` 跨 region 选 best 并可能 `REQUEST_NEW_SCREENSHOT`，而 696 只在 `TARGET_NOT_FOUND` 扩 region、concrete click miss 不扩；legacy queue/session 又与当前最小 turn 冲突。

建议：只在本文件增加一个最小、跨 package、typed、纯 `BufferedImage` facade，使用 nested immutable request/result。每次调用只执行 NpcClickService 明确指定的一类纯识别并返回有序 candidate/anchor/evidence；不接受 `JsonNode`、Base64、session/queue-store、outcome、owner、TTL，不生成 action、不选择下一策略、不点击、不 verify、不提交 memory。旧三个入口和 `DecisionEngine`/`NpcClickSmartQueueStore` caller 保持可编译但新生产路径零调用。输入图由 caller 所有，typed facade 不应擅自 flush caller frame。

## 4. TURN-28P 当前 public mechanics API

### 4.1 字节和接口

| 文件 | SHA-256 / mtime | 当前可用接口与精确行 |
|---|---|---|
| 双仓 `com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java` | 两仓同 SHA `3D3DD1C516FC7777A8513FDB04FBFBEA1C6A3AEF14D001AE76AA0C84626F25CC`；`2026-07-16T03:36:42.204/205-04:00` | nullable `clickDelayMs/queueHoldMs` `:5-16`，兼容 ctor `:18-27` |
| 双仓 `.../TurnCaptureSpec.java` | 两仓同 SHA `216C8F51B7B08702365E7C9CA8F2E2F43E4F9F12AA6E63FEBBAD495FD545472C`；`2026-07-16T03:36:42.2066217-04:00` | `UPLOAD_IMAGE` `:31-34`；`PixelChangeProbe(targetX,targetY,down,move,up,threshold)` `:49-65` |
| DHXY `TurnInputActionMapper.java` | `B5C6F173BA9A5C40774E24446E6726108701AB47A89A0C80434F15415319303A`；`2026-07-16T03:40:55.5973729-04:00` | click delay + same-list hold `:24-58`；非 click 拒 timing `:30-34`；范围 `[0,5000]` `:127-134` |
| DHXY `TurnInputStepExecutor.java` | `0EE95CBD48D3EC76FB9E50385108F9898F2979A33966487B39065352AF1F43FD`；`2026-07-16T03:41:54.0734553-04:00` | single input `:49-97`；mouse/positive-WAIT sequence `:99-149`；一次 queue submit `:166-178` |
| DHXY `LocalTurnActionExecutor.java` | `9E92CDE9A9F68455A178D6D71BB771A7480AD088AEDB7CB77321561AFA3428F0`；`2026-07-16T03:48:23.3848386-04:00` | action resolve once `:53-63`；mouse/WAIT/mouse 合并一次 transaction `:67-106`、`:135-160`；capture code 原样返回 `:199-213` |
| DHXY `TurnCaptureStepExecutor.java` | `5612B067E4A3F16B48845BD50DCC046CEA3E15FC93781888637210E867CE59F0`；`2026-07-16T06:04:08.7790490-04:00` | public execute `:93-165`；probe validation `:168-203`；exact frozen callback `:205-362`；typed projection `:370-404` |
| DHXY `InputSequences.java` | `2D1768E67A12BF34D58FB64F14102614DC0C597EB41476DC60A49841089F2B6A`；`2026-07-16T06:03:31.1777100-04:00` | typed `submitFrozenExactWindowExclusiveAndWait` `:66-87` |
| DHXY `InputActionQueue.java` / `InputActionWorker.java` | `BCD1E64A...B1ABC4` / `1359C236...032BD`；`2026-07-16T06:03:09.6830838-04:00` / `06:00:35.2517390-04:00` | TURN-28P exact generation、admission、typed STOP 的底层；当前只读，剩余 real-harness test 由 Euler 独占 |
| Cloud `TurnGameClient.java` | `A8F64D8DBB5F9ED2852975D518836E25AF92073F9C818D5F7E9DA7CF18056CB9`；`2026-07-15T20:18:03.0127784-04:00` | exact bind `:64-84`；plain capture `:86-105`；ordered execute `:107-126`；每次调用一 UUID/command `:161-168` |
| Cloud `TurnInvocationResult.java` | `052D9C80A2BFE575514886D1D4EEF30AF6B474F70A713E132FB6D9EF910024A7`；`2026-07-16T03:50:39.3891782-04:00` | command/outcome/frame contract `:27-76`；step/frame correlation `:78-170`；probe code仅 changed/unchanged `:172-174` |

### 4.2 TURN-28 正确调用建议

1. 左/右点击使用 `TurnGameClient.execute(...)`，同一 action 放 `MOVE_MOUSE -> WAIT150 -> CLICK_*`，click spec 固定 `clickDelayMs=150`、`queueHoldMs=firstWaitMs`。不要拆 public client 调用，也不要把 hold 写成 trailing action WAIT；证据为 protocol `:216-222`、DHXY executor `LocalTurnActionExecutor.java:74-105`。
2. Ctrl pixel probe不能用 `TurnGameClient.capture(...)`，因为该 convenience API只构造 plain capture `TurnGameClient.java:95-104`。必须用 `execute(...)` 发送唯一 CAPTURE step和 `PixelChangeProbe(x,y,80,280,100,0.05)`；协议 `:242-254`。
3. Probe `PIXELS_CHANGED` 只允许 Cloud 对返回的唯一 after raw PNG OCR；`PIXELS_UNCHANGED` 才是可继续下一个 origin 的普通 miss。FAILED/STOPPED/command uncertain/release failure/correlation error均终止本次业务推进，不能折成 unchanged。
4. 每个命令前直接 `TaskCheckpoint`，取一个 `TurnInvocationContext`、`bind`、读 latest metadata并校验 exact device/window/HWND/process/rect/STOP，然后才让 public client生成 UUID。不要新增 checkpoint wrapper、transport retry或第二 context cache。
5. TURN-28P 当前 production hashes可支撑 source coding；但 Euler 的两份 real queue/worker tests未交付，且 TURN-22 后续会改 frozen executor consumer，所以这些文件当前只读，最终集成仍必须等待两卡证据。

## 5. 最新 launch helper 的保留与失效部分

- Helper R2 的 SHA/mtime见 `1.1`。其 `:11-16` 以“TURN-28P source review passed”为 mechanics-open 理由；该理由已被后来的 TURN-28P Review #3 `:620-690` 推翻，不能继续作为当前门证据。
- 但 source-start 结论已被新的父级依据重新建立：`ACTIVE_WORK.md:16-25`、权威计划 `:1141-1143`、`:1458-1461`、TURN-28 固定卡 `:3-15`。因此不是“旧 helper 自动仍有效”，而是“父级在 production API 已落盘、剩余测试写集互斥的条件下重新分层授权”。
- R2 仍可保留的内容：四文件 write set `:46-75`；public/recognizer compatibility `:77-129`；strict 696流程/预算 `:131-319`；session/CR255-267/sourceTask/shared-file冲突 `:337-398`；禁令和 named-test matrix `:400-460`。这些内容已被父级固定卡 `:19-123` 明确吸收。

## 6. Exact TURN-28 implementation write set

| # | 唯一允许路径 | External B claim 初始字节 | 建议 |
|---|---|---|---|
| 1 | `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java` | 3406 行；SHA `F4E3842C...BFD870` | strict 696 Cloud-owned turn migration；移除/不迁移 sourceTask gate |
| 2 | `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java` | 914 行；SHA `D3DC3CC2...0F3FE1` | reservation-only；零 diff优先且有效 |
| 3 | `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/SmartClickRecognizer.java` | 3026 行；SHA `FFBD984A...24F102` | 仅最小 typed pure image facade；legacy dormant兼容 |
| 4 | `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NpcClickTurnContractTest.java` | `2026-07-16T08:20:31.9983131-04:00` 仍不存在 | 唯一 named test；真实 production + scripted turn result + in-memory PNG |
| 5 | `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28.md` | 153 行；SHA `7BDE6F13...D5782` | 仅 External B delivery与父级 review append；本 helper不写 |

禁止项来自 TURN-28 卡 `:31-34`、`:113-123`：不改 DHXY、protocol、TurnGameClient/result/factory/executor、`ImageAlgorithms`、`LocalOcrClient`、request/result model、Dialog/BattleRadar/Navigation/Task/caller、POM/config/resource/template、DecisionEngine/queue store/old routes、reference/shadow或第二测试；不新增第四 production 文件、runtime session/queue/poller/outcome reporter、owner/permit/ledger/TTL/cleanup/retry；不删除 reference/shadow。当前 External B claim 已生效，禁止第二 writer。

## 7. Source-start 与 final approval 分层

| 门 | 当前可否 | 精确依据 | Helper 建议 |
|---|---|---|---|
| Source-start | **父级已允许，且 External B 已 claim** | `ACTIVE_WORK.md:16-25`；计划 `:1142`、`:1458-1461`；TURN-28 卡 `:3-15`、`:127-153` | 维持单 owner，从四文件当前字节增量实施。本句是对父级状态的记录，不是 helper 批准。 |
| Source + test delivery / parent static review | 可与 28P 最后两测试并行推进 | TURN-28 卡 `:120-123`；写集互斥见 `:19-34` 和 TURN-28P `:1036-1054` | delivery 后父级逐文件审查；source review不得冒充 test/compile/final。 |
| Final `CARD APPROVED` | **当前不可** | 计划状态机 `:1684-1697`；TURN-28 卡 `:120-123`、`:151` | 至少等待 TURN-28P remaining tests、TURN-22 frozen-executor integration、TURN-28 named test fresh exit 0、父级 source/test review和适用 Cloud compile；另按计划 `:1609-1612` 核对实际调用链相关 Foundation test debt。 |
| Runtime acceptance | 不属于本 helper/本轮 | 协议 `:1474-1483`；TURN-41在计划 `:1172` | 不以 unit/compile替代用户 fresh runtime，也不为等待 runtime延长 source review。 |

结论：**可以分层**，且父级已这样冻结；source-start是 production API/write-set隔离判断，final approval是测试/集成/编译判断。二者不能互相冒充。本 helper无权批准或阻断任一层。

## 8. 会改变 696 业务语义的风险清单

| 风险 | 精确证据 | 必须保持的建议 |
|---|---|---|
| 保留 current `sourceTask` equality gate | Current Npc `:2057-2188`、`:2294-2326`；696 `:2056-2164`、`:2271-2295` | 按父级 fixed card删除/不迁移；否则 null/blank/mismatch proof会从696的可提交变成静默不提交。 |
| 激活 legacy Smart whole recognizer | Smart `:64-197`、`:2906-3025` | 新 Npc path对 `recognize/recognizeQueueMessages/produceQueueMessages` 零调用；仅纯 typed facade。 |
| 把旧 session/base screenshot当新 runtime | `docs/业务逻辑.md:1319-1363` 与新协议 `:48-73`、`:108-126` 冲突 | 父级已冻结为“保 FIFO业务顺序，不保 session/poller/共享 base frame”；每 action最多一 raw PNG，不新增 session/TTL/ledger。 |
| Typed recognizer选择 strategy/fallback | 协议 `:62-66`、`:335-352`；TURN-28卡 `:55-83` | NpcClickService保持业务脑；recognizer只返回指定 operation的有序视觉事实。 |
| Tooltip只取首 hit | Smart `:291-317` 对 696 `:1201-1247` | facade返回全部 dedup score-order hits；每个 hit各一次 click/verify，verified才短路。 |
| Yellow跨 region选best或自动新截图 | Smart `:1017-1049` 对 696 `:1052-1081`、`:1933-2047` | region按推荐顺序；仅 TARGET_NOT_FOUND扩，concrete click miss不扩；无 recognizer screenshot retry。 |
| Purple采用第二套 wash/raw shortcut | Smart `:554-657`；业务合同 `:1382-1404` | raw PNG走唯一 `ImageAlgorithms WASH_PURPLE`，保持 OCR-first再component/blob fallback；不改阈值/公式。 |
| 点击被拆成多个 command/queue request | 696 `:176-214`；protocol `:216-222`；Local executor `:74-105` | 一个 click action内 `MOVE/WAIT150/CLICK(delay150,hold)`；baseline retry才是新 UUID/action。 |
| 把 mechanics completion/OCR hit/pixel change当成功 | TURN-28卡 `:49-53`、`:85-101` | 只有 dialog accepted status或 BattleRadar combat proof闭合业务成功。 |
| 把 STOP/uncertain/release failure变普通 miss | `TurnInvocationResult.java:27-170`；probe executor `:370-404`；协议 `:242-254` | fatal/stop立即停止后续 candidate/click/memory；不自动重发、不 widen。 |
| 改 verifier次数或等待 | 696 `:241-301`；TURN-28卡 `:74-76` | dialog每 candidate一次；combat最多4读且4个known-false各等350，包括第4次。 |
| 删除 formula immediate Ctrl或跨stage去重 | 696 `:998-1050`、`:1084-1127` | immediate SMALL_RING和final Ctrl均保留；同点可再次探；只保各stage内部3px dedup与non-combat 15px filter。 |
| 混入 CR255/CR267 direct-combat语义 | launch helper `:353-374`；TURN-28卡 `:77-81` | 本卡只实现strict 696 flying/Alt+C/Alt+A/exit分支；不加authorization/event/restart hybrid。 |
| 更改 pending memory生命周期 | 696 `:1285-1400`、`:2056-2164`、`:2247-2305` | exact window/token/map/name/coords/option proof；无 TTL/expiry/cleanup scheduler/session owner。 |
| 修改 Objective map/coordinate行为 | Objective `:45-115`、`:175-187`；callers见第3.2节 | 首选零 diff；任何 visibility调整都要行为和签名等价，不能影响 TURN-27。 |
| 坐标空间/窗口 generation混用 | TurnCaptureSpec `:49-65`；TurnGameClient `:64-84`；TURN-28P Review #3 `:628-659` | 每命令重新核 exact context/latest metadata；frame-local candidate只按该 frame region转换为screen-absolute，不复用旧rect/base。 |
| 用测试替代真实 production边界 | TURN-28卡 `:103-111`；计划 `:1639`、`:1684-1697` | 唯一 named test必须驱动真实 NpcClickService和scripted typed outcomes，覆盖负例/次数/顺序；禁止source-string/reflection guard。 |

## 9. 给父级的收口建议

1. 保持 External B 当前单 owner，不再发第二 claim；若其 self-reported identity需要校正，只追加 identity correction，不改变同一 ownership/write set。证据：TURN-28卡 `:127-153`。
2. Static review先核三个 production文件相对本报告 initial SHA的精确 diff；`ObjectiveTextRecognizer`零 diff应接受为符合reservation，不要求凑改动。
3. 对 `NpcClickService` review按 696方法段逐项对照，不以“FIFO标签存在”代替条件/预算/等待/terminal检查；尤其核 `sourceTask` gate已消失、legacy Smart entry zero-call、每次click/probe只一个public turn调用。
4. Source delivery可以先审，但在 TURN-28P Euler delivery/review、TURN-22 integration、named test fresh exit 0和Cloud compile前只记录 source/test-source结论，不写 `CARD APPROVED`。
5. 本 helper报告不构成父级批准、review通过或 blocker；它不改变当前 fixed card、owner和任何 gate。

**非父级批准；无已批准业务差异；按 `696a12b0` 基线等价迁移。**

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-28 PARENT-FREEZE PREFLIGHT HELPER Laplace 019f6acc-732b-7fa1-95fc-8b2460cd6148 PRECHECK_COMPLETE 2026-07-16T08:24:48.4352637-04:00 -->
