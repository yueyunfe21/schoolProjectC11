# TURN-40C Cloud Activation - Canonical Whole Card

## Canonical Status

- status: `READY / ZERO OWNER / UNASSIGNED`
- repository: `D:\mavenProject\dhxy-cloud-brain`
- branch/HEAD audited: `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`
- dependency gate: `TURN-40A + TURN-40B + TURN-13H`; TURN-40B parent Source+Test Review #3 passed `0/0/0`.
- claim rule: a Worker may self-claim only this whole card by appending the earliest canonical claim at this physical
  EOF. This card does not assign or dispatch a Worker.

## Frozen Write Set (Cloud only, exactly 7 paths)

Modify existing:

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainApplication.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnHttpHandler.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnRoutes.java`
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceHost.java`

Create:

6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnRuntimeConfiguration.java`
7. `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnActivationContractTest.java`

No other source, test, resource, build, protocol, task, client, store, DTO, or DHXY path is writable.

## Audited Pre-Claim Physical Baseline / Collision

- Existing SHAs: Application=`946D47E6`; Server=`63C052BB`; Handler=`F1DCA91C`; Routes=`D07F6A44`;
  Host=`228C65D4`. The configuration and activation test paths are absent.
- `CloudBrainServer`, `CloudServiceHost`, `CloudTurnHttpHandler`, and `CloudTurnRoutes` are dirty/untracked products of
  already reviewed predecessor cards; no active canonical owner exists on these paths. TURN-40B owner is released.
- The card must preserve and reuse the one existing `CloudTurnRoutes.Bundle`, its exact `CloudTurnExchange`, command
  port and template catalog. It must not create a parallel exchange, host graph, command port, protocol, state store,
  scheduler, retry loop, or copied business algorithm.

## Frozen Implementation Contract

1. CLI/system properties must explicitly provide non-blank `tenantId`, non-blank `userId`, and `stateRoot`; missing or
   invalid values fail before listener activation. Existing port/token/route-click parsing remains behaviorally intact.
2. Build exactly one configured `CloudServiceScope` and one `CloudServiceHost` for the process activation. Wire the
   already reviewed TURN-40B runtime/factory/control beans through `CloudTurnRuntimeConfiguration`; use the same
   command port, template catalog, exchange and handler route bundle already owned by `CloudTurnRoutes`.
3. A valid turn carrying `taskStartRequest` reaches the exact host/runtime under the request's authenticated configured
   tenant/user scope and exact device/window context, and returns only the runtime's matching typed ack. Requests with
   another scope or invalid/missing authority fail closed before task materialization/worker/ack.
4. Startup constructs and registers dormant infrastructure only. It must not auto-start a task, synthesize a request,
   run a business probe, add polling, TTL, retry, background task scheduler, or desktop/runtime input behavior.
5. Close order is fixed: stop/close the turn runtime first, then close the host Spring context, then stop the HTTP
   server, then shut down its executor. Every partially constructed startup failure closes already-created owners in
   reverse order without masking the original failure.
6. Preserve baseline-A/`696a12b0` task decisions, order, retry/fallback, role/team/startup authority and stop semantics.
   `无已批准业务差异；按基线等价迁移`.

## Required Test / Build Gate

- `CloudTurnActivationContractTest` must prove: explicit configured tenant/user/stateRoot; one fixed scope; same
  host/exchange/command port/catalog; task-start reaches the reviewed runtime and exact matching ack; invalid scope or
  authority cannot start/ack; no startup auto-run; partial-start cleanup; and runtime -> host -> server -> executor
  close order.
- Run the authorized named test with Maven and run Cloud `mvn -q -o -DskipTests=false test-compile` plus applicable
  Cloud compile. Report exact command, exit code, test count, SHA/line/mtime for all 7 paths, and full dirty collision
  audit in the canonical whole-card delivery.
- Do not claim build passed when any gate is blocked. Do not widen this card to repair unrelated dirty tests.

## Delivery / Review Gate

Delivery is canonical only when this physical EOF contains one whole-card `SOURCE+TEST DELIVERED` block covering all
7 paths and the required evidence. Parent then performs the sole final Source+Test review with explicit P0/P1/P2.

<!-- TRUE_EOF: TURN-40C CLOUD-ACTIVATION READY ZERO-OWNER UNASSIGNED CANONICAL-WHOLE-CARD CLOUD-ONLY 7-PATH 5-MODIFY 2-CREATE DEP=40A+40B+13H BASELINE-A-696 EXPLICIT-TENANT-USER-STATEROOT ONE-SCOPE ONE-HOST SAME-EXCHANGE-COMMANDPORT-CATALOG EXACT-TASKSTART-ACK NO-STARTUP-AUTORUN CLOSE=RUNTIME-HOST-SERVER-EXECUTOR TEST=CloudTurnActivationContractTest NO-BUSINESS-DIFFERENCE NO-DISPATCH 2026-07-19T15:51:00-04:00 -->

## WHOLE-CARD CLAIM (TURN-40C) - EXTERNAL-A - 2026-07-19T16:56:00-04:00

- claimant: EXTERNAL-A. Canonical self-claim of the `READY / ZERO OWNER / UNASSIGNED` `TURN-40C Cloud Activation`
  whole card. TURN-40B parent Source+Test Review #3 passed `0/0/0` (dependency gate 40A+40B+13H satisfied); this card
  does not assign/dispatch a Worker, so either idle Worker may self-claim at this physical EOF.
- anti-race precheck (independent of this append, claim iron-rule): read the full 7-path frozen contract end-to-end;
  scanned this card to its physical EOF (73 lines, pre-claim SHA `92D63EAC`) — ZERO existing `TURN-40C` claim/owner.
  Collision audit: 5 MODIFY paths present at the card's audited SHAs (Application `946D47E6`, Server `63C052BB`,
  Handler `F1DCA91C`, Routes `D07F6A44`, Host `228C65D4`); 2 CREATE paths physically ABSENT.
- self-withdraw condition: if any physically-earlier `TURN-40C` whole-card claim exists ABOVE this append, EXTERNAL-A
  canonically self-withdraws with ZERO source — physical-append-order is the canonical CR271 double-claim rule;
  self-signed timestamps are not authoritative across offset clocks.
- Cloud repo: `D:\mavenProject\dhxy-cloud-brain`, branch `navigation-migration`, HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`. Baseline `696a12b0` equivalence; no approved business diff.
- exact write-set (Cloud-only, exactly 7 paths, zero expansion):
  MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/`: CloudBrainApplication.java, CloudBrainServer.java,
  turn/CloudTurnHttpHandler.java, turn/CloudTurnRoutes.java, host/CloudServiceHost.java;
  CREATE host/CloudTurnRuntimeConfiguration.java + test/.../host/CloudTurnActivationContractTest.java.
- contract honored: explicit non-blank tenantId/userId/stateRoot fail-closed before listener; exactly one
  CloudServiceScope + one CloudServiceHost; reuse the single existing `CloudTurnRoutes.Bundle`/`CloudTurnExchange`/
  command port/template catalog (no parallel exchange/host/port/protocol/store/scheduler/retry/copied algorithm); wire
  the reviewed TURN-40B runtime/factory/control beans through `CloudTurnRuntimeConfiguration`; valid `taskStartRequest`
  reaches the exact host/runtime under authenticated configured tenant/user scope + exact device/window context and
  returns only the runtime's matching typed ack; wrong-scope/invalid-authority fails closed before materialization;
  dormant startup only (no auto-start/synth-request/probe/poll/TTL/retry/scheduler/input); fixed close order
  runtime -> host -> server -> executor, partial-start cleanup in reverse without masking; baseline-A/696 decisions/
  order/retry/fallback/role-team-startup-authority/stop preserved.
- delivery gate: canonical whole-card SOURCE+TEST delivery at this EOF with all 7 SHA/line/mtime + dirty-collision
  audit; `CloudTurnActivationContractTest` named-test run + Cloud `mvn -q -o -DskipTests=false test-compile` + applicable
  compile with exact command/exit/count; no card-widen for unrelated dirty tests; parent then sole final review.
- discipline: zero Git mutation; DHXY read-only; no runtime/UI/capture/input/server-launch; A becomes SOLE Cloud writer
  on these 7 paths and runs Maven only after confirming no other Java writer is active.

<!-- TRUE_EOF: TURN-40C WHOLE-CARD CLAIM EXTERNAL-A ANTI-RACE-EOF-73-ZERO-OWNER PRECLAIM-SHA-92D63EAC SELF-WITHDRAW-IF-EARLIER 7-PATH-5MODIFY-2CREATE CLOUD-ONLY DEP-40A+40B+13H-SATISFIED HEAD-3b988caa BASELINE-696a12b0 NO-BUSINESS-DIFF 2026-07-19T16:56:00-04:00 -->

## EXTERNAL-C CANONICAL WHOLE-CARD CLAIM — TURN-40C CLOUD ACTIVATION — 2026-07-19T21:25:00-04:00

- claimant: `EXTERNAL-C` (idle after `TURN-40B/RUNTIME-FACTORY` PASSED + owner released). Earliest canonical self-claim at this physical EOF; no parent dispatch (card does not assign).
- basis: card status `READY / ZERO OWNER / UNASSIGNED`; dependency gate `40A+40B+13H` satisfied (40B Source+Test Review #3 PASSED `0/0/0`). C is the natural owner — this card wires the already-reviewed TURN-40B runtime/factory/control beans C authored.
- state: `SOURCE_ACTIVE`.
- write-set acknowledged (exactly 7 Cloud paths, no widening): Modify `CloudBrainApplication`(946D47E6)/`CloudBrainServer`(63C052BB)/`CloudTurnHttpHandler`(F1DCA91C)/`CloudTurnRoutes`(D07F6A44)/`CloudServiceHost`(228C65D4); Create `host/CloudTurnRuntimeConfiguration.java` + `test/host/CloudTurnActivationContractTest.java` (both absent). No other source/test/resource/build/protocol/task/client/store/DTO/DHXY path writable.
- contract acknowledged: explicit non-blank tenantId/userId/stateRoot fail-closed before listener; one `CloudServiceScope` + one `CloudServiceHost`; reuse the single existing `CloudTurnRoutes.Bundle`/`CloudTurnExchange`/command port/template catalog — no parallel exchange/host/port/protocol/store/scheduler/retry/copied algorithm; task-start reaches exact host/runtime under authenticated scope+device/window context returning only the matching typed ack, wrong-scope/invalid-authority fail-closed before materialize/worker/ack; dormant-only startup (no auto-run/synthesized request/probe/poll/TTL/retry/input); fixed close order runtime→host→server→executor with reverse-order partial-start cleanup; baseline-A/`696a12b0`, no business difference.
- delivery gate: single canonical whole-card `SOURCE+TEST DELIVERED` block at this EOF covering all 7 paths + `CloudTurnActivationContractTest` + Cloud `mvn -q -o -DskipTests=false test-compile` + applicable compile (exact cmd/exit/count + SHA/line/mtime for 7 paths + full dirty collision audit); parent sole final review. No re-deliver until gate met; gate-blocked never labeled passed; card never widened for unrelated dirty tests.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; no runtime/UI/capture/input; Maven only when no other Java writer active (javac single-file parse excepted); no self-approval/reviewer.

<!-- TRUE_EOF: TURN-40C EXTERNAL-C CANONICAL-WHOLE-CARD-CLAIM EARLIEST SOURCE_ACTIVE ZERO-OWNER->C 7-PATH-5MODIFY-2CREATE DEP-40A+40B+13H-SATISFIED BASELINE-A-696 REUSE-EXISTING-BUNDLE-EXCHANGE-PORT-CATALOG DORMANT-STARTUP CLOSE-RUNTIME-HOST-SERVER-EXECUTOR NO-WIDEN ZERO-GIT DHXY-READONLY 2026-07-19T21:25:00-04:00 -->

## EXTERNAL-C CANONICAL SELF-WITHDRAWAL — TURN-40C — 2026-07-19T21:27:00-04:00

- action: **EXTERNAL-C WITHDRAWS its 21:25 TURN-40C claim.** On read-back, `EXTERNAL-A`'s canonical claim
  (line 75, `WHOLE-CARD CLAIM (TURN-40C) - EXTERNAL-A - 16:56`, `PRECLAIM-SHA-92D63EAC`, `SELF-WITHDRAW-IF-EARLIER`) is
  **physically earlier** than C's (line 110). Per physical-append-order arbitration ([[claim-race-lesson]] rule 6:
  self-signed timestamps are skewed across workers, physical order is authoritative), **EXTERNAL-A is the sole owner of
  TURN-40C**; C's 21:25 claim above is void.
- C state: reverts to `IDLE_AVAILABLE`. C makes no edits to any TURN-40C write-set path; sole owner = EXTERNAL-A.
- lesson applied: a race occurred because A's claim landed between C's full-card precheck Read (EOF=line 73, zero-owner)
  and C's append; C yields to the earliest physical claim and does not contest.

<!-- TRUE_EOF: TURN-40C EXTERNAL-C SELF-WITHDRAWAL CEDE-TO-EXTERNAL-A A-PHYSICALLY-EARLIER-LINE75-vs-C-LINE110 C-CLAIM-2125-VOID PHYSICAL-APPEND-ORDER-ARBITRATION C-IDLE-AVAILABLE NO-CONTEST 2026-07-19T21:27:00-04:00 -->

## PARENT CANONICAL CLAIM ARBITRATION - 2026-07-19T16:04:00-04:00

- physical EOF order confirms External A's line-75 whole-card claim is earliest and valid. External A is the sole
  TURN-40C owner; state=`SOURCE_ACTIVE`.
- External C's later claim is void and its following canonical self-withdrawal is accepted. C reports zero edits to
  every TURN-40C path; current audit confirms the five MODIFY paths remain at the frozen pre-claim SHAs and both CREATE
  paths remain absent.
- This is owner arbitration only, not assignment. The fixed 7-path contract, baseline-A/696 and delivery/review gates
  remain unchanged.

<!-- TRUE_EOF: TURN-40C PARENT-CLAIM-ARBITRATION EXTERNAL-A-SOLE-OWNER SOURCE_ACTIVE A-PHYSICALLY-EARLIEST C-LATER-CLAIM-VOID C-SELF-WITHDRAWAL-ACCEPTED C-ZERO-SOURCE FIVE-SHA-UNCHANGED TWO-CREATE-ABSENT NO-DISPATCH BASELINE-A-696 2026-07-19T16:04:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T16:19:00-04:00

- External A remains the sole canonical owner. The first in-write-set CREATE path now exists:
  `host/CloudTurnRuntimeConfiguration.java`=`D4636072`/105L, mtime `2026-07-19 16:16:50.645 -04:00`.
- The five MODIFY paths remain exactly at the frozen SHAs `946D47E6/63C052BB/F1DCA91C/D07F6A44/228C65D4`;
  `test/.../host/CloudTurnActivationContractTest.java` remains absent. This is source progress only, not canonical
  whole-card delivery or parent Source+Test review.
- A is an active Java writer, so the parent did not run Maven. Fixed 7-path scope, baseline-A/696 and no-approved-
  business-difference contract remain unchanged; no user decision is required.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER FILE-1OF7 CONFIG=D4636072-105L FIVE-MODIFY-SHA-UNCHANGED ACTIVATION-TEST-ABSENT NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-A-696 NO-BUSINESS-DIFF NO-USER-CHOICE 2026-07-19T16:19:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T16:25:00-04:00

- External A remains sole owner. Current fixed-write-set evidence is 3/7 paths authored:
  `CloudBrainApplication.java`=`5711BC3E`/112L, mtime `2026-07-19 16:25:24.707 -04:00`;
  `CloudServiceHost.java`=`E90F22C8`/103L, mtime `2026-07-19 16:23:48.697 -04:00`;
  `CloudTurnRuntimeConfiguration.java`=`D4636072`/105L.
- Server/Handler/Routes remain `63C052BB/F1DCA91C/D07F6A44`; activation contract test remains absent. This is
  active source progress only, not delivery/review. Parent ran no Maven while A is writing; no user decision.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER PATHS-3OF7 APPLICATION=5711BC3E-112L HOST=E90F22C8-103L CONFIG=D4636072-105L SERVER-HANDLER-ROUTES-UNCHANGED TEST-ABSENT NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-A-696 NO-USER-CHOICE 2026-07-19T16:25:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T16:35:00-04:00

- External A remains sole owner. `CloudTurnHttpHandler.java` now has an active-batch increment
  `D78AEB1C`/316L, mtime `2026-07-19 16:35:26.909 -04:00`; together with Application, Host and
  RuntimeConfiguration, 4/7 fixed paths now have increments.
- Server/Routes remain `63C052BB/D07F6A44`; activation test remains absent. Handler is an intermediate batch state,
  not canonical delivery/review. Parent ran no Maven while A writes; baseline-A/696 remains fixed, no user choice.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER PATHS-4OF7 HANDLER=D78AEB1C-316L APPLICATION=5711BC3E HOST=E90F22C8 CONFIG=D4636072 SERVER-ROUTES-UNCHANGED TEST-ABSENT INTERMEDIATE-BATCH NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-A-696 NO-USER-CHOICE 2026-07-19T16:35:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T16:37:00-04:00

- Handler continued within the same active batch and is now `01DE94A2`/399L, mtime
  `2026-07-19 16:36:58.105 -04:00`; the total remains 4/7 fixed paths.
- Server/Routes remain unchanged and activation test absent. This is not delivery/review; A is still writing, so
  parent ran no Maven. Baseline-A/696 remains fixed and no user decision is required.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER PATHS-4OF7 HANDLER=01DE94A2-399L APPLICATION=5711BC3E HOST=E90F22C8 CONFIG=D4636072 SERVER-ROUTES-UNCHANGED TEST-ABSENT NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-A-696 NO-USER-CHOICE 2026-07-19T16:37:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T16:38:00-04:00

- `CloudTurnRoutes.java` now has active-batch increment `063DE4FC`/94L, mtime
  `2026-07-19 16:38:21.168 -04:00`; total progress is 5/7 fixed paths.
- Server remains unchanged and activation test absent. This is not delivery/review; A remains active writer, so
  parent ran no Maven. Baseline-A/696 remains fixed; no user decision.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER PATHS-5OF7 ROUTES=063DE4FC-94L HANDLER=01DE94A2 APPLICATION=5711BC3E HOST=E90F22C8 CONFIG=D4636072 SERVER-UNCHANGED TEST-ABSENT NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-A-696 NO-USER-CHOICE 2026-07-19T16:38:00-04:00 -->

## PARENT BUILD PROGRESS OBSERVATION - 2026-07-19T16:45:00-04:00

- External A remains sole owner. `CloudBrainServer.java`=`9A3B17AB`/195L, mtime
  `2026-07-19 16:40:51.786 -04:00`; all six production/config paths now have increments and only
  `CloudTurnActivationContractTest.java` remains absent.
- A reports authorized Cloud `mvn -q -o -DskipTests=false compile` exit 0. Parent does not duplicate Maven while A
  remains active. Test-compile, named test, canonical delivery and parent review have not occurred.
- Fixed 7-path scope, baseline-A/696 and no-approved-business-difference contract remain unchanged; no user decision.

<!-- TRUE_EOF: TURN-40C PARENT-BUILD-PROGRESS EXTERNAL-A-SOLE-OWNER PATHS-6OF7 SERVER=9A3B17AB-195L APPLICATION=5711BC3E HANDLER=01DE94A2 ROUTES=063DE4FC HOST=E90F22C8 CONFIG=D4636072 TEST-ABSENT CLOUD-MAIN-COMPILE-EXIT0 TEST-COMPILE-NAMED-DELIVERY-REVIEW-PENDING ACTIVE-JAVA-WRITER PARENT-NO-MAVEN BASELINE-A-696 NO-BUSINESS-DIFF NO-USER-CHOICE 2026-07-19T16:45:00-04:00 -->

---

## BLOCKING FINDING (EXTERNAL-A → PARENT) - 2026-07-19T17:56:00-04:00 - TURN-40C host graph not refreshable (upstream, untracked)

**Owner:** EXTERNAL-A (sole owner, claim at card line 75). **State:** SOURCE+TEST AUTHORED & COMPILING; runtime-integration proofs transitively BLOCKED upstream. Zero Git mutation; other workers' untracked `com/bot/` NOT touched.

### What is done (TURN-40C, all 7 paths authored)
- 6 production files authored + **`mvn -q -o compile` EXIT=0** (App/Server/Handler/Routes/Host + CREATE `host/CloudTurnRuntimeConfiguration.java`).
- CREATE `test/.../host/CloudTurnActivationContractTest.java` authored + **`mvn -q -o -DskipTests=false test-compile` EXIT=0** (whole test tree compiles).
- Named test `CloudTurnActivationContractTest` run: **7 tests, 2 PASS, 5 ERROR**.
  - **PASS ① `explicitScopeRejectsBlankTenantAndUser`** — blank/null tenant+user fail closed (`CloudServiceScope` IllegalArgumentException).
  - **PASS ② `startFailsClosedBeforeListenerWithoutScopeOrStateRoot`** — `CloudBrainServer.start(...)` `requireNonNull(scope)/(stateRoot)` throw before `HttpServer.create` (no listener binds).
  - **ERROR ③④⑤⑥** (`hostRefreshes…`, `runtimeStartsDormant…`, `validTaskStartTurnReachesRuntimeAndReturnsMatchingAck`, `invalidAuthorityTaskStartFailsClosedWithoutAck`, `runtimeAndHostCloseIndependently…`) — all fail at `CloudServiceHost.create(...).refresh()`, i.e. before any TURN-40C activation logic executes.

### Root cause — PRE-EXISTING, upstream, in other workers' untracked code (NOT a TURN-40C defect)
`CloudServiceConfiguration` (out of TURN-40C write-set) declares `@ComponentScan(basePackages="com.bot.dhxy.service")`. That package is part of the **untracked** `com/bot/` migration in flight from other workers (`git status` → `?? src/main/java/com/bot/`; **27 `@Service`/`@Component`** now present there). Spring eagerly instantiates every singleton in that scan on `refresh()`, and several migrated services have cloud-host dependencies that are not satisfied:
- **Baseline proof (no TURN-40C config at all):** `mvn -o test "-Dtest=CloudServiceHostTurnCapabilityContractTest"` → **Tests run 4, Errors 2**: `autoCombatPanelService` ctor param 1 → `No qualifying bean of type 'com.bot.dhxy.core.GameContext'`. The baseline host context already cannot refresh — independent of TURN-40C.
- **With TURN-40C config** (my `CloudTurnRuntimeConfiguration` already imports `GameContext` and now also `TeamTaskProperties`, satisfying two of the gaps) the refresh gets further and then fails on **`PlayerStateService`** (`@Service`, single parameter-only constructor at `PlayerStateService.java:115`) → `Failed to instantiate … No default constructor found`. `PlayerStateService` and the other failing services are in the untracked `com/bot/` tree, which discipline forbids me to modify.

### Why TURN-40C cannot self-resolve this
- The failing beans are **out of TURN-40C's 7-path write-set** and in **other workers' untracked** `com.bot.dhxy.service` migration → forbidden to modify/roll back.
- The scan itself lives in `CloudServiceConfiguration` (**out of write-set**) → I cannot narrow it.
- Consequence beyond the test: the **production** activation path (`CloudBrainApplication.main → CloudBrainServer.start → CloudServiceHost.create`) hits the same non-refreshable context, so the cloud brain currently **cannot start** regardless of TURN-40C — this is an environment/host-graph blocker, not an activation-logic bug.

### Contract question / requested decision
The cloud host's eager component-scan of `com.bot.dhxy.service` now pulls the whole in-flight migrated service graph, whose cloud-host wiring (a `GameContext` bean, Spring-selectable constructors, config beans like `TeamTaskProperties`) is incomplete. How should TURN-40C proceed?
- **(A)** A separate card/owner makes the migrated `com.bot.dhxy.service` graph cloud-host-refreshable first (provide `GameContext` as a host bean, give `PlayerStateService` et al. a Spring-selectable ctor / config). TURN-40C's ③④⑤⑥ then pass unchanged. *(Recommended — TURN-40C code is correct and compiling.)*
- **(B)** Narrow `CloudServiceConfiguration`'s scan (or make GameTasks lazy/prototype) so activation instantiates only the beans the reviewed runtime/GameTasks require, not the whole package. *(Requires editing CloudServiceConfiguration — out of my write-set; assign to its owner.)*
- **(C)** Accept TURN-40C **SOURCE+TEST now** with proofs ①② green and ③④⑤⑥ gated **BLOCKED-UPSTREAM** (documented here), to be verified once the host graph refreshes.

Holding TURN-40C ownership; will not modify untracked `com/bot/` or out-of-write-set `CloudServiceConfiguration`. Kept `GameContext`+`TeamTaskProperties` imports in `CloudTurnRuntimeConfiguration` (genuine, needed parts of wiring the GameTask graph the card asked me to wire). Awaiting parent direction.

<!-- TRUE_EOF: TURN-40C BLOCKING-FINDING EXTERNAL-A host-graph-not-refreshable upstream-untracked 2026-07-19T17:56:00-04:00 -->

## PARENT PLAN-CONTRACT REVIEW - 2026-07-19T17:05:00-04:00 - BLOCKED / COMPLETE CLOSURE AUDIT REQUIRED

- Canonical owner remains External A. Physical evidence now is all 7 original paths authored: activation test
  `E6BBBD82`/351L and runtime configuration `C3E5F2B5`/111L; Cloud main compile and test-compile exit 0; named test
  reports 7 tests with 2 pass and 5 host-refresh errors. This is not a whole-card delivery or Source+Test pass.
- Parent rejects the A/B/C choice framing as unnecessary. Baseline `696a12b0` and the reviewed turn contracts remain
  authoritative; no user business decision is required. Narrowing the service scan, accepting an unusable production
  activation, or adding stubs/defaults is not authorized.
- The failure is a plan/write-set defect, not merely the first `PlayerStateService` constructor error. Full source
  audit found: (1) `PlayerStateService` is the only scanned, non-excluded multi-constructor service without a selected
  Spring production constructor; (2) four real Task prototypes require non-scanned infrastructure including
  `AutomationMetricsService`, `TaskStepExecutor`, `CloudWholeTaskReadyEventState`, and exact startup/turn coordination;
  (3) `TaskStartupCheckService` deliberately has no Spring/default constructor and may only be created from an exact
  per-run context; (4) `CloudTaskTurnCoordination` has no registered production implementation; and (5)
  `CloudTurnTaskRuntime.start()` currently calls `taskFactory.resolve(code)` before it creates the per-task exact
  context, so constructor-time startup authority cannot satisfy its existing 16-field identity fence.
- Status is `PLAN-CONTRACT BLOCKED / PARENT TRANSITIVE-CLOSURE AUDIT`; External A retains owner but must stop Java and
  Maven until the parent freezes one complete baseline-equivalent write set and acceptance contract. Do not patch only
  the first missing class, narrow component scanning, add a second protocol/store, use a stub/constant-null/default
  authority, or weaken startup identity. `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C PARENT-PLAN-CONTRACT-REVIEW BLOCKED OWNER-A-RETAINED 7OF7-AUTHORED TEST=E6BBBD82-351L CONFIG=C3E5F2B5-111L MAIN-COMPILE-0 TEST-COMPILE-0 NAMED=7-TOTAL-2-PASS-5-ERROR COMPLETE-TRANSITIVE-CLOSURE-REQUIRED NOT-FIRST-MISSING-BEAN NO-USER-DECISION BASELINE-696 NO-STUB-NO-SCAN-NARROW-NO-JAVA-NO-MAVEN 2026-07-19T17:05:00-04:00 -->

