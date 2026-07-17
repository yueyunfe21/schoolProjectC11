# CR271 TURN-28P Repair #2 Independent Delivery Review R1

- Reviewer role: independent delivery reviewer R1; not the implementation owner and not the parent/final reviewer.
- Review timestamp: `2026-07-16T08:55:30.7184072-04:00`.
- Authoritative card: `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md`.
- Effective delivery reviewed: Euler `INTERNAL REPLACEMENT SOURCE+TEST DELIVERED` at the card's true EOF history, followed by the current working-tree bytes.
- Business baseline checked: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Repository snapshots: DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`; Cloud `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`.

## Verdict

**APPROVED**

`P0/P1/P2 = 0/0/0`

This approval is limited to TURN-28P Repair #2 production/test-source delivery review. It does not replace the parent final decision, the second independent reviewer, named-test execution, or the applicable compile/build gate.

## Findings

No P0, P1, or P2 finding remains in the reviewed scope.

## Independent Evidence

### 1. Public resolver reaches the real queue and worker

- `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java:51-87` resolves one action window and performs the sole `refreshAndCommit` before publishing the exact context/binding/metadata snapshot.
- `src/main/java/com/bot/dhxy/input/InputSequences.java:80-87` exposes the public typed facade and delegates directly to `InputActionQueue.submitFrozenExactWindowExclusiveAndWait`; it does not synthesize a result.
- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java:337-365` creates the frozen request while holding the runtime-context monitor, captures the epoch there, rejects object-generation drift before enqueue, and waits for the production request result.
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java:120-132,403-439` takes that request from the production queue, installs the captured task context, enters the real input transaction, rechecks the generation under the same context monitor, focuses the frozen binding, and runs the callback through `InputActionScope`.
- `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java:148-180` focuses the supplied frozen HWND/binding without a title search or second binding refresh.
- `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java:485-561,626-647` constructs and starts the real `InputActionQueue`/`InputActionWorker`; its counting queue delegates to production `super` and only records submissions.
- `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java:579-660,1077-1101` does the same through the public `LocalTurnActionExecutor` path. The old synchronous `ProbeInputSequences`/manual-result seam is absent from both delivery tests.

The only reflective construction in the two delivery tests is `Unsafe.theUnsafe` at probe test `:743-744` and local test `:804-805`, used to allocate inert collaborators. It does not invoke a private production helper, scan source text, or bypass the public queue/worker path.

### 2. Exact generation and A -> B -> A drift

- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java:440-450` first requires the same binding object; a value-equal replacement object therefore cannot revive generation A after A -> B -> A.
- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java:897-946` additionally checks identity epoch and every frozen HWND/process/geometry field.
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java:416-439` holds the context monitor from the authoritative worker check through focus, callback, and callback `finally`; `WindowNativeBindingRefreshService.refreshAndCommit` uses that same monitor (`src/main/java/com/bot/dhxy/window/runtime/WindowNativeBindingRefreshService.java:72-85`).
- `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java:239-344` independently rejects each exact field and a value-equal new A object after A -> B -> A, with zero callback/input.
- `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java:314-353` repeats A -> B -> A through the public resolver and real worker, proving zero capture/keyboard/move and no second refresh.

### 3. Admission STOP remains typed

- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java:908-911` maps a closed task stop token to `InputActionSafetyReason.STOP_REQUESTED`.
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java:403-408` applies detailed safety and worker admission before callback mechanics.
- `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java:370-400` projects release uncertainty first, then typed `STOP_REQUESTED`, and only afterwards generic queue/mechanics failure; a real admission stop cannot degrade to success or `PIXEL_PROBE_FAILED`.
- Probe test `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java:260-292` and local public-path test `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java:435-477` close the real stop token before worker admission and assert STOPPED with one queue crossing and zero mechanics.

### 4. Started cancellation owns a completion barrier

- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java:615-633` does not complete a started frozen request from the waiter/canceller; terminal publication remains worker-owned.
- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java:793-805` requests cooperative cancellation when the worker already owns the request, then joins its terminal result before returning and restores the interrupt flag.
- `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java:218-362` keeps Ctrl DOWN, MOVE, after-capture, Ctrl UP, release settle, and cleanup inside the frozen callback/finally.
- `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java:103-162` proves the waiter remains blocked behind the worker-owned Ctrl-UP/settle barrier.
- `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java:355-385` uses latches and a mechanics watermark rather than a timing guess, and proves that no capture/keyboard/move occurs after the interrupted caller returns.

### 5. Non-Runtime Ctrl-UP failure remains typed and cannot become success

