# TURN-10CR - GiveItem open-dialog closed local macro repair

## READY / PARENT FROZEN BRIEF - 2026-07-15 22:12 EDT

- 状态：`READY`；类型：Foundation production/test repair；父级是唯一 manager/final reviewer，Worker 不是
  reviewer。
- startDependsOn：`TURN-10C` source；approvalDependsOn：`TURN-T04` named tests 与适用 DHXY compile。
- 问题证据：协议与 adapter 把 `GIVE_ITEM_FROM_OPEN_DIALOG` 命名为一个不可拆本地操作，但当前
  `GiveItemLocalOperationExecutor` 只调用
  `GiveItemService.executeGiveDirectForExclusive(targetItemTemplate, knownBagIndex)`。该方法只负责打开后的物品选择和
  最终“给予”按钮；`696a12b0` 的前置“在当前 option dialog 内匹配给予入口 -> 随机安全点点击 -> 800ms”仍在
  `DialogService`。若 TURN-16 直接删除 Cloud Dialog 的本地 mechanics，本地 operation 就不是完整 open-dialog
  macro；若 Cloud 另发 MATCH/INPUT，再发 LOCAL_SERVICE，则产生第二个本地 queue transaction，不符合本卡冻结的
  单 closed action。
- 目标：在永久本地 `GiveItemService` 增加真实 open-dialog closed macro；一次现有 exclusive input callback 内严格
  保持 `696a12b0` 顺序：
  1. 在 `DIALOG_SMALL=(250,345,529,143)` 的 exact-window scaled rect 内，以 `0.85` 匹配
     `images/template/dialog/maintenance/dialog_opt_give.png`；
  2. 按既有 `20x5` random safe point 点击，click delay `150ms`，随后 `TaskSleep 800ms`；
  3. 调用现有 `executeGiveDirectForExclusive(...)`，保持其既有 `800ms -> BagService direct select -> 给予按钮
     0.85 -> 20x8 safe point -> click 100ms -> sleep 1000ms`；
  4. 任一步 miss/interrupted/false 立即返回 false，不追加 retry、capture、第二 command 或业务 fallback。
- 旧 `executeGive(...)` / `executeGiveDirectForExclusive(...)` public 语义保持不变，避免当前 legacy Dialog 在正式
  TURN-16 cutover 前重复点击给予入口。新增 API 必须明确表示“从已打开 option dialog 开始”，并且 direct 版本只在
  已有 input-worker exclusive boundary 内执行。

### Exact write set

- `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
- `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
- Create
  `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/service/GiveItemServiceOpenDialogContractTest.java`
- 本报告。

其余 DHXY/Cloud 文件全部只读；尤其禁止修改 `DialogService`、`LocalServiceStepDispatcher`、protocol、Cloud、POM、
Task、runtime、Server/routes 或其它报告。保护全部 dirty/untracked，不回滚、覆盖、清理或执行 Git mutation。

### Named-test acceptance

- `GiveItemLocalOperationExecutorContractTest` 必须改为证明 adapter 对合法 operation **恰好一次**调用新的 whole
  open-dialog Service API，typed `given=true/false` 保持；invalid/unsupported 零 Service 调用。
- `GiveItemServiceOpenDialogContractTest` 必须实际调用 production `GiveItemService`，用 fake
  `CoordinateHelper`、`InputProvider`、`BagService` 记录机械顺序，至少覆盖：
  - success：exact template/rect/threshold，给予入口 click 在 bag direct-select 与最终给予按钮 click 之前；
  - give-entry miss：零 input、零 bag select；
  - item-select false：不点击最终给予按钮；
  - stop/interrupted sleep：后续 mechanics 短路。
- 不得 mock 掉 `GiveItemService` 作为本卡唯一证明；不得加 sleep/retry abstraction、owner/session/ledger/TTL 或第五
  local Service。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级在所有 Java writers
  稳定后运行：
  `mvn -q -Dtest=GiveItemLocalOperationExecutorContractTest,GiveItemServiceOpenDialogContractTest test`，随后适用
  `mvn -q -DskipTests compile`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-10CR parent frozen brief -->

## CLAIMED - 2026-07-15 22:23:23 EDT

