# TURN-34BP2 - scoped maintenance coordination key foundation

## Claim gate

- Parent: `CR271 / TURN-34B`.
- Card type: bounded Cloud production implementation prerequisite.
- Status: `EXTERNAL-C NEXT / CLAIM REQUIRED`.
- `startDependsOn`: TURN-34BP1 Parent Review #3
  `SOURCE+TEST SOURCE REVIEW PASSED` at production `a9c34d4e...` / test `3b117895...`.
- Final BP1 independent review/build remains a later approval gate and does not block this disjoint source start.
- Only one physical true-EOF owner is allowed. External C must append `EXTERNAL-C TURN-34BP2 CLAIMED` before
  editing and produce a source increment, canonical delivery or `OWNER RETURNED` in its first five-minute window.

## Frozen starting identities

| Artifact | Frozen identity |
|---|---|
| Cloud `TaskExecutionContext.java` (read-only) | 527 lines / SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` |
| Cloud `TaskExecutionContextTurnContractTest.java` (read-only) | 872 lines / SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` |
| Cloud `TaskMaintenanceService.java` (modifiable) | 1,224 lines / SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc` |

If the maintenance source no longer matches this identity before claim, stop and return the card; do not layer this
implementation over unreviewed bytes.

## Exact write set

1. Modify only `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`.
2. Append only this child card.

All tests, BP1 files, holder/context/client/protocol/model/POM, AutoCombat/Task callers, Dialog/Summon/TeamReturn/
CommonBox services, DHXY Java, parent cards, plan, ACTIVE_WORK and dashboard are read-only to the Worker.

## Required source change

Replace only the four shared string-key maps with private typed keys:

- `activeTeamRoundByKey` -> `Map<ScopedTeamKey, Integer>`;
- `teamMaintenanceWindowStateByRound` -> `Map<TeamRoundKey, TeamMaintenanceWindowState>`;
- `localTeamSessions` -> `Map<ScopedLocalSessionKey, LocalTeamSessionState>`;
- `summonSkillClaimsByTeamRound` -> `Map<MaintenanceClaimKey, Set<ScopedWindowKey>>`.

Keep the four per-window Summon maps and `currentWindowKey`/fingerprint/generation/cache work unchanged for BP3.
Private key types belong at the file bottom and must encode execution scope (`tenantId/userId/deviceId`), exact
window or explicit local-team session, maintenance key, round/capability/epoch and formal-vs-local claim kind by
record/enum type rather than delimiter strings.

Supplied `TaskExecutionContext` outranks the holder. Only supplied-null plus empty holder may use an explicit
no-context key. Existing context authority failures must not broad-catch downgrade to unscoped state. Same scope
and explicit local session may share; different tenant/user/device must isolate; without a session, windows isolate.
Preserve the existing maintenance-key fallback order and use one typed map decision only: no dual lookup, prefix
parse, `team + "#"`, `local-team:` alias, compatibility fallback or new global key.

## Frozen public and business surface

All 19 public method signatures remain byte-compatible: `initializeForTaskStart`, `beginTeamMaintenanceRound`,
the three team-window open/close methods, two local-support open/close methods, three await/query methods, six
local-session candidate/role/capability methods, `completeLocalTeamSessionWindow`, and
`runOpportunisticMaintenance`. Preserve the five constructor collaborators and the six TURN-34A caller-visible
maintenance APIs. Do not activate the four currently uncalled local-session lifecycle APIs.

The business order remains normalize -> first checkpoint -> optional broadcast -> handled/failure/interrupted
short-circuit -> at most one Summon delegate -> no-action. Preserve CommonBox priority, TeamReturn capability-only
boundary, Summon gates/static-tail/UNKNOWN behavior, claim acquire/release/retain, `GameContext.ActionState`, and
the capability open/close set `5/1/5/2` exactly as current production and `696a12b0` define them.

BP2 adds zero metadata reads, checkpoints, delegates, commands, actions, UUIDs, retries, sleeps, timers, TTL,
session authority, owner, lease, ledger, queue or durable workflow. Terminal/uncertain exceptions propagate through
the existing path once; no retry/replay/resend or success fabrication is allowed.

## Delivery gate

Canonical delivery must record starting/final source SHA, line count and changed method/type index; prove the two-item
write set, four typed shared maps, four untouched per-window maps, 19 unchanged public signatures, supplied-context
precedence, scope/session/window isolation, zero delimiter/prefix fallback and zero new command/UUID/retry surface.
List every intentional business difference; expected text is `无已批准业务差异；按 696a12b0 等价迁移`.

Do not run Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or perform Git mutation.
After delivery stop editing and wait for parent source review; do not self-approve.

<!-- TRUE_EOF: TURN-34BP2 EXTERNAL-C-NEXT CLAIM-REQUIRED ONE-PRODUCTION-FILE START-SHA=963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc BP1-SOURCE-PASSED 2026-07-16T11:36:00-04:00 -->

## EXTERNAL-C TURN-34BP2 CLAIMED - 2026-07-16T11:46:43.410-04:00

EXTERNAL-C TURN-34BP2 CLAIMED | card=TURN-34BP2 | lane=CR271-External-Worker-C | role=implementation-worker(not-reviewer) | claimedAt=2026-07-16T11:46:43.410-04:00 | model=claude-opus-4-8 | platformAgentId=<待父级平台真实 ID 校正>

### 领取前冻结身份复核（独立复核，三处逐位一致）

- 可改：`TaskMaintenanceService.java` = `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`（1224 行）— 与卡载一致，**未叠加在未审字节之上**。
- 只读：`TaskExecutionContext.java` = `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e`（BP1 已 source PASSED）；`TaskExecutionContextTurnContractTest.java` = `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785`。

### 写集与边界

- 仅改 `TaskMaintenanceService.java` + append 本卡。全部测试、BP1 两文件、holder/context/client/protocol/model/POM、AutoCombat 与 Task caller、Dialog/Summon/TeamReturn/CommonBox 服务、DHXY Java、父卡/计划/ACTIVE_WORK/dashboard 一律只读。
- 仅把四个共享 string-key map 换成**文件底部 private typed key**：`activeTeamRoundByKey→Map<ScopedTeamKey,Integer>`、`teamMaintenanceWindowStateByRound→Map<TeamRoundKey,TeamMaintenanceWindowState>`、`localTeamSessions→Map<ScopedLocalSessionKey,LocalTeamSessionState>`、`summonSkillClaimsByTeamRound→Map<MaintenanceClaimKey,Set<ScopedWindowKey>>`；四个 per-window Summon map 与 `currentWindowKey`/fingerprint/generation/cache 留给 BP3、保持不变。
- key 以 record/enum 编码 scope(tenantId/userId/deviceId)、exact window 或显式 local-team session、maintenance key、round/capability/epoch、formal-vs-local claim kind，**不用分隔符字符串**；供入 `TaskExecutionContext` 优先于 holder；仅"供入 null + holder 空"才可用显式 no-context key；context authority 失败**不得 broad-catch 降级**为无 scope 状态；保留既有 maintenance-key fallback 顺序、**单次 typed map 决策**（无双查/前缀解析/`team + "#"`/`local-team:` 别名/兼容回退/新全局 key）。
- 19 个 public 方法签名字节兼容、五个构造 collaborator 与六个 TURN-34A caller-visible API 保留；不激活四个当前未被调用的 local-session lifecycle API。业务序 normalize → first checkpoint → optional broadcast → handled/failure/interrupted 短路 → 至多一次 Summon delegate → no-action 不变；CommonBox 优先、TeamReturn capability-only、Summon gates/static-tail/UNKNOWN、claim acquire/release/retain、`GameContext.ActionState`、capability 开关集 `5/1/5/2` 全部保持。
- BP2 **零新增** metadata 读/checkpoint/delegate/command/action/UUID/retry/sleep/timer/TTL/session authority/owner/lease/ledger/queue/durable workflow；terminal/uncertain 沿既有路径传播一次，无重试/重发/伪造成功。
- 不运行 Maven/JUnit/compile/package；不启 runtime/input；零 Git mutation；两仓 dirty/untracked 原样。首个 5 分钟窗口内产出真实 source 增量。

## EXTERNAL-C SOURCE DELIVERED - 2026-07-16T14:01:26.481-04:00

SOURCE DELIVERED | card=TURN-34BP2 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T14:01:26.481-04:00

| File | Lines | From SHA-256 | Delivered SHA-256 |
|---|---:|---|---|
| `TaskMaintenanceService.java` | 1224 → **1353** | `963b028c...` | **`d8ba531ed0cefbc250d243f835c19ec7b710dbf5bd3950e2427e188e68158762`** |

写集仅该 production 文件 + 本卡；BP1 两文件 `a9c34d4e...`/`3b117895...`、全部测试、holder/context/client/protocol/model/POM、AutoCombat/Task callers、Dialog/Summon/TeamReturn/CommonBox、DHXY Java、父卡/计划/ACTIVE_WORK/dashboard **均未触碰**。

### 四个共享 string-key map → private typed key（卡逐条对应）

- `activeTeamRoundByKey` → `Map<ScopedTeamKey, Integer>`
- `teamMaintenanceWindowStateByRound` → `Map<TeamRoundKey, TeamMaintenanceWindowState>`
- `localTeamSessions` → `Map<ScopedLocalSessionKey, LocalTeamSessionState>`
- `summonSkillClaimsByTeamRound` → `Map<MaintenanceClaimKey, Set<ScopedWindowKey>>`

### 文件底部 private 类型（record/enum 编码，零分隔符字符串）

`ExecutionScope(tenantId,userId,deviceId)`（含显式 `NONE`）、`ScopedTeamKey(scope,maintenanceKey)`、`TeamRoundKey(team,round)`、`ScopedWindowKey(scope,windowId)`、`ScopedLocalSessionKey(scope,sessionKey)`，以及 **sealed `MaintenanceClaimKey` permits `FormalTeamRoundClaimKey(teamRound)` / `LocalSessionCapabilityClaimKey(session,capability,epoch)`** —— formal 与 local 由**类型**区分（卡允许的 "by record/enum type"），两者不可能碰撞、也不可能由解析文本产生。

### 消除的字符串 key（终审 grep 全部零命中）

`team + "#" + round`（`teamRoundKey(String,int)` 已删）、`"local-team:" + session + "#" + capability + "#" + epoch`、prune 的 `teamKey + "#"` 前缀匹配 + `Integer.parseInt(substring)` + 吞 `NumberFormatException`、以及 `localTeamSessions` 对 `context.getLocalTeamSessionKey()` 的直查。prune 现为纯字段比较：claims 用 `key instanceof FormalTeamRoundClaimKey f && f.teamRound().team().equals(teamKey) && f.teamRound().round() < activeRound`，window state 用 `key.team().equals(teamKey) && key.round() < activeRound`。**这修掉了原实现的两个真实隐患**：前缀匹配会误扫文本前缀相同的另一 team key；formal prune 会误扫共用命名空间的 local claim。

### scope 与单次 typed 决策

- `executionScope(context)`：supplied 优先 holder（复用既有 `effectiveContext`）；**仅供入 null + holder 空**才落显式 `ExecutionScope.NONE`；**不 broad-catch 降级**（与 BP3 范围内 `scopePrefix` 的 `catch(RuntimeException)→null→裸 windowId` 刻意不同：静默共享他人 tenant 状态比 fail-closed 更糟）。
- `scopedTeamKey`/`scopedWindowKey`/`scopedLocalSessionKey`/`suppliedLocalSessionKey` 各为**单次决策**，无双查/前缀解析/别名/兼容回退/新全局 key。`normalizeTeamKey` 的既有 fallback 序（explicit→requestedTaskCode→taskCode→`DEFAULT_WINDOW_KEY`）**原样保留**。
- 无 session → `scopedLocalSessionKey` 返回 null，窗口互相隔离；同 scope + 同显式 session 才共享；不同 tenant/user/device 必隔离。
- `registerLocalTeamSessionCandidate`/`completeLocalTeamSessionWindow` 两个 public API **不带 context**，故用 `suppliedLocalSessionKey`（scope 取自 holder）；二者**同源同策**，注册与移除必然命中同一 key，不产生泄漏。

### 冻结面保持（已核实）

- `public` 方法数 **恰 19**、签名字节兼容；五个构造 collaborator 与六个 TURN-34A caller-visible API 未动；四个当前未被调用的 local-session lifecycle API 未激活。
- **BP3 边界未动**：四个 per-window Summon map（`Map<String, Long>`×3 + `Map<String, SummonSkillWindowState>`）与 `currentWindowKey`/`scopePrefix`/fingerprint/generation/cache 保持 `String` 原样。
- 业务序 normalize → first checkpoint → optional broadcast → 短路 → 至多一次 Summon delegate → no-action 未变；CommonBox 优先、TeamReturn capability-only、Summon gates/static-tail/UNKNOWN、claim acquire/release/retain、`GameContext.ActionState`、capability 开关集 `5/1/5/2`、`synchronized` 临界区与 `remove(key,value)` 原子二参均保持。日志/业务消息中的 `windowKey`/`sessionKey` 文本原样（**查表 typed、对外文本不变**）。
- **零新增** metadata 读/checkpoint/delegate/command/action/UUID/retry/sleep/timer/TTL/session authority/owner/lease/ledger/queue/durable workflow；terminal/uncertain 沿既有路径传播一次。
- 新增的 `sessionKey == null → return` 均为**不可达的类型安全兜底**（相应入口守卫已含 `!hasLocalTeamSession()`），不引入新短路语义。私有 helper 签名（如 `releaseSummonSkillRoundClaimIfOwned`、`pruneOlderTeamRoundClaims`）按需扩参/改型，不属冻结 public 面。

### 未运行门

未运行 Maven/JUnit/compile/package；**不声称** compile exit 0。未启 runtime/input；零 Git mutation；两仓 dirty/untracked 原样。已停止编辑，交父级审查，不写 `APPROVED/CLOSED`，不自批。

<!-- TRUE_EOF: TURN-34BP2 EXTERNAL-C SOURCE DELIVERED SHA=d8ba531ed0cefbc250d243f835c19ec7b710dbf5bd3950e2427e188e68158762 2026-07-16T14:01:26.481-04:00 -->

## PARENT WHOLE-CARD SOURCE REVIEW #1 - REPAIR #1 REQUIRED - 2026-07-16T14:06:11-04:00

- Verdict: `P0/P1/P2=0/1/0 / REPAIR #1 REQUIRED`. The complete existing TURN-34BP2 card returns to the same
  External C Worker. This is not a fragment, tranche or new subcard; C retains responsibility for the complete
  production file, report and every later whole-card repair until parent source pass or `OWNER RETURNED`.
