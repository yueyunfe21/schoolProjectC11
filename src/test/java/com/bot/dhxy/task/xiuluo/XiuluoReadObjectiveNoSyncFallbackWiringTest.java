package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR56: READ_OBJECTIVE must consume the accept-time background parse result
 * instead of running synchronous story/task-panel readers in the same phase.
 */
public class XiuluoReadObjectiveNoSyncFallbackWiringTest {

    public static void main(String[] args) throws Exception {
        Path source = Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo", "XiuluoTaskV2.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        String method = extractMethod(text, "private XiuluoStepOutcome readObjective(");

        assertContains(method, "objectiveParseFuture()");
        assertContains(method, "waitForBackgroundObjectiveResult");
        assertContains(method, "recoverBackgroundObjectiveReadFailure");

        assertNotContains(method, "tryReadCurrentStoryObjective");
        assertNotContains(method, "tryReadObjectiveFromTaskPanel");
        assertNotContains(method, "DialogHandleRequest.readStoryObjective");
        assertNotContains(method, "dialogService.handleDialog");
        assertNotContains(method, "handleKnownXiuluoOptionDialog");
        assertNotContains(method, "handleUnderThreeBlockedDialog");
        assertNotContains(method, "TaskSleep.sleepOrStop");

        System.out.println("XiuluoReadObjectiveNoSyncFallbackWiringTest passed");
    }

    private static String extractMethod(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Method signature not found: " + signature);
        }
        int brace = source.indexOf('{', start);
        if (brace < 0) {
            throw new AssertionError("Method body not found: " + signature);
        }
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        throw new AssertionError("Method body not closed: " + signature);
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }

    private static void assertNotContains(String value, String token) {
        if (value.contains(token)) {
            throw new AssertionError("Forbidden token present: " + token);
        }
    }
}
