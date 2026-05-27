package com.bot.dhxy.core;

import com.baidu.aip.ocr.AipOcr;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.model.ocr.OcrWordResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central OCR adapter for DHXY screenshots.
 *
 * <p>The service accepts already-captured image files and routes OCR requests to Baidu, the local
 * OCR sidecar, or a hybrid/fallback mode according to {@code bot.dhxy.ocr.provider}. Coordinates
 * returned by word-level APIs are image-local pixels, not screen-absolute or window-relative game
 * coordinates. Callers that crop from a bound game window must add the crop origin and the current
 * window base themselves.</p>
 *
 * <p>This class performs OCR/network or local HTTP calls only. It does not capture screenshots,
 * focus windows, send input, or mutate task state. The Baidu SDK client is guarded by
 * {@link #ocrLock} because callers may scan several registered windows concurrently.</p>
 */
@Slf4j
@Component
public class TextRecognizer {

    private static final String DEFAULT_APP_ID = "7663260";
    private static final String DEFAULT_API_KEY = "sPCs5AFdc13mfgtqHnovGP5b";
    private static final String DEFAULT_SECRET_KEY = "yoDcd7FEqh4fkC5qLaE3igfQ0wEbPudx";
    private static final Pattern COORDINATE_LINK_PATTERN =
            Pattern.compile("[\\(（]\\s*\\d+\\s*[,，]\\s*\\d+\\s*[\\)）]");

    private final BotProperties.OcrConfig ocrConfig;
    private final AipOcr client;
    private final Object ocrLock = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient localHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    /**
     * Create the OCR router from Spring configuration.
     *
     * @param botProperties optional application configuration; when null or incomplete, OCR falls
     *                      back to the built-in provider defaults and Baidu credentials.
     */
    public TextRecognizer(BotProperties botProperties) {
        this.ocrConfig = botProperties == null ? new BotProperties.OcrConfig() : botProperties.getOcr();
        log.info("[ocr] initializing Baidu OCR client, provider={}", providerName());
        client = new AipOcr(
                valueOrDefault(ocrConfig.getAppId(), DEFAULT_APP_ID),
                valueOrDefault(ocrConfig.getApiKey(), DEFAULT_API_KEY),
                valueOrDefault(ocrConfig.getSecretKey(), DEFAULT_SECRET_KEY));
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
    }

    /**
     * Read all OCR text from an image file using the configured provider route.
     *
     * @param imagePath filesystem path to an existing image; null is not expected and provider
     *                  failures are converted to an empty string.
     * @return concatenated OCR text, or an empty string when OCR is unavailable or no words are
     * found. The method may call the local OCR HTTP service or Baidu OCR depending on
     * {@code bot.dhxy.ocr.provider}.
     */
    public String readText(String imagePath) {
        OcrProvider provider = provider();
        if (provider == OcrProvider.LOCAL) {
            return tryLocalReadText(imagePath).orElse("");
        }
        if (provider == OcrProvider.HYBRID) {
            Optional<String> local = tryLocalReadText(imagePath);
            if (local.isPresent() && !local.get().isBlank()) {
                log.info("[ocr-hybrid] readText selected=local path={} localLen={} local='{}'",
                        imagePath, local.get().length(), abbreviate(local.get()));
                return local.get();
            }
            String baidu = readTextBaidu(imagePath);
            log.info("[ocr-hybrid] readText selected=baidu reason=local-empty path={} baiduLen={} baidu='{}'",
                    imagePath, baidu.length(), abbreviate(baidu));
            return baidu;
        }

        String baidu = readTextBaidu(imagePath);
        if (provider == OcrProvider.COMPARE) {
            Optional<String> local = tryLocalReadText(imagePath);
            log.info("[ocr-compare] readText path={} baiduLen={} localLen={} localAvailable={} baidu='{}' local='{}'",
                    imagePath, baidu.length(), local.map(String::length).orElse(-1), local.isPresent(),
                    abbreviate(baidu), abbreviate(local.orElse("")));
        }
        return baidu;
    }

    /**
     * Return every OCR text block detected in an image file.
     *
     * @param imagePath filesystem path to an existing image; coordinates in the result are relative
     *                  to this full image.
     * @return OCR word boxes. The list is empty when the provider fails or detects no text. This
     * method may perform local HTTP or Baidu network calls but does not touch the active game
     * window or input queue.
     */
    public List<OcrWordResult> getAllTextResults(String imagePath) {
        OcrProvider provider = provider();
        if (provider == OcrProvider.LOCAL) {
            return tryLocalAllTextResults(imagePath).orElseGet(ArrayList::new);
        }
        if (provider == OcrProvider.HYBRID) {
            Optional<List<OcrWordResult>> local = tryLocalAllTextResults(imagePath);
            if (local.isPresent() && !local.get().isEmpty()) {
                log.info("[ocr-hybrid] getAllTextResults selected=local path={} localCount={} localText='{}'",
                        imagePath, local.get().size(), abbreviate(joinText(local.get())));
                return local.get();
            }
            List<OcrWordResult> baidu = getAllTextResultsBaidu(imagePath);
            log.info("[ocr-hybrid] getAllTextResults selected=baidu reason=local-empty path={} baiduCount={} baiduText='{}'",
                    imagePath, baidu.size(), abbreviate(joinText(baidu)));
            return baidu;
        }

        List<OcrWordResult> baidu = getAllTextResultsBaidu(imagePath);
        if (provider == OcrProvider.COMPARE) {
            Optional<List<OcrWordResult>> local = tryLocalAllTextResults(imagePath);
            log.info("[ocr-compare] getAllTextResults path={} baiduCount={} localCount={} localAvailable={} baiduText='{}' localText='{}'",
                    imagePath, baidu.size(), local.map(List::size).orElse(-1), local.isPresent(),
                    abbreviate(joinText(baidu)), abbreviate(local.map(TextRecognizer::joinText).orElse("")));
        }
        return baidu;
    }

    /**
     * Run word-level OCR through the local OCR sidecar only.
     *
     * @param imagePath filesystem path to an existing image; result coordinates are image-local
     *                  pixels.
     * @return local OCR word boxes, or an empty list when the sidecar is unavailable or no text is
     * found. This is intended for diagnostics and does not fallback to Baidu.
     */
    public List<OcrWordResult> getAllTextResultsLocalOnly(String imagePath) {
        return tryLocalAllTextResults(imagePath).orElseGet(ArrayList::new);
    }

    /**
     * Run word-level OCR for a business matcher with local-first hybrid fallback.
     *
     * @param imagePath filesystem path to an existing image; result coordinates are image-local
     *                  pixels.
     * @param purpose short log label describing the business check, such as target NPC or map
     *                coordinate parsing. Null or blank values are logged as {@code -}.
     * @param matcher optional predicate that decides whether an OCR result is good enough for the
     *                caller. Null means any non-empty result is treated as a match.
     * @return the selected OCR result. In hybrid mode, local OCR is returned when it matches; Baidu
     * is used as fallback when local text is missing or rejected. If neither provider matches, the
     * method returns the most useful unmatched result for diagnostics.
     */
    public List<OcrWordResult> getAllTextResultsForMatch(String imagePath,
                                                         String purpose,
                                                         Predicate<List<OcrWordResult>> matcher) {
        OcrProvider provider = provider();
        if (provider != OcrProvider.HYBRID) {
            List<OcrWordResult> words = getAllTextResults(imagePath);
            boolean matched = safeMatches(matcher, words);
            log.info("[ocr-match] provider={} purpose={} path={} count={} matched={} text='{}'",
                    providerName(), safePurpose(purpose), imagePath, words.size(), matched,
                    abbreviate(joinText(words)));
            return words;
        }

        Optional<List<OcrWordResult>> localOptional = tryLocalAllTextResults(imagePath);
        List<OcrWordResult> localWords = localOptional.orElseGet(ArrayList::new);
        boolean localMatched = safeMatches(matcher, localWords);
        if (localOptional.isPresent() && !localWords.isEmpty() && localMatched) {
            log.info("[ocr-hybrid-match] purpose={} path={} selected=local localAvailable={} localCount={} "
                            + "localMatched=true baiduCount=-1 baiduMatched=false localText='{}'",
                    safePurpose(purpose), imagePath, true, localWords.size(), abbreviate(joinText(localWords)));
            return localWords;
        }

        List<OcrWordResult> baiduWords = getAllTextResultsBaidu(imagePath);
        boolean baiduMatched = safeMatches(matcher, baiduWords);
        String selected;
        List<OcrWordResult> selectedWords;
        if (baiduMatched) {
            selected = "baidu";
            selectedWords = baiduWords;
        } else if (localOptional.isPresent() && !localWords.isEmpty()) {
            selected = "local-unmatched";
            selectedWords = localWords;
        } else {
            selected = "baidu-unmatched";
            selectedWords = baiduWords;
        }
        log.info("[ocr-hybrid-match] purpose={} path={} selected={} localAvailable={} localCount={} "
                        + "localMatched={} baiduCount={} baiduMatched={} localText='{}' baiduText='{}'",
                safePurpose(purpose), imagePath, selected, localOptional.isPresent(), localWords.size(),
                localMatched, baiduWords.size(), baiduMatched,
                abbreviate(joinText(localWords)), abbreviate(joinText(baiduWords)));
        return selectedWords;
    }

    /**
     * Return the effective OCR provider name from configuration.
     *
     * @return normalized provider string used by logs and routing, defaulting to {@code baidu}
     * when configuration is missing.
     */
    public String currentProviderName() {
        return providerName();
    }

    /**
     * Parse current location text such as "长安 [14, 229]".
     */
    /**
     * Parse the current map and logical map coordinate from a screenshot.
     *
     * @param imagePath filesystem path to an existing crop that contains the in-game location text,
     *                  typically the top-left map label area.
     * @return parsed map name plus logical map X/Y coordinates, or null when OCR text does not
     * contain a standard {@code [x, y]} pattern. The method may use local OCR first and Baidu as
     * fallback in hybrid mode.
     */
    public LocationInfo parseLocation(String imagePath) {
        if (provider() == OcrProvider.HYBRID) {
            Optional<String> local = tryLocalReadText(imagePath);
            String localText = local.orElse("");
            LocationInfo localLocation = parseLocationText(localText);
            if (local.isPresent() && localLocation != null) {
                log.info("[ocr-hybrid-match] purpose=parse-location path={} selected=local localMatched=true "
                                + "baiduMatched=false local='{}' location={}",
                        imagePath, abbreviate(localText), localLocation);
                return localLocation;
            }

            String baiduText = readTextBaidu(imagePath);
            LocationInfo baiduLocation = parseLocationText(baiduText);
            log.info("[ocr-hybrid-match] purpose=parse-location path={} selected={} localAvailable={} "
                            + "localMatched={} baiduMatched={} local='{}' baidu='{}' location={}",
                    imagePath,
                    baiduLocation != null ? "baidu" : (localText.isBlank() ? "baidu-unmatched" : "local-unmatched"),
                    local.isPresent(), localLocation != null, baiduLocation != null,
                    abbreviate(localText), abbreviate(baiduText),
                    baiduLocation != null ? baiduLocation : localLocation);
            return baiduLocation != null ? baiduLocation : localLocation;
        }

        String rawText = readText(imagePath);
        LocationInfo location = parseLocationText(rawText);
        if (location == null && rawText != null && !rawText.isEmpty()) {
            log.warn("[ocr] location text did not match standard coordinate format: {}", rawText);
        }
        return location;
    }

    /**
     * Parse current map/coordinate using only the local OCR sidecar.
     *
     * @param imagePath filesystem path to an existing location crop.
     * @return parsed location, or null when the sidecar is unavailable or text cannot be parsed.
     */
    public LocationInfo parseLocationLocalOnly(String imagePath) {
        Optional<String> local = tryLocalReadText(imagePath);
        String rawText = local.orElse("");
        LocationInfo location = parseLocationText(rawText);
        log.info("[ocr-location] provider=local-only path={} available={} matched={} text='{}' location={}",
                imagePath, local.isPresent(), location != null, abbreviate(rawText), location);
        return location;
    }

    /**
     * Parse current map/coordinate using only Baidu OCR.
     *
     * @param imagePath filesystem path to an existing location crop.
     * @return parsed location, or null when Baidu OCR fails or text cannot be parsed.
     */
    public LocationInfo parseLocationBaiduOnly(String imagePath) {
        String rawText = readTextBaidu(imagePath);
        LocationInfo location = parseLocationText(rawText);
        log.info("[ocr-location] provider=baidu-only path={} matched={} text='{}' location={}",
                imagePath, location != null, abbreviate(rawText), location);
        return location;
    }

    private LocationInfo parseLocationText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return null;
        }
        log.info("[ocr] location raw text: {}", rawText);
        Pattern pattern = Pattern.compile("([^0-9\\[]+).*?\\[(\\d+)\\s*,\\s*(\\d+)\\]");
        Matcher matcher = pattern.matcher(rawText);

        if (matcher.find()) {
            String mapName = matcher.group(1).trim();
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(3));
            return new LocationInfo(mapName, x, y);
        }
        return null;
    }

    /**
     * Find the final clickable route coordinate in a map-route result screenshot.
     *
     * @param imagePath filesystem path to an existing image containing route text or coordinate
     *                  links.
     * @return image-local center point of the last coordinate-like OCR block, or null when no
     * clickable coordinate is found. Callers must translate this point before clicking a game
     * window.
     */
    public Point findLastCoordinateLink(String imagePath) {
        if (provider() == OcrProvider.HYBRID) {
            Optional<List<OcrWordResult>> localOptional = tryLocalAllTextResults(imagePath);
            List<OcrWordResult> localWords = localOptional.orElseGet(ArrayList::new);
            Point localPoint = findLastCoordinateLinkInWords(localWords);
            if (localOptional.isPresent() && localPoint != null) {
                log.info("[ocr-hybrid-match] purpose=last-coordinate-link path={} selected=local localMatched=true "
                                + "baiduMatched=false point=({}, {}) localText='{}'",
                        imagePath, localPoint.x, localPoint.y, abbreviate(joinText(localWords)));
                return localPoint;
            }

            List<OcrWordResult> baiduWords = getAllTextResultsBaidu(imagePath);
            Point baiduPoint = findLastCoordinateLinkInWords(baiduWords);
            Point selected = baiduPoint != null ? baiduPoint : localPoint;
            log.info("[ocr-hybrid-match] purpose=last-coordinate-link path={} selected={} localAvailable={} "
                            + "localCount={} localMatched={} baiduCount={} baiduMatched={} point={} localText='{}' baiduText='{}'",
                    imagePath,
                    baiduPoint != null ? "baidu" : (localWords.isEmpty() ? "baidu-unmatched" : "local-unmatched"),
                    localOptional.isPresent(), localWords.size(), localPoint != null,
                    baiduWords.size(), baiduPoint != null, pointText(selected),
                    abbreviate(joinText(localWords)), abbreviate(joinText(baiduWords)));
            return selected;
        }

        List<OcrWordResult> words = getAllTextResults(imagePath);
        return findLastCoordinateLinkInWords(words);
    }

    /**
     * Locate the last coordinate-looking OCR fragment in image-local word boxes.
     *
     * @param words OCR words whose bounding boxes are relative to the image passed to the provider.
     * @return image-local click point for the final coordinate token, or null when no token matches.
     */
    private Point findLastCoordinateLinkInWords(List<OcrWordResult> words) {
        int lastX = -1;
        int lastY = -1;

        for (OcrWordResult word : words) {
            if (word == null || word.getText() == null) {
                continue;
            }
            Matcher matcher = COORDINATE_LINK_PATTERN.matcher(word.getText());
            while (matcher.find()) {
                int textLength = Math.max(word.getText().length(), 1);
                int blockLeft = word.getLeft();
                int blockWidth = Math.max(word.getWidth(), 1);
                int matchLeft = blockLeft + (int) Math.round(blockWidth * (matcher.start() / (double) textLength));
                int matchRight = blockLeft + (int) Math.round(blockWidth * (matcher.end() / (double) textLength));
                lastX = (matchLeft + matchRight) / 2;
                lastX = Math.max(blockLeft, Math.min(blockLeft + blockWidth, lastX));
                lastY = word.getY();

                log.info("OCR coordinate match: words=[{}] match=[{}] block=({}, {}, {}, {}) range=({}, {}) point=({}, {})",
                        word.getText(), matcher.group(), word.getLeft(), word.getTop(), word.getWidth(), word.getHeight(),
                        matchLeft, matchRight, lastX, lastY);
            }
        }

        if (lastX != -1) {
            log.info("OCR coordinate final click point: {}, {}", lastX, lastY);
            return new Point(lastX, lastY);
        }
        return null;
    }

    private boolean safeMatches(Predicate<List<OcrWordResult>> matcher, List<OcrWordResult> words) {
        if (matcher == null) {
            return words != null && !words.isEmpty();
        }
        try {
            return matcher.test(words == null ? List.of() : words);
        } catch (Exception e) {
            log.warn("[ocr-match] matcher failed: reason={}", e.getMessage(), e);
            return false;
        }
    }

    private String safePurpose(String purpose) {
        return purpose == null || purpose.isBlank() ? "-" : purpose.trim();
    }

    private String pointText(Point point) {
        return point == null ? "-" : point.x + "," + point.y;
    }

    private String readTextBaidu(String imagePath) {
        try {
            HashMap<String, String> options = new HashMap<>();
            options.put("language_type", "CHN_ENG");
            options.put("detect_direction", "true");

            JSONObject res;
            synchronized (ocrLock) {
                res = client.basicGeneral(imagePath, options);
            }

            StringBuilder fullText = new StringBuilder();
            if (res.has("words_result")) {
                JSONArray wordsResult = res.getJSONArray("words_result");
                for (int i = 0; i < wordsResult.length(); i++) {
                    fullText.append(wordsResult.getJSONObject(i).getString("words"));
                }
            } else if (res.has("error_msg")) {
                log.warn("[ocr-baidu] readText error: {}", res.getString("error_msg"));
            }
            return fullText.toString();
        } catch (Exception e) {
            log.warn("[ocr-baidu] readText request failed: {}", e.getMessage(), e);
            return "";
        }
    }

    private List<OcrWordResult> getAllTextResultsBaidu(String imagePath) {
        List<OcrWordResult> results = new ArrayList<>();
        try {
            HashMap<String, String> options = new HashMap<>();
            options.put("language_type", "CHN_ENG");

            JSONObject res;
            synchronized (ocrLock) {
                res = client.general(imagePath, options);
            }

            if (!res.has("words_result")) {
                log.warn("[ocr-baidu] getAllTextResults returned no words: path={}", imagePath);
                return results;
            }

            JSONArray wordsResult = res.getJSONArray("words_result");
            for (int i = 0; i < wordsResult.length(); i++) {
                JSONObject item = wordsResult.getJSONObject(i);
                String words = item.getString("words");
                JSONObject loc = item.getJSONObject("location");
                int top = loc.getInt("top");
                int left = loc.getInt("left");
                int width = loc.getInt("width");
                int height = loc.getInt("height");
                results.add(new OcrWordResult(words, left, top, width, height));
            }

            log.info("[ocr-baidu] full scan complete: path={} count={}", imagePath, results.size());
        } catch (Exception e) {
            log.warn("[ocr-baidu] getAllTextResults request failed: path={} reason={}", imagePath, e.getMessage(), e);
        }
        return results;
    }

    private Optional<String> tryLocalReadText(String imagePath) {
        Optional<JsonNode> root = postLocal("/ocr/text", imagePath);
        return root.map(node -> node.path("text").asText(""));
    }

    /**
     * Ask the local OCR sidecar for word boxes and adapt its JSON response into image-local boxes.
     *
     * @param imagePath filesystem path sent to the local OCR process.
     * @return optional list. Empty optional means the sidecar call failed; present empty list means
     * the sidecar responded successfully but detected no words.
     */
    private Optional<List<OcrWordResult>> tryLocalAllTextResults(String imagePath) {
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

    private OcrProvider provider() {
        String value = providerName().trim().toLowerCase();
        return switch (value) {
            case "local" -> OcrProvider.LOCAL;
            case "compare" -> OcrProvider.COMPARE;
            case "hybrid" -> OcrProvider.HYBRID;
            default -> OcrProvider.BAIDU;
        };
    }

    private String providerName() {
        return valueOrDefault(ocrConfig == null ? null : ocrConfig.getProvider(), "baidu");
    }

    private String localEndpoint() {
        String endpoint = valueOrDefault(ocrConfig == null ? null : ocrConfig.getLocalEndpoint(),
                "http://127.0.0.1:18761");
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private int localTimeoutMs() {
        int value = ocrConfig == null ? 10_000 : ocrConfig.getLocalTimeoutMs();
        return Math.max(1_000, value);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String joinText(List<OcrWordResult> words) {
        StringBuilder builder = new StringBuilder();
        if (words != null) {
            for (OcrWordResult word : words) {
                if (word != null && word.getText() != null) {
                    builder.append(word.getText());
                }
            }
        }
        return builder.toString();
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "...";
    }

    private enum OcrProvider {
        BAIDU,
        LOCAL,
        COMPARE,
        HYBRID
    }

}
