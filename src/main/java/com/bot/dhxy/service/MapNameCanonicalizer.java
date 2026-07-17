package com.bot.dhxy.service;

import com.bot.dhxy.vision.OcrTextMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Canonicalizes OCR-read map names against the project's known map-name dictionary.
 *
 * <p>Inputs are raw OCR snippets from task trackers, story text, or other UI labels. The output is
 * either a known canonical map name, or the original trimmed text when the match is ambiguous. The
 * dictionary is loaded lazily from local map-label templates (plus a tiny name-only constant for the
 * few maps that have a cloud transform but no local template), then cached in memory so runtime
 * matching is only a tiny string-distance pass.</p>
 */
@Slf4j
@Service
public class MapNameCanonicalizer {

    private static final Path MAP_LABEL_DIR = Path.of("images", "template", "map_label");
    private static final int UNKNOWN_DISTANCE = 999;

    /**
     * CR262: map names that live only in the cloud-owned transform snapshot (former local
     * {@code config/maps.json}) and have no {@code map_label} template. Kept as a name-only constant
     * so the local {@code config/maps.json} can be deleted without losing fuzzy-correction for these
     * maps. This carries no calibration values, so it is not the "parameter table" the CR250
     * anti-crack work removes.
     */
    private static final List<String> TRANSFORM_ONLY_MAP_NAMES = List.of("天宫", "御马监");

    private final AtomicReference<List<String>> cachedMapNames = new AtomicReference<>();

    /**
     * Resolve a raw OCR map-name candidate to the closest known map name when the match is safe.
     *
     * @param rawMapName OCR text or already-known map name; null/blank is returned as an empty
     *                   result. The value may contain OCR confusion, punctuation, or whitespace.
     * @param source diagnostic source included in correction/ambiguity logs.
     * @return a canonical known map name when exact or safely fuzzy-matched; otherwise the trimmed
     *         original value so callers do not lose the OCR evidence.
     */
    public String canonicalize(String rawMapName, String source) {
        if (rawMapName == null || rawMapName.isBlank()) {
            return "";
        }
        String trimmed = rawMapName.trim();
        String normalized = normalizeForMatch(trimmed);
        if (normalized.isEmpty()) {
            return trimmed;
        }

        List<String> mapNames = knownMapNames();
        for (String candidate : mapNames) {
            if (normalizeForMatch(candidate).equals(normalized)) {
                return candidate;
            }
        }

        List<MapNameCandidate> ranked = mapNames.stream()
                .map(candidate -> new MapNameCandidate(candidate,
                        OcrTextMatcher.editDistance(normalized, normalizeForMatch(candidate))))
                .sorted(Comparator.comparingInt(MapNameCandidate::distance)
                        .thenComparing(candidate -> candidate.name().length()))
                .toList();
        if (ranked.isEmpty()) {
            return trimmed;
        }

        MapNameCandidate best = ranked.get(0);
        int secondDistance = ranked.size() > 1 ? ranked.get(1).distance() : UNKNOWN_DISTANCE;
        if (isSafeCorrection(normalized, best.distance(), secondDistance, source)) {
            log.info("map name corrected: source={} raw={} canonical={} distance={} secondDistance={}",
                    source, trimmed, best.name(), best.distance(), secondDistance);
            return best.name();
        }

        if (best.distance() <= 2) {
            log.warn("map name correction ambiguous: source={} raw={} best={} distance={} secondDistance={}",
                    source, trimmed, best.name(), best.distance(), secondDistance);
        }
        return trimmed;
    }

    private boolean isSafeCorrection(String normalizedRaw, int bestDistance, int secondDistance, String source) {
        if (bestDistance <= 0) {
            return true;
        }
        if (bestDistance == 1) {
            return true;
        }
        if (source != null && source.startsWith("wubei-tracker-green-map:")) {
            return normalizedRaw.length() >= 3
                    && bestDistance <= 2
                    && secondDistance >= bestDistance + 1;
        }
        int maxDistance = normalizedRaw.length() >= 4 ? 2 : 1;
        return bestDistance <= maxDistance && secondDistance >= bestDistance + 2;
    }

    private List<String> knownMapNames() {
        List<String> existing = cachedMapNames.get();
        if (existing != null) {
            return existing;
        }
        List<String> loaded = loadKnownMapNames();
        if (cachedMapNames.compareAndSet(null, loaded)) {
            log.info("Loaded {} known map names for OCR canonicalization", loaded.size());
            return loaded;
        }
        return cachedMapNames.get();
    }

    private List<String> loadKnownMapNames() {
        Set<String> names = new LinkedHashSet<>();
        loadMapLabelTemplateNames(names);
        names.addAll(TRANSFORM_ONLY_MAP_NAMES);
        return new ArrayList<>(names);
    }

    private void loadMapLabelTemplateNames(Set<String> names) {
        if (!Files.isDirectory(MAP_LABEL_DIR)) {
            return;
        }
        try (var stream = Files.list(MAP_LABEL_DIR)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".png"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.png$", ""))
                    .filter(name -> !name.isBlank())
                    .forEach(names::add);
        } catch (IOException e) {
            log.warn("Failed to load map label templates for OCR canonicalization: dir={}", MAP_LABEL_DIR, e);
        }
    }

    private String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s　:：,，。.;；()（）\\[\\]【】<>《》\"'`·|丨/\\\\-]+", "");
    }

    private record MapNameCandidate(String name, int distance) {
    }
}
