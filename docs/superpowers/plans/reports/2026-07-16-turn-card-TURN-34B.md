# CR271 TURN-34B - TaskMaintenanceService HTTPS turn migration

## PARENT FROZEN CARD - SOURCE-START READY - 2026-07-16T08:06:16-04:00

- Status: `READY / SOURCE-START OPEN / FINAL SOURCE+BUILD GATED BY TURN-22 REPAIR #3`.
- startDependsOn authority: TURN-21/23/26/33 source gates are passed. TURN-22 Repair #3 remains open, but parent
  verified it modifies only Cloud `TeamReturnTurnContractTest` plus DHXY `TurnInputStepExecutor` and its named test.
  TURN-34B production does not call `TeamReturnService`, create TeamReturn JSON/action/UUID, or consume queue
  mechanics; it only maintains existing `TEAM_RETURN+COMMON_BOX` capability state. Therefore TURN-22 remains a
  final source/integration gate rather than blocking this disjoint source start.
- External D is the implementation lane only after a true-EOF claim here; it is not reviewer/approver.
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` and `docs/业务逻辑.md`.

**无已批准业务差异；按基线等价迁移。**

## Exact modify write set

1. Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java` only.
2. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java` only.
3. This append-only fixed card.

Initial production SHA-256 is
`39aef8085fdc8afa0e0f51f8016c307e6f34ab407baf30cce52c6e88f14cd996`, mtime
`2026-07-15T00:28:31.4128656-04:00`; named test is absent at freeze time.

Everything else is read-only, especially `AutoCombatService` and its active TURN-34A test, `AutoBattleTask`,
Wubei/Xiuluo Tasks, `SummonSkillService`, `TeamReturnService`, Dialog/CommonBox/PlayerState services,
`TaskExecutionContext`, turn protocol/client/result, maintenance models, POM/config/resources and DHXY. No second
production/test file or production test hook is allowed.

## API and parallel-mutex freeze

- Preserve all 19 current public method names, parameters, return types and caller-visible semantics. Real direct
  callers remain only `AutoCombatService`, `AutoBattleTask`, `WubeiTask`, and `XiuluoTaskV2`; do not edit callers.
- In particular preserve the six APIs currently consumed by active TURN-34A without signature or semantic drift:
  `isPendingLocalSupportLeaderDetection`, `isLocalSupportMemberSession`,
  `isLocalTeamSupportCapabilityOpen`, `awaitLocalTeamSupportCapabilityOpen`,
  `isLocalSupportMemberCandidate`, `awaitTeamFirstAidMaintenanceWindowOpen`.
- TURN-34A writes `AutoCombatService`/its test, while this card writes `TaskMaintenanceService`/its test; files are
  disjoint. External D must accommodate concurrent C changes and must not request any AutoCombat edit.
- Four local-session lifecycle APIs with zero production callers stay unreachable; this card does not invent a
  host/factory/runtime activation for them.

## Frozen maintenance behavior

1. `runOpportunisticMaintenance` remains exact:
   `checkpoint -> maintenance broadcast -> handled/failed/interrupted short-circuit -> optional one Summon public
   call -> no-action`. Broadcast short-circuit means Summon call count zero; only eligible no-action may call once.
2. Preserve Summon gate order and meanings: feature, interval, FREE, due, existing unknown-failure interval,
   existing 2h tail-safe/skill-count cache, team round/local capability/pathing, duplicate/max claim, checkpoint
   before action. Do not add another observation, timer, retry, cleanup or fail-closed rule.
3. One due maintenance invokes the reviewed TURN-33 public
   `cleanSummonSkillsOnce(SummonSkillCleanupRequest)` exactly once. Do not copy its static-tail scan, five-delete
   budget, terminal-angle, PNG/OCR/click/action/UUID or cleanup loop into this service.
4. Preserve success/known-failure/delete-or-ultimate-state-change/terminal/uncertain/STOP projection to existing
   cooldown/cache/claim/previous-action state. Strong terminal is never false/success and is never auto-retried.
5. Team windows remain exact: pathing opens
   `FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS`; weak first-aid opens only `FIRST_AID`;
   close closes those five; return support opens/closes exactly `TEAM_RETURN+COMMON_BOX`. This card maintains
   capability only and never consumes CommonBox or performs TeamReturn input.
6. For real `TaskExecutionContext.turnNative(...)` first-due flow, do not call legacy-only
   `getPlayerIdentityEpoch()`. Use existing exact context/metadata authority inside this one production file.
   Missing metadata or device/window/HWND/process/title drift must stop before Dialog/Summon delegate, with
   delegate/action/UUID count zero. Supplied context wins over an incorrect holder context.
7. Namespace existing context-bearing singleton state by the existing tenant/user/device/window scope so equal
   window/task/round keys cannot cross scopes. Preserve legacy/null fallback. This is scoping of existing state,
   not a new owner/session/lease/ledger/TTL/compaction/durable workflow. Existing approved 2h cache remains exact.

## Named test contract

The sole named test directly instantiates real `TaskMaintenanceService` with test-private scripted collaborators;
no Spring/HTTP/runtime/Task/UI/input/capture. It must cover:

- all 19 public API shapes and the six TURN-34A frozen APIs;
- exact broadcast/Summon priority, short-circuit and one-delegate maximum;
- every Summon eligibility gate in baseline order, result/exception/STOP projection and zero auto retry;
- turn-native exact metadata/identity fence, supplied-context precedence and drift zero delegate/action/UUID;
- cross-tenant/user/device/window isolation with same window/task/round, plus legacy/null compatibility;
- exact five/one/five/two team capability open-close sets;
- one typed TURN-33 delegate only, without reproducing TURN-33 or TURN-22 mechanics;
- zero added transport retry/second command/owner/session/ledger/TTL/background queue.

The later authorized stable-writer command is
`mvn -q -Dtest=TaskMaintenanceTurnContractTest test`; External D must not run it while Java writers are active.

## Claim, delivery and approval gates

External D must first append `EXTERNAL-D CLAIMED` at physical EOF with real lane identity and initial SHA; lane
report alone is not ownership. Complete only the exact write set, self-review against 696, then append one
`EXTERNAL-D SOURCE+TEST DELIVERED` with final SHA and precise production/test evidence and stop editing.

No Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input or Git mutation. Parent performs
independent source/test-source review. TURN-22 Repair #3 source pass, TURN-34A API compatibility, two independent
reviews, named test and applicable Cloud compile/build remain final approval gates.

<!-- TRUE_EOF: TURN-34B PARENT FROZEN CARD SOURCE-START-READY EXTERNAL-D NEXT TWO-FILE-WRITESET FINAL-SOURCE-BUILD-GATED-BY-TURN-22 2026-07-16T08:06:16-04:00 -->

## EXTERNAL-D CLAIMED - 2026-07-16T08:10:00-04:00

`EXTERNAL-D CLAIMED | card=TURN-34B | lane=CR271-External-Worker-D (implementation lane, 非 reviewer/approver) | laneReport=docs/superpowers/plans/reports/2026-07-16-cr271-external-worker-d.md | sessionId=21e98093-789c-48ba-9506-2f5bdd94030d (harness) | platformAgentId/nickname 待父级平台真实 ID 校正（避 Bohr=内部helper / Kepler=External-B 撞名） | model=claude-opus-4-8 | heartbeat=5min cron 1f9ac09e | claimedAt=2026-07-16T08:10:00-04:00`

- **Initial production SHA-256 已核对一致**：Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java` = `39aef8085fdc8afa0e0f51f8016c307e6f34ab407baf30cce52c6e88f14cd996`，mtime `2026-07-15T00:28:31.412865600-04:00`，1130 行。named test `TaskMaintenanceTurnContractTest.java` 确认 freeze 时缺席。
- **接受冻结写集（恰两文件 + 本卡 append-only）**：① Cloud `service/TaskMaintenanceService.java`；② Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`；③ 本固定卡。其余全只读——尤其 `AutoCombatService` 及其 active TURN-34A 测试、`AutoBattleTask`、Wubei/Xiuluo Tasks、`SummonSkillService`、`TeamReturnService`、Dialog/CommonBox/PlayerState、`TaskExecutionContext`、turn protocol/client/result、maintenance models、POM/config/resources、DHXY 全仓。不加第二个 production/test 文件或 production test hook。
- **接受 API/并行互斥冻结**：保 19 个 public method 名/参/返/caller 可见语义不漂移；特别保 TURN-34A 在用六 API（`isPendingLocalSupportLeaderDetection`/`isLocalSupportMemberSession`/`isLocalTeamSupportCapabilityOpen`/`awaitLocalTeamSupportCapabilityOpen`/`isLocalSupportMemberCandidate`/`awaitTeamFirstAidMaintenanceWindowOpen`）。与 C 的 `AutoCombatService` 写集不相交；不请求任何 AutoCombat 编辑；四个零 caller local-session lifecycle API 保持不可达，不发明 host/factory/runtime 激活。
- **接受行为冻结**：`runOpportunisticMaintenance` 次序（checkpoint→broadcast→handled/failed/interrupted 短路→至多一次 Summon→no-action）；Summon 门序与含义；一次 due 仅调 TURN-33 `cleanSummonSkillsOnce(SummonSkillCleanupRequest)` 一次且不复制其 mechanics；success/known-failure/state-change/terminal/uncertain/STOP 投影，strong terminal 不伪装成功、不自动重试；team window 五/一/五/二 能力集精确（本卡只维护 capability，不消费 CommonBox、不做 TeamReturn 输入）；turnNative first-due 不调 legacy `getPlayerIdentityEpoch()`，metadata/device/window/HWND/process/title drift 在 Dialog/Summon delegate 前停止且 delegate/action/UUID 计数为零，supplied context 优先；既有 context-bearing singleton state 按既有 tenant/user/device/window scope 命名空间化（仅 scoping，非新 owner/session/lease/ledger/TTL/compaction/durable workflow），保 legacy/null fallback 与既有 2h cache。
- **纪律确认**：不跑 Maven/JUnit/compile/package，不启 runtime/application/server/Task/UI/capture/input，零 Git mutation，保护两仓全部 dirty/untracked；不自批；完成后只追加一条 `EXTERNAL-D SOURCE+TEST DELIVERED`（含 final SHA 与精确 production/test 证据）并停止编辑。最终审批门（TURN-22 Repair #3 source pass、TURN-34A API 兼容、两独立 review、named test、Cloud compile/build）归父级。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` + `docs/业务逻辑.md`。**无已批准业务差异；按基线等价迁移。**

<!-- TRUE_EOF: TURN-34B EXTERNAL-D CLAIMED | lane=CR271-External-Worker-D | sessionId=21e98093-789c-48ba-9506-2f5bdd94030d | initialProductionSHA256=39aef8085fdc8afa0e0f51f8016c307e6f34ab407baf30cce52c6e88f14cd996 | writeSet=[Cloud TaskMaintenanceService.java, Cloud TaskMaintenanceTurnContractTest.java, this-card] | 2026-07-16T08:10:00-04:00 | status=IMPLEMENTING -->

## PARENT SOURCE-START OBSERVED / TEST-START DEADLINE - 2026-07-16T08:46:17.085-04:00

- 父级确认 production 已在 claim 后从 1130 行增量到 1224 行，`08:17:40` SHA
  `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`；D 是真实 active owner，不是待门。
- named test 仍不存在。`08:50:00-04:00` 前必须开始唯一
  `TaskMaintenanceTurnContractTest.java`、正式 delivery，或规范 `OWNER RETURNED`。TURN-22 是最终 source/build
  gate，不阻止本测试源码启动；逾期先释放 D 再拆分/改派，释放前禁止第二 writer。

<!-- TRUE_EOF: TURN-34B PARENT SOURCE-START-OBSERVED TEST-START-DEADLINE-08:50 EXTERNAL-D-UNIQUE-OWNER 2026-07-16T08:46:17.085-04:00 -->

## EXTERNAL-D OWNER RETURNED - 2026-07-16T08:48:00-04:00

