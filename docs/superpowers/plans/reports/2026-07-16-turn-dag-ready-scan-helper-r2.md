CLAIMED | uuid=`019f6acc-b310-7310-aee7-8cff7296a844` | nickname=`Nietzsche` | role=`CR271 Internal helper` | snapshot-cutoff=`2026-07-16T08:42:09.4952980-04:00` | 非 implementation owner、非 reviewer、非父级批准

# CR271 Turn DAG READY Scan Helper R2

## 0. 权限与输出边界

- 唯一写入是 `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-dag-ready-scan-helper-r2.md`；本报告不修改 Java、POM、测试、现有卡片、权威计划、`ACTIVE_WORK.md`、业务文档或 dashboard。
- 本 helper 未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，未执行 Git mutation；两仓全部 dirty/untracked/ignored 原样保护。
- 本报告只转录最新 true EOF、精确依赖、SHA/mtime、write-set mutex 与调度建议；**不批准、不阻断、不关闭任何卡，非父级批准**。

## 1. 权威材料与 latest true EOF 快照

### 1.1 完整读取范围

| 文件与范围 | mtime (EDT) | SHA-256 |
|---|---|---|
| `AGENTS.md:1-392` | `2026-07-11T17:24:59.6508563-04:00` | `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md:1-1349` | `2026-07-16T03:32:25.4942933-04:00` | `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `docs/ACTIVE_WORK.md:1-145`（顶部 CR271；全文 79,934 行） | `2026-07-16T08:09:54.2102522-04:00` | `F17DC1659C897A227CA25E92E0081D0EE8DFD2D2CFC43F4B852B1F63A6A02318` |
| `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:1016-1697`（第 14-19 节） | `2026-07-16T08:09:54.2122469-04:00` | `D9D65F476200E3C5DD281BD00C239F3954B2A77C18BDDADD41CB45F83D6C3CD8` |
| `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:1-383` | `2026-07-16T03:13:40.4741505-04:00` | `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| `docs/业务逻辑.md:1-1426` | `2026-07-11T19:40:58.4813186-04:00` | `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` |

规则索引：权威注册表覆盖旧计划见 plan `:1041-1049`；`S=startDependsOn`、`A=approval/buildDependsOn` 见 `:1078-1080`；动态 READY 与 mutex 规则见 `:1007-1011,1411-1421,1455-1464`；业务源码可按正常 `S` 开始、T01-T04 只拦最终批准见 `:1609-1612`；完整批准状态机见 `:1684-1697`。协议的一 action/五 step、单 frame、exact metadata 与零自动 retry 分别见 protocol `:51-80,108-126`；queue-owned click timing 与 pixel probe 见 `:216-255`。基线差异门见 `docs/业务逻辑.md:213-224`，修罗导航/NPC phase 边界见 `:1168-1251`，NPC FIFO 与本地/Cloud 职责见 `:1305-1363`。

### 1.2 报告目录扫描口径

- 截至 `2026-07-16T08:41:52.3108854-04:00` 重新枚举 `reports/*.md` 共 `328` 份；`73` 份存在规范 `TRUE_EOF`，`255` 份没有 `TRUE_EOF`。每份文件只采用其最后一条 `TRUE_EOF`；无 marker 文件不作为状态转换证据。活动写集字节另截至 `2026-07-16T08:38:55.5669373-04:00` 取样。
- 当前 DAG 所需 latest true EOF 已逐份重读：TURN-22/28Q/28P/28/33/34A/34B 原卡，External A/B/C/D lane，以及 Galileo、Laplace、Leibniz、Dalton helper。原卡物理 EOF 高于旧 lane/ACTIVE 快照；物理源码 SHA 只能证明字节变化，不能冒充 delivery、review 或批准。
- `docs/ACTIVE_WORK.md:3-35` 与 plan `:1-15,1134,1142,1149-1150,1458-1461` 仍是 `08:09` 调度快照，已被后续原卡覆盖：A 已 CLAIM `TURN-28Q`；B 已归还 `TURN-28`；Euler 已交付 28P 两测试；C 的 34A test-only repair 已出现新字节；D 仍实施 34B。依赖定义仍以 plan 第 14-19 节为权威，新暴露 mechanics predecessor 以父级 `TURN-28Q` 固定卡为准。
- Laplace helper 已于 `08:24:48` 规范 `PRECHECK_COMPLETE`；其 `:142-151,177-187` 静态确认 TURN-28 source-start/final-gate 分层与 strict-696 边界，但明确不是父级批准。

### 1.3 最新关键报告 SHA

| 证据 | latest marker / 行 | mtime | SHA-256 |
|---|---|---|---|
| `reports/2026-07-16-turn-card-TURN-28Q.md` | `:123-216`，External A SOURCE+TEST DELIVERED，未运行/未编译，待 parent review | `2026-07-16T08:40:43.025-04:00` | `32E28E06125E1F83BB19A33368D2B648DD2C0D4F90CECC51DCB313A8BF804EDC` |
| `reports/2026-07-16-turn-card-TURN-22.md` | `:639-658`，父级确认 P1 prerequisite=`TURN-28Q`，A owner released/WIP preserved | `2026-07-16T08:25:48.872-04:00` | `53C265691D377B2BB4986FD1E06CE73935DF76FE68F54A517D7DE7CFCCD84D64` |
| `reports/2026-07-16-turn-22-repair3-source-start-preflight-helper.md` | `:101-112`，PRECHECK_COMPLETE / non-parent approval | `2026-07-16T08:16:32.190-04:00` | `3D0954FE50105B377AF3797697C2CE1DC6C953F52CD8441CBC16D023835133D8` |
| `reports/2026-07-16-turn-card-TURN-28P.md` | `:1072-1125`，Euler 两测试 SOURCE+TEST DELIVERED，尚未 parent review | `2026-07-16T08:28:41.420-04:00` | `0E06BCB1698278DDE9E490BAD400A7578BE7DC2B52F545BAAE49042689B8668D` |
| `reports/2026-07-16-turn-28-parent-freeze-preflight-helper.md` | `:177-187`，PRECHECK_COMPLETE / non-parent approval | `2026-07-16T08:24:55.461-04:00` | `49C5815E5D3F6FBD3E023B464C143C491ED3D22F6601F0149C9CF2192487B430` |
| `reports/2026-07-16-turn-card-TURN-28.md` | `:167-189`，External B OWNER RETURNED，四目标零变化 | `2026-07-16T08:31:09.114-04:00` | `DEA7892D3AC8B5EC9CB3B5B92C6BEC8AE08BA5D3A4834F09B2F2BC28145728C5` |
| `reports/2026-07-16-turn-card-TURN-34A.md` | `:379-387`，C test-only Repair #1 start directive；其后物理 test 已变化、未 delivery | `2026-07-16T08:28:19.139-04:00` | `FB5380C4BCFAA2AA3B6C0E000FD3DA9BEACAC80F4A26223299AA034EF6D01ED4` |
| `reports/2026-07-16-turn-34A-delivery-preflight-helper-r2.md` | `:184-210`，PRECHECK_COMPLETE / non-parent approval | `2026-07-16T08:19:13.267-04:00` | `BAEDC27F596568537CC0EAD8F37FEED673F14C1DD5A494D7C936F0A583F7671F` |
| `reports/2026-07-16-turn-34a-repair1-test-matrix-preflight-helper.md` | `:58-69`，gap-matrix PRECHECK_COMPLETE / non-reviewer / non-owner | `2026-07-16T08:40:58.197-04:00` | `F0EE4D21F2148D7108F3B62BB5CA37E281C55A1508FC65C11DA0CA20F67A0EF2` |
| `reports/2026-07-16-turn-card-TURN-34B.md` | `:98-109`，External D CLAIMED | `2026-07-16T08:08:21.576-04:00` | `DE939BAFEE5226B5492BFE2492729DEFC0210DD4268E1605A00DC7D04C350771` |
| `reports/2026-07-16-turn-34b-delivery-preflight-helper-r2.md` | `:263-269`，active-delivery PRECHECK_COMPLETE / non-reviewer / non-parent approval | `2026-07-16T08:38:08.779-04:00` | `DB11E43E3E89A88E164270CC4F4C4D43E945A31BBB01F29E3A3D3295A5EFDCB8` |
| `reports/2026-07-16-turn-card-TURN-33.md` | `:629-645`，dual review 2/2 passed / build pending | `2026-07-16T06:29:32.251-04:00` | `C683C6A8C5AD8C144587CFD8A8D32787402185AFC33986400DB64E4933AED8F8` |
| `reports/2026-07-16-turn-33-build-preflight-helper.md` | `:42-87`，PRECHECK_COMPLETE / non-parent approval | `2026-07-16T08:19:17.248-04:00` | `F051C6F9A1022D18D8D473D61A93CDC0703CEED3C0A6D422C75ADFF54153FA5B` |

