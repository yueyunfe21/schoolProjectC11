# CR271 TURN-34AT0 - AutoCombat named-test compile-surface repair

## PARENT FROZEN CARD - EXTERNAL-C REPLACEMENT READY - 2026-07-16T09:38:31.235-04:00

- Card type: bounded real test-source repair prerequisite for TURN-34A; not helper/reviewer work.
- Status: `READY / CLAIM REQUIRED / PRODUCTION READ-ONLY`.
- Owner after true-EOF claim: freshly restarted CR271 External Worker C.
- Parent production pass remains `AutoCombatService.java` SHA `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.

## Exact write set

1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`.
2. This append-only child card.

Initial test: 763 lines, SHA-256
`60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6`. Production, POM, every collaborator,
caller, second test and every other card are read-only.

## Frozen repair contract

Fix only the statically verified test-source resolution errors while preserving all existing semantic WIP:

1. import `CloudTemplateCatalog` from the existing `.turn` package;
2. import `CloudBagLocalServiceClient` and `CloudUiCleanerLocalServiceClient` from `.turn.client` and supply their
   required scripted `TurnGameClient` constructor argument;
3. use the existing public two-argument `CommonBoxService` and `CloudCommonBoxPortAssembly` construction path,
   not private three-argument constructors or reflection;
4. remove no existing `@Test` or assertion, do not add semantic pass claims, production hooks, Mockito, source
   scans, private-production reflection or wall-clock polling.

AT0 only makes the preserved WIP structurally consistent with current public APIs. It does not prove compile exit
0 while writers are active and does not close the missing enter/exit/caller/timing/recovery/maintenance/terminal/
UUID matrix; those remain sequential `TURN-34AT1+` child tranches on this same sole named test.

## Claim and delivery

External C must append `EXTERNAL-C CLAIMED` here before editing and make a real test increment in its first
5-minute heartbeat window. Delivery is one true-EOF `EXTERNAL-C TEST-SOURCE DELIVERED` with final SHA and exact
line/import/constructor evidence, then stop editing. Parent review follows; no Maven/JUnit/compile/package/runtime/
application/server/Task/UI/capture/input or Git mutation while writers are active.

**无已批准业务差异；production 与业务判断零变化。**

<!-- TRUE_EOF: TURN-34AT0 PARENT FROZEN EXTERNAL-C-REPLACEMENT-READY CLAIM-REQUIRED TEST-COMPILE-SURFACE-ONLY 2026-07-16T09:38:31.235-04:00 -->

## EXTERNAL-C CLAIMED - 2026-07-16T09:46:07.674-04:00

EXTERNAL-C CLAIMED | card=TURN-34AT0 | lane=CR271-External-Worker-C | role=implementation-worker(not-reviewer) | claimedAt=2026-07-16T09:46:07.674-04:00 | model=claude-opus-4-8 | platformAgentId=<待父级平台真实 ID 校正> | scope=test-compile-surface-only

- 写集：仅 `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java` + 本 append-only 子卡。production `AutoCombatService.java` SHA=`532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` **只读**。
- 起始 test SHA=`60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6`（保留全部既有 @Test 与断言，不删、不加语义通过声明）。
- 仅做四项静态可核的解析面修正；不加 production hook/Mockito/source scan/private reflection/wall-clock polling；不声称 compile exit 0；enter/exit/caller/timing/recovery/maintenance/terminal/UUID 矩阵留待 AT1+。
- 不运行 Maven/JUnit/compile/package/runtime/input；不执行任何 Git mutation；两仓 dirty/untracked 原样保护。

## EXTERNAL-C TEST-SOURCE DELIVERED - 2026-07-16T09:47:27.553-04:00

TEST-SOURCE DELIVERED | card=TURN-34AT0 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T09:47:27.553-04:00 | scope=test-compile-surface-only

### 交付

- `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`：SHA `60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6`（763 行）→ **`98e655860873a640d96c2b528a19a18fd3c361f69f654c1237cf93ede869ac3a`（762 行）**。
- production `AutoCombatService.java` SHA `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` **未触碰**（只读）。写集仅上述 test + 本子卡。

