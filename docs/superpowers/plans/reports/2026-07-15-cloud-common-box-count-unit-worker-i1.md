# Cloud CommonBox Count Unit Worker I1

## CLAIMED

- task: `W-COUNT-COMMON-BOX-CONSUME-1`
- role: Internal Count Worker I1, implementation only; not a reviewer
- claimedAt: `2026-07-15T00:29:27.1578175-04:00`
- countUnit: `CommonBoxService::consumePendingBoxIfAllowed`
- countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- baseline Service blob: `195c1dbfef052ddaf87ff40c6c85cba862be91f6`
- pre-edit Cloud Service blob: `aa97b23d190e37d99069907c2b3a3338fcc1533c`
- branches: DHXY `thin-client-design`; Cloud `navigation-migration`
- gate: only parent source review plus the parent's unified DHXY/Cloud build may apply the count delta

## Baseline Method Map

| `696a12b0` method | Count-unit disposition |
|---|---|
| `detectLeaderBoxAfterReturnHome` | Public leader caller remains unchanged and reaches the same role-specific detection path. |
| `detectMemberBoxAfterCombatExit` | Public member caller remains unchanged and reaches the same role-specific detection path. |
| `consumePendingBoxIfAllowed` | Count unit: preserve stop, task/run/window/role/toggle gates; 30-second TTL; stale window/identity/taskRun removal; one atomic move/sleep/click; success clears and failure retains pending. |
| `hasPendingBoxForCurrentWindow` | Preserve the read-only mirror of the consume eligibility gates. |
| `clearPendingForRole` | Preserve null no-op and role-only pending cleanup. |
| `detectBox` / `detectAndRecord` | Preserve task/role/toggle order and typed one-shot exact-window observation; only `MATCHED` creates pending. |
| `cachedTemplate` | Remains DHXY-local inside the CommonBox observation mechanics. |
| `roleFor` / `isRoleEnabled` / `normalizeSupportedTask` | Remain Cloud business gates with the baseline accepted values/default switches. |
| `pendingKey` / `taskRunKey` / `pruneExpiredPending` / `sameWindow` | Remain Cloud pending identity and 30-second lifecycle authority. |

## Pre-Edit Scope

- Cloud: `src/main/java/com/bot/dhxy/service/CommonBoxService.java` plus CommonBox-only typed contract, port, and assembly files.
- DHXY: existing `service/commonbox/CommonBoxLocalObservationMechanics.java`, `RemoteCommonBoxFact.java`, and the `COMMON_BOX` handler branch are the retained exact-window observation terminal; existing typed `INPUT_BUNDLE` remains the sole serialized physical-input terminal.
- Frozen: generic LocalMacro shared 12, all prohibited Services named in the task, runtime/application/server/Task/poller/UI/capture/input/tests, and every unrelated dirty/untracked file.
- Build: deferred to the parent because shared Java writers are active.

## Implementation

### File Table

| Repository | File | Action | Role in the countable chain |
|---|---|---|---|
| Cloud | `src/main/java/com/bot/dhxy/service/CommonBoxService.java` | Modify | Keeps the complete 696 business method graph and delegates only observation/input mechanics to the dedicated port. |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPort.java` | New | CommonBox-only typed boundary for one exact-window observation and one atomic consume click. |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java` | New | Binds the dedicated port directly to existing typed `COMMON_BOX` and `INPUT_BUNDLE` transport; no LocalMacro registration. |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CommonBoxObservationResult.java` | New | Closed five-mechanics-state plus three-transport-terminal observation result; only `MATCHED` carries the screen-absolute point/score/local match time. |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CommonBoxClickResult.java` | New | Closed `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` consume-click terminal. |
| DHXY | `src/main/java/com/bot/dhxy/service/commonbox/CommonBoxLocalObservationMechanics.java` | Reused unchanged | Existing exact-binding ROI/template mechanics; SHA-256 `7E9F09084495DFA71D83C516EC321E11B77890E902780148626E90A6C540DAFD`. |
| DHXY | `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommonBoxFact.java` | Reused unchanged | Existing strict screen-absolute `MATCHED`/negative wire fact. |
| DHXY | `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java` | CommonBox branches reused unchanged | Existing `COMMON_BOX -> exact binding -> mechanics -> RemoteCommonBoxFact` branch and typed input-bundle terminal. Shared writer continued elsewhere; I1 did not edit this file. |
| DHXY | `docs/superpowers/plans/reports/2026-07-15-cloud-common-box-count-unit-worker-i1.md` | New | This worker-only implementation report. |