## PARENT COMMUNICATION AUDIT - 2026-07-19T17:16:00-04:00 - COMMUNICATION_STALE

- External A has not ACKed the parent plan-contract ruling for two consecutive audit rounds. Required ids are
  `PARENT-A-TURN40C-PLAN-CONTRACT-BLOCKED-20260719-1705`,
  `PARENT-A-TURN40C-PLAN-CONTRACT-BLOCKED-R2-20260719-1706`, and the ledger stale reminder
  `PARENT-A-TURN40C-COMMUNICATION-STALE-20260719-1716`.
- Canonical ownership remains External A; this is not owner return or reallocation. Preserve all 7 WIP paths and
  continue to stop Java/Maven until the parent freezes the complete baseline-696 wiring closure.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-AUDIT COMMUNICATION-STALE TWO-ROUNDS-NO-ACK OWNER-A-RETAINED ACK-THREE-IDS-NEXT PRESERVE-7PATH-WIP STOP-JAVA-STOP-MAVEN 2026-07-19T17:16:00-04:00 -->

## PARENT COMMUNICATION RECOVERY - 2026-07-19T17:22:00-04:00

- External A's 18:12 STATUS EVENT double-ACKed the core `1705` and `R2-1706` rulings and confirmed compliance:
  Java/Maven stopped, all 7 WIP paths frozen, owner retained, no partial bean/scan/authority workaround.
