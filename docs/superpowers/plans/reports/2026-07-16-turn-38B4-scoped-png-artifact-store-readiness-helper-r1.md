# CR271 TURN-38B4 Scoped PNG Artifact Store Readiness PRECHECK R1

## 0. Role And Scope

- Evidence cutoff: `2026-07-16T07:48:08.606-04:00`.
- Role: non-binding readiness helper only. This report does not implement, review, claim, assign an owner, or make the parent decision for TURN-38B4.
- Sole write in this pass: this report. It did not exist before this pass.
- No Java, test, fixed card, authoritative plan, `docs/ACTIVE_WORK.md`, CR card, matrix, dashboard, Maven/JUnit/compile/package, application/server/Task/runtime, UI/capture/input, staging, commit, checkout, reset, clean, branch, or other Git mutation was performed.
- Both repositories' dirty and untracked files were treated as protected evidence.

## 1. Authority And Read Snapshot

The following files were read completely at the recorded snapshot. The line counts and SHA-256 values make this precheck reproducible without treating a concurrent worktree as a clean baseline.

| Authority/evidence | Lines | SHA-256 |
|---|---:|---|
| `AGENTS.md` | 392 | `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md` | 1349 | `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `docs/ACTIVE_WORK.md` | 79898 | `7FAE5E8C39C69C3E3346A4D65B248CA5FCC3BCBE72EC0B346C189A555C785619` |
| authoritative card plan | 1690 | `99BE035AD7693D6FD636F1016580ECBC9EDB7D174026B0CCB603DF02EBBF8A2F` |
| HTTPS turn protocol | 383 | `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| TURN-13H fixed card | 163 | `ECCBB1670A33E4313BA18B8E9ED9517E5FD35537C070816A17F26B6B753305B0` |
| TURN-17 fixed card | 135 | `01C718AB3D530D56B906AA6798311A8EC1DDE8F16C3C912429E1EF9DA94A7FED` |
| TURN-38A readiness report | 362 | `04C8C2722A3D6E2C62ED876DB0CB6073DC309595F35A96A76EC63D692CFE456F` |
| historical artifact/template adapter report | 1246 | `BCE5A9F8D3DA85E49D23BDF8A239F867C5E3F0A29C901BE9281697A94BA09DFA` |

Relevant source and tests were also read in full: `CloudArtifactStore`, `ScopedPngArtifactStore`, `CloudArtifactCapacityGovernor`, `CloudServiceStorage`, `CloudServiceScope`, `CloudServiceConfiguration`, `CloudServiceHost`, old `CloudTaskServiceExecutionContext`, dual-path `TaskExecutionContext`, `CloudQuestLocalServiceClient`, `CloudTurnFrame`, TURN-13H's host test, and TURN-17's client/test sources. No `ScopedPngArtifactStoreTurnTest.java` exists at this snapshot.

### 1.1 Worktree Snapshot

| Repository | Branch / HEAD | Read-only status count |
|---|---|---:|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 655 entries: 44 tracked dirty, 611 untracked |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 550 entries: 9 tracked dirty, 541 untracked |

The three current production targets are all protected Cloud untracked files, not HEAD-restorable baselines:

| Current file | Status | SHA-256 |
|---|---|---|
| `host/CloudArtifactStore.java` | `??` | `D690721110DB4A980E41934C9729BEC598900C70CF6615D7189EFF7EC29FBE9D` |
| `host/ScopedPngArtifactStore.java` | `??` | `CF7E857C9AF293F1FF2C5C2D25AEF54B76F0FEA90A7CB1DD08CA0B86E6B3B151` |
| `host/CloudServiceConfiguration.java` | `??` | `B047D9F910C724083B9594D431ED31DB1601BEFCC18F179ABF263D4D23A8199D` |
| future `host/ScopedPngArtifactStoreTurnTest.java` | absent | n/a |

Related read-only infrastructure is likewise untracked: `CloudArtifactCapacityGovernor.java` SHA `B3DB66C7F0C43FC33191008869795761DD29555CEE672661A9CFCF19D401F363` and `CloudServiceStorage.java` SHA `EF105DD1DA632BFED7241E0F590FEDA99AA247538EF114A21D6FC38F4E75EA0C`.

## 2. Authoritative TURN-38B4 Contract