No DHXY Java edit was needed or made: the CommonBox-specific observation mechanics/handler branch and the sole
serialized input terminal already existed and were caller-reachable. Adding a duplicate local input path would have
violated the single-queue and no-wrapper rules.

### Complete Chain Evidence

1. Public callers remain real and unchanged:
   - `AutoCombatService.runPendingMemberCommonBoxIfAllowed` checks pending before the first-aid gate, acquires the
     task turn, and calls `consumePendingBoxIfAllowed`.
   - `AutoBattleTask.tryRunLocalTeamReturnRelease` calls the same public method before return-team input when the
     `COMMON_BOX` capability is open.
2. Cloud `CommonBoxService.consumePendingBoxIfAllowed` still performs the 696 order: stop checkpoint -> prune ->
   supported task -> taskRun -> window -> role -> role switch/clear -> pending lookup -> 30-second TTL and stale
   window/identity/taskRun gates -> one click attempt -> success-only pending removal. `NOT_EXECUTED` retains pending
   until TTL; `STOPPED/UNKNOWN` are not folded into click failure or retry.
3. `CloudCommonBoxPortAssembly.click` creates exactly one ordered screen-absolute bundle:
   `MOVE_MOUSE(x,y) -> SLEEP(80ms) -> CLICK_LEFT(x,y,120ms)`. It calls the current task context's
   `CloudGameClient.executeInputBundle` once and returns one closed CommonBox terminal. There is no queue-in-queue,
   auto-retry, fallback, or second input owner.
4. The existing DHXY `EXECUTE_INPUT_BUNDLE` handler revalidates the current registration/binding, submits the whole
   list once to the single `InputActionQueue`, and returns the typed input outcome. That outcome maps back through
   the assembly to the Service boolean and success/failure pending rule.
5. Detection remains the producer for this consume unit: the leader/member public milestones reach
   `CloudCommonBoxPortAssembly.observe -> CloudGameClient.readWindowFact(COMMON_BOX) -> DHXY COMMON_BOX handler ->
   CommonBoxLocalObservationMechanics.observe(exact binding) -> RemoteCommonBoxFact -> CommonBoxObservationResult`.
   Only `MATCHED` records pending, using DHXY's real `matchedAtEpochMs`; ROI `623,590-682,618`, threshold `0.86`,
   window/run/role isolation and screen-absolute point conversion remain unchanged.

### Business Parity

- TTL remains exactly `30_000ms`, anchored at the DHXY-local match time.
- Pending key and stale gates remain window id + native handle + role + task + taskRun, plus identity epoch validation.
- Leader/member switches, defaults, clear behavior and independence remain in Cloud business authority.
- Box consumption remains ahead of first-aid/return-team maintenance at the existing callers.
- Click success clears pending; trusted non-execution retains it; no new cleanup, retry, TTL, verification, park,
  yield, fallback, cloud gate, or phase decision was added.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Scoped Verification

- Read-only/source checks only; no Maven, javac, tests, runtime, application, server, Task, poller, UI, capture, or
  input execution was run.
- `git diff --no-index --check` for the baseline-vs-Cloud Service and every New file has zero whitespace errors;
  exit `1` is the expected no-index difference status, with only repository LF/CRLF warnings.
- Delimiter checks are balanced for all five authored Cloud Java files.
- Generic LocalMacro shared 12 and all frozen Services were not edited by I1.
- Final Cloud SHA-256:
  - `CommonBoxService.java`: `5F3FFB1E8DED18035220B7A216DC845AF36E893FB62DC851775EC76D339D1F5B`
  - `CloudCommonBoxPort.java`: `1F4598BF0230B0A96F5EA0E185D4952BF58095DC9901EBAC40C1AFEE5D671B27`
  - `CloudCommonBoxPortAssembly.java`: `B9AE9555E5CA562CFCFD29BFF7F8BA81E97E6AF0C85F005007202A3B61F059FC`
  - `CommonBoxObservationResult.java`: `3F30C8D55D7577FEB48DE128D010113DA0B10FFC0172C77B87214B91C1AE4E4E`
  - `CommonBoxClickResult.java`: `5A95F34F159425106046B535BFE40A624B50AA8E914D626F01A37F9024672B8B`

## Handoff Gate

Implementation is delivered for parent review only. `countDelta=+1` has **not** been applied by I1. The unit counts
only after parent source review and the parent's unified DHXY compile plus Cloud clean package both pass.

