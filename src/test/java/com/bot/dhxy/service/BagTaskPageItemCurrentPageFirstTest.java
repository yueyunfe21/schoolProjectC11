package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

public final class BagTaskPageItemCurrentPageFirstTest {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/BagService.java"));
        String method = between(source,
                "private boolean interactWithMainBagTaskPageItemExclusive",
                "private Integer findItemPageIndexInOpenMainBag");

        int currentPageScan = method.indexOf(
                "searchItemInCurrentPageOnly(MAIN_BAG, baseAnchor, targetItemTemplate, context)");
        int taskPageScan = method.indexOf(
                "searchItemInTabOnly(MAIN_BAG, baseAnchor, targetItemTemplate");
        require(currentPageScan >= 0, "task-page item path must scan current visible page first");
        require(taskPageScan >= 0, "task-page item path must still scan task tab when current page misses");
        require(currentPageScan < taskPageScan, "current page scan must run before task tab click/search");
        require(method.contains("if (!success)"),
                "task tab click/search must be skipped when current page already matched");
    }

    private static String between(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex);
        if (startIndex < 0 || endIndex < 0) {
            throw new AssertionError("Could not find source slice");
        }
        return text.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