- Accepted and frozen: the delivered file is exactly 1,353 lines / SHA-256
  `d8ba531ed0cefbc250d243f835c19ec7b710dbf5bd3950e2427e188e68158762`; all 19 public method declarations remain;
  the four target shared maps now use the required private typed keys; the four per-window BP3 maps remain String
  keyed; no delimiter parsing, compatibility lookup, new command/action/UUID/retry/TTL/session/ledger/workflow or
  approved business difference was found.
- **P1 - a real context can still be downgraded to the shared no-context key.** The frozen contract at lines 47-49
  and the Worker delivery claim at lines 127-129 both require that only `context == null` plus an empty holder may
  produce `ExecutionScope.NONE`; any supplied/effective context authority failure must propagate rather than merge
  state into an unscoped key. In delivered `TaskMaintenanceService.java:1234-1244`, `executionScope(...)` correctly
  returns `NONE` when `effective == null`, but then also returns the same `NONE` when either
  `effective.getTurnServiceScope()` or `effective.getTurnInvocationContext()` is null. That second branch contradicts
  the exact invariant and makes the source-level isolation guarantee depend on today's constructor behavior instead
  of the typed key boundary itself.
- Required whole-card repair: keep every accepted map/key/public/business byte. Change the non-null effective-context
  path so missing scope or invocation authority fails closed and can never return `ExecutionScope.NONE`; the explicit
  no-context key remains reachable only from supplied-null plus empty holder. Update the nearby JavaDoc/delivery
  evidence to match the implemented invariant. Do not broaden this into BP3 `scopePrefix/currentWindowKey`, tests,
  callers or another file, and do not add retry/session/owner/lease/ledger/durable workflow behavior.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run because
  External D is an active TURN-28Q Java writer. Dirty/untracked files in both repositories remain protected.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34BP2 PARENT-WHOLE-CARD-SOURCE-REVIEW-1 REPAIR-1-REQUIRED P0-0-P1-1-P2-0 NONNULL-CONTEXT-MUST-NOT-DOWNGRADE-TO-EXECUTION-SCOPE-NONE RETURNED-TO-EXTERNAL-C SHA=d8ba531ed0cefbc250d243f835c19ec7b710dbf5bd3950e2427e188e68158762 2026-07-16T14:06:11-04:00 -->

