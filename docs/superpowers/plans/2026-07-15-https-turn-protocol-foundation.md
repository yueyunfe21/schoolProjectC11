# HTTPS Turn Protocol Foundation Implementation Plan

> **范围提示（2026-07-15）：** 本文现为 Foundation 细节附录。完整实施范围、卡片依赖、业务切流、激活与
> 删旧门以 `2026-07-15-https-turn-complete-migration-card-plan.md` 为唯一权威；不得把 Foundation 作为
> 独立结束点或另起后续迁移计划。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete client-initiated HTTPS `/turn` foundation, including JSON/multipart exchange, exact-window local action execution, approved local-Service dispatch, and hash-refreshed templates, without activating or deleting the existing `/poll + /outcome` bridge yet.

**Architecture:** DHXY keeps one explicit long-wait loop per registered window. Each request reports the previous action outcome and optional PNG; each response supplies the next closed action JSON. Cloud owns one package-private single-slot exchange per `deviceId + windowId`, retaining only the current action and the immediately preceding accepted result so a lost HTTP response cannot cause a second physical execution. The new path is built beside the old bridge; later cards in the authoritative master plan migrate callers and remove the old protocol only after their stated gates pass.

**Tech Stack:** Java 21, JDK `HttpClient`, JDK `HttpServer`, Jackson 2.15, PNG via `ImageIO`, OpenCV through existing `ImageFinder`, Win32 HWND capture through `BoundWindowCaptureService`, serialized physical input through `InputActionQueue`.

## Global Constraints

- Business baseline remains `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; this plan changes transport/mechanics only.
- Preserve every existing dirty/untracked file. Read current contents immediately before each patch; never reset, clean, checkout, overwrite, or commit.
- Do not start Cloud Brain, DHXY, a Task, poller, UI, capture runtime, or physical input during implementation.
- No automated tests, replay tests, source guards, or test assets are added or run unless the user separately requests a named test.
- Java changes require fresh `mvn -q -DskipTests compile` in DHXY and fresh `mvn -q clean package` in Cloud Brain after writers are stable.
- External traffic is HTTPS. Plain HTTP remains permitted only for the existing loopback development host or TLS-terminating reverse-proxy backend.
- Do not add WebSocket, raw TCP framing, frequent short polling, owner/permit/session/ledger/compaction/durable workflow/business TTL, or automatic business retry.
- Transport reconnection may repeat an outcome submission but must never repeat physical execution. One window has at most one current action.
- Large images use multipart PNG; never Base64-encode a screenshot in JSON.
- Coordinates are exact screen-absolute pixels derived from the current native window rectangle. Do not resize an image or scale a coordinate.
- Background HWND capture is mandatory. Background keyboard is used only for shortcuts already validated by `BoundWindowKeyboardService`; unsupported keys fail typed and never fall back silently. Mouse remains on the existing foreground serialized input path.
- Ordinary OCR and image algorithms remain in Cloud. Local template matching happens only when a payload explicitly requests it.
- The only `LOCAL_SERVICE` targets are `BagService`, `UICleanerService`, `GiveItemService`, and `QuestManagerService` through the closed operation allowlist in Task 1.
- `GiveItemService.executeGiveDirectForExclusive(...)` remains an indivisible local operation; do not split item selection and the Give click across network turns.

## Scope Decomposition

This appendix supplies only the Foundation implementation detail. The master plan owns all caller migration,
activation, user-runtime evidence, and zero-reference deletion cards; neither this appendix nor a Foundation review
may create a separate follow-on plan.

---

### Task 1: Create The Exact Shared Turn Protocol

**Files:**
- Create in both repositories with byte-identical contents:
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnAction.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnStep.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnStepType.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnMatchSpec.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnRegion.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowRect.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnFramePurpose.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalServiceCall.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnBagOperationArguments.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnReturnItemCachePoint.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnUiOperationArguments.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnGiveItemOperationArguments.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnQuestOperationArguments.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnOutcome.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnStepResult.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnMatchResult.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnFrameMetadata.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnRequest.java`
  - `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnResponse.java`

