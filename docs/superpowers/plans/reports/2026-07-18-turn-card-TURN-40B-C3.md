# CR271 TURN-40B-C3 Xiuluo Coordinate And OCR Cloud Form

## Canonical State

- Status: `READY / ZERO OWNER / UNASSIGNED`.
- Type: `WHOLE-CARD SOURCE+TEST IMPLEMENTATION`.
- Parent: `TURN-40B`; prerequisite `TURN-40BP1` Review #7 passed `0/0/0`.
- Any eligible Worker may claim this whole card by appending the first valid claim at the physical EOF and
  rereading it. The ledger announces availability only and does not assign an owner.

## Exact Write Set

Only these Cloud repository paths may change:

- Modify `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`.
- Modify `src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java` only to expose
  `coordinatePlausible` as `public static` and `mapTransform` as `public static` returning a defensive copy; no
  algorithm, map loader, constant, internal snapshot mutation or other API change.
- Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java`.

DHXY, mirrored protocol, `MiniMapPointResolver`, Navigation, runtime/factory/configuration and every other path are
read-only for this card. C3 is disjoint from C1 and may run concurrently.

## Frozen Implementation Contract

- Internalize randomized point behavior with the exact `(1,1)` baseline range.
- Internalize approach-coordinate behavior using `TurnWindowMetadata.windowRect` for the window base and preserve
  direction stepping plus the 龙窟/凤巢 cave-origin special case exactly. Do not add a `MiniMapPointResolver` method.
- Replace logical-coordinate plausibility with `ObjectiveTextRecognizer.coordinatePlausible` under the established
  margin-80 semantics. Amendment #2 authorizes exposing that existing pure function as `public static`.
- Internalize approach-coordinate behavior using the read-only defensive-copy result of
  `ObjectiveTextRecognizer.mapTransform`; never expose or mutate the internal transform array.
- Replace path-based OCR with turn capture-frame plus `LocalOcrClient`; remove the `imagePath` dependency without
  adding disk-path shims, constant/null OCR or another OCR provider/store.
- Preserve `docs/业务逻辑.md` and baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`: Xiuluo phase order,
  tracker shortcut/fallback, navigation/entry/combat order, retries, timers, maintenance and startup semantics may
  not change. `无已批准业务差异；按基线等价迁移`.

## Test And Delivery Gate

- Extend only `XiuluoWholeTaskTurnContractTest` to retain the exact randomize/approach/cave/plausibility/OCR mapping
  and prove no path-based OCR or new resolver seam remains.
- After active Java writers are stable, run only the authorized named
  `XiuluoWholeTaskTurnContractTest` and the applicable Cloud compile gate. Do not start
  runtime/application/server/Task/UI/capture/input.
- Delivery must append exact production/test SHAs, baseline comparison, changed-path list, named-test/compile
  evidence or explicit shared-writer deferral, and `无已批准业务差异；按基线等价迁移` to this same card.

<!-- TRUE_EOF: TURN-40B-C3 READY ZERO-OWNER UNASSIGNED WHOLE-CARD-SOURCE+TEST XIULUO-COORDINATE+OCR-CLOUD-FORM EXACT-TWO-FILE-WRITESET DISJOINT-WITH-C1 2026-07-18T13:35:00-04:00 -->

## PARENT AVAILABILITY AUDIT - 2026-07-18T13:52:00-04:00

- Canonical state remains `READY / ZERO OWNER / UNASSIGNED`; no claim exists at this physical EOF.
- C1 is independently owned by External C and has real source movement. This C3 card remains disjoint and may be
  canonical self-claimed by any eligible idle Worker; it is not assigned or reserved for External A.
- External A has not ACKed `PARENT-TURN40BP1-PASS-C1-C3-PARALLEL-READY-1338` for two consecutive parent audits
  and is now recorded `COMMUNICATION_STALE`. That communication state does not close or block this READY card.

<!-- TRUE_EOF: TURN-40B-C3 READY ZERO-OWNER UNASSIGNED PARENT-AVAILABILITY-AUDIT NO-CLAIM DISJOINT-WITH-C1 A-COMMUNICATION-STALE NOT-ASSIGNED 2026-07-18T13:52:00-04:00 -->

## Parent Naming/Availability Correction - 2026-07-18 16:00 EDT

- This card, `TURN-40B-C3`, remains the current Xiuluo READY card: `READY / ZERO OWNER / UNASSIGNED`.
- It is not `TURN-38B3`; that older card is source-review passed/owner released and cannot be claimed again. External A's user-facing prompt conflating them is obsolete.
- No Worker is assigned or reserved. Any claim must target this exact physical EOF and follow the whole-card anti-race contract without asking the user for lane permission.

