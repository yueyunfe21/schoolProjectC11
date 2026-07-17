# TURN-22 Reviewer-Finding Integration Parent Preflight

## Role And Boundary

- Role: parent-level reviewer-finding integration preflight helper only. This helper is not R1, R2, an implementation Worker, or the parent/final reviewer.
- This report supplies independent source/POM/status evidence and adopt/reject recommendations. It does not approve TURN-22, TURN-28P, any source, any test, or any card state.
- Sole write target: `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-22-review-findings-parent-preflight-helper.md`.
- No Java, test, CR card, authority plan, protocol, dashboard, or configuration file was changed. No Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git mutation was run.
- Read-only snapshot completed at `2026-07-16T05:48:13-04:00`. The parent documents were being appended concurrently; their latest appended sections were re-read through their then-current true EOF before this report was written.

## Complete-Read Inventory

The following authorities/evidence were read in full at the stated snapshot, except that "CR271 top" intentionally means the current top entries of that card rather than all 39,474 lines of the architecture document:

| Evidence | Read scope | Snapshot evidence |
|---|---|---|
| `D:/mavenProject/DHXY/AGENTS.md` | complete, 392 lines | SHA-256 `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md` | complete, 1,349 lines | SHA-256 `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `D:/mavenProject/DHXY/docs/PACKAGE_ARCHITECTURE.md` | current CR271 top, especially `35648-35685` | `35650-35655` now records TURN-22 Delivery Review #4 as not accepted, with the two P1 issues and one P2 issue |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-22.md` | complete through current line 501 true EOF | latest parent append is `446-501` |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-22-delivery-reviewer-r1.md` | complete, 74 lines | R1 reports `BLOCKED 0/2/0` |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-22-delivery-reviewer-r2.md` | complete, 74 lines | R2 reports `BLOCKED 0/1/1` |
| `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` | complete, current 1,671 lines | TURN-22 status/write/test split at `1115`, `1250-1253`, and `1606` |
| `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` | complete, 383 lines | exact metadata/click timing/coordinate rules at `77-81`, `216-222`, `283-288` |
| `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md` | complete, 839 lines | exact binding is resolved/refreshed once per action at `581-587` |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md` | complete through current line 618 true EOF | frozen contract at `455-494`; current Repair #1 source/test delivery at `546-618` |

The two reviewer reports, the TURN-22/28P source paths named below, both POM/source trees, and both repository statuses were independently inspected rather than treating the parent or reviewer summaries as proof.

## Two-Repository Status Snapshot

### DHXY

- Repository: `D:/mavenProject/DHXY`
- Branch: `thin-client-design`
- HEAD: `0114604e1ff5f15491d2910959c45252e893d04f`
- `git status --porcelain=v1 -uall`: dirty, 628 entries at the snapshot (44 non-untracked changes and 584 untracked paths by the read-only classification).
- Relevant existing changes include `InputSequences.java`, `WindowAwareInputCoordinator.java`, `InputActionQueue.java`, `InputActionRequest.java`, `InputActionWorker.java`, `PACKAGE_ARCHITECTURE.md`, the authority plan, both reviewer reports, both fixed cards, `TurnInputStepExecutor.java`, and `TurnInputStepExecutorContractTest.java`.
- These pre-existing/concurrent files were read only. Their dirty state was not cleaned, reverted, staged, committed, or otherwise mutated by this helper.

### dhxy-cloud-brain

- Repository: `D:/mavenProject/dhxy-cloud-brain`
- Branch: `navigation-migration`
- HEAD: `3b988caa010254973e03342272e6d1d6a9685b01`
- `git status --porcelain=v1 -uall`: dirty, 550 entries at the snapshot (9 non-untracked changes and 541 untracked paths by the read-only classification).
- `pom.xml` is modified and the Cloud protocol tree is untracked. The named tests do not appear in ordinary status output because `D:/mavenProject/dhxy-cloud-brain/.gitignore:14-16` ignores `src/test/`; that Git ignore rule does not remove Maven's normal `src/test/java` compilation source root.
- No status evidence shows a managed DHXY artifact, sibling source root, or test bridge.

## Dispute 1: Cloud Named-Test Compile Boundary

### Direct Answer

1. `SummonSkillTurnContractTest` does **not** import the DHXY local turn executor/mapper/input queue/window-runtime mechanics at issue.
2. `TeamReturnTurnContractTest` **does** directly import those DHXY-only mechanics/window types.
3. The current Cloud POM and source/class tree cannot resolve all of those imports. There is no existing shared mechanics bridge. Consequently, the current Cloud-owned `TeamReturnTurnContractTest` cannot reach JUnit execution through a normal Cloud `test-compile`.

This is a source/POM determination. Maven was deliberately not run.

### Import Evidence: Summon Versus TeamReturn

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java:3-45` imports Cloud-copied protocol/business/service/client types, but it does not import:

