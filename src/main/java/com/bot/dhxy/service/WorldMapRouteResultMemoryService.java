package com.bot.dhxy.service;

import com.bot.dhxy.model.navigation.WorldMapRouteResultMemoryEntry;
import com.bot.dhxy.model.navigation.WorldMapRouteResultMode;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent store for watcher-confirmed world-map route-result row clicks.
 *
 * <p>This store sits behind {@link MemoryService}. Its schema stays separate from dialog-choice
 * storage because route-result rows are not modal dialog options and their click coordinates are
 * stored relative to the game window, not relative to a dialog rectangle. Entries become usable only
 * after live watcher-confirmed successes, so offline replay/debug samples never promote the fast
 * path.</p>
 */
@Slf4j
@Service
public class WorldMapRouteResultMemoryService {

    private static final Path DEFAULT_MEMORY_PATH = Path.of("config", "world_map_route_result_memory.json");
    private static final int CLEAN_CONSECUTIVE_SUCCESS_THRESHOLD = 5;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Path memoryPath;
    private MemoryFile cache;

    public WorldMapRouteResultMemoryService() {
        this(DEFAULT_MEMORY_PATH);
    }

    public WorldMapRouteResultMemoryService(Path memoryPath) {
        this.memoryPath = memoryPath == null ? DEFAULT_MEMORY_PATH : memoryPath;
    }

    /**
     * Find a clean, non-disabled legacy green-link route-result click memory.
     *
     * @param fromMap canonical source map name.
     * @param targetMap canonical destination map name.
     * @return clean entry, or empty when missing/dirty/disabled/blank.
     */
    public synchronized Optional<WorldMapRouteResultMemoryEntry> findClean(String fromMap, String targetMap) {
        return findClean(fromMap, targetMap, WorldMapRouteResultMode.LEGACY_GREEN_LINK);
    }

