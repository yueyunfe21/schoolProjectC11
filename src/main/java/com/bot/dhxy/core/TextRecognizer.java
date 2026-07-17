package com.bot.dhxy.core;

import com.bot.dhxy.model.ocr.OcrWordResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Local-only OCR sidecar client.
 *
 * <p>Restored from the {@code 696a12b0} {@code TextRecognizer}, trimmed to the local HTTP word-box
 * path only. It deliberately keeps <b>no</b> Baidu SDK/client, credentials, provider routing, or
 * fallback mode, and adds no new POM dependency. It performs local HTTP calls only; it does not
 * capture screenshots, focus windows, send input, or mutate task state.</p>
 *
 * <p>The endpoint and timeout come from Spring configuration with the built-in defaults
 * {@code http://127.0.0.1:18761} and {@code 10000ms}. {@link #getAllTextResultsLocalOnly(String)}
 * returns a closed result that distinguishes a failed/unavailable sidecar call (empty optional) from
 * a successful call that detected no words (present but empty list); a miss is never faked.</p>
 */
@Slf4j
@Component
public class TextRecognizer {

    private static final String DEFAULT_LOCAL_ENDPOINT = "http://127.0.0.1:18761";

    @Value("${bot.dhxy.ocr.local-endpoint:http://127.0.0.1:18761}")
    private String localOcrEndpoint;

    @Value("${bot.dhxy.ocr.local-timeout-ms:10000}")
    private int localOcrTimeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient localHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    /**
     * Ask the local OCR sidecar for word boxes and adapt its JSON response into image-local boxes.
     *
     * @param imagePath filesystem path sent to the local OCR process.
     * @return empty optional means the sidecar call failed or is unavailable; a present list (which
     * may be empty) means the sidecar responded successfully.
     */
    public Optional<List<OcrWordResult>> getAllTextResultsLocalOnly(String imagePath) {
        Optional<JsonNode> root = postLocal("/ocr/words", imagePath);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        List<OcrWordResult> results = new ArrayList<>();
        JsonNode words = root.get().path("words");
        if (words.isArray()) {
            for (JsonNode item : words) {
                String text = item.path("text").asText("");
                int left = item.path("left").asInt(item.path("x").asInt(0));
                int top = item.path("top").asInt(item.path("y").asInt(0));
                int width = item.path("width").asInt(0);
                int height = item.path("height").asInt(0);
                int x = item.path("x").asInt(left + width / 2);
                int y = item.path("y").asInt(top + height / 2);
                results.add(new OcrWordResult(text, x, y, left, top, width, height,
                        item.path("score").asDouble(0.0)));
            }
        }
        return Optional.of(results);
    }

    /**
     * Post one image path to the local OCR HTTP sidecar.
     *
     * @param path sidecar route, for example {@code /ocr/words}.
     * @param imagePath filesystem path to the source image; the sidecar reads the file directly.
     * @return parsed JSON response, or empty when the sidecar is unavailable, times out, or returns
     * a non-2xx response.
     */
    private Optional<JsonNode> postLocal(String path, String imagePath) {
        String endpoint = localEndpoint();
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("imagePath", imagePath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + path))
                    .timeout(Duration.ofMillis(localTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = localHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[ocr-local] request failed: path={} status={} body={}",
                        path, response.statusCode(), abbreviate(response.body()));
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(response.body()));
        } catch (Exception e) {
            log.info("[ocr-local] unavailable: endpoint={} path={} image={} reason={}",
                    endpoint, path, imagePath, e.getMessage());
            return Optional.empty();
        }
    }

    private String localEndpoint() {
        String endpoint = localOcrEndpoint == null || localOcrEndpoint.isBlank()
                ? DEFAULT_LOCAL_ENDPOINT
                : localOcrEndpoint.trim();
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private int localTimeoutMs() {
        return Math.max(1_000, localOcrTimeoutMs);
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "...";
    }
}
