package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for the Zhang Wen special route to Ling Shou Village.
 *
 * <p>The route has two distinct targets: physical pathing must remain Chang'an, while the Zhang
 * Wen transfer dialog must prepare the Ling Shou Village route option. This protects the boundary
 * that previously caused target mismatch between `zhangWenApproach` and `ROUTE_TRANSFER`.</p>
 */
public class LingShouVillageRouteDialogTargetWiringTest {

    public static void main(String[] args) throws Exception {
        Path source = Path.of("src", "main", "java", "com", "bot", "dhxy", "service",
                "NavigationService.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        String routeMethod = extractMethod(text,
                "private NavigationResult navigateToLingShouVillageViaZhangWen(");

        assertContains(routeMethod, ".targetMapName(MAP_CHANG_AN)");
        assertContains(routeMethod, ".source(request.getSource() + \":viaChangAn\")");
        assertContains(routeMethod, ".source(request.getSource() + \":zhangWenApproach\")");

        int approachIndex = routeMethod.indexOf("NavigationResult zhangWenApproachResult");
        int preClickDialogIndex = routeMethod.indexOf("\"navigation:ling-shou-village:before-zhang-wen-click\"");
        int handoffIndex = routeMethod.indexOf("requestLingShouVillageRouteDialogPreparation(");
        int npcClickIndex = routeMethod.indexOf("npcClickService.clickNpcSmart");
        int routeDialogIndex = routeMethod.indexOf("\"navigation:ling-shou-village\"", npcClickIndex);
        require(approachIndex >= 0, "Zhang Wen approach leg missing");
        require(preClickDialogIndex > approachIndex,
                "existing Ling Shou route dialog must be checked after Zhang Wen approach");
        require(preClickDialogIndex < npcClickIndex,
                "existing Ling Shou route dialog must be consumed before another Zhang Wen click");
        require(handoffIndex > approachIndex,
                "Ling Shou route-dialog handoff must happen after Zhang Wen approach");
        require(handoffIndex < npcClickIndex,
                "Ling Shou route-dialog handoff must be armed before clicking Zhang Wen");
        require(routeDialogIndex > npcClickIndex,
                "Ling Shou route-dialog consume must still run after Zhang Wen click when pre-click consume is absent");

        String beforeDialogConsume = routeMethod.substring(0, npcClickIndex);
        assertNotContains(beforeDialogConsume, ".targetMapName(MAP_LING_SHOU_VILLAGE)");

        String handoffMethod = extractMethod(text,
                "private void requestLingShouVillageRouteDialogPreparation(");
        assertContains(handoffMethod, "DialogPreparationRequest.builder()");
        assertContains(handoffMethod, ".operation(DialogOperation.ROUTE_TRANSFER)");
        assertContains(handoffMethod, ".targetKeyword(MAP_LING_SHOU_VILLAGE)");
        assertContains(handoffMethod, ".source(source + \":zhangWenTransfer\")");
        assertContains(handoffMethod, ".findUsableRouteDialogChoice(fromMap, MAP_LING_SHOU_VILLAGE)");
        assertContains(handoffMethod, "runtime.updateDialogPreparationRequest(request)");

        System.out.println("LingShouVillageRouteDialogTargetWiringTest passed");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
