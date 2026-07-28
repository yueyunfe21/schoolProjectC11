# CR274 Cloud OCR Lifecycle and Start Failure Propagation

## Status

`SOURCE REVIEW COMPLETE / P0-P1-P2=0-0-0 / CLIENT+CLOUD COMPILE 0 / FRESH RUNTIME REQUIRED`

## Incident

- Client runtime: `D:\mavenProject\DHXY-cr271`, 2026-07-25 09:44-09:48.
- Cloud runtime: `D:\mavenProject\dhxy-cloud-brain`, port `18080`.
- OCR endpoint `127.0.0.1:18761` had no listener and no Python process.
- The supplied representative tooltip mask was valid and visibly contained leader ID `67555`.
- Cloud emitted `Cloud team-role tooltip OCR unavailable`, resolved all five windows as `UNKNOWN`,
  then produced `SKIPPED`.
- Client treated `SKIPPED` as a generic failure. Four empty-effective-queue loops remained busy until
  the 60-second terminal wait completed, keeping settings/task editing locked.

## Approved Contract

1. OCR remains a Python implementation detail but is lifecycle-owned by Cloud Brain.
2. `CloudBrainApplication` ensures an identity-valid loopback sidecar before opening the Cloud HTTP
   listener. A sidecar owned by this Cloud process is closed with it; an already-running matching
   sidecar is reused without ownership.
3. Missing Python/runtime/script, identity conflict, early exit, or health timeout fails Cloud startup
   before it accepts any task.
4. A role-preflight result without a usable leader ID rejects the task start before ACK with an exact,
   user-readable reason. It must not install a run or asynchronously collapse to `SKIPPED`.
5. Client start rejection immediately terminates the exact loop, releases busy/settings locks, and
   preserves Cloud problem `code/detail` in runtime/UI diagnostics.
6. No tooltip ROI, OCR selection, role assignment, task phase, input, navigation, or fallback behavior
   changes are authorized.

## Write Set

### Cloud

- `CloudBrainApplication`
- one Cloud-owned OCR lifecycle component
- `CloudTeamRolePreflightService` typed resolution/result
- `CloudTurnTaskRuntime` pre-ACK rejection
- related HTTP problem propagation only as needed

### Client

- HTTPS turn problem parsing
- `WindowTurnLoop` start-rejection terminal handling
- runtime/UI failure-detail projection only as needed

## Verification

- Mandatory Client and Cloud Java compile.
- Client `mvn -q -DskipTests compile`: exit `0`.
- Cloud `mvn -q -DskipTests=false compile`: exit `0`; Cloud enforcer rejects
  `skipTests=true`, while the `compile` lifecycle itself does not execute tests.
- Parent source review: `P0/P1/P2=0/0/0`.
- No local tests were created or run under the current user-approved no-local-test mode.
- No runtime/UI/capture/input is run by Codex.
- Fresh user gate:
  - IntelliJ Cloud main launch starts and health-gates OCR automatically;
  - owned OCR exits with Cloud, matching reused OCR is not killed;
  - unavailable OCR prevents Cloud listener readiness;
  - task rejection is immediate, names the OCR/role cause, and leaves no 60-second busy window.

## Baseline

- Client branch `thin-client-design`; authority worktree `D:\mavenProject\DHXY-cr271`.
- Cloud branch `navigation-migration`; authority worktree `D:\mavenProject\dhxy-cloud-brain`.
- Read-only business baseline remains `D:\mavenProject\DHXY` / `696a12b0`.
- No business behavior difference is authorized.

## Delivered Source

### Cloud

- `LocalOcrSidecarLifecycle` reuses an already healthy identity-valid loopback OCR process or starts
  `ocr/local_ocr_server.py` with the resolved Python runtime before Cloud opens port `18080`.
- A newly owned OCR process is health-polled, PID-checked, logged under `logs/`, and closed with
  Cloud. A reused process is never killed by Cloud shutdown.
- Missing script/Python/`rapidocr`, invalid endpoint, early process exit, PID conflict, or health
  timeout fails Cloud startup before the HTTP listener is created.
- `CloudTeamRolePreflightService.Resolution` preserves exact unresolved reasons.
  `CloudTurnTaskRuntime` rejects unresolved local role preflight before registry installation and
  before start ACK, so no empty effective queue or asynchronous `SKIPPED` run is created.

### Client

- `HttpsTurnClient` decodes Cloud problem JSON and exposes
  `Cloud request rejected [TASK_START_REJECTED]: <exact reason>`.
- The existing `WindowTurnLoop`/`WindowTaskControlService` start-failure path already exits and
  releases the window immediately when the start POST fails. No second release mechanism or
  business-state branch was added.

<!-- TRUE_EOF: CR274 SOURCE-REVIEW-COMPLETE FRESH-RUNTIME-REQUIRED 2026-07-25 -->
