package com.bot.dhxy.cloud.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class HttpRemoteCommandTransport implements RemoteCommandTransport {

    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:18080";
    public static final String POLL_PATH = "/api/cloud/remote/poll";
    public static final String OUTCOME_PATH = "/api/cloud/remote/outcome";
    public static final String FINAL_CONSUMED_RECEIPT_PATH =
            OUTCOME_PATH + "/final-consumed-receipt";
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 60_000L;
    public static final long DEFAULT_REQUEST_TIMEOUT_MS = 60_000L;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Duration POLL_TIMEOUT_GRACE = Duration.ofSeconds(5L);

    private final URI pollEndpoint;
    private final URI outcomeEndpoint;
    private final URI finalConsumedReceiptEndpoint;
    private final String authorizationValue;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final RemoteProtocolDigests protocolDigests;

    public HttpRemoteCommandTransport(String token) {
        this(DEFAULT_BASE_URL, token);
    }

    public HttpRemoteCommandTransport(String baseUrl, String token) {
        this(
                baseUrl,
                token,
                Duration.ofMillis(DEFAULT_CONNECT_TIMEOUT_MS),
                Duration.ofMillis(DEFAULT_REQUEST_TIMEOUT_MS));
    }

    /**
     * Creates a synchronous JSON transport with explicit connection and per-request timeouts.
     *
     * @param baseUrl HTTP(S) cloud-brain base URL, normally the same value as cloud.base-url
     * @param token bearer token without the Bearer prefix
     * @param connectTimeout positive TCP connection timeout
     * @param requestTimeout positive minimum HTTP request timeout; long polls add a five-second grace when needed
     */
    public HttpRemoteCommandTransport(
            String baseUrl,
            String token,
            Duration connectTimeout,
            Duration requestTimeout) {
        String normalizedBaseUrl = requireText(baseUrl, "baseUrl");
        String normalizedToken = requireText(token, "token");
        Duration normalizedConnectTimeout = requirePositive(connectTimeout, "connectTimeout");
        this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
        this.pollEndpoint = endpointUri(normalizedBaseUrl, POLL_PATH);
        this.outcomeEndpoint = endpointUri(normalizedBaseUrl, OUTCOME_PATH);
        this.finalConsumedReceiptEndpoint = endpointUri(
                normalizedBaseUrl, FINAL_CONSUMED_RECEIPT_PATH);
        this.authorizationValue = "Bearer " + normalizedToken;
        this.objectMapper = JsonMapper.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        this.protocolDigests = new RemoteProtocolDigests();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(normalizedConnectTimeout)
                .build();
    }

    @Override
    public RemoteCommandPollResponse poll(RemoteCommandPollRequest request) {
        validatePollRequest(request);
        String responseBody = postJson(
                pollEndpoint,
                serialize(request, pollEndpoint),
                pollRequestTimeout(request.getWaitTimeoutMs()));
        if (responseBody == null || responseBody.isBlank()) {
            throw failure(
                    RemoteCommandTransportException.FailureType.EMPTY_RESPONSE,
                    pollEndpoint,
                    null,
                    responseBody,
                    null,
                    null,
                    "empty poll response",
                    null);
        }

        RemoteCommandPollResponse response = deserializePollResponse(responseBody);
        validatePollResponse(response, request);
        return response;
    }

    @Override
    public RemoteCommandOutcomeAck submitOutcome(RemoteGameOutcomeEnvelope outcome) {
        validateOutcome(outcome);
        String responseBody = postJson(
                outcomeEndpoint,
                serialize(outcome, outcomeEndpoint),
                requestTimeout);
        if (responseBody == null || responseBody.isBlank()) {
            throw failure(
                    RemoteCommandTransportException.FailureType.EMPTY_RESPONSE,
                    outcomeEndpoint,
                    null,
                    responseBody,
                    null,
                    null,
                    "empty outcome acknowledgement",
                    null);
        }
        RemoteCommandOutcomeAck acknowledgement = deserializeOutcomeAck(responseBody);
        validateOutcomeAck(acknowledgement, outcome, responseBody);
        return acknowledgement;
    }

    @Override
    public RemoteFinalConsumedReceiptAck submitFinalConsumedReceipt(
            RemoteFinalConsumedReceipt receipt) {
        validateFinalConsumedReceipt(receipt);
        String responseBody = postJson(
                finalConsumedReceiptEndpoint,
                serialize(receipt, finalConsumedReceiptEndpoint),
                requestTimeout);
        if (responseBody == null || responseBody.isBlank()) {
            throw failure(
                    RemoteCommandTransportException.FailureType.EMPTY_RESPONSE,
                    finalConsumedReceiptEndpoint, null, responseBody, null, null,
                    "empty final-consumed receipt acknowledgement", null);
        }
        RemoteFinalConsumedReceiptAck acknowledgement;
        try {
            acknowledgement = objectMapper.readValue(
                    responseBody, RemoteFinalConsumedReceiptAck.class);
        } catch (JsonProcessingException e) {
            throw failure(
                    RemoteCommandTransportException.FailureType.DESERIALIZATION,
                    finalConsumedReceiptEndpoint, 200, responseBody, null, null,
                    "final-consumed receipt acknowledgement deserialization failure: "
                            + e.getOriginalMessage(), e);
        }
        validateFinalConsumedReceiptAck(acknowledgement, receipt, responseBody);
        return acknowledgement;
    }

    private String postJson(URI endpoint, String body, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(AUTHORIZATION_HEADER, authorizationValue)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw failure(
                    RemoteCommandTransportException.FailureType.HTTP_TIMEOUT,
                    endpoint,
                    null,
                    null,
                    null,
                    null,
                    "request timeout after " + timeout.toMillis() + "ms",
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure(
                    RemoteCommandTransportException.FailureType.INTERRUPTED,
                    endpoint,
                    null,
                    null,
                    null,
                    null,
                    "interrupted during http request",
                    e);
        } catch (IOException e) {
            throw failure(
                    RemoteCommandTransportException.FailureType.IO,
                    endpoint,
                    null,
                    null,
                    null,
                    null,
                    "http failure: " + e.getClass().getSimpleName(),
                    e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw httpStatusFailure(endpoint, response.statusCode(), response.body());
        }
        return response.body();
    }

    private String serialize(Object value, URI endpoint) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw failure(
                    RemoteCommandTransportException.FailureType.SERIALIZATION,
                    endpoint,
                    null,
                    null,
                    null,
                    null,
                    "json serialization failure: " + e.getOriginalMessage(),
                    e);
        }
    }

    private RemoteCommandPollResponse deserializePollResponse(String body) {
        try {
            return objectMapper.readValue(body, RemoteCommandPollResponse.class);
        } catch (JsonProcessingException e) {
            throw failure(
                    RemoteCommandTransportException.FailureType.DESERIALIZATION,
                    pollEndpoint,
                    null,
                    body,
                    null,
                    null,
                    "json deserialization failure: " + e.getOriginalMessage(),
                    e);
        }
    }

    private RemoteCommandOutcomeAck deserializeOutcomeAck(String body) {
        try {
            return objectMapper.readValue(body, RemoteCommandOutcomeAck.class);
        } catch (JsonProcessingException e) {
            throw failure(
                    RemoteCommandTransportException.FailureType.DESERIALIZATION,
                    outcomeEndpoint,
                    200,
                    body,
                    null,
                    null,
                    "outcome acknowledgement deserialization failure: " + e.getOriginalMessage(),
                    e);
        }
    }

    private void validateOutcomeAck(
            RemoteCommandOutcomeAck acknowledgement,
            RemoteGameOutcomeEnvelope outcome,
            String responseBody) {
        if (acknowledgement == null
                || acknowledgement.getStatus() == null
                || acknowledgement.getCode() == null) {
            throw failure(
                    RemoteCommandTransportException.FailureType.SCHEMA_MISMATCH,
                    outcomeEndpoint,
                    200,
                    responseBody,
                    null,
                    null,
                    "outcome acknowledgement requires status and code",
                    null);
        }
        if (acknowledgement.getStatus() == RemoteCommandOutcomeAckStatus.REJECTED) {
            throw failure(
                    RemoteCommandTransportException.FailureType.OUTCOME_REJECTED,
                    outcomeEndpoint,
                    200,
                    responseBody,
                    acknowledgement.getCode().name(),
                    acknowledgement.getMessage(),
                    "remote endpoint rejected outcome requestId=" + acknowledgement.getRequestId(),
                    null);
        }
        if (!Objects.equals(outcome.getRequestId(), acknowledgement.getRequestId())) {
            throw failure(
                    RemoteCommandTransportException.FailureType.SCHEMA_MISMATCH,
                    outcomeEndpoint,
                    200,
                    responseBody,
                    acknowledgement.getCode().name(),
                    acknowledgement.getMessage(),
                    "outcome acknowledgement requestId mismatch",
                    null);
        }
        if (acknowledgement.getCode() != outcome.getCode()) {
            throw failure(
                    RemoteCommandTransportException.FailureType.SCHEMA_MISMATCH,
                    outcomeEndpoint,
                    200,
                    responseBody,
                    acknowledgement.getCode().name(),
                    acknowledgement.getMessage(),
                    "outcome acknowledgement code mismatch",
                    null);
        }
    }

    private RemoteCommandTransportException httpStatusFailure(URI endpoint, int statusCode, String body) {
        String serverCode = null;
        String serverMessage = null;
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode error = root != null && root.path("error").isObject()
                        ? root.path("error")
                        : root;
                if (error != null && error.isObject()) {
                    serverCode = nullableText(error.get("code"));
                    serverMessage = nullableText(error.get("message"));
                }
            } catch (JsonProcessingException ignored) {
                // The raw body remains available on the structured exception.
            }
        }
        String detail = serverCode == null ? "" : " code=" + serverCode;
        return failure(
                RemoteCommandTransportException.FailureType.HTTP_STATUS,
                endpoint,
                statusCode,
                body,
                serverCode,
                serverMessage,
                "remote endpoint returned http status=" + statusCode + detail,
                null);
    }

    private static void validatePollRequest(RemoteCommandPollRequest request) {
        if (request == null) {
            throw invalidRequest("poll request is required");
        }
        if (request.getContractVersion() != 1) {
            throw invalidRequest("poll contractVersion must be 1");
        }
        requireWireText(request.getTenantId(), "poll.tenantId");
        requireWireText(request.getUserId(), "poll.userId");
        requireWireText(request.getDeviceId(), "poll.deviceId");
        requireWireText(request.getClientSessionId(), "poll.clientSessionId");
        try {
            RemoteCommandPollRequest.validateWaitTimeout(request.getWaitTimeoutMs());
        } catch (IllegalArgumentException e) {
            throw invalidRequest(e.getMessage());
        }
    }

    private void validatePollResponse(
            RemoteCommandPollResponse response,
            RemoteCommandPollRequest request) {
        if (response == null || response.getStatus() == null) {
            throw schemaMismatch("poll response status is required");
        }
        requireSchemaText(response.getCloudIncarnationId(),
                "poll response cloudIncarnationId");
        if (response.getRetryAfterMs() < 0L) {
            throw schemaMismatch("poll response retryAfterMs must not be negative");
        }
        if (response.getStatus() == RemoteCommandPollStatus.IDLE) {
            if (response.getCommand() != null || response.getFinalConsumedAck() != null) {
                throw schemaMismatch("IDLE poll response must not include a payload");
            }
            return;
        }
        if (response.getStatus() == RemoteCommandPollStatus.COMMAND) {
            if (response.getCommand() == null || response.getFinalConsumedAck() != null) {
                throw schemaMismatch(
                        "COMMAND poll response requires only a command payload");
            }
            validateCommand(response.getCommand());
            return;
        }
        if (response.getCommand() != null || response.getFinalConsumedAck() == null) {
            throw schemaMismatch(
                    "FINAL_CONSUMED poll response requires only a finalConsumedAck payload");
        }
        validateFinalConsumedAck(response.getFinalConsumedAck(), request);
    }

    private static void validateCommand(RemoteGameCommand command) {
        if (command.getContractVersion() != 1) {
            throw schemaMismatch("command contractVersion must be 1");
        }
        if (command.getOperation() == null) {
            throw schemaMismatch("command operation is required");
        }
        requireSchemaText(command.getRequestId(), "command.requestId");
        requireSchemaText(command.getActionId(), "command.actionId");
        requireSchemaText(command.getTaskRunId(), "command.taskRunId");
        if (command.getRunRevision() == null) {
            throw schemaMismatch("command.runRevision is required");
        }
        if (command.getRunRevision() < 0L) {
            throw schemaMismatch("command.runRevision must not be negative");
        }
        // Unknown enum values and explicit null already failed deserialization (DESERIALIZATION).
        // A structurally valid marker on an input bundle is the SCHEMA_MISMATCH case.
        if (command.getObservationMode() != null
                && (command.getOperation() == RemoteGameOperation.EXECUTE_INPUT_BUNDLE
                        || command.getOperation() == RemoteGameOperation.LOCAL_MACRO)) {
            throw schemaMismatch(
                    "command.observationMode is not allowed for " + command.getOperation().name()
                            + "; an input macro must not run under PAUSED_READ_ONLY");
        }
        RemoteSemanticAddress semanticAddress = command.getSemanticAddress();
        if (semanticAddress == null) {
            throw schemaMismatch("command.semanticAddress is required");
        }
        requireSchemaText(semanticAddress.getPhaseCode(),
                "command.semanticAddress.phaseCode");
        requireSchemaText(semanticAddress.getActionSlot(),
                "command.semanticAddress.actionSlot");
        if (semanticAddress.getOccurrence() < 0L || semanticAddress.getAttempt() < 0) {
            throw schemaMismatch("command semantic occurrence/attempt must not be negative");
        }
        if (command.getObservationMode() != null && semanticAddress.getAttempt() != 0) {
            throw schemaMismatch("PAUSED_READ_ONLY semanticAddress attempt must be 0");
        }
        requireDigest(command.getRequestDigest(), "command.requestDigest");
        if (command.getTimeoutMs() <= 0L) {
            throw schemaMismatch("command.timeoutMs must be positive");
        }
        if (command.getPayload() == null || !command.getPayload().isObject()) {
            throw schemaMismatch("command.payload must be a json object");
        }

        RemoteWindowBindingRef window = command.getWindow();
        if (window == null) {
            throw schemaMismatch("command.window is required");
        }
        requireSchemaText(window.getWindowId(), "command.window.windowId");
        String nativeHandle = requireSchemaText(window.getNativeHandle(), "command.window.nativeHandle");
        if (!nativeHandle.chars().allMatch(Character::isDigit)) {
            throw schemaMismatch("command.window.nativeHandle must be an unsigned decimal string");
        }
        if (window.getProcessId() <= 0L) {
            throw schemaMismatch("command.window.processId must be positive");
        }
        if (window.getPlayerIdentityEpoch() < 0L) {
            throw schemaMismatch("command.window.playerIdentityEpoch must not be negative");
        }

        RemoteStopRef stop = command.getStop();
        if (stop == null) {
            throw schemaMismatch("command.stop is required");
        }
        requireSchemaText(stop.getTaskRunId(), "command.stop.taskRunId");
        if (!Objects.equals(command.getTaskRunId(), stop.getTaskRunId())) {
            throw schemaMismatch("command.stop.taskRunId must equal command.taskRunId");
        }
        if (stop.getStopEpoch() < 0L) {
            throw schemaMismatch("command.stop.stopEpoch must not be negative");
        }
    }

    private void validateOutcome(RemoteGameOutcomeEnvelope outcome) {
        if (outcome == null) {
            throw invalidRequest("outcome is required");
        }
        if (outcome.getContractVersion() != 1) {
            throw invalidRequest("outcome contractVersion must be 1");
        }
        requireWireText(outcome.getTenantId(), "outcome.tenantId");
        requireWireText(outcome.getUserId(), "outcome.userId");
        requireWireText(outcome.getDeviceId(), "outcome.deviceId");
        requireWireText(outcome.getClientSessionId(), "outcome.clientSessionId");
        if (outcome.getOperation() == null) {
            throw invalidRequest("outcome.operation is required");
        }
        requireWireText(outcome.getRequestId(), "outcome.requestId");
        requireWireText(outcome.getActionId(), "outcome.actionId");
        requireWireText(outcome.getTaskRunId(), "outcome.taskRunId");
        if (outcome.getSemanticAddress() == null) {
            throw invalidRequest("outcome.semanticAddress is required");
        }
        requireWireDigest(outcome.getRequestDigest(), "outcome.requestDigest");
        requireWireDigest(outcome.getOutcomeDigest(), "outcome.outcomeDigest");
        if (outcome.getExecutionState() == null) {
            throw invalidRequest("outcome.executionState is required");
        }
        if (outcome.getCode() == null) {
            throw invalidRequest("outcome.code is required");
        }
        if (outcome.getAcceptedAtEpochMs() < 0L || outcome.getFinishedAtEpochMs() < 0L) {
            throw invalidRequest("outcome timestamps must not be negative");
        }
        if (outcome.getFinishedAtEpochMs() < outcome.getAcceptedAtEpochMs()) {
            throw invalidRequest("outcome.finishedAtEpochMs must not precede acceptedAtEpochMs");
        }
        if (outcome.getPayload() == null || !outcome.getPayload().isObject()) {
            throw invalidRequest("outcome.payload must be a json object");
        }
        String computedDigest;
        try {
            computedDigest = protocolDigests.computeOutcomeDigest(outcome);
        } catch (RuntimeException e) {
            throw invalidRequest("outcome typed digest input is invalid: " + e.getMessage());
        }
        if (!computedDigest.equals(outcome.getOutcomeDigest())) {
            throw invalidRequest("outcome.outcomeDigest does not match typed outcome");
        }
    }

    private void validateFinalConsumedAck(
            RemoteFinalConsumedAck acknowledgement,
            RemoteCommandPollRequest request) {
        if (acknowledgement == null
                || !Objects.equals(request.getTenantId(), acknowledgement.getTenantId())
                || !Objects.equals(request.getUserId(), acknowledgement.getUserId())
                || !Objects.equals(request.getDeviceId(), acknowledgement.getDeviceId())
                || !Objects.equals(request.getClientSessionId(),
                        acknowledgement.getClientSessionId())) {
            throw schemaMismatch(
                    "final-consumed acknowledgement scope does not match poll route");
        }
        try {
            if (!protocolDigests.finalConsumedAckDigestMatches(acknowledgement)) {
                throw schemaMismatch("final-consumed ackDigest does not match payload");
            }
        } catch (RuntimeException e) {
            throw schemaMismatch("invalid final-consumed acknowledgement: " + e.getMessage());
        }
    }

    private void validateFinalConsumedReceipt(RemoteFinalConsumedReceipt receipt) {
        if (receipt == null) {
            throw invalidRequest("final-consumed receipt is required");
        }
        try {
            if (!protocolDigests.finalConsumedReceiptDigestMatches(receipt)) {
                throw invalidRequest("final-consumed receiptDigest does not match payload");
            }
        } catch (RuntimeException e) {
            throw invalidRequest("invalid final-consumed receipt: " + e.getMessage());
        }
    }

    private void validateFinalConsumedReceiptAck(
            RemoteFinalConsumedReceiptAck acknowledgement,
            RemoteFinalConsumedReceipt receipt,
            String responseBody) {
        if (acknowledgement == null || acknowledgement.getStatus() == null
                || acknowledgement.getCode() == null
                || !Objects.equals(receipt.getAckDigest(), acknowledgement.getAckDigest())
                || !Objects.equals(receipt.getReceiptDigest(),
                        acknowledgement.getReceiptDigest())) {
            throw failure(
                    RemoteCommandTransportException.FailureType.SCHEMA_MISMATCH,
                    finalConsumedReceiptEndpoint, 200, responseBody, null, null,
                    "final-consumed receipt acknowledgement correlation mismatch", null);
        }
        if (acknowledgement.getStatus()
                == RemoteFinalConsumedReceiptAck.Status.REJECTED) {
            throw failure(
                    RemoteCommandTransportException.FailureType.OUTCOME_REJECTED,
                    finalConsumedReceiptEndpoint, 200, responseBody,
                    acknowledgement.getCode().name(), acknowledgement.getMessage(),
                    "cloud rejected final-consumed receipt", null);
        }
    }

    private Duration pollRequestTimeout(long waitTimeoutMs) {
        Duration serverWaitWithGrace = Duration.ofMillis(waitTimeoutMs).plus(POLL_TIMEOUT_GRACE);
        return serverWaitWithGrace.compareTo(requestTimeout) > 0
                ? serverWaitWithGrace
                : requestTimeout;
    }

    private static URI endpointUri(String baseUrl, String path) {
        URI base;
        try {
            base = URI.create(baseUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("baseUrl is not a valid URI", e);
        }
        String scheme = base.getScheme();
        if ((scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
                || base.getHost() == null
                || base.getRawQuery() != null
                || base.getRawFragment() != null) {
            throw new IllegalArgumentException("baseUrl must be an HTTP(S) URI without query or fragment");
        }
        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return URI.create(normalized + path);
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

    private static void requireWireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(field + " must not be blank");
        }
    }

    private static String requireSchemaText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw schemaMismatch(field + " must not be blank");
        }
        return value;
    }

    private static void requireWireDigest(String value, String field) {
        if (!isSha256Hex(value)) {
            throw invalidRequest(field + " must be a SHA-256 hex string");
        }
    }

    private static void requireDigest(String value, String field) {
        if (!isSha256Hex(value)) {
            throw schemaMismatch(field + " must be a SHA-256 hex string");
        }
    }

    private static boolean isSha256Hex(String value) {
        if (value == null || value.length() != 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean digit = ch >= '0' && ch <= '9';
            boolean lower = ch >= 'a' && ch <= 'f';
            boolean upper = ch >= 'A' && ch <= 'F';
            if (!digit && !lower && !upper) {
                return false;
            }
        }
        return true;
    }

    private static String nullableText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static RemoteCommandTransportException invalidRequest(String message) {
        return failure(
                RemoteCommandTransportException.FailureType.INVALID_REQUEST,
                null,
                null,
                null,
                null,
                null,
                message,
                null);
    }

    private static RemoteCommandTransportException schemaMismatch(String message) {
        return failure(
                RemoteCommandTransportException.FailureType.SCHEMA_MISMATCH,
                null,
                null,
                null,
                null,
                null,
                message,
                null);
    }

    private static RemoteCommandTransportException failure(
            RemoteCommandTransportException.FailureType failureType,
            URI endpoint,
            Integer statusCode,
            String responseBody,
            String serverCode,
            String serverMessage,
            String message,
            Throwable cause) {
        return new RemoteCommandTransportException(
                failureType,
                endpoint,
                statusCode,
                responseBody,
                serverCode,
                serverMessage,
                message,
                cause);
    }
}
