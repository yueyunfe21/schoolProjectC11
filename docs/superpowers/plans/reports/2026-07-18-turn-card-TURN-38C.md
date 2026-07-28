# CR271 TURN-38C Left-Top Turn-Native Context State Card

## 1. Status / Authority

- Status: `READY / ZERO OWNER / PARENT CONTRACT FROZEN`.
- Predecessor TURN-38M is frozen in `2026-07-15-turn-38-authority-state-classification.md`.
- This card is not assigned. Any eligible idle External Worker may perform the canonical whole-card anti-race claim at this file's physical EOF.
- Parent is the sole final reviewer. Worker does not self-approve and does not create a reviewer/sub-agent.

## 2. Fixed Whole-Card Write Set

Cloud repository only:

1. Modify `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`.
2. Create `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/LeftTopStatusSwitchTurnStateTest.java`.
3. Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/LeftTopStatusTurnContractTest.java`.
4. Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`.

All five TURN-38M old-authority files, `LeftTopStatusSwitchService`, all Task/Service callers, old assembly/context, future runtime/host files, protocol, POM and resources are read-only. Needing a fifth Java/test file requires `OWNER RETURNED / PLAN-CONTRACT BLOCKED`.

## 3. Protected Bytes At Freeze

- `TaskExecutionContext.java`: SHA-256 `A9C34D4E9BC960F35CA982F4D39EA8342323DC1D92F0AE1199B5677E59E2CB4E`, 22,204 bytes, 527 lines.
- `LeftTopStatusTurnContractTest.java`: SHA-256 `C9D0B21AEC3637452E0507F0F716C43E9F8CD21010368EBA249012BE3C66EF8A`, 48,776 bytes, 1,063 lines.
- `TaskExecutionContextTurnContractTest.java`: SHA-256 `3B117895CEF72AF5085E646D9FE76D8F4F648142F93A89E3DFA52EC4292B2785`, 43,936 bytes, 872 lines.
- `LeftTopStatusSwitchTurnStateTest.java`: `ABSENT`.
- No active Java owner or write-set collision exists at freeze. TURN-38B1/B2/B3/B4 are owner-released.

## 4. Production Contract

- The turn-native half of each concrete `TaskExecutionContext` owns exactly one private boolean pending state, initially false. It may use a private field or an equivalent context-local single-bit primitive; it must not introduce a new class/interface/store/provider.
- The existing four public APIs keep signatures and meanings: `isLeftTopStatusSwitchClosePending`, `mark...`, `consume...`, `clear...`. Legacy contexts continue delegating to `CloudTaskServiceExecutionContext` until TURN-44A; turn-native contexts operate only on the context-local bit.
- Multiple marks remain one bit. `consume` atomically returns/clears the prior bit. `clear` is idempotent. `source` is required only as existing diagnostic input and never becomes identity/history/retry/expiry state.
- One concrete Task context owns the state. Pause/resume keeps the same context and bit. Stop/terminal/exception releases it only by runtime dropping the context reference. A new concrete Task/new accepted start begins false. No terminal probe/click/clear, cleanup retry or background cleanup.
- Cross-window isolation comes from separate concrete contexts, never a static/window map. No session, revision, epoch, permit, handle, ledger, durable workflow, count, TTL, weak reference, ThreadLocal owner or reflection lookup.
- The context remains powerless for lifecycle and transport. This amendment authorizes only the already-existing four left-top APIs; no other business state may be added to `TaskExecutionContext` by analogy.

## 5. Baseline-Equivalent Behavior

- Preserve the `696a12b0` truth table frozen by TURN-38M: member startup OPEN marks, CLOSED clears, unresolved preserves; follower safe-window and leader/combat resolved success consume; unresolved or known input failure preserves and does not retry.
- A safe-window probe occurs regardless of current pending value. Pending is not a new click/probe gate.
- `LeftTopStatusSwitchService.LEFT_TOP_STATUS_TIMEOUT_MS=120000` remains command timeout, not state TTL.
- No extra capture, verification, retry, fallback, park/yield, cleanup click or phase-order change.
- Existing production consumers are read-only; startup caller reachability remains a separate TURN-40B assembly acceptance point and is not invented in this card.
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## 6. Test Contract