- `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java:305-354` attempts Ctrl UP whenever Ctrl DOWN was invoked and catches `Throwable`, so an `AssertionError` records release uncertainty rather than escaping before cleanup classification.
- `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java:370-404` gives `releaseFailed` precedence and emits `CTRL_RELEASE_FAILED` without a completed frame.
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java:209-225` also has the production outer `catch (Throwable)` for any non-Runtime callback failure not consumed by the mechanics-level release classifier.
- `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java:173-218,299-312` sends both a non-Runtime Ctrl-UP failure and a non-Runtime callback/capture failure through the real worker path; the former remains typed release uncertainty and the latter is normalized as a non-completed queue result.
- `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java:209-230` independently proves the real worker's outer `catch (Throwable)` closes an `AssertionError` callback without false success.

### 6. Cloud uncertainty is terminal, one UUID, one command, zero retry

- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java:60-106` rejects code-only and fully valid frame-only probe completions.
- The same test `:136-171` prohibits completed probe code/frame evidence on `DUPLICATE_OR_UNCERTAIN`.
- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java:296-323` separately covers timeout and interruption: one fixed action UUID, one command, zero retry, no invented outcome, and no frame.
- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java:161-168` mints one UUID and invokes the command port once for the action.

### 7. Baseline and prohibited semantic additions

The TURN-28P turn classes/tests do not exist at business baseline `696a12b0`; the baseline comparison therefore establishes this as ownership/mechanics migration rather than a replacement of an existing cloud turn implementation. Review of the current path found no new business/OCR decision, automatic retry, session/ledger/TTL, or durable-workflow behavior in the pixel-change probe. The retained-session APIs currently present in the shared queue/worker belong to a separate path; `submitFrozenExactWindowExclusiveAndWait` creates a frozen exclusive request and dispatches to `runFrozenExactWindowExclusive`, not the retained-session branch.

`docs/业务逻辑.md` was checked as the behavioral authority. Result: `无已批准业务差异；按基线等价迁移`.

## Reviewed Byte Snapshot

The two delivery-owned replacement tests remained byte-identical throughout this review. Shared queue/worker files received a concurrent frozen action-list increment from another card near the end of review; the current bytes were re-read and the callback path above remained unchanged. Approval is bound to this final snapshot:

| Repository | File | Lines | SHA-256 |
|---|---|---:|---|
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | 850 | `66fa536ef8b4c6cbf8874cd94d8842fd8b0f9d3f4e74bc52719f31f39e4660bf` |
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | 1118 | `23973b7eee06949138e8a2841e249c009eb69184804c2be0689aa317c29988de` |
| DHXY | `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | 748 | `7489084b773e6066213d383af86c82ac9c3431fb9e2d1d5acf3e9c11d423eac0` |
| DHXY | `src/main/java/com/bot/dhxy/input/InputSequences.java` | 210 | `b293e0c6792303d45a4314050c6e4f1c8b39d0f4dea426632586ed0f292dacb3` |
| DHXY | `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java` | 243 | `0f22571a5727248c34e26fdd8a7ed930c15b7b0106452050ccfaa3520f67e6b8` |
| DHXY | `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java` | 587 | `5612b067e4a3f16b48845bd50dcc046cea3e15fc93781888637210e867ce59f0` |
| DHXY | `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | 852 | `475399ef8656c7d193bfeb6f18ba69b7e01d4c531710367e74d00165ded03c44` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java` | 965 | `5d563bbb08747c7b298ec6c7c0795a600269bc86d8f5769bcc67588268fda818` |
| DHXY | `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` | 1275 | `88011cf17b24e68b8dcf5c7ef11edd30fb8a9df2aac27e639e320e3bd4dd3709` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java` | 328 | `2b35046d14c3b0b822537474a07f34233ecb0333c1143fe1a2eec10a3b230520` |
| Cloud | `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java` | 639 | `89da4fa3e61430dcfee39c313fc9cdb05d2905b3bbfd4a34bfac39f0a730ea67` |

## Verification Boundary

- Read both repository statuses and preserved all pre-existing dirty/untracked work.
- Did not modify Java, the TURN-28P card, ACTIVE_WORK, the parent plan, the dashboard, or any other document.
- Per the explicit reviewer constraints, did not run Maven/JUnit/compile, runtime/application/server/Task/UI/capture/input, and performed no Git mutation.
- Therefore this report makes no build/test-execution claim; those gates remain with the parent manager after all Java writers are stable.

<!-- TRUE_EOF: CR271 TURN-28P REPAIR #2 INDEPENDENT DELIVERY REVIEW R1 APPROVED; P0/P1/P2=0/0/0; 2026-07-16T08:55:30.7184072-04:00 -->