1. Plan section 14.1 lines `1037-1043` makes sections 16-18 authoritative over earlier conflicting prose. A planned card is not claimable; extra files require a parent plan revision.
2. Registry line `1158` fixes direct `startDependsOn` as `S=17+38A+13H` and identifies this pass as readiness only.
3. Exact production write set is fixed at plan lines `1321-1323` to three Cloud files: `CloudArtifactStore.java`, `ScopedPngArtifactStore.java`, and `CloudServiceConfiguration.java`.
4. Exact test ownership is fixed at plan line `1649` to the single class `host/ScopedPngArtifactStoreTurnTest`, with `STATE+IMG`: tenant scope, atomic PNG, terminal cleanup, and no shared path.
5. `STATE` requires tenant/user privacy, exact device/window, same state through pause/resume, stale rejection, terminal/restart release, and no TTL (`:1497`). `IMG` requires raw PNG rather than Base64 plus metadata/SHA/dimensions/region/source-step correlation, one frame, and defensive copy (`:1491`).
6. Tests and production belong to one future implementation owner (`:1467-1472`). Tests must use fakes/temp storage only; no real application/server/Task/input is authorized.
7. HTTPS protocol lines `68-80`, `105-126`, `281-290`, and `337-369` fix one raw PNG frame, exact window metadata, same-command Quest detail, no local business retry, no session/ledger, fixed process tenant/user/stateRoot, pixel preservation, and Cloud-owned business interpretation.

No final TURN-38B4 API, path key, cleanup hook, or caller has been frozen beyond those statements. In particular, unfinished TURN-38A must not be guessed into a stable contract.

## 3. Direct Source Gates: `S=17+38A+13H`

| Dependency | Current source evidence | TURN-38B4 precheck effect |
|---|---|---|
| TURN-13H | Registry `:1109` and fixed card `:134-163` record source/test-source review passed, owner released, Maven/Cloud compile pending. It delivered the exact host/configuration capability seam and deliberately left production host creation dormant. | Source gate met. Pending build evidence is not restated as completed. Its earlier write to `CloudServiceConfiguration.java` is serial history, not a current writer collision. |
| TURN-17 | Registry `:1123` and fixed card `:103-135` record source/test-source review passed, owner released, build pending. `CloudQuestLocalServiceClient.java:93-110,139-155,195-230` returns and validates one same-command raw `CloudTurnFrame`; the real Xiuluo caller remains TURN-37. | Source gate met. It supplies a raw-frame contract, not an artifact-store caller or a frozen persistence API. |
| TURN-38A | Registry `:1154` remains readiness evidence with parent audit and real unresolved items. Its own direct gates `34C/35/36/37` are not source-stable, and its report lines `317-344` records DAG, old-SCC, construction, metadata, test-ownership, and untracked-worktree issues. | Source gate not met. TURN-38B4 cannot freeze the context parameter or identity derivation by inference. |

Direct gate count at the evidence cutoff: **2 of 3 source gates met; TURN-38A remains unmet**.

## 4. Real Construction, Caller, Registration, And Close State

### 4.1 Construction Chain That Actually Exists

1. `CloudServiceConfiguration.java:88-92` is the only `new ScopedPngArtifactStore(...)` site. It supplies the host's `CloudServiceScope`, `CloudServiceStorage`, and the process-level governor keyed by `storage.stateRootKey()`.
2. `CloudServiceHost.create(...)` at `:39-65` constructs `CloudServiceStorage`, registers exact scope/storage/shared command port/template catalog, registers `CloudServiceConfiguration`, and refreshes the Spring context.
3. Context refresh eagerly constructs the artifact bean, but `ScopedPngArtifactStore.initScopeRoot()` is lazy (`:221-239`), so bean construction itself does not create an artifact path.
4. `CloudServiceHost.getService(...)` at `:74-75` can expose the interface. The current TURN-13H test creates the real host, but it does not request or invoke `CloudArtifactStore`.

### 4.2 Caller Census

Read-only full-source searches under Cloud `src/main/java` and `src/test/java` found:

- production calls to `CloudServiceHost.create(...)`: **0**;
- test call sites to `CloudServiceHost.create(...)`: six, all in `CloudServiceHostTurnCapabilityContractTest.java:55,78,105-112`;
- production calls to `TaskExecutionContext.turnNative(...)`: **0**; current uses are test fixtures only;
- `CloudArtifactStore.writePng(...)`, `readPng(...)`, and `delete(...)`: **definitions only**, with **zero production and zero test callers**;
- `new ScopedPngArtifactStore(...)`: exactly one configuration site;
- TURN-17's real Quest consumer remains deferred to TURN-37 and does not reference the artifact store.