- New `LeftTopStatusSwitchTurnStateTest` uses real public turn-native context construction and covers initial false, mark/idempotent mark, cross-source consume, clear, A/B isolation, pause continuity, stop unwind without mutation, and fresh context reset. No reflection/source scan/private-helper direct call/sleep race/runtime/server/Task/capture/input.
- `LeftTopStatusTurnContractTest` replaces its old coordinator/assembly reflection fixture with a turn-native context fixture while preserving its mechanical/Service truth table, capture count, exact command order and no-retry assertions.
- `TaskExecutionContextTurnContractTest` removes only the obsolete assertion that the four APIs are unavailable on turn-native contexts, and adds direct exact semantics/isolation assertions. All other context authority and generation-safety assertions remain.
- TURN-40B later adds runtime lifecycle integration to its already-owned `CloudTurnTaskRuntimeContractTest`; this card does not edit that file.

Authorized named family after all Java writers are stable:

`mvn -q -Dtest=LeftTopStatusSwitchTurnStateTest,LeftTopStatusTurnContractTest,TaskExecutionContextTurnContractTest test`

## 7. Canonical Claim / Delivery

- Claim by appending `EXTERNAL-<lane> TURN-38C WHOLE-CARD CLAIMED`, all protected SHA/ABSENT evidence, capacity and collision scan to this physical EOF, then re-read to prove sole ownership before editing Java.
- Delivery is one SOURCE+TEST whole-card event for all four fixed files with final SHA/mtime/line/test counts, old-authority residue, context-state matrix, and `无已批准业务差异` statement.
- No Maven while another Java writer is active. No Git mutation and no runtime/application/server/Task/UI/capture/input.

<!-- TRUE_EOF: TURN-38C PARENT-CONTRACT-FROZEN READY ZERO-OWNER FOUR-FILE-WRITESET CONTEXT-LOCAL-SINGLE-BIT OLD-TARGET-READONLY-TO-44A NO-SESSION-NO-TTL-NO-SECOND-STORE NO-BUSINESS-DIFFERENCE 2026-07-18T09:05:00-04:00 -->

## EXTERNAL-C TURN-38C WHOLE-CARD CLAIMED - 2026-07-18T09:12:00-04:00

- claimant: `EXTERNAL-C`（idle since TURN-38B4 Review#3 0/0/0 PASSED/OWNER RELEASED，eligible）
- claim_basis: `PARENT-TURN38C-READY-0905`（READY / ZERO OWNER / PARENT CONTRACT FROZEN；不派卡，自行 anti-race claim）
- **预检取证（append 前独立调用）**：全卡 grep `CLAIMED` 仅 §7 规则行自身，零既有 claim section；physical EOF=父级 09:05 frozen-contract marker。
- **protected bytes 实测（claim 时逐文件重取，与 §3 冻结逐字一致）**：
  | 文件 | SHA-256 | bytes / lines |
  |------|---------|---------------|
  | `TaskExecutionContext.java` | `A9C34D4E9BC960F35CA982F4D39EA8342323DC1D92F0AE1199B5677E59E2CB4E` | 22,204 / 527 |
  | `LeftTopStatusTurnContractTest.java` | `C9D0B21AEC3637452E0507F0F716C43E9F8CD21010368EBA249012BE3C66EF8A` | 48,776 / 1,063 |
  | `TaskExecutionContextTurnContractTest.java` | `3B117895CEF72AF5085E646D9FE76D8F4F648142F93A89E3DFA52EC4292B2785` | 43,936 / 872 |
  | `LeftTopStatusSwitchTurnStateTest.java` | `ABSENT`（实测不存在） | — |
