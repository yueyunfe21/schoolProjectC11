package com.bot.dhxy.window.observation;

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
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XinshouObservationChangeUploadContractTest {

    private static final ObservationInterest TRACKER_INTEREST = new ObservationInterest(
            WindowObservationSampler.XINSHOU_TRACKER_INTEREST,
            1L,
            null,
            0,
            100,
            280,
            604);
    private static final ObservationInterest DIALOG_INTEREST = new ObservationInterest(
            WindowObservationSampler.XINSHOU_DIALOG_INTEREST,
            1L,
            null,
            200,
            250,
            640,
            300);
    private static final ObservationInterest ANCHOR_INTEREST =
            new ObservationInterest("xinshou-anchor", 1L, null);

    @Test
    void trackerUploadsFirstAndChangedFramesButNotAcknowledgedDuplicates() {
        Fixture fixture = new Fixture("run-tracker");
        fixture.tracker.setFrame(frameWithTrackerColor(0x00224466));

        WindowObservationSampler.SampleBatch first = fixture.sampler.collect(List.of(TRACKER_INTEREST), 1L);
        assertEquals(1, first.rois().size());
        assertEquals(WindowObservationSampler.XINSHOU_TRACKER_INTEREST, first.rois().getFirst().roiKey());
        fixture.sampler.acknowledgeDeliveredRois(1L, first.rois());

        awaitInterestPeriod();
        assertTrue(fixture.sampler.collect(List.of(TRACKER_INTEREST), 2L).rois().isEmpty());

        fixture.tracker.setFrame(frameWithTrackerColor(0x00662244));
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch changed = fixture.sampler.collect(List.of(TRACKER_INTEREST), 3L);
        assertEquals(1, changed.rois().size());
    }

    @Test
    void suspendInvalidationForcesTheAcknowledgedUnchangedTrackerToUploadAfterResume() {
        Fixture fixture = new Fixture("run-tracker-suspend");
        fixture.tracker.setFrame(frameWithTrackerColor(0x00224477));

        WindowObservationSampler.SampleBatch first =
                fixture.sampler.collect(List.of(TRACKER_INTEREST), 1L);
        assertEquals(1, first.rois().size());
        fixture.sampler.acknowledgeDeliveredRois(1L, first.rois());

        awaitInterestPeriod();
        assertTrue(fixture.sampler.collect(
                List.of(TRACKER_INTEREST), 2L).rois().isEmpty());

        fixture.sampler.invalidateTerminalFrameForSuspend();
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch resumed =
                fixture.sampler.collect(List.of(TRACKER_INTEREST), 3L);
        assertEquals(1, resumed.rois().size());
        assertEquals(
                WindowObservationSampler.XINSHOU_TRACKER_INTEREST,
                resumed.rois().getFirst().roiKey());
    }

    @Test
    void absentDialogFactSharesObserverBatchWithTrackerRoi() {
        Fixture fixture = new Fixture("run-dialog-absent");
        fixture.tracker.setFrame(blankFrame());

        WindowObservationSampler.SampleBatch batch =
                fixture.sampler.collect(List.of(TRACKER_INTEREST, DIALOG_INTEREST), 1L);

        assertTrue(hasFact(batch, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "absent"));
        assertTrue(batch.rois().stream().anyMatch(roi ->
                WindowObservationSampler.XINSHOU_TRACKER_INTEREST.equals(roi.roiKey())));
        assertFalse(batch.rois().stream().anyMatch(roi ->
                WindowObservationSampler.XINSHOU_DIALOG_INTEREST.equals(roi.roiKey())));
    }

    @Test
    void unavailableSharedFrameProducesNeitherDialogPresenceFactNorRoi() {
        Fixture fixture = new Fixture("run-dialog-unavailable");
        fixture.tracker.setCaptureAvailable(false);

        WindowObservationSampler.SampleBatch batch =
                fixture.sampler.collect(List.of(DIALOG_INTEREST), 1L);

        assertFalse(batch.facts().stream().anyMatch(fact ->
                fact.factType() == ObservationFactType.XINSHOU_DIALOG_PRESENCE));
        assertTrue(batch.rois().isEmpty());
    }

    @Test
    void dialogPresenceAndRoiAreReliableChangeOnlyAndReopenAfterAbsence() {
        Fixture fixture = new Fixture("run-dialog");
        fixture.tracker.setFrame(frameWithDialog(0x00000000));

        WindowObservationSampler.SampleBatch first = fixture.sampler.collect(List.of(DIALOG_INTEREST), 1L);
        assertTrue(hasFact(first, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "present"));
        assertEquals(1, first.rois().size());
        assertEquals(WindowObservationSampler.XINSHOU_DIALOG_INTEREST, first.rois().getFirst().roiKey());

        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch transportRetry =
                fixture.sampler.collect(List.of(DIALOG_INTEREST), 2L);
        assertTrue(hasFact(transportRetry, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "present"),
                "an unacknowledged presence fact must be retried");
        assertEquals(1, transportRetry.rois().size(),
                "an unacknowledged dialog ROI must be retried");
        fixture.sampler.acknowledgeDeliveredFacts(2L, transportRetry.facts());
        fixture.sampler.acknowledgeDeliveredRois(2L, transportRetry.rois());

        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch duplicate =
                fixture.sampler.collect(List.of(DIALOG_INTEREST), 3L);
        assertFalse(hasFact(duplicate, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "present"),
                "an ACKed unchanged presence fact must not be resent");
        assertTrue(duplicate.rois().isEmpty(),
                "an ACKed unchanged dialog ROI must not be resent");

        fixture.tracker.setFrame(frameWithDialog(0x00AA2200));
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch changed =
                fixture.sampler.collect(List.of(DIALOG_INTEREST), 4L);
        assertFalse(hasFact(changed, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "present"),
                "dialog content changes must not fabricate a presence state change");
        assertEquals(1, changed.rois().size(),
                "changed dialog content must still upload a fresh ROI");
        fixture.sampler.acknowledgeDeliveredRois(4L, changed.rois());

        fixture.tracker.setFrame(blankFrame());
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch absent =
                fixture.sampler.collect(List.of(DIALOG_INTEREST), 5L);
        assertTrue(hasFact(absent, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "absent"));
        assertTrue(absent.rois().isEmpty(), "dialog absence is a fact, not an image upload");
        fixture.sampler.acknowledgeDeliveredFacts(5L, absent.facts());

        fixture.tracker.setFrame(frameWithDialog(0x00AA2200));
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch reopened =
                fixture.sampler.collect(List.of(DIALOG_INTEREST), 6L);
        assertTrue(hasFact(reopened, ObservationFactType.XINSHOU_DIALOG_PRESENCE, "present"));
        assertEquals(1, reopened.rois().size(),
                "an identical dialog reopened after absence is a new local event");
    }

    @Test
    void unacknowledgedFrameIsRetriedAndResetOrTaskRunSwitchClearsDedupState() {
        MutableFrameTracker tracker = new MutableFrameTracker();
        tracker.setFrame(frameWithTrackerColor(0x00112233));
        Fixture firstRun = new Fixture("run-one", tracker);

        WindowObservationSampler.SampleBatch unacknowledged =
                firstRun.sampler.collect(List.of(TRACKER_INTEREST), 1L);
        assertEquals(1, unacknowledged.rois().size());
        firstRun.sampler.acknowledgeDeliveredRois(99L, unacknowledged.rois());

        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch retry =
                firstRun.sampler.collect(List.of(TRACKER_INTEREST), 2L);
        assertEquals(1, retry.rois().size(),
                "transport failure or a mismatched observer sequence must not burn the changed frame");
        firstRun.sampler.acknowledgeDeliveredRois(2L, retry.rois());

        awaitInterestPeriod();
        assertTrue(firstRun.sampler.collect(List.of(TRACKER_INTEREST), 3L).rois().isEmpty());

        firstRun.sampler.reset();
        assertEquals(1, firstRun.sampler.collect(List.of(TRACKER_INTEREST), 4L).rois().size(),
                "reset starts a fresh content generation");

        Fixture secondRun = new Fixture("run-two", tracker);
        assertEquals(1, secondRun.sampler.collect(List.of(TRACKER_INTEREST), 1L).rois().size(),
                "a new taskRun owns a new sampler and cannot inherit the previous run's hash");
    }

    @Test
    void unchangedSceneIsReprovedAfterTenSecondsAndTransportFailureDoesNotBurnIt()
            throws Exception {
        Fixture fixture = new Fixture("run-no-progress-refresh");
        fixture.tracker.setFrame(blankFrame());

        WindowObservationSampler.SampleBatch first = fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 1L);
        fixture.sampler.acknowledgeDeliveredFacts(1L, first.facts());
        fixture.sampler.acknowledgeDeliveredRois(1L, first.rois());

        awaitInterestPeriod();
        assertTrue(fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 2L).rois().isEmpty(),
                "an ACKed unchanged scene stays transport-quiet before the no-progress bound");

        ageXinshouProgress(fixture.sampler);
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch timedOut = fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 3L);
        assertTrue(timedOut.rois().stream().anyMatch(roi ->
                        WindowObservationSampler.XINSHOU_TRACKER_INTEREST.equals(roi.roiKey())),
                "ten seconds without local progress must reprove the current Tracker scene");
        assertFalse(timedOut.rois().stream().anyMatch(roi ->
                        WindowObservationSampler.XINSHOU_DIALOG_INTEREST.equals(roi.roiKey())),
                "an absent Dialog is represented by the retained absence fact, not a fabricated image");
        assertTrue(hasFact(
                        timedOut,
                        ObservationFactType.XINSHOU_NO_PROGRESS_REFRESH,
                        "no-progress"),
                "Cloud must distinguish a bounded reproof from an arbitrary duplicate sequence");

        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch transportRetry = fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 4L);
        assertTrue(transportRetry.rois().stream().anyMatch(roi ->
                        WindowObservationSampler.XINSHOU_TRACKER_INTEREST.equals(roi.roiKey())),
                "a failed timeout upload must retry instead of waiting another ten seconds");

        fixture.sampler.acknowledgeDeliveredRois(4L, transportRetry.rois());
        awaitInterestPeriod();
        assertTrue(fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 5L).rois().isEmpty(),
                "the exact accepted refresh starts a new bounded wait");
    }

    @Test
    void combatExitReprovesTheAcknowledgedSceneWithoutWaitingTenSeconds() throws Exception {
        Fixture fixture = new Fixture("run-combat-exit-refresh");
        fixture.tracker.setFrame(frameWithEsc());

        WindowObservationSampler.SampleBatch first = fixture.sampler.collect(
                List.of(ANCHOR_INTEREST, TRACKER_INTEREST, DIALOG_INTEREST), 1L);
        fixture.sampler.acknowledgeDeliveredFacts(1L, first.facts());
        fixture.sampler.acknowledgeDeliveredRois(1L, first.rois());

        Field refreshPending = WindowObservationSampler.class
                .getDeclaredField("xinshouRefreshPending");
        refreshPending.setAccessible(true);
        refreshPending.setBoolean(fixture.sampler, true);

        /*
         * Real post-combat ordering: ESC disappears before the unchanged Tracker ROI is sampled.
         * That fact change must not cancel the already-armed scene reproof.
         */
        fixture.tracker.setFrame(blankFrame());
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch postCombat = fixture.sampler.collect(
                List.of(ANCHOR_INTEREST, TRACKER_INTEREST, DIALOG_INTEREST), 2L);

        assertTrue(postCombat.rois().stream().anyMatch(roi ->
                        WindowObservationSampler.XINSHOU_TRACKER_INTEREST.equals(roi.roiKey())),
                "combat exit must immediately reprove the unchanged Tracker scene");
        assertTrue(hasFact(
                        postCombat,
                        ObservationFactType.XINSHOU_NO_PROGRESS_REFRESH,
                        "no-progress"),
                "the immediate reproof must be distinguishable from an arbitrary duplicate");
    }

    @Test
    void noProgressRefreshCarriesDialogAndTrackerFromTheSameObservationCycle()
            throws Exception {
        Fixture fixture = new Fixture("run-no-progress-dialog");
        fixture.tracker.setFrame(frameWithDialog(0x00112233));

        WindowObservationSampler.SampleBatch first = fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 1L);
        fixture.sampler.acknowledgeDeliveredFacts(1L, first.facts());
        fixture.sampler.acknowledgeDeliveredRois(1L, first.rois());

        ageXinshouProgress(fixture.sampler);
        awaitInterestPeriod();
        WindowObservationSampler.SampleBatch refresh = fixture.sampler.collect(
                List.of(TRACKER_INTEREST, DIALOG_INTEREST), 2L);

        assertTrue(refresh.rois().stream().anyMatch(roi ->
                WindowObservationSampler.XINSHOU_DIALOG_INTEREST.equals(roi.roiKey())));
        assertTrue(refresh.rois().stream().anyMatch(roi ->
                WindowObservationSampler.XINSHOU_TRACKER_INTEREST.equals(roi.roiKey())));
    }

    @Test
    void titleHitProducesObservationOnlyAndCannotInvokeLocalBusinessMacro() throws IOException {
        Fixture fixture = new Fixture("run-title");
        fixture.tracker.setFrame(frameWithTitle("xunren.png"));
        ObservationInterest anchor = new ObservationInterest(
                XinshouAnchorLocalMechanics.INTEREST_KEY, 1L, null);

        WindowObservationSampler.SampleBatch batch = fixture.sampler.collect(List.of(anchor), 1L);

        assertTrue(batch.facts().stream().anyMatch(fact ->
                fact.factType() == ObservationFactType.XINSHOU_ANCHOR
                        && "xunren.png".equals(fact.value())));
        assertEquals(0, fixture.inputSequences.inputCalls.get(),
                "Runner title sensing must not submit keyboard or mouse input");
    }

    private static BufferedImage frameWithTrackerColor(int rgb) {
        BufferedImage frame = blankFrame();
        Graphics2D graphics = frame.createGraphics();
        graphics.setColor(new java.awt.Color(rgb));
        graphics.fillRect(0, 100, 280, 604);
        graphics.dispose();
        return frame;
    }

    private static BufferedImage frameWithDialog(int markerRgb) {
        BufferedImage frame = blankFrame();
        Graphics2D graphics = frame.createGraphics();
        graphics.setColor(new java.awt.Color(80, 80, 80));
        graphics.fillRect(250, 280, 540, 1);
        graphics.fillRect(250, 400, 540, 1);
        graphics.setColor(new java.awt.Color(markerRgb));
        graphics.fillRect(480, 330, 20, 20);
        graphics.dispose();
        return frame;
    }

    private static BufferedImage frameWithTitle(String templateName) throws IOException {
        BufferedImage frame = blankFrame();
        BufferedImage template = ImageIO.read(Path.of("images/template/xinshou", templateName).toFile());
        Graphics2D graphics = frame.createGraphics();
        graphics.drawImage(template, 6, 196, null);
        graphics.dispose();
        template.flush();
        return frame;
    }

    private static BufferedImage blankFrame() {
        return new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
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

    private static boolean hasFact(WindowObservationSampler.SampleBatch batch,
                                   ObservationFactType type,
                                   String value) {
        return batch.facts().stream().anyMatch(fact ->
                fact.factType() == type && value.equals(fact.value()));
    }

    private static void awaitInterestPeriod() {
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() - startedAt < 2L) {
            Thread.onSpinWait();
        }
    }

    private static void ageXinshouProgress(WindowObservationSampler sampler) throws Exception {
        Field progress = WindowObservationSampler.class
                .getDeclaredField("lastXinshouEffectiveProgressAtMs");
        Field acknowledged = WindowObservationSampler.class
                .getDeclaredField("lastXinshouRefreshAcknowledgedAtMs");
        progress.setAccessible(true);
        acknowledged.setAccessible(true);
        progress.setLong(
                sampler,
                System.currentTimeMillis()
                        - WindowObservationSampler.XINSHOU_NO_PROGRESS_REFRESH_MS - 1L);
        acknowledged.setLong(sampler, 0L);
    }

    private static final class Fixture {
        private final MutableFrameTracker tracker;
        private final RecordingInputSequences inputSequences = new RecordingInputSequences();
        private final WindowObservationSampler sampler;

        private Fixture(String taskRunId) {
            this(taskRunId, new MutableFrameTracker());
        }

        private Fixture(String taskRunId, MutableFrameTracker tracker) {
            this.tracker = tracker;
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
        private boolean captureAvailable = true;

        private MutableFrameTracker() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        private void setFrame(BufferedImage next) {
            frame.flush();
            frame = next;
        }

        private void setCaptureAvailable(boolean captureAvailable) {
            this.captureAvailable = captureAvailable;
        }

        @Override
        public BufferedImage captureToMemory(String elementName, int x1, int y1, int x2, int y2) {
            if (!captureAvailable) {
                return null;
            }
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