## 2. 两仓只读 status

`git status --porcelain=v1 --untracked-files=all` 各行按 UTF-8/LF 连接且末尾不补 LF 后取 SHA-256：

| Repo | branch / HEAD | entries | M / D / ?? | status SHA-256 |
|---|---|---:|---:|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 667 | 43 / 1 / 623 | `47E4A22EDC65AD7E9B962107E193982143865B57F40E58F6905C9B9BCD4875A0` |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 | 9 / 0 / 541 | `E85B93E8CE8DD62EF5FA8D729E9E723F458BD4A9E8351081636D8DC2BD132C72` |

Cloud `.gitignore:15` 忽略 `src/test/`，所以活动/交付测试必须按绝对路径 SHA/mtime 保护，不能用普通 status 缺席推断不存在。`0114604e...` 与 `3b988caa...` 只是当前 HEAD；业务 authority 仍是 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。

## 3. 当前 READY / source-start / approval-gated 清单

### 3.1 实时实施状态

| Card | exact source dependency | latest true EOF 状态 | 最终门 / 建议 |
|---|---|---|---|
| TURN-28Q | 父级新增真实 mechanics predecessor；补齐 TURN-22 已证实缺失的 frozen exact-window action-list API（card `:9-22,48-69`） | External A 已 SOURCE+TEST DELIVERED 并停止；五文件 final SHA见 `:128-136`，明确未运行/未编译且等待 parent review（`:195-216`） | 先做 parent latest-byte source/test-source review；通过后 A 才返回 TURN-22 item 2/3。delivery不是 source pass，双 reviewer、named test/compile仍是批准门 |
| TURN-28P | `S=09R+11+23P`（plan `:1141`） | Euler 已 SOURCE+TEST DELIVERED 两测试并停止写入（card `:1072-1125`）；这不是测试/parent review通过 | 下一步是父级按最新 SHA 静态 source/test-source review；之后才进入适用 reviewer/test/build 门。28Q 五文件与其两测试已由父级明确互斥（`:1060-1070`） |
| TURN-22 Repair #3 | 原 `S=14+18+23+28P production API`（plan `:1134`），现增加真实 predecessor=`TURN-28Q parent source/test-source pass`（card `:639-658`） | `P0/P1/P2=0/1/0`；A owner 已释放，Cloud test item 1=`2D290759...` 作为 WIP 保留，executor/test未动 | 等 28Q 通过后 A 原路返回 item 2/3；随后 parent review、28P review/test、named test/compile。当前是实际 production API 缺口，不是 final-approval gate |
| TURN-28 | `S=23+24/24A+26+28P production API`（card `:7-9`；plan `:1142`） | source-start 固定卡仍有效；External B 已 OWNER RETURNED，四目标与领取 SHA一致、test absent（card `:167-189`） | **当前唯一未 CLAIM 的正式 source-start READY 工作卡**，需父级改派后新 owner 先 true-EOF claim；28P review、`28Q -> 22` integration、own named test/compile只拦最终批准 |
| TURN-34A | `S=19+20+21+23+24A+33`（plan `:1149`） | production source parent passed；C 保留 test-only Repair #1 owner。test 在卡 `:379-387` 后已由 611 行/`5E2C...` 变为 630 行/`82DFB872...`，尚无 delivery marker | C 继续同一 named test并 delivery；production `532E6F84...` 只读。34A production surface已经满足下游 source dependency，不能被 test-only repair反向撤销 |
| TURN-34B | `S=21+22 production contract+23+26+33`（plan `:1150`） | External D CLAIMED（card `:98-109`）；production 已从 1,130 行变为 1,224 行，尚无 delivery，named test 不存在；最新 helper `:35-46,263-269` 同样记录 active WIP、非 review | 34B 只维护 TEAM_RETURN/COMMON_BOX capability，零 TeamReturn mechanics（card `:5-10,57-60`），可继续 source；最终仍等 TURN-22 contract、34A API compatibility、reviews/test/build |
| TURN-33 | `S=15+18+26`（plan `:1148`） | source/test source + reviewer `2/2` 已通过（card `:629-645`） | 仅 named `SummonSkillTurnContractTest` 与 Cloud compile/build 待 stable-writer window；精确命令/归因见 Dalton `:42-87`，当前禁止并发 Maven |

**当前正式 READY/owner 摘要：** C=`TURN-34A Repair #1` active，D=`TURN-34B` active；A 的 28Q 与 Euler 的 28P 均已 delivery并停止，等待各自 parent review；TURN-22 owner released；B 已归还且无卡。`TURN-28` 因 B 零源码归还而成为唯一正式、未 CLAIM 的 source-start READY 卡，必须由父级重新指派，不能沿用旧 B claim。

### 3.2 source-complete、仅 approval/test/build 待验 cohort

- Foundation source 已落盘但仍受对应 test/repair/approval 门约束：`TURN-01A/B/C/D, 02, 03A/B, 04, 05, 06, 07, 08A/B, 09, 10P/A/B/C/D/E, 11, 12`，逐卡精确 `S/A` 见 plan `:1088-1113`；不能据 source 状态写 CARD APPROVED。
- 已有 parent source/test-source review、owner 已释放而 named test/compile/foundation approval 仍待：`TURN-02R, 09R, 10CR, 13, 13G, 13H, 13C, T01, T02, T03, T04`（plan `:1093,1103,1108,1113-1120`），以及 `TURN-14..21, 23P, 23, 24A, 25, 26, 29, 30, 31, 32, 33`（`:1126-1148`）和 `TURN-40A`（`:1168`）。
- 对下游 source-start，这些已通过的 production/source surface 应按 `S` 计入满足；其 named tests、T01-T04、compile/build 只约束最终批准。plan `:1426-1428,1609-1612` 明确禁止用最终 test-debt gate 反向冻结业务源码启动。

## 4. 活动 write-set mutex

### 4.1 精确当前字节