<!-- TRUE_EOF: TURN-40B-C3 READY ZERO-OWNER UNASSIGNED NAMING-CORRECTED NOT-TURN38B3 NO-CLAIM NOT-ASSIGNED DO-NOT-ASK-USER 2026-07-18T16:00:00-04:00 -->

## EXTERNAL-A WHOLE-CARD CANONICAL CLAIM - 2026-07-18T15:53:00-04:00

- claimant: `EXTERNAL-A`（heartbeat `dea947fe`，capacity `ENOUGH_WHOLE_CARD`）。
- claim basis: 本卡 physical EOF 于预检时=`READY / ZERO OWNER / UNASSIGNED`（13:52 PARENT AVAILABILITY AUDIT，无任何在先 claim；card mtime 06:20:48 / EOF audit 13:52 已完整读取）。预检与本 append 为两次独立工具调用；append 后立即回读 EOF 验证唯一最早 owner，若发现更早 claim 立即 canonical 自撤归还。
- scope acknowledgment: 精确两文件 Cloud 写集——Modify `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` + Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java`；DHXY/镜像 protocol/`MiniMapPointResolver`/Navigation/runtime-factory-configuration 及其它一切路径只读；与 C1/C2 零碰撞可并行。
- contract acknowledgment（frozen）：①randomized point 内化=精确 `(1,1)` baseline range；②approach-coordinate 内化=`TurnWindowMetadata.windowRect` window base+方向 stepping+龙窟/凤巢 cave-origin 特例逐字保留，**不加 MiniMapPointResolver 方法**；③logical-coordinate plausibility→`ObjectiveTextRecognizer.coordinatePlausible`（margin-80 语义）；④path-based OCR→turn capture-frame+`LocalOcrClient`，删 `imagePath` 依赖，禁 disk-path shim/常量-null OCR/第二 OCR provider/store；⑤`docs/业务逻辑.md`+`696a12b0` 修罗 phase 顺序/tracker shortcut-fallback/navigation-entry-combat 顺序/retries/timers/maintenance/startup 语义零改动。`无已批准业务差异；按基线等价迁移`。
- pre-write mandate: 动笔前先全文精读 AGENTS.md + docs/DHXY_CONTEXT.md + docs/业务逻辑.md 修罗章 + 计划 14-19 节 + 40BP1 passed 合同，再改 Java。
- discipline: 零 Git mutation；`D:\mavenProject\DHXY` 只读；无 runtime/UI/capture/input；C（C2）active Java writer 期间不运行 Maven（javac 单文件 parse 除外）；writer 稳定后仅跑授权 `XiuluoWholeTaskTurnContractTest`+适用 Cloud compile；不自批。交付=canonical append 本卡（SHA/baseline 对照/changed-path/test-compile 证据或 shared-writer deferral+`无已批准业务差异` 声明）。

<!-- TRUE_EOF: TURN-40B-C3 EXTERNAL-A WHOLE-CARD CLAIMED OWNER-A CAPACITY-ENOUGH TWO-FILE-WRITESET-ACKED FROZEN-CONTRACT-ACKED DISJOINT-C1-C2 PRE-WRITE-DOCS-MANDATED 2026-07-18T15:53:00-04:00 -->

## Parent Claim Audit - 2026-07-18 16:02 EDT

- claim accepted: External A 的 canonical claim 目标是正确的 `TURN-40B-C3`，prior READY/zero owner、精确两文件写集、C2 碰撞检查与 frozen contract ACK 均有效；A 为 C3 sole owner，状态 `SOURCE_ACTIVE`。
- concurrency correction: 16:00 parent naming correction 与 15:53 claim 并发；其目的只是区分已关闭 38B3 与本卡，不覆盖本卡物理后落的有效 claim。
- communication: A 旧 communication stale 暂保留，直到 STATUS EVENT 具名 ACK `PARENT-A-OBSOLETE-38B3-PROMPT-CORRECTION-1600` 与 `PARENT-A-TURN40B-C3-CLAIM-ACCEPTED-1602`。当前 claim 未满 10 分钟且尚无源码变化，不标 ACTIVE_STALE。
- gates: C2 与 C3 两名 active Java writer 并行且写集不交；父级不运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-40B-C3 PARENT-CLAIM-AUDIT ACCEPTED OWNER-A SOURCE-ACTIVE COMMUNICATION-STALE ACK-PENDING=1600+1602 DISJOINT-C2 NO-ACTIVE-STALE NO-MAVEN 2026-07-18T16:02:00-04:00 -->

## Parent Communication Recovery Audit - 2026-07-18 16:16 EDT

- External A 16:14 STATUS EVENT has explicitly ACKed `PARENT-A-OBSOLETE-38B3-PROMPT-CORRECTION-1600` and `PARENT-A-TURN40B-C3-CLAIM-ACCEPTED-1602`, and confirms the obsolete TURN-38B3 user-authorization prompt is closed.
- `COMMUNICATION_STALE` is cleared. A remains C3 sole owner/source active in recon; the exact two Cloud write-set files still match their claim-time SHA/mtime and no canonical delivery exists.
- C2/C3 write sets remain disjoint. Both Java writers are active, so parent does not run Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-COMMUNICATION-RECOVERY ACK=1600+1602 OWNER-A SOURCE-ACTIVE COMMUNICATION-NORMAL RECON NO-SOURCE-DELIVERY DISJOINT-C2 NO-MAVEN 2026-07-18T16:16:00-04:00 -->

## Parent Recon Activity Audit - 2026-07-18 16:23 EDT

- External A 16:19 STATUS EVENT reports exact baseline extraction from `696a12b0`: symmetric `(1,1)` randomization, approach logical offset `2` with Longku/Fengchao short-circuit, and margin-80 plausibility bounds.
- A remains C3 sole owner/source active/communication normal. Physical SHA/mtime for the exact two write-set files remains unchanged from claim time; no canonical delivery or source-review trigger exists.
- C2 is independently source active at production 10/13. Both disjoint Java writers remain active; parent does not run Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-RECON-ACTIVITY-AUDIT OWNER-A SOURCE-ACTIVE COMMUNICATION-NORMAL BASELINE-SEMANTICS-EXTRACTED RANDOMIZE-1+1 APPROACH-OFFSET-2 CAVE-SHORTCIRCUIT MARGIN-80 TARGETS-UNCHANGED NO-DELIVERY DISJOINT-C2 NO-MAVEN 2026-07-18T16:23:00-04:00 -->

## OWNER-A PLAN-CONTRACT QUESTION #1 - PLAUSIBILITY VISIBILITY SEAM - 2026-07-18T16:26:00-04:00

**问题（精确）**：合同第三点规定 `:3735` 站点改用 `ObjectiveTextRecognizer.coordinatePlausible`（margin-80）。经只读核验：该方法为 **package-private**（`static boolean`，`com.yueyunfe.dhxy.cloudbrain` 包，CR247 注明 "package view for other cloud readers"），而 `XiuluoTaskV2` 在 `com.bot.dhxy.task.xiuluo` 包——**Java 可见性上无法直接调用**。同包内两个替代载体同样 package-private：`MiniMapPointResolver.ocrFallbackCoordinatePlausible`（CR258）与 `QuestDetailTextRecognizer.recognize`（仅 DecisionEngine 调用）。本卡写集仅两文件（XiuluoTaskV2+test），`ObjectiveTextRecognizer`/`MiniMapPointResolver` 均只读，owner 无权改可见性；而在 XiuluoTaskV2 内化 plausibility 需 map-transform 快照=违反 CR258 单 owner/禁第二 loader。语义核验已完成：Cloud `isCoordinatePlausible` 与基线 `isLogicalCoordinatePlausible(...,80)` 数学一致（unknown map→true；`zeroOffset+logical*scale ∈ [-80, 1024/768+80]`；`COORDINATE_PLAUSIBILITY_MARGIN_PX=80`）。

**请父级裁定落点（三选）**：
- (a) 父级批准将 `ObjectiveTextRecognizer.coordinatePlausible` 单词修饰符 `static`→`public static`（一行可见性变更，语义零改动），并相应扩本卡写集一文件或由父级自行落该行；
- (b) 指定既有其它 public 可达路径（若 owner recon 有遗漏请指明）；
- (c) 其它父级裁定形态。

**不受阻部分继续推进**（randomize `(1,1)` 内化 / approach 内化含龙窟凤巢+windowRect / :3657 path-OCR→capture-frame+`LocalOcrClient.readWords` 重写）；仅 `:3666` 一处 boolean 过滤表达式待此裁定。零字节未动，prod 仍 `15a4a5f4`。

<!-- TRUE_EOF: TURN-40B-C3 OWNER-A PLAN-CONTRACT-QUESTION-1 COORDINATEPLAUSIBLE-PACKAGE-PRIVATE-UNREACHABLE OPTIONS-A-B-C SEMANTICS-VERIFIED-MARGIN-80 UNBLOCKED-PARTS-CONTINUE PROD-UNCHANGED-15a4a5f4 2026-07-18T16:26:00-04:00 -->

## Parent Plan-Contract Amendment #1 - 2026-07-18 16:32 EDT

- decision: approve Q1 option (a). Full source/symbol/caller audit found no existing public reachable plausibility path. The authoritative single maps.json owner is the existing pure `ObjectiveTextRecognizer.coordinatePlausible` function.
- exact write-set expansion: add `src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java`, restricted to changing the method modifier from package-private `static` to `public static`. Its body, margin-80 constants, transform snapshot/loader and every other API/byte remain unchanged.
- test gate: the existing `XiuluoWholeTaskTurnContractTest` must call/retain the cross-package public seam and exact margin-80 mapping. No extra test file, resolver method, transform loader, wrapper, reflection, duplicated formula or business-semantic change is authorized.
- collision/baseline: ObjectiveTextRecognizer is outside C2 and all current active write sets. Baseline behavior remains exact; `无已批准业务差异；按基线等价迁移`.
- communication: A remains sole owner/source active. Before implementing this amendment, next STATUS EVENT must ACK `PARENT-A-TURN40B-C3-CONTRACT-AMENDMENT1-1632`.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-PLAN-CONTRACT-AMENDMENT-1 OPTION-A-APPROVED OWNER-A SOURCE-ACTIVE ACK-PENDING=AMENDMENT1-1632 EXACT-3-FILE-WRITESET OBJECTIVE-PUBLIC-MODIFIER-ONLY NO-BODY+LOADER+CONSTANT-CHANGE NO-SECOND-SEAM NO-BUSINESS-DIFFERENCE DISJOINT-C2 NO-MAVEN 2026-07-18T16:32:00-04:00 -->

## OWNER-A PLAN-CONTRACT QUESTION #1 ADDENDUM - TRANSFORM ACCESS ALSO AFFECTS APPROACH SITE - 2026-07-18T16:36:00-04:00

**补充发现（只读 recon）**：可见性 seam 不止 `:3666` plausibility 一处——**`:2006` approach 内化同样不可实施**：合同第二点的方向 stepping 需 map transform（`zeroOffsetX/Y, scaleX/Y`），Cloud 单一 owner 出口为 `ObjectiveTextRecognizer.mapTransform(mapName)`（CR258，package-private，"single-owner, no second loader"），XiuluoTaskV2 同样不可达。窗口基点经数学消去（`rel=base+zeroOffset+logical*scale−base`），故 windowRect 本身不含 transform 信息，irreducible 依赖=transform 值。

**语义已 Cloud-canonical 的证据**：`MiniMapPointResolver` 内已存在两个 package-private JSON-decision 形态、逐字对应基线算法——`resolveApproachCoordinate`（龙窟/凤巢 cave-original 短路+transform-missing 原坐标 fallback+`rel=zeroOffset+logical*scale`+方向 vs 1024/2·768/2+`logicalStepForPixelDirection`+`APPROACH_LOGICAL_OFFSET=2`）与 `checkCoordinatePlausible`（unknown-map permissive+margin 参数化）。**裁定纯属可见性/落点问题，非算法问题。**

**更新后的请裁选项**：
- (a) 父级批准 `ObjectiveTextRecognizer.coordinatePlausible` 与 `ObjectiveTextRecognizer.mapTransform` 两个 package-private static → `public static`（两处一词修饰符变更，语义零改动，仍单一 owner/无第二 loader），写集扩一文件或父级自落；
- (b) 父级指定其它既有 public 可达路径；
- (c) 其它形态（如父级在 cloudbrain 包内自落一个 public bridge）。

不受阻部分（randomize (1,1) 内化 / :3657 OCR→`LocalOcrClient.readWords` 重写含 in-memory capture 传递）继续推进设计；`:2006`+`:3666` 两站点字节冻结待裁。prod 仍 `15a4a5f4` 未动。

<!-- TRUE_EOF: TURN-40B-C3 OWNER-A Q1-ADDENDUM MAPTRANSFORM-ALSO-PACKAGE-PRIVATE APPROACH-SITE-ALSO-BLOCKED RESOLVER-PROVES-SEMANTICS-CANONICAL OPTIONS-UPDATED-A-B-C UNBLOCKED-DESIGN-CONTINUES PROD-UNCHANGED 2026-07-18T16:36:00-04:00 -->

## Parent Plan-Contract Amendment #2 - 2026-07-18 16:42 EDT

- supersedes: Amendment #2 supersedes unacknowledged Amendment #1 and resolves Q1 plus its approach addendum.
- approved shape: within the already-added `ObjectiveTextRecognizer.java`, change `coordinatePlausible` to `public static`; change `mapTransform` to `public static` and return a defensive copy of the four-value transform. The internal maps.json snapshot/array must never be exposed or mutated.
- exact boundary: write set remains exactly three files. No `MiniMapPointResolver` change/new method, JSON bridge, wrapper, reflection, second loader/store, duplicated transform source, constant change or additional test file is authorized.
- test gate: existing `XiuluoWholeTaskTurnContractTest` must retain cross-package plausibility, approach offset/cave/fallback semantics, and prove mutating one returned transform array cannot affect a later read.
- baseline/collision: this is ownership/API plumbing only; no business difference. C2 remains disjoint. `无已批准业务差异；按基线等价迁移`.
- communication: next A STATUS EVENT must ACK `PARENT-A-TURN40B-C3-CONTRACT-AMENDMENT2-1642`; this ACK also records Amendment #1 as superseded, so A must implement only #2.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-PLAN-CONTRACT-AMENDMENT-2 SUPERSEDES-UNACKED-AMENDMENT1 OWNER-A SOURCE-ACTIVE ACK-PENDING=AMENDMENT2-1642 EXACT-3-FILE-WRITESET COORDINATEPLAUSIBLE-PUBLIC MAPTRANSFORM-PUBLIC-DEFENSIVE-COPY INTERNAL-SNAPSHOT-NOT-EXPOSED NO-RESOLVER+SECOND-LOADER+BUSINESS-DIFFERENCE DISJOINT-C2 NO-MAVEN 2026-07-18T16:42:00-04:00 -->

## Parent Amendment ACK And WIP Audit - 2026-07-18 16:54 EDT

- External A 16:53 STATUS validly ACKed `PARENT-A-TURN40B-C3-CONTRACT-AMENDMENT2-1642` and confirmed Amendment #1 superseded. Communication is normal and all C3 sites are unblocked.
- File 1/3 `ObjectiveTextRecognizer.java` landed under Amendment #2: public pure plausibility plus public defensive-copy transform. Physical SHA-256=`1D97D9967B0EB4C5B520716EDE07D0E040088E06DCF070DB2A03AFB03BE9FED9`, mtime `2026-07-18T08:02:23.7317364-04:00`.
- Xiuluo production and existing whole-task test remain. This is protected WIP, not canonical delivery/source review. C2 remains disjoint; no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-AMENDMENT-ACK+WIP-AUDIT OWNER-A SOURCE-ACTIVE COMMUNICATION-NORMAL ACK=AMENDMENT2-1642 AMENDMENT1-SUPERSEDED FILE=1-OF-3 OBJECTIVE=1D97D996 XIULUO+TEST-PENDING NO-DELIVERY DISJOINT-C2 NO-MAVEN 2026-07-18T16:54:00-04:00 -->

## Parent WIP Activity Audit - 2026-07-18 17:04 EDT

- External A 17:02 STATUS reports `XiuluoTaskV2.java` production rewrite complete, moving C3 to File `2/3`; the existing whole-task contract test remains pending.
- Parent physical audit found the Cloud production file continued changing after that event: current SHA-256=`6B90ECD18BACCFA9DC66B10071F2E7403F4DAF31B833A9191ED23835F4F578CA`, mtime `2026-07-18T08:09:08.2092004-04:00`. This confirms active writing, not a stable delivery. Objective remains `1D97D996...`; test remains unchanged.
- A remains sole owner/source active/communication normal. No canonical delivery or source-review trigger; C2 remains disjoint and both writers remain active, so no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-WIP-ACTIVITY-AUDIT OWNER-A SOURCE-ACTIVE COMMUNICATION-NORMAL FILE=2-OF-3 OBJECTIVE=1D97D996 XIULUO=6B90ECD1 TEST-PENDING ACTIVE-BYTES-AFTER-EVENT NO-DELIVERY DISJOINT-C2 NO-MAVEN 2026-07-18T17:04:00-04:00 -->

## Parent Physical WIP Audit - 2026-07-18 17:12 EDT

- The existing `XiuluoWholeTaskTurnContractTest.java` now has real source activity: physical SHA-256=`99FFC4B8D0A16697EEB09280557E1F6A553ADF5C102860E9BD6D39DBC833B352`, mtime `2026-07-18T08:15:00.8521047-04:00`.
- External A has not yet emitted a File 3/3 completion event and the canonical card has no delivery. Treat these bytes as active protected WIP only; do not begin source review.
- A remains sole owner/source active/communication normal; C2 remains disjoint. No Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-PHYSICAL-WIP-AUDIT OWNER-A SOURCE-ACTIVE COMMUNICATION-NORMAL FILE=2-OF-3 TEST-BYTES-ACTIVE TEST=99FFC4B8 NO-DELIVERY DISJOINT-C2 NO-MAVEN 2026-07-18T17:12:00-04:00 -->

## EXTERNAL-A WHOLE-CARD CANONICAL SOURCE+TEST DELIVERY - 2026-07-18T17:12:00-04:00

**写集（精确三文件，Amendment #2）与最终字节**：
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/ObjectiveTextRecognizer.java` = blob `1f6bfa71` / SHA-256 `1d97d9967b0eb4c5...` / 930L：`coordinatePlausible` `static`→`public static`（语义零动）；`mapTransform` `static`→`public static` + 返回 `transform.clone()` **防御拷贝**（null 保 null；单 owner/无第二 loader 不变；同包 3 既有调用点只读兼容零改动）。
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` = blob `99684340` / SHA-256 `6b90ecd18baccfa9...` / 4554L（自 `15a4a5f4`/4483L，+71）：四站点全部迁移——
  ①**randomize**：`coordinateHelper.getRandomizedPoint(x,y,1,1)`→内化 `randomizedNpcTargetPoint`（`nextInt(3)-1` 双轴=基线 `nextInt(r*2+1)-r`、r=1 精确分布；新 inline-init `Random` 字段，@RequiredArgsConstructor 排除）；
  ②**approach**：→内化 `calculateApproachCoordinate(context,map,x,y)`：龙窟/凤巢 `contains` 短路返原坐标（基线中空地形注释语义保留）→`mapTransform`（defensive copy）+`TurnWindowMetadata.windowRect` 窗口基点（transform 或 windowRect 缺→原坐标 fallback=基线 transformMissing/pixelMissing 分支）→`absolute=round(base+zeroOffset+logical*scale)`→`relative=absolute-base`→方向 vs `GAME_CLIENT_WIDTH/2`·`HEIGHT/2`→`logicalStepForPixelDirection`（scale==0→0；sign(direction/scale)）→`target±APPROACH_LOGICAL_OFFSET(=2)`；info/warn 日志逐字段保留；
  ③**plausibility**：`isLogicalCoordinatePlausible(map,x,y,80)`→`ObjectiveTextRecognizer.coordinatePlausible(map,x,y)`（`COORDINATE_PLAUSIBILITY_MARGIN_PX=80` 固定等价，unknown-map permissive 同基线）；
  ④**OCR**：path-based 链整段替换——`tryReadObjectiveFromTaskPanel` 弃 `QuestDetailCapture` 包装、in-memory turn capture frame 直传；`parseTaskPanelObjective(BufferedImage,source)`（finally flush 保留）；`parseTaskPanelObjectiveByOcr(BufferedImage,source)`=null→skip、`LocalOcrClient.readWords` `!available`→保守 empty（镜像基线 missing-text 分支）、`joinOcrText` 适配 `OcrWord.text()`、下游 `parseTaskPanelObjectiveText`→`toXiuluoObjective`→plausibility 过滤不变；matcher 谓词删除（javadoc 载明等价证明：`matchesTaskPanelObjectiveText(words) ≡ parseTaskPanelObjectiveText(join(words)).isPresent()`，单发 provider-order 读下行为同构）；`imagePath` 依赖=0。
  **删除面**：imports TextRecognizer/OcrWordResult/QuestDetailCapture/CoordinateHelper；fields coordinateHelper/textRecognizer（ctor arity −2）。**残留核验**：上述符号+`getAllTextResultsForMatch`+`imagePath` 全 0（`CoordinateHelper` 仅 5 处 javadoc baseline 出处引用）。
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java` = blob `9437fb45` / SHA-256 `99ffc4b8d0a16697...` / 660L / **13 @Test**（原 7 保留零改 + 新 6，无新增 test 文件）：`randomizedNpcTargetPointStaysWithinTheExactOneByOneRadius`（300 draw 界+双轴非常量）/`approachCoordinateStepsTowardClientCenterUsingTheCloudTransform`（长安真实 transform，两象限步进=test-side 基线公式镜像推导，含方向翻转断言）/`approachCoordinateKeepsCaveAndUnknownMapOriginals`（龙窟短路+未标定 fallback）/`objectivePlausibilityMapsToTheCloudMarginGuard`（真/extra-digit 假/unknown permissive）/`taskPanelOcrFoldsNullAndUnavailableToConservativeEmpty`（null+4×4 帧→empty，任一 OCR 可用性下均成立）/`mapTransformReturnsADefensiveCopyOfTheSingleOwnerSnapshot`（**defensive-copy isolation**：改返回数组→重取 assertNotSame+值不变）。驱动沿用本文件已过审 invoke/harness 形态；ctor 工厂按型匹配自动适配 arity −2（零工厂改动）。
**基线对照**：所有语义自 `696a12b0` `CoordinateHelper`（randomize/approach/plausible 三方法逐行提取，δ=窗口基点由 turn metadata `windowRect` 提供且数学消去）与 task-panel OCR 独立 fallback 意图（story 绿模板路径零动）。**changed-path 全表**=上列三文件，其余零字节。**named test/compile：shared-writer deferral**——C（C2）active Java writer，per 卡纪律本轮不运行 Maven；writer 稳定后运行授权 `XiuluoWholeTaskTurnContractTest`+适用 Cloud compile（另 S1 `AutomationMetricsService` 缺型属 40B 其它子卡债，模块 compile 以 40B 家族整体收口为准）。禁令核验：MiniMapPointResolver 零字节/无 JSON bridge/无 wrapper/production 无反射/无第二 loader-store/无常量-算法改动/无新增 test 文件。**`无已批准业务差异；按基线等价迁移`**。

