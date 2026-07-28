# TURN-41 - Data Cutover And User Fresh Runtime Gate

- Canonical status: `A) SOURCE+TEST DATA-CUTOVER SOURCE ACTIVE / B) USER FRESH RUNTIME BLOCKED`
- Owner: `Huygens`
- Read-only business authority: `D:\mavenProject\DHXY`
- Writable repositories: `D:\mavenProject\DHXY-cr271`, `D:\mavenProject\dhxy-cloud-brain`
- Parent message: `PARENT-HUYGENS-TURN41-DATA-CUTOVER-20260720-202000`

## 1. Fixed SOURCE+TEST DATA-CUTOVER Subcard

### A. Worker-owned data cutover

1. Trace actual client/Cloud launch configuration and existing state roots to prove one exact non-blank
   `tenantId`, `userId`, `stateRoot`, and the resulting `CloudServiceStorage` hashed `scopeRoot`; never guess.
2. Provide a production-safe, repeatable, rollback-capable cutover command with explicit scope parameters,
   dry-run, pre-write backup, schema-compatible merge, count/hash verification, and rejection of blank or
   mismatched scope.
3. Import only baseline canonical stores: `dialog_choice_memory.json`, `vision_memory.json`,
   `world_map_route_result_memory.json`, and `map_camera_bounds.json`. The `data/*.json` sidecars,
   `transfer_choice_memory.json`, and `ocr_roi_memory.json` must not become a second authority.
4. Acceptance counts: dialog `22`; vision entries `460`, NPC samples `600`, target samples `1000`; route `80`;
   map-bounds SHA-256 `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`.
5. Prove all Cloud production owners resolve these exact private files through the same hashed scope.

### B. User-owned fresh runtime

- Opens only after A passes parent source review and exact target verification. Runtime/application/server/Task/UI/
  live capture/input remain forbidden to this Worker.
- `TEST READY` may be declared only when the real target scope exists and all A acceptance checks pass.

### Frozen safety boundary

- Baseline is strictly read-only. Preserve all three repositories' existing dirty/untracked content.
- No reset/clean/checkout/commit; no second store, endpoint, protocol, guessed directory, or guessed identity.
- If actual scope cannot be proven, complete the safe command and named tests, then record one exact user decision
  point; do not write canonical data to any target.

## 2. Canonical Claim

- Status: `SOURCE+TEST DATA-CUTOVER SOURCE ACTIVE / OWNER RETAINED / NOT APPROVED`.
- `ack_parent_message=PARENT-HUYGENS-TURN41-DATA-CUTOVER-20260720-202000`.
- Implementation begins with read-only scope tracing, followed by all safely implementable tooling and contracts.

<!-- TRUE_EOF: TURN-41 DATA-CUTOVER SOURCE-ACTIVE HUYGENS-OWNER ACK-PARENT-HUYGENS-TURN41-DATA-CUTOVER-20260720-202000 USER-FRESH-RUNTIME-BLOCKED NOT-APPROVED 2026-07-20T20:22:45-04:00 -->

## 3. Canonical WHOLE-CARD SOURCE+TEST Delivery

- Status: `WHOLE-CARD SOURCE+TEST DELIVERED / EXACT SCOPE DECISION REQUIRED / NOT TEST READY / NOT APPROVED`.
- `ack_parent_message=PARENT-HUYGENS-TURN41-DATA-CUTOVER-20260720-202000`.

### 3.1 Actual-scope trace evidence

- Cloud `CloudBrainApplication.ServerArgs` requires explicit `--tenant`, `--user`, and `--state-root` (or matching
  system properties) and provides no defaults.
- Before this card, both CR launch scripts passed only port/path/token. CR client production Java contains no
  tenant/user source; its former auto-sidecar implementation is absent. `application.properties` retains only
  stale sidecar path settings and contains no tenant/user/stateRoot.
- Cloud and CR `.idea`, `.run`, process environment, resources, scripts, and documentation contain no actual
  tenant/user/stateRoot value. A recursive three-worktree scan found zero 64-hex hashed scope directories.
- Cloud currently has only legacy process-global `data/vision_memory.json`; it is not an accepted private scope
  and was not copied. No actual scope target was created or written.

### 3.2 Production-safe cutover implementation

- Added Cloud `CloudStateCutoverCommand` with four explicit modes: `--inspect-scope`, `--dry-run`, `--apply`, and
  `--rollback`. It reuses `CloudServiceStorage` framing/hash/path containment, requires absolute roots and an
  `expected-scope-root` confirmation for every mutating/validation mode, and rejects blanks, relative paths,
  duplicate args, wrong scope, unsafe source/target files, malformed schemas, and wrong acceptance counts/SHA.
