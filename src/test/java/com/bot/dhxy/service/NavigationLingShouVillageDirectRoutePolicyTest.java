package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR106 Ling Shou Village routing.
 *
 * <p>Runtime navigation to Ling Shou Village must use the CR99 yellow-destination world-map
 * route. The old Zhang Wen transfer chain is kept only as deprecated retained source so it can be
 * compared/debugged, but `navigateToMap` must not dispatch to it.</p>
 */
public class NavigationLingShouVillageDirectRoutePolicyTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String navigation = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/NavigationService.java"), StandardCharsets.UTF_8);

        String navigateToMap = between(navigation,
                "private NavigationResult navigateToMap(NavigationRequest request)",
                "    // =========================\r\n    // Special map-entry routes");
        require(!navigateToMap.contains("navigateToLingShouVillageViaZhangWen("),
                "navigateToMap must not dispatch Ling Shou Village through Zhang Wen");
        require(!navigateToMap.contains("MAP_LING_SHOU_VILLAGE.equals(targetMapName)"),
                "navigateToMap must not keep a Ling Shou Village special branch");
        require(navigateToMap.contains("submitWorldMapSearchAndClickDestination("),
                "navigateToMap must still reach the default CR99 world-map yellow destination path");

        int methodIndex = navigation.indexOf("private NavigationResult navigateToLingShouVillageViaZhangWen(");
        require(methodIndex >= 0, "deprecated Zhang Wen retained method must remain in source");
        String beforeMethod = navigation.substring(Math.max(0, methodIndex - 520), methodIndex);
        require(beforeMethod.contains("@Deprecated"),
                "Zhang Wen retained method must be explicitly marked @Deprecated");
        require(beforeMethod.contains("CR106") && beforeMethod.contains("retained"),
                "Zhang Wen method comment must say it is CR106 retained source only");

        int firstUse = navigation.indexOf("navigateToLingShouVillageViaZhangWen(");
        int secondUse = navigation.indexOf("navigateToLingShouVillageViaZhangWen(", firstUse + 1);
        require(firstUse >= methodIndex && firstUse < navigation.indexOf('{', methodIndex) && secondUse < 0,
                "Zhang Wen route method must have no production call path");

        System.out.println("NavigationLingShouVillageDirectRoutePolicyTest passed");
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
