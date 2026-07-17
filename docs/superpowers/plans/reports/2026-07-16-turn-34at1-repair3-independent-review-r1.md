# TURN-34AT1 Repair #3 Independent Whole-Card Delivery Review R1

## Review identity and scope

- Role: independent whole-card delivery reviewer R1; not the implementation worker and not the parent final reviewer.
- Reviewed card: `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md` through its physical true EOF.
- Reviewed production: Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java`.
- Reviewed test: Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`.
- Authorities read independently: repository instructions/context, CR271 active-work top block, authoritative plan sections 14-19, HTTPS turn protocol, `docs/业务逻辑.md`, and the `696a12b0` migration baseline.
- Prohibited actions observed: no Java implementation edit, Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git mutation.

## Delivered identity

- Read-only production is 852 lines, SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`, exactly matching the frozen card identity.
- Delivered test is 1057 lines, SHA-256 `a326f50154468e3f008f9ceea5a778c909e08645f4b977d30f438236ba708767`, exactly matching the Repair #3 delivery identity.
- The delivery therefore changes test source only; production remains read-only for this card.

## Independent findings

### P0

None.

### P1

None.

### P2

None.

## Contract evidence

1. **Legal `FAILED` shape; `STOPPED` and uncertainty preserved.**
   - `AutoCombatServiceTurnContractTest.java:875-900` constructs `FAILED` with `failedStepIndex=0` and step 0 `FAILED`; `STOPPED` and `DUPLICATE_OR_UNCERTAIN` retain null failed index and `NOT_RUN` step results.
   - This is exercised through the public `TurnGameClient` created at `AutoCombatServiceTurnContractTest.java:942-948`, not by calling the validator helper directly.
   - `TurnGameClient.java:161-168` projects every command through `TurnInvocationResult.from`; `TurnInvocationResult.java:49-66` calls `TurnProtocolValidator.requireValid` for every completed command outcome. The validator's `TurnProtocolValidator.java:346-376` rules exactly accept the delivered three shapes.

2. **Terminal/uncertain results do not fabricate combat exit or success.**
   - Command uncertainty coverage at `AutoCombatServiceTurnContractTest.java:466-494` covers `BUSY`, `DUPLICATE_ACTION_ID`, `TIMED_OUT_UNCERTAIN`, and `INTERRUPTED_UNCERTAIN`; every case keeps both returned and stored state `IN_COMBAT`, emits exactly one command, and exhausts its only scripted reply.
   - Outcome coverage at `AutoCombatServiceTurnContractTest.java:503-530` covers `FAILED`, `STOPPED`, and `DUPLICATE_OR_UNCERTAIN`, explicitly keeps latest metadata `stopRequested=false`, preserves `IN_COMBAT`, emits one command, and exhausts the script.
   - Production `BattleRadarService.java:560-577` issues one capture only and converts every non-completed command/outcome into unavailable observation rather than success or an automatic retry. A `STOPPED` outcome checks the real task checkpoint before remaining unavailable.

3. **Eight commands, eight canonical fresh UUIDs, script exhaustion, and zero fallback.**
   - `AutoCombatServiceTurnContractTest.java:540-569` drives all seven terminal cases plus one trusted completed battle-flag capture through one shared service/context, calls the public probe exactly eight times, asserts eight command executions, checks the eighth command is completed, exhausts all replies, and validates eight IDs.
   - `AutoCombatServiceTurnContractTest.java:572-588` parses every ID as a canonical `UUID` and requires eight pairwise-distinct values.
   - `ScriptedCommandPort.execute` at `AutoCombatServiceTurnContractTest.java:1025-1036` fails immediately on any ninth/unscripted command, so retry, resend, Stage-2/3 fallback, or compensation cannot pass unnoticed.

4. **Same-team/same-window 30-second defer matches the baseline.**
   - `AutoCombatServiceTurnContractTest.java:639-657` locks the 29,999 ms defer, other-team independence, and 30,000 ms reopening boundary.
   - `AutoCombatServiceTurnContractTest.java:669-679` independently proves that the same team and same window at `now+10 ms` is deferred with positive retry delay.
   - Read-only production `AutoCombatService.java:812-827` keys `lastVerifyByTeam` by team (window only as blank-team fallback) and defers every nonnegative age below 30 seconds. The preserved `migration-baseline/696a12b0` source has the same gate rule at its lines 817-832.

5. **Minimal Stage-1 `CAPTURE` and raw-PNG correlation remain closed.**
   - `AutoCombatServiceTurnContractTest.java:394-440` proves one index-0 `CAPTURE`, no input/wait/match/local-service union, exact region `(1074,680,51,20)`, `UPLOAD_IMAGE`, 120-second timeout, exact current metadata, raw PNG dimensions/region/source-step index, and SHA correlation.
   - Repair #3 specifically closes both inner mechanics at `AutoCombatServiceTurnContractTest.java:417-423`: `clearPointerIfOverRegion()==null` and `pixelChangeProbe()==null`.
   - Production `BattleRadarService.java:548-624` uses one exact-window ROI capture, checks exact action/window/frame correlation and raw PNG/SHA/dimensions, and contains no fallback capture.

6. **No unauthorized business machinery or behavior difference.**
   - The reviewed Repair #3 changes are test-only; frozen production identity is unchanged.
   - The reviewed Stage-1 path creates one UUID per public invocation in `TurnGameClient.java:161-168` and owns no retry/cache/lifecycle/business interpretation (`TurnGameClient.java:20-25`).
   - No auto retry/replay/resend, session, ledger, TTL, or durable-workflow implementation was introduced by this delivery. No local OCR/business decision was added to the Stage-1 turn.
   - 无已批准业务差异；按 `696a12b0` 等价迁移。

## Verdict

**APPROVED — `P0/P1/P2=0/0/0`.**

The latest TURN-34AT1 Repair #3 production/test snapshot satisfies this independent whole-card delivery review. This approval does not replace R2, the parent final decision, the authorized named-test gate, or the applicable Cloud compile gate.

<!-- TRUE_EOF REVIEW_COMPLETE -->
