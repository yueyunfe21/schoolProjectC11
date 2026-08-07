package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.turn.protocol.TurnResponse;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueMessage;
import com.bot.dhxy.cloud.task.NpcClickSmartCloudSession;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueOutcome;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single-attempt JDK HTTP/2 implementation of the HTTPS turn and template transport.
 *
 * <p>No method in this class retries a request. A network-uncertain POST fails typed so the turn loop can
 * retain the exact previous outcome without treating it as acknowledged.</p>
 */
public final class HttpsTurnClient implements TurnClient {

    static final int MAX_JSON_BYTES = 256 * 1024;
    static final int MAX_PNG_BYTES = 8 * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final Pattern SHA256_ETAG = Pattern.compile("\\\"sha256:([0-9a-f]{64})\\\"");
    private static final String TURN_PATH = "/api/v1/client/turn";
    private static final String NPC_ARRIVAL_FRAME_QUEUE_PATH = "/api/v1/npc-arrival-frame/queue";
    private static final String TEMPLATE_PATH_PREFIX = "/api/v1/templates/";

    private final URI baseUri;
    private final URI turnUri;
    private final String bearerToken;
    private final String authorizationHeader;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private volatile HttpClient httpClient;

    /**
     * Creates a transport that lazily owns one reusable HTTP/2 client.
     *
     * @param baseUri origin URI with no path/query/fragment; non-loopback origins must use HTTPS
     * @param bearerToken bearer token without prefix; non-blank and without surrounding whitespace
     * @param connectTimeout positive TCP/TLS connect timeout
     * @param requestTimeout positive complete request timeout, sized for the configured long wait
     * @param objectMapper project Jackson mapper used through a strict defensive copy
     */
    public HttpsTurnClient(
            URI baseUri,
            String bearerToken,
            Duration connectTimeout,
            Duration requestTimeout,
            ObjectMapper objectMapper) {
        this.baseUri = requireBaseUri(baseUri);
        this.turnUri = this.baseUri.resolve(TURN_PATH);
        this.bearerToken = requireToken(bearerToken);
        this.authorizationHeader = "Bearer " + this.bearerToken;
        this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
        this.connectTimeout = requirePositive(connectTimeout, "connectTimeout");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper")
                .copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    /**
     * TURN-40G: creates the sibling observation-plane transport from this client's exact configuration and
     * authentication (origin, bearer token, timeouts, strict mapper). This is a pure configuration-reuse seam —
     * the returned client posts only to the independent observation endpoint and shares no turn state, so
     * observation traffic can never occupy this client's turn slot.
     */
    public com.bot.dhxy.window.observation.HttpsObservationClient newObservationClient() {
        return new com.bot.dhxy.window.observation.HttpsObservationClient(
                baseUri, bearerToken, connectTimeout, requestTimeout, objectMapper);
    }

    @Override
    public NpcClickSmartCloudSession openNpcArrivalFrame(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId)
            throws TurnTransportException {
        com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
        request.put("operation", "OPEN");
        putArrivalIdentity(request, tenantId, deviceId, windowId, hwnd,
                observationRunId, businessTaskRunId, intentId);
        byte[] response = postNpcArrivalJson(request);
        try {
            return objectMapper.readValue(response, NpcClickSmartCloudSession.class);
        } catch (IOException failure) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_PARSE,
                    "NPC arrival-frame open response is invalid",
                    failure);
        }
    }

    @Override
    public NpcClickSmartQueueMessage pollNpcArrivalFrame(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId)
            throws TurnTransportException {
        com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
        request.put("operation", "POLL");
        putArrivalIdentity(request, tenantId, deviceId, windowId, hwnd,
                observationRunId, businessTaskRunId, intentId);
        byte[] response = postNpcArrivalJson(request);
        try {
            return objectMapper.readValue(response, NpcClickSmartQueueMessage.class);
        } catch (IOException failure) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_PARSE,
                    "NPC arrival-frame poll response is invalid: cause="
                            + failure.getMessage()
                            + " response="
                            + boundedResponsePreview(response),
                    failure);
        }
    }

    private static String boundedResponsePreview(byte[] response) {
        if (response == null) {
            return "null";
        }
        String text = new String(response, StandardCharsets.UTF_8)
                .replace('\r', ' ')
                .replace('\n', ' ');
        return text.length() <= 1_024 ? text : text.substring(0, 1_024) + "...";
    }

    @Override
    public void replaceNpcArrivalFrame(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId,
            long frameId, long generation,
            long capturedAtMs, byte[] pngBytes) throws TurnTransportException {
        if (pngBytes == null || pngBytes.length > MAX_PNG_BYTES) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "NPC replacement frame is missing or oversized");
        }
        com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
        request.put("operation", "REPLACE");
        putArrivalIdentity(request, tenantId, deviceId, windowId, hwnd,
                observationRunId, businessTaskRunId, intentId);
        request.put("frameId", frameId);
        request.put("generation", generation);
        request.put("capturedAtMs", capturedAtMs);
        request.put("imagePayloadBase64", java.util.Base64.getEncoder().encodeToString(pngBytes));
        byte[] response = postNpcArrivalJson(request);
        try {
            String status = objectMapper.readTree(response).path("status").asText();
            if (!status.contains("FRAME_PREPARING")) {
                throw new TurnTransportException(
                        TurnTransportException.Kind.RESPONSE_CONTRACT,
                        "Cloud rejected NPC replacement frame: " + status);
            }
        } catch (java.io.IOException failure) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_PARSE,
                    "NPC replacement response is invalid",
                    failure);
        }
    }

    @Override
    public void reportNpcArrivalFrameOutcome(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId,
            NpcClickSmartQueueMessage message,
            NpcClickSmartQueueOutcome outcome,
            String reason) throws TurnTransportException {
        com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
        request.put("operation", "REPORT");
        putArrivalIdentity(request, tenantId, deviceId, windowId, hwnd,
                observationRunId, businessTaskRunId, intentId);
        request.put("taskRunId", businessTaskRunId);
        request.put("sessionId", message == null ? "" : message.getSessionId());
        request.put("decisionId", message == null ? "local-terminal" : message.getDecisionId());
        request.put("messageType", message == null || message.getType() == null
                ? "INVALID" : message.getType().name());
        request.put("strategy", message == null ? "INVALID" : message.getStrategy());
        request.put("candidateBox", message == null ? "" : message.getCandidateBox());
        request.put("matchedText", message == null ? "" : message.getMatchedText());
        request.put("result", outcome == null ? "FINAL_FAILED" : outcome.name());
        request.put("localVerificationReason", reason);
        request.put("reason", message == null || message.getReason() == null
                ? reason : message.getReason());
        if (message != null && message.getWindowRelativeClickPoint() != null) {
            com.fasterxml.jackson.databind.node.ObjectNode click = request.putObject("click");
            click.put("x", message.getWindowRelativeClickPoint().x);
            click.put("y", message.getWindowRelativeClickPoint().y);
        }
        postNpcArrivalJson(request);
    }

    private void putArrivalIdentity(
            com.fasterxml.jackson.databind.node.ObjectNode request,
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId) {
        request.put("tenantId", tenantId);
        request.put("deviceId", deviceId);
        request.put("windowId", windowId);
        request.put("hwnd", hwnd);
        request.put("observationRunId", observationRunId);
        request.put("businessTaskRunId", businessTaskRunId);
        request.put("intentId", intentId);
    }

    private byte[] postNpcArrivalJson(com.fasterxml.jackson.databind.node.ObjectNode request)
            throws TurnTransportException {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException failure) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.SERIALIZATION,
                    "cannot serialize NPC arrival-frame request",
                    failure);
        }
        HttpRequest httpRequest = requestBuilder(baseUri.resolve(NPC_ARRIVAL_FRAME_QUEUE_PATH))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<InputStream> response = sendOnce(httpRequest);
        byte[] responseBytes = readBounded(response, MAX_JSON_BYTES, "NPC arrival-frame response");
        if (response.statusCode() != 200) {
            throw httpStatusFailure(response.statusCode(), responseBytes);
        }
        requireContentType(response.headers(), "application/json", "NPC arrival-frame response");
        return responseBytes;
    }

    /**
     * Sends one JSON or JSON-plus-PNG turn request and accepts only one bounded valid 200 JSON response.
     *
     * @param request current bound-window request; non-null
     * @param optionalPng nullable raw PNG matching the previous outcome's frame metadata
     * @return validated response and explicit previous-outcome acknowledgement
     * @throws TurnTransportException typed failure without any fabricated business outcome
     */
    @Override
    public TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) throws TurnTransportException {
        requireValidRequest(request, optionalPng);
        byte[] requestJson = serializeRequest(request);
        if (requestJson.length > MAX_JSON_BYTES) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "turn metadata JSON exceeds " + MAX_JSON_BYTES + " bytes");
        }

        HttpRequest.Builder builder = requestBuilder(turnUri)
                .header("Accept", "application/json");
        if (optionalPng == null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestJson));
        } else {
            TurnMultipartBody multipart;
            try {
                multipart = TurnMultipartBody.create(requestJson, optionalPng);
            } catch (RuntimeException e) {
                throw new TurnTransportException(
                        TurnTransportException.Kind.SERIALIZATION,
                        "cannot build turn multipart body",
                        e);
            }
            builder.header("Content-Type", multipart.contentType())
                    .POST(multipart.bodyPublisher());
        }

        HttpResponse<InputStream> httpResponse = sendOnce(builder.build());
        byte[] responseBytes = readBounded(httpResponse, MAX_JSON_BYTES, "turn response");
        if (httpResponse.statusCode() != 200) {
            throw httpStatusFailure(httpResponse.statusCode(), responseBytes);
        }
        requireContentType(httpResponse.headers(), "application/json", "turn response");

        TurnResponse response;
        try {
            response = objectMapper.readValue(responseBytes, TurnResponse.class);
        } catch (IOException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_PARSE,
                    "turn response is not strict TurnResponse JSON",
                    e);
        }
        requireValidResponse(response, request);
        return TurnExchangeResult.accepted(response);
    }

    /**
     * Performs one authenticated conditional GET and verifies Cloud's SHA-256 ETag against 200 PNG bytes.
     *
     * @param templateKey canonical template wire key encoded as one URL path segment
     * @param ifNoneMatch nullable exact ETag for conditional GET
     * @return typed 200 or 304 result with verified hash identity
     * @throws TurnTransportException typed failure without retry
     */
    @Override
    public TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch)
            throws TurnTransportException {
        String checkedKey = requireTemplateKey(templateKey);
        URI templateUri = baseUri.resolve(TEMPLATE_PATH_PREFIX + encodePathSegment(checkedKey));
        HttpRequest.Builder builder = requestBuilder(templateUri)
                .header("Accept", "image/png")
                .GET();
        if (ifNoneMatch != null) {
            builder.header("If-None-Match", requireHeaderValue(ifNoneMatch, "ifNoneMatch"));
        }

        HttpResponse<InputStream> httpResponse = sendOnce(builder.build());
        int status = httpResponse.statusCode();
        byte[] body = readBounded(httpResponse, MAX_PNG_BYTES, "template response");
        if (status != 200 && status != 304) {
            throw httpStatusFailure(status, body);
        }

        String etag = httpResponse.headers().firstValue("ETag")
                .orElseThrow(() -> new TurnTransportException(
                        TurnTransportException.Kind.RESPONSE_CONTRACT,
                        "template response is missing ETag"));
        String etagSha256 = parseSha256Etag(etag);
        if (status == 304) {
            if (ifNoneMatch == null || !etag.equals(ifNoneMatch)) {
                throw new TurnTransportException(
                        TurnTransportException.Kind.RESPONSE_CONTRACT,
                        "304 template response ETag must equal the request If-None-Match");
            }
            if (body.length != 0) {
                throw new TurnTransportException(
                        TurnTransportException.Kind.RESPONSE_CONTRACT,
                        "304 template response must not contain a body");
            }
            return new TurnTemplateDownload(
                    TurnTemplateDownload.Status.NOT_MODIFIED_304,
                    etag,
                    etagSha256,
                    null);
        }

        requireContentType(httpResponse.headers(), "image/png", "template response");
        requirePng(body, "template response", TurnTransportException.Kind.RESPONSE_CONTRACT);
        String actualSha256 = sha256(body);
        if (!actualSha256.equals(etagSha256)) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.TEMPLATE_HASH_MISMATCH,
                    "template PNG SHA-256 does not match ETag");
        }
        return new TurnTemplateDownload(
                TurnTemplateDownload.Status.OK_200,
                etag,
                actualSha256,
                body);
    }

    private void requireValidRequest(TurnRequest request, byte[] optionalPng) throws TurnTransportException {
        try {
            TurnProtocolValidator.requireValid(Objects.requireNonNull(request, "request"));
        } catch (RuntimeException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "invalid turn request: " + e.getMessage(),
                    e);
        }

        TurnFrameMetadata frame = request.continuation() != null
                ? request.continuation().frame()
                : request.previousOutcome() == null ? null : request.previousOutcome().frame();
        if ((frame == null) != (optionalPng == null)) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "previous outcome frame metadata and PNG bytes must be present together");
        }
        if (optionalPng == null) {
            return;
        }
        if (optionalPng.length > MAX_PNG_BYTES) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "turn PNG exceeds " + MAX_PNG_BYTES + " bytes");
        }
        requirePng(optionalPng, "turn frame", TurnTransportException.Kind.REQUEST_CONTRACT);
        if (!"image/png".equals(frame.contentType())) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "turn frame metadata contentType must be image/png");
        }
        if (!sha256(optionalPng).equals(frame.sha256())) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "turn frame PNG SHA-256 does not match metadata");
        }
    }

    private byte[] serializeRequest(TurnRequest request) throws TurnTransportException {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.SERIALIZATION,
                    "cannot serialize TurnRequest",
                    e);
        }
    }

    private void requireValidResponse(TurnResponse response, TurnRequest request) throws TurnTransportException {
        if (response == null || response.status() == null) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_CONTRACT,
                    "turn response and status must be present");
        }
        try {
            TurnProtocolValidator.requireValid(response, request);
        } catch (RuntimeException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_CONTRACT,
                    "invalid turn response: " + e.getMessage(),
                    e);
        }
    }

    private HttpRequest.Builder requestBuilder(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Authorization", authorizationHeader);
    }

    private HttpResponse<InputStream> sendOnce(HttpRequest request) throws TurnTransportException {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .connectTimeout(connectTimeout)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .build();
                    httpClient = client;
                }
            }
        }
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TurnTransportException(
                    TurnTransportException.Kind.INTERRUPTED,
                    "HTTP request interrupted; acknowledgement is unknown",
                    e);
        } catch (IOException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.NETWORK,
                    "HTTP request failed; acknowledgement is unknown",
                    e);
        }
    }

    private static byte[] readBounded(
            HttpResponse<InputStream> response,
            int maxBytes,
            String label) throws TurnTransportException {
        Optional<String> contentLength = response.headers().firstValue("Content-Length");
        if (contentLength.isPresent()) {
            try {
                long declared = Long.parseLong(contentLength.get());
                if (declared < 0 || declared > maxBytes) {
                    closeQuietly(response.body());
                    throw new TurnTransportException(
                            TurnTransportException.Kind.RESPONSE_TOO_LARGE,
                            label + " Content-Length exceeds " + maxBytes + " bytes");
                }
            } catch (NumberFormatException e) {
                closeQuietly(response.body());
                throw new TurnTransportException(
                        TurnTransportException.Kind.RESPONSE_CONTRACT,
                        label + " has invalid Content-Length",
                        e);
            }
        }

        try (InputStream input = response.body();
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192))) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                total += read;
                if (total > maxBytes) {
                    throw new TurnTransportException(
                            TurnTransportException.Kind.RESPONSE_TOO_LARGE,
                            label + " exceeds " + maxBytes + " bytes");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (TurnTransportException e) {
            throw e;
        } catch (IOException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.NETWORK,
                    "failed while reading " + label + "; acknowledgement is unknown",
                    e);
        }
    }

    private static void requireContentType(HttpHeaders headers, String expected, String label)
            throws TurnTransportException {
        String actual = headers.firstValue("Content-Type").orElse("");
        String mediaType = actual.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(mediaType)) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_CONTENT_TYPE,
                    label + " Content-Type must be " + expected + " but was " + actual);
        }
    }

    private TurnTransportException httpStatusFailure(int status, byte[] body) {
        String detail = new String(body, 0, Math.min(body.length, 512), StandardCharsets.UTF_8)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        String message = "unexpected HTTP status " + status;
        String cloudErrorCode = null;
        if (!detail.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.JsonNode problem = objectMapper.readTree(detail);
                String code = problem.path("code").asText("").trim();
                cloudErrorCode = code.isEmpty() ? null : code;
                String explanation = problem.path("message").asText("").trim();
                if (!explanation.isEmpty()) {
                    message = "Cloud request rejected"
                            + (code.isEmpty() ? "" : " [" + code + "]")
                            + ": " + explanation;
                } else {
                    message += ": " + detail;
                }
            } catch (IOException invalidProblemJson) {
                message += ": " + detail;
            }
        }
        return new TurnTransportException(
                TurnTransportException.Kind.HTTP_STATUS,
                message,
                status,
                cloudErrorCode,
                null);
    }

    private static URI requireBaseUri(URI uri) {
        Objects.requireNonNull(uri, "baseUri");
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!uri.isAbsolute() || scheme == null || host == null) {
            throw new IllegalArgumentException("baseUri must be an absolute HTTP(S) origin");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))) {
            throw new IllegalArgumentException("baseUri must not contain user info, path, query, or fragment");
        }
        boolean https = "https".equalsIgnoreCase(scheme);
        boolean loopbackHttp = "http".equalsIgnoreCase(scheme) && isExactLoopback(host);
        if (!https && !loopbackHttp) {
            throw new IllegalArgumentException("non-loopback baseUri must use HTTPS");
        }
        return uri.resolve("/");
    }

    private static boolean isExactLoopback(String host) {
        return "127.0.0.1".equalsIgnoreCase(host)
                || "localhost".equalsIgnoreCase(host)
                || "::1".equalsIgnoreCase(host)
                || "[::1]".equalsIgnoreCase(host);
    }

    private static String requireToken(String token) {
        if (token == null || token.isBlank() || !token.equals(token.trim())
                || token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("bearerToken must be non-blank without surrounding whitespace");
        }
        return token;
    }

    private static Duration requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    private static String requireTemplateKey(String templateKey) throws TurnTransportException {
        if (templateKey == null || templateKey.isBlank() || !templateKey.equals(templateKey.trim())) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    "templateKey must be non-blank without surrounding whitespace");
        }
        return requireHeaderFree(templateKey, "templateKey");
    }

    private static String requireHeaderValue(String value, String name) throws TurnTransportException {
        if (value.isBlank() || !value.equals(value.trim())) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    name + " must be non-blank without surrounding whitespace");
        }
        return requireHeaderFree(value, name);
    }

    private static String requireHeaderFree(String value, String name) throws TurnTransportException {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.REQUEST_CONTRACT,
                    name + " must not contain CR or LF");
        }
        return value;
    }

    private static String encodePathSegment(String value) {
        StringBuilder encoded = new StringBuilder(value.length() * 2);
        for (byte current : value.getBytes(StandardCharsets.UTF_8)) {
            int octet = current & 0xff;
            if ((octet >= 'a' && octet <= 'z')
                    || (octet >= 'A' && octet <= 'Z')
                    || (octet >= '0' && octet <= '9')
                    || octet == '-' || octet == '.' || octet == '_' || octet == '~') {
                encoded.append((char) octet);
            } else {
                encoded.append('%');
                encoded.append(Character.toUpperCase(Character.forDigit((octet >>> 4) & 0xf, 16)));
                encoded.append(Character.toUpperCase(Character.forDigit(octet & 0xf, 16)));
            }
        }
        return encoded.toString();
    }

    private static String parseSha256Etag(String etag) throws TurnTransportException {
        Matcher matcher = SHA256_ETAG.matcher(etag);
        if (!matcher.matches()) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.RESPONSE_CONTRACT,
                    "template ETag must be quoted sha256:<64 lowercase hex>");
        }
        return matcher.group(1);
    }

    private static void requirePng(
            byte[] bytes,
            String label,
            TurnTransportException.Kind failureKind) throws TurnTransportException {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) {
            throw new TurnTransportException(
                    failureKind,
                    label + " is not a PNG byte stream");
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                throw new TurnTransportException(
                        failureKind,
                        label + " is not a PNG byte stream");
            }
        }
    }

    private static String sha256(byte[] bytes) throws TurnTransportException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new TurnTransportException(
                    TurnTransportException.Kind.INVALID_CONFIGURATION,
                    "SHA-256 is unavailable",
                    e);
        }
    }

    private static void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // The original bounded-response failure remains authoritative.
        }
    }
}