## EXTERNAL-C WHOLE-CARD REPAIR #1 CLAIMED - 2026-07-16T14:11:07.410-04:00

WHOLE-CARD REPAIR #1 CLAIMED | card=TURN-34BP2 | lane=CR271-External-Worker-C | claimedAt=2026-07-16T14:11:07.410-04:00 | platformAgentId=<待父级平台真实 ID 校正>

- P1 属实：`executionScope(...)` 在 `effective != null` 但 `getTurnServiceScope()`/`getTurnInvocationContext()` 为 null 时**仍返回 `ExecutionScope.NONE`**，把真实 context 降级进共享 no-context key——**与冻结合同及我自己的交付文本直接矛盾**，且让隔离保证依赖构造器当下行为而非 typed key 边界本身。
- 起始 SHA（已复核）：`d8ba531ed0cefbc250d243f835c19ec7b710dbf5bd3950e2427e188e68158762`（1353 行）。
- 修复边界：仅改该非空 effective-context 分支为 fail-closed + 同步 JavaDoc/证据；保留全部已接受的 map/key/public/业务字节；**不**扩到 BP3 `scopePrefix`/`currentWindowKey`、测试、caller 或其它文件；不加 retry/session/owner/lease/ledger/durable workflow。

## EXTERNAL-C WHOLE-CARD REPAIR #1 SOURCE DELIVERED - 2026-07-16T14:12:02.962-04:00