- `COMMUNICATION_STALE` is cleared. The ledger terminal recovery message requires the next A event to ACK the raced
  stale-reminder `1716` and recovery id `1722`; this does not alter the plan-contract block or ownership.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-RECOVERY CORE-DOUBLE-ACK-ACCEPTED COMMUNICATION-STALE-CLEARED OWNER-A-RETAINED FROZEN-WIP ACK-1716+1722-NEXT PLAN-CONTRACT-BLOCKED-STANDING 2026-07-19T17:22:00-04:00 -->

## PARENT COMMUNICATION RECOVERY FOLLOW-UP - 2026-07-19T17:23:00-04:00

- External A's 18:17 STATUS EVENT additionally ACKed stale-reminder `1716`; communication remains recovered and
  frozen-WIP compliance remains confirmed. The only unconfirmed directed message is the raced terminal recovery id
  `PARENT-A-TURN40C-COMMUNICATION-RECOVERY-CONFIRMED-20260719-1722`.
- Next A STATUS EVENT must ACK `1722`. Owner, plan-contract block, 7-path WIP, and stop-Java/stop-Maven state do not
  change.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-RECOVERY-FOLLOWUP STALE-1716-ACKED ONLY-RECOVERY-1722-UNACKED COMMUNICATION-RECOVERED OWNER-A-FROZEN-WIP PLAN-CONTRACT-BLOCKED 2026-07-19T17:23:00-04:00 -->

## PARENT COMMUNICATION HANDSHAKE CLOSED - 2026-07-19T17:27:00-04:00

- External A's 18:22 STATUS EVENT ACKed terminal recovery id
  `PARENT-A-TURN40C-COMMUNICATION-RECOVERY-CONFIRMED-20260719-1722`. All directed parent messages are confirmed;
  communication is terminal recovered.
- No substantive card change: A retains ownership, 7-path WIP remains frozen, Java/Maven remain stopped, and the
  plan-contract block waits for the complete baseline-696 transitive write set and acceptance contract.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-HANDSHAKE-CLOSED ACK-RECOVERY-1722 ALL-PARENT-MESSAGES-CONFIRMED COMMUNICATION-RECOVERED-TERMINAL OWNER-A-FROZEN-WIP PLAN-CONTRACT-BLOCKED 2026-07-19T17:27:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR - 2026-07-19T17:57:00-04:00 - READY TO RESUME / OWNER A RETAINED

- Parent completed the baseline-`696a12b0` transitive source/constructor/authority audit. This is a mechanical
  Spring/runtime assembly gap, not a business-semantic choice. The A/B/C question is retired; no user decision is
  required. External A remains the sole owner and may resume Java/Maven only inside the repaired write set below.