| Owner/card | modify path | lines / mtime | SHA-256 / 状态 |
|---|---|---|---|
| delivered / 28Q（无 active writer） | DHXY `InputSequences.java` / `InputActionQueue.java` / `InputActionRequest.java` / `InputActionWorker.java` / `InputActionFrozenExclusiveContractTest.java` | 210 / 850 / 1,118 / 735 / 734；mtime `08:35:19` / `08:35:08` / `08:34:56` / `08:35:47` / `08:38:13` | final `B293E0C6...` / `66FA536E...` / `23973B7E...` / `4B853F95...` / `943DC486...`；待 parent review，未运行/未编译 |
| delivered / 28P（无 active writer） | DHXY `TurnCapturePixelChangeProbeContractTest.java` / `LocalTurnActionExecutorContractTest.java` | 965 / 1,275；mtime `08:25:43` / `08:25:51` | `5D563BBB08747C7B298EC6C7C0795A600269BC86D8F5769BCC67588268FDA818` / `88011CF17B24E68B8DCF5C7EF11EDD30FB8A9DF2AAC27E639E320E3BD4DD3709`；待 parent review |
| released / 22 WIP | Cloud `TeamReturnTurnContractTest.java`; DHXY `TurnInputStepExecutor.java` / `TurnInputStepExecutorContractTest.java` | 1,658 / 229 / 394 | `2D290759...`（WIP保留）/ `0EE95CBD...`（未动）/ `BB1CCC43...`（未动）；28Q通过前只读 |
| READY unowned / 28 | Cloud `NpcClickService.java` / `ObjectiveTextRecognizer.java` / `SmartClickRecognizer.java` / named test | 3,406 / 914 / 3,026 / absent | `F4E3842C...` / `D3DC3CC2...` / `FFBD984A...` / absent；B 已归还，继任者 claim 前无人可写 |
| External C / 34A Repair #1 | Cloud `AutoCombatServiceTurnContractTest.java` only | 630 / `2026-07-16T08:35:55.5575741-04:00` | `82DFB872262CD87BB469E227149ADD8B6EE37D2496E3A821C048F83E59B21275`；production `AutoCombatService.java`=`532E6F84...` 只读；未 delivery |
| External D / 34B | Cloud `TaskMaintenanceService.java` | 1,224 / `2026-07-16T08:17:40.6760891-04:00` | `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC`；named test absent |

上表 28Q 是原卡 delivery SHA；C 是活跃 owner 的 **cutoff WIP 字节**，不是 delivery SHA。C 截止后可能继续合法变化；任何 parent review/readiness仍必须先按原卡 latest true EOF重取 SHA，本表不批准源码。

### 4.2 mutex 结论

1. 当前 active modify owner 只有 C=34A 单测试与 D=34B 两文件，文件交集为 `0`。A/Euler 已 delivery并停止，B 已 owner return，TURN-22 owner released，不能继续列为 active writer。
2. 28Q 与 28P 写集由父级在 28P card `:1060-1070` 明确互斥；两卡现在都处于 delivery-awaiting-parent-review。`28Q parent source/test-source pass -> A return 22` 是依赖顺序，review期间五个 delivery文件与22/Euler文件均应只读。
3. TURN-28 四文件当前无 owner，与 A/C/D 文件交集为 `0`；但只有父级重新指派且新 owner 在原卡 true EOF CLAIM 后才可写。候选 TURN-27 只读消费 `ObjectiveTextRecognizer` public static API；若父级并发开放 27，必须维持 TURN-28 card `:36-53` 的 semantic API freeze，并禁止 27 修改 Objective/SmartClick/NpcClick。
4. C/D 文件互斥，但 D 必须保持 C 消费的六个 TaskMaintenance public API；冻结点见 TURN-34B card `:31-40`。34A production 已 parent passed，C Repair #1 只写测试，因此 D 不应请求回改 AutoCombat。

## 5. final-approval gate 是否错误阻塞 source-start

### 5.1 已正确拆分

- TURN-28：fixed card `:3-15` 已把 28P 两测试与 TURN-22 executor integration 留作 final gate，同时允许 Cloud strict-696 source 开始；write set 与 28P/22 互斥，拆分成立。
- Laplace `:142-151,177-187` 对当前字节独立复核了该分层：Objective 可 reservation-only/零 diff，Smart 只允许最小 typed pure-image facade，28P tests与 `28Q -> 22` 仍是 final integration gate；这不改变 TURN-28 当前因 B owner return 而待改派的事实。
- TURN-34B：fixed card `:3-10,31-40` 已证明本卡不消费 TeamReturn mechanics，22 Repair #3 不改 Cloud TeamReturn production；把 TURN-22 留作 final integration gate而开放 34B source，拆分成立。
- TURN-34A：父级 `:326-377` 已把 production source pass 与 test-source Repair #1 分开。下游不得再因 34A test/compile 尚未通过而声称“34A production API 未落盘”。

### 5.2 已由父级纠正的反向误判：TURN-22 曾被提前当成 source-start-ready

- 父级 source-start reassessment 的前提是现有 frozen public API 可承载完整 `[CLICK_LEFT(150), SLEEP(500)]` list（TURN-22 card `:503-523`）。
- 实际 `InputSequences.java:67-87`、`InputActionQueue.java:319-365` 只接受 `Supplier<Boolean>`；`InputActionRequest.java:245-256` 对 frozen request 固定 `actions=List.of()`；`InputActionWorker.java:128-198,401-438` 只在 frozen callback 分支持 generation monitor，普通 list 仍走另一分支。当前 SHA 分别为 `2D1768E6...`、`BCD1E64A...`、`1CFF6130...`、`1359C236...`，mtime 均在 `06:00..06:03`，Euler 未改这些 production 字节。
- `TurnInputStepExecutor.java:60-67,125-145,166-177` 生成 list 后仍调用会 refresh 的 legacy `submitAndWait`；把该 list 嵌入 frozen callback 会触发 AGENTS `:293-295` 禁止的 queue-in-queue deadlock。
- Galileo 在 PRECHECK `:81-110` 给出同一反证，External A 在 TURN-22 `:596-637` 实盘确认。父级随后通过 TURN-22 `:639-658` 与新 TURN-28Q `:1-79` 正式覆盖旧 premise：`TURN-28Q` 是真实 source predecessor，不再把 28P“只剩测试”误写为足够。A 已在 28Q `:123-216` delivery，但尚无 parent source/test-source pass，故明确 DAG 仍是 `28Q review/pass -> 22`；本 helper不批准或阻断。

### 5.3 仍应 source-start reassessment 的候选：TURN-27

- registry 仍写 `TURN-27 PREFLIGHT COMPLETE / BLOCKED BY TURN-28 FINAL API`，`S=15+18+23+24+26+28`（plan `:1143`）。其中 15/18/23/24A/26 production/source gates 已通过（`:1127,1130,1136-1140`）；父级只需明确 split parent `TURN-24A` 满足 symbolic `TURN-24`。
- 新 TURN-28 fixed card 已冻结 NpcClick 四个 public API 与 Objective/SmartClick existing entry points（card `:36-53`），并把 Navigation/Task 列为只读（`:31-34`）。TURN-28 与 TURN-27 exact modify sets 分别见 plan `:1292-1297`，文件交集为 `0`。
- 当前 Cloud `NavigationService.java:185,216-248,524-570` 只有一个 `NpcClickService` field，主 `navigateToNPC` 只执行 map + current-map navigation；current-map active path仍是待迁移的 old macro。`MiniMapPointResolver.java:21,57-61,118,145,183` 只读调用 `ObjectiveTextRecognizer.mapTransform/coordinatePlausible`，这些 public static API已被 TURN-28 freeze。
- `696a12b0:src/main/java/com/bot/dhxy/service/NavigationService.java:204-247` 同样把 `navigateToNPC` 收口为 map + coordinate arrival，不点击 NPC；blob=`7857018df5c728f508cb58f1bb738081eec8356d`。当前 TURN-27 四 production SHA/mtime 为：Navigation `66D54807...` / `2026-07-15T03:02:03`，readability `CF782CD0...` / `2026-07-12T23:25:03`，point resolver `27049FF9...` / `2026-07-11T04:04:49`，route resolver `353D9862...` / `2026-07-11T10:28:22`；named test absent，且当前无 writer。
- **建议父级复判**：若确认 TURN-28 fixed public freeze 足以满足 TURN-27 的消费面，并在 fixed card 明确 `24A=>24`、`navigateToNPC` navigation-only、old macro “active-path 零调用而非全仓文字删除”，则可把 27 转 `SOURCE-START READY / FINAL INTEGRATION GATED BY TURN-28 DELIVERY+TEST`。TURN-28 尚未 delivery并不自动等于 consumer source不能写；真正条件应是其已冻结的 public surface 是否足够。这是候选，不是本 helper 的 READY/批准决定。