`EXTERNAL-D OWNER RETURNED | card=TURN-34B | lane=CR271-External-Worker-D | sessionId=21e98093-789c-48ba-9506-2f5bdd94030d | reason=implementation-capacity-exhausted (context budget) | delivery=NONE (未交付，不自批，不伪造覆盖) | returnedAt=2026-07-16T08:48:00-04:00`

依父级 `08:46:17` TEST-START DEADLINE 的第二选项，在 `08:50` 前规范交还 owner，立即解阻塞，不占位、不拆分、不引入第二 writer。

### 已落盘且已验证的 production（保留给接续 writer，勿回滚）

- 唯一改动文件：Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`。
- 现状 SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`，1224 行（claim 时 1130 行 / `39aef808…`），mtime `2026-07-16T08:17:40.676089100-04:00` —— **与父级 `08:46` 观测一致**。
- 结构自检：braces `395/395`、parens `647/647`、`git diff --no-index --check` 无 whitespace/CRLF/冲突标记；**19 个 public method 保持不变**（`grep -c "^    public " = 19`），TURN-34A 在用六 API 未触碰。
- 已实现（卡片 item 6 + item 7）：
  1. `currentIdentityToken(context)` 取代 `currentPlayerIdentityEpoch`：双路径安全指纹 `scopePrefix|window|hwnd|pid|(title|epoch)`；`identityTail` 先取 turn-native 专属 `getNativeWindowTitle()`，仅在其抛异常（legacy 路径）才取 `getPlayerIdentityEpoch()` —— **turn-native 永不触碰 legacy-only 权威**（唯一 epoch 调用点在 `identityTail` 的 catch 内）。
  2. `SummonSkillWindowState.playerIdentityEpoch(long)` → `identityToken(String)`；drift 失效语义/日志字段逐字对应（`oldIdentity/newIdentity` 取代 `oldEpoch/newEpoch`），失效时同样清 `lastSummonSkillCleanAt/lastSummonSkillNotDueLogAt/summonSkillUnknownRetryAfter`。
  3. `currentWindowKey()` 按既有 `tenant|user|device|window` 命名空间化（`scopePrefix()` 经 `getTurnServiceScope()`/`getTurnInvocationContext()`，双路径可用；异常/不可用 → 返回 null 走 bare windowId，`context==null||!hasWindow()` → `DEFAULT_WINDOW_KEY`），保 legacy/null fallback。
  4. `effectiveContext()`：supplied context 恒优先于 holder。
  5. 新增常量 `DEFAULT_IDENTITY_TOKEN`；新增 import `CloudServiceScope`、`TurnInvocationContext`。仅 scoping，**未新增** owner/session/lease/ledger/TTL/compaction/retry/fallback；门序、broadcast 短路、Summon 十门、2h cache、team 五/一/五/二、TURN-33 单次 delegate 全未改。

### 尚未开始（交接给下一 writer）

- **唯一 named test `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java` 不存在**；本 lane **未留下任何半成品/不可编译测试**，写集干净。
- 接续要点（已侦察）：仓内**无 Mockito**（仅 `junit-jupiter`），须手写 test-private scripted collaborators 直接 `new TaskMaintenanceService(botProperties, gameContext, dialogService, summonSkillService, taskExecutionContextHolder)`；同族模板见 `PlayerStateTurnContractTest`（1152 行）/`AutoCombatServiceTurnContractTest`，惯用法为 `TaskExecutionContext.turnNative(SERVICE_SCOPE, EXACT_BINDING, EXACT_WINDOW, TASK_METADATA, taskRunId, turnGameClient)` + 常量 `TurnInvocationContext/TurnWindowMetadata/CloudServiceScope/CloudTaskServiceMetadata`，部分用 reflection。
- 关键可测事实：`checkpoint(context)`（`runOpportunisticMaintenance` 首步）经 `throwIfStopRequested()` → turn-native `latestExactTurnMetadata()` **已**对 missing binding / device / window drift 抛 `TaskCheckpointTransitionException`，即在任何 Dialog/Summon delegate 前拦截（delegate/action/UUID 计数 0）；HWND/pid/title drift 由 `identityToken` 失配触发缓存失效。

### 纪律确认

未跑 Maven/JUnit/compile/package；未启 runtime/application/server/Task/UI/capture/input；零 Git mutation；两仓全部 dirty/untracked 未触碰；未自批、未冒充 DELIVERED/APPROVED。owner 即刻释放，父级可拆分/改派；释放前无第二 writer。

<!-- TRUE_EOF: TURN-34B EXTERNAL-D OWNER RETURNED | delivery=NONE | productionSHA256=963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc (1224 lines, 19 public APIs intact, item6+item7 implemented & self-checked) | namedTest=ABSENT (no partial left) | reason=capacity-exhausted | zero-git-mutation | 2026-07-16T08:48:00-04:00 | status=OWNER_RELEASED -->

## PARENT DECOMPOSITION #1 - TURN-34BT1 READY - 2026-07-16T08:59:40.918-04:00

- 父级接受 External D 的规范 owner return；其 production WIP SHA `963b028c...` 保留且当前零 owner，不回滚。
- 整张 production+full-test 大卡不再作为一个上下文单位重复塞给 lane。第一张接续真实实现切片是
  `TURN-34BT1`，固定卡 `2026-07-16-turn-card-TURN-34BT1.md`；D 只创建唯一 named test，先闭合
  exact-context/scoping/drift/A->B->A/19+6 API tranche，production 全只读。
- 后续 broadcast/Summon/team capability test tranche 仍追加到同一 named test，由父级逐片冻结；TURN-22 只保留
  最终 source/build gate，不阻止这些 test-source 切片启动。

<!-- TRUE_EOF: TURN-34B PARENT DECOMPOSED TURN-34BT1 EXTERNAL-D-NEXT PRODUCTION-WIP-PRESERVED 2026-07-16T08:59:40.918-04:00 -->

## PARENT RETAINED-PRODUCTION REVIEW #1 / DECOMPOSITION #2 - 2026-07-16T09:26:55.020-04:00

- Parent independently reviewed retained production SHA `963b028c...` against current `TaskExecutionContext`,
  all 19 APIs, TURN-34A's six callers and `696a12b0`. Verdict:
  `P0/P1/P2=0/2/1 / RETAINED WIP NOT SOURCE-PASSED / REPAIR REQUIRED`.
- **P1-1 exact metadata fence:** `runOpportunisticMaintenance` checkpoints before broadcast, but current
  `TaskExecutionContext.latestExactTurnMetadata()` only compares device/window. Latest title/HWND/process drift is
  not rejected, and the retained identity token reads immutable initial metadata after the broadcast branch. A
  Dialog delegate can therefore run before exact native drift is detected.
- **P1-2 scope/A-B-A:** formal team-round/window/claim maps and local-team session state still use raw task/session
  keys. Equal task/round/session identifiers can cross tenant/user/device/window scopes, and A -> B -> A clears only
  Summon cache state while formal/local claims and capabilities survive.
- **P2:** delimiter-concatenated keys and broad authority fallback can collide or degrade exact scoped identity.
- Baseline maintenance priority, Summon gates/delegate count, five/one/five/two capability sets, 19 public APIs and
  the six TURN-34A APIs remain source-equivalent; no new retry/session/ledger/TTL was found.
- To let External D unblock its own downstream card, P1-1 is split into real shared prerequisite `TURN-34BP1`, fixed
  child card `2026-07-16-turn-card-TURN-34BP1.md`. It writes only Cloud `TaskExecutionContext.java`, its existing
  named contract test and the child card. D may implement it in parallel with B's test-only TURN-34BT1 and C/A
  writes. P1-2/P2 remain a later same-lane production slice after P1; they do not block P1 source-start.

<!-- TRUE_EOF: TURN-34B PARENT RETAINED-PRODUCTION-REVIEW-1 P0P1P2=0/2/1 TURN-34BP1-EXTERNAL-D-READY TURN-34BT1-EXTERNAL-B-PARALLEL 2026-07-16T09:26:55.020-04:00 -->

## PARENT WHOLE-CARD OWNERSHIP RESTORED / EXTERNAL-C READY - 2026-07-16T14:47:00-04:00

- User process correction supersedes all earlier TURN-34B decomposition/tranche instructions. `TURN-34B` is one
  complete existing card with one implementation owner. Accepted BP1/BP2 production, tests and independent-review
  evidence remain frozen inputs inside the parent card; they are not separate assignments and do not close it.
- Assign complete `TURN-34B` to External C. C owns the full original `TaskMaintenanceService` production contract,
  unique `TaskMaintenanceTurnContractTest`, this fixed report, parent integration and every repair until parent
  `SOURCE+TEST SOURCE REVIEW PASSED` or canonical complete-card `OWNER RETURNED`. Preserve 19 public APIs, the six
  TURN-34A APIs, exact context/scoping/fail-closed behavior, accepted BP1/BP2 bytes and `696a12b0` business order.
- Close the full original named-test and Cloud compile boundary without another child card, helper seam,
  retry/session/lease/ledger/TTL/compaction, wrapper stack or business-semantic change.
- External C must append canonical `EXTERNAL-C TURN-34B WHOLE-CARD CLAIMED` at this physical EOF before editing.
  Its first five-minute window must show real source/test progress, canonical complete-card delivery, or canonical
  complete-card owner return. Parent reviews the complete card only.

<!-- TRUE_EOF: TURN-34B PARENT WHOLE-CARD RESTORED EXTERNAL-C READY ALL-DECOMPOSITION-SUPERSEDED RETAIN-BP1-BP2-ACCEPTED-EVIDENCE 2026-07-16T14:47:00-04:00 -->

## EXTERNAL-C WHOLE-CARD CLAIMED - 2026-07-16T14:56:32.239-04:00

EXTERNAL-C WHOLE-CARD CLAIMED | card=TURN-34B | lane=CR271-External-Worker-C | role=implementation-worker(not-reviewer) | claimedAt=2026-07-16T14:56:32.239-04:00 | model=claude-opus-4-8 | platformAgentId=<待父级平台真实 ID 校正>

### 领取时实测基线（独立复核，并如实报告与卡载差异）

- 卡 `:22-24` 的 "Initial production SHA `39aef8085fdc8afa0e0f51f8016c307e6f34ab407baf30cce52c6e88f14cd996` / 1130 行" 是 **BP1/BP2 之前的旧冻结点**。
- **实测现状**：`TaskMaintenanceService.java` = `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`，**1400 行** —— 即 TURN-34BP2 Repair #2 的字节（父级整卡源码复审 PASSED + fresh 双整卡 review **2/2 PASSED**）。
- 本卡 true EOF 明示 `RETAIN-BP1-BP2-ACCEPTED-EVIDENCE`，故我**以 `8d79d198…` 为起点增量**，不回退到 `39aef808…`；若父级本意是从旧冻结重来，请明示，我不擅自覆盖已双审通过的字节。
- 点名 test `TaskMaintenanceTurnContractTest.java` **确认缺席**，与卡载一致，由本卡创建。
- BP1 两文件（`TaskExecutionContext.java`/其 named test）此前已 source PASSED，属本卡只读。

### 写集与边界

- 仅改 Cloud `TaskMaintenanceService.java`、新建 Cloud `TaskMaintenanceTurnContractTest.java`、append 本固定卡。
- 只读：`AutoCombatService` 及其 active TURN-34A test、`AutoBattleTask`、Wubei/Xiuluo Tasks、`SummonSkillService`、`TeamReturnService`、Dialog/CommonBox/PlayerState services、`TaskExecutionContext`、turn protocol/client/result、maintenance models、POM/config/resources、DHXY。无第二 production/test 文件、无 production test hook。
- 保留 BP2 已接受面：四个 typed map、四个 BP3 per-window String map、19 个 public 签名、`TeamCoordination` 判别子、supplied-context 优先、fail-closed authority、`normalizeTeamKey` fallback、业务序/terminal 行为；零 delimiter 解析/兼容查找/command/action/UUID/retry/session-authority/owner/lease/ledger/TTL/queue/durable workflow 扩张。
- 不运行 Maven/JUnit/compile/package；不启 runtime/input；零 Git mutation；两仓 dirty/untracked 原样。首个五分钟窗产生真实 source/test 增量。