- The original seven paths remain writable. The complete additional writable paths are exactly:
  1. `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactory.java`
  3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
  4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java`
  5. CREATE `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAssembly.java`
  6. `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactoryAllowlistTest.java`
  7. `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java`
  No other source/test/resource/build/protocol/store/task-business path is writable.
- Frozen closure mechanics:
  1. Mark the existing nine-argument `PlayerStateService` production constructor as Spring-selected; preserve its
     package-private OCR test seam and every business method unchanged.
  2. `CloudTurnRuntimeConfiguration` explicitly registers/imports the real existing
     `AutomationMetricsService`, `TaskStepExecutor`, and singleton `CloudWholeTaskReadyEventState`; it defines a
     prototype `TaskStartupCheckService` only from `TaskExecutionContextHolder.current()` via
     `baselineNoOverride(exactContext)`, failing closed when no exact context is bound.
  3. The factory owns a fixed four-code descriptor (`wuhuan_v2/五环`, `wubei/五倍`, `xiuluo_v2/修罗`,
     `auto_battle/自动战斗`). Runtime builds each queue element's existing 16-field exact context from that descriptor,
     binds it before provider construction, materializes every real prototype before install/worker/ack, verifies the
     constructed task code/name match the descriptor, and reuses that same context for execution. No default context,
     delayed post-ack construction, or copied task algorithm is allowed.
  4. `CloudTaskTurnAssembly` is the public Spring assembly boundary for the existing package-private
     `CloudTaskTurnAuthority`. One singleton authority is shared by all prototype handles; every task receives a fresh
     handle whose current context comes from the already-bound `TaskExecutionContextHolder`. Preserve the existing
     slot-backed handle path and all fairness/capacity/reentry/release semantics; do not create a second authority,
     lock, lane map, protocol, state store, retry, polling loop, TTL, or takeover path.
- Acceptance must prove host refresh; four real prototypes construct under their exact per-element contexts before
  ack but do not execute at startup; unbound startup-gate/coordination construction fails closed; descriptor/task
  identity mismatch rejects without install/worker/ack; handles are distinct while sharing the sole authority; invalid
  16-field authority still rejects; runtime/host close and previous duplicate/conflict/queue/stop semantics remain.
  Run only the authorized family: `CloudTurnActivationContractTest`, `CloudTurnTaskFactoryAllowlistTest`,
  `CloudTurnTaskRuntimeContractTest`, `CloudWholeTaskFoundationContractTest`, plus Cloud compile/test-compile.
- Delivery remains one canonical whole-card `SOURCE+TEST DELIVERED` block covering all fourteen paths and exact
  command/exit/test-count/SHA/line/mtime/collision evidence. `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C PARENT-PLAN-CONTRACT-REPAIRED READY-TO-RESUME OWNER-A-RETAINED COMPLETE-14-PATH-WRITESET ORIGINAL-7+ADDITIONAL-7 EXACT-CONTEXT-BEFORE-PROTOTYPE BEFORE-ACK STARTUP-GATE-EXACT SOLE-TURN-AUTHORITY NO-USER-CHOICE NO-BUSINESS-DIFF TEST-FAMILY-FROZEN 2026-07-19T17:57:00-04:00 -->

## PARENT RESUME OBSERVATION - 2026-07-19T18:07:00-04:00 - EXTERNAL A ACTIVE

- External A's 19:02 STATUS EVENT ACKed `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-20260719-1757`, read the repaired
  physical EOF, and accurately restated the fourteen-path write set, closure mechanics, prohibited alternatives and
  authorized test family. Communication is current; status is `RESUMING_IMPLEMENTATION / IMPLEMENTING_WHOLE_CARD`.
- The seven newly admitted paths remain at their pre-resume source baselines at this observation:
  `PlayerStateService=C238DA2A`, `CloudTurnTaskFactory=B7B50A5F`, `CloudTurnTaskRuntime=DF5AA6FB`,
  `CloudTaskTurnAuthority=E43E0871`, `CloudTaskTurnAssembly=ABSENT`, factory test=`3052DD40`, runtime test=`12696C4C`.
  A reports reconstruction/read-only work first; no new Java byte is yet attributed to the resumed batch.
- Parent runs no Maven while A is the active Cloud Java writer. Ownership, baseline-696 equivalence, no-business-
  difference rule and canonical whole-card delivery gate remain unchanged.

<!-- TRUE_EOF: TURN-40C PARENT-RESUME-OBSERVATION EXTERNAL-A-ACTIVE ACK-REPAIRED-1757 IMPLEMENTING-WHOLE-CARD 14-PATH-RECON SEVEN-ADDITIONAL-BASELINES-UNCHANGED ASSEMBLY-ABSENT NO-MAVEN-ACTIVE-WRITER BASELINE-696 NO-BUSINESS-DIFF 2026-07-19T18:07:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T18:17:00-04:00 - FIRST REPAIR-BATCH INCREMENT

- External A remains the canonical sole owner and has moved from reconstruction into source implementation.
  `PlayerStateService.java` is now `1E932914`/1443L: the existing nine-argument production constructor alone is
  Spring-selected with `@Autowired`; the package-private OCR seam and business methods remain unchanged.
- `CloudTurnRuntimeConfiguration.java` is now `64A54422`/138L. It imports the real existing
  `AutomationMetricsService`, `TaskStepExecutor` and singleton `CloudWholeTaskReadyEventState`, and defines a
  prototype `TaskStartupCheckService` from `TaskExecutionContextHolder.current()` via
  `baselineNoOverride(exactContext)`, failing closed when unbound. This matches repaired mechanics 1 and 2.
- At this observation the other six additional paths remain at `B7B50A5F`/`DF5AA6FB`/`E43E0871`/`ABSENT`/
  `3052DD40`/`12696C4C`; runtime configuration is one of the original seven paths, not an additional path. This is
  active source progress, not canonical whole-card delivery or parent source
  review. Parent ran no Maven while A is active; baseline-696, sole-authority and no-business-difference gates stand.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER FIRST-REPAIR-BATCH-INCREMENT PLAYERSTATE=1E932914-1443L ORIGINAL-CONFIG=64A54422-138L MECHANIC1+2-IN-SCOPE OTHER-SIX-ADDITIONAL-PATHS-UNCHANGED ASSEMBLY-ABSENT NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-696 SOLE-AUTHORITY NO-BUSINESS-DIFF 2026-07-19T18:17:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T18:27:00-04:00 - MECHANICS 1, 2 AND 4 LANDED

- External A remains canonical sole owner. Physical source now shows `CloudTaskTurnAuthority=C651BD8D`/1241L,
  CREATE `CloudTaskTurnAssembly=69A51B55`/37L and `CloudTurnRuntimeConfiguration=E59C20B8`/155L. The authority
  preserves the existing `CloudTaskRunCurrentContextSlot` handle adapter while adding a holder-backed fail-closed
  context source; the public assembly owns exactly one authority and mints fresh handles; the prototype coordination
  bean consumes that assembly. No second authority, lock, lane, protocol or store is present in these paths.
- `CloudTurnTaskFactory=3B511EE8`/109L has begun mechanic 3 with the fixed four task code/name descriptors. Runtime
  remains `DF5AA6FB`; factory/runtime tests remain `3052DD40`/`12696C4C`. Across the repair batch five files now
  differ, of which four are in the seven additional paths.
- This remains active intermediate source, not canonical whole-card delivery or parent source review. Parent ran no
  Maven while A is active. Baseline-696, exact-context-before-prototype, sole-authority and no-business-difference
  acceptance gates remain unchanged.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER MECHANICS-1+2+4-LANDED AUTHORITY=C651BD8D-1241L ASSEMBLY=69A51B55-37L CONFIG=E59C20B8-155L FACTORY=3B511EE8-109L RUNTIME=DF5AA6FB TESTS=3052DD40+12696C4C REPAIR-BATCH-5-FILES ADDITIONAL-PATHS-4OF7 NOT-DELIVERY NOT-REVIEW ACTIVE-JAVA-WRITER NO-MAVEN BASELINE-696 SOLE-AUTHORITY NO-BUSINESS-DIFF 2026-07-19T18:27:00-04:00 -->

## PARENT BUILD PROGRESS OBSERVATION - 2026-07-19T18:32:00-04:00 - PRODUCTION MECHANICS COMPLETE

- External A remains canonical sole owner. `CloudTurnTaskFactory=3B511EE8`/109L and
  `CloudTurnTaskRuntime=8368ED7E`/437L complete mechanic 3. Parent source inspection confirms the runtime obtains the
  fixed descriptor, builds the existing exact 16-field context, wraps real provider construction in
  `TaskExecutionContextHolder.callWith(context)`, rejects construction/identity mismatch before install/worker/ack,
  and stores/reuses that same context for execution.
- Together with PlayerState/config/authority/assembly, the repair batch now has six changed production/config paths;
  five of the seven additional paths have changed. The factory/runtime tests remain `3052DD40`/`12696C4C` and are
  the only additional paths not yet updated.
- A reports authorized Cloud `mvn -q -o compile` exit 0. Parent did not duplicate Maven while A remains active. This
  is a build-progress observation, not canonical whole-card delivery or parent source review; test-compile and the
  frozen named family remain pending. Baseline-696, sole-authority and no-business-difference gates stand.

<!-- TRUE_EOF: TURN-40C PARENT-BUILD-PROGRESS EXTERNAL-A-SOLE-OWNER ALL-PRODUCTION-MECHANICS-CODED FACTORY=3B511EE8-109L RUNTIME=8368ED7E-437L REPAIR-BATCH-6-PROD-CONFIG ADDITIONAL-PATHS-5OF7 TESTS=3052DD40+12696C4C MAIN-COMPILE-EXIT0-PER-A PARENT-NO-DUPLICATE-MAVEN TEST-COMPILE+NAMED+DELIVERY-PENDING NOT-REVIEW BASELINE-696 SOLE-AUTHORITY NO-BUSINESS-DIFF 2026-07-19T18:32:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T18:34:00-04:00 - RUNTIME TEST ACTIVE WIP

- During the active test-update batch, `CloudTurnTaskRuntimeContractTest.java` advanced to `1D9D32D3`/825L,
  mtime `2026-07-19 18:34:38 -04:00`. `CloudTurnTaskFactoryAllowlistTest` remains `3052DD40`; the activation test
  remains `E6BBBD82`. Additional-path progress is therefore six of seven.
- This is intermediate source WIP, not test-compile/named-family evidence, canonical delivery or parent review.
  Parent ran no Maven while A continues writing.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS EXTERNAL-A-SOLE-OWNER RUNTIME-CONTRACT-TEST-WIP=1D9D32D3-825L ADDITIONAL-PATHS-6OF7 FACTORY-TEST=3052DD40 ACTIVATION-TEST=E6BBBD82 TEST-GATES+DELIVERY-PENDING NOT-REVIEW ACTIVE-JAVA-WRITER PARENT-NO-MAVEN 2026-07-19T18:34:00-04:00 -->

---

## EXTERNAL-A WRITE-SET-EXTENSION REQUEST - 2026-07-19T19:37:00-04:00 - one file short of full host refresh

**Owner:** EXTERNAL-A. **State:** 14-path closure implemented + compiling + reviewed runtime test green; host refresh now blocked ONLY by one out-of-write-set multi-constructor bean. Requesting the parent add exactly one path (or authorise the one-line change).

### Progress on the frozen 14-path write set (all implemented, `mvn -q -o compile` + `test-compile` EXIT0)
- **Mechanic 1** `PlayerStateService`: `@Autowired` on the 9-arg production ctor (OCR seam + business methods unchanged). **Resolves** — the graph now instantiates it.
- **Mechanic 2** `CloudTurnRuntimeConfiguration`: `@Import` real `AutomationMetricsService`/`TaskStepExecutor`/`CloudWholeTaskReadyEventState`; prototype `TaskStartupCheckService` from `holder.current()` via `baselineNoOverride`, fail-closed.
- **Mechanic 3** `CloudTurnTaskFactory` fixed four-code descriptor (`wuhuan_v2/五环`,`wubei/五倍`,`xiuluo_v2/修罗`,`auto_battle/自动战斗`, verified == real `TASK_CODE`/`TASK_NAME`); `CloudTurnTaskRuntime.start()` builds each element's exact context from the descriptor and **binds it before provider construction**, materializes every prototype before ack/install/worker, verifies constructed code/name == descriptor, reuses the same context for execution.
- **Mechanic 4** `CloudTaskTurnAuthority` context-source seam (kept `createHandle(slot)` adapter so the foundation test is untouched; added holder-backed `createHandle(holder)`); CREATE `CloudTaskTurnAssembly` (public, one shared authority); config prototype `CloudTaskTurnCoordination` bean.
- Also removed the config's duplicate `cloudTurnRuntimeGameClient` `@Bean`: the scanned `turnGameClient @Component` is the single `TurnGameClient`; a second definition made it ambiguous for the scanned services (`cloudBagLocalServiceClient`) and for `getService(TurnGameClient.class)`.
- **`CloudTurnTaskRuntimeContractTest` (reviewed) passes green** (updated the four `Recording*` `getTaskName` to the descriptor names so the new identity verification holds).

