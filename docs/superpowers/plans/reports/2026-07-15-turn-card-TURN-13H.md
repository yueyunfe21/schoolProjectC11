# TURN-13H - Shared Turn Capabilities in Dormant Cloud Service Host

## CLAIMED

- claimedAt: `2026-07-15T19:28:56-04:00`
- owner: `TURN-13H implementation Worker (current task)`
- role: implementation only; parent remains the sole reviewer and final decision owner.
- businessBaseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- startDependsOn: `TURN-13G + TURN-05 + TURN-13` (source start gate recorded as satisfied by CR271).
- authority: `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`, exact `TURN-13H` card and Sections 18-19. The earlier readiness helper is advisory where it predates the authoritative test write set.
- businessDifference: `无已批准业务差异；按 696a12b0 基线等价迁移。`

### Exact write set

Cloud production:

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnRoutes.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceHost.java`
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java`

Cloud test:

5. `src/test/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceHostTurnCapabilityContractTest.java`

Process record:

6. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-13H.md`

No POM, fixture, configuration, other production/test/documentation, target, or Git write is in scope. Existing dirty and untracked files in both repositories remain protected. Maven execution is deferred while Ohm/Nash Java writers are active.

## SOURCE + TEST DELIVERED

- deliveredAt: `2026-07-15T19:36:01-04:00`
- implementationRole: source/test delivery only; no review verdict is asserted here.
- businessDifference: `无已批准业务差异；按 696a12b0 基线等价迁移。`

### Changed files and behavior

1. `CloudTurnRoutes.java`
   - `Bundle` now exposes the exact `CloudTurnCommandPort` backed by the supplied `CloudTurnExchange` and the exact single `CloudTemplateCatalog` created for the template handler.
   - The same catalog local is passed to both `CloudTemplateHttpHandler` and `Bundle`; no second exchange, catalog, wrapper state, or fallback was introduced.
2. `CloudBrainServer.java`
   - Retains only the two new dormant turn capabilities, `CloudTurnCommandPort` and `CloudTemplateCatalog`, obtained from the existing route bundle.
   - Does not create a `CloudServiceHost`, add a route/endpoint/startup hook, or activate a Task/loop/timer.
3. `CloudServiceHost.java`
   - Replaces the unused two-argument create form with required `scope/stateRoot/commandPort/catalog` arguments.
   - Fail-fast null checks run before context creation, and the caller-supplied exact command port/catalog identities are registered before configuration refresh.
4. `CloudServiceConfiguration.java`
   - Preserves the existing service scan, remote port imports, scoped storage beans, and `CloudTemplateAssets` behavior.
   - Adds only the narrow `com.yueyunfe.dhxy.cloudbrain.turn.client` scan, explicit existing `TaskExecutionContextHolder` import required by the authoritative card, and one stateless `CloudTurnActionFactory` bean.
5. `CloudServiceHostTurnCapabilityContractTest.java`
   - Adds four real JUnit contract methods in the authorized `HTTPS_TURN_CONTRACT_TEST_FAMILY`; no source-text guard or server startup is used.

### Contract assertions delivered

- Route bundle exposes the supplied exchange as the same command-port identity and exposes one stable template-catalog identity.
- Host registration returns the exact route command port/catalog objects and constructs both the stateless action factory and scanned `TurnGameClient`.
- Fake command/catalog counters remain zero during dormant host wiring, proving no command, metadata read, or template load is triggered.
- Null `scope`, `stateRoot`, `commandPort`, or `catalog` each fail fast.
- The test never starts `CloudBrainServer`, a Task, a loop, capture/input/UI, or a physical Service action.

### Source self-check evidence

- `rg CloudServiceHost.create` found only this new test; production remains dormant with zero create callers.
- Focused scans found no server start, thread/executor/timer/Task, reflection, or source-string guard in the test.
- All five source/test files passed the read-only trailing-whitespace check.
- `git diff --check` for tracked `CloudBrainServer.java` reported no whitespace error.
- The test file is physically present, but existing `.gitignore:15` ignores the whole `src/test/` tree; no Git/index mutation was permitted or performed.

### Deferred verification and residual risk

- `Maven deferred due active writers` (Ohm T01 and Nash T03B); no Maven command and no shared `target` write was performed.
- Parent still needs to run `mvn -q -Dtest=CloudServiceHostTurnCapabilityContractTest test`, the applicable Cloud compile gate, and the planned package cohort after all Java writers are stable.
- Until that named test runs, full Spring host refresh and the existing migrated service graph remain compile/runtime verification risks rather than claimed evidence.
- The ignored test-path fact must be accounted for by the parent workflow; this worker did not alter `.gitignore` or the Git index because both are outside the frozen write set.

## PARENT SOURCE/TEST REVIEW #1 - TEST REPAIR REQUIRED

- reviewedAt: `2026-07-15T19:38:47-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/1/0 / REPAIR #1`
- Production source accepted in this review: parent independently verified that `CloudTurnRoutes.create(...)` passes
  one local `CloudTemplateCatalog` to both the template handler and Bundle, and exposes the supplied exchange as the
  exact command port. `CloudBrainServer` only retains those two Bundle capabilities. `CloudServiceHost.create(...)`
  requires and registers exact scope/stateRoot/commandPort/catalog before refresh. Configuration keeps the existing
  service graph, adds only `turn.client`, imports the required existing holder and creates one stateless action
  factory. No second exchange/catalog, host caller, endpoint, Task factory, retry or activation was added.

