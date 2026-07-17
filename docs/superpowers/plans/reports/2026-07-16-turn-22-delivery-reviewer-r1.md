# TURN-22 Repair #2 Independent Delivery Review R1

## REVIEW CLAIMED

- Role: independent delivery reviewer R1; not implementation Worker and not parent/final reviewer.
- Scope: TURN-22 Repair #2 production/test delivery and its real DHXY input mapper/executor/queue boundary.
- Mutation boundary: this report is the only writable file; all Java, tests, fixed cards, plans, documentation, and both repositories are read-only.
- Verification boundary: source review only; no Maven, runtime, application, server, Task, UI, capture, input, or Git mutation.

<!-- TRUE_EOF: TURN-22 REPAIR-2 INDEPENDENT DELIVERY REVIEW R1 CLAIMED -->

## Independent Review Scope

- Reviewer: Codex Desktop independent delivery reviewer R1; implementation Worker and parent/final reviewer conclusions were not used as approval evidence.
- Authorities read in full: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/业务逻辑.md`, authority-plan Sections 14-19, and the current TURN-22 fixed card through its true EOF at line 444.
- Production/test sources read line by line: `CloudTeamReturnPortAssembly.java` (538 lines), `TeamReturnTurnContractTest.java` (1755 lines), `TurnInputActionMapper.java` (149 lines), `TurnInputStepExecutor.java` (229 lines), and the current `InputActionQueue.java` (775 lines). The complete `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:TeamReturnService.java` baseline (528 lines) was also read line by line.
- Final read-only source snapshot SHA-256: assembly `4435B30C4BFC923E222B12DE3CDA5BE9AEEC766AA1F826F26EA534BC1A5CFD66`; named test `CB41A6DD4AC931EABD470E67E25C9A5F653C55E1BBA240F4367E7D267CCF508B`; mapper `B5C6F173BA9A5C40774E24446E6726108701AB47A89A0C80434F15415319303A`; executor `0EE95CBD48D3EC76FB9E50385108F9898F2979A33966487B39065352AF1F43FD`; queue `95572C202D1CFF73732FECEBFB7710AA07DC770A27940B3A85577C212031866E`.
- Per the explicit review boundary, no Maven/JUnit/compile, runtime, application/server, Task/UI, capture, or input was run. No Git mutation was performed.

## Decision

**BLOCKED - P0/P1/P2 = 0/2/0.**

The emitted action shape, timing mapper, one executor call, terminal projection, and no-retry behavior are source-correct, but the delivery has two blocking defects: its required named test is not compilable in its owning Cloud module, and the real production input path can discard the immutable action-window binding before executing the already-mapped absolute coordinates.

## Findings

### P1-1 - The Cloud named test cannot compile against its owning module

**Evidence**

- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java:3-6,21,24,26,36,38-39` directly imports DHXY-only mechanics/window types, including `TurnExecutionWindow`, `TurnInputActionMapper`, `TurnInputStepExecutor`, `TurnKeyMapper`, `InputActionQueue`, `WindowIsolationProperties`, `BoundWindowKeyboardService`, `WindowTaskRunner`, `WindowRuntimeContext`, and `WindowTaskContextHolder`.
- A recursive read-only source/class lookup in `D:/mavenProject/dhxy-cloud-brain` found zero `.java` and zero `.class` files for each of those types.
- `D:/mavenProject/dhxy-cloud-brain/pom.xml:27-82` contains no DHXY artifact/module dependency. `pom.xml:84-192` configures only the normal compiler/surefire/enforcer/exec/antrun/shade plugins and adds no sibling DHXY source root or test classpath.
- Consequently, the new path at `TeamReturnTurnContractTest.java:1348-1415` cannot reach JUnit in the card's stated Cloud command. Its assertions on the production executor and recording queue are syntactically meaningful but unresolved in that module.

**Impact**

Repair #2 does not provide runnable card-level proof that this card's emitted spec reaches the production executor and one queue submission. A clean Cloud test compilation fails before the assertions can execute; source review cannot substitute for the explicitly required named test.

**Repair condition**

The parent must first freeze an executable cross-repository test boundary and authorize its exact write set. Put the mechanics assertion in a module that actually owns the DHXY executor/queue, or provide a legitimate test-scoped shared artifact/source arrangement that compiles without duplicate same-FQN classes. Then run the named authorized test successfully. Do not copy the mapper/executor, use source-text guards, rely on stale `target/classes`, or inject an ad-hoc sibling classpath. Because the current Repair #2 write set is test-only and `pom.xml`/DHXY are read-only, this cannot be repaired within the presently frozen write set.