### 5.4 当前不属于 final-gate 误阻塞

- TURN-34C `S=19+21+22+23+34A+34B`（plan `:1151`）：19/21/23 已满足，34A production source已满足；但 `TURN-28Q -> TURN-22 item 2/3` 与 TURN-34B 尚未 delivery 是真实 source 缺口。待 22/34B production/source gate闭合后，不应再等 34A test-only Repair或各卡最终 build才开始 34C。
- TURN-35/36/37 的精确 `S` 见 plan `:1157-1159`；仍缺 22/27/28/34B 等真实 source surface，不是仅被 final approval gate 阻塞。38A 及后续依赖链 `38A -> 38B*/38M/38C -> 39 -> 40B/C/D -> 41 -> manifests/deletion` 见 `:1160-1183`，均有真实 predecessor/manifest/source 缺口。
- 扫描结论：正式未 CLAIM READY 只有 TURN-28；28Q 已 CLAIM。除已拆分的 28/34A/34B 外，**TURN-27 仍是唯一疑似被 `TURN-28 final API` 反向阻塞 source-start、应由父级立即复判的候选**。未发现第二张仅因 final approval debt 而可直接开源的卡；TURN-22 则已被证实是实际 source API 前提不成立。

## 6. External A/B/C/D 与 Internal 调度建议

| Lane | 当前卡 | 推荐下一动作 / 下一卡 |
|---|---|---|
| External A | TURN-28Q DELIVERED / awaiting parent review | 停止写五个 delivery文件；等 parent latest-byte source/test-source verdict。只有明确 pass 后才返回 TURN-22 item 2/3并保留 Cloud WIP `2D290759...`；不能以 delivery marker自行解门 |
| External B | 无卡；TURN-28 OWNER RETURNED | 不沿用旧 claim。当前最高优先未领卡仍是 TURN-28；仅在父级明确重新指派且能力/上下文足够时重新 true-EOF claim，否则由其它可用 External接手。TURN-28交付后可再候选父级冻结的 TURN-27 |
| External C | TURN-34A Repair #1 test-only active | 继续唯一 test，从 cutoff `82DFB872...` 增量完成并 delivery；production `532E6F84...` 只读。parent test-source review后，下一候选为父级冻结后的 TURN-27 |
| External D | TURN-34B CLAIMED | 继续两文件，当前 production `963B028C...`；完成 source delivery/review后，待 22 source + 34A production + 34B source同时满足，下一候选 TURN-34C |

Internal 建议：

1. Euler 已交付并停止；当前最有价值的 Internal readiness 是按 `5D563BBB...` / `88011CF1...` 做一次 28P latest-byte 静态 preflight，交给父级 source/test-source review使用。helper结论不算 reviewer approval。
2. Galileo 的 callback/list 反证已被父级转换为 28Q；当前最高优先 Internal readiness之一是按 28Q final SHA做 **public API + TURN-22 consumer compatibility** preflight，核对 action list一次提交、generation monitor、typed terminal与 callback non-regression，不把未运行测试冒充通过。
3. Laplace 已完成 TURN-28 freeze preflight；无需重复。TURN-28 新 owner delivery 后才对最新 SHA做 source readiness。B 的移交发现见 TURN-28 `:182-185`，可供继任者只读复用。
4. Leibniz 已 `PRECHECK_COMPLETE`；新 34A gap-matrix helper也已 `PRECHECK_COMPLETE`，但其 `:14-23` 基于较早 `44A30BFD...`，而 cutoff WIP 已是 `82DFB872...`。两者只能给 C 提供 gap索引，C delivery后仍须按最终 SHA重做同字节 readiness。
5. TURN-34B active-delivery helper已 `PRECHECK_COMPLETE`，确认 `963B028C...` + named test missing；它不是 delivery/reviewer结论。D 最终 delivery后必须按新 SHA重做 readiness，不能复用该 WIP preflight批准源码。
6. Dalton 已冻结 TURN-33 命令矩阵；Java writers活动期间不运行。stable-writer window后才由有权父级执行其命令门；本 helper未运行也不授权 Maven。
7. 其余空闲 Internal readiness优先准备 TURN-27 fixed-card decision（5.3），再准备 28Q parent verdict后的 TURN-22 consumer与 post-22/34B 的 TURN-34C delta。独立 reviewers只在父级完成 source/test-source review后按最新 SHA派发，本 helper不自任 reviewer。

## 7. PRECHECK 结论

- Snapshot 时正式未 CLAIM READY 是 TURN-28；活动 owner为 C=34A test Repair #1、D=34B。A/Euler均已 delivery并停止，B已归还、TURN-22 owner released；当前 active exact modify paths文件级互斥。
- 最高优先依赖链已被父级明确为 `TURN-28Q delivery -> parent source/test-source pass -> TURN-22 item 2/3`。28Q/28P都已 delivery但尚无 parent review；不能把 delivery冒充批准，也不能在 28Q review前把旧 callback-only缺口视为闭合。
- 唯一仍应立即复判的额外 source-start 候选是 TURN-27；其 public consumer surface已被 TURN-28 fixed card冻结且物理写集互斥，但 READY 必须由父级明确 `24A=>24`、consumer API sufficiency与 macro scope后产生。
- TURN-34C 与 Whole Tasks 仍有真实 source 缺口；T01-T04、named tests、reviews、compile/build 是最终批准门，不得反向冒充它们的 source-start blocker。
- **非父级批准：本报告不批准、不阻断、不关闭任何卡。**

<!-- TRUE_EOF: CR271 TURN DAG READY SCAN HELPER R2 PRECHECK_COMPLETE Nietzsche 019f6acc-b310-7310-aee7-8cff7296a844 snapshot-cutoff=2026-07-16T08:42:09.4952980-04:00 NON_PARENT_APPROVAL -->

# LATEST TRUE-EOF REFRESH - 2026-07-16 09:40 EDT

> 本段以 `2026-07-16T09:40:06.4594827-04:00` 源码快照和截至 `09:37:48` 的最新 fixed-report true EOF
> 覆盖本文件前面的 `08:42` 调度快照。前文保留为历史证据，不再代表本轮队列状态。

## 1. Helper 边界

- 身份：CR271 Internal DAG READY/source-start 扫描 helper；不是 implementation owner、reviewer 或父级。
- 唯一写入：本指定报告的本段追加。未修改 Java、卡片、权威计划、`ACTIVE_WORK.md` 或其它已有文档。
- 未运行 Maven、JUnit、compile、package、runtime、application/server、Task/UI、capture/input；未执行 Git mutation。
- 本段只列父级可复核的 source-start 候选与依赖门，不批准、不阻断、不领取、不改派任何卡。

