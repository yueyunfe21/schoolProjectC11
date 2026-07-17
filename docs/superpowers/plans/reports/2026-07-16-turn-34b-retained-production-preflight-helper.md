# CR271 TURN-34B Retained-Production Parent-Review Preflight Helper

- Role: `CR271 TURN-34B retained-production parent-review preflight helper` only.
- This report is not an implementation delivery, reviewer decision, parent adjudication, approval, or card closure.
- Snapshot cutoff: `2026-07-16T09:08:19.9935594-04:00`.
- Sole write set: this report.
- No Java, original/child card, plan, `ACTIVE_WORK`, dashboard, protocol, business document, or other file was modified.
- No Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git mutation was performed.

Any `SOURCE_MATCH`, `PARTIAL`, or `SOURCE_GAP` wording below is a static precheck label for the parent. It is not a
P0/P1/P2 ruling and does not approve or reject TURN-34B.

## 1. Authority and current physical state

Read for this pass:

- `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the top CR271 section of `docs/ACTIVE_WORK.md`;
- Sections 14-19 of
  `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`;
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` and the applicable constraints in
  `docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md`;
- `docs/业务逻辑.md`, including local-team capability boundaries, CommonBox priority, summon-skill static boundary,
  STOP, and the `696a12b0` baseline rule;
