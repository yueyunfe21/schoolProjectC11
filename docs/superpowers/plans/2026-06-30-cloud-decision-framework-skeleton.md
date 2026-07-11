# Cloud Decision Framework Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first Hybrid Cloud skeleton so DHXY can call a mock cloud decision layer in Shadow mode without changing 五倍/修罗 runtime behavior.

**Architecture:** Add a small `com.bot.dhxy.cloud.decision` package with typed service ids, request/response models, client interface, mock implementation, and a coordinator that records local-vs-cloud shadow decisions. Execution defaults stay off; the framework is inert until future hook cards call it.

**Tech Stack:** Java 17, Spring Boot configuration properties/beans, Lombok value objects, JUnit tests through Maven.

---

## Scope Rules

- Do not modify 五倍/修罗/NPC/navigation behavior in this skeleton card.
- Do not add real HTTP calls yet.
- Do not return or execute click actions.
- Do not touch `WubeiTask`, `XiuluoTaskV2`, `NavigationService`, `NpcClickService`, or `DialogService` in this first card.
- All execute flags must default to disabled.
- Shadow calls may be tested through unit tests only.

## File Map

- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionServiceId.java`
  - Enum of small cloud service ids.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionMode.java`
  - Enum for `SHADOW`, `EXECUTE`, `DISABLED`.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudFallbackMode.java`
  - Enum for `LOCAL`, `STOP`, `SHADOW_ONLY`.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionRequest.java`
  - Immutable request model.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionResponse.java`
  - Immutable response model.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionResult.java`
  - Immutable local-vs-cloud result model used by the coordinator.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionClient.java`
  - Interface for cloud clients.
- Create `src/main/java/com/bot/dhxy/cloud/decision/MockCloudDecisionClient.java`
  - Spring bean mock client used for first skeleton.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionProperties.java`
  - `@ConfigurationProperties(prefix = "cloud")`.
- Create `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
  - Shadow-only coordinator that logs comparison and returns result.
- Modify `src/main/resources/application.properties`
  - Add disabled-by-default cloud config.
- Create `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinatorTest.java`
  - Tests mock success, disabled mode, timeout/failure fallback.
- Create `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionPropertiesTest.java`
  - Tests default flags/fallback values.

## Task 1: Domain Model And Properties

**Files:**

- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionServiceId.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionMode.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudFallbackMode.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionRequest.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionResponse.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionResult.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionProperties.java`
- Modify: `src/main/resources/application.properties`
- Create: `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionPropertiesTest.java`

- [ ] Define enums with these values:

```java
CloudDecisionServiceId:
TASK_CLASSIFIER, TASK_POLICY, TASK_RECOVERY,
ROUTE_CANDIDATE, MAP_TRANSFORM_ASSET, ROUTE_MEMORY,
TRACKER_LINK_RANKER, NPC_CLICK_STRATEGY, DIALOG_POLICY,
CAPABILITY_GATE, MAINTENANCE_THRESHOLD, TEAM_RETURN_POLICY,
SIGNED_ASSET_BUNDLE, LEARNED_MEMORY, POLICY_VERSION,
FAILURE_CLASSIFIER, FEATURE_FLAG, METRICS_INGEST

CloudDecisionMode:
DISABLED, SHADOW, EXECUTE

CloudFallbackMode:
LOCAL, STOP, SHADOW_ONLY
```

- [ ] Define `CloudDecisionRequest` as an immutable Lombok `@Value @Builder` class with:

```java
CloudDecisionServiceId serviceId;
String traceId;
String taskCode;
String phase;
String windowId;
String taskRunId;
String policyVersion;
String localDecision;
Map<String, String> context;
Instant createdAt;
```

Use `@Builder.Default` for `context = Map.of()` and `createdAt = Instant.now()`.

- [ ] Define `CloudDecisionResponse` as an immutable Lombok `@Value @Builder` class with:

```java
CloudDecisionServiceId serviceId;
String traceId;
String policyVersion;
String decision;
double confidence;
long ttlMs;
String fallbackReason;
Map<String, String> diagnostics;
Instant createdAt;
```

Use defaults for `diagnostics = Map.of()`, `confidence = 0.0d`, `ttlMs = 0L`, `createdAt = Instant.now()`.

- [ ] Define `CloudDecisionResult` as an immutable Lombok `@Value @Builder` class with:

```java
CloudDecisionMode mode;
CloudFallbackMode fallbackMode;
CloudDecisionRequest request;
CloudDecisionResponse response;
String localDecision;
String effectiveDecision;
boolean cloudAvailable;
boolean agreement;
boolean executed;
long elapsedMs;
String reason;
```

- [ ] Define `CloudDecisionProperties` as a Spring `@ConfigurationProperties(prefix = "cloud")` class using Lombok `@Data`.

Required defaults:

```java
private boolean enabled = false;
private String baseUrl = "http://127.0.0.1:18080";
private long timeoutMs = 300L;
private CloudFallbackMode defaultFallback = CloudFallbackMode.LOCAL;
private Map<CloudDecisionServiceId, Service> services = new EnumMap<>(CloudDecisionServiceId.class);

@Data
public static class Service {
    private boolean shadowEnabled = false;
    private boolean executeEnabled = false;
    private int executePercent = 0;
    private CloudFallbackMode fallback = CloudFallbackMode.LOCAL;
}
```

