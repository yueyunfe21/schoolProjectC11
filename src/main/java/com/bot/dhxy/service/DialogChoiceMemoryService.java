package com.bot.dhxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
 * Persistent memory for reusable option-dialog clicks.
 *
 * <p>Dialog choices such as route-transfer options and task-accept options are stable relative to
 * the dialog rectangle. This store records dialog-relative click points by a scoped business key,
 * never by screen-absolute pixels, so the same record can survive moved windows and multi-window
 * runs. Route-transfer helpers remain here because they define a specific key policy on top of the
 * generic store.</p>
 */
@Slf4j
@Service
public class DialogChoiceMemoryService {

    private static final Path MEMORY_PATH = Path.of("config", "dialog_choice_memory.json");
    private static final Path LEGACY_TRANSFER_MEMORY_PATH = Path.of("config", "transfer_choice_memory.json");
    private static final int MAX_FAILURES_BEFORE_DISABLE = 3;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private MemoryFile cache;

    /**
     * Find a usable remembered option click for a scoped dialog choice.
     *
     * @param scope business scope such as a task code or {@code navigation}; blank disables lookup.
     * @param action stable action within the scope, for example {@code acceptTask} or {@code routeTransfer}.
     * @param contextKey stable context key, such as NPC name or {@code fromMap->targetMap}; blank disables lookup.
     * @return remembered dialog-relative click point when confidence is still usable.
     */
    public synchronized Optional<DialogChoiceEntry> findUsable(String scope, String action, String contextKey) {
        return findByKey(key(scope, action, contextKey));
    }

    /**
     * Record a successful remembered dialog choice.
     *
     * @param scope business scope such as a task code or {@code navigation}.
     * @param action stable action within the scope.
     * @param contextKey stable context key under the action.
     * @param fromMap optional map observed before the option click.
     * @param fromX optional logical X observed before the option click.
     * @param fromY optional logical Y observed before the option click.
     * @param targetMap optional expected/confirmed target map for diagnostics.
     * @param relativeX dialog-relative clicked X.
     * @param relativeY dialog-relative clicked Y.
     * @param optionText OCR text or template id that matched the clicked option; nullable.
     * @param source short diagnostic source.
     */
    public synchronized void recordSuccess(String scope,
                                           String action,
                                           String contextKey,
                                           String fromMap,
                                           Integer fromX,
                                           Integer fromY,
                                           String targetMap,
                                           int relativeX,
                                           int relativeY,
                                           String optionText,
                                           String source) {
        String key = key(scope, action, contextKey);
        if (key == null) {
            return;
        }
        MemoryFile memory = load();
        DialogChoiceEntry entry = memory.entries.computeIfAbsent(key, ignored -> new DialogChoiceEntry());
        entry.scope = normalize(scope);
        entry.action = normalize(action);
        entry.contextKey = normalize(contextKey);
        entry.fromMap = fromMap;
        entry.fromX = fromX;
        entry.fromY = fromY;
        entry.targetMap = targetMap;
        entry.relativeX = relativeX;
        entry.relativeY = relativeY;
        entry.optionText = optionText;
        entry.source = source;
        entry.successCount++;
        entry.failCount = 0;
        entry.disabled = false;
        entry.lastSuccessAt = LocalDateTime.now().toString();
        save(memory);
        log.info("[dialog-choice-memory] success key={} clickRel=({}, {}) option={} successCount={}",
                key, relativeX, relativeY, optionText, entry.successCount);
    }

    /**
     * Record that a remembered dialog choice failed, disabling the key after repeated failures.
     *
     * @param scope business scope such as a task code or {@code navigation}.
     * @param action stable action within the scope.
     * @param contextKey stable context key under the action.
     * @param source short diagnostic source.
     */
    public synchronized void recordFailure(String scope, String action, String contextKey, String source) {
        String key = key(scope, action, contextKey);
        if (key == null) {
            return;
        }
        MemoryFile memory = load();
        DialogChoiceEntry entry = memory.entries.get(key);
        if (entry == null) {
            return;
        }
        entry.failCount++;
        entry.lastFailureAt = LocalDateTime.now().toString();
        entry.source = source;
        if (entry.failCount >= MAX_FAILURES_BEFORE_DISABLE) {
            entry.disabled = true;
        }
        save(memory);
        log.warn("[dialog-choice-memory] failure key={} failCount={} disabled={}",
                key, entry.failCount, entry.disabled);
    }

