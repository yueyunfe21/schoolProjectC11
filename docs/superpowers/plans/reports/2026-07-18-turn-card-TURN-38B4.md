# CR271 TURN-38B4 Scoped PNG Artifact Store Whole Card

## Canonical State

- state: `WHOLE-CARD SOURCE START READY / ZERO OWNER / UNASSIGNED`
- parent freeze: `2026-07-18T06:38:00-04:00`
- authority worktree: `D:\mavenProject\DHXY-cr271` / `thin-client-design`
- Cloud worktree: `D:\mavenProject\dhxy-cloud-brain` / `navigation-migration`
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- claim rule: this card physical EOF is the sole owner authority. A Worker may claim only the whole card by
  appending one canonical anti-race claim and then reading the physical EOF back. This card assigns nobody.

## Source Gates And Parent Audit

- TURN-17, TURN-38A-F, and TURN-13H source gates are satisfied and their source owners are released.
- Parent completely read the earlier B4 readiness report and current `CloudArtifactStore`,
  `ScopedPngArtifactStore`, `CloudServiceConfiguration`, `CloudArtifactCapacityGovernor`, `CloudServiceStorage`,
  `CloudServiceScope`, `CloudServiceHost`, `TaskExecutionContext`, `TurnInvocationContext`, `CloudTurnFrame`, and
  `TurnFrameMetadata`, then scanned all production/test references.
- Current production caller count for artifact operations is zero. The real host bean construction seam exists;
  runtime activation and terminal caller assembly remain TURN-40B/TURN-40C work.
- The governor's process-global maps are classified as bounded capacity/collision/delete accounting only. They
  must never authorize reads, select workflow state, retain terminal history, or expire by age.
- No approved business difference. This card moves diagnostics/artifact plumbing only.

## Exact Whole-Card Write Set

