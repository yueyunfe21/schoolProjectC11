# TURN-34AT1 Repair #4 Independent Whole-Card Review R2

- Reviewer: fresh independent R2
- Reviewed at: `2026-07-16T13:43:25-04:00`
- Verdict: **APPROVED**
- Severity: **P0/P1/P2 = 0/0/0**
- Scope: complete TURN-34AT1 Repair #4 card, frozen production/test sources, HTTPS turn protocol, and `696a12b0` baseline
- Independence: R1 report was not read or used

## Frozen identity

- Production `AutoCombatService.java`: `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` (852 lines)
- Test `AutoCombatServiceTurnContractTest.java`: `bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221` (1047 lines, 22 tests)
- Both hashes exactly match Parent Whole-Card Test-Source Review #7.

## Whole-card findings

1. **Public contract and real production path pass.** The test constructs the real production `AutoCombatService` and its public collaborators, binds the real `TaskExecutionContextHolder`, and reaches Stage-1 through `probeWindowCombatStateReadOnly(...)`. The frozen public API guard remains present through public `getMethods()` / `getMethod(...)` checks.
2. **Stage-1 minimal HTTPS action passes.** The positive path emits one index-0 `CAPTURE`, exact screen region `(1074,680,51,20)`, `UPLOAD_IMAGE`, 120-second timeout, exact metadata/raw-PNG SHA correlation, and no INPUT/WAIT/MATCH/LOCAL_SERVICE union member.
3. **CAPTURE inner-null contract passes.** The test explicitly asserts both `clearPointerIfOverRegion()==null` and `pixelChangeProbe()==null`.
4. **Terminal/uncertain and UUID contract passes.** BUSY, duplicate, timed-out uncertain, interrupted uncertain, legal FAILED, STOPPED, and DUPLICATE_OR_UNCERTAIN each produce one command and no fallback/retry. One shared harness then executes those seven cases plus one successful Stage-1 capture and asserts exactly eight commands, eight distinct canonical UUIDs, and exhausted scripted replies.
5. **Legal terminal fixtures pass.** FAILED carries `failedStepIndex=0` with step 0 FAILED. STOPPED and DUPLICATE_OR_UNCERTAIN carry no failed index and keep the step NOT_RUN, so the tests reach the intended production terminal paths instead of validator fallback.
6. **Strict `696a12b0` team gate passes.** Production keeps `REFRESH_DUE_PANEL_VERIFY_GUARD_MS=30_000`; tests cover same-team defer at 29,999 ms, allow at 30,000 ms, other-team independence, and same-team/same-window defer at `now+10ms`.
7. **Repair #4 is complete.** `getDeclaredFields`, all declared-member reflection, `setAccessible`, `sun.misc.Unsafe`, and `allocateInstance` are absent. No `Files.read*`, `Paths.get`, `.java` literal, or other source-scan replacement exists. The remaining reflection is limited to the explicitly required public API guard.
8. **Business baseline is unchanged.** No new retry/replay/resend, session, ledger, TTL, durable workflow, local OCR/business decision, second command, or production test hook was introduced. `AutoCombatService.java` remains the frozen read-only production SHA.

No P0/P1/P2 issue was found in the reviewed whole-card snapshot. Named test and Cloud compile remain the parent stable-writer build gate and were not run by this reviewer.

<!-- TRUE_EOF: TURN-34AT1 REPAIR-4 INDEPENDENT-WHOLE-CARD-REVIEW-R2 APPROVED P0P1P2=0/0/0 productionSHA256=532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9 testSHA256=bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221 NO-MAVEN NO-GIT-MUTATION REVIEW_COMPLETE 2026-07-16T13:43:25-04:00 -->
