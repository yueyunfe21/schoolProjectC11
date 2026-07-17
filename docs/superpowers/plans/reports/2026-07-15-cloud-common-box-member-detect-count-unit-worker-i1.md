# Cloud CommonBox Member Detect Count Unit Worker I1

## CLAIMED

- task: `W-COUNT-COMMON-BOX-MEMBER-DETECT-1`
- role: Internal Count Worker I1, implementation only; not a reviewer
- claimedAt: `2026-07-15T01:15:07-04:00`
- countUnit: `CommonBoxService::detectMemberBoxAfterCombatExit`
- countDelta: `+1`
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- gate: only parent source review plus the parent's unified fresh DHXY/Cloud build may apply the delta

## Result

`NO_CODE_CHANGE / COMPLETE MEMBER DETECTION SOURCE CHAIN DELIVERED`。

Current Cloud already closes the exact count unit from the real combat-exit caller through the existing dedicated
CommonBox observation port to the DHXY exact-window mechanics and back to a closed pending mutation. I1 found no
method-level gap inside the authorized write set and therefore did not create another protocol, adapter, or Java diff.

## Real Caller

Active Cloud `AutoCombatService.consumeExitAndRecover` reaches the count unit at line 366 only after a trusted combat
exit signal has been consumed and the existing combat-exit bookkeeping has run:

`consumeCombatExitSignal* -> recordCombatExit/resetCheckCounter -> log exit ->
commonBoxService.detectMemberBoxAfterCombatExit(context, safeRequestedTaskCode(context), source + ":combat-exit")`.

The caller passes the existing exact `TaskExecutionContext`, requested task code, and combat-exit diagnostic source.
It is frozen and unchanged by I1; current SHA-256 is
`80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D`.

## 696 Branch And Mutation Map

| Decision/mutation | Active Cloud behavior |
|---|---|
| public member entry | `detectMemberBoxAfterCombatExit` delegates once to `detectBox(..., CommonBoxRole.MEMBER, source)` |
| expiry cleanup | existing pending entries are pruned before the new detection gates |
| supported task | only normalized `xiuluo_v2` / `wubei` continue |
| exact run/window | context must be non-null, have a window, and produce a non-null taskRun key |
| member role switch | `memberCommonBoxEnabled` remains the independent baseline switch, default false; switch-off clears MEMBER pending only |
| role fence | unknown role or a non-MEMBER context removes only the requested member key and skips observation |
| stop fence | a requested stop before observation returns without mutation |
| local observation | one `CloudCommonBoxPort.observe` call uses stable slot `member-detect` and typed `COMMON_BOX` |
| matched mutation | only `MATCHED` writes/replaces one pending entry keyed by window/native handle/role/task/taskRun |
| TTL | `detectedAt=matchedAtEpochMs`; `expiresAt=matchedAtEpochMs + 30_000`, preserving the DHXY-local match-time anchor |
| identity/source | pending stores current player identity epoch and the caller's `source + ":combat-exit"` diagnostic source |
| negative result | NOT_MATCHED/capture unavailable/template unavailable/mechanics failed/NOT_EXECUTED logs only; it does not create or clear a valid existing pending entry |
| stop/unknown terminal | STOPPED rechecks `TaskCheckpoint`; unconfirmed STOPPED and UNKNOWN are fatal, never a match or pending mutation |

The role switch is intentionally not bypassed. Member handling remains default-off exactly as the approved baseline;
when enabled for the member role, this same caller and typed chain performs the observation.

## Typed Observation Chain

1. `CommonBoxService.detectAndRecord` calls the injected `CloudCommonBoxPort` once with the caller's exact context,
   phase `common-box`, action slot `member-detect`, and the existing 120-second transport timeout.
2. The unique `CloudCommonBoxPortAssembly` reads `WindowFactKind.COMMON_BOX`. Production source reachability is
   present through `CloudServiceHost -> CloudServiceConfiguration`, whose current `@Import` contains
   `BotProperties.class` and `CloudCommonBoxPortAssembly.class`.
3. DHXY `LocalRemoteGameCommandHandler` resolves the exact registration/binding and executes the existing
   `CommonBoxLocalObservationMechanics.observe(access.binding())` under that exact window context.
4. The mechanics captures only window-relative ROI `(623,590)-(682,618)`, loads
   `images/template/common/leader_box_marker.png`, and matches at threshold `0.86`. It sends no input.
5. `MATCHED` carries window-client point, score, and the DHXY-local `matchedAtEpochMs`; the handler converts the point
   to `SCREEN_ABSOLUTE_PX` using the same binding origin. The closed fact/result preserves all five mechanics states.
6. Back in Cloud, only the closed `MATCHED` result creates pending. All other mechanics/transport terminals follow
   the table above, so failure is never disguised as a box and no duplicate observation or fallback is introduced.