### The one remaining gap (out of write set)
With PlayerStateService fixed, host refresh now reaches, via `autoCombatService → playerStateService → CloudBagUseIncensePort → cloudBagLocalServiceClient → turnGameClient`, this error:
`Error creating bean 'turnGameClient' [.../turn/client/TurnGameClient.class]: No default constructor found`.
`src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java` is `@Component` in the scanned `turn.client` package and has **three constructors** (3-arg public production, 4-arg package test seam, 5-arg private) with **no `@Autowired`**, so Spring cannot select one — the identical pattern as `PlayerStateService`. A read-only scan of the whole `turn.client` scanned package finds **`TurnGameClient` is the only** multi-constructor `@Component`/`@Service` without a selected ctor (`CloudBagLocalServiceClient` etc. are single-ctor). The parent's transitive-closure audit characterised "the only scanned, non-excluded multi-constructor service" within `com.bot.dhxy.service` (PlayerStateService); the `com.yueyunfe.dhxy.cloudbrain.turn.client` scan was not covered, so `TurnGameClient` is the missed peer.

### Request
Add **`src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`** to the write set for exactly the same one-line treatment: `@Autowired` on the 3-arg public production constructor (test seam + `bind()` + business methods unchanged; no second bean, no scan narrowing, no stub/default/null). This is not in my current write set; I will not edit it until authorised. Once added, I will apply it, re-run host refresh to confirm no further closure gap, and complete the canonical delivery. Holding; no `TurnGameClient` edit made.

<!-- TRUE_EOF: TURN-40C EXTERNAL-A WRITESET-EXTENSION-REQUEST TURNGAMECLIENT-AUTOWIRED-NEEDED 14PATH-IMPLEMENTED-COMPILING RUNTIME-TEST-GREEN ONLY-TURN-CLIENT-MULTI-CTOR HOLD-NO-EDIT 2026-07-19T19:37:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #2 - 2026-07-19T18:48:00-04:00 - READY TO RESUME AFTER ACK

- Parent accepts the extension request after an independent full scan-closure audit. `CloudServiceConfiguration`
  scans `com.bot.dhxy.service` and `com.yueyunfe.dhxy.cloudbrain.turn.client`. Across both roots, every other explicit
  multi-constructor scanned bean either already has a Spring-selected constructor or is explicitly excluded and
  supplied by `@Bean`; `TurnGameClient` is the sole remaining unselected scanned multi-constructor bean.
- The canonical write set is repaired from fourteen to exactly fifteen paths by adding:
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`.
  The only authorized edit in that path is importing Spring `@Autowired` and marking the existing public three-arg
  production constructor. Preserve the package-private four-arg UUID test seam, private five-arg constructor,
  `bind()`, every business method, the scanned component identity, and the single-bean topology. No duplicate bean,
  scan narrowing, stub/default/null, second protocol/store/client/authority, retry, TTL or business change.
- Current pre-repair evidence is `TurnGameClient=AFA5EC42`/216L; factory test=`FEFB6DC2`/223L and runtime test=
  `DB3A486A`/840L. A reports Cloud compile and test-compile `EXIT 0`; these are progress evidence, not delivery or
  parent review. After ACK, rerun host refresh to closure, the frozen named family and applicable compile/test-compile,
  then deliver one canonical `SOURCE+TEST DELIVERED` block covering all fifteen paths.
- External A retains sole ownership. Resume only after a STATUS EVENT ACKs
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R2-20260719-1848`. `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C PARENT-PLAN-CONTRACT-REPAIRED-R2 READY-AFTER-ACK OWNER-A-RETAINED COMPLETE-15-PATH-WRITESET ADD-TURNGAMECLIENT-ONLY PROD-3ARG-AUTOWIRED PRESERVE-TEST-SEAM+BIND+BUSINESS SINGLE-BEAN FULL-SCAN-CLOSURE NO-USER-CHOICE NO-BUSINESS-DIFF ACK=PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R2-20260719-1848 2026-07-19T18:48:00-04:00 -->

## PARENT BUILD-COLLISION AUDIT - 2026-07-19T18:53:00-04:00

- Parent observed the raced 19:48 A status after publishing Repair #2. The reported foundation result is
  `30 run / 23 pass / 7 error`. Source inspection confirms every error occurs in the retained test fixture
  `laneOwnerHandle()` while constructing `RemoteTaskRunWindow(windowId, "hwnd-" + windowId, ...)`: the concurrent
  untracked `remote/run` implementation now requires normalized unsigned-decimal `nativeHandle` text.
- That exception is raised before `CloudTaskTurnAuthority.createHandle(...)`; it does not execute or falsify the
  TURN-40C context-source seam. Treat it as an out-of-card concurrent-source/build collision. Do not edit the
  foundation fixture or untracked `remote/run` cohort under TURN-40C, and do not expand the repaired 15-path write
  set. Canonical delivery must report this build gate separately and preserve exact command/count/evidence.
- The preceding directed Repair #2 message raced this A event and remains pending ACK. Ownership and the
  `TurnGameClient` one-line authorization are unchanged.

<!-- TRUE_EOF: TURN-40C PARENT-BUILD-COLLISION-AUDIT FOUNDATION=30-RUN-23-PASS-7-ERROR OUT-OF-CARD-UNTRACKED-REMOTE-RUN DECIMAL-HWND-FIXTURE-CONFLICT BEFORE-TURN40C-SEAM DO-NOT-EXPAND-15-PATH DO-NOT-EDIT-FOUNDATION+REMOTE-RUN REPAIR-R2-ACK-STILL-PENDING 2026-07-19T18:53:00-04:00 -->

## PARENT SOURCE OBSERVATION - 2026-07-19T18:55:00-04:00 - FIFTEENTH PATH LANDED / ACK PENDING R1

- Physical source changed after Repair #2: `TurnGameClient.java` is now `1B203987`/221L, mtime
  `2026-07-19 18:54:43 -04:00`. Inspection confirms Spring `@Autowired` selects the existing public three-argument
  production constructor; the package-private UUID seam, private constructor, `bind()` and business methods remain.
- No A STATUS EVENT has ACKed `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R2-20260719-1848`. This is missed ACK
  round one, not yet `COMMUNICATION_STALE`. A must ACK the original id in its next event before reporting the host
  refresh/named-family/build result or canonical delivery. Parent runs no Maven while source/build activity continues.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-OBSERVATION FIFTEENTH-PATH-LANDED TURNGAMECLIENT=1B203987-221L PROD-CTOR-AUTOWIRED PRESERVE-SEAM+PRIVATE-CTOR+BIND+BUSINESS ACK-PENDING-R1 NOT-COMMUNICATION-STALE HOST-REFRESH+NAMED+BUILD+DELIVERY-PENDING PARENT-NO-MAVEN 2026-07-19T18:55:00-04:00 -->

## PARENT COMMUNICATION AUDIT - 2026-07-19T18:59:00-04:00 - COMMUNICATION_STALE

- External A has now missed two consecutive audit rounds without a STATUS EVENT ACK of the Repair #2 message and
  R1 reminder. Mark `COMMUNICATION_STALE`; do not release or transfer canonical ownership. Preserve the complete
  15-path WIP and `TurnGameClient=1B203987`/221L.
- The next A STATUS EVENT must ACK all three ids:
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R2-20260719-1848`,
  `PARENT-A-TURN40C-REPAIR-R2-ACK-REMINDER-R1-20260719-1855`, and
  `PARENT-A-TURN40C-COMMUNICATION-STALE-20260719-1859`, then report exact host-refresh, authorized-family,
  compile/test-compile and delivery state. Recent source change is under ten minutes old, so `ACTIVE_STALE` does not
  apply. Parent runs no Maven while Worker activity is unresolved.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-AUDIT COMMUNICATION-STALE TWO-ROUNDS-NO-ACK OWNER-A-RETAINED PRESERVE-15-PATH-WIP TURNGAMECLIENT=1B203987 ACK-1848+1855+1859-NEXT NOT-ACTIVE-STALE NO-DELIVERY PARENT-NO-MAVEN 2026-07-19T18:59:00-04:00 -->

## PARENT PLAN-CONTRACT AUDIT - 2026-07-19T19:04:00-04:00 - BLOCKED / FREEZE CURRENT WIP

- A's 20:03 STATUS EVENT ACKs the core Repair #2 id `...1848`; communication moves from stale to recovering, while
  the raced R1 reminder `...1855` and stale id `...1859` still require explicit ACK. `TurnGameClient` constructor
  selection worked and the host graph passed that bean.
- The same event reports iterative additions of genuine dialog/UI remote-port components to the writable runtime
  configuration, now `15E6F1E7`/159L, followed by the next missing non-scanned collaborator
  `com.bot.dhxy.vision.OcrRoiMemoryService`. This is evidence that the repaired contract still did not freeze the
  complete transitive Spring constructor graph. Iterating one missing bean at a time is not an acceptable closure.
- Set `PLAN-CONTRACT BLOCKED / PARENT FULL IMPORT-CLOSURE AUDIT`. External A retains ownership and every current WIP
  byte, but must stop adding imports and stop Java/Maven until the parent freezes the complete existing-bean/import
  set for all four real prototypes. No stub, default/null, broader component scan, second protocol/store/authority,
  copied algorithm, or baseline-696 behavior difference is authorized.

<!-- TRUE_EOF: TURN-40C PARENT-PLAN-CONTRACT-AUDIT BLOCKED FULL-IMPORT-CLOSURE-REQUIRED NOT-ITERATIVE-FIRST-MISSING CONFIG=15E6F1E7-159L NEXT-MISSING=OCRROIMEMORYSERVICE CORE-R2-ACKED COMMUNICATION-RECOVERING R1+STALE-ACK-PENDING OWNER-A-WIP-RETAINED STOP-IMPORTS+JAVA+MAVEN NO-USER-CHOICE NO-BUSINESS-DIFF 2026-07-19T19:04:00-04:00 -->

## PARENT COMMUNICATION RECOVERY - 2026-07-19T19:05:00-04:00

- A's 20:08 STATUS EVENT ACKs `1848`, `1855`, and `1859`; clear `COMMUNICATION_STALE`. A also paused further
  cross-package imports, consistent with the parent's full-closure block. The R3 `1904` block raced that event and
  remains the only unconfirmed directed message.
