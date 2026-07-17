# TURN-23P - conditional pointer clearance for CAPTURE

## READY / PARENT-FROZEN BRIEF - 2026-07-15 23:31 EDT

- 状态：`READY / PARENT BRIEF FROZEN / QUEUED`；类型：`FOUNDATION REPAIR`；`countDelta=0`。
- 目的：给现有 HTTPS turn `CAPTURE` JSON 增加一个通用、可选的
  `clearPointerIfOverRegion` 机械参数，使 Cloud 能在同一个 payload 中明确要求：只有当前鼠标位于本次 ROI
  （含 padding）时，本地才执行一次精确 move+wait，然后对同一 exact-bound HWND 后台截图。它不是第五个本地
  Service，也不把 HP/MP、香或任何业务判断放回 DHXY。
- startDependsOn：`TURN-01B`、`TURN-01D`、`TURN-08A`、`TURN-09R`；approvalDependsOn：同卡双端
  protocol parity、DHXY executor contract、named tests 与适用双仓 compile。
- 解锁：`TURN-23` 的 first-aid/status ROI capture；其它 capture 默认保持当前行为。

### Exact production write set

- DHXY：
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
- Cloud（前两文件必须与 DHXY 同字节）：
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- 只读：全部 Service/Task/caller、`TurnAction`/`TurnStep`/input DTO、HTTP/loop/exchange、Server/routes、
  `InputActionQueue`/`InputSequences` production、capture provider 与四个永久本地 Service。

### Frozen JSON contract

- `TurnCaptureSpec` 保持 `region`、`resultMode`，新增可选闭合对象 `clearPointerIfOverRegion`；其字段恰为：
  `paddingPx,targetX,targetY,settleMs`。对象缺失/null 表示保持当前纯后台 capture，不读取或移动鼠标。
- 对象存在时：
  - `region` 必须 non-null；full-window capture 不允许隐式套用本策略。
  - `paddingPx` 为 `0..128`；TURN-23 固定使用基线 `12`。
  - `settleMs` 为 `0..5000`；TURN-23 固定使用基线 `300`。
  - `targetX/targetY` 是 Cloud 根据最新 `TurnWindowMetadata.windowRect` 给出的精确
    `SCREEN_ABSOLUTE_PX`；允许多显示器负坐标，但执行前必须落在同一 refreshed window rect 内，并且不得落在
    padding 后的 capture region 内。不得 clamp、缩放、改写或本地随机另选点。
- DHXY 在 action 已冻结的 exact `TurnExecutionWindow` 上读取一次当前 pointer：
  - pointer 不可读或位于 padded region 外：零 input，直接进行一次现有后台 capture；这等价于
    `696a12b0` 的 null/outside 分支。
  - pointer 位于 padded region 内：只提交一次现有全局 input queue，内容精确为
    `MOVE_MOUSE(targetX,targetY) -> SLEEP(settleMs)`；queue 成功后才执行一次现有后台 capture。
  - queue false、STOPPED/interrupted 或异常：本 CAPTURE step 按现有 terminal/failure 规则结束，零 capture、零
    自动 retry、零第二 action/HTTP exchange。
- pointer、ROI、target 和 window rect 全在同一未缩放 screen-absolute 像素空间比较；不得使用
  `systemScaleRatio` 做坐标乘除。只有 pointer 真正在 padded ROI 内时才会触发前台鼠标动作；其余截图保持后台。

### Exact test write set and acceptance

- DHXY：
  - new `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java`
  - existing `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - existing `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - existing `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
- Cloud byte-parity tests：
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
- Named executor test 必须直接调用 production `TurnCaptureStepExecutor` 并覆盖：pointer outside、pointer null、
  pointer on padded boundary、inside success、queue false、STOPPED/interrupted、target outside window、target still
  inside padded ROI、negative monitor origin。逐例断言 input submission count、ordered MOVE/SLEEP、capture count、
  exact unscaled coordinates、同一 frame region 和零 retry。
- 双端 JSON/validator tests 必须覆盖对象 absent 的旧 capture、完整合法对象、缺/多 key、null primitive、numeric/
  string coercion、非法 padding/settle、full-window+clear 拒绝；双端 protocol production 与 golden fixtures 保持同字节。
- 父级 stable-writer cohort 命令：
  - DHXY `mvn -q -Dtest=TurnCapturePointerClearContractTest,TurnProtocolValidatorContractTest,TurnActionGoldenJsonTest,TurnCoreProtocolGoldenJsonTest test`
  - Cloud 对应 protocol named tests 与适用 compile/build。

### Baseline and prohibitions

- 基线证据：`696a12b0:PlayerStateService#moveMouseAwayBeforePlayerStateSnapshotIfNeeded` 只在 pointer 命中
  capture rect 加 `12px` padding 时移动，随后等待 `300ms`；pointer null/outside 不移动。本卡只把该条件变成
  Cloud payload 明示的通用 capture mechanics，不改变 first-aid/incense 的业务条件、阈值、顺序或结果解释。