## Closed Result Matrix

| Result | Pending effect |
|---|---|
| `MATCHED` | put one member pending with exact window/run/identity/source and 30-second expiry |
| `NOT_MATCHED` | no mutation |
| `CAPTURE_UNAVAILABLE` | no mutation |
| `TEMPLATE_UNAVAILABLE` | no mutation |
| `MECHANICS_FAILED` | no mutation |
| `NOT_EXECUTED` | no mutation |
| `STOPPED` | cooperative stop or fatal if stop is not confirmed; no pending write |
| `UNKNOWN` | fatal; no pending write |

The public API returns `void`; its closed business result is therefore exactly one of: a validated member pending put,
a closed no-mutation terminal, or stop/fatal unwind. It does not click or consume the pending in this count unit.

## File Table

| Repository | File | I1 action | Current SHA-256 |
|---|---|---|---|
| Cloud | `src/main/java/com/bot/dhxy/service/CommonBoxService.java` | read-only; count-unit implementation already complete | `5F3FFB1E8DED18035220B7A216DC845AF36E893FB62DC851775EC76D339D1F5B` |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPort.java` | read-only existing dedicated contract | `1F4598BF0230B0A96F5EA0E185D4952BF58095DC9901EBAC40C1AFEE5D671B27` |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudCommonBoxPortAssembly.java` | read-only existing assembly | `B9AE9555E5CA562CFCFD29BFF7F8BA81E97E6AF0C85F005007202A3B61F059FC` |
| Cloud | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CommonBoxObservationResult.java` | read-only existing closed result | `3F30C8D55D7577FEB48DE128D010113DA0B10FFC0172C77B87214B91C1AE4E4E` |
| Cloud | `src/main/java/com/bot/dhxy/service/AutoCombatService.java` | frozen real caller | `80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D` |
| DHXY | CommonBox mechanics/fact/handler | read-only existing exact-window terminal | unchanged by I1 |
| DHXY | `docs/superpowers/plans/reports/2026-07-15-cloud-common-box-member-detect-count-unit-worker-i1.md` | new | this report |

## Scope And Gate

- No Java file was edited. Caller, DHXY, generic shared transport, all other Services, leader-only caller paths, click,
  consume, and host/config were frozen and remain unchanged.
- No duplicate CommonBox protocol, helper, wrapper, pending owner, TTL, retry, cleanup, fallback, or observation was added.
- No Maven, javac, test, runtime/application/server/host, Task/poller, UI/capture/input, or Git command was run.
- `countDelta=+1` is claimed only. I1 does not update the ledger or issue a reviewer judgment.
- Status: `NO_CODE_CHANGE DELIVERED / COUNT PENDING PARENT SOURCE REVIEW AND UNIFIED FRESH BUILD`.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。父级源码审查与统一双构建通过前不真正计数。**

## Parent Source Review #1 / Next Count Task - 2026-07-15T01:27:00-04:00

父级独立复核 real caller、完整 gate 顺序、typed observation 与 pending mutation：`AutoCombatService:366` 在可信
combat-exit 后调用 member entry；task/window/run/role/switch/stop gates、单次 `COMMON_BOX` observation、五态 terminal、
MATCHED-only pending put、DHXY-local `matchedAt + 30s` 时间锚和 exact binding 均闭合。没有把 negative terminal
折成 match，也没有新增观察、TTL 或 retry。结论：
**P0=0/P1=0/P2=0，NO_CODE_CHANGE SOURCE APPROVED / COUNT PENDING BUILD。**
`countUnit=CommonBoxService::detectMemberBoxAfterCombatExit` 仅在统一 fresh build 通过当轮 `+1`。

下一任务另记固定报告 `docs/superpowers/plans/reports/2026-07-15-cloud-map-name-canonicalizer-count-unit-worker-i1.md`：
`W-COUNT-MAP-NAME-CANONICALIZER-WHOLE-1`，`countUnit=MapNameCanonicalizer::canonicalize`，
`countDelta=+1`。一次闭合真实 `NavigationService/TaskTrackerPanelService typed local map-text result -> Cloud
MapNameCanonicalizer exact/fuzzy/ambiguity algorithm -> original caller branch -> closed navigation/tracker terminal`；以
`696a12b0` 的 maps authority、distance/runner-up threshold、ambiguous 返回原文语义为权威。唯一 Java 写集 Cloud
`MapNameCanonicalizer.java` + 当前唯一既有 map-name resource（仅精确缺口时）+ 新报告；Navigation/Tracker caller、
DHXY、shared、其它 Service 冻结。不得读取 DHXY 本地模板目录、造 helper/第二资源或改阈值。父级源码审查 + fresh
build 同轮才 `+1`。
