package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEvent;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationKeyEventType;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.observation.ObservationRequest;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowPathingIntent;
import com.bot.dhxy.window.model.WindowPathingIntentType;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G005 WP3: the sampler-side contract of the 天庭 option probe.
 *
 * <p>The matcher being right is not enough — this probe is one of the few places the client sends
 * input on its own initiative, and the task deliberately keeps its interest installed across the
 * click. So the rules that matter live here: who may click, how often, and what is reported.</p>
 */
class TiantingDialogProbeContractTest {

    private static final String WINDOW = "window-tianting-probe";
    private static final String HWND = "0x2001";
    private static final String RUN_ID = "run-tianting-1";

    @Test
    void oneDialogIsAnsweredOnceEvenThoughTheInterestStaysArmed() {
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);

        fixture.collectCycles(3);

        assertEquals(1, fixture.inputSequences.clicks.size(),
                "a dialog that is still on screen while it closes must not be clicked again — the "
                        + "extra clicks would land on the combat screen that replaced it");
    }

    @Test
    void aNewDialogAfterTheFirstOneClosesIsAnsweredAgain() {
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);
        fixture.collectCycles(2);

        fixture.showNothing();      // the dialog closed
        fixture.collectCycles(1);
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);   // the next sub-quest offers it again
        fixture.collectCycles(1);

        assertEquals(2, fixture.inputSequences.clicks.size(),
                "the latch must re-arm once the dialog is gone, or the second sub-quest never gets answered");
    }

    @Test
    void aClickIsReportedAsARetainedKeyEventCarryingWhatWasAnswered() {
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        ObservationKeyEvent event = events.stream()
                .filter(candidate -> candidate.eventType() == ObservationKeyEventType.TIANTING_DIALOG_CLICKED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the click must be reported as a retained key "
                        + "event; a plain fact is dropped when the upload fails, and the click has "
                        + "already physically happened"));
        assertTrue(event.eventId().endsWith(TiantingDialogLocalMechanics.ACTION_ENTER_BATTLE_KAIDA),
                "event id uses the stable action key: " + event.eventId());
        assertTrue(event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_ENTER_BATTLE_KAIDA + "|"),
                "report carries the stable action key: " + event.detail());
        assertTrue(event.detail().contains("executed=true"), "report states the click executed: " + event.detail());
        /*
         * Reversed deliberately. This assertion used to require the task/run identity to be present, which
         * is exactly what makes the event unsendable: the wire validator requires those fields to be set
         * EXACTLY for expected-combat and replay edges, and it runs before the request leaves this side. A
         * dialog-click edge carrying them is rejected as a contract violation, and since key events are
         * retained until acked, the rejection repeats forever — the window's entire observation plane stops
         * uploading and never recovers. The test below sends this event through the real validator so the
         * next person cannot re-add them.
         */
        assertNull(event.taskCode(), "a dialog-click edge must not carry replay/combat identity");
        assertNull(event.businessTaskRunId(), "a dialog-click edge must not carry replay/combat identity");
    }

    @Test
    void failedPhysicalClickStillReportsStableActionWithExecutedFalse() {
        Fixture fixture = new Fixture();
        fixture.armRecoveryInterest();
        fixture.inputSequences.clickResult = false;
        fixture.showOption(TiantingDialogLocalMechanics.DUOXIE);

        ObservationKeyEvent event = fixture.collectCycles(1).stream()
                .filter(candidate -> candidate.eventType() == ObservationKeyEventType.TIANTING_DIALOG_CLICKED)
                .findFirst()
                .orElseThrow();

        assertTrue(event.eventId().endsWith(TiantingDialogLocalMechanics.ACTION_DUOXIE));
        assertTrue(event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_DUOXIE + "|"));
        assertTrue(event.detail().contains("executed=false"));
    }

    @Test
    void theReportedClickPassesTheRealWireValidator() {
        /*
         * The layer this task kept breaking is not the decision layer, it is the seam. Every previous
         * version of this probe passed its own unit tests and could not put a single byte on the wire.
         * So the event goes through the production validator here, in the same shape the sampler builds.
         */
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);
        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        ObservationKeyEvent event = events.stream()
                .filter(candidate -> candidate.eventType() == ObservationKeyEventType.TIANTING_DIALOG_CLICKED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("probe produced no click edge to validate"));

        ObservationRequest request = new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                "tenant-1",
                "device-1",
                fixture.context.getWindowId(),
                "0x1234",
                TaskType.TIANTING.getCode(),
                "run-1",
                0L,
                1_000L,
                0L,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(event),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        // Throws IllegalArgumentException on any contract breach — the same call the client makes before
        // it sends, which is why a breach here is a silent, permanent upload stall rather than a red test.
        assertDoesNotThrow(() -> ObservationProtocolValidator.requireValid(request));
    }

    @Test
    void answeringDuoxieOpensTheFengyaoWindowLocallyWithoutWaitingForTheCloud() {
        /*
         * 使用封妖符 is on screen for about a second after 多谢. Arming a probe for it from the cloud cannot
         * work: the answer to 多谢 needs a sampling cycle and two round trips to get there, so the interest
         * would arrive after the option had gone — and the 封妖符 branch would simply never start, with
         * nothing in the log to say so. The window is therefore opened by the click that causes it.
         */
        Fixture fixture = new Fixture();
        fixture.armInterest();      // only the four resident combat options are armed
        fixture.showOption(TiantingDialogLocalMechanics.DUOXIE);
        fixture.collectCycles(1);

        assertTrue(fixture.context.isTiantingFengyaoPending(),
                "answering 多谢 must retain the local 封妖符 follow-up without a wall-clock deadline");

        // 使用封妖符 now appears. Nothing re-armed the interest, and nothing had to.
        fixture.showOption(TiantingDialogLocalMechanics.FENGYAO);
        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertTrue(events.stream().anyMatch(event ->
                        event.eventType() == ObservationKeyEventType.TIANTING_DIALOG_CLICKED
                                && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_FENGYAO + "|")),
                "使用封妖符 must be answered inside the local window, with only the combat set armed");
        assertTrue(!fixture.context.isTiantingFengyaoPending(),
                "a successful 使用封妖符 click must consume the local follow-up immediately");
    }

    @Test
    void clearingTheOwningDialogInterestClearsAnUnconsumedFengyaoFollowUp() {
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.DUOXIE);
        fixture.collectCycles(1);

        assertTrue(fixture.context.isTiantingFengyaoPending());

        fixture.context.clearDialogInterest("test phase ended");

        assertTrue(!fixture.context.isTiantingFengyaoPending(),
                "the follow-up must not leak past the dialog interest that owns this phase");
    }

    @Test
    void fengyaoFollowUpSurvivesTheFormerTwoPointFiveSecondDeadline() throws InterruptedException {
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.DUOXIE);
        fixture.collectCycles(1);

        Thread.sleep(2_600L);
        fixture.showOption(TiantingDialogLocalMechanics.FENGYAO);
        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertTrue(events.stream().anyMatch(event -> event.detail() != null
                        && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_FENGYAO + "|")),
                "sampling later than the deleted 2.5s boundary must still consume 使用封妖符");
    }

    @Test
    void fengyaoIsNotAnsweredOutsideItsWindow() {
        // Outside the window it must stay unmatched: opening the coordinate dialog in a leg that has
        // nothing to do with it is exactly why this option is not one of the resident four.
        Fixture fixture = new Fixture();
        fixture.armInterest();
        fixture.showOption(TiantingDialogLocalMechanics.FENGYAO);

        List<ObservationKeyEvent> events = fixture.collectCycles(2);

        assertTrue(events.stream().noneMatch(event -> event.detail() != null
                        && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_FENGYAO + "|")),
                "使用封妖符 must not be answered when no 多谢 opened its window");
    }

    @Test
    void noMovementRecoveryCarriesEveryTiantingBusinessOptionAcrossTheRealSamplerSeam() {
        for (String templatePath : List.of(
                TiantingDialogLocalMechanics.YINYAO,
                TiantingDialogLocalMechanics.KAIDA,
                TiantingDialogLocalMechanics.DUOXIE,
                TiantingDialogLocalMechanics.ZHUOYUE,
                TiantingDialogLocalMechanics.YAOWANG,
                TiantingDialogLocalMechanics.FENGYAO,
                TiantingDialogLocalMechanics.ACCEPT)) {
            Fixture fixture = new Fixture();
            fixture.armRecoveryInterest();
            fixture.showOption(templatePath);

            List<ObservationKeyEvent> events = fixture.collectCycles(1);

            assertEquals(1, fixture.inputSequences.clicks.size(),
                    templatePath + " must produce one physical click through the production sampler");
            String actionKey = TiantingDialogLocalMechanics.actionKeyForTemplate(templatePath).orElseThrow();
            assertTrue(events.stream().anyMatch(event ->
                            event.eventType() == ObservationKeyEventType.TIANTING_DIALOG_CLICKED
                                    && event.eventId().endsWith(actionKey)
                                    && event.detail().startsWith(actionKey + "|")
                                    && event.detail().contains("executed=true")),
                    templatePath + " must cross the retained-event seam with its real identity");
        }
    }

    @Test
    void noMovementRecoveryAlwaysMatchesYinyaoWithoutTrackerOcrClassification() {
        Fixture fixture = new Fixture();
        fixture.armRecoveryInterest();
        fixture.showOption(TiantingDialogLocalMechanics.YINYAO);

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertEquals(1, fixture.inputSequences.clicks.size(),
                "天庭 no-movement fallback must always include yinyao.png");
        assertTrue(events.stream().anyMatch(event -> event.detail() != null
                        && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_YINYAO + "|")
                        && event.detail().contains("executed=true")),
                "fallback must publish the stable tianting.yinyao action without an OCR gate");
    }

    @Test
    void recoveryAfterAcceptedCycleYinyaoDoesNotClickYinyaoAgain() {
        Fixture fixture = new Fixture();
        fixture.armDialogInterest(DialogOperation.TIANTING_RECOVERY_OPTION_NO_YINYAO,
                "test-recovery-after-yinyao");
        fixture.showOption(TiantingDialogLocalMechanics.YINYAO);

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertTrue(fixture.inputSequences.clicks.isEmpty());
        assertTrue(events.stream().noneMatch(event -> event.detail() != null
                        && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_YINYAO + "|")),
                "the no-yinyao recovery operation must neither click nor publish 引妖香");
    }

    @Test
    void capturedPostCombatDialogMatchesYinyaoAndWritesMarkedReplay() throws IOException {
        Path evidence = Path.of("images/test-cases/tianting/g005-yinyao-fallback");
        Path rawPath = evidence.resolve("20260814_170710_yinyao-dialog-raw.png");
        BufferedImage raw = ImageIO.read(rawPath.toFile());
        assertTrue(raw != null, "the exact captured 640x300 dialog ROI must be readable");

        TiantingDialogLocalMechanics.OptionHit hit =
                TiantingDialogLocalMechanics.matchRecoveryOption(raw).orElseThrow();
        assertEquals(TiantingDialogLocalMechanics.ACTION_YINYAO, hit.actionKey());

        BufferedImage template = ImageIO.read(Path.of(TiantingDialogLocalMechanics.YINYAO).toFile());
        assertTrue(template != null, "yinyao template must be readable");
        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = marked.createGraphics();
        graphics.drawImage(raw, 0, 0, null);
        graphics.setColor(Color.RED);
        graphics.drawRect(hit.roiOffsetX() - template.getWidth() / 2,
                hit.roiOffsetY() - template.getHeight() / 2,
                template.getWidth(), template.getHeight());
        graphics.drawLine(hit.roiOffsetX() - 8, hit.roiOffsetY(), hit.roiOffsetX() + 8, hit.roiOffsetY());
        graphics.drawLine(hit.roiOffsetX(), hit.roiOffsetY() - 8, hit.roiOffsetX(), hit.roiOffsetY() + 8);
        graphics.dispose();
        ImageIO.write(marked, "png", evidence.resolve("20260814_170710_yinyao-dialog-marked.png").toFile());
        template.flush();
        raw.flush();
        marked.flush();
    }

    @Test
    void cancelOnlyOperationClicksQuxiaoAndReportsItsStableAction() {
        Fixture fixture = new Fixture();
        fixture.armCancelInterest();
        fixture.showOption(TiantingDialogLocalMechanics.CANCEL);

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertEquals(1, fixture.inputSequences.clicks.size());
        assertTrue(events.stream().anyMatch(event ->
                        event.eventType() == ObservationKeyEventType.TIANTING_DIALOG_CLICKED
                                && event.detail().startsWith(
                                TiantingDialogLocalMechanics.ACTION_CANCEL_TASK + "|")
                                && event.detail().contains("executed=true")),
                "cancel-only must cross the real sampler seam with tianting.cancelTask");
        assertTrue(events.stream().anyMatch(event -> event.detail() != null
                        && event.detail().contains("probeCorrelation=test-cancel")),
                "the click event must carry the exact Cloud probe correlation");
    }

    @Test
    void acceptAndRecoveryOperationsNeverTreatQuxiaoAsTheirOwnAction() {
        for (DialogOperation operation : List.of(
                DialogOperation.TIANTING_ACCEPT_TASK,
                DialogOperation.TIANTING_RECOVERY_OPTION)) {
            Fixture fixture = new Fixture();
            fixture.armDialogInterest(operation, "test-not-cancel");
            fixture.showOption(TiantingDialogLocalMechanics.CANCEL);

            List<ObservationKeyEvent> events = fixture.collectCycles(1);

            assertTrue(fixture.inputSequences.clicks.isEmpty(), operation + " must not click quxiao.png");
            assertTrue(events.stream().noneMatch(event -> event.detail() != null
                            && event.detail().startsWith(
                            TiantingDialogLocalMechanics.ACTION_CANCEL_TASK + "|")),
                    operation + " must not publish tianting.cancelTask");
        }
    }

    @Test
    void staleFengyaoFollowUpCannotOverrideCancelOnlyOperation() {
        Fixture fixture = new Fixture();
        fixture.context.markTiantingFengyaoPending("stale-duoxie");
        fixture.armCancelInterest();
        fixture.showOption(TiantingDialogLocalMechanics.FENGYAO);

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertTrue(fixture.inputSequences.clicks.isEmpty(),
                "cancel-only must not click a stale fengyao follow-up");
        assertTrue(events.stream().noneMatch(event -> event.detail() != null
                        && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_FENGYAO + "|")),
                "cancel-only must publish no fengyao action even when the old local latch remains set");
    }

    @Test
    void cancelTemplateReplayWritesMarkedClickEvidence() throws Exception {
        BufferedImage raw = Fixture.canvasWith(TiantingDialogLocalMechanics.CANCEL);
        BufferedImage roi = raw.getSubimage(
                TiantingDialogLocalMechanics.DIALOG_ROI_LEFT,
                TiantingDialogLocalMechanics.DIALOG_ROI_TOP,
                TiantingDialogLocalMechanics.DIALOG_ROI_WIDTH,
                TiantingDialogLocalMechanics.DIALOG_ROI_HEIGHT);
        TiantingDialogLocalMechanics.OptionHit hit =
                TiantingDialogLocalMechanics.matchCancelOption(roi).orElseThrow();
        BufferedImage template = ImageIO.read(new File(TiantingDialogLocalMechanics.CANCEL));
        Path evidence = Path.of("images/test-cases/tianting/cancel-option");
        Files.createDirectories(evidence);
        ImageIO.write(raw, "png", evidence.resolve("cancel-option-raw.png").toFile());

        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = marked.createGraphics();
        graphics.drawImage(raw, 0, 0, null);
        graphics.setColor(Color.RED);
        int clickX = TiantingDialogLocalMechanics.DIALOG_ROI_LEFT + hit.roiOffsetX();
        int clickY = TiantingDialogLocalMechanics.DIALOG_ROI_TOP + hit.roiOffsetY();
        graphics.drawRect(clickX - template.getWidth() / 2, clickY - template.getHeight() / 2,
                template.getWidth(), template.getHeight());
        graphics.drawLine(clickX - 8, clickY, clickX + 8, clickY);
        graphics.drawLine(clickX, clickY - 8, clickX, clickY + 8);
        graphics.dispose();
        ImageIO.write(marked, "png", evidence.resolve("cancel-option-marked.png").toFile());
        template.flush();
        roi.flush();
        raw.flush();
        marked.flush();
    }

    @Test
    void yinyaoInterestClicksOnlyTheOptionAndIgnoresThePreOptionTrackerPoint() {
        Fixture fixture = new Fixture();
        fixture.armYinyaoInterest(100L, "intent-yinyao-chain");
        fixture.showOption(TiantingDialogLocalMechanics.YINYAO);

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertEquals(1, fixture.inputSequences.clicks.size(),
                "the local probe must click only the 引妖香 option");
        assertTrue(fixture.inputSequences.submissions.isEmpty(),
                "the pre-option Tracker point must not be submitted as a follow-up input chain");
        assertFalse(fixture.inputSequences.clicks.get(0).endsWith("@321,222"),
                "the stale prepared Tracker coordinate must never be clicked");
        assertNull(fixture.context.getPathingSnapshot().getIntent(),
                "an option-only click must not register a Tracker pathing intent");
        assertTrue(events.stream().anyMatch(event -> event.detail() != null
                        && event.detail().startsWith(TiantingDialogLocalMechanics.ACTION_YINYAO + "|")
                        && event.detail().contains("executed=true")
                        && event.detail().contains("trackerChained=false")),
                "the retained event must state that only the option was clicked");
    }

    @Test
    void matchedYinyaoStopsProbingUntilTheTrackerTaskInstallsANewInterest() {
        Fixture fixture = new Fixture();
        fixture.armYinyaoInterest(100L, "intent-yinyao-first");
        fixture.showNothing();

        fixture.collectCycles(2);
        assertTrue(fixture.inputSequences.clicks.isEmpty(),
                "a miss must remain retryable for the current Tracker task");

        fixture.showOption(TiantingDialogLocalMechanics.YINYAO);
        fixture.collectCycles(1);
        assertEquals(1, fixture.inputSequences.clicks.size());

        fixture.showNothing();
        fixture.collectCycles(1);
        fixture.showOption(TiantingDialogLocalMechanics.YINYAO);
        fixture.collectCycles(2);
        assertEquals(1, fixture.inputSequences.clicks.size(),
                "a matched 引妖香 task must not resume matching when the old dialog pixels reappear");

        fixture.armYinyaoInterest(200L, "intent-yinyao-next-task");
        assertFalse(fixture.context.hasTiantingDialogOptionClaim(
                        "100|" + TiantingDialogLocalMechanics.ACTION_YINYAO),
                "replacing the Tracker task interest must clear the previous task's match claim");
        fixture.collectCycles(1);
        assertEquals(2, fixture.inputSequences.clicks.size(),
                "a new Tracker task interest must re-arm 引妖香 matching");
    }

    @Test
    void recoveryAllMissIsARetainedTerminalAndNeverClicks() {
        Fixture fixture = new Fixture();
        fixture.armRecoveryInterest();
        fixture.showNothing();

        List<ObservationKeyEvent> events = fixture.collectCycles(1);

        assertEquals(0, fixture.inputSequences.clicks.size());
        ObservationKeyEvent terminal = events.stream()
                .filter(event -> event.eventType() == ObservationKeyEventType.TIANTING_RECOVERY_ALL_MISSED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("an available frame with seven misses must cross the retained seam"));
        assertTrue(terminal.eventId().startsWith("tianting-recovery-all-missed-"));
        assertEquals("probeCorrelation=test-recovery", terminal.detail(),
                "the all-miss terminal must retain the exact Cloud-issued recovery correlation");
        assertDoesNotThrow(() -> ObservationProtocolValidator.requireValid(new ObservationRequest(
                ObservationProtocolValidator.CONTRACT_VERSION,
                "tenant-1", "device-1", fixture.context.getWindowId(), "0x1234",
                TaskType.TIANTING.getCode(), "run-1", 0L, 1_000L, 0L,
                null, null, null, null, null, List.of(), List.of(), List.of(terminal),
                List.of(), List.of(), List.of(), List.of(), List.of())));
    }

    @Test
    void duoxieFollowUpGapCannotPublishAllMissBeforeFengyaoAppears() {
        Fixture fixture = new Fixture();
        fixture.armRecoveryInterest();
        fixture.showOption(TiantingDialogLocalMechanics.DUOXIE);
        fixture.collectCycles(1);

        fixture.showNothing();
        List<ObservationKeyEvent> gapEvents = fixture.collectCycles(1);

        assertTrue(fixture.context.isTiantingFengyaoPending());
        assertTrue(gapEvents.stream().noneMatch(
                        event -> event.eventType() == ObservationKeyEventType.TIANTING_RECOVERY_ALL_MISSED),
                "the transition between 多谢 and 使用封妖符 is not an all-miss terminal");
    }

    @Test
    void anotherTasksInterestNeverDrivesThisProbe() {
        Fixture fixture = new Fixture();
        fixture.context.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.XIULUO_V2)
                .operations(List.of(DialogOperation.XIULUO_ENTER_BATTLE))
                .source("test")
                .localTemplateProbeOnly(true)
                .probeStartAtMs(1L)
                .build(), "test");
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);

        fixture.collectCycles(2);

        assertEquals(0, fixture.inputSequences.clicks.size(),
                "the 天庭 option templates must stay inert for every other task");
    }

    @Test
    void anExpiredInterestStopsTheProbeEvenIfTheCloudNeverClearedIt() {
        Fixture fixture = new Fixture();
        fixture.context.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.TIANTING)
                .operations(List.of(DialogOperation.TIANTING_COMBAT_OPTION))
                .source("test")
                .localTemplateProbeOnly(true)
                .probeStartAtMs(1L)
                .expiresAtMs(1L)
                .build(), "test");
        fixture.showOption(TiantingDialogLocalMechanics.KAIDA);

        fixture.collectCycles(2);

        assertEquals(0, fixture.inputSequences.clicks.size(),
                "a stale interest must not keep the client clicking after the cloud has moved on");
    }

    private static final class Fixture {
        final WindowRuntimeContext context = new WindowRuntimeContext(WINDOW, new GameContext());
        final WindowTaskContextHolder contextHolder =
                new WindowTaskContextHolder(new WindowIsolationProperties());
        final RecordingInputSequences inputSequences = new RecordingInputSequences();
        final ScriptedTracker tracker = new ScriptedTracker();

        Fixture() {
            context.setNativeBinding(new WindowNativeBinding(HWND, "t", "c", 88L, 0, 0, 1024, 768));
        }

        void armInterest() {
            context.updateDialogInterest(WindowDialogInterest.builder()
                    .taskType(TaskType.TIANTING)
                    .operations(List.of(DialogOperation.TIANTING_COMBAT_OPTION))
                    .source("test")
                    .localTemplateProbeOnly(true)
                    .probeStartAtMs(1L)
                    .build(), "test");
        }

        void armRecoveryInterest() {
            armDialogInterest(DialogOperation.TIANTING_RECOVERY_OPTION, "test-recovery");
        }

        void armCancelInterest() {
            armDialogInterest(DialogOperation.TIANTING_CANCEL_TASK, "test-cancel");
        }

        void armDialogInterest(DialogOperation operation, String source) {
            context.updateDialogInterest(WindowDialogInterest.builder()
                    .taskType(TaskType.TIANTING)
                    .operations(List.of(operation))
                    .source(source)
                    .localTemplateProbeOnly(true)
                    .probeStartAtMs(1L)
                    .build(), source);
        }

        void armYinyaoInterest(long createdAtMs, String intentId) {
            context.updateDialogInterest(WindowDialogInterest.builder()
                    .taskType(TaskType.TIANTING)
                    .operations(List.of(DialogOperation.TIANTING_YINYAO))
                    .source("test-yinyao-chain")
                    .createdAtMs(createdAtMs)
                    .localTemplateProbeOnly(true)
                    .probeStartAtMs(1L)
                    .followUpAbsoluteX(321)
                    .followUpAbsoluteY(222)
                    .followUpPathingIntent(WindowPathingIntent.builder()
                            .source("test-yinyao-chain")
                            .intentId(intentId)
                            .type(WindowPathingIntentType.UNTARGETED_TRACKER)
                            .build())
                    .build(), "test-yinyao-chain");
        }

        void showOption(String templatePath) {
            tracker.frame = canvasWith(templatePath);
        }

        void showNothing() {
            tracker.frame = canvasWith(null);
        }

        /**
         * A fresh sampler per cycle, the same way the kanda contract test drives its probe: pacing
         * state is per-sampler while the one-shot claim lives in the context, so this both bypasses
         * the 1s throttle and proves the claim survives a sampler rebuilt mid-run.
         */
        List<ObservationKeyEvent> collectCycles(int cycles) {
            List<ObservationKeyEvent> collected = new java.util.ArrayList<>();
            for (int cycle = 0; cycle < cycles; cycle++) {
                collected.addAll(newSampler().collect(List.of()).events());
            }
            return collected;
        }

        private WindowObservationSampler newSampler() {
            return new WindowObservationSampler(
                    context, contextHolder, tracker, new CoordinateHelper(tracker, null),
                    new DialogService(
                            new GameClientTracker(null, null, null, null, null, null, null, null,
                                    null, null, null),
                            new CoordinateHelper(null, null)),
                    inputSequences, RUN_ID, false);
        }

        private static BufferedImage canvasWith(String templatePath) {
            BufferedImage frame = new BufferedImage(1024, 768, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = frame.createGraphics();
            graphics.setColor(new Color(30, 26, 22));
            graphics.fillRect(0, 0, 1024, 768);
            if (templatePath != null) {
                try {
                    graphics.drawImage(ImageIO.read(new File(templatePath)),
                            TiantingDialogLocalMechanics.DIALOG_ROI_LEFT + 60,
                            TiantingDialogLocalMechanics.DIALOG_ROI_TOP + 70, null);
                } catch (IOException unreadable) {
                    throw new IllegalStateException(templatePath, unreadable);
                }
            }
            graphics.dispose();
            return frame;
        }
    }

    private static String name(String templatePath) {
        return templatePath.substring(templatePath.lastIndexOf('/') + 1);
    }

    /**
     * Returns the scripted screen for every capture. Capture rectangles are {@code (x1,y1,x2,y2)}
     * corners here, matching {@link GameClientTracker#captureToMemory}; the window sits at origin so
     * screen coordinates and frame coordinates coincide.
     */
    private static final class ScriptedTracker extends GameClientTracker {
        private volatile BufferedImage frame;

        ScriptedTracker() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public boolean refreshWindowState() {
            return true;
        }

        @Override
        public int getWindowBaseX() {
            return 0;
        }

        @Override
        public int getWindowBaseY() {
            return 0;
        }

        @Override
        public BufferedImage captureToMemory(String source, int x1, int y1, int x2, int y2) {
            BufferedImage current = frame;
            if (current == null) {
                return null;
            }
            int left = Math.max(0, Math.min(x1, current.getWidth() - 1));
            int top = Math.max(0, Math.min(y1, current.getHeight() - 1));
            int width = Math.max(1, Math.min(x2 - x1, current.getWidth() - left));
            int height = Math.max(1, Math.min(y2 - y1, current.getHeight() - top));
            return current.getSubimage(left, top, width, height);
        }
    }

    private static final class RecordingInputSequences extends InputSequences {
        final List<String> clicks = new CopyOnWriteArrayList<>();
        final List<List<InputAction>> submissions = new CopyOnWriteArrayList<>();
        volatile boolean clickResult = true;

        RecordingInputSequences() {
            super(null);
        }

        @Override
        public boolean moveAndClickLeft(String source, int x, int y, int moveDelayMs, int clickDelayMs) {
            clicks.add(source + "@" + x + "," + y);
            return clickResult;
        }

        @Override
        public boolean submitAndWait(String description, List<InputAction> actions) {
            submissions.add(List.copyOf(actions));
            return clickResult;
        }
    }
}