- TURN-34B original card through its latest physical true EOF, including External D's `OWNER RETURNED`;
- External D's lane report through its latest physical true EOF;
- the newly parent-frozen TURN-34BT1 child card, because it is now the current consumer of the retained production;
- current Cloud `TaskMaintenanceService.java`, current parent-reviewed TURN-34A `AutoCombatService.java` six-API
  caller, current `TaskExecutionContext` exact-metadata path, and baseline
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:TaskMaintenanceService.java`.

Current state at cutoff:

| Fact | Evidence |
|---|---|
| Original TURN-34B owner | External D returned owner at `2026-07-16T08:48:00-04:00`; `delivery=NONE` |
| Original card latest true EOF | Parent decomposition to TURN-34BT1 at `2026-07-16T08:59:40.918-04:00`; this is newer than the owner-return marker |
| Retained production | 1,224 lines, 66,012 bytes, mtime `2026-07-16T08:17:40.6760891-04:00`, SHA-256 `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC` |
| Initial pre-D production | 1,130 lines, SHA-256 `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996` |
| `696a12b0` baseline blob | `e93cfd01d9c282f98881a6311b8bb806bbc3e359` |
| Sole named test | `TaskMaintenanceTurnContractTest.java` is physically absent |
| TURN-34BT1 | `READY / CLAIM REQUIRED / PRODUCTION PRESERVED`; production is read-only and External D had not claimed the child card at cutoff |
| TURN-34A caller | Current `AutoCombatService.java` SHA-256 `532E6F840E0847381DE2CEF68153CBCAC563B11BD5DE9CCDFD0570C6B84AA6E9`, equal to its parent-reviewed production SHA |

The original card and External D both state that the 1,224-line file is retained WIP, not delivered source. The
latest parent true EOF preserves those bytes and splits the next work into a test-only tranche; it does not convert
the retained bytes into reviewed or approved production.

## 2. Item-by-item static precheck

| Check | Static label | Preflight result |
|---|---|---|
| Exact-context identity token | `PARTIAL` | Supplied context precedence and an atomic native fingerprint replacement exist. The token is built from initial context metadata, not compared with latest metadata; HWND/process/title drift is not stopped before delegates. |
| Tenant/user/device/window scoping | `SOURCE_GAP` | Four per-window Summon maps use the new scoped window key, but formal team-round/window/claim maps and local-team session state still use raw task/session keys. |
| `A -> B -> A` | `SOURCE_GAP` | Summon layout/cache state is replaced for a newly supplied fingerprint, but team-round claims and local-session state survive; live metadata drift inside one context is not observed by the token. |
| turn-native avoids legacy epoch | `SOURCE_MATCH`, test missing | With the current final `TaskExecutionContext` invariant, turn-native `getNativeWindowTitle()` returns validated initial metadata and does not enter the epoch catch. The sole epoch reference remains the legacy fallback. No named test locks this yet. |
| 19 public APIs | `SOURCE_MATCH` | Baseline/current extraction is exactly `19/19`; names, parameter types, return types, and declaration shapes match. |
| TURN-34A six APIs | `SOURCE_MATCH`, test missing | Current parent-reviewed AutoCombat call sites still bind directly to all six unchanged APIs. There is no wrapper or caller edit. No TaskMaintenance named test or compile evidence exists. |
| Maintenance/Summon/team baseline semantics | `SOURCE_MATCH` within one scope | Baseline method-body comparison is exact except the intended `summonSkillState(windowKey, context)` plumbing. Broadcast priority, Summon gates/delegate count, capability sets, and result projection remain baseline source. |
| No added retry/session/TTL | `SOURCE_MATCH` for no-new rule | D's delta adds no transport retry, automatic retry, owner, session, lease, ledger, TTL, queue, scheduler, or durable workflow. Existing baseline unknown-failure interval, two 2-hour caches, and local-team session remain present and are not new. |
| Named test/build evidence | `EVIDENCE_MISSING` | The named test is absent and no Maven/compile command has run for this retained production. |

## 3. Exact-context identity token

Positive source facts:

1. `effectiveContext(...)` at current lines `993-998` returns an explicitly supplied context before reading
   `TaskExecutionContextHolder`. A wrong holder therefore does not override the supplied context in the new key/token
   helpers.
2. `currentWindowKey(...)` at `1006-1013` uses tenant, user, device, and window when `scopePrefix(...)` succeeds.
3. `currentIdentityToken(...)` at `1068-1082` includes scope, window, native handle, process id, and title/epoch tail.
4. `summonSkillState(...)` at `1033-1054` replaces the state in one `ConcurrentHashMap.compute(...)` and clears the
   existing cooldown/not-due/unknown-interval maps when the token changes.

Unclosed source facts:

1. `runOpportunisticMaintenance(...)` checkpoints at `584`, but identity-token evaluation happens only after the
   broadcast branch, inside `maybeCleanSummonSkill(...)` at `652`. A broadcast-enabled request can therefore call
   `DialogService` at `605-607` without evaluating the new native fingerprint at all.
2. The current turn-native checkpoint reads latest metadata, but `TaskExecutionContext.latestExactTurnMetadata()`
   only rejects missing metadata and device/window mismatch. It does not compare latest title, native handle, or
   process id with the context's initial exact binding.
3. `currentIdentityToken(...)` reads `getNativeWindowTitle/Handle/ProcessId` from the immutable initial context. It
   never reads the latest metadata. A live same-device/same-window HWND, process, or title drift leaves the token
   unchanged and the following Dialog/Summon delegate remains reachable.
4. When a newly supplied context carries a different native fingerprint, the current behavior invalidates cache and
   continues. The frozen card instead requires missing metadata or device/window/HWND/process/title drift to stop
   before Dialog/Summon with delegate/action/UUID count zero.
5. `scopePrefix(...)`, `nativeHandleOrNull(...)`, `nativeProcessIdOrNull(...)`, and `identityTail(...)` catch broad
   runtime failures and fall back to a bare key, null component, or `identity:unavailable`. For malformed or missing
   turn-native authority this is fail-open key degradation, not the frozen pre-delegate stop.

The retained implementation therefore provides a useful cache fingerprint, but it is not yet the complete
exact-current-metadata fence required by the original card and TURN-34BT1 lines 26-31.

## 4. Scope isolation matrix

| State in `TaskMaintenanceService` | Current key | Scope result |
|---|---|---|
| `lastSummonSkillCleanAtByWindow` | `tenant|user|device|window` when scope lookup succeeds | Scoped, subject to fallback/collision risks |
| `lastSummonSkillNotDueLogAtByWindow` | same scoped window key | Scoped, subject to fallback/collision risks |
| `summonSkillUnknownRetryAfterByWindow` | same scoped window key | Scoped, subject to fallback/collision risks |
| `summonSkillStateByWindow` | same scoped window key plus stored native token | Scoped per logical window; native drift behavior remains partial |
| `activeTeamRoundByKey` | raw `teamMaintenanceKey` or requested/task code | Not tenant/user/device/window scoped |
| `teamMaintenanceWindowStateByRound` | raw `teamKey#round` | Not tenant/user/device/window scoped |
| `summonSkillClaimsByTeamRound` outer key | raw `teamKey#round`, or raw `localTeamSessionKey#capability#epoch` | Not fully scoped; claim values alone use scoped `windowKey` |
| `localTeamSessions` | raw `localTeamSessionKey` | Not tenant/user/device/window scoped |

Concrete cross-scope consequence: two contexts in different tenant/user/device scopes that both use task key
`wubei` and round `1` address the same `activeTeamRoundByKey["wubei"]` and
`teamMaintenanceWindowStateByRound["wubei#1"]`. They also share the outer max-claim set for `wubei#1`. Equal local
session keys likewise share leader/candidate/capability state across scopes.

