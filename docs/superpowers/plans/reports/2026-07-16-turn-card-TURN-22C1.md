# CR271 TURN-22C1 - Cloud TeamReturn named-test behavioral cleanup

## PARENT FROZEN CARD - EXTERNAL-B READY - 2026-07-16T08:59:40.918-04:00

- Card type: real implementation slice of TURN-22 Repair #3; not a helper or reviewer task.
- Status: `READY / CLAIM REQUIRED / FINAL TURN-22 GATE STILL WAITS TURN-28Q`.
- Owner after claim: CR271 External Worker B. Worker cannot approve this card.
- Business authority: `docs/业务逻辑.md` and `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

## Exact modify write set

1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`.
2. This append-only child card.

Initial test snapshot: 1658 lines, SHA-256
`2d2907592e96d3c44e4ae239a8f569adba785568b19309d3f35ce90cb49e9496`.

Everything else is read-only, including Cloud production/assembly/protocol/POM and all DHXY production/tests,
TURN-28P/28Q files, TURN-22 original card, A/C/D write sets and both repositories' dirty/untracked bytes.

## Frozen implementation contract

1. Rewrite only `sourceGateTemplateParityAndPermanentLocalServiceAllowlistStayClosed` so the retained test proves
   behavior/resources rather than reading Java or Markdown text. Remove reads of
   `src/main/java/.../TeamReturnService.java`, `CloudTeamReturnPortAssembly.java` and the authoritative plan.
2. Preserve the real resource parity assertions for member/leader PNG bytes and SHA, and preserve the exact
   `TurnLocalOperation` enum-set assertion. Rename the method to describe those retained behavioral/resource checks.
3. Remove the now-unused `occurrences(String,String)` helper and imports only when their whole-file usage becomes
   zero. Do not touch unrelated pre-existing reflection fixtures in this slice.
4. Preserve existing real assembly/JSON click assertions: one INPUT `CLICK_LEFT`, `clickDelayMs=150`,
   `queueHoldMs=500`, no WAIT/frame, one command, one UUID, closed terminal/uncertain fail-closed and zero retry.
5. Do not add a replacement source-string/SHA-only guard, sibling classpath, copied DHXY class, wrapper, retry,
   session, owner, ledger, TTL or durable workflow.

## Delivery

External B must first append `EXTERNAL-B CLAIMED` at physical EOF. Completion requires one
`EXTERNAL-B SOURCE+TEST DELIVERED` with final SHA and exact retained/removed evidence, then stop editing. No Maven,
JUnit, compile, package, runtime, application, server, Task, UI, capture, input or Git mutation while other writers
are active. Parent source/test-source review and TURN-22 final gates remain separate.

**无已批准业务差异；按基线等价迁移。**

<!-- TRUE_EOF: TURN-22C1 PARENT FROZEN EXTERNAL-B READY CLAIM-REQUIRED ONE-TEST-FILE BEHAVIORAL-CLEANUP 2026-07-16T08:59:40.918-04:00 -->

## EXTERNAL-B CLAIMED - 2026-07-16T09:04:43-04:00

