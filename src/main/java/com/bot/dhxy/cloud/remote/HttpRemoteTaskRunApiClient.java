package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Synchronous, non-retrying HTTP implementation of the typed lifecycle client. */
public final class HttpRemoteTaskRunApiClient implements RemoteTaskRunApiClient {

    public static final String TASK_RUN_PATH = "/api/cloud/remote/task-run";
    private static final int CONTRACT_VERSION = 1;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final BigInteger MAX_UNSIGNED_LONG = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    /**
     * Fixed bounded timeout for CONFIRM_RESUMED_EXECUTOR_READY only. The readiness send runs
     * synchronously inside the command poll boundary, so it must never inherit an arbitrarily
     * long lifecycle requestTimeout; all other lifecycle actions keep the configured timeout.
     */
    private static final Duration READINESS_REQUEST_TIMEOUT = Duration.ofSeconds(10L);

    private final URI endpoint;
    private final String authorizationValue;
    private final Duration requestTimeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates an inert lifecycle HTTP client. No request is sent by construction.
     *
     * @param baseUrl HTTP(S) cloud-brain base URL without query or fragment
     * @param token bearer credential without the Bearer prefix; never included in failures
     * @param connectTimeout positive TCP connect timeout
     * @param requestTimeout positive timeout for each lifecycle request
     */
    public HttpRemoteTaskRunApiClient(
            String baseUrl,
            String token,
            Duration connectTimeout,
            Duration requestTimeout) {
        this.endpoint = endpointUri(requireText(baseUrl, "baseUrl"));
        this.authorizationValue = "Bearer " + requireText(token, "token");
        this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requirePositive(connectTimeout, "connectTimeout"))
                .build();
    }

    @Override
    public RemoteTaskRunBinding prepare(
            RemoteTaskRunScope scope,
            String startRequestId,
            String taskType,
            RemoteTaskRunWindow window) {
        return execute(RemoteTaskRunAction.PREPARE, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.PREPARE)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .startRequestId(startRequestId)
                .taskType(taskType)
                .window(window)
                .build());
    }

    @Override
    public RemoteTaskRunBinding status(RemoteTaskRunScope scope, String taskRunId) {
        return execute(RemoteTaskRunAction.STATUS, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.STATUS)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .build());
    }

    @Override
    public RemoteTaskRunBinding activate(RemoteTaskRunScope scope, String taskRunId, long expectedRevision) {
        return execute(RemoteTaskRunAction.ACTIVATE, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.ACTIVATE)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .build());
    }

    @Override
    public RemoteTaskRunBinding confirmExecution(
            RemoteTaskRunScope scope,
            String taskRunId,
            long expectedRevision,
            RemoteTaskRunWindow window) {
        return execute(RemoteTaskRunAction.CONFIRM_EXECUTION, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.CONFIRM_EXECUTION)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .window(window)
                .build());
    }

    @Override
    public RemoteTaskRunBinding pause(RemoteTaskRunScope scope, String taskRunId, long expectedRevision) {
        return execute(RemoteTaskRunAction.PAUSE, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.PAUSE)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .build());
    }

    @Override
    public RemoteTaskRunBinding resume(RemoteTaskRunScope scope, String taskRunId, long expectedRevision) {
        return execute(RemoteTaskRunAction.RESUME, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.RESUME)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .build());
    }

    @Override
    public RemoteTaskRunBinding stop(RemoteTaskRunScope scope, String taskRunId, long expectedRevision) {
        return execute(RemoteTaskRunAction.STOP, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.STOP)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .build());
    }

    @Override
    public RemoteTaskRunBinding complete(RemoteTaskRunScope scope, String taskRunId, long expectedRevision) {
        return execute(RemoteTaskRunAction.COMPLETE, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.COMPLETE)
                .tenantId(scope == null ? null : scope.getTenantId())
                .userId(scope == null ? null : scope.getUserId())
                .deviceId(scope == null ? null : scope.getDeviceId())
                .clientSessionId(scope == null ? null : scope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .build());
    }

    @Override
    public RemoteTaskRunBinding findReplacement(
            RemoteTaskRunScope replacementScope,
            String startRequestId) {
        return execute(RemoteTaskRunAction.FIND_REPLACEMENT, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.FIND_REPLACEMENT)
                .tenantId(replacementScope == null ? null : replacementScope.getTenantId())
                .userId(replacementScope == null ? null : replacementScope.getUserId())
                .deviceId(replacementScope == null ? null : replacementScope.getDeviceId())
                .clientSessionId(replacementScope == null ? null : replacementScope.getClientSessionId())
                .startRequestId(startRequestId)
                .build());
    }

    @Override
    public RemoteTaskRunBinding stopReplacement(
            RemoteTaskRunScope replacementScope,
            String taskRunId,
            long expectedRevision) {
        return execute(RemoteTaskRunAction.STOP_REPLACEMENT, RemoteTaskRunActionRequest.builder()
                .contractVersion(CONTRACT_VERSION)
                .action(RemoteTaskRunAction.STOP_REPLACEMENT)
                .tenantId(replacementScope == null ? null : replacementScope.getTenantId())
                .userId(replacementScope == null ? null : replacementScope.getUserId())
                .deviceId(replacementScope == null ? null : replacementScope.getDeviceId())
                .clientSessionId(replacementScope == null ? null : replacementScope.getClientSessionId())
                .taskRunId(taskRunId)
                .expectedRevision(expectedRevision)
                .build());
    }

    @Override
    public RemoteTaskRunReceipt confirmResumedExecutorReady(RemoteTaskRunActionRequest request) {
        RemoteTaskRunActionResponse response = executeResponse(
                RemoteTaskRunAction.CONFIRM_RESUMED_EXECUTOR_READY, request);
        return response.getReceipt();
    }

    private RemoteTaskRunBinding execute(
            RemoteTaskRunAction expectedAction,
            RemoteTaskRunActionRequest request) {
        return executeResponse(expectedAction, request).getBinding();
    }

    private RemoteTaskRunActionResponse executeResponse(
            RemoteTaskRunAction expectedAction,
            RemoteTaskRunActionRequest request) {
        validateRequest(expectedAction, request);
        Duration effectiveTimeout =
                expectedAction == RemoteTaskRunAction.CONFIRM_RESUMED_EXECUTOR_READY
                        ? READINESS_REQUEST_TIMEOUT
                        : requestTimeout;
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw failure(
                    RemoteTaskRunClientException.FailureType.SERIALIZATION,
                    null, null, null, null,
                    "lifecycle request serialization failed: " + e.getOriginalMessage(), e);
        }

        HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(effectiveTimeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(AUTHORIZATION_HEADER, authorizationValue)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
        } catch (RuntimeException e) {
            throw invalidRequest(
                    "lifecycle HTTP request could not be constructed; request was not sent ("
                            + e.getClass().getSimpleName() + ")");
        }
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw failure(
                    RemoteTaskRunClientException.FailureType.HTTP_TIMEOUT,
                    null, null, null, null,
                    "lifecycle request timed out after " + effectiveTimeout.toMillis() + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure(
                    RemoteTaskRunClientException.FailureType.INTERRUPTED,
                    null, null, null, null,
                    "lifecycle request was interrupted", e);
        } catch (IOException e) {
            throw failure(
                    RemoteTaskRunClientException.FailureType.IO,
                    null, null, null, null,
                    "lifecycle request I/O failed: " + e.getClass().getSimpleName(), e);
        }

        String responseBody = httpResponse.body();
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw httpStatusFailure(httpResponse.statusCode(), responseBody);
        }
        if (responseBody == null || responseBody.isBlank()) {
            throw failure(
                    RemoteTaskRunClientException.FailureType.EMPTY_RESPONSE,
                    httpResponse.statusCode(), responseBody, null, null,
                    "lifecycle endpoint returned an empty response", null);
        }

        JsonNode responseTree;
        try {
            responseTree = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw failure(
                    RemoteTaskRunClientException.FailureType.SCHEMA_MISMATCH,
                    httpResponse.statusCode(), responseBody, null, null,
                    "lifecycle response JSON syntax is invalid: " + e.getOriginalMessage(), e);
        }
        validateResponseTree(expectedAction, responseTree, responseBody);
        RemoteTaskRunActionResponse response;
        try {
            response = objectMapper.treeToValue(responseTree, RemoteTaskRunActionResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw failure(
                    RemoteTaskRunClientException.FailureType.SCHEMA_MISMATCH,
                    httpResponse.statusCode(), responseBody, null, null,
                    "lifecycle response schema mismatch: " + e.getMessage(), e);
        }
        validateResponse(expectedAction, request, response, responseBody);
        return response;
    }

    private void validateRequest(
            RemoteTaskRunAction expectedAction,
            RemoteTaskRunActionRequest request) {
        if (request == null || request.getContractVersion() != CONTRACT_VERSION) {
            throw invalidRequest("lifecycle contractVersion must be 1");
        }
        if (request.getAction() != expectedAction) {
            throw invalidRequest("lifecycle request action mismatch");
        }
        requireNormalizedText(request.getTenantId(), "tenantId");
        requireNormalizedText(request.getUserId(), "userId");
        requireNormalizedText(request.getDeviceId(), "deviceId");
        requireNormalizedText(request.getClientSessionId(), "clientSessionId");
        switch (expectedAction) {
            case PREPARE -> {
                requireNormalizedText(request.getStartRequestId(), "startRequestId");
                requireOriginalText(request.getTaskType(), "taskType");
                validateRequestWindow(request.getWindow(), "request.window");
            }
            case STATUS -> requireNormalizedText(request.getTaskRunId(), "taskRunId");
            case ACTIVATE, PAUSE, RESUME, STOP, COMPLETE, STOP_REPLACEMENT -> {
                requireNormalizedText(request.getTaskRunId(), "taskRunId");
                if (request.getExpectedRevision() == null || request.getExpectedRevision() < 0L) {
                    throw invalidRequest("expectedRevision must be non-negative");
                }
            }
            case CONFIRM_EXECUTION -> {
                requireNormalizedText(request.getTaskRunId(), "taskRunId");
                if (request.getExpectedRevision() == null || request.getExpectedRevision() < 0L) {
                    throw invalidRequest("expectedRevision must be non-negative");
                }
                validateRequestWindow(request.getWindow(), "request.window");
            }
            case CONFIRM_RESUMED_EXECUTOR_READY -> {
                requireNormalizedText(request.getTaskRunId(), "taskRunId");
                requireNormalizedText(request.getRequestId(), "requestId");
                requireSha256(request.getRequestDigest(), "requestDigest");
                validateReadinessFact(request.getFact());
                RemoteProtocolDigests digests = new RemoteProtocolDigests();
                if (!digests.computeResumeFactDigest(request.getFact())
                        .equals(request.getFact().getFactDigest())
                        || !digests.computeTaskRunActionDigest(request)
                        .equals(request.getRequestDigest())) {
                    throw invalidRequest("readiness digest mismatch");
                }
            }
            case FIND_REPLACEMENT -> requireNormalizedText(
                    request.getStartRequestId(), "startRequestId");
        }
    }

    private void validateResponseTree(
            RemoteTaskRunAction expectedAction,
            JsonNode root,
            String responseBody) {
        if (root == null || !root.isObject()) {
            throw schemaMismatch(responseBody, "lifecycle response root must be an object");
        }
        JsonNode contractVersion = root.get("contractVersion");
        if (contractVersion == null
                || !contractVersion.isIntegralNumber()
                || !contractVersion.canConvertToInt()
                || contractVersion.intValue() != CONTRACT_VERSION) {
            throw schemaMismatch(responseBody, "contractVersion must be the integer 1");
        }
        JsonNode action = root.get("action");
        if (action == null
                || !action.isTextual()
                || !expectedAction.name().equals(action.textValue())) {
            throw schemaMismatch(responseBody, "response action must match " + expectedAction);
        }
        JsonNode success = root.get("success");
        if (success == null || !success.isBoolean()) {
            throw schemaMismatch(responseBody, "success must be a required boolean");
        }

        JsonNode error = root.get("error");
        JsonNode binding = root.get("binding");
        JsonNode receipt = root.get("receipt");
        if (success.booleanValue()) {
            if (error != null && !error.isNull()) {
                throw schemaMismatch(responseBody, "success response must omit error");
            }
            if (expectedAction == RemoteTaskRunAction.CONFIRM_RESUMED_EXECUTOR_READY) {
                if (binding != null && !binding.isNull()) {
                    throw schemaMismatch(responseBody, "readiness response must omit binding");
                }
                validateReceiptTree(receipt, responseBody);
                return;
            }
            if (binding == null || !binding.isObject() || (receipt != null && !receipt.isNull())) {
                throw schemaMismatch(responseBody, "success response requires only a binding");
            }
            validateBindingTree(binding, responseBody);
            return;
        }

        if (binding != null && !binding.isNull()) {
            throw schemaMismatch(responseBody, "failure response must omit binding");
        }
        if (error == null || !error.isObject()) {
            throw schemaMismatch(responseBody, "failure response requires an object error");
        }
        requireTextField(error, "code", "error.code", responseBody);
        requireTextField(error, "message", "error.message", responseBody);
    }

    private void validateReceiptTree(JsonNode receipt, String responseBody) {
        if (receipt == null || !receipt.isObject()) {
            throw schemaMismatch(responseBody, "readiness response requires receipt");
        }
        for (String field : java.util.List.of("taskRunId", "receiptId", "requestId",
                "requestDigest", "factDigest")) {
            requireTextField(receipt, field, "receipt." + field, responseBody);
        }
        requireIntegralField(receipt, "confirmedRunRevision",
                "receipt.confirmedRunRevision", responseBody);
        requireIntegralField(receipt, "recordedAtEpochMs", "receipt.recordedAtEpochMs", responseBody);
    }

    private void validateReadinessFact(ResumeExecutorReadinessFact fact) {
        if (fact == null) {
            throw invalidRequest("readiness fact is required");
        }
        requireOriginalText(fact.getTaskType(), "fact.taskType");
        requireNormalizedText(fact.getWindowId(), "fact.windowId");
        requireNormalizedText(fact.getNativeHandle(), "fact.nativeHandle");
        requireSha256(fact.getFactDigest(), "fact.factDigest");
        if (fact.getProcessId() <= 0L || fact.getPlayerIdentityEpoch() < 0L
                || fact.getStopEpoch() < 0L || fact.getResumedFromRunRevision() < 0L
                || fact.getNewActiveRunRevision() < 0L || fact.getLocalRegistrationGeneration() <= 0L
                || fact.getPauseTokenMechanicalGeneration() < 0L
                || fact.getOperationLedgerRevision() < 0L || fact.getObservedAtEpochMs() <= 0L
                || fact.getInFlightCaptureCount() != 0L || fact.getInFlightFactCount() != 0L
                || fact.getInFlightInputCount() != 0L
                || !"ACTIVE".equals(fact.getLocalRegistrationStatus())
                || !"PAUSED".equals(fact.getPreviousLocalStatus())
                || !"REGISTRY_RESUME_PUBLISH".equals(fact.getProducer())) {
            throw invalidRequest("invalid mechanical readiness fact");
        }
    }

    private void requireSha256(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw invalidRequest(field + " must be lowercase SHA-256 hex");
        }
    }

    private void validateBindingTree(JsonNode binding, String responseBody) {
        JsonNode scope = requireObjectField(
                binding, "scope", "binding.scope", responseBody);
        requireTextField(scope, "tenantId", "binding.scope.tenantId", responseBody);
        requireTextField(scope, "userId", "binding.scope.userId", responseBody);
        requireTextField(scope, "deviceId", "binding.scope.deviceId", responseBody);
        requireTextField(
                scope, "clientSessionId", "binding.scope.clientSessionId", responseBody);
        requireTextField(binding, "taskRunId", "binding.taskRunId", responseBody);
        requireTextField(binding, "startRequestId", "binding.startRequestId", responseBody);
        requireTextField(binding, "taskType", "binding.taskType", responseBody);

        JsonNode window = requireObjectField(
                binding, "window", "binding.window", responseBody);
        requireTextField(window, "windowId", "binding.window.windowId", responseBody);
        requireTextField(
                window, "nativeHandle", "binding.window.nativeHandle", responseBody);
        requireIntegralField(
                window, "processId", "binding.window.processId", responseBody);
        requireIntegralField(
                window,
                "playerIdentityEpoch",
                "binding.window.playerIdentityEpoch",
                responseBody);
        requireIntegralField(binding, "stopEpoch", "binding.stopEpoch", responseBody);
        requireIntegralField(binding, "runRevision", "binding.runRevision", responseBody);
        requireTextField(binding, "status", "binding.status", responseBody);
    }

    private JsonNode requireObjectField(
            JsonNode parent,
            String field,
            String path,
            String responseBody) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()) {
            throw schemaMismatch(responseBody, path + " must be a required object");
        }
        return value;
    }

    private JsonNode requireTextField(
            JsonNode parent,
            String field,
            String path,
            String responseBody) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isTextual()) {
            throw schemaMismatch(responseBody, path + " must be a required string");
        }
        return value;
    }

    private JsonNode requireIntegralField(
            JsonNode parent,
            String field,
            String path,
            String responseBody) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw schemaMismatch(responseBody, path + " must be a required 64-bit integer");
        }
        return value;
    }

    private void validateResponse(
            RemoteTaskRunAction expectedAction,
            RemoteTaskRunActionRequest request,
            RemoteTaskRunActionResponse response,
            String responseBody) {
        if (response == null
                || response.getContractVersion() != CONTRACT_VERSION
                || response.getAction() != expectedAction) {
            throw schemaMismatch(responseBody, "lifecycle response contractVersion/action mismatch");
        }
        if (response.isSuccess()) {
            if (response.getError() != null) {
                throw schemaMismatch(responseBody, "success response must omit error");
            }
            if (expectedAction == RemoteTaskRunAction.CONFIRM_RESUMED_EXECUTOR_READY) {
                RemoteTaskRunReceipt receipt = response.getReceipt();
                if (response.getBinding() != null || receipt == null
                        || !request.getTaskRunId().equals(receipt.getTaskRunId())
                        || !request.getRequestId().equals(receipt.getRequestId())
                        || !request.getRequestDigest().equals(receipt.getRequestDigest())
                        || !request.getFact().getFactDigest().equals(receipt.getFactDigest())
                        || request.getFact().getNewActiveRunRevision()
                                != receipt.getConfirmedRunRevision()) {
                    throw schemaMismatch(responseBody, "readiness receipt does not correlate");
                }
                return;
            }
            if (response.getBinding() == null || response.getReceipt() != null) {
                throw schemaMismatch(responseBody, "success response requires binding and no receipt");
            }
            RemoteTaskRunBinding binding = response.getBinding();
            validateBinding(binding, responseBody);
            if (expectedAction == RemoteTaskRunAction.CONFIRM_EXECUTION
                    && (!binding.getScope().getTenantId().equals(request.getTenantId())
                    || !binding.getScope().getUserId().equals(request.getUserId())
                    || !binding.getScope().getDeviceId().equals(request.getDeviceId())
                    || !binding.getScope().getClientSessionId().equals(request.getClientSessionId())
                    || !binding.getTaskRunId().equals(request.getTaskRunId())
                    || !binding.getWindow().equals(request.getWindow())
                    || binding.getStatus() != RemoteTaskRunWireStatus.ACTIVE
                    || binding.getRunRevision() != request.getExpectedRevision())) {
                throw schemaMismatch(
                        responseBody,
                        "CONFIRM_EXECUTION response binding must exactly match request "
                                + "scope/taskRunId/window with ACTIVE status and runRevision "
                                + "equal to expectedRevision");
            }
            return;
        }
        if (response.getBinding() != null
                || response.getError() == null
                || response.getError().getCode() == null
                || response.getError().getMessage() == null
                || response.getError().getMessage().isBlank()) {
            throw schemaMismatch(responseBody, "failure response requires a typed error and no binding");
        }
        throw failure(
                RemoteTaskRunClientException.FailureType.REMOTE_REJECTED,
                200,
                responseBody,
                response.getError().getCode().name(),
                response.getError().getMessage(),
                "lifecycle endpoint rejected " + expectedAction
                        + " code=" + response.getError().getCode(),
                null);
    }

    private void validateBinding(RemoteTaskRunBinding binding, String responseBody) {
        if (binding == null || binding.getScope() == null || binding.getWindow() == null) {
            throw schemaMismatch(responseBody, "lifecycle binding requires nested scope and window");
        }
        requireResponseText(
                binding.getScope().getTenantId(), "binding.scope.tenantId", responseBody, true);
        requireResponseText(
                binding.getScope().getUserId(), "binding.scope.userId", responseBody, true);
        requireResponseText(
                binding.getScope().getDeviceId(), "binding.scope.deviceId", responseBody, true);
        requireResponseText(
                binding.getScope().getClientSessionId(),
                "binding.scope.clientSessionId",
                responseBody,
                true);
        requireResponseText(binding.getTaskRunId(), "binding.taskRunId", responseBody, true);
        requireResponseText(binding.getStartRequestId(), "binding.startRequestId", responseBody, true);
        requireResponseText(binding.getTaskType(), "binding.taskType", responseBody, false);
        validateResponseWindow(binding.getWindow(), "binding.window", responseBody);
        if (binding.getStopEpoch() < 0L || binding.getRunRevision() < 0L) {
            throw schemaMismatch(responseBody, "binding epochs and revision must be non-negative");
        }
        if (binding.getStatus() == null) {
            throw schemaMismatch(responseBody, "binding.status is required");
        }
    }

    private void validateRequestWindow(RemoteTaskRunWindow window, String field) {
        if (window == null) {
            throw invalidRequest(field + " is required");
        }
        requireNormalizedText(window.getWindowId(), field + ".windowId");
        String nativeHandle = requireNormalizedText(window.getNativeHandle(), field + ".nativeHandle");
        if (!isNormalizedNativeHandle(nativeHandle)) {
            throw invalidRequest(field + ".nativeHandle must be normalized unsigned 64-bit decimal");
        }
        if (window.getProcessId() <= 0L || window.getPlayerIdentityEpoch() < 0L) {
            throw invalidRequest(field + " processId/identityEpoch are invalid");
        }
    }

    private void validateResponseWindow(
            RemoteTaskRunWindow window,
            String field,
            String responseBody) {
        if (window == null) {
            throw schemaMismatch(responseBody, field + " is required");
        }
        requireResponseText(window.getWindowId(), field + ".windowId", responseBody, true);
        String nativeHandle = requireResponseText(
                window.getNativeHandle(), field + ".nativeHandle", responseBody, true);
        if (!isNormalizedNativeHandle(nativeHandle)) {
            throw schemaMismatch(
                    responseBody,
                    field + ".nativeHandle must be normalized unsigned 64-bit decimal");
        }
        if (window.getProcessId() <= 0L || window.getPlayerIdentityEpoch() < 0L) {
            throw schemaMismatch(responseBody, field + " processId/identityEpoch are invalid");
        }
    }

    private String requireResponseText(
            String value,
            String field,
            String responseBody,
            boolean normalized) {
        if (value == null || value.isBlank()) {
            throw schemaMismatch(responseBody, field + " must not be blank");
        }
        if (normalized && !value.equals(value.trim())) {
            throw schemaMismatch(responseBody, field + " must be normalized");
        }
        return value;
    }

    private static boolean isNormalizedNativeHandle(String value) {
        return ("0".equals(value) || value.matches("[1-9][0-9]*"))
                && new BigInteger(value).compareTo(MAX_UNSIGNED_LONG) <= 0;
    }

    private RemoteTaskRunClientException httpStatusFailure(int statusCode, String responseBody) {
        String remoteCode = null;
        String remoteMessage = null;
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode error = root == null ? null : root.path("error");
                if (error != null && error.isObject()) {
                    remoteCode = nullableText(error.get("code"));
                    remoteMessage = nullableText(error.get("message"));
                }
            } catch (JsonProcessingException ignored) {
                // Raw body remains available on the exception.
            }
        }
        return failure(
                RemoteTaskRunClientException.FailureType.HTTP_STATUS,
                statusCode,
                responseBody,
                remoteCode,
                remoteMessage,
                "lifecycle endpoint returned HTTP " + statusCode,
                null);
    }

    private RemoteTaskRunClientException invalidRequest(String message) {
        return failure(
                RemoteTaskRunClientException.FailureType.INVALID_REQUEST,
                null, null, null, null, message, null);
    }

    private RemoteTaskRunClientException schemaMismatch(String responseBody, String message) {
        return failure(
                RemoteTaskRunClientException.FailureType.SCHEMA_MISMATCH,
                200, responseBody, null, null, message, null);
    }

    private RemoteTaskRunClientException failure(
            RemoteTaskRunClientException.FailureType failureType,
            Integer statusCode,
            String responseBody,
            String remoteErrorCode,
            String remoteErrorMessage,
            String message,
            Throwable cause) {
        return new RemoteTaskRunClientException(
                failureType,
                endpoint,
                statusCode,
                responseBody,
                remoteErrorCode,
                remoteErrorMessage,
                message,
                cause);
    }

    private String requireNormalizedText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(field + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw invalidRequest(field + " must be normalized");
        }
        return value;
    }

    private String requireOriginalText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(field + " must not be blank");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static Duration requirePositive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static URI endpointUri(String baseUrl) {
        URI base;
        try {
            base = URI.create(baseUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("baseUrl is not a valid URI", e);
        }
        if (base.getScheme() == null
                || (!base.getScheme().equalsIgnoreCase("http")
                && !base.getScheme().equalsIgnoreCase("https"))
                || base.getHost() == null
                || base.getRawQuery() != null
                || base.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "baseUrl must be an HTTP(S) URI without query or fragment");
        }
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return URI.create(normalized + TASK_RUN_PATH);
    }

    private static String nullableText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
