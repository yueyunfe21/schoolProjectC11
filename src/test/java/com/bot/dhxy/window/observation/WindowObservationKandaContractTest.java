package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.dialog.DialogFingerprintWashMode;
import com.bot.dhxy.model.dialog.DialogType;
import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.task.wubei.WubeiDialogCatalog;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.observation.WindowObservationSampler.SampleBatch;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.config.WindowIsolationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40G §6/§10.1 local-kanda contracts at the sampler level: gate discipline (interest + probe-only + timing
 * anchor + open attempt), ordinary miss producing absolutely nothing, consume-time revalidation, the one-shot
 * attempt CAS (a second hit for the same attempt never clicks; an unexecuted click releases the claim), the single
 * atomic move+click request through the one input path, the {@code ENTER_BATTLE_CLICKED} key event, and the rule
 * that a click never clears the interest or the schedule (only real IN_COMBAT does). No runtime, no capture, no
 * physical input — the matcher and the input boundary are scripted; every arbitration object is the real one.
 */
class WindowObservationKandaContractTest {

    private static final String WINDOW = "window-7";
    private static final String HWND = "12345";
    private static final String ATTEMPT = "attempt-1";
    private static final String RUN_ID = "run-1";
    private static final String TASK_RUN_ID = RUN_ID + ":0:XIULUO_V2";

    @Test
    void wubeiLocalPreparePublishesTypedFactWithoutInputAndOrdinaryMissPublishesNothing() {
        Fixture fixture = new Fixture();
        fixture.context.updateDialogInterest(WindowDialogInterest.builder()
                .taskType(TaskType.WUBEI)
                .operations(List.of(DialogOperation.WUBEI_ENTER_BATTLE))
                .source("wubei-test")
                .build(), "test");
        WindowObservationSampler sampler = fixture.sampler();

        assertTrue(sampler.collect(List.of(), 1L).preparedDialogs().isEmpty(),
                "ordinary local miss must publish no fact");
        long observerSeq = 2L;
        for (GreenTemplateClickSpec spec : WubeiDialogCatalog.enterBattleSpecs()) {
            fixture.dialogService.wubeiPrepared = preparedWubeiMatch(spec);
            SampleBatch hit = fixture.sampler().collect(List.of(), observerSeq);
            assertEquals(1, hit.preparedDialogs().size());
            assertEquals(observerSeq, hit.preparedDialogs().getFirst().observerSeq());
            assertEquals(spec.name(), hit.preparedDialogs().getFirst().actionKey());
            assertEquals(spec.templatePath(), hit.preparedDialogs().getFirst().templatePath());
            observerSeq++;
        }
        assertTrue(fixture.inputSequences.clicks.isEmpty(), "Client prepare must never click");

        fixture.context.clearDialogInterest("wubei-test-clear");
        fixture.dialogService.wubeiPrepared =
                preparedWubeiMatch(WubeiDialogCatalog.enterBattleSpecs().getFirst());
        assertTrue(fixture.sampler().collect(List.of(), observerSeq).preparedDialogs().isEmpty(),
                "cleared interest must stop local preparation");
        assertTrue(fixture.dialogService.wubeiPrepared != null,
                "cleared interest must not even enter the local matcher");
    }

    private static DialogService.LocalPreparedDialogMatch preparedWubeiMatch(GreenTemplateClickSpec spec) {
        long now = System.currentTimeMillis();
        return new DialogService.LocalPreparedDialogMatch(
                PreparedDialogAction.builder()
                        .dialogType(DialogType.OPTION)
                        .operation(DialogOperation.WUBEI_ENTER_BATTLE)
                        .targetKeyword(spec.name())
                        .matchedText(spec.templatePath())
                        .relativeX(80).relativeY(90)
                        .absoluteX(330).absoluteY(402)
                        .validationLeft(286).validationTop(384)
                        .validationRight(374).validationBottom(420)
                        .washMode(DialogFingerprintWashMode.TEMPLATE_SPECIFIC)
                        .fingerprint("1x1:f")
                        .clickRequired(true)
                        .preparedAtMs(now)
                        .lastVerifiedAtMs(now)
                        .source("local")
                        .build(),
                300, 380, 330, 400);
    }