Add a helper:

```java
public Service service(CloudDecisionServiceId serviceId) {
    return services.computeIfAbsent(serviceId, ignored -> new Service());
}
```

- [ ] Add disabled-by-default config to `src/main/resources/application.properties`:

```properties
# Hybrid cloud decision framework. Defaults are intentionally inert; runtime hooks must opt in per service.
cloud.enabled=false
cloud.base-url=http://127.0.0.1:18080
cloud.timeout-ms=300
cloud.default-fallback=LOCAL
```

- [ ] Add `CloudDecisionPropertiesTest` verifying:

```java
CloudDecisionProperties properties = new CloudDecisionProperties();
assertFalse(properties.isEnabled());
assertEquals(CloudFallbackMode.LOCAL, properties.getDefaultFallback());
assertFalse(properties.service(CloudDecisionServiceId.TASK_CLASSIFIER).isShadowEnabled());
assertFalse(properties.service(CloudDecisionServiceId.TASK_CLASSIFIER).isExecuteEnabled());
assertEquals(0, properties.service(CloudDecisionServiceId.TASK_CLASSIFIER).getExecutePercent());
```

- [ ] Run:

```powershell
mvn -q -Dtest=CloudDecisionPropertiesTest test
```

Expected: test passes.

## Task 2: Client And Coordinator

**Files:**

- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionClient.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/MockCloudDecisionClient.java`
- Create: `src/main/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinator.java`
- Create: `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionCoordinatorTest.java`

- [ ] Define `CloudDecisionClient`:

```java
public interface CloudDecisionClient {
    CloudDecisionResponse decide(CloudDecisionRequest request);
}
```

- [ ] Define `MockCloudDecisionClient` as a Spring `@Component`.

Behavior:

- Return a response with the same `serviceId` and `traceId`.
- Use request `policyVersion` if non-blank, otherwise `"mock-local"`.
- Use request `localDecision` if non-blank, otherwise `"LOCAL"`.
- Set confidence to `1.0d`.
- Set ttl to `1_000L`.
- Add diagnostics key `client=mock`.

- [ ] Define `CloudDecisionCoordinator` as a Spring `@Service` with constructor-injected `CloudDecisionProperties` and `CloudDecisionClient`.

Public method:

```java
public CloudDecisionResult shadow(CloudDecisionRequest request, String localDecision)
```

Behavior:

- If `cloud.enabled=false`, do not call client. Return result with `mode=DISABLED`, `cloudAvailable=false`, `effectiveDecision=localDecision`, `executed=false`, `reason="cloud disabled"`.
- If service shadow is disabled and execute is disabled, do not call client. Return result with `mode=DISABLED`, `reason="service disabled"`.
- Otherwise call client and measure elapsed ms.
- Compare `localDecision` with cloud response decision using null-safe string equality.
- Always return `effectiveDecision=localDecision` and `executed=false` in this skeleton.
- On client exception, return result with `cloudAvailable=false`, `effectiveDecision=localDecision`, `reason` containing exception class simple name.
- Log one structured info line:

```text
cloud.decision serviceId={} mode={} traceId={} localDecision={} cloudDecision={} agree={} elapsedMs={} fallback={} reason={}
```

- [ ] Add `CloudDecisionCoordinatorTest` with three tests:

1. Disabled global config does not call throwing client and returns local decision.
2. Shadow enabled calls mock client and returns agreement true when decisions match.
3. Throwing client returns local decision with cloudAvailable false and reason including exception class.

- [ ] Run:

```powershell
mvn -q -Dtest=CloudDecisionCoordinatorTest test
```

Expected: tests pass.

## Task 3: Wiring Guard And Documentation Update

**Files:**

- Create: `src/test/java/com/bot/dhxy/cloud/decision/CloudDecisionSkeletonWiringTest.java`
- Modify: `docs/HYBRID_CLOUD_WORKFLOW.md`

- [ ] Add a Spring wiring test that loads only the cloud decision beans and asserts:

```java
CloudDecisionProperties properties exists
CloudDecisionClient client exists
CloudDecisionCoordinator coordinator exists
cloud.enabled default is false
```

Use Spring Boot test style already present in repository tests.

- [ ] Update `docs/HYBRID_CLOUD_WORKFLOW.md` section `8. 第一张实现卡建议` after implementation with:

```text
Implementation note:
- CR-HC-001 skeleton implemented in package com.bot.dhxy.cloud.decision.
- No business hook is connected yet.
- Execute mode remains disabled by default.
- Next candidate hook: TaskClassifierCloud shadow mode.
```

- [ ] Run:

```powershell
mvn -q -DskipTests compile
mvn -q -Dtest=CloudDecisionPropertiesTest,CloudDecisionCoordinatorTest,CloudDecisionSkeletonWiringTest test
```

Expected: compile and tests pass.

## Manager Review Checklist

After workers return:

- [ ] Confirm no 五倍/修罗/navigation/NPC/Dialog behavior files were modified.
- [ ] Confirm `cloud.enabled=false` default.
- [ ] Confirm execute mode exists only as config/model and does not affect runtime.
- [ ] Confirm tests pass.
- [ ] Confirm logs include service id, trace id, local/cloud decision, agreement, elapsed time.
- [ ] Confirm no real HTTP client or remote dependency was introduced yet.