### P1-1 - The test does not execute the frozen zero-thread assertion

- Evidence: `CloudServiceHostTurnCapabilityContractTest.java:64-85` creates and closes the real Spring host, then
  asserts only zero command execution, metadata reads and template loads. It never snapshots or compares live threads.
  The worker report's source scan that says no thread API is present is not a runtime JUnit assertion.
- Impact: Section 19.4 requires this exact card test to prove “zero host/Task/thread activation”. A configuration or
  scanned constructor could start a thread while all three current counters remain zero, and the test would pass.
- Repair condition: in the existing dormant-host test, snapshot live thread IDs before context creation and assert no
  new live thread while the context is refreshed and again after close/settling. Keep the assertion deterministic and
  report any created thread id/name. Continue to use fake capabilities and do not start CloudBrainServer, Task, loop,
  capture/input/UI or a real command. Retain all four existing tests and production source unchanged.

### Frozen Repair #1 write set

Only `CloudServiceHostTurnCapabilityContractTest.java` and this report may change. No production, POM, fixture,
configuration, other test/document or Git mutation is authorized. Maven remains deferred while Java writers are
active; isolated/source claims must not be presented as the required named Maven pass.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## REPAIR #1 DELIVERED

- deliveredAt: `2026-07-15T19:41:27-04:00`
- owner: original `TURN-13H` implementation Worker.
- role: implementation repair delivery only; no review verdict is asserted here.
- repairWriteSet: only `CloudServiceHostTurnCapabilityContractTest.java` and this original report.
- productionChange: none; all four TURN-13H production files remained read-only during Repair #1.

### P1-1 implementation evidence

- The existing `dormantHostWiringDoesNotInvokeTurnOrTemplateCapabilities` test now snapshots every live JVM thread as exact `threadId -> threadName` immediately before `CloudServiceHost.create(...)`.
- While the refreshed host remains open, the test snapshots live threads again and asserts that the alive-ID difference from the baseline is empty.
- The try-with-resources close then completes, the current JUnit thread waits `100ms`, and a third live-thread snapshot is checked against the same baseline for post-close leakage.
- No thread ID or name is allowlisted or discarded. Only `Thread.isAlive()` entries participate, and any non-baseline entry is reported as `id=<id>, name="<name>"` in the assertion failure.
- The four original `@Test` methods, exact command-port/catalog identity checks, zero command/metadata/template-load counters, scanned client/factory construction, and four required null fail-fast assertions remain present.
- The test still does not start `CloudBrainServer`, a Task, loop, capture/input/UI, physical Service action, executor, timer, or a new thread, and it contains no source-string guard or reflection dispatch.

### Verification state

- Read-only source check: `TEST_COUNT=4`; no trailing whitespace; both thread-difference assertions and the `100ms` post-close settle are present.
- `Maven deferred due active writers`; no Maven command and no shared `target` write was performed.
- Parent retains the named `CloudServiceHostTurnCapabilityContractTest` Maven test plus compile/package cohort gates after Java writers stabilize.
- Existing `.gitignore:15` still hides `src/test/`; the repaired test remains physically present and no Git/index mutation was performed.

## PARENT SOURCE/TEST REVIEW #2 - REPAIR PASSED

- reviewedAt: `2026-07-15T19:46:15-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / REQUIRED MAVEN+CLOUD COMPILE PENDING`
- Parent independently reread all four production files and the complete repaired contract test. Repair #1 changed
  only the frozen test/report write set; the shared command-port/catalog identities, required host registration,
  narrow `turn.client` scan, stateless action factory and dormant production wiring remain unchanged.

### Closed P1 evidence

- `CloudServiceHostTurnCapabilityContractTest.java:76-96` snapshots live thread IDs immediately before the real
  Spring context refresh, compares the live-ID set while the host remains open, closes the host, waits for the
  bounded settle interval, and compares against the same baseline again.
- `CloudServiceHostTurnCapabilityContractTest.java:115-134` includes every alive JVM thread, applies no name/ID
  allowlist, and reports every added thread as exact `id/name`; therefore the assertion cannot silently discard a
  host, Task, loop, executor, timer or HTTP selector thread.
- The original exact command-port/catalog identity, `CloudTurnActionFactory`/`TurnGameClient` construction, zero
  command/metadata/template calls and four null fail-fast cases remain direct assertions. Production has no
  `CloudServiceHost.create(...)` caller, Task factory, loop start, endpoint, retry or second exchange/catalog.

### Remaining gates

- The parent did not run Maven while Nash is actively writing TURN-T03B Repair #1. The required
  `mvn -q -Dtest=CloudServiceHostTurnCapabilityContractTest test` and applicable Cloud compile remain pending;
  this source/test-source verdict is not `CARD APPROVED`.
- Cloud `src/test/` remains ignored by the existing `.gitignore:15`; the physical test is retained, and no Git/index
  mutation was made.

**No approved business differences; equivalent migration against baseline `696a12b0`.**
