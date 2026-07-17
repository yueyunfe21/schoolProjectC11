# CR271 TURN-34AT1 当前 WIP delivery-preflight（Internal helper）

## 1. 身份、边界与快照口径

- 角色：CR271 Internal helper，只做 TURN-34AT1 当前 WIP 的只读 delivery-preflight；不是 implementation owner、reviewer、manager 或 approver。
- 本报告不写 `Approved`、不替代父级 review、不改变任何卡片状态，也不授权 AT2 或 TURN-34A 整体继续。
- 唯一写入是本报告。未修改任何 Java、卡片、`ACTIVE_WORK.md`、权威计划、dashboard、POM、resource 或其它测试。
- 未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input，也未执行任何 Git mutation。
- 最终源码冻结时点：`2026-07-16T14:30:15.2218514Z`。External C 并发写入期间确有源码漂移，因此本文只对下列 SHA 快照负责，不把中途字节或交付声明写成批准。

## 2. 已读权威材料与当前锚点

以下材料均已完整读取；计划第 14-19 节按最新物理边界逐段读完，三张卡和相关 fixed helper 均读至物理 true EOF。

| 材料 | SHA-256 / mtime UTC / 当前说明 |
|---|---|
| `AGENTS.md` | `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` / `2026-07-11T21:24:59.6508563Z` |
| `docs/DHXY_CONTEXT.md` | `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` / `2026-07-16T07:32:25.4942933Z` |
| `docs/ACTIVE_WORK.md` 顶部 CR271 | `49437D7F9A0DB1CBDB6A7472A37C458ADBEF2A0A41636072065663E2D09732FA` / `2026-07-16T14:29:35.7297760Z`；顶部 `10:27` 段仍写 C Repair #1 active，时间早于 AT1 子卡 `10:27:27` delivery |
| 权威计划第 14-19 节 | 全文件 `A92A3C51D96939F79F33390D7BAF8D8B0015802440EF9191332308EEE9CA8786` / `2026-07-16T14:29:35.7312815Z` / 1741 行；当前第 14、19 节分别始于 `:1060`、`:1510` |
| HTTPS turn 协议规格 | `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` / `2026-07-16T07:13:40.4741505Z` |
| `docs/业务逻辑.md` | `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` / `2026-07-11T23:40:58.4813186Z`；唯一业务基线为 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` |

当前三张卡：

| 卡片 | 当前 SHA-256 | 当前物理 true EOF |
|---|---|---|
| TURN-34A | `375DA398CDF9415502FDD8AC883F642603A8FE0B25C32D7EC06487EB84D3A3E3` | `CHILD-AT1 REPAIR-1-REQUIRED P0P1P2=0/2/0 PRODUCTION-FROZEN` |
| TURN-34AT0 | `FD89859159293B1FBD5942B861345CF20A466887405B8B8C305D0DC6EAF96A9E` | `REVIEW-2 PASSED ... TEST-SOURCE-PASSED OWNER-RELEASED TURN-34AT1-NEXT` |
| TURN-34AT1 | `BB1A92C9020ED483364C1B2A379C993A7F059266CCAB450DA7BA46E4C23BCABB` | `EXTERNAL-C REPAIR #1 TEST-SOURCE DELIVERED SHA=35116f19...`；尚无父级 Repair #1 复审结论 |

已读至 true EOF 的相关 fixed helper 还包括：

- `turn-34A-readiness-preflight-helper` `55507915...`、`turn-34A-post-turn33-readiness-helper` `0FA7F911...`、`turn-34A-launch-preflight-helper-r2` `793E6B5F...`、`turn-34A-delivery-preflight-helper-r2` `BAEDC27F...`。
- `turn-34a-repair1-current-test-preflight-helper` `10258F4A...`、`turn-34a-repair1-test-matrix-preflight-helper` `52D1A48E...`、`turn-34a-current-delivery-delta-helper` `B0464BC2...`。
- `turn-34at1-readiness-helper` `4CD2E5B4...`；另补读相邻边界 `turn-34at2-readiness-preflight-helper` `471602F8...`，其条件就绪意见不构成 AT2 开工授权。

