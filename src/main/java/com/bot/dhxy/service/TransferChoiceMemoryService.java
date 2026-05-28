package com.bot.dhxy.service;

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
 * Persistent memory for navigation route option dialogs.
 *
 * <p>This store is intentionally narrower than the global dialog system: it only remembers which
 * option point successfully moved a route from one map toward a target map. Points are stored
 * relative to the dialog rectangle, never as screen-absolute pixels, so the same record can be used
 * across moved windows and multi-window runs.</p>
 */
@Slf4j
@Service
public class TransferChoiceMemoryService {

    private static final Path MEMORY_PATH = Path.of("config", "transfer_choice_memory.json");
    private static final int MAX_FAILURES_BEFORE_DISABLE = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MemoryFile cache;

    /**
     * Find a usable remembered option click for a route dialog.
     *
     * @param fromMap current map name before clicking the transfer option; blank disables lookup.
     * @param targetMap target map the navigation transaction is trying to reach; blank disables lookup.
     * @return remembered dialog-relative click point when confidence is still usable.
     */
    public synchronized Optional<TransferChoiceEntry> findUsable(String fromMap, String targetMap) {
        String key = key(fromMap, targetMap);
        if (key == null) {
            return Optional.empty();
        }
        TransferChoiceEntry entry = load().entries.get(key);
        if (entry == null || !entry.isUsable()) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    /**
     * Record that a route option click successfully reached the expected target map.
     *
     * @param fromMap map observed before the option click.
     * @param fromX logical X observed before the option click; nullable if unavailable.
     * @param fromY logical Y observed before the option click; nullable if unavailable.
     * @param targetMap target map confirmed after the click.
     * @param relativeX dialog-relative clicked X.
     * @param relativeY dialog-relative clicked Y.
     * @param optionText OCR text that matched the clicked option; nullable.
     * @param source short diagnostic source.
     */
    public synchronized void recordSuccess(String fromMap,
                                           Integer fromX,
                                           Integer fromY,
                                           String targetMap,
                                           int relativeX,
                                           int relativeY,
                                           String optionText,
                                           String source) {
        String key = key(fromMap, targetMap);
        if (key == null) {
            return;
        }
        MemoryFile memory = load();
        TransferChoiceEntry entry = memory.entries.computeIfAbsent(key, ignored -> new TransferChoiceEntry());
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
        log.info("[transfer-memory] success key={} clickRel=({}, {}) option={} successCount={}",
                key, relativeX, relativeY, optionText, entry.successCount);
    }

    /**
     * Record that a remembered route option click did not reach the expected target map.
     *
     * @param fromMap map observed before the option click.
     * @param targetMap target map that was expected.
     * @param source short diagnostic source.
     */
    public synchronized void recordFailure(String fromMap, String targetMap, String source) {
        String key = key(fromMap, targetMap);
        if (key == null) {
            return;
        }
        MemoryFile memory = load();
        TransferChoiceEntry entry = memory.entries.get(key);
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
        log.warn("[transfer-memory] failure key={} failCount={} disabled={}",
                key, entry.failCount, entry.disabled);
    }

    private MemoryFile load() {
        if (cache != null) {
            return cache;
        }
        if (!Files.exists(MEMORY_PATH)) {
            cache = new MemoryFile();
            return cache;
        }
        try {
            cache = objectMapper.readValue(MEMORY_PATH.toFile(), MemoryFile.class);
            if (cache.entries == null) {
                cache.entries = new LinkedHashMap<>();
            }
            return cache;
        } catch (IOException e) {
            log.warn("[transfer-memory] load failed, using empty memory: path={}", MEMORY_PATH, e);
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
            log.warn("[transfer-memory] save failed: path={}", MEMORY_PATH, e);
        }
    }

    private String key(String fromMap, String targetMap) {
        String from = normalize(fromMap);
        String target = normalize(targetMap);
        if (from == null || target == null) {
            return null;
        }
        return from + "->" + target;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class MemoryFile {
        public Map<String, TransferChoiceEntry> entries = new LinkedHashMap<>();
    }

    public static class TransferChoiceEntry {
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