Therefore, the current code contains a real but dormant bean construction seam, not an activated host and not an artifact lifecycle caller. A future TURN-38B4 card must distinguish these facts explicitly.

### 4.3 Close And Terminal Order That Actually Exists

- `CloudServiceHost.close()` only calls Spring `context.close()` (`CloudServiceHost.java:78-80`).
- `CloudArtifactStore` exposes only single-artifact `writePng/readPng/delete` (`:30,37,44`). It has no task-terminal, window-terminal, restart, or close cleanup operation.
- `ScopedPngArtifactStore` implements neither `AutoCloseable` nor a Spring destruction callback. `CloudServiceConfiguration` declares no destroy method.
- Closing a host discards that store's in-memory `ownerLedger`, but leaves committed PNG files on disk. A later governor reconcile may index/reclaim them for capacity; that is not terminal cleanup.
- Future TURN-40B owns the runtime/registry files and describes terminal release; TURN-40C later owns host activation/close. Neither current production source nor the authoritative TURN-38B4 lines freezes which of those future owners invokes artifact cleanup or the exact order.

Consequently, TURN-38B4 can at most deliver a dormant cleanup capability until a later card receives an explicit caller contract. It must not claim that terminal cleanup is wired merely because the named store test invokes an API directly.

## 5. Current Identity And Path Audit

| Dimension | Current implementation evidence | Delta that still needs parent freeze |
|---|---|---|
| tenant/user | `CloudServiceScope.java:3-18` contains only tenant/user. Store authorization compares only those two host-scope values at `ScopedPngArtifactStore.java:197-206`. | Keep host-fixed tenant/user; request-body text must never select storage. |
| device/window | `CloudServiceScope` deliberately excludes device. The old context contains device/window transitively, but the store does not persist or compare either field. | Freeze exact turn-native device/window source after 38A, normally the immutable `TurnInvocationContext`; do not infer from title/HWND/request text. |
| task/invocation | `OwnerRecord(taskRunId,runRevision)` at `:45,51-52,117-118,213-216` is the only per-artifact task gate. | Freeze task code and invocation identity sources. If `taskRunId` is retained, state that it is an opaque in-memory diagnostic invocation key, not a session/owner/ledger authority. |
| old authority | Public API takes `CloudTaskServiceExecutionContext` (`CloudArtifactStore.java:3,30,37,44`). Authorization calls old `scope()` and `revalidate()` (`ScopedPngArtifactStore.java:201-206`). | Remove reliance on old remote scope/revision/revalidation only after 38A freezes the replacement surface. |
| artifact ID | `ArtifactId` is only random `af1-<32 hex>` and intentionally carries no tenant/path/run identity (`CloudArtifactStore.java:47-58`). | Without `ownerLedger`, freeze a stateless, context-verifiable identity/path rule or another non-ledger mechanism before coding. |
| physical root | `CloudServiceStorage.java:36-40,83-88` hashes only tenant/user. Every artifact is a direct file under that same real scope root (`ScopedPngArtifactStore.java:241-252`). | Define whether "no shared path" means unique final/tmp file names or separate window/task directories. Current exact write set cannot silently change `CloudServiceStorage`. |

Current random tokens normally avoid one fixed shared filename, but all windows and tasks for the same tenant/user share one directory and rely on `ownerLedger` for cross-task isolation. If the requirement means directory-level isolation, the current storage/governor shape does not satisfy it. If it means no shared mutable artifact filename, the parent card must say so explicitly and require stateless context validation after the ledger is removed.

## 6. Atomic PNG, Metadata, Cleanup, And Forbidden State

### 6.1 Existing Atomic-Write Strengths

- Dimensions/pixels/encoded bytes are bounded at `ScopedPngArtifactStore.java:36-39,306-327`.
- The encode permit is acquired before PNG encoding (`:73-78`).
- Capacity victims are settled before a full-key collision-checked reservation; token minting is bounded to four attempts (`:82-111`).
- The attempt writes only its own `<token>.png.tmp` using `CREATE_NEW`, then moves with `ATOMIC_MOVE` and no `REPLACE_EXISTING` (`:241-257`).
- A failed non-moved attempt cleans only its own temp; a moved attempt may clean only the target it proved it created (`:122-139,265-303`).
- Governor startup cleanup recognizes only canonical `af1-<32>.png.tmp` files and uses no-follow/real-parent checks (`CloudArtifactCapacityGovernor.java:265-317`).

