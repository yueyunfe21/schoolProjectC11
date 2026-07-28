package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;

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
