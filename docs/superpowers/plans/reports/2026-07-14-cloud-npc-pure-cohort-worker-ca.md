# Internal CA / W-NPC-PURE-COHORT-IMP1

- Status: `DELIVERED_ZERO_JAVA`
- Claimed at: `2026-07-14T08:59:17.8371310-04:00`
- Role: implementation Worker only; not reviewer
- Unique Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
- Unique report write set: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-npc-pure-cohort-worker-ca.md`
- Git boundary: protect all dirty/untracked work in both repositories; no Git mutation, commit, reset, checkout, clean, revert, or overwrite of others' work

## Baseline

- DHXY: branch `thin-client-design`, HEAD and business baseline
  `0114604e1ff5f15491d2910959c45252e893d04f`.
- Cloud: branch `navigation-migration`, HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`.
- Committed source: `src/main/java/com/bot/dhxy/service/NpcClickService.java`, Git blob
  `cc858482e31ee4a352f59895054452ca28b61d6a`.
- Both worktrees were dirty/untracked before this task and remained protected. No Git mutation was run.
- Required documents were read before admission: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current CR271 top
  entries in `docs/ACTIVE_WORK.md`, the direct Service/InputBundle implementation plan, and the complete migration
  matrix.
- Applicable `docs/业务逻辑.md` contracts checked: `修罗与五倍普通怪共用：入战识别、云端 fallback 与失败上限`,
  the `Alt+A` direct-combat authorization and execution-order section, and `NPC Click 云端 FIFO 候选队列逻辑`.
  These rules retain HWND/capture/input/Ctrl/menu/verifier mechanics locally and prohibit moving post-click dialog
  business into Cloud.

## Admission Result

- Included blocks: none.
- Added Java definitions: `0`.
- Java result: zero-byte change. No source block was partially copied, adapted, wrapped, or given a substitute
  collaborator.
- Decision: every coherent priority cohort reaches a source dependency that is absent from the current target
  fields/types or explicitly prohibited for this Cloud pure cohort. Per task instructions, the result is
  `SOURCE_DEPENDENCY_EXCLUDED` with zero Java rather than an invented seam.

## Source Closure Audit

| Source block | Source anchor | Closure size | Result | Exact dependency boundary |
| --- | ---: | ---: | --- | --- |
| `dialogClickVerifier(String)`, `dialogClickVerifier(NpcClickRequest)`, both `verifyExpectedDialogVisible(...)` overloads, and `verifyExpectedRawDialogVisible(...)` | `0114604e:NpcClickService.java:128` | 5 methods + `NpcClickVerifier` interface | `SOURCE_DEPENDENCY_EXCLUDED` | Requires source field `dialogService`, `DialogService.handleDialog(...)`, and local dialog verification. The Cloud target has no `dialogService` field and no Cloud `DialogService` type. Moving this would also violate the baseline rule that post-click dialog verification stays local. |
| `combatClickVerifier()` and `isCombatVisibleAfterDirectClick(...)` | `0114604e:NpcClickService.java:151` | 2 methods; transitively uses `shouldStop()` | `SOURCE_DEPENDENCY_EXCLUDED` | Requires source field `battleRadarService`, four local combat observations, local `TaskSleep`, and local stop/input scope. The target has no `battleRadarService` field; this cohort may not attach combat observation. |
| `buildNpcClickSmartCloudRequest(...)` | `0114604e:NpcClickService.java:547` | 1 large method + 3 missing passive methods (`prepareNpcClickSmartCloudCaptureScene`, `currentTaskRunId`, `currentWindowBase`) | `SOURCE_DEPENDENCY_EXCLUDED` | Directly requires `tracker.captureToMemory`, `windowScopedTempPath`, `windowTaskContextHolder`, native binding/HWND, task execution context, screenshot encoding/storage, and capture-scene input preparation. Existing pure template/ROI/metadata helpers are already present in the target, but the complete request builder is not pure or closed. |
| `prepareNpcClickSmartCloudCaptureScene(...)` | `0114604e:NpcClickService.java:649` | 1 method, also counted in the request-builder closure | `SOURCE_DEPENDENCY_EXCLUDED` | Executes `Alt+4` and sleep through source field `inputSequences`. Input execution and caller attachment are forbidden in this cohort. |
| `ctrlMenuImageProcessorMetadata(...)` | `0114604e:NpcClickService.java:892` | 1 method | `SOURCE_DEPENDENCY_EXCLUDED` | `ImageProcessorService.RequestMetadata` exists, but the exact block requires source field `windowScopedTempPath` and publishes a local temp-file path. The target has no such field, and a replacement path/parameter would be a non-exact seam. |
| `directCombatAuthorizeCloudRequest(...)` | `0114604e:NpcClickService.java:1287` | 1 method; transitively uses `currentTaskRunId(...)` | `SOURCE_DEPENDENCY_EXCLUDED` | Reads `windowTaskContextHolder`, `WindowRuntimeContext.nativeBinding.hwnd`, and source `TaskExecutionContext.taskRunId:long`. The Cloud target has no holder field; the current Cloud context uses a different typed task-run contract, so parsing/defaulting would change semantics. HWND reads are explicitly forbidden. |
| `tryDirectCombatTargetClick(...)` and the remaining FIFO/session execution chain | `0114604e:NpcClickService.java:217` | operational cohort | `SOURCE_DEPENDENCY_EXCLUDED` | Requires Cloud decision-service wiring plus local input, capture, story-event bus, verifier, stop/pause, and window safety shell. It is not a pure request/result/metadata cohort and cannot be attached to caller/host in this task. |
| `confirmPendingSmartClick(...)` | `0114604e:NpcClickService.java:1342` | 1 method | `COHORT_SIZE_EXCLUDED` | This isolated no-op compatibility hook is compile-ready, but it is neither a coherent request/result/metadata cohort nor the required minimum of three complete methods (or one large method plus value cohort). Copying it alone would repeat the prohibited one-helper slicing. |

Unique priority-closure count, with overlaps counted once: 14 excluded method definitions plus one private functional
interface. The one separately compile-ready compatibility hook was also excluded by the cohort-size rule. No other
missing source method forms a pure closed cohort without entering the operational dependencies listed above.

## Exactness And SHA

- Included block exactness: `N/A` because admission produced no included block.
- Excluded blocks: not copied at all; no partial or normalized implementation was introduced.
- Target Java SHA-256 before audit: `9250D2902B80EDDDEAAC172C14464995432929304E34CC04EEC4B83B6DDF6153`.
- Target Java SHA-256 after compile: `9250D2902B80EDDDEAAC172C14464995432929304E34CC04EEC4B83B6DDF6153`.
- SHA equality: `true`.
- Added/changed/removed Java definitions: `0 / 0 / 0`.

## Compile Gate

- Command: `mvn -q compile`
- Working directory: `D:\mavenProject\dhxy-cloud-brain`
- Exit code: `0`
- Output: empty Maven quiet output; compilation completed successfully.
- `clean` was not run. No tests, application, server, host, Task, poller, UI, capture, template/OCR, or input path was
  run.

## Delivery

- Java: zero change by the explicit all-candidates-nonclosing fallback.
- Report: this file only.
- Intentional business differences: none. `无已批准业务差异；按 0114604e 基线等价迁移`.

## Parent Review #1 - 2026-07-14T09:18:00-04:00

**ACCEPTED_ZERO_JAVA，P0/P1/P2=0。** 父级确认 Java SHA before/after 相同、compile exit 0、无越写集。
该结果只证明上一父级“纯 cohort”合同无法形成闭包，不算迁移成果；任务关闭，后续不再派排除型任务。
