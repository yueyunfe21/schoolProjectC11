# TURN-28S2 Repair #1 Independent Whole-Card Delivery Review R2

- Role: independent whole-card delivery reviewer R2; not implementation owner and not parent/final reviewer.
- Independence: R1 report was not read or used.
- Verdict: **BLOCKED**.
- Severity: `P0/P1/P2=0/1/0`.
- Reviewed production SHA-256: `aa50ae7cb6fd9fe5c494225090ec123742d67c1faea9d154e7e01bafb1a72862` (3527 lines).
- Business authority: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved behavior difference.

## Scope and evidence read

- `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current CR271 top of `docs/ACTIVE_WORK.md`.
- Authority plan Sections 14-19 and the HTTPS turn protocol specification.
- The complete TURN-28S2 card through physical true EOF, including Repair #1 delivery and Parent Review #2.
- Current Cloud `NpcClickService.java`, protocol validator/client/context/checkpoint types, real Task callers, both repository statuses, and the strict-696 `NpcClickService.java` from Git object `696a12b0`.
- No Maven, runtime, application, server, Task, UI, capture, input, or Git mutation was run.

## Accepted source evidence

The Repair #1 source closes its original FAILED projection defect:

- Exactly four active sites call `executeAltShortcutTurn(...)`: generic `ALT_C/700` at `NpcClickService.java:638`, flying direct-combat `ALT_C/700` at `:678-679`, grounded direct-combat `ALT_A/350` at `:691`, and ordinary name-layer `ALT_4/400` at `:954-957`.
- Their surrounding strict-696 branch order, logs, fallback conditions, and delays remain unchanged from `696a12b0`.
- One reached site invokes one public `TurnGameClient.execute(...)` (`:3297-3300`). `TurnGameClient.invoke(...)` creates one fresh UUID and owns no retry/cache/lifecycle state.
- The payload is exactly ordered `INPUT KEY_TAP -> WAIT` with no requested frame (`NpcClickService.java:3285-3300`).
- Latest metadata is read from the exact bound client; device/window identity and positive rect are checked before invocation (`:3392-3403`), and the complete returned `TurnWindowMetadata` record is compared for equality (`:3349-3351`).
- Command BUSY, duplicate, timeout and interrupted uncertainty do not return success (`:3328-3335`); confirmed stop uses `TaskCheckpoint`, while unconfirmed STOPPED, duplicate/uncertain, metadata/step/frame mismatch and legal FAILED all throw `TaskFatalException` (`:3337-3387`). Only COMPLETED with both ordered steps COMPLETED reaches `return true`.
- The two excluded legacy private `inputProvider.pressAlt4()` sites remain at `:3424` and `:3455`. No S2-local retry/replay/resend/session/ledger/TTL/durable workflow was added.

## Blocking finding

### P1 - the real Wubei caller converts S2 fatal terminals into round recovery and later business actions

`NpcClickService` is a shared production Service and the four migrated mechanics are reached by real Wubei call paths, including `clickNpcSmart(...)` at `WubeiTask.java:1427`, `:1960`, and `:3399`, plus `tryDirectCombatTargetClick(...)` at `:3440`. However, the enclosing Wubei phase loop catches every `RuntimeException` at `WubeiTask.java:529-540`. `TaskFatalException` extends `RuntimeException`, and Wubei has no `TaskFatalException` exception/rethrow branch. The catch converts the S2 fatal into:

```java
roundState = recoverRoundAfterFailure(...);
phaseLoopGuard = 0;
continue;
```

That directly contradicts the frozen whole-card contract that BUSY/duplicate/timeout/interrupted uncertainty, FAILED, unconfirmed STOPPED, malformed/correlation/context/metadata drift are terminal with zero later action. On Wubei, those S2 outcomes can enter recovery and issue later navigation/input/business actions. This is not merely diagnostic exception typing: it changes the observable terminal guarantee that justified replacing the strict-696 boolean mechanics.

Xiuluo already demonstrates the required caller behavior at `XiuluoTaskV2.java:507-511` by explicitly rethrowing `TaskFatalException`; FiveRing also lets the exception escape its phase body. Wubei is the uncovered active caller.

## Required repair / re-review gate

- Keep the accepted TURN-28S2 four-site production mechanics and Repair #1 FAILED validation unchanged.
- Close the real Wubei caller boundary so `TaskFatalException` is rethrown before generic phase recovery; confirmed stop must continue through the existing checkpoint/stop path.
- Because the frozen TURN-28S2 exact write set currently makes callers read-only, the parent must either expand/correct the whole-card write set or assign the caller correction to an already-authorized existing complete card. It must not approve S2 while claiming terminal/zero-later-action is already end-to-end true.
- Re-run parent whole-card source review and both independent whole-card reviews against the resulting latest SHA. Named tests/build remain separate stable-writer gates.

<!-- TRUE_EOF REVIEW_COMPLETE -->

## Latest Round Re-review After Parent Scope Adjudication #3

- Role remains independent whole-card delivery reviewer R2; not implementation owner and not parent/final reviewer.
- Independence remains intact: no R1 report was read or used.
- Latest verdict: **APPROVED**.
- Latest severity: `P0/P1/P2=0/0/0`.
- Re-reviewed production SHA-256: `aa50ae7cb6fd9fe5c494225090ec123742d67c1faea9d154e7e01bafb1a72862`
  (3527 lines), byte-identical to the prior R2 round.

### Scope adjudication applied

I independently read the TURN-28S2 card through its new physical true EOF, including `PARENT REVIEWER-SCOPE
ADJUDICATION #3`, and re-read the authoritative TURN-35 registry, dependency and exact-write-set entries in the
plan. The previously reported Wubei generic `RuntimeException` catch risk is real, but it is outside this frozen
card's ownership:

- TURN-28S2 owns only Cloud `NpcClickService.java`; all callers are explicitly read-only. Its acceptance boundary
  is that non-confirmed-stop terminal outcomes leave the Service through the existing fatal path.
- Authoritative TURN-35 owns the complete Wubei turn wiring and exclusively writes `WubeiTask.java` (necessary DTOs
  may only be private nested types in that file). Its whole-task acceptance preserves the 14-state baseline and
  owns retry, fallback and terminal propagation, including the mandatory fatal-rethrow point identified here.
- Moving the caller repair into S2 would violate both cards' exact write sets. The earlier R2 P1 is therefore
  superseded for TURN-28S2 and remains a mandatory cross-card acceptance observation for TURN-35.

### Latest frozen-Service review evidence

- Exactly four active top-level Alt sites use `executeAltShortcutTurn(...)`: `ALT_C/700` at current lines 638 and
  678-679, `ALT_A/350` at line 691, and `ALT_4/400` at lines 954-957. Their surrounding strict-696 business order,
  branches, fallback conditions and waits remain equivalent to the `696a12b0` mirror.
- Each reached site performs one public `TurnGameClient.execute(...)` invocation with one client-generated fresh
  UUID, the latest exact bound metadata, ordered `INPUT KEY_TAP -> WAIT`, and `requestFrame=false`.
- Returned metadata is compared to the complete expected `TurnWindowMetadata`; malformed metadata, step count,
  step index/type/status, local/match payload or unexpected frame cannot continue.
- Only command `COMPLETED` plus outcome `COMPLETED` with both ordered steps `COMPLETED` returns. BUSY, duplicate,
  timeout/interrupted uncertainty, legal or malformed `FAILED`, unconfirmed `STOPPED`,
  `DUPLICATE_OR_UNCERTAIN`, correlation/metadata drift and malformed evidence terminate inside the Service.
  Confirmed stop uses `TaskCheckpoint`; every other terminal uses `npcClickFatal(...)`.
- The two excluded legacy private `inputProvider.pressAlt4()` sites remain untouched at current lines 3424 and
  3455. No S2-local retry, replay, resend, session, ledger, TTL, durable workflow, local OCR or business truth was
  introduced.

No unresolved P0/P1/P2 remains within the frozen TURN-28S2 whole-card boundary. This approval is an independent
delivery-review verdict only; it is not parent final approval and does not satisfy the separate named-test/build
gates. No Maven, runtime, application, server, Task, UI, capture, input or Git mutation was run.

<!-- TRUE_EOF REVIEW_COMPLETE -->
