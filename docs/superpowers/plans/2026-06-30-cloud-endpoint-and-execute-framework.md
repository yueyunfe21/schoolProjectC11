# Cloud Endpoint And Execute Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real local cloud-decision endpoint for live HTTP testing, then add the first cloud execution gate for `TASK_CLASSIFIER` only.

**Architecture:** CR-HC-005 creates a dev-only HTTP endpoint that speaks the existing `CloudDecisionRequest` / `CloudDecisionResponse` JSON contract and can be run as a separate process. CR-HC-006 updates the client-side decision coordinator and `TaskClassifierCloudShadowService` so `TASK_CLASSIFIER` can consume a cloud decision behind `execute-enabled` + `execute-percent`, while `TRACKER_LINK_RANKER` remains shadow-only.

**Tech Stack:** Java 17, Spring Boot configuration binding, Java `HttpServer` for the dev endpoint, existing `CloudDecisionCoordinator`, Maven focused tests, PowerShell helper scripts.

---

## File Structure

CR-HC-005 endpoint worker owns:

- Create `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServer.java`
  - Standalone local HTTP server for `POST /api/cloud/decision`.
  - Uses the same JSON shape as `HttpCloudDecisionClient`.
  - Echoes `localDecision` by default and can force a decision with environment variables.
- Create `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServerTest.java`
  - Verifies auth, response contract, default echo, forced decision, and bad JSON handling.
- Create `scripts/run-cloud-decision-dev-server.ps1`
  - Starts the dev endpoint with token/port/path defaults suitable for local testing.
- Modify `docs/HYBRID_CLOUD_WORKFLOW.md`
  - Record CR-HC-005 implementation and how to run it.
- Modify `docs/ACTIVE_WORK.md`
  - Record worker baseline, verification, and runtime commands.

CR-HC-006 execute-framework worker owns:

- Modify `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
  - Make execute mode actually choose `effectiveDecision=cloudDecision` only when allowed and percent gate hits.
  - Keep all failure paths `effectiveDecision=localDecision`.
- Modify `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionProperties.java`
  - Add deterministic execute-percent configuration support if missing.
- Modify `src/main/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowService.java`
  - Return the effective `TASK_CLASSIFIER` decision for supported/valid task keys.
  - Keep `TRACKER_LINK_RANKER` unchanged and shadow-only.
- Modify `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
  - Consume only `TASK_CLASSIFIER` effective decisions by mapping a cloud `taskKey` back to existing local `TaskTrackerTitleTemplate`.
  - Only adjust the `titleTemplate` of an already-found local result; never invent green links, click points, OCR paths, or found results.
- Create or modify focused tests under `src/test/java/com/bot/dhxy/cloud/task/` and `src/test/java/com/bot/dhxy/service/`
  - Verify execute percent, fallback, valid key override, invalid key rejection, and that link-ranker remains shadow-only.
- Modify `docs/HYBRID_CLOUD_WORKFLOW.md`
  - Record CR-HC-006 implementation, config, rollout, and rollback.
- Modify `docs/ACTIVE_WORK.md`
  - Record worker baseline, review findings, and verification.

## CR-HC-005: Local Dev Cloud Decision Endpoint

### Task 1: Add Dev Endpoint Contract Test

**Files:**
- Create: `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServerTest.java`
- Create: `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServer.java`

- [ ] **Step 1: Write the failing test**

Create `CloudDecisionDevServerTest` with a `main` entry that starts the server on an ephemeral port and sends four requests:

