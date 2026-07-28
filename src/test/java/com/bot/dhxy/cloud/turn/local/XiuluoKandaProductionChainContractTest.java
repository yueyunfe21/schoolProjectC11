package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.cloud.turn.LocalServiceExecution;
import com.bot.dhxy.cloud.turn.protocol.TurnAction;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalOperation;
import com.bot.dhxy.cloud.turn.protocol.TurnLocalServiceCall;
import com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidator;
import com.bot.dhxy.cloud.turn.protocol.TurnStep;
import com.bot.dhxy.cloud.turn.protocol.TurnStepType;
import com.bot.dhxy.cloud.turn.protocol.TurnWholeTaskRuntimeArguments;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.dialog.DialogOperation;
import com.bot.dhxy.model.job.PreparedActionJob;
import com.bot.dhxy.model.job.PreparedActionJobType;
import com.bot.dhxy.model.job.XiuluoGreenChainSchedule;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowDialogInterest;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.observation.WindowObservationSampler;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TURN-40G P1-repair production-entry contracts. The wire payload below is byte-equivalent to what Cloud
 * {@code XiuluoTaskV2.registerXiuluoShortcutEnterBattleInterest} sends for the initial green click and the
 * saved-green retry (probeOnly=TRUE, probeStartAtMs = firstTrackerGreenClickAtMs + 25s, exact schedule identity),
 * and it is executed through the REAL {@code WHOLE_TASK_DIALOG_INTEREST_UPDATE} local-operation executor against a
 * REAL {@code WindowRuntimeContext}. Proven: exact identity/anchor propagation into the installed probe-only
 * interest and the atomically opened schedule; anchor-before => zero matcher runs and zero input; anchor-after =>
 * exactly one current-attempt CAS winner; attempt replacement stale-fences the previous attempt (jobs dropped,
 * one-shot claim re-armed, new attempt clicks exactly once). No runtime, no capture, no physical input.
 */
class XiuluoKandaProductionChainContractTest {

    private static final String WINDOW = "window-7";
    private static final String HWND = "12345";
    private static final String TASK_RUN_ID = "start-req-40g";
    private static final long PROBE_DELAY_MS = 25_000L;

    @Test
    void initialAttemptInstallsExactProbeOnlyAnchorAndScheduleThroughTheRealExecutor() {
        Fixture fixture = new Fixture();
        long firstGreenClickAt = System.currentTimeMillis();
        long openedAt = firstGreenClickAt + 5L;

        fixture.executeInterestOp("attempt-1", 3, firstGreenClickAt, openedAt);

        WindowDialogInterest interest = fixture.context.getDialogInterest().orElseThrow();
        assertEquals(TaskType.XIULUO_V2, interest.getTaskType());
        assertEquals(List.of(DialogOperation.XIULUO_ENTER_BATTLE), interest.getOperations());
        assertTrue(interest.isLocalTemplateProbeOnly(), "the production interest must be probe-only");
        assertEquals(firstGreenClickAt + PROBE_DELAY_MS, interest.getProbeStartAtMs(),
                "the anchor must propagate exactly: first green click + 25s");

        XiuluoGreenChainSchedule schedule = fixture.context.getXiuluoGreenChainSchedule().orElseThrow();
        assertEquals(WINDOW, schedule.getWindowId());
        assertEquals(HWND, schedule.getHwnd(), "the schedule binds the live native handle");
        assertEquals(TASK_RUN_ID, schedule.getTaskRunId());
        assertEquals(3, schedule.getRound());
        assertEquals("attempt-1", schedule.getAttemptId());
        assertEquals(openedAt, schedule.getOpenedAtMs());
    }

    @Test
    void beforeTheAnchorTheProductionChainRunsZeroMatcherAndZeroInput() throws Exception {
        Fixture fixture = new Fixture();
        fixture.executeInterestOp("attempt-1", 1, System.currentTimeMillis(), System.currentTimeMillis());

        WindowObservationSampler.SampleBatch batch = fixture.sampler().collect(List.of());

        assertEquals(0, fixture.dialogService.findCalls.size(),
                "before first-green-click + 25s the matcher must never run");
        assertTrue(fixture.inputSequences.clicks.isEmpty(), "before the anchor there is never input");
        assertTrue(batch.events().isEmpty());
    }