- 禁止总是 move-away；禁止本地判断 HP/MP/香；禁止自动 retry、第二 capture、session/ledger/TTL/durable
  workflow；禁止 runtime/application/server/Task/UI/capture/input 和 Git mutation。

**无已批准业务差异；按 `696a12b0` pointer-over-ROI 条件等价迁移。**

<!-- TRUE_EOF: TURN-23P parent-frozen-brief -->

## CLAIMED - TURN-23P implementation Worker - 2026-07-15 EDT

- 角色：仅 implementation Worker；父级是唯一 manager/final reviewer，本 Worker 不作 reviewer 结论。
- Exact production write set：
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- Exact test write set：
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
- 唯一额外可写路径：本固定报告 true EOF，用于本次 claim 与交付证据。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `PlayerStateService#moveMouseAwayBeforePlayerStateSnapshotIfNeeded`；pointer null/outside 零 input，inclusive
  padded ROI 内才移动，基线 padding=`12px`、settle=`300ms`。本卡按 exact unscaled
  `SCREEN_ABSOLUTE_PX` 将该条件等价迁移到通用 CAPTURE mechanics。
- 冻结禁令：不得改 Service/Task/caller/input queue/capture provider/HTTP/loop/exchange/Server/routes；不得新增本地
  HP/MP/香业务判断、第五个本地 Service、retry/第二 capture/第二 action/session/ledger/TTL/durable workflow；不得
  clamp、缩放、随机改写 target；不得把 full-window capture 与 clear 策略组合；不得启动
  runtime/application/server/Task/UI/capture/input；不得运行 Maven/JUnit/compile；不得做 Git mutation；保留全部
  dirty/untracked 和并发改动。

**无已批准业务差异；按 `696a12b0` pointer-over-ROI 条件等价迁移。**

<!-- TRUE_EOF: TURN-23P implementation-worker-claimed -->

## REPLACEMENT CLAIMED - TURN-23P implementation Worker - 2026-07-16 EDT

- 前会话 Carver `019f68fe-a994-7a63-8b9d-9a49f5c49fab` 已返回 `not_found`；replacement Worker
  `019f6913-a053-7041-a134-6ec890af35a8` 仅接续原卡，不回滚、覆盖或重做前会话及并发作者已落盘的正确内容。
- Exact production write set（原样继承）：
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- Exact test write set（原样继承）：
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
- 唯一额外可写路径仍为本固定报告 true EOF。父级仍是唯一 manager/final reviewer；replacement Worker
  不是 reviewer。原冻结禁令、`696a12b0` 基线、dirty/untracked 保护和全部不运行门禁原样继承。

**无已批准业务差异；按 `696a12b0` pointer-over-ROI 条件等价迁移。**

<!-- TRUE_EOF: TURN-23P replacement-worker-claimed -->

## SOURCE DELIVERED + TEST DELIVERED - replacement implementation Worker - 2026-07-16T00:09:08-04:00

- Replacement continuity：Carver `019f68fe-a994-7a63-8b9d-9a49f5c49fab` 在 `not_found` 前已把冻结的
  production/test source 全部落盘；replacement `019f6913-a053-7041-a134-6ec890af35a8` 逐文件读取并按本卡
  合同静态核对，保留了这些正确字节，没有回滚、覆盖或重做 Java 内容。本 replacement 的唯一新写入是本报告
  true EOF 的 claim 与本交付记录。

### Actual production source delivered

