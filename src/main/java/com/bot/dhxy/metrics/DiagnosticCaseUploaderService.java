package com.bot.dhxy.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Best-effort uploader for locally captured diagnostic case JSON files.
 *
 * <p>The uploader intentionally owns only network delivery and persisted upload state. It never
 * throws into task execution or metrics capture, and it leaves eligible but disabled cases as
 * {@code PENDING} so a later startup/manual retry can upload them after endpoint/token config is
 * supplied.</p>
 */
@Slf4j
@Service
public class DiagnosticCaseUploaderService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final boolean enabled;
    private final URI endpoint;
    private final String token;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int maxAttempts;
    private final Duration retryDelay;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    @Autowired
    public DiagnosticCaseUploaderService(
            @Value("${case.upload.enabled:false}") boolean enabled,
            @Value("${case.upload.endpoint:}") String endpoint,
            @Value("${case.upload.token:}") String token,
            @Value("${case.upload.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${case.upload.read-timeout-ms:8000}") long readTimeoutMs,
            @Value("${case.upload.max-attempts:3}") int maxAttempts,
            @Value("${case.upload.retry-delay-ms:60000}") long retryDelayMs
    ) {
        this(enabled,
                parseEndpoint(endpoint),
                token,
                Duration.ofMillis(Math.max(1L, connectTimeoutMs)),
                Duration.ofMillis(Math.max(1L, readTimeoutMs)),
                maxAttempts,
                Duration.ofMillis(Math.max(1L, retryDelayMs)),
                null,
                true);
    }

    private DiagnosticCaseUploaderService(boolean enabled,
                                          URI endpoint,
                                          String token,
                                          Duration connectTimeout,
                                          Duration readTimeout,
                                          int maxAttempts,
                                          Duration retryDelay,
                                          HttpClient httpClient,
                                          boolean async) {
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.token = token == null ? "" : token.trim();
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        this.readTimeout = readTimeout == null ? Duration.ofSeconds(8) : readTimeout;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelay = retryDelay == null ? Duration.ofMinutes(1) : retryDelay;
        this.httpClient = httpClient == null
                ? HttpClient.newBuilder().connectTimeout(this.connectTimeout).build()
                : httpClient;
        this.executor = async ? Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dhxy-diagnostic-case-uploader");
            thread.setDaemon(true);
            return thread;
        }) : null;
    }

    public static DiagnosticCaseUploaderService disabled() {
        return new DiagnosticCaseUploaderService(false, null, "", Duration.ofMillis(1),
                Duration.ofMillis(1), 1, Duration.ofMillis(1), HttpClient.newHttpClient(), false);
    }

    static DiagnosticCaseUploaderService forTest(boolean enabled,
                                                 URI endpoint,
                                                 String token,
                                                 Duration connectTimeout,
                                                 Duration readTimeout,
                                                 int maxAttempts,
                                                 Duration retryDelay) {
        return new DiagnosticCaseUploaderService(enabled, endpoint, token, connectTimeout, readTimeout,
                maxAttempts, retryDelay, null, false);
    }

    public void enqueueUpload(Path casePath) {
        if (!isConfigured() || casePath == null) {
            return;
        }
        if (executor == null) {
            uploadCaseOnce(casePath);
            return;
        }
        executor.submit(() -> {
            try {
                uploadCaseOnce(casePath);
            } catch (Exception e) {
                log.warn("[diagnostic-case] async upload failed: path={} reason={}",
                        casePath, e.getMessage(), e);
            }
        });
    }

    @PostConstruct
    public void retryLocalCasesOnStartup() {
        retryPendingCases(Path.of("logs", "cases"));
    }

    public void retryPendingCases(Path casesRoot) {
        if (!isConfigured() || casesRoot == null || !Files.exists(casesRoot)) {
            return;
        }
        Runnable scan = () -> {
            try (Stream<Path> paths = Files.walk(casesRoot)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".case.json"))
                        .filter(this::shouldRetry)
                        .forEach(this::uploadCaseOnce);
            } catch (Exception e) {
                log.warn("[diagnostic-case] pending upload scan failed: root={} reason={}",
                        casesRoot, e.getMessage(), e);
            }
        };
        if (executor == null) {
            scan.run();
            return;
        }
        executor.submit(scan);
    }

    void uploadCaseOnce(Path casePath) {
        if (casePath == null || !Files.exists(casePath)) {
            return;
        }
        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(Files.readString(casePath, StandardCharsets.UTF_8));
            ObjectNode upload = uploadNode(root);
            if (!upload.path("eligible").asBoolean(false)) {
                return;
            }
            int attempts = upload.path("attempts").asInt(0);
            String status = upload.path("status").asText("");
            if ("UPLOADED".equals(status) || "FAILED_PERMANENT".equals(status) || attempts >= maxAttempts) {
                return;
            }
            if (!isConfigured()) {
                return;
            }

            upload.put("status", "PENDING");
            upload.put("attempts", attempts + 1);
            upload.put("lastAttemptAt", OffsetDateTime.now().toString());
            writeCase(casePath, root);

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(readTimeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(root)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            applyHttpResponse(upload, response.statusCode(), response.body());
            writeCase(casePath, root);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markRetryable(casePath, "INTERRUPTED", "case upload interrupted");
        } catch (Exception e) {
            markRetryable(casePath, "CLIENT_ERROR", e.getMessage());
        }
    }

    private void applyHttpResponse(ObjectNode upload, int statusCode, String responseBody) throws Exception {
        JsonNode body = parseResponse(responseBody);
        String code = text(body, "code");
        String message = text(body, "message");
        upload.put("responseHttpStatus", statusCode);
        upload.put("responseCode", code);
        upload.put("responseMessage", message);
        if (statusCode >= 200 && statusCode < 300 && "CASE_UPLOADED".equals(code)) {
            upload.put("status", "UPLOADED");
            upload.put("caseKey", text(body, "caseKey"));
            upload.put("indexKey", text(body, "indexKey"));
            upload.remove("nextAttemptAt");
            return;
        }
        if (isPermanentFailure(statusCode, code)) {
            upload.put("status", "FAILED_PERMANENT");
            upload.remove("nextAttemptAt");
            return;
        }
        upload.put("status", "FAILED_RETRYABLE");
        upload.put("nextAttemptAt", OffsetDateTime.now().plus(retryDelay).toString());
    }

    private void markRetryable(Path casePath, String code, String message) {
        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(Files.readString(casePath, StandardCharsets.UTF_8));
            ObjectNode upload = uploadNode(root);
            upload.put("status", "FAILED_RETRYABLE");
            upload.put("responseCode", code);
            upload.put("responseMessage", message == null ? "" : message);
            upload.put("nextAttemptAt", OffsetDateTime.now().plus(retryDelay).toString());
            writeCase(casePath, root);
        } catch (Exception e) {
            log.warn("[diagnostic-case] failed to persist retryable upload status: path={} reason={}",
                    casePath, e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return enabled && endpoint != null && !token.isBlank();
    }

    private boolean shouldRetry(Path casePath) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(casePath, StandardCharsets.UTF_8));
            JsonNode upload = root.path("upload");
            if (!upload.path("eligible").asBoolean(false)) {
                return false;
            }
            String status = upload.path("status").asText("");
            int attempts = upload.path("attempts").asInt(0);
            return attempts < maxAttempts
                    && ("PENDING".equals(status) || "FAILED_RETRYABLE".equals(status));
        } catch (Exception e) {
            log.debug("[diagnostic-case] skip unreadable pending upload case: path={} reason={}",
                    casePath, e.getMessage());
            return false;
        }
    }

    private boolean isPermanentFailure(int statusCode, String code) {
        String normalized = code == null ? "" : code.toUpperCase(Locale.ROOT);
        return statusCode == 400
                || statusCode == 401
                || statusCode == 403
                || statusCode == 413
                || "INVALID_CASE_SCHEMA".equals(normalized)
                || "LICENSE_REQUIRED".equals(normalized)
                || "UNAUTHORIZED".equals(normalized)
                || "PAYLOAD_TOO_LARGE".equals(normalized);
    }

    private JsonNode parseResponse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return OBJECT_MAPPER.createObjectNode();
        }
        return OBJECT_MAPPER.readTree(responseBody);
    }

    private ObjectNode uploadNode(ObjectNode root) {
        JsonNode existing = root.path("upload");
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        ObjectNode upload = OBJECT_MAPPER.createObjectNode();
        root.set("upload", upload);
        return upload;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private void writeCase(Path casePath, ObjectNode root) throws Exception {
        Files.writeString(casePath, OBJECT_MAPPER.writeValueAsString(root), StandardCharsets.UTF_8);
    }

    private static URI parseEndpoint(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value.trim());
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
