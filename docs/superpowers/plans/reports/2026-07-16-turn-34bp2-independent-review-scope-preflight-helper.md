# CR271 TURN-34BP2 independent-review scope preflight

## 1. Helper role and non-verdict boundary

- Role: `CR271 Internal` read-only helper for the future TURN-34BP2 two-independent-review gate.
- Purpose: freeze two non-dependent reviewer scopes that may start only after a canonical BP2 delivery has received
  an explicit parent source pass.
- This report is not implementation, delivery, source review, independent review, reviewer assignment, owner claim,
  parent judgment, test/build evidence, or card approval.
- The only writable artifact in this pass is this report. No Java, test, card, plan, `ACTIVE_WORK.md`, dashboard,
  protocol, business document, or other report was modified.
- No reviewer was created or dispatched. This helper is not either future reviewer and cannot approve its own
  preflight.
- No Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git command was run.

Business authority remains `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

## 2. Authority and precedence

This preflight was prepared after reading and cross-checking:

1. `D:\mavenProject\DHXY\AGENTS.md`.
2. `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`.
3. The current CR271 top section of `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md`.
4. Sections 14-19 of
   `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-15-https-turn-complete-migration-card-plan.md`.
5. `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-15-https-turn-thin-client-protocol-design.md`.
6. `D:\mavenProject\DHXY\docs\业务逻辑.md` in full.
7. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP1.md` and
   `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-16-turn-card-TURN-34BP2.md` through their
   physical tails at this preflight snapshot.
8. All current BP2 readiness/source-review/test preflights:
   - `2026-07-16-turn-34bp2-readiness-preflight-helper.md`;
   - `2026-07-16-turn-34bp2-readiness-delta-helper.md`;
   - `2026-07-16-turn-34bp2-delivery-source-review-preflight-helper.md`;
   - `2026-07-16-turn-34bp2-test-acceptance-preflight-helper.md`.

Sections 14-19 override conflicting older planning detail. The approved HTTPS turn protocol supplies the no
automatic retry/no ledger/no second workflow boundary. `docs/业务逻辑.md` and strict `696a12b0` supply business
order, priority, fallback, timing, claim, terminal, and maintenance semantics.

BP1 is a predecessor, not BP2 approval. Its parent-received source snapshot is:

| Artifact | Frozen BP1 identity |
|---|---|
| `TaskExecutionContext.java` | 527 lines / SHA-256 `a9c34d4e9bc960f35ca982f4d39ea8342323dc1d92f0ae1199b5677e59e2cb4e` |
| `TaskExecutionContextTurnContractTest.java` | 872 lines / SHA-256 `3b117895cef72af5085e646d9fe76d8f4f648142f93a89e3dfa52ec4292b2785` |

BP1 has parent source review and two independent source approvals, while its stable-writer test/build gate remains
pending. BP2 may consume only BP1's public context/checkpoint contract; neither BP2 reviewer may reopen BP1 or
claim that BP1 build evidence approves BP2.

## 3. Current WIP is excluded

At `2026-07-16T12:12:36.4400967-04:00`, identity-only observation of the current Cloud file was:

| Artifact | Identity-only observation |
|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java` | 1,290 lines / 69,155 bytes / SHA-256 `12edcb1ba98866e8f23b86633618e3290ae9e9540d530b2e75b8b8d7a978e51d` / mtime `2026-07-16T12:10:40.9101225-04:00` |

This identity is not a review snapshot, finding, approval, rejection, or future reviewer baseline. The source body
was not assessed in this pass. At the same snapshot, the BP2 card still had worker claim prose but no canonical
delivery followed by a parent source-pass receipt. Therefore:

- no current WIP byte may appear as evidence in R1 or R2;
- the frozen BP2 starting identity remains 1,224 lines / SHA-256
  `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`;
- the only reviewable target will be the exact final SHA named in a later parent source-pass receipt;
- a worker-declared delivery SHA without parent receipt is insufficient;
- if the source changes after parent receipt, both independent reviews become invalid and must restart after a new
  canonical delivery and new parent source pass.

## 4. Review activation gate

Neither reviewer may start until all boxes below are true:

- [ ] BP2 card physical tail contains a canonical delivery with final source SHA-256, line count, byte count,
  changed method/type index, two-item write-set statement, owner release, and no-business-difference statement.
- [ ] A later parent-authored receipt explicitly says BP2 source passed and names the same final production SHA.
- [ ] The source on disk independently recomputes to that parent-received SHA and line/byte identity.
- [ ] The BP2 card SHA is recorded so both reviewers bind to the same physical card tail.
- [ ] BP1 read-only artifacts still equal `a9c34d4e...` / `3b117895...`.
- [ ] No implementation or repair writer remains active on `TaskMaintenanceService.java`.
- [ ] Parent supplies the reviewed `963b028c... -> final` exact-delta provenance; it may guide review but cannot
  replace whole-file reading.
- [ ] Parent creates and assigns two different non-implementer reviewers. This helper does neither.

If any box is false, the independent gate is `NOT STARTED`; it cannot be converted into an early WIP review.

## 5. Shared whole-file and independence contract

R1 and R2 have different primary questions, not different source fragments. Each reviewer must independently read
the complete parent-received final `TaskMaintenanceService.java`, from the first import through the physical end of
the final private type. The minimum whole-file coverage table in each report is:

| Whole-file area | R1 | R2 |
|---|---:|---:|
| package/imports/class annotation/constructor collaborators | full line-by-line | full line-by-line |
| constants and every field declaration | full line-by-line | full line-by-line |
| all 19 public methods | full line-by-line | full line-by-line |
| formal team round/window paths | full line-by-line | full line-by-line |
| local-session lifecycle/capability paths | full line-by-line | full line-by-line |
| claim acquire/prune/release/retain paths | full line-by-line | full line-by-line |
| `runOpportunisticMaintenance` and all broadcast/Summon/cache/result helpers | full line-by-line | full line-by-line |
| all private records/enums/sealed interfaces/state types at file bottom | full line-by-line | full line-by-line |

The following do not satisfy this gate:

- reviewing only the changed hunks, changed-method index, typed-key declarations, business methods, or a line range;
- splitting the file so R1 reads types while R2 reads workflow;
- relying on parent `P0/P1/P2=0/0/0`, a helper checklist, grep/count output, or the other reviewer as a substitute
  for direct whole-file reading;
- approving a patch delta without reading unchanged code that calls or consumes the changed types;
- carrying forward an approval after any source SHA change.

Each reviewer must compute the final source SHA before and after review and record that both values equal the same
parent-received SHA. Each report must state: `I did not use the other independent-review report or verdict to reduce
my review scope.` A reviewer may report any P0/P1/P2 discovered outside the primary question; it must not be ignored
or deferred merely because the other reviewer owns the primary analysis of that area.

R1 and R2 must not cite each other's approval, finding count, or prose as evidence. The parent may compare both only
after each has independently reached a true-EOF verdict.

## 6. R1 primary scope - typed key, scope, authority, public API

R1 owns the independent conclusion for typed-key completeness, scope isolation, context authority, and public/API
compatibility. R1 still performs the shared whole-file review in Section 5.

### 6.1 Four migrated maps and four BP3-deferred maps

R1 must prove every access path of these four shared maps uses one typed decision:

```text
activeTeamRoundByKey                -> Map<ScopedTeamKey, Integer>
teamMaintenanceWindowStateByRound  -> Map<TeamRoundKey, TeamMaintenanceWindowState>
localTeamSessions                   -> Map<ScopedLocalSessionKey, LocalTeamSessionState>
summonSkillClaimsByTeamRound        -> Map<MaintenanceClaimKey, Set<ScopedWindowKey>>
```

R1 must separately prove BP2 did not absorb BP3 by changing these four per-window maps or their lifetime policy:

```text
Map<String, Long> lastSummonSkillCleanAtByWindow
Map<String, Long> lastSummonSkillNotDueLogAtByWindow
Map<String, Long> summonSkillUnknownRetryAfterByWindow
Map<String, SummonSkillWindowState> summonSkillStateByWindow
```

No raw compatibility map, typed-first/raw-fallback, dual read, dual write, prefix scan, alias key, or second lookup
may remain in any migrated domain. Remaining delimiter logic is acceptable only when R1 proves it belongs solely to
the four BP3-deferred per-window paths and records it as deferred, not as BP2 closure.

### 6.2 Typed dimensions and isolation matrix

R1 must prove the final private model encodes, by record/enum/sealed type rather than delimiter text:

- exact execution scope `(tenantId,userId,deviceId)` and a type-distinct explicit no-context variant;
- exact window identity `(scope,windowId)`;
- explicit local-team session `(scope,sessionKey)`;
- formal coordination kind `WINDOW` versus `LOCAL_SESSION`, coordination key, and maintenance key;
- formal team round `(ScopedTeamKey,round)`;
- local capability claim `(ScopedLocalSessionKey,capability,epoch)`;
- type distinction between formal and local claim identities.

Required outcomes:

| Relation | Required state behavior |
|---|---|
| different tenant, equal remaining text | isolated |
| different user, equal remaining text | isolated |
| different device, equal remaining text | isolated |
| same scope + same explicit session + different window | baseline local capability/session sharing |
| same scope + no explicit session + different window | isolated by exact window |
| equal session text + different scope | isolated |
| identifiers containing `|`, `#`, or `local-team:` | ordinary typed field values; no collision or parsing |
| formal round claim versus local capability claim | cannot alias by Java type |