**待父级统一 build；父级源码审查与统一双构建通过前不真正计数。**

## Parent Source Review #1 - SOURCE APPROVED / COUNT PENDING BUILD - 2026-07-15T00:42:00-04:00

父级独立对照 `migration-baseline/696a12b0` 的完整 `CommonBoxService` 与 active Cloud 文件，并逐项追到真实
`AutoCombatService` / `AutoBattleTask` callers、Cloud `CloudCommonBoxPortAssembly`、DHXY `COMMON_BOX`
handler / `CommonBoxLocalObservationMechanics` 以及既有 `EXECUTE_INPUT_BUNDLE` 单输入队列。

- baseline 五个 public API 与 private pending/state 图均保留；没有 missing/added public business method。
- `30_000ms` TTL 继续锚定 DHXY 本地真实 `matchedAtEpochMs`；window/native handle/identity epoch/taskRun/role
  陈旧闸、role switch clear、miss/failure 保留 pending、仅成功点击清 pending均未改变。
- observe 仍只做 exact-window `623,590-682,618`、`0.86` 模板机械事实；Cloud 只消费 typed fact，不接管本地
  capture/template。consume 仍是单个 screen-absolute `MOVE_MOUSE -> SLEEP(80ms) -> CLICK_LEFT(120ms)` bundle，
  DHXY 既有 handler 只向唯一 `InputActionQueue` 提交一次，不存在 queue-in-queue。
- `MATCHED` 才携 click/score/local timestamp；机械失败、未执行、stop/unknown 均为 closed terminal，未伪装成成功，
  未新增 retry、TTL、owner/session/ledger 或业务 fallback。

结论：**P0=0 / P1=0 / P2=0，SOURCE APPROVED。** `countUnit=CommonBoxService::consumePendingBoxIfAllowed`
已满足源码门；因 B 与其它 Java writers 仍在写入，本轮禁止并发构建，状态为 `COUNT PENDING BUILD`。统一
DHXY compile + Cloud clean package 通过当轮才可把 ledger `before -> before+1`；构建失败则退回原 I1 修复且不计数。

## Implementation Repair #1 - Production Host Bean Reachability - 2026-07-15

### Repair Trigger

父级 Source Review #1 被 H2/H3 后续生产 host 证据推翻：`CloudServiceHost.create` 只注册
`CloudServiceConfiguration`，而该配置原先只扫描 `com.bot.dhxy.service`。因此被扫描的
`CommonBoxService` 无法从同一生产 context 获得位于 `com.yueyunfe.dhxy.cloudbrain.remote` 的唯一
`CloudCommonBoxPortAssembly`，也无法获得位于 `com.bot.dhxy.config` 的 `BotProperties`。

### Narrow Repair

