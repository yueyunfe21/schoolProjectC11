package com.bot.dhxy.task.wuhuan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for the 五环 finish-story path after a generic STORY dialog result.
 *
 * <p>The 2026-06-25 09:15 logs showed a no-focus `GIVE_ITEM_IF_AVAILABLE result=STORY`
 * being treated as generic `STORY_IGNORED`; 五环 then entered `SYNC_TASK_PANEL`, which
 * clicked the left tracker and focused the window. This guard keeps the business invariant:
 * `STORY_IGNORED` must run the existing 五环 completion/daily-limit story classifier before
 * any combat wait or tracker sync fallback.</p>
 */
public class FiveRingStoryIgnoredCompletionWiringTest {

    public static void main(String[] args) throws Exception {
        storyIgnoredChecksCompletionStoryBeforeTrackerSync();
    }

    private static void storyIgnoredChecksCompletionStoryBeforeTrackerSync() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java"), StandardCharsets.UTF_8);

        String branch = between(task,
                "if (giveResult == DialogResultStatus.STORY_IGNORED) {",
                "if (giveResult == DialogResultStatus.NO_DIALOG) {");
        int completionCheck = branch.indexOf("resolveFiveRingCompletionStoryOutcome(");
        int dailyLimitCheck = branch.indexOf("isFiveRingDailyLimitStoryVisible(");
        int combatCheck = branch.indexOf("isWindowCombatActive()");
        int trackerSync = branch.indexOf("FiveRingPhase.SYNC_TASK_PANEL");

        require(completionCheck >= 0,
                "STORY_IGNORED must classify 五环 completion/daily-limit story before fallback");
        require(dailyLimitCheck >= 0,
                "STORY_IGNORED must classify 五环 daily-limit story before fallback");
        require(combatCheck < 0 || completionCheck < combatCheck,
                "STORY_IGNORED completion check must run before combat-state fallback");
        require(combatCheck < 0 || dailyLimitCheck < combatCheck,
                "STORY_IGNORED daily-limit check must run before combat-state fallback");
        require(trackerSync < 0 || completionCheck < trackerSync,
                "STORY_IGNORED completion check must run before SYNC_TASK_PANEL tracker sync");
        require(trackerSync < 0 || dailyLimitCheck < trackerSync,
                "STORY_IGNORED daily-limit check must run before SYNC_TASK_PANEL tracker sync");
        require(branch.contains("story-ignored"),
                "source guard sanity check: branch still represents story-ignored handling");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