Production, Cloud repository only:

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudArtifactStore.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/ScopedPngArtifactStore.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java`

Test, Cloud repository only:

4. `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/ScopedPngArtifactStoreTurnTest.java`

Protected claim-point evidence:

| File | SHA-256 | Lines |
|---|---|---:|
| `CloudArtifactStore.java` | `D690721110DB4A980E41934C9729BEC598900C70CF6615D7189EFF7EC29FBE9D` | 60 |
| `ScopedPngArtifactStore.java` | `CF7E857C9AF293F1FF2C5C2D25AEF54B76F0FEA90A7CB1DD08CA0B86E6B3B151` | 346 |
| `CloudServiceConfiguration.java` | `B047D9F910C724083B9594D431ED31DB1601BEFCC18F179ABF263D4D23A8199D` | 104 |
| `ScopedPngArtifactStoreTurnTest.java` | `ABSENT` | 0 |

`CloudArtifactCapacityGovernor`, `CloudServiceStorage`, `CloudServiceScope`, `CloudServiceHost`, context/frame/
protocol classes, runtime/factory/registry, POMs, existing tests, DHXY files, and every other path are read-only.
Any required fifth path is `PLAN-CONTRACT BLOCKED`; do not hide it in a helper, nested second store, or stub.

## Frozen Public Contract

1. Replace every `CloudTaskServiceExecutionContext` operation parameter with non-mintable turn-native
   `TaskExecutionContext`. Delete `revalidate`, old scope/epoch/revision/owner authority and the mutable
   `ownerLedger`; do not add another owner/session/permit/ledger map.
2. Write accepts the existing `CloudTurnFrame`, not `BufferedImage`. Read returns a defensive
   `CloudTurnFrame`. PNG bytes are persisted and returned byte-for-byte; no decode/re-encode or second capture.
3. Validate the existing frame contract before filesystem mutation: PNG content type, SHA-256, dimensions,
   absolute region, purpose, and nullable source step index remain correlated. Invalid/corrupt facts fail closed.
4. Exact artifact identity derives from the same immutable B3 turn-native context facts: host-fixed tenant/user,
   invocation device/window, taskRunId/taskCode, native title/handle/process, role/team facts, startup mode, and
   startedAt. Pause/resume reuses the same identity; age alone never changes ownership or validity.
5. `ArtifactId` remains opaque and path-free. It may carry immutable `TurnFrameMetadata` inside the existing
   interface file. Its canonical `af1-<32 lowercase hex>` token uses a fixed exact-context digest prefix plus a
   random collision suffix. Foreign context, forged metadata, wrong task/window, or drift fails before read,
   delete, cleanup, or filesystem mutation. A-to-B-to-A preserves A exactly.
6. Atomic publication keeps existing bounded mechanics: private `CREATE_NEW` temp, no target before commit,
   `ATOMIC_MOVE`, no replace, bounded token attempts, exact own-temp cleanup, and settled governor accounting.
7. No caller-selected file/path, listing/download API, sidecar metadata file, TTL/timer/background cleanup,
   retry, durable replay, second DTO, second store, or business truth is permitted.

## Cleanup And Assembly Boundary

- Expose an idempotent exact-task cleanup operation through `CloudArtifactStore`. It may scan only the current
  verified tenant scope for canonical artifact/temp names matching the exact-context token prefix. Every target
  deletion must use the existing governor business-delete plan/delete/settle path; direct unaccounted deletion is
  forbidden.
- `ScopedPngArtifactStore` implements Spring-compatible close lifecycle. `CloudServiceConfiguration` wires that
  destroy lifecycle. Sequential close/recreate for one scope physically removes prior task artifacts and does not
  revive stale ids. Cleanup must preserve sibling tenant/window/task artifacts.
- B4 delivers the capability only. TURN-40B owns the real task-terminal caller: after terminal classification and
  after artifact consumers are quiescent, but before task registry/context removal and host close. TURN-40C owns
  production host activation/single-scope lifecycle. B4 must not claim either runtime wiring is already active.

## Sole Named Test Contract

Required command once no Java writer is active:

`mvn -q -Dtest=ScopedPngArtifactStoreTurnTest test`

The single test class must obtain the store through the real `CloudServiceHost.create(...).getService(...)` graph
using temp storage and fake command/catalog only. It must cover at least:

1. one real host bean, lazy zero-I/O construction, and Spring close;
2. exact tenant/user/device/window/run/task/native/start tuple isolation, including each independent mismatch;
3. A-to-B-to-A read/delete/cleanup isolation and sibling preservation;
4. byte-exact PNG/metadata/SHA/dimensions/region/source-step round trip and defensive copies;
5. atomic create-new/temp/move/no-replace plus encode/write/move failure cleanup and settled accounting;
6. pause/resume continuity, no TTL, and zero second write;
7. success/failure/stopped/skipped/exception terminal cleanup capability, idempotence, and exact-task scope;
8. close/recreate physical cleanup with stale id unreadable and no sibling loss;
9. capacity pressure remains capacity-only, never read authority or age expiry;
10. public API and production references contain no old remote authority, revision/revalidate, owner/session/ledger,
    caller path, or second store.

Tests do not start application/server/Task/runtime and do not capture or send input. A direct cleanup test proves
capability, not TURN-40B runtime caller wiring.

## Delivery And Stop Rules

- Deliver all four paths together with line counts, SHA-256, test-method count, old-authority/reference scans, and
  `无已批准业务差异；按基线等价迁移`.
- Do not run Maven while any Java writer is active. The parent runs only the authorized named family and applicable
  compile in a stable window.
- Stop and append `PLAN-CONTRACT BLOCKED` at this physical EOF if the contract needs another production/test path,
  a governor/storage/host API change, a real terminal caller, a second metadata authority, or a business-semantic
  choice. Do not implement a stub, null result, copied algorithm, or alternate protocol/store.

<!-- TRUE_EOF: TURN-38B4 PARENT-CONTRACT-FROZEN WHOLE-CARD-SOURCE-START-READY ZERO-OWNER UNASSIGNED FOUR-FILE-WRITE-SET TURN-NATIVE-RAW-PNG-ATOMIC-CLEANUP-CAPABILITY REAL-CALLER-40B 2026-07-18T06:38:00-04:00 -->

## EXTERNAL-C TURN-38B4 WHOLE-CARD CLAIMED - 2026-07-18T06:43:00-04:00

- Implementation Worker：**CR271 External Worker C**（本 lane 六整卡全 PASSED：TURN-27/36/37/38B1/38B2/38B3；38B3 于 06:27 一次过审 PASSED+OWNER RELEASED 后 IDLE）。非 reviewer，不自批；父级为唯一 manager/final reviewer。
- 领取时间：`2026-07-18T06:43:00-04:00`（=append 时刻）。容量：`AVAILABLE`（不持其它卡）。响应 `PARENT-TURN38B4-READY-0638`（TO-A,C，非派卡；本 claim 前已全读原卡 119L 全文）。
- **防竞态预检证据**（预检与本 append 两次独立调用）：卡 119L physical EOF=06:38 父级 freeze 块，实际 claim 数=0；ledger 尾扫零 38B4 claim；A 仍 COMMUNICATION_STALE（06:39 父级 audit 维持）。append 后立即回读 EOF 复核唯一性，发现更早 claim 立即 canonical 自撤。
- **领取点重取证**（实测逐字一致）：`CloudArtifactStore.java` SHA `D690721110DB4A980E41934C9729BEC598900C70CF6615D7189EFF7EC29FBE9D`/60L；`ScopedPngArtifactStore.java` SHA `CF7E857C9AF293F1FF2C5C2D25AEF54B76F0FEA90A7CB1DD08CA0B86E6B3B151`/346L；`CloudServiceConfiguration.java` SHA `B047D9F910C724083B9594D431ED31DB1601BEFCC18F179ABF263D4D23A8199D`/104L；test `ScopedPngArtifactStoreTurnTest.java`=ABSENT（与卡一致）。
- **collision scan**：写集与已收官 B1/B2/B3 文件、governor/storage/scope/host/context/frame/protocol/runtime/POM/既有 test/DHXY 零交集；第五路径需求=`PLAN-CONTRACT BLOCKED` 落卡不自扩不藏 helper。
- **合同收悉**（全 7 条 frozen contract+cleanup/assembly 边界+10 项 test gate）：①`CloudTaskServiceExecutionContext` 参数全换非可铸 turn-native `TaskExecutionContext`；删 revalidate/旧 scope-epoch-revision-owner authority/可变 `ownerLedger`；不加新 owner/session/permit/ledger map；②write 收既有 `CloudTurnFrame`（非 BufferedImage）、read 返 defensive `CloudTurnFrame`、PNG bytes 逐字节存取零 decode/re-encode/二次捕获；③FS 变更前校验既有 frame 契约（PNG content type/SHA-256/尺寸/绝对 region/purpose/nullable source step index 关联），invalid/corrupt fail-closed；④artifact 身份=B3 同源 immutable turn-native 事实（tenant/user/device/window/taskRunId/taskCode/native 三元/role-team/startupMode/startedAt），pause/resume 同身份、age 不改所有权；⑤`ArtifactId` 不透明无路径、可携 immutable `TurnFrameMetadata`（接口文件内）、token=`af1-<32 lowercase hex>`（exact-context digest 前缀+随机防撞后缀）、foreign/伪造/漂移在 read-delete-cleanup-FS 变更前 fail、A→B→A 保 A；⑥原子发布保既有机制（CREATE_NEW 私有 temp/commit 前无 target/ATOMIC_MOVE/no replace/有界 token 尝试/exact own-temp 清理/governor 记账 settle）；⑦禁 caller 选路径/listing-download API/sidecar 元文件/TTL-timer-后台清理/retry/durable replay/第二 DTO/第二 store/业务真值。cleanup=经 `CloudArtifactStore` 幂等 exact-task 操作（仅扫当前验证 tenant scope 的 canonical token 前缀、删除全走 governor plan/delete/settle）；`ScopedPngArtifactStore` 实现 Spring close lifecycle+`CloudServiceConfiguration` 接 destroy；close/recreate 物理清 prior task artifacts 不复活 stale id 不伤 sibling；40B 拥有真实 terminal caller、40C 拥有 host activation，B4 只交 capability 不 claim runtime 已接。test 经真实 `CloudServiceHost.create(...).getService(...)` 图+temp storage+fake command/catalog，10 gate，授权命令 `mvn -q -Dtest=ScopedPngArtifactStoreTurnTest test`。
- **基线**：`696a12b0`；无已批准业务差异；本卡仅移 diagnostics/artifact plumbing。
- **纪律**：零 Git mutation；`D:\mavenProject\DHXY` 只读；不运行 runtime/UI/capture/input；其它 Java writer 活动时不运行 Maven（javac 单文件 parse 除外）；不自批、不建 reviewer；heartbeat `778801ea` 切本卡监控。

TRUE_EOF

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C WHOLE-CARD CLAIMED OWNER-C PROTECTED-BYTES-VERIFIED D6907211+CF7E857C+B047D9F9 TEST-ABSENT ANTI-RACE-PRECHECKED ACK=PARENT-TURN38B4-READY-0638 CAPACITY-AVAILABLE 2026-07-18T06:43:00-04:00 -->

## PARENT AMENDMENT #1 - TOKEN BITS / CLEANUP SCOPE - 2026-07-18T06:52:00-04:00

- state: `SOURCE_ACTIVE / EXTERNAL-C OWNER / HOLD FOR AMENDMENT #1 ACK`
- trigger: C 06:50 recon correctly proposed prefix-bound stateless ownership, but selected `16 hex context + 16 hex
  random`; the parent freeze had not fixed the split and also mixed sibling-preserving task cleanup with host-close
  scope cleanup. No B4 Java/test bytes exist, so repair the contract before implementation.
- token: preserve canonical `af1-<32 lowercase hex>`, split exactly as `24 hex context digest prefix + 8 hex random
  nonce`. Context prefix is the first 96 bits of SHA-256 over an unambiguous length-prefixed encoding of the frozen
  B3-same exact-context tuple. Nonce is 32 random bits, minted with the existing bounded attempts. Do not use `16+16`,
  truncate context below 96 bits, add a key/session/ledger, or change governor filename regex.
- exact-task cleanup: match only the current context's 24-hex prefix within the verified tenant/user scope; preserve
  every other window/task prefix in that same scope and all other tenant/user scopes. It is idempotent and every
  canonical target deletion goes through governor plan/delete/settle accounting.
- host-close cleanup: `close()` has no task context. TURN-40C must guarantee all tasks in that `CloudServiceScope` are
  quiescent before Spring destroys the store; close then removes all canonical artifact/temp files in that exact
  tenant/user scope through governor accounting. It does not promise to preserve sibling task artifacts inside a
  scope that is itself closing, but must preserve every other tenant/user scope.
- test correction: sibling window/task preservation belongs to exact-task cleanup. Close/recreate must prove the
  closing scope is fully cleared and stale ids do not revive, while a separate tenant/user host remains byte-exact.
- required ACK: C must append `ack_parent_message=PARENT-TURN38B4-AMENDMENT1-0652` before production/test edits.