- 双仓 `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - 两仓均 38 行 / 1461 bytes；SHA-256
    `CE41292AC4B9A9E2788BEC214F1B458FAB13BD7A62151C7BC50E950009390967`；byte parity=`true`。
- 双仓 `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - 两仓均 427 行 / 23102 bytes；SHA-256
    `E2F81039B638E1DFFB805E92C8DA1A61F826240075EE0D0A2FE341B7A0717097`；byte parity=`true`。
- DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
  - 208 行 / 10642 bytes；SHA-256
    `D153BBB67690CD6621085FDEDCA2A71E745E2917EF6BFDFDCB1D179B79D9D7E5`。

### Actual named-test source delivered

- DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java`
  - 404 行 / 16796 bytes；SHA-256
    `34A5623AB3536D7BA490A72A06600F27F269CE71B40BAE8F6FAD631A85968196`。
- 双仓 `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - 两仓均 321 行 / 19817 bytes；SHA-256
    `D3AD730C1BAC1995643C7C2B7FB34981932CF6852C56BF65B40730DF21EA6173`；byte parity=`true`。
- 双仓 `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - 两仓均 121 行 / 6231 bytes；SHA-256
    `4C68DF634B4A24F91DA47B1AE27F5ED8FDA633BD571DA85DF8F16A48ADDBD0C7`；byte parity=`true`。
- 双仓 `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
  - 两仓均 267 行 / 14431 bytes；SHA-256
    `2D1FE87CC85CCC8A40AC5D86D0CA3622AB585F09ACB45F444E56D7C7C609A147`；byte parity=`true`。
- Cloud `.gitignore` 继续忽略 `src/test/`；三份 Cloud parity test 已真实存在于冻结绝对路径。本 Worker 未
  force-add、stage、commit 或执行其它 Git mutation。

### Key static evidence

- `TurnCaptureSpec.java:11-36` 保留旧二参数构造，新增唯一可选
  `clearPointerIfOverRegion`，闭合字段恰为 `paddingPx,targetX,targetY,settleMs`；null 序列化时省略，旧 JSON
  capture 不增加字段。
- `TurnProtocolValidator.java:199-215,398-404` 保持 region/resultMode 原门，并闭合 non-null ROI、
  padding `0..128`、settle `0..5000` 及 target 不得位于 inclusive padded ROI。双仓 production validator
  与 spec 同字节。
- `TurnCaptureStepExecutor.java:76-124` 对 action 已冻结的同一 `TurnExecutionWindow` 工作：policy null 时不读
  pointer；policy 存在时先校验 ROI/window/target/stop，再只读 pointer 一次。只有 pointer 位于 inclusive padded
  ROI 内，`:106-114` 才在 `window.context()` 下调用一次现有 `InputSequences.submitAndWait(...)`，动作精确为
  `MOVE_MOUSE(targetX,targetY) -> SLEEP(settleMs)`；queue false、interrupt 或异常在本 step capture 前结束。
  outside/null 直接落到 `:124` 的一次现有 capture；`:152-160` 始终使用不可变 action binding 与同一绝对 ROI，
  没有 scale/clamp/随机 target、第二 capture 或 retry。
- `TurnCapturePointerClearContractTest.java:38-222` 直接实例化并调用 production executor，覆盖 policy absent、
  pointer null/outside、inclusive padded boundary、inside success、queue false/exception、STOPPED、pre/queue
  interrupted、target outside window、target on padded ROI 及负坐标 monitor；逐例固定 pointer/input/capture 次数、
  exact ordered MOVE/SLEEP、同一 context/HWND/region 和 signed coordinates。
- 双仓 protocol tests 在 `TurnProtocolValidatorContractTest.java:84-174`、
  `TurnActionGoldenJsonTest.java:50-83`、`TurnCoreProtocolGoldenJsonTest.java:101-130` 覆盖 legacy absent/null、合法
  round-trip、字段恰为四个、缺/多 key、null primitive、字符串/浮点 coercion、padding/settle 边界、full-window
  reject、inclusive target reject、负坐标保持及零 `systemScaleRatio`。真实 DHXY response parser 的 strict mapper
  仍负责 unknown/null/coercion/duplicate fail-closed；本卡没有修改 HTTP/client 路径。
- 冻结 production 三文件静态扫描对 `retry/session/ledger/TTL/systemScaleRatio` 为零命中；executor 只有一个
  pointer supplier read、一个 input submission site 和一个 requested capture return site。

### Baseline and workspace reconciliation