- Worker role: `TURN-10CR implementation Worker`；父级是唯一 manager/final reviewer。
- Exact write set:
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/service/GiveItemService.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
  - `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
  - Create `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/service/GiveItemServiceOpenDialogContractTest.java`
  - `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-10CR.md`
- 其余两仓文件全部只读；保护共享 dirty/untracked，不回滚、覆盖、清理、提交或执行其它 Git mutation。

<!-- TRUE_EOF: TURN-10CR worker claimed -->

## SOURCE + TEST SOURCES DELIVERED - 2026-07-15 22:30:06 EDT

### Delivered files and SHA-256

- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
  - `765114426765CD2A7566A94B2391F42AF56D2C7960C068F9D7D332192747EB54`
- `src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
  - `2FDD1B524E789E8E58AF1C32701A141FD1856263593AE5A7E4F67ABDCBC6B6E8`
- `src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
  - `8022BBCB54018BFDCDC8F7E2ECDD4E378F87C4FA81C53FF2FE0BE8E53E2B1260`
- `src/test/java/com/bot/dhxy/service/GiveItemServiceOpenDialogContractTest.java`（新建）
  - `872DFA34CAA23E775311BB7848D50934EBE00FF87A1321513DA8D22BD420A5CB`
- 本报告已追加 CLAIMED 与本交付段；报告自身不在正文内记录 self-referential SHA，父级可在读取最终 EOF 后取 hash。

### Static source evidence

- `GiveItemService` 新增且仅新增一个 whole direct API：
  `executeGiveFromOpenDialogDirectForExclusive(String, Integer)`。该 API 要求当前线程位于既有
  `dhxy-input-action-worker` exclusive callback；未新增 queue request 或第二 command。
- whole API 的固定顺序为：
  `getScaledRect(250,345,529,143)` ->
  `findGreenTextInRegion(images/template/dialog/maintenance/dialog_opt_give.png, rect, 0.85)` ->
  `getRandomizedPoint(point,20,5)` -> `InputProvider.clickLeft(...,150)` ->
  `TaskSleep.sleep(800)` -> `executeGiveDirectForExclusive(...)`。
- `git diff` 显示旧 `executeGive(...)` 与 `executeGiveDirectForExclusive(...)` 方法体没有修改；后者继续保留
  `TaskSleep 800 -> BagService.findAndSelectItemDirectForExclusive(GIVE_BAG,...) -> btn_give 0.85 ->
  20x8 safe point -> click 100 -> TaskSleep 1000`。
- adapter 合法 operation 中
  `giveItemService.executeGiveFromOpenDialogDirectForExclusive(...)` 静态调用数为 `1`，legacy direct 调用数为 `0`；
  invalid/unsupported 分支仍在 Service 调用前 fail closed，typed `given=true/false` 结构不变。
- 只读核查 `LocalServiceStepDispatcher`：`GIVE_ITEM_FROM_OPEN_DIALOG` 仍只建立一次既有
  `submitExclusiveAndWait(...)` callback，callback 内只调用 adapter；本卡未修改 dispatcher/Dialog/protocol。

### Test-source evidence

- `GiveItemLocalOperationExecutorContractTest` 分别记录 whole API 与 legacy direct API 调用数：合法 true/false
  均断言 whole API 恰好 `1` 次且 legacy `0` 次；invalid/unsupported 断言两者均 `0` 次。
- `GiveItemServiceOpenDialogContractTest` 直接实例化 production `GiveItemService`，未以 fake Service 代替目标类；
  fake `CoordinateHelper`、`InputProvider`、`BagService` 记录并断言 exact template/rect/threshold、`20x5` 与 `20x8`
  safe point、两次 click delay 及 `give-entry click -> direct bag select -> final give click` 顺序。
- 覆盖四条冻结场景：success、give-entry miss（零 input/零 bag select）、item-select false（无 final give match/click）、
  interrupted entry wait（入口 click 后保留基线语义并立即阻断后续 bag/final mechanics）。未新增 sleeper/wrapper。

### Baseline reconciliation

- 已核对 `docs/业务逻辑.md` 的“`五环唤醒 / Prepared Action 记录`”中交鞋 option dialog 与按
  operation/template 语义消费规则，以及“`通用任务类热启动 Policy`”第 2 条已知 dialog 动作边界。
- 已逐行核对 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`：
  `DialogService.tryGiveItemFromCurrentOptionDialog(...)` 的 give-entry template/rect/threshold、`20x5`、click 150、
  sleep 800 与后续 direct give 顺序；并核对同提交 `GiveItemService` 的旧 public/direct 实现。