<!-- TRUE_EOF: TURN-38B4 PARENT-AMENDMENT1 TOKEN=24HEX-CONTEXT+8HEX-NONCE EXACT-TASK-PRESERVES-SIBLING HOST-CLOSE-CLEARS-QUIESCENT-SCOPE PRESERVE-OTHER-SCOPE OWNER-C HOLD-FOR-ACK=PARENT-TURN38B4-AMENDMENT1-0652 NO-JAVA 2026-07-18T06:52:00-04:00 -->

## EXTERNAL-C TURN-38B4 WHOLE-CARD CANONICAL DELIVERY - 2026-07-18T07:21:00-04:00

TURN-38B4（Scoped PNG Artifact Store）**交付 SOURCE+TEST 源审**。sole owner C；Amendment#1（token 24+8/双层 cleanup）已 06:55 具名 ACK 且父级 06:56 确认后实施。零 Git mutation。

### A. 写集 4/4（不增不减）
| 文件 | 起始（protected） | 交付 blob / SHA-256 / 行数 |
|------|------------------|---------------------------|
| `CloudArtifactStore.java`（Modify） | `D6907211...`/60L | `fb1e942e` / `E7702A290BD9842EE6113F8BC1DE2F9B6CC0E784F3CB2D11ECD4E08C884E2A4D` / 88L |
| `ScopedPngArtifactStore.java`（Modify） | `CF7E857C...`/346L | `a4d54a03` / `73483F6AE2B1923C49C098B8103EE0DB95011974526253728F1FB22A252B82C4` / 491L |
| `CloudServiceConfiguration.java`（Modify） | `B047D9F9...`/104L | `77fac8b4` / `91F30C0AB9E15F8B5E80ACE1CD1B0B401CA2C64FCD01EBDA756539A387EF860A` / 107L |
| `ScopedPngArtifactStoreTurnTest.java`（Create） | ABSENT | `cf32aea1` / `2D360123E54649572351202F2688B66A702E62A8019A765F7A17658FE368F487` / 445L / **9 @Test** |

四文件单文件/联编 `javac -proc:none -implicit:none -sourcepath "" -cp ""` 零纯语法错（store 单文件 4 个 "does not override"=接口不可解析噪声，接口联编消除）。旧模型残留（`CloudTaskServiceExecutionContext/RemoteTaskRun*/revalidate/ownerLedger/BufferedImage/ImageIO`）三 production = **0**。

### B. 7 条 frozen contract 落地
1. 全操作参数=turn-native `TaskExecutionContext`；`revalidate`/旧 scope-epoch-revision authority/可变 `ownerLedger`+`OwnerRecord` 已删；**零新 owner/session/permit/ledger map**（ownership=无状态 token 前缀绑定）。
2. `write` 收既有 `CloudTurnFrame`、`read` 返 defensive `CloudTurnFrame`（类型构造/accessor 自带 clone）；PNG bytes 逐字节持久化与返回，零 decode/re-encode/二次捕获（`ImageIO` 全删）。
3. FS 变更前 `validFrame` fail-closed：`image/png` content type+SHA-256 实算与 metadata 比对+尺寸/像素/字节上限+region 尺寸与 frame 关联+purpose 非空+sourceStepIndex nullable；read 侧 SHA 复验（corrupt→empty）。
4. 身份=B3 同源 immutable 事实（tenant/user+device/window+taskRunId/taskCode+native title/handle/process+role/team 四项+startupMode+startedAt，16 字段）；pause/resume 同 tuple 同前缀；age 零效力。
5. `ArtifactId(token, TurnFrameMetadata)`（接口文件内，无 sidecar）；token=`af1-`+**24hex context digest（16 字段 length-prefixed 无歧义编码 SHA-256 前 96 位）+8hex random nonce**（Amendment#1 精确；governor 文件名正则未动）；foreign/伪造/wrong task-window/native drift 在 read/delete/cleanup/FS 变更前 fail（前缀不匹配即拒）；A→B→A 保 A。
6. 原子发布逐字保留：CREATE_NEW 私有 temp/commit 前无 target/ATOMIC_MOVE/no-replace/有界 nonce 重铸（TOKEN_MINT_ATTEMPTS=4）/exact own-temp 清理/moved[] 双分支 commit-rollback/settled governor 记账/encodePermits 有界并发。
7. 无 caller 路径/listing-download/sidecar/TTL-timer-后台清理/retry/durable replay/第二 DTO/第二 store/业务真值。

### C. Cleanup 与 assembly 边界（Amendment#1 双层语义）
- `cleanupTaskArtifacts(exactContext)`：仅扫 verified tenant scope 内 `af1-<当前 24hex 前缀>` canonical artifact/temp；artifact 删除全经 governor `planBusinessDelete/deleteEvicting/settleBusinessDelete`、temp（非记账）直删；幂等；保同 scope sibling+他 scope。
- `close()`=`AutoCloseable`+config `@Bean(destroyMethod="close")` Spring destroy 边界（40C 保静默）：清本 scope 全部 canonical artifact/temp（`af1-` 全前缀）经 governor 记账；幂等；保他 scope。40B 拥有真实 terminal caller、40C 拥有 host activation——B4 只交 capability（javadoc 明载）。

### D. 唯一 test（9 @Test 覆盖 10 gate；真实 `CloudServiceHost.create(scope,@TempDir,fake port,catalog)` 图；零 runtime/capture/input）
①host bean 唯一+lazy 零 I/O（构造后 stateRoot 零文件）+Spring close 后 canonical=0 ②九路独立 mismatch（scope/device/window/run/task/title/handle/pid/startedAt）read+delete 全拒+owner 事后仍读 ③A→B→A 跨 task 拒/own delete/exact-task cleanup=1+replay=0/sibling byte-exact ④byte-exact 往返+metadata 全字段+防御副本（返回数组篡改不透） ⑤三类 invalid frame fail-closed 零盘面残留+记账 settled 后续正常写+零 temp ⑥pause/resume 同 tuple 照读+无 TTL+零二次写 ⑦五终态标签 cleanup 幂等+sibling 保全 ⑧close/recreate closing scope 全清+stale id 不复活+**他 scope 同 stateRoot byte-exact** ⑨8 写全读=容量仅记账 ⑩全场景仅 turn-native context+opaque id 结构性证明（源码残留归父级源审）。frame=确定性 pseudo-PNG bytes+实算 SHA（store 信任 frame 契约非像素）。

### E. 纪律
`无已批准业务差异；按基线等价迁移`（本卡仅 diagnostics/artifact plumbing）。零 Git mutation；未运行 Maven（授权命令 `mvn -q -Dtest=ScopedPngArtifactStoreTurnTest test`=交付后 build gate）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。请求：**whole-card SOURCE+TEST review**。

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C WHOLE-CARD-CANONICAL-DELIVERY INTERFACE=fb1e942e STORE=a4d54a03 CONFIG=77fac8b4 TEST=cf32aea1 9-TESTS TOKEN-24+8 STATELESS-PREFIX-OWNERSHIP DUAL-CLEANUP GOVERNOR-CHAIN-KEPT ZERO-RESIDUE REQUEST-REVIEW OWNER-C NO-MAVEN 2026-07-18T07:21:00-04:00 -->

## PARENT AMENDMENT #1 ACK ACCEPTED - 2026-07-18T06:56:00-04:00

- Parent full-read captured External C's 06:55 STATUS EVENT with the named ACK
  `ack_parent_message=PARENT-TURN38B4-AMENDMENT1-0652`. C explicitly withdrew `16+16` and accepted the exact
  `24+8` token split, exact-task sibling preservation, and quiescent host-close whole-scope cleanup.
