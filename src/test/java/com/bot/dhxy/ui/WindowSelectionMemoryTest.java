package com.bot.dhxy.ui;

import java.util.List;

/**
 * Source-independent guard for preserving window selection while the table is filtered.
 */
public class WindowSelectionMemoryTest {

    public static void main(String[] args) {
        keepsHiddenSelectionsWhenOnlyOneWindowIsVisible();
        dropsOnlyVisibleDeselectedWindow();
        removesSelectionsForUnregisteredWindows();
    }

    private static void keepsHiddenSelectionsWhenOnlyOneWindowIsVisible() {
        WindowSelectionMemory memory = new WindowSelectionMemory();
        memory.replace(List.of("w1", "w2", "w3", "w4", "w5"));

        memory.replaceVisibleSelection(List.of("w3"), List.of("w3"));

        require(memory.selectedIds().equals(List.of("w1", "w2", "w3", "w4", "w5")),
                "search-filtered refresh must not shrink selection to the visible row");
    }

    private static void dropsOnlyVisibleDeselectedWindow() {
        WindowSelectionMemory memory = new WindowSelectionMemory();
        memory.replace(List.of("w1", "w2", "w3"));

        memory.replaceVisibleSelection(List.of("w2"), List.of());

        require(memory.selectedIds().equals(List.of("w1", "w3")),
                "unchecking a visible filtered row should only remove that row");
    }

    private static void removesSelectionsForUnregisteredWindows() {
        WindowSelectionMemory memory = new WindowSelectionMemory();
        memory.replace(List.of("w1", "w2", "w3"));

        memory.retainKnownIds(List.of("w1", "w3"));

        require(memory.selectedIds().equals(List.of("w1", "w3")),
                "selection memory must drop windows that are no longer registered");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