- 无 retry、第二 capture/command、第二 local Service、owner/session/ledger/TTL 或新业务 fallback。
- **无已批准业务差异；按 `696a12b0` 基线等价迁移。**

### Gates not run by Worker

- 按父级冻结门，本 Worker **未运行** Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或
  input；未执行 Git mutation，未启动任何真实桌面/游戏动作。
- 父级待 writers 稳定后运行：
  `mvn -q -Dtest=GiveItemLocalOperationExecutorContractTest,GiveItemServiceOpenDialogContractTest test`，随后
  `mvn -q -DskipTests compile`。
- 当前结论仅为 `SOURCE + TEST SOURCES DELIVERED`，等待父级 source/assertion review 与命名测试/compile 门；
  本 Worker 未写 `APPROVED` 或 `CLOSED`。

<!-- TRUE_EOF: TURN-10CR source-and-test-sources-delivered -->

## PARENT SOURCE + TEST SOURCE REVIEW PASSED - 2026-07-15 22:50 EDT

- 父级独立逐文件审查 production、两份 named-test source、只读 dispatcher 与 `696a12b0` 基线，结论：
  `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。
- `GiveItemService.executeGiveFromOpenDialogDirectForExclusive(...)` 只在既有
  `dhxy-input-action-worker` exclusive callback 内执行，严格保持
  `DIALOG_SMALL exact rect -> dialog_opt_give 0.85 -> 20x5 -> click 150ms -> wait 800ms -> existing direct
  give`；旧两个 public give API 的语义未改。
- `GiveItemLocalOperationExecutor` 对合法 operation 恰好调用一次 whole API；只读
  `LocalServiceStepDispatcher` 仍只有一次 `submitExclusiveAndWait(...)`，没有第二 command、第二 queue、capture、
  retry 或 fallback。
- 两份测试源码直接覆盖 whole adapter 与 production Service：success 顺序、entry miss、item-select false、
  interrupted wait 均有精确断言；未用 fake Service 替代目标类。
- 本卡 owner 释放，`TURN-16` 的 source 前置已解除。named tests 与 DHXY compile 仍归 stable-writer cohort；在门禁
  实际通过前不得写 `CARD APPROVED/CLOSED`。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-10CR parent-source-test-review-passed -->

## PARENT INTEGRATION REVIEW #2 - REPAIR #1 REQUIRED - 2026-07-15 22:56 EDT

- 本结论覆盖上一段仅针对冻结 mechanics brief 的 `0/0/0`：机械顺序仍通过，但与真实 Cloud caller 集成时发现
  `P0/P1/P2=0/1/0`，本卡不得保持 source-complete 状态。
- **P1 精确证据：**当前 whole API 与 adapter JSON 只有 `boolean given`。`696a12b0`/当前
  `DialogService.tryGiveItemFromCurrentOptionDialog(...)` 明确区分 `GIVE_OPTION_NOT_FOUND`、`GIVE_ITEM_FAILED`、
  `INTERRUPTED`；真实 `FiveRingTaskV2.handleDialog(...)` 对 `GIVE_OPTION_NOT_FOUND` 执行 cleanup+resync，而普通
  `GIVE_ITEM_FAILED` 会累计 UI error 并可能终止本轮。把三个状态压成 `false` 会改变已批准业务分支。
- **Repair #1 冻结合同：**不增加新命令、协议 DTO、retry 或 wrapper 链；仅把新 whole API 返回值升级为一个
  `GiveItemService` 内的 closed enum：`GIVEN`、`GIVE_OPTION_NOT_FOUND`、`GIVE_ITEM_FAILED`、`INTERRUPTED`。
  entry miss、entry 后首个 800ms 中断、既有 direct give true/false 分别映射上述 exact 状态；既有
  `executeGive(...)` 与 `executeGiveDirectForExclusive(...)` 仍保持 boolean API/实现不变。adapter 完成 JSON 固定为
  `{"state":"<ENUM>"}`。

### Repair #1 exact write set

- `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/service/GiveItemService.java`
- `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
- `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
- `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/service/GiveItemServiceOpenDialogContractTest.java`
- 本报告。

其余两仓全部只读；尤其不改 protocol、dispatcher、Cloud、Dialog caller、Task、POM 或其它报告。测试源码必须逐态
断言 Service 返回值与 adapter JSON，并保留原 success/entry miss/item false/interrupted 顺序断言。Worker 不运行
Maven/runtime/input/Git；父级在 writers 稳定后执行原 named tests 与 DHXY compile。

**无已批准业务差异；Repair #1 恢复 `696a12b0` 的既有状态分支。**

<!-- TRUE_EOF: TURN-10CR parent-integration-review-2-repair-1-required -->

## REPLACEMENT CLAIMED - 2026-07-15 23:03:09 EDT

- Replacement role: `TURN-10CR Repair #1 replacement implementation Worker`；上一个 Worker 连接中断不可达，
  本 Worker 在共享工作区保留的首版 source/test source 与原卡记录基础上继续。