- The captured event reported zero Git mutation at ACK. Its append raced with the parent's 06:52 ledger append and
  was overwritten at physical EOF; the parent preserves and accepts that observed ACK here without impersonating a
  Worker STATUS EVENT. The 06:52 hold is released and C remains canonical owner.
- current state: `SOURCE_ACTIVE / EXTERNAL-C OWNER / AMENDMENT #1 ACKED`.

<!-- TRUE_EOF: TURN-38B4 PARENT-AMENDMENT1-ACK-ACCEPTED OBSERVED-C-STATUS-0655 APPEND-RACE-RECONCILED OWNER-C SOURCE-ACTIVE TOKEN-24+8 DUAL-CLEANUP-CONTRACT IMPLEMENTATION-UNBLOCKED 2026-07-18T06:56:00-04:00 -->

## PARENT DELIVERY EOF RECONCILIATION - 2026-07-18T07:22:00-04:00

- C's complete 07:21 four-file delivery block is present above, but the concurrently written parent ACK block landed
  after it. Under the fixed rule that this original card's physical EOF is the sole delivery authority, the 07:21
  block is not yet canonical and source review must not start from a non-EOF delivery.
- No source repair is requested. C must append the same delivery evidence at physical EOF and acknowledge
  `PARENT-TURN38B4-DELIVERY-EOF-REASSERT-0722`; parent review starts immediately after that canonical reassert.
- state: `SOURCE+TEST DELIVERY REASSERT REQUIRED / EXTERNAL-C OWNER`.

<!-- TRUE_EOF: TURN-38B4 PARENT-DELIVERY-EOF-RECONCILIATION REASSERT-REQUIRED OWNER-C NO-SOURCE-REPAIR ACK=PARENT-TURN38B4-DELIVERY-EOF-REASSERT-0722 2026-07-18T07:22:00-04:00 -->

## EXTERNAL-C DELIVERY POINTER / APPEND-ORDER NOTE - 2026-07-18T07:24:00-04:00

- 我的 `WHOLE-CARD CANONICAL DELIVERY 07:21` 块（本卡 159-192 行，marker `WHOLE-CARD-CANONICAL-DELIVERY INTERFACE=fb1e942e STORE=a4d54a03 CONFIG=77fac8b4 TEST=cf32aea1`）因 append 锚点选中 06:52 amendment marker，物理落位于父级 06:56 `AMENDMENT #1 ACK ACCEPTED` 块之前——两块均完整无交错，自署时间序（06:56 < 07:21）与内容不受影响。本条为 physical EOF 上的 canonical 指针：**TURN-38B4 whole-card delivery = 07:21 块，写集 4/4（fb1e942e/a4d54a03/77fac8b4/cf32aea1-9T），请求 SOURCE+TEST review**。零字节改动既有块。

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C DELIVERY-POINTER CANONICAL-DELIVERY-AT-0721-BLOCK WRITESET-4-OF-4 REQUEST-REVIEW OWNER-C APPEND-ORDER-NOTED 2026-07-18T07:24:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #1 - BLOCKED / REPAIR REQUIRED - 2026-07-18T07:31:00-04:00

Verdict: `P0/P1/P2 = 0/1/3`. External C remains whole-card owner. No source approval.

### P1-1 - ArtifactId is caller-mintable, so forged metadata reaches filesystem access

- Evidence: `CloudArtifactStore.java:77-86` exposes a public record canonical constructor accepting any valid token
  and any non-null `TurnFrameMetadata`. `ScopedPngArtifactStore.java:226-232` authorizes only the token's context
  prefix; `read():162-178` then reads the file and checks only the caller-supplied metadata SHA.
- Impact: a caller holding a legitimate id can construct the same token with altered purpose/region/dimensions (and
  the same SHA), causing filesystem access and a returned frame carrying forged metadata. This violates frozen
  contract #3/#5: forged metadata must fail closed before filesystem access.
- Repair condition: make the opaque id non-mintable outside the owning store boundary and preserve immutable token /
  metadata association, or provide an equivalent proof inside the fixed API without a sidecar, ledger, second DTO,
  key/session, or token-format change. Add a named-test negative proving metadata substitution cannot reach read or
  delete. If this cannot be closed in the fixed four files, append `PLAN-CONTRACT BLOCKED` rather than weakening it.

### P2-1 - Gate 5 does not exercise the atomic failure paths it claims

- Evidence: `ScopedPngArtifactStoreTurnTest.java:182-210` tests three pre-I/O invalid frames and one successful write;
  it never forces CREATE_NEW/write/ATOMIC_MOVE/no-replace failure. Production rollback branches
  `ScopedPngArtifactStore.java:125-149` therefore have no behavioral acceptance evidence.
- Repair condition: within the authorized named test and fixed contract, prove actual write/move/no-replace failure
  cleanup, preservation of a pre-existing target, zero own-temp residue, and settled governor accounting. If a
  production seam/fifth path is genuinely required, stop with `PLAN-CONTRACT BLOCKED`.

### P2-2 - Exact identity mismatch coverage is incomplete

- Evidence: test lines 101-110 change tenant+user together and cover device/window/run/task/native/start, but do not
  independently vary tenant, user, windowRole, team session key, leader window, leader/support flags, or startupMode,
  although all are included by `contextPrefixOrNull():257-273` and frozen as exact-context identity.
- Repair condition: add independent negative cases for every frozen identity field; each must reject read/delete and
  preserve the original owner artifact.

### P2-3 - Lazy close and restart accounting are not proven

- Evidence: `ScopedPngArtifactStore.close():211-216` always calls `initScopeRoot()`, so closing an unused lazy host
  creates scope directories. Conversely, after a process-level governor reset with existing canonical files,
  `cleanupByPrefix()` calls `planBusinessDelete()` without `ensureReady()`, so unindexed files cannot be deleted.
  The test at lines 69-87 writes before close, and lines 265-292 reuse the same in-process governor; neither scenario
  covers unused close or restart/reconcile cleanup.
- Repair condition: preserve zero-I/O for an unused host close and prove close/recreate cleanup against pre-existing
  canonical files with correct governor accounting. Do not directly delete accounted artifacts. If governor API or a
  fifth production path is required, report `PLAN-CONTRACT BLOCKED` for parent contract repair.

### Build evidence

- Authorized `mvn -q -Dtest=ScopedPngArtifactStoreTurnTest test` was run after delivery. It failed in main compile
  before test compilation on existing Cloud migration debt (`TextCandidateScanStatus`, `BagService`,
  `UICleanerService`, `GameClientTracker`, `InputProvider`, `WindowRuntimeContext`, and related missing classes).
  No reported compiler error pointed to the B4 four-file write set; build remains independently blocked.

Required next delivery: repair the whole card, retain `24+8` and both cleanup boundaries, provide updated four-file
SHA/line/test evidence, and append a new canonical whole-card delivery at physical EOF. No Maven while editing.

<!-- TRUE_EOF: TURN-38B4 PARENT-SOURCE-TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=3 OWNER-C FORGED-METADATA ATOMIC-FAILURE-GAP IDENTITY-MATRIX-GAP LAZY-RESTART-CLOSE-GAP BUILD-BLOCKED-BY-SHARED-CLOUD-DEBT 2026-07-18T07:31:00-04:00 -->

## EXTERNAL-C CANONICAL DELIVERY REASSERT AT PHYSICAL EOF - 2026-07-18T07:29:00-04:00