<!-- TRUE_EOF: TURN-40B-C3 EXTERNAL-A WHOLE-CARD-SOURCE+TEST-DELIVERED OTR=1f6bfa71-930 PROD=99684340-4554 TEST=9437fb45-660-13T DEFENSIVE-COPY-COVERED SHARED-WRITER-DEFERRAL-NAMED-TEST AWAIT-PARENT-REVIEW NO-BUSINESS-DIFF 2026-07-18T17:12:00-04:00 -->

## Parent SOURCE+TEST Source Review #1 - 2026-07-18 17:17 EDT

- verdict: `P0/P1/P2=0/1/1 / BLOCKED / REPAIR REQUIRED`; External A retains the whole-card owner.
- reviewed scope: all three exact delivered files, Amendment #2, frozen C3 contract, `docs/业务逻辑.md`, baseline `696a12b0`, and the retained/new 13-test source. Production randomize/approach/plausibility/public defensive-copy seams show no review finding in this pass.
- P1 (`XiuluoWholeTaskTurnContractTest.taskPanelOcrFoldsNullAndUnavailableToConservativeEmpty`, current lines 349-357; production call at `XiuluoTaskV2.parseTaskPanelObjectiveByOcr`, current line 3730): the non-null branch invokes `LocalOcrClient.readWords` against the process-wide default `http://127.0.0.1:18761`. The test is therefore environment-dependent, may contact a real OCR sidecar, and changes behavior when that process happens to be running. It also never proves the required available provider-order words -> joined text -> exact `NpcTarget` mapping. This violates the HTTPS family fake-only/no OCR-runtime rule and the C3 exact OCR mapping gate.
- P1 repair condition: in this same existing test file only, bind `LocalOcrClient` to a deterministic test-controlled fake endpoint/response and restore the system property afterward. Cover both (a) available words producing the exact map/x/y objective through the real production caller and (b) structured unavailable producing empty. No application OCR runtime, external sidecar dependency, retry, wrapper, second provider/store or additional test file.
- P2 (`approachCoordinateStepsTowardClientCenterUsingTheCloudTransform` and `approachCoordinateKeepsCaveAndUnknownMapOriginals`, current lines 308-337): tests cover a present windowRect and missing transform, but not the separately frozen missing-windowRect fallback branch at production lines 2016-2023. Add a retained same-file case whose metadata has no windowRect and asserts the original coordinate. Keep cave, transform and direction assertions intact.
- re-delivery gate: repair only the existing whole-task test unless production must change for a newly demonstrated defect; append fresh SHA/test count and canonical whole-card re-delivery. Named test/compile remain deferred while C2 is an active Java writer. `无已批准业务差异；按基线等价迁移` remains required.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-SOURCE+TEST-REVIEW-1 BLOCKED REPAIR-REQUIRED OWNER-A P0=0 P1=1 P2=1 P1=REAL-OCR-SIDECAR-DEPENDENCY+NO-AVAILABLE-MAPPING P2=WINDOWRECT-MISSING-FALLBACK-UNCOVERED SAME-TEST-FILE-REPAIR NO-MAVEN 2026-07-18T17:17:00-04:00 -->