## 2. 完整读取范围

- 完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271 当前段。
- 完整读取权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
- 完整追到当前物理 true EOF：External A/B/C/D lane 报告、`TURN-22D1`、`TURN-34A`、`TURN-34B`、
  `TURN-34BT1`、`TURN-34BP1`、`TURN-28Q`、28Q 独立 R1/R2，以及当前相关的 22D1/34A/34B/28S2
  latest preflight/decomposition reports。helper 报告只作证据，不当成卡片状态。
- 对账当前关键源码/测试的存在性、行数、mtime 与 SHA-256；未用 lane 旧尾覆盖更新的 child-card 真尾。

## 3. 最新父级事实对账

| Lane / chain | 最新权威 true EOF | 当前事实 |
|---|---|---|
| A / TURN-22D1 | child card `09:34:10` | Repair #1 已 `SOURCE+TEST DELIVERED`，production 保持已审 SHA，只改测试；当前应等父级复核，不再算 source writer。`09:37` public-fixture helper 只是最小替换建议，不是返修指令。 |
| B / TURN-34BT1 | child card `09:32:59` | B 从未 claim，已 `OWNER RETURNED`；named test 仍不存在，production `963b028c...` 未动。当前零 owner、零 test WIP。 |
| C / TURN-34A | original card `09:31:43` | C 已 RETURN；production `532e6f84...` 已由父级 source review passed 并只读；763 行 test `60e49ed9...` 是未完成 WIP，不是 Repair #1 delivery。 |
| D / TURN-34BP1 | child card `09:26:55` | 父级卡写 `READY / CLAIM REQUIRED / SOURCE-START OPEN`；截至 `09:40:06` 仍无 claim，两个目标文件保持初始 SHA。`09:32` start window 已过，但卡内尚无父级撤销/改派真尾。 |
| TURN-28Q | card `09:09:13` + independent R1/R2 `09:30:53/09:28:53` | 原卡仍记录 parent source/test-source pass；随后 R1 写 `0/2/0`、R2 写 `0/3/0` 的独立不通过意见。父级尚未在原卡吸收、裁决或冻结返修写集。 |

真尾优先级说明：A/B/C 的 child/original card 都新于各 lane 报告 `09:26:55` 的旧状态；调度必须使用卡片新尾。

## 4. 当前源码快照

| Slice surface | 当前字节 |
|---|---|
| D1 production | `TurnInputStepExecutor.java` 264 行，SHA `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e` |
| D1 test | `TurnInputStepExecutorContractTest.java` 695 行，SHA `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81` |
| 34BP1 production | `TaskExecutionContext.java` 491 行，SHA `6d4e4a20a6fb4b6dba6a59cb45e95dd39c78a0415b9b2a650d75f9704151d003` |
| 34BP1 test | `TaskExecutionContextTurnContractTest.java` 753 行，SHA `d667d6958dbc38a6fccf2ba5e562cecd4ef60629df7a4cd55e347c9dbd9ed945` |
| 34B retained production | `TaskMaintenanceService.java` 1224 行，SHA `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc` |
| 34BT test | `TaskMaintenanceTurnContractTest.java` 不存在 |
| 34A production | `AutoCombatService.java` 852 行，SHA `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` |
| 34A test | `AutoCombatServiceTurnContractTest.java` 763 行，SHA `60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6` |
| TURN-28 strict baseline surface | `NpcClickService.java` 3374 行，SHA `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` |

## 5. 下一批真实 implementation 小片

以下三片的技术 start prerequisite 已落盘、Java 写集两两为零交集；它们仍各自需要父级在真尾完成
assignment/reassignment 与 claim 控制。本 helper 不把候选表写成父级 `READY`。

### 5.1 TURN-34BP1 - shared exact native-metadata checkpoint

**精确写集**

1. Cloud `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
2. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`
3. `2026-07-16-turn-card-TURN-34BP1.md`

**Source-start gate**

- 父级 fixed card 已冻结合同，两个目标 SHA 仍与卡片初始快照一致，当前没有源码增量或第二 owner。
- 因 `09:32` claim window 已过，必须由父级确认继续给 D、撤销后改派或刷新 start window；helper 不能替父级认定晚 claim 有效。
- 不等待 TURN-22、TURN-34BT1、独立 review 或 Cloud build 才写这两个互斥文件。

**Final review/build gate**

- production+test delivery、父级 source/test-source review、两名独立 reviewer、点名测试和 stable-writer Cloud compile/build。
- TURN-34B 聚合还要等后续 P1-2/P2 production slice、BT test tranches、TURN-22 final gate 与 34A 六 API 兼容复核。

### 5.2 TURN-34BT1 - TaskMaintenance named-test tranche 1

**精确写集**

1. Create Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
2. `2026-07-16-turn-card-TURN-34BT1.md`

**Source-start gate**

- B 已在 child-card 真尾归还且从未 claim；named test 仍不存在，零 WIP 可交接，retained production SHA 未漂移。
- 父级需先在卡内完成撤销/新 assignment，再由 replacement 真尾 claim；不能沿用 B/D 的旧 NEXT 文本。
- 该 test-source 可与 34BP1 并行落码。忠实测试可先表达预期 exact-context/scoping 红灯，但本轮不运行测试，
  test-only worker 不得改 production 或弱化断言。

**Final review/build gate**

- BT1 delivery 与父级 test-source review；BP1 和后续 34B production repair；同一 named test 的 BT2/BT3/BT4
  必须串行 handoff，不能并发写同文件。
- TURN-22 final integration、两名独立 reviewer、完整 `TaskMaintenanceTurnContractTest`、Cloud compile/build 仍保留。

### 5.3 TURN-34A Repair #1 - AutoCombat named-test completion

**精确写集**