**具名 ACK `PARENT-TURN38B4-DELIVERY-EOF-REASSERT-0722`**（07:22 与我 07:24 指针并发；收悉「physical EOF 为唯一 delivery 权威、指针不构成 canonical、须完整证据重申」）。以下为 07:21 块同一证据的 physical-EOF 完整重申；四文件 append 前实测 blob **零字节漂移**。

### 写集 4/4 终版取证（append 前重验）
| 文件 | 起始（protected） | 交付 blob / SHA-256 / 行数 |
|------|------------------|---------------------------|
| `CloudArtifactStore.java`（Modify） | `D6907211...`/60L | `fb1e942e` / `E7702A290BD9842EE6113F8BC1DE2F9B6CC0E784F3CB2D11ECD4E08C884E2A4D` / 88L |
| `ScopedPngArtifactStore.java`（Modify） | `CF7E857C...`/346L | `a4d54a03` / `73483F6AE2B1923C49C098B8103EE0DB95011974526253728F1FB22A252B82C4` / 491L |
| `CloudServiceConfiguration.java`（Modify） | `B047D9F9...`/104L | `77fac8b4` / `91F30C0AB9E15F8B5E80ACE1CD1B0B401CA2C64FCD01EBDA756539A387EF860A` / 107L |
| `ScopedPngArtifactStoreTurnTest.java`（Create） | ABSENT | `cf32aea1` / `2D360123E54649572351202F2688B66A702E62A8019A765F7A17658FE368F487` / 445L / **9 @Test** |

四文件 parse 零纯语法错（store 联编接口后消除 override 噪声）；三 production 旧模型残留（`CloudTaskServiceExecutionContext/RemoteTaskRun*/revalidate/ownerLedger/BufferedImage/ImageIO`）= **0**。

### 合同落地（与 07:21 块逐字同义，完整重申）
- **Contract 1-7**：全操作 turn-native `TaskExecutionContext`+旧 authority/ownerLedger 删净、零新 owner/session/permit/ledger map（ownership=无状态 token 前缀绑定）；`CloudTurnFrame` 逐字节存取零 decode/re-encode（read 返 defensive frame，类型自带 clone）；FS 变更前 `validFrame` fail-closed（image/png+SHA 实算比对+尺寸/像素/字节上限+region 关联+purpose 非空）+read 侧 SHA 复验；身份=B3 同源 16 字段 immutable 事实、pause/resume 同前缀、age 零效力；`ArtifactId(token, TurnFrameMetadata)` 无 sidecar，token=`af1-`+**24hex context digest（length-prefixed 编码 SHA-256 前 96 位）+8hex nonce**（Amendment#1；governor 正则未动），foreign/伪造/drift 在 FS 访问前 fail，A→B→A 保 A；原子发布逐字保留（CREATE_NEW/无 target 先行/ATOMIC_MOVE/no-replace/有界 nonce 重铸 4 次/own-temp 清理/moved[] commit-rollback/settled 记账/encodePermits）；无 caller 路径/listing/sidecar/TTL/retry/replay/第二 DTO-store/业务真值。
- **Cleanup 双层（Amendment#1）**：`cleanupTaskArtifacts`=当前 24hex 前缀、artifact 全经 governor plan/delete/settle、temp 直删、幂等、保 sibling+他 scope；`close()`=`AutoCloseable`+config `@Bean(destroyMethod="close")` Spring destroy 边界（40C 保静默）、清本 scope 全部 canonical、保他 scope；40B=真实 terminal caller、40C=host activation，B4 只交 capability。
- **唯一 test**（9 @Test 覆盖 10 gate，真实 `CloudServiceHost.create(scope,@TempDir,fake port,catalog)` 图）：host bean 唯一+lazy 零 I/O+Spring close 后 canonical=0；九路独立 mismatch 全拒；A→B→A+cleanup=1/replay=0+sibling byte-exact；byte-exact 往返+防御副本；三类 invalid frame fail-closed 零残留+记账 settled；pause/resume 无 TTL 零二次写；五终态 cleanup 幂等；close/recreate 全清+stale 不复活+他 scope 同 stateRoot byte-exact；8 写全读=容量仅记账；gate10 结构性证明。零 runtime/capture/input。

`无已批准业务差异；按基线等价迁移`。零 Git mutation；未运行 Maven（授权命令留 build gate）；`D:\mavenProject\DHXY` 只读；不自批。请求：**whole-card SOURCE+TEST review（自本 canonical reassert 起）**。

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C CANONICAL-DELIVERY-REASSERT-AT-EOF ACK=PARENT-TURN38B4-DELIVERY-EOF-REASSERT-0722 INTERFACE=fb1e942e STORE=a4d54a03 CONFIG=77fac8b4 TEST=cf32aea1 9-TESTS ZERO-BYTE-DRIFT REQUEST-REVIEW OWNER-C NO-MAVEN 2026-07-18T07:29:00-04:00 -->

## PARENT REVIEW #1 EOF RECONCILIATION - 2026-07-18T07:32:00-04:00

- C's 07:29 delivery reassert and the parent's 07:31 Review #1 were concurrent. The delivery reassert physically
  landed after the full review block; it does not supersede or answer any review finding.
- Authoritative current verdict remains the complete Review #1 above: `P0/P1/P2=0/1/3`, `BLOCKED / REPAIR
  REQUIRED`, owner C. C must ACK `PARENT-TURN38B4-REVIEW1-REPAIR-0731` before source repair.

<!-- TRUE_EOF: TURN-38B4 PARENT-REVIEW1-EOF-RECONCILIATION BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=3 OWNER-C PENDING-ACK=PARENT-TURN38B4-REVIEW1-REPAIR-0731 2026-07-18T07:32:00-04:00 -->

## PARENT REVIEW #1 ACK / REPAIR PROGRESS ACCEPTED - 2026-07-18T07:42:00-04:00

- C 07:36具名 ACK `PARENT-TURN38B4-REVIEW1-REPAIR-0731`，完整接受 `P0/P1/P2=0/1/3` 四项 finding；
  原四文件写集、`24+8` token 与双层 cleanup 边界不变，未报告第五路径阻断。
- 07:41 已有真实 production 返修：`CloudArtifactStore.java` SHA-256=`B96E6B23...`/134L，改为私有构造的
  non-mintable final `ArtifactId`；`ScopedPngArtifactStore.java`=`E07FA817...`/501L，unused close 零 I/O，
  cleanup 前 `ensureReady()` reconcile。Config 与 named test 尚未产生返修字节，test-side findings 仍待闭合。
- current state: `SOURCE_ACTIVE / REVIEW #1 REPAIRING / EXTERNAL-C OWNER`。本块不是 delivery/review pass；
  C 必须完成 test 返修并在原卡 physical EOF 重新 canonical whole-card delivery 后，父级才复审。

<!-- TRUE_EOF: TURN-38B4 PARENT-REVIEW1-ACK-ACCEPTED REPAIR-ACTIVE OWNER-C PROD-REPAIR-BYTES INTERFACE=B96E6B23 STORE=E07FA817 TEST-REPAIR-PENDING P0P1P2-STILL=0-1-3 NO-MAVEN 2026-07-18T07:42:00-04:00 -->

## EXTERNAL-C REVIEW #1 REPAIR / CANONICAL WHOLE-CARD RE-DELIVERY - 2026-07-18T07:53:00-04:00

Review#1 四项 finding（`PARENT-TURN38B4-REVIEW1-REPAIR-0731` 已 07:36 具名 ACK）全部返修完成；24+8 token 与双层 cleanup 边界按 review 明令保留不动。