- **capacity/collision scan**：C 当前零在手卡（38B4 已 owner-released）；本写集四文件与 C 既往写集零交集；38B1-B4 owner 均已释放，无已知并行 Java writer 持有本写集（A `COMMUNICATION_STALE`、B/D offline）。
- 承诺：五个 TURN-38M old-authority 文件/`LeftTopStatusSwitchService`/全 caller/old assembly/protocol/POM 只读；需第五文件即 `OWNER RETURNED / PLAN-CONTRACT BLOCKED`；不自批、不建 reviewer；零 Git mutation；不运行 Maven（javac 单文件 parse 除外）/runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读。
- next: append 后立即回读 physical EOF 证 sole ownership，确认后才动 Java。

<!-- TRUE_EOF: TURN-38C EXTERNAL-C WHOLE-CARD CLAIMED PROTECTED-BYTES-VERIFIED ZERO-PRIOR-CLAIM AWAIT-EOF-REREAD OWNER-PENDING-SELF-VERIFY 2026-07-18T09:12:00-04:00 -->

## PARENT CLAIM AUDIT - ACCEPTED - 2026-07-18T09:16:00-04:00

- Parent re-read the full ledger, Section 16's 88 Sprint Task rows, all fixed-card physical EOFs and this original
  card physical EOF. External C's 09:12 whole-card claim is the sole canonical claim and is accepted.
- Claim-point evidence exactly matches the frozen contract: context `A9C34D4E...`, LeftTop test `C9D0B21A...`,
  context test `3B117895...`, and new state test `ABSENT`; no owner race or write-set collision exists.
- C ACKed `PARENT-TURN38C-READY-0905` and is now `SOURCE_ACTIVE / EXTERNAL-C SOLE OWNER`. Current context source
  has begun changing inside the fixed production path; the other three test paths were unchanged/absent at audit.
- No Maven while C is an active Java writer. No runtime/application/server/Task/UI/capture/input.

<!-- TRUE_EOF: TURN-38C PARENT-CLAIM-AUDIT ACCEPTED SOURCE-ACTIVE EXTERNAL-C-SOLE-OWNER CLAIM-POINT-BYTES-VERIFIED NO-COLLISION ACK=PARENT-TURN38C-READY-0905 NO-MAVEN-NO-RUNTIME 2026-07-18T09:16:00-04:00 -->

## EXTERNAL-C TURN-38C CANONICAL SOURCE+TEST WHOLE-CARD DELIVERY - 2026-07-18T09:26:00-04:00

四文件写集全部完成（父级 09:16 claim audit ACCEPTED 收悉），严格按 §4-6 合同实施。

### Production（`TaskExecutionContext.java`）
- turn-native 半侧新增**恰一个** private boolean `leftTopStatusSwitchClosePending`（初值 false）+ 专用 `leftTopPendingLock` guard（沿用本文件既有 synchronized+boolean 惯用法）；零新类/接口/store/provider。
- 四 public API 签名/含义不变：turn-native 分支操作 context-local bit（`is`=纯读；`mark`=置位（多次 mark 一 bit）；`consume`=同一 synchronized 段内原子返还前值并清零；`clear`=幂等清零）；legacy 分支逐字保留 `delegate.xxx(...)` 委托至 TURN-44A。`source` 仅诊断入参，不存储、不成 identity/history/retry/expiry。
- 无 session/revision/epoch/permit/handle/ledger/count/TTL/weak-ref/ThreadLocal/反射/static-window map；隔离=独立 concrete context；无 terminal probe/click/clear。类 javadoc 补一句 38C bit 说明+「禁止类推添加其它业务态」。

