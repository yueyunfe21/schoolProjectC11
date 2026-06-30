package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for the 修罗 startup return-item fallback.
 *
 * <p>When startup tracker detection misses, the fallback is only a lightweight hot-start probe:
 * open the task-item page once and look for the 修罗 return item there. It must not perform the
 * slower two-attempt reverse full-bag scan used by post-combat return verification.</p>
 */
public final class XiuluoStartupReturnItemTaskPageOnlyWiringTest {

    private XiuluoStartupReturnItemTaskPageOnlyWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String xiuluo = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String bag = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/BagService.java"), StandardCharsets.UTF_8);

        String startup = between(xiuluo,
                "private XiuluoRoundContext resolveStartupTrackerOrReturnItem(",
                "@Deprecated");
        require(startup.contains("tryUseStartupReturnItemOnce("),
                "startup tracker miss must use the one-shot task-page return-item probe");
        require(!startup.contains("for (int attempt = 1; attempt <= RETURN_ITEM_VERIFY_ATTEMPTS; attempt++)"),
                "startup return-item fallback must not loop over RETURN_ITEM_VERIFY_ATTEMPTS");

        String startupProbe = between(xiuluo,
                "private boolean tryUseStartupReturnItemOnce(",
                "private ReturnItemUseResult useReturnItem(");
        require(startupProbe.contains("findAndUseMainBagTaskPageItem(RETURN_ITEM_TEMPLATE"),
                "startup return-item probe must search only the main-bag task page");
        require(!startupProbe.contains("findAndUseItemFromBack"),
                "startup return-item probe must not use reverse full-bag scanning");
        require(startupProbe.contains("startup-return-item"),
                "startup probe logs should be distinguishable from post-combat return attempts");
        require(!xiuluo.contains("findAndUseItemFromBack(BagService.MAIN_BAG, RETURN_ITEM_TEMPLATE"),
                "Xiuluo return item lookup must not use reverse full-bag scanning");

        require(bag.contains("public boolean findAndUseMainBagTaskPageItem("),
                "BagService must expose a task-page-only item use method");
        String taskPageMethod = between(bag,
                "public boolean findAndUseMainBagTaskPageItem(",
                "private boolean interactWithItem(");
        require(taskPageMethod.contains("interactWithMainBagTaskPageItemExclusive("),
                "task-page item method must enter the task-page-only exclusive path");
        require(!taskPageMethod.contains("pageScanOrder("),
                "task-page item method must not scan all bag pages");

        String taskPageExclusive = between(bag,
                "private boolean interactWithMainBagTaskPageItemExclusive(",
                "private Integer findItemPageIndexInOpenMainBag(");
        require(taskPageExclusive.contains("MAIN_BAG_TASK_TAB_INDEX"),
                "task-page exclusive path must explicitly select the main-bag task page");
        require(!taskPageExclusive.contains("pageScanOrder("),
                "task-page exclusive path must not scan all bag pages");
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