**Interfaces:**
- Consumes: no existing remote DTOs.
- Produces: the only JSON contract used by Tasks 2-7.

- [ ] **Step 1: Record scoped baseline evidence before editing**

Run in both repositories:

```powershell
git status --short
git diff -- src/main/java/com/bot/dhxy/cloud/turn
```

Expected: status is captured in the worker report; no existing file under the new package is overwritten.

- [ ] **Step 2: Add the closed enums**

Use these exact values:

```java
public enum TurnStepType {
    CAPTURE,
    MATCH_TEMPLATE,
    INPUT,
    WAIT,
    LOCAL_SERVICE
}

public enum TurnInputAction {
    CLICK_LEFT, CLICK_RIGHT, DOUBLE_CLICK_LEFT, DOUBLE_CLICK_RIGHT,
    DRAG_LEFT, SCROLL, KEY_TAP, KEY_DOWN, KEY_UP, TEXT_INPUT
}

public enum TurnLocalOperation {
    BAG_RETURN_ITEM,
    BAG_USE_INCENSE,
    UI_CLEAN_ALL,
    UI_CLOSE_GENERIC_WINDOWS,
    UI_CLEAN_LIGHTWEIGHT,
    UI_CLOSE_MAP_SEARCH_INPUT_BY_X2,
    GIVE_ITEM_FROM_OPEN_DIALOG,
    QUEST_ACTIVATE,
    QUEST_CAPTURE_DETAIL
}

public enum TurnFramePurpose {
    CAPTURE, MATCH_EVIDENCE, QUEST_DETAIL, FAILURE_EVIDENCE
}
```

`GIVE_ITEM_FROM_OPEN_DIALOG` maps only to `executeGiveDirectForExclusive`; it is not a generic Give workflow.

- [ ] **Step 3: Add the action and step records**

Use one strict step record instead of polymorphic reflection:

```java
public record TurnAction(
        int contractVersion,
        String actionId,
        String deviceId,
        String windowId,
        List<TurnStep> steps,
        boolean fullWindowFailureEvidence) {
}

public record TurnStep(
        int index,
        TurnStepType type,
        TurnInputAction inputAction,
        TurnInputSpec input,
        Long waitMs,
        TurnCaptureSpec capture,
        TurnMatchSpec match,
        TurnLocalServiceCall localService) {
}
```

Validation is explicit and centralized later; no class or method name is accepted from JSON.

- [ ] **Step 4: Add capture, match, and local-Service records**

```java
public record TurnRegion(int x, int y, int width, int height) {
}

public record TurnInputSpec(
        Integer x,
        Integer y,
        Integer endX,
        Integer endY,
        Integer scrollDelta,
        String key,
        String text) {
}

public record TurnCaptureSpec(TurnRegion region, ResultMode resultMode) {
    public enum ResultMode { UPLOAD_IMAGE, NO_IMAGE }
}

public record TurnMatchSpec(
        TurnRegion region,
        String templateKey,
        String contentHash,
        double threshold,
        OnMatch onMatch,
        ResultMode resultMode) {
    public enum OnMatch { NONE, CLICK }
    public enum ResultMode { RETURN_MATCH_RESULT, RETURN_MATCH_RESULT_AND_IMAGE }
}

public record TurnLocalServiceCall(
        TurnLocalOperation operation,
        TurnBagOperationArguments bag,
        TurnUiOperationArguments ui,
        TurnGiveItemOperationArguments giveItem,
        TurnQuestOperationArguments quest) {
}

public record TurnBagOperationArguments(
        ReturnItemIntent intent,
        String targetItemTemplate,
        Integer maxBagIndex,
        TurnReturnItemCachePoint cachedPoint,
        String source) {
    public enum ReturnItemIntent { PRESCAN_TASK_PAGE, PRESCAN_FROM_BACK, USE_CACHED_RETURN_ITEM }
}

public record TurnReturnItemCachePoint(
        String templatePath, int clickX, int clickY, long learnedAtMs, String source) {
}

public record TurnUiOperationArguments(String source) {
}

public record TurnGiveItemOperationArguments(String targetItemTemplate, Integer knownBagIndex) {
}

public record TurnQuestOperationArguments(String task, Boolean keepOpen) {
}
```