- `TurnExecutionWindow`
- `TurnInputActionMapper`
- `TurnInputStepExecutor`
- `TurnKeyMapper`
- `InputActionQueue`
- `WindowRuntimeContext`
- `WindowTaskContextHolder`
- `WindowTaskRunner`
- `BoundWindowKeyboardService`

It therefore is not evidence that Cloud already has a shared DHXY mechanics bridge.

By contrast, `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java` directly imports:

- DHXY turn mechanics at `3-6`: `TurnExecutionWindow`, `TurnInputActionMapper`, `TurnInputStepExecutor`, `TurnKeyMapper`.
- The input queue at `21`: `InputActionQueue`.
- Window/input configuration and driver types at `24`, `26`, and `36`: `WindowIsolationProperties`, `BoundWindowKeyboardService`, `WindowTaskRunner`.
- Window runtime types at `38-39`: `WindowRuntimeContext`, `WindowTaskContextHolder`.

The same test imports `InputAction`/`InputActionType` at `20`/`22` and `WindowNativeBinding` at `37`; unlike the types above, those three same-FQN classes currently do exist in the Cloud source tree. Their presence does not satisfy the missing executor/mapper/queue/runtime graph.

The illegal cross-repository path is exercised, not merely imported:

- `TeamReturnTurnContractTest.java:1348-1356` constructs `WindowTaskContextHolder`, `TurnInputStepExecutor`, both mappers, and a recording queue.
- `1358-1374` executes the production executor and asserts one recorded action list plus empty context restoration.
- `1378-1400` reflectively constructs `TurnExecutionWindow` from `WindowTaskRunner`, `WindowRuntimeContext`, `WindowNativeBinding`, and metadata.
- `1403-1415` subclasses `InputActionQueue` and overrides legacy `submitAndWait`.
- `1417-1421` subclasses `BoundWindowKeyboardService`.

### POM/Source-Graph Evidence

- A recursive `.java`/`.class` lookup across the current Cloud repository and `target` found no `TurnExecutionWindow`, `TurnInputActionMapper`, `TurnInputStepExecutor`, `TurnKeyMapper`, `InputActionQueue`, `WindowIsolationProperties`, `BoundWindowKeyboardService`, `WindowTaskRunner`, `WindowRuntimeContext`, or `WindowTaskContextHolder`.
- The same lookup found only the relevant partial copies `InputAction.java`, `InputActionType.java`, and `WindowNativeBinding.java`, plus protocol `TurnInputAction.java`.
- `D:/mavenProject/dhxy-cloud-brain/pom.xml:27-82` declares OpenCV, Jackson, JCS, Spring, Lombok, SLF4J, and JUnit dependencies only. It declares no DHXY module/artifact/test dependency.
- `pom.xml:84-192` configures the ordinary compiler, surefire, enforcer, exec, antrun, and shade plugins. It adds no sibling DHXY source root, build-helper source, `systemPath`, test classpath, or reactor module.
- `D:/mavenProject/dhxy-cloud-brain/README.md:3-9` explicitly states that Cloud Brain lives outside DHXY and does not reference the DHXY main/test classpath.
- The current `src/main/java/com/bot/dhxy/cloud/turn` Cloud tree owns protocol classes only; it does not contain a local executor bridge.

Therefore the unresolved TeamReturn imports fail during Cloud test compilation before its `@Test` assertions can run. A stale sibling `target/classes`, copied same-FQN mechanics, or an ad hoc source root would not be an existing reproducible bridge and would conflict with the current frozen repair direction.

### Test-Evidence Impact

- Repair #2's Cloud test does not provide executable proof that emitted TeamReturn input reaches the real DHXY executor and queue.
- Its `RecordingInputQueue` also overrides the very legacy queue body that contains the refresh defect, so even a manually supplied classpath would not detect dispute 2.
- The viable ownership split already frozen by the current authority plan is: Cloud keeps assembly/emitted-spec/command/terminal assertions; DHXY owns executor/queue mechanics assertions. See the authority plan at `1250-1253` and `1606`.

### Parent Finding Recommendations For Dispute 1

- **Adopt R1 P1-1.** Its unresolved-import/POM conclusion is independently confirmed.
- **Adopt R2 P1-1, but merge it with R1 P1-1 rather than count it twice.** Both reviewers identify the same compile-boundary defect.
- **Reject any inference that `SummonSkillTurnContractTest` proves an existing shared bridge.** It does not import the missing local mechanics types.
- **Adopt R2 P2-1 as a separate test-quality issue.** `TeamReturnTurnContractTest.java:1350` creates an empty holder; `1373-1374` only proves empty-to-empty. `1380-1396` creates a binding but never installs it on the target runtime context, and `1403-1414` never observes the holder inside the queue boundary. This can pass even if the executor never binds the exact context.
- For that P2, preserve the real restoration contract at `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/window/runtime/WindowTaskContextHolder.java:106-117`, but prove it in the DHXY test by prebinding a distinct sentinel, installing the target exact binding, observing exact window/HWND/process/rect/epoch at the frozen queue boundary, and asserting sentinel restoration afterward.