WHOLE-CARD REPAIR #1 SOURCE DELIVERED | card=TURN-34BP2 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T14:12:02.962-04:00

| File | Lines | From SHA-256 | Delivered SHA-256 |
|---|---:|---|---|
| `TaskMaintenanceService.java` | 1353 → **1365** | `d8ba531e...` | **`d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219`** |

写集仅该 production 文件 + 本卡。

### P1 修复 — 真实 context 不再可能降级为共享 no-context key

父级指出属实：交付版 `executionScope(...)` 在 `effective != null` 但 `getTurnServiceScope()`/`getTurnInvocationContext()` 为 null 时**仍返回 `ExecutionScope.NONE`**，与冻结合同（仅 supplied-null + holder 空可得 NONE）及**我自己的交付文本**直接矛盾，并把隔离保证寄托在构造器当下行为上、而非 typed key 边界本身。

现改为 **fail-closed**：该分支 `throw new IllegalStateException("maintenance state requires the exact tenant/user/device scope of the bound context")`，沿既有路径传播、不 broad-catch、不降级。

**字节验证（非自述）**：`grep -n "ExecutionScope.NONE"` 全文件仅三处 —— `:1229` JavaDoc、`:1244` **唯一** `return`（位于 `effective == null` 分支内）、`:1326` 常量定义。即 NONE **只有 supplied-null + holder 空一条可达路径**。