- 唯一写集仍为 `GiveItemService.java`、`GiveItemLocalOperationExecutor.java`、
  `GiveItemLocalOperationExecutorContractTest.java`、`GiveItemServiceOpenDialogContractTest.java` 与本报告；
  其余两仓文件全部只读。
- 已核对现状：首版 whole mechanics 已完整保留，Repair #1 四态 source/test 尚未写入；本次只实施父级冻结的
  `GIVEN/GIVE_OPTION_NOT_FOUND/GIVE_ITEM_FAILED/INTERRUPTED` 返回合同与固定 `{"state":"<ENUM>"}` JSON。
- 不回滚、覆盖、清理、提交或执行 Git mutation；不运行 Maven、JUnit、compile、runtime、application、server、
  Task、UI、capture 或 input。父级仍是唯一 manager/final reviewer，本 Worker 不写 `APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-10CR repair-1-replacement-claimed -->

## SOURCE + TEST SOURCES DELIVERED - REPAIR #1 - 2026-07-15 23:08:01 EDT

### Delivered files and SHA-256

- `src/main/java/com/bot/dhxy/service/GiveItemService.java`
  - `A08736885E1F48A3EEE6003C2A5863DE2B769D9F9E5255C3536A58C4632655AB`
- `src/main/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutor.java`
  - `C6CC4792CB376C6E85677EEA1C0E6B638A4E5B79B336003AB05C53E211DD8A56`
- `src/test/java/com/bot/dhxy/cloud/turn/local/GiveItemLocalOperationExecutorContractTest.java`
  - `CF6476F7ED685FB5950A5841AFDB0A5D46A409918B00118FF9F2618A69A868F6`
- `src/test/java/com/bot/dhxy/service/GiveItemServiceOpenDialogContractTest.java`
  - `4A08306FFAA04EC50118CA01CE801436A74BE76A4546D84FD94DEB275598E3D6`
- 本报告已追加 replacement claim 与本交付段；报告自身不记录 self-referential SHA。

### Repair #1 source evidence

- `GiveItemService` 内新增 closed enum `OpenDialogGiveState`，仅含父级冻结的
  `GIVEN/GIVE_OPTION_NOT_FOUND/GIVE_ITEM_FAILED/INTERRUPTED` 四态；whole API 直接返回该 enum。
- exact 映射为：give-entry miss -> `GIVE_OPTION_NOT_FOUND`；entry click 后首个 800ms wait false ->
  `INTERRUPTED`；既有 `executeGiveDirectForExclusive(...)` true/false -> `GIVEN/GIVE_ITEM_FAILED`。
- 首版已通过的 mechanics 顺序未改：exact scaled dialog rect、entry template `0.85`、`20x5` safe point、
  click `150ms`、wait `800ms`，再进入既有 direct give flow。旧 `executeGive(...)` 与
  `executeGiveDirectForExclusive(...)` 仍是原 boolean signature/implementation。
- `GiveItemLocalOperationExecutor` 对合法 operation 仍只调用 whole API 一次；完成 JSON 的唯一字段改为 enum
  `state`，固定形态为 `{"state":"<ENUM>"}`。invalid/unsupported 仍在 Service 调用前 fail closed。
- 未新增 command、retry、protocol DTO、wrapper、Service、queue acquisition、capture 或业务 fallback；未修改
  dispatcher、Dialog caller、Task、Cloud、protocol、POM 或其它报告。

### Repair #1 test-source evidence

- `GiveItemServiceOpenDialogContractTest` 直接调用 production Service，并对四条真实返回路径逐态断言：direct
  true -> `GIVEN`、entry miss -> `GIVE_OPTION_NOT_FOUND`、direct false -> `GIVE_ITEM_FAILED`、entry wait
  interrupted -> `INTERRUPTED`；原 exact template/rect/threshold、click 顺序及短路断言全部保留。
- `GiveItemLocalOperationExecutorContractTest` 对四个 enum 分别断言 exact JSON：
  `{"state":"GIVEN"}`、`{"state":"GIVE_OPTION_NOT_FOUND"}`、`{"state":"GIVE_ITEM_FAILED"}`、
  `{"state":"INTERRUPTED"}`；每态 whole API 恰好一次、legacy direct API 零次，invalid/unsupported 两者均零次。
- 静态扫描确认该 adapter/test 已无旧 `"given"`、`boolean given` 或 boolean result record；tracked Service
  `git diff --check` 无 whitespace error。以上是源码证据，不冒充测试或编译结果。

### Baseline reconciliation and gates not run

- 已核对 `docs/业务逻辑.md` 的“`五环唤醒 / Prepared Action 记录`”交鞋 option dialog 与按
  operation/template 语义消费规则；已核对 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 中
  `DialogService.tryGiveItemFromCurrentOptionDialog(...)` 的四分支映射和既有 `GiveItemService` boolean direct flow。
- **无已批准业务差异；Repair #1 恢复 `696a12b0` 的既有状态分支。**
- 按父级冻结禁令，本 replacement Worker **未运行** Maven、JUnit、compile、runtime、application、server、
  Task、UI、capture 或 input；未执行 Git mutation，未启动任何真实桌面/游戏动作。
- 待父级在 writers 稳定后运行原门：
  `mvn -q -Dtest=GiveItemLocalOperationExecutorContractTest,GiveItemServiceOpenDialogContractTest test`，随后
  `mvn -q -DskipTests compile`。当前仅为 `SOURCE + TEST SOURCES DELIVERED - REPAIR #1`，本 Worker 未写
  `APPROVED` 或 `CLOSED`。

