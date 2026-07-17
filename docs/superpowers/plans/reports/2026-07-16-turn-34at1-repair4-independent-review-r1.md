# TURN-34AT1 Repair #4 independent whole-card delivery review R1

- Reviewer role: fresh independent whole-card delivery reviewer R1; not the implementation Worker and not the parent/final adjudicator.
- Verdict: **APPROVED**.
- Severity: **P0/P1/P2 = `0/0/0`**.
- Reviewed card EOF: `TURN-34AT1 PARENT-REVIEW-7 PASSED ... FRESH-DUAL-INDEPENDENT-REVIEW-BUILD-PENDING`.
- Frozen source identities:
  - Cloud production `AutoCombatService.java`: `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
  - Cloud test `AutoCombatServiceTurnContractTest.java`: `bf7a671f6483b2461211f482561280d9cde07e8673ec77016fd12913f9d87221`, 1047 lines, 22 tests.
- Authority checked: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, current CR271 block in `docs/ACTIVE_WORK.md`, authoritative plan sections 14-19, HTTPS turn protocol, `docs/业务逻辑.md`, TURN-34AT1 true EOF, both repository statuses, and business baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

## Independent evidence

1. **Real public production path and public surface are retained.** The harness constructs the real `AutoCombatService`, real `BattleRadarService`, real `PackagedTemplateAssets`, production `TaskExecutionContextHolder`, `TurnGameClient`, and scripted `CloudTurnCommandPort`, then invokes public `probeWindowCombatStateReadOnly(...)`. The public API guards use only `Class.getMethods()/getMethod(...)`. Repair #4 removed the private collaborator-layout block; the test contains no `getDeclaredField(s)`, `getDeclaredMethod`, `getDeclaredConstructor`, `setAccessible`, Java-source file read, or source scan.
2. **Stage-1 exact battle flag and raw PNG contract are covered.** The positive path loads committed `images/template/battle/flag_battle.png`, paints it into an in-memory raw PNG for exact screen region `(1074,680,51,20)`, and proves `FREE -> IN_COMBAT` after exactly one command. The command is one index-0 `CAPTURE`, `UPLOAD_IMAGE`, 120-second timeout, with all non-capture unions null and both inner mechanics `clearPointerIfOverRegion`/`pixelChangeProbe` null. Result assertions bind exact current window metadata, action ID, frame purpose/content type/region/dimensions/source step, raw PNG bytes, and SHA.
3. **One command and fresh UUID are non-vacuously proved.** A shared real-service sequence drives the four command terminal statuses, three outcome terminal statuses, and one trusted completed capture. It asserts eight invocations, eight commands, exhausted scripted replies, and eight canonical pairwise-distinct UUIDs. An unscripted ninth command fails by construction, so no Stage-2/3, compensation, retry, resend, replay, or fallback is hidden.
4. **All seven terminal/uncertain cases preserve the state boundary.** `BUSY`, `DUPLICATE_ACTION_ID`, `TIMED_OUT_UNCERTAIN`, `INTERRUPTED_UNCERTAIN`, `FAILED`, `STOPPED`, and `DUPLICATE_OR_UNCERTAIN` each publish exactly one command, keep `IN_COMBAT`, and consume the sole scripted reply. Confirmed stop remains separately covered as a zero-command `TaskStopRequestedException` path.
5. **FAILED is protocol-legal.** The FAILED fixture uses `failedStepIndex=0` and step 0 `FAILED`; STOPPED and DUPLICATE_OR_UNCERTAIN retain null failed index with `NOT_RUN`. This reaches the intended terminal branches rather than validator/fallback behavior.
6. **Strict team-keyed 30-second defer matches `696a12b0`.** The gate tests cover first allow, same-team defer at 29,999 ms, other-team allow, allow at 30,000 ms, and same-team/same-window defer at `now+10 ms`. Current production retains the same `30_000L` team-keyed rule.
7. **No forbidden architecture was introduced by this card.** Production is byte-identical to the frozen SHA. The test adds no production hook, copied reducer, automatic retry/replay/resend, durable workflow, session, ledger, or TTL. `TaskRetryPolicy.none()` appears only in the frozen task metadata fixture and does not create retry behavior.
8. **Dirty/untracked protection observed.** Both repository statuses were read and left untouched. No Java, original card, authoritative plan, or status document was modified by this reviewer.

## Gates not executed

- Per assignment, no Maven/JUnit/compile/package, runtime/application/server/Task/UI, capture, input, or Git mutation was run.
- This approval is source/test-source review of the frozen whole-card bytes only. Stable-writer named-test and applicable Cloud compile remain parent-controlled completion gates; this R1 approval is not final card approval.

**Conclusion: APPROVED; no P0/P1/P2 finding. No approved business difference; equivalent migration against `696a12b0`.**

<!-- TRUE_EOF REVIEW_COMPLETE -->