    /**
     * Find a clean, non-disabled route-result click memory for a specific route mode.
     *
     * @param fromMap canonical source map name.
     * @param targetMap canonical destination map name.
     * @param routeMode route-result memory mode; null means legacy green-link.
     * @return clean entry, or empty when missing/dirty/disabled/blank.
     */
    public synchronized Optional<WorldMapRouteResultMemoryEntry> findClean(String fromMap,
                                                                          String targetMap,
                                                                          WorldMapRouteResultMode routeMode) {
        WorldMapRouteResultMode mode = effectiveRouteMode(routeMode);
        Optional<WorldMapRouteResultMemoryEntry> entry = findEntry(fromMap, targetMap, mode);
        if (entry.isEmpty()) {
            log.info("[world-map-route-memory] lookup skipped: reason=missing routeMode={} fromMap={} targetMap={}",
                    mode, normalize(fromMap), normalize(targetMap));
            return Optional.empty();
        }
        WorldMapRouteResultMemoryEntry value = entry.get();
        if (value.isDisabled()) {
            log.info("[world-map-route-memory] lookup skipped: reason=disabled routeMode={} fromMap={} targetMap={} successCount={} failureCount={}",
                    entryRouteMode(value), value.getFromMap(), value.getTargetMap(),
                    value.getSuccessCount(), value.getFailureCount());
            return Optional.empty();
        }
        if (!value.isClean()) {
            log.info("[world-map-route-memory] lookup skipped: reason=dirty routeMode={} fromMap={} targetMap={} consecutiveSuccessCount={} consecutiveFailureCount={}",
                    entryRouteMode(value), value.getFromMap(), value.getTargetMap(),
                    value.getConsecutiveSuccessCount(), value.getConsecutiveFailureCount());
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * Find any persisted entry, including dirty/disabled ones.
     *
     * @param fromMap canonical source map name.
     * @param targetMap canonical destination map name.
     * @return stored entry, or empty when key parts are blank or absent.
     */
    public synchronized Optional<WorldMapRouteResultMemoryEntry> findEntry(String fromMap, String targetMap) {
        return findEntry(fromMap, targetMap, WorldMapRouteResultMode.LEGACY_GREEN_LINK);
    }

    /**
     * Find any persisted route-result memory entry for a specific route mode.
     *
     * @param fromMap canonical source map name.
     * @param targetMap canonical destination map name.
     * @param routeMode route-result memory mode; null means legacy green-link.
     * @return stored entry, or empty when key parts are blank or absent.
     */
    public synchronized Optional<WorldMapRouteResultMemoryEntry> findEntry(String fromMap,
                                                                          String targetMap,
                                                                          WorldMapRouteResultMode routeMode) {
        WorldMapRouteResultMode mode = effectiveRouteMode(routeMode);
        String key = routeKey(fromMap, targetMap, mode);
        if (key == null) {
            log.info("[world-map-route-memory] lookup skipped: reason=blank-map routeMode={} fromMap={} targetMap={}",
                    mode, normalize(fromMap), normalize(targetMap));
            return Optional.empty();
        }
        return Optional.ofNullable(load().entries.get(key));
    }

    /**
     * Record watcher-confirmed arrival for a pending route-result click.
     *
     * @param pending pending route-result click that owns the watcher settlement.
     */
    public synchronized void recordSuccess(WorldMapRouteResultPendingMemory pending) {
        WorldMapRouteResultMode mode = pendingRouteMode(pending);
        String key = routeKey(pending == null ? null : pending.getFromMap(),
                pending == null ? null : pending.getTargetMap(), mode);
        if (key == null || pending.getRelativeX() == null || pending.getRelativeY() == null) {
            return;
        }
        MemoryFile memory = load();
        WorldMapRouteResultMemoryEntry previous = memory.entries.get(key);
        int consecutiveSuccess = previous == null ? 1 : previous.getConsecutiveSuccessCount() + 1;
        WorldMapRouteResultMemoryEntry next = WorldMapRouteResultMemoryEntry.builder()
                .fromMap(normalize(pending.getFromMap()))
                .targetMap(normalize(pending.getTargetMap()))
                .routeMode(mode)
                .relativeX(pending.getRelativeX())
                .relativeY(pending.getRelativeY())
                .matchedText(normalize(pending.getMatchedText()))
                .successCount(previous == null ? 1 : previous.getSuccessCount() + 1)
                .failureCount(previous == null ? 0 : previous.getFailureCount())
                .consecutiveSuccessCount(consecutiveSuccess)
                .consecutiveFailureCount(0)
                .clean(consecutiveSuccess >= CLEAN_CONSECUTIVE_SUCCESS_THRESHOLD)
                .disabled(previous != null && previous.isDisabled())
                .lastSuccessAt(nowText())
                .lastFailureAt(previous == null ? null : previous.getLastFailureAt())
                .lastAbandonedAt(previous == null ? null : previous.getLastAbandonedAt())
                .source(normalize(pending.getSource()))
                .build();
        memory.entries.put(key, next);
        save(memory);
        log.info("[world-map-route-memory] pending success: routeMode={} fromMap={} targetMap={} rel=({}, {}) clean={} successCount={} failureCount={} consecutiveSuccessCount={} usedMemory={} intentId={} source={}",
                mode, next.getFromMap(), next.getTargetMap(), next.getRelativeX(), next.getRelativeY(),
                next.isClean(), next.getSuccessCount(), next.getFailureCount(),
                next.getConsecutiveSuccessCount(), pending.isUsedMemory(), pending.getIntentId(), next.getSource());
    }

    /**
     * Record watcher-confirmed stopped-away failure for a pending route-result click.
     *
     * @param pending pending route-result click that owns the watcher settlement.
     */
    public synchronized void recordFailure(WorldMapRouteResultPendingMemory pending) {
        WorldMapRouteResultMode mode = pendingRouteMode(pending);
        String key = routeKey(pending == null ? null : pending.getFromMap(),
                pending == null ? null : pending.getTargetMap(), mode);
        if (key == null) {
            return;
        }
        MemoryFile memory = load();
        WorldMapRouteResultMemoryEntry previous = memory.entries.get(key);
        WorldMapRouteResultMemoryEntry next = previous == null
                ? WorldMapRouteResultMemoryEntry.builder()
                .fromMap(normalize(pending.getFromMap()))
                .targetMap(normalize(pending.getTargetMap()))
                .routeMode(mode)
                .relativeX(pending.getRelativeX() == null ? 0 : pending.getRelativeX())
                .relativeY(pending.getRelativeY() == null ? 0 : pending.getRelativeY())
                .matchedText(normalize(pending.getMatchedText()))
                .failureCount(1)
                .consecutiveSuccessCount(0)
                .consecutiveFailureCount(1)
                .clean(false)
                .lastFailureAt(nowText())
                .source(normalize(pending.getSource()))
                .build()
                : previous.toBuilder()
                .routeMode(entryRouteMode(previous))
                .failureCount(previous.getFailureCount() + 1)
                .consecutiveSuccessCount(0)
                .consecutiveFailureCount(previous.getConsecutiveFailureCount() + 1)
                .clean(false)
                .lastFailureAt(nowText())
                .source(normalize(pending.getSource()))
                .build();
        memory.entries.put(key, next);
        save(memory);
        log.warn("[world-map-route-memory] pending failure: routeMode={} fromMap={} targetMap={} clean={} successCount={} failureCount={} consecutiveFailureCount={} usedMemory={} intentId={} source={}",
                entryRouteMode(next), next.getFromMap(), next.getTargetMap(), next.isClean(), next.getSuccessCount(),
                next.getFailureCount(), next.getConsecutiveFailureCount(), pending.isUsedMemory(),
                pending.getIntentId(), next.getSource());
    }

    /**
     * Record abandoned settlement metadata without changing success/failure counters.
     *
     * @param pending pending route-result click replaced or cleared before watcher settlement.
     * @param reason diagnostic reason.
     */
    public synchronized void recordAbandoned(WorldMapRouteResultPendingMemory pending, String reason) {
        WorldMapRouteResultMode mode = pendingRouteMode(pending);
        String key = routeKey(pending == null ? null : pending.getFromMap(),
                pending == null ? null : pending.getTargetMap(), mode);
        if (key == null) {
            return;
        }
        MemoryFile memory = load();
        WorldMapRouteResultMemoryEntry previous = memory.entries.get(key);
        WorldMapRouteResultMemoryEntry next = previous == null
                ? WorldMapRouteResultMemoryEntry.builder()
                .fromMap(normalize(pending.getFromMap()))
                .targetMap(normalize(pending.getTargetMap()))
                .routeMode(mode)
                .relativeX(pending.getRelativeX() == null ? 0 : pending.getRelativeX())
                .relativeY(pending.getRelativeY() == null ? 0 : pending.getRelativeY())
                .matchedText(normalize(pending.getMatchedText()))
                .lastAbandonedAt(nowText())
                .source(normalize(pending.getSource()))
                .build()
                : previous.toBuilder()
                .routeMode(entryRouteMode(previous))
                .lastAbandonedAt(nowText())
                .source(normalize(pending.getSource()))
                .build();
        memory.entries.put(key, next);
        save(memory);
        log.info("[world-map-route-memory] pending abandoned: routeMode={} fromMap={} targetMap={} rel=({}, {}) reason={} successCount={} failureCount={} usedMemory={} intentId={} source={}",
                entryRouteMode(next), next.getFromMap(), next.getTargetMap(), next.getRelativeX(), next.getRelativeY(),
                normalize(reason), next.getSuccessCount(), next.getFailureCount(), pending.isUsedMemory(),
                pending.getIntentId(), next.getSource());
    }

    private MemoryFile load() {
        if (cache != null) {
            return cache;
        }
        if (!Files.exists(memoryPath)) {
            cache = new MemoryFile();
            return cache;
        }
        try {
            cache = objectMapper.readValue(memoryPath.toFile(), MemoryFile.class);
            if (cache.entries == null) {
                cache.entries = new LinkedHashMap<>();
            }
            return cache;
        } catch (IOException e) {
            log.warn("[world-map-route-memory] load failed, using empty memory: path={}", memoryPath, e);
            cache = new MemoryFile();
            return cache;
        }
    }

    private void save(MemoryFile memory) {
        try {
            Path parent = memoryPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = memoryPath.resolveSibling(memoryPath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), memory);
            try {
                Files.move(temp, memoryPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, memoryPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("[world-map-route-memory] save failed: path={}", memoryPath, e);
        }
    }

    private String routeKey(String fromMap, String targetMap, WorldMapRouteResultMode routeMode) {
        String from = normalize(fromMap);
        String target = normalize(targetMap);
        if (from == null || target == null) {
            return null;
        }
        WorldMapRouteResultMode mode = effectiveRouteMode(routeMode);
        if (mode == WorldMapRouteResultMode.LEGACY_GREEN_LINK) {
            return from + "->" + target;
        }
        return mode.name() + "|" + from + "->" + target;
    }

    private WorldMapRouteResultMode pendingRouteMode(WorldMapRouteResultPendingMemory pending) {
        return pending == null ? WorldMapRouteResultMode.LEGACY_GREEN_LINK
                : effectiveRouteMode(pending.getRouteMode());
    }

    private WorldMapRouteResultMode entryRouteMode(WorldMapRouteResultMemoryEntry entry) {
        return entry == null ? WorldMapRouteResultMode.LEGACY_GREEN_LINK
                : effectiveRouteMode(entry.getRouteMode());
    }

    private WorldMapRouteResultMode effectiveRouteMode(WorldMapRouteResultMode routeMode) {
        return routeMode == null ? WorldMapRouteResultMode.LEGACY_GREEN_LINK : routeMode;
    }

    private String nowText() {
        return LocalDateTime.now().toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class MemoryFile {
        public Map<String, WorldMapRouteResultMemoryEntry> entries = new LinkedHashMap<>();
    }
}