- Implementation Worker：**CR271 External Worker B**;非 reviewer,不能批准本卡;父级唯一 manager/final reviewer。本段不含 `APPROVED/CLOSED`,不自批。
- 身份(诚实自报,非平台权威真值):Claude Code 会话 `aa951b1e-8f04-4f92-b6e0-de08af49c39a`(UUIDv4,**非**平台 spawn 的 `019f…` UUIDv7);自选临时 nickname `Kepler`。权威 agent id/nickname 以平台 spawn 记录为准,父级可在本卡追加 `CLAIM IDENTITY CORRECTION` 承接同一 ownership/写集/禁令。lane 报告不构成领取;本段为唯一领取依据。
- 已**完整读取本卡全部 44 行**(card type/status/owner/business authority/exact modify write set/frozen implementation contract 1-5/delivery)。确认这是 TURN-22 Repair #3 的真实实现切片,非 helper/reviewer 任务。
- **领取时快照逐字核验一致**:`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java` = **1658 行**,SHA-256 `2d2907592e96d3c44e4ae239a8f569adba785568b19309d3f35ce90cb49e9496` —— 与卡内 Initial test snapshot 相同。
- **写集(恰 2 项)**:① 上述 Cloud 测试文件;② 本 append-only 子卡。其余全部只读:Cloud production/assembly/protocol/POM、全部 DHXY production/tests、TURN-28P/28Q 文件、TURN-22 原卡、A/C/D 写集、两仓 dirty/untracked 字节。本切片与 A 的 28Q、C 的 34A、D 的 34BT1 写集互斥。
- **基线**:`docs/业务逻辑.md` + `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。无已批准业务差异。
- **接受冻结契约 1-5**:①仅重写 `sourceGateTemplateParityAndPermanentLocalServiceAllowlistStayClosed`,使保留的测试**证明行为/资源而非读取 Java 或 Markdown 文本**;移除对 `src/main/java/.../TeamReturnService.java`、`CloudTeamReturnPortAssembly.java` 与权威计划的读取;②**保留** member/leader PNG 字节与 SHA 的真实 resource parity 断言,**保留**精确的 `TurnLocalOperation` enum-set 断言,并把方法**改名**以描述这些保留的行为/资源检查;③仅当 `occurrences(String,String)` helper 与相关 import 的**全文件用量归零**时才删除;**不动**本切片无关的既有 reflection fixture;④**保留**既有真实 assembly/JSON click 断言:一个 INPUT `CLICK_LEFT`、`clickDelayMs=150`、`queueHoldMs=500`、无 WAIT/frame、一 command、一 UUID、closed terminal/uncertain fail-closed、零 retry;⑤**不新增**替代的 source-string/SHA-only guard、sibling classpath、复制的 DHXY 类、wrapper、retry、session、owner、ledger、TTL 或 durable workflow。
- **已定位靶点(领取时行号)**:`:640` 方法签名;`:641-645` `Path.of("src/main/java/com/bot/dhxy/service/TeamReturnService.java")`、`CloudTeamReturnPortAssembly.java` 与两次 `Files.readString(...)`;`:661-664` 对 `assemblySource` 的 `occurrences(...)` 文本计数;`:701-702` 读取 `../DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`;`:1386` `occurrences(String,String)` helper。**保留**:`:677-683` 四个真实 PNG 字节读取(member/leader,Cloud/DHXY)及其 SHA parity、`TurnLocalOperation` enum-set 断言。
- **禁令**:不扩写集;其它 writer 活动期间**不跑 Maven/JUnit/compile/package**;不启 runtime/application/server/Task/UI/capture/input;**禁一切 Git mutation**;只从当前字节增量编辑;保护两仓 dirty/untracked。基线 DHXY HEAD `0114604e`、cloud-brain `3b988ca` 未动。我不再触碰 TURN-28P(Internal Euler)、TURN-28S1(已交付、owner 已释放、双 reviewer 进行中)与 TURN-28 其余写集。
- 交付:完成后在本卡追加**一次** `EXTERNAL-B SOURCE+TEST DELIVERED` + 最终 SHA + 精确 retained/removed 证据,然后停止编辑;父级 source/test-source review 与 TURN-22 最终门(仍等 TURN-28Q)独立进行,我不冒充其通过。

<!-- TRUE_EOF: TURN-22C1 EXTERNAL-B CLAIMED ONE-TEST-FILE BEHAVIORAL-CLEANUP Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T09:04:43-04:00 -->

## EXTERNAL-B SOURCE+TEST DELIVERED - 2026-07-16T09:10:29-04:00

按冻结契约完成 TURN-22C1。**本段不是 approval**;不含 `APPROVED/CLOSED`;不自批;请父级独立 source/test-source review。

**写集(恰 2 项,无扩张)**:① Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`;② 本卡。其余零改动:未碰 Cloud production/assembly/protocol/POM、任何 DHXY production/tests、TURN-28P/28Q 文件、TURN-22 原卡、A/C/D 写集;两仓 dirty/untracked 完好。

| 文件 | 领取时 | 交付时 |
|---|---|---|
| `TeamReturnTurnContractTest.java` | 1658 行 / `2d2907592e96d3c44e4ae239a8f569adba785568b19309d3f35ce90cb49e9496` | **1612 行** / **`d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab`** |

净删 46 行。

**移除证据(契约 1/3,grep 实证全为 0)**:
- 原 `:641-645`:`Path.of("src/main/java/com/bot/dhxy/service/TeamReturnService.java")`、`Path.of(".../CloudTeamReturnPortAssembly.java")` 与两次 `Files.readString(...)` —— 已删。全文件 `TeamReturnService.java`=**0**、`CloudTeamReturnPortAssembly.java`=**0**、`Files.readString`=**0**、`serviceSource`=**0**、`assemblySource`=**0**。
- 原 `:646-658`:对拼接 `production` 串的 10 个 forbidden 子串 `assertFalse(production.contains(...))` 循环 —— 已删。
- 原 `:659`:`assertFalse(assemblySource.contains("TurnStepType.LOCAL_SERVICE"))` —— 已删。
- 原 `:661-664`:`occurrences(assemblySource, "turnGameClient.bind(" / "boundClient.latestWindowMetadata(" / "boundClient.capture(" / "boundClient.execute(")` 四条文本计数 —— 已删。
- 原 `:666-675`:用 `serviceSource.indexOf(...)` 推断 `clickReturnTeamIfPresent → observeButton → ensureSheYaoXiangActive → observeButton → clickReturnButton` 顺序的五个 `indexOf` + 四个 `assertTrue` —— 已删。
- 原 `:701-708`:`Files.readString(Path.of("../DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md"))` 与对 `` `BagService` / `UICleanerService` / `GiveItemService` / `QuestManagerService` `` 的 `plan.contains(...)` 循环 —— 已删。全文件 `card-plan.md`=**0**。
- 原 `:1386-1394` `private static int occurrences(String,String)` helper —— **按契约 ③ 先验证全文件用量归零后**才删(删调用点后仅剩定义、零调用者)。现 `occurrences`=**0**。