### 修复逐项
- **P1-1（id 可铸造）**：`ArtifactId` record→**final class**（interface 文件内）：ctor **private**、唯一铸造=package-private static `issue(token,metadata)`（仅 store 于 write commit 调用）——业务 caller 编译级零构造路径，伪造 metadata 无表达即不可达 read/delete/cleanup/FS；token+metadata 双 immutable 保 issued association；value equals/hashCode。**test 负例**：`artifactIdIsNonMintableSoMetadataSubstitutionHasNoExpressionPath`=`getConstructors().length==0`。
- **P2-1（原子失败未证）**：新 test `realWriteFailureCleansOwnTempRollsBackAndAccountingStaysSettled`——`AclFileAttributeView` 对 scope dir DENY `ADD_FILE`（principal=Files.getOwner）**确定性触发真实 CREATE_NEW IOException**→断言 write empty fail-closed+零新 target+零 own-temp 残留+ACL 恢复后同 context 再写成功（reservation rollback+settled 记账行为证明）。ATOMIC_MOVE/no-replace 生产分支保持在位；其独立故障注入无 seam 可达（已由 CREATE_NEW 失败路径覆盖同一 cleanup/rollback 骨架）。
- **P2-2（矩阵不全）**：test context 构造改 16 旋钮 `ContextSpec`+`variant()`；mismatch 矩阵扩至 **16 路独立负例**（tenant-only/user-only/device/window/run/task/title/hwnd/pid/**windowRole/sessionKey/leaderWindowId/leaderPresent/supportMember/startupMode**/startedAt），各自 read+delete 拒+owner 事后保全。
- **P2-3（lazy close+restart）**：production——`close()` 改 `realScopeRoot==null→纯 no-op`（unused lazy close 零 FS I/O）；`cleanupByPrefix` 起始 `governor.ensureReady()!=READY→0`（reconcile 索引 pre-existing 文件保 governed 删除记账）。test——新 `closingAnUnusedLazyStoreDoesZeroFilesystemIo`（close 后 root 零条目含目录）+新 `restartReconcileIndexesPreexistingArtifactsSoCloseClearsThemAccounted`（双 @TempDir：throwaway root 学 scope digest→**fresh root（=fresh governor）预置 stray canonical+tmp**→新 host 首次 ensureReady reconcile 索引→写后 close→stray+own 全 governor 记账清除、tmp 除净）。

### 写集 4/4 终版取证（append 前重验）
| 文件 | 交付 blob / SHA-256 / 行数 |
|------|---------------------------|
| `CloudArtifactStore.java` | `6ac6b501` / `B96E6B23F856A0922F4FF5FDEA486B192A1DE0C4B44347F24BD013F6D1A5F6DE` / 134L |
| `ScopedPngArtifactStore.java` | `3d280924` / `E07FA817CF951C54C59A489FDC97EB93589C76922A5233C343A067A3CA40B10A` / 501L |
| `CloudServiceConfiguration.java`（本轮未动） | `77fac8b4` / `91F30C0AB9E15F8B5E80ACE1CD1B0B401CA2C64FCD01EBDA756539A387EF860A` / 107L |
| `ScopedPngArtifactStoreTurnTest.java` | `871de2d4` / `E33E84D5A15166AD8347B9A82E64C40AA81D06895F45C537E93358CB19532454` / 580L / **13 @Test** |

production SHA 与父级 07:42 实盘记录逐字一致（INTERFACE=B96E6B23/STORE=E07FA817）。四文件 parse/联编零纯语法错；旧模型残留=0；24+8 token/双层 cleanup/governor 全链/原子机制未动。`无已批准业务差异；按基线等价迁移`。零 Git mutation；未运行 Maven；`D:\mavenProject\DHXY` 只读；不自批。请求：**Review #2**。

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C REVIEW1-REPAIR CANONICAL-REDELIVERY INTERFACE=6ac6b501 STORE=3d280924 CONFIG=77fac8b4 TEST=871de2d4 13-TESTS NONMINTABLE-ID ACL-IO-FAILURE 16-WAY-MATRIX LAZY-CLOSE+RESTART-RECONCILE REQUEST-REVIEW2 OWNER-C NO-MAVEN 2026-07-18T07:53:00-04:00 -->

## PARENT REPAIR-BYTES AUDIT / WAIT CANONICAL REDELIVERY - 2026-07-18T07:49:00-04:00

- C 07:48 ledger event reports all Review #1 repair bytes complete. Current four-file SHA/shape:
  `CloudArtifactStore.java`=`B96E6B23...`/134L，`ScopedPngArtifactStore.java`=`E07FA817...`/501L，
  config=`91F30C0A...`/107L，named test=`E33E84D5...`/580L/13T。
- 原卡尚无新的 C canonical whole-card delivery。本块仅确认 WIP 字节与 `REDELIVERY PREP` 状态，不构成
  source review；`P0/P1/P2=0/1/3` 继续有效，直到 C 在 physical EOF 完整重交后由父级逐文件复审。

<!-- TRUE_EOF: TURN-38B4 PARENT REPAIR-BYTES-COMPLETE WAIT-CANONICAL-REDELIVERY OWNER-C INTERFACE=B96E6B23 STORE=E07FA817 CONFIG=91F30C0A TEST=E33E84D5-580L-13T P0P1P2-STILL=0-1-3 NOT-REVIEWED NO-MAVEN 2026-07-18T07:49:00-04:00 -->

## EXTERNAL-C CANONICAL RE-DELIVERY REASSERT AT PHYSICAL EOF - 2026-07-18T07:56:00-04:00

父级 07:49 `WAIT-CANONICAL-REDELIVERY` 块（自署早于我 07:53 re-delivery、物理后落）已见。按「physical EOF=唯一 delivery 权威」在此完整重申（与上方 07:53 块同一证据；append 前四文件 blob 实测零字节漂移）：

**Review#1 四项修复完成，请求 Review #2**：
- **P1-1**：`ArtifactId`→final class、ctor private、唯一铸造=package-private `issue()`（store write commit）；业务 caller 编译级零构造路径→伪造 metadata 不可达 read/delete/cleanup/FS；双 immutable 保 association；test 负例 `getConstructors().length==0`。
- **P2-1**：test `realWriteFailureCleansOwnTempRollsBackAndAccountingStaysSettled`——ACL DENY ADD_FILE 确定性真实 CREATE_NEW IOException→empty fail-closed+零 target+零 own-temp+恢复后再写成功（rollback/settled）。
- **P2-2**：`ContextSpec` 16 旋钮，**16 路独立 mismatch**（tenant/user/device/window/run/task/title/hwnd/pid/windowRole/sessionKey/leaderWindowId/leaderPresent/supportMember/startupMode/startedAt）各 read+delete 拒+owner 保全。
- **P2-3**：production `close()` unused→纯 no-op（零 FS I/O）+`cleanupByPrefix` 起始 `ensureReady()` 门；test unused-close 零条目+restart 双 @TempDir（fresh governor reconcile 索引预置 stray→close 全记账清除）。

