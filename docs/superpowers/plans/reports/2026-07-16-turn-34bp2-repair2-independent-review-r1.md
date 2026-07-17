# TURN-34BP2 Repair #2 Independent Whole-Card Review R1

## Verdict

**APPROVED**

`P0/P1/P2 = 0/0/0`.

Independent R1 whole-card source review found no blocking defect in CR271 TURN-34BP2 Repair #2. This approval is source-only for the frozen whole-card bytes; it does not claim the pending Cloud compile gate or final card closure.

## Frozen Evidence

- Reviewed Cloud source: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`.
- Frozen source identity: **1,400 lines / SHA-256 `8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101`**.
- Reviewed the complete original card through its then-current true EOF: 366 lines / SHA-256 `6f17dbc32313c42e04fb2328353568ab50e38be062dad1090bbc27e7edb9992d`.
- Canonical card TRUE_EOF reviewed:

```text
<!-- TRUE_EOF: TURN-34BP2 PARENT-FRESH-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED R1-RAWLS-019f6c31-9411-74a1-b81b-911626bed1a6 R2-GALILEO-019f6c31-db0e-7c93-9509-cc538010f312 SHA=8d79d198 DUAL-REVIEW-IN-PROGRESS 2026-07-16T14:30:45-04:00 -->
```

- Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, top CR271 entries in `docs/ACTIVE_WORK.md`, authoritative plan sections 14-19, the HTTPS turn protocol foundation, and `docs/业务逻辑.md`, including the confirmed `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` business baseline.
- Baseline maintenance source was independently identified as 1,123 lines / SHA-256 `4beaffd08314f694b41a841dff236c4ce00dc335cbe75de74a9f667a53803eda` in the Cloud migration baseline, byte-equal to the Git `696a12b0` source.
- Both repository statuses were read before and after source inspection. Existing dirty/untracked state was protected.

## Contract Verification

### Coordination discriminator and four combinations

- `ScopedTeamKey` is exactly `(ExecutionScope scope, TeamCoordination coordination, String maintenanceKey)` at lines 1364-1365.
- Its only constructor call is `scopedTeamKey(...)` at lines 1260-1265. `teamCoordination(...)` selects exactly one typed discriminator: nonblank explicit local-team session becomes `SessionCoordination(sessionKey)`; otherwise exact effective window becomes `WindowCoordination(windowId)` at lines 1273-1279.
- Same scope + same explicit session shares: record equality sees the same `ExecutionScope`, `SessionCoordination`, and maintenance key.
- Same scope + no session + different windows isolates: distinct `WindowCoordination(windowId)` values make the containing `ScopedTeamKey` unequal.
- Different scope never shares: `ExecutionScope(tenantId,userId,deviceId)` is the first `ScopedTeamKey` field.
- Formal and local claim kinds cannot collide: `FormalTeamRoundClaimKey` and `LocalSessionCapabilityClaimKey` are distinct records under sealed `MaintenanceClaimKey` at lines 1378-1390.

### Complete shared-state propagation

- Active round uses `Map<ScopedTeamKey,Integer>` and every read/write resolves through `scopedTeamKey(...)`.
- Maintenance window state uses `Map<TeamRoundKey,TeamMaintenanceWindowState>`, where `TeamRoundKey` embeds that same `ScopedTeamKey`.
- Formal claim uses `FormalTeamRoundClaimKey(new TeamRoundKey(teamKey,round))`; the outer claim limit therefore has the same session-or-window discriminator, not merely a scoped claim-member window.
- `pruneOlderTeamRoundClaims(...)` field-compares the same `ScopedTeamKey` for formal claims and window state at lines 1300-1306. No prefix scan, delimiter parse, dual lookup, or compatibility fallback remains.

### Frozen surfaces

- Four typed shared maps are present exactly as required at lines 59-65: `activeTeamRoundByKey`, `teamMaintenanceWindowStateByRound`, `localTeamSessions`, and `summonSkillClaimsByTeamRound`.
- Four BP3 per-window maps remain String-keyed and untouched in ownership at lines 55-58: three `Map<String,Long>` maps and `Map<String,SummonSkillWindowState>`.
- Exactly 19 public methods remain, with the frozen names/signatures spanning lines 73-615. No public API was added, removed, or activated.
- Supplied context precedence is explicit in `effectiveContext(...)` at lines 1017-1026. `ExecutionScope.NONE` has one return site, reachable only when the supplied context is null and the ThreadLocal holder is empty. Any nonnull effective context missing scope/invocation authority throws at lines 1239-1256; authority failures are not broad-caught or downgraded.
- The maintenance-key fallback order remains explicit key, supplied `requestedTaskCode`, supplied `taskCode`, then `DEFAULT_WINDOW_KEY` at lines 1281-1291.

### Business-equivalence and forbidden expansion

- The public maintenance flow remains normalize -> first checkpoint -> optional broadcast -> handled/failure/interrupted short-circuit -> at most one Summon delegate -> no-action at lines 597-615.
- Existing CommonBox/TeamReturn capability boundaries, Summon gates, claim acquire/release/retain behavior, `GameContext.ActionState` handling, UNKNOWN backoff, existing cache TTLs, and existing retry-related status bytes remain in their prior control-flow positions.
- Repair #2 adds only the typed session-or-window discriminator and its private record types. It adds no business decision, terminal conversion, command/action, UUID, retry, session authority, owner, lease, ledger, TTL, queue, durable workflow, delegate, checkpoint, sleep, timer, or automatic replay/resend behavior.
- No intentional business difference was found: **无已批准业务差异；按 `696a12b0` 与 exact-context HTTPS turn 合同等价迁移。**

## Execution Boundary

Per the review assignment, no Java/card/plan/ACTIVE_WORK/dashboard edit was made; only this R1 report was created. No Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture, input, or Git mutation was run.

<!-- TRUE_EOF: TURN-34BP2 REPAIR-2 INDEPENDENT-WHOLE-CARD-REVIEW-R1 APPROVED P0-0-P1-0-P2-0 FROZEN-SHA=8d79d198d9f0b7443aeea2057f5088bb6471aee63ee9edb17ade0fb6a528f101 CANONICAL-CARD-TRUE-EOF=PARENT-FRESH-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED SOURCE-ONLY CLOUD-COMPILE-PENDING 2026-07-16 -->