    @Test
    void probeIsGatedOnInterestProbeOnlyAnchorAndOpenAttempt() {
        Fixture fixture = new Fixture();
        // No dialog interest at all: the probe must not even run the matcher.
        fixture.sampler().collect(List.of());
        assertEquals(0, fixture.dialogService.findCalls.size(), "no interest means no probe");

        // Interest without probe-only mode must not enable the probe.
        fixture.context.updateDialogInterest(interest(false, 1L), "test");
        fixture.openSchedule();
        fixture.sampler().collect(List.of());
        assertEquals(0, fixture.dialogService.findCalls.size(), "non-probe-only interest means no probe");

        // Probe-only interest before the timing anchor must not enable the probe.
        fixture.context.updateDialogInterest(interest(true, System.currentTimeMillis() + 60_000L), "test");
        fixture.sampler().collect(List.of());
        assertEquals(0, fixture.dialogService.findCalls.size(), "the CR253 timing anchor gates the probe");

        // Complete gate: probe runs (and misses).
        fixture.context.updateDialogInterest(interest(true, 1L), "test");
        fixture.sampler().collect(List.of());
        assertEquals(1, fixture.dialogService.findCalls.size(), "the fully-gated probe runs");
    }

    @Test
    void ordinaryMissProducesNoEventNoClaimNoInput() {
        Fixture fixture = new Fixture();
        fixture.armGate();
        SampleBatch batch = fixture.sampler().collect(List.of());
        assertEquals(1, fixture.dialogService.findCalls.size());
        assertTrue(batch.events().isEmpty(), "a miss publishes nothing");
        assertTrue(fixture.inputSequences.clicks.isEmpty(), "a miss never inputs");
        assertFalse(batch.dialogInterests().getFirst().enterBattleClaimed(),
                "an ordinary local miss carries no claim and cannot create Cloud-side business truth");
        assertTrue(fixture.context.tryClaimXiuluoEnterBattleClick(fixture.liveSchedule(), "test-claim-free"),
                "a miss must not consume the attempt's one-shot claim");
        fixture.context.releaseXiuluoEnterBattleClick(fixture.liveSchedule(), "test-claim-free");
    }

    @Test
    void disabledLocalKandaNeverMatchesClaimsClicksOrPublishes() {
        Fixture fixture = new Fixture();
        fixture.armGate();
        fixture.dialogService.script(match(), match());

        SampleBatch batch = fixture.sampler(RUN_ID, false).collect(List.of());

        assertTrue(fixture.dialogService.findCalls.isEmpty(), "disabled means the matcher is not entered");
        assertTrue(fixture.inputSequences.clicks.isEmpty(), "disabled means no local input is submitted");
        assertTrue(batch.events().isEmpty(), "disabled means no local click fact is published");
        assertTrue(fixture.context.tryClaimXiuluoEnterBattleClick(fixture.liveSchedule(), "disabled-claim-free"),
                "disabled means the attempt claim remains untouched");
    }

