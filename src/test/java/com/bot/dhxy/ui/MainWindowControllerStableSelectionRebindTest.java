package com.bot.dhxy.ui;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;

import java.util.List;

/**
 * Contract checks for preserving selected players when a cold-start rescan replaces every HWND.
 */
public class MainWindowControllerStableSelectionRebindTest {

    public static void main(String[] args) {
        List<WindowTaskSnapshot> oldFive = List.of(
                snapshot("old-1", "甲", "101"),
                snapshot("old-2", "乙", "102"),
                snapshot("old-3", "丙", "103"),
                snapshot("old-4", "丁", "104"),
                snapshot("old-5", "戊", "105"));
        List<WindowTaskSnapshot> newFive = List.of(
                snapshot("new-1", "甲", "101"),
                snapshot("new-2", "乙", "102"),
                snapshot("new-3", "丙", "103"),
                snapshot("new-4", "丁", "104"),
                snapshot("new-5", "戊", "105"));

        require(MainWindowController.resolveColdStartWindowIds(
                        oldFive.stream().map(WindowTaskSnapshot::getWindowId).toList(), oldFive, newFive)
                        .equals(List.of("new-1", "new-2", "new-3", "new-4", "new-5")),
                "five selected players must rebind from old HWND ids to the same players' new ids");

        List<WindowTaskSnapshot> selectedSubset = List.of(oldFive.get(1), oldFive.get(3));
        require(MainWindowController.resolveColdStartWindowIds(
                        List.of("old-2", "old-4"), selectedSubset, newFive)
                        .equals(List.of("new-2", "new-4")),
                "an explicit subset must not expand to every newly discovered window");

        WindowTaskSnapshot unknown = snapshotWithTitle("old-unknown", "unparseable title");
        require(MainWindowController.resolveColdStartWindowIds(
                        List.of("old-unknown"), List.of(unknown), newFive)
                        .isEmpty(),
                "an unresolved explicit selection must fail closed instead of starting all windows");

        require(MainWindowController.resolveColdStartWindowIds(List.of(), List.of(), newFive)
                        .equals(List.of("new-1", "new-2", "new-3", "new-4", "new-5")),
                "no explicit selection must preserve the existing start-all behavior");

        System.out.println("MainWindowControllerStableSelectionRebindTest: 4/4 PASS");
    }

    private static WindowTaskSnapshot snapshot(String windowId, String playerName, String playerId) {
        return snapshotWithTitle(windowId,
                "大话西游2经典版 - 盛世华章 - " + playerName + "（ID：" + playerId + "）");
    }

    private static WindowTaskSnapshot snapshotWithTitle(String windowId, String title) {
        return new WindowTaskSnapshot(
                windowId,
                "",
                WindowRole.UNKNOWN,
                WindowRuntimeStatus.STOPPED,
                TaskType.TIANTING,
                TaskType.UNKNOWN,
                false,
                null,
                null,
                null,
                null,
                new WindowNativeBinding(windowId, title, "Win32Window", 1L, 0, 0, 1024, 768));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