There are two additional exact-key risks:

- `scopePrefix` and `currentIdentityToken` concatenate unconstrained identifiers with `|`. `CloudServiceScope` and
  `TurnInvocationContext` require nonblank text but do not prohibit that delimiter, so distinct tuples can produce
  the same string key. TURN-34A's reviewed state owner uses typed `LogicalStateKey` and `NativeFingerprint` records;
  the retained TaskMaintenance key is not equivalently collision-free.
- Broad exception fallback from a scoped key to bare `windowId` can merge otherwise separate scopes precisely when
  authority access is unhealthy.

## 5. `A -> B -> A` and legacy epoch path

### Native rebind sequence

For newly supplied contexts with the same logical scope/window and different initial title/HWND/process:

1. A creates one `SummonSkillWindowState`.
2. B changes the token and atomically replaces that state; A's layout/cache object is no longer retained.
3. A again changes the token and replaces B's state; the original A object is not revived.

That closes only the per-window Summon layout/cooldown portion. It does not clear:

- the raw formal team round/window entries;
- the outer team-round claim set;
- a claim made by the same scoped `windowKey`;
- raw local-session leader/capability/epoch state.

Thus B can inherit A's one-per-round claim, and the returned A can still encounter that stale claim. In addition,
an A-to-B live metadata change inside the same `TaskExecutionContext` is invisible because the token is made from
initial metadata.

### turn-native versus legacy epoch

`identityTail(...)` first calls `getNativeWindowTitle()` and calls `getPlayerIdentityEpoch()` only in the catch.
Current `TaskExecutionContext` is final; its validated turn-native constructor stores a nonblank initial title, and
`getNativeWindowTitle()` returns that title directly for turn-native while deliberately throwing only for the legacy
path. Under those current class invariants, the ordinary turn-native path does not touch the legacy epoch API.

This is source-level compatibility evidence only. It is exception-driven path discrimination rather than an explicit
public mode branch, and the required production-path named test is absent. The parent should require the child test
to prove the invariant without private reflection or a fake result standing in for the real service call.

## 6. Public API and TURN-34A compatibility

Read-only extraction found exactly 19 public method signatures in both the `696a12b0` baseline and current retained
production, with no missing, added, or changed signature. All 19 public method bodies are baseline-exact; the only
maintenance-path source adjustment outside the new private helpers is the intended context argument passed into
`summonSkillState`.

The six APIs consumed by current parent-reviewed TURN-34A remain:

| Frozen API | Current AutoCombat call sites |
|---|---|
| `isPendingLocalSupportLeaderDetection` | `485`, `535`, `654` |
| `isLocalSupportMemberSession` | `492`, `523`, `645` |
| `isLocalTeamSupportCapabilityOpen` | `493`, `646` |
| `awaitLocalTeamSupportCapabilityOpen` | `524` |
| `isLocalSupportMemberCandidate` | `541` |
| `awaitTeamFirstAidMaintenanceWindowOpen` | `544` |

The six TaskMaintenance method bodies remain byte-equivalent to baseline behavior, and they do not dereference
`DialogService` or `SummonSkillService`. Static source compatibility is therefore present. Contract-test compilation,
timeout/interruption assertions, and cross-scope semantic acceptance remain unproven because the named test is absent
and this helper was forbidden to run Maven.

## 7. Maintenance, Summon, and team semantics

Read-only method-body comparison against `696a12b0` found the following production behavior unchanged:

1. `runOpportunisticMaintenance`: checkpoint, maintenance broadcast, handled/failed/interrupted short-circuit,
   optional Summon, then no-action.
2. Broadcast `BUSINESS_OPTION_CLICKED`, `FAILED`, and `INTERRUPTED` all return before Summon.
3. Summon gate order remains feature, positive interval, FREE, due, existing unknown-failure interval, existing
   2-hour tail-safe/count cache, team/capability/pathing, duplicate/max claim, then checkpoint before action.
4. There is exactly one source call to
   `summonSkillService.cleanSummonSkillsOnce(SummonSkillCleanupRequest)` and no copied TURN-33 static-tail,
   five-delete, PNG/OCR/click/action/UUID loop.
5. Pathing open remains exactly `FIRST_AID/PATHING_WINDOW/COMMON_BOX/SUMMON_SKILL/LEFT_TOP_STATUS`.
6. Weak first-aid open remains exactly `FIRST_AID`; team close remains the same five; return support remains exactly
   `TEAM_RETURN+COMMON_BOX` open/close.