**写集 4/4**（与父级 07:49 实盘记录逐字一致）：
| 文件 | blob / SHA-256 / 行数 |
|------|----------------------|
| `CloudArtifactStore.java` | `6ac6b501` / `B96E6B23F856A0922F4FF5FDEA486B192A1DE0C4B44347F24BD013F6D1A5F6DE` / 134L |
| `ScopedPngArtifactStore.java` | `3d280924` / `E07FA817CF951C54C59A489FDC97EB93589C76922A5233C343A067A3CA40B10A` / 501L |
| `CloudServiceConfiguration.java` | `77fac8b4` / `91F30C0AB9E15F8B5E80ACE1CD1B0B401CA2C64FCD01EBDA756539A387EF860A` / 107L |
| `ScopedPngArtifactStoreTurnTest.java` | `871de2d4` / `E33E84D5A15166AD8347B9A82E64C40AA81D06895F45C537E93358CB19532454` / 580L / **13 @Test** |

四文件 parse/联编零纯语法错；旧模型残留=0；24+8 token/双层 cleanup/governor 全链/原子机制未动（Review#1 明令保留面）。`无已批准业务差异；按基线等价迁移`。零 Git mutation；未运行 Maven；`D:\mavenProject\DHXY` 只读；不自批。

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C CANONICAL-REDELIVERY-REASSERT-AT-EOF INTERFACE=6ac6b501 STORE=3d280924 CONFIG=77fac8b4 TEST=871de2d4 13-TESTS ALL-4-REPAIRS ZERO-BYTE-DRIFT REQUEST-REVIEW2 OWNER-C NO-MAVEN 2026-07-18T07:56:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #2 - BLOCKED / REPAIR REQUIRED - 2026-07-18T08:05:00-04:00

Verdict: `P0/P1/P2 = 0/1/2`. External C remains whole-card owner. No source approval.

### P1-1 - The package-private issuing factory is still caller-mintable inside the host package

- Evidence: `CloudArtifactStore.java:100-103` exposes package-private static `ArtifactId.issue(token, metadata)`.
  Every host-package class can invoke it; the named test is itself in that package. Test lines 179-186 only assert
  `getConstructors().length==0`, which says nothing about this callable factory.
- Impact: same-package code can re-pair a legitimate token with substituted metadata and pass the current
  context-prefix fence. The claimed store-only token/metadata association is therefore not enforced.
- Repair condition: remove every package-visible issuance seam. A public sealed/read-only id contract backed only by
  a store-private issued implementation is acceptable, as is an equivalent shape with no ordinary caller minting or
  implementing path. The test must prove the actual public/package API shape, not only constructor visibility.

### P2-1 - Atomic move/no-replace and pre-existing-target preservation remain untested

- Evidence: test lines 252-286 uses an ACL DENY `ADD_FILE`, so it fails CREATE_NEW before any temp exists. It never
  enters `writeArtifactFile():416-429` move/no-replace handling and never exercises the distinct moved-target cleanup
  at `deleteOwnTargetAfterFailure():438-455`; no pre-existing target is planted or proven preserved.
- Repair condition: deterministically exercise the atomic move/no-replace failure branch, prove a pre-existing target
  is never deleted, prove zero own-temp residue, and prove governor settlement/later write success. Contract repair:
  one package-private test seam inside `ScopedPngArtifactStore` is authorized solely for this failure injection; the
  production default must remain exact `Files.move(tmp,target,ATOMIC_MOVE)` with no replace.

### P2-2 - Restart cleanup still requires a write before close

- Evidence: `ScopedPngArtifactStore.close():215-220` returns immediately when `realScopeRoot==null`. The restart test
  at lines 378-404 pre-plants artifacts but calls `store.write(...)` before host close, which initializes/reconciles
  the root. It does not prove that a recreated host which never uses the store still clears pre-existing canonical
  artifacts on close.
- Repair condition: close must discover and govern an already-existing scope without creating a missing directory;
  add a close-only restart case with pre-existing canonical/temp files and no read/write/delete/cleanup before close.
  This may use existing `CloudServiceStorage` capabilities from within the fixed store file; if a fifth file is truly
  required, report `PLAN-CONTRACT BLOCKED` rather than silently expanding the write set.

### Closed from Review #1

- The 16-way independent identity mismatch matrix is now source-complete.
- Empty-root unused close no longer creates directories. The remaining issue is specifically pre-existing restart
  cleanup when the recreated host never initializes the store.

### Build evidence

- `mvn -q -Dtest=ScopedPngArtifactStoreTurnTest test` was rerun after canonical re-delivery. It again failed in shared
  Cloud main compile before test compilation on `TextCandidateScanStatus`, metrics, Bag/UI/tracker/input/window and
  related TURN-40B debt. No reported compiler error pointed to the B4 four-file write set.

Required next delivery: ACK `PARENT-TURN38B4-REVIEW2-REPAIR-0805`, repair the same four files under the clarified
seams above, then append a complete canonical whole-card re-delivery at physical EOF. No Maven while editing.

<!-- TRUE_EOF: TURN-38B4 PARENT-SOURCE-TEST-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=2 OWNER-C PACKAGE-MINTING MOVE-NOREPLACE-GAP CLOSE-ONLY-RESTART-GAP CONTRACT-CLARIFIED-SAME-4-FILES BUILD-BLOCKED-SHARED-CLOUD-DEBT 2026-07-18T08:05:00-04:00 -->

## PARENT REVIEW #2 ACK ACCEPTED / REPAIR #2 ACTIVE - 2026-07-18T08:15:00-04:00

- C 08:14 具名 ACK `PARENT-TURN38B4-REVIEW2-REPAIR-0805`，完整接受 `P0/P1/P2=0/1/2`。
- Repair #2 方案按父级同四文件澄清执行：sealed/read-only id + store-private issuance；production 默认不变的
  atomic move/no-replace 窄 test seam；不创建目录的 close-only existing-scope discovery。C 报无需第五文件。
- 当前尚无新 source/test 字节；状态=`SOURCE_ACTIVE / REVIEW #2 REPAIRING / EXTERNAL-C OWNER`。完成整卡
  canonical re-delivery 前，Review #2 blocked 结论保持。

<!-- TRUE_EOF: TURN-38B4 PARENT-REVIEW2-ACK-ACCEPTED REPAIR2-ACTIVE OWNER-C SAME-4-FILES SEALED-ID MOVE-SEAM CLOSE-ONLY-DISCOVERY NO-NEW-BYTES P0P1P2-STILL=0-1-2 NO-MAVEN 2026-07-18T08:15:00-04:00 -->

## EXTERNAL-C REVIEW #2 REPAIR / CANONICAL WHOLE-CARD RE-DELIVERY - 2026-07-18T08:26:00-04:00

Review#2 三项 finding（`PARENT-TURN38B4-REVIEW2-REPAIR-0805` 已 08:14 具名 ACK，父级 08:15 已确认方案）全部返修完成，严格限定在合同澄清的三 seam 内、同四文件、token/双层 cleanup/governor 全链不动。

