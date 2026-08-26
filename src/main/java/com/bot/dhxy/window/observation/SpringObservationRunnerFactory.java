package com.bot.dhxy.window.observation;

import com.bot.dhxy.cloud.turn.CloudTurnSidecarProperties;
import com.bot.dhxy.cloud.turn.HttpsTurnClient;
import com.bot.dhxy.cloud.turn.TurnClient;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.execution.MultiWindowTaskManager;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * TURN-40G: Spring wiring for the observation plane. Derives the observation transport from the configured turn
 * client's exact configuration/authentication (via the sanctioned {@link HttpsTurnClient#newObservationClient()}
 * reuse seam) and the tenant identity from the existing sidecar configuration, then registers itself on the
 * {@link ObservationRunnerWiring} bridge so the non-Spring turn loop factory can create per-window runners after a
 * start acknowledgement. Each created runner carries a per-window sampler bound to the exact registered
 * {@link WindowRuntimeContext} so Cloud-issued interests can be executed locally. When the turn client is not the
 * HTTPS transport (contract-test doubles), no factory is registered and turn loops run without observation.
 */
@Component
public class SpringObservationRunnerFactory implements WindowObservationRunnerFactory {

    private static final Logger log = LoggerFactory.getLogger(SpringObservationRunnerFactory.class);

    private final ObservationClient observationClient;
    private final String tenantId;
    private final MultiWindowTaskManager taskManager;
    private final WindowTaskContextHolder contextHolder;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final DialogService kandaDialogService;
    private final InputSequences inputSequences;
    private final LocalMaintenanceBroadcastHandler maintenanceBroadcastHandler;
    private final DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator;
    private final UICleanerService uiCleanerService;
    private final com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics combatLocalMechanics;
    private final boolean localKandaEnabled;
    private final LocalLeaderCombatBroadcast leaderCombatBroadcast;

    public SpringObservationRunnerFactory(TurnClient turnClient,
                                          CloudTurnSidecarProperties sidecarProperties,
                                          MultiWindowTaskManager taskManager,
                                          WindowTaskContextHolder contextHolder,
                                          GameClientTracker tracker,
                                          CoordinateHelper coordinateHelper,
                                          DialogService kandaDialogService,
                                          InputSequences inputSequences,
                                          LocalMaintenanceBroadcastHandler maintenanceBroadcastHandler,
                                          DeferredReturnHomeReplayCoordinator returnHomeReplayCoordinator,
                                          UICleanerService uiCleanerService,
                                          com.bot.dhxy.cloud.turn.local.XinshouCombatLocalMechanics combatLocalMechanics,
                                          @Value("${bot.xiuluo.local-kanda-enabled:false}") boolean localKandaEnabled) {
        Objects.requireNonNull(turnClient, "turnClient");
        this.tenantId = Objects.requireNonNull(sidecarProperties, "sidecarProperties").getTenantId();
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.coordinateHelper = Objects.requireNonNull(coordinateHelper, "coordinateHelper");
        this.kandaDialogService = Objects.requireNonNull(kandaDialogService, "kandaDialogService");
        this.inputSequences = Objects.requireNonNull(inputSequences, "inputSequences");
        this.maintenanceBroadcastHandler = Objects.requireNonNull(
                maintenanceBroadcastHandler, "maintenanceBroadcastHandler");
        this.returnHomeReplayCoordinator = Objects.requireNonNull(
                returnHomeReplayCoordinator, "returnHomeReplayCoordinator");
        this.uiCleanerService = Objects.requireNonNull(uiCleanerService, "uiCleanerService");
        this.combatLocalMechanics = Objects.requireNonNull(combatLocalMechanics, "combatLocalMechanics");
        this.localKandaEnabled = localKandaEnabled;
        this.leaderCombatBroadcast = new LocalLeaderCombatBroadcast(() -> taskManager.getAllSnapshots().stream()
                .map(snapshot -> taskManager.getRunner(snapshot.getWindowId())
                        .map(runner -> runner.getWindowContext())
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList());
        if (turnClient instanceof HttpsTurnClient httpsTurnClient) {
            this.observationClient = httpsTurnClient.newObservationClient();
            ObservationRunnerWiring.register(this);
            log.info("Observation plane wired: tenantId={} localKandaEnabled={}", tenantId, localKandaEnabled);
        } else {
            this.observationClient = null;
            log.info("Observation plane not wired: turn client is not the HTTPS transport");
        }
    }

    @Override
    public WindowObservationRunner create(String deviceId,
                                          String windowId,
                                          String hwnd,
                                          String taskCode,
                                          String taskRunId) {
        if (observationClient == null) {
            return null;
        }
        WindowRuntimeContext context = taskManager.getRunner(windowId)
                .map(runner -> runner.getWindowContext())
                .orElse(null);
        returnHomeReplayCoordinator.clear(context, "new acknowledged taskRun " + taskRunId);
        WindowObservationSampler sampler = context == null
                ? null
                : new WindowObservationSampler(context, contextHolder, tracker, coordinateHelper,
                kandaDialogService, inputSequences, taskRunId, localKandaEnabled,
                new LocalCombatSignalMechanics(tracker, coordinateHelper),
                returnHomeReplayCoordinator);
        if (sampler != null) {
            sampler.bindAutoPanelMechanics(combatLocalMechanics);
            sampler.bindLeaderCombatBroadcast(leaderCombatBroadcast);
        }
        // 五环仍可复用被动 UI 探针；terminal-frame capture 本身不再调用 cleanup。
        if (sampler != null && "WUHUAN_V3".equalsIgnoreCase(taskCode)) {
            sampler.bindUiCleanerService(uiCleanerService);
        }
        if (sampler == null) {
            log.warn("Observation runner created without a sampler (no registered window context): windowId={}",
                    windowId);
        }
        LocalMaintenanceBroadcastRunner localMaintenanceRunner = context != null
                && isAutoBattleOnly(taskCode)
                ? new LocalMaintenanceBroadcastRunner(context, contextHolder, maintenanceBroadcastHandler)
                : null;
        PreparedFrameCapture preparedFrameCapture = context == null
                ? null : new ExactWindowPreparedFrameCapture(context, contextHolder, tracker);
        return new WindowObservationRunner(
                observationClient, tenantId, deviceId, windowId, hwnd, taskCode, taskRunId,
                sampler, localMaintenanceRunner, preparedFrameCapture,
                WindowObservationRunner.PARKED_HEARTBEAT_PERIOD_MS);
    }

    /**
     * The local patrol may span the acknowledged queue lifetime, so every effective queue element must be passive
     * auto-battle. A mixed queue cannot safely patrol while a non-passive task owns the same window.
     */
    static boolean isAutoBattleOnly(String taskCodes) {
        if (taskCodes == null || taskCodes.isBlank()) {
            return false;
        }
        String[] codes = taskCodes.split(",");
        for (String code : codes) {
            if (!"AUTO_BATTLE".equalsIgnoreCase(code.trim())) {
                return false;
            }
        }
        return true;
    }
}