## 3. 两仓只读状态

`2026-07-16T14:30:15.2218514Z` 的只读 Git 快照：

| 仓库 | branch / HEAD | porcelain 状态 |
|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` / 无 upstream | 714 项：` D=1`、` M=43`、`??=670`；status 文本 SHA `36B01E9E0AE4AEFCB7CBBCB1910E4D474908E8935DD03B16F7BAC7D848CB95BE` |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` / 无 upstream | 550 项：` M=9`、`??=541`；status 文本 SHA `E85B93E8CE8DD62EF5FA8D729E9E723F458BD4A9E8351081636D8DC2BD132C72` |

Cloud named test 被 `.gitignore:15:src/test/` 忽略，因此普通 `git status` 不显示它；当前交付必须以本文记录的实际文件 SHA/mtime 为准，不能从 status 静默推断测试不存在或已持久化。

## 4. 并发漂移与最终源码快照

读取期间 External C 正在写同一测试，观测到三个真实阶段：

1. 中途快照：864 行，SHA `30E565CBE4A40957C118BACF1F28F9A267B64E872E095D59B7EBAA94AE7FB269`，mtime `2026-07-16T14:11:39.6977141Z`。
2. 首次交付：963 行，SHA `6BE1F3BF0F7037AA34AC9BC95C8245B93E59A88A30966D95FFBC1A77FCB45C68`，mtime `2026-07-16T14:16:09.0284871Z`；父级随后给出 `0/2/0 / REPAIR #1 REQUIRED`。
3. 当前 Repair #1 交付：1020 行、22 个 `@Test`，SHA `35116F19F57F170A4CA6E56FADF11D9047B76520A8F61F24B86FB63E11EC10A4`，mtime `2026-07-16T14:26:47.9931857Z`；在完整重读后至最终复算未再漂移。

生产与模板快照：

| 文件 | SHA-256 / mtime UTC / 大小 |
|---|---|
| Cloud `AutoCombatService.java` | `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9` / `2026-07-16T10:29:17.7816908Z` / 852 行、46,414 bytes |
| Cloud `AutoCombatServiceTurnContractTest.java` | `35116F19F57F170A4CA6E56FADF11D9047B76520A8F61F24B86FB63E11EC10A4` / `2026-07-16T14:26:47.9931857Z` / 1020 行、50,788 bytes |
| committed `flag_battle.png` | `3689C35D801CA4295F62AD3C2DD5BB8AF955FF7CD285D8185B99360D4FAE2A47` / `2026-05-08T20:14:51.7613671Z` / 36x11、852 bytes |

下图是 **Cloud committed Stage-1 battle-flag 模板**，不是本次 live incident 截图，也不是历史运行截图：

![Cloud committed Stage-1 battle-flag template](D:/mavenProject/dhxy-cloud-brain/src/main/resources/images/template/battle/flag_battle.png)

## 5. AT1 当前增量的真实覆盖

### 5.1 Stage-1 battle flag

当前源码真实覆盖该路径，而非只声明 helper：

- 测试 `:379-434` 从 `FREE` 经 public `probeWindowCombatStateReadOnly(context,"fivering")` 驱动 production `AutoCombatService`、真实 `BattleRadarService`、真实 `PackagedTemplateAssets`。
- `battleFlagRoiPng(...)` 把 committed 模板真实像素绘入请求 ROI 的内存 PNG；结果为 `IN_COMBAT`，`GameContext` 同步为 `IN_COMBAT`。
- `executeCalls==1`、`actions.size()==1`、scripted replies 耗尽；Stage-1 命中后没有 Stage-2/3 command。

### 5.2 exact one command / UUID / raw PNG