7. There are zero `TeamReturnService` references and zero `UUID/actionId` references in this service. It maintains
   capability state only and performs no CommonBox or TeamReturn mechanics.

These are static one-scope baseline matches. They do not cure the cross-scope key and live-drift gaps above.

## 8. No-new retry/session/TTL check

D's retained production delta adds context resolution, scope/fingerprint construction, one identity field rename, and
cache invalidation plumbing. It adds no executor, timer, background worker, transport call, second command, automatic
retry, owner, lease, ledger, session, TTL, compaction, or durable workflow.

The file still contains the following pre-existing baseline concepts, which must not be misreported as newly added:

- `summonSkillUnknownRetryAfterByWindow` and `SUMMON_SKILL_FAILED_RETRY_LATER` are the existing unknown-failure
  interval/result projection; no code automatically re-invokes the delegate.
- `SUMMON_SKILL_TAIL_SAFE_CACHE_TTL_MS` and `SUMMON_SKILL_COUNT_CACHE_TTL_MS` are the approved existing 2-hour caches.
- `localTeamSessions` is the existing UI-started business session state from the baseline and business document.

Therefore the correct preflight statement is `no newly added retry/session/TTL`, not literal zero occurrences of those
words or concepts.

## 9. Parent receipt risks

These are risk candidates for the parent, not reviewer findings or approval decisions:

1. **Retained WIP has no delivery status.** External D explicitly returned `delivery=NONE`; the current source has no
   owner and has not passed parent source review. Preserving SHA `963B028C...` is not acceptance.
2. **TURN-34BT1 is test-only while retained production has test-detectable source gaps.** A faithful tranche-1 test for
   same task/round cross-scope isolation, latest HWND/process/title drift before delegate, and claim-aware A-to-B-to-A
   will expose the gaps above. A test-only worker cannot close them while production remains read-only. Narrowing the
   assertions, using source scans/private reflection, or substituting fake results would violate the child card.
3. **Formal team state can cross tenants/users/devices/windows.** Raw task/round keys can share open/close state and
   max-claim budget between otherwise exact contexts.
4. **Local-team capability state can cross scopes.** Raw session keys can share leader detection, capability epoch,
   capability open/close, and completion state.
5. **Live native drift is not a pre-delegate fence.** Missing latest metadata and device/window mismatch stop, but
   latest title/HWND/process mismatch does not. Broadcast can execute before the identity token is even evaluated.
6. **A-to-B-to-A is only partially cleared.** Per-window Summon cache is replaced; formal/local claims and capability
   state can survive.
7. **String key construction is not exact tuple identity.** Unescaped delimiters and broad fallback can collide or
   degrade to a bare key.
8. **Evidence gates are still open.** The named test is absent; no TaskMaintenance test/compile evidence exists;
   TURN-22 final source/integration, TURN-34A final compatibility, two independent reviews, named test, and Cloud
   compile/build remain pending under the original card.
9. **Top-level status text is older than the card EOF.** `ACTIVE_WORK` top and plan registration still describe D as
   TURN-34B source-start next, while the original card now records owner return and parent decomposition. Parent
   scheduling should use the latest card true EOF, not the older summary text.

## 10. Parent handoff index

Without making an implementation or approval decision, the evidence supports this parent review order:

1. Freeze the retained production at SHA `963B028C...` until a newly claimed source-repair write set exists; do not
   treat TURN-34BT1 test creation as implicit source acceptance.
2. Require the tranche-1 test to express the frozen exact-context/scoping/A-to-B-to-A cases honestly against the real
   public service. If those cases fail by source inspection or later execution, reopen the one production file through
   a parent-frozen repair card rather than weakening the test.
3. Recheck every map/key, latest metadata fence, supplied-context precedence, and claim cleanup after any repair.
4. Only then continue the later broadcast/Summon/team tranches and the original parent/reviewer/test/build gates.

No approved business difference was found or authorized by this helper. Baseline maintenance decisions remain
`696a12b0`; the indexed gaps are exact-context ownership/scoping defects in the retained migration plumbing, not a
request to change maintenance, Summon, team-window, retry, cache, or task business semantics.

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-34B RETAINED-PRODUCTION PARENT-REVIEW PREFLIGHT HELPER PRECHECK_COMPLETE NON-REVIEWER NON-IMPLEMENTER NON-PARENT-APPROVAL SNAPSHOT=2026-07-16T09:08:19.9935594-04:00 PROD_SHA256=963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC NAMED_TEST=ABSENT SOURCE_GAPS_INDEXED -->
