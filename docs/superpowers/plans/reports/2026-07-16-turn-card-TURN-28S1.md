# CR271 TURN-28S1 - NpcClick pending-proof baseline cleanup slice

## PARENT FROZEN CARD - EXTERNAL-B NEXT - 2026-07-16T08:42:21.828-04:00

- Status: `READY / REAL IMPLEMENTATION SLICE / CLAIM REQUIRED`.
- Parent card: `TURN-28`. External B returned the whole four-file card at `08:31:09` with all target bytes
  unchanged because the remaining context could not safely carry the full 3406+3026-line migration. The parent
  has therefore decomposed TURN-28 into reviewable, disjoint implementation slices instead of treating a long
  claim as activity.
- This is not a helper, readiness report or placeholder. It removes one concrete unapproved business delta from
  the real TURN-28 production path and advances External B's own queue-head card.
- Business authority: DHXY commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` plus
  `docs/业务逻辑.md`. The baseline `PendingSmartClickEvidence` has no normalized `sourceTask` field or equality
  gate; it confirms by exact pending key, proof token and expected option proof.

**无已批准业务差异；按 `696a12b0` 等价恢复 pending-proof 条件。**

## Exact modify write set

External B may modify only:

1. Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`.
2. This append-only card.

Initial production snapshot: 3406 lines, SHA-256
`f4e3842cdb5f59580d8f25f0191ade4847bfe8ca6c7939ac73a70bd561bfd870`.

`ObjectiveTextRecognizer.java`, `SmartClickRecognizer.java`, all tests, DHXY, protocol, Tasks/callers, POM,
config/resources and every other report are read-only. Preserve both repositories' dirty/untracked bytes.

## Frozen implementation contract

1. Remove only the post-baseline `PendingSmartClickEvidence.sourceTask` state and its constructor/from wiring,
   `matchesSourceTask(...)`, `normalizeSourceTask(...)`, and the early source-task rejection branch/log in
   `confirmExpectedOptionProof(...)`.
2. Preserve the public `confirmExpectedOptionProof(String sourceTask, ...)` signature. Its `sourceTask` argument
   may remain in diagnostic logs after the baseline proof-token/option checks; this slice does not change callers.
3. Preserve every normal request-level `request.sourceTask()` branch, especially the Wubei tooltip-first checks.
   Do not remove or alter those same-named baseline business decisions.
4. Preserve pending key, proof-token match, expected action/text match, remove-on-option-mismatch behavior,
   confirmed-memory recording and all map/name/coordinate/window fences. Do not add a replacement gate, TTL,
   cleanup, retry, session, owner, permit, ledger or durable workflow.
5. Do not begin the rest of TURN-28 in this slice. The parent will issue the next disjoint slice after independent
   review of this delivery.

## Acceptance and delivery

- Production has zero `PendingSmartClickEvidence.sourceTask`, `matchesSourceTask` and `normalizeSourceTask` symbols.
- Public method signature and all request-level Wubei `sourceTask` conditions remain unchanged.
- Diff is limited to the frozen one-file deletion/rewire above; no formatting or unrelated cleanup.
- External B must first append `EXTERNAL-B CLAIMED` at physical true EOF with its lane/session identity and current
  production SHA. Only then may it edit. On completion append one `EXTERNAL-B SOURCE DELIVERED` with final SHA,
  exact line evidence and the baseline comparison, then stop editing.
- Do not run Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input and do not perform Git
  mutation while other Java writers are active. Parent performs source review; the original TURN-28 named test and
  later integration/build gates remain on the parent card.

<!-- TRUE_EOF: TURN-28S1 PARENT FROZEN EXTERNAL-B NEXT REAL-IMPLEMENTATION ONE-FILE BASELINE-SLICE CLAIM-REQUIRED 2026-07-16T08:42:21.828-04:00 -->

## EXTERNAL-B CLAIMED - 2026-07-16T08:45:33-04:00