- command：当前直接断言 exact device/window、一个 index-0 `CAPTURE`、screen region `(1074,680,51,20)`、`UPLOAD_IMAGE` 和 120 秒 timeout。
- Repair #1 已把完整最小 CAPTURE union shape 锁住：`inputAction/input/waitMs/match/localService` 在 `:409-413` 全部直接断言为 null；父级 Review #1 的第一项缺口在当前字节中已有对应修复证据。
- UUID：`collectCanonicalActionIds` 对每个 id 做非空与 `UUID.fromString(id).toString()==id`；`assertFreshCanonicalIds` 同时锁数量和集合基数。正例为 1 个 canonical UUID。
- raw PNG：outcome 使用同一 actionId 和完整 `TurnWindowMetadata`；frame 锁 `image/png`、purpose、region、width/height、sourceStepIndex=0，并用真实 `pngBytes()` 重算 SHA 与 metadata 比较。真实模板匹配成功进一步证明 production 消费了该 raw frame，而非只看 metadata。

### 5.3 first CAPTURE terminal/uncertain 与零 fallback

- command status 覆盖 `BUSY`、`DUPLICATE_ACTION_ID`、`TIMED_OUT_UNCERTAIN`、`INTERRUPTED_UNCERTAIN`；outcome status 覆盖 `FAILED`、`STOPPED`、`DUPLICATE_OR_UNCERTAIN`。
- 每例从 `IN_COMBAT` 开始并保守保持 `IN_COMBAT`；`STOPPED` case 的 latest metadata 明确 `stop=false`，与既有 confirmed-stop、pre-command 零 command 测试分开。
- 各四态/三态测试均断言一次 execute 与 replies 耗尽。新增 `:532-557` 还在同一 harness 中连续驱动全部七态，断言 7 次 invocation、7 次 command、7 个 canonical 且互不重复的 UUID、脚本耗尽；没有 Stage-2/3、retry、resend、compensation、fallback 或第二 action。
- 父级 Review #1 的第二项“单元素 `distinct()` 为恒真证据”在当前字节中已有针对性修复。当前 true EOF 仅证明 External C 已交付该修复；是否接受仍由后续父级复审决定。

## 6. Production 只读、`696a12b0` 与禁止机制

- AT1 claim/写入发生在 production mtime 之后；Repair 前后 production 始终为冻结 SHA `532E6F84...`。资源模板也保持五月份 committed 字节，AT1 没有修改 production、resource、POM 或 caller。
- 因本增量只改 named test 与 AT1 子卡，`696a12b0` 的生产决策、阶段、优先级、探测/fallback 顺序、delay、pending/park/terminal 语义没有被 AT1 改写。这里是“本增量未制造业务差异”的静态证据，不是本 helper 对 TURN-34A production 的重新批准。
- 当前 AT1 增量没有新增 auto retry/replay/resend、turn session、owner、ledger、TTL、lease 或 durable workflow。测试 metadata 使用 `TaskRetryPolicy.none()`；生产中既存的 `localTeamSessionKey` 日志、30 秒 team/urgent gate 和基线 one re-probe 是冻结 source 里的既有业务概念，不是 AT1 新增的 HTTPS-turn session/retry/TTL。

## 7. Delivery-preflight 结论

当前 `35116F19...` 快照中，Stage-1 真实 battle flag、exact one command/canonical fresh UUID/raw PNG，以及首 CAPTURE 七类 terminal/uncertain 保持 `IN_COMBAT`、零 fallback 的 AT1 主合同均有 production-path 测试源码证据；父级 Review #1 点名的两项 Repair #1 也都能在当前字节中直接定位。

但本报告 **不批准** TURN-34AT1：冻结时 AT1 卡的最后状态仍只是 External C `REPAIR #1 TEST-SOURCE DELIVERED`，尚无父级对 `35116F19...` 的 Repair #1 复审 true EOF；也没有本轮 named-test exit 0、compile 或 package 证据。TURN-34A 整体仍有 AT2/AT3+ 测试分片与后续 review/build 门，不能由 AT1 单片冒充完成。

若测试 SHA、mtime 或卡片 true EOF 在此后变化，本文只保留为上述时间点的 delivery-preflight 快照；不得跨字节复用为 review 或批准。

TRUE_EOF PRECHECK_COMPLETE