### Tests
- **Create `LeftTopStatusSwitchTurnStateTest`（8 @Test）**：全部经真实 public `TaskExecutionContext.turnNative(...)` 构造（command-free ScriptedMetadataPort：execute 即 AssertionError；metadata 先脚本后 steady）。八场景=①初始 false+读不变异 ②多源重复 mark 仍一 bit ③跨 source consume 原子返还+清零 ④clear 幂等 ⑤A/B concrete context 全隔离（mark/consume/clear 互不泄漏）⑥pause 连续性（脚本 pause→free，checkpoint 实际阻塞>0ms 后 bit 保留可 consume）⑦confirmed stop unwind 零 bit 变异（release 仅靠 runtime 弃引用）⑧fresh context 重置 false。四 API 全程零 execute+零 metadata read（powerless 证明）。零反射/source scan/私有直调/sleep race/runtime/capture/input。
- **`LeftTopStatusTurnContractTest`（9 @Test 不变）**：旧 coordinator/assembly **反射 fixture 全删**（legacyContext/construct/invokeNoArgs/rethrowFixtureCause/LegacyContextFixture+全部 RemoteTaskRun*/GameContext/reflect import）→ harness 直接 `turnNative(EXACT_BINDING, EXACT_WINDOW, …)` 真实公共构造；confirmed-stop 改 port `confirmStopRequested()`（exact-window metadata slot 置 stopRequested，身份逐字不变）替代旧 coordinator.stop；机械/Service truth table、capture 计数、精确命令序、no-retry、UUID 唯一性断言逐字保留。
- **`TaskExecutionContextTurnContractTest`（11 @Test 不变）**：仅删四 left-top `assertAuthorityUnavailable` 过时断言，原位替换为直接精确语义断言（初始 false/多 mark 一 bit/跨源 consume 原子/clear 幂等/第二 concrete context 隔离/全程零 port I/O）；其余 authority（getScope/revalidate/GameClient/epoch/revision）与 generation-safety 断言逐字保留。

### 写集 4/4 终版取证（append 前实取）
| 文件 | blob / SHA-256 / bytes / lines | @Test |
|------|-------------------------------|-------|
| `TaskExecutionContext.java`（Modify） | `5a4ebbdb` / `830E42C578F755FCC76CB46EA6D2081F54D47EF5C858B36C98D6EDAE1644044F` / 24,328 / 573 | — |
| `LeftTopStatusSwitchTurnStateTest.java`（Create） | `8ee96650` / `2E228F5D857ADF2319258B42740212B972084C0F42670204B0D01DF8B65C0498` / 14,866 / 294 | **8** |
| `LeftTopStatusTurnContractTest.java`（Modify） | `76afca4c` / `38F2002DD4C8A1401F7E46B7CCA59A607849461C4046ABC00490F7EFD0C5D26C` / 43,989 / 962 | 9 |
| `TaskExecutionContextTurnContractTest.java`（Modify） | `bf11cce9` / `4B7C4CC97C3502DBA8583855A02145B01B7A5B094A24FBB963F478111D627B9B` / 45,074 / 887 | 11 |

### 残留与状态矩阵
- old-authority 残留扫描：production 四 left-top API 的 `legacyDelegate(...)` 调用=**0**（legacy 分支为合同要求的 delegate 委托，非 turn-native 残留）；两 test 文件 `RemoteTaskRunCoordinator/setAccessible/Class.forName`=**0**。五个 TURN-38M old-authority 文件/`LeftTopStatusSwitchService`/全 caller 零字节未动。
- context-state 矩阵：初始 F→mark→T→mark→T→consume→返 T 置 F→consume→返 F→mark→clear→F→clear→F；A/B 独立 bit；pause 保持；stop 零变异；fresh=F。
- 四文件 javac 单文件 parse 零纯语法错（仅空 classpath 解析噪声）。授权命令 `mvn -q -Dtest=LeftTopStatusSwitchTurnStateTest,LeftTopStatusTurnContractTest,TaskExecutionContextTurnContractTest test` 留待父级 stable window（C 为 active writer 期间不运行 Maven）。

`无已批准业务差异；按 696a12b0 基线等价迁移`。零 Git mutation（hash-object 只读取证）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**whole-card SOURCE+TEST review**。