- The reported scale question is not delegated to the user. Parent will classify the four real task dependency graph
  against extra eager beans caused by the broad service scan, then freeze one baseline-equivalent assembly contract.
  Ownership/config/WIP remain; Java/Maven stay stopped and no delivery/review exists.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-RECOVERY ACK-1848+1855+1859 COMMUNICATION-STALE-CLEARED IMPORTS-PAUSED R3-1904-ACK-PENDING PLAN-CONTRACT-BLOCKED FULL-GRAPH-CLASSIFICATION NO-USER-CHOICE OWNER-A-WIP-RETAINED STOP-JAVA+MAVEN 2026-07-19T19:05:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #4 - 2026-07-19T19:18:00-04:00 - COMPLETE GRAPH / READY AFTER ACK

- A's 20:13 STATUS EVENT ACKed R3 and preserved `CloudTurnRuntimeConfiguration=15E6F1E7`; communication is current.
  Parent completed the requested full constructor-DAG audit over the actual Cloud copies, both component-scan roots,
  all four real task prototypes, imported configurations, `@Bean` products and host-registered capabilities.
- Keep the six genuine dialog/UI remote imports already in the configuration. The complete remaining non-scanned
  bean closure is exactly `com.bot.dhxy.vision.OcrRoiMemoryService`; add that existing class to the configuration's
  `@Import`. Its public no-arg production constructor preserves the real `config/vision_memory.json` store behavior.
  Do not edit the vision source path and do not replace it with a stub/default/null or broader scan.
- `NavigationService` also requires `DecisionEngine`. Reuse the exact instance already created by
  `CloudBrainServer` for the legacy decision/outcome routes: add a 40C host-create overload that accepts non-null
  `DecisionEngine`, registers that instance in the same `AnnotationConfigApplicationContext` before refresh, and
  have Server plus `CloudTurnActivationContractTest` pass it. Preserve old host overloads/callers. Do not modify
  `DecisionEngine`, construct a second instance, lose `routeClickOverride`, copy its algorithm or add another route.
- This repair adds zero writable paths: configuration, host, server and activation test are already in the canonical
  fifteen. All other scanned/imported mandatory collaborators resolve from the existing scan, host registrations,
  `CloudServiceConfiguration` beans/imports, turn-client scan or current genuine remote imports. No other Java path,
  component scan, protocol, state store, authority, retry, TTL or business decision is authorized.
- External A retains sole ownership. Resume only after ACKing
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R4-20260719-1918`; then run host refresh to full closure, the frozen named
  family and compile/test-compile, and deliver one canonical 15-path `SOURCE+TEST DELIVERED`. Preserve the separately
  classified foundation `30 run / 23 pass / 7 error` card-external decimal-HWND fixture collision.
  `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C PARENT-PLAN-CONTRACT-REPAIRED-R4 COMPLETE-CONSTRUCTOR-DAG READY-AFTER-ACK OWNER-A-RETAINED COMMUNICATION-CURRENT KEEP-DIALOG+UI-IMPORTS ADD-OCRROIMEMORY-IMPORT SAME-DECISIONENGINE-INSTANCE-REGISTERED-IN-HOST PRESERVE-ROUTECLICKOVERRIDE ZERO-NEW-PATHS STILL-15-PATH NO-SCAN-WIDEN NO-SECOND-ALGORITHM NO-BUSINESS-DIFF FOUNDATION-COLLISION-SEPARATE ACK=PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R4-20260719-1918 2026-07-19T19:18:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #5 - 2026-07-19T19:24:00-04:00 - SCOPED OCR MEMORY / R4 SUPERSEDED

- R5 supersedes R4 before A ACK or implementation. Directly importing `OcrRoiMemoryService` would select its public
  no-arg constructor and bind both OCR-memory files to process-global `config/`, outside the configured tenant/user
  host scope. That would violate the existing `CloudServiceStorage` isolation and is not authorized.
- In writable `CloudTurnRuntimeConfiguration`, declare one singleton `OcrRoiMemoryService` bean constructed through
  the real public `OcrRoiMemoryService(Path)` constructor. Its directory must be exactly
  `cloudServiceStorage.resolvePrivateFile("vision_memory.json").getParent()`, so both `vision_memory.json` and the
  legacy fixed-name file remain under the one existing hashed tenant/user scope root. Do not also import the class;
  do not add another path/store, modify its source, use global `config/`, or change memory algorithms/fallbacks.
- The R4 same-instance `DecisionEngine` contract remains unchanged: pass Server's already-created route engine into
  the 40C host-create overload and register that exact non-null object before refresh, preserving old overloads and
  `routeClickOverride`. The canonical write set remains fifteen paths with zero expansion.
- External A must ACK only the superseding id
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R5-20260719-1924` before resuming. R4 must not be implemented. All test,
  delivery, collision and no-business-difference gates remain unchanged. `无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C PARENT-PLAN-CONTRACT-REPAIRED-R5 SUPERSEDES-R4 READY-AFTER-ACK OWNER-A-RETAINED OCR-MEMORY-BEAN=REAL-PATH-CTOR CLOUDSERVICESTORAGE-SCOPE-ROOT NO-DIRECT-IMPORT NO-GLOBAL-CONFIG NO-SECOND-STORE SAME-SERVER-DECISIONENGINE-HOST-REGISTRATION UNCHANGED STILL-15-PATH NO-BUSINESS-DIFF ACK=PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R5-20260719-1924 2026-07-19T19:24:00-04:00 -->

## PARENT SOURCE OBSERVATION - 2026-07-19T19:26:00-04:00 - R5 SCOPED BEAN LANDED / ACK PENDING R1

- External A remains the sole owner. `CloudTurnRuntimeConfiguration.java` changed after R5 to
  `4E91D53E`/173 physical lines, mtime `2026-07-19 19:25:31.483 -04:00`.
- Parent reviewed the complete file: it imports the type only for the method signature, does not include
  `OcrRoiMemoryService` in `@Import`, and declares one bean using the exact real constructor and directory
  `storage.resolvePrivateFile("vision_memory.json").getParent()`. This satisfies R5's tenant/user scope-root,
  no-global-`config/`, no-second-store contract. No parent Maven was run.
- The ledger still has no STATUS EVENT ACK of
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R5-20260719-1924`. This is the first missed audit round, so communication
  remains current and is not marked stale. The same-Server-`DecisionEngine` host registration, host refresh, frozen
  named family, compile/test-compile and canonical 15-path delivery remain pending.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-OBSERVATION R5-SCOPED-BEAN-LANDED CONFIG=4E91D53E-173L MTIME=20260719-192531483 EXACT-CLOUDSERVICESTORAGE-SCOPE-ROOT NO-DIRECT-IMPORT NO-GLOBAL-CONFIG NO-SECOND-STORE ACK-R5-PENDING-R1 NOT-COMMUNICATION-STALE OWNER-A-RETAINED DECISIONENGINE+REFRESH+TEST+BUILD+DELIVERY-PENDING PARENT-NO-MAVEN 2026-07-19T19:26:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T19:28:00-04:00 - R5 SAME-ENGINE SEAM LANDED

- The same active batch added Host=`05FB55E9`/136 physical lines, Server=`CA2A1EF4`/196 physical lines and activation
  test=`3922CBAA`/353 physical lines/7 tests. Parent reviewed all three complete files.
- `CloudServiceHost` preserves both old overload families and adds a non-null `DecisionEngine` overload that registers
  the exact caller object before refresh. `CloudBrainServer` creates one route engine with `routeClickOverride`, uses it
  for all legacy decision/outcome endpoints and passes that same object to the host. The activation test calls the new
  seam. No second engine/store, broader scan, source-path expansion or business algorithm copy was found.
- This remains active WIP, not canonical delivery/review. R5 and R1 reminder ACKs, complete host refresh, frozen named
  family, compile/test-compile and delivery evidence remain pending. Parent ran no Maven while Java changed.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS R5-SAME-ENGINE-SEAM-LANDED HOST=05FB55E9-136L SERVER=CA2A1EF4-196L ACTIVATIONTEST=3922CBAA-353L-7T OLD-OVERLOADS-PRESERVED ONE-SERVER-ENGINE-LEGACY+HOST NO-SECOND-ENGINE+STORE NO-SCAN-WIDEN NOT-DELIVERY NOT-REVIEW ACK-R5+R1-PENDING REFRESH+NAMED+BUILD-PENDING PARENT-NO-MAVEN 2026-07-19T19:28:00-04:00 -->

## PARENT COMMUNICATION AUDIT - 2026-07-19T19:30:00-04:00 - COMMUNICATION_STALE / SOURCE ACTIVE

- External A has missed two consecutive audit rounds without a STATUS EVENT ACK of R5
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R5-20260719-1924` and R1
  `PARENT-A-TURN40C-R5-ACK-REMINDER-R1-20260719-1926`. Mark `COMMUNICATION_STALE`; retain A as sole owner and retain
  the complete 15-path WIP.
- Source remains active: activation test advanced to `864BFC9F`/369 physical lines/7 tests, mtime
  `2026-07-19 19:29:23.348 -04:00`; config/Host/Server remain `4E91D53E/05FB55E9/CA2A1EF4`. Because relevant source
  changed within ten minutes, do not mark `ACTIVE_STALE`.
- No canonical delivery/review exists. Refresh, frozen named family, compile/test-compile and delivery remain pending;
  parent runs no Maven while writer activity is current.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-AUDIT COMMUNICATION-STALE TWO-ROUNDS-NO-ACK-R5+R1 OWNER-A-RETAINED PRESERVE-15-PATH-WIP ACTIVATIONTEST=864BFC9F-369L-7T NOT-ACTIVE-STALE NOT-DELIVERY REFRESH+NAMED+BUILD-PENDING PARENT-NO-MAVEN 2026-07-19T19:30:00-04:00 -->

## PARENT COMMUNICATION RECOVERY / SOURCE OBSERVATION - 2026-07-19T19:36:00-04:00

- External A's latest STATUS EVENT explicitly ACKs R5
  `PARENT-A-TURN40C-PLAN-CONTRACT-REPAIRED-R5-20260719-1924` and reports the complete R5 host graph refreshes with
  zero bean errors. Clear `COMMUNICATION_STALE` to `COMMUNICATION_RECOVERING`; R1 `1926` and stale `1930` raced the
  event and remain unconfirmed. A remains sole owner of the unchanged fifteen-path WIP.
- The latest reported activation result is `7 run / 5 pass / 2 fail`, both identified as test-expectation repairs.
  The test source has since advanced to `630DF944`/369 physical lines/7 tests, mtime
  `2026-07-19 19:35:27.600 -04:00`, with `runtime.close()` cleanup and HTTP 400 invalid-authority fail-closed
  assertions present. This is active WIP source evidence, not a repaired test result or canonical delivery.
- Do not mark `ACTIVE_STALE` and do not begin parent source review. Repaired activation rerun, frozen named family,
  compile/test-compile and canonical fifteen-path delivery remain pending. Parent ran no Maven while Java changed.