### JavaDoc 与实现对齐

同步改写 `executionScope` 的 JavaDoc：明确 `NONE` 仅从 supplied-null + 空 holder 可达；任何 bound context 必须产出真实 scope，缺失即抛、抛出的 authority 失败沿既有路径传播，二者皆不被 broad-catch 或降级；并标注 `@throws IllegalStateException`。（这是我第 N 次被"文档/交付文本超前于字节"教训，故本轮先验字节再写交付。）

### 已接受面保持不变（已复核）

- `public` 方法数 **恰 19**；四个 typed map 声明（`ScopedTeamKey`/`TeamRoundKey`/`ScopedLocalSessionKey`/`MaintenanceClaimKey`）**4/4** 未动；BP3 四个 per-window String map（`Map<String,Long>`×3 + `Map<String,SummonSkillWindowState>`）**4/4** 未动。
- 未触碰 BP3 `scopePrefix`/`currentWindowKey`、测试、caller 或任何其它文件；未加 retry/session/owner/lease/ledger/durable workflow；业务序/优先级/开关集 `5/1/5/2`/临界区/原子二参 remove/对外日志文本均未变。

### 未运行门

未运行 Maven/JUnit/compile/package（External D 为活动 TURN-28Q Java writer）；**不声称** compile exit 0。未启 runtime/input；零 Git mutation；两仓 dirty/untracked 原样。已停止编辑，交父级审查，不写 `APPROVED/CLOSED`，不自批。

<!-- TRUE_EOF: TURN-34BP2 EXTERNAL-C WHOLE-CARD REPAIR #1 SOURCE DELIVERED SHA=d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219 2026-07-16T14:12:02.962-04:00 -->

## PARENT WHOLE-CARD SOURCE RE-REVIEW #2 - PASSED - 2026-07-16T14:12:30-04:00

- Verdict: `P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED`. External C's complete-card implementation owner is
  released. TURN-34BP2 now enters two independent whole-card reviewers plus the later stable-writer Cloud compile
  gate; this parent verdict is not CARD APPROVED.
- Parent independently recalculated 1,365 lines / SHA-256
  `d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219` and reviewed the delivered production
  file. `executionScope(...)` now has exactly one `return ExecutionScope.NONE`, inside the
  `effective == null` branch. A non-null effective context with missing scope or invocation authority throws
  `IllegalStateException`; accessor failures propagate. The Review #1 P1 is closed without broad catch or
  unscoped fallback.
- The accepted whole-card surface remains frozen: four required private typed shared maps, four unchanged BP3
  per-window String maps, 19 public methods, supplied-context precedence, explicit-session sharing/window isolation,
  typed formal-vs-local claim kinds, zero delimiter compatibility fallback, and unchanged business order/terminal
  behavior. No test or caller file was added to the write set.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run because
  External D remains an active TURN-28Q Java writer. Dirty/untracked files in both repositories remain protected.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34BP2 PARENT-WHOLE-CARD-SOURCE-REREVIEW-2 PASSED P0-0-P1-0-P2-0 SOURCE-REVIEW-PASSED OWNER-RELEASED DUAL-INDEPENDENT-REVIEW-AND-BUILD-PENDING SHA=d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219 2026-07-16T14:12:30-04:00 -->

## PARENT INDEPENDENT WHOLE-CARD REVIEW ASSIGNMENT - 2026-07-16T14:13:10-04:00

- R1 Hooke `019f6c22-b436-7b42-bdcf-8e5b9b121fcb` writes only
  `docs/superpowers/plans/reports/2026-07-16-turn-34bp2-repair1-independent-review-r1.md`.
- R2 Jason `019f6c22-c837-7a42-96bf-8959fcb01a53` writes only
  `docs/superpowers/plans/reports/2026-07-16-turn-34bp2-repair1-independent-review-r2.md`.
- Both review the same complete frozen production SHA `d97e1572...`; neither may edit Java, expand the card,
  run Maven/runtime/input or replace parent judgment. Any blocker returns the complete card to External C after
  parent adjudication; two latest APPROVED rounds still leave the stable-writer Cloud compile gate pending.

<!-- TRUE_EOF: TURN-34BP2 PARENT-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED R1-HOOKE-019f6c22-b436-7b42-bdcf-8e5b9b121fcb R2-JASON-019f6c22-c837-7a42-96bf-8959fcb01a53 FROZEN-SHA=d97e1572 DUAL-REVIEW-IN-PROGRESS 2026-07-16T14:13:10-04:00 -->