### P1-2 - Production click execution re-resolves the binding after mapping exact coordinates

**Evidence**

- `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java:60-67` maps the mouse input against the immutable `TurnExecutionWindow.metadata().windowRect()`. Lines `166-171` then bind only the mutable `window.context()` and call legacy `inputActionQueue.submitAndWait(description, actions)`; they do not pass `window.binding()` or the frozen identity epoch.
- `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/input/action/InputActionQueue.java:67-79` handles that legacy call by invoking `refreshAndValidateNativeBinding(...)` before constructing the request. Lines `592-603` show that method reading the mutable context binding and calling `bindingRefreshService.refreshAndCommit(context)` with no equality check against the executor's immutable `TurnExecutionWindow.binding()`/metadata.
- The same queue now exposes a no-refresh frozen-snapshot boundary at `InputActionQueue.java:319-347`, but `TurnInputStepExecutor.java:166-171` does not use it. That frozen API is currently callback-only and cannot receive the mapped action list as one request.
- `TeamReturnTurnContractTest.java:1403-1414` overrides `submitAndWait` and returns `true`; therefore the named test bypasses the production queue body at lines `67-79` and cannot detect this binding drift.

**Impact**

If HWND/process/geometry or identity changes between `TurnExecutionWindow` resolution and queue submission, the click coordinates remain those mapped for the old immutable window while the queue request/focus uses the newly refreshed mutable binding. Physical input can therefore be delivered to the wrong binding or wrong geometry, and the executor can still report `COMPLETED`. Later terminal correlation cannot undo a physical mis-click. This violates TURN-22's exact-window fail-closed contract even though the numeric X/Y values are preserved in the list.

**Repair condition**

Route the complete mapped `[CLICK_LEFT(delay=150), SLEEP(500)]` list through one queue API that accepts the already-resolved context, immutable native binding, and identity epoch, performs no refresh/search, rejects any drift before focus/first physical action, and returns a typed fail-closed result. `TurnInputStepExecutor` must use that boundary exactly once. Add executable coverage for binding/geometry/epoch drift and for the unchanged one-submission action order. Preserve one command/UUID and add no retry, session, ledger, or TTL semantics.

## Controls Verified

- Baseline: `696a12b0:TeamReturnService.java:65-91`, especially lines `75-89`, performs first observation, one incense check, refreshed observation, randomized exact point, and one queue fragment `[clickLeft(point,150), sleep(500)]`.
- Emission: `CloudTeamReturnPortAssembly.java:122-128` emits one INPUT step containing one `CLICK_LEFT` spec with the exact absolute X/Y, `clickDelayMs=150`, and `queueHoldMs=500`; no second action/WAIT step is emitted.
- Command cardinality and fail-closed outcome: `CloudTeamReturnPortAssembly.java:135-166` invokes `boundClient.execute(...)` once, rejects non-completion/correlation/frame-shape uncertainty, maps STOPPED/FAILED/uncertain closed terminals without retry, and only reports EXECUTED after exact completed-step correlation.
- Mapping: `TurnInputActionMapper.java:30-34` rejects click timing on non-click operations; lines `39-47` map this spec to the single ordered list `[CLICK_LEFT(delay=150), SLEEP(500)]`. Nullable timing defaults at lines `127-134` do not alter TURN-22's explicit `150/500` values.
- Submission cardinality: `TurnInputStepExecutor.java:60-67,166-171` forwards the whole mapped list through one queue API invocation. `InputActionQueue.java:78-79,626-630` creates one request containing that list and offers it once. This preserves atomic list cardinality but does not cure P1-2's binding drift.
- The TURN-22 assembly active path creates no second command or UUID and contains no automatic transport retry, owner/session/ledger/TTL, durable workflow, or extra verification. Shared queue session APIs are not called by this path.
- Capture/observation review found no additional P0/P1/P2: one exact raw capture per observation, strict frame/outcome correlation, and uncertain states fail closed rather than becoming PRESENT/ABSENT or click success.

This is an independent delivery-review conclusion only. It is not `CARD APPROVED`, `CLOSED`, or the parent/final reviewer judgment. R1 remains available for a fresh review after both repairs are delivered.

<!-- TRUE_EOF: TURN-22 REPAIR-2 INDEPENDENT DELIVERY REVIEW R1 BLOCKED P0=0 P1=2 P2=0 -->