```java
package com.bot.dhxy.cloud.dev;

public class CloudDecisionDevServerTest {
    public static void main(String[] args) throws Exception {
        CloudDecisionDevServer server = CloudDecisionDevServer.startForTest(0, "/api/cloud/decision", "local-dev-token", null);
        try {
            String baseUrl = "http://127.0.0.1:" + server.port();
            assertContains(post(baseUrl, "/api/cloud/decision", "local-dev-token", requestJson("TASK_CLASSIFIER", "trace-1", "wubei", "phase", "local-A")), "\"decision\":\"local-A\"", "default echo decision");
            assertContains(post(baseUrl, "/api/cloud/decision", "bad-token", requestJson("TASK_CLASSIFIER", "trace-2", "wubei", "phase", "local-A")), "401", "bad token rejected");
            assertContains(post(baseUrl, "/api/cloud/decision", "local-dev-token", "{bad-json"), "400", "bad json rejected");
        } finally {
            server.stop();
        }
        System.out.println("CloudDecisionDevServerTest passed");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -q -DskipTests test-compile
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServerTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

Expected: compile fails because `CloudDecisionDevServer` does not exist.

- [ ] **Step 3: Implement the dev endpoint**

Implement `CloudDecisionDevServer` as a test-scope standalone server:

```java
public final class CloudDecisionDevServer {
    public static CloudDecisionDevServer start(int port, String path, String token, String forcedDecision) { ... }
    public static CloudDecisionDevServer startForTest(int port, String path, String token, String forcedDecision) { ... }
    public int port() { ... }
    public void stop() { ... }
    public static void main(String[] args) { ... }
}
```

Required behavior:

- Accept only `POST`.
- Require `Authorization: Bearer <token>`.
- Parse `serviceId`, `traceId`, `localDecision`, `policyVersion`.
- Respond:

```json
{
  "serviceId": "TASK_CLASSIFIER",
  "traceId": "trace-1",
  "policyVersion": "dev-local-v1",
  "decision": "local-A",
  "confidence": 1.0,
  "ttlMs": 1000,
  "diagnostics": {
    "server": "dev-local",
    "forced": "false"
  }
}
```

- If `forcedDecision` is non-blank, use it as `decision`; otherwise use request `localDecision`.

- [ ] **Step 4: Run verification**

Run:

```powershell
mvn -q -DskipTests test-compile
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServerTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

Expected: `CloudDecisionDevServerTest passed`.

### Task 2: Add Local Start Script

**Files:**
- Create: `scripts/run-cloud-decision-dev-server.ps1`
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`
- Modify: `docs/ACTIVE_WORK.md`

- [ ] **Step 1: Write the script**

The script must set defaults without committing a production secret:

```powershell
param(
    [int]$Port = 18080,
    [string]$Path = "/api/cloud/decision",
    [string]$Token = "local-dev-token",
    [string]$ForcedDecision = ""
)

mvn -q -DskipTests test-compile
mvn -q -DskipTests `
  "-Dexec.classpathScope=test" `
  "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServer" `
  "-Dexec.args=--port $Port --path $Path --token $Token --forced-decision $ForcedDecision" `
  org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

- [ ] **Step 2: Document run config**

Record these client runtime settings:

```properties
cloud.real-transport-enabled=true
cloud.base-url=http://127.0.0.1:18080
cloud.endpoint-path=/api/cloud/decision
cloud.token=local-dev-token
cloud.services.task-classifier.shadow-enabled=true
cloud.services.task-classifier.execute-enabled=false
cloud.services.tracker-link-ranker.shadow-enabled=true
cloud.services.tracker-link-ranker.execute-enabled=false
```

