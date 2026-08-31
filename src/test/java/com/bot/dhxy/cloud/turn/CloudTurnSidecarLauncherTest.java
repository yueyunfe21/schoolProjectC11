package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudTurnSidecarLauncherTest {

    @Test
    void reportsReadyWhenConfiguredLoopbackListenerAlreadyExists() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            TurnClientProperties turn = new TurnClientProperties();
            turn.setBaseUri(URI.create("http://127.0.0.1:" + listener.getLocalPort()));
            CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
            sidecar.setAutoStartEnabled(false);

            CloudTurnSidecarLauncher.Readiness readiness =
                    new CloudTurnSidecarLauncher(turn, sidecar).ensureReady();

            assertTrue(readiness.ready());
        }
    }

    @Test
    void reportsUnavailableWithoutStartingWhenAutoStartIsDisabled() throws Exception {
        int unusedPort;
        try (ServerSocket listener = new ServerSocket(0)) {
            unusedPort = listener.getLocalPort();
        }
        TurnClientProperties turn = new TurnClientProperties();
        turn.setBaseUri(URI.create("http://127.0.0.1:" + unusedPort));
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setAutoStartEnabled(false);

        CloudTurnSidecarLauncher.Readiness readiness =
                new CloudTurnSidecarLauncher(turn, sidecar).ensureReady();

        assertFalse(readiness.ready());
        assertTrue(readiness.message().contains("自动启动已禁用"));
    }

    /**
     * G106 machine-portability contract: an unset brain project path must derive the client project
     * root's sibling, never a hardcoded machine path that silently resurfaces when the property is
     * absent (tests, alternate profiles, a deleted properties line).
     */
    @Test
    void derivesBrainProjectFromSiblingDirectoryWhenUnconfigured() {
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setBrainProjectPath(null);

        // Inject an empty environment so the assertion cannot depend on the developer's own shell.
        Path resolved = new CloudTurnSidecarLauncher(new TurnClientProperties(), sidecar, name -> null)
                .resolveBrainProject();

        Path clientRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        assertEquals(clientRoot.getParent().resolve("dhxy-cloud-brain"), resolved);
    }

    /** An empty property value is the configured "derive it" form, not a Path of "". */
    @Test
    void treatsBlankBrainProjectPathAsUnconfigured() {
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setBrainProjectPath(Path.of(""));

        Path resolved = new CloudTurnSidecarLauncher(new TurnClientProperties(), sidecar, name -> null)
                .resolveBrainProject();

        assertEquals(
                Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
                        .getParent().resolve("dhxy-cloud-brain"),
                resolved);
    }

    /**
     * The launch scripts publish {@code DHXY_CLOUD_BRAIN_ROOT}; client-side auto-start must honour
     * the same override or the two entry points disagree about where Cloud Brain lives.
     */
    @Test
    void environmentOverrideWinsOverDerivationWhenPropertyUnset() {
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setBrainProjectPath(null);

        Path resolved = new CloudTurnSidecarLauncher(new TurnClientProperties(), sidecar,
                name -> CloudTurnSidecarLauncher.CLOUD_BRAIN_ROOT_ENVIRONMENT.equals(name)
                        ? "E:/from-env/dhxy-cloud-brain"
                        : null).resolveBrainProject();

        assertEquals(Path.of("E:/from-env/dhxy-cloud-brain"), resolved);
    }

    /** Explicit configuration outranks the environment, matching the scripts' precedence order. */
    @Test
    void configuredPathWinsOverEnvironmentOverride() {
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setBrainProjectPath(Path.of("E:/from-config/dhxy-cloud-brain"));

        Path resolved = new CloudTurnSidecarLauncher(new TurnClientProperties(), sidecar,
                name -> "E:/from-env/dhxy-cloud-brain").resolveBrainProject();

        assertEquals(Path.of("E:/from-config/dhxy-cloud-brain"), resolved);
    }

    /** A blank environment value must not shadow the sibling derivation. */
    @Test
    void blankEnvironmentOverrideFallsBackToDerivation() {
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setBrainProjectPath(null);

        Path resolved = new CloudTurnSidecarLauncher(new TurnClientProperties(), sidecar,
                name -> "   ").resolveBrainProject();

        assertEquals(
                Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
                        .getParent().resolve("dhxy-cloud-brain"),
                resolved);
    }

    @Test
    void configuredBrainProjectPathWinsOverDerivation() {
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setBrainProjectPath(Path.of("E:/elsewhere/dhxy-cloud-brain"));

        Path resolved = new CloudTurnSidecarLauncher(new TurnClientProperties(), sidecar, name -> null)
                .resolveBrainProject();

        assertEquals(Path.of("E:/elsewhere/dhxy-cloud-brain"), resolved);
    }

    @Test
    void cancellationWinsBeforeListenerOrSidecarChecks() {
        TurnClientProperties turn = new TurnClientProperties();
        turn.setBaseUri(URI.create("http://127.0.0.1:1"));
        CloudTurnSidecarProperties sidecar = new CloudTurnSidecarProperties();
        sidecar.setAutoStartEnabled(true);

        CloudTurnSidecarLauncher.Readiness readiness =
                new CloudTurnSidecarLauncher(turn, sidecar).ensureReady(() -> true);

        assertFalse(readiness.ready());
        assertTrue(readiness.message().contains("启动已取消"));
    }
}
