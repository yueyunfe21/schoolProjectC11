package com.bot.dhxy.cloud.decision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Primary
@Component
public class HttpCloudDecisionClient implements CloudDecisionClient {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final CloudDecisionProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public HttpCloudDecisionClient(CloudDecisionProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .build();
    }

    /**
     * Sends one cloud-decision shadow request to the configured HTTP endpoint.
     *
     * @param request request metadata and local decision context; must include service id and trace id
     * @return parsed cloud response; callers still decide whether the response can affect behavior
     */
    @Override
    public CloudDecisionResponse decide(CloudDecisionRequest request) {
        ensureEnabled();

        String body;
        try {
            body = serializeRequest(request);
        } catch (JsonProcessingException e) {
            throw new CloudDecisionClientException("json serialize failure: " + e.getOriginalMessage(), e);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(endpointUri())
                .timeout(Duration.ofMillis(Math.max(1L, properties.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(AUTHORIZATION_HEADER, "Bearer " + properties.getToken().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new CloudDecisionClientException("timeout after " + properties.getTimeoutMs() + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudDecisionClientException("interrupted during http request", e);
        } catch (IOException e) {
            throw new CloudDecisionClientException("http failure: " + e.getClass().getSimpleName(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new CloudDecisionClientException("http status=" + response.statusCode());
        }
        if (!hasText(response.body())) {
            throw new CloudDecisionClientException("empty response");
        }

        return parseResponse(response.body());
    }

    private String serializeRequest(CloudDecisionRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        if (request.getServiceId() != null) {
            root.put("serviceId", request.getServiceId().name());
        }
        put(root, "traceId", request.getTraceId());
        put(root, "taskCode", request.getTaskCode());
        put(root, "phase", request.getPhase());
        put(root, "windowId", request.getWindowId());
        put(root, "taskRunId", request.getTaskRunId());
        put(root, "policyVersion", request.getPolicyVersion());
        put(root, "localDecision", request.getLocalDecision());
        if (request.getCreatedAt() != null) {
            root.put("createdAt", request.getCreatedAt().toString());
        }
        ObjectNode context = objectMapper.createObjectNode();
        if (request.getContext() != null) {
            request.getContext().forEach(context::put);
        }
        root.set("context", context);
        return objectMapper.writeValueAsString(root);
    }

    private void ensureEnabled() {
        if (!properties.isRealTransportEnabled()) {
            throw new CloudDecisionClientException("transport disabled: real transport not enabled");
        }
        if (!hasText(properties.getBaseUrl())) {
            throw new CloudDecisionClientException("transport disabled: missing endpoint");
        }
        if (!hasText(properties.getToken())) {
            throw new CloudDecisionClientException("transport disabled: missing token");
        }
    }

    private URI endpointUri() {
        String baseUrl = properties.getBaseUrl().trim();
        String endpointPath = hasText(properties.getEndpointPath())
                ? properties.getEndpointPath().trim()
                : "/api/cloud/decision";
        if (baseUrl.endsWith("/") && endpointPath.startsWith("/")) {
            return URI.create(baseUrl.substring(0, baseUrl.length() - 1) + endpointPath);
        }
        if (!baseUrl.endsWith("/") && !endpointPath.startsWith("/")) {
            return URI.create(baseUrl + "/" + endpointPath);
        }
        return URI.create(baseUrl + endpointPath);
    }

    private CloudDecisionResponse parseResponse(String body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new CloudDecisionClientException("json parse failure: " + e.getOriginalMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new CloudDecisionClientException("schema mismatch: response must be a json object");
        }

        return CloudDecisionResponse.builder()
                .serviceId(serviceId(root.path("serviceId").asText(null)))
                .traceId(text(root, "traceId"))
                .policyVersion(text(root, "policyVersion"))
                .decision(text(root, "decision"))
                .confidence(root.path("confidence").asDouble(0.0d))
                .ttlMs(root.path("ttlMs").asLong(0L))
                .fallbackReason(text(root, "fallbackReason"))
                .diagnostics(diagnostics(root.path("diagnostics")))
                .build();
    }

    private static CloudDecisionServiceId serviceId(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return CloudDecisionServiceId.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CloudDecisionClientException("schema mismatch: unknown serviceId=" + value, e);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static void put(ObjectNode root, String field, String value) {
        if (value == null) {
            root.putNull(field);
        } else {
            root.put(field, value);
        }
    }

    private static Map<String, String> diagnostics(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
        return values;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