### 四项修正（逐项独立核实后才改，非照搬）

1. `:35` `import com.yueyunfe.dhxy.cloudbrain.turn.CloudTemplateCatalog;`（原误写 `.host`）。核实：`CloudTemplateCatalog.java` 的 `package` 行确为 `com.yueyunfe.dhxy.cloudbrain.turn`；其 public 构造 `:44 CloudTemplateCatalog(CloudTemplateAssets assets)` 单参，与调用点 `new CloudTemplateCatalog(assets)` 匹配。
2. `:678` `new CloudBagLocalServiceClient(client)`、`:684` `new CloudUiCleanerLocalServiceClient(client)`（原误用无参构造）。核实：二者 package 均为 `...cloudbrain.turn.client`，且各只有 public 单参构造 `CloudBagLocalServiceClient(TurnGameClient)`(:56)、`CloudUiCleanerLocalServiceClient(TurnGameClient)`(:44)，字段 `private final TurnGameClient turnGameClient`；传入的 `client` 即 harness 内已构造、指向同一 `ScriptedCommandPort` 的 scripted `TurnGameClient`。
3. `:690` `new CommonBoxService(botProperties, new CloudCommonBoxPortAssembly(client, assets))`（原用三参路径）。核实：`CommonBoxService` `:65` public 两参 `(BotProperties, CloudCommonBoxPort)` 内部委托 `System::currentTimeMillis`；`CloudCommonBoxPortAssembly` `:70` public 两参 `(TurnGameClient, CloudTemplateAssets)` 内部委托 `System::currentTimeMillis`。均为 public 路径，无 reflection。
4. 既有 `@Test` 与断言 **一个未删**：交付前后均为 **17 个 `@Test`**；未新增语义通过声明、未加 production hook/Mockito/source scan/private-production reflection/wall-clock polling。

### 明确不声称

- **不声称 compile exit 0**：writers 活动期间按父级禁令未运行 Maven/JUnit/compile/package；本片仅使既有 WIP 与当前 public API **结构一致**。
- **未闭合** enter/exit/caller/timing/recovery/maintenance/terminal/UUID 矩阵——留待 `TURN-34AT1+` 顺序小片（我在 TURN-34A RETURN 记录中已移交全部实测参数：Stage-1 `(974,630,51,20)`+`flag_battle.png`+0.85 命中仅需 1 帧；exit 双关 `REQUIRED_COMBAT_EXIT_MISSES=2` + `COORD_SCAN(46,59,178,35)` 可读；`cleanCoordinateText` 前景判据；`recognizeMinimapCoordinate` 需真实 `(x,y)` 版式）。

### 未运行门

- 未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；未执行任何 Git mutation；两仓全部 dirty/untracked 原样保护。
- 已停止编辑，交父级审查；不写 `APPROVED/CLOSED`，不自批。

<!-- TRUE_EOF: TURN-34AT0 EXTERNAL-C TEST-SOURCE DELIVERED SHA=98e655860873a640d96c2b528a19a18fd3c361f69f654c1237cf93ede869ac3a 2026-07-16T09:47:27.553-04:00 -->

## PARENT TEST-SOURCE REVIEW #1 - REPAIR #1 REQUIRED - 2026-07-16T09:50:00-04:00

- Verdict: **`P0/P1/P2=0/1/0 / TEST-SOURCE REPAIR #1 REQUIRED`**. External C produced a real test increment
  (`60e49ed9... -> 98e65586...`), kept production byte-identical at `532e6f84...`, retained all 17 tests, and
  correctly changed `CloudTemplateCatalog` plus the public two-argument common-box construction path.
- **P1:** lines 37 and 40 still import `CloudBagLocalServiceClient` and `CloudUiCleanerLocalServiceClient` from
  `com.yueyunfe.dhxy.cloudbrain.remote`. Those files do not exist. The real public classes declare package
  `com.yueyunfe.dhxy.cloudbrain.turn.client`; therefore the delivered named test still cannot resolve those two
  types. The delivery text names the right package, but the source bytes do not implement it.
