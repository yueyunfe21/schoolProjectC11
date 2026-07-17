# TURN-16 - GiveItem indivisible Cloud facade

## READY / PARENT FROZEN BRIEF - 2026-07-15 22:56 EDT

- 状态：`READY`；类型：Cloud production + named contract test；父级是唯一 manager/final reviewer，Worker 不是
  reviewer。
- startDependsOn：`TURN-02R`、`TURN-13C`、`TURN-10CR Repair #1 frozen contract`；approvalDependsOn：
  `TURN-10CR Repair #1 source`、本卡 named test 与适用 Cloud compile/build。
- 目标：Cloud `DialogService` 的 `GIVE_ITEM_IF_AVAILABLE` 分支只发一次
  `LOCAL_SERVICE/GIVE_ITEM_FROM_OPEN_DIALOG`。DHXY 在同一 command/exclusive callback 内完成 give-entry、选物和
  最终 Give click；Cloud 不再执行这段本地 template/input/Bag mechanics，也不拆第二 capture/command。

### Exact production write set

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudGiveItemLocalServiceClient.java`

### Exact test write set

- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogGiveItemTurnContractTest.java`
- 本报告。

其余两仓文件全部只读；尤其禁止修改 DHXY、protocol、dispatcher、Task、Server/routes、POM、其它 Service/client/test
或报告。保护共享 dirty/untracked，不回滚、覆盖、清理、提交或执行其它 Git mutation。

### Frozen result contract

- Client 对每个合法调用创建一个 UUID、一个 `TurnLocalServiceCall`、一个 command，operation 必须为
  `GIVE_ITEM_FROM_OPEN_DIALOG`，arguments 只含 exact `targetItemTemplate/knownBagIndex`，零 retry/fallback。
- completed local JSON 使用 TURN-10CR Repair #1 固定的 closed shape：
  `{"state":"GIVEN|GIVE_OPTION_NOT_FOUND|GIVE_ITEM_FAILED|INTERRUPTED"}`；strict mapper 必须拒绝 unknown/duplicate/
  missing/null/numeric/coerced/scalar/trailing JSON。
- exact mapping：`GIVEN -> GIVE_ITEM_DONE`；`GIVE_OPTION_NOT_FOUND -> GIVE_OPTION_NOT_FOUND`；
  `GIVE_ITEM_FAILED -> GIVE_ITEM_FAILED`；`INTERRUPTED` 或 turn `STOPPED -> INTERRUPTED`；FAILED/uncertain/command
  non-completed 一律 fail-closed 为 `GIVE_ITEM_FAILED`，不得伪成功或自动 retry。
- `itemToGive == null` 保持旧行为：零 command，返回 `GIVE_ITEM_FAILED`。
- 删除 `DialogService` 本路径对 Cloud-local `GiveItemService`、`OPTION_GIVE_TEXT`、direct click/800ms mechanics 的
  依赖；其它 dialog detection/story/option/fallback 顺序和 public result 语义完全不动。

### Named-test acceptance

- `DialogGiveItemTurnContractTest` 必须调用 production client，并通过 production `DialogService` 的真实 give-option
  分支或等价可审 public path 证明上述四态映射；不能只测一个复制的 mapper/helper。
- 至少覆盖：GIVEN success、entry missing、item failure、interrupted、STOPPED、uncertain/command failure、null item；
  每条可执行 case 均断言一个 UUID、一个 command、零 retry、exact operation/arguments。
- 覆盖 strict JSON malformed/duplicate/unknown/numeric/scalar 等代表性负例；异常或 fail-closed 后仍为零 retry。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级待所有 Java writers 稳定后
  运行 `mvn -q -Dtest=DialogGiveItemTurnContractTest test` 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-16 parent-frozen-brief -->

## CLAIMED - 2026-07-15 23:15:34 EDT

- Worker role: `CR271 TURN-16 implementation Worker`; parent is the only manager/final reviewer, and this Worker is
  not a reviewer.