- Candidate construction deep-merges canonical object fields over an existing destination while retaining
  destination-only metadata. Any destination-only canonical entry makes acceptance counts fail before backup or
  mutation. Exact canonical bytes are retained when the merged tree equals the source; map bounds must retain the
  required byte SHA.
- Apply creates a per-run `.turn41-backups/<timestamp-uuid>` journal inside the verified real scope, records files
  that were absent, atomically replaces all four files, then rereads and revalidates the written target. Rollback
  accepts only a backup under the same real scope and restores exact bytes or deletes files recorded absent.
- Added non-runtime operator wrapper `dhxy-cloud-brain/scripts/turn41-state-cutover.ps1` for all four modes.
- `CloudBrainApplication` now binds `NpcClickMemoryStore`'s legacy property to the same hashed
  `vision_memory.json` used by scoped Spring owners and rejects any conflicting property. CR launchers now require
  and forward explicit scope values; the old `data/vision_memory.json` startup authority is removed.

### 3.3 Canonical source and owner verification

- Read-only baseline verified: dialog entries `22`; vision entries `460`; NPC samples `600`; target samples
  `1000`; route entries `80`; map-bounds SHA-256
  `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`.
- Canonical import allowlist is exactly the four `config/*.json` files in section 1. No `data/*.json`,
  `transfer_choice_memory.json`, or `ocr_roi_memory.json` is imported.
- Existing production owners resolve through one `CloudServiceStorage`: `DialogChoiceMemoryService`,
  `OcrRoiMemoryService`, `WorldMapRouteResultMemoryService`, and `CloudMapSurveyService`; the remaining legacy
  NPC consumer is now fail-closed onto that exact scoped vision file before server construction.

### 3.4 Exact write set, SHA, and verification

- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainApplication.java` SHA-256
  `38CBE8F2445E21FA57172BB1EDD7D53B0968A4BC51CCC8B3EB6FC8312E16271D`.
- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudStateCutoverCommand.java` SHA-256
  `0887A9E76B262DA7780DF1A3D3A4282708A8B560B98AC0C63E4FF7E1F4CF7D2F`.
- Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudStateCutoverCommandContractTest.java` SHA-256
  `62A5FF2450ECDBDA2A593A82F630E02108680DC5025DB804E82301998F7B47AB`.
- Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/CloudStateScopeOwnerContractTest.java` SHA-256
  `18074B5D25B2EE3822283514BDF462FE8B23A1D08A4F0B1FF49B887C9823F864`.
- Cloud `src/test/resources/turn41/map_camera_bounds.json` SHA-256
  `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`.
- Cloud `scripts/turn41-state-cutover.ps1` SHA-256
  `9BF2B7FD95EB46E036B461895653A8674410D3161FF061B5FE60D9EEE36830B6`.
- CR `scripts/run-cloud-brain-server.ps1` SHA-256
  `FBAB845328D3963F34540A1BC6C873EFA4C5EB8E0BFE4E8FA1E080616CDBEEEB`.
- CR `scripts/run-cloud-decision-dev-server.ps1` SHA-256
  `478AEAD377E95EF554FC68EFE23EC03127D17FF29D53B477D89EA82BEFEB7377`.
- PASS: Cloud
  `mvn -q "-Dtest=CloudStateCutoverCommandContractTest,CloudStateScopeOwnerContractTest" test` (`5+1`).
- PASS: Cloud `mvn -q compile`.
- PASS: PowerShell AST parse for all three changed scripts. Tests prove inspect/dry-run zero mutation, exact hash,
  merge/count verification, pre-write rejection, backup/apply/post-read verification, rollback, and owner binding.
- No runtime/application/server/Task/UI/live capture/input; baseline remained read-only; no reset/clean/checkout/
  commit or guessed target write.

### 3.5 Unique user decision point and exact continuation

Provide the one production tuple: **exact non-blank `tenantId`, exact non-blank `userId`, and absolute persistent
`stateRoot`**. No other business decision is pending. Then run:

1. `scripts/turn41-state-cutover.ps1 -Mode InspectScope -TenantId <tenant> -UserId <user> -StateRoot <absolute>`
2. Repeat with `-Mode DryRun -BaselineRoot D:\mavenProject\DHXY -ExpectedScopeRoot <printed-scope>`.
3. After checking the dry-run counts/SHA, repeat with `-Mode Apply`; retain the printed backup path for Rollback.

Only a successful real Apply plus post-read verification can change this card to `TEST READY`; user fresh runtime
remains blocked meanwhile. Waiting for parent source review and the exact scope tuple; this Worker does not approve.

