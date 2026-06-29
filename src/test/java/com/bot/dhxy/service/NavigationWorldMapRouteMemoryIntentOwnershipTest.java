package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR74 world-map route-result memory ownership.
 *
 * <p>The world-map route-result click path already registers a {@code worldMapRouteClick} intent
 * and creates pending route-result memory against that same intent. The outer {@code navigateToMap}
 * finally block must not register a second same-leg {@code navigateToMap} intent, or the window
 * runner will correctly abandon the pending memory as {@code intent-replaced} before it can ever
 * become clean.</p>
 */
public class NavigationWorldMapRouteMemoryIntentOwnershipTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String navigation = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/NavigationService.java"), StandardCharsets.UTF_8);
        String runner = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java"), StandardCharsets.UTF_8);

        String submitMethod = between(navigation,
                "private NavigationResult submitWorldMapSearchAndClickDestination(",
                "private boolean clickRememberedWorldMapRouteResult(");
        require(submitMethod.contains("registerWindowPathingIntent(request, \"worldMapRouteClick\""),
                "world-map route click path must continue to register its own pathing intent");
        require(submitMethod.contains("rememberPendingWorldMapRouteResultClick("),
                "world-map route click path must continue to create pending route-result memory");
        String legacyRouteBranch = between(submitMethod,
                "registerWindowPathingIntent(request, \"worldMapRouteClick\"",
                "return NavigationResult.pathingStarted(\"world-map legacy route clicked\")");
        require(legacyRouteBranch.contains("WorldMapRouteResultMode.LEGACY_GREEN_LINK"),
                "legacy route-result pending memory must remain in the legacy green mode");
        require(legacyRouteBranch.indexOf("registerWindowPathingIntent(request, \"worldMapRouteClick\"")
                        < legacyRouteBranch.indexOf("rememberPendingWorldMapRouteResultClick("),
                "pending route-result memory must bind to the active worldMapRouteClick intent");

        String worldMapSubmitBlock = between(navigation,
                "NavigationResult submitResult = submitWorldMapSearchAndClickDestination(",
                "if (!submitResult.success()) {");
        require(worldMapSubmitBlock.contains("submitResult.getStatus() == NavigationResultStatus.PATHING_STARTED"),
                "navigateToMap must explicitly handle successful world-map route submission");
        String pathingStartedBranch = between(navigation,
                "if (submitResult.getStatus() == NavigationResultStatus.PATHING_STARTED) {",
                "if (!submitResult.success()) {");
        require(pathingStartedBranch.contains("pathingIntentOwnedByNestedRoute = true"),
                "successful world-map route submission must mark the nested route as owning this pathing leg");
        require(pathingStartedBranch.indexOf("pathingIntentOwnedByNestedRoute = true")
                        < pathingStartedBranch.indexOf("return result;"),
                "nested ownership must be set before navigateToMap returns PATHING_STARTED");

        String finallyBlock = between(navigation,
                "} finally {",
                "LatencyMetrics.info(log, \"navigation.toMap\"");
        require(finallyBlock.contains("pathingIntentOwnedByNestedRoute"),
                "navigateToMap finally must still skip outer registration when a nested route owns the leg");
        require(finallyBlock.contains("reason=nested-route-owns-current-leg"),
                "duplicate intent skip must remain visible in logs");

        String pathingActiveGateBranch = between(navigation,
                "if (shouldYieldForRouteDialogBeforeWorldMap(",
                "log.info(\"navigate to map pathing-active route gate skipped:");
        require(pathingActiveGateBranch.contains("freshSameTargetRoutePending"),
                "same-target route gate must name the fresh-pending decision");
        require(pathingActiveGateBranch.contains("pathingIntentAlreadyActive = true"),
                "same-target route gate must skip the outer duplicate navigateToMap intent");

        String beforeWorldMapGateBranch = between(navigation,
                "NavigationResult gateResult = routeDialogGateBeforeWorldMap(",
                "NavigationResult submitResult = submitWorldMapSearchAndClickDestination(");
        require(beforeWorldMapGateBranch.contains("gateResult.getStatus() == NavigationResultStatus.PATHING_STARTED"),
                "route-dialog/world-map gate PATHING_STARTED must be handled explicitly");
        require(beforeWorldMapGateBranch.contains("pathingIntentAlreadyActive = runtime != null"),
                "route-dialog/world-map gate must skip duplicate outer intent when the active same-target intent owns the leg");

        String settlement = between(runner,
                "private void settlePendingWorldMapRouteResultMemory(",
                "private WindowPathingSnapshot updateUnknownPathing(");
        require(settlement.contains("recordWorldMapRouteResultAbandoned(consumed, \"intent-replaced\")"),
                "real second-navigation intent replacement must still abandon stale pending memory");
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