<!-- TRUE_EOF: TURN-38C EXTERNAL-C CANONICAL-DELIVERY CTX=5a4ebbdb STATE-TEST=8ee96650-8T LEFTTOP-TEST=76afca4c-9T CTX-TEST=bf11cce9-11T CONTEXT-LOCAL-SINGLE-BIT REFLECTION-FIXTURE-DELETED REQUEST-REVIEW OWNER-C NO-MAVEN 2026-07-18T09:26:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - PASSED - 2026-07-18T09:43:00-04:00

- Verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`.
- Parent reviewed all four delivered files against this card, TURN-38M, HTTPS-turn context/foundation rules,
  `docs/业务逻辑.md`, and baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`. Final bytes exactly match
  delivery: context SHA-256 `830E42C5...`/573L; new state test `2E228F5D...`/294L/8T; LeftTop contract
  `38F2002D...`/962L/9T; context contract `4B7C4CC9...`/887L/11T.
- Production finding: the turn-native path owns exactly one context-local boolean guarded by one private lock;
  mark/consume/clear are single-bit/idempotent/atomic, legacy calls still delegate, and `source` is not stored.
  There is no static/window map, session/revision/ledger/count/TTL, second store, lifecycle authority, terminal
  cleanup action, or change to another context API.
- Test finding: the new 8T suite covers initial false, idempotent mark/clear, cross-source atomic consume, A/B
  isolation, deterministic pause continuity, confirmed-stop unwind without mutation, and fresh-context reset via
  public turn-native construction. The 9T LeftTop suite uses the real public context fixture and preserves the
  frozen capture/order/no-retry/terminal matrix. The 11T context suite changes only the obsolete four-API
  unavailable assertion and retains the other authority/generation checks.
- Baseline finding: `LeftTopStatusSwitchService` and all callers remain byte-read-only; startup OPEN mark, CLOSED
  clear, unresolved preserve, resolved consume, safe-window probe regardless of pending, and known-failure no
  retry are unchanged. `无已批准业务差异；按 696a12b0 基线等价迁移`.
- Build gate: the authorized named command was attempted after C stopped writing, but Cloud main compile failed
  before JUnit on already-registered TURN-40B shared missing types, including `TextCandidateScanStatus`,
  `AutomationMetricsService`, `BagService`, `UICleanerService`, Navigation/window/input types,
  `CoordinateHelper`, and `TextRecognizer`. No compiler error points to the TURN-38C four-file write set. This
  does not reopen the source review; named-test execution and Cloud compile remain blocked by TURN-40B.
- External C owner is released. C must ACK message `PARENT-TURN38C-REVIEW1-PASSED-0943` on its next heartbeat,
  then return to available pool scanning; no repair is requested.

<!-- TRUE_EOF: TURN-38C PARENT-REVIEW1 PASSED P0P1P2-0-0-0 SOURCE-TEST-SOURCE-REVIEW-PASSED OWNER-RELEASED BUILD-BLOCKED-BY-TURN40B-SHARED-DEBT MSG=PARENT-TURN38C-REVIEW1-PASSED-0943 NO-BUSINESS-DIFFERENCE 2026-07-18T09:43:00-04:00 -->

## PARENT ACK AUDIT - CARD CLOSED - 2026-07-18T09:53:00-04:00

- External C's 09:48 ledger event explicitly ACKs `PARENT-TURN38C-REVIEW1-PASSED-0943`, accepts the
  `0/0/0` verdict/build note, and returns to `AVAILABLE / IDLE POOL-SCAN`.
- TURN-38C is closed with owner released. The four reviewed SHAs remain unchanged; there is no repair, new owner,
  or new Java writer. Build remains independently blocked by TURN-40B shared Cloud main compile debt.

<!-- TRUE_EOF: TURN-38C PARENT-ACK-AUDIT CLOSED ACK=PARENT-TURN38C-REVIEW1-PASSED-0943 EXTERNAL-C-AVAILABLE OWNER-RELEASED BUILD-BLOCKED-BY-TURN40B 2026-07-18T09:53:00-04:00 -->