- 已回读 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `PlayerStateService#moveMouseAwayBeforePlayerStateSnapshotIfNeeded`、`mouseOverCaptureRect` 及常量：pointer
  null/outside 零 input，inclusive padding=`12px` 内才 move，settle=`300ms`。本卡只把这段条件机械合同显式放入
  Cloud CAPTURE payload；未改变 first-aid/incense 条件、阈值、顺序、结果解释或 caller。
- 盘点时 DHXY=`thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`、Cloud=
  `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`，两仓均无 upstream；冻结文件处于既有
  untracked/ignored 工作树。全部其它 dirty/untracked 与并发作者内容保持原状。

### Gates not run by Worker

- 严格按父级冻结禁令，未运行 Maven、JUnit、compile/package、runtime、application、server、Task、UI、
  capture 或 input；未启动任何应用/服务/真实桌面动作，未做 Git mutation。
- 待父级 stable-writer cohort 运行 DHXY
  `mvn -q -Dtest=TurnCapturePointerClearContractTest,TurnProtocolValidatorContractTest,TurnActionGoldenJsonTest,TurnCoreProtocolGoldenJsonTest test`、
  Cloud 对应三份 protocol named tests 及适用双仓 compile/build。本记录只声明
  `SOURCE DELIVERED + TEST DELIVERED`，不作 reviewer、`CARD APPROVED` 或 `CLOSED` 结论。

**无已批准业务差异；按 `696a12b0` pointer-over-ROI 条件等价迁移。**

<!-- TRUE_EOF: TURN-23P SOURCE DELIVERED + TEST DELIVERED -->

## PARENT SOURCE + TEST-SOURCE REVIEW - 2026-07-16 00:11 EDT

- 父级独立逐文件审查结论：`P0/P1/P2=0/0/0`；状态更新为
  `SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+DUAL COMPILE PENDING`。本结论不是 Worker 自述，也不冒充
  `CARD APPROVED/CLOSED`。
- 双仓 `TurnCaptureSpec`、`TurnProtocolValidator` 及三组 protocol/golden test 已由父级重新计算确认 byte parity；
  可选对象只有 `paddingPx,targetX,targetY,settleMs`，legacy absent/null 保持兼容，full-window、非法范围、字段
  缺失/多余/null primitive/coercion 与 target 落在 inclusive padded ROI 均 fail-closed。
- DHXY production `TurnCaptureStepExecutor` 已确认：在 action 冻结的同一 `TurnExecutionWindow` 上只读 pointer 一次；
  null/outside 零 input 并只做一次 requested capture；inside 只提交一次全局 queue，顺序精确为
  `MOVE_MOUSE -> SLEEP`，且只有 queue 成功、线程未中断后才对同一 HWND/同一 absolute ROI capture。queue false、
  stop/interruption、异常均在 requested capture 前终止；没有 scale/clamp、第二 command 或自动 retry。
- 父级同时追到 `LocalTurnActionExecutor`：本卡“零 capture”约束指本 CAPTURE step 不得在 pointer-clear 失败后
  继续执行 requested ROI capture；action payload 若显式开启既有 `fullWindowFailureEvidence`，外层仍可按既有失败
  规则生成一张独立 full-window failure evidence。这不是本卡新增的第二 CAPTURE step，也不改变用户已确认的
  “失败时由 Cloud 看证据图”合同。
- Named executor test 直接调用 production executor，覆盖 policy absent、pointer null/outside/inclusive boundary、
  inside success、queue false/exception、stop/pre-interrupt/queue-interrupt、非法 target 与 negative monitor；双仓
  strict JSON/golden test source 覆盖冻结协议边界。父级未发现测试仅验证私有 helper、伪 caller 或放宽 mapper 的情况。
- 基线复核：`696a12b0` 的 pointer null/outside 零 input、inclusive `12px` 命中、单 move 与 `300ms` settle 条件均
  可由本通用 payload 精确表达。**无已批准业务差异；按 `696a12b0` pointer-over-ROI 条件等价迁移。**
- 当前 TURN-19/21/25 Java writers 仍活动，故父级本轮不运行 Maven/JUnit/compile。owner 可释放；本卡进入
  stable-writer cohort，待 named tests 与双仓适用 compile 通过后再决定 `CARD APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-23P PARENT SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING -->
