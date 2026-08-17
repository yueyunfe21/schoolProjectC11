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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

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
        return send(request, new ObservationSendCancellation());
    }

    @Override
    public ObservationResponse send(
            ObservationRequest request,
            ObservationSendCancellation cancellation) throws ObservationTransportException {
        Objects.requireNonNull(cancellation, "cancellation");
        if (cancellation.isCancelled()) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.INTERRUPTED,
                    "observation HTTP request cancelled before send");
        }
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

        HttpResponse<byte[]> httpResponse = sendOnce(httpRequest, cancellation);
        byte[] responseBytes = requireBoundedBody(httpResponse, MAX_JSON_BYTES, "observation response");
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

    private HttpResponse<byte[]> sendOnce(
            HttpRequest request,
            ObservationSendCancellation cancellation) throws ObservationTransportException {
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
        if (cancellation.isCancelled()) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.INTERRUPTED,
                    "observation HTTP request cancelled before transport start");
        }
        CompletableFuture<HttpResponse<byte[]>> future =
                client.sendAsync(request, boundedByteArrayHandler(MAX_JSON_BYTES, "observation response"));
        Runnable cancelAction = () -> future.cancel(true);
        cancellation.register(cancelAction);
        try {
            return future.get();
        } catch (CancellationException e) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.INTERRUPTED,
                    "observation HTTP request cancelled",
                    e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.INTERRUPTED,
                    "observation HTTP request interrupted",
                    e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            ObservationTransportException boundedFailure = findTransportFailure(cause);
            if (boundedFailure != null) {
                throw boundedFailure;
            }
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.NETWORK,
                    "observation HTTP request failed",
                    cause == null ? e : cause);
        } finally {
            cancellation.clear(cancelAction);
        }
    }

    /** Creates a subscriber that never retains more than the protocol's maximum response bytes. */
    private static HttpResponse.BodyHandler<byte[]> boundedByteArrayHandler(int maxBytes, String label) {
        return responseInfo -> {
            ObservationTransportException declaredLengthFailure = null;
            String contentLength = responseInfo.headers().firstValue("Content-Length").orElse(null);
            if (contentLength != null) {
                try {
                    long declared = Long.parseLong(contentLength);
                    if (declared < 0L || declared > maxBytes) {
                        declaredLengthFailure = new ObservationTransportException(
                                ObservationTransportException.Kind.RESPONSE_TOO_LARGE,
                                label + " Content-Length exceeds " + maxBytes + " bytes");
                    }
                } catch (NumberFormatException invalidLength) {
                    declaredLengthFailure = new ObservationTransportException(
                            ObservationTransportException.Kind.RESPONSE_CONTRACT,
                            label + " has invalid Content-Length",
                            invalidLength);
                }
            }
            return new BoundedByteArraySubscriber(maxBytes, label, declaredLengthFailure);
        };
    }

    private static ObservationTransportException findTransportFailure(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 8; depth++) {
            if (current instanceof ObservationTransportException transportFailure) {
                return transportFailure;
            }
            current = current.getCause();
        }
        return null;
    }

    private static byte[] requireBoundedBody(
            HttpResponse<byte[]> response,
            int maxBytes,
            String label) throws ObservationTransportException {
        String contentLength = response.headers().firstValue("Content-Length").orElse(null);
        if (contentLength != null) {
            try {
                long declared = Long.parseLong(contentLength);
                if (declared < 0 || declared > maxBytes) {
                    throw new ObservationTransportException(
                            ObservationTransportException.Kind.RESPONSE_TOO_LARGE,
                            label + " Content-Length exceeds " + maxBytes + " bytes");
                }
            } catch (NumberFormatException e) {
                throw new ObservationTransportException(
                        ObservationTransportException.Kind.RESPONSE_CONTRACT,
                        label + " has invalid Content-Length",
                        e);
            }
        }

        byte[] body = response.body();
        if (body == null) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.RESPONSE_CONTRACT,
                    label + " body is missing");
        }
        if (body.length > maxBytes) {
            throw new ObservationTransportException(
                    ObservationTransportException.Kind.RESPONSE_TOO_LARGE,
                    label + " exceeds " + maxBytes + " bytes");
        }
        return body;
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

    /** Incremental body subscriber; exceeding the bound cancels upstream before another chunk is retained. */
    private static final class BoundedByteArraySubscriber implements HttpResponse.BodySubscriber<byte[]> {

        private static final int COPY_BUFFER_SIZE = 8192;

        private final int maxBytes;
        private final String label;
        private final ByteArrayOutputStream output;
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
        private Flow.Subscription subscription;
        private int totalBytes;
        private boolean terminal;

        private BoundedByteArraySubscriber(
                int maxBytes,
                String label,
                ObservationTransportException initialFailure) {
            this.maxBytes = maxBytes;
            this.label = label;
            this.output = new ByteArrayOutputStream(Math.min(maxBytes, COPY_BUFFER_SIZE));
            if (initialFailure != null) {
                terminal = true;
                body.completeExceptionally(initialFailure);
            }
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription candidate) {
            if (candidate == null) {
                fail(new ObservationTransportException(
                        ObservationTransportException.Kind.NETWORK,
                        label + " body subscription is missing"));
                return;
            }
            if (subscription != null || terminal) {
                candidate.cancel();
                return;
            }
            subscription = candidate;
            candidate.request(1L);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (terminal || buffers == null) {
                return;
            }
            try {
                for (ByteBuffer buffer : buffers) {
                    if (buffer == null) {
                        continue;
                    }
                    int incoming = buffer.remaining();
                    if (incoming > maxBytes - totalBytes) {
                        fail(new ObservationTransportException(
                                ObservationTransportException.Kind.RESPONSE_TOO_LARGE,
                                label + " exceeds " + maxBytes + " bytes"));
                        return;
                    }
                    totalBytes += incoming;
                    while (buffer.hasRemaining()) {
                        int count = Math.min(buffer.remaining(), copyBuffer.length);
                        buffer.get(copyBuffer, 0, count);
                        output.write(copyBuffer, 0, count);
                    }
                }
                if (!terminal && subscription != null) {
                    subscription.request(1L);
                }
            } catch (RuntimeException bodyFailure) {
                fail(new ObservationTransportException(
                        ObservationTransportException.Kind.NETWORK,
                        "failed while reading " + label,
                        bodyFailure));
            }
        }

        @Override
        public void onError(Throwable failure) {
            if (terminal) {
                return;
            }
            terminal = true;
            body.completeExceptionally(failure);
        }

        @Override
        public void onComplete() {
            if (terminal) {
                return;
            }
            terminal = true;
            body.complete(output.toByteArray());
        }

        private void fail(ObservationTransportException failure) {
            if (terminal) {
                return;
            }
            terminal = true;
            if (subscription != null) {
                subscription.cancel();
            }
            body.completeExceptionally(failure);
        }
    }

}