    @Test
    void hitRevalidatesWinsCasClicksOnceAndPublishesClickedWithoutClearingAnything() {
        Fixture fixture = new Fixture();
        fixture.armGate();
        fixture.dialogService.script(match(), match());

        SampleBatch batch = fixture.sampler().collect(List.of());

        assertEquals(1, fixture.inputSequences.clicks.size(), "exactly one atomic move+click request");
        ScriptedInputSequences.Click click = fixture.inputSequences.clicks.get(0);
        assertEquals(500, click.x());
        assertEquals(400, click.y());
        assertEquals(1, batch.events().size());
        assertEquals("enter-battle-clicked-" + ATTEMPT, batch.events().get(0).eventId());
        assertEquals(ATTEMPT, batch.events().get(0).attemptId());
        assertEquals(1, batch.events().get(0).round());
        assertTrue(batch.dialogInterests().getFirst().enterBattleClaimed(),
                "the same request exposes the local winner so Cloud demand is revoked before fallback analysis");
        // A click is not combat: interest and schedule stay open so the probe keeps covering the attempt.
        assertTrue(fixture.context.getDialogInterest().isPresent(), "click must not clear the dialog interest");
        assertTrue(fixture.context.getXiuluoGreenChainSchedule().isPresent(),
                "click must not clear the green-chain schedule");

        // A second hit for the same attempt loses the one-shot CAS and never clicks again.
        fixture.dialogService.script(match(), match());
        SampleBatch second = fixture.sampler().collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size(), "the same attempt can never be clicked twice");
        assertTrue(second.events().isEmpty());
    }

    @Test
    void revalidationMissAbortsWithoutClaimOrClick() {
        Fixture fixture = new Fixture();
        fixture.armGate();
        fixture.dialogService.script(match(), null);

        SampleBatch batch = fixture.sampler().collect(List.of());

        assertTrue(fixture.inputSequences.clicks.isEmpty(), "a failed revalidation never clicks");
        assertTrue(batch.events().isEmpty());
        assertTrue(fixture.context.tryClaimXiuluoEnterBattleClick(fixture.liveSchedule(), "test-claim-free"),
                "an aborted consume must not burn the attempt claim");
        fixture.context.releaseXiuluoEnterBattleClick(fixture.liveSchedule(), "test-claim-free");
    }

    @Test
    void unexecutedClickReleasesTheClaimSoTheOpenAttemptKeepsItsFastPath() {
        Fixture fixture = new Fixture();
        fixture.armGate();
        fixture.inputSequences.clickResult = false;
        fixture.dialogService.script(match(), match());
        SampleBatch first = fixture.sampler().collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size());
        assertTrue(first.events().isEmpty(), "an unexecuted click publishes no clicked event");

        // The claim was released, so a later hit for the same still-open attempt may click.
        fixture.inputSequences.clickResult = true;
        fixture.dialogService.script(match(), match());
        SampleBatch second = fixture.sampler().collect(List.of());
        assertEquals(2, fixture.inputSequences.clicks.size(),
                "an unexecuted click consumes nothing; the attempt keeps its fast path");
        assertEquals(1, second.events().size());
    }

    @Test
    void replacedAttemptReArmsTheOneShotClaim() {
        Fixture fixture = new Fixture();
        fixture.armGate();
        fixture.dialogService.script(match(), match());
        fixture.sampler().collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size());

        // A new attempt (executed re-press semantics) re-arms the claim; the old attempt's click cannot satisfy it.
        fixture.openSchedule("attempt-2");
        fixture.dialogService.script(match(), match());
        SampleBatch second = fixture.sampler().collect(List.of());
        assertEquals(2, fixture.inputSequences.clicks.size(), "a fresh attempt has its own one-shot click");
        assertEquals("enter-battle-clicked-attempt-2", second.events().get(0).eventId());
    }

    @Test
    void pairedInstallIsAtomicToTheObservationReader() throws Exception {
        // TURN-40G review#3 P1: an attempt replacement installs interest+schedule as ONE transition. The reader
        // (the sampler's paired snapshot) must never observe interest and schedule from different transitions —
        // each installed pair carries the same attempt marker in both halves, so any torn read is detected.
        Fixture fixture = new Fixture();
        int iterations = 1_000;
        Thread writer = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                String attemptId = "attempt-" + i;
                fixture.context.updateDialogInterestWithXiuluoGreenChainSchedule(
                        WindowDialogInterest.builder()
                                .taskType(TaskType.XIULUO_V2)
                                .operations(List.of(DialogOperation.XIULUO_ENTER_BATTLE))
                                .source(attemptId)
                                .localTemplateProbeOnly(true)
                                .probeStartAtMs(1L)
                                .build(),
                        XiuluoGreenChainSchedule.builder()
                                .windowId(WINDOW)
                                .hwnd(HWND)
                                .taskRunId(TASK_RUN_ID)
                                .round(1)
                                .attemptId(attemptId)
                                .openedAtMs(System.currentTimeMillis())
                                .build(),
                        "race-" + i);
            }
        }, "kanda-race-writer");
        List<String> torn = new CopyOnWriteArrayList<>();
        writer.start();
        while (writer.isAlive()) {
            WindowRuntimeContext.XiuluoKandaProbeView view = fixture.context.getXiuluoKandaProbeView();
            if (view.interest() != null && view.schedule() != null
                    && !view.interest().getSource().equals(view.schedule().getAttemptId())) {
                torn.add(view.interest().getSource() + "!=" + view.schedule().getAttemptId());
            }
        }
        writer.join();
        assertTrue(torn.isEmpty(),
                "a reader must never see interest and schedule from different transitions: " + torn);
        assertEquals("attempt-" + (iterations - 1),
                fixture.context.getXiuluoKandaProbeView().schedule().getAttemptId(),
                "the final consistent pair is the last installed attempt");
    }

    @Test
    void oldRunnerCannotConsumeOrReleaseTheNewRunsPairedScheduleAndCurrentRunWinsOnce() {
        // TURN-40G review#4 P1: during a stop/restart overlap the new run has installed the paired probe-only
        // interest + green-chain schedule for its OWN run identity ("run-2"/"attempt-2"). An old runner whose
        // authoritative run is the retired "run-1" must be a clean no-op on it: no matcher, no input, no event,
        // and it must not be able to release the new run's one-shot claim. Only the current run ("run-2") acts.
        Fixture fixture = new Fixture();
        fixture.context.updateDialogInterest(interest(true, 1L), "test");
        fixture.openSchedule("attempt-2", "run-2:0:XIULUO_V2");
        fixture.dialogService.script(match(), match());

        // Old runner (retired run-1) samples the new run's schedule: fenced out before the matcher.
        SampleBatch stale = fixture.sampler("run-1").collect(List.of());
        assertEquals(0, fixture.dialogService.findCalls.size(),
                "an old run never runs the matcher on the new run's schedule");
        assertTrue(fixture.inputSequences.clicks.isEmpty(), "an old run never clicks for the new run");
        assertTrue(stale.events().isEmpty(), "an old run never publishes an enter-battle event for the new run");

        // The new run claims its one-shot click; the old runner cannot release it.
        assertTrue(fixture.context.tryClaimXiuluoEnterBattleClick(fixture.liveSchedule(), "current-run-claim"),
                "the new run's claim is untouched by the old runner");
        fixture.context.releaseXiuluoEnterBattleClick(
                fixture.expectedSchedule("attempt-2", TASK_RUN_ID), "old-runner-release-attempt");
        assertFalse(fixture.context.tryClaimXiuluoEnterBattleClick(fixture.liveSchedule(), "recheck"),
                "an old runner must not release (re-arm) the current run's held claim");
        // Restore the free claim for the current run's real consume below.
        fixture.context.releaseXiuluoEnterBattleClick(fixture.liveSchedule(), "reset-for-current-run");

        // The current run's sampler is the exactly-one CAS winner.
        fixture.dialogService.script(match(), match());
        SampleBatch current = fixture.sampler("run-2").collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size(), "the current run clicks exactly once");
        assertEquals(1, current.events().size());
        assertEquals("enter-battle-clicked-attempt-2", current.events().get(0).eventId());

        // A second current-run hit for the same attempt still loses the one-shot CAS.
        fixture.dialogService.script(match(), match());
        SampleBatch second = fixture.sampler("run-2").collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size(), "the same attempt can never be clicked twice");
        assertTrue(second.events().isEmpty());
    }

    @Test
    void queueRunFenceAcceptsOnlyColonDelimitedChildTaskRun() {
        Fixture fixture = new Fixture();
        fixture.context.updateDialogInterest(interest(true, 1L), "test");
        fixture.openSchedule(ATTEMPT, TASK_RUN_ID);
        fixture.dialogService.script(match(), match());

        SampleBatch current = fixture.sampler(RUN_ID).collect(List.of());

        assertEquals(1, fixture.inputSequences.clicks.size(),
                "the acknowledged queue run must own its exact child task run");
        assertEquals(1, current.events().size());

        Fixture prefixCollision = new Fixture();
        prefixCollision.context.updateDialogInterest(interest(true, 1L), "test");
        prefixCollision.openSchedule(ATTEMPT, "run-10:0:XIULUO_V2");
        prefixCollision.dialogService.script(match(), match());

        SampleBatch stale = prefixCollision.sampler(RUN_ID).collect(List.of());

        assertTrue(prefixCollision.inputSequences.clicks.isEmpty(),
                "a textual prefix collision without the colon boundary must remain fenced");
        assertTrue(stale.events().isEmpty());
        assertTrue(prefixCollision.dialogService.findCalls.isEmpty(),
                "the mismatched run must be rejected before matching or capture");
    }

    @Test
    void sameRunScheduleReplacementChangingRoundOrHwndFencesStaleMatcherClaimAndRelease() {
        // TURN-40G review#5 P1: within ONE run, replacing the schedule with a different round AND hwnd — even
        // while the taskRunId AND attemptId collide/reuse — is a different FULL identity. A stale sampler that
        // started its probe on the old schedule must click nothing and publish nothing, and the claim/release
        // fence must reject the stale full identity outright (never re-arming the replacement's claim).
        Fixture fixture = new Fixture();
        fixture.armGate();                              // schedule S1: attempt-1 / run-1 / round 1 / HWND
        XiuluoGreenChainSchedule stale = fixture.liveSchedule();
        fixture.dialogService.script(match(), match());
        // Between the probe and the post-fresh-frame read, the SAME run+attempt replaces the schedule with a
        // different round and hwnd — the sampler's captured S1 is now stale.
        fixture.dialogService.onRevalidate(() -> fixture.openSchedule(ATTEMPT, RUN_ID, 2, "99999"));

        SampleBatch batch = fixture.sampler().collect(List.of());

        assertTrue(fixture.inputSequences.clicks.isEmpty(),
                "a same-run round/hwnd replacement fences the stale matcher: zero click");
        assertTrue(batch.events().isEmpty(), "a fenced stale matcher publishes nothing");

        // The stale full identity can neither claim nor release the replacement's one-shot click.
        XiuluoGreenChainSchedule live = fixture.liveSchedule();   // S2: round 2 / hwnd 99999
        assertFalse(fixture.context.tryClaimXiuluoEnterBattleClick(stale, "stale-claim"),
                "a stale full identity (old round/hwnd) can never claim the replacement's click");
        assertTrue(fixture.context.tryClaimXiuluoEnterBattleClick(live, "live-claim"),
                "the replacement's exact identity claims its one-shot click");
        fixture.context.releaseXiuluoEnterBattleClick(stale, "stale-release");
        assertFalse(fixture.context.tryClaimXiuluoEnterBattleClick(live, "live-recheck"),
                "a stale full identity must not release (re-arm) the replacement's held claim");
    }

    // ---- fixtures ----

    private static WindowDialogInterest interest(boolean probeOnly, long probeStartAtMs) {
        return WindowDialogInterest.builder()
                .taskType(TaskType.XIULUO_V2)
                .operations(List.of(DialogOperation.XIULUO_ENTER_BATTLE))
                .source("test")
                .localTemplateProbeOnly(probeOnly)
                .probeStartAtMs(probeStartAtMs)
                .build();
    }

    private static DialogService.LocalDialogTemplateMatch match() {
        return new DialogService.LocalDialogTemplateMatch(new int[] {480, 380, 520, 420}, 500, 400, 0.91D);
    }

    private final class Fixture {
        final WindowRuntimeContext context;
        final WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
        final ScriptedDialogService dialogService = new ScriptedDialogService();
        final ScriptedInputSequences inputSequences = new ScriptedInputSequences();

        Fixture() {
            context = new WindowRuntimeContext(WINDOW, new GameContext());
            context.setNativeBinding(new WindowNativeBinding(HWND, "t", "c", 88L, 0, 0, 1024, 768));
        }

        void armGate() {
            context.updateDialogInterest(interest(true, 1L), "test");
            openSchedule();
        }

        void openSchedule() {
            openSchedule(ATTEMPT);
        }

        void openSchedule(String attemptId) {
            openSchedule(attemptId, TASK_RUN_ID);
        }

        void openSchedule(String attemptId, String taskRunId) {
            openSchedule(attemptId, taskRunId, 1, HWND);
        }

        void openSchedule(String attemptId, String taskRunId, int round, String hwnd) {
            context.updateXiuluoGreenChainSchedule(XiuluoGreenChainSchedule.builder()
                    .windowId(WINDOW)
                    .hwnd(hwnd)
                    .taskRunId(taskRunId)
                    .round(round)
                    .attemptId(attemptId)
                    .openedAtMs(System.currentTimeMillis())
                    .build(), "test");
        }

        /** The exact schedule currently open in the context (the expected full identity for claim/release). */
        XiuluoGreenChainSchedule liveSchedule() {
            return context.getXiuluoGreenChainSchedule().orElseThrow();
        }

        /** A standalone expected full identity (windowId/hwnd/round fixed to the fixture defaults). */
        XiuluoGreenChainSchedule expectedSchedule(String attemptId, String taskRunId) {
            return XiuluoGreenChainSchedule.builder()
                    .windowId(WINDOW).hwnd(HWND).taskRunId(taskRunId).round(1).attemptId(attemptId)
                    .openedAtMs(System.currentTimeMillis()).build();
        }

        /** Fresh sampler per cycle: pacing state is per-sampler while all arbitration state is in the context. */
        WindowObservationSampler sampler() {
            return sampler(RUN_ID);
        }

        /** Sampler bound to an explicit authoritative run identity (TURN-40G review#4 restart-overlap fencing). */
        WindowObservationSampler sampler(String taskRunId) {
            return sampler(taskRunId, true);
        }

        WindowObservationSampler sampler(String taskRunId, boolean localKandaEnabled) {
            return new WindowObservationSampler(
                    context,
                    contextHolder,
                    new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null),
                    new CoordinateHelper(null, null),
                    dialogService,
                    inputSequences,
                    taskRunId,
                    localKandaEnabled);
        }
    }

    /** Scripted matcher: each script entry is (probeResult, revalidateResult); unscripted probes miss. */
    private static final class ScriptedDialogService extends DialogService {
        final List<String> findCalls = new CopyOnWriteArrayList<>();
        private DialogService.LocalDialogTemplateMatch probeResult;
        private DialogService.LocalDialogTemplateMatch revalidateResult;
        /** Optional hook run inside revalidation, i.e. AFTER the probe and BEFORE the post-fresh-frame read. */
        private Runnable onRevalidate;
        private DialogService.LocalPreparedDialogMatch wubeiPrepared;

        ScriptedDialogService() {
            super(new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null),
                    new CoordinateHelper(null, null));
        }

        void script(DialogService.LocalDialogTemplateMatch probe,
                    DialogService.LocalDialogTemplateMatch revalidate) {
            this.probeResult = probe;
            this.revalidateResult = revalidate;
        }

        void onRevalidate(Runnable hook) {
            this.onRevalidate = hook;
        }

        @Override
        public Optional<LocalDialogTemplateMatch> findXiuluoEnterBattleLocalTemplate(String source, String phase) {
            findCalls.add(phase);
            DialogService.LocalDialogTemplateMatch result = probeResult;
            probeResult = null;
            return Optional.ofNullable(result);
        }

        @Override
        public Optional<LocalDialogTemplateMatch> revalidateXiuluoEnterBattleLocalTemplate(String source,
                                                                                          String reason) {
            if (onRevalidate != null) {
                Runnable hook = onRevalidate;
                onRevalidate = null;
                hook.run();
            }
            DialogService.LocalDialogTemplateMatch result = revalidateResult;
            revalidateResult = null;
            return Optional.ofNullable(result);
        }

        @Override
        public Optional<LocalPreparedDialogMatch> prepareWubeiEnterBattleLocal(String source) {
            LocalPreparedDialogMatch result = wubeiPrepared;
            wubeiPrepared = null;
            return Optional.ofNullable(result);
        }
    }

    /** Scripted single-input boundary: records the atomic move+click request; never touches a real queue. */
    private static final class ScriptedInputSequences extends InputSequences {
        record Click(String description, int x, int y, int settleMs, int delayMs) {
        }

        final List<Click> clicks = new CopyOnWriteArrayList<>();
        volatile boolean clickResult = true;

        ScriptedInputSequences() {
            super(null);
        }

        @Override
        public boolean moveAndClickLeft(String description, int x, int y, int settleMs, int delayMs) {
            clicks.add(new Click(description, x, y, settleMs, delayMs));
            return clickResult;
        }

        @Override
        public CompletionStage<Boolean> moveAndClickLeftAsync(
                String description, int x, int y, int settleMs, int delayMs) {
            clicks.add(new Click(description, x, y, settleMs, delayMs));
            return CompletableFuture.completedFuture(clickResult);
        }
    }
}
