# Xiuluo Brain v1 Start Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one real typed API endpoint, `POST /api/v1/xiuluo/brain/start`, so the team can inspect the new Gateway -> Endpoint -> Service shape before migrating the whole 修罗 brain.

**Architecture:** Keep legacy `/api/cloud/decision + serviceId=XIULUO_BRAIN` untouched. Add a new v1 endpoint that parses JSON into a typed start request, calls a dedicated Xiuluo brain service start method, and returns a typed JSON command response. This CR does not switch DHXY local runtime callers to the new endpoint.

**Tech Stack:** Java, `com.sun.net.httpserver.HttpServer`, Jackson `JsonNode`/`ObjectMapper`, Maven tests in `D:\mavenProject\dhxy-cloud-brain`.

## Global Constraints

- External cloud server root: `D:\mavenProject\dhxy-cloud-brain`.
- DHXY docs/root: `D:\mavenProject\DHXY`.
- 谢帅 is manager/reviewer only for this CR. Worker agents implement Java code; 谢帅 does not write Java business implementation.
- CR211 is one endpoint only: `POST /api/v1/xiuluo/brain/start`.
- Do not switch DHXY local production callers to `/api/v1/xiuluo/brain/start` in this CR.
- Do not remove or change legacy `/api/cloud/decision` behavior.
- Do not change OCR/template/click/navigation/NPC/dialog/tracker/image algorithms.
- Do not migrate 修罗 `step` or `action-outcome` in this CR.
- Do not allow mixed runtime use of v1 start followed by legacy step. Since no DHXY caller is switched, the v1 start endpoint is server-side contract/probe only.
- v1 start must preserve start semantics from current `DecisionEngine.xiuluoBrainStart(...)`:
  - require `windowId`
  - require `taskRunId`
  - create `sessionId`
  - `stateSeq=1`
  - generate `phaseToken` and `actionId`
  - if context has visible tracker hot-start facts, first phase is `TRY_TRACKER_SHORTCUT`
  - otherwise first phase is `initialPhase`, or `PREPARE_ROUND` when blank
  - return a command equivalent to legacy start fields.
- If response field naming needs a typed v1 schema, keep it simple and documented; do not invent extra fields not used by current 修罗 command parsing.

---

## File Structure

External cloud-brain files:

- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudBrainServer.java`
  - Register `POST /api/v1/xiuluo/brain/start` in the existing CR210 Gateway route table.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\xiuluo\XiuluoBrainStartEndpoint.java`
  - HTTP/JSON boundary for the new v1 start API.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainService.java`
  - Own the v1 start business method and session store for v1 sessions.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainStartRequest.java`
  - Typed start request parsed by endpoint.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainCommandResponse.java`
  - Typed command response serialized by endpoint.
- Optional create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainSession.java`
  - Private/package model for v1 session state if keeping it outside the service is clearer.
- Test: `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\XiuluoBrainV1StartEndpointTest.java`

DHXY docs:

- Modify: `D:\mavenProject\DHXY\docs\PACKAGE_ARCHITECTURE.md`
- Modify: `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md`
- Modify: `D:\mavenProject\DHXY\docs\cr-dashboard-data.js` after dashboard generation

## Task 1: Add v1 Xiuluo Brain Start Endpoint

**Files:**

- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudBrainServer.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\xiuluo\XiuluoBrainStartEndpoint.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainService.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainStartRequest.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\xiuluo\XiuluoBrainCommandResponse.java`
- Test: `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\XiuluoBrainV1StartEndpointTest.java`

**Interfaces:**

- `XiuluoBrainStartEndpoint implements CloudApiEndpoint`
  - Consumes raw `JsonNode`.
  - Converts to `XiuluoBrainStartRequest`.
  - Returns JSON serialized from `XiuluoBrainCommandResponse`.
- `XiuluoBrainService.start(XiuluoBrainStartRequest request) -> XiuluoBrainCommandResponse`
  - Owns v1 start semantics and v1 session registration.
- `XiuluoBrainStartRequest`
  - Fields: `windowId`, `taskRunId`, `initialPhase`, `context`.
- `XiuluoBrainCommandResponse`
  - Fields must include enough data for the current command contract: `status`, `windowId`, `taskRunId`, `sessionId`, `stateSeq`, `phaseToken`, `acceptedPhaseToken`, `phase`, `action`, `actionId`, `reason`, `ttlMs`.

- [ ] **Step 1: Write endpoint tests first**

Create `XiuluoBrainV1StartEndpointTest` that starts the real `CloudBrainServer` and posts to `/api/v1/xiuluo/brain/start`.

Test cases:

1. Normal start:

```json
{
  "windowId": "window-A",
  "taskRunId": "task-run-A",
  "initialPhase": "PREPARE_ROUND",
  "context": {}
}
```

Expected:

```text
HTTP 200
status=COMMAND
windowId=window-A
taskRunId=task-run-A
sessionId nonblank
stateSeq=1
phaseToken nonblank
actionId nonblank
acceptedPhaseToken blank
phase=PREPARE_ROUND
reason=xiuluo.brain.start
```

2. Blank initial phase defaults to `PREPARE_ROUND`.

3. Visible tracker facts choose `TRY_TRACKER_SHORTCUT`:

```json
{
  "windowId": "window-A",
  "taskRunId": "task-run-A",
  "initialPhase": "PREPARE_ROUND",
  "context": {
    "trackerFactSource": "runtime",
    "trackerFound": "true",
    "trackerGreenLinkCount": "1"
  }
}
```

4. Missing `windowId` returns HTTP 200 with a rejected/error JSON response, or HTTP 400 if the endpoint chooses API validation failure. Pick one behavior and document it in CR211; do not throw an unhandled exception.

Run:

```powershell
mvn -q -Dtest=XiuluoBrainV1StartEndpointTest test
```

Expected before implementation: fail because endpoint/classes do not exist.

- [ ] **Step 2: Implement typed request/result models**

Use simple Java records or final classes consistent with the external project style. Keep `context` as a `JsonNode` or `Map<String, String>` if the current hot-start facts need flexible fields.

Do not expose `DecisionEngine.Decision` or old private nested types.

- [ ] **Step 3: Implement `XiuluoBrainService.start(...)`**

Port only the start semantics from current `DecisionEngine.xiuluoBrainStart(...)`.

Do not call `DecisionEngine.decisionResponse(...)`.
Do not build a fake `/api/cloud/decision` request internally.
Do not route through `serviceId`.

The service may duplicate the small start logic for now because legacy callers are not switched in this CR. Record this as temporary until step/action-outcome are also migrated.

- [ ] **Step 4: Implement `XiuluoBrainStartEndpoint`**

The endpoint should:

```text
parse JsonNode -> XiuluoBrainStartRequest
call XiuluoBrainService.start(request)
serialize XiuluoBrainCommandResponse -> JSON
```

The endpoint should not decide next phase itself.

- [ ] **Step 5: Register route in `CloudBrainServer`**

Add:

```text
/api/v1/xiuluo/brain/start -> XiuluoBrainStartEndpoint
```

Keep all old CR210 routes unchanged.

- [ ] **Step 6: Run verification**

From `D:\mavenProject\dhxy-cloud-brain`:

```powershell
mvn -q -Dtest=XiuluoBrainV1StartEndpointTest test
mvn -q -Dtest=CloudApiGatewayCompatibilityTest test
mvn -q -Dtest=XiuluoBrainProtocolTest test
mvn -q -DskipTests compile
```

Expected: all exit `0`.

- [ ] **Step 7: Update CR211 docs**

Record:

```text
worker id/name
files changed
new endpoint path
response schema summary
tests run
explicit note that no DHXY runtime caller was switched
temporary limitation: only start is v1; do not mix v1 start with legacy step/action-outcome
```

If CR row/card changes, run from `D:\mavenProject\DHXY`:

```powershell
node scripts/generate-cr-dashboard-data.js
```

## Task 2: Two Independent Reviews

**Files:**

- Read-only review of CR211 changed files.
- Modify docs only if recording findings/status.

Review requirements:

- Reviewer A: API contract and test coverage.
- Reviewer B: behavior parity and no local caller switch.

Blockers:

- Any old endpoint behavior change is P1.
- Any DHXY production caller switch to `/api/v1/xiuluo/brain/start` is P1.
- Any internal call from v1 start back through `/api/cloud/decision` or `DecisionEngine.decisionResponse(...)` is P1, because this CR is meant to show the new endpoint/service shape.
- Missing tests for normal start and tracker hot-start is P1.

## Self-Review

- Spec coverage: the plan creates exactly one v1 endpoint and keeps the legacy route intact.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: endpoint, request, response, and service names are consistent across the plan.