1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`
2. `2026-07-16-turn-card-TURN-34A.md`

`AutoCombatService.java` 固定为父级已审 SHA `532e6f84...`，只读。

**Source-start gate**

- C 已在原卡真尾归还 owner，763 行 WIP 与 SHA 已明确交接；当前无 replacement claim。
- 父级可冻结 replacement 从当前字节继续，不需要等待 34BP1/34BT1、TURN-22 build 或独立 reviewer 才写该测试。
- replacement 必须先修当前可静态定位的 import/constructor 差额，再补真实 action、四 caller、timing/recovery/
  maintenance、terminal/UUID 证据；不得建第二测试或修改已通过 production。

**Final review/build gate**

- 一次完整 Repair #1 delivery、父级 source/test-source review、两名独立 reviewer、
  `AutoCombatServiceTurnContractTest` fresh exit 0 与适用 Cloud compile/build。
- 最终测试/构建使用届时稳定的 `TaskExecutionContext` 与 TaskMaintenance 六 API 字节，不能用当前中间 WIP 代替。

## 6. 写集互斥矩阵

| Candidate | 34BP1 context+test | 34BT1 maintenance test | 34A auto-combat test |
|---|---:|---:|---:|
| TURN-34BP1 | self | 0 | 0 |
| TURN-34BT1 | 0 | self | 0 |
| TURN-34A Repair #1 | 0 | 0 | self |

三片均与已交付待审的 DHXY TURN-22D1 两文件写集为 `0` 交集。逻辑/API 兼容留在最终 review/build gate，
不反向制造文件级 source-start 等待。

## 7. 本轮不能作为 implementation source-start 的项目

1. **TURN-22D1**：Repair #1 已于 `09:34:10` 交付，当前是父级 review gate，不是待领取 source slice。
   `09:37` helper 给出的共享 fixture 配方不能自行变成 Repair #2；是否要求清掉本测试新复制的 `Unsafe` residual
   由父级审查决定。
2. **TURN-28Q**：只能记录原卡 parent pass 与后发 R1/R2 不通过意见并存。父级尚未裁决、未回写原卡、未冻结
   repair exact write set，helper 不得据 reviewer 报告直接派 repair，也不得沿用旧 pass 宣称最终门已过。
3. **TURN-28S2**：只有 `PRECHECK_COMPLETE` helper，child card 尚不存在；它不是 `READY`。其 Alt-shortcut
   source-start 假设还必须由父级结合 28Q R1/R2 的 exact-binding Alt finding 重新核对后再决定是否冻结。
4. **TURN-34B 后续 production P1-2/P2**：父级只说明在 BP1 后开同 lane 后续片，尚无 fixed child card/claim；
   当前不能提前写 retained production。
5. **TURN-34BT2/BT3/BT4**：只是 helper decomposition label，且与 BT1 共用唯一 named test，必须等 BT1
   delivery/release 后串行冻结，不能拿来填并发槽。
6. **TURN-27、34C、35/36/37、38A..39、40B..47**：仍各有未满足 source dependency、parent freeze、manifest
   或 user runtime gate；Foundation/早期业务卡剩余 named tests/review/build 只是最终门，不产生新的 Java source 卡。

## 8. 父级可复核的批次形状

- 当前最小并发形状是三片：`34BP1 + 34BT1 replacement + 34A Repair #1 replacement`。
- 三片开始前各自只缺父级 owner/assignment 真尾与真实 claim；没有技术理由让其中一片等待另两片先写完。
- A 的 D1 与 28Q 先进入父级裁决/review 队列；只有父级产生新的 exact repair card 后，才重新参与 implementation 排班。
- 所有 named test、双 reviewer、compile/build 都保留在 final gate；本 helper 未把它们写成 source-start pass，
  也未用 helper 报告替代任何 fixed card。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF PRECHECK_COMPLETE

# PHYSICAL TRUE-EOF AUTHORITY NOTE - 2026-07-16 09:49 EDT

本报告是追加式扫描历史。物理位置较早的 `TRUE-EOF CLAIM/DELIVERY DELTA - 09:47:27 EDT` 是对
`FINAL TRUE-EOF CORRECTION - parent batch 09:38 EDT` 的后发状态覆盖；两段合并后的当前权威读法如下：

- A / TURN-28Q Repair #2：已于 `09:46:07.835` 真尾 claim，active implementation，四文件写集不变；
- B / TURN-28S2：fixed `READY / CLAIM REQUIRED / SOURCE-START OPEN`，截至本扫描仍待 fresh claim；
- C / TURN-34AT0：已于 `09:47:27.553` delivery，test final SHA=`98e65586...`，当前是父级 review gate，
  不是待写 source slice；
- D / TURN-34BP1：fixed replacement READY，截至本扫描仍待 fresh claim；
- TURN-34BT1：依赖满足且与上述写集互斥，但仍需父级 assignment + fresh claim；
- TURN-28S3：只有非绑定 helper preflight，且与 S2 共写 `NpcClickService.java`，不是 READY。

TURN-22D1 的独立 R1 于 `09:49:12.176` 在其报告真尾记录
`APPROVED / P0/P1/P2=0/0/0 / SOURCE-AND-TEST-SOURCE-ONLY`；这是外部 reviewer 的既有结论，不是本 helper
的批准。D1 仍处于 final gate：另一份所需独立 review、点名测试、DHXY compile及父级聚合条件均未由本扫描越过。

本 helper 不批准、不阻断、不派工、不 claim；所有 source-start authority 只取 fixed card，helper 的
`PRECHECK_COMPLETE` 从不等于 READY。

TRUE_EOF PRECHECK_COMPLETE

# TRUE-EOF CLAIM/DELIVERY DELTA - 2026-07-16 09:47:27 EDT

> 本段只更新上一段冻结后的 claim/delivery 时点；候选写集与 source-start/final-gate 边界仍以上一段为准。
> 截止本段落笔前的纯读取尾检，09:47:27 之后未出现新的 fixed-card 更新。

## 1. 已发生的真实 lane 推进

1. **A / TURN-28Q Repair #2 已通过 source-start claim gate。**
   - 原卡真尾已追加 `EXTERNAL-A REPAIR #2 CLAIMED - 2026-07-16T09:46:07.835-04:00`；四文件 claim SHA
     与父级 Review #3 一致。
   - A 当前是 active implementation owner，不再是“待 claim candidate”；截至本扫描时尚无 Repair #2 delivery。
   - 写集仍严格为 `InputActionRequest.java`、`InputActionQueue.java`、`InputActionWorker.java`、
     `InputActionFrozenExclusiveContractTest.java`，`InputSequences.java` 与 keyboard/focus/callers 只读。
2. **C / TURN-34AT0 已完成本小片 source delivery。**
   - C 于 `09:46:07.674` claim，并于 `09:47:27.553` 追加 `EXTERNAL-C TEST-SOURCE DELIVERED` 后停止编辑。
   - `AutoCombatServiceTurnContractTest.java` 从 763 行 SHA
     `60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6` 变为 762 行 SHA
     `98e655860873a640d96c2b528a19a18fd3c361f69f654c1237cf93ede869ac3a`；production SHA
     `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` 保持未触碰。
   - AT0 现在进入父级 test-source review gate，不再属于下一批“待写 implementation”；它没有 compile/test/semantic
     pass，也没有产生 AT1+ 的 source-start authority。

## 2. 截止当前真尾的 implementation frontier

| 类别 | 小片 | 当前 source-start 状态 | 仍需的直接动作 |
|---|---|---|---|
| Active writer | A / TURN-28Q Repair #2 | 已 claim，正在 implementation | A 按四文件冻结写集交付后停写；随后进入 parent review 与 final gates |
| Ready fixed slice | B / TURN-28S2 | 父级 fixed card 为 `READY / CLAIM REQUIRED / SOURCE-START OPEN`，尚无 claim/delivery 真尾 | fresh B 在 fixed card 真尾 claim 后才可写 `NpcClickService.java` 四个 Alt sites |
| Ready fixed slice | D / TURN-34BP1 replacement | 父级 fixed card 为 `REPLACEMENT READY / CLAIM REQUIRED`，尚无 fresh claim/delivery 真尾 | fresh D 重新 claim 后才可写 `TaskExecutionContext.java` 与唯一 named test |
| Unassigned real slice | TURN-34BT1 replacement | dependency 已满足、写集与 A/B/D 互斥，但当前没有新 parent assignment/owner | 父级给空闲 lane assignment，再由 fresh worker 真尾 claim；helper 不能代派 |
| Parent review gate | C / TURN-34AT0 | source delivered，owner 已停写 | 父级 review；只有父级另冻 AT1+ fixed child card 后才产生 C 的下一 source-start |

因此，按“下一批尚可真正开始写源码”的严格口径，当前是：

- **B / TURN-28S2** 与 **D / TURN-34BP1 replacement**：已有 fixed source-start authority，只差各自 fresh claim；
- **TURN-34BT1 replacement**：真实且互斥，但还差父级 assignment + fresh claim；
- **A / TURN-28Q Repair #2** 已经开始，不重复计作下一次待领取名额；
- **C / TURN-34AT0** 已交付，不重复计作 implementation source-start。

