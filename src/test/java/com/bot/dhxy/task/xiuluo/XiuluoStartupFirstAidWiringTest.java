package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR118 修罗 startup first-aid wiring.
 *
 * <p>修罗 first-round hot-start may go straight to tracker shortcut / accept navigation. Startup
 * HP/MP recovery must therefore run before hot-start selection, matching 五倍/五环 startup behavior.</p>
 */
public class XiuluoStartupFirstAidWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        String executeStartupBlock = between(task,
                "if (completedRuns == 0) {",
                "XiuluoRoundContext roundContext = completedRuns == 0");

        int firstAid = executeStartupBlock.indexOf("playerStateService.performStartupFirstAidCheck(context);");
        int incense = executeStartupBlock.indexOf("ensureStartupIncenseBeforeHotStart(context);");

        require(firstAid >= 0, "修罗首轮 hot-start 前必须执行 startup first-aid 血法检查");
        require(incense >= 0, "修罗首轮 hot-start 前仍必须执行 startup 摄妖香检查");
        require(firstAid < incense, "startup first-aid should run before startup incense/hot-start selection");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
