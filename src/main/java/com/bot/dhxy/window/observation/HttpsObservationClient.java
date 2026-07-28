package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationResponse;
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
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * TURN-40G: HTTPS transport for the independent observation plane. Reuses only the turn transport's configuration
 * and authentication conventions (origin rules, bearer header, strict JSON, single-attempt sends, bounded reads);
 * it posts exclusively to the observation endpoint and can never read, occupy or resolve the command plane's
 * per-window unresolved action slot.
 */
public final class HttpsObservationClient implements ObservationClient {

    // Eight protocol-valid 256 KiB PNG ROIs expand to about 2.7 MiB after Base64 encoding.
    static final int MAX_JSON_BYTES = 16 * 1024 * 1024;

    private static final String OBSERVATION_PATH = "/api/v1/client/observation";

    private final URI observationUri;
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
     * @param requestTimeout positive complete request timeout
     * @param objectMapper project Jackson mapper used through a strict defensive copy
     */
    public HttpsObservationClient(
            URI baseUri,
            String bearerToken,
            Duration connectTimeout,
            Duration requestTimeout,
            ObjectMapper objectMapper) {
        this.observationUri = requireBaseUri(baseUri).resolve(OBSERVATION_PATH);
        this.authorizationHeader = "Bearer " + requireToken(bearerToken);
        this.connectTimeout = requirePositive(connectTimeout, "connectTimeout");
        this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
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

    @Override
    public ObservationResponse send(ObservationRequest request) throws ObservationTransportException {
        try {
            ObservationProtocolValidator.requireValid(request);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.REQUEST_CONTRACT,
                    "invalid observation request: " + e.getMessage(),
                    e);
        }
        byte[] requestJson;
        try {
            requestJson = objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.SERIALIZATION,
                    "cannot serialize observation request",
                    e);
        }
        if (requestJson.length > MAX_JSON_BYTES) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.REQUEST_CONTRACT,
                    "observation request JSON exceeds " + MAX_JSON_BYTES + " bytes");
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(observationUri)
                .timeout(requestTimeout)
                .header("Authorization", authorizationHeader)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestJson))
                .build();

        HttpResponse<InputStream> httpResponse = sendOnce(httpRequest);
        byte[] responseBytes = readBounded(httpResponse, MAX_JSON_BYTES, "observation response");
        if (httpResponse.statusCode() != 200) {
            throw httpStatusFailure(httpResponse.statusCode(), responseBytes);
        }
        requireContentType(httpResponse.headers(), "application/json", "observation response");

        ObservationResponse response;
        try {
            response = objectMapper.readValue(responseBytes, ObservationResponse.class);
        } catch (IOException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.RESPONSE_PARSE,
                    "observation response is not strict ObservationResponse JSON",
                    e);
        }
        try {
            return ObservationProtocolValidator.requireValid(response, request);
        } catch (IllegalArgumentException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.RESPONSE_CONTRACT,
                    "invalid observation response: " + e.getMessage(),
                    e);
        }
    }

    private HttpResponse<InputStream> sendOnce(HttpRequest request) throws ObservationTransportException {
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
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.INTERRUPTED,
                    "observation HTTP request interrupted",
                    e);
        } catch (IOException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.NETWORK,
                    "observation HTTP request failed",
                    e);
        }
    }

    private static byte[] readBounded(
            HttpResponse<InputStream> response,
            int maxBytes,
            String label) throws ObservationTransportException {
        String contentLength = response.headers().firstValue("Content-Length").orElse(null);
        if (contentLength != null) {
            try {
                long declared = Long.parseLong(contentLength);
                if (declared < 0 || declared > maxBytes) {
                    closeQuietly(response.body());
                    throw new ObservationTransportException(
                            ObservationTransportException.Kind.RESPONSE_TOO_LARGE,
                            label + " Content-Length exceeds " + maxBytes + " bytes");
                }
            } catch (NumberFormatException e) {
                closeQuietly(response.body());
                throw new ObservationTransportException(
                        ObservationTransportException.Kind.RESPONSE_CONTRACT,
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
                    throw new ObservationTransportException(
                            ObservationTransportException.Kind.RESPONSE_TOO_LARGE,
                            label + " exceeds " + maxBytes + " bytes");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (ObservationTransportException e) {
            throw e;
        } catch (IOException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.NETWORK,
                    "failed while reading " + label,
                    e);
        }
    }

    private static void requireContentType(HttpHeaders headers, String expected, String label)
            throws ObservationTransportException {
        String actual = headers.firstValue("Content-Type").orElse("");
        String mediaType = actual.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(mediaType)) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.RESPONSE_CONTENT_TYPE,
                    label + " Content-Type must be " + expected + " but was " + actual);
        }
    }

    private static ObservationTransportException httpStatusFailure(int status, byte[] body) {
        String detail = new String(body, 0, Math.min(body.length, 512), StandardCharsets.UTF_8)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        String message = "unexpected HTTP status " + status;
        if (!detail.isEmpty()) {
            message += ": " + detail;
        }
        return new ObservationTransportException(
                ObservationTransportException.Kind.HTTP_STATUS,
                message,
                status,
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

    private static Duration requirePositive(Duration duration, String field) {
        Objects.requireNonNull(duration, field);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return duration;
    }

    private static void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // best-effort close on an already-failed response
        }
    }
}