- Confirmed the latest `TURN-10CR` true EOF is `PARENT RE-REVIEW - REPAIR #1 SOURCE + TEST SOURCE PASSED`, with
  `P0/P1/P2=0/0/0`; the frozen four-state `{"state":"<ENUM>"}` prerequisite is satisfied.
- Exact write set is limited to Cloud `DialogService.java`, new `CloudGiveItemLocalServiceClient.java`, new
  `DialogGiveItemTurnContractTest.java`, and this report. All other files in both repositories remain read-only.
- Shared dirty/untracked work is preserved. No rollback, cleanup, formatting, commit, or other Git mutation will be
  performed; no Maven, JUnit, compile, runtime, application, server, Task, UI, capture, or input will be run.

<!-- TRUE_EOF: TURN-16 worker-claimed -->

## SOURCE + TEST SOURCES DELIVERED - 2026-07-15 23:30:02 EDT

### Delivered files and SHA-256

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/DialogService.java`
  - `1621AF8F6588D2BF82FEC1852A775DDB0BF7A54739FBD7747BB282F6D65361EF`
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudGiveItemLocalServiceClient.java`
  - `422BB278984B6EF82A50B4B69ED3D90D10EF4A34B41339CA623656C8DBC4CE79`
- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/DialogGiveItemTurnContractTest.java`
  - `916DCAB4A44070507E6E6C4203FD7754B5BCDEBAB0E9AA49B3DE3B0F83DB4282`
- 本报告已追加 claim 与本交付段；报告自身不记录 self-referential SHA。

### Source evidence

- `DialogService` 保持既有 dialog detection、story/option 分流和 public `handleDialog(...)` 顺序；仅将
  `GIVE_ITEM_IF_AVAILABLE` 的既有 helper 改为调用一次 `CloudGiveItemLocalServiceClient`。`itemToGive == null`
  仍返回 `GIVE_ITEM_FAILED`，且不创建 GiveItem UUID/command。
- `CloudGiveItemLocalServiceClient.executeFromOpenDialog(...)` 只构造一个
  `TurnLocalServiceCall(GIVE_ITEM_FROM_OPEN_DIALOG)`；参数 union 只填
  `TurnGiveItemOperationArguments(targetItemTemplate, knownBagIndex)`，`fullWindowFailureEvidence=false`，随后只调用一次
  production `TurnGameClient.localService(...)`。源码无循环、retry、fallback 或第二 command/capture。
- completed JSON 只接受 `{"state":"GIVEN|GIVE_OPTION_NOT_FOUND|GIVE_ITEM_FAILED|INTERRUPTED"}`。production
  mapper 开启 duplicate、unknown、trailing、missing/null creator 与 numeric-enum 拒绝，并关闭 scalar coercion；
  private result record 还要求 `state` 非空。
- 映射精确为：`GIVEN -> GIVE_ITEM_DONE`；entry miss/item fail 保持
  `GIVE_OPTION_NOT_FOUND/GIVE_ITEM_FAILED`；local `INTERRUPTED` 与 confirmed turn `STOPPED` -> `INTERRUPTED`；
  turn `FAILED`、`DUPLICATE_OR_UNCERTAIN` 和全部 command non-completed -> `GIVE_ITEM_FAILED`。
- 本路径已无 Cloud-local `GiveItemService`、`OPTION_GIVE_TEXT`、`dialog:giveItemFlow`、direct give、give-entry
  click 或 800ms mechanics 引用；其它 Dialog branches 未改。

### Test-source evidence

- `DialogGiveItemTurnContractTest` 使用 production `TurnGameClient`、production
  `CloudGiveItemLocalServiceClient`，并从 production `DialogService.handleDialog(...)` 的真实
  `GIVE_ITEM_IF_AVAILABLE` option branch 进入；仅用 scripted command port 和固定 OPTION detection 替代外部 turn/
  dialog observation。
- 四个 completed state 分别断言 `GIVE_ITEM_DONE`、`GIVE_OPTION_NOT_FOUND`、`GIVE_ITEM_FAILED`、
  `INTERRUPTED`；另覆盖 turn `FAILED`、confirmed `STOPPED`、outcome uncertain，以及 `BUSY`、duplicate actionId、
  timeout uncertain、interrupt uncertain 四种 command non-completed。
- null item 断言 detection 后 UUID=0、GiveItem command=0。每个可执行 case 均断言 UUID=1、command=1、
  exact device/window、120000ms timeout、单 `LOCAL_SERVICE` step、exact operation/arguments、其它 argument union 全空、
  `fullWindowFailureEvidence=false`，从而锁住零 retry/fallback。
- strict JSON 负例覆盖 unknown field、unknown enum、duplicate、missing、null、numeric enum、quoted numeric/boolean
  coercion、top-level scalar 与 trailing token；每例通过 production parser 抛出并保持 UUID=1、command=1。

### Baseline reconciliation

- 已核对 `docs/业务逻辑.md` 的“`五环唤醒 / Prepared Action 记录`”：交鞋 option dialog 属于五环自身业务
  dialog，必须按 operation/template 语义消费；本卡未改变其它 dialog interest、route dialog 或 phase 语义。
- 已核对 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `DialogService.tryGiveItemFromCurrentOptionDialog(...)` 四分支，以及 TURN-10CR Repair #1 true EOF 父级
  `P0/P1/P2=0/0/0`：entry miss、entry wait interrupted、direct give false/true 仍逐态对应，机械顺序整体下沉到永久本地
  whole macro，Cloud caller 只消费四态结果。
- **无已批准业务差异；按 `696a12b0` 基线等价迁移。**

### Gates not run by Worker

- 按父级冻结禁令，本 Worker 未运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或
  input；未执行 Git mutation，也未启动任何真实桌面/游戏动作。
- 待父级在 Java writers 稳定后运行
  `mvn -q -Dtest=DialogGiveItemTurnContractTest test` 与适用 Cloud compile/build。本 Worker 仅交付 source/test
  source，等待父级 source/assertion review 和 fresh gate 结果。

<!-- TRUE_EOF: TURN-16 source-and-test-sources-delivered -->

## PARENT SOURCE + TEST SOURCE REVIEW PASSED - 2026-07-15 23:38 EDT

- Parent independent review: `P0/P1/P2=0/0/0`; Worker assertions were treated only as delivery notes.
- `DialogService.java:228-230,1505-1521` preserves the real `handleDialog(...)` option branch and maps one
  `CloudGiveItemLocalServiceClient` result to the existing four public statuses. `itemToGive == null` returns
  `GIVE_ITEM_FAILED` before invoking the client.
- `CloudGiveItemLocalServiceClient.java:54-68` builds exactly one
  `LOCAL_SERVICE/GIVE_ITEM_FROM_OPEN_DIALOG` call with only the frozen GiveItem argument member and delegates once to
  production `TurnGameClient`; there is no loop, fallback, second command or capture.
- `CloudGiveItemLocalServiceClient.java:79-116` maps completed four-state JSON, confirmed STOPPED and all
  failed/uncertain/non-completed outcomes exactly as frozen. The production mapper rejects duplicate, unknown,
  trailing, missing/null, numeric/coerced and scalar payloads instead of inventing success.
- `DialogGiveItemTurnContractTest.java:51-162` enters through production `DialogService.handleDialog(...)` and covers
  the four states, STOPPED/FAILED/uncertain/non-completed commands, null item and strict malformed JSON.
  `:262-298` asserts one UUID, one command, one LOCAL_SERVICE step, exact operation/arguments and no other union arm.
- Independent SHA-256 values match the delivery report for all three files. Source searches confirm zero retained
  Cloud-local `GiveItemService`, give-entry template/direct-click or 800ms mechanics reference in this path.
- Result: source and named-test source review passed. The named Maven/JUnit test and applicable Cloud compile/build
  remain pending in the stable-writer cohort; this is not yet `CARD APPROVED/CLOSED`.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-16 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
