# TURN-34AT1 Repair #3 Independent Whole-Card Delivery Review R2

- Reviewer role: independent whole-card delivery reviewer R2; not the implementation owner and not the parent final reviewer.
- Reviewed at: `2026-07-16T13:24:09.174-04:00`.
- Scope read independently: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the CR271 top block in `docs/ACTIVE_WORK.md`, authoritative plan sections 14-19, the HTTPS turn protocol, `docs/业务逻辑.md`, the complete TURN-34AT1 card through its true EOF, current Cloud production/test source, the preserved `696a12b0` source, and both repositories' status.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input or Git mutation was run.

## Verdict

**BLOCKED - `P0/P1/P2=0/1/0`.** Repair #3 closes its three named findings, but the delivered whole-card test still violates the card's explicit no-private-reflection acceptance rule and its delivery statement is therefore inaccurate.

## P1-1 - Production private-field reflection remains in the named test

- Frozen contract: `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md:24-25` explicitly forbids private reflection/source scans. The Repair #3 claim and delivery repeat that constraint at card lines 269 and 296.
- Exact source evidence: `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java:704-716` calls `AutoCombatService.class.getDeclaredFields()`, enumerates the production class's private field types, and asserts both absence of legacy collaborators and presence of `TaskExecutionContextHolder` from that private layout.
- Impact: this is private production reflection, directly contradicts the frozen test contract, and couples the named contract test to an implementation-private field layout rather than the required public production path. The delivery's statement that no private reflection is used is false even though the reflected block predates the three Repair #3 edits. A whole-card reviewer cannot approve the current frozen test SHA.
- Required repair: in the same whole-card implementation ownership, remove the `getDeclaredFields()`-based private collaborator assertions. Preserve the public API guard and the real production-constructor/public-path harness. Record legacy collaborator absence through parent source review plus the frozen production SHA, not through test reflection or a source scan. Do not modify production, POM, resources, callers, or any other test/card, and do not weaken the Repair #3 behavioral assertions below.
- Re-review point: a new canonical whole-card delivery must provide the new test SHA and show zero `getDeclaredFields`/private-field reflection/source-scan usage while retaining all legal-terminal, command-count, UUID, gate and CAPTURE assertions.

## Repair #3 findings independently confirmed closed

- Legal `FAILED` shape: `AutoCombatServiceTurnContractTest.java:875-900` emits `failedStepIndex=0` and step 0 `FAILED`; `STOPPED` and `DUPLICATE_OR_UNCERTAIN` retain null failed index and `NOT_RUN`. This reaches the real validator rules at `TurnProtocolValidator.java:346-375` through `TurnGameClient`/`TurnInvocationResult`, rather than failing fixture construction.
- Terminal/uncertain preservation: `AutoCombatServiceTurnContractTest.java:466-530` keeps command-level BUSY/duplicate/timeout/interrupted and outcome-level FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN at `IN_COMBAT`, with exactly one command per invocation and exhausted scripted replies.
- Eight commands/UUID/exhaustion/no fallback: `AutoCombatServiceTurnContractTest.java:540-569` drives seven terminal cases plus one trusted Stage-1 hit through one harness, asserts exactly eight executions, exhausted replies and eight canonical pairwise-distinct UUIDs. `ScriptedCommandPort.execute` at lines 1025-1036 fails any unexpected extra command.
- Strict same-team/same-window 30-second defer: test lines 637-679 assert 29,999 ms deferred, 30,000 ms allowed, and same-window `+10 ms` deferred. Current production lines 812-827 and preserved `696a12b0` lines 817-832 use the same team-keyed gate with no same-window exception.
- Minimal CAPTURE: test lines 404-426 assert one index-0 `CAPTURE`, exact `(1074,680,51,20)`, `UPLOAD_IMAGE`, 120 seconds, all outer unions null, plus `clearPointerIfOverRegion()==null` and `pixelChangeProbe()==null`; lines 428-440 retain exact metadata/raw-PNG/SHA correlation.
- Frozen identities independently recomputed: production `AutoCombatService.java` is 852 lines / SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`; test is 1057 lines / SHA-256 `a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767`.
- No Repair #3 production change or new business retry/replay/resend/session/ledger/TTL/durable workflow was found. The three accepted test corrections preserve the `696a12b0` business boundary; the sole blocker is the pre-existing but explicitly forbidden private-reflection assertion.

TRUE_EOF REVIEW_COMPLETE