<!-- TRUE_EOF: TURN-10CR repair-1-source-and-test-sources-delivered -->

## PARENT RE-REVIEW - REPAIR #1 SOURCE + TEST SOURCE PASSED - 2026-07-15 23:10 EDT

- 父级独立逐文件复审 `GiveItemService`、GiveItem local adapter、两份 named-test source，并回查
  `696a12b0` 的 `DialogService.tryGiveItemFromCurrentOptionDialog(...)` 四分支，结论：
  `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。
- `executeGiveFromOpenDialogDirectForExclusive(...)` 现精确保留四态：entry miss ->
  `GIVE_OPTION_NOT_FOUND`；entry click 后首个 800ms wait 被中断 -> `INTERRUPTED`；既有 direct give
  true/false -> `GIVEN/GIVE_ITEM_FAILED`。旧 `executeGive(...)` 与
  `executeGiveDirectForExclusive(...)` 的 boolean signature/实现未改。
- adapter 对合法 operation 仍只调用 whole macro 一次，完成 JSON 固定为
  `{"state":"<ENUM>"}`；invalid/unsupported 在 Service 调用前 fail closed。两份测试源码分别锁住四态 JSON、
  production Service 的 exact template/rect/threshold、click 顺序与全部短路，未增加 command、retry、协议 DTO、
  wrapper、Service 或第二 queue。
- Repair #1 owner 释放，`TURN-16` source 前置已满足。named tests 与 DHXY compile 仍留 stable-writer cohort；
  在门禁实际通过前不得写 `CARD APPROVED/CLOSED`。

**无已批准业务差异；Repair #1 恢复 `696a12b0` 的真实 caller 状态分支。**

<!-- TRUE_EOF: TURN-10CR repair-1-parent-source-test-review-passed -->