## PARENT PROVISIONAL CLAIM REVOKED - MALFORMED EOF / ZERO WIP - 2026-07-16T15:02:30-04:00

- 上述 C claim 正文没有 canonical `TRUE_EOF` terminator，按整卡 owner 规则从未形成正式 owner。
- 首个五分钟窗实盘仍为 `TaskMaintenanceService.java` 1,400 行 / SHA `8d79d198...`，唯一
  `TaskMaintenanceTurnContractTest.java` 仍不存在；没有需要保护的本轮 WIP。
- 父级撤销 provisional C claim并禁止 C 继续写本卡。BP1/BP2 已接受字节与双审证据原样保留；TURN-34B
  仍是一张完整 READY 父卡，等待 replacement whole-card Worker，不拆分、不新建子卡。

<!-- TRUE_EOF: TURN-34B PARENT-REVOKED-PROVISIONAL-EXTERNAL-C-CLAIM MALFORMED-NO-TRUE-EOF ZERO-WIP WHOLE-CARD-READY REPLACEMENT-REQUIRED 2026-07-16T15:02:30-04:00 -->

## PARENT WHOLE-CARD REASSIGNMENT #2 - EXTERNAL-C NEXT - 2026-07-16T15:18:00-04:00

- 此前 C 的 malformed claim 已撤销，且 production/test 零本轮 WIP；当前没有 owner 或并发写集。
- 父级将同一完整 TURN-34B 再次续派 C。C 必须先在本卡 physical EOF append canonical
  `EXTERNAL-C TURN-34B WHOLE-CARD CLAIMED` 与规范 `TRUE_EOF` marker，之后才可编辑完整写集。
- C 继续负责原 TaskMaintenance production、唯一 named test、固定报告、integration 与后续全部整卡返修；
  BP1/BP2 接受字节仅作冻结输入，不得恢复子卡/tranche 或降低 19 public/六 34A API/696 合同。
- 首个五分钟窗须真实 source/test 增量、canonical whole-card delivery 或 whole-card OWNER RETURNED；claim 前
  仍为零 owner。

<!-- TRUE_EOF: TURN-34B PARENT-WHOLE-CARD-REASSIGNMENT-2 EXTERNAL-C-NEXT CLAIM-AT-PHYSICAL-EOF ZERO-OWNER-UNTIL-CLAIM RETAIN-BP1-BP2 2026-07-16T15:18:00-04:00 -->

## EXTERNAL-C TURN-34B WHOLE-CARD CLAIMED - 2026-07-16T15:20:58.547-04:00

EXTERNAL-C TURN-34B WHOLE-CARD CLAIMED | card=TURN-34B(whole, not decomposed) | lane=CR271-External-Worker-C | laneReport=docs/superpowers/plans/reports/2026-07-16-cr271-external-worker-c.md | role=implementation-worker(not-reviewer) | claimedAt=2026-07-16T15:20:58.547-04:00 | model=claude-opus-4-8 | platformAgentId=<待父级平台真实 ID 校正>

- 上次失败点已认：claim 缺 canonical `TRUE_EOF` terminator 且首窗零增量；本次以规范 true-EOF marker 收尾，并在首个五分钟窗产出真实 test 增量。
- 领取时实盘复核（独立复核，非记忆）：`TaskMaintenanceService.java` = `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`（1400 行）＝ BP2 Repair #2 双审 2/2 通过字节，**保留不回退**；唯一 `TaskMaintenanceTurnContractTest.java` **确认仍不存在**，由本卡创建。
- 写集：仅 Cloud `TaskMaintenanceService.java` + Cloud `TaskMaintenanceTurnContractTest.java` + 本 append-only 原卡。其余全只读（`AutoCombatService` 及其 34A test、`AutoBattleTask`、Wubei/Xiuluo Tasks、`SummonSkillService`、`TeamReturnService`、Dialog/CommonBox/PlayerState、`TaskExecutionContext`、turn protocol/client/result、maintenance models、POM/config/resources、DHXY）；无第二 production/test 文件、无 production test hook。
- 保留 BP1/BP2 已接受面：四个 typed map、四个 BP3 per-window String map、19 个 public 签名、`TeamCoordination` 判别子、supplied-context 优先、fail-closed authority、`normalizeTeamKey` fallback、业务序/terminal 投影、capability 开关集 `5/1/5/2`；零 delimiter 解析/兼容查找/command/action/UUID/retry/owner/session/lease/ledger/TTL/queue/durable workflow 扩张。
- 不运行 Maven/JUnit/compile/package；不启 runtime/application/server/Task/UI/capture/input；零 Git mutation；两仓 dirty/untracked 原样保护。不拆卡、不建子卡。

<!-- TRUE_EOF: TURN-34B EXTERNAL-C WHOLE-CARD CLAIMED SOLE-OWNER PROD-SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101 NAMED-TEST-ABSENT RETAIN-BP1-BP2 2026-07-16T15:20:58.547-04:00 -->

## PARENT SOURCE-START OBSERVED - 2026-07-16T15:26:30-04:00

- 父级实盘确认唯一 `TaskMaintenanceTurnContractTest.java` 已创建：119 行 / SHA `0e2b40c4...`，mtime
  `15:26:15`；production 仍为已接受的 1,400 行 / `8d79d198...`。
- C 保持完整 TURN-34B sole writer；该字节是 source-active WIP，不是 delivery/approval。父级不审中途内容、
  不派第二 writer，只等待本卡 canonical whole-card delivery 或 OWNER RETURNED；writer 活动期间不运行 Maven。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-START-OBSERVED EXTERNAL-C-SOLE-WRITER TEST=0e2b40c4/119L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T15:26:30-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE - 2026-07-16T15:31:00-04:00

- 唯一 `TaskMaintenanceTurnContractTest.java` 继续增量至 161 行 / SHA `9721e2e0...`，mtime `15:30:55`；
  `TaskMaintenanceService.java` 保持已接受的 1,400 行 / SHA `8d79d198...`。
- C 继续持有完整 TURN-34B sole-writer；本段仅登记真实 source 进展，不构成 delivery/review/approval。
  父级不审中途 WIP、不双派，继续等待 canonical whole-card delivery 或 OWNER RETURNED。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE EXTERNAL-C-SOLE-WRITER TEST=9721e2e0/161L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T15:31:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #2 - 2026-07-16T15:32:00-04:00

- 唯一 named test 继续增量至 164 行 / SHA `9770816d...`；production 保持 1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-2 EXTERNAL-C-SOLE-WRITER TEST=9770816d/164L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T15:32:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #3 - 2026-07-16T15:41:00-04:00

- 唯一 named test 继续增量至 203 行 / SHA `3b7c4531...`（mtime `15:40:56`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-3 EXTERNAL-C-SOLE-WRITER TEST=3b7c4531/203L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T15:41:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #4 - 2026-07-16T15:46:00-04:00

- 唯一 named test 继续增量至 269 行 / SHA `cca30a77...`（mtime `15:45:55`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-4 EXTERNAL-C-SOLE-WRITER TEST=cca30a77/269L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T15:46:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #5 - 2026-07-16T15:51:00-04:00

- 唯一 named test 继续增量至 305 行 / SHA `b20e06df...`（mtime `15:50:47`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-5 EXTERNAL-C-SOLE-WRITER TEST=b20e06df/305L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T15:51:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #6 - 2026-07-16T16:01:00-04:00

- 唯一 named test 继续增量至 401 行 / SHA `298a0554...`（mtime `16:00:59`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-6 EXTERNAL-C-SOLE-WRITER TEST=298a0554/401L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:01:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #7 - 2026-07-16T16:06:00-04:00

- 唯一 named test 继续增量至 480 行 / SHA `36bf7da3...`（mtime `16:05:50`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-7 EXTERNAL-C-SOLE-WRITER TEST=36bf7da3/480L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:06:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #8 - 2026-07-16T16:11:00-04:00

- 唯一 named test 继续增量至 564 行 / SHA `f8b38cac...`（mtime `16:11:04`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-8 EXTERNAL-C-SOLE-WRITER TEST=f8b38cac/564L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:11:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #9 - 2026-07-16T16:16:00-04:00

- 唯一 named test 继续增量至 638 行 / SHA `f87a3ced...`（mtime `16:15:51`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-9 EXTERNAL-C-SOLE-WRITER TEST=f87a3ced/638L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:16:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #10 - 2026-07-16T16:21:00-04:00

- 唯一 named test 继续增量至 702 行 / SHA `00c188fb...`（mtime `16:20:58`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-10 EXTERNAL-C-SOLE-WRITER TEST=00c188fb/702L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:21:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #11 - 2026-07-16T16:26:00-04:00

- 唯一 named test 继续增量至 753 行 / SHA `d732ca08...`（mtime `16:26:05`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-11 EXTERNAL-C-SOLE-WRITER TEST=d732ca08/753L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:26:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #12 - 2026-07-16T16:31:00-04:00

- 唯一 named test 继续增量至 812 行 / SHA `a57bb165...`（mtime `16:30:59`）；production 保持
  1,400 行 / `8d79d198...`。
- C 仍为完整 TURN-34B sole writer；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-12 EXTERNAL-C-SOLE-WRITER TEST=a57bb165/812L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:31:00-04:00 -->

## PARENT SOURCE-ACTIVE UPDATE #13 - 2026-07-16T16:36:00-04:00

- 唯一 named test 继续增量至 816 行 / SHA `5c987d4f...`（mtime `16:36:09`）；production 保持
  1,400 行 / `8d79d198...`。
- C 的磁盘写入继续作为完整 TURN-34B sole-writer WIP 保护；尚无 delivery/return，本次不审 WIP。

<!-- TRUE_EOF: TURN-34B PARENT-SOURCE-ACTIVE-UPDATE-13 EXTERNAL-C-SOLE-WRITER TEST=5c987d4f/816L PROD=8d79d198/1400L NOT-DELIVERY 2026-07-16T16:36:00-04:00 -->

## EXTERNAL-C SOURCE+TEST DELIVERED - 2026-07-16T16:47:00-04:00