A 的 active write set、B/D 两个待 claim write set和未分配 BT1 write set两两文件交集仍为 `0`。AT0 delivery 文件也不与
它们重叠，但父级审查中的文件不能被 AT1+ 或第二 writer提前继续修改。

## 3. 新 helper 不能提升为 READY

- `2026-07-16-turn-28q-repair2-preflight-helper.md` 只复核父级已冻结的 Repair #2 边界；真实 claim 来自 TURN-28Q
  原卡真尾，不来自 helper 的 `PRECHECK_COMPLETE`。
- `2026-07-16-turn-28s3-preflight-helper.md` 只是非绑定下一片建议。S3 与 S2 共写 `NpcClickService.java`，必须先等
  S2 delivery、父级 source review、owner release、S2 final SHA 重钉和新的 parent fixed card；其第三次 probe 后
  `300ms` 文本冲突还必须由 parent/user 显式冻结。当前不能列为 READY 或并发片。

## 4. Final review/build gate 保持不变

- TURN-28Q Repair #2：delivery、父级 source/test-source review、独立 reviewer、
  `InputActionFrozenExclusiveContractTest` fresh exit 0 与 DHXY compile；TURN-22D1 最终 integration/build继续等待它。
- TURN-28S2：父级 source review后还要完成 TURN-28 后续串行 production/test slices、唯一
  `NpcClickTurnContractTest`、独立 reviewer 与 Cloud build。
- TURN-34AT0：当前 delivery 只闭合 compile-surface source；AT1+ semantic matrix、parent review、独立 reviewer、
  点名测试和 Cloud compile/build均未被本次交付越过。
- TURN-34BP1 / TURN-34BT1：各自 delivery/review/test/compile之外，TURN-34B 仍要后续 production与 BT2-4、
  TURN-22 final integration及34A API compatibility gate。

本增量不批准、不阻断、不派工、不 claim；只记录 fixed-card true EOF 已发生的 source-start 与 delivery 状态变化。

TRUE_EOF PRECHECK_COMPLETE

# FINAL TRUE-EOF CORRECTION - 2026-07-16 parent batch 09:38 EDT

> 本追加段覆盖本报告此前的 `LATEST TRUE-EOF REFRESH - 2026-07-16 09:40 EDT` 调度结论。
> 原因不是 helper 改变判断，而是父级同批 fixed cards / lane reports 在该段落写入后才刷新到文件系统真尾。
> 下文只转录并归并父级最新 fixed-card 事实，不作批准、阻断、派工或 claim。

## 1. 最终真尾输入与时间顺序

本轮已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部、总计划第 14-19 节，
并重新读到本次变更相关的最新 fixed reports 真尾，包括：

- External A/B/C/D lane reports；
- TURN-22D1、TURN-28Q、TURN-28S2、TURN-34A、TURN-34AT0、TURN-34B、TURN-34BP1、TURN-34BT1 fixed cards；
- TURN-22D1、TURN-28Q R1/R2、TURN-28/28S2、TURN-34A/34B/34BT/34BP 的最新 helper/reviewer reports。

父级 `09:38:31.235 EDT` 批次在文件系统于约 `09:42:32` 显现；总计划随后于 `09:44:10` 刷新。
因此用户消息所带的起始快照 `A=D1 repair / B=34BT1 window / C=34A window / D=34BP1 READY /
28Q R2 待父级裁决` 已被以下父级真尾部分推进：

1. **TURN-22D1**：父级 Review #2 已记录
   `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING`；
   production SHA=`a64422b...`、test SHA=`f5a7992...`（695 行），原 owner 已释放。它现在只处于最终审查/测试/构建门，
   不再是 A 的待写 implementation slice。
2. **TURN-28Q**：父级已裁决 R1/R2，并用 Review #3 覆盖旧 Review #2，记录
   `P0/P1/P2=0/4/0 / REPAIR #2 REQUIRED`。所以它已不再是“R2 BLOCKED 待父级裁决”，而是父级冻结了
   Repair #2 精确写集并交给 fresh A claim 的下一片。
3. **TURN-28S2**：父级已新建 fixed child card，记录
   `READY / CLAIM REQUIRED / SOURCE-START OPEN`，并把 fresh/restarted B 从已归还的 34BT1 window 转到本片。
   此 fixed card 覆盖此前仅有 helper precheck、不能视为 READY 的旧事实。
4. **TURN-34A**：父级接受 C 归还，production SHA=`532e6f84...` 保持已审只读；原 763 行 test WIP 被拆成
   串行的 AT0、AT1+。父级已新建 TURN-34AT0 fixed child card并分配 fresh/restarted C claim。
5. **TURN-34BP1**：原 D 超时 assignment 已被父级撤销；同一两文件小片现为
   `REPLACEMENT READY / CLAIM REQUIRED`，供 fresh/restarted D claim。
6. **TURN-34BT1**：B 已归还且零新增 test 字节；该片仍是父级可再分配的真实 replacement slice，但不再占当前 B lane。

## 2. 当前父级批次：四片可独立 source-start 的真实 implementation

以下“可开始”只表示：前置 source dependency 已满足、父级已冻结真实 source/test write set，且 fresh owner 完成 fixed-card
真尾 claim 后可以编辑。它不表示 source review、测试、双 reviewer、compile/build 或整卡最终通过。

| Lane | 最新父级小片 | 精确 source/test 写集（卡片自身另计） | Source-start gate | Final review/build gate |
|---|---|---|---|---|
| A | TURN-28Q Repair #2 | DHXY `InputActionRequest.java`、`InputActionQueue.java`、`InputActionWorker.java`、`InputActionFrozenExclusiveContractTest.java` | 父级 Review #3 已冻结四项 P1 修复范围；fresh A 必须先在 TURN-28Q 原卡真尾 claim。`InputSequences.java`、keyboard/focus services 与 callers 均只读 | delivery；父级 source/test review；所需独立 reviewer；点名测试 fresh exit 0；DHXY compile。TURN-22D1 的最终 integration/build 仍等本共享输入基座稳定 |
| B | TURN-28S2 | Cloud `NpcClickService.java` | fixed card、初始 3374 行 SHA=`cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441` 与 strict-696 byte parity 已冻结；fresh B 真尾 claim 后只改卡内四个 Alt shortcut site | 本片 delivery 与父级 source review；TURN-28 其余串行小片、唯一 `NpcClickTurnContractTest`、独立 review 与 Cloud build 仍在最终门。本片没有 test write set，也不能替代 TURN-28Q 的四项 DHXY repair |
| C | TURN-34AT0 | Cloud `AutoCombatServiceTurnContractTest.java` | fixed card、初始 763 行 SHA=`60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6` 已冻结；fresh C 真尾 claim 后只修 import/constructor/compile surface，production 保持只读 | AT0 只交付可继续审的 test source，不得宣称 semantic pass；AT1+ 必须在同文件串行完成，再做父级 test review、独立 review、点名测试与 Cloud compile/build |
| D | TURN-34BP1 | Cloud `TaskExecutionContext.java`、`TaskExecutionContextTurnContractTest.java` | replacement fixed card 与初始 SHA 仍为 `6d4e4a20...`（491 行）/`d667d695...`（753 行）；fresh D 必须重新真尾 claim，旧超时 owner/claim 不可继承 | delivery；父级 source/test review；独立 review；点名测试与 Cloud compile。TURN-34B 整体还需后续 production repair、BT test tranches、TURN-22 final integration 和 34A API compatibility gate |