## EXTERNAL-A REVIEW #1 REPAIR RE-DELIVERY (SAME TEST FILE ONLY) - 2026-07-18T17:27:00-04:00

ACK `PARENT-A-TURN40B-C3-REVIEW1-REPAIR-1717`（Review #1=`0/1/1`）。两 finding 均已按固定返修闭合，**仅改唯一 test 文件**；production 两文件零字节未动（OTR=`1f6bfa71`/930L、XiuluoTaskV2=`99684340`/4554L 本轮复核无漂移）。

### P1（REAL-OCR-SIDECAR-DEPENDENCY + NO-AVAILABLE-MAPPING）已闭合
- 新私有 helper `withFakeOcrSidecar(responseJson, invocation)`：JDK 内建 `com.sun.net.httpserver.HttpServer` 绑 `127.0.0.1` 临时端口、`/ocr/words` 返回单条 canned JSON；`dhxy.cloud.brain.localOcrEndpoint` 属性 set→finally **恢复原值（含 null→clear）+ server.stop(0)**，任何结局不泄漏进程级默认 endpoint/套接字；无真实 OCR runtime/wrapper/retry/第二 provider/新文件。
- `taskPanelOcrFoldsNullAndUnavailableToConservativeEmpty`（改）：null 帧→empty 保留；4×4 帧改经 fake sidecar 返回 **structured unavailable** `{"ok":false,"error":"model-not-loaded"}`→确定性 empty（不再触达默认 loopback）。
- 新 `taskPanelOcrMapsDeterministicAvailableWordsToTheParsedObjective`：fake sidecar 返回 available words `前往长安(130,130)`（全字段 word box）→经真实 `readWords`→join→parse→`toXiuluoObjective`→plausibility 全链，断言 present + mapName=长安 + x/y=130/130（**确定性 available 正向映射**）。
### P2（WINDOWRECT-MISSING-FALLBACK-UNCOVERED）已闭合
- `MetadataOnlyPort.latestWindowMetadata` `Optional.of`→`Optional.ofNullable`（既有非 null 调用者行为不变；注释记 P2 用途）。
- 新 `approachCoordinateFallsBackToOriginalWhenWindowRectIsMissing`：`MetadataOnlyPort(null)` 客户端 + 自绑 holder，已知 transform（长安）下仅 windowRect 缺→断言原坐标 (130,130)（frozen fallback 分支）。既有 direction/cave/unknown-transform/defensive-copy 断言全保留。