These are useful inherited mechanics, but the historical 2026-07-12 parent acceptance applied to the old dormant remote-authority design. It does not settle the new TURN-38B4 identity or lifecycle contract.

### 6.2 Current Deltas Against `STATE+IMG`

| Requirement | Current fact | Precheck consequence |
|---|---|---|
| raw PNG byte identity | `writePng` accepts `BufferedImage` and re-encodes it; `readPng` returns a decoded `BufferedImage`. | TURN-17's raw bytes and SHA are not preserved by this API. |
| metadata/SHA/dimensions/region/sourceStepIndex | No metadata is accepted or persisted. | The full `IMG` profile cannot be asserted without a frozen API/persistence decision. |
| defensive copy | Each read decodes a new image, but there is no raw-byte/metadata return contract. | Parent must freeze whether B4 stores/returns existing `CloudTurnFrame` or a narrower artifact value. |
| pause/resume continuity | Old `runRevision` equality rejects a revised context. | This conflicts with `STATE` if turn-native pause/resume keeps one state without a new revision. |
| terminal physical cleanup | Only deletion by one known `ArtifactId` exists. | No bulk task/window cleanup and no real caller exist. |
| restart release | Restart empties `ownerLedger`, making files unreadable, but files remain until capacity reclaim. | Fail-closed access is not the named terminal/restart physical-cleanup evidence. |
| no TTL | No age-based expiry exists. Governor uses modification time only for capacity FIFO ordering. | Preserve this; tests must prove age alone never expires a live artifact. |
| no session/owner/ledger | API depends on old client-session/run authority; store has an explicit `ownerLedger`. | Current shape must be replaced, not relabeled. |
| capacity accounting | Governor retains `byKey/evicting/pending` maps solely for bounded disk accounting and collision/delete settlement. | Parent must classify this as permitted capacity state, never authorization/lifecycle authority. If all per-artifact retained maps are forbidden, governor is outside B4's write set and the plan must change first. |

Directly scanning/deleting PNG files from `ScopedPngArtifactStore` would desynchronize governor bytes/count/key accounting. Therefore a terminal cleanup design that needs a new governor bulk-settle API cannot be squeezed into the current three-file write set.

## 7. Exact Future Write Set And Ownership Boundary

### 7.1 Sole Production Write Set

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudArtifactStore.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/host/ScopedPngArtifactStore.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java`

### 7.2 Sole Test Write Set

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/host/ScopedPngArtifactStoreTurnTest.java`

The test file is currently absent. It belongs to the same future implementation owner as the three production files; this helper creates no owner or claim. Existing `CloudServiceHostTurnCapabilityContractTest` and `QuestTurnClientContractTest` remain read-only under TURN-13H and TURN-17 ownership history.

### 7.3 Explicit Read-Only Files

`CloudServiceStorage`, `CloudServiceScope`, `CloudArtifactCapacityGovernor`, `CloudServiceHost`, `CloudTurnFrame`, `CloudQuestLocalServiceClient`, `TaskExecutionContext`, all turn runtime/factory/registry files, TURN-13H/TURN-17 tests, POMs, protocol DTOs, and all fixed reports/cards are outside TURN-38B4's production/test write set.

If any required identity directory, capacity settlement, task-terminal caller, host-close hook, metadata DTO, or second test cannot be implemented within the four frozen source/test paths, work must stop for a parent plan revision. A worker must not create a local helper file to hide the expansion.

## 8. Write-Set Mutex And Collision Evidence

| Lane/card | Relationship to TURN-38B4 |
|---|---|
| TURN-13H | Historical serial overlap on `CloudServiceConfiguration.java`; its source owner is released. B4 must preserve the same command port/catalog and dormant host seam. |
| TURN-17 | No file overlap. Its raw Quest-frame contract is a dependency, not an artifact caller. |
| TURN-38A | No exact file overlap, but hard API/identity dependency. B4 cannot run ahead of its source-stable contract. |
| TURN-38B1/B2/B3 | Plan `:1316-1323` gives disjoint production files and distinct named tests. They may be file-parallel only after each predecessor is met. No authority-bound state may be absorbed into B4. |
| TURN-39 | Waits for B1/B2/B3/B4 and 38C. B4 must remove its old artifact-context dependency before that convergence. |
| TURN-40B/40C | No B4 production-file overlap in the current plan, but they own the future runtime caller and host activation/close lifecycle. Their handoff to B4 must be explicitly frozen. |
| Current External writer | `docs/ACTIVE_WORK.md:21-30,43-55` identifies TURN-34A's `AutoCombatService` plus named test as the only real Java writer at this snapshot. It has no B4 file overlap. |