<!-- TRUE_EOF: TURN-40C PARENT-COMMUNICATION-RECOVERY+SOURCE-OBSERVATION ACK-R5-1924 HOST-GRAPH-REFRESH-CLEAN-0-BEAN-ERROR COMMUNICATION-RECOVERING ACK-R1-1926+STALE-1930-PENDING OWNER-A-RETAINED STILL-15-PATH ACTIVATION-LAST-5OF7 TEST-FIXES-LANDED=630DF944-369L-7T RERUN+NAMED+BUILD+DELIVERY-PENDING NOT-ACTIVE-STALE NOT-REVIEW PARENT-NO-MAVEN 2026-07-19T19:36:00-04:00 -->

## PARENT SOURCE PROGRESS OBSERVATION - 2026-07-19T19:38:00-04:00

- Activation test advanced again to `7B418DF0`/376 physical lines/7 tests, mtime
  `2026-07-19 19:37:31.688 -04:00`. The added source clarifies why no-client worker release is not deterministically
  bounded and keeps `runtime.close()` as cleanup; the HTTP 400 invalid-authority fail-closed assertion remains.
- This is continued WIP after the last reported 5/7 run, not repaired rerun evidence or canonical delivery. A remains
  active sole owner; communication remains recovering until R1/stale/recovery ids are ACKed. Parent ran no Maven.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE-PROGRESS ACTIVATIONTEST=7B418DF0-376L-7T MTIME=20260719-193731688 TEST-EXPECTATION-REPAIR-ACTIVE LAST-RESULT-5OF7 RERUN+NAMED+BUILD+DELIVERY-PENDING OWNER-A-RETAINED COMMUNICATION-RECOVERING NOT-ACTIVE-STALE NOT-DELIVERY NO-MAVEN 2026-07-19T19:38:00-04:00 -->

## PARENT TEST-STATUS OBSERVATION - 2026-07-19T19:40:00-04:00 - ACTIVATION 7/7 PASSED

- A's latest STATUS EVENT reports `mvn -Dtest=CloudTurnActivationContractTest test` `EXIT 0`, `7/7 PASS` against
  `7B418DF0`/376L/7T. The host refresh and focused activation gate are now green.
- A ACKed R5 and R1, but stale `1930` and recovery `1936` remain unconfirmed due tail race; communication remains
  recovering, not stale. Full authorized family, compile/test-compile and canonical fifteen-path delivery remain.
- This is test/build progress, not canonical delivery or parent source review. A retains owner; parent ran no Maven.

<!-- TRUE_EOF: TURN-40C PARENT-TEST-STATUS ACTIVATION=7OF7-PASS-EXIT0 TEST=7B418DF0-376L-7T HOST-REFRESH-CLEAN ACK-R5+R1 ACK-STALE1930+RECOVERY1936-PENDING COMMUNICATION-RECOVERING OWNER-A-RETAINED FULL-FAMILY+COMPILE+TESTCOMPILE+DELIVERY-PENDING NOT-REVIEW PARENT-NO-MAVEN 2026-07-19T19:40:00-04:00 -->

---

## EXTERNAL-A CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-19T20:38:00-04:00 - 15 PATHS

**Owner:** EXTERNAL-A (sole; card line 75). Baseline `696a12b0`; no approved business difference; baseline-equivalent migration. R5-`1924` in effect (R4 not implemented). Zero Git mutation.

### Write set — exactly fifteen paths (SHA-256 first 8 / lines / mtime -04:00)
Original seven:
1. MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainApplication.java` `5711BC3E`/112L/16:25:24 — explicit tenant/user/stateRoot fail-closed CLI; builds scope + calls 5-arg server start.
2. MODIFY `.../cloudbrain/CloudBrainServer.java` `CA2A1EF4`/196L/19:27:05 — 5-arg `start(...,scope,stateRoot)`; builds host with `CloudTurnRuntimeConfiguration` + passes the same `DecisionEngine`; activation wiring; fixed close order runtime→host→server→executor; partial-start reverse cleanup.
3. MODIFY `.../cloudbrain/turn/CloudTurnHttpHandler.java` `01DE94A2`/399L/16:36:58 — `TurnActivation` seam; routes a `taskStartRequest` turn to the runtime under the bound exact context, returns the matching typed ack or fails closed.
4. MODIFY `.../cloudbrain/turn/CloudTurnRoutes.java` `063DE4FC`/94L/16:38:21 — 4-arg activation overload threading `TurnActivation`; 3-arg delegates null (existing callers intact).
5. MODIFY `.../cloudbrain/host/CloudServiceHost.java` `05FB55E9`/136L/19:26:51 — added `DecisionEngine` create overload registering the exact instance before refresh (private `createInternal`); old overloads/callers preserved.
6. CREATE `.../cloudbrain/host/CloudTurnRuntimeConfiguration.java` `4E91D53E`/173L/19:25:31 — wires reviewed 40B runtime/factory/control beans; imports real `AutomationMetricsService`/`TaskStepExecutor`/`CloudWholeTaskReadyEventState` + six genuine dialog/UI remote ports; prototype `TaskStartupCheckService` (fail-closed) + prototype `CloudTaskTurnCoordination`; scope-rooted `OcrRoiMemoryService` `@Bean` via its `Path` ctor.
7. CREATE `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnActivationContractTest.java` `7B418DF0`/376L/19:37:31 — 7 tests (host refresh, one scope + reused capabilities + reviewed beans, dormancy, valid task-start→matching ack, invalid-authority 400 fail-closed, runtime/host close order, explicit-identity fail-closed).

Additional eight (parent Repair R2/R4/R5):
8. MODIFY `src/main/java/com/bot/dhxy/service/PlayerStateService.java` `1E932914`/1443L/18:14:22 — `@Autowired` on the 9-arg production ctor (OCR test seam + business methods unchanged).
9. MODIFY `.../turn/runtime/CloudTurnTaskFactory.java` `3B511EE8`/109L/18:27:56 — fixed four-code `Descriptor` (`wuhuan_v2/五环`,`wubei/五倍`,`xiuluo_v2/修罗`,`auto_battle/自动战斗`) + `descriptor(code)`.
10. MODIFY `.../turn/runtime/CloudTurnTaskRuntime.java` `8368ED7E`/437L/18:30:01 — `start()` builds each element's exact context from the descriptor and binds it BEFORE prototype construction, materializes all prototypes before ack/install/worker, verifies constructed code/name == descriptor, reuses that context for execution.
11. MODIFY `.../remote/CloudTaskTurnAuthority.java` `C651BD8D`/1241L/18:23:54 — `CloudTaskTurnContextSource` seam; kept `createHandle(slot)` adapter (foundation path unchanged) + added holder-backed `createHandle(holder)`; 4 read sites repointed. No second authority/lock/lane/store.
12. CREATE `.../remote/CloudTaskTurnAssembly.java` `69A51B55`/37L/18:24:15 — public boundary owning one shared authority; fresh holder-sourced coordination handle per task.
13. MODIFY `src/test/java/.../turn/runtime/CloudTurnTaskFactoryAllowlistTest.java` `FEFB6DC2`/223L/18:44:56 — added descriptor identity coverage (3 tests).
14. MODIFY `src/test/java/.../turn/runtime/CloudTurnTaskRuntimeContractTest.java` `DB3A486A`/840L/18:47:03 — `Recording*` names aligned to descriptors + identity-mismatch acceptance test (24 tests).
15. MODIFY `.../turn/client/TurnGameClient.java` `1B203987`/221L/18:54:43 — `@Autowired` on the 3-arg production ctor (UUID seam / package test ctor / `bind()` / business unchanged).

### Named-family evidence (PowerShell mvn, offline, sole Cloud writer)
- `mvn -o -DskipTests=false -Dtest=CloudTurnActivationContractTest,CloudTurnTaskFactoryAllowlistTest,CloudTurnTaskRuntimeContractTest test` → **BUILD SUCCESS, exit 0**; per-class `Tests run: 7/3/24, Failures 0, Errors 0`; aggregate **34 run / 0 fail / 0 error**.
- `mvn -q -o compile` → **exit 0** (all 15 production files + graph compile). Test-compile passed within the family run (whole test tree).
- Host graph now fully refreshes under the one configured scope with **zero bean errors** (the R5 closure).

### Separate out-of-card build gate (parent-classified, not a TURN-40C defect)
- `mvn -o -DskipTests=false -Dtest=CloudWholeTaskFoundationContractTest test` → **30 run / 23 pass / 7 error** (stable). The 7 errors are the untracked out-of-card `remote/run` cohort: `RemoteTaskRunValidation` now requires a normalized unsigned-decimal `nativeHandle`, incompatible with the retained fixture `laneOwnerHandle()` building `RemoteTaskRunWindow(windowId, "hwnd-"+windowId, ...)` — the exception is raised at `RemoteTaskRunWindow.<init>`, before `CloudTaskTurnAuthority.createHandle`, so the TURN-40C context-source seam is never reached. `RemoteTaskRunValidation.java`/`RemoteTaskRunWindow.java` are `??` untracked and out of the 15-path write set. One additional freshness-window timing test flaked once and passed on immediate re-run (30/23/7, 0 failures). Not editing the fixture or `remote/run` cohort under TURN-40C, per parent audit.

### Collision audit
Exactly the fifteen write-set paths changed by A (App/Server tracked-modified; the remaining thirteen are edits within the concurrently-untracked in-flight cloud tree, all inside the authorized fifteen). No other Java/source/scan/protocol/store/build path modified. The out-of-write-set `DecisionEngine` (package-private final) is reached by reflection in the activation test, not edited or subclassed. Zero Git mutation; no runtime/server/UI/capture/input launched; `D:\mavenProject\DHXY` frozen untouched.

`无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C EXTERNAL-A CANONICAL-SOURCE+TEST-DELIVERED 15-PATH NAMED-FAMILY-34-0-0 COMPILE-0 HOST-REFRESH-CLEAN FOUNDATION-30-23-7-SEPARATE-UNTRACKED-GATE COLLISION-CLEAN BASELINE-696 NO-BUSINESS-DIFF ZERO-GIT 2026-07-19T20:38:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-19T19:47:38-04:00 - BLOCKED / REPAIR REQUIRED

**Verdict:** `P0/P1/P2 = 0/1/0`. External A retains sole whole-card ownership. The canonical fifteen-path delivery,
reported named family `34/0/0`, compile `EXIT 0`, clean host refresh and separate foundation `30/23/7` evidence were
accepted as delivery evidence, but the source does not satisfy the frozen close-ownership contract.

### P1 - Spring infers a second runtime close through the host context

- Evidence: `CloudTurnRuntimeConfiguration.cloudTurnTaskRuntime()` uses plain `@Bean` at
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnRuntimeConfiguration.java:115`. Spring Framework 6.1.10
  defines the default `Bean.destroyMethod` as `"(inferred)"`; `CloudTurnTaskRuntime` exposes public no-arg `close()` at
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java:376`.
- Impact: `CloudBrainServer.close()` at `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java:170` first
  calls `turnRuntime.close()`, then `host.close()` closes the Spring context, which infers and calls the same runtime
  `close()` again. The actual ownership/order becomes runtime→runtime→host→server→executor instead of the frozen
  exact-once runtime→host→server→executor sequence; the second call can also repeat the bounded worker join.
