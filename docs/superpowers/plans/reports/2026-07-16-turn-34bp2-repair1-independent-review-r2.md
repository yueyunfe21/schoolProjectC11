# CR271 TURN-34BP2 Repair #1 independent whole-card review R2

## Review boundary

- Role: independent whole-card reviewer R2. This review covers the complete existing TURN-34BP2 Repair #1; it does not implement Java, expand the contract, split the card, or replace parent judgment.
- Frozen production artifact: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`, 1,365 lines, SHA-256 `d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219`.
- Baseline: `D:/mavenProject/dhxy-cloud-brain/migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`, 1,123 lines, SHA-256 `4beaffd08314f694b41a841dff236c4ce00dc335cbe75de74a9f667a53803eda`.
- Read in full: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top CR271 entry in `docs/ACTIVE_WORK.md`, authoritative plan Sections 14-19, the HTTPS turn protocol, `docs/业务逻辑.md`, and the TURN-34BP2 card through its current true EOF. Both repository statuses were inspected and all existing dirty/untracked files were protected.

## Verdict

**BLOCKED**

`P0/P1/P2 = 0/1/0`.

### P1 - Formal coordination keys still merge no-session windows in the same device scope

The frozen card requires the typed coordination address to encode the execution scope plus either an explicit local-team session or the exact window, so that same-scope windows share only through an explicit session and no-session windows remain isolated. The delivered formal-team key does not encode either discriminator:

- `TaskMaintenanceService.java:59-65` correctly converts the four shared maps to typed declarations, but `activeTeamRoundByKey`, `teamMaintenanceWindowStateByRound`, and the formal branch of `summonSkillClaimsByTeamRound` all ultimately use `ScopedTeamKey`/`TeamRoundKey`.
- `TaskMaintenanceService.java:1260-1262` constructs `ScopedTeamKey` from only `executionScope(context)` and the normalized maintenance key.
- `TaskMaintenanceService.java:1329-1335` defines `ScopedTeamKey(ExecutionScope scope, String maintenanceKey)` and `TeamRoundKey(ScopedTeamKey team, int round)`. Neither record contains `windowId`, a scoped explicit session, or a typed session-vs-window coordination discriminator.
- `TaskMaintenanceService.java:98-102`, `119-123`, `152-156`, `177-180`, `230-236`, and `577-586` therefore read and write the same active round/window-state address for two windows that have the same tenant/user/device, maintenance key, and round, even when neither context has a local-team session.
- `TaskMaintenanceService.java:1163-1171` wraps that same address in `FormalTeamRoundClaimKey`; `:747-765` then applies one shared claim set and `maxClaims` limit. `ScopedWindowKey` distinguishes claim members, but it does not isolate the containing formal claim namespace. One standalone window can therefore consume or block another standalone window's formal maintenance slot.

Concrete consequence: contexts `(tenant=T,user=U,device=D,window=A,session=null)` and `(T,U,D,B,null)` both resolve the formal key to `(ExecutionScope(T,U,D), maintenanceKey, round)`. A's `begin/open/close` changes B's observed round/window state, and A's claim counts against B's `maxClaims`. This violates the card's exact `same scope + explicit session may share / without a session windows isolate` contract and is not an allowed `696a12b0` business difference.

## Passed static checks

- Four required shared maps are typed at `:59-65`; formal and local claims are separated by the sealed typed variants at `:1343-1355`. No delimiter parsing, prefix compatibility lookup, `local-team:` alias, `substring`, or `Integer.parseInt` remains in this file.
- The four BP3 per-window maps remain String-keyed at `:55-58`; `currentWindowKey`, scope-prefix, identity fingerprint/generation/cache code remains in the BP3 area at `:1034-1143` and was not used to disguise the missing BP2 coordination discriminator.
- Exactly 19 public methods remain (`:73-597`), with the frozen signatures intact.
- Supplied context precedence is explicit at `:1017-1026`. `ExecutionScope.NONE` has exactly one return at `:1244`, only after `effectiveContext` returns null; non-null context authority access is not broad-caught, and null scope/invocation fails closed at `:1246-1255`.
- Explicit local-session state is typed and scope-bound at `:1174-1195`, `:1210-1225`, and `:1362-1364`; different tenant/user/device scopes do not share that local-session map.
- Against `696a12b0`, the maintenance order remains normalize -> first checkpoint -> optional broadcast -> handled/failure/interrupted short-circuit -> at most one Summon delegate -> no-action (`:597-615`). CommonBox/TeamReturn capability open-close sets remain `5/1/5/2` at `:124-128`, `:157`, `:181-186`, `:201-202`, and `:215-217`. Summon gate order, static-tail/UNKNOWN handling, claim retain/release behavior, `GameContext.ActionState` restoration, delegate count, and terminal propagation remain structurally unchanged at `:643-826`.
- No UUID generation, command/action emission, retry/replay/resend, new sleep/timer/TTL, owner/lease/ledger/queue, durable workflow, or success fabrication was found in the BP2 implementation.

## Whole-card repair condition

Return the complete TURN-34BP2 card to the same implementation owner. Repair the typed formal coordination address so every shared formal map decision includes exactly one typed coordination discriminator after `ExecutionScope`: use the scoped explicit local-team session when one is present; otherwise use the exact scoped window identity. Apply that same address consistently to active rounds, maintenance-window state, formal round claims, pruning, and all corresponding reads/writes.

The repair must prove all four combinations: same scope + same explicit session shares; same scope + no session + different windows isolates; different scope never shares; formal and local claim kinds cannot collide. Preserve the accepted four map types, four BP3 maps, 19 public signatures, supplied-context precedence and fail-closed authority path, maintenance-key fallback order, business sequence, terminal behavior, UUID/delegate/retry counts, and zero string parsing/compatibility fallback. Do not add a global key, dual lookup, session authority, owner, lease, ledger, TTL, queue, durable workflow, or any approved business difference.

## Unrun gates

Per reviewer instructions, no Maven, JUnit, compile, package, runtime, application/server/Task/UI, capture, or input command was run. No Git mutation was performed. Compile/test/runtime status is not claimed by this report and remains a later gate.

<!-- TRUE_EOF: CR271 TURN-34BP2 REPAIR-1 INDEPENDENT-WHOLE-CARD-REVIEW-R2 BLOCKED P0-0-P1-1-P2-0 NO-SESSION-WINDOW-ISOLATION-MISSING FROZEN-SHA=d97e1572b23713cd0ba9414be0ce6b402b3eb36ab002d5a044a6183edc336219 NO-MAVEN-JUNIT-COMPILE-RUNTIME-INPUT-GIT-MUTATION 2026-07-16T14:17:26-04:00 -->