`TurnBagOperationArguments`, `TurnUiOperationArguments`, `TurnGiveItemOperationArguments`, and
`TurnQuestOperationArguments` are operation-specific typed DTOs. `BAG_USE_INCENSE`, `UI_CLEAN_ALL`, and
`UI_CLOSE_GENERIC_WINDOWS` have a null argument group. The two source-taking UI operations require a nonblank
`source`; Give requires a nonblank template and nullable known index; Quest activate requires nonnull `keepOpen`,
while Quest detail requires it null and returns the image in the single `QUEST_DETAIL` frame. Do not substitute
generic scalar fields, `JsonNode arguments`, or reflection.

- [ ] **Step 5: Add outcome and turn envelope records**

```java
public record TurnOutcome(
        int contractVersion,
        String actionId,
        TurnWindowMetadata window,
        Status status,
        Integer failedStepIndex,
        String code,
        String message,
        List<TurnStepResult> stepResults,
        TurnFrameMetadata frame) {
    public enum Status { COMPLETED, FAILED, STOPPED, DUPLICATE_OR_UNCERTAIN }
}

public record TurnStepResult(
        int index,
        TurnStepType type,
        Status status,
        String code,
        TurnMatchResult match,
        String localResultJson) {
    public enum Status { COMPLETED, FAILED, NOT_RUN }
}

public record TurnMatchResult(
        boolean found,
        double score,
        Integer centerX,
        Integer centerY,
        TurnRegion rectangle) {
}

public record TurnWindowRect(int left, int top, int width, int height) {
}

public record TurnWindowMetadata(
        String deviceId,
        String windowId,
        String windowTitle,
        String nativeHandle,
        long processId,
        TurnWindowRect windowRect,
        boolean stopRequested) {
}

public record TurnFrameMetadata(
        TurnFramePurpose purpose,
        String contentType,
        String sha256,
        int width,
        int height,
        TurnRegion region,
        Integer sourceStepIndex) {
}

public record TurnRequest(
        int contractVersion,
        TurnWindowMetadata window,
        long waitTimeoutMs,
        TurnOutcome previousOutcome) {
}

public record TurnResponse(Status status, TurnAction action) {
    public enum Status { ACTION, IDLE }
}
```

Image bytes never appear in a protocol record.

- [ ] **Step 6: Add one strict validator in each repository**

Create byte-identical:

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`

It exposes:

```java
public final class TurnProtocolValidator {
    public static TurnRequest requireValid(TurnRequest request) { ... }
    public static TurnAction requireValid(TurnAction action) { ... }
    public static TurnOutcome requireValid(TurnOutcome outcome) { ... }
}
```

Required validation includes contract version `1`, nonblank identifiers, nonempty ordered steps with `step.index == list index`, positive dimensions/waits, threshold in `[0.0, 1.0]`, exact field presence per step type, and exact argument shape per local operation.

- [ ] **Step 7: Verify exact-source parity and compile both repositories**

Run a PowerShell SHA256 comparison over the 16 files, then:

```powershell
# D:\mavenProject\DHXY
mvn -q -DskipTests compile

# D:\mavenProject\dhxy-cloud-brain
mvn -q clean package
```

Expected: every protocol file hash matches between repositories; both commands exit `0`.

---

### Task 2: Build The Cloud Single-Slot Turn Exchange

**Files:**
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnExchange.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnCommandPort.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnFrame.java`

**Interfaces:**
- Consumes: Task 1 protocol records.
- Produces:

```java
public interface CloudTurnCommandPort {
    TurnOutcome execute(TurnAction action, Duration timeout);
}
```

- [ ] **Step 1: Implement a package-private single-slot state per device/window**

`CloudTurnExchange` uses `ConcurrentHashMap<WindowKey, WindowTurnState>`. `WindowTurnState` retains only:

```java
private TurnAction currentAction;
private CompletableFuture<TurnOutcome> currentOutcome;
private String lastAcceptedActionId;
private String lastAcceptedOutcomeSha256;
private TurnAction lastResponseAction;
```

There is no history collection, persistent store, owner table, TTL, cleanup timer, or retry executor.

- [ ] **Step 2: Implement synchronous Cloud command submission**

```java
public TurnOutcome execute(TurnAction action, Duration timeout) {
    TurnProtocolValidator.requireValid(action);
    // Atomically reject a second current action for the same device/window.
    // Publish currentAction, signal a waiting HTTP turn, then await currentOutcome once.
    // Timeout returns DUPLICATE_OR_UNCERTAIN and leaves the exact action fenced until its outcome arrives.
}
```

Do not create a replacement action on timeout and do not resend the action internally.

- [ ] **Step 3: Implement client turn acceptance**

Expose a package-private method used only by the HTTP handler:

```java
TurnDelivery exchange(TurnRequest request, CloudTurnFrame frame, Duration wait) {
    // 1. Validate and accept previousOutcome exactly once.
    // 2. If the same outcome is repeated after a lost response, do not fail and do not execute again.
    // 3. Return the exact current/lastResponseAction for that window, or IDLE on wait expiry.
}
```

`CloudTurnFrame` contains PNG bytes plus `TurnFrameMetadata`; it is attached only to the matching previous outcome and never serialized into JSON.

- [ ] **Step 4: Add deterministic outcome hashing**

Use Jackson with sorted properties and map keys to hash the outcome JSON excluding frame bytes. Verify any attached frame bytes against `TurnFrameMetadata.sha256` before completing `currentOutcome`.

- [ ] **Step 5: Review concurrency invariants and package Cloud**

Review these exact cases in source:

1. command before client wait;
2. client wait before command;
3. repeated previous outcome;
4. command timeout followed by late outcome;
5. second command while current action is unresolved;
6. interrupted HTTP wait without physical re-execution.

Run:

```powershell
mvn -q clean package
```

Expected: exit `0`; no host/runtime is started.

---

### Task 3: Add Cloud `/turn` Multipart And Template HTTP Handlers

**Files:**
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnHttpHandler.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\TurnMultipartReader.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTemplateHttpHandler.java`
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnRoutes.java`
- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudBrainServer.java`

**Interfaces:**
- Consumes: `CloudTurnExchange`, `PackagedTemplateAssets`, bearer token.
- Produces: `POST /api/v1/client/turn` and `GET /api/v1/templates/<encoded-key>`.

- [ ] **Step 1: Implement a bounded multipart reader for exactly two parts**

`TurnMultipartReader` accepts:

```text
metadata: Content-Type application/json; max 256 KiB
frame:    Content-Type image/png; max 8 MiB
```

It rejects missing boundary, duplicate names, extra parts, malformed CRLF framing, oversized bodies, non-PNG frame declarations, and trailing bytes. It returns:

```java
record MultipartTurn(TurnRequest request, byte[] frameBytes) { }
```

Do not add a general upload framework or retain uploaded files on disk.

- [ ] **Step 2: Implement the turn handler**

`CloudTurnHttpHandler` accepts only authenticated `POST`. It supports:

- `application/json` for requests without a frame;
- `multipart/form-data` for metadata plus one PNG frame.

It returns `200 application/json` with `TurnResponse`, `400` for invalid contract/body, `401` for bad token, `405` for wrong method, `409` for device/window/action correlation conflict, and `413` for size limits. Long-wait expiry returns `200` with `{ "status": "IDLE", "action": null }`.

- [ ] **Step 3: Implement the template handler**

`CloudTemplateHttpHandler` accepts authenticated `GET /api/v1/templates/<url-encoded-templateKey>` and optional `If-None-Match`.

Implementation rules:

```java
String resourceId = decodedTemplateKey; // must already start images/template/ and end .png
BufferedImage image = assets.loadTemplate(new TemplateId(resourceId)).orElseThrow(...);
byte[] png = encodePng(image);
String etag = "\"sha256:" + sha256(png) + "\"";
```

Return `304` when `If-None-Match` equals the exact ETag, `200 image/png` otherwise, `404` for a missing/invalid key, and never expose a filesystem path or folder listing.

- [ ] **Step 4: Register longest-prefix handlers without changing the legacy JSON gateway**

In `CloudBrainServer.start(...)`, register:

```java
httpServer.createContext("/api/v1/client/turn", turnRoutes.turnHandler());
httpServer.createContext("/api/v1/templates/", turnRoutes.templateHandler());
httpServer.createContext("/", gateway::handle);
```

Create one `CloudTurnExchange` and one `PackagedTemplateAssets` for the new route bundle. Keep the existing `/poll`, `/outcome`, and task-run contexts unchanged in Foundation A.

- [ ] **Step 5: Package Cloud and inspect route registration**

Run:

```powershell
mvn -q clean package
rg -n "api/v1/client/turn|api/v1/templates" src/main/java
```

Expected: package exit `0`; each route is registered once; no server is started.

---

### Task 4: Build The DHXY HTTPS Turn Client And Template Cache

**Files:**
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\HttpsTurnClient.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnMultipartBody.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnExchangeResult.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnTemplateCache.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnTransportException.java`