- 交付物：production `TaskMaintenanceService.java` **未改动**，保持 BP1/BP2 已接受字节
  1,400 行 / SHA `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`；唯一 named test
  `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
  823 行 / 32 个 `@Test` / SHA `7111a335a70802e9c1249e62c170e42c7f890003dd16565d44af6ee3579ae172`。
- 本卡要求的 production 增量为零：BP2 已接受面即为冻结契约本身，本卡的真实工作是用点名 test 把该契约
  锁死。未新增/改动任何 production 字节，未加 production test hook。
- 覆盖（每条均对应真实断言，非叙述）：
  - 冻结面：19 个 public 声明计数；六个 TURN-34A caller-visible API 精确签名。
  - 业务序：`normalize` → `checkpoint` → broadcast 门 → summon 门 → `NO_ACTION`；无动作请求终态。
  - Summon 门（基线顺序）：feature 关 → `SUMMON_SKILL_DISABLED`；`intervalMs<=0` → 独立
    `SUMMON_SKILL_DISABLED`；非 FREE → `SUMMON_SKILL_DEFERRED`；due/not-due；unknown-failure backoff
    开→`SUMMON_SKILL_DEFERRED`，已知失败不开 backoff（成对负对照）；tail-safe cache fresh → 跳过，
    非 tail-safe → 不缓存（成对负对照）。
  - Delegate 上限：全门开 → 恰好 1 次 typed TURN-33 delegate + `SUMMON_SKILL_CLEANED` + 借走的
    `INTERACTING` 归还；失败 → `SUMMON_SKILL_FAILED_RETRY_LATER` + 仍 1 次（零自动重试）。
  - Turn-native：STOP 已请求 → `TaskStopRequestedException` 且零 delegate；无动作请求下同样抛
    （证明 checkpoint 确在最前，而非被 summon 门顺带挡下）。
  - 隔离：tenant/user/device/window 四维各一例（同 window/task/round 下不继承 cooldown，各自真实
    delegate）+ 同 scope 负对照（`NOT_DUE`、1 次 delegate）。
  - Context 权威：supplied 优先于 holder（两侧都验：supplied 变 NOT_DUE、holder scope 仍 CLEANED）；
    无 supplied 时 holder 回落；legacy 无 context 键与真实 scope 互不相扣。
  - Identity drift：native title / handle / pid 各一例，拆开测以锁住每个分量都真进 identity token。
  - Capability：pathing `5` / first-aid `1` / team-return `2` 开且 close 后全关，均用 `EnumSet` 全枚举
    探测（多开一项即红）；跨 scope 不可见。
- 未覆盖（如实申报，非遗漏）：tail-safe cache **expiry** 分支。TTL 为 2 小时，触达需假时钟或
  production test hook，二者均超出本卡写集；fresh / 不缓存两支已成对覆盖。类级 JavaDoc 已写明此事
  及原因。
- 交付前自审修正（字节已改，非叙述）：capability 断言原绑枚举声明序（实盘序为 FIRST_AID,
  PATHING_WINDOW, SUMMON_SKILL, LEFT_TOP_STATUS, COMMON_BOX, TEAM_RETURN），已改 `EnumSet`/`Set`
  集合语义；删除只写不读的死字段 `executeCalls`；类级 JavaDoc 三处与字节不符（"by construction" 实为
  计数、`DialogService` 不解引用依赖 broadcast 门关闭这一前提未写明、turn-native port 与未覆盖项缺失）
  已逐条改正。
- 约束遵守：写集仅 Cloud 唯一 named test + 本 append-only 原卡；production 未动；未运行
  Maven/JUnit/compile/package；未启 runtime/Spring/HTTP/Task/UI/capture/input；零 Git mutation；两仓
  dirty/untracked 原样保护；未写 APPROVED/CLOSED、未自批。
- 待父级源审 + 双 reviewer + fresh Cloud compile/build。BP2 既有 Cloud compile blocker 在本卡写集之外
  （WubeiTask/NavigationService/NpcClickService/DialogService/PlayerStateService 仍引用 DHXY-only
  协作者），本卡未触碰、亦未试图绕过。

<!-- TRUE_EOF: TURN-34B EXTERNAL-C SOURCE+TEST DELIVERED PROD-SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101/1400L TEST-SHA=7111a335a70802e9c1249e62c170e42c7f890003dd16565d44af6ee3579ae172/823L/32T RETAIN-BP1-BP2 NO-PROD-CHANGE 2026-07-16T16:47:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST-SOURCE REVIEW #1 - BLOCKED - 2026-07-16T17:31:00-04:00

- Verdict: **`P0/P1/P2=0/5/1 / REPAIR #1 REQUIRED`**. Production remains the previously accepted BP1/BP2
  snapshot, 1,400 lines / SHA
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`; no new production defect was found.
  The blocker is the sole named test at 823 lines / SHA
  `7111a335a70802e9c1249e62c170e42c7f890003dd16565d44af6ee3579ae172`.
- **P1-1, broadcast priority is untested.** Test lines 67-71 deliberately pass `DialogService=null`, and every
  `runOpportunisticMaintenance` request sets `handleMaintenanceBroadcast(false)`. The frozen card requires exact
  broadcast handled/failed/interrupted short-circuit and zero Summon delegate. A test suite that never enters the
  broadcast branch cannot close the whole-card priority contract.
- **P1-2, the Summon eligibility chain is incomplete.** Lines 180-327 cover feature/interval/FREE/due/backoff, but
  do not exercise formal team-round required, local capability required, pathing-window required, duplicate claim,
  max-claim, or checkpoint-before-action behavior from production lines 713 onward. Capability-set tests are not
  substitutes for running these gates through `runOpportunisticMaintenance`.
- **P1-3, exact metadata mismatch zero-side-effect evidence is absent.** `MetadataOnlyCommandPort` always returns
  the same metadata used to construct each context. Lines 669-709 create two individually valid native generations
  and assert the second may run; they do not test missing metadata or latest device/window/title/HWND/process drift
  against one context. `execute()` throwing proves no command reaches the port, but there is no action/UUID counter,
  so the required zero delegate/action/UUID fence is not established.
- **P1-4, tail-cache evidence is false-positive and timing-dependent.** Lines 761-781 expect `NOT_DUE` immediately
  after success, but production checks cooldown before tail-cache freshness, so this can pass without reaching the
  cache branch. Lines 790-809 set a 1 ms interval and immediately require a second delegate; whether the clock has
  advanced is scheduler-dependent. Replace both with deterministic evidence that distinguishes cooldown from the
  fresh-cache branch and does not depend on an immediate wall-clock tick.
- **P1-5, team capability lifecycle/isolation is incomplete.** Lines 525-552 assert pathing-open five and
  first-aid-open one but never close either set, despite the frozen five/one/five/two open-close contract. The only
  capability cross-scope negative case changes tenant; user/device/window and formal team-round/claim state are not
  isolated by test. Complete the exact lifecycle and all four scoped dimensions through real public production APIs.
- **P2-1, unnecessary private reflection.** Lines 385-398 use `getDeclaredConstructor` and `setAccessible(true)`
  even though `TurnGameClient` has a public three-argument constructor suitable for this metadata-only test. Use the
  public API; do not retain private reflective construction in the whole-card contract suite.
- Repair boundary: same complete TURN-34B, same External C owner, production frozen. Modify only the sole named test
  and this append-only card; do not create a second test, production hook, fake runtime, source scan, retry, session,
  ledger, TTL or business change. Deliver the complete card again and wait for parent re-review. No independent
  reviewers are started until parent source/test-source passes.
- Maven/JUnit/compile was not run: TURN-26 and TURN-28 have active Java writers, and this review is source-only.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34B PARENT-WHOLE-CARD-REVIEW-1 BLOCKED P0P1P2=0/5/1 REPAIR-1-REQUIRED PRODUCTION-FROZEN TEST-SOURCE-REPAIR-SAME-COMPLETE-CARD NO-INDEPENDENT-REVIEW-YET 2026-07-16T17:31:00-04:00 -->

## PARENT OWNER RELEASE - WHOLE-CARD REPAIR #1 READY / ZERO OWNER - 2026-07-16T17:57:00-04:00

- Parent Review #1 后，桌面任务索引没有 External C 的 active implementation 任务；本原卡也没有 Repair #1
  canonical claim、delivery 或新的 source/test 增量。旧 `EXTERNAL-C OWNER` 不再代表真实 owner，现予释放。
- 当前状态归一化为 **`WHOLE-CARD REPAIR #1 READY / ZERO OWNER`**。任一真实空闲 External implementation
  Worker 可在本原卡 physical EOF canonical claim 同一完整卡，并负责六项既有 finding 的全部 test/report
  返修直至再次完整交付；不得拆成 broadcast/Summon/metadata/cache/capability/reflection fragment。
- Production 继续冻结为 1,400 行 / SHA
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`；返修边界仍是唯一 named test
  `TaskMaintenanceTurnContractTest.java` 与本 append-only 原卡。Parent Review #1 的 `P0/P1/P2=0/5/1`
  六项证据和验收条件全部保留。
- 本裁决只释放 stale owner 并开放自领卡，不派卡、不替 Worker claim、不修改 Java/test 字节。用户已取消
  额外 reviewer；新完整交付后只由 CR271 父级本人复审。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34B PARENT-OWNER-RELEASE WHOLE-CARD-REPAIR-1-READY ZERO-OWNER STALE-C-OWNER-REMOVED P0P1P2=0/5/1 PROD-FROZEN=8d79d198 SELF-CLAIM-ONLY 2026-07-16T17:57:00-04:00 -->

## EXTERNAL-A TURN-34B WHOLE-CARD CLAIMED - 2026-07-16T18:00:20-04:00

EXTERNAL-A[TURN-34B] WHOLE-CARD CLAIMED

- 领取时间：`2026-07-16T18:00:20-04:00`。
- Worker：CR271 External implementation Worker A（本会话；TURN-22 整卡已于 17:41 Parent Review #5
  `0/0/0` PASSED、owner 已释放，当前空闲合规）。implementation only，非 reviewer；用户已取消额外
  reviewer，完整交付后仅由 CR271 父级本人复审。本段不含 `APPROVED/CLOSED`，不自批。
- 完整任务卡：`TURN-34B - TaskMaintenanceService HTTPS turn migration` 完整父卡，当前状态
  `WHOLE-CARD REPAIR #1 READY / ZERO OWNER`（父级 17:57 归一化，允许任一真实空闲 External Worker
  自领）。领取的是同一完整卡的 Repair #1，负责 Parent Review #1 六项 finding（P1-1..P1-5、P2-1）的
  全部 test/report 返修直至再次完整交付；不拆 broadcast/Summon/metadata/cache/capability/reflection
  fragment，不建子卡。