R1 must verify the existing maintenance-key fallback order remains explicit key -> requested task code -> task code
-> existing default. Key migration may not reorder or broaden it.

### 6.3 Context authority

R1 must trace every public-to-key path and prove:

```text
supplied context present                  -> supplied context only
supplied context null + holder present    -> holder context
supplied context null + holder empty      -> explicit typed no-context
present context with invalid authority    -> existing typed failure; never no-context downgrade
```

No field may be mixed between supplied and holder contexts. No broad catch may turn scope/invocation/window
authority failure into a raw/default/global key. BP2 must not read BP1's private latch, duplicate
`latestExactTurnMetadata()`, or add a second latest-metadata observation.

### 6.4 Public and construction surface

R1 must enumerate and compare all 19 public declarations by visibility, return type, name, parameter types, order,
arity, overload set, and checked-exception surface. Method count or names alone are insufficient.

The six live TURN-34A APIs requiring caller-compatible behavior are:

```text
awaitTeamFirstAidMaintenanceWindowOpen
awaitLocalTeamSupportCapabilityOpen
isLocalSupportMemberSession
isLocalSupportMemberCandidate
isPendingLocalSupportLeaderDetection
isLocalTeamSupportCapabilityOpen
```

The five Lombok constructor collaborators must remain exactly:

```text
BotProperties
GameContext
DialogService
SummonSkillService
TaskExecutionContextHolder
```

R1 must also confirm no new production caller activates these four dormant lifecycle APIs:

```text
registerLocalTeamSessionCandidate
markLocalTeamWindowRoleDetected
markLocalTeamLeaderDetected
completeLocalTeamSessionWindow
```

R1 records the current caller evidence for the six live APIs and the zero-external-caller evidence for the four
dormant APIs. Search output is supporting evidence only; R1 must inspect the declarations and relevant call sites.

### 6.5 Structure findings owned by R1

R1 verifies private key types remain after the main workflow, no public type leaks are introduced, and there is no
same-scope wrapper/helper ladder or one-line compatibility adapter hiding authority or lookup decisions.

R1's APPROVED verdict requires every R1 item above plus shared whole-file completion. Any scope collision,
authority downgrade, API drift, unapproved BP3 work, or incomplete typed-map migration is at least P1; a path that
can delegate against the wrong window/context is P0. Documentation/placement/wrapper problems are P2 unless their
runtime effect raises severity.

## 7. R2 primary scope - 696 order, claim maps, terminal/zero-new surface

R2 owns the independent conclusion for strict `696a12b0` business equivalence, claim-map branch semantics,
terminal propagation, and the zero-new command/workflow surface. R2 still performs the shared whole-file review in
Section 5.

### 7.1 Strict business order

R2 must read the complete strict baseline artifact at:

`D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`

The recorded baseline identity is 1,123 lines / SHA-256
`4beaffd08314f694b41a841dff236c4ce00dc335cbe75de74a9f667a53803eda`.

R2 must prove the final public path remains:

```text
normalize request
-> first checkpoint
-> optional maintenance broadcast
-> handled / BROADCAST_FAILED / INTERRUPTED short-circuit
-> optional Summon maintenance
-> no-action
```

