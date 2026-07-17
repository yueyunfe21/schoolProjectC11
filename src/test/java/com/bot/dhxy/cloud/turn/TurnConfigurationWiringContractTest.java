package com.bot.dhxy.cloud.turn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnConfigurationWiringContractTest {

    @TempDir
    Path templateRoot;

    @Test
    void directBeanConstructionIsInertAndStartsNoLoopWindowRunnerOrApplicationThread() {
        TurnContractFixtures.ActionHarness actionHarness = TurnContractFixtures.actionHarness(true, false);
        TurnContractFixtures.TestTaskManager manager = new TurnContractFixtures.TestTaskManager();
        TurnContractFixtures.RecordingCaptureService capture =
                new TurnContractFixtures.RecordingCaptureService();
        TurnCaptureStepExecutor captureExecutor = new TurnCaptureStepExecutor(capture, new TurnPngCodec());
        TurnClientProperties properties = validProperties();
        TurnConfiguration configuration = new TurnConfiguration();
        Set<Long> threadsBefore = liveThreadIds();

        TurnClient turnClient = configuration.turnClient(properties, new ObjectMapper());
        TurnTemplateCache templateCache = configuration.turnTemplateCache(properties, turnClient);
        TurnMatchStepExecutor matchExecutor = configuration.turnMatchStepExecutor(
                templateCache, captureExecutor);
        TurnLoopFactory loopFactory = configuration.turnLoopFactory(
                turnClient, actionHarness.executor());
        TurnLoopRegistry registry = configuration.turnLoopRegistry(loopFactory);
        TurnModeGuard modeGuard = configuration.turnModeGuard(properties, manager, registry);

        assertInstanceOf(HttpsTurnClient.class, turnClient);
        assertNotNull(templateCache);
        assertNotNull(matchExecutor);
        assertNotNull(loopFactory);
        assertNotNull(modeGuard);
        assertEquals(0, registry.size(), "bean wiring must not create a per-window loop");
        assertEquals(0, manager.getRunnerCalls(), "bean wiring must not inspect or start local runners");
        assertEquals(0, capture.totalCalls(), "bean wiring must not capture a window");
        assertEquals(0, actionHarness.queue().submissions.size(), "bean wiring must not send input");
        assertNoNewThreads(threadsBefore);
    }

    @Test
    void invalidPropertiesFailBeforeAnyLoopFactoryOrRegistryCanBeBuilt() {
        TurnClientProperties invalid = validProperties();
        invalid.setRequestTimeoutMs(invalid.getLongWaitTimeoutMs());
        TurnConfiguration configuration = new TurnConfiguration();
        Set<Long> threadsBefore = liveThreadIds();

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> configuration.turnClient(invalid, new ObjectMapper()));

        assertEquals(
                "cloud.turn.request-timeout-ms must be greater than cloud.turn.long-wait-timeout-ms",
                failure.getMessage());
        assertNoNewThreads(threadsBefore);
    }

    private TurnClientProperties validProperties() {
        TurnClientProperties properties = new TurnClientProperties();
        properties.setTemplateRoot(templateRoot);
        properties.setConnectTimeoutMs(500L);
        properties.setLongWaitTimeoutMs(1_000L);
        properties.setRequestTimeoutMs(2_000L);
        return properties;
    }

    private static Set<Long> liveThreadIds() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .map(Thread::threadId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void assertNoNewThreads(Set<Long> threadsBefore) {
        Map<Long, String> newThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(thread -> !threadsBefore.contains(thread.threadId()))
                .collect(Collectors.toUnmodifiableMap(Thread::threadId, Thread::getName));
        assertTrue(newThreads.isEmpty(),
                "inert bean construction must start zero threads, but created " + newThreads);
    }
}