**Interfaces:**
- Consumes: Task 1 protocol records and optional PNG bytes from Task 5.
- Produces:

```java
public interface TurnClient {
    TurnResponse exchange(TurnRequest request, byte[] optionalPng);
    Path resolveTemplate(String templateKey, String contentHash);
}
```

- [ ] **Step 1: Create one reusable JDK HTTP/2 client**

Configure `HttpClient.Version.HTTP_2`, a positive connect timeout, bearer token, and HTTPS base URI. Reject non-HTTPS base URIs unless host is exactly `127.0.0.1`, `localhost`, or `[::1]`.

- [ ] **Step 2: Implement JSON and multipart turn requests**

When PNG is absent, send `application/json`. When present, `TurnMultipartBody` writes exactly:

```text
--<boundary>\r\n
Content-Disposition: form-data; name="metadata"\r\n
Content-Type: application/json\r\n\r\n
<json>\r\n
--<boundary>\r\n
Content-Disposition: form-data; name="frame"; filename="frame.png"\r\n
Content-Type: image/png\r\n\r\n
<raw png bytes>\r\n
--<boundary>--\r\n
```

Do not use Base64 and do not retry the request internally. An interrupted or uncertain request throws `TurnTransportException` without changing the previous outcome.

- [ ] **Step 3: Implement strict response parsing**

Accept only `200 application/json`, strict contract version `1`, and `ACTION/IDLE`. Validate every received action through `TurnProtocolValidator` before returning it to the loop.

- [ ] **Step 4: Implement hash-addressed template refresh in the existing template tree**

`TurnTemplateCache` is rooted at the configured `images/template` directory. `resolveTemplate(...)`:

1. canonicalizes and verifies the key remains under the root;
2. hashes an existing file and returns it when the exact SHA256 matches;
3. performs one authenticated HTTPS GET when missing/stale;
4. verifies response content type, PNG decode, and SHA256;
5. writes a sibling temporary file and atomically replaces the target;
6. returns typed failure without automatic retry when Cloud is unavailable or hash mismatches.

No restart is required and no sidecar database is created.

- [ ] **Step 5: Compile DHXY**

Run:

```powershell
mvn -q -DskipTests compile
```

Expected: exit `0`; no request is sent because no loop is started.

---

### Task 5: Implement The Exact-Window Local Action Executor

**Files:**
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\LocalTurnActionExecutor.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\LocalTurnStepMapper.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\LocalServiceStepDispatcher.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\ExecutedTurn.java`
- Reuse without broad refactor:
  - `src/main/java/com/bot/dhxy/driver/BoundWindowCaptureService.java`
  - `src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
  - `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
  - `src/main/java/com/bot/dhxy/core/ImageFinder.java`

**Interfaces:**
- Consumes: one validated `TurnAction`.
- Produces:

```java
public record ExecutedTurn(TurnOutcome outcome, byte[] optionalPng) { }