The lack of an active overlap does not make the three untracked B4 files claimable. Parent must re-hash them immediately before any future assignment.

## 9. Parent-Freeze Questions Before A Fixed Card

The following must be persisted by the parent before implementation. They are questions/acceptance boundaries, not a helper-selected design.

1. **Exact API authority:** the exact post-38A public context/value accepted by write/read/cleanup, and the old `CloudTaskServiceExecutionContext` symbols that must disappear from these files.
2. **Identity tuple:** exact host-fixed tenant/user plus device/window, task code, and task-invocation identity sources. State explicitly which fields are isolation keys and which are diagnostics only.
3. **No-ledger validation:** how an `ArtifactId` is proven to belong to the exact context without `ownerLedger`, session, run revision, or another mutable ownership table.
4. **Raw-frame contract:** whether the store persists an existing `CloudTurnFrame` byte-for-byte, how metadata survives read/pause, and how SHA/dimensions/region/sourceStepIndex remain correlated without a new DTO file or sidecar authority.
5. **No-shared-path meaning:** file-level uniqueness versus per-window/task directory isolation, including temp paths and A-to-B-to-A reuse/restart behavior.
6. **Terminal cleanup API:** which terminal classes invoke cleanup, whether cleanup is task-, invocation-, window-, or host-scoped, and whether it is idempotent/exactly once.
7. **Real caller owner/order:** whether B4 deliberately exposes a dormant cleanup capability and TURN-40B later calls it from runtime `finally`, plus the exact ordering relative to holder/state release, registry removal, host close, and unresolved exchange fences.
8. **Restart/close semantics:** what physically disappears on task terminal, host close, and process restart; fail-closed unreadability alone must not be mislabeled as physical cleanup.
9. **Capacity governor classification:** confirm its process-global maps are capacity-only and cannot authorize reads, select private state, retain terminal workflow, or expire by time. If its API must change for cleanup, revise the write set first.
10. **Construction test boundary:** require the named test to retrieve the store through real `CloudServiceHost.create(...).getService(CloudArtifactStore.class)` while keeping production activation deferred to TURN-40C.

## 10. Future Named-Test Matrix

The sole class name is fixed: `com.yueyunfe.dhxy.cloudbrain.host.ScopedPngArtifactStoreTurnTest`. The parent should freeze method names/assertions equivalent to this matrix after resolving section 9.

