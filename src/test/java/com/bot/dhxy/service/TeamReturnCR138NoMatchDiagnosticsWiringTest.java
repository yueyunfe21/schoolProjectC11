package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR138 return-team no-match diagnostics.
 */
public final class TeamReturnCR138NoMatchDiagnosticsWiringTest {

    private TeamReturnCR138NoMatchDiagnosticsWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/service/TeamReturnService.java"));

        require(source.contains("team return: return button not found"),
                "CR138 no-match log must remain explicit");
        require(source.contains("nativeTitle={}") && source.contains("player={}/{}"),
                "CR138 no-match log must include native title and parsed player identity");
        require(source.contains("localSession={}") && source.contains("leaderWindow={}")
                        && source.contains("currentWindowReturnMarkerPresent={}")
                        && !source.contains("leaderSignalPresent={}"),
                "CR138 no-match log must identify the current-window return marker, not a misleading leader signal");
        require(source.contains("runtime={}") && source.contains("pathingTarget="),
                "CR138 no-match log must include runtime/pathing state");
        require(source.contains("memberScanCapture={}") && source.contains("bestScore={}")
                        && source.contains("bestPoint=({}, {})") && source.contains("bestRect={}"),
                "CR138 no-match log must include member ROI capture and template best-match diagnostics");
        require(source.contains("lastFoundAgeMs={}") && source.contains("lastClickedAgeMs={}"),
                "CR138 no-match log must include recent found/click ages");
        require(source.contains("ImageFinder.find(snapshot, template, -1.0)"),
                "CR138 no-match diagnostics must record the best candidate score without changing the click threshold");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
