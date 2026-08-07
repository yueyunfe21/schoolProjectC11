package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFactType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouRecoveryObservationContractTest {

    private static final ObservationInterest ANCHOR_INTEREST = new ObservationInterest(
            XinshouAnchorLocalMechanics.INTEREST_KEY, 1L, null);

    @Test
    void reportsOnlyAllowListedRecoveryTemplateNamesOrAbsentFromTheSharedBatch() throws IOException {
        Fixture gate = new Fixture("run-gate", frameWithRecovery("quedingguan_.png"));
        WindowObservationSampler.SampleBatch gateBatch = gate.sampler.collect(List.of(ANCHOR_INTEREST), 1L);
        assertRecoveryFact(gateBatch, "quedingguan_.png");
        assertSameBatch(gateBatch);

        Fixture confirm = new Fixture("run-confirm", frameWithRecovery("confirm.png"));
        WindowObservationSampler.SampleBatch confirmBatch =
                confirm.sampler.collect(List.of(ANCHOR_INTEREST), 1L);
        assertRecoveryFact(confirmBatch, "confirm.png");
        assertSameBatch(confirmBatch);

        Fixture absent = new Fixture("run-absent", blankFrame());
        WindowObservationSampler.SampleBatch absentBatch =
                absent.sampler.collect(List.of(ANCHOR_INTEREST), 1L);
        assertRecoveryFact(absentBatch, "absent");
        assertSameBatch(absentBatch);
    }

    @Test
    void resetAndTaskRunRestartCannotRetainAnOldRecoveryValue() throws IOException {
        MutableFrameTracker tracker = new MutableFrameTracker(frameWithRecovery("confirm.png"));
        Fixture firstRun = new Fixture("run-one", tracker);
        assertRecoveryFact(firstRun.sampler.collect(List.of(ANCHOR_INTEREST), 1L), "confirm.png");

        tracker.setFrame(blankFrame());
        firstRun.sampler.reset();
        assertRecoveryFact(firstRun.sampler.collect(List.of(ANCHOR_INTEREST), 2L), "absent");

        Fixture secondRun = new Fixture("run-two", tracker);
        assertRecoveryFact(secondRun.sampler.collect(List.of(ANCHOR_INTEREST), 1L), "absent");
    }

    @Test
    void recoveryObservationCannotExecuteMechanicsPublishReadyWakeOrInput() throws IOException {
        String sampler = Files.readString(Path.of(
                "src", "main", "java", "com", "bot", "dhxy", "window", "observation",
                "WindowObservationSampler.java"), StandardCharsets.UTF_8);
        String branch = section(
                sampler,
                "} else if (XinshouAnchorLocalMechanics.INTEREST_KEY.equals(interest.interestKey())) {",
                "} else if (!LocalCombatSignalMechanics.INTEREST_KEY.equals(interest.interestKey())");

        assertFalse(branch.contains("XinshouRecoveryLocalMechanics.pressEscapeOnce"));
        assertFalse(branch.contains("XinshouRecoveryLocalMechanics.matchAndClickOnce"));
        assertFalse(branch.contains("PREPARED_ACTION_READY"));
        assertFalse(branch.contains("submitAndWait("));
        assertFalse(branch.contains("moveAndClickLeft("));
        assertFalse(branch.contains("InputAction"));

        Fixture fixture = new Fixture("run-passive", frameWithRecovery("quedingguan_.png"));
        WindowObservationSampler.SampleBatch batch = fixture.sampler.collect(List.of(ANCHOR_INTEREST), 1L);
        assertRecoveryFact(batch, "quedingguan_.png");
        assertTrue(batch.events().isEmpty(), "recovery observation must not publish a foreground wake event");
    }

    private static void assertRecoveryFact(WindowObservationSampler.SampleBatch batch, String expectedValue) {
        List<ObservationFact> recoveryFacts = batch.facts().stream()
                .filter(fact -> fact.factType() == ObservationFactType.XINSHOU_RECOVERY_STATUS)
                .toList();
        assertEquals(1, recoveryFacts.size());
        assertEquals(expectedValue, recoveryFacts.getFirst().value());
        assertTrue(List.of("quedingguan_.png", "confirm.png", "absent")
                .contains(recoveryFacts.getFirst().value()));
    }

    private static void assertSameBatch(WindowObservationSampler.SampleBatch batch) {
        ObservationFact anchor = batch.facts().stream()
                .filter(fact -> fact.factType() == ObservationFactType.XINSHOU_ANCHOR)
                .findFirst()
                .orElseThrow();
        ObservationFact recovery = batch.facts().stream()
                .filter(fact -> fact.factType() == ObservationFactType.XINSHOU_RECOVERY_STATUS)
                .findFirst()
                .orElseThrow();
        assertEquals(anchor.observedAtMs(), recovery.observedAtMs());
    }

    private static BufferedImage frameWithRecovery(String templateName) throws IOException {
        BufferedImage frame = blankFrame();
        BufferedImage template = ImageIO.read(
                Path.of("images", "template", "xinshou", templateName).toFile());
        Graphics2D graphics = frame.createGraphics();
        graphics.drawImage(template, 569, 365, null);
        graphics.dispose();
        template.flush();
        return frame;
    }

    private static BufferedImage blankFrame() {
        return new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        assertTrue(start >= 0, "missing source-contract start marker: " + startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(end > start, "missing source-contract end marker: " + endMarker);
        return source.substring(start, end);
    }

    private static final class Fixture {
        private final WindowObservationSampler sampler;

        private Fixture(String taskRunId, BufferedImage frame) {
            this(taskRunId, new MutableFrameTracker(frame));
        }

        private Fixture(String taskRunId, MutableFrameTracker tracker) {
            WindowRuntimeContext context = new WindowRuntimeContext("window-1", new GameContext());
            context.setNativeBinding(new WindowNativeBinding(
                    "12345", "game", "class", 77L, 0, 0, 1024, 768));
            WindowTaskContextHolder contextHolder =
                    new WindowTaskContextHolder(new WindowIsolationProperties());
            CoordinateHelper coordinateHelper = new CoordinateHelper(null, null) {
                @Override
                public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
                    return new int[]{offsetX, offsetY, offsetX + width, offsetY + height};
                }
            };
            sampler = new WindowObservationSampler(
                    context,
                    contextHolder,
                    tracker,
                    coordinateHelper,
                    new DialogService(tracker, coordinateHelper),
                    new InputSequences(null),
                    taskRunId,
                    false,
                    new LocalCombatSignalMechanics(
                            stage -> null,
                            path -> null,
                            (source, template, threshold) -> false));
        }
    }

    private static final class MutableFrameTracker extends GameClientTracker {
        private BufferedImage frame;

        private MutableFrameTracker(BufferedImage frame) {
            super(null, null, null, null, null, null, null, null, null, null, null);
            this.frame = frame;
        }

        private void setFrame(BufferedImage next) {
            frame.flush();
            frame = next;
        }

        @Override
        public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
            BufferedImage copy = new BufferedImage(
                    frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = copy.createGraphics();
            graphics.drawImage(frame, 0, 0, null);
            graphics.dispose();
            return copy;
        }
    }
}