- Implementation Worker：**CR271 External Worker B**;非 reviewer,不能批准本卡;父级唯一 manager/final reviewer。本段不含 `APPROVED/CLOSED`,不自批。
- 身份(诚实自报,非平台权威真值):Claude Code 会话 `aa951b1e-8f04-4f92-b6e0-de08af49c39a`(UUIDv4;**不是**平台 spawn 的 `019f…` UUIDv7);自选临时 nickname `Kepler`。按父级「Worker 自报的非平台 UUID/nickname 不作 owner 真值」,权威身份以平台 spawn 记录为准,父级可在本卡追加 `CLAIM IDENTITY CORRECTION` 承接同一 ownership/写集/禁令。lane 报告 `reports/2026-07-16-cr271-external-worker-b.md` 不构成领取;本段为唯一领取依据。
- 已**完整读取本卡全部 58 行**(status/parent card/business authority/exact modify write set/frozen implementation contract 1-5/acceptance and delivery),并确认这是真实实现切片,非 helper/占位。
- **领取时产线快照已逐字核验一致**:`src/main/java/com/bot/dhxy/service/NpcClickService.java` = **3406 行**,SHA-256 `f4e3842cdb5f59580d8f25f0191ade4847bfe8ca6c7939ac73a70bd561bfd870` —— 与卡内 Initial production snapshot 相同。
- **写集(恰 2 项)**:① Cloud `NpcClickService.java`;② 本 append-only 卡。`ObjectiveTextRecognizer.java`/`SmartClickRecognizer.java`/全部 tests/DHXY/protocol/Tasks-callers/POM/config-resources/其它 report **全部只读**;保护两仓 dirty/untracked 字节。
- **基线**:DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` + `docs/业务逻辑.md`。基线 `PendingSmartClickEvidence` **无** normalized `sourceTask` 字段与等值门,仅凭 exact pending key + proof token + expected option proof 确认。无已批准业务差异。
- **接受冻结契约 1-5**:①仅删 post-baseline 的 `PendingSmartClickEvidence.sourceTask` 状态及其 constructor/from 接线、`matchesSourceTask(...)`、`normalizeSourceTask(...)`,以及 `confirmExpectedOptionProof(...)` 内的 early source-task 拒绝分支/日志;②**保留** public `confirmExpectedOptionProof(String sourceTask, ...)` 签名,其 `sourceTask` 实参可在 baseline proof-token/option 检查之后留作诊断日志,本切片不改 caller;③**保留全部 request-level `request.sourceTask()` 业务分支,尤其 Wubei tooltip-first**(与待删符号同名,绝不误删);④保留 pending key、proof-token 匹配、expected action/text 匹配、option 不匹配即移除、confirmed-memory 记录与全部 map/name/coordinate/window 围栏,**不加**替代门/TTL/cleanup/retry/session/owner/permit/ledger/durable workflow;⑤**不在本切片开始 TURN-28 其余部分**。
- **已定位的删除靶点(领取时行号)**:`:2072` 字段 `final String sourceTask;`、`:2088`/`:2107` constructor 形参与赋值、`:2130` 相关注释、`:2171-2176` `matchesSourceTask(...)`、`:2183` `normalizeSourceTask(...)`、`:2306` `if (!pending.matchesSourceTask(sourceTask))` 拒绝分支 + `:2308-2311` 其注释/日志;`:2316`/`:2322` 等诊断日志中的 `sourceTask` 实参按契约 ② 可保留。**保留不动**:`:813`/`:850`/`:894` 等 `request.sourceTask().equals(TaskType.WUBEI)` 业务分支。
- **禁令**:不扩写集、不做格式化或无关清理、diff 仅限上述冻结的一文件删除/rewire;其它 Java writer 活动期间**不跑 Maven/JUnit/compile/package**;不启 runtime/application/server/Task/UI/capture/input;**禁一切 Git mutation**;只从当前字节增量编辑。基线 DHXY HEAD `0114604e`、cloud-brain `3b988ca` 未动。我亦不再触碰 TURN-28P(现属 Internal Euler)与 TURN-28 其余写集。
- 交付:完成后在本卡追加**一次** `EXTERNAL-B SOURCE DELIVERED` + 最终 SHA + 精确行证据 + 基线对照,然后停止编辑;父级独立源审;原 TURN-28 named test 与后续 integration/build 门留在父卡,我不冒充其通过。

<!-- TRUE_EOF: TURN-28S1 EXTERNAL-B CLAIMED ONE-FILE BASELINE-SLICE Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T08:45:33-04:00 -->

## EXTERNAL-B SOURCE DELIVERED - 2026-07-16T08:51:38-04:00

按冻结契约完成 TURN-28S1 唯一切片。**本段不是 approval**;不含 `APPROVED/CLOSED`;不自批;请父级独立源审。

**写集(恰 2 项,无扩张)**:① Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`;② 本卡。其它文件零改动 —— 已核 `ObjectiveTextRecognizer.java`(`d3dc3cc247058ae8…`)、`SmartClickRecognizer.java`(`ffbd984a4ed5841c…`)仍等于领取时 SHA;未碰任何 test/DHXY/protocol/Task/POM/config/其它 report;未碰 TURN-28P(属 Internal Euler)与 TURN-28 其余写集。

