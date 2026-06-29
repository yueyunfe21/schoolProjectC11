package com.bot.dhxy.ui;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the user's selected window ids independent from the currently filtered table rows.
 */
final class WindowSelectionMemory {

    private final Set<String> selectedWindowIds = new LinkedHashSet<>();

    List<String> selectedIds() {
        return List.copyOf(selectedWindowIds);
    }

    int size() {
        return selectedWindowIds.size();
    }

    boolean isSelected(String windowId) {
        return hasText(windowId) && selectedWindowIds.contains(windowId);
    }

    void select(String windowId) {
        if (hasText(windowId)) {
            selectedWindowIds.add(windowId);
        }
    }

    void deselect(String windowId) {
        if (hasText(windowId)) {
            selectedWindowIds.remove(windowId);
        }
    }

    void clear() {
        selectedWindowIds.clear();
    }

    void replace(Collection<String> windowIds) {
        selectedWindowIds.clear();
        if (windowIds == null) {
            return;
        }
        for (String windowId : windowIds) {
            select(windowId);
        }
    }

    void replaceVisibleSelection(Collection<String> visibleWindowIds, Collection<String> selectedVisibleWindowIds) {
        if (visibleWindowIds == null) {
            return;
        }
        Set<String> selectedVisibleIds = normalize(selectedVisibleWindowIds);
        for (String visibleWindowId : visibleWindowIds) {
            if (!hasText(visibleWindowId)) {
                continue;
            }
            if (selectedVisibleIds.contains(visibleWindowId)) {
                selectedWindowIds.add(visibleWindowId);
            } else {
                selectedWindowIds.remove(visibleWindowId);
            }
        }
    }

    void retainKnownIds(Collection<String> knownWindowIds) {
        if (knownWindowIds == null) {
            selectedWindowIds.clear();
            return;
        }
        Set<String> knownIds = normalize(knownWindowIds);
        selectedWindowIds.removeIf(id -> !knownIds.contains(id));
    }

    private static Set<String> normalize(Collection<String> windowIds) {
        Set<String> normalized = new LinkedHashSet<>();
        if (windowIds == null) {
            return normalized;
        }
        for (String windowId : windowIds) {
            if (hasText(windowId)) {
                normalized.add(windowId);
            }
        }
        return normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
