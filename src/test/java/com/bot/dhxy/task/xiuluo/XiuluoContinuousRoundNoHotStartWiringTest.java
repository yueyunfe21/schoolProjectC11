package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR130: internal 修罗 round continuation must not run startup screen inspection.
 */
public class XiuluoContinuousRoundNoHotStartWiringTest {

    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo",
                "XiuluoTaskV2.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        String execute = extractMethod(source, "public TaskRunResult execute(TaskExecutionContext executionContext)");

        require(execute.contains(": XiuluoRoundContext.start(round)"),
                "continuous rounds must enter normal PREPARE_ROUND directly");
        require(!execute.contains(": hotStartResolver.resolve(round, false)"),
                "continuous rounds must not run per-round hot-start screen inspection");
        require(execute.contains("startup-screen-resume"),
                "true startup resume path should be named explicitly");
        require(execute.contains("after-combat-exit-startup-screen-resume"),
                "after-combat startup resume path should remain explicit");

        System.out.println("XiuluoContinuousRoundNoHotStartWiringTest passed");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
