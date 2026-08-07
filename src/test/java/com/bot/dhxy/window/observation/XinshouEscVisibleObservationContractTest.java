package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFact;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationFactType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationInterest;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
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
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class XinshouEscVisibleObservationContractTest {

    private static final ObservationInterest ANCHOR_INTEREST = new ObservationInterest(
            XinshouAnchorLocalMechanics.INTEREST_KEY, 1L, null);
    private static final List<ObservationFactType> VERSIONED_FACT_TYPES = List.of(
            ObservationFactType.XINSHOU_ANCHOR,
            ObservationFactType.XINSHOU_ESC_VISIBLE,
            ObservationFactType.XINSHOU_SKIP_VISIBLE,
            ObservationFactType.XINSHOU_ESC_BOT,
            ObservationFactType.XINSHOU_ADOPTION,
            ObservationFactType.XINSHOU_RECOVERY_STATUS);

    @Test
    void escVisibilityUsesAcceptedSequenceDedupAndNeverExecutesInput() throws IOException {
        Fixture fixture = new Fixture("run-esc-visible");
        fixture.tracker.setFrame(frameWithEsc());

        WindowObservationSampler.SampleBatch first = fixture.sampler.collect(List.of(ANCHOR_INTEREST), 1L);
        ObservationFact firstPresent = fact(first, "present");
        assertNotNull(firstPresent, "a fresh ESC hit must report present");
        assertEquals(VERSIONED_FACT_TYPES, versionedFactTypes(first),
                "the first frame must publish all six versioned facts");

        fixture.sampler.acknowledgeDeliveredFacts(0L, first.facts());
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch retry = fixture.sampler.collect(List.of(ANCHOR_INTEREST), 2L);
        assertNotNull(fact(retry, "present"), "a stale/NACK sequence must leave present retryable");
        assertEquals(VERSIONED_FACT_TYPES, versionedFactTypes(retry),
                "a stale/NACK sequence must leave every versioned fact retryable");

        fixture.sampler.acknowledgeDeliveredFacts(2L, retry.facts());
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch acknowledged =
                fixture.sampler.collect(List.of(ANCHOR_INTEREST), 3L);
        assertNull(escFact(acknowledged), "an ACKed unchanged frame must be deduplicated");
        assertEquals(List.of(), versionedFactTypes(acknowledged),
                "an exact ACK must deduplicate all six unchanged facts");

        fixture.tracker.setFrame(blankFrame());
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch absent = fixture.sampler.collect(List.of(ANCHOR_INTEREST), 4L);
        ObservationFact absentFact = fact(absent, "absent");
        assertNotNull(absentFact, "the same shared frame must report an explicit absence");
        ObservationFact anchorFact = absent.facts().stream()
                .filter(candidate -> candidate.factType() == ObservationFactType.XINSHOU_ANCHOR)
                .findFirst()
                .orElseThrow();
        assertEquals(anchorFact.observedAtMs(), absentFact.observedAtMs(),
                "anchor and ESC absence must belong to the same observation frame");

        fixture.sampler.acknowledgeDeliveredFacts(4L, absent.facts());
        awaitInterestPeriod();
        assertNull(escFact(fixture.sampler.collect(List.of(ANCHOR_INTEREST), 5L)),
                "an ACKed unchanged absence must be deduplicated");

        fixture.sampler.reset();
        WindowObservationSampler.SampleBatch afterReset =
                fixture.sampler.collect(List.of(ANCHOR_INTEREST), 6L);
        assertNotNull(fact(afterReset, "absent"), "reset must start a fresh ESC visibility generation");
        assertEquals(VERSIONED_FACT_TYPES, versionedFactTypes(afterReset),
                "reset must start a fresh generation for all six facts");
        assertEquals(0, fixture.inputSequences.inputCalls.get(),
                "ESC observation is evidence-only and must never submit physical input");
    }

    private static List<ObservationFactType> versionedFactTypes(
            WindowObservationSampler.SampleBatch batch) {
        return batch.facts().stream()
                .map(ObservationFact::factType)
                .filter(VERSIONED_FACT_TYPES::contains)
                .toList();
    }

    private static ObservationFact fact(WindowObservationSampler.SampleBatch batch, String value) {
        ObservationFact fact = escFact(batch);
        return fact != null && value.equals(fact.value()) ? fact : null;
    }

    private static ObservationFact escFact(WindowObservationSampler.SampleBatch batch) {
        return batch.facts().stream()
                .filter(candidate -> candidate.factType() == ObservationFactType.XINSHOU_ESC_VISIBLE)
                .findFirst()
                .orElse(null);
    }

    private static BufferedImage frameWithEsc() throws IOException {
        BufferedImage frame = blankFrame();
        BufferedImage template = ImageIO.read(Path.of("images/template/xinshou/ESC.png").toFile());
        Graphics2D graphics = frame.createGraphics();
        graphics.drawImage(template, 870, 57, null);
        graphics.dispose();
        template.flush();
        return frame;
    }

    private static BufferedImage blankFrame() {
        return new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
    }

    private static void awaitInterestPeriod() {
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() - startedAt < 2L) {
            Thread.onSpinWait();
        }
    }

    private static final class Fixture {
        private final MutableFrameTracker tracker = new MutableFrameTracker();
        private final RecordingInputSequences inputSequences = new RecordingInputSequences();
        private final WindowObservationSampler sampler;

        private Fixture(String taskRunId) {
            WindowRuntimeContext context = new WindowRuntimeContext("window-esc", new GameContext());
            context.setNativeBinding(new WindowNativeBinding(
                    "esc-player", "game", "class", 91L, 0, 0, 1024, 768));
            WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
            CoordinateHelper coordinateHelper = new CoordinateHelper(null, null) {
                @Override
                public int[] getScaledRect(int offsetX, int offsetY, int width, int height) {
                    return new int[]{offsetX, offsetY, offsetX + width, offsetY + height};
                }
            };
            DialogService dialogService = new DialogService(tracker, coordinateHelper);
            LocalCombatSignalMechanics combat = new LocalCombatSignalMechanics(
                    stage -> null,
                    path -> null,
                    (source, template, threshold) -> false);
            sampler = new WindowObservationSampler(
                    context,
                    contextHolder,
                    tracker,
                    coordinateHelper,
                    dialogService,
                    inputSequences,
                    taskRunId,
                    false,
                    combat);
        }
    }

    private static final class MutableFrameTracker extends GameClientTracker {
        private BufferedImage frame = blankFrame();

        private MutableFrameTracker() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        private void setFrame(BufferedImage next) {
            frame.flush();
            frame = next;
        }

        @Override
        public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
            BufferedImage copy = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = copy.createGraphics();
            graphics.drawImage(frame, 0, 0, null);
            graphics.dispose();
            return copy;
        }
    }

    private static final class RecordingInputSequences extends InputSequences {
        private final AtomicInteger inputCalls = new AtomicInteger();

        private RecordingInputSequences() {
            super(null);
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            inputCalls.incrementAndGet();
            return true;
        }

        @Override
        public boolean moveAndClickLeft(String description, int x, int y, int settleMs, int delayMs) {
            inputCalls.incrementAndGet();
            return true;
        }
    }
}