Broadcast short-circuits must reach zero Summon delegates. An eligible pass reaches at most one
`cleanSummonSkillsOnce(...)`. Checkpoint placement, conditions, return projection, failure propagation, and
fall-through may not move.

### 7.2 Priority, capability, and Summon gates

R2 must prove:

- CommonBox remains the current window's higher-priority maintenance fact; TaskMaintenance does not consume a box.
- TeamReturn remains capability-only; TaskMaintenance performs no return input or business progression.
- capability open/close sets remain exact `5/1/5/2`:
  `FIRST_AID, PATHING_WINDOW, COMMON_BOX, SUMMON_SKILL, LEFT_TOP_STATUS` / `FIRST_AID` / the same five / 
  `TEAM_RETURN, COMMON_BOX`.
- existing overlap behavior is unchanged; no lease/reference-count policy appears.
- Summon gates remain feature -> interval -> FREE -> due -> UNKNOWN retry-after -> existing 2h tail/count cache ->
  team/local round -> capability/pathing -> same-window duplicate/max cleaner -> second checkpoint -> one delegate.
- static-tail, skill-count, `UNKNOWN`, ultimate, cooldown, and cache invalidation semantics remain baseline exact.
- no second observation, safer-looking cleanup, retry, fail-closed rule, TTL change, or fallback change was added.

### 7.3 Claim-map behavior

R2 must trace every formal and local claim branch, including typed-key construction as consumed by the branch, and
prove:

1. Formal acquisition uses one `TeamRoundKey`; local acquisition uses one capability/session/epoch claim key.
2. Same-window duplicate rejection remains before max-cleaner rejection.
3. Effective max cleaners remains `max(1, configured)` and the owner is added once.
4. Known failure with no Summon state change releases only the current scoped-window claim.
5. Delete/ultimate state change retains the claim exactly as before.
6. Strong terminal/uncertain/STOP propagation is not converted to an ordinary failure that releases and retries.
7. Empty owner set removes only its own entry.
8. Older-round pruning removes only older formal `TeamRoundKey` entries for the same scoped team and never local
   capability claims.
9. No TTL, lease, owner authority, reference count, compaction, background cleanup, retry, or compatibility branch
   was introduced.

R2 must not treat typed-map declarations alone as claim proof; it must follow acquire, duplicate, max, delegate,
result classification, release/retain, and prune control flow end to end.

### 7.4 Terminal, ActionState, and zero-new surface

R2 must prove existing STOP, typed checkpoint transition, fatal, terminal, and uncertain channels propagate once,
without false/success conversion, retry, replay, resend, or a second delegate. It must verify the existing
`GameContext.ActionState` save -> `INTERACTING` -> guarded `finally` restore rule does not overwrite a newer state.

Relative to the parent-verified `963b028c...` start, BP2 must add zero of every surface below:

| Surface | Net new allowed |
|---|---:|
| latest metadata observation | 0 |
| checkpoint | 0 |
| Dialog delegate | 0 |
| Summon delegate | 0 |
| `TurnGameClient.execute` / command | 0 |
| action / actionId / UUID | 0 |
| retry / replay / resend | 0 |
| sleep / wait policy / timer / scheduler / thread | 0 |
| TTL / expiry policy | 0 |
| session authority / owner / lease / ledger / queue / durable workflow | 0 |
| host/factory/runtime/startup activation | 0 |
| CommonBox/TeamReturn physical mechanics | 0 |

Source-count tripwires may support this review but never replace full control-flow reading. The retained start
counts were `checkpoint(context)=4`, `dialogService.handleDialog=1`,
`summonSkillService.cleanSummonSkillsOnce=1`, `System.currentTimeMillis=12`, `Thread.sleep=0`, monitor
`wait=2`, UUID/randomUUID=0, and `TurnGameClient/.execute=0`. Equal counts do not prove equal order or semantics.

R2's APPROVED verdict requires every R2 item above plus shared whole-file completion. A new/replayed command,
automatic retry, terminal success fabrication, wrong-window delegate, CommonBox consumption, or TeamReturn input is
P0. Business-order, claim, capability, cache/UNKNOWN, ActionState, TTL, or zero-new workflow drift is at least P1.
Evidence or explanation gaps with no demonstrated runtime change are P2 unless further review raises severity.

## 8. Independence matrix

