package com.bot.dhxy.cloud.turn.local;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Source contract for the exact-window startup cleanup before local team-role preflight. */
public final class G034StartupUiCleanupContractTest {

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/LocalTeamRolePreflightService.java"),
                StandardCharsets.UTF_8);
        int cleanupCall = source.indexOf("clearStartupUiBlockers(selected, cancelled)");
        int panelProbe = source.indexOf("return detectGroupedBatch(selected, cancelled)");
        require(cleanupCall >= 0 && panelProbe > cleanupCall,
                "startup UI cleanup must run before the existing grouped panel matcher");

        String cleanup = between(source,
                "private void clearStartupUiBlockers(",
                "private Map<String, Preflight> detectGroupedBatch(");
        require(cleanup.contains("windowTaskContextHolder.callWith(context, uiCleanerService::closeAllGenericWindows)"),
                "generic panels must use the existing exact-window cleaner");
        require(cleanup.contains("dialogFramePresenceMechanics.isPresent(dialog)"),
                "story input must be gated by structural dialog presence");
        require(cleanup.contains("submitFrozenExactWindowExclusiveAndWait"),
                "story click must retain the exact frozen HWND input boundary");
        require(cleanup.contains("dialogStoryAdvanceLocalMacroMechanics.advanceStoryDialog(binding)"),
                "startup cleanup must reuse the existing story-advance mechanical action");
        require(!cleanup.contains("clickLeft") && !cleanup.contains("clickRight"),
                "startup cleanup must not invent a new click implementation");
        System.out.println("G034_STARTUP_UI_CLEANUP_CONTRACT_PASS=1/1");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = startIndex < 0 ? -1 : source.indexOf(end, startIndex);
        if (startIndex < 0 || endIndex < 0) {
            throw new AssertionError("missing source markers: " + start + " -> " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
