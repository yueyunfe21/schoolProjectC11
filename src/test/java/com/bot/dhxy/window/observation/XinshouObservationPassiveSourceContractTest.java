package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the new-player observation plane passive: it may sample evidence, but it cannot own business input.
 */
class XinshouObservationPassiveSourceContractTest {

    private static final Path OBSERVATION_SOURCE = Path.of(
            "src", "main", "java", "com", "bot", "dhxy", "window", "observation");
    private static final List<String> OBSERVATION_ENTRY_POINTS = List.of(
            "WindowObservationSampler.java",
            "WindowObservationRunner.java",
            "SpringObservationRunnerFactory.java");
    private static final List<String> FORBIDDEN_COLLABORATORS = List.of(
            "XinshouLocalTitleHandler",
            "XinshouTitleMechanicalExecutor",
            "XinshouRecoveryLocalMechanics");
    private static final List<String> FORBIDDEN_BRANCH_EXECUTION = List.of(
            "inputSequences.",
            "bagService.",
            "giveItemService.",
            "uiCleanerService.",
            "new Thread(",
            "ExecutorService",
            "Executors.",
            "submitAndWait(",
            "moveAndClickLeft(");

    @Test
    void observationEntryPointsCannotReferenceXinshouBusinessExecutors() throws IOException {
        for (String sourceName : OBSERVATION_ENTRY_POINTS) {
            String source = readSource(sourceName);
            for (String forbidden : FORBIDDEN_COLLABORATORS) {
                assertFalse(source.contains(forbidden),
                        sourceName + " must not reference local new-player business executor " + forbidden);
            }
        }
    }

    @Test
    void xinshouSamplingBranchesCannotExecuteLocalBusinessActions() throws IOException {
        String sampler = readSource("WindowObservationSampler.java");
        String anchorBranch = section(
                sampler,
                "} else if (XinshouAnchorLocalMechanics.INTEREST_KEY.equals(interest.interestKey())) {",
                "} else if (!LocalCombatSignalMechanics.INTEREST_KEY.equals(interest.interestKey())");
        String roiSampling = section(
                sampler,
                "private boolean sampleRoi(",
                "void acknowledgeDeliveredRois(");

        assertPassive(anchorBranch, "new-player anchor observation branch");
        assertPassive(roiSampling, "new-player Tracker/Dialog ROI observation branch");
    }

    @Test
    void samplerCannotHideBusinessInputBehindAnotherXinshouHelper() throws IOException {
        String sampler = readSource("WindowObservationSampler.java");
        String withoutSanctionedKandaInput = withoutMethod(
                sampler, "private void sampleXiuluoLocalKanda(");
        String withoutNonXinshouMechanicalInput = withoutMethod(
                withoutSanctionedKandaInput, "private void sampleTiantingDialogProbe(");
        String withoutAllSanctionedMechanicalInput = withoutMethod(
                withoutNonXinshouMechanicalInput, "private void sampleGhostKingChangshouFlightAssist(");

        assertPassive(withoutAllSanctionedMechanicalInput,
                "observation sampler outside the explicitly sanctioned non-Xinshou mechanical methods");
    }

    @Test
    void legacyAutonomousTitleHandlerRemainsDeleted() {
        assertFalse(Files.exists(OBSERVATION_SOURCE.resolve("XinshouLocalTitleHandler.java")),
                "the autonomous local title handler must not return");
        assertFalse(Files.exists(Path.of(
                        "src", "test", "java", "com", "bot", "dhxy", "window", "observation",
                        "XinshouLocalTitleHandlerTest.java")),
                "the deleted handler's state-machine test must not return");
    }

    private static void assertPassive(String source, String scope) {
        for (String forbidden : FORBIDDEN_BRANCH_EXECUTION) {
            assertFalse(source.contains(forbidden), scope + " must not contain " + forbidden);
        }
    }

    private static String readSource(String sourceName) throws IOException {
        return Files.readString(OBSERVATION_SOURCE.resolve(sourceName), StandardCharsets.UTF_8);
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        assertTrue(start >= 0, "missing source-contract start marker: " + startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(end > start, "missing source-contract end marker: " + endMarker);
        return source.substring(start, end);
    }

    private static String withoutMethod(String source, String signature) {
        int methodStart = source.indexOf(signature);
        assertTrue(methodStart >= 0, "missing sanctioned method signature: " + signature);
        int bodyStart = source.indexOf('{', methodStart + signature.length());
        assertTrue(bodyStart > methodStart, "missing sanctioned method body: " + signature);
        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(0, methodStart) + source.substring(index + 1);
            }
        }
        throw new AssertionError("unterminated sanctioned method body: " + signature);
    }
}