    public synchronized Optional<DialogChoiceEntry> findUsableRoute(String fromMap, String targetMap) {
        return findUsable("navigation", "routeTransfer", routeContextKey(fromMap, targetMap));
    }

    public synchronized void recordRouteSuccess(String fromMap,
                                                Integer fromX,
                                                Integer fromY,
                                                String targetMap,
                                                int relativeX,
                                                int relativeY,
                                                String optionText,
                                                String source) {
        recordSuccess("navigation", "routeTransfer", routeContextKey(fromMap, targetMap),
                fromMap, fromX, fromY, targetMap, relativeX, relativeY, optionText, source);
    }

    public synchronized void recordRouteFailure(String fromMap, String targetMap, String source) {
        recordFailure("navigation", "routeTransfer", routeContextKey(fromMap, targetMap), source);
    }

    private Optional<DialogChoiceEntry> findByKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        DialogChoiceEntry entry = load().entries.get(key);
        if (entry == null || !entry.isUsable()) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private MemoryFile load() {
        if (cache != null) {
            return cache;
        }
        Path pathToLoad = Files.exists(MEMORY_PATH) ? MEMORY_PATH : LEGACY_TRANSFER_MEMORY_PATH;
        if (!Files.exists(pathToLoad)) {
            cache = new MemoryFile();
            return cache;
        }
        try {
            cache = objectMapper.readValue(pathToLoad.toFile(), MemoryFile.class);
            if (cache.entries == null) {
                cache.entries = new LinkedHashMap<>();
            }
            if (LEGACY_TRANSFER_MEMORY_PATH.equals(pathToLoad)) {
                cache.entries = migrateLegacyRouteKeys(cache.entries);
            }
            return cache;
        } catch (IOException e) {
            log.warn("[dialog-choice-memory] load failed, using empty memory: path={}", pathToLoad, e);
            cache = new MemoryFile();
            return cache;
        }
    }

    private void save(MemoryFile memory) {
        try {
            Path parent = MEMORY_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = MEMORY_PATH.resolveSibling(MEMORY_PATH.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), memory);
            try {
                Files.move(temp, MEMORY_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, MEMORY_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("[dialog-choice-memory] save failed: path={}", MEMORY_PATH, e);
        }
    }

    private String key(String scope, String action, String contextKey) {
        String normalizedScope = normalize(scope);
        String normalizedAction = normalize(action);
        String normalizedContext = normalize(contextKey);
        if (normalizedScope == null || normalizedAction == null || normalizedContext == null) {
            return null;
        }
        return normalizedScope + "|" + normalizedAction + "|" + normalizedContext;
    }

    private String routeContextKey(String fromMap, String targetMap) {
        String from = normalize(fromMap);
        String target = normalize(targetMap);
        return from == null || target == null ? null : from + "->" + target;
    }

    private Map<String, DialogChoiceEntry> migrateLegacyRouteKeys(Map<String, DialogChoiceEntry> legacyEntries) {
        Map<String, DialogChoiceEntry> migrated = new LinkedHashMap<>();
        legacyEntries.forEach((legacyKey, entry) -> {
            String contextKey = entry == null
                    ? legacyKey
                    : routeContextKey(entry.fromMap, entry.targetMap);
            String newKey = key("navigation", "routeTransfer", contextKey == null ? legacyKey : contextKey);
            if (newKey != null && entry != null) {
                entry.scope = "navigation";
                entry.action = "routeTransfer";
                entry.contextKey = contextKey == null ? legacyKey : contextKey;
                migrated.put(newKey, entry);
            }
        });
        return migrated;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class MemoryFile {
        public Map<String, DialogChoiceEntry> entries = new LinkedHashMap<>();
    }

    public static class DialogChoiceEntry {
        public String scope;
        public String action;
        public String contextKey;
        public String fromMap;
        public Integer fromX;
        public Integer fromY;
        public String targetMap;
        public int relativeX;
        public int relativeY;
        public String optionText;
        public String source;
        public int successCount;
        public int failCount;
        public boolean disabled;
        public String lastSuccessAt;
        public String lastFailureAt;

        public boolean isUsable() {
            return !disabled
                    && successCount > 0
                    && failCount < MAX_FAILURES_BEFORE_DISABLE;
        }
    }
}