| 文件 | 领取时 | 交付时 |
|---|---|---|
| `NpcClickService.java` | 3406 行 / `f4e3842cdb5f59580d8f25f0191ade4847bfe8ca6c7939ac73a70bd561bfd870` | **3374 行** / **`cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`** |

净删 32 行,无新增功能行。

**逐项行证据(按契约 1-5)**:
1. **删 `PendingSmartClickEvidence.sourceTask` 状态与接线**:删字段 `final String sourceTask;`(原 :2072);删 private 构造末位形参 `String sourceTask`(原 :2088)与 `this.sourceTask = sourceTask;`(原 :2107);删 `from(...)` 末位实参 —— 即原 :2129-2131 的注释 `Bind the pending evidence to the creating request's business source…` 及 `normalizeSourceTask(request == null || request.sourceTask() == null ? null : request.sourceTask().getCode())`,`from(...)` 现以 `result.message()` 收尾。
2. **删 `matchesSourceTask(...)`**(原 :2171-2176 整方法)与 **`normalizeSourceTask(...)`**(原 :2183-2189 整方法);保留同区 `normalizeNullable(...)` 与 `matchesProofToken(...)` 原样。
3. **删 `confirmExpectedOptionProof(...)` 内 early source-task 拒绝分支**(原 :2306-2313):`if (!pending.matchesSourceTask(sourceTask)) { …三行注释… log.debug(… pendingSourceTask={} …); return; }` 整块移除;其后 `matchesProofToken` 检查现为该方法在 `pending == null` 之后的**第一道** baseline 检查。
4. **删随之失效的 `import java.util.Locale;`**(原 :66):`normalizeSourceTask` 是其唯一使用者(`Locale.ROOT`),删方法后该 import 完全无引用。此为本删除的直接 rewire 后果,非无关清理;全文件 `Locale` 出现次数现为 **0**。