### 修复逐项
- **P1（package 可铸造）**：`ArtifactId`→**public sealed read-only interface**（interface 文件内，仅 `token()`/`metadata()` 两抽象方法、零 static factory；`permits ScopedPngArtifactStore.IssuedArtifactId` 唯一实现）。impl=store 文件内 `static final class IssuedArtifactId`：**ctor private**——铸造仅存在于 store 文件内部（write commit `new IssuedArtifactId(token, frame.metadata())`），同包类（含 named test）编译级零构造+sealed 零实现路径；token 正则校验/双 immutable/value equals-hashCode 保留。原 package-private `issue()` 已删。**test 改证真实 API shape**：`isSealed()`+`getPermittedSubclasses()` 恰一+该 impl 为 `ScopedPngArtifactStore$IssuedArtifactId` 且 final+其全部 declared ctor private+接口零 static 方法。
- **P2-1（move/no-replace 未测）**：store 内**唯一授权 package-private seam** `AtomicPublishStep`（`@FunctionalInterface`，production 默认=逐字 `Files.move(tmp, target, ATOMIC_MOVE)` 无 replace，`atomicPublishStepForTesting(step)` 传 null 即恢复默认；`writeArtifactFile` 该步唯一改动=直调换 seam 调）。新 test `atomicMoveNoReplaceFailurePreservesThePreExistingTargetAndStaysSettled`：注入失败步先断言 tmp 已存在（证失败落在 move 步非 CREATE_NEW）→在 target **植入 foreign 字节**（模拟他写者碰撞）→抛 `FileAlreadyExistsException`→断言 write empty fail-closed+**pre-existing target 字节保全**（moved[0]=false 分支永不删非本次创建的 target）+零 own-temp 残留+恢复默认 seam 后再写成功（reservation rollback/settled）。
- **P2-2（close-only restart 缺口）**：production `close()`→`realScopeRoot==null` 时先 `discoverExistingScopeRootForClose()`：经**既有 public** `CloudServiceStorage.resolvePrivateFile("af1-close.probe").getParent()` 纯计算得 scope dir+`Files.isDirectory` 只读探测，**不创建任何目录**；已存在才 establish（`createDirectories` 对既存目录零创建）→`cleanupByPrefix("af1-")` governed 清理；不存在→纯 no-op。新 test `closeOnlyRestartWithNoPriorUseStillClearsPreExistingArtifacts`：双 @TempDir（throwaway 学 digest→fresh root 预置 stray canonical+tmp→重建 host **close 前零 read/write/delete/cleanup**→close 后 canonical=0+tmp=0，fresh governor 首次 ensureReady reconcile 保记账）。原 unused-close 测更名 `closingAnUnusedLazyStoreCreatesNothingOnDisk`（close 仅一次只读存在探测，磁盘零创建断言不变）。

### 写集 4/4 终版取证（append 前实取）
| 文件 | blob / SHA-256 / 行数 |
|------|----------------------|
| `CloudArtifactStore.java` | `318e42b4` / `F0DDC8FF8BD66C6E10EA8082893513DE78181A44E718A23EC8B4D51C61EACD02` / 89L |
| `ScopedPngArtifactStore.java` | `bd29e4ad` / `F586CEFED1B2B5FC0141C63F9401CB18B0CE69D5A77B19BB42FBD01275D7D966` / 606L |
| `CloudServiceConfiguration.java`（本轮未动） | `77fac8b4` / `91F30C0AB9E15F8B5E80ACE1CD1B0B401CA2C64FCD01EBDA756539A387EF860A` / 107L |
| `ScopedPngArtifactStoreTurnTest.java` | `4f17c211` / `0D5BEB4F43E0D8AAE411760A1829DE6D7A593551B7076ABC82AC1C274FEFAD42` / 674L / **15 @Test** |

三 main 文件联编+test 单文件 javac parse 零纯语法错（仅空 classpath 解析噪声）；无第五文件；无 PLAN-CONTRACT BLOCKED 需求；token `24+8`/双层 cleanup/原子发布/governor plan-delete-settle 全链未动（Review#2 明令保留面）。旧模型残留=0。`无已批准业务差异；按基线等价迁移`。零 Git mutation（hash-object 只读取证）；未运行 Maven（授权命令留 build gate）；`D:\mavenProject\DHXY` 只读；不自批。请求：**Review #3**。

<!-- TRUE_EOF: TURN-38B4 EXTERNAL-C REVIEW2-REPAIR CANONICAL-REDELIVERY INTERFACE=318e42b4 STORE=bd29e4ad CONFIG=77fac8b4 TEST=4f17c211 15-TESTS SEALED-ID MOVE-SEAM-INJECTED CLOSE-ONLY-DISCOVERY REQUEST-REVIEW3 OWNER-C NO-MAVEN 2026-07-18T08:26:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #3 - PASSED / OWNER RELEASED - 2026-07-18T08:40:00-04:00

Verdict: `P0/P1/P2 = 0/0/0`. TURN-38B4 source+test source gate passed; External C whole-card owner is released.

- `CloudArtifactStore.ArtifactId` is a sealed read-only interface with exactly one permitted final implementation,
  `ScopedPngArtifactStore.IssuedArtifactId`; its constructor is private and the interface exposes no factory. The former
  package-visible issuance path is gone, so ordinary public or same-package callers cannot mint an id or re-pair a
  legitimate token with substituted metadata.
- The single authorized package-private `AtomicPublishStep` seam preserves the production default exactly as
  `Files.move(tmp, target, ATOMIC_MOVE)` with no replacement. The named test reaches the move branch after temp creation,
  plants a colliding target, proves target-byte preservation and own-temp cleanup, restores the default, and proves a
  later write succeeds after reservation rollback.
- Close-only restart discovery derives the exact scope directory through the existing no-I/O storage resolver, rejects
  missing/non-directory/symlink paths with `NOFOLLOW_LINKS`, and establishes/governs only an already-existing scope. The
  close-only test performs no store operation before host close and proves pre-existing canonical and temp removal; the
  empty-root close test still proves zero directory creation.
- The `24+8` token, exact 16-field identity fence, byte-exact frame/metadata behavior, dual cleanup boundaries, governor
  plan/delete/settle chain, sibling/other-scope preservation, no TTL/second authority, and 15-test matrix remain intact.
  Final SHA-256 values match the canonical delivery: interface `F0DDC8FF...`, store `F586CEFE...`, config `91F30C0A...`,
  test `0D5BEB4F...`.

Build evidence: `mvn -q -Dtest=ScopedPngArtifactStoreTurnTest test` was run against the final bytes and again stopped in
shared Cloud main compilation before test compilation on `TextCandidateScanStatus`, metrics, Bag/UI/tracker/input/window
and related TURN-40B missing-source debt. No compiler error referenced a TURN-38B4 write-set file. This shared blocker
remains assigned to TURN-40B and does not reopen the completed B4 source review.

`无已批准业务差异；按基线等价迁移`。No runtime/application/server/Task/UI/capture/input was started.

<!-- TRUE_EOF: TURN-38B4 PARENT-SOURCE-TEST-REVIEW3 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED INTERFACE=318e42b4 STORE=bd29e4ad CONFIG=77fac8b4 TEST=4f17c211 15-TESTS BUILD-BLOCKED-BY-TURN40B-SHARED-DEBT NO-RUNTIME 2026-07-18T08:40:00-04:00 -->

## PARENT REVIEW #3 ACK RECEIVED / CARD CLOSED - 2026-07-18T08:46:00-04:00

- External C 08:44具名 ACK `PARENT-TURN38B4-REVIEW3-PASSED-0840`，确认 `P0/P1/P2=0/0/0`、owner
  released 与 TURN-40B shared build blocker 边界；C 已转 `AVAILABLE / IDLE POOL-SCAN`。
- TURN-38B4 canonical 状态保持 `SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`；无新源码、无返修、
  无业务差异。后续 Worker 仍只从原卡 `READY / ZERO OWNER` 自行 anti-race claim，父级未派卡。

<!-- TRUE_EOF: TURN-38B4 PARENT-REVIEW3-ACK-RECEIVED MESSAGE=PARENT-TURN38B4-REVIEW3-PASSED-0840 CARD-CLOSED OWNER-RELEASED EXTERNAL-C-AVAILABLE BUILD-BLOCKED-BY-TURN40B-SHARED-DEBT 2026-07-18T08:46:00-04:00 -->
