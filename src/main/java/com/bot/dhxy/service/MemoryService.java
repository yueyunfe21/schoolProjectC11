package com.bot.dhxy.service;

import com.bot.dhxy.model.navigation.WorldMapRouteResultMemoryEntry;
import com.bot.dhxy.model.navigation.WorldMapRouteResultMode;
import com.bot.dhxy.model.navigation.WorldMapRouteResultPendingMemory;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Single entry point for persisted automation memory.
 *
 * <p>The facade keeps task/navigation code from depending on multiple memory services, while the
 * underlying stores still own their separate schemas and settlement rules. Dialog choices are
 * dialog-relative option memories; world-map route-result entries are pathing memories settled only
 * by watcher proof.</p>
 */
@Service
public class MemoryService {

    private final DialogChoiceMemoryService dialogChoiceMemoryService;
    private final WorldMapRouteResultMemoryService worldMapRouteResultMemoryService;

    public MemoryService(DialogChoiceMemoryService dialogChoiceMemoryService,
                         WorldMapRouteResultMemoryService worldMapRouteResultMemoryService) {
        this.dialogChoiceMemoryService = dialogChoiceMemoryService;
        this.worldMapRouteResultMemoryService = worldMapRouteResultMemoryService;
    }

    /**
     * Find a usable remembered dialog choice.
     *
     * @param scope business scope such as task code.
     * @param action stable action within the scope.
     * @param contextKey stable context key under the action.
     * @return dialog-relative remembered click, if usable.
     */
    public Optional<DialogChoiceEntry> findUsableDialogChoice(String scope, String action, String contextKey) {
        return dialogChoiceMemoryService.findUsable(scope, action, contextKey)
                .map(DialogChoiceEntry::from);
    }

    public Optional<DialogChoiceEntry> findStableTaskDialogChoice(String scope, String action, String contextKey) {
        return dialogChoiceMemoryService.findStableTaskChoice(scope, action, contextKey)
                .map(DialogChoiceEntry::from);
    }

    public void recordDialogChoiceSuccess(String scope,
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
        dialogChoiceMemoryService.recordSuccess(scope, action, contextKey, fromMap, fromX, fromY,
                targetMap, relativeX, relativeY, optionText, source);
    }

    public void recordDialogChoiceFailure(String scope, String action, String contextKey, String source) {
        dialogChoiceMemoryService.recordFailure(scope, action, contextKey, source);
    }

    public Optional<DialogChoiceEntry> findUsableRouteDialogChoice(String fromMap, String targetMap) {
        return dialogChoiceMemoryService.findUsableRoute(fromMap, targetMap).map(DialogChoiceEntry::from);
    }

    public void recordRouteDialogChoiceSuccess(String fromMap,
                                               Integer fromX,
                                               Integer fromY,
                                               String targetMap,
                                               int relativeX,
                                               int relativeY,
                                               String optionText,
                                               String source) {
        dialogChoiceMemoryService.recordRouteSuccess(fromMap, fromX, fromY, targetMap,
                relativeX, relativeY, optionText, source);
    }

    public void recordRouteDialogChoiceFailure(String fromMap, String targetMap, String source) {
        dialogChoiceMemoryService.recordRouteFailure(fromMap, targetMap, source);
    }

    public Optional<WorldMapRouteResultMemoryEntry> findCleanWorldMapRouteResult(String fromMap, String targetMap) {
        return worldMapRouteResultMemoryService.findClean(fromMap, targetMap);
    }

    public Optional<WorldMapRouteResultMemoryEntry> findCleanWorldMapRouteResult(String fromMap,
                                                                                String targetMap,
                                                                                WorldMapRouteResultMode routeMode) {
        return worldMapRouteResultMemoryService.findClean(fromMap, targetMap, routeMode);
    }

    public Optional<WorldMapRouteResultMemoryEntry> findWorldMapRouteResultEntry(String fromMap, String targetMap) {
        return worldMapRouteResultMemoryService.findEntry(fromMap, targetMap);
    }

    public Optional<WorldMapRouteResultMemoryEntry> findWorldMapRouteResultEntry(String fromMap,
                                                                                String targetMap,
                                                                                WorldMapRouteResultMode routeMode) {
        return worldMapRouteResultMemoryService.findEntry(fromMap, targetMap, routeMode);
    }

    public void recordWorldMapRouteResultSuccess(WorldMapRouteResultPendingMemory pending) {
        worldMapRouteResultMemoryService.recordSuccess(pending);
    }

    public void recordWorldMapRouteResultFailure(WorldMapRouteResultPendingMemory pending) {
        worldMapRouteResultMemoryService.recordFailure(pending);
    }

    public void recordWorldMapRouteResultAbandoned(WorldMapRouteResultPendingMemory pending, String reason) {
        worldMapRouteResultMemoryService.recordAbandoned(pending, reason);
    }

    @Value
    public static class DialogChoiceEntry {
        String scope;
        String action;
        String contextKey;
        String fromMap;
        Integer fromX;
        Integer fromY;
        String targetMap;
        int relativeX;
        int relativeY;
        String optionText;
        String source;
        int successCount;
        int failCount;
        int consecutiveSuccessCount;
        int consecutiveFailureCount;
        boolean disabled;
        String lastSuccessAt;
        String lastFailureAt;

        private static DialogChoiceEntry from(DialogChoiceMemoryService.DialogChoiceEntry entry) {
            return new DialogChoiceEntry(
                    entry.scope,
                    entry.action,
                    entry.contextKey,
                    entry.fromMap,
                    entry.fromX,
                    entry.fromY,
                    entry.targetMap,
                    entry.relativeX,
                    entry.relativeY,
                    entry.optionText,
                    entry.source,
                    entry.successCount,
                    entry.failCount,
                    entry.consecutiveSuccessCount,
                    entry.consecutiveFailureCount,
                    entry.disabled,
                    entry.lastSuccessAt,
                    entry.lastFailureAt);
        }
    }
}