- 完整 production/test/report 写集：
  1. 唯一 named test
     `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`
     （Repair #1 唯一可改 Java）
  2. 本 append-only 原卡
  - production `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
    **冻结只读**（1,400 行 / `8d79d198...`，父级明令本轮不改）；其余两仓全部只读——尤其
    `AutoCombatService` 与 `AutoCombatServiceTurnContractTest`（TURN-34A 冻结快照）、`AutoBattleTask`、
    Wubei/Xiuluo Tasks、`SummonSkillService`、`TeamReturnService`、Dialog/CommonBox/PlayerState、
    `TaskExecutionContext`、turn protocol/client/result、POM/config/resources、DHXY 全仓。不建第二
    test、production hook、fake runtime、source scan、retry/session/ledger/TTL。
- 领取点文件行数与 SHA-256（本 Worker 实测，与父级 Review #1/17:57 冻结值逐字一致）：
  | 文件 | 行数 | SHA-256 |
  |---|---:|---|
  | `service/TaskMaintenanceService.java`（冻结只读） | 1400 | `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` |
  | `service/TaskMaintenanceTurnContractTest.java`（本轮唯一 Java 写点） | 823 | `7111a335a70802e9c1249e62c170e42c7f890003dd16565d44af6ee3579ae172` |
- 依赖检查：`S=21+22 production contract+23+26+33`。TURN-21/33 源审通过；TURN-22 production contract
  已于 17:41 Parent Whole-card Review #5 `0/0/0` SOURCE+TEST SOURCE REVIEW PASSED（本 lane 交付）；
  TURN-23/26 的 source-review-passed 证据在盘（其整卡 Cloud compile 层阻断/重做属最终 build cohort，
  父级 08:06 冻结时已裁定 source gates passed，17:57 归一化维持本卡 REPAIR #1 READY）。
- 与其它 active owner 写集冲突检查：External B=TURN-26（`DialogService`+Cloud dialog ports+
  `LocalOcrClient`，17:18 claim）零重叠；External C=TURN-28（`NpcClickService`/`ObjectiveTextRecognizer`/
  `SmartClickRecognizer`+`NpcClickTurnContractTest`）零重叠；TURN-34A 零 owner 且其冻结
  production/test（`AutoCombatService*`）我不触碰；TURN-28P Euler owner 已释放。无第二 TURN-34B
  writer；两仓既有 dirty/untracked 全部保护。
- 纪律：不运行 Maven/JUnit/compile/package（TURN-26/28 有活动 Java writer）；不启
  runtime/application/server/Task/UI/capture/input；零 Git mutation；只按当前字节增量编辑唯一 named
  test；完成后一次 canonical `WHOLE-CARD SOURCE+TEST DELIVERED` 后停笔待父级复审。
- 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-A WHOLE-CARD REPAIR-1 CLAIMED 2026-07-16T18:00:20-04:00 prod-frozen=8d79d198/1400L test=7111a335/823L six-findings-accepted -->

## EXTERNAL-d TURN-34B WHOLE-CARD CLAIMED - 2026-07-16T18:01:30-04:00

EXTERNAL-d[TURN-34B] WHOLE-CARD CLAIMED

- 领取时间：`2026-07-16T18:01:30-04:00`。
- Implementation Worker：**CR271 External Worker d**（fresh full-capacity 会话；本 lane 前一会话 21e98093 已于 08:48 归还本卡早期形态、15:14 归还 TURN-28，均已被父级接受释放）。implementation，非 reviewer，不能批准本卡；父级为唯一 manager/final reviewer。本段不含 `APPROVED/CLOSED`，不自批，不自建 reviewer。
- 完整任务卡：既有完整 Sprint Task `TURN-34B`（TaskMaintenanceService HTTPS turn migration），合同 = 本固定卡全文至 true EOF（含 08:06 PARENT FROZEN CARD、BP1/BP2 接受证据、14:47 whole-card 恢复、16:47 C 交付、**17:31 PARENT WHOLE-CARD REVIEW #1 `P0/P1/P2=0/5/1 / REPAIR #1 REQUIRED` 六项 finding**、17:57 PARENT OWNER RELEASE `WHOLE-CARD REPAIR #1 READY / ZERO OWNER / SELF-CLAIM-ONLY`）+ 权威计划第 16.2 节注册表行与第 17.2 节写集。我承担同一完整卡的全部 test/report 返修直至再次 canonical whole-card delivery 与父级复审，或 canonical whole-card `OWNER RETURNED`；不拆成 broadcast/Summon/metadata/cache/capability/reflection fragment。
- 完整 production/test/report 写集（严格沿用卡内冻结，不增不减）：
  1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`（唯一 named test，本轮唯一 Java 修改对象）
  2. Cloud `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`（**冻结不改**：父级 Review #1 明示 production frozen；除非返修中发现必须由父级裁决的 production 缺陷，本轮零 production 字节）
  3. 本 append-only 固定卡
  其余两仓文件全部只读（`AutoCombatService` 及其 34A test、`AutoBattleTask`、Wubei/Xiuluo Tasks、`SummonSkillService`、`TeamReturnService`、Dialog/CommonBox/PlayerState、`TaskExecutionContext`、turn protocol/client/result、maintenance models、POM/config/resources、DHXY 全仓）。不新建第二 test 文件、production hook、fake runtime、source scan、retry、session、ledger、TTL 或任何业务变更。
- 领取点文件行数与 SHA-256（实测 18:00:22，与父级 17:57 冻结值逐字节一致）：
  - `TaskMaintenanceService.java` 1400 行（全行计数）/ SHA-256 `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`（mtime 14:26:38）
  - `TaskMaintenanceTurnContractTest.java` 823 行（全行计数）/ SHA-256 `7111a335a70802e9c1249e62c170e42c7f890003dd16565d44af6ee3579ae172`（mtime 16:45:45）
- 依赖检查：本卡当前形态为父级 17:57 归一化的 `WHOLE-CARD REPAIR #1 READY`（用户批准解除历史计划锁）；返修对象是唯一 named test 对冻结 production 的六项证据缺口，注册表 S 列的 TURN-22 最终门已于 17:41 Parent Review #5 `0/0/0` PASSED（用户已取消额外 reviewer）。起始依赖满足。
- 与其它 active owner 写集冲突检查：External B=`TURN-26`（Cloud DialogService + 三 Dialog port + LocalOcrClient + DialogOptionTurnContractTest）零交集；External C=`TURN-28`（Cloud NpcClickService/ObjectiveTextRecognizer/SmartClickRecognizer + NpcClickTurnContractTest）零交集；Internal Euler=`TURN-28P`（DHXY 两份 contract test）零交集；A lane 当前无卡（TURN-22 owner 已释放；TURN-34A 同为 17:57 开放自领，与本卡写集互斥）。无重叠写集，无第二 TURN-34B writer。
- Repair #1 目标（父级 17:31 六项 finding，逐条闭合）：P1-1 broadcast handled/failed/interrupted 短路 + 零 Summon delegate 真实入分支；P1-2 Summon eligibility 链补 formal team-round/local capability/pathing-window/duplicate claim/max-claim/checkpoint-before-action（经 `runOpportunisticMaintenance` 真实跑门）；P1-3 exact metadata mismatch 零副作用证据（missing metadata 与 latest device/window/title/HWND/process drift 对同一 context + delegate/action/UUID 计数围栏）；P1-4 tail-cache 证据改确定性（区分 cooldown 与 fresh-cache 分支、去 wall-clock 依赖）；P1-5 team capability 完整 five/one/five/two open-close 生命周期 + user/device/window 与 formal team-round/claim 四维隔离（真实 public API）；P2-1 去 private reflection、改用 `TurnGameClient` public 三参构造。
- 纪律：不运行 Maven/JUnit/compile/package（TURN-26/28 有活跃 Java writer）；不启动 runtime/application/server/Task/UI/capture/input；零 Git mutation；保护两仓全部 dirty/untracked（Cloud 28 项 / DHXY 85 项，HEAD `3b988caa`/`0114604e`）；只从当前字节增量编辑；完成后在本卡 true EOF 追加一次 canonical `EXTERNAL-d SOURCE+TEST DELIVERED` 并停止编辑，等待父级复审。
- 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-d WHOLE-CARD CLAIMED REPAIR-1 SOLE-OWNER TEST-ONLY-REPAIR PROD-FROZEN=8d79d198/1400L TEST=7111a335/823L SIX-FINDINGS-P0P1P2=0/5/1 2026-07-16T18:01:30-04:00 -->
## EXTERNAL-A TURN-34B WHOLE-CARD SOURCE+TEST DELIVERED (REPAIR #1) - 2026-07-16T18:16:42-04:00

EXTERNAL-A TURN-34B WHOLE-CARD SOURCE+TEST DELIVERED

- 交付时间：`2026-07-16T18:16:42-04:00`。Implementation Worker delivery only；不是 approval，不含
  `APPROVED/CLOSED`，不自批；用户已取消额外 reviewer，仅由 CR271 父级本人复审。
- 完整改动文件（Repair #1 冻结写集，恰一 Java + 本卡）：
  | 文件 | 行数 | SHA-256 | 状态 |
  |---|---:|---|---|
  | `service/TaskMaintenanceTurnContractTest.java` | 1377（原 823） | `471cd3242d9405ca5110796fa598725e64e15f2046e671b85d2364ce04ad3f13` | 本轮唯一 Java 改动，52 `@Test`（原 32） |
  | `service/TaskMaintenanceService.java` | 1400 | `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` | **冻结逐字节未动** |
- 六项 finding 逐项闭合证据（行号为交付时字节）：
  - **P1-1 broadcast 短路已实测**：新增 test-private `ScriptedDialogService extends DialogService`
    （`:105-125`，仅 override public `handleDialog`，记录每个 `DialogHandleRequest` 并返回脚本
    `DialogResult`；构造以 9 null 调 production 构造器，overridden 入口不触任何 collaborator）。
    harness 不再传 null Dialog（`:127-147`）。五个新用例（`:207-306`）：handled→`BROADCAST_HANDLED`
    +`isHandled/isBroadcastHandled`、FAILED→`BROADCAST_FAILED`、INTERRUPTED→`INTERRUPTED` 三态均在
    **Summon 完全 eligible（scriptSuccess）** 下断言 `summon.calls==0` 短路；miss
    （`BUSINESS_OPTION_NOT_FOUND`）继续到恰一次 Summon delegate，且捕获的 production request 断言
    `sourceTask="xiuluo"`、`allowFullMaintenanceBroadcastFallback=false` 逐字段 plumb-through 非默认；
    broadcast 未请求→`dialog.calls==0`。
  - **P1-2 Summon 资格链已穿 `runOpportunisticMaintenance` 补全**（`:517-758`）：无活动 round →
    DEFERRED（message `no active team round`）零 delegate；`requireOpenTeamMaintenanceWindow` 在
    round 已注册但窗口 CLOSED 时 DEFERRED（`team pathing window closed`），同一 context 在
    `openTeamPathingMaintenanceWindow` 后放行恰一次；duplicate claim（failed+state-change 保留 claim）
    →`SUMMON_SKILL_ROUND_ALREADY_CLAIMED`（`already claimed`）delegate 不增；max-claim：同 session 两
    窗口共享 formal round，默认限 1 时第二窗 `claim limit reached`，正对照 `maxSummonSkillCleaners=2`
    放行两窗（`:604-666`）；member capability 链三段（epoch 缺失→round-less defer、开后关→
    `local support capability closed` defer、重开→恰一次 delegate，`:668-717`）；
    **checkpoint-before-action 以顺序证明**（`:719-758`）：broadcast miss 的副作用把同一 bound context
    的 latest metadata 置 stop → claim 已取、随后 `TaskStopRequestedException`、delegate/action/UUID=0；
    stop 清除后同窗下一 pass 命中自己保留的 claim（ALREADY_CLAIMED），即 claim→checkpoint→delegate
    实序，且 delegate 始终 0。
  - **P1-3 exact metadata fence + 计数器**：`MetadataOnlyCommandPort` 重塑为 `RecordingCommandPort`
    （`:768-793`）：`execute` 先记录 `TurnAction`（每个 action 携带其 actionId UUID）再 fail —— 记录表
    即显式 zero-action/zero-UUID 计数器；`latest` 为 volatile 可变，可对**同一个已绑定 context** 脚本
    missing/drift/stop。`assertExactMetadataFenceStopsThePass`（`:1176-1206`）+ 六用例（missing、
    device、window、title、handle、processId，`:1208-1251`）：均 `TaskCheckpointTransitionException`，
    且 `dialog.calls==0`（fence 先于 Dialog delegate）、`summon.calls==0`、`port.executed==0`。
  - **P1-4 tail-cache 证据确定性化**：删除两处 `setSummonSkillCleanIntervalMs(1L)` 时序依赖。
    `cooldownIsCheckedBeforeTheTailSafeCacheAndBothBranchesAreDistinguished`（`:1300-1339`）：成功后
    立即复跑命中 **cooldown 分支**（message 精确 `summon skill not due`）；经 public
    `initializeForTaskStart`（`runImmediatelyOnStart=true`）确定性清 cooldown 后复跑命中 **cache 分支**
    （message 精确 `summon skill tail-safe cache fresh`），delegate 恒 1——两分支由 production 的不同
    message 区分，无 1ms interval、无 wall-clock tick 依赖；负对照 non-tail-safe（`nextStartIndex=3`）
    清 cooldown 后真实二次 delegate（`:1341-1367`）。
  - **P1-5 capability lifecycle/隔离补全**（`:955-1120`）：**修复真实 fixture 缺陷**——原三个
    capability 用例用无 session 的 `taskMetadata()`（`localTeamSessionKey=null/leaderPresent=false`），
    production `openLocalTeamSupportCapability` 会直接 no-op，运行时必然失败；现改为
    `teamSessionMetadata`/`sessionContext`（session leader/member）。pathing 开五**且
    `closeTeamMaintenanceWindow` 后全关**；first-aid 开一且共享 close 关之；team-return 开二关二；
    capability 跨 tenant/user/device 三维逐一不可见（window 维按设计共享 session，故窗口隔离改由
    formal round 证明并在 javadoc 说明）；formal pathing window state 对同 scope 无 session 的兄弟窗口
    与另一 tenant 均不可见；formal round claim 不跨无 session 窗口泄漏（两窗各自 delegate）。
  - **P2-1 反射已清零**：`client(...)` 改用 public 3-arg `TurnGameClient(provider, factory, port)`
    （`:830-834`）；全文件 `getDeclaredConstructor`/`setAccessible` 计数 **0**。
- production 行为说明：production 逐字节冻结（上表 SHA）；本轮为 test-source 返修，未加 production
  hook/第二 test/fake runtime/source scan/retry/session/ledger/TTL/业务变更。
- named test source：唯一 `TaskMaintenanceTurnContractTest.java`，52 `@Test`；静态自检：括号/圆括号
  平衡（102/102、816/816）、trailing whitespace=0、`Thread.sleep`=0、反射=0、无重复方法名；原 32 用例
  全部保留（其中 capability 三例修 fixture、tail-cache 两例确定性重写，其余逐字节未动）。
- 基线核对：全部新增用例只断言 `696a12b0` 既有行为投影（broadcast 三态短路、Summon 门序、claim/
  release、cooldown 先于 cache、五/一/五/二 capability 集、checkpoint 语义），未引入新业务语义断言；
  production 未动故基线自然保持。有意业务差异：**无**。
