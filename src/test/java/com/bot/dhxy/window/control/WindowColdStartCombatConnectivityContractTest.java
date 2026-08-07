package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.local.LocalTeamRolePreflightService;
import com.bot.dhxy.cloud.turn.protocol.TurnWindowMetadata;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.runner.context.TaskStartupMode;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the production cold-start ordering that was lost during the thin-client migration. */
class WindowColdStartCombatConnectivityContractTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java");

    @Test
    void selectedTaskCannotRunRolePreflightCalibrationOrCloudStartBeforeCombatGate() throws IOException {
        String method = methodBody("private WindowTaskCommandResult startRemoteSelectedTask", "private WindowTaskCommandResult startRemoteBatch");
        assertOrdered(method,
                "awaitColdStartCombatExit(ids, taskTypes, startEpoch)",
                "prepareLocalTeamRoles(ids, teamSessionKey, startEpoch, roleResolutionDeadlineNanos)",
                "calibrateMainBagTaskTabBeforeRemoteStart(ids, startEpoch)",
                "startOneRemote(deviceId, windowId");
    }

    @Test
    void sameQueueCannotRunRolePreflightCalibrationOrCloudStartBeforeCombatGate() throws IOException {
        String method = methodBody("private WindowTaskCommandResult startRemoteBatch", "private void calibrateMainBagTaskTabBeforeRemoteStart");
        assertOrdered(method,
                "awaitColdStartCombatExit(windowIds, taskTypes, startEpoch)",
                "prepareLocalTeamRoles(windowIds, teamSessionKey, startEpoch, roleResolutionDeadlineNanos)",
                "calibrateMainBagTaskTabBeforeRemoteStart(windowIds, startEpoch)",
                "startOneRemote(");
    }

    @Test
    void waitedStartupModeMustReachTheMetadataSupplier() throws IOException {
        String source = Files.readString(SOURCE);
        String startOne = slice(source, "private WindowTaskCommandDetail startOneRemote", "private void projectRemoteTerminal");
        assertTrue(startOne.contains("teamPreflight, startupMode)"));
        assertTrue(startOne.contains("explicitSupportMember), startupMode")
                        || startOne.contains("explicitLeaderWindowId), startupMode")
                        || startOne.contains("explicitLeaderWindowId),\n                        startupMode"),
                "the explicit-team metadata path must also carry startupMode");
        assertTrue(source.contains("startupMode.name(),"),
                "wire metadata must publish the actual startup mode instead of hardcoding NORMAL");
    }

    @Test
    void combatDeferredModeIsPublishedByTheRealMetadataSupplier() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-after-combat", new GameContext());
        WindowNativeBinding binding = new WindowNativeBinding(
                "0x21", "leader", "cls", 8L, 0, 0, 1024, 768);
        LocalTeamRolePreflightService.Preflight preflight = new LocalTeamRolePreflightService.Preflight(
                context.getWindowId(), LocalTeamRolePreflightService.Role.LEADER, null, false, null);

        TurnWindowMetadata metadata = new WindowTaskControlService.RemoteTurnMetadataSupplier(
                "device-after-combat", context, new FixedRefreshService(binding),
                "team-after-combat", preflight, TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP).get();

        assertEquals("AFTER_COMBAT_EXIT_STARTUP", metadata.startupMode());
    }

    private static String methodBody(String startMarker, String endMarker) throws IOException {
        return slice(Files.readString(SOURCE), startMarker, endMarker);
    }

    private static String slice(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0, "missing production marker: " + startMarker);
        assertTrue(end > start, "missing production boundary after: " + startMarker);
        return source.substring(start, end);
    }

    private static void assertOrdered(String source, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = source.indexOf(marker);
            assertTrue(current >= 0, "missing production stage: " + marker);
            assertTrue(current > previous, "production stage is out of order: " + marker);
            previous = current;
        }
    }

    private static final class FixedRefreshService extends WindowNativeBindingRefreshService {
        private final WindowNativeBinding binding;

        private FixedRefreshService(WindowNativeBinding binding) {
            this.binding = binding;
        }

        @Override
        public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext context) {
            return Optional.of(binding);
        }
    }
}
