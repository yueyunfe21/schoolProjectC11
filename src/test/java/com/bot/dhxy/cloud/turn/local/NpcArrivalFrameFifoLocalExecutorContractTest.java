package com.bot.dhxy.cloud.turn.local;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcArrivalFrameFifoLocalExecutorContractTest {

    @Test
    void replacementIsReachableOnlyFromFirstEndAndThereIsNoThirdSession() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java"),
        StandardCharsets.UTF_8);

        assertTrue(source.contains("for (int attempt = 1; attempt <= 2; attempt++)"));
        assertTrue(source.contains("attempt == 2"));
        assertTrue(source.contains("result.outcome() == SessionOutcome.STORY_BLOCKED"));
        assertTrue(source.contains("result.outcome() == SessionOutcome.EXHAUSTED"));
        assertTrue(source.contains("uiCleanerService.cleanUpAll();"));
        assertTrue(source.contains("turnClient.replaceNpcArrivalFrame("));
        assertTrue(!source.contains("NpcPreparedClickPlan"),
                "the local consumer must stream queue messages, not materialize a candidate plan");
        assertEquals(1, occurrences(source, "uiCleanerService.cleanUpAll();"));
        assertEquals(1, occurrences(source, "turnClient.replaceNpcArrivalFrame("));
    }

    @Test
    void ordinaryClickAndCtrlProbeKeepTheirExistingSafetyBoundaries() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("private static final int CANDIDATE_LIMIT = 12;"));
        assertFalse(source.contains("CANDIDATE_LIMIT = 16"));
        assertTrue(source.contains("insideAllowedRegion(click, spec)"));
        assertTrue(source.contains("InputAction.moveMouse(absoluteX, absoluteY)"));
        assertTrue(source.contains("InputAction.sleep(150)"));
        assertTrue(source.contains("InputAction.clickLeft(absoluteX, absoluteY, 150)"));
        assertTrue(source.contains("InputAction.sleep(1_500)"));
        assertTrue(source.contains("submitExclusiveAndWait("));
        assertTrue(source.contains("\"npcClick:fifoCtrlMenuScan:\""));
        assertTrue(source.contains("{0, 0}, {0, -18}, {18, 0}, {0, 18}, {-18, 0}"));
        assertTrue(source.contains("private static final double CTRL_TEMPLATE_THRESHOLD = 0.80d;"));
        assertTrue(source.contains("private static final int CTRL_MENU_SCAN_W = 150;"));
        assertTrue(source.contains("private static final int CTRL_MENU_SCAN_H = 120;"));
        assertTrue(source.contains("InputActionScope.checkpoint()"));
        assertTrue(source.contains("inputProvider.releaseCtrl();"));
    }

    @Test
    void fifoConsumerPreservesTypedOutcomesAndExactIdentityStaleHandling() throws Exception {
        String source = source();

        for (String outcome : new String[]{
                "CANCELLED",
                "STALE_IGNORED",
                "INPUT_SUBMIT_FAILED",
                "DIALOG_OPEN_UNVERIFIED",
                "VERIFICATION_FAILED",
                "SAFETY_REJECTED",
                "FINAL_FAILED"}) {
            assertTrue(source.contains("NpcClickSmartQueueOutcome." + outcome), outcome);
        }
        assertTrue(source.contains("session.getSessionId().equals(message.getSessionId())"));
        assertTrue(source.contains("spec.windowId().equals(message.getWindowId())"));
        assertTrue(source.contains("spec.businessTaskRunId().equals(message.getTaskRunId())"));
        assertTrue(source.contains("stale session/window/task mismatch ignored"));
        assertTrue(source.contains("TaskCheckpoint.throwIfStopRequested("));
        assertTrue(source.contains("message.getType() == NpcClickSmartQueueMessage.Type.MEMORY"));
        assertTrue(source.contains("message.isOrdinaryClickCandidate()"));
        assertTrue(source.contains("message.getType() == NpcClickSmartQueueMessage.Type.CTRL_CANDIDATES"));
        assertTrue(source.contains("message.getType() == NpcClickSmartQueueMessage.Type.END"));
    }

    @Test
    void verifierAndStoryBoundaryDoNotCollapseTerminalBusinessOutcomesToBoolean() throws Exception {
        String source = source();

        assertTrue(source.contains("dialogService.verifyNpcArrivalExpectedDialog("));
        assertTrue(source.contains("verification.optionDialogVisible()"));
        assertTrue(source.contains("return NpcClickSmartQueueOutcome.DIALOG_OPEN_UNVERIFIED;"));
        assertTrue(source.contains("freshStorySequence("));
        assertTrue(source.contains("story dialog blocker observed at FIFO boundary"));
        assertTrue(source.contains("NpcClickSmartQueueOutcome.CANCELLED"));
        assertTrue(source.contains("fastClickKnownSmallStoryDialog(arguments.source())"));
        assertTrue(source.contains("lastConsumedStorySequence = result.storySequence();"));
    }

    @Test
    void exactWaitAndRetainedPointReplayHaveNoWallClockExpiry() throws Exception {
        String source = source();

        assertFalse(source.contains("WAIT_TIMEOUT_MS"));
        assertFalse(source.contains("WAIT timeout"));
        assertTrue(source.contains("spec.reuseLastVerifiedPoint()"));
        assertTrue(source.contains("replayLastVerifiedPoint(arguments, spec, binding)"));
        assertTrue(source.contains("verifiedReplayPoints.get(ReplayPointKey.from(arguments, spec))"));
        assertTrue(source.contains("fifoRetainedPointReplay"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java"),
                StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