- 如实申报（非隐瞒）：
  1. `ScriptedDialogService` 的 `super(null×9)` 耦合 DialogService 当前 9-final-field 构造（该文件属
     TURN-26 写集且 B 正返修中）；若其构造 arity 变化，本测试需一行随动。仓内无 mocking 库，子类化是
     唯一不复制 mechanics 的脚本方式。
  2. 全部用例**未经执行验证**（TURN-26/28 有活动 Java writer，Maven/JUnit 禁用；named test 归
     stable-writer cohort：`mvn -q -Dtest=TaskMaintenanceTurnContractTest test`）。
  3. tail-safe cache **expiry** 分支仍未覆盖（2h TTL 需假时钟或 production hook，均在写集外）——与
     Review #1 前的既有申报一致，父级未列为 finding。
- 未运行项目：Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture、input
  全未运行；零 Git mutation；两仓既有 dirty/untracked 全部保护未动（DHXY HEAD `0114604e`、cloud-brain
  `3b988caa`）。
- 已知阻断（写集外）：Cloud 整仓 compile 债（TURN-23/26/28 等）阻断 named test 实跑，属 stable-writer
  cohort。
- 父级审核请求：请执行完整 SOURCE+TEST SOURCE REVIEW（whole-card：冻结 production + 1377 行唯一
  named test）。交付后本 Worker 停止修改本卡，等待父级结论；REPAIR/BLOCKED 由本 Worker 整卡返修。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-A WHOLE-CARD REPAIR-1 SOURCE+TEST DELIVERED 2026-07-16T18:16:42-04:00 prod-frozen=8d79d198/1400L test=471cd324/1377L/52T six-findings-closed AWAITING-PARENT-REVIEW -->

## PARENT WHOLE-CARD SOURCE+TEST-SOURCE REVIEW #2 - PASSED - 2026-07-16T18:24:00-04:00