public final class LocalTurnActionExecutor {
    public ExecutedTurn execute(TurnAction action) { ... }
}
```

- [ ] **Step 1: Resolve and refresh the exact bound window once per action**

Use `MultiWindowTaskManager.getRunner(action.windowId())`, obtain `WindowRuntimeContext`, call `WindowNativeBindingRefreshService.refreshAndCommit(...)`, and construct `TurnWindowRect` from the refreshed binding. Reject mismatched/missing native handles before any side effect.

- [ ] **Step 2: Execute steps strictly in list order**

Stop at the first failure. Fill one `TurnStepResult` per declared index; later steps are `NOT_RUN`. On failure, capture the full bound window in the background when `fullWindowFailureEvidence=true` and attach the PNG plus metadata.

- [ ] **Step 3: Implement capture without coordinate scaling**

For a null region, call `captureWindow(binding)`. For an ROI, treat region coordinates as screen-absolute and call `captureRegion(binding, binding.getX(), binding.getY(), x1, y1, x2, y2)`. Encode with `ImageIO.write(..., "png", ...)`; metadata width/height must equal encoded pixels.

- [ ] **Step 4: Implement explicit local template matching**

Resolve the exact template through `TurnTemplateCache`, capture the requested ROI, decode the template, and call:

```java
double[] hit = ImageFinder.find(frame, template, spec.threshold());
```

Convert ROI-relative center to screen-absolute by adding `region.x/region.y`. `onMatch=NONE` returns the result. `onMatch=CLICK` submits one atomic `InputActionQueue.submitAndWaitDetailed(...)` click request before returning. A miss never clicks.

- [ ] **Step 5: Implement keyboard and mouse mechanics**

- `KEY_PRESS`: map only existing `BoundWindowKeyboardService.AltShortcut` values whose `backgroundHwndSupported()` is true. Unsupported values fail with `BACKGROUND_KEY_UNSUPPORTED`; do not focus and fall back.
- `CLICK`: create `InputAction.clickLeft(x, y, delayMs)` and submit through the existing single `InputActionQueue` with the exact window context.
- `WAIT`: use interruptible existing task sleep/checkpoint behavior and return `STOPPED` on interruption.

Do not invoke `InputProvider` directly outside the existing input worker/exclusive boundary.

- [ ] **Step 6: Implement the closed local-Service dispatcher**

Map exactly:

```text
BAG_RETURN_ITEM            -> BagService.runReturnItemMacroDirectForExclusive(...)
BAG_USE_INCENSE            -> BagService.runUseIncenseMacroDirectForExclusive(...)
UI_CLEAN_ALL               -> UICleanerService.cleanUpAll()
UI_CLEAN_LIGHTWEIGHT       -> UICleanerService.cleanLightweightInterruptions(textArgument)
GIVE_ITEM_FROM_OPEN_DIALOG -> GiveItemService.executeGiveDirectForExclusive(textArgument, integerArgument)
QUEST_ACTIVATE             -> QuestManagerService.activateTaskIfPresent(textArgument, booleanArgument)
QUEST_CAPTURE_DETAIL       -> QuestManagerService.captureCurrentQuestDetailForTask(textArgument)
```

The dispatcher returns a small typed JSON string derived from each existing result. It does not accept class names, method names, arbitrary reflection, or another Service.

- [ ] **Step 7: Compile DHXY and statically inspect physical-input ownership**

Run:

```powershell
mvn -q -DskipTests compile
rg -n "InputProvider|clickLeft|pressShortcut|captureWindow|captureRegion" src/main/java/com/bot/dhxy/cloud/turn
```

Expected: compile exit `0`; mouse calls reach `InputActionQueue`; capture uses bound HWND; validated keyboard uses `BoundWindowKeyboardService`.

---

### Task 6: Add The Explicit DHXY Long-Wait Turn Loop

**Files:**
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\WindowTurnLoop.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnLoopRegistry.java`
- Create: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\TurnLoopFactory.java`

**Interfaces:**
- Consumes: `TurnClient`, `LocalTurnActionExecutor`, immutable device/window identity.
- Produces: explicit `start()`, `stop()`, `awaitStopped(Duration)` lifecycle for one window.

- [ ] **Step 1: Implement one loop with previous-outcome carry-forward**

The loop body is exactly:

```java
TurnOutcome previousOutcome = null;
byte[] previousFrame = null;
while (!stopRequested && !Thread.currentThread().isInterrupted()) {
    TurnRequest request = new TurnRequest(1, deviceId, windowId, waitTimeoutMs, previousOutcome);
    TurnResponse response = client.exchange(request, previousFrame);
    if (response.status() == TurnResponse.Status.IDLE) {
        continue;
    }
    ExecutedTurn executed = executor.execute(response.action());
    previousOutcome = executed.outcome();
    previousFrame = executed.optionalPng();
}
```

Clear `previousOutcome/previousFrame` only after Cloud returns a response proving that request was accepted. A transport failure keeps them unchanged for the next connection attempt; it never re-executes `response.action()`.

- [ ] **Step 2: Reject duplicate actions locally without a ledger**

Keep only `lastExecutedActionId` and `lastExecutedTurn` in memory. If Cloud repeats the same action while its outcome is still being carried, return the same `ExecutedTurn`; never invoke the executor again. Reject a different action while the previous outcome is unacknowledged.

- [ ] **Step 3: Keep lifecycle explicit**

`TurnLoopRegistry` may create/remove loops only from an explicit caller. Do not add `@PostConstruct`, Spring auto-start, UI hooks, Task hooks, schedulers, or runtime activation in Foundation A.

- [ ] **Step 4: Compile DHXY and inspect for forbidden machinery**

Run:

```powershell
mvn -q -DskipTests compile
rg -n "@PostConstruct|Scheduled|scheduleAtFixedRate|WebSocket|Socket\(" src/main/java/com/bot/dhxy/cloud/turn
```

Expected: compile exit `0`; scan returns no auto-start/scheduler/WebSocket/raw socket use.

---

### Task 7: Expose One Cloud Command Port Without Reopening The Old Broker

**Files:**
- Create: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnActionFactory.java`
- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnRoutes.java`
- Modify: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudBrainServer.java`