<!-- TRUE_EOF: TURN-41 WHOLE-CARD-SOURCE-TEST-DELIVERED EXACT-SCOPE-DECISION-REQUIRED NOT-TEST-READY NOT-APPROVED TESTS-6-PASS CLOUD-COMPILE-PASS NO-REAL-TARGET-WRITE 2026-07-20T20:35:41-04:00 -->

## 4. Parent SOURCE+TEST Review #1 - Repair Required

- Verdict: `P0/P1/P2=0/1/1 / BLOCKED / REPAIR REQUIRED / OWNER RETAINED`.
- **P1 - Apply不是失败原子操作。** `CloudStateCutoverCommand.execute()`在创建backup后依次写四个目标并做
  post-read验证，但只在全部成功后输出/返回backup路径。任一后续写入或验证异常都可能留下部分新状态，正常
  错误输出也没有可恢复路径。证据：production `execute()`写入/验证序列及`backup()`；现有5项command tests
  只覆盖成功Apply/Rollback与pre-write rejection，没有注入mid-write或post-read failure。
- Repair condition: 首次mutation前持久输出backup路径；backup后的全部写入与验证置于事务边界，任一失败自动
  恢复四文件精确原字节并删除原先不存在的文件，保留backup并报告rollback结果；原异常继续抛出，rollback异常
  作为suppressed/diagnostic保留。测试须至少覆盖一项写入成功后的失败与post-read验证失败，并断言精确pre-state。
- **P2 - 安全敏感public API缺少合同JavaDoc。** 为public command/main补充args/mode、写入边界和失败恢复不变量。
- Huygens保留same-card owner；submission=`019f821f-7a09-7ba1-a9a5-02b90712f85f`，parent message id=
  `PARENT-HUYGENS-TURN41-REPAIR1-ATOMIC-APPLY-20260720-204000`。精确scope三元组仍未知且禁止猜测。
- No Maven rerun while Java writer is active; no runtime/application/server/Task/UI/live capture/input.

<!-- TRUE_EOF: TURN-41 PARENT-SOURCE-TEST-REVIEW-1 P0-0-P1-1-P2-1 REPAIR-REQUIRED OWNER-RETAINED ATOMIC-APPLY-RESTORE-REQUIRED NOT-TEST-READY PARENT-HUYGENS-TURN41-REPAIR1-ATOMIC-APPLY-20260720-204000 2026-07-20T20:40:00-04:00 -->

## 4. Repair #1 acknowledgement - 2026-07-20T20:40:00-04:00

- status: `REPAIR #1 SOURCE+TEST ACTIVE / OWNER RETAINED / NOT APPROVED`
- ack_parent_message: `PARENT-HUYGENS-TURN41-REPAIR1-ATOMIC-APPLY-20260720-204000`
- Accepted parent review `P0/P1/P2=0/1/1`. This repair is restricted to transactional Apply recovery,
  recoverable pre-mutation backup reporting, focused injected-failure tests, and public command JavaDoc.
- No exact production scope is known; no real Apply, runtime/application/server/Task/UI/capture/input will run.

<!-- TRUE_EOF: TURN-41 REPAIR1 SOURCE-TEST-ACTIVE OWNER-RETAINED NOT-APPROVED ACK-PARENT-HUYGENS-TURN41-REPAIR1-ATOMIC-APPLY-20260720-204000 2026-07-20T20:40:00-04:00 -->

## 5. Repair #1 canonical whole-card re-delivery - 2026-07-20T20:48:52-04:00

- status: `WHOLE-CARD SOURCE+TEST RE-DELIVERED / EXACT SCOPE DECISION REQUIRED / NOT TEST READY / NOT APPROVED`
- ack_parent_message: `PARENT-HUYGENS-TURN41-REPAIR1-ATOMIC-APPLY-20260720-204000`
- P1 closed in source+test: Apply now completes the backup manifest first, emits `APPLY backup-ready=<absolute>`
  and flushes it before mutation. All four atomic replacements and post-write reread/acceptance verification execute
  inside one failure boundary. Any failure automatically restores exact pre-Apply bytes and deletes targets recorded
  absent; the original Apply failure is propagated, rollback failure is attached as suppressed, and output records
  `rollback-complete` or `rollback-failed` with the preserved backup path.
- P2 closed: the public command and `main(String[] args)` now document modes, explicit scope inputs, and the
  pre-mutation journal/automatic-restore invariant.

### 5.1 Exact Repair #1 write set and SHA-256

- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudStateCutoverCommand.java`
  `2F36032CA3997E0E08A6203225BB13CAB44831E13D775A8D25653B7F24F76717`.
- Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudStateCutoverCommandContractTest.java`
  `62D638B9264A585EDD9ABAD3E904E254D2BAFFF9489C1AEE1809BAEFF7F710A2`.