## Dispute 2: Frozen Spec Through The Current DHXY Executor

### Direct Answer

Yes. The production TeamReturn spec is emitted with the correct single typed click and maps to the correct atomic list, but the current `TurnInputStepExecutor` still submits that list through the legacy `InputActionQueue.submitAndWait(String,List<InputAction>)`. That API refreshes the mutable window binding again after coordinates were derived from the immutable action snapshot. The normal worker focus path can refresh yet again.

TURN-28P Repair #1 is the correct shared prerequisite for a no-refresh frozen queue/focus primitive, but it does not by itself repair TURN-22:

- TURN-28P's frozen write set does not include `TurnInputStepExecutor`.
- The currently delivered frozen API is callback-only, while TURN-22 must submit the mapper-produced complete action list once.
- TURN-22 therefore needs its own Repair #3 after the TURN-28P frozen API reaches the parent source/test-source gate and its usable signature is frozen.

### Current Production Call Chain

| Stage | Current source evidence | Consequence |
|---|---|---|
| Resolve/freeze action window | `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java:51-69` resolves the runner/context and calls `refreshAndCommit(context)` at `68`; `74-87` constructs metadata and returns the immutable binding snapshot. | This is the authorized once-per-action refresh and snapshot A. |
| Map absolute click coordinates | `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java:60-67` maps against `window.metadata().windowRect()`. | X/Y belong to snapshot A. The mapper still correctly produces `[CLICK_LEFT(delay=150), SLEEP(500)]`. |
| Submit through legacy API | `TurnInputStepExecutor.java:166-171` temporarily binds only `window.context()` and calls `inputActionQueue.submitAndWait(description, actions)`; it passes neither `window.binding()` nor the frozen identity epoch. | The exact snapshot is dropped at the queue API boundary. |
| Queue refreshes mutable context | `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionQueue.java:67-79` invokes `refreshAndValidateNativeBinding` before constructing the request. `587-627`, especially `595-612`, reads the mutable binding, calls `bindingRefreshService.refreshAndCommit(context)` at `606`, and then reads the refreshed mutable binding. | This is refresh #2 relative to action resolution. There is no equality fence against snapshot A. |
| Normal request captures the new mutable state | `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionRequest.java:220-227` captures `windowContext.getNativeBinding()` and `getPlayerIdentityEpoch()` for a non-frozen request. | The request can now carry snapshot B while its absolute coordinates still belong to A. |
| Worker selects legacy focus | `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionWorker.java:116-139` chooses `focusCurrentWindowInActiveTransaction` for a non-frozen request; only frozen requests use `focusFrozenWindowInActiveTransaction` at `129-135`. | TURN-22 is treated as a mutable/legacy request. |
| Legacy focus refreshes again | `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java:193-221` calls `bindingRefreshService.refreshAndCommit(context)` at `205` before focusing `context.getNativeBinding()` at `211-216`. | The normal path can reach refresh #3 and focus snapshot C while executing coordinates from A. |

The single list submission still prevents another request from interleaving between `CLICK_LEFT(150)` and `SLEEP(500)`. That atomicity is real, but it does not cure exact-window drift before the first physical action.

### Runtime Impact

If HWND, process, screen rectangle, or identity epoch changes between action resolution and queue/focus:

- coordinates remain derived from old snapshot A;
- queue/focus can use new snapshot B or C;
- physical input can target the wrong binding or wrong geometry;
- the legacy boolean queue can still return success, allowing the executor to project `COMPLETED`;
- later command correlation cannot undo a physical mis-click.

This conflicts with:

- current bound-window metadata at `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:77-81`;
- complete-list, one-queue click timing at `216-222`;
- exact unscaled screen coordinates at `283-288`;
- the once-per-action resolve/refresh contract at `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md:581-587`.

### TURN-28P Repair #1 Relationship

The relationship has two distinct layers:

1. **Shared prerequisite: yes.** `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md:480-492` freezes a generic exact-window queue boundary with no enqueue/focus refresh, exact snapshot checks, drift-zero input, cancellation completion, and real queue/worker tests. The current Repair #1 delivery now exposes `InputActionQueue.submitFrozenExactWindowExclusiveAndWait` at `InputActionQueue.java:330-349`, forwards it at `InputSequences.java:75-83`, and selects frozen focus in `InputActionWorker.java:129-135`.
2. **TURN-22 closure: no.** TURN-28P's production write set at `TURN-28P.md:460-467` includes queue/request/worker/facade/coordinator and `TurnCaptureStepExecutor`, but excludes `TurnInputStepExecutor`. The delivered method accepts an exclusive callback, not a mapper-produced `List<InputAction>`. `TurnInputStepExecutor.java:166-171` still calls the legacy list API.

At this snapshot TURN-28P Repair #1 is `SOURCE+TEST DELIVERED` at `TURN-28P.md:546-618`; it is not represented here as parent-reviewed or approved. TURN-22 must wait for the parent source/test-source gate, then consume the frozen shared boundary through the separately authorized TURN-22 Repair #3. It must not guess a second frozen wrapper or bypass the mapper/list through direct input.

### Parent Finding Recommendations For Dispute 2

- **Adopt R1 P1-2.** The immutable coordinates are handed to a legacy API that performs another refresh; the full queue/worker path independently confirms the defect.
- **Reject/supersede R2's statements at R2 report `54-61` and `70` that the production queue chain has no blocker.** R2 correctly verified one-list/one-request atomicity but did not carry the binding identity through queue refresh and worker focus. Atomic action ordering and exact-window identity are separate contracts.
- **Return/keep TURN-22 at Repair #3 prerequisite-blocked status.** The current parent state already does this at `D:/mavenProject/DHXY/docs/PACKAGE_ARCHITECTURE.md:35650-35655`, TURN-22 card `446-501`, and authority plan `1115`.
- **Treat TURN-28P Repair #1 as the shared primitive gate, not as automatic TURN-22 completion.** After that gate, use the authority-plan Repair #3 write set at `1250-1253`: Cloud `TeamReturnTurnContractTest`, DHXY `TurnInputStepExecutor`, and DHXY `TurnInputStepExecutorContractTest`; keep assembly/service/mapper/protocol/POM/caller read only.
- The DHXY test responsibility already frozen at authority-plan `1606` should prove exact snapshot plus sentinel restoration, exactly one frozen queue submission with `CLICK_LEFT(150)+SLEEP(500)`, and zero physical input on binding/geometry/epoch drift. Generic cancellation/worker mechanics remain TURN-28P's responsibility.

## Reviewer-Finding Integration Matrix

This is a recommendation matrix for the parent, not a reviewer verdict:

| Reviewer material | Independent preflight result | Parent recommendation |
|---|---|---|
| R1 P1-1: Cloud named test cannot compile | Confirmed by imports, absent classes, POM, README, and source-tree lookup | Adopt |
| R2 P1-1: same Cloud compile boundary | Confirmed; same root defect as R1 P1-1 | Adopt and de-duplicate |
| R1 P1-2: frozen coordinates enter refresh-capable legacy queue | Confirmed through executor, queue, request, worker, and coordinator | Adopt |
| R2 P2-1: context restoration test is empty-to-empty | Confirmed at test lines `1350`, `1373-1374`, `1380-1396`, `1403-1414` | Adopt as separate P2 test-evidence defect |
| R2 production-chain statement: no production blocker | Contradicted by queue refresh #2 and worker/focus refresh #3 | Reject/supersede |
| Any claim that Summon proves a shared DHXY mechanics bridge | Contradicted by Summon imports and Cloud POM/source graph | Reject |

If the parent adopts the independently confirmed and de-duplicated set, the inventory remains two distinct P1 defects plus one P2 defect. This helper does not assign or approve the parent disposition; it notes that the current parent documents already record `0/2/1` and Repair #3 prerequisite-blocked.

## Parent Integration Recommendation

1. Preserve the current TURN-22 return: `DELIVERY REVIEW #4 / REPAIR #3 PREREQUISITE BLOCKED`.
2. Do not use the current Cloud TeamReturn test as executable DHXY mechanics evidence and do not manufacture a sibling classpath/shared artifact inside this repair.
3. Wait for the TURN-28P Repair #1 parent source/test-source gate and final shared API shape.
4. Then execute the already-frozen TURN-22 Repair #3 split: Cloud-only emitted-spec/terminal assertions; DHXY-owned executor/frozen-queue/sentinel/drift assertions.
5. Preserve the approved baseline mechanics: one command/UUID, one typed click spec, one queue action list, `CLICK_LEFT(delay=150)` followed by `SLEEP(500)`, no retry/session/owner/ledger/TTL/durable workflow, and no new business decision.

No approved business difference is required; this is exact-window mechanical equivalence work.

PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-22 REVIEW-FINDINGS PARENT PREFLIGHT HELPER PRECHECK_COMPLETE 2026-07-16T05:48:13-04:00 -->