**保留项验证(grep 实证)**:
- public 签名 `public void confirmExpectedOptionProof(String sourceTask, String actionKey, String matchedText, String proofToken, String verificationStrength, String reason)` **未改**(现 :2271;计数=1);未改任何 caller。
- 其 `sourceTask` 实参按契约 ② **保留在 baseline proof-token/option 检查之后的诊断日志**中(`source={}` 日志 15 处)。
- **全部 request-level `request.sourceTask()` 业务分支保留**:计数 **16**,含 Wubei tooltip-first 三处 `:813` `&& !request.sourceTask().equals(TaskType.WUBEI)`、`:850` `if (request.sourceTask().equals(TaskType.WUBEI))`、`:894` `if (!request.sourceTask().equals(TaskType.WUBEI) && tryNormalTooltipStrategy(...))` —— 与被删符号同名但属 696 基线业务决策,逐字未动。
- pending key、`matchesProofToken`、`matchesExpectedOptionProof`、option 不匹配即 `removePendingSmartClickEvidence(...)`、confirmed-memory 记录、map/name/coordinate/window 围栏**全部原样**;**未新增**任何替代门/TTL/cleanup/retry/session/owner/permit/ledger/durable workflow。
- **未开始 TURN-28 其余部分**(契约 ⑤):`NpcClickService` 的 turn cutover(仍在用 `InputSequences`/`tracker.`)、named test、其余 production 均未触碰,留待父级后续互斥切片。

**验收自检(父级请复核)**:production 中 `PendingSmartClickEvidence.sourceTask` / `matchesSourceTask` / `normalizeSourceTask` 符号计数均为 **0**;public 签名与 request-level Wubei 条件不变;diff 仅限上述冻结的一文件删除/rewire,无格式化、无无关清理。

**基线对照**:`696a12b0` 的 `PendingSmartClickEvidence` 无 normalized `sourceTask` 字段与等值门,仅凭 exact pending key + proof token + expected option proof 确认。本切片删除的正是 post-baseline 增补项,恢复为 696 等价条件。**无已批准业务差异**。

**未运行门**:本切片**未运行** Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input(其它 Java writer 活动中);**未执行任何 Git mutation**;两仓全部既有 dirty/untracked 与他人半成品完好;DHXY HEAD `0114604e`、cloud-brain `3b988ca` 未动。原 TURN-28 named test 与后续 integration/build 门仍在父卡,我不冒充其通过。自此停止编辑,等父级源审或下一张互斥切片。

<!-- TRUE_EOF: TURN-28S1 EXTERNAL-B SOURCE DELIVERED one-file 3406->3374 sha cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441 Kepler(self-reported; platform id pending) session:aa951b1e-8f04-4f92-b6e0-de08af49c39a 2026-07-16T08:51:38-04:00 -->

## PARENT SOURCE REVIEW #1 - PASSED - 2026-07-16T08:59:40.918-04:00

- 父级独立读取 TURN-28S1 固定合同、交付卡、当前 Cloud `NpcClickService.java`、
  `migration-baseline/696a12b0` 对应文件及 `docs/业务逻辑.md`，并重算当前 production SHA 为
  `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`。
- 当前 production 与 `migration-baseline/696a12b0/.../NpcClickService.java` **3374 行逐字节相同**；
  `PendingSmartClickEvidence.sourceTask`、`matchesSourceTask(...)`、`normalizeSourceTask(...)` 均为零，
  `confirmExpectedOptionProof(String sourceTask, ...)` 签名与 proof-token/expected-option/window/key/坐标围栏保留。
- `request.sourceTask()` 业务分支当前与基线均为 16 处，Wubei tooltip-first 与 non-Wubei 条件未被误删；未发现
  替代 sourceTask gate、TTL、cleanup、retry、session、owner、ledger 或 durable workflow。
- 结论：**`P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED / INDEPENDENT REVIEW PENDING`**。External B 的 S1
  implementation owner 已释放；R1/R2 已另派，不能用 Worker 自述或父级结论替代双 reviewer 门。
- 本轮因 A/C 仍为 Java writer，未运行 Maven/JUnit/compile/package；未启动 runtime/application/server/Task/UI/
  capture/input，未执行 Git mutation。

<!-- TRUE_EOF: TURN-28S1 PARENT SOURCE-REVIEW-1 PASSED P0P1P2=0/0/0 SOURCE-REVIEW-PASSED DUAL-REVIEW-PENDING EXTERNAL-B-OWNER-RELEASED 2026-07-16T08:59:40.918-04:00 -->