## PARENT INDEPENDENT-REVIEW ADJUDICATION #3 - REPAIR #2 REQUIRED - 2026-07-16T14:19:20-04:00

- R1 Hooke latest round is `APPROVED 0/0/0`; R2 Jason latest round is `BLOCKED 0/1/0`. Parent independently
  re-read the frozen card and production SHA
  `d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219` and upholds R2. Verdict:
  `P0/P1/P2=0/1/0 / REPAIR #2 REQUIRED`.
- **P1 - the formal coordination address still merges no-session windows.** The frozen contract requires the
  typed address to encode execution scope plus exactly one coordination discriminator: the explicit local-team
  session when present, otherwise the exact scoped window. Current `scopedTeamKey(...)` builds only
  `ScopedTeamKey(ExecutionScope, maintenanceKey)`. Consequently `activeTeamRoundByKey`,
  `teamMaintenanceWindowStateByRound` and `FormalTeamRoundClaimKey` share one namespace for two same-scope,
  same-maintenance-key windows that have no session. `ScopedWindowKey` distinguishes claim members but cannot
  isolate the containing formal claim limit; one standalone window can consume another's formal slot.
- The complete existing TURN-34BP2 card returns to the same External C Worker for whole-card Repair #2. This is
  not a fragment or new subcard. Use one private typed coordination discriminator after `ExecutionScope`: scoped
  explicit session when present, otherwise exact scoped window. Apply it consistently to active round, maintenance
  window state, formal round claim, pruning and all reads/writes. Prove the four frozen combinations: same scope +
  same explicit session shares; same scope + no session + different windows isolates; different scope never shares;
  formal and local claim kinds cannot collide.
- Preserve the accepted four typed maps, four BP3 per-window maps, 19 public signatures, supplied-context precedence,
  fail-closed authority path, maintenance-key fallback, business order/terminal behavior, and zero delimiter parse,
  compatibility lookup, command/action/UUID/retry/session-authority/owner/lease/ledger/TTL/queue/durable-workflow
  expansion. Any new delivery restarts parent review and both independent reviewers against the latest whole-card SHA.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34BP2 PARENT-INDEPENDENT-REVIEW-ADJUDICATION-3 REPAIR-2-REQUIRED P0-0-P1-1-P2-0 R1-APPROVED R2-BLOCKED NO-SESSION-FORMAL-WINDOW-ISOLATION-MISSING WHOLE-CARD-RETURNED-TO-EXTERNAL-C SHA=d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219 2026-07-16T14:19:20-04:00 -->

## EXTERNAL-C WHOLE-CARD REPAIR #2 CLAIMED - 2026-07-16T14:26:19.067-04:00

WHOLE-CARD REPAIR #2 CLAIMED | card=TURN-34BP2 | lane=CR271-External-Worker-C | claimedAt=2026-07-16T14:26:19.067-04:00 | platformAgentId=<待父级平台真实 ID 校正>

- R2 Jason 的 P1 属实：`ScopedTeamKey(ExecutionScope, maintenanceKey)` 只编码了 scope 与 maintenance key，**缺协调判别子**。故同 scope + 同 maintenance key 且**无 session** 的两个窗口在 `activeTeamRoundByKey`/`teamMaintenanceWindowStateByRound`/`FormalTeamRoundClaimKey` 上共用同一命名空间——`ScopedWindowKey` 只区分 claim 成员，无法隔离**外层 formal claim 名额**，一个独立窗口可消耗另一个的 formal slot，违反卡的"without a session, windows isolate"。
- 起始 SHA（已复核）：`d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219`（1365 行）。
- 修复：在 `ExecutionScope` 之后引入**单一 private typed 协调判别子**（有显式 session 用 scoped session，否则用 exact scoped window），并一致应用于 active round、maintenance window state、formal round claim、prune 及全部读写。
- 保留已接受面：四个 typed map、四个 BP3 per-window map、19 个 public 签名、supplied-context 优先、fail-closed authority、maintenance-key fallback、业务序/terminal 行为；零 delimiter 解析/兼容查找/command/action/UUID/retry/session-authority/owner/lease/ledger/TTL/queue/durable-workflow 扩张。
- 写集仅该 production 文件 + 本卡；不运行 Maven；零 Git mutation。

## EXTERNAL-C WHOLE-CARD REPAIR #2 SOURCE DELIVERED - 2026-07-16T14:27:15.539-04:00

WHOLE-CARD REPAIR #2 SOURCE DELIVERED | card=TURN-34BP2 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T14:27:15.539-04:00

| File | Lines | From SHA-256 | Delivered SHA-256 |
|---|---:|---|---|
| `TaskMaintenanceService.java` | 1365 → **1400** | `d97e1572...` | **`8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`** |