- CR card and external-worker ledger received status-only append records; no client Java or baseline file changed.

### 5.2 Focused verification

- PASS: Cloud `mvn -q "-Dtest=CloudStateCutoverCommandContractTest,CloudStateScopeOwnerContractTest" test`:
  cutover `7`, owner `1`, failures/errors/skipped `0/0/0`.
- New production-path failure tests inject after two successful replacements and at post-write verification. Both
  assert exact existing-file bytes, new-file absence, preserved manifest, pre-mutation backup-path output, and
  automatic rollback outcome; the write-failure test also reuses that path for successful manual rollback.
- PASS: Cloud `mvn -q compile`.
- No real scope Apply and no runtime/application/server/Task/UI/live capture/input. Exact scope rules remain
  fail-closed; the sole remaining user decision is still exact `tenantId`, `userId`, and absolute `stateRoot`.

<!-- TRUE_EOF: TURN-41 REPAIR1 WHOLE-CARD-SOURCE-TEST-RE-DELIVERED EXACT-SCOPE-DECISION-REQUIRED NOT-TEST-READY NOT-APPROVED ACK-PARENT-HUYGENS-TURN41-REPAIR1-ATOMIC-APPLY-20260720-204000 TESTS-8-PASS CLOUD-COMPILE-PASS TRANSACTIONAL-ROLLBACK-PASS 2026-07-20T20:48:52-04:00 -->

## 6. Parent SOURCE+TEST Review #2 - Repair #1 Passed

- Verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`.
- `CloudStateCutoverCommand.execute()` now completes and reports/flushed the durable backup before mutation;
  every replacement plus post-write reread/verification shares one failure boundary. Any exception restores exact
  pre-state/absence across all four files, preserves the backup, propagates the original exception, and retains a
  rollback failure as suppressed diagnostic. Public safety JavaDoc is present.
- Parent inspected both injected failure paths and reran
  `mvn -q "-Dtest=CloudStateCutoverCommandContractTest,CloudStateScopeOwnerContractTest" test`: `7+1` PASS;
  parent `mvn -q compile` PASS. No runtime/application/server/Task/UI/live capture/input or real target write.
- Huygens owner is released. The sole remaining gate is exact nonblank `tenantId`, exact nonblank `userId`, and
  absolute persistent `stateRoot`, followed by successful real InspectScope/DryRun/Apply/post-read verification.
  Until then TURN-41 remains `EXACT SCOPE DECISION REQUIRED / NOT TEST READY`.

<!-- TRUE_EOF: TURN-41 PARENT-SOURCE-TEST-REVIEW-2 PASSED P0-0-P1-0-P2-0 OWNER-RELEASED TESTS-8-PASS CLOUD-COMPILE-PASS EXACT-SCOPE-DECISION-REQUIRED NOT-TEST-READY 2026-07-20T20:49:30-04:00 -->

## 7. Real production scope cutover - Passed / Test Ready

- Production tuple fixed by machine-stable policy:
  - tenantId=`dhxy-local`
  - userId=`S-1-5-21-2512076465-2442708813-415061167-1001` (current Windows account SID)
  - stateRoot=`C:\Users\Yunfeng Yue\AppData\Local\DHXY\cloud-brain\state` (outside all Git worktrees)
  - scopeRoot=`...\5f8f5fe289f501f11ca5459581a9a78c8982e0ace554268e524857051fa6a71e`
- `InspectScope` PASS with zero mutation; `DryRun` PASS; real `Apply` PASS with durable backup
  `.turn41-backups\2026-07-21T00-54-32.417285400Z-1204b683-5483-4bd1-9626-6d46277b627a`.
- Independent post-read verified dialog=`22`, vision entries=`460`, NPC samples=`600`, target samples=`1000`,
  route=`80`; file SHA-256 values are `33133663...c736e4`, `0e39a58c...7392e`, `382f7565...eb967e`, and
  map bounds `4428f7f9...e8a74b`.
- Status: `REAL DATA CUTOVER PASSED / TEST READY / USER FRESH RUNTIME READY / ZERO OWNER`.
- No server/application/Task/UI/live capture/input was started. Fresh runtime acceptance remains required before
  declaring the migration complete.

<!-- TRUE_EOF: TURN-41 REAL-PRODUCTION-DATA-CUTOVER PASSED TEST-READY USER-FRESH-RUNTIME-READY ZERO-OWNER COUNTS-22-460-600-1000-80 FOUR-SHA-PASS BACKUP-RETAINED NO-RUNTIME 2026-07-20T20:54:32-04:00 -->