| Question | R1 primary | R2 primary | May either skip it? |
|---|---:|---:|---:|
| full final file line-by-line | yes | yes | no |
| parent-received final SHA/card-tail binding | yes | yes | no |
| exact two-item write-set/provenance | yes | yes | no |
| four typed maps / four BP3-deferred maps | yes | supporting read | no whole-file skip |
| scope/session/window isolation | yes | supporting read | no whole-file skip |
| supplied/holder/no-context authority | yes | supporting read | no whole-file skip |
| 19 public / 5 collaborator / 6 live / 4 dormant surfaces | yes | supporting read | no whole-file skip |
| strict 696 top-level and Summon order | supporting read | yes | no whole-file skip |
| claim acquire/prune/release/retain | supporting read | yes | no whole-file skip |
| terminal/uncertain/ActionState | supporting read | yes | no whole-file skip |
| zero-new command/action/UUID/workflow surface | supporting read | yes | no whole-file skip |

`Supporting read` means the reviewer still reads and may raise a finding; it only identifies which reviewer must
produce the dedicated acceptance analysis. Neither approval is evidence for the other.

## 9. Mandatory evidence in both reviewer reports

Both reports must include all common evidence:

1. Reviewer identity and explicit non-implementer/non-helper role.
2. Parent source-pass receipt heading/timestamp and exact 64-character final production SHA.
3. BP2 card SHA, final production line/byte count, and review-start/review-end independently recomputed source SHA.
4. Frozen BP2 start SHA `963b028c...` and the parent-provided exact-delta provenance identifier.
5. Whole-file coverage `line 1..N` plus a table covering imports/fields, 19 public methods, formal paths,
   local-session paths, claim paths, maintenance workflow/helpers, and bottom private types.
6. Exact final line anchors for each primary-scope assertion; grep/count alone is insufficient.
7. Confirmation BP1 artifacts remained `a9c34d4e...` / `3b117895...` and were read-only.
8. Confirmation no current/mid-edit WIP identity was used.
9. Confirmation the reviewer did not use the other reviewer report/verdict to reduce scope.
10. Explicit `P0/P1/P2=x/y/z`, source-only verdict, unresolved-item list, and repair/re-review gate.
11. `TEST NOT RUN / BUILD NOT RUN / RUNTIME NOT RUN`: BP2 has `testWriteSet=empty`; the sole
    `TaskMaintenanceTurnContractTest` is a later post-BP3 stable-writer gate.
12. `无已批准业务差异；按 696a12b0 等价迁移`.

R1 additionally records map/access-site index, key-dimension matrix, authority path matrix, all 19 signatures, five
collaborators, six live caller checks, and four dormant caller checks.

R2 additionally records strict-baseline identity, ordered branch table, Summon-gate table, claim
acquire/prune/release/retain table, capability `5/1/5/2`, terminal/ActionState table, and every zero-new surface.

## 10. BLOCKED report format

Use `BLOCKED / REVIEW REQUIRED` whenever there is any P0/P1/P2, missing parent receipt, SHA drift, incomplete
whole-file coverage, insufficient provenance, or unresolved evidence. Missing evidence cannot be converted to
APPROVED.

```text
# TURN-34BP2 independent review R1|R2

Reviewer: <identity; non-implementer>
Parent source receipt: <card heading and timestamp>
Reviewed source: TaskMaintenanceService.java
Parent-received final SHA-256: <64 hex>
Review-start SHA / review-end SHA: <same 64 hex> / <same 64 hex>
Final identity: <N lines / N bytes>
BP2 card SHA-256: <64 hex>
Whole-file coverage: lines 1..<N>, all areas in Section 5 complete
Primary scope: <R1 or R2 frozen scope>
Other-review dependency: none

VERDICT: BLOCKED / REVIEW REQUIRED
P0/P1/P2=<x>/<y>/<z>
SOURCE APPROVAL: NO
TEST/BUILD/RUNTIME: NOT RUN / OUT OF THIS SOURCE REVIEW

Findings
- [P1][R1-01 or R2-01] <short title>
  File/method/line: <exact final-SHA anchor>
  Evidence: <direct source and authority evidence>
  Violated contract: <card/plan/protocol/business rule>
  Runtime impact: <concrete effect>
  Required repair: <bounded direction; no implementation by reviewer>
  Re-review point: new canonical delivery -> parent source pass -> both reviewers restart full-file review

Unresolved evidence: <none or explicit list>
Independence statement: I did not use the other reviewer report or verdict to reduce my scope.
Business difference: 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF REVIEW_COMPLETE
```

