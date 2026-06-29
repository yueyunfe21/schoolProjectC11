package com.bot.dhxy.window.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR95 P2 identity-drift follow-up wiring.
 */
public class WindowIdentityDriftP2WiringTest {

    public static void main(String[] args) throws Exception {
        String inputQueue = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionQueue.java");
        String inputRequest = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionRequest.java");
        String inputWorker = source("src", "main", "java", "com", "bot", "dhxy", "input", "action",
                "InputActionWorker.java");
        String inputCoordinator = source("src", "main", "java", "com", "bot", "dhxy", "input",
                "WindowAwareInputCoordinator.java");
        String hwndKeyboard = source("src", "main", "java", "com", "bot", "dhxy", "driver",
                "BoundWindowKeyboardService.java");
        String clientIdentity = source("src", "main", "java", "com", "bot", "dhxy", "service",
                "ClientIdentityService.java");
        String teamRole = source("src", "main", "java", "com", "bot", "dhxy", "team",
                "TeamRoleDetectionService.java");
        String autoCombat = source("src", "main", "java", "com", "bot", "dhxy", "service",
                "AutoCombatService.java");
        String navigation = source("src", "main", "java", "com", "bot", "dhxy", "service",
                "NavigationService.java");
        String tracker = source("src", "main", "java", "com", "bot", "dhxy", "core",
                "GameClientTracker.java");

        assertContains(inputQueue, "WindowNativeBindingRefreshService");
        assertContains(inputQueue, "refreshAndCommit");
        assertOrder(inputQueue, "refreshAndValidateNativeBinding(context, description)",
                "new InputActionRequest(context, description, actions, pauseToken)");
        assertOrder(inputQueue, "refreshAndValidateNativeBinding(context, description)",
                "new InputActionRequest(context, description, callback, pauseToken)");
        String inputQueueRefresh = methodBody(inputQueue, "private boolean refreshAndValidateNativeBinding");
        assertContains(inputQueueRefresh, "refreshedBinding.isEmpty()");
        assertContains(inputQueueRefresh, "Input action rejected because live binding refresh is unavailable");
        assertContains(inputRequest, "private final long playerIdentityEpoch;");
        assertContains(inputRequest, "windowContext.getPlayerIdentityEpoch()");
        assertContains(inputRequest, "isPlayerIdentityEpochCurrent()");
        assertContains(inputWorker, "player identity epoch changed");
        assertOrder(inputWorker, "isPlayerIdentityEpochCurrent(request, \"before-focus\")",
                "boolean preferBackgroundKeyboard = canUseBackgroundKeyboard(request)");
        assertContains(inputWorker, "private boolean pressAltShortcut");
        assertContains(inputWorker, "attempt.terminalFailure()");
        assertOrder(inputWorker, "attempt.terminalFailure()",
                "inputCoordinator.focusCurrentWindowInActiveTransaction");
        assertNotContains(inputWorker, "case PRESS_ALT_A -> BoundWindowKeyboardService.AltShortcut.ALT_A");
        assertNotContains(inputWorker, "case PRESS_ALT_C -> BoundWindowKeyboardService.AltShortcut.ALT_C");
        assertNotContains(inputWorker, "case PRESS_ALT_U -> BoundWindowKeyboardService.AltShortcut.ALT_U");

        assertContains(inputCoordinator, "WindowNativeBindingRefreshService");
        assertOrder(inputCoordinator, "refreshAndCommit", "windowFocusService.focusWithoutLock");
        assertContains(inputCoordinator, "FocusPreparationResult.ABORT_INPUT");
        assertContains(inputCoordinator, "live binding refresh unavailable before input focus");
        assertContains(inputCoordinator, "Input window focus rejected because live binding refresh is unavailable");

        assertContains(hwndKeyboard, "WindowNativeBindingRefreshService");
        assertOrder(hwndKeyboard, "refreshAndCommit", "toHwnd(binding)");
        assertContains(hwndKeyboard, "long requestEpoch = context.getPlayerIdentityEpoch();");
        assertContains(hwndKeyboard, "player-identity-epoch-changed");
        assertContains(hwndKeyboard, "live-binding-refresh-unavailable");
        assertContains(hwndKeyboard, "unvalidated-background-shortcut");
        assertContains(hwndKeyboard, "backgroundHwndSupported()");
        assertContains(hwndKeyboard, "ALT_A(\"Alt+A\", 0x41, 0x1E, false)");
        assertContains(hwndKeyboard, "ALT_C(\"Alt+C\", 0x43, 0x2E, false)");
        assertContains(hwndKeyboard, "ALT_U(\"Alt+U\", 0x55, 0x16, false)");
        assertContains(hwndKeyboard, "terminalNotAttempted");
        assertContains(hwndKeyboard, "boolean terminalFailure;");
        assertOrder(hwndKeyboard, "refreshAndCommit(context);",
                "if (requestEpoch != context.getPlayerIdentityEpoch())");
        assertOrder(hwndKeyboard, "if (requestEpoch != context.getPlayerIdentityEpoch())",
                "WindowNativeBinding binding = context.getNativeBinding();");
        assertOrder(hwndKeyboard, "live-binding-refresh-unavailable",
                "if (requestEpoch != context.getPlayerIdentityEpoch())");

        String submitMiniMapClick = methodBody(navigation, "private boolean submitMiniMapClick");
        assertContains(submitMiniMapClick, "if (!pressAlt1ForMiniMap(description + \":open\"))");
        assertOrder(submitMiniMapClick, "if (!pressAlt1ForMiniMap(description + \":open\"))",
                "inputProvider.clickLeft(pixelPoint.x, pixelPoint.y, 200)");
        String miniMapAlt1 = methodBody(navigation, "private boolean pressAlt1ForMiniMap");
        assertContains(miniMapAlt1, "attempt.terminalFailure()");
        assertContains(miniMapAlt1, "return false;");
        assertOrder(miniMapAlt1, "attempt.terminalFailure()", "inputProvider.pressAlt1()");

        String boundTracker = methodBody(tracker, "private boolean useBoundWindowIfAvailable");
        assertContains(tracker, "WindowTitleIdentityParser");
        assertContains(tracker, "private String resolveBoundWindowTitle");
        assertContains(tracker, "retaining previous parseable tracker title");
        assertContains(tracker, "tracker title will stay blank");
        assertNotContains(boundTracker, "? context.getWindowId()");

        assertContains(clientIdentity, "WindowNativeBindingRefreshService");
        assertOrder(clientIdentity, "bindingRefreshService.refreshAndCommit(runtime);",
                "WindowNativeBinding binding = runtime.getNativeBinding();");

        String resolver = methodBody(teamRole, "private Optional<String> resolveCurrentPlayerId");
        assertContains(teamRole, "WindowNativeBindingRefreshService");
        assertOrder(resolver, "bindingRefreshService.refreshAndCommit(runtime);",
                "WindowNativeBinding binding = runtime.getNativeBinding();");
        assertOrder(resolver, "windowTaskContextHolder.rawCurrent()", "context.getNativeWindowTitle()");

        assertContains(autoCombat, "currentPlayerIdentityEpoch()");
        assertContains(autoCombat, "existing.playerIdentityEpoch != epoch");
        assertContains(autoCombat, "auto-combat runtime state invalidated by player identity drift");
        assertContains(autoCombat, "private long playerIdentityEpoch;");

        System.out.println("WindowIdentityDriftP2WiringTest passed");
    }

    private static String source(String first, String... more) throws Exception {
        return Files.readString(Path.of(first, more), StandardCharsets.UTF_8);
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            throw new AssertionError("Expected method missing: " + signature);
        }
        int nextMethod = source.indexOf("\n    private ", start + signature.length());
        if (nextMethod < 0) {
            return source.substring(start);
        }
        return source.substring(start, nextMethod);
    }

    private static void assertContains(String value, String token) {
        if (!value.contains(token)) {
            throw new AssertionError("Expected token missing: " + token);
        }
    }

    private static void assertNotContains(String value, String token) {
        if (value.contains(token)) {
            throw new AssertionError("Unexpected token present: " + token);
        }
    }

    private static void assertOrder(String value, String firstToken, String secondToken) {
        int first = value.indexOf(firstToken);
        int second = value.indexOf(secondToken);
        if (first < 0 || second < 0 || first >= second) {
            throw new AssertionError("Expected token order missing: " + firstToken + " before " + secondToken);
        }
    }
}