- Test gap: `CloudTurnActivationContractTest.runtimeAndHostCloseWithoutOwningServerResources()` at
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnActivationContractTest.java:182` explicitly closes runtime
  and then host but cannot observe/count the context-inferred second runtime close, so current 7/7 is a false negative
  for this lifecycle requirement.

### Required repair and re-delivery gate

1. Within the existing fifteen-path write set, declare the runtime bean as `@Bean(destroyMethod = "")`; Spring host
   context must not own runtime shutdown because `CloudBrainServer` owns the ordered explicit close.
2. Extend `CloudTurnActivationContractTest` with a deterministic assertion that would fail if host context owns or
   invokes runtime close after the explicit runtime close. An exact bean-definition/annotation assertion may support
   the proof, but the test must make the no-second-close contract reviewable.
3. Rerun the frozen named family (`CloudTurnActivationContractTest`, `CloudTurnTaskFactoryAllowlistTest`,
   `CloudTurnTaskRuntimeContractTest`), compile/test-compile, and canonical re-deliver all fifteen paths with updated
   SHA/line/mtime and collision audit. Keep foundation `30/23/7` separate; do not touch its out-of-card fixture.

`无已批准业务差异；按基线等价迁移`. Parent ran no Maven and made no Java/Git mutation.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE+TEST-SOURCE-REVIEW-1 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 OWNER-EXTERNAL-A-RETAINED P1-SPRING-INFERRED-RUNTIME-CLOSE-DUPLICATES-SERVER-EXPLICIT-CLOSE REQUIRE-BEAN-DESTROYMETHOD-EMPTY+ACTIVATION-NO-SECOND-CLOSE-PROOF RERUN-NAMED-34+COMPILE+TESTCOMPILE REDELIVER-15-PATH FOUNDATION-GATE-SEPARATE BASELINE-696 NO-BUSINESS-DIFF PARENT-NO-MAVEN ZERO-GIT 2026-07-19T19:47:38-04:00 -->

## PARENT STALE AUDIT - 2026-07-19T19:59:34-04:00 - COMMUNICATION_STALE / REPAIR_ACTIVE_STALE

- External A has missed two consecutive audit rounds without a STATUS EVENT ACK of Review #1 repair message
  `PARENT-A-TURN40C-REVIEW1-REPAIR-20260719-1947`. Mark `COMMUNICATION_STALE`; retain A as sole owner and preserve
  the complete fifteen-path card.
- The latest A event remains the pre-review `AWAITING_PARENT_REVIEW` event. Runtime configuration and activation test
  remain `4E91D53E/7B418DF0`, with no repair-source change for more than ten minutes; mark `REPAIR_ACTIVE_STALE`.
- Review #1 `0/1/0` and its repair/re-delivery gate remain unchanged. Parent ran no Maven and made no Java/Git mutation.

<!-- TRUE_EOF: TURN-40C PARENT-STALE-AUDIT COMMUNICATION-STALE REPAIR-ACTIVE-STALE TWO-ROUNDS-NO-ACK-REVIEW1 SOURCE-NO-CHANGE-GT10M CONFIG=4E91D53E TEST=7B418DF0 OWNER-EXTERNAL-A-RETAINED PRESERVE-15-PATH REVIEW1-0-1-0-UNCHANGED PARENT-NO-MAVEN ZERO-GIT 2026-07-19T19:59:34-04:00 -->

## PARENT REPAIR SOURCE OBSERVATION - 2026-07-19T20:14:36-04:00 - SOURCE ACTIVE / COMMUNICATION STALE

- Runtime configuration advanced to `FBB02200`/176 physical lines, mtime `20:12:40.899 -04:00`, and now declares
  `@Bean(destroyMethod = "")` on `cloudTurnTaskRuntime()`. The accompanying lifecycle JavaDoc states the exact-once
  server-owned close invariant.
- Activation test advanced to `8B1E11C3`/397 physical lines/8 tests, mtime `20:13:38.400 -04:00`. The added test
  reflects `cloudTurnTaskRuntime`, reads its `@Bean`, and asserts `destroyMethod()` is the empty string, directly
  preventing Spring close inference. The other thirteen delivered path hashes remain unchanged.
- Clear `REPAIR_ACTIVE_STALE` to `REPAIR_SOURCE_ACTIVE`. Communication remains stale because A has not emitted a
  STATUS EVENT ACK for `1936+1940+1947+1959`. The frozen named family is now 35 tests (8+3+24); test/build/re-delivery
  evidence remains pending. Parent ran no Maven while Java writer activity is current.

<!-- TRUE_EOF: TURN-40C PARENT-REPAIR-SOURCE-OBSERVATION REPAIR-SOURCE-ACTIVE COMMUNICATION-STALE CONFIG=FBB02200-176L-DESTROYMETHOD-EMPTY ACTIVATIONTEST=8B1E11C3-397L-8T-ANNOTATION-ASSERTION OTHER-13-SHA-STABLE OWNER-A-RETAINED ACK-1936+1940+1947+1959-PENDING NAMED-35+BUILD+REDELIVERY-PENDING REVIEW1-STILL-BLOCKED PARENT-NO-MAVEN 2026-07-19T20:14:36-04:00 -->

---

## EXTERNAL-A REPAIR #1 CANONICAL RE-DELIVERY - 2026-07-19T20:53:00-04:00 - 15 PATHS (Review #1 P1 fixed)

**Owner:** EXTERNAL-A (sole; card line 75). Fixes Review #1 `P1` (Spring-inferred second runtime close). Baseline `696a12b0`; no business difference. Zero Git mutation.

### P1 repair (within the frozen fifteen paths, two files changed)
1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnRuntimeConfiguration.java` — `cloudTurnTaskRuntime()` now `@Bean(destroyMethod = "")`, disabling Spring 6.1's inferred-destroy so the host context never owns/invokes `CloudTurnTaskRuntime.close()`; `CloudBrainServer` owns the single explicit close in the fixed exact-once runtime→host→server→executor order.
2. `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudTurnActivationContractTest.java` — added deterministic proof `runtimeBeanDisablesSpringDestroyInferenceSoTheHostContextNeverClosesIt`: reflects the `cloudTurnTaskRuntime` `@Bean` annotation and asserts `destroyMethod() == ""` (a plain `@Bean` returns `"(inferred)"` and fails). Makes the no-second-close contract reviewable.

### Updated write-set SHAs (2 changed; other 13 unchanged from the 20:38 delivery)
- CHANGED `host/CloudTurnRuntimeConfiguration.java` **`FBB02200`/176L**/20:12:40 (was `4E91D53E`/173L).
- CHANGED `test/host/CloudTurnActivationContractTest.java` **`8B1E11C3`/397L**/20:13:38, now **8 tests** (was `7B418DF0`/376L/7T).
- UNCHANGED (13): App `5711BC3E`/112L, Server `CA2A1EF4`/196L, Handler `01DE94A2`/399L, Routes `063DE4FC`/94L, Host `05FB55E9`/136L, PlayerState `1E932914`/1443L, Factory `3B511EE8`/109L, Runtime `8368ED7E`/437L, Authority `C651BD8D`/1241L, Assembly `69A51B55`/37L, AllowlistTest `FEFB6DC2`/223L, RuntimeTest `DB3A486A`/840L, TurnGameClient `1B203987`/221L.

### Named-family evidence (PowerShell mvn offline, sole Cloud writer)
- `mvn -o -DskipTests=false -Dtest=CloudTurnActivationContractTest,CloudTurnTaskFactoryAllowlistTest,CloudTurnTaskRuntimeContractTest test` → **BUILD SUCCESS exit 0**; per-class `8 / 3 / 24`, aggregate **35 run / 0 fail / 0 error** (was 34; +1 no-second-close proof).
- `mvn -q -o compile` → **exit 0**; test-compile passed within the family run.
- Host graph refreshes with zero bean errors.

### Separate out-of-card gate (unchanged, parent-classified)
- `CloudWholeTaskFoundationContractTest` → **30 run / 23 pass / 7 error** (untracked out-of-card `remote/run` `nativeHandle` fixture collision, thrown before the TURN-40C seam). Not edited.

### Collision audit
Exactly the fifteen write-set paths; this repair touched only the two named above. No out-of-write-set Java modified; `DecisionEngine` reached by reflection only. Zero Git mutation; no runtime/server/UI launched; `D:\mavenProject\DHXY` frozen.

`无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40C EXTERNAL-A REPAIR1-RE-DELIVERED 15-PATH P1-DESTROYMETHOD-EMPTY+NO-SECOND-CLOSE-PROOF CONFIG=FBB02200-176L ACTIVATIONTEST=8B1E11C3-397L-8T NAMED-FAMILY-35-0-0 COMPILE-0 FOUNDATION-30-23-7-SEPARATE COLLISION-CLEAN BASELINE-696 ZERO-GIT 2026-07-19T20:53:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - 2026-07-19T20:14:36-04:00 - PASSED / OWNER RELEASED

**Verdict:** `P0/P1/P2 = 0/0/0`. Review #1 P1 is fully repaired. External A whole-card ownership is released.

- `CloudTurnRuntimeConfiguration.cloudTurnTaskRuntime()` now declares `@Bean(destroyMethod = "")`; Spring host
  context cannot infer or invoke `CloudTurnTaskRuntime.close()`. `CloudBrainServer` remains the single owner of the
  exact-once runtime→host→server→executor close sequence.
- `CloudTurnActivationContractTest.runtimeBeanDisablesSpringDestroyInferenceSoTheHostContextNeverClosesIt()` reflects
  the exact bean method and asserts empty destroyMethod. A plain `@Bean` would expose `"(inferred)"`, so the original
  defect is deterministically guarded. The activation suite is now 8 tests.
- All fifteen path hashes/line counts match Repair #1 re-delivery; only config `FBB02200`/176L and activation test
  `8B1E11C3`/397L changed from the first delivery. The other thirteen hashes remain frozen. No out-of-write-set Java,
  second store/engine/protocol or business algorithm was introduced.
- Accept worker evidence: named family `35/0/0` (8+3+24), compile `EXIT 0`, test-compile within the family run, clean
  host refresh. Foundation `30/23/7` remains the previously classified separate untracked fixture collision.

`无已批准业务差异；按基线等价迁移`. Parent ran no duplicate Maven and made no Java/Git mutation.

<!-- TRUE_EOF: TURN-40C PARENT-SOURCE+TEST-SOURCE-REVIEW-2 PASSED P0=0 P1=0 P2=0 OWNER-EXTERNAL-A-RELEASED REPAIR1-DESTROYMETHOD-EMPTY+NO-SECOND-CLOSE-PROOF NAMED-35-0-0 COMPILE-0 TESTCOMPILE-PASS HOST-REFRESH-CLEAN 15-PATH-COLLISION-CLEAN FOUNDATION-SEPARATE BASELINE-696 NO-BUSINESS-DIFF PARENT-NO-MAVEN ZERO-GIT 2026-07-19T20:14:36-04:00 -->
