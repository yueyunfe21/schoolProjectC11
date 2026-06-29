package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR99/CR122 world-map yellow-destination routing.
 *
 * <p>The yellow route-result row only opens the destination mini-map. The final target-coordinate
 * click must follow mini-map handoff semantics: prove movement first, then record movement, queue
 * cleanup, register the coordinate intent, and return PATHING_STARTED.</p>
 */
public class NavigationWorldMapYellowDestinationRoutePolicyTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String navigation = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/NavigationService.java"), StandardCharsets.UTF_8);

        String submitMethod = between(navigation,
                "private NavigationResult submitWorldMapSearchAndClickDestination(",
                "private boolean clickRememberedWorldMapRouteResult(");
        require(navigation.contains("legacyWorldMapGreenLinkEnabled"),
                "legacy green coordinate path must be switchable");
        require(submitMethod.contains("performWorldMapSearchAndClickDestination("),
                "world-map submit must route through the guarded route-result action");
        require(submitMethod.contains("\"worldMapYellowDestinationMiniMap\""),
                "default world-map submit must still register a coordinate-aware yellow-destination intent");
        require(submitMethod.contains("source + \":yellow-destination-mini-map-pathing-confirmed\", true)"),
                "default world-map submit must register target coordinates only after final mini-map movement is confirmed");
        require(submitMethod.contains("NavigationResult.pathingStarted(\"world-map yellow destination mini-map pathing confirmed\")"),
                "default world-map submit must report PATHING_STARTED only for confirmed yellow mini-map handoff");
        require(submitMethod.indexOf("\"worldMapYellowDestinationMiniMap\"")
                        < submitMethod.indexOf("rememberPendingWorldMapRouteResultClick("),
                "yellow-destination route must register before creating yellow pending memory");

        String yellowFlow = between(navigation,
                "private WorldMapDestinationClickResult clickYellowDestinationAndTargetMiniMap(",
                "private void cleanupYellowDestinationRouteQueued(");
        require(yellowFlow.contains("verifyWorldMapRouteDestination("),
                "yellow flow must keep the existing guarded destination-row verification");
        require(yellowFlow.contains("destinationResult.destinationCenterX()"),
                "yellow flow must use the matched yellow destination center as its first click");
        require(yellowFlow.contains("isMiniMapPanelVisible(description + \":after-yellow-click\", true)"),
                "yellow flow must require the target mini-map opened by the yellow destination link");
        require(yellowFlow.contains("resolveMiniMapClickPoint("),
                "yellow flow must reuse the existing mini-map coordinate mapping");
        require(yellowFlow.contains("confirmMiniMapPathingStartedForHandoff("),
                "yellow flow final coordinate click must reuse mini-map handoff movement confirmation");
        String yellowBeforeCleanup = between(yellowFlow,
                "inputProvider.clickLeft(yellowClickX, yellowClickY, 150);",
                "cleanupYellowDestinationRouteQueued(");
        require(!yellowBeforeCleanup.contains("pressAlt1ForMiniMap("),
                "yellow flow must not press Alt+1 before the final coordinate click; the destination mini-map must already be open");
        int finalCoordinateClick = yellowFlow.indexOf("inputProvider.clickLeft(miniMapClickPoint.pixelPoint().x");
        int movementConfirm = yellowFlow.indexOf("confirmMiniMapPathingStartedForHandoff(");
        int recordMovement = yellowFlow.indexOf("gameStateUtil.recordMovementIntent(");
        int cleanupAfterConfirm = yellowFlow.indexOf("cleanupYellowDestinationRouteQueued(");
        int clickedReturn = yellowFlow.indexOf("return WorldMapDestinationClickResult.CLICKED");
        int confirmFailureBranch = yellowFlow.indexOf("if (confirmResult != MiniMapPathingAttemptResult.PATHING_STARTED)");
        int confirmFailureReturn = yellowFlow.indexOf("return WorldMapDestinationClickResult.NOT_FOUND;", confirmFailureBranch);
        require(finalCoordinateClick >= 0,
                "yellow flow must still click the resolved destination mini-map coordinate");
        require(movementConfirm > finalCoordinateClick,
                "yellow flow must confirm movement after the final mini-map coordinate click");
        require(recordMovement > movementConfirm,
                "yellow flow must not record movement intent before movement confirmation");
        require(cleanupAfterConfirm > confirmFailureReturn,
                "yellow flow must not close the destination mini-map before confirmed movement");
        require(clickedReturn > movementConfirm,
                "yellow flow must not report CLICKED/PATHING_STARTED before movement confirmation");
        require(!yellowFlow.contains("findLastWorldMapRouteCoordinate("),
                "yellow flow must not look for the old green coordinate route link");

        String queuedCleanup = between(navigation,
                "private void cleanupYellowDestinationRouteQueued(",
                "private void cleanupYellowDestinationRouteAfterCoordinateClickDirect(");
        require(queuedCleanup.contains("submitExclusiveAndWait("),
                "yellow cleanup must be queued after movement confirmation instead of bypassing input serialization");
        require(queuedCleanup.contains("cleanupYellowDestinationRouteAfterCoordinateClickDirect(source)"),
                "queued cleanup must be the normal caller for direct yellow cleanup");

        String yellowCleanup = between(navigation,
                "private void cleanupYellowDestinationRouteAfterCoordinateClickDirect(",
                "private WorldMapDestinationClickResult clickDestinationFromWorldMapSearchResults(");
        require(!yellowCleanup.contains("submitExclusiveAndWait("),
                "direct yellow cleanup must not enqueue nested input while already inside the worker");
        require(yellowCleanup.contains("pressAlt1ForMiniMap("),
                "yellow cleanup must close the destination mini-map after the final coordinate click");
        require(yellowCleanup.contains("closeMapSearchInputAfterRouteClick("),
                "yellow cleanup must close the route/search panel after the final coordinate click");
        require(yellowCleanup.contains("isWorldMapTitleVisible()"),
                "yellow cleanup must only press Alt+2 when the world map is still visibly open");

        String guardedRouteAction = between(navigation,
                "private boolean performWorldMapSearchAndClickDestination(",
                "private boolean prepareWorldMapSearchResultsDirect(");
        require(guardedRouteAction.contains("&& !legacyWorldMapGreenLinkEnabled"),
                "default yellow route must be disabled only by the explicit legacy switch");
        String legacyFlow = between(guardedRouteAction,
                "} else {",
                "long scanElapsedMs = System.currentTimeMillis() - scanStartMs;");
        require(legacyFlow.contains("clickRememberedWorldMapRouteResult("),
                "legacy world-map route-result memory fast path must remain isolated");
        require(legacyFlow.contains("clickDestinationFromWorldMapSearchResults("),
                "legacy green coordinate link path must remain available behind the switch");
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