**保留证据(契约 2/4)**:
- **未删任何 import**:逐项复核用量后确认仍被使用 —— `java.nio.file.Files`(`Files.readAllBytes` 4 处)、`java.nio.file.Path`(4)、`java.io.IOException`(`:649 throws` + `:1389 catch`)、`java.util.List`(16)、`java.util.Set`(2)。契约 ③ 只允许在用量归零时删,故均保留。
- **真实 resource parity 原样保留**:四次 `Files.readAllBytes` 读 Cloud `src/main/resources/images/template/status/gui.png`、`zhao.png` 与 DHXY `../DHXY/images/template/status/gui.png`、`zhao.png`;`assertArrayEquals(cloudMember, dhxyMember)`、`assertArrayEquals(cloudLeader, dhxyLeader)`;`assertEquals(MEMBER_TEMPLATE_SHA256, sha256(cloudMember).toUpperCase())`、`assertEquals(LEADER_TEMPLATE_SHA256, sha256(cloudLeader).toUpperCase())`。
- **精确 `TurnLocalOperation` enum-set 断言原样保留**:9 个常量(`BAG_RETURN_ITEM`/`BAG_USE_INCENSE`/`UI_CLEAN_ALL`/`UI_CLOSE_GENERIC_WINDOWS`/`UI_CLEAN_LIGHTWEIGHT`/`UI_CLOSE_MAP_SEARCH_INPUT_BY_X2`/`GIVE_ITEM_FROM_OPEN_DIALOG`/`QUEST_ACTIVATE`/`QUEST_CAPTURE_DETAIL`)与 `assertEquals(expectedOperations, Set.of(TurnLocalOperation.values()))`。
- **方法已改名**以描述保留的行为/资源检查:`sourceGateTemplateParityAndPermanentLocalServiceAllowlistStayClosed` → **`memberAndLeaderTemplateBytesMatchDhxyAndLocalOperationAllowlistStaysClosed`**(现 `:649`),并加 javadoc 说明为何资源/enum 是本测试真正拥有的不变量、而行为合同由本类的真实 assembly/JSON 测试证明。
- **既有真实 assembly/JSON click 断言未动**(契约 ④):一个 INPUT `CLICK_LEFT`、`clickDelayMs=150`、`queueHoldMs=500`、无 WAIT/frame、一 command、一 UUID、closed terminal/uncertain fail-closed、零 retry —— 全部由本类其它测试原样持有。
- **未动**本切片无关的既有 reflection fixture(契约 ③)。

**未新增(契约 ⑤)**:无替代 source-string/SHA-only guard、无 sibling classpath、无复制的 DHXY 类、无 wrapper/retry/session/owner/ledger/TTL/durable workflow。diff 仅限上述冻结的一文件删除/改名/javadoc,无格式化、无无关清理。

**未运行门**:本切片**未运行** Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input(其它 writer 活动中);**未执行任何 Git mutation**;DHXY HEAD `0114604e`、cloud-brain `3b988ca` 未动。TURN-22 最终门仍等 TURN-28Q,父级 review 独立进行,我不冒充其通过。自此停止编辑。

<!-- TRUE_EOF: TURN-22C1 EXTERNAL-B SOURCE+TEST DELIVERED one-test-file 1658->1612 sha d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T09:10:29-04:00 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #1 - PASSED - 2026-07-16T09:13:36.373-04:00

- 父级独立读取当前 1612 行测试、冻结契约与原卡交付证据，复算 SHA-256
  `d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab`，与 delivery 一致。
- 结论：**`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`**。External B 的 TURN-22C1
  implementation owner 释放；本切片进入 independent review/build pending，尚非 parent TURN-22 pass。
- 原 Java/Markdown source scan、`Files.readString`、production substring counts、plan text lookup 与
  `occurrences(...)` helper 均归零；未引入替代 source guard、sibling classpath 或复制类。
- `memberAndLeaderTemplateBytesMatchDhxyAndLocalOperationAllowlistStaysClosed` 只保留真实 member/leader PNG
  byte+SHA parity 与 exact `TurnLocalOperation.values()` 集合；其它真实 assembly/JSON cases 仍覆盖单
  `CLICK_LEFT`、`clickDelayMs=150`、`queueHoldMs=500`、一 command/UUID、无 frame、terminal/uncertain
  fail-closed 与零 retry。
- 未发现写集扩大、业务语义变化或 retry/session/owner/ledger/TTL/durable workflow。Java writers仍活动，
  本轮不运行 Maven/JUnit/compile/runtime/input，不做 Git mutation。

<!-- TRUE_EOF: TURN-22C1 PARENT REVIEW-1 PASSED P0P1P2=0/0/0 SOURCE-TEST-SOURCE-REVIEW-PASSED OWNER-RELEASED INDEPENDENT-REVIEW-BUILD-PENDING 2026-07-16T09:13:36.373-04:00 -->
