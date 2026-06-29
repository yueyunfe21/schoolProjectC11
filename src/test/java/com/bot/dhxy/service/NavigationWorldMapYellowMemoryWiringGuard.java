package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NavigationWorldMapYellowMemoryWiringGuard {

    public static void main(String[] args) throws Exception {
        Path navigationPath = Path.of("src/main/java/com/bot/dhxy/service/NavigationService.java").toAbsolutePath();
        String navigation = Files.readString(navigationPath, StandardCharsets.UTF_8);

        String perform = between(navigation,
                "private boolean performWorldMapSearchAndClickDestination(",
                "private boolean prepareWorldMapSearchResultsDirect(");
        require(perform.contains("clickRememberedYellowDestinationAndTargetMiniMap("),
                "yellow route should try remembered yellow-row memory before fresh OCR");
        require(perform.indexOf("clickRememberedYellowDestinationAndTargetMiniMap(")
                        < perform.indexOf("clickYellowDestinationAndTargetMiniMap("),
                "yellow memory fast path must run before fresh yellow OCR/template scan");
        require(perform.contains("memoryStatus == WorldMapDestinationClickResult.NOT_FOUND"),
                "yellow route should run fresh OCR only when no remembered yellow row was attempted");
        require(perform.contains("status = memoryStatus"),
                "failed yellow memory attempts must force route UI cleanup/reprepare instead of scanning dirty UI");

        String navigateToNpc = between(navigation,
                "public NavigationResult navigateToNPC(",
                "private NavigationResult navigateToMap(");
        require(navigateToNpc.indexOf("NavigationResult mapResult = navigateToMap(")
                        < navigateToNpc.indexOf("NavigationResult currentMapResult = navigateInCurrentMap("),
                "navigateToNPC must keep map navigation first and current-map coordinate navigation second");

        String navigateToMap = between(navigation,
                "private NavigationResult navigateToMap(",
                "private RecentPathingMapCheck confirmCurrentMapFromRecentPathingSnapshot(");
        require(navigateToMap.contains("gameStateUtil.confirmCurrentMapFresh("),
                "top-level navigateToMap must keep fresh current-map confirmation");
        require(navigateToMap.indexOf("NavigationResult.arrived(\"target map confirmed by stale-cache guard\")")
                        < navigateToMap.indexOf("submitWorldMapSearchAndClickDestination("),
                "already-on-target-map confirmation must return ARRIVED before yellow world-map search");

        String submit = between(navigation,
                "private NavigationResult submitWorldMapSearchAndClickDestination(",
                "private boolean clickRememberedWorldMapRouteResult(");
        require(submit.contains("\"worldMapYellowDestinationMiniMap\""),
                "yellow route must register the coordinate-aware pathing intent");
        require(submit.contains("WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP"),
                "yellow route pending memory must be stored under the yellow route mode");
        require(submit.indexOf("\"worldMapYellowDestinationMiniMap\"")
                        < submit.indexOf("WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP"),
                "yellow pending memory must be created after the yellow pathing intent registration");

        String yellowFastPath = between(navigation,
                "private WorldMapDestinationClickResult clickRememberedYellowDestinationAndTargetMiniMap(",
                "private void rememberPendingWorldMapRouteResultClick(");
        require(yellowFastPath.contains("findCleanWorldMapRouteResult("),
                "yellow fast path should use route-result memory lookup");
        require(yellowFastPath.contains("WorldMapRouteResultMode.YELLOW_DESTINATION_MINI_MAP"),
                "yellow fast path lookup must be isolated from legacy green memory");
        require(yellowFastPath.contains("isMiniMapPanelVisible(description + \":after-yellow-memory-click\", true)"),
                "yellow fast path must verify the destination mini-map opened before final coordinate click");
        require(!yellowFastPath.contains("verifyOpenedDestinationMiniMap("),
                "yellow fast path must not use current/player map identity as a destination mini-map guard");
        require(!yellowFastPath.contains("gameStateUtil.isSameMapName("),
                "yellow fast path must not compare current/player map label with the remote destination");
        require(yellowFastPath.contains("confirmMiniMapPathingStartedForHandoff("),
                "yellow fast path final coordinate click must reuse mini-map handoff movement confirmation");
        int finalCoordinateClick = yellowFastPath.indexOf("inputProvider.clickLeft(miniMapClickPoint.pixelPoint().x");
        int movementConfirm = yellowFastPath.indexOf("confirmMiniMapPathingStartedForHandoff(");
        int recordMovement = yellowFastPath.indexOf("gameStateUtil.recordMovementIntent(description)");
        int cleanupAfterConfirm = yellowFastPath.indexOf("cleanupYellowDestinationRouteQueued(");
        int successReturn = yellowFastPath.lastIndexOf("return WorldMapDestinationClickResult.CLICKED;");
        int confirmFailureBranch = yellowFastPath.indexOf("if (confirmResult != MiniMapPathingAttemptResult.PATHING_STARTED)");
        int confirmFailureReturn = yellowFastPath.indexOf("return false;", confirmFailureBranch);
        require(movementConfirm > finalCoordinateClick,
                "yellow fast path must confirm movement after the final mini-map coordinate click");
        require(recordMovement > movementConfirm,
                "yellow fast path must not record movement intent before movement confirmation");
        require(cleanupAfterConfirm > confirmFailureReturn,
                "yellow fast path must not queue destination mini-map cleanup before confirmed movement");
        require(successReturn > movementConfirm,
                "yellow fast path must not return success before movement confirmation");
        require(yellowFastPath.contains("state.lastWorldMapRouteUsedMemory = true"),
                "yellow fast path pending metadata must mark usedMemory=true");
        require(yellowFastPath.contains("recordYellowMemoryFastPathFailure("),
                "failed remembered yellow clicks must dirty/demote the yellow memory entry");

        String queuedCleanup = between(navigation,
                "private void cleanupYellowDestinationRouteQueued(",
                "private void cleanupYellowDestinationRouteAfterCoordinateClickDirect(");
        require(queuedCleanup.contains("submitExclusiveAndWait("),
                "yellow cleanup must be queued after movement confirmation instead of bypassing input serialization");

        require(!navigation.contains("private boolean verifyOpenedDestinationMiniMap("),
                "the stale destination mini-map identity helper must be removed from yellow navigation");

        String freshYellow = between(navigation,
                "private WorldMapDestinationClickResult clickYellowDestinationAndTargetMiniMap(",
                "private void cleanupYellowDestinationRouteAfterCoordinateClickDirect(");
        require(freshYellow.contains("state.lastWorldMapRouteUsedMemory = false"),
                "fresh yellow OCR path pending metadata must mark usedMemory=false");
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