| Proposed named case | Required production-path evidence | Current-code regression value |
|---|---|---|
| `hostConstructsExactlyOneStoreFromFixedScopeAndStateRootWithoutArtifactIo` | Create the real dormant host with fake command/catalog, retrieve `CloudArtifactStore`, prove singleton identity and no artifact path before first write. | Locks the real construction seam rather than directly testing a replacement fake. |
| `turnNativeIdentityRequiresExactTenantUserDeviceWindowTaskAndInvocation` | Invoke the public store API; independently vary each identity dimension and prove rejection before filesystem mutation. | Old API cannot accept a turn-native context and uses old revalidation/ledger authority. |
| `sameTenantUserDifferentWindowOrTaskNeverSharesReadsDeletesOrCleansPath` | Write under A; B cannot observe/read/delete/cleanup; A remains intact. Repeat A-to-B-to-A. | Current physical root is shared and isolation depends on `ownerLedger`. |
| `rawPngRoundTripsBytesMetadataAndShaWithoutReencode` | Persist a valid existing raw frame and assert byte-exact PNG, purpose, dimensions, absolute region, sourceStepIndex, and SHA. | Current `BufferedImage` API re-encodes and drops all metadata. |
| `returnedRawPngAndMetadataAreDefensiveCopies` | Mutating one returned byte array/value cannot affect a later read or sibling context. | Current API has no raw-frame return contract. |
| `writePublishesOnlyAfterCreateNewTempAndAtomicMoveWithoutReplace` | Inspect temp storage: no target before commit, canonical private temp, atomic publish, no overwrite of an existing target. | Preserves the strongest current atomic/collision mechanics. |
| `encodeOrMoveFailureLeavesNoPublishedArtifactAndNoOwnedTemp` | Script each failure boundary through the public operation and prove no reachable target, no temp leak, and settled capacity accounting. | Prevents cleanup from deleting a pre-existing target or leaving a ghost reservation. |
| `pauseResumeKeepsSameArtifactAndAgeAloneNeverExpiresIt` | Reuse the same exact context/state across pause/resume; advance a fake clock or file age without terminal and prove continuity, with zero second write. | Old revision ownership can reject resume; no TTL must remain explicit. |
| `eachTerminalClassCleansExactlyItsTaskArtifacts` | Parameterize success, failure, stopped, skipped, task exception, and frozen close fallback; cleanup is idempotent and physically removes only the exact task's target/temp files. | Current API has no bulk terminal cleanup or caller. |
| `terminalCleanupPreservesSiblingTenantWindowAndTaskArtifacts` | Cleanup A while B/C remain byte-exact and readable by their own contexts. | Protects against unsafe shared-root scans/deletes. |
| `restartDoesNotReviveOrRetainPriorTaskArtifacts` | Close/recreate the permitted host/runtime boundary and prove old task artifacts are physically released without time-based expiry. | Current restart only loses `ownerLedger`; files remain as capacity orphans. |
| `publicApiContainsNoLegacyRemoteAuthorityOrOwnerSessionLedgerHandle` | Reflect the public interface/context parameter and exercise restart/A-B-A behavior; no old remote context, owner string, session ID, revision, list, path, or durable replay capability is exposed. | Fails against the current interface signature and old authority. |
| `capacityPressureIsCapacityOnlyNotTtlOrReadAuthority` | Capacity reclaim may remove the selected oldest file but cannot authorize a foreign read, alter a live task by age alone, or act as terminal history. | Distinguishes allowed bounded storage accounting from forbidden lifecycle authority. |

Test execution rules for the future card:

- Every core assertion must enter through the production `CloudArtifactStore` obtained from the real host bean graph. Package-private fault injection may support deterministic I/O failure, but it must not replace the public operation under test.
- Use only `@TempDir`, deterministic tiny raw PNG fixtures, fake command port/catalog, and in-memory metadata. Do not start application/server/Task/runtime or use capture/input.
- Do not modify TURN-13H or TURN-17 tests, create a second B4 test class, use source-text guards as a substitute for behavior, or re-capture/re-request a Quest frame.
- The current helper did not run the future named test or any build command.

## 11. Stop-Work Conditions

Future work must stop and return to the parent before editing when any of these is true:

1. TURN-38A has not produced a source-stable, parent-frozen replacement authority/context contract.
2. The implementation would guess the task/window/run identity, infer it from request text/title/HWND, or preserve old `runRevision/revalidate` semantics without explicit parent direction.
3. Correct no-shared-path or terminal cleanup requires modifying `CloudServiceStorage`, `CloudArtifactCapacityGovernor`, `CloudServiceScope`, `CloudServiceHost`, runtime/factory/registry, Quest client, protocol DTOs, a POM, or a second test.
4. Cleanup would scan/delete files outside a stateless exact identity boundary or bypass governor accounting.
5. A session, owner permit/string, mutable ownership ledger, durable workflow/replay state, TTL, timer/background cleanup, implicit retry, or second frame/capture is introduced.
6. `IMG` assertions are narrowed below raw bytes plus metadata/SHA/dimensions/region/sourceStepIndex without a parent plan amendment.
7. Terminal cleanup has no named production caller/ordering owner, yet delivery text proposes to claim it is wired.
8. Any of the three current production hashes drifts, the named test appears from another lane, or a real writer claims an overlapping file before the parent records a fresh mutex snapshot.

## 12. PRECHECK Summary

- Direct source gates met: TURN-13H and TURN-17.
- Direct source gate not met: TURN-38A.
- Current construction: one real configuration bean path, reachable only when the dormant host is created; production host create caller count is zero.
- Current artifact caller: zero. Current terminal cleanup caller/API: zero.
- Existing atomic temp/create-new/move/no-replace mechanics are useful, but raw-frame metadata preservation, stateless exact window/task identity, terminal/restart physical cleanup, and no-owner/session/ledger semantics are not present.
- Exact future write ownership remains three production files plus the one named test. No owner or claim was created by this helper.
- Parent must resolve the identity/path/cleanup/governor/caller questions above before issuing an implementation card.

PRECHECK_COMPLETE TRUE_EOF