- [ ] **Step 3: Verify script starts**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-cloud-decision-dev-server.ps1 -Port 18080
```

Expected: process stays alive and logs the listening URL. Stop it manually after verification.

## CR-HC-006: TaskClassifier Execute Framework

### Task 3: Coordinator Execute Gate

**Files:**
- Modify: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
- Modify: `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinatorTest.java`

- [ ] **Step 1: Add failing tests**

Add tests proving:

- `execute-enabled=true` and `execute-percent=100` with cloud success returns `executed=true` and `effectiveDecision=cloudDecision`.
- `execute-enabled=true` and `execute-percent=0` returns `executed=false` and `effectiveDecision=localDecision`.
- cloud timeout/failure always returns `executed=false` and `effectiveDecision=localDecision`.

- [ ] **Step 2: Implement deterministic percent gate**

Use a deterministic hash of `traceId + serviceId + taskCode + phase` so one sample is stable across retries:

```java
private static boolean executePercentHit(CloudDecisionRequest request, int percent) {
    if (percent <= 0) {
        return false;
    }
    if (percent >= 100) {
        return true;
    }
    String key = request.getTraceId() + "|" + request.getServiceId() + "|" + request.getTaskCode() + "|" + request.getPhase();
    int bucket = Math.floorMod(key.hashCode(), 100);
    return bucket < percent;
}
```

On cloud success:

```java
boolean execute = mode == CloudDecisionMode.EXECUTE && executePercentHit(request, service.getExecutePercent());
String effectiveDecision = execute ? response.getDecision() : localDecision;
```

- [ ] **Step 3: Run tests**

Run:

```powershell
mvn -q -Dtest=CloudDecisionCoordinatorTest test
```

Expected: pass.

### Task 4: TaskClassifier Effective Decision Consumption

**Files:**
- Modify: `src/main/java/com/bot/dhxy/cloud/task/TaskClassifierCloudShadowService.java`
- Modify: `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- Create or modify: `src/test/java/com/bot/dhxy/cloud/task/TaskClassifierCloudExecuteWiringTest.java`

- [ ] **Step 1: Add failing wiring test**

The test must assert:

- `TaskTrackerPanelService` may consume `TaskClassifierCloudShadowService` effective decisions.
- It must not consume cloud decisions when the local result is `found=false`.
- It must not create or modify green links.
- It must reject unknown `taskKey`.
- It must not import or call `TrackerLinkRankerCloudShadowService` for classifier execution.

- [ ] **Step 2: Return classifier result from cloud service**

Change `TaskClassifierCloudShadowService` from void-only reporting to a method that can return a decision result while preserving existing callers:

```java
public CloudDecisionResult shadowWubeiTrackerResult(String source, TaskTrackerPanelReadResult result) { ... }
public CloudDecisionResult shadowXiuluoTrackerResult(String source, TaskTrackerPanelReadResult result) { ... }
```

Existing behavior must remain safe when callers ignore the return value.

- [ ] **Step 3: Apply only safe classifier override**

In `TaskTrackerPanelService`, after the local result is built:

- call `TaskClassifierCloudShadowService`;
- if result is found and cloud executed;
- if effective decision maps to an existing local title template for that task family;
- rebuild only `titleTemplate`;
- keep `found`, `detailRawPath`, `detailYellowPath`, absolute coordinates, `yellowText`, `greenLinks`, `greenBandWidth`, and `probeObjective` unchanged.

Invalid cloud keys must log and return local result.

- [ ] **Step 4: Keep TrackerLinkRanker shadow-only**

Do not change `TrackerLinkRankerCloudShadowService` return type or task usage. Even if `cloud.services.tracker-link-ranker.execute-enabled=true`, no task may consume its `effectiveDecision` in CR-HC-006.

- [ ] **Step 5: Run focused tests**

Run:

```powershell
mvn -q -Dtest="CloudDecisionCoordinatorTest,TaskClassifierCloudShadowServiceTest,TaskClassifierCloudExecuteWiringTest,TrackerLinkRankerCloudShadowWiringTest" test
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
```

Expected: pass.

## Final Verification

Run after both workers land:

```powershell
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
mvn -q -Dtest="com.bot.dhxy.cloud.**.*Test" test
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.dev.CloudDecisionDevServerTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.decision.CloudHttpDecisionClientTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.cloud.task.CloudRealShadowServicesIntegrationTest" org.codehaus.mojo:exec-maven-plugin:3.1.0:java
```

Expected:

- All commands pass.
- `TASK_CLASSIFIER` can be executed by cloud only when `execute-enabled=true` and percent gate hits.
- `TRACKER_LINK_RANKER` remains shadow-only.
- No OCR/template/click/navigation/Runner/input queue behavior changes.
