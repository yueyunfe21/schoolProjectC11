package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DeferredReturnHomeReplayCoordinatorContractTest {

    @Test
    void replayIsPerWindowAsyncAndSuccessPrecedesBusinessEdge() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinator.java"));
        assertTrue(source.contains("ConcurrentHashMap<String, ExecutorService> replayExecutors"));
        assertTrue(source.contains("Executors.newSingleThreadExecutor"));
        assertTrue(source.contains("ExecutorService executor = executorFor(context.getWindowId())"));
        assertTrue(source.contains("executor.execute(() -> executeClaimed("));
        int success = source.indexOf("RETURN_HOME_REPLAY_SUCCEEDED");
        int business = source.indexOf("publisher.accept(businessExit)", success);
        assertTrue(success >= 0 && business > success,
                "success terminal must be enqueued before the business exit edge");
    }

    @Test
    void everyFailurePathHasTypedTerminalAndCannotSilentlyPublishBusinessExit() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinator.java"));
        assertTrue(source.contains("RETURN_HOME_REPLAY_FAILED"));
        assertTrue(source.contains("RETURN_HOME_REPLAY_IDENTITY_REJECTED"));
        assertFalse(source.contains("replayOnLocalExit("),
                "the observation-thread synchronous replay API must not return");
    }

    @Test
    void exactWindowFenceAllowsTranslationButRejectsHandleOrSizeChanges() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinator.java"));
        assertTrue(source.contains("replay.sourceHwnd().equals(live.getNativeHandle())"));
        assertTrue(source.contains("replay.sourceWidth() == live.getWidth()"));
        assertTrue(source.contains("replay.sourceHeight() == live.getHeight()"));
        assertTrue(source.contains("int dx = live.getX() - replay.sourceX()"));
        assertTrue(source.contains("int dy = live.getY() - replay.sourceY()"));
        assertTrue(source.contains("point.clickX() + dx"));
        assertTrue(source.contains("point.clickY() + dy"));
    }

    @Test
    void correctionGenerationPublishesReplayTerminalWithoutInventingBusinessExit() throws Exception {
        String coordinator = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinator.java"));
        assertTrue(coordinator.contains("publishBusinessExitIfPresent(publisher, businessExit)"));
        assertTrue(coordinator.contains("if (businessExit != null)"));

        String sampler = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java"));
        assertTrue(sampler.contains("submitArmedReturnHomeReplayWithoutBusinessExit();"));
        assertTrue(sampler.contains("context.currentArmedReturnHomeReplay(taskRunId)"));
        assertTrue(sampler.contains("replay.businessTaskRunId(),\n                null,\n                publisher"));
    }

}
