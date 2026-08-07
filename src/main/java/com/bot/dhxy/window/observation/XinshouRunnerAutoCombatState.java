package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the exact-run gate for Runner automatic-combat maintenance.
 *
 * <p>Cloud remains the sole business authority. A new observation run starts disarmed; only a
 * successful Cloud-authorized {@code RESTORE_AUTO_COMBAT} command may arm a Xinshou run. Wild
 * Battle is intentionally armed at observation-run creation: it is an infinite combat-only task
 * and has no Cloud-side combat decision. Once armed, the Client Runner may maintain the
 * already-authorized automatic-combat invariant once per local combat generation. Failed local
 * evidence is retried with a short bound and never becomes a Cloud business fact.</p>
 */
@Component
public final class XinshouRunnerAutoCombatState {

    static final long FAILED_RETRY_DELAY_MS = 2_000L;
    private static final Logger log =
            LoggerFactory.getLogger(XinshouRunnerAutoCombatState.class);

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final MaintenancePort maintenancePort;

    @Autowired
    public XinshouRunnerAutoCombatState(XinshouCombatLocalMechanics mechanics) {
        this(mechanics::maintainRunnerAutoCombatOnce);
    }

    XinshouRunnerAutoCombatState(MaintenancePort maintenancePort) {
        this.maintenancePort = Objects.requireNonNull(maintenancePort, "maintenancePort");
    }

    /**
     * Starts one exact observation-run session in the disarmed state.
     *
     * @param context exact registered window runtime
     * @param taskCode acknowledged task code; Xinshou and Wild Battle create a session
     * @param taskRunId exact observation task-run identity
     */
    public void begin(WindowRuntimeContext context, String taskCode, String taskRunId) {
        if (context == null || taskRunId == null || taskRunId.isBlank() || taskCode == null) {
            if (context != null) {
                sessions.remove(context.getWindowId());
            }
            return;
        }
        String normalizedTaskCode = taskCode.trim();
        boolean xinshou = "XINSHOU".equalsIgnoreCase(normalizedTaskCode);
        boolean wildBattle = "WILD_BATTLE".equalsIgnoreCase(normalizedTaskCode);
        if (!xinshou && !wildBattle) {
            sessions.remove(context.getWindowId());
            return;
        }
        sessions.put(context.getWindowId(), new Session(context, taskRunId, normalizedTaskCode, wildBattle));
        if (wildBattle) {
            log.info("[local-runner] Wild Battle auto-combat maintenance armed: windowId={} taskRunId={}",
                    context.getWindowId(), taskRunId);
        }
    }

    /**
     * Arms the current exact Xinshou observation run after its restore command succeeds.
     *
     * @param context current exact window runtime
     * @return true only when a live Xinshou session was armed
     */
    public boolean arm(WindowRuntimeContext context) {
        Session session = currentSession(context, null);
        if (session == null) {
            return false;
        }
        synchronized (session) {
            if (!isCurrent(session)) {
                return false;
            }
            session.armed = true;
            log.info("[local-runner] Runner auto-combat maintenance armed: windowId={} task={} taskRunId={}",
                    context.getWindowId(), session.taskCode, session.taskRunId);
            return true;
        }
    }