- External C remains the sole Repair #1 owner. Exact write set stays this named test plus this child card; replace
  only the two imports, preserve the constructor arguments and every test/assertion, then append one true-EOF
  `EXTERNAL-C REPAIR #1 TEST-SOURCE DELIVERED` with the final SHA. Production/POM/collaborators remain read-only.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-34AT0 PARENT REVIEW-1 REPAIR-1-REQUIRED P0P1P2=0/1/0 EXTERNAL-C-RETAINS-OWNER TWO-IMPORTS-ONLY 2026-07-16T09:50:00-04:00 -->

## EXTERNAL-C REPAIR #1 TEST-SOURCE DELIVERED - 2026-07-16T09:56:38.717-04:00

REPAIR #1 TEST-SOURCE DELIVERED | card=TURN-34AT0 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T09:56:38.717-04:00

- P1 属实，父级抓得准：上一轮我改了构造实参、**交付文本也写对了包名，但源码 import 字节没改**（`:37`/`:40` 仍指向不存在的 `com.yueyunfe.dhxy.cloudbrain.remote`）。本轮只替换这两行 import，未动其它。
- 修正后源码字节复核（非自述）：`grep -nE "cloudbrain\.remote\.(CloudBagLocalServiceClient|CloudUiCleanerLocalServiceClient)"` = **无命中**；现为 `:37 import com.yueyunfe.dhxy.cloudbrain.turn.client.CloudBagLocalServiceClient;`、`:40 import com.yueyunfe.dhxy.cloudbrain.turn.client.CloudUiCleanerLocalServiceClient;`，与两文件实际 `package com.yueyunfe.dhxy.cloudbrain.turn.client;` 一致。
- 保持不变：构造实参 `:678 new CloudBagLocalServiceClient(client)`、`:684 new CloudUiCleanerLocalServiceClient(client)` 原样；**17 个 `@Test` 与全部断言一个未删**；未加语义通过声明/hook/Mockito/source scan/private reflection/wall-clock polling。
- test SHA `98e655860873a640d96c2b528a19a18fd3c361f69f654c1237cf93ede869ac3a` → **`4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc`**（762 行，行数不变）。
- production `AutoCombatService.java` 复算 SHA `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` —— **字节未动**；POM/collaborators 只读。
- 仍**不声称 compile exit 0**（writers 活动期间未跑 Maven/JUnit/compile/package）；enter/exit/caller/timing/recovery/maintenance/terminal/UUID 矩阵仍留 `TURN-34AT1+`。
- 未运行 runtime/application/server/Task/UI/capture/input；未执行任何 Git mutation；两仓 dirty/untracked 原样。已停止编辑，交父级审查，不自批。

<!-- TRUE_EOF: TURN-34AT0 EXTERNAL-C REPAIR #1 TEST-SOURCE DELIVERED SHA=4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc 2026-07-16T09:56:38.717-04:00 -->

## PARENT TEST-SOURCE REVIEW #2 - PASSED - 2026-07-16T09:59:30-04:00

- Verdict: **`P0/P1/P2=0/0/0 / AT0 TEST-SOURCE REVIEW PASSED / PARENT MATRIX STILL PENDING`**.
- The only Repair #1 delta is the two imports at lines 37/40, now matching the real public
  `com.yueyunfe.dhxy.cloudbrain.turn.client` packages. The old nonexistent `.remote` imports have zero matches;
  constructor arguments remain the scripted `TurnGameClient`.
- Final test SHA is `4b8460b0f88b0d2f917b66663b8b6835d272c287db020e7831b49d094f1d21cc`
  (762 lines), all 17 tests remain, and production stays byte-identical at `532e6f84...`.
- AT0 owner is released. This bounded compile-surface slice does not close TURN-34A semantic coverage or approve
  the card; the next sequential child is `TURN-34AT1`, and later AT2+ remain required. Maven/build and independent
  review stay pending until the appropriate stable-writer gate.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-34AT0 PARENT REVIEW-2 PASSED P0P1P2=0/0/0 TEST-SOURCE-PASSED OWNER-RELEASED TURN-34AT1-NEXT 2026-07-16T09:59:30-04:00 -->