**Interfaces:**
- Consumes: Cloud business-selected action facts and Task 1 protocol.
- Produces: package-private `CloudTurnCommandPort` retained by `CloudTurnRoutes`, not exposed through the HTTP ingress object.

- [ ] **Step 1: Add typed action factories, not generic maps**

`CloudTurnActionFactory` supplies:

```java
TurnAction capture(String actionId, String deviceId, String windowId,
                   TurnRegion region, boolean fullWindowFailureEvidence);

TurnAction input(String actionId, String deviceId, String windowId,
                 List<TurnStep> orderedSteps, boolean fullWindowFailureEvidence);

TurnAction localService(String actionId, String deviceId, String windowId,
                        TurnLocalServiceCall call, boolean fullWindowFailureEvidence);
```

It validates actions before returning them. It contains no business retry or action-id generation; callers provide the stable action ID.

- [ ] **Step 2: Keep command and ingress capabilities separate**

`CloudTurnRoutes` returns an opaque bundle:

```java
public final class Bundle {
    public HttpHandler turnHandler();
    public HttpHandler templateHandler();
    CloudTurnCommandPort commandPort(); // package-private
}
```

The client HTTP handler cannot call `execute(...)`; Cloud business code cannot directly invoke HTTP parsing.

- [ ] **Step 3: Package Cloud and verify no old authority dependency**

Run:

```powershell
mvn -q clean package
rg -n "RemoteGameCommandBroker|CloudTaskRunActionLedger|RemoteTaskRunCoordinator" src/main/java/com/yueyunfe/dhxy/cloudbrain/turn
```

Expected: package exit `0`; scan returns no dependency from the new turn package to the old broker/ledger/coordinator.

---

### Task 8: Complete Foundation Review, Documentation, And Dual Build Gate