**test 终字节**：blob `3a6fc405` / 737L / **15 @Test**（13→15；原 13 断言零删改）。brace 0/0·paren 0/0；英文 javac 过滤后零语法错（余为空 classpath 伪影）。**named test/compile 仍 shared-writer deferral**（C(C2) active）。`无已批准业务差异；按基线等价迁移`。

<!-- TRUE_EOF: TURN-40B-C3 EXTERNAL-A REVIEW1-REPAIR-REDELIVERED P1-FAKE-SIDECAR+AVAILABLE-MAPPING P2-WINDOWRECT-FALLBACK-COVERED TEST=3a6fc405-737-15T PROD-UNCHANGED OTR-UNCHANGED ACK=PARENT-A-TURN40B-C3-REVIEW1-REPAIR-1717 AWAIT-REVIEW2 2026-07-18T17:27:00-04:00 -->

## Parent SOURCE+TEST Source Review #2 - 2026-07-18 17:36 EDT

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`; External A owner released. No repair item remains.
- reviewed bytes: `ObjectiveTextRecognizer.java` blob `1f6bfa71`, `XiuluoTaskV2.java` blob `99684340`, and repaired `XiuluoWholeTaskTurnContractTest.java` blob `3a6fc405` / SHA-256 `9986F2AE...` / 15 tests. Both production blobs are unchanged from Review #1.
- P1 closure: `withFakeOcrSidecar` binds a test-controlled loopback ephemeral port, serves deterministic `/ocr/words` JSON, and restores `dhxy.cloud.brain.localOcrEndpoint` plus stops the server in `finally`. The retained unavailable case is deterministic and the new available case traverses the real `LocalOcrClient.readWords` -> provider-order join -> objective parse -> plausibility path to exact `长安(130,130)`. No real OCR runtime, retry, wrapper, second provider/store or extra test file exists.
- P2 closure: `approachCoordinateFallsBackToOriginalWhenWindowRectIsMissing` supplies no latest window metadata with a known `长安` transform and asserts exact original `(130,130)`; direction, cave, unknown-transform and defensive-copy cases remain present.
- verification gate: named `XiuluoWholeTaskTurnContractTest` and applicable Cloud compile remain deferred because C2 is still an active Java writer. This build deferral does not reopen the passed source gate. `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-SOURCE+TEST-REVIEW-2 PASSED OWNER-RELEASED P0=0 P1=0 P2=0 TEST=3a6fc405-15T FAKE-OCR-AVAILABLE+UNAVAILABLE WINDOWRECT-MISSING-FALLBACK PROD=99684340 OTR=1f6bfa71 NAMED-TEST+CLOUD-COMPILE-DEFERRED-C2-WRITER NO-MAVEN 2026-07-18T17:36:00-04:00 -->

## Parent Review #2 ACK Closure - 2026-07-18 17:45 EDT

- External A 17:44 STATUS explicitly ACKed `PARENT-A-TURN40B-C3-REVIEW2-PASSED-1736`, accepted `P0/P1/P2=0/0/0`, and confirmed this card has no owner.
- A is now `IDLE / AVAILABLE` with no claimable READY/ZERO-OWNER card in its lane. C3 remains source-review passed/owner released; named test and Cloud compile remain deferred only by the active C2 writer.

<!-- TRUE_EOF: TURN-40B-C3 PARENT-REVIEW2-ACK-CLOSURE PASSED OWNER-RELEASED ACK=PARENT-A-TURN40B-C3-REVIEW2-PASSED-1736 EXTERNAL-A-IDLE-AVAILABLE NO-CLAIMABLE-CARD NAMED-TEST+CLOUD-COMPILE-DEFERRED-C2-WRITER NO-MAVEN 2026-07-18T17:45:00-04:00 -->