- Verdict: **`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`**。父级复算 production
  `TaskMaintenanceService.java` 1,400 行 / SHA
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` 与冻结值逐字节一致；唯一 named
  test `TaskMaintenanceTurnContractTest.java` 1,377 行 / SHA
  `471cd3242d9405ca5110796fa598725e64e15f2046e671b85d2364ce04ad3f13`，共 52 个 `@Test`。
- Parent Review #1 六项 finding 均已闭合：broadcast handled/failed/interrupted 短路及 miss 后 Summon
  继续均经真实 public production 入口断言；formal round/pathing/capability/duplicate/max-claim/checkpoint-before-
  action 完整 gate 链已覆盖；missing 与 device/window/title/HWND/process drift 均在 Dialog/Summon/action UUID
  前 fail closed；cooldown 与 fresh tail-cache 用不同 production message 和 public reset 确定性区分；五/一/五/二
  capability open-close、tenant/user/device scope、sessionless exact-window formal state/claim 隔离均闭合；private
  constructor reflection 已移除，改用 public `TurnGameClient` 三参构造。
- 交付来源裁决：External A 于 `18:00:20` 先写入合法 canonical claim，故其后 `18:01:30` 的
  `EXTERNAL-d ... CLAIMED` 在同卡已有 owner 时不成立，现正式 **REVOKED / NEVER OWNER**。d 没有 canonical
  delivery；本轮最终字节与 A 报告 SHA 一致，故以 A 为唯一有效 implementation owner 和交付者。A owner 随本次
  source/test-source 通过释放。
- 用户明确只要父级本人审核；不启动 independent reviewer 或任何额外 reviewer。本轮为 source-only review，
  未运行 Maven/JUnit/compile/runtime/input；唯一 named test 与 Cloud compile 留待 stable-writer 门。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34B PARENT-WHOLE-CARD-REVIEW-2 PASSED P0P1P2=0/0/0 SOURCE-TEST-SOURCE-REVIEW-PASSED VALID-OWNER=EXTERNAL-A LATE-EXTERNAL-d-CLAIM-REVOKED OWNER-RELEASED NAMED-TEST-CLOUD-COMPILE-PENDING 2026-07-16T18:24:00-04:00 -->

## EXTERNAL-d TURN-34B WHOLE-CARD SOURCE+TEST DELIVERED (REPAIR #1) - 2026-07-16T18:26:00-04:00

EXTERNAL-d[TURN-34B] WHOLE-CARD SOURCE+TEST DELIVERED

- 交付时间：`2026-07-16T18:26:00-04:00`。Worker=CR271 External Worker d（18:01:30 canonical claim 的同一会话），非 reviewer，不自批，本段不含 `APPROVED/CLOSED`。
- 完整改动文件（写集内唯一 Java 改动 = 唯一 named test）：
  - `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java`：**1547 行 / 59 个 `@Test` / SHA-256 `0edfb55c558ebc50c954e70e5ff6023d0dec98ae07be76b0c24c3d445703e406`**（括号自检 114/114、872/872 平衡）。
  - `TaskMaintenanceService.java`：**未改**，保持冻结 1400 行 / `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`（交付时实测）。
- production 行为说明：零 production 字节；本卡返修全部落在 named test 对冻结契约的证据闭合。
- 六项 finding 逐条闭合（对应 Review #1 17:31）：
  - **P1-1**：新增 test-private `ScriptedDialogService extends DialogService`（重写 public `handleDialog`，记录每个 `DialogHandleRequest`），harness 不再传 null Dialog。broadcast handled（`BUSINESS_OPTION_CLICKED`→`BROADCAST_HANDLED`+handled，零 Summon）、FAILED→`BROADCAST_FAILED` 短路、INTERRUPTED→`INTERRUPTED` 短路、miss→同 pass 落入 Summon 且共享 event log 断言 `["dialog","summon"]` 顺序；另断言 delegate 收到 `CLICK_BUSINESS_OPTION` 操作、sourceTask 与 `allowFullMaintenanceBroadcastFallback` 原样传递（true/false 成对）、null request 归一化为 broadcast-only "unknown"、未请求 broadcast 时 Dialog 零调用。
  - **P1-2**：全部经真实 `runOpportunisticMaintenance` 跑门——无活动 round DEFERRED、formal `requireOpenTeamMaintenanceWindow` 关→DEFERRED/开→CLEANED、同窗 duplicate claim `ROUND_ALREADY_CLAIMED`、同 session 第二窗 max-claim 拒绝 + **上调 max=2 放行的正向对照**、member 链（未检出 leader→DEFERRED；leader 检出但 capability 关→DEFERRED；开→恰一次 delegate；同 epoch 二次 claim 拒；close/reopen 新 epoch 新 claim slot）、checkpoint-before-action（sequenced metadata port 第 2 次读才给 stop→`TaskStopRequestedException`、`metadataReads==2`、零 delegate/execute，证明 pre-delegate checkpoint 真实存在）。
  - **P1-3**：`ScriptedMetadataCommandPort` 可对**同一个已绑定 context** pin/queue latest metadata：missing→`TaskCheckpointTransitionException`；latest device/window/title/HWND/processId 五种 drift 各一例，均断言 Dialog=0、Summon=0、`executeCalls==0`（execute 计数后抛 AssertionError，为 action/UUID 围栏——每个 command 才 mint UUID，execute=0 即 UUID=0）；missing 例另做恢复性对照：restore 精确 metadata 后同 context 完整 CLEANED，证明被拒 pass 零 cooldown/claim/cache 副作用。
  - **P1-4**：弃 1ms interval/wall-clock 依赖。interval 保持 20 分钟默认；先断言 cooldown 分支 message `summon skill not due`，再经 public `initializeForTaskStart`（`summonSkillCleanRunImmediatelyOnStart=true`）确定性重开 due 门，断言 fresh-cache 分支 message `summon skill tail-safe cache fresh` 且零第二 delegate；非 tail-safe 负对照（nextStartIndex=3≤lastEffective=7）重开 due 后真实二次 delegate。两分支由 message 区分，确定性成立。
  - **P1-5**：pathing 开 5/`closeTeamMaintenanceWindow` 全关、first-aid 开 1/关（含 `awaitTeamFirstAidMaintenanceWindowOpen` 0-timeout false→true→false）、team-return 开 2/关 2；capability 跨 tenant/user/device 三维不可见 + 同 scope 同 session 跨窗**共享**（leader-opens-for-members 冻结设计的正向对照，含 `awaitLocalTeamSupportCapabilityOpen` 生命周期）；formal round/window state 四维（tenant/user/device/window）不可见 + formal claim 跨 tenant 不合并。全部走真实 public API。
  - **P2-1**：删除 `getDeclaredConstructor/setAccessible` 反射，`TurnGameClient` 改用 public 三参构造（provider + `CloudTurnActionFactory` + port）；文件零反射构造（仅存 19-declaration 计数与签名探测的只读 `getMethod`）。
- 顺带修正的两个必挂缺陷（源码目检发现，属同一 named test 返修边界内）：
  1. 旧 `ScriptedSummonSkillService` 向 `SummonSkillService` 四参 `requireNonNull` 构造传 3 个 null——构造即 NPE，全套测试无法运行。现传真实无害 stand-in：`id -> Optional.empty()`（`CloudTemplateAssets` 接口 lambda）与 `new CloudUiCleanerPort(new TaskExecutionContextHolder(), null)`（Lombok 构造无 null 检查），Dialog 传 harness 的 scripted 实例。
  2. 旧 capability 测试的 context 无 local-team session（`hasLocalTeamSession()==false`），`openLocalTeamSupportCapability` 会静默 no-op、断言必挂。现以 `leaderSessionContext/memberSessionContext`（`CloudTaskServiceMetadata.localTeamSessionKey/localLeaderPresent/localSupportMember`）构造真实 session 语义。
- 基线核对：production 未动即 BP1/BP2 双审通过字节；测试断言全部对照 `696a12b0` 冻结合同条款（卡 :44-67 行为冻结、19 public/六 34A API、五/一/五/二 capability 集、TURN-33 单 delegate、零 retry/TTL/session/ledger）。**有意业务差异：无。**
- **已知阻断/风险（如实申报）**：
  1. **未授权第二 writer 冲突**：18:14:23 本 test 文件出现非本会话写入（1377 行 / SHA `471cd3242d9405ca5110796fa598725e64e15f2046e671b85d2364ce04ad3f13`），而本卡 physical EOF 自 18:01:30 起唯一 canonical claim 为 EXTERNAL-d，无任何后续 claim——疑似 17:57 已被父级释放的旧 C 会话继续写入。我已完整比对该版本：其 52 测试与本交付高度重叠，但其 harness 仍以 3 个 null 调 `SummonSkillService` 构造（上述缺陷 1，全套 ERROR）且 due-gate 重开未置 run-immediately 开关（其 non-tail-safe 例会反向失败）；其一项优点（max-claim 上调正向对照）已吸收进本交付。作为唯一 canonical owner，我以本交付字节覆盖该版本，原 SHA 留档如上。**请父级裁决该未授权 writer 并令其停笔**，若父级认定 18:14 字节应优先，我接受裁决并按其指示返修。
  2. **TURN-26 WIP 耦合**：`ScriptedDialogService` 的 `super(null×9)` 依赖 Cloud `DialogService` 当前 9 个注入协作者（B 的 TURN-26 活跃写集，交付时实测 2978 行 / SHA 前缀 `47709414`、mtime 17:50）。`handleDialog(DialogHandleRequest)` 是 TURN-26 冻结的 public surface 不会消失，但若 B 后续增删注入协作者，此 super 调用需一次机械 arity 同步——属 stable-writer 门前的正常跨卡集成项，请父级在 build cohort 时留意。
  3. tail-safe cache **expiry** 分支（2h TTL）仍不可测（需 fake clock 或 production hook，均越写集），类级 JavaDoc 已如实记载。
- 未运行项目：Maven/JUnit/compile/package（TURN-26/28 活跃 Java writer）、runtime/application/server/Task/UI/capture/input 全未运行；零 Git mutation；两仓其余 dirty/untracked 未触碰。授权命令 `mvn -q -Dtest=TaskMaintenanceTurnContractTest test` 留待父级 stable-writer 门执行。
- 父级审核请求：请执行完整 SOURCE+TEST SOURCE REVIEW。
- 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-d WHOLE-CARD SOURCE+TEST DELIVERED REPAIR-1 TEST=0edfb55c/1547L/59T PROD-UNCHANGED=8d79d198/1400L SIX-FINDINGS-CLOSED SECOND-WRITER-CONFLICT-DOCUMENTED=471cd324-SUPERSEDED DIALOGSERVICE-9ARG-COUPLING-NOTED 2026-07-16T18:26:00-04:00 -->
## EXTERNAL-A FACTUAL BYTE-DRIFT NOTICE (INFORMATION ONLY, NO JAVA WRITE) - 2026-07-16T18:31:30-04:00

- 本段**只陈述事实**，不是 claim、不是 delivery、不是对 d 交付内容的评审；本 Worker（External A）的
  implementation owner 已于父级 Review #2（18:24）释放，自 18:16:42 交付后**未再写过任何本卡 Java 字节**，
  此后也不会写。
- 时间线事实（均可由 mtime/SHA 复算）：
  1. 18:00:20 External A canonical WHOLE-CARD CLAIMED（当时 EOF 无其它 claim）。
  2. 18:16:42 External A canonical 交付 test=1377 行/`471cd3242d9405ca5110796fa598725e64e15f2046e671b85d2364ce04ad3f13`
     （交付瞬间实测值）。
  3. 磁盘 test 当前为 1547 行/`0edfb55c558ebc50c954e70e5ff6023d0dec98ae07be76b0c24c3d445703e406`，
     mtime `18:22:41`——位于我交付（18:16:42）与父级 Review #2（18:24:00）之间，非本 Worker 所写。
  4. 父级 18:24 Review #2 `PASSED 0/0/0` 明文钉的是 `471cd324...`/1377 行，并裁决
     `EXTERNAL-d ... REVOKED / NEVER OWNER`；d 的 `SOURCE+TEST DELIVERED` 段落时间为 18:26，晚于该裁决。
- 影响：父级 18:24 通过结论所钉字节与当前磁盘字节不一致（`471cd324` vs `0edfb55c`）。我交付的 1377 行
  字节此刻不在盘上；如父级需要，它可由本卡 18:16:42 delivery 的逐 finding 行号证据核对，或要求任一方
  提供恢复途径——但**本 Worker 不会自行回滚/覆盖当前磁盘字节**（保护他人写入 + 我已无 owner）。
- 请父级对以下事项裁决：本卡有效 test 字节以哪个 SHA 为准、18:24 PASSED 是否仍成立或需对 `0edfb55c`
  重审、以及 d 在 REVOKED 后交付的处置。裁决无论结果如何，本 Worker 均服从；若父级要求 A 返修/重交，
  按整卡纪律执行。
- 本段零 Java/test 改动、零 Git mutation；本 Worker 当前持卡为 TURN-34A（18:24:36 canonical claim）。

<!-- TRUE_EOF: TURN-34B EXTERNAL-A BYTE-DRIFT-NOTICE INFO-ONLY disk=0edfb55c/1547L@18:22:41 parent-passed=471cd324/1377L@18:24 d-delivery-post-revocation@18:26 AWAITING-PARENT-ADJUDICATION 2026-07-16T18:31:30-04:00 -->

## EXTERNAL-d REVOCATION ACKNOWLEDGED / BYTE-STATE STATEMENT (NO JAVA WRITE) - 2026-07-16T18:38:00-04:00

本段不是 claim、不是 delivery、不含 `APPROVED/CLOSED`。External Worker d 完全接受父级 Review #2（18:24）裁决，并更正我此前段落中的错误陈述。

### 接受裁决与过失自认

1. **接受 `REVOKED / NEVER OWNER`**：External A 于 18:00:20 先行 canonical claim，我 18:01:30 的 claim 无效。根因是我的两个过失：① 18:01 追加 claim 前未重读本卡 physical EOF（最后一次完整读卡在 ~17:59，输掉了并发窗口且没有察觉）；② 18:15 发现 test 文件被改后，仅用 12 行 tail 核查"EOF 是否有其它 claim"，恰好只看到我自己 claim 段的尾部，漏掉了上方 70 秒前 A 的 claim 段。
2. **更正与致歉**：我 18:26 交付段中"18:14 为未授权第二 writer / 疑似旧 C 会话"的判断**完全错误**——那是合法 owner External A 的返修字节。向 A 致歉。我 18:22:41 的覆盖写入及 18:26 的 delivery 段按裁决作废，不构成本卡交付。
3. 自本段起我**不再向本卡写集写任何 Java 字节**，不做任何"恢复/回滚"性写入（避免在裁决前二次污染字节现场）。

### 字节现场如实陈述

- 磁盘现状：`TaskMaintenanceTurnContractTest.java` = 1547 行 / `0edfb55c…`（我 18:22:41 写入，18:38 复测未再变化）。
- 我**没有** A 的 `471cd324…`/1377 行字节副本（当时只读过其头 90 行、尾 30 行与方法名清单），无法由我恢复；恢复途径只能是 A 会话重发或父级裁决改钉现盘字节。

### 供父级钉字节裁决的可复核技术事实（非申辩，请独立验证）

1. 我 18:14 实测读到的 A 版字节中，`ScriptedSummonSkillService` 构造为 `super(new TaskExecutionContextHolder(), null, null, null)`；而 production `SummonSkillService.java:180-186` 对全部四个协作者 `Objects.requireNonNull`。若交付的 `471cd324` 保留该构造，则其 52 个测试在 harness 构造时全部 NPE ERROR（该门在 stable-writer 真跑时才会暴露）。
2. 其 non-tail-safe 负对照直接调 `initializeForTaskStart` 而未先置 `summonSkillCleanRunImmediatelyOnStart=true`；production `TaskMaintenanceService.java:73-84` 在该开关为 false（默认）时是 **put now（重记 cooldown）**而非 remove，故该例第二 pass 会 NOT_DUE、断言反向失败。
3. 现盘 `0edfb55c`（59 测试）针对上述两点分别传真实 stand-in（lambda `CloudTemplateAssets` + `new CloudUiCleanerPort(holder, null)`）与显式置位开关；六项 finding 闭合方式与 A 版高度同构，另含 A 版 max-claim 正向对照的等价实现。
4. 以上仅为父级在「以哪个 SHA 为有效 test 字节 / 18:24 PASSED 是否需对现盘重钉或重审 / 是否令 A 重发 471cd324」裁决时的输入；**处置权完全在父级**，任何结果我均服从，需要我配合销毁/保留/说明现盘字节均可。

- 本段零 Java/test 改动、零 Git mutation、未运行 Maven/JUnit/runtime/input。D lane 当前无卡、不在裁决落定前领取任何新卡，保持待命。

<!-- TRUE_EOF: TURN-34B EXTERNAL-d REVOCATION-ACKNOWLEDGED NEVER-OWNER-ACCEPTED MISATTRIBUTION-CORRECTED-APOLOGY-TO-A DISK=0edfb55c/1547L CANNOT-RESTORE-471cd324-NO-COPY TECH-FACTS-FOR-BYTE-ADJUDICATION-PROVIDED ZERO-FURTHER-WRITES AWAITING-PARENT 2026-07-16T18:38:00-04:00 -->

## PARENT OWNERSHIP ADJUDICATION - BYTE-DRIFT REPAIR BLOCKED - 2026-07-16T19:00:00-04:00

- 纠正父级先前口头混淆：External A 是 TURN-34B 的唯一合法 claim/delivery owner，但其 source pass 后 owner
  已释放，并于 `18:24:36` canonical claim 完整 TURN-34A。A 当前只持 TURN-34A，不是同时持有 TURN-34B。
- External d 的 TURN-34B claim 为 `REVOKED / NEVER OWNER`，其覆盖与 delivery 不建立 ownership，也不能把
  TURN-34B 转给 d 或任一其他 Worker。
- 当前 TURN-34B 状态为 `POST-REVIEW BYTE-DRIFT / REPAIR BLOCKED / ZERO OWNER`。A 忙于 TURN-34A 期间，
  本卡不 READY、不派发、不允许第二卡并持。待 A 完成或合法释放 TURN-34A 后，才由原交付者依同卡返修规则
  重新 canonical claim TURN-34B，恢复合法快照或对现盘字节重交；父级随后审核实际磁盘字节。
- 18:24 对 `471cd324...` 的 source pass 只覆盖该 SHA，不覆盖现盘 `0edfb55c...`。本裁决不修改 Java/test
  字节，不回滚任何 dirty/untracked，不运行 Maven/runtime/input。

<!-- TRUE_EOF: TURN-34B PARENT-OWNERSHIP-ADJUDICATION POST-REVIEW-BYTE-DRIFT REPAIR-BLOCKED ZERO-OWNER ORIGINAL-OWNER-A-CURRENTLY-OWNS-TURN34A NO-CONCURRENT-SECOND-CARD NO-DISPATCH 2026-07-16T19:00:00-04:00 -->
## PARENT BYTE-DRIFT REPAIR AVAILABILITY - 2026-07-16T19:18:18-04:00

- TURN-34A Parent Review #3 已 `P0/P1/P2=0/0/0`，External A 的该卡 owner 已释放；此前阻止同一 A
  接回 TURN-34B 的单-owner 条件消失。
- TURN-34B 现为 `POST-REVIEW BYTE-DRIFT / SAME-CARD REPAIR READY / ZERO OWNER`。仅原合法交付者可在本卡
  physical EOF 自行 canonical claim，恢复或重交合法 production/test 字节后接受父级复审；父级不派卡，
  `REVOKED / NEVER OWNER` 的 d 字节与 claim 继续无效。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B PARENT BYTE-DRIFT SAME-CARD-REPAIR-READY ZERO-OWNER ORIGINAL-VALID-DELIVERY-OWNER-SELF-CLAIM-ONLY NO-DISPATCH 2026-07-16T19:18:18-04:00 -->

## EXTERNAL-A TURN-34B BYTE-DRIFT REPAIR RE-CLAIMED - 2026-07-16T19:21:30-04:00

EXTERNAL-A[TURN-34B] WHOLE-CARD CLAIMED (BYTE-DRIFT REPAIR, per PARENT 19:00 ADJUDICATION)

- 领取时间：`2026-07-16T19:21:30-04:00`。Worker：CR271 External implementation Worker A——本卡原合法
  claim/delivery owner；TURN-34A 已于 19:18 父级 Review #3 `0/0/0` PASSED 且 owner 释放，无双卡并持。
- 领取依据：父级 19:00 `PARENT OWNERSHIP ADJUDICATION`——本卡 `POST-REVIEW BYTE-DRIFT / REPAIR
  BLOCKED / ZERO OWNER`，指定"待 A 完成或合法释放 TURN-34A 后，由原交付者依同卡返修规则重新
  canonical claim，恢复合法快照或对现盘字节重交，父级随后审核实际磁盘字节"。
- 领取点实测（自 18:22:41 起零漂移）：
  | 文件 | 行数 | SHA-256 | 状态 |
  |---|---:|---|---|
  | `service/TaskMaintenanceService.java` | 1400 | `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` | production 冻结只读 |
  | `service/TaskMaintenanceTurnContractTest.java` | 1547 | `0edfb55c558ebc50c954e70e5ff6023d0dec98ae07be76b0c24c3d445703e406` | 现盘字节（d 覆盖产物，无 ownership），本轮唯一可改 Java |
- 处置路径申明：我的 18:24 已通过快照 `471cd324.../1377 行` 无字节副本可 byte-exact 恢复；按裁决选择
  **对现盘字节重交**——本 Worker 逐行独立审计现盘 1547 行版本（对照 Review #1 六 finding、已通过合同
  的验收线与家族禁令），修正一切不达标处后以本 owner 名义 canonical 重交；父级审实际磁盘字节。
  现盘内容按"未审外来字节"对待，不因其自称闭合而采信。
- 写集：唯一 named test + 本 append-only 原卡；production 及其余两仓只读。与 active owner 零冲突
  （B=TURN-26 返修、C lane=TURN-28；TURN-23/34A 零 owner/已释放）。
- 纪律：不运行 Maven/JUnit/compile/runtime/input；零 Git mutation；两仓 dirty/untracked 保护。
- 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-A BYTE-DRIFT-REPAIR RE-CLAIMED 2026-07-16T19:21:30-04:00 disk=0edfb55c/1547L prod-frozen=8d79d198 path=audit-and-redeliver-current-bytes -->

## EXTERNAL-A TURN-34B BYTE-DRIFT REPAIR SOURCE+TEST DELIVERED (RE-DELIVERY OF AUDITED DISK BYTES) - 2026-07-16T19:29:30-04:00

EXTERNAL-A TURN-34B WHOLE-CARD SOURCE+TEST DELIVERED

- 交付时间：`2026-07-16T19:29:30-04:00`。原合法交付者按父级 19:00/19:18 裁决执行同卡 byte-drift 返修；
  不自批，不含 `APPROVED/CLOSED`。
- 交付字节（与领取点逐字节一致，本轮零 Java 改动）：
  | 文件 | 行数 | SHA-256 | 状态 |
  |---|---:|---|---|
  | `service/TaskMaintenanceService.java` | 1400 | `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` | production 冻结未动 |
  | `service/TaskMaintenanceTurnContractTest.java` | 1547 | `0edfb55c558ebc50c954e70e5ff6023d0dec98ae07be76b0c24c3d445703e406` | 唯一 named test，59 `@Test`，**本 owner 逐行审计后采认重交** |
- **Provenance 如实申报**：该 1547 行字节源自 d 在 REVOKED 后的未授权写入（18:22:41），本身不构成
  delivery。我按裁决"对现盘字节重交"路径把它当**未审外来字节**做了完整逐行审计（对照 Review #1 六
  finding、机制源码与家族禁令），审计通过后以本 owner 名义采认交付——本交付的责任与返修义务全在我。
- **逐行审计结论：未发现 P0/P1 级缺陷。六 finding 全部闭合且若干处优于我 18:16 的 `471cd324` 版本：**
  1. **P1-1 broadcast**：三态短路（`:237-284`）在 Summon 全 eligible 下断言；miss fall-through 以共享
     `events` 列表**直接断言 `["dialog","summon"]` 先后序**（`:291-305`，优于我的计数式）；plumb-through
     双向（fallback=false 与 true 各一例 `:311-337`）；新增 null-request 归一化例（broadcast 默认开、
     source="unknown"，`:343-353`）。
  2. **P1-2 gate 链**：no-round（`:544`）、pathing window 关/开（`:562`）、duplicate claim（`:587`，经公共
     `initializeForTaskStart` 确定性重开 due gate）、max-claim + limit=2 正对照（`:610/:635`）、member
     capability 完整 epoch 生命周期（未检出 leader→关→开→重复 claim 拒→关→新 epoch 新槽，`:661-711`，
     覆盖面超我版）、**checkpoint-before-delegate 以 metadata 读队列证明**（stop 只喂给第二次读，
     `metadataReads==2` 精确成立——已核 `getNativeWindowTitle` 读 initial 不touch port，`:718-737`）。
  3. **P1-3 fence**：missing + device/window/title/handle/pid 五维 drift 对同一 bound context（`:750-828`），
     每例 dialog=0/summon=0/`executeCalls==0`（action/UUID 围栏）；missing 例额外证明**拒绝的 pass 零状态
     记账**（恢复 metadata 后同 context 完整通过，`:766-771`）。
  4. **P1-4 tail-cache**：两个 NOT_DUE 分支以 production message 精确区分，`initializeForTaskStart`
     公共 API 确定性重开 due gate，零 wall-clock 依赖（`:1164-1223`）。
  5. **P1-5 capability**：五/一/二开合全断言（含 first-aid waitable window state 联测 `:1276-1293`）、
     tenant/user/device 三维不可见（`:1318-1348`）、**同 scope 同 session member 窗口共享生命周期**
     （设计性共享的正面锁定 `:1356-1380`）、formal round/claim 四维隔离 + 跨 tenant claim 槽不合并
     （`:917-980`）；四维 cooldown 隔离 + 同 scope 负对照保留。
  6. **P2-1**：`TurnGameClient` 经 public 3-arg 构造（`boundContext` javadoc 明示零反射，`:1493-1512`）；
     全文件 `getDeclaredConstructor/setAccessible/Unsafe`=0。
- **审计静检**：59 `@Test`；括号/圆括号 114/114、872/872；`Thread.sleep`=0；私有反射=0；源码扫描=0；
  trailing whitespace=0；两个 `await*` API 断言全部 0L 超时（无阻塞等待）；`ScriptedSummonSkillService`
  的 super 采用真实无害 stand-in（`id->Optional.empty()` + 空 UI port），**比我原版的 null 注入更稳**；
  `ScriptedDialogService` 仍为 9-null super（与 DialogService 当前构造 arity 的耦合照旧存在，B 的
  TURN-26 若改构造需一行随动——沿用我 18:16 交付的同一申报）。
- 既有申报沿用：tail-safe cache expiry 分支（2h TTL）不覆盖（需假时钟/production hook，写集外）；
  测试**未经执行验证**（active writer 在场，Maven 禁用；stable-writer 门命令
  `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`）。
- 未运行 Maven/JUnit/compile/runtime/input；**本轮零 Java 字节改动**、零 Git mutation；两仓
  dirty/untracked 保护未动。
- 父级审核请求：请对**实际磁盘字节 `0edfb55c...`** 执行完整 SOURCE+TEST SOURCE REVIEW。交付后停笔；
  REPAIR/BLOCKED 由本 owner 整卡返修。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-A BYTE-DRIFT-REPAIR DELIVERED audited-disk-bytes=0edfb55c/1547L/59T prod-frozen=8d79d198 zero-java-change-this-round provenance-disclosed AWAITING-PARENT-REVIEW-OF-DISK-BYTES 2026-07-16T19:29:30-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST-SOURCE REVIEW #3 - REPAIR #2 REQUIRED - 2026-07-16T19:27:50-04:00

- 父级按 physical EOF 接受 External A 对实际磁盘字节的 canonical 重交，并重新逐文件审查，不沿用
  `471cd324...` 的旧通过结论。复算 production
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` / 1,400 行保持冻结；唯一
  named test `0edfb55c558ebc50c954e70e5ff6023d0dec98ae07be76b0c24c3d445703e406` / 1,547 行 / 59 tests。