    /**
     * Maintains automatic combat once for one exact local combat generation.
     *
     * @param context exact registered window runtime
     * @param taskRunId exact observation task-run identity
     * @param combatGeneration positive local combat generation
     * @param nowMs current epoch milliseconds used for retry gating
     * @return the local maintenance disposition; it is never a task-state verdict
     */
    public MaintenanceResult maintain(
            WindowRuntimeContext context,
            String taskRunId,
            long combatGeneration,
            long nowMs) {
        Session session = currentSession(context, taskRunId);
        if (session == null || combatGeneration <= 0L) {
            return MaintenanceResult.SKIPPED;
        }
        synchronized (session) {
            if (!isCurrent(session)
                    || !session.armed
                    || combatGeneration <= session.completedGeneration
                    || combatGeneration == session.inFlightGeneration
                    || nowMs < session.nextRetryAtMs) {
                return MaintenanceResult.SKIPPED;
            }
            session.inFlightGeneration = combatGeneration;
        }

        XinshouCombatLocalMechanics.Result result;
        try {
            result = maintenancePort.maintain();
        } catch (RuntimeException failure) {
            synchronized (session) {
                session.inFlightGeneration = 0L;
                if (isCurrent(session)) {
                    session.nextRetryAtMs = nowMs + FAILED_RETRY_DELAY_MS;
                }
            }
            log.warn("[local-runner] Runner auto-combat maintenance threw and will retry: "
                            + "windowId={} task={} taskRunId={} generation={} retryAfterMs={}",
                    context.getWindowId(), session.taskCode, taskRunId, combatGeneration, FAILED_RETRY_DELAY_MS, failure);
            throw failure;
        }
        synchronized (session) {
            session.inFlightGeneration = 0L;
            if (!isCurrent(session)) {
                return MaintenanceResult.STALE;
            }
            if (result != null
                    && result.status() == XinshouCombatLocalMechanics.Status.COMPLETED) {
                session.completedGeneration = Math.max(
                        session.completedGeneration, combatGeneration);
                session.nextRetryAtMs = 0L;
                log.info("[local-runner] Runner auto-combat generation maintained: "
                                + "windowId={} task={} taskRunId={} generation={} panel={}",
                        context.getWindowId(), session.taskCode, taskRunId, combatGeneration,
                        result.observedPanel());
                return MaintenanceResult.COMPLETED;
            }
            session.nextRetryAtMs = nowMs + FAILED_RETRY_DELAY_MS;
            log.warn("[local-runner] Runner auto-combat maintenance will retry: "
                            + "windowId={} task={} taskRunId={} generation={} status={} retryAfterMs={}",
                    context.getWindowId(), session.taskCode, taskRunId, combatGeneration,
                    result == null ? null : result.status(), FAILED_RETRY_DELAY_MS);
            return MaintenanceResult.RETRY_LATER;
        }
    }

    /**
     * Closes only the exact session owned by the stopping observation run.
     *
     * @param context exact registered window runtime
     * @param taskRunId exact stopping task-run identity
     */
    public void close(WindowRuntimeContext context, String taskRunId) {
        Session session = currentSession(context, taskRunId);
        if (session != null) {
            sessions.remove(context.getWindowId(), session);
        }
    }

    boolean isArmed(WindowRuntimeContext context, String taskRunId) {
        Session session = currentSession(context, taskRunId);
        if (session == null) {
            return false;
        }
        synchronized (session) {
            return isCurrent(session) && session.armed;
        }
    }

    private Session currentSession(WindowRuntimeContext context, String taskRunId) {
        if (context == null) {
            return null;
        }
        Session session = sessions.get(context.getWindowId());
        if (session == null
                || session.context != context
                || (taskRunId != null && !session.taskRunId.equals(taskRunId))) {
            return null;
        }
        return session;
    }

    private boolean isCurrent(Session session) {
        return sessions.get(session.context.getWindowId()) == session;
    }

    enum MaintenanceResult {
        SKIPPED,
        COMPLETED,
        RETRY_LATER,
        STALE
    }

    @FunctionalInterface
    interface MaintenancePort {
        XinshouCombatLocalMechanics.Result maintain();
    }

    private static final class Session {
        private final WindowRuntimeContext context;
        private final String taskRunId;
        private final String taskCode;
        private boolean armed;
        private long completedGeneration;
        private long inFlightGeneration;
        private long nextRetryAtMs;

        private Session(WindowRuntimeContext context, String taskRunId, String taskCode, boolean armed) {
            this.context = context;
            this.taskRunId = taskRunId;
            this.taskCode = taskCode;
            this.armed = armed;
        }
    }
}