**Files:**
- Modify: `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md`
- Modify: `D:\mavenProject\DHXY\docs\PACKAGE_ARCHITECTURE.md`
- Modify: `D:\mavenProject\DHXY\docs\cr-dashboard-data.js` through the generator only
- Create: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-https-turn-foundation-review.md`

**Interfaces:**
- Consumes: Tasks 1-7.
- Produces: reviewable Foundation handoff for the authoritative master-plan cards.

- [ ] **Step 1: Perform a spec-to-source review**

The report must verify:

1. `/turn` is client initiated and long-wait, not short polling;
2. JSON and multipart share one logical endpoint;
3. screenshots are raw PNG bytes and never Base64;
4. actual `windowRect.left/top` and unscaled coordinates are returned;
5. failure stops the payload and returns failed step plus screenshot;
6. repeated action IDs do not execute physical input twice;
7. template hash mismatch downloads and replaces without restart;
8. local matching is explicit per payload;
9. only the seven allowlisted operations reach the four permanent local Services;
10. no new WebSocket/socket/scheduler/owner/session/ledger/TTL/automatic business retry exists.

- [ ] **Step 2: Run final source scans**

```powershell
rg -n "Base64|WebSocket|ServerSocket|new Socket|@Scheduled|@PostConstruct" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn `
  D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn

rg -n "BagService|UICleanerService|GiveItemService|QuestManagerService|Service" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\turn\LocalServiceStepDispatcher.java
```

Expected: no forbidden transport/auto-start pattern; dispatcher contains only the four approved Service classes.

- [ ] **Step 3: Run the stable-window dual build gate**

```powershell
# D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

# D:\mavenProject\DHXY
mvn -q -DskipTests compile
```

Expected: both exit `0`. If concurrent writers are still active, wait for a stable source window; do not build against partially written Java.

- [ ] **Step 4: Record Foundation A status without claiming cutover**

Write CR271 and ACTIVE_WORK entries stating:

- Foundation A source/build status;
- old `/poll + /outcome` remains current until its authoritative replacement/deletion cards pass;
- no runtime/application was started;
- no service migration ledger increment is claimed merely for transport scaffolding;
- exact remaining operation families for the master-plan cards.

Then run:

```powershell
node scripts/generate-cr-dashboard-data.js
```

- [ ] **Step 5: Record the master-plan candidate inventory**

Record these master-plan operation families in the review report:

```text
CAPTURE, EXECUTE_INPUT_BUNDLE
BAG_RETURN_ITEM, BAG_USE_INCENSE, UI_CLEAN, GIVE_ITEM, QUEST operations
BINDING/GEOMETRY/FOCUS/STOP metadata facts
LEFT_TOP_STATUS, AUTO_COMBAT_PANEL, COMMON_BOX, TEAM_RETURN, TASK_TRACKER, BATTLE_RADAR vision facts
NAVIGATION, DIALOG, PLAYER_STATE and remaining non-approved LOCAL_MACRO paths
old poll/outcome/final-consumed/task-run transport-only deletion
```

Do not start a later card automatically; follow the authoritative master plan's READY/dependency/parent-review gate.

---

## Plan Self-Review

- Spec coverage: `/turn`, multipart PNG, exact coordinates, failure evidence, local matching, template refresh, background-capable operations, four permanent local Services, no WebSocket, and Cloud-owned retry are each assigned to a concrete task.
- Scope: this plan intentionally stops before mass Service cutover. Mixing all Service rewrites into Foundation A would recreate the oversized migration that the new protocol is intended to replace.
- Type consistency: `TurnAction`, `TurnRequest`, `TurnResponse`, `TurnOutcome`, `ExecutedTurn`, `TurnClient`, and `CloudTurnCommandPort` have one definition and the same names throughout.
- Placeholder scan: no `TBD`, `TODO`, generic “handle errors”, unnamed tests, or unspecified implementation steps remain.
- Project-rule variance: generic TDD/commit steps are omitted because the repository explicitly forbids unrequested tests and Git mutation. Compile/package and source review are the approved gates.