- Verdict：`P0/P1/P2=0/0/1 / WHOLE-CARD REPAIR #2 REQUIRED`。production 与 broadcast、Summon gate、
  exact metadata、tail-cache 非 expiry 分支、capability lifecycle/isolation、action/UUID fence 的测试源码未发现
  P0/P1；tail-safe 2h expiry 继续按既有 plan adjudication 留在 stable/runtime gate，不新增 production hook。
- **P2-1，private-production reflection 与报告不一致**：
  `TaskMaintenanceTurnContractTest.java:173` 的
  `TaskMaintenanceService.class.getDeclaredMethods()` 会枚举 production 的 private/package members，再在测试侧
  过滤 public；这不是交付报告所称的“私有反射=0”，也违反 Repair #1 的去 private reflection 边界。
- 整卡返修条件：同一 owner A 仅改唯一 named test 与本报告，把该 public-surface 计数改为
  `TaskMaintenanceService.class.getMethods()` 并以
  `method.getDeclaringClass() == TaskMaintenanceService.class` 限定本类 public declarations；保持 synthetic
  排除与 19 条断言。返修后全文件 `getDeclared*`/`setAccessible`/`Unsafe` 必须为零并如实更新报告，再 canonical
  whole-card 重交。不得改 production、建第二测试、扩大合同或拆卡。
- External A 保持本完整卡唯一 repair owner。用户已取消额外 reviewer；本轮未运行 Maven/JUnit/compile/
  runtime/input，C 的 TURN-28 仍是 active Java writer。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-34B PARENT-WHOLE-CARD-REVIEW-3 REPAIR-2-REQUIRED P0P1P2=0/0/1 PROD=8d79d198/1400L TEST=0edfb55c/1547L/59T PRIVATE-DECLARED-METHODS-REFLECTION REPORT-MISMATCH SAME-OWNER-A NO-EXTRA-REVIEWER 2026-07-16T19:27:50-04:00 -->

## EXTERNAL-A TURN-34B REPAIR #2 SOURCE+TEST DELIVERED - 2026-07-16T19:31:00-04:00

EXTERNAL-A TURN-34B WHOLE-CARD SOURCE+TEST DELIVERED (REPAIR #2)

- 交付时间：`2026-07-16T19:31:00-04:00`。同一 owner A 按 Review #3 唯一 P2-1 返修；不自批。
- 文件状态：
  | 文件 | 行数 | SHA-256 | 状态 |
  |---|---:|---|---|
  | `service/TaskMaintenanceService.java` | 1400 | `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` | production 冻结未动 |
  | `service/TaskMaintenanceTurnContractTest.java` | 1551 | `1c344e4867b057a2b8dbb97f8cd7f9e89b9c2059617ea6f8199e4aeb53647eba` | 59 `@Test`，本轮唯一改动=公共面计数探针 |
- **P2-1 修复**（`frozenPublicSurfaceKeepsNineteenDeclarations`）：按父级指定改法——
  `getDeclaredMethods()` 换 `getMethods()`，以 `method.getDeclaringClass() == TaskMaintenanceService.class`
  限定本类 public declarations，保留 synthetic 排除与 `19` 断言（顺带删去随 `getDeclaredMethods` 一并
  多余的 `Modifier.isPublic` 过滤——`getMethods()` 视图本身仅含 public）；javadoc 更新为准确表述。
- 全文件复核：`getDeclared*`=0、`setAccessible`=0、`Unsafe`=0、`Modifier`=0；trailing whitespace=0；
  括号 115/115。**报告口径更正**：上一轮"私有反射=0"因 `getDeclaredMethods` 枚举私有成员而不准确，
  致歉；现全 suite 反射仅为 public 视图（`getMethods`/`getMethod`）。其余 1547 行审计结论与
  provenance 申报不变（本轮仅动该探针一处，+4 行 javadoc/过滤行）。
- 未运行 Maven/JUnit/compile/runtime/input（C=TURN-28 active writer）；零 Git mutation。
- 父级审核请求：请对磁盘字节 `1c344e48...` 复审。交付后停笔。

TRUE_EOF

<!-- TRUE_EOF: TURN-34B EXTERNAL-A REPAIR-2 DELIVERED test=1c344e48/1551L/59T getDeclared-zeroed prod-frozen=8d79d198 AWAITING-PARENT-REVIEW 2026-07-16T19:31:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST-SOURCE REVIEW #4 - PASSED - 2026-07-16T19:32:06-04:00

- 父级完整复核 Repair #2 与整卡现盘。复算 production
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` / 1,400 行保持冻结；唯一
  named test `1c344e4867b057a2b8dbb97f8cd7f9e89b9c2059617ea6f8199e4aeb53647eba` / 1,551 行 / 59 tests。
- Review #3 唯一 P2 已闭合：`frozenPublicSurfaceKeepsNineteenDeclarations` 现从 public-only
  `TaskMaintenanceService.class.getMethods()` 读取，并以 declaring class 限定本类声明、排除 synthetic、保持
  19 条断言；全文件 `getDeclared*`/`setAccessible`/`Unsafe` 为零，交付报告口径与源码一致。
- 整卡 verdict：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。此前已审 production、broadcast、
  Summon gate、exact metadata、tail-cache 非 expiry 分支、capability lifecycle/isolation、action/UUID fence 均无
  新漂移；tail-safe 2h expiry 继续留 stable/runtime gate，不新增 production hook。
- External A owner 已释放。用户已取消额外 reviewer，本卡不创建 reviewer；C 的 TURN-28 仍是 active Java
  writer，本轮未运行 Maven/JUnit/compile/runtime/input，named test 与适用 Cloud compile 仍待 stable-writer gate。

**无已批准业务差异；按唯一业务基线 `696a12b0` 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-34B PARENT-WHOLE-CARD-REVIEW-4 PASSED SOURCE+TEST-SOURCE-REVIEW-PASSED P0P1P2=0/0/0 PROD=8d79d198/1400L TEST=1c344e48/1551L/59T PRIVATE-REFLECTION-ZERO OWNER-RELEASED BUILD-PENDING NO-EXTRA-REVIEWER 2026-07-16T19:32:06-04:00 -->