| Repository | File | Repair |
|---|---|---|
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java` | 精确 `@Import` 现有 `BotProperties` 与唯一 `CloudCommonBoxPortAssembly`，使二者进入 `CloudServiceHost` 注册的同一个 context。 |

没有扩大 component scan，没有注册 `CloudCommonBoxProperties` / `CommonBoxStateGovernor`，没有新增第二个
pending、role switch、TTL、governor 或状态 owner。`CommonBoxService`、`AutoCombatService`、`AutoBattleTask`
及全部 DHXY Java 均保持冻结；CommonBox 的判断、30 秒 TTL、pending、角色开关、优先级、窗口/run 绑定、
点击/清理/fallback 顺序均未修改。

### Reachable Chain And Disclosure

- 本 count unit 只以 active Cloud 当前真实存在的 member 链为 caller 证据：
  `AutoBattleTask -> AutoCombatService.handleCombatTick -> detectMemberBoxAfterCombatExit -> CommonBoxService`
  记录 pending，随后由 `AutoCombatService.runPendingMemberCommonBoxIfAllowed` 或
  `AutoBattleTask.tryRunLocalTeamReturnRelease` 调用 `consumePendingBoxIfAllowed`，再经现在可由生产 host 注入的
  `CloudCommonBoxPortAssembly -> typed COMMON_BOX / INPUT_BUNDLE -> DHXY mechanics/handler -> closed result`。
- active Cloud 中 `detectLeaderBoxAfterReturnHome` 仍没有真实 Xiuluo/Wubei leader caller。该 leader 路径为
  **P2 disclosure**，本 Repair 不伪造 caller，也不宣称 leader 链完成；member 真链足以作为本 count unit 的父级
  复审候选。

### Repair Gate

- `countUnit=CommonBoxService::consumePendingBoxIfAllowed`
- `countDelta=+1` 仍未由 I1 应用；当前 ledger 不变。
- 按指令未运行 Maven、test、runtime、application/server、Task/poller、UI/capture/input，也未做任何 Git mutation。
- 仅完成 scoped source/diff/check；等待父级重新源码审查与统一 fresh DHXY compile + Cloud package。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

**待父级统一 build；父级源码审查与统一双构建通过前不真正计数。**

## Parent Source Review #2 - SUPERSEDES #1 / BLOCKED - 2026-07-15T00:54:19-04:00

新的生产 host 证据推翻上一轮 SOURCE APPROVED；历史结论保留但不再有效。

- **P1=1：Cloud CommonBox assembly 在生产 Spring 图中不可达。** `CloudServiceHost` 只注册
  `CloudServiceConfiguration`，该配置只扫描 `com.bot.dhxy.service`；现有
  `CloudCommonBoxPortAssembly` 位于 `com.yueyunfe.dhxy.cloudbrain.remote`，不会自动成为 bean。
  同时 `CommonBoxService` 所需 `BotProperties` 位于 `com.bot.dhxy.config`，当前配置也没有来源清晰的
  bean 声明。影响是 host 创建时依赖图不能闭合，真实 member caller 无法执行本 count unit。
- **P2=1（披露，不阻断本 count unit）：** active Cloud 有 `AutoBattleTask` / `AutoCombatService` 的 member
  caller，但没有基线 leader task caller；不得再宣称 leader/member 两条都已闭合。本计数单只验收已有真实 member
  caller -> `consumePendingBoxIfAllowed` 链。

结论：**P0=0 / P1=1 / P2=1，BLOCKED；COUNT NOT ELIGIBLE。** 已把同一 I1 恢复为
`Implementation Repair #1`：只在现有 Cloud host/config 与 CommonBox assembly 注册面做窄修，复用唯一
`BotProperties`，禁止新增第二状态 governor/owner、TTL、retry 或业务判断。修复后交回父级复审；统一构建前 ledger
仍为 `189/407`。

## Implementation Repair #1 Delivery - 2026-07-15T00:58:05-04:00

- Review #2 的 P1 host bean 缺口已按窄写集修复：`CloudServiceConfiguration` 精确
  `@Import({BotProperties.class, CloudCommonBoxPortAssembly.class})`；生产 host 仍只注册该配置入口。
- 未扩大 package scan，未注册 `CloudCommonBoxProperties` / `CommonBoxStateGovernor`，未修改任何业务判断、
  TTL、pending、角色开关、优先级、窗口/run 绑定、点击/清理/fallback 顺序。
- Cloud 配置文件当前 SHA-256：
  `C8FD7C3B3FD166C6E356219DB0CA608D3B5AF4B4EAED102C77BEC401BE318769`；
  `CommonBoxService` 与 assembly 哈希仍分别为
  `5F3FFB1E8DED18035220B7A216DC845AF36E893FB62DC851775EC76D339D1F5B`、
  `B9AE9555E5CA562CFCFD29BFF7F8BA81E97E6AF0C85F005007202A3B61F059FC`。
- leader caller 缺失继续作为 **P2 disclosure** 保留，不宣称完成；本计数候选只基于真实 member caller 链。
- I1 不作 reviewer 结论、不应用 `countDelta=+1`；待父级重新源码审查和统一 fresh 双构建。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。待父级统一 build。**

## Parent Source Review #3 - REPAIR SOURCE APPROVED / COUNT PENDING BUILD - 2026-07-15T01:00:00-04:00

父级独立复核 `CloudServiceHost.create -> CloudServiceConfiguration` 生产图与 Repair diff。配置只增加
`@Import({BotProperties.class, CloudCommonBoxPortAssembly.class})`：现有 mutable config bean 与唯一 stateless
assembly 进入同一 context；没有扩大 component scan、没有注册 dormant governor、没有第二 pending/state owner，
`CommonBoxService` 与真实 member caller/typed fact/input bundle 均未改。

结论：**P0=0 / P1=0 / P2=1，REPAIR SOURCE APPROVED。** P2 仅披露 active Cloud 缺 leader caller，
不阻断本 countUnit 的真实 member 链。`CommonBoxService::consumePendingBoxIfAllowed` 重新进入统一 fresh 双构建队列；
构建前 `countDelta` 仍未应用。
