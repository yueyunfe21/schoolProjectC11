# Cloud Brain API Gateway Modular Monolith Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put `dhxy-cloud-brain` behind an explicit API Gateway / route / endpoint structure without changing any current cloud decision algorithm.

**Architecture:** DHXY local code talks only to the cloud API Gateway. In this first version the "microservices" are logical services inside the same `dhxy-cloud-brain` JVM, not separate deployed servers. The Gateway owns HTTP concerns and dispatches to endpoint adapters; `DecisionEngine` remains the internal business collaborator until later CRs split it by capability.

**Tech Stack:** Java 17-style source, `com.sun.net.httpserver.HttpServer`, Jackson `JsonNode`/`ObjectMapper`, Maven tests in `D:\mavenProject\dhxy-cloud-brain`.

## Global Constraints

- Current branch/workspace: `D:\mavenProject\DHXY` branch `codex/hybrid-cloud-protection`; external cloud server is `D:\mavenProject\dhxy-cloud-brain`.
- 谢帅 is manager/reviewer only for this CR. Worker agents implement Java code; 谢帅 does not write cloud-brain business implementation.
- CR210 is structure-only. Do not change OCR, template matching, NPC click, navigation, tracker, route memory, xiuluo brain phase, retry, fallback, threshold, coordinate, or clicked-point business behavior.
- Preserve all currently supported HTTP endpoints and auth behavior:
  - `POST /api/cloud/decision`
  - `POST /api/cloud/route-memory/outcome`
  - `POST /api/cloud/route-memory/migrate`
  - `POST /api/cloud/npc-click-smart/outcome`
- Preserve current error semantics unless a test proves a documented current behavior is impossible to keep:
  - unknown path -> HTTP 404 JSON error
  - non-POST -> HTTP 405 with `Allow: POST`
  - bad bearer token -> HTTP 401 JSON error
  - invalid/non-object JSON -> HTTP 400 JSON error
- The new Gateway must be a modular-monolith boundary: endpoint handlers may call Java collaborators directly; do not introduce HTTP calls between internal services in this CR.
- Do not add new production dependencies unless the worker records the reason in the CR card and both reviewers approve.
- New tests must run from `D:\mavenProject\dhxy-cloud-brain`; DHXY local Java tests are optional unless the worker touches `D:\mavenProject\DHXY\src`.

---

## File Structure

External cloud-brain files:

- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudBrainServer.java`
  - Keep server lifecycle, bind address, executor, `baseUrl()`, and `close()`.
  - Replace inline path/auth/method/JSON dispatch with a call into `CloudApiGateway`.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiGateway.java`
  - Own route registry, method/auth/body validation, route lookup, and response writing.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiEndpoint.java`
  - Functional interface for endpoint adapters: consume `JsonNode`, return JSON string.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiRoute.java`
  - Small immutable route definition: path + endpoint.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiResponseWriter.java`
  - Central JSON response/error helper used by `CloudApiGateway`.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\LegacyDecisionEndpoint.java`
  - Adapter from `/api/cloud/decision` to `DecisionEngine.decisionResponse(...)`.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\RouteMemoryOutcomeEndpoint.java`
  - Adapter from `/api/cloud/route-memory/outcome` to `DecisionEngine.routeMemoryOutcomeResponse(...)`.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\RouteMemoryMigrationEndpoint.java`
  - Adapter from `/api/cloud/route-memory/migrate` to `DecisionEngine.routeMemoryMigrationResponse(...)`.
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\NpcClickSmartOutcomeEndpoint.java`
  - Adapter from `/api/cloud/npc-click-smart/outcome` to `DecisionEngine.npcClickSmartOutcomeResponse(...)`.
- Create or modify tests under `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\`.
  - Add a gateway compatibility test covering route, auth, method, bad JSON, and old endpoint behavior.

DHXY repo docs:

- Modify: `D:\mavenProject\DHXY\docs\PACKAGE_ARCHITECTURE.md`
  - Keep CR210 card updated with worker status, reviewer findings, and verification commands.
- Modify: `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md`
  - Record baseline, worker handoff, tests, and review state.
- Modify: `D:\mavenProject\DHXY\docs\HYBRID_CLOUD_WORKFLOW.md`
  - Record the settled Gateway / logical microservice architecture.

## Task 1: Gateway Skeleton + Legacy Endpoint Compatibility

**Files:**

- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudBrainServer.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiGateway.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiEndpoint.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiRoute.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\gateway\CloudApiResponseWriter.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\LegacyDecisionEndpoint.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\RouteMemoryOutcomeEndpoint.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\RouteMemoryMigrationEndpoint.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\NpcClickSmartOutcomeEndpoint.java`
- Test: `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\CloudApiGatewayCompatibilityTest.java`