### 2.1 A 的四项父级 Repair #2 语义边界

本 helper 不重判严重级别，只记录 TURN-28Q 父级 Review #3 已冻结的四项修复目标：

1. typed stop closure / final stop；
2. frozen Alt exact binding 且零 refresh；
3. paused cancellation completion；
4. deterministic pause barrier。

这些目标只授权上述四个 DHXY 文件写集；不能顺带修改 `InputSequences.java`、焦点/键盘服务或调用方。

### 2.2 B 的四个 TURN-28S2 production site

父级 fixed card 只允许在 `NpcClickService.java` 把以下四处 shortcut 各收敛为一次 `TurnGameClient` action：

1. `ALT_C + 700` generic second pipeline；
2. `ALT_C + 700` confirmed flying；
3. `ALT_A + 350` grounded direct mode；
4. `ALT_4 + 400` name layer。

这是一张独立 Cloud action-path source card，不把 TURN-28Q exact-binding 问题视为已解决，也不增加测试写集。

## 3. 写集互斥核对

| Candidate | A: 28Q input core+test | B: 28S2 NpcClick | C: 34AT0 AutoCombat test | D: 34BP1 context+test | Unassigned: 34BT1 maintenance test |
|---|---:|---:|---:|---:|---:|
| A: TURN-28Q Repair #2 | self | 0 | 0 | 0 | 0 |
| B: TURN-28S2 | 0 | self | 0 | 0 | 0 |
| C: TURN-34AT0 | 0 | 0 | self | 0 | 0 |
| D: TURN-34BP1 | 0 | 0 | 0 | self | 0 |
| TURN-34BT1 replacement | 0 | 0 | 0 | 0 | self |

当前 A/B/C/D 四片文件级交集均为 `0`：A 在 DHXY；B/C/D 在 Cloud 但分别写 production service、AutoCombat test、
context production+test。逻辑/API 兼容关系留在 final review/build gate，不反向制造文件级 source-start 等待。

未来串行约束仍保留：

- TURN-34AT1+ 与 AT0 共用 `AutoCombatServiceTurnContractTest.java`，必须等 AT0 delivery/release；
- TURN-34BT2/BT3/BT4 与 BT1 共用 `TaskMaintenanceTurnContractTest.java`，必须按父级顺序逐片 handoff；
- TURN-34B 后续 production repair 要等 BP1 边界稳定并由父级另冻真实 child card。

## 4. 依赖已满足但当前未占 A/B/C/D lane 的真实小片

### TURN-34BT1 replacement - TaskMaintenance named-test tranche 1

**精确写集**

1. Create Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
2. TURN-34BT1 fixed child card

**Source-start gate**

- 父级卡记录 retained production SHA=`963b028c...`、B 零新增 test 字节并已归还；该真实 test-source slice 与当前
  A/B/C/D 四片写集互斥。
- 它当前没有新 owner。只有父级重新 assignment 且 fresh worker 在卡片真尾 claim 后才能写；本 helper 不把
  “replacement READY”当作自行派工权限。

**Final review/build gate**

- BT1 delivery 与父级 test-source review；随后 BT2/BT3/BT4 在同一文件串行完成；
- BP1 与后续 34B production repair、TURN-22 final integration、独立 review、完整点名测试和 Cloud compile/build
  仍在最终门。

## 5. 不是当前 source-start implementation 的项

1. **TURN-22D1**：父级 production/test-source review 已完成，剩余的是独立 review、点名测试和 DHXY build；
   它是 final gate，不是 helper 可再次派发的 source slice。
2. **TURN-28Q R1/R2 reviewer/helper reports**：它们是父级裁决输入，不是 READY 卡。真实 source-start authority
   来自父级已回写的 TURN-28Q Repair #2 精确写集。
3. **TURN-28/28S2、34A/34B/34BT/34BP 的 helper reports**：`PRECHECK_COMPLETE` 只表示预检结束，不能用来
   claim、批准或替代 fixed child card。
4. **TURN-34AT1+、TURN-34BT2/BT3/BT4**：虽已有父级串行分解方向，但与前序共用测试文件，前序未 release 前
   不是并发 source-start。
5. **TURN-34B 后续 production P1-2/P2**：尚无新的 exact fixed child card/claim，不能提前修改 retained production。
6. **TURN-27、34C、35/36/37、38A..39、40B..47 及 Foundation 补债最终门**：仍分别受前置 source、父级冻结、
   manifest、串行 consumer 或 user runtime gate 约束；本轮没有新增可独立落码的真实 fixed slice。

## 6. 父级可复核的 source-start / final-gate 分界

- **当前立即可形成的四 lane source batch：**
  `A=TURN-28Q Repair #2 + B=TURN-28S2 + C=TURN-34AT0 + D=TURN-34BP1 replacement`；
  每片都只差 fresh owner 的 fixed-card 真尾 claim，四片写集互斥。
- **额外可用真实 slice：** TURN-34BT1 replacement 的 dependency 与写集条件已满足，但需父级另分配空闲 lane 后再 claim。
- **Source-start 不等待：** 双 reviewer、点名测试执行、compile/build、整卡 approval 或 sibling slice 完成；这些是 final gate。
- **Final gate 不被本扫描缩短：** 每张卡仍按其 fixed card 保留 parent review、independent review、required tests、
  applicable compile/build 与上游/下游 integration 条件。
- 本 helper 没有把任何 helper/reviewer report 标成 READY，没有产生 assignment/claim，也没有批准或阻断任何卡。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF PRECHECK_COMPLETE

# FINAL PHYSICAL TRUE-EOF DELTA - 2026-07-16 09:50 EDT

本段是本报告的最终时点覆盖，合并前述 `parent batch 09:38`、`claim/delivery 09:47` 与 `D1 R1 09:49`：

| Lane/片 | 当前真实状态 | Source-start / next gate |
|---|---|---|
| A / TURN-28Q Repair #2 | `09:46:07.835` 已 claim，active implementation | 四文件 delivery 后进入 parent review；不是待领取片 |
| B / TURN-28S2 | fixed `READY / CLAIM REQUIRED / SOURCE-START OPEN`，尚无 claim | fresh B 真尾 claim 后可写唯一 `NpcClickService.java` 四个 Alt sites |
| C / TURN-34AT0 | `09:47:27.553` 已 delivery，test SHA=`98e65586...` | 当前是 parent review gate；AT1+ 尚无 fixed source-start card |
| D / TURN-34BP1 | fixed replacement READY，尚无 fresh claim | fresh D 真尾 claim 后可写 context production + sole named test |
| TURN-34BT1 | dependency satisfied、与 A/B/D 写集互斥、无新 owner | 仍需 parent assignment + fresh claim，helper 不代派 |

当前尚待开始的真实 implementation frontier 是 B / TURN-28S2 与 D / TURN-34BP1；TURN-34BT1 是可由父级另分配的
互斥备用真实片。A 已在写，C 已交付。TURN-28S3 仍只是非绑定 helper preflight，与 S2 共写同一 production 文件，
不是 READY。

TURN-22D1 independent R1 已在其报告记录 source/test-source-only `APPROVED 0/0/0`；本 helper只转录该外部结论。
D1 仍保留另一所需 independent review、点名测试、DHXY compile和父级聚合 final gates。

本扫描没有批准、阻断、assignment 或 claim；`PRECHECK_COMPLETE` helper 永远不作为 READY authority。

TRUE_EOF PRECHECK_COMPLETE