写集仅该 production 文件 + 本卡。

### P1 修复 — formal 协调地址不再合并 no-session 窗口

R2 Jason 指出属实：`ScopedTeamKey(ExecutionScope, maintenanceKey)` 缺协调判别子，导致同 scope + 同 maintenance key 且无 session 的两个窗口在 `activeTeamRoundByKey`/`teamMaintenanceWindowStateByRound`/`FormalTeamRoundClaimKey` 上共用命名空间；`ScopedWindowKey` 只区分 claim 成员，挡不住**外层 formal claim 名额**被别的独立窗口吃掉。

现引入**单一 private typed 协调判别子**并置于 `ExecutionScope` 之后：

- `sealed interface TeamCoordination permits SessionCoordination(String sessionKey), WindowCoordination(String windowId)`
- `ScopedTeamKey(ExecutionScope scope, TeamCoordination coordination, String maintenanceKey)`
- `teamCoordination(context)`：`scopedLocalSessionKey(context) != null` → `SessionCoordination(session.sessionKey())`；否则 → `WindowCoordination(scopedWindowKey(context).windowId())`。**恰一个判别子**，无双查/回退。

因 `ScopedTeamKey` 的**唯一构造点**是 `scopedTeamKey(...)`（字节复核：`new ScopedTeamKey(` 仅 `:1261` 一处），该判别子**自动一致地**传导到 active round、maintenance window state、`FormalTeamRoundClaimKey`（其内嵌 `TeamRoundKey.team()`）、prune（字段比较 `team().equals(teamKey)`）与全部读写——无需逐点改写，也不存在漏改点。

### 四个冻结组合（由类型结构保证）

1. **同 scope + 同显式 session → 共享**：两窗口的 `scopedLocalSessionKey` 相等 → 同 `SessionCoordination` → 同 `ScopedTeamKey`。
2. **同 scope + 无 session + 不同窗口 → 隔离**：各自 `WindowCoordination(windowId)` 不等 → `ScopedTeamKey` 不等 → round/window state/formal claim 三者全隔离。
3. **不同 scope → 永不共享**：`ExecutionScope(tenant,user,device)` 为 `ScopedTeamKey` 首字段，不等即不等。
4. **formal 与 local claim 不可碰撞**：sealed `MaintenanceClaimKey` 的 `FormalTeamRoundClaimKey` 与 `LocalSessionCapabilityClaimKey` 是不同 record 类型，`equals` 跨类型恒 false。

### 已接受面保持（字节复核）

`public` 恰 **19**；四个 typed map **4/4**、四个 BP3 per-window String map **4/4** 未动；`return ExecutionScope.NONE` 仍**仅 1 处**（supplied-null + 空 holder），fail-closed authority 路径保持；`normalizeTeamKey` fallback 序、supplied-context 优先、业务序/terminal 行为、临界区与原子二参 remove 均未变；`+ "#" +`／`"local-team:"`／`teamRoundKey(` 残留 **零**；未新增 delimiter 解析/兼容查找/command/action/UUID/retry/session-authority/owner/lease/ledger/TTL/queue/durable workflow。

### 未运行门

未运行 Maven/JUnit/compile/package；**不声称** compile exit 0。零 Git mutation；两仓 dirty/untracked 原样。已停止编辑；本次新交付将重启父级审查与两名独立 reviewer（对齐最新整卡 SHA）。不写 `APPROVED/CLOSED`，不自批。

<!-- TRUE_EOF: TURN-34BP2 EXTERNAL-C WHOLE-CARD REPAIR #2 SOURCE DELIVERED SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101 2026-07-16T14:27:15.539-04:00 -->

## PARENT WHOLE-CARD SOURCE RE-REVIEW #4 - PASSED - 2026-07-16T14:29:00-04:00

- Verdict: `P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED`. External C's complete-card implementation owner is
  released. TURN-34BP2 now waits for two fresh independent whole-card reviewers against the latest SHA and the
  later stable-writer Cloud compile gate; this verdict is not `CARD APPROVED`.