**Interfaces:**

- `CloudApiEndpoint`:
  - Consumes: Jackson `JsonNode request`.
  - Produces: JSON response string.
- `CloudApiRoute`:
  - Produces route entries with `String path()` and `CloudApiEndpoint endpoint()`.
- `CloudApiGateway`:
  - Consumes: `HttpExchange`, bearer token, and a route registry.
  - Produces: HTTP response with the same JSON status semantics as old `CloudBrainServer.handle(...)`.
- Endpoint adapters:
  - Consume one `DecisionEngine`.
  - Produce the exact JSON string returned by the matching existing `DecisionEngine` method.

- [ ] **Step 1: Write compatibility tests before refactor**

Add `CloudApiGatewayCompatibilityTest` that starts `CloudBrainServer` on port `0` with token `local-dev-token`, then verifies:

```text
POST /api/cloud/decision with valid token and {"serviceId":"FEATURE_FLAG"} returns HTTP 200 and body serviceId=FEATURE_FLAG.
POST /api/cloud/route-memory/outcome with valid token returns HTTP 200 JSON, not 404.
POST /api/cloud/route-memory/migrate with valid token returns HTTP 200 JSON, not 404.
POST /api/cloud/npc-click-smart/outcome with valid token returns HTTP 200 JSON, not 404.
GET /api/cloud/decision returns HTTP 405 and Allow=POST.
POST /api/cloud/decision without the exact bearer token returns HTTP 401.
POST /api/cloud/unknown returns HTTP 404.
POST /api/cloud/decision with invalid JSON returns HTTP 400.
```

Run:

```powershell
mvn -q -Dtest=CloudApiGatewayCompatibilityTest test
```

Expected before implementation: the file does not exist or the new assertions are not yet covered.

- [ ] **Step 2: Create gateway abstractions**

Create the gateway package with exactly one HTTP owner:

```text
CloudApiGateway.handle(HttpExchange exchange)
CloudApiResponseWriter.respond(...)
CloudApiResponseWriter.jsonError(...)
CloudApiRoute.of(String path, CloudApiEndpoint endpoint)
CloudApiEndpoint.handle(JsonNode request)
```

Keep these classes small. Do not move `DecisionEngine` logic into the gateway package.

- [ ] **Step 3: Create legacy endpoint adapters**

Create one adapter class per old endpoint. Each adapter should be a thin collaborator around the matching `DecisionEngine` method:

```text
LegacyDecisionEndpoint -> decisionResponse
RouteMemoryOutcomeEndpoint -> routeMemoryOutcomeResponse
RouteMemoryMigrationEndpoint -> routeMemoryMigrationResponse
NpcClickSmartOutcomeEndpoint -> npcClickSmartOutcomeResponse
```

The adapters must not parse `serviceId`, inspect `decision`, or rewrite body fields.

- [ ] **Step 4: Wire `CloudBrainServer` through `CloudApiGateway`**

`CloudBrainServer.start(...)` should still create the `HttpServer`, executor, token, and `DecisionEngine`, but route registration should be explicit and visible:

```text
/api/cloud/decision -> LegacyDecisionEndpoint
/api/cloud/route-memory/outcome -> RouteMemoryOutcomeEndpoint
/api/cloud/route-memory/migrate -> RouteMemoryMigrationEndpoint
/api/cloud/npc-click-smart/outcome -> NpcClickSmartOutcomeEndpoint
```

