# W-CBOX-DIRECT-IMP1 - Internal Worker AN

## CLAIMED

- task: `W-CBOX-DIRECT-IMP1`
- claimedAt: `2026-07-13T20:54:21.2098651-04:00`
- role: Internal Worker AN, implementation only; no review or approval authority
- baseline: committed DHXY `0114604e:src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- approach: preserve the baseline public API, role/task decisions, `CompletableFuture.runAsync` boundary, pending lifecycle, 30,000 ms TTL, logging semantics, and 80/120 move-and-click values; replace only local observation/input mechanics with the frozen typed Cloud contract

### Unique write set (final narrowed scope)

- New: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- Append-only: this report

### Explicitly frozen / not touched

- All Cloud `remote/**` files, including `WindowFactKind`, `WindowFact`, `WindowFactOutcome`, and `RemoteCommandOutcomeEnvelope`
- Existing `CloudCommonBoxProperties.java` and the obsolete governor/configRevision route
- All DHXY Java, schema, tests, host, caller, and Task files
- No Git mutation, runtime/application/Task/UI/capture/input startup, or Maven `clean`

### Frozen future contract consumed by this implementation

- `WindowFactKind.COMMON_BOX`
- `WindowFact.CommonBoxFact(state, clickX, clickY, matchScore, matchedAtEpochMs, CoordinateSpace.SCREEN_ABSOLUTE_PX)`
- `CommonBoxState`: `MATCHED`, `NOT_MATCHED`, `CAPTURE_UNAVAILABLE`, `TEMPLATE_UNAVAILABLE`, `MECHANICS_FAILED`
- `MATCHED` carries all four match fields; every negative state carries all four as null
- Constructor receives immutable per-run boolean snapshots: `leaderCommonBoxEnabled` and `memberCommonBoxEnabled`

## Internal Worker AN - W-CBOX-DIRECT-IMP1 Implementation #1

- deliveredAt: `2026-07-13T21:04:25.0430578-04:00`
- Java write set: exactly one New Cloud file,
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- report write set: this append-only file
- file evidence: `22068` bytes; SHA-256
  `6F02FBE2227C5500321A74E356A72D34F229A5BF010E98A67DF323BC8A9890D1`

### Baseline parity against committed `0114604e`

- Preserved all five public business methods:
  `detectLeaderBoxAfterReturnHome`, `detectMemberBoxAfterCombatExit`,
  `consumePendingBoxIfAllowed`, `hasPendingBoxForCurrentWindow`, and
  `clearPendingForRole`.
- Preserved the supported task allowlist (`xiuluo_v2` / `wubei`), LEADER/MEMBER
  role interpretation, role-toggle skips/clears, and detect role-mismatch cleanup order.
- Preserved the single `CompletableFuture.runAsync` detect boundary. The synchronous
  path performs only task/role/toggle/current-run validation; the asynchronous body performs one
  typed `COMMON_BOX` fact read.
- `NOT_MATCHED`, `CAPTURE_UNAVAILABLE`, `TEMPLATE_UNAVAILABLE`, and
  `MECHANICS_FAILED` keep their distinct log semantics and never clear or replace an existing
  pending record. Transport non-`OBSERVED`, wrong fact variant, interruption, and remote failure
  likewise record/exit without being folded into `NOT_MATCHED` and without automatic retry.
- `MATCHED` alone creates/replaces pending. Pending remains keyed by taskRun/window/role/task and
  validated by current window plus player-identity epoch, matching the committed business
  qualification.
- TTL is exactly `30_000 ms` and starts from the local observation's
  `matchedAtEpochMs`, not Cloud receipt time. Expiry uses
  `Math.addExact(matchedAtEpochMs, 30_000)`; overflow fails without creating pending, and an
  already-expired fact does not create a consumable pending.
- Pending deliberately does **not** bind `runRevision`. Each current
  `TaskExecutionContext` supplies its own revision-fenced `CloudGameClient`; therefore
  ACTIVE -> PAUSED -> ACTIVE within the same taskRun does not shorten the committed 30-second
  business pending window.
- Consume still submits exactly one existing `InputSequences.moveAndClickLeft` bundle with
  settle/delay values `80/120`. Only a successful terminal bundle removes pending; failed,
  stopped, or uncertain input keeps pending until the same TTL expires.
- Constructor consumes the two immutable per-run boolean snapshots directly. It neither reads nor
  modifies the obsolete `CloudCommonBoxProperties` / governor / configRevision route.

### Frozen contract / wire use

- Reads only `WindowFactKind.COMMON_BOX` and `WindowFact.CommonBoxFact`.
- Consumes `MATCHED` screen-absolute click coordinates and score; timestamp is used only for the
  committed pending clock. The Cloud record constructor remains the single wire authority for the
  MATCHED/negative null matrix and `SCREEN_ABSOLUTE_PX` constraint.
- No local capture/template/OCR classes, HWND, filesystem path, callback, or raw payload crossed
  into the Cloud business class.

### Parent incremental corrections closed before delivery

1. Removed the initial `staleRevision` pending invalidation from consume and read-only pending
   checks. Revision remains only in the current remote safety fence.
2. Replaced Cloud-receipt-time TTL with `matchedAtEpochMs + 30_000`, including overflow and
   already-expired fail-safe handling.

### Verification and scope

- Final command: Cloud `mvn -q compile` from `D:/mavenProject/dhxy-cloud-brain`, no `clean`.
- Final result: exit `0`; no compiler output under `-q`.
- No DHXY compile was required because this Worker changed no DHXY Java.
- No tests were created or run (repository no-local-test mode); no application/server/host/Task/UI,
  capture, or input was started.
- No Git add/commit/reset/checkout/clean and no deletion, overwrite, or cleanup of other dirty or
  untracked work.
- Existing Cloud remote fact files and all DHXY remote/handler/mechanics files were written by
  their assigned External Workers and were not touched by AN.

Self-QA only: `P0=0 / P1=0 / P2=0`; this is not parent review or approval.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - APPROVED / `W-CBOX-DIRECT-IMP1` - 2026-07-13T21:07:00-04:00

父级逐方法对照 committed `0114604e:CommonBoxService`：五个 public API、task/role/toggle 分支、
`CompletableFuture.runAsync` 边界、negative 不清 pending、MATCHED 单独写 pending、30,000 ms TTL、点击成功清除/
失败保留以及 move+click 的 80/120 参数均保持。机械替换仅为一次 typed `COMMON_BOX` fact 与一次现有
`InputSequences.moveAndClickLeft`；本地 capture/template/OCR/HWND 未进入 Cloud。

增量审查发现的两项偏差已在正式交付前关闭：pending 不再绑定 `runRevision`，每次远程调用仍由当前 context 做
revision safety fence；TTL 以 `matchedAtEpochMs + 30_000` 起算，溢出或到达 Cloud 时已过期不会形成可消费 pending。
构造器只接收 per-run 两个 boolean 快照，不依赖旧 `CloudCommonBoxProperties`/governor 路径。AN 的 Cloud
`mvn -q compile` 已 exit 0；最终 fresh package 等整波写入稳定。

结论：`W-CBOX-DIRECT-IMP1 SOURCE APPROVED`，`P0=0 / P1=0 / P2=0`。**无已批准业务差异；按
`0114604e` 基线等价迁移。**