- Parent independently recalculated 1,400 lines / SHA-256
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101` and read the complete production file.
  `ScopedTeamKey` now contains `(ExecutionScope, TeamCoordination, maintenanceKey)`, with a sealed
  `SessionCoordination`/`WindowCoordination` discriminator. Its only construction path is `scopedTeamKey(...)`;
  explicit local-team session wins, otherwise the exact scoped window is used.
- Because `activeTeamRoundByKey`, `teamMaintenanceWindowStateByRound`, `TeamRoundKey`,
  `FormalTeamRoundClaimKey` and `pruneOlderTeamRoundClaims(...)` all carry/equality-compare that same
  `ScopedTeamKey`, the Review #3 P1 is closed consistently: same scope + same explicit session shares; same scope +
  no session + different windows isolates; different scope never shares; formal/local claim record kinds cannot
  collide. There is no dual lookup, text parse or compatibility fallback.
- The accepted complete-card surface remains frozen: four typed shared maps, four untouched BP3 per-window String
  maps, exactly 19 public methods, supplied-context precedence, the single no-context `ExecutionScope.NONE` path,
  fail-closed authority, maintenance-key fallback, and existing business order/terminal behavior. Existing baseline
  cache TTL/retry bytes are unchanged; Repair #2 adds no command/action/UUID/retry/session-authority/owner/lease/
  ledger/TTL/queue/durable-workflow behavior.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run. TURN-28Q
  reviewers occupy Internal `2/2`; BP2's fresh dual review starts when those whole-card reviewers close.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34BP2 PARENT-WHOLE-CARD-SOURCE-REREVIEW-4 PASSED P0-0-P1-0-P2-0 SOURCE-REVIEW-PASSED OWNER-RELEASED FRESH-DUAL-INDEPENDENT-REVIEW-AND-BUILD-PENDING SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101 2026-07-16T14:29:00-04:00 -->

## PARENT FRESH INDEPENDENT WHOLE-CARD REVIEW ASSIGNMENT - 2026-07-16T14:30:45-04:00

- R1 Rawls `019f6c31-9411-74a1-b81b-911626bed1a6` writes only
  `docs/superpowers/plans/reports/2026-07-16-turn-34bp2-repair2-independent-review-r1.md`.
- R2 Galileo `019f6c31-db0e-7c93-9509-cc538010f312` writes only
  `docs/superpowers/plans/reports/2026-07-16-turn-34bp2-repair2-independent-review-r2.md`.
- Both independently review the same complete latest-SHA whole card `8d79d198...`; neither may edit Java, expand
  the contract, run Maven/runtime/input or replace parent judgment. Any blocker returns the complete card to C
  after parent adjudication; two latest APPROVED rounds still leave stable-writer Cloud compile pending.

<!-- TRUE_EOF: TURN-34BP2 PARENT-FRESH-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED R1-RAWLS-019f6c31-9411-74a1-b81b-911626bed1a6 R2-GALILEO-019f6c31-db0e-7c93-9509-cc538010f312 SHA=8d79d198 DUAL-REVIEW-IN-PROGRESS 2026-07-16T14:30:45-04:00 -->

## PARENT FRESH DUAL INDEPENDENT WHOLE-CARD REVIEW GATE - PASSED 2/2 - 2026-07-16T14:35:53-04:00

- R1 Rawls latest canonical round is `APPROVED P0/P1/P2=0/0/0`; R2 Galileo latest canonical round is
  `APPROVED P0/P1/P2=0/0/0`. Parent independently read both reports through their physical true EOF and confirmed
  that both reviewed the same complete frozen production: 1,400 lines / SHA-256
  `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`.
- Dual independent whole-card review is therefore `PASSED 2/2`. Both Internal reviewer sessions are closed and
  Internal returns to `0/2`. No implementation owner is active for this card.
- TURN-34BP2 remains `BUILD PENDING`, not `CARD APPROVED`, until the stable-writer Cloud compile gate succeeds.
  No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run in this
  review-closing pass.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-34BP2 REPAIR-2 FRESH-DUAL-INDEPENDENT-WHOLE-CARD-REVIEW PASSED-2-OF-2 R1-RAWLS-APPROVED-P0-0-P1-0-P2-0 R2-GALILEO-APPROVED-P0-0-P1-0-P2-0 SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101 INTERNAL-0-OF-2 CLOUD-COMPILE-PENDING NOT-CARD-APPROVED 2026-07-16T14:35:53-04:00 -->

## PARENT STABLE-WRITER CLOUD COMPILE GATE #1 - BLOCKED - 2026-07-16T14:40:21-04:00

- Cloud Maven main compilation exited 1 before any named test. Representative failures are incomplete whole-card
  migration owners: `WubeiTask`, `NavigationService`, `NpcClickService`, `DialogService`, and
  `PlayerStateService` still reference DHXY-only collaborators absent from Cloud.
- The compiler did not identify `TaskMaintenanceService.java` as a failing source. The blocker is outside
  TURN-34BP2's accepted frozen one-file write set; BP2 is not returned for source repair and remains
  `SOURCE REVIEW PASSED / DUAL REVIEW PASSED 2/2 / CLOUD COMPILE BLOCKED / NOT CARD APPROVED`.
- No runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-34BP2 PARENT-STABLE-WRITER-CLOUD-COMPILE-GATE-1 MAIN-COMPILE-BLOCKED-EXIT-1 BLOCKER-OUTSIDE-BP2-WRITE-SET OWNED-BY-PLANNED-WHOLE-CARD-PREREQUISITES NO-BP2-SOURCE-REPAIR NOT-CARD-APPROVED 2026-07-16T14:40:21-04:00 -->