The `CloudBrainServer.handle(...)` method should become a small delegation to the gateway or disappear if `createContext("/", gateway::handle)` is clearer.

- [ ] **Step 5: Run focused external tests**

Run from `D:\mavenProject\dhxy-cloud-brain`:

```powershell
mvn -q -Dtest=CloudApiGatewayCompatibilityTest test
mvn -q -Dtest=XiuluoBrainProtocolTest test
mvn -q -DskipTests compile
```

Expected: all commands exit `0`.

- [ ] **Step 6: Update CR210 docs**

Update the CR210 card and `ACTIVE_WORK.md` with:

```text
worker id/name
files changed
behavior-preservation statement
tests run and exit status
any concerns
```

If the CR table row changes, run from `D:\mavenProject\DHXY`:

```powershell
node scripts/generate-cr-dashboard-data.js
```

Expected: `docs/cr-dashboard-data.js` regenerated successfully.

## Task 2: Typed v1 API Design Draft, No Runtime Switch

**Files:**

- Modify: `D:\mavenProject\DHXY\docs\HYBRID_CLOUD_WORKFLOW.md`
- Modify: `D:\mavenProject\DHXY\docs\PACKAGE_ARCHITECTURE.md`
- Optional create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\api\ApiPathCatalog.java`

**Interfaces:**

- Produces a documented v1 path catalog only. No DHXY local caller may be switched to these paths in CR210.

- [ ] **Step 1: Draft path catalog**

Document the intended next endpoints:

```text
/api/v1/xiuluo/brain/start
/api/v1/xiuluo/brain/step
/api/v1/xiuluo/brain/action-outcome
/api/v1/npc-click/start
/api/v1/npc-click/poll
/api/v1/npc-click/outcome
/api/v1/tracker-panel/read
/api/v1/tracker-link/rank
/api/v1/navigation/route-candidate
/api/v1/navigation/route-memory
/api/v1/image/preprocess
/api/v1/dialog/resolve-option
```

- [ ] **Step 2: Mark CR210 boundary**

Record explicitly that CR210 does not migrate callers to the v1 paths. The first runtime caller migration must be a later CR with focused tests and fresh-run gate.

- [ ] **Step 3: Review-only verification**

Run:

```powershell
node scripts/generate-cr-dashboard-data.js
```

Expected: dashboard data refresh exits `0`.

## Task 3: Two Independent Reviews

**Files:**

- Read-only review of all files changed by Task 1 and Task 2.
- Modify only docs if review findings/status must be recorded.

**Interfaces:**

- Reviewer A and Reviewer B each produce a verdict:
  - `APPROVED`
  - or blocker severity with exact file/method/evidence.

- [ ] **Step 1: Dispatch Reviewer A**

Reviewer A checks:

```text
No algorithm/business behavior change.
Old endpoint compatibility preserved.
Gateway owns HTTP concerns only.
DecisionEngine remains internal collaborator.
Tests cover auth/method/path/bad-json/current endpoints.
```

- [ ] **Step 2: Dispatch Reviewer B**

Reviewer B independently checks the same diff, with special attention to:

```text
No new serviceId switch semantics.
No hidden local/DHXY caller changes.
No swallowed exceptions that change HTTP status.
No route memory or NPC outcome response rewrite.
```

- [ ] **Step 3: Record review result**

Only if both reviewers approve may CR210 be marked `Review`.

If either reviewer finds P0/P1/P2, keep CR210 open, record the finding in the card, and send the worker back for repair.

## Self-Review

- Spec coverage: CR210 covers the user's approved Gateway / logical microservice structure at the HTTP entry level. It deliberately does not split every `DecisionEngine` service yet, because that would mix structure refactor with active business algorithms.
- Placeholder scan: no `TBD` / `TODO` / "fill later" items remain in the CR210 task scope.
- Type consistency: route/endpoint/gateway names are consistent across file structure and task steps.
