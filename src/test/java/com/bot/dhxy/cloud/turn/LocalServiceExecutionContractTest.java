package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnFrameMetadata;
import com.bot.dhxy.cloud.turn.protocol.TurnFramePurpose;
import com.bot.dhxy.cloud.turn.protocol.TurnRegion;
import com.bot.dhxy.cloud.turn.protocol.TurnStepResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalServiceExecutionContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completedAndFailedResultsPreserveTypedJsonAndOptionalQuestFrame() throws Exception {
        TurnFrame questFrame = questFrame();
        String completedJson = objectMapper.writeValueAsString(
                new QuestResult(true, "wuhuan", 2));
        String failedJson = objectMapper.writeValueAsString(
                new FailureResult(false, "QUEST_DETAIL_CAPTURE_FAILED"));

        LocalServiceExecution completed = LocalServiceExecution.completed(
                "QUEST_DETAIL_CAPTURED", completedJson, questFrame);
        LocalServiceExecution failed = LocalServiceExecution.failed(
                "QUEST_DETAIL_CAPTURE_FAILED", failedJson);

        assertEquals(TurnStepResult.Status.COMPLETED, completed.status());
        assertEquals("QUEST_DETAIL_CAPTURED", completed.code());
        assertNotNull(completed.frame());
        assertEquals(TurnFramePurpose.QUEST_DETAIL, completed.frame().metadata().purpose());
        JsonNode completedTree = objectMapper.readTree(completed.localResultJson());
        assertEquals(true, completedTree.get("captured").asBoolean());
        assertEquals("wuhuan", completedTree.get("task").asText());
        assertEquals(2, completedTree.get("optionCount").asInt());

        assertEquals(TurnStepResult.Status.FAILED, failed.status());
        assertEquals("QUEST_DETAIL_CAPTURE_FAILED", failed.code());
        assertNull(failed.frame(), "a failed local result cannot consume the one frame slot");
        JsonNode failedTree = objectMapper.readTree(failed.localResultJson());
        assertEquals(false, failedTree.get("captured").asBoolean());
        assertEquals("QUEST_DETAIL_CAPTURE_FAILED", failedTree.get("reason").asText());
    }

    @Test
    void rejectsIllegalStatusFramePurposeHashAndDimensionPairings() {
        TurnFrame questFrame = questFrame();

        assertThrows(IllegalArgumentException.class, () -> new LocalServiceExecution(
                TurnStepResult.Status.NOT_RUN, "NOT_RUN", null, null));
        assertThrows(IllegalArgumentException.class, () -> new LocalServiceExecution(
                TurnStepResult.Status.FAILED, "FAILED", "{}", questFrame));

        TurnFrame wrongPurpose = frame(TurnFramePurpose.CAPTURE, 2, 2, 45, 67);
        IllegalArgumentException purposeFailure = assertThrows(
                IllegalArgumentException.class,
                () -> LocalServiceExecution.completed("OK", "{}", wrongPurpose));
        assertEquals("local Service frame purpose must be QUEST_DETAIL or TASK_TRACKER_PANEL",
                purposeFailure.getMessage());

        TurnFrameMetadata badHashMetadata = new TurnFrameMetadata(
                TurnFramePurpose.QUEST_DETAIL,
                "image/png",
                "0".repeat(64),
                questFrame.metadata().width(),
                questFrame.metadata().height(),
                questFrame.metadata().region(),
                questFrame.metadata().sourceStepIndex());
        TurnFrame badHash = new TurnFrame(badHashMetadata, questFrame.pngBytes());
        IllegalArgumentException hashFailure = assertThrows(
                IllegalArgumentException.class,
                () -> LocalServiceExecution.completed("OK", "{}", badHash));
        assertEquals("local Service frame SHA-256 does not match its raw PNG bytes", hashFailure.getMessage());

        TurnFrameMetadata badDimensionsMetadata = new TurnFrameMetadata(
                TurnFramePurpose.QUEST_DETAIL,
                "image/png",
                questFrame.metadata().sha256(),
                3,
                2,
                new TurnRegion(45, 67, 3, 2),
                4);
        TurnFrame badDimensions = new TurnFrame(badDimensionsMetadata, questFrame.pngBytes());
        IllegalArgumentException dimensionFailure = assertThrows(
                IllegalArgumentException.class,
                () -> LocalServiceExecution.completed("OK", "{}", badDimensions));
        assertEquals("Quest frame dimensions do not match its raw PNG bytes", dimensionFailure.getMessage());
    }

    @Test
    void questFrameBytesAreDefensivelyCopiedAtConstructionAndEveryRead() {
        TurnFrame encoded = questFrame();
        byte[] source = encoded.pngBytes();
        byte[] expected = source.clone();
        TurnFrame supplied = new TurnFrame(encoded.metadata(), source);
        LocalServiceExecution execution = LocalServiceExecution.completed("OK", "{}", supplied);

        source[0] ^= 0x7f;
        byte[] firstRead = execution.frame().pngBytes();
        assertArrayEquals(expected, firstRead);

        firstRead[1] ^= 0x7f;
        assertArrayEquals(expected, execution.frame().pngBytes());
    }

    private static TurnFrame questFrame() {
        return frame(TurnFramePurpose.QUEST_DETAIL, 2, 2, 45, 67);
    }

    private static TurnFrame frame(TurnFramePurpose purpose,
                                   int width,
                                   int height,
                                   int left,
                                   int top) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, 0xff000000 | ((x + 1) << 16) | ((y + 2) << 8) | (x + y));
                }
            }
            return new TurnPngCodec().encode(
                    image,
                    purpose,
                    new TurnRegion(left, top, width, height),
                    4);
        } finally {
            image.flush();
        }
    }

    private record QuestResult(boolean captured, String task, int optionCount) {
    }

    private record FailureResult(boolean captured, String reason) {
    }
}