    @Test
    void afterTheAnchorExactlyOneCurrentAttemptCasWinnerClicks() throws Exception {
        Fixture fixture = new Fixture();
        long firstGreenClickAt = System.currentTimeMillis() - PROBE_DELAY_MS - 5_000L;
        fixture.executeInterestOp("attempt-1", 1, firstGreenClickAt, System.currentTimeMillis());

        fixture.dialogService.script(match(), match());
        WindowObservationSampler.SampleBatch first = fixture.sampler().collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size(), "the anchored attempt clicks exactly once");
        assertEquals(1, first.events().size());
        assertEquals("attempt-1", first.events().get(0).attemptId());

        fixture.dialogService.script(match(), match());
        WindowObservationSampler.SampleBatch second = fixture.sampler().collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size(),
                "a second hit for the same attempt loses the one-shot CAS and never clicks");
        assertTrue(second.events().isEmpty());
    }

    @Test
    void attemptReplacementStaleFencesTheOldAttemptAndReArmsTheNewOne() throws Exception {
        Fixture fixture = new Fixture();
        long firstGreenClickAt = System.currentTimeMillis() - PROBE_DELAY_MS - 5_000L;
        fixture.executeInterestOp("attempt-1", 1, firstGreenClickAt, System.currentTimeMillis());

        fixture.dialogService.script(match(), match());
        fixture.sampler().collect(List.of());
        assertEquals(1, fixture.inputSequences.clicks.size(), "attempt-1 clicked once");

        // A pending typed job stamped for attempt-1 must die with the schedule replacement.
        assertTrue(fixture.context.publishPreparedActionJob(PreparedActionJob.builder()
                .type(PreparedActionJobType.XIULUO_ENTER_BATTLE)
                .windowId(WINDOW)
                .hwnd(HWND)
                .taskRunId(TASK_RUN_ID)
                .round(1)
                .attemptId("attempt-1")
                .source("test")
                .preparedAtMs(System.currentTimeMillis())
                .build(), "test-stale-job"));

        // Saved-green retry semantics: same ORIGINAL anchor, new attempt identity, schedule REPLACED atomically.
        fixture.executeInterestOp("attempt-2", 1, firstGreenClickAt, System.currentTimeMillis());

        assertEquals("attempt-2",
                fixture.context.getXiuluoGreenChainSchedule().orElseThrow().getAttemptId());
        assertNull(fixture.context.peekPreparedActionJob(PreparedActionJobType.XIULUO_ENTER_BATTLE),
                "the previous attempt's typed job is stale-fenced by the replacement");
        assertEquals(firstGreenClickAt + PROBE_DELAY_MS,
                fixture.context.getDialogInterest().orElseThrow().getProbeStartAtMs(),
                "the retry keeps the ORIGINAL timing anchor");

        fixture.dialogService.script(match(), match());
        WindowObservationSampler.SampleBatch retry = fixture.sampler().collect(List.of());
        assertEquals(2, fixture.inputSequences.clicks.size(),
                "the replaced attempt re-arms the one-shot claim: the new attempt clicks exactly once");
        assertEquals(1, retry.events().size());
        assertEquals("attempt-2", retry.events().get(0).attemptId());

        fixture.dialogService.script(match(), match());
        fixture.sampler().collect(List.of());
        assertEquals(2, fixture.inputSequences.clicks.size(), "attempt-2 can also never click twice");
    }

    @Test
    void collidingHashRunIdsNeverSatisfyEachOthersFence() throws Exception {
        // Deterministic Java-hash collision ("Aa"/"BB"); a shared queue suffix preserves the collision, so the
        // removed legacy long projection could not tell these two Cloud runs apart.
        String runA = "Aa-start-req-40g";
        String runB = "BB-start-req-40g";
        assertEquals(runA.hashCode(), runB.hashCode(),
                "precondition: the two run ids collide under the legacy hash projection");

        Fixture fixture = new Fixture();
        long firstGreenClickAt = System.currentTimeMillis() - PROBE_DELAY_MS - 5_000L;
        fixture.executeInterestOp(runA, "attempt-1", 1, firstGreenClickAt, System.currentTimeMillis());
        assertTrue(fixture.context.publishPreparedActionJob(PreparedActionJob.builder()
                .type(PreparedActionJobType.XIULUO_ENTER_BATTLE)
                .windowId(WINDOW)
                .hwnd(HWND)
                .taskRunId(runA)
                .round(1)
                .attemptId("attempt-1")
                .source("test")
                .preparedAtMs(System.currentTimeMillis())
                .build(), "run-a-job"));

        // A DIFFERENT run whose id collides in hash (same round, even the same attempt id) replaces the
        // schedule: the exact String identity must stale-fence run A's work. Under the removed hash projection
        // sameIdentity would have been true and run A's job would have survived into run B.
        fixture.executeInterestOp(runB, "attempt-1", 1, firstGreenClickAt, System.currentTimeMillis());

        assertEquals(runB, fixture.context.getXiuluoGreenChainSchedule().orElseThrow().getTaskRunId());
        assertNull(fixture.context.peekPreparedActionJob(PreparedActionJobType.XIULUO_ENTER_BATTLE),
                "run A's typed job must never survive into colliding-hash run B");
    }

    @Test
    void partialScheduleTupleIsRejectedOnTheWireAndMutatesNothing() {
        // Wire level: any partial tuple is rejected all-or-none before it can travel.
        assertThrows(IllegalArgumentException.class,
                () -> requireValidInterestAction(interestArguments("attempt-1", null, null, null)),
                "attempt id alone is a partial identity");
        assertThrows(IllegalArgumentException.class,
                () -> requireValidInterestAction(interestArguments("attempt-1", 1, TASK_RUN_ID, null)),
                "a tuple missing openedAtMs is partial");
        assertThrows(IllegalArgumentException.class,
                () -> requireValidInterestAction(interestArguments(null, 1, TASK_RUN_ID, 123L)),
                "a tuple missing the attempt id is partial");
        assertThrows(IllegalArgumentException.class,
                () -> requireValidInterestAction(interestArguments("attempt-1", 1, null, 123L)),
                "a tuple missing the run id is partial");

        // Executor level: even with the wire validator bypassed, a partial tuple fails fast BEFORE any runtime
        // mutation — no interest, no schedule, no fabricated identity.
        Fixture fixture = new Fixture();
        assertThrows(NullPointerException.class,
                () -> fixture.executeRawInterestOp(interestArguments("attempt-1", 1, TASK_RUN_ID, null)));
        assertTrue(fixture.context.getDialogInterest().isEmpty(), "zero mutation: no interest was installed");
        assertTrue(fixture.context.getXiuluoGreenChainSchedule().isEmpty(),
                "zero mutation: no schedule was installed");
        assertNull(fixture.context.peekPreparedActionJob(PreparedActionJobType.XIULUO_ENTER_BATTLE));
    }

    @Test
    void roundZeroIsRejectedOnTheWire() {
        IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                () -> requireValidInterestAction(interestArguments("attempt-1", 0, TASK_RUN_ID, 123L)));
        assertTrue(zero.getMessage().contains("positive"),
                "rounds are one-based: round zero must be rejected, got: " + zero.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> requireValidInterestAction(interestArguments("attempt-1", -1, TASK_RUN_ID, 123L)));
        // The exact one-based production tuple stays valid.
        requireValidInterestAction(interestArguments("attempt-1", 1, TASK_RUN_ID, 123L));
    }

    /** Wraps the interest payload exactly as it rides the wire and runs the shared protocol validator. */
    private static void requireValidInterestAction(TurnWholeTaskRuntimeArguments arguments) {
        TurnLocalServiceCall call = new TurnLocalServiceCall(
                TurnLocalOperation.WHOLE_TASK_DIALOG_INTEREST_UPDATE, null, null, null, null, arguments);
        TurnProtocolValidator.requireValid(new TurnAction(1, "action-1", "device-1", WINDOW,
                List.of(new TurnStep(0, TurnStepType.LOCAL_SERVICE, null, null, null, null, null, call)), false));
    }

    private static TurnWholeTaskRuntimeArguments interestArguments(String attemptId,
                                                                   Integer round,
                                                                   String taskRunId,
                                                                   Long openedAtMs) {
        return new TurnWholeTaskRuntimeArguments(
                "xiuluo-v2:shortcut-enter-battle:test",
                null, null, null, null, null, null, null, null, null, null, null, null,
                TaskType.XIULUO_V2.getCode(),
                null, null,
                List.of(DialogOperation.XIULUO_ENTER_BATTLE.name()),
                null,
                Boolean.TRUE,
                null, null, null, null, null, null, null,
                123_456L,
                attemptId,
                round,
                taskRunId,
                openedAtMs);
    }

    private static DialogService.LocalDialogTemplateMatch match() {
        return new DialogService.LocalDialogTemplateMatch(new int[] {480, 380, 520, 420}, 500, 400, 0.9D);
    }

    private static final class Fixture {
        final WindowRuntimeContext context;
        final WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(new WindowIsolationProperties());
        final WholeTaskRuntimeLocalOperationExecutor executor;
        final ScriptedDialogService dialogService = new ScriptedDialogService();
        final ScriptedInputSequences inputSequences = new ScriptedInputSequences();

        Fixture() {
            context = new WindowRuntimeContext(WINDOW, new GameContext());
            context.setNativeBinding(new WindowNativeBinding(HWND, "t", "c", 88L, 0, 0, 1024, 768));
            executor = new WholeTaskRuntimeLocalOperationExecutor(
                    contextHolder,
                    new LocalMovementFactMechanics(
                            nullTracker(), new CoordinateHelper(null, null), contextHolder),
                    allocate(FiveRingAcceptDialogLocalOperation.class),
                    new ObjectMapper());
        }

        /**
         * Executes the EXACT wire payload Cloud {@code registerXiuluoShortcutEnterBattleInterest} sends, through
         * the real local-operation executor under the real thread-local window binding.
         */
        void executeInterestOp(String attemptId, int round, long firstGreenClickAtMs, long openedAtMs) {
            executeInterestOp(TASK_RUN_ID, attemptId, round, firstGreenClickAtMs, openedAtMs);
        }

        void executeInterestOp(String taskRunId, String attemptId, int round,
                               long firstGreenClickAtMs, long openedAtMs) {
            TurnWholeTaskRuntimeArguments arguments = new TurnWholeTaskRuntimeArguments(
                    "xiuluo-v2:shortcut-enter-battle:" + round,
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    TaskType.XIULUO_V2.getCode(),
                    null, null,
                    List.of(DialogOperation.XIULUO_ENTER_BATTLE.name()),
                    null,
                    Boolean.TRUE,
                    null, null, null, null, null, null, null,
                    firstGreenClickAtMs + PROBE_DELAY_MS,
                    attemptId,
                    round,
                    taskRunId,
                    openedAtMs);
            LocalServiceExecution execution = executeRawInterestOp(arguments);
            assertNotNull(execution);
        }

        /** Runs an arbitrary interest payload through the REAL executor (used by the negative contracts too). */
        LocalServiceExecution executeRawInterestOp(TurnWholeTaskRuntimeArguments arguments) {
            TurnLocalServiceCall call = new TurnLocalServiceCall(
                    TurnLocalOperation.WHOLE_TASK_DIALOG_INTEREST_UPDATE,
                    null, null, null, null, arguments);
            return contextHolder.callWith(
                    context, () -> executor.execute(call, "action-test", 0, null));
        }

        WindowObservationSampler sampler() {
            return new WindowObservationSampler(
                    context, contextHolder, nullTracker(), new CoordinateHelper(null, null),
                    dialogService, inputSequences, TASK_RUN_ID);
        }

        private static GameClientTracker nullTracker() {
            return new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

    private static final class ScriptedDialogService extends DialogService {
        final List<String> findCalls = new CopyOnWriteArrayList<>();
        private DialogService.LocalDialogTemplateMatch probeResult;
        private DialogService.LocalDialogTemplateMatch revalidateResult;

        ScriptedDialogService() {
            super(new GameClientTracker(null, null, null, null, null, null, null, null, null, null, null, null),
                    new CoordinateHelper(null, null));
        }

        void script(DialogService.LocalDialogTemplateMatch probe,
                    DialogService.LocalDialogTemplateMatch revalidate) {
            this.probeResult = probe;
            this.revalidateResult = revalidate;
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
            DialogService.LocalDialogTemplateMatch result = revalidateResult;
            revalidateResult = null;
            return Optional.ofNullable(result);
        }
    }

    private static final class ScriptedInputSequences extends InputSequences {
        record Click(String description, int x, int y) {
        }

        final List<Click> clicks = new CopyOnWriteArrayList<>();

        ScriptedInputSequences() {
            super(null);
        }

        @Override
        public boolean moveAndClickLeft(String description, int x, int y, int settleMs, int delayMs) {
            clicks.add(new Click(description, x, y));
            return true;
        }
    }

    /** Inert never-invoked collaborator (our operation path cannot reach it). */
    private static <T> T allocate(Class<T> type) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            return type.cast(unsafe.allocateInstance(type));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot allocate inert test double " + type.getName(), e);
        }
    }
}