Every finding must state severity, final SHA, file/method/line, evidence, impact, repair direction, and the next
acceptance point. A source repair invalidates both R1 and R2 reports, even if the patch appears to touch only one
primary scope. Both reviewers must bind to the newly parent-received SHA and repeat the complete file.

## 11. APPROVED report format

APPROVED is permitted only after direct whole-file completion, stable SHA, all primary evidence, and no unresolved
P0/P1/P2 or repair request. It means one independent BP2 source review passed; it does not mean BP2/TURN-34B is
`CARD APPROVED`, tested, built, or runtime accepted.

```text
# TURN-34BP2 independent review R1|R2

Reviewer: <identity; non-implementer>
Parent source receipt: <card heading and timestamp>
Reviewed source: TaskMaintenanceService.java
Parent-received final SHA-256: <64 hex>
Review-start SHA / review-end SHA: <same 64 hex> / <same 64 hex>
Final identity: <N lines / N bytes>
BP2 card SHA-256: <64 hex>
Whole-file coverage: lines 1..<N>, all areas in Section 5 complete
Primary scope: <R1 or R2 frozen scope>
Other-review dependency: none

VERDICT: APPROVED
P0/P1/P2=0/0/0
SOURCE APPROVAL: YES, FOR THIS INDEPENDENT REVIEW ONLY
TEST/BUILD/RUNTIME: NOT RUN / STILL PENDING OUTSIDE BP2 SOURCE REVIEW

Evidence
- <common evidence index>
- <R1- or R2-specific evidence index with exact final-SHA line anchors>

Unresolved findings: none
Repair requests: none
Independence statement: I did not use the other reviewer report or verdict to reduce my scope.
Business difference: 无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF REVIEW_COMPLETE
```

An APPROVED report with only a finding count, only grep/count output, no full-file coverage, no parent receipt, no
stable final SHA, or no explicit `APPROVED` text is invalid.

## 12. Prohibitions for the future review pass

- Do not review current WIP, a worker claim, a mid-edit hash, or a delivery lacking parent source pass.
- Do not split the final file between reviewers or perform hunk-only/patch-only review.
- Do not rely on the other reviewer, parent verdict, implementation prose, helper prose, tests, or counts as a
  substitute for direct source reading.
- Do not let an implementer, this helper, or the parent count as either independent reviewer.
- Do not dispatch reviewers from this preflight; reviewer assignment belongs to the parent after the activation
  gate.
- Do not modify Java, tests, cards, plan, `ACTIVE_WORK.md`, dashboard, protocol, business logic, or the other
  reviewer's report. Each future reviewer may write only the parent-assigned fixed review report.
- Do not run Maven/JUnit/compile/package or runtime/application/server/Task/UI/capture/input for this source-only
  gate. This preflight also authorizes no Git command.
- Do not use stale `target/classes`, old test results, prior compile output, or runtime anecdotes as source approval.
- Do not create a BP2 test or second test class. BP2 `testWriteSet` is empty; the sole TURN-34B named test remains
  `TaskMaintenanceTurnContractTest` after BP3.
- Do not claim BP2 closes BP3's four per-window maps, fingerprint lifetime, valid-context A -> B -> A cleanup, or
  final TURN-34B test/build gates.
- Do not add or approve a TTL, retry, second observation, cleanup, fail-closed rule, owner/session/lease/ledger,
  queue, durable workflow, or business-order change as a safety improvement.
- Do not write `CARD APPROVED`, `DONE`, or self-approval. Each reviewer may state only its own source-review verdict.
- Do not preserve either approval after any source or card-tail SHA change.

## 13. Parent handoff

After a future parent source pass, the parent should freeze two separate fixed review reports, each with one distinct
non-implementer reviewer and the same parent-received final production SHA. R1 owns typed-key/scope/authority/public
API analysis; R2 owns strict-696 order/claim/terminal-zero-new analysis. Both read the entire final file, produce
non-dependent true-EOF verdicts, and restart together after any repair.

This preflight stops here. It does not observe further WIP, create reviewers, or make a BP2 source judgment.

TRUE_EOF PRECHECK_COMPLETE
